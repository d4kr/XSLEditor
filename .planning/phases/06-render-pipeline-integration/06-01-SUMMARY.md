---
phase: 06-render-pipeline-integration
plan: "01"
subsystem: render-pipeline
tags: [render, controller, skeleton, wave-0, tdd-nyquist]
dependency_graph:
  requires:
    - 05-05-SUMMARY.md  # SearchDialog/Find-in-Files complete (editor sub-system stable)
  provides:
    - RenderController compile-green skeleton with correct signatures
    - EditorController.saveAll() public API
    - RenderControllerTest disabled stubs (REND-02, REND-04, REND-05)
  affects:
    - src/main/java/ch/ti/gagi/xlseditor/ui/EditorController.java
    - src/main/java/ch/ti/gagi/xlseditor/ui/RenderController.java
    - src/test/java/ch/ti/gagi/xlseditor/ui/RenderControllerTest.java
tech_stack:
  added: []
  patterns:
    - "Sub-controller initialized via initialize() called from MainController (same as FileTreeController/EditorController)"
    - "BooleanProperty.bind() for button disable state"
    - "saveAll() propagates IOException — caller decides; does NOT swallow like saveTab()"
    - "Wave 0 skeleton + @Disabled test stubs = Nyquist compliance before implementation"
key_files:
  created:
    - src/main/java/ch/ti/gagi/xlseditor/ui/RenderController.java
    - src/test/java/ch/ti/gagi/xlseditor/ui/RenderControllerTest.java
  modified:
    - src/main/java/ch/ti/gagi/xlseditor/ui/EditorController.java
decisions:
  - "RenderController.previewManager eagerly instantiated (field initializer) — simple; no DI needed for desktop tool"
  - "saveAll() throws IOException rather than showing alert — RenderController caller decides error UX (D-09)"
  - "Three @Disabled stubs map 1:1 to REND-02, REND-04, REND-05 automated requirements; REND-01/REND-06 are manual"
metrics:
  duration: "~10 minutes"
  completed: "2026-04-19"
  tasks_completed: 2
  tasks_total: 2
  files_created: 2
  files_modified: 1
---

# Phase 06 Plan 01: Wave 0 Skeleton — RenderController + saveAll() + Test Stubs Summary

**One-liner:** Compile-green RenderController skeleton wired to PreviewManager, EditorController.saveAll() with IOException propagation, and three @Disabled Nyquist test stubs for Wave 1.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Create RenderController skeleton | 562d3d7 | `src/main/java/.../ui/RenderController.java` (created) |
| 2 | Add saveAll() + RenderControllerTest stubs | a11993a | `EditorController.java` (modified), `RenderControllerTest.java` (created) |

## What Was Built

### Task 1 — RenderController.java

`public final class RenderController` follows the established sub-controller pattern (same shape as `FileTreeController`):

- **Fields:** `renderButton`, `logListView`, `statusSet`, `statusTransient`, `pdfCallback`, `outdatedCallback`, `projectContext`, `editorController`, and an eagerly instantiated `previewManager`
- **`initialize()`**: eight-argument method, all `Objects.requireNonNull`-guarded; binds `renderButton.disableProperty()` to `projectContext.projectLoadedProperty().not()` (REND-02 base case)
- **`handleRender()`**: empty Wave 0 stub — Wave 1 fills the full Task lifecycle

### Task 2A — EditorController.saveAll()

Added immediately after `saveTab()`. Key contract differences vs `saveTab()`:
- Iterates the entire `registry` (all open tabs)
- Only writes tabs where `et.dirty.get()` is true
- **Propagates `IOException`** to the caller — no `showError()` dialog (caller decides)
- Calls `updateAppDirtyState()` after all saves

### Task 2B — RenderControllerTest.java

Three `@Disabled` stubs with `@BeforeAll Platform.startup()` pattern (mirrors `EditorTabTest`):
- `handleRender_doesNothing_whenProjectIsNull` — maps to REND-02
- `handleRender_callsPdfCallback_onSuccess` — maps to REND-04
- `handleRender_routesErrorsToLog_onFailure` — maps to REND-05

REND-01 (full pipeline E2E) and REND-06 (< 5s performance) are manual-only per VALIDATION.md.

## Verification

```
./gradlew test
BUILD SUCCESSFUL — zero failures, three @Disabled skips
```

## Deviations from Plan

None — plan executed exactly as written.

## Threat Surface Scan

No new network endpoints, auth paths, or file access patterns introduced. `saveAll()` writes only to paths already opened from the user's filesystem (same as `saveTab()`). Threat register entries T-06-01 and T-06-02 remain accurate.

## Known Stubs

- `RenderController.handleRender()` — intentionally empty; Wave 1 (06-02-PLAN.md) implements the full Task lifecycle. This is a planned Wave 0 skeleton, not an accidental stub.
- All three `RenderControllerTest` methods — `@Disabled` pending Wave 1 implementation.

## Self-Check: PASSED

- `src/main/java/ch/ti/gagi/xlseditor/ui/RenderController.java` — FOUND
- `src/test/java/ch/ti/gagi/xlseditor/ui/RenderControllerTest.java` — FOUND
- `EditorController.java` contains `saveAll()` — FOUND
- Commit 562d3d7 — FOUND
- Commit a11993a — FOUND
- `./gradlew test` BUILD SUCCESSFUL — VERIFIED
