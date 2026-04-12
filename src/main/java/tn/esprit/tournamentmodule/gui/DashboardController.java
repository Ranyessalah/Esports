package tn.esprit.tournamentmodule.gui;

import tn.esprit.tournamentmodule.models.Fixture;
import tn.esprit.tournamentmodule.models.FixtureDAO;
import tn.esprit.tournamentmodule.models.FixtureStatus;
import tn.esprit.tournamentmodule.models.League;
import tn.esprit.tournamentmodule.models.LeagueDAO;
import tn.esprit.tournamentmodule.models.LeagueStatus;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public class DashboardController {

    @FXML private Label totalLeaguesLabel;
    @FXML private Label activeLeaguesLabel;
    @FXML private Label totalFixturesLabel;
    @FXML private Label teamsCountLabel;

    @FXML private VBox leagueListBox;
    @FXML private VBox fixtureListBox;

    private final LeagueDAO leagueDAO = new LeagueDAO();
    private final FixtureDAO fixtureDAO = new FixtureDAO();

    @FXML
    public void initialize() {
        try {
            List<League> leagues  = leagueDAO.selectAll();
            List<Fixture> fixtures = fixtureDAO.selectAll();

            // ── Metrics ───────────────────────────────────────────────
            totalLeaguesLabel.setText(String.valueOf(leagues.size()));

            long active = leagues.stream()
                .filter(l -> l.getStatus() == LeagueStatus.ACTIVE).count();
            activeLeaguesLabel.setText(String.valueOf(active));

            totalFixturesLabel.setText(String.valueOf(fixtures.size()));

            int teams = leagues.stream()
                .mapToInt(l -> l.getTeams() == null ? 0 : l.getTeams().size()).sum();
            teamsCountLabel.setText(String.valueOf(teams));

            // ── League rows ───────────────────────────────────────────
            leagueListBox.getChildren().clear();
            for (League l : leagues) {
                javafx.scene.layout.HBox row = buildLeagueRow(l);
                leagueListBox.getChildren().add(row);
            }

            // ── Recent fixture rows ───────────────────────────────────
            fixtureListBox.getChildren().clear();
            fixtures.stream().limit(5).forEach(f -> {
                javafx.scene.layout.HBox row = buildFixtureRow(f);
                fixtureListBox.getChildren().add(row);
            });

        } catch (Exception e) {
            System.err.println("Dashboard load error: " + e.getMessage());
        }
    }

    private javafx.scene.layout.HBox buildLeagueRow(League l) {
        Label name   = new Label(l.getName());
        name.getStyleClass().add("dash-row-title");
        Label detail = new Label(l.getGame() + " · " + l.getSeason()
            + " · " + l.getTeamsDisplay());
        detail.getStyleClass().add("dash-row-sub");
        javafx.scene.layout.VBox info = new javafx.scene.layout.VBox(2, name, detail);
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label badge = new Label(l.getStatus().toString());
        badge.getStyleClass().addAll("status-badge", "status-" + l.getStatus().name().toLowerCase());

        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10, info, spacer, badge);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("dash-row");
        return row;
    }

    private javafx.scene.layout.HBox buildFixtureRow(Fixture f) {
        Label home   = new Label(f.getHomeTeam());
        home.getStyleClass().add("dash-team");
        Label score  = new Label(f.getResultDisplay());
        score.getStyleClass().add("dash-score");
        Label away   = new Label(f.getAwayTeam());
        away.getStyleClass().add("dash-team");

        Label meta = new Label(f.getMatchDate() != null ? f.getMatchDate().toString() : "TBD");
        meta.getStyleClass().add("dash-row-sub");

        javafx.scene.layout.VBox info = new javafx.scene.layout.VBox(4,
            new javafx.scene.layout.HBox(8, home, score, away), meta);

        Label badge = new Label(f.getStatus().toString());
        badge.getStyleClass().addAll("status-badge", "status-" + f.getStatus().name().toLowerCase());

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10, info, spacer, badge);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("dash-row");
        return row;
    }
}
