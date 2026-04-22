# Phase 7: PDF Preview Panel - Research

**Researched:** 2026-04-20
**Domain:** JavaFX WebView PDF rendering, sub-controller pattern, node visibility, threading
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Use WebView + temp file approach. Write `byte[]` to a fixed temp file created once per
  session via `Files.createTempFile("xsleditor-preview", ".pdf")`. Load it with
  `webView.getEngine().load(tempFile.toUri().toString())`. Overwrite the same temp file on each
  successful render — no new file per render, no explicit cleanup needed (OS cleans at reboot).
- **D-02:** PDFViewerFX is NOT available on Maven Central. WebView (already in `javafx.web` module,
  already in FXML as `fx:id="previewWebView"`) is the confirmed implementation technology.
- **D-03:** Scroll and zoom rely on the platform's built-in WebView PDF plugin (Chrome/Edge on
  Windows, WebKit on macOS). Acceptable for an internal developer tool.
- **D-04:** Outdated banner: a `Label` aligned to `TOP_CENTER` inside `previewPane` StackPane,
  initially `managed="false" visible="false"`. Made visible when `outdatedCallback` is called
  with `true`.
- **D-05:** Banner style: orange (`#f97316`) background, text `"Preview outdated — last render
  failed"`, CSS class `"preview-outdated-banner"`. Defined in `main.css`.
- **D-06:** Banner hidden again on next successful render (`outdatedCallback` receives `false`).
- **D-07:** Dedicated `PreviewController` class, following established pattern. `MainController`
  creates one instance as a field and calls
  `previewController.initialize(previewPane, previewWebView, previewPlaceholderLabel, outdatedBannerLabel)`
  from its own `initialize()`.
- **D-08:** `PreviewController` exposes:
  - `displayPdf(byte[])` — writes bytes to temp file, loads WebView
  - `setOutdated(boolean)` — shows/hides outdated banner
- **D-09:** `MainController.initialize()` replaces Phase 6 no-op lambdas:
  - `bytes -> { }` → `previewController::displayPdf`
  - `b -> { }` → `previewController::setOutdated`
- **D-10:** Before first render: `previewWebView` hidden, `previewPlaceholderLabel` visible.
- **D-11:** On first successful render: hide `previewPlaceholderLabel`, show `previewWebView`.
  From this point `previewWebView` stays visible even after failures.
- **D-12:** After render failure with prior PDF: keep `previewWebView` visible with last PDF, show
  outdated banner.
- **D-13:** After render failure with no prior render: `previewWebView` stays hidden, placeholder
  stays visible, outdated banner NOT shown.

### Claude's Discretion

- Exact FXML node order / StackPane.alignment values for the outdated banner
- Whether temp file is a field in `PreviewController` or created in `initialize()`
- Whether `displayPdf(byte[])` handles `IOException` with an Alert or logs silently to console

### Deferred Ideas (OUT OF SCOPE)

- Log entries copyable — Phase 8 responsibility
- Zoom controls UI (explicit zoom in/out buttons) — WebView native PDF viewer handles zoom; deferred
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PREV-01 | Split view shows editor on left, PDF preview on right | Already satisfied by Phase 6 SplitPane; Phase 7 wires WebView content |
| PREV-02 | PDF preview supports scroll and zoom | WebView's native PDF plugin handles both; no custom code needed |
| PREV-03 | When preview is outdated (last render failed), a visual indicator is shown | `outdatedBannerLabel` toggled via `setOutdated(boolean)` |
| PREV-04 | No PDF is shown before the first successful render | `previewWebView` starts hidden; `hasRenderedOnce` boolean tracks first-render state |
</phase_requirements>

---

## Summary

Phase 7 is a wiring phase. All infrastructure is already in place: the WebView node exists in FXML,
the temp-file approach is a locked decision, the callback seams are already in `RenderController`
as no-op lambdas, and the four display states are fully specified in the UI contract. The
implementation is a new `PreviewController` class (roughly 80–100 lines) plus three small edits —
FXML (add banner label), CSS (add one rule), and `MainController` (replace two lambdas).

