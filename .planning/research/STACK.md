# Technology Stack — v0.4.0 GitHub Actions CI/CD & Distribution

**Project:** XSLEditor
**Milestone:** v0.4.0 GitHub Releases & Distribution
**Researched:** 2026-04-24
**Mode:** New milestone — CI/CD and distribution pipeline only

Existing stack (Java 21, JavaFX 21, Gradle 9, com.gradleup.shadow 9.0.0-beta12, Saxon-HE 12.4, Apache FOP 2.9) is unchanged. This document covers only what is needed for the new distribution milestone.

---

## GitHub Actions — Action Versions

Use these exact versions. All are the current stable major tags as of April 2026.

| Action | Version | Purpose |
|--------|---------|---------|
| `actions/checkout` | `v4` | Checkout repo with full git history (needed for release notes) |
| `actions/setup-java` | `v5` | Install JDK 21 (Temurin distribution) with Gradle cache |
| `actions/upload-artifact` | `v4` | Pass per-platform build output to the release job |
| `actions/download-artifact` | `v4` | Collect artifacts in the release job |
| `softprops/action-gh-release` | `v2` | Create GitHub Release and attach assets |
| `apple-actions/import-codesign-certs` | `v3` | Import p12 certificate into macOS keychain |

**Why v2 for action-gh-release, not v3:** v3.0.0 (Node 24 runtime) was released April 2026 and is still settling. v2.6.2 is the last stable Node 20 line and is fully functional. Upgrade to v3 when it has been stable for a few weeks.

**Why v4 for artifact actions:** v3 was deprecated January 30, 2025 and is unsupported. v4 is the stable line; v5+ exists but is experimental. Do not use v3.

---

## Workflow Structure

One workflow file: `.github/workflows/release.yml`

**Trigger:** `push` on tags matching `v*`

**Jobs (sequential via `needs:`):**

```
build-macos   (macos-latest)  →  upload artifact: XSLEditor-macos.dmg
build-windows (windows-latest) →  upload artifact: XSLEditor-windows.msi
build-jar     (ubuntu-latest)  →  upload artifact: XSLEditor-{version}.jar
release       (ubuntu-latest)  →  download all 3, create GitHub Release, attach assets
```

Run the three build jobs in parallel (no `needs:` between them). The release job `needs: [build-macos, build-windows, build-jar]`.

Do NOT use a matrix strategy here. The three platforms require platform-specific signing steps and different jpackage options. A matrix collapses that distinction and makes per-platform conditionals ugly. Three named jobs is simpler, more readable, and easier to debug.

---

## Java Setup — Recommended Configuration

```yaml
- uses: actions/setup-java@v5
  with:
    distribution: 'temurin'
    java-version: '21'
    cache: 'gradle'
```

Use `temurin` (Eclipse Adoptium). It ships full JDK including `jpackage` on all three platforms. Avoid `zulu` — jpackage availability on Zulu builds is inconsistent. Avoid `oracle` — license friction.

The `cache: 'gradle'` option caches `~/.gradle/caches` and `~/.gradle/wrapper`. Enables significantly faster CI runs after the first.

---

## jpackage Integration with shadowJar

### The Launcher Wrapper Requirement

`XSLEditorApp` extends `javafx.application.Application` directly (verified in codebase). When a class that extends `Application` is the JAR manifest `Main-Class`, the JVM checks that JavaFX modules are on the module path. In a fat JAR, JavaFX is on the classpath, not the module path — this causes a runtime error.

**Required fix: add a Launcher class** that does not extend `Application`:

```java
// src/main/java/ch/ti/gagi/xsleditor/Launcher.java
package ch.ti.gagi.xsleditor;

public class Launcher {
    public static void main(String[] args) {
        XSLEditorApp.main(args);
    }
}
```

Update `build.gradle` and `shadowJar` manifest to use `ch.ti.gagi.xsleditor.Launcher` as `Main-Class`. The existing `application { mainClass }` can stay as-is for local `./gradlew run`.

