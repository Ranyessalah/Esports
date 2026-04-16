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

/**
 * A dialog for displaying QR codes and team details
 */
public class QRCodeDialog {

    private final EquipeService equipeService = new EquipeService();
    private final PlayerService playerService = new PlayerService();

    /**
     * PC : uniquement le QR. La fiche détaillée n’apparaît pas ici : elle est dans le texte encodé,
     * lisible après scan (téléphone ou site « scanner QR en ligne »).
     */
    public void showShareableTeamQr(Equipe equipeRef) {
        try {
            Equipe equipe = equipeService.getById(equipeRef.getId());
            if (equipe == null) {
                equipe = equipeRef;
            }
            List<Player> players = playerService.getPlayersByEquipe(equipe.getId());

            String qrText = QRCodeUtil.createRichTeamQRString(equipe, players);
            Image qrImage = QRCodeUtil.generateQRCodeImage(qrText, 280, 280);

            ImageView qrImageView = new ImageView(qrImage);
            qrImageView.setStyle("-fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 10;");

            Label title = new Label("Code QR — " + equipe.getNom());
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

            Label hint = new Label(
                    "Ne pas chercher la fiche sur cet écran : scannez ce QR avec la caméra du téléphone "
                            + "ou un site de scan QR en ligne. Le résultat du scan est le texte de la fiche équipe "
                            "(nom, jeu, catégorie, joueurs, coach), identique à l’affichage détail dans l’application."
            );
            hint.setWrapText(true);
            hint.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 13px;");
            hint.setMaxWidth(480);

            Button closeBtn = new Button("Fermer");
            closeBtn.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

            VBox box = new VBox(14);
            box.setAlignment(Pos.TOP_CENTER);
            box.setStyle("-fx-background-color: #191c24; -fx-padding: 20px; -fx-border-color: #2e3b4e; -fx-border-radius: 10px; -fx-background-radius: 10px;");
            box.getChildren().addAll(title, hint, qrImageView, closeBtn);

            ScrollPane scroll = new ScrollPane(box);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background: #0f111a;");

            Stage qrStage = new Stage();
            qrStage.setTitle("Code QR — " + equipe.getNom());
            Scene scene = new Scene(scroll, 420, 520);
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

    private VBox buildTeamDetailsVBox(Equipe equipe, List<Player> players) {
        VBox detailsBox = new VBox(15);
        detailsBox.setAlignment(Pos.TOP_CENTER);
        detailsBox.setStyle("-fx-background-color: #191c24; -fx-padding: 30px; -fx-border-color: #2e3b4e; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-min-width: 450px;");

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

        Separator separator = new Separator();
        separator.setStyle("-fx-padding: 10px 0;");
        detailsBox.getChildren().add(separator);

        Label playersTitleLabel = new Label("👥 Joueurs (" + (players != null ? players.size() : 0) + ")");
        playersTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 10px 0 5px 0;");
        detailsBox.getChildren().add(playersTitleLabel);

        if (players == null || players.isEmpty()) {
            Label noPlayerLabel = new Label("Aucun joueur inscrit");
            noPlayerLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #ef4444; -fx-padding: 10px;");
            detailsBox.getChildren().add(noPlayerLabel);
        } else {
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
        return detailsBox;
    }

    /**
     * Show team details dialog
     * @param equipe The team
     * @param players List of players
     */
    public void showTeamDetailsDialog(Equipe equipe, List<Player> players) {
        VBox detailsBox = buildTeamDetailsVBox(equipe, players);

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
