# Domain Pitfalls — v0.4.0 GitHub Actions CI/CD Distribution

**Project:** XSLEditor
**Researched:** 2026-04-24
**Scope:** GitHub Actions workflow, jpackage, macOS codesigning/notarization,
Windows MSI via WiX, fat JAR (shadowJar) compatibility, release automation.

**Project facts that affect every pitfall:**
- Build: Gradle 9 + `com.gradleup.shadow` 9.0.0-beta12 (fat JAR)
- Main class: `ch.ti.gagi.xsleditor.XSLEditorApp` (extends `javafx.application.Application`)
- JavaFX modules used: `javafx.controls`, `javafx.fxml`, `javafx.web`
- Target: macOS `.app` + Windows `.msi`/`.exe`, both with embedded JRE via jpackage

---

## Critical Pitfalls

Mistakes that block the release or produce a broken installer.

---

### Pitfall C-01: jpackage rejects a fat JAR containing JavaFX when the main class extends `Application`

**Severity:** BLOCKS RELEASE

**What goes wrong:** jpackage is invoked with `--main-jar xsleditor.jar` (the
shadowJar fat JAR). The produced bundle launches, but the JVM immediately
aborts with:

```
Error: JavaFX runtime components are missing, and are required to run
this application
```

**Why it happens:** Since Java 11 the JVM startup check inspects whether the
main class extends `javafx.application.Application`. If it does, the check
requires `javafx.graphics` to be a *named module* on the module path — not a
plain class on the classpath. A shadowJar merges JavaFX JARs into an
anonymous uber-JAR; the merged JAR is on the classpath, not the module path,
so the named-module check fails.

**This project's exact exposure:** `XSLEditorApp extends Application` and the
fat JAR is the artifact that jpackage would receive if wired naively.

**Prevention — two viable strategies:**

**Option A (Launcher class — minimal change):** Create a thin
`ch.ti.gagi.xsleditor.Launcher` class whose `main()` simply calls
`XSLEditorApp.main(args)`. `Launcher` does *not* extend `Application`, so the
startup check is skipped. Point both the fat JAR manifest and jpackage at
`Launcher`. This is the lowest-friction fix for an existing project.

```java
// src/main/java/ch/ti/gagi/xsleditor/Launcher.java
public class Launcher {
    public static void main(String[] args) {
        XSLEditorApp.main(args);
    }
}
```

In `build.gradle`, update `mainClass` and the `shadowJar` manifest to
`ch.ti.gagi.xsleditor.Launcher`.

**Option B (JDK+FX distribution):** In the GitHub Actions workflow, install a
JDK that bundles JavaFX (e.g. Liberica JDK with `java-package: jdk+fx` in
`actions/setup-java`). JavaFX modules then live on the module path of the
embedded JRE that jpackage bundles, and the named-module requirement is met
without a launcher shim.

Option B requires a JavaFX-bundled JDK on *every* OS runner, which is a
harder dependency to pin and maintain. Option A is recommended.

**Detection:** Run the produced `.app` / `.exe` from a terminal; the JVM abort
message appears on stderr before any window opens.

**Phase:** Must be resolved in the first CI/CD phase before any distribution
artifact is tested.

---

### Pitfall C-02: jpackage does not bundle native JavaFX WebKit libraries (`javafx.web`)

**Severity:** BLOCKS RELEASE (app crashes at runtime on user machines)

**What goes wrong:** The app launches but crashes when the `WebView` PDF
preview tab is opened. Stacktrace contains
`java.lang.UnsatisfiedLinkError: libjfxwebkit.dylib` (macOS) or a similar
native library error on Windows.

**Why it happens:** `javafx.web` depends on a native library (`libjfxwebkit`)
that is NOT included in a standard OpenJDK JRE. jpackage copies the JRE that
is installed on the build runner. If that JRE is vanilla OpenJDK (no JavaFX),
the native library is absent from the embedded runtime.

The same issue affects `javafx.media` if it were used.

**Prevention:** The GitHub Actions workflow must install a JDK distribution
that ships JavaFX native libraries. Specifically:

- **Liberica JDK:** `distribution: 'liberica'`, `java-package: 'jdk+fx'` in
  `actions/setup-java`. This is the most reliable option in CI.
- **Azul Zulu JDK+FX:** `distribution: 'zulu'`, `java-package: 'jdk+fx'`.

Both distributions include the `libjfxwebkit` native library in the JRE.
jpackage will then copy it into the bundle automatically.

