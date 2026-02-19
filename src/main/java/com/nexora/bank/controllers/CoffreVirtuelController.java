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
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.geometry.Pos;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.ResourceBundle;

public class CoffreVirtuelController implements Initializable {

    // Stat Labels
    @FXML private Label lblTotalCoffres;
    @FXML private Label lblMontantTotal;
    @FXML private Label lblCoffresActifs;

    // Form Fields
    @FXML private TextField txtNom;
    @FXML private TextField txtObjectifMontant;
    @FXML private TextField txtMontantActuel;
    @FXML private DatePicker dpDateCreation;
    @FXML private DatePicker dpDateObjectif;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private CheckBox chkEstVerrouille;

    // Buttons
    @FXML private Button btnAjouter;
    @FXML private Button btnSupprimer;
    @FXML private Button btnAnnuler;

    // Table
    @FXML private TableView<CoffreVirtuel> tableCoffres;
    @FXML private TableColumn<CoffreVirtuel, String> colNom;
    @FXML private TableColumn<CoffreVirtuel, String> colObjectif;
    @FXML private TableColumn<CoffreVirtuel, String> colMontantActuel;
    @FXML private TableColumn<CoffreVirtuel, Void> colProgression;
    @FXML private TableColumn<CoffreVirtuel, String> colDateCreation;
    @FXML private TableColumn<CoffreVirtuel, String> colDateObjectif;
    @FXML private TableColumn<CoffreVirtuel, String> colStatus;
    @FXML private TableColumn<CoffreVirtuel, String> colVerrouille;
    @FXML private TableColumn<CoffreVirtuel, Void> colActions;

    // Search and Info
    @FXML private TextField txtRecherche;
    @FXML private Label lblTableInfo;

