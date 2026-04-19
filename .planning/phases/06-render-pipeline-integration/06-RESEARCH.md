# Phase 6: Render Pipeline Integration - Research

**Researched:** 2026-04-19
**Domain:** JavaFX Task<T> background execution, FXML ToolBar wiring, existing pipeline DTOs
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Add a `ToolBar` node in FXML below the `MenuBar` (inside the `BorderPane` top zone, after MenuBar via VBox). Render button lives in the toolbar.
- **D-02:** Also add a "Render" menu item under the existing empty "Run" menu in MenuBar. Both toolbar button and menu item trigger the same action handler.
- **D-03:** Keyboard shortcut: **F5**. Wired to the menu item accelerator.
- **D-04:** Introduce a dedicated `RenderController` class. `MainController` holds a reference and calls `renderController.initialize(...)` from its own `initialize()`. `RenderController` owns: Task lifecycle, button state, progress feedback, result routing.
- **D-05:** Button label changes from `"Render"` to `"Rendering..."` while Task runs. Button is disabled during execution. Reverts on completion (success or failure).
- **D-06:** No cancel support — button stays disabled for the duration.
- **D-07:** Render button disabled when: no project loaded OR entrypoint null OR xmlInput null (REND-02). Bind to `ProjectContext.projectLoadedProperty()` AND null-checks on entrypoint/xmlInput.
- **D-08:** Auto-save all dirty editor tabs silently before the pipeline runs. No confirmation dialog.
- **D-09:** `EditorController.saveAll()` method handles save-all. `RenderController` calls it before spawning the Task. If saveAll throws (disk error), show error alert and abort render.
- **D-10:** Use existing `statusLabel` in `MainController` for progress text. `RenderController` receives a `Consumer<String>` callback (or reference to `showTransientStatus`).
- **D-11:** During render: status label shows `"Rendering..."` (persistent, NOT a timed fade).
- **D-12:** On success: status label shows `"Render complete (X.Xs)"` then auto-clears after 3 seconds via `PauseTransition`.
- **D-13:** On failure: status label shows `"Render failed"` then auto-clears after 3 seconds.
- **D-14:** Render duration: `System.currentTimeMillis()` delta from Task start to completion. Logged as INFO in logListView AND shown in status label on success.
- **D-15:** On success: call a `Consumer<byte[]>` callback (or `previewController.displayPdf(byte[])` seam) with `Preview.pdf()`. Phase 7 wires actual display; Phase 6 scaffolds a no-op stub.
- **D-16:** On failure: append `Preview.errors()` to `logListView.getItems()` as formatted strings. Format: `"[ERROR] {type}: {message}"` with `" @ {file}:{line}"` appended if present.
- **D-17:** Log panel cleared before each new render. `logListView.getItems().clear()` called on FX thread before spawning Task.

### Claude's Discretion

- Exact ToolBar + VBox FXML structure (how toolbar integrates with existing BorderPane top zone)
- Whether `renderableProperty()` is a computed property on `ProjectContext` or derived inline in `RenderController`
- Whether `RenderController` receives `EditorController` by constructor or setter
- `PreviewManager` instantiation location (RenderController constructor, or `XLSEditorApp.start()` wiring)
- Error string formatting details beyond `"[ERROR] {type}: {message}"` prefix

### Deferred Ideas (OUT OF SCOPE)

- Cancel button / Task.cancel() support
- LogManager observable binding to logListView (Phase 8)
- PDFViewerFX actual rendering (Phase 7)
- ProgressIndicator spinner in toolbar
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| REND-01 | Render button triggers the full pipeline (save-all → preprocess → validate → transform → render → preview) | `PreviewManager.generatePreview(Project, Path)` fully implements the pipeline; `EditorController.saveAll()` is the save-all seam; `Task<Preview>` orchestrates both in sequence |
| REND-02 | Render is disabled when no project is loaded, no entrypoint set, or no XML input set | `ProjectContext.projectLoadedProperty()` is an observable `BooleanProperty`; `Project.entryPoint()` and `Project.xmlInput()` are inspected at action time; disable logic belongs in `RenderController` |
| REND-03 | During rendering, UI shows a progress/loading indicator | `statusLabel` (existing `@FXML Label`) shows persistent `"Rendering..."` text during Task execution; no new UI node needed |
| REND-04 | Successful render updates the PDF preview | `Preview.pdf()` returns `byte[]`; Phase 6 scaffolds a `Consumer<byte[]>` seam; Phase 7 fills it |
| REND-05 | Failed render keeps the previous PDF preview and marks it as outdated | On failure the `Consumer<byte[]>` is NOT called; `Preview.outdated()` is available on the DTO if Phase 7 needs it; errors go to logListView |
| REND-06 | Render target: < 5 seconds for typical projects | Pipeline is Saxon + FOP; these are I/O-bound; running on a background `Task` thread prevents FX thread blocking; no explicit timing constraint enforcement needed in Phase 6 beyond keeping render off the FX thread |
</phase_requirements>

