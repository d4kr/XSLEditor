# Roadmap: XSLEditor

## Milestones

- ✅ **v0.1.0 MVP** — Phases 1–9 (shipped 2026-04-21)
- ✅ **v0.2.0 Developer UX Improvements** — Phases 10–12 (shipped 2026-04-22)
- ✅ **v0.2.1 XSLEditor Full Rename** — Phase 13 (shipped 2026-04-22)
- ✅ **v0.3.0 Polish & Usability** — Phases 14–18 (shipped 2026-04-24)
- 🚧 **v0.4.0 GitHub Releases & Distribution** — Phases 19–23 (in progress)

## Phases

<details>
<summary>✅ v0.1.0 MVP (Phases 1–9) — SHIPPED 2026-04-21</summary>

- [x] Phase 1: JavaFX Application Shell (1/1 plans) — completed 2026-04-14
- [x] Phase 2: Project Management (2/2 plans) — completed 2026-04-15
- [x] Phase 3: File Tree View (3/3 plans) — completed 2026-04-17
- [x] Phase 4: Multi-Tab Editor Core (3/3 plans) — completed 2026-04-18
- [x] Phase 5: Editor Features (5/5 plans) — completed 2026-04-19
- [x] Phase 6: Render Pipeline Integration (2/2 plans) — completed 2026-04-19
- [x] Phase 7: PDF Preview Panel (2/2 plans) — completed 2026-04-20
- [x] Phase 8: Error & Log Panel (2/2 plans) — completed 2026-04-20
- [x] Phase 9: Testing (4/4 plans) — completed 2026-04-21

Full archive: `.planning/milestones/v1.0-ROADMAP.md`

</details>

<details>
<summary>✅ v0.2.0 Developer UX Improvements (Phases 10–12) — SHIPPED 2026-04-22</summary>

- [x] Phase 10: Saxon URI Fix (1/1 plans) — completed 2026-04-21
- [x] Phase 11: About Dialog (3/3 plans) — completed 2026-04-22
- [x] Phase 12: AI Assist in Error Log (1/1 plans) — completed 2026-04-22

Full archive: `.planning/milestones/v0.2.0-ROADMAP.md`

</details>

<details>
<summary>✅ v0.2.1 XSLEditor Full Rename (Phase 13) — SHIPPED 2026-04-22</summary>

- [x] Phase 13: Full Project Rename (3/3 plans) — completed 2026-04-22

</details>

<details>
<summary>✅ v0.3.0 Polish & Usability (Phases 14–18) — SHIPPED 2026-04-24</summary>

- [x] **Phase 14: Version & Icon Housekeeping** — Bump version to 0.3.0, move icon to resources, wire icon in app and About dialog — completed 2026-04-23
- [x] **Phase 15: Dark Theme CSS Fixes** — Make editor, file tree, and log panel text fully readable on dark backgrounds
- [x] **Phase 16: Log Panel Layout** — Full-width table, no phantom column, no squashed columns at narrow widths
- [x] **Phase 17: Encoding Investigation & Fix** — Diagnose root cause of non-ASCII character issues, fix at the confirmed layer
- [x] **Phase 18: README Rewrite** — Complete README with correct version, icon, screenshot, and build instructions

</details>

### 🚧 v0.4.0 GitHub Releases & Distribution (In Progress)

**Milestone Goal:** Automate build and publication of the app on GitHub Releases — JAR + signed/notarized macOS DMG + Windows MSI + Windows portable ZIP — triggered by a git tag push, with release notes generated automatically from git history.

- [ ] **Phase 19: Launcher Shim & Local Build** — Add Launcher.java, update shadowJar manifest, verify fat JAR and jpackage locally (2 plans)
- [ ] **Phase 20: CI Skeleton — Unsigned Release Pipeline** — GitHub Actions workflow producing unsigned DMGs, Windows MSI, Windows ZIP, fat JAR, and a GitHub Release on tag push
- [x] **Phase 21: macOS Signing** — Sign both DMGs with Developer ID Application certificate; codesign verification passes (completed 2026-04-26)
- [x] **Phase 22: macOS Notarization** — Notarize and staple both DMGs; Gatekeeper accepts without quarantine dialog (completed 2026-04-26)
- [x] **Phase 23: Signing Documentation** — docs/SIGNING.md guides a maintainer through configuring all 7 macOS signing secrets (completed 2026-04-27)