### jpackage Input — Fat JAR from shadowJar

jpackage accepts `--input <dir>` containing the JAR and `--main-jar <filename>`. The shadow JAR (with `archiveClassifier.set('')`) produces `build/libs/XSLEditor-<version>.jar`. Use that directly.

```bash
jpackage \
  --input build/libs \
  --main-jar XSLEditor-${VERSION}.jar \
  --main-class ch.ti.gagi.xsleditor.Launcher \
  --name XSLEditor \
  --app-version ${VERSION} \
  --type dmg          # or msi on Windows
```

jpackage will run `jlink` internally to create an embedded JRE that includes the JVM. Because JavaFX is bundled in the fat JAR (classpath), NOT on the module path, jpackage does NOT need JavaFX jmods. Do NOT pass `--add-modules javafx.controls` etc. — that would conflict with the fat JAR approach.

**What jpackage embeds:** A stripped JRE (via jlink) containing only the modules referenced by the application's code. Since JavaFX is on the classpath (fat JAR), jpackage's jlink step only bundles the core JVM modules. This is correct behavior for the non-modular fat JAR approach.

---

## macOS Signing & Notarization

### Certificate Type

Use **Developer ID Application** certificate (not Mac App Store, not Development). This is required for distribution outside the App Store. The certificate is issued by Apple Developer Program membership.

### Secrets Required (repository secrets)

| Secret Name | Value |
|-------------|-------|
| `MACOS_CERTIFICATE` | Base64-encoded `.p12` file |
| `MACOS_CERTIFICATE_PWD` | Password used when exporting the `.p12` |
| `MACOS_CERTIFICATE_NAME` | Common name, e.g. `Developer ID Application: Name (TEAMID)` |
| `APPLE_ID` | Apple ID email (for notarization) |
| `APPLE_APP_SPECIFIC_PWD` | App-specific password from appleid.apple.com |
| `APPLE_TEAM_ID` | 10-character Team ID from developer.apple.com |

Export the `.p12` from Keychain Access: select the Developer ID Application cert + private key → Export → Personal Information Exchange (.p12). Base64-encode: `base64 -i cert.p12 | pbcopy`.

### macOS Signing Steps (in workflow)

```yaml
- uses: apple-actions/import-codesign-certs@v3
  with:
    p12-file-base64: ${{ secrets.MACOS_CERTIFICATE }}
    p12-password: ${{ secrets.MACOS_CERTIFICATE_PWD }}

- name: Build DMG (unsigned)
  run: |
    ./gradlew shadowJar
    jpackage \
      --input build/libs \
      --main-jar XSLEditor-${VERSION}.jar \
      --main-class ch.ti.gagi.xsleditor.Launcher \
      --name XSLEditor \
      --app-version ${VERSION} \
      --type dmg \
      --mac-sign \
      --mac-signing-key-user-name "${{ secrets.MACOS_CERTIFICATE_NAME }}"

- name: Notarize DMG
  run: |
    xcrun notarytool submit XSLEditor-${VERSION}.dmg \
      --apple-id "${{ secrets.APPLE_ID }}" \
      --password "${{ secrets.APPLE_APP_SPECIFIC_PWD }}" \
      --team-id "${{ secrets.APPLE_TEAM_ID }}" \
      --wait

- name: Staple DMG
  run: xcrun stapler staple XSLEditor-${VERSION}.dmg
```

**Use `notarytool`, not `altool`.** Apple deprecated `altool` after Fall 2023; it no longer works. `notarytool` is available on macOS 12+ runners. `macos-latest` is macOS 15 as of April 2026 — confirmed safe.

**The `--wait` flag** on `notarytool submit` blocks the step until Apple's notarization service returns a result (typically 1-5 minutes). Simpler than polling.

**Stapling** attaches the notarization ticket to the DMG so Gatekeeper can verify offline. Never skip stapling.

