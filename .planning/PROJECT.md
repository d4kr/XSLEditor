# XLSEditor

## What This Is

Local desktop developer tool for editing multi-file XSLT/XSL-FO templates, generating PDFs on demand, and debugging locally. Built in Java with JavaFX for internal developers who work with the XML → XSLT → XSL-FO → PDF pipeline. No auth, no multi-user, no external backend dependencies.

## Core Value

A developer can open a project, edit XSLT templates, trigger a render, and see the PDF — all in one window without context switching.

## Requirements

### Validated

- ✓ LibraryPreprocessor: resolves `<?LIBRARY NAME?>` directives via physical merge — existing
- ✓ DependencyResolver: builds `xsl:include`/`xsl:import` dependency graph with circular detection — existing
- ✓ ValidationEngine: validates XML/XSLT well-formedness, aggregates errors — existing
- ✓ RenderEngine: Saxon XSLT transformation + Apache FOP PDF rendering — existing
- ✓ RenderOrchestrator: full pipeline orchestration with `renderSafe` error handling — existing
- ✓ PreviewManager: facade over RenderOrchestrator, returns Preview DTO — existing
- ✓ ErrorManager: exception normalization + file position extraction — existing
- ✓ LogManager: in-memory log storage with level filtering — existing

### Active

- [ ] JavaFX application shell with main window and layout skeleton
- [ ] Project management: open directory, select entrypoint XSLT, select XML input
- [ ] File tree view: flat project directory listing
- [ ] Multi-tab editor with RichTextFX (XML/XSLT syntax highlighting)
- [ ] Manual save (Ctrl+S) and dirty state indicator
- [ ] Render trigger button connected to PreviewManager pipeline
- [ ] Split view: editor left, PDF preview right (PDFViewerFX)
- [ ] Error/log panel with severity filtering
- [ ] Inline error navigation (click error → jump to file:line)
- [ ] Project config persistence (entrypoint + XML input in `.xslfo-tool.json`)
- [ ] Unit tests for all backend modules
- [ ] Integration tests for full render pipeline

### Out of Scope

- Authentication — internal tool, no auth needed
- Multi-user collaboration — local only
- HTML preview — PDF only per PRD
- File rename/delete — not in MVP
- Multiple XML inputs at once — single active input
- Auto-render — manual trigger only
- File watching — no filesystem monitoring
- Advanced autocomplete (semantic) — static tags only for v1
- XSLT debugger — deferred to v2

## Context

- Stack: Java 21, Saxon-HE 12.4, Apache FOP 2.9, Jackson 2.17.2
- UI: JavaFX (to be added), RichTextFX for editor, PDFViewerFX for PDF display
- Backend pipeline is fully implemented and tested manually (T1–T10)
- Projects are small (≈5–10 files, <1MB each)
- Performance target: edit-to-preview < 5 seconds
- Existing codebase has known tech debt (RenderEngine factory init, DocumentBuilderFactory reuse, error parsing fragility) — tracked in CONCERNS.md, addressed in v1 tests

## Constraints

- **Platform**: Desktop application, Java 21 runtime required
- **UI**: JavaFX only — no Electron, no web frontend
- **Pipeline**: Saxon for XSLT, Apache FOP for XSL-FO — not swappable
- **Portability**: Projects must be fully portable (relative paths, config in `.xslfo-tool.json`)
- **Performance**: Render cycle target < 5s for typical projects

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| JavaFX for UI | Standard modern Java desktop, CSS styling, FXML | — Pending |
| RichTextFX for editor | Native JavaFX, lightweight, syntax highlighting via CSS | — Pending |
| PDFViewerFX for preview | Open-source, JavaFX-native, based on PDFBox | — Pending |
| Fine granularity phasing | Complex UI work benefits from focused phases | — Pending |
| Interactive mode | Prefer confirmation at key decision points | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-14 after initialization*