## Phase Details

### Phase 14: Version & Icon Housekeeping
**Goal**: The app reports the correct version (0.3.0) automatically from the build, and the app icon is visible in the window title bar and About dialog
**Depends on**: Phase 13
**Requirements**: VER-01, VER-02, ICON-01, ICON-02
**Success Criteria** (what must be TRUE):
  1. About dialog displays "0.3.0" — not hardcoded, not a literal placeholder like `${version}`
  2. App icon is visible in the macOS window title bar (wired before `primaryStage.show()`)
  3. About dialog shows the app icon alongside the version information
  4. Icon file lives at `src/main/resources/` (not project root), and a missing/misplaced icon logs a warning rather than crashing silently
**Plans**: 2 plans
Plans:
- [x] 14-01-PLAN.md — Bump version to 0.3.0 and move icon.png to resources tree
- [x] 14-02-PLAN.md — Wire icon in XSLEditorApp stage and About dialog ImageView
**UI hint**: yes

### Phase 15: Dark Theme CSS Fixes
**Goal**: All text in the UI is readable against the dark background — in the code editor, file tree, and log panel — including selected and focused states
**Depends on**: Phase 14
**Requirements**: UI-01, UI-02, UI-03, UI-04
**Success Criteria** (what must be TRUE):
  1. Code editor shows light text on a dark background; syntax colors are distinguishable; caret and selection highlight are visible
  2. File tree cells show readable text in default, hover, and selected states — no dark-on-dark inversion
  3. Log panel rows show readable text for all severity levels (INFO, WARNING, ERROR) in default and selected states
  4. Selected rows in both TreeView and TableView show a visible highlight with readable (not invisible) text
**Plans**: 1 plan
Plans:
- [x] 15-01-PLAN.md — Append Phase 15 CSS block: CodeArea dark bg, TreeView/TableView selected state fixes
**UI hint**: yes

### Phase 16: Log Panel Layout
**Goal**: The log panel TableView fills the full container width with no phantom filler column, and no column compresses to an unreadable width at narrow window sizes
**Depends on**: Phase 15
**Requirements**: LOG-01, LOG-02, LOG-03
**Success Criteria** (what must be TRUE):
  1. Log panel TableView expands horizontally to fill 100% of its container width — no empty space at the right edge
  2. No phantom empty column appears at the right side of the log table
  3. Time, Level, and Action columns retain a readable minimum width even when the window is at its minimum size; Message column absorbs remaining width
**Plans**: 1 plan
Plans:
- [x] 16-01-PLAN.md — Set CONSTRAINED_RESIZE_POLICY + minWidth/maxWidth on all five log table columns
**UI hint**: yes

### Phase 17: Encoding Investigation & Fix
**Goal**: Non-ASCII and special characters (including accented Italian characters) display correctly in the code editor, log panel, and PDF output, with the root cause identified and fixed at the correct pipeline layer
**Depends on**: Phase 16
**Requirements**: ENC-01, ENC-02, ENC-03
**Success Criteria** (what must be TRUE):
  1. The root cause of the encoding issue is documented (BOM artifact, Saxon `xsl:output` declaration mismatch, or FOP font substitution for PDF glyphs)
  2. Opening a UTF-8 file with non-ASCII characters in the editor shows those characters correctly — no BOM character prepended, no replacement glyphs
  3. Log panel messages containing non-ASCII characters (e.g., error messages referencing Italian filenames or XSLT values) display correctly
**Plans**: 1 plan
Plans:
- [x] 17-01-PLAN.md — New XmlCharsetDetector utility; fix EditorTab/EditorController read+write, RenderOrchestrator, LibraryPreprocessor to respect declared XML encoding

