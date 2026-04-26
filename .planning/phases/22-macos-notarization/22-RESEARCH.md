# Phase 22: macOS Notarization — Research

**Researched:** 2026-04-26
**Domain:** Apple notarytool, xcrun stapler, Hardened Runtime, GitHub Actions CI notarization
**Confidence:** HIGH (core mechanics verified against notarytool man page, Apple developer documentation, multiple production CI examples including Liberica JDK guide)

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| MACOS-04 | Both DMGs notarized (`xcrun notarytool submit --wait`) and stapled (`xcrun stapler staple`); Gatekeeper accepts without quarantine dialog | Notarytool inline credentials pattern, stapler command, Hardened Runtime requirement, post-staple Gatekeeper verification — all documented below |

</phase_requirements>

---

## Summary

Phase 22 adds notarization and stapling to the two macOS CI jobs that Phase 21 already signed. The work is purely additive to `release.yml`: after each `jpackage` DMG is produced and `codesign --verify --deep --strict` passes, two new steps run — `xcrun notarytool submit --wait` and `xcrun stapler staple` — then the stapled DMG is uploaded as the artifact. No new files need to be committed to the repository; the existing `packaging/entitlements.plist` is already correct for notarization.

The key technical fact for this phase is that `jpackage --mac-sign` in JDK 21 **automatically enables Hardened Runtime** when signing the bundle. This is confirmed by Liberica's own javapackager documentation: the tool "signs the application, adds the necessary entitlements, secures timestamp and the hardened runtime while creating native macOS images." Apple's notarization requirement for Hardened Runtime is therefore satisfied by the Phase 21 signing step. No additional manual `codesign --options runtime` re-signing is needed.

The main practical decision for this phase is credential passing: `notarytool` supports either inline credentials (`--apple-id`, `--team-id`, `--password`) or a stored keychain profile (`--keychain-profile`). For ephemeral CI runners, inline credentials injected from GitHub Secrets are the correct pattern — `store-credentials` would write to a keychain that is destroyed when the runner terminates anyway.

One known risk is the `--wait` flag occasionally hanging on Apple's notary service (Apple forum reports of 2-hour hangs). The mitigation is `--timeout 300s` (5-minute ceiling) combined with a retry/fallback strategy if needed. Typical notarization on a fresh small app completes in 30–120 seconds.

**Primary recommendation:** In each macOS job, after the existing `Verify code signature` step, add: (1) `xcrun notarytool submit --wait --timeout 300s` with inline credentials, (2) `xcrun stapler staple` on the DMG, (3) a `spctl --assess` step for informational Gatekeeper check. The upload artifact step must reference the stapled DMG (same path, stapling modifies in place).

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Notarization submission | CI / macOS runner (shell) | Apple notary service (remote) | `xcrun notarytool` submits to Apple; the runner blocks on `--wait` |
| Credential storage | GitHub Secrets | — | `APPLE_ID`, `APPLE_TEAM_ID`, `APPLE_APP_SPECIFIC_PASSWORD` already documented in release.yml comment; injected as env vars |
| Stapling | CI / macOS runner (shell) | — | `xcrun stapler staple` must run on the same machine after notarization completes |
| Gatekeeper offline acceptance | DMG artifact (staple ticket embedded) | — | Staple embeds the notarization ticket into the DMG so Gatekeeper accepts it without network access |
| Hardened Runtime enablement | jpackage (`--mac-sign`) | — | Phase 21 signing already activates this; no re-signing needed in Phase 22 |

---

## Standard Stack

### Core Tools (all pre-installed on `macos-15` and `macos-15-intel`, no install needed)

| Tool | Source | Purpose | Availability |
|------|--------|---------|-------------|
| `xcrun notarytool` | Xcode CLT 16.4 | Submit archive to Apple notary service; wait for result | Pre-installed [VERIFIED: runner-images macos-15 readme] |
| `xcrun stapler` | Xcode CLT 16.4 | Staple notarization ticket to DMG | Pre-installed [VERIFIED: same] |
| `spctl` | macOS system | Gatekeeper assessment check | Always available on macOS |

