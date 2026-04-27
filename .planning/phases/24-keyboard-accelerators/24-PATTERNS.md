# Phase 24: Keyboard Accelerators — Pattern Map

**Mapped:** 2026-04-27
**Files analyzed:** 1 (modified only)
**Analogs found:** 2 / 1 (two existing accelerator patterns in the same file)

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` | view/FXML | request-response | Same file, lines 32–39 (existing accelerators) | exact |

---

## Pattern Assignments

### `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` (view, request-response)

**Analog:** Same file — two existing `MenuItem` accelerator declarations, lines 32–39.

**Existing accelerator pattern — single-key (line 32–35):**
```xml
<MenuItem fx:id="menuItemRender"
          text="Render"
          accelerator="F5"
          onAction="#handleRender"/>
```

**Existing accelerator pattern — modifier+key with `Ctrl+` (line 38–39):**
```xml
<MenuItem fx:id="findInFilesMenuItem" text="Find in Files"
          accelerator="Ctrl+Shift+F" onAction="#handleFindInFiles"/>
```

> NOTE: `Ctrl+Shift+F` is technically macOS-incorrect. Phase 24 uses `Shortcut+` for all new
> entries to remain cross-platform (Cmd on macOS, Ctrl on Windows/Linux).

**Current state of the five target MenuItems (lines 20–27):**
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

**Target state after Phase 24 (add `accelerator` attribute only — no other change):**
```xml
<!-- KBD-01 -->
<MenuItem fx:id="menuItemOpenProject"
          text="Open Project..."
          accelerator="Shortcut+O"
          onAction="#handleOpenProject"/>
<SeparatorMenuItem/>
<!-- KBD-04 — no onAction in FXML; handler bound in FileTreeController.initialize() -->
<MenuItem fx:id="menuItemSetEntrypoint"
          text="Set Entrypoint"
          accelerator="Shortcut+Shift+E"/>
<!-- KBD-05 — no onAction in FXML; handler bound in FileTreeController.initialize() -->
<MenuItem fx:id="menuItemSetXmlInput"
          text="Set XML Input"
          accelerator="Shortcut+Shift+I"/>
<SeparatorMenuItem/>
<!-- KBD-02 -->
<MenuItem fx:id="menuItemNewFile"
          text="New File..."
          accelerator="Shortcut+N"
          onAction="#handleNewFile"/>
<SeparatorMenuItem/>
<!-- KBD-03 — Exit has no fx:id; accelerator works without it -->
<MenuItem text="Exit"
          accelerator="Shortcut+Q"
          onAction="#handleExit"/>
```

**Inline format rule:** Items that already fit on one line (e.g. `findInFilesMenuItem`) may stay
inline; multi-attribute items should be expanded to one attribute per line (see `menuItemRender`
pattern, lines 32–35). Apply the multi-line style for all five new entries for readability.

---

## Shared Patterns

### Accelerator Token Convention
**Source:** `main.fxml` lines 32–39 (existing items) + JavaFX 21 `KeyCombination.valueOf` docs
**Apply to:** All five new MenuItem additions in Phase 24

| Use | Avoid | Reason |
|-----|-------|--------|
| `Shortcut+O` | `Ctrl+O` or `Meta+O` | `Shortcut` maps to Cmd/Ctrl automatically at runtime |
| `Shortcut+Shift+E` | `Ctrl+Shift+E` | Same cross-platform rationale |

### Attribute Placement Order
**Source:** `main.fxml` lines 32–35 (`menuItemRender`)
**Apply to:** All five new entries

Order within a `<MenuItem>` element:
1. `fx:id` (if present)
2. `text`
3. `accelerator`
4. `onAction` (if present)

### No `onAction` for KBD-04 / KBD-05
**Source:** `main.fxml` lines 22–23; `FileTreeController.java` lines 185–186
**Apply to:** `menuItemSetEntrypoint` and `menuItemSetXmlInput` only

These items have no `onAction` in FXML — their handlers are bound via `setOnAction()` in
`FileTreeController.initialize()`. Add only the `accelerator` attribute. Do NOT add `onAction`.

---

## No Analog Found

None — the `accelerator` FXML attribute pattern is already used in the same file (lines 32–39)
and constitutes an exact match for all five new entries.

---

## Conflict Audit

Existing accelerators in `main.fxml` before Phase 24:

| Accelerator | MenuItem |
|-------------|----------|
| `F5` | Render (line 34) |
| `Ctrl+Shift+F` | Find in Files (line 39) |

New accelerators introduced in Phase 24:

| Req ID | Accelerator | Conflict? |
|--------|-------------|-----------|
| KBD-01 | `Shortcut+O` | No |
| KBD-02 | `Shortcut+N` | No |
| KBD-03 | `Shortcut+Q` | No |
| KBD-04 | `Shortcut+Shift+E` | No |
| KBD-05 | `Shortcut+Shift+I` | No |

---

## Metadata

**Analog search scope:** `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml`
**Files scanned:** 1 (single-file phase; no Java changes)
**Pattern extraction date:** 2026-04-27