---

## Summary

Phase 6 wires an already-complete backend pipeline (`PreviewManager` / `RenderOrchestrator`) into the JavaFX UI. All heavy lifting (XSLT compile, XML transform, FOP render) is done in existing classes that were implemented before any UI work began. The UI work is: add a ToolBar to the FXML, add a Render item to the existing empty "Run" menu, build a `RenderController` sub-controller that owns the `Task<Preview>` lifecycle, and route success/failure back to `statusLabel`, `logListView`, and the PDF seam.

The established controller pattern (dedicated sub-controller, `initialize(...)` called from `MainController.initialize()`, callbacks for cross-controller communication) is already proven in `FileTreeController` and `EditorController`. `RenderController` is the third instance of this pattern and follows the same shape exactly.

The key gap to close before implementation: `EditorController` does not yet have a `saveAll()` public method. This must be added as part of Phase 6 Wave 0. All other integration seams (`statusLabel`, `logListView`, `previewPane`, `projectContext`) already exist with correct `fx:id` or object references.

**Primary recommendation:** Build `RenderController` as a minimal sub-controller that delegates immediately to `PreviewManager.generatePreview(...)` inside a `Task<Preview>`, using the callback pattern already established by `EditorController` and `FileTreeController`. No new patterns, no new libraries.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Render trigger (button, menu, F5) | UI / FXML + RenderController | — | User gesture → action handler; FXML owns layout, RenderController owns handler |
| Save-all before render | EditorController (saveAll()) | RenderController (caller) | EditorController owns tab state; RenderController orchestrates the pre-render sequence |
| Background pipeline execution | RenderController (Task<Preview>) | PreviewManager (called inside Task) | Task lifecycle is UI concern; actual work is in existing backend class |
| Progress feedback (status label) | RenderController → Consumer<String> → MainController | — | MainController owns the label; RenderController communicates via callback |
| PDF bytes delivery | RenderController → Consumer<byte[]> | Phase 7 fills consumer | Seam defined in Phase 6; implementation deferred to Phase 7 |
| Error routing to log panel | RenderController | logListView (direct list mutation) | Stopgap for Phase 6; Phase 8 replaces with LogManager binding |
| Render duration timing | RenderController | — | System.currentTimeMillis() delta; logged and shown in status |
| Disable-state computation | RenderController | ProjectContext (property source) | RenderController binds to observable properties; ProjectContext owns state |

---

## Standard Stack

### Core (all already in build.gradle — no new dependencies)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| javafx.concurrent.Task<T> | JavaFX 21 | Background work with FX-thread callbacks | Built into JavaFX; `setOnSucceeded`/`setOnFailed` called on FX thread automatically |
| javafx.scene.control.ToolBar | JavaFX 21 | Render button container below MenuBar | Standard JavaFX toolbar; integrates with FXML via `<?import?>` |
| javafx.beans.binding.Bindings | JavaFX 21 | Compose disable condition from multiple properties | `Bindings.or()` / `BooleanExpression.not()` for compound bindings |
| javafx.animation.PauseTransition | JavaFX 21 | 3-second auto-clear of status label | Already used in `MainController.showTransientStatus()` |

[VERIFIED: codebase grep — all four are already imported/used in the project]

### No New Dependencies Required

The backend pipeline is already compiled and available:
- `ch.ti.gagi.xlseditor.preview.PreviewManager` [VERIFIED: codebase read]
- `ch.ti.gagi.xlseditor.preview.Preview` [VERIFIED: codebase read]
- `ch.ti.gagi.xlseditor.preview.PreviewError` [VERIFIED: codebase read]
- `ch.ti.gagi.xlseditor.render.RenderOrchestrator` [VERIFIED: codebase read]

