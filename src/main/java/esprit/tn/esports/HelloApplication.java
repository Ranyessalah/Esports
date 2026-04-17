package esprit.tn.esports;

import esprit.tn.esports.utils.TeamWebServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Start the local web server so QR codes link to a live HTML page
        TeamWebServer.startServer();

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Login.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 1200, 760);
        stage.setTitle("ClutchX - Connexion");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}