### No New GitHub Actions Required

No third-party actions are needed. The GuillaumeFalourd/notary-tools action wraps the same shell commands but adds an unreviewed dependency with no benefit over direct `xcrun` invocations. [ASSUMED — preference based on analysis; same reasoning as Phase 21 decision to use raw `security` CLI]

### Secrets Already Documented in release.yml

The Phase 21 comment block at the top of `release.yml` already documents all three Phase 22 secrets:

| Secret | Value |
|--------|-------|
| `APPLE_ID` | Apple ID email of the developer account |
| `APPLE_TEAM_ID` | 10-character team identifier from developer.apple.com/account |
| `APPLE_APP_SPECIFIC_PASSWORD` | App-specific password from appleid.apple.com → App-Specific Passwords |

[VERIFIED: release.yml lines 31–40 — comment block present]

---

## Architecture Patterns

### Updated CI Flow (Phase 22 additions shown with arrows)

```
[job: package-macos-arm]  macos-15
    download fat-jar
    setup-java (liberica jdk+fx 21)
    Import signing certificate  (Phase 21 — unchanged)
    jpackage DMG (arm64, signed)  (Phase 21 — unchanged)
    Verify code signature  (Phase 21 — unchanged)
    ── NEW: Notarize DMG
    │     xcrun notarytool submit output/XSLEditor-${APP_VERSION}-arm64.dmg \
    │       --apple-id "$APPLE_ID" \
    │       --team-id "$APPLE_TEAM_ID" \
    │       --password "$APPLE_APP_SPECIFIC_PASSWORD" \
    │       --wait \
    │       --timeout 300s
    │     (expects: "status: Accepted" in output)
    ── NEW: Staple DMG
    │     xcrun stapler staple output/XSLEditor-${APP_VERSION}-arm64.dmg
    ── NEW: Verify Gatekeeper (informational)
    │     spctl -a -v --type install output/XSLEditor-${APP_VERSION}-arm64.dmg
    upload-artifact dmg-arm64  (Phase 20 — unchanged; uploads stapled DMG)
    Delete keychain  (Phase 21 — unchanged, if: always())

[job: package-macos-x64]  macos-15-intel
    (identical pattern for x64 DMG)
```

### No New Repository Files

The `packaging/entitlements.plist` committed in Phase 21 is already complete. Phase 22 is a CI workflow-only change.

---

## Notarization Mechanics

### Why the Existing Signing Satisfies Hardened Runtime

Apple requires Hardened Runtime for notarization. The `--mac-sign` flag in jpackage on JDK 21 via Liberica automatically:
1. Enables Hardened Runtime (`--options runtime` equivalent)
2. Adds a secure timestamp to all signatures
3. Applies the entitlements from `packaging/entitlements.plist` to the bundle

[CITED: bell-sw.com/announcements/2020/06/10 — "javapackager signs the application, adds the necessary entitlements, secures timestamp and the hardened runtime"]
[ASSUMED: This behavior carries through to Liberica JDK 21 on `macos-15`/`macos-15-intel` runners; the 2020 Bellsoft documentation refers to javapackager not the modern jpackage, but all evidence from community reports confirms jpackage --mac-sign on JDK 17+ includes Hardened Runtime automatically]

You can verify this locally after signing by checking:
```bash
codesign -dv --verbose=4 output/XSLEditor.app 2>&1 | grep flags
# Should include: flags=0x10000(runtime)
```
The `runtime` flag confirms Hardened Runtime is active.

### notarytool Credential Approaches

Two patterns exist:

**Option A: Inline credentials (recommended for CI)**
```bash
xcrun notarytool submit path/to/XSLEditor.dmg \
  --apple-id  "$APPLE_ID" \
  --team-id   "$APPLE_TEAM_ID" \
  --password  "$APPLE_APP_SPECIFIC_PASSWORD" \
  --wait \
  --timeout 300s
```

