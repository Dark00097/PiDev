//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.nexora.bank.controllers;

import com.nexora.bank.Models.Credit;
import com.nexora.bank.Service.CreditService;
import com.nexora.bank.Utils.PdfReportUtil;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

public class CreditController implements Initializable {
    private static final DateTimeFormatter TABLE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    @FXML
    private Label lblCreditsAccordes;
    @FXML
    private Label lblMontantTotal;
    @FXML
    private Label lblCreditsEnCours;
    @FXML
    private ComboBox<Integer> cmbCompte;
    @FXML
    private ComboBox<String> cmbTypeCredit;
    @FXML
    private ComboBox<String> cmbStatut;
    @FXML
    private TextField txtMontantDemande;
    @FXML
    private TextField txtMontantAccord;
    @FXML
    private TextField txtDuree;
    @FXML
    private TextField txtTauxInteret;
    @FXML
    private TextField txtMensualite;
    @FXML
    private TextField txtMontantRestant;
    @FXML
    private TextField txtRecherche;
    @FXML
    private DatePicker dpDateDemande;
    @FXML
    private Button btnAjouter;
    @FXML
    private TableView<Credit> tableCredits;
    @FXML
    private TableColumn<Credit, String> colCompte;
    @FXML
    private TableColumn<Credit, String> colType;
    @FXML
    private TableColumn<Credit, String> colMontantDemande;
    @FXML
    private TableColumn<Credit, String> colMontantAccord;
    @FXML
    private TableColumn<Credit, String> colDuree;
    @FXML
    private TableColumn<Credit, String> colTaux;
    @FXML
    private TableColumn<Credit, String> colMensualite;
    @FXML
    private TableColumn<Credit, String> colRestant;
    @FXML
    private TableColumn<Credit, String> colDate;
    @FXML
    private TableColumn<Credit, String> colStatut;
    @FXML
    private TableColumn<Credit, Void> colActions;
    @FXML
    private Label lblTableInfo;
    private final CreditService creditService = new CreditService();
    private final ObservableList<Credit> creditsList = FXCollections.observableArrayList();
    private FilteredList<Credit> filteredData;
    private Credit selectedCredit;
    private boolean isEditMode;

    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.initializeTable();
        this.initializeSearch();
        this.setupAutoCalculation();
        this.loadCompteIds();
        this.loadCredits();
    }

    private void initializeTable() {
        this.colCompte.setCellValueFactory((cell) -> new SimpleStringProperty(String.valueOf(((Credit)cell.getValue()).getIdCompte())));
        this.colType.setCellValueFactory(new PropertyValueFactory("typeCredit"));
        this.colMontantDemande.setCellValueFactory((cell) -> new SimpleStringProperty(this.formatAmount(((Credit)cell.getValue()).getMontantDemande())));
        this.colMontantAccord.setCellValueFactory((cell) -> {
            Double montant = ((Credit)cell.getValue()).getMontantAccord();
            return new SimpleStringProperty(montant == null ? "-" : this.formatAmount(montant));
        });
        this.colDuree.setCellValueFactory((cell) -> new SimpleStringProperty(((Credit)cell.getValue()).getDuree() + " mois"));
        this.colTaux.setCellValueFactory((cell) -> new SimpleStringProperty(this.formatPercent(((Credit)cell.getValue()).getTauxInteret())));
        this.colMensualite.setCellValueFactory((cell) -> new SimpleStringProperty(this.formatAmount(((Credit)cell.getValue()).getMensualite())));
        this.colRestant.setCellValueFactory((cell) -> new SimpleStringProperty(this.formatAmount(((Credit)cell.getValue()).getMontantRestant())));
        this.colDate.setCellValueFactory((cell) -> new SimpleStringProperty(this.formatDateForTable(((Credit)cell.getValue()).getDateDemande())));
        this.colStatut.setCellValueFactory(new PropertyValueFactory("statut"));
        this.colStatut.setCellFactory((col) -> new TableCell<Credit, String>() {
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty && item != null) {
                        Label badge = new Label(item);
                        badge.getStyleClass().add("nx-badge");
                        String lowered = item.toLowerCase();
                        if (lowered.contains("accept")) {
                            badge.getStyleClass().add("nx-badge-success");
                        } else if (lowered.contains("cours")) {
                            badge.getStyleClass().add("nx-badge-warning");
                        } else if (!lowered.contains("refus") && !lowered.contains("annul")) {
                            if (lowered.contains("rembours")) {
                                badge.getStyleClass().add("nx-badge-info");
                            }
                        } else {
                            badge.getStyleClass().add("nx-badge-error");
                        }

                        this.setGraphic(badge);
                    } else {
                        this.setGraphic((Node)null);
                    }
                }
            });
        this.colActions.setCellFactory((col) -> new TableCell<Credit, Void>() {
                private final Button editButton = new Button();
                private final Button deleteButton = new Button();
                private final HBox box;

                {
                    this.box = new HBox((double)8.0F, new Node[]{this.editButton, this.deleteButton});
                    this.editButton.getStyleClass().addAll(new String[]{"nx-table-action", "nx-table-action-edit"});
                    this.deleteButton.getStyleClass().addAll(new String[]{"nx-table-action", "nx-table-action-delete"});
                    this.editButton.setTooltip(new Tooltip("Modifier"));
                    this.deleteButton.setTooltip(new Tooltip("Supprimer"));
                    SVGPath editIcon = new SVGPath();
                    editIcon.setContent("M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z");
                    editIcon.getStyleClass().add("nx-action-icon");
                    this.editButton.setGraphic(editIcon);
                    SVGPath deleteIcon = new SVGPath();
                    deleteIcon.setContent("M19 7l-.867 12.142A2 2 0 0 1 16.138 21H7.862a2 2 0 0 1-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v3M4 7h16");
                    deleteIcon.getStyleClass().add("nx-action-icon");
                    this.deleteButton.setGraphic(deleteIcon);
                    this.box.setAlignment(Pos.CENTER);
                    this.editButton.setOnAction((event) -> CreditController.this.editCredit((Credit)this.getTableView().getItems().get(this.getIndex())));
                    this.deleteButton.setOnAction((event) -> CreditController.this.deleteCredit((Credit)this.getTableView().getItems().get(this.getIndex())));
                }

                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    this.setGraphic(empty ? null : this.box);
                }
            });
        this.tableCredits.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                this.editCredit(newValue);
            }

        });
    }

    private void initializeSearch() {
        this.filteredData = new FilteredList<>(this.creditsList, c -> true);
        this.txtRecherche.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase();
            this.filteredData.setPredicate((credit) -> query.isEmpty() || String.valueOf(credit.getIdCredit()).contains(query) || String.valueOf(credit.getIdCompte()).contains(query) || this.safeLower(credit.getTypeCredit()).contains(query) || this.safeLower(credit.getStatut()).contains(query));
            this.updateTableInfo();
        });
        SortedList<Credit> sortedData = new SortedList<>(this.filteredData);
        sortedData.comparatorProperty().bind(this.tableCredits.comparatorProperty());
        this.tableCredits.setItems(sortedData);
    }

    private void setupAutoCalculation() {
        this.txtMontantAccord.textProperty().addListener((obs, oldValue, newValue) -> {
            this.calculateMensualite();
            this.syncMontantRestant();
        });
        this.txtDuree.textProperty().addListener((obs, oldValue, newValue) -> this.calculateMensualite());
        this.txtTauxInteret.textProperty().addListener((obs, oldValue, newValue) -> this.calculateMensualite());
    }

    private void calculateMensualite() {
        try {
            double montant = this.parseDouble(this.txtMontantAccord.getText(), (double)0.0F);
            int duree = (int)this.parseDouble(this.txtDuree.getText(), (double)0.0F);
            double tauxAnnuel = this.parseDouble(this.txtTauxInteret.getText(), (double)0.0F);
            if (montant <= (double)0.0F || duree <= 0) {
                this.txtMensualite.clear();
                return;
            }

            double mensualite;
            if (tauxAnnuel <= (double)0.0F) {
                mensualite = montant / (double)duree;
            } else {
                double tauxMensuel = tauxAnnuel / (double)100.0F / (double)12.0F;
                double numerateur = montant * tauxMensuel * Math.pow((double)1.0F + tauxMensuel, (double)duree);
                double denominateur = Math.pow((double)1.0F + tauxMensuel, (double)duree) - (double)1.0F;
                mensualite = denominateur == (double)0.0F ? (double)0.0F : numerateur / denominateur;
            }

            this.txtMensualite.setText(String.format("%.2f", mensualite));
        } catch (NumberFormatException var14) {
            this.txtMensualite.clear();
        }

    }

    private void syncMontantRestant() {
        if (this.txtMontantRestant.getText() == null || this.txtMontantRestant.getText().isBlank()) {
            this.txtMontantRestant.setText(this.txtMontantAccord.getText());
        }

    }

    private void loadCompteIds() {
        List<Integer> compteIds = this.creditService.getCompteIds();
        this.cmbCompte.setItems(FXCollections.observableArrayList(compteIds));
    }

    private void loadCredits() {
        this.creditsList.setAll(this.creditService.getAllCredits());
        this.updateStats();
        this.updateTableInfo();
    }

    private void updateStats() {
        long accordes = this.creditsList.stream().filter((c) -> {
            String statut = this.safeLower(c.getStatut());
            return statut.contains("accept") || statut.contains("cours") || statut.contains("rembours");
        }).count();
        double total = this.creditsList.stream().map(Credit::getMontantAccord).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
        long enCours = this.creditsList.stream().filter((c) -> this.safeLower(c.getStatut()).contains("cours")).count();
        this.lblCreditsAccordes.setText(String.valueOf(accordes));
        this.lblMontantTotal.setText(this.formatAmount(total));
        this.lblCreditsEnCours.setText(String.valueOf(enCours));
    }

    private void updateTableInfo() {
        int total = this.creditsList.size();
        int filtered = this.filteredData == null ? total : this.filteredData.size();
        this.lblTableInfo.setText(String.format("Affichage de %d sur %d entrees", filtered, total));
    }

    private void populateForm(Credit credit) {
        if (!this.cmbCompte.getItems().contains(credit.getIdCompte())) {
            this.cmbCompte.getItems().add(credit.getIdCompte());
        }

        this.cmbCompte.setValue(credit.getIdCompte());
        this.cmbTypeCredit.setValue(credit.getTypeCredit());
        this.txtMontantDemande.setText(String.valueOf(credit.getMontantDemande()));
        this.txtMontantAccord.setText(credit.getMontantAccord() == null ? "" : String.valueOf(credit.getMontantAccord()));
        this.txtDuree.setText(String.valueOf(credit.getDuree()));
        this.txtTauxInteret.setText(String.valueOf(credit.getTauxInteret()));
        this.txtMensualite.setText(String.valueOf(credit.getMensualite()));
        this.txtMontantRestant.setText(String.valueOf(credit.getMontantRestant()));
        this.dpDateDemande.setValue(this.parseDate(credit.getDateDemande()));
        this.cmbStatut.setValue(credit.getStatut());
    }

   private void clearForm() {
    cmbCompte.setValue(null);
    cmbTypeCredit.setValue(null);
    txtMontantDemande.clear();
    txtMontantAccord.clear();
    txtDuree.clear();
    txtTauxInteret.clear();
    txtMensualite.clear();
    txtMontantRestant.clear();
    dpDateDemande.setValue(null);
    cmbStatut.setValue(null);

    selectedCredit = null;
    isEditMode = false;
    btnAjouter.setText("Ajouter");
    tableCredits.getSelectionModel().clearSelection();
}


    @FXML
    private void handleAjouter() {
        try {
            Integer idCompte = cmbCompte.getValue();
            String type = cmbTypeCredit.getValue();
            String statut = cmbStatut.getValue();
            LocalDate date = dpDateDemande.getValue();

            if (idCompte == null || type == null || statut == null || date == null) {
                this.showAlert(AlertType.ERROR, "Validation", "Selectionnez compte, type, date et statut.");
                return;
            }

            double montantDemande = this.parseRequiredDouble(this.txtMontantDemande.getText(), "Montant demande");
            int duree = (int)this.parseRequiredDouble(this.txtDuree.getText(), "Duree");
            double taux = this.parseRequiredDouble(this.txtTauxInteret.getText(), "Taux interet");
            Double montantAccord = this.parseOptionalDouble(this.txtMontantAccord.getText());
            double mensualite = this.parseDouble(this.txtMensualite.getText(), (double)0.0F);
            double montantRestant = this.parseDouble(this.txtMontantRestant.getText(), montantAccord == null ? (double)0.0F : montantAccord);
            Credit credit = new Credit(0, idCompte, type, montantDemande, montantAccord, duree, taux, mensualite, montantRestant, date.toString(), statut);
            if (this.isEditMode && this.selectedCredit != null) {
                credit.setIdCredit(this.selectedCredit.getIdCredit());
                boolean updated = this.creditService.updateCredit(credit);
                if (!updated) {
                    this.showAlert(AlertType.ERROR, "Erreur", "Echec de modification du credit.");
                    return;
                }

                this.showAlert(AlertType.INFORMATION, "Succes", "Credit modifie.");
            } else {
                this.creditService.addCredit(credit);
                this.showAlert(AlertType.INFORMATION, "Succes", "Credit ajoute.");
            }

            this.clearForm();
            this.loadCredits();
        } catch (IllegalArgumentException ex) {
            this.showAlert(AlertType.ERROR, "Erreur", ex.getMessage());
        } catch (RuntimeException ex) {
            this.showAlert(AlertType.ERROR, "Erreur", "Operation impossible: " + ex.getMessage());
        }

    }

    @FXML
    private void handleSupprimer() {
        if (this.selectedCredit == null) {
            this.showAlert(AlertType.WARNING, "Suppression", "Selectionnez un credit a supprimer.");
        } else {
            this.deleteCredit(this.selectedCredit);
        }
    }

    @FXML
    private void handleAnnuler() {
        this.clearForm();
    }

    private void editCredit(Credit credit) {
        this.selectedCredit = credit;
        this.populateForm(credit);
        this.isEditMode = true;
        this.btnAjouter.setText("Modifier");
    }

    private void deleteCredit(Credit credit) {
        Alert confirm = new Alert(AlertType.CONFIRMATION, "Supprimer le credit #" + credit.getIdCredit() + " ?", new ButtonType[]{ButtonType.OK, ButtonType.CANCEL});
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            boolean deleted = this.creditService.deleteCredit(credit.getIdCredit());
            if (deleted) {
                this.clearForm();
                this.loadCredits();
                this.showAlert(AlertType.INFORMATION, "Succes", "Credit supprime.");
            } else {
                this.showAlert(AlertType.ERROR, "Erreur", "Echec de suppression du credit.");
            }
        }

    }

    @FXML
