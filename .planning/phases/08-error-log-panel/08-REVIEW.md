---
phase: 08-error-log-panel
reviewed: 2026-04-21T05:02:00Z
depth: standard
files_reviewed: 6
files_reviewed_list:
  - src/main/java/ch/ti/gagi/xsleditor/ui/LogController.java
  - src/main/java/ch/ti/gagi/xsleditor/log/LogEntry.java
  - src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
  - src/main/resources/ch/ti/gagi/xsleditor/ui/main.css
  - src/main/java/ch/ti/gagi/xsleditor/ui/RenderController.java
  - src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java
findings:
  critical: 0
  warning: 3
  info: 4
  total: 7
status: issues_found
---

# Phase 8: Code Review Report

**Reviewed:** 2026-04-21T05:02:00Z
**Depth:** standard
**Files Reviewed:** 6
**Status:** issues_found

## Summary

Phase 8 introduced a log panel (`LogController`, `LogEntry`) wired into the sub-controller graph via `Consumer` callbacks in `MainController`. The design is sound — `ObservableList` + `FilteredList`, programmatic `ToggleGroup`, cell factory for severity colors, and row-click navigation. No critical issues were found.

Three warnings stand out: the `setOnFailed` path in `RenderController` never invokes `errorsCallback`, leaving stale errors visible; all `PreviewError` entries are unconditionally promoted to `"ERROR"` level regardless of source severity; and the CSS column-header text-fill selector is missing the required `.label` descendant to take effect in JavaFX. Four info-level items cover a dead `@FXML` field, a location-parsing edge case on non-Unix paths, lossy INFO history, and a silent `updateTitle` guard.

---

## Warnings

### WR-01: `setOnFailed` does not clear or replace the log panel

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/RenderController.java:161`

**Issue:** When the background `Task<Preview>` throws an unexpected exception (the `setOnFailed` branch), `errorsCallback` is never called. The log panel retains errors from the previous render run while the status label says "Render failed" — the user sees no entry that explains the current failure, and the stale previous errors are misleading.

`setOnSucceeded` / failure branch calls `errorsCallback.accept(result.errors())` correctly, but `setOnFailed` only calls `infoCallback` with a one-line message.

**Fix:**
```java
task.setOnFailed(e -> {
    renderButton.setDisable(false);
    renderButton.setText("Render");
    renderButton.disableProperty().bind(projectContext.projectLoadedProperty().not());
    Throwable ex = task.getException();
    String msg = ex != null ? ex.getMessage() : "unknown";
    // Clear previous errors and surface the unexpected failure as a log entry
    errorsCallback.accept(List.of(
        new ch.ti.gagi.xsleditor.preview.PreviewError(
            "Unexpected render error: " + msg, "INTERNAL", null, null)));
    statusTransient.accept("Render failed");
});
```
Alternatively, call `errorsCallback.accept(List.of())` to at least clear stale entries and add the detail via `infoCallback`.

---

### WR-02: All `PreviewError` entries are hardcoded to `"ERROR"` level

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/LogController.java:153-157`

**Issue:** `setErrors` maps every `PreviewError` to a `LogEntry` with level `"ERROR"` unconditionally:
```java
allEntries.add(new LogEntry(
    err.message(), "ERROR", now,       // <-- always ERROR
    err.type(), err.file(), err.line()));
```
`PreviewError` has no severity field today, so all pipeline diagnostics (including FO processor warnings that appear in the error list) are displayed as red `ERROR` entries. When the pipeline is extended to surface recoverable warnings, they will be wrongly shown as errors and will also trigger the auto-expand behavior (D-12), which is reserved for errors/warnings.

**Fix:** Add a `severity` field to `PreviewError` (defaulting to `"ERROR"` for existing callsites) and pass it through to `LogEntry`:
```java
// PreviewError — add field
public final class PreviewError {
    private final String severity; // "ERROR" | "WARN" | "INFO"
    // ...
    public String severity() { return severity != null ? severity : "ERROR"; }
}

// LogController.setErrors
allEntries.add(new LogEntry(
    err.message(), err.severity(), now,
    err.type(), err.file(), err.line()));
```

---

### WR-03: CSS column-header text color selector will not apply

