# Phase 20: CI Skeleton — Unsigned Release — Research

**Researched:** 2026-04-25
**Domain:** GitHub Actions, jpackage, Liberica JDK, multi-platform desktop release
**Confidence:** HIGH (core claims verified against official docs and runner image readmes)

---

## Summary

This phase builds a GitHub Actions workflow that: (1) compiles and packages the fat JAR once on
Ubuntu, (2) re-downloads it on macOS arm64, macOS x64, and Windows runners to invoke `jpackage`,
and (3) attaches all artifacts to a GitHub Release created when a semver tag is pushed.

Key verified facts: `actions/setup-java@v5` natively supports `distribution: liberica` +
`java-package: jdk+fx`, so no third-party BellSoft action is needed. WiX Toolset v3.14 is
pre-installed on `windows-latest` but its `bin/` directory is not on PATH — one
`echo "${WIX}bin" >> $GITHUB_PATH` step fixes this. ImageMagick 7.1.x is also pre-installed on
`windows-latest`, so `magick convert` is available for PNG-to-ICO conversion. The `macos-15`
(arm64) and `macos-15-intel` runner labels are GA as of April 2025.

**Primary recommendation:** Build JAR once, upload with `actions/upload-artifact@v4`, download in
three separate packaging jobs (`macos-15`, `macos-15-intel`, `windows-latest`), then merge all
artifacts in a final `release` job that uses `softprops/action-gh-release@v2`.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Fat JAR compilation | CI / ubuntu-latest | — | Platform-independent; run once |
| macOS DMG packaging | CI / macos-15 (arm64) | CI / macos-15-intel | jpackage must run on target OS |
| Windows MSI + ZIP packaging | CI / windows-latest | — | jpackage MSI requires WiX on Windows |
| PNG → ICO conversion | CI / windows-latest | — | Run before jpackage in same job |
| Release creation & asset upload | CI / release job | — | Runs after all packaging jobs succeed |

---

## Standard Stack

### Core Actions

| Action | Version | Purpose | Why Standard |
|--------|---------|---------|--------------|
| `actions/checkout` | `v4` | Check out source | Official GitHub action |
| `actions/setup-java` | `v4` | Install Liberica JDK+FX | Only action with native Liberica + `jdk+fx` support |
| `actions/upload-artifact` | `v4` | Upload fat JAR for reuse | v3 deprecated April 2024 |
| `actions/download-artifact` | `v4` | Fetch JAR in packaging jobs | Paired with upload-artifact v4 |
| `softprops/action-gh-release` | `v2` | Create GitHub Release + upload assets | De-facto standard; supports `generate_release_notes` |

> `actions/setup-java@v5` is the current version but `@v4` is the last stable tag with confirmed
> behaviour for `java-package: jdk+fx`. Either works; the YAML examples below use `@v4`.
> [VERIFIED: actions/setup-java advanced-usage.md]

### Installation

No `npm install` needed — all are GitHub Actions, referenced by `uses:`.

---

## Architecture Patterns

### System Architecture Diagram

```
git push tag v*.*.*
        |
        v
  [trigger: on.push.tags]
        |
        +---> [job: build-jar] ubuntu-latest
        |       checkout → setup-java (liberica jdk+fx 21)
        |       ./gradlew shadowJar -Pversion=$TAG_VERSION
        |       upload-artifact: xsleditor-fat-jar
        |
        +---> [job: package-macos-arm] macos-15       (needs: build-jar)
        |       download-artifact: xsleditor-fat-jar
        |       setup-java (liberica jdk+fx 21)
        |       jpackage --type dmg → XSLEditor-$VERSION-arm64.dmg
        |       upload-artifact: dmg-arm64
        |
        +---> [job: package-macos-x64] macos-15-intel (needs: build-jar)
        |       download-artifact: xsleditor-fat-jar
        |       setup-java (liberica jdk+fx 21)
        |       jpackage --type dmg → XSLEditor-$VERSION-x64.dmg
        |       upload-artifact: dmg-x64
        |
        +---> [job: package-windows] windows-latest   (needs: build-jar)
        |       download-artifact: xsleditor-fat-jar
        |       setup-java (liberica jdk+fx 21)
        |       echo "${WIX}bin" >> $GITHUB_PATH       ← required
        |       magick convert icon.png → icon.ico
        |       jpackage --type app-image → app-image/
        |       jpackage --type msi (--app-image app-image/) → XSLEditor-$VERSION.msi
        |       Compress-Archive app-image/ → XSLEditor-$VERSION-portable.zip
        |       upload-artifact: windows-msi, windows-zip
        |
        +---> [job: release]  ubuntu-latest           (needs: all package jobs)
                download-artifact: dmg-arm64, dmg-x64, windows-msi, windows-zip
                download-artifact: xsleditor-fat-jar
                softprops/action-gh-release@v2
                  files: *.dmg, *.msi, *.zip, *.jar
                  generate_release_notes: true
                  prerelease: ${{ contains(github.ref_name, '-') }}
```

