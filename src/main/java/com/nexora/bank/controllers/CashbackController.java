package com.nexora.bank.controllers;

import com.nexora.bank.Models.Cashback;
import com.nexora.bank.Models.Partenaire;
import com.nexora.bank.Service.CashbackService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.geometry.Pos;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Admin controller for managing cashback records.
 * Uses the shared CashbackService for database operations and calculations.
 */
public class CashbackController implements Initializable {

    @FXML private Label lblTotalCashback;
    @FXML private Label lblNombreBeneficiaires;
    @FXML private Label lblCashbackMois;

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
    @FXML private Button btnAnnuler;

    @FXML private TableView<Cashback> tableCashbacks;
    @FXML private TableColumn<Cashback, String> colPartenaire;
    @FXML private TableColumn<Cashback, String> colMontantAchat;
    @FXML private TableColumn<Cashback, String> colTaux;
    @FXML private TableColumn<Cashback, String> colMontantCashback;
    @FXML private TableColumn<Cashback, String> colDateAchat;
    @FXML private TableColumn<Cashback, String> colDateCredit;
    @FXML private TableColumn<Cashback, String> colDateExpiration;
    @FXML private TableColumn<Cashback, String> colStatut;
    @FXML private TableColumn<Cashback, Void> colActions;

    @FXML private TextField txtRecherche;
    @FXML private Label lblTableInfo;

    private final CashbackService cashbackService = new CashbackService();
    private ObservableList<Cashback> cashbacksList = FXCollections.observableArrayList();
    private FilteredList<Cashback> filteredData;
    private Cashback selectedCashback = null;
    private boolean isEditMode = false;

