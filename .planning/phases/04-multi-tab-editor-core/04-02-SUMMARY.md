---
phase: 04-multi-tab-editor-core
plan: "02"
subsystem: editor-controller
tags:
  - javafx
  - richtextfx
  - sub-controller
  - multi-tab
dependency_graph:
  requires:
    - 04-01  # EditorTab model
  provides:
    - EditorController.openOrFocusTab(Path)
    - EditorController.initialize(StackPane, Supplier<Stage>, Consumer<Boolean>)
  affects:
    - MainController (Plan 03 wires editorController field)
    - FileTreeController (Plan 03 sets onFileOpenRequest seam)
tech_stack:
  added: []
  patterns:
    - TabPane registry dedup via toAbsolutePath().normalize()
    - WellBehavedFX Nodes.addInputMap for per-CodeArea Ctrl+S (not scene-level)
    - VirtualizedScrollPane wrapping CodeArea (flowless)
    - Dirty-title binding via EditorTab.dirty BooleanBinding listener
    - setOnCloseRequest + event.consume() for cancellable close confirmation
    - setOnClosed for registry cleanup (distinct from setOnCloseRequest)
    - Consumer<Boolean> dirtyCallback for aggregate dirty propagation
key_files:
  created:
    - src/main/java/ch/ti/gagi/xlseditor/ui/EditorController.java
  modified: []
decisions:
  - "Both tasks implemented atomically in one file creation; skeleton + methods written together since all acceptance criteria were verifiable in a single compile pass"
  - "Objects.requireNonNull used with message strings (editorPane, primaryStageSupplier, dirtyCallback) for clearer NPE diagnostics"
  - "openOrFocusTab includes requireNonNull on path parameter per threat model T-04-05 (path traversal defence-in-depth)"
metrics:
  duration: "2m 10s"
  completed: "2026-04-18"
  tasks_completed: 2
  files_created: 1
  files_modified: 0
---

# Phase 04 Plan 02: EditorController Summary

**One-liner:** Multi-tab EditorController with registry dedup, per-CodeArea Ctrl+S via WellBehavedFX, dirty-title binding, and YES/CANCEL close confirmation.

---

## Method Signatures Exposed (for Plan 03 wiring)

```java
// Initialize — call from MainController.initialize() after @FXML injection
public void initialize(
    StackPane editorPane,
    Supplier<Stage> primaryStageSupplier,
    Consumer<Boolean> dirtyCallback
)

// Open or focus — wire to FileTreeController.setOnFileOpenRequest(...)
public void openOrFocusTab(Path path)
```

**Plan 03 handoff note:** Plan 03 must call:
```java
editorController.initialize(editorPane, () -> primaryStage, this::setDirty);
fileTreeController.setOnFileOpenRequest(editorController::openOrFocusTab);
```

---

## File Metrics

| File | Lines | Size |
|------|-------|------|
| `EditorController.java` | 186 | ~7.2 KB |

---

## Test Results

- `./gradlew build -x test`: PASS (BUILD SUCCESSFUL)
- `./gradlew test`: PASS (BUILD SUCCESSFUL, existing EditorTabTest suite green)

---

## Pitfall Compliance Checklist

| Pitfall | Description | Status | Evidence |
|---------|-------------|--------|----------|
| Pitfall 1 | Tab opens as dirty | N/A (in EditorTab, Plan 01) | EditorTabTest green |
| Pitfall 2 | Close dialog not cancelling | COMPLIANT | `event.consume()` in setOnCloseRequest; `grep -c "event.consume()" = 1` |
| Pitfall 3 | File can't be reopened after close | COMPLIANT | `tab.setOnClosed` removes from registry; `grep -c "tab.setOnClosed" = 1` |
| Pitfall 4 | Window close ignores editor dirty | COMPLIANT | `updateAppDirtyState()` called from dirty listener, saveTab, and setOnClosed |
| Pitfall 5 | Ghost placeholder Label | COMPLIANT | `editorPane.getChildren().setAll(tabPane)` — `grep -c "getChildren().add(tabPane)" = 0` |

---

## EDIT Requirement Coverage