### Phase 18: README Rewrite
**Goal**: The README accurately describes XSLEditor as it exists after v0.3.0 — with correct version, build instructions, visible app icon, and a screenshot of the working UI
**Depends on**: Phase 17
**Requirements**: DOC-01, DOC-02, DOC-03
**Success Criteria** (what must be TRUE):
  1. README includes project overview, prerequisites (Java 21), build command, and run command — sufficient for a developer to build and run without prior context
  2. README includes the app icon image and a screenshot of the main window showing the editor, file tree, and PDF preview
  3. README states the correct version (0.3.0) and lists the current tech stack (Java 21, Saxon-HE 12.4, Apache FOP 2.9, JavaFX, RichTextFX)
**Plans**: 1 plan
Plans:
- [x] 18-01-PLAN.md — Write complete README.md, build shadow JAR, capture screenshot via human checkpoint

---

## v0.4.0 Phase Details

### Phase 19: Launcher Shim & Local Build
**Goal**: The fat JAR launches cleanly via `java -jar` and jpackage can wrap it into a native bundle locally — the mandatory prerequisite for all CI packaging work
**Depends on**: Phase 18
**Requirements**: BUILD-01, BUILD-02, BUILD-03, BUILD-04
**Success Criteria** (what must be TRUE):
  1. Running `java -jar build/libs/XSLEditor-*.jar` on the developer machine launches the full app without "JavaFX runtime components are missing" errors
  2. The fat JAR manifest points to `ch.ti.gagi.xsleditor.Launcher` and `META-INF/services/javax.xml.transform.TransformerFactory` is present in the JAR (Saxon/FOP service registration intact after `mergeServiceFiles()`)
  3. Passing `-Pversion=0.4.0` to `./gradlew shadowJar` produces a JAR whose `version.properties` reports `0.4.0`
  4. Running jpackage locally with the fat JAR and an `icon.png` produces a native bundle (`.app` on macOS or `.exe` on Windows) where the app icon is visible in the Dock/taskbar
**Plans**: TBD

### Phase 20: CI Skeleton — Unsigned Release Pipeline
**Goal**: Pushing a `v*` tag triggers a GitHub Actions workflow that builds all platform artifacts and publishes a complete GitHub Release — unsigned, but end-to-end verified
**Depends on**: Phase 19
**Requirements**: CI-01, CI-02, CI-03, MACOS-01, MACOS-02, WIN-01, WIN-02, REL-01, REL-02, REL-03
**Success Criteria** (what must be TRUE):
  1. Pushing a `v0.4.0-test1` tag triggers the workflow and all 5 jobs complete green on GitHub Actions; the fat JAR is built once on `ubuntu-latest` and downloaded by all platform packaging jobs without re-running Gradle
  2. The macOS arm64 job (`macos-15`) and macOS x64 job (`macos-15-intel`) each produce a DMG artifact using Liberica JDK+FX 21; both DMGs mount and the app launches
  3. The Windows job (`windows-latest`) produces a signed-off MSI installer (unsigned at this phase) and a portable ZIP; the MSI installs without errors and the app launches; the ZIP runs without installation
  4. A GitHub Release is created automatically with all 5 assets attached (arm64 DMG, x64 DMG, MSI, Windows portable ZIP, fat JAR); release notes are populated from the git log between the previous tag and the new tag
  5. Tags containing `-` (e.g. `v0.4.0-beta1`) are automatically marked as pre-release on GitHub; clean tags (e.g. `v0.4.0`) are marked as full release
**Plans**: TBD

### Phase 21: macOS Signing
**Goal**: Both macOS DMGs (arm64 and x64) are signed with a Developer ID Application certificate so Gatekeeper accepts them as coming from an identified developer
**Depends on**: Phase 20
**Requirements**: MACOS-03
**Success Criteria** (what must be TRUE):
  1. `codesign --verify --deep --strict` passes on both the arm64 and x64 DMG artifacts produced by the CI workflow
  2. The CI signing sequence (import cert to keychain, `security set-key-partition-list`, jpackage with `--mac-sign --mac-entitlements`) runs without interactive prompts and does not hang on a headless runner
  3. The `entitlements.plist` file (with `com.apple.security.cs.allow-jit` and `com.apple.security.cs.disable-library-validation`) is committed to the repository and referenced in the jpackage invocation
  4. The 7 required GitHub Actions secrets (`MACOS_CERTIFICATE`, `MACOS_CERTIFICATE_PASSWORD`, `MACOS_SIGNING_IDENTITY`, `MACOS_KEYCHAIN_PASSWORD`, `APPLE_ID`, `APPLE_TEAM_ID`, `APPLE_APP_SPECIFIC_PASSWORD`) are documented and the workflow reads them correctly
