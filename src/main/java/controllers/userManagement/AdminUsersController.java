package controllers.userManagement;

import controllers.AdminDashboardController;
import entities.userManagement.Roles;
import entities.userManagement.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import services.userManagement.UserService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminUsersController implements Initializable {

    @FXML private Label totalUsersLabel;
    @FXML private Label totalCoachesLabel;
    @FXML private Label totalPlayersLabel;
    @FXML private Label totalAdminsLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> sortByFilter;
    @FXML private ComboBox<String> orderFilter;
    @FXML private Label resultCountLabel;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colUser;
    @FXML private TableColumn<User, Roles>   colRole;
    @FXML private TableColumn<User, String>  colEmail;
    @FXML private TableColumn<User, Boolean> colStatus;
    @FXML private TableColumn<User, Void>    colActions;

    private final UserService userService = new UserService();
    private ObservableList<User> allUsers = FXCollections.observableArrayList();

    // Reference to layout controller — set by AdminDashboardController after load
    private AdminDashboardController layoutController;

    public void setLayoutController(AdminDashboardController layoutController) {
        this.layoutController = layoutController;
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
                "Tous les rôles", "Admin", "Coach", "Joueur"));
        roleFilter.setValue("Tous les rôles");

        statusFilter.setItems(FXCollections.observableArrayList(
                "Tous les statuts", "Actif", "Bloqué"));
        statusFilter.setValue("Tous les statuts");

        sortByFilter.setItems(FXCollections.observableArrayList("ID", "Email", "Rôle"));
        sortByFilter.setValue("ID");

        orderFilter.setItems(FXCollections.observableArrayList("Décroissant", "Croissant"));
        orderFilter.setValue("Décroissant");
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                setText(empty || id == null ? null : "#" + id);
                setStyle("-fx-text-fill: #c0c0e0; -fx-font-weight: bold;");
            }
        });

        colUser.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUser.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String email, boolean empty) {
                super.updateItem(email, empty);
                if (empty || email == null) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());
                String initials = email.substring(0, Math.min(2, email.length())).toUpperCase();
                Label avatarLabel = new Label(initials);
                avatarLabel.setStyle(
                        "-fx-background-color: " + roleColor(u.getRole()) + ";" +
                                "-fx-background-radius: 50%; -fx-min-width: 34; -fx-max-width: 34;" +
                                "-fx-min-height: 34; -fx-max-height: 34; -fx-alignment: CENTER;" +
                                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
                String displayName = email.contains("@") ? email.split("@")[0] : email;
                Label nameLabel = new Label(capitalize(displayName));
                nameLabel.setStyle("-fx-text-fill: #e0e0ff; -fx-font-weight: bold; -fx-font-size: 13px;");
                Label idSub = new Label("ID: " + u.getId());
                idSub.setStyle("-fx-text-fill: #8080a0; -fx-font-size: 11px;");
                VBox vb = new VBox(1, nameLabel, idSub);
                HBox hb = new HBox(10, avatarLabel, vb);
                hb.setAlignment(Pos.CENTER_LEFT);
                setGraphic(hb);
                setText(null);
            }
        });

        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Roles role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());
                String color = roleColor(u.getRole());
                Label badge = new Label(roleFr(u.getRole()));
                badge.setStyle(
                        "-fx-background-color: " + color + "22; -fx-border-color: " + color + "66;" +
                                "-fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4;" +
                                "-fx-text-fill: " + color + "; -fx-font-size: 11px;" +
                                "-fx-font-weight: bold; -fx-padding: 3 10;");
                setGraphic(badge);
                setText(null);
            }
        });

        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String email, boolean empty) {
                super.updateItem(email, empty);
                setText(empty ? null : email);
                setStyle("-fx-text-fill: #c0c0e0; -fx-font-size: 12px;");
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("blocked"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean blocked, boolean empty) {
                super.updateItem(blocked, empty);
                if (empty || blocked == null) { setGraphic(null); return; }
                Label badge = new Label(blocked ? "Bloqué" : "Actif");
                badge.setStyle(blocked
                        ? "-fx-background-color:#ef444422;-fx-border-color:#ef444466;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-text-fill:#ef4444;-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 10;"
                        : "-fx-background-color:#22c55e22;-fx-border-color:#22c55e66;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-text-fill:#22c55e;-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 10;");
                setGraphic(badge);
                setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn  = new Button("✏");
            private final Button blockBtn = new Button("🔒");
            {
                editBtn.setStyle("-fx-background-color:#3b82f622;-fx-border-color:#3b82f666;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-text-fill:#3b82f6;-fx-cursor:hand;-fx-font-size:13px;-fx-padding:4 8;");
                blockBtn.setStyle("-fx-background-color:#f59e0b22;-fx-border-color:#f59e0b66;-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-text-fill:#f59e0b;-fx-cursor:hand;-fx-font-size:13px;-fx-padding:4 8;");
                editBtn.setOnAction(e  -> onEditUser(getTableView().getItems().get(getIndex())));
                blockBtn.setOnAction(e -> onToggleBlock(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(6, editBtn, blockBtn);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
    }

    // ════════════════════════════════════════
    //  DATA
    // ════════════════════════════════════════

    public void loadUsers() {
        try {
            List<User> users = userService.selectAll();

            List<User> filtered = users.stream()
                    .filter(u -> u.getRole() != Roles.ROLE_ADMIN)
                    .collect(Collectors.toList());

            allUsers.setAll(filtered);
            applyFilters();
            updateStats(users);

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

    @FXML private void onApplyFilters() { applyFilters(); }

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
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String role   = roleFilter.getValue();
        String status = statusFilter.getValue();
        String sortBy = sortByFilter.getValue();
        String order  = orderFilter.getValue();

        List<User> result = allUsers.stream()
                .filter(u -> search.isEmpty() ||
                        u.getEmail().toLowerCase().contains(search) ||
                        roleFr(u.getRole()).toLowerCase().contains(search))
                .filter(u -> role == null || role.equals("Tous les rôles") ||
                        roleFr(u.getRole()).equalsIgnoreCase(role))
                .filter(u -> {
                    if (status == null || status.equals("Tous les statuts")) return true;
                    return status.equals("Actif") ? !u.isBlocked() : u.isBlocked();
                })
                .sorted((a, b) -> {
                    int cmp = switch (sortBy == null ? "ID" : sortBy) {
                        case "Email" -> a.getEmail().compareToIgnoreCase(b.getEmail());
                        case "Rôle"  -> roleFr(a.getRole()).compareToIgnoreCase(roleFr(b.getRole()));
                        default      -> Integer.compare(a.getId(), b.getId());
                    };
                    return "Croissant".equals(order) ? cmp : -cmp;
                })
                .collect(Collectors.toList());

        usersTable.setItems(FXCollections.observableArrayList(result));
        resultCountLabel.setText(result.size() + " utilisateur" +
                (result.size() > 1 ? "s" : "") + " trouvé" + (result.size() > 1 ? "s" : ""));
    }

    // ════════════════════════════════════════
    //  ACTIONS
    // ════════════════════════════════════════

    private void onEditUser(User user) {
        if (user.getRole() == Roles.ROLE_ADMIN) {
            showInfo("Non autorisé", "Impossible de modifier un administrateur.");
            return;
        }
        javafx.stage.Stage owner = (javafx.stage.Stage) usersTable.getScene().getWindow();
        new ProfileDialog().show(owner, user, false, updatedUser -> loadUsers());
    }

    private void onToggleBlock(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/userManagement/BanConfirmDialog.fxml"));
            VBox dialogRoot = loader.load();
            BanConfirmDialogController ctrl = loader.getController();
            ctrl.init(user.getEmail(), !user.isBlocked());

            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.initStyle(javafx.stage.StageStyle.UNDECORATED);
            Scene scene = new Scene(dialogRoot);
            scene.getStylesheets().add(
                    getClass().getResource("/userManagement/clutchx-theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.showAndWait();

            if (ctrl.isConfirmed()) {
                user.setBlocked(!user.isBlocked());
                userService.updateOne(user);
                loadUsers();
            }
        } catch (IOException | SQLException e) {
            showError("Erreur", "Impossible de modifier le statut : " + e.getMessage());
        }
    }

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
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}