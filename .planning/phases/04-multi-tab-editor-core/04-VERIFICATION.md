---
phase: 04-multi-tab-editor-core
verified: 2026-04-21T00:00:00Z
status: passed
score: 12/12
overrides_applied: 0
human_verification:
  - test: "Launch `./gradlew run`, open a project, double-click file A, double-click file B, then double-click A again"
    expected: "Exactly 2 tabs appear; second double-click on A focuses the existing tab without duplicating it (EDIT-01 dedup). Phase 1 placeholder Label is NOT visible behind the TabPane (Pitfall 5 check)."
    why_human: "Tab dedup and placeholder removal require a running JavaFX UI. Cannot verify tab selection model or visual layer ordering via grep."
  - test: "With a tab open and clean, type a character; then Ctrl+Z back to original"
    expected: "Tab title immediately shows `*filename` on edit; `*` drops without save after undo (EDIT-02 reactive binding via UndoManager.atMarkedPosition)."
    why_human: "Reactive UI binding on keystroke-level events requires a running application."
  - test: "With a dirty tab (shows `*`), press Ctrl+S"
    expected: "Tab title reverts to `filename`; file content on disk matches buffer (EDIT-03)."
    why_human: "Requires keyboard focus in CodeArea and disk-state inspection outside the JVM."
  - test: "With a dirty tab, click the tab X close button; click CANCEL in the dialog; then click X again and click YES"
    expected: "CANCEL keeps the tab open with edits intact; YES closes the tab and discards changes (EDIT-09)."
    why_human: "Modal Alert interaction and tab close event are UI-only behaviours."
  - test: "Close a tab cleanly, then double-click the same file in the tree"
    expected: "A new tab opens immediately — registry was cleaned up by setOnClosed (Pitfall 3 regression check)."
    why_human: "Registry cleanup timing requires observing runtime state."
  - test: "With a dirty tab open, click the window close (X) button"
    expected: "The Phase 1 aggregate close-confirmation dialog appears (Pitfall 4 — dirtyCallback propagated to MainController.setDirty)."
    why_human: "End-to-end dirty propagation to window close requires running app."
---

# Phase 4: Multi-Tab Editor Core — Verification Report

**Phase Goal:** Multi-tab code editor with RichTextFX, file open/save, and dirty state tracking.
**Verified:** 2026-04-18T16:30:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | EditorTab is not dirty immediately after construction | VERIFIED | `EditorTab.java` line 68: `Bindings.not(um.atMarkedPositionProperty())`; load order replaceText→mark→forgetHistory confirmed (lines 57–65); `EditorTabTest.newTabIsNotDirtyAfterLoad` exists and is structured to pass |
| 2 | EditorTab becomes dirty after any text change | VERIFIED | Dirty binding is `NOT atMarkedPosition`; any edit moves UndoManager off marked position; `EditorTabTest.tabBecomesDirtyAfterEdit` |
| 3 | EditorTab returns to clean when mark() is called (simulates save) | VERIFIED | `EditorTabTest.tabBecomesCleanAfterMarkCall` present; saveTab() calls `getUndoManager().mark()` (EditorController line 178) |
| 4 | UndoManager.forgetHistory() called after load | VERIFIED | EditorTab constructor line 65; `EditorTabTest.undoHistoryClearedAfterLoad` asserts `isUndoAvailable()` is false |
| 5 | EditorController owns a TabPane and Map<Path,Tab> registry with normalized-path keys (EDIT-01) | VERIFIED | EditorController lines 47–48: `private TabPane tabPane` + `private final Map<Path, Tab> registry = new LinkedHashMap<>()`; `openOrFocusTab` uses `toAbsolutePath().normalize()` (line 118) |
| 6 | openOrFocusTab creates one tab per file, reads via Files.readString UTF-8, focuses existing tabs (EDIT-01) | VERIFIED | Lines 118–138: registry.containsKey early-return; Files.readString(key, StandardCharsets.UTF_8) |
| 7 | Tab title shows `*` prefix when dirty, drops prefix when clean (EDIT-02) | VERIFIED | EditorController lines 147–150: `editorTab.dirty.addListener(... tab.setText(isDirty ? "*" + baseName : baseName) ...)` |
| 8 | Ctrl+S on focused CodeArea writes to disk and calls mark() (EDIT-03) | VERIFIED | Lines 152–154: `Nodes.addInputMap(editorTab.codeArea, consume(keyPressed(S, CONTROL_DOWN), e -> saveTab(editorTab)))`; saveTab writes via Files.writeString UTF-8 and calls mark() |
| 9 | Closing dirty tab shows YES/CANCEL confirmation; CANCEL cancels close (EDIT-09) | VERIFIED | Lines 157–170: setOnCloseRequest with Alert.CONFIRMATION, ButtonType.YES/CANCEL, event.consume() on non-YES result |
| 10 | Aggregate dirty state propagated via dirtyCallback on every dirty transition, save, and close | VERIFIED | updateAppDirtyState() called in dirty listener (line 149), saveTab (line 179), and setOnClosed (line 130); initial seed at initialize() line 75 |
| 11 | MainController owns EditorController instance, wired via initialize() and connected to FileTree seam | VERIFIED | MainController line 63: `private final EditorController editorController = new EditorController()`; initialize() lines 82–88: editorController.initialize(...) then setOnFileOpenRequest(editorController::openOrFocusTab) |
| 12 | UI behaviours work correctly at runtime (EDIT-01 dedup, EDIT-02 reactive title, EDIT-03 Ctrl+S, EDIT-09 close dialog) | HUMAN NEEDED | The 04-03-SUMMARY.md reports human approval, but per verification policy this requires confirmation at verification time. Code structure is correct; runtime confirmation needed. |

