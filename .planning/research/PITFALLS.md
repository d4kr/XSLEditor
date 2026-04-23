# Domain Pitfalls — v0.3.0 Polish & Usability

**Project:** XSLEditor
**Researched:** 2026-04-23
**Scope:** JavaFX dark theme CSS, RichTextFX CodeArea styling, TableView layout,
encoding, version.properties loading, app icon wiring.

---

## 1. Dark Theme CSS — RichTextFX CodeArea

### Pitfall 1.1: CodeArea background stays white despite CSS rules on `.code-area`
**What goes wrong:** You apply `-fx-background-color` to `.code-area` in the
stylesheet. The editor background stays white. The rule is correct but targets
the wrong node.

**Why it happens:** RichTextFX `CodeArea` is a compound control. Its visible
background is rendered by an inner `VirtualFlow` (or `.content` sub-node), not
the CodeArea node itself. JavaFX CSS specificity rules mean the default Caspian
or Modena style for those sub-nodes wins over a rule on `.code-area`.

**Prevention:** Target the internal sub-structure explicitly:
```css
.code-area .content {
    -fx-background-color: #1e1e1e;
}
```
Also set the `VirtualizedScrollPane` background, because scrolling exposes
its viewport:
```css
.virtualized-scroll-pane {
    -fx-background-color: #1e1e1e;
}
.virtualized-scroll-pane > .scroll-pane {
    -fx-background-color: #1e1e1e;
}
```

**Detection:** Right-click → Inspect (Scenic View, or log `scene.lookup()`)
to find which node is actually painted white.

---

### Pitfall 1.2: Caret and selected-text colors remain from Modena (invisible or
wrong)
**What goes wrong:** Text is dark, caret is dark, selection highlight is dark
— the user cannot see the cursor or selected text.

**Why it happens:** JavaFX Modena sets caret and text selection colors on
`.text-input` and `.text-area`. RichTextFX does not inherit these rules
automatically.

**Prevention:** Add explicit caret and selection rules:
```css
.code-area .caret {
    -fx-stroke: #aeafad;
}
.code-area .selection {
    -fx-fill: rgba(14, 99, 156, 0.6);
}
```

---

### Pitfall 1.3: RichTextFX syntax-highlight spans use `-fx-fill`, not
`-fx-text-fill`
**What goes wrong:** You add `.xml-element { -fx-text-fill: #4ec9b0; }` and
the color does not apply.

**Why it happens:** RichTextFX `StyleSpans` render each span as a `Text` node.
`Text` inherits from `Shape`, not from `Control`, so the CSS property for color
is `-fx-fill`, not `-fx-text-fill`. The current `main.css` already uses
`-fx-fill` correctly (line 94+), but any new span classes added during this
milestone must follow the same pattern.

**Prevention:** Always use `-fx-fill` for span classes. Never use
`-fx-text-fill` on RichTextFX span selectors.

---

### Pitfall 1.4: TreeView text becomes invisible when `.tree-cell:selected` lacks
explicit `-fx-text-fill`
**What goes wrong:** A selected tree cell shows black text on a dark background
after a CSS change — the item disappears visually.

**Why it happens:** JavaFX Modena sets selected cell text to a light color
only when `-fx-accent` is in play. When you override the selected state
background without also overriding text fill, Modena's default black text
bleeds through.

**Current code risk:** `main.css` line 76-79 sets background for
`.file-tree-view .tree-cell:selected` but the rule does not override
`-fx-text-fill`. If Modena's selected-text override fires differently on a
target machine, text may go dark.

**Prevention:** Always pair a selected-state background override with an
explicit `-fx-text-fill`:
```css
.file-tree-view .tree-cell:selected {
    -fx-background-color: #1e1e1e;
    -fx-text-fill: #cccccc;   /* <- must be present */
    -fx-border-color: #555555;
    -fx-border-width: 0 0 0 2;
}
```

---

### Pitfall 1.5: TableView header text invisible against dark header background
**What goes wrong:** `.log-table-view .column-header` has a dark background,
but the column header text is black (default Modena) so it is invisible.

**Why it happens:** The `.column-header .label` sub-node needs its own
`-fx-text-fill` rule; the parent `.column-header` rule does not cascade text
color into the label child.

**Current code risk:** `main.css` lines 138-143 set `-fx-text-fill` on
`.column-header` but NOT on `.column-header .label`. Some JavaFX versions
apply this to the label; others do not.

**Prevention:** Be explicit — target both nodes:
```css
.log-table-view .column-header .label {
    -fx-text-fill: #888888;
    -fx-font-size: 11px;
}
```

---

### Pitfall 1.6: Applying `getStylesheets().add()` after the scene is shown
does not retroactively fix previously rendered controls
**What goes wrong:** CSS added at runtime refreshes current node styles but
already-constructed cells (pooled by VirtualFlow) retain old paint state until
they cycle.

