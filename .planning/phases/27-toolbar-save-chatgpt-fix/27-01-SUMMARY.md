---
phase: 27-toolbar-save-chatgpt-fix
plan: "01"
subsystem: ui
tags: [toolbar, save, javafx, fxml, undo-manager, dirty-tracking]
dependency_graph:
  requires:
    - 26-01 (rebindUndoRedo method and EditorTab.dirty BooleanBinding)
  provides:
    - saveButton in main.fxml toolbar wired to handleToolbarSave
    - EditorController.saveActiveTab() public method
    - EditorController.getActiveDirtyProperty() public method
    - rebindToolbarButtons() including saveButton.disableProperty binding
  affects:
    - MainController (saveButton field, handler, renamed rebind method)
    - EditorController (two new public methods)
    - main.fxml (saveButton node + Separator + tooltips on all 4 buttons)
tech_stack:
  added: []
  patterns:
    - JavaFX disableProperty().bind() to BooleanBinding (dirty state)
    - Bindings.createBooleanBinding with ObservableValue as invalidation source
    - getUserData() instanceof EditorTab pattern (established in Phase 25)
key_files:
  created: []
  modified:
    - src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java
    - src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java
    - src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
decisions:
  - "Renamed rebindUndoRedo() to rebindToolbarButtons() because saveButton is now part of the rebind set — more inclusive name is correct per plan CONTEXT.md discretion note"
  - "getActiveDirtyProperty() returns Optional<ObservableValue<Boolean>> (not BooleanBinding directly) to match the plan signature using the broader javafx.beans.value.ObservableValue supertype"
  - "Added ObservableValue import (javafx.beans.value.ObservableValue) to EditorController — was not previously imported"
metrics:
  duration: "~8 minutes"
  completed: "2026-05-01"
  tasks_completed: 3
  tasks_total: 3
  files_changed: 3
---

# Phase 27 Plan 01: Toolbar Save Button — Summary

**One-liner:** Toolbar 💾 Save button wired via saveActiveTab()+getActiveDirtyProperty() with dirty-state binding and tooltips on all four toolbar buttons.

## What Was Built

A 💾 Save button added to the main toolbar between the existing post-undo/redo Separator and the Render button. The button is disabled when no tab is open or when the active tab is clean (not dirty), and enabled when the active tab is dirty. Clicking it saves the active file to disk. Tooltips were added to all four toolbar buttons (Undo, Redo, Save, Render).

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Extend EditorController with saveActiveTab() and getActiveDirtyProperty() | bebce24 | EditorController.java |
| 2 | Add saveButton to main.fxml toolbar with tooltips on all four toolbar buttons | ff98f11 | main.fxml |
| 3 | Wire saveButton in MainController — @FXML field, handler, extended rebind | 754af6d | MainController.java |

## Implementation Details

### EditorController (Task 1)

Two new public methods added after `getActiveCodeArea()`:

- `saveActiveTab()` — resolves the selected tab via `tabPane.getSelectionModel().getSelectedItem()`, unwraps `EditorTab` via `getUserData() instanceof EditorTab`, delegates to existing `private saveTab(EditorTab)`. No-op when no tab is selected.
- `getActiveDirtyProperty()` — returns `Optional<ObservableValue<Boolean>>` wrapping the active `EditorTab.dirty` BooleanBinding. Used by MainController for the save button disable binding. Returns `Optional.empty()` when no tab is selected.

Import added: `javafx.beans.value.ObservableValue` (was not previously imported).

### main.fxml (Task 2)

Replaced the `<ToolBar>` block to add:
- `saveButton` with text `"💾"` wired to `#handleToolbarSave`
- A `<Separator/>` after `saveButton` (before `renderButton`)
- `<tooltip><Tooltip text="..."/>` on all four buttons (Undo, Redo, Save, Render)

Final toolbar layout: `[ ↺ ] [ ↻ ] | [ 💾 ] | [ Render ]`

### MainController (Task 3)

Four changes:
1. `@FXML private Button saveButton;` field added under Phase 27 additions comment
2. `handleToolbarSave()` action handler delegates to `editorController.saveActiveTab()`
3. `rebindUndoRedo()` renamed to `rebindToolbarButtons()` — both `initialize()` call sites updated
4. `rebindToolbarButtons()` extended to unbind/rebind `saveButton.disableProperty()` using `Bindings.createBooleanBinding(() -> !Boolean.TRUE.equals(dirtyProp.getValue()), dirtyProp)` from `editorController.getActiveDirtyProperty()`; null-tab path sets `saveButton.setDisable(true)`

## Deviations from Plan

None — plan executed exactly as written. The `rebindUndoRedo` → `rebindToolbarButtons` rename was explicitly called for in the plan.

## Known Stubs

None. All functionality is fully wired.

## Threat Surface Scan

No new network endpoints, auth paths, file access patterns, or trust boundary changes introduced. The only disk write path is `saveActiveTab()` → `saveTab(EditorTab)` → `Files.writeString(editorTab.path, ...)` using the original `EditorTab.path` established at file-open time — T-27-01 (accept) as per plan threat model.

T-27-02 (read-only file IOError) and T-27-03 (dirty binding leak across tabs) both mitigated via existing `saveTab()` error dialog and the `saveButton.disableProperty().unbind()` call at the top of `rebindToolbarButtons()`.

## Self-Check: PASSED

- EditorController.java: saveActiveTab() present (1), getActiveDirtyProperty() present (1), existing saveTab(EditorTab) preserved (1), existing getActiveCodeArea() preserved (1)
- main.fxml: saveButton (1), 💾 text (1), handleToolbarSave (1), Tooltip Save/Undo/Redo/Render (1 each), 2 Separators, saveButton line 88 > redoButton line 84 < renderButton line 92
- MainController.java: @FXML saveButton (1), handleToolbarSave (1), editorController.saveActiveTab (1), rebindToolbarButtons (3), rebindUndoRedo (0), saveButton.disableProperty().bind (1), saveButton.setDisable(true) (2), getActiveDirtyProperty (1)
- ./gradlew compileJava: BUILD SUCCESSFUL
- ./gradlew test: BUILD SUCCESSFUL (all tests pass)
