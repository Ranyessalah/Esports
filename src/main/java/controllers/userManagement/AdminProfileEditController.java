package controllers.userManagement;

import entities.userManagement.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.userManagement.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AdminProfileEditController implements Initializable {

    @FXML private Label avatarLabel;
    @FXML private Label subtitleLabel;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label messageLabel;
    @FXML private Button saveButton;

    // Error labels
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label confirmError;

    private User user;
    private Stage dialog;
    private Runnable onSavedCallback;
    private final UserService userService = new UserService();

    public void init(User user, Stage dialog, Runnable onSavedCallback) {
        this.user = user;
        this.dialog = dialog;
        this.onSavedCallback = onSavedCallback;

        String initials = user.getEmail()
                .substring(0, Math.min(2, user.getEmail().length())).toUpperCase();
        avatarLabel.setText(initials);
        subtitleLabel.setText(user.getEmail());
        emailField.setText(user.getEmail());
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupLiveValidation();
    }

    private void setupLiveValidation() {
        emailField.textProperty().addListener((obs, o, n) -> clearError(emailField, emailError));
        emailField.focusedProperty().addListener((obs, was, is) -> { if (!is) validateEmail(); });

        passwordField.textProperty().addListener((obs, o, n) -> clearError(passwordField, passwordError));
        passwordField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && !passwordField.getText().isEmpty()) validatePassword();
        });

        confirmField.textProperty().addListener((obs, o, n) -> clearError(confirmField, confirmError));
        confirmField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && !confirmField.getText().isEmpty()) validateConfirm();
        });
    }

    // ── Validators ──────────────────────────────────────────────

    private boolean validateEmail() {
        String v = emailField.getText().trim();
        if (v.isEmpty()) return setError(emailField, emailError, "L'email est requis.");
        if (!v.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$"))
            return setError(emailField, emailError, "Format d'email invalide.");
        clearError(emailField, emailError);
        return true;
    }

    private boolean validatePassword() {
        String v = passwordField.getText();
        if (v.isEmpty()) { clearError(passwordField, passwordError); return true; }
        if (v.length() < 6) return setError(passwordField, passwordError, "Minimum 6 caractères requis.");
        clearError(passwordField, passwordError);
        return true;
    }

    private boolean validateConfirm() {
        String v = confirmField.getText();
        if (passwordField.getText().isEmpty() && v.isEmpty()) {
            clearError(confirmField, confirmError); return true;
        }
        if (!v.equals(passwordField.getText()))
            return setError(confirmField, confirmError, "Les mots de passe ne correspondent pas.");
        clearError(confirmField, confirmError);
        return true;
    }

    // ── Helpers ─────────────────────────────────────────────────

    private boolean setError(Control field, Label label, String message) {
        label.setText("⚠  " + message);
        label.setVisible(true); label.setManaged(true);
        if (field != null) { field.getStyleClass().remove("input-error"); field.getStyleClass().add("input-error"); }
        return false;
    }

    private void clearError(Control field, Label label) {
        label.setVisible(false); label.setManaged(false); label.setText("");
        if (field != null) field.getStyleClass().remove("input-error");
    }

    // ── Actions ─────────────────────────────────────────────────

    @FXML
    private void onCancel() { dialog.close(); }

    @FXML
    private void onSave() {
        // Hide previous message
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

        boolean valid = true;
        valid &= validateEmail();
        valid &= validatePassword();
        valid &= validateConfirm();
        if (!valid) return;

        user.setEmail(emailField.getText().trim());
        try {
            userService.updateOne(user);
            String pw = passwordField.getText();
            if (!pw.isEmpty()) userService.updatePassword(user, pw);

            showMessage("✅  Profil mis à jour avec succès !", false);
            saveButton.setDisable(true);

            new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    dialog.close();
                    if (onSavedCallback != null) onSavedCallback.run();
                });
            }).start();

        } catch (SQLException ex) {
            showMessage("❌  Erreur : " + ex.getMessage(), true);
        }
    }

    private void showMessage(String text, boolean isError) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().removeAll("msg-success", "msg-error");
        messageLabel.getStyleClass().add(isError ? "msg-error" : "msg-success");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }
}