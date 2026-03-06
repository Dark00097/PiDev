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
import java.util.Comparator;
import java.util.Optional;
import java.util.ResourceBundle;

public class PartenaireController implements Initializable {

    // Stat Labels
    @FXML private Label lblPartenairesActifs;
    @FXML private Label lblOffresDisponibles;
    @FXML private Label lblPartenairesPremium;

    // Form Fields
    @FXML private TextField txtNom;
    @FXML private ComboBox<String> cmbCategorie;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtTauxCashback;
    @FXML private TextField txtTauxCashbackMax;
    @FXML private TextField txtPlafondMensuel;
    @FXML private TextArea txtConditions;
    @FXML private ComboBox<String> cmbStatut;

    // Buttons
    @FXML private Button btnAjouter;
    @FXML private Button btnSupprimer;
    @FXML private Button btnAnnuler;

    // Table
    @FXML private TableView<Partenaire> tablePartenaires;
    @FXML private TableColumn<Partenaire, String> colNom;
    @FXML private TableColumn<Partenaire, String> colCategorie;
    @FXML private TableColumn<Partenaire, String> colTaux;
    @FXML private TableColumn<Partenaire, String> colTauxMax;
    @FXML private TableColumn<Partenaire, String> colPlafond;
    @FXML private TableColumn<Partenaire, String> colDescription;
    @FXML private TableColumn<Partenaire, String> colStatut;
    @FXML private TableColumn<Partenaire, Void> colActions;

    // Search and Info
    @FXML private TextField txtRecherche;
    @FXML private Label lblTableInfo;

