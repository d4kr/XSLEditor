---
phase: 21-macos-signing
plan: "01"
subsystem: ci-signing
tags: [macos, codesign, jpackage, github-actions, hardened-runtime]
dependency_graph:
  requires: []
  provides: [packaging/entitlements.plist, macos-signing-ci-steps]
  affects: [.github/workflows/release.yml]
tech_stack:
  added: [entitlements.plist, Apple Hardened Runtime, security(1) keychain commands, codesign(1)]
  patterns: [ephemeral-keychain, base64-p12-secret, set-key-partition-list-headless]
key_files:
  created:
    - packaging/entitlements.plist
  modified:
    - .github/workflows/release.yml
decisions:
  - "Three JVM entitlements required: allow-jit, disable-library-validation, allow-unsigned-executable-memory"
  - "app-sandbox intentionally omitted — incompatible with JVM apps"
  - "security set-key-partition-list mandatory to prevent codesign GUI dialog hang on headless CI"
  - "MACOS_SIGNING_IDENTITY holds name portion only (not full Developer ID string)"
  - "Step-level env injection for MACOS_SIGNING_IDENTITY to minimize secret exposure scope"
  - "/tmp/certificate.p12 deleted immediately after security import (T-21-01 mitigation)"
metrics:
  duration: "~10 minutes"
  completed_date: "2026-04-26"
  tasks_completed: 2
  tasks_total: 3
  files_created: 1
  files_modified: 1
---

# Phase 21 Plan 01: macOS Signing — CI Sequence and Entitlements Summary

**One-liner:** Hardened Runtime entitlements.plist for JVM/JavaFX + ephemeral-keychain signing sequence in both macOS CI jobs with codesign verification and guaranteed keychain cleanup.

---

## Tasks Executed

### Task 1: Create packaging/entitlements.plist

Created `packaging/entitlements.plist` with the three entitlements required for a signed JavaFX/JVM app under Hardened Runtime:

- `com.apple.security.cs.allow-jit` — JVM JIT needs writable+executable memory; without this the app crashes at launch
- `com.apple.security.cs.disable-library-validation` — JavaFX and FOP load unsigned native .dylib files; also mitigates JDK-8358723 regression
- `com.apple.security.cs.allow-unsigned-executable-memory` — Java bytecode interpretation needs executable memory

`com.apple.security.app-sandbox` intentionally absent — incompatible with JVM apps.

**Commit:** `29e0bdf`

### Task 2: Inject signing steps into both macOS CI jobs

Modified `.github/workflows/release.yml` to add four new steps in each of `package-macos-arm` and `package-macos-x64`:

1. **Import signing certificate** — decodes base64 p12 from `MACOS_CERTIFICATE` secret, creates ephemeral `build.keychain`, imports the certificate, runs `security set-key-partition-list` (prevents headless hang), deletes `/tmp/certificate.p12` immediately
2. **jpackage DMG (signed)** — replaced unsigned jpackage step with signed variant; adds `--mac-sign`, `--mac-signing-key-user-name`, `--mac-entitlements packaging/entitlements.plist`, `--mac-package-identifier ch.ti.gagi.xsleditor`, `--mac-signing-keychain build.keychain`
3. **Verify code signature** — `codesign --verify --deep --strict` + `codesign -dv | grep TeamIdentifier`
4. **Delete keychain** (`if: always()`) — `security delete-keychain build.keychain || true`

Windows and release jobs are untouched.

**Commit:** `3423704`

### Task 3: Configure GitHub Secrets and push test tag

**Status: CHECKPOINT — awaiting human action**

The workflow is committed and ready. CI will fail until the four required secrets are configured in GitHub.

---

## Deviations from Plan

None — plan executed exactly as written.

---

## Known Stubs

None.

---

## Threat Flags

None beyond what is already documented in the plan's threat model. All `mitigate` dispositions have been implemented:

| Flag | File | Description |
|------|------|-------------|
| T-21-01 mitigated | .github/workflows/release.yml | `/tmp/certificate.p12` deleted with `rm -f` immediately after `security import` — present in both macOS jobs |

---

## Self-Check: PASSED

- `packaging/entitlements.plist` exists: FOUND
- commit `29e0bdf` exists: FOUND
- commit `3423704` exists: FOUND
- `grep -c "security set-key-partition-list" .github/workflows/release.yml` returns 2: PASSED
- `grep -c "if: always()" .github/workflows/release.yml` returns 2: PASSED
- `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/release.yml'))"` exits 0: PASSED