**Why it happens:** JavaFX cell virtualization reuses cells. Programmatic CSS
refresh (`Node.applyCss()`) is needed to force re-evaluation of pooled cells.

**Prevention:** Load the stylesheet at scene construction time in FXML
(already done via `stylesheets="@main.css"` in `main.fxml`). Do not add dark
theme rules via `getStylesheets().add()` at runtime — embed them in the
existing `main.css`.

---

## 2. TableView Column Width — Log Panel

### Pitfall 2.1: `CONSTRAINED_RESIZE_POLICY` hides content when a column uses
`prefWidth` without `minWidth`
**What goes wrong:** You switch the `logTableView` to
`TableView.CONSTRAINED_RESIZE_POLICY`. The Message column shrinks below
readable width. Or: the columns do not add up to 100%, leaving an empty
filler column at the right.

**Why it happens:** `CONSTRAINED_RESIZE_POLICY` distributes available space
proportionally, but if columns specify `prefWidth` without `minWidth`, the
policy can squeeze them to zero. The filler column is JavaFX's own artifact
when total column width < table width; it appears as an empty header cell.

**Current code risk:** `main.fxml` lines 108-113 specify fixed `prefWidth`
values (65, 60, 100, 400, 40) with no `minWidth`. If the table is narrower
than 665 px, `CONSTRAINED_RESIZE_POLICY` will compress all columns equally,
causing the Time/Level columns to disappear.

**Prevention:** Set `minWidth` per column (e.g. 40 for Time, 50 for Level,
60 for Type, 100 for Message, 36 for AI). Use
`TableView.CONSTRAINED_RESIZE_POLICY` only when you intend all columns to
share space. For a "fill width, remove trailing filler" requirement, the
correct policy is `TableView.CONSTRAINED_RESIZE_POLICY` combined with
explicit `minWidth` guards — or keep `prefWidth`-based sizing and set the
`columnResizePolicy` only on the Message column to expand via `HBox.hgrow`.

**Alternative — explicit column binding in Java:**
```java
colMessage.prefWidthProperty().bind(
    logTableView.widthProperty()
        .subtract(colTime.getWidth())
        .subtract(colLevel.getWidth())
        .subtract(colType.getWidth())
        .subtract(colAi.getWidth())
        .subtract(2) // border
);
```
This is fragile when the table is resized before layout completes. Prefer
`CONSTRAINED_RESIZE_POLICY` + `minWidth` instead.

---

### Pitfall 2.2: The "extra blank column" is the JavaFX filler column, not a
real column
**What goes wrong:** A blank column appears at the right of the TableView with
no header and no data. Attempts to remove it by changing column definitions
have no effect.

**Why it happens:** JavaFX automatically adds a filler `TableColumn` when the
declared columns do not fill the table's full width. It is not in the FXML
columns list and cannot be targeted by normal column removal.

**Prevention:** Use `TableView.CONSTRAINED_RESIZE_POLICY` so JavaFX expands
declared columns to fill available width, eliminating the filler. There is no
direct CSS class for the filler column; the only reliable fix is the resize
policy.

---

### Pitfall 2.3: Changing `columnResizePolicy` in FXML vs. Java — ordering
matters
**What goes wrong:** Setting `columnResizePolicy` in FXML before columns are
fully initialized produces a layout glitch on first render. Column widths flash
and then settle.

**Prevention:** Set the policy in the controller's `initialize()` method after
the FXML injection is complete, not in the FXML file.

---

## 3. Encoding

### Pitfall 3.1: `Files.readString` without explicit charset on non-UTF-8 files
**What goes wrong:** A file saved on Windows with UTF-16 or Latin-1 encoding is
read by `Files.readString(path, StandardCharsets.UTF_8)` and produces garbled
characters or throws `MalformedInputException`.

**Why it happens:** `StandardCharsets.UTF_8` decoding fails on non-UTF-8 bytes
rather than substituting replacement characters (unlike e.g. `ISO-8859-1`).
The current codebase always specifies UTF-8 (EditorController line 136,
ProjectFileManager line 12, LibraryPreprocessor line 49) — this is correct for
files created by the tool, but may fail for files imported from external
sources.

**Prevention:** The tool's own pipeline consistently writes UTF-8 (`Files.writeString` with UTF-8,
`StandardCharsets.UTF_8` in `RenderEngine`). For the encoding fix in v0.3.0,
identify where characters are *displayed* incorrectly — not just read. The most
likely sources are:

1. **Saxon XSLT output:** `RenderEngine.transformToString` already sets
   `Serializer.Property.ENCODING` to `UTF-8`. If the XSL-FO output contains a
   declaration like `<?xml version="1.0" encoding="ISO-8859-1"?>`, Saxon will
   re-encode to the declared charset, producing bytes that `StringWriter`
   interprets wrongly. Prevention: strip or override the encoding declaration,
   or read the `Serializer` output as bytes and then decode explicitly.

