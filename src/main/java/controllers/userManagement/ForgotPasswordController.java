package controllers.userManagement;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.userManagement.PasswordResetService;

import java.net.URL;
import java.util.ResourceBundle;

public class ForgotPasswordController implements Initializable {

    @FXML private TextField emailField;
    @FXML private Label emailError;
    @FXML private Label globalMessage;
    @FXML private Button sendBtn;

    private final PasswordResetService resetService = new PasswordResetService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        emailField.textProperty().addListener((obs, o, n) -> clearError(emailField, emailError));
    }

    @FXML
    private void onSendCode() {
        clearAllErrors();
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showFieldError(emailField, emailError, "L'email est requis.");
            return;
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            showFieldError(emailField, emailError, "Format d'email invalide.");
            return;
        }

        sendBtn.setDisable(true);
        sendBtn.setText("Envoi en cours...");

        // Envoyer OTP dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            boolean sent = resetService.sendOtp(email);
            javafx.application.Platform.runLater(() -> {
                sendBtn.setDisable(false);
                sendBtn.setText("📧  Envoyer le code");
                if (sent) {
                    navigateToOtp(email);
                } else {
                    showGlobalMessage("❌  Email introuvable ou erreur d'envoi.", true);
                }
            });
        }).start();
    }

    @FXML
    private void onBack() {
        navigateTo("/userManagement/Login.fxml", "ClutchX — Connexion");
    }

    private void navigateToOtp(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/userManagement/OtpVerification.fxml"));
            Parent root = loader.load();
            OtpVerificationController ctrl = loader.getController();
            ctrl.setEmail(email);
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/userManagement/clutchx-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("ClutchX — Vérification OTP");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navigateTo(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/userManagement/clutchx-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showGlobalMessage(String msg, boolean isError) {
        globalMessage.setText(msg);
        globalMessage.setStyle(isError
                ? "-fx-text-fill: #fc8181; -fx-background-color: rgba(229,62,62,0.15); " +
                "-fx-border-color: rgba(229,62,62,0.4); -fx-border-width:1; " +
                "-fx-border-radius:6; -fx-background-radius:6; -fx-padding: 8 12;"
                : "-fx-text-fill: #68d391; -fx-background-color: rgba(72,187,120,0.15); " +
                "-fx-border-color: rgba(72,187,120,0.4); -fx-border-width:1; " +
                "-fx-border-radius:6; -fx-background-radius:6; -fx-padding: 8 12;");
        globalMessage.setVisible(true);
        globalMessage.setManaged(true);
    }

    private void showFieldError(Control field, Label lbl, String msg) {
        lbl.setText("⚠  " + msg);
        lbl.setVisible(true); lbl.setManaged(true);
        if (field != null) field.getStyleClass().add("input-error");
    }

    private void clearError(Control field, Label lbl) {
        lbl.setVisible(false); lbl.setManaged(false); lbl.setText("");
        if (field != null) field.getStyleClass().remove("input-error");
    }

    private void clearAllErrors() {
        clearError(emailField, emailError);
        globalMessage.setVisible(false); globalMessage.setManaged(false);
    }
}