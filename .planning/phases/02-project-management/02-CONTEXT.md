# Phase 2: Project Management - Context

**Gathered:** 2026-04-15
**Status:** Ready for planning

<domain>
## Phase Boundary

User can open a project directory via "Open Project" menu, which loads `.xslfo-tool.json` (if present) and restores entrypoint + XML input. Phase 2 delivers: Open Project action, config read/write, title update, and New File dialog. It does NOT include the file tree (Phase 3) or editor (Phase 4-5). "Set Entrypoint" and "Set XML Input" actions are scaffolded but disabled — Phase 3 enables them contextually from the tree.

Requirements in scope: PROJ-01, PROJ-02, PROJ-03, PROJ-04, PROJ-05, PROJ-06

</domain>

<decisions>
## Implementation Decisions

### Config loading — missing or partial config

- **D-01:** When a directory without `.xslfo-tool.json` is opened, load the project with `entryPoint=null` and `xmlInput=null`. Do NOT create the file immediately.
- **D-02:** `.xslfo-tool.json` is created (or updated) only when the user actively sets entrypoint or XML input. No file = no config state — this is valid.
- **D-03:** `ProjectConfig` must be relaxed to allow partial state: both `entryPoint` and `xmlInput` are optional on read (nullable). The constraint "both required" moves to the render layer (REND-02 already disables render when either is unset).

### Set Entrypoint / Set XML Input — Phase 2 scope

- **D-04:** "Set Entrypoint" and "Set XML Input" menu/toolbar actions are created in Phase 2 but remain **disabled** (grayed out). They have no functional implementation in this phase.
- **D-05:** Phase 3 (file tree) enables these actions and implements selection from the selected tree node. Phase 2 only scaffolds the actions with correct disabled state.

### Project state management

- **D-06:** Project state (loaded `Project` object, nullable entrypoint, nullable xmlInput) lives in a `ProjectContext` class (or equivalent service), not inline in `MainController`. `MainController` delegates to `ProjectContext` for all project-related state.
- **D-07:** On project open: call `MainController.updateTitle(projectName)` with the directory name.

### New File dialog (PROJ-06)

- **D-08:** "New File" dialog accepts any filename (free input, no extension restriction). Internal developer tool — no validation on extension.
- **D-09:** After file creation: write an empty file to the project root directory. No auto-open in editor (editor does not exist in Phase 2). Visual confirmation comes in Phase 3 when the tree refreshes.
- **D-10:** "New File" is a menu action under File menu. Disabled when no project is open.

### Claude's Discretion

- Exact class name: `ProjectContext` or `AppController` (either is fine per roadmap)
- How `ProjectConfig.write()` / save method is added (method on record, static helper, or via `ProjectManager`)
- Menu placement: exact menu items and keyboard shortcuts for "Open Project" and "New File"
- Error handling UX for invalid directories (no files, permission denied)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` §Project Management — PROJ-01..PROJ-06 (exact acceptance criteria)
- `docs/PRD.md` — Product requirements and non-goals

### Existing model classes (MUST read before modifying)
- `src/main/java/ch/ti/gagi/xlseditor/model/ProjectConfig.java` — Current record structure (needs relaxation for D-03)
- `src/main/java/ch/ti/gagi/xlseditor/model/Project.java` — Domain object
- `src/main/java/ch/ti/gagi/xlseditor/model/ProjectManager.java` — Current load logic
- `src/main/java/ch/ti/gagi/xlseditor/model/ProjectFileManager.java` — File save/load utilities
- `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` — Integration point (updateTitle, FXML slots)

### Phase 1 output (build on this)
- `.planning/phases/01-javafx-application-shell/01-CONTEXT.md` — Layout decisions and integration points defined in Phase 1

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ProjectConfig` (record) — already reads from JSON via Jackson; needs `read()` relaxed to allow nulls and a `write()` method added
- `Project` — domain object already holds rootPath + entryPoint + xmlInput; Phase 2 populates it via UI
- `ProjectManager.loadProject()` — wraps config read + Project construction; needs variant for "no config" case
- `ProjectFileManager.save()` — can be used for New File creation (write empty content to new path)
- `MainController.updateTitle()` — already implemented in Phase 1, called after project load

### Established Patterns
- Phase 1 used FXML + controller pattern; Phase 2 follows same pattern (no new FXML files needed — actions wired to existing MainController or a new ProjectContext)
- JavaFX `DirectoryChooser` for Open Project (standard JavaFX pattern, no external dep)
- Jackson `ObjectMapper` already used in `ProjectConfig.read()` — reuse for write

### Integration Points
- `MainController.fileTreePane` (Phase 3), `MainController.editorPane` (Phase 4-5) — Phase 2 does NOT touch these
- `MainController.updateTitle(String)` — called after project open (already implemented)
- `MainController.setDirty(boolean)` — Phase 2 does NOT need to touch dirty state (no editor yet)
- FXML MenuBar already scaffold — Phase 2 adds handlers to existing File menu items or creates new ones

</code_context>

<specifics>
## Specific Ideas

- No specific UX references given — standard JavaFX dialog patterns are fine
- Tool is internal for developers — minimal polish, functionality first

</specifics>

<deferred>
## Deferred Ideas

- "Set Entrypoint" / "Set XML Input" functional implementation → Phase 3 (file tree selection)
- Auto-open new file in editor after creation → Phase 4 (editor TabPane)
- Extension validation on New File → not needed (developer tool, free input)

</deferred>

---

*Phase: 02-project-management*
*Context gathered: 2026-04-15*
