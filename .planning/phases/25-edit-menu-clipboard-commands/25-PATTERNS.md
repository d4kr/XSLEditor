# Phase 25: Edit Menu Clipboard Commands - Pattern Map

**Mapped:** 2026-04-27
**Files analyzed:** 3 (2 modified Java, 1 modified FXML)
**Analogs found:** 3 / 3

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` | config/view | request-response | `main.fxml` File menu (lines 19-40) | exact |
| `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` | controller | request-response | `MainController.handleRender()` + `handleFindInFiles()` (lines 287-308) | exact |
| `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java` | service/sub-controller | request-response | `EditorController.navigateTo()` (lines 159-167) | exact |

---

## Pattern Assignments

### `main.fxml` — Edit Menu MenuItems

**Analog:** `main.fxml` File menu block (lines 19-40) and Run menu block (lines 43-48).

**Attribute order convention** (lines 20-23 and 44-47): `fx:id` (if needed), `text`, `accelerator`, `onAction`. MenuItems that require no programmatic reference omit `fx:id` entirely (see `Exit` at line 37-39 and `About` at line 54).

**Minimal MenuItem — no fx:id** (lines 37-39):
```xml
<MenuItem text="Exit"
          accelerator="Shortcut+Q"
          onAction="#handleExit"/>
```

**MenuItem with fx:id — needed for disable binding** (lines 20-23):
```xml
<MenuItem fx:id="menuItemOpenProject"
          text="Open Project..."
          accelerator="Shortcut+O"
          onAction="#handleOpenProject"/>
```

**SeparatorMenuItem inside a Menu** (lines 24, 31, 36):
```xml
<SeparatorMenuItem/>
```

**Currently empty Edit menu — insertion target** (line 41):
```xml
<Menu text="Edit"/>
```

**Target result — replace line 41 with:**
```xml
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

Key decisions:
- No `fx:id` on any of the four items — clipboard is always available, no disable binding required (confirmed in RESEARCH.md Pitfall 4).
- `Shortcut+` prefix (not `Ctrl+`) — cross-platform, identical to all other accelerators in this file.
- `SeparatorMenuItem` between Paste and Select All — standard HIG grouping (cut/copy/paste together, select-all separate).

---

### `MainController.java` — Four @FXML Handler Methods

**Analog:** `handleRender()` (lines 287-290) — the shortest existing delegation handler. One line, delegates entirely to a sub-controller. No try/catch needed because the sub-controller returns `Optional` and uses `ifPresent`.

**Existing one-liner delegation pattern** (lines 287-290):
```java
@FXML
private void handleRender() {
    renderController.handleRender();
}
```

**Existing sub-controller field declaration** (line 82):
```java
private final EditorController editorController = new EditorController();  // Phase 4
```

**Import block relevant to new handlers** (lines 1-21): `org.fxmisc.richtext.CodeArea` is not yet imported in `MainController`. It must be added.

**Existing imports to follow for placement** (lines 1-21):
```java
import ch.ti.gagi.xsleditor.XSLEditorApp;
import ch.ti.gagi.xsleditor.log.LogEntry;
import ch.ti.gagi.xsleditor.ui.AboutDialog;
import ch.ti.gagi.xsleditor.model.Project;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
...
import java.util.Optional;
```

**New import to add** (after existing imports, alphabetically within `org.fxmisc`):
```java
import org.fxmisc.richtext.CodeArea;
```

**Four new handler methods — add after `handleAbout()` at line 316, before the private helpers section:**
```java
// --- Phase 25 action handlers (Edit menu clipboard commands) ---

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

### `EditorController.java` — `getActiveCodeArea()` Method

**Analog 1 — tabPane selection query** (lines 133-134):
```java
if (registry.containsKey(key)) {
    tabPane.getSelectionModel().select(registry.get(key));
```

**Analog 2 — getUserData instanceof EditorTab pattern** (lines 116-119 and 163-165):
```java
// lines 116-119 (updateAppDirtyState)
registry.values().stream().anyMatch(tab -> {
    Object ud = tab.getUserData();
    return ud instanceof EditorTab et && et.dirty.get();
});

// lines 163-165 (navigateTo)
if (tab != null && tab.getUserData() instanceof EditorTab et) {
    et.codeArea.moveTo(line, col);
    et.codeArea.requestFollowCaret();
}
```

**Existing import — `Optional` already imported** (line 32):
```java
import java.util.Optional;
```

**Existing import — `CodeArea` already imported** (line 16):
```java
import org.fxmisc.richtext.CodeArea;
```

**Existing import — `Tab` already imported** (line 8):
```java
import javafx.scene.control.Tab;
```

No new imports needed in `EditorController`.

**New public method — add after `navigateTo()` at line 167, before `buildTab()` at line 169:**
```java
/**
 * Returns the CodeArea of the currently selected editor tab,
 * or empty if no tab is open.
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

Placement rationale: `navigateTo()` is the closest caller-shape analog (also queries `tabPane` selection and accesses `EditorTab` via `getUserData`). Place the new method immediately after it for locality.

---

## Shared Patterns

### @FXML Handler Style
**Source:** `MainController.java` lines 190-195, 287-290, 312-315
**Apply to:** All four new Edit menu handlers
```java
@FXML
private void handleXxx() {
    // one-line delegation — no try/catch when the callee returns Optional
    someController.doSomething();
}
```

### Section Comment Separator
**Source:** `MainController.java` lines 189, 285, 293, 311, 319
**Apply to:** New handler group in `MainController`
```java
// --- Phase 25 action handlers (Edit menu clipboard commands) ---
```

### Optional + ifPresent for null-safe delegation
**Source:** `MainController.java` line 252
```java
result.ifPresent(name -> { ... });
```
**Apply to:** All four handlers — `editorController.getActiveCodeArea().ifPresent(CodeArea::cut)` is the established null-safety idiom; no separate null check in the caller.

### getUserData instanceof Pattern (Java 16+ pattern matching)
**Source:** `EditorController.java` lines 117-119, 163
```java
ud instanceof EditorTab et && et.dirty.get()
tab.getUserData() instanceof EditorTab et
```
**Apply to:** `getActiveCodeArea()` body — use `selected.getUserData() instanceof EditorTab et` (same syntax, same class).

---

## No Analog Found

None — all three files have close analogs in the codebase.

---

## Metadata

**Analog search scope:** `src/main/java/ch/ti/gagi/xsleditor/ui/`, `src/main/resources/ch/ti/gagi/xsleditor/ui/`
**Files scanned:** 3 (main.fxml, MainController.java, EditorController.java)
**Pattern extraction date:** 2026-04-27
