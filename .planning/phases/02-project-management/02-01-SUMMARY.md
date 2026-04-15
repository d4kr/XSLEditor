---
phase: 02-project-management
plan: "01"
subsystem: model-service
tags: [model, service, tdd, project-management, path-traversal, junit]
dependency_graph:
  requires: []
  provides:
    - ProjectConfig.write(Path) — JSON serialization of partial config
    - ProjectConfig relaxed constructor — null fields allowed (D-03)
    - ProjectConfig.read() null-safe — JSON NullNode returns Java null
    - ProjectManager.loadProject() missing-config path — returns partial Project (D-01)
    - ProjectContext — state service with BooleanProperty + createFile() + openProject()
  affects:
    - RenderOrchestrator — D-03 TODO comment added on null entryPoint assumption
tech_stack:
  added:
    - org.junit.platform:junit-platform-launcher:1.10.0 (testRuntimeOnly — required by Gradle 9)
  patterns:
    - JUnit Jupiter 5.10.0 with @TempDir for filesystem tests
    - Jackson LinkedHashMap-based write() with null-field omission
    - Dual-layer path traversal guard: pattern block + normalize().startsWith() check
key_files:
  created:
    - src/main/java/ch/ti/gagi/xlseditor/ui/ProjectContext.java
    - src/test/java/ch/ti/gagi/xlseditor/model/ProjectConfigTest.java
    - src/test/java/ch/ti/gagi/xlseditor/model/ProjectContextTest.java
  modified:
    - src/main/java/ch/ti/gagi/xlseditor/model/ProjectConfig.java
    - src/main/java/ch/ti/gagi/xlseditor/model/ProjectManager.java
    - src/main/java/ch/ti/gagi/xlseditor/render/RenderOrchestrator.java
    - build.gradle
decisions:
  - "Null fields in ProjectConfig are allowed (D-03) — both entryPoint and xmlInput are optional for partial config state"
  - "ProjectContext placed in ui package (not model) because it holds JavaFX BooleanProperty"
  - "junit-platform-launcher added as testRuntimeOnly — Gradle 9 no longer auto-adds it"
  - "createFile() uses dual-layer guard: validateSimpleFilename() blocks patterns, then normalize().startsWith(root) as defense-in-depth"
metrics:
  duration_seconds: 240
  completed_date: "2026-04-15"
  tasks_completed: 3
  tasks_total: 3
  files_created: 3
  files_modified: 4
---

# Phase 02 Plan 01: Model and Service Layer for Project Management Summary

**One-liner:** Relaxed ProjectConfig with JSON write(), null-safe read(), partial-project ProjectManager, and path-traversal-guarded ProjectContext state service — TDD cycle with 14 green JUnit Jupiter tests.

---

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Bootstrap test source tree (RED) | d7e5da2 | ProjectConfigTest.java, ProjectContextTest.java |
| 2 | Relax ProjectConfig + fix ProjectManager (GREEN partial) | 0cef2fd | ProjectConfig.java, ProjectManager.java, RenderOrchestrator.java |
| 3 | Create ProjectContext state service (GREEN all) | fd0a2de | ProjectContext.java, build.gradle |

---

## TDD Gate Compliance

- **RED gate:** commit `d7e5da2` — `test(02-01): add failing test stubs for ProjectConfig.write and ProjectContext` — compile failed on missing `write()` and `ProjectContext` (expected).
- **GREEN gate:** commit `fd0a2de` — `feat(02-01): add ProjectContext state service and fix JUnit Platform launcher` — all 14 tests pass.
- No REFACTOR gate needed — code was clean as written.

---

## Test Results

`./gradlew test` exits 0.

**ProjectConfigTest (7 tests — all PASS):**
- `writesJsonFile` — write() produces valid JSON with both fields
- `writesJsonFileOmittingNullFields` — null xmlInput omitted from JSON output
- `readsPartialConfig` — missing xmlInput reads as Java null (D-03)
- `readsExplicitNullFields` — JSON `null` values read as Java null (pitfall A2 guard)
- `rejectsAbsoluteEntryPoint` — absolute path throws IllegalArgumentException
- `rejectsAbsoluteXmlInput` — absolute path throws IllegalArgumentException
- `allowsBothFieldsNull` — `new ProjectConfig(null, null)` accepted (D-03)

**ProjectContextTest (7 tests — all PASS):**
- `openProjectWithoutConfigReturnsPartialProject` — empty dir returns partial Project, isProjectLoaded() true
- `createFileWritesEmptyFile` — creates zero-byte file at project root
- `createFileRejectsParentTraversal` — `../escape.xsl` throws IllegalArgumentException, no file created
- `createFileRejectsSubdirectory` — `sub/nested.xsl` throws IllegalArgumentException
- `createFileRejectsAbsolutePath` — `/etc/passwd` throws IllegalArgumentException
- `createFileRejectsBlankFilename` — blank and whitespace-only names throw IllegalArgumentException
- `createFileWithoutOpenProjectThrows` — no project loaded throws IllegalStateException

**Total: 14 tests, 0 failures, 0 errors.**

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Missing JUnit Platform launcher for Gradle 9**
- **Found during:** Task 3 (first test run)
- **Issue:** Gradle 9 no longer auto-adds `junit-platform-launcher` to the test runtime classpath. Running `./gradlew test` produced: "Failed to load JUnit Platform. Please ensure that all JUnit Platform dependencies are available."
- **Fix:** Added `testRuntimeOnly 'org.junit.platform:junit-platform-launcher:1.10.0'` to `build.gradle`. Version matches the `junit-bom:5.10.0` constraint already in the dependency graph.
- **Files modified:** `build.gradle`
- **Commit:** fd0a2de

### Caller Audit (ProjectConfig D-03)

**RenderOrchestrator** at lines 23, 32, 52, 61 calls `project.entryPoint()` without null checks. These calls will NPE if render is triggered on a partial project. Added `// Phase 2 D-03` TODO comment to `render()` method. The `renderSafe()` method will wrap in a try-catch so it returns `RenderResult.failure` rather than crashing — acceptable per T-02-03 (internal tool, stack traces acceptable). The UI wiring in Plan 02 must ensure render is only called when entryPoint is set.

---

## Known Stubs

None — all model/service layer APIs are fully wired. No placeholder data flows to UI rendering in this plan (UI wiring is Plan 02).

---

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: path-traversal | ProjectContext.java | New `createFile(String)` method accepts untrusted filename string — mitigated by `validateSimpleFilename()` + `normalize().startsWith(root)` defense-in-depth. Covered by T-02-01 and 3 dedicated unit tests. |

---

## Self-Check: PASSED

- `src/main/java/ch/ti/gagi/xlseditor/ui/ProjectContext.java` — FOUND
- `src/main/java/ch/ti/gagi/xlseditor/model/ProjectConfig.java` — FOUND (contains `public void write(`)
- `src/main/java/ch/ti/gagi/xlseditor/model/ProjectManager.java` — FOUND (contains `Files.exists(configPath)`)
- `src/test/java/ch/ti/gagi/xlseditor/model/ProjectConfigTest.java` — FOUND (7 @Test)
- `src/test/java/ch/ti/gagi/xlseditor/model/ProjectContextTest.java` — FOUND (7 @Test)
- Commit `d7e5da2` — FOUND (RED test stubs)
- Commit `0cef2fd` — FOUND (ProjectConfig + ProjectManager)
- Commit `fd0a2de` — FOUND (ProjectContext + build.gradle)
- All 14 tests PASS, 0 failures
