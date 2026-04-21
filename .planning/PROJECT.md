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

_(all MVP requirements are now validated — v1.0 milestone complete)_

### Previously Active — Validated in Phase 09: Testing

- ✓ Unit tests for all backend modules — Validated in Phase 09 (TEST-01..TEST-06, TEST-08)
- ✓ Integration tests for full render pipeline — Validated in Phase 09 (TEST-07, TEST-08)

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
- UI: JavaFX with RichTextFX editor, PDFBox-based WebView PDF display
- **v1.0 milestone complete** — Phase 9 (Testing) passed 8/8 requirements, 96 tests green
- Projects are small (≈5–10 files, <1MB each)
- Performance target: edit-to-preview < 5 seconds
- Tech debt in CONCERNS.md addressed by Phase 9 test coverage

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
*Last updated: 2026-04-21 — Phase 09 complete, v1.0 milestone achieved*
