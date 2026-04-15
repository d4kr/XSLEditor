# Phase 2: Project Management - Research

**Researched:** 2026-04-15
**Domain:** JavaFX project-open workflow, JSON config read/write, dialog patterns
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Directory without `.xslfo-tool.json` → load project with `entryPoint=null` and `xmlInput=null`. Do NOT create the file immediately.
- **D-02:** `.xslfo-tool.json` is created (or updated) only when the user actively sets entrypoint or XML input.
- **D-03:** `ProjectConfig` must be relaxed to allow partial state: both `entryPoint` and `xmlInput` are optional on read (nullable). "Both required" constraint moves to render layer.
- **D-04:** "Set Entrypoint" and "Set XML Input" actions are created in Phase 2 but remain **disabled**. No functional implementation this phase.
- **D-05:** Phase 3 enables these actions and implements tree-selection wiring.
- **D-06:** Project state lives in a `ProjectContext` class, not inline in `MainController`. `MainController` delegates to `ProjectContext`.
- **D-07:** On project open: call `MainController.updateTitle(projectName)` with the directory name.
- **D-08:** "New File" accepts any filename (no extension restriction).
- **D-09:** After file creation: write empty file to project root. No auto-open in editor.
- **D-10:** "New File" is a menu action under File menu. Disabled when no project is open.

### Claude's Discretion

- Exact class name: `ProjectContext` or `AppController` (either acceptable)
- How `ProjectConfig.write()` / save method is added (method on record, static helper, or via `ProjectManager`)
- Menu placement: exact menu items and keyboard shortcuts for "Open Project" and "New File"
- Error handling UX for invalid directories (no files, permission denied)

### Deferred Ideas (OUT OF SCOPE)

- "Set Entrypoint" / "Set XML Input" functional implementation → Phase 3
- Auto-open new file in editor after creation → Phase 4
- Extension validation on New File → not needed
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PROJ-01 | User can open a project by selecting a directory from the filesystem | `DirectoryChooser.showDialog()` returns `File`; wrap in `ProjectContext.openProject(Path)` |
| PROJ-02 | User can select the entrypoint XSLT file from the project files | Scaffold disabled `MenuItem`; `setDisable(true)` — enabled in Phase 3 |
| PROJ-03 | User can select the XML input file from the project files | Scaffold disabled `MenuItem`; `setDisable(true)` — enabled in Phase 3 |
| PROJ-04 | Entrypoint and XML input selections are persisted in `.xslfo-tool.json` | Jackson `ObjectMapper.writeValue(File, Object)` in new `ProjectConfig.write(Path)` method |
| PROJ-05 | Application reads `.xslfo-tool.json` on project open and restores selections | `ProjectConfig.read(Path)` with null-safe field access (compact-constructor relaxation) |
| PROJ-06 | User can create a new file in the project root directory | `TextInputDialog` + `Files.writeString(path, "")` via `ProjectFileManager`-style helper |
</phase_requirements>

---

## Summary

Phase 2 wires the "Open Project" UX onto the existing backend model classes (`Project`, `ProjectConfig`, `ProjectManager`). The primary work is three-fold: (1) relax `ProjectConfig`'s compact constructor so it accepts null fields, (2) add a `write(Path)` method to `ProjectConfig` using the already-imported Jackson `ObjectMapper`, and (3) introduce a `ProjectContext` class that holds the live `Project` reference and drives `MainController` updates.

No new external dependencies are introduced. All dialog types (`DirectoryChooser`, `TextInputDialog`, `Alert`) are standard JavaFX 21 controls already on the classpath. Jackson is already on the classpath for `ProjectConfig.read()`. The FXML menu scaffold is in place; Phase 2 adds `MenuItem` entries and wires `onAction` handlers.

The largest risk is the `ProjectConfig` compact-constructor rewrite: the existing code throws on null fields, which contradicts decision D-03. Every downstream consumer of `ProjectConfig` that assumes non-null `entryPoint`/`xmlInput` must be audited before the constraint is removed.

