# Phase 16: Log Panel Layout - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-23
**Phase:** 16-log-panel-layout
**Areas discussed:** Resize policy

---

## Resize Policy

### Area selection

| Option | Selected |
|--------|----------|
| Resize policy | ✓ |
| Column sizing strategy | |
| Min widths at narrow windows | |

---

## Resize Policy — Where to apply

| Option | Description | Selected |
|--------|-------------|----------|
| FXML only | `columnResizePolicy="CONSTRAINED_RESIZE_POLICY"` in main.fxml + minWidth/maxWidth per column | ✓ |
| Programmatic in LogController | `logTableView.setColumnResizePolicy(...)` + column constraints in Java | |

**User's choice:** FXML only
**Notes:** "deve occupare tutta la larghezza" — must fill full width. User confirmed exact FXML diff with column widths: Time/Level/AI fixed, Type 80–120 flex, Message absorbs remainder.

---

## Claude's Discretion

- CSS additions to `main.css` for column headers/cells (if needed)
- Whether `text` attributes need updating

## Deferred Ideas

None.
