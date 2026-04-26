package esprit.tn.esports.utils;

import esprit.tn.esports.entite.Matchs;
import esprit.tn.esports.service.MatchService;
import javafx.application.Platform;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MatchNotificationService {

    private static MatchNotificationService instance;
    private Timeline timeline;
    private boolean running = false;

    private final Set<Integer> notifiedMatchIds = new HashSet<>();
    private final Set<Integer> warnedMatchIds = new HashSet<>();
    private final MatchService matchService = new MatchService();
    private Consumer<Matchs> onMatchStarted;
    private Consumer<Matchs> onMatchWarning;

    private MatchNotificationService() {}

    public static MatchNotificationService getInstance() {
        if (instance == null) {
            instance = new MatchNotificationService();
        }
        return instance;
    }

    public void setOnMatchStarted(Consumer<Matchs> callback) {
        this.onMatchStarted = callback;
    }

    public void setOnMatchWarning(Consumer<Matchs> callback) {
        this.onMatchWarning = callback;
    }

    public void start() {
        if (running) {
            System.out.println("[MatchNotif] Service déjà en cours, callbacks mis à jour.");
            return;
        }
        running = true;

        // Utilisation de Timeline JavaFX pour éviter les problèmes de concurrence SQLite
        timeline = new Timeline(new KeyFrame(Duration.seconds(10), event -> {
            checkMatches();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        
        // Exécuter une première vérification immédiatement
        checkNow();
        
        timeline.play();
        System.out.println("[MatchNotif] Service Timeline JavaFX démarré.");
    }

    public void checkNow() {
        if (Platform.isFxApplicationThread()) {
            checkMatches();
        } else {
            Platform.runLater(this::checkMatches);
        }
    }

    public void resetMatchNotifications(int matchId) {
        warnedMatchIds.remove(matchId);
        notifiedMatchIds.remove(matchId);
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
        running = false;
    }

    private void checkMatches() {
        try {
            List<Matchs> allMatchs = matchService.getAll();
            LocalDateTime now = LocalDateTime.now();

            for (Matchs m : allMatchs) {
                if (m.getDateMatch() == null) continue;
                String statut = m.getStatut() != null ? m.getStatut().toLowerCase() : "";

                // On ne saute que les matchs vraiment terminés ou annulés
                if (statut.contains("termine") || statut.contains("annule")) {
                    continue;
                }

                long secondsUntilStart = ChronoUnit.SECONDS.between(now, m.getDateMatch());

                // === ALERTE MATCH DÉMARRÉ (Démarré maintenant ou dans le passé) ===
                if (secondsUntilStart <= 60 && !notifiedMatchIds.contains(m.getId())) {
                    
                    notifiedMatchIds.add(m.getId());
                    System.out.println("[MatchNotif] 🔴 DÉTECTION Match " + m.getId() + " (" + m.getNomMatch() + ")");
                    
                    // Mettre à jour le statut en base
                    m.setStatut("en_cours");
                    matchService.update(m);
                    
                    if (onMatchStarted != null) {
                        Platform.runLater(() -> {
                            onMatchStarted.accept(m);
                            System.out.println("[MatchNotif] Callback onMatchStarted exécuté pour ID: " + m.getId());
                        });
                    }
                }
            }
        } catch (Throwable e) {
            System.err.println("[MatchNotif] ERREUR FATALE checkMatches:");
            e.printStackTrace();
        }
    }

    // ============ TOAST POPUP ============
    public static void showToast(Stage owner, String title, String message) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showToast(owner, title, message));
            return;
        }
        try {
            Popup popup = new Popup();
            popup.setAutoHide(true);

            VBox box = new VBox(6);
            box.setPadding(new Insets(16, 20, 16, 20));
            box.setMaxWidth(400);
            box.setStyle(
                "-fx-background-color: #1e293b;" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: #7c3aed;" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 2;" +
                "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.5), 20, 0.3, 0, 4);"
            );

            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);

            Label icon = new Label("🔴 LIVE");
            icon.setStyle(
                "-fx-text-fill: #f43f5e;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-color: rgba(244,63,94,0.15);" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 2 8 2 8;"
            );

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label closeBtn = new Label("✕");
            closeBtn.setStyle("-fx-text-fill: #64748b; -fx-font-size: 16px; -fx-cursor: hand;");
            closeBtn.setOnMouseClicked(e -> popup.hide());

            header.getChildren().addAll(icon, titleLabel, spacer, closeBtn);

            Label msgLabel = new Label(message);
            msgLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
            msgLabel.setWrapText(true);

            Label timeLabel = new Label("🔊 Alerte sonore · Admin notifié");
            timeLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

            box.getChildren().addAll(header, msgLabel, timeLabel);
            popup.getContent().add(box);

            if (owner != null && owner.isShowing()) {
                double x = owner.getX() + owner.getWidth() - 430;
                double y = owner.getY() + 30;
                popup.show(owner, x, y);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), box);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();

                PauseTransition pause = new PauseTransition(Duration.seconds(8));
                pause.setOnFinished(e -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(500), box);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(ev -> popup.hide());
                    fadeOut.play();
                });
                pause.play();
            } else {
                System.err.println("[MatchNotif] Toast ignoré: fenêtre non disponible");
            }
        } catch (Exception e) {
            System.err.println("[MatchNotif] Erreur showToast:");
            e.printStackTrace();
        }
    }

    // ============ PLAY ALERT SOUND (non-blocking) ============
    public static void playAlertSound() {
        new Thread(() -> {
            try {
                // Un petit son d'alerte plus mélodique (double bip rapide)
                java.awt.Toolkit.getDefaultToolkit().beep();
                Thread.sleep(150);
                java.awt.Toolkit.getDefaultToolkit().beep();
            } catch (Exception e) {
                // Fallback silencieux
            }
        }, "alert-sound-thread").start();
    }
}
