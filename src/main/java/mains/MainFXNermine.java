package mains;

import controllers.AdminDashboardController;
import controllers.MainLayoutController;
import controllers.gestion_match.ClientEquipeController;
import controllers.gestion_match.MatchController;
import entities.Roles;
import entities.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utils.PreferencesRepository;
import utils.TeamWebServer;

import java.io.IOException;

import static javafx.application.Application.launch;

public class MainFXNermine extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        PreferencesRepository prefs = new PreferencesRepository();
        User savedUser = prefs.loadSession(); // null si pas de rememberMe ou pas de session
        System.out.println("saved user: " + savedUser);
        if (savedUser != null) {
            routeByRole(savedUser, primaryStage);
        } else {
            loadLogin(primaryStage);
        }

        primaryStage.show();
    }

    private void routeByRole(User user, Stage stage) {
        if (user.getRole() == Roles.ROLE_ADMIN) {
            loadAdmin(user, stage);
        } else {
            loadMain(user, stage);
        }
    }

    private void loadLogin(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 760);
            scene.getStylesheets().add(getClass().getResource("/clutchx-theme.css").toExternalForm());
            stage.setTitle("ClutchX");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAdmin(User user, Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_match/matchIndex.fxml"));
            Parent root = loader.load();

            MatchController ctrl = loader.getController();
            //ctrl.setAdminEmail(user.getEmail());

            Scene scene = new Scene(root, 1280, 760);
            scene.getStylesheets().add(getClass().getResource("/clutchx-theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/admin-theme.css").toExternalForm());
            stage.setTitle("ClutchX — Backoffice Admin");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadMain(User user, Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gestion_match/equipeIndex_client.fxml"));
            Parent root = loader.load();

            ClientEquipeController ctrl = loader.getController();
            //ctrl.setUserEmail(user.getEmail());

            Scene scene = new Scene(root, 1280, 760);
            scene.getStylesheets().add(getClass().getResource("/clutchx-theme.css").toExternalForm());
            stage.setTitle("ClutchX — Dashboard");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}