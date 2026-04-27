---
phase: 23-signing-documentation
reviewed: 2026-04-27T00:00:00Z
depth: standard
files_reviewed: 3
files_reviewed_list:
  - docs/SIGNING.md
  - README.md
  - .github/workflows/release.yml
findings:
  critical: 4
  warning: 4
  info: 2
  total: 10
status: issues_found
---

# Phase 23: Code Review Report

**Reviewed:** 2026-04-27
**Depth:** standard
**Files Reviewed:** 3
**Status:** issues_found

## Summary

Three files were reviewed: the new signing guide (`docs/SIGNING.md`), the updated `README.md`, and the release workflow (`.github/workflows/release.yml`). The workflow contains multiple non-existent action versions that will cause hard CI failures on first tag push, a `notarytool --timeout` flag with an invalid value that will break every notarization attempt, and a DMG mountpoint leak when signature verification fails under `set -euo pipefail`. The signing guide also contradicts itself and the workflow regarding whether notarization secrets are active, which will mislead a maintainer into skipping required secret configuration. The README states a JAR filename casing that does not match what the build system actually produces.

---

## Critical Issues

### CR-01: Non-existent action versions will cause hard workflow failures

**File:** `.github/workflows/release.yml:58, 76, 89, 96, 212, 228, 235, 352, 366, 430, 435, 446`

**Issue:** Multiple GitHub Actions are pinned to version tags that do not exist:
- `actions/checkout@v6.0.2` — latest stable is v4.x; v6.0.2 does not exist (lines 58, 89, 228, 366)
- `actions/upload-artifact@v7.0.1` — latest stable is v4.x; v7.0.1 does not exist (lines 76, 212, 352, 430, 435)
- `actions/download-artifact@v8.0.1` — latest stable is v4.x; v8.0.1 does not exist (lines 96, 235, 446)

Every job in the workflow uses at least one of these actions. The workflow will fail at the first step of every job with "Unable to resolve action" before executing any build or packaging logic. No release can be produced until this is fixed.

**Fix:** Replace all three actions with their current stable major-version references:
```yaml
- uses: actions/checkout@v4
- uses: actions/upload-artifact@v4
- uses: actions/download-artifact@v4
```
Pin to a specific SHA for supply-chain security if required:
```yaml
- uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683  # v4.2.2
```

---

### CR-02: `notarytool --timeout 300s` uses invalid argument format

**File:** `.github/workflows/release.yml:197, 336`

**Issue:** The `xcrun notarytool submit` command is invoked with `--timeout 300s`. The `notarytool` binary does not accept a duration string with a unit suffix for `--timeout`; it expects a plain integer representing seconds. Passing `300s` causes `notarytool` to reject the argument and exit with a usage error, failing the notarization step on every release for both arm64 and x64.

```yaml
# Line 197 (arm64) and line 336 (x64):
--timeout 300s    # INVALID — notarytool rejects the 's' suffix
```

**Fix:** Remove the unit suffix:
```yaml
--timeout 300
```

---

### CR-03: DMG mountpoint leaks when signature verification fails under `set -euo pipefail`

**File:** `.github/workflows/release.yml:161-178, 300-317`

**Issue:** Both `Verify code signature` steps begin with `set -euo pipefail` and then run `hdiutil attach`. If the `codesign --verify --deep --strict` command on the next line exits non-zero, bash exits immediately due to `set -e` — before reaching any `hdiutil detach` call. The DMG volume is left mounted at `/tmp/xsleditor-verify-arm64` or `/tmp/xsleditor-verify-x64` for the rest of the runner's life. On a shared runner this is benign (the VM is discarded), but it also means that when the step fails, subsequent retry attempts within the same run cannot re-attach to the same mountpoint, which could obscure the real error with a secondary `hdiutil attach` failure.

More critically, the explicit `TEAM` check block (lines 172-176 / 311-315) does call `hdiutil detach` before `exit 1`, but the `codesign --verify` line above it does not — so a verification failure exits without cleanup.

**Fix:** Use a `trap` to guarantee cleanup regardless of exit path:
```bash
set -euo pipefail
hdiutil attach "output/XSLEditor-${APP_VERSION}-arm64.dmg" \
  -mountpoint /tmp/xsleditor-verify-arm64 -nobrowse -quiet
trap 'hdiutil detach /tmp/xsleditor-verify-arm64 -quiet || true' EXIT
codesign --verify --deep --strict --verbose=2 \
  "/tmp/xsleditor-verify-arm64/XSLEditor.app"
...
# Remove manual hdiutil detach at end — trap handles it
```

---

### CR-04: SIGNING.md falsely states notarization secrets are "not yet used"

**File:** `docs/SIGNING.md:89` (comment block reproduced from workflow)

**Issue:** The Secrets Reference block in SIGNING.md labels the three Apple notarization secrets under:

```
# ── Phase 22: macOS Notarization (reserved — not yet used in this workflow) ──
```

This is factually wrong. The current `release.yml` actively uses `APPLE_ID`, `APPLE_TEAM_ID`, and `APPLE_APP_SPECIFIC_PASSWORD` in the `Notarize DMG` step for both arm64 and x64 jobs (lines 182-198, 320-337). The guard checks (lines 187, 326) will cause the workflow to fail with an explicit error if these secrets are absent.

