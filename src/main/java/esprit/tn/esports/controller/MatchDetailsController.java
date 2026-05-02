package esprit.tn.esports.controller;

import esprit.tn.esports.entite.Matchs;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.Stage;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MatchDetailsController {

    @FXML private Label matchTitle;
    @FXML private Label statusLabel;
    @FXML private Label team1Name;
    @FXML private Label team2Name;
    @FXML private Label scoreLabel;
    @FXML private Label dateMatchLabel;
    @FXML private Label dateFinLabel;
    @FXML private ImageView team1Logo;
    @FXML private ImageView team2Logo;

    private Matchs match;
    private Runnable onBack;

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    public void setMatch(Matchs match) {
        this.match = match;

        matchTitle.setText(match.getNomMatch() != null ? match.getNomMatch().toUpperCase() : "DÉTAILS MATCH");

        team1Name.setText(match.getEquipe1().getNom());
        team2Name.setText(match.getEquipe2().getNom());

        scoreLabel.setText(match.getScoreEquipe1() + " - " + match.getScoreEquipe2());

        // 🔥 Refined Date Formatting
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH:mm", Locale.FRENCH);
        dateMatchLabel.setText("Début : " + (match.getDateMatch() != null ? match.getDateMatch().format(dtf) : "N/A"));
        dateFinLabel.setText("Fin : " + (match.getDateFinMatch() != null ? match.getDateFinMatch().format(dtf) : "N/A"));

        // STATUS (ne pas forcer TERMINÉ selon le score; afficher le statut réel)
        String statut = match.getStatut();
        String normalized = statut != null ? statut.trim().toUpperCase() : "";
        statusLabel.setText(!normalized.isEmpty() ? normalized : "STATUT INCONNU");

        // Status Colors (basé sur le statut DB)
        statusLabel.getStyleClass().removeAll("badge-played", "badge-pending");
        if (normalized.contains("TERMINE") || normalized.contains("TERMINÉ")) {
            statusLabel.getStyleClass().add("badge-played");
        } else if (normalized.contains("EN_COURS") || normalized.contains("EN COURS")) {
            statusLabel.getStyleClass().add("badge-played");
        } else {
            statusLabel.getStyleClass().add("badge-pending");
        }

        // LOGOS
        setLogo(team1Logo, match.getEquipe1() != null ? match.getEquipe1().getLogo() : null);
        setLogo(team2Logo, match.getEquipe2() != null ? match.getEquipe2().getLogo() : null);
    }

    private void setLogo(ImageView img, String path) {
        try {
            if (img == null) return;
            if (path == null || path.isBlank()) {
                img.setImage(null);
                return;
            }
            File file = new File(path);
            if (file.exists()) {
                img.setImage(new Image(file.toURI().toString()));
            } else {
                img.setImage(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        if (onBack != null) {
            onBack.run();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/matchIndex_client.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) matchTitle.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}