**File:** `src/main/resources/ch/ti/gagi/xsleditor/ui/main.css:138-143`

**Issue:** The rule:
```css
.log-table-view .column-header,
.log-table-view .column-header-background {
    -fx-background-color: #252526;
    -fx-text-fill: #888888;   /* <-- has no effect */
    -fx-font-size: 11px;
}
```
In JavaFX `TableView`, column header text is rendered by a nested `Label` node inside `.column-header`. `-fx-text-fill` set directly on `.column-header` is not inherited by that label. The background color and font-size rules will apply; only the text color will silently have no effect, leaving header text at the default (likely white or system default), which may be invisible or wrong on a dark background.

**Fix:**
```css
.log-table-view .column-header .label {
    -fx-text-fill: #888888;
    -fx-font-size: 11px;
}
```
Keep the background color rule on `.column-header` and `.column-header-background` separately.

---

## Info

### IN-01: `menuItemRender` is injected but never used

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java:71`

**Issue:** `@FXML private MenuItem menuItemRender;` is declared and FXML-injected but is never referenced in the controller body. The accelerator and `onAction="#handleRender"` wiring are in FXML, so the menu item works — the field is simply dead. This suggests a planned use (e.g., disabling it during render like the render button) that was not implemented.

**Fix:** Either remove the field if no programmatic access is planned, or wire it to `renderButton.disableProperty()` to disable the menu item during rendering, consistent with how the toolbar button is treated:
```java
menuItemRender.disableProperty().bind(projectContext.projectLoadedProperty().not());
// and unbind/rebind in RenderController alongside renderButton
```

---

### IN-02: `PreviewManager` location parsing fails silently on Windows-style paths

**File:** `src/main/java/ch/ti/gagi/xsleditor/preview/PreviewManager.java:37-44`

**Issue:** `location.lastIndexOf(':')` is used to split `"file:line"`. On Windows absolute paths (`C:\path\to\file:42`), this finds the drive-letter colon rather than the line separator, causing the line number to not be parsed and `file` to be set to the full location string including the line number. Navigation from the log panel will silently fail (the file path will not resolve). This project is macOS-only per the CLAUDE.md description, so this is low-risk today, but the silent failure mode is worth noting.

**Fix:** If macOS-only, add a comment documenting the assumption. If portability matters, use a regex that anchors the trailing `:digits` pattern:
```java
java.util.regex.Matcher m =
    java.util.regex.Pattern.compile("^(.+):(\\d+)$").matcher(location);
if (m.matches()) {
    file = m.group(1);
    line = Integer.parseInt(m.group(2));
} else {
    file = location;
}
```

---

### IN-03: `setErrors` discards all prior INFO entries

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/LogController.java:151`

**Issue:** `allEntries.clear()` on every `setErrors` call wipes `addInfo` entries added by `handleOpenProject` or earlier renders. After the first render attempt, the "Loaded entrypoint: ..." project-open message is gone from the log. This is consistent with the ERR-05 spec ("clears before populating"), but means INFO history is non-persistent across render cycles, which may surprise users who open the log panel after a failed render.

**Fix:** No code change required if this is intentional. If INFO history should survive render cycles, maintain two lists (persistent INFO history + current render errors) and compose the filtered view from both. At minimum, document the behavior in the method Javadoc.

---

### IN-04: `updateTitle` silent guard may mask call-ordering bugs in production

**File:** `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java:159-165`

**Issue:** The `updateTitle` guard silently returns when `primaryStage == null`, with a comment noting this "should" never happen and suggesting an `IllegalStateException` during development. In the current build there is no way to distinguish a production silent-skip from an actual call-ordering regression. The guard is safe (no crash), but the suggestion in the comment is not implemented.

**Fix:** If this is a developer tool with no strict release hardening, consider converting the guard to an assertion or throwing `IllegalStateException` unconditionally:
```java
if (primaryStage == null) {
    throw new IllegalStateException(
        "updateTitle() called before setPrimaryStage() — call ordering bug");
}
```
Since `setPrimaryStage` is always called before any user-triggered path, this will only fire during test or integration regressions, not in normal use.

---

_Reviewed: 2026-04-21T05:02:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
