package esprit.tn.esports.utils;

import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.service.EquipeService;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class QRCodeDialog {

    private final EquipeService equipeService = new EquipeService();

    public void showShareableTeamQr(Equipe equipeRef, Stage ownerStage) {
        try {
            Equipe equipe = equipeService.getById(equipeRef.getId());
            if (equipe == null) equipe = equipeRef;

            final String localIpAddress = TeamWebServer.getLocalIp();
            final String qrUrl = "http://" + localIpAddress + ":4567/equipe/" + equipe.getId();
            final Image qrImage = QRCodeUtil.generateQRCodeImage(qrUrl, 350, 350);

            VBox root = new VBox(15);
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: white; -fx-padding: 25px;");

            ImageView qrView = new ImageView(qrImage);
            qrView.setFitWidth(320);
            qrView.setFitHeight(320);

            Button closeBtn = new Button("✖ Fermer");
            closeBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 8 25; -fx-cursor: hand; -fx-background-radius: 5px;");
            closeBtn.setOnAction(e -> ((Stage) root.getScene().getWindow()).close());

            root.getChildren().addAll(qrView, closeBtn);

            Stage qrStage = new Stage();
            qrStage.initOwner(ownerStage);
            qrStage.initModality(Modality.WINDOW_MODAL);
            qrStage.setTitle("QR Code");
            qrStage.setScene(new Scene(root));
            qrStage.setResizable(false);
            qrStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}