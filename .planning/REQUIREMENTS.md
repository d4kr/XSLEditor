# Requirements: XLSEditor

**Defined:** 2026-04-14
**Core Value:** A developer can open a project, edit XSLT templates, trigger a render, and see the PDF — all in one window without context switching.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Application Shell

- [x] **APP-01**: Application launches and displays a main window with split-pane layout
- [x] **APP-02**: Application title reflects project name when a project is loaded
- [x] **APP-03**: Closing with unsaved changes prompts a confirmation dialog
- [x] **APP-04**: Application state (last opened project) is NOT persisted between sessions (no session restore)

### Project Management

- [ ] **PROJ-01**: User can open a project by selecting a directory from the filesystem
- [ ] **PROJ-02**: User can select the entrypoint XSLT file from the project files
- [ ] **PROJ-03**: User can select the XML input file from the project files
- [ ] **PROJ-04**: Entrypoint and XML input selections are persisted in `.xslfo-tool.json`
- [ ] **PROJ-05**: Application reads `.xslfo-tool.json` on project open and restores selections
- [ ] **PROJ-06**: User can create a new file in the project root directory

### File Tree

- [ ] **TREE-01**: File tree panel shows all files in the project root directory
- [ ] **TREE-02**: Entrypoint XSLT is visually distinguished in the tree
- [ ] **TREE-03**: XML input file is visually distinguished in the tree
- [ ] **TREE-04**: Double-click on a file opens it in the editor

### Editor

- [ ] **EDIT-01**: Editor supports multi-tab layout (one tab per open file)
- [ ] **EDIT-02**: Tab shows filename with dirty indicator (`*`) when unsaved changes exist
- [ ] **EDIT-03**: Ctrl+S saves the current file and clears dirty state
- [x] **EDIT-04**: Syntax highlighting for XML/XSLT files (element names, attributes, comments, CDATA)
- [x] **EDIT-05**: Static autocomplete for common XSL/XSL-FO tags (via keyword list)
- [x] **EDIT-06**: Variable/template name highlighting (regex-based, same file scope)
- [x] **EDIT-07**: Go-to-definition for `xsl:include`/`xsl:import` hrefs (opens referenced file in new tab)
- [x] **EDIT-08**: Multi-file text search (search across all project files, results list navigable)
- [ ] **EDIT-09**: Closing a tab with unsaved changes prompts confirmation

### Render Pipeline

- [ ] **REND-01**: Render button triggers the full pipeline (save-all → preprocess → validate → transform → render → preview)
- [ ] **REND-02**: Render is disabled when no project is loaded, no entrypoint set, or no XML input set
- [ ] **REND-03**: During rendering, UI shows a progress/loading indicator
- [ ] **REND-04**: Successful render updates the PDF preview
- [ ] **REND-05**: Failed render keeps the previous PDF preview and marks it as outdated
- [ ] **REND-06**: Render target: < 5 seconds for typical projects (≈5–10 files, < 1MB each)

### PDF Preview

- [ ] **PREV-01**: Split view shows editor on left, PDF preview on right
- [ ] **PREV-02**: PDF preview supports scroll and zoom
- [ ] **PREV-03**: When preview is outdated (last render failed), a visual indicator is shown
- [ ] **PREV-04**: No PDF is shown before the first successful render

### Error & Log Panel

- [ ] **ERR-01**: Dedicated log/error panel displays all log entries from the last render
- [ ] **ERR-02**: Each entry shows: timestamp, severity (error/warning/info), type, message
- [ ] **ERR-03**: Log panel supports filtering by severity level
- [ ] **ERR-04**: Clicking an error entry with file+line info navigates the editor to that location
- [ ] **ERR-05**: Log panel is cleared before each new render run

### Testing

- [ ] **TEST-01**: Unit tests for LibraryPreprocessor (directive resolution, missing library error)
- [ ] **TEST-02**: Unit tests for DependencyResolver (include/import graph, circular detection)
- [ ] **TEST-03**: Unit tests for ValidationEngine (well-formed pass, malformed XML error collection)
- [ ] **TEST-04**: Unit tests for RenderEngine (Saxon transform, FOP render — with fixture files)
- [ ] **TEST-05**: Unit tests for ErrorManager (exception normalization, position extraction)
- [ ] **TEST-06**: Unit tests for LogManager (add entries, filter by level, clear)
- [ ] **TEST-07**: Integration test: full render pipeline with real XSLT/XML fixture → PDF output
- [ ] **TEST-08**: Integration test: pipeline failure (invalid XSLT) → PreviewError with correct type/location

## v2 Requirements

Deferred to future release.

### Advanced Editor

- **ADV-01**: Semantic autocomplete (namespace-aware, context-sensitive XSL/XPath)
- **ADV-02**: XSLT template rename refactoring
- **ADV-03**: Real-time validation as-you-type (incremental, debounced)

### Debugger

- **DBG-01**: Step-through XSLT debugger with breakpoints
- **DBG-02**: Variable watch panel during XSLT execution
- **DBG-03**: XSLT profiler (template call counts, execution time)

### Multiple Inputs

- **MINP-01**: Support switching between multiple XML input files per project
- **MINP-02**: Quick-switch XML input without reopening project

### Auto-Render

- **AUTO-01**: Auto-render on save (configurable, off by default)
- **AUTO-02**: Debounce auto-render (500ms after last change)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Authentication | Internal tool — no auth needed |
| Multi-user collaboration | Local-only desktop app |
| HTML preview | PDF-only per PRD spec |
| File rename / delete | Not in MVP scope |
| File watching | External modifications not tracked |
| Multiple simultaneous XML inputs | Single active input per project |
| Session restore (last project) | Keep startup simple — user opens project manually |
| Inline error gutter | Deferred — log panel sufficient for v1 |
| Export/share PDF | Open in system viewer via PDFViewerFX scroll/zoom |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| APP-01..04 | Phase 1 | Pending |
| PROJ-01..06 | Phase 2 | Pending |
| TREE-01..04 | Phase 3 | Pending |
| EDIT-01..03 | Phase 4 | Pending |
| EDIT-04..09 | Phase 5 | Pending |
| REND-01..06 | Phase 6 | Pending |
| PREV-01..04 | Phase 7 | Pending |
| ERR-01..05 | Phase 8 | Pending |
| TEST-01..08 | Phase 9 | Pending |

**Coverage:**
- v1 requirements: 46 total
- Mapped to phases: 46
- Unmapped: 0 ✓

---
*Requirements defined: 2026-04-14*
*Last updated: 2026-04-14 after initial definition*
