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
     * Wave 0: skeleton body — Wave 1 replaces with full implementation.
     */
    public void handleRender() {
        // Wave 0 stub — implementation in Wave 1 (06-02-PLAN.md)
    }
}
