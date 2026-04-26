package esprit.tn.esports.controller;

import esprit.tn.esports.entite.StatsRow;
import esprit.tn.esports.entite.Matchs;
import esprit.tn.esports.service.MatchService;
import esprit.tn.esports.service.StatsService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.chart.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

    private final StatsService service = new StatsService();
    private final MatchService matchService = new MatchService();

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
        goalsChart.getData().add(bpSeries);
        goalsChart.getData().add(bcSeries);
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

    @FXML
    public void exportLast10YearsToGoogleCalendar() {
        if (root == null || root.getScene() == null) {
            showError("Impossible d'exporter: scene non disponible.");
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime tenYearsAgo = now.minusYears(10);

            List<Matchs> matches = matchService.getAll().stream()
                    .filter(m -> m.getDateMatch() != null)
                    .filter(m -> !m.getDateMatch().isBefore(tenYearsAgo) && !m.getDateMatch().isAfter(now))
                    .collect(Collectors.toList());

            if (matches.isEmpty()) {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setHeaderText(null);
                info.setContentText("Aucun match trouvé sur les 10 dernières années.");
                info.showAndWait();
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exporter vers Google Calendar (CSV)");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv")
            );
            fileChooser.setInitialFileName("matches_last_10_years_" + LocalDate.now() + ".csv");

            File file = fileChooser.showSaveDialog(root.getScene().getWindow());
            if (file == null) return;

            writeGoogleCalendarCsv(file, matches);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("https://calendar.google.com/calendar/u/0/r/settings/import"));
            }

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setHeaderText("Export terminé");
            ok.setContentText(
                    "Fichier CSV généré avec succès (" + matches.size() + " matchs).\n\n" +
                    "Google Calendar Import s'est ouvert.\n" +
                    "Choisissez le fichier CSV exporté pour importer tous les matchs."
            );
            ok.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur export Google Calendar: " + e.getMessage());
        }
    }

    private void writeGoogleCalendarCsv(File file, List<Matchs> matches) throws Exception {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US);
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

        try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
            // BOM UTF-8 aide certains imports Google Calendar sur Windows.
            writer.print('\uFEFF');
            writer.println("Subject,Start Date,Start Time,End Date,End Time,All Day Event,Description,Location,Private");

            for (Matchs m : matches) {
                LocalDateTime start = m.getDateMatch();
                LocalDateTime end = m.getDateFinMatch() != null ? m.getDateFinMatch() : start.plusHours(2);

                String e1 = m.getEquipe1() != null ? m.getEquipe1().getNom() : "Equipe 1";
                String e2 = m.getEquipe2() != null ? m.getEquipe2().getNom() : "Equipe 2";
                String subject = (m.getNomMatch() == null || m.getNomMatch().isBlank())
                        ? (e1 + " vs " + e2)
                        : m.getNomMatch();
                String description = "Match e-sports: " + e1 + " vs " + e2 +
                        " | Score: " + m.getScoreEquipe1() + "-" + m.getScoreEquipe2() +
                        " | Statut: " + (m.getStatut() != null ? m.getStatut() : "N/A");

                writer.println(toCsv(sanitizeForGoogleCsv(subject)) + "," +
                        toCsv(start.toLocalDate().format(dateFmt)) + "," +
                        toCsv(start.toLocalTime().format(timeFmt)) + "," +
                        toCsv(end.toLocalDate().format(dateFmt)) + "," +
                        toCsv(end.toLocalTime().format(timeFmt)) + "," +
                        "False," +
                        toCsv(sanitizeForGoogleCsv(description)) + "," +
                        toCsv("ClutchX Esports") + "," +
                        "False");
            }
        }
    }

    private String toCsv(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String sanitizeForGoogleCsv(String value) {
        if (value == null) return "";
        return value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace("🏆", " ")
                .replace("🔥", " ")
                .replace("🛡️", " ");
    }

    private void generatePDF(File file) throws Exception {
        // Créer le document PDF
        com.itextpdf.text.Document document = new com.itextpdf.text.Document();
        com.itextpdf.text.pdf.PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // Titre
        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD
        );
        com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("🏆 Classement des équipes", titleFont);
        title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        document.add(title);

        document.add(new com.itextpdf.text.Paragraph(" "));

        // Date
        com.itextpdf.text.Paragraph datePara = new com.itextpdf.text.Paragraph(
                "Généré le: " + new java.util.Date().toString(),
                new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10)
        );
        document.add(datePara);

        document.add(new com.itextpdf.text.Paragraph(" "));

        // Récupérer les données filtrées
        String keyword = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String selectedGame = gameFilter.getValue();
        String sort = sortFilter.getValue();

        List<StatsRow> filtered = masterData.stream()
                .filter(r -> r.getTeam().toLowerCase().contains(keyword))
                .filter(r -> selectedGame == null
                        || selectedGame.equals("Tous les jeux")
                        || r.getGame().equalsIgnoreCase(selectedGame))
                .collect(Collectors.toList());

        // Appliquer le tri
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

        // Cartes de statistiques
        if (!filtered.isEmpty()) {
            StatsRow topTeam = filtered.get(0);
            StatsRow mostBp = filtered.stream().max(Comparator.comparingInt(StatsRow::getBp)).orElse(topTeam);
            StatsRow bestDefense = filtered.stream().min(Comparator.comparingInt(StatsRow::getBc)).orElse(topTeam);

            document.add(new com.itextpdf.text.Paragraph("📊 STATISTIQUES CLÉS",
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD)));
            document.add(new com.itextpdf.text.Paragraph(" "));

            document.add(new com.itextpdf.text.Paragraph("🏆 Meilleure équipe: " + topTeam.getTeam()));
            document.add(new com.itextpdf.text.Paragraph("🔥 Meilleure attaque: " + mostBp.getTeam() + " (" + mostBp.getBp() + " BP)"));
            document.add(new com.itextpdf.text.Paragraph("🛡️ Meilleure défense: " + bestDefense.getTeam() + " (" + bestDefense.getBc() + " BC)"));
            document.add(new com.itextpdf.text.Paragraph(" "));
        }

        // Tableau des équipes
        com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(8);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        // En-têtes
        String[] headers = {"Rang", "Équipe", "Jeu", "J", "G", "N", "P", "Points"};
        for (String header : headers) {
            com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                    new com.itextpdf.text.Phrase(header,
                            new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD))
            );
            cell.setBackgroundColor(com.itextpdf.text.BaseColor.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }

        // Données
        for (StatsRow row : filtered.stream().limit(20).collect(Collectors.toList())) {
            table.addCell(String.valueOf(row.getRank()));
            table.addCell(row.getTeam());
            table.addCell(row.getGame());
            table.addCell(String.valueOf(row.getPlayed()));
            table.addCell(String.valueOf(row.getWins()));
            table.addCell(String.valueOf(row.getDraws()));
            table.addCell(String.valueOf(row.getLosses()));
            table.addCell(String.valueOf(row.getPoints()));
        }

        document.add(table);

        // Ajouter le total
        document.add(new com.itextpdf.text.Paragraph(" "));
        document.add(new com.itextpdf.text.Paragraph("Total équipes: " + filtered.size()));

        // Résultats globaux
        if (!filtered.isEmpty()) {
            int totalWins = filtered.stream().mapToInt(StatsRow::getWins).sum();
            int totalLosses = filtered.stream().mapToInt(StatsRow::getLosses).sum();
            int totalDraws = filtered.stream().mapToInt(StatsRow::getDraws).sum();

            document.add(new com.itextpdf.text.Paragraph(" "));
            document.add(new com.itextpdf.text.Paragraph("📈 RÉSULTATS GLOBAUX",
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD)));
            document.add(new com.itextpdf.text.Paragraph("Victoires: " + totalWins));
            document.add(new com.itextpdf.text.Paragraph("Défaites: " + totalLosses));
            document.add(new com.itextpdf.text.Paragraph("Matchs nuls: " + totalDraws));
        }

        document.close();
    }
}