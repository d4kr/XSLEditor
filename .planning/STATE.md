---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: paused
paused_at: Completed 07-02-PLAN.md — PreviewController wired, PDFBox rendering fix, PREV-01..04 human-verified. Phase 07 fully complete.
last_updated: "2026-04-20T14:00:00Z"
progress:
  total_phases: 9
  completed_phases: 5
  total_plans: 14
  completed_plans: 17
  percent: 100
---

# Project State: XLSEditor

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-14)

**Core value:** A developer can open a project, edit XSLT templates, trigger a render, and see the PDF — all in one window without context switching.
**Current focus:** Phase 08 — error-log-panel

---

## Current Position

Phase: 07 (pdf-preview-panel) — COMPLETE
Plan: Not started
**Milestone:** v1.0
**Phase:** 8 of 9 (error-log-panel)
**Status:** Ready to plan

---

## Completed Phases

- **Phase 01** — JavaFX Application Shell (2026-04-14): fat JAR shell with three-zone layout, WebView PDF scaffold, close-confirmation dirty-state pattern
  - **Verification:** 2026-04-15 — status: human_needed (all automated checks PASS, 4 visual items for human confirmation)
  - **Report:** `.planning/phases/01-javafx-application-shell/01-VERIFICATION.md`

---

## Key Decisions

| Decision | Context | Date |
|----------|---------|------|
| JavaFX for UI | Standard modern Java desktop framework | 2026-04-14 |
| RichTextFX for editor | Native JavaFX, CSS-based syntax highlighting | 2026-04-14 |
| PDFViewerFX for PDF preview | Open-source, JavaFX-native, PDFBox-based | 2026-04-14 |
| Shadow plugin migrated to com.gradleup.shadow 9.0.0-beta12 | com.github.johnrengelman.shadow 8.1.1 incompatible with Gradle 9 (MissingPropertyException: mode) | 2026-04-14 |
| WebView for PDF preview (Phase 1 scaffold) | PDFViewerFX not on Maven Central; JavaFX WebView loads file:// URIs natively via WebKit | 2026-04-14 |
| Fine granularity phasing | 9 phases for focused, reviewable increments | 2026-04-14 |
| Interactive workflow mode | Confirmation at key decision points | 2026-04-14 |
| Backend pipeline complete (T1–T10) | Saxon + FOP pipeline ready before UI work begins | 2026-04-14 |
| EditorTab uses public final fields (data carrier) | No setters needed; Plan 02 EditorController owns tab lifecycle | 2026-04-18 |
| Dirty state via Bindings.not(atMarkedPositionProperty()) | UndoManager-based binding handles undo-back-to-clean correctly | 2026-04-18 |
| EditorController uses Consumer&lt;Boolean&gt; dirtyCallback | Decouples sub-controller from MainController; no upward import | 2026-04-18 |
| Per-CodeArea Ctrl+S via WellBehavedFX Nodes.addInputMap | Scene-level handler causes focus bugs; per-node InputMap is the correct pattern | 2026-04-18 |
| Skeleton production classes in Wave 0 | Java @Disabled tests still compile; skeletons with correct signatures allow test stubs to compile and remain green until Wave 1 replaces bodies | 2026-04-19 |
| SearchDialog.search() as static method | Extracted from Task.call() lambda for testability; follows RenderOrchestrator pattern | 2026-04-19 |
| dialog.show() non-blocking for SearchDialog | Allows user to keep editing while background search runs; showAndWait() would block FX thread | 2026-04-19 |
| SearchExecutor shutdownNow() dual guard | Called at new search start (cancel prior Task) AND in setOnCloseRequest (T-05-10 mitigation) | 2026-04-19 |
| Initial highlight threshold 500 chars | Files < 500 chars highlighted sync on FX thread (safe); >= 500 submitted off-thread via hlExecutor to avoid stutter | 2026-04-19 |
| highlightSub.unsubscribe() before hlExecutor.shutdownNow() | Unsubscribe first releases CodeArea strong reference; then executor shutdown releases thread — ordering required for correct GC | 2026-04-19 |
| PDFBox for WebView PDF rendering | macOS JavaFX WebView does not render PDFs via file:// URIs; PDFBox 2.0.31 renders pages as PNG images embedded as base64 HTML | 2026-04-20 |
| PDFBox 150 DPI page rendering | Each page rendered at 150 DPI as PNG, base64-encoded, assembled into HTML loaded via loadContent() — avoids same-URI WebKit cache issue | 2026-04-20 |

---

## Blockers / Concerns

_(none at start)_

---

## Pending Todos

_(none yet)_

---

## Session Continuity

**Last session:** 2026-04-20
**Paused at:** Phase 08 context gathered. Ready to plan.

---

## Notes

- Backend (pipeline, error handling, log) is fully implemented per commits T1–T10.3
- No UI code exists yet — Phase 1 starts from scratch on the JavaFX layer
- Codebase map is at `.planning/codebase/` (7 documents, analyzed 2026-04-14)
- Tech debt items tracked in `.planning/codebase/CONCERNS.md` — addressed in Phase 9 (tests)
