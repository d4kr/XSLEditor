# Phase 6: Render Pipeline Integration - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-19
**Phase:** 06-render-pipeline-integration
**Areas discussed:** Render trigger & placement, Save-all before render, Progress indicator style

---

## Render trigger & placement

| Option | Description | Selected |
|--------|-------------|----------|
| Toolbar + menu item | ToolBar below MenuBar + Run menu item + F5 shortcut | ✓ |
| Menu item only | No ToolBar, menu item + shortcut only | |
| Toolbar only | ToolBar only, no menu item | |

**User's choice:** Toolbar + menu item

| Option | Description | Selected |
|--------|-------------|----------|
| Disabled + label changes | Button shows "Rendering..." and is disabled during Task | ✓ |
| Disabled + spinner | Button disabled, inline ProgressIndicator in toolbar | |
| Stays enabled (cancel) | Button becomes Cancel, Task.cancel() wired | |

**User's choice:** Disabled + label changes ("Rendering...")

| Option | Description | Selected |
|--------|-------------|----------|
| F5 | Standard "run" key (IntelliJ/VS Code) | ✓ |
| Ctrl+R / Cmd+R | Two-key chord, less conflict risk | |
| Claude decides | Either F5 or chord | |

**User's choice:** F5

---

## Save-all before render

| Option | Description | Selected |
|--------|-------------|----------|
| Auto-save all, then render | Silent save-all, matches REND-01 pipeline | ✓ |
| Warn first, user confirms | Alert dialog before save+render | |
| Render without saving | Render from disk state, ignore dirty tabs | |

**User's choice:** Auto-save all silently before render

---

## Progress indicator style

| Option | Description | Selected |
|--------|-------------|----------|
| Status label | statusLabel shows "Rendering..." then result; no new nodes | ✓ |
| Spinner in toolbar | ProgressIndicator added to ToolBar | |
| Both status + spinner | Status label text AND toolbar spinner | |

**User's choice:** Status label only (reuse existing showTransientStatus pattern with persistent "Rendering..." during Task)

---

## Claude's Discretion

- ToolBar FXML structure details (VBox wrapping MenuBar + ToolBar in BorderPane top)
- Whether `renderableProperty()` is on ProjectContext or derived in RenderController
- RenderController constructor vs. setter injection
- PreviewManager instantiation location
- Error string formatting beyond agreed prefix pattern

## Deferred Ideas

- Cancel/Task.cancel() support — too complex for Phase 6
- LogManager observable binding — Phase 8
- PDFViewerFX display — Phase 7
- Toolbar spinner — skipped in favor of status label text
