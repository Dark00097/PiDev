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

public class CreditController implements Initializable {

    @FXML private Label lblCreditsAccordes, lblMontantTotal, lblCreditsEnCours;
    @FXML private ComboBox<String> cmbTypeCredit, cmbStatut;
    @FXML private TextField txtMontantDemande, txtMontantAccord, txtDuree, txtTauxInteret, txtMensualite, txtMontantRestant, txtRecherche;
    @FXML private DatePicker dpDateDemande;
    @FXML private Button btnAjouter;
    @FXML private TableView<Credit> tableCredits;
    @FXML private TableColumn<Credit, String> colType, colMontantDemande, colMontantAccord, colDuree, colTaux, colMensualite, colRestant, colDate, colStatut;
    @FXML private TableColumn<Credit, Void> colActions;
    @FXML private Label lblTableInfo;

    private ObservableList<Credit> creditsList = FXCollections.observableArrayList();
    private FilteredList<Credit> filteredData;
    private Credit selectedCredit = null;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeTable();
        initializeSearch();
        loadSampleData();
        updateStats();
        setupAutoCalculation();
    }

    private void setupAutoCalculation() {
        // Auto-calculate mensualité when values change
        txtMontantAccord.textProperty().addListener((obs, o, n) -> calculateMensualite());
        txtDuree.textProperty().addListener((obs, o, n) -> calculateMensualite());
        txtTauxInteret.textProperty().addListener((obs, o, n) -> calculateMensualite());
    }

    private void calculateMensualite() {
        try {
            double montant = Double.parseDouble(txtMontantAccord.getText().isEmpty() ? "0" : txtMontantAccord.getText());
            int duree = Integer.parseInt(txtDuree.getText().isEmpty() ? "0" : txtDuree.getText());
            double taux = Double.parseDouble(txtTauxInteret.getText().isEmpty() ? "0" : txtTauxInteret.getText()) / 100 / 12;
            
            if (montant > 0 && duree > 0 && taux > 0) {
                double mensualite = montant * (taux * Math.pow(1 + taux, duree)) / (Math.pow(1 + taux, duree) - 1);
                txtMensualite.setText(String.format("%.2f", mensualite));
            }
        } catch (NumberFormatException ignored) {}
    }

    private void initializeTable() {
        colType.setCellValueFactory(new PropertyValueFactory<>("typeCredit"));
        colMontantDemande.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getMontantDemande())));
        colMontantAccord.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getMontantAccord())));
        colDuree.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDuree() + " mois"));
        colTaux.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTauxInteret() + "%"));
        colMensualite.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getMensualite())));
        colRestant.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getMontantRestant())));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateDemande().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Statut badge
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().add("nx-badge");
                switch (item) {
                    case "Accepté": badge.getStyleClass().add("nx-badge-success"); break;
                    case "En cours": badge.getStyleClass().add("nx-badge-warning"); break;
                    case "Refusé": case "Annulé": badge.getStyleClass().add("nx-badge-error"); break;
                    case "Remboursé": badge.getStyleClass().add("nx-badge-info"); break;
                }
                setGraphic(badge);
            }
        });

        // Actions
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
                edit.setOnAction(e -> editCredit(getTableView().getItems().get(getIndex())));
                delete.setOnAction(e -> deleteCredit(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) { super.updateItem(item, empty); setGraphic(empty ? null : box); }
        });

        tableCredits.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) { selectedCredit = n; populateForm(n); isEditMode = true; btnAjouter.setText("Modifier"); }
        });
    }

    private void initializeSearch() {
        filteredData = new FilteredList<>(creditsList, p -> true);
        txtRecherche.textProperty().addListener((obs, o, n) -> {
            filteredData.setPredicate(c -> n == null || n.isEmpty() ||
                c.getTypeCredit().toLowerCase().contains(n.toLowerCase()) ||
                c.getStatut().toLowerCase().contains(n.toLowerCase()));
            updateTableInfo();
        });
        SortedList<Credit> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(tableCredits.comparatorProperty());
        tableCredits.setItems(sorted);
    }

    private void loadSampleData() {
        creditsList.addAll(
            new Credit("Crédit Immobilier", 250000, 240000, 240, 5.5, 1542.89, 185146.80, LocalDate.of(2022, 3, 15), "En cours"),
            new Credit("Crédit Automobile", 45000, 45000, 60, 7.0, 891.04, 26731.20, LocalDate.of(2023, 6, 1), "Accepté"),
            new Credit("Crédit Personnel", 15000, 12000, 36, 9.5, 385.22, 7704.40, LocalDate.of(2024, 1, 10), "En cours"),
            new Credit("Crédit Professionnel", 100000, 0, 84, 6.5, 0, 0, LocalDate.of(2024, 5, 20), "Refusé"),
            new Credit("Crédit Immobilier", 180000, 180000, 180, 5.0, 1423.92, 0, LocalDate.of(2019, 1, 1), "Remboursé"),
            new Credit("Crédit Immobilier", 320000, 320000, 240, 5.2, 2154.11, 214320.00, LocalDate.of(2021, 9, 12), "En cours"),
            new Credit("Crédit Automobile", 60000, 60000, 72, 6.8, 1018.33, 40250.75, LocalDate.of(2023, 11, 8), "Accepté"),
            new Credit("Crédit Personnel", 8000, 8000, 24, 10.5, 371.52, 1324.20, LocalDate.of(2024, 7, 2), "En cours"),
            new Credit("Crédit Professionnel", 150000, 150000, 96, 6.2, 1983.47, 137800.00, LocalDate.of(2022, 4, 25), "Accepté")
        );
        updateTableInfo();
    }

    private void updateStats() {
        long accordes = creditsList.stream().filter(c -> c.getStatut().equals("Accepté") || c.getStatut().equals("En cours") || c.getStatut().equals("Remboursé")).count();
        double total = creditsList.stream().mapToDouble(Credit::getMontantAccord).sum();
        long enCours = creditsList.stream().filter(c -> c.getStatut().equals("En cours")).count();
        
        lblCreditsAccordes.setText(String.valueOf(accordes));
        lblMontantTotal.setText(String.format("%,.2f DT", total));
        lblCreditsEnCours.setText(String.valueOf(enCours));
    }

    private void updateTableInfo() {
        lblTableInfo.setText(String.format("Affichage de %d sur %d entrées", filteredData.size(), creditsList.size()));
    }

    private void populateForm(Credit c) {
        cmbTypeCredit.setValue(c.getTypeCredit());
        txtMontantDemande.setText(String.valueOf(c.getMontantDemande()));
        txtMontantAccord.setText(String.valueOf(c.getMontantAccord()));
        txtDuree.setText(String.valueOf(c.getDuree()));
        txtTauxInteret.setText(String.valueOf(c.getTauxInteret()));
        txtMensualite.setText(String.valueOf(c.getMensualite()));
        txtMontantRestant.setText(String.valueOf(c.getMontantRestant()));
        dpDateDemande.setValue(c.getDateDemande());
        cmbStatut.setValue(c.getStatut());
    }

    private void clearForm() {
        cmbTypeCredit.setValue(null); txtMontantDemande.clear(); txtMontantAccord.clear();
        txtDuree.clear(); txtTauxInteret.clear(); txtMensualite.clear(); txtMontantRestant.clear();
        dpDateDemande.setValue(null); cmbStatut.setValue(null);
        selectedCredit = null; isEditMode = false; btnAjouter.setText("Ajouter");
        tableCredits.getSelectionModel().clearSelection();
    }

    @FXML private void handleAjouter() {
        try {
            String type = cmbTypeCredit.getValue();
            double demande = Double.parseDouble(txtMontantDemande.getText());
            double accord = txtMontantAccord.getText().isEmpty() ? 0 : Double.parseDouble(txtMontantAccord.getText());
            int duree = Integer.parseInt(txtDuree.getText());
            double taux = Double.parseDouble(txtTauxInteret.getText());
            double mensualite = txtMensualite.getText().isEmpty() ? 0 : Double.parseDouble(txtMensualite.getText());
            double restant = txtMontantRestant.getText().isEmpty() ? accord : Double.parseDouble(txtMontantRestant.getText());
            LocalDate date = dpDateDemande.getValue();
            String statut = cmbStatut.getValue();

            if (type == null || date == null || statut == null) {
                showAlert(Alert.AlertType.ERROR, "Validation", "Remplissez les champs obligatoires.");
                return;
            }

            if (isEditMode && selectedCredit != null) {
                selectedCredit.setTypeCredit(type); selectedCredit.setMontantDemande(demande);
                selectedCredit.setMontantAccord(accord); selectedCredit.setDuree(duree);
                selectedCredit.setTauxInteret(taux); selectedCredit.setMensualite(mensualite);
                selectedCredit.setMontantRestant(restant); selectedCredit.setDateDemande(date);
                selectedCredit.setStatut(statut);
                tableCredits.refresh();
            } else {
                creditsList.add(new Credit(type, demande, accord, duree, taux, mensualite, restant, date, statut));
            }
            clearForm(); updateStats(); updateTableInfo();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Valeurs numériques invalides.");
        }
    }

    @FXML private void handleSupprimer() { if (selectedCredit != null) deleteCredit(selectedCredit); }
    @FXML private void handleAnnuler() { clearForm(); }

    private void editCredit(Credit c) { selectedCredit = c; populateForm(c); isEditMode = true; btnAjouter.setText("Modifier"); }
    private void deleteCredit(Credit c) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce crédit?", ButtonType.OK, ButtonType.CANCEL);
        if (a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            creditsList.remove(c); clearForm(); updateStats(); updateTableInfo();
        }
    }

    @FXML private void trierParType() { creditsList.sort(Comparator.comparing(Credit::getTypeCredit)); }
    @FXML private void trierParMontant() { creditsList.sort(Comparator.comparing(Credit::getMontantDemande).reversed()); }
    @FXML private void trierParDate() { creditsList.sort(Comparator.comparing(Credit::getDateDemande).reversed()); }
    @FXML private void trierParStatut() { creditsList.sort(Comparator.comparing(Credit::getStatut)); }
    @FXML private void exporterPDF() { showAlert(Alert.AlertType.INFORMATION, "Export", "En développement."); }
    @FXML private void envoyerSMS() { showAlert(Alert.AlertType.INFORMATION, "SMS", "En développement."); }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    public static class Credit {
        private String typeCredit, statut;
        private double montantDemande, montantAccord, tauxInteret, mensualite, montantRestant;
        private int duree;
        private LocalDate dateDemande;

        public Credit(String typeCredit, double montantDemande, double montantAccord, int duree, 
                     double tauxInteret, double mensualite, double montantRestant, LocalDate dateDemande, String statut) {
            this.typeCredit = typeCredit; this.montantDemande = montantDemande; this.montantAccord = montantAccord;
            this.duree = duree; this.tauxInteret = tauxInteret; this.mensualite = mensualite;
            this.montantRestant = montantRestant; this.dateDemande = dateDemande; this.statut = statut;
        }

        // Getters/Setters
        public String getTypeCredit() { return typeCredit; } public void setTypeCredit(String v) { this.typeCredit = v; }
        public double getMontantDemande() { return montantDemande; } public void setMontantDemande(double v) { this.montantDemande = v; }
        public double getMontantAccord() { return montantAccord; } public void setMontantAccord(double v) { this.montantAccord = v; }
        public int getDuree() { return duree; } public void setDuree(int v) { this.duree = v; }
        public double getTauxInteret() { return tauxInteret; } public void setTauxInteret(double v) { this.tauxInteret = v; }
        public double getMensualite() { return mensualite; } public void setMensualite(double v) { this.mensualite = v; }
        public double getMontantRestant() { return montantRestant; } public void setMontantRestant(double v) { this.montantRestant = v; }
        public LocalDate getDateDemande() { return dateDemande; } public void setDateDemande(LocalDate v) { this.dateDemande = v; }
        public String getStatut() { return statut; } public void setStatut(String v) { this.statut = v; }
    }
}
