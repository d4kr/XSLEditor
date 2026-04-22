# Phase 11: About Dialog - Pattern Map

**Mapped:** 2026-04-22
**Files analyzed:** 6
**Analogs found:** 5 / 6

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `src/main/java/ch/ti/gagi/xlseditor/ui/AboutDialog.java` | component | request-response | `src/main/java/ch/ti/gagi/xlseditor/ui/SearchDialog.java` | exact |
| `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` | controller | request-response | self (existing file, add handler) | exact |
| `src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml` | config | event-driven | self (existing file, add Menu node) | exact |
| `src/main/java/ch/ti/gagi/xlseditor/XLSEditorApp.java` | config | request-response | self (existing file, expose hostServices) | exact |
| `build.gradle` | config | batch | self (existing file, version + processResources) | exact |
| `src/main/resources/version.properties` | config | batch | no analog — new resource type | none |

---

## Pattern Assignments

### `src/main/java/ch/ti/gagi/xlseditor/ui/AboutDialog.java` (component, request-response)

**Analog:** `src/main/java/ch/ti/gagi/xlseditor/ui/SearchDialog.java`

**Imports pattern** (SearchDialog.java lines 1-19):
```java
package ch.ti.gagi.xlseditor.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
```
For AboutDialog replace with VBox/GridPane imports and add HostServices:
```java
package ch.ti.gagi.xlseditor.ui;

import ch.ti.gagi.xlseditor.XLSEditorApp;
import javafx.application.HostServices;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
```

**Class declaration and constructor signature** (SearchDialog.java lines 25, 88-95):
```java
public class SearchDialog extends Dialog<Void> {

    public SearchDialog(Stage ownerStage, Path projectRoot, EditorController editorController) {
        this.projectRoot      = projectRoot;
        this.editorController = editorController;

        if (ownerStage != null) initOwner(ownerStage);
        setTitle("Find in Files");
        setHeaderText(null);
        setResizable(true);
        ...
    }
```
AboutDialog mirrors this exactly — replace constructor parameters:
```java
public class AboutDialog extends Dialog<Void> {

    public AboutDialog(Stage ownerStage, HostServices hostServices) {
        if (ownerStage != null) initOwner(ownerStage);
        initModality(javafx.stage.Modality.APPLICATION_MODAL);
        setTitle("About " + XLSEditorApp.APP_NAME);
        setHeaderText(null);
        setResizable(false);
        ...
    }
```

**DialogPane + ButtonType pattern** (SearchDialog.java lines 111-112):
```java
getDialogPane().setContent(content);
getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
```
Copy verbatim into AboutDialog.

**Version loading pattern** — no existing analog; use standard Java resource loading:
```java
private String loadVersion() {
    try (InputStream in = getClass().getResourceAsStream("/version.properties")) {
        if (in == null) return "unknown";
        Properties p = new Properties();
        p.load(in);
        return p.getProperty("version", "unknown");
    } catch (IOException e) {
        return "unknown";
    }
}
```

**Runtime version retrieval pattern** — no existing analog; use API calls with fallback:
```java
// Saxon-HE
String saxonVersion = net.sf.saxon.Version.getProductVersion();

// FOP — org.apache.fop.Version is available in fop:2.9
String fopVersion = org.apache.fop.Version.getVersion();

// JavaFX
String javafxVersion = System.getProperty("javafx.version", "unknown");

// Java
String javaVersion = System.getProperty("java.version", "unknown");
```

**Hyperlink / HostServices pattern** — no existing analog; standard JavaFX:
```java
Hyperlink link = new Hyperlink("Apache License 2.0");
link.setOnAction(e -> hostServices.showDocument(
    "https://www.apache.org/licenses/LICENSE-2.0"));
```

---

### `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` — add `handleAbout()` (controller, request-response)

**Analog:** self — existing handler methods in MainController.java

**Handler naming pattern** (MainController.java lines 187-188, 195-196, 284-285, 292-293):
```java
@FXML
private void handleExit() { ... }

@FXML
private void handleOpenProject() { ... }

@FXML
private void handleRender() { ... }

@FXML
private void handleFindInFiles() { ... }
```
New handler follows the same structure — place after the Phase 5 handler block (after line 305):
```java
@FXML
private void handleAbout() {
    AboutDialog dialog = new AboutDialog(primaryStage, XLSEditorApp.hostServices());
    dialog.showAndWait();
}
```

**SearchDialog instantiation pattern** (MainController.java lines 303-304) — direct analog for handleAbout:
```java
SearchDialog dialog = new SearchDialog(primaryStage, root, editorController);
dialog.show(); // non-blocking — user can keep editing
```
AboutDialog uses `showAndWait()` instead of `show()` because it is APPLICATION_MODAL (D-04).

