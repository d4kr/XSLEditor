# Phase 24: Keyboard Accelerators — Research

**Researched:** 2026-04-27
**Domain:** JavaFX 21 MenuItem accelerators / FXML attribute syntax
**Confidence:** HIGH

---

## Summary

Phase 24 adds keyboard accelerators to five existing File menu items in `main.fxml`. The project
decision (STATE.md) is explicit: **this is FXML-only work — no Java logic changes are required**.
Every target menu item already has a wired `onAction` handler; adding an `accelerator` attribute
in FXML is the complete implementation.

JavaFX resolves the `accelerator` attribute string via `KeyCombination.valueOf()`. The token
`Shortcut` maps to Cmd on macOS and Ctrl on Windows/Linux — exactly the cross-platform behaviour
the requirements describe. The FXML parser accepts the string format directly: `"Shortcut+O"`,
`"Shortcut+Shift+E"`, etc.

The project already uses this pattern for two menu items (`F5` on Render, `Ctrl+Shift+F` on Find
in Files), so the pattern is proven and consistent.

**Primary recommendation:** Add `accelerator="Shortcut+X"` attributes directly to the five
target `<MenuItem>` elements in `main.fxml`. Zero Java changes required.

---

<phase_requirements>
## Phase Requirements

| ID     | Description                                                        | Research Support                                                           |
|--------|--------------------------------------------------------------------|----------------------------------------------------------------------------|
| KBD-01 | `Shortcut+O` opens Open Project file chooser                      | MenuItem already has `onAction="#handleOpenProject"` — add accelerator only |
| KBD-02 | `Shortcut+N` opens New File dialog                                 | MenuItem already has `onAction="#handleNewFile"` — add accelerator only     |
| KBD-03 | `Shortcut+Q` triggers application exit flow                        | MenuItem already has `onAction="#handleExit"` — add accelerator only        |
| KBD-04 | `Shortcut+Shift+E` invokes Set Entrypoint on selected file         | MenuItem `fx:id="menuItemSetEntrypoint"` wired by FileTreeController        |
| KBD-05 | `Shortcut+Shift+I` invokes Set XML Input on selected file          | MenuItem `fx:id="menuItemSetXmlInput"` wired by FileTreeController          |
</phase_requirements>

---

## Architectural Responsibility Map

| Capability               | Primary Tier | Secondary Tier | Rationale                                                                  |
|--------------------------|-------------|----------------|----------------------------------------------------------------------------|
| Keyboard accelerators    | FXML/View   | —              | JavaFX resolves accelerators at the scene level; declared in FXML          |
| Action execution         | Controller  | —              | Existing `onAction` handlers already route to the correct controller logic  |
| Disable state for KBD-04/05 | Controller (FileTreeController) | — | `disableProperty().bind()` already set; accelerators respect disable state |

---

## Standard Stack

### Core

| Library | Version | Purpose                        | Why Standard                        |
|---------|---------|--------------------------------|-------------------------------------|
| JavaFX  | 21      | UI toolkit, MenuBar/MenuItem  | Project standard (build.gradle D-01) |

### Supporting

None — this phase requires no additional libraries.

### Alternatives Considered

| Instead of                  | Could Use                              | Tradeoff                                                         |
|-----------------------------|----------------------------------------|------------------------------------------------------------------|
| FXML `accelerator` attribute | Java `menuItem.setAccelerator(...)` | FXML is simpler, keeps UI declarations in one place; Java code only needed if dynamic |

---

## Architecture Patterns

### System Architecture Diagram

```
User presses Shortcut+O
        │
        ▼
JavaFX Scene KeyCombination matching
        │  (accelerator registered on MenuItem at FXML load time)
        ▼
MenuItem.onAction fires  ──────────────────────────────────────────────────────┐
        │                                                                      │
  (same path as mouse click)                                           (same path for all 5 items)
        │
        ▼
MainController / FileTreeController handler
        │
        ▼
Existing business logic (DirectoryChooser, TextInputDialog, WindowEvent, setEntrypoint, setXmlInput)
```

### Recommended Project Structure

No structural changes. All modifications are within:

```
src/main/resources/ch/ti/gagi/xsleditor/ui/
└── main.fxml     ← only file modified in this phase
```

### Pattern 1: FXML Accelerator Attribute

**What:** A `MenuItem` element in FXML accepts an `accelerator` string that JavaFX parses via
`KeyCombination.valueOf()` at load time. The `Shortcut` token is cross-platform: Cmd on macOS,
Ctrl on Windows/Linux.

**When to use:** Any time a menu item needs a keyboard shortcut and the item already has a
working `onAction` handler.

**Example (from existing project pattern + JavaFX 21 docs):**

