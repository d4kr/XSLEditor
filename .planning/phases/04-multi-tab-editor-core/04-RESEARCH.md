# Phase 4: Multi-Tab Editor (Core) - Research

**Researched:** 2026-04-18
**Domain:** JavaFX TabPane + RichTextFX CodeArea, file I/O, dirty-state tracking
**Confidence:** HIGH

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| EDIT-01 | Editor supports multi-tab layout (one tab per open file) | `TabPane` + `Tab` holding `VirtualizedScrollPane<CodeArea>`. Dedup by path. |
| EDIT-02 | Tab shows filename with dirty indicator (`*`) when unsaved changes exist | `UndoManager.atMarkedPositionProperty()` drives `BooleanProperty dirty`; tab title bound to it. |
| EDIT-03 | Ctrl+S saves the current file and clears dirty state | WellBehavedFX `Nodes.addInputMap` on `CodeArea`; `undoManager.mark()` after write. |
| EDIT-09 | Closing a tab with unsaved changes prompts confirmation | `Tab.setOnCloseRequest(Event)` — consume event to cancel; show `Alert.CONFIRMATION`. |
</phase_requirements>

---

## Summary

Phase 4 introduces the multi-tab editor — the central UI component of XSLEditor. The editor
pane (`StackPane editorPane`, fx:id wired in main.fxml) currently holds a placeholder `Label`;
Phase 4 replaces it with a `TabPane` at runtime (programmatically, not via FXML). Each tab
holds one `VirtualizedScrollPane<CodeArea>` from RichTextFX 0.11.5, which is already on the
classpath (`build.gradle` line 37).

Dirty state is cleanly handled via `UndoManager.atMarkedPositionProperty()`: `mark()` is called
after load and after save, and the inverse of `atMarkedPosition` drives the `*` prefix on the
tab title. The `Tab.setOnCloseRequest` hook intercepts close events before they fire and shows a
confirmation dialog when the tab is dirty. The no-duplicate-tabs invariant is implemented via a
`Map<Path, Tab>` registry.

The key integration seam is `FileTreeController.setOnFileOpenRequest(Consumer<Path>)` — already
scaffolded with a no-op default in Phase 3 (D-05). Phase 4 wires in
`editorController::openOrFocusTab` via this seam, so Phase 3 code is unchanged.

**Primary recommendation:** Create `EditorController` as a plain Java sub-controller (not an
FXML controller), instantiated in `MainController.initialize()` exactly like `FileTreeController`.
Pass it `editorPane` and a dirty-count callback. It owns the `TabPane` and a
`Map<Path, Tab>` registry.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Tab management (open, focus, dedup) | UI sub-controller (`EditorController`) | `MainController` (wires seam) | Keeps MainController thin; mirrors Phase 3 pattern |
| Dirty state per tab | `EditorController` per-tab model (`EditorTab`) | `MainController.setDirty()` aggregate | Per-tab flag drives title; aggregate drives close-window confirmation |
| File read/write | `EditorController` (direct `Files.readString`/`Files.writeString`) | — | Simple local I/O; no pipeline involved |
| Ctrl+S keyboard shortcut | `CodeArea` `InputMap` (WellBehavedFX) | — | Must attach to the focused CodeArea, not the Scene |
| Close-tab confirmation | `Tab.setOnCloseRequest` inside `EditorController` | — | Only `EditorController` knows dirty state per tab |
| Phase 3 integration seam | `FileTreeController.setOnFileOpenRequest` | `MainController.initialize()` | Already scaffolded, zero Phase 3 changes needed |

---

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| RichTextFX (`org.fxmisc.richtext:richtextfx`) | 0.11.5 | `CodeArea` — memory-efficient virtualized text area | Already in `build.gradle`; project decision [VERIFIED: build.gradle] |
| JavaFX Controls (`javafx.scene.control.TabPane`) | 21 | Multi-tab container | Bundled with JavaFX 21 on classpath [VERIFIED: build.gradle] |
| WellBehavedFX (transitive via RichTextFX) | bundled | `Nodes.addInputMap` for `Ctrl+S` | Required API from RichTextFX wiki [VERIFIED: Context7 /fxmisc/richtextfx] |
| `org.fxmisc.flowless:flowless` (transitive) | bundled | `VirtualizedScrollPane` wrapper | Required to scroll a `CodeArea` [VERIFIED: Context7 /fxmisc/richtextfx] |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `java.nio.file.Files` | JDK 21 | `readString` / `writeString` with charset | Standard file I/O; no extra dep needed |
| `javafx.scene.control.Alert` | JavaFX 21 | Close-tab confirmation dialog | Consistent with Phase 1/2/3 patterns already in codebase |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `UndoManager.atMarkedPositionProperty()` for dirty | `plainTextChanges()` stream + manual flag | The `atMarkedPosition` approach is cleaner: it automatically goes `false` on edit, `true` on undo-back-to-saved; no manual flag to reset |
| Programmatic `TabPane` mount | Add `TabPane` to FXML | FXML approach requires injecting it into `MainController`; programmatic mount in `EditorController` keeps the sub-controller self-contained (same pattern as `FileTreeController`) |

