# Phase 8: Error & Log Panel - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-20
**Phase:** 08-error-log-panel
**Areas discussed:** Component, LogEntry model, Data flow, Auto-expand, Filter bar placement

---

## Component

| Option | Description | Selected |
|--------|-------------|----------|
| TableView | Native column support — timestamp / severity / type / message. Sortable. Fits ERR-02 exactly. | ✓ |
| ListView + custom cells | Single-row cards with inline severity badge. More layout flexibility, manual CSS for column alignment. | |

**User's choice:** TableView
**Notes:** —

---

## LogEntry model

| Option | Description | Selected |
|--------|-------------|----------|
| Extend LogEntry | Add optional type, file, line fields to LogEntry. Single model. Small backend change. | ✓ |
| UI-only wrapper | Keep LogEntry minimal. Introduce a LogPanelEntry (UI layer only). LogManager untouched. | |

**User's choice:** Extend LogEntry
**Notes:** —

---

## Data flow

| Option | Description | Selected |
|--------|-------------|----------|
| LogController owns list | RenderController gets Consumer<List<PreviewError>> callback. LogController processes entries and owns ObservableList. Clean separation, same pattern as pdfCallback/outdatedCallback. | ✓ |
| RenderController writes directly | RenderController gets a reference to LogController and calls logController.setEntries(...). More direct, slightly more coupling. | |
| Observable LogManager | Change LogManager to back its list with ObservableList<LogEntry>. UI binds directly. Cleanest long term, modifies backend class. | |

**User's choice:** LogController owns list
**Notes:** —

---

## Auto-expand

| Option | Description | Selected |
|--------|-------------|----------|
| Auto-expand on ERROR/WARN | TitledPane.setExpanded(true) when render produces errors or warnings. Stays collapsed for INFO-only logs. | ✓ |
| Manual only | Never auto-expand. User opens the panel when they want to. No surprise layout shifts. | |
| Auto-expand always | Expand on every render. Always visible. More intrusive. | |

**User's choice:** Auto-expand on ERROR/WARN
**Notes:** —

---

## Filter bar placement

| Option | Description | Selected |
|--------|-------------|----------|
| Above table, inside TitledPane | HBox with ToggleButtons above TableView, both inside TitledPane. Standard log viewer layout. | ✓ |
| In TitledPane header line | Filter buttons inline with 'Log' title. Space-efficient, trickier FXML (graphic slot). | |

**User's choice:** Above table, inside TitledPane
**Notes:** —

---

## Claude's Discretion

- Exact CSS for severity badge colors
- Column widths and prefWidth values in FXML
- LogController method signatures for INFO message routing
- LogEntry constructor overloads vs builder for optional fields
- ToggleButton CSS styling details
