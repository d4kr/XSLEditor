---
phase: 24-keyboard-accelerators
plan: "01"
subsystem: ui/fxml
tags:
  - javafx
  - fxml
  - menu
  - accelerator
dependency_graph:
  requires: []
  provides:
    - keyboard-accelerators-file-menu
  affects:
    - src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
tech_stack:
  added: []
  patterns:
    - JavaFX Shortcut+ token for cross-platform accelerators in FXML
key_files:
  created: []
  modified:
    - src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
decisions:
  - FXML-only change; no Java setAccelerator() calls needed
  - Used Shortcut+ token (not Ctrl+/Meta+) for cross-platform support
  - KBD-04/05 items (Set Entrypoint, Set XML Input) have no onAction in FXML; handler binding is in FileTreeController.initialize()
metrics:
  duration: "~5 minutes"
  completed_date: "2026-04-27T20:02:41Z"
  tasks_completed: 1
  tasks_total: 2
  files_modified: 1
requirements:
  - KBD-01
  - KBD-02
  - KBD-03
  - KBD-04
  - KBD-05
---

# Phase 24 Plan 01: Keyboard Accelerators (File Menu) Summary

**One-liner:** Added five cross-platform keyboard accelerators (Shortcut+O/N/Q/Shift+E/Shift+I) to File menu items in main.fxml via FXML attribute-only change.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Add accelerator attributes to five File menu items | 8d60f8f | src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml |

## Tasks Awaiting Human Verification

| Task | Name | Status |
|------|------|--------|
| 2 | Smoke-test all five keyboard accelerators in the running app | Awaiting human verification |

## What Was Built

Five keyboard accelerator attributes were added to `main.fxml` in the File menu:

- `menuItemOpenProject`: `accelerator="Shortcut+O"` (KBD-01)
- `menuItemNewFile`: `accelerator="Shortcut+N"` (KBD-02)
- Exit item: `accelerator="Shortcut+Q"` (KBD-03)
- `menuItemSetEntrypoint`: `accelerator="Shortcut+Shift+E"` (KBD-04, no onAction added)
- `menuItemSetXmlInput`: `accelerator="Shortcut+Shift+I"` (KBD-05, no onAction added)

Existing accelerators `F5` (Render) and `Ctrl+Shift+F` (Find in Files) are preserved.

The multi-line attribute format matches the existing `menuItemRender` pattern. `shadowJar` builds successfully; fat JAR at `build/libs/xsleditor.jar`.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None.

## Threat Flags

None — this is a static FXML attribute change. No new network endpoints, auth paths, file access patterns, or schema changes at trust boundaries. All threats were pre-analyzed in the plan's threat model and accepted or mitigated by existing controls.

## Self-Check: PASSED

- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` — modified, verified with grep checks
- Commit `8d60f8f` — verified with `git rev-parse --short HEAD`
- 5 Shortcut+ accelerators present: confirmed (`grep -c` returns 5)
- KBD-04/05 items have no onAction: confirmed (`awk` check returns 0)
- Existing F5 and Ctrl+Shift+F accelerators preserved: confirmed
- `./gradlew compileJava` exits 0: confirmed
- `./gradlew shadowJar` exits 0: confirmed
