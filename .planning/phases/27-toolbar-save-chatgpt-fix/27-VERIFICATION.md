---
phase: 27-toolbar-save-chatgpt-fix
verified: 2026-05-01T08:00:00Z
status: human_needed
score: 12/12 must-haves verified
overrides_applied: 0
human_verification:
  - test: "Save button enable/disable state on dirty tab"
    expected: "After opening a project and typing one character, the Save button becomes enabled; after clicking Save, the button returns to disabled and the tab title * marker disappears"
    why_human: "Requires JavaFX runtime with an open project — cannot verify dirty BooleanBinding state transitions programmatically from the shell"
  - test: "Tab-switch updates Save button state immediately"
    expected: "Switching between two tabs with different dirty states causes the Save button to update to match the newly active tab's dirty state within the same JavaFX event tick"
    why_human: "Requires interactive runtime testing; tab-switch callback chain cannot be exercised without launching the app"
  - test: "ChatGPT button opens browser with pre-filled URL"
    expected: "Clicking the chat bubble button on an error log row opens the default browser at https://chatgpt.com/?q=<encoded-prompt> and the query text appears in the ChatGPT input field"
    why_human: "Requires external browser and a live render error in the log panel — cannot verify event dispatch behavior (addEventHandler arming ButtonBase) without running the app"
  - test: "ChatGPT button does NOT change log row selection"
    expected: "Clicking the chat bubble button on an error row does not change which log row is selected"
    why_human: "Event propagation suppression by consume() after bubbling is only verifiable at runtime with a populated log panel"
---

# Phase 27: Toolbar Save & ChatGPT Fix — Verification Report

