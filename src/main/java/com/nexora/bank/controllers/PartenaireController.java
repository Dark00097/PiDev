package com.nexora.bank.controllers;

import com.nexora.bank.Models.Partenaire;
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
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.util.Comparator;
import java.util.Optional;
import java.util.ResourceBundle;

public class PartenaireController implements Initializable {

    @FXML private Label lblPartenairesActifs;
    @FXML private Label lblOffresDisponibles;
    @FXML private Label lblPartenairesPremium;

    @FXML private TextField txtNom;
    @FXML private ComboBox<String> cmbCategorie;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtTauxCashback;
    @FXML private TextField txtTauxCashbackMax;
    @FXML private TextField txtPlafondMensuel;
    @FXML private TextArea txtConditions;
    @FXML private ComboBox<String> cmbStatut;

    @FXML private Button btnAjouter;

    @FXML private TableView<Partenaire> tablePartenaires;
    @FXML private TableColumn<Partenaire, String> colNom;
    @FXML private TableColumn<Partenaire, String> colCategorie;
    @FXML private TableColumn<Partenaire, String> colTaux;
    @FXML private TableColumn<Partenaire, String> colTauxMax;
    @FXML private TableColumn<Partenaire, String> colPlafond;
    @FXML private TableColumn<Partenaire, String> colDescription;
    @FXML private TableColumn<Partenaire, String> colStatut;
    @FXML private TableColumn<Partenaire, Void> colActions;

    @FXML private TextField txtRecherche;
    @FXML private Label lblTableInfo;

    private final ObservableList<Partenaire> partenairesList = FXCollections.observableArrayList();
    private FilteredList<Partenaire> filteredData;
    private Partenaire selectedPartenaire;
    private boolean isEditMode;

