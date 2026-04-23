# Phase 16: Log Panel Layout - Context

**Gathered:** 2026-04-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Fix the log panel TableView so it fills 100% of its container width, eliminates the phantom filler column at the right edge, and keeps all five columns at readable widths when the window is resized to minimum size. No new features, no new columns, no LogController logic changes.

</domain>

<decisions>
## Implementation Decisions

### Resize Policy
- **D-01:** Fix applied in `main.fxml` only — no Java code changes for layout. All sizing declared declaratively.
- **D-02:** Set `columnResizePolicy="CONSTRAINED_RESIZE_POLICY"` on `logTableView`. This eliminates the phantom column (LOG-02) and forces columns to fill full width (LOG-01).

### Column Sizing
- **D-03:** `colTime` — fixed: `minWidth="65" maxWidth="65" prefWidth="65"`. Must show "HH:mm:ss" without truncation.
- **D-04:** `colLevel` — fixed: `minWidth="60" maxWidth="60" prefWidth="60"`. Must show "ERROR" without truncation.
- **D-05:** `colType` — flexible: `minWidth="80" maxWidth="120" prefWidth="100"`. Slight flex acceptable.
- **D-06:** `colMessage` — absorbs remaining width: `minWidth="120" maxWidth="1.7976931348623157E308" prefWidth="400"`. This is the only column that flexes at wider widths.
- **D-07:** `colAi` (AI button column) — fixed: `minWidth="40" maxWidth="40" prefWidth="40"`. Button needs stable width.

### Claude's Discretion
- Whether to add any CSS rules in `main.css` for column header or cell alignment (not discussed — implement as needed).
- Whether `text` attributes on columns need updating (currently: Time, Level, Type, Message, "").

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Layout to Modify
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` — TableView definition with current column widths (no columnResizePolicy set)

### Controller (read-only reference — no changes needed)
- `src/main/java/ch/ti/gagi/xsleditor/ui/LogController.java` — Column wiring and cell factories; must remain compatible with FXML column IDs

### Requirements
- `.planning/REQUIREMENTS.md` — LOG-01, LOG-02, LOG-03 (full-width, no phantom column, readable at narrow widths)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `logTableView` in FXML: already uses `VBox.vgrow="ALWAYS"` — height expansion is fine; width expansion is the gap.
- Columns already have `sortable="false"` — no sorting concerns.
- `colAi` has no text and carries a Button graphic — fixed width critical to avoid button clipping.

### Established Patterns
- Phase 15 CSS fixes went into `main.css` only. This phase targets `main.fxml` only — consistent pattern of single-file fixes.
- No `columnResizePolicy` is currently set anywhere in the FXML → default `UNCONSTRAINED_RESIZE_POLICY` is the root cause of both LOG-01 and LOG-02.

### Integration Points
- `LogController.initialize()` receives the TableColumn references from MainController and configures cell value/cell factories. Column fx:id values must remain unchanged.
- `main.fxml` `<TableView>` is the only file to edit.

</code_context>

<specifics>
## Specific Ideas

- User confirmed the exact FXML diff during discussion:
  ```xml
  <TableView fx:id="logTableView"
    columnResizePolicy="CONSTRAINED_RESIZE_POLICY"
    prefHeight="120" VBox.vgrow="ALWAYS">
    <columns>
      <TableColumn fx:id="colTime"    prefWidth="65"  minWidth="65"  maxWidth="65"  sortable="false" text="Time"/>
      <TableColumn fx:id="colLevel"   prefWidth="60"  minWidth="60"  maxWidth="60"  sortable="false" text="Level"/>
      <TableColumn fx:id="colType"    prefWidth="100" minWidth="80"  maxWidth="120" sortable="false" text="Type"/>
      <TableColumn fx:id="colMessage" prefWidth="400" minWidth="120" maxWidth="1.7976931348623157E308" sortable="false" text="Message"/>
      <TableColumn fx:id="colAi"      prefWidth="40"  minWidth="40"  maxWidth="40"  sortable="false" text=""/>
    </columns>
  </TableView>
  ```

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 16-log-panel-layout*
*Context gathered: 2026-04-23*
