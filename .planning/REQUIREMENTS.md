# Requirements: XLSEditor

**Defined:** 2026-04-21
**Milestone:** v1.1 Developer UX Improvements
**Core Value:** A developer can open a project, edit XSLT templates, trigger a render, and see the PDF — all in one window without context switching.

## v1.1 Requirements

### About

- [ ] **ABOUT-01**: User can open About dialog from Help menu
- [ ] **ABOUT-02**: About dialog displays current app version (e.g. 1.1.0)
- [ ] **ABOUT-03**: About dialog displays runtime stack versions (Java, Saxon-HE, Apache FOP, JavaFX)
- [ ] **ABOUT-04**: About dialog displays author / credits
- [ ] **ABOUT-05**: About dialog displays license info (text or link)

### Error Log

- [ ] **ERR-04**: Saxon runtime error navigation fixed — URI-decode `file://` paths in PreviewManager.toPreviewErrors() so click-to-navigate works on macOS
- [ ] **ERR-06**: Each error row shows an AI assist button that opens ChatGPT in the browser with the error message pre-filled

## Future Requirements

### Editor

- **EDIT-06**: Occurrence highlighting edge cases across element boundaries — deferred, low priority
- **EDIT-07**: Confirm Ctrl+Click go-to-definition with xsl:include files — human verification pending

## Out of Scope

| Feature | Reason |
|---------|--------|
| Other AI providers (Copilot, Gemini) | ChatGPT sufficient for v1.1; multi-provider adds complexity |
| Inline error gutter | Log panel sufficient per v1.0 decision |
| Error history / persistence | Clears on render — by design |
| About dialog with update checker | No auto-update mechanism in scope |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| ERR-04      | Phase 10 | Pending |
| ABOUT-01    | Phase 11 | Pending |
| ABOUT-02    | Phase 11 | Pending |
| ABOUT-03    | Phase 11 | Pending |
| ABOUT-04    | Phase 11 | Pending |
| ABOUT-05    | Phase 11 | Pending |
| ERR-06      | Phase 12 | Pending |

**Coverage:**
- v1.1 requirements: 7 total
- Mapped to phases: 7
- Unmapped: 0 ✓

---
*Requirements defined: 2026-04-21*
*Last updated: 2026-04-21 after initial definition*
