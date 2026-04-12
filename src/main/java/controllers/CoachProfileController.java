package controllers;

import entities.Coach;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import services.CoachService;
import services.UserService;
import utils.PreferencesRepository;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class CoachProfileController implements Initializable {

    @FXML private ImageView profileAvatar;
    @FXML private Label displayEmail;
    @FXML private Label disponibiliteTag;
    @FXML private Label infoEmail;
    @FXML private Label infoPays;
    @FXML private Label infoSpecialite;
    @FXML private Label infoDisponibilite;

    private User currentUser;
    private Coach currentCoach;
    private final CoachService coachService = new CoachService();
    private final PreferencesRepository prefs = new PreferencesRepository();
    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = prefs.loadSession();
        applyCircularClip();
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

        if (currentUser.getProfileImage() != null && !currentUser.getProfileImage().equals("default.png")) {
            try {
                profileAvatar.setImage(new Image("file:" + currentUser.getProfileImage()));
            } catch (Exception ignored) {}
        }

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
    private void onBack() {
        try {
            // Navigate back to the dashboard or previous screen.
            // Adjust the FXML path to match your actual dashboard view.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainLayout.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) infoEmail.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(
                    getClass().getResource("/clutchx-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("ClutchX — Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onEditClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EditCoachProfile.fxml"));
            VBox dialogRoot = loader.load();

            EditCoachProfileController ctrl = loader.getController();
            ctrl.setData(currentUser, currentCoach);
            ctrl.setOnSaved(this::loadCoachData);

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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DeleteConfirmDialog.fxml"));
            VBox dialogRoot = loader.load();

            DeleteConfirmDialogController ctrl = loader.getController();
            ctrl.setEmail(currentUser.getEmail());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.setTitle("Supprimer le compte");

            Scene scene = new Scene(dialogRoot);
            scene.getStylesheets().add(getClass().getResource("/clutchx-theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.showAndWait();

            if (ctrl.isConfirmed()) {
                userService.deleteOne(currentUser);
                onLogout();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert();
        }
    }

    private void showErrorAlert() {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle("Erreur");
        error.setHeaderText(null);
        error.setContentText("Erreur lors de la suppression du compte.");
        error.showAndWait();
    }

    private void onLogout() {
        try {
            this.prefs.clearSession();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) infoEmail.getScene().getWindow();
            Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(
                    getClass().getResource("/clutchx-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("ClutchX — Connexion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void applyCircularClip() {
        double radius = 36; // fitWidth / 2 = 72 / 2
        Circle clip = new Circle(radius, radius, radius);
        profileAvatar.setClip(clip);
    }
}