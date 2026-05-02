package esprit.tn.esports.utils;

import esprit.tn.esports.entite.Equipe;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class SimpleQrCodeDialog {

    public void showQr(Equipe equipe, Stage ownerStage) {

        // ✅ DATA simple
        String data =
                "=== TEAM DETAILS ===\n\n" +
                        "Name: " + equipe.getNom() + "\n" +
                        "Game: " + equipe.getGame() + "\n" +
                        "Category: " + equipe.getCategorie();

        // ✅ QR
        Image qrImage = QRCodeGenerator.generateQRCodeWithLogo(
                data,
                300,
                equipe.getLogo()
        );

        ImageView imageView = new ImageView(qrImage);
        imageView.setFitWidth(250);
        imageView.setFitHeight(250);

        Label title = new Label("QR Code - " + equipe.getNom());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");

        Button downloadBtn = new Button("📥 Télécharger");
        downloadBtn.setOnAction(e -> saveQR(imageView.getImage(), equipe.getNom()));

        VBox root = new VBox(15, title, imageView, downloadBtn);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0f172a; -fx-padding: 20;");

        Stage stage = new Stage();
        stage.initOwner(ownerStage);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(root, 320, 400));
        stage.setTitle("QR Code");
        stage.show();
    }

    private void saveQR(Image image, String name) {
        try {
            FileChooser fc = new FileChooser();
            fc.setInitialFileName("QR_" + name + ".png");

            File file = fc.showSaveDialog(null);
            if (file != null) {
                BufferedImage buffered = javafx.embed.swing.SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(buffered, "png", file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}