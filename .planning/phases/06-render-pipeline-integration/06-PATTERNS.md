# Phase 6: Render Pipeline Integration - Pattern Map

**Mapped:** 2026-04-19
**Files analyzed:** 5 (3 new, 2 modified)
**Analogs found:** 5 / 5

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `src/main/java/ch/ti/gagi/xlseditor/ui/RenderController.java` | controller | request-response + event-driven (Task) | `FileTreeController.java` | exact |
| `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` (modify) | controller | request-response | `MainController.java` lines 70-96 (initialize block) | self-referential (add wiring) |
| `src/main/java/ch/ti/gagi/xlseditor/ui/EditorController.java` (modify) | controller | file-I/O | `EditorController.java` lines 299-308 (saveTab) | self-referential (extract + promote) |
| `src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml` (modify) | config | — | `main.fxml` lines 16-35 (existing top zone) | self-referential (extend VBox) |
| `src/test/java/ch/ti/gagi/xlseditor/ui/RenderControllerTest.java` | test | — | `EditorTabTest.java` | role-match |

---

## Pattern Assignments

### `RenderController.java` (new — controller, request-response + Task)

**Primary analog:** `src/main/java/ch/ti/gagi/xlseditor/ui/FileTreeController.java`
**Secondary analog:** `src/main/java/ch/ti/gagi/xlseditor/ui/EditorController.java` (Task pattern)

**Imports pattern** — copy the structural imports from FileTreeController + add Task and preview types:

```java
// FileTreeController.java lines 1-30 — import block structure to follow:
package ch.ti.gagi.xlseditor.ui;

import ch.ti.gagi.xlseditor.model.Project;
import ch.ti.gagi.xlseditor.preview.Preview;
import ch.ti.gagi.xlseditor.preview.PreviewError;
import ch.ti.gagi.xlseditor.preview.PreviewManager;
import ch.ti.gagi.xlseditor.render.RenderOrchestrator;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
```

**Class declaration + field pattern** — mirrors FileTreeController.java lines 51-73:

```java
// FileTreeController.java lines 51-73:
public final class FileTreeController {

    // --- State ---

    private StackPane fileTreePane;
    private ProjectContext projectContext;
    private MenuItem menuItemSetEntrypoint;
    private MenuItem menuItemSetXmlInput;
    private Consumer<String> statusCallback;          // MainController.showTransientStatus
    private Supplier<Stage> primaryStageSupplier;     // deferred — primaryStage is set after initialize()

    private TreeView<FileItem> fileTree;
    // ...

// RenderController follows the same field layout:
public final class RenderController {

    // --- State ---

    private Button renderButton;
    private ListView<String> logListView;
    private Consumer<String> statusSet;        // persistent: s -> statusLabel.setText(s)
    private Consumer<String> statusTransient;  // auto-clearing: this::showTransientStatus
    private Consumer<byte[]> pdfCallback;      // Phase 7 stub: bytes -> {}
    private ProjectContext projectContext;
    private EditorController editorController;
    private final PreviewManager previewManager = new PreviewManager(new RenderOrchestrator());
```

**initialize() signature pattern** — mirrors FileTreeController.java lines 89-111:

```java
// FileTreeController.java lines 89-111:
public void initialize(
    StackPane fileTreePane,
    ProjectContext projectContext,
    MenuItem menuItemSetEntrypoint,
    MenuItem menuItemSetXmlInput,
    Consumer<String> statusCallback,
    Supplier<Stage> primaryStageSupplier
) {
    this.fileTreePane          = Objects.requireNonNull(fileTreePane);
    this.projectContext        = Objects.requireNonNull(projectContext);
    // ... assign all fields with Objects.requireNonNull
    buildTreeView();
    wireMenuActions();
    observeProjectLoaded();
    observeProjectFiles();
}

// RenderController.initialize() mirrors this shape:
public void initialize(
    Button renderButton,
    ListView<String> logListView,
    Consumer<String> statusSet,
    Consumer<String> statusTransient,
    Consumer<byte[]> pdfCallback,
    ProjectContext projectContext,
    EditorController editorController
) {
    this.renderButton    = Objects.requireNonNull(renderButton, "renderButton");
    this.logListView     = Objects.requireNonNull(logListView, "logListView");
    this.statusSet       = Objects.requireNonNull(statusSet, "statusSet");
    this.statusTransient = Objects.requireNonNull(statusTransient, "statusTransient");
    this.pdfCallback     = Objects.requireNonNull(pdfCallback, "pdfCallback");
    this.projectContext  = Objects.requireNonNull(projectContext, "projectContext");
    this.editorController = Objects.requireNonNull(editorController, "editorController");

    // Bind disable to projectLoadedProperty().not() (REND-02 base case)
    renderButton.disableProperty().bind(projectContext.projectLoadedProperty().not());
}
```

