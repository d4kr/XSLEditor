# Feature Landscape: v0.3.0 Polish & Usability

**Domain:** JavaFX desktop developer tool (XML/XSLT editor + PDF pipeline)
**Researched:** 2026-04-23
**Source:** Codebase inspection + JavaFX CSS/layout conventions (MEDIUM confidence)

---

## Feature Group 1: Dark Theme Text Visibility

### What the bug is

JavaFX's default Modena theme sets control backgrounds to light grays and text to near-black.
`main.css` overrides backgrounds for named containers (`.log-table-view`, `.file-tree-view`,
`.placeholder-pane`) but does NOT set a global dark background or override the Modena defaults
for TabPane, Tab headers, the scene root, the ToolBar, or MenuBar. As a result:

- `CodeArea` (RichTextFX) uses Modena's default white background with black caret — invisible
  on the surrounding dark chrome.
- `TabPane` tab headers get Modena's light gray; tab labels are dark text on light — correct
  by accident but visually inconsistent.
- `ToolBar` and `MenuBar` get Modena's light-gray skin — bright strip against the dark content
  area below.
- `TableView` row text for INFO entries falls back to Modena's near-black — invisible on
  `#1e1e1e` cell background. Only ERROR/WARN rows get explicit color overrides; INFO has none.
- `TreeView` selected-cell text has `tree-cell:selected` with no `-fx-text-fill`, so Modena
  may invert it to white-on-white or dark-on-dark depending on platform.

### Table stakes behavior (expected for a dark-theme desktop tool)

