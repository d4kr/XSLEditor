# Requirements: XSLEditor v0.5.0 — Undo, Fix & Licenza

**Defined:** 2026-04-29
**Milestone:** v0.5.0
**Core Value:** A developer can open a project, edit XSLT templates, trigger a render, and see the PDF — all in one window without context switching.

## v0.5.0 Requirements

### Bug Fix

- [ ] **ERR-07**: Developer can click "💬" in the error log and the browser opens ChatGPT with the pre-filled prompt correctly (fix broken chatgpt.com/?q= URL param)

### Editor — Undo / Redo

- [ ] **EDIT-14**: Developer can undo the last edit via Edit > Undo (Ctrl+Z) in the active editor tab
- [ ] **EDIT-15**: Developer can redo an undone edit via Edit > Redo (Ctrl+Shift+Z) in the active editor tab

### Toolbar

- [ ] **TOOL-01**: Developer can click Undo button in the toolbar (disabled when no undo history available)
- [ ] **TOOL-02**: Developer can click Redo button in the toolbar (disabled when no redo history available)
- [ ] **TOOL-03**: Developer can click Save button in the toolbar (saves the active tab; disabled when tab is clean/not dirty)

### Documentation & Licensing

- [ ] **DOC-01**: Repository contains a LICENSE file (MIT, 2026) in the project root
- [ ] **DOC-02**: About dialog shows "License: MIT" with a link to the MIT license text
- [ ] **DOC-03**: README logo is rendered at reduced size via HTML `<img width="96">` tag instead of bare Markdown image link

## Previous Milestone Requirements (v0.4.1) — Archived

All v0.4.1 requirements (KBD-01..05, EDIT-10..13) validated and shipped. See `.planning/milestones/v0.4.1-REQUIREMENTS.md` for archive.

## Future Requirements

*(none defined)*

## Out of Scope

| Feature | Reason |
|---------|--------|
| Undo history persistence across sessions | Not needed for local dev tool |
| Undo/Redo per-file history panel | Standard editor behavior is sufficient |
| Global undo across files | Per-CodeArea undo only; matches user expectation |
| Delete Line / Duplicate Line | Not requested for this milestone |
| View menu items | No features defined |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| ERR-07 | Phase 27 | Pending |
| EDIT-14 | Phase 26 | Pending |
| EDIT-15 | Phase 26 | Pending |
| TOOL-01 | Phase 26 | Pending |
| TOOL-02 | Phase 26 | Pending |
| TOOL-03 | Phase 27 | Pending |
| DOC-01 | Phase 28 | Pending |
| DOC-02 | Phase 28 | Pending |
| DOC-03 | Phase 28 | Pending |

**Coverage:**
- v0.5.0 requirements: 9 total
- Mapped to phases: 9
- Unmapped: 0

---
*Requirements defined: 2026-04-29*
*Last updated: 2026-04-29 — roadmap created, all 9 requirements mapped*
