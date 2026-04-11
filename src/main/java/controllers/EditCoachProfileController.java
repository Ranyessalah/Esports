package controllers;

import entities.Coach;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.CoachService;
import services.UserService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class EditCoachProfileController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> paysCombo;
    @FXML private ComboBox<String> specialiteCombo;
    @FXML private RadioButton ouiRadio;
    @FXML private RadioButton nonRadio;
    @FXML private Label fileNameLabel;
    @FXML private Label errorLabel;

    private User currentUser;
    private Coach currentCoach;
    private File selectedFile;
    private Runnable onSaved;

    private final UserService userService = new UserService();
    private final CoachService coachService = new CoachService();

    private static final List<String> PAYS_LIST = List.of(
            "Tunisie", "France", "Algérie", "Maroc", "Espagne", "Allemagne",
            "Italie", "Belgique", "Suisse", "Canada", "États-Unis", "Autre"
    );

    private static final List<String> SPECIALITES = List.of(
            "Attaque", "Défense", "Milieu de terrain", "Gardien de but",
            "Préparation physique", "Tactique", "Mental", "Autre"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        paysCombo.getItems().addAll(PAYS_LIST);
        specialiteCombo.getItems().addAll(SPECIALITES);
    }

    public void setData(User user, Coach coach) {
        this.currentUser = user;
        this.currentCoach = coach;

        if (user != null) {
            emailField.setText(user.getEmail());
        }

        if (coach != null) {
            if (coach.getPays() != null) paysCombo.setValue(coach.getPays());
            if (coach.getSpecialite() != null) specialiteCombo.setValue(coach.getSpecialite());
            if (coach.isDisponibilite()) ouiRadio.setSelected(true);
            else nonRadio.setSelected(true);
        } else {
            ouiRadio.setSelected(true);
        }
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    @FXML
    private void onChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        Stage stage = (Stage) emailField.getScene().getWindow();
        selectedFile = chooser.showOpenDialog(stage);
        if (selectedFile != null) {
            fileNameLabel.setText(selectedFile.getName());
        }
    }

    @FXML
    private void onSave() {
        hideError();

        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String pays = paysCombo.getValue();
        String specialite = specialiteCombo.getValue();
        boolean disponibilite = ouiRadio.isSelected();

        // Validation
        if (email.isEmpty()) { showError("L'email est requis."); return; }
        if (!email.contains("@")) { showError("Format d'email invalide."); return; }
        if (!password.isEmpty()) {
            if (password.length() < 6) { showError("Mot de passe trop court (min. 6 caractères)."); return; }
            if (!password.equals(confirmPassword)) { showError("Les mots de passe ne correspondent pas."); return; }
        }

        try {
            // Update User
            currentUser.setEmail(email);
            if (selectedFile != null) {
                currentUser.setProfileImage(selectedFile.getAbsolutePath());
            }
            userService.updateOne(currentUser);

            // Update password if changed
            if (!password.isEmpty()) {
                userService.updatePassword(currentUser, password);
            }

            // Update Coach
            if (currentCoach == null) currentCoach = new Coach();
            currentCoach.setId(currentUser.getId());
            currentCoach.setPays(pays);
            currentCoach.setSpecialite(specialite);
            currentCoach.setDisponibilite(disponibilite);
            coachService.modifier(currentCoach);

            // Callback to refresh profile view
            if (onSaved != null) onSaved.run();

            closeDialog();

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            showError("Erreur base de données : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}