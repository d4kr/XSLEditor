# Phase 6: Render Pipeline Integration - Context

**Gathered:** 2026-04-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Render button triggers the full backend pipeline on a background thread and routes the result to the UI. Phase 6 delivers: ToolBar with Render button, menu item + F5 shortcut, background `Task<Preview>` execution, auto-save before render, status-label progress feedback, PDF bytes handed to preview pane, errors routed to log panel, render duration logged.

PDF display (PDFViewerFX) → Phase 7. Log panel UI (filtering, click-to-navigate) → Phase 8.

Requirements in scope: REND-01, REND-02, REND-03, REND-04, REND-05, REND-06

</domain>

<decisions>
## Implementation Decisions

### Render trigger placement

- **D-01:** Add a `ToolBar` node in FXML below the `MenuBar` (inside the `BorderPane` top zone, after MenuBar via VBox). Render button lives in the toolbar.
- **D-02:** Also add a "Render" menu item under a new "Run" menu in MenuBar. Both toolbar button and menu item trigger the same action handler.
- **D-03:** Keyboard shortcut: **F5**. Standard "run" key (IntelliJ/VS Code convention). Wired to the menu item accelerator.
- **D-04:** Follow established controller pattern — introduce a dedicated `RenderController` class. `MainController` holds a `RenderController` reference and calls `renderController.initialize(...)` from its own `initialize()`. `RenderController` owns: Task lifecycle, button state, progress feedback, result routing.

### Render button state during execution

- **D-05:** Button label changes from `"Render"` to `"Rendering..."` while `Task` runs. Button is disabled (`disableProperty` bound to running state). Reverts to `"Render"` and re-enables on Task completion (success or failure).
- **D-06:** No cancel support — button stays disabled for the duration. Keep it simple for Phase 6.
- **D-07:** Render button disabled when: no project loaded OR entrypoint null OR xmlInput null (REND-02). Bind to `ProjectContext.projectLoadedProperty()` AND null-checks on entrypoint/xmlInput. `ProjectContext` needs a new `BooleanProperty renderableProperty()` (or equivalent) that composes these conditions, OR `RenderController` derives it dynamically on each render attempt.

### Save-all before render

- **D-08:** Auto-save all dirty editor tabs silently before the pipeline runs. No confirmation dialog — user clicks Render, all tabs save, render proceeds. Matches REND-01 "save-all → preprocess → ..." pipeline sequence.
- **D-09:** `EditorController.saveAll()` method handles the save-all. `RenderController` calls `editorController.saveAll()` before spawning the Task. If saveAll throws (disk error), show an error alert and abort render.

### Progress feedback

- **D-10:** Use the existing `statusLabel` in `MainController` for progress text. `RenderController` receives a `Consumer<String>` callback (or reference to `showTransientStatus`) to update the label.
- **D-11:** During render: status label shows `"Rendering..."` (persistent, not a timed fade — `PauseTransition` is NOT used during the render).
- **D-12:** On success: status label shows `"Render complete (X.Xs)"` — then auto-clears after 3 seconds via `PauseTransition` (reuse existing pattern).
- **D-13:** On failure: status label shows `"Render failed"` — then auto-clears after 3 seconds. Errors surface in log panel (not in status label).
- **D-14:** Render duration: captured as `System.currentTimeMillis()` delta from Task start to completion. Logged as INFO entry in logListView AND shown in status label on success.

### Result routing

- **D-15:** On success: `RenderController` calls a `Consumer<byte[]>` callback (or `previewController.displayPdf(byte[])` seam) with `Preview.pdf()`. Phase 7 will wire the actual PDFViewerFX display; Phase 6 scaffolds the seam with a no-op or stub.
- **D-16:** On failure: `RenderController` appends `Preview.errors()` to `logListView.getItems()` as formatted strings. Format: `"[ERROR] {type}: {message}"` with file+line appended if present. This is a stopgap (same pattern Phase 2 uses for log entries); Phase 8 replaces logListView behavior with a full LogManager-backed TableView.
- **D-17:** Log panel cleared before each new render (REND-05 / ERR-05). `logListView.getItems().clear()` called at Task start (on FX thread before spawning Task).

### Claude's Discretion

