---
phase: 17
plan: "17-01"
subsystem: encoding
tags: [encoding, charset, xml, editor, render-pipeline]
dependency_graph:
  requires: [phase-16]
  provides: [correct-charset-read-write]
  affects: [EditorTab, EditorController, RenderOrchestrator, LibraryPreprocessor]
tech_stack:
  added: [XmlCharsetDetector utility]
  patterns: [BOM detection, XML declaration scan, charset-preserving round-trip]
key_files:
  created:
    - src/main/java/ch/ti/gagi/xsleditor/util/XmlCharsetDetector.java
  modified:
    - src/main/java/ch/ti/gagi/xsleditor/ui/EditorTab.java
    - src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java
    - src/main/java/ch/ti/gagi/xsleditor/render/RenderOrchestrator.java
    - src/main/java/ch/ti/gagi/xsleditor/library/LibraryPreprocessor.java
    - src/test/java/ch/ti/gagi/xsleditor/ui/EditorTabTest.java
decisions:
  - "XmlCharsetDetector reads only the first 200 bytes as ISO-8859-1 (ASCII-safe) to extract the XML encoding declaration without risk of mis-decoding"
  - "UnsupportedCharsetException from Charset.forName() is caught and logged; falls back to UTF-8 to avoid hard crash on exotic charsets"
  - "EditorTab stores charset as public final field so save paths can use it without re-detecting"
metrics:
  duration: "~10 minutes"
  completed: "2026-04-23"
  tasks_completed: 8
  files_changed: 6
---

# Phase 17 Plan 01: XML Encoding Detection — Editor Read/Write Fix Summary

**One-liner:** New `XmlCharsetDetector` utility detects BOM and XML encoding declaration so windows-1252 (and other non-UTF-8) files round-trip correctly through the editor and render pipeline.

## What Was Done

Root cause (D-01): `Files.readString(path, UTF_8)` ignores `<?xml ... encoding="windows-1252"?>`. Windows-1252 bytes for characters like `ä` (0xE4) and `ü` (0xFC) are invalid UTF-8 sequences — Java substitutes U+FFFD, causing the editor to display replacement characters instead of the original glyphs.

Fix: a new `XmlCharsetDetector` utility reads the first 200 bytes of any file, checks for UTF-8/UTF-16 BOMs first, then scans the XML declaration for an `encoding="..."` attribute using a regex on ISO-8859-1-decoded bytes (safe because the XML declaration is always ASCII). The detected `Charset` is used for every file read and write in both the editor and the render pipeline.

## Files Changed

| File | Change |
|------|--------|
| `src/main/java/ch/ti/gagi/xsleditor/util/XmlCharsetDetector.java` | New — BOM + XML declaration charset detector; UTF-8 fallback |
| `src/main/java/ch/ti/gagi/xsleditor/ui/EditorTab.java` | Added `public final Charset charset` field and `charset` constructor parameter |
| `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java` | `openOrFocusTab` detects charset before read; `saveTab`/`saveAll` write with `editorTab.charset` / `et.charset` |
| `src/main/java/ch/ti/gagi/xsleditor/render/RenderOrchestrator.java` | XSLT entry file read uses `XmlCharsetDetector.detect()` |
| `src/main/java/ch/ti/gagi/xsleditor/library/LibraryPreprocessor.java` | Library `.xsl` file reads use `XmlCharsetDetector.detect()` |
| `src/test/java/ch/ti/gagi/xsleditor/ui/EditorTabTest.java` | Updated all `new EditorTab(path, content)` calls to the new 3-arg constructor |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] EditorTabTest used old 2-arg EditorTab constructor**
- **Found during:** Task 7 (build verification)
- **Issue:** `EditorTabTest` called `new EditorTab(Path, String)` — the constructor no longer exists after Task 3 added the mandatory `Charset` parameter. Build failed with 4 compilation errors.
- **Fix:** Added `import java.nio.charset.StandardCharsets;` and updated all 4 call sites to `new EditorTab(path, content, StandardCharsets.UTF_8)`.
- **Files modified:** `src/test/java/ch/ti/gagi/xsleditor/ui/EditorTabTest.java`
- **Commit:** 977bf30 (included in the single fix commit)

## Success Criteria

| Criterion | Status |
|-----------|--------|
| ENC-01: root cause documented | Done — D-01 in CONTEXT.md and this summary |
| ENC-02: non-ASCII chars correct in editor | Done — Tasks 2, 3, 4 |
| ENC-03: log panel non-ASCII correct | Done — Tasks 5, 6 (render path fixed) |

## Commit

`977bf30` — `fix(17): detect XML encoding declaration for file read/write; preserve windows-1252 round-trip`

## Self-Check: PASSED

- `src/main/java/ch/ti/gagi/xsleditor/util/XmlCharsetDetector.java` — exists
- `977bf30` — commit verified in git log
- Build: `./gradlew compileJava compileTestJava` — exit 0
- Tests: `./gradlew test` — all pass
