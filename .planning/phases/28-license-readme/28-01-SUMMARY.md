---
phase: 28
plan: 01
subsystem: documentation
tags: [license, readme, about-dialog, mit]
dependency_graph:
  requires: []
  provides: [DOC-01, DOC-02, DOC-03]
  affects: [README.md, LICENSE, AboutDialog]
tech_stack:
  added: []
  patterns: []
key_files:
  created:
    - LICENSE
  modified:
    - src/main/java/ch/ti/gagi/xsleditor/ui/AboutDialog.java
    - README.md
decisions:
  - MIT license text uses canonical OSI wording, copyright 2026 d4kr
  - README License section updated to point to LICENSE file
metrics:
  duration: ~5 minutes
  completed: 2026-05-01
---

# Phase 28 Plan 01: License & README Summary

**One-liner:** MIT LICENSE file added, AboutDialog license updated from Apache 2.0 to MIT, README logo moved above title with reduced size and version bumped to 0.5.0.

## Tasks Completed

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 | Add MIT LICENSE file | a157292 | LICENSE (new) |
| 2 | Update AboutDialog license label and URL | 4ea29ea | AboutDialog.java |
| 3 | Update README logo, version, license section | d7bf2b0 | README.md |

## What Was Built

### LICENSE (DOC-01)
Standard OSI MIT License text added to project root. Copyright line: `Copyright 2026 d4kr`.

### AboutDialog (DOC-02)
Two-line change in `AboutDialog.java` (~line 104):
- `Label("License: Apache 2.0")` → `Label("License: MIT")`
- Hyperlink URL: `https://www.apache.org/licenses/LICENSE-2.0` → `https://opensource.org/licenses/MIT`
No structural changes; same `HBox` layout retained.

### README (DOC-03)
- Logo moved above `# XSLEditor` heading
- Bare Markdown `![App icon](...)` replaced with `<img src="..." width="96">`
- Version bumped from `0.3.0` to `0.5.0` (badge, Build output path, Run command)
- License section updated from "Internal developer tool" to `MIT — see [LICENSE](LICENSE).`

## Deviations from Plan

**1. [Rule 2 - Missing functionality] README License section updated**
- **Found during:** Task 3
- **Issue:** README License section said "Internal developer tool — not distributed publicly" — inconsistent with the new MIT LICENSE file being added
- **Fix:** Updated section to `MIT — see [LICENSE](LICENSE).`
- **Files modified:** README.md
- **Commit:** d7bf2b0

No other deviations — plan executed as written for the three core requirements.

## Self-Check: PASSED

- LICENSE: FOUND
- AboutDialog.java: FOUND
- README.md: FOUND
- Commit a157292: FOUND
- Commit 4ea29ea: FOUND
- Commit d7bf2b0: FOUND