**Score:** 11/12 truths verified automatically (1 requires human confirmation)

---

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/java/ch/ti/gagi/xlseditor/ui/EditorTab.java` | Per-file model: path + CodeArea + dirty BooleanBinding | VERIFIED | 70 lines; `public final class EditorTab`; all 3 public final fields present; load-order contract correct; no @FXML, no textProperty listener |
| `src/test/java/ch/ti/gagi/xlseditor/ui/EditorTabTest.java` | 4 JUnit 5 tests for EDIT-02 dirty-state logic | VERIFIED | 50 lines; all 4 test methods present with correct names; Platform.startup bootstrap; assertion messages include "EDIT-02" |
| `src/main/java/ch/ti/gagi/xlseditor/ui/EditorController.java` | Sub-controller: TabPane, open/save/close, registry, Ctrl+S, close confirmation | VERIFIED | 186 lines; `public final class EditorController`; no @FXML; openOrFocusTab, buildTab, saveTab, initialize, showError, updateAppDirtyState all present |
| `src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java` | Integration wiring: EditorController field + initialize + seam hookup | VERIFIED | EditorController field on line 63; editorController.initialize() on line 82; setOnFileOpenRequest seam on line 88 |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| EditorTab.java | UndoManager.atMarkedPositionProperty() | `Bindings.not(um.atMarkedPositionProperty())` (line 68) | WIRED | Exact idiom present |
| EditorTab constructor | load-order contract | replaceText (line 57) → mark() (line 62) → forgetHistory() (line 65) | WIRED | Correct sequence confirmed via multiline grep |
| EditorController.java | EditorTab.java | `new EditorTab(key, content)` in openOrFocusTab (line 125) | WIRED | Direct instantiation |
| EditorController.java | WellBehavedFX Nodes.addInputMap | `Nodes.addInputMap(editorTab.codeArea, consume(keyPressed(S, CONTROL_DOWN), ...)` (lines 153–154) | WIRED | Per-CodeArea binding, not scene-level |
| EditorController.java | java.nio.file.Files | `Files.readString(key, StandardCharsets.UTF_8)` (line 124); `Files.writeString(editorTab.path, ..., StandardCharsets.UTF_8)` (line 177) | WIRED | Both UTF-8 charset explicit |
| EditorController.java | Tab.setOnClosed / setOnCloseRequest | setOnCloseRequest (line 157) for confirmation; setOnClosed (line 128) for registry cleanup | WIRED | Distinct handlers, correct responsibility split |
| MainController.java | EditorController.java | `editorController.initialize(editorPane, () -> primaryStage, this::setDirty)` (line 82) | WIRED | Deferred stage supplier pattern correct |
| MainController.java | FileTreeController.java | `fileTreeController.setOnFileOpenRequest(editorController::openOrFocusTab)` (line 88) | WIRED | Phase 3 D-05 seam activated |

---

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| EditorController (tab content display) | `content` String in openOrFocusTab | `Files.readString(key, StandardCharsets.UTF_8)` (line 124) | Yes — reads actual file from disk | FLOWING |
| EditorController (dirty aggregate) | `anyDirty` in updateAppDirtyState | streams registry values, checks `et.dirty.get()` against EditorTab BooleanBinding | Yes — driven by UndoManager state | FLOWING |
| EditorController (save) | `editorTab.codeArea.getText()` | in-memory CodeArea buffer written via Files.writeString | Yes — writes real buffer to disk | FLOWING |

---

### Behavioral Spot-Checks

Step 7b: SKIPPED — JavaFX application requires a display. Tests run headless and cannot exercise UI rendering. The `./gradlew test` target was reported as BUILD SUCCESSFUL; the 4 EditorTabTest tests are the only automated behavioral checks possible without a display.

---

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| EDIT-01 | 04-02-PLAN, 04-03-PLAN | Multi-tab layout, one tab per open file, dedup | SATISFIED | openOrFocusTab with registry.containsKey dedup and toAbsolutePath().normalize() key; seam wired to FileTree |
| EDIT-02 | 04-01-PLAN, 04-02-PLAN | Dirty indicator `*` in tab title | SATISFIED | EditorTab BooleanBinding; EditorController dirty listener on tab title |
| EDIT-03 | 04-02-PLAN | Ctrl+S saves current file and clears dirty | SATISFIED | per-CodeArea Nodes.addInputMap; saveTab writes UTF-8 and calls mark() |
| EDIT-09 | 04-02-PLAN, 04-03-PLAN | Close-tab confirmation when dirty | SATISFIED | setOnCloseRequest with YES/CANCEL Alert; event.consume() on CANCEL |

**Note on REQUIREMENTS.md traceability table:** The table (line 128) lists `EDIT-04..09` under Phase 5, which would imply EDIT-09 belongs to Phase 5. However, ROADMAP.md Phase 4 entry explicitly lists `EDIT-09` as a Phase 4 requirement. All three Phase 4 plans (04-02-PLAN, 04-03-PLAN) claim EDIT-09, and it is fully implemented. The REQUIREMENTS.md traceability table is inconsistent with ROADMAP.md — ROADMAP.md takes precedence per verification protocol. EDIT-09 is correctly implemented and satisfied in Phase 4.

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | — | — | — | — |

No TODO/FIXME/placeholder comments found. No empty return null/empty list implementations in phase-introduced code. No shadow dirty boolean field in EditorController. No scene-level Ctrl+S handler. No MainController import in EditorController. No @FXML annotation in EditorController (only in Javadoc comment mentioning the design intent). The `dirtyCallback.accept(false)` initial seed is not a stub — it is an intentional baseline call.

---

### Human Verification Required

The 04-03-SUMMARY.md documents that a human verified all 6 scenarios and approved them on 2026-04-18. Because the verification protocol requires confirming human tests at verification time (not trusting SUMMARY claims), the following scenarios must be re-confirmed by running the application:

#### 1. EDIT-01 Multi-tab dedup and placeholder removal

**Test:** `./gradlew run`; open a project; double-click file A — tab opens; double-click file B — second tab opens; double-click A again.
**Expected:** Exactly 2 tabs; focus moves to A's existing tab; no duplication. "Editor" placeholder Label not visible.
**Why human:** Tab selection model and visual layer cannot be verified via grep.

#### 2. EDIT-02 Reactive dirty indicator

**Test:** With A.xsl clean, type a character.
**Expected:** Tab title immediately becomes `*A.xsl`. Undo until original content is restored — `*` disappears.
**Why human:** Reactive binding on keystroke events requires live UI.

#### 3. EDIT-03 Ctrl+S save

**Test:** Edit A.xsl so it is dirty; press Ctrl+S.
**Expected:** Title reverts to `A.xsl`; file content on disk matches the edited buffer.
**Why human:** Keyboard focus in CodeArea and disk-state verification require a running application.

#### 4. EDIT-09 Close-tab confirmation

**Test:** Dirty tab — click X; click CANCEL; confirm tab stays open. Click X again; click YES; confirm tab closes.
**Expected:** CANCEL cancels close; YES discards and closes.
**Why human:** Modal Alert interactions and tab close event require a running JavaFX scene.

#### 5. Pitfall 3 regression — reopen after close

**Test:** Close a tab cleanly; double-click same file in tree.
**Expected:** New tab opens immediately (registry cleanup via setOnClosed worked correctly).
**Why human:** Registry state timing is only observable at runtime.

#### 6. Pitfall 4 — window-close aggregate dirty

**Test:** Dirty tab open; click window X.
**Expected:** The Phase 1 aggregate close-confirmation dialog fires.
**Why human:** Cross-controller dirty propagation end-to-end requires a running application.

---

### Gaps Summary

No automated verification gaps found. All code artifacts exist, are substantive, and are correctly wired. The only outstanding item is the human verification confirmation of runtime behaviours — which is expected for a JavaFX UI phase and is structurally identical to the checkpoint that was already performed and documented in 04-03-SUMMARY.md (human approval recorded as "approved" for all 6 scenarios).

If the human approval documented in 04-03-SUMMARY.md is accepted as sufficient, status is `passed`. If the verification protocol requires a fresh human sign-off at verification time, status remains `human_needed`.

---

_Verified: 2026-04-18T16:30:00Z_
_Verifier: Claude (gsd-verifier)_
