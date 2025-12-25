package smartcitymanagemnet;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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

public class LoginController {

    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;
    @FXML private Label lblMessage;
    @FXML private BorderPane mainCard;
    @FXML private Button btnLogin;

    // Database Config
    private final String URL = "jdbc:mysql://localhost:3306/smart_city_erbil";
    private final String USER = "root";
    private final String PASS = "0000";

    // Drag Offsets
    private double xOffset = 0;
    private double yOffset = 0;

    private double hue = 0;

    @FXML
    public void initialize() {
        startRGBAnimation();
        Platform.runLater(() -> txtUser.requestFocus());
    }

    @FXML
    private void handleLogin() {
        String username = txtUser.getText();
        String password = txtPass.getText();

        if (username.isEmpty() || password.isEmpty()) {
            shakeNode(mainCard); // Visual Feedback
            lblMessage.setText("Please enter credentials");
            return;
        }

        lblMessage.setText("Verifying...");
        lblMessage.setStyle("-fx-text-fill: #a6accd;");

        // Run DB Check in Background
        new Thread(() -> {
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement stmt = conn.prepareStatement("SELECT role FROM users WHERE username=? AND password=?")) {

                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String role = rs.getString("role");
                    Platform.runLater(() -> openDashboard(role));
                } else {
                    Platform.runLater(() -> {
                        shakeNode(mainCard);
                        lblMessage.setStyle("-fx-text-fill: #ff4757;");
                        lblMessage.setText("Invalid Username or Password");
                        txtPass.clear();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblMessage.setText("Database Error: Is MySQL Running?");
                    shakeNode(mainCard);
                });
            }
        }).start();
    }

    @FXML
    private void handleSignUp() {
        // Switch to the full Register View
        Parent loginRoot = txtUser.getScene().getRoot();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), loginRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/RegisterView.fxml"));
                Parent registerRoot = loader.load();

                Stage stage = (Stage) txtUser.getScene().getWindow();
                Scene scene = new Scene(registerRoot);
                scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                stage.setScene(scene);

                // Animate In
                registerRoot.setOpacity(0.0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), registerRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        fadeOut.play();
    }


    private void openDashboard(String role) {
        Parent loginRoot = txtUser.getScene().getRoot();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), loginRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {
            try {

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/SmartCityView.fxml"));
                Parent dashboardRoot = loader.load();

                SmartCityController mainController = loader.getController();
                mainController.setUserRole(role);

                Stage stage = (Stage) txtUser.getScene().getWindow();

                stage.hide();

                Scene scene = new Scene(dashboardRoot);
                scene.setFill(javafx.scene.paint.Color.web("#1e1e2e")); // Match Theme Background
                stage.setScene(scene);

                stage.setWidth(1600);
                stage.setHeight(900);
                stage.centerOnScreen();

                dashboardRoot.setOpacity(0.0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(600), dashboardRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);

                stage.show();
                fadeIn.play();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        fadeOut.play();
    }

    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }

    private void startRGBAnimation() {
        if (mainCard == null) return;
        Timeline rainbow = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            hue = (hue + 2) % 360;
            String c1 = toHex(Color.hsb(hue, 1.0, 1.0));
            String c2 = toHex(Color.hsb((hue + 120) % 360, 1.0, 1.0));
            String c3 = toHex(Color.hsb((hue + 240) % 360, 1.0, 1.0));

            mainCard.setStyle(
                    "-fx-background-color: #2e2e42;" +
                            "-fx-background-radius: 20;" +
                            "-fx-border-color: linear-gradient(to bottom right, " + c1 + ", " + c2 + ", " + c3 + ");" +
                            "-fx-border-width: 1.2;" +
                            "-fx-border-radius: 20;"
            );
        }));
        rainbow.setCycleCount(Animation.INDEFINITE);
        rainbow.play();
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X", (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    // --- WINDOW CONTROLS ---
    @FXML private void handleClose(javafx.event.ActionEvent event) { System.exit(0); }
    @FXML private void handleMinimize(javafx.event.ActionEvent event) { ((Stage)((Node)event.getSource()).getScene().getWindow()).setIconified(true); }
    @FXML private void handleWindowDragStart(MouseEvent event) { xOffset = event.getSceneX(); yOffset = event.getSceneY(); }
    @FXML private void handleWindowDrag(MouseEvent event) {
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }
}