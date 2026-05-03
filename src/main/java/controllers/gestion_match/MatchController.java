package controllers.gestion_match;

import entities.Equipe;
import entities.Matchs;
import services.gestion_match.MatchService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MatchController {

    @FXML private VBox matchContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label countLabel;
    @FXML private BorderPane mainBorderPane;
    @FXML private VBox mainContent;

    private final MatchService service = new MatchService();
    private List<Matchs> allMatchs = new ArrayList<>();


    // ================= INIT =================
    @FXML
    public void initialize() {

        statusFilter.setItems(FXCollections.observableArrayList(
                "Tous",
                "À jouer",
                "En cours",
                "Terminé",
                "Annulé"
        ));

        statusFilter.setValue("Tous");

        refresh();
    }


    // ================= REFRESH =================
    @FXML
    public void refresh() {

        allMatchs = service.getAll();
        search();
    }


    // ================= SEARCH + FILTER =================
    @FXML
    public void search() {

        String keyword = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();

        String statut = statusFilter.getValue();

        List<Matchs> filtered = allMatchs.stream()

                .filter(m ->
                        safe(m.getNomMatch()).contains(keyword)
                                || safe(m.getEquipe1() != null ? m.getEquipe1().getNom() : "").contains(keyword)
                                || safe(m.getEquipe2() != null ? m.getEquipe2().getNom() : "").contains(keyword)
                )

                .filter(m -> {

                    if (statut == null || statut.equals("Tous"))
                        return true;

                    String dbStatus = m.getStatut() != null ? m.getStatut().toLowerCase() : "";

                    if (statut.equals("Terminé"))
                        return dbStatus.contains("termine") || dbStatus.contains("terminé");

                    if (statut.equals("À jouer"))
                        return dbStatus.contains("a jouer");

                    if (statut.equals("En cours"))
                        return dbStatus.contains("en_cours");

                    if (statut.equals("Annulé"))
                        return dbStatus.contains("annule") || dbStatus.contains("annulé");

                    return true;
                })

                .collect(Collectors.toList());

        displayMatchs(filtered);
        updateCount(filtered.size());
    }


    // ================= COUNT =================
    private void updateCount(int size) {
        countLabel.setText(size + (size > 1 ? " matchs" : " match"));
    }


    // ================= DISPLAY =================
    private void displayMatchs(List<Matchs> matchs) {

        matchContainer.getChildren().clear();

        if (matchs == null || matchs.isEmpty()) {

            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            emptyBox.getStyleClass().add("empty-box");

            Label title = new Label("Aucun match trouvé");
            title.getStyleClass().add("empty-title");

            Label sub = new Label("Essayez un autre filtre.");
            sub.getStyleClass().add("empty-subtitle");

            Button addBtn = new Button("+ Ajouter Match");
            addBtn.getStyleClass().add("btn-success");
            addBtn.setOnAction(e -> openAddModal());

            emptyBox.getChildren().addAll(title, sub, addBtn);
            matchContainer.getChildren().add(emptyBox);
            return;
        }

        for (Matchs m : matchs) {
            matchContainer.getChildren().add(createCard(m));
        }
    }


    // ================= CARD (Horizontal Scoreboard Row) =================
    private HBox createCard(Matchs m) {

        HBox row = new HBox(0); // Zero spacing for internal padding and accents
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("match-row-horizontal");
        row.setPrefHeight(100);

        // Sidebar Accent (Red for Home/Team 1)
        Region leftAccent = new Region();
        leftAccent.getStyleClass().add("accent-strip-left");
        VBox.setVgrow(leftAccent, Priority.ALWAYS);
        leftAccent.setPrefHeight(100);

        // --- TEAM 1 ---
        HBox team1 = new HBox(15);
        team1.setAlignment(Pos.CENTER_LEFT);
        team1.setPadding(new Insets(0, 0, 0, 25));
        team1.setPrefWidth(240);

        ImageView img1 = new ImageView();
        img1.setFitWidth(54);
        img1.setFitHeight(54);
        setTeamLogo(img1, m.getEquipe1());
        javafx.scene.shape.Circle clip1 = new javafx.scene.shape.Circle(27, 27, 27);
        img1.setClip(clip1);

        Label name1 = new Label(m.getEquipe1() != null ? m.getEquipe1().getNom() : "Equipe 1");
        name1.getStyleClass().add("horizontal-team-name");

        team1.getChildren().addAll(img1, name1);

        // --- CENTER: SCORE + STATUS ---
        VBox center = new VBox(4);
        center.setAlignment(Pos.CENTER);
        HBox.setHgrow(center, Priority.ALWAYS);

        String currentStatus = m.getStatut() != null ? m.getStatut().toUpperCase() : "À JOUER";
        Label status = new Label(currentStatus);
        status.getStyleClass().add("horizontal-status-pill");

        // Dynamic styling based on status type
        if (currentStatus.equals("TERMINÉ")) {
            status.setStyle("-fx-text-fill: #10b981; -fx-border-color: #10b981;");
        } else if (currentStatus.equals("ANNULÉ")) {
            status.setStyle("-fx-text-fill: #f43f5e; -fx-border-color: #f43f5e;");
        } else if (currentStatus.equals("EN_COURS")) {
            status.setStyle("-fx-text-fill: #f59e0b; -fx-border-color: #f59e0b;");
        } else { // À JOUER
            status.setStyle("-fx-text-fill: #3b82f6; -fx-border-color: #3b82f6;");
        }

        Label matchLabel = new Label(m.getNomMatch() != null ? m.getNomMatch().toUpperCase() : "MATCH");
        matchLabel.getStyleClass().add("match-title");
        matchLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7c3aed;");

        Label score = new Label(m.getScoreEquipe1() + " - " + m.getScoreEquipe2());
        score.getStyleClass().add("horizontal-score-text");

        center.getChildren().addAll(status, matchLabel, score);

        // --- TEAM 2 ---
        HBox team2 = new HBox(15);
        team2.setAlignment(Pos.CENTER_RIGHT);
        team2.setPadding(new Insets(0, 25, 0, 0));
        team2.setPrefWidth(240);

        Label name2 = new Label(m.getEquipe2() != null ? m.getEquipe2().getNom() : "Equipe 2");
        name2.getStyleClass().add("horizontal-team-name");

        ImageView img2 = new ImageView();
        img2.setFitWidth(54);
        img2.setFitHeight(54);
        setTeamLogo(img2, m.getEquipe2());
        javafx.scene.shape.Circle clip2 = new javafx.scene.shape.Circle(27, 27, 27);
        img2.setClip(clip2);

        team2.getChildren().addAll(name2, img2);

        // --- ACTIONS ---
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(0, 15, 0, 15));

        Button resBtn = new Button("👁");
        resBtn.getStyleClass().add("btn-view");
        resBtn.setStyle("-fx-background-radius: 10; -fx-min-width: 40; -fx-min-height: 40;");
        resBtn.setOnAction(e -> openResultModal(m));

        Button edBtn = new Button("✎");
        edBtn.getStyleClass().add("btn-edit");
        edBtn.setStyle("-fx-background-radius: 10; -fx-min-width: 40; -fx-min-height: 40;");
        edBtn.setOnAction(e -> openEditModal(m));

        Button delBtn = new Button("🗑");
        delBtn.getStyleClass().add("btn-delete");
        delBtn.setStyle("-fx-background-radius: 10; -fx-min-width: 40; -fx-min-height: 40;");
        delBtn.setOnAction(e -> deleteMatch(m));

        actions.getChildren().addAll(resBtn, edBtn, delBtn);

        // Right Accent (Blue for Away/Team 2)
        Region rightAccent = new Region();
        rightAccent.getStyleClass().add("accent-strip-right");
        VBox.setVgrow(rightAccent, Priority.ALWAYS);
        rightAccent.setPrefHeight(100);

        row.getChildren().addAll(leftAccent, team1, center, team2, actions, rightAccent);

        return row;
    }


    // ================= SCORE BOX =================
    private VBox createScoreBox(String txt, String val) {

        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);

        Label label = new Label(txt);
        label.getStyleClass().add("mini-stat-label");

        Label value = new Label(val);
        value.getStyleClass().add("score-value");

        box.getChildren().addAll(label, value);

        return box;
    }


    // ================= IMAGE =================
    private void setTeamLogo(ImageView img, Equipe equipe) {

        try {

            if (equipe != null &&
                    equipe.getLogo() != null &&
                    !equipe.getLogo().isBlank()) {

                File file = new File(equipe.getLogo());

                if (file.exists()) {
                    img.setImage(new Image(file.toURI().toString()));
                    return;
                }
            }

        } catch (Exception ignored) {}

        try {
            img.setImage(new Image(
                    getClass()
                            .getResource("/gestion_match/default-team.png")
                            .toExternalForm()
            ));
        } catch (Exception ignored) {}
    }


    // ================= HELPERS =================
    private boolean isPlayed(Matchs m) {
        return m.getScoreEquipe1() > 0 || m.getScoreEquipe2() > 0;
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase();
    }


    // ================= MODALS =================
    @FXML
    public void openAddModal() {
        openForm(null, false);
    }

    private void openEditModal(Matchs match) {
        openForm(match, false);
    }

    private void openResultModal(Matchs match) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/gestion_match/matchDetails.fxml")
            );

            Parent root = loader.load();
            MatchDetailsController controller = loader.getController();

            controller.setMatch(match);
            controller.setOnBack(() -> closeForm()); // Returns to the list

            mainBorderPane.setCenter(root);

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Impossible d'ouvrir les détails du match.");
        }
    }

    // =====================
