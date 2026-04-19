---
phase: 6
slug: render-pipeline-integration
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-19
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Gradle test task) |
| **Config file** | `build.gradle` (existing test configuration) |
| **Quick run command** | `./gradlew test --tests "*RenderController*"` |
| **Full suite command** | `./gradlew test` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew test --tests "*RenderController*"`
- **After every plan wave:** Run `./gradlew test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 20 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 6-01-01 | 01 | 0 | REND-01..06 | — | N/A | unit stub | `./gradlew test --tests "*RenderController*"` | ❌ W0 | ⬜ pending |
| 6-02-01 | 02 | 1 | REND-01 | — | N/A | unit | `./gradlew test --tests "*RenderController*"` | ✅ | ⬜ pending |
| 6-02-02 | 02 | 1 | REND-02 | — | N/A | unit | `./gradlew test --tests "*RenderController*"` | ✅ | ⬜ pending |
| 6-02-03 | 02 | 1 | REND-03 | — | N/A | manual | `./gradlew build` compiles | ✅ | ⬜ pending |
| 6-03-01 | 03 | 2 | REND-04 | — | N/A | manual | `./gradlew build` compiles | ✅ | ⬜ pending |
| 6-03-02 | 03 | 2 | REND-05 | — | N/A | manual | `./gradlew build` compiles | ✅ | ⬜ pending |
| 6-03-03 | 03 | 2 | REND-06 | — | N/A | manual | app launched, render < 5s | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/ch/ti/gagi/xlseditor/ui/RenderControllerTest.java` — stubs for REND-01, REND-02 (button disabled logic, task lifecycle)
- [ ] Skeleton `RenderController.java` with correct method signatures (so stubs compile)

*Existing JUnit 5 infrastructure covers all other test needs.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Render button shows "Rendering..." during Task | REND-03 | JavaFX UI requires running app | Launch app, open project, click Render, confirm label changes |
| Status label shows "Render complete (X.Xs)" | REND-01 | Timing + UI requires running app | Complete render, confirm status label + duration |
| PDF bytes passed to previewPane seam | REND-04 | Phase 7 consumer not yet wired | Verify Consumer<byte[]> called via log output or debug |
| Outdated state on failure | REND-05 | Requires deliberate pipeline failure | Point entrypoint at invalid file, confirm previous PDF retained |
| Render < 5 seconds | REND-06 | Performance — requires real XSLT fixture | Time render with System.currentTimeMillis delta in log |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 20s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