    private ObservableList<Partenaire> partenairesList = FXCollections.observableArrayList();
    private FilteredList<Partenaire> filteredData;
    private Partenaire selectedPartenaire = null;
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
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colTaux.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getTauxCashback() + "%"));
        colTauxMax.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getTauxCashbackMax() + "%"));
        colPlafond.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("%.2f DT", cellData.getValue().getPlafondMensuel())));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Categorie with colored badge
        colCategorie.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("nx-badge", "nx-badge-category");
                    setGraphic(badge);
                }
            }
        });

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
                        case "Actif": badge.getStyleClass().add("nx-badge-success"); break;
                        case "Premium": badge.getStyleClass().add("nx-badge-purple"); break;
                        case "Inactif": badge.getStyleClass().add("nx-badge-warning"); break;
                        case "Suspendu": badge.getStyleClass().add("nx-badge-error"); break;
                    }
                    setGraphic(badge);
                }
            }
        });

        // Taux with visual indicator
        colTaux.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Partenaire p = getTableView().getItems().get(getIndex());
                    HBox hbox = new HBox(8);
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    
                    ProgressBar progress = new ProgressBar(p.getTauxCashback() / 20.0);
                    progress.setPrefWidth(50);
                    progress.setPrefHeight(8);
                    progress.getStyleClass().add("nx-mini-progress");
                    
                    Label label = new Label(item);
                    label.getStyleClass().add("nx-taux-label");
                    
                    hbox.getChildren().addAll(progress, label);
                    setGraphic(hbox);
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
                    Partenaire partenaire = getTableView().getItems().get(getIndex());
                    editPartenaire(partenaire);
                });

                btnDelete.setOnAction(event -> {
                    Partenaire partenaire = getTableView().getItems().get(getIndex());
                    deletePartenaire(partenaire);
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
        filteredData = new FilteredList<>(partenairesList, p -> true);

        txtRecherche.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(partenaire -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return partenaire.getNom().toLowerCase().contains(lowerCaseFilter) ||
                       partenaire.getCategorie().toLowerCase().contains(lowerCaseFilter) ||
                       partenaire.getStatut().toLowerCase().contains(lowerCaseFilter);
            });
            updateTableInfo();
        });

        SortedList<Partenaire> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablePartenaires.comparatorProperty());
        tablePartenaires.setItems(sortedData);
    }

    private void setupTableSelection() {
        tablePartenaires.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedPartenaire = newSelection;
                populateForm(newSelection);
                isEditMode = true;
                btnAjouter.setText("Modifier");
            }
        });
    }

    private void loadSampleData() {
        partenairesList.addAll(
            new Partenaire("Carrefour", "Grande Distribution", "Supermarche et produits alimentaires", 3.0, 8.0, 50.0, "Valable sur tous les achats", "Premium"),
            new Partenaire("Zara", "Mode et Vetements", "Pret-a-porter et accessoires", 5.0, 15.0, 100.0, "Hors soldes", "Actif"),
            new Partenaire("Technopolis", "Electronique", "Electromenager et high-tech", 2.0, 5.0, 75.0, "Sur produits selectionnes", "Actif"),
            new Partenaire("Pizza Hut", "Restauration", "Restaurants et livraison", 10.0, 20.0, 30.0, "Commandes en ligne uniquement", "Premium"),
            new Partenaire("Tunisair", "Voyage et Tourisme", "Billets d'avion et packages", 1.5, 3.0, 200.0, "Vols reguliers uniquement", "Actif"),
            new Partenaire("Pharmacie Centrale", "Sante et Bien-etre", "Produits parapharmaceutiques", 4.0, 10.0, 40.0, "Hors medicaments", "Inactif"),
            new Partenaire("Decathlon", "Sport et Loisirs", "Equipements sportifs et outdoor", 3.5, 9.0, 60.0, "Hors promotions", "Actif"),
            new Partenaire("Orange", "Telecoms", "Forfaits mobile et internet", 2.0, 6.0, 35.0, "Forfaits hors subventions", "Actif"),
            new Partenaire("Marriott", "Hotellerie", "Sejours et services", 2.5, 7.0, 150.0, "Reservation directe", "Premium")
        );
        updateTableInfo();
    }

    private void updateStats() {
        long actifs = partenairesList.stream()
            .filter(p -> p.getStatut().equals("Actif") || p.getStatut().equals("Premium")).count();
        int offres = partenairesList.size();
        long premium = partenairesList.stream()
            .filter(p -> p.getStatut().equals("Premium")).count();

        lblPartenairesActifs.setText(String.valueOf(actifs));
        lblOffresDisponibles.setText(String.valueOf(offres));
        lblPartenairesPremium.setText(String.valueOf(premium));
    }

    private void updateTableInfo() {
        int total = partenairesList.size();
        int filtered = filteredData.size();
        lblTableInfo.setText(String.format("Affichage de %d sur %d entrees", filtered, total));
    }

    private void populateForm(Partenaire partenaire) {
        txtNom.setText(partenaire.getNom());
        cmbCategorie.setValue(partenaire.getCategorie());
        txtDescription.setText(partenaire.getDescription());
        txtTauxCashback.setText(String.valueOf(partenaire.getTauxCashback()));
        txtTauxCashbackMax.setText(String.valueOf(partenaire.getTauxCashbackMax()));
        txtPlafondMensuel.setText(String.valueOf(partenaire.getPlafondMensuel()));
        txtConditions.setText(partenaire.getConditions());
        cmbStatut.setValue(partenaire.getStatut());
    }

    private void clearForm() {
        txtNom.clear();
        cmbCategorie.setValue(null);
        txtDescription.clear();
        txtTauxCashback.clear();
        txtTauxCashbackMax.clear();
        txtPlafondMensuel.clear();
        txtConditions.clear();
        cmbStatut.setValue(null);
        selectedPartenaire = null;
        isEditMode = false;
        btnAjouter.setText("Ajouter");
        tablePartenaires.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleAjouter() {
        if (!validateForm()) {
            return;
        }

        try {
            String nom = txtNom.getText().trim();
            String categorie = cmbCategorie.getValue();
            String description = txtDescription.getText();
            double taux = Double.parseDouble(txtTauxCashback.getText().trim());
            double tauxMax = txtTauxCashbackMax.getText().isEmpty() ? taux : Double.parseDouble(txtTauxCashbackMax.getText().trim());
            double plafond = txtPlafondMensuel.getText().isEmpty() ? 0 : Double.parseDouble(txtPlafondMensuel.getText().trim());
            String conditions = txtConditions.getText();
            String statut = cmbStatut.getValue();

            if (isEditMode && selectedPartenaire != null) {
                selectedPartenaire.setNom(nom);
                selectedPartenaire.setCategorie(categorie);
                selectedPartenaire.setDescription(description);
                selectedPartenaire.setTauxCashback(taux);
                selectedPartenaire.setTauxCashbackMax(tauxMax);
                selectedPartenaire.setPlafondMensuel(plafond);
                selectedPartenaire.setConditions(conditions);
                selectedPartenaire.setStatut(statut);
                tablePartenaires.refresh();
                showSuccessAlert("Succes", "Le partenaire a ete modifie avec succes!");
            } else {
                Partenaire newPartenaire = new Partenaire(nom, categorie, description, taux, tauxMax, plafond, conditions, statut);
                partenairesList.add(newPartenaire);
                showSuccessAlert("Succes", "Le partenaire a ete ajoute avec succes!");
            }

            clearForm();
            updateStats();
            updateTableInfo();
        } catch (NumberFormatException e) {
            showErrorAlert("Erreur", "Veuillez entrer des valeurs numeriques valides.");
        }
    }

    @FXML
    private void handleSupprimer() {
        if (selectedPartenaire == null) {
            showWarningAlert("Avertissement", "Veuillez selectionner un partenaire a supprimer.");
            return;
        }
        deletePartenaire(selectedPartenaire);
    }

    @FXML
    private void handleAnnuler() {
        clearForm();
    }

    private void editPartenaire(Partenaire partenaire) {
        selectedPartenaire = partenaire;
        populateForm(partenaire);
        isEditMode = true;
        btnAjouter.setText("Modifier");
    }

    private void deletePartenaire(Partenaire partenaire) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmation de suppression");
        confirmDialog.setHeaderText("Supprimer le partenaire \"" + partenaire.getNom() + "\"x");
        confirmDialog.setContentText("Cette action est irreversible.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            partenairesList.remove(partenaire);
            clearForm();
            updateStats();
            updateTableInfo();
            showSuccessAlert("Succes", "Le partenaire a ete supprime avec succes!");
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (txtNom.getText().trim().isEmpty()) {
            errors.append("- Le nom est obligatoire\n");
        }
        if (cmbCategorie.getValue() == null) {
            errors.append("- La categorie est obligatoire\n");
        }
        if (txtTauxCashback.getText().trim().isEmpty()) {
            errors.append("- Le taux cashback est obligatoire\n");
        }
        if (cmbStatut.getValue() == null) {
            errors.append("- Le statut est obligatoire\n");
        }

        if (errors.length() > 0) {
            showErrorAlert("Validation", "Veuillez corriger les erreurs suivantes:\n" + errors.toString());
            return false;
        }
        return true;
    }

    // Sorting methods
    @FXML private void trierParNom() { partenairesList.sort(Comparator.comparing(Partenaire::getNom)); }
    @FXML private void trierParCategorie() { partenairesList.sort(Comparator.comparing(Partenaire::getCategorie)); }
    @FXML private void trierParTaux() { partenairesList.sort(Comparator.comparing(Partenaire::getTauxCashback).reversed()); }
    @FXML private void trierParStatut() { partenairesList.sort(Comparator.comparing(Partenaire::getStatut)); }

    @FXML private void exporterPDF() { showInfoAlert("Export PDF", "Fonctionnalite en cours de developpement."); }
    @FXML private void envoyerSMS() { showInfoAlert("SMS", "Fonctionnalite en cours de developpement."); }

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

    // Inner class for Partenaire
    public static class Partenaire {
        private String nom;
        private String categorie;
        private String description;
        private double tauxCashback;
        private double tauxCashbackMax;
        private double plafondMensuel;
        private String conditions;
        private String statut;

        public Partenaire(String nom, String categorie, String description, double tauxCashback, 
                         double tauxCashbackMax, double plafondMensuel, String conditions, String statut) {
            this.nom = nom;
            this.categorie = categorie;
            this.description = description;
            this.tauxCashback = tauxCashback;
            this.tauxCashbackMax = tauxCashbackMax;
            this.plafondMensuel = plafondMensuel;
            this.conditions = conditions;
            this.statut = statut;
        }

        // Getters and Setters
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public String getCategorie() { return categorie; }
        public void setCategorie(String categorie) { this.categorie = categorie; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public double getTauxCashback() { return tauxCashback; }
        public void setTauxCashback(double tauxCashback) { this.tauxCashback = tauxCashback; }
        public double getTauxCashbackMax() { return tauxCashbackMax; }
        public void setTauxCashbackMax(double tauxCashbackMax) { this.tauxCashbackMax = tauxCashbackMax; }
        public double getPlafondMensuel() { return plafondMensuel; }
        public void setPlafondMensuel(double plafondMensuel) { this.plafondMensuel = plafondMensuel; }
        public String getConditions() { return conditions; }
        public void setConditions(String conditions) { this.conditions = conditions; }
        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }
    }
}