**Primary recommendation:** Introduce `ProjectContext` as the single source of truth for the open project; wire `MainController` to delegate all project-related state reads and writes to it. Keep `MainController` as a thin UI coordinator.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Directory selection dialog | UI (MainController handler) | — | JavaFX `DirectoryChooser` must run on FX Application Thread; result passed to `ProjectContext` |
| Project state (open Project object) | Service layer (`ProjectContext`) | — | D-06: state must not live inline in `MainController` |
| Config read | Model (`ProjectConfig.read`) | `ProjectManager` | Already implemented; needs null relaxation |
| Config write | Model (`ProjectConfig.write`) | — | New method, same class, same `ObjectMapper` instance |
| New file creation | Model/IO helper | `ProjectContext` as coordinator | `Files.writeString` to project root; no editor coupling in Phase 2 |
| Menu item enabled state | UI (`MainController` or `ProjectContext` observable) | — | `MenuItem.disableProperty()` bound to `ProjectContext.projectLoadedProperty()` |
| Window title update | UI (`MainController.updateTitle`) | — | Already implemented in Phase 1; called from `ProjectContext` callback or directly |
| Status feedback (transient label) | UI | — | 3-second `PauseTransition` + label text toggle |

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JavaFX Controls | 21 | `DirectoryChooser`, `TextInputDialog`, `Alert`, `MenuItem` | Already on classpath via `javafx.controls` module |
| Jackson Databind | 2.17.2 | JSON config write (`ObjectMapper.writeValue`) | Already imported in `ProjectConfig.read()` |
| Java NIO (`java.nio.file`) | JDK 21 | `Files.writeString`, `Files.exists`, `Files.createFile` | Standard JDK — no dep needed |
| JavaFX Animation (`PauseTransition`) | 21 | 3-second auto-hide for status label | Standard `javafx.animation` module — already on classpath |

### No New Dependencies

Phase 2 requires **zero new build.gradle additions**. All needed APIs are already present.

---

## Architecture Patterns

### System Architecture Diagram

```
User clicks "Open Project"
         |
         v
MainController.handleOpenProject()
         |
         v
DirectoryChooser.showDialog(primaryStage)  --> [OS native dialog]
         |
    [null = cancelled]
         |
    [File returned]
         |
         v
ProjectContext.openProject(Path rootPath)
         |
         +---> Project.deriveConfigPath(rootPath) --> Path configPath
         |
         +---> configPath exists?
         |       YES --> ProjectConfig.read(configPath)
         |                   |
         |                   +--> entryPoint/xmlInput may be null (D-03)
         |                   |
         |                   v
         |               Project(rootPath, entryPoint, xmlInput)
         |       NO  --> Project(rootPath, null, null)          (D-01)
         |
         v
ProjectContext stores Project (field)
         |
         +---> MainController.updateTitle(project.rootPath().getFileName())  (D-07)
         +---> menuItemSetEntrypoint.setDisable(false)   [still disabled — D-04]
         +---> menuItemSetXmlInput.setDisable(false)     [still disabled — D-04]
         +---> menuItemNewFile.setDisable(false)         (D-10)
         +---> [log entry] config restored / no config found
         +---> [status label] "Project opened: {name}" for 3 seconds
```

### Recommended Project Structure Changes

```
src/main/java/ch/ti/gagi/xlseditor/
├── model/
│   ├── ProjectConfig.java       [MODIFY: relax compact constructor, add write()]
│   ├── Project.java             [unchanged]
│   ├── ProjectManager.java      [MODIFY: handle missing config (no-config path)]
│   ├── ProjectFileManager.java  [unchanged — reuse for new file creation]
│   └── ProjectFile.java         [unchanged]
└── ui/
    ├── MainController.java      [MODIFY: add menu handlers, delegate to ProjectContext]
    └── ProjectContext.java      [NEW: project state service]
```

```
src/main/resources/ch/ti/gagi/xlseditor/ui/
├── main.fxml     [MODIFY: add MenuItem entries to File menu]
└── main.css      [MODIFY: add .status-label-success and .menu-item:disabled rules]
```

### Pattern 1: Relax `ProjectConfig` Compact Constructor

**What:** Remove the non-null assertions; allow `entryPoint` and `xmlInput` to be null.
**When to use:** Required by D-03 to support partial config state.

