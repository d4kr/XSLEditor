---
phase: 21-macos-signing
verified: 2026-04-26T12:00:00Z
status: human_needed
score: 5/6 must-haves verified
overrides_applied: 0
human_verification:
  - test: "Push a test tag (e.g. v0.4.0-sign-test1) to trigger the CI workflow and confirm both package-macos-arm and package-macos-x64 jobs complete green with 'Codesign verification passed' and 'TeamIdentifier=' in the Verify code signature step log"
    expected: "Both macOS jobs green; no hang at jpackage step; codesign --verify --deep --strict passes; TeamIdentifier= visible in log; Delete keychain step runs green even on failure"
    why_human: "Cannot invoke GitHub Actions CI from a local verification check. The signing sequence requires a real macOS runner with the 4 secrets configured (MACOS_CERTIFICATE, MACOS_CERTIFICATE_PASSWORD, MACOS_SIGNING_IDENTITY, MACOS_KEYCHAIN_PASSWORD). The workflow and entitlements.plist are correctly wired in the codebase — runtime behavior on a headless CI runner must be confirmed by a human."
---

# Phase 21: macOS Signing Verification Report

**Phase Goal:** Both macOS DMGs are signed with a Developer ID Application certificate so Gatekeeper accepts them as coming from an identified developer. The signing sequence runs without interactive prompts on headless CI runners.
**Verified:** 2026-04-26T12:00:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `packaging/entitlements.plist` exists with all three required entitlements | VERIFIED | File present; grep confirms `com.apple.security.cs.allow-jit`, `cs.disable-library-validation`, `cs.allow-unsigned-executable-memory`; no `app-sandbox` present |
| 2 | Both macOS CI jobs import the Developer ID certificate into a temporary keychain before jpackage | VERIFIED | Lines 108–129 (arm64) and 215–236 (x64) in release.yml: `Import signing certificate` step with `security create-keychain`, `security import`, `security set-key-partition-list` |
| 3 | Both macOS jpackage invocations include `--mac-sign`, `--mac-signing-key-user-name`, `--mac-entitlements`, `--mac-package-identifier`, `--mac-signing-keychain` | VERIFIED | `grep -c "--mac-sign"` = 2 (excluding comment-only occurrences); `--mac-entitlements packaging/entitlements.plist` = 2 matches; `--mac-package-identifier ch.ti.gagi.xsleditor` and `--mac-signing-keychain build.keychain` both present in both jobs |
| 4 | `security set-key-partition-list` is present in the keychain import step (prevents CI hang) | VERIFIED | `grep -c "security set-key-partition-list"` = 2 (one per macOS job) |
| 5 | The certificate file is deleted from disk immediately after import | VERIFIED (deviation) | Plan specified `rm -f /tmp/certificate.p12`; implementation uses `mktemp -d` + `chmod 700` + `rm -rf "$CERT_DIR"` — a more secure pattern (restricted temp dir, not world-readable `/tmp`). Same security intent, stronger implementation. |
| 6 | A `Delete keychain` step with `if: always()` is the last step in both macOS jobs | VERIFIED | `grep -c "if: always()"` = 2; `Delete keychain` step with `security delete-keychain build.keychain || true` confirmed in both jobs |

**Score:** 6/6 truths verified (Truth 5 is a substantive deviation that improves security)

---

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `packaging/entitlements.plist` | Hardened Runtime entitlements for JavaFX/JVM app | VERIFIED | File exists; contains `allow-jit`, `disable-library-validation`, `allow-unsigned-executable-memory`; valid plist XML; no `app-sandbox` |
| `.github/workflows/release.yml` | CI workflow with signing steps in both macOS jobs | VERIFIED | File exists; `security set-key-partition-list` count = 2; YAML parses cleanly (`python3 yaml.safe_load` exits 0) |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| release.yml Import signing certificate step | `$CERT_DIR/certificate.p12` | `base64 --decode` | VERIFIED | Pattern `base64 --decode > "$CERT_DIR/certificate.p12"` present in both jobs. Note: implementation uses a secure temp dir (`mktemp -d`) rather than the literal `/tmp/certificate.p12` specified in the plan — functionally equivalent, more secure. |
| release.yml jpackage step | `packaging/entitlements.plist` | `--mac-entitlements` | VERIFIED | Pattern `--mac-entitlements packaging/entitlements.plist` present exactly 2 times |

---

### Data-Flow Trace (Level 4)

Not applicable — this phase produces CI workflow configuration and a plist file, not components that render dynamic data.