| Requirement | Description | Implementation |
|-------------|-------------|----------------|
| EDIT-01 | Dedup via normalized path | `toAbsolutePath().normalize()` as registry key; `registry.containsKey(key)` before open |
| EDIT-02 | Dirty `*` prefix on tab title | `editorTab.dirty.addListener(...)` toggles `"*" + baseName` / `baseName` |
| EDIT-03 | Ctrl+S saves to disk | `Nodes.addInputMap(codeArea, consume(keyPressed(S, CONTROL_DOWN), e -> saveTab(...)))` |
| EDIT-09 | Close confirmation when dirty | `setOnCloseRequest` + Alert CONFIRMATION + `event.consume()` on CANCEL/empty |

---

## Threat Model Compliance

| Threat | Disposition | Implementation |
|--------|-------------|----------------|
| T-04-05 Path traversal | mitigate | `toAbsolutePath().normalize()` applied in `openOrFocusTab` before registry lookup and `Files.readString` |
| T-04-06 Save outside project root | mitigate | `saveTab` writes to `editorTab.path` (the already-normalized key, immutable after construction) |
| T-04-07 Large file DoS | accept | Noted — no background I/O in Phase 4; typical XSLT < 1 MB |
| T-04-08 Path in error dialogs | accept | `getFileName()` only (not full absolute path) |
| T-04-09 Charset mismatch | accept | UTF-8 assumption (RESEARCH.md A3); XML encoding declaration visible to user |
| T-04-10 IOException silently ignored | mitigate | `saveTab` catches and shows Alert.ERROR; dirty stays true, `*` prefix persists |

---

## Grep Verification Evidence

```
grep -c "public final class EditorController"     => 1  PASS
grep -c "private final Map<Path, Tab> registry"   => 1  PASS
grep -c "editorPane.getChildren().setAll(tabPane)"=> 1  PASS
grep -n "getChildren().add(tabPane)"              => (none) PASS
grep -c "Objects.requireNonNull"                  => 4  PASS (>=3)
grep -c "Alert.AlertType.ERROR"                   => 1  PASS
grep -c "dirtyCallback.accept(false)"             => 1  PASS
grep -n "@FXML" (actual annotation)               => (none) PASS
grep -c "public void openOrFocusTab(Path path)"   => 1  PASS
grep -c "private Tab buildTab(EditorTab editorTab)"=> 1  PASS
grep -c "private void saveTab(EditorTab editorTab)"=> 1  PASS
grep -c "toAbsolutePath().normalize()"            => 1  PASS
grep -c "Files.readString(key, StandardCharsets"  => 1  PASS
grep -c "Files.writeString(editorTab.path"        => 1  PASS
grep -c "StandardCharsets.UTF_8"                  => 2  PASS (>=2)
grep -c "tab.setOnClosed"                         => 1  PASS
grep -c "tab.setOnCloseRequest"                   => 1  PASS
grep -c "event.consume()"                         => 1  PASS
grep -c "new VirtualizedScrollPane<>"             => 1  PASS
grep -c "Nodes.addInputMap"                       => 1  PASS
grep -cE "consume\(keyPressed\(S, CONTROL_DOWN\)" => 1  PASS
grep -c "ButtonType.YES"                          => 2  PASS (>=1)
grep -c "ButtonType.CANCEL"                       => 1  PASS
grep -c "tab.setUserData(editorTab)"              => 1  PASS
grep -n "scene.setOnKeyPressed"                   => (none) PASS
grep -n "import.*MainController"                  => (none) PASS
```

---

## Known Stubs

None. The controller is fully implemented. No placeholder text, hardcoded empty returns, or wired-but-empty data flows exist. Plan 03 will wire `MainController` to consume `EditorController`, but that is an intentional integration seam deferred to the next plan.

---

## Deviations from Plan

**Deviation:** Both tasks (Task 1 skeleton + Task 2 methods) were implemented atomically in a single file write rather than appending to an existing skeleton. This was safe because the file did not exist before this plan, and all acceptance criteria from both tasks were verified in one compile pass. The result is identical to the two-step plan description.

---

## Self-Check: PASSED

- `src/main/java/ch/ti/gagi/xlseditor/ui/EditorController.java` exists: CONFIRMED
- Commit `d39c6e4` exists: CONFIRMED via `git log`
- All grep assertions pass: CONFIRMED above
- `./gradlew test` exits 0: CONFIRMED
