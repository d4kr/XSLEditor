---
phase: 26-undo-redo-system
fixed_at: 2026-04-30T00:00:00Z
review_path: .planning/phases/26-undo-redo-system/26-REVIEW.md
iteration: 1
findings_in_scope: 4
fixed: 4
skipped: 0
status: all_fixed
---

# Phase 26: Code Review Fix Report

**Fixed at:** 2026-04-30
**Source review:** .planning/phases/26-undo-redo-system/26-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 4 (WR-01 through WR-04; CR-* none; IN-* excluded by fix_scope)
- Fixed: 4
- Skipped: 0

## Fixed Issues

### WR-01: `setOnActiveTabChanged` adds a new listener every call — listeners accumulate

**Files modified:** `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java`
**Commit:** 59bdb85
**Applied fix:** Added a `private ChangeListener<Tab> activeTabListener` field. In `setOnActiveTabChanged`, the method now removes the stored listener (if non-null) before registering the new one, then saves the new listener to the field. Updated the Javadoc comment to accurately describe the replaced-listener semantics (resolves IN-02 as a side effect). This closes the latent runtime `IllegalArgumentException` that would occur on any tab switch if the method were called a second time.

---

### WR-02: `handleToolbarUndo` / `handleToolbarRedo` are exact duplicates of `handleEditUndo` / `handleEditRedo`

**Files modified:** `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java`
**Commit:** a472409
**Applied fix:** Extracted private `performUndo()` and `performRedo()` methods. All four `@FXML` handler methods now delegate to these shared helpers, eliminating the duplicated `getActiveCodeArea().ifPresent(CodeArea::undo/redo)` body.

---

### WR-03: `selectedTextProperty` anonymous listener in `buildTab` is never removed on tab close

**Files modified:** `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java`
**Commit:** 78d5bda
**Applied fix:** Replaced the anonymous lambda for occurrence highlighting with a named `ChangeListener<String> selectionListener` local variable. Added `editorTab.codeArea.selectedTextProperty().removeListener(selectionListener)` to the `tab.setOnClosed` handler so the CodeArea is released for GC when a tab is closed.

---

### WR-04: `rebindUndoRedo` binds `disableProperty` without guarding against a closed/GC-eligible `CodeArea`

**Files modified:** `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java`, `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java`
**Commit:** 6e0bc02
**Applied fix:** Added a `private Runnable tabClosedCallback` field and a public `setOnTabClosed(Runnable)` method to `EditorController`. The `tab.setOnClosed` handler now calls `tabClosedCallback.run()` as its last action. In `MainController.initialize()`, the callback is wired to `() -> rebindUndoRedo(Optional.empty())`, which immediately unbinds all four controls and disables them the moment any tab closes — before any selection-change event propagates — eliminating the stale UndoManager binding window.

---

_Fixed: 2026-04-30_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