The primary technical concern is the WebView reload pattern. JavaFX `WebEngine.load()` is
asynchronous and does not detect that the underlying file has changed if the URI is identical to
the previously loaded URI — calling `load()` with the same URI a second time may be a no-op on
some platforms. The correct mitigation (confirmed by the locked decision D-01) is to overwrite a
fixed temp file and reload via `load(uri)` each call; however, a practical reinforcement pattern
used in this codebase's context is to call `load("")` (blank) then `load(uri)` to force a fresh
load. This is a contingency, not a requirement of Wave 0 — the planner should note it as a
potential pitfall to address if the first render after the second call appears to show a stale PDF.

Threading is straightforward: `RenderController.handleRender()` already calls both callbacks on
the JavaFX Application Thread (inside `task.setOnSucceeded`), so `PreviewController.displayPdf()`
and `setOutdated()` will always be called on the FX thread. No `Platform.runLater()` is needed
inside `PreviewController` itself.

**Primary recommendation:** Implement `PreviewController` with a `hasPdf` boolean flag to track
whether a successful render has ever occurred, then wire it into `MainController.initialize()` by
replacing the two no-op lambdas.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| PDF byte[] → temp file write | PreviewController | — | PreviewController owns all preview I/O |
| WebView URI load | PreviewController | — | WebView is owned by PreviewController |
| Outdated banner visibility toggle | PreviewController | — | State and view both in PreviewController |
| Placeholder hide/show | PreviewController | — | First-render state tracked by PreviewController |
| Callback wiring (lambda replace) | MainController | — | MainController is the FXML controller and wiring hub |
| FXML node declaration | main.fxml | — | Single FXML controller pattern throughout project |
| CSS styling | main.css | — | Single stylesheet, per project convention |

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| javafx.web (WebView) | 21 | Renders PDF from file:// URI via platform PDF plugin | Already on classpath; declared in build.gradle javafx modules |
| java.nio.file (Files, Path) | JDK 21 | `Files.createTempFile()`, `Files.write()` | Standard JDK; no dependency needed |

No new dependencies required for Phase 7. [VERIFIED: build.gradle line 18 — `javafx.web` already declared]

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| javafx.animation (PauseTransition) | 21 | Used in `showTransientStatus()` — NOT needed in Phase 7 | Already in project for Phase 2 status feedback |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Fixed temp file (D-01) | New file per render | New-file approach avoids same-URI reload problem but accumulates files in /tmp; fixed-file approach is simpler and OS cleans up |
| WebView file:// URI | `webEngine.loadContent(base64)` | Base64 embed avoids temp file entirely but PDF base64 content is large and not all WebKit versions handle `data:application/pdf;base64,...` correctly |

---

## Architecture Patterns

### System Architecture Diagram

```
RenderController.handleRender() [FX thread, task.setOnSucceeded]
         |
         |-- success path --> pdfCallback.accept(bytes)
         |                        |
         |                        v
         |                PreviewController.displayPdf(bytes)
         |                        |
         |                        +--> Files.write(tempFile, bytes)
         |                        +--> engine.load(tempFile.toUri().toString())
         |                        +--> if first render: hide placeholder, show WebView
         |                        +--> setOutdated(false) [called separately]
         |
         |-- failure path --> outdatedCallback.accept(true)
                                  |
                                  v
                          PreviewController.setOutdated(true)
                                  |
                                  +--> if hasPdf: show outdatedBannerLabel
                                  +--> if !hasPdf: no-op (D-13)
```

### Recommended Project Structure

No new directories. Single new file:

```
src/main/java/ch/ti/gagi/xsleditor/ui/
├── PreviewController.java     # NEW — Phase 7
├── MainController.java        # EDIT — replace 2 no-op lambdas, add previewController field
src/main/resources/ch/ti/gagi/xsleditor/ui/
├── main.fxml                  # EDIT — add outdatedBannerLabel as first child of previewPane
├── main.css                   # EDIT — add .preview-outdated-banner rule
```

### Pattern 1: Sub-Controller with initialize()

The project uses a consistent sub-controller pattern. `PreviewController` follows the same shape
as `RenderController`, `EditorController`, and `FileTreeController`:

