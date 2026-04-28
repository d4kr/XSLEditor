---
phase: 25
slug: edit-menu-clipboard-commands
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-28
---

# Phase 25 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 (Gradle) |
| **Config file** | `build.gradle` |
| **Quick run command** | `./gradlew test -x shadowJar` |
| **Full suite command** | `./gradlew test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew test -x shadowJar`
- **After every plan wave:** Run `./gradlew test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 25-01-01 | 01 | 1 | EDIT-10..13 | — | N/A | compile | `./gradlew compileJava` | ✅ | ⬜ pending |
| 25-01-02 | 01 | 1 | EDIT-10..13 | — | N/A | compile | `./gradlew compileJava` | ✅ | ⬜ pending |
| 25-01-03 | 01 | 1 | EDIT-10..13 | — | N/A | compile+test | `./gradlew test` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. No new test files needed — clipboard operations are manual-only per RichTextFX headless test limitations.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Cut removes selection and updates clipboard | EDIT-10 | JavaFX clipboard not testable headless | Run app, select text, Edit > Cut, verify removed + paste elsewhere |
| Copy leaves editor unchanged, clipboard updated | EDIT-11 | JavaFX clipboard not testable headless | Run app, select text, Edit > Copy, verify text remains + paste elsewhere |
| Paste inserts at cursor | EDIT-12 | JavaFX clipboard not testable headless | Run app, copy text, click cursor, Edit > Paste, verify insertion |
| Select All selects all text | EDIT-13 | JavaFX clipboard not testable headless | Run app, open file, Edit > Select All, verify all text highlighted |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
