package controllers.gestion_match;

import entities.Equipe;
import services.gestion_match.EquipeService;
import utils.QRCodeDialog;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ClientEquipeController {

    @FXML private FlowPane cardContainer;
    @FXML private TextField searchField;
    @FXML private Label countLabel;

    private EquipeService service = new EquipeService();
    private List<Equipe> allEquipes;

    @FXML
    public void initialize() {
        refresh();
    }

    // ================= LOAD =================
    @FXML
    public void refresh() {
        allEquipes = service.getAll();
        display(allEquipes);
        updateCount(allEquipes.size());
    }

    private void updateCount(int size) {
        countLabel.setText(size + (size > 1 ? " équipes" : " équipe"));
    }

    private void display(List<Equipe> equipes) {
        cardContainer.getChildren().clear();

        for (Equipe e : equipes) {
            cardContainer.getChildren().add(createCard(e));
        }
    }

    // ================= CARD =================
    private VBox createCard(Equipe e) {

        VBox card = new VBox(12);
        card.setPrefWidth(220);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card");

        ImageView img = new ImageView();
        img.setFitWidth(80);
        img.setFitHeight(80);

        try {
            File file = new File(e.getLogo());
            if (file.exists()) {
                img.setImage(new Image(file.toURI().toString()));
            }
        } catch (Exception ex) {}

        // 🔥 Make logo round
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(40, 40, 40);
        img.setClip(clip);

        Label name = new Label(e.getNom());
        name.getStyleClass().add("card-title");

        Label game = new Label(e.getGame());
        game.getStyleClass().add("card-sub");

        Button qrBtn = new Button("Voir Équipe");
        qrBtn.getStyleClass().add("btn-view");
        qrBtn.setOnAction(ev -> {
            Stage owner = (Stage) cardContainer.getScene().getWindow();
            new QRCodeDialog().showShareableTeamQr(e, owner);
        });

        card.getChildren().addAll(img, name, game, qrBtn);

        return card;
    }

    // ================= DETAILS =================
    private void openDetails(Equipe e) {
        try {
            // Capture stage BEFORE navigating — cardContainer loses its scene after setScene()
            Stage stage = (Stage) cardContainer.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_match/EquipeDetails.fxml"));
            Parent root = loader.load();

            EquipeDetailsController ctrl = loader.getController();
            ctrl.setEquipe(e);
            ctrl.setOnBack(() -> {
                try {
                    FXMLLoader backLoader = new FXMLLoader(getClass().getResource("/gestion_match/equipeIndex_client.fxml"));
                    Parent backRoot = backLoader.load();
                    stage.setScene(new Scene(backRoot, 1200, 760));
                    stage.show();
                } catch (Exception ex) { ex.printStackTrace(); }
            });

            stage.setScene(new Scene(root, 1200, 760));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ================= SEARCH =================
    @FXML
    void search() {
        String text = searchField.getText().toLowerCase();

        List<Equipe> filtered = allEquipes.stream()
                .filter(e -> e.getNom().toLowerCase().contains(text))
                .collect(Collectors.toList());

        display(filtered);
        updateCount(filtered.size());
    }

    // ================= NAVIGATION =================
    @FXML
    private void goMatchs(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_match/matchIndex_client.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goStats(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_match/stats_client.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void logout(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.setTitle("ClutchX - Connexion");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
