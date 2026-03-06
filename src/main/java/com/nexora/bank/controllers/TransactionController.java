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

public class TransactionController implements Initializable {

    // Stat Labels
    @FXML private Label lblTotalTransactions;
    @FXML private Label lblMontantTotal;
    @FXML private Label lblTransactionsJour;

    // Form Fields
    @FXML private ComboBox<String> cmbCategorie;
    @FXML private DatePicker dpDateTransaction;
    @FXML private TextField txtMontant;
    @FXML private ComboBox<String> cmbTypeTransaction;
    @FXML private ComboBox<String> cmbStatutTransaction;
    @FXML private TextField txtSoldeApres;
    @FXML private TextArea txtDescription;

    // Buttons
    @FXML private Button btnAjouter;
    @FXML private Button btnSupprimer;
    @FXML private Button btnAnnuler;

    // Table
    @FXML private TableView<Transaction> tableTransactions;
    @FXML private TableColumn<Transaction, String> colCategorie;
    @FXML private TableColumn<Transaction, String> colDate;
    @FXML private TableColumn<Transaction, String> colMontant;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, String> colStatut;
    @FXML private TableColumn<Transaction, String> colSoldeApres;
    @FXML private TableColumn<Transaction, String> colDescription;
    @FXML private TableColumn<Transaction, Void> colActions;

    // Search and Info
    @FXML private TextField txtRecherche;
    @FXML private Label lblTableInfo;

