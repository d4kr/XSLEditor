---
phase: 03-file-tree-view
plan: 01
subsystem: ui
tags: [javafx, observable-list, jackson, path-traversal, tdd, projectcontext]

# Dependency graph
requires:
  - phase: 02-project-management
    provides: ProjectContext, Project, ProjectConfig, ProjectManager foundation built in plans 01/02
provides:
  - ProjectContext.projectFilesProperty() — live ObservableList<Path> of flat project root files
  - ProjectContext.setEntrypoint(Path) — immediate write-back to .xslfo-tool.json, preserves xmlInput
  - ProjectContext.setXmlInput(Path) — immediate write-back to .xslfo-tool.json, preserves entryPoint
  - refreshProjectFiles() — dotfile-excluded, subdirectory-excluded, lexicographically sorted list population
  - validateProjectRelativePath() — narrower path guard allowing subdirectory segments while blocking traversal
affects:
  - 03-02: FileTreeController binds TreeView to projectFilesProperty
  - 03-03: menu actions call setEntrypoint/setXmlInput on role assignment

# Tech tracking
tech-stack:
  added: []
  patterns:
    - ObservableList backed by FXCollections.observableArrayList() for reactive UI binding
    - try-with-resources on Files.list(Stream) to guarantee directory handle closure
    - write-back via ProjectConfig.write() immediately on setter call (D-01 contract)
    - two-layer traversal defense (validateProjectRelativePath + ProjectConfig compact constructor)

key-files:
  created: []
  modified:
    - src/main/java/ch/ti/gagi/xlseditor/ui/ProjectContext.java
    - src/test/java/ch/ti/gagi/xlseditor/model/ProjectContextTest.java

key-decisions:
  - "validateProjectRelativePath is narrower than validateSimpleFilename: allows subdirectory path segments (future-proofing for config paths) while still blocking absolute paths and ../ traversal"
  - "refreshProjectFiles uses Files::isRegularFile to exclude subdirectories and their contents entirely — Phase 3 is flat per TREE-01"
  - "createFile appends directly to projectFiles after Files.createFile rather than calling refreshProjectFiles to avoid a full directory scan per D-07"
  - "setEntrypoint/setXmlInput rebuild currentProject as a new Project instance rather than mutating the existing one — consistent with Project being immutable value object"

patterns-established:
  - "ObservableList mutation pattern: clear() + batch add via sorted stream in refreshProjectFiles; single add() in createFile"
  - "Write-back guard ordering: null-check state, validate path, write config, rebuild in-memory object — abort before any I/O on validation failure"
  - "Two-layer defense: application-level validateProjectRelativePath runs before ProjectConfig compact constructor (which also rejects absolute strings)"

requirements-completed: [PROJ-04, PROJ-05]

# Metrics
duration: 15min
completed: 2026-04-17
---

# Phase 3 Plan 01: ProjectContext Observable List and Setter API Summary

**ObservableList<Path> reactive file list plus setEntrypoint/setXmlInput write-back added to ProjectContext, enabling JavaFX tree binding and immediate .xslfo-tool.json persistence without UI-layer filesystem access**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-04-17T00:00:00Z
- **Completed:** 2026-04-17T00:15:00Z
- **Tasks:** 2 (TDD: RED + GREEN)
- **Files modified:** 2

## Accomplishments

- Extended ProjectContext with `projectFilesProperty()` returning a live `ObservableList<Path>` that the Phase 3 FileTreeController can bind directly to a TreeView
- `openProject()` now populates the observable list via `refreshProjectFiles()` which uses `Files::isRegularFile` to exclude subdirectories, filters out dotfiles (names starting with `.`), and sorts entries lexicographically via `Comparator.comparing(Path::toString)`
- `createFile()` appends the new relative path to `projectFiles` after `Files.createFile` so `ListChangeListener`s fire without requiring a full project reload
- `setEntrypoint(Path)` and `setXmlInput(Path)` write `.xslfo-tool.json` immediately via `ProjectConfig.write()` (D-01), preserve the opposite field, and rebuild `currentProject` as a new `Project` instance
- `validateProjectRelativePath()` provides narrower traversal guard than `validateSimpleFilename`: rejects absolute paths and paths that escape root after `resolve().normalize()`, but allows subdirectory segments for future multi-level config paths
- All 26 tests in `ProjectContextTest` pass (7 original Plan 02 tests preserved + 19 new Phase 3 tests); full test suite green

