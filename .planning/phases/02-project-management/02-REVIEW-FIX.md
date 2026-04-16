---
phase: 02-project-management
fixed_at: 2026-04-15T00:00:00Z
review_path: .planning/phases/02-project-management/02-REVIEW.md
iteration: 1
findings_in_scope: 4
fixed: 4
skipped: 0
status: all_fixed
---

# Phase 02: Code Review Fix Report

**Fixed at:** 2026-04-15
**Source review:** .planning/phases/02-project-management/02-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 4 (1 Critical, 3 Warning)
- Fixed: 4
- Skipped: 0

## Fixed Issues

### CR-01: NullPointerException when `xmlInput` is null in `RenderOrchestrator`

**Files modified:** `src/main/java/ch/ti/gagi/xlseditor/render/RenderOrchestrator.java`
**Commit:** 6484a67
**Applied fix:** Added null guards for both `project.entryPoint()` and `project.xmlInput()` in `render()` (throws `IllegalStateException`) and in `renderSafe()` (returns `RenderResult.failure(...)`) before each respective `rootPath.resolve()` call. This prevents `NullPointerException` when a partial project has either field unset.

---

### WR-01: Duplicate render pipeline in `RenderOrchestrator` — steps 3-7 copy-pasted verbatim

**Files modified:** `src/main/java/ch/ti/gagi/xlseditor/render/RenderOrchestrator.java`
**Commit:** 19d2861
**Applied fix:** Extracted the shared steps 3-7 (null-guard, load XSLT, preprocess, compile, transform, render FO) into a private `executePipeline(Project, Path)` method. Both `render()` and `renderSafe()` now delegate to it. The null guards from CR-01 live exclusively in `executePipeline`, eliminating the duplication that caused the original CR-01 miss.

---

### WR-02: `ProjectManager.loadProject` resolves relative paths without validating they stay inside the project root

**Files modified:** `src/main/java/ch/ti/gagi/xlseditor/model/ProjectManager.java`
**Commit:** 7037450
**Applied fix:** After reading the config, paths are resolved against the absolute-normalized root and checked with `startsWith(root)` before use. A path that escapes the root (e.g. `../../etc/passwd`) throws `IOException`. Paths are then relativized back before being passed to `Project` to preserve the existing relative-path convention.

---

### WR-03: Concurrent `showTransientStatus` calls cancel each other without stopping the previous `PauseTransition`

**Files modified:** `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java`
**Commit:** c0951af
**Applied fix:** Added a `private PauseTransition statusPause` field. `showTransientStatus` now calls `statusPause.stop()` before creating a new transition, preventing two live timers from racing to clear the label. The new transition is stored back into the field so subsequent calls can stop it.

---

_Fixed: 2026-04-15_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
