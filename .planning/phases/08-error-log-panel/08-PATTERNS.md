# Phase 8: Error & Log Panel - Pattern Map

**Mapped:** 2026-04-20
**Files analyzed:** 6
**Analogs found:** 6 / 6

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `src/main/java/ch/ti/gagi/xlseditor/log/LogEntry.java` | model | transform | `src/main/java/ch/ti/gagi/xlseditor/preview/PreviewError.java` | exact |
| `src/main/java/ch/ti/gagi/xlseditor/ui/LogController.java` | controller | event-driven | `src/main/java/ch/ti/gagi/xlseditor/ui/PreviewController.java` | exact |
| `src/main/java/ch/ti/gagi/xlseditor/ui/RenderController.java` | controller | request-response | self (modify existing) | self |
| `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` | controller | request-response | self (modify existing) | self |
| `src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml` | config | request-response | self (modify existing) | self |
| `src/main/resources/ch/ti/gagi/xlseditor/ui/main.css` | config | N/A | self (modify existing) | self |

---

## Pattern Assignments

### `src/main/java/ch/ti/gagi/xlseditor/log/LogEntry.java` (model, transform)

**Analog:** `src/main/java/ch/ti/gagi/xlseditor/preview/PreviewError.java`

The existing `LogEntry` is a plain final class with three fields. The three optional fields (`type`, `file`, `line`) being added exactly match the field set already present in `PreviewError`. Use `PreviewError` as the field-shape template for the extension.

**Current LogEntry shape** (`src/main/java/ch/ti/gagi/xlseditor/log/LogEntry.java` lines 1-18):
```java
package ch.ti.gagi.xlseditor.log;

public final class LogEntry {

    private final String message;
    private final String level;
    private final long timestamp;

    public LogEntry(String message, String level, long timestamp) {
        this.message = message;
        this.level = level;
        this.timestamp = timestamp;
    }

    public String message() { return message; }
    public String level()   { return level; }
    public long timestamp() { return timestamp; }
}
```

**Field shape to copy from PreviewError** (`src/main/java/ch/ti/gagi/xlseditor/preview/PreviewError.java` lines 1-21):
```java
// These three nullable fields are the ones being added to LogEntry
private final String type;
private final String file;
private final Integer line;

public String type()    { return type; }
public String file()    { return file; }
public Integer line()   { return line; }
```

**Extension pattern:** Add a second constructor that accepts all six fields (`message`, `level`, `timestamp`, `type`, `file`, `line`). Keep the existing three-arg constructor delegating to the full constructor with nulls. This matches the D-04 decision: existing `LogManager` convenience methods remain unchanged.

```java
// Primary constructor (all fields)
public LogEntry(String message, String level, long timestamp,
                String type, String file, Integer line) {
    this.message   = message;
    this.level     = level;
    this.timestamp = timestamp;
    this.type      = type;
    this.file      = file;
    this.line      = line;
}

// Convenience constructor for simple messages (backwards-compatible)
public LogEntry(String message, String level, long timestamp) {
    this(message, level, timestamp, null, null, null);
}
```

**Secondary analog for optional-field pattern:** `src/main/java/ch/ti/gagi/xlseditor/validation/ValidationError.java` (a record with nullable `line`/`column` Integer fields) confirms the project convention: optional numeric fields use `Integer` (boxed), not `int`.

---

### `src/main/java/ch/ti/gagi/xlseditor/ui/LogController.java` (controller, event-driven) — NEW

**Analog:** `src/main/java/ch/ti/gagi/xlseditor/ui/PreviewController.java`

This is a brand-new sub-controller. The entire structural pattern (class declaration, state fields, `initialize()` signature with `Objects.requireNonNull`, Javadoc lifecycle comment) is copied from `PreviewController`.

**Class header and Javadoc pattern** (`PreviewController.java` lines 1-30):
```java
package ch.ti.gagi.xlseditor.ui;

import ...;
import java.util.Objects;

/**
 * Sub-controller for the [concern] pane.
 * Owns [responsibilities].
 *
 * Lifecycle: MainController creates one instance as a field and calls
 * {@link #initialize} from its own {@code initialize()}. This controller is
 * NOT an FXML controller — MainController is the only @FXML controller.
 *
 * Phase 8 / ERR-01..ERR-05
 */
public final class LogController {
```

**State fields pattern** (`PreviewController.java` lines 31-36):
```java
// Each UI node injected via initialize() — never @FXML, never static
private TitledPane logPane;
private TableView<LogEntry> logTableView;
private EditorController editorController;

private final ObservableList<LogEntry> allEntries = FXCollections.observableArrayList();
private FilteredList<LogEntry> filteredEntries;
```