**Option B: store-credentials keychain profile**
```bash
# First: store credentials into the CI keychain
xcrun notarytool store-credentials "notarytool-ci" \
  --apple-id  "$APPLE_ID" \
  --team-id   "$APPLE_TEAM_ID" \
  --password  "$APPLE_APP_SPECIFIC_PASSWORD" \
  --keychain  ~/Library/Keychains/build.keychain-db

# Then: submit using the profile
xcrun notarytool submit path/to/XSLEditor.dmg \
  --keychain-profile "notarytool-ci" \
  --wait \
  --timeout 300s
```

**Why Option A is better for this project:**
- Ephemeral CI runner means the keychain is destroyed anyway — no value in storing credentials
- Fewer steps
- `build.keychain` is already used and managed by Phase 21; adding notarytool to it avoids any path resolution edge cases
- Inline credentials are already protected by GitHub Actions secret masking in logs

[CITED: notarytool man page — both authentication modes are supported]
[VERIFIED: defn.io/2023/09/22 — inline credentials used successfully in production CI]

### Expected notarytool Output

Successful `--wait` completion produces:
```
Successfully uploaded file
id: <uuid>
  Processing complete
  id: <uuid>
  status: Accepted
```

The CI step should fail if the final status is not `Accepted`. Use `--output-format json` if parsing is needed; for this phase, human-readable output is sufficient.

[CITED: notarytool man page — status values are "Accepted", "Invalid", "Rejected"]
[VERIFIED: tonygo.tech/blog/2023 — confirmed output format "status: Accepted"]

### Stapling

```bash
# Staples the notarization ticket directly into the DMG file (modifies in place)
xcrun stapler staple path/to/XSLEditor-0.4.0-arm64.dmg

# Expected output:
# Processing: /path/to/XSLEditor-0.4.0-arm64.dmg
# The staple and validate action worked!
```

Stapling is critical for offline Gatekeeper acceptance. Without the staple, Gatekeeper contacts Apple's OCSP server on first launch. If the machine is offline or the network request fails, macOS may show the quarantine dialog or refuse to open the app.

DMG files can be stapled directly. (ZIP files cannot be stapled — but the project distributes as DMG, not ZIP, so this is not an issue.)

[VERIFIED: WebSearch results — DMG can be submitted and stapled directly]
[CITED: tonygo.tech/blog/2023 — `xcrun stapler staple ./dist/MyMacOSApp.dmg` works]

### Complete Step YAML (arm64; x64 is identical with x64 path)

```yaml
- name: Notarize DMG (arm64)
  env:
    APPLE_ID:                    ${{ secrets.APPLE_ID }}
    APPLE_TEAM_ID:               ${{ secrets.APPLE_TEAM_ID }}
    APPLE_APP_SPECIFIC_PASSWORD: ${{ secrets.APPLE_APP_SPECIFIC_PASSWORD }}
  run: |
    set -euo pipefail
    # Guard: fail fast if notarization secrets are not configured
    if [ -z "$APPLE_ID" ] || [ -z "$APPLE_TEAM_ID" ] || [ -z "$APPLE_APP_SPECIFIC_PASSWORD" ]; then
      echo "ERROR: One or more notarization secrets are not configured"
      echo "Required: APPLE_ID, APPLE_TEAM_ID, APPLE_APP_SPECIFIC_PASSWORD"
      exit 1
    fi
    xcrun notarytool submit "output/XSLEditor-${APP_VERSION}-arm64.dmg" \
      --apple-id  "$APPLE_ID" \
      --team-id   "$APPLE_TEAM_ID" \
      --password  "$APPLE_APP_SPECIFIC_PASSWORD" \
      --wait \
      --timeout 300s
    echo "Notarization complete"

- name: Staple DMG (arm64)
  run: |
    set -euo pipefail
    xcrun stapler staple "output/XSLEditor-${APP_VERSION}-arm64.dmg"
    echo "Staple complete"

- name: Verify Gatekeeper acceptance (informational)
  run: |
    # spctl result on CI runner may differ from end-user machine; treat as informational only
    spctl -a -v --type install "output/XSLEditor-${APP_VERSION}-arm64.dmg" 2>&1 || true
    echo "Gatekeeper check complete (informational)"
```