**Installation:** No new `build.gradle` changes needed for Phase 6.

---

## Architecture Patterns

### System Architecture Diagram

```
User clicks Render (ToolBar button | Run menu | F5)
        │
        ▼
RenderController.handleRender()
        │
        ├─ [REND-02 guard] project loaded? entryPoint != null? xmlInput != null?
        │   └─ NO → show Alert, return
        │
        ├─ [D-17] logListView.getItems().clear()
        │
        ├─ [D-08/D-09] editorController.saveAll()
        │   └─ throws IOException → show Alert, return
        │
        ├─ renderButton.setDisable(true), renderButton.setText("Rendering...")
        ├─ statusLabel: set "Rendering..." (persistent, no PauseTransition yet)
        │
        ├─ startTime = System.currentTimeMillis()
        │
        ▼
Task<Preview>.call()    [background thread — NOT FX thread]
        │
        └─ PreviewManager.generatePreview(project, rootPath)
                │
                ▼
        RenderOrchestrator.renderSafe(project, rootPath)   [never throws]
                │
                ├─ DependencyResolver.buildGraph()
                ├─ ValidationEngine.validateProject()
                ├─ LibraryPreprocessor.mergeLibraries()
                ├─ RenderEngine.compileXslt()
                ├─ RenderEngine.transformToString()
                └─ RenderEngine.renderFoToPdf()
                        │
                        ▼
                RenderResult → Preview DTO
                        │
                ┌───────┴────────┐
                │                │
          success()          failure()
                │                │
                ▼                ▼
        Task.setOnSucceeded  Task.setOnFailed
        [FX thread]          [FX thread]
                │                │
                ▼                ▼
    duration = now - start   logListView.getItems()
    statusLabel "Render       .add("[ERROR] ...")
    complete (X.Xs)"         statusLabel "Render failed"
    PauseTransition 3s       PauseTransition 3s
    Consumer<byte[]>
    .accept(preview.pdf())
    [Phase 7 stub: no-op]
                │
                ▼
    renderButton re-enabled,
    label reverts to "Render"
```

### Recommended Project Structure (new files only)

```
src/main/java/ch/ti/gagi/xlseditor/ui/
└── RenderController.java    # new sub-controller for render lifecycle

src/test/java/ch/ti/gagi/xlseditor/ui/
└── RenderControllerTest.java  # unit tests for pure-logic methods (error formatting, etc.)
```

**Modified files:**
- `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` — add `renderController` field + `initialize()` call + FXML `@FXML` refs (ToolBar button, Run menu item)
- `src/main/java/ch/ti/gagi/xlseditor/ui/EditorController.java` — add `saveAll()` public method
- `src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml` — add ToolBar in VBox, add "Render" item to "Run" menu

### Pattern 1: RenderController Shape (mirrors FileTreeController)

```java
// Source: FileTreeController.java (established pattern — VERIFIED: codebase read)
public final class RenderController {

    private Button renderButton;
    private ListView<String> logListView;
    private Consumer<String> statusCallback;   // D-10
    private Consumer<byte[]> pdfCallback;      // D-15
    private ProjectContext projectContext;
    private EditorController editorController;

    public void initialize(
        Button renderButton,
        ListView<String> logListView,
        Consumer<String> statusCallback,
        Consumer<byte[]> pdfCallback,
        ProjectContext projectContext,
        EditorController editorController
    ) { ... }

    public void handleRender() { ... }
}
```

### Pattern 2: JavaFX Task<T> for Background Work

```java
// Source: EditorController.java line 201 (Task usage — VERIFIED: codebase read)
// Pattern: create Task, set callbacks, submit to executor OR new Thread().start()
Task<Preview> task = new Task<>() {
    @Override
    protected Preview call() {
        return previewManager.generatePreview(project, rootPath);
    }
};
task.setOnSucceeded(e -> {
    Preview result = task.getValue();
    // runs on FX thread — safe to update UI here
    handleSuccess(result);
});
task.setOnFailed(e -> {
    // runs on FX thread — safe to update UI here
    handleFailure(task.getException());
});
Thread t = new Thread(task, "render-thread");
t.setDaemon(true);
t.start();
```