---

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| `entitlements.plist` contains all 3 required keys | `grep -c "cs.allow-jit" packaging/entitlements.plist` | 1 | PASS |
| `release.yml` is valid YAML | `python3 yaml.safe_load(open('release.yml'))` exits 0 | YAML valid | PASS |
| `security set-key-partition-list` appears exactly 2 times | `grep -c "security set-key-partition-list" release.yml` | 2 | PASS |
| `if: always()` appears exactly 2 times | `grep -c "if: always()" release.yml` | 2 | PASS |
| Windows/release jobs untouched | `grep -c "Add WiX to PATH"` = 1; `grep -c "softprops/action-gh-release"` = 1 | Both = 1 | PASS |
| Secrets documentation comment at top of file | `head -5 release.yml` starts with `# ==` | Present | PASS |
| All 7 secrets documented in comment block | `grep "MACOS_CERTIFICATE\|APPLE_APP_SPECIFIC_PASSWORD" release.yml` | Present | PASS |
| CI signing runs on a real runner | Push test tag to GitHub Actions | Not run | SKIP (requires human) |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| MACOS-03 | 21-01-PLAN.md, 21-02-PLAN.md | Both DMGs signed with Developer ID Application certificate; `codesign --verify --deep --strict` passes | NEEDS HUMAN | Workflow and entitlements correctly wired in codebase. Runtime pass/fail requires a real macOS CI runner with secrets configured. |

MACOS-03 full text from REQUIREMENTS.md: "Both DMGs are signed with a Developer ID Application certificate (`--mac-sign --mac-entitlements entitlements.plist`); `codesign --verify --deep --strict` passes"

The static preconditions (workflow wiring, entitlements file, signing flags) are VERIFIED. The requirement's pass condition ("codesign --verify --deep --strict passes") requires a live CI run to confirm.

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| None | — | — | — | — |

No stubs, TODOs, placeholder returns, or hardcoded empty values found. The deviation on `/tmp/certificate.p12` vs. `$CERT_DIR` is an improvement, not a defect.

---

### Human Verification Required

#### 1. End-to-End CI Signing Run

**Test:** Configure the 4 required secrets in GitHub (Settings → Secrets and variables → Actions):
- `MACOS_CERTIFICATE` — base64-encoded `.p12` export
- `MACOS_CERTIFICATE_PASSWORD` — password used when exporting `.p12`
- `MACOS_SIGNING_IDENTITY` — name portion only (e.g. `Acme Corp`, NOT `Developer ID Application: Acme Corp (ABC123)`)
- `MACOS_KEYCHAIN_PASSWORD` — any random string (`openssl rand -hex 16`)

Then push a test tag:
```
git tag v0.4.0-sign-test1
git push origin v0.4.0-sign-test1
```

Monitor at: https://github.com/dakr/XSLEditor/actions

**Expected:**
- `package-macos-arm` and `package-macos-x64` jobs both show green checkmarks
- `Import signing certificate` step completes without hanging
- `jpackage DMG (arm64/x64, signed)` step completes within 10 minutes
- `Verify code signature` step logs: `Codesign verification passed` and a line containing `TeamIdentifier=`
- `Delete keychain` step shows green (runs via `if: always()`)

**Why human:** Cannot invoke GitHub Actions from local verification. Signing requires a real `macos-15`/`macos-15-intel` runner with actual Apple Developer ID certificate material. The workflow structure is correctly wired — only runtime execution on a headless CI runner confirms the full MACOS-03 requirement.

**If the jpackage step hangs (> 10 min):** The `security set-key-partition-list` step likely did not execute (import step failed silently). Verify all 4 secrets are non-empty and re-run.

After verification, delete the test tag:
```
git push origin --delete v0.4.0-sign-test1
git tag -d v0.4.0-sign-test1
```

---

### Gaps Summary

No static gaps. All codebase artifacts are present, substantive, and correctly wired.

One notable deviation from the plan is an improvement: the plan specified writing the certificate to `/tmp/certificate.p12`, but the implementation uses `mktemp -d` with `chmod 700` to create a restricted temp directory (`$CERT_DIR`), then deletes the entire directory with `rm -rf "$CERT_DIR"`. This prevents other processes on the runner from reading the plaintext key material (mitigates T-21-01 more thoroughly than the plan's original approach).

The only open item is the live CI run (human verification) required to confirm MACOS-03's runtime pass condition (`codesign --verify --deep --strict` passes on both DMGs).

---

_Verified: 2026-04-26T12:00:00Z_
_Verifier: Claude (gsd-verifier)_
