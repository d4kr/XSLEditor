# Phase 21: macOS Signing — Research

**Researched:** 2026-04-25
**Domain:** macOS code signing, jpackage `--mac-sign`, GitHub Actions headless keychain, entitlements.plist
**Confidence:** HIGH (core mechanics verified against official jpackage docs, Apple security commands, and production CI examples)

---

## Summary

Phase 21 adds Developer ID Application code signing to both macOS DMG jobs in the existing `release.yml`
workflow. The unsigned pipeline from Phase 20 already produces the DMGs; this phase injects a keychain
setup sequence before `jpackage` and adds `--mac-sign`, `--mac-signing-key-user-name`, and
`--mac-entitlements` flags to the existing jpackage invocations.

The mechanics divide into four areas: (1) certificate lifecycle — import a base64-encoded `.p12` into
a temporary CI keychain on every run; (2) partition list — a mandatory `security set-key-partition-list`
command that allows `codesign` to access the keychain without an interactive GUI prompt; (3) jpackage
signing flags — tell jpackage which identity to use and which entitlements to apply; (4) verification —
`codesign --verify --deep --strict` on the produced DMG. None of these steps require installing anything
beyond what is pre-installed on `macos-15` and `macos-15-intel`.

There is one known OpenJDK regression to be aware of: JDK-8358723 (introduced in JDK 21+35) causes
the main launcher binary inside the `.app` bundle to be signed without entitlements. The fix is
tracked in JDK-8369477 (a JDK 21 backport), but it is not yet confirmed in Liberica 21.0.7. The
workaround is to include the necessary entitlements in `entitlements.plist` and pass them via
`--mac-entitlements` — which is already the required pattern for Hardened Runtime and notarization
readiness.

**Primary recommendation:** Extend `package-macos-arm` and `package-macos-x64` jobs in `release.yml`
with a three-step signing preamble (decode cert → create/configure keychain → import cert → partition
list), then append `--mac-sign`, `--mac-signing-key-user-name`, and `--mac-entitlements` to each
jpackage invocation. Commit `packaging/entitlements.plist` to the repository. Add a post-jpackage
`codesign --verify --deep --strict` step to gate on success.

---

## Current CI State (What Phase 20 Built)

The file `.github/workflows/release.yml` contains five jobs:

| Job | Runner | What it produces |
|-----|--------|-----------------|
| `build-jar` | ubuntu-latest | `fat-jar` artifact (fat JAR) |
| `package-macos-arm` | macos-15 | `dmg-arm64` artifact (unsigned DMG) |
| `package-macos-x64` | macos-15-intel | `dmg-x64` artifact (unsigned DMG) |
| `package-windows` | windows-latest | `windows-msi`, `windows-zip` artifacts |
| `release` | ubuntu-latest | GitHub Release with all 5 assets |

The two macOS jobs currently call `jpackage --type dmg` without any `--mac-sign` flag. They use
`VERSION_MACOS` (major bumped to 1 when source version is 0.x). No secrets are referenced in these
jobs today.

[VERIFIED: read `.github/workflows/release.yml` directly]

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Certificate storage | GitHub Secrets | — | CI runners are ephemeral; secrets injected as env vars per run |
| Keychain lifecycle | CI / macOS runner (shell) | — | `security` CLI only available on macOS; must run in the signing job |
| jpackage signing | CI / macOS runner (jpackage) | — | Signing identity must be in keychain on the jpackage host |
| entitlements.plist | Repository (`packaging/`) | — | Committed to repo; referenced by path in jpackage invocation |
| Signature verification | CI / macOS runner (post-jpackage step) | — | `codesign --verify` must run on the same machine that produced the DMG |

---

## Standard Stack

### Core Tools (pre-installed on macOS runners, no install needed)

| Tool | Source | Purpose | Availability |
|------|--------|---------|-------------|
| `codesign` | Xcode CLT 16.4 | Sign and verify bundles | Pre-installed on `macos-15` and `macos-15-intel` [VERIFIED: runner-images macos-15-arm64-Readme.md] |
| `security` | macOS system | Keychain management (create/unlock/import/partition-list) | Always available on macOS |
| `base64` | macOS system | Decode certificate from GitHub secret | Always available |
| `jpackage` (JDK 21) | Installed by `actions/setup-java@v4` | Build and sign DMG | Available after setup-java step |

### No New Actions Required

The signing sequence uses only shell commands. The existing `actions/setup-java@v4` step already
installs jpackage.