Do NOT use `temurin` (Eclipse Adoptium), `corretto`, or `microsoft`
distributions — they do not bundle JavaFX native libraries.

**Detection:** After producing the bundle, inspect its contents:
- macOS: `find XSLEditor.app -name "libjfxwebkit*"` — must return a result.
- Windows: `dir /s libjfxwebkit.dll` inside the install directory.

**Phase:** Workflow JDK setup step (Phase 1 of CI/CD). Must be verified before
moving to signing.

---

### Pitfall C-03: macOS codesign fails silently or prompts for password in headless CI

**Severity:** BLOCKS RELEASE

**What goes wrong:** The signing step in GitHub Actions exits with code 0 but
the resulting `.app` or `.dmg` is not signed. Or the step hangs indefinitely
waiting for a keychain password prompt that never appears.

**Why it happens (two sub-issues):**

*Sub-issue A — missing partition list:* Since macOS 10.12.5, a keychain item
requires its signing identity to be in the keychain's partition list (ACL) for
`/usr/bin/codesign` to access it without a UI password prompt. If
`security set-key-partition-list` is not called after the certificate import,
`codesign` blocks on a UI dialogue that never resolves in headless CI.

*Sub-issue B — process substitution broken in newer macOS runners:* GitHub
Actions macOS runners changed behavior around August 2023. Using process
substitution (`security import <(base64 --decode <<< "$CERT")`) to pipe the
decoded certificate fails silently. The import command exits 0 but the
certificate is not actually imported.

**Prevention (complete keychain setup sequence):**

```yaml
- name: Import signing certificate
  env:
    MACOS_CERTIFICATE: ${{ secrets.MACOS_CERTIFICATE_P12_BASE64 }}
    MACOS_CERTIFICATE_PWD: ${{ secrets.MACOS_CERTIFICATE_PASSWORD }}
  run: |
    # Write cert to a real file — process substitution is broken on new runners
    echo "$MACOS_CERTIFICATE" | base64 --decode > /tmp/certificate.p12

    # Create a dedicated temporary keychain
    security create-keychain -p "ci-keychain-password" build.keychain
    security default-keychain -s build.keychain
    security unlock-keychain -p "ci-keychain-password" build.keychain

    # Import certificate
    security import /tmp/certificate.p12 \
      -k build.keychain \
      -P "$MACOS_CERTIFICATE_PWD" \
      -T /usr/bin/codesign \
      -T /usr/bin/productbuild

    # Critical: set partition list so codesign can access without UI prompt
    security set-key-partition-list \
      -S apple-tool:,apple: \
      -s \
      -k "ci-keychain-password" \
      build.keychain

    # Clean up cert file
    rm /tmp/certificate.p12
```

**Detection:** After the signing step, verify with:
```bash
codesign --verify --deep --strict --verbose=2 XSLEditor.app
spctl --assess --type exec XSLEditor.app
```
Any output containing "code object is not signed" or "CSSMERR_TP_NOT_TRUSTED"
means the signing did not take effect.

**Phase:** macOS signing phase. The partition-list step is mandatory — omitting
it is the single most common CI signing failure.

---

### Pitfall C-04: macOS notarization requires `xcrun notarytool`, not `altool`

**Severity:** BLOCKS RELEASE

**What goes wrong:** The workflow uses `xcrun altool --notarize-app ...` and
fails with an error about the tool being discontinued.

**Why it happens:** Apple deprecated `altool` for notarization in November
2023. It no longer accepts submission requests. All new workflows must use
`xcrun notarytool`.

**Prevention:**

```yaml
- name: Notarize app
  env:
    APPLE_ID: ${{ secrets.APPLE_ID }}
    APPLE_TEAM_ID: ${{ secrets.APPLE_TEAM_ID }}
    APPLE_APP_SPECIFIC_PASSWORD: ${{ secrets.APPLE_APP_SPECIFIC_PASSWORD }}
  run: |
    xcrun notarytool submit XSLEditor.dmg \
      --apple-id "$APPLE_ID" \
      --team-id "$APPLE_TEAM_ID" \
      --password "$APPLE_APP_SPECIFIC_PASSWORD" \
      --wait
    xcrun stapler staple XSLEditor.dmg
```

The `--wait` flag polls until Apple's notarization service responds. Without
`stapler staple`, the notarization ticket is not attached to the artifact;
users on machines without internet access cannot open the app.

