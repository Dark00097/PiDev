package com.nexora.bank.controllers;

import com.nexora.bank.Service.PendingCompteNotificationService;
import com.nexora.bank.Service.PendingCompteNotificationService.CompteEnAttente;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Locale;

public class NotificationPanelController {

    private final PendingCompteNotificationService pendingService = new PendingCompteNotificationService();
    private Stage popupStage;
    private VBox listContainer;
    private Label badgeLabel;
    private Runnable onClose;

    public void show(Window owner) {
        show(owner, null);
    }

    public void show(Window owner, Runnable onClose) {
        this.onClose = onClose;

        popupStage = new Stage();
        popupStage.initStyle(StageStyle.TRANSPARENT);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            popupStage.initOwner(owner);
        }
        popupStage.setResizable(false);

        VBox panel = buildPanel();
        StackPane wrapper = new StackPane(panel);
        wrapper.setStyle("-fx-background-color: transparent;");
        wrapper.setPadding(new Insets(20));

        Scene scene = new Scene(wrapper, 500, 640);
        scene.setFill(Color.TRANSPARENT);
        popupStage.setScene(scene);

        panel.setOpacity(0);
        panel.setScaleX(0.92);
        panel.setScaleY(0.92);

        popupStage.setOnShown(e -> {
            if (owner != null) {
                double cx = owner.getX() + owner.getWidth() / 2;
                double cy = owner.getY() + owner.getHeight() / 2;
                popupStage.setX(cx - popupStage.getWidth() / 2);
                popupStage.setY(cy - popupStage.getHeight() / 2);
            }

            FadeTransition fade = new FadeTransition(Duration.millis(220), panel);
            ScaleTransition scale = new ScaleTransition(Duration.millis(220), panel);
            fade.setFromValue(0);
            fade.setToValue(1);
            scale.setFromX(0.92);
            scale.setToX(1);
            scale.setFromY(0.92);
            scale.setToY(1);
            new ParallelTransition(fade, scale).play();
        });

        popupStage.setOnHidden(e -> {
            if (this.onClose != null) {
                this.onClose.run();
            }
        });

