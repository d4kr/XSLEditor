# Phase 11: About Dialog - Context

**Gathered:** 2026-04-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Add a Help menu to the menu bar and an About dialog reachable from it. The dialog shows app version, runtime stack versions (Java, Saxon-HE, FOP, JavaFX), author/credits, and license info. No new windows — modal dialog only. Also corrects build.gradle version from the erroneous '1.0.0' to the correct '0.1.0' (0.x schema adopted; 0.2.0 will be the next milestone).

</domain>

<decisions>
## Implementation Decisions

### Version source
- **D-01:** Version string is injected at build time by Gradle from `build.gradle` into `src/main/resources/version.properties`. The dialog reads `version.properties` at runtime via `getClass().getResourceAsStream(...)`. No hardcoding — version stays in sync with the build automatically.
- **D-02:** Build.gradle version is corrected from `'1.0.0'` to `'0.1.0'` in this phase (0.x versioning schema; v1.0 MVP retroactively becomes 0.1.0).

### Dialog layout
- **D-03:** Single-panel programmatic dialog (`Dialog<Void>`, same pattern as `SearchDialog`). No FXML. Layout is a `VBox` with distinct sections:
  1. App title + version (e.g. `XLSEditor v0.1.0`)
  2. `Separator`
  3. Runtime Stack label + `GridPane` (Java / Saxon-HE / FOP / JavaFX — label + version per row)
  4. `Separator`
  5. Author/credits line
  6. License line (short text + `Hyperlink` opening browser)
- **D-04:** Dialog is opened with `initOwner(primaryStage)` and `initModality(APPLICATION_MODAL)`, same as existing SearchDialog pattern.

### License display
- **D-05:** One short line of license text (e.g. "Licensed under the Apache License 2.0") followed by a `Hyperlink` that opens the canonical Apache 2.0 URL in the default browser via `getHostServices().showDocument(url)`.

### Help menu wiring
- **D-06:** A `<Menu text="Help">` is added to `main.fxml` (after the Search menu). It contains one `<MenuItem text="About XLSEditor..." onAction="#handleAbout"/>`. The handler `handleAbout()` is added to `MainController`.

### Claude's Discretion
- Exact CSS styling of the dialog (padding, font sizes, separator color)
- How to retrieve Saxon-HE version at runtime (e.g. `net.sf.saxon.Version.getProductVersion()` or from classpath manifest)
- How to retrieve FOP version at runtime (e.g. `org.apache.fop.Version.getVersion()` or hardcoded fallback if API unavailable)
- Whether to add a small app icon/logo in the dialog header

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` §ABOUT-01..05 — full About dialog requirements

### Source files (read before planning)
- `src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml` — menu bar to extend with Help menu
- `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` — add `handleAbout()` handler here
- `src/main/java/ch/ti/gagi/xlseditor/ui/SearchDialog.java` — reference dialog implementation pattern (Dialog<Void>, programmatic, initOwner, initModality)
- `src/main/java/ch/ti/gagi/xlseditor/XLSEditorApp.java` — `APP_NAME` constant already defined here
- `build.gradle` — version field to update from '1.0.0' to '0.1.0'; add processResources copy task for version.properties

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `SearchDialog extends Dialog<Void>` — exact dialog lifecycle pattern to follow (initOwner, initModality, DialogPane, ButtonType.CLOSE)
- `XLSEditorApp.APP_NAME` — already defined as `"XLSEditor"`, use in dialog title
- `MainController.primaryStage` — already stored as field; pass to AboutDialog constructor for initOwner

### Established Patterns
- Programmatic dialog construction (no FXML) — SearchDialog is the canonical example
- Handler method naming in MainController: `#handleXxx` in FXML → `@FXML private void handleXxx()` in Java
- `getHostServices().showDocument(url)` available from `Application` subclass for opening browser — XLSEditorApp must expose it (or pass it down)

### Integration Points
- `main.fxml`: add `<Menu text="Help">` after `<Menu text="Search">`
- `MainController`: add `@FXML private void handleAbout()` that instantiates `AboutDialog` and calls `showAndWait()`
- `build.gradle`: add `processResources { ... }` block to write `version.properties` into resources
- New file: `src/main/java/ch/ti/gagi/xlseditor/ui/AboutDialog.java`

</code_context>

<specifics>
## Specific Ideas

- Dialog mockup confirmed by user:
  ```
  XLSEditor  v0.1.0
  ─────────────────────
  Runtime Stack
   Java      21.0.3
   Saxon-HE  12.4
   FOP       2.9
   JavaFX    21
  ─────────────────────
  Author: ...
  License: Apache 2.0  [link]
  ```
- Version schema: 0.x — MVP was 0.1.0, next milestone (v1.1 with About + AI assist) will be 0.2.0

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 11-about-dialog*
*Context gathered: 2026-04-22*