**Key fact:** `Task.setOnSucceeded` and `setOnFailed` callbacks execute on the JavaFX Application Thread automatically. `Platform.runLater` is NOT needed inside these callbacks. [VERIFIED: JavaFX 21 Task Javadoc pattern; ASSUMED based on training — confirmed by existing `Task<StyleSpans>` usage in EditorController.java lines 202-215 which already uses this exact pattern without `Platform.runLater`]

### Pattern 3: FXML VBox Wrapping MenuBar + ToolBar

```xml
<!-- Source: CONTEXT.md D-01 + Phase 1 CONTEXT D-05 (three-zone BorderPane — VERIFIED: codebase read) -->
<top>
    <VBox>
        <MenuBar fx:id="menuBar">
            <!-- existing menus -->
            <Menu text="Run">
                <MenuItem fx:id="menuItemRender"
                          text="Render"
                          accelerator="F5"
                          onAction="#handleRender"/>
            </Menu>
        </MenuBar>
        <ToolBar>
            <Button fx:id="renderButton" text="Render" onAction="#handleRender"/>
        </ToolBar>
    </VBox>
</top>
```

**Note:** The current `main.fxml` already has `<Menu text="Run"/>` (empty, line 30). Phase 6 adds the `<MenuItem>` inside it — no new menu node, just content. [VERIFIED: codebase read of main.fxml]

**Note on FXML imports:** `ToolBar` requires `<?import javafx.scene.control.ToolBar?>` (or include in wildcard `javafx.scene.control.*` — already present as `<?import javafx.scene.control.*?>` in main.fxml line 4). VBox requires `<?import javafx.scene.layout.VBox?>` — already present as `<?import javafx.scene.layout.*?>` in main.fxml line 5. [VERIFIED: codebase read of main.fxml]

### Pattern 4: Disable Binding (compound condition)

```java
// Source: MainController.java line 74 (existing binding pattern — VERIFIED: codebase read)
// REND-02: disable when no project OR no entrypoint OR no XML input
// Approach A (simpler, evaluated at action time): guard inside handleRender()
//   — avoids needing to observe entryPoint/xmlInput changes as properties
// Approach B (full reactive binding): add observable properties to ProjectContext
//   — requires more ProjectContext changes, overkill for Phase 6
//
// Recommendation (Claude's Discretion per D-07): Approach A for disable-at-action,
// plus bind disableProperty to projectLoadedProperty().not() for the basic project-not-loaded case.
renderButton.disableProperty().bind(projectContext.projectLoadedProperty().not());
// Runtime guard inside handleRender() checks entryPoint/xmlInput non-null.
```

### Pattern 5: EditorController.saveAll() (new method)

`EditorController` currently has `saveTab(EditorTab)` as a private method. Phase 6 needs a public `saveAll()` that iterates the registry.

```java
// Source: EditorController.java lines 299-308 (saveTab pattern — VERIFIED: codebase read)
public void saveAll() throws IOException {
    for (Tab tab : registry.values()) {
        if (tab.getUserData() instanceof EditorTab et && et.dirty.get()) {
            Files.writeString(et.path, et.codeArea.getText(), StandardCharsets.UTF_8);
            et.codeArea.getUndoManager().mark();
        }
    }
    updateAppDirtyState();
}
```

**Note:** `saveTab` currently shows an error dialog on `IOException` and swallows it. `saveAll()` for render should propagate the exception so `RenderController` can decide to abort. This is a different contract from `saveTab`.

### Anti-Patterns to Avoid