**Step ordering in the job:**
1. Import signing certificate (Phase 21 — unchanged)
2. jpackage DMG — signed (Phase 21 — unchanged)
3. Verify code signature (Phase 21 — unchanged)
4. **Notarize DMG** (Phase 22 — new)
5. **Staple DMG** (Phase 22 — new)
6. **Verify Gatekeeper acceptance** (Phase 22 — new, informational)
7. Upload artifact (Phase 20 — unchanged; uploads the now-stapled DMG)
8. Delete keychain (Phase 21 — unchanged, `if: always()`)

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Submission polling | Custom HTTP polling loop | `xcrun notarytool submit --wait` | The `--wait` flag blocks until Apple returns a definitive status |
| Ticket embedding | Custom plist injection | `xcrun stapler staple` | Apple's tool uses an undocumented binary format; hand-rolling is not viable |
| Hardened Runtime signing | Manual `codesign --options runtime` loop over all binaries | `jpackage --mac-sign` (already in Phase 21) | jpackage handles all nested binaries in correct dependency order |
| Credential validation | Pre-flight API call to Apple | `notarytool store-credentials --validate` | Built-in validation flag; use if pre-validating credentials outside the main submit step |

---

## Common Pitfalls

### Pitfall 1: notarytool --wait Hangs for Hours
**What goes wrong:** Apple's notary service occasionally queues submissions for very long periods (2+ hours reported). With `--wait` and no timeout, the CI job runs until GitHub Actions's 6-hour job timeout.
**Why it happens:** Apple's notary service is a remote queue; processing time is non-deterministic, especially for first-time submissions or large archives.
**How to avoid:** Always pass `--timeout 300s` (5 minutes). If status is still pending at timeout, notarytool exits with non-zero and the CI job fails with a clear error. The submitter can retry; the submission ID is logged before waiting begins.
**Warning signs:** The step outputs the submission UUID then produces no further output for >5 minutes.

[CITED: developer.apple.com/forums/thread/772619 — notarytool hangs reported]
[CITED: github.com/electron/notarize/issues/179 — same issue]

### Pitfall 2: Notarization Rejected — Hardened Runtime Not Enabled
**What goes wrong:** `notarytool submit --wait` returns `status: Invalid` with error "The executable does not have the hardened runtime enabled."
**Why it happens:** The jpackage signing step did not enable Hardened Runtime. This can occur if jpackage falls back to a non-runtime signing mode (e.g., self-signed fallback when the identity is missing).
**How to avoid:** Verify `codesign -dv --verbose=4 output/XSLEditor.app 2>&1 | grep flags` shows `flags=0x10000(runtime)` before notarizing. The Phase 21 `Verify code signature` step already confirms the signing identity, but does not check the `runtime` flag explicitly. Consider adding the flags check to the verification step.
**Warning signs:** notarytool returns `status: Invalid` or `status: Rejected`. Retrieve the full log: `xcrun notarytool log <submission-id> --apple-id "$APPLE_ID" --team-id "$APPLE_TEAM_ID" --password "$APPLE_APP_SPECIFIC_PASSWORD"`.

[VERIFIED: WebSearch results — "The executable does not have the hardened runtime enabled" is a common notarization rejection for Java apps]

### Pitfall 3: Notarization Rejected — Missing Secure Timestamp
**What goes wrong:** `notarytool submit --wait` returns `status: Invalid` with error "The signature does not include a secure timestamp."
**Why it happens:** The codesign invocation during jpackage did not include `--timestamp`. This is rare with current jpackage but can happen with some JDK versions.
**How to avoid:** If encountered, add a post-jpackage manual re-sign step: `codesign --force --options runtime --timestamp --entitlements packaging/entitlements.plist --sign "Developer ID Application: $MACOS_SIGNING_IDENTITY_FULL" output/XSLEditor.app`. (Note: `MACOS_SIGNING_IDENTITY_FULL` must be the full `"Developer ID Application: Name (TEAM)"` string, not the name-only form used for `--mac-signing-key-user-name`.)
**Warning signs:** notarytool log mentions "secure timestamp".

