# Phase 5: Editor Features (Syntax & Navigation) - Research

**Researched:** 2026-04-18
**Domain:** RichTextFX CodeArea — syntax highlighting, autocomplete, occurrence highlighting, go-to-definition, multi-file search
**Confidence:** HIGH (all core claims verified via Context7 / official RichTextFX source)

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| EDIT-04 | Syntax highlighting for XML/XSLT files (element names, attributes, comments, CDATA) | StyleSpans + regex XML pattern; CSS classes applied via `codeArea.setStyleSpans(0, spans)` |
| EDIT-05 | Static autocomplete for common XSL/XSL-FO tags (keyword list, Ctrl+Space or `<xsl:` trigger) | ContextMenu or Popup anchored at caret via `getCaretBounds`; triggered via `Nodes.addInputMap` |
| EDIT-06 | Variable/template name highlighting (regex-based, same file scope) | Overlay StyleSpans for occurrence markers; or `setStyle(start, end, classes)` per occurrence |
| EDIT-07 | Go-to-definition for `xsl:include`/`xsl:import` hrefs (opens referenced file in new tab) | Ctrl+Click: `addEventFilter(MouseEvent.MOUSE_CLICKED, ...)` + `CharacterHit hit = area.hit(x, y)` |
| EDIT-08 | Multi-file text search across all project files, results list navigable | Background `Task<List<SearchHit>>`, Dialog with ListView, `editorController.openOrFocusTab(path)` + `moveTo(line, col)` |
</phase_requirements>

---

## Summary

Phase 5 adds intelligent editor features on top of the RichTextFX `CodeArea` already wired in Phase 4. All five requirements are achievable with the libraries already on the classpath — no new runtime dependencies are needed. The core pattern is the same for all features: subscribe to a CodeArea event stream (text changes, key presses, or mouse events), compute a result on a background thread when necessary, and apply the result back on the JavaFX Application Thread.

The most technically nuanced requirement is EDIT-04 (syntax highlighting): for large files the regex must run off the JavaFX thread using `Task` and `ExecutorService`, otherwise typing becomes sluggish. The RichTextFX team ships an official async demo (`JavaKeywordsAsyncDemo`) that provides the exact pattern. Every other requirement (autocomplete, occurrence highlighting, go-to-definition, multi-file search) is straightforward JavaFX: either a `ContextMenu`/`Popup`, a per-occurrence `setStyle` call, a mouse event filter, or a `Task`-backed `Dialog`.

**Primary recommendation:** Use the RichTextFX XMLEditorDemo regex pattern for EDIT-04. Apply highlighting asynchronously via a single-threaded `ExecutorService` + `Task`. Wire all keyboard shortcuts via `Nodes.addInputMap` (same pattern as Ctrl+S in Phase 4, not scene-level).

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Syntax highlighting (EDIT-04) | Editor UI (CodeArea) | Background thread (ExecutorService) | Regex runs off-thread; `setStyleSpans` called on FX thread |
| Autocomplete popup (EDIT-05) | Editor UI (CodeArea) | — | Pure UI: ContextMenu or Popup anchored to caret bounds |
| Occurrence highlighting (EDIT-06) | Editor UI (CodeArea) | — | Selection listener + regex scan + StyleSpans overlay in same tab |
| Go-to-definition (EDIT-07) | Editor UI (CodeArea) | EditorController | Mouse event in CodeArea; file open delegated to EditorController |
| Multi-file search (EDIT-08) | Search Task (Background) | EditorController | Background Task scans files; result navigates via existing `openOrFocusTab` |

---

## Standard Stack

### Core (already on classpath — no new dependencies)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `org.fxmisc.richtext:richtextfx` | 0.11.5 | CodeArea, StyleSpans, StyleSpansBuilder | Already declared in `build.gradle` |
| `org.fxmisc.wellbehaved:wellbehavedfx` | (transitive) | `Nodes.addInputMap` for keyboard shortcuts | Used in Phase 4 for Ctrl+S |
| `org.reactfx:reactfx` | (transitive) | `multiPlainChanges().successionEnds()` event stream | Bundled with RichTextFX |
| JavaFX `javafx.scene.control.ContextMenu` | JDK 21 | Autocomplete popup for EDIT-05 | Standard JavaFX, no dep needed |
| JavaFX `javafx.concurrent.Task` | JDK 21 | Background thread work for highlighting, search | Standard JavaFX concurrency |

