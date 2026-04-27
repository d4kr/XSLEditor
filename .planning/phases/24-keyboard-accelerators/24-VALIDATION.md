---
phase: 24
slug: keyboard-accelerators
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-27
---

# Phase 24 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | none — manual UI verification |
| **Config file** | none |
| **Quick run command** | `mvn compile -q` |
| **Full suite command** | `mvn compile -q` |
| **Estimated runtime** | ~10 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn compile -q`
- **After every plan wave:** Run `mvn compile -q`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 24-01-01 | 01 | 1 | KBD-01 | — | N/A | manual | `mvn compile -q` | ✅ | ⬜ pending |
| 24-01-02 | 01 | 1 | KBD-02 | — | N/A | manual | `mvn compile -q` | ✅ | ⬜ pending |
| 24-01-03 | 01 | 1 | KBD-03 | — | N/A | manual | `mvn compile -q` | ✅ | ⬜ pending |
| 24-01-04 | 01 | 1 | KBD-04 | — | N/A | manual | `mvn compile -q` | ✅ | ⬜ pending |
| 24-01-05 | 01 | 1 | KBD-05 | — | N/A | manual | `mvn compile -q` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

*Existing infrastructure covers all phase requirements.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Shortcut+O opens Open Project dialog | KBD-01 | JavaFX keyboard event requires running app | Launch app, press Shortcut+O, verify file chooser opens |
| Shortcut+N opens New File dialog | KBD-02 | JavaFX keyboard event requires running app | Launch app, press Shortcut+N, verify dialog opens |
| Shortcut+Q triggers exit flow | KBD-03 | JavaFX keyboard event requires running app | Launch app, press Shortcut+Q, verify exit/confirm dialog |
| Shortcut+Shift+E invokes Set Entrypoint | KBD-04 | JavaFX keyboard event requires running app | Launch app, select file, press Shortcut+Shift+E |
| Shortcut+Shift+I invokes Set XML Input | KBD-05 | JavaFX keyboard event requires running app | Launch app, select file, press Shortcut+Shift+I |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
