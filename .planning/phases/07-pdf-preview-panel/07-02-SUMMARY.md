---
phase: 07-pdf-preview-panel
plan: 02
subsystem: ui
tags: [javafx, webview, preview, pdf, sub-controller, tdd]

# Dependency graph
requires:
  - phase: 06-render-pipeline-integration
    provides: pdfCallback and outdatedCallback seams as no-ops in RenderController
  - plan: 07-01
    provides: Wave 0 @Disabled test stubs for PreviewController
provides:
  - PreviewController.java — PDF display logic, outdated banner toggle, placeholder state machine
  - MainController wired with previewController field and method references replacing no-op lambdas
  - main.fxml outdatedBannerLabel node + previewWebView initial hidden state
  - main.css .preview-outdated-banner rule
  - All 4 PreviewControllerTest tests enabled and passing
affects: [07-verify-work]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Sub-controller initialize() with Objects.requireNonNull guards (PreviewController)"
    - "Files.createTempFile + Files.write for PDF byte[] → temp file I/O"
    - "load('') then load(uri) blank-reload mitigation for WebView same-URI cache (RESEARCH.md Pitfall 1)"
    - "setManaged(false) + setVisible(false) node toggle pattern (both always together)"
    - "Platform.runLater + CountDownLatch for FX-thread unit tests requiring WebView"

key-files:
  created:
    - src/main/java/ch/ti/gagi/xlseditor/ui/PreviewController.java
  modified:
    - src/main/java/ch/ti/gagi/xlseditor/ui/MainController.java
    - src/main/resources/ch/ti/gagi/xlseditor/ui/main.fxml
    - src/main/resources/ch/ti/gagi/xlseditor/ui/main.css
    - src/test/java/ch/ti/gagi/xlseditor/ui/PreviewControllerTest.java

key-decisions:
  - "Single temp file created once in initialize() (D-01) — not per render"
  - "load('') blank-reload before load(uri) on every displayPdf() call — macOS WebKit same-URI cache mitigation"
  - "D-13 guard: setOutdated(true) is a no-op when hasPdf=false — banner never shown before first render"
  - "previewController.initialize() called BEFORE renderController.initialize() so method references are valid"
  - "outdatedBannerLabel added as LAST child of previewPane StackPane (z-order: banner on top of WebView)"

# Metrics
duration: 4min
completed: 2026-04-20
---

# Phase 7 Plan 02: PDF Preview Panel Wave 1 Summary

**PreviewController sub-controller wired into MainController, delivering working PDF display (PREV-01/02), outdated indicator (PREV-03), and no-PDF-before-first-render behavior (PREV-04) — all 4 unit tests passing**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-04-20T13:13:03Z
- **Completed:** 2026-04-20T13:17:04Z
- **Tasks:** 3/4 complete (Task 4 awaiting human verification)
- **Files modified:** 5

## Accomplishments

- Created `PreviewController.java` (77 lines) — sub-controller with `displayPdf(byte[])` and `setOutdated(boolean)`, single temp file, D-13 guard, blank-reload WebKit mitigation
- Wired `MainController`: added `@FXML Label outdatedBannerLabel`, `previewController` field, `previewController.initialize()` call, replaced Phase 6 no-op lambdas with `previewController::displayPdf` and `previewController::setOutdated`
- Updated `main.fxml`: WebView starts hidden (`managed="false" visible="false"`), `outdatedBannerLabel` added as last StackPane child (D-04, correct z-order)
- Updated `main.css`: `.preview-outdated-banner` rule with orange `#f97316` background, Phase 7 comment
- Enabled all four `@Disabled` Wave 0 test stubs in `PreviewControllerTest.java` with full implementations using `Platform.runLater` + `CountDownLatch` — all 4 pass, full suite green

## Task Commits

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Create PreviewController.java | 8e389ba | src/main/java/ch/ti/gagi/xlseditor/ui/PreviewController.java |
| 2 | Wire MainController + FXML + CSS | a27ffe2 | MainController.java, main.fxml, main.css |
| 3 | Enable PreviewControllerTest stubs | bb90d3f | PreviewControllerTest.java |
| 4 | Human verify PREV-01..04 | PENDING | — |

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None. All display states are fully wired:
- PREV-01: SplitPane layout already in place from Phase 6; WebView now receives PDF content
- PREV-02: WebView native PDF plugin handles scroll/zoom — no code needed
- PREV-03: `outdatedBannerLabel` toggled via `setOutdated(boolean)` — fully wired
- PREV-04: `hasPdf` flag and placeholder hide/show — fully implemented

## Threat Flags

No new security surface introduced. Threat register entries T-07-01 and T-07-02 accepted per plan:
- T-07-01 (temp file disclosure): local developer tool, OS cleans on reboot
- T-07-02 (malformed PDF bytes): internal pipeline only, platform PDF plugin rejects malformed input

## Self-Check

- [x] `PreviewController.java` exists at `src/main/java/ch/ti/gagi/xlseditor/ui/PreviewController.java`
- [x] Commit `8e389ba` exists (Task 1)
- [x] Commit `a27ffe2` exists (Task 2)
- [x] Commit `bb90d3f` exists (Task 3)
- [x] `./gradlew test` exits 0 with 4 PreviewControllerTest tests passing

## Self-Check: PASSED

---
*Phase: 07-pdf-preview-panel*
*Status: Awaiting human verification (Task 4 checkpoint)*
*Completed tasks: 3/4*
