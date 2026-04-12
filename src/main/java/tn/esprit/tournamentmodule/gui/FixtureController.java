package tn.esprit.tournamentmodule.gui;

import tn.esprit.tournamentmodule.models.Fixture;
import tn.esprit.tournamentmodule.models.FixtureStatus;
import tn.esprit.tournamentmodule.models.FixtureStatus;
import tn.esprit.tournamentmodule.models.FixtureDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.List;
import java.util.Optional;

public class FixtureController {

    @FXML private TextField                         searchField;
    @FXML private ComboBox<String>                  statusFilter;
    @FXML private TableView<Fixture>                fixtureTable;
    @FXML private TableColumn<Fixture, String>      leagueCol;
    @FXML private TableColumn<Fixture, String>      homeTeamCol;
    @FXML private TableColumn<Fixture, String>      awayTeamCol;
    @FXML private TableColumn<Fixture, String>      dateCol;
    @FXML private TableColumn<Fixture, String>      timeCol;
    @FXML private TableColumn<Fixture, String>      resultCol;
    @FXML private TableColumn<Fixture, FixtureStatus> statusCol;
    @FXML private TableColumn<Fixture, Void>        actionsCol;

    private final FixtureDAO dao = new FixtureDAO();
    private final ObservableList<Fixture> tableData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList(
            "All Statuses","Scheduled","Live","Completed","Cancelled"));
        statusFilter.setValue("All Statuses");

        leagueCol  .setCellValueFactory(new PropertyValueFactory<>("leagueName"));
        homeTeamCol.setCellValueFactory(new PropertyValueFactory<>("homeTeam"));
        awayTeamCol.setCellValueFactory(new PropertyValueFactory<>("awayTeam"));
        resultCol  .setCellValueFactory(new PropertyValueFactory<>("resultDisplay"));
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getMatchDate() != null ? c.getValue().getMatchDate().toString() : "—"));
        timeCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getMatchTime() != null ? c.getValue().getMatchTime().toString() : "—"));

        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(FixtureStatus s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label b = new Label(s.toString());
                b.getStyleClass().addAll("status-badge","status-"+s.name().toLowerCase());
                setGraphic(b); setText(null);
            }
        });

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button edit = new Button("Edit");
            private final Button del  = new Button("Delete");
            { edit.getStyleClass().addAll("btn","btn-sm","btn-secondary");
              del .getStyleClass().addAll("btn","btn-sm","btn-danger");
              edit.setOnAction(e -> openDialog(getTableRow().getItem()));
              del .setOnAction(e -> deleteFixture(getTableRow().getItem())); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(6, edit, del);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        fixtureTable.setItems(tableData);
        loadData();
    }

    @FXML public void onAddFixture() { openDialog(null); }
    @FXML public void onSearch()     { loadData(); }
    @FXML public void onFilter()     { loadData(); }

    private void openDialog(Fixture existing) {
        try {
            URL url = getClass().getResource("/tn/esprit/tournamentmodule/view/FixtureDialog.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            javafx.scene.Parent content = loader.load();
            FixtureDialogController ctrl = loader.getController();
            if (existing != null) ctrl.populate(existing);

            Dialog<Fixture> dialog = new Dialog<>();
            dialog.setTitle(existing == null ? "New Fixture" : "Edit Fixture");
            dialog.getDialogPane().setContent(content);
            ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
            dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/tn/esprit/tournamentmodule/view/app.css").toExternalForm());
            dialog.setResultConverter(b -> b != save ? null : ctrl.buildFixture(existing));

            Optional<Fixture> result = dialog.showAndWait();
            result.ifPresent(f -> {
                try {
                    if (existing == null) dao.insertOne(f);
                    else                  dao.updateOne(f);
                    loadData();
                } catch (Exception e) { showError("Save failed: " + e.getMessage()); }
            });
        } catch (Exception e) { showError("Cannot open dialog: " + e.getMessage()); }
    }

    private void deleteFixture(Fixture f) {
        if (f == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + f.getMatchup() + "\"?", ButtonType.YES, ButtonType.CANCEL);
        a.setTitle("Confirm Delete");
        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                try { dao.deleteOne(f); loadData(); }
                catch (Exception e) { showError("Delete failed: " + e.getMessage()); }
            }
        });
    }

    private void loadData() {
        try {
            String kw     = searchField.getText();
            String status = statusFilter.getValue();
            List<Fixture> fixtures = (kw != null && !kw.isBlank())
                ? dao.search(kw) : dao.selectAll();
            if (status != null && !status.equals("All Statuses")) {
                FixtureStatus fs = FixtureStatus.valueOf(status.toUpperCase());
                fixtures = fixtures.stream().filter(f -> f.getStatus() == fs).toList();
            }
            tableData.setAll(fixtures);
        } catch (Exception e) { showError("Load failed: " + e.getMessage()); }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