2. **FOP rendering pass:** `RenderEngine.renderFoToPdf` converts the FO string
   to bytes with `getBytes(StandardCharsets.UTF_8)` and feeds them into a
   `ByteArrayInputStream`. If the XSL-FO string was produced with non-UTF-8
   content (e.g. Windows-1252 entities like `&#x92;`) and the PDF font does not
   embed those glyphs, the rendered PDF will show boxes or wrong characters.
   This is a font embedding issue, not a Java encoding bug.

3. **File read before display in editor:** `EditorController.openOrFocusTab`
   calls `Files.readString(key, StandardCharsets.UTF_8)`. If the file has a
   BOM (Byte Order Mark, 0xEF 0xBB 0xBF), `Files.readString` with UTF-8 does
   NOT strip the BOM — the BOM character (`﻿`) appears as the first
   character in the editor. Prevention: strip the BOM after reading.

**Detection:** Print `bytes[0], bytes[1], bytes[2]` of the raw file read to
detect a UTF-8 BOM (EF BB BF). Log `foContent.substring(0, 80)` before FOP
to detect encoding mismatches at the FO boundary.

---

### Pitfall 3.2: Hardcoded byte conversion loses encoding consistency
**What goes wrong:** Calling `string.getBytes()` without a charset argument
uses the JVM default charset, which is platform-dependent and may differ between
macOS (UTF-8) and Windows (Cp1252).

**Current code risk:** None found — the codebase consistently uses
`StandardCharsets.UTF_8`. But any new code added in this milestone that calls
`.getBytes()` without a charset argument reintroduces the bug.

**Prevention:** In code review, treat bare `.getBytes()` as a compile-error
equivalent. Always write `.getBytes(StandardCharsets.UTF_8)`.

---

## 4. About Dialog — Version Loading

### Pitfall 4.1: `getResourceAsStream("/version.properties")` returns null in
the fat JAR if the resource is not at the classpath root
**What goes wrong:** The dialog shows "unknown" for the version despite
`version.properties` existing.

**Why it happens:** The `processResources` block in `build.gradle` expands
`version.properties` into `build/resources/main/`, making it available at
the classpath root in dev mode. In the fat JAR (shadow plugin), it is included
at `/version.properties`. The current `AboutDialog.loadVersion()` uses
`getClass().getResourceAsStream("/version.properties")` with a leading slash
— this is the correct form for classpath-root resources and works in both
dev and fat-JAR modes.

**Risk remaining:** If the Gradle `version` property is updated in `build.gradle`
but a stale `build/` directory is not cleaned, the expanded file may contain the
old version string. `./gradlew clean shadowJar` is required after any version
bump.

**Prevention:** Confirm the expanded value by running:
```
jar tf build/libs/xsleditor-*.jar | grep version.properties
jar xf build/libs/xsleditor-*.jar version.properties && cat version.properties
```
Add a note in the project's build instructions: "always run `clean` before
release builds."

---

### Pitfall 4.2: `build.gradle` version string not propagated to the JAR filename
**What goes wrong:** `build.gradle` line 13 declares `version = '0.1.0'` but
this value has not been updated to `0.3.0` for this milestone. The fat JAR
will embed the old version string.

**Why it happens:** The `processResources` block injects `${version}` from
`project.version`, which reads the `version` property in `build.gradle`. No
other mechanism updates this value.

**Prevention:** At the start of the v0.3.0 milestone, update `version` in
`build.gradle` to `'0.3.0'`. This is the single source of truth. Do not
hardcode the version string in any Java source file.

---

## 5. App Icon — Resource Loading

### Pitfall 5.1: `Image` constructed from a resource URL silently fails if the
resource path is wrong
**What goes wrong:** The stage shows the default OS window icon instead of the
app icon. No exception is thrown. The app runs normally.

**Why it happens:** `new Image(url)` with `backgroundLoading=false` (the
default for the string constructor) does not throw on a 404 — it creates an
`Image` in an error state. `stage.getIcons().add(errorStateImage)` adds it
silently; JavaFX falls back to the default icon.

**Prevention:** After loading the image, check for errors:
```java
URL iconUrl = getClass().getResource("/icon.png");
if (iconUrl != null) {
    Image icon = new Image(iconUrl.toExternalForm());
    if (!icon.isError()) {
        primaryStage.getIcons().add(icon);
    } else {
        System.err.println("[XSLEditorApp] icon.png failed to load");
    }
}
```
Always log the failure path so it is visible during testing.

---

### Pitfall 5.2: Icon placed in the wrong resource directory is not found in
the fat JAR
**What goes wrong:** `icon.png` placed under
`src/main/resources/ch/ti/gagi/xsleditor/ui/` is accessible via
`getClass().getResource("icon.png")` (relative) but NOT via
`getClass().getResource("/icon.png")` (classpath root). Conversely, if the
intent is to load it from the class root, it must be in
`src/main/resources/` directly.