**No new `build.gradle` dependencies required for this phase.** [VERIFIED: build.gradle in codebase]

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| ContextMenu for autocomplete | `javafx.stage.Popup` with custom ListView | Popup gives more layout control but requires manual positioning; ContextMenu is simpler and sufficient for a static keyword list |
| Single-threaded ExecutorService | `Platform.runLater` inline | `Platform.runLater` blocks the FX thread during computation; ExecutorService+Task is the correct off-thread pattern |
| Regex-based occurrence highlighting | SAX/DOM parse | Regex is fast, sufficient for MVP (same-file, user-selected token). SAX is overkill — EDIT-06 is scoped to same file |

---

## Architecture Patterns

### System Architecture Diagram

```
User types in CodeArea
        |
        v
[multiPlainChanges EventStream]
        |-- successionEnds(500ms) -->
        |
        v
[ExecutorService single-thread]
        |
        +-- computeHighlighting(text) --> StyleSpans
        |          (off FX thread)
        v
[Platform.runLater / reactive subscription]
        |
        v
[codeArea.setStyleSpans(0, spans)]  <-- FX thread
        |
        v
[CSS classes applied to text ranges]

Ctrl+Space / "<xsl:" typed
        |
        v
[Nodes.addInputMap KeyPressed]
        |
        v
[Filter keyword list, build ContextMenu items]
        |
        v
[ContextMenu.show(codeArea, side, x, y)]

User selects "$varName" or "name="template""
        |
        v
[selectedTextProperty listener]
        |
        v
[Regex scan full text for occurrences]
        |
        v
[codeArea.setStyle(start, end, ["occurrence"])] for each match

Ctrl+Click on href value
        |
        v
[addEventFilter(MOUSE_CLICKED) + isControlDown()]
        |
        v
[area.hit(x,y) -> CharacterHit -> insertionIndex]
        |
        v
[regex extract href value at char index]
        |
        v
[editorController.openOrFocusTab(resolvedPath)]

Multi-file search dialog submitted
        |
        v
[Task<List<SearchHit>> on background thread]
        |-- Files.walk project root, grep each file
        v
[Dialog ListView populated on FX thread]
        |-- user clicks result
        v
[openOrFocusTab(path) + moveTo(line, col)]
```

### Recommended Project Structure

```
src/main/java/ch/ti/gagi/xlseditor/
├── ui/
│   ├── EditorController.java      (Phase 4 — extend with phase 5 hooks)
│   ├── EditorTab.java             (Phase 4 — unchanged)
│   ├── XmlSyntaxHighlighter.java  (NEW: static computeHighlighting method)
│   ├── AutocompleteProvider.java  (NEW: keyword list + ContextMenu show/hide)
│   ├── OccurrenceHighlighter.java (NEW: regex-based same-file occurrence scan)
│   └── SearchDialog.java          (NEW: Dialog subclass for EDIT-08)
src/main/resources/ch/ti/gagi/xlseditor/ui/
│   └── main.css                   (extend with syntax highlight CSS classes)
```

**One class per concern.** `EditorController` wires the new helpers into each `EditorTab.codeArea` during `buildTab()`, keeping the per-feature logic encapsulated.

---

### Pattern 1: XML Syntax Highlighting with StyleSpans (EDIT-04)

**What:** Regex-based scan of full document text; builds a `StyleSpans<Collection<String>>` mapping character ranges to CSS classes. Applied via `codeArea.setStyleSpans(0, spans)`.

**When to use:** On every text change, debounced at 300–500 ms via ReactFX `successionEnds`.

**Source:** [VERIFIED: Context7 /fxmisc/richtextfx, XMLEditorDemo.java on GitHub]

