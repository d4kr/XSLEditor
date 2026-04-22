---
phase: 09-testing
plan: "01"
subsystem: testing
tags: [unit-tests, library-preprocessor, dependency-resolver, junit5, tempdir]
dependency_graph:
  requires: []
  provides: [TEST-01, TEST-02]
  affects: []
tech_stack:
  added: []
  patterns: [JUnit5-TempDir-inline-fixtures]
key_files:
  created:
    - src/test/java/ch/ti/gagi/xsleditor/library/LibraryPreprocessorTest.java
    - src/test/java/ch/ti/gagi/xsleditor/dependency/DependencyResolverTest.java
  modified: []
decisions:
  - "Used inline @TempDir file writes for all filesystem fixtures per D-02 — no static fixture files on disk"
  - "Helper stylesheet() method in DependencyResolverTest encapsulates namespace-aware XSLT fixture generation"
metrics:
  duration: "74 seconds"
  completed: "2026-04-21"
  tasks_completed: 2
  files_created: 2
  files_modified: 0
requirements:
  - TEST-01
  - TEST-02
---

# Phase 09 Plan 01: Library and Dependency Unit Tests Summary

Unit tests for LibraryPreprocessor and DependencyResolver — directive detection/merge/cache/error + include/import parsing/graph/cycle detection, all green with zero regressions.

## What Was Built

### Task 1: LibraryPreprocessorTest (TEST-01)

Five tests in `src/test/java/ch/ti/gagi/xsleditor/library/LibraryPreprocessorTest.java`:

| Test | Scenario | API Called |
|------|----------|------------|
| `detectsSingleLibraryDirective` | Happy path: single `<?LIBRARY name?>` in content | `detectLibraries(String)` |
| `detectsMultipleLibraryDirectivesInOrder` | Multiple directives extracted in source order | `detectLibraries(String)` |
| `mergesLibraryContentInPlace` | Directive replaced verbatim with library file contents | `mergeLibraries(Path, String)` |
| `cachesLibraryContentAcrossMultipleDirectives` | Duplicate directive hits cache, both occurrences substituted | `mergeLibraries(Path, String)` |
| `throwsLibraryProcessingExceptionWhenFileMissing` | Missing `.xsl` file throws `LibraryProcessingException` with "Library file not found" | `mergeLibraries(Path, String)` |

All 5 tests use `@TempDir` with `Files.writeString` inline. No static fixtures. No Mockito.

### Task 2: DependencyResolverTest (TEST-02)

Five tests in `src/test/java/ch/ti/gagi/xsleditor/dependency/DependencyResolverTest.java`:

| Test | Scenario | API Called |
|------|----------|------------|
| `parseIncludesReturnsHrefsInOrder` | Two `xsl:include` hrefs returned in document order | `parseIncludes(Path)` |
| `parseImportsReturnsHrefs` | Single `xsl:import` href extracted | `parseImports(Path)` |
| `buildGraphDiscoversTransitiveDependencies` | Three-file chain: a→b→c with mixed include/import | `buildGraph(Path, Path)` |
| `buildGraphDetectsCircularDependency` | a includes b, b includes a — `IllegalStateException` starting "Circular dependency detected" | `buildGraph(Path, Path)` |
| `buildGraphReturnsSingleEntryForLeafStylesheet` | Leaf stylesheet: graph has 1 entry, no dependencies | `buildGraph(Path, Path)` |

All XSLT fixtures declare `xmlns:xsl="http://www.w3.org/1999/XSL/Transform"` (required by namespace-aware `DocumentBuilderFactory`). A private `stylesheet(String body)` helper generates minimal valid XSLT documents. No Mockito.

## Test Results

| Suite | Before | After | Delta |
|-------|--------|-------|-------|
| Existing tests (model + ui) | 59 passing | 59 passing | 0 |
| LibraryPreprocessorTest | — | 5 passing | +5 |
| DependencyResolverTest | — | 5 passing | +5 |
| **Total** | **59** | **69** | **+10** |

`./gradlew test` exits 0. Zero regressions.

## Commits

| Task | Hash | Message |
|------|------|---------|
| 1 | c403264 | test(09-01): add LibraryPreprocessorTest — directive detection, merge, cache, missing-file error |
| 2 | 3a01da4 | test(09-01): add DependencyResolverTest — parse hrefs, transitive graph, circular detection |

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None.

## Self-Check: PASSED

- `src/test/java/ch/ti/gagi/xsleditor/library/LibraryPreprocessorTest.java` — FOUND
- `src/test/java/ch/ti/gagi/xsleditor/dependency/DependencyResolverTest.java` — FOUND
- Commit c403264 — FOUND
- Commit 3a01da4 — FOUND
