package esprit.tn.esports.controller;

import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Matchs;
import esprit.tn.esports.service.EquipeService;
import esprit.tn.esports.service.MatchService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    @FXML private Label errorLabel;

    @FXML private Label nomError;
    @FXML private Label equipe1Error;
    @FXML private Label equipe2Error;
    @FXML private Label score1Error;
    @FXML private Label score2Error;
    @FXML private Label dateMatchError;

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
        statusBoxField.setValue("a jouer");
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

            for (int m = 0; m < 60; m += 15) {
                String time = String.format("%02d:%02d", h, m);
                hourStartBox.getItems().add(time);
            }
        }

        hourStartBox.setValue("18:00");
        
        hourStartBox.setEditable(true);
        
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

        if (m.getDateMatch() != null) {
            dateMatchPicker.setValue(m.getDateMatch().toLocalDate());
            hourStartBox.setValue(String.format("%02d:%02d", m.getDateMatch().getHour(), m.getDateMatch().getMinute()));
        }
        if (m.getStatut() != null) {
            statusBox.setVisible(true);
            statusBox.setManaged(true);
            statusBoxField.setValue(m.getStatut());
        }

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
            Image img = new Image(getClass().getResourceAsStream("/esprit/tn/esports/images/" + e.getLogo()));
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
        hourStartBox.valueProperty().addListener((obs, o, n) -> validateAll());
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

            String startVal = hourStartBox.getValue();
            String[] hs = startVal.split(":");

            LocalDateTime start = dateMatchPicker.getValue().atTime(
                    Integer.parseInt(hs[0]),
                    Integer.parseInt(hs[1])
            );

            // Statut (normalisé)
            String newStatus = (statusBoxField.getValue() == null) ? null : statusBoxField.getValue().trim().toLowerCase();

            if (match.getId() == 0) {
                // Création: statut fixé et dates issues du formulaire
                match.setDateMatch(start);
                match.setDateFinMatch(start.plusHours(2));
                match.setStatut("a jouer"); // Always "a jouer" for new matches
            } else {
                // Edition:
                // - On permet de modifier le statut sans casser le chrono.
                // - Si on passe à en_cours et que la date de début est absente, on démarre maintenant.
                // - Sinon on garde la date déjà enregistrée (ne pas la réécrire systématiquement).
                if (newStatus != null && (newStatus.contains("en_cours") || newStatus.contains("en cours"))) {
                    if (match.getDateMatch() == null) {
                        match.setDateMatch(LocalDateTime.now());
                    }
                } else {
                    // Pour les autres statuts, on autorise l'admin à modifier la date via le formulaire.
                    match.setDateMatch(start);
                }
                match.setDateFinMatch(match.getDateMatch() != null ? match.getDateMatch().plusHours(2) : null);
                match.setStatut(newStatus);
            }

            if (match.getId() == 0) {
                matchService.add(match);
            } else {
                matchService.update(match);
            }

            // Déclencher une vérification immédiate des notifications après l'enregistrement
            new Thread(() -> {
                try {
                    Thread.sleep(300); // Petit délai pour laisser la DB se stabiliser
                    Platform.runLater(() -> {
                        esprit.tn.esports.utils.MatchNotificationService notifService =
                                esprit.tn.esports.utils.MatchNotificationService.getInstance();
                        notifService.resetMatchNotifications(match.getId());
                        notifService.checkNow();
                        System.out.println("[FormMatch] Notification reset & check triggered pour ID: " + match.getId());
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

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
                        validateDateMatch();

        if (!ok) return false;

        // équipes différentes
        if (equipe1Box.getValue().getId() == equipe2Box.getValue().getId()) {
            showFieldError(equipe2Error, equipe2Box, "Les équipes doivent être différentes");
            return false;
        }

        String startVal = hourStartBox.getValue();
        if (startVal == null || !startVal.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            showError("Format de l'heure de début invalide (ex: 18:30)");
            return false;
        }
        String[] hs = startVal.split(":");

        LocalDateTime start = dateMatchPicker.getValue().atTime(
                Integer.parseInt(hs[0]),
                Integer.parseInt(hs[1])
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

    @FXML
    public void openGoogleCalendarEvent() {
        try {
            hideError();

            if (dateMatchPicker.getValue() == null) {
                showError("Choisissez la date/heure du match avant d'ouvrir Google Calendar.");
                return;
            }

            String startVal = hourStartBox.getValue();
            if (startVal == null || !startVal.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                showError("Heure invalide. Utilisez le format HH:mm (ex: 18:30).");
                return;
            }

            String[] hs = startVal.split(":");
            LocalDateTime start = dateMatchPicker.getValue().atTime(
                    Integer.parseInt(hs[0]),
                    Integer.parseInt(hs[1])
            );
            LocalDateTime end = start.plusHours(2);

            DateTimeFormatter gCalFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
            String startUtc = start.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneId.of("UTC"))
                    .format(gCalFmt);
            String endUtc = end.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneId.of("UTC"))
                    .format(gCalFmt);

            String team1 = equipe1Box.getValue() != null ? equipe1Box.getValue().getNom() : "Equipe 1";
            String team2 = equipe2Box.getValue() != null ? equipe2Box.getValue().getNom() : "Equipe 2";
            String title = nomField.getText() != null && !nomField.getText().trim().isEmpty()
                    ? nomField.getText().trim()
                    : (team1 + " vs " + team2);

            String details = "Match e-sports: " + team1 + " vs " + team2
                    + "\nStatut: " + (statusBoxField.getValue() != null ? statusBoxField.getValue() : "a jouer");

            String url = "https://calendar.google.com/calendar/render?action=TEMPLATE"
                    + "&text=" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                    + "&details=" + URLEncoder.encode(details, StandardCharsets.UTF_8)
                    + "&dates=" + startUtc + "/" + endUtc;

            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                showError("Ouverture du navigateur non supportee sur cet environnement.");
                return;
            }

            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            showError("Impossible d'ouvrir Google Calendar: " + e.getMessage());
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