```java
// Source: verified by reading RenderController.java, FileTreeController.java
public final class PreviewController {

    private StackPane previewPane;
    private WebView previewWebView;
    private Label previewPlaceholderLabel;
    private Label outdatedBannerLabel;
    private Path tempFile;
    private boolean hasPdf = false;

    public void initialize(
        StackPane previewPane,
        WebView previewWebView,
        Label previewPlaceholderLabel,
        Label outdatedBannerLabel
    ) {
        this.previewPane             = Objects.requireNonNull(previewPane);
        this.previewWebView          = Objects.requireNonNull(previewWebView);
        this.previewPlaceholderLabel = Objects.requireNonNull(previewPlaceholderLabel);
        this.outdatedBannerLabel     = Objects.requireNonNull(outdatedBannerLabel);
        try {
            this.tempFile = Files.createTempFile("xsleditor-preview", ".pdf");
        } catch (IOException e) {
            // Temp file creation failure: log to stderr; displayPdf will be a no-op
            System.err.println("[PreviewController] Failed to create temp file: " + e.getMessage());
        }
    }

    public void displayPdf(byte[] bytes) {
        if (tempFile == null) return;
        try {
            Files.write(tempFile, bytes);
        } catch (IOException e) {
            System.err.println("[PreviewController] Failed to write PDF to temp file: " + e.getMessage());
            return;
        }
        // Reload. Call load("") first to force WebKit to treat the next load() as a new request.
        // Required on macOS WebKit when the URI is identical to the previous load.
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

    public void setOutdated(boolean outdated) {
        if (outdated && !hasPdf) return; // D-13: no banner without prior PDF
        outdatedBannerLabel.setManaged(outdated);
        outdatedBannerLabel.setVisible(outdated);
    }
}
```

[VERIFIED: pattern matches existing sub-controllers in codebase]

### Pattern 2: managed + visible Node Toggle

The project already uses the `managed=false, visible=false` pattern for hiding nodes without
affecting layout. [VERIFIED: existing FXML shows `previewPlaceholderLabel` with no explicit
managed/visible — it defaults to both true, matching the "visible by default" design]:

```java
// Source: openjfx.io/javadoc/21 Node API (VERIFIED via Context7)
node.setManaged(false); // removes from layout calculation
node.setVisible(false); // hides from rendering
// Restore:
node.setManaged(true);
node.setVisible(true);
```

Setting only `visible=false` keeps the node in the layout flow (it occupies space).
Setting `managed=false` also removes it from the layout — this is the correct pattern for
overlays in a StackPane.

### Pattern 3: WebEngine.load() for File URI

```java
// Source: openjfx.io/javadoc/21 WebEngine API (VERIFIED via Context7)
webView.getEngine().load(tempFile.toUri().toString());
// toUri().toString() produces: "file:///path/to/xsleditor-preview12345.pdf"
// WebEngine.load() is asynchronous — returns immediately, loads in background.
// All UI updates around this call are safe on the FX thread without Platform.runLater().
```

### Pattern 4: FXML outdated banner label

The UI spec defines the exact FXML snippet (07-UI-SPEC.md Component Inventory):

```xml
<!-- Source: 07-UI-SPEC.md § Component Inventory (VERIFIED) -->
<Label fx:id="outdatedBannerLabel"
       text="Preview outdated — last render failed"
       styleClass="preview-outdated-banner"
       managed="false"
       visible="false"
       maxWidth="Infinity"
       StackPane.alignment="TOP_CENTER"/>
```

This must be added as a child of `previewPane` StackPane. In StackPane, children are rendered
in declaration order (later children render on top). The banner must be the LAST child declared
so it overlays the WebView.

### Pattern 5: MainController lambda replacement

```java
// Source: MainController.java lines 107-108 (VERIFIED by reading file)
// Replace:
bytes -> { },   // Phase 6 no-op
b -> { },       // Phase 6 no-op
// With:
previewController::displayPdf,
previewController::setOutdated,
```

The `previewController` field must be declared and initialized before `renderController.initialize()`
is called, since both happen in `MainController.initialize()`.

### Anti-Patterns to Avoid

- **Calling `Platform.runLater()` inside `PreviewController`:** Both callbacks are already called
  on the FX thread by `RenderController.handleRender()` (inside `task.setOnSucceeded`). Wrapping
  again in `Platform.runLater()` is redundant and causes unnecessary deferred execution.
- **Creating a new temp file on each render:** Produces accumulating files in the OS temp directory.
  The locked decision (D-01) uses a single fixed temp file created once in `initialize()`.
- **Setting only `visible=false` without `managed=false`:** The banner would remain in the layout
  flow, reserving space at the top of the StackPane even when hidden, displacing the WebView.
