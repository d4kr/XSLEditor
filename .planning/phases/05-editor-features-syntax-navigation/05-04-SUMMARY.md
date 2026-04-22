---
phase: 05-editor-features-syntax-navigation
plan: 04
subsystem: editor-ui
tags: [wiring, syntax-highlighting, autocomplete, occurrence, go-to-definition, richtextfx, css]
dependency_graph:
  requires:
    - "05-02"  # XmlSyntaxHighlighter + AutocompleteProvider
    - "05-03"  # OccurrenceHighlighter + HrefExtractor
  provides:
    - EditorController.buildTab() wired with all four Phase 5 features
    - navigateTo(Path, int, int) method for log panel and search dialog
    - main.css Phase 5 syntax highlight + occurrence CSS classes
  affects:
    - "05-05"  # SearchDialog will call navigateTo()
tech_stack:
  added:
    - ReactFX multiPlainChanges().successionEnds(300ms) subscription pattern for async highlighting
    - Per-tab single-thread ExecutorService (daemon) for off-thread regex computation
    - JavaFX Task<StyleSpans> for background highlight computation with FX-thread onSucceeded callback
    - File size guard (> 5MB skip) and initial-highlight threshold (< 500 chars sync, >= 500 off-thread)
    - MOUSE_CLICKED event filter with CharacterHit for Ctrl+Click go-to-definition
    - selectedTextProperty listener for occurrence highlight delegation
  patterns:
    - Per-CodeArea Nodes.addInputMap (established Phase 4 pattern — extended with SPACE)
    - Subscription + ExecutorService dual disposal in tab.setOnClosed (T-05-08 mitigation)
    - navigateTo delegates to openOrFocusTab then moveTo + requestFollowCaret
key_files:
  created: []
  modified:
    - src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java
    - src/main/resources/ch/ti/gagi/xsleditor/ui/main.css
decisions:
  - "Initial highlight uses 500-char threshold: < 500 is synchronous (safe for FX thread), >= 500 submits off-thread Task to avoid stutter on medium/large files"
  - "tab.setOnClosed unsubscribes highlightSub before shutdownNow — ordering matters to release CodeArea strong reference first (T-05-08)"
  - "navigateTo placed between openOrFocusTab and buildTab in source order — natural read order from tab-open to tab-navigate to tab-build"
metrics:
  duration: 138s
  completed_date: "2026-04-19"
  tasks_completed: 2
  tasks_total: 2
  files_created: 0
  files_modified: 2
---

# Phase 05 Plan 04: EditorController Wiring and CSS Summary

**One-liner:** EditorController.buildTab() wired with async ReactFX syntax highlighting, Ctrl+Space autocomplete, selection-based occurrence overlay, and Ctrl+Click go-to-definition; main.css extended with 8 Phase 5 CSS classes.

---

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Wire async highlighting, occurrence, and go-to-definition in buildTab() | e22591f | EditorController.java |
| 2 | Add Phase 5 CSS syntax-highlight classes to main.css | b7b141d | main.css |

---

## Verification Results

- `./gradlew test` exits 0 — compilation clean, all test suites pass
- `highlightSub.unsubscribe()` present in tab.setOnClosed (T-05-08 leak prevention confirmed)
- `hlExecutor.shutdownNow()` present in tab.setOnClosed (T-05-08 thread release confirmed)
- `successionEnds(Duration.ofMillis(300))` present — debounced async subscription
- `XmlSyntaxHighlighter.computeHighlighting` referenced 3 times (subscription + sync initial + async initial)
- `AutocompleteProvider.triggerAt` wired via Nodes.addInputMap(SPACE, CONTROL_DOWN)
- `OccurrenceHighlighter.applyTo` wired via selectedTextProperty listener
- `HrefExtractor.extractHref` wired via MOUSE_CLICKED event filter
- `public void navigateTo` present with moveTo + requestFollowCaret
- File size guard `5 * 1024 * 1024` present
- Initial highlight threshold `initialText.length() >= 500` present
- CSS class count: 8 (7 syntax + 1 occurrence) confirmed via grep -c
- Existing Phase 1-4 CSS rules unchanged (`.file-tree-header` still present at line 46)

---

## Deviations from Plan

None — plan executed exactly as written. Both task action blocks were applied verbatim. The file size guard, initial highlight split, and all four EDIT-0x wiring blocks match the plan specification precisely.

---

## Known Stubs

None. All four features are fully wired. The Wave 1 logic classes (XmlSyntaxHighlighter, AutocompleteProvider, OccurrenceHighlighter, HrefExtractor) were implemented in plans 05-02 and 05-03 and are now connected to the live CodeArea instances in buildTab().

---

## Threat Surface Scan

No new trust boundaries introduced beyond what the plan's threat model covers.

| Threat ID | Component | Disposition | Status |
|-----------|-----------|-------------|--------|
| T-05-06 | buildTab() highlighting subscription | mitigate | File size guard (> 5MB) applied — tooLargeForHighlighting skips executor and subscription entirely |
| T-05-07 | go-to-def MOUSE_CLICKED filter | transfer | HrefExtractor.extractHref owns path traversal guard; EditorController delegates fully |
| T-05-08 | Subscription/ExecutorService leak on tab close | mitigate | tab.setOnClosed calls highlightSub.unsubscribe() then hlExecutor.shutdownNow() |

---

## Self-Check: PASSED

Files exist on disk:
- src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java - FOUND
- src/main/resources/ch/ti/gagi/xsleditor/ui/main.css - FOUND

Commits verified:
- e22591f (Task 1 — EditorController wiring) - FOUND
- b7b141d (Task 2 — main.css CSS classes) - FOUND

Test suite: BUILD SUCCESSFUL — ./gradlew test exits 0