**Installation:** No new dependencies needed. RichTextFX 0.11.5 and JavaFX 21 are already in
`build.gradle`. [VERIFIED: build.gradle line 37]

---

## Architecture Patterns

### System Architecture Diagram

```
FileTreeController
  double-click / Enter
        |
        v  onFileOpenRequest(Path)
EditorController.openOrFocusTab(Path)
        |
        +-- path in registry? --> focus existing Tab
        |
        +-- new path --> Files.readString(path)
                          |
                          v
                    new EditorTab(path, content)
                          |
                    CodeArea.replaceText(content)
                    undoManager.mark()           <-- "saved baseline"
                    undoManager.forgetHistory()
                          |
                    Tab title = filename
                    dirty listener:
                      atMarkedPosition=false --> title = "*" + filename
                      atMarkedPosition=true  --> title = filename
                          |
                    Tab.setOnCloseRequest --> if dirty: Alert.CONFIRMATION
                          |
                    TabPane.getTabs().add(tab)
                    tabPane.getSelectionModel().select(tab)
        |
  Ctrl+S (InputMap on CodeArea)
        |
        v
  Files.writeString(path, codeArea.getText())
  undoManager.mark()    <-- "new saved baseline"
  MainController.setDirty(anyTabDirty())
```

### Recommended Project Structure
```
src/main/java/ch/ti/gagi/xsleditor/ui/
  MainController.java          # Phase 1-3 — add wiring call only
  ProjectContext.java          # unchanged
  FileTreeController.java      # unchanged — seam already there
  EditorController.java        # NEW — owns TabPane, registry, open/save/close logic
  EditorTab.java               # NEW — model for one open file (path, dirty prop, codeArea ref)
```

### Pattern 1: EditorTab model record
**What:** A lightweight class (or record) holding the path, the `CodeArea` reference, and the
observable dirty flag derived from `UndoManager`.
**When to use:** Every time a file is opened.
**Example:**
```java
// Source: Context7 /fxmisc/richtextfx — UndoManager dirty detection
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.undo.UndoManager;
import javafx.beans.binding.BooleanBinding;
import java.nio.file.Path;

public final class EditorTab {
    public final Path path;
    public final CodeArea codeArea;
    public final BooleanBinding dirty;  // true when unsaved changes exist

    public EditorTab(Path path, String content) {
        this.path = path;
        this.codeArea = new CodeArea();
        this.codeArea.replaceText(content);

        UndoManager<?> um = codeArea.getUndoManager();
        um.mark();             // mark "just loaded from disk" as saved baseline
        um.forgetHistory();    // clear the undo stack so Ctrl+Z can't undo past load

        // dirty == "NOT at the marked position"
        this.dirty = um.atMarkedPositionProperty().not();
    }
}
```

### Pattern 2: VirtualizedScrollPane wrapping
**What:** `CodeArea` must be wrapped in `VirtualizedScrollPane` to display scrollbars.
**When to use:** Always — bare `CodeArea` has no scrollbars.
**Example:**
```java
// Source: Context7 /fxmisc/richtextfx — Simple Code Editor
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

CodeArea codeArea = new CodeArea();
VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
tab.setContent(scrollPane);
```

### Pattern 3: Ctrl+S via WellBehavedFX InputMap
**What:** Attach a save shortcut to the `CodeArea` using `Nodes.addInputMap` (not `Scene`-level
key handlers, which would conflict with other controls).
**When to use:** Per-tab, attached when the `CodeArea` is created.
**Example:**
```java
// Source: Context7 /fxmisc/richtextfx — Implement Custom Keyboard Shortcuts
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import static javafx.scene.input.KeyCode.S;
import static javafx.scene.input.KeyCombination.CONTROL_DOWN;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;

Nodes.addInputMap(codeArea, consume(keyPressed(S, CONTROL_DOWN), e -> saveTab(editorTab)));
```

