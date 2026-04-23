# Technology Stack — v0.3.0 Polish & Usability

**Project:** XSLEditor
**Milestone:** v0.3.0 Polish & Usability
**Researched:** 2026-04-23
**Mode:** Existing app — gaps and fixes only

No new libraries required. All four feature areas are solvable with the existing stack (JavaFX 21, RichTextFX 0.11.5, Jackson 2.17.2, version.properties). The issues are CSS omissions, a single missing FXML attribute, and no encoding bug in the pipeline code itself.

---

## Feature Area 1: Dark Theme — Editor (CodeArea) Text Visibility

### Problem

`main.css` has no `.code-area` or `.styled-text-area` selectors. RichTextFX ships with a minimal internal CSS (`code-area.css`, `styled-text-area.css`) that sets only font-family, cursor, caret blink rate, and line spacing — no background, no text fill. JavaFX's Modena theme (the default) supplies a white/near-white background and near-black text for text input controls. Because CodeArea extends `StyledTextArea`, Modena's input-field background (#ffffff or system default) wins unless overridden.

Result: the CodeArea renders as a white rectangle inside the dark shell.

### Fix — CSS only, no Java changes

Add to `main.css`:

```css
/* RichTextFX CodeArea — dark theme override */
.code-area {
    -fx-background-color: #1e1e1e;
}

.code-area .styled-text-area {
    -fx-background-color: #1e1e1e;
}

/* Default text fill for unstyled/plain text segments */
.code-area .styled-text-area .text {
    -fx-fill: #d4d4d4;
}

/* Caret color */
.code-area .styled-text-area .caret {
    -fx-stroke: #aeafad;
}

/* Selection highlight */
.code-area .styled-text-area .selection {
    -fx-fill: rgba(38, 79, 120, 0.8);
}

/* Line number gutter (if LineNumberFactory is used — currently not, but safe to include) */
.lineno {
    -fx-background-color: #252526;
    -fx-text-fill: #5a5a5a;
    -fx-padding: 0 4 0 4;
}
```

**Confidence: HIGH.** Verified against RichTextFX 0.11.5 bundled CSS (extracted from JAR). These are the exact selectors used by the library's own demo dark-theme stylesheets.

**Why `.code-area .styled-text-area .text` not `.code-area .text`:** The paragraph text nodes sit inside `.styled-text-area`'s paragraph boxes. The `.text` selector targets JavaFX `Text` nodes, which use `-fx-fill` (not `-fx-text-fill`). This is the RichTextFX-specific distinction — regular Label/TextField nodes use `-fx-text-fill`, but the internal text flow uses `-fx-fill`.

**Integration note:** The existing syntax highlight CSS classes (`.xml-element`, `.xml-attribute`, etc.) already use `-fx-fill`. They will continue to work correctly. The `.text` default only applies to segments that carry no style class (plain/unstyled text).

---

## Feature Area 2: Dark Theme — TreeView and TableView Text Visibility

### Problem analysis

The existing `main.css` already has correct rules for:
- `.file-tree-view .tree-cell` (`-fx-text-fill: #cccccc`)
- `.file-tree-view .tree-cell.entrypoint` (green)
- `.file-tree-view .tree-cell.xml-input` (blue)
- `.log-table-view .table-cell` (`-fx-text-fill: #cccccc`)
- `.log-table-view .column-header` + `.column-header-background`

These look correct on paper. If text is still invisible, the cause is Modena's selected-row override winning over the custom cell background. The fix is to also define the `:selected` pseudo-class text fill explicitly.

Check for these missing rules and add if absent:

```css
/* Ensure selected table row does not revert text to dark */
.log-table-view .table-row-cell:selected .table-cell {
    -fx-text-fill: #ffffff;
    -fx-background-color: #094771;
}

/* TreeView selected row text stays readable */
.file-tree-view .tree-cell:selected {
    -fx-text-fill: #ffffff;
}
```

**The existing `.file-tree-view .tree-cell:selected` rule sets background and border but no `-fx-text-fill`, so selected items get Modena's default selected-row text color (which may be dark on dark).**

**Confidence: HIGH** — standard JavaFX CSS dark-theme fix; documented behavior of Modena overriding text-fill on selected rows.

---

## Feature Area 3: Log Panel — Full-Width Columns, Remove Extra Column

### Problem

The FXML defines 5 columns: `colTime` (65px), `colLevel` (60px), `colType` (100px), `colMessage` (400px), `colAi` (40px). Fixed `prefWidth` values with no `columnResizePolicy` attribute on the `TableView` means JavaFX uses `UNCONSTRAINED_RESIZE_POLICY` by default — columns do not fill the available width, leaving empty gray space on the right.

`colType` carries the error category string (e.g. "XSLT_COMPILE"), which is redundant because the message already implies the type. "Remove extra column" means `colType`.

### Fix — Two-part

**Part A: Remove `colType`**

In `main.fxml`: delete the `<TableColumn fx:id="colType" .../>` element.

In `MainController.java`: remove the `@FXML private TableColumn<LogEntry, String> colType;` field declaration and the `colType` argument from the `logController.initialize(...)` call.

In `LogController.java`: remove the `colType` parameter from `initialize(...)`, its `Objects.requireNonNull` call, and the `colType.setCellValueFactory(...)` line.

**Part B: Full-width column policy**

In `main.fxml`, add `columnResizePolicy` to the `TableView` element:

```xml
<TableView fx:id="logTableView" prefHeight="120" VBox.vgrow="ALWAYS"
           columnResizePolicy="CONSTRAINED_RESIZE_POLICY">
```

`TableView.CONSTRAINED_RESIZE_POLICY` distributes excess width across all columns proportionally. With 4 columns remaining (Time 65, Level 60, Message flexible, AI 40), `colMessage` will expand to fill all remaining space.

**Confidence: HIGH** — `CONSTRAINED_RESIZE_POLICY` is a standard JavaFX constant. FXML accepts it as a string literal; JavaFX FXML loader resolves it to `TableView.CONSTRAINED_RESIZE_POLICY` automatically.

**Important caveat:** `CONSTRAINED_RESIZE_POLICY` prevents the user from resizing individual columns below the total width. If resize-by-drag is needed, use `columnResizePolicy="UNCONSTRAINED_RESIZE_POLICY"` and instead set `colMessage`'s `maxWidth` to `Infinity` via `HBox.hgrow="ALWAYS"` equivalent — but for a log panel this is not required.

---

## Feature Area 4: About Dialog — Version Auto-Update

### Problem assessment

**This is already implemented correctly.** The `version.properties` file at `src/main/resources/version.properties` contains `version=${version}`. Gradle's `processResources` block expands this at build time using `project.version` from `build.gradle`. `AboutDialog.loadVersion()` reads it via `getClass().getResourceAsStream("/version.properties")`.

The only issue: `build.gradle` has `version = '0.1.0'` hardcoded. This version string needs to be updated to `'0.3.0'` as part of the milestone. The mechanism itself is complete and correct — no code changes needed.

**What to verify manually:** Run `./gradlew processResources` and confirm `build/resources/main/version.properties` contains `version=0.3.0` (not `${version}` literally). If the About dialog shows `${version}` literally, it means the resource filter is not running before the JAR is built — the fix is `./gradlew clean shadowJar` to force re-processing.

**Confidence: HIGH** — mechanism verified in codebase. `build/resources/main/version.properties` already exists from a prior build.

---

## Feature Area 5: Encoding Issues

### Problem assessment

The pipeline code is correct. A grep across all Java source shows:
- `EditorController`: `Files.readString(path, StandardCharsets.UTF_8)` and `Files.writeString(path, text, StandardCharsets.UTF_8)`
- `RenderOrchestrator`: `Files.readString(entryPath, StandardCharsets.UTF_8)`
- `LibraryPreprocessor`: `Files.readString(file, StandardCharsets.UTF_8)`
- `RenderEngine`: `foContent.getBytes(StandardCharsets.UTF_8)`, serializer encoding set to "UTF-8"

No implicit platform-default charset calls exist. No `new String(bytes)` without charset, no `FileReader` without charset.

**The encoding problem is most likely in the XSL-FO template itself, not the Java code.** Common causes:
1. The XSLT stylesheet omits `<xsl:output encoding="UTF-8"/>`, causing Saxon to default to UTF-16 for some output methods.
2. The XSL-FO document lacks `<?xml version="1.0" encoding="UTF-8"?>` declaration, causing FOP's SAX parser to interpret it differently.
3. The XML input file contains non-ASCII characters (e.g. accented Italian words like "è", "à") and the XSLT passes them through as-is. FOP renders them correctly if FOP's font configuration includes the glyph; if not, the glyph appears as a box or is dropped — this is a FOP font issue, not a Java encoding issue.

**The one potential Java-level gap:** `RenderEngine.transformToString()` uses a `StringWriter` as the Saxon serializer output. The serializer has `ENCODING=UTF-8` set, but `StringWriter` is an in-memory character stream — UTF-8 encoding/decoding is irrelevant when the result is a Java `String`. However, `foContent.getBytes(StandardCharsets.UTF_8)` in `renderFoToPdf()` re-encodes correctly for the `ByteArrayInputStream` fed to FOP. This chain is correct.

**Recommended investigation approach (no code changes yet):**
1. Identify which specific character(s) are mangled and at which stage (post-XSLT? post-FOP?).
2. Check the XSLT template for `<xsl:output method="xml" encoding="UTF-8" indent="yes"/>`.
3. Check FOP font configuration for the required glyphs.

**If a Java fix is needed**, the only realistic gap is if `StreamSource(xmlFile.toFile())` triggers platform-default encoding in some edge case. The robust fix:

```java
// In RenderEngine.transformToString(), replace:
transformer.setSource(new StreamSource(xmlFile.toFile()));
// With:
transformer.setSource(new StreamSource(
    new java.io.InputStreamReader(
        new java.io.FileInputStream(xmlFile.toFile()),
        StandardCharsets.UTF_8)));
```

But this is a speculative fix — Saxon resolves external entity URIs from the file path and typically handles encoding via the XML declaration. Only apply if the investigation confirms the source.

**Confidence: MEDIUM** — pipeline code analysis is HIGH confidence (no implicit charset calls), but root cause of the reported encoding issue is unconfirmed without a reproducer.

---

## No New Dependencies Needed

| Library | Current Version | Status |
|---------|----------------|--------|
| JavaFX | 21 | No change — all required APIs present |
| RichTextFX | 0.11.5 | No change — CSS override is the fix |
| Apache FOP | 2.9 | No change |
| Saxon-HE | 12.4 | No change |
| Jackson | 2.17.2 | No change |
| PDFBox | 2.0.31 | No change |

---

## Key JavaFX APIs for Implementer Reference

| API | Location | Purpose |
|-----|----------|---------|
| `-fx-fill` on `.text` | CSS | RichTextFX text node color (NOT `-fx-text-fill`) |
| `-fx-background-color` on `.code-area` | CSS | Editor background |
| `TableView.CONSTRAINED_RESIZE_POLICY` | FXML attribute | Full-width column distribution |
| `getClass().getResourceAsStream("/version.properties")` | AboutDialog | Already implemented |
| `TableView.getColumns().remove(col)` | Java API | Alternative to FXML removal (not recommended — use FXML) |

---

## Sources

- RichTextFX 0.11.5 bundled CSS extracted from JAR: `org/fxmisc/richtext/code-area.css`, `styled-text-area.css` — HIGH confidence
- JavaFX 21 CSS Reference Guide (Modena defaults for input controls) — HIGH confidence
- Codebase inspection: `main.css`, `main.fxml`, `LogController.java`, `AboutDialog.java`, `EditorTab.java`, `RenderEngine.java`, `RenderOrchestrator.java` — HIGH confidence