**initialize() signature pattern** (`PreviewController.java` lines 38-53):
```java
public void initialize(
    TitledPane logPane,
    TableView<LogEntry> logTableView,
    EditorController editorController
) {
    this.logPane          = Objects.requireNonNull(logPane,          "logPane");
    this.logTableView     = Objects.requireNonNull(logTableView,     "logTableView");
    this.editorController = Objects.requireNonNull(editorController, "editorController");
    // setup FilteredList, column factories, row click handler, toggle group
}
```

**Consumer callback pattern for setErrors** (from `RenderController.java` lines 34-37 and `PreviewController.java` line 55, adapted):
```java
// This method IS the Consumer<List<PreviewError>> passed to RenderController (D-06)
public void setErrors(List<PreviewError> errors) {
    allEntries.clear();  // ERR-05: clear before each render
    for (PreviewError err : errors) {
        allEntries.add(new LogEntry(
            err.message(), "ERROR", System.currentTimeMillis(),
            err.type(), err.file(), err.line()
        ));
    }
    // D-12: auto-expand if any ERROR or WARN present
    boolean hasErrorOrWarn = allEntries.stream()
        .anyMatch(e -> "ERROR".equals(e.level()) || "WARN".equals(e.level()));
    if (hasErrorOrWarn) logPane.setExpanded(true);
}
```

**FilteredList pattern** (standard JavaFX; D-10):
```java
// In initialize():
filteredEntries = new FilteredList<>(allEntries, e -> true);
logTableView.setItems(filteredEntries);
```

**ToggleGroup wiring pattern** (D-09/D-11) — copy the CSS class toggle style from `MainController.showTransientStatus` (`MainController.java` lines 328-329) for button state:
```java
// Button stays selected: prevent deselection of active toggle
toggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
    if (newVal == null) toggleGroup.selectToggle(oldVal);
});
```

**TableView cell factory for Level column** (D-02) — copy style from `FileItemTreeCell` / CSS class pattern in main.css:
```java
levelCol.setCellFactory(col -> new TableCell<>() {
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        getStyleClass().removeAll("log-error", "log-warn");
        if (empty || item == null) { setText(null); return; }
        setText(item);
        if ("ERROR".equals(item)) getStyleClass().add("log-error");
        else if ("WARN".equals(item))  getStyleClass().add("log-warn");
    }
});
```

**Row click handler for navigation** (D-14) — copy the mouse-click pattern from `FileTreeController.java` lines 131-135:
```java
logTableView.setOnMouseClicked(evt -> {
    if (evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 1) {
        LogEntry entry = logTableView.getSelectionModel().getSelectedItem();
        if (entry != null && entry.file() != null && entry.line() != null) {
            editorController.navigateTo(Path.of(entry.file()), entry.line() - 1, 0);
        }
    }
});
```

**addInfo() method** (D-08):
```java
public void addInfo(String message) {
    allEntries.add(new LogEntry(message, "INFO", System.currentTimeMillis()));
}
```

---

### `src/main/java/ch/ti/gagi/xlseditor/ui/RenderController.java` (controller, request-response) — MODIFY

**Current file:** `src/main/java/ch/ti/gagi/xlseditor/ui/RenderController.java`

The modification replaces the `ListView<String> logListView` parameter in `initialize()` with two `Consumer` callbacks and updates the body to use them instead of `logListView.getItems()`.

**Current initialize() signature** (lines 57-79) — replace `ListView<String> logListView` param:
```java
// BEFORE (lines 58-59, 67-68):
ListView<String> logListView,
...
this.logListView = Objects.requireNonNull(logListView, "logListView");

// AFTER (D-06/D-08):
Consumer<List<PreviewError>> errorsCallback,
Consumer<String> infoCallback,
...
this.errorsCallback = Objects.requireNonNull(errorsCallback, "errorsCallback");
this.infoCallback   = Objects.requireNonNull(infoCallback,   "infoCallback");
```

**Current log-clear call** (line 98) — replace with callback:
```java
// BEFORE:
logListView.getItems().clear();

// AFTER (ERR-05 — clear is now LogController's responsibility, triggered by setErrors):
// No explicit clear needed here; setErrors() in LogController clears before populating.
```

**Current success log call** (lines 140-142) — replace with callback:
```java
// BEFORE:
logListView.getItems().add("[INFO] Render complete in " + ...);

// AFTER (D-08):
infoCallback.accept("Render complete in " + String.format("%.1f", duration / 1000.0) + "s");
```

