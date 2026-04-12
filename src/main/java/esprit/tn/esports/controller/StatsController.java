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
import javafx.scene.chart.*;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class StatsController {

    @FXML private Label totalLabel;
    @FXML private Label bestTeamLabel;
    @FXML private Label bestBpLabel;
    @FXML private Label bestBcLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> gameFilter;
    @FXML private ComboBox<String> sortFilter;

    @FXML private BarChart<String, Number> pointsChart;
    @FXML private PieChart performancePie;
    @FXML private PieChart gamePie;
    @FXML private BarChart<String, Number> goalsChart;

    private final StatsService service = new StatsService();

    private ObservableList<StatsRow> masterData =
            FXCollections.observableArrayList();


    // ================= INIT =================
    @FXML
    public void initialize() {
        initFilters();
        refresh();
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
                .filter(r -> r.getTeam().toLowerCase().contains(keyword))
                .filter(r -> selectedGame == null
                                || selectedGame.equals("Tous les jeux")
                                || r.getGame().equalsIgnoreCase(selectedGame))
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

        updateDashboard(filtered);
        totalLabel.setText("Total équipes : " + filtered.size());
    }

    private void updateDashboard(List<StatsRow> data) {
        if (data.isEmpty()) {
            bestTeamLabel.setText("-");
            bestBpLabel.setText("-");
            bestBcLabel.setText("-");
            pointsChart.getData().clear();
            performancePie.getData().clear();
            gamePie.getData().clear();
            goalsChart.getData().clear();
            return;
        }

        // 1. Metric Cards
        StatsRow topTeam = data.get(0);
        bestTeamLabel.setText(topTeam.getTeam());

        StatsRow mostBp = data.stream().max(Comparator.comparingInt(StatsRow::getBp)).orElse(topTeam);
        bestBpLabel.setText(mostBp.getTeam() + " (" + mostBp.getBp() + ")");

        StatsRow bestDefense = data.stream().min(Comparator.comparingInt(StatsRow::getBc)).orElse(topTeam);
        bestBcLabel.setText(bestDefense.getTeam() + " (" + bestDefense.getBc() + ")");

        // 2. BarChart: Points Comparison (Top 10)
        pointsChart.getData().clear();
        XYChart.Series<String, Number> pointsSeries = new XYChart.Series<>();
        pointsSeries.setName("Points");
        data.stream().limit(10).forEach(r -> {
            pointsSeries.getData().add(new XYChart.Data<>(r.getTeam(), r.getPoints()));
        });
        pointsChart.getData().add(pointsSeries);

        // 3. PieChart: Global Performance results
        performancePie.getData().clear();
        int totalWins = data.stream().mapToInt(StatsRow::getWins).sum();
        int totalLosses = data.stream().mapToInt(StatsRow::getLosses).sum();
        int totalDraws = data.stream().mapToInt(StatsRow::getDraws).sum();

        if (totalWins + totalLosses + totalDraws > 0) {
            performancePie.getData().add(new PieChart.Data("Victoires (" + totalWins + ")", totalWins));
            performancePie.getData().add(new PieChart.Data("Défaites (" + totalLosses + ")", totalLosses));
            performancePie.getData().add(new PieChart.Data("Matchs Nuls (" + totalDraws + ")", totalDraws));
        }

        // 4. PieChart: Game Distribution
        gamePie.getData().clear();
        data.stream()
            .collect(Collectors.groupingBy(StatsRow::getGame, Collectors.counting()))
            .forEach((game, count) -> {
                gamePie.getData().add(new PieChart.Data(game + " (" + count + ")", count));
            });

        // 5. BarChart: BP vs BC (Top 5)
        goalsChart.getData().clear();
        XYChart.Series<String, Number> bpSeries = new XYChart.Series<>();
        bpSeries.setName("Attaque (BP)");
        XYChart.Series<String, Number> bcSeries = new XYChart.Series<>();
        bcSeries.setName("Défense (BC)");

        data.stream().limit(5).forEach(r -> {
            bpSeries.getData().add(new XYChart.Data<>(r.getTeam(), r.getBp()));
            bcSeries.getData().add(new XYChart.Data<>(r.getTeam(), r.getBc()));
        });
        goalsChart.getData().addAll(bpSeries, bcSeries);
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
}