```java
// XmlSyntaxHighlighter.java
private static final Pattern XML_TAG = Pattern.compile(
    "(?<COMMENT><!--[\\s\\S]*?-->)"
    + "|(?<CDATA><!\\[CDATA\\[[\\s\\S]*?\\]\\]>)"
    + "|(?<PI><\\?[\\s\\S]*?\\?>)"
    + "|(?<ELEMENT>(</?)(\\w[\\w:-]*)((?:[^<>\"'/]|\"[^\"]*\"|'[^']*'|/(?!>))*)(/?>))"
);
private static final Pattern ATTRIBUTES = Pattern.compile(
    "(\\w[\\w:-]*)\\s*(=)\\s*(\"[^\"]*\"|'[^']*')"
);

public static StyleSpans<Collection<String>> computeHighlighting(String text) {
    Matcher matcher = XML_TAG.matcher(text);
    StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
    int lastKwEnd = 0;
    while (matcher.find()) {
        spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
        if (matcher.group("COMMENT") != null) {
            spansBuilder.add(Collections.singleton("xml-comment"), matcher.end() - matcher.start());
        } else if (matcher.group("CDATA") != null) {
            spansBuilder.add(Collections.singleton("xml-cdata"), matcher.end() - matcher.start());
        } else if (matcher.group("PI") != null) {
            spansBuilder.add(Collections.singleton("xml-pi"), matcher.end() - matcher.start());
        } else {
            // Element: nested attribute highlighting
            int elemStart = matcher.start();
            // ... nested ATTRIBUTES matcher pass
            spansBuilder.add(Collections.singleton("xml-tagmark"), ...);
        }
        lastKwEnd = matcher.end();
    }
    spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
    return spansBuilder.create();
}
```

CSS classes needed in `main.css`:
```css
.xml-comment   { -fx-fill: #6a9955; }
.xml-cdata     { -fx-fill: #ce9178; }
.xml-pi        { -fx-fill: #c586c0; }
.xml-tagmark   { -fx-fill: #808080; }
.xml-element   { -fx-fill: #4ec9b0; }
.xml-attribute { -fx-fill: #9cdcfe; }
.xml-avalue    { -fx-fill: #ce9178; }
```

---

### Pattern 2: Async Highlighting Subscription (EDIT-04 performance)

**What:** Off-thread computation via `ExecutorService` + `Task`. Result applied on FX thread.

**When to use:** Always — the sync inline pattern blocks the FX thread during regex computation for large files.

**Source:** [VERIFIED: Context7 /fxmisc/richtextfx, JavaKeywordsAsyncDemo.java referenced in README]

```java
// Inside EditorController.buildTab(), wiring highlighting for a new CodeArea:
ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "highlight-thread-" + path.getFileName());
    t.setDaemon(true);   // Does not block JVM shutdown
    return t;
});

Subscription highlightSub = codeArea
    .multiPlainChanges()
    .successionEnds(Duration.ofMillis(300))
    .subscribe(change -> {
        String snapshot = codeArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<>() {
            @Override protected StyleSpans<Collection<String>> call() {
                return XmlSyntaxHighlighter.computeHighlighting(snapshot);
            }
        };
        task.setOnSucceeded(e ->
            codeArea.setStyleSpans(0, task.getValue()));
        executor.submit(task);
    });

// On tab close: highlightSub.unsubscribe(); executor.shutdownNow();
// Store both in EditorTab or as local captured vars in buildTab closure.
```

**Critical:** the `Subscription` and `ExecutorService` must be disposed when the tab closes (in `tab.setOnClosed`). Failure to do this causes a memory/thread leak.

---

### Pattern 3: Autocomplete Popup (EDIT-05)

**What:** `ContextMenu` or `Popup` with a `ListView` of matching keywords. Shown at caret position. Dismissed on Escape or selection.

**When to use:** On Ctrl+Space (Nodes.addInputMap) or when user types `<xsl:` (text change listener checks last 5 chars).

**Source:** [VERIFIED: Context7 /fxmisc/richtextfx — ContextMenu example, keyboard shortcut InputMap example]