    private ObservableList<Transaction> transactionsList = FXCollections.observableArrayList();
    private FilteredList<Transaction> filteredData;
    private Transaction selectedTransaction = null;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        initializeSearch();
        loadSampleData();
        updateStats();
        setupTableSelection();
    }

    private void initializeTable() {
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colDate.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDateTransaction().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        colMontant.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f DT", cellData.getValue().getMontant())));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeTransaction"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutTransaction"));
        colSoldeApres.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f DT", cellData.getValue().getSoldeApres())));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Type column with styling
        colType.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("nx-badge");
                    if (item.equals("Credit")) {
                        badge.getStyleClass().add("nx-badge-success");
                    } else {
                        badge.getStyleClass().add("nx-badge-error");
                    }
                    setGraphic(badge);
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
                        case "Valide": badge.getStyleClass().add("nx-badge-success"); break;
                        case "En attente": badge.getStyleClass().add("nx-badge-warning"); break;
                        case "Echouee": badge.getStyleClass().add("nx-badge-error"); break;
                    }
                    setGraphic(badge);
                }
            }
        });

        // Montant with color based on type
        colMontant.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    Transaction trans = getTableView().getItems().get(getIndex());
                    String prefix = trans.getTypeTransaction().equals("Credit") ? "+ " : "- ";
                    setText(prefix + item);
                    if (trans.getTypeTransaction().equals("Credit")) {
                        setStyle("-fx-text-fill: #10B981; -fx-font-weight: 600;");
                    } else {
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: 600;");
                    }
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

                btnEdit.setOnAction(event -> editTransaction(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(event -> deleteTransaction(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void initializeSearch() {
        filteredData = new FilteredList<>(transactionsList, p -> true);
        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(trans -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return trans.getCategorie().toLowerCase().contains(lowerCaseFilter) ||
                       trans.getTypeTransaction().toLowerCase().contains(lowerCaseFilter) ||
                       trans.getStatutTransaction().toLowerCase().contains(lowerCaseFilter) ||
                       trans.getDescription().toLowerCase().contains(lowerCaseFilter);
            });
            updateTableInfo();
        });
        SortedList<Transaction> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableTransactions.comparatorProperty());
        tableTransactions.setItems(sortedData);
    }

    private void setupTableSelection() {
        tableTransactions.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedTransaction = newSel;
                populateForm(newSel);
                isEditMode = true;
                btnAjouter.setText("Modifier");
            }
        });
    }

    private void loadSampleData() {
        transactionsList.addAll(
            new Transaction("Virement", LocalDate.now(), 1500.00, "Debit", "Valide", 8500.00, "Virement vers compte epargne"),
            new Transaction("Paiement", LocalDate.now().minusDays(1), 250.00, "Debit", "Valide", 10000.00, "Paiement facture electricite"),
            new Transaction("Retrait", LocalDate.now().minusDays(2), 500.00, "Debit", "Valide", 10250.00, "Retrait DAB"),
            new Transaction("Virement", LocalDate.now().minusDays(3), 3000.00, "Credit", "Valide", 10750.00, "Reception salaire"),
            new Transaction("Paiement", LocalDate.now(), 150.00, "Debit", "En attente", 8350.00, "Paiement en ligne"),
            new Transaction("Virement", LocalDate.now().minusDays(5), 200.00, "Debit", "Echouee", 7750.00, "Virement international"),
            new Transaction("Paiement", LocalDate.now().minusDays(7), 320.00, "Debit", "Valide", 7200.00, "Abonnement Internet"),
            new Transaction("Virement", LocalDate.now().minusDays(10), 2200.00, "Credit", "Valide", 9500.00, "Remboursement client"),
            new Transaction("Retrait", LocalDate.now().minusDays(12), 400.00, "Debit", "Valide", 6800.00, "Retrait agence"),
            new Transaction("Paiement", LocalDate.now().minusDays(15), 90.00, "Debit", "Echouee", 7600.00, "Paiement carte étrangère")
        );
        updateTableInfo();
    }

    private void updateStats() {
        int totalTrans = transactionsList.size();
        double montantTotal = transactionsList.stream().mapToDouble(Transaction::getMontant).sum();
        long transJour = transactionsList.stream()
            .filter(t -> t.getDateTransaction().equals(LocalDate.now())).count();

        lblTotalTransactions.setText(String.valueOf(totalTrans));
        lblMontantTotal.setText(String.format("%,.2f DT", montantTotal));
        lblTransactionsJour.setText(String.valueOf(transJour));
    }

    private void updateTableInfo() {
        lblTableInfo.setText(String.format("Affichage de %d sur %d entrees", filteredData.size(), transactionsList.size()));
    }

    private void populateForm(Transaction trans) {
        cmbCategorie.setValue(trans.getCategorie());
        dpDateTransaction.setValue(trans.getDateTransaction());
        txtMontant.setText(String.valueOf(trans.getMontant()));
        cmbTypeTransaction.setValue(trans.getTypeTransaction());
        cmbStatutTransaction.setValue(trans.getStatutTransaction());
        txtSoldeApres.setText(String.valueOf(trans.getSoldeApres()));
        txtDescription.setText(trans.getDescription());
    }

    private void clearForm() {
        cmbCategorie.setValue(null);
        dpDateTransaction.setValue(null);
        txtMontant.clear();
        cmbTypeTransaction.setValue(null);
        cmbStatutTransaction.setValue(null);
        txtSoldeApres.clear();
        txtDescription.clear();
        selectedTransaction = null;
        isEditMode = false;
        btnAjouter.setText("Ajouter");
        tableTransactions.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleAjouter() {
        if (!validateForm()) return;

        try {
            String categorie = cmbCategorie.getValue();
            LocalDate date = dpDateTransaction.getValue();
            double montant = Double.parseDouble(txtMontant.getText().trim());
            String type = cmbTypeTransaction.getValue();
            String statut = cmbStatutTransaction.getValue();
            double soldeApres = txtSoldeApres.getText().isEmpty() ? 0 : Double.parseDouble(txtSoldeApres.getText().trim());
            String description = txtDescription.getText();

            if (isEditMode && selectedTransaction != null) {
                selectedTransaction.setCategorie(categorie);
                selectedTransaction.setDateTransaction(date);
                selectedTransaction.setMontant(montant);
                selectedTransaction.setTypeTransaction(type);
                selectedTransaction.setStatutTransaction(statut);
                selectedTransaction.setSoldeApres(soldeApres);
                selectedTransaction.setDescription(description);
                tableTransactions.refresh();
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Transaction modifiee avec succès!");
            } else {
                transactionsList.add(new Transaction(categorie, date, montant, type, statut, soldeApres, description));
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Transaction ajoutee avec succès!");
            }
            clearForm();
            updateStats();
            updateTableInfo();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Valeurs numeriques invalides.");
        }
    }

    @FXML private void handleSupprimer() {
        if (selectedTransaction != null) deleteTransaction(selectedTransaction);
        else showAlert(Alert.AlertType.WARNING, "Avertissement", "Selectionnez une transaction.");
    }

    @FXML private void handleAnnuler() { clearForm(); }

    private void editTransaction(Transaction trans) {
        selectedTransaction = trans;
        populateForm(trans);
        isEditMode = true;
        btnAjouter.setText("Modifier");
    }

    private void deleteTransaction(Transaction trans) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette transaction?", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            transactionsList.remove(trans);
            clearForm();
            updateStats();
            updateTableInfo();
        }
    }

    private boolean validateForm() {
        if (cmbCategorie.getValue() == null || dpDateTransaction.getValue() == null ||
            txtMontant.getText().isEmpty() || cmbTypeTransaction.getValue() == null ||
            cmbStatutTransaction.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Remplissez tous les champs obligatoires.");
            return false;
        }
        return true;
    }

    // Sorting
    @FXML private void trierParDateDesc() { transactionsList.sort(Comparator.comparing(Transaction::getDateTransaction).reversed()); }
    @FXML private void trierParDateAsc() { transactionsList.sort(Comparator.comparing(Transaction::getDateTransaction)); }
    @FXML private void trierParMontantAsc() { transactionsList.sort(Comparator.comparing(Transaction::getMontant)); }
    @FXML private void trierParMontantDesc() { transactionsList.sort(Comparator.comparing(Transaction::getMontant).reversed()); }
    @FXML private void trierParStatut() { transactionsList.sort(Comparator.comparing(Transaction::getStatutTransaction)); }

    @FXML private void exporterPDF() { showAlert(Alert.AlertType.INFORMATION, "Export", "Export PDF en developpement."); }
    @FXML private void envoyerSMS() { showAlert(Alert.AlertType.INFORMATION, "SMS", "Envoi SMS en developpement."); }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner class
    public static class Transaction {
        private String categorie;
        private LocalDate dateTransaction;
        private double montant;
        private String typeTransaction;
        private String statutTransaction;
        private double soldeApres;
        private String description;

        public Transaction(String categorie, LocalDate dateTransaction, double montant, 
                          String typeTransaction, String statutTransaction, double soldeApres, String description) {
            this.categorie = categorie;
            this.dateTransaction = dateTransaction;
            this.montant = montant;
            this.typeTransaction = typeTransaction;
            this.statutTransaction = statutTransaction;
            this.soldeApres = soldeApres;
            this.description = description;
        }

        // Getters and Setters
        public String getCategorie() { return categorie; }
        public void setCategorie(String categorie) { this.categorie = categorie; }
        public LocalDate getDateTransaction() { return dateTransaction; }
        public void setDateTransaction(LocalDate dateTransaction) { this.dateTransaction = dateTransaction; }
        public double getMontant() { return montant; }
        public void setMontant(double montant) { this.montant = montant; }
        public String getTypeTransaction() { return typeTransaction; }
        public void setTypeTransaction(String typeTransaction) { this.typeTransaction = typeTransaction; }
        public String getStatutTransaction() { return statutTransaction; }
        public void setStatutTransaction(String statutTransaction) { this.statutTransaction = statutTransaction; }
        public double getSoldeApres() { return soldeApres; }
        public void setSoldeApres(double soldeApres) { this.soldeApres = soldeApres; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
