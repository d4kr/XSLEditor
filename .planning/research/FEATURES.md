# Feature Landscape: GitHub Actions CI/CD Distribution Pipeline

**Domain:** Java 21 + JavaFX desktop app distribution via GitHub Actions
**Project:** XSLEditor v0.4.0
**Researched:** 2026-04-24
**Confidence:** HIGH (verified against official docs, established ecosystem patterns, multiple sources)

---

## Table Stakes

Features every Java desktop release pipeline must have. Missing = pipeline is incomplete or users cannot install the app.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Workflow triggered by `v*` tag push | Standard trigger; releases are always tag-based | Low | `on: push: tags: ['v*']` — one-liner in workflow |
| Fat JAR as release asset | Lowest-friction artifact; works on any machine with Java 21 installed | Low | Already built via `shadowJar`; simply upload to release |
| macOS `.app` / `.dmg` via jpackage | macOS users expect a bundle they can drag to Applications, not a raw JAR | Medium | Requires `macos-latest` runner; JRE embedded; `--type dmg` is the standard delivery format |
| Windows `.exe` or `.msi` via jpackage | Windows users expect a native installer | Medium | Requires `windows-latest` runner; WiX 3 toolset required for MSI |
| Platform-specific runners (matrix or separate jobs) | jpackage cannot cross-compile — each platform must build on its own OS | Low-Medium | Matrix strategy or two named jobs; artifacts uploaded separately and merged into one release |
| Upload all artifacts to one GitHub Release | Users expect one release page listing all downloads | Low | `softprops/action-gh-release@v2` with `files:` glob; multi-job artifact merge via `actions/upload-artifact` + `actions/download-artifact` |
| macOS code signing with Apple Developer ID | Without signing, macOS Gatekeeper shows "unidentified developer" and blocks the app on first launch | High | p12 certificate + password stored as GitHub Secrets; passed to jpackage via `--mac-signing-key-user-name` |
| Secrets setup guide for maintainers | Contributors need to know which secrets to add for the signing pipeline to succeed | Low | Document: `MACOS_CERTIFICATE` (base64 p12), `MACOS_CERTIFICATE_PWD`, `MACOS_KEYCHAIN_PASSWORD`, and Apple ID credentials for notarization |

---

## Differentiators

Features that make this pipeline notably good but are not universally present in basic setups.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Auto-generated release notes (tag-to-tag) | Eliminates manual changelog writing; gives users a clear diff of what changed | Low | GitHub native: `generate_release_notes: true` in `softprops/action-gh-release`; zero-config baseline sufficient for internal tool |
| macOS notarization | Without notarization, macOS 10.15+ shows a blocking quarantine dialog on first launch for Developer ID distribution; notarization eliminates the dialog entirely | High | Requires Apple Developer Program ($99/yr); notarization via `notarytool`; ticket stapled to the .dmg; first submission can take up to 12 hours, subsequent ~10 minutes |
| Draft release with manual publish | Lets a developer review the release page and notes before it goes public | Low | `draft: true` in `softprops/action-gh-release`; human publishes manually via GitHub UI |
| Separate JAR-only job on Linux runner | Produces the fat JAR without consuming a macOS or Windows runner minute; faster and cheaper | Low | `ubuntu-latest` is the fastest/cheapest runner tier; Gradle `shadowJar` runs fine on Linux |
| Windows MSI (vs EXE) | MSI supports silent install and is the professional standard for developer tools on Windows | Medium | Requires WiX 3 toolset on the Windows runner (`choco install wixtoolset`); jpackage `--type msi` |
| Version injected from git tag | Single source of truth for the version string; no manual `build.gradle` edit needed | Low | `VERSION=$(git describe --tags --abbrev=0)` in workflow; passed to Gradle via `-Pversion=$VERSION` or `ORG_GRADLE_PROJECT_version` env var; the existing `version.properties` processResources mechanism picks it up |

---

## Anti-Features

Features that add complexity without value for an internal developer tool.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Auto-update / in-app update check | Internal tool — developers pull releases manually; auto-update requires a server, protocol, and signing trust chain (jDeploy, Sparkle, etc.) | Tag-based release is sufficient; developers are informed of releases via git/Slack |
| Homebrew cask publication | Useful for consumer tools; overkill for a closed internal tool without a public Homebrew tap | Keep distribution to GitHub Releases only |
| Linux `.deb` / `.rpm` / AppImage | XSLEditor targets macOS and Windows; no stated Linux user base | Omit; Linux users can run the fat JAR directly |
| Mac App Store submission | Requires App Store sandboxing; XSLT/FOP file I/O is incompatible with the App Store sandbox model | Not feasible; Developer ID signing is the correct distribution path |
| Windows EV code signing certificate | EV certs cost ~$300/yr; SmartScreen shows a warning on first run but does not block; controlled internal distribution makes this tolerable | Skip; document that SmartScreen may appear on first run for Windows |
| Automatic version-bump commits from CI | Requires CI to push commits back to the repo; complicates branch protection rules | Use the git tag itself as the version source |
| Test execution in release workflow | Tests already run on every commit/PR push; re-running on tag doubles build time with no new information | Keep test job on PR/push to main; release workflow assumes tests already passed |
| Separate staging vs production release environments | Internal tool shipped to known developers; no need for environment protection rules beyond draft review | One release workflow, one release step |

---

## Feature Dependencies

