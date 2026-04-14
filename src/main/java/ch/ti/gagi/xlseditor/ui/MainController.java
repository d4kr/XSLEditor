package ch.ti.gagi.xlseditor.ui;

import ch.ti.gagi.xlseditor.XLSEditorApp;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Controller for main.fxml.
 *
 * Phase 1 responsibilities:
 *  - Wire close-request confirmation dialog (D-09)
 *  - Expose updateTitle() for use by project-loading phases (D-08)
 *  - Expose setDirty() so Phase 4 can flip the dirty flag (D-09 scaffold)
 *
 * Integration points for downstream phases:
 *  - fileTreePane  (fx:id) → Phase 3 replaces Label with TreeView
 *  - editorPane    (fx:id) → Phases 4-5 replace Label with TabPane/CodeArea
 *  - previewPane   (fx:id) → Phase 7 drives previewWebView
 *  - logListView   (fx:id) → Phase 8 binds to LogManager observable list
 */
public class MainController {

    // --- FXML injections ---

    @FXML private MenuBar menuBar;
    @FXML private SplitPane outerSplitPane;
    @FXML private SplitPane innerSplitPane;
    @FXML private StackPane fileTreePane;
    @FXML private StackPane editorPane;
    @FXML private StackPane previewPane;
    @FXML private WebView previewWebView;
    @FXML private Label previewPlaceholderLabel;
    @FXML private TitledPane logPane;
    @FXML private ListView<String> logListView;

    // --- State ---

    private Stage primaryStage;
    private boolean dirty = false;

    // --- Lifecycle ---

    @FXML
    public void initialize() {
        // Nothing to wire in Phase 1; downstream phases call into this controller.
    }

    /**
     * Called by XLSEditorApp.start() immediately after FXML load.
     * Registers the close-request handler (D-09).
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        stage.setOnCloseRequest(this::handleCloseRequest);
    }

    // --- Public API for other phases ---

    /**
     * Updates the window title. (D-08)
     *
     * @param projectName the loaded project name, or null to reset to "XLSEditor"
     */
    public void updateTitle(String projectName) {
        if (primaryStage == null) return;
        if (projectName == null || projectName.isBlank()) {
            primaryStage.setTitle(XLSEditorApp.APP_NAME);
        } else {
            primaryStage.setTitle(XLSEditorApp.APP_NAME + " \u2014 " + projectName);
        }
    }

    /**
     * Marks the application dirty (unsaved changes exist). (D-09 scaffold)
     * Phase 4 calls setDirty(true) when an editor tab is modified.
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return dirty;
    }

    // --- FXML action handlers ---

    @FXML
    private void handleExit() {
        if (primaryStage != null) {
            primaryStage.fireEvent(new WindowEvent(primaryStage, WindowEvent.WINDOW_CLOSE_REQUEST));
        }
    }

    // --- Private helpers ---

    /**
     * Close-request handler. Shows confirmation dialog when dirty. (D-09)
     * Consume the event to cancel the close; otherwise the window closes normally.
     */
    private void handleCloseRequest(WindowEvent event) {
        if (!dirty) {
            return; // No unsaved changes — allow close
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(primaryStage);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes.");
        alert.setContentText("Close anyway and discard all changes?");
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        alert.showAndWait().ifPresentOrElse(
            button -> {
                if (button != ButtonType.OK) {
                    event.consume(); // Cancel the close
                }
                // OK: allow close (do nothing — event proceeds)
            },
            () -> event.consume() // Dialog closed without selection — cancel close
        );
    }
}
