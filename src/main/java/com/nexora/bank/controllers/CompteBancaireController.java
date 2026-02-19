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

public class CompteBancaireController implements Initializable {

    // Stat Labels
    @FXML private Label lblClientsActifs;
    @FXML private Label lblTotalDepots;
    @FXML private Label lblTotalComptes;

    // Form Fields
    @FXML private TextField txtNumeroCompte;
    @FXML private TextField txtSolde;
    @FXML private DatePicker dpDateOuverture;
    @FXML private ComboBox<String> cmbStatutCompte;
    @FXML private TextField txtPlafondRetrait;
    @FXML private TextField txtPlafondVirement;
    @FXML private ComboBox<String> cmbTypeCompte;

    // Buttons
    @FXML private Button btnAjouter;
    @FXML private Button btnSupprimer;
    @FXML private Button btnAnnuler;

    // Table
    @FXML private TableView<CompteBancaire> tableComptes;
    @FXML private TableColumn<CompteBancaire, String> colNumero;
    @FXML private TableColumn<CompteBancaire, String> colSolde;
    @FXML private TableColumn<CompteBancaire, String> colDateOuverture;
    @FXML private TableColumn<CompteBancaire, String> colStatut;
    @FXML private TableColumn<CompteBancaire, String> colPlafondRetrait;
    @FXML private TableColumn<CompteBancaire, String> colPlafondVirement;
    @FXML private TableColumn<CompteBancaire, String> colType;
    @FXML private TableColumn<CompteBancaire, Void> colActions;

    // Search and Info
    @FXML private TextField txtRecherche;
    @FXML private Label lblTableInfo;