```java
// Trigger on Ctrl+Space
Nodes.addInputMap(codeArea, consume(keyPressed(SPACE, CONTROL_DOWN), e -> {
    String prefix = extractTokenBeforeCaret(codeArea);
    List<String> matches = XSL_KEYWORDS.stream()
        .filter(k -> k.startsWith(prefix))
        .collect(Collectors.toList());
    if (!matches.isEmpty()) showAutocomplete(codeArea, matches);
}));

// Trigger on "<xsl:" typed (text listener)
codeArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
    String text = codeArea.getText();
    if (newPos >= 5 && text.substring(newPos - 5, newPos).equals("<xsl:")) {
        showAutocomplete(codeArea, XSL_KEYWORDS);
    }
});

private void showAutocomplete(CodeArea area, List<String> items) {
    ContextMenu menu = new ContextMenu();
    for (String item : items) {
        MenuItem mi = new MenuItem(item);
        mi.setOnAction(e -> insertCompletion(area, item));
        menu.getItems().add(mi);
    }
    Optional<Bounds> caretBounds = area.getCaretBounds();
    caretBounds.ifPresent(b ->
        menu.show(area, b.getMaxX(), b.getMaxY()));
}
```

**Static keyword list** (hardcoded `List<String>` in `AutocompleteProvider`): common XSL/XSL-FO tags such as `xsl:template`, `xsl:apply-templates`, `xsl:call-template`, `xsl:value-of`, `xsl:for-each`, `xsl:if`, `xsl:choose`, `xsl:when`, `xsl:otherwise`, `xsl:variable`, `xsl:param`, `xsl:with-param`, `xsl:include`, `xsl:import`, `fo:block`, `fo:inline`, `fo:page-sequence`, etc.

---

### Pattern 4: Occurrence Highlighting (EDIT-06)

**What:** When user selects a token (`$varName` or text of `name="..."` attribute), scan the full document text with a regex and apply a CSS class to each match's character range.

**When to use:** On `selectedTextProperty().addListener(...)`. Only trigger when selected text is non-empty and matches `\$[\w-]+` or a bare word in a name attribute.

**Source:** [VERIFIED: Context7 /fxmisc/richtextfx — setStyleSpans / StyleSpans overlay]

```java
codeArea.selectedTextProperty().addListener((obs, oldSel, newSel) -> {
    // Clear previous occurrence styles
    codeArea.setStyleSpans(0, XmlSyntaxHighlighter.computeHighlighting(codeArea.getText()));
    if (newSel.isBlank() || newSel.length() < 2) return;

    // Overlay occurrence highlights on top of syntax spans
    String text = codeArea.getText();
    Pattern pat = Pattern.compile(Pattern.quote(newSel));
    Matcher m = pat.matcher(text);
    while (m.find()) {
        // StyleSpans overlay: addStyle merges with existing spans
        codeArea.setStyle(m.start(), m.end(), Collections.singleton("occurrence"));
    }
});
```

CSS:
```css
.occurrence { -fx-background-color: rgba(255, 215, 0, 0.3); }
```

**Note:** Merging occurrence highlights with syntax-highlight spans requires care. The simplest approach is to recompute the full syntax `StyleSpans` and then overlay occurrence ranges via `codeArea.setStyle(start, end, classes)` — this modifies spans in-place without needing a second `setStyleSpans` call. [ASSUMED — the exact merge semantics of setStyle vs setStyleSpans need confirmation from the RichTextFX API at runtime; an alternative is to build a combined StyleSpans manually]

---

### Pattern 5: Go-to-Definition (EDIT-07)

**What:** Ctrl+Click on text opens the file named in the `href` attribute of `xsl:include` or `xsl:import` at the cursor position.

**When to use:** `addEventFilter(MouseEvent.MOUSE_CLICKED)` checking `event.isControlDown()`.

**Source:** [VERIFIED: Context7 /fxmisc/richtextfx — `area.hit(x, y)` returns `CharacterHit`; ViewActionsExample]

```java
codeArea.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
    if (!event.isControlDown() || event.getButton() != MouseButton.PRIMARY) return;
    event.consume(); // prevent caret repositioning

    CharacterHit hit = codeArea.hit(event.getX(), event.getY());
    int charIndex = hit.getInsertionIndex();
    String fullText = codeArea.getText();

    // Walk backwards/forwards from charIndex to find if we are inside href="..."
    Optional<Path> target = HrefExtractor.extractHref(fullText, charIndex, currentFilePath);
    target.ifPresent(editorController::openOrFocusTab);
});
```

`HrefExtractor` is a small static utility: scans outward from `charIndex` to find if the character falls inside an `href="..."` value of an `xsl:include` or `xsl:import` element. Returns the resolved absolute `Path` relative to the current file's directory.

---

### Pattern 6: Multi-File Search Dialog (EDIT-08)

