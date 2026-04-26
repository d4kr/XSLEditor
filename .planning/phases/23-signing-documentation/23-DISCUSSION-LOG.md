# Phase 23: Signing Documentation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-26
**Phase:** 23-signing-documentation
**Areas discussed:** Document style, Workflow reference, Verification steps, Windows SmartScreen, README cross-link

---

## Document Style

| Option | Description | Selected |
|--------|-------------|----------|
| Concise + reference workflow | Step-by-step numbered guide with copy-paste commands. Point reader to release.yml comments for secret descriptions — DRY, no duplication. | ✓ |
| Self-contained reference | Fully standalone doc repeating all secret descriptions inline. More beginner-friendly but drifts if workflow changes. | |
| Hybrid | Full secret table in SIGNING.md (canonical), with a note that release.yml also has inline comments. | |

**User's choice:** Concise + reference workflow
**Notes:** None

---

## Workflow Reference (user-added area)

| Option | Description | Selected |
|--------|-------------|----------|
| Include comment block as snippet | Copy the `# Required GitHub Actions Secrets` block from release.yml verbatim as a fenced code block in SIGNING.md. | ✓ |
| Link to file with line reference | Markdown link to release.yml with a note pointing to the comment block. | |

**User's choice:** Include the block as a code snippet
**Notes:** User added "Mostrare la release con il commento" — show the release workflow with the comment block embedded in SIGNING.md.

---

## Verification Steps

| Option | Description | Selected |
|--------|-------------|----------|
| Push a test tag | Guide ends with: push a pre-release tag (e.g. v0.4.0-test1) to trigger CI and verify macOS packaging succeeds. | ✓ |
| Pre-flight checklist only | Validate inputs before pushing: cert in Keychain, team ID format, app-specific password generated. No test tag step. | |
| No verification section | Trust-and-push. Success criteria doesn't require verification steps. | |

**User's choice:** Push a test tag
**Notes:** None

---

## Windows SmartScreen

| Option | Description | Selected |
|--------|-------------|----------|
| Maintainer note only | Short paragraph: MSI unsigned, users see SmartScreen warning, bypass via More info → Run anyway, Authenticode signing deferred. No screenshots. | ✓ |
| End-user bypass guide | Step-by-step for end-users with screenshots. Makes SIGNING.md feel like a user manual. | |
| Separate file | docs/WINDOWS-INSTALL.md with link from SIGNING.md. | |

**User's choice:** Maintainer note only
**Notes:** None

---

## README Cross-link

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — Contributing section | Add a Contributing / Release Setup section or bullet in README.md pointing to docs/SIGNING.md. | ✓ |
| No — standalone doc | SIGNING.md lives in docs/ and is findable via file browser. No README change. | |
| Link from workflow header only | Add pointer comment in release.yml. README unchanged. | |

**User's choice:** Yes — Contributing section
**Notes:** None

---

## Claude's Discretion

None — all areas had explicit user decisions.

## Deferred Ideas

None — discussion stayed within phase scope.