```
[Tag push trigger]
    |
    +--> [Linux job: shadowJar (ubuntu-latest)] ──────────────────────┐
    |                                                                   |
    +--> [macOS job: jpackage .dmg (macos-latest)]                    |
    |        |                                                          |
    |        +--> [Code signing: Apple Developer ID certificate]        |
    |                  |                                                +--> [Release job]
    |                  +--> [Notarization via notarytool] (optional)    |    needs: all 3 jobs
    |                            |                                      |    download-artifact
    |                            +--> [Staple ticket to .dmg]           |    softprops/action-gh-release
    |                                                                   |    generate_release_notes: true
    +--> [Windows job: jpackage .exe/.msi (windows-latest)] ─────────┘
             |
             +--> [WiX toolset installed on runner (for MSI)]

ORDERING CONSTRAINTS:
  1. Code signing must happen BEFORE notarization.
  2. Notarization must happen BEFORE DMG stapling.
  3. All platform jobs must complete BEFORE the release job runs.
  4. Release job declares: needs: [build-macos, build-windows, build-jar]
```

---

## MVP Recommendation

Build in this order:

**Phase 1 — Minimum working pipeline (ship this first):**
1. Tag-triggered workflow (`on: push: tags: ['v*']`)
2. Fat JAR built on Linux runner (`shadowJar`), uploaded as artifact
3. macOS `.app` or `.dmg` via jpackage on `macos-latest` — unsigned initially
4. Windows `.exe` via jpackage on `windows-latest`
5. Release job: download all artifacts, attach to GitHub Release
6. Auto-generated release notes (`generate_release_notes: true`)

**Phase 2 — Signing pass (after confirming the pipeline works end-to-end):**
7. macOS code signing with Apple Developer ID certificate (add secrets, add `--mac-signing-key-user-name` to jpackage call)
8. Secrets setup guide committed to repo (`.github/SIGNING.md` or `docs/signing.md`)

**Phase 3 — Notarization (optional, high effort, eliminates quarantine dialog):**
9. macOS notarization via `notarytool` + staple
10. Draft release mode for pre-publish review

**Defer indefinitely:**
- Auto-update, Homebrew tap, Linux packages, Windows EV signing, App Store

---

## Implementation Notes

### jpackage cannot cross-compile (HIGH confidence)
jpackage produces native installers only for the OS it runs on. A macOS `.app` can only be produced on a macOS runner. A Windows `.msi` requires a Windows runner. GitHub Actions solves this with separate jobs using `runs-on: macos-latest` and `runs-on: windows-latest`. Each job uploads its artifact with `actions/upload-artifact`; the release job downloads all of them with `actions/download-artifact` and attaches them in one `softprops/action-gh-release` call.

### macOS Gatekeeper without signing (HIGH confidence)
An unsigned `.app` distributed outside the Mac App Store triggers "Apple could not verify..." on first launch (macOS 10.15+). The user can bypass it via right-click > Open, but this is friction. For an internal tool used exclusively by developers, this is tolerable in Phase 1 but should be resolved in Phase 2.

### macOS notarization is separate from signing (HIGH confidence)
Signing with a Developer ID certificate establishes identity. Notarization means Apple's automated scanner has verified the binary is free of known malware. Both are required for zero-friction distribution. The tool is `notarytool` (successor to `altool`, deprecated 2023). First submission: up to 12 hours. Subsequent submissions: ~10 minutes. Requires a paid Apple Developer Program membership ($99/yr).

### Windows MSI requires WiX 3 (MEDIUM confidence)
`jpackage --type msi` on Windows requires WiX Toolset 3.x to be installed on the runner. Install it in the workflow step via `choco install wixtoolset`. WiX 4 is not compatible with jpackage as of JDK 21. Starting with `--type exe` (no WiX dependency) is a lower-friction starting point.

### Release notes: GitHub native is sufficient (HIGH confidence)
GitHub's built-in `generate_release_notes: true` (via `softprops/action-gh-release`) uses PR titles and labels between tags. For this internal tool with direct commits to main (no PR workflow), the notes will show raw commit messages. This is acceptable. If structured conventional commits are adopted, `mikepenz/release-changelog-builder-action` gives grouping; not needed for Phase 1.

### Version from git tag (MEDIUM confidence)
The existing `version.properties` mechanism (Gradle `processResources` with token expansion) already supports version injection. In the CI workflow: `VERSION=${GITHUB_REF_NAME}` (GitHub sets `GITHUB_REF_NAME` to the tag name on tag pushes), then pass `ORG_GRADLE_PROJECT_version=$VERSION` as an environment variable. Gradle picks this up automatically without modifying `build.gradle`.

---

## Sources

- [sualeh/build-jpackage — cross-platform jpackage GitHub Actions examples](https://github.com/sualeh/build-jpackage)
- [wiverson/maven-jpackage-template — macOS signing and notarization guide](https://github.com/wiverson/maven-jpackage-template/blob/main/docs/apple-sign-notarize.md)
- [Automatic Code-signing and Notarization for macOS via GitHub Actions — Federico Terzi](https://federicoterzi.com/blog/automatic-code-signing-and-notarization-for-macos-apps-using-github-actions/)
- [softprops/action-gh-release — canonical release action](https://github.com/softprops/action-gh-release)
- [GitHub: Automatically generated release notes](https://docs.github.com/en/repositories/releasing-projects-on-github/automatically-generated-release-notes)
- [Release Changelog Builder Action (marketplace)](https://github.com/marketplace/actions/release-changelog-builder)
- [How to Create a Release With Multiple Artifacts From GitHub Actions Matrix Strategy — Luca Cavallin](https://www.lucavall.in/blog/how-to-create-a-release-with-multiple-artifacts-from-a-github-actions-workflow-using-the-matrix-strategy)
- [How to Build macOS DMG and Windows MSI Installers with jpackage — Business Compass (2025)](https://blogs.businesscompassllc.com/2025/12/how-to-build-and-package-java.html)
- [Oracle jpackage Command Reference — JDK 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html)
- [indygreg/apple-code-sign-action — open source signing action](https://github.com/indygreg/apple-code-sign-action)