**Required GitHub Actions secrets:**
- `MACOS_CERTIFICATE_P12_BASE64` — base64-encoded `.p12` file
- `MACOS_CERTIFICATE_PASSWORD` — `.p12` export password
- `MACOS_CERTIFICATE_NAME` — full identity string, e.g.
  `"Developer ID Application: Your Name (XXXXXXXXXX)"`
- `APPLE_ID` — Apple Developer account email
- `APPLE_TEAM_ID` — 10-character Team ID from Apple Developer portal
- `APPLE_APP_SPECIFIC_PASSWORD` — app-specific password from appleid.apple.com

**Phase:** macOS signing/notarization phase. Never use `altool` in new workflows.

---

### Pitfall C-05: jpackage + JavaFX requires hardened runtime entitlements for notarization

**Severity:** BLOCKS RELEASE (notarization rejection)

**What goes wrong:** Notarization is submitted but Apple rejects it with an
error about hardened runtime not being enabled, or the app crashes at launch
with a code signing error because JVM memory tricks are blocked.

**Why it happens:** Apple requires the hardened runtime for notarized apps.
However, the JVM requires several entitlements that the hardened runtime
disables by default:
- JIT compilation (`cs.allow-jit`)
- Unsigned executable memory (`cs.allow-unsigned-executable-memory`)
- DYLD environment variables (`cs.allow-dyld-environment-variables`)
- Library validation bypass (`cs.disable-library-validation`) — required for
  JavaFX native libraries loaded at runtime

Without `cs.disable-library-validation`, JavaFX's dynamic loading of
`libjfxwebkit.dylib` is blocked by hardened runtime library validation.

**Prevention:** Provide an entitlements file and pass it to jpackage:

```xml
<!-- entitlements.plist -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.security.cs.allow-jit</key>              <true/>
    <key>com.apple.security.cs.allow-unsigned-executable-memory</key> <true/>
    <key>com.apple.security.cs.allow-dyld-environment-variables</key> <true/>
    <key>com.apple.security.cs.disable-library-validation</key>       <true/>
</dict>
</plist>
```

Pass to jpackage:
```
--mac-entitlements entitlements.plist
--mac-sign
--mac-signing-key-user-name "Developer ID Application: Your Name (XXXXXXXXXX)"
```

**Detection:** Submit the app to notarization and check the log with
`xcrun notarytool log <submission-id>`. Library validation failures appear as
explicit rejection reasons.

**Phase:** macOS signing phase. Create the entitlements file before invoking
jpackage.

---

### Pitfall C-06: WiX version mismatch blocks jpackage MSI creation on Windows runner

**Severity:** BLOCKS RELEASE (Windows artifacts cannot be produced)

**What goes wrong:** The GitHub Actions Windows runner step that calls jpackage
fails with a WiX-related error such as:
```
WiX 3.x is required to create a Windows installer
```
or the step succeeds with WiX but produces no MSI file.

**Why it happens:** jpackage requires WiX 3.x (specifically WiX 3.0+) to
build MSI installers. The GitHub Actions `windows-latest` runner ships with
WiX pre-installed, but the version on the runner may drift, and there is a
known incompatibility between jpackage and WiX 4/5 — jpackage on JDK 21 does
not support WiX 4 or 5.

**Prevention:**

1. Pin the runner image: use `windows-2022` instead of `windows-latest` to
   reduce runner image drift.
2. Add a step to verify the WiX version before jpackage:
   ```yaml
   - name: Verify WiX
     run: |
       & "C:\Program Files (x86)\WiX Toolset v3.11\bin\candle.exe" -?
   ```
3. If WiX 3.x is absent, install it explicitly:
   ```yaml
   - name: Install WiX 3
     run: choco install wixtoolset --version=3.11.2 -y
   ```
4. After installation, ensure WiX is on `PATH`:
   ```yaml
   - name: Add WiX to PATH
     run: |
       echo "C:\Program Files (x86)\WiX Toolset v3.11\bin" | Out-File -Append -FilePath $env:GITHUB_PATH
   ```

**Detection:** jpackage's stderr will mention WiX explicitly if the failure
is WiX-related. If the step exits 0 but no MSI is created, check for a missing
WiX candle/light in the PATH.

**Phase:** Windows packaging phase.

---

## Moderate Pitfalls

Mistakes that cause incorrect behavior but can be worked around without a
full rewrite.

---