### jpackage `--mac-sign` vs manual codesign

Use `--mac-sign` + `--mac-signing-key-user-name` on jpackage. This signs the `.app` bundle internally (all executables inside) during the jpackage build, which is the correct approach. Do NOT sign the DMG manually with `codesign` after jpackage — sign the `.app` via jpackage, then notarize the DMG.

---

## Windows Installer

### WiX Requirement

jpackage requires WiX 3 to build `.msi` and `.exe` installer types on Windows. WiX 3.14.1 is preinstalled on `windows-latest` GitHub-hosted runners. No installation step needed.

**Critical version constraint:** jpackage (up to JDK 21) looks for `candle.exe` and `light.exe` on PATH — these are WiX 3 tools. WiX 4 and WiX 5 use a different CLI (`wix build`) and do NOT provide `candle.exe`/`light.exe`. If WiX 4+ is on PATH and WiX 3 is not, jpackage fails. The `windows-latest` runner has WiX 3 on PATH — do not install WiX 4 in the workflow.

### Windows jpackage Command

```bash
jpackage `
  --input build/libs `
  --main-jar XSLEditor-${VERSION}.jar `
  --main-class ch.ti.gagi.xsleditor.Launcher `
  --name XSLEditor `
  --app-version ${VERSION} `
  --type msi `
  --win-dir-chooser `
  --win-menu `
  --win-shortcut
```

Windows runner uses PowerShell by default — use backtick (`` ` ``) for line continuation, or write as a single line. Alternatively use `shell: bash` in the step to use bash syntax.

**Prefer MSI over EXE.** MSI is the standard Windows installer format, supports silent install (`/quiet`), and is more appropriate for developer tools. EXE (NSIS-based via jpackage) requires no extra tooling but is less scriptable.

### Windows Signing (OPTIONAL for v0.4.0)

Windows code signing requires an EV (Extended Validation) code signing certificate from a CA (Sectigo, DigiCert, etc.), which costs ~$300+/year and requires hardware token or cloud HSM. This is out of scope for v0.4.0. The MSI will trigger a SmartScreen warning on first run, which is acceptable for an internal developer tool. Document this in the release notes.

---

## Release Notes Generation

### Recommended Approach: GitHub's Built-in Auto-Generation

Use `generate_release_notes: true` on `softprops/action-gh-release`. GitHub compares the current tag to the previous tag and generates a categorized changelog from PR titles and commit messages.

```yaml
- uses: softprops/action-gh-release@v2
  with:
    generate_release_notes: true
    files: |
      XSLEditor-*.dmg
      XSLEditor-*.msi
      XSLEditor-*.jar
```

This requires no extra tooling, no configuration files, and produces a reasonable result immediately. The generated notes include: merged PRs, contributors, and a "Full Changelog" link.

**Alternative if more control is needed:** `git log --oneline <prev-tag>..<current-tag>` piped into the release body. This is a one-liner in the workflow, no extra action required. Use this if GitHub's auto-generation produces noise.

Do NOT add `mikepenz/release-changelog-builder-action` or similar third-party tools — over-engineering for this project.

---

## Version Extraction in Workflow

The version number needs to be extracted from the git tag to pass to jpackage. The tag format is `v0.4.0`; jpackage expects `0.4.0`.

```yaml
- name: Extract version
  id: version
  run: echo "VERSION=${GITHUB_REF_NAME#v}" >> $GITHUB_OUTPUT
```

Then use `${{ steps.version.outputs.VERSION }}` in subsequent steps. This works on all three platforms (bash is available via `shell: bash`).

---

## build.gradle Changes Required

Two changes needed:

**1. Update mainClass for jpackage Launcher:**

```groovy
application {
    mainClass = 'ch.ti.gagi.xsleditor.Launcher'  // was: XSLEditorApp
}

shadowJar {
    archiveClassifier.set('')
    manifest {
        attributes 'Main-Class': 'ch.ti.gagi.xsleditor.Launcher'  // was: XSLEditorApp
    }
    mergeServiceFiles()
}
```

