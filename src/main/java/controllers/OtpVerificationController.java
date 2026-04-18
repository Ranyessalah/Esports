package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.PasswordResetService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class OtpVerificationController implements Initializable {

    @FXML private Label subtitleLabel;
    @FXML private TextField otp1, otp2, otp3, otp4, otp5, otp6;
    @FXML private Label otpError, globalMessage, timerLabel;
    @FXML private Button verifyBtn, resendBtn;

    private String email;
    private javafx.animation.Timeline countdownTimer;
    private int secondsLeft = 300; // 5 minutes

    private final PasswordResetService resetService = new PasswordResetService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupOtpFields();
        startCountdown();
    }

    public void setEmail(String email) {
        this.email = email;
        subtitleLabel.setText("Un code à 6 chiffres a été envoyé à " + email);
    }

    private void setupOtpFields() {
        List<TextField> fields = List.of(otp1, otp2, otp3, otp4, otp5, otp6);

        for (int i = 0; i < fields.size(); i++) {
            final int idx = i;
            TextField field = fields.get(i);

            // Centrer le texte
            field.setStyle(field.getStyle() + "; -fx-alignment: center;");

            field.textProperty().addListener((obs, oldVal, newVal) -> {
                // Accepter un seul chiffre
                if (newVal.length() > 1) {
                    field.setText(newVal.substring(newVal.length() - 1));
                    return;
                }
                // Autoriser seulement les chiffres
                if (!newVal.matches("[0-9]?")) {
                    field.setText(oldVal);
                    return;
                }
                // Auto-focus suivant
                if (newVal.length() == 1 && idx < fields.size() - 1) {
                    fields.get(idx + 1).requestFocus();
                }
                clearError(otpError);
            });

            // Backspace → focus précédent
            field.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.BACK_SPACE
                        && field.getText().isEmpty() && idx > 0) {
                    fields.get(idx - 1).requestFocus();
                }
            });
        }
    }

    private void startCountdown() {
        secondsLeft = 300;
        resendBtn.setDisable(true);

        if (countdownTimer != null) countdownTimer.stop();

        countdownTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                    secondsLeft--;
                    int min = secondsLeft / 60;
                    int sec = secondsLeft % 60;
                    timerLabel.setText(String.format("%02d:%02d", min, sec));

                    if (secondsLeft <= 60)
                        timerLabel.setStyle("-fx-text-fill: #fc8181; -fx-font-weight: bold; -fx-font-size: 13px;");

                    if (secondsLeft <= 0) {
                        countdownTimer.stop();
                        timerLabel.setText("Expiré");
                        verifyBtn.setDisable(true);
                        resendBtn.setDisable(false);
                    }
                })
        );
        countdownTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        countdownTimer.play();
    }

    @FXML
    private void onVerify() {
        String code = otp1.getText() + otp2.getText() + otp3.getText()
                + otp4.getText() + otp5.getText() + otp6.getText();

        if (code.length() < 6) {
            showError(otpError, "Veuillez entrer les 6 chiffres du code.");
            return;
        }

        if (resetService.verifyOtp(email, code)) {
            countdownTimer.stop();
            navigateToReset(email, code);
        } else {
            showError(otpError, "Code incorrect ou expiré.");
            shakeFields();
        }
    }

    @FXML
    private void onResend() {
        resendBtn.setDisable(true);
        clearOtpFields();
        globalMessage.setVisible(false); globalMessage.setManaged(false);

        new Thread(() -> {
            boolean sent = resetService.sendOtp(email);
            javafx.application.Platform.runLater(() -> {
                if (sent) {
                    startCountdown();
                    verifyBtn.setDisable(false);
                    showGlobalMessage("✅  Nouveau code envoyé !", false);
                } else {
                    resendBtn.setDisable(false);
                    showGlobalMessage("❌  Erreur lors de l'envoi.", true);
                }
            });
        }).start();
    }

    @FXML
    private void onBack() {
        if (countdownTimer != null) countdownTimer.stop();
        navigateTo("/ForgotPassword.fxml", "ClutchX — Réinitialisation");
    }

    private void clearOtpFields() {
        for (TextField f : List.of(otp1, otp2, otp3, otp4, otp5, otp6)) f.clear();
        otp1.requestFocus();
    }

    private void shakeFields() {
        for (TextField f : List.of(otp1, otp2, otp3, otp4, otp5, otp6)) {
            javafx.animation.TranslateTransition tt =
                    new javafx.animation.TranslateTransition(javafx.util.Duration.millis(60), f);
            tt.setFromX(0); tt.setByX(6); tt.setCycleCount(6); tt.setAutoReverse(true);
            tt.play();
        }
    }

    private void navigateToReset(String email, String otp) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResetPassword.fxml"));
            Parent root = loader.load();
            ResetPasswordController ctrl = loader.getController();
            ctrl.setContext(email, otp);
            Stage stage = (Stage) otp1.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/clutchx-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("ClutchX — Nouveau mot de passe");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navigateTo(String fxml, String title) {
        try {
            if (countdownTimer != null) countdownTimer.stop();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) otp1.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/clutchx-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(Label lbl, String msg) {
        lbl.setText("⚠  " + msg);
        lbl.setVisible(true); lbl.setManaged(true);
    }

    private void clearError(Label lbl) {
        lbl.setVisible(false); lbl.setManaged(false); lbl.setText("");
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
        globalMessage.setVisible(true); globalMessage.setManaged(true);
    }
}