---
phase: 26-undo-redo-system
reviewed: 2026-04-30T00:00:00Z
depth: standard
files_reviewed: 3
files_reviewed_list:
  - src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
  - src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java
  - src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java
findings:
  critical: 0
  warning: 4
  info: 2
  total: 6
status: issues_found
---

# Phase 26: Undo/Redo System — Code Review Report

**Reviewed:** 2026-04-30
**Depth:** standard
**Files Reviewed:** 3
**Status:** issues_found

## Summary

Phase 26 adds Undo/Redo to the Edit menu and toolbar: four FXML controls (`undoMenuItem`, `redoMenuItem`, `undoButton`, `redoButton`), two duplicate handler pairs in `MainController`, and a `setOnActiveTabChanged` / `rebindUndoRedo` binding chain in `EditorController` + `MainController`.

The implementation is architecturally sound and the `EditorTab` load-order contract (replaceText → mark → forgetHistory) is correct. However, four warning-level defects and two info-level items were found. None are data-loss or security issues, but two of the warnings (listener accumulation and duplicate handler duplication) are latent correctness bugs that will manifest in extended use or if the method is called more than once.

---

## Warnings

### WR-01: `setOnActiveTabChanged` adds a new listener every call — listeners accumulate

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java:205`

**Issue:** Every call to `setOnActiveTabChanged` calls `tabPane.getSelectionModel().selectedItemProperty().addListener(...)` with a new anonymous lambda. The comment acknowledges the prior listener is not removed ("we hold no reference to the prior ChangeListener"), and declares "one-shot semantics: caller invokes once." That contract is fragile and not enforced. If `setOnActiveTabChanged` is called a second time (e.g. during a future refactor or test), two listeners accumulate on the property. Each fires on every tab switch, causing `rebindUndoRedo` to run twice, producing duplicate `bind()` calls on the same four `disableProperty()` instances. The second `bind()` call throws a runtime `IllegalArgumentException` ("A bound value cannot be set") from JavaFX — crashing on any tab switch.

**Fix:** Store the listener reference and remove it before registering the new one:
```java
private ChangeListener<Tab> activeTabListener;

public void setOnActiveTabChanged(Consumer<Optional<CodeArea>> callback) {
    Objects.requireNonNull(callback, "callback");
    if (activeTabListener != null) {
        tabPane.getSelectionModel().selectedItemProperty()
               .removeListener(activeTabListener);
    }
    activeTabListener = (obs, oldTab, newTab) ->
        callback.accept(extractCodeArea(newTab));
    tabPane.getSelectionModel().selectedItemProperty()
           .addListener(activeTabListener);
    callback.accept(extractCodeArea(
        tabPane.getSelectionModel().getSelectedItem()));
}
```

---

### WR-02: `handleToolbarUndo` / `handleToolbarRedo` are exact duplicates of `handleEditUndo` / `handleEditRedo`

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java:363-370`

**Issue:** The two toolbar handler methods are identical in body to the two Edit-menu handlers:
```java
@FXML private void handleEditUndo()    { editorController.getActiveCodeArea().ifPresent(CodeArea::undo); }
@FXML private void handleEditRedo()    { editorController.getActiveCodeArea().ifPresent(CodeArea::redo); }
@FXML private void handleToolbarUndo() { editorController.getActiveCodeArea().ifPresent(CodeArea::undo); }
@FXML private void handleToolbarRedo() { editorController.getActiveCodeArea().ifPresent(CodeArea::redo); }
```
The duplication means any future change to the undo/redo invocation path (e.g. adding a check, logging, or delegating to a manager) must be applied in two places, and it is easy to diverge silently.

**Fix:** Delegate the toolbar handlers to the menu handlers, or extract a shared private method:
```java
@FXML private void handleEditUndo()    { performUndo(); }
@FXML private void handleEditRedo()    { performRedo(); }
@FXML private void handleToolbarUndo() { performUndo(); }
@FXML private void handleToolbarRedo() { performRedo(); }

private void performUndo() { editorController.getActiveCodeArea().ifPresent(CodeArea::undo); }
private void performRedo() { editorController.getActiveCodeArea().ifPresent(CodeArea::redo); }
```

---

