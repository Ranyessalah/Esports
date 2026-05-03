package controllers.matchManagement;

import entities.matchManagement.Matchs;
import entities.matchManagement.StatsRow;
import services.matchManagement.MatchService;
import services.matchManagement.StatsService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.chart.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class StatsController {

    @FXML private BorderPane root;

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
    @FXML private BarChart<String, Number> yearlyWinsChart;

    private final StatsService service = new StatsService();
    private final MatchService matchService = new MatchService();

    private int startYear = LocalDateTime.now().getYear() - 9;
    private int endYear = LocalDateTime.now().getYear();

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
            yearlyWinsChart.getData().clear();
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

        // 6. Yearly Stats (Last 10 Years)
        updateYearlyStats();
    }

    @FXML
    public void showYearRangeDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Choisir la plage d'années");
        dialog.setHeaderText("Sélectionnez les années pour les statistiques");

        ComboBox<Integer> startCombo = new ComboBox<>();
        ComboBox<Integer> endCombo = new ComboBox<>();

        int currentYear = LocalDateTime.now().getYear();
        for (int y = currentYear; y >= currentYear - 20; y--) {
            startCombo.getItems().add(y);
            endCombo.getItems().add(y);
        }

        startCombo.setValue(startYear);
        endCombo.setValue(endYear);

        VBox content = new VBox(10, 
            new Label("Année de début :"), startCombo,
            new Label("Année de fin :"), endCombo
        );
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (startCombo.getValue() > endCombo.getValue()) {
                    showError("L'année de début doit être inférieure ou égale à l'année de fin.");
                } else {
                    this.startYear = startCombo.getValue();
                    this.endYear = endCombo.getValue();
                    updateYearlyStats();
                }
            }
        });
    }

    private void updateYearlyStats() {
        yearlyWinsChart.setAnimated(false);
        yearlyWinsChart.getXAxis().setAnimated(false);
        yearlyWinsChart.getYAxis().setAnimated(false);

        yearlyWinsChart.getData().clear();
        if (yearlyWinsChart.getXAxis() instanceof CategoryAxis) {
            ((CategoryAxis) yearlyWinsChart.getXAxis()).getCategories().clear();
        }
        yearlyWinsChart.setTitle("Matchs par Année (" + startYear + " - " + endYear + ")");

        List<Matchs> allMatches = matchService.getAll();

        XYChart.Series<String, Number> winsSeries = new XYChart.Series<>();
        winsSeries.setName("Victoires (Décisifs)");
        XYChart.Series<String, Number> drawsSeries = new XYChart.Series<>();
        drawsSeries.setName("Matchs Nuls");

        for (int year = startYear; year <= endYear; year++) {
            final int y = year;
            List<Matchs> yearMatches = allMatches.stream()
                    .filter(m -> m.getDateMatch() != null && m.getDateMatch().getYear() == y)
                    .collect(Collectors.toList());

            long wins = yearMatches.stream()
                    .filter(m -> m.getScoreEquipe1() != null && m.getScoreEquipe2() != null 
                            && !m.getScoreEquipe1().equals(m.getScoreEquipe2()))
                    .count();

            long draws = yearMatches.stream()
                    .filter(m -> m.getScoreEquipe1() != null && m.getScoreEquipe2() != null 
                            && m.getScoreEquipe1().equals(m.getScoreEquipe2()))
                    .count();

            winsSeries.getData().add(new XYChart.Data<>(String.valueOf(year), wins));
            drawsSeries.getData().add(new XYChart.Data<>(String.valueOf(year), draws));
        }

        yearlyWinsChart.getData().addAll(winsSeries, drawsSeries);
        yearlyWinsChart.layout();
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
                    getClass().getResource("/matchManagement/equipeIndex.fxml")
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
                    getClass().getResource("/matchManagement/matchIndex.fxml")
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
                    getClass().getResource("/matchManagement/Login.fxml")
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


    // ================= Google Calendar: UNE SEULE FENÊTRE avec calendrier visuel =================
    // Ouvre un WebView unique avec un calendrier FullCalendar affichant TOUS les matchs.
    // Chaque événement est cliquable → ouvre Google Calendar pour cet événement.

    private static final DateTimeFormatter GCAL_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    @FXML
    public void exportMatchesForGoogleCalendar() {
        try {
            // Récupérer les matchs des 10 dernières années
            LocalDateTime until = LocalDateTime.now();
            LocalDateTime since = until.minusYears(10);
            List<Matchs> matches = matchService.findMatchesBetween(since, until);

            if (matches.isEmpty()) {
                showError("Aucun match trouvé dans les 10 dernières années.");
                return;
            }

            // Filtrer les matchs sans date
            List<Matchs> validMatches = matches.stream()
                    .filter(m -> m.getDateMatch() != null)
                    .collect(Collectors.toList());

            if (validMatches.isEmpty()) {
                showError("Aucun match avec une date définie.");
                return;
            }

            // Construire le JSON des événements pour FullCalendar
            StringBuilder eventsJson = new StringBuilder("[");
            for (int i = 0; i < validMatches.size(); i++) {
                Matchs m = validMatches.get(i);
                String t1 = m.getEquipe1() != null && m.getEquipe1().getNom() != null ? m.getEquipe1().getNom() : "Eq1";
                String t2 = m.getEquipe2() != null && m.getEquipe2().getNom() != null ? m.getEquipe2().getNom() : "Eq2";
                String title = (m.getNomMatch() != null && !m.getNomMatch().isBlank()) ? m.getNomMatch().trim() : (t1 + " vs " + t2);
                String statut = m.getStatut() != null ? m.getStatut().toLowerCase() : "a jouer";

                String color;
                if (statut.contains("termine")) color = "#10b981";
                else if (statut.contains("en_cours") || statut.contains("en cours")) color = "#f59e0b";
                else if (statut.contains("annul")) color = "#f43f5e";
                else color = "#3b82f6";

                LocalDateTime start = m.getDateMatch();
                LocalDateTime end = m.getDateFinMatch() != null ? m.getDateFinMatch() : start.plusHours(2);
                String score = (m.getScoreEquipe1() != null && m.getScoreEquipe2() != null)
                        ? m.getScoreEquipe1() + " - " + m.getScoreEquipe2() : "";

                String gcalUrl = buildGoogleCalendarUrl(m);

                if (i > 0) eventsJson.append(",");
                eventsJson.append("{")
                    .append("\"title\":\"").append(escapeJs(title)).append("\",")
                    .append("\"start\":\"").append(start.toString()).append("\",")
                    .append("\"end\":\"").append(end.toString()).append("\",")
                    .append("\"color\":\"").append(color).append("\",")
                    .append("\"extendedProps\":{")
                        .append("\"team1\":\"").append(escapeJs(t1)).append("\",")
                        .append("\"team2\":\"").append(escapeJs(t2)).append("\",")
                        .append("\"score\":\"").append(escapeJs(score)).append("\",")
                        .append("\"statut\":\"").append(escapeJs(statut)).append("\",")
                        .append("\"gcalUrl\":\"").append(escapeJs(gcalUrl)).append("\"")
                    .append("}")
                .append("}");
            }
            eventsJson.append("]");

            // Construire le HTML complet avec FullCalendar
            String html = buildCalendarHtml(eventsJson.toString(), validMatches.size());

            // Ouvrir UNE SEULE fenêtre WebView
            Stage calendarStage = new Stage();
            calendarStage.initModality(javafx.stage.Modality.NONE);
            calendarStage.setTitle("📅 ClutchX — Calendrier des Matchs (10 ans)");

            javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
            javafx.scene.web.WebEngine engine = webView.getEngine();

            // Ouvrir les liens Google Calendar dans le navigateur externe
            engine.setCreatePopupHandler(param -> {
                // Pas de popup interne
                return null;
            });

            // Intercepter les changements de location pour ouvrir Google Calendar dans le vrai navigateur
            engine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
                if (newUrl != null && newUrl.contains("calendar.google.com")) {
                    javafx.application.Platform.runLater(() -> {
                        engine.loadContent(html, "text/html"); // Recharger le calendrier
                        try {
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                Desktop.getDesktop().browse(new URI(newUrl));
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            });

            engine.loadContent(html, "text/html");

            Scene scene = new Scene(webView, 1100, 750);
            calendarStage.setScene(scene);
            calendarStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur calendrier: " + e.getMessage());
        }
    }

    private String buildCalendarHtml(String eventsJson, int totalMatches) {
        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>");
        h.append("<title>ClutchX Calendar</title>");
        h.append("<link href='https://cdn.jsdelivr.net/npm/fullcalendar@6.1.11/index.global.min.css' rel='stylesheet'>");
        h.append("<script src='https://cdn.jsdelivr.net/npm/fullcalendar@6.1.11/index.global.min.js'></script>");
        h.append("<style>");
        h.append("* { margin:0; padding:0; box-sizing:border-box; }");
        h.append("body { font-family:'Segoe UI',system-ui,sans-serif; background:linear-gradient(135deg,#0f172a 0%,#1e1b4b 50%,#0f172a 100%); color:#e2e8f0; min-height:100vh; }");
        h.append(".header { background:rgba(15,23,42,0.95); border-bottom:2px solid #7c3aed; padding:16px 28px; display:flex; align-items:center; justify-content:space-between; }");
        h.append(".header h1 { font-size:22px; font-weight:800; background:linear-gradient(to right,#a78bfa,#c084fc); -webkit-background-clip:text; -webkit-text-fill-color:transparent; }");
        h.append(".header .badge { background:linear-gradient(to right,#7c3aed,#6d28d9); color:white; padding:6px 16px; border-radius:20px; font-size:13px; font-weight:700; }");
        h.append(".legend { display:flex; gap:18px; padding:12px 28px; background:rgba(30,27,75,0.5); border-bottom:1px solid rgba(124,58,237,0.2); }");
        h.append(".legend-item { display:flex; align-items:center; gap:6px; font-size:12px; color:#94a3b8; }");
        h.append(".legend-dot { width:10px; height:10px; border-radius:50%; }");
        h.append("#calendar-container { padding:20px 28px 28px; }");
        h.append(".fc { --fc-border-color:rgba(124,58,237,0.15); --fc-page-bg-color:transparent; --fc-neutral-bg-color:rgba(30,27,75,0.3); --fc-list-event-hover-bg-color:rgba(124,58,237,0.15); --fc-today-bg-color:rgba(124,58,237,0.08); }");
        h.append(".fc .fc-toolbar-title { color:#e2e8f0; font-size:18px; font-weight:700; }");
        h.append(".fc .fc-button { background:rgba(124,58,237,0.3)!important; border:1px solid rgba(124,58,237,0.4)!important; color:#e2e8f0!important; font-weight:600!important; border-radius:8px!important; }");
        h.append(".fc .fc-button:hover { background:rgba(124,58,237,0.5)!important; }");
        h.append(".fc .fc-button-active { background:#7c3aed!important; border-color:#7c3aed!important; }");
        h.append(".fc .fc-col-header-cell { color:#a78bfa; font-weight:700; font-size:12px; }");
        h.append(".fc .fc-daygrid-day-number { color:#94a3b8; font-weight:600; }");
        h.append(".fc .fc-daygrid-day.fc-day-today .fc-daygrid-day-number { color:#c084fc; }");
        h.append(".fc-event { border-radius:6px!important; padding:2px 6px!important; font-weight:600!important; font-size:11px!important; cursor:pointer!important; border:none!important; }");
        h.append(".fc-event:hover { transform:scale(1.03); box-shadow:0 4px 12px rgba(0,0,0,0.4); }");
        h.append(".tooltip-overlay { display:none; position:fixed; background:rgba(15,23,42,0.97); border:2px solid #7c3aed; border-radius:14px; padding:18px 22px; z-index:9999; min-width:280px; box-shadow:0 12px 40px rgba(124,58,237,0.4); }");
        h.append(".tooltip-overlay.active { display:block; }");
        h.append(".tooltip-title { font-size:16px; font-weight:800; color:#c084fc; margin-bottom:10px; }");
        h.append(".tooltip-row { display:flex; justify-content:space-between; padding:5px 0; font-size:13px; border-bottom:1px solid rgba(124,58,237,0.1); }");
        h.append(".tooltip-label { color:#64748b; }");
        h.append(".tooltip-value { color:#e2e8f0; font-weight:600; }");
        h.append(".gcal-btn { display:block; width:100%; margin-top:14px; padding:10px; background:linear-gradient(to right,#0d9488,#059669); color:white; border:none; border-radius:10px; font-size:14px; font-weight:700; cursor:pointer; text-align:center; text-decoration:none; }");
        h.append(".gcal-btn:hover { box-shadow:0 6px 20px rgba(16,185,129,0.4); }");
        h.append("</style></head><body>");
        h.append("<div class='header'><h1>ClutchX — Calendrier des Matchs</h1>");
        h.append("<span class='badge'>").append(totalMatches).append(" matchs (10 ans)</span></div>");
        h.append("<div class='legend'>");
        h.append("<div class='legend-item'><div class='legend-dot' style='background:#3b82f6'></div> A jouer</div>");
        h.append("<div class='legend-item'><div class='legend-dot' style='background:#f59e0b'></div> En cours</div>");
        h.append("<div class='legend-item'><div class='legend-dot' style='background:#10b981'></div> Termine</div>");
        h.append("<div class='legend-item'><div class='legend-dot' style='background:#f43f5e'></div> Annule</div>");
        h.append("<div class='legend-item' style='margin-left:auto;color:#a78bfa;font-weight:600;'>Cliquez sur un match pour l'ajouter a Google Calendar</div>");
        h.append("</div>");
        h.append("<div id='calendar-container'></div>");
        h.append("<div class='tooltip-overlay' id='tooltip'></div>");
        h.append("<script>");
        h.append("var tooltip=document.getElementById('tooltip');");
        h.append("document.addEventListener('DOMContentLoaded',function(){");
        h.append("var calendarEl=document.getElementById('calendar-container');");
        h.append("var calendar=new FullCalendar.Calendar(calendarEl,{");
        h.append("initialView:'dayGridMonth',locale:'fr',height:'auto',");
        h.append("headerToolbar:{left:'prev,next today',center:'title',right:'dayGridMonth,timeGridWeek,listYear'},");
        h.append("buttonText:{today:\"Aujourd'hui\",month:'Mois',week:'Semaine',list:'Liste'},");
        h.append("events:").append(eventsJson).append(",");
        h.append("eventMouseEnter:function(info){");
        h.append("var p=info.event.extendedProps;var r=info.el.getBoundingClientRect();");
        h.append("tooltip.innerHTML='<div class=\"tooltip-title\">'+info.event.title+'</div>'");
        h.append("+'<div class=\"tooltip-row\"><span class=\"tooltip-label\">Equipes</span><span class=\"tooltip-value\">'+p.team1+' vs '+p.team2+'</span></div>'");
        h.append("+(p.score?'<div class=\"tooltip-row\"><span class=\"tooltip-label\">Score</span><span class=\"tooltip-value\">'+p.score+'</span></div>':'')");
        h.append("+'<div class=\"tooltip-row\"><span class=\"tooltip-label\">Statut</span><span class=\"tooltip-value\">'+p.statut.toUpperCase()+'</span></div>'");
        h.append("+'<div class=\"tooltip-row\"><span class=\"tooltip-label\">Date</span><span class=\"tooltip-value\">'+info.event.start.toLocaleDateString(\"fr-FR\")+'</span></div>'");
        h.append("+'<a class=\"gcal-btn\" href=\"'+p.gcalUrl+'\" target=\"_blank\">Ajouter a Google Calendar</a>';");
        h.append("tooltip.style.left=Math.min(r.left,window.innerWidth-310)+'px';");
        h.append("tooltip.style.top=(r.bottom+8)+'px';");
        h.append("tooltip.classList.add('active');},");
        h.append("eventMouseLeave:function(info){setTimeout(function(){if(!tooltip.matches(':hover'))tooltip.classList.remove('active');},300);},");
        h.append("eventClick:function(info){info.jsEvent.preventDefault();var url=info.event.extendedProps.gcalUrl;if(url)window.location.href=url;}");
        h.append("});calendar.render();});");
        h.append("tooltip.addEventListener('mouseleave',function(){tooltip.classList.remove('active');});");
        h.append("</script></body></html>");
        return h.toString();
    }

    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    /**
     * Construit l'URL Google Calendar pour un match donné.
     */
    private String buildGoogleCalendarUrl(Matchs m) {
        LocalDateTime start = m.getDateMatch();
        LocalDateTime end = m.getDateFinMatch() != null ? m.getDateFinMatch() : start.plusHours(2);

        String startUtc = start.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(java.time.ZoneOffset.UTC)
                .format(GCAL_FMT);
        String endUtc = end.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(java.time.ZoneOffset.UTC)
                .format(GCAL_FMT);

        String t1 = m.getEquipe1() != null && m.getEquipe1().getNom() != null
                ? m.getEquipe1().getNom() : "Équipe 1";
        String t2 = m.getEquipe2() != null && m.getEquipe2().getNom() != null
                ? m.getEquipe2().getNom() : "Équipe 2";

        String title = (m.getNomMatch() != null && !m.getNomMatch().isBlank())
                ? m.getNomMatch().trim()
                : (t1 + " vs " + t2);

        StringBuilder details = new StringBuilder();
        details.append("Match E-Sports: ").append(t1).append(" vs ").append(t2);
        if (m.getStatut() != null && !m.getStatut().isBlank()) {
            details.append(" | Statut: ").append(m.getStatut());
        }
        if (m.getScoreEquipe1() != null && m.getScoreEquipe2() != null) {
            details.append(" | Score: ").append(m.getScoreEquipe1()).append("-").append(m.getScoreEquipe2());
        }

        try {
            return "https://calendar.google.com/calendar/render?action=TEMPLATE"
                    + "&text=" + java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8)
                    + "&details=" + java.net.URLEncoder.encode(details.toString(), java.nio.charset.StandardCharsets.UTF_8)
                    + "&dates=" + startUtc + "/" + endUtc;
        } catch (Exception e) {
            return "https://calendar.google.com/calendar/render?action=TEMPLATE&text=Match";
        }
    }

    // ================= PDF EXPORT (SANS FENÊTRE D'IMPRESSION) =================
    @FXML
    public void downloadPdf() {
        if (root == null || root.getScene() == null) {
            showError("Impossible d'exporter: scene non disponible.");
            return;
        }

        try {
            // Ouvrir une boîte de dialogue pour choisir l'emplacement de sauvegarde
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le PDF");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf")
            );
            fileChooser.setInitialFileName("classement_equipes_" +
                    java.time.LocalDate.now().toString() + ".pdf");

            File file = fileChooser.showSaveDialog(root.getScene().getWindow());

            if (file == null) {
                // L'utilisateur a annulé
                return;
            }

            // Générer le PDF
            generatePDF(file);

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setHeaderText(null);
            ok.setContentText("PDF généré avec succès !\nEmplacement: " + file.getAbsolutePath());
            ok.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erreur lors de la génération du PDF: " + ex.getMessage());
        }
    }

    private void generatePDF(File file) throws Exception {
        com.itextpdf.text.Document document = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4);
        com.itextpdf.text.pdf.PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // Colors
        com.itextpdf.text.BaseColor primaryColor = new com.itextpdf.text.BaseColor(91, 33, 182); // Purple #5b21b6
        com.itextpdf.text.BaseColor secondaryColor = new com.itextpdf.text.BaseColor(30, 27, 75); // Navy #1e1b4b
        com.itextpdf.text.BaseColor headerBg = new com.itextpdf.text.BaseColor(243, 244, 246); // Light gray #f3f4f6

        // Fonts
        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 22, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE);
        com.itextpdf.text.Font sectionFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD, primaryColor);
        com.itextpdf.text.Font tableHeaderFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, com.itextpdf.text.BaseColor.WHITE);
        com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL);
        com.itextpdf.text.Font smallFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.ITALIC, com.itextpdf.text.BaseColor.GRAY);

        // 1. Header Banner
        com.itextpdf.text.pdf.PdfPTable headerTable = new com.itextpdf.text.pdf.PdfPTable(1);
        headerTable.setWidthPercentage(100);
        com.itextpdf.text.pdf.PdfPCell headerCell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("🏆 CLASSEMENT DES ÉQUIPES E-SPORTS", titleFont));
        headerCell.setBackgroundColor(secondaryColor);
        headerCell.setPadding(20);
        headerCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        headerCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        headerTable.addCell(headerCell);
        document.add(headerTable);

        document.add(new com.itextpdf.text.Paragraph(" "));

        // 2. Info Row (Date & Total)
        com.itextpdf.text.Paragraph infoPara = new com.itextpdf.text.Paragraph();
        infoPara.add(new com.itextpdf.text.Chunk("Généré le: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), smallFont));
        infoPara.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
        document.add(infoPara);

        document.add(new com.itextpdf.text.Paragraph(" "));

        // 3. Key Statistics Cards (Simulated with a table)
        document.add(new com.itextpdf.text.Paragraph("📊 STATISTIQUES CLÉS", sectionFont));
        document.add(new com.itextpdf.text.Paragraph(" "));

        List<StatsRow> filtered = getFilteredDataForPdf();

        if (!filtered.isEmpty()) {
            StatsRow topTeam = filtered.get(0);
            StatsRow mostBp = filtered.stream().max(Comparator.comparingInt(StatsRow::getBp)).orElse(topTeam);
            StatsRow bestDefense = filtered.stream().min(Comparator.comparingInt(StatsRow::getBc)).orElse(topTeam);

            com.itextpdf.text.pdf.PdfPTable statsTable = new com.itextpdf.text.pdf.PdfPTable(3);
            statsTable.setWidthPercentage(100);
            statsTable.setSpacingBefore(10);
            statsTable.setSpacingAfter(20);

            addStatCell(statsTable, "Meilleure Équipe", topTeam.getTeam(), "🏆", headerBg, primaryColor);
            addStatCell(statsTable, "Meilleure Attaque", mostBp.getTeam() + " (" + mostBp.getBp() + " BP)", "🔥", headerBg, primaryColor);
            addStatCell(statsTable, "Meilleure Défense", bestDefense.getTeam() + " (" + bestDefense.getBc() + " BC)", "🛡️", headerBg, primaryColor);
            
            document.add(statsTable);
        }

        // 4. Main Ranking Table
        document.add(new com.itextpdf.text.Paragraph("📈 CLASSEMENT DÉTAILLÉ", sectionFont));
        document.add(new com.itextpdf.text.Paragraph(" "));

        com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 3, 2, 1, 1, 1, 1, 1.5f});

        // Table Headers
        String[] headers = {"Rang", "Équipe", "Jeu", "J", "G", "N", "P", "Points"};
        for (String h : headers) {
            com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(h, tableHeaderFont));
            cell.setBackgroundColor(primaryColor);
            cell.setPadding(8);
            cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Table Data with zebra striping
        int count = 0;
        for (StatsRow row : filtered) {
            com.itextpdf.text.BaseColor rowBg = (count % 2 == 0) ? com.itextpdf.text.BaseColor.WHITE : headerBg;
            
            table.addCell(createDataCell(String.valueOf(row.getRank()), rowBg, com.itextpdf.text.Element.ALIGN_CENTER));
            table.addCell(createDataCell(row.getTeam(), rowBg, com.itextpdf.text.Element.ALIGN_LEFT));
            table.addCell(createDataCell(row.getGame(), rowBg, com.itextpdf.text.Element.ALIGN_LEFT));
            table.addCell(createDataCell(String.valueOf(row.getPlayed()), rowBg, com.itextpdf.text.Element.ALIGN_CENTER));
            table.addCell(createDataCell(String.valueOf(row.getWins()), rowBg, com.itextpdf.text.Element.ALIGN_CENTER));
            table.addCell(createDataCell(String.valueOf(row.getDraws()), rowBg, com.itextpdf.text.Element.ALIGN_CENTER));
            table.addCell(createDataCell(String.valueOf(row.getLosses()), rowBg, com.itextpdf.text.Element.ALIGN_CENTER));
            table.addCell(createDataCell(String.valueOf(row.getPoints()), rowBg, com.itextpdf.text.Element.ALIGN_CENTER));
            
            count++;
            if (count >= 50) break; // Limit to top 50
        }

        document.add(table);

        // 5. Global Results Summary
        if (!filtered.isEmpty()) {
            document.add(new com.itextpdf.text.Paragraph(" "));
            int totalWins = filtered.stream().mapToInt(StatsRow::getWins).sum();
            int totalLosses = filtered.stream().mapToInt(StatsRow::getLosses).sum();
            int totalDraws = filtered.stream().mapToInt(StatsRow::getDraws).sum();

            com.itextpdf.text.Paragraph summaryPara = new com.itextpdf.text.Paragraph();
            summaryPara.setSpacingBefore(15);
            summaryPara.add(new com.itextpdf.text.Chunk("RÉSULTATS GLOBAUX DU GROUPE: ", normalFont));
            summaryPara.add(new com.itextpdf.text.Chunk("G: " + totalWins + " | N: " + totalDraws + " | P: " + totalLosses, sectionFont));
            document.add(summaryPara);
        }

        document.close();
    }

    private List<StatsRow> getFilteredDataForPdf() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String selectedGame = gameFilter.getValue();
        String sort = sortFilter.getValue();

        List<StatsRow> filtered = masterData.stream()
                .filter(r -> r.getTeam().toLowerCase().contains(keyword))
                .filter(r -> selectedGame == null || selectedGame.equals("Tous les jeux") || r.getGame().equalsIgnoreCase(selectedGame))
                .collect(Collectors.toList());

        switch (sort != null ? sort : "Classement officiel") {
            case "Victoires": filtered.sort(Comparator.comparingInt(StatsRow::getWins).reversed()); break;
            case "Défaites": filtered.sort(Comparator.comparingInt(StatsRow::getLosses).reversed()); break;
            case "Différence": filtered.sort(Comparator.comparingInt(StatsRow::getDiff).reversed()); break;
            default: filtered.sort(Comparator.comparingInt(StatsRow::getPoints).reversed()); break;
        }
        return filtered;
    }

    private void addStatCell(com.itextpdf.text.pdf.PdfPTable table, String label, String value, String icon, com.itextpdf.text.BaseColor bg, com.itextpdf.text.BaseColor accent) {
        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setPadding(10);
        cell.setBorderColor(com.itextpdf.text.BaseColor.LIGHT_GRAY);

        com.itextpdf.text.Paragraph p = new com.itextpdf.text.Paragraph(icon + " " + label, new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL, com.itextpdf.text.BaseColor.GRAY));
        cell.addElement(p);
        
        com.itextpdf.text.Paragraph v = new com.itextpdf.text.Paragraph(value, new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD, accent));
        cell.addElement(v);
        
        table.addCell(cell);
    }

    private com.itextpdf.text.pdf.PdfPCell createDataCell(String text, com.itextpdf.text.BaseColor bg, int align) {
        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(text, new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9)));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(new com.itextpdf.text.BaseColor(229, 231, 235)); // #e5e7eb
        return cell;
    }
}