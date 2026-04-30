package ch.ti.gagi.xsleditor.ui;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.wellbehaved.event.Nodes;
import org.reactfx.Subscription;

import ch.ti.gagi.xsleditor.util.XmlCharsetDetector;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static javafx.scene.input.KeyCode.S;
import static javafx.scene.input.KeyCode.SPACE;
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
    private ChangeListener<Tab> activeTabListener;

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
            Charset cs = XmlCharsetDetector.detect(key);
            String content = Files.readString(key, cs);
            EditorTab editorTab = new EditorTab(key, content, cs);
            Tab tab = buildTab(key, editorTab);
            registry.put(key, tab);
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);
        } catch (IOException ex) {
            showError("Open File Failed",
                "Could not read file: " + key.getFileName() + ". Check file permissions.");
        }
    }

    /**
     * Opens or focuses the tab for the given path, then moves the caret to the
     * specified line and column. Called by SearchDialog and log panel click handlers.
     *
     * @param path absolute path to open
     * @param line 0-based line index
     * @param col  0-based column index
     */
    public void navigateTo(Path path, int line, int col) {
        Path key = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        openOrFocusTab(key);
        Tab tab = registry.get(key);
        if (tab != null && tab.getUserData() instanceof EditorTab et) {
            et.codeArea.moveTo(line, col);
            et.codeArea.requestFollowCaret();
        }
    }

    /**
     * Returns the CodeArea of the currently selected editor tab,
     * or {@link Optional#empty()} if no tab is open.
     *
     * <p>Called from MainController Edit menu action handlers
     * (EDIT-10..13: Cut, Copy, Paste, Select All).</p>
     *
     * <p>Uses the established {@code getUserData() instanceof EditorTab}
     * pattern (see {@link #navigateTo}) so callers do not need to know
     * about the EditorTab/Tab wrapping convention.</p>
     */
    public Optional<CodeArea> getActiveCodeArea() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getUserData() instanceof EditorTab et) {
            return Optional.of(et.codeArea);
        }
        return Optional.empty();
    }

    /**
     * Registers a listener that fires whenever the selected editor tab changes,
     * receiving the new active CodeArea (or {@link Optional#empty()} if no tab is selected).
     *
     * <p>Used by MainController to rebind Undo/Redo disable properties to the
     * UndoManager of the newly focused tab (TOOL-01, TOOL-02, EDIT-14, EDIT-15).</p>
     *
     * <p>Safe to call multiple times: any previously registered listener is removed
     * before the new one is added, so only one listener is ever active at a time.</p>
     *
     * <p>The callback is invoked once immediately with the current selection so the
     * caller's bindings are correct at startup (when typically no tab is open yet).</p>
     */
    public void setOnActiveTabChanged(Consumer<Optional<CodeArea>> callback) {
        Objects.requireNonNull(callback, "callback");
        // Remove the prior listener (if any) before registering a new one,
        // so repeated calls never accumulate listeners on the selection property.
        if (activeTabListener != null) {
            tabPane.getSelectionModel().selectedItemProperty()
                   .removeListener(activeTabListener);
        }
        activeTabListener = (obs, oldTab, newTab) ->
            callback.accept(extractCodeArea(newTab));
        tabPane.getSelectionModel().selectedItemProperty()
               .addListener(activeTabListener);
        // Fire once with the current selection so initial disable state is correct.
        callback.accept(extractCodeArea(tabPane.getSelectionModel().getSelectedItem()));
    }

    private Optional<CodeArea> extractCodeArea(Tab tab) {
        if (tab != null && tab.getUserData() instanceof EditorTab et) {
            return Optional.of(et.codeArea);
        }
        return Optional.empty();
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

        // EDIT-04: async syntax highlighting via ReactFX successionEnds + ExecutorService
        // File size guard: skip for files > 5MB to avoid catastrophic backtracking
        final boolean tooLargeForHighlighting = editorTab.codeArea.getLength() > 5 * 1024 * 1024;
        final ExecutorService hlExecutor;
        final Subscription highlightSub;

        if (!tooLargeForHighlighting) {
            hlExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "highlight-thread-" + key.getFileName());
                t.setDaemon(true);
                return t;
            });
            highlightSub = editorTab.codeArea
                .multiPlainChanges()
                .successionEnds(Duration.ofMillis(300))
                .subscribe(change -> {
                    String snapshot = editorTab.codeArea.getText();
                    Task<StyleSpans<Collection<String>>> hlTask = new Task<>() {
                        @Override protected StyleSpans<Collection<String>> call() {
                            return XmlSyntaxHighlighter.computeHighlighting(snapshot);
                        }
                    };
                    hlTask.setOnSucceeded(e -> {
                        StyleSpans<Collection<String>> spans = hlTask.getValue();
                        // Pitfall 5: guard against race where text changed after snapshot
                        if (editorTab.codeArea.getLength() > 0
                                && spans.length() == editorTab.codeArea.getLength()) {
                            editorTab.codeArea.setStyleSpans(0, spans);
                        }
                    });
                    hlExecutor.submit(hlTask);
                });
            // Pitfall 6: apply initial highlighting for the file already loaded
            // WARNING FIX: files > 100 lines cause FX thread stutter if highlighted sync.
            // Use threshold of 500 chars: small files (< 500) are safe sync; larger go off-thread.
            String initialText = editorTab.codeArea.getText();
            if (initialText.length() > 0 && initialText.length() < 500) {
                // Small file: safe to highlight synchronously on FX thread
                editorTab.codeArea.setStyleSpans(0, XmlSyntaxHighlighter.computeHighlighting(initialText));
            } else if (initialText.length() >= 500) {
                // Large file: off-thread via hlExecutor to avoid FX thread stutter
                Task<StyleSpans<Collection<String>>> initTask = new Task<>() {
                    @Override protected StyleSpans<Collection<String>> call() {
                        return XmlSyntaxHighlighter.computeHighlighting(initialText);
                    }
                };
                initTask.setOnSucceeded(e -> {
                    StyleSpans<Collection<String>> spans = initTask.getValue();
                    if (editorTab.codeArea.getLength() > 0
                            && spans.length() == editorTab.codeArea.getLength()) {
                        editorTab.codeArea.setStyleSpans(0, spans);
                    }
                });
                if (hlExecutor != null) hlExecutor.submit(initTask);
            }
        } else {
            hlExecutor = null;
            highlightSub = null;
        }

        // EDIT-05: Ctrl+Space autocomplete — per-CodeArea InputMap (never scene-level)
        Nodes.addInputMap(editorTab.codeArea,
            consume(keyPressed(SPACE, CONTROL_DOWN),
                e -> AutocompleteProvider.triggerAt(editorTab.codeArea)));

        // EDIT-06: occurrence highlighting on text selection
        // Strip outer XML punctuation (<, >, /, whitespace) so selecting a full tag
        // like "<xsl:template>" matches the same name in closing tags and other occurrences.
        editorTab.codeArea.selectedTextProperty().addListener((obs, oldSel, newSel) -> {
            String token = (newSel == null) ? "" : newSel.strip().replaceAll("^[<>/\"'=]+|[<>/\"'=]+$", "");
            OccurrenceHighlighter.applyTo(editorTab.codeArea, token);
        });

        // EDIT-07: go-to-definition via Ctrl+Click on xsl:include/import href
        // Pitfall 3: use event.getX()/getY() (CodeArea-local coords, not screen coords)
        editorTab.codeArea.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (!event.isControlDown() || event.getButton() != MouseButton.PRIMARY) return;
            event.consume(); // prevent caret repositioning
            CharacterHit hit = editorTab.codeArea.hit(event.getX(), event.getY());
            HrefExtractor.extractHref(
                editorTab.codeArea.getText(),
                hit.getInsertionIndex(),
                key
            ).ifPresent(EditorController.this::openOrFocusTab);
        });

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
            if (highlightSub != null) highlightSub.unsubscribe(); // Pitfall 2: release CodeArea ref
            if (hlExecutor != null)   hlExecutor.shutdownNow();   // Pitfall 2: release thread
            editorTab.dirty.removeListener(dirtyListener);         // existing — keep
            registry.remove(key);                                  // existing — keep
            updateAppDirtyState();                                 // existing — keep
        });

        return tab;
    }

    private void saveTab(EditorTab editorTab) {
        try {
            Files.writeString(editorTab.path, editorTab.codeArea.getText(), editorTab.charset);
            editorTab.codeArea.getUndoManager().mark();
            updateAppDirtyState();
        } catch (IOException ex) {
            showError("Save Failed",
                "Could not save file: " + editorTab.path.getFileName()
                    + ". Check file permissions.");
        }
    }

    /**
     * Saves all dirty tabs silently. Called by RenderController before spawning the render Task (D-08/D-09).
     * Throws IOException on the first disk error so the caller can abort and alert the user.
     * Unlike saveTab(), this method does NOT show an error dialog — the caller decides how to handle the error.
     */
    public void saveAll() throws IOException {
        for (Tab tab : registry.values()) {
            if (tab.getUserData() instanceof EditorTab et && et.dirty.get()) {
                Files.writeString(et.path, et.codeArea.getText(), et.charset);
                et.codeArea.getUndoManager().mark();
            }
        }
        updateAppDirtyState();
    }
}
