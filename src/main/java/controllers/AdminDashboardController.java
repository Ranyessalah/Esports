package controllers;

import entities.Roles;
import entities.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import services.UserService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminDashboardController implements Initializable {

    // ── Top bar ──
    @FXML private Label adminEmailLabel;
    @FXML private TextField globalSearch;

    // ── Stat cards ──
    @FXML private Label totalUsersLabel;
    @FXML private Label totalCoachesLabel;
    @FXML private Label totalPlayersLabel;
    @FXML private Label totalAdminsLabel;

    // ── Filters ──
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> sortByFilter;
    @FXML private ComboBox<String> orderFilter;
    @FXML private Label resultCountLabel;

    // ── Table ──
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colUser;
    @FXML private TableColumn<User, Roles>  colRole;
    @FXML private TableColumn<User, String>  colEmail;
    @FXML private TableColumn<User, Boolean> colStatus;
    @FXML private TableColumn<User, Void>    colActions;

    @FXML private StackPane userMenuPane;
    @FXML private Label adminAvatarLabel;
    private Popup userDropdown;
    private boolean dropdownOpen = false;
    private User currentAdmin;
    private final UserService userService = new UserService();
    private ObservableList<User> allUsers = FXCollections.observableArrayList();

    // ── Called from LoginController after login ──
    public void setAdminEmail(String email) {
        if (adminEmailLabel != null) adminEmailLabel.setText(email);
        if (adminAvatarLabel != null && email != null && !email.isEmpty()) {
            adminAvatarLabel.setText(email.substring(0, Math.min(2, email.length())).toUpperCase());
        }
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilters();
        setupTable();
        loadUsers();
    }

    // ════════════════════════════════════════
    //  SETUP
    // ════════════════════════════════════════

    private void setupFilters() {
        roleFilter.setItems(FXCollections.observableArrayList(
                "Tous les rôles", "Admin", "Coach", "Joueur"
        ));
        roleFilter.setValue("Tous les rôles");

        statusFilter.setItems(FXCollections.observableArrayList(
                "Tous les statuts", "Actif", "Bloqué"
        ));
        statusFilter.setValue("Tous les statuts");

        sortByFilter.setItems(FXCollections.observableArrayList(
                "ID", "Email", "Rôle"
        ));
        sortByFilter.setValue("ID");

        orderFilter.setItems(FXCollections.observableArrayList(
                "Décroissant", "Croissant"
        ));
        orderFilter.setValue("Décroissant");
    }

    private void setupTable() {
        // ID column
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                setText(empty || id == null ? null : "#" + id);
                setStyle("-fx-text-fill: #c0c0e0; -fx-font-weight: bold;");
            }
        });

        // User column — avatar + name + id
        colUser.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUser.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String email, boolean empty) {
                super.updateItem(email, empty);
                if (empty || email == null) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());

                // Avatar circle with initials
                String initials = email.substring(0, Math.min(2, email.length())).toUpperCase();
                Label avatarLabel = new Label(initials);
                avatarLabel.setStyle(
                        "-fx-background-color: " + roleColor(u.getRole()) + ";" +
                                "-fx-background-radius: 50%;" +
                                "-fx-min-width: 34; -fx-max-width: 34;" +
                                "-fx-min-height: 34; -fx-max-height: 34;" +
                                "-fx-alignment: CENTER;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 12px;");

                // Name + ID sub-label
                String displayName = email.contains("@") ? email.split("@")[0] : email;
                Label nameLabel = new Label(capitalize(displayName));
                nameLabel.setStyle("-fx-text-fill: #e0e0ff; -fx-font-weight: bold; -fx-font-size: 13px;");
                Label idSub = new Label("ID: " + u.getId());
                idSub.setStyle("-fx-text-fill: #8080a0; -fx-font-size: 11px;");

                javafx.scene.layout.VBox vb = new javafx.scene.layout.VBox(1, nameLabel, idSub);
                HBox hb = new HBox(10, avatarLabel, vb);
                hb.setAlignment(Pos.CENTER_LEFT);
                setGraphic(hb);
                setText(null);
            }
        });

        // Role column — colored badge
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Roles role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());
                String labelText = roleFr(u.getRole());
                String color = roleColor(u.getRole());
                Label badge = new Label(labelText);
                badge.setStyle(
                        "-fx-background-color: " + color + "22;" +
                                "-fx-border-color: " + color + "66;" +
                                "-fx-border-width: 1;" +
                                "-fx-border-radius: 4;" +
                                "-fx-background-radius: 4;" +
                                "-fx-text-fill: " + color + ";" +
                                "-fx-font-size: 11px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 3 10;");
                setGraphic(badge);
                setText(null);
            }
        });

        // Email column
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String email, boolean empty) {
                super.updateItem(email, empty);
                setText(empty ? null : email);
                setStyle("-fx-text-fill: #c0c0e0; -fx-font-size: 12px;");
            }
        });

        // Status column — Actif / Bloqué badge
        colStatus.setCellValueFactory(new PropertyValueFactory<>("blocked"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean blocked, boolean empty) {
                super.updateItem(blocked, empty);
                if (empty || blocked == null) { setGraphic(null); return; }
                boolean isBlocked = blocked;
                Label badge = new Label(isBlocked ? "Bloqué" : "Actif");
                badge.setStyle(isBlocked
                        ? "-fx-background-color: #ef444422; -fx-border-color: #ef444466; -fx-border-width:1; -fx-border-radius:4; -fx-background-radius:4; -fx-text-fill:#ef4444; -fx-font-size:11px; -fx-font-weight:bold; -fx-padding:3 10;"
                        : "-fx-background-color: #22c55e22; -fx-border-color: #22c55e66; -fx-border-width:1; -fx-border-radius:4; -fx-background-radius:4; -fx-text-fill:#22c55e; -fx-font-size:11px; -fx-font-weight:bold; -fx-padding:3 10;");
                setGraphic(badge);
                setText(null);
            }
        });

        // Actions column — edit + block/unblock
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn  = new Button("✏");
            private final Button blockBtn = new Button("🔒");

            {
                editBtn.setStyle(
                        "-fx-background-color: #3b82f622; -fx-border-color: #3b82f666;" +
                                "-fx-border-width:1; -fx-border-radius:4; -fx-background-radius:4;" +
                                "-fx-text-fill:#3b82f6; -fx-cursor:hand; -fx-font-size:13px; -fx-padding:4 8;");
                blockBtn.setStyle(
                        "-fx-background-color: #f59e0b22; -fx-border-color: #f59e0b66;" +
                                "-fx-border-width:1; -fx-border-radius:4; -fx-background-radius:4;" +
                                "-fx-text-fill:#f59e0b; -fx-cursor:hand; -fx-font-size:13px; -fx-padding:4 8;");

                editBtn.setOnAction(e -> onEditUser(getTableView().getItems().get(getIndex())));
                blockBtn.setOnAction(e -> onToggleBlock(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());

                // Show delete btn only if not admin #1
                HBox box = new HBox(6, editBtn, blockBtn);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
    }

    // ════════════════════════════════════════
    //  DATA
    // ════════════════════════════════════════

    private void loadUsers() {
        try {
            List<User> users = userService.selectAll();

            utils.PreferencesRepository prefs = new utils.PreferencesRepository();
            int currentAdminId = prefs.getSessionUserId();

            // ✅ récupérer admin connecté
            currentAdmin = users.stream()
                    .filter(u -> u.getId() == currentAdminId)
                    .findFirst()
                    .orElse(null);

            if (currentAdmin != null) {
                setAdminEmail(currentAdmin.getEmail());
            }

            // ✅ afficher uniquement Coach + Player
            List<User> filteredUsers = users.stream()
                    .filter(user -> user.getRole() != Roles.ROLE_ADMIN)
                    .collect(Collectors.toList());

            allUsers.setAll(filteredUsers);

            applyFilters();
            updateStats(users); // garder stats globales

        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les utilisateurs : " + e.getMessage());
        }
    }
    private void updateStats(List<User> users) {
        totalUsersLabel.setText(String.valueOf(users.size()));
        totalCoachesLabel.setText(String.valueOf(
                users.stream().filter(u -> u.getRole() == Roles.ROLE_COACH).count()));
        totalPlayersLabel.setText(String.valueOf(
                users.stream().filter(u -> u.getRole() == Roles.ROLE_PLAYER).count()));
        totalAdminsLabel.setText(String.valueOf(
                users.stream().filter(u -> u.getRole() == Roles.ROLE_ADMIN).count()));
    }

    // ════════════════════════════════════════
    //  FILTERS
    // ════════════════════════════════════════

    @FXML
    private void onApplyFilters() {
        applyFilters();
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        roleFilter.setValue("Tous les rôles");
        statusFilter.setValue("Tous les statuts");
        sortByFilter.setValue("ID");
        orderFilter.setValue("Décroissant");
        applyFilters();
    }

    private void applyFilters() {
        String search  = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String role    = roleFilter.getValue();
        String status  = statusFilter.getValue();
        String sortBy  = sortByFilter.getValue();
        String order   = orderFilter.getValue();

        List<User> filtered = allUsers.stream()
                .filter(u -> {
                    if (!search.isEmpty()) {
                        return u.getEmail().toLowerCase().contains(search) ||
                                roleFr(u.getRole()).toLowerCase().contains(search);
                    }
                    return true;
                })
                .filter(u -> {
                    if (role == null || role.equals("Tous les rôles")) return true;
                    return roleFr(u.getRole()).equalsIgnoreCase(role);
                })
                .filter(u -> {
                    if (status == null || status.equals("Tous les statuts")) return true;
                    if (status.equals("Actif"))  return !u.isBlocked();
                    if (status.equals("Bloqué")) return u.isBlocked();
                    return true;
                })
                .sorted((a, b) -> {
                    int cmp;
                    if ("Email".equals(sortBy)) {
                        cmp = a.getEmail().compareToIgnoreCase(b.getEmail());
                    } else if ("Rôle".equals(sortBy)) {
                        cmp = roleFr(a.getRole()).compareToIgnoreCase(roleFr(b.getRole()));
                    } else {
                        cmp = Integer.compare(a.getId(), b.getId());
                    }
                    return "Croissant".equals(order) ? cmp : -cmp;
                })
                .collect(Collectors.toList());

        usersTable.setItems(FXCollections.observableArrayList(filtered));
        resultCountLabel.setText(filtered.size() + " utilisateur" + (filtered.size() > 1 ? "s" : "") + " trouvé" + (filtered.size() > 1 ? "s" : ""));
    }

    // ════════════════════════════════════════
    //  ACTIONS
    // ════════════════════════════════════════

    private void onEditUser(User user) {
        // Bloquer la modification de l'admin #1
        if (user.getRole() == Roles.ROLE_ADMIN) {
            showInfo("Non autorisé", "Impossible de modifier un administrateur depuis cette interface.");
            return;
        }
        Stage owner = (Stage) usersTable.getScene().getWindow();
        new ProfileDialog().show(owner, user, false, updatedUser -> {
            loadUsers(); // rafraîchit la table après sauvegarde
        });
    }

    private void onToggleBlock(User user) {
        String action = user.isBlocked() ? "débloquer" : "bloquer";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous " + action + " l'utilisateur " + user.getEmail() + " ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    user.setBlocked(!user.isBlocked());
                    userService.updateOne(user);
                    loadUsers(); // refresh
                } catch (SQLException e) {
                    showError("Erreur", "Impossible de modifier le statut : " + e.getMessage());
                }
            }
        });
    }

    @FXML private void onDashboard() { /* already here */ }

    @FXML private void onUsers() { loadUsers(); }

    // ════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════

    private String roleFr(Roles role) {
        if (role == null) return "Inconnu";
        return switch (role) {
            case ROLE_ADMIN  -> "Admin";
            case ROLE_COACH  -> "Coach";
            case ROLE_PLAYER -> "Joueur";
            default          -> role.name();
        };
    }

    private String roleColor(Roles role) {
        if (role == null) return "#718096";
        return switch (role) {
            case ROLE_ADMIN  -> "#f59e0b";
            case ROLE_COACH  -> "#3b82f6";
            case ROLE_PLAYER -> "#22c55e";
            default          -> "#a855f7";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    @FXML
    private void onToggleUserMenu() {
        if (dropdownOpen) {
            userDropdown.hide();
            dropdownOpen = false;
            return;
        }
        if (userDropdown == null) {
            userDropdown = buildUserDropdown();
        }
        javafx.geometry.Bounds bounds = userMenuPane.localToScreen(userMenuPane.getBoundsInLocal());
        userDropdown.show(userMenuPane,
                bounds.getMaxX() - 200, // aligne à droite
                bounds.getMaxY() + 6);
        dropdownOpen = true;

        // Ferme si on clique ailleurs
        userDropdown.setAutoHide(true);
        userDropdown.setOnHidden(e -> dropdownOpen = false);
    }

    private Popup buildUserDropdown() {
        Popup popup = new Popup();
        popup.setAutoFix(true);

        VBox menu = new VBox(0);
        menu.getStyleClass().add("admin-user-dropdown");
        menu.setPrefWidth(200);

        // Email en en-tête
        Label emailLbl = new Label(adminEmailLabel.getText());
        emailLbl.getStyleClass().add("dropdown-email-label");

        Separator sep1 = new Separator();
        sep1.getStyleClass().add("dropdown-separator");
        sep1.setStyle("-fx-padding: 0; -fx-pref-height:1; -fx-background-color: rgba(255,255,255,0.07);");

        // Items
        Button profileBtn = new Button("👤  Mon profil");
        profileBtn.getStyleClass().add("dropdown-item-btn");
        profileBtn.setMaxWidth(Double.MAX_VALUE);
        profileBtn.setOnAction(e -> {
            popup.hide();
            dropdownOpen = false;

            utils.PreferencesRepository prefs = new utils.PreferencesRepository();
            if (currentAdmin != null) {
                try {
                    Node currentCenter = ((BorderPane) userMenuPane.getScene().getRoot()).getCenter();

                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/AdminProfileView.fxml"));
                    Node profileScreen = loader.load();

                    AdminProfileViewController ctrl = loader.getController();
                    ctrl.init(
                            currentAdmin,
                            (BorderPane) userMenuPane.getScene().getRoot(),
                            currentCenter,
                            () -> {
                                prefs.updateEmail(currentAdmin.getEmail());
                                setAdminEmail(currentAdmin.getEmail());
                                loadUsers();
                            }
                    );

                    ((BorderPane) userMenuPane.getScene().getRoot()).setCenter(profileScreen);

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        Button settingsBtn = new Button("⚙  Paramètres");
        settingsBtn.getStyleClass().add("dropdown-item-btn");
        settingsBtn.setMaxWidth(Double.MAX_VALUE);
        settingsBtn.setOnAction(e -> { popup.hide(); dropdownOpen = false; showInfo("Paramètres", "Paramètres (à implémenter)"); });

        Separator sep2 = new Separator();
        sep2.setStyle("-fx-padding: 0; -fx-pref-height:1; -fx-background-color: rgba(255,255,255,0.07);");

        Button logoutBtn = new Button("⏻  Se déconnecter");
        logoutBtn.getStyleClass().add("dropdown-logout-btn");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> { popup.hide(); dropdownOpen = false; onLogout(); });

        menu.getChildren().addAll(emailLbl, sep1, profileBtn, settingsBtn, sep2, logoutBtn);

        // Appliquer le stylesheet du projet
        Scene currentScene = userMenuPane.getScene();
        if (currentScene != null) {
            menu.getStylesheets().addAll(currentScene.getStylesheets());
        }

        popup.getContent().add(menu);
        return popup;
    }

    private void onLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Déconnexion");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vous déconnecter ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new utils.PreferencesRepository().clearSession();
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
                    javafx.scene.Parent root = loader.load();
                    javafx.stage.Stage stage = (javafx.stage.Stage) userMenuPane.getScene().getWindow();
                    javafx.scene.Scene scene = new javafx.scene.Scene(
                            root, stage.getWidth(), stage.getHeight()
                    );
                    scene.getStylesheets().add(
                            getClass().getResource("/clutchx-theme.css").toExternalForm()
                    );
                    stage.setScene(scene);
                    stage.setTitle("ClutchX — Connexion");
                } catch (IOException e) {
                    showError("Erreur", "Impossible de retourner au login : " + e.getMessage());
                }
            }
        });
    }
}