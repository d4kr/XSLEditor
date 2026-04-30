# Phase 27: Toolbar Save & ChatGPT Fix — Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-30
**Phase:** 27-toolbar-save-chatgpt-fix
**Areas discussed:** ChatGPT URL fix, Save button style, Save handler wiring

---

## ChatGPT URL Fix

### Q1: What approach to fix the broken ChatGPT button?

| Option | Description | Selected |
|--------|-------------|----------|
| ?q= but different host | Try chatgpt.com/chat?q= or chat.openai.com/?q= | |
| Clipboard + open chatgpt.com | Copy prompt to clipboard, open chatgpt.com | |
| You decide / research it | Claude researches correct URL format at plan time | ✓ |

**User's choice:** Research it at plan time.

---

### Q2: Fallback if no pre-fill URL found?

| Option | Description | Selected |
|--------|-------------|----------|
| Clipboard + open chatgpt.com | Copy prompt to clipboard, open URL | |
| Open chatgpt.com, no clipboard | Just open URL, lose pre-fill | |
| Keep ?q= as-is | Don't fix | |

**User's response (free text):** "il link come strutturato è corretto (https://chatgpt.com?q=errore%20xsflo) quindi forse l'errore è nella gestione del pulsante o altro..."

**Notes:** User confirmed the URL format works when tested manually. The bug is in the button handling, not the URL construction. Claude diagnosed: `addEventFilter(MOUSE_PRESSED, consume)` on the button itself prevents ButtonBase from arming → button never fires.

---

### Q3: What actually happens when the button is clicked?

| Option | Description | Selected |
|--------|-------------|----------|
| Browser opens, wrong URL | Malformed URL in browser | |
| Browser opens, ChatGPT doesn't pre-fill | URL correct but no pre-fill | |
| Nothing happens / button doesn't fire | No browser, no output | ✓ |
| Other / describe | Different symptom | |

**User's choice:** Nothing happens — button doesn't fire.

---

### Q4: Fix approach for event handling?

| Option | Description | Selected |
|--------|-------------|----------|
| addEventHandler (Recommended) | Swap filter → handler. Button arms first, then consumes. | ✓ |
| Remove filter, test if needed | Remove entirely; add back as handler only if TableView row also selects | |
| You decide | Claude picks at research/plan time | |

**User's choice:** `addEventHandler` — one-word change in `createAiButton()`.

---

## Save Button Style

### Q1: Label/style for Save toolbar button?

| Option | Description | Selected |
|--------|-------------|----------|
| 💾 icon (Recommended) | Floppy disk emoji — matches Undo/Redo Unicode symbol pattern | ✓ |
| Text 'Save' | Matches Render button text style — mixed toolbar | |
| You decide | Claude picks | |

**User's choice:** 💾 icon. Confirmed layout: `[ ↺ ] [ ↻ ] | [ 💾 ] | [ Render ]`

---

### Q2: Tooltip for Save button?

| Option | Description | Selected |
|--------|-------------|----------|
| No tooltip | Icon is self-explanatory | |
| 'Save' tooltip | Short hover tooltip | |

**User's response (free text):** "Add tooltips to all buttons in toolbar"

**Notes:** Scope extended slightly — add tooltips to Undo, Redo, Save, and Render buttons (all four). Text: "Undo" / "Redo" / "Save" / "Render".

---

### Q3: Tooltip text for all buttons?

| Option | Description | Selected |
|--------|-------------|----------|
| Undo / Redo / Save / Render (Recommended) | Single-word tooltips matching action label | ✓ |
| You decide | Claude picks descriptive text | |

**User's choice:** Undo / Redo / Save / Render.

---

## Save Handler Wiring

### Q1: How to expose saveTab() to MainController?

| Option | Description | Selected |
|--------|-------------|----------|
| saveActiveTab() public method (Recommended) | EditorController.saveActiveTab() mirrors Phase 25 clipboard pattern | ✓ |
| You decide | Claude picks | |

**User's choice:** `saveActiveTab()` public method on EditorController.

---

### Q2: Disable binding rebind placement?

| Option | Description | Selected |
|--------|-------------|----------|
| Extend existing rebind method | Add saveButton binding to the same method handling undoButton/redoButton | ✓ |
| You decide | Claude picks | |

**User's choice:** Extend existing rebind method.

---

## Claude's Discretion

- Exact name for the extended rebind method (`rebindUndoRedo()` → `rebindToolbarButtons()`?)
- Whether `saveActiveTab()` shows an error dialog on IOException (consistent with existing `saveTab()` behavior expected)

## Deferred Ideas

None — discussion stayed within phase scope.