- **Adding the banner label before the WebView in FXML:** StackPane renders children in order;
  if the banner is the first child it will render behind the WebView and be invisible.
- **Not calling `load("")` before `load(uri)` on subsequent renders:** On macOS WebKit, if the URI
  string is identical to the previously loaded URI, `load()` may be treated as a no-op and the
  WebView will continue showing the old PDF. [ASSUMED — common macOS WebKit behavior, not verified
  against JavaFX 21 release notes]

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| PDF rendering | Custom PDFBox renderer | WebView + temp file | WebView provides platform PDF rendering with scroll/zoom built-in; PDFBox rendering into an image requires custom zoom/scroll controls |
| Scroll/zoom controls | Manual ScrollPane + zoom buttons | Native WebView PDF plugin | Deferred by D-03 and UI spec; platform PDF plugins provide full UX |
| Node animation for banner | Custom fade/slide | Static show/hide | D-04 explicitly: no animation; simpler code, no regression risk |

**Key insight:** WebView's platform PDF plugin eliminates the need for any custom PDF rendering,
scroll, or zoom code. The entire preview feature reduces to: write bytes to file, call load(uri),
toggle node visibility.

---

## Common Pitfalls

### Pitfall 1: WebEngine Same-URI Reload No-Op

**What goes wrong:** After the first successful render, the second render writes new bytes to the
same temp file and calls `load(uri)` again. Because the URI string is identical, some WebKit
versions treat this as a cache hit and display the stale PDF.

**Why it happens:** WebView/WebKit caches by URL. If the URL has not changed, the page is served
from cache rather than re-read from disk.

**How to avoid:** Before calling `load(tempFile.toUri().toString())`, first call
`previewWebView.getEngine().load("")`. This forces WebKit to unload the current page, making the
subsequent `load(uri)` a fresh fetch.

**Warning signs:** User renders twice; second render appears to show no change even though the
XSLT output was different.

### Pitfall 2: Calling Both setManaged and setVisible in Wrong Order

**What goes wrong:** A node can be `managed=true, visible=false` (occupies layout space but is
invisible) or `managed=false, visible=true` (not in layout but technically visible if overlapping).
Either inconsistency produces unexpected spacing.

**How to avoid:** Always set both properties together:
```java
node.setManaged(false); node.setVisible(false);  // hide
node.setManaged(true);  node.setVisible(true);   // show
```

**Warning signs:** Empty whitespace gap at top of preview pane when banner should be hidden.

### Pitfall 3: FXML @FXML injection mismatch

**What goes wrong:** Adding `outdatedBannerLabel` to FXML but not adding the `@FXML` annotation
in `MainController` (since `MainController` is the FXML controller). The node will be `null` at
runtime when passed to `PreviewController.initialize()`.

**How to avoid:** Add `@FXML private Label outdatedBannerLabel;` to `MainController` alongside
the other `@FXML` fields. Pass it to `previewController.initialize()`.

**Warning signs:** `NullPointerException` in `PreviewController.setOutdated()` at first call.

### Pitfall 4: StackPane Child Ordering for Banner Overlay

**What goes wrong:** If the banner label is added as the first child of `previewPane` in FXML,
it will be rendered below (behind) the WebView and invisible.

**How to avoid:** In StackPane, later children appear on top. The banner must be the last child
in the FXML declaration:
```
previewPane (StackPane)
  └── previewWebView      (child 1 — background)
  └── previewPlaceholderLabel  (child 2 — overlays WebView when shown)
  └── outdatedBannerLabel      (child 3 — topmost layer, anchored TOP_CENTER)
```

**Warning signs:** Banner set to visible but never visible on screen.

### Pitfall 5: displayPdf() Called Before initialize()

**What goes wrong:** If `MainController.initialize()` calls `previewController.initialize(...)`
after wiring the lambdas to `renderController`, the callbacks could theoretically be invoked
before `PreviewController` fields are populated (not possible in current single-threaded
initialization, but worth guarding with `Objects.requireNonNull`).

**How to avoid:** Initialize `previewController` before passing its methods as lambdas to
`renderController.initialize()`. The existing `initialize()` method body already sequences
sub-controller initialization linearly, so ordering is straightforward.

---

## Code Examples

