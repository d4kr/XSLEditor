---
phase: 19
plan: 02
subsystem: build/packaging
tags: [jpackage, icon, macos, app-bundle]
dependency_graph:
  requires: [19-01]
  provides: [icon.icns, local-app-bundle]
  affects: [build/packaging]
tech_stack:
  added: [jpackage, sips, iconutil]
  patterns: [fat-jar-to-app-image]
key_files:
  created:
    - src/main/resources/ch/ti/gagi/xsleditor/icon.icns
  modified: []
decisions:
  - JAR name is xsleditor-0.4.0.jar (lowercase), not XSLEditor-*.jar — adapted jpackage invocation
metrics:
  duration: ~5 minutes
  completed: 2026-04-25
  tasks_completed: 5
  files_changed: 1
---

# Phase 19 Plan 02: Local jpackage Bundle with Icon Summary

**One-liner:** icon.png converted to icon.icns via sips+iconutil; jpackage produced XSLEditor.app (288K icon embedded in bundle).

## What Was Done

1. Verified `icon.png` exists at `src/main/resources/ch/ti/gagi/xsleditor/icon.png`
2. Converted `icon.png` to `icon.icns` using `sips` (resize to all required sizes) + `iconutil -c icns`
3. Ran `jpackage --type app-image` with the fat JAR `xsleditor-0.4.0.jar`, producing `/tmp/xsleditor-package/XSLEditor.app`
4. Verified `.app` bundle contents: launcher binary + `XSLEditor.icns` (288K) in Resources
5. Committed `icon.icns` to repo (commit `9e06e9e`)

## Verification Checklist

- [x] `icon.icns` exists at `src/main/resources/ch/ti/gagi/xsleditor/icon.icns`
- [x] `/tmp/xsleditor-package/XSLEditor.app` created without errors
- [x] App bundle contains correct icon (`XSLEditor.icns` 288K in Contents/Resources/)
- [x] `icon.icns` committed to repo

## Commits

| Hash | Message |
|------|---------|
| 9e06e9e | feat(build): add icon.icns for jpackage native bundle |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] JAR name mismatch**
- **Found during:** Task 3
- **Issue:** Plan referenced `XSLEditor-*.jar` but actual file is `xsleditor-0.4.0.jar` (lowercase, gradle default naming)
- **Fix:** Used `xsleditor-0.4.0.jar` directly in jpackage invocation
- **Files modified:** None (invocation-only change)

## Notes for CI (Phase 20)

- macOS runners: `--icon src/main/resources/ch/ti/gagi/xsleditor/icon.icns` + `--type dmg`
- Windows runners: convert `icon.png` to `icon.ico` via ImageMagick; reference as `--icon icon.ico`
- The `--java-options "--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED"` flag was included preemptively per plan guidance

## Self-Check: PASSED

- FOUND: src/main/resources/ch/ti/gagi/xsleditor/icon.icns
- FOUND: /tmp/xsleditor-package/XSLEditor.app
- FOUND: commit 9e06e9e
