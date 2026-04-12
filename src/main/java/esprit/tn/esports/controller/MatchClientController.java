package esprit.tn.esports.controller;

import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Matchs;
import esprit.tn.esports.service.MatchService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MatchClientController {

    @FXML private FlowPane matchContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label countLabel;

    private final MatchService service = new MatchService();
    private List<Matchs> allMatchs = new ArrayList<>();


    // ================= INIT =================
    @FXML
    public void initialize() {

        statusFilter.setItems(FXCollections.observableArrayList(
                "Tous",
                "À jouer",
                "Terminé"
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

                    if (statut == null || statut.equals("Tous")) {
                        return true;
                    }

                    if (statut.equals("Terminé")) {
                        return isPlayed(m);
                    }

                    if (statut.equals("À jouer")) {
                        return !isPlayed(m);
                    }

                    return true;
                })

                .collect(Collectors.toList());

        displayMatchs(filtered);
        countLabel.setText(filtered.size() + " matchs");
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

            emptyBox.getChildren().addAll(title, sub);

            matchContainer.getChildren().add(emptyBox);
            return;
        }

        for (Matchs m : matchs) {
            matchContainer.getChildren().add(createCard(m));
        }
    }


    // ================= CARD =================
    private VBox createCard(Matchs m) {

        VBox card = new VBox(16);
        card.setPrefWidth(305);
        card.getStyleClass().add("match-card");

        // HEADER
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane logoWrap = new StackPane();
        logoWrap.getStyleClass().add("mini-logo-wrap");

        ImageView icon = new ImageView();
        icon.setFitWidth(34);
        icon.setFitHeight(34);

        setTeamLogo(icon, m.getEquipe1());

        logoWrap.getChildren().add(icon);

        VBox titleBox = new VBox(4);

        Label title = new Label(
                m.getNomMatch() == null || m.getNomMatch().isBlank()
                        ? "Match"
                        : m.getNomMatch()
        );

        title.getStyleClass().add("match-title");

        Label sub = new Label("Match e-sport");
        sub.getStyleClass().add("match-subtitle");

        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(isPlayed(m) ? "TERMINÉ" : "À JOUER");
        badge.getStyleClass().add(
                isPlayed(m) ? "badge-played" : "badge-pending"
        );

        header.getChildren().addAll(logoWrap, titleBox, spacer, badge);


        // TEAMS
        HBox teams = new HBox(14);
        teams.setAlignment(Pos.CENTER);

        VBox team1 = createTeamBox(m.getEquipe1());

        Label vs = new Label("VS");
        vs.getStyleClass().add("vs-label");

        VBox team2 = createTeamBox(m.getEquipe2());

        teams.getChildren().addAll(team1, vs, team2);


        // SCORE
        HBox scoreRow = new HBox(22);
        scoreRow.setAlignment(Pos.CENTER);
        scoreRow.getStyleClass().add("score-row");

        VBox s1 = createScoreBox("Score", String.valueOf(m.getScoreEquipe1()));
        VBox s2 = createScoreBox("Score", String.valueOf(m.getScoreEquipe2()));

        scoreRow.getChildren().addAll(s1, s2);


        // BUTTON
        Button viewBtn = new Button("Voir Match");
        viewBtn.getStyleClass().add("btn-view");
        viewBtn.setOnAction(e -> openDetailsPage(m));

        HBox actions = new HBox(viewBtn);
        actions.setAlignment(Pos.CENTER);

        card.getChildren().addAll(header, teams, scoreRow, actions);

        return card;
    }


    // ================= TEAM BOX =================
    private VBox createTeamBox(Equipe equipe) {

        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(110);

        ImageView img = new ImageView();
        img.setFitWidth(56);
        img.setFitHeight(56);

        setTeamLogo(img, equipe);

        Label name = new Label(
                equipe != null ? equipe.getNom() : "Equipe"
        );

        name.getStyleClass().add("team-name");
        name.setWrapText(true);
        name.setMaxWidth(100);
        name.setAlignment(Pos.CENTER);

        box.getChildren().addAll(img, name);

        return box;
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


    // ================= OPEN DETAILS =================
    private void openDetailsPage(Matchs m) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/esprit/tn/esports/matchDetails.fxml")
            );

            Parent root = loader.load();

            MatchDetailsController controller = loader.getController();
            controller.setMatch(m);

            Stage stage = (Stage) matchContainer.getScene().getWindow(); // This one is fine as it uses the container of the cards, which is usually present
            stage.setScene(new Scene(root, 1200, 760));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
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
                            .getResource("/esprit/tn/esports/default-team.png")
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


    // ================= NAVIGATION =================
    @FXML
    public void goEquipes(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/esprit/tn/esports/equipeIndex_client.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goStats(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/esprit/tn/esports/stats_client.fxml")
            );

            Parent root = loader.load();

            // 🔥 nouvelle fenêtre
            Stage stage = new Stage();

            stage.setTitle("Classement des équipes");
            stage.setScene(new Scene(root, 1200, 760));
            stage.centerOnScreen();
            stage.show();



        } catch (Exception e) {
            e.printStackTrace();
         }
    }

    @FXML
    public void logout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/esprit/tn/esports/Login.fxml")
            );
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.setTitle("ClutchX - Connexion");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}