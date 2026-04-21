# Roadmap: XLSEditor v1.0

**Milestone:** v1.0 — MVP desktop tool (JavaFX UI + full pipeline integration + tests)
**Created:** 2026-04-14

---

## Phase 1: JavaFX Application Shell

**Goal:** Runnable JavaFX application with main window, split-pane layout skeleton, and Gradle dependencies wired up.

**Requirements:** APP-01, APP-02, APP-03, APP-04

**Deliverables:**
- JavaFX added to `build.gradle` (javafx-controls, javafx-fxml)
- RichTextFX dependency added
- PDFViewerFX dependency added
- Main `Application` class with `start()` and primary `Stage`
- Main FXML layout: MenuBar, SplitPane (left=editor area, right=preview area), bottom=log panel placeholder
- App title updates to "XLSEditor — [ProjectName]" when project loaded, "XLSEditor" otherwise
- Close confirmation dialog when unsaved changes exist

---

## Phase 2: Project Management

**Goal:** User can open a project directory, select entrypoint and XML input, and have choices persisted in `.xslfo-tool.json`.

**Requirements:** PROJ-01, PROJ-02, PROJ-03, PROJ-04, PROJ-05, PROJ-06

**Deliverables:**
- "Open Project" menu action using `DirectoryChooser`
- Loads `.xslfo-tool.json` and restores entrypoint + XML input
- Toolbar/menu actions: "Set Entrypoint", "Set XML Input" (from currently selected file in tree)
- Saves changes to `.xslfo-tool.json` via existing `ProjectConfig` model
- "New File" dialog: prompts for filename, creates empty file in project root
- Project state managed in a `AppController` or `ProjectContext` class

---

## Phase 3: File Tree View

**Goal:** Left panel shows all project files in a flat tree with visual distinction for entrypoint and XML input.

**Requirements:** TREE-01, TREE-02, TREE-03, TREE-04

**Deliverables:**
- `TreeView<FileItem>` in left panel showing all files in project root
- Entrypoint file shown with distinct icon/color
- XML input file shown with distinct icon/color
- Double-click on file opens it in the editor (calls editor open tab)
- Tree refreshes when new file is created

---

## Phase 4: Multi-Tab Editor (Core)

**Goal:** Multi-tab code editor with RichTextFX, file open/save, and dirty state tracking.

**Requirements:** EDIT-01, EDIT-02, EDIT-03, EDIT-09

**Plans:** 3 plans

**Deliverables:**
- `TabPane` with one `CodeArea` (RichTextFX) per open file
- Tab title: filename, with `*` prefix when dirty
- Ctrl+S saves current file and clears dirty state
- Close-tab confirmation dialog when file is dirty
- Opening a file that's already open in a tab switches to that tab (no duplicates)

**Plans:**
- [x] 04-01-PLAN.md — EditorTab model + dirty-state unit tests (Wave 0 gap closed)
- [x] 04-02-PLAN.md — EditorController sub-controller: TabPane, open/save/close, Ctrl+S, confirmation dialog
- [x] 04-03-PLAN.md — Wire EditorController into MainController + FileTree open-seam + human verification

---

## Phase 5: Editor Features (Syntax & Navigation)

**Goal:** XML/XSLT syntax highlighting, static autocomplete, variable highlighting, and go-to-definition.

**Requirements:** EDIT-04, EDIT-05, EDIT-06, EDIT-07, EDIT-08

**Plans:** 5 plans

**Deliverables:**
- XML/XSLT syntax highlighting via RichTextFX `StyleSpans` (CSS classes for elements, attributes, comments, CDATA, processing instructions)
- Static autocomplete popup for common XSL/XSL-FO keywords (triggered by Ctrl+Space or typing `<xsl:`)
- Variable/template reference highlighting: regex-based highlight of all occurrences of selected `$var` or `name="template"` in current file
- Go-to-definition: Ctrl+Click or menu action on `xsl:include`/`xsl:import` href opens the referenced file
- Multi-file search dialog: search string across all project files, results list with filename:line, click navigates editor

**Plans:**
- [x] 05-01-PLAN.md — Wave 0: test stubs for all 5 logic classes (XmlSyntaxHighlighter, AutocompleteProvider, OccurrenceHighlighter, HrefExtractor, SearchTask)
- [x] 05-02-PLAN.md — Wave 1: XmlSyntaxHighlighter + AutocompleteProvider implementation
- [x] 05-03-PLAN.md — Wave 1: OccurrenceHighlighter + HrefExtractor implementation (parallel with 05-02)
- [x] 05-04-PLAN.md — Wave 2: Wire highlighting, autocomplete, occurrence, go-to-def in EditorController + CSS classes
- [x] 05-05-PLAN.md — Wave 2: SearchDialog + Find in Files menu wiring + human verification checkpoint

---

## Phase 6: Render Pipeline Integration

**Goal:** Render button triggers the full backend pipeline and reports success/failure to the UI.

