package controllers;

import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AdminProfileEditController implements Initializable {

    @FXML private Label         avatarLabel;
    @FXML private Label         subtitleLabel;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label         messageLabel;
    @FXML private Button        saveButton;

    private User user;
    private Stage dialog;
    private Runnable onSavedCallback;

    private final UserService userService = new UserService();

    public void init(User user, Stage dialog, Runnable onSavedCallback) {
        this.user            = user;
        this.dialog          = dialog;
        this.onSavedCallback = onSavedCallback;

        String initials = user.getEmail()
                .substring(0, Math.min(2, user.getEmail().length())).toUpperCase();
        avatarLabel.setText(initials);
        subtitleLabel.setText(user.getEmail());
        emailField.setText(user.getEmail());
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    // ── Cancel: close ONLY the dialog, profile screen stays intact ──
    @FXML
    private void onCancel() {
        dialog.close();
    }

    @FXML
    private void onSave() {
        String email   = emailField.getText().trim();
        String pw      = passwordField.getText();
        String confirm = confirmField.getText();

        if (email.isEmpty() || !email.contains("@")) {
            showMessage("Adresse email invalide.", true);
            return;
        }
        if (!pw.isEmpty()) {
            if (pw.length() < 6) {
                showMessage("Le mot de passe doit contenir au moins 6 caractères.", true);
                return;
            }
            if (!pw.equals(confirm)) {
                showMessage("Les mots de passe ne correspondent pas.", true);
                return;
            }
        }

        user.setEmail(email);
        try {
            userService.updateOne(user);
            if (!pw.isEmpty()) userService.updatePassword(user, pw);

            showMessage("Profil mis à jour avec succès !", false);
            saveButton.setDisable(true);

            // Close dialog after short delay, then refresh
            new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    dialog.close();
                    if (onSavedCallback != null) onSavedCallback.run();
                });
            }).start();

        } catch (SQLException ex) {
            showMessage("Erreur : " + ex.getMessage(), true);
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