    private final PartenaireService partenaireService = new PartenaireService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        initializeSearch();
        setupTableSelection();
        reloadPartenaires();
    }

    private void initializeTable() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colTaux.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTauxCashback() + "%"));
        colTauxMax.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTauxCashbackMax() + "%"));
        colPlafond.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getPlafondMensuel())));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getStatus())));

        colCategorie.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().addAll("nx-badge", "nx-badge-category");
                setGraphic(badge);
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
                    case "Actif" -> badge.getStyleClass().add("nx-badge-success");
                    case "Premium" -> badge.getStyleClass().add("nx-badge-purple");
                    case "Inactif" -> badge.getStyleClass().add("nx-badge-warning");
                    case "Suspendu" -> badge.getStyleClass().add("nx-badge-error");
                    default -> { }
                }
                setGraphic(badge);
            }
        });

        colTaux.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

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
        });

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

                btnEdit.setOnAction(event -> editPartenaire(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(event -> deletePartenaire(getTableView().getItems().get(getIndex())));
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
                if (newValue == null || newValue.isBlank()) {
                    return true;
                }
                String filter = newValue.trim().toLowerCase();
                return safe(partenaire.getNom()).toLowerCase().contains(filter)
                        || safe(partenaire.getCategorie()).toLowerCase().contains(filter)
                        || safe(partenaire.getStatus()).toLowerCase().contains(filter);
            });
            updateTableInfo();
        });

        SortedList<Partenaire> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tablePartenaires.comparatorProperty());
        tablePartenaires.setItems(sortedData);
    }

    private void setupTableSelection() {
        tablePartenaires.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection == null) {
                return;
            }
            selectedPartenaire = newSelection;
            populateForm(newSelection);
            isEditMode = true;
            btnAjouter.setText("Modifier");
        });
    }

    private void reloadPartenaires() {
        partenairesList.setAll(partenaireService.getAllPartenaires());
        updateStats();
        updateTableInfo();
    }

    private void updateStats() {
        long actifs = partenairesList.stream()
                .filter(p -> "Actif".equalsIgnoreCase(p.getStatus()) || "Premium".equalsIgnoreCase(p.getStatus()))
                .count();
        long premium = partenairesList.stream()
                .filter(p -> "Premium".equalsIgnoreCase(p.getStatus()))
                .count();

        lblPartenairesActifs.setText(String.valueOf(actifs));
        lblOffresDisponibles.setText(String.valueOf(partenairesList.size()));
        lblPartenairesPremium.setText(String.valueOf(premium));
    }

    private void updateTableInfo() {
        int filtered = filteredData == null ? 0 : filteredData.size();
        lblTableInfo.setText(String.format("Affichage de %d sur %d entrees", filtered, partenairesList.size()));
    }

    private void populateForm(Partenaire partenaire) {
        txtNom.setText(partenaire.getNom());
        cmbCategorie.setValue(partenaire.getCategorie());
        txtDescription.setText(partenaire.getDescription());
        txtTauxCashback.setText(String.valueOf(partenaire.getTauxCashback()));
        txtTauxCashbackMax.setText(String.valueOf(partenaire.getTauxCashbackMax()));
        txtPlafondMensuel.setText(String.valueOf(partenaire.getPlafondMensuel()));
        txtConditions.setText(partenaire.getConditions());
        cmbStatut.setValue(partenaire.getStatus());
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
            String description = safe(txtDescription.getText());
            double taux = Double.parseDouble(txtTauxCashback.getText().trim());
            double tauxMax = txtTauxCashbackMax.getText().isBlank() ? taux : Double.parseDouble(txtTauxCashbackMax.getText().trim());
            double plafond = txtPlafondMensuel.getText().isBlank() ? 0 : Double.parseDouble(txtPlafondMensuel.getText().trim());
            String conditions = safe(txtConditions.getText());
            String status = cmbStatut.getValue();

            Partenaire partenaire = new Partenaire();
            partenaire.setNom(nom);
            partenaire.setCategorie(categorie);
            partenaire.setDescription(description);
            partenaire.setTauxCashback(taux);
            partenaire.setTauxCashbackMax(tauxMax);
            partenaire.setPlafondMensuel(plafond);
            partenaire.setConditions(conditions);
            partenaire.setStatus(status);

            if (isEditMode && selectedPartenaire != null) {
                partenaire.setIdPartenaire(selectedPartenaire.getIdPartenaire());
                boolean updated = partenaireService.updatePartenaire(partenaire);
                if (!updated) {
                    showErrorAlert("Erreur", "Modification impossible.");
                    return;
                }
                showSuccessAlert("Succes", "Le partenaire a ete modifie avec succes.");
            } else {
                int createdId = partenaireService.createPartenaire(partenaire);
                if (createdId <= 0) {
                    showErrorAlert("Erreur", "Ajout impossible.");
                    return;
                }
                showSuccessAlert("Succes", "Le partenaire a ete ajoute avec succes.");
            }

            clearForm();
            reloadPartenaires();
        } catch (NumberFormatException ex) {
            showErrorAlert("Erreur", "Veuillez entrer des valeurs numeriques valides.");
        } catch (RuntimeException ex) {
            showErrorAlert("Erreur", ex.getMessage());
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
        confirmDialog.setHeaderText("Supprimer le partenaire \"" + partenaire.getNom() + "\" ?");
        confirmDialog.setContentText("Cette action est irreversible.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            boolean deleted = partenaireService.deletePartenaire(partenaire.getIdPartenaire());
            if (!deleted) {
                showErrorAlert("Erreur", "Suppression impossible.");
                return;
            }

            clearForm();
            reloadPartenaires();
            showSuccessAlert("Succes", "Le partenaire a ete supprime avec succes.");
        } catch (RuntimeException ex) {
            showErrorAlert("Erreur", ex.getMessage());
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

        if (!txtTauxCashback.getText().trim().isEmpty()) {
            try {
                Double.parseDouble(txtTauxCashback.getText().trim());
            } catch (NumberFormatException ex) {
                errors.append("- Taux cashback invalide\n");
            }
        }

        if (!txtTauxCashbackMax.getText().trim().isEmpty()) {
            try {
                Double.parseDouble(txtTauxCashbackMax.getText().trim());
            } catch (NumberFormatException ex) {
                errors.append("- Taux cashback max invalide\n");
            }
        }

        if (!txtPlafondMensuel.getText().trim().isEmpty()) {
            try {
                Double.parseDouble(txtPlafondMensuel.getText().trim());
            } catch (NumberFormatException ex) {
                errors.append("- Plafond mensuel invalide\n");
            }
        }

        if (!errors.isEmpty()) {
            showErrorAlert("Validation", "Veuillez corriger les erreurs suivantes:\n" + errors);
            return false;
        }
        return true;
    }

    @FXML private void trierParNom() { partenairesList.sort(Comparator.comparing(p -> safe(p.getNom()))); }
    @FXML private void trierParCategorie() { partenairesList.sort(Comparator.comparing(p -> safe(p.getCategorie()))); }
    @FXML private void trierParTaux() { partenairesList.sort(Comparator.comparing(Partenaire::getTauxCashback).reversed()); }
    @FXML private void trierParStatut() { partenairesList.sort(Comparator.comparing(p -> safe(p.getStatus()))); }

    @FXML private void exporterPDF() { showInfoAlert("Export PDF", "Fonctionnalite en cours de developpement."); }
    @FXML private void envoyerSMS() { showInfoAlert("SMS", "Fonctionnalite en cours de developpement."); }

    private String safe(String value) {
        return value == null ? "" : value;
    }

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
}
