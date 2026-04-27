---
phase: 23-signing-documentation
plan: "01"
subsystem: documentation
tags:
  - documentation
  - signing
  - macos
  - ci
dependency_graph:
  requires: []
  provides:
    - docs/SIGNING.md (macOS signing + notarization maintainer guide)
    - README.md Contributing section cross-linking to signing guide
    - release.yml pointer comment to signing guide
  affects:
    - New repository maintainers configuring CI signing secrets
tech_stack:
  added: []
  patterns:
    - Verbatim code block embedding from CI workflow (single source of truth)
    - Bold-prefixed prerequisite callouts (no GitHub admonition syntax)
    - Backtick-wrapped relative Markdown links for cross-references
key_files:
  created:
    - docs/SIGNING.md
  modified:
    - README.md
    - .github/workflows/release.yml
decisions:
  - Embedded verbatim release.yml secrets block in SIGNING.md to avoid maintaining two sources of truth (D-02)
  - Used bold-prefixed prose notes instead of GitHub admonition syntax to match README.md style (D-01)
  - MACOS_SIGNING_IDENTITY gotcha calls out name-portion-only requirement and jpackage flag explicitly
  - Windows SmartScreen note defers Authenticode to EV cert future milestone
metrics:
  duration: "100 seconds"
  completed: "2026-04-27"
  tasks_completed: 3
  tasks_total: 3
  files_changed: 3
---

# Phase 23 Plan 01: Signing Documentation Summary

Self-contained macOS signing and notarization maintainer guide in `docs/SIGNING.md` with all 7 GitHub Actions secrets documented, verbatim block from `release.yml`, cross-linked from `README.md` and `release.yml`.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Create docs/SIGNING.md with full secrets guide | 706826b | docs/SIGNING.md (created, 117 lines) |
| 2 | Add Contributing / Release Setup section to README.md | 74895e8 | README.md (+4 lines) |
| 3 | Add docs/SIGNING.md pointer comment to release.yml | 078775d | .github/workflows/release.yml (+1 line) |

## Files Created

- `docs/SIGNING.md` — Full maintainer guide for macOS signing + notarization secret configuration

## Files Modified

- `README.md` — Added `## Contributing / Release Setup` section with backtick-wrapped relative link to docs/SIGNING.md
- `.github/workflows/release.yml` — Added single pointer comment `# Full setup guide: docs/SIGNING.md` after closing secrets banner block

## Secret Names Covered

All 7 required secrets are documented in `docs/SIGNING.md`:

1. `MACOS_CERTIFICATE` — Base64-encoded Developer ID Application .p12 certificate
2. `MACOS_CERTIFICATE_PASSWORD` — Password used when exporting the .p12 from Keychain Access
3. `MACOS_SIGNING_IDENTITY` — Name portion only of certificate subject (gotcha documented)
4. `MACOS_KEYCHAIN_PASSWORD` — Ephemeral CI keychain password (generated via `openssl rand -hex 16`)
5. `APPLE_ID` — Apple ID email of the developer account
6. `APPLE_TEAM_ID` — 10-character team identifier from developer.apple.com/account
7. `APPLE_APP_SPECIFIC_PASSWORD` — App-specific password from appleid.apple.com

## Verbatim Block Source

The `## Secrets Reference` section in `docs/SIGNING.md` reproduces verbatim the content of `.github/workflows/release.yml` lines 1–41 (the entire `# Required GitHub Actions Secrets` banner-bracketed block) as a fenced `yaml` code block.

## Verification — All Acceptance Criteria Gates Passed

- `docs/SIGNING.md` exists with exactly 1 H1 and 5 H2 sections
- All 7 secret names present (14 total occurrences across prose + verbatim block)
- Apple Developer Program + $99/yr stated in prerequisite callout
- SmartScreen note present with EV cert deferral explanation
- `v0.4.0-test1` verify step present
- `base64 -i` and `openssl rand -hex 16` CLI commands present
- `--mac-signing-key-user-name` jpackage flag referenced in gotcha
- Phase 21 and Phase 22 banners present in verbatim block (`── Phase 21: macOS Signing`, `── Phase 22: macOS Notarization`)
- 1 fenced `yaml` block, 3 fenced `bash` blocks present
- No real secret values committed (no base64 payloads, only placeholder text and generation commands)
- README.md cross-link exact: `` [`docs/SIGNING.md`](docs/SIGNING.md) ``
- README.md H2 count increased from 9 to 10
- release.yml pointer line present, gap ≤ 3 lines to `name: Release`
- Workflow YAML still valid (python3 yaml.safe_load passed)
- README.md version line `**Version:** 0.3.0` untouched

## Deviations from Plan

None — plan executed exactly as written.

## Threat Flags

None. All content in `docs/SIGNING.md` uses only placeholder identifiers (`Acme Corp`, `ABC123XYZ7`), official Apple URLs, and CLI generation commands. No real secret values, no actual base64 payloads, no third-party intermediary URLs.

## Self-Check: PASSED

- `docs/SIGNING.md` exists: FOUND
- `README.md` cross-link present: FOUND
- `.github/workflows/release.yml` pointer comment present: FOUND
- Commit 706826b exists: FOUND
- Commit 74895e8 exists: FOUND
- Commit 078775d exists: FOUND