### Pitfall M-01: Cross-platform builds must run on their native OS runner

**Severity:** CAUSES INCORRECT ARTIFACTS

**What goes wrong:** A single Linux or macOS runner is used to produce both
the macOS `.app` and the Windows `.msi`. jpackage produces an artifact that
looks correct but either will not install or will not run on the target OS.

**Why it happens:** jpackage does NOT support cross-compilation. A macOS
jpackage invocation can only produce macOS artifacts; a Windows invocation can
only produce Windows artifacts. The JRE embedded by jpackage is the one
installed on the build runner — an x86_64 Linux JRE embedded into a Windows
installer produces a non-functional installer.

**Prevention:** The workflow matrix must specify separate jobs:

```yaml
jobs:
  build-macos:
    runs-on: macos-14          # Apple Silicon runner (arm64)
    steps: [...]               # produces .dmg

  build-windows:
    runs-on: windows-2022
    steps: [...]               # produces .msi

  publish:
    needs: [build-macos, build-windows]
    runs-on: ubuntu-latest
    steps: [upload all artifacts to GitHub Release]
```

**Note on macOS runner architecture:** `macos-14` is arm64 (Apple Silicon).
`macos-13` is x86_64 (Intel). If only one architecture is targeted, pick the
one matching most of your users. There is no jpackage universal binary support;
a separate job would be needed for the other architecture.

**Phase:** Workflow design phase (Phase 1). Get the matrix right before any
signing work.

---

### Pitfall M-02: `shadowJar mergeServiceFiles()` may silently drop Saxon or FOP service registrations

**Severity:** CAUSES RUNTIME ERRORS IN PRODUCTION BUILD

**What goes wrong:** The shadow fat JAR works correctly in development
(`./gradlew run`) but the jpackage-bundled app fails to find Saxon's
`TransformerFactory` or FOP's `DocumentBuilderFactory` at runtime. Error
messages resemble:
```
javax.xml.transform.TransformerFactoryConfigurationError:
  Provider net.sf.saxon.TransformerFactoryImpl not found
```

**Why it happens:** Saxon and Apache FOP both use Java's `ServiceLoader`
mechanism, registering implementations in `META-INF/services/` files. When
multiple JARs contribute to the same service file, `mergeServiceFiles()` in
shadow concatenates them. However, if `DuplicatesStrategy.EXCLUDE` is active
elsewhere in the build, service files may be dropped instead of merged.

The project's `build.gradle` already calls `mergeServiceFiles()`, which is
correct, but this must be verified to cover all service files from Saxon 12.4
and FOP 2.9.

**Prevention:**
1. After building the fat JAR, verify that key service files are present and
   correctly merged:
   ```bash
   jar tf build/libs/xsleditor-*.jar | grep META-INF/services
   jar xf build/libs/xsleditor-*.jar META-INF/services/javax.xml.transform.TransformerFactory
   cat META-INF/services/javax.xml.transform.TransformerFactory
   # must contain: net.sf.saxon.TransformerFactoryImpl
   ```
2. Do not set `duplicatesStrategy = DuplicatesStrategy.EXCLUDE` on the
   `shadowJar` task — this overrides `mergeServiceFiles()`.

**Detection:** Run a render cycle from the jpackage-produced bundle against a
known-good project. If the render fails with a provider error, this pitfall is
the cause.

**Phase:** JAR build verification step. Check before submitting the JAR to
jpackage.

---

### Pitfall M-03: Git tag format assumptions break release notes generation

**Severity:** CAUSES EMPTY OR INCORRECT RELEASE NOTES

**What goes wrong:** The workflow uses `git log v0.3.0..v0.4.0 --oneline` to
generate release notes, but:
- No previous tag `v0.3.0` exists (first CI-automated release after manual
  tagging history), so git log produces all commits since the beginning.
- The tag name contains a non-semver suffix (e.g. `v0.4.0-rc1`) that the
  release notes script does not handle.
- `${{ github.ref_name }}` strips `refs/tags/` automatically (correct), but
  the script that derives the *previous* tag uses `git describe --abbrev=0
  --tags HEAD^` which may land on a non-release tag or annotation-vs-lightweight
  tag mismatch.

**Prevention:**
1. Use `git tag --sort=-version:refname` to list tags and derive the previous
   semver tag programmatically.
