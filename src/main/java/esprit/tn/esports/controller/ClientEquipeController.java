package esprit.tn.esports.controller;

import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.service.EquipeService;
import esprit.tn.esports.utils.QRCodeUtil;
import esprit.tn.esports.utils.TeamWebServer;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class ClientEquipeController {

    @FXML
    private FlowPane cardContainer;
    @FXML
    private TextField searchField;
    @FXML
    private Label countLabel;

    private EquipeService service = new EquipeService();
    private List<Equipe> allEquipes;

    public ClientEquipeController() {
    }

    @FXML
    public void initialize() {
        this.refresh();
        // Démarrer le serveur web si nécessaire
        TeamWebServer.startServer();
        System.out.println("✅ ClientEquipeController initialisé");
        System.out.println("🌐 Serveur QR Code: " + TeamWebServer.getServerUrl());
    }

    @FXML
    public void refresh() {
        this.allEquipes = this.service.getAll();
        this.display(this.allEquipes);
        this.updateCount(this.allEquipes.size());
    }

    private void updateCount(int size) {
        this.countLabel.setText(size + (size > 1 ? " équipes" : " équipe"));
    }

    private void display(List<Equipe> equipes) {
        this.cardContainer.getChildren().clear();

        for(Equipe e : equipes) {
            this.cardContainer.getChildren().add(this.createCard(e));
        }
    }

    private VBox createCard(Equipe e) {
        VBox card = new VBox(12.0);
        card.setPrefWidth(220.0);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card");

        ImageView img = new ImageView();
        img.setFitWidth(80.0);
        img.setFitHeight(80.0);

        try {
            File file = new File(e.getLogo());
            if (file.exists()) {
                img.setImage(new Image(file.toURI().toString()));
            } else {
                try {
                    img.setImage(new Image(getClass().getResourceAsStream("/esprit/tn/esports/images/default_team.png")));
                } catch (Exception ex) {
                    // Ignorer
                }
            }
        } catch (Exception var8) {
            try {
                img.setImage(new Image(getClass().getResourceAsStream("/esprit/tn/esports/images/default_team.png")));
            } catch (Exception ex) {
                // Ignorer
            }
        }

        Circle clip = new Circle(40.0, 40.0, 40.0);
        img.setClip(clip);

        Label name = new Label(e.getNom());
        name.getStyleClass().add("card-title");

        Label game = new Label(e.getGame());
        game.getStyleClass().add("card-sub");

        Button qrBtn = new Button("📱 QR Code");
        qrBtn.getStyleClass().add("qr-button");
        qrBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");

        qrBtn.setOnAction(ev -> {
            showQRCodeDialog(e);
        });

        Button detailsBtn = new Button("👁️ Voir Détails");
        detailsBtn.getStyleClass().add("details-button");
        detailsBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");

        detailsBtn.setOnAction(ev -> {
            this.openDetails(e);
        });

        VBox buttonBox = new VBox(8);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(qrBtn, detailsBtn);

        card.getChildren().addAll(img, name, game, buttonBox);
        return card;
    }

    private void showQRCodeDialog(Equipe equipe) {
        try {
            Stage owner = (Stage) cardContainer.getScene().getWindow();

            Stage dialog = new Stage();
            dialog.setTitle("QR Code - " + equipe.getNom());
            dialog.initOwner(owner);
            dialog.setResizable(false);

            VBox dialogLayout = new VBox(20);
            dialogLayout.setAlignment(Pos.CENTER);
            dialogLayout.setStyle("-fx-padding: 20; -fx-background-color: white;");

            Label titleLabel = new Label("QR Code pour " + equipe.getNom());
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

            String teamUrl = TeamWebServer.getServerUrl() + "/team?id=" + equipe.getId();
            Label urlLabel = new Label("URL: " + teamUrl);
            urlLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-wrap-text: true;");

            ImageView qrImageView = new ImageView();
            qrImageView.setFitWidth(250);
            qrImageView.setFitHeight(250);
            qrImageView.setPreserveRatio(true);

            Label loadingLabel = new Label("🔄 Génération du QR code...");
            loadingLabel.setStyle("-fx-text-fill: #666;");

            dialogLayout.getChildren().addAll(titleLabel, loadingLabel, qrImageView, urlLabel);

            try {
                Image qrImage = QRCodeUtil.generateQRCodeImage(teamUrl, 250, 250);
                qrImageView.setImage(qrImage);
                dialogLayout.getChildren().remove(loadingLabel);

                HBox buttonBox = new HBox(10);
                buttonBox.setAlignment(Pos.CENTER);

                Button downloadBtn = new Button("💾 Télécharger");
                downloadBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                downloadBtn.setOnAction(e -> downloadQRCode(qrImageView.getImage(), equipe));

                Button closeBtn = new Button("✖️ Fermer");
                closeBtn.setStyle("-fx-background-color: #666; -fx-text-fill: white;");
                closeBtn.setOnAction(e -> dialog.close());

                buttonBox.getChildren().addAll(downloadBtn, closeBtn);
                dialogLayout.getChildren().add(buttonBox);
            } catch (Exception e) {
                loadingLabel.setText("❌ Erreur: " + e.getMessage());
                loadingLabel.setStyle("-fx-text-fill: red;");
            }

            Scene scene = new Scene(dialogLayout);
            dialog.setScene(scene);
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'affichage du QR code: " + e.getMessage());
        }
    }

    private void downloadQRCode(Image qrImage, Equipe equipe) {
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Enregistrer le QR code");
            fileChooser.setInitialFileName("qrcode_" + equipe.getNom().replaceAll("\\s+", "_") + ".png");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Images PNG", "*.png")
            );

            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                // Convert JavaFX Image to BufferedImage and save
                BufferedImage bufferedImage = javafx.embed.swing.SwingFXUtils.fromFXImage(qrImage, null);
                ImageIO.write(bufferedImage, "png", file);
                showInfo("Succès", "QR code enregistré: " + file.getName());
            }
        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement: " + e.getMessage());
        }
    }

    private void openDetails(Equipe e) {
        try {
            Stage stage = (Stage)this.cardContainer.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/esprit/tn/esports/EquipeDetails.fxml"));
            Parent root = loader.load();
            EquipeDetailsController ctrl = loader.getController();
            ctrl.setEquipe(e);
            ctrl.setOnBack(() -> {
                try {
                    FXMLLoader backLoader = new FXMLLoader(this.getClass().getResource("/esprit/tn/esports/equipeIndex_client.fxml"));
                    Parent backRoot = backLoader.load();
                    stage.setScene(new Scene(backRoot, 1200.0, 760.0));
                    stage.show();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            stage.setScene(new Scene(root, 1200.0, 760.0));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    void search() {
        String text = this.searchField.getText().toLowerCase();
        List<Equipe> filtered = this.allEquipes.stream()
                .filter(e -> e.getNom().toLowerCase().contains(text))
                .collect(Collectors.toList());
        this.display(filtered);
        this.updateCount(filtered.size());
    }

    private void showError(String msg) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @FXML
    private void goMatchs(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/esprit/tn/esports/matchIndex_client.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200.0, 760.0));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goStats(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/esprit/tn/esports/stats_client.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200.0, 760.0));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openQRCodeGenerator(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/qr_code_generator.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Générateur de QR Code - ClutchX");
            stage.setScene(new Scene(root, 800, 700));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir le générateur: " + e.getMessage());
        }
    }

    @FXML
    private void logout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/esprit/tn/esports/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200.0, 760.0));
            stage.setTitle("ClutchX - Connexion");
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}