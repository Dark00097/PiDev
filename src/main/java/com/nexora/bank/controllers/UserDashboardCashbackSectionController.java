package com.nexora.bank.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class UserDashboardCashbackSectionController {

    @FXML private VBox partnerFormContainer;
    @FXML private VBox cashbackFormContainer;
    
    @FXML private Button tabPartner;
    @FXML private Button tabCashback;
    
    @FXML private TextField txtPartnerName;
    @FXML private TextField txtBaseRate;
    @FXML private TextField txtMaxRate;
    @FXML private TextField txtMonthlyLimit;
    @FXML private DatePicker dpValidUntil;
    @FXML private TextArea txtPartnerDescription;
    
    @FXML private TextField txtPurchaseAmount;
    @FXML private TextField txtAppliedRate;
    @FXML private TextField txtCashbackAmount;
    @FXML private TextField txtTransactionRef;
    @FXML private DatePicker dpPurchaseDate;

    private static final Duration ANIMATION_DURATION = Duration.millis(250);
    private static final String TAB_ACTIVE_CLASS = "form-tab-active";

    @FXML
    private void initialize() {
        // Setup listeners for cashback calculation
        if (txtPurchaseAmount != null) {
            txtPurchaseAmount.textProperty().addListener((obs, oldVal, newVal) -> calculateCashback());
        }
        
        // Show partner form by default
        showPartnerForm();
    }

    @FXML
    private void showPartnerForm() {
        updateTabStyles(tabPartner);
        setVisibleManaged(partnerFormContainer, true);
        setVisibleManaged(cashbackFormContainer, false);
        animateFadeIn(partnerFormContainer);
    }

    @FXML
    private void showCashbackForm() {
        updateTabStyles(tabCashback);
        setVisibleManaged(partnerFormContainer, false);
        setVisibleManaged(cashbackFormContainer, true);
        animateFadeIn(cashbackFormContainer);
    }

    @FXML
    private void addPartner() {
        if (validatePartnerForm()) {
            showNotification("Partenaire ajoute", 
                "Le partenaire \"" + txtPartnerName.getText() + "\" a ete enregistre avec succes !");
            clearPartnerForm();
        }
    }

    @FXML
    private void clearPartnerForm() {
        if (txtPartnerName != null) txtPartnerName.clear();
        if (txtBaseRate != null) txtBaseRate.clear();
        if (txtMaxRate != null) txtMaxRate.clear();
        if (txtMonthlyLimit != null) txtMonthlyLimit.clear();
        if (dpValidUntil != null) dpValidUntil.setValue(null);
        if (txtPartnerDescription != null) txtPartnerDescription.clear();
    }

    @FXML
    private void recordCashback() {
        if (validateCashbackForm()) {
            String amount = txtCashbackAmount != null ? txtCashbackAmount.getText() : "0.00";
            showNotification("Cashback enregistre", 
                "Le cashback de $" + amount + " a ete enregistre et sera credite sous 3-5 jours ouvrables.");
            clearCashbackForm();
        }
    }

    @FXML
    private void clearCashbackForm() {
        if (txtPurchaseAmount != null) txtPurchaseAmount.clear();
        if (txtCashbackAmount != null) txtCashbackAmount.setText("0.00");
        if (txtTransactionRef != null) txtTransactionRef.clear();
        if (dpPurchaseDate != null) dpPurchaseDate.setValue(null);
    }

    @FXML
    private void viewRewardsHistory() {
        showNotification("Historique des recompenses", "Ouverture de l historique complet de vos recompenses...");
    }

    @FXML
    private void redeemRewards() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Echanger les recompenses");
        alert.setHeaderText("Echanger vos recompenses disponibles ?");
        alert.setContentText("Vous avez $127.50 disponibles. Voulez-vous :\n\n" +
            "- Transferer vers votre compte bancaire\n" +
            "- Appliquer au prochain achat\n" +
            "- Convertir en cartes cadeaux");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showNotification("Echange lance", 
                    "L echange de vos recompenses a ete lance. Vous recevrez un email de confirmation bientot.");
            }
        });
    }

    private void calculateCashback() {
        try {
            if (txtPurchaseAmount != null && !txtPurchaseAmount.getText().isEmpty() && txtAppliedRate != null) {
                double amount = Double.parseDouble(txtPurchaseAmount.getText().replace(",", ""));
                double rate = Double.parseDouble(txtAppliedRate.getText().replace("%", ""));
                double cashback = amount * (rate / 100);
                
                if (txtCashbackAmount != null) {
                    txtCashbackAmount.setText(String.format("%.2f", cashback));
                }
            }
        } catch (NumberFormatException e) {
            // Ignore parsing errors
        }
    }

    private boolean validatePartnerForm() {
        if (txtPartnerName == null || txtPartnerName.getText().trim().isEmpty()) {
            showNotification("Erreur de validation", "Veuillez saisir un nom de partenaire.");
            return false;
        }
        if (txtBaseRate == null || txtBaseRate.getText().trim().isEmpty()) {
            showNotification("Erreur de validation", "Veuillez saisir un taux de cashback de base.");
            return false;
        }
        return true;
    }

    private boolean validateCashbackForm() {
        if (txtPurchaseAmount == null || txtPurchaseAmount.getText().trim().isEmpty()) {
            showNotification("Erreur de validation", "Veuillez saisir un montant d achat.");
            return false;
        }
        if (dpPurchaseDate == null || dpPurchaseDate.getValue() == null) {
            showNotification("Erreur de validation", "Veuillez selectionner une date d achat.");
            return false;
        }
        return true;
    }

    private void updateTabStyles(Button activeTab) {
        if (tabPartner != null) {
            tabPartner.getStyleClass().remove(TAB_ACTIVE_CLASS);
        }
        if (tabCashback != null) {
            tabCashback.getStyleClass().remove(TAB_ACTIVE_CLASS);
        }
        if (activeTab != null && !activeTab.getStyleClass().contains(TAB_ACTIVE_CLASS)) {
            activeTab.getStyleClass().add(TAB_ACTIVE_CLASS);
        }
    }

    private void animateFadeIn(VBox node) {
        if (node == null) return;
        
        node.setOpacity(0);
        
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        fade.play();
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

