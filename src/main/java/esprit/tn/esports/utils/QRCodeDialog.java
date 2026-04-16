package esprit.tn.esports.utils;

import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Player;
import esprit.tn.esports.service.EquipeService;
import esprit.tn.esports.service.PlayerService;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

/**
 * A dialog for displaying QR codes and team details
 */
public class QRCodeDialog {

    private final EquipeService equipeService = new EquipeService();
    private final PlayerService playerService = new PlayerService();

    /**
     * Affiche le QR d'une équipe (écran partagé, second appareil ou impression).
     * L’accès à la fiche détaillée depuis la liste exige de scanner ce même QR.
     */
    public void showShareableTeamQr(Equipe equipeRef) {
        try {
            Equipe equipe = equipeService.getById(equipeRef.getId());
            if (equipe == null) {
                equipe = equipeRef;
            }
            List<Player> players = playerService.getPlayersByEquipe(equipe.getId());
            String qrText = QRCodeUtil.createRichTeamQRString(equipe, players);
            Image qrImage = QRCodeUtil.generateQRCodeImage(qrText, 320, 320);

            ImageView qrImageView = new ImageView(qrImage);
            qrImageView.setStyle("-fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 10;");

            Label title = new Label("Code QR — " + equipe.getNom());
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

            Label hint = new Label("Scannez ce QR avec le téléphone ou un site « scanner QR en ligne », copiez le texte affiché, puis dans l’app : « Voir détails » → collez le texte. La webcam PC est optionnelle.");
            hint.setWrapText(true);
            hint.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");
            hint.setMaxWidth(360);

            VBox box = new VBox(16);
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-background-color: #191c24; -fx-padding: 24px; -fx-border-color: #2e3b4e; -fx-border-radius: 10px; -fx-background-radius: 10px;");
            box.getChildren().addAll(title, qrImageView, hint);

            Button closeBtn = new Button("Fermer");
            closeBtn.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            box.getChildren().add(closeBtn);

            // Fenêtre non modale : vous pouvez garder le QR affiché et cliquer « Voir détails » sur la liste
            Stage qrStage = new Stage();
            qrStage.setTitle("QR — " + equipe.getNom());
            Scene scene = new Scene(box);
            scene.setFill(javafx.scene.paint.Color.web("#0f111a"));
            qrStage.setScene(scene);
            closeBtn.setOnAction(ev -> qrStage.close());
            qrStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de la génération du code QR.");
        }
    }

    /**
     * Show a QR code dialog for a team
     * @param teamId The team ID
     * @param teamName The team name
     * @param onSuccess Callback when QR code is scanned
     */
    public void showQRCodeDialog(int teamId, String teamName, Consumer<Equipe> onSuccess) {
        try {
            Equipe fullEquipe = equipeService.getById(teamId);
            if (fullEquipe == null) {
                showError("Équipe non trouvée");
                return;
            }

            List<Player> players = playerService.getPlayersByEquipe(teamId);

            // Generate QR Code with a rich text summary for mobile scanners
            String qrText = QRCodeUtil.createRichTeamQRString(fullEquipe, players);
            Image qrImage = QRCodeUtil.generateQRCodeImage(qrText, 350, 350);

            ImageView qrImageView = new ImageView(qrImage);
            qrImageView.setCursor(javafx.scene.Cursor.HAND);
            qrImageView.setStyle("-fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 10;");

            VBox mainVBox = new VBox(20);
            mainVBox.setAlignment(Pos.CENTER);
            mainVBox.setStyle("-fx-background-color: #191c24; -fx-padding: 30px; -fx-border-color: #2e3b4e; -fx-border-radius: 10px; -fx-background-radius: 10px;");

            Label qrTitleLabel = new Label("📱 Scannez le code QR de l'équipe");
            qrTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

            Label instructionLabel = new Label("Utilisez la webcam ou CLIQUEZ sur le code pour voir les détails");
            instructionLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 14px; -fx-padding: 10px 0;");
            instructionLabel.setWrapText(true);

            mainVBox.getChildren().addAll(qrTitleLabel, qrImageView, instructionLabel);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("QR Code - " + teamName);
            dialog.getDialogPane().setContent(mainVBox);
            dialog.getDialogPane().setStyle("-fx-background-color: #0f111a;");

            ButtonType scanButtonType = new ButtonType("✓ Confirmer", ButtonBar.ButtonData.OK_DONE);
            ButtonType webcamButtonType = new ButtonType("🎥 Webcam", ButtonBar.ButtonData.OTHER);
            ButtonType closeButtonType = new ButtonType("✕ Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(scanButtonType, webcamButtonType, closeButtonType);

            // Style buttons
            javafx.scene.Node scanButton = dialog.getDialogPane().lookupButton(scanButtonType);
            javafx.scene.Node webcamButton = dialog.getDialogPane().lookupButton(webcamButtonType);
            javafx.scene.Node closeButton = dialog.getDialogPane().lookupButton(closeButtonType);

            if (scanButton != null) {
                scanButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                scanButton.setOnMouseClicked(event -> {
                    dialog.close();
                    if (onSuccess != null) {
                        onSuccess.accept(fullEquipe);
                    }
                });
            }

            if (webcamButton != null) {
                webcamButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                webcamButton.setOnMouseClicked(event -> {
                    dialog.close();
                    // Open the scanner directly
                    new QRScannerDialog().startScanner(code -> {
                        // After scan, manually trigger success logic
                        if (onSuccess != null) {
                            onSuccess.accept(fullEquipe);
                        }
                    });
                });
            }

            if (closeButton != null) {
                closeButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            }

            // Support click on the QR image to immediately see details (Backup/Simulated Scan)
            qrImageView.setStyle("-fx-cursor: hand;");
            qrImageView.setOnMouseClicked(event -> {
                dialog.close();
                if (onSuccess != null) {
                    onSuccess.accept(fullEquipe);
                }
            });

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de la génération du QR code");
        }
    }

    /**
     * Public method to show only the team details dialog (used by webcam scanner)
     * @param equipe The team to show
     */
    public void showOnlyDetails(Equipe equipe) {
        try {
            List<Player> players = playerService.getPlayersByEquipe(equipe.getId());
            showTeamDetailsDialog(equipe, players);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de la récupération des détails.");
        }
    }

    /**
     * Minimalist dialog for scan results (Players Only)
     */
    public void showPlayersOnlyDialog(Equipe equipe, List<Player> players) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0f172a; -fx-padding: 30px; -fx-border-color: #3b82f6; -fx-border-width: 2px; -fx-border-radius: 15px; -fx-background-radius: 15px;");
        root.setMinWidth(400);

        Label title = new Label("🔥 ROSTER : " + equipe.getNom().toUpperCase());
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        VBox playersBox = new VBox(10);
        playersBox.setAlignment(Pos.CENTER);
        
        if (players.isEmpty()) {
            Label noPlayer = new Label("Aucun joueur inscrit");
            noPlayer.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 16px;");
            playersBox.getChildren().add(noPlayer);
        } else {
            for (Player p : players) {
                String mail = (p.getUser() != null) ? p.getUser().getEmail() : "vv@gmail.com";
                Label pLabel = new Label("• " + mail);
                pLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 18px; -fx-font-weight: bold;");
                playersBox.getChildren().add(pLabel);
            }
        }

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 20; -fx-background-radius: 5px;");
        
        root.getChildren().addAll(title, playersBox, closeBtn);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(root));
        stage.setTitle("Membres de l'équipe");
        
