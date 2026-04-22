# Requirements: v0.2.1 XSLEditor Full Rename

**Milestone:** v0.2.1 XSLEditor Full Rename
**Status:** 📋 Draft
**Date:** 2026-04-22

## v0.2.1 Requirements

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

## Traceability

| Requirement | Phase | Status | Outcome |
|-------------|-------|--------|---------|
| RENAME-01   | 13    | ✅ Complete | Updated UI strings in FXML and Java constants |
| RENAME-02   | 13    | 📋 Pending | |
| RENAME-03   | 13    | ✅ Complete | Updated title and version label in AboutDialog |
| RENAME-04   | 13    | ✅ Complete | Renamed to XSLEditorApp.java and updated class definition |
| RENAME-05   | 13    | ✅ Complete | Updated package declarations in all Java files |
| RENAME-06   | 13    | ✅ Complete | Renamed directories to ch/ti/gagi/xsleditor |
| RENAME-07   | 13    | ✅ Complete | Updated all imports, FXML controller references and resource paths |
| RENAME-08   | 13    | ✅ Complete | Updated mainClass and JAR manifest in build.gradle |
| RENAME-09   | 13    | 📋 Pending | |
| RENAME-10   | 13    | 📋 Pending | |

## Out of Scope

- Migration tool for old `.xslfo-tool.json` files (User must manually rename or re-configure).
- Renaming existing git branches or remote repositories.

---
*Requirements created: 2026-04-22*
