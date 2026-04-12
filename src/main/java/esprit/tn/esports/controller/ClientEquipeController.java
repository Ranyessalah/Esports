package esprit.tn.esports.controller;

import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.service.EquipeService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.Modality;
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

        Button view = new Button("Voir");
        view.getStyleClass().add("btn-view");
        view.setOnAction(ev -> openDetails(e));

        card.getChildren().addAll(img, name, game, view);

        return card;
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

    // ================= DETAILS =================
    private void openDetails(Equipe e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/EquipeDetails.fxml"));
            Parent root = loader.load();

            // 🔥 send data
            EquipeDetailsController controller = loader.getController();
            controller.setEquipe(e);

            // ✅ REPLACE CURRENT SCENE (Instead of new window)
            Stage stage = (Stage) cardContainer.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    // ================= NAVIGATION =================
    @FXML
    private void goMatchs(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/matchIndex_client.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/stats_client.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/Login.fxml"));
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