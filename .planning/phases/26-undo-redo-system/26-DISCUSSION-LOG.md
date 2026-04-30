# Phase 26: Undo/Redo System — Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-30
**Phase:** 26-undo-redo-system
**Areas discussed:** Edit menu disable, Toolbar button style, Toolbar layout

---

## Edit Menu Disable State

| Option | Description | Selected |
|--------|-------------|----------|
| Sì, disabilita | Comportamento coerente con TOOL-01/02. UX corretta. Stesso rebinding dei toolbar button. | ✓ |
| No, no-op silenzioso | Phase 25 non ha disable su Cut/Copy/Paste; scope minimo. | |

**User's choice:** Disabilita Edit > Undo e Edit > Redo quando nessuna history disponibile.
**Notes:** Requisiti EDIT-14/15 sono silenti sul disable, ma l'utente vuole coerenza con il comportamento toolbar (TOOL-01/02).

---

## Toolbar Button Style

| Option | Description | Selected |
|--------|-------------|----------|
| Testo puro: "Undo" / "Redo" | Coerente con "Render" button. | |
| Simboli Unicode: ↺ / ↻ | Compatti, iconici, standard. | ✓ |
| Testo + simbolo: "↺ Undo" / "↻ Redo" | Massima chiarezza, più largo. | |

**User's choice:** Simboli Unicode ↺ e ↻ senza testo aggiuntivo.

---

## Toolbar Layout

| Option | Description | Selected |
|--------|-------------|----------|
| Undo/Redo prima di Render | `[ ↺ ] [ ↻ ] \| [ Render ]` — convention standard desktop | ✓ |
| Undo/Redo dopo Render | `[ Render ] \| [ ↺ ] [ ↻ ]` — Render prominente | |
| Nessun separatore per ora | `[ ↺ ] [ ↻ ] [ Render ]` — separatori aggiunti in Phase 27 | |

**User's choice:** Undo/Redo prima di Render con separatore. Phase 27 inserirà Save tra i due gruppi.
**Notes:** Layout finale target: `[ ↺ ] [ ↻ ] | [ Save ] | [ Render ]`

---

## Claude's Discretion

- Strategia di rebinding al cambio tab
- `codeArea.undo()` vs `codeArea.getUndoManager().undo()`
- Dove vive la logica di rebinding (MainController vs EditorController helper)

## Deferred Ideas

Nessuna idea fuori scope emersa durante la discussione.
