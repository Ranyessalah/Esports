package esprit.tn.esports.controller;


import esprit.tn.esports.entite.StatsRow;
import esprit.tn.esports.service.StatsService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Statsclientcontroller {

    @FXML private Label totalLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> gameFilter;
    @FXML private ComboBox<String> sortFilter;

    @FXML private TableView<StatsRow> tableView;

    @FXML private TableColumn<StatsRow,Integer> rankCol;
    @FXML private TableColumn<StatsRow,String> teamCol;
    @FXML private TableColumn<StatsRow,String> gameCol;
    @FXML private TableColumn<StatsRow,Integer> playedCol;
    @FXML private TableColumn<StatsRow,Integer> winCol;
    @FXML private TableColumn<StatsRow,Integer> drawCol;
    @FXML private TableColumn<StatsRow,Integer> lossCol;
    @FXML private TableColumn<StatsRow,Integer> bpCol;
    @FXML private TableColumn<StatsRow,Integer> bcCol;
    @FXML private TableColumn<StatsRow,Integer> diffCol;
    @FXML private TableColumn<StatsRow,Integer> ptsCol;

    private final StatsService service = new StatsService();

    private ObservableList<StatsRow> masterData =
            FXCollections.observableArrayList();


    // ================= INIT =================
    @FXML
    public void initialize() {

        initTable();
        initFilters();
        refresh();
    }


    // ================= TABLE =================
    private void initTable() {

        rankCol.setCellValueFactory(new PropertyValueFactory<>("rank"));
        teamCol.setCellValueFactory(new PropertyValueFactory<>("team"));
        gameCol.setCellValueFactory(new PropertyValueFactory<>("game"));
        playedCol.setCellValueFactory(new PropertyValueFactory<>("played"));
        winCol.setCellValueFactory(new PropertyValueFactory<>("wins"));
        drawCol.setCellValueFactory(new PropertyValueFactory<>("draws"));
        lossCol.setCellValueFactory(new PropertyValueFactory<>("losses"));
        bpCol.setCellValueFactory(new PropertyValueFactory<>("bp"));
        bcCol.setCellValueFactory(new PropertyValueFactory<>("bc"));
        diffCol.setCellValueFactory(new PropertyValueFactory<>("diff"));
        ptsCol.setCellValueFactory(new PropertyValueFactory<>("points"));
    }


    // ================= FILTERS =================
    private void initFilters() {

        sortFilter.setItems(FXCollections.observableArrayList(
                "Classement officiel",
                "Victoires",
                "Défaites",
                "Différence"
        ));

        sortFilter.setValue("Classement officiel");
    }


    // ================= REFRESH =================
    @FXML
    public void refresh() {

        List<StatsRow> rows = service.getClassement();

        masterData.setAll(rows);

        loadGames();
        filterData();
    }


    // ================= LOAD GAMES =================
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


    // ================= FILTER =================
    @FXML
    public void filterData() {

        String keyword = searchField.getText() == null
                ? ""
                : searchField.getText().toLowerCase().trim();

        String selectedGame = gameFilter.getValue();
        String sort = sortFilter.getValue();

        List<StatsRow> filtered = masterData.stream()

                .filter(r ->
                        r.getTeam().toLowerCase().contains(keyword)
                )

                .filter(r ->
                        selectedGame == null
                                || selectedGame.equals("Tous les jeux")
                                || r.getGame().equalsIgnoreCase(selectedGame)
                )

                .collect(Collectors.toList());


        // SORT
        switch (sort) {

            case "Victoires":
                filtered.sort(Comparator.comparingInt(StatsRow::getWins).reversed());
                break;

            case "Défaites":
                filtered.sort(Comparator.comparingInt(StatsRow::getLosses).reversed());
                break;

            case "Différence":
                filtered.sort(Comparator.comparingInt(StatsRow::getDiff).reversed());
                break;

            default:
                filtered.sort(Comparator.comparingInt(StatsRow::getPoints).reversed());
        }


        // update rank
        for (int i = 0; i < filtered.size(); i++) {
            filtered.get(i).setRank(i + 1);
        }

        tableView.setItems(FXCollections.observableArrayList(filtered));

        totalLabel.setText("Total équipes : " + filtered.size());
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
    public void goDashboard(javafx.event.ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/esprit/tn/esports/AdminDashboard.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.setTitle("Dashboard");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir Dashboard");
        }
    }


    @FXML
    public void goEquipes(javafx.event.ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/esprit/tn/esports/equipeIndex.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.setTitle("Gestion Equipes");
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
                    getClass().getResource("/esprit/tn/esports/matchIndex.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.setTitle("Gestion Matchs");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir Matchs");
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