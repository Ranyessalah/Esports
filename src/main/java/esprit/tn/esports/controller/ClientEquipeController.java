package esprit.tn.esports.controller;

import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.service.EquipeService;
import esprit.tn.esports.utils.QRCodeDialog;
import esprit.tn.esports.utils.QRCodeUtil;
import esprit.tn.esports.utils.QRScannerDialog;
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
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.event.ActionEvent;
import javafx.scene.input.Clipboard;

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

        Button qrBtn = new Button("Code QR");
        qrBtn.getStyleClass().add("btn-view");
        qrBtn.setOnAction(ev -> new QRCodeDialog().showShareableTeamQr(e));

        Button view = new Button("Voir détails");
        view.getStyleClass().add("btn-view");
        view.setOnAction(ev -> openDetailsAfterQrScan(e));

        VBox actions = new VBox(8, qrBtn, view);
        actions.setAlignment(Pos.CENTER);

        card.getChildren().addAll(img, name, game, actions);

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

    // ================= DETAILS (vérification par QR : collage téléphone / web, webcam optionnelle) =================
    private void openDetailsAfterQrScan(Equipe e) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Vérification — " + e.getNom());
        dialog.setHeaderText(null);

        Label info = new Label(
                "1) Cliquez sur « Code QR » pour cette équipe et laissez le QR visible à l’écran.\n"
                        + "2) Scannez-le avec l’appareil photo / une appli QR sur votre téléphone, ou un scanner en ligne dans le navigateur.\n"
                        + "3) Copiez tout le texte affiché (ou seulement la 1ʳᵉ ligne, ex. « 12|NomEquipe ») et collez-le ci-dessous.\n\n"
                        + "Sans webcam sur ce PC : c’est le moyen habituel pour valider l’accès."
        );
        info.setWrapText(true);
        info.setMaxWidth(460);
        info.setStyle("-fx-text-fill: #e5e7eb;");

        TextArea pasteArea = new TextArea();
        pasteArea.setPromptText("Collez ici le texte lu après le scan…");
        pasteArea.setPrefRowCount(6);
        pasteArea.setWrapText(true);

        Button pasteClip = new Button("Coller depuis le presse-papiers");
        pasteClip.setOnAction(ev -> {
            Clipboard cb = Clipboard.getSystemClipboard();
            if (cb.hasString()) {
                pasteArea.setText(cb.getString());
            }
        });

        VBox box = new VBox(10, info, pasteArea, pasteClip);
        box.setStyle("-fx-padding: 8 0 0 0;");
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().setStyle("-fx-background-color: #0f111a;");

        ButtonType validateType = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        ButtonType webcamType = new ButtonType("Webcam (PC)", ButtonBar.ButtonData.HELP);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(validateType, webcamType, cancelType);

        dialog.setResultConverter(btn -> btn);

        final Button validateBtn = (Button) dialog.getDialogPane().lookupButton(validateType);
        validateBtn.addEventFilter(ActionEvent.ACTION, event -> {
            if (pasteArea.getText() == null || pasteArea.getText().trim().isEmpty()) {
                event.consume();
                Alert w = new Alert(Alert.AlertType.WARNING);
                w.setHeaderText(null);
                w.setContentText("Collez d’abord le texte affiché après le scan du QR (téléphone ou site en ligne).");
                w.showAndWait();
            }
        });

        Optional<ButtonType> choice = dialog.showAndWait();
        if (choice.isEmpty() || choice.get() == cancelType) {
            return;
        }
        if (choice.get() == webcamType) {
            new QRScannerDialog().startScanner(scanned -> verifyQrPayloadAndOpenDetails(e, scanned));
            return;
        }
        if (choice.get() == validateType) {
            verifyQrPayloadAndOpenDetails(e, pasteArea.getText().trim());
        }
    }

    private void verifyQrPayloadAndOpenDetails(Equipe e, String payload) {
        int id = QRCodeUtil.extractTeamIdFromQR(payload);
        if (id == e.getId()) {
            openDetails(e);
            return;
        }
        Alert err = new Alert(Alert.AlertType.ERROR);
        err.setTitle("QR invalide");
        err.setHeaderText(null);
        if (id < 0) {
            err.setContentText("Texte non reconnu. Assurez-vous de coller le résultat du scan du bon QR (équipe « "
                    + e.getNom() + " »).");
        } else {
            err.setContentText("Ce texte correspond à une autre équipe (ID " + id + "). Utilisez le QR affiché via « Code QR » pour « "
                    + e.getNom() + " ».");
        }
        err.showAndWait();
    }

    private void openDetails(Equipe e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/EquipeDetails.fxml"));
            Parent root = loader.load();

            EquipeDetailsController controller = loader.getController();
            controller.setEquipe(e);

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