- **Updating UI inside `Task.call()`:** `Task.call()` runs on a background thread. ANY UI update (label text, button state, list mutations) inside `call()` is a threading violation. All UI changes go in `setOnSucceeded`/`setOnFailed` callbacks.
- **Using `Platform.runLater` inside `setOnSucceeded`/`setOnFailed`:** These callbacks are already on the FX thread. Wrapping them in `Platform.runLater` adds an unnecessary dispatch and can cause ordering bugs.
- **Leaving button disabled on Task exception:** If `Task.call()` throws an unchecked exception that isn't caught by `PreviewManager` (shouldn't happen since `renderSafe` catches everything), `setOnFailed` still fires — re-enable the button there too.
- **Not cancelling an in-progress PauseTransition before starting a new one:** `MainController.showTransientStatus` already handles this (stops previous transition). `RenderController` must NOT start a new PauseTransition while the "Rendering..." label is shown — only start PauseTransition in success/failure handlers.
- **Using `Thread(task)` without daemon flag:** A non-daemon thread will prevent JVM shutdown if the render is still running when the user closes the window. Always set `t.setDaemon(true)`.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Background render execution | Custom thread + callback mechanism | `javafx.concurrent.Task<T>` | Task handles thread safety, exception propagation, FX-thread callback dispatch, cancellation hooks automatically |
| Progress reporting | Polling loop / shared mutable flag | `Task.updateMessage()` or direct FX callback in setOnSucceeded | Task provides built-in progress/message properties bindable to UI |
| XML/XSLT/FOP processing | Any new code | `PreviewManager.generatePreview()` | Already implemented, tested, and handles all error cases |
| Error normalization | Custom exception parsing | `PreviewError` DTO (already produced by pipeline) | `PreviewError` carries `message`, `type`, `file`, `line` — all formatting info is there |

---

## Common Pitfalls

### Pitfall 1: Task Callback Threading
**What goes wrong:** Developer puts `renderButton.setDisable(false)` or `statusLabel.setText(...)` inside `Task.call()`. JavaFX throws `IllegalStateException: Not on FX application thread`.
**Why it happens:** `Task.call()` runs on the background thread. UI nodes are not thread-safe.
**How to avoid:** All UI mutations go in `setOnSucceeded` and `setOnFailed` (they fire on the FX thread). Button state changes (`setDisable`, `setText`) go there too.
**Warning signs:** Runtime `IllegalStateException` with "Not on FX application thread" in the stack trace.

### Pitfall 2: PreviewManager Instantiation
**What goes wrong:** `RenderController` instantiates `PreviewManager(new RenderOrchestrator())` in `handleRender()` (i.e., once per click). `RenderOrchestrator` creates a `RenderEngine` which may hold heavy Saxon/FOP state.
**Why it happens:** Forgetting that `PreviewManager` should be created once and reused.
**How to avoid:** Instantiate `PreviewManager` once — either in `RenderController`'s constructor/`initialize()`, or (if XLSEditorApp wiring is preferred) pass it in as a constructor argument.
**Warning signs:** Slower first render each time, memory churn.

### Pitfall 3: statusLabel Owned by MainController
**What goes wrong:** `RenderController` holds a direct reference to `statusLabel` (a private `@FXML` field of `MainController`). This creates a tight coupling and requires exposing the label or exposing a setter on `MainController`.
**Why it happens:** Convenience — the label is right there.
**How to avoid:** Pass a `Consumer<String>` callback into `RenderController.initialize()` (D-10). `MainController` passes `this::showTransientStatus` for auto-clearing messages, and a separate `label -> statusLabel.setText(label)` lambda for the persistent "Rendering..." state. Or: expose a `showPersistentStatus(String)` and `showTransientStatus(String)` pair on `MainController` and pass both.
**Warning signs:** `MainController` needs to expose its private label node to `RenderController`.

### Pitfall 4: Race Between Successive Renders
**What goes wrong:** User double-clicks Render quickly. Two Tasks spawn. The second Task completes first. The first Task overwrites the result with stale data.
**Why it happens:** No guard against launching a second Task while one is running.
**How to avoid:** The disable binding (D-05/D-06) prevents this: once render starts, the button is disabled until the Task completes. This is sufficient for Phase 6.
**Warning signs:** Log panel shows two render's worth of errors interleaved.

### Pitfall 5: saveAll() Contract Mismatch
**What goes wrong:** `saveAll()` uses the same error-swallowing pattern as `saveTab()` (shows error alert, continues). On disk error, render proceeds with unsaved (stale) content — the user never knows.
**Why it happens:** Copy-paste from `saveTab`.
**How to avoid:** `saveAll()` must throw `IOException` on failure (or return a boolean). `RenderController.handleRender()` catches this, shows an alert, and aborts before spawning the Task.
**Warning signs:** Render runs after a disk error with no user feedback about the save failure.

### Pitfall 6: FXML @FXML Injection for RenderController
**What goes wrong:** Developer puts `@FXML private Button renderButton` in `RenderController` and makes `RenderController` a secondary FXML controller. FXML doesn't support multiple controller classes without explicit `fx:include`.
**Why it happens:** Confusion with other frameworks.
**How to avoid:** Follow the established pattern: `MainController` holds `@FXML private Button renderButton` (injected from FXML), then passes the button reference to `renderController.initialize(renderButton, ...)`. `RenderController` is NOT annotated with `@FXML` fields.
**Warning signs:** `NullPointerException` on `renderButton` in `RenderController`.

