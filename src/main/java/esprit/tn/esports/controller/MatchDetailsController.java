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

    public void setMatch(Matchs match) {
        this.match = match;

        matchTitle.setText(match.getNomMatch());

        team1Name.setText(match.getEquipe1().getNom());
        team2Name.setText(match.getEquipe2().getNom());

        scoreLabel.setText(match.getScoreEquipe1() + " - " + match.getScoreEquipe2());

        dateMatchLabel.setText("Début : " + match.getDateMatch());
        dateFinLabel.setText("Fin : " + match.getDateFinMatch());

        // STATUS
        String statut = match.getStatut();
        statusLabel.setText(statut.toUpperCase());

        // LOGOS
        setLogo(team1Logo, match.getEquipe1().getLogo());
        setLogo(team2Logo, match.getEquipe2().getLogo());
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