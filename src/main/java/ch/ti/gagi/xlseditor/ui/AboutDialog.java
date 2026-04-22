package ch.ti.gagi.xlseditor.ui;

import ch.ti.gagi.xlseditor.XLSEditorApp;
import javafx.application.HostServices;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Modal About dialog showing app version, runtime stack, author and license info.
 * Follows the SearchDialog programmatic Dialog&lt;Void&gt; pattern (D-03).
 */
public class AboutDialog extends Dialog<Void> {

    /**
     * Creates and configures the About dialog.
     *
     * @param ownerStage   the primary stage used for modal ownership (may be null in tests)
     * @param hostServices JavaFX HostServices for opening the license URL in the default browser
     */
    public AboutDialog(Stage ownerStage, HostServices hostServices) {
        // 1. Owner, modality, title, header (D-04)
        if (ownerStage != null) {
            initOwner(ownerStage);
            initModality(Modality.APPLICATION_MODAL);
        }
        setTitle("About " + XLSEditorApp.APP_NAME);
        setHeaderText(null);
        setResizable(false);

        // 2. Style the DialogPane
        getDialogPane().setPrefWidth(360);
        getDialogPane().setStyle("-fx-background-color: #2b2b2b;");
        getDialogPane().setPadding(new Insets(24, 16, 16, 16));

        // 3. App title label
        String version = loadVersion();
        Label titleLabel = new Label("XLSEditor  v" + version);   // two spaces between name and version
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setStyle("-fx-text-fill: #cccccc;");

        // 4. First separator
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #444444;");
        VBox.setMargin(sep1, new Insets(4, 0, 4, 0));

        // 5. Runtime Stack section header
        Label stackHeader = new Label("Runtime Stack");
        stackHeader.setFont(Font.font("System", FontWeight.BOLD, 12));
        stackHeader.setStyle("-fx-text-fill: #888888;");

        // 6. GridPane with 4 runtime version rows
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(4);

        String[][] rows = {
            {"Java",      System.getProperty("java.version", "unknown")},
            {"Saxon-HE",  saxonVersion()},
            {"FOP",       fopVersion()},
            {"JavaFX",    System.getProperty("javafx.version", "unknown")}
        };
        for (int i = 0; i < rows.length; i++) {
            Label lbl = new Label(rows[i][0]);
            lbl.setFont(Font.font("Monospaced", 13));
            lbl.setStyle("-fx-text-fill: #888888;");

            Label val = new Label(rows[i][1]);
            val.setFont(Font.font("System", 13));
            val.setStyle("-fx-text-fill: #cccccc;");

            grid.add(lbl, 0, i);
            grid.add(val, 1, i);
        }

        // 7. Second separator
        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color: #444444;");
        VBox.setMargin(sep2, new Insets(4, 0, 4, 0));

        // 8. Author line
        Label authorLabel = new Label("Author: Davide Krähenbühl & Claude Code");
        authorLabel.setFont(Font.font("System", 13));
        authorLabel.setStyle("-fx-text-fill: #cccccc;");

        // 9. License HBox with label and hyperlink (D-05)
        Label licenseLabel = new Label("License: Apache 2.0");
        licenseLabel.setFont(Font.font("System", 13));
        licenseLabel.setStyle("-fx-text-fill: #cccccc;");

        Hyperlink viewLink = new Hyperlink("View license");
        viewLink.setStyle("-fx-text-fill: #007acc;");
        viewLink.setOnAction(e ->
            hostServices.showDocument("https://www.apache.org/licenses/LICENSE-2.0"));

        HBox licenseBox = new HBox(8, licenseLabel, viewLink);
        licenseBox.setAlignment(Pos.CENTER_LEFT);

        // 10. Assemble root VBox
        VBox content = new VBox(8,
            titleLabel, sep1, stackHeader, grid,
            sep2, authorLabel, licenseBox);

        // 11. Wire DialogPane
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }

    // --- Private helpers ---

    /**
     * Reads the app version from version.properties at the classpath root (D-01).
     * Returns "unknown" on any failure — dialog always renders even if resource is missing.
     */
    private String loadVersion() {
        try (InputStream in = getClass().getResourceAsStream("/version.properties")) {
            if (in == null) return "unknown";
            Properties p = new Properties();
            p.load(in);
            return p.getProperty("version", "unknown");
        } catch (IOException e) {
            return "unknown";
        }
    }

    /**
     * Returns the Saxon-HE product version string (T-11-07 mitigation: wrapped in try/catch).
     */
    private String saxonVersion() {
        try {
            return net.sf.saxon.Version.getProductVersion();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Returns the Apache FOP version string (T-11-07 mitigation: wrapped in try/catch).
     * Falls back to "2.9" if the API is unavailable.
     */
    private String fopVersion() {
        try (InputStream in = org.apache.fop.Version.class
                .getResourceAsStream("/META-INF/maven/org.apache.xmlgraphics/fop/pom.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String v = p.getProperty("version");
                if (v != null && !v.isEmpty()) return v;
            }
        } catch (Exception ignored) {}
        try {
            String v = org.apache.fop.Version.getVersion();
            if (v != null && !v.equals("SVN")) return v;
        } catch (Exception ignored) {}
        return "2.9";
    }
}
