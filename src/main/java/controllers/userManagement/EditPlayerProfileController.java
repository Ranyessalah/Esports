package controllers.userManagement;

import entities.userManagement.Player;
import entities.userManagement.User;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.userManagement.PlayerService;
import services.userManagement.UserService;

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

    // Error labels
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label confirmError;
    @FXML private Label paysError;
    @FXML private Label niveauError;
    @FXML private Label globalError;

    // Strength bar
    @FXML private javafx.scene.layout.HBox strengthBar;
    @FXML private Label strengthLabel;

    private User currentUser;
    private Player currentPlayer;
    private File selectedFile;
    private Runnable onSaved;

    private final UserService userService = new UserService();
    private final PlayerService playerService = new PlayerService();

    private static final List<String> PAYS_LIST = List.of(
            "Tunisie","France","Algérie","Maroc","Espagne","Allemagne",
            "Italie","Belgique","Suisse","Canada","États-Unis","Autre"
    );
    private static final List<String> NIVEAUX = List.of(
            "Débutant","Intermédiaire","Avancé","Semi-professionnel","Professionnel"
    );

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        paysCombo.getItems().addAll(PAYS_LIST);
        niveauCombo.getItems().addAll(NIVEAUX);
        setupLiveValidation();
    }

    private void setupLiveValidation() {
        emailField.textProperty().addListener((obs, o, n) -> clearError(emailField, emailError));
        emailField.focusedProperty().addListener((obs, was, is) -> { if (!is) validateEmail(); });

        passwordField.textProperty().addListener((obs, o, n) -> {
            clearError(passwordField, passwordError);
            updateStrengthBar(n);
        });
        passwordField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && !passwordField.getText().isEmpty()) validatePassword();
        });

        confirmPasswordField.textProperty().addListener((obs, o, n) -> clearError(confirmPasswordField, confirmError));
        confirmPasswordField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && !confirmPasswordField.getText().isEmpty()) validateConfirm();
        });

        paysCombo.valueProperty().addListener((obs, o, n) -> { if (n != null) clearError(null, paysError); });
        niveauCombo.valueProperty().addListener((obs, o, n) -> { if (n != null) clearError(null, niveauError); });
    }

    public void setData(User user, Player player) {
        this.currentUser = user;
        this.currentPlayer = player;
        if (user != null) emailField.setText(user.getEmail());
        if (player != null) {
            if (player.getPays() != null) paysCombo.setValue(player.getPays());
            if (player.getNiveau() != null) niveauCombo.setValue(player.getNiveau());
            if (player.isStatut()) actifRadio.setSelected(true);
            else inactifRadio.setSelected(true);
        } else {
            actifRadio.setSelected(true);
        }
    }

    public void setOnSaved(Runnable callback) { this.onSaved = callback; }

    // ── Validators ──────────────────────────────────────────────

    private boolean validateEmail() {
        String v = emailField.getText().trim();
        if (v.isEmpty()) return setError(emailField, emailError, "L'email est requis.");
        if (!v.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$"))
            return setError(emailField, emailError, "Format d'email invalide.");
        clearError(emailField, emailError);
        return true;
    }

    private boolean validatePassword() {
        String v = passwordField.getText();
        if (v.isEmpty()) { clearError(passwordField, passwordError); return true; } // optional
        if (v.length() < 6) return setError(passwordField, passwordError, "Minimum 6 caractères requis.");
        if (!v.matches(".*[A-Z].*")) return setError(passwordField, passwordError, "Au moins une majuscule requise.");
        clearError(passwordField, passwordError);
        return true;
    }

    private boolean validateConfirm() {
        String v = confirmPasswordField.getText();
        if (passwordField.getText().isEmpty() && v.isEmpty()) {
            clearError(confirmPasswordField, confirmError);
            return true;
        }
        if (!v.equals(passwordField.getText()))
            return setError(confirmPasswordField, confirmError, "Les mots de passe ne correspondent pas.");
        clearError(confirmPasswordField, confirmError);
        return true;
    }

    private void updateStrengthBar(String password) {
        if (strengthBar == null || strengthLabel == null) return;
        if (password.isEmpty()) {
            strengthBar.getChildren().forEach(n -> n.getStyleClass()
                    .removeAll("strength-weak","strength-fair","strength-good","strength-strong"));
            strengthLabel.setText("");
            return;
        }
        int score = 0;
        if (password.length() >= 6)                                                 score++;
        if (password.matches(".*[A-Z].*"))                                          score++;
        if (password.matches(".*[0-9].*"))                                          score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"))    score++;

        String[] styles = {"","strength-weak","strength-fair","strength-good","strength-strong"};
        String[] labels = {"","Faible","Moyen","Bon","Fort"};
        for (int i = 0; i < strengthBar.getChildren().size(); i++) {
            strengthBar.getChildren().get(i).getStyleClass()
                    .removeAll("strength-weak","strength-fair","strength-good","strength-strong");
            if (i < score)
                strengthBar.getChildren().get(i).getStyleClass().add(styles[score]);
        }
        strengthLabel.setText(score > 0 ? labels[score] : "");
        strengthLabel.getStyleClass()
                .removeAll("strength-weak","strength-fair","strength-good","strength-strong");
        if (score > 0) strengthLabel.getStyleClass().add(styles[score]);
    }

    // ── Helpers ─────────────────────────────────────────────────

    private boolean setError(Control field, Label label, String message) {
        label.setText("⚠  " + message);
        label.setVisible(true);
        label.setManaged(true);
        if (field != null) { field.getStyleClass().remove("input-error"); field.getStyleClass().add("input-error"); }
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
        clearError(null, paysError);
        clearError(null, niveauError);
        globalError.setVisible(false);
        globalError.setManaged(false);
    }

    private void shake(Control field) {
        javafx.animation.TranslateTransition tt =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(60), field);
        tt.setFromX(0); tt.setByX(8); tt.setCycleCount(6); tt.setAutoReverse(true);
        tt.play();
    }

    // ── Actions ─────────────────────────────────────────────────

    @FXML
    private void onChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif")
        );
        Stage stage = (Stage) emailField.getScene().getWindow();
        selectedFile = chooser.showOpenDialog(stage);
        if (selectedFile != null) fileNameLabel.setText(selectedFile.getName());
    }

    @FXML
    private void onSave() {
        clearAllErrors();
        boolean valid = true;

        valid &= validateEmail();
        valid &= validatePassword();
        valid &= validateConfirm();

        if (paysCombo.getValue() == null) {
            setError(null, paysError, "Veuillez sélectionner votre pays.");
            shake(paysCombo); valid = false;
        }
        if (niveauCombo.getValue() == null) {
            setError(null, niveauError, "Veuillez sélectionner votre niveau.");
            shake(niveauCombo); valid = false;
        }

        if (!valid) return;

        try {
            currentUser.setEmail(emailField.getText().trim());
            if (selectedFile != null) currentUser.setProfileImage(selectedFile.getAbsolutePath());
            userService.updateOne(currentUser);

            String pw = passwordField.getText();
            if (!pw.isEmpty()) userService.updatePassword(currentUser, pw);

            if (currentPlayer == null) currentPlayer = new Player();
            currentPlayer.setId(currentUser.getId());
            currentPlayer.setPays(paysCombo.getValue());
            currentPlayer.setNiveau(niveauCombo.getValue());
            currentPlayer.setStatut(actifRadio.isSelected());
            playerService.modifier(currentPlayer);

            if (onSaved != null) onSaved.run();
            closeDialog();

        } catch (IllegalArgumentException e) {
            globalError.setText("⚠  " + e.getMessage());
            globalError.setVisible(true); globalError.setManaged(true);
        } catch (SQLException e) {
            globalError.setText("❌  Erreur base de données : " + e.getMessage());
            globalError.setVisible(true); globalError.setManaged(true);
        }
    }

    @FXML private void onCancel() { closeDialog(); }

    private void closeDialog() {
        ((Stage) emailField.getScene().getWindow()).close();
    }
}