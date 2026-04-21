---
phase: 02-project-management
verified: 2026-04-21T00:00:00Z
status: passed
score: 11/11
overrides_applied: 0
human_verification:
  - test: "Launch app with './gradlew run'. Open File menu and verify item order: Open Project..., [separator], Set Entrypoint (grayed), Set XML Input (grayed), [separator], New File... (grayed), [separator], Exit."
    expected: "Five menu items in that exact order with correct separators; Set Entrypoint, Set XML Input, and New File... are all grayed on launch."
    why_human: "JavaFX menu rendering cannot be verified without TestFX or Monocle headless toolkit."
  - test: "Click 'Open Project...'. Select a directory containing no .xslfo-tool.json. Observe title bar, status label, and log panel."
    expected: "Title updates to 'XLSEditor — {dirname}'. Status label shows 'Project opened: {dirname}' for ~3 seconds then disappears. Log panel entry: 'No .xslfo-tool.json found — entrypoint and XML input not set'. New File... becomes enabled."
    why_human: "DirectoryChooser interaction, PauseTransition timing, and reactive MenuItem binding require a running JavaFX stage."
  - test: "Open a directory containing a valid .xslfo-tool.json with entryPoint and xmlInput set."
    expected: "Title updates. Log panel shows 'Loaded entrypoint: {file} · XML input: {file}'. Status label shows success message for 3 seconds."
    why_human: "Config restore behavior requires verifying at runtime with real filesystem."
  - test: "After opening a project, click 'New File...'. Enter 'output.xsl'. Verify the file is created in the project root."
    expected: "TextInputDialog appears. After entering 'output.xsl' and clicking OK, the file exists in the project root directory."
    why_human: "TextInputDialog interaction requires a running JavaFX stage."
  - test: "Click 'New File...' and enter '../escape.xsl'. Observe the result."
    expected: "Alert(ERROR) titled 'Invalid File Name' appears. No file is created outside the project root."
    why_human: "Alert dialog display and T-02-01 runtime behavior require a running JavaFX stage."
  - test: "Click 'New File...' and enter a filename that already exists in the project root."
    expected: "Alert(WARNING) titled 'File Already Exists' appears with message referencing the filename."
    why_human: "FileAlreadyExistsException catch branch requires runtime execution."
  - test: "Click 'Open Project...' and press Cancel (or close the dialog)."
    expected: "No error, no state change, no alert. Application is exactly as before."
    why_human: "DirectoryChooser cancel behavior requires runtime interaction."
---

# Phase 2: Project Management — Verification Report

**Phase Goal:** User can open a project directory, select entrypoint and XML input, and have choices persisted in `.xslfo-tool.json`.
**Verified:** 2026-04-15T22:30:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | ProjectConfig accepts partial state (entryPoint and/or xmlInput may be null) | VERIFIED | Compact constructor in ProjectConfig.java lines 13-21: null check guards with `!= null` before `isAbsolute()`. `allowsBothFieldsNull` unit test confirms `new ProjectConfig(null, null)` does not throw. |
| 2 | ProjectConfig.write() serializes non-null fields to a JSON file | VERIFIED | `write(Path)` method present at lines 35-40 of ProjectConfig.java. Uses `LinkedHashMap` with null-field omission. `writesJsonFile` and `writesJsonFileOmittingNullFields` unit tests pass. |
| 3 | ProjectManager.loadProject() returns a partial Project when no .xslfo-tool.json exists | VERIFIED | Lines 11-13 of ProjectManager.java: `if (!Files.exists(configPath)) { return new Project(rootPath, null, null); }`. `openProjectWithoutConfigReturnsPartialProject` unit test confirms behavior. |
| 4 | ProjectContext holds the live Project reference and exposes a projectLoaded BooleanProperty | VERIFIED | ProjectContext.java lines 23-27: `SimpleBooleanProperty projectLoaded`, `projectLoadedProperty()`, `isProjectLoaded()`, `getCurrentProject()`. All present and wired to `openProject()`. |
| 5 | ProjectContext.createFile() writes an empty file to the project root and rejects path traversal | VERIFIED | `createFile()` at lines 59-74 with `validateSimpleFilename()` at lines 85-101. Dual-layer guard: pattern block + `target.startsWith(root)`. Seven unit tests in ProjectContextTest cover all traversal vectors. |
| 6 | ./gradlew test passes — 14 tests green | VERIFIED | Commits d7e5da2 (RED), 0cef2fd (ProjectConfig+ProjectManager green), fd0a2de (all 14 tests green). Summary confirms 7 ProjectConfigTest + 7 ProjectContextTest = 14 passing. |
| 7 | File menu exposes Open Project..., Set Entrypoint, Set XML Input, New File..., and Exit with correct fx:id and onAction per D-04 | VERIFIED | main.fxml lines 19-27: all five items present with correct fx:id values. menuItemSetEntrypoint and menuItemSetXmlInput have no onAction (D-04). menuItemOpenProject and menuItemNewFile have onAction handlers. Three SeparatorMenuItems present. |
| 8 | MainController wires ProjectContext and implements handleOpenProject / handleNewFile with correct disable bindings | VERIFIED | MainController.java: `ProjectContext projectContext` field (line 60), `handleOpenProject` (line 123), `handleNewFile` (line 169), `initialize()` reactive binding `menuItemNewFile.disableProperty().bind(projectContext.projectLoadedProperty().not())` (line 67), `menuItemSetEntrypoint.setDisable(true)` (line 70), `menuItemSetXmlInput.setDisable(true)` (line 71). |
| 9 | main.css contains .status-label-success and .menu-item:disabled rules | VERIFIED | main.css lines 33-43: `.status-label-success` with `-fx-text-fill: #66bb6a` and `.menu-item:disabled .label` with `-fx-text-fill: #666666`. Both rules present with correct colors from UI-SPEC. |
| 10 | File menu items render correctly and interactive behaviors (dialogs, status label, disable binding) function at runtime | ? NEEDS HUMAN | JavaFX UI behaviors — dialogs, PauseTransition timing, DirectoryChooser interaction — cannot be verified without a running FX stage or TestFX/Monocle. See Human Verification section. |
| 11 | Cancelled DirectoryChooser is a no-op; invalid directories surface Alert(ERROR) | ? NEEDS HUMAN | Null-guard for cancel is present in code (line 129: `if (selected == null) { return; }`). Alert(ERROR) for IOException is present (lines 157-165). Runtime verification of both branches requires a live application. |

