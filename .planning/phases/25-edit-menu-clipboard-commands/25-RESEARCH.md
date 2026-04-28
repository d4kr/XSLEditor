# Phase 25: Edit Menu Clipboard Commands — Research

**Researched:** 2026-04-27
**Domain:** JavaFX 21 MenuBar, RichTextFX 0.11.5 CodeArea clipboard API, sub-controller delegation
**Confidence:** HIGH

---

## Summary

Phase 25 adds four Edit menu items (Cut, Copy, Paste, Select All) to `main.fxml` and wires each one
to the currently active `CodeArea` via a new `getActiveCodeArea()` method on `EditorController`.

**The critical architectural discovery:** RichTextFX `GenericStyledAreaBehavior` already registers
native keyboard handlers for `Shortcut+C`, `Shortcut+X`, `Shortcut+V`, and `Shortcut+A` at the
CodeArea level. Furthermore, issue #185 confirms that when a RichTextFX control is focused, MenuBar
accelerators do NOT fire — the CodeArea consumes the key events first. This means:

1. The keyboard shortcuts (Cmd+C etc.) already work in the CodeArea with no code changes.
2. The `onAction` handler on each MenuItem is the only required implementation — it provides
   mouse-accessible access and fires when the CodeArea is NOT focused (or when the user actually
   clicks the menu item).
3. Adding `accelerator="Shortcut+C"` to the MenuItems is still correct: the accelerator text is
   displayed next to the menu item as a standard UI hint, even if the CodeArea intercepts the
   key when focused.

The implementation is two files only:
- `main.fxml`: add four `<MenuItem>` elements inside `<Menu text="Edit"/>` with `onAction` and
  `accelerator` attributes.
- `EditorController.java`: add one public method `getActiveCodeArea()` that returns
  `Optional<CodeArea>` by querying `tabPane.getSelectionModel().getSelectedItem()` and
  extracting the `EditorTab.codeArea` from `getUserData()`.
- `MainController.java`: add four `@FXML` handler methods that call
  `editorController.getActiveCodeArea().ifPresent(ca -> ca.cut())` (and copy, paste, selectAll).

**Primary recommendation:** Add `getActiveCodeArea()` to `EditorController`, add four FXML menu
items in `main.fxml`, and add four `@FXML` handlers in `MainController` that delegate to the
active CodeArea. Zero new dependencies required.

---

<phase_requirements>
## Phase Requirements

| ID      | Description                                           | Research Support                                                                |
|---------|-------------------------------------------------------|---------------------------------------------------------------------------------|
| EDIT-10 | Cut selected text in active editor via Edit > Cut     | `CodeArea.cut()` — verified in RichTextFX docs; delegation via `getActiveCodeArea()` |
| EDIT-11 | Copy selected text in active editor via Edit > Copy   | `CodeArea.copy()` — same delegation pattern                                     |
| EDIT-12 | Paste clipboard text into active editor via Edit > Paste | `CodeArea.paste()` — same delegation pattern                                 |
| EDIT-13 | Select all text in active editor via Edit > Select All | `CodeArea.selectAll()` — verified in RichTextFX docs (NavigationActions)       |
</phase_requirements>

---

## Architectural Responsibility Map

| Capability                          | Primary Tier             | Secondary Tier       | Rationale                                                                               |
|-------------------------------------|--------------------------|----------------------|-----------------------------------------------------------------------------------------|
| Clipboard Cut/Copy/Paste/SelectAll  | Editor widget (CodeArea) | MenuBar (delegation) | RichTextFX owns the clipboard logic; MenuBar delegates to it, not the reverse           |
| Active tab resolution               | EditorController         | —                    | EditorController owns `tabPane` and `registry`; only it can resolve the active CodeArea |
| Menu item wiring                    | FXML + MainController    | —                    | Standard pattern: FXML declares items, MainController provides `@FXML` handlers          |
| Keyboard shortcut on focused editor | RichTextFX (built-in)    | —                    | `GenericStyledAreaBehavior` handles Shortcut+C/X/V/A natively; no extra wiring needed  |

---

## Standard Stack

### Core

