# Phase 23: Signing Documentation - Pattern Map

**Mapped:** 2026-04-26
**Files analyzed:** 3 (new/modified)
**Analogs found:** 3 / 3

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `docs/SIGNING.md` | doc (maintainer guide) | reference | `README.md` | role-match (same prose + fenced-code style) |
| `README.md` | doc (project overview) | reference | `README.md` itself (add section) | exact (in-place edit) |
| `.github/workflows/release.yml` | config (CI workflow) | N/A | `.github/workflows/release.yml` itself | exact (add one comment line) |

---

## Pattern Assignments

### `docs/SIGNING.md` (new maintainer guide)

**Analog:** `README.md`

**Document heading style** (`README.md` lines 1–6):
```markdown
# XSLEditor

> Local desktop tool for developers to ...

**Version:** 0.3.0
```
- H1 title, optional blockquote subtitle, bold key/value metadata lines.
- Use the same H1 → prose → numbered steps → fenced code blocks structure for SIGNING.md.

**Fenced code block style** (`README.md` lines 33–35):
```markdown
```bash
./gradlew shadowJar
```
```
- Language tag always present (`bash`, `yaml`, `text`).
- Single blank line before and after every fenced block.

**Table style** (`README.md` lines 53–62):
```markdown
| Component | Library / Version |
|-----------|-------------------|
| Language  | Java 21           |
```
- Two-column tables with left-aligned pipes and header separator dashes.

**Note/callout style** (`README.md` lines 44–49):
```markdown
macOS note: if you see a Gatekeeper warning, right-click the JAR and choose Open, or run:
```
- Inline bold-prefixed note sentences (no `> [!NOTE]` admonition syntax — the project uses plain prose notes).

**Verbatim block to embed** (`.github/workflows/release.yml` lines 1–41):
```yaml
# =============================================================================
# Required GitHub Actions Secrets
# =============================================================================
# Configure these secrets at:
# GitHub → Settings → Secrets and variables → Actions → New repository secret
#
# ── Phase 21: macOS Signing (used in package-macos-arm and package-macos-x64) ──
#
# MACOS_CERTIFICATE
#   Base64-encoded Developer ID Application certificate (.p12 export from Keychain Access)
#   How to obtain: Keychain Access → right-click "Developer ID Application: Name (TEAM)"
#                  → Export → save as .p12 with a password
#   How to encode: base64 -i /path/to/cert.p12 | pbcopy
#
# MACOS_CERTIFICATE_PASSWORD
#   The password you set when exporting the .p12 from Keychain Access.
#
# MACOS_SIGNING_IDENTITY
#   The NAME PORTION ONLY of the certificate subject.
#   Correct:   Acme Corp
#   Incorrect: Developer ID Application: Acme Corp (ABC123XYZ7)
#   jpackage uses --mac-signing-key-user-name which expects the name only.
#
# MACOS_KEYCHAIN_PASSWORD
#   Any random string used as the password for the ephemeral CI keychain.
#   Generate: openssl rand -hex 16
#   This keychain is created and deleted on every CI run.
#
# ── Phase 22: macOS Notarization (reserved — not yet used in this workflow) ──
#
# APPLE_ID
#   Apple ID email address of the developer account that owns the certificate.
#
# APPLE_TEAM_ID
#   10-character team identifier from developer.apple.com/account (e.g. ABC123XYZ7)
#
# APPLE_APP_SPECIFIC_PASSWORD
#   App-specific password for the Apple ID.
#   Generate: appleid.apple.com → Sign-In and Security → App-Specific Passwords
#
# =============================================================================
```
Per decision D-02, this entire block is reproduced verbatim as a `yaml` fenced code snippet inside SIGNING.md.

