package controllers.userManagement;

import entities.userManagement.Roles;
import entities.userManagement.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.userManagement.FaceIdService;
import services.userManagement.GoogleAuthService;
import services.userManagement.UserService;
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
    @FXML private javafx.scene.layout.HBox captchaBox;

    // ─── CAPTCHA ──────────────────────────────────────────────────────────
    private WebView captchaWebView;
    private com.sun.net.httpserver.HttpServer captchaServer;
    private javafx.animation.Timeline heightPoller;
    private boolean captchaVerified = false;
    private String lastCaptchaToken = "";

    // ─── Google OAuth ─────────────────────────────────────────────────────
    private GoogleAuthService googleAuthService;

    private final PreferencesRepository prefs = new PreferencesRepository();

    // ─────────────────────────── LIFECYCLE ────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        passwordField.setOnAction(e -> onLogin());
        emailField.textProperty().addListener((obs, o, n) -> clearError(emailField, emailError));
        passwordField.textProperty().addListener((obs, o, n) -> clearError(passwordField, passwordError));
        captchaVerified = false;
    }

    // ─────────────────────────── GOOGLE LOGIN ─────────────────────────────

    @FXML
    private void onGoogleLogin() {
        // Disable button to prevent double-click
        setGoogleButtonsDisabled(true);
        showGlobalInfo("⏳  Ouverture de Google dans votre navigateur…");

        googleAuthService = new GoogleAuthService();

        googleAuthService.authenticate()
                .thenAccept(googleUser -> Platform.runLater(() -> {
                    setGoogleButtonsDisabled(false);
                    clearAllErrors();
                    handleGoogleUser(googleUser);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setGoogleButtonsDisabled(false);
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        if (msg != null && msg.contains("access_denied")) {
                            showGlobalError("❌  Connexion Google annulée.");
                        } else if (msg != null && msg.contains("timeout")) {
                            showGlobalError("⏱  Délai dépassé. Veuillez réessayer.");
                        } else {
                            showGlobalError("❌  Erreur Google : " + msg);
                        }
                    });
                    return null;
                });
    }

    /**
     * Called after successful Google authentication.
     * Tries to find an existing account by email, or auto-registers the user.
     */
    private void handleGoogleUser(GoogleAuthService.GoogleUser googleUser) {
        if (!googleUser.emailVerified()) {
            showGlobalError("❌  Cet email Google n'est pas vérifié.");
            return;
        }

        UserService userService = new UserService();
        User user = null;

        try {
            // Attempt login by email (Google users have no password stored)
            user = userService.findByEmail(googleUser.email());
        } catch (Exception ignored) {}

        if (user == null) {
            // Auto-register with Google profile data
            try {
                user = userService.registerWithGoogle(
                        googleUser.email(),
                        googleUser.firstName(),
                        googleUser.lastName(),
                        googleUser.googleId()
                );
            } catch (IllegalStateException e) {
                showGlobalError("Compte bloqué.");
                return;
            } catch (Exception e) {
                showGlobalError("❌  Impossible de créer le compte : " + e.getMessage());
                return;
            }
        }

        if (user != null) {
            prefs.saveSession(user, true); // "remember me" always true for OAuth
            routeByRole(user);
        } else {
            showGlobalError("❌  Impossible de se connecter avec Google.");
        }
    }

    private void setGoogleButtonsDisabled(boolean disabled) {
        if (emailField.getScene() != null) {
            var node = emailField.getScene().lookup(".social-btn");
            if (node instanceof Button btn) {
                btn.setDisable(disabled);
            }
        }
    }

    // ─────────────────────────── CAPTCHA POPUP ────────────────────────────

    @FXML
    private void onOpenCaptchaPopup() {
        if (captchaVerified) return;

        lastCaptchaToken = "";

        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initOwner(emailField.getScene().getWindow());
        popupStage.setTitle("Vérification reCAPTCHA");
        popupStage.setResizable(false);

        StackPane container = new StackPane();
        container.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 20;");
        container.setPrefSize(360, 460);

        try {
            startCaptchaServer();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        captchaWebView = new WebView();
        captchaWebView.setPrefSize(304, 78);
        captchaWebView.setStyle("-fx-background-color: transparent;");
        captchaWebView.getEngine().setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
        );
        captchaWebView.getEngine().load("http://localhost:8768/captcha");

        captchaWebView.getEngine().documentProperty().addListener((obs, oldDoc, doc) -> {
            if (doc != null) startHeightPoller(captchaWebView, container, popupStage);
        });

        container.getChildren().add(captchaWebView);
        popupStage.setScene(new javafx.scene.Scene(container));

        popupStage.setOnHidden(e -> {
            stopCaptchaServer();
            if (!lastCaptchaToken.isEmpty() && verifyCaptcha(lastCaptchaToken)) {
                captchaVerified = true;
                updateCaptchaUI(true);
                clearError(null, captchaError);
            }
        });

        popupStage.show();
    }

    private void updateCaptchaUI(boolean verified) {
        if (verified) {
            captchaCheck.setSelected(true);
            captchaCheck.setDisable(true);
        }
    }

    private void startCaptchaServer() throws Exception {
        stopCaptchaServer();
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                captchaServer = com.sun.net.httpserver.HttpServer.create(
                        new java.net.InetSocketAddress("localhost", 8768), 0);
                break;
            } catch (java.net.BindException e) {
                if (i == maxRetries - 1) throw e;
                Thread.sleep(300);
            }
        }

        String html = """
        <!DOCTYPE html>
        <html>
        <head>
            <script src="https://www.google.com/recaptcha/api.js?onload=onRecaptchaLoad&render=explicit" async defer></script>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body { background: transparent !important; width: 304px; overflow: visible; }
            </style>
        </head>
        <body>
            <div id="recaptcha-container"></div>
            <script>
                function onRecaptchaLoad() {
                    grecaptcha.render('recaptcha-container', {
                        'sitekey': '6LeqfbksAAAAAEKK6Ylor5-KLnUNrLa1rfg2DWDJ',
                        'theme': 'dark',
                        'size': 'normal',
                        'callback': function(token) { window.captchaToken = token; },
                        'expired-callback': function() { window.captchaToken = ''; }
                    });
                }
                function getCaptchaToken() { return window.captchaToken || ''; }
            </script>
        </body>
        </html>
        """;

        captchaServer.createContext("/captcha", exchange -> {
            byte[] bytes = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        });
        captchaServer.start();
    }

    private void stopCaptchaServer() {
        if (heightPoller != null) { heightPoller.stop(); heightPoller = null; }
        if (captchaServer != null) { captchaServer.stop(1); captchaServer = null; }
    }

    private void startHeightPoller(WebView wv, StackPane container, Stage stage) {
        if (heightPoller != null) heightPoller.stop();
        heightPoller = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(200), e -> {
                    try {
                        Object result = wv.getEngine().executeScript(
                                "(function(){ var el=document.getElementById('recaptcha-container');" +
                                        "if(!el) return 78;" +
                                        "return Math.max(78, document.body.scrollHeight); })()"
                        );
                        if (result instanceof Number) {
                            double h = Math.min(((Number) result).doubleValue(), 500);
                            wv.setPrefHeight(h);
                            container.setPrefHeight(h + 40);
                            stage.sizeToScene();

                            String token = getCaptchaToken();
                            if (token != null && !token.isEmpty()) {
                                lastCaptchaToken = token;
                                javafx.application.Platform.runLater(stage::close);
                            }
                        }
                    } catch (Exception ignored) {}
                })
        );
        heightPoller.setCycleCount(javafx.animation.Animation.INDEFINITE);
        heightPoller.play();
    }

    private String getCaptchaToken() {
        try {
            return (String) captchaWebView.getEngine().executeScript("getCaptchaToken()");
        } catch (Exception e) { return ""; }
    }

    private boolean verifyCaptcha(String token) {
        try {
            String secret = "6LeqfbksAAAAADW7yWqD_CxKKSuStm4KFm7uhtJV";
            java.net.URL url = new java.net.URL("https://www.google.com/recaptcha/api/siteverify");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            String params = "secret=" + secret + "&response=" + token;
            conn.getOutputStream().write(params.getBytes());
            conn.getOutputStream().flush();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            return response.toString().contains("\"success\": true");
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ─────────────────────────── LOGIN ────────────────────────────────────

    @FXML
    private void onLogin() {
        clearAllErrors();
        boolean valid = true;

        String email    = emailField.getText().trim();
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

        if (!captchaVerified) {
            setError(null, captchaError, "Veuillez valider le reCAPTCHA.");
            return;
        }

        if (!valid) return;

        User user = null;
        try {
            user = authenticate(email, password);
        } catch (IllegalStateException e) {
            showGlobalError("Compte bloqué.");
            return;
        } catch (Exception e) {
            user = null;
        }

        if (user != null) {
            prefs.saveSession(user, rememberMe.isSelected());
            routeByRole(user);
        } else {
            showGlobalError("❌  Email ou mot de passe incorrect.");
            shakeField(emailField);
            shakeField(passwordField);
        }
    }

    // ─────────────────────────── NAVIGATION ───────────────────────────────

    private void routeByRole(User user) {
        if (user.getRole() == Roles.ROLE_ADMIN) navigateToAdmin(user);
        else navigateToMain(user);
    }

    private void navigateToAdmin(User user) {
        stopCaptchaServer();
        stopGoogleAuth();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/userManagement/AdminDashboard.fxml"));
            Parent root = loader.load();
            AdminDashboardController ctrl = loader.getController();
            ctrl.setAdminEmail(user.getEmail());
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/userManagement/clutchx-theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/userManagement/admin-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("ClutchX — Backoffice Admin");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void navigateToMain(User user) {
        stopCaptchaServer();
        stopGoogleAuth();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/userManagement/MainLayout.fxml"));
            Parent root = loader.load();
            MainLayoutController ctrl = loader.getController();
            ctrl.setUserEmail(user.getEmail());
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/userManagement/clutchx-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("ClutchX — Dashboard");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void navigateTo(String fxmlPath, String title) {
        stopCaptchaServer();
        stopGoogleAuth();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/userManagement/clutchx-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void stopGoogleAuth() {
        if (googleAuthService != null) {
            googleAuthService.stop();
            googleAuthService = null;
        }
    }

    // ─────────────────────────── UI HELPERS ───────────────────────────────

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

    private void showFieldError(Control field, Label errorLabel, String message) {
        setError(field, errorLabel, message);
    }

    private void showGlobalError(String message) {
        globalError.setText(message);
        globalError.setStyle("-fx-text-fill: #e74c3c;");
        globalError.setVisible(true);
        globalError.setManaged(true);
    }

    private void showGlobalInfo(String message) {
        globalError.setText(message);
        globalError.setStyle("-fx-text-fill: #3498db;");
        globalError.setVisible(true);
        globalError.setManaged(true);
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
        tt.setFromX(0); tt.setByX(8); tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.play();
    }

    // ─────────────────────────── AUTH ─────────────────────────────────────

    private User authenticate(String email, String password) throws Exception {
        return new UserService().login(email, password);
    }

    // ─────────────────────────── FXML ACTIONS ─────────────────────────────

    @FXML private void onSignupPlayer() { navigateTo("/userManagement/PlayerSignup.fxml", "ClutchX — Inscription Joueur"); }
    @FXML private void onSignupCoach()  { navigateTo("/userManagement/CoachSignup.fxml",  "ClutchX — Inscription Coach"); }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    @FXML
    private void onForgotPassword() {
        stopCaptchaServer();
        navigateTo("/userManagement/ForgotPassword.fxml", "ClutchX — Mot de passe oublié");
    }
    private void openFaceIdCamera(FaceIdCameraController.Mode mode,
                                  int userId,
                                  java.util.function.Consumer<Boolean> onEnroll,
                                  java.util.function.Consumer<Integer> onAuth) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/userManagement/FaceIdCamera.fxml"));
            javafx.scene.Parent root = loader.load();
            FaceIdCameraController ctrl = loader.getController();
            ctrl.configure(mode, userId, onEnroll, onAuth);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(emailField.getScene().getWindow());
            stage.setTitle("Face ID");
            stage.setResizable(false);
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/userManagement/clutchx-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showGlobalError("❌  Impossible d'ouvrir Face ID : " + e.getMessage());
        }
    }
    @FXML
    private void onFaceIdLogin() {
        if (!FaceIdService.getInstance().isAvailable()) {
            showGlobalError("❌  Face ID non disponible (OpenCV introuvable).");
            return;
        }

        openFaceIdCamera(FaceIdCameraController.Mode.AUTHENTICATE, -1,
                null,
                userId -> {
                    if (userId < 0) {
                        showGlobalError("❌  Visage non reconnu. Réessayez.");
                        return;
                    }
                    UserService us = new UserService();
                    try {
                        User user = us.findById(userId);
                        if (user == null) {
                            showGlobalError("❌  Aucun compte associé.");
                            return;
                        }
                        if (user.isBlocked()) {
                            showGlobalError("🚫  Compte bloqué.");
                            return;
                        }
                        prefs.saveSession(user, true);
                        routeByRole(user);
                    } catch (Exception e) {
                        showGlobalError("❌  Erreur : " + e.getMessage());
                    }
                });
    }

}