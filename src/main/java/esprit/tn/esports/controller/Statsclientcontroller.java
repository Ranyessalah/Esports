package esprit.tn.esports.controller;


import esprit.tn.esports.entite.StatsRow;
import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.service.StatsService;
import esprit.tn.esports.service.EquipeService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.image.ImageView;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Statsclientcontroller {

    @FXML private Label totalLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> gameFilter;
    @FXML private ComboBox<String> sortFilter;

    @FXML private TableView<StatsRow> tableView;
    @FXML private TableColumn<StatsRow, Integer> rankCol;
    @FXML private TableColumn<StatsRow, String> teamCol;
    @FXML private TableColumn<StatsRow, String> gameCol;
    @FXML private TableColumn<StatsRow, Integer> playedCol;
    @FXML private TableColumn<StatsRow, Integer> winCol;
    @FXML private TableColumn<StatsRow, Integer> drawCol;
    @FXML private TableColumn<StatsRow, Integer> lossCol;
    @FXML private TableColumn<StatsRow, Integer> bpCol;
    @FXML private TableColumn<StatsRow, Integer> bcCol;
    @FXML private TableColumn<StatsRow, Integer> diffCol;
    @FXML private TableColumn<StatsRow, Integer> ptsCol;

    private final StatsService service = new StatsService();
    private final EquipeService equipeService = new EquipeService();
    private ObservableList<StatsRow> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        initTable();
        initFilters();
        refresh();
    }

    private void initTable() {
        // Position / Rank
        rankCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("rank"));
        rankCol.setCellFactory(column -> new TableCell<StatsRow, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); }
                else {
                    Label lbl = new Label("#" + item);
                    lbl.getStyleClass().add("table-rank-badge");
                    setGraphic(lbl);
                }
            }
        });

        // Team Logo + Name
        teamCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("team"));
        teamCol.setCellFactory(column -> new TableCell<StatsRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); }
                else {
                    StatsRow row = getTableView().getItems().get(getIndex());
                    HBox box = new HBox(12);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    
                    ImageView iv = new ImageView();
                    iv.setFitWidth(30); iv.setFitHeight(30);
                    setCircularImage(iv, row.getLogo());
                    
                    Label lbl = new Label(item);
                    lbl.getStyleClass().add("table-team-text");
                    box.getChildren().addAll(iv, lbl);
                    setGraphic(box);
                }
            }
        });

        gameCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("game"));
        playedCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("played"));
        winCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("wins"));
        drawCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("draws"));
        lossCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("losses"));
        bpCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("bp"));
        bcCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("bc"));
        diffCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("diff"));

        // Points Pill
        ptsCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("points"));
        ptsCol.setCellFactory(column -> new TableCell<StatsRow, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); }
                else {
                    Label lbl = new Label(item + " PTS");
                    lbl.getStyleClass().add("table-pts-pill");
                    setGraphic(lbl);
                }
            }
        });

        // 🔥 ROW CLICK LISTENER
        tableView.setRowFactory(tv -> {
            TableRow<StatsRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) {
                    StatsRow selected = row.getItem();
                    showTeamDetails(selected);
                }
            });
            return row;
        });
    }

    private void showTeamDetails(StatsRow selected) {
        try {
            Equipe fullEquipe = equipeService.getById(selected.getTeamId());
            if (fullEquipe == null) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/EquipeDetails.fxml"));
            Parent root = loader.load();

            EquipeDetailsController controller = loader.getController();
            controller.setEquipe(fullEquipe);
            
            Stage stage = (Stage) tableView.getScene().getWindow();

            // Set back navigation to this stats page
            controller.setOnBack(() -> {
                try {
                    FXMLLoader sLoader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/stats_client.fxml"));
                    Parent sRoot = sLoader.load();
                    stage.setScene(new Scene(sRoot, 1200, 760));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            stage.setScene(new Scene(root, 1200, 760));

        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir les détails de l'équipe");
        }
    }

    private void initFilters() {
        sortFilter.setItems(FXCollections.observableArrayList(
                "Classement officiel",
                "Victoires",
                "Défaites",
                "Différence"
        ));
        sortFilter.setValue("Classement officiel");
    }

    @FXML
    public void refresh() {
        List<StatsRow> rows = service.getClassement();
        masterData.setAll(rows);
        loadGames();
        filterData();
    }

    private void loadGames() {
        List<String> games = masterData.stream()
                .map(StatsRow::getGame)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        gameFilter.getItems().clear();
        gameFilter.getItems().add("Tous les jeux");
        gameFilter.getItems().addAll(games);
        gameFilter.setValue("Tous les jeux");
    }

    @FXML
    public void filterData() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String selectedGame = gameFilter.getValue();
        String sort = sortFilter.getValue();

        List<StatsRow> filtered = masterData.stream()
                .filter(r -> r.getTeam().toLowerCase().contains(keyword))
                .filter(r -> selectedGame == null || selectedGame.equals("Tous les jeux") || r.getGame().equalsIgnoreCase(selectedGame))
                .collect(Collectors.toList());

        // SORT
        switch (sort) {
            case "Victoires": filtered.sort(Comparator.comparingInt(StatsRow::getWins).reversed()); break;
            case "Défaites": filtered.sort(Comparator.comparingInt(StatsRow::getLosses).reversed()); break;
            case "Différence": filtered.sort(Comparator.comparingInt(StatsRow::getDiff).reversed()); break;
            default: filtered.sort(Comparator.comparingInt(StatsRow::getPoints).reversed());
        }

        // RANK
        for (int i = 0; i < filtered.size(); i++) {
            filtered.get(i).setRank(i + 1);
        }

        tableView.setItems(FXCollections.observableArrayList(filtered));
        totalLabel.setText("Total équipes : " + filtered.size());
    }

    private void setCircularImage(ImageView iv, String path) {
        try {
            if (path != null && !path.isEmpty()) {
                iv.setImage(new javafx.scene.image.Image("file:" + path));
            } else {
                iv.setImage(new javafx.scene.image.Image(getClass().getResourceAsStream("/esprit/tn/esports/images/default_team.png")));
            }
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(iv.getFitWidth()/2, iv.getFitHeight()/2, iv.getFitWidth()/2);
            iv.setClip(clip);
        } catch (Exception e) {
            // fallback
        }
    }


    // ================= RESET =================
    @FXML
    public void resetFilters() {

        searchField.clear();
        gameFilter.setValue("Tous les jeux");
        sortFilter.setValue("Classement officiel");

        filterData();
    }








    // ================= NAVIGATION =================

    @FXML
    public void goEquipes(javafx.event.ActionEvent event) {
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
            showError("Impossible d'ouvrir Equipes");
        }
    }

    @FXML
    public void goMatchs(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/esprit/tn/esports/matchIndex_client.fxml")
            );
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir Matchs");
        }
    }

    @FXML
    public void goStats(javafx.event.ActionEvent event) {
        // Déjà sur la page stats
        refresh();
    }

    @FXML
    public void logout(javafx.event.ActionEvent event) {
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


    // ================= ALERT =================
    private void showError(String msg) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}