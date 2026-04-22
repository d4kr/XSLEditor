---
phase: 05-editor-features-syntax-navigation
plan: 01
subsystem: editor-tests
tags: [tdd, wave-0, test-stubs, syntax-highlighting, autocomplete, occurrence, href, search]
dependency_graph:
  requires: []
  provides:
    - XmlSyntaxHighlighterTest (Wave 0 stub)
    - AutocompleteProviderTest (Wave 0 stub)
    - OccurrenceHighlighterTest (Wave 0 stub)
    - HrefExtractorTest (Wave 0 stub — path traversal security gate)
    - SearchTaskTest (Wave 0 stub)
  affects:
    - Wave 1 plans (05-02, 05-03): test stubs become enabled after production classes are implemented
tech_stack:
  added:
    - XmlSyntaxHighlighter skeleton (public final, static method, RichTextFX StyleSpans)
    - AutocompleteProvider skeleton (public final, static getMatches + triggerAt)
    - OccurrenceHighlighter skeleton (public final, static findOccurrences + applyTo)
    - HrefExtractor skeleton (public final, static extractHref returning Optional<Path>)
    - SearchDialog skeleton (with SearchHit record and static search() helper)
  patterns:
    - Wave 0 TDD: @Disabled stubs reference real production class APIs, fail to compile if API drifts
    - Skeleton production classes: compilable stubs with correct signatures, Wave 1 replaces body
    - No-JavaFX test pattern: plain JUnit 5, no Platform.startup(), analogous to ProjectConfigTest
    - @TempDir fixture: used in HrefExtractorTest and SearchTaskTest for file system isolation
key_files:
  created:
    - src/test/java/ch/ti/gagi/xsleditor/ui/XmlSyntaxHighlighterTest.java
    - src/test/java/ch/ti/gagi/xsleditor/ui/AutocompleteProviderTest.java
    - src/test/java/ch/ti/gagi/xsleditor/ui/OccurrenceHighlighterTest.java
    - src/test/java/ch/ti/gagi/xsleditor/ui/HrefExtractorTest.java
    - src/test/java/ch/ti/gagi/xsleditor/ui/SearchTaskTest.java
    - src/main/java/ch/ti/gagi/xsleditor/ui/XmlSyntaxHighlighter.java
    - src/main/java/ch/ti/gagi/xsleditor/ui/AutocompleteProvider.java
    - src/main/java/ch/ti/gagi/xsleditor/ui/OccurrenceHighlighter.java
    - src/main/java/ch/ti/gagi/xsleditor/ui/HrefExtractor.java
    - src/main/java/ch/ti/gagi/xsleditor/ui/SearchDialog.java
  modified: []
decisions:
  - Skeleton production classes created in Wave 0 so test stubs compile: @Disabled prevents execution but not compilation; skeletons have correct method signatures with no-op bodies to be replaced in Wave 1/2
  - SearchDialog.search() designed as a package-level static method for testability, following RenderOrchestrator pattern
  - OccurrenceHighlighter.findOccurrencesReturnsEmptyForSingleCharToken guards token.length() < 2 (not just blank) to match the planned implementation guard
metrics:
  duration: 164s
  completed_date: "2026-04-19"
  tasks_completed: 2
  tasks_total: 2
  files_created: 10
  files_modified: 0
---

# Phase 05 Plan 01: Wave 0 Test Stubs Summary

**One-liner:** Five @Disabled JUnit 5 test stubs with meaningful assertions covering syntax highlighting, autocomplete, occurrence highlighting, href path traversal guard, and multi-file search, backed by compilable production class skeletons.

---

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | XmlSyntaxHighlighter and AutocompleteProvider test stubs | 67151bd | XmlSyntaxHighlighterTest.java, AutocompleteProviderTest.java + 5 skeleton production classes |
| 2 | OccurrenceHighlighter, HrefExtractor, and SearchTask test stubs | 81b6e5d | OccurrenceHighlighterTest.java, HrefExtractorTest.java, SearchTaskTest.java |

---

## Verification Results

