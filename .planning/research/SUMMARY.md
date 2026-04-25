# Research Summary — v0.4.0 GitHub Releases & Distribution

**Project:** XSLEditor
**Milestone:** v0.4.0 — GitHub Actions CI/CD & Native Distribution
**Synthesized:** 2026-04-24
**Confidence:** HIGH

---

## Executive Summary

- v0.4.0 adds a full cross-platform release pipeline: tagged commits on `v*` trigger GitHub Actions jobs that produce a signed/notarized macOS DMG, an unsigned Windows MSI, and a fat JAR, all attached to a single GitHub Release with auto-generated release notes.
- The existing Gradle/Shadow/JavaFX stack is unchanged. The only code change required before CI works is a 5-line `Launcher.java` shim and a one-line `shadowJar` manifest update — both mandatory because `XSLEditorApp extends Application` causes a fatal JVM startup error when launched from a classpath fat JAR.
- The project uses `javafx.web` (WebView for PDF preview), which means the CI JDK must be **Liberica JDK+FX** (`distribution: liberica`, `java-package: jdk+fx`) on all runners — standard Temurin/Adoptium omits the native `libjfxwebkit` library that jpackage would bundle into the installer.
- macOS signing + notarization is in scope for v0.4.0 and requires 7 repository secrets, an `entitlements.plist` file, and a precise keychain setup sequence including `security set-key-partition-list`. Windows code signing is explicitly deferred.
- Each full release consumes approximately 214 billed GitHub Actions minutes (2× macOS arm64+x64 at the x10 multiplier). At the free-tier limit of 2,000 min/month this allows roughly 9 releases per month.

---

## Stack Additions

| Layer | Addition | Why |
|-------|----------|-----|
| CI runtime — all runners | Liberica JDK+FX 21 (`distribution: liberica`, `java-package: jdk+fx`) | Only distribution that ships `libjfxwebkit` native lib; required for WebView |
| Packaging | jpackage (bundled with JDK 14+, no install needed) | Produces native DMG/MSI with embedded JRE; invoked as shell command, not Gradle plugin |
| Workflow — releases | `softprops/action-gh-release@v2` | Creates GitHub Release and attaches multi-file assets; v2 is stable Node 20 line |
| Workflow — artifacts | `actions/upload-artifact@v4` + `actions/download-artifact@v4` | v3 deprecated Jan 2025 |
| Source file | `Launcher.java` (5 lines) | Shim bypassing JavaFX module-path startup check — mandatory |
| Config file | `entitlements.plist` | Required for JVM + JavaFX under macOS hardened runtime |
| Workflow file | `.github/workflows/release.yml` | Single file with all 5 jobs |
| Docs | `docs/SIGNING.md` | Secrets setup guide for maintainers |

Not added: badass-runtime-plugin, jlink, wix-actions, semantic-release, auto-update, Linux packages.

---

## Feature Table Stakes

| Deliverable | Key constraint |
|-------------|----------------|
| Tag-triggered workflow (`on: push: tags: ['v*']`) | Single trigger for the entire pipeline |
| Fat JAR built on `ubuntu-latest` | `./gradlew shadowJar -Pversion=X.Y.Z` |
| macOS arm64 DMG — signed + notarized + stapled | `macos-15` runner; Liberica JDK+FX; 7 secrets; entitlements.plist |
| macOS x64 DMG — signed + notarized + stapled | `macos-15-intel` runner (available until Aug 2027) |
| Windows MSI — unsigned | `windows-latest`; WiX 3 preinstalled; no signing secrets |
| All assets on one GitHub Release | `release` job `needs: [build-jar, build-macos-arm64, build-macos-x64, build-windows]` |
| Auto-generated release notes | `generate_release_notes: true` on `softprops/action-gh-release@v2` |
| `contents: write` permission on release job | Without it: 403 on release creation |

---

## Build Order

**Phase 1 — Launcher class + local build verification.**
Create `Launcher.java`, update `shadowJar` manifest, bump `version` to `0.4.0`. Verify fat JAR launches via `java -jar`. Mandatory prerequisite — nothing works without it.