    private ObservableList<CoffreVirtuel> coffresList = FXCollections.observableArrayList();
    private FilteredList<CoffreVirtuel> filteredData;
    private CoffreVirtuel selectedCoffre = null;
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
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colObjectif.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f DT", cellData.getValue().getObjectifMontant())));
        colMontantActuel.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f DT", cellData.getValue().getMontantActuel())));
        colDateCreation.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        colDateObjectif.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDateObjectifs().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Progression column with progress bar
        colProgression.setCellFactory(column -> new TableCell<>() {
            private final ProgressBar progressBar = new ProgressBar();
            private final Label label = new Label();
            private final HBox hbox = new HBox(8);

            {
                progressBar.setPrefWidth(80);
                progressBar.setPrefHeight(8);
                progressBar.getStyleClass().add("nx-progress-bar");
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.getChildren().addAll(progressBar, label);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    CoffreVirtuel coffre = getTableView().getItems().get(getIndex());
                    double progress = coffre.getObjectifMontant() > 0 ? 
                        coffre.getMontantActuel() / coffre.getObjectifMontant() : 0;
                    progressBar.setProgress(Math.min(progress, 1.0));
                    label.setText(String.format("%.0f%%", progress * 100));
                    label.getStyleClass().add("nx-progress-label");
                    
                    // Color based on progress
                    progressBar.getStyleClass().removeAll("nx-progress-success", "nx-progress-warning", "nx-progress-error");
                    if (progress >= 1.0) {
                        progressBar.getStyleClass().add("nx-progress-success");
                    } else if (progress >= 0.5) {
                        progressBar.getStyleClass().add("nx-progress-warning");
                    } else {
                        progressBar.getStyleClass().add("nx-progress-error");
                    }
                    
                    setGraphic(hbox);
                }
            }
        });

        // Status column with badge
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("nx-badge");
                    switch (item) {
                        case "Actif": badge.getStyleClass().add("nx-badge-success"); break;
                        case "Bloqué": badge.getStyleClass().add("nx-badge-warning"); break;
                        case "Clôturé": badge.getStyleClass().add("nx-badge-error"); break;
                    }
                    setGraphic(badge);
                }
            }
        });

        // Verrouillé column with icon
        colVerrouille.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    CoffreVirtuel coffre = getTableView().getItems().get(getIndex());
                    SVGPath icon = new SVGPath();
                    if (coffre.isEstVerrouille()) {
                        icon.setContent("M12 15v2m-6 4h12a2 2 0 0 0 2-2v-6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2zm10-10V7a4 4 0 0 0-8 0v4h8z");
                        icon.getStyleClass().addAll("nx-lock-icon", "nx-lock-locked");
                    } else {
                        icon.setContent("M8 11V7a4 4 0 1 1 8 0m-4 8v2m-6 4h12a2 2 0 0 0 2-2v-6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2z");
                        icon.getStyleClass().addAll("nx-lock-icon", "nx-lock-unlocked");
                    }
                    StackPane container = new StackPane(icon);
                    container.setAlignment(Pos.CENTER);
                    setGraphic(container);
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
                    CoffreVirtuel coffre = getTableView().getItems().get(getIndex());
                    editCoffre(coffre);
                });

                btnDelete.setOnAction(event -> {
                    CoffreVirtuel coffre = getTableView().getItems().get(getIndex());
                    deleteCoffre(coffre);
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
        filteredData = new FilteredList<>(coffresList, p -> true);

        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(coffre -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return coffre.getNom().toLowerCase().contains(lowerCaseFilter) ||
                       coffre.getStatus().toLowerCase().contains(lowerCaseFilter);
            });
            updateTableInfo();
        });

        SortedList<CoffreVirtuel> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableCoffres.comparatorProperty());
        tableCoffres.setItems(sortedData);
    }

    private void setupTableSelection() {
        tableCoffres.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedCoffre = newSelection;
                populateForm(newSelection);
                isEditMode = true;
                btnAjouter.setText("Modifier");
            }
        });
    }

    private void loadSampleData() {
        coffresList.addAll(
            new CoffreVirtuel("Vacances 2024", 5000.00, 3500.00, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1), "Actif", true),
            new CoffreVirtuel("Achat Voiture", 30000.00, 12000.00, LocalDate.of(2023, 6, 15), LocalDate.of(2025, 1, 1), "Actif", true),
            new CoffreVirtuel("Fonds d'urgence", 10000.00, 10000.00, LocalDate.of(2022, 1, 1), LocalDate.of(2024, 12, 31), "Actif", false),
            new CoffreVirtuel("Mariage", 20000.00, 5000.00, LocalDate.of(2024, 3, 1), LocalDate.of(2026, 6, 1), "Actif", true),
            new CoffreVirtuel("Études enfants", 50000.00, 15000.00, LocalDate.of(2020, 9, 1), LocalDate.of(2035, 9, 1), "Bloqué", true),
            new CoffreVirtuel("Projet Maison", 80000.00, 22000.00, LocalDate.of(2021, 5, 20), LocalDate.of(2028, 5, 20), "Actif", true),
            new CoffreVirtuel("Santé & Bien-être", 12000.00, 3500.00, LocalDate.of(2024, 2, 10), LocalDate.of(2025, 2, 10), "Actif", false),
            new CoffreVirtuel("Technologie", 9000.00, 7200.00, LocalDate.of(2024, 1, 5), LocalDate.of(2024, 10, 1), "Actif", true)
        );
        updateTableInfo();
    }

    private void updateStats() {
        int totalCoffres = coffresList.size();
        double montantTotal = coffresList.stream().mapToDouble(CoffreVirtuel::getMontantActuel).sum();
        long coffresActifs = coffresList.stream().filter(c -> c.getStatus().equals("Actif")).count();

        lblTotalCoffres.setText(String.valueOf(totalCoffres));
        lblMontantTotal.setText(String.format("%,.2f DT", montantTotal));
        lblCoffresActifs.setText(String.valueOf(coffresActifs));
    }

    private void updateTableInfo() {
        int total = coffresList.size();
        int filtered = filteredData.size();
        lblTableInfo.setText(String.format("Affichage de %d sur %d entrées", filtered, total));
    }

    private void populateForm(CoffreVirtuel coffre) {
        txtNom.setText(coffre.getNom());
        txtObjectifMontant.setText(String.valueOf(coffre.getObjectifMontant()));
        txtMontantActuel.setText(String.valueOf(coffre.getMontantActuel()));
        dpDateCreation.setValue(coffre.getDateCreation());
        dpDateObjectif.setValue(coffre.getDateObjectifs());
        cmbStatus.setValue(coffre.getStatus());
        chkEstVerrouille.setSelected(coffre.isEstVerrouille());
    }

    private void clearForm() {
        txtNom.clear();
        txtObjectifMontant.clear();
        txtMontantActuel.clear();
        dpDateCreation.setValue(null);
        dpDateObjectif.setValue(null);
        cmbStatus.setValue(null);
        chkEstVerrouille.setSelected(false);
        selectedCoffre = null;
        isEditMode = false;
        btnAjouter.setText("Ajouter");
        tableCoffres.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleAjouter() {
        if (!validateForm()) {
            return;
        }

        try {
            String nom = txtNom.getText().trim();
            double objectif = Double.parseDouble(txtObjectifMontant.getText().trim());
            double actuel = Double.parseDouble(txtMontantActuel.getText().trim());
            LocalDate dateCreation = dpDateCreation.getValue();
            LocalDate dateObjectif = dpDateObjectif.getValue();
            String status = cmbStatus.getValue();
            boolean verrouille = chkEstVerrouille.isSelected();

            if (isEditMode && selectedCoffre != null) {
                selectedCoffre.setNom(nom);
                selectedCoffre.setObjectifMontant(objectif);
                selectedCoffre.setMontantActuel(actuel);
                selectedCoffre.setDateCreation(dateCreation);
                selectedCoffre.setDateObjectifs(dateObjectif);
                selectedCoffre.setStatus(status);
                selectedCoffre.setEstVerrouille(verrouille);
                tableCoffres.refresh();
                showSuccessAlert("Succès", "Le coffre a été modifié avec succès!");
            } else {
                CoffreVirtuel newCoffre = new CoffreVirtuel(nom, objectif, actuel, dateCreation, dateObjectif, status, verrouille);
                coffresList.add(newCoffre);
                showSuccessAlert("Succès", "Le coffre a été ajouté avec succès!");
            }

            clearForm();
            updateStats();
            updateTableInfo();
        } catch (NumberFormatException e) {
            showErrorAlert("Erreur", "Veuillez entrer des valeurs numériques valides pour les montants.");
        }
    }

    @FXML
    private void handleSupprimer() {
        if (selectedCoffre == null) {
            showWarningAlert("Avertissement", "Veuillez sélectionner un coffre à supprimer.");
            return;
        }
        deleteCoffre(selectedCoffre);
    }

    @FXML
    private void handleAnnuler() {
        clearForm();
    }

    private void editCoffre(CoffreVirtuel coffre) {
        selectedCoffre = coffre;
        populateForm(coffre);
        isEditMode = true;
        btnAjouter.setText("Modifier");
    }

    private void deleteCoffre(CoffreVirtuel coffre) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation de suppression");
        confirmDialog.setHeaderText("Supprimer le coffre \"" + coffre.getNom() + "\"?");
        confirmDialog.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            coffresList.remove(coffre);
            clearForm();
            updateStats();
            updateTableInfo();
            showSuccessAlert("Succès", "Le coffre a été supprimé avec succès!");
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (txtNom.getText().trim().isEmpty()) {
            errors.append("- Le nom est obligatoire\n");
        }
        if (txtObjectifMontant.getText().trim().isEmpty()) {
            errors.append("- L'objectif montant est obligatoire\n");
        }
        if (dpDateCreation.getValue() == null) {
            errors.append("- La date de création est obligatoire\n");
        }
        if (dpDateObjectif.getValue() == null) {
            errors.append("- La date objectif est obligatoire\n");
        }
        if (cmbStatus.getValue() == null) {
            errors.append("- Le statut est obligatoire\n");
        }

        if (errors.length() > 0) {
            showErrorAlert("Validation", "Veuillez corriger les erreurs suivantes:\n" + errors.toString());
            return false;
        }
        return true;
    }

    // Sorting methods
    @FXML private void trierParNom() { coffresList.sort(Comparator.comparing(CoffreVirtuel::getNom)); }
    @FXML private void trierParObjectif() { coffresList.sort(Comparator.comparing(CoffreVirtuel::getObjectifMontant).reversed()); }
    @FXML private void trierParMontantActuel() { coffresList.sort(Comparator.comparing(CoffreVirtuel::getMontantActuel).reversed()); }
    @FXML private void trierParDateCreation() { coffresList.sort(Comparator.comparing(CoffreVirtuel::getDateCreation)); }
    @FXML private void trierParStatut() { coffresList.sort(Comparator.comparing(CoffreVirtuel::getStatus)); }

    @FXML private void exporterPDF() { showInfoAlert("Export PDF", "Fonctionnalité d'export PDF en cours de développement."); }
    @FXML private void envoyerSMS() { showInfoAlert("SMS", "Fonctionnalité d'envoi SMS en cours de développement."); }
    @FXML private void pageFirst() { }
    @FXML private void pagePrev() { }
    @FXML private void pageNext() { }
    @FXML private void pageLast() { }

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

    // Inner class for CoffreVirtuel
    public static class CoffreVirtuel {
        private String nom;
        private double objectifMontant;
        private double montantActuel;
        private LocalDate dateCreation;
        private LocalDate dateObjectifs;
        private String status;
        private boolean estVerrouille;

        public CoffreVirtuel(String nom, double objectifMontant, double montantActuel, 
                             LocalDate dateCreation, LocalDate dateObjectifs, String status, boolean estVerrouille) {
            this.nom = nom;
            this.objectifMontant = objectifMontant;
            this.montantActuel = montantActuel;
            this.dateCreation = dateCreation;
            this.dateObjectifs = dateObjectifs;
            this.status = status;
            this.estVerrouille = estVerrouille;
        }

        // Getters and Setters
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public double getObjectifMontant() { return objectifMontant; }
        public void setObjectifMontant(double objectifMontant) { this.objectifMontant = objectifMontant; }
        public double getMontantActuel() { return montantActuel; }
        public void setMontantActuel(double montantActuel) { this.montantActuel = montantActuel; }
        public LocalDate getDateCreation() { return dateCreation; }
        public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }
        public LocalDate getDateObjectifs() { return dateObjectifs; }
        public void setDateObjectifs(LocalDate dateObjectifs) { this.dateObjectifs = dateObjectifs; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public boolean isEstVerrouille() { return estVerrouille; }
        public void setEstVerrouille(boolean estVerrouille) { this.estVerrouille = estVerrouille; }
    }
}
