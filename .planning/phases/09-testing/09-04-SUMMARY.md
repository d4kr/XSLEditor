---
phase: 09-testing
plan: "04"
subsystem: preview-integration-tests
tags: [testing, integration, preview, pipeline, pdf, xslt]
dependency_graph:
  requires: ["09-03"]
  provides: ["TEST-07", "TEST-08"]
  affects: ["ErrorManager"]
tech_stack:
  added: []
  patterns: ["JUnit 5 @Tag(integration)", "real Saxon+FOP pipeline, no mocks"]
key_files:
  created:
    - src/test/java/ch/ti/gagi/xsleditor/preview/PreviewManagerIntegrationTest.java
  modified:
    - src/main/java/ch/ti/gagi/xsleditor/error/ErrorManager.java
decisions:
  - "SAXParseException classified as XSLT error type in ErrorManager (malformed XML in XSLT files caught at dependency-graph stage)"
metrics:
  duration: "~8 minutes"
  completed: "2026-04-21"
  tasks_completed: 1
  files_changed: 2
---

# Phase 9 Plan 04: PreviewManager Integration Tests Summary

Integration tests (TEST-07, TEST-08) for the full backend pipeline via PreviewManager — real Saxon + FOP, no mocks.

## What Was Built

Two integration tests covering the complete render pipeline
(DependencyResolver → ValidationEngine → LibraryPreprocessor → RenderEngine → PreviewManager):

### TEST-07: Happy-path PDF (fullPipelineProducesNonEmptyPdfWithMagicBytes)

Given a `Project` pointing at `fixtures/identity.xsl` + `fixtures/input.xml`, calling
`PreviewManager.generatePreview()` returns:
- `preview.success() == true`
- `preview.outdated() == false`
- `preview.pdf()` is non-null, non-empty, starts with `%PDF` magic bytes

**Result:** PASS (0.292s)

### TEST-08: Invalid-XSLT failure (invalidXsltProducesPreviewFailureWithXsltTypeError)

Given a `Project` with `fixtures/invalid.xsl` (malformed XML — unclosed `<broken>` tag),
`generatePreview()` returns:
- `preview.success() == false`
- `preview.outdated() == true`
- `preview.pdf() == null`
- At least one `PreviewError` with `type() == "XSLT"`
- `message()` is non-null and non-blank

**Result:** PASS (0.006s)

## Full Phase 9 Test Count

| Plan | Test Class(es) | Tests |
|------|----------------|-------|
| 09-01 (ErrorManager tests) | ErrorManagerTest | 10 |
| 09-01 (LogManager tests) | LogManagerTest | 6 |
| 09-01 (ValidationEngine) | ValidationEngineTest | 4 |
| 09-01 (RenderEngine) | RenderEngineTest | 5 |
| 09-02 (UI tests) | AutocompleteProviderTest, EditorTabTest, HrefExtractorTest, OccurrenceHighlighterTest, PreviewControllerTest, RenderControllerTest, SearchTaskTest, XmlSyntaxHighlighterTest | 26 |
| 09-03 (fixtures) | — (fixture files, no test class) | 0 |
| 09-04 (integration) | PreviewManagerIntegrationTest | 2 |
| Pre-Phase-9 (model/UI) | ProjectConfigTest, ProjectContextTest | 33 |
| **Total** | **15 classes** | **86** |

Phase 9 added: ~53 tests across plans 01, 02, 04 (excluding pre-existing model tests).

## Final ./gradlew test Result

```
BUILD SUCCESSFUL in 7s
86 tests total, 0 failures, 0 skipped
```

Zero regressions against pre-Phase-9 test count.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] ErrorManager.fromException maps SAXParseException to XSLT type**

- **Found during:** Task 1 (GREEN phase) — TEST-08 failed with `expected: <XSLT> but was: <UNKNOWN>`
- **Issue:** `DependencyResolver.buildGraph` parses the XSLT file as XML using `DocumentBuilder.parse()`. When `invalid.xsl` contains malformed XML (unclosed `<broken>` element), a `SAXParseException` is thrown. The `ErrorManager.fromException` method only mapped `SaxonApiException` and `TransformerException` to type `"XSLT"` — `SAXParseException` fell through to `"UNKNOWN"`.
- **Fix:** Added `|| e instanceof SAXParseException` to the XSLT branch in `fromException`. Also extended `extractLocation` to extract `systemId` and `lineNumber` from `SAXParseException` for proper error location reporting.
- **Files modified:** `src/main/java/ch/ti/gagi/xsleditor/error/ErrorManager.java`
- **Commit:** e54dfd6
- **Rationale:** `SAXParseException` is semantically an XML/XSLT parsing error — mapping it to `UNKNOWN` was incorrect. This fix also improves error location reporting for malformed XSLT files.

## Known Stubs

None. Both tests exercise the full live pipeline and assert on real outputs.

## Self-Check: PASSED
