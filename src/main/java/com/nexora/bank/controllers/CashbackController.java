package com.nexora.bank.controllers;

import com.nexora.bank.Models.Cashback;
import com.nexora.bank.Models.Partenaire;
import com.nexora.bank.Service.CashbackService;
import com.nexora.bank.Service.PartenaireService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

public class CashbackController implements Initializable {

    @FXML private Label lblTotalCashback;
    @FXML private Label lblNombreBeneficiaires;
    @FXML private Label lblCashbackMois;

    @FXML private TextField txtUserId;
    @FXML private TextField txtMontantAchat;
    @FXML private TextField txtTauxApplique;
    @FXML private TextField txtMontantCashback;
    @FXML private DatePicker dpDateAchat;
    @FXML private DatePicker dpDateCredit;
    @FXML private DatePicker dpDateExpiration;
    @FXML private ComboBox<String> cmbStatut;
    @FXML private ComboBox<String> cmbPartenaire;

    @FXML private Button btnAjouter;
    @FXML private Button btnSupprimer;
    @FXML private Button btnDonnerReward;

    @FXML private TableView<Cashback> tableCashbacks;
    @FXML private TableColumn<Cashback, String> colUser;
    @FXML private TableColumn<Cashback, String> colPartenaire;
    @FXML private TableColumn<Cashback, String> colMontantAchat;
    @FXML private TableColumn<Cashback, String> colTaux;
    @FXML private TableColumn<Cashback, String> colMontantCashback;
    @FXML private TableColumn<Cashback, String> colDateAchat;
    @FXML private TableColumn<Cashback, String> colDateCredit;
    @FXML private TableColumn<Cashback, String> colDateExpiration;
    @FXML private TableColumn<Cashback, String> colUserRating;
    @FXML private TableColumn<Cashback, String> colBonusDecision;
    @FXML private TableColumn<Cashback, String> colStatut;
    @FXML private TableColumn<Cashback, Void> colActions;

    @FXML private TextField txtRecherche;
    @FXML private Label lblTableInfo;

    private final CashbackService cashbackService = new CashbackService();
    private final PartenaireService partenaireService = new PartenaireService();

