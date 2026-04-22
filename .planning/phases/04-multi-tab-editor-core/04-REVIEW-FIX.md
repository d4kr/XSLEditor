---
phase: 04-multi-tab-editor-core
fixed_at: 2026-04-18T00:00:00Z
review_path: .planning/phases/04-multi-tab-editor-core/04-REVIEW.md
iteration: 1
findings_in_scope: 2
fixed: 2
skipped: 0
status: all_fixed
---

# Phase 04: Code Review Fix Report

**Fixed at:** 2026-04-18
**Source review:** .planning/phases/04-multi-tab-editor-core/04-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 2 (WR-01, WR-02; IN-01 excluded by fix_scope=critical_warning)
- Fixed: 2
- Skipped: 0

## Fixed Issues

### WR-01: EditorTab listener leaks on close — CodeArea retained indefinitely

**Files modified:** `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java`
**Commit:** 0a5a9ec
**Applied fix:**
- Added `import javafx.beans.value.ChangeListener;`
- Updated `buildTab` signature from `buildTab(EditorTab)` to `buildTab(Path key, EditorTab editorTab)` so the registry key is available inside the method
- Replaced the anonymous lambda on `editorTab.dirty` with a named `ChangeListener<Boolean> dirtyListener` variable
- Moved `tab.setOnClosed` from `openOrFocusTab` into `buildTab` (where `dirtyListener` is in scope) and added `editorTab.dirty.removeListener(dirtyListener)` as the first statement, followed by `registry.remove(key)` and `updateAppDirtyState()`
- The `openOrFocusTab` call site updated to pass `key` as first argument to `buildTab`

This eliminates the strong reference chain `BooleanBinding → ChangeListener → lambda captures (EditorTab, CodeArea, UndoManager)` that previously survived tab close, preventing GC across repeated open/close cycles.

### WR-02: `updateTitle` silently no-ops when `primaryStage` is null

**Files modified:** `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java`
**Commit:** 7b3e746
**Applied fix:**
- Expanded the single-line `if (primaryStage == null) return;` into a block form with an inline comment explaining the contract: `setPrimaryStage()` is always called before any user-triggered path reaches `updateTitle()`, so hitting this guard signals a call-ordering regression
- Comment also suggests promoting the guard to `IllegalStateException` during development to make violations immediately visible rather than silently swallowed

---

_Fixed: 2026-04-18_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
