---
phase: 28-license-readme
verified: 2026-05-01T00:00:00Z
status: human_needed
score: 5/5 must-haves verified
overrides_applied: 0
human_verification:
  - test: "Launch the application and open Help > About. Confirm the dialog shows 'License: MIT' (not 'Apache 2.0') and that clicking 'View license' opens https://opensource.org/licenses/MIT in the default browser."
    expected: "Label reads 'License: MIT'; browser opens https://opensource.org/licenses/MIT on click."
    why_human: "JavaFX HostServices.showDocument() invocation and browser launch cannot be exercised without a running GUI."
  - test: "Open README.md in GitHub's web renderer and confirm the app icon appears visibly smaller (96px wide) and sits above the '# XSLEditor' heading."
    expected: "Icon renders at ~96px width, placed above the H1 heading with no Markdown alt-text syntax visible."
    why_human: "GitHub Markdown rendering fidelity (HTML img tag inside Markdown) requires a live browser view to confirm the visual result."
---

# Phase 28: License & README Verification Report

**Phase Goal:** The repository declares MIT as its license in a standard LICENSE file, the About dialog reflects MIT, and the README logo renders at a reduced display size.
**Verified:** 2026-05-01
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | A LICENSE file exists in the project root containing the standard MIT License text with year 2026 and the author name | VERIFIED | `LICENSE` present at project root; `head -1` = `MIT License`; `Copyright 2026 d4kr` on line 3; full OSI body confirmed |
| 2 | Opening the About dialog shows "License: MIT" (not "Apache 2.0") with a clickable link | VERIFIED (code) / HUMAN NEEDED (runtime) | `new Label("License: MIT")` present at line 104; no `Apache 2.0` string remains; `showDocument("https://opensource.org/licenses/MIT")` at line 111 |
| 3 | Clicking the 'View license' hyperlink opens https://opensource.org/licenses/MIT | HUMAN NEEDED | `hostServices.showDocument("https://opensource.org/licenses/MIT")` wired correctly in `setOnAction`; browser launch requires human test |
| 4 | The README logo renders smaller via an HTML `<img width="96">` tag, placed above the `# XSLEditor` heading | VERIFIED | `head -1 README.md` = `<img src="src/main/resources/ch/ti/gagi/xsleditor/icon.png" width="96">`; line 2 blank; line 3 `# XSLEditor` |
| 5 | The README **Version:** line reads 0.5.0 | VERIFIED | `grep '^\*\*Version:\*\* 0.5.0$' README.md` matches; `0.3.0` not present |

**Score:** 5/5 truths verified (2 additionally need human runtime confirmation)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `LICENSE` | Standard MIT license text, year 2026, holder d4kr | VERIFIED | File present; starts with `MIT License`; `Copyright 2026 d4kr`; full permissive body; no Apache text |
| `src/main/java/ch/ti/gagi/xsleditor/ui/AboutDialog.java` | About dialog with MIT license label and URL | VERIFIED | `License: MIT` label present; `opensource.org/licenses/MIT` URL present; Apache references removed; compiles clean |
| `README.md` | README with resized logo above title and v0.5.0 version | VERIFIED | `<img` tag at line 1 with `width="96"`; `# XSLEditor` at line 3; `**Version:** 0.5.0` present |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `AboutDialog.java` | `https://opensource.org/licenses/MIT` | `hostServices.showDocument(...)` | WIRED | Pattern `opensource\.org/licenses/MIT` found at line 111 inside `setOnAction` lambda |
| `README.md` | `src/main/resources/ch/ti/gagi/xsleditor/icon.png` | `<img src=...>` with `width="96"` | WIRED | Pattern `<img[^>]*width="96"` matches line 1; icon resource confirmed at expected path |

### Data-Flow Trace (Level 4)

