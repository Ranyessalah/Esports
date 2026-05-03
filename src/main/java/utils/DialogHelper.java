package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.function.Consumer;

public class DialogHelper {

    /**
     * Charge un FXML dans un Stage modal non décoré.
     * Le Consumer<T> permet d'initialiser le controller avant show().
     */
    public static <T> void openModal(Stage owner, String fxmlPath,
                                     Consumer<T> controllerInit) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    DialogHelper.class.getResource(fxmlPath));
            Parent root = loader.load();

            Stage modal = new Stage();
            modal.initOwner(owner);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.UNDECORATED);
            modal.setResizable(false);

            Scene scene = new Scene(root);
            scene.setFill(null);
            if (owner.getScene() != null)
                scene.getStylesheets().addAll(owner.getScene().getStylesheets());
            modal.setScene(scene);

            T controller = loader.getController();
            controllerInit.accept(controller);

            modal.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}