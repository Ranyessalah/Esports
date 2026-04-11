package controllers;

import entities.Reward;
import entities.Roles;
import entities.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import services.RewardService;
import services.UserService;

import java.sql.SQLException;

public class ProfileDialog {

    public interface OnSaveCallback {
        void onSaved(User updatedUser);
    }

    private final UserService userService = new UserService();

    /**
     * Ouvre le dialogue.
     *
     * @param owner        fenêtre parente
     * @param user         utilisateur à modifier
     * @param isOwnProfile true  → profil admin (email + mdp seulement, pas de rôle)
     *                     false → édition par admin (email + rôle)
     * @param callback     appelé après sauvegarde réussie
     */
    public void show(Stage owner, User user, boolean isOwnProfile, OnSaveCallback callback) {

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setResizable(false);

        // ── Root ──
        VBox root = new VBox(0);
        root.setStyle(
                "-fx-background-color: #1a1a32;" +
                        "-fx-border-color: rgba(124,58,237,0.5);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 14;" +
                        "-fx-background-radius: 14;"
        );
        root.setPrefWidth(440);

        // ── Header ──
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);
        header.setPadding(new Insets(18, 20, 18, 20));
        header.setStyle(
                "-fx-background-color: rgba(124,58,237,0.12);" +
                        "-fx-border-color: rgba(124,58,237,0.25);" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-background-radius: 14 14 0 0;"
        );

        String initials = user.getEmail().substring(0, Math.min(2, user.getEmail().length())).toUpperCase();
        Label avatar = new Label(initials);
        avatar.setStyle(
                "-fx-background-color: #7c3aed;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 42; -fx-max-width: 42;" +
                        "-fx-min-height: 42; -fx-max-height: 42;" +
                        "-fx-alignment: CENTER;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;"
        );

        VBox titleBox = new VBox(2);
        Label titleLbl = new Label(isOwnProfile ? "Mon profil" : "Modifier l'utilisateur");
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label subtitleLbl = new Label(user.getEmail());
        subtitleLbl.setStyle("-fx-text-fill: #9090b8; -fx-font-size: 12px;");
        titleBox.getChildren().addAll(titleLbl, subtitleLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-text-fill: #6060908;" +
                        "-fx-text-fill: #606090;" +
                        "-fx-font-size: 14px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0 4;"
        );
        closeBtn.setOnAction(e -> dialog.close());

        header.getChildren().addAll(avatar, titleBox, spacer, closeBtn);

        // ── Form ──
        VBox form = new VBox(14);
        form.setPadding(new Insets(24, 24, 20, 24));

        // Email
        TextField emailField = styledField(user.getEmail(), false);
        form.getChildren().addAll(fieldGroup("Email", emailField));

        // Nouveau mot de passe (toujours affiché)
        PasswordField passwordField = styledPasswordField("Laisser vide pour ne pas changer");
        PasswordField confirmField = styledPasswordField("Confirmer le nouveau mot de passe");
        form.getChildren().addAll(
                fieldGroup("Nouveau mot de passe", passwordField),
                fieldGroup("Confirmer le mot de passe", confirmField)
        );

        // Rôle — uniquement si édition admin sur autre utilisateur
        ComboBox<String> roleCombo = null;
        if (!isOwnProfile) {
            roleCombo = styledCombo();
            roleCombo.getItems().addAll("Admin", "Coach", "Joueur");
            roleCombo.setValue(roleFr(user.getRole()));
            form.getChildren().add(fieldGroup("Rôle", roleCombo));
        }
        RewardService rewardService = new RewardService();
        TextField gemsField;
        if (user.getRole() == Roles.ROLE_PLAYER) {
            gemsField = styledField("", false); // empty field
            form.getChildren().add(fieldGroup("Add Gems", gemsField));
        } else {
            gemsField = null;
        }
        // Message d'erreur / succès
        Label messageLbl = new Label();
        messageLbl.setStyle("-fx-font-size: 12px;");
        messageLbl.setWrapText(true);