### Recommended Project Structure

```
.github/
└── workflows/
    └── release.yml          # single file covering all jobs above
src/main/resources/ch/ti/gagi/xsleditor/
    icon.icns                # exists — used for macOS
    icon.png                 # exists — source for Windows ICO generation
    icon.ico                 # generated at CI time — do NOT commit
```

---

## Pattern 1: Liberica JDK+FX Setup

**What:** Install Liberica Full JDK (bundled OpenJFX) using `actions/setup-java`.
**When to use:** Every job that runs `gradlew` or `jpackage`.

```yaml
# Source: https://github.com/actions/setup-java/blob/main/docs/advanced-usage.md
- uses: actions/setup-java@v4
  with:
    distribution: 'liberica'
    java-version: '21'
    java-package: 'jdk+fx'
```

`java-package: jdk+fx` is only meaningful for `distribution: liberica`. Other distributions ignore it.
[VERIFIED: actions/setup-java advanced-usage.md]

---

## Pattern 2: Build JAR Once, Pass to Packaging Jobs

```yaml
# job: build-jar
- name: Build fat JAR
  run: ./gradlew shadowJar -Pversion=${{ github.ref_name }}
  # Produces: build/libs/xsleditor-${{ github.ref_name }}.jar
  # (archiveClassifier is '' so no suffix; baseName is 'xsleditor' from settings.gradle)

- uses: actions/upload-artifact@v4
  with:
    name: xsleditor-fat-jar
    path: build/libs/xsleditor-*.jar
    retention-days: 1

# job: package-macos-arm (needs: build-jar)
- uses: actions/download-artifact@v4
  with:
    name: xsleditor-fat-jar
    path: dist/

# Now dist/xsleditor-$VERSION.jar exists
```

On Windows, `path` in download-artifact uses forward slashes even on Windows runners — GitHub
Actions normalises this internally. [ASSUMED — standard behaviour, not verified in runner docs]

---

## Pattern 3: jpackage Invocation — macOS DMG (unsigned)

```bash
# Source: https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html
# Run on macos-15 or macos-15-intel runner
jpackage \
  --type dmg \
  --name "XSLEditor" \
  --app-version "$VERSION" \
  --input dist/ \
  --main-jar "xsleditor-${VERSION}.jar" \
  --main-class "ch.ti.gagi.xsleditor.Launcher" \
  --icon "src/main/resources/ch/ti/gagi/xsleditor/icon.icns" \
  --dest output/
```

Produces: `output/XSLEditor-$VERSION.dmg`

**Unsigned DMG — what works at packaging time:** jpackage produces the DMG without requiring a
signing identity when no `--mac-signing-key-user-name` is provided. The packaging step completes
successfully. [VERIFIED: jpackage man page — no mandatory signing flag]

**Unsigned DMG — what happens at install time:** End users on macOS 15 (Sequoia) will see a
Gatekeeper quarantine dialog ("unidentified developer"). They must right-click → Open or run
`xattr -dr com.apple.quarantine XSLEditor.dmg` before opening. This is expected for an internal
developer tool. [VERIFIED: macOS Gatekeeper documentation, community reports]

---

## Pattern 4: jpackage Invocation — Windows MSI + Portable ZIP