### Verified Pattern: Reading existing `previewPane` StackPane child ordering

```xml
<!-- Source: main.fxml lines 73-79 (VERIFIED by reading file) -->
<StackPane fx:id="previewPane" styleClass="placeholder-pane">
    <WebView fx:id="previewWebView"/>
    <Label fx:id="previewPlaceholderLabel"
           text="No preview — trigger a render first"
           styleClass="placeholder-label"/>
    <!-- Phase 7: add outdatedBannerLabel here as last child -->
</StackPane>
```

### Verified Pattern: CSS rule placement in main.css

```css
/* Source: 07-UI-SPEC.md CSS Contract (VERIFIED) */
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

### Verified Pattern: Callback wiring in MainController

```java
// Source: MainController.java lines 101-111 (VERIFIED by reading file)
// Phase 7 edit — add before renderController.initialize():
private final PreviewController previewController = new PreviewController();

// In initialize():
previewController.initialize(
    previewPane,
    previewWebView,
    previewPlaceholderLabel,
    outdatedBannerLabel           // new @FXML injection
);
renderController.initialize(
    renderButton,
    logListView,
    s -> statusLabel.setText(s),
    this::showTransientStatus,
    previewController::displayPdf,    // replaces bytes -> { }
    previewController::setOutdated,   // replaces b -> { }
    projectContext,
    editorController
);
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| PDFViewerFX (planned in ROADMAP.md) | WebView + temp file | Phase 1 (PDFViewerFX not on Maven Central) | Simpler dependency setup; platform PDF plugin provides native scroll/zoom |

**Deprecated/outdated:**
- ROADMAP.md Phase 7 mentions "PDFViewerFX component" — this is stale. The actual implementation
  is WebView (confirmed in CONTEXT.md D-02 and STATE.md decisions table).

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | On macOS WebKit, calling `load(uri)` a second time with the same URI may be a no-op (cache hit), requiring a `load("")` blank-page call first | Common Pitfalls §1, Pattern 1 | If wrong (WebKit always reloads file:// URIs), the `load("")` call is a harmless extra navigation; no broken behavior |
| A2 | `previewWebView` currently defaults to `managed=true, visible=true` (no explicit FXML attributes set) | FXML Analysis | If wrong (WebView is already hidden), Phase 7 initialization state logic needs adjustment |

A2 note: FXML at lines 74 shows `<WebView fx:id="previewWebView"/>` with no `managed` or `visible`
attributes. JavaFX defaults for both are `true`. [VERIFIED: main.fxml line 74]

**If this table is empty (it is not):** Two assumptions identified; both are low-risk.

---

## Open Questions

1. **WebView PDF rendering on macOS Sonoma (WebKit)**
   - What we know: WebKit on macOS has a built-in PDF plugin that renders `file://` PDFs inline.
     This has worked since at least macOS Big Sur.
   - What's unclear: Whether JavaFX 21's WebView on macOS 14 (Sonoma, Darwin 24.6.0) renders
     PDFs inline or prompts a download, and whether the blank-page reload trick is needed.
   - Recommendation: Implement the `load("")` then `load(uri)` pattern as a defensive default.
     If the second render shows correctly without the blank reload, the extra `load("")` call
     is harmless and can be removed in cleanup.

2. **`previewWebView` initial visibility in FXML**
   - What we know: FXML line 74 declares `<WebView fx:id="previewWebView"/>` with no visibility
     attributes. JavaFX defaults: `managed=true, visible=true`. This means the WebView is
     currently visible and takes up space in the StackPane at app start, showing a blank white
     area instead of the placeholder label.
   - What's unclear: Whether Phase 6 or earlier phases already handled this (the placeholder
     label overlays the WebView via StackPane layering, so the visual may be acceptable).
   - Recommendation: Phase 7 Wave 0 should set `managed="false" visible="false"` on
     `previewWebView` in FXML (matching the placeholder pattern), so the initial state is
     correct per D-10. This is a one-line FXML fix.

---

## Environment Availability

Step 2.6: SKIPPED (no external dependencies — Phase 7 is pure JavaFX code using javafx.web
already on the classpath and standard JDK Files API).

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter 5.10.0 |
| Config file | build.gradle (`test { useJUnitPlatform() }`) |
| Quick run command | `./gradlew test --tests "ch.ti.gagi.xsleditor.ui.PreviewControllerTest"` |
| Full suite command | `./gradlew test` |