### Pattern 4: Tab close confirmation
**What:** Intercept `Tab.setOnCloseRequest` to show a confirmation dialog when dirty.
**When to use:** Every tab, wired at creation time.
**Example:**
```java
// Source: [ASSUMED] — standard JavaFX Tab API; confirmed in official JavaFX 21 Javadoc
tab.setOnCloseRequest(event -> {
    if (editorTab.dirty.get()) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(primaryStage);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Close \"" + editorTab.path.getFileName() + "\" without saving?");
        alert.setContentText("Your changes will be lost.");
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.CANCEL);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            event.consume(); // cancel the close
        }
    }
});
```

### Pattern 5: No-duplicate-tabs via registry
**What:** A `Map<Path, Tab>` keyed on the canonical (absolute, normalized) path prevents
opening the same file twice.
**When to use:** In `EditorController.openOrFocusTab(Path)`.
**Example:**
```java
// Source: [ASSUMED] — standard Java Map; no library needed
private final Map<Path, Tab> registry = new LinkedHashMap<>();

public void openOrFocusTab(Path absolutePath) {
    Path key = absolutePath.toAbsolutePath().normalize();
    if (registry.containsKey(key)) {
        tabPane.getSelectionModel().select(registry.get(key));
        return;
    }
    // ... create new tab, add to registry
    tab.setOnClosed(e -> registry.remove(key)); // cleanup on close
}
```

### Anti-Patterns to Avoid
- **Bare `CodeArea` without `VirtualizedScrollPane`:** No scrollbars render; content overflows
  silently. Always wrap.
- **Scene-level `Ctrl+S` handler instead of InputMap:** Fires even when a different control has
  focus. Use `Nodes.addInputMap` on the `CodeArea` itself.
- **Calling `undoManager.forgetHistory()` without `mark()` first:** The undo history is cleared
  but no baseline is set; `atMarkedPosition` may give incorrect results. Always `mark()` before
  `forgetHistory()`.
- **Using `codeArea.textProperty()` change listener for dirty detection:** Fires on programmatic
  `replaceText()` during load, falsely marking the file dirty immediately. Use `atMarkedPosition`
  instead.
- **Path dedup via filename string only:** Two files with the same name in different directories
  would collide. Always normalize and use absolute `Path` as the map key.
- **Mounting `TabPane` in FXML:** Requires injecting it into `MainController`. The programmatic
  approach used by `FileTreeController` keeps the sub-controller self-contained.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Dirty state detection | Manual `boolean dirty` + text listener | `UndoManager.atMarkedPositionProperty()` | Handles undo correctly: file becomes clean again if user undoes all changes back to saved state |
| Keyboard shortcut binding | `scene.setOnKeyPressed` global handler | `Nodes.addInputMap` (WellBehavedFX) | Per-control binding; correct event consumption; no focus-management bugs |
| Scrollable code area | Custom `ScrollPane` over `CodeArea` | `VirtualizedScrollPane<CodeArea>` | Required by RichTextFX virtualized layout; plain `ScrollPane` breaks rendering |

**Key insight:** RichTextFX's `UndoManager` already tracks save state correctly — including undo
back to clean. Building a manual dirty flag duplicates this logic with more edge cases.

---

## Common Pitfalls

### Pitfall 1: Dirty flag fires on initial load
**What goes wrong:** Calling `codeArea.replaceText(content)` after `mark()` triggers the undo
history, so `atMarkedPosition` becomes `false` immediately after load — tab shows `*` on open.
**Why it happens:** `replaceText` is recorded as an undo-able change after the mark is set.
**How to avoid:** Call `replaceText(content)` FIRST, then `mark()`, then `forgetHistory()`. This
order sets the baseline after the text is loaded.
**Warning signs:** Tab title shows `*` immediately when a file is opened, before any keystroke.

### Pitfall 2: Close-request handler not consuming event
**What goes wrong:** The confirmation dialog shows but the tab closes anyway.
**Why it happens:** `event.consume()` must be called on the `Event` parameter of
`tab.setOnCloseRequest` — not on a mouse/key event.
**How to avoid:** Use the exact signature `tab.setOnCloseRequest(Event event -> {...})` and call
`event.consume()` to cancel.
**Warning signs:** Tab always closes regardless of user's Cancel choice.

