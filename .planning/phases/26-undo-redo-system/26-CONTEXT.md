# Phase 26: Undo/Redo System — Context

**Gathered:** 2026-04-30
**Status:** Ready for planning

<domain>
## Phase Boundary

Expose the per-CodeArea `UndoManager` (already instantiated in `EditorTab`) as:
1. Edit > Undo (Ctrl+Z) and Edit > Redo (Ctrl+Shift+Z) menu items
2. Toolbar Undo (↺) and Redo (↻) buttons

All four controls must be disabled when no history is available. Toolbar buttons and menu items
must rebind when the active editor tab changes so disable state always reflects the current tab.

This phase does NOT add a Save button or fix the ChatGPT URL — those are Phase 27.

</domain>

<decisions>
## Implementation Decisions

### Edit Menu

- **D-01:** Edit > Undo and Edit > Redo get `disableProperty()` bindings — grayed out when
  no undo/redo history is available (or no tab is open). Same disable logic as toolbar buttons.
  This goes beyond the explicit requirement (EDIT-14/15 are silent on menu item disable) but
  is the correct desktop UX and is consistent with TOOL-01/TOOL-02.

### Toolbar Buttons

- **D-02:** Toolbar buttons use Unicode symbols only — **↺** for Undo, **↻** for Redo.
  No text label. The `text` attribute on the `<Button>` FXML element is set to `"↺"` / `"↻"`.

- **D-03:** Toolbar layout — Undo/Redo go **before** Render, separated by a `<Separator>`:
  ```
  [ ↺ ] [ ↻ ] | [ Render ]
  ```
  Phase 27 will insert a Save button between the separator and Render, yielding the final layout:
  ```
  [ ↺ ] [ ↻ ] | [ Save ] | [ Render ]
  ```
  Phase 26 must add the separator in anticipation of this so Phase 27 only inserts a button.

### Claude's Discretion

- Rebinding strategy on tab switch: how to implement live disable binding when
  `tabPane.getSelectionModel().selectedItemProperty()` changes — Claude chooses the approach
  (listener-based rebind vs. expression binding).
- Whether handler calls `codeArea.undo()` / `codeArea.redo()` directly or goes through
  `codeArea.getUndoManager().undo()` / `.redo()` — use whichever is idiomatic in RichTextFX.
- Where the rebinding logic lives — MainController or a new helper in EditorController.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` — EDIT-14, EDIT-15, TOOL-01, TOOL-02 (success criteria for this phase)

### Prior Phase Research
- `.planning/phases/25-edit-menu-clipboard-commands/25-RESEARCH.md` — RichTextFX accelerator
  conflict research: CodeArea consumes keyboard events first when focused; MenuItem accelerator
  is visual hint only; same pattern applies to Ctrl+Z/Ctrl+Shift+Z.

### Source Files to Read Before Planning
- `src/main/java/ch/ti/gagi/xsleditor/ui/EditorTab.java` — UndoManager instantiation order
  (replaceText → mark → forgetHistory); `undoAvailableProperty()` / `redoAvailableProperty()`
  come from `codeArea.getUndoManager()`
- `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java` — `getActiveCodeArea()` method
  and TabPane ownership; any existing tab-selection listener
- `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` — clipboard handler pattern
  (Phase 25); existing `disableProperty().bind()` calls; `@FXML private Button renderButton`
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` — current Edit menu structure and
  ToolBar node (only `renderButton` today)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `EditorController.getActiveCodeArea()` — returns `Optional<CodeArea>` for current tab;
  already used by all clipboard handlers in Phase 25; same delegation for undo/redo handlers
- `EditorTab.dirty` (`BooleanBinding`) — same construction pattern for undo/redo availability
  properties; `codeArea.getUndoManager().undoAvailableProperty()` and
  `codeArea.getUndoManager().redoAvailableProperty()`

### Established Patterns
- **Handler pattern (Phase 25):** `@FXML private void handleEditUndo() { editorController.getActiveCodeArea().ifPresent(ca -> ca.undo()); }` — exact same structure as cut/copy/paste
- **Disable binding pattern:** `menuItemNewFile.disableProperty().bind(projectContext.projectLoadedProperty().not())` in `MainController.initialize()` — same approach for undo/redo menu items and toolbar buttons
- **MenuItem accelerator:** `accelerator="Shortcut+Z"` / `accelerator="Shortcut+Shift+Z"` in FXML — no conflict with CodeArea native handling (per Phase 25 RESEARCH.md issue #185)

### Integration Points
- `main.fxml` Edit menu: Undo/Redo items go at the TOP (before Cut), separated by a `<SeparatorMenuItem/>`
- `main.fxml` ToolBar: add `<Button fx:id="undoButton" text="↺"/>` and `<Button fx:id="redoButton" text="↻"/>` at the start, then `<Separator/>`, then existing `renderButton`
- `MainController.java`: add `@FXML private Button undoButton;`, `@FXML private Button redoButton;`, `@FXML private MenuItem undoMenuItem;`, `@FXML private MenuItem redoMenuItem;`, and wire bindings in `initialize()`
- The rebinding must happen when `tabPane.getSelectionModel().selectedItemProperty()` changes — `EditorController` owns the TabPane, so the rebinding helper likely belongs there or the tab-switch listener fires a callback to MainController

</code_context>

<specifics>
## Specific Ideas

- Toolbar final target layout (confirmed by user, Phase 26 sets foundation):
  ```
  [ ↺ ] [ ↻ ] | [ Save ] | [ Render ]
  ```
  Phase 26 delivers `[ ↺ ] [ ↻ ] | [ Render ]`; Phase 27 inserts Save before the second separator.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 26-undo-redo-system*
*Context gathered: 2026-04-30*
