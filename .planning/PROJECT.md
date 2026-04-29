# XSLEditor

## Current Milestone: v0.5.0 Undo, Fix & Licenza

**Goal:** Undo/redo funzionante nell'editor, fix link ChatGPT nel log, shortcut completi nel menu Edit, licenza MIT corretta con file LICENSE, logo README ridimensionato.

**Target features:**
- Fix bug ChatGPT link nel log (URL param cambiato)
- Undo/Redo completo — esporre UndoManager in Edit menu (Ctrl+Z / Ctrl+Shift+Z)
- Shortcut keyboard Undo/Redo nel menu Edit
- Licenza MIT — aggiungere file LICENSE, aggiornare AboutDialog e README
- README: logo più piccolo via tag HTML `<img width="…">`

## Previous Milestone: v0.4.1 Keyboard Shortcuts & Edit Menu — COMPLETE (2026-04-28)

**Delivered:**
- Acceleratori tastiera (Shortcut+key) su tutti i 5 item del menu File — Phase 24
- Menu Edit — Cut, Copy, Paste, Select All sul CodeArea attivo via `EditorController.getActiveCodeArea()` — Phase 25
- Undo/Redo: deferred — UndoManager usato solo per dirty tracking, non ancora esposto

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
- ✓ TEST-01..08: Test suite — unit tests all backend modules + integration tests full pipeline — v0.1.0
- ✓ Backend pipeline complete (LibraryPreprocessor, DependencyResolver, ValidationEngine, RenderEngine, etc.) — pre-v0.1.0
- ✓ ERR-04: Saxon URI-decode fix — percent-encoded file:// paths navigate correctly on macOS — v0.2.0
- ✓ ABOUT-01..05: About dialog — version, runtime stack, author, license — v0.2.0
- ✓ ERR-06: ChatGPT button per error log row — opens pre-filled query in browser — v0.2.0

### Validated (v0.4.1)

- ✓ **KBD-01..05**: Keyboard accelerators on File menu items — Validated in Phase 24
- ✓ **EDIT-10..13**: Edit menu — Cut, Copy, Paste, Select All commands wired to active CodeArea — Validated in Phase 25

### Active (v0.5.0)

- [ ] **ERR-07**: Fix ChatGPT link URL in error log button
- [ ] **EDIT-14**: Undo command in Edit menu with Ctrl+Z shortcut
- [ ] **EDIT-15**: Redo command in Edit menu with Ctrl+Shift+Z shortcut
- [ ] **DOC-01**: Add MIT LICENSE file to repository
- [ ] **DOC-02**: Update AboutDialog license label and link to MIT
- [ ] **DOC-03**: README logo smaller via HTML `<img>` tag

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
- **v0.1.0 shipped 2026-04-21** — 9 phases, 24 plans, 3,435 Java LOC, 96 tests green, 14 days
- **v0.2.0 shipped 2026-04-22** — 3 phases, 5 plans, 3,676 Java LOC total, +241 LOC, 2 days
- Projects are small (≈5–10 files, <1MB each); performance target met (< 5s render confirmed)
- Tech debt: EDIT-06 partial, EDIT-07 unverified, missing VERIFICATION.md for phases 01/05/07, no automated tests for About dialog

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
| TableCell<T, Void> for action columns | No cell value needed; only graphic (button) | ✓ Good — clean pattern, reusable |
| Italian prompt preamble in ChatGPT URL | Tool targets Italian-speaking devs; hardcoded per product intent | ✓ Good — no i18n overhead |
| version.properties via Gradle processResources | Build-time injection avoids runtime classpath scan | ✓ Good — version always accurate |
| addEventFilter(MOUSE_PRESSED) on AI button | evt.consume() on ActionEvent doesn't block TableView MouseEvent | ✓ Good — WR-01 fix; correct event model |

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
*Last updated: 2026-04-29 — Milestone v0.5.0 started: Undo, Fix & Licenza*
