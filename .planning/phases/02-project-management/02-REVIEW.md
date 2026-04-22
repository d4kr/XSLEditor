---
phase: 02-project-management
reviewed: 2026-04-15T00:00:00Z
depth: standard
files_reviewed: 10
files_reviewed_list:
  - build.gradle
  - src/main/java/ch/ti/gagi/xsleditor/model/ProjectConfig.java
  - src/main/java/ch/ti/gagi/xsleditor/model/ProjectManager.java
  - src/main/java/ch/ti/gagi/xsleditor/render/RenderOrchestrator.java
  - src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java
  - src/main/java/ch/ti/gagi/xsleditor/ui/ProjectContext.java
  - src/main/resources/ch/ti/gagi/xsleditor/ui/main.css
  - src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
  - src/test/java/ch/ti/gagi/xsleditor/model/ProjectConfigTest.java
  - src/test/java/ch/ti/gagi/xsleditor/model/ProjectContextTest.java
findings:
  critical: 1
  warning: 3
  info: 2
  total: 6
status: issues_found
---

# Phase 02: Code Review Report

**Reviewed:** 2026-04-15
**Depth:** standard
**Files Reviewed:** 10
**Status:** issues_found

## Summary

Phase 02 delivers project open/load, config persistence, file creation, and the UI scaffolding for menus and status feedback. The overall structure is clean and the security controls around path traversal in `ProjectContext.createFile` are solid and well-tested. The critical finding is a crash path in `RenderOrchestrator.render()` and `renderSafe()` when `project.xmlInput()` is null — both call `rootPath.resolve(project.xmlInput())` without a null guard, mirroring the null-check discipline already applied to `project.entryPoint()` in the comment on line 22 but not extended to `xmlInput`. Three warnings cover a duplicate rendering pipeline, a missing null guard in `ProjectManager`, and the fact that the `PauseTransition` in `MainController` is replaced rather than stopped on repeated calls. Two info items flag a missing read round-trip test and an FXML status-label placement that may surprise future phases.

---

## Critical Issues

### CR-01: NullPointerException when `xmlInput` is null in `RenderOrchestrator`

**File:** `src/main/java/ch/ti/gagi/xsleditor/render/RenderOrchestrator.java:44` (and `:73`)

**Issue:** Both `render()` and `renderSafe()` call `rootPath.resolve(project.xmlInput())` without checking whether `xmlInput` is null. `Project.xmlInput()` is explicitly allowed to be null (partial project). When a project has no XML input configured, `Path.resolve(null)` throws `NullPointerException`, which in `render()` is unhandled and in `renderSafe()` is silently swallowed by the broad `catch (Exception e)` — a poor error message. The comment on line 22 acknowledges the entryPoint null concern but does not extend the guard to `xmlInput`.

**Fix:**
```java
// In render() — add before step 6:
if (project.xmlInput() == null) {
    throw new IllegalStateException("No XML input configured for this project");
}
Path xmlPath = rootPath.resolve(project.xmlInput()).normalize();

// In renderSafe() — add before step 6:
if (project.xmlInput() == null) {
    return RenderResult.failure(List.of(
        ErrorManager.fromException(new IllegalStateException("No XML input configured"))));
}
Path xmlPath = rootPath.resolve(project.xmlInput()).normalize();
```

The same guard should be added for `project.entryPoint()` in both methods (lines 25/53) for symmetry, as the comment on line 22 warns but the code does not enforce it.

---

## Warnings

### WR-01: Duplicate render pipeline in `RenderOrchestrator` — steps 3-7 copy-pasted verbatim

**File:** `src/main/java/ch/ti/gagi/xsleditor/render/RenderOrchestrator.java:34-48` and `63-78`

**Issue:** `render()` and `renderSafe()` share an identical 5-step pipeline body (load XSLT, preprocess, compile, transform, render FO). Any bug fix or change to the pipeline must be applied twice. The only difference is how errors are surfaced. This is a maintenance hazard: CR-01 is itself a consequence of this duplication (the null guard added to `render()` was not replicated to `renderSafe()`).

**Fix:** Extract the shared pipeline into a private helper that returns `byte[]`, then wrap calls in each public method:
```java
private byte[] executePipeline(Project project, Path rootPath) throws Exception {
    // guard nulls once, execute steps 3-7
}

public byte[] render(Project project, Path rootPath) throws Exception {
    DependencyGraph graph = DependencyResolver.buildGraph(rootPath, project.entryPoint());
    List<ValidationError> errors = ValidationEngine.validateProject(rootPath, project, graph);
    if (!errors.isEmpty()) { throw new IllegalStateException(...); }
    return executePipeline(project, rootPath);
}

public RenderResult renderSafe(Project project, Path rootPath) {
    try {
        DependencyGraph graph = DependencyResolver.buildGraph(rootPath, project.entryPoint());
        List<ValidationError> errors = ValidationEngine.validateProject(rootPath, project, graph);
        if (!errors.isEmpty()) { return RenderResult.failure(...); }
        return RenderResult.success(executePipeline(project, rootPath));
    } catch (Exception e) {
        return RenderResult.failure(List.of(ErrorManager.fromException(e)));
    }
}
```