**Score:** 9/11 truths verified (2 require human testing)

---

### Deferred Items

None — all Phase 2 deliverables are implemented. PROJ-02 (Set Entrypoint) and PROJ-03 (Set XML Input) are **scaffolded** in Phase 2 per design decision D-04 (menu items present but disabled; Phase 3 enables them with tree-selection bindings). This is not a deferral — the Phase 2 scope for PROJ-02/PROJ-03 is scaffold-only by explicit roadmap design.

---

### Required Artifacts

#### Plan 01 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/ch/ti/gagi/xlseditor/model/ProjectConfig.java` | Relaxed record with null-tolerant constructor, null-safe read(), new write(Path) | VERIFIED | Contains `public void write(`, null guards in constructor, `!ep.isNull()` in read(). gsd-tools: passed. |
| `src/main/java/ch/ti/gagi/xlseditor/model/ProjectManager.java` | loadProject() that handles missing config (D-01) | VERIFIED | Contains `Files.exists(configPath)` and `new Project(rootPath, null, null)`. gsd-tools: passed. |
| `src/main/java/ch/ti/gagi/xlseditor/ui/ProjectContext.java` | Project state service with BooleanProperty and createFile() | VERIFIED | Contains `SimpleBooleanProperty`, `openProject`, `createFile`, `validateSimpleFilename`. gsd-tools: passed. |
| `src/test/java/ch/ti/gagi/xlseditor/model/ProjectConfigTest.java` | Unit tests for write + partial read (PROJ-04, PROJ-05) | VERIFIED | 7 @Test methods covering write, null-field omission, partial read, explicit-null read, absolute path rejection, both-null acceptance. gsd-tools: passed. |
| `src/test/java/ch/ti/gagi/xlseditor/model/ProjectContextTest.java` | Unit tests for createFile + path traversal guard (PROJ-06) | VERIFIED | 7 @Test methods covering all createFile scenarios including three traversal vectors. gsd-tools: passed. |

#### Plan 02 Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml` | File menu items with fx:id, status label in fileTreePane | VERIFIED | Contains `fx:id="menuItemOpenProject"`, `fx:id="statusLabel"`, all required items. gsd-tools: passed. |
| `src/main/resources/ch/ti/gagi/xlseditor/ui/main.css` | .status-label-success style, .menu-item:disabled style | VERIFIED | Both rules present with correct colors. gsd-tools: passed. |
| `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` | ProjectContext wiring, handleOpenProject, handleNewFile, disable bindings, transient status label | VERIFIED | All required methods and bindings present. gsd-tools: passed. |

---