**Current error routing block** (lines 150-158) — replace with callback:
```java
// BEFORE: builds strings and adds to listview items

// AFTER (D-06):
errorsCallback.accept(result.errors());
```

**Current unexpected error log** (line 173) — replace with callback:
```java
// BEFORE:
logListView.getItems().add("[ERROR] Unexpected render error: " + ...);

// AFTER: wrap as a single-element list or use infoCallback with ERROR prefix
infoCallback.accept("Unexpected render error: " + (ex != null ? ex.getMessage() : "unknown"));
// OR: create a synthetic PreviewError and call errorsCallback with a singleton list
```

---

### `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` (controller, request-response) — MODIFY

**Current file:** `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java`

Three changes: add `logController` field, update FXML injections, rewire `renderController.initialize()`.

**Sub-controller field declaration pattern** (lines 71-73) — add LogController alongside existing:
```java
// BEFORE (lines 71-73):
private final RenderController renderController = new RenderController();
private final PreviewController previewController = new PreviewController();

// AFTER (add line):
private final LogController logController = new LogController();  // Phase 8
```

**FXML injection change** (lines 48-49) — replace ListView with TableView:
```java
// BEFORE:
@FXML private ListView<String> logListView;

// AFTER:
@FXML private TableView<LogEntry> logTableView;
// logPane stays: @FXML private TitledPane logPane;  (line 47, unchanged)
```

**initialize() wiring block** (lines 110-120) — add logController init and update renderController call:
```java
// ADD before renderController.initialize():
logController.initialize(logPane, logTableView, editorController);  // Phase 8

// CHANGE renderController.initialize() call:
renderController.initialize(
    renderButton,
    logController::setErrors,    // replaces logListView param (D-06)
    logController::addInfo,      // replaces ListView string appends (D-08)
    s -> statusLabel.setText(s),
    this::showTransientStatus,
    previewController::displayPdf,
    previewController::setOutdated,
    projectContext,
    editorController
);
```

**handleOpenProject log calls** (lines 195-209) — replace `logListView.getItems().add(...)` with `logController.addInfo(...)`:
```java
// BEFORE:
logListView.getItems().add("Loaded entrypoint: ...");

// AFTER:
logController.addInfo("Loaded entrypoint: ...");
```

---

### `src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml` (config) — MODIFY

**Current file:** `src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml`

Replace lines 95-97 (inner content of `logPane`) only. The `TitledPane` element itself is unchanged.

**Current inner content** (lines 95-97):
```xml
<TitledPane fx:id="logPane" text="Log" expanded="false" styleClass="log-titled-pane">
    <ListView fx:id="logListView" prefHeight="150"/>
</TitledPane>
```

**Replacement inner content** (D-17, D-18, D-09):
```xml
<TitledPane fx:id="logPane" text="Log" expanded="false" styleClass="log-titled-pane">
    <VBox>
        <HBox spacing="4" styleClass="log-filter-bar">
            <ToggleButton fx:id="filterAllButton"    text="All"     selected="true" styleClass="log-filter-btn"/>
            <ToggleButton fx:id="filterErrorButton"  text="Error"                   styleClass="log-filter-btn"/>
            <ToggleButton fx:id="filterWarnButton"   text="Warning"                 styleClass="log-filter-btn"/>
            <ToggleButton fx:id="filterInfoButton"   text="Info"                    styleClass="log-filter-btn"/>
        </HBox>
        <TableView fx:id="logTableView" prefHeight="120" VBox.vgrow="ALWAYS">
            <columns>
                <TableColumn fx:id="colTime"    text="Time"    prefWidth="65"  sortable="false"/>
                <TableColumn fx:id="colLevel"   text="Level"   prefWidth="60"  sortable="false"/>
                <TableColumn fx:id="colType"    text="Type"    prefWidth="100" sortable="false"/>
                <TableColumn fx:id="colMessage" text="Message" prefWidth="400" sortable="false"/>
            </columns>
        </TableView>
    </VBox>
</TitledPane>
```

Note: `ToggleGroup` membership and cell factories are set programmatically in `LogController.initialize()` — not in FXML — following the existing pattern where `FileTreeController` builds its `TreeView` entirely in Java.

Import to add at top of FXML (following existing `<?import ...?>` block pattern lines 3-5):
```xml
<?import javafx.scene.control.ToggleButton?>
<?import javafx.scene.control.ToggleGroup?>
<?import javafx.scene.control.TableView?>
<?import javafx.scene.control.TableColumn?>
```

