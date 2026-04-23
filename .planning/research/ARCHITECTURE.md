# Architecture Integration: v0.3.0 Polish & Usability

**Project:** XSLEditor
**Milestone:** v0.3.0 Polish & Usability
**Researched:** 2026-04-23
**Confidence:** HIGH — all integration points verified directly in source

---

## System Map (Relevant Components)

```
XSLEditorApp.start()
  └── loads main.fxml → MainController.initialize()
        ├── FileTreeController  (TreeView in fileTreePane StackPane)
        ├── EditorController    (TabPane + CodeArea in editorPane StackPane)
        ├── LogController       (TableView in FXML logTableView)
        ├── RenderController    (render pipeline trigger)
        └── PreviewController   (WebView PDF display)

Resources (classpath):
  src/main/resources/
    ch/ti/gagi/xsleditor/ui/main.fxml     ← stylesheet ref: @main.css
    ch/ti/gagi/xsleditor/ui/main.css      ← single stylesheet for entire app
    version.properties                    ← injected by Gradle processResources

Build:
  build.gradle processResources block     ← expand(version: project.version)
  version in build.gradle line 14:        version = '0.1.0'  (stale — needs bump)
```

---

## Feature 1: Dark Theme CSS Fix

**What:** Fix text colors for TreeView, CodeArea, and TableView in the dark theme.

**Integration point:** Single file — `src/main/resources/ch/ti/gagi/xsleditor/ui/main.css`.