**Disable binding pattern** — from MainController.java line 73:

```java
// MainController.java line 73:
menuItemNewFile.disableProperty().bind(projectContext.projectLoadedProperty().not());

// RenderController reuses the exact same pattern:
renderButton.disableProperty().bind(projectContext.projectLoadedProperty().not());
// Runtime guard in handleRender() covers entryPoint/xmlInput null-checks.
```

**Alert pattern** — from MainController.java lines 185-193 and FileTreeController.java lines 227-235:

```java
// FileTreeController.java lines 227-235:
private void showError(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    Stage owner = primaryStageSupplier.get();
    if (owner != null) alert.initOwner(owner);
    alert.setTitle(title);
    alert.setHeaderText(title);
    alert.setContentText(content);
    alert.showAndWait();
}

// MainController.java lines 185-193 (WARNING variant):
Alert alert = new Alert(Alert.AlertType.WARNING);
alert.initOwner(primaryStage);
alert.setTitle("Open Project Failed");
alert.setHeaderText("Open Project Failed");
alert.setContentText("...");
alert.showAndWait();
```

**Task<T> background pattern** — from EditorController.java lines 201-215:

```java
// EditorController.java lines 201-215:
Task<StyleSpans<Collection<String>>> hlTask = new Task<>() {
    @Override protected StyleSpans<Collection<String>> call() {
        return XmlSyntaxHighlighter.computeHighlighting(snapshot);
    }
};
hlTask.setOnSucceeded(e -> {
    StyleSpans<Collection<String>> spans = hlTask.getValue();
    // UI update here — already on FX thread, no Platform.runLater needed
    if (editorTab.codeArea.getLength() > 0
            && spans.length() == editorTab.codeArea.getLength()) {
        editorTab.codeArea.setStyleSpans(0, spans);
    }
});
hlExecutor.submit(hlTask);

// RenderController uses the same Task shape with daemon Thread (not executor):
Task<Preview> task = new Task<>() {
    @Override
    protected Preview call() {
        return previewManager.generatePreview(project, rootPath);
    }
};
task.setOnSucceeded(e -> { /* FX thread — safe for UI updates */ });
task.setOnFailed(e -> { /* FX thread — safe for UI updates */ });
Thread t = new Thread(task, "render-thread");
t.setDaemon(true);   // CRITICAL: prevents JVM hang on window close
t.start();
```

**Status label pattern** — from MainController.java lines 289-305:

```java
// MainController.java lines 289-305:
private void showTransientStatus(String message) {
    if (statusPause != null) {
        statusPause.stop();   // CRITICAL: stop previous transition to prevent double-clear
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

// RenderController calls:
//   statusSet.accept("Rendering...")          — persistent, no PauseTransition (D-11)
//   statusTransient.accept("Render complete (X.Xs)")  — triggers existing PauseTransition (D-12)
//   statusTransient.accept("Render failed")           — triggers existing PauseTransition (D-13)
```

**Full handleRender() skeleton** — integrating all patterns above:

```java
public void handleRender() {
    // REND-02 runtime guard (entryPoint/xmlInput null-check beyond the binding)
    Project project = projectContext.getCurrentProject();
    if (project == null || project.entryPoint() == null || project.xmlInput() == null) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Cannot Render");
        alert.setContentText("Set both an entrypoint XSLT and an XML input file before rendering.");
        alert.showAndWait();
        return;
    }

    // D-17: clear log before new render
    logListView.getItems().clear();

    // D-08/D-09: save all dirty tabs; abort on disk error
    try {
        editorController.saveAll();
    } catch (IOException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Save Failed");
        alert.setContentText("Could not save all files before render: " + e.getMessage());
        alert.showAndWait();
        return;
    }

    // D-05: disable button and change label
    renderButton.setDisable(true);
    renderButton.setText("Rendering...");
    // D-11: persistent status during render (no PauseTransition)
    statusSet.accept("Rendering...");

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
            logListView.getItems().add(
                "[INFO] Render complete in " + String.format("%.1f", duration / 1000.0) + "s");
            statusTransient.accept(
                "Render complete (" + String.format("%.1f", duration / 1000.0) + "s)");
            pdfCallback.accept(result.pdf());   // Phase 7 fills this; Phase 6: no-op
        } else {
            for (PreviewError err : result.errors()) {
                String entry = "[ERROR] " + err.type() + ": " + err.message();
                if (err.file() != null) {
                    entry += " @ " + err.file();
                    if (err.line() != null) entry += ":" + err.line();
                }
                logListView.getItems().add(entry);
            }
            statusTransient.accept("Render failed");
        }
    });

    task.setOnFailed(e -> {
        // renderSafe() never throws — this covers unexpected runtime exceptions only
        renderButton.setDisable(false);
        renderButton.setText("Render");
        logListView.getItems().add(
            "[ERROR] Unexpected render error: " + task.getException().getMessage());
        statusTransient.accept("Render failed");
    });

    Thread t = new Thread(task, "render-thread");
    t.setDaemon(true);
    t.start();
}
```

