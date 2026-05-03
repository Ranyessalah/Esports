package controllers.gestion_match;

import entities.Equipe;
import entities.Matchs;
import services.gestion_match.EquipeService;
import services.gestion_match.MatchService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class FormMatchController {

    @FXML private Label formTitleLabel;

    @FXML private TextField nomField;
    @FXML private ComboBox<Equipe> equipe1Box;
    @FXML private ComboBox<Equipe> equipe2Box;
    @FXML private ImageView logo1;
    @FXML private ImageView logo2;
    @FXML private TextField score1Field;
    @FXML private TextField score2Field;
    @FXML private DatePicker dateMatchPicker;
    @FXML private DatePicker dateFinPicker;

    @FXML private Label errorLabel;

    @FXML private Label nomError;
    @FXML private Label equipe1Error;
    @FXML private Label equipe2Error;
    @FXML private Label score1Error;
    @FXML private Label score2Error;
    @FXML private Label dateMatchError;
    @FXML private Label dateFinError;

    @FXML private Button saveBtn;
    @FXML private Button incScore1Btn;
    @FXML private Button decScore1Btn;
    @FXML private Button incScore2Btn;
    @FXML private Button decScore2Btn;

    private final MatchService matchService = new MatchService();
    private final EquipeService equipeService = new EquipeService();

    private Matchs match;
    private MatchController parentController;

    @FXML private VBox statusBox;
    @FXML private ComboBox<String> statusBoxField;
    private List<Equipe> allEquipes;
// ========= AJOUTER EN HAUT AVEC LES @FXML =========

    @FXML private Label score1Display;
    @FXML private Label score2Display;



    @FXML private ComboBox<String> hourStartBox;
    @FXML private ComboBox<String> hourEndBox;
    private void updateScoreDisplays() {

        score1Display.setText(score1Field.getText().isEmpty() ? "0" : score1Field.getText());
        score2Display.setText(score2Field.getText().isEmpty() ? "0" : score2Field.getText());
    }



// ========= SCORE 1 =========

    @FXML
    public void increaseScore1() {

        int val = Integer.parseInt(score1Field.getText());
        val++;

        score1Field.setText(String.valueOf(val));
        updateScoreDisplays();
    }

    @FXML
    public void decreaseScore1() {

        int val = Integer.parseInt(score1Field.getText());

        if (val > 0) val--;

        score1Field.setText(String.valueOf(val));
        updateScoreDisplays();
    }



// ========= SCORE 2 =========

    @FXML
    public void increaseScore2() {

        int val = Integer.parseInt(score2Field.getText());
        val++;

        score2Field.setText(String.valueOf(val));
        updateScoreDisplays();
    }

    @FXML
    public void decreaseScore2() {

        int val = Integer.parseInt(score2Field.getText());

        if (val > 0) val--;

        score2Field.setText(String.valueOf(val));
        updateScoreDisplays();
    }
    // ================= INIT =================
    @FXML
    public void initialize() {

        initComboBox();
        loadEquipes();
        initNumericFields();
        initFilters();
        initRealtimeValidation();
        updateScoreDisplays();
        statusBoxField.setItems(FXCollections.observableArrayList(
                "a jouer",
                "en_cours",
                "termine",
                "annulé"

        ));
        score1Field.setText("0");
        score2Field.setText("0");

        // Logic for score locking in "Add" mode
        if (match == null || match.getId() == 0) {
            incScore1Btn.setDisable(true);
            decScore1Btn.setDisable(true);
            incScore2Btn.setDisable(true);
            decScore2Btn.setDisable(true);
        }
        for (int h = 0; h < 24; h++) {

            for (int m = 0; m < 60; m += 30) {

                String time = String.format("%02d:%02d", h, m);

                hourStartBox.getItems().add(time);
                hourEndBox.getItems().add(time);
            }
        }

        hourStartBox.setValue("18:00");
        hourEndBox.setValue("20:00");
        
        initDatePickerRestrictions();
    }

    private void initDatePickerRestrictions() {
        javafx.util.Callback<DatePicker, DateCell> dayCellFactory = d -> new DateCell() {
            @Override
            public void updateItem(java.time.LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isBefore(java.time.LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #2c3448; -fx-text-fill: #555;");
                }
            }
        };
        dateMatchPicker.setDayCellFactory(dayCellFactory);
        dateFinPicker.setDayCellFactory(dayCellFactory);
    }

    // ================= LOAD =================
    private void loadEquipes() {
        allEquipes = equipeService.getAll();

        equipe1Box.setItems(FXCollections.observableArrayList(allEquipes));
        equipe2Box.setItems(FXCollections.observableArrayList(allEquipes));
    }
// =====================
// FormMatchController.java
// AJOUTER CETTE MÉTHODE
// =====================

    public void loadData(Matchs m) {

        if (m == null) return;

        this.match = m;

        formTitleLabel.setText("Modifier Match");

        nomField.setText(m.getNomMatch());

        equipe1Box.setValue(m.getEquipe1());
        equipe2Box.setValue(m.getEquipe2());

        score1Field.setText(String.valueOf(m.getScoreEquipe1()));
        score2Field.setText(String.valueOf(m.getScoreEquipe2()));

        if (score1Display != null)
            score1Display.setText(String.valueOf(m.getScoreEquipe1()));

        if (score2Display != null)
            score2Display.setText(String.valueOf(m.getScoreEquipe2()));

        if (m.getDateMatch() != null)
            dateMatchPicker.setValue(m.getDateMatch().toLocalDate());

        if (m.getDateFinMatch() != null)
            dateFinPicker.setValue(m.getDateFinMatch().toLocalDate());
        // 🔥 afficher statut uniquement en mode edit
        statusBox.setVisible(true);
        statusBox.setManaged(true);

        statusBoxField.setValue(
                m.getStatut() == null ? "a jouer" : m.getStatut()
        );


        saveBtn.setText("Mettre à jour");

        // 🔥 Afficher les logos
        displayTeamLogo(m.getEquipe1(), logo1);
        displayTeamLogo(m.getEquipe2(), logo2);

        // 🔥 enable score modification in edit mode
        incScore1Btn.setDisable(false);
        decScore1Btn.setDisable(false);
        incScore2Btn.setDisable(false);
        decScore2Btn.setDisable(false);
    }
    // ================= COMBO DISPLAY =================
    private void initComboBox() {

        StringConverter<Equipe> converter = new StringConverter<>() {
            @Override
            public String toString(Equipe e) {
                return e == null ? "" : e.getNom();
            }

            @Override
            public Equipe fromString(String string) {
                return null;
            }
        };

        equipe1Box.setConverter(converter);
        equipe2Box.setConverter(converter);
    }

    // ================= FILTER =================
    private void initFilters() {

        equipe1Box.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayTeamLogo(newVal, logo1);
                equipe2Box.setItems(FXCollections.observableArrayList(
                        allEquipes.stream()
                                .filter(e -> e.getId() != newVal.getId())
                                .collect(Collectors.toList())
                ));
            }
        });

        equipe2Box.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayTeamLogo(newVal, logo2);
                equipe1Box.setItems(FXCollections.observableArrayList(
                        allEquipes.stream()
                                .filter(e -> e.getId() != newVal.getId())
                                .collect(Collectors.toList())
                ));
            }
        });
    }

    private void displayTeamLogo(Equipe e, ImageView target) {
        if (e == null || e.getLogo() == null || e.getLogo().isEmpty() || target == null) {
            target.setImage(null);
            return;
        }
        try {
            Image img = new Image(getClass().getResourceAsStream("/images/" + e.getLogo()));
            target.setImage(img);
        } catch (Exception ex) {
            target.setImage(null);
        }
    }

    // ================= NUMERIC =================
    private void initNumericFields() {

        score1Field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                score1Field.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        score2Field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                score2Field.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

    // ================= REALTIME =================
    private void initRealtimeValidation() {

        nomField.textProperty().addListener((obs, o, n) -> validateNom());
        equipe1Box.valueProperty().addListener((obs, o, n) -> validateEquipe1());
        equipe2Box.valueProperty().addListener((obs, o, n) -> validateEquipe2());
        score1Field.textProperty().addListener((obs, o, n) -> validateScore1());
        score2Field.textProperty().addListener((obs, o, n) -> validateScore2());
        dateMatchPicker.valueProperty().addListener((obs, o, n) -> validateAll());
        dateFinPicker.valueProperty().addListener((obs, o, n) -> validateAll());
        hourStartBox.valueProperty().addListener((obs, o, n) -> validateAll());
        hourEndBox.valueProperty().addListener((obs, o, n) -> validateAll());
    }

    // ================= SAVE =================
    @FXML
    public void saveMatch() {

        hideError();

        if (!validateAll()) {
            showError("Veuillez corriger les champs.");
            return;
        }

        try {

            if (match == null) {
                match = new Matchs();
            }

            match.setNomMatch(nomField.getText().trim());
            match.setEquipe1(equipe1Box.getValue());
            match.setEquipe2(equipe2Box.getValue());

            match.setScoreEquipe1(Integer.parseInt(score1Field.getText()));
            match.setScoreEquipe2(Integer.parseInt(score2Field.getText()));

            String[] hs = hourStartBox.getValue().split(":");
            String[] he = hourEndBox.getValue().split(":");

            LocalDateTime start = dateMatchPicker.getValue().atTime(
                    Integer.parseInt(hs[0]),
                    Integer.parseInt(hs[1])
            );

            LocalDateTime end = dateFinPicker.getValue().atTime(
                    Integer.parseInt(he[0]),
                    Integer.parseInt(he[1])
            );

            match.setDateMatch(start);
            match.setDateFinMatch(end);

            LocalDateTime now = LocalDateTime.now();

            if (match.getId() != 0) {
                // mode edit => valeur manuelle absolue depuis le ComboBox
                match.setStatut(statusBoxField.getValue());
            } else {
                // mode add => calcul automatique
                if (now.isBefore(start)) {
                    match.setStatut("a jouer");
                } else if (now.isAfter(start) && now.isBefore(end)) {
                    match.setStatut("en_cours");
                } else {
                    match.setStatut("termine");
                }
            }
            if (match.getId() == 0) {
                matchService.add(match);
            } else {
                matchService.update(match);
            }

            if (parentController != null) {
                parentController.refresh();
            }

            closeWindow();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erreur lors de l'enregistrement.");
        }
    }

    // ================= VALIDATE ALL =================
// REMPLACER validateAll() PAR CELUI-CI

// ==============================
// PROBLÈME 1:
// en edit tu bloques date future
// SOLUTION validateAll()
// ==============================

    private boolean validateAll() {

        boolean ok =
                validateNom() &
                        validateEquipe1() &
                        validateEquipe2() &
                        validateScore1() &
                        validateScore2() &
                        validateDateMatch() &
                        validateDateFin();

        if (!ok) return false;

        // équipes différentes
        if (equipe1Box.getValue().getId() == equipe2Box.getValue().getId()) {
            showFieldError(equipe2Error, equipe2Box, "Les équipes doivent être différentes");
            return false;
        }

        // heure début
        String[] hs = hourStartBox.getValue().split(":");

        // heure fin
        String[] he = hourEndBox.getValue().split(":");

        LocalDateTime start = dateMatchPicker.getValue().atTime(
                Integer.parseInt(hs[0]),
                Integer.parseInt(hs[1])
        );

        LocalDateTime end = dateFinPicker.getValue().atTime(
                Integer.parseInt(he[0]),
                Integer.parseInt(he[1])
        );

        LocalDateTime now = LocalDateTime.now();

        // 🔥 FIX: allow past date ONLY in edit mode
        boolean isEditMode = (match != null && match.getId() != 0);

        // Date/heure match doit être présente ou future (Uniquement en mode AJOUT)
        if (!isEditMode && start.isBefore(now)) {
            showFieldError(dateMatchError, dateMatchPicker, "Le début doit être dans le futur (aujourd'hui ou après)");
            return false;
        } else {
            hideFieldError(dateMatchError, dateMatchPicker);
        }

        // FIN > DEBUT (Strictement)
        if (!end.isAfter(start)) {
            showFieldError(dateFinError, dateFinPicker, "La date de fin doit être APRES la date de début");
            return false;
        } else {
            hideFieldError(dateFinError, dateFinPicker);
        }

        return true;
    }

    private boolean validateNom() {
        if (nomField.getText().trim().isEmpty()) {
            showFieldError(nomError, nomField, "Champ obligatoire");
            return false;
        }
        hideFieldError(nomError, nomField);
        return true;
    }

    private boolean validateEquipe1() {
        if (equipe1Box.getValue() == null) {
            showFieldError(equipe1Error, equipe1Box, "Champ obligatoire");
            return false;
        }
        hideFieldError(equipe1Error, equipe1Box);
        return true;
    }

    private boolean validateEquipe2() {
        if (equipe2Box.getValue() == null) {
            showFieldError(equipe2Error, equipe2Box, "Champ obligatoire");
            return false;
        }
        hideFieldError(equipe2Error, equipe2Box);
        return true;
    }

    private boolean validateScore1() {
        if (score1Field.getText().trim().isEmpty()) {
            showFieldError(score1Error, score1Field, "Champ obligatoire");
            return false;
        }
        hideFieldError(score1Error, score1Field);
        return true;
    }

    private boolean validateScore2() {
        if (score2Field.getText().trim().isEmpty()) {
            showFieldError(score2Error, score2Field, "Champ obligatoire");
            return false;
        }
        hideFieldError(score2Error, score2Field);
        return true;
    }

    private boolean validateDateMatch() {
        if (dateMatchPicker.getValue() == null) {
            showFieldError(dateMatchError, dateMatchPicker, "Champ obligatoire");
            return false;
        }
        hideFieldError(dateMatchError, dateMatchPicker);
        return true;
    }

    private boolean validateDateFin() {
        if (dateFinPicker.getValue() == null) {
            showFieldError(dateFinError, dateFinPicker, "Champ obligatoire");
            return false;
        }
        hideFieldError(dateFinError, dateFinPicker);
        return true;
    }

    // ================= HELPERS =================
    private void showFieldError(Label label, Control input, String msg) {
        label.setText(msg);
        if (!label.getStyleClass().contains("error-label")) {
            label.getStyleClass().add("error-label");
        }
        label.setVisible(true);
        label.setManaged(true);
        if (input != null && !input.getStyleClass().contains("form-input-error")) {
            input.getStyleClass().add("form-input-error");
        }
    }

    private void hideFieldError(Label label, Control input) {
        label.setVisible(false);
        label.setManaged(false);
        if (input != null) {
            input.getStyleClass().remove("form-input-error");
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        if (!errorLabel.getStyleClass().contains("error-label")) {
            errorLabel.getStyleClass().add("error-label");
        }
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    // ================= EXTERNAL =================
    public void setMatch(Matchs match) {
        this.match = match;
    }

    public void setParentController(MatchController parentController) {
        this.parentController = parentController;
    }

    // ================= CLOSE =================
    @FXML
    public void closeWindow() {
        if (parentController != null) {
            parentController.closeForm();
        } else {
            Stage stage = (Stage) saveBtn.getScene().getWindow();
            stage.close();
        }
    }
    // AJOUTER dans FormMatchController.java

    private boolean resultMode = false;


    // ================= RESULT MODE =================
    public void setResultMode(boolean resultMode) {

        this.resultMode = resultMode;

        if (resultMode) {

            formTitleLabel.setText("Mettre Résultat");

            nomField.setDisable(true);
            equipe1Box.setDisable(true);
            equipe2Box.setDisable(true);

            nomField.setOpacity(0.7);
            equipe1Box.setOpacity(0.7);
            equipe2Box.setOpacity(0.7);

            // 🔥 ensure score buttons are enabled for result mode
            incScore1Btn.setDisable(false);
            decScore1Btn.setDisable(false);
            incScore2Btn.setDisable(false);
            decScore2Btn.setDisable(false);
        }
    }
}