### WR-03: `selectedTextProperty` anonymous listener in `buildTab` is never removed on tab close

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java:305-308`

**Issue:** The `tab.setOnClosed` handler (line 341) removes the `dirtyListener`, unsubscribes the `highlightSub`, and shuts down `hlExecutor`, but it does not remove the `selectedTextProperty` listener registered at line 305. This listener captures `editorTab.codeArea` by closure. After the tab is closed, the `codeArea` is removed from the scene but the listener keeps the `codeArea` strongly referenced from the `selectedTextProperty` observable, preventing GC. In a long session with many open-and-close cycles this accumulates. If OccurrenceHighlighter holds any external state (e.g. decorations on the closed CodeArea), the listener may also produce silent no-ops or, depending on the OccurrenceHighlighter implementation, exceptions when it tries to modify a detached node.

**Fix:** Capture the lambda reference and remove it in `setOnClosed`:
```java
// At declaration site:
ChangeListener<String> selectionListener = (obs, oldSel, newSel) -> {
    String token = (newSel == null) ? "" : newSel.strip()
        .replaceAll("^[<>/\"'=]+|[<>/\"'=]+$", "");
    OccurrenceHighlighter.applyTo(editorTab.codeArea, token);
};
editorTab.codeArea.selectedTextProperty().addListener(selectionListener);

// In setOnClosed:
editorTab.codeArea.selectedTextProperty().removeListener(selectionListener);
```

---

### WR-04: `rebindUndoRedo` binds `disableProperty` without guarding against a closed/GC-eligible `CodeArea`

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java:407-422`

**Issue:** `rebindUndoRedo` binds the four `disableProperty()` values to `ca.getUndoManager().undoAvailableProperty()` and `redoAvailableProperty()`. If `setOnActiveTabChanged` is fired with a non-empty `Optional<CodeArea>` for a tab that is subsequently closed (possible if the close event fires before the selection-change event propagates), the binding holds a reference to the old `CodeArea`'s `UndoManager`. The next tab-switch fires `rebindUndoRedo` again, which calls `unbind()` first, so the stale reference is released. However, in the window between the close and the next selection change, the four controls remain bound to the UndoManager of a closed tab. If the UndoManager fires a change notification during that window (e.g. from a background thread completing), the lambda `() -> !ca.getUndoManager().isUndoAvailable()` executes against a potentially closed `CodeArea`. While RichTextFX's UndoManager is unlikely to throw in this scenario, the code relies on a timing assumption that is not enforced.

**Fix:** In `tab.setOnClosed`, after the existing cleanup, fire `rebindUndoRedo(Optional.empty())` explicitly to ensure the controls are immediately unbound and disabled when a tab closes, regardless of selection-change event ordering:
```java
tab.setOnClosed(e -> {
    if (highlightSub != null) highlightSub.unsubscribe();
    if (hlExecutor != null)   hlExecutor.shutdownNow();
    editorTab.dirty.removeListener(dirtyListener);
    registry.remove(key);
    updateAppDirtyState();
    // Notify MainController to unbind undo/redo from this (now closed) tab's UndoManager
    // This must be wired back via a callback; see note below.
});
```
Alternatively, expose a `onTabClosed` callback in `EditorController` (analogous to `setOnActiveTabChanged`) and call `rebindUndoRedo(Optional.empty())` from `MainController` via that callback.

---

## Info

### IN-01: Redo accelerator uses `Shortcut+Shift+Z` — `Shortcut+Y` is not offered as alternative

**File:** `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml:48`

**Issue:** `Shortcut+Shift+Z` is the macOS convention (Cmd+Shift+Z) and is correct. On Windows/Linux `Shortcut+Shift+Z` maps to Ctrl+Shift+Z, which many users know, but `Ctrl+Y` is the muscle-memory default for a large share of Windows users (VS Code, Office, etc.). JavaFX `MenuItem` supports only one accelerator — there is no built-in multi-accelerator support. This is an ergonomic limitation rather than a bug, but worth noting for a developer-targeted tool.

**Fix:** No code change required. If Windows/Linux parity matters, add a `KeyEvent` filter at the scene level in `MainController` to intercept `Ctrl+Y` and call `performRedo()`.

---

### IN-02: Comment in `setOnActiveTabChanged` is self-contradictory about listener replacement

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java:202-204`

**Issue:** The Javadoc says "calling this method twice replaces the previous listener," but the implementation comment on line 203 says "Since we hold no reference to the prior ChangeListener, we accept a one-shot semantics: caller invokes once." These two statements contradict each other. The Javadoc claim is wrong — calling twice adds a second listener, it does not replace the first. This misleads future maintainers into assuming safe idempotency.

**Fix:** After fixing WR-01, update the comment to accurately describe the replaced-listener behavior. Until WR-01 is fixed, change the Javadoc to remove the false "replaces the previous listener" claim and clarify it is strictly one-shot.

---

_Reviewed: 2026-04-30_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
