package ch.ti.gagi.xlseditor.ui;

import ch.ti.gagi.xlseditor.model.Project;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Sub-controller for the file tree pane. Owns the TreeView&lt;FileItem&gt;, the
 * cell factory (FileItemTreeCell), menu-item enable/disable bindings for
 * Set Entrypoint / Set XML Input, and the handlers that delegate to
 * ProjectContext.setEntrypoint / setXmlInput with .xslfo-tool.json write-back
 * and transient status feedback.
 *
 * Lifecycle: MainController creates one instance as a field and calls
 * {@link #initialize} from its own {@code initialize()}. This controller is
 * NOT an FXML controller — MainController is the only @FXML controller.
 *
 * Reactive bindings (D-06, D-07):
 *  - projectContext.projectFilesProperty() -&gt; rebuilds TreeView on list change
 *    (handles Phase 2 createFile appends and Phase 3 openProject refresh)
 *  - projectContext.projectLoadedProperty() -&gt; mounts the VBox+TreeView on true,
 *    restores the Phase 1 placeholder on false (defensive — never flips to false
 *    in current code path)
 *  - tree selection AND projectLoaded -&gt; compound disable binding for menu items
 *
 * Phase 3 / TREE-01..04, PROJ-02, PROJ-03
 */
public final class FileTreeController {

    // --- State ---

    private StackPane fileTreePane;
    private ProjectContext projectContext;
    private MenuItem menuItemSetEntrypoint;
    private MenuItem menuItemSetXmlInput;
    private Consumer<String> statusCallback;          // MainController.showTransientStatus
    private Supplier<Stage> primaryStageSupplier;     // deferred — primaryStage is set after initialize()

    private TreeView<FileItem> fileTree;              // created lazily on first project load
    private VBox treeContainer;                       // VBox(headerLabel, fileTree)
    private List<Node> originalPaneChildren;          // snapshot for restore on unload

    // D-05: Phase 4 replaces this no-op with the actual tab-open handler via setOnFileOpenRequest
    private Consumer<Path> onFileOpenRequest = path -> { /* no-op default (null-safe) */ };

    // --- Public API ---

    /**
     * Wires this sub-controller to its host stack pane and services.
     * Idempotent; safe to call once from MainController.initialize().
     *
     * @param fileTreePane             the StackPane that will hold the tree UI
     * @param projectContext           state service owning projectLoaded + projectFiles
     * @param menuItemSetEntrypoint    menu item whose disable state and onAction this controller manages
     * @param menuItemSetXmlInput      sibling menu item
     * @param statusCallback           called with a message for 3-second transient status (e.g. this::showTransientStatus)
     * @param primaryStageSupplier     supplier for the primary Stage, used only for Alert.initOwner;
     *                                 deferred because MainController.primaryStage is populated AFTER initialize runs
     */
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
        this.menuItemSetEntrypoint = Objects.requireNonNull(menuItemSetEntrypoint);
        this.menuItemSetXmlInput   = Objects.requireNonNull(menuItemSetXmlInput);
        this.statusCallback        = Objects.requireNonNull(statusCallback);
        this.primaryStageSupplier  = Objects.requireNonNull(primaryStageSupplier);

        // Snapshot Phase 1 placeholder children so we can restore on unload (defensive)
        this.originalPaneChildren = new ArrayList<>(fileTreePane.getChildren());

        buildTreeView();
        wireMenuActions();
        observeProjectLoaded();
        observeProjectFiles();
    }

    /**
     * Phase 4 integration seam. Phase 4 will pass a handler that opens the
     * file in a new tab. Phase 3 leaves the default no-op (D-05).
     */
    public void setOnFileOpenRequest(Consumer<Path> callback) {
        this.onFileOpenRequest = (callback != null) ? callback : path -> { };
    }

    // --- Construction ---

    private void buildTreeView() {
        fileTree = new TreeView<>();
        fileTree.getStyleClass().add("file-tree-view");
        fileTree.setShowRoot(false);                                   // D-12
        fileTree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        fileTree.setCellFactory(tv -> new FileItemTreeCell());         // Plan 02 cell

        // Double-click -> onFileOpenRequest (D-05, TREE-04)
        fileTree.setOnMouseClicked(evt -> {
            if (evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2) {
                openSelected();
            }
        });
        // Enter key -> onFileOpenRequest (UI-SPEC § Interaction Contracts)
        fileTree.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ENTER) {
                openSelected();
            }
        });

        // Header label (D-11, UI-SPEC § Panel Header)
        Label headerLabel = new Label();
        headerLabel.getStyleClass().add("file-tree-header");
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        headerLabel.setAlignment(Pos.CENTER_LEFT);

        treeContainer = new VBox();
        treeContainer.getChildren().addAll(headerLabel, fileTree);
        VBox.setVgrow(fileTree, javafx.scene.layout.Priority.ALWAYS);
    }

    private void openSelected() {
        TreeItem<FileItem> selected = fileTree.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;
        // Resolve against rootPath so Phase 4 gets an absolute path (FileItem stores relative)
        Project project = projectContext.getCurrentProject();
        if (project == null) return;
        Path absolute = project.rootPath().resolve(selected.getValue().path());
        onFileOpenRequest.accept(absolute);
    }

    // --- Menu wiring ---

    private void wireMenuActions() {
        // D-04 compound disable: no project loaded OR no tree selection -> disabled
        menuItemSetEntrypoint.disableProperty().bind(
            projectContext.projectLoadedProperty().not()
                .or(fileTree.getSelectionModel().selectedItemProperty().isNull()));
        menuItemSetXmlInput.disableProperty().bind(
            projectContext.projectLoadedProperty().not()
                .or(fileTree.getSelectionModel().selectedItemProperty().isNull()));

        menuItemSetEntrypoint.setOnAction(e -> handleSetEntrypoint());
        menuItemSetXmlInput.setOnAction(e -> handleSetXmlInput());
    }

    private void handleSetEntrypoint() {
        TreeItem<FileItem> selected = fileTree.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;
        Path relativePath = selected.getValue().path();
        try {
            projectContext.setEntrypoint(relativePath);
            rebuildTree();                                             // re-derive roles
            statusCallback.accept("Entrypoint set: " + relativePath.getFileName());
        } catch (IOException ex) {
            showError("Set Entrypoint Failed",
                "Could not set entrypoint: " + relativePath.getFileName()
                    + ". Check directory permissions.");
        } catch (IllegalArgumentException ex) {
            // Should not occur — paths come from projectFiles which are all valid
            // relatives — but surface defensively.
            showError("Set Entrypoint Failed",
                "Invalid file path for entrypoint: " + ex.getMessage());
        }
    }

    private void handleSetXmlInput() {
        TreeItem<FileItem> selected = fileTree.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;
        Path relativePath = selected.getValue().path();
        try {
            projectContext.setXmlInput(relativePath);
            rebuildTree();
            statusCallback.accept("XML input set: " + relativePath.getFileName());
        } catch (IOException ex) {
            showError("Set XML Input Failed",
                "Could not set XML input: " + relativePath.getFileName()
                    + ". Check directory permissions.");
        } catch (IllegalArgumentException ex) {
            showError("Set XML Input Failed",
                "Invalid file path for XML input: " + ex.getMessage());
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        Stage owner = primaryStageSupplier.get();
        if (owner != null) alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- Reactive bindings ---

    private void observeProjectLoaded() {
        ChangeListener<Boolean> listener = (obs, wasLoaded, isLoaded) -> {
            if (isLoaded) {
                mountTree();
            } else {
                unmountTree();
            }
        };
        projectContext.projectLoadedProperty().addListener(listener);
        // Seed state in case projectContext is already loaded (defensive — not the current code path)
        if (projectContext.isProjectLoaded()) {
            mountTree();
        }
    }

    private void observeProjectFiles() {
        // D-06/D-07: rebuild tree whenever the file list changes (createFile append, openProject refresh)
        projectContext.projectFilesProperty().addListener(
            (ListChangeListener<Path>) change -> {
                if (projectContext.isProjectLoaded()) rebuildTree();
            });
    }

    private void mountTree() {
        Project project = projectContext.getCurrentProject();
        if (project == null) return;
        // Update header label text (first child of VBox, added in buildTreeView)
        Label header = (Label) treeContainer.getChildren().get(0);
        header.setText(project.rootPath().getFileName() + "/");

        // Replace the Phase 1 placeholder. Preserve the Phase 2 statusLabel so
        // 3-second status messages still appear on top of the tree (StackPane layering).
        Node statusLabel = findStatusLabel();
        fileTreePane.getChildren().clear();
        fileTreePane.getChildren().add(treeContainer);
        if (statusLabel != null) {
            fileTreePane.getChildren().add(statusLabel);
            StackPane.setAlignment(statusLabel, Pos.BOTTOM_CENTER);
        }
        rebuildTree();
    }

    private void unmountTree() {
        // Defensive path: restore Phase 1 children. Current code flow never calls this
        // (projectLoaded only transitions false -> true in Phase 3).
        fileTreePane.getChildren().setAll(originalPaneChildren);
    }

    private Node findStatusLabel() {
        for (Node n : originalPaneChildren) {
            if (n instanceof Label l && "statusLabel".equals(l.getId())) return l;
        }
        return null;
    }

    // --- Tree population ---

    private void rebuildTree() {
        Project project = projectContext.getCurrentProject();
        if (project == null) return;

        // Capture current selection for restoration
        TreeItem<FileItem> prevSelected = fileTree.getSelectionModel().getSelectedItem();
        Path prevSelectedPath = (prevSelected != null && prevSelected.getValue() != null)
            ? prevSelected.getValue().path()
            : null;

        TreeItem<FileItem> root = new TreeItem<>(
            new FileItem(project.rootPath(), FileItem.FileRole.REGULAR));

        TreeItem<FileItem> itemToSelect = null;
        for (Path relative : projectContext.projectFilesProperty()) {
            FileItem.FileRole role = deriveRole(relative, project);
            TreeItem<FileItem> item = new TreeItem<>(new FileItem(relative, role));
            root.getChildren().add(item);
            if (relative.equals(prevSelectedPath)) itemToSelect = item;
        }
        fileTree.setRoot(root);

        // Restore selection — important so the compound disable binding stays in the
        // "enabled" state after a Set Entrypoint rebuild.
        if (itemToSelect != null) {
            fileTree.getSelectionModel().select(itemToSelect);
        }
    }

    private FileItem.FileRole deriveRole(Path relative, Project project) {
        if (relative.equals(project.entryPoint())) return FileItem.FileRole.ENTRYPOINT;
        if (relative.equals(project.xmlInput()))   return FileItem.FileRole.XML_INPUT;
        return FileItem.FileRole.REGULAR;
    }
}