### Pitfall 3: Registry not cleaned up on close
**What goes wrong:** After closing and reopening a file, `openOrFocusTab` finds the stale entry
in the map and tries to select a tab that no longer exists in the `TabPane`.
**Why it happens:** `setOnCloseRequest` is not the same as `setOnClosed`. Only `setOnClosed`
fires after the tab is definitively removed.
**How to avoid:** Call `registry.remove(key)` inside `tab.setOnClosed(e -> ...)`, not inside
`setOnCloseRequest`.
**Warning signs:** File can't be reopened after closing; no tab appears; focus goes to wrong tab.

### Pitfall 4: MainController `dirty` aggregate not updated
**What goes wrong:** The app-level close dialog (Phase 1 `handleCloseRequest`) doesn't reflect
editor dirty state; user can close the window and lose unsaved changes.
**Why it happens:** `MainController.dirty` was set up as a scaffold in Phase 1 but nothing
updates it until Phase 4.
**How to avoid:** `EditorController` must call `mainController.setDirty(anyTabDirty())` whenever
any tab's dirty state changes. Wire a listener on each `EditorTab.dirty` property.
**Warning signs:** Window close without any confirmation even when a tab has unsaved changes.

### Pitfall 5: editorPane children not cleared before mounting TabPane
**What goes wrong:** The placeholder `Label` ("Editor") remains visible behind the `TabPane`
because `StackPane` layers children.
**Why it happens:** `editorPane.getChildren().add(tabPane)` adds on top but doesn't remove the
placeholder.
**How to avoid:** Use `editorPane.getChildren().setAll(tabPane)` to replace, mirroring the
pattern in `FileTreeController.mountTree()`.
**Warning signs:** Ghost "Editor" text visible through or around the tab content area.

---

## Code Examples

Verified patterns from official sources:

### UndoManager dirty detection (correct load order)
```java
// Source: Context7 /fxmisc/richtextfx — Manage Undo and Redo History
CodeArea codeArea = new CodeArea();
codeArea.replaceText(0, 0, fileContent);  // 1. load content FIRST
UndoManager<?> um = codeArea.getUndoManager();
um.mark();              // 2. set "saved" baseline AFTER loading
um.forgetHistory();     // 3. clear history so Ctrl+Z can't undo the load
BooleanBinding dirty = um.atMarkedPositionProperty().not();
```

### Ctrl+S save via WellBehavedFX
```java
// Source: Context7 /fxmisc/richtextfx — Implement Custom Keyboard Shortcuts
Nodes.addInputMap(codeArea, consume(
    keyPressed(S, CONTROL_DOWN),
    e -> saveTab(editorTab)   // saveTab writes file, calls um.mark()
));
```

### Save implementation (mark after write)
```java
// Source: [ASSUMED] standard Files API + RichTextFX UndoManager pattern
private void saveTab(EditorTab editorTab) throws IOException {
    Files.writeString(editorTab.path, editorTab.codeArea.getText(), StandardCharsets.UTF_8);
    editorTab.codeArea.getUndoManager().mark();  // reset "saved" baseline
    updateAppDirtyState();
}
```

### VirtualizedScrollPane wrapping
```java
// Source: Context7 /fxmisc/richtextfx — Create a Simple Code Editor
VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
tab.setContent(scrollPane);
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `TextArea` for code editing | `CodeArea` (RichTextFX) | RichTextFX 0.x | Virtual layout; `StyleSpans` for syntax highlighting in Phase 5 |
| Manual dirty boolean | `UndoManager.atMarkedPositionProperty()` | RichTextFX 0.7+ | Undo correctly restores clean state |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `Tab.setOnCloseRequest` with `event.consume()` prevents tab close in JavaFX 21 | Patterns §4 | If API changed, tab would close regardless; verify with a quick test in Wave 0 |
| A2 | `saveTab` exception should be surfaced via `Alert` (consistent with Phase 2/3 pattern) | Code Examples | If a different error strategy is required, the planner must add an explicit error-handling task |
| A3 | `Files.writeString` with UTF-8 is sufficient for XSLT/XML files in this project | Code Examples | Non-UTF-8 encoded files would be corrupted on save; no charset detection added |

---

## Open Questions

1. **Should Ctrl+S also trigger the aggregate dirty update in MainController?**
   - What we know: `MainController.setDirty(boolean)` exists; Phase 4 must call it.
   - What's unclear: Should save on one tab also refresh the title bar immediately?
   - Recommendation: Yes — call `setDirty(anyTabDirty())` after every save and after every close.

2. **Should the editor show line numbers?**
   - What we know: `LineNumberFactory.get(codeArea)` is one line of code (RichTextFX).
   - What's unclear: Not explicitly in EDIT-01..03/EDIT-09; could be deferred to Phase 5.
   - Recommendation: Add in Phase 4 as it is trivial and improves usability immediately.

---

## Environment Availability

Step 2.6: SKIPPED — Phase 4 is pure code change. All dependencies (JavaFX 21, RichTextFX 0.11.5)
are already on the Gradle classpath and verified in `build.gradle`. No new tools, CLIs, or
external services are required.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter 5.10.0 |
| Config file | `build.gradle` — `test { useJUnitPlatform() }` |
| Quick run command | `./gradlew test` |
| Full suite command | `./gradlew test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| EDIT-01 | TabPane holds one tab per open file | manual-only | — (JavaFX UI; headless test complex) | N/A |
| EDIT-02 | Tab title shows `*` when dirty | unit (logic) | `./gradlew test --tests "*.EditorTabTest"` | ❌ Wave 0 |
| EDIT-03 | Ctrl+S saves and clears dirty | manual-only | — (requires focus + keyboard injection) | N/A |
| EDIT-09 | Close-tab dialog when dirty | manual-only | — (requires modal dialog interaction) | N/A |

