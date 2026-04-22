# Phase 7: PDF Preview Panel - Context

**Gathered:** 2026-04-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Right panel displays PDF output from the render pipeline. Phase 7 wires the `pdfCallback` and `outdatedCallback` seams left by Phase 6 (currently no-ops in `RenderController`) to actually display the PDF via WebView and show an outdated banner on render failure.

In scope: PREV-01, PREV-02, PREV-03, PREV-04 — PDFViewerFX component (WebView), PDF display from byte[], scroll/zoom, outdated indicator, placeholder before first render.

Out of scope: Log panel UI (Phase 8), render trigger (Phase 6 done).

</domain>

<decisions>
## Implementation Decisions

### PDF rendering mechanism

- **D-01:** Use WebView + temp file approach. Write `byte[]` to a fixed temp file created once per session via `Files.createTempFile("xsleditor-preview", ".pdf")`. Load it with `webView.getEngine().load(tempFile.toUri().toString())`. Overwrite the same temp file on each successful render — no new file per render, no explicit cleanup needed (OS cleans at reboot).
- **D-02:** PDFViewerFX is NOT available on Maven Central — do not attempt to add it as a dependency. WebView (already in `javafx.web` module, already in FXML as `fx:id="previewWebView"`) is the confirmed implementation technology.
- **D-03:** Scroll and zoom rely on the platform's built-in WebView PDF plugin (Chrome/Edge on Windows, WebKit on macOS). This is acceptable for an internal developer tool.

### Outdated indicator

- **D-04:** Show a banner-style indicator at the top of `previewPane` (StackPane) when preview is outdated. Implementation: add a `Label` (or `HBox` with a label) as the first child of `previewPane` in FXML, aligned to `TOP_CENTER`, initially invisible (`managed="false" visible="false"`). Phase 7 makes it visible when `outdatedCallback` is called with `true`.
- **D-05:** Banner style: orange background, text `"Preview outdated — last render failed"`. CSS class: `"preview-outdated-banner"`. Defined in `main.css`.
- **D-06:** Banner is hidden again on next successful render (when `outdatedCallback` receives `false`).

### Controller structure

- **D-07:** Introduce a dedicated `PreviewController` class, following the established pattern (FileTreeController, EditorController, RenderController). `MainController` creates one instance as a field and calls `previewController.initialize(previewPane, previewWebView, previewPlaceholderLabel, outdatedBannerLabel)` from its own `initialize()`.
- **D-08:** `PreviewController` exposes two public methods:
  - `displayPdf(byte[])` — called by `pdfCallback` seam in `RenderController`; writes bytes to temp file and loads WebView
  - `setOutdated(boolean)` — called by `outdatedCallback` seam; shows/hides the outdated banner
- **D-09:** `MainController.initialize()` replaces the Phase 6 no-op lambdas:
  - `bytes -> { }` → `previewController::displayPdf`
  - `b -> { }` → `previewController::setOutdated`

### Placeholder and failure state behavior

- **D-10:** Before first successful render: `previewWebView` is hidden, `previewPlaceholderLabel` is visible with existing text `"No preview — trigger a render first"`.
- **D-11:** On first successful render: hide `previewPlaceholderLabel`, show `previewWebView`. From this point forward, `previewWebView` stays visible (even after subsequent render failures).
- **D-12:** After a render failure: keep `previewWebView` visible with the last successfully rendered PDF (do not revert to placeholder). Show the outdated banner (D-04). This gives the developer a reference point for debugging.
- **D-13:** After a render failure with no prior successful render: `previewWebView` stays hidden, `previewPlaceholderLabel` stays visible. The outdated banner is NOT shown (no PDF to be "outdated").

### Claude's Discretion

- Exact FXML node order / StackPane.alignment values for the outdated banner
- Whether temp file is a field in `PreviewController` or created in `initialize()`
- Whether `displayPdf(byte[])` handles `IOException` with an Alert or logs silently to console

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` §PDF Preview — PREV-01..PREV-04 (exact acceptance criteria)
- `docs/PRD.md` — Product requirements (split view, no auto-render, < 5s target)

### Existing UI seams (Phase 6 left these as no-ops — Phase 7 fills them)
- `src/main/java/ch/ti/gagi/xsleditor/ui/RenderController.java` — `pdfCallback` (line ~61) and `outdatedCallback` (line ~63) fields; constructor args in `initialize()` at lines ~84–85; wired as `bytes -> { }` and `b -> { }` in `MainController`
- `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` — `previewController` field to add; `initialize()` where seam lambdas are replaced; `previewPane`, `previewWebView`, `previewPlaceholderLabel` fx:id injections

### Existing FXML (must read before editing)
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` — `previewPane` StackPane with `previewWebView` + `previewPlaceholderLabel`; outdated banner label must be added here

### Existing CSS
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.css` — add `.preview-outdated-banner` style here

### Prior phase context
- `.planning/phases/06-render-pipeline-integration/06-CONTEXT.md` — D-15: `pdfCallback` seam; REND-05: `outdatedCallback` seam; D-04: RenderController sub-controller pattern to follow

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `previewWebView` (WebView, fx:id) — already in FXML inside `previewPane` StackPane; ready to use
- `previewPlaceholderLabel` (Label, fx:id) — already in FXML; visible by default; Phase 7 hides it on first PDF display
- `RenderController.pdfCallback` / `outdatedCallback` — Consumer seams already wired; just replace no-op lambdas in `MainController.initialize()`
- Sub-controller pattern: `initialize(pane, ...)` — same shape as FileTreeController, EditorController, RenderController

### Established Patterns
- Dedicated sub-controller per concern (all prior UI phases)
- `managed="false" visible="false"` FXML pattern to hide nodes without layout impact (see Phase 3 placeholder)
- CSS class toggle for state-based styling (see `status-label-success` in `MainController.showTransientStatus()`)
- `BooleanProperty.bind()` for reactive state (prefer over manual show/hide where possible)

### Integration Points
- `MainController.initialize()` — replace two no-op lambdas with `previewController::displayPdf` and `previewController::setOutdated`
- `previewPane` (StackPane) — Phase 7 adds outdated banner label as new child node (via FXML)
- `main.css` — add `.preview-outdated-banner` CSS rule (orange background, white text, padding)

</code_context>

<specifics>
## Specific Ideas

- Temp file created once in `PreviewController.initialize()` via `Files.createTempFile("xsleditor-preview", ".pdf")`; overwritten on each `displayPdf()` call
- Outdated banner CSS: `background-color: #f97316; -fx-text-fill: white; -fx-padding: 6 12;` — matches orange used in prior phases' discussion (same color as chat-more indicator in power mode)
- No need for animation or dismissal on the banner — it clears automatically on next successful render

</specifics>

<deferred>
## Deferred Ideas

- Log entries copyable (user request) — Phase 8 responsibility. Currently `logListView` items are not selectable/copyable. Phase 8 should use a `TableView` or `ListView` with `setEditable(false)` + `setCellFactory` that enables text selection/copy.
- Zoom controls UI (explicit zoom in/out buttons) — WebView's native PDF viewer provides browser-level zoom; explicit buttons deferred until user requests them.

</deferred>

---

*Phase: 07-pdf-preview-panel*
*Context gathered: 2026-04-19*
