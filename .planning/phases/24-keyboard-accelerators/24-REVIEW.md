---
phase: 24-keyboard-accelerators
reviewed: 2026-04-27T00:00:00Z
depth: standard
files_reviewed: 1
files_reviewed_list:
  - src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
findings:
  critical: 0
  warning: 1
  info: 1
  total: 2
status: issues_found
---

# Phase 24: Code Review Report

**Reviewed:** 2026-04-27
**Depth:** standard
**Files Reviewed:** 1
**Status:** issues_found

## Summary

This phase adds five keyboard accelerators to File menu items in `main.fxml` (KBD-01 through KBD-05). The five required accelerators are correctly added with the right token (`Shortcut+`), correct attribute order, and the KBD-04/KBD-05 items correctly omit `onAction`. However, an out-of-scope change was also committed: the pre-existing `findInFilesMenuItem` accelerator was changed from `Ctrl+Shift+F` to `Shortcut+Shift+F`. This directly violates the phase's stated anti-pattern ("Do NOT modify the existing Find in Files (Ctrl+Shift+F) accelerator") and causes the plan's own acceptance criterion (`grep -q 'accelerator="Ctrl+Shift+F"'`) to fail. It also changes cross-platform behavior on Windows/Linux where `Ctrl+Shift+F` was the intentional binding.

---

## Warnings

### WR-01: Out-of-scope change to Find in Files accelerator breaks acceptance criteria and alters cross-platform behavior

**File:** `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml:51`

**Issue:** The `findInFilesMenuItem` accelerator was changed from `Ctrl+Shift+F` to `Shortcut+Shift+F` as part of this phase's commit. This change is explicitly forbidden by the phase plan:

> "Do NOT modify the existing Render (F5) or Find in Files (Ctrl+Shift+F) accelerators — those are out of scope for this phase."

This has two concrete consequences:

1. **Acceptance criterion failure:** The plan's task acceptance criteria requires `grep -q 'accelerator="Ctrl+Shift+F"' src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` to succeed. It now fails.

2. **Acceptance criterion failure (count):** The plan's acceptance criteria requires `grep -c 'accelerator="Shortcut+'` to return exactly `5`. The actual count is `6` because the out-of-scope change converts the Find in Files accelerator to a `Shortcut+` form, inflating the count.

3. **Cross-platform behavioral change:** `Ctrl+Shift+F` is a platform-specific binding (Ctrl on all platforms). `Shortcut+Shift+F` resolves to `Cmd+Shift+F` on macOS and `Ctrl+Shift+F` on Windows/Linux. On macOS the user would now press `Cmd+Shift+F` instead of `Ctrl+Shift+F` to invoke Find in Files. Whether this is desirable is a product decision — but it was made implicitly as a side-effect of an out-of-scope edit, not explicitly.

**Fix:** Revert the `findInFilesMenuItem` accelerator to its prior value:

```xml
<MenuItem fx:id="findInFilesMenuItem" text="Find in Files"
          accelerator="Ctrl+Shift+F" onAction="#handleFindInFiles"/>
```

If the intent is to also convert Find in Files to cross-platform `Shortcut+Shift+F`, that decision should be made explicitly in a separate phase (or as an explicitly approved scope extension in this one), with the acceptance criteria updated accordingly.

---

## Info

### IN-01: Acceptance criteria count check is fragile against future accelerator additions

**File:** `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` (verification script in `24-01-PLAN.md:144`)

**Issue:** The acceptance criterion `grep -c 'accelerator="Shortcut+' ... | grep -q '^5$'` hard-codes the expected count as exactly 5. This means any future phase that adds a `Shortcut+`-style accelerator to this file will retroactively cause phase 24's verification step to fail if the script is re-run. The count check conflates "did phase 24 add exactly 5 new accelerators" with "does the file contain exactly 5 Shortcut+ accelerators in total."

**Fix:** Replace the absolute count check with explicit per-item presence checks (the plan already lists these individually in criteria 2–6). The count check can be dropped or replaced with a minimum bound (`-ge 5`) in the future. No FXML change is needed; this is a note for the verification script author.

---

_Reviewed: 2026-04-27_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
