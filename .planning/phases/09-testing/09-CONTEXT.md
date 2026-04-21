# Phase 9: Testing - Context

**Gathered:** 2026-04-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Write unit tests for all 6 backend modules and 2 integration tests for the full render pipeline. Zero regressions. No UI tests (those were added incrementally in Phases 4–8 and already exist).

Requirements in scope: TEST-01, TEST-02, TEST-03, TEST-04, TEST-05, TEST-06, TEST-07, TEST-08.

</domain>

<decisions>
## Implementation Decisions

### Fixture files

- **D-01:** Fixture files are **minimal syntactic** — just enough XML/XSLT/FO to exercise the code path. No attempt to mimic real-world templates. Small, readable, easy to maintain.
- **D-02:** `LibraryPreprocessor` tests use `@TempDir` with `Files.writeString()` to create library and template files inline. No static fixture files on disk for this module — fully self-contained test setup.
- **D-03:** Integration tests (TEST-07, TEST-08) require real XSLT + XML fixture files. These go in `src/test/resources/fixtures/`. Minimal content: XSLT with one template, XML with one element, FO with one `fo:block`.

### RenderEngine testing strategy

- **D-04:** TEST-04 uses the **real Saxon + FOP pipeline** — no mocks, no refactor of the constructor. `RenderEngineTest` instantiates `RenderEngine` directly and calls it with minimal fixture files. Tests will be slow (a few seconds) but verify actual behavior.
- **D-05:** No refactor of `RenderEngine` factory init (the CONCERNS.md tech debt) as part of this phase. Constructor-injected dependencies are deferred.

### Tech debt handling

- **D-06:** Test-as-is by default. Fix a backend issue **only if it actively blocks writing a meaningful test** — e.g., if a workaround would be needed to make the test pass rather than the code. In that case, fix the code and note it in the commit.
- **D-07:** If a fragility is not blocking (e.g., `PreviewManager` Windows path parsing) but is worth flagging, add a `// FIXME:` comment in the test rather than fixing the production code.

### Coverage reporting

- **D-08:** No JaCoCo. `./gradlew test` is the only required gate. No coverage threshold, no report plugin.

### Claude's Discretion

- Exact package structure under `src/test/java/` (mirror `src/main/java/` per ROADMAP deliverable)
- Which specific scenarios to cover within each module (happy path + key error path is sufficient)
- Whether to use `@Nested` classes for grouping scenarios within a test class
- Exact fixture XSLT/XML content (as long as it's syntactically valid and minimal)
- Whether `RenderEngineTest` uses `@Tag("integration")` or a separate source set to allow skipping slow tests in fast CI runs

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` §Testing — TEST-01..TEST-08 (exact acceptance criteria for each unit and integration test)
- `.planning/ROADMAP.md` §Phase 9 — Deliverables list (directory structure, fixture location, JaCoCo note)

### Backend modules under test (do not re-implement)
- `src/main/java/ch/ti/gagi/xlseditor/library/LibraryPreprocessor.java` — TEST-01
- `src/main/java/ch/ti/gagi/xlseditor/dependency/DependencyResolver.java` — TEST-02
- `src/main/java/ch/ti/gagi/xlseditor/validation/ValidationEngine.java` — TEST-03
- `src/main/java/ch/ti/gagi/xlseditor/render/RenderEngine.java` — TEST-04
- `src/main/java/ch/ti/gagi/xlseditor/error/ErrorManager.java` — TEST-05
- `src/main/java/ch/ti/gagi/xlseditor/log/LogManager.java` — TEST-06
- `src/main/java/ch/ti/gagi/xlseditor/preview/PreviewManager.java` — used by TEST-07, TEST-08 (full pipeline)
- `src/main/java/ch/ti/gagi/xlseditor/render/RenderOrchestrator.java` — entry point for TEST-07, TEST-08

### Existing tests (context, not targets)
- `src/test/java/ch/ti/gagi/xlseditor/model/ProjectConfigTest.java` — pattern for `@TempDir` usage
- `src/test/java/ch/ti/gagi/xlseditor/ui/RenderControllerTest.java` — Wave 0/Wave 1 stub pattern (reference only)

### Tech debt reference
- `.planning/codebase/CONCERNS.md` — Full list of known issues; use to identify which ones block test writing (D-06/D-07)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `@TempDir` pattern established in `ProjectConfigTest` — use same pattern for LibraryPreprocessor and DependencyResolver tests
- JUnit 5.10.0 + `junit-platform-launcher` already in `build.gradle` — no new test dependencies needed
- `useJUnitPlatform()` already configured in `test {}` block

### Established Patterns
- `@TempDir Path tempDir` for filesystem isolation (see `ProjectConfigTest`, `ProjectContextTest`)
- `@Disabled` for stubs not yet implemented (see UI tests from Phase 4-5)
- No Mockito in `build.gradle` — pure JUnit 5 only; avoid introducing mock frameworks unless strictly needed

### Integration Points
- `RenderOrchestrator.renderSafe(ProjectContext)` — entry point for TEST-07 and TEST-08; takes a `ProjectContext` which wraps project root, entrypoint, XML input
- `PreviewManager` wraps `RenderOrchestrator` — may be the better entry point for integration tests (returns `Preview` DTO)
- `src/test/resources/` directory does not exist yet — must be created with `fixtures/` subfolder

</code_context>

<specifics>
## Specific Ideas

- Integration test fixture structure: one minimal XSLT (`identity.xsl` or similar), one XML (`input.xml`), verify that `PreviewManager.render()` returns a non-empty `Preview` with PDF bytes
- Failure integration test: XSLT with a syntax error → `PreviewManager.render()` returns `PreviewError` with `type=XSLT` and non-null `line`
- `LibraryPreprocessor` test: create `library.xsl` via `Files.writeString(tempDir.resolve("library.xsl"), ...)`, create `template.xsl` with `<?LIBRARY library?>`, assert merged output contains library content

</specifics>

<deferred>
## Deferred Ideas

- RenderEngine constructor refactor (inject `Processor`/`FopFactory`) — CONCERNS.md tech debt, deferred post-v1
- JaCoCo coverage reporting — explicitly out of scope per user decision
- Fix `PreviewManager` Windows path parsing fragility — deferred unless it blocks a test (D-07)
- `DocumentBuilderFactory` instance reuse in `DependencyResolver`/`ValidationEngine` — deferred unless it causes test failures

</deferred>

---

*Phase: 09-testing*
*Context gathered: 2026-04-21*
