---
status: partial
phase: 27-toolbar-save-chatgpt-fix
source: [27-VERIFICATION.md]
started: 2026-05-01T00:00:00Z
updated: 2026-05-01T00:00:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. Save button enable/disable transitions
expected: Save button is disabled when no tab is open or active tab is clean. Becomes enabled when the active tab is dirty (after any edit). Returns to disabled after saving.

result: [pending]

### 2. Tab-switch updates Save button state
expected: Switching tabs immediately updates the Save button state to reflect the newly active tab's dirty state (no lag, no stale binding).

result: [pending]

### 3. ChatGPT button opens browser with correct URL
expected: Clicking the 💬 button in the log panel opens the default browser with a pre-filled ChatGPT URL containing the log entry's error message.

result: [pending]

### 4. ChatGPT button does not change log row selection
expected: Clicking the 💬 button does not select/deselect the underlying log table row.

result: [pending]

## Summary

total: 4
passed: 0
issues: 0
pending: 4
skipped: 0
blocked: 0

## Gaps
