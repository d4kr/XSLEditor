# Roadmap: XLSEditor

## Milestones

- ✅ **v1.0 MVP** — Phases 1–9 (shipped 2026-04-21)
- 🚧 **v1.1 Developer UX Improvements** — Phases 10–12 (in progress)

## Phases

<details>
<summary>✅ v1.0 MVP (Phases 1–9) — SHIPPED 2026-04-21</summary>

- [x] Phase 1: JavaFX Application Shell (1/1 plans) — completed 2026-04-14
- [x] Phase 2: Project Management (2/2 plans) — completed 2026-04-15
- [x] Phase 3: File Tree View (3/3 plans) — completed 2026-04-17
- [x] Phase 4: Multi-Tab Editor Core (3/3 plans) — completed 2026-04-18
- [x] Phase 5: Editor Features (5/5 plans) — completed 2026-04-19
- [x] Phase 6: Render Pipeline Integration (2/2 plans) — completed 2026-04-19
- [x] Phase 7: PDF Preview Panel (2/2 plans) — completed 2026-04-20
- [x] Phase 8: Error & Log Panel (2/2 plans) — completed 2026-04-20
- [x] Phase 9: Testing (4/4 plans) — completed 2026-04-21

Full archive: `.planning/milestones/v1.0-ROADMAP.md`

</details>

### 🚧 v1.1 Developer UX Improvements (In Progress)

**Milestone Goal:** Fix Saxon error navigation, add an About dialog, and add AI-assist links in the error log so developers can instantly look up pipeline errors with ChatGPT.

- [x] **Phase 10: Saxon URI Fix** — Fix URI-decode bug so click-to-navigate works on macOS for Saxon runtime errors (completed 2026-04-21)
- [ ] **Phase 11: About Dialog** — Add Help menu entry and About dialog showing version, stack, credits, and license
- [ ] **Phase 12: AI Assist in Error Log** — Add a ChatGPT button per error row that opens a pre-filled query in the browser

## Phase Details

### Phase 10: Saxon URI Fix
**Goal**: Saxon runtime error click-to-navigate works correctly on macOS
**Depends on**: Phase 9 (v1.0 complete)
**Requirements**: ERR-04
**Success Criteria** (what must be TRUE):
  1. User triggers a render that produces a Saxon runtime error referencing a source file
  2. User clicks the error row and the editor opens at the correct line in the correct file
  3. Errors whose systemId contains a percent-encoded `file://` URI (e.g. `file:///path/to/my%20file.xsl`) navigate correctly without throwing an exception
**Plans**: 1 plan

Plans:
- [x] 10-01-PLAN.md — Complete resolveFilePath() exception safety, make methods package-private, and add PreviewManagerTest unit tests

### Phase 11: About Dialog
**Goal**: Users can view app version, runtime stack, credits, and license from the Help menu
**Depends on**: Phase 10
**Requirements**: ABOUT-01, ABOUT-02, ABOUT-03, ABOUT-04, ABOUT-05
**Success Criteria** (what must be TRUE):
  1. User opens Help menu and sees an "About XLSEditor" menu item
  2. Clicking About opens a dialog (not a new window)
  3. The dialog shows the current app version (e.g. 0.1.0)
  4. The dialog shows the versions of Java, Saxon-HE, Apache FOP, and JavaFX in the running environment
  5. The dialog shows author/credits and license information (text or link)
**Plans**: 3 plans

Plans:
- [ ] 11-01-PLAN.md — Correct build.gradle version to 0.1.0, add processResources for version.properties, expose static HostServices accessor in XLSEditorApp
- [ ] 11-02-PLAN.md — Create AboutDialog.java with full dark-themed programmatic UI (title, runtime stack, author, license hyperlink)
- [ ] 11-03-PLAN.md — Wire Help menu in main.fxml and handleAbout() in MainController; human verify checkpoint

### Phase 12: AI Assist in Error Log
**Goal**: Developers can send any error directly to ChatGPT with one click, pre-filled with the error message
**Depends on**: Phase 11
**Requirements**: ERR-06
**Success Criteria** (what must be TRUE):
  1. Each row in the error log table shows an AI assist action (button or icon)
  2. Clicking the AI assist action opens the default browser to ChatGPT with the error message text pre-filled in the prompt
  3. The action is available for all severity levels (error, warning, info)
  4. Clicking the AI assist action does not navigate the editor or alter the error log state
**Plans**: TBD

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. JavaFX Application Shell | v1.0 | 1/1 | Complete | 2026-04-14 |
| 2. Project Management | v1.0 | 2/2 | Complete | 2026-04-15 |
| 3. File Tree View | v1.0 | 3/3 | Complete | 2026-04-17 |
| 4. Multi-Tab Editor Core | v1.0 | 3/3 | Complete | 2026-04-18 |
| 5. Editor Features | v1.0 | 5/5 | Complete | 2026-04-19 |
| 6. Render Pipeline Integration | v1.0 | 2/2 | Complete | 2026-04-19 |
| 7. PDF Preview Panel | v1.0 | 2/2 | Complete | 2026-04-20 |
| 8. Error & Log Panel | v1.0 | 2/2 | Complete | 2026-04-20 |
| 9. Testing | v1.0 | 4/4 | Complete | 2026-04-21 |
| 10. Saxon URI Fix | v1.1 | 1/1 | Complete | 2026-04-21 |
| 11. About Dialog | v1.1 | 0/3 | Not started | - |
| 12. AI Assist in Error Log | v1.1 | 0/? | Not started | - |

---
*Roadmap created: 2026-04-14*
*v1.0 archived: 2026-04-21*
*v1.1 phases added: 2026-04-21*
