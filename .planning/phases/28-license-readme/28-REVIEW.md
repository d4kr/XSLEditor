---
phase: 28-license-readme
reviewed: 2026-05-01T19:12:43Z
depth: standard
files_reviewed: 3
files_reviewed_list:
  - LICENSE
  - README.md
  - src/main/java/ch/ti/gagi/xsleditor/ui/AboutDialog.java
findings:
  critical: 1
  warning: 3
  info: 2
  total: 6
status: issues_found
---

# Phase 28: Code Review Report

**Reviewed:** 2026-05-01T19:12:43Z
**Depth:** standard
**Files Reviewed:** 3
**Status:** issues_found

## Summary

Phase 28 adds a LICENSE file (MIT), a README.md, and an `AboutDialog` Java class. The LICENSE and README are mostly correct documentation artifacts; no injection or security issues exist in them. The `AboutDialog` Java class contains one critical null-dereference bug (unguarded `hostServices` use), two correctness warnings (hardcoded version fallback and version mismatch between README and build.gradle), and minor quality items.

---

## Critical Issues

### CR-01: NullPointerException when `hostServices` is null

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/AboutDialog.java:111`

**Issue:** The constructor accepts `hostServices` as a parameter documented as potentially null in tests (`ownerStage` has an explicit null-guard, but `hostServices` does not). At line 111 the lambda calls `hostServices.showDocument(...)` unconditionally. If the caller ever passes `null` — or if `XSLEditorApp.hostServices()` returns `null` because `start()` has not yet run — clicking "View license" throws a `NullPointerException` and crashes the dialog handler on the JavaFX Application Thread.

The call site in `MainController.java:335` passes `XSLEditorApp.hostServices()`, which is a static field initialised inside `start()`. If the dialog were somehow opened before `start()` completes, or from a test context without a real JavaFX runtime, this field is `null`.

**Fix:**
```java
Hyperlink viewLink = new Hyperlink("View license");
viewLink.setStyle("-fx-text-fill: #007acc;");
viewLink.setOnAction(e -> {
    if (hostServices != null) {
        hostServices.showDocument("https://opensource.org/licenses/MIT");
    }
});
```

Alternatively, disable the hyperlink when `hostServices` is null:
```java
if (hostServices == null) {
    viewLink.setDisable(true);
}
```

---

## Warnings

### WR-01: Version hardcoded as fallback `"0.4.1"` in `build.gradle` disagrees with README `0.5.0`

**File:** `README.md:7` / `build.gradle:13`

**Issue:** `build.gradle` line 13 sets the project version fallback:
```groovy
version = project.findProperty('version') ?: '0.4.1'
```
`README.md` line 7 states `**Version:** 0.5.0` and references `build/libs/XSLEditor-0.5.0.jar` (lines 37, 42, 48). When the project is built without an explicit `-Pversion=` flag, the shadow JAR is named `XSLEditor-0.4.1.jar`, which contradicts the README instructions (`XSLEditor-0.5.0.jar`). A developer following the README will look for a file that does not exist.

**Fix:** Update `build.gradle` line 13 to match the current release version:
```groovy
version = project.findProperty('version') ?: '0.5.0'
```

### WR-02: Hardcoded `"2.9"` last-resort fallback in `fopVersion()` is a silent lie

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/AboutDialog.java:197`

**Issue:** When both the pom.properties resource and `org.apache.fop.Version.getVersion()` are unavailable, the method returns the string literal `"2.9"`. This is not "unknown" — it is a specific version that will be displayed as fact in the About dialog even if FOP is absent or is a different version. This can mislead debugging efforts. The same pattern used for Saxon (`"unknown"`) should be used here.

**Fix:**
```java
// Replace the final line:
return "unknown";
```

### WR-03: `IOException` from the icon `InputStream.close()` is silently swallowed by a bare `catch (Exception ignored)`

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/AboutDialog.java:129`

**Issue:** The try-with-resources block at lines 118-131 catches `Exception` broadly. `Image(InputStream)` can throw `IllegalArgumentException` (not `IOException`), but the outer catch discards it silently. This is the same pattern as in `XSLEditorApp.java` and is intentional for fail-silent behaviour. However, the catch swallows *all* exceptions including `RuntimeException`s from the `new Image(...)` constructor, making it impossible to distinguish between "icon absent" and "icon corrupted/format error". At minimum the condition `!img.isError()` already covers the error case; a RuntimeException from the constructor itself would be silently lost. This is a robustness concern, not a crash risk, but the catch scope is too broad.

**Fix:**
```java
} catch (IOException ignored) {
    // Dialog must not crash if icon is absent
}
```
The `img.isError()` check already handles bad image data; the IOException catch covers stream failures. Other RuntimeExceptions should not be silently swallowed.

---

## Info

### IN-01: `docs/screenshot.png` linked in README may not track with actual UI changes

**File:** `README.md:22`

**Issue:** The README embeds a screenshot (`docs/screenshot.png`). Screenshots go stale as the UI evolves but are not flagged by the build. This is a maintenance note, not a blocking defect.

**Fix:** Add a comment in the "Contributing" section noting that the screenshot should be refreshed on significant UI changes.

### IN-02: MIT LICENSE copyright holder is a username, not a legal name

**File:** `LICENSE:3`

**Issue:** `Copyright 2026 d4kr` uses a username. For an MIT license to be legally effective, the copyright holder should be a legal name (person or organisation). For a strictly internal developer tool this may be intentional, but it is worth noting.

**Fix:** Replace `d4kr` with the legal entity or person name if this project is ever distributed externally.

---

_Reviewed: 2026-05-01T19:12:43Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
