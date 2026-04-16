---
status: partial
phase: 02-project-management
source: [02-VERIFICATION.md]
started: 2026-04-16T00:00:00Z
updated: 2026-04-16T00:00:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. File menu layout and initial disable state
expected: File menu items appear in correct order with separators; Set Entrypoint, Set XML Input disabled on launch; New File disabled on launch
result: [pending]

### 2. Open Project without config
expected: Title updates to "XLSEditor — [FolderName]"; 3-second status label appears; log entry written; New File becomes enabled
result: [pending]

### 3. Open Project with config
expected: .xslfo-tool.json loaded; entrypoint and XML input restored and logged
result: [pending]

### 4. New File — valid filename
expected: File created in project root; status feedback shown
result: [pending]

### 5. New File — path traversal (../escape.xsl)
expected: Alert(ERROR) shown; no file created outside project root (T-02-01)
result: [pending]

### 6. New File — duplicate filename
expected: Alert(WARNING) shown with filename reference; no file overwritten
result: [pending]

### 7. Cancel DirectoryChooser
expected: Strict no-op — no state change, no title change, no status label
result: [pending]

## Summary

total: 7
passed: 0
issues: 0
pending: 7
skipped: 0
blocked: 0

## Gaps
