---
phase: 21-macos-signing
reviewed: 2026-04-26T00:00:00Z
depth: standard
files_reviewed: 2
files_reviewed_list:
  - packaging/entitlements.plist
  - .github/workflows/release.yml
findings:
  critical: 4
  warning: 3
  info: 1
  total: 8
status: issues_found
---

# Phase 21: Code Review Report

**Reviewed:** 2026-04-26
**Depth:** standard
**Files Reviewed:** 2
**Status:** issues_found

## Summary

Two files were reviewed: the new `packaging/entitlements.plist` and the extended `.github/workflows/release.yml`. The entitlements plist is structurally correct. The workflow has four blocking defects: the GitHub Actions version pins reference versions that do not exist (the workflow will fail on first run), the `codesign --verify` target is a DMG (which silently passes without verifying the app bundle inside), the certificate is written to world-readable `/tmp`, and the `grep "TeamIdentifier"` pipe in the verify step can swallow a non-zero exit code, allowing verification to pass silently even when the signature is absent. Three additional warnings cover the floating `softprops/action-gh-release@v2` tag, duplicated code blocks, and missing `com.apple.security.cs.disable-executable-memory-protection` discussion.

---

## Critical Issues

### CR-01: Non-existent GitHub Actions versions will fail every run

**File:** `.github/workflows/release.yml:57,66,75,95,157,180,242,265,321,326,337`

**Issue:** Every action reference in this workflow uses a version tag that does not exist in the public GitHub Actions marketplace. As of the current date (2026-04-26), the real latest tags are `actions/checkout@v4`, `actions/setup-java@v4`, `actions/upload-artifact@v4`, and `actions/download-artifact@v4`. The workflow uses `@v6.0.2`, `@v5.2.0`, `@v7.0.1`, and `@v8.0.1` respectively — none of which are published. GitHub Actions resolves action tags at runtime; referencing a non-existent tag causes an immediate workflow failure with "Unable to resolve action". The RESEARCH.md itself (line 79) documents the correct version as `actions/setup-java@v4`, confirming these inflated version numbers are an implementation error.

**Fix:** Pin every action to its real latest version. Using exact SHAs is preferred for supply-chain safety, but at minimum:
```yaml
- uses: actions/checkout@v4
- uses: actions/setup-java@v4
  with:
    distribution: liberica
    java-version: '21'
    java-package: jdk+fx
- uses: actions/upload-artifact@v4
- uses: actions/download-artifact@v4
```
Repeat for all five jobs. The upload/download major versions must also match (both v4 currently).

---

### CR-02: `codesign --verify` on a DMG does not verify the app bundle signature

**File:** `.github/workflows/release.yml:151-155,236-240`

**Issue:** `jpackage --mac-sign` signs the `.app` bundle placed *inside* the DMG, not the DMG container itself. The DMG is an ordinary disk image — it is not a Mach-O binary or a bundle, so `codesign --verify --deep --strict` run directly against `XSLEditor-*.dmg` will either exit non-zero with "code object is not signed at all" (causing a false-positive failure) or exit 0 without traversing the bundle. Neither outcome actually verifies that the `.app` inside is correctly signed. The verification step as written provides no assurance that signing succeeded. This was a primary requirement of Phase 21 (MACOS-03).

**Fix:** Mount the DMG and verify the `.app` bundle inside:
```bash
# Mount the DMG
hdiutil attach "output/XSLEditor-${APP_VERSION}-arm64.dmg" \
  -mountpoint /tmp/xsleditor-verify -nobrowse -quiet

# Verify the .app bundle (this is what codesign actually signed)
codesign --verify --deep --strict --verbose=2 \
  "/tmp/xsleditor-verify/XSLEditor.app"

# Also confirm TeamIdentifier
codesign -dv --verbose=4 "/tmp/xsleditor-verify/XSLEditor.app" 2>&1 \
  | grep "TeamIdentifier"

# Detach
hdiutil detach /tmp/xsleditor-verify -quiet
```
Apply the same pattern in the x64 job.

---

### CR-03: Certificate written to world-readable `/tmp`

**File:** `.github/workflows/release.yml:114,199`

**Issue:** The decoded `.p12` certificate is written to `/tmp/certificate.p12` before being imported into the keychain. On macOS, `/tmp` is a symlink to `/private/tmp`, which is readable by all users on the system (`drwxrwxrwt`). Although GitHub Actions runners are ephemeral VMs (one per run), any process running concurrently in the same job — including third-party actions or malicious code introduced via a compromised dependency — can read the certificate from `/tmp` during the import window. The `rm -f` cleanup only runs after the import; if any step between decode and cleanup fails, the file may persist. The cleanup `rm -f` does not appear in `if: always()` and so will be skipped on failure.

**Fix:** Write to a dedicated directory with restricted permissions:
```bash
CERT_DIR=$(mktemp -d)
chmod 700 "$CERT_DIR"
echo "$MACOS_CERTIFICATE" | base64 --decode > "$CERT_DIR/certificate.p12"
# ... import ...
rm -rf "$CERT_DIR"
```
Alternatively, import directly from stdin if the `security import` command supports it:
```bash
echo "$MACOS_CERTIFICATE" | base64 --decode | \
  security import /dev/stdin -k build.keychain -f pkcs12 \
    -T /usr/bin/codesign -T /usr/bin/security \
    -P "$MACOS_CERTIFICATE_PASSWORD"
```
Either approach eliminates the window where the certificate file is world-readable.

