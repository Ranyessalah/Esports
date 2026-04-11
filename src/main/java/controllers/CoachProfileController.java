package controllers;

import entities.Coach;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import services.CoachService;
import services.UserService;
import utils.PreferencesRepository;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class CoachProfileController implements Initializable {

    @FXML private ImageView profileAvatar;
    @FXML private Label displayEmail;
    @FXML private Label disponibiliteTag;
    @FXML private Label infoEmail;
    @FXML private Label infoPays;
    @FXML private Label infoSpecialite;
    @FXML private Label infoDisponibilite;
//    @FXML private Label info2fa;
//    @FXML private Label twoFaWarning;
//    @FXML private Button twoFaBtn;

    private User currentUser;
    private Coach currentCoach;
    private final CoachService coachService = new CoachService();
    private final PreferencesRepository prefs = new PreferencesRepository();
    private final UserService userService=new UserService();
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = prefs.loadSession();
        if (currentUser != null) {
            loadCoachData();
        }
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadCoachData();
    }

    private void loadCoachData() {
        try {
            // Load coach-specific data
            currentCoach = coachService.getById(currentUser.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        populateView();
    }

    private void populateView() {
        if (currentUser == null) return;

        infoEmail.setText(currentUser.getEmail());
        displayEmail.setText(currentUser.getEmail());

        // Profile image
        if (currentUser.getProfileImage() != null && !currentUser.getProfileImage().equals("default.png")) {
            try {
                profileAvatar.setImage(new Image("file:" + currentUser.getProfileImage()));
            } catch (Exception ignored) {}
        }

        // 2FA
        boolean totpOn = currentUser.isTotpEnabled();
//        info2fa.setText(totpOn ? "✅ Activée" : "❌ Non activée");
//        twoFaWarning.setVisible(!totpOn);
//        twoFaWarning.setManaged(!totpOn);
//        twoFaBtn.setText(totpOn ? "Désactiver La 2FA" : "Activer La 2FA");

        if (currentCoach != null) {
            infoPays.setText(currentCoach.getPays() != null ? currentCoach.getPays() : "—");
            infoSpecialite.setText(currentCoach.getSpecialite() != null ? currentCoach.getSpecialite() : "—");
            boolean dispo = currentCoach.isDisponibilite();
            infoDisponibilite.setText(dispo ? "Disponible" : "Non disponible");
            disponibiliteTag.setText(dispo ? "● Disponible" : "● Non disponible");
            disponibiliteTag.setStyle(dispo
                    ? "-fx-text-fill: #48bb78;"
                    : "-fx-text-fill: #fc8181;");
        }
    }

    @FXML
    private void onEditClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EditCoachProfile.fxml"));
            VBox dialogRoot = loader.load();

            EditCoachProfileController ctrl = loader.getController();
            ctrl.setData(currentUser, currentCoach);
            ctrl.setOnSaved(this::loadCoachData); // refresh after save

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Modifier le profil");
            Scene scene = new Scene(dialogRoot);
            scene.getStylesheets().add(getClass().getResource("/clutchx-theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void onDeleteAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer votre compte ?");
        alert.setContentText("Cette action est irréversible. Voulez-vous continuer ?");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {

                userService.deleteOne(currentUser);
                onLogout();

            } catch (Exception e) {
                e.printStackTrace();

                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Erreur");
                error.setHeaderText(null);
                error.setContentText("Erreur lors de la suppression du compte.");
                error.showAndWait();
            }
        }
    }
    private void onLogout() {
        try {
            this.prefs.clearSession();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) infoEmail.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(
                    root, stage.getWidth(), stage.getHeight()
            );
            scene.getStylesheets().add(
                    getClass().getResource("/clutchx-theme.css").toExternalForm()
            );
            stage.setScene(scene);
            stage.setTitle("ClutchX — Connexion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//    @FXML
//    private void onToggle2FA() {
//        // Navigate to 2FA setup screen or toggle
//        // Implementation depends on existing 2FA controller
//    }
}