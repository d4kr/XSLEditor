# Phase 1: JavaFX Application Shell - Context

**Gathered:** 2026-04-14
**Status:** Ready for planning

<domain>
## Phase Boundary

Runnable JavaFX application with main window, split-pane layout skeleton, and Gradle dependencies wired up. No project loading, no file editing — just the shell that all subsequent phases will fill in.

Requirements in scope: APP-01, APP-02, APP-03, APP-04

</domain>

<decisions>
## Implementation Decisions

### Build & Gradle Setup

- **D-01:** Use `org.openjfx.javafxplugin` Gradle plugin for JavaFX configuration — handles module path automatically, one declaration per required JavaFX module.
- **D-02:** Package as fat JAR (using Gradle `shadow` plugin or equivalent) — launchable with `java -jar`, no runtime image required for this internal tool.
- **D-03:** Add RichTextFX and PDFViewerFX dependencies to `build.gradle` alongside JavaFX modules.

### Java Module System

- **D-04:** No `module-info.java` — use traditional classpath with `--add-opens` where required. Simpler to add dependencies, avoids declaring requires for Saxon, FOP, Jackson, etc.

### Main Window Layout

- **D-05:** Layout defined via FXML (`.fxml` file) with a separate controller class — standard JavaFX pattern, maintainable.
- **D-06:** Three-zone layout:
  - Outer: `BorderPane` root — MenuBar top, log panel bottom, center = main split
  - Center: `SplitPane` horizontal — left panel (file tree placeholder), right panel = another `SplitPane` horizontal — editor area (left) + PDF preview area (right)
  - Bottom: Log panel as a collapsible `TitledPane` or `Accordion` — initially collapsed

### Application State at Launch

- **D-07:** Empty panes on startup — all three zones show placeholder/gray areas, menus enabled. No welcome screen, no auto-open dialog. `File > Open Project` is the entry point.

### Window Behavior

- **D-08:** App title: `"XLSEditor"` when no project open, `"XLSEditor — {projectName}"` when project is loaded.
- **D-09:** Close confirmation dialog when unsaved changes exist (scaffold in Phase 1 — actual dirty state is wired up in Phase 4).

### Claude's Discretion

- Exact JavaFX module list required (javafx-controls, javafx-fxml, javafx-web if needed for PDF)
- Specific PDFViewerFX artifact coordinates (org.jpro.web/pdfviewerfx or equivalent)
- Window minimum/preferred size
- CSS stylesheet approach (inline vs external `.css` file)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `docs/PRD.md` §19 — Architecture constraints (Java desktop, JavaFX implied)
- `docs/PRD.md` §12–§14 — File management, preview, and error model specs
- `.planning/REQUIREMENTS.md` §Application Shell — APP-01..04

### Existing codebase
- `build.gradle` — Current Gradle setup (Java 21 toolchain, existing deps to preserve)
- `src/main/java/ch/ti/gagi/xlseditor/` — Existing module structure (no UI yet)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- `PreviewManager` (`src/main/java/ch/ti/gagi/xlseditor/preview/PreviewManager.java`) — Facade that returns a `Preview` DTO with PDF bytes or errors. Phase 1 only scaffolds the preview pane; Phase 6 wires it to this class.
- `LogManager` (`src/main/java/ch/ti/gagi/xlseditor/log/LogManager.java`) — In-memory log. Phase 1 only scaffolds the log panel; Phase 8 wires it.

### Established Patterns

- All backend modules follow single-responsibility (one class = one job). The UI controller should follow the same pattern: `MainController` delegates to service classes, not doing pipeline logic inline.
- Error handling in `RenderOrchestrator.renderSafe()` returns structured results — not exceptions. UI will poll this result.

### Integration Points

- Phase 1 creates the skeleton. Subsequent phases wire into specific pane regions:
  - Phase 3: File tree → left pane
  - Phase 4–5: Editor TabPane → center-left pane
  - Phase 7: PDFViewerFX → center-right pane
  - Phase 8: Log entries → bottom log panel

</code_context>

<specifics>
## Specific Ideas

- No specific visual references given — standard IDE layout (like IntelliJ IDEA split pane approach) is fine.
- Tool is for internal developers, not end users — utility over polish for Phase 1.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within Phase 1 scope.

</deferred>

---

*Phase: 01-javafx-application-shell*
*Context gathered: 2026-04-14*