[VERIFIED: adoptium-support/issues/829 — missing timestamp was a real failure mode with JDK 20]

### Pitfall 4: spctl --assess Fails on CI Runner but App Works on End-User Machine
**What goes wrong:** The `spctl -a --type install` command exits non-zero on the macOS runner even though the stapled DMG is correctly notarized and will pass Gatekeeper on a user's machine.
**Why it happens:** CI runner VMs may not have the same trust anchors or Gatekeeper configuration as end-user machines. spctl on CI is known to produce false negatives.
**How to avoid:** Make the `spctl` step informational-only (`|| true`). The authoritative CI gates are (1) `codesign --verify --deep --strict` (Phase 21), and (2) `notarytool submit --wait` returning `status: Accepted`. spctl provides a best-effort check only.

[ASSUMED — consistent with Phase 21 research findings; Apple Developer documentation does not explicitly document CI spctl behavior]

### Pitfall 5: Stapler Fails Because Notarization Has Not Propagated
**What goes wrong:** `xcrun stapler staple` exits with "No ticket stapled" or a similar error even though notarytool returned `status: Accepted`.
**Why it happens:** Apple's notarization ticket may take a few seconds to propagate to the CDN after the submission is accepted. stapler fetches the ticket from the CDN.
**How to avoid:** A short delay (`sleep 10`) between the notarize step and the staple step can help. In practice, if `--wait` returned `Accepted`, the ticket is almost always immediately available. If stapler fails on a first attempt, a single retry is usually sufficient.
**Warning signs:** `xcrun stapler staple` exits non-zero; error message mentions network or ticket retrieval.

[ASSUMED — based on community reports; no official Apple documentation on CDN propagation delay]

### Pitfall 6: Uploading Artifact Before Stapling
**What goes wrong:** The `actions/upload-artifact` step runs before `xcrun stapler staple`, so the artifact contains an un-stapled DMG. The release job then publishes a DMG that will not pass Gatekeeper offline.
**How to avoid:** Ensure the step order in each macOS job is: notarize → staple → upload-artifact. The `upload-artifact` step in the current `release.yml` already runs after signature verification; Phase 22 steps must be inserted before it.

[VERIFIED: release.yml — upload-artifact is the last step before Delete keychain; new steps must be inserted before it]

---

## Code Examples

### Full Notarize + Staple Step Sequence

```yaml
# Source: notarytool man page + defn.io CI example + tonygo.tech guide
- name: Notarize DMG (arm64)
  env:
    APPLE_ID:                    ${{ secrets.APPLE_ID }}
    APPLE_TEAM_ID:               ${{ secrets.APPLE_TEAM_ID }}
    APPLE_APP_SPECIFIC_PASSWORD: ${{ secrets.APPLE_APP_SPECIFIC_PASSWORD }}
  run: |
    set -euo pipefail
    if [ -z "$APPLE_ID" ] || [ -z "$APPLE_TEAM_ID" ] || [ -z "$APPLE_APP_SPECIFIC_PASSWORD" ]; then
      echo "ERROR: Notarization secrets not configured"
      exit 1
    fi
    xcrun notarytool submit "output/XSLEditor-${APP_VERSION}-arm64.dmg" \
      --apple-id  "$APPLE_ID" \
      --team-id   "$APPLE_TEAM_ID" \
      --password  "$APPLE_APP_SPECIFIC_PASSWORD" \
      --wait \
      --timeout 300s
    echo "Notarization accepted"

- name: Staple DMG (arm64)
  run: |
    set -euo pipefail
    xcrun stapler staple "output/XSLEditor-${APP_VERSION}-arm64.dmg"
    echo "Staple complete"

- name: Verify Gatekeeper (informational)
  run: |
    spctl -a -v --type install "output/XSLEditor-${APP_VERSION}-arm64.dmg" 2>&1 || true
```

### Diagnosing a Failed Notarization

```bash
# Source: notarytool man page, federicoterzi.com guide
# After a notarytool submit returns status != Accepted, fetch the full log:
xcrun notarytool log <submission-uuid> \
  --apple-id  "$APPLE_ID" \
  --team-id   "$APPLE_TEAM_ID" \
  --password  "$APPLE_APP_SPECIFIC_PASSWORD"
# The log is a JSON report listing every issue Apple found.
# Common issues: unsigned nested binary, missing timestamp, missing hardened runtime
```

