# Phase 10: Saxon URI Fix - Context

**Gathered:** 2026-04-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Fix URI-decode bug in `PreviewManager.toPreviewErrors()` so click-to-navigate works on macOS when Saxon runtime errors reference percent-encoded `file://` paths. No new features — single targeted fix + unit test coverage.

</domain>

<decisions>
## Implementation Decisions

### Exception Safety
- **D-01:** `resolveFilePath()` wraps `URI.create(raw)` in `try/catch IllegalArgumentException`. Fallback: strip `file://` prefix manually and return the remainder. Exception must not propagate — navigation degrades silently rather than crashing.

### Test Strategy
- **D-02:** Unit test only (no integration test). Test `toPreviewErrors()` directly with a fabricated `RenderError` carrying a `file:///path/my%20file.xsl:10` location string. Assert that `PreviewError.file` equals the decoded filesystem path and `PreviewError.line` equals `10`.
- **D-03:** `toPreviewErrors()` and `resolveFilePath()` are made package-private (drop `private` modifier) to allow direct test access from same-package test class. No reflection, no extraction to a helper class.

### Claude's Discretion
- Exact fallback string manipulation for the try/catch branch (e.g. how many slashes to strip for `file:///` vs `file://`)
- Whether to also add a test case for the try/catch branch (malformed URI input)
- Whether to add a `@VisibleForTesting` annotation or comment

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` §ERR-04 — URI-decode requirement spec

### Source files (read before planning)
- `src/main/java/ch/ti/gagi/xsleditor/preview/PreviewManager.java` — contains the partial fix already in working tree (uncommitted diff: `resolveFilePath()` added, used in `toPreviewErrors()`)
- `src/main/java/ch/ti/gagi/xsleditor/error/ErrorManager.java` — where Saxon `systemId` is extracted and passed into `RenderError.location`

### Existing tests
- `src/test/java/ch/ti/gagi/xsleditor/preview/PreviewManagerIntegrationTest.java` — integration test for PreviewManager; does not cover URI-decode path

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `RenderError(String message, String type, String location)` — record with `location` field already carrying raw Saxon systemId string (e.g. `file:///path/to/my%20file.xsl:42`)
- `PreviewError` record — target type; has `file` and `line` fields

### Established Patterns
- Test package mirrors source package (`ch.ti.gagi.xsleditor.preview`) — package-private access works without reflection
- `try/catch` with `ignored` local variable already used in `toPreviewErrors()` for `NumberFormatException` — consistent exception-handling style

### Integration Points
- `PreviewManager.toPreviewErrors()` is the sole consumer of `RenderError.location` for navigation purposes
- No changes needed to `ErrorManager` — Saxon URI is passed through correctly, decode belongs at consumption point

</code_context>

<specifics>
## Specific Ideas

- The partial fix is already in the working tree (uncommitted). Planner should account for this: complete the fix (add try/catch to `resolveFilePath`), make methods package-private, add unit test, commit.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 10-saxon-uri-fix*
*Context gathered: 2026-04-21*