```java
// Source: existing ProjectConfig.java — modified compact constructor
public record ProjectConfig(String entryPoint, String xmlInput) {

    public ProjectConfig {
        // Nulls allowed — both fields are optional (D-03)
        // Relative-path assertion kept only when value is non-null
        if (entryPoint != null && Path.of(entryPoint).isAbsolute()) {
            throw new IllegalArgumentException("entryPoint must be a relative path");
        }
        if (xmlInput != null && Path.of(xmlInput).isAbsolute()) {
            throw new IllegalArgumentException("xmlInput must be a relative path");
        }
    }

    // ... read() unchanged (already null-safe in current implementation)

    public void write(Path configPath) throws IOException {
        // Source: Jackson Databind ObjectMapper.writeValue(File, Object)
        // [VERIFIED: Context7 /fasterxml/jackson-databind]
        Map<String, String> map = new LinkedHashMap<>();
        if (entryPoint != null) map.put("entryPoint", entryPoint);
        if (xmlInput != null)   map.put("xmlInput", xmlInput);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), map);
    }
}
```

### Pattern 2: `ProjectManager` — No-Config Path

**What:** Overload or modify `loadProject` to handle the case where no `.xslfo-tool.json` exists.
**When to use:** Required by D-01.

```java
// Source: existing ProjectManager.java — modified
public static Project loadProject(Path rootPath) throws IOException {
    Path configPath = Project.deriveConfigPath(rootPath);
    if (!Files.exists(configPath)) {
        // D-01: no config → partial project, both nulls
        return new Project(rootPath, null, null);
    }
    try {
        ProjectConfig config = ProjectConfig.read(configPath);
        Path ep = config.entryPoint() != null ? Path.of(config.entryPoint()) : null;
        Path xi = config.xmlInput()   != null ? Path.of(config.xmlInput())   : null;
        return new Project(rootPath, ep, xi);
    } catch (IOException e) {
        // Config parse failure — surface as warning, return partial project (D-03)
        throw e; // caller (ProjectContext) catches and shows Alert(WARNING)
    }
}
```

### Pattern 3: `ProjectContext` — State Service

**What:** Single class holding the live `Project` reference and `BooleanProperty` for "project is loaded".
**Why:** D-06 prohibits inline state in `MainController`.

```java
// Source: JavaFX BooleanProperty pattern [VERIFIED: Context7 /websites/openjfx_io_javadoc_21]
public final class ProjectContext {

    private Project currentProject;
    private final BooleanProperty projectLoaded = new SimpleBooleanProperty(false);

    public BooleanProperty projectLoadedProperty() { return projectLoaded; }
    public boolean isProjectLoaded()               { return projectLoaded.get(); }
    public Project getCurrentProject()             { return currentProject; }

    public void openProject(Path rootPath, MainController controller) throws IOException {
        Project project = ProjectManager.loadProject(rootPath);
        this.currentProject = project;
        this.projectLoaded.set(true);
        controller.updateTitle(rootPath.getFileName().toString()); // D-07
    }

    public void createFile(String filename) throws IOException {
        if (currentProject == null) throw new IllegalStateException("No project loaded");
        Path target = currentProject.rootPath().resolve(filename);
        Files.createFile(target); // throws FileAlreadyExistsException if present
    }
}
```

### Pattern 4: `DirectoryChooser` Usage

**What:** Standard JavaFX pattern for OS-native directory picker.
**Source:** [VERIFIED: Context7 /websites/openjfx_io_javadoc_21]

```java
// In MainController.handleOpenProject():
DirectoryChooser chooser = new DirectoryChooser();
chooser.setTitle("Open Project");
chooser.setInitialDirectory(new File(System.getProperty("user.home")));

File selected = chooser.showDialog(primaryStage);
if (selected == null) return; // cancelled — no action (UI spec)

try {
    projectContext.openProject(selected.toPath(), this);
    // log entry + status label (UI spec)
} catch (IOException e) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.initOwner(primaryStage);
    alert.setTitle("Open Project Failed");
    alert.setHeaderText("Open Project Failed");
    alert.setContentText("Could not open project: " + selected.getAbsolutePath()
        + ". Check that the directory is readable.");
    alert.showAndWait();
}
```

### Pattern 5: `TextInputDialog` — New File

**What:** Prompt for filename, validate, create file.
**Source:** [VERIFIED: Context7 /websites/openjfx_io_javadoc_21]

