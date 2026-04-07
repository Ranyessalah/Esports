package controllers;

import entities.Player;
import entities.Roles;
import entities.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.PlayerService;
import services.UserService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class PlayerSignupController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> countryCombo;
    @FXML private ComboBox<String> levelCombo;
    @FXML private CheckBox activeCheck;
    @FXML private Label fileNameLabel;
    @FXML private CheckBox captchaCheck;

    private File selectedProfilePhoto;
    private final UserService userService = new UserService();
    private final PlayerService playerService = new PlayerService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCountries();
        setupLevels();
    }

    private void setupCountries() {
        countryCombo.setItems(FXCollections.observableArrayList(
                "Tunisie", "France", "Maroc", "Algérie", "Belgique",
                "Suisse", "Canada", "Sénégal", "Côte d'Ivoire", "Autre"
        ));
    }

    private void setupLevels() {
        levelCombo.setItems(FXCollections.observableArrayList(
                "Débutant", "Intermédiaire", "Avancé", "Expert", "Professionnel"
        ));
    }

    @FXML
    private void onChooseFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        Stage stage = (Stage) fileNameLabel.getScene().getWindow();
        selectedProfilePhoto = fc.showOpenDialog(stage);
        if (selectedProfilePhoto != null) {
            String name = selectedProfilePhoto.getName();
            fileNameLabel.setText(name.length() > 28 ? name.substring(0, 25) + "..." : name);
        }
    }

    @FXML
    private void onCreateAccount() {
        if (!validateForm()) return;

        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String country = countryCombo.getValue();
        String level = levelCombo.getValue();
        boolean active = activeCheck.isSelected();
        String profileImage = selectedProfilePhoto != null ? selectedProfilePhoto.getName() : "default.png";

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(password);
        newUser.setRole(Roles.ROLE_PLAYER);
        newUser.setType("player");
        newUser.setProfileImage(profileImage);

        Player player = new Player();
        player.setPays(country);
        player.setNiveau(level);
        player.setStatut(active);

        try {
            User saved = userService.insertOne(newUser);
            player.setId(saved.getId());
            playerService.ajouter(player);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Compte créé");
            success.setHeaderText("Bienvenue sur ClutchX !");
            success.setContentText("Votre compte joueur a été créé avec succès.");
            success.showAndWait();

            onBackToLogin();

        } catch (SQLException e) {
            showError("Erreur", "Impossible de créer le compte : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            showError("Validation", e.getMessage());
        }
    }

    @FXML
    private void onGoogleSignup() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Google Sign-Up");
        info.setHeaderText(null);
        info.setContentText("L'inscription via Google sera bientôt disponible.");
        info.showAndWait();
    }

    @FXML
    private void onBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(
                    getClass().getResource("/clutchx-theme.css").toExternalForm()
            );
            stage.setScene(scene);
            stage.setTitle("ClutchX — Connexion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean validateForm() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (email.isEmpty() || !email.contains("@")) {
            showError("Email invalide", "Veuillez saisir une adresse email valide.");
            return false;
        }
        if (password.length() < 8) {
            showError("Mot de passe faible", "Au moins 8 caractères requis.");
            return false;
        }
        if (!password.equals(confirm)) {
            showError("Mots de passe différents", "Les mots de passe ne correspondent pas.");
            return false;
        }
        if (countryCombo.getValue() == null) {
            showError("Pays manquant", "Veuillez sélectionner votre pays.");
            return false;
        }
        if (levelCombo.getValue() == null) {
            showError("Niveau manquant", "Veuillez sélectionner votre niveau.");
            return false;
        }
        if (!captchaCheck.isSelected()) {
            showError("CAPTCHA", "Veuillez cocher la case reCAPTCHA.");
            return false;
        }
        return true;
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}