**2. Update version:**

```groovy
version = '0.4.0'  // was: '0.3.0'
```

No other build.gradle changes needed. The shadow plugin already produces the correct fat JAR. No new Gradle plugins are required for CI/CD — jpackage is invoked directly as a CLI tool in the workflow.

---

## What NOT to Add

| Tool / Approach | Why Not |
|-----------------|---------|
| `badass-runtime-plugin` (Gradle) | Unnecessary; jpackage CLI invocation directly in workflow is simpler and more transparent |
| `jlink` manual step | jpackage calls jlink internally; no manual step needed for fat JAR approach |
| `wix-actions/setup-wix` | WiX 3 is preinstalled on windows-latest; no setup step needed |
| `semantic-release` | Over-engineering; simple tag push → release workflow is sufficient |
| `mikepenz/release-changelog-builder-action` | GitHub's built-in `generate_release_notes: true` is sufficient |
| `gradle/actions/setup-gradle` | `actions/setup-java@v5` with `cache: gradle` covers this; the dedicated Gradle action is only needed for advanced use cases |
| Windows EV code signing | Cost (~$300+/year), hardware HSM requirements — out of scope for v0.4.0 |
| macOS `.pkg` installer | `.dmg` is simpler and standard for developer tools; pkg adds complexity with no benefit |
| `NSIS` EXE type via jpackage | MSI is preferred; NSIS EXE provides no advantage for this project |

---

## macOS Runner Note

`macos-latest` on GitHub-hosted runners is macOS 15 (Sequoia) as of early 2026. It includes:
- Xcode 16+ (provides `xcrun`, `codesign`, `notarytool`, `stapler`)
- JDK is NOT preinstalled at the version we need — `actions/setup-java@v5` handles this

The `macos-latest` runner is ARM (Apple Silicon M1). JavaFX 21 from Temurin includes both x86_64 and aarch64 fat binaries — the build will produce an ARM `.app`. If x86_64 compatibility is needed, use `macos-13` (Intel runner) for that job. For v0.4.0, ARM-only is acceptable.

---

## Sources

- [actions/setup-java — GitHub](https://github.com/actions/setup-java): v5 confirmed current major — MEDIUM confidence (WebSearch)
- [actions/upload-artifact — GitHub](https://github.com/actions/upload-artifact): v4 stable, v3 deprecated Jan 2025 — HIGH confidence (official changelog)
- [softprops/action-gh-release — GitHub](https://github.com/softprops/action-gh-release): v2.6.2 last stable Node 20 line — MEDIUM confidence (WebSearch)
- [apple-actions/import-codesign-certs — GitHub](https://github.com/Apple-Actions/import-codesign-certs): v3 confirmed (v6.0.0 is latest tag, action tag v3 still current for usage) — MEDIUM confidence (WebSearch)
- [foojay.io: Creating Executables For JavaFX Applications](https://foojay.io/today/creating-executables-for-javafx-applications/): jpackage fat JAR + Launcher pattern — MEDIUM confidence
- [inside.java: Package a JavaFX Application as a Platform Specific Executable](https://inside.java/2023/11/14/package-javafx-native-exec/): Oracle official, 2023 — HIGH confidence
- [WiX Toolset discussions — jpackage WiX 4 incompatibility](https://github.com/orgs/wixtoolset/discussions/7982): WiX 3 requirement confirmed, WiX 4 breaks jpackage — MEDIUM confidence
- [federicoterzi.com: Automatic Code-signing and Notarization for macOS using GitHub Actions](https://federicoterzi.com/blog/automatic-code-signing-and-notarization-for-macos-apps-using-github-actions/): Full signing workflow pattern — MEDIUM confidence
- [GitHub Actions community: WiX preinstalled on windows-latest](https://github.com/orgs/community/discussions/27149): WiX 3 on PATH confirmed — MEDIUM confidence
