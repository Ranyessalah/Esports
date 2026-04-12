package esprit.tn.esports.controller;

import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Player;
import esprit.tn.esports.service.PlayerService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;

public class EquipeDetailsController {

    @FXML private ImageView logo;
    @FXML private Label name;
    @FXML private Label game;
    @FXML private Label category;

    @FXML private Label nbPlayers;
    @FXML private Label gameStat;

    @FXML private FlowPane playersContainer;

    private PlayerService playerService = new PlayerService();

    public void setEquipe(Equipe e) {
        name.setText(e.getNom());
        game.setText("Game: " + e.getGame());
        category.setText("Category: " + e.getCategorie());

        gameStat.setText(e.getGame());

        try {
            logo.setImage(new Image("file:" + e.getLogo()));
        } catch (Exception ex) {}

        loadPlayers(e);
    }

    // 🔥 LOAD PLAYERS CARDS
    private void loadPlayers(Equipe equipe) {
        playersContainer.getChildren().clear();

        List<Player> players = playerService.getAll();

        int count = 0;

        for (Player p : players) {
            if (p.getEquipe().getId() == equipe.getId()) {
                playersContainer.getChildren().add(createPlayerCard(p));
                count++;
            }
        }

        nbPlayers.setText(String.valueOf(count));
    }

    // 🔥 PLAYER CARD
    private VBox createPlayerCard(Player p) {
        VBox card = new VBox(8);
        card.getStyleClass().add("player-card");

        Label name = new Label("Player #" + p.getId());
        name.getStyleClass().add("card-title");

        Label pays = new Label(p.getPays());
        pays.getStyleClass().add("card-sub");

        Label niveau = new Label(p.getNiveau());
        niveau.getStyleClass().add("badge");

        card.getChildren().addAll(name, pays, niveau);

        return card;
    }
}