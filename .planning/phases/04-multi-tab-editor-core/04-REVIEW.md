---
phase: 04-multi-tab-editor-core
reviewed: 2026-04-18T00:00:00Z
depth: standard
files_reviewed: 4
files_reviewed_list:
  - src/main/java/ch/ti/gagi/xsleditor/ui/EditorTab.java
  - src/test/java/ch/ti/gagi/xsleditor/ui/EditorTabTest.java
  - src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java
  - src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java
findings:
  critical: 0
  warning: 2
  info: 1
  total: 3
status: issues_found
---

# Phase 04: Code Review Report

**Reviewed:** 2026-04-18
**Depth:** standard
**Files Reviewed:** 4
**Status:** issues_found

## Summary

Reviewed the multi-tab editor core implementation across `EditorTab`, `EditorController`, and `MainController`, plus the `EditorTabTest` suite. The core construction order in `EditorTab` is correct and well-documented; the `UndoManager` integration (replaceText → mark → forgetHistory) is sound. The EDIT-01 dedup via `toAbsolutePath().normalize()`, EDIT-03 Ctrl+S via `Nodes.addInputMap`, and EDIT-09 close-request guard are all correctly implemented.

Two warnings were found:

1. A listener memory leak in `EditorController.buildTab`: the `dirty` `BooleanBinding` listener is never removed on tab close, preventing `EditorTab` and its `CodeArea` from being garbage-collected over repeated open/close cycles.
2. A silent no-op in `MainController.updateTitle` when called before `setPrimaryStage()`, which hides call-ordering bugs.

One info item covers a UTF-8 assumption in file I/O.

No security or correctness-critical issues were found.

---

## Warnings

### WR-01: EditorTab listener leaks on close — CodeArea retained indefinitely

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java:147`

**Issue:** A `ChangeListener` is added to `editorTab.dirty` (a `BooleanBinding` backed by the `UndoManager`) but is never removed when the tab is closed. `setOnClosed` (line 128) removes the `Tab` from the registry, but the listener registered at line 147 keeps a reference chain alive: `BooleanBinding → listener → lambda captures (tab, baseName)`. Because `BooleanBinding` holds a strong reference to its listeners, neither the `EditorTab`, its `CodeArea`, nor the `UndoManager` can be collected. After many open/close cycles this constitutes a cumulative memory leak.

**Fix:** Remove the listener when the tab is closed. Capture the listener reference and call `dirty.removeListener(...)` in `setOnClosed`:

```java
// In buildTab(), capture the listener
ChangeListener<Boolean> dirtyListener = (obs, wasDirty, isDirty) -> {
    tab.setText(isDirty ? "*" + baseName : baseName);
    updateAppDirtyState();
};
editorTab.dirty.addListener(dirtyListener);

// In setOnClosed (or inline in buildTab after the listener is declared)
tab.setOnClosed(e -> {
    editorTab.dirty.removeListener(dirtyListener);
    registry.remove(key);
    updateAppDirtyState();
});
```

---

### WR-02: `updateTitle` silently no-ops when `primaryStage` is null

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java:110`

**Issue:** `updateTitle` returns silently when `primaryStage == null`. The Javadoc for `setPrimaryStage` says it is called "immediately after FXML load", but `initialize()` — which delegates to `EditorController.initialize()` — runs before `setPrimaryStage`. Any future code path that calls `updateTitle` from within `initialize()` (or before the app finishes starting) would silently do nothing. The current call site at line 153 is inside `handleOpenProject`, which is user-triggered and thus always post-start, so this is not an active bug. However, the silent guard makes ordering violations invisible. A developer-facing assertion or log statement makes the contract explicit.

**Fix:**

```java
public void updateTitle(String projectName) {
    if (primaryStage == null) {
        // This should not happen in normal operation; left as a safe guard.
        // Consider: throw new IllegalStateException("updateTitle called before setPrimaryStage");
        return;
    }
    // ... rest of method
}
```

At minimum, add an inline comment documenting that the null check is intentional (not a silent bug swallower). If this codebase develops tests that exercise `updateTitle` in isolation, the silent return will mask incorrect test setup.

---

## Info

### IN-01: UTF-8 assumed for all file reads — no charset detection

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java:124`

**Issue:** `Files.readString(key, StandardCharsets.UTF_8)` unconditionally reads as UTF-8. XSLT/XML files may declare a different encoding in their XML declaration (e.g., `<?xml version="1.0" encoding="ISO-8859-1"?>`). Files with non-UTF-8 encoding will be silently mojibake'd or throw a `MalformedInputException` at runtime (which is caught and shown as "Open File Failed", giving the user no useful diagnosis).

**Fix:** For MVP this is acceptable — the PRD does not mention encoding detection. Consider adding a note in the error message when `IOException` is caught to suggest encoding as a possible cause, or validate the XML declaration's encoding attribute before reading. A future task could read the BOM or XML prolog to detect encoding.

---

_Reviewed: 2026-04-18_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