Not applicable — this phase modifies static documentation (LICENSE, README.md) and a hardcoded string constant in AboutDialog.java. No dynamic data rendering involved.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| LICENSE file exists with MIT heading | `test -f LICENSE && head -1 LICENSE` | `MIT License` | PASS |
| LICENSE has correct copyright line | `grep -c '^Copyright 2026 d4kr$' LICENSE` | `1` | PASS |
| LICENSE has full permissive body | `grep -c 'WITHOUT WARRANTY OF ANY KIND' LICENSE` | `1` | PASS |
| No Apache strings in AboutDialog | `grep -c 'Apache 2.0' AboutDialog.java` | `0` | PASS |
| MIT URL in AboutDialog | `grep -c '"https://opensource.org/licenses/MIT"' AboutDialog.java` | `1` | PASS |
| README first line is img tag | `head -1 README.md` | `<img src="..." width="96">` | PASS |
| README version is 0.5.0 | `grep -c '^\*\*Version:\*\* 0.5.0$' README.md` | `1` | PASS |
| README heading at line 3 | `head -3 README.md \| tail -1` | `# XSLEditor` | PASS |
| Old Markdown image removed | `grep -c '^!\[App icon\]' README.md` | `0` | PASS |
| Project compiles | `./gradlew compileJava -q` | Exit 0 | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| DOC-01 | 28-01-PLAN.md | Repository contains a LICENSE file (MIT, 2026) in the project root | SATISFIED | `LICENSE` at project root; `MIT License` heading; `Copyright 2026 d4kr`; full OSI body |
| DOC-02 | 28-01-PLAN.md | About dialog shows "License: MIT" with a link to the MIT license text | SATISFIED (code) | `Label("License: MIT")` and `showDocument("https://opensource.org/licenses/MIT")` confirmed in source |
| DOC-03 | 28-01-PLAN.md | README logo rendered at reduced size via HTML `<img width="96">` tag | SATISFIED | `<img src="..." width="96">` at line 1 of README.md |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| None | — | — | — | — |

No TODOs, FIXMEs, stub implementations, placeholder text, or hardcoded empty values found in any of the three modified files.

### Notable Finding: Copyright `(c)` vs No `(c)`

The PLAN task spec required `Copyright (c) 2026 d4kr` (with the `(c)` symbol), but the actual LICENSE file contains `Copyright 2026 d4kr` (without `(c)`). Assessment: this is not a failure. The ROADMAP success criteria (the contract) states only "year 2026 and the author name" — no `(c)` required. REQUIREMENTS.md DOC-01 states only "MIT, 2026". The OSI MIT License template accepts both forms. The intent of the requirement is fully satisfied.

### Human Verification Required

#### 1. About Dialog Runtime — License Label and Hyperlink

**Test:** Launch XSLEditor, open Help > About, read the license line, then click "View license."
**Expected:** The dialog label reads "License: MIT" (not "Apache 2.0"). Clicking "View license" opens the system default browser to `https://opensource.org/licenses/MIT`.
**Why human:** JavaFX `HostServices.showDocument()` delegates to the OS browser via the JavaFX application lifecycle. There is no way to test this without a running GUI session.

#### 2. README Logo Rendering in GitHub

**Test:** Open `README.md` in GitHub's web interface (or equivalent Markdown renderer) and observe the top of the file.
**Expected:** The app icon appears as a visually smaller image (approximately 96px wide) sitting above the `# XSLEditor` heading. The raw `<img>` HTML tag is rendered as an image, not shown as raw markup.
**Why human:** GitHub's Markdown-to-HTML pipeline processes `<img>` tags inside Markdown, but the visual rendering can only be confirmed in an actual browser rendering the page. The source is correct — confirmation that the renderer honours the `width` attribute requires a visual check.

### Gaps Summary

No gaps. All five must-have truths are verified at the code level. Two items require human confirmation for runtime/visual behaviour, but no implementation is missing or broken.

---

_Verified: 2026-05-01_
_Verifier: Claude (gsd-verifier)_
