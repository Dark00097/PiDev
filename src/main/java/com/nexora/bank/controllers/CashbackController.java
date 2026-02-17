package com.nexora.bank.controllers;

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
import java.util.Comparator;
import java.util.Optional;
import java.util.ResourceBundle;

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

    private ObservableList<Cashback> cashbacksList = FXCollections.observableArrayList();
    private FilteredList<Cashback> filteredData;
    private Cashback selectedCashback = null;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        initializeSearch();
        loadSampleData();
        updateStats();
        setupTableSelection();
        setupAutoCalculation();
    }

    private void setupAutoCalculation() {
        // Auto-calculate cashback amount
        txtMontantAchat.textProperty().addListener((obs, old, newVal) -> calculateCashback());
        txtTauxApplique.textProperty().addListener((obs, old, newVal) -> calculateCashback());
    }

    private void calculateCashback() {
        try {
            double montant = Double.parseDouble(txtMontantAchat.getText().isEmpty() ? "0" : txtMontantAchat.getText());
            double taux = Double.parseDouble(txtTauxApplique.getText().isEmpty() ? "0" : txtTauxApplique.getText());
            double cashback = montant * taux / 100;
            txtMontantCashback.setText(String.format("%.2f", cashback));
        } catch (NumberFormatException e) {
            txtMontantCashback.setText("0.00");
        }
    }

    private void initializeTable() {
        colPartenaire.setCellValueFactory(new PropertyValueFactory<>("partenaire"));
        colMontantAchat.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getMontantAchat())));
        colTaux.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTauxApplique() + "%"));
        colMontantCashback.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getMontantCashback())));
        colDateAchat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateAchat().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        colDateCredit.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getDateCredit() != null ? c.getValue().getDateCredit().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-"));
        colDateExpiration.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getDateExpiration() != null ? c.getValue().getDateExpiration().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-"));
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
                return cashback.getPartenaire().toLowerCase().contains(filter) ||
                       cashback.getStatut().toLowerCase().contains(filter);
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

    private void loadSampleData() {
        cashbacksList.addAll(
            new Cashback("Carrefour", 250.00, 3.0, 7.50, LocalDate.now().minusDays(5), LocalDate.now().minusDays(2), LocalDate.now().plusMonths(6), "Credite"),
            new Cashback("Zara", 450.00, 5.0, 22.50, LocalDate.now().minusDays(3), LocalDate.now(), LocalDate.now().plusMonths(6), "Credite"),
            new Cashback("Pizza Hut", 85.00, 10.0, 8.50, LocalDate.now().minusDays(1), null, LocalDate.now().plusMonths(6), "Valide"),
            new Cashback("Technopolis", 1200.00, 2.0, 24.00, LocalDate.now(), null, LocalDate.now().plusMonths(6), "En attente"),
            new Cashback("Tunisair", 850.00, 1.5, 12.75, LocalDate.now().minusMonths(1), LocalDate.now().minusDays(20), LocalDate.now().minusDays(10), "Expire"),
            new Cashback("Carrefour", 180.00, 3.0, 5.40, LocalDate.now().minusDays(10), LocalDate.now().minusDays(7), LocalDate.now().plusMonths(5), "Credite"),
            new Cashback("Decathlon", 320.00, 4.0, 12.80, LocalDate.now().minusDays(12), LocalDate.now().minusDays(9), LocalDate.now().plusMonths(6), "Credite"),
            new Cashback("Orange", 60.00, 6.0, 3.60, LocalDate.now().minusDays(2), null, LocalDate.now().plusMonths(6), "Valide"),
            new Cashback("Marriott", 540.00, 2.5, 13.50, LocalDate.now().minusDays(14), LocalDate.now().minusDays(11), LocalDate.now().plusMonths(6), "Credite")
        );
        updateTableInfo();
    }

    private void updateStats() {
        double totalCashback = cashbacksList.stream()
            .filter(c -> c.getStatut().equals("Credite"))
            .mapToDouble(Cashback::getMontantCashback).sum();
        
        long beneficiaires = cashbacksList.stream()
            .filter(c -> c.getStatut().equals("Credite") || c.getStatut().equals("Valide"))
            .count();
        
        double cashbackMois = cashbacksList.stream()
            .filter(c -> c.getDateAchat().getMonth() == LocalDate.now().getMonth())
            .mapToDouble(Cashback::getMontantCashback).sum();

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
        dpDateAchat.setValue(cashback.getDateAchat());
        dpDateCredit.setValue(cashback.getDateCredit());
        dpDateExpiration.setValue(cashback.getDateExpiration());
        cmbStatut.setValue(cashback.getStatut());
        cmbPartenaire.setValue(cashback.getPartenaire());
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
            String partenaire = cmbPartenaire.getValue() != null ? cmbPartenaire.getValue() : "Non specifie";
            double montantAchat = Double.parseDouble(txtMontantAchat.getText());
            double taux = Double.parseDouble(txtTauxApplique.getText());
            double montantCashback = Double.parseDouble(txtMontantCashback.getText());
            LocalDate dateAchat = dpDateAchat.getValue();
            LocalDate dateCredit = dpDateCredit.getValue();
            LocalDate dateExpiration = dpDateExpiration.getValue();
            String statut = cmbStatut.getValue();

            if (isEditMode && selectedCashback != null) {
                selectedCashback.setPartenaire(partenaire);
                selectedCashback.setMontantAchat(montantAchat);
                selectedCashback.setTauxApplique(taux);
                selectedCashback.setMontantCashback(montantCashback);
                selectedCashback.setDateAchat(dateAchat);
                selectedCashback.setDateCredit(dateCredit);
                selectedCashback.setDateExpiration(dateExpiration);
                selectedCashback.setStatut(statut);
                tableCashbacks.refresh();
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Cashback modifie avec succes!");
            } else {
                Cashback newCashback = new Cashback(partenaire, montantAchat, taux, montantCashback, 
                    dateAchat, dateCredit, dateExpiration, statut);
                cashbacksList.add(newCashback);
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Cashback ajoute avec succes!");
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce cashbackx", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            cashbacksList.remove(cashback);
            clearForm();
            updateStats();
            updateTableInfo();
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

    @FXML private void trierParDateAchat() { cashbacksList.sort(Comparator.comparing(Cashback::getDateAchat).reversed()); }
    @FXML private void trierParMontant() { cashbacksList.sort(Comparator.comparing(Cashback::getMontantAchat).reversed()); }
    @FXML private void trierParCashback() { cashbacksList.sort(Comparator.comparing(Cashback::getMontantCashback).reversed()); }
    @FXML private void trierParStatut() { cashbacksList.sort(Comparator.comparing(Cashback::getStatut)); }
    @FXML private void exporterPDF() { showAlert(Alert.AlertType.INFORMATION, "Export", "En developpement."); }
    @FXML private void envoyerSMS() { showAlert(Alert.AlertType.INFORMATION, "SMS", "En developpement."); }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    // Inner class
    public static class Cashback {
        private String partenaire, statut;
        private double montantAchat, tauxApplique, montantCashback;
        private LocalDate dateAchat, dateCredit, dateExpiration;

        public Cashback(String partenaire, double montantAchat, double tauxApplique, double montantCashback,
                       LocalDate dateAchat, LocalDate dateCredit, LocalDate dateExpiration, String statut) {
            this.partenaire = partenaire;
            this.montantAchat = montantAchat;
            this.tauxApplique = tauxApplique;
            this.montantCashback = montantCashback;
            this.dateAchat = dateAchat;
            this.dateCredit = dateCredit;
            this.dateExpiration = dateExpiration;
            this.statut = statut;
        }

        // Getters and Setters
        public String getPartenaire() { return partenaire; }
        public void setPartenaire(String partenaire) { this.partenaire = partenaire; }
        public double getMontantAchat() { return montantAchat; }
        public void setMontantAchat(double montantAchat) { this.montantAchat = montantAchat; }
        public double getTauxApplique() { return tauxApplique; }
        public void setTauxApplique(double tauxApplique) { this.tauxApplique = tauxApplique; }
        public double getMontantCashback() { return montantCashback; }
        public void setMontantCashback(double montantCashback) { this.montantCashback = montantCashback; }
        public LocalDate getDateAchat() { return dateAchat; }
        public void setDateAchat(LocalDate dateAchat) { this.dateAchat = dateAchat; }
        public LocalDate getDateCredit() { return dateCredit; }
        public void setDateCredit(LocalDate dateCredit) { this.dateCredit = dateCredit; }
        public LocalDate getDateExpiration() { return dateExpiration; }
        public void setDateExpiration(LocalDate dateExpiration) { this.dateExpiration = dateExpiration; }
        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }
    }
}
