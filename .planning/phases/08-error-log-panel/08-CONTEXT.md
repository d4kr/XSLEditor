# Phase 8: Error & Log Panel - Context

**Gathered:** 2026-04-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Replace the `ListView<String>` stopgap (`logListView`) with a proper log panel backed by `LogManager`. The panel lives in the existing `TitledPane` (fx:id=`logPane`) at the BorderPane bottom. Deliverables: structured `TableView<LogEntry>` with timestamp/severity/type/message columns, severity filter buttons, auto-expand on ERROR/WARN, click-to-navigate for entries with file+line info.

Requirements in scope: ERR-01, ERR-02, ERR-03, ERR-04, ERR-05.

</domain>

<decisions>
## Implementation Decisions

### Component

- **D-01:** Use `TableView<LogEntry>` — not `ListView`. Four columns: Time (HH:mm:ss), Level (severity badge), Type, Message. Native column support fits ERR-02 exactly.
- **D-02:** Level column uses color-coded cell factory: ERROR → red text, WARN → yellow text, INFO → default color (per ROADMAP).

### LogEntry model

- **D-03:** Extend `LogEntry` with three optional fields: `String type`, `String file`, `Integer line`. All nullable. Backend `LogManager` stays as the single model — no separate UI wrapper. `RenderController` constructs rich `LogEntry` objects from `PreviewError` data when routing errors.
- **D-04:** Existing `LogManager.info()` / `warn()` / `error()` convenience methods remain for simple log messages (no file/line context). A new overload or constructor on `LogEntry` handles the richer case.

### Data flow

- **D-05:** Introduce a dedicated `LogController` sub-controller, following the same pattern as `RenderController` and `PreviewController`. `MainController` creates one instance and calls `logController.initialize(logPane, logTableView, ...)` from its own `initialize()`.
- **D-06:** `RenderController` receives a `Consumer<List<PreviewError>>` callback (instead of the current `ListView<String>` reference). `LogController` implements this callback — it converts `PreviewError` entries to `LogEntry` objects, populates its own `ObservableList<LogEntry>`, and handles ERR-05 (clear before render).
- **D-07:** `MainController` wires the callback: `renderController.initialize(..., logController::setErrors, ...)`. This is the same seam pattern used for `pdfCallback` and `outdatedCallback`.
- **D-08:** The `LogController`-owned `ObservableList<LogEntry>` is the single source of truth for the table. `LogManager` entries (simple info/warn messages like "Render complete in Xs") are also funneled through `LogController` — `RenderController` adds them via a second callback or `logController.addInfo(String)`.

### Severity filtering

- **D-09:** Four `ToggleButton` nodes in an `HBox` above the `TableView`, inside the `TitledPane`: **All**, **Error**, **Warning**, **Info**. Part of a `ToggleGroup` — only one active at a time, with "All" selected by default.
- **D-10:** Filter is implemented via `FilteredList<LogEntry>` wrapping the `ObservableList`. When a filter button is selected, the `FilteredList` predicate updates. ERR-03 requirement.
- **D-11:** "All" button cannot be deselected — if user clicks the active button, it stays selected (ToggleGroup `selectedToggle` stays non-null).

### Auto-expand

- **D-12:** `logPane.setExpanded(true)` when the incoming entry list contains any ERROR or WARN entry. Stays collapsed for INFO-only renders (e.g., "Render complete"). ERR-01 adjacent behavior — developer sees errors immediately without manual interaction.
- **D-13:** Panel never auto-collapses. User can collapse it manually at any time.

### Click-to-navigate (ERR-04)

- **D-14:** `TableView` row click handler: if the selected `LogEntry` has non-null `file` and `line`, call `editorController.navigateTo(Path.of(entry.file()), entry.line() - 1, 0)`. Note: `navigateTo` uses 0-based line index; `LogEntry.line()` stores 1-based (matching `PreviewError`).
- **D-15:** Entries without file/line (INFO messages, errors with no location) are clickable but do nothing on navigate.
- **D-16:** `LogController` receives `EditorController` reference via `initialize()` for the navigation callback.

### FXML changes

- **D-17:** Replace `<ListView fx:id="logListView" .../>` with a `VBox` containing:
  1. `HBox` with four `ToggleButton` nodes (filter bar)
  2. `TableView fx:id="logTableView"` with four `TableColumn` nodes
