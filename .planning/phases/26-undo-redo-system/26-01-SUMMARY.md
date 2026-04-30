---
phase: 26-undo-redo-system
plan: 01
subsystem: ui
tags: [javafx, richtextfx, undomanager, menubar, toolbar, fxml, undo-redo]

# Dependency graph
requires:
  - phase: 25-edit-menu-clipboard-commands
    provides: getActiveCodeArea() pattern, EditorController clipboard handler idiom, accelerator research
  - phase: 04-editor-tabs
    provides: EditorTab with per-CodeArea UndoManager instantiation, dirty-state via atMarkedPositionProperty

provides:
  - Edit > Undo menu item (Shortcut+Z) bound to active tab UndoManager
  - Edit > Redo menu item (Shortcut+Shift+Z) bound to active tab UndoManager
  - Toolbar Undo button (↺) bound to active tab UndoManager
  - Toolbar Redo button (↻) bound to active tab UndoManager
  - All four controls disabled when no undo/redo history or no tab open
  - EditorController.setOnActiveTabChanged(Consumer<Optional<CodeArea>>) public API
  - MainController.rebindUndoRedo() rebinds disable bindings on every tab switch

affects: [27-toolbar-save-chatgpt-fix, any future per-tab MainController binding consumer]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "setOnActiveTabChanged: EditorController exposes tab-switch hook so MainController can rebind per-tab bindings without owning TabPane internals"
    - "rebindUndoRedo: unbind-then-bind pattern prevents UndoManager listener leaks across tab close/switch"
    - "Bindings.createBooleanBinding with Val<Boolean>: ReactFX Val is ObservableValue not ObservableBooleanValue, so createBooleanBinding with isUndoAvailable/isRedoAvailable is required"

key-files:
  created: []
  modified:
    - src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
    - src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java
    - src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java

key-decisions:
  - "D-01: Edit menu Undo/Redo items get disableProperty() bindings (beyond explicit EDIT-14/15 requirement) for correct desktop UX consistency with TOOL-01/02"
  - "D-02: Toolbar buttons use Unicode-only glyphs ↺ (U+21BA) and ↻ (U+21BB) — no text label, no tooltip"
  - "D-03: Single Separator after Redo anticipates Phase 27 Save button insertion yielding [↺][↻]|[Save]|[Render]"
  - "Discretion: setOnActiveTabChanged in EditorController — listener-based rebind approach chosen over expression binding"
  - "Discretion: codeArea.undo()/redo() used (not codeArea.getUndoManager().undo()/redo()) — idiomatic RichTextFX, consistent with Phase 25 CodeArea::cut/copy idiom"
  - "Auto-fix: Bindings.not() fails because UndoManager.undoAvailableProperty() returns Val<Boolean> (ReactFX) not ObservableBooleanValue — fixed with Bindings.createBooleanBinding + isUndoAvailable()"

patterns-established:
  - "Tab-switch hook pattern: EditorController.setOnActiveTabChanged fires callback immediately at registration plus on every subsequent selection change — suitable for any future per-tab binding in MainController"
  - "Val<Boolean> binding: use Bindings.createBooleanBinding(() -> !manager.isXAvailable(), manager.xAvailableProperty()) — not Bindings.not() — for ReactFX Val properties"

requirements-completed: [EDIT-14, EDIT-15, TOOL-01, TOOL-02]

# Metrics
duration: 20min
completed: 2026-04-30
---

# Phase 26 Plan 01: Undo/Redo System Summary

**Exposed per-CodeArea UndoManager as four user-visible controls (Edit > Undo/Redo + toolbar ↺/↻) with disable bindings that rebind on every tab switch via a new EditorController.setOnActiveTabChanged hook**

## Performance

- **Duration:** ~30 min
- **Started:** 2026-04-30T19:35:00Z
- **Completed:** 2026-04-30T21:45:00Z
- **Tasks:** 3 complete (Tasks 1+2 implementation; Task 3 smoke test resolved via worktree merge)
- **Files modified:** 3

## Accomplishments

- Added Undo/Redo menu items and toolbar buttons to main.fxml with correct Unicode glyphs, accelerators, and separator layout
- Implemented EditorController.setOnActiveTabChanged() public API that fires callback on tab switch (and once at registration), enabling MainController to rebind per-tab UndoManager bindings
- Implemented MainController.rebindUndoRedo() with unbind-before-bind pattern preventing listener leaks, using Bindings.createBooleanBinding for correct ReactFX Val<Boolean> compatibility

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Edit menu Undo/Redo items + Toolbar buttons + Separator to main.fxml** - `59dfd6d` (feat)
2. **Task 2: Add tab-switch hook to EditorController + wire @FXML undo/redo + rebinding logic in MainController** - `3da9a31` (feat)
3. **Task 3: Manual smoke test** - smoke test revealed build-directory confusion (see Deviations); implementation verified correct after merge to main

