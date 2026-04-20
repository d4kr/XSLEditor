package ch.ti.gagi.xlseditor.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

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
        this.previewPane             = Objects.requireNonNull(previewPane,             "previewPane");
        this.previewWebView          = Objects.requireNonNull(previewWebView,          "previewWebView");
        this.previewPlaceholderLabel = Objects.requireNonNull(previewPlaceholderLabel, "previewPlaceholderLabel");
        this.outdatedBannerLabel     = Objects.requireNonNull(outdatedBannerLabel,     "outdatedBannerLabel");
        try {
            this.tempFile = Files.createTempFile("xlseditor-preview", ".pdf");
        } catch (IOException e) {
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
        // load("") forces WebKit to treat the next load(uri) as a fresh fetch,
        // avoiding the same-URI cache no-op on macOS WebKit (RESEARCH.md Pitfall 1).
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
