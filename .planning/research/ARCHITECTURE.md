# Architecture: GitHub Actions CI/CD Distribution Pipeline

**Project:** XSLEditor — Java 21 + JavaFX desktop app
**Milestone:** v0.4.0 — GitHub Releases & Distribution
**Researched:** 2026-04-24
**Overall confidence:** HIGH

---

## Decision: Single Workflow File

Use **one workflow file** — `.github/workflows/release.yml` — triggered by `push` on tags matching `v*`.

Rationale: The three build artifacts (macOS arm64, macOS x64, Windows x64) are built in parallel and then collected by a single release job. A single file keeps all dependencies in one place, makes secret usage auditable, and avoids cross-workflow artifact sharing (which requires additional `github-token` complexity and has a shorter retention window).

A separate `ci.yml` for test-only runs on PRs is appropriate but is out of scope for this milestone.

---

## Job Structure

```
release.yml (on: push tags v*)
│
├── job: build-jar          (ubuntu-latest)
│   └── outputs: fat JAR via ./gradlew shadowJar
│   └── uploads artifact: xsleditor-jar
│
├── job: build-macos-arm64  (macos-15)          ← needs: build-jar
│   └── downloads: xsleditor-jar
│   └── sets up keychain with Developer ID cert
│   └── runs jpackage --type dmg --mac-sign
│   └── notarizes + staples DMG
│   └── uploads artifact: xsleditor-macos-arm64
│
├── job: build-macos-x64    (macos-15-intel)    ← needs: build-jar
│   └── downloads: xsleditor-jar
│   └── sets up keychain with Developer ID cert
│   └── runs jpackage --type dmg --mac-sign
│   └── notarizes + staples DMG
│   └── uploads artifact: xsleditor-macos-x64
│
├── job: build-windows      (windows-latest)    ← needs: build-jar
│   └── downloads: xsleditor-jar
│   └── runs jpackage --type msi
│   └── uploads artifact: xsleditor-windows
│
└── job: release            (ubuntu-latest)     ← needs: [build-macos-arm64, build-macos-x64, build-windows]
    └── downloads: all 4 artifacts (pattern xsleditor-*)
    └── creates GitHub Release via softprops/action-gh-release@v2
    └── attaches: .dmg (arm64), .dmg (x64), .msi, .jar
    └── release notes auto-generated from git log
```

### Why build-jar runs on Linux

The fat JAR produced by `shadowJar` is platform-independent bytecode. Building on `ubuntu-latest` costs x1 GitHub Actions minutes (macOS = x10, Windows = x2). All three platform jobs download this single artifact rather than each running a redundant Gradle build. This is the primary cost optimization.

### Why two separate macOS jobs instead of a matrix

macOS arm64 (`macos-15`) and macOS x64 (`macos-15-intel`) require different physical runners — jpackage produces a native `.app` bundle that must run on the same CPU architecture as the build machine. Cross-compilation is not possible. A `matrix:` strategy over `[macos-15, macos-15-intel]` is functionally equivalent but harder to read when only two variants exist; explicit named jobs are preferred.

Runner label notes:
- `macos-15` — Apple Silicon (arm64), available now
- `macos-15-intel` — Intel x64, available until August 2027
- `macos-13` — retired December 2025, do not use
- `macos-latest` — currently resolves to arm64 (`macos-15`); do not rely on this for architecture-specific builds

---

## jpackage Integration Strategy

### Run jpackage as a shell step, not inside Gradle

Do not create a Gradle task for jpackage. Reasons:

1. The `org.beryx.jpackage` Gradle plugin is unmaintained and incompatible with Gradle 9.
2. The fat JAR already exists on disk after `shadowJar` — jpackage only needs to read it as `--main-jar`.
3. Shell steps are easier to debug in CI logs than opaque Gradle task failures.
4. jpackage is bundled with JDK 14+ — no additional dependency needed.

### jpackage command structure (macOS)