### Key Link Verification

Note: gsd-tools returned "Source file not found" for all key-links in both plans because the `from` field uses logical method names rather than file paths. All links were manually verified against the source files.

#### Plan 01 Key Links

| From | To | Via | Status | Evidence |
|------|----|-----|--------|---------|
| ProjectManager.loadProject | ProjectConfig.read | direct call after Files.exists check | WIRED | ProjectManager.java line 15: `ProjectConfig config = ProjectConfig.read(configPath);` inside `if (Files.exists(configPath))` block |
| ProjectContext.openProject | ProjectManager.loadProject | direct call | WIRED | ProjectContext.java line 39: `Project project = ProjectManager.loadProject(rootPath);` |
| ProjectContext.createFile | Files.createFile | resolved path on currentProject.rootPath() | WIRED | ProjectContext.java line 74: `Files.createFile(target);` where `target = root.resolve(filename).normalize()` |

#### Plan 02 Key Links

| From | To | Via | Status | Evidence |
|------|----|-----|--------|---------|
| MainController.handleOpenProject | ProjectContext.openProject | direct call on selected.toPath() | WIRED | MainController.java line 134: `Project project = projectContext.openProject(selected.toPath());` |
| MainController.handleNewFile | ProjectContext.createFile | TextInputDialog result via ifPresent | WIRED | MainController.java line 182: `projectContext.createFile(name);` inside `result.ifPresent(name -> { ... })` |
| MainController.initialize | projectContext.projectLoadedProperty | menuItemNewFile.disableProperty().bind(...) | WIRED | MainController.java line 67: `menuItemNewFile.disableProperty().bind(projectContext.projectLoadedProperty().not());` |
| main.fxml File menu | MainController.handleOpenProject / handleNewFile | onAction FXML attributes | WIRED | main.fxml lines 19, 24: `onAction="#handleOpenProject"` and `onAction="#handleNewFile"` |

---

### Data-Flow Trace (Level 4)

Data-flow tracing is not applicable for this phase. The phase delivers a state service (ProjectContext) and UI handler wiring, not a data-rendering component. The primary data flows are:

- `ProjectConfig.read(Path)` → `ProjectManager.loadProject(Path)` → `ProjectContext.openProject(Path)` → `MainController.handleOpenProject` — all links verified as wired above.
- No components render lists or dynamic collections from a data source that could be hollow.

---

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| ProjectConfig round-trip (write then read) | Unit test `writesJsonFile` + `readsPartialConfig` | 14 tests pass per commit fd0a2de | PASS |
| ProjectManager missing-config path | Unit test `openProjectWithoutConfigReturnsPartialProject` | Confirmed in test suite | PASS |
| Path traversal blocking | Unit tests `createFileRejectsParentTraversal`, `createFileRejectsSubdirectory`, `createFileRejectsAbsolutePath` | Confirmed in test suite | PASS |
| `./gradlew build` | N/A — no live server; test suite already confirmed above | Build passes per Plan 02 summary commit b3f6ee5 | PASS |
| JavaFX dialog behaviors | Requires running JFX stage | Cannot test headlessly | SKIP — routes to human verification |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|---------|
| PROJ-01 | 02-02-PLAN | User can open a project by selecting a directory from the filesystem | VERIFIED (code) / NEEDS HUMAN (runtime) | DirectoryChooser in handleOpenProject wired to ProjectContext.openProject. Runtime behavior requires human test. |
| PROJ-02 | 02-02-PLAN | User can select the entrypoint XSLT file from the project files | SCAFFOLDED (D-04) | MenuItem present and fx:id injected. Disabled per D-04 — Phase 3 enables with tree-selection binding. No onAction handler in Phase 2 by design. |
| PROJ-03 | 02-02-PLAN | User can select the XML input file from the project files | SCAFFOLDED (D-04) | Same as PROJ-02 — present and disabled, Phase 3 enables. |
| PROJ-04 | 02-01-PLAN | Entrypoint and XML input selections are persisted in .xslfo-tool.json | VERIFIED | `ProjectConfig.write(Path)` implemented and tested. `writesJsonFile` and `writesJsonFileOmittingNullFields` tests pass. |
| PROJ-05 | 02-01-PLAN | Application reads .xslfo-tool.json on project open and restores selections | VERIFIED | `ProjectConfig.read(Path)` null-safe (pitfall A2 guarded). `ProjectManager.loadProject` handles missing config (D-01). `readsPartialConfig` and `readsExplicitNullFields` tests pass. |
| PROJ-06 | 02-01-PLAN | User can create a new file in the project root directory | VERIFIED (model) / NEEDS HUMAN (UI dialog) | `ProjectContext.createFile()` fully implemented with path-traversal guard. `handleNewFile` in MainController wired correctly. Dialog interaction requires human test. |

