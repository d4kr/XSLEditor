---
gsd_state_version: 1.0
milestone: v0.4.0
milestone_name: GitHub Releases & Distribution
status: roadmap ready
last_updated: "2026-04-24T00:00:00Z"
progress:
  total_phases: 5
  completed_phases: 0
  total_plans: 0
  completed_plans: 0
---

# Project State: XSLEditor

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-24)

**Core value:** A developer can open a project, edit XSLT templates, trigger a render, and see the PDF — all in one window without context switching.
**Current focus:** v0.4.0 — GitHub Releases & Distribution (Phase 19 next)

---

## Current Position

Phase: 19 — Launcher Shim & Local Build (not started)
Plan: —
Status: Roadmap ready, awaiting first plan
Last activity: 2026-04-24 — v0.4.0 roadmap created (Phases 19–23)

Progress: [__________] 0% (v0.4.0 — 0/5 phases)

---

## Completed Phases

- Phases 1–9 — v0.1.0 MVP — shipped 2026-04-21
- Phase 10 — Saxon URI Fix — 2026-04-22
- Phase 11 — About Dialog — 2026-04-22
- Phase 12 — AI Assist in Error Log — 2026-04-22
- Phase 13 — Full Project Rename — 2026-04-22
- Phase 14 — Version & Icon Housekeeping — 2026-04-23
- Phase 15 — Dark Theme CSS Fixes — 2026-04-23
- Phase 16 — Log Panel Layout — 2026-04-23
- Phase 17 — Encoding Investigation & Fix — 2026-04-23
- Phase 18 — README Rewrite — 2026-04-24

---

## v0.4.0 Phase Map

| Phase | Name | Requirements | Status |
|-------|------|--------------|--------|
| 19 | Launcher Shim & Local Build | BUILD-01, BUILD-02, BUILD-03, BUILD-04 | Not started |
| 20 | CI Skeleton — Unsigned Release Pipeline | CI-01, CI-02, CI-03, MACOS-01, MACOS-02, WIN-01, WIN-02, REL-01, REL-02, REL-03 | Not started |
| 21 | macOS Signing | MACOS-03 | Not started |
| 22 | macOS Notarization | MACOS-04 | Not started |
| 23 | Signing Documentation | SIGN-01 | Not started |

---

## Key Decisions

- v0.4.0 goal: CI/CD + distribution pipeline via GitHub Actions, jpackage, signing
- Launcher.java shim is mandatory (Phase 19) — JavaFX Application subclass cannot be the fat JAR main class
- All runners must use Liberica JDK+FX 21 — Temurin omits libjfxwebkit required for WebView
- Fat JAR built once on ubuntu-latest; downloaded by platform packaging jobs (no duplicate Gradle runs)
- macOS signing requires Apple Developer ID certificate stored as GitHub secret (p12 + password)
- Notarization (Phase 22) requires Apple Developer Program membership ($99/yr)
- Windows MSI unsigned in v0.4.0 — Authenticode signing deferred (requires EV cert ~$200–500/yr)
- Release notes auto-generated from git log between tags via softprops/action-gh-release@v2
- Tags with `-` auto-marked as pre-release; clean tags as full release

See .planning/PROJECT.md Key Decisions table for full log.

---

## Accumulated Context

- v0.3.0 shipped 2026-04-24: 5 phases (14–18), UI polish, encoding fix, README
- Tech debt carried: EDIT-06 partial, EDIT-07 unverified, no automated tests for About dialog
- Fat JAR build already works via Gradle shadowJar (com.gradleup.shadow 9.0.0-beta12)
- jpackage requires JDK 14+; Java 21 already in use — compatible
- Critical pitfalls documented in research/SUMMARY.md (C-01 through C-06)
- macOS x64 runner (macos-15-intel) available until Aug 2027 only

---

## Blockers / Concerns

- Phase 22 (notarization) requires active Apple Developer Program membership ($99/yr) — confirm before starting
