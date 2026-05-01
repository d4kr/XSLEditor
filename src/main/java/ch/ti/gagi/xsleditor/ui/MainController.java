package ch.ti.gagi.xsleditor.ui;

import ch.ti.gagi.xsleditor.XSLEditorApp;
import ch.ti.gagi.xsleditor.log.LogEntry;
import ch.ti.gagi.xsleditor.ui.AboutDialog;
import ch.ti.gagi.xsleditor.model.Project;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
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
import org.fxmisc.richtext.CodeArea;

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
 *  - logTableView  (fx:id) → Phase 8 binds via LogController (ERR-01..05)
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
    @FXML private Label outdatedBannerLabel;         // Phase 7
    @FXML private TitledPane logPane;
    @FXML private TableView<LogEntry> logTableView;
    @FXML private TableColumn<LogEntry, String> colTime;
    @FXML private TableColumn<LogEntry, String> colLevel;
    @FXML private TableColumn<LogEntry, String> colType;
    @FXML private TableColumn<LogEntry, String> colMessage;
    @FXML private TableColumn<LogEntry, Void> colAi;
    @FXML private ToggleButton filterAllButton;
    @FXML private ToggleButton filterErrorButton;
    @FXML private ToggleButton filterWarnButton;
    @FXML private ToggleButton filterInfoButton;

    // Phase 2 additions
    @FXML private MenuItem menuItemOpenProject;
    @FXML private MenuItem menuItemSetEntrypoint;
    @FXML private MenuItem menuItemSetXmlInput;
    @FXML private MenuItem menuItemNewFile;
    @FXML private Label statusLabel;

    // Phase 5 additions
    @FXML private MenuItem findInFilesMenuItem;

    // Phase 6 additions
    @FXML private Button renderButton;
    @FXML private MenuItem menuItemRender;

    // Phase 26 additions
    @FXML private MenuItem undoMenuItem;
    @FXML private MenuItem redoMenuItem;
    @FXML private Button undoButton;
    @FXML private Button redoButton;

    // Phase 27 additions
    @FXML private Button saveButton;

    // --- State ---

    private Stage primaryStage;
    private boolean dirty = false;
    private final ProjectContext projectContext = new ProjectContext();
    private PauseTransition statusPause;
    private final FileTreeController fileTreeController = new FileTreeController();
    private final EditorController editorController = new EditorController();  // Phase 4
    private final RenderController renderController = new RenderController();  // Phase 6
    private final PreviewController previewController = new PreviewController();  // Phase 7
    private final LogController logController = new LogController();  // Phase 8

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
        // Phase 4 — EditorController setup (EDIT-01..03, EDIT-09)
        editorController.initialize(
            editorPane,
            () -> primaryStage,
            this::setDirty
        );
        // Phase 26/27 — toolbar button bindings rebind on tab switch (EDIT-14, EDIT-15, TOOL-01..03)
        editorController.setOnActiveTabChanged(this::rebindToolbarButtons);
        // Phase 26/27 / WR-04 — immediately unbind toolbar controls when a tab closes so controls
        // are disabled before the selection-change event propagates (avoids stale UndoManager window).
        editorController.setOnTabClosed(() -> rebindToolbarButtons(Optional.empty()));
        // Wire Phase 3 integration seam (FileTreeController.java line 117, D-05)
        fileTreeController.setOnFileOpenRequest(editorController::openOrFocusTab);
        // Phase 5 — Find in Files (EDIT-08)
        findInFilesMenuItem.setOnAction(e -> handleFindInFiles());
        // statusLabel starts empty — handleOpenProject populates it transiently.
        statusLabel.setText("");
        // Phase 7 — PreviewController setup (PREV-01..PREV-04)
        previewController.initialize(
            previewPane,
            previewWebView,
            previewPlaceholderLabel,
            outdatedBannerLabel
        );
        // Phase 8 — LogController setup (ERR-01..ERR-05)
        logController.initialize(
            logPane,
            logTableView,
            colTime, colLevel, colType, colMessage,
            colAi,
            filterAllButton, filterErrorButton, filterWarnButton, filterInfoButton,
            editorController
        );
        // Phase 6 — RenderController setup (REND-01..06)
        renderController.initialize(
            renderButton,
            logController::setErrors,     // D-06: Consumer<List<PreviewError>> callback
            logController::addInfo,       // D-08: INFO messages
            s -> statusLabel.setText(s),  // D-11: persistent setter (no PauseTransition)
            this::showTransientStatus,    // D-12/D-13: 3s auto-clear
            previewController::displayPdf,
            previewController::setOutdated,
            projectContext,
            editorController
        );
    }

    /**
     * Called by XSLEditorApp.start() immediately after FXML load.
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
     * @param projectName the loaded project name, or null to reset to "XSLEditor"
     */
    public void updateTitle(String projectName) {
        if (primaryStage == null) {
            // Safe guard: setPrimaryStage() is always called before any user-triggered
            // code path reaches updateTitle(). If this guard is ever hit it means
            // a call-ordering bug was introduced — consider throwing IllegalStateException
            // here during development to make such violations visible immediately.
            return;
        }
        if (projectName == null || projectName.isBlank()) {
            primaryStage.setTitle(XSLEditorApp.APP_NAME);
        } else {
            primaryStage.setTitle(XSLEditorApp.APP_NAME + " \u2014 " + projectName);
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
                logController.addInfo(
                    "Loaded entrypoint: " + project.entryPoint().getFileName()
                        + " \u00b7 XML input: " + project.xmlInput().getFileName());
            } else if (project.entryPoint() == null && project.xmlInput() == null) {
                logController.addInfo(
                    "No .xslfo-tool.json found \u2014 entrypoint and XML input not set");
            } else {
                // Partial config — one field set, the other not
                logController.addInfo(
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

    // --- Phase 6 action handler ---

    @FXML
    private void handleRender() {
        renderController.handleRender();
    }

    // --- Phase 5 action handlers ---

    @FXML
    private void handleFindInFiles() {
        if (!projectContext.projectLoadedProperty().get()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Project");
            alert.setHeaderText("No project open");
            alert.setContentText("Open a project first to use Find in Files.");
            if (primaryStage != null) alert.initOwner(primaryStage);
            alert.showAndWait();
            return;
        }
        java.nio.file.Path root = projectContext.getCurrentProject().rootPath();
        SearchDialog dialog = new SearchDialog(primaryStage, root, editorController);
        dialog.show(); // non-blocking — user can keep editing
    }

    // --- Phase 11 action handler ---

    @FXML
    private void handleAbout() {
        AboutDialog dialog = new AboutDialog(primaryStage, XSLEditorApp.hostServices());
        dialog.showAndWait();
    }

    // --- Phase 25 action handlers (Edit menu clipboard commands) ---

    @FXML
    private void handleEditCut() {
        editorController.getActiveCodeArea().ifPresent(CodeArea::cut);
    }

    @FXML
    private void handleEditCopy() {
        editorController.getActiveCodeArea().ifPresent(CodeArea::copy);
    }

    @FXML
    private void handleEditPaste() {
        editorController.getActiveCodeArea().ifPresent(CodeArea::paste);
    }

    @FXML
    private void handleEditSelectAll() {
        editorController.getActiveCodeArea().ifPresent(CodeArea::selectAll);
    }

    // --- Phase 26 action handlers (Undo/Redo) ---

    @FXML
    private void handleEditUndo()    { performUndo(); }

    @FXML
    private void handleEditRedo()    { performRedo(); }

    @FXML
    private void handleToolbarUndo() { performUndo(); }

    @FXML
    private void handleToolbarRedo() { performRedo(); }

    @FXML
    private void handleToolbarSave() { editorController.saveActiveTab(); }

    private void performUndo() { editorController.getActiveCodeArea().ifPresent(CodeArea::undo); }
    private void performRedo() { editorController.getActiveCodeArea().ifPresent(CodeArea::redo); }

    /**
     * Rebinds the disable properties of the toolbar/menu controls
     * (Edit menu Undo/Redo items, toolbar ↺ ↻ 💾 buttons) to the
     * UndoManager / dirty property of the newly active tab.
     * Called from EditorController whenever the selected tab changes.
     *
     * <p>When no tab is open, all controls are disabled. When a tab
     * is open, undo/redo controls disable iff their UndoManager
     * availability property is false, and the saveButton disables
     * iff the active EditorTab's dirty binding is false.</p>
     *
     * <p>Unbinds any previous bindings before binding new ones to
     * avoid leaking listeners on closed tabs.</p>
     */
    private void rebindToolbarButtons(Optional<CodeArea> activeCodeArea) {
        // Unbind first — safe whether or not a binding currently exists.
        undoMenuItem.disableProperty().unbind();
        redoMenuItem.disableProperty().unbind();
        undoButton.disableProperty().unbind();
        redoButton.disableProperty().unbind();
        saveButton.disableProperty().unbind();

        if (activeCodeArea.isEmpty()) {
            // No tab open — all toolbar controls disabled (Phase 27 D-07 null-tab path).
            undoMenuItem.setDisable(true);
            redoMenuItem.setDisable(true);
            undoButton.setDisable(true);
            redoButton.setDisable(true);
            saveButton.setDisable(true);
            return;
        }

        CodeArea ca = activeCodeArea.get();
        // disable == NOT available.
        // UndoManager.undoAvailableProperty() returns Val<Boolean> (ReactFX), not ObservableBooleanValue,
        // so we use Bindings.createBooleanBinding with the Val as invalidation source.
        undoMenuItem.disableProperty().bind(
            Bindings.createBooleanBinding(
                () -> !ca.getUndoManager().isUndoAvailable(),
                ca.getUndoManager().undoAvailableProperty()));
        redoMenuItem.disableProperty().bind(
            Bindings.createBooleanBinding(
                () -> !ca.getUndoManager().isRedoAvailable(),
                ca.getUndoManager().redoAvailableProperty()));
        undoButton.disableProperty().bind(
            Bindings.createBooleanBinding(
                () -> !ca.getUndoManager().isUndoAvailable(),
                ca.getUndoManager().undoAvailableProperty()));
        redoButton.disableProperty().bind(
            Bindings.createBooleanBinding(
                () -> !ca.getUndoManager().isRedoAvailable(),
                ca.getUndoManager().redoAvailableProperty()));

        // Phase 27 D-07: saveButton.disable == !activeTab.dirty
        editorController.getActiveDirtyProperty().ifPresentOrElse(
            dirtyProp -> saveButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                    () -> !Boolean.TRUE.equals(dirtyProp.getValue()),
                    dirtyProp)),
            () -> saveButton.setDisable(true)
        );
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