[VERIFIED: build.gradle line 57]

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PREV-01 | Split view layout | manual-only | — visual inspection | N/A |
| PREV-02 | Scroll and zoom | manual-only | — native WebView PDF plugin, not unit-testable | N/A |
| PREV-03 | Outdated indicator shows/hides | unit | `./gradlew test --tests "*.PreviewControllerTest"` | ❌ Wave 0 |
| PREV-04 | No PDF before first render | unit | `./gradlew test --tests "*.PreviewControllerTest"` | ❌ Wave 0 |

**Manual-only justification (PREV-01, PREV-02):** These require a running JavaFX Stage with a
rendered PDF file. JavaFX UI tests without a display are not viable in this project's test
infrastructure (no TestFX or headless toolkit configured). Consistent with Phase 6 pattern
(REND-01 and REND-06 are also manual-only).

### Unit-Testable Behaviors

`PreviewController` has pure-logic state transitions that CAN be tested:

- `setOutdated(true)` when `hasPdf=false` → banner remains hidden (D-13)
- `setOutdated(true)` when `hasPdf=true` → banner becomes visible
- `setOutdated(false)` → banner hidden regardless of state
- `displayPdf(bytes)` sets `hasPdf=true` and hides placeholder (requires FX toolkit init,
  same as `EditorTabTest` pattern with `Platform.startup()`)

### Sampling Rate

- **Per task commit:** `./gradlew test --tests "*.PreviewControllerTest"`
- **Per wave merge:** `./gradlew test`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps

- [ ] `src/test/java/ch/ti/gagi/xsleditor/ui/PreviewControllerTest.java` — covers PREV-03, PREV-04

*(Existing test infrastructure — JUnit 5, `Platform.startup()` pattern — covers all Phase 7
  test needs. No framework install or additional fixtures needed.)*

---

## Security Domain

Phase 7 writes a `byte[]` (PDF bytes produced by the internal FOP render pipeline) to a temp file
and loads it in WebView. There are no external inputs, no user-supplied file paths, and no network
requests.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — |
| V3 Session Management | no | — |
| V4 Access Control | no | — |
| V5 Input Validation | partial | `byte[]` bytes come from internal pipeline, not user input |
| V6 Cryptography | no | — |

**V5 note:** The temp file path is constructed by `Files.createTempFile()` (JDK), which is
safe from path traversal. The bytes written are the output of the internal Saxon/FOP pipeline,
not raw user input. No sanitization required beyond what the pipeline already provides.

### Known Threat Patterns

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Temp file disclosure | Information disclosure | Temp file contains the last rendered PDF — acceptable for a local developer tool; OS cleans on reboot |
| Malformed PDF bytes | Tampering (hypothetical) | WebView renders via platform PDF plugin; malformed PDF is rejected by the plugin, not a security risk |

---

## Sources

### Primary (HIGH confidence)

- `/websites/openjfx_io_javadoc_21` (Context7) — WebView, WebEngine, Node visibility, Platform.runLater
- `src/main/java/ch/ti/gagi/xsleditor/ui/RenderController.java` — callback seam signatures verified
- `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` — no-op lambda locations verified
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` — previewPane StackPane structure verified
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.css` — existing CSS conventions verified
- `.planning/phases/07-pdf-preview-panel/07-CONTEXT.md` — all locked decisions
- `.planning/phases/07-pdf-preview-panel/07-UI-SPEC.md` — CSS contract, component inventory

### Secondary (MEDIUM confidence)

- Pattern established by `FileTreeController.java`, `EditorController.java` — sub-controller shape

### Tertiary (LOW confidence)

- macOS WebKit same-URI cache behavior (A1) — [ASSUMED], not verified against JavaFX 21 release
  notes or macOS Sonoma WebKit changelog

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — javafx.web already on classpath, no new deps
- Architecture: HIGH — all patterns verified in existing codebase
- Pitfalls: MEDIUM — WebView same-URI reload is assumed behavior; other pitfalls are verified
- Validation: HIGH — test pattern matches EditorTabTest/RenderControllerTest exactly

**Research date:** 2026-04-20
**Valid until:** 2026-05-20 (stable JavaFX 21 API, no fast-moving dependencies)
