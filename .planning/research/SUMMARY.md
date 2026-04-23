# Research Summary — v0.3.0 Polish & Usability

**Project:** XSLEditor
**Milestone:** v0.3.0 Polish & Usability
**Researched:** 2026-04-23
**Confidence:** HIGH

## Executive Summary

v0.3.0 is a pure polish milestone: no new libraries, no architectural changes, no new pipeline
stages. All six feature areas (dark theme text visibility, log panel full-width layout, character
encoding, About version auto-update, app icon wiring, README rewrite) are solvable with the
existing stack — JavaFX 21, RichTextFX 0.11.5, Gradle, and the single `main.css` stylesheet
already loaded by FXML. The largest risk is not implementation complexity but CSS specificity:
JavaFX's Modena theme overwrites custom background and text-fill rules on selected/focused
states unless every pseudo-class combination is explicitly targeted.

The recommended approach is to sequence changes from zero-risk (version bump, icon move) through
pure-CSS (dark theme), one-line Java/FXML (log panel resize policy), to investigation-first
(encoding). This ordering prevents wasted debugging effort: the dark theme and log panel fixes
are verifiable in seconds; the encoding root cause must be reproduced before any code is touched.

Key risk: encoding. The Java pipeline is correctly hardcoded to UTF-8 throughout; the reported
encoding problem almost certainly originates in one of three places — a UTF-8 BOM left in the
editor display, an XSLT `xsl:output` encoding declaration mismatch, or a missing FOP font for
non-ASCII glyphs. Diagnosing the exact symptom location before writing a fix is mandatory;
otherwise the wrong layer gets patched and the issue recurs.

---

## Stack Additions

No new dependencies. All changes touch existing files only.

| What | Where | Change type |
|------|-------|-------------|
| CSS selectors for RichTextFX dark theme | `main.css` | Additions |
| CSS selectors for TreeView/TableView selected states | `main.css` | Additions |
| `.root` Modena cascade override | `main.css` | Addition |
| `columnResizePolicy` on `logTableView` | `LogController.java` | 1 line |
| `TableColumn` `minWidth` values | `main.fxml` | Attribute additions |
| Stage icon wiring | `XSLEditorApp.java` | ~5 lines |
| `icon.png` moved to classpath | `src/main/resources/` | File move |
| `version` string bump | `build.gradle` | 1 line |

**Critical CSS selectors for RichTextFX CodeArea (not obvious from JavaFX docs):**
- `.code-area .content` — actual painted background (not `.code-area` alone)
- `.code-area .text` — uses `-fx-fill`, not `-fx-text-fill` (Text node, not Control)
- `.code-area .caret` — uses `-fx-stroke`
- `.code-area .selection` — uses `-fx-fill`
- `.virtualized-scroll-pane` — must also be darkened (scroll viewport exposes its background)

**Critical CSS selectors for Modena dark override:**
- `.root { -fx-base; -fx-background; -fx-control-inner-background; -fx-text-base-color }` — cascades into most standard controls automatically

---

## Feature Table Stakes

