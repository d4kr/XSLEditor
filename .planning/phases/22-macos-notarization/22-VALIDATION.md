---
phase: 22
slug: macos-notarization
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-26
---

# Phase 22 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | CI workflow logs (GitHub Actions) — no unit test framework applicable |
| **Config file** | `.github/workflows/release.yml` |
| **Quick run command** | `xcrun notarytool history --apple-id $APPLE_ID --team-id $APPLE_TEAM_ID --password $APPLE_APP_SPECIFIC_PASSWORD` |
| **Full suite command** | `xcrun spctl --assess --type open --context context:primary-signature --verbose=2 <dmg>` |
| **Estimated runtime** | ~300 seconds (notarization wait timeout) |

---

## Sampling Rate

- **After every task commit:** Validate YAML syntax of `release.yml` with `yq` or `yamllint`
- **After every plan wave:** Trigger test tag push; inspect CI logs for `Accepted` status
- **Before `/gsd-verify-work`:** Both DMGs stapled and `spctl` passes
- **Max feedback latency:** 300 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 22-01-01 | 01 | 1 | MACOS-04 | — | Secrets not logged in CI output | manual/CI | `grep -v 'APPLE' <ci-log>` | ✅ | ⬜ pending |
| 22-01-02 | 01 | 1 | MACOS-04 | — | notarytool exits with `Accepted` | CI | `xcrun notarytool submit --wait ...` | ✅ | ⬜ pending |
| 22-01-03 | 01 | 1 | MACOS-04 | — | stapler embeds ticket in DMG | CI | `xcrun stapler validate <dmg>` | ✅ | ⬜ pending |
| 22-01-04 | 01 | 1 | MACOS-04 | — | Gatekeeper accepts app offline | manual | Open app on clean macOS VM | ❌ manual | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `APPLE_ID`, `APPLE_TEAM_ID`, `APPLE_APP_SPECIFIC_PASSWORD` configured in GitHub Secrets — prerequisite for all CI verification

*No new test framework installation required — verification is via CI pipeline execution.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Gatekeeper quarantine dialog absent | MACOS-04 | Requires real macOS hardware/VM outside CI | Download stapled DMG, open app, confirm no quarantine dialog appears |
| Hardened Runtime flags set | MACOS-04 | Requires local codesign inspection | Run `codesign -dv --verbose=4 XSLEditor.app 2>&1 \| grep flags` and confirm `flags=0x10000(runtime)` |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 300s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