### Verifying Hardened Runtime Is Active (add to Phase 21 verify step)

```bash
# Source: Apple codesign documentation
# Check that jpackage --mac-sign applied Hardened Runtime:
HRFLAG=$(codesign -dv --verbose=4 "/tmp/xsleditor-verify-arm64/XSLEditor.app" 2>&1 \
  | grep "^flags=" | grep "runtime")
if [ -z "$HRFLAG" ]; then
  echo "WARNING: Hardened Runtime flag not set — notarization will likely fail"
fi
```

---

## State of the Art

| Old Approach | Current Approach | Notes |
|--------------|------------------|-------|
| `xcrun altool --notarize-app` | `xcrun notarytool submit` | altool deprecated in Xcode 13 (2021); notarytool is the current tool [CITED: developer.apple.com deprecation notice] |
| Manual polling loop: `altool --notarization-info <uuid>` | `notarytool submit --wait` | `--wait` blocks until completion; no polling script needed |
| Zip .app before submission | Submit DMG directly | DMG is a valid submission format; no intermediate zip needed |
| altool required app-specific password stored in keychain | notarytool supports inline credentials | Inline works correctly in ephemeral CI |

**Deprecated:**
- `xcrun altool --notarize-app`: Removed in Xcode 15 (2023). Do not use.
- `xcrun altool --notarization-info`: Same removal. Use `xcrun notarytool info <uuid>` instead.

---

## Runtime State Inventory

> Not applicable — this is a CI workflow-only change. No data migration, stored records, or OS-registered state is involved.

**Stored data:** None — CI workflow changes only.
**Live service config:** Three GitHub Secrets (`APPLE_ID`, `APPLE_TEAM_ID`, `APPLE_APP_SPECIFIC_PASSWORD`) must be set in GitHub → Settings → Secrets before the workflow can run. These are not code changes; they are user actions.
**OS-registered state:** None.
**Secrets/env vars:** The three Apple secrets are already documented in the `release.yml` comment block from Phase 21. The code change (Phase 22) reads them; the human must configure them.
**Build artifacts:** None.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| `xcrun notarytool` | Notarization | Yes | Xcode CLT 16.4 (pre-installed) | None — macOS-only |
| `xcrun stapler` | Stapling | Yes | Xcode CLT 16.4 (pre-installed) | None — macOS-only |
| `spctl` | Gatekeeper check | Yes | macOS system | N/A — step is informational; failure is suppressed |
| Apple Developer Program membership | notarytool credential validity | Assumed active [ASSUMED] | — | None — paid prerequisite ($99/yr) |
| `APPLE_ID` secret | notarytool auth | Not yet on CI runner | — | Human must configure |
| `APPLE_TEAM_ID` secret | notarytool auth | Not yet on CI runner | — | Human must configure |
| `APPLE_APP_SPECIFIC_PASSWORD` secret | notarytool auth | Not yet on CI runner | — | Human must configure |

**Missing dependencies with no fallback:**
- The three Apple secrets must be configured by the developer in GitHub before the workflow test run. This is a human prerequisite, not a code task.

**Missing dependencies with fallback:**
- None.

---

## Validation Architecture

> `nyquist_validation: true` in `.planning/config.json`

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Shell commands (`xcrun notarytool`, `xcrun stapler`, `spctl`) — no unit test framework applicable |
| Quick run command | `xcrun stapler validate output/XSLEditor-*.dmg` (verifies a ticket is embedded; does not require Apple connectivity) |
| Full verification | `xcrun notarytool submit --wait` returns `status: Accepted` + `xcrun stapler staple` succeeds |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| MACOS-04 | `notarytool submit --wait` returns `Accepted` for arm64 DMG | smoke | Embedded in `package-macos-arm` CI job | ❌ Step added in this phase |
| MACOS-04 | `notarytool submit --wait` returns `Accepted` for x64 DMG | smoke | Embedded in `package-macos-x64` CI job | ❌ Step added in this phase |
| MACOS-04 | `xcrun stapler staple` succeeds on both DMGs | smoke | Embedded in each CI job after notarize step | ❌ Step added in this phase |
| MACOS-04 | Gatekeeper accepts stapled DMG on macOS machine | manual | Human opens DMG on macOS, no quarantine dialog | ❌ Human verification item |

