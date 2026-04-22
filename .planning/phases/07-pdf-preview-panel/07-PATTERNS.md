# Phase 7: PDF Preview Panel - Pattern Map

**Mapped:** 2026-04-20
**Files analyzed:** 4 (1 new, 3 edits)
**Analogs found:** 4 / 4

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `src/main/java/ch/ti/gagi/xsleditor/ui/PreviewController.java` | sub-controller | request-response | `src/main/java/ch/ti/gagi/xsleditor/ui/RenderController.java` | exact |
| `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` | controller (edit) | request-response | self — existing file | n/a (edit) |
| `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` | FXML view (edit) | n/a | self — lines 73–78 (previewPane StackPane) | n/a (edit) |
| `src/main/resources/ch/ti/gagi/xsleditor/ui/main.css` | stylesheet (edit) | n/a | self — `.status-label-success` rule (line 34) | n/a (edit) |

---

## Pattern Assignments

### `src/main/java/ch/ti/gagi/xsleditor/ui/PreviewController.java` (new sub-controller, request-response)

**Analog:** `src/main/java/ch/ti/gagi/xsleditor/ui/RenderController.java`

**Imports pattern** — copy structure from RenderController.java lines 1–16:
```java
package ch.ti.gagi.xsleditor.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
```
Note: no `javafx.concurrent`, no `Consumer` (PreviewController receives calls directly rather than storing callbacks). `java.nio.file.Files` and `Path` replace RenderController's render-pipeline imports.

**Class declaration pattern** — copy `final` modifier and Javadoc shape from RenderController.java lines 28–27:
```java
/**
 * Sub-controller for the PDF preview pane.
 * Owns temp-file I/O, WebView URI load, placeholder show/hide, and
 * outdated banner toggle.
 *
 * Lifecycle: MainController creates one instance as a field and calls
 * {@link #initialize} from its own {@code initialize()}. This controller is
 * NOT an FXML controller — MainController is the only @FXML controller.
 *
 * Phase 7 / PREV-01..PREV-04
 */
public final class PreviewController {
```

**State fields pattern** — copy private field block shape from RenderController.java lines 32–40:
```java
    private StackPane previewPane;
    private WebView previewWebView;
    private Label previewPlaceholderLabel;
    private Label outdatedBannerLabel;
    private Path tempFile;
    private boolean hasPdf = false;
```