```java
// In MainController.handleNewFile():
TextInputDialog dialog = new TextInputDialog();
dialog.initOwner(primaryStage);
dialog.setTitle("New File");
dialog.setHeaderText("Create a new file in the project root");
dialog.setContentText("File name:");
dialog.getEditor().setPromptText("e.g. output.xsl");

Optional<String> result = dialog.showAndWait();
result.ifPresent(name -> {
    if (name.isBlank()) {
        showError("Invalid File Name", "File name cannot be empty.");
        return;
    }
    try {
        projectContext.createFile(name);
        // Phase 3 tree will refresh; no action needed here (D-09)
    } catch (FileAlreadyExistsException e) {
        Alert warn = new Alert(Alert.AlertType.WARNING);
        warn.initOwner(primaryStage);
        warn.setTitle("File Already Exists");
        warn.setHeaderText("File Already Exists");
        warn.setContentText("A file named \"" + name
            + "\" already exists in the project root. Choose a different name.");
        warn.showAndWait();
    } catch (IOException e) {
        showError("Create File Failed", "Could not create file: " + e.getMessage());
    }
});
```

### Pattern 6: MenuItem Disable Binding

**What:** Bind `MenuItem.disableProperty()` to the inverse of `ProjectContext.projectLoadedProperty()`.
**When to use:** D-04 and D-10 — grayed-out actions until project is loaded.
**Source:** [VERIFIED: Context7 /websites/openjfx_io_javadoc_21]

```java
// In MainController, after ProjectContext is wired:
menuItemNewFile.disableProperty().bind(projectContext.projectLoadedProperty().not());
// Set Entrypoint / Set XML Input remain disabled unconditionally in Phase 2 (D-04):
menuItemSetEntrypoint.setDisable(true);
menuItemSetXmlInput.setDisable(true);
```

### Pattern 7: Transient Status Label (3-second fade)

**What:** Show "Project opened: {name}" for 3 seconds then clear.

```java
// Source: JavaFX PauseTransition [VERIFIED: standard javafx.animation]
statusLabel.setText("Project opened: " + projectName);
statusLabel.getStyleClass().add("status-label-success");
PauseTransition pause = new PauseTransition(Duration.seconds(3));
pause.setOnFinished(e -> statusLabel.setText(""));
pause.play();
```

### FXML Menu Changes

Add inside `<Menu text="File">` in `main.fxml`:

```xml
<MenuItem fx:id="menuItemOpenProject" text="Open Project..." onAction="#handleOpenProject"/>
<SeparatorMenuItem/>
<MenuItem fx:id="menuItemSetEntrypoint" text="Set Entrypoint"/>
<MenuItem fx:id="menuItemSetXmlInput" text="Set XML Input"/>
<SeparatorMenuItem/>
<MenuItem fx:id="menuItemNewFile" text="New File..." onAction="#handleNewFile"/>
<SeparatorMenuItem/>
<MenuItem text="Exit" onAction="#handleExit"/>
```

Note: `menuItemSetEntrypoint` and `menuItemSetXmlInput` have no `onAction` in Phase 2 (D-04).

### Anti-Patterns to Avoid

- **Storing `Project` directly in `MainController`:** Violates D-06. All project state goes in `ProjectContext`.
- **Using `show()` instead of `showAndWait()` for dialogs:** JavaFX docs require `showAndWait()` for blocking input; `show()` is non-blocking and incompatible with sequential validation logic. [VERIFIED: Context7 /websites/openjfx_io_javadoc_21]
- **Creating config file on directory open:** Violates D-01/D-02. File is only written when entrypoint or XML input is actively set.
- **Not calling `alert.initOwner(primaryStage)`:** Pattern established in Phase 1 `handleCloseRequest` — must be applied consistently to all Alert instances in Phase 2.
- **Blocking the FX Application Thread:** `ProjectManager.loadProject()` is fast (local file I/O), so no `Task<>` is needed for Phase 2. Do not over-engineer with background threads.
- **Jackson `ObjectMapper` instantiation per call:** The `MAPPER` static field on `ProjectConfig` is the correct pattern; do not create new instances per write.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| OS-native directory picker | Custom dialog | `DirectoryChooser` | Platform look and feel; handles permissions and bookmark access |
| Text prompt dialog | Custom `Stage` | `TextInputDialog` | Standard API; handles keyboard, accessibility, owner modal |
| JSON serialization | String concatenation | `ObjectMapper.writeValue` | Handles escaping, pretty-print, null omission |
| Empty file creation | `FileOutputStream` boilerplate | `Files.createFile(path)` + optional `Files.writeString(path, "")` | Atomic, throws `FileAlreadyExistsException` on conflict |
| Timed UI feedback | `Thread.sleep` + Platform.runLater | `PauseTransition` | Integrates with FX animation loop; no thread management |