---

### `EditorController.java` — add `saveAll()` (modify existing file)

**Analog:** `EditorController.java` lines 299-308 (existing `saveTab()`)

**Existing saveTab() pattern** (lines 299-308) — saveAll() extends this, but propagates IOException instead of swallowing it:

```java
// EditorController.java lines 299-308:
private void saveTab(EditorTab editorTab) {
    try {
        Files.writeString(editorTab.path, editorTab.codeArea.getText(), StandardCharsets.UTF_8);
        editorTab.codeArea.getUndoManager().mark();
        updateAppDirtyState();
    } catch (IOException ex) {
        showError("Save Failed",
            "Could not save file: " + editorTab.path.getFileName()
                + ". Check file permissions.");
    }
}

// New public saveAll() — different contract: throws IOException (D-09 / Pitfall 5)
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

**Registry iteration pattern** — EditorController.java lines 113-118:

```java
// EditorController.java lines 113-118:
private void updateAppDirtyState() {
    boolean anyDirty = registry.values().stream().anyMatch(tab -> {
        Object ud = tab.getUserData();
        return ud instanceof EditorTab et && et.dirty.get();
    });
    dirtyCallback.accept(anyDirty);
}
// saveAll() iterates registry.values() in the same way.
```

---

### `MainController.java` — add RenderController wiring (modify existing file)

**Analog:** `MainController.java` lines 70-96 (existing `initialize()`)

**Existing sub-controller wiring pattern** (lines 76-93) — RenderController follows the exact same call sequence:

```java
// MainController.java lines 76-93:
fileTreeController.initialize(
    fileTreePane,
    projectContext,
    menuItemSetEntrypoint,
    menuItemSetXmlInput,
    this::showTransientStatus,
    () -> primaryStage
);
editorController.initialize(
    editorPane,
    () -> primaryStage,
    this::setDirty
);

// New RenderController wiring appended to initialize():
renderController.initialize(
    renderButton,             // @FXML Button (new in FXML)
    logListView,              // existing @FXML ListView<String>
    s -> statusLabel.setText(s),     // persistent setter: Consumer<String>
    this::showTransientStatus,       // 3s auto-clear: Consumer<String>
    bytes -> { },            // Phase 6 no-op PDF callback; Phase 7 fills this
    projectContext,
    editorController
);
```

**New FXML field declarations to add** (pattern from MainController.java lines 37-57):

```java
// MainController.java lines 37-57 — @FXML injection block to extend:
@FXML private MenuBar menuBar;
@FXML private MenuItem menuItemOpenProject;
// ... existing fields ...

// Add for Phase 6:
@FXML private Button renderButton;        // in ToolBar (new FXML node)
@FXML private MenuItem menuItemRender;    // in Run menu (new FXML node)
```

**New controller field** (pattern from MainController.java line 65-66):

```java
// MainController.java lines 65-66:
private final FileTreeController fileTreeController = new FileTreeController();
private final EditorController editorController = new EditorController();  // Phase 4

