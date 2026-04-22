---
phase: 07-pdf-preview-panel
plan: 02
subsystem: ui
tags: [javafx, webview, preview, pdf, sub-controller, pdfbox, tdd]

# Dependency graph
requires:
  - phase: 06-render-pipeline-integration
    provides: pdfCallback and outdatedCallback seams as no-ops in RenderController
  - plan: 07-01
    provides: Wave 0 @Disabled test stubs for PreviewController
provides:
  - PreviewController.java — PDF display logic via PDFBox page-image rendering, outdated banner toggle, placeholder state machine
  - MainController wired with previewController field and method references replacing no-op lambdas
  - main.fxml outdatedBannerLabel node + previewWebView initial hidden state
  - main.css .preview-outdated-banner rule
  - All 4 PreviewControllerTest tests enabled and passing
affects: [07-verify-work, 08-error-log-panel]

# Tech tracking
tech-stack:
  added:
    - "org.apache.pdfbox:pdfbox:2.0.31 — PDF page rendering to PNG images for WebView display"
  patterns:
    - "Sub-controller initialize() with Objects.requireNonNull guards (PreviewController)"
    - "Files.createTempFile + Files.write for PDF byte[] → temp file I/O"
    - "PDFBox PDFRenderer.renderImageWithDPI() + Base64 PNG embedding in HTML for WebView PDF display"
    - "setManaged(false) + setVisible(false) node toggle pattern (both always together)"
    - "Platform.runLater + CountDownLatch for FX-thread unit tests requiring WebView"

key-files:
  created:
    - src/main/java/ch/ti/gagi/xsleditor/ui/PreviewController.java
  modified:
    - src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java
    - src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml
    - src/main/resources/ch/ti/gagi/xsleditor/ui/main.css
    - src/test/java/ch/ti/gagi/xsleditor/ui/PreviewControllerTest.java
    - build.gradle

key-decisions:
  - "Single temp file created once in initialize() (D-01) — not per render"
  - "PDFBox rendering (not WebView file:// URI) for PDF display — macOS JavaFX WebView does not render PDFs inline via file:// URIs (WebKit limitation discovered at runtime)"
  - "PDFBox renders each page as PNG at 150 DPI, embedded as base64 data URIs in HTML loaded via loadContent()"
  - "D-13 guard: setOutdated(true) is a no-op when hasPdf=false — banner never shown before first render"
  - "previewController.initialize() called BEFORE renderController.initialize() so method references are valid"
  - "outdatedBannerLabel added as LAST child of previewPane StackPane (z-order: banner on top of WebView)"

requirements-completed: [PREV-01, PREV-02, PREV-03, PREV-04]

# Metrics
duration: ~20min
completed: 2026-04-20
---

# Phase 7 Plan 02: PDF Preview Panel Wave 1 Summary

**PreviewController wired into MainController using PDFBox PNG-rendering for WebView display — PREV-01..PREV-04 delivered, all 4 unit tests passing, human-verified**

## Performance

- **Duration:** ~20 min
- **Started:** 2026-04-20T13:13:03Z
- **Completed:** 2026-04-20T13:35:00Z
- **Tasks:** 4/4 complete
- **Files modified:** 6

## Accomplishments

- Created `PreviewController.java` with `displayPdf(byte[])` and `setOutdated(boolean)`, single temp file, D-13 guard, and PDFBox-based rendering (each PDF page rendered to PNG at 150 DPI and embedded as base64 HTML in WebView)
- Wired `MainController`: added `@FXML Label outdatedBannerLabel`, `previewController` field, `previewController.initialize()` call, replaced Phase 6 no-op lambdas with `previewController::displayPdf` and `previewController::setOutdated`
- Updated `main.fxml`: WebView starts hidden (`managed="false" visible="false"`), `outdatedBannerLabel` added as last StackPane child (correct z-order for banner overlay)
- Updated `main.css`: `.preview-outdated-banner` rule with orange `#f97316` background, Phase 7 comment
- Enabled all four `@Disabled` Wave 0 test stubs in `PreviewControllerTest.java` with full implementations using `Platform.runLater` + `CountDownLatch` — all 4 pass, full suite green
- Human verified all four behaviors (PREV-01 split view, PREV-02 scroll/zoom, PREV-03 outdated banner, PREV-04 no PDF before render) — approved

## Task Commits

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Create PreviewController.java | 8e389ba | PreviewController.java |
| 2 | Wire MainController + FXML + CSS | a27ffe2 | MainController.java, main.fxml, main.css |
| 3 | Enable PreviewControllerTest stubs | bb90d3f | PreviewControllerTest.java |
| 4 | PDFBox fix for macOS WebView | 6f7ae47 | PreviewController.java, build.gradle |