    private final ObservableList<Cashback> cashbacksList = FXCollections.observableArrayList();
    private FilteredList<Cashback> filteredData;
    private Cashback selectedCashback;
    private boolean isEditMode;

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        initializeSearch();
        setupAutoCalculation();
        setupTableSelection();
        cmbPartenaire.setVisibleRowCount(8);
        cmbStatut.setVisibleRowCount(6);
        loadPartenaires();
        reloadCashbacks();
        updateRewardButtonState();
    }

    private void initializeTable() {
        colUser.setCellValueFactory(c -> {
            Cashback cashback = c.getValue();
            String displayName = cashback.getUserDisplayName() == null || cashback.getUserDisplayName().isBlank()
                    ? "User #" + cashback.getIdUser()
                    : cashback.getUserDisplayName().trim() + " (#" + cashback.getIdUser() + ")";
            return new SimpleStringProperty(displayName);
        });
        colPartenaire.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPartenaireNom()));
        colMontantAchat.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getMontantAchat())));
        colTaux.setCellValueFactory(c -> new SimpleStringProperty(String.format(Locale.US, "%.2f%%", c.getValue().getTauxApplique())));
        colMontantCashback.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getMontantCashback())));
        colDateAchat.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getDateAchat())));
        colDateCredit.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getDateCredit())));
        colDateExpiration.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getDateExpiration())));
        colUserRating.setCellValueFactory(c -> new SimpleStringProperty(formatUserRating(c.getValue().getUserRating())));
        colBonusDecision.setCellValueFactory(c -> new SimpleStringProperty(formatBonusDecision(c.getValue())));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));

        colMontantCashback.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText("+ " + item);
                setStyle("-fx-text-fill: #D4A82A; -fx-font-weight: 700;");
            }
        });

        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().add("nx-badge");
                switch (item) {
                    case "Credite" -> badge.getStyleClass().add("nx-badge-success");
                    case "Valide" -> badge.getStyleClass().add("nx-badge-info");
                    case "En attente" -> badge.getStyleClass().add("nx-badge-warning");
                    case "Expire", "Annule" -> badge.getStyleClass().add("nx-badge-error");
                    default -> { }
                }
                setGraphic(badge);
            }
        });

        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnReward = new Button();
            private final Button btnDelete = new Button();
            private final HBox hbox = new HBox(6);

            {
                btnEdit.getStyleClass().addAll("nx-table-action", "nx-table-action-edit");
                SVGPath editIcon = new SVGPath();
                editIcon.setContent("M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z");
                editIcon.getStyleClass().add("nx-action-icon");
                btnEdit.setGraphic(editIcon);

                btnReward.getStyleClass().addAll("nx-table-action", "nx-table-action-edit");
                SVGPath rewardIcon = new SVGPath();
                rewardIcon.setContent("M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8V7m0 1v8m0 0v1");
                rewardIcon.getStyleClass().add("nx-action-icon");
                btnReward.setGraphic(rewardIcon);
                btnReward.setTooltip(new Tooltip("Donner reward"));

                btnDelete.getStyleClass().addAll("nx-table-action", "nx-table-action-delete");
                SVGPath deleteIcon = new SVGPath();
                deleteIcon.setContent("M19 7l-.867 12.142A2 2 0 0 1 16.138 21H7.862a2 2 0 0 1-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v3M4 7h16");
                deleteIcon.getStyleClass().add("nx-action-icon");
                btnDelete.setGraphic(deleteIcon);

                hbox.setAlignment(Pos.CENTER);
                hbox.getChildren().addAll(btnEdit, btnReward, btnDelete);

                btnEdit.setOnAction(event -> editCashback(getTableView().getItems().get(getIndex())));
                btnReward.setOnAction(event -> grantReward(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(event -> deleteCashback(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void initializeSearch() {
        filteredData = new FilteredList<>(cashbacksList, c -> true);
        txtRecherche.textProperty().addListener((obs, old, newValue) -> {
            String filter = newValue == null ? "" : newValue.trim().toLowerCase();
            filteredData.setPredicate(cashback -> {
                if (filter.isEmpty()) {
                    return true;
                }
                String userToken = String.valueOf(cashback.getIdUser());
                String userName = cashback.getUserDisplayName() == null ? "" : cashback.getUserDisplayName().toLowerCase();
                return userToken.contains(filter)
                        || userName.contains(filter)
                        || safe(cashback.getPartenaireNom()).toLowerCase().contains(filter)
                        || safe(cashback.getStatut()).toLowerCase().contains(filter);
            });
            updateTableInfo();
        });

        SortedList<Cashback> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableCashbacks.comparatorProperty());
        tableCashbacks.setItems(sortedData);
    }

    private void setupAutoCalculation() {
        txtMontantAchat.textProperty().addListener((obs, old, value) -> calculateCashback());
        cmbPartenaire.valueProperty().addListener((obs, old, value) -> calculateCashback());
    }

    private void setupTableSelection() {
        tableCashbacks.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value == null) {
                updateRewardButtonState();
                return;
            }
            selectedCashback = value;
            populateForm(value);
            isEditMode = true;
            btnAjouter.setText("Modifier");
            updateRewardButtonState();
        });
    }

    private void loadPartenaires() {
        List<Partenaire> partenaires = partenaireService.getAllPartenaires();
        Set<String> names = new LinkedHashSet<>();
        for (Partenaire partenaire : partenaires) {
            if (partenaire.getNom() != null && !partenaire.getNom().isBlank()) {
                names.add(partenaire.getNom().trim());
            }
        }

        // Fallback when partner table is empty: keep admin UX usable.
        if (names.isEmpty()) {
            names.add("Carrefour");
            names.add("Zara");
            names.add("Technopolis");
            names.add("Pizza Hut");
            names.add("Tunisair");
        }

        cmbPartenaire.getItems().setAll(names);
    }

    private void reloadCashbacks() {
        cashbacksList.setAll(cashbackService.getAllCashbacks());
        updateStats();
        updateTableInfo();
    }

    private void calculateCashback() {
        try {
            double montant = parseDouble(txtMontantAchat.getText());
            if (montant <= 0) {
                txtTauxApplique.setText("0.00");
                txtMontantCashback.setText("0.00");
                return;
            }
            double rating = resolveSelectedPartnerRating();
            double taux = cashbackService.resolveEffectiveRatePercent(montant, rating);
            txtTauxApplique.setText(String.format(Locale.US, "%.2f", taux));
            txtMontantCashback.setText(String.format(Locale.US, "%.2f", cashbackService.calculateCashbackByAmountAndRating(montant, rating)));
        } catch (NumberFormatException ex) {
            txtTauxApplique.setText("0.00");
            txtMontantCashback.setText("0.00");
        }
    }

    private void updateStats() {
        lblTotalCashback.setText(String.format("%,.2f DT", cashbackService.getTotalCreditedGlobal()));
        lblNombreBeneficiaires.setText(String.valueOf(cashbackService.countActiveUsersWithCashback()));
        lblCashbackMois.setText(String.format("%,.2f DT", cashbackService.getCurrentMonthTotalGlobal()));
    }

    private void updateTableInfo() {
        int filteredCount = filteredData == null ? 0 : filteredData.size();
        lblTableInfo.setText(String.format("Affichage de %d sur %d entrees", filteredCount, cashbacksList.size()));
    }

    private void populateForm(Cashback cashback) {
        txtUserId.setText(String.valueOf(cashback.getIdUser()));
        txtMontantAchat.setText(String.valueOf(cashback.getMontantAchat()));
        txtTauxApplique.setText(String.valueOf(cashback.getTauxApplique()));
        txtMontantCashback.setText(String.valueOf(cashback.getMontantCashback()));
        dpDateAchat.setValue(cashback.getDateAchat());
        dpDateCredit.setValue(cashback.getDateCredit());
        dpDateExpiration.setValue(cashback.getDateExpiration());
        cmbStatut.setValue(cashback.getStatut());
        cmbPartenaire.setValue(cashback.getPartenaireNom());
    }

    private void clearForm() {
        txtUserId.clear();
        txtMontantAchat.clear();
        txtTauxApplique.clear();
        txtMontantCashback.setText("0.00");
        dpDateAchat.setValue(null);
        dpDateCredit.setValue(null);
        dpDateExpiration.setValue(null);
        cmbStatut.setValue(null);
        cmbPartenaire.setValue(null);

        selectedCashback = null;
        isEditMode = false;
        btnAjouter.setText("Ajouter");
        tableCashbacks.getSelectionModel().clearSelection();
        updateRewardButtonState();
    }

    @FXML
    private void handleAjouter() {
        if (!validateForm()) {
            return;
        }

        try {
            Cashback cashback = buildFromForm();
            if (isEditMode && selectedCashback != null) {
                cashback.setIdCashback(selectedCashback.getIdCashback());
                if (!cashbackService.updateCashbackAsAdmin(cashback)) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Mise a jour impossible.");
                    return;
                }
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Cashback modifie avec succes.");
            } else {
                int createdId = cashbackService.createCashback(cashback);
                if (createdId <= 0) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Creation impossible.");
                    return;
                }
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Cashback ajoute avec succes.");
            }

            clearForm();
            reloadCashbacks();
        } catch (RuntimeException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        if (selectedCashback == null) {
            showAlert(Alert.AlertType.WARNING, "Avertissement", "Selectionnez un cashback.");
            return;
        }
        deleteCashback(selectedCashback);
    }

    @FXML
    private void handleAnnuler() {
        clearForm();
    }

    @FXML
    private void handleDonnerReward() {
        Cashback target = selectedCashback;
        if (target == null && tableCashbacks != null) {
            target = tableCashbacks.getSelectionModel().getSelectedItem();
        }
        if (target == null) {
            showAlert(Alert.AlertType.WARNING, "Reward", "Selectionnez un cashback dans le tableau.");
            return;
        }
        grantReward(target);
    }

    private Cashback buildFromForm() {
        Cashback cashback = new Cashback();
        cashback.setIdUser(Integer.parseInt(txtUserId.getText().trim()));
        cashback.setPartenaireNom(cmbPartenaire.getValue() == null ? "" : cmbPartenaire.getValue().trim());

        Optional<Partenaire> partenaireOpt = partenaireService.findByName(cashback.getPartenaireNom());
        cashback.setIdPartenaire(partenaireOpt.map(Partenaire::getIdPartenaire).orElse(null));

        double montant = parseDouble(txtMontantAchat.getText());
        double rating = partenaireOpt.map(Partenaire::getRating).orElse(0.0);
        cashback.setMontantAchat(montant);
        cashback.setTauxApplique(cashbackService.resolveEffectiveRatePercent(montant, rating));
        cashback.setMontantCashback(cashbackService.calculateCashbackByAmountAndRating(montant, rating));
        cashback.setDateAchat(dpDateAchat.getValue());
        cashback.setDateCredit(dpDateCredit.getValue());
        cashback.setDateExpiration(dpDateExpiration.getValue());
        cashback.setStatut(cmbStatut.getValue());
        cashback.setTransactionRef("");
        return cashback;
    }

    private void editCashback(Cashback cashback) {
        selectedCashback = cashback;
        populateForm(cashback);
        isEditMode = true;
        btnAjouter.setText("Modifier");
    }

    private void grantReward(Cashback cashback) {
        if (cashback == null) {
            return;
        }
        if (cashback.getUserRating() == null) {
            showAlert(Alert.AlertType.WARNING, "Decision bonus", "L utilisateur n a pas encore note ce cashback.");
            return;
        }

        Alert decisionDialog = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Approuver un bonus pour ce cashback ?\nOui = bonus accorde, Non = pas de bonus.",
            ButtonType.YES,
            ButtonType.NO,
            ButtonType.CANCEL
        );
        decisionDialog.setTitle("Decision bonus");
        String comment = safe(cashback.getUserRatingComment());
        String header = "Notation utilisateur: " + formatUserRating(cashback.getUserRating());
        if (!comment.isBlank()) {
            header += "\nCommentaire: " + comment;
        }
        decisionDialog.setHeaderText(header);
        Optional<ButtonType> decisionResult = decisionDialog.showAndWait();
        if (decisionResult.isEmpty() || decisionResult.get() == ButtonType.CANCEL) {
            return;
        }

        if (decisionResult.get() == ButtonType.NO) {
            TextInputDialog rejectReasonDialog = new TextInputDialog("Pas de bonus sur cette transaction");
            rejectReasonDialog.setTitle("Decision bonus");
            rejectReasonDialog.setHeaderText("Raison du refus");
            rejectReasonDialog.setContentText("Raison:");
            Optional<String> rejectReasonResult = rejectReasonDialog.showAndWait();
            if (rejectReasonResult.isEmpty()) {
                return;
            }
            String reason = rejectReasonResult.get();

            try {
                boolean updated = cashbackService.setAdminBonusDecision(cashback.getIdCashback(), false, reason);
                if (!updated) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d enregistrer le refus du bonus.");
                    return;
                }
                reloadCashbacks();
                showAlert(Alert.AlertType.INFORMATION, "Decision enregistree", "Bonus refuse pour cette transaction.");
            } catch (RuntimeException ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
            }
            return;
        }

        TextInputDialog amountDialog = new TextInputDialog("5.00");
        amountDialog.setTitle("Reward Admin");
        amountDialog.setHeaderText("Accorder un bonus cashback");
        amountDialog.setContentText("Montant bonus (DT):");
        Optional<String> bonusResult = amountDialog.showAndWait();
        if (bonusResult.isEmpty()) {
            return;
        }

        double bonus;
        try {
            bonus = parseDouble(bonusResult.get());
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Montant bonus invalide.");
            return;
        }
        if (bonus <= 0) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Le bonus doit etre superieur a 0.");
            return;
        }

        TextInputDialog reasonDialog = new TextInputDialog("Bonus fidelite");
        reasonDialog.setTitle("Reward Admin");
        reasonDialog.setHeaderText("Raison du bonus (optionnel)");
        reasonDialog.setContentText("Raison:");
        Optional<String> reasonResult = reasonDialog.showAndWait();
        if (reasonResult.isEmpty()) {
            return;
        }
        String reason = reasonResult.get();

        try {
            boolean granted = cashbackService.grantAdminReward(cashback.getIdCashback(), bonus, reason);
            if (!granted) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d appliquer le reward.");
                return;
            }
            reloadCashbacks();
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Reward applique: +" + String.format(Locale.US, "%.2f", bonus) + " DT");
        } catch (RuntimeException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }

    private void deleteCashback(Cashback cashback) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer ce cashback ?",
                ButtonType.OK,
                ButtonType.CANCEL);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        boolean deleted = cashbackService.deleteCashbackAsAdmin(cashback.getIdCashback());
        if (!deleted) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible.");
            return;
        }

        clearForm();
        reloadCashbacks();
        updateRewardButtonState();
    }

    private boolean validateForm() {
        if (txtUserId.getText().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "L identifiant utilisateur est obligatoire.");
            return false;
        }
        if (cmbPartenaire.getValue() == null || cmbPartenaire.getValue().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Selectionnez un partenaire.");
            return false;
        }
        if (txtMontantAchat.getText().isBlank() || txtTauxApplique.getText().isBlank() || txtMontantCashback.getText().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Les champs montant/taux sont obligatoires.");
            return false;
        }
        if (dpDateAchat.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Validation", "La date d achat est obligatoire.");
            return false;
        }
        if (cmbStatut.getValue() == null || cmbStatut.getValue().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Le statut est obligatoire.");
            return false;
        }

        try {
            Integer.parseInt(txtUserId.getText().trim());
            parseDouble(txtMontantAchat.getText());
            parseDouble(txtTauxApplique.getText());
            parseDouble(txtMontantCashback.getText());
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Valeurs numeriques invalides.");
            return false;
        }

        return true;
    }

    @FXML
    private void trierParDateAchat() {
        cashbacksList.sort(Comparator.comparing(Cashback::getDateAchat, Comparator.nullsLast(LocalDate::compareTo)).reversed());
    }

    @FXML
    private void trierParMontant() {
        cashbacksList.sort(Comparator.comparing(Cashback::getMontantAchat).reversed());
    }

    @FXML
    private void trierParCashback() {
        cashbacksList.sort(Comparator.comparing(Cashback::getMontantCashback).reversed());
    }

    @FXML
    private void trierParStatut() {
        cashbacksList.sort(Comparator.comparing(c -> safe(c.getStatut())));
    }

    @FXML
    private void exporterPDF() {
        showAlert(Alert.AlertType.INFORMATION, "Export", "Export PDF en developpement.");
    }

    @FXML
    private void envoyerSMS() {
        showAlert(Alert.AlertType.INFORMATION, "SMS", "Envoi SMS en developpement.");
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DISPLAY_DATE_FORMAT);
    }

    private double parseDouble(String text) {
        return Double.parseDouble(text == null ? "0" : text.trim().replace(",", "."));
    }

    private double resolveSelectedPartnerRating() {
        String partnerName = cmbPartenaire.getValue();
        if (partnerName == null || partnerName.isBlank()) {
            return 0.0;
        }
        return partenaireService.findByName(partnerName).map(Partenaire::getRating).orElse(0.0);
    }

    private String formatUserRating(Double rating) {
        if (rating == null) {
            return "\u2606\u2606\u2606\u2606\u2606";
        }
        return toStars(rating) + " (" + String.format(Locale.US, "%.1f/5", rating) + ")";
    }

    private String toStars(double rating) {
        int rounded = (int) Math.round(rating);
        rounded = Math.max(0, Math.min(5, rounded));
        StringBuilder stars = new StringBuilder(5);
        for (int i = 1; i <= 5; i++) {
            stars.append(i <= rounded ? "\u2605" : "\u2606");
        }
        return stars.toString();
    }

    private String formatBonusDecision(Cashback cashback) {
        if (cashback == null) {
            return "Pending";
        }
        String decision = safe(cashback.getBonusDecision());
        if (decision.isBlank()) {
            decision = "Pending";
        }
        String note = safe(cashback.getBonusNote());
        if (note.isBlank()) {
            return decision;
        }
        return decision + " (" + note + ")";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void updateRewardButtonState() {
        if (btnDonnerReward == null || tableCashbacks == null) {
            return;
        }
        Cashback current = selectedCashback;
        if (current == null) {
            current = tableCashbacks.getSelectionModel().getSelectedItem();
        }
        btnDonnerReward.setDisable(current == null);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}