        popupStage.show();
    }

    private VBox buildPanel() {
        VBox panel = new VBox(0);
        panel.setPrefWidth(460);
        panel.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 18;" +
            "-fx-effect: dropshadow(gaussian, rgba(10,37,64,0.28), 40, 0, 0, 10);"
        );
        panel.getChildren().addAll(buildHeader(), buildScrollArea());
        return panel;
    }

    private HBox buildHeader() {
        FontIcon bellIcon = new FontIcon("fas-bell");
        bellIcon.setStyle("-fx-fill: #FACC15; -fx-icon-size: 20;");
        StackPane bellWrap = new StackPane(bellIcon);
        bellWrap.setStyle(
            "-fx-background-color: rgba(250,204,21,0.15);" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 10;"
        );

        Label title = new Label("Pending Account Validations");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0A2540;");

        int count = pendingService.countPending();
        badgeLabel = new Label(String.valueOf(count));
        badgeLabel.setStyle(
            "-fx-background-color: " + (count > 0 ? "#EF4444" : "#10B981") + ";" +
            "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 700;" +
            "-fx-background-radius: 10; -fx-padding: 2 8;"
        );

        Label subtitle = new Label("Requests waiting for admin approval");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        VBox titleBox = new VBox(3, new HBox(8, title, badgeLabel), subtitle);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        FontIcon closeIcon = new FontIcon("fas-times");
        closeIcon.setStyle("-fx-fill: #9CA3AF; -fx-icon-size: 15;");
        Button closeButton = new Button();
        closeButton.setGraphic(closeIcon);
        closeButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 8;");
        closeButton.setOnMouseEntered(e -> closeButton.setStyle(
            "-fx-background-color: #F3F4F6; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8;"));
        closeButton.setOnMouseExited(e -> closeButton.setStyle(
            "-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 8;"));
        closeButton.setOnAction(e -> closeWithAnimation());

        HBox header = new HBox(14, bellWrap, titleBox, spacer, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
            "-fx-background-color: #FAFAFA;" +
            "-fx-background-radius: 18 18 0 0;" +
            "-fx-padding: 18 20;" +
            "-fx-border-color: #E5E7EB; -fx-border-width: 0 0 1 0;"
        );
        return header;
    }

    private ScrollPane buildScrollArea() {
        listContainer = new VBox(12);
        listContainer.setPadding(new Insets(16));
        listContainer.setStyle("-fx-background-color: transparent;");

        refreshList();

        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(520);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private void refreshList() {
        listContainer.getChildren().clear();
        List<CompteEnAttente> comptes = pendingService.getPendingComptes();

        if (comptes.isEmpty()) {
            FontIcon checkIcon = new FontIcon("fas-check-circle");
            checkIcon.setStyle("-fx-fill: #10B981; -fx-icon-size: 44;");
            Label emptyTitle = new Label("All clear");
            emptyTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1F2937;");
            Label emptyDesc = new Label("No pending accounts to validate.");
            emptyDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #9CA3AF;");
            VBox emptyBox = new VBox(10, checkIcon, emptyTitle, emptyDesc);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(50));
            listContainer.getChildren().add(emptyBox);
        } else {
            for (CompteEnAttente compte : comptes) {
                listContainer.getChildren().add(buildCard(compte));
            }
        }

        int count = pendingService.countPending();
        if (badgeLabel != null) {
            badgeLabel.setText(String.valueOf(count));
            badgeLabel.setStyle(
                "-fx-background-color: " + (count > 0 ? "#EF4444" : "#10B981") + ";" +
                "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 700;" +
                "-fx-background-radius: 10; -fx-padding: 2 8;"
            );
        }
    }

    private VBox buildCard(CompteEnAttente compte) {
        FontIcon accountIcon = new FontIcon("fas-university");
        accountIcon.setStyle("-fx-fill: #00B4A0; -fx-icon-size: 17;");
        StackPane iconWrap = new StackPane(accountIcon);
        iconWrap.setStyle("-fx-background-color: rgba(0,180,160,0.12); -fx-background-radius: 8; -fx-padding: 8;");

        Label accountLabel = new Label("Account " + safe(compte.numeroCompte));
        accountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0A2540;");

        String type = safe(compte.typeCompte);
        String typeStyle;
        switch (type) {
            case "Epargne":
                typeStyle = "-fx-background-color:#FEF9C3; -fx-text-fill:#92400E;";
                break;
            case "Professionnel":
                typeStyle = "-fx-background-color:#EDE9FE; -fx-text-fill:#5B21B6;";
                break;
            default:
                typeStyle = "-fx-background-color:#DBEAFE; -fx-text-fill:#1D4ED8;";
                break;
        }
        Label typeLabel = new Label(type);
        typeLabel.setStyle(typeStyle + "-fx-font-size: 10px; -fx-font-weight: 700; -fx-background-radius: 6; -fx-padding: 2 8;");

        Region spacerTop = new Region();
        HBox.setHgrow(spacerTop, Priority.ALWAYS);
        HBox top = new HBox(10, iconWrap, accountLabel, spacerTop, typeLabel);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox infoBox = new VBox(6,
            infoRow("fas-user", "Client", safe(compte.nomClient)),
            infoRow("fas-coins", "Initial balance", String.format(Locale.US, "%,.2f DT", compte.solde)),
            infoRow("fas-calendar", "Opening date", safe(compte.dateOuverture)),
            infoRow("fas-arrow-down", "Withdrawal limit", String.format(Locale.US, "%,.2f DT", compte.plafondRetrait)),
            infoRow("fas-arrow-right", "Transfer limit", String.format(Locale.US, "%,.2f DT", compte.plafondVirement))
        );
        infoBox.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8; -fx-padding: 10 12;");

        Button acceptButton = buildActionButton("fas-check", "Accept", "#10B981", "#059669");
        acceptButton.setOnAction(e -> handleAccept(compte));
        Button refuseButton = buildActionButton("fas-times", "Refuse", "#EF4444", "#DC2626");
        refuseButton.setOnAction(e -> handleRefuse(compte));

        Region spacerButtons = new Region();
        HBox.setHgrow(spacerButtons, Priority.ALWAYS);
        HBox buttons = new HBox(10, spacerButtons, acceptButton, refuseButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(12, top, infoBox, buttons);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(10,37,64,0.06), 8, 0, 0, 2);"
        );
        return card;
    }

    private HBox infoRow(String iconLiteral, String label, String value) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setStyle("-fx-fill: #9CA3AF; -fx-icon-size: 11;");
        Label name = new Label(label + ":");
        name.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280; -fx-min-width: 115;");
        Label content = new Label(value);
        content.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #0A2540;");
        HBox row = new HBox(8, icon, name, content);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button buildActionButton(String iconLiteral, String text, String colorNormal, String colorHover) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setStyle("-fx-fill: white; -fx-icon-size: 12;");
        Button button = new Button(text);
        button.setGraphic(icon);
        button.setContentDisplay(ContentDisplay.LEFT);

        String baseStyle =
            "-fx-background-color: " + colorNormal + "; -fx-text-fill: white;" +
            "-fx-font-weight: 700; -fx-font-size: 12px; -fx-background-radius: 8;" +
            "-fx-padding: 8 18; -fx-cursor: hand; -fx-graphic-text-gap: 7;";
        String hoverStyle =
            "-fx-background-color: " + colorHover + "; -fx-text-fill: white;" +
            "-fx-font-weight: 700; -fx-font-size: 12px; -fx-background-radius: 8;" +
            "-fx-padding: 8 18; -fx-cursor: hand; -fx-graphic-text-gap: 7;";

        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
        return button;
    }

    private void handleAccept(CompteEnAttente compte) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm approval");
        confirm.setHeaderText("Activate account " + safe(compte.numeroCompte) + "?");
        confirm.setContentText("The account will become active immediately.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                pendingService.accepterCompte(compte.idCompte);
                refreshList();
                showToast("Account " + safe(compte.numeroCompte) + " activated.");
            }
        });
    }

    private void handleRefuse(CompteEnAttente compte) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm refusal");
        confirm.setHeaderText("Refuse account " + safe(compte.numeroCompte) + "?");
        confirm.setContentText("The account request will be removed from pending list.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                pendingService.refuserCompteAvecSms(compte.idCompte, true);
                refreshList();
                showToast("Account " + safe(compte.numeroCompte) + " refused.");
            }
        });
    }

    private void closeWithAnimation() {
        if (popupStage == null) {
            return;
        }
        VBox panel = (VBox) ((StackPane) popupStage.getScene().getRoot()).getChildren().get(0);
        FadeTransition fade = new FadeTransition(Duration.millis(180), panel);
        ScaleTransition scale = new ScaleTransition(Duration.millis(180), panel);
        fade.setToValue(0);
        scale.setToX(0.92);
        scale.setToY(0.92);
        ParallelTransition transition = new ParallelTransition(fade, scale);
        transition.setOnFinished(e -> popupStage.close());
        transition.play();
    }

    private void showToast(String message) {
        if (popupStage == null || popupStage.getScene() == null) {
            return;
        }
        Label toast = new Label(message);
        toast.setStyle(
            "-fx-background-color: #0A2540; -fx-text-fill: white;" +
            "-fx-font-size: 13px; -fx-font-weight: 600;" +
            "-fx-background-radius: 10; -fx-padding: 12 20;"
        );
        toast.setOpacity(0);

        StackPane root = (StackPane) popupStage.getScene().getRoot();
        StackPane wrap = new StackPane(toast);
        wrap.setAlignment(Pos.BOTTOM_CENTER);
        wrap.setMouseTransparent(true);
        wrap.setPadding(new Insets(0, 0, 28, 0));
        root.getChildren().add(wrap);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), toast);
        PauseTransition pause = new PauseTransition(Duration.millis(1600));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(260), toast);
        fadeIn.setToValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> root.getChildren().remove(wrap));
        new SequentialTransition(fadeIn, pause, fadeOut).play();
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "-" : value.trim();
    }
}
