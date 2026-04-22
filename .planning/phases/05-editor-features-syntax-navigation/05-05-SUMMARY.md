---
phase: 05-editor-features-syntax-navigation
plan: 05
subsystem: editor-ui
tags: [search, find-in-files, dialog, javafx, background-task, richtextfx, menu-wiring]

# Dependency graph
requires:
  - phase: "05-04"
    provides: "EditorController.navigateTo(Path, int, int) used by SearchDialog result click"
provides:
  - SearchDialog.java with SearchHit record, static search() method, and background Task (EDIT-08)
  - Find in Files menu item in Search menu (Ctrl+Shift+F) wired in MainController
  - SearchTaskTest enabled and passing (both tests)
affects:
  - "06-xx"  # Future phases may surface search results from log panel errors

# Tech tracking
tech-stack:
  added:
    - SearchDialog extends Dialog<Void> with JavaFX concurrent Task<List<SearchHit>>
    - Files.walk + readAllLines pattern for multi-file text scan
    - Daemon ExecutorService (single-thread) with shutdownNow() on re-search and on dialog close
  patterns:
    - Static search(Path, String) extracted from Task.call() for testability without JavaFX toolkit
    - SearchHit record with 1-based line display in toString() (0-based int in record)
    - Non-blocking dialog.show() (not showAndWait()) to allow continued editing

key-files:
  created:
    - src/main/java/ch/ti/gagi/xsleditor/ui/SearchDialog.java
  modified:
    - src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java
    - src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
    - src/test/java/ch/ti/gagi/xsleditor/ui/SearchTaskTest.java
    - src/main/java/ch/ti/gagi/xsleditor/ui/AutocompleteProvider.java
    - src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java

key-decisions:
  - "SearchDialog.search() is a static method so SearchTaskTest can call it without a JavaFX toolkit"
  - "dialog.show() (non-blocking) rather than showAndWait() — user can keep editing while search runs"
  - "shutdownNow() called both on new search start and on dialog close — T-05-10 mitigation for Task accumulation"
  - "Occurrence highlight token stripped of outer XML punctuation before regex search (EDIT-06 partial fix)"

patterns-established:
  - "Static logic extraction pattern: Dialog hosts static testable method, Task.call() delegates to it"
  - "Executor lifecycle: shutdownNow on trigger + setOnCloseRequest cleanup — dual shutdown guards"

requirements-completed: [EDIT-08]

# Metrics
duration: approx 25min
completed: 2026-04-19
---

# Phase 05 Plan 05: SearchDialog and Find in Files Summary

**SearchDialog with Files.walk background task, SearchHit record, static search() for testability, and Find in Files menu wired to MainController (EDIT-08); autocomplete prefix extraction bug fixed (EDIT-05).**

---

## Performance

- **Duration:** approx 25 min
- **Started:** 2026-04-19T06:28:00Z
- **Completed:** 2026-04-19T12:28:00Z
- **Tasks:** 2 (plus 1 bug-fix commit)
- **Files modified:** 6

---

## Accomplishments

- `SearchDialog.java` created: full `Dialog<Void>` subclass with `SearchHit` record, static `search(Path, String)` using `Files.walk`, background `Task<List<SearchHit>>` with daemon executor, and single-click navigation via `editorController.navigateTo()`
- `SearchTaskTest` enabled (both `@Disabled` annotations removed); both tests pass without a JavaFX toolkit
- Find in Files wired in `MainController`: `@FXML MenuItem findInFilesMenuItem` + `handleFindInFiles()` + Search menu entry in `main.fxml` (Ctrl+Shift+F accelerator)
- Autocomplete prefix extraction fixed: `extractPrefixBeforeCaret` now stops at `<` so `xsl:if` is returned instead of `<xsl:if`, enabling keyword matches in the popup (EDIT-05 bug corrected)
- Occurrence token stripping improved: outer XML punctuation (`<>/\"'=`) stripped before regex search so selecting `<xsl:template>` finds the name in closing tags (EDIT-06 partial improvement)
- Human verification confirmed EDIT-04 (syntax highlighting), EDIT-05 (autocomplete after fix), EDIT-08 (Find in Files) working

---

## Task Commits

Each task was committed atomically:

1. **Task 1: SearchDialog — Dialog with background search Task and SearchHit record** - `17f677d` (feat)
2. **Task 2: Wire Find in Files menu action in MainController** - `f82640f` (feat)
3. **Bug fix: EDIT-05 prefix extraction + EDIT-06 token stripping** - `4a33be5` (fix)

---

## Files Created/Modified