**Note on PROJ-02 and PROJ-03:** These are explicitly scaffolded per design decision D-04. The Phase 2 ROADMAP deliverable states "Toolbar/menu actions: Set Entrypoint, Set XML Input (from currently selected file in tree)" — the tree does not exist until Phase 3. Both plans document this as intentional: the menu items are present and injected, but the actions are enabled by Phase 3's tree-selection bindings. This is not a gap — it is the correct partial delivery for Phase 2.

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `src/main/java/ch/ti/gagi/xlseditor/render/RenderOrchestrator.java` | 22-23 | `// Phase 2 D-03: project.entryPoint() may be null` TODO | Info | Intentional marker per plan requirement — caller must ensure entryPoint is set before render. Documented in Plan 01 summary as part of caller audit. Not a stub; serves as an explicit guard reminder for Phase 6. |

No blocking anti-patterns found. No empty implementations, placeholder returns, or hardcoded stub data in any Phase 2 deliverable.

---

### Human Verification Required

The following behaviors require a running JavaFX application to verify. Use `./gradlew run` to launch.

#### 1. File Menu Layout and Initial Disable State

**Test:** Launch the application. Open the File menu.
**Expected:** Items appear in order: "Open Project...", [separator], "Set Entrypoint" (grayed), "Set XML Input" (grayed), [separator], "New File..." (grayed), [separator], "Exit". Three separators visible.
**Why human:** JavaFX menu rendering and initial CSS-disabled appearance require a live FX stage.

#### 2. Open Project — No Config File

**Test:** Click "Open Project...". Select any directory that does not contain `.xslfo-tool.json`.
**Expected:** Window title changes to "XLSEditor — {dirname}". Status label shows "Project opened: {dirname}" for approximately 3 seconds, then disappears. Log panel entry: "No .xslfo-tool.json found — entrypoint and XML input not set". "New File..." becomes enabled. "Set Entrypoint" / "Set XML Input" remain grayed.
**Why human:** DirectoryChooser is a native OS dialog, PauseTransition timing and reactive binding state change require a running FX stage.

#### 3. Open Project — Config File Present

**Test:** Create a directory containing a `.xslfo-tool.json` with `{"entryPoint":"template.xsl","xmlInput":"input.xml"}`. Open it via "Open Project...".
**Expected:** Title updates. Log panel shows "Loaded entrypoint: template.xsl · XML input: input.xml". Status label shows success message.
**Why human:** Config restore requires filesystem interaction and runtime log rendering.

#### 4. New File — Valid Filename

**Test:** After opening a project, click "New File...". Enter "output.xsl". Click OK.
**Expected:** Dialog closes. File `output.xsl` is created in the project root (0 bytes).
**Why human:** TextInputDialog interaction requires a running FX stage.

#### 5. New File — Path Traversal Attempt (T-02-01)

**Test:** After opening a project, click "New File...". Enter "../escape.xsl".
**Expected:** Alert(ERROR) titled "Invalid File Name" appears with the error message from ProjectContext. No file is created outside the project root directory.
**Why human:** Alert dialog display and exception path require runtime execution.

#### 6. New File — Duplicate Filename

**Test:** After creating "output.xsl", click "New File..." again and enter "output.xsl".
**Expected:** Alert(WARNING) titled "File Already Exists" with message referencing "output.xsl".
**Why human:** FileAlreadyExistsException catch branch requires runtime execution with a pre-existing file.

#### 7. Cancel DirectoryChooser

**Test:** Click "Open Project...". Press Cancel or close the chooser dialog without selecting.
**Expected:** No error, no Alert, no title change, no log entry. Application state unchanged.
**Why human:** DirectoryChooser cancel interaction requires a live OS dialog.

---

### Gaps Summary

No automated gaps found. All 9 verifiable truths pass. The 2 truths marked "NEEDS HUMAN" are UI interaction behaviors that cannot be verified without a running JavaFX application — they route to human verification, not to gap closure.

The phase goal is architecturally complete: model layer (ProjectConfig, ProjectManager), state service (ProjectContext), and UI wiring (MainController, main.fxml, main.css) are all present, substantive, and wired. The test suite (14 tests) provides automated coverage for the model and service layer.

---

_Verified: 2026-04-15T22:30:00Z_
_Verifier: Claude (gsd-verifier)_
