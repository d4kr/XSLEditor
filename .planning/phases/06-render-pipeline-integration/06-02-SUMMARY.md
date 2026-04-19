---
phase: 06-render-pipeline-integration
plan: 02
subsystem: ui
tags: [javafx, task, rendercontroller, maincontroller, fxml, toolbar, preview]

# Dependency graph
requires:
  - phase: 06-01
    provides: RenderController skeleton, EditorController.saveAll(), RenderControllerTest stubs
  - phase: 04-multi-tab-editor-core
    provides: EditorController with openOrFocusTab and saveAll
  - phase: 05-editor-features-syntax-navigation
    provides: SearchDialog, findInFilesMenuItem wiring
provides:
  - Full RenderController.handleRender() with JavaFX Task<Preview> lifecycle
  - Render button in ToolBar (below MenuBar) wired to MainController.handleRender()
  - Run > Render menu item with F5 accelerator
  - MainController RenderController instance with full initialize() wiring
  - Phase 7 seams: pdfCallback and outdatedCallback as no-op lambdas
affects:
  - 06-03 (PDF display): pdfCallback seam ready to wire
  - 07-pdf-preview: outdatedCallback seam ready to wire

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "JavaFX Task<Preview> on daemon thread — setOnSucceeded/setOnFailed on FX thread, no Platform.runLater needed"
    - "disableProperty().unbind() before setDisable(true); rebind in setOnSucceeded/setOnFailed"
    - "Sub-controller (RenderController) initialized from MainController.initialize() after FXML injection"
    - "Persistent statusSet vs transient statusTransient consumers for different feedback lifetimes"

key-files:
  created: []
  modified:
    - src/main/java/ch/ti/gagi/xlseditor/ui/RenderController.java
    - src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java
    - src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml

key-decisions:
  - "unbind disableProperty before Task spawn; rebind in setOnSucceeded and setOnFailed to restore project-loaded guard"
  - "Re-bind (not just re-enable) renderButton after Task to restore REND-02 base-case guard"

patterns-established:
  - "Task<Preview> lifecycle: unbind property → disable → spawn daemon thread → restore binding in callbacks"

requirements-completed:
  - REND-01
  - REND-02
  - REND-03
  - REND-04
  - REND-05
  - REND-06

# Metrics
duration: 15min
completed: 2026-04-19
---

# Phase 06 Plan 02: Render Pipeline Integration Wave 1 Summary

**Full render pipeline wired end-to-end: Task<Preview> on daemon thread, toolbar render button + F5 menu item, success/failure routing to log/status/PDF seam**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-04-19
- **Completed:** 2026-04-19
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- `RenderController.handleRender()` fully implemented with complete Task<Preview> lifecycle: null guard (REND-02), log clear (D-17), saveAll with abort (D-09), button disable with unbind/rebind pattern, daemon thread, success routing (D-12, D-14, D-15), failure routing (D-13, D-16), and setOnFailed fallback
- `main.fxml` updated: MenuBar + ToolBar wrapped in VBox in `<top>`; Run menu populated with `menuItemRender` + F5 accelerator; `renderButton` in ToolBar, both wired to `#handleRender`
- `MainController` wired: @FXML fields for `renderButton` and `menuItemRender`, `RenderController` field, `renderController.initialize()` at end of `initialize()`, `handleRender()` delegation; Phase 7 seams as no-op lambdas ready for wiring

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement RenderController.handleRender()** - `53d777b` (feat)
2. **Task 2: Modify main.fxml and wire MainController** - `c92455b` (feat)

## Files Created/Modified

- `src/main/java/ch/ti/gagi/xlseditor/ui/RenderController.java` - Full handleRender() with Task<Preview> lifecycle, button state management, result routing
- `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` - Added renderButton/@FXML, menuItemRender/@FXML, RenderController field, initialize() wiring, handleRender() action handler
- `src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml` - MenuBar+ToolBar wrapped in VBox, Run menu with Render item + F5, renderButton in ToolBar

## Decisions Made

- **unbind before disable, rebind after Task**: `renderButton.disableProperty()` is bound in `initialize()` to `projectContext.projectLoadedProperty().not()`. JavaFX throws `RuntimeException` if you call `setDisable()` on a bound property. Solution: `unbind()` before Task spawn, then rebind in both `setOnSucceeded` and `setOnFailed` to restore the REND-02 base-case guard. This was not explicitly called out in the plan — identified and fixed inline as Rule 1 (correctness).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Unbind disableProperty before Task spawn**

- **Found during:** Task 1 (RenderController.handleRender() implementation)
- **Issue:** `renderButton.disableProperty()` is bound to `projectContext.projectLoadedProperty().not()` in `initialize()`. Calling `renderButton.setDisable(true)` on a bound property throws `RuntimeException: A bound value cannot be set`.
- **Fix:** Call `renderButton.disableProperty().unbind()` before `setDisable(true)`. Re-bind with `renderButton.disableProperty().bind(projectContext.projectLoadedProperty().not())` in both `setOnSucceeded` and `setOnFailed` handlers to restore the project-loaded guard after the Task completes.
- **Files modified:** `src/main/java/ch/ti/gagi/xlseditor/ui/RenderController.java`
- **Verification:** `./gradlew build` exits 0
- **Committed in:** `53d777b` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 - correctness bug)
**Impact on plan:** Essential for correct JavaFX property management. No scope creep.

## Issues Encountered

None beyond the binding deviation above.

## Known Stubs

- `pdfCallback`: `bytes -> { }` no-op in Phase 6. Phase 7 (PDF preview) wires actual PDF display.
- `outdatedCallback`: `b -> { }` no-op in Phase 6. Phase 7 wires outdated-preview indicator.

## Threat Surface Scan

No new network endpoints, auth paths, or file access patterns introduced beyond what the plan's threat model covers (T-06-03, T-06-04, T-06-05 all addressed).

## Next Phase Readiness

- Render button and pipeline are fully functional end-to-end
- `./gradlew build` green (compile + tests)
- Phase 7 can wire `pdfCallback` and `outdatedCallback` seams to display PDF bytes in the WebView
- No blockers

---
*Phase: 06-render-pipeline-integration*
*Completed: 2026-04-19*
