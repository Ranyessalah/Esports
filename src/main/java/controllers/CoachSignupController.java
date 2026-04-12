package controllers;

import entities.Coach;
import entities.Roles;
import entities.User;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
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

    // Error labels
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label confirmError;
    @FXML private Label specialityError;
    @FXML private Label countryError;
    @FXML private Label captchaError;
    @FXML private Label globalError;

    // Password strength
    @FXML private javafx.scene.layout.HBox strengthBar;
    @FXML private Label strengthLabel;

    private ToggleGroup availabilityGroup;
    private File selectedProfilePhoto;
    private final UserService userService = new UserService();
    private final CoachService coachService = new CoachService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSpecialities();
        setupCountries();
        setupAvailabilityToggle();
        setupLiveValidation();
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

        // Set initial state
        toggleNon.setSelected(true);
        toggleOui.getStyleClass().remove("toggle-btn-active");
        toggleNon.getStyleClass().remove("toggle-btn-active");
        toggleNon.getStyleClass().add("toggle-btn-active");

        availabilityGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            // Prevent full deselection — restore previous if user clicks selected button
            if (newVal == null) {
                if (oldVal != null) oldVal.setSelected(true);
                return;
            }

            toggleOui.getStyleClass().remove("toggle-btn-active");
            toggleNon.getStyleClass().remove("toggle-btn-active");

            if (newVal == toggleOui) {
                toggleOui.getStyleClass().add("toggle-btn-active");
            } else {
                toggleNon.getStyleClass().add("toggle-btn-active");
            }
        });
    }
    private void setupLiveValidation() {
        // Email — clear on type, validate on focus lost
        emailField.textProperty().addListener((obs, o, n) -> clearError(emailField, emailError));
        emailField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateEmail();
        });

        // Password — live strength + validate on focus lost
        passwordField.textProperty().addListener((obs, o, n) -> {
            clearError(passwordField, passwordError);
            updateStrengthBar(n);
        });
        passwordField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validatePassword();
        });

        // Confirm — clear on type, validate on focus lost
        confirmPasswordField.textProperty().addListener((obs, o, n) -> clearError(confirmPasswordField, confirmError));
        confirmPasswordField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateConfirm();
        });

        // Combos — clear error on selection
        specialityCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) clearError(null, specialityError);
        });
        countryCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) clearError(null, countryError);
        });

        // Captcha
        captchaCheck.selectedProperty().addListener((obs, o, n) -> {
            if (n) clearError(null, captchaError);
        });
    }


    private boolean validateEmail() {
        String v = emailField.getText().trim();
        if (v.isEmpty())
            return setError(emailField, emailError, "L'email est requis.");
        if (!v.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$"))
            return setError(emailField, emailError, "Format d'email invalide.");
        clearError(emailField, emailError);
        return true;
    }

    private boolean validatePassword() {
        String v = passwordField.getText();
        if (v.isEmpty())
            return setError(passwordField, passwordError, "Le mot de passe est requis.");
        if (v.length() < 8)
            return setError(passwordField, passwordError, "Minimum 8 caractères requis.");
        if (!v.matches(".*[A-Z].*"))
            return setError(passwordField, passwordError, "Au moins une majuscule requise.");
        if (!v.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"))
            return setError(passwordField, passwordError, "Au moins un caractère spécial requis.");
        clearError(passwordField, passwordError);
        return true;
    }

    private boolean validateConfirm() {
        String v = confirmPasswordField.getText();
        if (v.isEmpty())
            return setError(confirmPasswordField, confirmError, "Veuillez confirmer le mot de passe.");
        if (!v.equals(passwordField.getText()))
            return setError(confirmPasswordField, confirmError, "Les mots de passe ne correspondent pas.");
        clearError(confirmPasswordField, confirmError);
        return true;
    }

    private void updateStrengthBar(String password) {
        if (strengthBar == null || strengthLabel == null) return;
        int score = 0;
        if (password.length() >= 8)                                                   score++;
        if (password.matches(".*[A-Z].*"))                                            score++;
        if (password.matches(".*[0-9].*"))                                            score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"))      score++;

        String[] styles = { "", "strength-weak", "strength-fair", "strength-good", "strength-strong" };
        String[] labels = { "", "Faible", "Moyen", "Bon", "Fort" };

        for (int i = 0; i < strengthBar.getChildren().size(); i++) {
            strengthBar.getChildren().get(i).getStyleClass()
                    .removeAll("strength-weak", "strength-fair", "strength-good", "strength-strong");
            if (i < score)
                strengthBar.getChildren().get(i).getStyleClass().add(styles[score]);
        }

        strengthLabel.setText(score > 0 ? labels[score] : "");
        strengthLabel.getStyleClass()
                .removeAll("strength-weak", "strength-fair", "strength-good", "strength-strong");
        if (score > 0) strengthLabel.getStyleClass().add(styles[score]);
    }


    private boolean setError(Control field, Label label, String message) {
        label.setText("⚠  " + message);
        label.setVisible(true);
        label.setManaged(true);
        if (field != null) {
            field.getStyleClass().remove("input-error");
            field.getStyleClass().add("input-error");
        }
        return false;
    }

    private void clearError(Control field, Label label) {
        label.setVisible(false);
        label.setManaged(false);
        label.setText("");
        if (field != null) field.getStyleClass().remove("input-error");
    }

    private void clearAllErrors() {
        clearError(emailField, emailError);
        clearError(passwordField, passwordError);
        clearError(confirmPasswordField, confirmError);
        clearError(null, specialityError);
        clearError(null, countryError);
        clearError(null, captchaError);
        globalError.setVisible(false);
        globalError.setManaged(false);
    }

    private void shake(Control field) {
        javafx.animation.TranslateTransition tt =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(60), field);
        tt.setFromX(0); tt.setByX(8); tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.play();
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
        clearAllErrors();
        boolean valid = true;

        valid &= validateEmail();
        valid &= validatePassword();
        valid &= validateConfirm();

        if (specialityCombo.getValue() == null) {
            setError(null, specialityError, "Veuillez sélectionner une spécialité.");
            shake(specialityCombo);
            valid = false;
        }
        if (countryCombo.getValue() == null) {
            setError(null, countryError, "Veuillez sélectionner votre pays.");
            shake(countryCombo);
            valid = false;
        }
        if (!captchaCheck.isSelected()) {
            setError(null, captchaError, "Veuillez cocher la case reCAPTCHA.");
            valid = false;
        }

        if (!valid) return;

        String email        = emailField.getText().trim();
        String password     = passwordField.getText();
        String speciality   = specialityCombo.getValue();
        String country      = countryCombo.getValue();
        boolean available   = toggleOui.isSelected();
        String profileImage = selectedProfilePhoto != null ? selectedProfilePhoto.getName() : "default.png";

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(password);
        newUser.setRole(Roles.ROLE_COACH);
        newUser.setType("coach");
        newUser.setProfileImage(profileImage);

        Coach coach = new Coach();
        coach.setSpecialite(speciality);
        coach.setPays(country);
        coach.setDisponibilite(available);

        try {
            User saved = userService.insertOne(newUser);
            coach.setId(saved.getId());
            coachService.ajouter(coach);
            onBackToLogin();

        } catch (SQLException e) {
            globalError.setText("❌  Impossible de créer le compte : " + e.getMessage());
            globalError.setVisible(true);
            globalError.setManaged(true);
        } catch (IllegalArgumentException e) {
            globalError.setText("❌  " + e.getMessage());
            globalError.setVisible(true);
            globalError.setManaged(true);
        }
    }

    @FXML
    private void onBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/clutchx-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("ClutchX — Connexion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onGoogleSignup(ActionEvent actionEvent) {
    }
}