| Library           | Version | Purpose                           | Why Standard                                      |
|-------------------|---------|-----------------------------------|---------------------------------------------------|
| JavaFX 21         | 21      | MenuBar, MenuItem, scene graph    | Project standard (build.gradle)                   |
| RichTextFX        | 0.11.5  | CodeArea with built-in clipboard  | Project standard; already provides cut/copy/paste |

### Supporting

No new libraries required. All capabilities are provided by the existing stack.

### Alternatives Considered

| Instead of                          | Could Use                                    | Tradeoff                                                                                   |
|-------------------------------------|----------------------------------------------|--------------------------------------------------------------------------------------------|
| `getActiveCodeArea()` on EditorController | Scene focus owner (`scene.getFocusOwner()`) | `getFocusOwner()` is fragile — returns any focused node, not necessarily a CodeArea; delegation via EditorController is the established pattern (all existing sub-controllers follow it) |
| Delegate via `onAction` handlers    | WellBehavedFX `Nodes.addInputMap` per CodeArea | InputMap pattern is for per-CodeArea key-level handlers (Ctrl+S, Ctrl+Space); menu-driven clipboard belongs in the action handler, not the input map |
| Adding `accelerator` only (no onAction) | Rely solely on CodeArea native shortcuts | MenuItem would have no click behavior; requirements explicitly require the menu items to work |

**Installation:** No new packages — zero `npm install` / `gradle` dependency changes.

---

## Architecture Patterns

### System Architecture Diagram

```
User clicks Edit > Cut (or presses Shortcut+X when NOT in CodeArea)
        │
        ▼
MenuItem.onAction → MainController.handleEditCut()
        │
        ▼
editorController.getActiveCodeArea()
        │  (tabPane.getSelectionModel().getSelectedItem()
        │   → Tab.getUserData() → EditorTab.codeArea)
        │
        ├── Optional.empty() (no tab open) → no-op
        │
        └── Optional<CodeArea> present
                │
                ▼
           codeArea.cut()  ← RichTextFX ClipboardActions
                │
                ▼
        System clipboard updated; editor content modified

--- PARALLEL PATH (keyboard, CodeArea focused) ---

User presses Shortcut+X while CodeArea is focused
        │
        ▼
RichTextFX GenericStyledAreaBehavior key handler (Shortcut+X)
        │  (event consumed here — MenuBar accelerator does NOT fire)
        ▼
codeArea.cut() called internally
        │
        ▼
System clipboard updated; editor content modified
        (MenuItem.onAction is NOT called in this path)
```

### Recommended Project Structure

No new files, no structural changes:

```
src/main/
├── java/ch/ti/gagi/xsleditor/ui/
│   ├── EditorController.java   ← add getActiveCodeArea(): Optional<CodeArea>
│   └── MainController.java     ← add handleEditCut/Copy/Paste/SelectAll()
└── resources/ch/ti/gagi/xsleditor/ui/
    └── main.fxml               ← add four <MenuItem> inside <Menu text="Edit"/>
```

### Pattern 1: Active CodeArea Resolution

**What:** `EditorController.getActiveCodeArea()` returns the CodeArea of the currently selected tab,
or empty if no tab is open.

**When to use:** Any MenuBar action that needs to operate on the active editor.

```java
// Source: EditorController pattern derived from existing tabPane.getSelectionModel() usage
// (lines 134, 144 of EditorController.java) + EditorTab.getUserData() pattern (lines 118, 163)

/**
 * Returns the CodeArea of the currently selected editor tab, or empty if no tab is open.
 * Called from MainController Edit menu action handlers.
 */
public Optional<CodeArea> getActiveCodeArea() {
    Tab selected = tabPane.getSelectionModel().getSelectedItem();
    if (selected != null && selected.getUserData() instanceof EditorTab et) {
        return Optional.of(et.codeArea);
    }
    return Optional.empty();
}
```

### Pattern 2: MainController Delegation Handlers

**What:** Four `@FXML` methods in `MainController` delegate to the active CodeArea.

**When to use:** Every Edit menu action.

```java
// Source: standard JavaFX @FXML handler pattern; RichTextFX CodeArea.cut/copy/paste/selectAll
// verified in Context7 ContextMenu example

@FXML
private void handleEditCut() {
    editorController.getActiveCodeArea().ifPresent(CodeArea::cut);
}

@FXML
private void handleEditCopy() {
    editorController.getActiveCodeArea().ifPresent(CodeArea::copy);
}

@FXML
private void handleEditPaste() {
    editorController.getActiveCodeArea().ifPresent(CodeArea::paste);
}

@FXML
private void handleEditSelectAll() {
    editorController.getActiveCodeArea().ifPresent(CodeArea::selectAll);
}
```

