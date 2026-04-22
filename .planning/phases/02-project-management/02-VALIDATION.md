---
phase: 2
slug: project-management
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-15
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter 5.10.0 |
| **Config file** | `build.gradle` → `test { useJUnitPlatform() }` — framework declared, no test dir yet |
| **Quick run command** | `./gradlew test` |
| **Full suite command** | `./gradlew test` |
| **Estimated runtime** | ~10 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew test`
- **After every plan wave:** Run `./gradlew test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 2-W0-01 | 01 | 0 | PROJ-04, PROJ-05 | — | N/A | unit | `./gradlew test --tests "*.ProjectConfigTest"` | ❌ W0 | ⬜ pending |
| 2-W0-02 | 01 | 0 | PROJ-06 | — | Path traversal guard on filename | unit | `./gradlew test --tests "*.ProjectContextTest"` | ❌ W0 | ⬜ pending |
| 2-01-01 | 01 | 1 | PROJ-04, PROJ-05 | — | Null-safe `!ep.isNull()` guard | unit | `./gradlew test --tests "*.ProjectConfigTest"` | ✅ W0 | ⬜ pending |
| 2-01-02 | 01 | 1 | PROJ-06 | — | `filename.contains("/")` traversal block | unit | `./gradlew test --tests "*.ProjectContextTest"` | ✅ W0 | ⬜ pending |
| 2-02-01 | 02 | 2 | PROJ-01 | — | N/A | manual | See manual table | N/A | ⬜ pending |
| 2-02-02 | 02 | 2 | PROJ-02, PROJ-03 | — | N/A | manual | See manual table | N/A | ⬜ pending |
| 2-02-03 | 02 | 2 | PROJ-01, PROJ-05 | — | N/A | manual | See manual table | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/ch/ti/gagi/xsleditor/model/ProjectConfigTest.java` — stubs for PROJ-04, PROJ-05 (config write/read with null fields)
- [ ] `src/test/java/ch/ti/gagi/xsleditor/model/ProjectContextTest.java` — stubs for PROJ-06 (createFile, path traversal guard)
- [ ] `src/test/resources/` — temp directory fixtures (use `@TempDir` JUnit annotation; no static fixtures needed)

*Existing infrastructure: JUnit Jupiter 5.10.0 declared in `build.gradle`; `test { useJUnitPlatform() }` configured; no source directory exists yet.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| "Open Project" dialog opens OS directory picker | PROJ-01 | `DirectoryChooser` requires OS GUI / FX Application Thread | 1. Launch app; 2. File → Open Project…; 3. Verify native directory picker appears; 4. Select a folder; 5. Verify title bar updates to folder name |
| Project without `.xslfo-tool.json` loads with null entrypoint | PROJ-01 | Requires OS GUI + filesystem | 1. Open a directory with no `.xslfo-tool.json`; 2. Verify app loads without error; 3. "Set Entrypoint" / "Set XML Input" remain disabled |
| Project with `.xslfo-tool.json` restores entrypoint and XML input paths | PROJ-05 | Requires OS GUI + config file on disk | 1. Create `.xslfo-tool.json` with known paths; 2. Open that directory; 3. Verify state visible (log panel shows restored paths) |
| "Set Entrypoint" MenuItem is disabled | PROJ-02 | Requires FX toolkit rendering | 1. Open app with no project; 2. File → check "Set Entrypoint" is grayed; 3. Open project; 4. Still grayed (D-04) |
| "Set XML Input" MenuItem is disabled | PROJ-03 | Requires FX toolkit rendering | 1. Same as above for "Set XML Input" |
| "New File" dialog creates empty file in project root | PROJ-06 | Requires OS GUI for dialog | 1. Open project; 2. File → New File…; 3. Enter filename; 4. Verify file exists in project root; 5. Verify file is empty |
| "New File" disabled when no project open | PROJ-06 | Requires FX toolkit rendering | 1. Launch app (no project); 2. File → verify "New File…" is grayed |
| Corrupt `.xslfo-tool.json` shows Warning alert | PROJ-05 | Requires OS GUI | 1. Create malformed JSON in `.xslfo-tool.json`; 2. Open directory; 3. Verify `Alert(WARNING)` appears |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
