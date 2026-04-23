# Roadmap: XSLEditor

## Milestones

- ✅ **v0.1.0 MVP** — Phases 1–9 (shipped 2026-04-21)
- ✅ **v0.2.0 Developer UX Improvements** — Phases 10–12 (shipped 2026-04-22)
- ✅ **v0.2.1 XSLEditor Full Rename** — Phase 13 (shipped 2026-04-22)
- 🚧 **v0.3.0 Polish & Usability** — Phases 14–18 (in progress)

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

### 🚧 v0.3.0 Polish & Usability (In Progress)

**Milestone Goal:** Improve UI readability in dark theme, fix encoding issues, wire app icon correctly, auto-source the About version from the build, and rewrite the README to reflect the current state of the project.

- [x] **Phase 14: Version & Icon Housekeeping** — Bump version to 0.3.0, move icon to resources, wire icon in app and About dialog — completed 2026-04-23
- [ ] **Phase 15: Dark Theme CSS Fixes** — Make editor, file tree, and log panel text fully readable on dark backgrounds
- [ ] **Phase 16: Log Panel Layout** — Full-width table, no phantom column, no squashed columns at narrow widths
- [ ] **Phase 17: Encoding Investigation & Fix** — Diagnose root cause of non-ASCII character issues, fix at the confirmed layer
- [ ] **Phase 18: README Rewrite** — Complete README with correct version, icon, screenshot, and build instructions

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
**Plans**: TBD
**UI hint**: yes

### Phase 17: Encoding Investigation & Fix
**Goal**: Non-ASCII and special characters (including accented Italian characters) display correctly in the code editor, log panel, and PDF output, with the root cause identified and fixed at the correct pipeline layer
**Depends on**: Phase 16
**Requirements**: ENC-01, ENC-02, ENC-03
**Success Criteria** (what must be TRUE):
  1. The root cause of the encoding issue is documented (BOM artifact, Saxon `xsl:output` declaration mismatch, or FOP font substitution for PDF glyphs)
  2. Opening a UTF-8 file with non-ASCII characters in the editor shows those characters correctly — no BOM character prepended, no replacement glyphs
  3. Log panel messages containing non-ASCII characters (e.g., error messages referencing Italian filenames or XSLT values) display correctly
**Plans**: TBD

### Phase 18: README Rewrite
**Goal**: The README accurately describes XSLEditor as it exists after v0.3.0 — with correct version, build instructions, visible app icon, and a screenshot of the working UI
**Depends on**: Phase 17
**Requirements**: DOC-01, DOC-02, DOC-03
**Success Criteria** (what must be TRUE):
  1. README includes project overview, prerequisites (Java 21), build command, and run command — sufficient for a developer to build and run without prior context
  2. README includes the app icon image and a screenshot of the main window showing the editor, file tree, and PDF preview
  3. README states the correct version (0.3.0) and lists the current tech stack (Java 21, Saxon-HE 12.4, Apache FOP 2.9, JavaFX, RichTextFX)
**Plans**: TBD

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1–13 | v0.1.0–v0.2.1 | 32/32 | Complete | 2026-04-22 |
| 14. Version & Icon Housekeeping | v0.3.0 | 2/2 | Complete | 2026-04-23 |
| 15. Dark Theme CSS Fixes | v0.3.0 | 0/1 | Planned | - |
| 16. Log Panel Layout | v0.3.0 | 0/? | Not started | - |
| 17. Encoding Investigation & Fix | v0.3.0 | 0/? | Not started | - |
| 18. README Rewrite | v0.3.0 | 0/? | Not started | - |

---
*Roadmap updated: 2026-04-23*