**What:** A `Dialog<Void>` with a search field and a `ListView<SearchHit>`. User types a query, clicks Search. A `Task` scans all project files on a background thread.

**When to use:** Menu item "Find in Files" (EDIT-08).

**Source:** [ASSUMED — standard JavaFX Task pattern; no specific RichTextFX dependency]

```java
// SearchHit record
record SearchHit(Path file, int line, int column, String lineText) {
    @Override public String toString() {
        return file.getFileName() + ":" + (line+1) + "  " + lineText.strip();
    }
}

// Background Task
Task<List<SearchHit>> task = new Task<>() {
    @Override protected List<SearchHit> call() throws Exception {
        List<SearchHit> hits = new ArrayList<>();
        Files.walk(projectRoot)
            .filter(p -> Files.isRegularFile(p) && !Files.isHidden(p))
            .forEach(file -> {
                try {
                    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                    for (int i = 0; i < lines.size(); i++) {
                        int col = lines.get(i).indexOf(query);
                        if (col >= 0) hits.add(new SearchHit(file, i, col, lines.get(i)));
                    }
                } catch (IOException ignored) {}
            });
        return hits;
    }
};

task.setOnSucceeded(e -> {
    listView.getItems().setAll(task.getValue());
});
executor.submit(task);

// On result click:
listView.setOnMouseClicked(e -> {
    SearchHit hit = listView.getSelectionModel().getSelectedItem();
    if (hit != null) {
        editorController.openOrFocusTab(hit.file());
        // Then scroll/navigate to line
        // Requires EditorController to expose a navigateTo(path, line, col) method
    }
});
```

**EditorController extension needed:** a `navigateTo(Path path, int line, int col)` method that calls `openOrFocusTab` then `codeArea.moveTo(line, col)` and `codeArea.requestFollowCaret()`.

---

### Anti-Patterns to Avoid

- **Computing StyleSpans on the FX thread:** For files > 100 lines, regex matching blocks the FX thread long enough to cause perceptible stutter. Always use `Task` + `ExecutorService`.
- **Scene-level Ctrl+Space:** Same pattern as Phase 4's scene-level Ctrl+S bug. Use `Nodes.addInputMap` per CodeArea.
- **Not unsubscribing ReactFX Subscriptions on tab close:** Causes the closed `CodeArea` to be retained by the event stream's strong reference. Memory leak. Unsubscribe in `tab.setOnClosed`.
- **Not shutting down the ExecutorService on tab close:** Daemon threads will terminate on JVM exit, but shutting down explicitly in `tab.setOnClosed` is the correct approach.
- **Using `setStyleSpans` for occurrence overlay without restoring syntax spans:** Calling `setStyleSpans(0, occurrenceSpans)` replaces all existing syntax highlighting. Either build a merged StyleSpans or use `setStyle(start, end, classes)` for occurrence additions.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Debounced text change subscription | Custom Timer/ScheduledExecutor | `multiPlainChanges().successionEnds(Duration.ofMillis(300))` | ReactFX already on classpath; correctly handles rapid typing collapse |
| Keyboard shortcut binding | `scene.addEventHandler` | `Nodes.addInputMap(codeArea, consume(...))` | Scene-level causes focus bugs (established Phase 4 decision) |
| Character-at-mouse-position | Manual coordinate math | `area.hit(mouseX, mouseY).getInsertionIndex()` | RichTextFX API handles virtualized layout offsets |
| Caret screen position for popup | Manual layout math | `area.getCaretBounds()` → `Optional<Bounds>` | RichTextFX API; handles scroll offset correctly |
| Style span merging from scratch | Custom span tree | Compute full syntax span + then `setStyle` calls for occurrences | Simplest; RichTextFX merges style classes per character range |

---

## Common Pitfalls

### Pitfall 1: StyleSpans built with zero remaining length
**What goes wrong:** `StyleSpansBuilder.create()` throws if the sum of all span lengths does not equal `text.length()`. The error message is cryptic.
**Why it happens:** The trailing `spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd)` must always be present, even if the regex matches to the very end.
**How to avoid:** Always add the trailing span with `text.length() - lastKwEnd` — even when that value is 0.
**Warning signs:** `IllegalStateException` or `IllegalArgumentException` at `StyleSpansBuilder.create()`.

