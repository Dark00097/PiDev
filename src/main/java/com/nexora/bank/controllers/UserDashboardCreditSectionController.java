package com.nexora.bank.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class UserDashboardCreditSectionController {

    @FXML private VBox garantieFormContainer;
    @FXML private VBox garantiesDisplayContainer;
    
    @FXML private TextField txtRequestedAmount;
    @FXML private TextField txtInterestRate;
    @FXML private TextField txtMonthlyPayment;
    @FXML private ComboBox<String> cbDuree;
    @FXML private ComboBox<String> cbPurpose;
    @FXML private TextArea txtNotes;

    private static final Duration ANIMATION_DURATION = Duration.millis(250);

    @FXML
    private void initialize() {
        setVisibleManaged(garantieFormContainer, false);
        setVisibleManaged(garantiesDisplayContainer, false);
        
        // Add listener to calculate monthly payment when amount or duration changes
        if (txtRequestedAmount != null) {
            txtRequestedAmount.textProperty().addListener((obs, oldVal, newVal) -> calculateMonthlyPayment());
        }
        if (cbDuree != null) {
            cbDuree.valueProperty().addListener((obs, oldVal, newVal) -> calculateMonthlyPayment());
        }
    }

    @FXML
    private void showGarantieForm() {
        if (garantiesDisplayContainer.isVisible()) {
            setVisibleManaged(garantiesDisplayContainer, false);
        }
        
        setVisibleManaged(garantieFormContainer, true);
        animateSlideIn(garantieFormContainer);
    }

    @FXML
    private void hideGarantieForm() {
        animateSlideOut(garantieFormContainer, () -> {
            setVisibleManaged(garantieFormContainer, false);
        });
    }

    @FXML
    private void submitGuarantee() {
        showNotification("Garantie soumise", "Votre garantie a ete soumise pour examen.");
        animateSlideOut(garantieFormContainer, () -> {
            setVisibleManaged(garantieFormContainer, false);
            setVisibleManaged(garantiesDisplayContainer, true);
            animateSlideIn(garantiesDisplayContainer);
        });
    }

    @FXML
    private void showGaranties() {
        setVisibleManaged(garantieFormContainer, false);
        setVisibleManaged(garantiesDisplayContainer, true);
        animateSlideIn(garantiesDisplayContainer);
    }

    @FXML
    private void hideGaranties() {
        animateSlideOut(garantiesDisplayContainer, () -> {
            setVisibleManaged(garantiesDisplayContainer, false);
        });
    }

    @FXML
    private void newCreditApplication() {
        clearForm();
        showNotification("Nouvelle demande", "Pret a creer une nouvelle demande de credit.");
    }

    @FXML
    private void selectLoanType(MouseEvent event) {
        // Handle loan type selection
        // In a real application, you would update the interest rate based on the selected loan type
        showNotification("Type de pret selectionne", "Le taux d interet a ete mis a jour selon le type de pret.");
    }

    @FXML
    private void submitApplication() {
        if (validateForm()) {
            showNotification("Demande soumise", 
                "Votre demande de credit a ete soumise avec succes !\n" +
                "Reference : CR-" + (int)(Math.random() * 9000 + 1000));
            clearForm();
        }
    }

    @FXML
    private void saveAsDraft() {
        showNotification("Brouillon enregistre", "Votre demande a ete enregistree en brouillon.");
    }

    @FXML
    private void clearForm() {
        if (txtRequestedAmount != null) txtRequestedAmount.clear();
        if (cbDuree != null) cbDuree.getSelectionModel().clearSelection();
        if (cbPurpose != null) cbPurpose.getSelectionModel().clearSelection();
        if (txtNotes != null) txtNotes.clear();
        if (txtMonthlyPayment != null) txtMonthlyPayment.setText("0.00");
    }

    @FXML
    private void viewPaymentSchedule() {
        showNotification("Echeancier", "Ouverture du visualiseur d echeancier...");
    }

    private void calculateMonthlyPayment() {
        // Simple calculation for demonstration
        // In a real application, this would use proper amortization formulas
        try {
            if (txtRequestedAmount != null && !txtRequestedAmount.getText().isEmpty() && cbDuree != null && cbDuree.getValue() != null) {
                double amount = Double.parseDouble(txtRequestedAmount.getText().replace(",", ""));
                int months = Integer.parseInt(cbDuree.getValue().split(" ")[0]);
                double rate = 3.5 / 100 / 12; // Monthly rate
                
                double monthlyPayment = (amount * rate * Math.pow(1 + rate, months)) / (Math.pow(1 + rate, months) - 1);
                
                if (txtMonthlyPayment != null) {
                    txtMonthlyPayment.setText(String.format("%,.2f", monthlyPayment));
                }
            }
        } catch (NumberFormatException e) {
            // Ignore parsing errors
        }
    }

    private boolean validateForm() {
        if (txtRequestedAmount == null || txtRequestedAmount.getText().trim().isEmpty()) {
            showNotification("Erreur de validation", "Veuillez saisir un montant demande.");
            return false;
        }
        if (cbDuree == null || cbDuree.getValue() == null) {
            showNotification("Erreur de validation", "Veuillez selectionner une duree de pret.");
            return false;
        }
        if (cbPurpose == null || cbPurpose.getValue() == null) {
            showNotification("Erreur de validation", "Veuillez selectionner un motif de pret.");
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
