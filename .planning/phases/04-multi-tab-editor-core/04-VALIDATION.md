---
phase: 4
slug: multi-tab-editor-core
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-18
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + TestFX (JavaFX UI testing) |
| **Config file** | `build.gradle` — `test { useJUnitPlatform() }` |
| **Quick run command** | `./gradlew test --tests "com.xlseditor.editor.*"` |
| **Full suite command** | `./gradlew test` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew test --tests "com.xlseditor.editor.*"`
- **After every plan wave:** Run `./gradlew test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 4-01-01 | 01 | 1 | EDIT-01 | — | N/A | unit | `./gradlew test --tests "*.EditorControllerTest.testOpenFileCreatesTab"` | ❌ W0 | ⬜ pending |
| 4-01-02 | 01 | 1 | EDIT-01 | — | N/A | unit | `./gradlew test --tests "*.EditorControllerTest.testDuplicateOpenSwitchesTab"` | ❌ W0 | ⬜ pending |
| 4-01-03 | 01 | 1 | EDIT-02 | — | N/A | unit | `./gradlew test --tests "*.EditorControllerTest.testDirtyStateOnEdit"` | ❌ W0 | ⬜ pending |
| 4-01-04 | 01 | 1 | EDIT-03 | — | N/A | unit | `./gradlew test --tests "*.EditorControllerTest.testSaveFileClearsDirty"` | ❌ W0 | ⬜ pending |
| 4-01-05 | 01 | 1 | EDIT-09 | — | N/A | manual | Tab shows `*` prefix when dirty; title cleared on save | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/com/xlseditor/editor/EditorControllerTest.java` — stubs for EDIT-01, EDIT-02, EDIT-03, EDIT-09
- [ ] Verify `testImplementation("org.testfx:testfx-junit5:4.0.18")` in `build.gradle`

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Tab title shows `*` prefix when dirty | EDIT-09 | JavaFX `Tab.textProperty()` binding requires rendering | 1. Open a file. 2. Edit content. 3. Verify tab title = `*filename`. 4. Ctrl+S. 5. Verify title = `filename` (no `*`). |
| Close-tab confirmation dialog when dirty | EDIT-02 | Modal dialog requires UI interaction | 1. Open file. 2. Edit. 3. Click X on tab. 4. Verify dialog appears. 5. Cancel → tab stays. 6. Repeat, confirm close → tab removed. |
| Ctrl+S saves current file | EDIT-03 | File I/O + keyboard shortcut | 1. Open file. 2. Edit. 3. Ctrl+S. 4. Check file on disk has new content. |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
