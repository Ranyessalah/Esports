package controllers;

import entities.Roles;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.UserService;
import utils.PreferencesRepository;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.scene.web.WebView;

public class LoginController implements Initializable {

    public VBox captchaVBox;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMe;
    @FXML private CheckBox captchaCheck;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label captchaError;
    @FXML private Label globalError;
    @FXML private StackPane captchaContainer;

    private WebView webView;
    private javafx.animation.Timeline heightPoller;
    private final PreferencesRepository prefs = new PreferencesRepository();
    private com.sun.net.httpserver.HttpServer captchaServer;

    // ─────────────────────────── CAPTCHA SERVER ───────────────────────────

    private void startCaptchaServer() throws Exception {
        stopCaptchaServer();

        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                captchaServer = com.sun.net.httpserver.HttpServer.create(
                        new java.net.InetSocketAddress("localhost", 8765), 0);
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
                    html, body {
                        background: transparent !important;
                        width: 304px;
                        height: auto;
                        overflow: visible;
                    }
                </style>
            </head>
            <body>
                <div id="recaptcha-container"></div>
                <script>
                    function onRecaptchaLoad() {
                        grecaptcha.render('recaptcha-container', {
                            'sitekey': '6LeqfbksAAAAAEKK6Ylor5-KLnUNrLa1rfg2DWDJ',
                            'theme'  : 'dark',
                            'size'   : 'normal',
                            'callback': function(token) {
                                window.captchaToken = token;
                                document.body.style.height = '78px';
                                document.body.style.overflow = 'hidden';
                            },
                            'expired-callback': function() {
                                window.captchaToken = '';
                            }
                        });
                    }
                    function getCaptchaToken() {
                        return window.captchaToken || '';
                    }
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
        if (heightPoller != null) {
            heightPoller.stop();
            heightPoller = null;
        }
        if (captchaServer != null) {
            captchaServer.stop(1);
            captchaServer = null;
        }
    }

    // ─────────────────────────── CAPTCHA WEBVIEW ──────────────────────────

    private void loadCaptcha() {
        try { startCaptchaServer(); } catch (Exception e) { e.printStackTrace(); }

        webView = new WebView();
        webView.setPrefHeight(78);
        webView.setMinHeight(78);
        webView.setMaxHeight(Double.MAX_VALUE);
        webView.setStyle("-fx-background-color: transparent;");
        webView.getEngine().setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
        );
        webView.getEngine().load("http://localhost:8765/captcha");
        captchaContainer.getChildren().add(webView);

        // Démarrer le polling de hauteur dès que le document est chargé
        webView.getEngine().documentProperty().addListener((obs, oldDoc, doc) -> {
            if (doc != null) startHeightPoller();
        });