---

## Code Examples

### RenderController.handleRender() Skeleton

```java
// Source: EditorController.java Task pattern (VERIFIED: codebase read)
//         FileTreeController.java initialize pattern (VERIFIED: codebase read)
public void handleRender() {
    // REND-02 guard
    Project project = projectContext.getCurrentProject();
    if (project == null || project.entryPoint() == null || project.xmlInput() == null) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Cannot Render");
        alert.setContentText("Set both an entrypoint XSLT and an XML input file before rendering.");
        alert.showAndWait();
        return;
    }

    // D-17: clear log before render
    logListView.getItems().clear();

    // D-08/D-09: save all dirty tabs
    try {
        editorController.saveAll();
    } catch (IOException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Save Failed");
        alert.setContentText("Could not save all files before render: " + e.getMessage());
        alert.showAndWait();
        return;
    }

    // D-05: disable button, update label
    renderButton.setDisable(true);
    renderButton.setText("Rendering...");
    // D-11: persistent status during render
    statusSetPersistent.accept("Rendering...");

    long startTime = System.currentTimeMillis();
    Path rootPath = project.rootPath();

    Task<Preview> task = new Task<>() {
        @Override
        protected Preview call() {
            return previewManager.generatePreview(project, rootPath);
        }
    };

    task.setOnSucceeded(e -> {
        Preview result = task.getValue();
        long duration = System.currentTimeMillis() - startTime;
        renderButton.setDisable(false);
        renderButton.setText("Render");

        if (result.success()) {
            // D-14: log duration
            logListView.getItems().add("[INFO] Render complete in " + (duration / 1000.0) + "s");
            // D-12: transient success status
            statusShowTransient.accept("Render complete (" + String.format("%.1f", duration / 1000.0) + "s)");
            // D-15: hand PDF bytes to preview seam
            pdfCallback.accept(result.pdf());
        } else {
            // D-16: route errors to log
            for (PreviewError err : result.errors()) {
                String entry = "[ERROR] " + err.type() + ": " + err.message();
                if (err.file() != null) {
                    entry += " @ " + err.file();
                    if (err.line() != null) entry += ":" + err.line();
                }
                logListView.getItems().add(entry);
            }
            // D-13: transient failure status
            statusShowTransient.accept("Render failed");
        }
    });

    task.setOnFailed(e -> {
        // renderSafe() never throws — this branch covers unexpected runtime exceptions
        renderButton.setDisable(false);
        renderButton.setText("Render");
        logListView.getItems().add("[ERROR] Unexpected render error: " + task.getException().getMessage());
        statusShowTransient.accept("Render failed");
    });

    Thread t = new Thread(task, "render-thread");
    t.setDaemon(true);
    t.start();
}
```

### EditorController.saveAll() (new public method)

```java
// Source: EditorController.java saveTab() lines 299-308 (VERIFIED: codebase read)
// NOTE: Throws IOException (unlike saveTab which shows alert + swallows) — see Pitfall 5
public void saveAll() throws IOException {
    for (Tab tab : registry.values()) {
        if (tab.getUserData() instanceof EditorTab et && et.dirty.get()) {
            Files.writeString(et.path, et.codeArea.getText(), StandardCharsets.UTF_8);
            et.codeArea.getUndoManager().mark();
        }
    }
    updateAppDirtyState();
}
```

### MainController initialize() addition

```java
// Source: MainController.java lines 85-89 pattern (VERIFIED: codebase read)
// In MainController.initialize():
renderController.initialize(
    renderButton,            // @FXML Button (new)
    menuItemRender,          // @FXML MenuItem (new) — for label toggle if needed
    logListView,             // existing @FXML
    statusLabel::setText,    // persistent setter: Consumer<String>
    this::showTransientStatus, // 3s auto-clear: Consumer<String>
    projectContext,
    editorController
);
```

---

## State of the Art