---

### CR-04: `grep "TeamIdentifier"` pipe silently swallows verification failure

**File:** `.github/workflows/release.yml:154-155,239-240`

**Issue:** The verify step pipes `codesign -dv` output through `grep "TeamIdentifier"`. If `grep` finds no match (because the signature is absent or malformed), it exits with code 1. However, because this is the last command in the step's `run:` block, the step *does* fail — but only because of `grep`, not because the signature was correctly checked. More importantly, the step will also silently pass if `codesign -dv` exits non-zero but its stderr (redirected to stdout via `2>&1`) contains the word "TeamIdentifier" for any other reason (e.g., an error message that happens to include the word). This is a fragile signal. Additionally, without `set -e` / `set -o pipefail` in the `run:` block, intermediate failures in the pipeline are not guaranteed to be caught.

**Fix:** Use `spctl` for a definitive Gatekeeper assessment, and check `grep`'s exit explicitly:
```bash
# Verify app bundle directly (see CR-02 for mounting steps)
codesign --verify --deep --strict --verbose=2 \
  "/tmp/xsleditor-verify/XSLEditor.app"

# Confirm TeamIdentifier is present and non-empty
TEAM=$(codesign -dv --verbose=4 "/tmp/xsleditor-verify/XSLEditor.app" 2>&1 \
  | grep "^TeamIdentifier=" | cut -d= -f2)
if [ -z "$TEAM" ]; then
  echo "ERROR: TeamIdentifier not found in signature"
  exit 1
fi
echo "TeamIdentifier: $TEAM"
```

---

## Warnings

### WR-01: `softprops/action-gh-release@v2` is a floating mutable tag

**File:** `.github/workflows/release.yml:343`

**Issue:** `softprops/action-gh-release@v2` is a major-version floating tag. The tag owner can push any commit to it at any time; the workflow will silently pick up the change on the next run without any diff or review. This is a supply-chain risk: a compromised or misconfigured `v2` push could exfiltrate the `GITHUB_TOKEN` used to publish the release. The other actions (once fixed per CR-01) should also use SHA pins for the same reason.

**Fix:** Pin to a specific release SHA:
```yaml
- uses: softprops/action-gh-release@v2.3.2  # or a full commit SHA
```
Prefer a full commit SHA for maximum supply-chain assurance:
```yaml
- uses: softprops/action-gh-release@da05d552573ad5aba039eaac05058a918a7bf631
```

---

### WR-02: Keychain import sequence is fully duplicated between arm64 and x64 jobs

**File:** `.github/workflows/release.yml:108-125,193-210`

**Issue:** The six-command keychain import sequence (decode cert → create keychain → set-keychain-settings → unlock → import → list-keychains → set-key-partition-list → rm cert) is copy-pasted verbatim between `package-macos-arm` and `package-macos-x64`. Any future fix (e.g., CR-03) must be applied in two places; missing one silently leaves a vulnerability in one of the two jobs. The same applies to the jpackage invocation and the verify step.

**Fix:** Extract the repetitive logic into a reusable composite action under `.github/actions/macos-sign-keychain/action.yml`, or at minimum add a comment marking the duplication as intentional and requiring both locations to be kept in sync. Given that this is a two-job workflow, a composite action is the cleaner approach.

---

### WR-03: No guard against empty `MACOS_SIGNING_IDENTITY` secret

**File:** `.github/workflows/release.yml:129-145,214-230`

**Issue:** `MACOS_SIGNING_IDENTITY` is injected as an environment variable and passed directly to `--mac-signing-key-user-name "$MACOS_SIGNING_IDENTITY"`. If the secret is not configured (returns empty string), jpackage will be invoked with `--mac-signing-key-user-name ""`. Depending on the jpackage version, this may silently produce an unsigned DMG that still exits 0, or fail with a cryptic error. There is no pre-flight check that the secret is non-empty.

**Fix:** Add an explicit guard before the jpackage step:
```bash
if [ -z "$MACOS_SIGNING_IDENTITY" ]; then
  echo "ERROR: MACOS_SIGNING_IDENTITY secret is not configured"
  exit 1
fi
```
Apply the same pattern for the other required secrets (`MACOS_CERTIFICATE`, `MACOS_CERTIFICATE_PASSWORD`, `MACOS_KEYCHAIN_PASSWORD`).

---

## Info

### IN-01: `com.apple.security.cs.disable-library-validation` is a broad entitlement

**File:** `packaging/entitlements.plist:13-14`

**Issue:** `com.apple.security.cs.disable-library-validation` is the widest possible exception to Hardened Runtime library validation. It allows the app to load any unsigned or third-party-signed dynamic library, which undermines a meaningful part of the Hardened Runtime guarantee. The comment correctly notes this is required for the JDK-8358723/JDK-8369477 workaround and for JavaFX native libraries. However, Apple may scrutinize this entitlement during notarization (Phase 22). There is no comment explaining which specific libraries require this (JavaFX, FOP native, Saxon JNI) or whether narrower entitlements were evaluated and ruled out.

**Fix:** Expand the existing comment to document which libraries are loaded and why narrower alternatives (e.g., specific library exclusions) are insufficient. This will streamline any notarization review. No code change is required for Phase 21 functionality.

---

_Reviewed: 2026-04-26_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