```xml
<!-- Source: openjfx.io/javadoc/21 — KeyCombination string format + existing main.fxml pattern -->
<MenuItem text="Open Project..."
          accelerator="Shortcut+O"
          onAction="#handleOpenProject"/>

<MenuItem text="New File..."
          accelerator="Shortcut+N"
          onAction="#handleNewFile"/>

<MenuItem text="Exit"
          accelerator="Shortcut+Q"
          onAction="#handleExit"/>

<MenuItem fx:id="menuItemSetEntrypoint"
          text="Set Entrypoint"
          accelerator="Shortcut+Shift+E"/>

<MenuItem fx:id="menuItemSetXmlInput"
          text="Set XML Input"
          accelerator="Shortcut+Shift+I"/>
```

**Note on KBD-04 / KBD-05:** These two items have no `onAction` in FXML — their handlers are
bound by `FileTreeController.initialize()` via `setOnAction()`. The accelerator attribute is
independent of the action binding mechanism; JavaFX fires the same `ActionEvent` regardless of
whether the action was set in FXML or Java code. The accelerator will therefore invoke the
`FileTreeController` handler correctly. [VERIFIED: openjfx.io/javadoc/21 MenuItem.accelerator]

### Anti-Patterns to Avoid

- **Scene-level KeyEvent handler for menu items:** The project already documents this anti-pattern
  (EditorController.java line 183, STATE.md Accumulated Context). Menu accelerators belong on
  `MenuItem.accelerator`, not on scene `setOnKeyPressed`. The scene-level approach bypasses
  JavaFX's disable state and menu validation.
- **`CONTROL_DOWN` instead of `SHORTCUT_DOWN` in Java:** Use `Shortcut+` in FXML (or
  `SHORTCUT_DOWN` in Java) to stay cross-platform. `CONTROL_DOWN` breaks on macOS where
  the standard modifier is Cmd.
- **Hardcoding `Ctrl+` in FXML:** Same issue — `Shortcut+` is the correct cross-platform token.
  The existing `Ctrl+Shift+F` accelerator on Find in Files is technically macOS-incorrect; this
  phase should use `Shortcut+` for consistency.

---

## Don't Hand-Roll

| Problem                          | Don't Build                          | Use Instead                    | Why                                              |
|----------------------------------|--------------------------------------|--------------------------------|--------------------------------------------------|
| Cross-platform shortcut modifier | Custom platform detection + key maps | `Shortcut+` FXML token         | JavaFX resolves Cmd/Ctrl automatically           |
| Keyboard → action dispatch       | Scene-level KeyEvent filter          | `MenuItem.accelerator` attr    | Respects disable, menu validation, focus state   |

---

## Common Pitfalls

### Pitfall 1: Forgetting that accelerators respect `disableProperty()`

**What goes wrong:** Developer adds accelerator to a disabled menu item and wonders why pressing
the shortcut does nothing.

**Why it happens:** JavaFX does not fire `onAction` for disabled menu items even via accelerator.

**How to avoid:** This is actually correct behaviour for KBD-04 and KBD-05 — these items are
disabled when no file is selected in the tree. The shortcut will silently do nothing when
disabled, which matches the spec ("identical to clicking the menu item").

**Warning signs:** If a manual test shows the shortcut does nothing even with a file selected,
check that the disable binding condition is satisfied (file selected AND project loaded).

### Pitfall 2: FXML attribute vs. Java `setAccelerator()` ordering

**What goes wrong:** If Java code calls `setAccelerator()` after FXML load, it overwrites the
FXML-declared value.

**Why it happens:** `initialize()` runs after FXML parsing; any Java call on the item wins.

**How to avoid:** This phase is FXML-only. Do not add any `setAccelerator()` call in Java. If
a future phase needs to change accelerators dynamically, use `setAccelerator()` consistently
and remove the FXML attribute.

### Pitfall 3: Conflict with existing accelerators

**What goes wrong:** A new accelerator clashes with an existing one in the same scene.

**Why it happens:** JavaFX does not warn at runtime about duplicate accelerators — the first
match wins.

**How to avoid:** Audit existing accelerators before adding new ones. Current registered
accelerators in `main.fxml`:
- `F5` — Render
- `Ctrl+Shift+F` — Find in Files

None of the five new shortcuts (Shortcut+O, Shortcut+N, Shortcut+Q, Shortcut+Shift+E,
Shortcut+Shift+I) conflict with these. [VERIFIED: main.fxml inspection]

### Pitfall 4: `menuItemSetEntrypoint` and `menuItemSetXmlInput` have no `onAction` in FXML

**What goes wrong:** Developer assumes adding `accelerator` is sufficient and also tries to add
`onAction="#handleSetEntrypoint"` in FXML — but no such method exists in MainController.

**Why it happens:** These items' actions are bound in Java by `FileTreeController`, not via FXML.

**How to avoid:** Add only the `accelerator` attribute. Do not add `onAction`. The existing
`setOnAction` binding in `FileTreeController.initialize()` (line 185–186) already handles
dispatch. [VERIFIED: FileTreeController.java lines 185–186]

---

## Code Examples

