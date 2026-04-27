---
phase: 24-keyboard-accelerators
verified: 2026-04-27T20:30:00Z
status: human_needed
score: 6/7 must-haves verified
overrides_applied: 0
human_verification:
  - test: "Launch the fat JAR and exercise all five new keyboard accelerators plus verify no regression on F5 and Shortcut+Shift+F"
    expected: "Shortcut+O opens Open Project chooser; Shortcut+N opens New File dialog; Shortcut+Q triggers exit; Shortcut+Shift+E sets entrypoint on selected file (no-op when disabled); Shortcut+Shift+I sets XML input on selected file; F5 triggers Render; Shortcut+Shift+F opens Find in Files"
    why_human: "Runtime keyboard event dispatch through JavaFX scene graph cannot be verified by static FXML analysis. Accelerator registration is correct in FXML, but actual invocation of the bound handlers (especially KBD-04/05 whose handlers are wired in FileTreeController.initialize(), not FXML onAction) requires a live JVM session."
---

# Phase 24: Keyboard Accelerators Verification Report

**Phase Goal:** Add keyboard accelerators (keyboard shortcuts) to all File menu items so developers can trigger every File action from the keyboard without touching the mouse.
**Verified:** 2026-04-27T20:30:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Pressing Shortcut+O opens the Open Project file chooser | VERIFIED | `main.fxml` line 22: `accelerator="Shortcut+O"` on `menuItemOpenProject` with `onAction="#handleOpenProject"` |
| 2 | Pressing Shortcut+N opens the New File dialog | VERIFIED | `main.fxml` line 34: `accelerator="Shortcut+N"` on `menuItemNewFile` with `onAction="#handleNewFile"` |
| 3 | Pressing Shortcut+Q triggers the application exit flow | VERIFIED | `main.fxml` line 38: `accelerator="Shortcut+Q"` on Exit MenuItem with `onAction="#handleExit"` |
| 4 | Pressing Shortcut+Shift+E invokes Set Entrypoint on the selected file (when enabled) | VERIFIED | `main.fxml` line 27: `accelerator="Shortcut+Shift+E"` on `menuItemSetEntrypoint`; no `onAction` in FXML (correct — handler bound in `FileTreeController.initialize()`) |
| 5 | Pressing Shortcut+Shift+I invokes Set XML Input on the selected file (when enabled) | VERIFIED | `main.fxml` line 30: `accelerator="Shortcut+Shift+I"` on `menuItemSetXmlInput`; no `onAction` in FXML (correct — handler bound in `FileTreeController.initialize()`) |
| 6 | App starts without FXML LoadException (accelerator strings parse via KeyCombination.valueOf) | VERIFIED | All five accelerator strings use the `Shortcut+` token or `Shortcut+Shift+` compound — these are valid `KeyCombination.valueOf()` inputs per JavaFX 21. Fat JAR exists at `build/libs/xsleditor.jar` confirming `shadowJar` succeeded. |
| 7 | All five shortcuts fire the correct handler at runtime (live app smoke test) | UNCERTAIN — human needed | Static FXML analysis confirms wiring; runtime dispatch must be confirmed by a human. KBD-04/05 use `FileTreeController` handler binding (no FXML `onAction`) — this code path requires live verification. |

**Score:** 6/7 truths verified (1 requires human)

---

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` | Five new accelerator attributes on File menu items | VERIFIED | All five attributes present at lines 22, 27, 30, 34, 38. Multi-line attribute format matches existing `menuItemRender` analog. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `menuItemOpenProject` (line 20-23) | `#handleOpenProject` in MainController | `accelerator="Shortcut+O"` + existing `onAction` | VERIFIED | Both attributes present in multi-line block. `onAction="#handleOpenProject"` at line 23. |
| `menuItemSetEntrypoint` (line 25-27) | FileTreeController.setOnAction binding | `accelerator="Shortcut+Shift+E"` (NO onAction in FXML) | VERIFIED | Accelerator present at line 27. `onAction` absent — confirmed by `awk` returning 0. |
| `menuItemSetXmlInput` (line 28-30) | FileTreeController.setOnAction binding | `accelerator="Shortcut+Shift+I"` (NO onAction in FXML) | VERIFIED | Accelerator present at line 30. `onAction` absent — confirmed by `awk` returning 0. |
| `menuItemNewFile` (line 32-35) | `#handleNewFile` in MainController | `accelerator="Shortcut+N"` + existing `onAction` | VERIFIED | Both attributes present. `onAction="#handleNewFile"` at line 35. |
| Exit MenuItem (line 37-39) | `#handleExit` in MainController | `accelerator="Shortcut+Q"` + existing `onAction` | VERIFIED | Both attributes present. No `fx:id` — intentional per plan. |

### Data-Flow Trace (Level 4)