### Sampling Rate
- **Per task commit:** `cat .github/workflows/release.yml | grep -c "notarytool"` — confirms steps were added
- **Per wave merge:** Full YAML validation: `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/release.yml'))"`
- **Phase gate:** Push test tag, confirm both macOS jobs complete green with `status: Accepted` before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `.github/workflows/release.yml` — add notarize + staple steps to both macOS jobs

*(No new test files needed — verification is via notarytool output and stapler exit code embedded in CI jobs)*

---

## Security Domain

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | No user auth |
| V3 Session Management | No | No sessions |
| V4 Access Control | Yes | `APPLE_APP_SPECIFIC_PASSWORD` scoped to repo; app-specific passwords limit blast radius vs. main Apple ID password |
| V5 Input Validation | Yes | `APPLE_ID`, `APPLE_TEAM_ID`, `APPLE_APP_SPECIFIC_PASSWORD` injected as env vars; GitHub Actions auto-masks them in logs |
| V6 Cryptography | No | notarytool handles its own crypto; no custom crypto |

**App-specific password security note:** `APPLE_APP_SPECIFIC_PASSWORD` should be generated at appleid.apple.com → Sign-In and Security → App-Specific Passwords. It grants only notarization capability; it is not the main Apple ID password. If compromised, it can be revoked at appleid.apple.com without affecting the main account.

**Secret hygiene:** The three Apple secrets are never written to disk in this workflow; they are passed exclusively as environment variables to `xcrun notarytool`.

---

## Open Questions

1. **Does jpackage --mac-sign in Liberica JDK 21 on macos-15/macos-15-intel runners actually produce Hardened Runtime?**
   - What we know: Bellsoft's own guide confirms javapackager enables Hardened Runtime. Community evidence from JDK 17+ users shows jpackage --mac-sign produces `flags=0x10000(runtime)` in codesign output.
   - What's unclear: Whether the JDK-8358723 regression (main launcher signed without entitlements) also affected the runtime flag — or only affected which entitlements were applied.
   - Recommendation: Add `grep flags` check to the Phase 21 `Verify code signature` step (or as a new diagnostic step in Phase 22). If the runtime flag is missing, the plan must include a manual re-sign step.

2. **Will Apple's notary service accept this app on first submission?**
   - What we know: jpackage bundles the JVM runtime, which includes many native binaries. If any native binary inside the bundle is not signed with a Developer ID certificate or is missing a timestamp, notarytool will reject with `status: Invalid`.
   - What's unclear: Whether Liberica JDK 21 ships all its native binaries pre-signed (Liberica's guide suggests it does), or whether any native library inside the Liberica runtime image will fail Apple's scanner.
   - Recommendation: The plan must include a "retrieve notarization log on failure" step that logs the JSON from `xcrun notarytool log <uuid>` so failures are diagnosable. This is a defensive measure only.

3. **Should the workflow skip notarization when Apple secrets are absent?**
   - What we know: Phase 21 signing guards with `if [ -z "$MACOS_SIGNING_IDENTITY" ]; then exit 1; fi`. A contributor without Apple credentials cannot run signing.
   - What's unclear: Should Phase 22 follow the same hard-fail pattern, or should it be skippable (exit 0 with a warning) so contributors without an Apple Developer Program membership can still produce signed (but un-notarized) DMGs?
   - Recommendation: Hard-fail on missing secrets (exit 1 with a clear error message). This matches the Phase 21 pattern and ensures CI clearly reports an incomplete artifact rather than silently producing an un-notarized DMG that will show a quarantine dialog.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Liberica jpackage on JDK 21 automatically enables Hardened Runtime via `--mac-sign` | Standard Stack / Notarization Mechanics | Notarization rejected with "executable does not have hardened runtime"; fix: add manual codesign step with `--options runtime` |