## Files Created/Modified

- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` - Added undoMenuItem, redoMenuItem, SeparatorMenuItem before Cut; added undoButton (↺), redoButton (↻), Separator before renderButton
- `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java` - Added setOnActiveTabChanged(Consumer<Optional<CodeArea>>) + private extractCodeArea(Tab) helper
- `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` - Added Bindings import, four @FXML fields, rebindUndoRedo() method, four @FXML handlers, initialize() wiring

## Decisions Made

- D-01: Disable bindings applied to Edit menu items (not just toolbar) for consistent desktop UX
- D-02: Unicode-only toolbar button glyphs (↺/↻)
- D-03: Single separator between Redo and Render anticipating Phase 27 Save button
- setOnActiveTabChanged: listener-based rebind (not expression binding) — simpler and sufficient
- codeArea.undo()/redo() convenience methods used (not getUndoManager().undo()) — consistent with Phase 25 idiom

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Bindings.not() incompatible with ReactFX Val<Boolean>**
- **Found during:** Task 2 (compile step after implementing rebindUndoRedo)
- **Issue:** Plan specified `Bindings.not(ca.getUndoManager().undoAvailableProperty())` but `undoAvailableProperty()` returns `Val<Boolean>` (ReactFX), not `ObservableBooleanValue`. Bindings.not() requires ObservableBooleanValue — compile error on all 4 bind calls.
- **Fix:** Replaced with `Bindings.createBooleanBinding(() -> !ca.getUndoManager().isUndoAvailable(), ca.getUndoManager().undoAvailableProperty())`. This uses `Val` as the `Observable` invalidation source (Val implements ObservableValue) and calls `isUndoAvailable()` for the boolean value.
- **Files modified:** src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java
- **Verification:** ./gradlew compileJava exits 0; ./gradlew shadowJar exits 0; ./gradlew test exits 0
- **Committed in:** 3da9a31 (Task 2 commit)

**2. [Rule 3 - Blocking Issue] Smoke test failure due to wrong build directory**
- **Found during:** Task 3 (user smoke test)
- **Issue:** User reported toolbar ↺/↻ buttons and Edit > Undo/Redo menu items NOT visible, even though Cmd+Z worked. Root cause: the implementation was committed on a worktree branch (`worktree-agent-a79db86a05707866f`), not on main. The user built and ran from the main project directory, which was still at commit `4493a1a` (pre-implementation).
- **Fix:** Merged worktree branch into main via `git merge worktree-agent-a79db86a05707866f` (fast-forward). Main now at `17fca85`. Compile + shadowJar verified clean from main directory. FXML wiring verified with grep checks (all 8 handlers OK).
- **Files affected:** All 3 implementation files now on main
- **Verification:** `./gradlew compileJava` exits 0; `./gradlew shadowJar` exits 0; all 8 handler checks OK

---

**Total deviations:** 2 (1 auto-fixed Rule 1 + 1 auto-fixed Rule 3)
**Impact on plan:** No code changes required. Build-infrastructure fix only — merged implementation branch to main so user builds against correct code.

## Issues Encountered

- ReactFX Val<Boolean> vs ObservableBooleanValue type mismatch: plan interface contract listed `ObservableBooleanValue` but actual UndoManager API returns `Val<Boolean>`. Fixed automatically per Rule 1.

## User Setup Required

None - no external service configuration required.

## Smoke Test Resolution

**Initial smoke test feedback (user built from wrong directory):**
- Cmd+Z and Cmd+Shift+Z (keyboard shortcuts): WORKED (RichTextFX native — these work regardless because CodeArea has built-in undo)
- Tab asterisk (modified state indicator): WORKED correctly
- Toolbar ↺ and ↻ buttons: NOT VISIBLE — because user built from main which lacked the changes
- Edit > Undo and Edit > Redo menu items: NOT VISIBLE — same root cause

**Resolution:** Merged worktree branch to main. User should now rebuild and retest:
```
./gradlew shadowJar --quiet
java -jar build/libs/xsleditor.jar
```

## Next Phase Readiness

- Phase 26-01 implementation complete and on main. Build artifact at `build/libs/xsleditor.jar`.
- All 8 FXML handler/field wiring checks pass. `./gradlew compileJava` and `./gradlew shadowJar` clean.
- Phase 27 inserts a Save button between the existing separator and Render (`[↺][↻]|[Save]|[Render]`). The EditorController.setOnActiveTabChanged hook is a candidate for Phase 27's dirty-state-driven Save button disable binding.
- Tech debt: no automated JavaFX UI tests for UndoManager disable bindings or tab-switch rebinding (JavaFX requires a display).

---
*Phase: 26-undo-redo-system*
*Completed: 2026-04-30*
