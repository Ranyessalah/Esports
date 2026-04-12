package tn.esprit.tournamentmodule;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        javafx.scene.Parent root = FXMLLoader.load(Objects.requireNonNull(
            getClass().getResource("/tn/esprit/tournamentmodule/view/MainView.fxml")));

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(Objects.requireNonNull(
            getClass().getResource(
                "/tn/esprit/tournamentmodule/view/app.css")).toExternalForm());

        stage.setTitle("ClutchX – Tournament Module");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