```bash
APP_VERSION="${GITHUB_REF_NAME#v}"   # strip leading "v" — jpackage rejects "v0.4.0"

jpackage \
  --type dmg \
  --input build/libs \
  --main-jar "XSLEditor-${APP_VERSION}.jar" \
  --main-class ch.ti.gagi.xsleditor.Launcher \
  --name XSLEditor \
  --app-version "$APP_VERSION" \
  --vendor "TI GAGI" \
  --mac-package-identifier ch.ti.gagi.xsleditor \
  --mac-signing-key-user-name "$MACOS_SIGNING_IDENTITY" \
  --mac-sign \
  --dest dist/
```

Key flags:
- `--input build/libs` — directory containing the fat JAR (shadowJar writes to `build/libs/`)
- `--main-jar` — references the fat JAR; uses `--main-jar` not `--module` because the app is non-modular
- `--mac-sign` — instructs jpackage to invoke `codesign` internally on all bundled native libs and the .app
- `--mac-signing-key-user-name` — must match the keychain identity name exactly (see Secrets section)

jpackage auto-generates the embedded JRE via jlink using the JDK on the runner. No `--runtime-image` pre-building step is needed.

### jpackage command structure (Windows)

```powershell
$APP_VERSION = $env:GITHUB_REF_NAME -replace '^v', ''

jpackage `
  --type msi `
  --input build\libs `
  --main-jar "XSLEditor-$APP_VERSION.jar" `
  --main-class ch.ti.gagi.xsleditor.Launcher `
  --name XSLEditor `
  --app-version $APP_VERSION `
  --vendor "TI GAGI" `
  --win-dir-chooser `
  --win-menu `
  --win-shortcut `
  --dest dist\
