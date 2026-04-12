package tn.esprit.tournamentmodule.gui;

import tn.esprit.tournamentmodule.models.Fixture;
import tn.esprit.tournamentmodule.models.FixtureStatus;
import tn.esprit.tournamentmodule.models.FixtureStatus;
import tn.esprit.tournamentmodule.models.League;
import tn.esprit.tournamentmodule.models.LeagueDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class FixtureDialogController {

    @FXML private ComboBox<League>        leagueCombo;
    @FXML private TextField               homeTeamField;
    @FXML private TextField               awayTeamField;
    @FXML private DatePicker              datePicker;
    @FXML private TextField               timeField;
    @FXML private TextField               homeScoreField;
    @FXML private TextField               awayScoreField;
    @FXML private ComboBox<FixtureStatus> statusCombo;

    private final LeagueDAO leagueDAO = new LeagueDAO();

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList(FixtureStatus.values()));
        statusCombo.setValue(FixtureStatus.SCHEDULED);
        try {
            List<League> leagues = leagueDAO.selectAll();
            leagueCombo.setItems(FXCollections.observableArrayList(leagues));
            leagueCombo.setConverter(new StringConverter<>() {
                @Override public String toString(League l)   { return l == null ? "" : l.getName(); }
                @Override public League fromString(String s) { return null; }
            });
        } catch (Exception e) { System.err.println("Cannot load leagues: " + e.getMessage()); }
    }

    public void populate(Fixture f) {
        homeTeamField.setText(f.getHomeTeam());
        awayTeamField.setText(f.getAwayTeam());
        datePicker.setValue(f.getMatchDate());
        if (f.getMatchTime()  != null) timeField.setText(f.getMatchTime().toString());
        if (f.getHomeScore()  != null) homeScoreField.setText(String.valueOf(f.getHomeScore()));
        if (f.getAwayScore()  != null) awayScoreField.setText(String.valueOf(f.getAwayScore()));
        statusCombo.setValue(f.getStatus());
        leagueCombo.getItems().stream()
            .filter(l -> l.getId() == f.getLeagueId())
            .findFirst().ifPresent(leagueCombo::setValue);
    }

    public Fixture buildFixture(Fixture existing) {
        Fixture f = existing != null ? existing : new Fixture();
        League sel = leagueCombo.getValue();
        if (sel != null) { f.setLeagueId(sel.getId()); f.setLeagueName(sel.getName()); }
        f.setHomeTeam (homeTeamField.getText().trim());
        f.setAwayTeam (awayTeamField.getText().trim());
        f.setMatchDate(datePicker.getValue());
        f.setStatus   (statusCombo.getValue());
        try { String t = timeField.getText().trim();
              if (!t.isEmpty()) f.setMatchTime(LocalTime.parse(t)); }
        catch (DateTimeParseException ignored) {}
        try { f.setHomeScore(Integer.parseInt(homeScoreField.getText().trim())); }
        catch (NumberFormatException ignored) { f.setHomeScore(null); }
        try { f.setAwayScore(Integer.parseInt(awayScoreField.getText().trim())); }
        catch (NumberFormatException ignored) { f.setAwayScore(null); }
        return f;
    }
}
