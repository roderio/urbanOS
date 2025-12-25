package smartcitymanagemnet;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.*;

public class RegisterController {

    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass, txtConfirmPass, txtAdminKey; // Added Admin Key
    @FXML private Label lblMessage;
    @FXML private BorderPane mainCard;

    private final String URL = "jdbc:mysql://localhost:3306/smart_city_erbil";
    private final String USER = "root";
    private final String PASS = "0000";

    private final String SECRET_ADMIN_KEY = "city-admin-2025";

    private double xOffset = 0, yOffset = 0, hue = 0;

    @FXML
    public void initialize() {
        startRGBAnimation();
    }

    @FXML
    private void handleRegister() {
        String username = txtUser.getText().trim();
        String password = txtPass.getText();
        String confirm = txtConfirmPass.getText();
        String adminKey = txtAdminKey.getText().trim();

        // Basic Validation
        if (username.isEmpty() || password.isEmpty()) {
            shakeNode(mainCard);
            lblMessage.setText("Please fill in all fields.");
            return;
        }

        // Password Match Check
        if (!password.equals(confirm)) {
            shakeNode(txtConfirmPass);
            lblMessage.setText("Passwords do not match!");
            return;
        }

        // DETERMINE ROLE
        String role = "USER"; // Default

        if (!adminKey.isEmpty()) {
            if (adminKey.equals(SECRET_ADMIN_KEY)) {
                role = "ADMIN"; // Promotion!
            } else {
                // Wrong Key -> Fail Registration
                shakeNode(txtAdminKey);
                lblMessage.setText("Invalid Admin Key!");
                return;
            }
        }

        //Database Insert
        String finalRole = role;

        new Thread(() -> {
            String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.setString(3, finalRole);
                stmt.executeUpdate();

                Platform.runLater(() -> {
                    lblMessage.setStyle("-fx-text-fill: #00ff00;");
                    lblMessage.setText("Account Created (" + finalRole + ")! Redirecting...");

                    PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                    delay.setOnFinished(e -> handleBackToLogin());
                    delay.play();
                });

            } catch (SQLException e) {
                Platform.runLater(() -> {
                    shakeNode(mainCard);
                    lblMessage.setStyle("-fx-text-fill: #ff4757;");
                    lblMessage.setText("Username already exists!");
                });
            }
        }).start();
    }

    @FXML
    private void handleBackToLogin() {
        switchScene("/LoginView.fxml");
    }

    private void switchScene(String fxmlPath) {
        Parent currentRoot = txtUser.getScene().getRoot();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentRoot);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {
            try {
                Parent newRoot = FXMLLoader.load(getClass().getResource(fxmlPath));
                Scene scene = new Scene(newRoot);
                scene.setFill(Color.TRANSPARENT);

                Stage stage = (Stage) txtUser.getScene().getWindow();
                stage.setScene(scene);

                newRoot.setOpacity(0.0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newRoot);
                fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
                fadeIn.play();
            } catch (IOException ex) { ex.printStackTrace(); }
        });
        fadeOut.play();
    }

    private void startRGBAnimation() {
        if (mainCard == null) return;
        Timeline rainbow = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            hue = (hue + 2) % 360;
            String c1 = String.format("hsb(%f, 100%%, 100%%)", hue);
            String c2 = String.format("hsb(%f, 100%%, 100%%)", (hue + 120) % 360);
            mainCard.setStyle("-fx-background-color: #2e2e42;" +
                    " -fx-background-radius: 20;" +
                    " -fx-border-color: linear-gradient(to bottom right, " + c1 + ", " + c2 + ");" +
                    " -fx-border-width: 1.2;" +
                    " -fx-border-radius: 20;");
        }));
        rainbow.setCycleCount(Animation.INDEFINITE); rainbow.play();
    }

    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0); tt.setByX(10); tt.setCycleCount(5); tt.setAutoReverse(true); tt.play();
    }

    @FXML private void handleClose() { System.exit(0); }
    @FXML private void handleMinimize(javafx.event.ActionEvent event) { ((Stage)((Node)event.getSource()).getScene().getWindow()).setIconified(true); }
    @FXML private void handleWindowDragStart(MouseEvent event) { xOffset = event.getSceneX(); yOffset = event.getSceneY(); }
    @FXML private void handleWindowDrag(MouseEvent event) {
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset); stage.setY(event.getScreenY() - yOffset);
    }
}