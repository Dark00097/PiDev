package com.nexora.bank;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        SceneRouter.showOn(
            stage,
            "/fxml/Home.fxml",
            "NEXORA BANK - Welcome",
            1200,
            760,
            980,
            680
        );
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {

        launch(args);
    }
}