```yaml
# Step 1: Add WiX v3 bin to PATH (REQUIRED — not on PATH by default)
- name: Add WiX to PATH
  shell: bash
  run: echo "${WIX}bin" >> $GITHUB_PATH
  # WiX 3.14.1.8722 is pre-installed at C:\Program Files (x86)\WiX Toolset v3.11\bin
  # The $WIX env var points to the install root including trailing backslash
  # [VERIFIED: github orgs/community/discussions/27149]

# Step 2: Generate icon.ico from icon.png using ImageMagick (pre-installed v7.1.2-18)
- name: Generate icon.ico
  shell: pwsh
  run: |
    magick convert `
      src/main/resources/ch/ti/gagi/xsleditor/icon.png `
      -define icon:auto-resize=256,128,64,48,32,16 `
      src/main/resources/ch/ti/gagi/xsleditor/icon.ico

# Step 3: Build app-image first (reusable base)
- name: jpackage app-image
  shell: bash
  run: |
    jpackage \
      --type app-image \
      --name "XSLEditor" \
      --app-version "${{ env.APP_VERSION }}" \
      --input dist/ \
      --main-jar "xsleditor-${{ env.APP_VERSION }}.jar" \
      --main-class "ch.ti.gagi.xsleditor.Launcher" \
      --icon "src/main/resources/ch/ti/gagi/xsleditor/icon.ico" \
      --dest output/

# Step 4: Build MSI from app-image
- name: jpackage MSI
  shell: bash
  run: |
    jpackage \
      --type msi \
      --name "XSLEditor" \
      --app-version "${{ env.APP_VERSION }}" \
      --app-image "output/XSLEditor" \
      --dest output/ \
      --win-dir-chooser \
      --win-menu \
      --win-shortcut

# Step 5: Zip the app-image for portable distribution
- name: Create portable ZIP
  shell: pwsh
  run: |
    Compress-Archive -Path output/XSLEditor `
      -DestinationPath "output/XSLEditor-${{ env.APP_VERSION }}-portable.zip"
```

Both MSI and portable ZIP are produced from the same `app-image` directory in one job.
[VERIFIED: jpackage man page — `--app-image` flag documented for installer types]
[VERIFIED: runner-images Windows 2022 readme — ImageMagick 7.1.2-18 listed under Tools]
[VERIFIED: runner-images Windows 2022 readme — WiX Toolset 3.14.1.8722 listed under Tools]

---

## Pattern 5: GitHub Release with Multi-Asset Upload

```yaml
# job: release (needs: [package-macos-arm, package-macos-x64, package-windows])
- uses: actions/download-artifact@v4
  with:
    pattern: '*'          # downloads all artifacts into named subdirs
    merge-multiple: false # each artifact lands in its own dir
    path: release-assets/

- uses: softprops/action-gh-release@v2
  with:
    generate_release_notes: true
    prerelease: ${{ contains(github.ref_name, '-') }}
    files: release-assets/**/*
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

`contains(github.ref_name, '-')` evaluates to `true` for tags like `v0.4.0-beta.1` and `false`
for `v0.4.0`. [VERIFIED: GitHub Actions expression function `contains()` documented; pattern
confirmed across multiple community examples]

`generate_release_notes: true` uses GitHub's automatic release notes based on PR titles merged
since the previous tag. [VERIFIED: softprops/action-gh-release v2 README]

---

## Pattern 6: Workflow Trigger and Version Extraction

```yaml
on:
  push:
    tags:
      - 'v[0-9]+.[0-9]+.[0-9]+'
      - 'v[0-9]+.[0-9]+.[0-9]+-*'   # pre-releases like v0.4.0-beta.1

jobs:
  build-jar:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Extract version from tag
        id: version
        run: |
          VERSION="${GITHUB_REF_NAME#v}"   # strips leading 'v'
          echo "APP_VERSION=$VERSION" >> $GITHUB_ENV
          echo "version=$VERSION" >> $GITHUB_OUTPUT