### Pitfall 2: Subscription and ExecutorService leak on tab close
**What goes wrong:** Closed tab's CodeArea is kept alive by the ReactFX subscription and/or the ExecutorService task queue.
**Why it happens:** `multiPlainChanges()` holds a strong reference to the `CodeArea`. If the `Subscription` is not unsubscribed, the CodeArea cannot be garbage-collected.
**How to avoid:** In `tab.setOnClosed`, call `highlightSub.unsubscribe()` and `executor.shutdownNow()`.
**Warning signs:** Heap grows with each tab open/close cycle.

### Pitfall 3: `hit()` returns wrong index after scroll
**What goes wrong:** Ctrl+Click navigates to the wrong character when the CodeArea is scrolled.
**Why it happens:** `hit(x, y)` takes coordinates relative to the CodeArea's local coordinate system (already accounting for scroll). Using screen coordinates directly is wrong.
**How to avoid:** Use `event.getX(), event.getY()` from a `MouseEvent` registered on the `codeArea` node, not `event.getScreenX()`.
**Warning signs:** Go-to-definition opens wrong file, or HrefExtractor finds wrong text.

### Pitfall 4: Autocomplete ContextMenu does not close on Escape
**What goes wrong:** The ContextMenu stays visible after the user presses Escape.
**Why it happens:** Default `ContextMenu` key handling may be swallowed by CodeArea's `InputMap`.
**How to avoid:** Add `Nodes.addInputMap(codeArea, consume(keyPressed(ESCAPE), e -> menu.hide()))` while the menu is showing, or rely on JavaFX's default ContextMenu dismissal (it usually works unless CodeArea consumes ESCAPE first).
**Warning signs:** User has to click elsewhere to dismiss the autocomplete popup.

### Pitfall 5: `setStyleSpans` applied when text is empty
**What goes wrong:** RichTextFX throws if you call `setStyleSpans(0, spans)` on a CodeArea with 0 characters but the StyleSpans has a non-zero total length.
**Why it happens:** Race condition: text is cleared, then the async task's `onSucceeded` fires with spans computed from the old text snapshot.
**How to avoid:** In `task.setOnSucceeded`, guard with `if (codeArea.getLength() > 0 && spans.length() == codeArea.getLength())` before applying.
**Warning signs:** Exception thrown asynchronously, highlights applied after file close.

### Pitfall 6: Initial highlighting not applied
**What goes wrong:** When a tab is first opened, the highlighting only appears after the first keystroke.
**Why it happens:** The ReactFX subscription fires on changes; the initial `replaceText()` in `EditorTab` constructor fires before the subscription is established (in `buildTab()`).
**How to avoid:** After setting up the highlighting subscription in `buildTab()`, explicitly compute and apply highlighting once: `codeArea.setStyleSpans(0, XmlSyntaxHighlighter.computeHighlighting(codeArea.getText()))`.
**Warning signs:** New tabs open with plain text; highlighting appears only after first edit.

---

## Code Examples

### Verified: ReactFX successionEnds pattern
Source: [Context7 /fxmisc/richtextfx]
```java
// Inside buildTab():
Subscription highlightSub = codeArea
    .multiPlainChanges()
    .successionEnds(Duration.ofMillis(300))
    .subscribe(changes -> {
        String snapshot = codeArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<>() {
            @Override protected StyleSpans<Collection<String>> call() {
                return XmlSyntaxHighlighter.computeHighlighting(snapshot);
            }
        };
        task.setOnSucceeded(evt -> {
            if (codeArea.getLength() > 0 && task.getValue().length() == codeArea.getLength()) {
                codeArea.setStyleSpans(0, task.getValue());
            }
        });
        executor.submit(task);
    });
```

### Verified: Caret bounds for popup positioning
Source: [Context7 /fxmisc/richtextfx]
```java
Optional<Bounds> bounds = codeArea.getCaretBounds();
bounds.ifPresent(b -> contextMenu.show(codeArea, b.getMaxX(), b.getMaxY()));
```

### Verified: hit() for Ctrl+Click character index
Source: [Context7 /fxmisc/richtextfx — ViewActionsExample comment]
```java
codeArea.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
    if (!event.isControlDown()) return;
    CharacterHit hit = codeArea.hit(event.getX(), event.getY());
    int charIndex = hit.getInsertionIndex();
    // extract token at charIndex...
});
```