---

## Common Pitfalls

### Pitfall 1: `ProjectConfig` Compact Constructor Still Throws on Null

**What goes wrong:** After removing the null check from the compact constructor, the relative-path assertion `Path.of(null)` still throws `NullPointerException` because `Path.of(String)` does not accept null.
**Why it happens:** Compact constructor body runs before field assignment; `Path.of(entryPoint)` is called even when `entryPoint` is null.
**How to avoid:** Guard the path assertion with a null check: `if (entryPoint != null && Path.of(entryPoint).isAbsolute())`.
**Warning signs:** `NullPointerException` from within `ProjectConfig` constructor when opening a directory with no config file.

### Pitfall 2: `DirectoryChooser` Returns `File`, Not `Path`

**What goes wrong:** Calling `.toPath()` on the result without null-guarding; or passing `File` directly to `ProjectManager.loadProject(Path)`.
**Why it happens:** `DirectoryChooser.showDialog()` returns `java.io.File` (legacy API); phase uses `Path` everywhere.
**How to avoid:** Always null-check: `File selected = chooser.showDialog(...); if (selected == null) return;` then `selected.toPath()`.
**Warning signs:** NPE on cancel if the null guard is missing.

### Pitfall 3: Config Parse Failure Swallowed Silently

**What goes wrong:** `ProjectConfig.read()` throws `IOException` when JSON is malformed; if not caught, the stack trace appears in stderr and the UI freezes with no feedback.
**Why it happens:** `ProjectManager.loadProject()` propagates the exception; the caller in `MainController` must catch and present `Alert(WARNING)` per the UI spec.
**How to avoid:** `catch (IOException e)` in `handleOpenProject()`, with two branches: generic IO failure → `Alert(ERROR)`, JSON parse failure → `Alert(WARNING)` per copywriting contract.
**Warning signs:** No alert shown on opening a directory with a corrupted `.xslfo-tool.json`.

### Pitfall 4: Menu Item `onAction` Without `fx:id`

**What goes wrong:** FXML MenuItem with `onAction="#handleOpenProject"` but no `fx:id` cannot be accessed from the controller for `disableProperty()` binding.
**Why it happens:** FXML injection requires `fx:id`; `onAction` alone does not create an injectable field.
**How to avoid:** Always declare both `fx:id` and `onAction` on menu items that need property binding.
**Warning signs:** `NullPointerException` on `menuItemNewFile.disableProperty()` in `initialize()`.

### Pitfall 5: Transient Status Label Has No `fx:id` in FXML

**What goes wrong:** The status label must be in the FXML with an `fx:id` to be injectable; if added only in CSS or omitted from FXML, `PauseTransition` target is null.
**Why it happens:** UI spec introduces a transient status label that does not yet exist in Phase 1's FXML.
**How to avoid:** Add `<Label fx:id="statusLabel" .../>` to `main.fxml` in Phase 2, likely in the `fileTreePane` area or a dedicated status row.
**Warning signs:** NPE calling `statusLabel.setText()` in `handleOpenProject()`.

### Pitfall 6: `showAndWait()` Called Outside FX Application Thread

**What goes wrong:** `IllegalStateException: Not on FX application thread` if dialog methods are called from a background thread.
**Why it happens:** JavaFX dialog API is thread-confined.
**How to avoid:** Keep all dialog calls in the `@FXML` handler methods or wrap with `Platform.runLater()` if ever triggered from a background task. Phase 2 does not use background threads, so this is only a risk if someone later moves project loading to a `Task<>`.

---

## Code Examples

### Reading Config (already exists — for reference)

```java
// Source: existing ProjectConfig.java
public static ProjectConfig read(Path configPath) throws IOException {
    JsonNode root = MAPPER.readTree(configPath.toFile());
    JsonNode ep = root.get("entryPoint");
    JsonNode xi = root.get("xmlInput");
    return new ProjectConfig(
        ep != null ? ep.asText() : null,
        xi != null ? xi.asText() : null
    );
}
```

