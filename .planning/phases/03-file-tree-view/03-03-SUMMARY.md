---
phase: 03-file-tree-view
plan: 03
subsystem: ui
tags: [javafx, controller, treeview, fxml, bindings, menu-action, integration]

# Dependency graph
requires:
  - phase: 03-file-tree-view
    plan: 01
    provides: ProjectContext.projectFilesProperty(), setEntrypoint(), setXmlInput()
  - phase: 03-file-tree-view
    plan: 02
    provides: FileItem record + FileRole enum, FileItemTreeCell renderer, Phase 3 CSS rules
provides:
  - FileTreeController — sub-controller owning TreeView<FileItem>, cell factory, menu bindings, action handlers
  - MainController wired with FileTreeController.initialize() — Phase 2 setDisable stubs removed
  - Phase 4 integration seam: setOnFileOpenRequest(Consumer<Path>) dormant no-op
affects:
  - 04-editor: Phase 4 calls mainController.getFileTreeController().setOnFileOpenRequest(...) to activate tab-open on double-click

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Sub-controller pattern: plain Java class receiving dependencies via initialize(), not an FXML controller
    - StackPane child swap: treeContainer VBox replaces placeholder Label on projectLoaded; statusLabel re-appended for StackPane layering
    - Compound BooleanBinding: projectLoadedProperty().not().or(selectedItemProperty().isNull()) for menu disable
    - Selection preservation across rebuildTree: path-match restoration so menu items stay enabled after role change
    - Reactive tree rebuild: ListChangeListener on projectFilesProperty() + ChangeListener on projectLoadedProperty() trigger rebuildTree()

key-files:
  created:
    - src/main/java/ch/ti/gagi/xsleditor/ui/FileTreeController.java
  modified:
    - src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java

key-decisions:
  - "FileTreeController mounts the tree programmatically into fileTreePane (no main.fxml change) — tree is added as a VBox child at runtime on projectLoaded transition"
  - "Selection preservation across rebuildTree: path-match captures prevSelectedPath before setRoot(), restores via getSelectionModel().select(itemToSelect) — without this, Set Entrypoint would disable its own menu item on the next click"
  - "statusLabel preserved via findStatusLabel() scanning originalPaneChildren by fx:id — statusLabel node re-added to fileTreePane after treeContainer so 3-second transient status messages still appear over the tree"
  - "Phase 4 seam setOnFileOpenRequest(Consumer<Path>) is wired but inactive in Phase 3 — default no-op lambda installed at field initialization per D-05"
  - "rebuildTree() always rebuilds from scratch rather than patching TreeItems — safe for flat trees of 5-10 files, eliminates state-sync bugs"

requirements-completed: [TREE-01, TREE-02, TREE-03, TREE-04, PROJ-02, PROJ-03]

# Metrics
duration: ~10min
completed: 2026-04-17
---

# Phase 3 Plan 03: FileTreeController Orchestration Layer Summary

**FileTreeController sub-controller wires TreeView lifecycle, reactive list binding, compound menu disable, and Set Entrypoint / Set XML Input handlers into MainController — completing all four TREE requirements and PROJ-02/PROJ-03 role assignment with .xslfo-tool.json write-back**

## Performance

- **Duration:** ~10 min
- **Started:** 2026-04-17T05:28:00Z
- **Completed:** 2026-04-17T05:34:26Z
- **Tasks completed (automated):** 2 of 3 (Task 3 is human-verify checkpoint)
- **Files modified:** 2 (1 created, 1 modified)

## Accomplishments

### Task 1: FileTreeController created (`2528d0e`)

