package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import services.PasswordResetService;

import java.net.URL;
import java.util.ResourceBundle;

public class ResetPasswordController implements Initializable {

    @FXML private PasswordField passwordField, confirmField;
    @FXML private Label passwordError, confirmError, globalMessage;
    @FXML private Label strengthLabel, rule1, rule2, rule3, rule4;
    @FXML private Region seg1, seg2, seg3, seg4;

    private String email, otpCode;
    private final PasswordResetService resetService = new PasswordResetService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        passwordField.textProperty().addListener((obs, o, n) -> {
            clearError(passwordField, passwordError);
            updateStrength(n);
            updateRules(n);
            // Re-vérifier la confirmation si déjà saisie
            if (!confirmField.getText().isEmpty()) checkConfirm();
        });
        confirmField.textProperty().addListener((obs, o, n) -> {
            clearError(confirmField, confirmError);
            checkConfirm();
        });
        confirmField.setOnAction(e -> onReset());
    }

    public void setContext(String email, String otpCode) {
        this.email   = email;
        this.otpCode = otpCode;
    }

    @FXML
    private void onReset() {
        clearAllErrors();
        String pwd     = passwordField.getText();
        String confirm = confirmField.getText();
        boolean valid  = true;

        if (pwd.isEmpty()) {
            showFieldError(passwordField, passwordError, "Le mot de passe est requis.");
            valid = false;
        } else if (pwd.length() < 8) {
            showFieldError(passwordField, passwordError, "Minimum 8 caractères.");
            valid = false;
        } else if (!pwd.matches(".*[A-Z].*")) {
            showFieldError(passwordField, passwordError, "Doit contenir une majuscule.");
            valid = false;
        } else if (!pwd.matches(".*[0-9].*")) {
            showFieldError(passwordField, passwordError, "Doit contenir un chiffre.");
            valid = false;
        } else if (!pwd.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            showFieldError(passwordField, passwordError, "Doit contenir un caractère spécial.");
            valid = false;
        }

        if (confirm.isEmpty()) {
            showFieldError(confirmField, confirmError, "La confirmation est requise.");
            valid = false;
        } else if (!pwd.equals(confirm)) {
            showFieldError(confirmField, confirmError, "Les mots de passe ne correspondent pas.");
            valid = false;
        }

        if (!valid) return;

        boolean success = resetService.resetPassword(email, otpCode, pwd);
        if (success) {
            showGlobalMessage("✅  Mot de passe réinitialisé avec succès !", false);
            // Rediriger vers login après 2 secondes
            javafx.animation.PauseTransition pause =
                    new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            pause.setOnFinished(e -> navigateTo("/Login.fxml", "ClutchX — Connexion"));
            pause.play();
        } else {
            showGlobalMessage("❌  Erreur lors de la réinitialisation. Recommencez.", true);
        }
    }

    private void updateStrength(String pwd) {
        int score = 0;
        if (pwd.length() >= 8)                                          score++;
        if (pwd.matches(".*[A-Z].*"))                                   score++;
        if (pwd.matches(".*[0-9].*"))                                   score++;
        if (pwd.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score++;

        String[] styles = { "", "strength-weak", "strength-fair", "strength-good", "strength-strong" };
        String[] labels = { "", "Faible", "Moyen", "Bon", "Fort" };

        Region[] segs = { seg1, seg2, seg3, seg4 };
        for (int i = 0; i < segs.length; i++) {
            segs[i].getStyleClass().removeAll("strength-weak","strength-fair","strength-good","strength-strong");
            if (i < score) segs[i].getStyleClass().add(styles[score]);
        }

        if (pwd.isEmpty()) {
            strengthLabel.setVisible(false); strengthLabel.setManaged(false);
        } else {
            strengthLabel.setVisible(true); strengthLabel.setManaged(true);
            strengthLabel.getStyleClass().removeAll("strength-weak","strength-fair","strength-good","strength-strong");
            strengthLabel.getStyleClass().add(styles[score]);
            strengthLabel.setText(labels[score]);
        }
    }

    private void updateRules(String pwd) {
        setRule(rule1, pwd.length() >= 8,           "Au moins 8 caractères");
        setRule(rule2, pwd.matches(".*[A-Z].*"),     "Une majuscule");
        setRule(rule3, pwd.matches(".*[0-9].*"),     "Un chiffre");
        setRule(rule4, pwd.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"),
                "Un caractère spécial");
    }

    private void setRule(Label lbl, boolean ok, String text) {
        lbl.setText((ok ? "✓  " : "✗  ") + text);
        lbl.setStyle(ok
                ? "-fx-text-fill: #68d391; -fx-font-size: 11px;"
                : "-fx-text-fill: #606090; -fx-font-size: 11px;");
    }

    private void checkConfirm() {
        String pwd     = passwordField.getText();
        String confirm = confirmField.getText();
        if (!confirm.isEmpty() && !pwd.equals(confirm)) {
            showFieldError(confirmField, confirmError, "Les mots de passe ne correspondent pas.");
        } else {
            clearError(confirmField, confirmError);
        }
    }

    private void navigateTo(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) passwordField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/clutchx-theme.css").toExternalForm());
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
        globalMessage.setVisible(true); globalMessage.setManaged(true);
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
        clearError(passwordField, passwordError);
        clearError(confirmField, confirmError);
        globalMessage.setVisible(false); globalMessage.setManaged(false);
    }
}