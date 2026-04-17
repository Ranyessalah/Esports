package esprit.tn.esports.utils;

import esprit.tn.esports.controller.EquipeDetailsController;
import esprit.tn.esports.entite.Equipe;
import esprit.tn.esports.entite.Player;
import esprit.tn.esports.service.EquipeService;
import esprit.tn.esports.service.PlayerService;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

/**
 * A dialog for displaying QR codes and team details
 */
public class QRCodeDialog {

    private final EquipeService equipeService = new EquipeService();
    private final PlayerService playerService = new PlayerService();

    /**
     * Affiche un rapport professionnel de l'équipe au format A4 avec le QR code.
     */
    public void showShareableTeamQr(Equipe equipeRef, Stage ownerStage) {
        showShareableTeamQrWithCustomIp(equipeRef, ownerStage, null);
    }

    public void showShareableTeamQrWithCustomIp(Equipe equipeRef, Stage ownerStage, String customIp) {
        try {
            Equipe equipe = equipeService.getById(equipeRef.getId());
            if (equipe == null) equipe = equipeRef;
            List<Player> players = playerService.getPlayersByEquipe(equipe.getId());

            String localIpAddress = (customIp != null) ? customIp : esprit.tn.esports.utils.TeamWebServer.getLocalIp();
            
            // Generate QR text using the custom/detected IP
            String qrUrl = "http://" + localIpAddress + ":4567/equipe/" + equipe.getId();
            Image qrImage = QRCodeUtil.generateQRCodeImage(qrUrl, 220, 220);

            // --- THE "A4 PAPER" SHEET ---
            VBox paper = new VBox(0);
            paper.setAlignment(Pos.CENTER); // Changed to center
            paper.setMinWidth(620);
            paper.setMaxWidth(620);
            paper.setStyle("-fx-background-color: white; " +
                    "-fx-padding: 50px; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 8);");

            // 1. HEADER (Logo + Title)
            HBox header = new HBox(30);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new javafx.geometry.Insets(0, 0, 30, 0));
            header.setStyle("-fx-border-color: #e5e7eb; -fx-border-width: 0 0 2px 0;");

            ImageView logoView = new ImageView();
            logoView.setFitWidth(100);
            logoView.setFitHeight(100);
            logoView.setPreserveRatio(true);
            try {
                if (equipe.getLogo() != null && !equipe.getLogo().isBlank()) {
                    File file = new File(equipe.getLogo());
                    if (file.exists()) logoView.setImage(new Image(file.toURI().toString()));
                }
            } catch (Exception ignored) {}

            VBox titleBox = new VBox(5);
            Label docType = new Label("CLUTCHX ESPORTS - OFFICIAL REPORT");
            docType.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold; -fx-font-size: 14px; -fx-letter-spacing: 2px;");
            Label teamName = new Label(equipe.getNom().toUpperCase());
            teamName.setStyle("-fx-font-size: 38px; -fx-font-weight: 900; -fx-text-fill: #111827;");
            titleBox.getChildren().addAll(docType, teamName);
            header.getChildren().addAll(logoView, titleBox);

            // 2. METADATA GRID
            GridPane infoGrid = new GridPane();
            infoGrid.setHgap(40);
            infoGrid.setVgap(10);
            infoGrid.setPadding(new javafx.geometry.Insets(30, 0, 30, 0));

            addInfoRow(infoGrid, 0, "🎮 DISCIPLINE", equipe.getGame());
            addInfoRow(infoGrid, 1, "📂 CATÉGORIE", equipe.getCategorie());
            String coach = (equipe.getCoach() != null && equipe.getCoach().getUser() != null) 
                    ? equipe.getCoach().getUser().getEmail() : "Non assigné";
            addInfoRow(infoGrid, 2, "👨‍🏫 HEAD COACH", coach);

            // 3. ROSTER LIST
            VBox rosterBox = new VBox(15);
            rosterBox.setPadding(new javafx.geometry.Insets(20, 0, 40, 0));
            Label rosterTitle = new Label("TEAM ROSTER (" + (players != null ? players.size() : 0) + " Members)");
            rosterTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #374151; -fx-padding: 0 0 10 0;");
            rosterBox.getChildren().add(rosterTitle);

            if (players != null) {
                for (Player p : players) {
                    HBox row = new HBox(15);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-padding: 10px; -fx-border-color: #f3f4f6; -fx-border-width: 0 0 1px 0;");
                    
                    Label pName = new Label("•  " + (p.getUser() != null ? p.getUser().getEmail().replace("@", " [at] ") : "Joueur #" + p.getId()));
                    pName.setStyle("-fx-font-size: 15px; -fx-text-fill: #4b5563;");
                    
                    Label pLevel = new Label(p.getNiveau() != null ? p.getNiveau() : "Membre");
                    pLevel.setStyle("-fx-font-size: 11px; -fx-background-color: #f3f4f6; -fx-padding: 3 10; -fx-background-radius: 5; -fx-font-weight: bold;");
                    
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    row.getChildren().addAll(pName, spacer, pLevel);
                    rosterBox.getChildren().add(row);
                }
            }

            // 4. FOOTER (QR CODE)
            HBox footer = new HBox(40);
            footer.setAlignment(Pos.CENTER_LEFT);
            footer.setPadding(new javafx.geometry.Insets(30, 0, 0, 0));
            footer.setStyle("-fx-border-color: #e5e7eb; -fx-border-width: 2px 0 0 0;");

            VBox qrBox = new VBox(8);
            qrBox.setAlignment(Pos.CENTER);
            if (qrImage != null) {
                qrBox.getChildren().add(new ImageView(qrImage));
            }
            
            // URL label
            Label urlLabel = new Label(qrUrl);
            urlLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280; -fx-font-weight: bold;");
            urlLabel.setWrapText(true);
            urlLabel.setMaxWidth(230);
            
            Button copyBtn = new Button("Copier le lien");
            copyBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #f3f4f6; -fx-padding: 4 8; -fx-cursor: hand;");
            copyBtn.setOnAction(ev -> {
                final javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                final javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(qrUrl);
                clipboard.setContent(content);
                copyBtn.setText("Lien Copié !");
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override public void run() { javafx.application.Platform.runLater(() -> copyBtn.setText("Copier le lien")); }
                }, 2000);
            });

            Button changeIpBtn = new Button("Modifier IP");
            changeIpBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #f3f4f6; -fx-padding: 4 8; -fx-cursor: hand; -fx-margin-left: 5;");
            changeIpBtn.setOnAction(ev -> {
                TextInputDialog diag = new TextInputDialog(localIpAddress);
                diag.setTitle("Configuration Réseau");
                diag.setHeaderText("Saisir manuellement l'adresse IP de votre PC");
                diag.setContentText("IP (ex: 192.168.1.15) :");
                diag.showAndWait().ifPresent(newIp -> {
                    Stage currentStage = (Stage) paper.getScene().getWindow();
                    currentStage.close();
                    showShareableTeamQrWithCustomIp(equipe, ownerStage, newIp);
                });
            });

            HBox actions = new HBox(8, copyBtn, changeIpBtn);
            actions.setAlignment(Pos.CENTER);

            qrBox.getChildren().addAll(urlLabel, actions);
            qrBox.setStyle("-fx-padding: 10; -fx-border-color: #e5e7eb; -fx-border-radius: 5;");

            VBox instructions = new VBox(10);
            instructions.setAlignment(Pos.CENTER_LEFT);
            Label scanTitle = new Label("VÉRIFICATION DIGITALE");
            scanTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #111827;");
            
            Label scanHint = new Label(localIpAddress.equals("localhost") || localIpAddress.equals("127.0.0.1")
                ? "⚠️ ERREUR RÉSEAU : Votre PC n'est pas connecté au WiFi. Le QR Code ne fonctionnera pas sur mobile."
                : "Scannez ce QR Code avec votre téléphone pour ouvrir le rapport officiel.");
            scanHint.setWrapText(true);
            scanHint.setMaxWidth(280);
            scanHint.setStyle("-fx-font-size: 11px; " + (localIpAddress.contains("127.0.0.1") || localIpAddress.equals("localhost") ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;" : "-fx-text-fill: #6b7280;"));

            Label wifiNote = new Label("💡 Tip : Pour que ça marche, connectez votre PC et votre téléphone sur le MÊME WiFi (ou hotspot).");
            wifiNote.setWrapText(true);
            wifiNote.setMaxWidth(280);
            wifiNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #b45309; -fx-background-color: #fef3c7; -fx-padding: 8; -fx-background-radius: 5;");

            Button closeBtn = new Button("CLÔTURER LE RAPPORT");
            closeBtn.setStyle("-fx-background-color: #111827; -fx-text-fill: white; -fx-padding: 10 25; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 12px;");

            instructions.getChildren().addAll(scanTitle, scanHint, wifiNote, new Separator(), closeBtn);
            footer.getChildren().addAll(qrBox, instructions);

            paper.getChildren().addAll(header, infoGrid, rosterBox, footer);

            // Container for shadowing and centering the paper
            VBox overlay = new VBox(paper);
            overlay.setAlignment(Pos.CENTER);
            overlay.setStyle("-fx-background-color: #0f111a; -fx-padding: 40px;");
            
            ScrollPane scroll = new ScrollPane(overlay);
            scroll.setFitToWidth(true);
            scroll.setPrefViewportHeight(700);
            scroll.setStyle("-fx-background-color: #0f111a; -fx-background: #0f111a;");

            Stage qrStage = new Stage();
            if (ownerStage != null) {
                qrStage.initOwner(ownerStage);
                qrStage.initModality(Modality.WINDOW_MODAL);
            }
            qrStage.setTitle("Official Team Report - " + equipe.getNom());
            Scene scene = new Scene(scroll, 750, 850);
            qrStage.setScene(scene);
            closeBtn.setOnAction(ev -> qrStage.close());
            qrStage.show();
            qrStage.toFront();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible de générer le rapport : " + e.getMessage());
        }
    }

    private void addInfoRow(GridPane grid, int row, String labelText, String valueText) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        Label value = new Label(valueText != null ? valueText : "N/A");
        value.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    /**
     * Navigates the main stage to the EquipeDetails.fxml view.
     * @param equipe The team to display
     * @param stage The stage to update
     */
    public void navigateToDetails(Equipe equipe, Stage stage) {
        if (stage == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/esprit/tn/esports/EquipeDetails.fxml"));
            Parent root = loader.load();

            EquipeDetailsController controller = loader.getController();
            controller.setEquipe(equipe);

            stage.setScene(new Scene(root, 1200, 760));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir les détails : " + e.getMessage());
        }
    }

    /**
     * Old method for backward compatibility if needed, but we prefer full navigation now.
     */
    public void showOnlyDetails(Equipe equipe) {
        // Fallback to dialog if no stage is available
        try {
            List<Player> players = playerService.getPlayersByEquipe(equipe.getId());
            showTeamDetailsDialog(equipe, players);
        } catch (Exception e) {
            e.printStackTrace();
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