### Complete diff — five target MenuItems after modification

```xml
<!-- Source: main.fxml current state + JavaFX 21 accelerator attribute format -->

<!-- KBD-01 -->
<MenuItem fx:id="menuItemOpenProject"
          text="Open Project..."
          accelerator="Shortcut+O"
          onAction="#handleOpenProject"/>

<!-- KBD-02 -->
<MenuItem fx:id="menuItemNewFile"
          text="New File..."
          accelerator="Shortcut+N"
          onAction="#handleNewFile"/>

<!-- KBD-03 — Exit item currently has no fx:id; accelerator still works -->
<MenuItem text="Exit"
          accelerator="Shortcut+Q"
          onAction="#handleExit"/>

<!-- KBD-04 — action binding stays in FileTreeController; only accelerator added -->
<MenuItem fx:id="menuItemSetEntrypoint"
          text="Set Entrypoint"
          accelerator="Shortcut+Shift+E"/>

<!-- KBD-05 — action binding stays in FileTreeController; only accelerator added -->
<MenuItem fx:id="menuItemSetXmlInput"
          text="Set XML Input"
          accelerator="Shortcut+Shift+I"/>
```

---

## State of the Art

| Old Approach                    | Current Approach                | When Changed     | Impact                                            |
|---------------------------------|---------------------------------|------------------|---------------------------------------------------|
| Scene-level KeyEvent handler    | `MenuItem.accelerator` attribute | JavaFX 2.x+     | Respects disable state, menu validation, and OS conventions |
| `Ctrl+` hardcoded in FXML       | `Shortcut+` token               | Always supported | Resolves to Cmd on macOS, Ctrl on Windows/Linux   |

**Deprecated/outdated:**
- Scene-level keyboard dispatch for menu items: replaced by `MenuItem.accelerator` + `KeyCombination` — avoid for any shortcut that maps to a menu action.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The Exit MenuItem has no `fx:id` — adding only `accelerator` without an `fx:id` is sufficient | Code Examples | Low — fx:id is needed only for Java injection; accelerator works without it |

**All other claims verified via FXML source inspection or JavaFX 21 official docs.**

---

## Open Questions

None. The implementation path is fully determined by existing code inspection and official docs.

---

## Environment Availability

Step 2.6: SKIPPED — phase is a pure FXML edit with no external tool dependencies.

---

## Validation Architecture

### Test Framework

| Property       | Value                                                              |
|----------------|--------------------------------------------------------------------|
| Framework      | Manual / smoke testing (no automated UI test infrastructure found) |
| Config file    | none                                                               |
| Quick run      | Build: `./gradlew shadowJar` then launch app manually             |
| Full suite     | Same — no automated test suite detected in project                |

### Phase Requirements → Test Map

| Req ID | Behavior                                      | Test Type | Automated Command | File Exists? |
|--------|-----------------------------------------------|-----------|-------------------|-------------|
| KBD-01 | Shortcut+O opens Open Project chooser         | smoke     | manual            | N/A         |
| KBD-02 | Shortcut+N opens New File dialog              | smoke     | manual            | N/A         |
| KBD-03 | Shortcut+Q triggers exit flow                 | smoke     | manual            | N/A         |
| KBD-04 | Shortcut+Shift+E invokes Set Entrypoint       | smoke     | manual            | N/A         |
| KBD-05 | Shortcut+Shift+I invokes Set XML Input        | smoke     | manual            | N/A         |

### Wave 0 Gaps

None — no test infrastructure setup required. All verification is manual smoke testing
by running the built JAR and pressing each keyboard shortcut.

---

## Security Domain

Not applicable — this phase adds FXML attributes to menu items. No input handling, network
access, file I/O, or authentication logic is introduced or modified.

---

## Sources

### Primary (HIGH confidence)

- `/websites/openjfx_io_javadoc_21` (Context7) — `MenuItem.accelerator`, `KeyCombination.valueOf`, `SHORTCUT_DOWN` modifier
- `src/main/resources/.../main.fxml` — existing accelerator usage (`F5`, `Ctrl+Shift+F`) and current MenuItem structure
- `src/main/java/.../MainController.java` — action handler wiring, confirmed no Java accelerator code exists
- `src/main/java/.../FileTreeController.java` lines 185–186 — confirmed `setOnAction` binding for KBD-04/05 items
- `.planning/STATE.md` Key Decisions — locked decision: "Phase 24 is FXML-only work"

### Secondary (MEDIUM confidence)

None required — primary sources fully cover the implementation.

### Tertiary (LOW confidence)

None.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — JavaFX 21, single file change, verified against official docs
- Architecture: HIGH — FXML-only change confirmed by STATE.md decision + source inspection
- Pitfalls: HIGH — all identified from direct code inspection, not hypothetical

**Research date:** 2026-04-27
**Valid until:** 2026-05-27 (stable JavaFX 21 API; no time-sensitive dependencies)