| Element | Expected |
|---------|----------|
| Scene/root background | Dark (#1e1e1e or #2b2b2b) |
| CodeArea (RichTextFX) | Dark background, light caret, light default text |
| TabPane tab headers | Dark background, readable tab labels |
| ToolBar background | Dark, matching menu bar |
| MenuBar background | Dark |
| TableView INFO rows | Readable text (e.g. #cccccc) on dark cell background |
| TreeView selected cell | Text remains visible (no inversion to white-on-white) |
| Dialog panes (About) | Already dark via inline style — this is fine |

### How dark theme is done correctly in JavaFX

JavaFX 21 ships with `Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA)` as
the default. There are two correct approaches to a full dark theme:

**Option A — CSS root override (recommended for this project).**
Target `.root` in `main.css` with `-fx-base`, `-fx-background`, `-fx-control-inner-background`
and `-fx-text-base-color`. These four Modena-derived lookups cascade into almost every built-in
control automatically. Then add targeted overrides for RichTextFX `CodeArea` which does not
participate in Modena's lookup chain.

```css
.root {
    -fx-base: #3c3f41;
    -fx-background: #1e1e1e;
    -fx-control-inner-background: #2b2b2b;
    -fx-text-base-color: #cccccc;
}
```

The `CodeArea` needs explicit overrides because it is not a standard JavaFX control:
```css
.code-area {
    -fx-background-color: #1e1e1e;
}
.code-area .content {
    -fx-background-color: #1e1e1e;
}
.code-area .caret {
    -fx-stroke: #cccccc;
}
.code-area .text {
    -fx-fill: #cccccc;
}
```

**Option B — AtlantaFX or custom user-agent stylesheet.** Replace Modena entirely with a
third-party dark stylesheet JAR. Not appropriate here because it would add a dependency and
risk breaking the existing class-based CSS that works for log colors, tree colors, and
preview banner.

**Verdict: Option A.** Add `.root` overrides to `main.css`. The stylesheet is already linked
via FXML; the `.root` selector applies to the Scene root `BorderPane` and cascades outward.
All existing named-class overrides continue to win because CSS specificity of a named class
`.log-table-view` beats the type selector cascade from `.root`.

### Complexity

**Low.** CSS-only changes to `main.css`. No Java changes. No layout changes. Risk: Modena
lookup cascades for some controls (e.g. buttons, progress bars) may need additional targeted
fixes after seeing the result — but the pattern is well-known and incremental.

### Dependencies

- `main.css` (already loaded via `stylesheets="@main.css"` in `main.fxml`)
- RichTextFX `CodeArea` CSS classes: `.code-area`, `.code-area .content`, `.code-area .caret`,
  `.code-area .text` — these are the documented RichTextFX CSS hooks.

---

## Feature Group 2: Log Panel Full-Width Layout

### What the bug is

The `TableView` in the log panel has five `TableColumn` elements with hardcoded `prefWidth`
values summing to 665 px (65+60+100+400+40). The `TableView` itself has no column resize
policy set, so Modena defaults to `UNCONSTRAINED_RESIZE_POLICY`. This means:

1. Columns do not grow with window width — a blank scrollable area appears to the right.
2. JavaFX renders an **extra empty trailing column** header at the right of every TableView
   that uses UNCONSTRAINED policy — this is the visual "extra column" reported.
3. The `VBox` containing the filter bar + table has no `prefWidth` binding to the window
   either, so the outer container may not be stretching to `BorderPane.bottom` full width
   as expected.

### Table stakes behavior

| Element | Expected |
|---------|----------|
| TableView | Fills the full width of the log panel on any window size |
| Column headers | No blank trailing header column visible |
| Message column | Takes available space after fixed-width Time/Level/Type/AI columns |
| Filter bar | Full width, no horizontal gap |

### How full-width TableView works in JavaFX

The correct approach uses `TableView.CONSTRAINED_RESIZE_POLICY` and designates one
"flex" column with no `prefWidth` override, letting it absorb residual width.

In FXML:
```xml
<TableView fx:id="logTableView" prefHeight="120" VBox.vgrow="ALWAYS"
           columnResizePolicy="$TableView.CONSTRAINED_RESIZE_POLICY">
```

Or in Java (LogController.initialize):
```java
logTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
```

With `CONSTRAINED_RESIZE_POLICY`:
- The blank trailing column disappears.
- Columns cannot overflow the table width (horizontal scrollbar suppressed).
- Fixed columns (Time, Level, Type, AI) keep their `prefWidth` as a minimum hint.
- The Message column should have `prefWidth` removed or set high; it absorbs remaining space.

The filter bar `HBox` width issue: the `TitledPane`'s content `VBox` needs no change —
`BorderPane.bottom` already stretches children to full width by default. If the filter bar
has a gap it is likely because the `HBox` has `spacing="4"` but no `maxWidth="Infinity"`.
Setting `HBox.setMaxWidth(Double.MAX_VALUE)` or adding `maxWidth="Infinity"` in FXML closes it.

### Complexity

**Low.** One attribute change in `main.fxml` for the resize policy. Remove hardcoded
`prefWidth` from `colMessage` or let it remain as a hint only. The constraint policy treats
`prefWidth` as a relative weight, not an absolute pixel size — so fixed columns remain small
and the message column expands. No Java code changes needed if done in FXML.

### Dependencies

- `main.fxml` (column definitions)
- `LogController.initialize` if resize policy set there instead of FXML
- No changes to `LogEntry`, no changes to cell factories

---

## Feature Group 3: Character Encoding

### What the bug is

All file reads in the codebase use `StandardCharsets.UTF_8` explicitly:
- `EditorController`: `Files.readString(key, StandardCharsets.UTF_8)`
- `RenderOrchestrator`: `Files.readString(entryPath, StandardCharsets.UTF_8)`
- `LibraryPreprocessor`: `Files.readString(file, StandardCharsets.UTF_8)`
- `ProjectFileManager`: `Files.readString(absolutePath, StandardCharsets.UTF_8)`

The file read path is consistent and correct. However, the XSLT Saxon pipeline uses
`StringWriter` → `writer.toString()` → string returned. Saxon serializes the XSL-FO result
to a Java `String` object; Java strings are always Unicode internally. The XSL-FO string is
then encoded back to `byte[]` via `.getBytes(StandardCharsets.UTF_8)` before being fed to FOP.
This chain is internally consistent.

**Likely actual source of the encoding problem:** The display of characters in the `CodeArea`
(RichTextFX) or in the log panel `TableView`. Two candidate causes:

1. **System-locale-dependent behavior.** If the JVM is started without `-Dfile.encoding=UTF-8`
   (or `-Dstdout.encoding=UTF-8` in Java 17+) and the OS locale is not UTF-8, some
   platform-dependent Java APIs fall back to the system charset. The `Files.readString` calls
   all pin UTF-8 so this is unlikely to be the problem there.

2. **XML declaration encoding mismatch.** XSLT/XSL-FO files may declare `encoding="ISO-8859-1"`
   or `encoding="windows-1252"` in their `<?xml version="1.0" encoding="..."?>` declaration
   but actually be saved in UTF-8. Saxon honors the declared encoding when given a `StreamSource`
   from `File`; when given a `StringReader` (the path after LibraryPreprocessor), the declared
   encoding is ignored because string content is already decoded — this can expose mismatches.

3. **FOP font encoding for PDF.** Characters not in the embedded font's encoding appear as
   boxes or wrong glyphs in the PDF. This is an FOP/font issue, not a Java string issue.
   FOP requires explicit font configuration for non-ASCII characters in PDF output.

4. **PreviewController HTML rendering.** Pages are rendered as PNG via PDFBox, embedded as
   base64 in HTML. If PDF glyphs are missing (FOP font issue), the PNG will show boxes.
   This is a rendering artifact, not a Java string encoding bug.

### Expected behavior

| Stage | Expected |
|-------|----------|
| File open in editor | All UTF-8 characters (including accented, special) display correctly |
| XSLT compile/run | No garbled text in error messages |
| XSL-FO to PDF | Characters from the XSLT output appear correctly in PDF |
| Log panel messages | Error messages from Saxon/FOP display without garbling |

### Investigation needed to confirm root cause

Before implementing a fix, the exact symptom should be confirmed:
- Is it in the editor display (JavaFX rendering)?
- In error messages in the log panel?
- In the generated PDF (FOP font)?
- In a specific file type (XML input, XSLT, or both)?

### Likely fix

If the symptom is in the **editor display**: RichTextFX `CodeArea` renders Java Strings, which
are Unicode. If the file is genuinely UTF-8 and `Files.readString` with `UTF_8` is used, the
CodeArea will display it correctly. The issue may be a font: if the UI font does not contain
the glyph, JavaFX substitutes or omits it. Setting a font with good Unicode coverage (e.g.
system default or explicitly DejaVu, Noto) resolves this.

If the symptom is in **Saxon error messages**: Saxon error text is already a Java String;
no encoding issue. May be a platform locale issue with how macOS stdout handles the JVM.

If the symptom is in the **PDF output**: FOP requires an `fop.xconf` with `<fonts>` section
referencing fonts that cover the required character ranges. This is a configuration task, not
a code change.

### Complexity

**Medium.** Root cause diagnosis requires running the app and observing the symptom location.
Once confirmed, the fix is likely: (a) CSS font-family on CodeArea, or (b) FOP font config
file added to resources. The diagnosis step is the unknown cost.

### Dependencies

- `RenderEngine.java` if FOP font config is needed
- `main.css` if it is a UI font issue
- New `fop.xconf` resource file if FOP font embedding is the culprit

---

## Feature Group 4: About Version Auto-Update

### What the current state is

The `version.properties` file in `src/main/resources/` contains:
```
version=${version}
```

`build.gradle` has:
```groovy
processResources {
    filesMatching('version.properties') {
        expand(version: project.version)
    }
}
```

`AboutDialog.loadVersion()` reads this file from the classpath at runtime. This mechanism is
already implemented and correct — `project.version` is `'0.1.0'` in `build.gradle`, and the
Gradle `processResources` task expands the token at build time.

**The bug:** `build.gradle` still declares `version = '0.1.0'` — hardcoded. The actual
project version is v0.3.0 (current milestone). The mechanism works; the version string just
needs to be updated in `build.gradle` at each release.

### Table stakes behavior

The About dialog should show the version that matches the built artifact. The correct pattern
is:
1. `build.gradle`: `version = '0.3.0'` (single source of truth)
2. `processResources` expands it into `version.properties` at build time
3. `AboutDialog.loadVersion()` reads it — no code change needed

### Differentiator (nice to have)

If CI/CD ever creates releases, reading from a git tag at build time is possible via:
```groovy
version = System.getenv("VERSION") ?: "0.3.0-dev"
```
This is out of scope for v0.3.0.

### Complexity

**Trivial.** One line change: update `version = '0.1.0'` to `version = '0.3.0'` in
`build.gradle`. The machinery is already in place. No Java code changes.

### Dependencies

- `build.gradle` only

---

## Feature Group 5: App Icon Placement

### What the current state is

`icon.png` exists at the project root (`/icon.png`). It is not referenced anywhere in the
Java code — `XSLEditorApp.start()` does not call `primaryStage.getIcons().add(...)`.
There is no resources path for images under `src/main/resources/`.

### Table stakes behavior

For a JavaFX desktop app:
1. **Stage icon:** `primaryStage.getIcons().add(new Image(stream))` sets the window title bar
   icon and macOS Dock icon. JavaFX accepts multiple sizes for the `ObservableList<Image>`;
   the platform picks the best-fitting one.
2. **Resource location:** The image must be on the classpath at runtime (inside the fat JAR).
   The standard location is `src/main/resources/` with any subdirectory, e.g.
   `src/main/resources/ch/ti/gagi/xsleditor/icon.png`.
3. **README usage:** The icon can be referenced in `README.md` using a relative path from the
   repo root. If the icon moves to `src/main/resources/...`, the README path must update too.
   Alternatively, a copy stays at the project root for README and docs use; the resources
   copy is the one loaded at runtime.

### Expected implementation

```java
// In XSLEditorApp.start(), after scene creation:
try (InputStream iconStream = getClass()
        .getResourceAsStream("/ch/ti/gagi/xsleditor/icon.png")) {
    if (iconStream != null) {
        primaryStage.getIcons().add(new Image(iconStream));
    }
} catch (Exception ignored) {}
```

The `try-with-resources` + null check ensures the app starts even if the icon resource is
missing (e.g. during tests or if the resource path is wrong).

### Complexity

**Low.** Two steps:
1. Move/copy `icon.png` to `src/main/resources/ch/ti/gagi/xsleditor/icon.png`.
2. Add 5 lines to `XSLEditorApp.start()`.

No build changes needed — Gradle's `processResources` copies all non-Java files from
`src/main/resources` into the build classpath automatically.

### Dependencies

- `XSLEditorApp.java` (3–5 lines added)
- New resource path for `icon.png`
- `README.md` path reference (if README uses the icon)

---

## Feature Group 6: README Rewrite

### What the current state is

`README.md` is 18 lines. It contains: project name, one-line description, the pipeline
diagram, a purpose sentence, and a status line pointing to `docs/PRD.md`. It is a skeleton.

### Table stakes for a developer tool README

| Section | Why expected |
|---------|--------------|
| Project description (2-3 sentences) | Answers "what is this and who is it for" |
| Screenshot or pipeline diagram | Developers scan before reading |
| Requirements (Java version, OS) | Determines if they can run it |
| Build instructions | `./gradlew shadowJar` + how to run the JAR |
| How to open a project | Core user workflow in 3-4 steps |
| Feature list | What it does at a glance |
| App icon in header | Visual identity |

### Differentiators (nice to have but not blocking)

- Keyboard shortcut reference table
- Known limitations section
- Link to `docs/PRD.md` for full requirements (already exists)
- Contributor note (Claude Code attribution already in About dialog)

### Anti-features for this README

- Installation wizard / package manager instructions — this is an internal tool
- Badges (CI, coverage) — no CI pipeline configured
- Internationalization notes — single-language tool

### Complexity

**Low to Medium.** Content writing task. No code involved. The main cost is deciding what
level of detail to include for an internal developer audience. The pipeline diagram can be
promoted from the current README since it is already correct. The build/run instructions
require knowing the exact `./gradlew` commands and JAR output path.

### Dependencies

- `build.gradle` version (must be updated to 0.3.0 first for accurate README)
- Icon placement (README should reference the icon once it has a stable location)
- None in terms of code

---

## Anti-Features for v0.3.0

| Anti-Feature | Why to Avoid | What to Do Instead |
|--------------|-------------|-------------------|
| Replace RichTextFX with a different editor | Massive scope, regressions in 5 existing features | CSS overrides for dark theme |
| Third-party dark theme JAR (AtlantaFX etc.) | New dependency, risk breaking existing CSS | `.root` CSS override in `main.css` |
| Automated version bump scripts | Overkill for internal tool | Update `build.gradle` manually per release |
| CI badge generation | No CI pipeline | Omit from README for now |
| FOP full font configuration | Out of scope until encoding root cause confirmed | Diagnose first |

---

## Feature Dependency Order

```
Icon placement → README (README references icon)
build.gradle version → About dialog (version shown correctly)
Dark theme diagnosis → Dark theme fix (cannot fix without knowing what breaks)
Encoding diagnosis → Encoding fix (root cause unknown)
TableView resize policy → (independent, no dependencies)
```

---

## Complexity Summary

| Feature | Complexity | Risk | Java changes | CSS changes | FXML changes |
|---------|------------|------|--------------|-------------|--------------|
| Dark theme (CodeArea bg) | Low | Low | No | Yes | No |
| Dark theme (root cascade) | Low | Low | No | Yes | No |
| Dark theme (INFO row text) | Low | Low | No | Yes | No |
| Log panel full-width | Low | Low | No | No | Yes (1 attr) |
| Log panel extra column | Low | Low | No | No | Yes (1 attr) |
| Encoding (diagnosis) | Medium | Medium | Maybe | Maybe | No |
| About version | Trivial | None | No | No | No |
| App icon (resource) | Low | None | Yes (5 lines) | No | No |
| README rewrite | Low-Med | None | No | No | No |

---

## Sources

- Codebase inspection: `main.css`, `main.fxml`, `LogController.java`, `EditorTab.java`,
  `AboutDialog.java`, `XSLEditorApp.java`, `RenderEngine.java`, `build.gradle`
- JavaFX CSS reference: `.root` Modena lookup keys (`-fx-base`, `-fx-control-inner-background`,
  `-fx-text-base-color`) — standard JavaFX 21 CSS documentation pattern
- RichTextFX CSS hooks: `.code-area`, `.code-area .content`, `.code-area .caret` —
  documented in RichTextFX README and stylesheet (MEDIUM confidence; version 0.11.5 in use)
- `TableView.CONSTRAINED_RESIZE_POLICY` — JavaFX 21 standard API; behavior of trailing
  phantom column under UNCONSTRAINED policy is a well-known JavaFX behavior (HIGH confidence)
- Gradle `processResources` token expansion — documented Gradle feature, confirmed working
  in `build.gradle` (HIGH confidence; mechanism already in place)