Note: `asText()` on a missing node returns `"null"` as a String in some Jackson versions — verify behavior. Safer: `ep != null && !ep.isNull() ? ep.asText() : null`.

### Writing Config (new)

```java
// Source: Jackson Databind ObjectMapper.writeValue [VERIFIED: Context7 /fasterxml/jackson-databind]
public void write(Path configPath) throws IOException {
    Map<String, String> map = new LinkedHashMap<>();
    if (entryPoint != null) map.put("entryPoint", entryPoint);
    if (xmlInput   != null) map.put("xmlInput",   xmlInput);
    MAPPER.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), map);
}
```

### Creating Empty File

```java
// Source: JDK 21 java.nio.file.Files [ASSUMED — standard JDK API, stable]
// Files.createFile throws FileAlreadyExistsException (checked, extends IOException)
Files.createFile(target); // Creates empty file atomically
// If empty content is explicitly needed: Files.writeString(target, "")
```

---

## Runtime State Inventory

Not applicable. Phase 2 is a greenfield UI addition, not a rename/refactor/migration. No runtime state migration needed.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JavaFX 21 | All dialogs, FXML | Yes (build.gradle) | 21 | — |
| Jackson Databind | Config write | Yes (build.gradle) | 2.17.2 | — |
| JDK 21 | `Files.createFile`, `Path` | Yes (toolchain) | 21 | — |
| Gradle 9 + Shadow 9.0.0-beta12 | Build | Yes (existing) | 9.x | — |

No missing dependencies. Phase 2 requires no new additions to `build.gradle`.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter 5.10.0 |
| Config file | `build.gradle` → `test { useJUnitPlatform() }` |
| Quick run command | `./gradlew test` |
| Full suite command | `./gradlew test` |

Note: No test source directory exists yet (`src/test/` is absent). All tests will be Wave 0 gaps.

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PROJ-01 | `DirectoryChooser` dialog result processed | manual-only | — `DirectoryChooser` requires OS GUI | N/A |
| PROJ-02 | "Set Entrypoint" MenuItem is disabled | manual-only | — requires FX toolkit | N/A |
| PROJ-03 | "Set XML Input" MenuItem is disabled | manual-only | — requires FX toolkit | N/A |
| PROJ-04 | Config written to `.xslfo-tool.json` | unit | `./gradlew test --tests ProjectConfigTest.writesJsonFile` | Wave 0 |
| PROJ-05 | Config read restores entrypoint + xmlInput | unit | `./gradlew test --tests ProjectConfigTest.readsPartialConfig` | Wave 0 |
| PROJ-06 | New file created in project root | unit | `./gradlew test --tests ProjectContextTest.createFileWritesEmptyFile` | Wave 0 |

PROJ-01/02/03 require the JavaFX Application Thread and an OS display — unit testing is impractical without TestFX or Monocle. These are best verified manually (or deferred to Phase 9 integration tests). Phase 2 unit tests focus on the pure model layer: `ProjectConfig` read/write and `ProjectContext.createFile`.

### Sampling Rate

- **Per task commit:** `./gradlew test`
- **Per wave merge:** `./gradlew test`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps

- [ ] `src/test/java/ch/ti/gagi/xlseditor/model/ProjectConfigTest.java` — covers PROJ-04, PROJ-05
- [ ] `src/test/java/ch/ti/gagi/xlseditor/model/ProjectContextTest.java` — covers PROJ-06
- [ ] `src/test/resources/` — temp directory fixtures for file I/O tests (use `@TempDir` JUnit annotation)

---

## Security Domain

Phase 2 has no network I/O, no authentication, no external service calls, and no untrusted input from a remote source. The tool is internal-developer-only (CLAUDE.md, PRD). Standard ASVS categories:

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | Internal tool, no auth |
| V3 Session Management | No | Desktop app, no sessions |
| V4 Access Control | No | Single-user, no roles |
| V5 Input Validation | Minimal | Filename blank-check + `FileAlreadyExistsException` |
| V6 Cryptography | No | No secrets stored |

