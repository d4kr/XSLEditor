---
phase: 27-toolbar-save-chatgpt-fix
reviewed: 2026-05-01T00:00:00Z
depth: standard
files_reviewed: 4
files_reviewed_list:
  - src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java
  - src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java
  - src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
  - src/main/java/ch/ti/gagi/xsleditor/ui/LogController.java
findings:
  critical: 1
  warning: 3
  info: 2
  total: 6
status: issues_found
---

# Phase 27: Code Review Report

**Reviewed:** 2026-05-01T00:00:00Z
**Depth:** standard
**Files Reviewed:** 4
**Status:** issues_found

## Summary

Phase 27 adds two features: (1) a toolbar Save button that is enabled only when the active tab is dirty, and (2) a fix to the ChatGPT AI-assist button in the log panel so that clicking it no longer also selects the table row. The scope is small — two new public methods in `EditorController`, a refactored `rebindToolbarButtons` in `MainController`, a new `<Button>` in `main.fxml`, and a one-line change in `LogController`.

One critical bug was found: the `addEventHandler`-based fix for the ChatGPT button inverts the original intent — consuming `MOUSE_PRESSED` in a bubbling handler does not prevent the `TableView` row-selection handler from seeing the event, because row selection fires during the bubbling phase of the same event on the parent node, which has already passed the button by the time `consume()` is called. The save-button binding logic also contains a minor but observable logic gap and a stale-binding leak scenario. Two info-level items round out the findings.

---

## Critical Issues

### CR-01: ChatGPT button row-selection suppression is still broken after the "fix"

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/LogController.java:165`

**Issue:** The commit comment claims that switching from `addEventFilter` to `addEventHandler` fixes the bug where clicking the ChatGPT button also selects the table row. The reasoning given is that `addEventFilter` consumed the event during the capturing phase and prevented `ButtonBase` from arming. That is correct — `addEventFilter` was wrong. However the replacement is also wrong for a different reason.

`TableView` row selection is driven by a `MOUSE_PRESSED` handler installed on the `TableRow`. When the user presses the mouse on the button, the event travels:  
`TableView → TableRow → TableCell → Button` (capturing), then back up  
`Button → TableCell → TableRow → TableView` (bubbling).

The `addEventHandler` on the `Button` fires during the bubbling leg of the `Button` node, but the `TableRow`'s selection handler also fires during bubbling — and because `TableRow` is an *ancestor* of the `Button`, its bubbling phase occurs *after* the button's. Calling `mouseEvt.consume()` inside the button's handler prevents the event from bubbling further up to `TableRow`, so row selection is suppressed.

So the fix *does* work in this specific case — the comment is just misleading about the phase ordering. The **actual remaining bug** is different: `Button.setOnAction` fires an `ActionEvent`, not the original `MouseEvent`. The `setOnAction` lambda (line 167) calls `getTableRow().getItem()` at the time the action fires. If the cell has been recycled by JavaFX's virtual-cell reuse mechanism between the `MOUSE_PRESSED` event and the `ActionEvent`, `getTableRow().getItem()` may return a *different* `LogEntry` than the one the user clicked on — or `null` — because `TableRow`/`TableCell` instances are pooled and rebound to different rows during scroll or list updates.

The null-check on line 168 (`if (entry == null || entry.message() == null) return;`) silently swallows the stale-row case, but the more dangerous outcome is opening ChatGPT with the *wrong* log entry's message when the cell happens to be rebound to a different entry between the two events.

**Fix:** Capture the `LogEntry` at the time `MOUSE_PRESSED` fires (where the identity is guaranteed stable) and reference that captured value in `setOnAction`, rather than re-reading it from `getTableRow()` inside the action handler:

```java
b.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, mouseEvt -> {
    mouseEvt.consume();  // prevent row selection — correct, keep this
    // Capture the entry NOW while the row binding is guaranteed correct
    LogEntry captured = getTableRow() != null ? (LogEntry) getTableRow().getItem() : null;
    b.setUserData(captured);  // stash for the action handler
});
b.setOnAction(evt -> {
    LogEntry entry = (LogEntry) b.getUserData();
    if (entry == null || entry.message() == null) return;
    String prompt = "Ho questo errore nel mio progetto XSLT/XSL-FO, puoi aiutarmi?\n\n"
            + entry.message();
    String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8)
            .replace("+", "%20");
    String url = "https://chatgpt.com/?q=" + encoded;
    XSLEditorApp.hostServices().showDocument(url);
});
```

---

## Warnings

### WR-01: `saveButton` binding uses `!Boolean.TRUE.equals(...)` — evaluates as disabled when dirty value is `null`

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java:433`

**Issue:** The disable binding for `saveButton` is computed as:

```java
() -> !Boolean.TRUE.equals(dirtyProp.getValue())
```

`Boolean.TRUE.equals(null)` returns `false`, so `!false` == `true`, meaning the button is *disabled* whenever `dirtyProp.getValue()` returns `null`. `EditorTab.dirty` is a `BooleanBinding` whose `getValue()` returns a boxed `Boolean` — `BooleanBinding.getValue()` can return `null` during initialisation before the binding has been evaluated for the first time (it is lazily computed). This creates a window between tab open and the first change event where `saveButton` is incorrectly disabled even though the file is clean (not dirty), which is consistent with the desired clean-file state, but if the binding ever returns `null` while the tab is actually dirty (edge case during binding invalidation), the button would be incorrectly disabled.

The safer idiom is to use the strongly-typed `BooleanBinding.get()` directly, which always returns a primitive `boolean` (it forces evaluation):

