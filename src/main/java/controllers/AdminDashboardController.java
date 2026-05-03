package controllers;

import controllers.userManagement.AdminProfileViewController;
import controllers.userManagement.AdminUsersController;
import entities.userManagement.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import utils.PreferencesRepository;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private StackPane  contentArea;
    @FXML private Label      adminEmailLabel;
    @FXML private Label      adminAvatarLabel;
    @FXML private TextField  globalSearch;
    @FXML private StackPane  userMenuPane;



    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnEquipes;
    @FXML private Button btnMatchs;
    @FXML private Button btnStats;
    @FXML private Label  topBarTitle;




    private Popup   userDropdown;
    private boolean dropdownOpen = false;
    private User    currentAdmin;

    private final PreferencesRepository prefs = new PreferencesRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rootPane.setUserData(this);
        loadCurrentAdmin();
        onDashboard(); // default view
    }

    // ════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════
    private void setActiveBtn(Button active, String title) {
        for (Button b : new Button[]{btnDashboard, btnUsers, btnEquipes, btnMatchs/*, btnStats*/}) {
            b.getStyleClass().remove("active");
            b.setStyle("");
        }
        active.getStyleClass().add("active");
        if (topBarTitle != null) topBarTitle.setText(title);
    }
    @FXML
    public void onDashboard() {
        setActiveBtn(btnDashboard, "Dashboard");
        loadContent("/matchManagement/stats.fxml");
    }

    @FXML
    public void onUsers() {
        setActiveBtn(btnUsers, "Utilisateurs");
        loadContentWithController("/userManagement/AdminUsersView.fxml");
    }

    @FXML
    public void onEquipes() {
        setActiveBtn(btnEquipes, "Équipes");
        loadContent("/matchManagement/equipeIndex.fxml");
    }

    @FXML
    public void onMatchs() {
        setActiveBtn(btnMatchs, "Matchs");
        loadContent("/matchManagement/matchIndex.fxml");
    }

    @FXML
    public void onStats() {
        setActiveBtn(btnStats, "Statistiques");
        loadContent("/matchManagement/stats.fxml");
    }

    // Called from FXML logout button directly
    @FXML
    public void onLogoutBtn() {
        onLogout();
    }
    // ════════════════════════════════════════
    //  CONTENT LOADING
    // ════════════════════════════════════════

    public void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            e.printStackTrace();
            Label err = new Label("Erreur : " + fxmlPath);
            err.setStyle("-fx-text-fill: red;");
            contentArea.getChildren().setAll(err);
        }
    }

    public void loadNode(Node node) {
        contentArea.getChildren().setAll(node);
    }

    private void loadContentWithController(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();

            // Pass layout reference to sub-controllers that need it
            Object controller = loader.getController();
            if (controller instanceof AdminUsersController) {
                ((AdminUsersController) controller).setLayoutController(this);
            }

            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════
    //  ADMIN SESSION
    // ════════════════════════════════════════

    private void loadCurrentAdmin() {
        try {
            int adminId = prefs.getSessionUserId();
            services.userManagement.UserService svc = new services.userManagement.UserService();
            currentAdmin = svc.selectAll().stream()
                    .filter(u -> u.getId() == adminId)
                    .findFirst().orElse(null);
            if (currentAdmin != null) setAdminEmail(currentAdmin.getEmail());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAdminEmail(String email) {
        if (adminEmailLabel != null) adminEmailLabel.setText(email);
        if (adminAvatarLabel != null && email != null && !email.isEmpty())
            adminAvatarLabel.setText(email.substring(0, Math.min(2, email.length())).toUpperCase());
    }

    // ════════════════════════════════════════
    //  USER DROPDOWN
    // ════════════════════════════════════════

    @FXML
    private void onToggleUserMenu() {
        if (dropdownOpen) {
            userDropdown.hide();
            dropdownOpen = false;
            return;
        }
        if (userDropdown == null) userDropdown = buildUserDropdown();
        javafx.geometry.Bounds b = userMenuPane.localToScreen(userMenuPane.getBoundsInLocal());
        userDropdown.show(userMenuPane, b.getMaxX() - 200, b.getMaxY() + 6);
        dropdownOpen = true;
        userDropdown.setAutoHide(true);
        userDropdown.setOnHidden(e -> dropdownOpen = false);
    }

    private Popup buildUserDropdown() {
        Popup popup = new Popup();
        popup.setAutoFix(true);

        VBox menu = new VBox(0);
        menu.getStyleClass().add("admin-user-dropdown");
        menu.setPrefWidth(200);

        Label emailLbl = new Label(adminEmailLabel.getText());
        emailLbl.getStyleClass().add("dropdown-email-label");

        javafx.scene.control.Separator sep1 = new javafx.scene.control.Separator();
        sep1.setStyle("-fx-pref-height:1; -fx-background-color: rgba(255,255,255,0.07);");

        javafx.scene.control.Button profileBtn = new javafx.scene.control.Button("👤  Mon profil");
        profileBtn.getStyleClass().add("dropdown-item-btn");
        profileBtn.setMaxWidth(Double.MAX_VALUE);
        profileBtn.setOnAction(e -> {
            popup.hide();
            dropdownOpen = false;
            openAdminProfile();
        });

        javafx.scene.control.Button settingsBtn = new javafx.scene.control.Button("⚙  Paramètres");
        settingsBtn.getStyleClass().add("dropdown-item-btn");
        settingsBtn.setMaxWidth(Double.MAX_VALUE);
        settingsBtn.setOnAction(e -> { popup.hide(); dropdownOpen = false; });

        javafx.scene.control.Separator sep2 = new javafx.scene.control.Separator();
        sep2.setStyle("-fx-pref-height:1; -fx-background-color: rgba(255,255,255,0.07);");

        javafx.scene.control.Button logoutBtn = new javafx.scene.control.Button("⏻  Se déconnecter");
        logoutBtn.getStyleClass().add("dropdown-logout-btn");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> { popup.hide(); dropdownOpen = false; onLogout(); });

        menu.getChildren().addAll(emailLbl, sep1, profileBtn, settingsBtn, sep2, logoutBtn);

        if (userMenuPane.getScene() != null)
            menu.getStylesheets().addAll(userMenuPane.getScene().getStylesheets());

        popup.getContent().add(menu);
        return popup;
    }

    private void openAdminProfile() {
        if (currentAdmin == null) return;
        try {
            Node previousContent = contentArea.getChildren().isEmpty()
                    ? null : contentArea.getChildren().get(0);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/userManagement/AdminProfileView.fxml"));
            Node profileScreen = loader.load();

            AdminProfileViewController ctrl = loader.getController();
            ctrl.init(currentAdmin, rootPane, previousContent, () -> {
                prefs.updateEmail(currentAdmin.getEmail());
                setAdminEmail(currentAdmin.getEmail());
            });

            contentArea.getChildren().setAll(profileScreen);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Déconnexion");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vous déconnecter ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                prefs.clearSession();
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/userManagement/Login.fxml"));
                    javafx.scene.Parent root = loader.load();
                    javafx.stage.Stage stage =
                            (javafx.stage.Stage) userMenuPane.getScene().getWindow();
                    javafx.scene.Scene scene = new javafx.scene.Scene(
                            root, stage.getWidth(), stage.getHeight());
                    scene.getStylesheets().add(
                            getClass().getResource("/userManagement/clutchx-theme.css")
                                    .toExternalForm());
                    stage.setScene(scene);
                    stage.setTitle("ClutchX — Connexion");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}