        // Enregistrer le popup handler dès que la Scene est disponible
        webView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) registerPopupHandler(newScene);
        });
    }

    private void startHeightPoller() {
        if (heightPoller != null) heightPoller.stop();

        // Polling rapide (100ms) pour détecter l'ouverture du challenge immédiatement
        heightPoller = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.millis(100),
                        e -> updateWebViewHeight()
                )
        );
        heightPoller.setCycleCount(javafx.animation.Animation.INDEFINITE);
        heightPoller.play();

        webView.sceneProperty().addListener((obs, o, n) -> {
            if (n == null && heightPoller != null) heightPoller.stop();
        });
    }
    private void updateWebViewHeight() {
        try {
            Object result = webView.getEngine().executeScript(
                    "(function() {" +
                            "  var el = document.getElementById('recaptcha-container');" +
                            "  if (!el) return 78;" +
                            "  var iframe = document.querySelector('iframe[title*=\"recaptcha\"], iframe[src*=\"recaptcha\"]');" +
                            "  if (iframe) {" +
                            "    var rect = iframe.getBoundingClientRect();" +
                            "    return Math.max(78, rect.bottom + 8);" +
                            "  }" +
                            "  return Math.max(78, document.body.scrollHeight);" +
                            "})()"
            );

            if (result instanceof Number) {
                double rawHeight = ((Number) result).doubleValue();
                double clamped   = Math.max(78, Math.min(rawHeight, 600));

                if (Math.abs(webView.getPrefHeight() - clamped) > 4) {
                    // 1. Redimensionner le WebView
                    webView.setPrefHeight(clamped);
                    webView.setMinHeight(clamped);

                    // 2. Redimensionner le StackPane conteneur
                    captchaContainer.setPrefHeight(clamped);
                    captchaContainer.setMinHeight(clamped);
                    captchaContainer.setMaxHeight(clamped);

                    // 3. Redimensionner le VBox parent
                    captchaVBox.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);

                    // 4. Forcer le layout sur toute la chaîne
                    captchaContainer.requestLayout();
                    captchaVBox.requestLayout();
                    if (captchaVBox.getParent() != null) {
                        captchaVBox.getParent().requestLayout();
                        if (captchaVBox.getParent().getParent() != null) {
                            captchaVBox.getParent().getParent().requestLayout();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void registerPopupHandler(Scene parentScene) {
        webView.getEngine().setCreatePopupHandler(config -> {
            WebView popupView = new WebView();
            popupView.getEngine().setUserAgent(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36"
            );

            Stage popupStage = new Stage();
            popupStage.initOwner(parentScene.getWindow());
            popupStage.initModality(Modality.WINDOW_MODAL);
            popupStage.setTitle("Vérification de sécurité");
            popupStage.setResizable(true);
            popupStage.setMinWidth(420);
            popupStage.setMinHeight(500);

            StackPane pane = new StackPane(popupView);
            pane.setStyle("-fx-background-color: #1a1a2e;");
            popupStage.setScene(new Scene(pane, 460, 580));

            popupStage.setOnShown(e -> {
                javafx.geometry.Rectangle2D screen =
                        javafx.stage.Screen.getPrimary().getVisualBounds();
                popupStage.setX((screen.getWidth()  - 460) / 2);
                popupStage.setY((screen.getHeight() - 580) / 2);
            });

            // Fermer automatiquement quand le challenge est validé
            popupView.getEngine().documentProperty().addListener((obs, o, doc) -> {
                if (doc != null) {
                    popupView.getEngine().titleProperty().addListener((o2, old, title) -> {
                        if (title != null && title.isEmpty()) {
                            javafx.application.Platform.runLater(popupStage::close);
                        }
                    });
                }
            });

            popupStage.show();
            return popupView.getEngine();
        });
    }

    // ─────────────────────────── LIFECYCLE ────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        passwordField.setOnAction(e -> onLogin());
        emailField.textProperty().addListener((obs, o, n) -> clearError(emailField, emailError));
        passwordField.textProperty().addListener((obs, o, n) -> clearError(passwordField, passwordError));
        loadCaptcha();
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

        String token = getCaptchaToken();
        if (token == null || token.isEmpty()) {
            showCaptchaError("Veuillez valider le CAPTCHA.");
            return;
        }
        if (!verifyCaptcha(token)) {
            showCaptchaError("CAPTCHA invalide.");
            return;
        }

        if (!valid) return;

        User user = null;
        try {
            user = authenticate(email, password);
        } catch (IllegalStateException e) {
            globalError.setText("Compte bloqué.");
            globalError.setVisible(true);
            globalError.setManaged(true);
            return;
        } catch (Exception e) {
            user = null;
        }

        if (user != null) {
            prefs.saveSession(user, rememberMe.isSelected());
            routeByRole(user);
        } else {
            globalError.setText("❌  Email ou mot de passe incorrect.");
            globalError.setVisible(true);
            globalError.setManaged(true);
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
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void navigateToMain(User user) {
        stopCaptchaServer();
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
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void navigateTo(String fxmlPath, String title) {
        stopCaptchaServer();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/clutchx-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ─────────────────────────── UI HELPERS ───────────────────────────────

    private void showCaptchaError(String msg) {
        captchaError.setText(msg);
        captchaError.setVisible(true);
        captchaError.setManaged(true);
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
        tt.setFromX(0); tt.setByX(8); tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.play();
    }

    // ─────────────────────────── AUTH / CAPTCHA ───────────────────────────

    private User authenticate(String email, String password) throws Exception {
        return new UserService().login(email, password);
    }

    private String getCaptchaToken() {
        try {
            return (String) webView.getEngine().executeScript("getCaptchaToken()");
        } catch (Exception e) { return ""; }
    }

    private boolean verifyCaptcha(String token) {
        try {
            String secret = "6LeqfbksAAAAADW7yWqD_CxKKSuStm4KFm7uhtJV";
            URL url = new URL("https://www.google.com/recaptcha/api/siteverify");
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

    // ─────────────────────────── FXML ACTIONS ─────────────────────────────

    @FXML private void onGoogleLogin()    { showInfo("Google Login", "OAuth Google à implémenter."); }
    @FXML private void onFaceIdLogin()    { showInfo("Face ID", "Reconnaissance faciale à implémenter."); }
    @FXML private void onSignupPlayer()   { navigateTo("/PlayerSignup.fxml", "ClutchX — Inscription Joueur"); }
    @FXML private void onSignupCoach()    { navigateTo("/CoachSignup.fxml",  "ClutchX — Inscription Coach"); }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
    @FXML
    private void onForgotPassword() {
        stopCaptchaServer();
        navigateTo("/ForgotPassword.fxml", "ClutchX — Mot de passe oublié");
    }
}