**Why it happens:** The leading `/` in `getResource("/icon.png")` means
"relative to the classpath root". Without the slash, it is relative to the
calling class's package. These resolve to different paths.

**Prevention:** Decide on one convention:
- Place `icon.png` at `src/main/resources/icon.png` and load with
  `getClass().getResource("/icon.png")`.
- Or place it at `src/main/resources/ch/ti/gagi/xsleditor/ui/icon.png` and
  load with `getClass().getResource("icon.png")` (no leading slash) from
  `XSLEditorApp`.

The milestone context says "icon.png → src/main/resources/" — this matches
the first convention. Use `getClass().getResource("/icon.png")` in
`XSLEditorApp.start()`.

---

### Pitfall 5.3: `stage.getIcons()` must be populated before `stage.show()`
**What goes wrong:** Icon is added after `primaryStage.show()`. On macOS,
the Dock icon updates mid-launch with a visible flicker. On Windows, the
taskbar icon may not update until the window is repainted.

**Prevention:** Add the icon to `primaryStage.getIcons()` before the
`primaryStage.show()` call. The current `XSLEditorApp.start()` calls
`primaryStage.show()` at line 49 — icon wiring must precede this line.

---

## 6. General JavaFX Dark Theme Pitfalls

### Pitfall 6.1: `Alert` and `Dialog` panes do not inherit the scene stylesheet
**What goes wrong:** All app dialogs look dark, but any new `Alert` created in
the fix shows with Modena light theme.

**Why it happens:** `Alert` and `Dialog` open in their own sub-stage with their
own scene. The parent scene's stylesheet is not inherited.

**Current code risk:** `AboutDialog` uses inline `.setStyle()` calls for
individual nodes (lines 49, 55, etc.) — correct approach, no inheritance
assumed. But any `Alert` dialogs opened by `EditorController.showError()`
(line 103) or by new code in this milestone will use Modena light by default.

**Prevention:** To style `Alert` dialogs, add the stylesheet programmatically:
```java
alert.getDialogPane().getStylesheets().add(
    getClass().getResource("/ch/ti/gagi/xsleditor/ui/main.css").toExternalForm()
);
```
Or use inline styles for critical elements.

---

### Pitfall 6.2: CSS pseudo-class specificity overrides application styles
**What goes wrong:** You add a dark-background rule to `.table-row-cell` but
selected rows or focused rows revert to Modena blue because the Modena
`:selected:focused` rule has higher specificity.

**Prevention:** Always add `:selected` and `:focused` pseudo-class rules
alongside any row/cell background change:
```css
.log-table-view .table-row-cell:selected,
.log-table-view .table-row-cell:selected:focused {
    -fx-background-color: #094771;
}
.log-table-view .table-row-cell:selected .table-cell {
    -fx-text-fill: #ffffff;
}
```

---

## Phase-Specific Warnings

| Milestone Task | Pitfall to Watch | Mitigation |
|----------------|-----------------|------------|
| Dark theme CSS for CodeArea | Background targets wrong sub-node (1.1) | Target `.code-area .content`, not `.code-area` |
| Dark theme CSS for CodeArea | Caret/selection invisible (1.2) | Add explicit `.caret` and `.selection` rules |
| Dark theme CSS for CodeArea | New span classes use wrong property (1.3) | Use `-fx-fill`, not `-fx-text-fill` |
| Dark theme CSS for TreeView | Selected cell text invisible (1.4) | Add `-fx-text-fill` to every selected-state rule |
| Dark theme CSS for TableView | Column header text invisible (1.5) | Target `.column-header .label` explicitly |
| Log panel column width fix | Filler column appears (2.2) | Use `CONSTRAINED_RESIZE_POLICY` |
| Log panel column width fix | Columns compress below readable width (2.1) | Set `minWidth` per column |
| Encoding fix | BOM character appears in editor (3.1) | Strip `﻿` after `Files.readString` |
| Encoding fix | Saxon encoding declaration mismatch (3.1) | Override or strip XSL-FO encoding declaration |
| About dialog version | Stale build cache shows old version (4.1) | Run `clean` before release |
| About dialog version | `version` in `build.gradle` not updated (4.2) | Bump `version` property before building |
| App icon | Silent load failure, no icon shown (5.1) | Check `icon.isError()` after construction |
| App icon | Wrong resource path convention (5.2) | Confirm leading-slash rule matches placement |
| App icon | Added after `show()`, causes flicker (5.3) | Wire icon before `primaryStage.show()` |
| Any new Alert/Dialog | Light theme bleeds in (6.1) | Add stylesheet to `getDialogPane()` explicitly |