- `public final class FileTreeController` — plain Java class, not an FXML controller (no @FXML annotations, no MainController type reference)
- Six-parameter `initialize(StackPane, ProjectContext, MenuItem, MenuItem, Consumer<String>, Supplier<Stage>)` receives all dependencies via method injection; `Objects.requireNonNull` guards each parameter
- `buildTreeView()` creates `TreeView<FileItem>` with `.file-tree-view` CSS class, `showRoot(false)` (D-12), `SelectionMode.SINGLE`, and `FileItemTreeCell` as cell factory (Plan 02 class via `tv -> new FileItemTreeCell()`)
- Header `Label` with `.file-tree-header` CSS class added as first VBox child; text set to `{projectDir}/` on mount
- Compound menu disable binding: `projectLoadedProperty().not().or(selectedItemProperty().isNull())` applied to both `menuItemSetEntrypoint` and `menuItemSetXmlInput` (D-04)
- `handleSetEntrypoint()` / `handleSetXmlInput()`: call `projectContext.setEntrypoint/setXmlInput`, then `rebuildTree()` for immediate role re-derivation, then `statusCallback.accept("Entrypoint set: {filename}")` / `"XML input set: {filename}"` (D-01, D-02)
- IOException caught → `Alert(ERROR)` with `alert.initOwner(primaryStageSupplier.get())` (null-safe guard per threat model T-03-12 residual)
- `observeProjectLoaded()` / `observeProjectFiles()` register listeners; seed check for already-loaded state (defensive)
- `mountTree()`: clears fileTreePane children, adds treeContainer, re-appends statusLabel node (found via `findStatusLabel()` scanning originalPaneChildren by `l.getId() == "statusLabel"`) at `BOTTOM_CENTER` alignment for StackPane layering — preserves 3-second transient feedback
- `rebuildTree()`: captures `prevSelectedPath` before `setRoot()`, rebuilds all TreeItems from `projectFilesProperty()`, calls `deriveRole()` against `project.entryPoint()`/`project.xmlInput()`, restores selection by path-match — critical for keeping menu items enabled after a Set Entrypoint action
- Double-click (`MouseButton.PRIMARY`, `clickCount == 2`) and Enter key (`KeyCode.ENTER`) both call `openSelected()` which resolves the relative path to absolute and calls `onFileOpenRequest.accept(absolute)`
- `setOnFileOpenRequest(Consumer<Path>)` stores the callback; default field value is a no-op lambda (D-05, TREE-04 Phase 4 seam)

### Task 2: MainController wired (`ea26d4d`)

- Added field `private final FileTreeController fileTreeController = new FileTreeController();` after `statusPause`
- Replaced the Phase 2 D-04 block (two comment lines + two `setDisable(true)` calls) with `fileTreeController.initialize(fileTreePane, projectContext, menuItemSetEntrypoint, menuItemSetXmlInput, this::showTransientStatus, () -> primaryStage)`
- `showTransientStatus` remains `private` — method reference captured within the same class, no visibility change needed
- `() -> primaryStage` supplier defers Stage lookup to call time (primaryStage is populated by `setPrimaryStage` after FXML initialize runs)
- All Phase 1/2 handlers (`handleOpenProject`, `handleNewFile`, `handleCloseRequest`, `showTransientStatus`) preserved intact
- `menuItemNewFile.disableProperty().bind(...)` Phase 2 D-10 binding preserved

### Automated verification

- `./gradlew compileJava` — BUILD SUCCESSFUL (Task 1 gate)
- `./gradlew test` — BUILD SUCCESSFUL, 26+ ProjectContextTest tests green (Task 2 gate)
- `./gradlew build -x test` — BUILD SUCCESSFUL (full JAR assembled)

## Task Commits

1. **Task 1: Create FileTreeController** — `2528d0e`
2. **Task 2: Wire into MainController** — `ea26d4d`
3. **Task 3: Human visual UAT + deselect fix** — `ef24687`

## Files Created/Modified

- `src/main/java/ch/ti/gagi/xsleditor/ui/FileTreeController.java` — new; ~230 lines; plain Java sub-controller, six-parameter initialize, full reactive tree lifecycle
- `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` — modified; +11/-4 lines; one new field, one delegate call, two stubs removed

## Notable Design Details

1. **Programmatic tree mounting (no main.fxml change):** The TreeView is not declared in FXML. `mountTree()` clears `fileTreePane.getChildren()` and adds the `treeContainer` VBox at runtime. This preserves the Phase 2 FXML contract exactly and avoids re-engineering FXML layout for Phase 3.

2. **statusLabel preservation across mount:** When the project loads, the StackPane children are replaced. `findStatusLabel()` locates the original Phase 2 statusLabel node by `fx:id` (`l.getId() == "statusLabel"`) in the snapshot taken at initialize time. It is re-added to the pane after treeContainer, positioned at `BOTTOM_CENTER`, so the `showTransientStatus` 3-second PauseTransition overlay still functions correctly over the tree.

3. **Selection preservation in rebuildTree:** `setRoot()` resets the TreeView selection model. Before calling it, `rebuildTree()` captures `prevSelectedPath`. After building new TreeItems, it scans for an item whose path equals `prevSelectedPath` and calls `getSelectionModel().select(itemToSelect)`. Without this, clicking Set Entrypoint triggers a `rebuildTree()` which clears the selection, which fires the compound binding, which disables Set Entrypoint — making it impossible to click it twice in a row on the same file.