## Test Count

- **Before this plan:** 7 tests (Plan 02 contracts)
- **After this plan:** 26 tests total (+19 new Phase 3 tests)
- **New tests cover:** observable list population, dotfile exclusion, subdirectory exclusion, sorted order, list refresh on re-open, createFile append + ListChangeListener, setEntrypoint/setXmlInput round-trip, null-clear, absolute/traversal rejection, IllegalStateException without open project

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend ProjectContextTest with failing tests (RED)** - `ca5b416` (test)
2. **Task 2: Extend ProjectContext implementation (GREEN)** - `fdfc6b6` (feat)

**Plan metadata:** (see final commit below)

## Files Created/Modified

- `src/main/java/ch/ti/gagi/xlseditor/ui/ProjectContext.java` — Added `projectFiles` field, `projectFilesProperty()`, `refreshProjectFiles()`, extended `openProject()` and `createFile()`, added `setEntrypoint()`, `setXmlInput()`, `writeConfigAndRebuildProject()`, `validateProjectRelativePath()`
- `src/test/java/ch/ti/gagi/xlseditor/model/ProjectContextTest.java` — 19 new `@Test` methods appended; original 7 tests preserved unchanged

## Decisions Made

1. **`validateProjectRelativePath` is narrower than `validateSimpleFilename`:** `validateSimpleFilename` rejects any path separator, suitable for the `createFile` dialog input where only bare filenames are valid. `validateProjectRelativePath` allows multi-segment relative paths (e.g. `templates/main.xsl`) because the entrypoint/xmlInput config fields are allowed to reference files in subdirectories per the config schema — Phase 3 is flat in the UI tree, but the config schema already supports nested paths for future use.

2. **`refreshProjectFiles` excludes subdirectories via `Files::isRegularFile`:** Directories are silently excluded (not added to the list). A subdirectory `sub/` will not appear in `projectFiles` even though it is a filesystem entry in the root. This matches TREE-01 (flat tree). Dotfiles are excluded by filename prefix check.

3. **`createFile` appends directly, not via full refresh:** After `Files.createFile(target)`, only the single new `relative` path is appended to `projectFiles`. A full `refreshProjectFiles` call would work but is unnecessary and slightly heavier. The `ListChangeListener` fires on the single `add()` call, satisfying D-07.

4. **`currentProject` is rebuilt as a new `Project` instance in `writeConfigAndRebuildProject`:** `Project` is an immutable record-like final class with no setters. The write-back creates a fresh `Project(rootPath, entryPoint, xmlInput)` after persisting the config — consistent with the existing design and safe for concurrent read from `getCurrentProject()`.

## Deviations from Plan

None - plan executed exactly as written. All method bodies, guards, and ordering followed the plan specification precisely.

## Issues Encountered

None. Compilation failed as expected in the RED phase (Task 1). All 26 tests passed on first GREEN run (Task 2).

## Known Stubs

None. `projectFilesProperty()` is wired to real filesystem data. `setEntrypoint`/`setXmlInput` perform real I/O.

## Threat Flags

No new threat surface beyond what is documented in the plan's threat model. `validateProjectRelativePath` (T-03-01 mitigation) and `ProjectConfig` compact constructor provide two-layer defense for all path inputs.

## Next Phase Readiness

- Phase 3 Plan 02 (FileTreeController) can now bind `TreeView.setRoot` to `projectFilesProperty()` via a cell factory
- Phase 3 Plan 03 (menu/role actions) can call `setEntrypoint(path)` and `setXmlInput(path)` directly from context menu handlers
- No blockers. `projectFilesProperty()` is non-null and ready for binding before `openProject()` is called (returns empty list).

## Self-Check

- `src/main/java/ch/ti/gagi/xlseditor/ui/ProjectContext.java` — exists and contains all required methods
- `src/test/java/ch/ti/gagi/xlseditor/model/ProjectContextTest.java` — 26 @Test methods confirmed
- Commits `ca5b416` (test RED) and `fdfc6b6` (feat GREEN) — both exist in git log
- `./gradlew test` exits 0

## Self-Check: PASSED

---
*Phase: 03-file-tree-view*
*Completed: 2026-04-17*
