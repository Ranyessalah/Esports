package controllers;

import entities.Coach;
import entities.Roles;
import entities.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.CoachService;
import services.GoogleAuthService;
import services.UserService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class CoachSignupController implements Initializable {

    // ─── CAPTCHA ──────────────────────────────────────────────────────────
    private WebView captchaWebView;
    private com.sun.net.httpserver.HttpServer captchaServer;
    private javafx.animation.Timeline heightPoller;
    private boolean captchaVerified = false;
    private String lastCaptchaToken = "";

    // ─── Google OAuth ─────────────────────────────────────────────────────
    private GoogleAuthService googleAuthService;

    // ─── State for Google pre-fill ────────────────────────────────────────
    /** Set when the user authenticated via Google; signals we skip email/password fields. */
    private GoogleAuthService.GoogleUser pendingGoogleUser = null;

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

    // Password section container — hidden after Google auth
    @FXML private VBox passwordSection;
    @FXML private VBox confirmSection;

    private ToggleGroup availabilityGroup;
    private File selectedProfilePhoto;
    private final UserService userService = new UserService();
    private final CoachService coachService = new CoachService();

    // ─────────────────────────── LIFECYCLE ────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSpecialities();
        setupCountries();
        setupAvailabilityToggle();
        setupLiveValidation();
        captchaVerified = false;
    }

    // ─────────────────────────── GOOGLE SIGNUP ────────────────────────────

    @FXML
    public void onGoogleSignup(ActionEvent actionEvent) {
        setGoogleButtonDisabled(true);
        showGlobalInfo("⏳  Ouverture de Google dans votre navigateur…");

        googleAuthService = new GoogleAuthService();

        googleAuthService.authenticate()
                .thenAccept(googleUser -> Platform.runLater(() -> {
                    setGoogleButtonDisabled(false);
                    clearAllErrors();
                    handleGoogleUser(googleUser);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setGoogleButtonDisabled(false);
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        if (msg != null && msg.contains("access_denied")) {
                            showGlobalError("❌  Inscription Google annulée.");
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
     * Called after successful Google OAuth.
     * If the email already has an account → redirect to login.
     * Otherwise → pre-fill email, hide password fields, wait for the user
     * to fill speciality/country and click "Créer mon compte".
     */
    private void handleGoogleUser(GoogleAuthService.GoogleUser googleUser) {
        if (!googleUser.emailVerified()) {
            showGlobalError("❌  Cet email Google n'est pas vérifié.");
            return;
        }

        // Check if account already exists
        try {
            User existing = userService.findByEmail(googleUser.email());
            if (existing != null) {
                showGlobalError("⚠  Un compte existe déjà avec cet email. Veuillez vous connecter.");
                return;
            }
        } catch (Exception e) {
            showGlobalError("❌  Erreur lors de la vérification : " + e.getMessage());
            return;
        }

        // Store for later use in onCreateAccount()
        pendingGoogleUser = googleUser;

        // Pre-fill email and lock it
        emailField.setText(googleUser.email());
        emailField.setDisable(true);

        // Hide password fields — not needed for Google accounts
        if (passwordSection != null) {
            passwordSection.setVisible(false);
            passwordSection.setManaged(false);
        }
        if (confirmSection != null) {
            confirmSection.setVisible(false);
            confirmSection.setManaged(false);
        }

        // Mark captcha as verified — Google auth replaces it
        captchaVerified = true;
        updateCaptchaUI(true);

        showGlobalInfo("✅  Connecté avec " + googleUser.email()
                + ". Complétez votre profil coach ci-dessous.");
    }

    private void setGoogleButtonDisabled(boolean disabled) {
        if (emailField.getScene() != null) {
            for (Node n : emailField.getScene().getRoot().lookupAll(".social-btn")) {
                n.setDisable(disabled);
            }
        }
    }

    // ─────────────────────────── CREATE ACCOUNT ───────────────────────────

    @FXML
    private void onCreateAccount() {
        clearAllErrors();
        boolean valid = true;

        // Email
        valid &= validateEmail();

        // Password — only required for non-Google signups
        if (pendingGoogleUser == null) {
            valid &= validatePassword();
            valid &= validateConfirm();
        }

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
        if (!captchaVerified) {
            setError(null, captchaError, "Veuillez valider le reCAPTCHA.");
            valid = false;
        }

        if (!valid) return;

        String speciality   = specialityCombo.getValue();
        String country      = countryCombo.getValue();
        boolean available   = toggleOui.isSelected();
        String profileImage = selectedProfilePhoto != null ? selectedProfilePhoto.getName() : "default.png";

        User newUser = new User();
        newUser.setRole(Roles.ROLE_COACH);
        newUser.setType("coach");
        newUser.setProfileImage(profileImage);

        if (pendingGoogleUser != null) {
            // ── Google signup path ─────────────────────────────────────────
            newUser.setEmail(pendingGoogleUser.email());
            newUser.setGoogleId(pendingGoogleUser.googleId());
            newUser.setPassword(null);

            // Bypass the password validation inside insertOne by calling a
            // dedicated Google insert (same as registerWithGoogle but with type=coach)
            try {
                newUser = insertGoogleCoach(newUser);
            } catch (Exception e) {
                showGlobalError("❌  " + e.getMessage());
                return;
            }
        } else {
            // ── Standard signup path ───────────────────────────────────────
            newUser.setEmail(emailField.getText().trim());
            newUser.setPassword(passwordField.getText());

            try {
                newUser = userService.insertOne(newUser);
            } catch (SQLException e) {
                showGlobalError("❌  Impossible de créer le compte : " + e.getMessage());
                return;
            } catch (IllegalArgumentException e) {
                showGlobalError("❌  " + e.getMessage());
                return;
            }
        }

        // Create the Coach profile
        Coach coach = new Coach();
        coach.setId(newUser.getId());
        coach.setSpecialite(speciality);
        coach.setPays(country);
        coach.setDisponibilite(available);

        try {
            coachService.ajouter(coach);
        } catch (Exception e) {
            showGlobalError("❌  Profil coach non créé : " + e.getMessage());
            return;
        }

        onBackToLogin();
    }

    /**
     * Inserts a Google-authenticated coach user directly (no password required).
     * Mirrors UserService.insertOne() but skips password validation and hashing.
     */
    private User insertGoogleCoach(User user) throws SQLException {
        if (userService.emailExists(user.getEmail())) {
            throw new IllegalArgumentException("Email déjà utilisé.");
        }

        String req = "INSERT INTO user (email, roles, password, type, google_id, is_blocked, profile_image, totp_secret, is_totp_enabled) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        java.sql.Connection cnx = utils.DBConnection.getInstance().getCnx();
        try (java.sql.PreparedStatement ps = cnx.prepareStatement(req, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, "[\"" + user.getRole().name() + "\"]");
            ps.setNull(3, java.sql.Types.VARCHAR);   // no password
            ps.setString(4, user.getType());
            ps.setString(5, user.getGoogleId());
            ps.setBoolean(6, false);
            ps.setString(7, user.getProfileImage());
            ps.setNull(8, java.sql.Types.VARCHAR);
            ps.setBoolean(9, false);

            ps.executeUpdate();

            try (java.sql.ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) user.setId(rs.getInt(1));
                else throw new SQLException("Création échouée, pas d'ID retourné.");
            }
        }
        return user;
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
        captchaWebView.getEngine().load("http://localhost:8766/captcha");

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
                        new java.net.InetSocketAddress("localhost", 8766), 0);
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

    // ─────────────────────────── SETUP ────────────────────────────────────

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
        toggleOui.getStyleClass().remove("toggle-btn-active");
        toggleNon.getStyleClass().remove("toggle-btn-active");
        toggleNon.getStyleClass().add("toggle-btn-active");

        availabilityGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                if (oldVal != null) oldVal.setSelected(true);
                return;
            }
            toggleOui.getStyleClass().remove("toggle-btn-active");
            toggleNon.getStyleClass().remove("toggle-btn-active");
            if (newVal == toggleOui) toggleOui.getStyleClass().add("toggle-btn-active");
            else toggleNon.getStyleClass().add("toggle-btn-active");
        });
    }

    private void setupLiveValidation() {
        emailField.textProperty().addListener((obs, o, n) -> clearError(emailField, emailError));
        emailField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateEmail();
        });

        passwordField.textProperty().addListener((obs, o, n) -> {
            clearError(passwordField, passwordError);
            updateStrengthBar(n);
        });
        passwordField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validatePassword();
        });

        confirmPasswordField.textProperty().addListener((obs, o, n) -> clearError(confirmPasswordField, confirmError));
        confirmPasswordField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateConfirm();
        });

        specialityCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) clearError(null, specialityError);
        });
        countryCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) clearError(null, countryError);
        });
    }

    // ─────────────────────────── VALIDATION ───────────────────────────────

    private boolean validateEmail() {
        // Skip validation if field is locked (Google pre-fill)
        if (emailField.isDisabled()) return true;

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

    private void shake(Control field) {
        javafx.animation.TranslateTransition tt =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(60), field);
        tt.setFromX(0); tt.setByX(8); tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.play();
    }

    // ─────────────────────────── FILE CHOOSER ─────────────────────────────

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

    // ─────────────────────────── NAVIGATION ───────────────────────────────

    @FXML
    private void onBackToLogin() {
        stopCaptchaServer();
        if (googleAuthService != null) { googleAuthService.stop(); googleAuthService = null; }
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
}