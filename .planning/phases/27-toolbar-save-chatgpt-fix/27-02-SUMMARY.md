---
phase: 27-toolbar-save-chatgpt-fix
plan: "02"
subsystem: ui
tags: [javafx, event-handling, chatgpt, log-panel]

# Dependency graph
requires:
  - phase: 12-ai-assist-error-log
    provides: "createAiButton() with ChatGPT URL construction and event wiring"
provides:
  - "ERR-07 fix: ChatGPT button in error log now correctly fires setOnAction via addEventHandler"
affects: [27-toolbar-save-chatgpt-fix]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "JavaFX event wiring: use addEventHandler (bubbling phase) not addEventFilter (capturing phase) when needing ButtonBase to arm before consuming the event"

key-files:
  created: []
  modified:
    - src/main/java/ch/ti/gagi/xsleditor/ui/LogController.java

key-decisions:
  - "Single token swap: addEventFilter -> addEventHandler in createAiButton(). No URL, preamble, or encoding changes."
  - "addEventHandler runs in the bubbling phase (after ButtonBase's internal MOUSE_PRESSED handler), so ButtonBase arms and setOnAction fires; addEventFilter ran during capturing, preventing ButtonBase from arming."

patterns-established:
  - "JavaFX ButtonBase pattern: addEventHandler(MOUSE_PRESSED) lets ButtonBase arm first, then handler can consume to prevent event bubbling to parent containers"

requirements-completed: [ERR-07]

# Metrics
duration: 5min
completed: 2026-05-01
---

# Phase 27 Plan 02: ChatGPT Button Fix Summary

**One-token fix in LogController.createAiButton(): `addEventFilter` replaced with `addEventHandler` for MOUSE_PRESSED, restoring ButtonBase arming so setOnAction fires and opens ChatGPT in the browser**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-05-01T07:05:00Z
- **Completed:** 2026-05-01T07:07:50Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments

- Fixed ERR-07: the ChatGPT button in the error log panel now correctly opens the default browser at `https://chatgpt.com/?q=<encoded-prompt>`
- Root cause identified and corrected: `addEventFilter` consumed MOUSE_PRESSED during JavaFX's capturing phase, before ButtonBase's internal MOUSE_PRESSED handler could arm the button, so `setOnAction` never fired
- Switched to `addEventHandler` (bubbling phase): ButtonBase arms first, then the handler runs and `consume()` prevents the event from bubbling up to the TableView row selector — row selection is still suppressed
- URL format, Italian preamble (`"Ho questo errore nel mio progetto XSLT/XSL-FO, puoi aiutarmi?\n\n"`), and URL encoding (`replace("+", "%20")`) are byte-for-byte unchanged (D-02 confirmed)

## Task Commits

1. **Task 1: Replace addEventFilter with addEventHandler in createAiButton()** - `3c00019` (fix)

## Files Created/Modified

- `src/main/java/ch/ti/gagi/xsleditor/ui/LogController.java` - Replaced `addEventFilter` with `addEventHandler` in `createAiButton()` at lines 161-165; updated comment to explain JavaFX capturing-vs-bubbling phase semantics

## Decisions Made

- Only one token changed: `addEventFilter` -> `addEventHandler`. No URL, encoding, or preamble changes per D-02.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## JavaFX Event Phase Semantics (root cause explanation)

JavaFX processes input events in three phases:
1. **Capturing** (top-down from root to target): event filters run here
2. **Target**: event handlers on the target node run
3. **Bubbling** (bottom-up from target to root): event handlers on parent nodes run

`addEventFilter(MOUSE_PRESSED, ...)` installs a listener in the **capturing** phase. When the user clicks the button, this filter ran first and called `consume()`, which marked the event as consumed. ButtonBase checks for this and skips its internal MOUSE_PRESSED handler (which normally "arms" the button). Since the button never armed, the subsequent MOUSE_RELEASED never triggered the action, and `setOnAction` never fired.

`addEventHandler(MOUSE_PRESSED, ...)` installs a listener in the **bubbling** phase. ButtonBase's internal capturing-phase handler (installed by the framework, not user code) runs first, arming the button. Then our handler runs and calls `consume()` — this stops the event from continuing to bubble up to the TableRow/TableView, preserving the desired behavior of not changing the selected row.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- ERR-07 is complete; the ChatGPT button fix is ready for validation
- Phase 27-01 (toolbar Save button) runs in parallel in the same wave

---
*Phase: 27-toolbar-save-chatgpt-fix*
*Completed: 2026-05-01*
