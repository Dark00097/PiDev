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
import java.util.ResourceBundle;

public class GarantieCreditController implements Initializable {

    @FXML private Label lblNombreGaranties, lblValeurTotale, lblGarantiesActives;
    @FXML private ComboBox<String> cmbTypeGarantie, cmbStatut;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtAdresseBien, txtValeurEstimee, txtValeurRetenue, txtDocumentJustificatif, txtNomGarant, txtRecherche;
    @FXML private DatePicker dpDateEvaluation;
    @FXML private Button btnAjouter;
    @FXML private TableView<GarantieCredit> tableGaranties;
    @FXML private TableColumn<GarantieCredit, String> colType, colValeurEstimee, colValeurRetenue, colAdresse, colGarant, colDate, colStatut;
    @FXML private TableColumn<GarantieCredit, Void> colActions;
    @FXML private Label lblTableInfo;

    private ObservableList<GarantieCredit> garantiesList = FXCollections.observableArrayList();
    private FilteredList<GarantieCredit> filteredData;
    private GarantieCredit selectedGarantie = null;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeTable();
        initializeSearch();
        loadSampleData();
        updateStats();
    }

    private void initializeTable() {
        colType.setCellValueFactory(new PropertyValueFactory<>("typeGarantie"));
        colValeurEstimee.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getValeurEstimee())));
        colValeurRetenue.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getValeurRetenue())));
        colAdresse.setCellValueFactory(new PropertyValueFactory<>("adresseBien"));
        colGarant.setCellValueFactory(new PropertyValueFactory<>("nomGarant"));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateEvaluation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().add("nx-badge");
                switch (item) {
                    case "Accepté": badge.getStyleClass().add("nx-badge-success"); break;
                    case "En attente": badge.getStyleClass().add("nx-badge-warning"); break;
                    case "Refusé": case "Annulé": badge.getStyleClass().add("nx-badge-error"); break;
                }
                setGraphic(badge);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button edit = new Button(), delete = new Button();
            private final HBox box = new HBox(8, edit, delete);
            {
                edit.getStyleClass().addAll("nx-table-action", "nx-table-action-edit");
                delete.getStyleClass().addAll("nx-table-action", "nx-table-action-delete");
                SVGPath eIcon = new SVGPath(), dIcon = new SVGPath();
                eIcon.setContent("M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z");
                dIcon.setContent("M19 7l-.867 12.142A2 2 0 0 1 16.138 21H7.862a2 2 0 0 1-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v3M4 7h16");
                eIcon.getStyleClass().add("nx-action-icon"); dIcon.getStyleClass().add("nx-action-icon");
                edit.setGraphic(eIcon); delete.setGraphic(dIcon);
                box.setAlignment(Pos.CENTER);
                edit.setOnAction(e -> editGarantie(getTableView().getItems().get(getIndex())));
                delete.setOnAction(e -> deleteGarantie(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) { super.updateItem(item, empty); setGraphic(empty ? null : box); }
        });

        tableGaranties.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) { selectedGarantie = n; populateForm(n); isEditMode = true; btnAjouter.setText("Modifier"); }
        });
    }

    private void initializeSearch() {
        filteredData = new FilteredList<>(garantiesList, p -> true);
        txtRecherche.textProperty().addListener((obs, o, n) -> {
            filteredData.setPredicate(g -> n == null || n.isEmpty() ||
                g.getTypeGarantie().toLowerCase().contains(n.toLowerCase()) ||
                g.getNomGarant().toLowerCase().contains(n.toLowerCase()));
            updateTableInfo();
        });
        SortedList<GarantieCredit> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(tableGaranties.comparatorProperty());
        tableGaranties.setItems(sorted);
    }

    private void loadSampleData() {
        garantiesList.addAll(
            new GarantieCredit("Hypothèque Immobilière", "Villa 3 étages", "Tunis, Les Berges du Lac", 850000, 680000, "titre_propriete.pdf", LocalDate.of(2024, 2, 15), "Ahmed Ben Salah", "Accepté"),
            new GarantieCredit("Caution Personnelle", "Garantie salariale", "", 0, 50000, "fiche_paie.pdf", LocalDate.of(2024, 3, 10), "Mohamed Trabelsi", "Accepté"),
            new GarantieCredit("Nantissement", "Compte épargne bloqué", "", 120000, 120000, "releve_compte.pdf", LocalDate.of(2024, 4, 5), "", "En attente"),
            new GarantieCredit("Garantie Bancaire", "Lettre de garantie BNA", "", 200000, 180000, "lettre_garantie.pdf", LocalDate.of(2024, 1, 20), "", "Accepté"),
            new GarantieCredit("Assurance Crédit", "Assurance décès/invalidité", "", 0, 250000, "contrat_assurance.pdf", LocalDate.of(2024, 5, 1), "Fatma Gharbi", "Refusé"),
            new GarantieCredit("Hypothèque Immobilière", "Appartement centre-ville", "Sfax, Route Soukra", 420000, 330000, "titre_propriete2.pdf", LocalDate.of(2023, 12, 7), "Nour Ben Youssef", "Accepté"),
            new GarantieCredit("Caution Personnelle", "Garantie parentale", "", 0, 60000, "attestation_revenus.pdf", LocalDate.of(2024, 6, 12), "Amine Khelifi", "En attente"),
            new GarantieCredit("Nantissement", "Dépôt à terme", "", 90000, 90000, "attestation_depot.pdf", LocalDate.of(2024, 2, 2), "Salma Ayadi", "Accepté")
        );
        updateTableInfo();
    }

    private void updateStats() {
        lblNombreGaranties.setText(String.valueOf(garantiesList.size()));
        lblValeurTotale.setText(String.format("%,.2f DT", garantiesList.stream().mapToDouble(GarantieCredit::getValeurRetenue).sum()));
        lblGarantiesActives.setText(String.valueOf(garantiesList.stream().filter(g -> g.getStatut().equals("Accepté")).count()));
    }

    private void updateTableInfo() {
        lblTableInfo.setText(String.format("Affichage de %d sur %d entrées", filteredData.size(), garantiesList.size()));
    }

    private void populateForm(GarantieCredit g) {
        cmbTypeGarantie.setValue(g.getTypeGarantie());
        txtDescription.setText(g.getDescription());
        txtAdresseBien.setText(g.getAdresseBien());
        txtValeurEstimee.setText(String.valueOf(g.getValeurEstimee()));
        txtValeurRetenue.setText(String.valueOf(g.getValeurRetenue()));
        txtDocumentJustificatif.setText(g.getDocumentJustificatif());
        dpDateEvaluation.setValue(g.getDateEvaluation());
        txtNomGarant.setText(g.getNomGarant());
        cmbStatut.setValue(g.getStatut());
    }

    private void clearForm() {
        cmbTypeGarantie.setValue(null); txtDescription.clear(); txtAdresseBien.clear();
        txtValeurEstimee.clear(); txtValeurRetenue.clear(); txtDocumentJustificatif.clear();
        dpDateEvaluation.setValue(null); txtNomGarant.clear(); cmbStatut.setValue(null);
        selectedGarantie = null; isEditMode = false; btnAjouter.setText("Ajouter");
        tableGaranties.getSelectionModel().clearSelection();
    }

    @FXML private void handleAjouter() {
        try {
            String type = cmbTypeGarantie.getValue();
            String desc = txtDescription.getText();
            String adresse = txtAdresseBien.getText();
            double estimee = Double.parseDouble(txtValeurEstimee.getText().isEmpty() ? "0" : txtValeurEstimee.getText());
            double retenue = Double.parseDouble(txtValeurRetenue.getText().isEmpty() ? "0" : txtValeurRetenue.getText());
            String doc = txtDocumentJustificatif.getText();
            LocalDate date = dpDateEvaluation.getValue();
            String garant = txtNomGarant.getText();
            String statut = cmbStatut.getValue();

            if (type == null || date == null || statut == null) {
                showAlert(Alert.AlertType.ERROR, "Validation", "Remplissez les champs obligatoires.");
                return;
            }

            if (isEditMode && selectedGarantie != null) {
                selectedGarantie.setTypeGarantie(type); selectedGarantie.setDescription(desc);
                selectedGarantie.setAdresseBien(adresse); selectedGarantie.setValeurEstimee(estimee);
                selectedGarantie.setValeurRetenue(retenue); selectedGarantie.setDocumentJustificatif(doc);
                selectedGarantie.setDateEvaluation(date); selectedGarantie.setNomGarant(garant);
                selectedGarantie.setStatut(statut);
                tableGaranties.refresh();
            } else {
                garantiesList.add(new GarantieCredit(type, desc, adresse, estimee, retenue, doc, date, garant, statut));
            }
            clearForm(); updateStats(); updateTableInfo();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Valeurs numériques invalides.");
        }
    }

    @FXML private void handleSupprimer() { if (selectedGarantie != null) deleteGarantie(selectedGarantie); }
    @FXML private void handleAnnuler() { clearForm(); }

    private void editGarantie(GarantieCredit g) { selectedGarantie = g; populateForm(g); isEditMode = true; btnAjouter.setText("Modifier"); }
    private void deleteGarantie(GarantieCredit g) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette garantie?", ButtonType.OK, ButtonType.CANCEL);
        if (a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            garantiesList.remove(g); clearForm(); updateStats(); updateTableInfo();
        }
    }

    @FXML private void trierParType() { garantiesList.sort(Comparator.comparing(GarantieCredit::getTypeGarantie)); }
    @FXML private void trierParValeur() { garantiesList.sort(Comparator.comparing(GarantieCredit::getValeurRetenue).reversed()); }
    @FXML private void trierParDate() { garantiesList.sort(Comparator.comparing(GarantieCredit::getDateEvaluation).reversed()); }
    @FXML private void trierParStatut() { garantiesList.sort(Comparator.comparing(GarantieCredit::getStatut)); }
    @FXML private void exporterPDF() { showAlert(Alert.AlertType.INFORMATION, "Export", "En développement."); }
    @FXML private void envoyerSMS() { showAlert(Alert.AlertType.INFORMATION, "SMS", "En développement."); }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    public static class GarantieCredit {
        private String typeGarantie, description, adresseBien, documentJustificatif, nomGarant, statut;
        private double valeurEstimee, valeurRetenue;
        private LocalDate dateEvaluation;

        public GarantieCredit(String typeGarantie, String description, String adresseBien, double valeurEstimee, 
                             double valeurRetenue, String documentJustificatif, LocalDate dateEvaluation, String nomGarant, String statut) {
            this.typeGarantie = typeGarantie; this.description = description; this.adresseBien = adresseBien;
            this.valeurEstimee = valeurEstimee; this.valeurRetenue = valeurRetenue; this.documentJustificatif = documentJustificatif;
            this.dateEvaluation = dateEvaluation; this.nomGarant = nomGarant; this.statut = statut;
        }

        // Getters/Setters
        public String getTypeGarantie() { return typeGarantie; } public void setTypeGarantie(String v) { this.typeGarantie = v; }
        public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
        public String getAdresseBien() { return adresseBien; } public void setAdresseBien(String v) { this.adresseBien = v; }
        public double getValeurEstimee() { return valeurEstimee; } public void setValeurEstimee(double v) { this.valeurEstimee = v; }
        public double getValeurRetenue() { return valeurRetenue; } public void setValeurRetenue(double v) { this.valeurRetenue = v; }
        public String getDocumentJustificatif() { return documentJustificatif; } public void setDocumentJustificatif(String v) { this.documentJustificatif = v; }
        public LocalDate getDateEvaluation() { return dateEvaluation; } public void setDateEvaluation(LocalDate v) { this.dateEvaluation = v; }
        public String getNomGarant() { return nomGarant; } public void setNomGarant(String v) { this.nomGarant = v; }
        public String getStatut() { return statut; } public void setStatut(String v) { this.statut = v; }
    }
}