### Pattern 3: FXML MenuItem Declaration

**What:** Four new `<MenuItem>` elements inside `<Menu text="Edit"/>` in `main.fxml`.

**When to use:** Any new Edit menu item.

```xml
<!-- Source: existing main.fxml MenuItem pattern (menuItemRender at line 44-47)
     Attribute order: text, accelerator, onAction -->
<Menu text="Edit">
    <MenuItem text="Cut"
              accelerator="Shortcut+X"
              onAction="#handleEditCut"/>
    <MenuItem text="Copy"
              accelerator="Shortcut+C"
              onAction="#handleEditCopy"/>
    <MenuItem text="Paste"
              accelerator="Shortcut+V"
              onAction="#handleEditPaste"/>
    <SeparatorMenuItem/>
    <MenuItem text="Select All"
              accelerator="Shortcut+A"
              onAction="#handleEditSelectAll"/>
</Menu>
```

Note: these MenuItems do NOT need `fx:id` — MainController accesses them only via `onAction`.

### Anti-Patterns to Avoid

- **Scene-level KeyEvent filter for Ctrl+C:** The existing Ctrl+S uses `Nodes.addInputMap` on the
  CodeArea itself (per-CodeArea, not scene-level). The STATE.md explicitly records this as the
  correct pattern and warns against scene-level handlers. Do NOT use a scene-level handler for
  clipboard.
- **`scene.getFocusOwner()` cast to CodeArea:** Fragile — focus owner can be any node (file tree,
  toolbar button). Always go through `EditorController.getActiveCodeArea()` which queries the
  TabPane selection.
- **Adding `onAction` to `<Menu text="Edit"/>` itself:** The `<Menu>` element has no `onAction`;
  only `<MenuItem>` elements inside it do.
- **Wiring accelerators per-CodeArea with `Nodes.addInputMap`:** The clipboard shortcuts already
  work natively in the CodeArea (RichTextFX built-in). Adding them again via InputMap would
  double-handle and could cause unexpected behavior with the UndoManager.
- **`CONTROL_DOWN` instead of `SHORTCUT_DOWN` / `Shortcut+`:** Always use the `Shortcut+` token
  in FXML (cross-platform: Cmd on macOS, Ctrl on Windows/Linux).

---

## Don't Hand-Roll

| Problem              | Don't Build                                   | Use Instead                         | Why                                                                          |
|----------------------|-----------------------------------------------|-------------------------------------|------------------------------------------------------------------------------|
| Clipboard cut        | Manual `getSelectedText()` + `Clipboard.put()` + `replaceSelection("")` | `CodeArea.cut()` | RichTextFX handles style preservation, UndoManager integration, and edge cases |
| Clipboard copy       | Manual `getSelectedText()` + `Clipboard.put()` | `CodeArea.copy()`                  | Same — built-in handles empty selection gracefully                            |
| Clipboard paste      | Manual `Clipboard.get()` + `insertText()`     | `CodeArea.paste()`                  | Built-in handles caret placement, replaces selection, integrates with UndoManager |
| Select all text      | Manual `selectRange(0, area.getLength())`     | `CodeArea.selectAll()`              | Built-in is correct and idiomatic; `selectRange` requires knowing exact bounds |

**Key insight:** RichTextFX's `ClipboardActions` interface (implemented by all RichTextFX text areas)
provides these operations as first-class methods. They integrate with the `UndoManager` correctly.
Custom re-implementations would break undo/redo tracking.

---

## Common Pitfalls

### Pitfall 1: MenuBar Accelerator vs. RichTextFX Event Consumption

**What goes wrong:** Developer adds `accelerator="Shortcut+C"` to the MenuItem and expects that
pressing Cmd+C always fires `onAction`. When CodeArea is focused, it does NOT — RichTextFX
`GenericStyledAreaBehavior` consumes the key event first.