2. Guard for the "no previous tag" case (first release):
   ```bash
   PREV_TAG=$(git tag --sort=-version:refname | grep -E '^v[0-9]+\.[0-9]+' | sed -n '2p')
   if [ -z "$PREV_TAG" ]; then
     git log --oneline > release-notes.txt
   else
     git log "${PREV_TAG}..HEAD" --oneline > release-notes.txt
   fi
   ```
3. Use `github.ref_name` (not `github.ref`) for the current tag — GitHub
   Actions strips the `refs/tags/` prefix automatically.

**Phase:** Release publish step.

---

### Pitfall M-04: Multiline `git log` output breaks `GITHUB_OUTPUT` variable assignment

**Severity:** CAUSES BROKEN RELEASE NOTES IN GITHUB UI

**What goes wrong:** A workflow step assigns release notes to a GitHub Actions
output variable using the old `::set-output` syntax, or uses a single-line
`echo "notes=$(git log ...)"` assignment. Multi-line commit messages or commit
messages containing special characters (`%`, `\n`, `:`) corrupt the variable.

**Why it happens:** `GITHUB_OUTPUT` requires multi-line values to use the
heredoc delimiter syntax. The old `::set-output` mechanism was deprecated and
removed.

**Prevention:** Use the heredoc form for any multi-line output:

```yaml
- name: Generate release notes
  id: notes
  run: |
    NOTES=$(git log "${PREV_TAG}..HEAD" --pretty=format:"- %s")
    echo "release_notes<<EOF" >> $GITHUB_OUTPUT
    echo "$NOTES" >> $GITHUB_OUTPUT
    echo "EOF" >> $GITHUB_OUTPUT
```

Alternatively, write the notes to a file and pass `--notes-file` to
`gh release create`, which avoids the environment variable entirely.

**Phase:** Release publish step. Use file-based approach as the safer default.

---

### Pitfall M-05: `actions/setup-java` with `distribution: liberica` requires `java-package` to be set explicitly

**Severity:** CAUSES MISSING JAVAFX NATIVE LIBRARIES (leads to C-02)

**What goes wrong:** The workflow uses `distribution: 'liberica'` but omits
`java-package`, defaulting to `jdk` (no JavaFX). The JDK installed is the
standard Liberica JDK without bundled JavaFX native libraries. jpackage bundles
this JRE, and the app crashes when `WebView` is initialized.

**Prevention:** Always specify both fields together:

```yaml
- uses: actions/setup-java@v4
  with:
    distribution: 'liberica'
    java-version: '21'
    java-package: 'jdk+fx'    # critical — without this, JavaFX natives are absent
```

**Detection:** Same as Pitfall C-02 — inspect the bundle for
`libjfxwebkit.dylib` / `libjfxwebkit.dll`.

**Phase:** Workflow JDK setup step (all OS runners).

---

## Minor Pitfalls

Issues that cause friction but can be fixed quickly once discovered.

---

### Pitfall N-01: `build.gradle` version not bumped before tagging

**What goes wrong:** The GitHub Release says `v0.4.0` but the embedded
`version.properties` and the JAR filename still read `0.3.0` because
`build.gradle`'s `version` property was not updated before the tag was pushed.

**Prevention:** The release workflow should fail fast if the build version does
not match the tag. Add a verification step:

```yaml
- name: Verify version matches tag
  run: |
    BUILD_VERSION=$(./gradlew properties -q | grep "^version:" | awk '{print $2}')
    TAG_VERSION="${{ github.ref_name }}"  # e.g. v0.4.0
    if [ "v$BUILD_VERSION" != "$TAG_VERSION" ]; then
      echo "ERROR: build.gradle version '$BUILD_VERSION' != tag '$TAG_VERSION'"
      exit 1
    fi
```

**Phase:** Pre-build gate step. Runs before any compilation or packaging.

---

### Pitfall N-02: macOS `.app` without a DMG is awkward for users to install

**What goes wrong:** jpackage's `--type app-image` produces a `.app` directory,
not a file. GitHub Releases requires a single file upload; uploading a directory
fails. Even if zipped, users do not know to drag it to `/Applications`.

**Prevention:** Use `--type dmg` for macOS distribution. jpackage handles DMG
creation natively and produces a single distributable file. The DMG includes a
drag-to-Applications UI by default.

**Phase:** jpackage invocation step.

---

### Pitfall N-03: Release artifacts uploaded by separate jobs need a coordinating publish job

**What goes wrong:** Both the macOS and Windows jobs call `gh release create`
independently. One job creates the release with only its artifact; the other
job's `gh release create` fails because the release already exists.

