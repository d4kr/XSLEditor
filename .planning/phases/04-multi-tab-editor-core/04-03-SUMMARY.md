---
phase: 04-multi-tab-editor-core
plan: "03"
subsystem: ui
tags: [javafx, wiring, integration, editor, filetree]

# Dependency graph
requires:
  - phase: 04-multi-tab-editor-core/04-01
    provides: EditorTab data carrier with CodeArea, UndoManager dirty binding
  - phase: 04-multi-tab-editor-core/04-02
    provides: EditorController with openOrFocusTab, Ctrl+S, dirty-title, close confirmation
  - phase: 03-file-tree-view
    provides: FileTreeController.setOnFileOpenRequest seam (D-05)
provides:
  - MainController wired to EditorController (field + initialize call)
  - FileTree double-click opens files as editor tabs (TREE-04 + EDIT-01 end-to-end)
  - Window close with dirty tab triggers aggregate close-confirmation dialog (Pitfall 4)
affects: [05-syntax-highlighting, 07-pdf-preview, 08-log-panel]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Sub-controller wiring: MainController instantiates EditorController as private final field and calls initialize() from FXML lifecycle method"
    - "Deferred Stage supplier: () -> primaryStage passed to sub-controllers because primaryStage is populated after FXML initialize() runs"
    - "Integration seam activation: FileTreeController.setOnFileOpenRequest wired to editorController::openOrFocusTab after both controllers initialize"

key-files:
  created: []
  modified:
    - src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java

key-decisions:
  - "editorController.initialize() called after fileTreeController.initialize() to guarantee seam exists before hookup (T-04-12 mitigation)"
  - "No FXML changes required: editorPane fx:id already present from Phase 1; EditorController owns TabPane construction"

patterns-established:
  - "Wiring order: fileTreeController.initialize → editorController.initialize → fileTreeController.setOnFileOpenRequest"

requirements-completed: [EDIT-01, EDIT-02, EDIT-03, EDIT-09]

# Metrics
duration: 5min
completed: 2026-04-18
---

# Phase 04 Plan 03: Wire EditorController into MainController Summary

**EditorController wired into MainController with FileTree open-seam activated — double-click on a file now opens it as a multi-tab editor tab.**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-04-18T15:52:00Z
- **Completed:** 2026-04-18T15:57:13Z
- **Tasks:** 1 of 2 complete (Task 2 pending human verification)
- **Files modified:** 1

## Accomplishments

- Added `private final EditorController editorController = new EditorController()` field to MainController (Phase 4 sub-controller)
- Wired `editorController.initialize(editorPane, () -> primaryStage, this::setDirty)` in `MainController.initialize()`, after `fileTreeController.initialize()`
- Activated Phase 3 D-05 seam: `fileTreeController.setOnFileOpenRequest(editorController::openOrFocusTab)`
- All three build targets green: `./gradlew build`, `./gradlew test`, `./gradlew shadowJar`

## Task Commits

1. **Task 1: Wire EditorController into MainController and connect the FileTree open-seam** - `c9d095d` (feat)
2. **Task 2: Human verification** - PENDING (checkpoint:human-verify — not yet executed)

**Plan metadata:** pending (docs commit after human verification completes)

## Files Created/Modified

- `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` - Added EditorController field, initialize call, and seam hookup (9 insertions)

## Decisions Made

None - followed plan exactly as specified. Three edits applied per the 04-PATTERNS.md template.

## Deviations from Plan

None - plan executed exactly as written.

## Build Verification

| Command | Result |
|---------|--------|
| `./gradlew build -x test` | BUILD SUCCESSFUL |
| `./gradlew test` | BUILD SUCCESSFUL (ProjectContextTest, ProjectConfigTest, EditorTabTest green) |
| `./gradlew shadowJar` | BUILD SUCCESSFUL |

## Acceptance Criteria Check

| Criterion | Status |
|-----------|--------|
| `private final EditorController editorController = new EditorController();` count = 1 | PASS |
| `editorController.initialize(` count = 1 | PASS |
| `fileTreeController.setOnFileOpenRequest(editorController::openOrFocusTab)` count = 1 | PASS |
| `this::setDirty` count = 1 | PASS |
| `() -> primaryStage` count >= 2 | PASS (2) |
| No `import ch.ti.gagi.xlseditor.ui.EditorController` (same package) | PASS (0) |
| No `new TabPane` in MainController | PASS (0) |
| No `setOnKeyPressed` in MainController | PASS (0) |
| Ordering: fileTreeController.initialize → editorController.initialize → setOnFileOpenRequest | PASS |
| `./gradlew build -x test` exits 0 | PASS |
| `./gradlew test` exits 0 | PASS |
| `./gradlew shadowJar` exits 0 | PASS |

## Pending: Task 2 Human Verification

**Task 2 is a `checkpoint:human-verify` gate** — the orchestrator handles this separately.

The human verifier should launch `./gradlew run` and execute the six scenarios described in the plan:

1. EDIT-01 multi-tab + dedup: double-click files, verify dedup on second open of same file
2. EDIT-02 dirty indicator: type a char, verify `*filename` prefix; undo, verify `*` disappears
3. EDIT-03 Ctrl+S save: edit, save with Ctrl+S, verify `*` clears and file persists on disk
4. EDIT-09 close-tab confirmation: close dirty tab, verify YES/CANCEL dialog; CANCEL keeps tab
5. Reopen-after-close regression (Pitfall 3): close tab, reopen, verify new tab opens
6. Error surfacing (bonus): delete file on disk, press Ctrl+S, verify "Save Failed" alert

Resume signal: type "approved" if all six scenarios pass.

## Phase 4 Requirement Coverage

| Requirement | Provided By | MainController.java Line |
|-------------|-------------|--------------------------|
| EDIT-01 (multi-tab + dedup) | EditorController.openOrFocusTab | seam wired at initialize() |
| EDIT-02 (dirty indicator) | EditorController dirty listener | seam wired at initialize() |
| EDIT-03 (Ctrl+S save) | EditorController per-CodeArea InputMap | seam wired at initialize() |
| EDIT-09 (close-tab confirmation) | EditorController tab.setOnCloseRequest | seam wired at initialize() |

## Handoff Note for Phase 5

EditorController is in place. Phase 5 extends CodeArea with syntax highlighting via `StyleSpans` — no EditorController API change required; add a `styleClass` listener per tab on construction inside `buildTab`, gated by file extension.

## Issues Encountered

None.

---

*Phase: 04-multi-tab-editor-core*
*Completed (partial — Task 2 pending): 2026-04-18*