**initialize() signature pattern** — copy parameter-list style and `Objects.requireNonNull` guards from RenderController.java lines 57–79:
```java
    public void initialize(
        StackPane previewPane,
        WebView previewWebView,
        Label previewPlaceholderLabel,
        Label outdatedBannerLabel
    ) {
        this.previewPane             = Objects.requireNonNull(previewPane,             "previewPane");
        this.previewWebView          = Objects.requireNonNull(previewWebView,          "previewWebView");
        this.previewPlaceholderLabel = Objects.requireNonNull(previewPlaceholderLabel, "previewPlaceholderLabel");
        this.outdatedBannerLabel     = Objects.requireNonNull(outdatedBannerLabel,     "outdatedBannerLabel");
        try {
            this.tempFile = Files.createTempFile("xsleditor-preview", ".pdf");
        } catch (IOException e) {
            System.err.println("[PreviewController] Failed to create temp file: " + e.getMessage());
        }
    }
```
The `Objects.requireNonNull` guard with string label is the exact pattern at RenderController.java lines 67–74. Temp file creation failure is silent-to-stderr (same pattern as other phases' non-fatal init failures — the controller degrades gracefully rather than throwing).

**Core method pattern — displayPdf():**
```java
    public void displayPdf(byte[] bytes) {
        if (tempFile == null) return;
        try {
            Files.write(tempFile, bytes);
        } catch (IOException e) {
            System.err.println("[PreviewController] Failed to write PDF to temp file: " + e.getMessage());
            return;
        }
        previewWebView.getEngine().load("");
        previewWebView.getEngine().load(tempFile.toUri().toString());

        if (!hasPdf) {
            hasPdf = true;
            previewPlaceholderLabel.setManaged(false);
            previewPlaceholderLabel.setVisible(false);
            previewWebView.setManaged(true);
            previewWebView.setVisible(true);
        }
        setOutdated(false);
    }
```
The `setManaged(false); setVisible(false)` pair is the established pattern for hiding nodes without layout impact (see CONTEXT.md §Established Patterns and RESEARCH.md §Pattern 2). Always set both together.

**Core method pattern — setOutdated():**
```java
    public void setOutdated(boolean outdated) {
        if (outdated && !hasPdf) return;  // D-13: no banner without prior PDF
        outdatedBannerLabel.setManaged(outdated);
        outdatedBannerLabel.setVisible(outdated);
    }
```

**No-analog for test file** — see `src/test/java/ch/ti/gagi/xsleditor/ui/PreviewControllerTest.java` below.

---

### `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` (edit — add field + replace two lambdas)

**Analog:** self (existing file at lines 63–111)

**Sub-controller field declaration pattern** — copy from MainController.java lines 69–71:
```java
    private final FileTreeController fileTreeController = new FileTreeController();
    private final EditorController editorController = new EditorController();  // Phase 4
    private final RenderController renderController = new RenderController();  // Phase 6
```
Add immediately after renderController field (line 71):
```java
    private final PreviewController previewController = new PreviewController();  // Phase 7
```

**@FXML injection pattern for new node** — copy from MainController.java lines 44–45:
```java
    @FXML private WebView previewWebView;
    @FXML private Label previewPlaceholderLabel;
```
Add immediately after previewPlaceholderLabel (after line 45):
```java
    @FXML private Label outdatedBannerLabel;
```
Pattern: `@FXML private <Type> <fxId>;` — field name must exactly match the `fx:id` in FXML.

**initialize() sub-controller call pattern** — copy call shape from MainController.java lines 81–111. The new `previewController.initialize()` call must appear BEFORE `renderController.initialize()` (so the method references are valid when passed as lambdas). Replace the two no-op lambdas at lines 107–108:
```java
        // Phase 6 no-ops to replace (lines 107-108):
        bytes -> { },   // D-15: Phase 6 no-op PDF seam; Phase 7 fills this
        b -> { },       // REND-05: Phase 6 no-op outdated seam; Phase 7 fills this

        // Phase 7 replacements:
        previewController::displayPdf,
        previewController::setOutdated,
```
Add the `previewController.initialize()` call before `renderController.initialize()` in `initialize()`:
```java
        previewController.initialize(
            previewPane,
            previewWebView,
            previewPlaceholderLabel,
            outdatedBannerLabel
        );
```

---

### `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` (edit — add banner label + fix WebView initial visibility)

**Analog:** existing `previewPane` StackPane block at main.fxml lines 73–78

**Current previewPane block** (lines 73–78):
```xml
<StackPane fx:id="previewPane" styleClass="placeholder-pane">
    <WebView fx:id="previewWebView"/>
    <Label fx:id="previewPlaceholderLabel"
           text="No preview — trigger a render first"
           styleClass="placeholder-label"/>
</StackPane>
```

**Pattern for managed/visible on hidden nodes** — the `statusLabel` at main.fxml lines 57–61 uses `StackPane.alignment`:
```xml
<Label fx:id="statusLabel"
       styleClass="placeholder-label"
       StackPane.alignment="BOTTOM_CENTER"
       style="-fx-padding: 8;"/>
```
Copy this `StackPane.alignment` attribute pattern for the outdated banner.

**Target state after Phase 7 edit:**
```xml
<StackPane fx:id="previewPane" styleClass="placeholder-pane">
    <WebView fx:id="previewWebView"
             managed="false"
             visible="false"/>
    <Label fx:id="previewPlaceholderLabel"
           text="No preview — trigger a render first"
           styleClass="placeholder-label"/>
    <Label fx:id="outdatedBannerLabel"
           text="Preview outdated — last render failed"
           styleClass="preview-outdated-banner"
           managed="false"
           visible="false"
           maxWidth="Infinity"
           StackPane.alignment="TOP_CENTER"/>
</StackPane>
```
Two changes:
1. Add `managed="false" visible="false"` to `previewWebView` (RESEARCH.md Open Question 2 — D-10 requires WebView hidden before first render; current FXML has no attributes so it defaults to visible).
2. Add `outdatedBannerLabel` as the LAST child (StackPane renders last child on top — banner must overlay WebView and placeholder).

---

### `src/main/resources/ch/ti/gagi/xsleditor/ui/main.css` (edit — add one CSS rule)

**Analog:** `.status-label-success` rule at main.css lines 34–37:
```css
.status-label-success {
    -fx-text-fill: #66bb6a;
    -fx-font-size: 13px;
}
```
Copy the single-class rule structure. Orange color `#f97316` matches the discussion in CONTEXT.md §Specific Ideas.

**New rule to append:**
```css
/* Phase 7: Outdated preview banner */
.preview-outdated-banner {
    -fx-background-color: #f97316;
    -fx-text-fill: white;
    -fx-padding: 8 12 8 12;
    -fx-font-size: 13px;
    -fx-font-weight: bold;
    -fx-alignment: CENTER;
}
```
Append after line 104 (the last existing rule). Follow the per-phase comment convention used throughout main.css (e.g., `/* Phase 3: ... */`, `/* Phase 5: ... */`).

---

### `src/test/java/ch/ti/gagi/xsleditor/ui/PreviewControllerTest.java` (new test)

**Analog:** `src/test/java/ch/ti/gagi/xsleditor/ui/RenderControllerTest.java` (primary) and `EditorTabTest.java` (for Platform.startup pattern)

**Test class skeleton pattern** — copy from RenderControllerTest.java lines 1–27:
```java
package ch.ti.gagi.xsleditor.ui;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreviewControllerTest {

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // Toolkit was already initialised by a previous test class — OK.
        }
    }
```
Copy the `Platform.startup()` guard idiom exactly — it is used in both `EditorTabTest.java` lines 13–19 and `RenderControllerTest.java` lines 21–27 and is the established pattern for tests requiring JavaFX nodes.

**Test method pattern** — copy `@Test` method structure from RenderControllerTest.java lines 29–72. Tests to implement:
- `setOutdated_doesNothing_whenHasPdfIsFalse()` — D-13
- `setOutdated_showsBanner_whenHasPdfIsTrue()` — D-04/PREV-03
- `setOutdated_false_hidesBanner()` — D-06
- `displayPdf_setHasPdfTrue_andHidesPlaceholder()` — D-11/PREV-04 (requires FX toolkit for WebView + Label instantiation)

Note: Tests that create `WebView` instances must run on the FX thread via `Platform.runLater()` and await completion — same constraint as `EditorTabTest`.

---

## Shared Patterns

### managed + visible Node Toggle
**Source:** CONTEXT.md §Established Patterns; applied throughout existing controllers
**Apply to:** PreviewController (placeholder hide/show, banner show/hide), main.fxml (WebView initial state, banner initial state)
```java
// Hide — always set BOTH properties together:
node.setManaged(false);
node.setVisible(false);
// Show:
node.setManaged(true);
node.setVisible(true);
```
Setting only `visible=false` leaves the node in the StackPane layout flow, reserving space. Setting only `managed=false` makes the node invisible from layout but technically still visible for rendering. Always set both.

### Objects.requireNonNull Guards in initialize()
**Source:** `src/main/java/ch/ti/gagi/xsleditor/ui/RenderController.java` lines 67–74
**Apply to:** PreviewController.initialize() — all four node parameters
```java
this.previewPane = Objects.requireNonNull(previewPane, "previewPane");
```
Pattern: second argument is the parameter name as a string (provides a readable NPE message). Import: `java.util.Objects`.

### Sub-controller Field Declaration in MainController
**Source:** `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` lines 69–71
**Apply to:** PreviewController field addition in MainController
```java
private final PreviewController previewController = new PreviewController();  // Phase 7
```
Pattern: `private final`, inline instantiation with `new`, phase comment.

### Phase Comment in CSS
**Source:** `src/main/resources/ch/ti/gagi/xsleditor/ui/main.css` lines 33, 45, 53, 91
**Apply to:** New `.preview-outdated-banner` rule
```css
/* Phase 7: Outdated preview banner */
```
All CSS sections carry a phase comment. Match this convention.

### Platform.startup() Guard in Tests
**Source:** `src/test/java/ch/ti/gagi/xsleditor/ui/RenderControllerTest.java` lines 21–27
**Apply to:** PreviewControllerTest.@BeforeAll
```java
@BeforeAll
static void initJavaFxToolkit() {
    try {
        Platform.startup(() -> { });
    } catch (IllegalStateException alreadyStarted) {
        // Toolkit was already initialised by a previous test class — OK.
    }
}
```

---

## No Analog Found

All four files have direct analogs in the codebase. No files require fallback to RESEARCH.md-only patterns.

---

## Metadata

**Analog search scope:** `src/main/java/ch/ti/gagi/xsleditor/ui/`, `src/main/resources/ch/ti/gagi/xsleditor/ui/`, `src/test/java/ch/ti/gagi/xsleditor/ui/`
**Files scanned:** 7 (MainController, RenderController, FileTreeController, main.fxml, main.css, RenderControllerTest, EditorTabTest)
**Pattern extraction date:** 2026-04-20