**Optional alternative:** `apple-actions/import-codesign-certs@v3` is a community action that
wraps the keychain setup steps. It is acceptable but adds an unreviewed dependency. The manual
`security` command sequence is preferred for transparency and control.
[ASSUMED — preference based on analysis; not a documented requirement]

---

## Architecture Patterns

### Updated System Architecture Diagram

```
git push tag v*.*.*
        |
        v
  [trigger: on.push.tags]
        |
        +---> [job: build-jar]           ubuntu-latest (unchanged from Phase 20)
        |       → fat-jar artifact
        |
        +---> [job: package-macos-arm]   macos-15
        |       download fat-jar
        |       setup-java (liberica jdk+fx 21)
        |       ── NEW: decode MACOS_CERTIFICATE → certificate.p12
        |       ── NEW: security create-keychain → build.keychain
        |       ── NEW: security set-keychain-settings -lut 21600
        |       ── NEW: security unlock-keychain
        |       ── NEW: security import certificate.p12 -T /usr/bin/codesign
        |       ── NEW: security list-keychains (register build.keychain)
        |       ── NEW: security set-key-partition-list -S apple-tool:,apple: ← CRITICAL
        |       jpackage --type dmg
        |         --mac-sign
        |         --mac-signing-key-user-name "$MACOS_SIGNING_IDENTITY"
        |         --mac-entitlements packaging/entitlements.plist
        |       ── NEW: codesign --verify --deep --strict output/XSLEditor-*.dmg
        |       ── NEW: security delete-keychain build.keychain (cleanup)
        |       → dmg-arm64 artifact (SIGNED)
        |
        +---> [job: package-macos-x64]   macos-15-intel (identical pattern)
        |       → dmg-x64 artifact (SIGNED)
        |
        +---> [job: package-windows]     windows-latest (unchanged)
        |
        +---> [job: release]             ubuntu-latest (unchanged)
                → GitHub Release with signed DMGs
```

### New Files to Commit

```
packaging/
└── entitlements.plist     # committed; referenced in jpackage invocation
```

---

## macOS Code Signing Mechanics

### Developer ID Certificate Flow

1. Developer exports "Developer ID Application" certificate from Keychain Access as `.p12` (PKCS12)
2. `.p12` is base64-encoded: `base64 -i cert.p12 | pbcopy`
3. The base64 string is stored as `MACOS_CERTIFICATE` GitHub secret
4. `MACOS_CERTIFICATE_PASSWORD` stores the `.p12` export password
5. On each CI run: decode → import into temporary keychain → jpackage signs → delete keychain

[VERIFIED: localazy.com guide, Federico Terzi guide, Apple developer documentation pattern]

### The 7 Required GitHub Secrets

| Secret | Value | Used by |
|--------|-------|---------|
| `MACOS_CERTIFICATE` | Base64-encoded `.p12` file content | Keychain import step |
| `MACOS_CERTIFICATE_PASSWORD` | Password protecting the `.p12` | `security import -P` flag |
| `MACOS_SIGNING_IDENTITY` | Name portion of cert: `"John Doe"` (not full string) | `--mac-signing-key-user-name` |
| `MACOS_KEYCHAIN_PASSWORD` | Any random string; password for the temporary CI keychain | `security create-keychain -p` |
| `APPLE_ID` | Apple ID email (for Phase 22 notarization only) | Phase 22 |
| `APPLE_TEAM_ID` | 10-char Apple Team ID e.g. `ABC123XYZ7` (for Phase 22) | Phase 22 |
| `APPLE_APP_SPECIFIC_PASSWORD` | App-specific password for Apple ID (for Phase 22) | Phase 22 |

Phase 21 uses only the first four. The remaining three are documented now (for SIGN-01 readiness)
but not exercised until Phase 22.

