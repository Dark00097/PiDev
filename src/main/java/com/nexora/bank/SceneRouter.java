package com.nexora.bank;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public final class SceneRouter {

    private SceneRouter() {
    }

    public static void show(String fxmlPath, String title, double width, double height, double minWidth, double minHeight) {
        Stage stage = MainApp.getPrimaryStage();
        if (stage == null) {
            return;
        }
        showOn(stage, fxmlPath, title, width, height, minWidth, minHeight);
    }

    public static void showOn(Stage stage, String fxmlPath, String title, double width, double height, double minWidth, double minHeight) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneRouter.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setMinWidth(minWidth);
            stage.setMinHeight(minHeight);

            if (stage.getIcons().isEmpty()) {
                Image icon = new Image(SceneRouter.class.getResourceAsStream("/images/logo.png"));
                stage.getIcons().add(icon);
            }

            stage.centerOnScreen();
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible de charger la vue");
            alert.setContentText("Vue: " + fxmlPath);
            alert.show();
        }
    }
}