A maintainer reading this guide will conclude those three secrets are optional and skip setting them, causing every release to fail at the notarization step with "One or more notarization secrets are not configured."

The same incorrect label appears in the source comment block in `release.yml` at lines 28-39.

**Fix:** Update the label in both `docs/SIGNING.md` (line 89) and `.github/workflows/release.yml` (line 29) to reflect that notarization is active:

```
# ── Phase 22: macOS Notarization (active — used in package-macos-arm and package-macos-x64) ──
```

---

## Warnings

### WR-01: README JAR filename casing does not match build output

**File:** `README.md:37, 42`

**Issue:** The README documents the build output as `build/libs/XSLEditor-0.3.0.jar` (PascalCase) and instructs users to run `java -jar build/libs/XSLEditor-0.3.0.jar`. However, the workflow artifact glob at `.github/workflows/release.yml:79` is `xsleditor-*.jar` (all lowercase), and the `--main-jar` argument at lines 147 and 285 is `"xsleditor-${APP_VERSION}.jar"` (lowercase). Gradle's `shadowJar` task derives the output filename from the project `artifactId` (typically lowercase in Gradle convention). Developers following the README will get a file-not-found error when running the `java -jar` command.

**Fix:** Update the README to use the actual filename produced by the build:
```markdown
Output: `build/libs/xsleditor-0.3.0.jar`
```
```bash
java -jar build/libs/xsleditor-0.3.0.jar
```
And update the Gatekeeper workaround command to match:
```bash
xattr -d com.apple.quarantine build/libs/xsleditor-0.3.0.jar
```

---

### WR-02: SIGNING.md contradicts itself about notarization secrets in the verification section

**File:** `docs/SIGNING.md:113`

**Issue:** The "Verify Your Setup" section (line 113) says "If `codesign --verify` or `notarytool submit` fails, check the job log." This implicitly acknowledges notarization runs — but the Secrets Reference section (line 89) labels those same secrets as "reserved — not yet used." A maintainer who skips configuring the notarization secrets based on that label will not know why `notarytool submit` fails, because the guide told them that step would not run. The self-contradiction creates a support trap.

**Fix:** After fixing CR-04 (correcting the "not yet used" label), review the entire document for consistency. The "Verify Your Setup" section content is accurate; the Secrets Reference section label is not.

---

### WR-03: Windows WiX PATH construction is fragile

**File:** `.github/workflows/release.yml:381`

**Issue:** The step adds WiX to PATH using:
```bash
echo "${WIX}bin" >> $GITHUB_PATH
```
The `WIX` environment variable on GitHub-hosted Windows runners is set to a path that includes a trailing backslash (e.g., `C:\Program Files (x86)\WiX Toolset v3.11\`). When running under `shell: bash` (Git Bash/MSYS2 on Windows), `${WIX}` expands to the Windows path and the string concatenation becomes `C:\Program Files (x86)\WiX Toolset v3.11\bin`. This works only because of the trailing backslash in the runner-provided value. If the runner changes or a different WiX version is used that omits the trailing backslash, the PATH entry becomes `...v3.11bin` (broken).

**Fix:** Use a path separator explicitly:
```bash
echo "${WIX}bin" >> $GITHUB_PATH
# or more robustly:
echo "$(cygpath -u "${WIX}")bin" >> $GITHUB_PATH
```
Or prefer the documented recommended approach for WiX on GitHub Actions runners, which avoids relying on `${WIX}` entirely:
```bash
echo "C:/Program Files (x86)/WiX Toolset v3.11/bin" >> $GITHUB_PATH
```

---

### WR-04: `actions/setup-java@v5.2.0` likely does not exist

**File:** `.github/workflows/release.yml:67, 90, 229, 369`

**Issue:** `actions/setup-java` latest stable is v4.x. Version v5.2.0 does not appear to exist in the official repository. If this version tag is absent, the workflow will fail on all four jobs at the Java setup step. This is a separate failure mode from CR-01 (the other action versions) and affects all jobs.

**Fix:** Use the current stable version:
```yaml
- uses: actions/setup-java@v4
  with:
    distribution: liberica
    java-version: '21'
    java-package: jdk+fx
```

---

## Info

### IN-01: Inline image path in README likely broken in most contexts

**File:** `README.md:5`

**Issue:** The README references `![App icon](src/main/resources/ch/ti/gagi/xsleditor/icon.png)`. This relative path resolves correctly when GitHub renders the README from the repo root, but only if that file exists in the repository. If the icon file is absent (e.g., only `.icns` is committed), the image is silently broken in the GitHub README view.

**Fix:** Verify the `.png` file exists at that exact path. If only `.icns` exists, either commit a PNG separately or remove the broken image reference.

---

### IN-02: README version number is stale relative to workflow output

**File:** `README.md:6, 37, 42`

**Issue:** README states `**Version:** 0.3.0` and uses hardcoded `0.3.0` in the build output path and run command. The workflow injects the version from the git tag at build time, so the actual released JAR will have a different version number. Keeping a hardcoded version in the README creates ongoing maintenance overhead and will mislead developers on non-release builds.

**Fix:** Replace the hardcoded version with a placeholder that acknowledges this:
```markdown
**Version:** see [Releases](../../releases) for the latest tag

Output: `build/libs/xsleditor-<version>.jar`
```

---

_Reviewed: 2026-04-27_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
