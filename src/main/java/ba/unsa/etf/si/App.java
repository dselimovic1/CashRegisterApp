package ba.unsa.etf.si;

<<<<<<< HEAD
import ba.unsa.etf.si.controllers.LoginFormController;
=======
import ba.unsa.etf.si.controllers.PrimaryController;
>>>>>>> Dependency injection in controller
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        scene = new Scene(loadFXML("fxml/loginForm.fxml"), 800, 600);
        stage.setScene(scene);
        stage.setTitle("Cash Register App");
        stage.getIcons().add(new Image("/ba/unsa/etf/si/img/appIcon.png"));
        stage.show();
    }


    private Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml));
        fxmlLoader.setControllerFactory(c -> new LoginFormController(primaryStage));
        return fxmlLoader.load();
    }

    public static Rectangle2D getScreenSize() {
        Screen screen = Screen.getPrimary();
        return screen.getBounds();
    }

    private static void setStage(Stage stage) {
        Rectangle2D rect = getScreenSize();
        stage.setWidth(rect.getWidth());
        stage.setHeight(rect.getHeight());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setResizable(false);
    }

    public static void main(String[] args) {
        launch();
    }

}