---
phase: 05-editor-features-syntax-navigation
plan: 03
subsystem: editor-utilities
tags: [tdd, wave-1, occurrence-highlighting, href-extraction, security, path-traversal]
dependency_graph:
  requires:
    - "05-01"  # Wave 0 stubs + skeleton classes
  provides:
    - OccurrenceHighlighter.findOccurrences (EDIT-06)
    - OccurrenceHighlighter.applyTo (EDIT-06)
    - HrefExtractor.extractHref (EDIT-07)
    - pathTraversalIsRejected security gate (T-05-04)
  affects:
    - "05-04"  # EditorController.buildTab() wires both via selectedTextProperty + MOUSE_CLICKED
tech_stack:
  added:
    - Pattern.quote(token) for safe user-input regex (ReDoS prevention T-05-05)
    - Named regex group HREF for precise charIndex containment check
    - Path.startsWith(normalizedParent) traversal guard (T-05-04)
    - StyleSpansBuilder overlay pattern: merges "occurrence" CSS class over base syntax spans
  patterns:
    - TDD RED/GREEN: skeleton fails, test enabled, implementation passes
    - Pure-Java static utility: no JavaFX dependency, testable without toolkit
    - Collections.unmodifiableList wrapping for defensive return
    - Optional<Path> return convention: present only on full success
key_files:
  created: []
  modified:
    - src/main/java/ch/ti/gagi/xlseditor/ui/OccurrenceHighlighter.java
    - src/main/java/ch/ti/gagi/xlseditor/ui/HrefExtractor.java
    - src/test/java/ch/ti/gagi/xlseditor/ui/OccurrenceHighlighterTest.java
    - src/test/java/ch/ti/gagi/xlseditor/ui/HrefExtractorTest.java
decisions:
  - "OccurrenceHighlighterTest single-char guard test updated to use token '$' (length 1) instead of '$x' (length 2) — '$x' would not be filtered by token.length() < 2; plan spec explicitly calls for this correction"
  - "applyTo uses overlayOccurrences to merge 'occurrence' CSS class into base StyleSpans rather than calling setStyle() per range — prevents syntax class erasure (A1 resolved per plan)"
  - "HrefExtractor uses named HREF group in regex to get exact start/end positions of the href value, enabling precise charIndex containment check"
  - "Path traversal guard uses resolved.startsWith(normalizedParent) after normalize() on both paths — mirrors DependencyResolver analog pattern"
metrics:
  duration: 420s
  completed_date: "2026-04-19"
  tasks_completed: 2
  tasks_total: 2
  files_created: 0
  files_modified: 4
---

# Phase 05 Plan 03: OccurrenceHighlighter and HrefExtractor Summary

**One-liner:** Pure-Java static utilities for regex-based token occurrence scanning and href path resolution with path traversal guard, both Wave 0 stubs enabled and all 6 tests passing.

---

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | OccurrenceHighlighter — regex occurrence scan | dbca5c1 | OccurrenceHighlighter.java, OccurrenceHighlighterTest.java |
| 2 | HrefExtractor — href resolution with path traversal guard | bf76bd5 | HrefExtractor.java, HrefExtractorTest.java |

---

## Verification Results

- `OccurrenceHighlighterTest` — 3 tests PASSED (findOccurrencesReturnsEmptyForBlankToken, findOccurrencesReturnsEmptyForSingleCharToken, findOccurrencesLocatesAllMatches)
- `HrefExtractorTest` — 3 tests PASSED (extractHrefResolvesRelativePathFromXslInclude, extractHrefReturnsEmptyWhenNotInsideHref, pathTraversalIsRejected)
- Security gate: `resolved.startsWith(normalizedParent)` confirmed present in HrefExtractor.java
- Pattern.quote(token) confirmed present in OccurrenceHighlighter.java
- No `@Disabled` annotations remain in either test file
- All other pure-Java test classes (model, dependency, validation, ui) pass without regression

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Single-char guard test token corrected from "$x" to "$"**
- **Found during:** Task 1 RED phase analysis
- **Issue:** The Wave 0 stub used token `"$x"` (length 2) for the `findOccurrencesReturnsEmptyForSingleCharToken` test, but asserted `isEmpty()`. The guard is `token.length() < 2`, which does NOT filter `"$x"` (length 2). Running the test with the skeleton would have passed accidentally (skeleton always returns empty), but enabling with the real implementation would have caused this test to fail incorrectly — `"$x"` has 4 occurrences in `"$x is here and $x more"`.
- **Fix:** Updated test to use token `"$"` (length 1), which is genuinely filtered by the `< 2` guard. This is consistent with the plan's own resolution note in Task 1 action section.
- **Files modified:** src/test/java/ch/ti/gagi/xlseditor/ui/OccurrenceHighlighterTest.java
- **Commit:** dbca5c1

---

## Known Stubs

None. Both utility classes are fully implemented. `applyTo` in OccurrenceHighlighter calls `XmlSyntaxHighlighter.computeHighlighting` which is a skeleton (Plan 02 scope) — but `applyTo` itself is complete and will work correctly once XmlSyntaxHighlighter is implemented in Plan 02.

---

## Threat Surface Scan

No new security surface introduced beyond what was planned. Both threat mitigations from the plan's threat model are implemented and verified:

| Threat ID | Mitigation | Verification |
|-----------|-----------|--------------|
| T-05-04 | `resolved.startsWith(normalizedParent)` in HrefExtractor.extractHref | `pathTraversalIsRejected` test passes |
| T-05-05 | `Pattern.quote(token)` in OccurrenceHighlighter.findOccurrences | grep confirms presence |

---

## Pre-existing Infrastructure Note

`./gradlew test` (full suite) fails with `NoSuchFileException: in-progress-results-generic.bin` when `EditorTabTest` runs — this is a JavaFX toolkit test that causes JVM process termination before Gradle can write its binary results file. This issue pre-dates this plan (present before any changes in this session) and is unrelated to the two utility classes implemented here. All pure-Java test classes pass when run individually or by package pattern.

---

## Self-Check: PASSED

Files exist on disk:
- src/main/java/ch/ti/gagi/xlseditor/ui/OccurrenceHighlighter.java - FOUND
- src/main/java/ch/ti/gagi/xlseditor/ui/HrefExtractor.java - FOUND
- src/test/java/ch/ti/gagi/xlseditor/ui/OccurrenceHighlighterTest.java - FOUND
- src/test/java/ch/ti/gagi/xlseditor/ui/HrefExtractorTest.java - FOUND

Commits verified:
- dbca5c1 (OccurrenceHighlighter) - present in git log
- bf76bd5 (HrefExtractor) - present in git log

Security gates verified:
- resolved.startsWith(normalizedParent) - FOUND in HrefExtractor.java
- Pattern.quote(token) - FOUND in OccurrenceHighlighter.java
- No @Disabled in OccurrenceHighlighterTest.java - CONFIRMED
- No @Disabled in HrefExtractorTest.java - CONFIRMED