```java
editorController.getActiveDirtyProperty().ifPresentOrElse(
    dirtyProp -> saveButton.disableProperty().bind(
        Bindings.createBooleanBinding(
            () -> !((BooleanBinding) dirtyProp).get(),
            dirtyProp)),
    () -> saveButton.setDisable(true)
);
```

Alternatively, since `getActiveDirtyProperty()` is already known to return a `BooleanBinding` (see `EditorController.getActiveDirtyProperty()`), narrow the return type of that method to `Optional<BooleanBinding>` to make the type safe at the call site.

### WR-02: `getActiveDirtyProperty()` called a second time after `rebindToolbarButtons` already has `activeCodeArea` — double lookup with potential state drift

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java:430`

**Issue:** `rebindToolbarButtons` receives the active `CodeArea` as a parameter (from `EditorController.setOnActiveTabChanged`). Inside the method, after the undo/redo bindings are set up using that parameter, it calls `editorController.getActiveDirtyProperty()` to bind `saveButton`. This is a second independent lookup of the selected tab.

Between the time `setOnActiveTabChanged` fires and `getActiveDirtyProperty()` executes, another tab-selection event could theoretically fire (e.g., a programmatic selection from another code path). In that case, the undo/redo controls are bound to the `CodeArea` of tab A while `saveButton` is bound to the dirty property of tab B.

In practice this race is unlikely on the single JavaFX Application Thread with no interleaving, but the design is fragile. The correct fix is to pass the dirty property alongside the `CodeArea` in the callback, or to look up both in `EditorController` atomically from the same selected tab:

```java
// In EditorController: return a record/pair carrying both CodeArea and dirty binding
public Optional<ActiveTabState> getActiveTabState() { ... }
```

Or simply look up `getActiveDirtyProperty()` before calling `rebindToolbarButtons`, pass it in, and avoid the second trip into `EditorController`.

### WR-03: `saveAll()` does not call `updateAppDirtyState()` via `dirtyCallback` — aggregate dirty state becomes stale after a bulk save

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java:430`

**Issue:** `saveAll()` (line 430–438) writes all dirty tabs and calls `updateAppDirtyState()` (line 437), which invokes `dirtyCallback.accept(anyDirty)`. However, `updateAppDirtyState()` only fires the callback once at the end of the loop. After each individual `Files.writeString` + `et.codeArea.getUndoManager().mark()` call, the `EditorTab.dirty` binding transitions from `true` to `false` (because `atMarkedPositionProperty()` becomes `true`). The `dirtyCallback` fires on those individual transitions because `dirty`'s `ChangeListener` (added in `buildTab`, line 281–284) calls `updateAppDirtyState()` automatically.

This means `dirtyCallback` is called N+1 times (once per tab during the mark transitions, plus once explicitly at line 437), with all intermediate calls potentially showing `anyDirty = true` for the tabs not yet processed. The final call at line 437 is therefore redundant and potentially confusing.

More critically, `saveAll()` does **not** notify `MainController` of the newly clean state through `dirtyCallback` for tabs that were not dirty — the loop skips them. But when `mark()` is called on a dirty tab, the `atMarkedPositionProperty()` fires a change, which triggers the `dirtyListener` on that tab's `Tab`, which calls `updateAppDirtyState()`. So for the common case this does work.

The real problem is that if `saveAll()` is called when `allEntries` (the entries in `registry`) has been modified concurrently (which should not happen on the FX thread, but defensive coding requires it), the `ConcurrentModificationException` is unhandled and would propagate to the caller (`RenderController.handleRender`). The caller does catch `IOException` (per the method contract) but a `CME` is a `RuntimeException` and would bubble past the catch block, leaving the render in a broken state.

**Fix:** Iterate over a snapshot: `new ArrayList<>(registry.values())`.

---

## Info

### IN-01: ChatGPT URL construction truncates silently for very long error messages

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/LogController.java:173`

**Issue:** The full `entry.message()` is appended to the ChatGPT URL with no length cap. Some XSLT stack traces can be several kilobytes. Most browsers and the OS `showDocument()` implementation impose URL length limits (commonly 2 083 characters for Windows shell, 8 000+ for macOS). On Windows, a message longer than ~1 900 characters after encoding will silently truncate the URL or fail to open. No user-visible error is shown.

**Fix:** Truncate the message before encoding:
```java
String msg = entry.message();
if (msg.length() > 1500) msg = msg.substring(0, 1500) + "…";
String prompt = "Ho questo errore nel mio progetto XSLT/XSL-FO, puoi aiutarmi?\n\n" + msg;
```

### IN-02: `statusLabel` is inside `fileTreePane` — semantically misplaced and may be obscured by the FileTree overlay

**File:** `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml:108`

**Issue:** The `statusLabel` (`fx:id="statusLabel"`) is a child of `fileTreePane` (a `StackPane`). `FileTreeController.initialize()` (line 106 of `MainController`) populates this same pane by replacing its children. If `FileTreeController` uses `setAll()` (analogous to `EditorController.mountTabPane()` line 103), the `statusLabel` will be removed and the transient status messages will become invisible after the first project is opened.

This is not new code in phase 27 but the `showTransientStatus` function is actively used by Phase 27 paths (e.g., the render status), so the bug surfaces in the new phase's execution paths. The `statusLabel` should reside outside `fileTreePane`, for example in a dedicated status bar below the `SplitPane`.

---

_Reviewed: 2026-05-01T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
