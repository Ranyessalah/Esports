package tn.esprit.tournamentmodule.gui;

import tn.esprit.tournamentmodule.models.League;
import tn.esprit.tournamentmodule.models.LeagueDAO;
import tn.esprit.tournamentmodule.models.LeagueStatus;
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

public class LeagueController {

    @FXML private TextField                         searchField;
    @FXML private ComboBox<String>                  statusFilter;
    @FXML private TableView<League>                 leagueTable;
    @FXML private TableColumn<League, String>       nameCol;
    @FXML private TableColumn<League, String>       gameCol;
    @FXML private TableColumn<League, String>       seasonCol;
    @FXML private TableColumn<League, String>       teamsCol;
    @FXML private TableColumn<League, LeagueStatus> statusCol;
    @FXML private TableColumn<League, Void>         actionsCol;

    private final LeagueDAO dao = new LeagueDAO();
    private final ObservableList<League> tableData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList(
            "All Statuses", "Active", "Upcoming", "Completed"));
        statusFilter.setValue("All Statuses");

        nameCol  .setCellValueFactory(new PropertyValueFactory<>("name"));
        gameCol  .setCellValueFactory(new PropertyValueFactory<>("game"));
        seasonCol.setCellValueFactory(new PropertyValueFactory<>("season"));
        teamsCol .setCellValueFactory(new PropertyValueFactory<>("teamsDisplay"));

        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LeagueStatus s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label b = new Label(s.toString());
                b.getStyleClass().addAll("status-badge", "status-" + s.name().toLowerCase());
                setGraphic(b); setText(null);
            }
        });

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button edit = new Button("Edit");
            private final Button del  = new Button("Delete");
            { edit.getStyleClass().addAll("btn","btn-sm","btn-secondary");
              del .getStyleClass().addAll("btn","btn-sm","btn-danger");
              edit.setOnAction(e -> openDialog(getTableRow().getItem()));
              del .setOnAction(e -> deleteLeague(getTableRow().getItem())); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(6, edit, del);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        leagueTable.setItems(tableData);
        loadData();
    }

    @FXML public void onAddLeague() { openDialog(null); }
    @FXML public void onSearch()    { loadData(); }
    @FXML public void onFilter()    { loadData(); }

    private void openDialog(League existing) {
        try {
            URL url = getClass().getResource("/tn/esprit/tournamentmodule/view/LeagueDialog.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            javafx.scene.Parent content = loader.load();
            LeagueDialogController ctrl = loader.getController();
            if (existing != null) ctrl.populate(existing);

            Dialog<League> dialog = new Dialog<>();
            dialog.setTitle(existing == null ? "New League" : "Edit League");
            dialog.getDialogPane().setContent(content);
            ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
            dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/tn/esprit/tournamentmodule/view/app.css").toExternalForm());
            dialog.setResultConverter(b -> b != save ? null : ctrl.buildLeague(existing));

            Optional<League> result = dialog.showAndWait();
            result.ifPresent(l -> {
                try {
                    if (existing == null) dao.insertOne(l);
                    else                  dao.updateOne(l);
                    loadData();
                } catch (Exception e) { showError("Save failed: " + e.getMessage()); }
            });
        } catch (Exception e) { showError("Cannot open dialog: " + e.getMessage()); }
    }

    private void deleteLeague(League l) {
        if (l == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + l.getName() + "\"?\nAll its fixtures will also be removed.",
            ButtonType.YES, ButtonType.CANCEL);
        a.setTitle("Confirm Delete");
        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                try { dao.deleteOne(l); loadData(); }
                catch (Exception e) { showError("Delete failed: " + e.getMessage()); }
            }
        });
    }

    private void loadData() {
        try {
            String kw     = searchField.getText();
            String status = statusFilter.getValue();
            List<League> leagues = (kw != null && !kw.isBlank())
                ? dao.search(kw) : dao.selectAll();
            if (status != null && !status.equals("All Statuses")) {
                LeagueStatus ls = LeagueStatus.valueOf(status.toUpperCase());
                leagues = leagues.stream().filter(l -> l.getStatus() == ls).toList();
            }
            tableData.setAll(leagues);
        } catch (Exception e) { showError("Load failed: " + e.getMessage()); }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
