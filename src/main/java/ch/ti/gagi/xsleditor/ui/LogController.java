package ch.ti.gagi.xsleditor.ui;

import ch.ti.gagi.xsleditor.XLSEditorApp;
import ch.ti.gagi.xsleditor.log.LogEntry;
import ch.ti.gagi.xsleditor.preview.PreviewError;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Sub-controller for the log panel pane.
 * Owns the ObservableList&lt;LogEntry&gt;, FilteredList, filter bar toggle group,
 * TableView column factories, severity color cell factory, and row click navigation.
 *
 * Lifecycle: MainController creates one instance as a field and calls
 * {@link #initialize} from its own {@code initialize()}. This controller is
 * NOT an FXML controller — MainController is the only @FXML controller.
 *
 * Phase 8 / ERR-01..ERR-05; Phase 12 / ERR-06
 */
public final class LogController {

    private TitledPane logPane;
    private TableView<LogEntry> logTableView;
    private EditorController editorController;

    private final ObservableList<LogEntry> allEntries = FXCollections.observableArrayList();
    private FilteredList<LogEntry> filteredEntries;

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public void initialize(
        TitledPane logPane,
        TableView<LogEntry> logTableView,
        TableColumn<LogEntry, String> colTime,
        TableColumn<LogEntry, String> colLevel,
        TableColumn<LogEntry, String> colType,
        TableColumn<LogEntry, String> colMessage,
        TableColumn<LogEntry, Void>   colAi,
        ToggleButton filterAllButton,
        ToggleButton filterErrorButton,
        ToggleButton filterWarnButton,
        ToggleButton filterInfoButton,
        EditorController editorController
    ) {
        this.logPane          = Objects.requireNonNull(logPane,          "logPane");
        this.logTableView     = Objects.requireNonNull(logTableView,     "logTableView");
        this.editorController = Objects.requireNonNull(editorController, "editorController");
        Objects.requireNonNull(colTime,            "colTime");
        Objects.requireNonNull(colLevel,           "colLevel");
        Objects.requireNonNull(colType,            "colType");
        Objects.requireNonNull(colMessage,         "colMessage");
        Objects.requireNonNull(colAi,              "colAi");
        Objects.requireNonNull(filterAllButton,    "filterAllButton");
        Objects.requireNonNull(filterErrorButton,  "filterErrorButton");
        Objects.requireNonNull(filterWarnButton,   "filterWarnButton");
        Objects.requireNonNull(filterInfoButton,   "filterInfoButton");

        // 1. Set up FilteredList and bind to TableView (D-10)
        filteredEntries = new FilteredList<>(allEntries, e -> true);
        logTableView.setItems(filteredEntries);
        logTableView.getStyleClass().add("log-table-view");
        logTableView.setEditable(false);

        // 2. Column cell value factories (lambda, not PropertyValueFactory — LogEntry uses method-style accessors)
        colTime.setCellValueFactory(cd ->
            new SimpleStringProperty(
                TIME_FMT.format(Instant.ofEpochMilli(cd.getValue().timestamp()))));
        colLevel.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().level()));
        colType.setCellValueFactory(cd ->
            new SimpleStringProperty(
                cd.getValue().type() != null ? cd.getValue().type() : ""));
        colMessage.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().message()));

        // 3. Level column cell factory for severity colors (D-02)
        colLevel.setCellFactory(col -> new TableCell<LogEntry, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("log-error", "log-warn");
                if (empty || item == null) { setText(null); return; }
                setText(item);
                if ("ERROR".equals(item))      getStyleClass().add("log-error");
                else if ("WARN".equals(item))  getStyleClass().add("log-warn");
            }
        });

        // 4. Filter bar ToggleGroup (D-09/D-11)
        ToggleGroup group = new ToggleGroup();
        filterAllButton.setToggleGroup(group);
        filterErrorButton.setToggleGroup(group);
        filterWarnButton.setToggleGroup(group);
        filterInfoButton.setToggleGroup(group);
        filterAllButton.setSelected(true);

        // D-11: active button cannot be deselected
        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null && oldVal != null) {
                group.selectToggle(oldVal);
                return;
            }
            filteredEntries.setPredicate(buildPredicate(
                filterAllButton, filterErrorButton, filterWarnButton, filterInfoButton));
        });
        filteredEntries.setPredicate(e -> true);

        // 5. Row click handler for navigation (D-14/D-15)
        logTableView.setOnMouseClicked(evt -> {
            if (evt.getButton() != MouseButton.PRIMARY) return;
            LogEntry entry = logTableView.getSelectionModel().getSelectedItem();
            if (entry == null) return;
            if (entry.file() != null && entry.line() != null) {
                // LogEntry.line is 1-based (from PreviewError); navigateTo is 0-based
                editorController.navigateTo(Path.of(entry.file()), entry.line() - 1, 0);
            }
        });

        // 6. AI assist column cell factory (ERR-06 / D-01..D-05)
        colAi.setCellFactory(col -> new TableCell<LogEntry, Void>() {
            private final Button btn = createAiButton();

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }

            private Button createAiButton() {
                Button b = new Button("💬"); // chat bubble
                b.setTooltip(new Tooltip("Ask ChatGPT about this error"));
                b.setStyle("-fx-padding: 1 4 1 4; -fx-font-size: 11;");
                b.setFocusTraversable(false);
                // D-05: consume MOUSE_PRESSED before it bubbles to the TableView row handler
                b.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED,
                        mouseEvt -> mouseEvt.consume());
                b.setOnAction(evt -> {
                    LogEntry entry = getTableRow().getItem();
                    if (entry == null || entry.message() == null) return;
                    // D-02: Italian preamble + raw message
                    String prompt = "Ho questo errore nel mio progetto XSLT/XSL-FO, puoi aiutarmi?\n\n"
                            + entry.message();
                    // D-03: URL-encode, replace "+" with "%20" for compatibility
                    String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8)
                            .replace("+", "%20");
                    String url = "https://chatgpt.com/?q=" + encoded;
                    XLSEditorApp.hostServices().showDocument(url);
                });
                return b;
            }
        });
    }

    private static Predicate<LogEntry> buildPredicate(
            ToggleButton all, ToggleButton err, ToggleButton warn, ToggleButton info) {
        if (all.isSelected())   return e -> true;
        if (err.isSelected())   return e -> "ERROR".equals(e.level());
        if (warn.isSelected())  return e -> "WARN".equals(e.level());
        if (info.isSelected())  return e -> "INFO".equals(e.level());
        return e -> true;
    }

    /**
     * D-06: IS the Consumer&lt;List&lt;PreviewError&gt;&gt; passed to RenderController by MainController.
     * ERR-05: clears before populating.
     */
    public void setErrors(List<PreviewError> errors) {
        Objects.requireNonNull(errors, "errors");
        allEntries.clear();
        long now = System.currentTimeMillis();
        for (PreviewError err : errors) {
            allEntries.add(new LogEntry(
                err.message(), "ERROR", now,
                err.type(), err.file(), err.line()));
        }
        // D-12: auto-expand if any ERROR or WARN present
        boolean hasErrorOrWarn = allEntries.stream()
            .anyMatch(e -> "ERROR".equals(e.level()) || "WARN".equals(e.level()));
        if (hasErrorOrWarn) logPane.setExpanded(true);
    }

    /**
     * D-08: simple INFO messages (no file/line context)
     */
    public void addInfo(String message) {
        Objects.requireNonNull(message, "message");
        allEntries.add(new LogEntry(message, "INFO", System.currentTimeMillis()));
    }

    /**
     * Exposed for tests / deferred coverage. Returns the live ObservableList.
     */
    public ObservableList<LogEntry> entries() {
        return allEntries;
    }
}
