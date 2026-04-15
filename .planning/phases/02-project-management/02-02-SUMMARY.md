---
phase: 02-project-management
plan: "02"
subsystem: ui
tags: [javafx, fxml, css, menu, project-management, controller, fxml-binding]

dependency_graph:
  requires:
    - phase: 02-01
      provides: ProjectContext state service with BooleanProperty, openProject(), createFile(), path-traversal guard
  provides:
    - File menu with Open Project..., Set Entrypoint (disabled), Set XML Input (disabled), New File..., Exit
    - statusLabel transient feedback — 3-second PauseTransition success message in fileTreePane
    - main.css Phase 2 rules — .status-label-success (#66bb6a), .menu-item:disabled (.label #666666)
    - MainController.handleOpenProject — DirectoryChooser, openProject delegation, title update, log branching
    - MainController.handleNewFile — TextInputDialog, createFile delegation, tri-catch error handling
    - Reactive disable binding: menuItemNewFile bound to projectContext.projectLoadedProperty().not()
    - D-04 scaffold: menuItemSetEntrypoint and menuItemSetXmlInput unconditionally disabled in Phase 2
  affects:
    - Phase 03 (file tree) — will replace fileTreePane placeholder and enable Set Entrypoint / Set XML Input bindings
    - Phase 07 (preview) — render is safe only when entryPoint is set; see D-03 TODO in RenderOrchestrator

tech-stack:
  added: []
  patterns:
    - PauseTransition 3-second transient status label pattern (showTransientStatus helper)
    - Reactive MenuItem disable binding via BooleanProperty.not() in initialize()
    - Unconditional setDisable(true) for Phase 2 D-04 scaffold items (no observable state to bind)
    - Tri-catch order: IllegalArgumentException -> FileAlreadyExistsException -> IOException (subclass before superclass)
    - All new Alert instances use initOwner(primaryStage) + showAndWait() — zero use of show()

key-files:
  created: []
  modified:
    - src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml
    - src/main/resources/ch/ti/gagi/xlseditor/ui/main.css
    - src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java

key-decisions:
  - "PROJ-02 / PROJ-03 (Set Entrypoint, Set XML Input) are scaffolded-only per D-04 — fx:id declared, no onAction, setDisable(true) in initialize(); Phase 3 enables them with tree-selection bindings"
  - "showTransientStatus extracted as private helper to avoid duplicating PauseTransition logic across future callers"
  - "handleNewFile catches IllegalArgumentException (blank/traversal) before FileAlreadyExistsException before IOException — required catch order for subclass specificity"
  - "T-02-01 mitigation chain: handleNewFile -> projectContext.createFile -> validateSimpleFilename; no secondary validation in controller (single point of enforcement)"

patterns-established:
  - "Alert pattern: always initOwner(primaryStage) + showAndWait(), never show() — established Phase 1, confirmed Phase 2"
  - "PauseTransition status label: removeAll + add style class, set text, pause.setOnFinished clears both"

requirements-completed: [PROJ-01, PROJ-02, PROJ-03]

duration: 15min
completed: "2026-04-15"
---

# Phase 02 Plan 02: UI Layer for Project Management Summary

**File menu wired to ProjectContext API: Open Project with DirectoryChooser, transient 3-second statusLabel feedback, reactive New File binding, and D-04 disabled scaffold for Set Entrypoint / Set XML Input.**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-04-15T21:43:00Z
- **Completed:** 2026-04-15T21:58:18Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- FXML: replaced Phase 1 File menu stub with five items (Open Project..., Set Entrypoint, Set XML Input, New File..., Exit) and three SeparatorMenuItems, all with correct fx:id / onAction per D-04
- CSS: appended `.status-label-success` (#66bb6a) and `.menu-item:disabled .label` (#666666) rules to main.css
- MainController: full `handleOpenProject` handler (DirectoryChooser, null-guard cancel, openProject delegation, updateTitle, 3-second PauseTransition feedback, log panel branching for full/partial/no config)
- MainController: full `handleNewFile` handler (TextInputDialog, createFile delegation, tri-catch in correct subclass-first order)
- Reactive `menuItemNewFile` disable binding to `projectContext.projectLoadedProperty().not()`

## Task Commits

1. **Task 1: Add File menu items, statusLabel, and Phase 2 CSS styles** - `bc0a4c6` (feat)
2. **Task 2: Wire ProjectContext, implement handleOpenProject/handleNewFile, bind disable states** - `b3f6ee5` (feat)

## Files Created/Modified

- `src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml` — File menu replaced, fileTreePane placeholder copy updated, statusLabel added
- `src/main/resources/ch/ti/gagi/xlseditor/ui/main.css` — Two Phase 2 CSS rules appended
- `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` — New imports, @FXML injections, ProjectContext field, initialize() bindings, handleOpenProject, handleNewFile, showTransientStatus

## Decisions Made

- **PROJ-02 / PROJ-03 scaffolded-only per D-04:** `menuItemSetEntrypoint` and `menuItemSetXmlInput` have `fx:id` (required for FXML injection) but no `onAction` and are `setDisable(true)` in `initialize()`. Phase 3 will replace `setDisable(true)` with tree-selection-driven observable bindings.
- **`showTransientStatus` extracted as private helper:** The PauseTransition logic (removeAll + add style class, set text, onFinished clear) is encapsulated to prevent duplication. Future phases that need transient feedback can call it directly.
- **Tri-catch order enforced:** `IllegalArgumentException` → `FileAlreadyExistsException` → `IOException`. `FileAlreadyExistsException extends IOException` — wrong order would silently swallow the specific warning.

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None.

## Threat Surface Notes

- **T-02-01 (inherited):** `handleNewFile` relies entirely on `ProjectContext.createFile` → `validateSimpleFilename` for path-traversal prevention. No secondary validation in the controller; this is the intended single point of enforcement. Verified by Plan 01's `ProjectContextTest.createFileRejectsParentTraversal`, `createFileRejectsSubdirectory`, and `createFileRejectsAbsolutePath` (all green).
- **T-02-06 (Alert.initOwner):** All 5 new `initOwner(primaryStage)` calls verified — 1 in `handleOpenProject`, 1 on TextInputDialog in `handleNewFile`, 3 on Alert instances in `handleNewFile`. Zero use of `.show()`.
- **Cross-cutting convention for future plans:** Any new `Alert` or dialog added to `MainController` MUST call `initOwner(primaryStage)` before `showAndWait()`.

## PROJ-02 / PROJ-03 Scaffolding Notice

PROJ-02 (Set Entrypoint) and PROJ-03 (Set XML Input) are present in the menu as disabled items per D-04. The Phase 2 scope delivers the menu scaffold only. Phase 3 (file tree view) must:
1. Replace `menuItemSetEntrypoint.setDisable(true)` with an observable binding to a tree selection property
2. Replace `menuItemSetXmlInput.setDisable(true)` with the same
3. Implement the `onAction` handlers for both items

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Phase 3 (file tree) can now mount a `TreeView` into `fileTreePane` and enable the scaffolded Set Entrypoint / Set XML Input menu items
- `ProjectContext.getCurrentProject()` is available for Phase 3 to retrieve the loaded project root path
- `MainController.updateTitle()` and `setDirty()` remain unmodified (Phase 1 contracts preserved)
- `./gradlew build` exits 0, all 14 Plan 01 unit tests green

---
*Phase: 02-project-management*
*Completed: 2026-04-15*
