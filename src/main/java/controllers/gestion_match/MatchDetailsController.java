package controllers.gestion_match;

import entities.Matchs;
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

        // STATUS
        String statut = match.getStatut();
        statusLabel.setText(statut != null ? statut.toUpperCase() : "STATUT INCONNU");
        
        // Status Colors
        statusLabel.getStyleClass().removeAll("badge-played", "badge-pending");
        if (match.getScoreEquipe1() > 0 || match.getScoreEquipe2() > 0) {
            statusLabel.getStyleClass().add("badge-played");
            statusLabel.setText("TERMINÉ");
        } else {
            statusLabel.getStyleClass().add("badge-pending");
            statusLabel.setText("À JOUER");
        }

        // LOGOS
        setLogo(team1Logo, match.getEquipe1().getLogo());
        setLogo(team2Logo, match.getEquipe2().getLogo());

        // 🔥 Circular Clips (Radius 80)
        javafx.scene.shape.Circle clip1 = new javafx.scene.shape.Circle(80, 80, 80);
        team1Logo.setClip(clip1);
        javafx.scene.shape.Circle clip2 = new javafx.scene.shape.Circle(80, 80, 80);
        team2Logo.setClip(clip2);
    }

    private void setLogo(ImageView img, String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                img.setImage(new Image(file.toURI().toString()));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_match/matchIndex_client.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) matchTitle.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}