**Phase Goal:** Add toolbar Save button (TOOL-03) and fix ChatGPT button event handling (ERR-07)
**Verified:** 2026-05-01T08:00:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | A Save button is visible in the toolbar between the existing Separator and the Render button | VERIFIED | main.fxml line 88: `<Button fx:id="saveButton" text="💾" onAction="#handleToolbarSave">` between `<Separator/>` lines 87 and 91; renderButton at line 92 |
| 2 | The Save button is disabled when no editor tab is open | VERIFIED | MainController.rebindToolbarButtons(): null-tab path at line 404 calls `saveButton.setDisable(true)` |
| 3 | The Save button is disabled when the active tab is clean (not dirty) | VERIFIED | Binding: `Bindings.createBooleanBinding(() -> !Boolean.TRUE.equals(dirtyProp.getValue()), dirtyProp)` — returns true (disabled) when dirty is false |
| 4 | The Save button is enabled when the active tab is dirty (has unsaved changes) | VERIFIED | Same binding — returns false (enabled) when dirtyProp is true |
| 5 | Clicking the Save button writes the active tab's content to disk and clears the dirty marker | VERIFIED | handleToolbarSave (MainController line 371) → editorController.saveActiveTab() → saveTab(EditorTab) which calls Files.writeString + um.mark() + updateAppDirtyState() |
| 6 | Switching tabs immediately updates the Save button's disable state to match the newly active tab's dirty state | UNCERTAIN (human) | Rebind fires via setOnActiveTabChanged callback wired to rebindToolbarButtons — wiring exists, runtime behavior needs human check |
| 7 | All four toolbar buttons (Undo, Redo, Save, Render) have tooltips | VERIFIED | main.fxml lines 82, 85, 89, 93: Tooltip nodes present on all four buttons |
| 8 | D-03: Save button label is the floppy disk emoji (no text) | VERIFIED | main.fxml line 88: `text="💾"` |
| 9 | D-04: saveButton inserted in FXML between the post-undo/redo Separator and renderButton with own Separator | VERIFIED | Toolbar order: undoButton(81) → redoButton(84) → Separator(87) → saveButton(88) → Separator(91) → renderButton(92) |
| 10 | D-05: Tooltip nodes added to all four toolbar buttons | VERIFIED | All four tooltips present per grep output |
| 11 | D-06: EditorController.saveActiveTab() exposed as public method delegating to private saveTab(EditorTab) | VERIFIED | EditorController.java line 202: `public void saveActiveTab()` calls `saveTab(et)` at line 205 |
| 12 | D-07: saveButton.disableProperty() bound to activeEditorTab.dirty.not() in rebindToolbarButtons(); set to true in the null-tab path | VERIFIED | MainController lines 396 (unbind), 404 (null path setDisable), 431-434 (binding to dirty property) |
| 13 | ERR-07: Clicking chat button opens default browser with correct ChatGPT URL | UNCERTAIN (human) | addEventHandler confirmed at LogController line 165; URL construction at line 176 confirmed correct; browser open is runtime behavior |
| 14 | Fix is addEventFilter → addEventHandler in LogController.createAiButton() | VERIFIED | `addEventFilter(MOUSE_PRESSED)` count = 0; `addEventHandler(MOUSE_PRESSED)` count = 1; comment at line 161 references ERR-07 |
| 15 | URL construction unchanged (https://chatgpt.com/?q=TEXT) | VERIFIED | LogController line 176: `"https://chatgpt.com/?q=" + encoded` |
| 16 | Italian preamble unchanged | VERIFIED | LogController line 171: `"Ho questo errore nel mio progetto XSLT/XSL-FO, puoi aiutarmi?\n\n"` |
| 17 | URL encoding unchanged (replace "+" with "%20") | VERIFIED | LogController line 175: `.replace("+", "%20")` |
| 18 | D-01: addEventFilter changed to addEventHandler in createAiButton() | VERIFIED | grep -c addEventFilter = 0; grep -c addEventHandler = 1 |

**Score:** 12/12 must-haves verified (4 items require human runtime confirmation)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` | Toolbar with saveButton between post-undo Separator and renderButton, plus Separator after saveButton | VERIFIED | Contains `fx:id="saveButton"`, correct toolbar ordering confirmed at lines 81-94 |
| `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` | @FXML saveButton field, handleToolbarSave action, extended rebind method with saveButton.disable binding | VERIFIED | All three elements present and wired; rebindUndoRedo renamed to rebindToolbarButtons (0 occurrences of old name) |
| `src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java` | Public saveActiveTab() method + getActiveDirtyProperty() accessor | VERIFIED | Both public methods present at lines 202 and 214; private saveTab(EditorTab) preserved at line 413 |
| `src/main/java/ch/ti/gagi/xsleditor/ui/LogController.java` | createAiButton() with MOUSE_PRESSED handler (not filter) consuming after ButtonBase arms | VERIFIED | addEventHandler present at line 165; addEventFilter count = 0 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| main.fxml saveButton | MainController.handleToolbarSave | onAction="#handleToolbarSave" | WIRED | main.fxml line 88 onAction="#handleToolbarSave"; MainController handleToolbarSave at line 371 |
| MainController.handleToolbarSave | EditorController.saveActiveTab | direct call | WIRED | MainController line 371: `editorController.saveActiveTab()` |
| MainController.rebindToolbarButtons | EditorController active EditorTab dirty property | Bindings.createBooleanBinding via getActiveDirtyProperty() | WIRED | MainController lines 430-434: getActiveDirtyProperty().ifPresentOrElse binding present |
| createAiButton setOnAction | XSLEditorApp.hostServices().showDocument | ButtonBase action fired after MOUSE_PRESSED arms | WIRED | LogController line 177: `XSLEditorApp.hostServices().showDocument(url)` inside setOnAction |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| EditorController.saveActiveTab() | EditorTab content | `editorTab.codeArea.getText()` written via `Files.writeString(editorTab.path, ...)` | Yes — reads live CodeArea text | FLOWING |
| MainController.rebindToolbarButtons | dirtyProp | `editorController.getActiveDirtyProperty()` → `et.dirty` BooleanBinding from EditorTab | Yes — live binding to UndoManager mark state | FLOWING |
| LogController createAiButton setOnAction | prompt string | `getTableRow().getItem().message()` — live LogEntry from ObservableList | Yes — real error message from render pipeline | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Project compiles | `./gradlew compileJava` | BUILD SUCCESSFUL | PASS |
| Tests pass | `./gradlew test` | BUILD SUCCESSFUL (5 tasks) | PASS |
| addEventFilter removed from LogController | `grep -c "addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED"` | 0 | PASS |
| addEventHandler present in LogController | `grep -c "addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED"` | 1 | PASS |
| saveButton in FXML between correct Separators | grep -n line ordering | undoButton(81) < redoButton(84) < Separator(87) < saveButton(88) < Separator(91) < renderButton(92) | PASS |
| rebindUndoRedo fully renamed | `grep -c "rebindUndoRedo"` | 0 | PASS |
| rebindToolbarButtons wired in initialize() | `grep -n rebindToolbarButtons` | 3 occurrences (definition + 2 call sites) | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| TOOL-03 | 27-01-PLAN.md | Developer can click Save button in the toolbar (saves the active tab; disabled when tab is clean/not dirty) | SATISFIED | saveButton in FXML wired to handleToolbarSave → saveActiveTab() → saveTab(); disable binding to dirty property confirmed in code |
| ERR-07 | 27-02-PLAN.md | Developer can click chat button and browser opens ChatGPT with pre-filled prompt | SATISFIED (code) / human needed (runtime) | addEventFilter → addEventHandler swap confirmed; URL construction unchanged; browser open requires runtime verification |

Both requirements from REQUIREMENTS.md Phase 27 traceability table are accounted for. No orphaned requirements.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| None | - | No TODO/FIXME/placeholder patterns found in modified files | - | - |

No stub implementations detected. All handlers contain substantive logic — no empty bodies, no hardcoded `return null` or `return []` paths, no console-log-only implementations.

### Human Verification Required

#### 1. Save button enable/disable on dirty tab

**Test:** Open a project, open one file tab without editing, confirm Save button (floppy disk) is greyed out. Type one character — confirm Save button becomes enabled within the same event. Click Save — confirm the tab title `*` prefix disappears and Save button returns to disabled.
**Expected:** Button transitions disabled → enabled on first keystroke; disabled again after save
**Why human:** JavaFX BooleanBinding state transitions require a running app with an open project and active tab

#### 2. Tab-switch Save button state update

**Test:** Open two files, make one dirty. Switch between tabs and observe the Save button state.
**Expected:** Save button is enabled only when the dirty tab is active; immediately updates on tab switch
**Why human:** Requires interactive runtime verification of the setOnActiveTabChanged callback chain

#### 3. ChatGPT button opens browser with pre-filled URL

**Test:** Open a project with a broken XSLT, trigger a render error, observe an error row in the log panel. Click the chat bubble (💬) button on the error row.
**Expected:** Default browser opens at `https://chatgpt.com/?q=...` and the ChatGPT input field is pre-filled with the Italian preamble + error message
**Why human:** External browser interaction cannot be verified programmatically; ButtonBase arming requires runtime event dispatch

#### 4. ChatGPT button does not change log row selection

**Test:** In the log panel, select one error row, then click the 💬 button on a different row.
**Expected:** The originally selected row remains selected; the click on 💬 does NOT change the selection
**Why human:** Event bubbling suppression via consume() is only verifiable at runtime with a populated log panel

### Gaps Summary

No gaps found. All code-verifiable must-haves pass. Four items require human runtime validation due to JavaFX UI behavior that cannot be exercised from the shell.

---

_Verified: 2026-05-01T08:00:00Z_
_Verifier: Claude (gsd-verifier)_
