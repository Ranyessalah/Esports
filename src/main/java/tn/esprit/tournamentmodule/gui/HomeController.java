package tn.esprit.tournamentmodule.gui;

import tn.esprit.tournamentmodule.models.League;
import tn.esprit.tournamentmodule.models.LeagueDAO;
import tn.esprit.tournamentmodule.models.LeagueStatus;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.List;

public class HomeController {

    @FXML private FlowPane cardsPane;
    @FXML private Label filterAll;
    @FXML private Label filterActive;
    @FXML private Label filterUpcoming;
    @FXML private Label filterCompleted;
    @FXML private Label liveLabel;

    private final LeagueDAO dao = new LeagueDAO();
    private List<League> allLeagues;

    @FXML
    public void initialize() {
        try {
            allLeagues = dao.selectAll();
            long active = allLeagues.stream()
                .filter(l -> l.getStatus() == LeagueStatus.ACTIVE).count();
            liveLabel.setText(active + " Active now");
            renderCards(allLeagues);
        } catch (Exception e) {
            System.err.println("HomeController error: " + e.getMessage());
        }
    }

    @FXML public void onFilterAll()       { setFilter(filterAll);       renderCards(allLeagues); }
    @FXML public void onFilterActive()    { setFilter(filterActive);    renderCards(filterBy(LeagueStatus.ACTIVE)); }
    @FXML public void onFilterUpcoming()  { setFilter(filterUpcoming);  renderCards(filterBy(LeagueStatus.UPCOMING)); }
    @FXML public void onFilterCompleted() { setFilter(filterCompleted); renderCards(filterBy(LeagueStatus.COMPLETED)); }

    private List<League> filterBy(LeagueStatus s) {
        return allLeagues.stream().filter(l -> l.getStatus() == s).toList();
    }

    private void setFilter(Label active) {
        for (Label l : List.of(filterAll, filterActive, filterUpcoming, filterCompleted))
            l.getStyleClass().remove("filter-active");
        active.getStyleClass().add("filter-active");
    }

    private void renderCards(List<League> leagues) {
        cardsPane.getChildren().clear();
        for (League l : leagues)
            cardsPane.getChildren().add(buildCard(l));
    }

    private VBox buildCard(League l) {
        // Game icon placeholder (colored label)
        Label icon = new Label(gameIcon(l.getGame()));
        icon.getStyleClass().add("cover-icon");

        Label game = new Label(l.getGame().toUpperCase());
        game.getStyleClass().add("cover-game");

        Label name = new Label(l.getName());
        name.getStyleClass().add("cover-name");
        name.setWrapText(true);
        name.setMaxWidth(160);

        VBox top = new VBox(8, icon, game, name);
        top.setAlignment(Pos.CENTER);
        VBox.setVgrow(top, Priority.ALWAYS);

        // Footer
        int teamCount = l.getTeams() == null ? 0 : l.getTeams().size();
        Label teams = new Label(teamCount + " teams");
        teams.getStyleClass().add("cover-teams");

        Label badge = new Label(l.getStatus().toString());
        badge.getStyleClass().addAll("status-badge",
            "status-" + l.getStatus().name().toLowerCase());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox footer = new HBox(teams, sp, badge);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("cover-footer");

        VBox card = new VBox(top, footer);
        card.getStyleClass().addAll("cover-card", "accent-" + accentClass(l.getGame()));
        card.setPrefWidth(190);
        card.setPrefHeight(240);
        card.setAlignment(Pos.TOP_CENTER);
        return card;
    }

    private String gameIcon(String game) {
        if (game == null) return "?";
        String g = game.toLowerCase();
        if (g.contains("valorant"))  return "[ V ]";
        if (g.contains("legend"))    return "[ L ]";
        if (g.contains("cs") || g.contains("counter")) return "[ C ]";
        if (g.contains("fortnite")) return "[ F ]";
        if (g.contains("dota"))     return "[ D ]";
        return "[ " + game.substring(0, Math.min(2, game.length())).toUpperCase() + " ]";
    }

    private String accentClass(String game) {
        if (game == null) return "purple";
        String g = game.toLowerCase();
        if (g.contains("valorant"))  return "red";
        if (g.contains("legend"))    return "teal";
        if (g.contains("cs"))        return "blue";
        return "purple";
    }
}
