package controllers;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PopupControl;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.scene.Scene;
import javafx.scene.control.Button;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainLayoutController implements Initializable {

    @FXML private VBox sidebar;
    @FXML private StackPane contentArea;
    @FXML private Label userEmailLabel;
    @FXML private HBox userProfileArea;
    @FXML private Button hamburgerBtn;
    @FXML private ImageView userAvatarHeader;
    @FXML private ImageView sidebarAvatar;
    @FXML private Label sidebarEmail;

    private boolean sidebarVisible = true;
    private Popup userDropdown;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUserDropdown();
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
        menu.getStylesheets().add(getClass().getResource("/css/clutchx-theme.css").toExternalForm());
        menu.getStyleClass().add("user-dropdown-popup");
        menu.setMinWidth(180);

        String[][] items = {
                {"⚙", "Settings"},
                {"👤", "Profile"},
                {"✉", "My Messages"},
                {"🔒", "Lock Screen"},
                {"⎋", "Logout"}
        };

        for (int i = 0; i < items.length; i++) {
            String icon = items[i][0];
            String text = items[i][1];
            Button btn = new Button(icon + "  " + text);
            btn.getStyleClass().add("dropdown-item");
            if (text.equals("Logout")) {
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
        if (userDropdown != null && userEmailLabel != null) {
            javafx.geometry.Bounds b = userEmailLabel.localToScreen(userEmailLabel.getBoundsInLocal());
            userDropdown.show(userEmailLabel, b.getMinX() - 60, b.getMaxY() + 6);
        }
    }

    private void onLogout() {
        System.out.println("Logout clicked");
        // Navigate to login screen
    }
}