**Static HostServices exposure pattern** — add to XLSEditorApp and reference from MainController:
```java
// In XLSEditorApp (static accessor added in this phase):
private static HostServices hostServicesInstance;

public static HostServices getHostServices() {
    return hostServicesInstance;
}

// In start():
hostServicesInstance = getHostServices();
```

---

### `src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml` — add Help menu (config, event-driven)

**Analog:** self — existing `<Menu text="Search">` block (main.fxml lines 37-40)

**Existing Search menu pattern** (main.fxml lines 37-40):
```xml
<Menu text="Search">
    <MenuItem fx:id="findInFilesMenuItem" text="Find in Files"
              accelerator="Ctrl+Shift+F" onAction="#handleFindInFiles"/>
</Menu>
```
Help menu copies the same structure — insert after the closing `</Menu>` of Search (after line 40):
```xml
<Menu text="Help">
    <MenuItem text="About XLSEditor..." onAction="#handleAbout"/>
</Menu>
```
No `fx:id` needed on the MenuItem — it is not referenced from Java code.

---

### `src/main/java/ch/ti/gagi/xlseditor/XLSEditorApp.java` — expose hostServices (config, request-response)

**Analog:** self — existing `start()` method (XLSEditorApp.java lines 26-43)

**Existing start() pattern** (XLSEditorApp.java lines 26-43):
```java
@Override
public void start(Stage primaryStage) throws IOException {
    URL fxml = getClass().getResource("/ch/ti/gagi/xlseditor/ui/main.fxml");
    if (fxml == null) {
        throw new IllegalStateException("Cannot find main.fxml on classpath");
    }

    FXMLLoader loader = new FXMLLoader(fxml);
    Scene scene = new Scene(loader.load(), 1280, 800);

    MainController controller = loader.getController();
    controller.setPrimaryStage(primaryStage);

    primaryStage.setTitle(APP_NAME);
    primaryStage.setMinWidth(900);
    primaryStage.setMinHeight(600);
    primaryStage.setScene(scene);
    primaryStage.show();
}
```
Add static field and accessor alongside existing `APP_NAME` constant (line 23), then assign in `start()`:
```java
public static final String APP_NAME = "XLSEditor";
private static javafx.application.HostServices hostServicesInstance;

public static javafx.application.HostServices hostServices() {
    return hostServicesInstance;
}
```
Inside `start()`, assign before `primaryStage.show()`:
```java
hostServicesInstance = getHostServices(); // Application.getHostServices() inherited instance method
```

---

### `build.gradle` — version correction + processResources (config, batch)

**Analog:** self — existing `build.gradle`

**Version field to correct** (build.gradle line 13):
```groovy
version = '1.0.0'    // current — WRONG
```
Change to:
```groovy
version = '0.1.0'    // correct per D-02
```

**processResources block to add** — no existing analog in this file; Gradle standard pattern:
```groovy
processResources {
    filesMatching('version.properties') {
        expand(version: project.version)
    }
}
```
Insert after the `test { }` block (after line 64). The `expand()` call substitutes `${version}` tokens in the resource file at build time.

---

### `src/main/resources/version.properties` (config, batch)

**No analog** — new resource file. Standard Java `.properties` format with a Gradle `expand()` placeholder:
```properties
version=${version}
```
Place at `src/main/resources/version.properties` (classpath root, not under the package subdirectory) so `getClass().getResourceAsStream("/version.properties")` resolves it from any class.

---

## Shared Patterns

### Dialog ownership and modality
**Source:** `src/main/java/ch/ti/gagi/xlseditor/ui/SearchDialog.java` lines 92-95
**Apply to:** `AboutDialog.java` constructor
```java
if (ownerStage != null) initOwner(ownerStage);
setTitle("...");
setHeaderText(null);
setResizable(true);
```
AboutDialog additionally calls `initModality(Modality.APPLICATION_MODAL)` and sets `setResizable(false)` (D-04).

### @FXML handler declaration
**Source:** `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` lines 187-188
**Apply to:** `handleAbout()` in MainController
```java
@FXML
private void handleXxx() { ... }
```

### Alert with initOwner
**Source:** `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` lines 230-235
**Apply to:** any error path in handleAbout (unlikely needed but consistent if added)
```java
Alert alert = new Alert(Alert.AlertType.ERROR);
alert.initOwner(primaryStage);
alert.setTitle("...");
alert.setHeaderText("...");
alert.setContentText("...");
alert.showAndWait();
```

---

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `src/main/resources/version.properties` | config | batch | No `.properties` resource files exist in the project yet |

---

## Metadata

**Analog search scope:** `src/main/java/ch/ti/gagi/xlseditor/`, `src/main/resources/`, `build.gradle`
**Files scanned:** 5 source files read in full
**Pattern extraction date:** 2026-04-22
