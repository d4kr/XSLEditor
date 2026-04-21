# XLSEditor

## Current Milestone: v1.1 Developer UX Improvements

**Goal:** Add an About dialog and ChatGPT quick-link in the error log so developers can instantly look up pipeline errors with AI assistance.

**Target features:**
- About dialog — app version, stack info, credits
- ChatGPT link in error log — per-error action that opens ChatGPT pre-filled with the error message
- ERR-04 fix — Saxon `file://` URI-decode in PreviewManager.toPreviewErrors() (carried from v1.0)

## What This Is

Local desktop developer tool for editing multi-file XSLT/XSL-FO templates, generating PDFs on demand, and debugging locally. Built in Java 21 with JavaFX for internal developers who work with the XML → XSLT → XSL-FO → PDF pipeline. No auth, no multi-user, no external backend dependencies. v1.0 shipped with full editor, render pipeline, PDF preview, error log, and 96-test suite.

## Core Value

A developer can open a project, edit XSLT templates, trigger a render, and see the PDF — all in one window without context switching.

## Requirements

### Validated

- ✓ APP-01..04: JavaFX shell — launches, title, close confirmation, no session restore — v1.0
- ✓ PROJ-01..06: Project management — open, entrypoint, XML input, persist, restore, new file — v1.0
- ✓ TREE-01..04: File tree — list, visual distinction entrypoint/xml, double-click open — v1.0
- ✓ EDIT-01..03, EDIT-09: Multi-tab editor — tabs, dirty `*`, Ctrl+S, close confirmation — v1.0
- ✓ EDIT-04..08: Editor features — syntax highlight, autocomplete, occurrence highlight, go-to-def, find-in-files — v1.0
- ✓ REND-01..06: Render pipeline — full pipeline, disabled guard, progress, PDF update, error routing, < 5s — v1.0
- ✓ PREV-01..04: PDF preview — split view, scroll/zoom, outdated indicator, placeholder — v1.0
- ✓ ERR-01..05: Error log — TableView, severity columns, filter, click-to-navigate, clear on render — v1.0
- ✓ TEST-01..08: Test suite — unit tests all backend modules + integration tests full pipeline — v1.0
- ✓ Backend pipeline complete (LibraryPreprocessor, DependencyResolver, ValidationEngine, RenderEngine, etc.) — pre-v1.0

### Active

- [ ] **ERR-04 fix**: URI-decode `file://` paths in PreviewManager.toPreviewErrors() — Saxon runtime error navigation broken on macOS
- [ ] **ABOUT-01**: User can open an About dialog showing app version, stack info, and credits
- [ ] **ERR-06**: Error log shows a ChatGPT link per error that opens a pre-filled query for instant AI assistance
- [ ] **EDIT-06 edge cases**: Occurrence highlighting across element boundaries
- [ ] **Nyquist compliance**: Systematically apply test contracts for v1.1 phases
- [ ] **EDIT-07 human verify**: Confirm Ctrl+Click go-to-definition with xsl:include file

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
- Session restore (last project) — user opens manually, keep startup simple
- Inline error gutter — log panel sufficient for v1

## Context

- **Stack:** Java 21, Saxon-HE 12.4, Apache FOP 2.9, Jackson 2.17.2, PDFBox 2.0.31
- **UI:** JavaFX with RichTextFX editor, PDFBox-based WebView PDF rendering (150 DPI PNG pages)
- **v1.0 shipped 2026-04-21** — 9 phases, 24 plans, 3,435 Java LOC, 96 tests green, 14 days
- Projects are small (≈5–10 files, <1MB each); performance target met (< 5s render confirmed)
- Tech debt: ERR-04 Saxon URI edge case, EDIT-06 partial, missing VERIFICATION.md for phases 01/05/07

## Constraints

- **Platform:** Desktop application, Java 21 runtime required
- **UI:** JavaFX only — no Electron, no web frontend
- **Pipeline:** Saxon for XSLT, Apache FOP for XSL-FO — not swappable
- **Portability:** Projects must be fully portable (relative paths, config in `.xslfo-tool.json`)
- **Performance:** Render cycle target < 5s for typical projects

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| JavaFX for UI | Standard modern Java desktop, CSS styling, FXML | ✓ Good — clean architecture, CSS theming works well |
| RichTextFX for editor | Native JavaFX, lightweight, syntax highlighting via CSS | ✓ Good — CSS StyleSpans approach solid |
| Shadow plugin → com.gradleup.shadow 9.0.0-beta12 | com.github.johnrengelman.shadow incompatible with Gradle 9 | ✓ Good — fat JAR works |
| WebView + PDFBox 150 DPI PNG rendering | macOS JavaFX WebView does not render PDFs via file:// URIs | ✓ Good — renders correctly, scroll/zoom via HTML |
| EditorTab public final fields (data carrier) | No setters needed; EditorController owns lifecycle | ✓ Good — clean separation |
| Dirty state via UndoManager.atMarkedPositionProperty() | Handles undo-back-to-clean correctly | ✓ Good — correct behavior confirmed |
| Per-CodeArea Ctrl+S via WellBehavedFX Nodes.addInputMap | Scene-level handler causes focus bugs | ✓ Good — no focus issues |
| Consumer callback seam for log/preview | Decouples sub-controllers, no upward imports | ✓ Good — clean dependency graph |
| LogController wired before RenderController | Callbacks must be ready at RenderController.initialize() | ✓ Good — ordering matters, documented |
| Fine granularity phasing (9 phases) | Complex UI work benefits from focused increments | ✓ Good — each phase reviewable in isolation |
| Skeleton production classes in Wave 0 | @Disabled tests compile; skeletons allow test stubs green until Wave 1 | ✓ Good — Wave pattern worked well |
| Set Entrypoint/XML Input disabled Phase 2, enabled Phase 3 | Tree doesn't exist until Phase 3 (D-04) | ✓ Good — correct partial delivery |
| SearchDialog.search() as static method | Testability; follows RenderOrchestrator pattern | ✓ Good |
| SearchExecutor shutdownNow() dual guard | Cancel prior task on new search + on close (T-05-10) | ✓ Good — no leaked threads |

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
*Last updated: 2026-04-21 after v1.1 milestone start*