## Files Created/Modified

- `src/main/java/ch/ti/gagi/xsleditor/ui/PreviewController.java` — PDF display sub-controller with PDFBox rendering, state machine for placeholder/banner
- `src/main/java/ch/ti/gagi/xsleditor/ui/MainController.java` — added @FXML Label outdatedBannerLabel, previewController field, initialize() wiring
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.fxml` — WebView initial hidden state, outdatedBannerLabel as last StackPane child
- `src/main/resources/ch/ti/gagi/xsleditor/ui/main.css` — .preview-outdated-banner rule
- `src/test/java/ch/ti/gagi/xsleditor/ui/PreviewControllerTest.java` — 4 enabled unit tests with FX thread pattern
- `build.gradle` — added org.apache.pdfbox:pdfbox:2.0.31 dependency

## Decisions Made

- **PDFBox over WebView file:// URI**: macOS JavaFX WebView (WebKit) does not render PDFs inline via `file://` URIs at runtime. The plan's original approach (`previewWebView.getEngine().load(tempFile.toUri().toString())`) produced a blank WebView on macOS. Fixed by using PDFBox 2.0.31 to render each page as a BufferedImage at 150 DPI, encoding to PNG base64, and loading the assembled HTML via `loadContent()`. The fallback path (direct URI load) is retained for non-macOS platforms.
- **Single temp file reuse**: PDF bytes are written to one temp file created at initialize() time. This avoids accumulating temp files on disk across renders.
- **D-13 guard preserved**: `setOutdated(true)` remains a no-op when `hasPdf=false`, ensuring the outdated banner is never shown before the first render.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] PDFBox rendering fallback for macOS WebView PDF limitation**
- **Found during:** Task 4 (human verification)
- **Issue:** macOS JavaFX WebView does not render PDFs inline via `file://` URIs — WebKit on macOS lacks the PDF plugin active in Safari. The WebView rendered blank for every PDF loaded via `load(tempFile.toUri().toString())`.
- **Fix:** Added `org.apache.pdfbox:pdfbox:2.0.31` to `build.gradle`. Implemented `renderPdfToHtml(Path)` private method in `PreviewController`: opens PDF with PDDocument, iterates pages with PDFRenderer at 150 DPI, base64-encodes each PNG, assembles HTML with dark background and full-width img tags, loads via `loadContent()`. Falls back to direct URI load if PDFBox throws.
- **Files modified:** `PreviewController.java`, `build.gradle`
- **Verification:** Human confirmed PDF renders correctly after fix (Task 4 verification approved)
- **Committed in:** `6f7ae47`

---

**Total deviations:** 1 auto-fixed (Rule 1 - bug at runtime)
**Impact on plan:** Essential fix for macOS compatibility. The plan's threat model (T-07-02) was unaffected — PDF bytes still originate exclusively from the internal FOP pipeline. No scope creep.

## Issues Encountered

None beyond the macOS WebView PDF rendering limitation documented as a deviation above.

## Known Stubs

None. All display states are fully wired:
- PREV-01: SplitPane layout in place from Phase 6; WebView now receives PDF content via PDFBox HTML
- PREV-02: WebView native scroll/pan via HTML img scaling; zoom available via browser zoom keys
- PREV-03: `outdatedBannerLabel` toggled via `setOutdated(boolean)` — fully wired
- PREV-04: `hasPdf` flag and placeholder hide/show — fully implemented

## Threat Flags

No new security surface introduced beyond plan's threat register. PDFBox addition does not expand the trust boundary — bytes still come exclusively from the internal Saxon/FOP pipeline before reaching `PDDocument.load()`. Entries T-07-01 and T-07-02 accepted per plan.

## Next Phase Readiness

- Phase 7 complete. PDF preview panel functional end-to-end on macOS.
- Phase 8 (Error & Log Panel) can proceed — log panel already partially functional (ListView visible from Phase 6 wiring).
- No blockers.

## Self-Check

- [x] `PreviewController.java` exists at `src/main/java/ch/ti/gagi/xsleditor/ui/PreviewController.java`
- [x] Commit `8e389ba` exists (Task 1)
- [x] Commit `a27ffe2` exists (Task 2)
- [x] Commit `bb90d3f` exists (Task 3)
- [x] Commit `6f7ae47` exists (PDFBox fix)
- [x] `build.gradle` contains `pdfbox:2.0.31`
- [x] All 4 PreviewControllerTest tests pass

## Self-Check: PASSED

---
*Phase: 07-pdf-preview-panel*
*Completed: 2026-04-20*
