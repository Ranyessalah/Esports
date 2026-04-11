package controllers;

import entities.Player;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.PlayerService;
import services.UserService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class EditPlayerProfileController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> paysCombo;
    @FXML private ComboBox<String> niveauCombo;
    @FXML private RadioButton actifRadio;
    @FXML private RadioButton inactifRadio;
    @FXML private Label fileNameLabel;
    @FXML private Label errorLabel;

    private User currentUser;
    private Player currentPlayer;
    private File selectedFile;
    private Runnable onSaved;

    private final UserService userService = new UserService();
    private final PlayerService playerService = new PlayerService();

    private static final List<String> PAYS_LIST = List.of(
            "Tunisie", "France", "Algérie", "Maroc", "Espagne", "Allemagne",
            "Italie", "Belgique", "Suisse", "Canada", "États-Unis", "Autre"
    );

    private static final List<String> NIVEAUX = List.of(
            "Débutant", "Intermédiaire", "Avancé", "Semi-professionnel", "Professionnel"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        paysCombo.getItems().addAll(PAYS_LIST);
        niveauCombo.getItems().addAll(NIVEAUX);
    }

    public void setData(User user, Player player) {
        this.currentUser = user;
        this.currentPlayer = player;

        if (user != null) {
            emailField.setText(user.getEmail());
        }

        if (player != null) {
            if (player.getPays() != null) paysCombo.setValue(player.getPays());
            if (player.getNiveau() != null) niveauCombo.setValue(player.getNiveau());
            if (player.isStatut()) actifRadio.setSelected(true);
            else inactifRadio.setSelected(true);
        } else {
            actifRadio.setSelected(true);
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
        String niveau = niveauCombo.getValue();
        boolean statut = actifRadio.isSelected();

        if (email.isEmpty()) { showError("L'email est requis."); return; }
        if (!email.contains("@")) { showError("Format d'email invalide."); return; }
        if (!password.isEmpty()) {
            if (password.length() < 6) { showError("Mot de passe trop court (min. 6 caractères)."); return; }
            if (!password.equals(confirmPassword)) { showError("Les mots de passe ne correspondent pas."); return; }
        }

        try {
            currentUser.setEmail(email);
            if (selectedFile != null) {
                currentUser.setProfileImage(selectedFile.getAbsolutePath());
            }
            userService.updateOne(currentUser);

            if (!password.isEmpty()) {
                userService.updatePassword(currentUser, password);
            }

            if (currentPlayer == null) currentPlayer = new Player();
            currentPlayer.setId(currentUser.getId());
            currentPlayer.setPays(pays);
            currentPlayer.setNiveau(niveau);
            currentPlayer.setStatut(statut);
            playerService.modifier(currentPlayer);

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