        closeBtn.setOnAction(e -> stage.close());
        stage.show();
    }

    /**
     * Show team details dialog
     * @param equipe The team
     * @param players List of players
     */
    public void showTeamDetailsDialog(Equipe equipe, List<Player> players) {
        VBox detailsBox = new VBox(15);
        detailsBox.setAlignment(Pos.TOP_CENTER);
        detailsBox.setStyle("-fx-background-color: #191c24; -fx-padding: 30px; -fx-border-color: #2e3b4e; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-min-width: 450px;");

        // Team Information
        Label titleLabel = new Label("🏆 " + equipe.getNom());
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #3b82f6;");

        Label gameLabel = new Label("Jeu : " + equipe.getGame());
        gameLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #9ca3af;");

        Label categoryLabel = new Label("Catégorie : " + equipe.getCategorie());
        categoryLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #9ca3af;");

        String coachName = (equipe.getCoach() != null && equipe.getCoach().getUser() != null)
                ? equipe.getCoach().getUser().getEmail() : "Aucun";
        Label coachLabel = new Label("👨‍🏫 Coach : " + coachName);
        coachLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #f59e0b; -fx-font-weight: bold;");

        detailsBox.getChildren().addAll(titleLabel, gameLabel, categoryLabel, coachLabel);

        // Separator
        Separator separator = new Separator();
        separator.setStyle("-fx-padding: 10px 0;");
        detailsBox.getChildren().add(separator);

        // Players Section
        Label playersTitleLabel = new Label("👥 Joueurs (" + (players != null ? players.size() : 0) + ")");
        playersTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 10px 0 5px 0;");
        detailsBox.getChildren().add(playersTitleLabel);

        if (players == null || players.isEmpty()) {
            Label noPlayerLabel = new Label("Aucun joueur inscrit");
            noPlayerLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #ef4444; -fx-padding: 10px;");
            detailsBox.getChildren().add(noPlayerLabel);
        } else {
            // Create scrollable player list
            VBox playersList = new VBox(8);
            playersList.setStyle("-fx-padding: 10px; -fx-background-color: #0f111a; -fx-border-radius: 8; -fx-border-color: #2e3b4e;");

            for (Player p : players) {
                HBox playerBox = createPlayerRow(p);
                playersList.getChildren().add(playerBox);
            }

            ScrollPane scrollPane = new ScrollPane(playersList);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
            scrollPane.setPrefHeight(200);
            scrollPane.setFitToWidth(true);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            detailsBox.getChildren().add(scrollPane);
        }

        // Dialog
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails de l'équipe - " + equipe.getNom());
        dialog.getDialogPane().setContent(detailsBox);
        dialog.getDialogPane().setStyle("-fx-background-color: #0f111a;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        javafx.scene.Node closeButton = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (closeButton != null) {
            closeButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        }

        dialog.showAndWait();
    }

    /**
     * Create a player row in the details dialog
     */
    private HBox createPlayerRow(Player player) {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-padding: 10px; -fx-background-color: #191c24; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label numberLabel = new Label("•");
        numberLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #3b82f6; -fx-font-weight: bold;");

        String displayName = (player.getUser() != null && player.getUser().getEmail() != null)
                ? player.getUser().getEmail()
                : "Joueur #" + player.getId();
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #e5e7eb;");

        Label niveauLabel = new Label(player.getNiveau() != null ? player.getNiveau() : "Membre");
        niveauLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #10b981; -fx-font-weight: bold; -fx-padding: 4 8 4 8; -fx-background-color: #0f111a; -fx-border-radius: 4;");

        Label paysLabel = new Label(player.getPays() != null ? player.getPays() : "");
        paysLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        box.getChildren().addAll(numberLabel, nameLabel, spacer, paysLabel, niveauLabel);
        return box;
    }

    /**
     * Show an error dialog
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color: #0f111a; -fx-text-fill: white;");
        alert.showAndWait();
    }
}
