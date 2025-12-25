package smartcitymanagemnet;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class SmartCityManagemnet extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SmartCityManagemnet.class.getResource("/LoginView.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);

        scene.setFill(javafx.scene.paint.Color.web("#181825"));
        stage.setTitle("Smart City Login");

        // --- ANIMATION START ---

        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

        stage.setScene(scene);
        stage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), root); // 1 seconds fade
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        // --- ANIMATION END ---
    }

    public static void main(String[] args) {
        launch();
    }
}