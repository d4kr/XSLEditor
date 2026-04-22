# Phase 12: AI Assist in Error Log - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-22
**Phase:** 12-ai-assist-error-log
**Areas discussed:** Prompt format, Button label / icon

---

## Action placement

*Not selected for discussion — default applied.*

| Option | Description | Selected |
|--------|-------------|----------|
| Dedicated 5th column | Button/icon column after Message | ✓ (default) |
| Context menu | Right-click on row | |
| Inline in Message cell | Button embedded in message text | |

**Decision:** Dedicated 5th column (narrow, ~40px).

---

## Prompt format

| Option | Description | Selected |
|--------|-------------|----------|
| Error message only | Raw `LogEntry.message()` | |
| Type + message | Prefix with `LogEntry.type()` | |
| Error message only + Italian preamble | Fixed Italian preamble + blank line + message | ✓ |

**User's choice:** Error message only — then clarified with note: prompt must be in Italian.

**Follow-up — Italian preamble format:**

| Option | Description | Selected |
|--------|-------------|----------|
| Minimal preamble | "Ho questo errore nel mio progetto XSLT/XSL-FO, puoi aiutarmi?\n\n{message}" | ✓ |
| Tool context preamble | "Sto usando XLSEditor (pipeline XML→XSLT→XSL-FO→PDF) e ricevo questo errore:\n\n{message}" | |
| Generic question | "Cosa significa questo errore?\n\n{message}" | |

**Notes:** Multi-language preferences deferred to a future phase.

---

## Button label / icon

| Option | Description | Selected |
|--------|-------------|----------|
| Small icon button | Compact chat icon (💬), no text, ~40px column | ✓ |
| Text button "AI" | Short text label | |
| Text button "ChatGPT" | Explicit branding, wider column | |

**User's choice:** Small icon button (💬).

---

## Claude's Discretion

- Exact Unicode character for icon
- Tooltip text
- Column header text
- CSS styling of the button

## Deferred Ideas

- Multi-language prompt preference setting — future phase
- Other AI providers (Copilot, Gemini) — out of scope
