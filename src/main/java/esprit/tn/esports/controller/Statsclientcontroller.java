package esprit.tn.esports.controller;

import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.StatsRow;
import esprit.tn.esports.service.ChatbotStatsService;
import esprit.tn.esports.service.EquipeService;
import esprit.tn.esports.service.StatsService;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Statsclientcontroller {

    // ── TABLE ──
    @FXML private Label   totalLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> gameFilter;
    @FXML private ComboBox<String> sortFilter;
    @FXML private TableView<StatsRow> tableView;
    @FXML private TableColumn<StatsRow, Integer> rankCol;
    @FXML private TableColumn<StatsRow, String>  teamCol;
    @FXML private TableColumn<StatsRow, String>  gameCol;
    @FXML private TableColumn<StatsRow, Integer> playedCol;
    @FXML private TableColumn<StatsRow, Integer> winCol;
    @FXML private TableColumn<StatsRow, Integer> drawCol;
    @FXML private TableColumn<StatsRow, Integer> lossCol;
    @FXML private TableColumn<StatsRow, Integer> bpCol;
    @FXML private TableColumn<StatsRow, Integer> bcCol;
    @FXML private TableColumn<StatsRow, Integer> diffCol;
    @FXML private TableColumn<StatsRow, Integer> ptsCol;

    // ── CHAT ──
    @FXML private TextField  chatInput;
    @FXML private VBox       chatMessagesBox;   // fx:id="chatMessagesBox"
    @FXML private ScrollPane chatScrollPane;    // fx:id="chatScrollPane"
    @FXML private HBox       suggestionsBox;    // fx:id="suggestionsBox"

    // ── SERVICES ──
    private final StatsService            service        = new StatsService();
    private final EquipeService           equipeService  = new EquipeService();
    private       ObservableList<StatsRow> masterData    = FXCollections.observableArrayList();
    private final ChatbotStatsService     chatbotService = new ChatbotStatsService(() -> masterData);

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[] SUGGESTIONS = {
            "top 5", "leader", "meilleure attaque", "pire défense",
            "compare ESS vs Esperance", "combien d'équipes", "aide"
    };

    // ═══════════════════════════ INIT ═══════════════════════════

    @FXML
    public void initialize() {
        initTable();
        initFilters();
        initSuggestions();
        refresh();
        addBotMessage("Bonjour ! Je suis votre assistant statistiques ⚡\n"
                + "Posez une question sur le classement.\n"
                + "Tapez aide pour voir tout ce que je peux faire.");
    }

    // ═══════════════════════════ TABLE ═══════════════════════════

    private void initTable() {
        rankCol.setCellValueFactory(new PropertyValueFactory<>("rank"));
        rankCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label("#" + item);
                lbl.getStyleClass().add("table-rank-badge");
                setGraphic(lbl);
            }
        });

        teamCol.setCellValueFactory(new PropertyValueFactory<>("team"));
        teamCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                StatsRow row = getTableView().getItems().get(getIndex());
                HBox box = new HBox(10);
                box.setAlignment(Pos.CENTER_LEFT);
                ImageView iv = new ImageView();
                iv.setFitWidth(30); iv.setFitHeight(30);
                setCircularImage(iv, row.getLogo());
                Label lbl = new Label(item);
                lbl.getStyleClass().add("table-team-text");
                box.getChildren().addAll(iv, lbl);
                setGraphic(box);
            }
        });

        gameCol.setCellValueFactory(new PropertyValueFactory<>("game"));
        playedCol.setCellValueFactory(new PropertyValueFactory<>("played"));
        winCol.setCellValueFactory(new PropertyValueFactory<>("wins"));
        drawCol.setCellValueFactory(new PropertyValueFactory<>("draws"));
        lossCol.setCellValueFactory(new PropertyValueFactory<>("losses"));
        bpCol.setCellValueFactory(new PropertyValueFactory<>("bp"));
        bcCol.setCellValueFactory(new PropertyValueFactory<>("bc"));
        diffCol.setCellValueFactory(new PropertyValueFactory<>("diff"));

        ptsCol.setCellValueFactory(new PropertyValueFactory<>("points"));
        ptsCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(item + " PTS");
                lbl.getStyleClass().add("table-pts-pill");
                setGraphic(lbl);
            }
        });

        tableView.setRowFactory(tv -> {
            TableRow<StatsRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty() && e.getClickCount() == 1)
                    showTeamDetails(row.getItem());
            });
            return row;
        });
    }

    private void initFilters() {
        sortFilter.setItems(FXCollections.observableArrayList(
                "Classement officiel", "Victoires", "Défaites", "Différence"));
        sortFilter.setValue("Classement officiel");
    }

    // ═══════════════════════════ CHAT PANEL ═══════════════════════════

    private void initSuggestions() {
        if (suggestionsBox == null) return;
        suggestionsBox.getChildren().clear();
        for (String s : SUGGESTIONS) {
            Button chip = new Button(s);
            chip.getStyleClass().add("suggestion-chip");
            chip.setOnAction(e -> {
                if (chatInput != null) chatInput.setText(s);
                sendMessage(s);
            });
            suggestionsBox.getChildren().add(chip);
        }
    }

    @FXML
    public void askChatbot(javafx.event.ActionEvent event) {
        if (chatInput == null) return;
        String question = chatInput.getText() == null ? "" : chatInput.getText().trim();
        if (question.isEmpty()) return;
        chatInput.clear();
        sendMessage(question);
    }

    private void sendMessage(String question) {
        addUserMessage(question);
        showTypingIndicator();
        new Thread(() -> {
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
            String answer = chatbotService.answer(question);
            Platform.runLater(() -> {
                removeTypingIndicator();
                addBotMessage(answer);
            });
        }).start();
    }

    /** Bulle utilisateur (droite) */
    private void addUserMessage(String text) {
        if (chatMessagesBox == null) return;

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(row, new Insets(2, 0, 2, 60));

        VBox col = new VBox(4);
        col.setAlignment(Pos.CENTER_RIGHT);

        Label bubble = new Label(text);
        bubble.getStyleClass().add("bubble-user-text");
        bubble.setWrapText(true);
        bubble.setMaxWidth(260);

        StackPane wrapper = new StackPane(bubble);
        wrapper.getStyleClass().add("bubble-user");
        wrapper.setMaxWidth(260);

        Label time = new Label(LocalTime.now().format(TIME_FMT));
        time.getStyleClass().add("bubble-time");

        col.getChildren().addAll(wrapper, time);
        row.getChildren().add(col);

        animateBubble(row);
        chatMessagesBox.getChildren().add(row);
        scrollToBottom();
    }

    /** Bulle bot (gauche) avec avatar */
    private void addBotMessage(String text) {
        if (chatMessagesBox == null) return;

        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(row, new Insets(2, 60, 2, 0));

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("chat-avatar");
        avatar.setMinSize(32, 32); avatar.setMaxSize(32, 32);
        Label avatarIcon = new Label("🤖");
        avatarIcon.setStyle("-fx-font-size: 14px;");
        avatar.getChildren().add(avatarIcon);

        VBox col = new VBox(4);

        Label bubble = new Label(text);
        bubble.getStyleClass().add("bubble-bot-text");
        bubble.setWrapText(true);
        bubble.setMaxWidth(260);

        StackPane wrapper = new StackPane(bubble);
        wrapper.getStyleClass().add("bubble-bot");
        wrapper.setMaxWidth(260);

        Label time = new Label(LocalTime.now().format(TIME_FMT));
        time.getStyleClass().add("bubble-time");

        col.getChildren().addAll(wrapper, time);
        row.getChildren().addAll(avatar, col);

        animateBubble(row);
        chatMessagesBox.getChildren().add(row);
        scrollToBottom();
    }

    // ── Typing indicator ──
    private HBox typingNode = null;

    private void showTypingIndicator() {
        if (typingNode != null || chatMessagesBox == null) return;

        typingNode = new HBox(10);
        typingNode.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(typingNode, new Insets(2, 60, 2, 0));

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("chat-avatar");
        avatar.setMinSize(32, 32); avatar.setMaxSize(32, 32);
        Label ai = new Label("🤖"); ai.setStyle("-fx-font-size: 14px;");
        avatar.getChildren().add(ai);

        HBox dots = new HBox(5);
        dots.getStyleClass().add("typing-indicator");
        dots.setAlignment(Pos.CENTER);

        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(4);
            dot.getStyleClass().add("typing-dot");
            FadeTransition ft = new FadeTransition(Duration.millis(600), dot);
            ft.setFromValue(0.2); ft.setToValue(1.0);
            ft.setDelay(Duration.millis(i * 200));
            ft.setCycleCount(Animation.INDEFINITE);
            ft.setAutoReverse(true);
            ft.play();
            dots.getChildren().add(dot);
        }

        typingNode.getChildren().addAll(avatar, dots);
        chatMessagesBox.getChildren().add(typingNode);
        scrollToBottom();
    }

    private void removeTypingIndicator() {
        if (typingNode != null && chatMessagesBox != null) {
            chatMessagesBox.getChildren().remove(typingNode);
            typingNode = null;
        }
    }

    private void animateBubble(HBox node) {
        node.setOpacity(0);
        node.setTranslateY(12);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(250),
                        new KeyValue(node.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(node.translateYProperty(), 0, Interpolator.EASE_OUT))
        );
        tl.play();
    }

    private void scrollToBottom() {
        if (chatScrollPane != null)
            Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    @FXML
    public void showChatbotHelp(javafx.event.ActionEvent event) {
        addBotMessage(chatbotService.getHelpMessage());
    }

    @FXML
    public void clearChatbot(javafx.event.ActionEvent event) {
        if (chatMessagesBox != null) chatMessagesBox.getChildren().clear();
        if (chatInput != null) chatInput.clear();
        addBotMessage("Conversation vidée ✓ Posez une nouvelle question.");
    }

    // ═══════════════════════════ REFRESH / FILTER ═══════════════════════════

    @FXML
    public void refresh() {
        List<StatsRow> rows = service.getClassement();
        masterData.setAll(rows);
        loadGames();
        filterData();
    }

    private void loadGames() {
        List<String> games = masterData.stream()
                .map(StatsRow::getGame).distinct().sorted().collect(Collectors.toList());
        gameFilter.getItems().clear();
        gameFilter.getItems().add("Tous les jeux");
        gameFilter.getItems().addAll(games);
        gameFilter.setValue("Tous les jeux");
    }

    @FXML
    public void filterData() {
        String kw   = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String game = gameFilter.getValue();
        String sort = sortFilter.getValue();

        List<StatsRow> filtered = masterData.stream()
                .filter(r -> r.getTeam().toLowerCase().contains(kw))
                .filter(r -> game == null || game.equals("Tous les jeux") || r.getGame().equalsIgnoreCase(game))
                .collect(Collectors.toList());

        switch (sort == null ? "" : sort) {
            case "Victoires"  -> filtered.sort(Comparator.comparingInt(StatsRow::getWins).reversed());
            case "Défaites"   -> filtered.sort(Comparator.comparingInt(StatsRow::getLosses).reversed());
            case "Différence" -> filtered.sort(Comparator.comparingInt(StatsRow::getDiff).reversed());
            default           -> filtered.sort(Comparator.comparingInt(StatsRow::getPoints).reversed());
        }

        for (int i = 0; i < filtered.size(); i++) filtered.get(i).setRank(i + 1);

        tableView.setItems(FXCollections.observableArrayList(filtered));
        totalLabel.setText("Total équipes : " + filtered.size());
    }

    @FXML
    public void resetFilters() {
        searchField.clear();
        gameFilter.setValue("Tous les jeux");
        sortFilter.setValue("Classement officiel");
        filterData();
    }

    // ═══════════════════════════ HELPERS ═══════════════════════════

    private void setCircularImage(ImageView iv, String path) {
        try {
            if (path != null && !path.isEmpty())
                iv.setImage(new Image("file:" + path));
            else
                iv.setImage(new Image(getClass().getResourceAsStream(
                        "/esprit/tn/esports/images/default_team.png")));
            Circle clip = new Circle(iv.getFitWidth() / 2, iv.getFitHeight() / 2, iv.getFitWidth() / 2);
            iv.setClip(clip);
        } catch (Exception ignored) {}
    }

    private void showTeamDetails(StatsRow selected) {
        try {
            Equipe fullEquipe = equipeService.getById(selected.getTeamId());
            if (fullEquipe == null) return;
            navigateToTeamDetails(fullEquipe);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'affichage des détails de l'équipe.");
        }
    }

    private void navigateToTeamDetails(Equipe equipe) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/esprit/tn/esports/EquipeDetails.fxml"));
            Parent root = loader.load();
            EquipeDetailsController ctrl = loader.getController();
            ctrl.setEquipe(equipe);
            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir les détails de l'équipe.");
        }
    }

    // ═══════════════════════════ NAVIGATION ═══════════════════════════

    @FXML public void goEquipes(javafx.event.ActionEvent event) { navigate(event, "/esprit/tn/esports/equipeIndex_client.fxml"); }
    @FXML public void goMatchs(javafx.event.ActionEvent event)  { navigate(event, "/esprit/tn/esports/matchIndex_client.fxml"); }
    @FXML public void goStats(javafx.event.ActionEvent event)   { refresh(); }

    @FXML
    public void logout(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.setTitle("ClutchX - Connexion");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navigate(javafx.event.ActionEvent event, String fxml) {
        try {
            Parent root = new FXMLLoader(getClass().getResource(fxml)).load();
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir la page.");
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
