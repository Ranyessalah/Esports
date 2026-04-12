package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class BanConfirmDialogController {

    @FXML private StackPane iconCircle;
    @FXML private Label     iconLabel;
    @FXML private Label     dialogTitle;
    @FXML private Label     targetEmail;
    @FXML private Label     consequenceBox;
    @FXML private Label     reversibleNote;
    @FXML private Button    confirmBtn;

    private boolean confirmed = false;

    /**
     * Call this right after loading the FXML.
     *
     * @param email     The target user's email
     * @param banning   true  = we are about to BLOCK the user
     *                  false = we are about to UNBLOCK the user
     */
    public void init(String email, boolean banning) {
        targetEmail.setText(email);

        if (banning) {
            iconLabel.setText("🔒");
            iconCircle.setStyle(
                    "-fx-background-color: rgba(245,158,11,0.15);" +
                    "-fx-background-radius: 32;");
            dialogTitle.setText("Bloquer l'utilisateur ?");
            consequenceBox.setText(
                    "Cet utilisateur ne pourra plus se connecter à la plateforme ClutchX " +
                    "tant que son compte sera bloqué.");
            reversibleNote.setText("✓  Cette action est réversible à tout moment depuis le tableau de bord.");
            confirmBtn.setText("Bloquer");
            confirmBtn.setStyle(
                    "-fx-background-color: #f59e0b;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10 24;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-width: 0;" +
                    "-fx-cursor: hand;");
        } else {
            iconLabel.setText("🔓");
            iconCircle.setStyle(
                    "-fx-background-color: rgba(34,197,94,0.15);" +
                    "-fx-background-radius: 32;");
            dialogTitle.setText("Débloquer l'utilisateur ?");
            consequenceBox.setText(
                    "Cet utilisateur retrouvera immédiatement l'accès complet à la plateforme ClutchX.");
            reversibleNote.setText("✓  Vous pourrez bloquer à nouveau cet utilisateur à tout moment.");
            confirmBtn.setText("Débloquer");
            confirmBtn.setStyle(
                    "-fx-background-color: #22c55e;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 10 24;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-width: 0;" +
                    "-fx-cursor: hand;");
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @FXML
    private void onConfirm() {
        confirmed = true;
        close();
    }

    @FXML
    private void onCancel() {
        confirmed = false;
        close();
    }

    private void close() {
        ((Stage) confirmBtn.getScene().getWindow()).close();
    }
}
