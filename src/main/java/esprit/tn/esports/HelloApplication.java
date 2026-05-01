package esprit.tn.esports;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // 🔥 Démarrer le serveur web avec support QR code
        System.out.println("🚀 Démarrage du serveur web avec génération de QR code...");
        esprit.tn.esports.utils.TeamWebServer.startServer();

        // Afficher l'IP locale pour les QR codes
        String localIp = esprit.tn.esports.utils.TeamWebServer.getLocalIp();
        String serverUrl = esprit.tn.esports.utils.TeamWebServer.getServerUrl();
        System.out.println("📱 Serveur QR Code accessible sur: " + serverUrl);
        System.out.println("🔗 Exemple: " + serverUrl + "/team?id=1");
        System.out.println("📱 Scannez le QR code depuis n'importe quel appareil sur le même réseau");

        // Charger l'interface JavaFX
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 760);

        // Ajouter un titre à la fenêtre
        stage.setTitle("ClutchX Esports Management System");

        stage.setScene(scene);
        stage.show();

        // Afficher un message dans la console
        System.out.println("✅ Application démarrée avec succès!");
        System.out.println("💡 Astuce: Utilisez l'interface pour générer des QR codes pour vos équipes");
    }

    @Override
    public void stop() throws Exception {
        System.out.println("🛑 Arrêt de l'application...");
        // Arrêter le serveur web proprement
        esprit.tn.esports.utils.TeamWebServer.stopServer();
        super.stop();
        System.out.println("✅ Application arrêtée");
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        System.out.println("🎮 ClutchX Esports Management System");
        System.out.println("=====================================");
        launch(args);
    }
}