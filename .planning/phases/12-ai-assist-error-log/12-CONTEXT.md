# Phase 12: AI Assist in Error Log - Context

**Gathered:** 2026-04-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Add a per-row AI assist action to the existing error log TableView. Clicking the action opens the default browser to ChatGPT with a pre-filled Italian prompt containing the error message. Available for all severity levels (ERROR, WARN, INFO). Must not navigate the editor or alter the error log state.

</domain>

<decisions>
## Implementation Decisions

### Action placement
- **D-01:** Dedicated 5th column added to the TableView, after the Message column. Column is narrow (~40px), no header text (or a short "AI" header). Button rendered via custom `setCellFactory` in `LogController`.

### Prompt format
- **D-02:** Prompt is in Italian. Format: fixed preamble + blank line + raw `LogEntry.message()`.
  ```
  Ho questo errore nel mio progetto XSLT/XSL-FO, puoi aiutarmi?

  {message}
  ```
  The preamble is hardcoded Italian for now. Multi-language prompt preferences are deferred to a future phase.
- **D-03:** Full prompt is URL-encoded and appended to `https://chatgpt.com/?q=`. Opened via `XSLEditorApp.hostServices().showDocument(url)` — same pattern as the About dialog license link.

### Button style
- **D-04:** Small icon button (no text label). Use a chat/AI icon — `💬` as Unicode label or a Glyph if available, otherwise a simple text icon character. Column width ~40px.
- **D-05:** Clicking the button opens the browser. It does NOT select the row, navigate the editor, or modify `allEntries`/`filteredEntries`.

### Claude's Discretion
- Exact Unicode character or icon used for the button (💬, 🤖, or a simple "✦")
- Tooltip text on hover (e.g., "Ask ChatGPT about this error")
- Column header text (empty or "AI")
- Whether the button is styled with a CSS class or left as default

### Deferred: Multi-language prompt
- Language preference for the ChatGPT prompt is Italian now; a future settings phase could allow the user to choose the prompt language.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` §ERR-06 — AI assist button requirement

### Source files (read before planning)
- `src/main/java/ch/ti/gagi/xsleditor/ui/LogController.java` — owns column factories; add 5th column cell factory here
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` — TableView declaration (lines ~98–111); add `<TableColumn fx:id="colAi">` here
- `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` — wires LogController; must pass `colAi` in the `initialize()` call
- `src/main/java/ch/ti/gagi/xsleditor/log/LogEntry.java` — data model; `message()` is the field used for the prompt
- `src/main/java/ch/ti/gagi/xsleditor/XSLEditorApp.java` — `hostServices()` static accessor for `showDocument(url)`

### Prior phase context
- `.planning/phases/11-about-dialog/11-CONTEXT.md` — established `HostServices` pattern (D-05 / license link)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `XSLEditorApp.hostServices().showDocument(url)` — already wired in Phase 11; open browser with this
- `LogController.initialize()` — receives each `TableColumn` as a parameter; add `TableColumn<LogEntry, Void> colAi` as the last parameter and wire the cell factory inside
- `LogEntry.message()` — the string to embed in the ChatGPT URL

### Established Patterns
- Column cell factory pattern: `col.setCellFactory(col -> new TableCell<>() { ... })` — already used for `colLevel` severity coloring; same pattern for the action button
- `TableColumn<LogEntry, Void>` with `setCellFactory` returning a button — standard JavaFX pattern for action columns; no `setCellValueFactory` needed

### Integration Points
- `main.fxml`: add `<TableColumn fx:id="colAi" text="" prefWidth="40" sortable="false"/>` inside the TableView's columns list, after `colMessage`
- `MainController.initialize()`: add `colAi` field (`@FXML TableColumn<LogEntry, Void> colAi`) and pass it to `logController.initialize(..., colAi)`
- `LogController.initialize()`: add `colAi` parameter, call `colAi.setCellFactory(...)` to render the button

</code_context>

<specifics>
## Specific Ideas

- Prompt preamble (Italian, hardcoded): `"Ho questo errore nel mio progetto XSLT/XSL-FO, puoi aiutarmi?\n\n" + entry.message()`
- Target URL: `"https://chatgpt.com/?q=" + URLEncoder.encode(prompt, StandardCharsets.UTF_8).replace("+", "%20")`

</specifics>

<deferred>
## Deferred Ideas

- Multi-language prompt preference setting — future settings/preferences phase
- Support for other AI providers (Copilot, Gemini) — out of scope per REQUIREMENTS.md

</deferred>

---

*Phase: 12-ai-assist-error-log*
*Context gathered: 2026-04-22*
