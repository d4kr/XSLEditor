---
phase: "09-testing"
plan: "03"
subsystem: "render"
tags: ["testing", "render-engine", "saxon", "fop", "fixtures", "xslt", "xsl-fo", "pdf"]
dependency_graph:
  requires: []
  provides: ["TEST-04", "shared-test-fixtures"]
  affects: ["09-04-integration-tests"]
tech_stack:
  added: []
  patterns: ["real-pipeline-testing", "shared-fixture-files", "tdd-red-green"]
key_files:
  created:
    - src/test/resources/fixtures/identity.xsl
    - src/test/resources/fixtures/input.xml
    - src/test/resources/fixtures/invalid.xsl
    - src/test/java/ch/ti/gagi/xlseditor/render/RenderEngineTest.java
  modified: []
decisions:
  - "Filesystem path (Paths.get) used for fixture loading — working dir is repo root in Gradle test runs"
  - "No @Tag(integration) added — tests run with every ./gradlew test per D-04 intent"
  - "invalid.xsl uses unclosed <broken> element causing Saxon XML parse error SXXP0003"
metrics:
  duration_seconds: 95
  completed_date: "2026-04-21T11:36:07Z"
  tasks_completed: 2
  files_created: 4
  files_modified: 0
---

# Phase 9 Plan 3: Shared Test Fixtures and RenderEngineTest Summary

Real Saxon+FOP render pipeline exercised end-to-end via five unit tests using minimal XSLT/XML/FO fixture files shared with Phase 9 Plan 4 integration tests.

## What Was Built

### Shared Test Fixtures (`src/test/resources/fixtures/`)

Three minimal fixture files created and shared across Phase 9 plans:

| File | Purpose |
|------|---------|
| `identity.xsl` | Valid XSLT 1.0 that transforms `<doc><item>TEXT</item></doc>` to minimal XSL-FO with `fo:block` — used by RenderEngineTest (Plan 03) and integration tests (Plan 04) |
| `input.xml` | Minimal XML: `<doc><item>hello</item></doc>` — the "hello" value propagates through the pipeline to the PDF |
| `invalid.xsl` | Intentionally malformed XSLT: unclosed `<broken>` element inside `xsl:template` — used by failure-path tests to assert Saxon throws `SaxonApiException` |

### `RenderEngineTest` (5 tests — TEST-04)

All tests use `new RenderEngine()` directly with no mocks (per D-04):

1. **`compilesValidXsltFromPath()`** — `compileXslt(Path)` returns non-null `XsltExecutable`
2. **`compilesValidXsltFromString()`** — `compileXslt(String)` overload also works
3. **`compilingMalformedXsltThrowsSaxonApiException()`** — Saxon raises `SaxonApiException` on `invalid.xsl`
4. **`transformsXmlToFoStringContainingBlockAndItemText()`** — FO string contains `fo:block` and item value `hello`
5. **`rendersFoToPdfWithPdfMagicBytes()`** — FOP renders FO to PDF bytes starting with `%PDF`

## Test Results

```
testsuite tests="5" skipped="0" failures="0" errors="0" time="0.375s"
```

Pipeline runtime note: FOP init takes ~100ms on first render. Full `./gradlew test` passes with no regressions.

## Commits

| Hash | Description |
|------|-------------|
| `00324f2` | feat(09-03): add shared test fixtures for RenderEngine and integration tests |
| `1476096` | test(09-03): add failing RenderEngineTest for Saxon+FOP pipeline (RED+GREEN — implementation already existed) |

## Deviations from Plan

None — plan executed exactly as written. RED/GREEN TDD steps were combined into one test file commit because RenderEngine.java already fully implemented all required methods; the GREEN verification confirmed all 5 tests pass.

## Known Stubs

None — all test assertions exercise real behavior. No placeholder data or hardcoded responses.

## Self-Check: PASSED

- `src/test/resources/fixtures/identity.xsl`: FOUND
- `src/test/resources/fixtures/input.xml`: FOUND
- `src/test/resources/fixtures/invalid.xsl`: FOUND
- `src/test/java/ch/ti/gagi/xlseditor/render/RenderEngineTest.java`: FOUND
- Commit `00324f2`: FOUND
- Commit `1476096`: FOUND