        // ── Buttons ──
        HBox btnRow = new HBox(10);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(0, 24, 22, 24));

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: rgba(255,255,255,0.2);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: #9090b8;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 8 18;" +
                        "-fx-cursor: hand;"
        );
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("💾  Enregistrer");
        saveBtn.setStyle(
                "-fx-background-color: #7c3aed;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 20;" +
                        "-fx-cursor: hand;"
        );

        final ComboBox<String> finalRoleCombo = roleCombo;

        saveBtn.setOnAction(e -> {
            String newEmail = emailField.getText().trim();
            String newPassword = passwordField.getText();
            String confirmPass = confirmField.getText();

            int gemsValue = 0;

            if (gemsField != null) {
                String gemsText = gemsField.getText().trim();

                if (!gemsText.isEmpty()) {
                    try {
                        gemsValue = Integer.parseInt(gemsText);
                        if (gemsValue < 0) {
                            showMsg(messageLbl, "❌ Gems doit être >= 0.", true);
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        showMsg(messageLbl, "❌ Gems doit être un nombre valide.", true);
                        return;
                    }
                }
            }
            // ── Validations ──
            if (newEmail.isEmpty() || !newEmail.contains("@")) {
                showMsg(messageLbl, "❌  Adresse email invalide.", true);
                return;
            }

            if (!newPassword.isEmpty()) {
                if (newPassword.length() < 6) {
                    showMsg(messageLbl, "❌  Le mot de passe doit contenir au moins 6 caractères.", true);
                    return;
                }
                if (!newPassword.equals(confirmPass)) {
                    showMsg(messageLbl, "❌  Les mots de passe ne correspondent pas.", true);
                    return;
                }
                user.setPassword(newPassword); // sera haché dans UserService si tu adaptes updateOne
            }

            user.setEmail(newEmail);

            if (!isOwnProfile && finalRoleCombo != null) {
                String selectedRole = finalRoleCombo.getValue();
                user.setRole(roleFromFr(selectedRole));
                user.setType(typeFromRole(user.getRole()));
            }

            try {
                userService.updateOne(user);
                userService.updatePassword(user, user.getPassword());
                if (gemsValue > 0) {
                    rewardService.addGems(user.getId(), gemsValue);
                }
                showMsg(messageLbl, "✅  Modifications enregistrées avec succès.", false);
                saveBtn.setDisable(true);
                // Fermeture différée + callback
                new Thread(() -> {
                    try {
                        Thread.sleep(1100);
                    } catch (InterruptedException ignored) {
                    }
                    javafx.application.Platform.runLater(() -> {
                        dialog.close();
                        if (callback != null) callback.onSaved(user);
                    });
                }).start();

            } catch (SQLException ex) {
                showMsg(messageLbl, "❌  Erreur : " + ex.getMessage(), true);
            }
        });

        btnRow.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(header, form, messageLbl, btnRow);
        VBox.setMargin(messageLbl, new Insets(0, 24, 0, 24));

        // Stylesheet hérité
        Scene scene = new Scene(root);
        scene.setFill(null);
        if (owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        dialog.setScene(scene);
        dialog.show();
    }

    // ── Helpers UI ──

    private VBox fieldGroup(String label, javafx.scene.Node field) {
        VBox group = new VBox(5);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #8080a8; -fx-font-size: 10px; -fx-font-weight: bold;");
        group.getChildren().addAll(lbl, field);
        return group;
    }

    private TextField styledField(String value, boolean editable) {
        TextField tf = new TextField(value);
        tf.setEditable(editable || true);
        applyFieldStyle(tf);
        return tf;
    }

    private PasswordField styledPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        applyFieldStyle(pf);
        return pf;
    }

    private void applyFieldStyle(TextInputControl tf) {
        tf.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05);" +
                        "-fx-border-color: rgba(255,255,255,0.12);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #505080;" +
                        "-fx-padding: 8 12;" +
                        "-fx-font-size: 13px;" +
                        "-fx-pref-height: 36;"
        );
        tf.focusedProperty().addListener((obs, o, focused) -> {
            String base = "-fx-background-color: rgba(255,255,255,0.05);" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 8;" +
                    "-fx-background-radius: 8;" +
                    "-fx-text-fill: white;" +
                    "-fx-prompt-text-fill: #505080;" +
                    "-fx-padding: 8 12;" +
                    "-fx-font-size: 13px;" +
                    "-fx-pref-height: 36;";
            tf.setStyle(base + (focused
                    ? "-fx-border-color: #7c3aed;"
                    : "-fx-border-color: rgba(255,255,255,0.12);"));
        });
    }

    private ComboBox<String> styledCombo() {
        ComboBox<String> cb = new ComboBox<>();
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle(
                "-fx-background-color: rgba(255,255,255,0.05);" +
                        "-fx-border-color: rgba(255,255,255,0.12);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: #c0c0e0;" +
                        "-fx-pref-height: 36;" +
                        "-fx-font-size: 13px;"
        );
        return cb;
    }

    private void showMsg(Label lbl, String text, boolean isError) {
        lbl.setText(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isError ? "#ef4444" : "#22c55e") + ";");
    }

    // ── Role helpers ──
    private String roleFr(Roles role) {
        if (role == null) return "Joueur";
        return switch (role) {
            case ROLE_ADMIN -> "Admin";
            case ROLE_COACH -> "Coach";
            case ROLE_PLAYER -> "Joueur";
        };
    }

    private Roles roleFromFr(String fr) {
        return switch (fr) {
            case "Admin" -> Roles.ROLE_ADMIN;
            case "Coach" -> Roles.ROLE_COACH;
            default -> Roles.ROLE_PLAYER;
        };
    }

    private String typeFromRole(Roles role) {
        return switch (role) {
            case ROLE_ADMIN -> "user";
            case ROLE_COACH -> "coach";
            default -> "player";
        };
    }
}