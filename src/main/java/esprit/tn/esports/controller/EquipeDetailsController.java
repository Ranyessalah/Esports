package esprit.tn.esports.controller;

import esprit.tn.esports.entite.Coach;
import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Player;
import esprit.tn.esports.service.PlayerService;
 import esprit.tn.esports.utils.QRCodeUtil;
import esprit.tn.esports.utils.TeamWebServer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.shape.Circle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class EquipeDetailsController {

    @FXML private ImageView logo;
    @FXML private Label name;
    @FXML private Label game;
    @FXML private Label category;

    @FXML private Label nbPlayers;
    @FXML private Label gameStat;

    @FXML private VBox staffSection;
    @FXML private FlowPane staffContainer;
    @FXML private FlowPane playersContainer;

    private PlayerService playerService = new PlayerService();
    private Runnable onBack;
    private Equipe currentEquipe;

    // === Code de votre camarade conservé ===
    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    public void setEquipe(Equipe e) {
        this.currentEquipe = e;
        name.setText(e.getNom());
        game.setText(e.getGame());
        category.setText(e.getCategorie());

        // 🔥 Load & Modernize Hero Logo
        setCircularImage(logo, e.getLogo(), 80);

        nbPlayers.setText(String.valueOf(e.getPlayers() != null ? e.getPlayers().size() : 0));
        loadRoster(e);
    }

    // 🔥 LOAD ROSTER (Coach + Players)
    private void loadRoster(Equipe equipe) {
        // COACH
        staffContainer.getChildren().clear();
        if (equipe.getCoach() != null) {
            staffSection.setVisible(true);
            staffSection.setManaged(true);
            staffContainer.getChildren().add(createCoachCard(equipe.getCoach()));
        } else {
            staffSection.setVisible(false);
            staffSection.setManaged(false);
        }

        // PLAYERS
        playersContainer.getChildren().clear();
        List<Player> players = playerService.getPlayersByEquipe(equipe.getId());
        for (Player p : players) {
            playersContainer.getChildren().add(createPlayerCard(p));
        }
        nbPlayers.setText(String.valueOf(players.size()));
    }

    // 🔥 HELPER: SET CIRCULAR IMAGE
    private void setCircularImage(ImageView iv, String path, double radius) {
        iv.setFitWidth(radius * 2);
        iv.setFitHeight(radius * 2);
        Circle clip = new Circle(radius, radius, radius);
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

        // Fallback or dynamic placeholder logic could go here
    }

    // 🔥 COACH CARD
    private VBox createCoachCard(Coach c) {
        VBox card = new VBox(12);
        card.getStyleClass().add("player-card-modern");
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(200);

        ImageView img = new ImageView();
        String imgPath = (c.getUser() != null) ? c.getUser().getProfileImage() : null;
        setCircularImage(img, imgPath, 35);

        Label nameLabel = new Label(c.getUser() != null ? c.getUser().getEmail() : "Coach");
        nameLabel.getStyleClass().add("card-title");

        Label spec = new Label(c.getSpecialite() != null ? c.getSpecialite() : "Coach Principal");
        spec.getStyleClass().add("card-sub");

        Label badge = new Label("COACH");
        badge.getStyleClass().addAll("badge");
        badge.setStyle("-fx-background-color: #ef4444;");

        card.getChildren().addAll(img, nameLabel, spec, badge);
        return card;
    }

    // 🔥 PLAYER CARD
    private VBox createPlayerCard(Player p) {
        VBox card = new VBox(12);
        card.getStyleClass().add("player-card-modern");
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(200);

        ImageView img = new ImageView();
        String imgPath = (p.getUser() != null) ? p.getUser().getProfileImage() : null;
        setCircularImage(img, imgPath, 35);

        String displayName = (p.getUser() != null && p.getUser().getEmail() != null)
                ? p.getUser().getEmail()
                : "Player #" + p.getId();

        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("card-title");

        Label pays = new Label(p.getPays() != null ? p.getPays() : "Inconnu");
        pays.getStyleClass().add("card-sub");

        Label niveau = new Label(p.getNiveau() != null ? p.getNiveau() : "Debutant");
        niveau.getStyleClass().add("badge");

        card.getChildren().addAll(img, nameLabel, pays, niveau);
        return card;
    }

    // === MÉTHODE showQR ORIGINALE conservée ===
    @FXML
    private void showQR() {
        if (currentEquipe != null) {
            Stage owner = (Stage) name.getScene().getWindow();
            // Utiliser la nouvelle version améliorée au lieu de l'ancienne
            showModernQRCodeDialog(currentEquipe, owner);
        }
    }

    // === NOUVELLE MÉTHODE : QR Code moderne avec TeamWebServer ===
    private void showModernQRCodeDialog(Equipe equipe, Stage owner) {
        try {
            // Vérifier si le serveur est accessible
            String serverUrl = TeamWebServer.getServerUrl();
            if (serverUrl == null || serverUrl.isEmpty()) {
                showError("Serveur web non disponible. Vérifiez que l'application a démarré correctement.");
                return;
            }

            // Créer le dialogue
            Stage dialog = new Stage();
            dialog.setTitle("QR Code - " + equipe.getNom());
            dialog.initOwner(owner);
            dialog.setResizable(false);

            // Layout principal
            VBox dialogLayout = new VBox(15);
            dialogLayout.setAlignment(Pos.CENTER);
            dialogLayout.setStyle("-fx-padding: 25; -fx-background-color: white; -fx-border-radius: 10;");

            // Titre
            Label titleLabel = new Label("📱 " + equipe.getNom());
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

            // Sous-titre
            Label subtitleLabel = new Label("Scannez ce QR code avec votre smartphone");
            subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

            // URL
            String teamUrl = serverUrl + "/team?id=" + equipe.getId();
            Label urlLabel = new Label(teamUrl);
            urlLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999; -fx-wrap-text: true; -fx-text-alignment: center;");

            // ImageView pour le QR code
            ImageView qrImageView = new ImageView();
            qrImageView.setFitWidth(250);
            qrImageView.setFitHeight(250);
            qrImageView.setPreserveRatio(true);

            // Indicateur de chargement
            Label loadingLabel = new Label("🔄 Génération du QR code...");
            loadingLabel.setStyle("-fx-text-fill: #666;");

            dialogLayout.getChildren().addAll(titleLabel, subtitleLabel, loadingLabel, qrImageView, urlLabel);

            try {
                Image qrImage = QRCodeUtil.generateQRCodeImage(teamUrl, 250, 250);
                qrImageView.setImage(qrImage);
                dialogLayout.getChildren().remove(loadingLabel);

                // Ajouter les boutons d'action
                HBox buttonBox = new HBox(10);
                buttonBox.setAlignment(Pos.CENTER);

                Button downloadBtn = new Button("💾 Télécharger");
                downloadBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;");
                downloadBtn.setOnAction(e -> downloadQRCode(qrImageView.getImage(), equipe));

                Button copyBtn = new Button("📋 Copier URL");
                copyBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;");
                copyBtn.setOnAction(e -> copyUrlToClipboard(teamUrl));

                Button closeBtn = new Button("✖️ Fermer");
                closeBtn.setStyle("-fx-background-color: #666; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 5;");
                closeBtn.setOnAction(e -> dialog.close());

                buttonBox.getChildren().addAll(downloadBtn, copyBtn, closeBtn);
                dialogLayout.getChildren().add(buttonBox);
            } catch (Exception e) {
                loadingLabel.setText("❌ Erreur: " + e.getMessage());
                loadingLabel.setStyle("-fx-text-fill: red;");

                // Ajouter un bouton pour réessayer
                Button retryBtn = new Button("🔄 Réessayer");
                retryBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
                retryBtn.setOnAction(ev -> {
                    dialogLayout.getChildren().remove(retryBtn);
                    loadingLabel.setText("🔄 Génération du QR code...");
                    loadingLabel.setStyle("-fx-text-fill: #666;");
                    dialog.close();
                    showModernQRCodeDialog(equipe, owner);
                });
                dialogLayout.getChildren().add(retryBtn);
            }

            Scene scene = new Scene(dialogLayout);
            dialog.setScene(scene);
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'affichage du QR code: " + e.getMessage());
        }
    }

    // === CORRECTED: Télécharger le QR code ===
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
                // Convert JavaFX Image to BufferedImage
                BufferedImage bufferedImage = javafx.embed.swing.SwingFXUtils.fromFXImage(qrImage, null);
                // Write to file using ImageIO
                ImageIO.write(bufferedImage, "png", file);
                showInfo("Succès", "QR code enregistré: " + file.getName());
            }
        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement: " + e.getMessage());
        }
    }

    // === NOUVELLE MÉTHODE : Copier l'URL dans le presse-papier ===
    private void copyUrlToClipboard(String url) {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(url);
        clipboard.setContent(content);
        showInfo("Succès", "URL copiée dans le presse-papier!");
    }

    // === NOUVELLE MÉTHODE : Afficher les infos du serveur ===
    @FXML
    private void showServerInfo() {
        String serverUrl = TeamWebServer.getServerUrl();
        String localIp = TeamWebServer.getLocalIp();

        String info = "📡 Informations du serveur:\n\n" +
                "🌐 URL: " + serverUrl + "\n" +
                "📱 IP Locale: " + localIp + "\n" +
                "🔌 Port: 8081\n\n" +
                "💡 Utilisez cette URL pour accéder aux QR codes\n" +
                "⚠️ Votre smartphone doit être sur le même réseau WiFi";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informations du serveur");
        alert.setHeaderText("Serveur Web - ClutchX");
        alert.setContentText(info);
        alert.showAndWait();
    }

    // === MÉTHODES UTILITAIRES ===
    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // === MÉTHODE goBack ORIGINALE conservée ===
    @FXML
    private void goBack() {
        if (onBack != null) {
            onBack.run();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/equipeIndex_client.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) name.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 760));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}