// MatchController.java
// REMPLACER openEditModal + openForm
// =====================
    private void openForm(Matchs match, boolean resultOnly) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/gestion_match/formMatch.fxml")
            );

            Parent formRoot = loader.load();
            FormMatchController controller = loader.getController();

            controller.setParentController(this);
            controller.setResultMode(resultOnly);

            if (match != null) {
                controller.setMatch(match);
                controller.loadData(match);
            }

            // Swapping the center content instead of opening a new Stage
            mainBorderPane.setCenter(formRoot);

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Impossible d'ouvrir le formulaire.");
        }
    }

    public void closeForm() {
        mainBorderPane.setCenter(mainContent);
        refresh();
    }


    // ================= DELETE =================
    private void deleteMatch(Matchs m) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer match ?");
        alert.setContentText("Voulez-vous supprimer ce match ?");

        alert.showAndWait().ifPresent(btn -> {

            if (btn == ButtonType.OK) {
                service.delete(m.getId());
                refresh();
            }
        });
    }


    // ================= ALERTS =================
    public void showSuccess(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    public void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }


    // ================= NAVIGATION =================
    @FXML
    public void goEquipes() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/gestion_match/equipeIndex.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) mainBorderPane.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goDashboard() {
        showSuccess("Ajoutez votre page Dashboard ici.");
    }

    @FXML
    public void goStats(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/gestion_match/stats.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root, 1200, 760));
            stage.setTitle("Classement des équipes");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir Statistiques");
        }
    }

    @FXML
    public void logout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Login.fxml")
            );
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.setTitle("ClutchX - Connexion");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible de se déconnecter.");
        }
    }
}