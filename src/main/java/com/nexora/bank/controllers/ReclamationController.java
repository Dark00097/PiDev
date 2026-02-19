package com.nexora.bank.controllers;

import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.Reclamation;
import com.nexora.bank.Models.Transaction;
import com.nexora.bank.Service.ReclamationService;
import com.nexora.bank.Service.TransactionService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ReclamationController implements Initializable {

    private final ReclamationService service = new ReclamationService();
    private final TransactionService transactionService = new TransactionService();

    @FXML private Button btnAjouter;
    @FXML private Button btnAnnuler;
    @FXML private Button btnSupprimer;
    @FXML private ComboBox<String> cmbTransaction;
    @FXML private ComboBox<String> cmbTypeReclamation;
    @FXML private ComboBox<String> cmbStatut;
    @FXML private DatePicker dpDateReclamation;
    @FXML private TextArea txtDescription;

    @FXML private TableView<Reclamation> tableReclamations;
    @FXML private TableColumn<Reclamation, Integer> colId;
    @FXML private TableColumn<Reclamation, String> colTransaction;
    @FXML private TableColumn<Reclamation, String> colDate;
    @FXML private TableColumn<Reclamation, String> colType;
    @FXML private TableColumn<Reclamation, String> colDescription;
    @FXML private TableColumn<Reclamation, String> colStatut;
    @FXML private TableColumn<Reclamation, Void> colActions;

    @FXML private TextField txtRecherche;
    @FXML private Label lblTableInfo;
    @FXML private Label lblNombreReclamations;
    @FXML private Label lblEnAttente;
    @FXML private Label lblResolues;

    @FXML private Label lblTransactionError;
    @FXML private Label lblDateError;
    @FXML private Label lblTypeError;
    @FXML private Label lblStatutError;
    @FXML private Label lblDescriptionError;

    private final ObservableList<Reclamation> reclamationsList = FXCollections.observableArrayList();
    private FilteredList<Reclamation> filteredData;
    private Reclamation selectedReclamation = null;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        initializeSearch();
        setupTableSelection();
        setupComboTransaction();
        setupRealTimeValidation();
        refreshData();
    }

    private void setupComboTransaction() {
        List<String> categories = transactionService.getCategories();
        cmbTransaction.setItems(FXCollections.observableArrayList(categories));
        cmbTransaction.setPromptText("Sélectionner une catégorie");
    }

    private void initializeTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idReclamation"));

        colTransaction.setCellValueFactory(cellData -> {
            Transaction t = transactionService.getAll().stream()
                    .filter(tr -> tr.getIdTransaction() == cellData.getValue().getIdTransaction())
                    .findFirst().orElse(null);
            return new javafx.beans.property.SimpleStringProperty(
                    t != null ? t.getCategorie() : ""
            );
        });

        colDate.setCellValueFactory(new PropertyValueFactory<>("dateReclamation"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeReclamation"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("status"));

        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnEdit = new Button("✏");
            private final Button btnDelete = new Button("🗑");
            private final HBox hbox = new HBox(8, btnEdit, btnDelete);

            {
                hbox.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> editReclamation(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> deleteReclamation(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void initializeSearch() {
        filteredData = new FilteredList<>(reclamationsList, r -> true);

        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(r -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String f = newVal.toLowerCase();
                return (r.getTypeReclamation() != null && r.getTypeReclamation().toLowerCase().contains(f)) ||
                       (r.getStatus() != null && r.getStatus().toLowerCase().contains(f)) ||
                       (r.getDescription() != null && r.getDescription().toLowerCase().contains(f));
            });
            updateTableInfo();
        });

        SortedList<Reclamation> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(tableReclamations.comparatorProperty());
        tableReclamations.setItems(sorted);
    }

    private void setupTableSelection() {
        tableReclamations.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedReclamation = newSel;
                populateForm(newSel);
                isEditMode = true;
                btnAjouter.setText("Modifier");
            }
        });
    }

    private void setupRealTimeValidation() {
        cmbTransaction.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty())
                setSuccess(lblTransactionError, "✓ Catégorie sélectionnée");
            else
                setError(lblTransactionError, "❌ La catégorie est obligatoire.");
        });

        dpDateReclamation.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) setSuccess(lblDateError, "✓ Date valide");
            else setError(lblDateError, "❌ La date est obligatoire.");
        });

        cmbTypeReclamation.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) setSuccess(lblTypeError, "✓ Type valide");
            else setError(lblTypeError, "❌ Le type est obligatoire.");
        });

        cmbStatut.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) setSuccess(lblStatutError, "✓ Statut valide");
            else setError(lblStatutError, "❌ Le statut est obligatoire.");
        });

        txtDescription.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().length() >= 5) setSuccess(lblDescriptionError, "✓ Description valide");
            else setError(lblDescriptionError, "❌ La description doit contenir au moins 5 caractères.");
        });
    }

    private void refreshData() {
        reclamationsList.setAll(service.getAll());

        long enAttente = reclamationsList.stream()
                .filter(r -> "En attente".equalsIgnoreCase(r.getStatus()))
                .count();
        long resolues = reclamationsList.stream()
                .filter(r -> "Résolue".equalsIgnoreCase(r.getStatus()))
                .count();

        lblNombreReclamations.setText(String.valueOf(reclamationsList.size()));
        lblEnAttente.setText(String.valueOf(enAttente));
        lblResolues.setText(String.valueOf(resolues));

        updateTableInfo();
    }

    private void populateForm(Reclamation r) {
        Transaction t = transactionService.getAll().stream()
                .filter(tr -> tr.getIdTransaction() == r.getIdTransaction())
                .findFirst().orElse(null);
        cmbTransaction.setValue(t != null ? t.getCategorie() : null);
        dpDateReclamation.setValue(java.time.LocalDate.parse(r.getDateReclamation()));
        cmbTypeReclamation.setValue(r.getTypeReclamation());
        cmbStatut.setValue(r.getStatus());
        txtDescription.setText(r.getDescription());
    }

    private void clearForm() {
        cmbTransaction.setValue(null);
        dpDateReclamation.setValue(null);
        cmbTypeReclamation.setValue(null);
        cmbStatut.setValue(null);
        txtDescription.clear();
        selectedReclamation = null;
        isEditMode = false;
        btnAjouter.setText("Ajouter");
        clearErrorLabels();
    }

    private void clearErrorLabels() {
        lblTransactionError.setText("");
        lblDateError.setText("");
        lblTypeError.setText("");
        lblStatutError.setText("");
        lblDescriptionError.setText("");
    }

    private boolean validateForm() {
        boolean valid = true;
        clearErrorLabels();
        if (cmbTransaction.getValue() == null || cmbTransaction.getValue().trim().isEmpty()) {
            setError(lblTransactionError, "❌ La catégorie est obligatoire."); valid = false;
        }
        if (dpDateReclamation.getValue() == null) {
            setError(lblDateError, "❌ La date est obligatoire."); valid = false;
        }
        if (cmbTypeReclamation.getValue() == null || cmbTypeReclamation.getValue().trim().isEmpty()) {
            setError(lblTypeError, "❌ Le type est obligatoire."); valid = false;
        }
        if (cmbStatut.getValue() == null || cmbStatut.getValue().trim().isEmpty()) {
            setError(lblStatutError, "❌ Le statut est obligatoire."); valid = false;
        }
        if (txtDescription.getText().trim().length() < 5) {
            setError(lblDescriptionError, "❌ La description doit contenir au moins 5 caractères."); valid = false;
        }
        return valid;
    }

    @FXML
    void handleAjouter(ActionEvent event) {
        if (!validateForm()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez corriger les erreurs en rouge dans le formulaire.").showAndWait();
            return;
        }

        // ✅ Récupération de l'idUser depuis AuthSession
        int idUser = AuthSession.getCurrentUser().getIdUser();

        String categorieChoisie = cmbTransaction.getValue();
        Transaction transactionLiee = transactionService.getAll().stream()
                .filter(t -> categorieChoisie.equals(t.getCategorie()))
                .findFirst().orElse(null);

        if (transactionLiee == null) {
            new Alert(Alert.AlertType.ERROR, "Aucune transaction trouvée pour cette catégorie.").showAndWait();
            return;
        }

        if (isEditMode && selectedReclamation != null) {
            Reclamation r = new Reclamation(
                    selectedReclamation.getIdReclamation(),
                    selectedReclamation.getIdUser(), // ✅ on garde l'idUser original
                    transactionLiee.getIdTransaction(),
                    dpDateReclamation.getValue().toString(),
                    cmbTypeReclamation.getValue(),
                    txtDescription.getText().trim(),
                    cmbStatut.getValue()
            );
            service.edit(r);

            new Alert(Alert.AlertType.INFORMATION, "Réclamation modifiée avec succès !").showAndWait();
        } else {
            Reclamation r = new Reclamation(
                    idUser, // ✅ idUser de l'utilisateur connecté
                    transactionLiee.getIdTransaction(),
                    dpDateReclamation.getValue().toString(),
                    cmbTypeReclamation.getValue(),
                    txtDescription.getText().trim(),
                    cmbStatut.getValue()
            );
            service.add(r);

            new Alert(Alert.AlertType.INFORMATION, "Réclamation ajoutée avec succès !").showAndWait();
        }

        clearForm();
        refreshData();
    }

    @FXML
    void handleAnnuler(ActionEvent event) { clearForm(); }

    @FXML
    void handleSupprimer(ActionEvent event) {
        if (selectedReclamation != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "Voulez-vous vraiment supprimer cette réclamation ?");
            confirmation.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    service.remove(selectedReclamation);
                    clearForm();
                    refreshData();
                }
            });
        }
    }

    private void editReclamation(Reclamation r) {
        selectedReclamation = r;
        populateForm(r);
        isEditMode = true;
        btnAjouter.setText("Modifier");
    }

    private void deleteReclamation(Reclamation r) {
        service.remove(r);
        clearForm();
        refreshData();
    }

    private void setError(Label label, String message) {
        label.setText(message);
        label.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;");
    }

    private void setSuccess(Label label, String message) {
        label.setText(message);
        label.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12px; -fx-font-weight: bold;");
    }

    private void updateTableInfo() {
        lblTableInfo.setText("Total : " + filteredData.size());
    }

    @FXML
    void exporterPDF(ActionEvent e) {
        if (reclamationsList.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Aucune réclamation à exporter !").showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        File file = fileChooser.showSaveDialog(new Stage());

        if (file != null) {
            try {
                PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);

                document.add(new Paragraph("Liste des réclamations").setBold().setFontSize(16));
                document.add(new Paragraph(" "));

                float[] columnWidths = {50, 100, 80, 100, 200, 80};
                Table table = new Table(columnWidths);

                table.addHeaderCell(new Cell().add(new Paragraph("ID")));
                table.addHeaderCell(new Cell().add(new Paragraph("Catégorie")));
                table.addHeaderCell(new Cell().add(new Paragraph("Date")));
                table.addHeaderCell(new Cell().add(new Paragraph("Type")));
                table.addHeaderCell(new Cell().add(new Paragraph("Description")));
                table.addHeaderCell(new Cell().add(new Paragraph("Statut")));

                for (Reclamation r : filteredData) {
                    table.addCell(String.valueOf(r.getIdReclamation()));
                    Transaction t = transactionService.getAll().stream()
                            .filter(tr -> tr.getIdTransaction() == r.getIdTransaction())
                            .findFirst().orElse(null);
                    table.addCell(t != null ? t.getCategorie() : "");
                    table.addCell(r.getDateReclamation());
                    table.addCell(r.getTypeReclamation());
                    table.addCell(r.getDescription());
                    table.addCell(r.getStatus());
                }

                document.add(table);
                document.close();
                new Alert(Alert.AlertType.INFORMATION, "PDF exporté avec succès !").showAndWait();

            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Impossible de créer le fichier PDF !").showAndWait();
            }
        }
    }

    @FXML private void trierParDate() {}
    @FXML private void trierParType() {}
    @FXML private void trierParStatut() {}
    @FXML private void envoyerSMS() {}
}