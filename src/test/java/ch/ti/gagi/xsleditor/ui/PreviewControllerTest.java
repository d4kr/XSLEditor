package ch.ti.gagi.xlseditor.ui;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PreviewController state-transition logic.
 * PREV-03, PREV-04.
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
     */
    @Test
    void setOutdated_doesNothing_whenHasPdfIsFalse() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                PreviewController ctrl = new PreviewController();
                StackPane pane = new StackPane();
                WebView wv = new WebView();
                Label placeholder = new Label();
                Label banner = new Label();
                ctrl.initialize(pane, wv, placeholder, banner);

                ctrl.setOutdated(true); // hasPdf=false — D-13: must be no-op

                assertFalse(banner.isVisible());
                assertFalse(banner.isManaged());
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }

    /**
     * PREV-03 / D-04: after a successful render (hasPdf=true), setOutdated(true) must show banner.
     */
    @Test
    void setOutdated_showsBanner_whenHasPdfIsTrue() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                PreviewController ctrl = new PreviewController();
                StackPane pane = new StackPane();
                WebView wv = new WebView();
                Label placeholder = new Label();
                Label banner = new Label();
                ctrl.initialize(pane, wv, placeholder, banner);

                ctrl.displayPdf(new byte[]{1}); // sets hasPdf=true
                ctrl.setOutdated(true);

                assertTrue(banner.isVisible());
                assertTrue(banner.isManaged());
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }

    /**
     * D-06: setOutdated(false) must hide banner regardless of prior state.
     */
    @Test
    void setOutdated_false_hidesBanner() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                PreviewController ctrl = new PreviewController();
                StackPane pane = new StackPane();
                WebView wv = new WebView();
                Label placeholder = new Label();
                Label banner = new Label();
                ctrl.initialize(pane, wv, placeholder, banner);

                ctrl.displayPdf(new byte[]{1}); // sets hasPdf=true
                ctrl.setOutdated(true);         // show banner
                ctrl.setOutdated(false);        // hide banner

                assertFalse(banner.isVisible());
                assertFalse(banner.isManaged());
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }

    /**
     * PREV-04 / D-11: displayPdf(byte[]) must hide previewPlaceholderLabel and show previewWebView.
     */
    @Test
    void displayPdf_hidesPlaceholderAndShowsWebView() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                PreviewController ctrl = new PreviewController();
                StackPane pane = new StackPane();
                WebView wv = new WebView();
                Label placeholder = new Label();
                Label banner = new Label();
                ctrl.initialize(pane, wv, placeholder, banner);

                ctrl.displayPdf(new byte[]{1});

                assertTrue(wv.isVisible());
                assertFalse(placeholder.isVisible());
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }
}
