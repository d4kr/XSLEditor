# Phase 3: File Tree View - Context

**Gathered:** 2026-04-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Flat file tree in left panel showing all files in project root. Visual distinction for entrypoint (▶ green) and XML input (■ blue). Also fully implements Set Entrypoint / Set XML Input actions — scaffolded as disabled stubs in Phase 2.

Requirements in scope: TREE-01, TREE-02, TREE-03, TREE-04 (and PROJ-02, PROJ-03, PROJ-04, PROJ-05 deferred from Phase 2).

</domain>

<decisions>
## Implementation Decisions

### Set Entrypoint / Set XML Input — wiring

- **D-01:** Immediate write, no confirmation dialog. Select file in tree → click Set Entrypoint → `ProjectContext` updates entrypoint + writes `.xslfo-tool.json` → tree cell re-renders with green glyph. Matches PROJ-04 directly.
- **D-02:** Show 3-second transient status label after write (consistent with Phase 2 "Project opened" pattern): `"Entrypoint set: filename.xsl"` / `"XML input set: filename.xml"`.
- **D-03:** No file-type validation on selection. Internal developer tool — any file can be set as entrypoint or XML input. User knows their project.
- **D-04:** Menu items enabled only when a tree item is selected. Bind `menuItemSetEntrypoint.disableProperty()` and `menuItemSetXmlInput.disableProperty()` to `TreeView.selectionModel.selectedItemProperty().isNull()` AND `projectLoaded.not()` (both conditions must be false). Disabled when no project open or nothing selected.

### Double-click → editor (Phase 4 stub)

- **D-05:** Phase 3 defines a `Consumer<Path>` callback (or equivalent) in `FileTreeController` — named `onFileOpenRequest`. Double-click fires the callback. Phase 3 sets a no-op default (null-safe). Phase 4 sets the actual handler (tab open). Zero user-visible behavior in Phase 3, but clean integration seam.

### Tree refresh architecture

- **D-06:** `ProjectContext` exposes an `ObservableList<Path>` of files in the project root (`projectFilesProperty()` or equivalent). `FileTreeController` binds to this list. `createFile()` adds the new path to the list → tree updates automatically. Follows JavaFX reactive pattern, no cross-controller coupling.
- **D-07:** On project load, `ProjectContext.openProject()` populates the `ObservableList` from the directory. On `createFile()`, the new path is appended to the list.

### FileTree controller separation

- **D-08:** Dedicated `FileTreeController` class. Owns: cell factory, selection listener, ObservableList binding, `onFileOpenRequest` callback wiring. `MainController` holds a `FileTreeController` reference and calls `fileTreeController.initialize(fileTreePane, projectContext)` from its own `initialize()`. Follows single-responsibility convention (Phase 1 CONTEXT: "MainController delegates to service classes, not doing pipeline logic inline").
- **D-09:** `FileTreeController` receives `menuItemSetEntrypoint` and `menuItemSetXmlInput` references so it can wire enable/disable bindings and action handlers (or MainController passes a callback — implementation detail left to Claude).

### Visual / UI

- **D-10:** All visual decisions locked in `03-UI-SPEC.md`. `FileItem` record with `FileRole` enum (`ENTRYPOINT`, `XML_INPUT`, `REGULAR`). `FileItemTreeCell` custom cell with glyph prefix + color per UI-SPEC table. CSS classes in `main.css` per UI-SPEC.
- **D-11:** Panel layout: when project is loaded, replace placeholder Label in `fileTreePane` with a `VBox` containing a header Label (project dir name, 12px) + `TreeView<FileItem>`. When no project loaded, preserve Phase 1 placeholder Label unchanged.
- **D-12:** Tree `showRoot = false`. Root label = project directory name (for accessibility). Children = one `TreeItem<FileItem>` per file in root directory (flat, no subdirectories).

### Claude's Discretion