    private ObservableList<CompteBancaire> comptesList = FXCollections.observableArrayList();
    private FilteredList<CompteBancaire> filteredData;
    private CompteBancaire selectedCompte = null;
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
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroCompte"));
        colSolde.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f DT", cellData.getValue().getSolde())));
        colDateOuverture.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDateOuverture().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutCompte"));
        colPlafondRetrait.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f DT", cellData.getValue().getPlafondRetrait())));
        colPlafondVirement.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f DT", cellData.getValue().getPlafondVirement())));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeCompte"));

        // Status column with badge styling
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
                        case "Actif":
                            badge.getStyleClass().add("nx-badge-success");
                            break;
                        case "Bloque":
                            badge.getStyleClass().add("nx-badge-warning");
                            break;
                        case "Cloture":
                            badge.getStyleClass().add("nx-badge-error");
                            break;
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
                btnEdit.setTooltip(new Tooltip("Modifier"));

                btnDelete.getStyleClass().addAll("nx-table-action", "nx-table-action-delete");
                SVGPath deleteIcon = new SVGPath();
                deleteIcon.setContent("M19 7l-.867 12.142A2 2 0 0 1 16.138 21H7.862a2 2 0 0 1-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v3M4 7h16");
                deleteIcon.getStyleClass().add("nx-action-icon");
                btnDelete.setGraphic(deleteIcon);
                btnDelete.setTooltip(new Tooltip("Supprimer"));

                hbox.setAlignment(Pos.CENTER);
                hbox.getChildren().addAll(btnEdit, btnDelete);

                btnEdit.setOnAction(event -> {
                    CompteBancaire compte = getTableView().getItems().get(getIndex());
                    editCompte(compte);
                });

                btnDelete.setOnAction(event -> {
                    CompteBancaire compte = getTableView().getItems().get(getIndex());
                    deleteCompte(compte);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void initializeSearch() {
        filteredData = new FilteredList<>(comptesList, p -> true);

        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(compte -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return compte.getNumeroCompte().toLowerCase().contains(lowerCaseFilter) ||
                       compte.getStatutCompte().toLowerCase().contains(lowerCaseFilter) ||
                       compte.getTypeCompte().toLowerCase().contains(lowerCaseFilter);
            });
            updateTableInfo();
        });

        SortedList<CompteBancaire> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableComptes.comparatorProperty());
        tableComptes.setItems(sortedData);
    }

    private void setupTableSelection() {
        tableComptes.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedCompte = newSelection;
                populateForm(newSelection);
                isEditMode = true;
                btnAjouter.setText("Modifier");
            }
        });
    }

    private void loadSampleData() {
        comptesList.addAll(
            new CompteBancaire("CB-2024-001", 15000.00, LocalDate.of(2024, 1, 15), "Actif", 5000.00, 10000.00, "Courant"),
            new CompteBancaire("CB-2024-002", 45000.00, LocalDate.of(2024, 2, 20), "Actif", 8000.00, 15000.00, "Epargne"),
            new CompteBancaire("CB-2024-003", 250000.00, LocalDate.of(2024, 3, 10), "Actif", 20000.00, 50000.00, "Professionnel"),
            new CompteBancaire("CB-2024-004", 0.00, LocalDate.of(2023, 11, 5), "Cloture", 0.00, 0.00, "Courant"),
            new CompteBancaire("CB-2024-005", 5000.00, LocalDate.of(2024, 4, 1), "Bloque", 1000.00, 2000.00, "Epargne"),
            new CompteBancaire("CB-2024-006", 9800.00, LocalDate.of(2024, 5, 8), "Actif", 3000.00, 6000.00, "Courant"),
            new CompteBancaire("CB-2024-007", 125000.00, LocalDate.of(2022, 8, 12), "Actif", 15000.00, 30000.00, "Professionnel"),
            new CompteBancaire("CB-2024-008", 3200.00, LocalDate.of(2024, 1, 28), "Bloque", 500.00, 1000.00, "Epargne"),
            new CompteBancaire("CB-2024-009", 67000.00, LocalDate.of(2023, 9, 18), "Actif", 10000.00, 20000.00, "Epargne")
        );
        updateTableInfo();
    }

    private void updateStats() {
        long clientsActifs = comptesList.stream().filter(c -> c.getStatutCompte().equals("Actif")).count();
        double totalDepots = comptesList.stream().mapToDouble(CompteBancaire::getSolde).sum();
        int totalComptes = comptesList.size();

        lblClientsActifs.setText(String.valueOf(clientsActifs));
        lblTotalDepots.setText(String.format("%,.2f DT", totalDepots));
        lblTotalComptes.setText(String.valueOf(totalComptes));
    }

    private void updateTableInfo() {
        int total = comptesList.size();
        int filtered = filteredData.size();
        lblTableInfo.setText(String.format("Affichage de %d sur %d entrees", filtered, total));
    }

    private void populateForm(CompteBancaire compte) {
        txtNumeroCompte.setText(compte.getNumeroCompte());
        txtSolde.setText(String.valueOf(compte.getSolde()));
        dpDateOuverture.setValue(compte.getDateOuverture());
        cmbStatutCompte.setValue(compte.getStatutCompte());
        txtPlafondRetrait.setText(String.valueOf(compte.getPlafondRetrait()));
        txtPlafondVirement.setText(String.valueOf(compte.getPlafondVirement()));
        cmbTypeCompte.setValue(compte.getTypeCompte());
    }

    private void clearForm() {
        txtNumeroCompte.clear();
        txtSolde.clear();
        dpDateOuverture.setValue(null);
        cmbStatutCompte.setValue(null);
        txtPlafondRetrait.clear();
        txtPlafondVirement.clear();
        cmbTypeCompte.setValue(null);
        selectedCompte = null;
        isEditMode = false;
        btnAjouter.setText("Ajouter");
        tableComptes.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleAjouter() {
        if (!validateForm()) {
            return;
        }

        try {
            String numero = txtNumeroCompte.getText().trim();
            double solde = Double.parseDouble(txtSolde.getText().trim());
            LocalDate date = dpDateOuverture.getValue();
            String statut = cmbStatutCompte.getValue();
            double plafondRetrait = Double.parseDouble(txtPlafondRetrait.getText().trim());
            double plafondVirement = Double.parseDouble(txtPlafondVirement.getText().trim());
            String type = cmbTypeCompte.getValue();

            if (isEditMode && selectedCompte != null) {
                // Update existing
                selectedCompte.setNumeroCompte(numero);
                selectedCompte.setSolde(solde);
                selectedCompte.setDateOuverture(date);
                selectedCompte.setStatutCompte(statut);
                selectedCompte.setPlafondRetrait(plafondRetrait);
                selectedCompte.setPlafondVirement(plafondVirement);
                selectedCompte.setTypeCompte(type);
                tableComptes.refresh();
                showSuccessAlert("Succes", "Le compte a ete modifie avec succes!");
            } else {
                // Add new
                CompteBancaire newCompte = new CompteBancaire(numero, solde, date, statut, plafondRetrait, plafondVirement, type);
                comptesList.add(newCompte);
                showSuccessAlert("Succes", "Le compte a ete ajoute avec succes!");
            }

            clearForm();
            updateStats();
            updateTableInfo();
        } catch (NumberFormatException e) {
            showErrorAlert("Erreur", "Veuillez entrer des valeurs numeriques valides pour les montants.");
        }
    }

    @FXML
    private void handleSupprimer() {
        if (selectedCompte == null) {
            showWarningAlert("Avertissement", "Veuillez selectionner un compte a supprimer.");
            return;
        }
        deleteCompte(selectedCompte);
    }

    @FXML
    private void handleAnnuler() {
        clearForm();
    }

    private void editCompte(CompteBancaire compte) {
        selectedCompte = compte;
        populateForm(compte);
        isEditMode = true;
        btnAjouter.setText("Modifier");
    }

    private void deleteCompte(CompteBancaire compte) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation de suppression");
        confirmDialog.setHeaderText("Supprimer le compte " + compte.getNumeroCompte() + "a");
        confirmDialog.setContentText("Cette action est irreversible.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            comptesList.remove(compte);
            clearForm();
            updateStats();
            updateTableInfo();
            showSuccessAlert("Succes", "Le compte a ete supprime avec succes!");
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (txtNumeroCompte.getText().trim().isEmpty()) {
            errors.append("- Le numero de compte est obligatoire\n");
        }
        if (txtSolde.getText().trim().isEmpty()) {
            errors.append("- Le solde est obligatoire\n");
        }
        if (dpDateOuverture.getValue() == null) {
            errors.append("- La date d'ouverture est obligatoire\n");
        }
        if (cmbStatutCompte.getValue() == null) {
            errors.append("- Le statut est obligatoire\n");
        }
        if (cmbTypeCompte.getValue() == null) {
            errors.append("- Le type de compte est obligatoire\n");
        }

        if (errors.length() > 0) {
            showErrorAlert("Validation", "Veuillez corriger les erreurs suivantes:\n" + errors.toString());
            return false;
        }
        return true;
    }

    // Sorting methods
    @FXML private void trierParNumero() { 
        comptesList.sort(Comparator.comparing(CompteBancaire::getNumeroCompte)); 
    }
    
    @FXML private void trierParSoldeAsc() { 
        comptesList.sort(Comparator.comparing(CompteBancaire::getSolde)); 
    }
    
    @FXML private void trierParSoldeDesc() { 
        comptesList.sort(Comparator.comparing(CompteBancaire::getSolde).reversed()); 
    }
    
    @FXML private void trierParDate() { 
        comptesList.sort(Comparator.comparing(CompteBancaire::getDateOuverture)); 
    }
    
    @FXML private void trierParStatut() { 
        comptesList.sort(Comparator.comparing(CompteBancaire::getStatutCompte)); 
    }

    @FXML
    private void exporterPDF() {
        showInfoAlert("Export PDF", "Fonctionnalite d'export PDF en cours de developpement.");
    }

    @FXML
    private void envoyerSMS() {
        showInfoAlert("SMS", "Fonctionnalite d'envoi SMS en cours de developpement.");
    }

    // Pagination methods
    @FXML private void pageFirst() { /* Implementation */ }
    @FXML private void pagePrev() { /* Implementation */ }
    @FXML private void pageNext() { /* Implementation */ }
    @FXML private void pageLast() { /* Implementation */ }

    // Alert helpers
    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner class for CompteBancaire
    public static class CompteBancaire {
        private String numeroCompte;
        private double solde;
        private LocalDate dateOuverture;
        private String statutCompte;
        private double plafondRetrait;
        private double plafondVirement;
        private String typeCompte;

        public CompteBancaire(String numeroCompte, double solde, LocalDate dateOuverture, 
                              String statutCompte, double plafondRetrait, double plafondVirement, String typeCompte) {
            this.numeroCompte = numeroCompte;
            this.solde = solde;
            this.dateOuverture = dateOuverture;
            this.statutCompte = statutCompte;
            this.plafondRetrait = plafondRetrait;
            this.plafondVirement = plafondVirement;
            this.typeCompte = typeCompte;
        }

        // Getters and Setters
        public String getNumeroCompte() { return numeroCompte; }
        public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }
        public double getSolde() { return solde; }
        public void setSolde(double solde) { this.solde = solde; }
        public LocalDate getDateOuverture() { return dateOuverture; }
        public void setDateOuverture(LocalDate dateOuverture) { this.dateOuverture = dateOuverture; }
        public String getStatutCompte() { return statutCompte; }
        public void setStatutCompte(String statutCompte) { this.statutCompte = statutCompte; }
        public double getPlafondRetrait() { return plafondRetrait; }
        public void setPlafondRetrait(double plafondRetrait) { this.plafondRetrait = plafondRetrait; }
        public double getPlafondVirement() { return plafondVirement; }
        public void setPlafondVirement(double plafondVirement) { this.plafondVirement = plafondVirement; }
        public String getTypeCompte() { return typeCompte; }
        public void setTypeCompte(String typeCompte) { this.typeCompte = typeCompte; }
    }
}