### WR-02: `ProjectManager.loadProject` resolves relative paths without validating they stay inside the project root

**File:** `src/main/java/ch/ti/gagi/xsleditor/model/ProjectManager.java:16-17`

**Issue:** `Path.of(config.entryPoint())` and `Path.of(config.xmlInput())` turn raw strings from the JSON config file into `Path` objects. Although `ProjectConfig` rejects absolute paths, a value like `../../etc/passwd` stored in `.xslfo-tool.json` passes that check and is stored as a relative `Path`. When `RenderOrchestrator` later calls `rootPath.resolve(entryPoint).normalize()` no bounds check is applied — the normalized path can escape the project root. The `createFile` path in `ProjectContext` has this protection; the config-load path does not.

**Fix:** Add a containment check in `ProjectManager.loadProject` after resolving:
```java
Path root = rootPath.toAbsolutePath().normalize();

Path ep = config.entryPoint() != null
    ? root.resolve(config.entryPoint()).normalize()
    : null;
if (ep != null && !ep.startsWith(root)) {
    throw new IOException("entryPoint escapes project root: " + config.entryPoint());
}

Path xi = config.xmlInput() != null
    ? root.resolve(config.xmlInput()).normalize()
    : null;
if (xi != null && !xi.startsWith(root)) {
    throw new IOException("xmlInput escapes project root: " + config.xmlInput());
}
return new Project(rootPath, ep, xi);
```

Note: `Project` stores relative paths (the constructor receives relative `Path` objects from `ProjectManager`). If the intent changes to storing absolute paths, update accordingly — but the bounds check must exist somewhere before the path reaches the file system.

### WR-03: Concurrent `showTransientStatus` calls cancel each other without stopping the previous `PauseTransition`

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java:249-254`

**Issue:** Each call to `showTransientStatus` creates a new `PauseTransition` and calls `play()`, but does not stop any previous transition still running. If the user opens two projects in quick succession (within 3 seconds), two transitions are alive simultaneously. Both fire `setOnFinished`, both call `statusLabel.setText("")` and `removeAll("status-label-success")`. The label text is overwritten immediately by the second call, then blanked twice by the two `onFinished` callbacks. The visual result is unpredictable (the second message may vanish early).

**Fix:** Store the transition as a field and stop it before creating a new one:
```java
private PauseTransition statusPause;

private void showTransientStatus(String message) {
    if (statusPause != null) {
        statusPause.stop();
    }
    statusLabel.setText(message);
    statusLabel.getStyleClass().removeAll("status-label-success");
    statusLabel.getStyleClass().add("status-label-success");

    statusPause = new PauseTransition(Duration.seconds(3));
    statusPause.setOnFinished(e -> {
        statusLabel.setText("");
        statusLabel.getStyleClass().removeAll("status-label-success");
    });
    statusPause.play();
}
```

---

## Info

### IN-01: `ProjectConfigTest` has no round-trip test (write then read back)

**File:** `src/test/java/ch/ti/gagi/xsleditor/model/ProjectConfigTest.java`

**Issue:** The `writesJsonFile` test verifies that the written JSON contains expected substrings, but no test calls `write()` followed by `read()` to assert that the deserialized value equals the original. A round-trip regression would not be caught — for example, a serialization format change that writes `entry_point` instead of `entryPoint` would pass the write test but break real load behaviour.

**Fix:** Add a round-trip test:
```java
@Test
void roundTrip(@TempDir Path tempDir) throws IOException {
    Path configPath = tempDir.resolve(".xslfo-tool.json");
    ProjectConfig original = new ProjectConfig("templates/main.xsl", "data/input.xml");
    original.write(configPath);
    ProjectConfig loaded = ProjectConfig.read(configPath);
    assertEquals(original.entryPoint(), loaded.entryPoint());
    assertEquals(original.xmlInput(), loaded.xmlInput());
}
```

### IN-02: `statusLabel` placed inside `fileTreePane` in FXML — unexpected coupling

**File:** `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml:43-46`

**Issue:** The `statusLabel` (fx:id) is a child of `fileTreePane` (the file tree `StackPane`), aligned to `BOTTOM_CENTER`. When Phase 3 replaces or clears the content of `fileTreePane` to install the `TreeView`, the `statusLabel` node may be removed or hidden behind the tree. A status bar belongs in a dedicated bottom zone (e.g. inside the `BorderPane`'s bottom, or a second row of the bottom `VBox`), not nested inside a functional pane that downstream phases will replace.

**Fix:** Move `statusLabel` out of `fileTreePane`. A minimal approach is to wrap the `BorderPane`'s `bottom` in a `VBox` containing both the `TitledPane` log and the status label:
```xml
<bottom>
    <VBox>
        <Label fx:id="statusLabel" styleClass="placeholder-label" style="-fx-padding: 4 8;"/>
        <TitledPane fx:id="logPane" text="Log" expanded="false" styleClass="log-titled-pane">
            <ListView fx:id="logListView" prefHeight="150"/>
        </TitledPane>
    </VBox>
</bottom>
```

This keeps the status label visible regardless of what Phase 3 does to `fileTreePane`.

---

_Reviewed: 2026-04-15_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