The only input from users is a filename string in `TextInputDialog`. Validation required: non-blank check (UI spec). Path traversal (`../`) is a potential concern if `filename` is used in `Path.resolve()` without normalization — the plan should include a `filename.contains("/")` or `Path.normalize()` guard.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `Files.createFile(path)` throws `FileAlreadyExistsException` as a checked `IOException` subclass | Code Examples | Risk LOW — standard JDK 21 API, stable behavior |
| A2 | `JsonNode.asText()` returns `"null"` string (not Java null) when `JsonNode.isNull()` is true | Code Examples | Risk MEDIUM — if `ep.asText()` returns `"null"`, `ProjectConfig` stores the string `"null"` as entrypoint; add `!ep.isNull()` guard |
| A3 | `PauseTransition` is available in `javafx.animation` which is transitively included via `javafx.controls` | Code Examples | Risk LOW — standard JavaFX module graph |

---

## Open Questions (RESOLVED)

1. **Where is the status label placed in the layout?**
   - What we know: UI spec calls for a transient "Project opened: {name}" text label; Phase 1 FXML has no dedicated status row.
   - What's unclear: Should it overlay the `fileTreePane` placeholder, appear in a new bottom status bar, or be placed in the log panel as an info entry?
   - Recommendation: Simplest compliant approach — add an info entry to `logListView` (already in FXML with `fx:id="logListView"`); for the transient colored label, add a `Label` with `fx:id="statusLabel"` to the `fileTreePane` StackPane (it stacks over the placeholder and is invisible when empty).

2. **Should `ProjectContext` hold a reference to `MainController`, or use callbacks?**
   - What we know: D-06 requires `ProjectContext` as state owner; D-07 requires `MainController.updateTitle()` to be called after project open.
   - What's unclear: Whether `ProjectContext` should directly call `MainController` methods (coupling) or emit events / call a `Runnable` callback.
   - Recommendation: For Phase 2 simplicity, `handleOpenProject()` in `MainController` calls `projectContext.openProject(path)` and then calls `this.updateTitle(...)` itself — `ProjectContext` remains a pure state service with no UI dependency.

3. **`JsonNode.asText()` null behavior**
   - What we know: Jackson `JsonNode.asText()` is documented to return empty string `""` when the node is missing, but a `NullNode` returns the string `"null"`.
   - What's unclear: The current `read()` implementation does `ep != null ? ep.asText() : null` — if the JSON has `"entryPoint": null`, `ep` will be a `NullNode` (not Java null), so `ep.asText()` returns `"null"` (the string).
   - Recommendation: Add `!ep.isNull()` check: `ep != null && !ep.isNull() ? ep.asText() : null`.

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `java.io.File` for all I/O | `java.nio.file.Path` + `Files` | Java 7+ | More explicit error handling, `FileAlreadyExistsException` |
| Manual JSON string building | `ObjectMapper.writeValue` | Jackson 1.x → 2.x | Handles escaping, nulls, pretty-print |
| Inline state in Controller | Dedicated state/service class (`ProjectContext`) | JavaFX MVC maturity | Testability, single responsibility |

---

## Sources

### Primary (HIGH confidence)
- Context7 `/websites/openjfx_io_javadoc_21` — `DirectoryChooser`, `TextInputDialog`, `Alert`, `Dialog.showAndWait()`, `MenuItem.disableProperty()`, `BooleanProperty`
- Context7 `/fasterxml/jackson-databind` — `ObjectMapper.writeValue()`, serialization patterns
- Existing source files (read directly): `ProjectConfig.java`, `Project.java`, `ProjectManager.java`, `ProjectFileManager.java`, `MainController.java`, `main.fxml`, `main.css`, `build.gradle`

### Secondary (MEDIUM confidence)
- CONTEXT.md decisions D-01 through D-10 — locked design decisions from discussion phase

### Tertiary (LOW confidence)
- None

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries already on classpath; verified via build.gradle
- Architecture: HIGH — all model classes read directly; patterns verified via Context7 docs
- Pitfalls: HIGH — identified from direct code inspection of existing classes and JavaFX API behavior
- Test infrastructure: MEDIUM — no test directory exists; JUnit Jupiter already declared in build.gradle

**Research date:** 2026-04-15
**Valid until:** 2026-07-15 (JavaFX 21 LTS; Jackson 2.x stable; JDK 21 LTS)