// Add for Phase 6:
private final RenderController renderController = new RenderController();
```

**New FXML handler method** (pattern from MainController.java lines 143-148):

```java
// MainController.java lines 143-148:
@FXML
private void handleExit() {
    if (primaryStage != null) {
        primaryStage.fireEvent(new WindowEvent(primaryStage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }
}

// Add for Phase 6 (wires both ToolBar button AND Run menu item via onAction="#handleRender"):
@FXML
private void handleRender() {
    renderController.handleRender();
}
```

---

### `main.fxml` — add ToolBar + Run menu item (modify existing file)

**Analog:** `main.fxml` lines 16-35 (existing top zone + MenuBar)

**Current top zone** (lines 16-35) — wrap MenuBar in VBox and add ToolBar:

```xml
<!-- main.fxml lines 16-35 — CURRENT: -->
<top>
    <MenuBar fx:id="menuBar">
        <Menu text="File">
            ...
        </Menu>
        <Menu text="Edit"/>
        <Menu text="View"/>
        <Menu text="Run"/>
        <Menu text="Search">
            <MenuItem fx:id="findInFilesMenuItem" text="Find in Files"
                      accelerator="Ctrl+Shift+F" onAction="#handleFindInFiles"/>
        </Menu>
    </MenuBar>
</top>

<!-- main.fxml — MODIFIED: wrap MenuBar + add ToolBar in VBox -->
<top>
    <VBox>
        <MenuBar fx:id="menuBar">
            <Menu text="File">
                <!-- existing items unchanged -->
            </Menu>
            <Menu text="Edit"/>
            <Menu text="View"/>
            <Menu text="Run">
                <MenuItem fx:id="menuItemRender"
                          text="Render"
                          accelerator="F5"
                          onAction="#handleRender"/>
            </Menu>
            <Menu text="Search">
                <MenuItem fx:id="findInFilesMenuItem" text="Find in Files"
                          accelerator="Ctrl+Shift+F" onAction="#handleFindInFiles"/>
            </Menu>
        </MenuBar>
        <ToolBar>
            <Button fx:id="renderButton" text="Render" onAction="#handleRender"/>
        </ToolBar>
    </VBox>
</top>
```

**Import note:** Both `ToolBar` (via `javafx.scene.control.*`) and `VBox` (via `javafx.scene.layout.*`) are already covered by wildcard imports on lines 3-5 of main.fxml — no new `<?import?>` directives needed.

---

### `RenderControllerTest.java` (new — test)

**Analog:** `src/test/java/ch/ti/gagi/xlseditor/ui/EditorTabTest.java`

**Test class skeleton pattern** (EditorTabTest.java lines 1-20):

```java
// EditorTabTest.java lines 1-20:
package ch.ti.gagi.xlseditor.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EditorTabTest {

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // Toolkit was already initialised by a previous test class — OK.
        }
    }

    @Test
    void newTabIsNotDirtyAfterLoad() { ... }
}

// RenderControllerTest follows the exact same structure:
package ch.ti.gagi.xlseditor.ui;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderControllerTest {

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // Toolkit was already initialised by a previous test class — OK.
        }
    }

    // Unit tests for pure-logic methods:
    // - formatError() with null file/line (REND-05)
    // - formatError() with file and line present (REND-05)
    // - handleRender() null guard for project/entryPoint/xmlInput (REND-02)
}
```

---

## Shared Patterns

### Sub-controller lifecycle (apply to RenderController)

**Source:** `FileTreeController.java` lines 79-111 and `EditorController.java` lines 74-88

Pattern: constructor creates instance with no args; `initialize(...)` is called once from `MainController.initialize()`; all fields are `Objects.requireNonNull`-guarded; the class is `final`.

```java
// Both sub-controllers open with:
public final class [Name]Controller {

    // --- State ---
    // (fields, no @FXML — MainController is the sole @FXML controller)

    // --- Public API ---

    public void initialize(...) {
        this.field = Objects.requireNonNull(arg, "arg");
        // ... more assignments ...
        // then setup calls
    }
```

### Alert error display (apply to RenderController)

**Source:** `FileTreeController.java` lines 227-235 and `MainController.java` lines 185-193

```java
// FileTreeController.java lines 227-235:
private void showError(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    Stage owner = primaryStageSupplier.get();
    if (owner != null) alert.initOwner(owner);
    alert.setTitle(title);
    alert.setHeaderText(title);
    alert.setContentText(content);
    alert.showAndWait();
}
```

RenderController can inline this pattern since it has no Stage supplier; `alert.showAndWait()` without `initOwner` is acceptable for a developer-only tool.

### Task threading rules (apply to RenderController Task body)

**Source:** `EditorController.java` lines 201-215

- `Task.call()` runs on background thread — NEVER update UI nodes here
- `setOnSucceeded` / `setOnFailed` run on FX thread — safe for ALL UI updates
- `Platform.runLater` is NOT needed inside `setOnSucceeded` / `setOnFailed`
- Always set `t.setDaemon(true)` before `t.start()`

### Consumer<String> callback for cross-controller status (apply to RenderController)

**Source:** `FileTreeController.java` line 59 + `MainController.java` line 81

```java
// FileTreeController.java line 59:
private Consumer<String> statusCallback;   // MainController.showTransientStatus

// MainController.java line 81:
this::showTransientStatus,

// RenderController extends this to TWO callbacks:
private Consumer<String> statusSet;        // s -> statusLabel.setText(s) — persistent
private Consumer<String> statusTransient;  // this::showTransientStatus — 3s auto-clear
```

---

## No Analog Found

All Phase 6 files have close analogs in the codebase. No files require falling back to RESEARCH.md patterns.

---

## Metadata

**Analog search scope:** `src/main/java/ch/ti/gagi/xlseditor/ui/`, `src/test/java/ch/ti/gagi/xlseditor/ui/`, `src/main/java/ch/ti/gagi/xlseditor/preview/`, `src/main/resources/ch/ti/gagi/xlseditor/ui/`
**Files read:** MainController.java, EditorController.java, FileTreeController.java, main.fxml, EditorTabTest.java, PreviewManager.java, Preview.java, PreviewError.java
**Pattern extraction date:** 2026-04-19