[VERIFIED: MACOS_SIGNING_IDENTITY format confirmed — Apple Developer Forums thread 718795:
"The value for --mac-signing-key-user-name should be the name portion only, not the full string
'Developer ID Application: Name (ID)'. Use just 'Name'."]

### Keychain Setup Sequence (Exact Commands)

```bash
# 1. Decode certificate from GitHub secret
echo "$MACOS_CERTIFICATE" | base64 --decode > /tmp/certificate.p12

# 2. Create temporary keychain (random password; the keychain is ephemeral)
security create-keychain -p "$MACOS_KEYCHAIN_PASSWORD" build.keychain

# 3. Set 6-hour timeout (prevents premature locking during slow packaging jobs)
security set-keychain-settings -lut 21600 build.keychain

# 4. Unlock keychain (must unlock before import)
security unlock-keychain -p "$MACOS_KEYCHAIN_PASSWORD" build.keychain

# 5. Import certificate, granting codesign (and related tools) access
security import /tmp/certificate.p12 \
  -k build.keychain \
  -f pkcs12 \
  -T /usr/bin/codesign \
  -T /usr/bin/security \
  -P "$MACOS_CERTIFICATE_PASSWORD"

# 6. Register the new keychain in the user keychain list
security list-keychains -d user -s build.keychain login.keychain

# 7. CRITICAL: Allow codesign to access keychain items without interactive prompts
#    Without this step the build hangs indefinitely on headless CI.
security set-key-partition-list -S apple-tool:,apple: -k "$MACOS_KEYCHAIN_PASSWORD" build.keychain

# 8. Remove decoded p12 from disk
rm -f /tmp/certificate.p12
```

[VERIFIED: apple-actions/import-codesign-certs source (security.ts) — these exact commands confirmed]
[VERIFIED: multiple production CI examples (Federico Terzi, Localazy, rwwagner90 DEV.to article)]

**Why step 7 is critical:** Starting macOS 10.12.5, Apple introduced a new keychain ACL (partition
list) that requires applications to be on an access-control list before reading a keychain item
without prompting the user. If `set-key-partition-list` is omitted, `codesign` shows a GUI dialog
saying "codesign wants to access key X in your keychain" — the CI runner cannot answer this dialog
and the job hangs until the runner's job timeout kills it.
[VERIFIED: multiple bug reports (actions/runner#3407, tauri-apps/tauri-action#941, fastlane#15185)]

### Cleanup Step

```bash
# Remove temporary keychain at end of job (good hygiene; runner is ephemeral anyway)
security delete-keychain build.keychain
```

---

## jpackage Signing Flags

### macOS-Specific Flags (from jpackage man page)

| Flag | Value | Purpose |
|------|-------|---------|
| `--mac-sign` | (no value, flag only) | Enables code signing |
| `--mac-signing-key-user-name` | `"John Doe"` | Name portion of cert (not full "Developer ID Application: John Doe (TEAM)") |
| `--mac-entitlements` | `packaging/entitlements.plist` | Path to entitlements file |
| `--mac-package-identifier` | `ch.ti.gagi.xsleditor` | Unique bundle identifier (recommended for notarization readiness) |
| `--mac-signing-keychain` | `build.keychain` | Explicitly target the CI keychain (optional but safer in CI) |

[VERIFIED: jpackage man page — Java 21 docs.oracle.com]

### Updated jpackage Invocation (both macOS jobs)

```bash
jpackage \
  --type dmg \
  --name XSLEditor \
  --app-version "$VERSION_MACOS" \
  --input dist/ \
  --main-jar "xsleditor-${APP_VERSION}.jar" \
  --main-class ch.ti.gagi.xsleditor.Launcher \
  --icon src/main/resources/ch/ti/gagi/xsleditor/icon.icns \
  --dest output/ \
  --mac-sign \
  --mac-signing-key-user-name "$MACOS_SIGNING_IDENTITY" \
  --mac-entitlements packaging/entitlements.plist \
  --mac-package-identifier ch.ti.gagi.xsleditor \
  --mac-signing-keychain build.keychain
```

**Note:** `--mac-signing-keychain` uses the keychain name (not the full path). On macOS the
system resolves the keychain file from the registered list.
[ASSUMED — based on `security list-keychains` documentation and community examples; not explicitly
stated in jpackage man page]

---

## entitlements.plist Requirements for a JavaFX/JVM App

### Required Entitlements

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- Required: JVM uses JIT compilation; Hardened Runtime blocks JIT by default -->
    <key>com.apple.security.cs.allow-jit</key>
    <true/>

    <!-- Required: JVM loads native libraries (JavaFX, FOP, Saxon JNI) -->
    <!-- Also the critical workaround for JDK-8358723 / JDK-8369477 when the  -->
    <!-- main launcher would otherwise load libjli.dylib without this entitlement -->
    <key>com.apple.security.cs.disable-library-validation</key>
    <true/>

    <!-- Recommended: JVM may need to map executable memory for bytecode -->
    <key>com.apple.security.cs.allow-unsigned-executable-memory</key>
    <true/>
</dict>
</plist>
```

### Why These Entitlements

| Entitlement | Why Needed | Without It |
|-------------|-----------|------------|
| `cs.allow-jit` | The JVM JIT compiler creates writable+executable memory | App crashes at launch under Hardened Runtime |
| `cs.disable-library-validation` | JavaFX and FOP load native .dylib files not signed by Apple | Library load fails; also covers JDK-8358723 regression |
| `cs.allow-unsigned-executable-memory` | Java bytecode interpretation needs exec memory | May crash under strict Hardened Runtime |

[VERIFIED: Apple Developer Documentation for `cs.allow-jit` and `cs.disable-library-validation`]
[VERIFIED: AdoptOpenJDK blog post on notarizing Java applications]
[CITED: https://developer.apple.com/documentation/BundleResources/Entitlements/com.apple.security.cs.allow-jit]
[CITED: https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.security.cs.disable-library-validation]

### File Location in Repository

```
packaging/
└── entitlements.plist    # new file to commit in this phase
```

This path is referenced in jpackage as `packaging/entitlements.plist` (relative to repo root,
since jpackage runs from repo root in the CI job).

---

## Headless CI Signing — Full Pattern

The complete CI step set for one macOS job (arm64; x64 is identical):

```yaml
- name: Import signing certificate
  env:
    MACOS_CERTIFICATE: ${{ secrets.MACOS_CERTIFICATE }}
    MACOS_CERTIFICATE_PASSWORD: ${{ secrets.MACOS_CERTIFICATE_PASSWORD }}
    MACOS_KEYCHAIN_PASSWORD: ${{ secrets.MACOS_KEYCHAIN_PASSWORD }}
  run: |
    echo "$MACOS_CERTIFICATE" | base64 --decode > /tmp/certificate.p12
    security create-keychain -p "$MACOS_KEYCHAIN_PASSWORD" build.keychain
    security set-keychain-settings -lut 21600 build.keychain
    security unlock-keychain -p "$MACOS_KEYCHAIN_PASSWORD" build.keychain
    security import /tmp/certificate.p12 \
      -k build.keychain -f pkcs12 \
      -T /usr/bin/codesign -T /usr/bin/security \
      -P "$MACOS_CERTIFICATE_PASSWORD"
    security list-keychains -d user -s build.keychain login.keychain
    security set-key-partition-list -S apple-tool:,apple: \
      -k "$MACOS_KEYCHAIN_PASSWORD" build.keychain
    rm -f /tmp/certificate.p12

- name: jpackage DMG (arm64, signed)
  env:
    MACOS_SIGNING_IDENTITY: ${{ secrets.MACOS_SIGNING_IDENTITY }}
  run: |
    mkdir -p output
    jpackage \
      --type dmg \
      --name XSLEditor \
      --app-version "$VERSION_MACOS" \
      --input dist/ \
      --main-jar "xsleditor-${APP_VERSION}.jar" \
      --main-class ch.ti.gagi.xsleditor.Launcher \
      --icon src/main/resources/ch/ti/gagi/xsleditor/icon.icns \
      --dest output/ \
      --mac-sign \
      --mac-signing-key-user-name "$MACOS_SIGNING_IDENTITY" \
      --mac-entitlements packaging/entitlements.plist \
      --mac-package-identifier ch.ti.gagi.xsleditor \
      --mac-signing-keychain build.keychain
    mv "output/XSLEditor-${VERSION_MACOS}.dmg" \
       "output/XSLEditor-${APP_VERSION}-arm64.dmg"

- name: Verify code signature
  run: |
    codesign --verify --deep --strict --verbose=2 \
      "output/XSLEditor-${APP_VERSION}-arm64.dmg"
    echo "Codesign verification passed"
    codesign -dv --verbose=4 "output/XSLEditor-${APP_VERSION}-arm64.dmg" 2>&1 \
      | grep "TeamIdentifier"

- name: Delete keychain
  if: always()
  run: security delete-keychain build.keychain || true
```

[VERIFIED: command sequence matches apple-actions/import-codesign-certs source]
[VERIFIED: jpackage flags from official man page]
[VERIFIED: codesign verify flags from Apple documentation and community examples]

---

## codesign --verify Verification Approach

### Verification Commands

```bash
# Primary check: deep structural verification (checks all nested binaries and dylibs)
codesign --verify --deep --strict --verbose=2 path/to/XSLEditor.dmg

# Informational: show signer identity and team ID
codesign -dv --verbose=4 path/to/XSLEditor.dmg 2>&1 | grep -E "TeamIdentifier|Authority|Identifier"

# Gatekeeper acceptance check (verifies the cert is trusted by macOS, not just well-formed)
spctl -a -v --type install path/to/XSLEditor.dmg
```

### What Each Check Validates

| Command | What it Checks | Success indicator |
|---------|---------------|-------------------|
| `codesign --verify --deep --strict` | All nested binaries and libraries are properly signed; no broken chains | Exit code 0; no error output |
| `codesign -dv` | Displays signing identity, authority chain, team ID | Output contains `TeamIdentifier=<your-team-id>` |
| `spctl -a --type install` | Gatekeeper would accept the DMG (cert trusted by macOS trust store) | `accepted` in output |

Note: `spctl` on a CI runner may not reflect the same Gatekeeper state as an end-user machine
because runner VMs may not have up-to-date trust anchors. `codesign --verify` is the reliable CI
gate; `spctl` is informational only.
[ASSUMED — based on community reports that spctl behavior varies on cloud VMs; not officially documented]

---

## Key Risks and Landmines

### Pitfall 1: set-key-partition-list Omitted → Infinite Hang

**What goes wrong:** Without `security set-key-partition-list`, the `codesign` invoked by jpackage
shows a macOS dialog asking permission to access the keychain. The CI runner cannot dismiss this
dialog. The job runs until the runner's timeout (usually 6 hours for GitHub Actions) and then fails.

**How to avoid:** Always include `set-key-partition-list` after `security import`. It must use the
keychain password, not an empty string.

**Warning signs:** Job hangs at the jpackage step with no error output; eventually times out.

[VERIFIED: actions/runner#3407, tauri-apps/tauri-action#941, fastlane/fastlane#15185]

---

### Pitfall 2: Keychain Locking Mid-Job

**What goes wrong:** The default keychain lock timeout is 5 minutes on GitHub macOS runners. If
jpackage compilation takes longer than 5 minutes (possible on large apps), the keychain auto-locks
and signing fails mid-bundle.

**How to avoid:** `security set-keychain-settings -lut 21600` (6 hours). This must run after
`create-keychain` and before `unlock-keychain`.

**Warning signs:** Signing failure with error "errSecInteractionNotAllowed" or "keychain is locked"
appearing partway through jpackage output.

[VERIFIED: GitHub Actions issue macos-latest keychain locked #4519 in actions/runner-images]

---

### Pitfall 3: JDK-8358723 — Main Launcher Signed Without Entitlements

**What goes wrong:** In JDK 21+35 (and possibly up to the current 21.0.7 release), a regression
causes jpackage to sign the main launcher binary inside the `.app` bundle without applying
entitlements. This means `libjli.dylib` fails to load because `disable-library-validation` is not
present on the launcher that calls `dlopen`. The app launches but crashes immediately, or fails
`codesign --verify --strict` due to mismatched entitlements.

**Status:** The fix is tracked as JDK-8369477 (backport of JDK-8358723 to JDK 21). As of
Liberica 21.0.7 the fix is not confirmed in the release notes. This is a risk.

**Mitigation:** Including `--mac-entitlements packaging/entitlements.plist` with `cs.disable-library-validation`
is the correct defence. The bug is that jpackage sometimes ignores this on directory signing, but
the patch restores the intended behaviour. The workaround if the bug is still present: after
jpackage creates the `.app` bundle (before wrapping into DMG), manually re-sign the main launcher:
```bash
# Find the launcher (its name matches --name, without extension)
codesign --force --options runtime \
  --entitlements packaging/entitlements.plist \
  --sign "$MACOS_SIGNING_IDENTITY_FULL" \
  "output/XSLEditor.app/Contents/MacOS/XSLEditor"
```
where `MACOS_SIGNING_IDENTITY_FULL` is the full string
`"Developer ID Application: Name (TEAMID)"` — different from the `--mac-signing-key-user-name`
value which is just the name portion.

[CITED: https://bugs.openjdk.org/browse/JDK-8358723]
[CITED: https://github.com/adoptium/adoptium-support/issues/1251]

---

### Pitfall 4: Wrong --mac-signing-key-user-name Format

**What goes wrong:** Passing the full certificate string `"Developer ID Application: Acme Corp (ABC123)"` to
`--mac-signing-key-user-name` instead of just `"Acme Corp"`. jpackage searches the keychain for
certificates containing the supplied string as a substring match on the common name. The full string
format causes "no identity found" errors.

**How to avoid:** Store only the name portion (e.g., `"Acme Corp"`) in the `MACOS_SIGNING_IDENTITY`
secret.

**Warning signs:** jpackage error "Failed to find signing identity" or "no valid identity found".

[VERIFIED: Apple Developer Forums thread 718795 — confirmed name-only format]

---

### Pitfall 5: --mac-signing-keychain Path vs Name

**What goes wrong:** Passing a full path like `/tmp/build.keychain` to `--mac-signing-keychain`
when the keychain was registered with `security list-keychains` using only the name `build.keychain`.

**How to avoid:** Use just the name `build.keychain`. macOS resolves the keychain file from the
user's search list.
[ASSUMED — based on macOS keychain resolution documentation; not explicitly tested]

---

### Pitfall 6: Secrets Not Available in if:always() Cleanup Step

**What goes wrong:** The cleanup step `security delete-keychain build.keychain` must run even if
earlier steps fail. Using `if: always()` ensures this, but it must not reference secrets
(env vars are available in `if: always()` steps).

**How to avoid:** The delete-keychain step does not need secrets — it uses only the keychain name.
Add `|| true` to suppress errors if the keychain was never created.

[ASSUMED — standard GitHub Actions behaviour; env vars are available in all steps including `if: always()`]

---

### Pitfall 7: Two-Step Signing Interaction (app-image + DMG)

**What goes wrong:** The Windows job uses a two-step `app-image` → `msi` pattern. If a similar
two-step is attempted on macOS (app-image → signed app-image → dmg), the signing must happen on
the intermediate `.app` directory, not the final DMG. jpackage's `--mac-sign` flag on `--type dmg`
signs the embedded `.app` and produces the DMG. The current Phase 20 pattern uses direct
`--type dmg` (no intermediate app-image step on macOS) so this pitfall does not apply.

**Recommendation:** Keep the direct `--type dmg` path for macOS with `--mac-sign`. Do not introduce
a two-step app-image approach unless explicitly needed for post-processing.

[VERIFIED: jpackage man page — `--type dmg` with `--mac-sign` is a documented single-step path]

---

## GitHub Actions Secrets Pattern

### Storing the Certificate

```bash
# On developer machine:
# 1. Export "Developer ID Application: Name (TEAM)" from Keychain Access as .p12
# 2. Base64-encode it:
base64 -i /path/to/certificate.p12 | pbcopy
# 3. Paste into GitHub → Settings → Secrets → MACOS_CERTIFICATE

# To verify the base64 decodes correctly:
echo "$MACOS_CERTIFICATE" | base64 --decode > /tmp/test.p12
security import /tmp/test.p12 -P "$MACOS_CERTIFICATE_PASSWORD" -T /usr/bin/codesign
security find-identity -v -p codesigning
```

### Secrets Block in release.yml

```yaml
env:
  MACOS_CERTIFICATE: ${{ secrets.MACOS_CERTIFICATE }}
  MACOS_CERTIFICATE_PASSWORD: ${{ secrets.MACOS_CERTIFICATE_PASSWORD }}
  MACOS_KEYCHAIN_PASSWORD: ${{ secrets.MACOS_KEYCHAIN_PASSWORD }}
  MACOS_SIGNING_IDENTITY: ${{ secrets.MACOS_SIGNING_IDENTITY }}
```

### Conditional Execution: Sign Only When Secrets Are Present

Since Phase 21 secrets may not be set by all contributors, the signing steps can be gated:

```yaml
- name: Import signing certificate
  if: ${{ secrets.MACOS_CERTIFICATE != '' }}
  ...
```

However, for the Phase 21 CI gate, the workflow should fail (not silently skip) when secrets are
absent — so conditional skipping is optional and should be a deliberate choice.
[ASSUMED — design decision; either approach is valid]

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Keychain setup | Custom Swift/Python keychain management | `security` CLI (pre-installed) | Platform tool handles all edge cases |
| Certificate import | Direct OpenSSL PKCS12 parsing | `security import` | Native macOS trust chain integration |
| Partition list configuration | Custom ACL editing | `security set-key-partition-list` | The only supported way to grant headless codesign access |
| App bundle signing | Manual `codesign` invocation on each file | `jpackage --mac-sign` | jpackage handles nested bundles, frameworks, and dylibs in the correct order |
| Signing verification | Manual trust chain inspection | `codesign --verify --deep --strict` | Apple-maintained verification tool; mirrors Gatekeeper checks |

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| `codesign` | Signing and verification | Yes | Xcode CLT 16.4 | None — macOS-only tool |
| `security` | Keychain management | Yes | macOS system | None — macOS-only tool |
| `jpackage` | DMG creation and signing | Yes (via setup-java) | JDK 21 | None — required |
| `base64` | Certificate decoding | Yes | macOS system | `openssl base64 -d` |
| Apple Developer ID certificate | Signing identity | Not on runner | — | None — must be in GitHub Secrets |
| Apple Developer Program membership | Certificate issuance | Assumed active [ASSUMED] | — | None — paid prerequisite |

[VERIFIED: macos-15-arm64-Readme.md — Xcode CLT 16.4 confirmed pre-installed]
[ASSUMED: Apple Developer Program membership is active; the project REQUIREMENTS.md and STATE.md indicate signing is planned]

---

## Validation Architecture

> `nyquist_validation: true` in `.planning/config.json`

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Shell commands (`codesign`, `security`) — no unit test framework applicable |
| Quick run command | `codesign --verify --deep --strict output/XSLEditor-*.dmg` |
| Full verification | `codesign --verify --deep --strict` + `codesign -dv` team ID check |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | Notes |
|--------|----------|-----------|-------------------|-------|
| MACOS-03 | `codesign --verify --deep --strict` passes on arm64 DMG | smoke | `codesign --verify --deep --strict output/XSLEditor-*-arm64.dmg` | Run in `package-macos-arm` job post-jpackage |
| MACOS-03 | `codesign --verify --deep --strict` passes on x64 DMG | smoke | `codesign --verify --deep --strict output/XSLEditor-*-x64.dmg` | Run in `package-macos-x64` job post-jpackage |
| MACOS-03 | CI signing sequence runs without interactive prompts | integration | Workflow completes without hanging | Validated by job completing in < 30 min |
| MACOS-03 | `entitlements.plist` committed and referenced | static | `ls packaging/entitlements.plist && grep "allow-jit" packaging/entitlements.plist` | Wave 0 |
| MACOS-03 | 7 secrets documented | documentation | N/A — human verification | Phase 23 (SIGN-01) |

### Wave 0 Gaps

- [ ] `packaging/entitlements.plist` — new file; covers MACOS-03 entitlements requirement
- [ ] `release.yml` updated — signing steps added to both macOS jobs

*(No new test files needed — verification is via codesign commands embedded in CI jobs)*

---

## Security Domain

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | N/A — no user auth |
| V3 Session Management | No | N/A |
| V4 Access Control | Yes | Secrets scoped to repo; `contents: write` permission already set on release job |
| V5 Input Validation | Yes | `MACOS_SIGNING_IDENTITY` injected as env var; no shell injection risk since it's a name string |
| V6 Cryptography | Yes | Private key stored as GitHub Secret (encrypted at rest by GitHub); never written to disk as plaintext (decoded only to `/tmp/certificate.p12`, deleted after import) |

**Secret hygiene:**
- The decoded `.p12` file (`/tmp/certificate.p12`) must be deleted immediately after `security import`
- Do not log secret values (`echo "$MACOS_CERTIFICATE"` without redirection is acceptable because GitHub Actions automatically masks secrets in logs)
- The MACOS_KEYCHAIN_PASSWORD is ephemeral — it only needs to be random, not guessable

---

## State of the Art

| Old Approach | Current Approach | Notes |
|--------------|------------------|-------|
| Manual `codesign` invocations on every file in the bundle | `jpackage --mac-sign` (handles all nested signing) | jpackage 14+ handles signing order correctly |
| devbotsxyz/import-signing-certificate GitHub Action | Manual `security` CLI steps or apple-actions/import-codesign-certs | Direct shell is more transparent |
| BellSoft `javapackager` (pre-jpackage) | `jpackage` (JDK 14+) | jpackage is the current standard |
| Separate app-image signing then DMG wrapping | Direct `jpackage --type dmg --mac-sign` | Simpler; fewer steps; no intermediate artifact |

---

## Open Questions

1. **JDK-8369477 backport status in Liberica 21.0.7**
   - What we know: The fix for the main launcher missing entitlements (JDK-8358723) is tracked as JDK-8369477 backport to JDK 21. Liberica 21.0.7 release notes do not mention it.
   - What's unclear: Whether the fix is present in the Liberica build that `actions/setup-java` installs.
   - Recommendation: Include `packaging/entitlements.plist` with all three entitlements (including `cs.disable-library-validation`) as a defence. If signing succeeds but the app crashes at runtime, apply the manual re-sign workaround documented above.

2. **`--mac-signing-keychain` flag name vs. `--mac-signing-keychain`**
   - What we know: The jpackage man page lists `--mac-signing-keychain _keychain-name_` for specifying the keychain.
   - What's unclear: Whether specifying this is required when the build keychain is registered via `security list-keychains -d user -s build.keychain login.keychain`.
   - Recommendation: Include `--mac-signing-keychain build.keychain` explicitly in the jpackage invocation as a safety measure.

3. **Signing secrets availability**
   - What we know: Phase 21 requires the developer to have an Apple Developer Program membership and a Developer ID Application certificate.
   - What's unclear: Whether the certificate is already set up on the developer's Apple Developer account.
   - Recommendation: The planner must include a human checkpoint ("secrets configured in GitHub") before the workflow test tag push.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `--mac-signing-keychain` accepts the keychain name (not full path) after `list-keychains -d user -s` registration | jpackage flags section | Signing fails with "keychain not found"; fix: use full path `~/Library/Keychains/build.keychain-db` |
| A2 | GitHub Actions env vars are available in `if: always()` cleanup steps | Pitfall 6 | Cleanup step may fail silently; runner still ephemeral so no security risk |
| A3 | Conditional signing (skip when secrets empty) is an optional design choice | Secrets Pattern section | If chosen and MACOS_CERTIFICATE is unset, unsigned DMG passes silently; planner must decide |
| A4 | Apple Developer Program membership is active for this project | Environment Availability | Phase 21–22 cannot proceed without it; $99/yr prerequisite |
| A5 | `spctl -a --type install` on CI runner reflects real Gatekeeper trust | Verification section | spctl false-negative on CI; use `codesign --verify` as the CI gate instead |

---

## Sources

### Primary (HIGH confidence)
- [jpackage man page — Java 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html) — all `--mac-*` flags and their descriptions
- [apple-actions/import-codesign-certs source (security.ts)](https://github.com/Apple-Actions/import-codesign-certs/blob/main/src/security.ts) — exact security command sequence verified
- [Apple Developer Documentation: cs.allow-jit](https://developer.apple.com/documentation/BundleResources/Entitlements/com.apple.security.cs.allow-jit) — entitlement purpose and value
- [Apple Developer Documentation: cs.disable-library-validation](https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.security.cs.disable-library-validation) — entitlement purpose and value
- [GitHub runner-images macos-15-arm64-Readme.md](https://github.com/actions/runner-images/blob/main/images/macos/macos-15-arm64-Readme.md) — Xcode CLT 16.4 confirmed pre-installed
- [Apple Developer Forums thread 718795 — mac-signing-key-user-name format](https://developer.apple.com/forums/thread/718795) — name-only format confirmed

### Secondary (MEDIUM confidence)
- [Federico Terzi blog — Automatic Code-signing with GitHub Actions](https://federicoterzi.com/blog/automatic-code-signing-and-notarization-for-macos-apps-using-github-actions/) — full keychain command sequence
- [Localazy — How to automatically sign macOS apps with GitHub Actions](https://localazy.com/blog/how-to-automatically-sign-macos-apps-using-github-actions) — CI codesign workflow
- [DEV.to rwwagner90 — Signing Electron Apps with GitHub Actions](https://dev.to/rwwagner90/signing-electron-apps-with-github-actions-4cof) — complete shell script for keychain setup
- [AdoptOpenJDK blog — A Simple Guide to Notarizing Your Java Application](https://blog.adoptopenjdk.net/2020/05/a-simple-guide-to-notarizing-your-java-application/) — entitlements.plist content for Java apps
- [OpenJDK mail — JDK-8358723 integrated](https://www.mail-archive.com/core-libs-dev@openjdk.org/msg60749.html) — regression and fix description
- [adoptium-support #1251 — macOS JPackage Runtime exec crash](https://github.com/adoptium/adoptium-support/issues/1251) — JDK-8358723 real-world impact

### Tertiary (LOW confidence — flagged with [ASSUMED])
- Community reports on `spctl` behavior on cloud runners (multiple forum threads)
- `--mac-signing-keychain` name vs. path resolution behavior
- JDK-8369477 backport inclusion in Liberica 21.0.7

---

## Metadata

**Confidence breakdown:**
- Core signing mechanics (keychain, security commands): HIGH — verified against apple-actions source and multiple production examples
- jpackage `--mac-sign` flags: HIGH — from official jpackage man page
- entitlements.plist content: HIGH — from Apple Developer Documentation and AdoptOpenJDK guide
- JDK-8358723 workaround: MEDIUM — bug confirmed; fix status in Liberica 21.0.7 unconfirmed
- Keychain name resolution in `--mac-signing-keychain`: MEDIUM/LOW — assumed; planner should treat as risk

**Research date:** 2026-04-25
**Valid until:** 2026-07-25 (runner images and JDK patch levels update; re-verify Liberica 21.x includes JDK-8369477 fix)