private void trierParType() {
    this.creditsList.sort(
        Comparator.comparing((Credit c) -> safeLower(c.getTypeCredit()))
    );
}

@FXML
private void trierParMontant() {
    this.creditsList.sort(
        Comparator.comparingDouble((Credit c) -> c.getMontantDemande()).reversed()
    );
}

@FXML
private void trierParDate() {
    this.creditsList.sort(
        Comparator.comparing(
            (Credit c) -> parseDate(c.getDateDemande()), 
            Comparator.nullsLast(Comparator.naturalOrder())
        ).reversed()
    );
}

@FXML
private void trierParStatut() {
    this.creditsList.sort(
        Comparator.comparing((Credit c) -> safeLower(c.getStatut()))
    );
}

    @FXML
    private void exporterPDF() {
        List<Credit> rows = new ArrayList<>(this.tableCredits.getItems());
        if (rows.isEmpty()) {
            this.showAlert(AlertType.WARNING, "Export", "Aucune donnee a exporter.");
        } else {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exporter les credits en PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", new String[]{"*.pdf"}));
            LocalDateTime var10001 = LocalDateTime.now();
            chooser.setInitialFileName("credits_" + var10001.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
            File file = chooser.showSaveDialog(this.tableCredits.getScene().getWindow());
            if (file != null) {
                try {
                    PdfReportUtil.exportCredits(rows, file);
                    this.showAlert(AlertType.INFORMATION, "Export", "PDF genere: " + file.getAbsolutePath());
                } catch (IOException ex) {
                    this.showAlert(AlertType.ERROR, "Export", "Echec export PDF: " + ex.getMessage());
                }

            }
        }
    }

    @FXML
    private void envoyerSMS() {
        this.showAlert(AlertType.INFORMATION, "SMS", "Fonction en cours de developpement.");
    }

    private double parseDouble(String value, double defaultValue) {
        return value != null && !value.isBlank() ? Double.parseDouble(value.trim().replace(",", ".")) : defaultValue;
    }

    private double parseRequiredDouble(String value, String fieldName) {
        if (value != null && !value.isBlank()) {
            return this.parseDouble(value, (double)0.0F);
        } else {
            throw new IllegalArgumentException(fieldName + " est obligatoire.");
        }
    }

    private Double parseOptionalDouble(String value) {
        return value != null && !value.isBlank() ? this.parseDouble(value, (double)0.0F) : null;
    }

    private LocalDate parseDate(String value) {
        if (value != null && !value.isBlank()) {
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException var3) {
                return null;
            }
        } else {
            return null;
        }
    }

    private String formatDateForTable(String value) {
        LocalDate date = this.parseDate(value);
        if (date == null) {
            return value == null ? "" : value;
        } else {
            return date.format(TABLE_DATE_FORMAT);
        }
    }

    private String formatAmount(double value) {
        return String.format("%,.2f DT", value);
    }

    private String formatPercent(double value) {
        return String.format("%.2f%%", value);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText((String)null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
