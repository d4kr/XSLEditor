---
phase: 01-javafx-application-shell
plan: "01"
subsystem: ui
tags: [javafx, fxml, gradle, shadow, richtextfx, webview]

# Dependency graph
requires: []
provides:
  - JavaFX application shell with BorderPane three-zone layout
  - XLSEditorApp entry point (fat JAR, shadowJar)
  - MainController with updateTitle(), setDirty(), close-request dialog
  - fx:id anchors: fileTreePane, editorPane, previewPane, previewWebView, logListView
  - Gradle build with JavaFX plugin, shadow plugin, RichTextFX, javafx.web
affects:
  - 01-02  # Phase 2: project management wires into menuBar and updateTitle()
  - 01-03  # Phase 3: file tree replaces fileTreePane
  - 01-04  # Phase 4-5: editor replaces editorPane, uses richtextfx
  - 01-07  # Phase 7: PDF preview wires previewWebView
  - 01-08  # Phase 8: log panel wires logListView

# Tech tracking
tech-stack:
  added:
    - org.openjfx.javafxplugin 0.1.0 (javafx.controls, javafx.fxml, javafx.web)
    - com.gradleup.shadow 9.0.0-beta12 (fat JAR packaging)
    - org.fxmisc.richtext:richtextfx:0.11.5
  patterns:
    - FXMLLoader pattern: XLSEditorApp.start() loads main.fxml, retrieves controller, calls setPrimaryStage()
    - Controller API pattern: MainController exposes public methods for downstream phases
    - PDF preview via WebView: file:// URI loaded into previewWebView, no external library needed

key-files:
  created:
    - build.gradle
    - settings.gradle
    - src/main/java/ch/ti/gagi/xlseditor/XLSEditorApp.java
    - src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java
    - src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml
    - src/main/resources/ch/ti/gagi/xlseditor/ui/main.css
  modified: []

key-decisions:
  - "Shadow plugin migrated to com.gradleup.shadow 9.0.0-beta12: original com.github.johnrengelman.shadow 8.1.1 fails on Gradle 9 with MissingPropertyException on 'mode' property"
  - "WebView PDF preview approach adopted: PDFViewerFX is not on Maven Central; JavaFX WebView loads file:// URIs from temp files — satisfies Phase 7 requirements natively"
  - "No module-info.java: traditional classpath approach per D-04 decision"

patterns-established:
  - "FXMLLoader: XLSEditorApp.start() loads /ch/ti/gagi/xlseditor/ui/main.fxml from classpath"
  - "Controller lifecycle: setPrimaryStage() called after FXMLLoader.getController() to register close handler"
  - "Title format: 'XLSEditor' baseline, 'XLSEditor — {projectName}' when project is loaded (em-dash U+2014)"
  - "Dirty flag: setDirty(true) triggers confirmation dialog on window close"

requirements-completed: [APP-01, APP-02, APP-03, APP-04]

# Metrics
duration: 3min
completed: 2026-04-14
---

# Phase 1 Plan 01: JavaFX Application Shell Summary

**JavaFX 21 shell with BorderPane three-zone layout, fat JAR via shadow plugin, WebView PDF preview scaffold, and close-confirmation dirty-state pattern**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-04-14T21:29:22Z
- **Completed:** 2026-04-14T21:31:42Z
- **Tasks:** 6 (5 code + 1 verification)
- **Files modified:** 6

## Accomplishments
- Runnable fat JAR built via `./gradlew shadowJar` — produces `build/libs/xlseditor-1.0.0.jar` (60MB including JavaFX natives)
- Three-zone layout with fx:id anchors ready for Phase 3 (file tree), Phase 4-5 (editor), Phase 7 (PDF), Phase 8 (log)
- WebView PDF preview approach adopted — no additional dependency needed; Phase 7 wires actual PDF loading
- Close-confirmation scaffold wired via `setDirty()` / `handleCloseRequest()`

## Task Commits

1. **Task 1: build.gradle + settings.gradle** - `e99c259` (chore)
2. **Tasks 2-5: Application shell source files** - `d0ad1d4` (feat)
3. **Task 6 fix: shadow plugin compatibility** - `259ba04` (fix)

## Files Created/Modified

- `build.gradle` - JavaFX plugin, shadow plugin (gradleup 9.0.0-beta12), RichTextFX, shadowJar config
- `settings.gradle` - pluginManagement block, rootProject.name
- `src/main/java/ch/ti/gagi/xlseditor/XLSEditorApp.java` - Application entry point, FXMLLoader, min window 900x600
- `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` - FXML controller: updateTitle(), setDirty(), handleCloseRequest()
- `src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml` - BorderPane with MenuBar, SplitPanes, TitledPane log
- `src/main/resources/ch/ti/gagi/xlseditor/ui/main.css` - Dark placeholder styling, divider and log panel styles

## Decisions Made

- **WebView for PDF preview:** PDFViewerFX (`org.jpro.web:pdfviewerfx`) is not on Maven Central. Adopted JavaFX WebView loading `file://` URIs from temp files — WebKit renders PDFs natively on all major platforms. Placeholder WebView (`fx:id="previewWebView"`) is already in the FXML for Phase 7 to wire.
- **Shadow plugin fork:** `com.github.johnrengelman.shadow` 8.1.1 is incompatible with Gradle 9.x (`MissingPropertyException: mode`). Migrated to `com.gradleup.shadow` 9.0.0-beta12, the maintained fork for Gradle 9.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Shadow plugin version incompatible with Gradle 9**
- **Found during:** Task 6 (shadowJar build verification)
- **Issue:** `com.github.johnrengelman.shadow 8.1.1` throws `MissingPropertyException: No such property: mode` when running `shadowJar` under Gradle 9.5.0-rc-2
- **Fix:** Replaced with `com.gradleup.shadow 9.0.0-beta12` — the maintained Gradle 9-compatible fork
- **Files modified:** `build.gradle`
- **Verification:** `./gradlew clean shadowJar` exits 0, produces `build/libs/xlseditor-1.0.0.jar` (60MB)
- **Committed in:** `259ba04`

---

**Total deviations:** 1 auto-fixed (Rule 1 - build bug)
**Impact on plan:** Necessary compatibility fix. No scope creep. All must-haves satisfied.

## Issues Encountered

- Shadow plugin 8.1.1 / Gradle 9 incompatibility — resolved via plugin migration (see Deviations above)

## fx:id Anchors Available for Downstream Phases

| fx:id | Type | Phase |
|-------|------|-------|
| `fileTreePane` | StackPane | Phase 3: replace Label with TreeView |
| `editorPane` | StackPane | Phase 4-5: replace Label with TabPane/CodeArea |
| `previewPane` | StackPane | Phase 7: drive previewWebView |
| `previewWebView` | WebView | Phase 7: load file:// PDF URI |
| `logListView` | ListView<String> | Phase 8: bind to LogManager observable list |
| `menuBar` | MenuBar | Phase 2: add Open Project menu item |
| `logPane` | TitledPane | Phase 8: expand programmatically on new entries |

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Fat JAR is runnable: `java -jar build/libs/xlseditor-1.0.0.jar` launches the shell window
- All pane fx:id anchors are in place for Phase 2 (project management) and beyond
- RichTextFX dependency declared and resolvable — Phase 4-5 can use it immediately
- WebView declared in FXML — Phase 7 can wire PDF loading without FXML changes
- No blockers for Phase 2

## Self-Check: PASSED

All files present, all commits verified.

---
*Phase: 01-javafx-application-shell*
*Completed: 2026-04-14*
