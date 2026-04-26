---
phase: 21-macos-signing
plan: "02"
subsystem: infra
tags: [github-actions, ci, macos, signing, secrets]

# Dependency graph
requires:
  - phase: 21-macos-signing/21-01
    provides: Import signing certificate + jpackage --mac-sign steps in both macOS jobs

provides:
  - Inline secrets documentation block at the top of release.yml covering all 7 required secrets
  - MACOS_SIGNING_IDENTITY naming caveat documented (name portion only, not full DN)
  - Phase 22 notarization secrets pre-documented (APPLE_ID, APPLE_TEAM_ID, APPLE_APP_SPECIFIC_PASSWORD)

affects: [21-macos-signing, 22-macos-notarization, 23-signing-documentation]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Secrets surface documented inline in workflow YAML via comment block — maintainer can understand full secret requirements without external docs"

key-files:
  created: []
  modified:
    - .github/workflows/release.yml

key-decisions:
  - "Comment block placed before `name: Release` line — top of file for maximum visibility"
  - "MACOS_SIGNING_IDENTITY caveat (name-only, not full DN) prominently documented to prevent misconfiguration"
  - "Phase 22 secrets documented as reserved-not-yet-used to clarify future workflow surface"

patterns-established:
  - "YAML comment blocks for secrets documentation: use # lines above name: key — valid YAML, ignored by GitHub Actions, visible to maintainers"

requirements-completed:
  - MACOS-03

# Metrics
duration: 8min
completed: 2026-04-26
---

# Phase 21 Plan 02: Secrets Documentation Comment Summary

**7-secret GitHub Actions surface documented inline in release.yml with per-secret obtain instructions and Phase 21/22 phase attribution**

## Performance

- **Duration:** 8 min
- **Started:** 2026-04-26T00:00:00Z
- **Completed:** 2026-04-26T00:08:00Z
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments

- Added 42-line YAML comment block at the top of release.yml documenting all 7 required secrets
- MACOS_SIGNING_IDENTITY caveat (name portion only, not full "Developer ID Application: Name (TEAM)" string) explicitly documented
- Phase 22 notarization secrets pre-documented as reserved, establishing clear scope boundary
- ROADMAP.md Phase 21 plan count already correctly set to 2 (done by Plan 01 — no additional change needed)

## Task Commits

1. **Task 1: Add secrets documentation comment to release.yml** - `9ecd68c` (docs)
2. **Task 2: Update ROADMAP.md Phase 21 plan count** - no commit needed (already correct from Plan 01)

**Plan metadata:** committed with SUMMARY.md

## Files Created/Modified

- `.github/workflows/release.yml` — 42-line secrets documentation comment block inserted before `name: Release`

## Decisions Made

- Comment block placed at the very top of the file (before `name: Release`) for maximum discoverability
- Phase 21 and Phase 22 secrets clearly separated with section headers

## Deviations from Plan

### Pre-existing Correct State

**1. [No Rule Required] Task 2 (ROADMAP.md update) already satisfied**
- **Found during:** Task 2
- **Issue:** None — ROADMAP.md already contained `**Plans**: 2 plans`, `21-01-PLAN.md`, and `21-02-PLAN.md` from Plan 01 execution
- **Fix:** No change needed; all acceptance criteria already passed
- **Verification:** `grep "2 plans"`, `grep "21-01-PLAN.md"`, `grep "21-02-PLAN.md"` all return matches
- **Committed in:** Not applicable — no modification required

---

**Total deviations:** 0 auto-fixes, 1 no-op (Task 2 pre-satisfied by Plan 01)
**Impact on plan:** No scope creep. Task 2 criteria were already met — ROADMAP.md was not re-touched unnecessarily.

## Issues Encountered

None — workflow file modified cleanly; YAML validation passes.

## User Setup Required

None — no external service configuration required by this plan. Secret values themselves are set via GitHub Settings → Secrets (documented in the added comment block).

## Next Phase Readiness

- release.yml now self-documenting for all 7 macOS secrets
- Phase 22 (notarization) can reference APPLE_ID / APPLE_TEAM_ID / APPLE_APP_SPECIFIC_PASSWORD documentation already in place
- Phase 23 (docs/SIGNING.md) has a reference model to follow — the comment block mirrors what SIGNING.md will contain

## Self-Check

- [x] `.github/workflows/release.yml` exists and begins with `# ===` comment block
- [x] `head -5 release.yml` returns "Required GitHub Actions Secrets" line
- [x] `python3 yaml.safe_load` exits 0 — valid YAML
- [x] `grep -c "security set-key-partition-list"` returns 2 — Plan 01 steps intact
- [x] commit `9ecd68c` exists in git log

## Self-Check: PASSED

---
*Phase: 21-macos-signing*
*Completed: 2026-04-26*
