package ch.ti.gagi.xlseditor.ui;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dialog for multi-file text search (EDIT-08).
 * Search logic is in the static method {@link #search(Path, String)} for testability.
 */
public class SearchDialog extends Dialog<Void> {

    /**
     * A single search result (file, line, column, matching line text).
     * toString() formats for display in the ListView.
     */
    public record SearchHit(Path file, int line, int column, String lineText) {
        @Override public String toString() {
            return file.getFileName() + ":" + (line + 1) + "  " + lineText.strip();
        }
    }

    // --- Static search logic (testable without JavaFX) ---

    /**
     * Scans all regular, non-hidden files under projectRoot for lines containing query.
     * Safe to call on a background thread. Returns an unmodifiable list of SearchHit.
     *
     * @param projectRoot root directory to scan
     * @param query       literal string to search for (case-sensitive)
     * @return list of hits, never null
     * @throws IOException if the root directory cannot be walked
     */
    public static List<SearchHit> search(Path projectRoot, String query) throws IOException {
        if (query == null || query.isBlank()) return List.of();
        List<SearchHit> hits = new ArrayList<>();
        Files.walk(projectRoot)
            .filter(p -> Files.isRegularFile(p) && !isHidden(p))
            .forEach(file -> {
                try {
                    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                    for (int i = 0; i < lines.size(); i++) {
                        int col = lines.get(i).indexOf(query);
                        if (col >= 0) {
                            hits.add(new SearchHit(file, i, col, lines.get(i)));
                        }
                    }
                } catch (IOException ignored) {
                    // Skip unreadable files (binary, permission errors)
                }
            });
        return Collections.unmodifiableList(hits);
    }

    private static boolean isHidden(Path p) {
        try { return Files.isHidden(p); } catch (IOException e) { return false; }
    }

    // --- JavaFX UI ---

    private final Path projectRoot;
    private final EditorController editorController;
    private final ListView<SearchHit> resultList = new ListView<>();
    private final TextField queryField = new TextField();
    private ExecutorService searchExecutor;

    /**
     * Creates and configures the search dialog.
     *
     * @param ownerStage       the primary stage (for modal ownership)
     * @param projectRoot      root directory to scan; must not be null
     * @param editorController for navigation on result click
     */
    public SearchDialog(Stage ownerStage, Path projectRoot, EditorController editorController) {
        this.projectRoot      = projectRoot;
        this.editorController = editorController;

        if (ownerStage != null) initOwner(ownerStage);
        setTitle("Find in Files");
        setHeaderText(null);
        setResizable(true);

        // Layout
        queryField.setPromptText("Search for...");
        queryField.setPrefWidth(400);
        Button searchBtn = new Button("Search");
        HBox searchBar = new HBox(8, queryField, searchBtn);
        searchBar.setPadding(new Insets(8));
        HBox.setHgrow(queryField, Priority.ALWAYS);

        resultList.setPrefHeight(300);
        BorderPane content = new BorderPane();
        content.setTop(searchBar);
        content.setCenter(resultList);
        content.setPrefWidth(500);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Search button action
        searchBtn.setOnAction(e -> triggerSearch());
        // Also trigger on Enter in query field
        queryField.setOnAction(e -> triggerSearch());

        // Result click: single-click navigates (friendlier than double-click for a dialog)
        resultList.setOnMouseClicked(e -> {
            SearchHit hit = resultList.getSelectionModel().getSelectedItem();
            if (hit != null) {
                editorController.navigateTo(hit.file(), hit.line(), hit.column());
            }
        });

        // Clean up executor when dialog closes — T-05-10 mitigation
        setOnCloseRequest(e -> {
            if (searchExecutor != null) searchExecutor.shutdownNow();
        });
    }

    private void triggerSearch() {
        String query = queryField.getText();
        if (query == null || query.isBlank()) return;

        // Shut down any in-progress search — T-05-10 mitigation
        if (searchExecutor != null) searchExecutor.shutdownNow();
        searchExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "search-thread");
            t.setDaemon(true);
            return t;
        });

        resultList.getItems().clear();
        resultList.setPlaceholder(new Label("Searching..."));

        Task<List<SearchHit>> task = new Task<>() {
            @Override protected List<SearchHit> call() throws Exception {
                return search(projectRoot, query);
            }
        };
        task.setOnSucceeded(e -> {
            List<SearchHit> results = task.getValue();
            resultList.getItems().setAll(results);
            if (results.isEmpty()) {
                resultList.setPlaceholder(new Label("No results found."));
            }
        });
        task.setOnFailed(e -> resultList.setPlaceholder(
            new Label("Search failed: " + task.getException().getMessage())));
        searchExecutor.submit(task);
    }
}
