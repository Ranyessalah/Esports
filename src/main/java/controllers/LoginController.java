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
    private final PreferencesRepository prefs= new PreferencesRepository();
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        passwordField.setOnAction(e -> onLogin());
    }

    @FXML
    private void onLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Champs manquants", "Veuillez remplir l'email et le mot de passe.");
            return;
        }
        if (!captchaCheck.isSelected()) {
            showError("CAPTCHA", "Veuillez cocher la case reCAPTCHA.");
            return;
        }

        User user = authenticate(email, password);

        if (user != null) {
            this.prefs.saveSession(user,rememberMe.isSelected());
            routeByRole(user);
        } else {
            showError("Connexion échouée", "Email ou mot de passe incorrect.");
        }
    }

    private void routeByRole(User user) {
        if (user.getRole() == Roles.ROLE_ADMIN) {
            navigateToAdmin(user);
        } else {
            navigateToMain(user);
        }
    }

    // ── Admin Backoffice ──
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
            showError("Erreur", "Impossible de charger le dashboard admin.");
        }
    }

    // ── Main Dashboard (coach / player) ──
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
            showError("Erreur", "Impossible de charger le dashboard.");
        }
    }

    @FXML private void onGoogleLogin()    { showInfo("Google Login", "OAuth Google à implémenter."); }
    @FXML private void onFaceIdLogin()    { showInfo("Face ID", "Reconnaissance faciale à implémenter."); }
    @FXML private void onForgotPassword() { showInfo("Mot de passe oublié", "Un email de réinitialisation sera envoyé."); }

    @FXML private void onSignupPlayer() { navigateTo("/PlayerSignup.fxml", "ClutchX — Inscription Joueur"); }
    @FXML private void onSignupCoach()  { navigateTo("/CoachSignup.fxml",  "ClutchX — Inscription Coach");  }

    private User authenticate(String email, String password) {
        try { return new UserService().login(email, password); }
        catch (Exception e) { return null; }
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
            showError("Erreur", "Impossible de charger : " + fxmlPath);
        }
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}