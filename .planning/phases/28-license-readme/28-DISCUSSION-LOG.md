# Phase 28: License & README - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-01
**Phase:** 28-license-readme
**Areas discussed:** Author name, MIT link URL, README extras, Logo position

---

## Author Name

| Option | Description | Selected |
|--------|-------------|----------|
| d4kr | Matches git user and About dialog author field | ✓ |
| Gagi TI / org | For institutional project | |
| d4kr & Claude Code | Match the About dialog author line exactly | |

**User's choice:** d4kr
**Notes:** Copyright line: `Copyright 2026 d4kr`

---

## MIT Link URL

| Option | Description | Selected |
|--------|-------------|----------|
| opensource.org/licenses/MIT | Canonical OSI reference — stable, authoritative | ✓ |
| choosealicense.com/licenses/mit | GitHub's license picker — readable but unofficial | |
| Repo LICENSE file (GitHub raw) | Points to actual file in repo — fragile if repo moves | |

**User's choice:** `https://opensource.org/licenses/MIT`
**Notes:** Replaces current `https://www.apache.org/licenses/LICENSE-2.0`

---

## README Extras

| Option | Description | Selected |
|--------|-------------|----------|
| Bump to 0.5.0 | README currently says 0.3.0 — two milestones behind | ✓ |
| Leave version as-is | Keep change minimal — DOC-03 only scopes the img tag | |

**User's choice:** Bump to 0.5.0

---

## Logo Position

| Option | Description | Selected |
|--------|-------------|----------|
| Above the title | Logo first, then `# XSLEditor` heading below it | ✓ |
| After the title, before tagline | Title first, then logo, then description | |
| Inline with title (HTML) | Icon and title side by side via HTML table | |

**User's choice:** Above the title
**Notes:** User also requested: "Move the logo/icon on top and reduce the size" — confirmed width="96" and position above `# XSLEditor`

---

## Claude's Discretion

- Exact MIT license boilerplate text: use canonical OSI/MIT text
- README img tag alignment: standard left-align (no `<p align="center">` required)

## Deferred Ideas

None.
