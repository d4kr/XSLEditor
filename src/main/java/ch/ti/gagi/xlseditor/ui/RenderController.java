package ch.ti.gagi.xlseditor.ui;

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

/**
 * Sub-controller for the render toolbar button and result routing.
 * Owns the JavaFX Task lifecycle, button state, progress feedback, and
 * result routing (PDF bytes → pdfCallback, errors → logListView).
 *
 * Lifecycle: MainController creates one instance as a field and calls
 * {@link #initialize} from its own {@code initialize()}. This controller is
 * NOT an FXML controller — MainController is the only @FXML controller.
 *
 * Phase 6 / REND-01..REND-05
 */
public final class RenderController {

    // --- State (D-04: sub-controller owns Task lifecycle, button state, progress, result routing) ---

    private Button renderButton;
    private ListView<String> logListView;
    private Consumer<String> statusSet;       // persistent: s -> statusLabel.setText(s) (D-11)
    private Consumer<String> statusTransient; // 3s auto-clear: this::showTransientStatus (D-12/D-13)
    private Consumer<byte[]> pdfCallback;     // Phase 7 stub: bytes -> {} (D-15)
    private Consumer<Boolean> outdatedCallback; // Phase 7 seam: b -> {} no-op in Phase 6 (REND-05)
    private ProjectContext projectContext;
    private EditorController editorController;
    private final PreviewManager previewManager = new PreviewManager(new RenderOrchestrator());

    // --- Public API ---

    /**
     * Wires this sub-controller to its host UI nodes and services.
     * Idempotent; safe to call once from MainController.initialize().
     *
     * @param renderButton      the toolbar/menu-bar render trigger button
     * @param logListView       the log panel list view; cleared and populated on each render
     * @param statusSet         persistent status updater (no auto-clear)
     * @param statusTransient   3-second auto-clearing status updater
     * @param pdfCallback       receives PDF bytes on success; Phase 7 wires actual display
     * @param outdatedCallback  receives outdated flag from Preview; Phase 7 seam
     * @param projectContext    source of current project and projectLoadedProperty
     * @param editorController  used to call saveAll() before spawning the render Task
     */
    public void initialize(
        Button renderButton,
        ListView<String> logListView,
        Consumer<String> statusSet,
        Consumer<String> statusTransient,
        Consumer<byte[]> pdfCallback,
        Consumer<Boolean> outdatedCallback,
        ProjectContext projectContext,
        EditorController editorController
    ) {
        this.renderButton     = Objects.requireNonNull(renderButton,     "renderButton");
        this.logListView      = Objects.requireNonNull(logListView,      "logListView");
        this.statusSet        = Objects.requireNonNull(statusSet,        "statusSet");
        this.statusTransient  = Objects.requireNonNull(statusTransient,  "statusTransient");
        this.pdfCallback      = Objects.requireNonNull(pdfCallback,      "pdfCallback");
        this.outdatedCallback = Objects.requireNonNull(outdatedCallback, "outdatedCallback");
        this.projectContext   = Objects.requireNonNull(projectContext,   "projectContext");
        this.editorController = Objects.requireNonNull(editorController, "editorController");

        // REND-02 base case: disable when no project loaded (D-07)
        // Runtime guard in handleRender() covers entryPoint/xmlInput null-checks.
        renderButton.disableProperty().bind(projectContext.projectLoadedProperty().not());
    }

    /**
     * Called from MainController.handleRender() (wired via onAction="#handleRender" in FXML).
     * Full implementation: REND-01..06 — Task<Preview> lifecycle with button state management,
     * progress feedback, success/failure routing, and PDF/outdated seams for Phase 7.
     */
    public void handleRender() {
        // REND-02: runtime guard — check entryPoint and xmlInput beyond the binding
        ch.ti.gagi.xlseditor.model.Project project = projectContext.getCurrentProject();
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

        // D-05: unbind first (bound property cannot be set), then disable button and update label
        renderButton.disableProperty().unbind();
        renderButton.setDisable(true);
        renderButton.setText("Rendering...");
        // D-11: persistent status during render (no PauseTransition — stays until success/failure)
        statusSet.accept("Rendering...");

        long startTime = System.currentTimeMillis();
        Path rootPath = project.rootPath();

        Task<Preview> task = new Task<>() {
            @Override
            protected Preview call() {
                // Runs on background thread — NEVER update UI nodes here (Pitfall 1)
                return previewManager.generatePreview(project, rootPath);
            }
        };

        task.setOnSucceeded(e -> {
            // On FX thread — safe for all UI updates; Platform.runLater NOT needed (Pattern 2)
            Preview result = task.getValue();
            long duration = System.currentTimeMillis() - startTime;
            renderButton.setDisable(false);
            renderButton.setText("Render");
            // Re-bind disable to project loaded state now that task is done
            renderButton.disableProperty().bind(projectContext.projectLoadedProperty().not());

            if (result.success()) {
                // D-14: log render duration as INFO entry
                logListView.getItems().add(
                    "[INFO] Render complete in " + String.format("%.1f", duration / 1000.0) + "s");
                // D-12: transient success status (auto-clears after 3s via showTransientStatus)
                statusTransient.accept(
                    "Render complete (" + String.format("%.1f", duration / 1000.0) + "s)");
                // D-15: pass PDF bytes to preview seam (Phase 7 fills this; Phase 6: no-op)
                pdfCallback.accept(result.pdf());
                // REND-05: notify that preview is NOT outdated (success -> outdated = false)
                outdatedCallback.accept(false);
            } else {
                // D-16: route errors to log panel as formatted strings
                for (PreviewError err : result.errors()) {
                    String entry = "[ERROR] " + err.type() + ": " + err.message();
                    if (err.file() != null) {
                        entry += " @ " + err.file();
                        if (err.line() != null) entry += ":" + err.line();
                    }
                    logListView.getItems().add(entry);
                }
                // D-13: transient failure status
                statusTransient.accept("Render failed");
                // REND-05: notify that preview IS outdated (failure -> outdated = true)
                outdatedCallback.accept(true);
            }
        });

        task.setOnFailed(e -> {
            // renderSafe() never throws — this branch covers unexpected runtime exceptions only
            renderButton.setDisable(false);
            renderButton.setText("Render");
            // Re-bind disable to project loaded state now that task is done
            renderButton.disableProperty().bind(projectContext.projectLoadedProperty().not());
            Throwable ex = task.getException();
            logListView.getItems().add(
                "[ERROR] Unexpected render error: " + (ex != null ? ex.getMessage() : "unknown"));
            statusTransient.accept("Render failed");
        });

        // CRITICAL: daemon thread prevents JVM hang on window close (anti-pattern in RESEARCH.md)
        Thread t = new Thread(task, "render-thread");
        t.setDaemon(true);
        t.start();
    }
}
