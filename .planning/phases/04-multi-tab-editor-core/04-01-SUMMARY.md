---
phase: 04-multi-tab-editor-core
plan: "01"
subsystem: editor-model
tags:
  - javafx
  - richtextfx
  - model
  - test
  - dirty-state
dependency_graph:
  requires:
    - "RichTextFX 0.11.5 (already in build.gradle)"
    - "JUnit Jupiter 5.10.0 (already in build.gradle)"
  provides:
    - "EditorTab model class (path + CodeArea + BooleanBinding dirty)"
    - "EditorTabTest (4 JUnit 5 tests for EDIT-02 dirty-state logic)"
  affects:
    - "Plan 02: EditorController consumes new EditorTab(path, content)"
tech_stack:
  added: []
  patterns:
    - "UndoManager.atMarkedPositionProperty() via Bindings.not() for dirty detection"
    - "replaceText → mark() → forgetHistory() load-order contract (RESEARCH.md Pitfall 1)"
    - "Platform.startup() bootstrap in @BeforeAll for JavaFX unit tests without TestFX"
key_files:
  created:
    - src/main/java/ch/ti/gagi/xlseditor/ui/EditorTab.java
    - src/test/java/ch/ti/gagi/xlseditor/ui/EditorTabTest.java
  modified: []
decisions:
  - "Use Bindings.not(atMarkedPositionProperty()) for dirty binding (plan-specified idiom)"
  - "public final fields (data carrier pattern) — no setters, no @FXML"
  - "null-safe: requireNonNull on path, requireNonNullElse on content per threat model T-04-01/T-04-02"
metrics:
  duration_minutes: 8
  completed_date: "2026-04-18"
  tasks_completed: 2
  tasks_total: 2
  files_created: 2
  files_modified: 0
---

# Phase 04 Plan 01: EditorTab Model and Dirty-State Tests Summary

**One-liner:** EditorTab model with UndoManager-based dirty BooleanBinding and 4 JUnit 5 unit tests covering EDIT-02 dirty-state transitions.

---

## Objective

Introduce the `EditorTab` per-file state carrier (path, CodeArea, dirty BooleanBinding) and
cover its dirty-state logic with JUnit 5 unit tests, closing the Wave 0 gap in VALIDATION.md
for EDIT-02.

---

## Files Created

| File | Lines | Role |
|------|-------|------|
| `src/main/java/ch/ti/gagi/xlseditor/ui/EditorTab.java` | 70 | Per-file model: path + CodeArea + dirty binding |
| `src/test/java/ch/ti/gagi/xlseditor/ui/EditorTabTest.java` | 50 | 4 JUnit 5 tests for EDIT-02 dirty-state logic |

---

## Load-Order Contract (for future phase reference)

The constructor body of `EditorTab` **must** execute in this exact order (RESEARCH.md Pitfall 1):

1. `codeArea.replaceText(content)` — load file content FIRST
2. `um.mark()` — set "saved" baseline AFTER content is loaded
3. `um.forgetHistory()` — clear undo stack so Ctrl+Z cannot undo the load
4. `this.dirty = Bindings.not(um.atMarkedPositionProperty())` — bind dirty flag

Any other order causes the tab to appear dirty immediately on open (tab title shows `*` on load).

---

## Test Results

```
./gradlew test --tests "ch.ti.gagi.xlseditor.ui.EditorTabTest"  →  BUILD SUCCESSFUL (4/4 tests pass)
./gradlew test                                                   →  BUILD SUCCESSFUL (full suite green, no regressions)
```

Tests covered:
| Test | Behavior | Result |
|------|----------|--------|
| `newTabIsNotDirtyAfterLoad` | After construction, `dirty.get()` is `false` | PASS |
| `tabBecomesDirtyAfterEdit` | After `appendText(" ")`, `dirty.get()` is `true` | PASS |
| `tabBecomesCleanAfterMarkCall` | After edit + `mark()`, `dirty.get()` is `false` | PASS |
| `undoHistoryClearedAfterLoad` | After construction, `isUndoAvailable()` is `false` | PASS |

---

## Deviations from Plan

None — plan executed exactly as written.

The `Bindings.not(atMarkedPositionProperty())` grep check returns 2 instead of the expected 1
because the expression also appears in the Javadoc comment (line 35). Both occurrences are
correct and intentional; the implementation in code (line 68) is the authoritative one.

---

## Threat Model Compliance

| Threat ID | Disposition | Implementation |
|-----------|-------------|----------------|
| T-04-01 | mitigate | `Objects.requireNonNull(path, "path")` — NPE at construction |
| T-04-02 | mitigate | `Objects.requireNonNullElse(content, "")` — empty-string fallback |
| T-04-03 | accept | Tests use `Path.of("dummy.xsl")` — no real filesystem paths |
| T-04-04 | accept | No size cap; scope is XSLT/XSL-FO templates (< 1 MB) |

---

## Known Stubs

None. `EditorTab` is a complete, self-contained model with no placeholder values.

---

## Handoff Note

**Plan 02 `EditorController.openOrFocusTab` consumes `new EditorTab(path, content)` — no API changes expected.**

The `EditorTab` class is intentionally minimal: it owns only the `CodeArea` and the
`dirty` binding. `EditorController` (Plan 02) will:
- Wrap `editorTab.codeArea` in `VirtualizedScrollPane` for display
- Build a `Tab` instance with title binding on `editorTab.dirty`
- Wire Ctrl+S via `Nodes.addInputMap` on `editorTab.codeArea`
- Set `Tab.setOnCloseRequest` for close confirmation when dirty

---

## Self-Check: PASSED

| Item | Status |
|------|--------|
| `src/main/java/ch/ti/gagi/xlseditor/ui/EditorTab.java` exists | FOUND |
| `src/test/java/ch/ti/gagi/xlseditor/ui/EditorTabTest.java` exists | FOUND |
| `.planning/phases/04-multi-tab-editor-core/04-01-SUMMARY.md` exists | FOUND |
| Commit `ebebcf6` (feat: EditorTab model) | FOUND |
| Commit `0077535` (test: EditorTabTest) | FOUND |
