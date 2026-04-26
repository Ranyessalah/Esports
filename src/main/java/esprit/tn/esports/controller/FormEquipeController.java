package esprit.tn.esports.controller;

import esprit.tn.esports.entite.Coach;
import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Player;
import esprit.tn.esports.service.CoachService;
import esprit.tn.esports.service.EquipeService;
import esprit.tn.esports.service.PlayerService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.util.StringConverter;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class FormEquipeController {

    @FXML private Label formTitleLabel;
    @FXML private TextField nameField;
    @FXML private TextField gameField;
    @FXML private TextField catField;

    @FXML private Label nameError;
    @FXML private Label gameError;
    @FXML private Label catError;
    @FXML private Label imageError;

    @FXML private ComboBox<Coach> coachBox;
    @FXML private Label coachError;

    @FXML private TextField playerSearchField;
    @FXML private ListView<Player> playersListView;
    @FXML private Label playersError;

    @FXML private ImageView preview;
    @FXML private Button saveBtn;

    private String imagePath;
    private Equipe equipe;
    private AdminEquipeController parent;

    private ObservableList<Player> allPlayers = FXCollections.observableArrayList();
    private FilteredList<Player> filteredPlayers;
    private ObservableList<Player> selectedPlayers = FXCollections.observableArrayList();

    private EquipeService service = new EquipeService();
    private CoachService coachService = new CoachService();
    private PlayerService playerService = new PlayerService();

    @FXML
    public void initialize() {
        initValidation();
        initSelectors();
        makePreviewRound();
    }

    private void makePreviewRound() {
        // Create a circular clip
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(75, 75, 75);
        preview.setClip(clip);
        
        // Ensure the preview is always square for the circle clip
        preview.setFitWidth(150);
        preview.setFitHeight(150);
    }

    private void initSelectors() {
        // Coach Selector
        coachBox.setItems(FXCollections.observableArrayList(coachService.getAll()));
        coachBox.setConverter(new StringConverter<Coach>() {
            @Override
            public String toString(Coach coach) {
                return (coach == null || coach.getUser() == null) ? "" : coach.getUser().getEmail();
            }

            @Override
            public Coach fromString(String string) {
                return null;
            }
        });

    private void initSelectors() {
        // Coach Selector
        coachBox.setItems(FXCollections.observableArrayList(coachService.getAll()));
        coachBox.setConverter(new StringConverter<Coach>() {
            @Override
            public String toString(Coach coach) {
                return (coach == null || coach.getUser() == null) ? "" : coach.getUser().getEmail();
            }
            @Override
            public Coach fromString(String string) { return null; }
        });

        // Player Searchable List
        allPlayers.setAll(playerService.getAll());
        filteredPlayers = new FilteredList<>(allPlayers, p -> true);
        
        playerSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredPlayers.setPredicate(player -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String filter = newVal.toLowerCase();
                return player.getUser() != null && player.getUser().getEmail().toLowerCase().contains(filter);
            });
        });

        playersListView.setItems(filteredPlayers);
        playersListView.setCellFactory(lv -> new ListCell<Player>() {
            @Override
            protected void updateItem(Player player, boolean empty) {
                super.updateItem(player, empty);
                if (empty || player == null || player.getUser() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    CheckBox cb = new CheckBox(player.getUser().getEmail());
                    cb.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");
                    
                    // Sync with selectedPlayers
                    cb.setSelected(selectedPlayers.stream().anyMatch(p -> p.getId() == player.getId()));
                    
                    cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal) {
                            if (selectedPlayers.stream().noneMatch(p -> p.getId() == player.getId())) {
                                selectedPlayers.add(player);
                            }
                        } else {
                            selectedPlayers.removeIf(p -> p.getId() == player.getId());
                        }
                        validateForm();
                    });

                    HBox hbox = new HBox(10, cb);
                    hbox.setAlignment(Pos.CENTER_LEFT);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    String level = (player.getNiveau() == null) ? "default" : player.getNiveau().toLowerCase();
                    Label badge = new Label(level.toUpperCase());
                    badge.getStyleClass().addAll("level-badge", "badge-" + level);

                    hbox.getChildren().addAll(spacer, badge);
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });
    }

    private void setCircularImage(ImageView iv, String path, double radius) {
        iv.setFitWidth(radius * 2);
        iv.setFitHeight(radius * 2);
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(radius, radius, radius);
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
    }

    // 🔥 VALIDATION LIVE
    private void initValidation() {

        nameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateName();
        });

        gameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateGame();
        });

        catField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateCategory();
        });

        nameField.textProperty().addListener((obs, o, n) -> validateForm());
        gameField.textProperty().addListener((obs, o, n) -> validateForm());
        catField.textProperty().addListener((obs, o, n) -> validateForm());

        coachBox.valueProperty().addListener((obs, o, n) -> {
            validateCoach();
            validateForm();
        });
    }

    private boolean validateName() {
        if (nameField.getText().isEmpty()) {
            showFieldError(nameError, nameField, "Nom de l'équipe requis");
            return false;
        }
        hideFieldError(nameError, nameField);
        return true;
    }

    private boolean validateGame() {
        if (gameField.getText().isEmpty()) {
            showFieldError(gameError, gameField, "Jeu requis");
            return false;
        }
        hideFieldError(gameError, gameField);
        return true;
    }

    private boolean validateCategory() {
        if (catField.getText().isEmpty()) {
            showFieldError(catError, catField, "Catégorie requise");
            return false;
        }
        hideFieldError(catError, catField);
        return true;
    }

    private boolean validateCoach() {
        if (coachBox.getValue() == null) {
            showFieldError(coachError, coachBox, "Coach requis");
            return false;
        }
        hideFieldError(coachError, coachBox);
        return true;
    }

    private boolean validatePlayers() {
        if (selectedPlayers.isEmpty()) {
            showFieldError(playersError, playersListView, "Au moins un membre requis");
            return false;
        }
        hideFieldError(playersError, playersListView);
        return true;
    }

    private boolean validateImage() {
        if (imagePath == null) {
            showFieldError(imageError, null, "Logo requis");
            return false;
        }
        hideFieldError(imageError, null);
        return true;
    }

    private void validateForm() {
        boolean valid = validateName() & validateGame() & validateCategory() & validateCoach() & validatePlayers();
        saveBtn.setDisable(!valid);
    }

    // STYLE HELPERS
    private void showFieldError(Label label, Node input, String msg) {
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

    private void hideFieldError(Label label, Node input) {
        label.setVisible(false);
        label.setManaged(false);
        if (input != null) {
            input.getStyleClass().remove("form-input-error");
        }
    }

    // IMAGE
    @FXML
    void uploadImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fc.showOpenDialog(null);

        if (file != null) {
            imagePath = file.getAbsolutePath();
            preview.setImage(new Image("file:" + imagePath));
            imageError.setText("");
        }
    }

    // SETTERS
    public void setEquipe(Equipe e) {
        this.equipe = e;

        if (e != null) {
            formTitleLabel.setText("Modifier Équipe");
            nameField.setText(e.getNom());
            gameField.setText(e.getGame());
            catField.setText(e.getCategorie());
            imagePath = e.getLogo();
            
            // Select Coach
            if (e.getCoach() != null) {
                for (Coach c : coachBox.getItems()) {
                    if (c.getId() == e.getCoach().getId()) {
                        coachBox.setValue(c);
                        break;
                    }
                }
            }

            // Select Players
            List<Player> teamMembers = playerService.getPlayersByEquipe(e.getId());
            selectedPlayers.setAll(teamMembers);
            playersListView.refresh();

            try {
                preview.setImage(new Image("file:" + imagePath));
            } catch (Exception ex) {}
            saveBtn.setText("Mettre à jour");
        }
    }

    public void setParentController(AdminEquipeController parent) {
        this.parent = parent;
    }

    // SAVE
    @FXML
    void save() {

        if (!validateName() | !validateGame() | !validateCategory() | !validateCoach() | !validatePlayers() | !validateImage()) {
            return;
        }

        Equipe e = (equipe == null) ? new Equipe() : equipe;

        e.setNom(nameField.getText());
        e.setGame(gameField.getText());
        e.setCategorie(catField.getText());
        e.setLogo(imagePath);

        // Set Coach
        e.setCoach(coachBox.getValue());
        
        // Set Players
        e.setPlayers(selectedPlayers);

        if (equipe == null)
            service.addEquipe(e);
        else
            service.updateEquipe(e);

        parent.refresh();
        closeWindow();
    }

    @FXML
    public void closeWindow() {
        if (parent != null) {
            parent.closeForm();
        } else {
            Stage stage = (Stage) saveBtn.getScene().getWindow();
            stage.close();
        }
    }
}