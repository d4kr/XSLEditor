package ch.ti.gagi.xlseditor;

import ch.ti.gagi.xlseditor.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * JavaFX application entry point.
 *
 * Launch sequence:
 *   1. Load main.fxml
 *   2. Set minimum window dimensions
 *   3. Register close-request handler via MainController
 *   4. Show the stage
 */
public class XLSEditorApp extends Application {

    public static final String APP_NAME = "XLSEditor";

    @Override
    public void start(Stage primaryStage) throws IOException {
        URL fxml = getClass().getResource("/ch/ti/gagi/xlseditor/ui/main.fxml");
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
        primaryStage.show();
    }

    /** Standard main() for fat-JAR launch (not strictly needed by JavaFX but required by shadow JAR). */
    public static void main(String[] args) {
        launch(args);
    }
}