**Why it happens:** Known RichTextFX behavior (issue #185). The CodeArea's own keyboard handler
runs at lower bubbling phase than the scene-level MenuBar accelerator mechanism.

**How to avoid:** This is not actually a bug for this phase — when the CodeArea is focused,
RichTextFX handles clipboard natively (same result). The `onAction` handler fires when the user
clicks the menu item with the mouse OR when the MenuBar accelerator fires (CodeArea NOT focused,
which is rare for clipboard).

**Warning signs:** If a manual test shows Cmd+C fires `handleEditCopy()` instead of the CodeArea's
native handler, something is wrong with the event routing.

### Pitfall 2: No Active Tab — NullPointerException

**What goes wrong:** `getActiveCodeArea()` returns `null` instead of `Optional.empty()`, and the
caller does `area.cut()` without a null check.

**Why it happens:** `tabPane.getSelectionModel().getSelectedItem()` returns `null` when no tabs
are open.

**How to avoid:** Return `Optional<CodeArea>` from `getActiveCodeArea()` (not a nullable `CodeArea`).
Use `ifPresent()` in all callers. No null check needed in MainController.

**Warning signs:** `NullPointerException` in `MainController.handleEditCut()` when no tab is open.

### Pitfall 3: `getActiveCodeArea()` Placed on Wrong Class

**What goes wrong:** Developer puts the method on `MainController` instead of `EditorController`,
duplicating the `tabPane` access logic.

**Why it happens:** `MainController` is the wiring point and feels like the natural home.

**How to avoid:** `EditorController` owns `tabPane` (private field). Access must go through it.
Consistent with how `RenderController`, `LogController`, etc. all receive `EditorController` as a
collaborator rather than accessing `tabPane` directly.

### Pitfall 4: `fx:id` Injection for Cut/Copy/Paste/SelectAll MenuItems

**What goes wrong:** Adding `fx:id` to each new MenuItem and declaring `@FXML private MenuItem
menuItemCut;` etc. in `MainController`, then never actually using those references.

**Why it happens:** Pattern from existing menu items like `menuItemOpenProject` which need `fx:id`
for programmatic disable bindings.

**How to avoid:** These four MenuItems do NOT need disable bindings (clipboard is always available).
Omit `fx:id` entirely. The `onAction` wiring is sufficient.

### Pitfall 5: `Shortcut+A` Conflicts with Select All in File Tree / Other Controls

**What goes wrong:** Pressing Shortcut+A when the file tree is focused triggers the Edit >
Select All MenuItem, which tries to call `getActiveCodeArea().selectAll()` — but succeeds
silently because `getActiveCodeArea()` returns `Optional.empty()` when no tab is open.

**Why it happens:** MenuBar accelerators fire regardless of which control is focused (unless the
focused control consumes the event first). The file tree (TreeView) does not consume Shortcut+A.

**How to avoid:** The `Optional.empty()` path is already a no-op, so there is no harmful
behavior. This is acceptable — the spec says "selects all text in the active editor tab", and
with no active tab, there is nothing to select.

### Pitfall 6: accelerator="Ctrl+C" instead of "Shortcut+C"

**What goes wrong:** Using `Ctrl+C` hardcodes the modifier; on macOS this binds to Control+C
(not Cmd+C). The user expects Cmd+C to trigger the menu item display hint.

**Why it happens:** Developer confuses `Ctrl+` (explicit modifier) with `Shortcut+` (platform
shortcut modifier).

**How to avoid:** Always use `Shortcut+` in FXML. This is identical to the pattern established
by Phase 24 for KBD-01 through KBD-05 and the existing `Shortcut+Shift+F` for Find in Files.

---

## Code Examples

### getActiveCodeArea — Full Implementation

```java
// Source: EditorController.java — derived from existing tabPane.getSelectionModel() usage
// (lines 134, 144) and getUserData instanceof EditorTab pattern (lines 118, 163)
import java.util.Optional;
import org.fxmisc.richtext.CodeArea;

/**
 * Returns the CodeArea of the currently selected editor tab, or empty if no tab is open.
 * Called from MainController Edit menu action handlers (EDIT-10..13).
 */
public Optional<CodeArea> getActiveCodeArea() {
    Tab selected = tabPane.getSelectionModel().getSelectedItem();
    if (selected != null && selected.getUserData() instanceof EditorTab et) {
        return Optional.of(et.codeArea);
    }
    return Optional.empty();
}
```

### main.fxml — Edit Menu After Change

```xml
<!-- Source: main.fxml existing structure + JavaFX 21 MenuItem accelerator format
     Attribute order convention: text, accelerator, onAction (no fx:id needed) -->
<Menu text="Edit">
    <MenuItem text="Cut"
              accelerator="Shortcut+X"
              onAction="#handleEditCut"/>
    <MenuItem text="Copy"
              accelerator="Shortcut+C"
              onAction="#handleEditCopy"/>
    <MenuItem text="Paste"
              accelerator="Shortcut+V"
              onAction="#handleEditPaste"/>
    <SeparatorMenuItem/>
    <MenuItem text="Select All"
              accelerator="Shortcut+A"
              onAction="#handleEditSelectAll"/>
</Menu>
```

### MainController — Four Handler Methods

```java
// Source: JavaFX @FXML handler pattern; RichTextFX ClipboardActions (verified Context7)
// Add after the existing handleRender() and handleFindInFiles() methods.
// Import needed: import org.fxmisc.richtext.CodeArea;

@FXML
private void handleEditCut() {
    editorController.getActiveCodeArea().ifPresent(CodeArea::cut);
}

@FXML
private void handleEditCopy() {
    editorController.getActiveCodeArea().ifPresent(CodeArea::copy);
}

@FXML
private void handleEditPaste() {
    editorController.getActiveCodeArea().ifPresent(CodeArea::paste);
}

@FXML
private void handleEditSelectAll() {
    editorController.getActiveCodeArea().ifPresent(CodeArea::selectAll);
}
```

---

## State of the Art

| Old Approach                         | Current Approach                                  | When Changed      | Impact                                                              |
|--------------------------------------|---------------------------------------------------|-------------------|---------------------------------------------------------------------|
| JavaFX TextArea (no styling)         | RichTextFX CodeArea                               | Phase 4 (v0.1.0)  | Clipboard handled by RichTextFX, not JavaFX TextInputControl        |
| Manual clipboard via `javafx.scene.input.Clipboard` | `CodeArea.cut/copy/paste()` from ClipboardActions | Always in RichTextFX | Built-in handles UndoManager integration |

**Deprecated/outdated:**
- Scene-level `EventFilter` for Ctrl+C/X/V/A: the project's STATE.md explicitly documents that
  scene-level handlers cause focus bugs (see Ctrl+S history in Accumulated Context). Per-CodeArea
  handling or menu delegation is the mandated approach.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `getActiveCodeArea()` with `Optional<CodeArea>` return type is the correct API surface — no other caller needs the active CodeArea differently | Architecture Patterns | Low — if a future caller needs a different signature, the method can be overloaded |
| A2 | Cut/Copy/Paste/SelectAll MenuItems require no `disableProperty()` bindings (they are always enabled) | Pitfall 4 | Low — worst case is user clicks "Copy" with nothing selected; `copy()` is a no-op |

---

## Open Questions

1. **Should the MenuItem for Cut/Copy be disabled when no text is selected?**
   - What we know: The existing context menu example from RichTextFX docs shows conditional
     disabling (`cut.setDisable(area.getSelection().getLength() == 0)`) in `setOnShowing`.
   - What's unclear: Requirements (EDIT-10..13) do not specify this behavior.
   - Recommendation: Do NOT add disable bindings in this phase — keep it simple. A no-op cut/copy
     when nothing is selected is standard JavaFX behavior and not a regression.

---

## Environment Availability

Step 2.6: SKIPPED — phase is pure Java/FXML changes with no external runtime dependencies beyond
the existing project stack (Java 21, JavaFX 21, RichTextFX 0.11.5), all already verified present.

---

## Validation Architecture

### Test Framework

| Property          | Value                                                    |
|-------------------|----------------------------------------------------------|
| Framework         | JUnit Jupiter 5.10.0                                     |
| Config file       | `build.gradle` → `test { useJUnitPlatform() }`           |
| Quick run command | `./gradlew test --tests "*.EditorTabTest" --quiet`       |
| Full suite command| `./gradlew test --quiet`                                 |

### Phase Requirements → Test Map

| Req ID  | Behavior                                  | Test Type    | Automated Command                                                     | File Exists? |
|---------|-------------------------------------------|--------------|-----------------------------------------------------------------------|--------------|
| EDIT-10 | Cut removes selection and updates clipboard | manual       | N/A — JavaFX Platform + system clipboard; headless JUnit cannot test | manual only  |
| EDIT-11 | Copy copies selection without modifying editor | manual    | N/A — same reason                                                     | manual only  |
| EDIT-12 | Paste inserts clipboard text at cursor    | manual       | N/A — same reason                                                     | manual only  |
| EDIT-13 | Select All selects all text               | manual       | N/A — same reason                                                     | manual only  |
| (compile) | `getActiveCodeArea()` compiles; handlers wire | unit (compile) | `./gradlew compileJava --quiet`                                   | existing     |

**Manual-only justification:** JavaFX clipboard operations require a display (`DISPLAY` env on Linux,
window focus on macOS). The existing test suite uses `Platform.startup()` for headless JavaFX
construction (e.g., `EditorTabTest`) but does not access the system clipboard. Testing clipboard
behavior requires a full smoke test in the running app.

### Sampling Rate

- **Per task commit:** `./gradlew compileJava --quiet`
- **Per wave merge:** `./gradlew test --quiet`
- **Phase gate:** Full suite green + human smoke test before `/gsd-verify-work`

### Wave 0 Gaps

None — existing test infrastructure covers all automatable requirements. Clipboard behavior is
manual-only (see table above).

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category        | Applies | Standard Control                                                          |
|----------------------|---------|---------------------------------------------------------------------------|
| V2 Authentication    | no      | No auth in this tool (CLAUDE.md: "No authentication")                     |
| V3 Session Management| no      | Desktop tool, single user, no sessions                                    |
| V4 Access Control    | no      | No access control                                                         |
| V5 Input Validation  | no      | Clipboard paste inserts into the editor — same trust level as keyboard typing; no external data boundary |
| V6 Cryptography      | no      | No crypto                                                                 |

### Known Threat Patterns

| Pattern               | STRIDE     | Standard Mitigation                                                                           |
|-----------------------|------------|-----------------------------------------------------------------------------------------------|
| Clipboard injection   | Tampering  | Accept — clipboard paste is equivalent to keyboard typing in a developer tool; no command execution or script injection path exists in the XSL editor pipeline |
| Accelerator shadowing | Spoofing   | Accept — `Shortcut+C` accelerator displays correctly in the Edit menu; the CodeArea handles the key event first when focused, which is the expected behavior |

ASVS Level 1: no in-scope controls triggered. This phase adds a standard text editor clipboard
delegation with zero network I/O, persistence change, or authentication surface.

---

## Sources

### Primary (HIGH confidence)

- [Context7 `/fxmisc/richtextfx`] — `CodeArea.cut()`, `copy()`, `paste()`, `selectAll()` verified
  via Context Menu and Clipboard examples; `selectAll()` verified via NavigationActions example
- [Context7 `/fxmisc/richtextfx`] — `GenericStyledAreaBehavior` registers `SHORTCUT_X/C/V/A` as
  native keyboard handlers (confirmed via web fetch of GitHub source)
- Codebase grep — `EditorController.java` lines 118, 134, 144, 163: `tabPane.getSelectionModel()`
  and `getUserData() instanceof EditorTab` patterns verified as established conventions
- `main.fxml` lines 41 and 44-47: `<Menu text="Edit"/>` is currently empty; `menuItemRender`
  provides the attribute order convention

### Secondary (MEDIUM confidence)

- [RichTextFX Issue #185](https://github.com/FXMisc/RichTextFX/issues/185) — MenuBar accelerators
  do not fire when RichTextFX is focused; confirmed via WebFetch

### Tertiary (LOW confidence)

None — all claims verified via codebase inspection or official sources.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — RichTextFX 0.11.5 is in build.gradle; clipboard methods verified in Context7
- Architecture: HIGH — `getActiveCodeArea()` pattern derived directly from existing codebase conventions
- Pitfalls: HIGH — Pitfall 1 (accelerator/focus) confirmed via GitHub issue #185; all others from direct code analysis
- Security: HIGH — no new threat surface

**Research date:** 2026-04-27
**Valid until:** Stable (no fast-moving dependencies; JavaFX 21 LTS + RichTextFX 0.11.5 are frozen for this project)