### Verified: moveTo + requestFollowCaret for navigation
Source: [Context7 /fxmisc/richtextfx — NavigationExample]
```java
codeArea.moveTo(lineIndex, columnIndex);
codeArea.requestFollowCaret();
```

### Verified: InputMap for Ctrl+Space
Source: [Context7 /fxmisc/richtextfx — KeyboardShortcutsExample]
```java
Nodes.addInputMap(codeArea, consume(keyPressed(SPACE, CONTROL_DOWN), e -> {
    // show autocomplete
}));
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `textProperty().addListener()` sync highlighting | `multiPlainChanges().successionEnds()` + Task | RichTextFX 0.9+ | Collapse rapid edits; off-thread safe |
| Manual caret position tracking | `area.getCaretBounds()` | RichTextFX 0.10+ | Direct API, handles virtualized scroll |
| Scene-level key handlers | `Nodes.addInputMap` per CodeArea | WellBehavedFX (Phase 4 decision) | No focus bugs |

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (jupiter:5.10.0) |
| Config file | build.gradle `test { useJUnitPlatform() }` |
| Quick run command | `./gradlew test` |
| Full suite command | `./gradlew test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| EDIT-04 | `XmlSyntaxHighlighter.computeHighlighting()` returns correct CSS classes for XML element, attribute, comment, CDATA, PI | unit | `./gradlew test --tests "*.XmlSyntaxHighlighterTest"` | No — Wave 0 |
| EDIT-04 | `computeHighlighting()` total span length equals input text length (no StyleSpansBuilder crash) | unit | `./gradlew test --tests "*.XmlSyntaxHighlighterTest"` | No — Wave 0 |
| EDIT-05 | `AutocompleteProvider.getMatches(prefix)` returns correct keyword subset | unit | `./gradlew test --tests "*.AutocompleteProviderTest"` | No — Wave 0 |
| EDIT-06 | `OccurrenceHighlighter.findOccurrences(text, token)` returns correct start/end ranges | unit | `./gradlew test --tests "*.OccurrenceHighlighterTest"` | No — Wave 0 |
| EDIT-07 | `HrefExtractor.extractHref(text, charIndex, basePath)` resolves correct path from xsl:include href | unit | `./gradlew test --tests "*.HrefExtractorTest"` | No — Wave 0 |
| EDIT-08 | `SearchTask` returns correct `SearchHit` list for a known set of fixture files | unit | `./gradlew test --tests "*.SearchTaskTest"` | No — Wave 0 |

**Note:** JavaFX UI integration tests (CodeArea rendering) require `Platform.startup()` via `@BeforeAll` (same pattern as `EditorTabTest`). Pure logic classes (`XmlSyntaxHighlighter`, `HrefExtractor`, `AutocompleteProvider`, `OccurrenceHighlighter`) have no JavaFX dependency and can be tested without toolkit initialization.