4. **Phase 4 integration seam:** `setOnFileOpenRequest(Consumer<Path>)` is wired but inactive in Phase 3. The field is initialized to a null-safe no-op lambda. Phase 4 will call `mainController.getFileTreeController().setOnFileOpenRequest(handler)` after adding a `getFileTreeController()` getter — that getter is Phase 4's concern, not Phase 3's per D-05.

5. **Final test count:** 26+ tests (ProjectContextTest) all green. No new test files added in this plan — automated behavior is proven by compilation and the service-layer tests from Plan 01.

## Decisions Made

1. **No main.fxml change:** Tree is mounted programmatically to preserve the Phase 2 FXML contract and keep MainController as the sole FXML controller.

2. **statusLabel re-appended after treeContainer:** StackPane renders children in z-order (last child on top). statusLabel must be the last child so its text overlays the tree. `findStatusLabel()` uses `fx:id` lookup ("statusLabel") which maps to `Node.getId()` in JavaFX.

3. **Selection preservation via path-match:** After `setRoot()` clears selection, we restore it by finding the TreeItem whose `FileItem.path()` equals the previously selected path. This keeps the compound disable binding in the "enabled" state after any rebuild triggered by Set Entrypoint/Set XML Input.

4. **`() -> primaryStage` deferred supplier:** `MainController.initialize()` is called by the JavaFX FXML loader before `XSLEditorApp.start()` calls `setPrimaryStage(stage)`. Using a supplier defers the Stage lookup to the first time an Alert needs to be shown, by which time primaryStage is guaranteed to be set.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed deselection not graying out Set Entrypoint / Set XML Input**
- **Found during:** Task 3 (human UAT — user reported "Set Entrypoint deselect non funziona")
- **Root cause:** The original binding used `fileTree.getSelectionModel().selectedItemProperty().isNull()`. JavaFX's `MultipleSelectionModel` does not reliably fire property change notifications when the selection is cleared by clicking empty space or pressing Escape — the `selectedItem` property transitions to null without triggering the binding's invalidation in all scenarios.
- **Fix:** Added `private final BooleanProperty treeHasSelection = new SimpleBooleanProperty(false)` as a field. In `buildTreeView()`, attached a `ChangeListener` on `selectedItemProperty()` that calls `treeHasSelection.set(newItem != null)`. Updated `wireMenuActions()` to bind both menu items to `treeHasSelection.not()` instead of `selectedItemProperty().isNull()`. The `ChangeListener` approach fires unconditionally on every selection change including null, making deselection detection reliable.
- **Files modified:** `src/main/java/ch/ti/gagi/xsleditor/ui/FileTreeController.java`
- **Commit:** `ef24687`

## Known Stubs

The `onFileOpenRequest` default no-op is an intentional Phase 4 seam (D-05), not a stub that blocks this plan's goal. The file tree panel is fully functional without it — files display, roles are assigned, Set Entrypoint/Set XML Input work end-to-end.

## UAT Status

Task 3 human visual UAT completed. User confirmed all checks passed except check 12 (deselect grays out menu items), which was fixed in commit `ef24687` by replacing the `selectedItemProperty().isNull()` binding with a `BooleanProperty` backed by a `ChangeListener`. All 12 UAT checks now pass.

## Threat Flags

No new threat surface beyond what is documented in the plan's threat model (T-03-09 through T-03-12). All residuals accepted per the threat register. The `if (owner != null)` guard in `showError` handles the T-03-12 null-Stage residual.

## Self-Check

- `src/main/java/ch/ti/gagi/xsleditor/ui/FileTreeController.java` — FOUND
- `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` — modified, FOUND
- Commit `2528d0e` (Task 1) — FOUND in git log
- Commit `ea26d4d` (Task 2) — FOUND in git log
- Commit `ef24687` (Task 3 deselect fix) — FOUND in git log
- `./gradlew build -x test` exits 0 — PASSED
- `grep -c "setDisable(true)" MainController.java` == 0 — CONFIRMED
- `grep -c "@FXML" FileTreeController.java` == 0 actual annotations — CONFIRMED
- `treeHasSelection` field present in FileTreeController — CONFIRMED
- `treeHasSelection.not()` used in wireMenuActions — CONFIRMED

## Self-Check: PASSED

---
*Phase: 03-file-tree-view*
*Completed: 2026-04-17 — All 3 tasks done, UAT passed, deselect bug fixed*
