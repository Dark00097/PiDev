package com.nexora.bank.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class UserDashboardTransactionsSectionController {

    @FXML private VBox reclamationFormContainer;
    @FXML private VBox reclamationsDisplayContainer;
    
    @FXML private ComboBox<String> cbCategorie;
    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TextField txtMontant;
    @FXML private TextField txtBalanceAfter;
    @FXML private TextField txtRecipient;
    @FXML private DatePicker dpDate;
    @FXML private TextArea txtDescription;

    private static final Duration ANIMATION_DURATION = Duration.millis(250);

    @FXML
    private void initialize() {
        setVisibleManaged(reclamationFormContainer, false);
        setVisibleManaged(reclamationsDisplayContainer, false);
    }

    @FXML
    private void showReclamationForm() {
        // Hide complaints list if visible
        if (reclamationsDisplayContainer.isVisible()) {
            setVisibleManaged(reclamationsDisplayContainer, false);
        }
        
        setVisibleManaged(reclamationFormContainer, true);
        animateSlideIn(reclamationFormContainer);
    }

    @FXML
    private void hideReclamationForm() {
        animateSlideOut(reclamationFormContainer, () -> {
            setVisibleManaged(reclamationFormContainer, false);
        });
    }

    @FXML
    private void submitComplaint() {
        // Validate and submit complaint
        showNotification("Reclamation soumise", "Votre reclamation a ete enregistree avec succes. Reference : REC-" + (int)(Math.random() * 9000 + 1000));
        
        // Show complaints list after submission
        animateSlideOut(reclamationFormContainer, () -> {
            setVisibleManaged(reclamationFormContainer, false);
            setVisibleManaged(reclamationsDisplayContainer, true);
            animateSlideIn(reclamationsDisplayContainer);
        });
    }

    @FXML
    private void showReclamations() {
        setVisibleManaged(reclamationFormContainer, false);
        setVisibleManaged(reclamationsDisplayContainer, true);
        animateSlideIn(reclamationsDisplayContainer);
    }

    @FXML
    private void hideReclamations() {
        animateSlideOut(reclamationsDisplayContainer, () -> {
            setVisibleManaged(reclamationsDisplayContainer, false);
        });
    }

    @FXML
    private void newTransaction() {
        clearForm();
        showNotification("Nouvelle transaction", "Pret a creer une nouvelle transaction.");
    }

    @FXML
    private void saveTransaction() {
        if (validateForm()) {
            showNotification("Transaction enregistree", "La transaction a ete enregistree avec succes.");
            clearForm();
        }
    }

    @FXML
    private void deleteTransaction() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer la transaction");
        alert.setHeaderText("Voulez-vous vraiment supprimer cette transaction ?");
        alert.setContentText("Cette action est irreversible.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                clearForm();
                showNotification("Transaction supprimee", "La transaction a ete supprimee.");
            }
        });
    }

    @FXML
    private void clearForm() {
        if (cbCategorie != null) cbCategorie.getSelectionModel().clearSelection();
        if (cbType != null) cbType.getSelectionModel().clearSelection();
        if (cbStatut != null) cbStatut.getSelectionModel().clearSelection();
        if (txtMontant != null) txtMontant.clear();
        if (txtBalanceAfter != null) txtBalanceAfter.clear();
        if (txtRecipient != null) txtRecipient.clear();
        if (dpDate != null) dpDate.setValue(null);
        if (txtDescription != null) txtDescription.clear();
    }

    @FXML
    private void selectTransaction(MouseEvent event) {
        // Handle transaction selection - populate form with transaction data
        showNotification("Transaction selectionnee", "Les donnees de la transaction ont ete chargees dans le formulaire.");
    }

    @FXML
    private void retryTransaction() {
        showNotification("Nouvel essai", "Nouveau traitement de la transaction...");
    }

    @FXML
    private void exportTransactions() {
        showNotification("Export", "Export des transactions en CSV...");
    }

    private boolean validateForm() {
        if (cbCategorie == null || cbCategorie.getValue() == null) {
            showNotification("Erreur de validation", "Veuillez selectionner une categorie.");
            return false;
        }
        if (txtMontant == null || txtMontant.getText().trim().isEmpty()) {
            showNotification("Erreur de validation", "Veuillez saisir un montant.");
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
