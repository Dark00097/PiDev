package com.nexora.bank.controllers;

import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.Cashback;
import com.nexora.bank.Models.Partenaire;
import com.nexora.bank.Models.User;
import com.nexora.bank.Service.CashbackService;
import com.nexora.bank.Service.EmailNotificationService;
import com.nexora.bank.Service.PartenaireService;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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

    @FXML private ComboBox<String> cmbPartnerSelection;
    @FXML private TextField txtPurchaseAmount;
    @FXML private TextField txtAppliedRate;
    @FXML private TextField txtCashbackAmount;
    @FXML private TextField txtTransactionRef;
    @FXML private DatePicker dpPurchaseDate;

    @FXML private Label lblKpiTotalRewards;
    @FXML private Label lblKpiThisMonth;
    @FXML private Label lblKpiMonthDescription;
    @FXML private Label lblKpiPartnerCount;
    @FXML private Label lblKpiPendingAmount;
    @FXML private Label lblTotalRewardsValue;
    @FXML private Label lblQuickPendingAmount;
    @FXML private VBox cashbackListContainer;

    private static final Duration ANIMATION_DURATION = Duration.millis(250);
    private static final String TAB_ACTIVE_CLASS = "form-tab-active";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final CashbackService cashbackService = new CashbackService();
    private final PartenaireService partenaireService = new PartenaireService();
    private final EmailNotificationService emailNotificationService = new EmailNotificationService();

    private final ObservableList<Cashback> userCashbacks = FXCollections.observableArrayList();
    private Cashback editingCashback;

    @FXML
    private void initialize() {
        if (txtPurchaseAmount != null) {
            txtPurchaseAmount.textProperty().addListener((obs, oldVal, newVal) -> calculateCashback());
        }
        if (txtAppliedRate != null) {
            txtAppliedRate.textProperty().addListener((obs, oldVal, newVal) -> calculateCashback());
        }

        showCashbackForm();
        refreshDashboardData();
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
        if (!validatePartnerForm()) {
            return;
        }

        try {
            Partenaire partenaire = new Partenaire();
            partenaire.setNom(txtPartnerName.getText().trim());
            partenaire.setCategorie("General");
            partenaire.setDescription(txtPartnerDescription == null ? "" : txtPartnerDescription.getText().trim());
            partenaire.setTauxCashback(parseDouble(txtBaseRate.getText()));
            partenaire.setTauxCashbackMax(txtMaxRate == null || txtMaxRate.getText().isBlank()
                    ? partenaire.getTauxCashback()
                    : parseDouble(txtMaxRate.getText()));
            partenaire.setPlafondMensuel(txtMonthlyLimit == null || txtMonthlyLimit.getText().isBlank()
                    ? 0
                    : parseDouble(txtMonthlyLimit.getText()));
            partenaire.setConditions(dpValidUntil != null && dpValidUntil.getValue() != null
                    ? "Valid until " + dpValidUntil.getValue()
                    : "");
            partenaire.setStatus("Actif");

            partenaireService.createPartenaire(partenaire);
            showNotification("Partenaire ajoute", "Le partenaire a ete enregistre avec succes.");
            clearPartnerForm();
            refreshDashboardData();
        } catch (RuntimeException ex) {
            showNotification("Erreur", ex.getMessage());
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
        if (!validateCashbackForm()) {
            return;
        }

        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null || currentUser.getIdUser() <= 0) {
            showNotification("Session", "Veuillez vous reconnecter.");
            return;
        }

        try {
            Cashback cashback = new Cashback();
            cashback.setIdUser(currentUser.getIdUser());

            String selectedPartenaire = cmbPartnerSelection == null ? "" : cmbPartnerSelection.getValue();
            if (selectedPartenaire == null || selectedPartenaire.isBlank()) {
                showNotification("Validation", "Selectionnez un partenaire.");
                return;
            }
            cashback.setPartenaireNom(selectedPartenaire);
            cashback.setIdPartenaire(partenaireService.findByName(selectedPartenaire).map(Partenaire::getIdPartenaire).orElse(null));

            cashback.setMontantAchat(parseDouble(txtPurchaseAmount.getText()));
            cashback.setTauxApplique(parseDouble(txtAppliedRate.getText()));
            cashback.setMontantCashback(parseDouble(txtCashbackAmount.getText()));
            cashback.setDateAchat(dpPurchaseDate.getValue());
            cashback.setDateCredit(null);
            cashback.setDateExpiration(dpPurchaseDate.getValue() == null ? null : dpPurchaseDate.getValue().plusMonths(6));
            cashback.setStatut("En attente");
            cashback.setTransactionRef(txtTransactionRef == null ? "" : txtTransactionRef.getText().trim());

            if (editingCashback != null) {
                cashback.setIdCashback(editingCashback.getIdCashback());
                boolean updated = cashbackService.updateCashbackForUser(cashback, currentUser.getIdUser());
                if (!updated) {
                    showNotification("Erreur", "Modification refusee.");
                    return;
                }
                showNotification("Cashback modifie", "Votre cashback a ete mis a jour.");
            } else {
                int created = cashbackService.createCashback(cashback);
                if (created <= 0) {
                    showNotification("Erreur", "Creation impossible.");
                    return;
                }
                cashback.setIdCashback(created);
                try {
                    emailNotificationService.sendCashbackSubmittedEmail(currentUser, cashback);
                } catch (RuntimeException mailEx) {
                    showNotification("Information", "Cashback enregistre, mais l'email de confirmation n'a pas pu etre envoye.");
                }
                showNotification("Cashback enregistre", "Le cashback a ete enregistre avec succes.");
            }

            clearCashbackForm();
            refreshDashboardData();
        } catch (RuntimeException ex) {
            showNotification("Erreur", ex.getMessage());
        }
    }

    @FXML
    private void clearCashbackForm() {
        if (txtPurchaseAmount != null) txtPurchaseAmount.clear();
        if (txtAppliedRate != null && txtAppliedRate.getText().isBlank()) txtAppliedRate.setText("5.0");
        if (txtCashbackAmount != null) txtCashbackAmount.setText("0.00");
        if (txtTransactionRef != null) txtTransactionRef.clear();
        if (dpPurchaseDate != null) dpPurchaseDate.setValue(null);
        if (cmbPartnerSelection != null && !cmbPartnerSelection.getItems().isEmpty()) {
            cmbPartnerSelection.getSelectionModel().selectFirst();
        }
        editingCashback = null;
    }

    @FXML
    private void viewRewardsHistory() {
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null || currentUser.getIdUser() <= 0) {
            showNotification("Session", "Veuillez vous reconnecter.");
            return;
        }

        List<Cashback> rows = cashbackService.getCashbacksByUser(currentUser.getIdUser());
        if (rows.isEmpty()) {
            showNotification("Historique", "Aucun cashback enregistre.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Historique Cashback");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        TableView<Cashback> table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        TableColumn<Cashback, String> colPartner = new TableColumn<>("Partenaire");
        colPartner.setCellValueFactory(new PropertyValueFactory<>("partenaireNom"));

        TableColumn<Cashback, String> colPurchase = new TableColumn<>("Achat");
        colPurchase.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getMontantAchat())));

        TableColumn<Cashback, String> colRate = new TableColumn<>("Taux");
        colRate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTauxApplique() + "%"));

        TableColumn<Cashback, String> colCashback = new TableColumn<>("Cashback");
        colCashback.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getMontantCashback())));

        TableColumn<Cashback, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getDateAchat())));

        TableColumn<Cashback, String> colStatus = new TableColumn<>("Statut");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statut"));

        table.getColumns().addAll(colPartner, colPurchase, colRate, colCashback, colDate, colStatus);
        table.getItems().setAll(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button btnEdit = new Button("Modifier");
        Button btnDelete = new Button("Supprimer");

        btnEdit.setOnAction(event -> {
            Cashback selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showNotification("Historique", "Selectionnez une ligne a modifier.");
                return;
            }
            loadCashbackToForm(selected);
            dialog.close();
        });

        btnDelete.setOnAction(event -> {
            Cashback selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showNotification("Historique", "Selectionnez une ligne a supprimer.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce cashback ?", ButtonType.OK, ButtonType.CANCEL);
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                boolean deleted = cashbackService.deleteCashbackForUser(selected.getIdCashback(), currentUser.getIdUser());
                if (deleted) {
                    table.getItems().remove(selected);
                    refreshDashboardData();
                } else {
                    showNotification("Erreur", "Suppression refusee.");
                }
            }
        });

        HBox actions = new HBox(10, btnEdit, btnDelete);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(12, table, actions);
        content.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        dialog.getDialogPane().setPrefWidth(850);
        dialog.getDialogPane().setPrefHeight(450);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    @FXML
    private void redeemRewards() {
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null || currentUser.getIdUser() <= 0) {
            showNotification("Session", "Veuillez vous reconnecter.");
            return;
        }

        double available = cashbackService.getCreditedTotalByUser(currentUser.getIdUser());
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Echanger les recompenses");
        alert.setHeaderText("Echanger vos recompenses disponibles ?");
        alert.setContentText("Solde disponible: $" + String.format("%.2f", available));

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showNotification("Echange lance", "Demande de redemption enregistree.");
            }
        });
    }

    private void loadCashbackToForm(Cashback cashback) {
        editingCashback = cashback;
        showCashbackForm();
        if (cmbPartnerSelection != null) cmbPartnerSelection.setValue(cashback.getPartenaireNom());
        if (txtPurchaseAmount != null) txtPurchaseAmount.setText(String.valueOf(cashback.getMontantAchat()));
        if (txtAppliedRate != null) txtAppliedRate.setText(String.valueOf(cashback.getTauxApplique()));
        if (txtCashbackAmount != null) txtCashbackAmount.setText(String.format("%.2f", cashback.getMontantCashback()));
        if (txtTransactionRef != null) txtTransactionRef.setText(cashback.getTransactionRef());
        if (dpPurchaseDate != null) dpPurchaseDate.setValue(cashback.getDateAchat());
    }

    private void refreshDashboardData() {
        User currentUser = AuthSession.getCurrentUser();

        List<Partenaire> partenaires = partenaireService.getAllPartenaires();
        if (cmbPartnerSelection != null) {
            cmbPartnerSelection.getItems().setAll(partenaires.stream().map(Partenaire::getNom).toList());
            if (!cmbPartnerSelection.getItems().isEmpty() && cmbPartnerSelection.getValue() == null) {
                cmbPartnerSelection.getSelectionModel().selectFirst();
            }
        }

        if (currentUser == null || currentUser.getIdUser() <= 0) {
            userCashbacks.clear();
            renderCashbackList();
            return;
        }

        userCashbacks.setAll(cashbackService.getCashbacksByUser(currentUser.getIdUser()));
        userCashbacks.sort(Comparator.comparing(Cashback::getDateAchat, Comparator.nullsLast(LocalDate::compareTo)).reversed());
        renderCashbackList();

        double total = cashbackService.getCreditedTotalByUser(currentUser.getIdUser());
        double month = cashbackService.getCurrentMonthTotalByUser(currentUser.getIdUser());
        double pending = cashbackService.getPendingTotalByUser(currentUser.getIdUser());

        if (lblKpiTotalRewards != null) lblKpiTotalRewards.setText(formatMoney(total));
        if (lblKpiThisMonth != null) lblKpiThisMonth.setText("+" + formatMoney(month));
        if (lblKpiMonthDescription != null) {
            String monthName = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            lblKpiMonthDescription.setText(monthName + " " + LocalDate.now().getYear());
        }
        if (lblKpiPartnerCount != null) lblKpiPartnerCount.setText(String.valueOf(partenaires.size()));
        if (lblKpiPendingAmount != null) lblKpiPendingAmount.setText(formatMoney(pending));
        if (lblTotalRewardsValue != null) lblTotalRewardsValue.setText(formatMoney(total));
        if (lblQuickPendingAmount != null) lblQuickPendingAmount.setText(formatMoney(pending));
    }

    private void renderCashbackList() {
        if (cashbackListContainer == null) {
            return;
        }

        cashbackListContainer.getChildren().clear();
        if (userCashbacks.isEmpty()) {
            Label empty = new Label("Aucun cashback enregistre.");
            empty.getStyleClass().add("cashback-item-date");
            cashbackListContainer.getChildren().add(empty);
            return;
        }

        int limit = Math.min(6, userCashbacks.size());
        for (int i = 0; i < limit; i++) {
            Cashback cashback = userCashbacks.get(i);
            cashbackListContainer.getChildren().add(buildCashbackRow(cashback));
        }
    }

    private HBox buildCashbackRow(Cashback cashback) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("cashback-item");

        if ("En attente".equalsIgnoreCase(cashback.getStatut())) {
            row.getStyleClass().add("cashback-item-pending");
        } else if ("Expire".equalsIgnoreCase(cashback.getStatut())) {
            row.getStyleClass().add("cashback-item-expired");
        }

        VBox details = new VBox(4);
        HBox.setHgrow(details, Priority.ALWAYS);

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        Label merchant = new Label(cashback.getPartenaireNom());
        merchant.getStyleClass().add("cashback-item-merchant");

        Label status = new Label(safe(cashback.getStatut()));
        status.getStyleClass().add("cashback-item-rate");

        top.getChildren().addAll(merchant, status);

        HBox meta = new HBox(10);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label purchase = new Label("Achat : " + formatMoney(cashback.getMontantAchat()));
        purchase.getStyleClass().add("cashback-item-purchase");

        Label date = new Label(formatDate(cashback.getDateAchat()));
        date.getStyleClass().add("cashback-item-date");

        meta.getChildren().addAll(purchase, date);
        details.getChildren().addAll(top, meta);

        VBox amounts = new VBox(2);
        amounts.setAlignment(Pos.CENTER_RIGHT);

        Label cashbackAmount = new Label("+" + formatMoney(cashback.getMontantCashback()));
        cashbackAmount.getStyleClass().add("cashback-item-amount");

        Label rate = new Label("Taux " + cashback.getTauxApplique() + "%");
        rate.getStyleClass().add("cashback-item-rate");

        amounts.getChildren().addAll(cashbackAmount, rate);

        row.getChildren().addAll(details, amounts);
        return row;
    }

    private void calculateCashback() {
        try {
            if (txtPurchaseAmount == null || txtAppliedRate == null || txtCashbackAmount == null) {
                return;
            }
            double amount = parseDouble(txtPurchaseAmount.getText());
            double rate = parseDouble(txtAppliedRate.getText().replace("%", ""));
            txtCashbackAmount.setText(String.format("%.2f", amount * (rate / 100)));
        } catch (NumberFormatException ex) {
            txtCashbackAmount.setText("0.00");
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

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FORMAT);
    }

    private String formatMoney(double amount) {
        return "$" + String.format("%.2f", amount);
    }

    private double parseDouble(String value) {
        String normalized = value == null ? "0" : value.trim().replace(",", ".");
        if (normalized.isBlank()) {
            return 0;
        }
        return Double.parseDouble(normalized);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showNotification(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
