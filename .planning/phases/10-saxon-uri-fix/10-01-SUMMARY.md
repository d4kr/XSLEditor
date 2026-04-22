---
plan: 10-01
phase: 10-saxon-uri-fix
status: complete
completed: 2026-04-21
requirements: [ERR-04]
---

## Summary

Completed the Saxon URI-decode fix in `PreviewManager` and added unit tests.

## What Was Built

- `PreviewManager.resolveFilePath()` — wraps `URI.create()` in `try/catch IllegalArgumentException`; fallback strips `file://` prefix manually so malformed URIs degrade silently
- Both `resolveFilePath()` and `toPreviewErrors()` promoted from `private static` → package-private `static` to enable direct testing
- `PreviewManagerTest.java` — 3 unit tests covering percent-encoded paths, plain file URIs, and malformed URI fallback

## Tasks

| Task | Status | Notes |
|------|--------|-------|
| Task 1: Complete resolveFilePath() | ✓ complete | try/catch added, methods made package-private |
| Task 2: Add PreviewManagerTest | ✓ complete | 3 tests, all green |

## Key Files

### Modified
- `src/main/java/ch/ti/gagi/xsleditor/preview/PreviewManager.java`

### Created
- `src/test/java/ch/ti/gagi/xsleditor/preview/PreviewManagerTest.java`

## Verification

```
./gradlew test  →  BUILD SUCCESSFUL (all tests pass)
```

- `PreviewManagerTest`: 3/3 tests pass
- `PreviewManagerIntegrationTest`: 2/2 tests pass
- No regressions across full suite

## Self-Check: PASSED

All must_haves satisfied:
- ✓ Saxon runtime errors with percent-encoded file:// paths decoded to real filesystem paths
- ✓ Malformed URI does not propagate exception — degrades silently
- ✓ Both methods accessible from same-package test code without reflection
- ✓ Unit test asserts correct file and line extraction from `file:///path/my%20file.xsl:10`