- Exact ToolBar + VBox FXML structure (how toolbar integrates with existing BorderPane top zone)
- Whether `renderableProperty()` is a computed property on `ProjectContext` or derived inline in `RenderController`
- Whether `RenderController` receives `EditorController` by constructor or setter
- `PreviewManager` instantiation location (RenderController constructor, or `XLSEditorApp.start()` wiring)
- Error string formatting details beyond `"[ERROR] {type}: {message}"` prefix

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` §Render Pipeline — REND-01..REND-06 (exact acceptance criteria)
- `docs/PRD.md` — Product requirements (performance target < 5s, no auto-render, manual trigger)

### Existing backend (MUST read — fully implemented, do not re-implement)
- `src/main/java/ch/ti/gagi/xlseditor/preview/PreviewManager.java` — Entry point: `generatePreview(Project, Path)` → `Preview`
- `src/main/java/ch/ti/gagi/xlseditor/preview/Preview.java` — DTO: `success()`, `outdated()`, `pdf()`, `errors()`
- `src/main/java/ch/ti/gagi/xlseditor/preview/PreviewError.java` — Error DTO: `message()`, `type()`, `file()`, `line()`
- `src/main/java/ch/ti/gagi/xlseditor/render/RenderOrchestrator.java` — Passed to `PreviewManager` constructor; `renderSafe()` never throws
- `src/main/java/ch/ti/gagi/xlseditor/log/LogManager.java` — In-memory log (Phase 8 will bind to it; Phase 6 uses `logListView` as stopgap)

### Existing UI (integration points)
- `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` — `statusLabel`, `logListView`, `previewPane`, `menuBar`; all wiring seams for Phase 6
- `src/main/java/ch/ti/gagi/xlseditor/ui/ProjectContext.java` — `getCurrentProject()`, `projectLoadedProperty()`, `projectFilesProperty()`
- `src/main/java/ch/ti/gagi/xlseditor/ui/EditorController.java` — Needs `saveAll()` method (or equivalent); `RenderController` calls this before spawning Task
- `src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml` — Current FXML (BorderPane, MenuBar, SplitPane layout); ToolBar and Run menu must be added here

### Prior phase context
- `.planning/phases/01-javafx-application-shell/01-CONTEXT.md` — D-05: FXML + controller pattern; D-06: three-zone BorderPane layout (ToolBar goes into top VBox)
- `.planning/phases/02-project-management/02-CONTEXT.md` — D-06: ProjectContext owns project state; showTransientStatus pattern
- `.planning/phases/03-file-tree-view/03-CONTEXT.md` — D-08: dedicated controller pattern; constructor/initialize injection pattern

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `PreviewManager.generatePreview(Project, Path)` — call directly; returns `Preview` DTO, never throws
- `showTransientStatus(String)` in `MainController` — 3-second PauseTransition fade; reuse for success/failure message
- `logListView.getItems()` — Phase 6 appends/clears via this list (Phase 8 replaces with LogManager binding)
- `PauseTransition` + `Duration.seconds(3)` pattern — already wired in MainController for status fade

### Established Patterns
- Dedicated controller per concern: `FileTreeController`, `EditorController` → `RenderController` follows same shape
- `@FXML` injection via `initialize()` in MainController, sub-controllers initialized via `controller.initialize(pane, context, ...)`
- `BooleanProperty.bind()` for disable state (see `menuItemNewFile` and tree selection binding)
- JavaFX `Task<T>` for background work: `setOnSucceeded`, `setOnFailed` on FX thread; `Platform.runLater` not needed if using Task callbacks
- `System.currentTimeMillis()` delta for timing; no external timing library

### Integration Points
- `previewPane` (StackPane, fx:id) — Phase 6 scaffolds `Consumer<byte[]>` seam; Phase 7 fills it with PDFViewerFX
- `logListView` (ListView<String>, fx:id) — Phase 6 writes formatted error strings; Phase 8 replaces
- `statusLabel` (Label, fx:id) — shared progress/status output
- `menuBar` — Phase 6 adds "Run" menu with "Render" item + F5 accelerator
- `BorderPane` top zone — Phase 6 adds ToolBar (wrap MenuBar + ToolBar in a VBox in `<top>`)

</code_context>

<specifics>
## Specific Ideas

- Render button label toggle `"Render"` → `"Rendering..."` preferred over spinner (simpler, no new UI nodes)
- Status label: persistent `"Rendering..."` during Task, then `"Render complete (X.Xs)"` / `"Render failed"` with 3s auto-clear
- Error format in logListView: `"[ERROR] {type}: {message}"` + optional `" @ {file}:{line}"`
- Internal developer tool — utility over polish; no custom icons required for Phase 6

</specifics>

<deferred>
## Deferred Ideas

- Cancel button / Task.cancel() support — out of scope for Phase 6
- LogManager observable binding to logListView — Phase 8 responsibility
- PDFViewerFX actual rendering — Phase 7
- ProgressIndicator spinner in toolbar — skipped in favor of status label text

</deferred>

---

*Phase: 06-render-pipeline-integration*
*Context gathered: 2026-04-19*
