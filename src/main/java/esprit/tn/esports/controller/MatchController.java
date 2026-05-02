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
import java.time.Duration;
import java.time.LocalDateTime;
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
    @FXML private Button notifButton;
    private List<Matchs> pendingMatches = new ArrayList<>();

    private final MatchService service = new MatchService();
    private List<Matchs> allMatchs = new ArrayList<>();
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

        refresh();

        // 🔔 Démarrer le service de notification
        esprit.tn.esports.utils.MatchNotificationService notifService = esprit.tn.esports.utils.MatchNotificationService.getInstance();
        
        notifService.setOnMatchWarning(m -> {
            try {
                System.out.println("[MatchController] ⏳ WARNING reçu: " + m.getNomMatch());
                pushNotification(m);
                esprit.tn.esports.utils.MatchNotificationService.playAlertSound();
                String e1 = m.getEquipe1() != null ? m.getEquipe1().getNom() : "Eq1";
                String e2 = m.getEquipe2() != null ? m.getEquipe2().getNom() : "Eq2";
                Stage window = getActiveStage();
                if (window != null) {
                    esprit.tn.esports.utils.MatchNotificationService.showToast(
                        window, 
                        "⏳ Match dans 15 min", 
                        "🔊 " + m.getNomMatch() + " (" + e1 + " VS " + e2 + ") va bientôt démarrer !"
                    );
                }
            } catch (Exception ex) {
                System.err.println("[MatchController] Erreur warning callback:");
                ex.printStackTrace();
            }
        });

        notifService.setOnMatchStarted(m -> {
            try {
                System.out.println("[MatchController] 🔴 STARTED reçu: " + m.getNomMatch());
                pushNotification(m);
                esprit.tn.esports.utils.MatchNotificationService.playAlertSound();
                String e1 = m.getEquipe1() != null ? m.getEquipe1().getNom() : "Eq1";
                String e2 = m.getEquipe2() != null ? m.getEquipe2().getNom() : "Eq2";
                Stage window = getActiveStage();
                if (window != null) {
                    esprit.tn.esports.utils.MatchNotificationService.showToast(
                        window, 
                        "🔴 Match Démarré !", 
                        "🔊 " + m.getNomMatch() + " (" + e1 + " VS " + e2 + ") est maintenant EN COURS !"
                    );
                }
                refresh();
            } catch (Exception ex) {
                System.err.println("[MatchController] Erreur started callback:");
                ex.printStackTrace();
            }
        });
        notifService.start();

        // ⏱️ Démarrer le chronomètre pour les matchs en cours
        chronoTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> updateChronometers())
        );
        chronoTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        chronoTimeline.play();
    }

    // Récupère la fenêtre active même si la scène change
    private Stage getActiveStage() {
        try {
            if (mainBorderPane != null && mainBorderPane.getScene() != null 
                    && mainBorderPane.getScene().getWindow() != null
                    && mainBorderPane.getScene().getWindow().isShowing()) {
                return (Stage) mainBorderPane.getScene().getWindow();
            }
            // Fallback: chercher n'importe quelle fenêtre JavaFX ouverte
            for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
                if (w instanceof Stage && w.isShowing()) {
                    return (Stage) w;
                }
            }
        } catch (Exception e) {
            System.err.println("[MatchController] Erreur getActiveStage: " + e.getMessage());
        }
        return null;
    }

    // ---------- NOTIFICATIONS LOGIC ----------
    public void pushNotification(Matchs m) {
        javafx.application.Platform.runLater(() -> {
            if (!pendingMatches.contains(m)) {
                pendingMatches.add(m);
            }
            int count = pendingMatches.size();
            if (notifButton != null) {
                notifButton.setText("🔔 " + count);
                notifButton.setStyle("-fx-background-color: #f43f5e; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8px;");
                Tooltip tip = new Tooltip(count + " nouvelle(s) notification(s)");
                Tooltip.install(notifButton, tip);
            }
        });
    }

    @FXML
    private void openNotifications() {
        if (!pendingMatches.isEmpty()) {
            // === Custom Notification Center Dialog ===
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            if (mainBorderPane.getScene() != null && mainBorderPane.getScene().getWindow() != null) {
                dialog.initOwner(mainBorderPane.getScene().getWindow());
            }

            VBox root = new VBox(0);
            root.setStyle(
                "-fx-background-color: #0f172a;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #7c3aed;" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 2;" +
                "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.6), 30, 0.3, 0, 6);"
            );
            root.setPrefWidth(420);
            root.setMaxWidth(420);

            // -- HEADER --
            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(20, 24, 16, 24));
            header.setStyle("-fx-border-color: transparent transparent rgba(124,58,237,0.2) transparent; -fx-border-width: 0 0 1 0;");

            Label bellIcon = new Label("🔔");
            bellIcon.setStyle("-fx-font-size: 22px;");

            VBox headerText = new VBox(2);
            Label titleL = new Label("Centre de Notifications");
            titleL.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;");
            Label subtitleL = new Label(pendingMatches.size() + " match(s) en direct");
            subtitleL.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 12px; -fx-font-weight: bold;");
            headerText.getChildren().addAll(titleL, subtitleL);
            HBox.setHgrow(headerText, Priority.ALWAYS);

            Label liveTag = new Label("🔴 LIVE");
            liveTag.setStyle(
                "-fx-text-fill: #f43f5e; -fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-background-color: rgba(244,63,94,0.15); -fx-background-radius: 8; -fx-padding: 4 10 4 10;"
            );

            Label closeLabel = new Label("✕");
            closeLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 18px; -fx-cursor: hand;");
            closeLabel.setOnMouseClicked(e -> dialog.close());
            closeLabel.setOnMouseEntered(e -> closeLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 18px; -fx-cursor: hand;"));
            closeLabel.setOnMouseExited(e -> closeLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 18px; -fx-cursor: hand;"));

            header.getChildren().addAll(bellIcon, headerText, liveTag, closeLabel);

            // -- MATCH CARDS --
            VBox cardsContainer = new VBox(12);
            cardsContainer.setPadding(new Insets(16, 20, 16, 20));

            for (Matchs m : pendingMatches) {
                String e1 = m.getEquipe1() != null ? m.getEquipe1().getNom() : "Équipe 1";
                String e2 = m.getEquipe2() != null ? m.getEquipe2().getNom() : "Équipe 2";

                VBox card = new VBox(10);
                card.setPadding(new Insets(14, 18, 14, 18));
                card.setStyle(
                    "-fx-background-color: rgba(30,41,59,0.8);" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-color: rgba(124,58,237,0.3);" +
                    "-fx-border-radius: 12;" +
                    "-fx-border-width: 1;"
                );

                // Match name row
                HBox nameRow = new HBox(8);
                nameRow.setAlignment(Pos.CENTER_LEFT);
                Label trophy = new Label("🏆");
                trophy.setStyle("-fx-font-size: 16px;");
                Label matchName = new Label(m.getNomMatch());
                matchName.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
                Label statusBadge = new Label("EN COURS");
                statusBadge.setStyle(
                    "-fx-text-fill: #22c55e; -fx-font-size: 10px; -fx-font-weight: bold;" +
                    "-fx-background-color: rgba(34,197,94,0.15); -fx-background-radius: 6; -fx-padding: 2 8 2 8;"
                );
                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                nameRow.getChildren().addAll(trophy, matchName, sp, statusBadge);

                // Teams VS row
                HBox vsRow = new HBox(12);
                vsRow.setAlignment(Pos.CENTER);
                vsRow.setPadding(new Insets(6, 0, 4, 0));

                Label team1Label = new Label(e1);
                team1Label.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 14px; -fx-font-weight: bold;");
                Label vsLabel = new Label("⚔️  VS  ⚔️");
                vsLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: bold;");
                Label team2Label = new Label(e2);
                team2Label.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 14px; -fx-font-weight: bold;");

                Region sp1 = new Region();
                HBox.setHgrow(sp1, Priority.ALWAYS);
                Region sp2 = new Region();
                HBox.setHgrow(sp2, Priority.ALWAYS);
                vsRow.getChildren().addAll(team1Label, sp1, vsLabel, sp2, team2Label);

                // Time row
                Label timeInfo = new Label("🔊 Notification reçue · Maintenant");
                timeInfo.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

                card.getChildren().addAll(nameRow, vsRow, timeInfo);
                cardsContainer.getChildren().add(card);
            }

            ScrollPane scrollPane = new ScrollPane(cardsContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setMaxHeight(320);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

            // -- FOOTER --
            HBox footer = new HBox(12);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.setPadding(new Insets(14, 24, 18, 24));
            footer.setStyle("-fx-border-color: rgba(124,58,237,0.2) transparent transparent transparent; -fx-border-width: 1 0 0 0;");

            Button clearBtn = new Button("✓ Tout marquer comme lu");
            clearBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #7c3aed, #6d28d9);" +
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-radius: 10; -fx-padding: 10 24 10 24; -fx-cursor: hand;"
            );
            clearBtn.setOnMouseEntered(e -> clearBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #8b5cf6, #7c3aed);" +
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-radius: 10; -fx-padding: 10 24 10 24; -fx-cursor: hand;"
            ));
            clearBtn.setOnMouseExited(e -> clearBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #7c3aed, #6d28d9);" +
                "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
                "-fx-background-radius: 10; -fx-padding: 10 24 10 24; -fx-cursor: hand;"
            ));
            clearBtn.setOnAction(e -> {
                pendingMatches.clear();
                if (notifButton != null) {
                    notifButton.setText("🔔 0");
                    notifButton.setStyle("-fx-background-color: #334155; -fx-text-fill: #cbd5e1; -fx-font-size: 14px; -fx-background-radius: 8px;");
                }
                dialog.close();
            });

            footer.getChildren().add(clearBtn);

            root.getChildren().addAll(header, scrollPane, footer);

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            dialog.setScene(scene);
            dialog.showAndWait();

        } else {
            // === Empty state: custom small popup ===
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            if (mainBorderPane.getScene() != null && mainBorderPane.getScene().getWindow() != null) {
                dialog.initOwner(mainBorderPane.getScene().getWindow());
            }

            VBox root = new VBox(16);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(30, 40, 30, 40));
            root.setStyle(
                "-fx-background-color: #0f172a;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #334155;" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 2;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0.3, 0, 4);"
            );

            Label emptyIcon = new Label("🔕");
            emptyIcon.setStyle("-fx-font-size: 36px;");
            Label emptyMsg = new Label("Aucune notification");
            emptyMsg.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px; -fx-font-weight: bold;");
            Label emptyHint = new Label("Les matchs en direct apparaîtront ici.");
            emptyHint.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");

            Button okBtn = new Button("Fermer");
            okBtn.setStyle(
                "-fx-background-color: #334155; -fx-text-fill: #cbd5e1; -fx-font-size: 13px;" +
                "-fx-background-radius: 10; -fx-padding: 8 24 8 24; -fx-cursor: hand;"
            );
            okBtn.setOnAction(e -> dialog.close());

            root.getChildren().addAll(emptyIcon, emptyMsg, emptyHint, okBtn);

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            dialog.setScene(scene);
            dialog.showAndWait();
        }
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


    // ================= CHRONOMETER UPDATE =================
    private void updateChronometers() {
        if (matchContainer == null) return;
        boolean needsRefresh = false;
        
        for (Node node : matchContainer.lookupAll(".chrono-timer-label")) {
            if (node instanceof Label) {
                Label label = (Label) node;
                Matchs m = (Matchs) label.getProperties().get("match");
                if (m != null && m.getDateMatch() != null) {
                    long seconds = Duration.between(m.getDateMatch(), LocalDateTime.now()).toSeconds();
                    if (seconds < 0) seconds = 0;
                    
                    if (seconds >= 90 * 60) {
                        // 🏁 Le match est terminé automatiquement après 90 min
                        m.setStatut("termine");
                        service.update(m);
                        needsRefresh = true;
                    } else {
                        long mins = seconds / 60;
                        long secs = seconds % 60;
                        label.setText(String.format("%02d:%02d", mins, secs));
                    }
                }
            }
        }

        if (needsRefresh) {
            refresh(); // Mettre à jour l'affichage pour passer au badge vert "TERMINÉ"
        }
    }


    // ================= COUNT =================
    private void updateCount(int size) {
        if (countLabel != null) {
            countLabel.setText(size + (size > 1 ? " matchs" : " match"));
        }
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

        HBox statusBox = new HBox(8);
        statusBox.setAlignment(Pos.CENTER);

        String currentStatus = m.getStatut() != null ? m.getStatut().toUpperCase() : "À JOUER";
        Label status = new Label(currentStatus);
        status.getStyleClass().add("horizontal-status-pill");

        // Dynamic styling based on status type
        if (currentStatus.contains("TERMINE")) {
            status.setStyle("-fx-text-fill: #10b981; -fx-border-color: #10b981; -fx-background-color: rgba(16, 185, 129, 0.1);");
        } else if (currentStatus.contains("ANNULE")) {
            status.setStyle("-fx-text-fill: #f43f5e; -fx-border-color: #f43f5e; -fx-background-color: rgba(244, 63, 94, 0.1);");
        } else if (currentStatus.contains("EN_COURS")) {
            status.setStyle("-fx-text-fill: #f59e0b; -fx-border-color: #f59e0b; -fx-background-color: rgba(245, 158, 11, 0.1);");
        } else { // À JOUER
            status.setStyle("-fx-text-fill: #3b82f6; -fx-border-color: #3b82f6; -fx-background-color: rgba(59, 130, 246, 0.1);");
        }

        statusBox.getChildren().add(status);

        // ⏱️ Ajouter le chronomètre si le match est en cours
        // (tolère les variantes: "EN_COURS", "en_cours", "EN COURS", etc.)
        if (currentStatus.contains("EN_COURS") || currentStatus.contains("EN COURS")) {
            Label chronoLabel = new Label("00:00");
            chronoLabel.getStyleClass().add("chrono-timer-label"); // For lookup
            chronoLabel.getProperties().put("match", m);
            chronoLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', system-ui, sans-serif; -fx-font-size: 13px;");
            
            // Initial calculation
            if (m.getDateMatch() != null) {
                long seconds = Duration.between(m.getDateMatch(), LocalDateTime.now()).toSeconds();
                if (seconds < 0) seconds = 0;
                if (seconds > 90 * 60) seconds = 90 * 60;
                chronoLabel.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
            } else {
                // Si un match passe en cours mais n'a pas de date de début, on la fixe pour démarrer le chrono
                // (et on persiste pour que Player/Coach voient le même chrono)
                try {
                    m.setDateMatch(LocalDateTime.now());
                    service.update(m);
                } catch (Exception ignored) {}
            }
            
            statusBox.getChildren().add(chronoLabel);
        }

        Label matchLabel = new Label(m.getNomMatch() != null ? m.getNomMatch().toUpperCase() : "MATCH");
        matchLabel.getStyleClass().add("match-title");
        matchLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7c3aed;");

        Label score = new Label(m.getScoreEquipe1() + " - " + m.getScoreEquipe2());
        score.getStyleClass().add("horizontal-score-text");

        center.getChildren().addAll(statusBox, matchLabel, score);

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
                    getClass().getResource("/esprit/tn/esports/matchDetails.fxml")
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
                    getClass().getResource("/esprit/tn/esports/formMatch.fxml")
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
                    getClass().getResource("/esprit/tn/esports/equipeIndex.fxml")
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
                    getClass().getResource("/esprit/tn/esports/stats.fxml")
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
                    getClass().getResource("/esprit/tn/esports/Login.fxml")
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