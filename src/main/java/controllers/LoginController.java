package controllers;

import entities.Roles;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.UserService;
import utils.PreferencesRepository;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMe;
    @FXML private CheckBox captchaCheck;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label captchaError;
    @FXML private Label globalError;

    private final PreferencesRepository prefs = new PreferencesRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        passwordField.setOnAction(e -> onLogin());

        // Clear field errors on typing
        emailField.textProperty().addListener((obs, o, n) -> clearError(emailField, emailError));
        passwordField.textProperty().addListener((obs, o, n) -> clearError(passwordField, passwordError));
        captchaCheck.selectedProperty().addListener((obs, o, n) -> {
            if (n) clearError(null, captchaError);
        });
    }

    @FXML
    private void onLogin() {
        clearAllErrors();
        boolean valid = true;

        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty()) {
            showFieldError(emailField, emailError, "L'email est requis.");
            valid = false;
        } else if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            showFieldError(emailField, emailError, "Format d'email invalide.");
            valid = false;
        }

        if (password.isEmpty()) {
            showFieldError(passwordField, passwordError, "Le mot de passe est requis.");
            valid = false;
        } else if (password.length() < 6) {
            showFieldError(passwordField, passwordError, "Minimum 6 caractères.");
            valid = false;
        }

        if (!captchaCheck.isSelected()) {
            captchaError.setText("Veuillez cocher la case reCAPTCHA.");
            captchaError.setVisible(true);
            captchaError.setManaged(true);
            valid = false;
        }

        if (!valid) return;
        User user=null;
        try {
             user = authenticate(email, password);

        }catch (IllegalStateException e){
            globalError.setText("Account is blocked");
            globalError.setVisible(true);
            globalError.setManaged(true);
            return;
        }catch (Exception e){user=null;}
        if (user != null) {
            prefs.saveSession(user, rememberMe.isSelected());
            routeByRole(user);
        } else {
            globalError.setText("❌  Email ou mot de passe incorrect.");
            globalError.setVisible(true);
            globalError.setManaged(true);
            // Shake animation on both fields
            shakeField(emailField);
            shakeField(passwordField);
        }
    }

    private void showFieldError(Control field, Label errorLabel, String message) {
        errorLabel.setText("⚠  " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        if (field != null) field.getStyleClass().add("input-error");
    }

    private void clearError(Control field, Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setText("");
        if (field != null) field.getStyleClass().remove("input-error");
    }

    private void clearAllErrors() {
        clearError(emailField, emailError);
        clearError(passwordField, passwordError);
        clearError(null, captchaError);
        globalError.setVisible(false);
        globalError.setManaged(false);
        globalError.setText("");
    }

    private void shakeField(Control field) {
        javafx.animation.TranslateTransition tt =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(60), field);
        tt.setFromX(0); tt.setByX(8); tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }

    private void routeByRole(User user) {
        if (user.getRole() == Roles.ROLE_ADMIN) navigateToAdmin(user);
        else navigateToMain(user);
    }

    private void navigateToAdmin(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminDashboard.fxml"));
            Parent root = loader.load();
            AdminDashboardController ctrl = loader.getController();
            ctrl.setAdminEmail(user.getEmail());
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/clutchx-theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/admin-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("ClutchX — Backoffice Admin");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToMain(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainLayout.fxml"));
            Parent root = loader.load();
            MainLayoutController ctrl = loader.getController();
            ctrl.setUserEmail(user.getEmail());
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/clutchx-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("ClutchX — Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML private void onGoogleLogin()    { showInfo("Google Login", "OAuth Google à implémenter."); }
    @FXML private void onFaceIdLogin()    { showInfo("Face ID", "Reconnaissance faciale à implémenter."); }
    @FXML private void onForgotPassword() { showInfo("Mot de passe oublié", "Un email de réinitialisation sera envoyé."); }
    @FXML private void onSignupPlayer()   { navigateTo("/PlayerSignup.fxml", "ClutchX — Inscription Joueur"); }
    @FXML private void onSignupCoach()    { navigateTo("/CoachSignup.fxml",  "ClutchX — Inscription Coach"); }

    private User authenticate(String email, String password) throws Exception {
         return new UserService().login(email, password);

    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/clutchx-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}