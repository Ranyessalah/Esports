package controllers.userManagement;

import entities.userManagement.Roles;
import entities.userManagement.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminProfileViewController implements Initializable {

    @FXML private Label avatarLabel;
    @FXML private Label nameLabel;
    @FXML private Label emailSubLabel;
    @FXML private Label badgeLabel;
    @FXML private Label onlineDot;
    @FXML private Label onlineLabel;
    @FXML private Label rowEmail;
    @FXML private Label rowRole;
    @FXML private Label rowStatus;
    @FXML private Label rowType;
    @FXML private Label rowTotp;

    private User user;
    private BorderPane mainRoot;   // the dashboard BorderPane
    private Node previousCenter;   // the ScrollPane with the table
    private Runnable onSavedCallback;

    /**
     * Called by AdminDashboardController before showing this screen.
     * @param user             the logged-in admin
     * @param mainRoot         the dashboard BorderPane so we can swap center
     * @param previousCenter   the table ScrollPane to restore on Back
     * @param onSavedCallback  called when profile is saved (refreshes topbar etc.)
     */
    public void init(User user, BorderPane mainRoot,
                     Node previousCenter, Runnable onSavedCallback) {
        this.user             = user;
        this.mainRoot         = mainRoot;
        this.previousCenter   = previousCenter;
        this.onSavedCallback  = onSavedCallback;
        populate();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    private void populate() {
        String initials = user.getEmail()
                .substring(0, Math.min(2, user.getEmail().length())).toUpperCase();
        avatarLabel.setText(initials);
        avatarLabel.setStyle(
                "-fx-background-color: " + roleColor(user.getRole()) + ";" +
                        "-fx-border-color: " + roleColor(user.getRole()) + "44;");

        String name = user.getEmail().contains("@")
                ? user.getEmail().split("@")[0] : user.getEmail();
        nameLabel.setText(capitalize(name));
        emailSubLabel.setText(user.getEmail());

        badgeLabel.setText(roleFr(user.getRole()));
        badgeLabel.setStyle(badgeLabel.getStyle()
                + "-fx-text-fill: " + roleColor(user.getRole()) + ";"
                + "-fx-border-color: " + roleColor(user.getRole()) + "55;"
                + "-fx-background-color: " + roleColor(user.getRole()) + "18;");

        boolean blocked = user.isBlocked();
        onlineDot.setStyle("-fx-text-fill: " + (blocked ? "#ef4444" : "#22c55e") + "; -fx-font-size: 10px;");
        onlineLabel.setText(blocked ? "Bloqué" : "En ligne");
        onlineLabel.setStyle("-fx-text-fill: " + (blocked ? "#ef4444" : "#22c55e")
                + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        rowEmail.setText(user.getEmail());
        rowRole.setText(roleFr(user.getRole()));

        rowStatus.setText(blocked ? "Bloqué" : "Actif");
        rowStatus.getStyleClass().removeAll("row-value-green", "row-value-red");
        rowStatus.getStyleClass().add(blocked ? "row-value-red" : "row-value-green");

        rowType.setText(user.getType() != null ? user.getType() : "—");

        boolean totp = user.isTotpEnabled();
        rowTotp.setText(totp ? "Activé" : "Désactivé");
        rowTotp.getStyleClass().removeAll("row-value-green", "row-value-amber");
        rowTotp.getStyleClass().add(totp ? "row-value-green" : "row-value-amber");
    }

    // ── Back: restore table in center ──
    @FXML
    private void onBack() {
        mainRoot.setCenter(previousCenter);
    }

    // ── Edit: open dialog on top, dialog only closes itself ──
    @FXML
    private void onEdit() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/userManagement/AdminProfileEdit.fxml"));
            Parent root = loader.load();

            Stage dialog = new Stage();
            dialog.initOwner(mainRoot.getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.setResizable(false);

            Scene scene = new Scene(root);
            scene.setFill(null);
            scene.getStylesheets().addAll(
                    mainRoot.getScene().getStylesheets());
            dialog.setScene(scene);

            AdminProfileEditController ctrl = loader.getController();
            ctrl.init(user, dialog, () -> {
                // After save: refresh profile screen + notify dashboard
                populate();
                if (onSavedCallback != null) onSavedCallback.run();
            });

            dialog.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Helpers ──
    private String roleFr(Roles r) {
        if (r == null) return "Inconnu";
        return switch (r) {
            case ROLE_ADMIN  -> "Administrateur";
            case ROLE_COACH  -> "Coach";
            case ROLE_PLAYER -> "Joueur";
        };
    }

    private String roleColor(Roles r) {
        if (r == null) return "#718096";
        return switch (r) {
            case ROLE_ADMIN  -> "#f59e0b";
            case ROLE_COACH  -> "#3b82f6";
            case ROLE_PLAYER -> "#22c55e";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}