- `./gradlew test` exits 0 — all five @Disabled stubs compile but do not execute
- All five test files exist in `src/test/java/ch/ti/gagi/xsleditor/ui/`
- `totalSpanLengthEqualsInputLength` present in XmlSyntaxHighlighterTest
- `getMatchesReturnsSubsetMatchingPrefix` present in AutocompleteProviderTest
- `findOccurrencesReturnsEmptyForBlankToken` present in OccurrenceHighlighterTest
- `pathTraversalIsRejected` present in HrefExtractorTest (security regression gate T-05-01)
- `findsMatchInFixtureFile` present in SearchTaskTest

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Created skeleton production classes so test stubs compile**
- **Found during:** Task 1 verification
- **Issue:** Java compiles @Disabled test classes — `cannot find symbol` for XmlSyntaxHighlighter, AutocompleteProvider, OccurrenceHighlighter, HrefExtractor, and SearchDialog. The plan required `./gradlew test exits 0` but the referenced classes did not exist yet.
- **Fix:** Created five skeleton production classes with correct method signatures (matching the production API shape documented in PATTERNS.md and RESEARCH.md) and no-op bodies. Wave 1 will replace the no-op bodies with real implementations. This is the standard TDD RED-phase pattern for statically typed languages: tests compile against the API surface, assertions fail when enabled.
- **Files created:** XmlSyntaxHighlighter.java, AutocompleteProvider.java, OccurrenceHighlighter.java, HrefExtractor.java, SearchDialog.java
- **Commits:** 67151bd (skeleton classes included in Task 1 commit)

---

## Known Stubs

| Stub | File | Reason |
|------|------|--------|
| XmlSyntaxHighlighter.computeHighlighting returns single unstyled span | src/main/java/ch/ti/gagi/xsleditor/ui/XmlSyntaxHighlighter.java | Skeleton only — Wave 1 replaces with regex-based XML highlighter |
| AutocompleteProvider.getMatches always returns empty list | src/main/java/ch/ti/gagi/xsleditor/ui/AutocompleteProvider.java | Skeleton only — Wave 1 replaces with full keyword list and filter |
| OccurrenceHighlighter.findOccurrences always returns empty list | src/main/java/ch/ti/gagi/xsleditor/ui/OccurrenceHighlighter.java | Skeleton only — Wave 1 implements scan |
| HrefExtractor.extractHref always returns Optional.empty() | src/main/java/ch/ti/gagi/xsleditor/ui/HrefExtractor.java | Skeleton only — Wave 1 implements href parsing and path resolution |
| SearchDialog.search always returns empty list | src/main/java/ch/ti/gagi/xsleditor/ui/SearchDialog.java | Skeleton only — Wave 2 implements Files.walk file scanner |

These stubs are intentional Wave 0 artifacts. They are replaced by Wave 1 (plans 05-02 through 05-04) and Wave 2 (plan 05-05). The test @Disabled annotations will be removed once each production class is implemented.

---

## Threat Surface Scan

| Flag | File | Description |
|------|------|-------------|
| threat_flag: path-traversal | src/main/java/ch/ti/gagi/xsleditor/ui/HrefExtractor.java | HrefExtractor.extractHref() will resolve filesystem paths from user-controlled href values; the skeleton returns Optional.empty() (safe default). Wave 1 must implement the traversal guard confirmed by the pathTraversalIsRejected test. |

---

## Self-Check: PASSED

All created files exist on disk:
- src/test/java/ch/ti/gagi/xsleditor/ui/XmlSyntaxHighlighterTest.java - FOUND
- src/test/java/ch/ti/gagi/xsleditor/ui/AutocompleteProviderTest.java - FOUND
- src/test/java/ch/ti/gagi/xsleditor/ui/OccurrenceHighlighterTest.java - FOUND
- src/test/java/ch/ti/gagi/xsleditor/ui/HrefExtractorTest.java - FOUND
- src/test/java/ch/ti/gagi/xsleditor/ui/SearchTaskTest.java - FOUND
- src/main/java/ch/ti/gagi/xsleditor/ui/XmlSyntaxHighlighter.java - FOUND
- src/main/java/ch/ti/gagi/xsleditor/ui/AutocompleteProvider.java - FOUND
- src/main/java/ch/ti/gagi/xsleditor/ui/OccurrenceHighlighter.java - FOUND
- src/main/java/ch/ti/gagi/xsleditor/ui/HrefExtractor.java - FOUND
- src/main/java/ch/ti/gagi/xsleditor/ui/SearchDialog.java - FOUND

Commits verified:
- 67151bd - FOUND
- 81b6e5d - FOUND

Build status: ./gradlew test exits 0