**Phase 2 — CI skeleton (unsigned).**
Write `.github/workflows/release.yml` with the 5-job structure. Use Liberica JDK+FX on all runners. Produce unsigned DMG and MSI, upload artifacts, create GitHub Release. Push a `v0.4.0-test1` tag and confirm all artifacts appear.

**Phase 3 — macOS signing.**
Add the 7 secrets, create `entitlements.plist`, insert the full keychain setup sequence (including `security set-key-partition-list`), add `--mac-sign --mac-entitlements` to jpackage. Verify with `codesign --verify --deep --strict`.

**Phase 4 — macOS notarization.**
Add `xcrun notarytool submit --wait` and `xcrun stapler staple` steps after jpackage. Confirm the stapled DMG passes Gatekeeper on a real macOS machine. Requires Apple Developer Program ($99/yr).

**Phase 5 — Windows MSI polish + release docs.**
Add `--win-dir-chooser --win-menu --win-shortcut`. Pin `windows-2022` to avoid WiX runner drift. Write `docs/SIGNING.md`. Document SmartScreen warning in release notes.

**Phase 6 — Release job hardening.**
Add version-match gate (build version vs git tag). Set `prerelease` auto-detection on tags containing `-`. Add `contents: write` permission explicitly.

---

## Watch Out For — Top 5 Critical Pitfalls

| ID | Pitfall | One-line prevention |
|----|---------|---------------------|
| C-01 | **Launcher shim missing** — JVM aborts with "JavaFX runtime components are missing" | Create `Launcher.java`; point `shadowJar` manifest at `ch.ti.gagi.xsleditor.Launcher` |
| C-02 | **Liberica JDK+FX required** — standard Temurin omits `libjfxwebkit`; WebView crashes in bundle | Use `distribution: liberica` + `java-package: jdk+fx` on every OS runner |
| C-03 | **Keychain partition-list for headless signing** — `codesign` hangs or exits 0 with no signature | Always run `security set-key-partition-list -S apple-tool:,apple: -s -k <pwd> build.keychain` after cert import |
| C-04 | **`notarytool` not `altool`** — `altool` removed after Nov 2023 | Use `xcrun notarytool submit --wait` then `xcrun stapler staple` |
| C-05 | **`entitlements.plist` required for JVM hardened runtime** — Apple rejects notarization without JVM entitlements | Create `entitlements.plist` with `cs.allow-jit`, `cs.disable-library-validation`; pass via `--mac-entitlements` |

Also: C-06 — WiX 3 vs 4 on Windows runner: jpackage JDK 21 requires WiX 3 (`candle.exe`/`light.exe`); do not install WiX 4+.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Launcher class requirement | HIGH | JavaFX constraint; applies to this exact codebase |
| Liberica JDK+FX requirement | HIGH | Temurin exclusion is definitive for WebView |
| jpackage flags and invocation | HIGH | JDK 21 official docs validated |
| macOS signing sequence | HIGH | Keychain partition-list confirmed by multiple sources |
| `notarytool` vs `altool` | HIGH | Apple policy, unambiguous since Nov 2023 |
| `entitlements.plist` content | HIGH | Apple hardened runtime requirements well-documented |
| `softprops/action-gh-release@v2` | HIGH | Stable, widely used |
| Windows WiX 3 preinstalled | MEDIUM | Pin `windows-2022` as guard |
| macOS runner labels | HIGH | Confirmed from GitHub official docs 2026 |

**Overall: HIGH.**

**Gaps to watch:**
- Apple Developer Program membership ($99/yr) required before Phase 4 (notarization).
- Verify `shadowJar mergeServiceFiles()` covers Saxon 12.4 + FOP 2.9 service files (Pitfall M-02).
- macOS x64 runner (`macos-15-intel`) available until Aug 2027 only.

---

*Research completed: 2026-04-24*
*Ready for roadmap: yes*