> Note: Most editor UI behaviors are not automatable without TestFX or Monocle headless mode.
> The project currently uses plain JUnit 5 with no TestFX. Only the `EditorTab` dirty-state logic
> (pure Java, no UI) is unit-testable. UI requirements remain manual verification items.

### Sampling Rate
- **Per task commit:** `./gradlew test` (existing unit tests must remain green)
- **Per wave merge:** `./gradlew test`
- **Phase gate:** All automated tests green + manual UI checklist complete before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/ch/ti/gagi/xsleditor/ui/EditorTabTest.java` — covers EDIT-02 dirty-state logic
  (unit-testable: create `EditorTab`, verify `dirty.get() == false` after load, `true` after edit,
  `false` after `mark()`)

---

## Security Domain

> `security_enforcement` not explicitly set in config.json (absent = enabled).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — (local desktop tool, no auth) |
| V3 Session Management | no | — |
| V4 Access Control | no | — |
| V5 Input Validation | yes (file paths) | Same `validateProjectRelativePath` pattern from `ProjectContext` |
| V6 Cryptography | no | — |

### Known Threat Patterns for JavaFX file editor

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Path traversal via crafted file open request | Tampering | `Path.toAbsolutePath().normalize()` + verify inside project root before reading; `openOrFocusTab` receives paths from `FileTreeController` (project-scoped) so risk is LOW |
| Writing outside project root | Tampering | Save only writes back to `editorTab.path`, which was set from the original open path — no user input changes the save target |
| Large file load freezing UI thread | Denial of Service | `Files.readString` is synchronous; files in scope (XSLT/XSL-FO templates) are typically < 1 MB — acceptable. Flag in docs if very large files encountered. |

---

## Sources

### Primary (HIGH confidence)
- Context7 `/fxmisc/richtextfx` — UndoManager dirty detection, VirtualizedScrollPane, keyboard shortcuts via WellBehavedFX InputMap, CodeArea setup
- `build.gradle` (project) — verified RichTextFX 0.11.5 already on classpath
- `src/main/java/ch/ti/gagi/xsleditor/ui/FileTreeController.java` — integration seam `setOnFileOpenRequest` already present
- `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` — `editorPane` StackPane fx:id, `setDirty()` scaffold
- Maven Central (via WebFetch) — confirmed 0.11.5 is current latest [VERIFIED: Maven Central]

### Secondary (MEDIUM confidence)
- JavaFX 21 `Tab.setOnCloseRequest` / `Tab.setOnClosed` — standard JavaFX API, behavior consistent with JavaFX 8+ contract [ASSUMED from training; verified as low-risk given API stability]

### Tertiary (LOW confidence)
- None.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries already in `build.gradle`; RichTextFX API verified via Context7
- Architecture: HIGH — mirrors existing Phase 3 sub-controller pattern exactly; seam already coded
- Pitfalls: HIGH — `mark()`/`forgetHistory()` order and `setOnClosed` vs `setOnCloseRequest` verified via Context7 code examples

**Research date:** 2026-04-18
**Valid until:** 2026-07-18 (RichTextFX 0.11.x is stable; JavaFX 21 LTS)