    // Map partner names to IDs for the combo box
    private List<Partenaire> partners;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        initializeSearch();
        loadData();
        updateStats();
        setupTableSelection();
        setupAutoCalculation();
        loadPartnerCombo();
    }

    private void setupAutoCalculation() {
        txtMontantAchat.textProperty().addListener((obs, old, newVal) -> calculateCashback());
        txtTauxApplique.textProperty().addListener((obs, old, newVal) -> calculateCashback());
    }

    private void calculateCashback() {
        try {
            double montant = Double.parseDouble(txtMontantAchat.getText().isEmpty() ? "0" : txtMontantAchat.getText());
            double taux = Double.parseDouble(txtTauxApplique.getText().isEmpty() ? "0" : txtTauxApplique.getText());
            double cashback = CashbackService.calculateCashbackAmount(montant, taux);
            txtMontantCashback.setText(String.format("%.2f", cashback));
        } catch (NumberFormatException e) {
            txtMontantCashback.setText("0.00");
        }
    }

    private void loadPartnerCombo() {
        partners = cashbackService.getAllPartners();
        if (cmbPartenaire != null) {
            ObservableList<String> partnerNames = FXCollections.observableArrayList();
            for (Partenaire p : partners) {
                partnerNames.add(p.getNom());
            }
            cmbPartenaire.setItems(partnerNames);
        }
    }

    private int getPartnerIdByName(String name) {
        if (partners != null && name != null) {
            for (Partenaire p : partners) {
                if (p.getNom().equals(name)) return p.getIdPartenaire();
            }
        }
        return 0;
    }

    private String getPartnerNameById(int id) {
        if (partners != null) {
            for (Partenaire p : partners) {
                if (p.getIdPartenaire() == id) return p.getNom();
            }
        }
        return cashbackService.getPartnerNameById(id);
    }

    private void initializeTable() {
        colPartenaire.setCellValueFactory(c -> {
            String name = c.getValue().getPartenaireNom();
            if (name == null || name.isEmpty()) {
                name = getPartnerNameById(c.getValue().getIdPartenaire());
            }
            return new SimpleStringProperty(name);
        });
        colMontantAchat.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getMontantAchat())));
        colTaux.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTauxApplique() + "%"));
        colMontantCashback.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getMontantCashback())));
        colDateAchat.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getDateAchat())));
        colDateCredit.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getDateCredit() != null ? formatDate(c.getValue().getDateCredit()) : "-"));
        colDateExpiration.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getDateExpiration() != null ? formatDate(c.getValue().getDateExpiration()) : "-"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Cashback amount with gold styling
        colMontantCashback.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("+ " + item);
                    setStyle("-fx-text-fill: #D4A82A; -fx-font-weight: 700;");
                }
            }
        });

        // Status column with badge
        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("nx-badge");
                    switch (item) {
                        case "Credite": badge.getStyleClass().add("nx-badge-success"); break;
                        case "Valide": badge.getStyleClass().add("nx-badge-info"); break;
                        case "En attente": badge.getStyleClass().add("nx-badge-warning"); break;
                        case "Expire": case "Annule": badge.getStyleClass().add("nx-badge-error"); break;
                    }
                    setGraphic(badge);
                }
            }
        });

        // Actions column
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox hbox = new HBox(8);

            {
                btnEdit.getStyleClass().addAll("nx-table-action", "nx-table-action-edit");
                SVGPath editIcon = new SVGPath();
                editIcon.setContent("M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z");
                editIcon.getStyleClass().add("nx-action-icon");
                btnEdit.setGraphic(editIcon);

                btnDelete.getStyleClass().addAll("nx-table-action", "nx-table-action-delete");
                SVGPath deleteIcon = new SVGPath();
                deleteIcon.setContent("M19 7l-.867 12.142A2 2 0 0 1 16.138 21H7.862a2 2 0 0 1-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v3M4 7h16");
                deleteIcon.getStyleClass().add("nx-action-icon");
                btnDelete.setGraphic(deleteIcon);

                hbox.setAlignment(Pos.CENTER);
                hbox.getChildren().addAll(btnEdit, btnDelete);

                btnEdit.setOnAction(event -> editCashback(getTableView().getItems().get(getIndex())));
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
        filteredData = new FilteredList<>(cashbacksList, p -> true);
        txtRecherche.textProperty().addListener((obs, old, newVal) -> {
            filteredData.setPredicate(cashback -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String filter = newVal.toLowerCase();
                String partnerName = cashback.getPartenaireNom() != null
                    ? cashback.getPartenaireNom() : getPartnerNameById(cashback.getIdPartenaire());
                return (partnerName != null && partnerName.toLowerCase().contains(filter)) ||
                       (cashback.getStatut() != null && cashback.getStatut().toLowerCase().contains(filter));
            });
            updateTableInfo();
        });
        SortedList<Cashback> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableCashbacks.comparatorProperty());
        tableCashbacks.setItems(sortedData);
    }

    private void setupTableSelection() {
        tableCashbacks.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectedCashback = newVal;
                populateForm(newVal);
                isEditMode = true;
                btnAjouter.setText("Modifier");
            }
        });
    }

    private void loadData() {
        cashbacksList.clear();
        List<Cashback> dbCashbacks = cashbackService.getAllCashbacks();
        cashbacksList.addAll(dbCashbacks);
        updateTableInfo();
    }

    private void updateStats() {
        double totalCashback = cashbackService.getTotalCashbackAll();
        long beneficiaires = cashbackService.getBeneficiaryCount();
        double cashbackMois = cashbackService.getCurrentMonthCashbackAll();

        lblTotalCashback.setText(String.format("%,.2f DT", totalCashback));
        lblNombreBeneficiaires.setText(String.valueOf(beneficiaires));
        lblCashbackMois.setText(String.format("%,.2f DT", cashbackMois));
    }

    private void updateTableInfo() {
        lblTableInfo.setText(String.format("Affichage de %d sur %d entrees", filteredData.size(), cashbacksList.size()));
    }

    private void populateForm(Cashback cashback) {
        txtMontantAchat.setText(String.valueOf(cashback.getMontantAchat()));
        txtTauxApplique.setText(String.valueOf(cashback.getTauxApplique()));
        txtMontantCashback.setText(String.valueOf(cashback.getMontantCashback()));
        dpDateAchat.setValue(parseDate(cashback.getDateAchat()));
        dpDateCredit.setValue(parseDate(cashback.getDateCredit()));
        dpDateExpiration.setValue(parseDate(cashback.getDateExpiration()));
        cmbStatut.setValue(cashback.getStatut());
        String partnerName = cashback.getPartenaireNom() != null
            ? cashback.getPartenaireNom() : getPartnerNameById(cashback.getIdPartenaire());
        cmbPartenaire.setValue(partnerName);
    }

    private void clearForm() {
        txtMontantAchat.clear();
        txtTauxApplique.clear();
        txtMontantCashback.clear();
        dpDateAchat.setValue(null);
        dpDateCredit.setValue(null);
        dpDateExpiration.setValue(null);
        cmbStatut.setValue(null);
        cmbPartenaire.setValue(null);
        selectedCashback = null;
        isEditMode = false;
        btnAjouter.setText("Ajouter");
        tableCashbacks.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleAjouter() {
        if (!validateForm()) return;

        try {
            String partenaireNom = cmbPartenaire.getValue() != null ? cmbPartenaire.getValue() : "Non specifie";
            int idPartenaire = getPartnerIdByName(partenaireNom);
            double montantAchat = Double.parseDouble(txtMontantAchat.getText());
            double taux = Double.parseDouble(txtTauxApplique.getText());
            double montantCashback = Double.parseDouble(txtMontantCashback.getText());
            String dateAchat = dpDateAchat.getValue() != null ? dpDateAchat.getValue().toString() : null;
            String dateCredit = dpDateCredit.getValue() != null ? dpDateCredit.getValue().toString() : null;
            String dateExpiration = dpDateExpiration.getValue() != null ? dpDateExpiration.getValue().toString() : null;
            String statut = cmbStatut.getValue();

            if (isEditMode && selectedCashback != null) {
                selectedCashback.setIdPartenaire(idPartenaire);
                selectedCashback.setMontantAchat(montantAchat);
                selectedCashback.setTauxApplique(taux);
                selectedCashback.setMontantCashback(montantCashback);
                selectedCashback.setDateAchat(dateAchat);
                selectedCashback.setDateCredit(dateCredit);
                selectedCashback.setDateExpiration(dateExpiration);
                selectedCashback.setStatut(statut);
                selectedCashback.setPartenaireNom(partenaireNom);

                if (cashbackService.updateCashback(selectedCashback)) {
                    tableCashbacks.refresh();
                    showAlert(Alert.AlertType.INFORMATION, "Succes", "Cashback modifie avec succes!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Echec de la modification.");
                }
            } else {
                Cashback newCashback = new Cashback(idPartenaire, montantAchat, taux, montantCashback,
                    dateAchat, dateCredit, dateExpiration, statut);
                newCashback.setPartenaireNom(partenaireNom);

                if (cashbackService.addCashback(newCashback)) {
                    cashbacksList.add(newCashback);
                    showAlert(Alert.AlertType.INFORMATION, "Succes", "Cashback ajoute avec succes!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Echec de l'ajout.");
                }
            }

            clearForm();
            updateStats();
            updateTableInfo();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Valeurs numeriques invalides.");
        }
    }

    @FXML private void handleSupprimer() {
        if (selectedCashback != null) deleteCashback(selectedCashback);
        else showAlert(Alert.AlertType.WARNING, "Avertissement", "Selectionnez un cashback.");
    }

    @FXML private void handleAnnuler() { clearForm(); }

    private void editCashback(Cashback cashback) {
        selectedCashback = cashback;
        populateForm(cashback);
        isEditMode = true;
        btnAjouter.setText("Modifier");
    }

    private void deleteCashback(Cashback cashback) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce cashback?", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (cashbackService.deleteCashback(cashback.getIdCashback())) {
                cashbacksList.remove(cashback);
                clearForm();
                updateStats();
                updateTableInfo();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Echec de la suppression.");
            }
        }
    }

    private boolean validateForm() {
        if (txtMontantAchat.getText().isEmpty() || txtTauxApplique.getText().isEmpty() ||
            dpDateAchat.getValue() == null || cmbStatut.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Remplissez tous les champs obligatoires.");
            return false;
        }
        return true;
    }

    @FXML private void trierParDateAchat() {
        cashbacksList.sort(Comparator.comparing(Cashback::getDateAchat, Comparator.nullsLast(Comparator.reverseOrder())));
    }
    @FXML private void trierParMontant() {
        cashbacksList.sort(Comparator.comparing(Cashback::getMontantAchat).reversed());
    }
    @FXML private void trierParCashback() {
        cashbacksList.sort(Comparator.comparing(Cashback::getMontantCashback).reversed());
    }
    @FXML private void trierParStatut() {
        cashbacksList.sort(Comparator.comparing(Cashback::getStatut, Comparator.nullsLast(Comparator.naturalOrder())));
    }
    @FXML private void exporterPDF() { showAlert(Alert.AlertType.INFORMATION, "Export", "En developpement."); }
    @FXML private void envoyerSMS() { showAlert(Alert.AlertType.INFORMATION, "SMS", "En developpement."); }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // ═══════════════════ DATE HELPERS ═══════════════════

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "-";
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException e) {
            return dateStr;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