**Current state analysis:**
- TreeView (`file-tree-view .tree-cell`) — CSS rule exists and sets `-fx-text-fill: #cccccc`. However, JavaFX default Modena stylesheet sets `.tree-cell:selected` with a system blue that overrides custom rules unless specificity is matched. The existing rule targets `.tree-cell:selected` only for border; text-fill is missing from the `:selected` state.
- CodeArea (RichTextFX) — RichTextFX CodeArea renders text via its own internal `.styled-text-area` CSS class. The existing `main.css` has no rules for `.styled-text-area` or `.text-flow`. Modena defaults give white background and black text. Background is implicitly dark because the surrounding `editorPane` StackPane has `.placeholder-pane` (-fx-background-color: #2b2b2b), but the CodeArea itself paints its own viewport white unless styled directly.
- TableView (log panel) — `.log-table-view .table-cell` sets text-fill correctly. However `.table-row-cell:selected .table-cell` and `.table-row-cell:focused` states are not overridden, so selected rows revert to Modena system-blue background with potentially unreadable text.

**Required CSS additions — all in `main.css`:**
```
.styled-text-area            /* CodeArea background + text base color */
.styled-text-area .text      /* actual text node fill */
.file-tree-view .tree-cell:selected   /* add -fx-text-fill to selected state */
.log-table-view .table-row-cell:selected .table-cell   /* selected row text */
.log-table-view .table-row-cell:selected               /* selected row bg */
```

**Dependencies:** None. CSS changes are hot-reloadable during dev; no Java changes required.

**Risk:** RichTextFX `.styled-text-area` CSS class names must be confirmed against RichTextFX 0.11.5. The class name is documented in the RichTextFX source and has been stable since 0.9.x (HIGH confidence). The `.text` pseudo-element inside it controls fill for unstyled text.

---

## Feature 2: Log Panel TableView — Column Width Fix

**What:** Make columns fill the TableView container; remove any phantom extra column.

**Integration points:**

1. **`main.fxml`** — `TableView` definition with explicit `prefWidth` on each `TableColumn`:
   - `colTime`: 65, `colLevel`: 60, `colType`: 100, `colMessage`: 400, `colAi`: 40
   - Total fixed widths: 665 px. No column has `fx:id` equivalent of `maxWidth="Infinity"` or a resize policy set. JavaFX default resize policy is `UNCONSTRAINED_RESIZE_POLICY`, which leaves the remaining space as an empty phantom column.

2. **`LogController.initialize()`** — `logTableView.setEditable(false)` is called but no `columnResizePolicy` is set.

**Two-part fix required:**

Part A (Java — `LogController.initialize()`): Add one line after the `logTableView` setup:
```java
logTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
```
This eliminates the phantom column and distributes extra width across columns.

Part B (FXML — `main.fxml`): With `CONSTRAINED_RESIZE_POLICY`, `prefWidth` values become initial weights rather than hard sizes. The `colMessage` column should be given a larger `prefWidth` relative to others so it absorbs the majority of available width after constraint distribution. Alternatively, individual column `minWidth` constraints can lock narrow columns (Time, Level, AI) while letting Message grow.

**Alternative:** Set `colMessage` to have no `maxWidth` constraint and explicitly set `minWidth` on the narrow columns; leave `prefWidth` as hints. Both approaches are valid — CONSTRAINED_RESIZE_POLICY is the simpler one-line fix.

**Dependencies:** None from other features in this milestone.

---

## Feature 3: Encoding Fix

**What:** Identify and fix character encoding issues in the render/display pipeline.

**Integration point analysis — all pipeline stages read with explicit UTF-8:**

| Stage | Location | Encoding |
|-------|----------|----------|
| File read into editor | `EditorController.java:136` | `Files.readString(key, StandardCharsets.UTF_8)` — correct |
| File save from editor | `EditorController.java:301,319` | `Files.writeString(..., StandardCharsets.UTF_8)` — correct |
| XSLT entrypoint load | `RenderOrchestrator.java:69` | `Files.readString(entryPath, StandardCharsets.UTF_8)` — correct |
| Library preprocessor | `LibraryPreprocessor.java:49` | `Files.readString(file, StandardCharsets.UTF_8)` — correct |
| FO → PDF render | `RenderEngine.java:72` | `foContent.getBytes(StandardCharsets.UTF_8)` — correct |
| XSLT transform output | `RenderEngine.java:48-49` | `Serializer.Property.ENCODING = "UTF-8"` — correct |

**Most likely root cause:** Saxon's XSLT serializer writes the FO result to a `StringWriter`. The `StringWriter` is Java's in-memory Unicode writer — no encoding loss there. However, if the XSLT stylesheet contains `<xsl:output encoding="..."/>` that differs from UTF-8, Saxon will attempt to honor it in the byte output path. In the current code the output goes to a `StringWriter` (character stream), so encoding directives are irrelevant.

**Second candidate:** `FopFactory.newInstance(new java.net.URI("."))` uses the JVM default `user.dir` as base URI. If FOP resolves any external font or image resources, the resolved path may differ from the project root. This can cause FOP to fail silently, producing garbled characters or substitution glyphs in the PDF.

**Third candidate:** XML input file encoding declaration. If an XML file declares `<?xml version="1.0" encoding="ISO-8859-1"?>` but is read as UTF-8 (or vice versa), Saxon will throw a parse error or silently misinterpret characters. Saxon reads via `StreamSource(xmlFile.toFile())` without an explicit charset — Saxon respects the XML declaration in this case (correct behavior), but a mismatch between the file's actual bytes and its declared encoding would cause a problem.

**Investigation path for the phase:**
1. Reproduce with a concrete example (what characters, what file, at what pipeline stage does corruption appear?).
2. Check the XML input file's declared encoding vs its actual byte encoding on disk.
3. If display corruption only (not PDF), check `RenderEngine.transformToString()` — `StringWriter` output fed into `PreviewController` which renders as HTML in WebView. If that HTML is not served with a charset header, WebView may misinterpret the bytes.
4. If PDF corruption, check FOP font substitution logs.

**Files to investigate (no changes yet):**
- `RenderEngine.java` — transform output path
- `PreviewController.java` — how FO/HTML is passed to WebView (base64 encoding bypasses charset issues, but the HTML wrapper must declare `charset=UTF-8`)

**Dependencies:** Independent of all other features. This is an investigation task first.

---

## Feature 4: About Version Auto-Update

**What:** Ensure the version shown in AboutDialog matches the build version in `build.gradle`, without hardcoding.

**Integration point analysis:**

The mechanism already exists and is correctly wired end-to-end:

1. `build.gradle` line 14: `version = '0.1.0'`
2. `build.gradle` lines 66–70: `processResources` block expands `${version}` in `version.properties`
3. `src/main/resources/version.properties` contains: `version=${version}` (the template literal)
4. `AboutDialog.loadVersion()` reads `/version.properties` from classpath and returns `p.getProperty("version", "unknown")`

**The version mechanism works correctly when running from the built JAR or via `./gradlew run`.** The version will show as the literal string `${version}` only when running from an IDE that does not trigger `processResources` before launching. This is a dev-environment issue, not a production bug.

**The real fix needed:** The version in `build.gradle` is `0.1.0` but the milestone is `v0.3.0`. The version property has never been bumped. The task is to update `build.gradle` line 14 to `version = '0.3.0'` (or whatever the correct current version is). Everything downstream — `version.properties`, `AboutDialog` — will reflect this automatically after the next build.

**Single-file change:** `build.gradle`, line 14.

**No Java changes required.** `AboutDialog.loadVersion()` is correct as-is.

**Dependencies:** None from other features.

---

## Feature 5: App Icon

**What:** Move `icon.png` from project root to `src/main/resources/`, wire into the JavaFX Stage.

**Current state:** `icon.png` exists at `/Users/kraehend/Developer/XSLEditor/icon.png` (project root — not on classpath, not in JAR).

**Integration points:**

1. **Resource placement:** Move to `src/main/resources/ch/ti/gagi/xsleditor/icon.png` (alongside `ui/` directory) or to `src/main/resources/icon.png` (classpath root). Either works; the `ch/ti/gagi/xsleditor/` path is more organized.

2. **`XSLEditorApp.start()`:** This is the only place the primary Stage is configured before `primaryStage.show()`. Icon wiring belongs here, immediately before `primaryStage.show()`:
   ```java
   URL iconUrl = getClass().getResource("/ch/ti/gagi/xsleditor/icon.png");
   if (iconUrl != null) {
       primaryStage.getIcons().add(new javafx.scene.image.Image(iconUrl.toExternalForm()));
   }
   ```

3. **`AboutDialog`:** Optionally, the app icon can also be shown in the dialog's header area. This is a secondary concern — wiring to the Stage is the critical path.

**Files changed:**
- `icon.png` moved from root → `src/main/resources/ch/ti/gagi/xsleditor/icon.png`
- `XSLEditorApp.java` — 3-line addition before `primaryStage.show()`

**Dependencies:** None from other features. Independent.

**Note:** On macOS, the dock icon is controlled by the `.app` bundle's `Info.plist` when running as a packaged app. When running from JAR or `./gradlew run`, `Stage.getIcons()` controls the window title bar icon but the dock shows the generic Java coffee cup. This is a macOS platform limitation, not a code defect.

---

## Feature 6: README Rewrite

**What:** Documentation-only update. No code, no build changes.

**Integration points:** None. This is a standalone Markdown file.

**Content that requires accuracy verification from code:**
- Correct JAR invocation command (check `shadowJar` output name in `build.gradle`)
- Current version number (aligns with Feature 4 — bump to 0.3.0)
- Icon display (include `icon.png` screenshot or reference — aligns with Feature 5 move)
- Build prerequisites: Java 21, `./gradlew shadowJar`

**Build order note:** Write README after Features 4 and 5 are complete so the version number and icon path documented in the README are accurate.

---

## Build Order Recommendation

Features are largely independent; the following order minimizes rework and respects the one documentation dependency:

| Order | Feature | Rationale |
|-------|---------|-----------|
| 1 | Version bump (Feature 4) | One-line change, zero risk, unblocks README accuracy. Do first. |
| 2 | App icon move + wire (Feature 5) | Zero dependencies, trivial, unblocks README accuracy. |
| 3 | CSS dark theme fix (Feature 1) | CSS-only, visually verifiable, no Java changes, independent. |
| 4 | Log panel column fix (Feature 2) | One Java line + optional FXML tweak, independent. |
| 5 | Encoding investigation (Feature 3) | Requires reproduction steps; may or may not result in code changes. Place here so CSS/column fixes don't interfere with render pipeline testing. |
| 6 | README rewrite (Feature 6) | Last — documents the actual final state of icon, version, and build. |

---

## Component Modification Summary

| File | Feature(s) | Change Type |
|------|-----------|-------------|
| `build.gradle` | 4 | Version string bump (1 line) |
| `src/main/resources/ch/ti/gagi/xsleditor/ui/main.css` | 1 | CSS rule additions |
| `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` | 2 | `prefWidth` / `minWidth` tuning on TableColumns |
| `src/main/java/.../ui/LogController.java` | 2 | `setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY)` |
| `src/main/java/.../XSLEditorApp.java` | 5 | Stage icon wiring (3 lines) |
| `src/main/resources/ch/ti/gagi/xsleditor/icon.png` | 5 | New file (moved from root) |
| `src/main/java/.../render/RenderEngine.java` | 3 | Possible encoding fix (TBD after investigation) |
| `src/main/java/.../ui/PreviewController.java` | 3 | Possible charset declaration in HTML wrapper (TBD) |
| `README.md` | 6 | Full rewrite |

**Not touched:** `AboutDialog.java` (version loading is correct), `MainController.java`, `FileTreeController.java`, `EditorController.java`, all backend pipeline classes.

---

## Dependency Graph Between Features

```
Feature 4 (version bump)  ──┐
Feature 5 (icon)          ──┤──► Feature 6 (README)
                              │
Feature 1 (CSS)  ─── independent
Feature 2 (columns) ─ independent
Feature 3 (encoding) ─ independent (investigation-first)
```

Features 1, 2, 3 are fully orthogonal to each other and to Features 4/5/6.
Features 4 and 5 are prerequisites only for Feature 6 (README accuracy), not for each other.