### Sampling Rate
- **Per task commit:** `./gradlew test`
- **Per wave merge:** `./gradlew test`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/ch/ti/gagi/xlseditor/ui/XmlSyntaxHighlighterTest.java` — covers EDIT-04
- [ ] `src/test/java/ch/ti/gagi/xlseditor/ui/AutocompleteProviderTest.java` — covers EDIT-05
- [ ] `src/test/java/ch/ti/gagi/xlseditor/ui/OccurrenceHighlighterTest.java` — covers EDIT-06
- [ ] `src/test/java/ch/ti/gagi/xlseditor/ui/HrefExtractorTest.java` — covers EDIT-07
- [ ] `src/test/java/ch/ti/gagi/xlseditor/ui/SearchTaskTest.java` — covers EDIT-08

---

## Environment Availability

Step 2.6: Scoped but lightweight. All dependencies are already on the classpath; no external services required.

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 21 | All code | Yes | OpenJDK 21.0.2 | — |
| RichTextFX 0.11.5 | EDIT-04..07 | Yes | 0.11.5 | — |
| ReactFX (transitive) | EDIT-04 event stream | Yes | bundled with 0.11.5 | — |
| WellBehavedFX (transitive) | EDIT-05 InputMap | Yes | bundled | — |
| JavaFX 21 (controls, fxml, web) | All UI | Yes | 21 | — |

No missing dependencies.

---

## Security Domain

This phase involves no authentication, no network calls, no file-write (other than pre-existing save), and no external input beyond local filesystem. The one security-relevant operation is path resolution for EDIT-07 (go-to-definition): resolving an `href` attribute value as a file path.

| Risk | Standard Mitigation |
|------|---------------------|
| Path traversal via href (e.g. `href="../../../../etc/passwd"`) | Resolve `basePath.getParent().resolve(href).normalize()`, then verify the resolved path is within the project root before opening. Reject if outside. |
| Extremely large files causing regex catastrophic backtracking | Limit syntax highlighting to files < 5 MB; show a "file too large for highlighting" notice otherwise. |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `setStyle(start, end, classes)` can be called after `setStyleSpans` to overlay occurrence highlights without replacing the full syntax span set | Pattern 4 (Occurrence Highlighting) | Occurrence highlighting replaces syntax highlighting; fix: build a merged StyleSpans manually |
| A2 | `codeArea.getCaretBounds()` returns screen-adjusted bounds suitable for ContextMenu.show() positioning | Pattern 3 (Autocomplete) | Popup appears at wrong screen position; fix: use `localToScreen()` conversion |
| A3 | `CharacterHit hit = area.hit(event.getX(), event.getY())` uses CodeArea-local coordinates (not screen) | Pattern 5 (Go-to-definition) | Wrong char index; fix: convert screen coords via `area.screenToLocal()` |

---

## Open Questions

1. **StyleSpans overlay for occurrence highlighting**
   - What we know: `setStyleSpans(0, spans)` replaces all spans. `setStyle(start, end, classes)` exists but its interaction with existing StyleSpans is not documented definitively in Context7.
   - What's unclear: Whether `setStyle` merges CSS classes with existing StyleSpans or replaces them.
   - Recommendation: Test with a simple proof-of-concept in Wave 0. If merge fails, build a combined StyleSpans: compute syntax spans, then iterate occurrences and inject the "occurrence" class into the relevant ranges.

2. **`getCaretBounds()` coordinate system**
   - What we know: Returns `Optional<Bounds>`. Used in RichTextFX README for popup positioning.
   - What's unclear: Whether bounds are in screen coordinates or local CodeArea coordinates (matters for `ContextMenu.show()`).
   - Recommendation: Use `area.localToScreen(caretBounds)` as a safe conversion step if the raw bounds don't work.

---

## Sources

### Primary (HIGH confidence)
- Context7 `/fxmisc/richtextfx` — StyleSpansBuilder, multiPlainChanges, successionEnds, InputMap keyboard shortcuts, ContextMenu, hit(), getCaretBounds(), moveTo(), requestFollowCaret()
- [RichTextFX README](https://github.com/fxmisc/richtextfx/blob/master/README.md) — features overview, popup positioning utilities, CharacterHit API
- [RichTextFX demos README](https://github.com/fxmisc/richtextfx/blob/master/richtextfx-demos/README.md) — async vs sync highlighting demo existence confirmed
- Codebase: `EditorController.java`, `EditorTab.java`, `build.gradle` — confirmed existing patterns and dependency versions

### Secondary (MEDIUM confidence)
- [JavaKeywordsAsyncDemo](https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/JavaKeywordsAsyncDemo.java) — async highlighting architecture described via WebFetch summary
- [XMLEditorDemo](https://github.com/FXMisc/RichTextFX/blob/master/richtextfx-demos/src/main/java/org/fxmisc/richtext/demo/XMLEditorDemo.java) — XML regex pattern structure described via WebFetch summary

### Tertiary (LOW confidence)
- None — all claims are backed by verified sources or explicitly tagged `[ASSUMED]`

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all on classpath, versions verified in build.gradle
- Architecture (highlighting, autocomplete, search): HIGH — Context7 verified API calls
- StyleSpans occurrence overlay merge: LOW — tagged [ASSUMED] A1; needs runtime test
- Pitfalls: HIGH — derived from official demo patterns and known RichTextFX issues

**Research date:** 2026-04-18
**Valid until:** 2026-07-18 (RichTextFX is stable; ReactFX API is frozen at 2.0)
