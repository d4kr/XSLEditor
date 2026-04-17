---
phase: 03-file-tree-view
plan: 02
subsystem: ui
tags: [javafx, treecell, css, record, unicode-glyph]

# Dependency graph
requires:
  - phase: 03-file-tree-view
    plan: 01
    provides: ProjectContext.projectFilesProperty() — live ObservableList<Path> consumed by Plan 03 FileTreeController
provides:
  - FileItem record (Path + FileRole enum) — immutable data carrier for TreeView nodes
  - FileItemTreeCell — custom TreeCell<FileItem> with glyph prefix, CSS class driver, role-based tooltip
  - Phase 3 CSS rules in main.css — seven rules covering file-tree-header, file-tree-view, tree-cell default/hover/selected, entrypoint accent, xml-input accent
affects:
  - 03-03: FileTreeController imports FileItem and passes FileItemTreeCell constructor to fileTree.setCellFactory(...)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - JavaFX TreeCell recycling pattern: unconditional getStyleClass().removeAll() before updateItem branch applies new CSS class
    - No-arg cell factory constructor: event wiring stays on the TreeView (Plan 03), not on individual cells — avoids per-cell lambda allocations
    - Unicode glyph prefix via setText (no graphic node) — color driven entirely by CSS, zero inline -fx-text-fill

key-files:
  created:
    - src/main/java/ch/ti/gagi/xlseditor/ui/FileItem.java
    - src/main/java/ch/ti/gagi/xlseditor/ui/FileItemTreeCell.java
  modified:
    - src/main/resources/ch/ti/gagi/xlseditor/ui/main.css

key-decisions:
  - "FileItemTreeCell uses a no-arg constructor (deviates from 03-PATTERNS.md Consumer<Path> sketch): event wiring belongs on the TreeView in Plan 03 via fileTree.setOnMouseClicked — cells are state-free, factory is tv -> new FileItemTreeCell() with zero captured references"
  - "TreeCell recycling handled by unconditional removeAll(CSS_CLASS_ENTRYPOINT, CSS_CLASS_XML_INPUT) before any updateItem branch sets new styling — prevents stale accent colors when cells are reused for different FileItems"
  - "No Phase 1 or Phase 2 CSS rule was modified — Phase 3 block appended after last Phase 2 rule with matching blank-line separator convention"

requirements-completed: [TREE-02, TREE-03]

# Metrics
duration: ~2min
completed: 2026-04-17
---

# Phase 3 Plan 02: FileItem Record, FileItemTreeCell Renderer, and Phase 3 CSS Summary

**FileItem immutable record plus FileItemTreeCell custom TreeCell renderer (Unicode glyph + CSS class + role-based tooltip) and seven Phase 3 CSS rules appended to main.css — pure rendering primitives for Plan 03 to wire into FileTreeController**

## Performance

- **Duration:** ~2 min
- **Started:** 2026-04-17T05:28:26Z
- **Completed:** 2026-04-17T05:30:00Z
- **Tasks:** 3
- **Files modified:** 3 (2 created, 1 appended)

## Accomplishments

- Created `FileItem` as a Java record with compact non-null constructor: `Objects.requireNonNull` on both `path` and `role`. Nested `FileRole` enum has exactly three values: `ENTRYPOINT`, `XML_INPUT`, `REGULAR`. Record auto-derives equals/hashCode so the TreeView detects role changes (e.g. REGULAR → ENTRYPOINT) even when the Path is unchanged.
- Created `FileItemTreeCell` as a `final` class extending `TreeCell<FileItem>` with a no-arg constructor. `updateItem` maps each `FileRole` to a Unicode glyph prefix (`▶ ` / `■ ` / `□ `), a CSS class (`entrypoint` / `xml-input` / none), and a tooltip (`Entrypoint XSLT` / `XML Input` / null). The unconditional `getStyleClass().removeAll(CSS_CLASS_ENTRYPOINT, CSS_CLASS_XML_INPUT)` before every branch prevents stale colors on recycled cells.
- Appended seven Phase 3 CSS rules to `main.css` verbatim from `03-UI-SPEC.md § CSS Classes to Add`: `.file-tree-header`, `.file-tree-view`, `.file-tree-view .tree-cell`, `.file-tree-view .tree-cell:hover`, `.file-tree-view .tree-cell:selected`, `.file-tree-view .tree-cell.entrypoint`, `.file-tree-view .tree-cell.xml-input`. All Phase 1 and Phase 2 rules preserved unchanged.
- `./gradlew build -x test` and `./gradlew test` both exit 0. All 26 Plan 01 tests remain green.

