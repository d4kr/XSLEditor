package ch.ti.gagi.xlseditor.ui;

import ch.ti.gagi.xlseditor.XLSEditorApp;
import ch.ti.gagi.xlseditor.model.Project;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Optional;

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

    // Phase 2 additions
    @FXML private MenuItem menuItemOpenProject;
    @FXML private MenuItem menuItemSetEntrypoint;
    @FXML private MenuItem menuItemSetXmlInput;
    @FXML private MenuItem menuItemNewFile;
    @FXML private Label statusLabel;

    // --- State ---

    private Stage primaryStage;
    private boolean dirty = false;
    private final ProjectContext projectContext = new ProjectContext();
    private PauseTransition statusPause;
    private final FileTreeController fileTreeController = new FileTreeController();

    // --- Lifecycle ---

    @FXML
    public void initialize() {
        // D-10: New File enabled only when a project is loaded
        menuItemNewFile.disableProperty().bind(projectContext.projectLoadedProperty().not());
        // D-04 (Phase 3): compound selection-and-loaded binding delegated to FileTreeController.
        // Also wires tree population, cell factory, and Set Entrypoint / Set XML Input handlers.
        fileTreeController.initialize(
            fileTreePane,
            projectContext,
            menuItemSetEntrypoint,
            menuItemSetXmlInput,
            this::showTransientStatus,
            () -> primaryStage
        );
        // statusLabel starts empty — handleOpenProject populates it transiently.
        statusLabel.setText("");
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

    @FXML
    private void handleOpenProject() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open Project");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File selected = chooser.showDialog(primaryStage);
        if (selected == null) {
            return; // Cancelled — no action, no error (UI-SPEC)
        }

        try {
            Project project = projectContext.openProject(selected.toPath());
            String projectName = project.rootPath().getFileName().toString();
            updateTitle(projectName); // D-07

            // Transient success feedback — 3-second auto-hide (UI-SPEC)
            showTransientStatus("Project opened: " + projectName);

            // Log panel entry (UI-SPEC § Copywriting Contract)
            if (project.entryPoint() != null && project.xmlInput() != null) {
                logListView.getItems().add(
                    "Loaded entrypoint: " + project.entryPoint().getFileName()
                        + " \u00b7 XML input: " + project.xmlInput().getFileName());
            } else if (project.entryPoint() == null && project.xmlInput() == null) {
                logListView.getItems().add(
                    "No .xslfo-tool.json found \u2014 entrypoint and XML input not set");
            } else {
                // Partial config — one field set, the other not
                logListView.getItems().add(
                    "Loaded partial config: entryPoint="
                        + (project.entryPoint() != null ? project.entryPoint().getFileName() : "(unset)")
                        + " \u00b7 xmlInput="
                        + (project.xmlInput() != null ? project.xmlInput().getFileName() : "(unset)"));
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(primaryStage);
            alert.setTitle("Open Project Failed");
            alert.setHeaderText("Open Project Failed");
            alert.setContentText("Could not open project: " + selected.getAbsolutePath()
                + ". Check that the directory is readable.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleNewFile() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(primaryStage);
        dialog.setTitle("New File");
        dialog.setHeaderText("Create a new file in the project root");
        dialog.setContentText("File name:");
        dialog.getEditor().setPromptText("e.g. output.xsl");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                // ProjectContext.createFile enforces path-traversal guard (T-02-01 inherited mitigation).
                // It also throws IllegalArgumentException on blank/invalid names.
                projectContext.createFile(name);
                // D-09: no auto-open; Phase 3 tree refresh will surface the new file.
            } catch (IllegalArgumentException e) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.initOwner(primaryStage);
                err.setTitle("Invalid File Name");
                err.setHeaderText("Invalid File Name");
                err.setContentText(e.getMessage());
                err.showAndWait();
            } catch (FileAlreadyExistsException e) {
                Alert warn = new Alert(Alert.AlertType.WARNING);
                warn.initOwner(primaryStage);
                warn.setTitle("File Already Exists");
                warn.setHeaderText("File Already Exists");
                warn.setContentText("A file named \"" + name
                    + "\" already exists in the project root. Choose a different name.");
                warn.showAndWait();
            } catch (IOException e) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.initOwner(primaryStage);
                err.setTitle("Create File Failed");
                err.setHeaderText("Create File Failed");
                err.setContentText("Could not create file: " + e.getMessage());
                err.showAndWait();
            }
        });
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

    /**
     * Shows a transient success message in the statusLabel for 3 seconds, then clears it.
     * Uses PauseTransition on the JavaFX animation timer — no thread management needed.
     */
    private void showTransientStatus(String message) {
        // Stop any previous transition still running to prevent double-clear races
        if (statusPause != null) {
            statusPause.stop();
        }
        statusLabel.setText(message);
        // Ensure the success style is applied exactly once (avoid duplicates across calls)
        statusLabel.getStyleClass().removeAll("status-label-success");
        statusLabel.getStyleClass().add("status-label-success");

        statusPause = new PauseTransition(Duration.seconds(3));
        statusPause.setOnFinished(e -> {
            statusLabel.setText("");
            statusLabel.getStyleClass().removeAll("status-label-success");
        });
        statusPause.play();
    }
}
