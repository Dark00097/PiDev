package com.nexora.bank;

import java.io.IOException;
import java.net.URL;
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
            System.out.println("Loading FXML: " + fxmlPath);
            URL fxmlUrl = SceneRouter.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IOException("FXML file not found: " + fxmlPath);
            }
            System.out.println("FXML URL: " + fxmlUrl);
            
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            
            System.out.println("FXML loaded successfully: " + fxmlPath);

            Scene scene = new Scene(root, width, height);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setMinWidth(minWidth);
            stage.setMinHeight(minHeight);

            if (stage.getIcons().isEmpty()) {
                try {
                    Image icon = new Image(SceneRouter.class.getResourceAsStream("/images/logo.png"));
                    if (icon != null && !icon.isError()) {
                        stage.getIcons().add(icon);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load icon: " + e.getMessage());
                }
            }

            stage.centerOnScreen();
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible de charger la vue");
            alert.setContentText("Vue: " + fxmlPath + "\nErreur: " + ex.getMessage());
            alert.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors du chargement");
            alert.setContentText("Vue: " + fxmlPath + "\nErreur: " + ex.getMessage());
            alert.show();
        }
    }
}