| Old Approach | Current Approach | Notes |
|--------------|------------------|-------|
| `SwingWorker` for background tasks | `javafx.concurrent.Task<T>` | Standard JavaFX approach; callbacks on FX thread |
| Shared mutable state for cross-controller comms | `Consumer<T>` callbacks / property bindings | Already used in this project |
| `Platform.runLater` for everything | `Task.setOnSucceeded`/`setOnFailed` | Task callbacks already on FX thread |

---

## Integration Point Map (verified against codebase)

| Integration Point | Existing fx:id / API | Current State | Phase 6 Action |
|-------------------|----------------------|---------------|----------------|
| `statusLabel` | `@FXML Label statusLabel` in MainController | Exists, used for transient messages | Pass callback to RenderController; add persistent-set variant |
| `logListView` | `@FXML ListView<String> logListView` in MainController | Exists, used by Phase 2 for project-load entries | Pass reference to RenderController; clear + append |
| `previewPane` | `@FXML StackPane previewPane` in MainController | Exists, contains WebView placeholder | No change in Phase 6; Consumer<byte[]> is a no-op stub |
| `projectContext` | `private final ProjectContext projectContext` in MainController | Exists, has `projectLoadedProperty()` and `getCurrentProject()` | Pass to RenderController |
| `editorController` | `private final EditorController editorController` in MainController | Exists; saveAll() NOT yet present | Add `saveAll()` to EditorController; pass ref to RenderController |
| `menuBar` / Run menu | `<Menu text="Run"/>` in main.fxml (line 30) | Empty menu, already declared | Add `<MenuItem fx:id="menuItemRender" .../>` inside it |
| ToolBar | Not present | Missing | Add `<VBox>` wrapping `<MenuBar>` + new `<ToolBar>` in `<top>` |
| `renderButton` | Not present | Missing | Add `<Button fx:id="renderButton" .../>` in ToolBar |

[VERIFIED: all integration points confirmed by codebase read of main.fxml, MainController.java, EditorController.java, ProjectContext.java]

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `Task.setOnSucceeded`/`setOnFailed` fire on the FX thread automatically (no `Platform.runLater` needed) | Architecture Patterns Pattern 2 | Low — corroborated by existing EditorController Task usage that makes no use of Platform.runLater in its callbacks |
| A2 | `PreviewManager.generatePreview()` is safe to call from any thread (no FX thread dependency) | Don't Hand-Roll | Low — it delegates to `RenderOrchestrator.renderSafe()` which uses Saxon + FOP, both thread-safe; no JavaFX imports in PreviewManager |
| A3 | A single `Thread(task)` is sufficient (no ExecutorService needed for render) | Architecture Patterns | Low — only one render runs at a time (button disabled during render); no need for a thread pool |

**All other claims verified by direct codebase read.**

---

## Open Questions (RESOLVED)