- Exact class names for FileItem, FileItemTreeCell, FileTreeController (align with existing naming conventions — PascalCase, `final` where possible)
- Whether `onFileOpenRequest` is a `Consumer<Path>`, a functional interface, or a property
- Exact method signature for `projectFilesProperty()` on `ProjectContext`
- How `FileTreeController` receives menu item references (constructor injection vs. setter)
- Order of files in tree (directory listing order vs. alphabetical)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` §File Tree — TREE-01..04 (exact acceptance criteria)
- `.planning/REQUIREMENTS.md` §Project Management — PROJ-02..05 (Set Entrypoint/XML Input write-back, deferred from Phase 2)

### UI Design Contract (MANDATORY — all visual decisions locked here)
- `.planning/phases/03-file-tree-view/03-UI-SPEC.md` — FileItem model, FileItemTreeCell glyphs + colors, panel layout, CSS classes, state matrix, interaction contracts

### Existing code (MUST read before modifying)
- `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` — Integration points: `fileTreePane` (fx:id StackPane), `menuItemSetEntrypoint`, `menuItemSetXmlInput`, `showTransientStatus()` pattern
- `src/main/java/ch/ti/gagi/xsleditor/ui/ProjectContext.java` — Current state service (needs ObservableList<Path> added, plus setEntrypoint/setXmlInput methods)
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` — Current FXML (fileTreePane StackPane, menu item fx:ids)
- `src/main/java/ch/ti/gagi/xsleditor/model/ProjectManager.java` — loadProject() — review before adding config write-back
- `src/main/java/ch/ti/gagi/xsleditor/model/ProjectConfig.java` — Record structure (write() needed for PROJ-04)

### Prior phase context
- `.planning/phases/02-project-management/02-CONTEXT.md` — D-04/D-05: Set Entrypoint/XML Input deferred to Phase 3; D-06: ProjectContext owns project state
- `.planning/phases/01-javafx-application-shell/01-CONTEXT.md` — D-05/D-06: FXML + controller pattern; fileTreePane integration point defined

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ProjectContext` — already holds `currentProject` and `projectLoaded` BooleanProperty; needs `ObservableList<Path> projectFiles` + `setEntrypoint(Path)` + `setXmlInput(Path)` methods
- `MainController.showTransientStatus(String)` — 3-second fade pattern; reuse for Set Entrypoint/XML Input feedback (D-02)
- `ProjectConfig` (record) — already reads from JSON via Jackson; needs `write(Path rootPath)` method for PROJ-04 write-back
- `ProjectManager.loadProject()` — wraps config read + Project construction; review for file listing

### Established Patterns
- Single-responsibility: all UI controllers are `final` classes that delegate to service classes (ProjectContext, etc.)
- JavaFX FXML + controller: `@FXML` injection for all FXML-declared nodes
- Jackson ObjectMapper already used in `ProjectConfig.read()` — reuse for write
- `BooleanProperty` + `.bind()` for menu enable/disable state (pattern established in Phase 2 `menuItemNewFile`)
- `PauseTransition` for transient status messages (Phase 2)

### Integration Points
- `fileTreePane` (StackPane, `fx:id`) — Phase 3 replaces contents with VBox + TreeView when project loads
- `menuItemSetEntrypoint`, `menuItemSetXmlInput` — Phase 3 wires enable/disable bindings and action handlers
- `editorPane` (StackPane, `fx:id`) — Phase 4 integration point; Phase 3 fires `onFileOpenRequest` callback only
- `ProjectContext.projectLoadedProperty()` — already observable; `FileTreeController` can observe for tree initialization

</code_context>

<specifics>
## Specific Ideas

- No specific UX references — standard IDE-style file tree behavior is correct (think VS Code file explorer, minimal chrome)
- Tool is for internal developers; utility over polish

</specifics>

<deferred>
## Deferred Ideas

- Right-click context menu on tree items (Set Entrypoint/XML Input from tree directly) — UI-SPEC explicitly defers to post-Phase 3
- File rename / delete from tree — out of scope (PROJECT.md Out of Scope)
- Subdirectory support — Phase 3 is flat tree only (TREE-01: "project root directory")

</deferred>

---

*Phase: 03-file-tree-view*
*Context gathered: 2026-04-16*
