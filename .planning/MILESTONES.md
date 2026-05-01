# Milestones: XSLEditor

## v0.5.0 Undo, Fix & Licenza (Shipped: 2026-05-01)

**Phases completed:** 5 phases, 6 plans, 3 tasks

**Key accomplishments:**

- XSLEditorApp.java
- One-liner:
- One-liner:
- Complete README.md with pipeline overview, Java 21 prerequisite, exact build/run commands, full tech stack table, and 3038×2046 screenshot of the three-panel UI

---

## v1.0 MVP — Shipped 2026-04-21

**Phases:** 1–9 | **Plans:** 24 | **Timeline:** 14 days (2026-04-07 → 2026-04-21)
**Java LOC:** 3,435 | **Files changed:** 183 | **Tests:** 96 green

### Delivered

Full-featured local desktop developer tool for editing multi-file XSLT/XSL-FO templates. Java 21 + JavaFX. Complete XML → XSLT → XSL-FO → PDF pipeline with integrated editor, file tree, render pipeline, PDF preview, error log panel, and comprehensive test suite.

### Key Accomplishments

1. JavaFX 3-zone shell — fat JAR, shadowJar, window lifecycle, close-confirmation
2. Project management — open/persist/restore `.xslfo-tool.json`, path-traversal guard
3. File tree — FileItem model, entrypoint/XML input visual roles, context menu wiring
4. Multi-tab RichTextFX editor — dirty tracking via UndoManager, Ctrl+S, close confirmation, dedup
5. Editor features — XML syntax highlight, autocomplete, occurrence highlight, Ctrl+Click go-to-def, find-in-files
6. Full render pipeline — Saxon+FOP via JavaFX Task daemon, progress feedback, error routing
7. PDF preview — PDFBox 150 DPI PNG rendering (macOS WebKit workaround), outdated overlay
8. Error log panel — TableView, severity filter, click-to-navigate to source line
9. Test suite — 96 tests, unit coverage all backend modules + integration tests with real Saxon+FOP

### Stats

| Metric | Value |
|--------|-------|
| Phases | 9 |
| Plans | 24 |
| Java LOC | 3,435 |
| Test count | 96 |
| Files changed | 183 |
| Timeline | 14 days |
| Requirements | 40/40 |
| E2E flows | 5/5 |

### Known Tech Debt at Close

- **ERR-04** (MEDIUM): Saxon runtime errors return `file://` URI systemId — Path.of() fails on macOS. Fix: URI-decode in PreviewManager.toPreviewErrors()
- **EDIT-06** (LOW): Occurrence highlighting partial — edge cases across element boundaries
- **EDIT-07** (LOW): Go-to-definition wired but Ctrl+Click not explicitly human-verified
- Missing VERIFICATION.md for Phases 01, 05, 07 (documentation gap only)
- Nyquist compliance PARTIAL — not systematically applied in v1.0

### Archive

- `.planning/milestones/v1.0-ROADMAP.md` — full phase details
- `.planning/milestones/v1.0-REQUIREMENTS.md` — all 40 requirements with outcomes
- `.planning/milestones/v1.0-MILESTONE-AUDIT.md` — audit report (tech_debt, 0 blockers)

---
