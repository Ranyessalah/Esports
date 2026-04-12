package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DeleteConfirmDialogController {

    @FXML private Label emailLabel;

    private boolean confirmed = false;

    public void setEmail(String email) {
        emailLabel.setText(email);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @FXML
    private void onConfirm() {
        confirmed = true;
        closeDialog();
    }

    @FXML
    private void onCancel() {
        confirmed = false;
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) emailLabel.getScene().getWindow();
        stage.close();
    }
}
