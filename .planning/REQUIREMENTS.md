# Requirements: XSLEditor v0.3.0 Polish & Usability

**Milestone:** v0.3.0  
**Goal:** Migliorare leggibilità UI, correggere encoding, aggiornare documentazione e sistemare la versione About.  
**Date:** 2026-04-23  

---

## v0.3.0 Requirements

### UI Dark Theme

- [ ] **UI-01**: User sees readable text in the code editor (CodeArea background dark, syntax colors visible)
- [ ] **UI-02**: User sees readable text in the file tree (TreeView cells readable in default and selected states)
- [ ] **UI-03**: User sees readable text in the log panel rows (TableView cells readable, all severity levels)
- [ ] **UI-04**: User sees a visible selected-row highlight with readable text in both TreeView and TableView

### Log Panel Layout

- [ ] **LOG-01**: Log panel TableView expands to fill the full width of its container
- [ ] **LOG-02**: No phantom empty column appears at the right edge of the log table
- [ ] **LOG-03**: Log columns (Time, Level, Message, Source, Action) remain usable and not squashed at narrow window widths

### About & Version

- [ ] **VER-01**: About dialog displays the correct version (0.3.0), sourced automatically from the build
- [ ] **VER-02**: About dialog displays the app icon

### App Icon

- [ ] **ICON-01**: App icon is visible in the macOS Dock and window title bar
- [ ] **ICON-02**: Icon file is stored in `src/main/resources/` (not project root)

### Encoding Fix

- [ ] **ENC-01**: Root cause of encoding issue is identified (BOM, Saxon xsl:output declaration, or FOP font)
- [ ] **ENC-02**: Special and non-ASCII characters display correctly in the code editor
- [ ] **ENC-03**: Special and non-ASCII characters display correctly in the log panel messages

### Documentation

- [ ] **DOC-01**: README includes project overview, prerequisites, build/run instructions, and stack description
- [ ] **DOC-02**: README includes the app icon and a screenshot of the main window
- [ ] **DOC-03**: README accurately reflects the current version and full tech stack

---

## Archived: v0.2.1 Requirements (XSLEditor Full Rename)

### Brand Update (UI/Docs)

- [x] **RENAME-01**: Update all user-facing UI strings from "XSLEditor" to "XSLEditor".
- [ ] **RENAME-02**: Update README.md and CLAUDE.md to use the new "XSLEditor" name.
- [x] **RENAME-03**: Update About dialog title and content.

### Codebase Refactor

- [x] **RENAME-04**: Rename main application class from `XSLEditorApp` to `XSLEditorApp`.
- [x] **RENAME-05**: Update base package from `ch.ti.gagi.xsleditor` to `ch.ti.gagi.xsleditor`.
- [x] **RENAME-06**: Update all source file directory structures to match the new package name.
- [x] **RENAME-07**: Update all imports and FXML references to the new package.

### Build & Config

- [x] **RENAME-08**: Update `build.gradle` and `settings.gradle` with new project name and main class path.
- [ ] **RENAME-09**: Rename project configuration file from `.xslfo-tool.json` to `.xsle-tool.json`.
- [ ] **RENAME-10**: Update `ProjectManager` to load/save using the new configuration filename (Breaking Change).

---

## Future Requirements (Deferred)

- Inline error gutter in editor — deferred per PRD (log panel sufficient)
- XSLT debugger — deferred to v2.0
- Multiple XML inputs simultaneously — out of scope
- PDF font embedding improvements — deferred (FOP defaults acceptable)

---

## Out of Scope

| Item | Reason |
|------|--------|
| Auto-render on save | Manual trigger only — product decision |
| File rename/delete in tree | Not in MVP scope |
| macOS Dock icon badge/progress | Platform-specific, low value |
| Session restore (last project) | Startup simplicity — user opens manually |
| HTML preview | PDF only per PRD |
| Migration tool for old `.xslfo-tool.json` | User must manually rename or re-configure |

---

## Traceability

| REQ-ID | Phase | Status |
|--------|-------|--------|
| UI-01..04 | TBD | — |
| LOG-01..03 | TBD | — |
| VER-01..02 | TBD | — |
| ICON-01..02 | TBD | — |
| ENC-01..03 | TBD | — |
| DOC-01..03 | TBD | — |

---

*Requirements created: 2026-04-23*