**Plans**: 2 plans
Plans:
- [x] 21-01-PLAN.md — entitlements.plist + CI signing sequence in both macOS jobs
- [x] 21-02-PLAN.md — Secrets documentation comment in release.yml

### Phase 22: macOS Notarization
**Goal**: Both macOS DMGs are notarized by Apple and stapled so that Gatekeeper accepts them on any macOS machine without a quarantine dialog — even offline
**Depends on**: Phase 21
**Requirements**: MACOS-04
**Success Criteria** (what must be TRUE):
  1. `xcrun notarytool submit --wait` completes with status `Accepted` for both the arm64 and x64 DMGs in the CI workflow
  2. `xcrun stapler staple` runs successfully on both DMGs after notarization; the staple ticket is embedded in the DMG
  3. Downloading the stapled arm64 DMG on a macOS machine and opening the app does not show a Gatekeeper quarantine dialog ("Apple cannot verify that this app is free of malware")
**Plans**: 1 plan
Plans:
- [x] 22-01-PLAN.md — Notarize + staple + Gatekeeper-check steps in both macOS CI jobs

### Phase 23: Signing Documentation
**Goal**: A developer who is new to the repository can configure all macOS signing secrets and have the full signing + notarization pipeline working by following `docs/SIGNING.md` alone
**Depends on**: Phase 22
**Requirements**: SIGN-01
**Success Criteria** (what must be TRUE):
  1. `docs/SIGNING.md` exists in the repository and documents the end-to-end process: exporting the Developer ID Application certificate as `.p12` from Keychain Access, base64-encoding it, and setting all 7 required GitHub Actions secrets
  2. The document names each secret (`MACOS_CERTIFICATE`, `MACOS_CERTIFICATE_PASSWORD`, `MACOS_SIGNING_IDENTITY`, `MACOS_KEYCHAIN_PASSWORD`, `APPLE_ID`, `APPLE_TEAM_ID`, `APPLE_APP_SPECIFIC_PASSWORD`) and explains what value each expects
  3. The document notes the Apple Developer Program membership requirement ($99/yr) for notarization and explains the SmartScreen warning that appears on Windows due to unsigned MSI
**Plans**: 1 plan
Plans:
- [x] 23-01-PLAN.md — Create docs/SIGNING.md, link from README, add pointer comment in release.yml

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1–13 | v0.1.0–v0.2.1 | 32/32 | Complete | 2026-04-22 |
| 14. Version & Icon Housekeeping | v0.3.0 | 2/2 | Complete | 2026-04-23 |
| 15. Dark Theme CSS Fixes | v0.3.0 | 1/1 | Complete | 2026-04-23 |
| 16. Log Panel Layout | v0.3.0 | 1/1 | Complete | 2026-04-23 |
| 17. Encoding Investigation & Fix | v0.3.0 | 1/1 | Complete | 2026-04-23 |
| 18. README Rewrite | v0.3.0 | 1/1 | Complete | 2026-04-24 |
| 19. Launcher Shim & Local Build | v0.4.0 | 0/2 | Not started | - |
| 20. CI Skeleton — Unsigned Release Pipeline | v0.4.0 | 0/? | Not started | - |
| 21. macOS Signing | v0.4.0 | 2/2 | Complete    | 2026-04-26 |
| 22. macOS Notarization | v0.4.0 | 1/1 | Complete    | 2026-04-26 |
| 23. Signing Documentation | v0.4.0 | 1/1 | Complete   | 2026-04-27 |

---
*Roadmap updated: 2026-04-26*
