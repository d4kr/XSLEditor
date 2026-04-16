# Phase 3: File Tree View - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-16
**Phase:** 03-file-tree-view
**Areas discussed:** Set Entrypoint/XML Input wiring, Double-click stub, Tree refresh architecture, FileTree controller separation

---

## Set Entrypoint / XML Input wiring

| Option | Description | Selected |
|--------|-------------|----------|
| Immediate write, no confirm | Write .xslfo-tool.json on click, no dialog | ✓ |
| Immediate write + status | Same + 3-second status label feedback | (implicit follow-up) ✓ |
| Confirm dialog before writing | Modal dialog before persisting | |

**Validation question:**

| Option | Description | Selected |
|--------|-------------|----------|
| No validation — allow any file | Internal tool, user knows project | ✓ |
| Warn on .xml for entrypoint | Alert for likely-wrong selection | |
| Block non-.xsl/.xslt | Hard enforcement | |

**Enable state question:**

| Option | Description | Selected |
|--------|-------------|----------|
| Enabled when tree file is selected | Bind to selectedItem != null | ✓ |
| Enabled when project loaded | Always clickable, error if no selection | |

**User's choices:** Immediate write (recommended default). No file-type validation. Menu enabled only when tree item is selected.

---

## Double-click stub (Phase 4 not built yet)

| Option | Description | Selected |
|--------|-------------|----------|
| Define callback interface, no-op | Consumer<Path> or equivalent; Phase 4 sets handler | ✓ |
| Log to status label as stub | 3-second "Opening: X" feedback | |
| Log to log panel | Adds entry to log ListView | |

**User's choice:** Clean callback seam (recommended). No visible behavior in Phase 3.

---

## Tree refresh architecture

| Option | Description | Selected |
|--------|-------------|----------|
| ProjectContext ObservableList<Path> | Reactive binding; tree updates automatically | ✓ |
| MainController calls refreshTree() | Explicit call after createFile() | |
| FileWatcher / WatchService | Background thread watches filesystem | |

**User's choice:** ObservableList approach (recommended). Follows JavaFX reactive pattern.

---

## FileTree controller separation

| Option | Description | Selected |
|--------|-------------|----------|
| Separate FileTreeController class | Single-responsibility, MainController delegates | ✓ |
| All in MainController | Simpler but MainController grows large | |

**User's choice:** Dedicated FileTreeController (recommended). Consistent with Phase 1 convention.

---

## Claude's Discretion

- Exact class names (FileItem, FileItemTreeCell, FileTreeController)
- Whether onFileOpenRequest is Consumer<Path> or a custom interface
- Method signature for projectFilesProperty()
- FileTreeController receives menu item references via constructor or setter
- File order in tree (directory listing order vs. alphabetical)

## Deferred Ideas

- Right-click context menu on tree items → post-Phase 3 (UI-SPEC explicit deferral)
- File rename/delete from tree → out of scope (PROJECT.md)
- Subdirectory support → Phase 3 flat tree only
