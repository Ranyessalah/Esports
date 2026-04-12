package tn.esprit.tournamentmodule.gui;

import tn.esprit.tournamentmodule.models.League;
import tn.esprit.tournamentmodule.models.LeagueStatus;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LeagueDialogController {

    @FXML private TextField              nameField;
    @FXML private TextField              gameField;
    @FXML private TextField              seasonField;
    @FXML private TextArea               teamsArea;
    @FXML private ComboBox<LeagueStatus> statusCombo;

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList(LeagueStatus.values()));
        statusCombo.setValue(LeagueStatus.UPCOMING);
    }

    public void populate(League l) {
        nameField.setText(l.getName());
        gameField.setText(l.getGame());
        seasonField.setText(l.getSeason());
        statusCombo.setValue(l.getStatus());
        if (l.getTeams() != null) teamsArea.setText(String.join(", ", l.getTeams()));
    }

    public League buildLeague(League existing) {
        League l = existing != null ? existing : new League();
        l.setName(nameField.getText().trim());
        l.setGame(gameField.getText().trim());
        l.setSeason(seasonField.getText().trim());
        l.setStatus(statusCombo.getValue());
        String raw = teamsArea.getText();
        if (raw != null && !raw.isBlank()) {
            l.setTeams(Arrays.stream(raw.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList()));
        } else {
            l.setTeams(List.of());
        }
        return l;
    }
}
