package ch.ti.gagi.xsleditor.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
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
            this.tempFile = Files.createTempFile("xsleditor-preview", ".pdf");
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
        // macOS JavaFX WebView does not render PDFs inline via file:// URIs (WebKit limitation).
        // PDFBox renders each page as a PNG image embedded in an HTML page loaded into WebView.
        String html = renderPdfToHtml(tempFile);
        if (html != null) {
            previewWebView.getEngine().loadContent(html);
        } else {
            previewWebView.getEngine().load("");
            previewWebView.getEngine().load(tempFile.toUri().toString());
        }
        if (!hasPdf) {
            hasPdf = true;
            previewPlaceholderLabel.setManaged(false);
            previewPlaceholderLabel.setVisible(false);
            previewWebView.setManaged(true);
            previewWebView.setVisible(true);
        }
        setOutdated(false);
    }

    private String renderPdfToHtml(Path pdfFile) {
        try (PDDocument doc = PDDocument.load(pdfFile.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            StringBuilder html = new StringBuilder(
                "<html><body style='margin:0;padding:0;background:#2b2b2b;'>");
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, 150);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                html.append("<img src='data:image/png;base64,").append(b64)
                    .append("' style='width:100%;display:block;margin-bottom:4px;'/>");
            }
            html.append("</body></html>");
            return html.toString();
        } catch (IOException e) {
            System.err.println("[PreviewController] PDFBox render failed: " + e.getMessage());
            return null;
        }
    }

    public void setOutdated(boolean outdated) {
        if (outdated && !hasPdf) return; // D-13: no banner without prior PDF
        outdatedBannerLabel.setManaged(outdated);
        outdatedBannerLabel.setVisible(outdated);
    }
}