- **D-18:** Keep `TitledPane fx:id="logPane"` and its `prefHeight="150"` — just replace the inner content node. `expanded="false"` by default (LogController expands programmatically per D-12).

### Claude's Discretion

- Exact CSS for severity badge colors (red/yellow/default in Level column)
- Column widths and `prefWidth` values in FXML
- Whether `LogController.addInfo(String)` is a separate method or part of the `Consumer<List<PreviewError>>` callback signature (implementation detail)
- Whether `LogEntry` constructor overloads or uses a builder pattern for the optional fields
- Exact `ToggleButton` CSS styling (outline vs filled, spacing)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` §Error & Log Panel — ERR-01..ERR-05 (exact acceptance criteria)
- `docs/PRD.md` — Product requirements (error feedback, navigation from error to source line)

### Existing backend (do not re-implement)
- `src/main/java/ch/ti/gagi/xsleditor/log/LogManager.java` — In-memory log storage; Phase 8 extends `LogEntry` and routes through `LogController`
- `src/main/java/ch/ti/gagi/xsleditor/log/LogEntry.java` — **Extend this class** with optional `type`, `file`, `line` fields (D-03)
- `src/main/java/ch/ti/gagi/xsleditor/preview/PreviewError.java` — Source of `type()`, `file()`, `line()` data; converted to `LogEntry` by `LogController`

### Existing UI (integration points)
- `src/main/java/ch/ti/gagi/xsleditor/ui/RenderController.java` — Replace `ListView<String> logListView` parameter with `Consumer<List<PreviewError>>` callback (D-06); also add `Consumer<String>` for INFO messages (D-08)
- `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` — Wire `logController.initialize(...)` and replace `logListView` injection; all sub-controller wiring seams
- `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java` — `navigateTo(Path, int, int)` at line 156; called by `LogController` for click-to-navigate (D-14)
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` — `TitledPane fx:id="logPane"` at line 95; inner `ListView fx:id="logListView"` replaced by VBox+HBox+TableView (D-17)
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.css` — Add severity color rules and filter button styles

### Prior phase context
- `.planning/phases/06-render-pipeline-integration/06-CONTEXT.md` — D-16: `logListView` stopgap pattern being replaced; D-17: `logListView.getItems().clear()` pattern (ERR-05) moves to `LogController`
- `.planning/phases/07-pdf-preview-panel/07-CONTEXT.md` — deferred: "Log entries copyable" — Phase 8 should enable text selection in TableView cells

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `TitledPane fx:id="logPane"` — already at BorderPane bottom, `prefHeight="150"`, `expanded="false"`; keep the pane, replace inner content only
- `EditorController.navigateTo(Path, int, int)` — ready-to-use navigation seam; takes 0-based line index
- Sub-controller pattern: `initialize(node, ...)` — same shape as FileTreeController, EditorController, RenderController, PreviewController
- `Consumer<T>` callback pattern — established by `pdfCallback`/`outdatedCallback` in Phase 6/7; Phase 8 uses same pattern for error routing

### Established Patterns
- Dedicated sub-controller per concern (all prior UI phases)
- `@FXML` injection in `MainController.initialize()`, sub-controllers initialized via `controller.initialize(...)`
- `FilteredList<T>` wrapping `ObservableList<T>` — standard JavaFX pattern for live filtering
- `ToggleGroup` + `ToggleButton` for mutually exclusive filter buttons
- CSS class toggle for state-based styling (see `status-label-success` pattern)

### Integration Points
- `RenderController.initialize()` — signature changes: `ListView<String> logListView` parameter removed; two new Consumer params added (D-06/D-08)
- `MainController.initialize()` — add `logController` field, wire `logController.initialize(logPane, logTableView, editorController)`, replace no-op lambda patterns
- `main.fxml` — replace `<ListView fx:id="logListView">` with `<VBox>` containing filter HBox + TableView
- `main.css` — add `.log-error`, `.log-warn` CSS classes for severity coloring

</code_context>

<specifics>
## Specific Ideas

- From Phase 7 deferred: "Log entries copyable" — TableView with `setEditable(false)` + `setCellFactory` enabling text selection/copy on the Message column
- Timestamp display: `HH:mm:ss` format (short, fits narrow column)
- Filter buttons above table (not in TitledPane header line) — standard log viewer layout, chosen explicitly

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 08-error-log-panel*
*Context gathered: 2026-04-20*
