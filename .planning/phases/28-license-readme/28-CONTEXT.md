# Phase 28: License & README - Context

**Gathered:** 2026-05-01
**Status:** Ready for planning

<domain>
## Phase Boundary

Pure documentation/metadata phase — no new Java features. Delivers three things:
1. `LICENSE` file (MIT, 2026) in the project root
2. AboutDialog updated: "License: MIT" with link to `https://opensource.org/licenses/MIT`
3. README logo moved above the `# XSLEditor` title, replaced with `<img width="96">`, version bumped to 0.5.0

</domain>

<decisions>
## Implementation Decisions

### LICENSE file
- **D-01:** Standard MIT License text, year 2026, copyright holder `d4kr`
  - Full line: `Copyright 2026 d4kr`

### AboutDialog (Java)
- **D-02:** Replace `Label("License: Apache 2.0")` with `Label("License: MIT")` at ~line 104
- **D-03:** Update `Hyperlink("View license")` URL from `https://www.apache.org/licenses/LICENSE-2.0` to `https://opensource.org/licenses/MIT`

### README
- **D-04:** Move logo above the `# XSLEditor` title (logo first, then heading)
- **D-05:** Replace bare Markdown image `![App icon](...)` with `<img src="src/main/resources/ch/ti/gagi/xsleditor/icon.png" width="96">`
- **D-06:** Bump `**Version:** 0.3.0` to `**Version:** 0.5.0`

### Claude's Discretion
- Exact MIT license boilerplate text: use the canonical OSI/MIT text (no variation)
- README alignment for the img tag: centered via `<p align="center">` is optional — standard left-align is fine

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Source files to modify
- `src/main/java/ch/ti/gagi/xsleditor/ui/AboutDialog.java` — license label + hyperlink at lines ~104–111
- `README.md` — logo at line 5, version at line 7

### Requirements
- `.planning/REQUIREMENTS.md` §DOC-01, DOC-02, DOC-03 — the three acceptance criteria for this phase

### No external specs
No ADRs or additional docs — requirements fully captured in decisions above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `AboutDialog.java` lines 103–113: existing license `Label` + `Hyperlink` + `HBox` structure — edit in place, no structural changes needed

### Established Patterns
- AboutDialog already uses `hostServices.showDocument(url)` for the hyperlink action — same pattern for MIT URL
- README uses standard GitHub Markdown — `<img>` HTML tag is valid in GitHub-flavored Markdown

### Integration Points
- No Java compilation needed for README or LICENSE changes
- AboutDialog edit is a 2-line string change — no new imports, no structural change

</code_context>

<specifics>
## Specific Ideas

- Logo above the title: `<img src="..." width="96">` as its own line, then blank line, then `# XSLEditor`
- MIT LICENSE boilerplate: use standard OSI text with `Copyright 2026 d4kr` on the copyright line

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 28-license-readme*
*Context gathered: 2026-05-01*
