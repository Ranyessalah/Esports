package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PopupControl;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import utils.PreferencesRepository;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainLayoutController implements Initializable {

    // ── FXML injections ──
    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private HBox logoZone;
    @FXML private Label logoTextFull;
    @FXML private Label logoTextAccent;
    @FXML private HBox sidebarUserInfo;
    @FXML private Label sidebarEmail;
    @FXML private StackPane contentArea;
    @FXML private Label userEmailLabel;
    @FXML private HBox userProfileArea;
    @FXML private Button hamburgerBtn;

    // Nav labels (hidden when sidebar is collapsed)
    @FXML private Label labelDashboard;
    @FXML private Label labelComponents;
    @FXML private Label labelFormComponents;
    @FXML private Label labelCoach;
    @FXML private Label chevronComponents;

    // Avatars
    @FXML private ImageView userAvatarHeader;
    @FXML private ImageView sidebarAvatar;

    private boolean sidebarExpanded = true;
    private static final double SIDEBAR_FULL = 250;
    private static final double SIDEBAR_MINI = 64;

    private Popup userDropdown;
    private final PreferencesRepository prefs=new PreferencesRepository();
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUserDropdown();
        setupResponsiveListener();
        onDashboardClick();
    }

    // ── Responsive: auto-collapse sidebar on small windows ──
    private void setupResponsiveListener() {
        rootPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            double w = newVal.doubleValue();
            if (w < 800 && sidebarExpanded) {
                collapseSidebar();
            } else if (w >= 800 && !sidebarExpanded) {
                expandSidebar();
            }
        });
    }

    // ── Toggle sidebar ──
    @FXML
    private void toggleSidebar() {
        if (sidebarExpanded) collapseSidebar();
        else expandSidebar();
    }

    private void collapseSidebar() {
        sidebarExpanded = false;
        sidebar.setPrefWidth(SIDEBAR_MINI);
        sidebar.setMinWidth(SIDEBAR_MINI);
        sidebar.setMaxWidth(SIDEBAR_MINI);
        logoZone.setPrefWidth(SIDEBAR_MINI);
        logoZone.setMaxWidth(SIDEBAR_MINI);

        // Hide text labels
        setLabelsVisible(false);
        logoTextFull.setVisible(false);
        logoTextFull.setManaged(false);
        logoTextAccent.setVisible(false);
        logoTextAccent.setManaged(false);
        sidebarEmail.setVisible(false);
        sidebarEmail.setManaged(false);
        userEmailLabel.setVisible(false);
        userEmailLabel.setManaged(false);
    }

    private void expandSidebar() {
        sidebarExpanded = true;
        sidebar.setPrefWidth(SIDEBAR_FULL);
        sidebar.setMinWidth(SIDEBAR_FULL);
        sidebar.setMaxWidth(SIDEBAR_FULL);
        logoZone.setPrefWidth(SIDEBAR_FULL);
        logoZone.setMaxWidth(SIDEBAR_FULL);

        // Show text labels
        setLabelsVisible(true);
        logoTextFull.setVisible(true);
        logoTextFull.setManaged(true);
        logoTextAccent.setVisible(true);
        logoTextAccent.setManaged(true);
        sidebarEmail.setVisible(true);
        sidebarEmail.setManaged(true);
        userEmailLabel.setVisible(true);
        userEmailLabel.setManaged(true);
    }

    private void setLabelsVisible(boolean visible) {
        for (Label l : new Label[]{
                labelDashboard, labelComponents,
                labelFormComponents, labelCoach, chevronComponents
        }) {
            if (l != null) {
                l.setVisible(visible);
                l.setManaged(visible);
            }
        }
    }

    // ── Navigation ──
    @FXML public void onDashboardClick() {
        loadContent(null, "Dashboard");
    }

    @FXML public void onComponentsClick() {
        loadContent(null, "Components");
    }

    @FXML public void onFormComponentsClick() {
        loadContent(null, "Form Components");
    }

    @FXML public void onCoachSignupClick() {
        loadContent("/CoachSignup.fxml", null);
    }

    @FXML public void onCloseTab() {
        // Close current tab or navigate back
    }

    private void loadContent(String fxmlPath, String placeholderText) {
        contentArea.getChildren().clear();
        if (fxmlPath != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Node node = loader.load();
                contentArea.getChildren().add(node);
            } catch (IOException e) {
                e.printStackTrace();
                Label err = new Label("Erreur : " + fxmlPath);
                err.setStyle("-fx-text-fill: red;");
                contentArea.getChildren().add(err);
            }
        } else if (placeholderText != null) {
            Label lbl = new Label(placeholderText);
            lbl.setStyle("-fx-text-fill: #718096; -fx-font-size: 18px;");
            contentArea.getChildren().add(lbl);
        }
    }

    // ── Set logged-in user info ──
    public void setUserEmail(String email) {
        if (userEmailLabel != null) userEmailLabel.setText(email);
        if (sidebarEmail != null) sidebarEmail.setText(email);
    }

    // ── User dropdown ──
    private void setupUserDropdown() {
        if (userProfileArea != null) {
            userProfileArea.setOnMouseClicked(e -> showUserDropdown());
        }
        buildDropdown();
    }

    private void buildDropdown() {
        userDropdown = new Popup();
        userDropdown.setAutoHide(true);

        VBox menu = new VBox(0);
        menu.getStylesheets().add(
                getClass().getResource("/clutchx-theme.css").toExternalForm()
        );
        menu.getStyleClass().add("user-dropdown-popup");
        menu.setMinWidth(190);

        String[][] items = {
                {"⚙", "Settings"},
                {"👤", "Profile"},
                {"✉", "My Messages"},
                {"🔒", "Lock Screen"},
                {"⎋", "Logout"}
        };

        for (String[] item : items) {
            Button btn = new Button(item[0] + "  " + item[1]);
            btn.getStyleClass().add("dropdown-item");
            if (item[1].equals("Logout")) {
                btn.getStyleClass().add("dropdown-item-danger");
                btn.setOnAction(e -> { userDropdown.hide(); onLogout(); });
            } else {
                btn.setOnAction(e -> userDropdown.hide());
            }
            menu.getChildren().add(btn);
        }

        userDropdown.getContent().add(menu);
    }

    private void showUserDropdown() {
        if (userDropdown != null && userProfileArea != null) {
            javafx.geometry.Bounds b =
                    userProfileArea.localToScreen(userProfileArea.getBoundsInLocal());
            userDropdown.show(userProfileArea, b.getMinX(), b.getMaxY() + 4);
        }
    }

    private void onLogout() {
        try {
            this.prefs.clearSession();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) rootPane.getScene().getWindow();
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
}