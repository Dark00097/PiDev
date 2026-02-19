package com.nexora.bank.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

public class UserDashboardAccountsSectionController {

    @FXML private VBox coffreFormContainer;
    @FXML private VBox coffresDisplayContainer;
    
    @FXML private TextField txtAccountNumber;
    @FXML private TextField txtBalance;
    @FXML private DatePicker dpOpeningDate;
    @FXML private ComboBox<String> cbStatus;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField txtWithdrawLimit;
    @FXML private TextField txtTransferLimit;

    private static final Duration ANIMATION_DURATION = Duration.millis(250);

    @FXML
    private void initialize() {
        setVisibleManaged(coffreFormContainer, false);
        setVisibleManaged(coffresDisplayContainer, false);
    }

    @FXML
    private void toggleCoffreForm() {
        boolean willBeVisible = !coffreFormContainer.isVisible();
        
        if (willBeVisible) {
            setVisibleManaged(coffreFormContainer, true);
            animateSlideIn(coffreFormContainer);
        } else {
            animateSlideOut(coffreFormContainer, () -> setVisibleManaged(coffreFormContainer, false));
        }
    }

    @FXML
    private void hideCoffreForm() {
        animateSlideOut(coffreFormContainer, () -> setVisibleManaged(coffreFormContainer, false));
    }

    @FXML
    private void showCoffres() {
        if (!coffresDisplayContainer.isVisible()) {
            setVisibleManaged(coffresDisplayContainer, true);
            animateSlideIn(coffresDisplayContainer);
        }
    }

    @FXML
    private void hideCoffres() {
        animateSlideOut(coffresDisplayContainer, () -> setVisibleManaged(coffresDisplayContainer, false));
    }

    @FXML
    private void selectAccount(MouseEvent event) {
        // Handle account selection - populate form with account data
        // This would typically load the selected account's data into the form
        showNotification("Compte selectionne", "Les donnees du compte ont ete chargees dans le formulaire.");
    }

    @FXML
    private void addNewAccount() {
        clearForm();
        showNotification("Nouveau compte", "Pret a creer un nouveau compte.");
    }

    @FXML
    private void saveAccount() {
        // Validate and save account
        if (validateForm()) {
            showNotification("Succes", "Compte enregistre avec succes.");
        }
    }

    @FXML
    private void deleteAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le compte");
        alert.setHeaderText("Voulez-vous vraiment supprimer ce compte ?");
        alert.setContentText("Cette action est irreversible.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                clearForm();
                showNotification("Supprime", "Le compte a ete supprime.");
            }
        });
    }

    @FXML
    private void clearForm() {
        if (txtAccountNumber != null) txtAccountNumber.clear();
        if (txtBalance != null) txtBalance.clear();
        if (dpOpeningDate != null) dpOpeningDate.setValue(null);
        if (cbStatus != null) cbStatus.getSelectionModel().clearSelection();
        if (cbType != null) cbType.getSelectionModel().clearSelection();
        if (txtWithdrawLimit != null) txtWithdrawLimit.clear();
        if (txtTransferLimit != null) txtTransferLimit.clear();
    }

    @FXML
    private void refreshData() {
        // Refresh account data from backend
        showNotification("Actualise", "Les donnees du compte ont ete actualisees.");
    }

    private boolean validateForm() {
        if (txtAccountNumber == null || txtAccountNumber.getText().trim().isEmpty()) {
            showNotification("Erreur de validation", "Veuillez saisir un numero de compte.");
            return false;
        }
        return true;
    }

    private void animateSlideIn(VBox node) {
        if (node == null) return;
        
        node.setOpacity(0);
        node.setTranslateY(-20);
        
        ParallelTransition transition = new ParallelTransition();
        
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        
        TranslateTransition slide = new TranslateTransition(ANIMATION_DURATION, node);
        slide.setFromY(-20);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        
        transition.getChildren().addAll(fade, slide);
        transition.play();
    }

    private void animateSlideOut(VBox node, Runnable onFinished) {
        if (node == null) {
            if (onFinished != null) onFinished.run();
            return;
        }
        
        ParallelTransition transition = new ParallelTransition();
        
        FadeTransition fade = new FadeTransition(Duration.millis(150), node);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setInterpolator(Interpolator.EASE_IN);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(150), node);
        slide.setFromY(0);
        slide.setToY(-10);
        slide.setInterpolator(Interpolator.EASE_IN);
        
        transition.getChildren().addAll(fade, slide);
        transition.setOnFinished(e -> {
            if (onFinished != null) onFinished.run();
        });
        transition.play();
    }

    private void setVisibleManaged(VBox node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private void showNotification(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