**Requirements:** REND-01, REND-02, REND-03, REND-04, REND-05, REND-06

**Deliverables:**
- Render button in toolbar, disabled when preconditions not met (no project / no entrypoint / no XML input)
- Render executes on a background thread (`Task<Preview>`) to keep UI responsive
- Progress spinner/indicator shown during render
- On success: passes PDF bytes to preview panel
- On failure: marks preview as outdated, passes errors to log panel
- Render duration logged as info entry

---

## Phase 7: PDF Preview Panel

**Goal:** Right panel displays PDF output with scroll/zoom, and shows an outdated indicator on failure.

**Requirements:** PREV-01, PREV-02, PREV-03, PREV-04

**Plans:** 2 plans

**Deliverables:**
- WebView component (javafx.web) embedded in right SplitPane pane, loading PDF from temp file
- Renders PDF from `byte[]` returned by pipeline via Files.write() + WebEngine.load(file://)
- Scroll and zoom via native WebView PDF plugin (no custom controls)
- "Outdated" banner (orange #f97316) shown at top of preview pane when last render failed
- No PDF shown (WebView hidden, placeholder visible) before first successful render

**Plans:**
- [x] 07-01-PLAN.md — Wave 0: PreviewControllerTest stubs (PREV-03, PREV-04)
- [x] 07-02-PLAN.md — Wave 1: PreviewController + MainController wiring + FXML/CSS + enable tests + human verify

---

## Phase 8: Error & Log Panel

**Goal:** Bottom panel displays all render log entries with severity filtering and click-to-navigate for errors with locations.

**Requirements:** ERR-01, ERR-02, ERR-03, ERR-04, ERR-05

**Plans:** 2 plans

**Plans:**
- [x] 08-01-PLAN.md — LogEntry extension + LogController (TableView, filter bar, cell factory, navigation) + FXML/CSS
- [x] 08-02-PLAN.md — RenderController Consumer-callback refactor + MainController wiring + human verify

**Deliverables:**
- Log panel as `TableView` or styled `ListView` at bottom of main window
- Columns: timestamp, severity badge (color-coded), type, message
- Severity filter buttons (All / Error / Warning / Info)
- On render start: log panel cleared
- Clicking an entry with file+line info opens the file in editor and jumps to that line
- Error entries shown in red, warnings in yellow, info in default color

---

## Phase 9: Testing

**Goal:** Unit tests for all backend modules and integration tests for the full pipeline. Zero regressions.

**Requirements:** TEST-01, TEST-02, TEST-03, TEST-04, TEST-05, TEST-06, TEST-07, TEST-08

**Plans:** 1/4 plans executed

**Deliverables:**
- `src/test/java/` directory structure mirrors `src/main/java/`
- Unit tests: LibraryPreprocessor, DependencyResolver, ValidationEngine, RenderEngine, ErrorManager, LogManager
- Test fixtures: minimal XSLT + XML files in `src/test/resources/fixtures/`
- Integration test: full pipeline with real Saxon + FOP producing a valid PDF
- Integration test: pipeline failure scenario (invalid XSLT) → correct PreviewError type and location
- All tests pass via `./gradlew test`
- Test coverage report available via JaCoCo (optional, added to build.gradle)

**Plans:**
- [x] 09-01-PLAN.md — Wave 1: LibraryPreprocessor + DependencyResolver unit tests (TEST-01, TEST-02)
- [ ] 09-02-PLAN.md — Wave 1: ValidationEngine + ErrorManager + LogManager unit tests (TEST-03, TEST-05, TEST-06)
- [ ] 09-03-PLAN.md — Wave 1: Shared fixtures (identity.xsl, input.xml, invalid.xsl) + RenderEngine unit tests with real Saxon+FOP (TEST-04)
- [ ] 09-04-PLAN.md — Wave 2: PreviewManager integration tests — full pipeline success + invalid-XSLT failure (TEST-07, TEST-08)

---

## Summary

| Phase | Name | Requirements | Status |
|-------|------|--------------|--------|
| 1 | 1/1 | Complete   | 2026-04-14 |
| 2 | Project Management | PROJ-01..06 | Pending |
| 3 | File Tree View | TREE-01..04 | Pending |
| 4 | Multi-Tab Editor (Core) | EDIT-01..03, EDIT-09 | Complete (2026-04-18) |
| 5 | Editor Features | EDIT-04..08 | Complete (2026-04-19) |
| 6 | Render Pipeline Integration | REND-01..06 | Pending |
| 7 | PDF Preview Panel | PREV-01..04 | Complete (2026-04-20) |
| 8 | Error & Log Panel | ERR-01..05 | Complete (2026-04-20) |
| 9 | 1/4 | In Progress|  |

**Total phases:** 9
**Total v1 requirements:** 46
**Coverage:** 46/46 ✓

---
*Roadmap created: 2026-04-14*