## Task Commits

Each task was committed atomically:

1. **Task 1: Create FileItem record with FileRole enum** — `81848cb` (feat)
2. **Task 2: Create FileItemTreeCell custom renderer** — `5dd8851` (feat)
3. **Task 3: Append Phase 3 CSS rules to main.css** — `24a74b8` (feat)

## Files Created/Modified

- `src/main/java/ch/ti/gagi/xlseditor/ui/FileItem.java` — new; public record with compact non-null constructor and nested FileRole enum
- `src/main/java/ch/ti/gagi/xlseditor/ui/FileItemTreeCell.java` — new; final TreeCell subclass, no-arg constructor, switch-expression on FileRole, unconditional CSS class cleanup
- `src/main/resources/ch/ti/gagi/xlseditor/ui/main.css` — appended seven Phase 3 rules after line 43; no existing rule modified

## Decisions Made

1. **FileItemTreeCell has a no-arg constructor (deviates from 03-PATTERNS.md Consumer<Path> sketch):** The patterns doc sketched a `Consumer<Path> onOpenRequest` field on the cell for double-click handling. The plan specification overrides this: event handlers (double-click / Enter) belong on the `TreeView` itself in Plan 03 via `fileTree.setOnMouseClicked`. A state-free cell keeps the factory to `tv -> new FileItemTreeCell()` with zero captured references and avoids per-cell lambda allocations. Plan 03 is the wiring plan — this plan is a rendering primitive.

2. **Unconditional CSS class cleanup in updateItem:** JavaFX virtualizes TreeCell instances — the same cell object may render an ENTRYPOINT row, then be scrolled off-screen and recycled to render a REGULAR row. Without `removeAll(CSS_CLASS_ENTRYPOINT, CSS_CLASS_XML_INPUT)` before each branch, recycled cells carry forward stale `.entrypoint` or `.xml-input` classes, causing wrong text colors. The removeAll call is the first operation after `super.updateItem` — before the empty/null check — so it fires unconditionally on every updateItem invocation.

3. **No Phase 1/2 CSS rule modified:** The Phase 3 block is appended after the last Phase 2 rule (`.menu-item:disabled .label`), separated by a single blank line matching the Phase 1→Phase 2 spacing convention. All seven Phase 3 comments use the `/* Phase 3: ... */` header pattern for grepping. `grep -c "/* Phase 3:" main.css` == 7 confirmed.

## Deviations from Plan

### Design Deviation (not a deviation error)

**1. [No-arg constructor — matches plan spec, deviates from 03-PATTERNS.md sketch]**
- **Found during:** Plan authoring (pre-task note in Task 2)
- **Issue:** 03-PATTERNS.md sketched `Consumer<Path> onOpenRequest` in the cell; plan specification explicitly overrides this with a rationale
- **Resolution:** Implemented no-arg constructor as specified in the plan. Rationale documented in plan and in this summary (event wiring on TreeView, not on cells).
- **Files modified:** `FileItemTreeCell.java`

No other deviations. All three tasks executed exactly as written.

## Known Stubs

None. FileItem and FileItemTreeCell are pure rendering primitives with no data wiring of their own. Data wiring (binding TreeView to projectFilesProperty) is Plan 03's responsibility.

## Threat Flags

No new threat surface. T-03-07 (stale CSS class on recycled TreeCell) is mitigated by the unconditional `removeAll` in `updateItem` — confirmed by grep acceptance criterion (getStyleClass().removeAll is present and fires first).

## Self-Check

- `src/main/java/ch/ti/gagi/xlseditor/ui/FileItem.java` — FOUND
- `src/main/java/ch/ti/gagi/xlseditor/ui/FileItemTreeCell.java` — FOUND
- Commit `81848cb` (Task 1) — FOUND in git log
- Commit `5dd8851` (Task 2) — FOUND in git log
- Commit `24a74b8` (Task 3) — FOUND in git log
- `./gradlew test` exits 0 — PASSED (26 tests green)
- `grep "/* Phase 3:" main.css | wc -l` == 7 — CONFIRMED

## Self-Check: PASSED

---
*Phase: 03-file-tree-view*
*Completed: 2026-04-17*
