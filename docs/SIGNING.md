# macOS Signing & Notarization — Maintainer Guide

> Configures the 7 GitHub Actions secrets required for the release workflow to sign and notarize macOS DMG installers using an Apple Developer ID certificate.

**Prerequisite:** An active Apple Developer Program membership ($99/yr) is required for code signing and notarization. Sign up at https://developer.apple.com/programs/.

## Overview

This guide walks a new repository maintainer through setting up all 7 GitHub Actions secrets that the release workflow uses to sign and notarize macOS DMG installers. Once these secrets are in place, every tag push to the repository will produce a Gatekeeper-accepted macOS application bundle. The Windows MSI is produced by the same workflow but is unsigned (see [Windows SmartScreen Note](#windows-smartscreen-note)).

## Setup Steps

### Step 1: Export the Developer ID Application certificate as .p12

Open Keychain Access → expand the `login` keychain → find the entry named `Developer ID Application: <Your Name> (<TEAM_ID>)` → right-click → Export → save as `cert.p12` with a strong password (you will need this password for `MACOS_CERTIFICATE_PASSWORD`).

### Step 2: Base64-encode the .p12

```bash
base64 -i /path/to/cert.p12 | pbcopy
```

The encoded value is now on your clipboard — paste it as the value of the `MACOS_CERTIFICATE` secret.

### Step 3: Generate a random keychain password

```bash
openssl rand -hex 16
```

Use the output as the value of `MACOS_KEYCHAIN_PASSWORD`. This password protects an ephemeral keychain that is created and destroyed on every CI run.

### Step 4: Obtain your Apple Team ID

Visit https://developer.apple.com/account → the 10-character ID under your name (e.g. `ABC123XYZ7`) is the value for `APPLE_TEAM_ID`.

### Step 5: Generate an App-Specific Password

Visit https://appleid.apple.com → Sign-In and Security → App-Specific Passwords → Generate. Use the resulting password for `APPLE_APP_SPECIFIC_PASSWORD`. Use your Apple ID email for `APPLE_ID`.

### Step 6: Determine MACOS_SIGNING_IDENTITY

**Gotcha:** `MACOS_SIGNING_IDENTITY` is the NAME PORTION ONLY of the certificate subject — NOT the full string.

```text
Correct:   Acme Corp
Incorrect: Developer ID Application: Acme Corp (ABC123XYZ7)
```

jpackage uses `--mac-signing-key-user-name` which expects only the name portion.

### Step 7: Add all 7 secrets in GitHub

Navigate to: GitHub → Settings → Secrets and variables → Actions → New repository secret. Add each of the 7 secrets listed below.

## Secrets Reference

The release workflow's authoritative secret specifications (reproduced from `.github/workflows/release.yml` per the single-source-of-truth principle):

```yaml
# =============================================================================
# Required GitHub Actions Secrets
# =============================================================================
# Configure these secrets at:
# GitHub → Settings → Secrets and variables → Actions → New repository secret
#
# ── Phase 21: macOS Signing (used in package-macos-arm and package-macos-x64) ──
#
# MACOS_CERTIFICATE
#   Base64-encoded Developer ID Application certificate (.p12 export from Keychain Access)
#   How to obtain: Keychain Access → right-click "Developer ID Application: Name (TEAM)"
#                  → Export → save as .p12 with a password
#   How to encode: base64 -i /path/to/cert.p12 | pbcopy
#
# MACOS_CERTIFICATE_PASSWORD
#   The password you set when exporting the .p12 from Keychain Access.
#
# MACOS_SIGNING_IDENTITY
#   The NAME PORTION ONLY of the certificate subject.
#   Correct:   Acme Corp
#   Incorrect: Developer ID Application: Acme Corp (ABC123XYZ7)
#   jpackage uses --mac-signing-key-user-name which expects the name only.
#
# MACOS_KEYCHAIN_PASSWORD
#   Any random string used as the password for the ephemeral CI keychain.
#   Generate: openssl rand -hex 16
#   This keychain is created and deleted on every CI run.
#
# ── Phase 22: macOS Notarization (used in package-macos-arm and package-macos-x64) ──
#
# APPLE_ID
#   Apple ID email address of the developer account that owns the certificate.
#
# APPLE_TEAM_ID
#   10-character team identifier from developer.apple.com/account (e.g. ABC123XYZ7)
#
# APPLE_APP_SPECIFIC_PASSWORD
#   App-specific password for the Apple ID.
#   Generate: appleid.apple.com → Sign-In and Security → App-Specific Passwords
#
# =============================================================================
```

## Verify Your Setup

Push a pre-release tag to trigger the release workflow without producing a public release:

```bash
git tag v0.4.0-test1
git push origin v0.4.0-test1
```

The `-` suffix in `v0.4.0-test1` causes the release to be auto-marked as pre-release. Watch the workflow at GitHub → Actions → Release. The `package-macos-arm` and `package-macos-x64` jobs should complete green. If `codesign --verify` or `notarytool submit` fails, check the job log for the specific secret that was rejected.

## Windows SmartScreen Note

The Windows MSI installer published by the release workflow is **unsigned** in v0.4.0. End users will see a Microsoft Defender SmartScreen warning when running the installer; they can bypass it via *More info → Run anyway*. Authenticode code signing is deferred to a future release because it requires an EV (Extended Validation) code-signing certificate (~$200–500/yr).
