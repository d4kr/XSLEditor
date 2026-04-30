# Phase 27: Toolbar Save & ChatGPT Fix — Context

**Gathered:** 2026-04-30
**Status:** Ready for planning

<domain>
## Phase Boundary

Two independent changes in one phase:

1. **Toolbar Save button** — Add a 💾 Save button to the toolbar between the existing
   `<Separator/>` and `renderButton`. Button is disabled when no tab is open or the active
   tab is clean (not dirty). Clicking it saves the active tab to disk and clears the dirty
   indicator. Disable binding rebinds on tab switch, using the same mechanism as
   `undoButton`/`redoButton` (Phase 26).

2. **ChatGPT button fix** — The 💬 button in the error log fires nothing (no browser opens).
   Root cause: `addEventFilter(MOUSE_PRESSED, mouseEvt -> mouseEvt.consume())` on the
   button itself prevents ButtonBase from arming (event consumed before internal handlers
   run). Fix: change `addEventFilter` → `addEventHandler`. The URL format
   `https://chatgpt.com/?q=TEXT` is confirmed correct by manual test — no change needed
   to URL construction.

This phase does NOT change the render pipeline, error log layout, or ChatGPT URL format.

</domain>

<decisions>
## Implementation Decisions

### ChatGPT Button Fix

- **D-01:** Change `addEventFilter(MOUSE_PRESSED, mouseEvt -> mouseEvt.consume())` to
  `addEventHandler(MOUSE_PRESSED, mouseEvt -> mouseEvt.consume())` in
  `LogController.createAiButton()`. This allows ButtonBase to arm on MOUSE_PRESSED
  (internal behavior runs first), then the handler consumes the event before it bubbles
  to the TableView row selector. One-word change.

- **D-02:** The URL format `https://chatgpt.com/?q=TEXT` is confirmed correct. No change
  to URL construction, encoding, or the Italian preamble.

### Save Button — Style & Layout

- **D-03:** Save button label: `"💾"` (floppy disk emoji, no text). Matches the
  Undo/Redo Unicode symbol convention from Phase 26.

- **D-04:** FXML position: insert `<Button fx:id="saveButton" text="💾" onAction="#handleToolbarSave"/>` followed by `<Separator/>` between the existing `<Separator/>` and `<Button fx:id="renderButton"/>`.
  Final toolbar layout:
  ```
  [ ↺ ] [ ↻ ] | [ 💾 ] | [ Render ]
  ```

- **D-05:** All four toolbar buttons get tooltips: `"Undo"`, `"Redo"`, `"Save"`,
  `"Render"`. Undo/Redo/Render currently have none — add tooltips to all in this phase.

### Save Button — Handler & Disable Binding

- **D-06:** Expose `EditorController.saveActiveTab()` as a public method (mirrors the
  Phase 25 clipboard pattern: `editorController.getActiveCodeArea().ifPresent(...)`).
  Internally, `saveActiveTab()` resolves the active `EditorTab` and calls the existing
  private `saveTab(editorTab)`.

- **D-07:** `saveButton.disableProperty()` binding is folded into the existing toolbar
  rebind method (the same method that rebinds `undoButton`/`redoButton` on tab switch).
  Bind to `activeEditorTab.dirty.not()`. When no tab is open, `saveButton.setDisable(true)`
  (same null-tab path as undo/redo).

### Claude's Discretion

- Exact method name for the extended rebind method (e.g., keep `rebindUndoRedo()` or
  rename to `rebindToolbarButtons()` / `rebindToolbar()`) — Claude picks the cleaner name.
- Whether `saveActiveTab()` shows an error dialog on `IOException` (like `saveTab()` does)
  or delegates to the existing error-dialog path — use the same error-dialog approach as
  `saveTab()` for consistency.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` — TOOL-03 (Save toolbar button), ERR-07 (ChatGPT button fix)
  with success criteria for this phase

### Prior Phase Context
- `.planning/phases/26-undo-redo-system/26-CONTEXT.md` — D-02/D-03 establish toolbar
  layout and button style conventions; rebind pattern for tab-switch disable binding

### Source Files to Read Before Planning
- `src/main/java/ch/ti/gagi/xsleditor/ui/LogController.java` — `createAiButton()` method
  (~line 156); `addEventFilter(MOUSE_PRESSED, ...)` is the bug location
- `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java` — `saveTab(editorTab)`
  private method (~line 381); `saveAll()` public method (~line 398); Ctrl+S InputMap wiring
  (~line 256)
- `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` — `@FXML private Button undoButton/redoButton`; `rebindUndoRedo()` / tab-switch listener; existing toolbar
  disable binding pattern
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` — ToolBar node (lines ~80–85);
  current `<Separator/>` + `renderButton` layout to insert Save between them

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `EditorController.saveTab(editorTab)` — existing private save logic (write to disk,
  mark UndoManager, update app dirty state); `saveActiveTab()` wraps this
- `EditorController.getActiveCodeArea()` — Phase 25 pattern; analog for resolving active
  EditorTab in `saveActiveTab()`
- `rebindUndoRedo()` / tab-switch listener in `MainController` — extend to include
  `saveButton` binding; same `EditorTab.dirty` property used

### Established Patterns
- **Toolbar button style (Phase 26):** `<Button fx:id="undoButton" text="↺" onAction="#handleToolbarUndo"/>` — same FXML structure for saveButton
- **Disable binding:** `undoButton.disableProperty().bind(undoManager.undoAvailableProperty().not())` in `MainController.initialize()` / rebind method — analog: `saveButton.disableProperty().bind(activeTab.dirty.not())`
- **Handler pattern (Phase 25/26):** `@FXML private void handleToolbarSave() { editorController.saveActiveTab(); }` — exact same structure as handleToolbarUndo/Redo

### Integration Points
- `main.fxml` ToolBar: insert `<Button fx:id="saveButton" .../>` + `<Separator/>` between
  the existing `<Separator/>` and `<Button fx:id="renderButton"/>`
- `MainController.java`: add `@FXML private Button saveButton;` and wire tooltip + disable
  binding in `initialize()`; extend rebind method for tab-switch
- `LogController.java`: one-word change in `createAiButton()` — `addEventFilter` →
  `addEventHandler`

</code_context>

<specifics>
## Specific Ideas

- Toolbar final layout confirmed by user:
  ```
  [ ↺ ] [ ↻ ] | [ 💾 ] | [ Render ]
  ```
- User confirmed URL format is correct: `https://chatgpt.com/?q=TEXT` works when tested
  manually in browser. Bug is purely in event handling, not URL construction.
- All four toolbar buttons get tooltips in this phase (Undo, Redo, Save, Render) — even
  though only Save is new. Retroactive addition to Undo/Redo/Render.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 27-toolbar-save-chatgpt-fix*
*Context gathered: 2026-04-30*
