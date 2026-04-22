---
phase: 05-editor-features-syntax-navigation
plan: "02"
subsystem: xml-syntax-highlighting-autocomplete
tags: [syntax-highlighting, autocomplete, richtext-fx, xsl, xsl-fo]
dependency_graph:
  requires: ["05-01"]
  provides: ["EDIT-04", "EDIT-05"]
  affects: ["EditorController"]
tech_stack:
  added: []
  patterns: ["pure static utility class", "regex-based StyleSpans", "ContextMenu autocomplete"]
key_files:
  created:
    - src/main/java/ch/ti/gagi/xsleditor/ui/XmlSyntaxHighlighter.java
    - src/main/java/ch/ti/gagi/xsleditor/ui/AutocompleteProvider.java
  modified:
    - src/test/java/ch/ti/gagi/xsleditor/ui/XmlSyntaxHighlighterTest.java
    - src/test/java/ch/ti/gagi/xsleditor/ui/AutocompleteProviderTest.java
decisions:
  - "XmlSyntaxHighlighter uses possessive/reluctant regex quantifiers ([\\s\\S]*?) to avoid catastrophic backtracking"
  - "AutocompleteProvider.getMatches() is pure Java — no JavaFX dependency, fully testable without toolkit"
  - "triggerAt() uses localToScreen guard before ContextMenu.show() to avoid NPE on headless bounds"
metrics:
  duration: "~30 minutes"
  completed: "2026-04-21"
  tasks_completed: 2
  files_changed: 4
---

# Phase 5 Plan 02: XmlSyntaxHighlighter + AutocompleteProvider Summary

Implemented two pure-static utility classes for syntax highlighting and keyword autocomplete.

## What Was Built

### Task 1: XmlSyntaxHighlighter (EDIT-04)

Regex-based StyleSpans computation for XML/XSLT source text. Maps token regions to CSS class names consumed by RichTextFX CodeArea:

- `xml-comment` — `<!-- ... -->`
- `xml-cdata` — `<![CDATA[ ... ]]>`
- `xml-pi` — `<?... ?>`
- `xml-tagmark` — `<`, `</`, `>`, `/>`
- `xml-element` — element names (e.g. `xsl:template`)
- `xml-attribute` — attribute names
- `xml-avalue` — attribute values

Key invariant: `spans.length() == text.length()` always holds (trailing span guard prevents StyleSpansBuilder crash). Empty/null input returns zero-length spans without throwing.

### Task 2: AutocompleteProvider (EDIT-05)

Static keyword list (44 entries: 27 `xsl:` + 17 `fo:` keywords) with prefix-filtered `getMatches(String prefix)`. UI method `triggerAt(CodeArea area)` extracts the prefix before the caret, builds a `ContextMenu`, and positions it via `localToScreen()` guard (prevents NPE on headless caret bounds).

## Test Results

| Test Class | Tests | Result |
|------------|-------|--------|
| XmlSyntaxHighlighterTest | 4 | ✓ PASSED |
| AutocompleteProviderTest | 3 | ✓ PASSED |

Full suite: BUILD SUCCESSFUL, 0 failures.

## Deviations from Plan

None. Both classes implemented exactly as specified in the plan. Files were already present in the working tree from a prior execution (worktree or session) — SUMMARY was missing; this document closes the gap.

## Self-Check: PASSED
