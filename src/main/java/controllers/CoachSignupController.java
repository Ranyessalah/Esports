package controllers;

import entities.Coach;
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
import services.CoachService;
import services.UserService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class CoachSignupController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> specialityCombo;
    @FXML private ComboBox<String> countryCombo;
    @FXML private ToggleButton toggleOui;
    @FXML private ToggleButton toggleNon;
    @FXML private Label fileNameLabel;
    @FXML private CheckBox captchaCheck;

    private ToggleGroup availabilityGroup;
    private File selectedProfilePhoto;
    private final UserService userService = new UserService();
    private final CoachService coachService = new CoachService();
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSpecialities();
        setupCountries();
        setupAvailabilityToggle();
    }

    private void setupSpecialities() {
        specialityCombo.setItems(FXCollections.observableArrayList(
                "League of Legends", "Valorant", "CS:GO / CS2",
                "Fortnite", "Apex Legends", "Overwatch",
                "Dota 2", "Rainbow Six Siege", "FIFA / EA FC", "Rocket League"
        ));
    }

    private void setupCountries() {
        countryCombo.setItems(FXCollections.observableArrayList(
                "Tunisie", "France", "Maroc", "Algérie", "Belgique",
                "Suisse", "Canada", "Sénégal", "Côte d'Ivoire", "Autre"
        ));
    }

    private void setupAvailabilityToggle() {
        availabilityGroup = new ToggleGroup();
        toggleOui.setToggleGroup(availabilityGroup);
        toggleNon.setToggleGroup(availabilityGroup);
        toggleNon.setSelected(true);

        availabilityGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == toggleOui) {
                toggleOui.getStyleClass().add("toggle-btn-active");
                toggleNon.getStyleClass().remove("toggle-btn-active");
            } else if (newVal == toggleNon) {
                toggleNon.getStyleClass().add("toggle-btn-active");
                toggleOui.getStyleClass().remove("toggle-btn-active");
            }
        });
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

        // Récupérer les valeurs du formulaire
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String role = "COACH"; // assuming your enum has COACH
        String type = "coach"; // default
        String speciality = specialityCombo.getValue();
        String country = countryCombo.getValue();
        boolean available = toggleOui.isSelected();
        String profileImage = selectedProfilePhoto != null ? selectedProfilePhoto.getName() : "default.png";

        // Créer l'objet User
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(password);
        newUser.setRole(Roles.ROLE_COACH); // adjust based on your enum
        newUser.setType(type);
        newUser.setProfileImage(profileImage);
        Coach coach = new Coach();
        // Optionally, store extra info in User if your entity allows it
        coach.setSpecialite(speciality);
        coach.setPays(country);
        coach.setDisponibilite(available);


        try {
            User u1 =new User();
            u1 = userService.insertOne(newUser);
            coach.setId(u1.getId());
            coachService.ajouter(coach);
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Compte créé");
            success.setHeaderText("Bienvenue sur ClutchX !");
            success.setContentText("Votre compte coach a été créé avec succès.");
            success.showAndWait();

            onBackToLogin();

        } catch (SQLException e) {
            showError("Erreur", "Impossible de créer le compte : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            showError("Validation", e.getMessage());
        }
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
        if (specialityCombo.getValue() == null) {
            showError("Spécialité manquante", "Veuillez sélectionner une spécialité.");
            return false;
        }
        if (countryCombo.getValue() == null) {
            showError("Pays manquant", "Veuillez sélectionner votre pays.");
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
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}