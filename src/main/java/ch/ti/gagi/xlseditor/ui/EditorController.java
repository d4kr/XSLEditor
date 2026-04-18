package ch.ti.gagi.xlseditor.ui;

import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.wellbehaved.event.Nodes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static javafx.scene.input.KeyCode.S;
import static javafx.scene.input.KeyCombination.CONTROL_DOWN;
import static org.fxmisc.wellbehaved.event.EventPattern.keyPressed;
import static org.fxmisc.wellbehaved.event.InputMap.consume;

/**
 * Sub-controller for the editor pane. Owns a TabPane + Map<Path, Tab> registry,
 * implements open/save/close semantics (EDIT-01..03, EDIT-09), propagates the
 * aggregate dirty state back to MainController via a Consumer<Boolean>.
 *
 * Lifecycle: MainController creates one instance as a field and calls
 * {@link #initialize} from its own {@code initialize()}. This controller is
 * NOT an FXML controller — MainController is the only @FXML controller.
 */
public final class EditorController {

    // --- State ---

    private StackPane editorPane;
    private Supplier<Stage> primaryStageSupplier;
    private Consumer<Boolean> dirtyCallback;

    private TabPane tabPane;
    private final Map<Path, Tab> registry = new LinkedHashMap<>();

    // --- Public API ---

    /**
     * Wires this sub-controller to its host stack pane and services.
     * Idempotent; safe to call once from MainController.initialize().
     *
     * @param editorPane            the StackPane that will hold the TabPane
     * @param primaryStageSupplier  supplier for the primary Stage, used only for Alert.initOwner;
     *                              deferred because MainController.primaryStage is populated AFTER initialize runs
     * @param dirtyCallback         called with aggregate dirty state on every tab dirty transition,
     *                              save, and close; feeds MainController.setDirty(boolean)
     */
    public void initialize(
        StackPane editorPane,
        Supplier<Stage> primaryStageSupplier,
        Consumer<Boolean> dirtyCallback
    ) {
        this.editorPane           = Objects.requireNonNull(editorPane, "editorPane");
        this.primaryStageSupplier = Objects.requireNonNull(primaryStageSupplier, "primaryStageSupplier");
        this.dirtyCallback        = Objects.requireNonNull(dirtyCallback, "dirtyCallback");

        buildTabPane();
        mountTabPane();

        // Seed known-clean state so MainController aggregate starts from a defined baseline
        dirtyCallback.accept(false);
    }

    // --- Construction ---

    private void buildTabPane() {
        this.tabPane = new TabPane();
    }

    private void mountTabPane() {
        // Use setAll() (not add()) — StackPane layers children; add() leaves the Phase 1 placeholder Label visible behind the TabPane.
        editorPane.getChildren().setAll(tabPane);
    }

    // --- Helpers ---

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        Stage owner = primaryStageSupplier.get();
        if (owner != null) alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void updateAppDirtyState() {
        boolean anyDirty = registry.values().stream().anyMatch(tab -> {
            Object ud = tab.getUserData();
            return ud instanceof EditorTab et && et.dirty.get();
        });
        dirtyCallback.accept(anyDirty);
    }

    // --- Tab lifecycle (Task 2) ---

    /**
     * Opens the file at the given path in a new tab, or selects the existing tab
     * if the file is already open (EDIT-01 dedup via toAbsolutePath + normalize).
     *
     * @param path path to the file to open; must not be {@code null}
     */
    public void openOrFocusTab(Path path) {
        Path key = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        if (registry.containsKey(key)) {
            tabPane.getSelectionModel().select(registry.get(key));
            return;
        }
        try {
            String content = Files.readString(key, StandardCharsets.UTF_8);
            EditorTab editorTab = new EditorTab(key, content);
            Tab tab = buildTab(key, editorTab);
            registry.put(key, tab);
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);
        } catch (IOException ex) {
            showError("Open File Failed",
                "Could not read file: " + key.getFileName() + ". Check file permissions.");
        }
    }

    private Tab buildTab(Path key, EditorTab editorTab) {
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(editorTab.codeArea);
        String baseName = editorTab.path.getFileName().toString();
        Tab tab = new Tab(baseName, scrollPane);
        tab.setUserData(editorTab);   // consumed by updateAppDirtyState()

        // EDIT-02 — dirty prefix on tab title.
        // Listener is captured so it can be removed on tab close (WR-01: prevent memory leak).
        ChangeListener<Boolean> dirtyListener = (obs, wasDirty, isDirty) -> {
            tab.setText(isDirty ? "*" + baseName : baseName);
            updateAppDirtyState();
        };
        editorTab.dirty.addListener(dirtyListener);

        // EDIT-03 — Ctrl+S per focused CodeArea (NOT scene-level; RESEARCH.md Anti-Pattern)
        Nodes.addInputMap(editorTab.codeArea,
            consume(keyPressed(S, CONTROL_DOWN), e -> saveTab(editorTab)));

        // EDIT-09 — close-tab confirmation when dirty
        tab.setOnCloseRequest((Event event) -> {
            if (!editorTab.dirty.get()) return;
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            Stage owner = primaryStageSupplier.get();
            if (owner != null) alert.initOwner(owner);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("Close \"" + baseName + "\" without saving?");
            alert.setContentText("Your changes will be lost.");
            alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.CANCEL);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.YES) {
                event.consume();  // cancel the close
            }
        });

        // EDIT-09 / WR-01 — remove dirty listener on close to release EditorTab, CodeArea,
        // and UndoManager from the BooleanBinding's strong reference chain.
        tab.setOnClosed(e -> {
            editorTab.dirty.removeListener(dirtyListener);
            registry.remove(key);
            updateAppDirtyState();
        });

        return tab;
    }

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
}