---

### `src/main/resources/ch/ti/gagi/xlseditor/ui/main.css` (config) — MODIFY

**Current file:** `src/main/resources/ch/ti/gagi/xlseditor/ui/main.css`

**Phase comment convention pattern** (lines 92-93, 105-106) — every new CSS block opens with a phase comment:
```css
/* Phase N: description */
```

**Severity color pattern** — follow existing VS Code dark-theme palette. Error red mirrors nothing present; use a muted VS Code-style red. Warn yellow follows the `status-label-success` green pattern (line 35: `#66bb6a`). Existing `.file-tree-view .tree-cell.entrypoint` (line 83) and `.xml-input` (line 88) show the project's technique for role-based text coloring via CSS classes set programmatically.

**Add after line 113** (end of Phase 7 block):
```css
/* Phase 8: Log panel severity colors and filter bar */

.log-error {
    -fx-text-fill: #f44747;   /* VS Code error red */
}

.log-warn {
    -fx-text-fill: #cca700;   /* VS Code warning yellow */
}

/* TableView base — dark theme consistent with .log-titled-pane > .content */
.log-table-view {
    -fx-background-color: #1e1e1e;
    -fx-border-color: transparent;
}

.log-table-view .table-cell {
    -fx-text-fill: #cccccc;
    -fx-font-size: 12px;
    -fx-font-family: "Monospaced";
    -fx-background-color: transparent;
    -fx-padding: 2 6 2 6;
}

.log-table-view .column-header,
.log-table-view .column-header-background {
    -fx-background-color: #252526;
    -fx-text-fill: #888888;
    -fx-font-size: 11px;
}

/* Filter bar above the table */
.log-filter-bar {
    -fx-padding: 4 6 4 6;
    -fx-background-color: #1e1e1e;
}

.log-filter-btn {
    -fx-font-size: 11px;
    -fx-padding: 2 8 2 8;
    -fx-background-color: #2d2d2d;
    -fx-text-fill: #cccccc;
    -fx-border-color: #444444;
    -fx-border-radius: 3;
    -fx-background-radius: 3;
}

.log-filter-btn:selected {
    -fx-background-color: #094771;
    -fx-text-fill: #ffffff;
    -fx-border-color: #007acc;
}
```

---

## Shared Patterns

### Sub-controller lifecycle (apply to LogController)
**Source:** `src/main/java/ch/ti/gagi/xlseditor/ui/PreviewController.java` lines 1-53
- Class is `public final`
- Not an `@FXML` controller
- All UI node fields are private, set only in `initialize()`
- Every `initialize()` parameter guarded with `Objects.requireNonNull(param, "paramName")`
- Javadoc on the class names the phase requirements (e.g. `Phase 8 / ERR-01..ERR-05`)

### Consumer callback wiring (apply to RenderController/MainController changes)
**Source:** `src/main/java/ch/ti/gagi/xlseditor/ui/RenderController.java` lines 34-37 and `MainController.java` lines 116-117
- Callbacks stored as `Consumer<T>` fields, never as direct node references across controller boundaries
- Wired in `MainController.initialize()` using method references: `controller::method`
- D-07 pattern: `renderController.initialize(..., logController::setErrors, logController::addInfo, ...)`

### CSS class toggle for state-based styling (apply to Level column cell factory)
**Source:** `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` lines 328-329 and `main.css` lines 34-37
- CSS classes added/removed programmatically via `getStyleClass().removeAll(...) / .add(...)`
- Never set styles inline (`setStyle(...)`) when a CSS class exists
- Pattern: `node.getStyleClass().removeAll("cls-a", "cls-b"); node.getStyleClass().add("cls-x");`

### Phase-scoped CSS comments
**Source:** `src/main/resources/ch/ti/gagi/xlseditor/ui/main.css` lines 92, 105
- Every new CSS section opens with `/* Phase N: description */`
- Colors follow VS Code dark theme palette (established in Phase 5 comment lines 91-93)

---

## No Analog Found

All files have sufficient analogs. No files require falling back to RESEARCH.md patterns.

---

## Metadata

**Analog search scope:** `src/main/java/ch/ti/gagi/xlseditor/ui/`, `src/main/java/ch/ti/gagi/xlseditor/log/`, `src/main/java/ch/ti/gagi/xlseditor/preview/`, `src/main/java/ch/ti/gagi/xlseditor/validation/`, `src/main/resources/ch/ti/gagi/xlseditor/ui/`
**Files scanned:** 12 source files
**Pattern extraction date:** 2026-04-20
