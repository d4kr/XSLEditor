---
phase: 08-error-log-panel
plan: "01"
subsystem: log-panel-ui
tags: [javafx, log-panel, tableview, filterlist, ui]
dependency_graph:
  requires: []
  provides: [LogController, LogEntry-extended, log-panel-fxml, log-panel-css]
  affects: [MainController, RenderController]
tech_stack:
  added: []
  patterns: [FilteredList, ToggleGroup, TableCell-cell-factory, sub-controller-lifecycle]
key_files:
  created:
    - src/main/java/ch/ti/gagi/xsleditor/ui/LogController.java
  modified:
    - src/main/java/ch/ti/gagi/xsleditor/log/LogEntry.java
    - src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
    - src/main/resources/ch/ti/gagi/xsleditor/ui/main.css
decisions:
  - "D-01/D-02: TableView with four columns; Level column uses CSS class cell factory for severity color"
  - "D-03/D-04: LogEntry extended with nullable type/file/Integer line; backwards-compatible 3-arg constructor delegates to 6-arg"
  - "D-05/D-06: LogController is a sub-controller (not @FXML); setErrors() IS the Consumer<List<PreviewError>>"
  - "D-08: addInfo(String) routes plain INFO messages without file/line context"
  - "D-09/D-11: ToggleGroup with non-null guard prevents deselecting active filter button"
  - "D-10: FilteredList<LogEntry> wraps ObservableList for live predicate-based filtering"
  - "D-12: logPane.setExpanded(true) auto-triggers when setErrors produces any ERROR or WARN entry"
  - "D-14/D-15: row click calls editorController.navigateTo(Path.of(file), line-1, 0); no-op for entries without file/line"
  - "D-17/D-18: FXML replaces ListView with VBox containing HBox filter bar + TableView; logPane unchanged"
metrics:
  duration: "~20 minutes"
  completed: "2026-04-20"
  tasks_completed: 3
  tasks_total: 3
---

# Phase 08 Plan 01: Log Panel UI — TableView, Filter Bar, Severity Colors Summary

Extended `LogEntry` with optional type/file/line fields, created `LogController` sub-controller owning the log `TableView<LogEntry>` with `FilteredList` predicate filtering and severity color cell factory, and replaced the `logPane` `ListView` in FXML with a VBox filter bar + TableView with Phase 8 CSS.

## Tasks Completed

| Task | Name | Commit | Key Files |
|------|------|--------|-----------|
| 1 | Extend LogEntry with optional type/file/line fields | c711061 | LogEntry.java |
| 2 | Create LogController with TableView, filter bar, cell factory, row click navigation | f29fde3 | LogController.java |
| 3 | Replace logPane inner content in FXML + add Phase 8 CSS | 246e121 | main.fxml, main.css |

## Requirements Implemented

| Requirement | Implementation |
|-------------|----------------|
| ERR-01 | TableView<LogEntry> with Time/Level/Type/Message columns (colTime, colLevel, colType, colMessage) |
| ERR-02 | Level column cell factory applies .log-error (red) / .log-warn (yellow) CSS classes |
| ERR-03 | FilteredList<LogEntry> with ToggleGroup predicate; All/Error/Warning/Info buttons |
| ERR-04 | Row click handler calls editorController.navigateTo(Path.of(file), line-1, 0) when file+line non-null |
| ERR-05 | setErrors() calls allEntries.clear() before populating |

## Decisions Implemented

- **D-01/D-02**: TableView with four columns; Level column uses programmatic cell factory with `getStyleClass().removeAll("log-error","log-warn")` followed by conditional add — no inline styles
- **D-03/D-04**: `LogEntry` extended with `String type`, `String file`, `Integer line` (boxed, nullable per project convention). Six-arg primary constructor; three-arg delegate calls `this(..., null, null, null)`
- **D-05**: `LogController` follows sub-controller lifecycle pattern — `public final class`, not `@FXML`, all UI nodes injected via `initialize()`, every param guarded with `Objects.requireNonNull`
- **D-06**: `setErrors(List<PreviewError>)` IS the `Consumer<List<PreviewError>>` that Plan 02 wires as `logController::setErrors` in `MainController.initialize()`
- **D-08**: `addInfo(String message)` creates `LogEntry` with level `"INFO"` and no file/line context
- **D-09/D-11**: ToggleGroup with `selectedToggleProperty` listener — if `newVal == null && oldVal != null`, re-selects `oldVal` preventing deselection
- **D-10**: `filteredEntries = new FilteredList<>(allEntries, e -> true)` bound to `logTableView.setItems(filteredEntries)`
- **D-12**: After `setErrors()` populates `allEntries`, checks `anyMatch(e -> "ERROR".equals(e.level()) || "WARN".equals(e.level()))` and calls `logPane.setExpanded(true)`
- **D-14/D-15**: `logTableView.setOnMouseClicked` — PRIMARY button only; `entry.line() - 1` converts 1-based `PreviewError.line` to 0-based `navigateTo` parameter; no-ops silently for entries with null file or line
- **D-17/D-18**: FXML `logPane` inner content replaced: `<ListView fx:id="logListView"/>` → `<VBox>` containing `<HBox styleClass="log-filter-bar">` with 4 `ToggleButton` + `<TableView fx:id="logTableView">` with 4 `TableColumn` nodes

## Intentional FXML / MainController Mismatch

After Task 3, `MainController` still declares `@FXML private ListView<String> logListView` (line 48) and references it in `handleOpenProject` and `renderController.initialize()`. The FXML no longer contains `fx:id="logListView"`, so `FXMLLoader.load()` will fail at runtime. This is **intentional and expected**: Plan 02 Task 1 removes the `logListView` field, rewires `MainController.initialize()` to call `logController.initialize(...)`, and updates `renderController.initialize()` to accept `Consumer` callbacks. The project compiles cleanly — the mismatch is a runtime-only concern resolved in the next plan.

## Deviations from Plan

None — plan executed exactly as written.

## Compile Verification

```
./gradlew compileJava  →  BUILD SUCCESS (exit 0)
```

All three tasks verified:
- `LogEntry` — 6-arg constructor, 3-arg delegate, `type()` / `file()` / `line()` accessors
- `LogController` — `setErrors`, `addInfo`, `initialize`, `entries` methods; no `@FXML` annotations; `FilteredList`, ToggleGroup guard, `logPane.setExpanded(true)`, `editorController.navigateTo` wiring all present
- FXML — `logTableView` present, `logListView` absent, 4 filter buttons, 4 columns, `log-filter-bar` styleClass
- CSS — Phase 8 block with `.log-error`, `.log-warn`, `.log-table-view`, `.log-filter-bar`, `.log-filter-btn`, `.log-filter-btn:selected`

## Self-Check: PASSED

Files verified:
- FOUND: src/main/java/ch/ti/gagi/xsleditor/log/LogEntry.java
- FOUND: src/main/java/ch/ti/gagi/xsleditor/ui/LogController.java
- FOUND: src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
- FOUND: src/main/resources/ch/ti/gagi/xsleditor/ui/main.css

Commits verified:
- FOUND: c711061 (feat(08-01): extend LogEntry)
- FOUND: f29fde3 (feat(08-01): create LogController)
- FOUND: 246e121 (feat(08-01): replace logPane ListView)
