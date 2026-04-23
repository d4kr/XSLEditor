package ch.ti.gagi.xsleditor;

import ch.ti.gagi.xsleditor.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.image.Image;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

/**
 * JavaFX application entry point.
 *
 * Launch sequence:
 *   1. Load main.fxml
 *   2. Set minimum window dimensions
 *   3. Register close-request handler via MainController
 *   4. Show the stage
 */
public class XSLEditorApp extends Application {

    public static final String APP_NAME = "XSLEditor";

    private static javafx.application.HostServices hostServicesInstance;

    public static javafx.application.HostServices hostServices() {
        return hostServicesInstance;
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        URL fxml = getClass().getResource("/ch/ti/gagi/xsleditor/ui/main.fxml");
        if (fxml == null) {
            throw new IllegalStateException("Cannot find main.fxml on classpath");
        }

        FXMLLoader loader = new FXMLLoader(fxml);
        Scene scene = new Scene(loader.load(), 1280, 800);

        MainController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);

        primaryStage.setTitle(APP_NAME);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setScene(scene);
        hostServicesInstance = getHostServices();
        // Load and set the application window icon (D-03)
        try (InputStream iconStream =
                getClass().getResourceAsStream("/ch/ti/gagi/xsleditor/icon.png")) {
            if (iconStream != null) {
                Image icon = new Image(iconStream);
                if (!icon.isError()) {
                    primaryStage.getIcons().add(icon);
                } else {
                    Logger.getLogger(XSLEditorApp.class.getName())
                        .warning("App icon image reported an error during loading");
                }
            } else {
                Logger.getLogger(XSLEditorApp.class.getName())
                    .warning("App icon not found on classpath: /ch/ti/gagi/xsleditor/icon.png");
            }
        } catch (Exception e) {
            // Non-fatal: app launches without icon
        }
        primaryStage.show();
    }

    /** Standard main() for fat-JAR launch (not strictly needed by JavaFX but required by shadow JAR). */
    public static void main(String[] args) {
        launch(args);
    }
}
