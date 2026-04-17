package esprit.tn.esports.controller;

import esprit.tn.esports.entite.Coach;
import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Player;
import esprit.tn.esports.service.PlayerService;
import esprit.tn.esports.utils.QRCodeDialog;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.shape.Circle;

import java.io.File;
import java.util.List;

public class EquipeDetailsController {

    @FXML private ImageView logo;
    @FXML private Label name;
    @FXML private Label game;
    @FXML private Label category;

    @FXML private Label nbPlayers;
    @FXML private Label gameStat;

    @FXML private VBox staffSection;
    @FXML private FlowPane staffContainer;
    @FXML private FlowPane playersContainer;

    private PlayerService playerService = new PlayerService();
    private Runnable onBack;
    private Equipe currentEquipe;

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    public void setEquipe(Equipe e) {
        this.currentEquipe = e;
        name.setText(e.getNom());
        game.setText(e.getGame());
        category.setText(e.getCategorie());

        // 🔥 Load & Modernize Hero Logo
        setCircularImage(logo, e.getLogo(), 80);

        nbPlayers.setText(String.valueOf(e.getPlayers() != null ? e.getPlayers().size() : 0));
        loadRoster(e);
    }

    // 🔥 LOAD ROSTER (Coach + Players)
    private void loadRoster(Equipe equipe) {
        // COACH
        staffContainer.getChildren().clear();
        if (equipe.getCoach() != null) {
            staffSection.setVisible(true);
            staffSection.setManaged(true);
            staffContainer.getChildren().add(createCoachCard(equipe.getCoach()));
        } else {
            staffSection.setVisible(false);
            staffSection.setManaged(false);
        }

        // PLAYERS
        playersContainer.getChildren().clear();
        List<Player> players = playerService.getPlayersByEquipe(equipe.getId());
        for (Player p : players) {
            playersContainer.getChildren().add(createPlayerCard(p));
        }
        nbPlayers.setText(String.valueOf(players.size()));
    }

    // 🔥 HELPER: SET CIRCULAR IMAGE
    private void setCircularImage(ImageView iv, String path, double radius) {
        iv.setFitWidth(radius * 2);
        iv.setFitHeight(radius * 2);
        Circle clip = new Circle(radius, radius, radius);
        iv.setClip(clip);

        try {
            if (path != null && !path.isBlank()) {
                File file = new File(path);
                if (file.exists()) {
                    iv.setImage(new Image(file.toURI().toString()));
                    return;
                }
            }
        } catch (Exception ignored) {}

        // Fallback or dynamic placeholder logic could go here
    }

    // 🔥 COACH CARD
    private VBox createCoachCard(Coach c) {
        VBox card = new VBox(12);
        card.getStyleClass().add("player-card-modern");
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(200);

        ImageView img = new ImageView();
        String imgPath = (c.getUser() != null) ? c.getUser().getProfileImage() : null;
        setCircularImage(img, imgPath, 35);

        Label nameLabel = new Label(c.getUser() != null ? c.getUser().getEmail() : "Coach");
        nameLabel.getStyleClass().add("card-title");

        Label spec = new Label(c.getSpecialite() != null ? c.getSpecialite() : "Coach Principal");
        spec.getStyleClass().add("card-sub");

        Label badge = new Label("COACH");
        badge.getStyleClass().addAll("badge");
        badge.setStyle("-fx-background-color: #ef4444;");

        card.getChildren().addAll(img, nameLabel, spec, badge);
        return card;
    }

    // 🔥 PLAYER CARD
    private VBox createPlayerCard(Player p) {
        VBox card = new VBox(12);
        card.getStyleClass().add("player-card-modern");
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(200);

        ImageView img = new ImageView();
        String imgPath = (p.getUser() != null) ? p.getUser().getProfileImage() : null;
        setCircularImage(img, imgPath, 35);

        String displayName = (p.getUser() != null && p.getUser().getEmail() != null)
                ? p.getUser().getEmail()
                : "Player #" + p.getId();

        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("card-title");

        Label pays = new Label(p.getPays() != null ? p.getPays() : "Inconnu");
        pays.getStyleClass().add("card-sub");

        Label niveau = new Label(p.getNiveau() != null ? p.getNiveau() : "Debutant");
        niveau.getStyleClass().add("badge");

        card.getChildren().addAll(img, nameLabel, pays, niveau);
        return card;
    }

    @FXML
    private void showQR() {
        if (currentEquipe != null) {
            Stage owner = (Stage) name.getScene().getWindow();
            new QRCodeDialog().showShareableTeamQr(currentEquipe, owner);
        }
    }

    @FXML
    private void goBack() {
        if (onBack != null) {
            onBack.run();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/equipeIndex_client.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) name.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}