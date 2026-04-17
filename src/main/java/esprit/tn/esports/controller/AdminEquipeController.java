package esprit.tn.esports.controller;

import esprit.tn.esports.*;
import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.service.EquipeService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.*;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class AdminEquipeController {

    @FXML private FlowPane cardContainer;
    @FXML private TextField searchField;
    @FXML private Label countLabel;
    @FXML private BorderPane mainBorderPane;
    @FXML private VBox mainContent;

    private EquipeService service = new EquipeService();

    @FXML
    public void initialize() {
        loadData();
    }

    // LOAD CARDS
    private void loadData() {
        cardContainer.getChildren().clear();
        List<Equipe> equipes = service.getAll();

        for (Equipe e : equipes) {
            cardContainer.getChildren().add(createCard(e));
        }
        updateCount(equipes.size());
    }

    private void updateCount(int size) {
        if (countLabel != null) {
            countLabel.setText(size + (size > 1 ? " équipes" : " équipe"));
        }
    }

    // CREATE CARD UI
    private VBox createCard(Equipe e) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        ImageView img = new ImageView();
        img.setFitWidth(80);
        img.setFitHeight(80);

        try {
            File file = new File(e.getLogo());
            if (file.exists()) {
                img.setImage(new Image(file.toURI().toString()));
            } else {
                // Fallback image or icon could be set here
            }
        } catch (Exception ex) {
            System.err.println("Erreur chargement logo: " + ex.getMessage());
        }

        // 🔥 Make logo round
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(40, 40, 40);
        img.setClip(clip);

        Label name = new Label(e.getNom());
        name.getStyleClass().add("card-title");

        Label game = new Label(e.getGame());
        game.getStyleClass().add("card-sub");

        Button view = new Button("voir");
        view.getStyleClass().add("btn-view");
        view.setOnAction(ev -> openDetailsPage(e));
        Button edit = new Button("modifier");
        edit.getStyleClass().add("btn-edit");
        edit.setOnAction(ev -> openEditModal(e));

        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("btn-delete");

        delete.setOnAction(ev -> {
            // ... (rest of logic unchanged from previous viewed version)
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Suppression d'équipe");
            confirm.setContentText("Voulez-vous vraiment supprimer l'équipe : " + e.getNom() + " ?");

            // Personnaliser boutons
            ButtonType ouiBtn = new ButtonType("Oui", ButtonBar.ButtonData.OK_DONE);
            ButtonType nonBtn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

            confirm.getButtonTypes().setAll(ouiBtn, nonBtn);

            Optional<ButtonType> result = confirm.showAndWait();

            if (result.isPresent() && result.get() == ouiBtn) {
                if (service.deleteEquipe(e.getId())) {
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Succès");
                    success.setHeaderText(null);
                    success.setContentText("Équipe supprimée avec succès !");
                    success.show();

                    loadData();
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Échec de suppression");
                    error.setHeaderText(null);
                    error.setContentText("Impossible de supprimer cette équipe. Elle est probablement liée à des matchs existants.");
                    error.show();
                }
            }
        });

        HBox actions = new HBox(10, view, edit, delete);
        actions.setAlignment(javafx.geometry.Pos.CENTER);

        card.getChildren().addAll(img, name, game, actions);
        card.setAlignment(javafx.geometry.Pos.CENTER);

        return card;
    }

    // SEARCH
    @FXML
    void search() {
        String text = searchField.getText().toLowerCase();
        cardContainer.getChildren().clear();

        List<Equipe> all = service.getAll();
        int count = 0;
        for (Equipe e : all) {
            if (e.getNom().toLowerCase().contains(text)) {
                cardContainer.getChildren().add(createCard(e));
                count++;
            }
        }
        updateCount(count);
    }

    // MODAL ADD
    @FXML
    void openAddModal() {
        openForm(null);
    }

    void openEditModal(Equipe e) {
        openForm(e);
    }

    private void openForm(Equipe e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/formEquipe.fxml"));
            Parent formRoot = loader.load();

            FormEquipeController controller = loader.getController();
            controller.setEquipe(e);
            controller.setParentController(this);

            mainBorderPane.setCenter(formRoot);

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Impossible d'ouvrir le formulaire.");
            alert.show();
        }
    }

    public void closeForm() {
        mainBorderPane.setCenter(mainContent);
        refresh();
    }

    // REFRESH
    public void refresh() {
        loadData();
    }

    // DETAILS MODAL
    private void showDetails(Equipe e) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Team Details");

        alert.setHeaderText(e.getNom());
        alert.setContentText(
                "Game: " + e.getGame() +
                        "\nCategory: " + e.getCategorie()
        );

        alert.showAndWait();
    }

    private void openDetailsPage(Equipe e) {
        navigateToDetailsView(e);
    }

    private void navigateToDetailsView(Equipe e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/EquipeDetails.fxml"));
            Parent root = loader.load();

            EquipeDetailsController controller = loader.getController();
            controller.setEquipe(e);

            // 🔥 Custom back handler to restore the list
            controller.setOnBack(() -> {
                mainBorderPane.setCenter(mainContent);
                refresh();
            });

            // ✅ Replace center instead of opening new window
            mainBorderPane.setCenter(root);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void goMatchs() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/matchIndex.fxml"));
            Parent root = loader.load();

            // 🔥 récupérer la fenêtre actuelle
            Stage stage = (Stage) mainBorderPane.getScene().getWindow();

            // 🔥 changer la scène
            Scene scene = new Scene(root, 1200, 760);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Impossible d'ouvrir la page Matchs.");
            alert.show();
        }
    }

    @FXML
    public void goStats(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/esprit/tn/esports/stats.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root, 1200, 760));
            stage.setTitle("Classement des équipes");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
         }
    }

    @FXML
    public void logout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/esprit/tn/esports/Login.fxml")
            );
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.setTitle("ClutchX - Connexion");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}