**Prevention:** Use a two-stage approach:
1. macOS and Windows jobs each upload their artifact as a workflow artifact
   (`actions/upload-artifact`).
2. A final `publish` job with `needs: [build-macos, build-windows]` downloads
   all artifacts and creates the GitHub Release in a single step using
   `gh release create` or `softprops/action-gh-release`.

**Phase:** Release publish step. Design this into the workflow from the start.

---

### Pitfall N-04: GitHub Actions `contents: write` permission required for release creation

**What goes wrong:** The `gh release create` step fails with `403 Resource not
accessible by integration`.

**Why it happens:** The default `GITHUB_TOKEN` permissions for a workflow do
not include write access to contents (releases). This must be explicitly granted.

**Prevention:** Add permissions at the job level:

```yaml
jobs:
  publish:
    permissions:
      contents: write
```

**Phase:** Release publish job setup.

---

### Pitfall N-05: jpackage app name and identifier must be consistent across signing, notarization, and the bundle

**What goes wrong:** The app is signed with one name (`--name XSLEditor`) but
the notarization submission refers to a different bundle identifier, causing
Apple to reject the notarization or staple to fail.

**Prevention:** Set `--name`, `--app-version`, and `--mac-package-identifier`
consistently in every jpackage invocation. Keep a single source of truth (the
Gradle `version` property for the version; a fixed string for the identifier,
e.g. `ch.ti.gagi.xsleditor`).

**Phase:** jpackage invocation step.

---

## Phase-Specific Warnings

| CI/CD Phase | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Workflow design | Cross-platform jobs share a single runner (M-01) | Use OS matrix with native runners |
| JDK setup (all runners) | Missing JavaFX natives (C-02, M-05) | Use `liberica` + `jdk+fx` |
| Fat JAR preparation | Launcher shim missing, app aborts with module error (C-01) | Add `Launcher` class; point manifest at it |
| Fat JAR preparation | Service file merge incomplete (M-02) | Verify `META-INF/services` after build |
| macOS jpackage | Entitlements file missing (C-05) | Create `entitlements.plist` before jpackage |
| macOS signing | Keychain partition list not set — codesign hangs (C-03) | Run `security set-key-partition-list` |
| macOS notarization | altool used instead of notarytool (C-04) | Use `xcrun notarytool submit` |
| macOS notarization | Staple step omitted (C-04) | Always run `xcrun stapler staple` after submit |
| Windows packaging | WiX version mismatch (C-06) | Pin `windows-2022`; verify WiX 3.x on PATH |
| Release publish | Multiple jobs both try `gh release create` (N-03) | Use dedicated `publish` job with `needs:` |
| Release publish | Missing `contents: write` permission (N-04) | Add `permissions: contents: write` |
| Release publish | Multiline notes break `GITHUB_OUTPUT` (M-04) | Use heredoc or `--notes-file` |
| Pre-build gate | Version mismatch between tag and build.gradle (N-01) | Add version-check step before compilation |

---

## Sources

- Apple documentation on notarization workflow: https://developer.apple.com/documentation/security/customizing-the-notarization-workflow
- OpenJDK bug — jpackage macOS architecture tagging: https://bugs.openjdk.org/browse/JDK-8266179
- GitHub discussion — macOS security import broken with process substitution: https://github.com/orgs/community/discussions/63731
- GitHub discussion — WiX path setup in Windows runner: https://github.com/orgs/community/discussions/27149
- adoptium-support — WiX 5 incompatibility with jpackage: https://github.com/adoptium/adoptium-support/issues/1262
- shadow plugin mergeServiceFiles issue — DuplicatesStrategy interaction: https://github.com/GradleUp/shadow/issues/1348
- GradleUp shadow plugin — mergeServiceFiles regression in 9.0.0: https://github.com/GradleUp/shadow/issues/1599
- actions/setup-java — java-package options including jdk+fx: https://github.com/actions/setup-java
- Apple-Actions/import-codesign-certs — multiple cert import limitation: https://github.com/Apple-Actions/import-codesign-certs
- macOS signing keychain partition list guide: https://localazy.com/blog/how-to-automatically-sign-macos-apps-using-github-actions
- macOS jpackage signing issues JDK bug: https://bugs.openjdk.org/browse/JDK-8358723
- Eden Coding — JavaFX runtime components missing root cause: https://edencoding.com/runtime-components-error/
