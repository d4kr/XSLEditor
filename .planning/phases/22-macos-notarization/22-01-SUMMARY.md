---
phase: 22-macos-notarization
plan: "01"
subsystem: ci
tags:
  - ci
  - macos
  - notarization
  - github-actions
dependency_graph:
  requires:
    - "21-macos-signing/21-01 (signed DMG artifacts in output/)"
  provides:
    - "Notarized + stapled DMGs in both macOS CI jobs"
    - "Gatekeeper offline acceptance for end users"
  affects:
    - ".github/workflows/release.yml"
tech_stack:
  added: []
  patterns:
    - "xcrun notarytool submit --wait --timeout 300s with inline credentials"
    - "xcrun stapler staple modifies DMG in place before upload-artifact"
    - "Per-step env: secret injection (never job-level)"
    - "Hard-fail guard for missing secrets (exit 1)"
    - "Informational-only step with || true (spctl)"
key_files:
  created: []
  modified:
    - ".github/workflows/release.yml"
decisions:
  - "Inline credentials for notarytool (--apple-id / --team-id / --password) rather than store-credentials keychain profile — ephemeral runners make keychain storage pointless"
  - "Hard-fail on missing Apple secrets (exit 1) rather than skip-with-warning — ensures CI never silently publishes an un-notarized DMG"
  - "No third-party Actions used (GuillaumeFalourd/notary-tools) — direct xcrun calls are simpler and avoid unreviewed dependencies"
  - "spctl Gatekeeper step is informational-only (|| true) — CI runner spctl is known-unreliable; authoritative gate is notarytool status: Accepted"
metrics:
  duration: "~8 minutes"
  completed: "2026-04-26"
  tasks_completed: 2
  tasks_total: 3
  files_changed: 1
---

# Phase 22 Plan 01: macOS Notarization — CI Workflow Summary

**One-liner:** Added `xcrun notarytool submit --wait` + `xcrun stapler staple` to both macOS CI jobs so downloaded DMGs pass Gatekeeper offline without a quarantine dialog.

## What Was Built

Three new steps inserted into both `package-macos-arm` (arm64) and `package-macos-x64` (x64) jobs in `.github/workflows/release.yml`:

1. **Notarize DMG** — submits the signed DMG to Apple's notary service using inline credentials from GitHub Secrets; hard-fails if any Apple secret is missing; `--wait --timeout 300s` blocks until Apple returns a definitive status or times out at 5 minutes.
2. **Staple DMG** — runs `xcrun stapler staple` to embed the notarization ticket directly into the DMG file (modifies in place); the existing `upload-artifact` step picks up the stapled file automatically.
3. **Verify Gatekeeper acceptance (informational)** — runs `spctl -a -v --type install` with `|| true`; output is advisory only since CI runner spctl is unreliable.

**Step ordering invariant satisfied in both jobs:**
```
Import signing certificate
→ jpackage DMG (signed)
→ Verify code signature
→ Notarize DMG          ← NEW (Phase 22)
→ Staple DMG            ← NEW (Phase 22)
→ Verify Gatekeeper     ← NEW (Phase 22, informational)
→ actions/upload-artifact  (unchanged; uploads stapled DMG)
→ Delete keychain (always())
```

## Diff Summary (line ranges)

**package-macos-arm job** — new steps at lines 179–211 (32 lines added):
- `Notarize DMG (arm64)`: lines 179–198
- `Staple DMG (arm64)`: lines 199–204
- `Verify Gatekeeper acceptance (informational, arm64)`: lines 205–209

**package-macos-x64 job** — new steps at lines 318–350 (32 lines added):
- `Notarize DMG (x64)`: lines 318–337
- `Staple DMG (x64)`: lines 338–343
- `Verify Gatekeeper acceptance (informational, x64)`: lines 344–348

Total: 64 lines added, 0 lines modified.

## Commits

| Task | Description | Hash | Files |
|------|-------------|------|-------|
| Task 1 | Notarize + staple + Gatekeeper for arm64 job | b93d79f | .github/workflows/release.yml (+32 lines) |
| Task 2 | Notarize + staple + Gatekeeper for x64 job | 393374d | .github/workflows/release.yml (+32 lines) |

## Threat Mitigations Applied

| Threat ID | Mitigation |
|-----------|------------|
| T-22-01 | Secrets injected via per-step `env:` only; never echoed to stdout; GitHub Actions auto-masks |
| T-22-03 | `--timeout 300s` prevents 6-hour CI hangs from Apple notary service delays |
| T-22-06 | Hard-fail guard (`exit 1`) when any Apple secret is empty — CI fails loudly rather than publishing un-notarized DMG |

## Static Verification Results (post-both-tasks)

| Check | Expected | Actual |
|-------|----------|--------|
| YAML valid (python3 yaml.safe_load) | exit 0 | PASS |
| xcrun notarytool submit occurrences | 2 | 2 |
| xcrun stapler staple occurrences | 2 | 2 |
| --timeout 300s occurrences | 2 | 2 |
| arm64 step ordering (hdiutil detach < Notarize < upload) | true | PASS |
| x64 step ordering (hdiutil detach < Notarize < upload) | true | PASS |

## Awaiting Human Verification (Task 3)

The following remain to be verified by the developer:

1. Configure three GitHub Secrets if not already present: `APPLE_ID`, `APPLE_TEAM_ID`, `APPLE_APP_SPECIFIC_PASSWORD`
2. Push a test tag: `git tag v0.4.0-notarize-test1 && git push origin v0.4.0-notarize-test1`
3. Watch CI — both macOS jobs must log `status: Accepted` (Notarize step) and `The staple and validate action worked!` (Staple step)
4. Download the stapled DMG; open on a real macOS machine; confirm no Gatekeeper quarantine dialog appears
5. Locally: `xcrun stapler validate ~/Downloads/XSLEditor-*-arm64.dmg` must output `The validate action worked!`

See Task 3 in 22-01-PLAN.md for exact verification steps.

## Deviations from Plan

None — plan executed exactly as written. The verbatim YAML blocks from the plan's `<action>` sections were inserted at the precise locations specified in `<interfaces>`. No existing Phase 20/21 steps were modified.

## Known Stubs

None — this plan is purely additive CI configuration. No UI components or data stubs.

## Threat Flags

None — no new network endpoints, auth paths, or file access patterns beyond those already described in the plan's threat model.

## Self-Check: PASSED