**SIGNING.md content outline** (derived from decisions D-01 through D-04 and specifics):
1. H1: `# macOS Signing & Notarization — Maintainer Guide`
2. Prerequisite callout: Apple Developer Program membership required ($99/yr) — place prominently before step 1.
3. Numbered steps:
   - Step 1: Export Developer ID Application certificate as `.p12` from Keychain Access
   - Step 2: Base64-encode the `.p12` (`base64 -i /path/to/cert.p12 | pbcopy`)
   - Step 3: Generate a random keychain password (`openssl rand -hex 16`)
   - Step 4: Obtain Apple Team ID from `developer.apple.com/account`
   - Step 5: Generate App-Specific Password at `appleid.apple.com`
   - Step 6: Set all 7 secrets in GitHub Actions (link: `GitHub → Settings → Secrets and variables → Actions`)
4. Verbatim secrets reference block (full `release.yml` header comment as `yaml` fenced block, per D-02).
5. `MACOS_SIGNING_IDENTITY` gotcha callout (name portion only, not full subject string — per specifics).
6. Windows SmartScreen note (unsigned MSI in v0.4.0, SmartScreen bypass, Authenticode deferred, per D-04).
7. Verify section: push `v0.4.0-test1` tag, confirm macOS packaging job passes, tag auto-marked pre-release (per D-03).

---

### `README.md` (add "Contributing / Release Setup" section)

**Analog:** `README.md` itself — `## Development Notes` section (lines 82–87) shows the pattern for short maintainer-oriented sections at the bottom of the file.

**Section insertion point** — after `## Development Notes`, before `## License` (lines 88–91):
```markdown
## License

Internal developer tool — not distributed publicly.
```
Insert a new section immediately before `## License`:
```markdown
## Contributing / Release Setup

To configure macOS signing and notarization secrets for CI releases, see [`docs/SIGNING.md`](docs/SIGNING.md).
```
- One-sentence bullet or prose, matching the terse style of `## Development Notes`.
- Relative Markdown link — same style as `[Download from Adoptium](https://adoptium.net/)` used in `## Prerequisites` (line 28).

---

### `.github/workflows/release.yml` (optional pointer comment)

**Analog:** `.github/workflows/release.yml` lines 1–41 — the existing `# Required GitHub Actions Secrets` banner block sets the comment style for the file header.

**Pattern:** Add a single line directly after the closing `# ===` banner line (line 41), before `name: Release` (line 43):
```yaml
# Full setup guide: docs/SIGNING.md
```
- Plain `#` comment, no banner decoration, consistent with short inline comments used throughout the file.
- One blank line between this line and `name: Release` to preserve existing spacing.

---

## Shared Patterns

### Markdown heading hierarchy
**Source:** `README.md` (entire file)
**Apply to:** `docs/SIGNING.md`
- H1 for document title only.
- H2 (`##`) for top-level sections.
- No H3/H4 unless a section genuinely nests (avoid in a short how-to guide).

### Fenced code blocks with language tag
**Source:** `README.md` lines 33–35, 40–42
**Apply to:** `docs/SIGNING.md` (all command examples and the verbatim secrets block)
```markdown
```bash
<command here>
```
```
Always tag the language; use `bash` for shell commands, `yaml` for the secrets block.

### Relative Markdown links
**Source:** `README.md` line 22 (`docs/screenshot.png`) and line 28 (`https://...`)
**Apply to:** `README.md` new section and optionally inside `docs/SIGNING.md` for back-references
```markdown
[`docs/SIGNING.md`](docs/SIGNING.md)
```

---

## No Analog Found

No files in this phase lack a close analog. All three targets are either in-place edits of existing files or a new Markdown doc whose style is directly modelled on `README.md`.

---

## Metadata

**Analog search scope:** `/Users/kraehend/Developer/XSLEditor/docs/`, `/Users/kraehend/Developer/XSLEditor/README.md`, `/Users/kraehend/Developer/XSLEditor/.github/workflows/release.yml`
**Files scanned:** 4 (README.md, docs/PRD.md, docs/SPEC.md, .github/workflows/release.yml)
**Pattern extraction date:** 2026-04-26
