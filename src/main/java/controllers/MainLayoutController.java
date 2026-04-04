package controllers;

<<<<<<< HEAD

=======
>>>>>>> fa93614ad86cdc72a85100622304ce9f69e3e54a
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
<<<<<<< HEAD
=======
import javafx.scene.control.Button;
>>>>>>> fa93614ad86cdc72a85100622304ce9f69e3e54a
import javafx.scene.control.Label;
import javafx.scene.control.PopupControl;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Popup;
<<<<<<< HEAD
import javafx.scene.Scene;
import javafx.scene.control.Button;
=======
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
>>>>>>> fa93614ad86cdc72a85100622304ce9f69e3e54a

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainLayoutController implements Initializable {

<<<<<<< HEAD
    @FXML private VBox sidebar;
=======
    // ── FXML injections ──
    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private HBox logoZone;
    @FXML private Label logoTextFull;
    @FXML private Label logoTextAccent;
    @FXML private HBox sidebarUserInfo;
    @FXML private Label sidebarEmail;
>>>>>>> fa93614ad86cdc72a85100622304ce9f69e3e54a
    @FXML private StackPane contentArea;
    @FXML private Label userEmailLabel;
    @FXML private HBox userProfileArea;
    @FXML private Button hamburgerBtn;
<<<<<<< HEAD
    @FXML private ImageView userAvatarHeader;
    @FXML private ImageView sidebarAvatar;
    @FXML private Label sidebarEmail;

    private boolean sidebarVisible = true;
=======

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

>>>>>>> fa93614ad86cdc72a85100622304ce9f69e3e54a
    private Popup userDropdown;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUserDropdown();
<<<<<<< HEAD
        // Load dashboard by default
        onDashboardClick();
    }

    // ── Toggle sidebar visibility ──
    @FXML
    private void toggleSidebar() {
        sidebarVisible = !sidebarVisible;
        sidebar.setVisible(sidebarVisible);
        sidebar.setManaged(sidebarVisible);
    }

    // ── Navigation handlers ──
    @FXML
    private void onDashboardClick() {
        clearActiveNav();
        contentArea.getChildren().clear();
        Label placeholder = new Label("Dashboard — Bienvenue sur ClutchX");
        placeholder.setStyle("-fx-text-fill: #718096; -fx-font-size: 18px;");
        contentArea.getChildren().add(placeholder);
    }

    @FXML
    private void onComponentsClick() {
        clearActiveNav();
        contentArea.getChildren().clear();
        Label placeholder = new Label("Components");
        placeholder.setStyle("-fx-text-fill: #718096; -fx-font-size: 18px;");
        contentArea.getChildren().add(placeholder);
    }

    @FXML
    private void onFormComponentsClick() {
        clearActiveNav();
        contentArea.getChildren().clear();
        Label placeholder = new Label("Form Components");
        placeholder.setStyle("-fx-text-fill: #718096; -fx-font-size: 18px;");
        contentArea.getChildren().add(placeholder);
    }

    @FXML
    private void onCoachSignupClick() {
        clearActiveNav();
        contentArea.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CoachSignup.fxml"));
            Node signupView = loader.load();
            contentArea.getChildren().add(signupView);
        } catch (IOException e) {
            e.printStackTrace();
            Label err = new Label("Erreur de chargement du formulaire.");
            err.setStyle("-fx-text-fill: red;");
            contentArea.getChildren().add(err);
        }
    }

    private void clearActiveNav() {
        // Update active state styling on nav items if needed
=======
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
>>>>>>> fa93614ad86cdc72a85100622304ce9f69e3e54a
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
<<<<<<< HEAD
        menu.getStylesheets().add(getClass().getResource("/css/clutchx-theme.css").toExternalForm());
        menu.getStyleClass().add("user-dropdown-popup");
        menu.setMinWidth(180);
=======
        menu.getStylesheets().add(
                getClass().getResource("/clutchx-theme.css").toExternalForm()
        );
        menu.getStyleClass().add("user-dropdown-popup");
        menu.setMinWidth(190);
>>>>>>> fa93614ad86cdc72a85100622304ce9f69e3e54a

        String[][] items = {
                {"⚙", "Settings"},
                {"👤", "Profile"},
                {"✉", "My Messages"},
                {"🔒", "Lock Screen"},
                {"⎋", "Logout"}
        };

<<<<<<< HEAD
        for (int i = 0; i < items.length; i++) {
            String icon = items[i][0];
            String text = items[i][1];
            Button btn = new Button(icon + "  " + text);
            btn.getStyleClass().add("dropdown-item");
            if (text.equals("Logout")) {
=======
        for (String[] item : items) {
            Button btn = new Button(item[0] + "  " + item[1]);
            btn.getStyleClass().add("dropdown-item");
            if (item[1].equals("Logout")) {
>>>>>>> fa93614ad86cdc72a85100622304ce9f69e3e54a
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
<<<<<<< HEAD
        if (userDropdown != null && userEmailLabel != null) {
            javafx.geometry.Bounds b = userEmailLabel.localToScreen(userEmailLabel.getBoundsInLocal());
            userDropdown.show(userEmailLabel, b.getMinX() - 60, b.getMaxY() + 6);
=======
        if (userDropdown != null && userProfileArea != null) {
            javafx.geometry.Bounds b =
                    userProfileArea.localToScreen(userProfileArea.getBoundsInLocal());
            userDropdown.show(userProfileArea, b.getMinX(), b.getMaxY() + 4);
>>>>>>> fa93614ad86cdc72a85100622304ce9f69e3e54a
        }
    }

    private void onLogout() {
<<<<<<< HEAD
        System.out.println("Logout clicked");
        // Navigate to login screen
=======
        try {
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
>>>>>>> fa93614ad86cdc72a85100622304ce9f69e3e54a
    }
}