| Feature area | Must work |
|---|---|
| Dark theme — CodeArea | Dark background (#1e1e1e), light text (#d4d4d4), visible caret and selection highlight |
| Dark theme — TreeView | Selected cell text remains readable; no dark-on-dark inversion |
| Dark theme — TableView | INFO row text visible; selected row text visible; column header labels visible |
| Dark theme — ToolBar/MenuBar | Dark background (achieved via `.root` cascade, no extra rules) |
| Log panel layout | No phantom filler column; Message column fills remaining width; no horizontal scrollbar |
| Log panel layout | Time/Level/AI columns do not compress below readable width (requires `minWidth`) |
| Encoding | UTF-8 files display correctly in editor; no BOM artifact as first character |
| Encoding | Non-ASCII characters survive XSLT transform and appear correctly in PDF |
| About version | Dialog shows `0.3.0`, not `0.1.0` or literal `${version}` |
| App icon | Window title bar shows icon; loaded before `primaryStage.show()` |
| README | Build instructions, requirements (Java 21), feature list, pipeline diagram, run command |

**Defer to later:**
- FOP full font embedding — needed only if encoding investigation confirms PDF glyph issue
- macOS Dock icon for packaged `.app` — platform limitation, not fixable via `Stage.getIcons()`
- CI badges in README — no CI pipeline configured
- Third-party dark theme JAR (AtlantaFX) — unnecessary dependency; `.root` cascade is sufficient

---

## Build Order

| Order | Phase | Rationale |
|-------|-------|-----------|
| 1 | Zero-risk housekeeping (version bump, icon) | 1–5 line changes, zero regression risk; unblocks README accuracy |
| 2 | CSS dark theme fix | CSS-only, hot-reloadable, additive; no Java changes; largest surface area, lowest risk |
| 3 | Log panel layout fix | One Java line + FXML `minWidth`; independent of CSS; confined to log panel |
| 4 | Encoding investigation and fix | Investigation-first; fixed dark theme and log panel make error reading easier |
| 5 | README rewrite | Last — documents final state of icon path, version, and build commands |

**Phase 1 detail — Zero-risk housekeeping:**
- Bump `version` in `build.gradle` to `'0.3.0'`
- Move `icon.png` to `src/main/resources/ch/ti/gagi/xsleditor/icon.png`
- Wire icon in `XSLEditorApp.start()` before `primaryStage.show()`; check `icon.isError()` after load
- Run `./gradlew clean shadowJar` (clean is required to purge stale expanded `version.properties`)

**Phase 2 detail — CSS dark theme:**
- Add `.root` with `-fx-base: #3c3f41`, `-fx-background: #1e1e1e`, `-fx-control-inner-background: #2b2b2b`, `-fx-text-base-color: #cccccc`
- Add `.code-area .content`, `.code-area .text` (`-fx-fill`), `.code-area .caret` (`-fx-stroke`), `.code-area .selection`
- Add `.virtualized-scroll-pane` background
- Add `-fx-text-fill` to `.file-tree-view .tree-cell:selected`
- Add `.log-table-view .table-row-cell:selected`, `:selected:focused` with text-fill
- Add `.log-table-view .column-header .label` with `-fx-text-fill`

**Phase 3 detail — Log panel layout:**
- Set `minWidth` on `colTime` (40), `colLevel` (50), `colAi` (36) in `main.fxml`
- Call `logTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY)` in `LogController.initialize()` — do in Java, not FXML, to avoid layout flash on first render

**Phase 4 detail — Encoding (investigation-first):**
- Reproduce with a concrete XML file containing known non-ASCII characters (accented Italian)
- Determine symptom stage: editor display? log panel? PDF output?
- Editor display: check for BOM (strip after `Files.readString`); check UI font coverage
- XSLT output: check `<xsl:output encoding>` declaration in XSLT templates
- PDF: add `fop.xconf` font configuration for required glyph ranges
- Fix only the confirmed layer

---

## Watch Out For

1. **CodeArea background targets the wrong node** — `.code-area { -fx-background-color }` has no
   visible effect; the painted background belongs to `.code-area .content`. Always target the inner
   sub-node. Confirm with ScenicView or `scene.lookup(".code-area .content")` before writing CSS.

2. **Modena `:selected:focused` overrides custom cell styles** — any `.table-row-cell` or
   `.tree-cell` background rule must be paired with matching `:selected`, `:focused`, and
   `:selected:focused` pseudo-class rules, each with explicit `-fx-text-fill`. Without them,
   Modena wins by specificity and text becomes invisible on selection.

3. **`CONSTRAINED_RESIZE_POLICY` without `minWidth` compresses narrow columns to zero** — always
   set `minWidth` on Time (40), Level (50), and AI (36) before enabling the policy; otherwise the
   log panel becomes unreadable at narrower window sizes.

4. **Icon loads silently into error state** — `new Image(url)` does not throw on a wrong path; it
   produces an errored `Image` that JavaFX silently ignores. Always check `icon.isError()` and log
   the failure. Icon must be added to `Stage.getIcons()` before `primaryStage.show()`.

5. **Encoding: diagnose before fixing** — the Java pipeline is clean (all explicit UTF-8). The
   three candidates in order of likelihood: UTF-8 BOM in editor display, Saxon `xsl:output`
   encoding mismatch, FOP font substitution in PDF. Confirm the stage first; do not speculatively
   patch all three layers.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | No new deps; all APIs verified against RichTextFX 0.11.5 JAR bundled CSS and codebase |
| Features | HIGH | Root issues confirmed by codebase inspection; Modena/RichTextFX CSS behavior well-documented |
| Architecture | HIGH | All integration points verified directly in source; no inferred wiring |
| Pitfalls | HIGH | Standard JavaFX CSS specificity and RichTextFX sub-node behavior; encoding pipeline verified |

**Overall confidence: HIGH**

### Gaps to Address

- **Encoding root cause (MEDIUM):** The Java pipeline is confirmed clean, but the actual symptom
  has not been reproduced in a running instance. Phase 4 must begin with reproduction steps, not
  implementation. Confirm symptom location (editor / log / PDF) before writing any fix.

- **macOS Dock icon:** Known platform limitation when running from JAR. Out of scope; document as
  a known limitation in the README.

- **Alert/Dialog dark theme:** `EditorController.showError()` opens a standard `Alert` that uses
  Modena light theme. Not in scope for v0.3.0, but if any phase touches error dialogs, add
  `getDialogPane().getStylesheets().add(mainCssUrl)` for consistency.

---

## Sources

### Primary (HIGH confidence)
- Codebase inspection: `main.css`, `main.fxml`, `LogController.java`, `EditorController.java`,
  `AboutDialog.java`, `XSLEditorApp.java`, `RenderEngine.java`, `RenderOrchestrator.java`,
  `LibraryPreprocessor.java`, `build.gradle`
- RichTextFX 0.11.5 bundled CSS extracted from JAR: `code-area.css`, `styled-text-area.css`
- JavaFX 21 CSS Reference Guide — Modena derived lookup keys, pseudo-class specificity rules
- `TableView.CONSTRAINED_RESIZE_POLICY` — JavaFX 21 standard API

### Secondary (MEDIUM confidence)
- RichTextFX README and community dark-theme examples — `.code-area .content` sub-node targeting
- JavaFX community documentation on Modena `.root` cascade properties

---
*Research completed: 2026-04-23*
*Ready for roadmap: yes*
