package ch.ti.gagi.xlseditor.ui;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PreviewController state-transition logic.
 * Wave 0 stubs — Wave 1 enables and implements. PREV-03, PREV-04.
 */
class PreviewControllerTest {

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // Toolkit was already initialised by a previous test class — OK.
        }
    }

    /**
     * D-13: setOutdated(true) before any displayPdf() call must NOT make the banner visible.
     * Wave 0 stub — implement in Wave 1 (07-02-PLAN.md).
     */
    @Test
    @Disabled("Wave 0 stub — implement in Wave 1 (07-02-PLAN.md)")
    void setOutdated_doesNothing_whenHasPdfIsFalse() {
        // TODO Wave 1:
        // 1. Create PreviewController
        // 2. Call initialize() with Label/StackPane/WebView
        // 3. Call setOutdated(true)
        // 4. Assert outdatedBannerLabel.isVisible() == false
        fail("Not yet implemented");
    }

    /**
     * PREV-03 / D-04: after a successful render (hasPdf=true), setOutdated(true) must show banner.
     * Wave 0 stub — implement in Wave 1 (07-02-PLAN.md).
     */
    @Test
    @Disabled("Wave 0 stub — implement in Wave 1 (07-02-PLAN.md)")
    void setOutdated_showsBanner_whenHasPdfIsTrue() {
        // TODO Wave 1:
        // 1. Create PreviewController
        // 2. Call initialize()
        // 3. Call displayPdf(new byte[1]) to set hasPdf=true
        // 4. Call setOutdated(true)
        // 5. Assert outdatedBannerLabel.isVisible() == true
        fail("Not yet implemented");
    }

    /**
     * D-06: setOutdated(false) must hide banner regardless of prior state.
     * Wave 0 stub — implement in Wave 1 (07-02-PLAN.md).
     */
    @Test
    @Disabled("Wave 0 stub — implement in Wave 1 (07-02-PLAN.md)")
    void setOutdated_false_hidesBanner() {
        // TODO Wave 1:
        // 1. Create PreviewController
        // 2. Put it in shown-banner state (initialize + displayPdf + setOutdated(true))
        // 3. Call setOutdated(false)
        // 4. Assert outdatedBannerLabel.isVisible() == false
        fail("Not yet implemented");
    }

    /**
     * PREV-04 / D-11: displayPdf(byte[]) must hide previewPlaceholderLabel and show previewWebView.
     * Wave 0 stub — implement in Wave 1 (07-02-PLAN.md).
     * Note: WebView instantiation requires FX thread — wrap in Platform.runLater() + CountDownLatch.
     */
    @Test
    @Disabled("Wave 0 stub — implement in Wave 1 (07-02-PLAN.md)")
    void displayPdf_hidesPlaceholderAndShowsWebView() {
        // TODO Wave 1:
        // 1. Create PreviewController in initial state
        //    (WebView managed=false/visible=false, placeholder managed=true/visible=true)
        // 2. Wrap WebView instantiation in Platform.runLater() + CountDownLatch (same pattern as EditorTabTest)
        // 3. Call displayPdf(new byte[1])
        // 4. Assert previewWebView.isVisible() == true AND previewPlaceholderLabel.isVisible() == false
        fail("Not yet implemented");
    }
}
