package esprit.tn.esports;

import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.service.EquipeService;
import esprit.tn.esports.utils.TeamWebServer;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class HelloController {

    // === Code existant de votre camarade ===
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    // === NOUVELLES FONCTIONNALITÉS AJOUTÉES ===

    // Composants QR Code
    @FXML
    private ComboBox<Equipe> equipeComboBox;

    @FXML
    private ImageView qrCodeImageView;

    @FXML
    private TextField urlTextField;

    @FXML
    private Label serverUrlLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private Button generateQRBtn;

    @FXML
    private Button downloadQRBtn;

    @FXML
    private Button copyUrlBtn;

    @FXML
    private Button openBrowserBtn;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Tab qrCodeTab;

    // Services
    private EquipeService equipeService;
    private Equipe currentSelectedEquipe;

    // === INITIALISATION ===
    @FXML
    public void initialize() {
        // Initialiser les services
        equipeService = new EquipeService();

        // Configurer l'interface QR Code si les composants existent
        if (equipeComboBox != null) {
            setupQRCodeGenerator();
        }

        // Afficher les informations du serveur
        displayServerInfo();

        // Démarrer un thread pour vérifier le serveur
        checkServerStatus();
    }

    // === CONFIGURATION DU GÉNÉRATEUR QR CODE ===
    private void setupQRCodeGenerator() {
        loadingIndicator.setVisible(false);

        // Désactiver les boutons au début
        if (downloadQRBtn != null) downloadQRBtn.setDisable(true);
        if (copyUrlBtn != null) copyUrlBtn.setDisable(true);
        if (openBrowserBtn != null) openBrowserBtn.setDisable(true);

        // Charger les équipes
        loadEquipesIntoComboBox();

        // Ajouter un listener pour la sélection d'équipe
        equipeComboBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        currentSelectedEquipe = newVal;
                        updateUrlField(newVal);
                        // Réinitialiser l'affichage du QR code
                        if (qrCodeImageView != null) {
                            qrCodeImageView.setImage(null);
                        }
                        if (downloadQRBtn != null) downloadQRBtn.setDisable(true);
                        if (generateQRBtn != null) generateQRBtn.setDisable(false);
                    }
                }
        );
    }

    private void loadEquipesIntoComboBox() {
        try {
            List<Equipe> equipes = equipeService.getAll();
            if (equipes != null && !equipes.isEmpty()) {
                equipeComboBox.getItems().addAll(equipes);

                // Personnaliser l'affichage
                equipeComboBox.setCellFactory(param -> new ListCell<Equipe>() {
                    @Override
                    protected void updateItem(Equipe item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getNom() + " - " + item.getGame());
                        }
                    }
                });

                equipeComboBox.setButtonCell(new ListCell<Equipe>() {
                    @Override
                    protected void updateItem(Equipe item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText("Sélectionner une équipe");
                        } else {
                            setText(item.getNom() + " - " + item.getGame());
                        }
                    }
                });

                statusLabel.setText("✅ " + equipes.size() + " équipes chargées");
            } else {
                statusLabel.setText("⚠️ Aucune équipe trouvée");
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Erreur: " + e.getMessage());
            showAlert("Erreur", "Impossible de charger les équipes: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateUrlField(Equipe equipe) {
        if (urlTextField != null) {
            String url = TeamWebServer.getServerUrl() + "/team?id=" + equipe.getId();
            urlTextField.setText(url);
        }
    }

    private void displayServerInfo() {
        if (serverUrlLabel != null) {
            String serverUrl = TeamWebServer.getServerUrl();
            String localIp = TeamWebServer.getLocalIp();
            serverUrlLabel.setText("🌐 Serveur: " + serverUrl + " | IP: " + localIp);
        }
    }

    private void checkServerStatus() {
        new Thread(() -> {
            try {
                URL url = new URL(TeamWebServer.getServerUrl());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000);
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();

                Platform.runLater(() -> {
                    if (statusLabel != null) {
                        if (responseCode == 200) {
                            statusLabel.setText("✅ Serveur web actif sur " + TeamWebServer.getServerUrl());
                        } else {
                            statusLabel.setText("⚠️ Serveur répond mais avec code: " + responseCode);
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (statusLabel != null) {
                        statusLabel.setText("⚠️ Vérification du serveur: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    // === ACTIONS POUR QR CODE ===
    @FXML
    private void generateQRCode() {
        if (currentSelectedEquipe == null) {
            showAlert("Erreur", "Veuillez sélectionner une équipe", Alert.AlertType.WARNING);
            return;
        }

        // Afficher le chargement
        loadingIndicator.setVisible(true);
        if (generateQRBtn != null) generateQRBtn.setDisable(true);
        if (statusLabel != null) statusLabel.setText("🔄 Génération du QR code...");

        // Exécuter dans un thread séparé
        new Thread(() -> {
            try {
                String qrUrl = TeamWebServer.getServerUrl() + "/qrcode/image?id=" + currentSelectedEquipe.getId();
                Image qrImage = downloadImage(qrUrl);

                Platform.runLater(() -> {
                    qrCodeImageView.setImage(qrImage);
                    loadingIndicator.setVisible(false);
                    if (generateQRBtn != null) generateQRBtn.setDisable(false);
                    if (downloadQRBtn != null) downloadQRBtn.setDisable(false);
                    if (copyUrlBtn != null) copyUrlBtn.setDisable(false);
                    if (openBrowserBtn != null) openBrowserBtn.setDisable(false);
                    if (statusLabel != null) statusLabel.setText("✅ QR code généré avec succès!");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    if (generateQRBtn != null) generateQRBtn.setDisable(false);
                    if (statusLabel != null) statusLabel.setText("❌ Erreur: " + e.getMessage());
                    showAlert("Erreur", "Impossible de générer le QR code: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    @FXML
    private void downloadQRCode() {
        if (qrCodeImageView.getImage() == null) {
            showAlert("Erreur", "Aucun QR code à télécharger. Veuillez d'abord en générer un.", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le QR code");
        fileChooser.setInitialFileName("qrcode_equipe_" + currentSelectedEquipe.getId() + "_" +
                currentSelectedEquipe.getNom().replaceAll("\\s+", "_") + ".png");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images PNG", "*.png")
        );

        File file = fileChooser.showSaveDialog(qrCodeImageView.getScene().getWindow());

        if (file != null) {
            try {
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(qrCodeImageView.getImage(), null);
                ImageIO.write(bufferedImage, "png", file);
                showAlert("Succès", "QR code enregistré: " + file.getName(), Alert.AlertType.INFORMATION);
                statusLabel.setText("✅ QR code sauvegardé: " + file.getName());
            } catch (IOException e) {
                showAlert("Erreur", "Impossible d'enregistrer: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void copyUrlToClipboard() {
        if (urlTextField != null && urlTextField.getText() != null && !urlTextField.getText().isEmpty()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(urlTextField.getText());
            clipboard.setContent(content);
            showAlert("Succès", "URL copiée dans le presse-papier!", Alert.AlertType.INFORMATION);
            statusLabel.setText("📋 URL copiée dans le presse-papier");
        }
    }

    @FXML
    private void openInBrowser() {
        if (urlTextField != null && urlTextField.getText() != null && !urlTextField.getText().isEmpty()) {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(urlTextField.getText()));
                statusLabel.setText("🌐 Ouverture dans le navigateur...");
            } catch (Exception e) {
                showAlert("Erreur", "Impossible d'ouvrir le navigateur: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    // === MÉTHODES UTILITAIRES ===
    private Image downloadImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.connect();

        if (connection.getResponseCode() != 200) {
            throw new IOException("Erreur HTTP: " + connection.getResponseCode());
        }

        BufferedImage bufferedImage = ImageIO.read(connection.getInputStream());
        if (bufferedImage == null) {
            throw new IOException("Impossible de lire l'image");
        }

        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // === MÉTHODES POUR OUVRIR LES FENÊTRES ===
    @FXML
    private void openQRCodeGeneratorWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/qr_code_generator.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("Générateur de QR Code - ClutchX Esports");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
            statusLabel.setText("📱 Fenêtre QR Code ouverte");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le générateur de QR code: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void refreshEquipes() {
        equipeComboBox.getItems().clear();
        loadEquipesIntoComboBox();
        statusLabel.setText("🔄 Liste des équipes actualisée");
    }

    @FXML
    private void showServerInfo() {
        String info = "📡 Informations du serveur:\n\n" +
                "🌐 URL: " + TeamWebServer.getServerUrl() + "\n" +
                "📱 IP Locale: " + TeamWebServer.getLocalIp() + "\n" +
                "🔌 Port: 8081\n" +
                "📋 Statut: Actif\n\n" +
                "💡 Utilisez cette URL pour générer des QR codes accessibles depuis votre smartphone\n" +
                "⚠️ Assurez-vous que votre téléphone est connecté au même réseau WiFi";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informations du serveur");
        alert.setHeaderText("Serveur Web - ClutchX Esports");
        alert.setContentText(info);
        alert.showAndWait();
    }

    // === CODE EXISTANT CONSERVÉ ===
    @FXML
    private void onExitButtonClick() {
        Platform.exit();
    }

    @FXML
    private void onAboutButtonClick() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("À propos");
        alert.setHeaderText("ClutchX Esports Management System");
        alert.setContentText("Version: 1.0\n© 2024 ClutchX Esports\n\nFonctionnalités:\n- Gestion des équipes\n- Génération de QR codes\n- Accès mobile aux informations");
        alert.showAndWait();
    }
}