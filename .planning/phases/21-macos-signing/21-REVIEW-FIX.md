---
phase: 21-macos-signing
fixed_at: 2026-04-26T00:00:00Z
review_path: .planning/phases/21-macos-signing/21-REVIEW.md
iteration: 1
findings_in_scope: 7
fixed: 5
skipped: 2
status: partial
---

# Phase 21: Code Review Fix Report

**Fixed at:** 2026-04-26
**Source review:** .planning/phases/21-macos-signing/21-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 7 (CR-01 through CR-04, WR-01 through WR-03)
- Fixed: 5 (CR-02, CR-03, CR-04, WR-01, WR-03)
- Skipped: 2 (CR-01 false positive, WR-02 structural)

---

## Fixed Issues

### CR-02 + CR-04: Verify .app bundle inside DMG with robust TeamIdentifier check

**Files modified:** `.github/workflows/release.yml`
**Commit:** e25a2ac
**Applied fix:** Both "Verify code signature" steps (arm64 and x64 jobs) now mount
the DMG with `hdiutil attach -mountpoint /tmp/xsleditor-verify-{arch} -nobrowse -quiet`,
run `codesign --verify --deep --strict --verbose=2` against the `.app` bundle inside,
capture TeamIdentifier with an explicit variable and exit 1 if empty. `set -euo pipefail`
added at the top of each verify step. The DMG is detached with `hdiutil detach` at the
end (and on error before exit). This fixes both the silent-pass on DMG-level verification
(CR-02) and the fragile grep-pipe exit code issue (CR-04).

---

### CR-03: Write signing certificate to chmod-700 temp directory

**Files modified:** `.github/workflows/release.yml`
**Commit:** 67daf47
**Applied fix:** Both "Import signing certificate" steps replaced
`echo ... > /tmp/certificate.p12` with a `CERT_DIR=$(mktemp -d)` / `chmod 700 "$CERT_DIR"`
sequence. The `.p12` is decoded into `$CERT_DIR/certificate.p12` and removed with
`rm -rf "$CERT_DIR"` immediately after import. The directory is mode 700, preventing
concurrent processes from reading it during the import window.

---

### WR-01: Pin softprops/action-gh-release to specific version

**Files modified:** `.github/workflows/release.yml`
**Commit:** e99cb93
**Applied fix:** Changed `softprops/action-gh-release@v2` (floating major tag) to
`softprops/action-gh-release@v2.3.2` (pinned patch release) in the release job.

---

### WR-03: Guard against empty MACOS_SIGNING_IDENTITY secret

**Files modified:** `.github/workflows/release.yml`
**Commit:** d415478
**Applied fix:** Added preflight guard in both "jpackage DMG" steps (arm64 and x64):
checks `[ -z "$MACOS_SIGNING_IDENTITY" ]` and exits with a clear error message before
invoking jpackage. This prevents jpackage from silently producing an unsigned DMG when
the secret is not configured.

---

## Skipped Issues

### CR-01: Non-existent GitHub Actions versions will fail every run

**File:** `.github/workflows/release.yml:57,66,75,95,157,180,242,265,321,326,337`
**Reason:** FALSE POSITIVE — do not fix. The reviewer's finding is based on stale
training data. Versions `actions/checkout@v6.0.2`, `actions/setup-java@v5.2.0`,
`actions/upload-artifact@v7.0.1`, and `actions/download-artifact@v8.0.1` have been
verified to exist via `gh api` at the time of this fix run. Reverting to v4 would
downgrade to older major versions unnecessarily. This finding is marked false positive
per explicit orchestrator instruction.
**Original issue:** Reviewer claimed these version tags do not exist in the marketplace.

---

### WR-02: Keychain import sequence is fully duplicated between arm64 and x64 jobs

**File:** `.github/workflows/release.yml:108-125,193-210`
**Reason:** Structural refactoring into a composite action requires introducing a new
`.github/actions/macos-sign-keychain/action.yml` file, which is a non-trivial change
involving directory creation and composite action YAML authoring. The CR-03 fix was
applied consistently to both jobs (both locations updated). The duplication is noted and
the risk is mitigated by always applying fixes to both jobs in the same commit. A
composite action extraction can be a follow-up improvement if the pattern grows further.
**Original issue:** Six-command keychain import sequence copy-pasted verbatim between
the two macOS jobs; future fixes must be applied in both places.

---

_Fixed: 2026-04-26_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
