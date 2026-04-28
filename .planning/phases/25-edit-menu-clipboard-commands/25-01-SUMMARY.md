---
phase: 25-edit-menu-clipboard-commands
plan: "01"
subsystem: ui/javafx
tags:
  - javafx
  - richtextfx
  - menubar
  - clipboard
dependency_graph:
  requires:
    - keyboard-accelerators-file-menu
  provides:
    - edit-menu-clipboard-commands
  affects:
    - src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java
    - src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java
    - src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
tech_stack:
  added: []
  patterns:
    - Optional<CodeArea> return type for null-safe delegation
    - ifPresent(CodeArea::method) method reference pattern
    - Shortcut+ token for cross-platform FXML accelerators
key_files:
  created: []
  modified:
    - src/main/java/ch/ti/gagi/xsleditor/ui/EditorController.java
    - src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java
    - src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
decisions:
  - "getActiveCodeArea() placed on EditorController (not MainController) — keeps clipboard logic co-located with tab/CodeArea ownership"
  - "Optional<CodeArea> return type avoids NPE at call sites; ifPresent() is the idiomatic null-safe delegation"
  - "No fx:id on the four new MenuItems — no programmatic disable bindings needed"
  - "SeparatorMenuItem between Paste and Select All per HIG grouping convention"
  - "Shortcut+ token used (not Ctrl+) for cross-platform support — macOS Cmd, Win/Linux Ctrl"
metrics:
  duration: "~20 minutes"
  files_changed: 3
  lines_added: ~57
requirements_closed:
  - EDIT-10
  - EDIT-11
  - EDIT-12
  - EDIT-13
---

## What Was Built

Added Cut, Copy, Paste, and Select All to the previously empty Edit menu, delegating each command to the active RichTextFX `CodeArea` via a new `EditorController.getActiveCodeArea()` method.

**3 files modified, ~57 lines net addition:**

- `EditorController.java`: new `public Optional<CodeArea> getActiveCodeArea()` method using the established `getUserData() instanceof EditorTab et` pattern. No new imports needed (Tab, CodeArea, Optional already imported).
- `MainController.java`: added `import org.fxmisc.richtext.CodeArea` and four `@FXML` handlers (`handleEditCut/Copy/Paste/SelectAll`), each a one-liner delegating via `editorController.getActiveCodeArea().ifPresent(CodeArea::method)`.
- `main.fxml`: replaced `<Menu text="Edit"/>` empty element with a populated menu containing Cut, Copy, Paste, SeparatorMenuItem, Select All — each with `Shortcut+X/C/V/A` accelerator and the corresponding `onAction` reference.

## Manual Smoke Test Results (Windows)

All 8 checks passed (operator-approved):

1. ✓ Edit > Select All — selects all text; menu shows accelerator hint
2. ✓ Edit > Copy — text copied to system clipboard; editor unchanged
3. ✓ Edit > Cut — selected text removed from editor; dirty indicator appears; text available in clipboard
4. ✓ Edit > Paste — clipboard text inserted at cursor
5. ✓ Accelerator hints visible in Edit menu (Ctrl+X/C/V/A on Windows)
6. ✓ RichTextFX native keyboard shortcuts (Ctrl+A/C/X/V) work independently
7. ✓ No-tab no-op — no crash, no error log entry when no tab is open
8. ✓ SeparatorMenuItem visible between Paste and Select All

## Issues / Deviations

- **Pre-existing test failures (2 of 99):** Two tests were already failing on HEAD before this phase. Not introduced by Phase 25 — confirmed via git stash isolation.
- **About dialog version not updated (Windows):** Version display in About dialog is a pre-existing issue unrelated to EDIT-10..13. Noted for future investigation.
- **build.gradle default version bumped 0.3.0 → 0.4.1** as part of this phase delivery (minor housekeeping, separate commit).

## Tech Debt / Follow-ups

- Undo/Redo (EDIT-14/15) still deferred — no menu items added for those
- No automated test for clipboard behavior — JavaFX clipboard requires a display; headless test environment cannot verify system clipboard integration
- `getActiveCodeArea()` is a reusable API surface for any future MenuBar action targeting the active editor

## Self-Check: PASSED
