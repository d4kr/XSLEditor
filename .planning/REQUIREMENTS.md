# Requirements: XSLEditor v0.4.0 — GitHub Releases & Distribution

**Milestone goal:** Automate build and publication of the app on GitHub Releases — JAR + signed/notarized macOS DMG + Windows MSI + Windows portable ZIP — triggered by a git tag push, with release notes generated automatically from git history.

---

## v0.4.0 Requirements

### Build Prerequisites

- [ ] **BUILD-01**: Developer can launch the app from the fat JAR via `java -jar` — `Launcher.java` shim added, `shadowJar` manifest updated to `ch.ti.gagi.xsleditor.Launcher`
- [ ] **BUILD-02**: Fat JAR embeds the correct version from the git tag (e.g. `0.4.0`) in `version.properties` — Gradle receives `-Pversion=X.Y.Z` from CI
- [ ] **BUILD-03**: Fat JAR passes Saxon/FOP service registration check (`META-INF/services/javax.xml.transform.TransformerFactory` present and correct) before being passed to jpackage
- [ ] **BUILD-04**: jpackage bundle includes `icon.png` — visible in macOS Dock and Windows taskbar

### CI Workflow

- [ ] **CI-01**: Workflow triggers automatically on `git push` of a tag matching `v*` and runs all build + release jobs end-to-end
- [ ] **CI-02**: All platform runners use Liberica JDK+FX 21 (`distribution: liberica`, `java-package: jdk+fx`) — required for WebView native library
- [ ] **CI-03**: Fat JAR built once on `ubuntu-latest` is downloaded and reused by all platform packaging jobs — no duplicate Gradle builds

### macOS Distribution

- [ ] **MACOS-01**: CI produces a DMG for Apple Silicon (arm64) via `macos-15` runner
- [ ] **MACOS-02**: CI produces a DMG for Intel x64 via `macos-15-intel` runner
- [ ] **MACOS-03**: Both DMGs are signed with a Developer ID Application certificate (`--mac-sign --mac-entitlements entitlements.plist`); `codesign --verify --deep --strict` passes
- [ ] **MACOS-04**: Both DMGs are notarized (`xcrun notarytool submit --wait`) and stapled (`xcrun stapler staple`); Gatekeeper accepts the app without quarantine dialog

### Windows Distribution

- [ ] **WIN-01**: CI produces a Windows MSI installer (unsigned) via `windows-latest` with `--win-dir-chooser --win-menu --win-shortcut`; installs and launches correctly
- [ ] **WIN-02**: CI produces a Windows portable ZIP (`--type app-image`) via `windows-latest` — no installer, no admin rights required; ZIP extracted and app launched correctly without installation

### GitHub Release

- [ ] **REL-01**: CI creates a GitHub Release on tag push, attaching all 5 assets: arm64 DMG, x64 DMG, MSI, Windows portable ZIP, fat JAR
- [ ] **REL-02**: GitHub Release includes auto-generated release notes from git log (tag-to-tag) via `generate_release_notes: true` on `softprops/action-gh-release@v2`
- [ ] **REL-03**: Tags containing `-` (e.g. `v0.4.0-beta1`) are automatically marked as pre-release; clean tags (e.g. `v0.4.0`) are marked as full release

### Documentation

- [ ] **SIGN-01**: `docs/SIGNING.md` documents how to export the Developer ID Application certificate as `.p12`, encode as base64, and configure all 7 required GitHub Actions secrets (`MACOS_CERTIFICATE`, `MACOS_CERTIFICATE_PASSWORD`, `MACOS_SIGNING_IDENTITY`, `MACOS_KEYCHAIN_PASSWORD`, `APPLE_ID`, `APPLE_TEAM_ID`, `APPLE_APP_SPECIFIC_PASSWORD`)

---

## Future Requirements (deferred)

- Windows Authenticode code signing — eliminates SmartScreen warning; requires EV certificate (~$200-500/yr)
- Linux packages (DEB, RPM, AppImage) — not needed for current team
- macOS universal binary (single DMG arm64+x64) — requires `lipo` merge post-jpackage; two separate DMGs simpler
- Auto-update in-app — no mechanism needed for internal dev tool
- Mac App Store distribution — different signing + sandboxing requirements; internal tool only
- Separate `ci.yml` for PR test runs — useful once release pipeline is stable

## Out of Scope

- Authentication — internal tool
- Multi-user collaboration — local only
- HTML preview — PDF only
- Auto-render — manual trigger only
- Session restore — keep startup simple
- XSLT debugger — deferred to v2

---

## Archived: v0.3.0 Requirements

Previously completed. Requirements UI-01..04, LOG-01..03, VER-01..02, ICON-01..02, ENC-01..03, DOC-01..03 — all delivered in phases 14–18.

---

## Traceability

| REQ-ID | Phase | Status |
|--------|-------|--------|
| BUILD-01 | Phase 19 | Pending |
| BUILD-02 | Phase 19 | Pending |
| BUILD-03 | Phase 19 | Pending |
| BUILD-04 | Phase 19 | Pending |
| CI-01 | Phase 20 | Pending |
| CI-02 | Phase 20 | Pending |
| CI-03 | Phase 20 | Pending |
| MACOS-01 | Phase 20 | Pending |
| MACOS-02 | Phase 20 | Pending |
| MACOS-03 | Phase 21 | Pending |
| MACOS-04 | Phase 22 | Pending |
| WIN-01 | Phase 20 | Pending |
| WIN-02 | Phase 20 | Pending |
| REL-01 | Phase 20 | Pending |
| REL-02 | Phase 20 | Pending |
| REL-03 | Phase 20 | Pending |
| SIGN-01 | Phase 23 | Pending |

---

*Requirements created: 2026-04-25 — Milestone v0.4.0*
