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

    @FXML private VBox matchContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label countLabel;

    private final MatchService service = new MatchService();
    private final esprit.tn.esports.service.PredictionService predictionService = new esprit.tn.esports.service.PredictionService();
    private List<Matchs> allMatchs = new ArrayList<>();
    
    // For live goal tracking
    private java.util.Map<Integer, Matchs> previousMatches = new java.util.HashMap<>();
    private java.util.Map<Integer, Long> team1Goals = new java.util.HashMap<>();
    private java.util.Map<Integer, Long> team2Goals = new java.util.HashMap<>();
    private javafx.animation.Timeline autoRefreshTimer;
    private javafx.animation.Timeline chronoTimeline;


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

        // Initial load
        allMatchs = service.getAll();
        for (Matchs m : allMatchs) {
            previousMatches.put(m.getId(), m);
        }
        search();

        // Start auto-refresh timer (every 5 seconds)
        autoRefreshTimer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5), e -> checkForLiveUpdates()));
        autoRefreshTimer.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        autoRefreshTimer.play();

        // ⏱️ Chronomètre live (mise à jour chaque seconde)
        chronoTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> updateChronometers())
        );
        chronoTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        chronoTimeline.play();
    }

    private void checkForLiveUpdates() {
        // Cette méthode peut être appelée par un Timeline: on force l'exécution sur le thread JavaFX
        if (!javafx.application.Platform.isFxApplicationThread()) {
            javafx.application.Platform.runLater(this::checkForLiveUpdates);
            return;
        }

        List<Matchs> newMatches = service.getAll();
        boolean uiNeedsUpdate = false;

        for (Matchs newM : newMatches) {
            Matchs oldM = previousMatches.get(newM.getId());
            if (oldM != null) {
                boolean scoreChanged = false;
                if (newM.getScoreEquipe1() > oldM.getScoreEquipe1()) {
                    team1Goals.put(newM.getId(), System.currentTimeMillis());
                    scoreChanged = true;
                } 
                if (newM.getScoreEquipe2() > oldM.getScoreEquipe2()) {
                    team2Goals.put(newM.getId(), System.currentTimeMillis());
                    scoreChanged = true;
                }
                
                // If status changed or anything else changed, we should update UI anyway
                if (scoreChanged || !String.valueOf(newM.getStatut()).equals(String.valueOf(oldM.getStatut()))) {
                    uiNeedsUpdate = true;
                }
            } else {
                // New match added
                uiNeedsUpdate = true;
            }
            previousMatches.put(newM.getId(), newM);
        }

        if (uiNeedsUpdate) {
            allMatchs = newMatches;
            // Get scroll pane safely to prevent ClassCastException
            ScrollPane sp = null;
            javafx.scene.Node p = matchContainer.getParent();
            while (p != null) {
                if (p instanceof ScrollPane) {
                    sp = (ScrollPane) p;
                    break;
                }
                p = p.getParent();
            }
            
            double vVal = (sp != null) ? sp.getVvalue() : 0.0;
            
            search(); // This rebuilds the UI
            
            // Restore scroll position
            if (sp != null) {
                ScrollPane finalSp = sp;
                javafx.application.Platform.runLater(() -> finalSp.setVvalue(vVal));
            }
        }
    }

    // ================= CHRONOMETER UPDATE (CLIENT) =================
    private void updateChronometers() {
        if (matchContainer == null) return;

        // Met à jour tous les labels de chrono présents dans la liste
        for (javafx.scene.Node node : matchContainer.lookupAll(".chrono-timer-label")) {
            if (!(node instanceof Label)) continue;
            Label label = (Label) node;

            Object p = label.getProperties().get("match");
            if (!(p instanceof Matchs)) continue;
            Matchs m = (Matchs) p;

            if (m.getDateMatch() == null) {
                label.setText("00:00");
                continue;
            }

            long seconds = java.time.Duration.between(m.getDateMatch(), java.time.LocalDateTime.now()).toSeconds();
            if (seconds < 0) seconds = 0;
            // Option: plafonner à 90 minutes (affichage uniquement côté client)
            if (seconds > 90 * 60) seconds = 90 * 60;

            long mins = seconds / 60;
            long secs = seconds % 60;
            label.setText(String.format("%02d:%02d", mins, secs));
        }
    }

    // ================= REFRESH =================
    @FXML
    public void refresh() {
        // Manually trigger the live update check so we don't miss any goals
        checkForLiveUpdates();
        // Force a re-render just in case
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

                    String dbStatus = m.getStatut() != null ? m.getStatut().toLowerCase() : "";

                    if (statut.equals("Terminé")) {
                        return dbStatus.contains("termine") || dbStatus.contains("terminé");
                    }

                    if (statut.equals("À jouer")) {
                        return dbStatus.contains("a jouer");
                    }

                    if (statut.equals("En cours")) {
                        return dbStatus.contains("en_cours");
                    }

                    if (statut.equals("Annulé")) {
                        return dbStatus.contains("annule") || dbStatus.contains("annulé");
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
    // ================= CARD (Horizontal Scoreboard Row) =================
    private HBox createCard(Matchs m) {

        HBox row = new HBox(0); 
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

        VBox nameBox1 = new VBox(5);
        nameBox1.setAlignment(Pos.CENTER_LEFT);
        nameBox1.getChildren().add(name1);

        team1.getChildren().addAll(img1, nameBox1);

        // Goal Alert Team 1
        long now = System.currentTimeMillis();
        Long t1GoalTime = team1Goals.get(m.getId());
        if (t1GoalTime != null && (now - t1GoalTime) < 120000) { // 2 minutes = 120,000 ms
            long remainingMs = 120000 - (now - t1GoalTime);
            int cycles = (int) (remainingMs / 800); // Calculate remaining flashes

            Label goalAlert = new Label("⚽ BUT MARQUÉ !");
            goalAlert.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 6;");
            nameBox1.getChildren().add(goalAlert);

            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(800), goalAlert);
            ft.setFromValue(1.0);
            ft.setToValue(0.2);
            ft.setCycleCount(cycles > 0 ? cycles : 1); 
            ft.setAutoReverse(true);
            ft.setOnFinished(ev -> nameBox1.getChildren().remove(goalAlert));
            ft.play();
        }

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

        // ⏱️ Ajouter le chronomètre si le match est en cours
        if (currentStatus.equals("EN_COURS")) {
            Label chronoLabel = new Label("00:00");
            chronoLabel.getStyleClass().add("chrono-timer-label");
            chronoLabel.getProperties().put("match", m);
            chronoLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', system-ui, sans-serif; -fx-font-size: 13px;");

            // calcul initial
            if (m.getDateMatch() != null) {
                long seconds = java.time.Duration.between(m.getDateMatch(), java.time.LocalDateTime.now()).toSeconds();
                if (seconds < 0) seconds = 0;
                if (seconds > 90 * 60) seconds = 90 * 60;
                chronoLabel.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
            }

            center.getChildren().addAll(status, chronoLabel, matchLabel, score);
        } else {
            center.getChildren().addAll(status, matchLabel, score);
        }

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

        VBox nameBox2 = new VBox(5);
        nameBox2.setAlignment(Pos.CENTER_RIGHT);
        nameBox2.getChildren().add(name2);

        // Goal Alert Team 2
        Long t2GoalTime = team2Goals.get(m.getId());
        if (t2GoalTime != null && (now - t2GoalTime) < 120000) { // 2 minutes
            long remainingMs = 120000 - (now - t2GoalTime);
            int cycles = (int) (remainingMs / 800);

            Label goalAlert = new Label("⚽ BUT MARQUÉ !");
            goalAlert.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 6;");
            nameBox2.getChildren().add(goalAlert); // Add below name

            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(800), goalAlert);
            ft.setFromValue(1.0);
            ft.setToValue(0.2);
            ft.setCycleCount(cycles > 0 ? cycles : 1);
            ft.setAutoReverse(true);
            ft.setOnFinished(ev -> nameBox2.getChildren().remove(goalAlert));
            ft.play();
        }

        team2.getChildren().addAll(nameBox2, img2);

        // --- ACTIONS ---
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(0, 20, 0, 10));

        Button viewBtn = new Button("Voir Match");
        viewBtn.getStyleClass().add("btn-view");
        viewBtn.setPrefWidth(120);
        viewBtn.setPrefHeight(40);
        viewBtn.setOnAction(e -> openDetailsPage(m));

        actions.getChildren().add(viewBtn);

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


    // ================= PREDICTION DIALOG =================
    private void showPredictionDialog(Matchs m) {
        esprit.tn.esports.service.PredictionService.PredictionResult result = predictionService.predictWinner(m);

        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        if (matchContainer.getScene() != null && matchContainer.getScene().getWindow() != null) {
            dialog.initOwner(matchContainer.getScene().getWindow());
        }

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setStyle(
            "-fx-background-color: #0f172a;" +
            "-fx-background-radius: 18;" +
            "-fx-border-color: #7c3aed;" +
            "-fx-border-radius: 18;" +
            "-fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.5), 25, 0.3, 0, 4);"
        );

        Label title = new Label("🔮 Prédiction IA");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        
        Label matchName = new Label(m.getNomMatch() != null ? m.getNomMatch() : "Match à venir");
        matchName.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Teams Row
        HBox teamsRow = new HBox(30);
        teamsRow.setAlignment(Pos.CENTER);
        
        VBox t1Box = new VBox(8);
        t1Box.setAlignment(Pos.CENTER);
        ImageView img1 = new ImageView();
        img1.setFitWidth(60); img1.setFitHeight(60);
        setTeamLogo(img1, m.getEquipe1());
        javafx.scene.shape.Circle clip1 = new javafx.scene.shape.Circle(30, 30, 30);
        img1.setClip(clip1);
        Label t1Name = new Label(m.getEquipe1() != null ? m.getEquipe1().getNom() : "Eq1");
        t1Name.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label t1Prob = new Label(result.probTeam1 + "%");
        t1Prob.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 24px; -fx-font-weight: bold;");
        t1Box.getChildren().addAll(img1, t1Name, t1Prob);

        Label vs = new Label("VS");
        vs.setStyle("-fx-text-fill: #64748b; -fx-font-size: 18px; -fx-font-weight: bold;");

        VBox t2Box = new VBox(8);
        t2Box.setAlignment(Pos.CENTER);
        ImageView img2 = new ImageView();
        img2.setFitWidth(60); img2.setFitHeight(60);
        setTeamLogo(img2, m.getEquipe2());
        javafx.scene.shape.Circle clip2 = new javafx.scene.shape.Circle(30, 30, 30);
        img2.setClip(clip2);
        Label t2Name = new Label(m.getEquipe2() != null ? m.getEquipe2().getNom() : "Eq2");
        t2Name.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label t2Prob = new Label(result.probTeam2 + "%");
        t2Prob.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 24px; -fx-font-weight: bold;");
        t2Box.getChildren().addAll(img2, t2Name, t2Prob);

        teamsRow.getChildren().addAll(t1Box, vs, t2Box);

        // Progress Bar
        HBox barContainer = new HBox(0);
        barContainer.setPrefHeight(12);
        barContainer.setPrefWidth(300);
        barContainer.setStyle("-fx-background-radius: 6; -fx-border-radius: 6;");
        
        Region bar1 = new Region();
        bar1.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 6 0 0 6;");
        bar1.setPrefWidth(300 * (result.probTeam1 / 100.0));
        
        Region bar2 = new Region();
        bar2.setStyle("-fx-background-color: #3b82f6; -fx-background-radius: 0 6 6 0;");
        bar2.setPrefWidth(300 * (result.probTeam2 / 100.0));
        
        barContainer.getChildren().addAll(bar1, bar2);

        Label conclusion = new Label(result.favoriteName.equals("Match Nul") ? "Pronostic : Match extrêmement serré" : "L'équipe " + result.favoriteName + " est favorite !");
        conclusion.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 15px; -fx-font-weight: bold;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(title, matchName, teamsRow, barContainer, conclusion, closeBtn);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ================= SIMULATOR DIALOG =================
    @FXML
    public void openSimulatorDialog(ActionEvent event) {
        esprit.tn.esports.service.EquipeService eqService = new esprit.tn.esports.service.EquipeService();
        List<Equipe> allEquipes = eqService.getAll();

        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        if (matchContainer.getScene() != null && matchContainer.getScene().getWindow() != null) {
            dialog.initOwner(matchContainer.getScene().getWindow());
        }

        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(35, 45, 35, 45));
        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #0f172a, #1e1b4b);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: linear-gradient(to right, #8b5cf6, #d946ef);" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 2.5;" +
            "-fx-effect: dropshadow(gaussian, rgba(139,92,246,0.6), 35, 0.4, 0, 8);"
        );

        Label title = new Label("🔮 Simulateur IA");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        
        Label sub = new Label("Sélectionnez deux équipes pour prédire le gagnant");
        sub.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px; -fx-font-family: 'Segoe UI';");

        HBox selectionRow = new HBox(25);
        selectionRow.setAlignment(Pos.CENTER);

        String comboStyle = "-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-weight: bold; -fx-pref-width: 170; -fx-pref-height: 40; -fx-background-radius: 8;";

        ComboBox<Equipe> combo1 = new ComboBox<>(FXCollections.observableArrayList(allEquipes));
        combo1.setPromptText("Sélectionner Équipe 1");
        combo1.setStyle(comboStyle);
        combo1.setButtonCell(new javafx.scene.control.ListCell<Equipe>() {
            @Override protected void updateItem(Equipe item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText("Sélectionner Équipe 1"); setStyle("-fx-text-fill: white; -fx-font-weight: bold;"); }
                else { setText(item.getNom()); setStyle("-fx-text-fill: white; -fx-font-weight: bold;"); }
            }
        });
        combo1.setCellFactory(lv -> new javafx.scene.control.ListCell<Equipe>() {
            @Override protected void updateItem(Equipe item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item.getNom()); setStyle("-fx-text-fill: black; -fx-font-weight: bold;"); }
            }
        });

        Label vs = new Label("VS");
        vs.setStyle("-fx-text-fill: #f472b6; -fx-font-size: 20px; -fx-font-weight: 900; -fx-font-style: italic;");

        ComboBox<Equipe> combo2 = new ComboBox<>(FXCollections.observableArrayList(allEquipes));
        combo2.setPromptText("Sélectionner Équipe 2");
        combo2.setStyle(comboStyle);
        combo2.setButtonCell(new javafx.scene.control.ListCell<Equipe>() {
            @Override protected void updateItem(Equipe item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText("Sélectionner Équipe 2"); setStyle("-fx-text-fill: white; -fx-font-weight: bold;"); }
                else { setText(item.getNom()); setStyle("-fx-text-fill: white; -fx-font-weight: bold;"); }
            }
        });
        combo2.setCellFactory(lv -> new javafx.scene.control.ListCell<Equipe>() {
            @Override protected void updateItem(Equipe item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else { setText(item.getNom()); setStyle("-fx-text-fill: black; -fx-font-weight: bold;"); }
            }
        });

        selectionRow.getChildren().addAll(combo1, vs, combo2);

        VBox resultBox = new VBox(20);
        resultBox.setAlignment(Pos.CENTER);
        resultBox.setVisible(false);
        resultBox.setManaged(false);

        Button predictBtn = new Button("🚀 Lancer l'IA");
        predictBtn.setStyle("-fx-background-color: linear-gradient(to right, #10b981, #059669); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(16,185,129,0.4), 10, 0, 0, 2);");
        
        predictBtn.setOnMouseEntered(e -> predictBtn.setStyle("-fx-background-color: linear-gradient(to right, #34d399, #10b981); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(16,185,129,0.6), 15, 0, 0, 3);"));
        predictBtn.setOnMouseExited(e -> predictBtn.setStyle("-fx-background-color: linear-gradient(to right, #10b981, #059669); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(16,185,129,0.4), 10, 0, 0, 2);"));

        predictBtn.setOnAction(e -> {
            Equipe eq1 = combo1.getValue();
            Equipe eq2 = combo2.getValue();
            if (eq1 == null || eq2 == null || eq1.getId() == eq2.getId()) {
                sub.setText("⚠️ Veuillez sélectionner deux équipes différentes !");
                sub.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 14px; -fx-font-weight: bold;");
                return;
            }
            sub.setText("Analyse IA terminée ✅");
            sub.setStyle("-fx-text-fill: #34d399; -fx-font-size: 14px; -fx-font-weight: bold;");

            esprit.tn.esports.service.PredictionService.PredictionResult res = predictionService.predictWinner(eq1, eq2);

            resultBox.getChildren().clear();

            HBox teamsRow = new HBox(40);
            teamsRow.setAlignment(Pos.CENTER);
            
            VBox t1Box = new VBox(10);
            t1Box.setAlignment(Pos.CENTER);
            ImageView img1 = new ImageView();
            img1.setFitWidth(70); img1.setFitHeight(70);
            setTeamLogo(img1, eq1);
            javafx.scene.shape.Circle clip1 = new javafx.scene.shape.Circle(35, 35, 35);
            img1.setClip(clip1);
            Label t1Name = new Label(eq1.getNom());
            t1Name.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            Label t1Prob = new Label(res.probTeam1 + "%");
            t1Prob.setStyle("-fx-text-fill: #34d399; -fx-font-size: 28px; -fx-font-weight: 900; -fx-effect: dropshadow(gaussian, rgba(52,211,153,0.5), 10, 0, 0, 0);");
            t1Box.getChildren().addAll(img1, t1Name, t1Prob);

            Label vsRes = new Label("VS");
            vsRes.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 22px; -fx-font-weight: 900; -fx-font-style: italic;");

            VBox t2Box = new VBox(10);
            t2Box.setAlignment(Pos.CENTER);
            ImageView img2 = new ImageView();
            img2.setFitWidth(70); img2.setFitHeight(70);
            setTeamLogo(img2, eq2);
            javafx.scene.shape.Circle clip2 = new javafx.scene.shape.Circle(35, 35, 35);
            img2.setClip(clip2);
            Label t2Name = new Label(eq2.getNom());
            t2Name.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            Label t2Prob = new Label(res.probTeam2 + "%");
            t2Prob.setStyle("-fx-text-fill: #60a5fa; -fx-font-size: 28px; -fx-font-weight: 900; -fx-effect: dropshadow(gaussian, rgba(96,165,250,0.5), 10, 0, 0, 0);");
            t2Box.getChildren().addAll(img2, t2Name, t2Prob);

            teamsRow.getChildren().addAll(t1Box, vsRes, t2Box);

            HBox barContainer = new HBox(0);
            barContainer.setPrefHeight(16);
            barContainer.setPrefWidth(350);
            barContainer.setStyle("-fx-background-color: #334155; -fx-background-radius: 8; -fx-border-radius: 8;");
            
            Region bar1 = new Region();
            bar1.setStyle("-fx-background-color: linear-gradient(to right, #10b981, #34d399); -fx-background-radius: 8 0 0 8;");
            bar1.setPrefWidth(350 * (res.probTeam1 / 100.0));
            if(res.probTeam1 == 100) bar1.setStyle("-fx-background-color: linear-gradient(to right, #10b981, #34d399); -fx-background-radius: 8;");
            
            Region bar2 = new Region();
            bar2.setStyle("-fx-background-color: linear-gradient(to right, #3b82f6, #60a5fa); -fx-background-radius: 0 8 8 0;");
            bar2.setPrefWidth(350 * (res.probTeam2 / 100.0));
            if(res.probTeam2 == 100) bar2.setStyle("-fx-background-color: linear-gradient(to right, #3b82f6, #60a5fa); -fx-background-radius: 8;");
            
            barContainer.getChildren().addAll(bar1, bar2);

            Label conclusion = new Label(res.favoriteName.equals("Match Nul") ? "⚖️ Pronostic : Match extrêmement serré" : "🏆 L'équipe " + res.favoriteName + " est grande favorite !");
            conclusion.setStyle("-fx-text-fill: #fcd34d; -fx-font-size: 16px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(252,211,77,0.3), 5, 0, 0, 1);");

            resultBox.getChildren().addAll(teamsRow, barContainer, conclusion);
            resultBox.setVisible(true);
            resultBox.setManaged(true);
            dialog.sizeToScene(); // Force window to resize
        });

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-background-radius: 10; -fx-padding: 10 25; -fx-cursor: hand;"));
        closeBtn.setOnAction(e -> dialog.close());

        HBox btns = new HBox(20, predictBtn, closeBtn);
        btns.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, sub, selectionRow, btns, resultBox);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
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

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
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