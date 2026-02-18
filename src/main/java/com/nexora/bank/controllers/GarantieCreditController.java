//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.nexora.bank.controllers;

import com.nexora.bank.Models.GarantieCredit;
import com.nexora.bank.Service.GarentieCreditService;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

public class GarantieCreditController implements Initializable {
    private static final DateTimeFormatter TABLE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    @FXML
    private Label lblNombreGaranties;
    @FXML
    private Label lblValeurTotale;
    @FXML
    private Label lblGarantiesActives;
    @FXML
    private ComboBox<Integer> cmbCredit;
    @FXML
    private ComboBox<String> cmbTypeGarantie;
    @FXML
    private ComboBox<String> cmbStatut;
    @FXML
    private TextArea txtDescription;
    @FXML
    private TextField txtAdresseBien;
    @FXML
    private TextField txtValeurEstimee;
    @FXML
    private TextField txtValeurRetenue;
    @FXML
    private TextField txtDocumentJustificatif;
    @FXML
    private TextField txtNomGarant;
    @FXML
    private TextField txtRecherche;
    @FXML
    private DatePicker dpDateEvaluation;
    @FXML
    private Button btnAjouter;
    @FXML
    private TableView<GarantieCredit> tableGaranties;
    @FXML
    private TableColumn<GarantieCredit, String> colCredit;
    @FXML
    private TableColumn<GarantieCredit, String> colType;
    @FXML
    private TableColumn<GarantieCredit, String> colValeurEstimee;
    @FXML
    private TableColumn<GarantieCredit, String> colValeurRetenue;
    @FXML
    private TableColumn<GarantieCredit, String> colAdresse;
    @FXML
    private TableColumn<GarantieCredit, String> colGarant;
    @FXML
    private TableColumn<GarantieCredit, String> colDate;
    @FXML
    private TableColumn<GarantieCredit, String> colStatut;
    @FXML
    private TableColumn<GarantieCredit, Void> colActions;
    @FXML
    private Label lblTableInfo;
    private final GarentieCreditService garantieService = new GarentieCreditService();
    private final ObservableList<GarantieCredit> garantiesList = FXCollections.observableArrayList();
    private FilteredList<GarantieCredit> filteredData;
    private GarantieCredit selectedGarantie;
    private boolean isEditMode;

    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.initializeTable();
        this.initializeSearch();
        this.loadCreditIds();
        this.loadGaranties();
    }

    private void initializeTable() {
        this.colCredit.setCellValueFactory((cell) -> new SimpleStringProperty(String.valueOf(((GarantieCredit)cell.getValue()).getIdCredit())));
        this.colType.setCellValueFactory(new PropertyValueFactory("typeGarantie"));
        this.colValeurEstimee.setCellValueFactory((cell) -> new SimpleStringProperty(this.formatAmount(((GarantieCredit)cell.getValue()).getValeurEstimee())));
        this.colValeurRetenue.setCellValueFactory((cell) -> new SimpleStringProperty(this.formatAmount(((GarantieCredit)cell.getValue()).getValeurRetenue())));
        this.colAdresse.setCellValueFactory(new PropertyValueFactory("adresseBien"));
        this.colGarant.setCellValueFactory(new PropertyValueFactory("nomGarant"));
        this.colDate.setCellValueFactory((cell) -> new SimpleStringProperty(this.formatDateForTable(((GarantieCredit)cell.getValue()).getDateEvaluation())));
        this.colStatut.setCellValueFactory(new PropertyValueFactory("statut"));
        this.colStatut.setCellFactory((col) -> new TableCell<GarantieCredit, String>() {
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty && item != null) {
                        Label badge = new Label(item);
                        badge.getStyleClass().add("nx-badge");
                        String lowered = item.toLowerCase();
                        if (lowered.contains("accept")) {
                            badge.getStyleClass().add("nx-badge-success");
                        } else if (!lowered.contains("attente") && !lowered.contains("cours")) {
                            if (lowered.contains("refus") || lowered.contains("annul")) {
                                badge.getStyleClass().add("nx-badge-error");
                            }
                        } else {
                            badge.getStyleClass().add("nx-badge-warning");
                        }

                        this.setGraphic(badge);
                    } else {
                        this.setGraphic((Node)null);
                    }
                }
            });
        this.colActions.setCellFactory((col) -> new TableCell<GarantieCredit, Void>() {
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
                    this.editButton.setOnAction((event) -> GarantieCreditController.this.editGarantie((GarantieCredit)this.getTableView().getItems().get(this.getIndex())));
                    this.deleteButton.setOnAction((event) -> GarantieCreditController.this.deleteGarantie((GarantieCredit)this.getTableView().getItems().get(this.getIndex())));
                }

                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    this.setGraphic(empty ? null : this.box);
                }
            });
        this.tableGaranties.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                this.editGarantie(newValue);
            }

        });
    }

    private void initializeSearch() {
        this.filteredData = new FilteredList<>(this.garantiesList, g -> true);

        this.txtRecherche.textProperty().addListener((obs, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase();
            this.filteredData.setPredicate((garantie) -> query.isEmpty() || String.valueOf(garantie.getIdGarantie()).contains(query) || String.valueOf(garantie.getIdCredit()).contains(query) || this.safeLower(garantie.getTypeGarantie()).contains(query) || this.safeLower(garantie.getNomGarant()).contains(query) || this.safeLower(garantie.getStatut()).contains(query));
            this.updateTableInfo();
        });
        SortedList<GarantieCredit> sortedData = new SortedList<>(this.filteredData);

        sortedData.comparatorProperty().bind(this.tableGaranties.comparatorProperty());
        this.tableGaranties.setItems(sortedData);
    }

    private void loadCreditIds() {
        List<Integer> creditIds = this.garantieService.getCreditIds();
        this.cmbCredit.setItems(FXCollections.observableArrayList(creditIds));
    }

    private void loadGaranties() {
        this.garantiesList.setAll(this.garantieService.getAllGaranties());
        this.updateStats();
        this.updateTableInfo();
    }

    private void updateStats() {
        this.lblNombreGaranties.setText(String.valueOf(this.garantiesList.size()));
        double totalValeur = this.garantiesList.stream().mapToDouble(GarantieCredit::getValeurRetenue).sum();
        this.lblValeurTotale.setText(this.formatAmount(totalValeur));
        long garantiesActives = this.garantiesList.stream().filter((g) -> this.safeLower(g.getStatut()).contains("accept")).count();
        this.lblGarantiesActives.setText(String.valueOf(garantiesActives));
    }

    private void updateTableInfo() {
        int total = this.garantiesList.size();
        int filtered = this.filteredData == null ? total : this.filteredData.size();
        this.lblTableInfo.setText(String.format("Affichage de %d sur %d entrees", filtered, total));
    }

    private void populateForm(GarantieCredit garantie) {
        if (!this.cmbCredit.getItems().contains(garantie.getIdCredit())) {
            this.cmbCredit.getItems().add(garantie.getIdCredit());
        }

        this.cmbCredit.setValue(garantie.getIdCredit());
        this.cmbTypeGarantie.setValue(garantie.getTypeGarantie());
        this.txtDescription.setText(garantie.getDescription());
        this.txtAdresseBien.setText(garantie.getAdresseBien());
        this.txtValeurEstimee.setText(String.valueOf(garantie.getValeurEstimee()));
        this.txtValeurRetenue.setText(String.valueOf(garantie.getValeurRetenue()));
        this.txtDocumentJustificatif.setText(garantie.getDocumentJustificatif());
        this.dpDateEvaluation.setValue(this.parseDate(garantie.getDateEvaluation()));
        this.txtNomGarant.setText(garantie.getNomGarant());
        this.cmbStatut.setValue(garantie.getStatut());
    }

    private void clearForm() {
    cmbCredit.setValue(null);
    cmbTypeGarantie.setValue(null);
    txtDescription.clear();
    txtAdresseBien.clear();
    txtValeurEstimee.clear();
    txtValeurRetenue.clear();
    txtDocumentJustificatif.clear();
    dpDateEvaluation.setValue(null);
    txtNomGarant.clear();
    cmbStatut.setValue(null);

    selectedGarantie = null;
    isEditMode = false;
    btnAjouter.setText("Ajouter");
    tableGaranties.getSelectionModel().clearSelection();
}


    @FXML
    private void handleAjouter() {
        try {
            Integer idCredit = cmbCredit.getValue();
            String type = cmbTypeGarantie.getValue();
            String statut = cmbStatut.getValue();
            LocalDate date = dpDateEvaluation.getValue();

            if (idCredit == null || type == null || statut == null || date == null) {
                this.showAlert(AlertType.ERROR, "Validation", "Selectionnez credit, type, date et statut.");
                return;
            }

            String description = this.safeText(this.txtDescription.getText());
            String adresse = this.safeText(this.txtAdresseBien.getText());
            double valeurEstimee = this.parseRequiredDouble(this.txtValeurEstimee.getText(), "Valeur estimee");
            double valeurRetenue = this.parseDouble(this.txtValeurRetenue.getText(), valeurEstimee);
            String document = this.safeText(this.txtDocumentJustificatif.getText());
            String garant = this.safeText(this.txtNomGarant.getText());
            GarantieCredit garantie = new GarantieCredit(idCredit, type, description, adresse, valeurEstimee, valeurRetenue, document, date.toString(), garant, statut);
            if (this.isEditMode && this.selectedGarantie != null) {
                garantie.setIdGarantie(this.selectedGarantie.getIdGarantie());
                boolean updated = this.garantieService.updateGarantie(garantie);
                if (!updated) {
                    this.showAlert(AlertType.ERROR, "Erreur", "Echec de modification de la garantie.");
                    return;
                }

                this.showAlert(AlertType.INFORMATION, "Succes", "Garantie modifiee.");
            } else {
                this.garantieService.addGarantie(garantie);
                this.showAlert(AlertType.INFORMATION, "Succes", "Garantie ajoutee.");
            }

            this.clearForm();
            this.loadGaranties();
            this.loadCreditIds();
        } catch (IllegalArgumentException ex) {
            this.showAlert(AlertType.ERROR, "Erreur", ex.getMessage());
        } catch (RuntimeException ex) {
            this.showAlert(AlertType.ERROR, "Erreur", "Operation impossible: " + ex.getMessage());
        }

    }

    @FXML
    private void handleSupprimer() {
        if (this.selectedGarantie == null) {
            this.showAlert(AlertType.WARNING, "Suppression", "Selectionnez une garantie a supprimer.");
        } else {
            this.deleteGarantie(this.selectedGarantie);
        }
    }

    @FXML
    private void handleAnnuler() {
        this.clearForm();
    }

    private void editGarantie(GarantieCredit garantie) {
        this.selectedGarantie = garantie;
        this.populateForm(garantie);
        this.isEditMode = true;
        this.btnAjouter.setText("Modifier");
    }

    private void deleteGarantie(GarantieCredit garantie) {
        Alert confirm = new Alert(AlertType.CONFIRMATION, "Supprimer la garantie #" + garantie.getIdGarantie() + " ?", new ButtonType[]{ButtonType.OK, ButtonType.CANCEL});
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            boolean deleted = this.garantieService.deleteGarantie(garantie.getIdGarantie());
            if (deleted) {
                this.clearForm();
                this.loadGaranties();
                this.showAlert(AlertType.INFORMATION, "Succes", "Garantie supprimee.");
            } else {
                this.showAlert(AlertType.ERROR, "Erreur", "Echec de suppression de la garantie.");
            }
        }

    }

    @FXML
    private void trierParType() {
        this.garantiesList.sort(Comparator.comparing((g) -> this.safeLower(g.getTypeGarantie())));
    }

    @FXML
    private void trierParValeur() {
        this.garantiesList.sort(Comparator.comparing(GarantieCredit::getValeurRetenue).reversed());
    }

    @FXML
private void trierParDate() {
    this.garantiesList.sort(
        Comparator.comparing(
            (GarantieCredit g) -> this.parseDate(g.getDateEvaluation()),
            Comparator.nullsLast(Comparator.naturalOrder())
        ).reversed()
    );
}


    @FXML
    private void trierParStatut() {
        this.garantiesList.sort(Comparator.comparing((g) -> this.safeLower(g.getStatut())));
    }

    @FXML
    private void exporterPDF() {
      List<GarantieCredit> rows = new ArrayList<>(this.tableGaranties.getItems());

        if (rows.isEmpty()) {
            this.showAlert(AlertType.WARNING, "Export", "Aucune donnee a exporter.");
        } else {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exporter les garanties en PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", new String[]{"*.pdf"}));
            LocalDateTime var10001 = LocalDateTime.now();
            chooser.setInitialFileName("garanties_" + var10001.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
            File file = chooser.showSaveDialog(this.tableGaranties.getScene().getWindow());
            if (file != null) {
                try {
                    PdfReportUtil.exportGaranties(rows, file);
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

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText((String)null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
