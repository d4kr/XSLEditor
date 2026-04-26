---
phase: 21
slug: macos-signing
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-25
---

# Phase 21 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | none — CI shell commands + codesign CLI |
| **Config file** | `.github/workflows/release.yml` |
| **Quick run command** | `codesign --verify --deep --strict <dmg>` |
| **Full suite command** | `codesign --verify --deep --strict <dmg> && codesign -dv <dmg> \| grep TeamIdentifier` |
| **Estimated runtime** | ~30 seconds per DMG |

---

## Sampling Rate

- **After every task commit:** Review workflow diff for correctness
- **After every plan wave:** Push test tag, inspect CI logs for hang / non-zero exit
- **Before `/gsd-verify-work`:** `codesign --verify --deep --strict` must exit 0 on both DMG artifacts
- **Max feedback latency:** ~600 seconds (CI run time)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 21-01-01 | 01 | 1 | MACOS-03 | — | entitlements.plist committed to repo | file check | `test -f packaging/entitlements.plist` | ❌ W0 | ⬜ pending |
| 21-01-02 | 01 | 1 | MACOS-03 | — | keychain preamble present in release.yml | grep | `grep -c 'set-key-partition-list' .github/workflows/release.yml` | ✅ | ⬜ pending |
| 21-01-03 | 01 | 1 | MACOS-03 | — | jpackage --mac-sign flag present | grep | `grep -c '\-\-mac-sign' .github/workflows/release.yml` | ✅ | ⬜ pending |
| 21-01-04 | 01 | 2 | MACOS-03 | — | codesign verification step in workflow | grep | `grep -c 'codesign --verify' .github/workflows/release.yml` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `packaging/entitlements.plist` — must exist before jpackage signing step runs

*All other verifications are CI-execution checks (codesign CLI) or grep-based static checks on workflow YAML.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| codesign --verify passes on arm64 DMG | MACOS-03 | Requires real DMG artifact from CI run | Download artifact from successful CI run; run `codesign --verify --deep --strict XSLEditor-*.dmg` |
| codesign --verify passes on x64 DMG | MACOS-03 | Requires real DMG artifact from CI run | Download artifact from successful CI run; run `codesign --verify --deep --strict XSLEditor-*.dmg` |
| No GUI prompt hang on headless runner | MACOS-03 | Can only be observed in CI logs | Check Actions run logs for absence of "The operation couldn't be completed" or runner timeout |
| GitHub Actions secrets configured | MACOS-03 | Secrets are not readable post-creation | Confirm all 7 secrets exist in repo Settings > Secrets before test tag push |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 600s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
