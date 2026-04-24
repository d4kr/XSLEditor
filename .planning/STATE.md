---
gsd_state_version: 1.0
milestone: v0.4.0
milestone_name: GitHub Releases & Distribution
status: defining requirements
last_updated: "2026-04-24T00:00:00Z"
progress:
  total_phases: 0
  completed_phases: 0
  total_plans: 0
  completed_plans: 0
---

# Project State: XSLEditor

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-24)

**Core value:** A developer can open a project, edit XSLT templates, trigger a render, and see the PDF — all in one window without context switching.
**Current focus:** v0.4.0 — GitHub Releases & Distribution (requirements + roadmap phase)

---

## Current Position

Phase: Not started (defining requirements)
Plan: —
Status: Defining requirements
Last activity: 2026-04-24 — Milestone v0.4.0 started

Progress: [__________] 0% (v0.4.0)

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

## Key Decisions

- v0.4.0 goal: CI/CD + distribution pipeline via GitHub Actions, jpackage, signing
- macOS signing requires Apple Developer ID certificate stored as GitHub secret (p12 + password)
- Windows build cross-compiles on Windows runner (jpackage is platform-native)
- Release notes auto-generated from git log between tags

See .planning/PROJECT.md Key Decisions table for full log.

---

## Accumulated Context

- v0.3.0 shipped 2026-04-24: 5 phases (14–18), UI polish, encoding fix, README
- Tech debt carried: EDIT-06 partial, EDIT-07 unverified, no automated tests for About dialog
- Fat JAR build already works via Gradle shadowJar (com.gradleup.shadow 9.0.0-beta12)
- jpackage requires JDK 14+; Java 21 already in use — compatible
- macOS signing: requires Developer ID Application cert; notarization optional for v0.4.0

---

## Blockers / Concerns

None.
