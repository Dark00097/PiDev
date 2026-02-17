package com.nexora.bank.controllers;

import com.nexora.bank.SceneRouter;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private VBox heroContent;
    @FXML private VBox welcomeCard;
    @FXML private HBox featurePillsRow;
    @FXML private HBox footerSection;
    @FXML private Button loginBtn;
    @FXML private Button signupBtn;

    @FXML private Circle meshOrb1;
    @FXML private Circle meshOrb2;
    @FXML private Circle meshOrb3;
    @FXML private Circle meshOrb4;
    @FXML private Circle centerOrb1;
    @FXML private Circle centerOrb2;
    @FXML private Circle centerOrb3;

    @FXML private StackPane logoComplex;
    @FXML private HBox statusIndicator;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        normalizeStyleClasses(rootPane);
        playEntrance();
        startFloat(meshOrb1, 20, 14, 9000);
        startFloat(meshOrb2, -18, 20, 11000);
        startFloat(meshOrb3, 24, -16, 8200);
        startFloat(meshOrb4, -16, -12, 9800);
        startFloat(centerOrb1, 22, 16, 12000);
        startFloat(centerOrb2, -16, -12, 10000);
        startFloat(centerOrb3, 18, -20, 13000);
        pulseNode(logoComplex, 1.05, 2600);
        pulseNode(statusIndicator, 1.04, 1500);
    }

    private void normalizeStyleClasses(Node node) {
        if (node == null) return;

        ObservableList<String> classes = node.getStyleClass();
        if (classes.size() == 1) {
            String raw = classes.get(0);
            if (raw != null && (raw.contains(" ") || raw.contains(","))) {
                classes.clear();
                String[] parts = raw.split("[,\\s]+");
                for (String part : parts) {
                    if (!part.isBlank()) {
                        classes.add(part.trim());
                    }
                }
            }
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                normalizeStyleClasses(child);
            }
        }
    }

    private void playEntrance() {
        if (heroContent != null) {
            heroContent.setOpacity(0);
            heroContent.setTranslateX(-40);
            FadeTransition fade = new FadeTransition(Duration.millis(700), heroContent);
            fade.setFromValue(0);
            fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(700), heroContent);
            slide.setFromX(-40);
            slide.setToX(0);
            new ParallelTransition(fade, slide).play();
        }

        if (welcomeCard != null) {
            welcomeCard.setOpacity(0);
            welcomeCard.setScaleX(0.92);
            welcomeCard.setScaleY(0.92);
            FadeTransition fade = new FadeTransition(Duration.millis(650), welcomeCard);
            fade.setFromValue(0);
            fade.setToValue(1);
            ScaleTransition scale = new ScaleTransition(Duration.millis(650), welcomeCard);
            scale.setFromX(0.92);
            scale.setFromY(0.92);
            scale.setToX(1);
            scale.setToY(1);
            new ParallelTransition(fade, scale).play();
        }

        if (featurePillsRow != null) {
            featurePillsRow.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(500), featurePillsRow);
            fade.setDelay(Duration.millis(250));
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }

        if (footerSection != null) {
            footerSection.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(400), footerSection);
            fade.setDelay(Duration.millis(400));
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }
    }

    private void startFloat(Node node, double x, double y, int durationMs) {
        if (node == null) return;
        TranslateTransition t = new TranslateTransition(Duration.millis(durationMs), node);
        t.setFromX(0);
        t.setFromY(0);
        t.setToX(x);
        t.setToY(y);
        t.setAutoReverse(true);
        t.setCycleCount(Animation.INDEFINITE);
        t.play();
    }

    private void pulseNode(Node node, double scale, int durationMs) {
        if (node == null) return;
        ScaleTransition s = new ScaleTransition(Duration.millis(durationMs), node);
        s.setFromX(1);
        s.setFromY(1);
        s.setToX(scale);
        s.setToY(scale);
        s.setAutoReverse(true);
        s.setCycleCount(Animation.INDEFINITE);
        s.play();
    }

    @FXML
    private void openLogin() {
        SceneRouter.show("/fxml/Login.fxml", "NEXORA BANK - Login", 1200, 760, 980, 680);
    }

    @FXML
    private void openSignup() {
        SceneRouter.show("/fxml/Signup.fxml", "NEXORA BANK - Sign Up", 1200, 760, 980, 680);
    }
}