- `src/main/java/ch/ti/gagi/xsleditor/ui/SearchDialog.java` — Created. Full search dialog with SearchHit record and static search() method
- `src/test/java/ch/ti/gagi/xsleditor/ui/SearchTaskTest.java` — @Disabled removed; both tests now active and passing
- `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` — Added @FXML findInFilesMenuItem field and handleFindInFiles() handler
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` — Added Search menu with findInFilesMenuItem and Ctrl+Shift+F accelerator
- `src/main/java/ch/ti/gagi/xsleditor/ui/AutocompleteProvider.java` — Fixed extractPrefixBeforeCaret to stop at `<`
- `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java` — Improved occurrence token stripping in selection listener

---

## Decisions Made

- `SearchDialog.search()` extracted as a `public static` method so `SearchTaskTest` can call it on a plain JVM without a JavaFX toolkit — mirrors the `RenderOrchestrator` testability pattern established in Phase 1
- `dialog.show()` (non-blocking) chosen over `showAndWait()` so the user can continue editing while a long search runs in the background
- `searchExecutor.shutdownNow()` called in two places: at the start of each new search (to cancel any prior in-flight Task) and in `setOnCloseRequest` (to release daemon thread on dialog close) — T-05-10 mitigation

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed autocomplete prefix extraction stopping at `<` (EDIT-05)**
- **Found during:** Human verification checkpoint
- **Issue:** `extractPrefixBeforeCaret` returned `<xsl:if` instead of `xsl:if` because it did not stop at `<`. No keyword in the autocomplete list starts with `<`, so the popup would never match any entry.
- **Fix:** Added `'<'` to the stop-character set in `extractPrefixBeforeCaret` in `AutocompleteProvider.java`
- **Files modified:** `src/main/java/ch/ti/gagi/xsleditor/ui/AutocompleteProvider.java`
- **Verification:** Human verified autocomplete popup shows keyword entries after fix
- **Committed in:** `4a33be5`

**2. [Rule 1 - Bug] Improved occurrence highlight token stripping (EDIT-06 partial)**
- **Found during:** Bug fix pass after EDIT-05 fix
- **Issue:** Selection of `<xsl:template>` (with angle brackets) would not match the name token in closing tags because the regex included the punctuation characters
- **Fix:** Stripped outer XML punctuation (`<>/\"'=`) from the selected token before the regex search in `EditorController`
- **Files modified:** `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java`
- **Verification:** Committed; full occurrence highlighting for complex selections remains a known issue (see below)
- **Committed in:** `4a33be5`

---

**Total deviations:** 2 auto-fixed (both Rule 1 — bugs discovered at human verification)
**Impact on plan:** Both fixes necessary for correctness. No scope creep.

---

## Known Issues

### EDIT-06: Occurrence Highlighting — Partial Implementation

**Status:** Partially working. Simple `$varName` selections highlight correctly. Complex selections involving mixed XML structure (e.g. selecting across tag boundaries) may not highlight all occurrences correctly.

**Root cause:** The regex-based OccurrenceHighlighter operates on the raw text of the selection. When the selection spans or includes XML punctuation in a non-trivial pattern, the stripped token may still not match all intended occurrences.

**Impact:** Non-blocking for MVP — the feature provides value for the common case (`$var`, `name="..."` selections) even in its current state.

**Recommendation:** Revisit in a Phase 9 testing pass with structured test cases covering edge-case selections. Consider an AST-aware approach if regex proves insufficient.

### EDIT-07: Go-to-Definition — Not Tested in Human Verification

**Status:** Implementation was committed in Plan 05-04 (`HrefExtractor` wired via Ctrl+Click in `EditorController.buildTab()`). Human verification during the 05-05 checkpoint did not explicitly test EDIT-07. The feature compiles and is wired; correctness is unconfirmed in the running application.

**Recommendation:** Include EDIT-07 in the next verification pass (e.g. Phase 6 or Phase 9 integration testing).

---

## Threat Surface Scan

| Threat ID | Component | Disposition | Status |
|-----------|-----------|-------------|--------|
| T-05-09 | SearchDialog.search Files.walk | accept | projectRoot set by DirectoryChooser (user-selected, not user-typed); traversal scoped to project |
| T-05-10 | SearchDialog concurrent Task | mitigate | shutdownNow() on new search start AND on setOnCloseRequest — Task accumulation prevented; daemon threads do not block JVM exit |
| T-05-11 | Search results ListView | accept | Results show only files within projectRoot; no content outside project exposed |

No new trust boundaries beyond the plan's threat model.

---

## Next Phase Readiness

- Phase 5 (Editor Features) is fully committed. All five EDIT requirements have implementations:
  - EDIT-04: syntax highlighting — confirmed working
  - EDIT-05: autocomplete — confirmed working after prefix fix
  - EDIT-06: occurrence highlighting — partial (known issue above)
  - EDIT-07: go-to-definition — implemented, not explicitly verified
  - EDIT-08: Find in Files — confirmed working
- `./gradlew test` exits 0 with all tests passing including SearchTaskTest
- Phase 6 (Render Pipeline Integration) can begin — EditorController.navigateTo() is available for the log panel to use when clicking error entries

---

## Self-Check: PASSED

Files exist on disk:
- `src/main/java/ch/ti/gagi/xsleditor/ui/SearchDialog.java` — FOUND (commit 17f677d)
- `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` — FOUND (commit f82640f)
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` — FOUND (commit f82640f)

Commits verified:
- `17f677d` (Task 1 — SearchDialog) — FOUND
- `f82640f` (Task 2 — MainController wiring) — FOUND
- `4a33be5` (fix — EDIT-05 prefix, EDIT-06 token) — FOUND

---

*Phase: 05-editor-features-syntax-navigation*
*Completed: 2026-04-19*