Not applicable. This phase modifies FXML attributes only; no data rendering or state management is involved. Accelerator registration is a declarative wiring mechanism, not a data pipeline.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Exactly 5 new Shortcut+ accelerators on File menu items | `grep -c 'accelerator="Shortcut+' main.fxml` | 6 (5 new File menu + 1 updated Find in Files) | PASS — The 6th entry is `Shortcut+Shift+F` on Find in Files, changed from `Ctrl+Shift+F` via user-approved improvement (commit `b736413`). All 5 new File menu accelerators are present. |
| Shortcut+O present | `grep -n 'accelerator="Shortcut+O"'` | Line 22 | PASS |
| Shortcut+N present | `grep -n 'accelerator="Shortcut+N"'` | Line 34 | PASS |
| Shortcut+Q present | `grep -n 'accelerator="Shortcut+Q"'` | Line 38 | PASS |
| Shortcut+Shift+E present | `grep -n 'accelerator="Shortcut+Shift+E"'` | Line 27 | PASS |
| Shortcut+Shift+I present | `grep -n 'accelerator="Shortcut+Shift+I"'` | Line 30 | PASS |
| KBD-04 menuItemSetEntrypoint has no onAction | `awk '/menuItemSetEntrypoint/,/\/>/` pipe grep -c onAction` | 0 | PASS |
| KBD-05 menuItemSetXmlInput has no onAction | `awk '/menuItemSetXmlInput/,/\/>/` pipe grep -c onAction` | 0 | PASS |
| Pre-existing F5 accelerator preserved | `grep -n 'accelerator="F5"'` | Line 46 | PASS |
| Find in Files accelerator present (user-approved Shortcut+Shift+F) | `grep -n 'accelerator="Shortcut+Shift+F"'` | Line 51 | PASS |
| Fat JAR exists | `ls build/libs/` | `xsleditor.jar`, `xsleditor-0.4.0.jar` | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|---------|
| KBD-01 | 24-01-PLAN.md | User can open a project via keyboard with Shortcut+O | SATISFIED | `accelerator="Shortcut+O"` on `menuItemOpenProject` (line 22) |
| KBD-02 | 24-01-PLAN.md | User can create a new file via keyboard with Shortcut+N | SATISFIED | `accelerator="Shortcut+N"` on `menuItemNewFile` (line 34) |
| KBD-03 | 24-01-PLAN.md | User can exit the application via keyboard with Shortcut+Q | SATISFIED | `accelerator="Shortcut+Q"` on Exit MenuItem (line 38) |
| KBD-04 | 24-01-PLAN.md | User can set XSLT entrypoint via keyboard with Shortcut+Shift+E | SATISFIED (pending runtime confirmation) | `accelerator="Shortcut+Shift+E"` on `menuItemSetEntrypoint` (line 27), no spurious `onAction` |
| KBD-05 | 24-01-PLAN.md | User can set XML input via keyboard with Shortcut+Shift+I | SATISFIED (pending runtime confirmation) | `accelerator="Shortcut+Shift+I"` on `menuItemSetXmlInput` (line 30), no spurious `onAction` |

All five Phase 24 requirement IDs are claimed by plan 24-01 and verified in `main.fxml`. No orphaned requirements for Phase 24.

REQUIREMENTS.md also lists EDIT-10 through EDIT-13 mapped to Phase 25 — these are out of scope for Phase 24 and correctly deferred.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| — | — | — | — | None found |

No TODOs, FIXMEs, placeholders, stubs, or empty implementations detected in the modified file. The change is purely additive (FXML attribute additions).

One item to note — NOT a gap: The plan's acceptance criterion stated "`grep -c 'accelerator=\"Shortcut+'` returns exactly 5". The actual count is 6. The extra entry is `Shortcut+Shift+F` on Find in Files (line 51), changed from `Ctrl+Shift+F` per user approval during the human checkpoint. This is an intentional improvement, not a regression.

---

### Human Verification Required

#### 1. Full Keyboard Accelerator Smoke Test

**Test:** Launch the app with `java -jar build/libs/xsleditor.jar`. With the app focused:
1. Press `Cmd+O` (macOS) / `Ctrl+O` (Win/Linux) — expected: Open Project directory chooser opens.
2. Cancel. Open any project so the file tree is populated.
3. Press `Cmd+N` / `Ctrl+N` — expected: New File dialog opens.
4. Cancel. Select any file in the file tree.
5. Press `Cmd+Shift+E` / `Ctrl+Shift+E` — expected: selected file set as XSLT entrypoint.
6. Select a different file. Press `Cmd+Shift+I` / `Ctrl+Shift+I` — expected: selected file set as XML input.
7. Press `Cmd+Q` / `Ctrl+Q` — expected: exit flow runs (close confirmation or direct exit).

**Regression checks:**
- Press `F5` — expected: Render triggers.
- Press `Cmd+Shift+F` / `Ctrl+Shift+F` — expected: Find in Files opens.

**Expected:** All five new shortcuts fire the correct action. No FXML LoadException in console at startup. F5 and Shortcut+Shift+F not regressed.

**Why human:** JavaFX accelerator dispatch is a runtime event that cannot be verified by static analysis. KBD-04 and KBD-05 are particularly important to smoke-test: their handlers are registered by `FileTreeController.initialize()` via `setOnAction()` at runtime, not via `onAction` in FXML. A static check cannot confirm the runtime binding is invoked when the accelerator fires.

---

### Gaps Summary

No blocker gaps. All five accelerator attributes are present in `main.fxml` with the correct token (`Shortcut+`), correct key combinations, correct attribute ordering, and correct absence of `onAction` for KBD-04/05. Pre-existing accelerators are preserved (F5 unchanged; Find in Files improved from `Ctrl+` to `Shortcut+` per user approval).

The single outstanding item is the mandatory human smoke-test (Task 2 of the plan), which was designed as a blocking checkpoint from the outset. Automated verification is complete and passes. Human confirmation of runtime behavior is the final gate.

---

_Verified: 2026-04-27T20:30:00Z_
_Verifier: Claude (gsd-verifier)_
