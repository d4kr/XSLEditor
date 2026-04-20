---
phase: 7
slug: pdf-preview-panel
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-20
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter 5.10.0 |
| **Config file** | `build.gradle` (`test { useJUnitPlatform() }`) |
| **Quick run command** | `./gradlew test --tests "ch.ti.gagi.xlseditor.ui.PreviewControllerTest"` |
| **Full suite command** | `./gradlew test` |
| **Estimated runtime** | ~10 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew test --tests "ch.ti.gagi.xlseditor.ui.PreviewControllerTest"`
- **After every plan wave:** Run `./gradlew test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** ~10 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 7-01-01 | 01 | 0 | PREV-03, PREV-04 | — | N/A | unit | `./gradlew test --tests "*.PreviewControllerTest"` | ❌ W0 | ⬜ pending |
| 7-02-01 | 02 | 1 | PREV-04 | — | N/A | unit | `./gradlew test --tests "*.PreviewControllerTest"` | ❌ W0 | ⬜ pending |
| 7-02-02 | 02 | 1 | PREV-03 | — | N/A | unit | `./gradlew test --tests "*.PreviewControllerTest"` | ❌ W0 | ⬜ pending |
| 7-03-01 | 03 | 1 | PREV-01 | — | N/A | manual-only | — visual inspection of split view | N/A | ⬜ pending |
| 7-03-02 | 03 | 1 | PREV-02 | — | N/A | manual-only | — native WebView PDF plugin (not unit-testable) | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/ch/ti/gagi/xlseditor/ui/PreviewControllerTest.java` — stubs for PREV-03, PREV-04

*Existing test infrastructure (JUnit 5, `Platform.startup()` pattern from EditorTabTest) covers all Phase 7 test needs. No framework install or additional fixtures needed.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Split view layout (editor left, PDF right) | PREV-01 | Requires running JavaFX Stage with rendered PDF — no headless toolkit configured | Launch app, trigger render, visually confirm split view |
| PDF scroll and zoom | PREV-02 | Native WebView PDF plugin — not unit-testable; consistent with Phase 6 pattern | Launch app, render PDF, scroll and pinch/Ctrl+scroll in preview pane |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 10s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
