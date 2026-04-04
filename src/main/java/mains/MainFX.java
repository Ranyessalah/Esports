package mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainFX extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
        try {

            Scene scene = new Scene(loader.load(), 1280, 760);
            scene.getStylesheets().add(getClass().getResource("/clutchx-theme.css").toExternalForm());

            primaryStage.setTitle("ClutchX");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(600);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }




}