1. **`RenderController` receives `statusLabel` as two callbacks or one?**
   - What we know: D-10 says `Consumer<String>` callback; D-11 says persistent (no PauseTransition during render); D-12/D-13 say auto-clear with PauseTransition on completion.
   - What's unclear: Whether to pass `statusLabel::setText` (persistent) and `this::showTransientStatus` (auto-clearing) as two separate callbacks, or expose a new `showPersistentStatus(String)` method on `MainController`.
   - Recommendation (Claude's Discretion): Two callbacks — `Consumer<String> statusSet` (persistent) and `Consumer<String> statusTransient` (auto-clearing). Naming makes intent clear. `MainController` passes `s -> statusLabel.setText(s)` and `this::showTransientStatus`.

2. **`renderableProperty()` on `ProjectContext` vs. inline guard?**
   - What we know: D-07 allows either approach.
   - Recommendation: Inline guard in `handleRender()` for Phase 6 (simpler, no ProjectContext changes). Bind `renderButton.disableProperty()` only to `projectLoadedProperty().not()` — this covers the primary case. The entryPoint/xmlInput null-check is done at action time.

---

## Environment Availability

Step 2.6: SKIPPED — Phase 6 has no new external dependencies. All required tools (JDK 21, Gradle, JavaFX 21, Saxon, FOP) were verified in Phase 1 setup.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter 5.10.0 |
| Config file | `build.gradle` — `test { useJUnitPlatform() }` |
| Quick run command | `./gradlew test --tests "ch.ti.gagi.xlseditor.ui.RenderController*"` |
| Full suite command | `./gradlew test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| REND-01 | Pipeline called with correct project + rootPath | Integration (manual smoke) | n/a — requires live JavaFX stage | No — manual |
| REND-02 | Guard rejects null entryPoint / xmlInput | Unit | `./gradlew test --tests "*RenderController*"` | ❌ Wave 0 |
| REND-03 | Progress label set during render | Unit (mock callback) | `./gradlew test --tests "*RenderController*"` | ❌ Wave 0 |
| REND-04 | PDF callback called on success | Unit (mock previewManager) | `./gradlew test --tests "*RenderController*"` | ❌ Wave 0 |
| REND-05 | PDF callback NOT called on failure; errors routed to log | Unit (mock previewManager) | `./gradlew test --tests "*RenderController*"` | ❌ Wave 0 |
| REND-06 | < 5s performance | Integration (manual) | n/a — requires real XSLT + FOP | No — manual |

**Note on JavaFX test strategy:** `RenderController` pure-logic methods (error string formatting, null-guard logic) can be tested without a JavaFX stage using extracted static methods. The `Task<Preview>` lifecycle requires a running JavaFX toolkit — use `Platform.startup(() -> {})` pattern from `EditorTabTest.java` (VERIFIED: codebase read). However, Phase 6 logic is primarily tested via human verification at the end of the phase (same as Phase 3, Phase 4, Phase 5 all ended with a human verification plan).

### Wave 0 Gaps

- [ ] `src/test/java/ch/ti/gagi/xlseditor/ui/RenderControllerTest.java` — covers REND-02, REND-04, REND-05 via extracted logic methods
- [ ] `EditorController.saveAll()` method — must exist before any test can compile against it

*(Existing test infrastructure: JUnit Jupiter platform configured, `Platform.startup` pattern established in EditorTabTest — no new framework setup needed)*

---

## Security Domain

This phase is an internal developer tool with no authentication, no external backend, no user-provided data that crosses security boundaries. The render pipeline accepts `Project` and `Path` — both are already validated by `ProjectContext` (path-traversal guards exist in `setEntrypoint`, `setXmlInput`, `createFile`). No new security-sensitive surfaces are introduced in Phase 6.

Applicable ASVS categories: None for Phase 6.

---

## Sources

### Primary (HIGH confidence — direct codebase reads)
- `src/main/java/ch/ti/gagi/xlseditor/preview/PreviewManager.java` — API signature, contract
- `src/main/java/ch/ti/gagi/xlseditor/preview/Preview.java` — DTO fields: success(), outdated(), pdf(), errors()
- `src/main/java/ch/ti/gagi/xlseditor/preview/PreviewError.java` — DTO fields: message(), type(), file(), line()
- `src/main/java/ch/ti/gagi/xlseditor/render/RenderOrchestrator.java` — renderSafe() contract (never throws)
- `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` — statusLabel, logListView, showTransientStatus pattern, existing controller init pattern
- `src/main/java/ch/ti/gagi/xlseditor/ui/ProjectContext.java` — projectLoadedProperty(), getCurrentProject() API
- `src/main/java/ch/ti/gagi/xlseditor/ui/EditorController.java` — Task<StyleSpans> usage pattern (lines 201–215), saveTab() pattern, registry structure
- `src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml` — existing ToolBar-less structure, Run menu, fx:id names
- `src/main/java/ch/ti/gagi/xlseditor/log/LogManager.java` — in-memory log (not bound to UI in Phase 6)
- `src/main/java/ch/ti/gagi/xlseditor/log/LogEntry.java` — LogEntry structure
- `.planning/phases/06-render-pipeline-integration/06-CONTEXT.md` — all locked decisions D-01..D-17
- `src/test/java/ch/ti/gagi/xlseditor/ui/EditorTabTest.java` — Platform.startup test pattern

### Secondary (MEDIUM confidence)
- JavaFX 21 Task Javadoc (ASSUMED based on training, corroborated by existing Task usage patterns in EditorController.java)

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new dependencies; all APIs verified in codebase
- Architecture: HIGH — established patterns fully verified in existing controllers
- Pitfalls: HIGH — derived from direct inspection of existing code and patterns
- Integration points: HIGH — every fx:id and method reference verified by codebase read

**Research date:** 2026-04-19
**Valid until:** 2026-05-19 (stable JavaFX 21 + stable codebase conventions)
