# Phase 23: Signing Documentation - Context

**Gathered:** 2026-04-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Create `docs/SIGNING.md` — a maintainer guide for configuring all 7 macOS signing + notarization GitHub Actions secrets and getting the full signing pipeline working. Target reader: a developer who is new to this repository but familiar with macOS/Keychain.

</domain>

<decisions>
## Implementation Decisions

### Document Style
- **D-01:** Concise + reference workflow. SIGNING.md is a numbered, step-by-step guide with copy-paste commands. It does NOT duplicate secret descriptions inline — it directs the reader to the inline comments in `.github/workflows/release.yml` which are already authoritative.
- **D-02:** Include the full `# Required GitHub Actions Secrets` comment block from `release.yml` as a fenced code snippet in SIGNING.md. This gives readers everything in one place without maintaining a second source of truth for the secret specs.

### Verification
- **D-03:** End the guide with a "Verify your setup" step: push a pre-release tag (e.g. `v0.4.0-test1`) to trigger CI and confirm the macOS packaging job completes successfully. The `-` in the tag suffix causes it to be auto-marked as pre-release, so it's safe to test with.

### Windows SmartScreen
- **D-04:** Maintainer note only — a short paragraph stating: the Windows MSI is unsigned in v0.4.0, users will see a SmartScreen warning, they can bypass via "More info → Run anyway," Authenticode signing is deferred to a future release (requires EV cert ~$200–500/yr). No end-user tutorial, no screenshots.

### README Cross-link
- **D-05:** Add a "Contributing / Release Setup" section or bullet to `README.md` pointing to `docs/SIGNING.md`. Makes the guide discoverable for new contributors without opening the docs folder manually.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### CI Workflow (primary source)
- `.github/workflows/release.yml` — Contains the authoritative `# Required GitHub Actions Secrets` comment block (lines 1–30 approx.) documenting all 7 secrets with how-to-obtain and how-to-encode instructions. SIGNING.md must include this block verbatim as a code snippet.

### Requirements
- `.planning/ROADMAP.md` §Phase 23 — Success criteria: lists all 7 secret names, what each expects, Apple Developer Program note, and SmartScreen note. Planner MUST match all 3 success criteria exactly.

### Docs Folder
- `docs/` — Existing folder (contains PRD.md, SPEC.md). SIGNING.md goes here. No new folder needed.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `.github/workflows/release.yml` header comment block — verbatim content to embed in SIGNING.md as a fenced code snippet (saves authoring effort, ensures accuracy)

### Established Patterns
- `docs/` folder already exists with PRD.md and SPEC.md — SIGNING.md follows the same location convention
- `README.md` already exists — needs a Contributing/Release Setup bullet added

### Integration Points
- `README.md` — add link to `docs/SIGNING.md` in a new "Contributing / Release Setup" section
- `.github/workflows/release.yml` — add a single pointer comment near the top referencing `docs/SIGNING.md` for full setup instructions (optional but consistent with D-01 "reference workflow" approach)

</code_context>

<specifics>
## Specific Ideas

- The 7 secrets are: `MACOS_CERTIFICATE`, `MACOS_CERTIFICATE_PASSWORD`, `MACOS_SIGNING_IDENTITY`, `MACOS_KEYCHAIN_PASSWORD`, `APPLE_ID`, `APPLE_TEAM_ID`, `APPLE_APP_SPECIFIC_PASSWORD`. All 7 must appear in the document.
- `MACOS_SIGNING_IDENTITY` gotcha: expects the **name portion only** (e.g. `Acme Corp`), NOT the full subject string (`Developer ID Application: Acme Corp (ABC123XYZ7)`). This is a common mistake — highlight it.
- `MACOS_KEYCHAIN_PASSWORD` is an ephemeral CI-only password — suggest `openssl rand -hex 16` to generate it.
- `APPLE_APP_SPECIFIC_PASSWORD` is generated at `appleid.apple.com → Sign-In and Security → App-Specific Passwords`.
- Apple Developer Program ($99/yr) is required for notarization — note this prominently at the top of the document.
- Test tag pattern: `v0.4.0-test1` (pre-release, `-` suffix triggers pre-release auto-detection per existing CI logic).

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 23-signing-documentation*
*Context gathered: 2026-04-26*