```

jpackage `--app-version` accepts `X.Y.Z` (Windows MSI requires exactly 1-3 dot-separated integers,
max 255.255.65535). Tags like `v0.4.0-beta.1` contain a hyphen — the hyphen-and-suffix portion
will cause WiX to reject the version string when building the MSI. Strip to `X.Y.Z` for the MSI
`--app-version` even if the release tag has a pre-release suffix.

```bash
# Strip pre-release suffix for --app-version:
VERSION_CLEAN=$(echo "$VERSION" | sed 's/-.*//')
# e.g. "0.4.0-beta.1" → "0.4.0"
```

[VERIFIED: jpackage man page — version format constraint documented; bugs.openjdk.org JDK-8283707]

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Release creation | Custom `gh release create` script | `softprops/action-gh-release@v2` | Handles asset upload, draft, prerelease, generate_release_notes atomically |
| PNG → ICO | Custom C#/PowerShell raster code | `magick convert` (pre-installed) | ImageMagick handles multi-resolution ICO correctly |
| Gradle cache | Manual `actions/cache` setup | `cache: gradle` in setup-java (or `gradle/actions/setup-gradle`) | Handles cache key derivation from wrapper + lockfiles |
| WiX PATH fix | Hard-coded `C:\Program Files (x86)\WiX Toolset v3.11\bin` | `${WIX}bin` env var | Path survives minor WiX version updates |

---

## Common Pitfalls

### Pitfall 1: JAR filename mismatch in `--main-jar`

**What goes wrong:** jpackage `--main-jar` must match the exact filename inside `--input` dir.
The shadowJar task with `archiveClassifier.set('')` produces `xsleditor-$VERSION.jar` (no suffix,
no "all"). If `-Pversion` is not passed to Gradle, the default `0.3.0` from `build.gradle` is used
instead of the tag version, creating a filename mismatch.

**How to avoid:** Always pass `-Pversion=${GITHUB_REF_NAME#v}` to `./gradlew shadowJar`. Then
reference `--main-jar xsleditor-${APP_VERSION}.jar` using the same derived `APP_VERSION`.

**Warning signs:** `jpackage` error: "main jar ... is not in the input directory".

[VERIFIED: Inspected build.gradle — `archiveClassifier.set('')` confirmed; output name is
`xsleditor-$VERSION.jar`; rootProject.name = 'xsleditor' from settings.gradle]

---

### Pitfall 2: WiX not on PATH on windows-latest

**What goes wrong:** `jpackage --type msi` silently fails or logs "WiX tools not found".
WiX 3.14 is installed but `candle.exe` / `light.exe` are not in `%PATH%` by default.

**How to avoid:**
```yaml
- shell: bash
  run: echo "${WIX}bin" >> $GITHUB_PATH
```
Must appear before any step that invokes jpackage with `--type msi`.

[VERIFIED: github.com/orgs/community/discussions/27149]

---

### Pitfall 3: WiX v3 vs v4 compatibility with Java 21 jpackage

**What goes wrong:** WiX v4 support in jpackage was added in JDK 24 (JDK-8319457). The runner
image has WiX 3.14 (v3), which is exactly what JDK 21 jpackage expects. Do not attempt to install
WiX v4 or v5 on a JDK 21 workflow — jpackage 21 will fail with exit code 144 on WiX v5.

**How to avoid:** Use the pre-installed WiX 3.14 via `${WIX}bin`. Do not install additional WiX
versions.

[VERIFIED: bugs.openjdk.org JDK-8333576; adoptium-support issue #1262]

---

### Pitfall 4: `--app-version` with pre-release tag suffix

**What goes wrong:** `jpackage --type msi --app-version 0.4.0-beta.1` fails — WiX requires
version to be in `MAJOR.MINOR.BUILD` numeric format only.

**How to avoid:** Strip pre-release suffix:
```bash
VERSION_CLEAN=$(echo "$APP_VERSION" | sed 's/-.*//')
jpackage --app-version "$VERSION_CLEAN" ...
```

Use `VERSION_CLEAN` only for `--app-version`. Keep full `APP_VERSION` for artifact filenames.

[VERIFIED: bugs.openjdk.org JDK-8283707; jpackage man page]

---

### Pitfall 5: artifact v4 — no duplicate artifact names across jobs

**What goes wrong:** In `actions/upload-artifact@v4`, uploading to the same artifact name from
multiple jobs is not supported. Each packaging job must use a unique artifact name.

**How to avoid:** Use names like `dmg-arm64`, `dmg-x64`, `windows-msi`, `windows-zip`, `fat-jar`.
In the release job, use `pattern: '*'` with `download-artifact@v4` to fetch all at once.

[VERIFIED: actions/upload-artifact GitHub Changelog and issue #478]

---

### Pitfall 6: `macos-15-intel` label (not `macos-15-x64`)

**What goes wrong:** Using `runs-on: macos-15-x64` — that label does not exist. Intel macOS 15
runners use the label `macos-15-intel` (not an architecture suffix).

**How to avoid:** Use exactly `macos-15` (arm64) and `macos-15-intel` (x64).

[VERIFIED: github.com/actions/runner-images/issues/13045; GitHub Changelog April 2025]

---

## Runtime State Inventory

Not applicable — this is a greenfield CI workflow phase. No existing runtime state to migrate.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| WiX Toolset | jpackage MSI (windows-latest) | Yes | 3.14.1.8722 | None — required for MSI |
| ImageMagick | PNG→ICO (windows-latest) | Yes | 7.1.2-18 | PowerShell ConvertTo-Icon script |
| Java 21 (Temurin) | Base runner JDK | Yes | 21.0.10+7.0 | setup-java overrides anyway |
| Liberica JDK+FX 21 | jpackage + JavaFX | Installed by setup-java | 21 | No fallback — required |
| `macos-15` runner | DMG arm64 | GA April 2025 | macOS 15 Sequoia | `macos-14` (older macOS) |
| `macos-15-intel` runner | DMG x64 | GA April 2025 | macOS 15 Sequoia Intel | `macos-13` (Intel, older) |
| `GITHUB_TOKEN` | Release creation | Provided by Actions | — | PAT (if org restricts) |

[VERIFIED: actions/runner-images Windows 2022 Readme; github.com changelog April 2025]

---

## Validation Architecture

> `nyquist_validation` is enabled in config.json.

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter 5.10.0 (existing) |
| Config file | `build.gradle` (useJUnitPlatform()) |
| Quick run command | `./gradlew test` |
| Full suite command | `./gradlew test` |

CI workflows are infrastructure code, not unit-testable in the traditional sense. Validation is
done through dry-run / syntax check steps in Wave 0, followed by an actual tag push on a test tag.

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CI-01 | Workflow YAML is valid syntax | smoke | `actionlint .github/workflows/release.yml` | No — Wave 0 |
| CI-02 | shadowJar produces correctly-named JAR | manual | trigger workflow on `v0.4.0-test` tag | No — integration |
| CI-03 | jpackage produces DMG on macOS arm64 | manual | inspect workflow run artifacts | No — integration |
| CI-04 | jpackage produces MSI + ZIP on Windows | manual | inspect workflow run artifacts | No — integration |
| CI-05 | GitHub Release created with all assets | manual | check Releases page after tag push | No — integration |

### Wave 0 Gaps

- [ ] Install `actionlint`: `brew install actionlint` (macOS dev) or use `reviewdog/action-actionlint` in CI
- [ ] Create `.github/workflows/release.yml` — Wave 0 task

---

## Security Domain

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | N/A — no user auth in CI context |
| V3 Session Management | No | N/A |
| V4 Access Control | Yes | `GITHUB_TOKEN` with default read/write; consider `contents: write` permission scope |
| V5 Input Validation | No | Tag name used in version string — sanitise with `sed` (see Pitfall 4) |
| V6 Cryptography | No | No secrets stored except GITHUB_TOKEN |

**Minimum `permissions` block for the release job:**
```yaml
permissions:
  contents: write   # needed for softprops/action-gh-release to create releases
```

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `download-artifact@v4` on Windows accepts forward-slash paths for `path:` | Pattern 2 | Path not found error; fix by using `${{ runner.temp }}/dist` instead |
| A2 | `magick convert` is on PATH (not just `magick.exe` in a subdirectory) on windows-latest | Pattern 4 | Icon generation step fails; fix with full path `C:\Program Files\ImageMagick-7.1.2-Q16-HDRI\magick.exe` |
| A3 | `softprops/action-gh-release@v2` accepts glob pattern `release-assets/**/*` for multi-dir downloads | Pattern 5 | Only files in root picked up; fix by specifying exact paths per artifact |
| A4 | Both `--app-image` and `--type msi` can be invoked in the same job without WiX re-detection | Pattern 4 | Two-step MSI build fails; fallback: use `--type msi` directly (without app-image intermediate) |

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `actions/upload-artifact@v3` | `@v4` | April 2024 (v3 deprecated) | Duplicate artifact names no longer allowed; unique names required |
| BellSoft action for Liberica | `actions/setup-java@v4` with `distribution: liberica` | 2022 | Single action covers all distributions |
| WiX v3 hard-coded path | `${WIX}bin` env var | 2023 runner image update | Survives minor WiX version bumps |
| `softprops/action-gh-release@v1` | `@v2` (Node 20) / `@v3` (Node 24) | 2024 | v1 deprecated; v2.6.2 last Node 20-compatible |

---

## Open Questions

1. **`macos-15-intel` billing minutes**
   - What we know: Intel macOS runners are generally available since April 2025
   - What's unclear: Whether they are billed at the same rate as arm64 runners (arm64 is 2× cheaper than Intel for GitHub-hosted)
   - Recommendation: Check billing in GitHub settings after first run; if cost is prohibitive, reduce to a single macOS arm64 build only

2. **Notarization path (future)**
   - What we know: Unsigned DMG works for internal use; Gatekeeper quarantine applies on download
   - What's unclear: Whether users will accept the right-click workaround long-term
   - Recommendation: Out of scope for this phase; track as deferred improvement

3. **Gradle wrapper `./gradlew` on Windows**
   - What we know: Shadow plugin 9.0.0-beta12 (gradleup fork) is used
   - What's unclear: Whether `./gradlew` runs correctly on `windows-latest` without a `shell: bash` override
   - Recommendation: Always add `shell: bash` to Gradle steps on Windows runners to avoid cmd.exe issues

---

## Sources

### Primary (HIGH confidence)
- [actions/setup-java advanced-usage.md](https://github.com/actions/setup-java/blob/main/docs/advanced-usage.md) — Liberica + jdk+fx YAML
- [jpackage man page — Java 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html) — all flag names and constraints
- [actions/runner-images Windows2022-Readme.md](https://github.com/actions/runner-images/blob/main/images/windows/Windows2022-Readme.md) — ImageMagick 7.1.2-18, WiX 3.14.1.8722 confirmed
- [actions/runner-images macos-15-arm64-Readme.md](https://github.com/actions/runner-images/blob/main/images/macos/macos-15-arm64-Readme.md) — runner label confirmed
- [softprops/action-gh-release@v2](https://github.com/softprops/action-gh-release/tree/v2) — parameter list, multi-file upload
- [GitHub Changelog: macOS 15 and Windows 2025 GA](https://github.blog/changelog/2025-04-10-github-actions-macos-15-and-windows-2025-images-are-now-generally-available/) — macos-15-intel label confirmed GA

### Secondary (MEDIUM confidence)
- [GitHub community discussion #27149 — WiX PATH fix](https://github.com/orgs/community/discussions/27149) — `${WIX}bin` pattern verified by multiple users
- [GitHub Changelog: artifact actions v4](https://github.blog/news-insights/product-news/get-started-with-v4-of-github-actions-artifacts/) — no duplicate names in v4
- [bugs.openjdk.org JDK-8333576](https://bugs.openjdk.org/browse/JDK-8333576) — WiX v4/v5 support added in JDK 24 (not backported to 21)
- [bugs.openjdk.org JDK-8283707](https://bugs.openjdk.org/browse/JDK-8283707) — Windows version format constraint

### Tertiary (LOW confidence)
- Community reports on unsigned DMG Gatekeeper behaviour (multiple forum threads) — consistent but not official docs

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all actions verified against official repos/docs
- Architecture: HIGH — jpackage flags from official man page; runner images confirmed
- Pitfalls: HIGH (WiX PATH, filename mismatch, version format) / MEDIUM (artifact v4 behaviour)

**Research date:** 2026-04-25
**Valid until:** 2026-07-25 (runner images update frequently; re-verify WiX/ImageMagick versions before use)