```

No code signing on Windows for v0.4.0. Windows SmartScreen warnings are acceptable for an internal developer tool. Authenticode signing can be added in a future milestone.

---

## Critical Prerequisite: JavaFX Launcher Class

**This is a mandatory code change before jpackage will produce a working installer.**

JavaFX raises "JavaFX runtime components are missing" when the `Main-Class` in the fat JAR manifest extends `javafx.application.Application`. This happens because the Java launcher's module system detects that JavaFX is being launched outside its module context.

The current `build.gradle` sets `Main-Class: ch.ti.gagi.xsleditor.XSLEditorApp`, which extends `Application`. This must be changed for the fat JAR (the `application` plugin's run task is unaffected).

**Fix — create `src/main/java/ch/ti/gagi/xsleditor/Launcher.java`:**

```java
public class Launcher {
    public static void main(String[] args) {
        XSLEditorApp.main(args);
    }
}
```

**Fix — update `build.gradle` shadowJar block:**

```groovy
shadowJar {
    archiveClassifier.set('')
    manifest {
        attributes 'Main-Class': 'ch.ti.gagi.xsleditor.Launcher'   // was XSLEditorApp
    }
    mergeServiceFiles()
}
```

The `application { mainClass }` entry stays as `XSLEditorApp` for the Gradle `run` task. Only shadowJar's manifest changes.

---

## Artifact Upload/Download Strategy

Use `actions/upload-artifact@v4` and `actions/download-artifact@v4` (v4 is current; v3 is deprecated).

**Naming convention:** `xsleditor-{platform}` — one artifact per job.

Upload in each platform job:

```yaml
- uses: actions/upload-artifact@v4
  with:
    name: xsleditor-macos-arm64        # unique per job
    path: dist/*.dmg
    retention-days: 7
```

Download in the release job using pattern matching:

```yaml
- uses: actions/download-artifact@v4
  with:
    pattern: xsleditor-*
    merge-multiple: true               # flattens all artifacts into one dir
    path: dist/
```

This places all files flat into `dist/` — the glob `dist/*` then works directly as the `files:` input to softprops/action-gh-release.

Artifact retention: 7 days is sufficient (GitHub Release is the permanent record). The fat JAR artifact (`xsleditor-jar`) is also uploaded to the release as a standalone asset for users who already have Java 21.

---

## macOS Signing Architecture

### Certificate types required

- **Developer ID Application** certificate — signs the `.app` bundle inside the DMG
- **Developer ID Installer** certificate — only needed for `.pkg` format; skip for DMG in v0.4.0

### Keychain setup (must run before jpackage on every macOS job)

The ephemeral runner has no certificates. The keychain must be populated from secrets:

```bash
# Decode certificate
echo "$MACOS_CERTIFICATE" | base64 --decode > /tmp/cert.p12

# Create temporary keychain
security create-keychain -p "$MACOS_KEYCHAIN_PASSWORD" build.keychain
security default-keychain -s build.keychain
security unlock-keychain -p "$MACOS_KEYCHAIN_PASSWORD" build.keychain
security set-keychain-settings -lut 21600 build.keychain

# Import certificate
security import /tmp/cert.p12 \
  -k build.keychain \
  -P "$MACOS_CERTIFICATE_PASSWORD" \
  -T /usr/bin/codesign \
  -T /usr/bin/security

# Allow codesign non-interactive access
security set-key-partition-list \
  -S apple-tool:,apple: \
  -s -k "$MACOS_KEYCHAIN_PASSWORD" build.keychain
```

jpackage (via `--mac-sign --mac-signing-key-user-name`) then finds the identity in the default keychain without any UI prompt.

### Signing happens inside jpackage

`--mac-sign` causes jpackage to invoke `codesign` on every native library bundled in the `.app`, then on the `.app` bundle itself, then on the final `.dmg`. This is the correct order. Do not run `codesign` manually before or after jpackage.

### Notarization and stapling (after jpackage)

```bash
# Submit and block until Apple responds (typically 2-5 min, up to 10 min)
xcrun notarytool submit dist/XSLEditor-*.dmg \
  --apple-id "$NOTARIZE_APPLE_ID" \
  --password "$NOTARIZE_APP_SPECIFIC_PASSWORD" \
  --team-id "$NOTARIZE_TEAM_ID" \
  --wait

# Staple the notarization ticket to the DMG (enables offline Gatekeeper)
xcrun stapler staple dist/XSLEditor-*.dmg
```

Use `xcrun notarytool` only — `xcrun altool` was removed in Xcode 14. The `macos-15` runner ships Xcode 16.

The step order within each macOS job is:

```
keychain setup → jpackage (signs .app + DMG) → notarytool submit --wait → stapler staple → upload-artifact
```

Only the signed, notarized, stapled DMG is uploaded. The release job receives a fully valid artifact.

---

## Release Creation

Use **`softprops/action-gh-release@v2`**.

Do not use v3 (requires Node 24 runner runtime, not yet universal as of 2026-04). Do not use `gh release create` CLI — multi-file uploads require shell quoting complexity and offer no benefit over the action.

```yaml
- uses: softprops/action-gh-release@v2
  with:
    files: dist/*
    generate_release_notes: true
    draft: false
    prerelease: ${{ contains(github.ref_name, '-') }}
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

`generate_release_notes: true` uses GitHub's built-in changelog (commits between previous tag and current tag). This satisfies REL-01 without a custom script or `git log` parsing.

`prerelease` auto-detection: tags containing a hyphen (e.g. `v0.4.0-beta1`) are marked pre-release automatically.

`GITHUB_TOKEN` is provided automatically — no manual secret setup needed for release creation.

---

## Secrets Management

### Required GitHub repository secrets

Navigate to: Settings > Security > Secrets and variables > Actions

| Secret name | Value | How to obtain |
|-------------|-------|---------------|
| `MACOS_CERTIFICATE` | Base64 of `.p12` file | Export from Keychain Access; `base64 -i cert.p12 \| pbcopy` |
| `MACOS_CERTIFICATE_PASSWORD` | Password set when exporting `.p12` | Set during Keychain Access export |
| `MACOS_SIGNING_IDENTITY` | Full identity string | `security find-identity -v -p codesigning` — e.g. `Developer ID Application: GAGI (TEAMXYZ12)` |
| `MACOS_KEYCHAIN_PASSWORD` | Random strong password | Generated; used only for the ephemeral CI keychain |
| `NOTARIZE_APPLE_ID` | Apple Developer account email | Apple Developer portal |
| `NOTARIZE_APP_SPECIFIC_PASSWORD` | App-specific password | appleid.apple.com > App-Specific Passwords |
| `NOTARIZE_TEAM_ID` | 10-character Team ID | Apple Developer portal > Membership |

### Secrets not needed for Windows

The Windows MSI job uses no secrets in v0.4.0. If Authenticode signing is added later, it requires `WINDOWS_CERTIFICATE` + `WINDOWS_CERTIFICATE_PASSWORD`.

### GITHUB_TOKEN

Provided automatically by GitHub Actions for the repository. Grant it write access to contents (for release creation): the default `GITHUB_TOKEN` permissions include `contents: write` when triggered by a tag push. No manual configuration needed.

---

## Version Extraction

The git tag is the single source of truth for the version. Do not rely on `version` in `build.gradle` (currently hardcoded to `0.3.0`).

```yaml
env:
  GITHUB_REF_NAME: ${{ github.ref_name }}   # e.g. "v0.4.0"
```

In shell steps:

```bash
APP_VERSION="${GITHUB_REF_NAME#v}"   # strips leading "v" → "0.4.0"
```

Pass version to Gradle so `version.properties` inside the JAR reflects the tag:

```bash
./gradlew shadowJar -Pversion="${GITHUB_REF_NAME#v}"
```

This overrides `project.version` at build time. The `processResources { expand(version: project.version) }` block will inject the correct version into the bundled `version.properties`.

---

## New Files and Directories

### Files to create (no `.github` directory currently exists)

```
.github/
  workflows/
    release.yml           ← single workflow file (all 5 jobs)

src/main/java/ch/ti/gagi/xsleditor/
  Launcher.java           ← shim class (5 lines) — critical prerequisite

docs/
  SIGNING.md              ← Developer ID secrets setup guide (SIGN-01 requirement)
```

### Files to modify

```
build.gradle              ← shadowJar manifest Main-Class → Launcher
```

### Files not to create

- No `Makefile` — jpackage invocations are short enough to inline in YAML
- No `jpackage/*.cfg` files — platform-specific flags are few enough to inline; config files add indirection with no benefit at this scale
- No separate `ci.yml` for this milestone

---

## Build Order Summary

```
[tag push v*]
      │
      ▼
build-jar (ubuntu, ~2 min)
  ./gradlew shadowJar -Pversion=X.Y.Z
  → build/libs/XSLEditor-X.Y.Z.jar
  → upload artifact: xsleditor-jar
      │
      ├──────────────────────────────────────────────────────┐
      ▼                                                      ▼
build-macos-arm64 (macos-15, ~10 min)          build-macos-x64 (macos-15-intel, ~10 min)
  download: xsleditor-jar                        download: xsleditor-jar
  keychain setup                                 keychain setup
  jpackage --type dmg --mac-sign                 jpackage --type dmg --mac-sign
  notarytool submit --wait                       notarytool submit --wait
  stapler staple                                 stapler staple
  upload: xsleditor-macos-arm64                  upload: xsleditor-macos-x64
      │                                                      │
      │                        ┌─────────────────────────────┘
      │                        │
      │          build-windows (windows-latest, ~5 min)
      │            download: xsleditor-jar
      │            jpackage --type msi
      │            upload: xsleditor-windows
      │                        │
      └───────────┬────────────┘
                  ▼
            release (ubuntu, ~1 min)
              download-artifact pattern: xsleditor-*
              softprops/action-gh-release@v2
              → GitHub Release with 4 assets:
                  XSLEditor-X.Y.Z-arm64.dmg
                  XSLEditor-X.Y.Z-x64.dmg
                  XSLEditor-X.Y.Z.msi
                  XSLEditor-X.Y.Z.jar
```

---

## Integration Points with Existing Gradle Build

| Existing element | CI usage |
|-----------------|----------|
| `id 'com.gradleup.shadow' version '9.0.0-beta12'` | `./gradlew shadowJar` — no change |
| `shadowJar { archiveClassifier.set('') }` | Output filename is `XSLEditor-{version}.jar` (no classifier) — predictable for `--main-jar` |
| `mainClass = 'ch.ti.gagi.xsleditor.XSLEditorApp'` | Unchanged — used by Gradle `run` task only |
| `java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }` | CI uses `actions/setup-java@v4` with `java-version: '21'` and `distribution: 'temurin'`; toolchain resolves to the runner JDK |
| `processResources { expand(version: project.version) }` | Pass `-Pversion=X.Y.Z` to Gradle so `version.properties` in JAR matches the tag |
| `test { useJUnitPlatform() }` | Run `./gradlew test` in `build-jar` job before `shadowJar` to gate on green tests |

---

## Cost Profile

| Job | Runner | Est. wall time | Minute multiplier | Est. billed min |
|-----|--------|---------------|-------------------|-----------------|
| build-jar | ubuntu-latest | 3 min | x1 | 3 |
| build-macos-arm64 | macos-15 | 10 min | x10 | 100 |
| build-macos-x64 | macos-15-intel | 10 min | x10 | 100 |
| build-windows | windows-latest | 5 min | x2 | 10 |
| release | ubuntu-latest | 1 min | x1 | 1 |
| **Total per release** | | ~10 min wall | | **~214 min** |

Free tier: 2,000 min/month. Each release consumes ~214 billed minutes. Approximately 9 releases per month before hitting the free tier limit.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Single workflow file architecture | HIGH | Standard, widely used pattern |
| macOS runner labels (macos-15 / macos-15-intel) | HIGH | Confirmed from GitHub official docs 2026 |
| jpackage flags and invocation | HIGH | JDK 21 official docs + validated templates |
| Launcher class requirement | HIGH | JavaFX constraint, well-documented |
| notarytool (not altool) | HIGH | altool removed Xcode 14; macos-15 = Xcode 16 |
| softprops/action-gh-release@v2 | HIGH | Stable, maintained, widely used |
| Windows jpackage (unsigned MSI) | HIGH | No signing secrets, straightforward |
| Notarization wait time | MEDIUM | Apple SLA: typically 2-5 min, can spike to 10 min |
| macOS Actions billing multiplier | MEDIUM | x10 confirmed but GitHub pricing can change |

---

## Sources

- [GitHub-hosted runners reference — GitHub Docs](https://docs.github.com/en/actions/reference/runners/github-hosted-runners)
- [softprops/action-gh-release — GitHub Marketplace](https://github.com/marketplace/actions/gh-release)
- [actions/upload-artifact@v4 — GitHub Marketplace](https://github.com/marketplace/actions/upload-a-build-artifact)
- [Get started with v4 of GitHub Actions Artifacts — GitHub Blog](https://github.blog/news-insights/product-news/get-started-with-v4-of-github-actions-artifacts/)
- [maven-jpackage-template apple-sign-notarize.md — Will Iverson](https://github.com/wiverson/maven-jpackage-template/blob/main/docs/apple-sign-notarize.md)
- [Automatic Code-signing and Notarization for macOS apps using GitHub Actions — Federico Terzi](https://federicoterzi.com/blog/automatic-code-signing-and-notarization-for-macos-apps-using-github-actions/)
- [How to Create Platform-Specific Installers from GitHub Actions — DEV Community](https://dev.to/sualeh/how-to-create-platform-specific-installers-for-your-java-applications-from-github-actions-2c15)
- [Cross-platform release builds with GitHub Actions — Electric UI](https://electricui.com/blog/github-actions)
- [How to Fix JavaFX Runtime Components are Missing — Eden Coding](https://edencoding.com/runtime-components-error/)
- [Package a JavaFX Application as Platform Specific Executable — Inside.java](https://inside.java/2023/11/14/package-javafx-native-exec/)
- [macos-15-intel availability — actions/runner-images #13045](https://github.com/actions/runner-images/issues/13045)
- [Merge matrix build artifacts — GitHub Community Discussion #25338](https://github.com/orgs/community/discussions/25338)