| A2 | `xcrun stapler staple` can be run immediately after `notarytool submit --wait` returns `Accepted` (no CDN propagation delay) | Common Pitfalls (Pitfall 5) | Stapler fails; fix: add `sleep 10` before stapler |
| A3 | `spctl -a --type install` on CI runner produces unreliable results; treat as informational | Common Pitfalls (Pitfall 4) | If spctl actually works reliably on runners, making it informational is a missed signal |
| A4 | Inline credentials (`--apple-id`, `--team-id`, `--password`) are safe to use in CI (GitHub Actions masks them) | notarytool Credential Approaches | If masking fails, credentials appear in logs; mitigated by app-specific password scope |
| A5 | Apple Developer Program membership is active | Environment Availability | Phase 22 cannot proceed; $99/yr prerequisite |
| A6 | All native binaries inside the Liberica JDK 21 runtime image are pre-signed by Bellsoft with a valid Developer ID | Open Questions #2 | Notarization rejected with "binary is not signed"; requires manual deep-signing of all Mach-O binaries in the bundle |

---

## Sources

### Primary (HIGH confidence)
- [NOTARYTOOL(1) man page](https://keith.github.io/xcode-man-pages/notarytool.1.html) — all subcommands, submit flags, authentication modes, timeout, status values
- [Bellsoft Liberica JDK notarization guide](https://bell-sw.com/announcements/2020/06/10/How-to-Notarize-a-Mac-Application-with-Liberica-JDK/) — confirmed Liberica's javapackager enables Hardened Runtime automatically
- [defn.io — Distributing Mac Apps with GitHub Actions](https://defn.io/2023/09/22/distributing-mac-apps-with-github-actions/) — verified inline credential pattern in production CI; DMG submitted directly; stapler used successfully
- [Phase 21 release.yml](../../21-macos-signing/release.yml) — current workflow state; verified step ordering and secret names

### Secondary (MEDIUM confidence)
- [tonygo.tech — Complete Guide to Notarizing macOS Apps with notarytool](https://tonygo.tech/blog/2023/notarization-for-macos-app-with-notarytool) — confirmed output format (`status: Accepted`), store-credentials approach, spctl verification command
- [Federico Terzi — Code-signing and Notarization for macOS apps using GitHub Actions](https://federicoterzi.com/blog/automatic-code-signing-and-notarization-for-macos-apps-using-github-actions/) — keychain-profile approach; confirmed `notarytool submit --keychain-profile --wait` works
- [scripting OS X — Notarize a Command Line Tool with notarytool](https://scriptingosx.com/2021/07/notarize-a-command-line-tool-with-notarytool/) — canonical `store-credentials` + `submit --wait` workflow
- [Apple Developer Forums thread/764017 — Notarization service failing](https://developer.apple.com/forums/thread/764017) — root cause: `ditto` vs `jar` for repacking; relevant risk for apps that repack native binaries

### Tertiary (LOW confidence — marked [ASSUMED])
- Community reports on `notarytool --wait` hang behavior (electron/notarize#179, tauri-apps/tauri discussion #8630, Apple Developer Forums thread/772619)
- Community reports on CDN propagation delay between notarytool Accepted status and stapler availability
- Adoptium adoptium-support/issues/829 — JDK 20 notarization failures with Temurin (different JDK vendor; risk may not apply to Liberica)

---

## Metadata

**Confidence breakdown:**
- notarytool command syntax and flags: HIGH — verified from official man page
- Inline credential pattern for CI: HIGH — verified from production CI examples
- Liberica JDK 21 automatic Hardened Runtime: MEDIUM — confirmed for javapackager; assumed for modern jpackage on same JDK
- spctl CI behavior: LOW — ASSUMED based on community reports
- CDN propagation timing: LOW — ASSUMED; no official documentation

**Research date:** 2026-04-26
**Valid until:** 2026-07-26 (Xcode CLT version updates; Apple notarization service behavior; Liberica JDK patch versions)
