package com.nexora.bank.controllers;

import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.Transaction;
import com.nexora.bank.Service.TransactionService;
import com.nexora.bank.Utils.EmailUtil;
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
import java.time.LocalDate;
import java.util.ResourceBundle;

public class TransactionController implements Initializable {

    private final TransactionService service = new TransactionService();

    @FXML private Button btnAjouter;
    @FXML private Button btnAnnuler;
    @FXML private Button btnSupprimer;
    @FXML private ComboBox<String> cmbCategorie;
    @FXML private ComboBox<String> cmbStatutTransaction;
    @FXML private ComboBox<String> cmbTypeTransaction;

    @FXML private TableColumn<Transaction, String>    colCategorie;
    @FXML private TableColumn<Transaction, LocalDate> colDate;
    @FXML private TableColumn<Transaction, Double>    colMontant;
    @FXML private TableColumn<Transaction, String>    colType;
    @FXML private TableColumn<Transaction, String>    colStatut;
    @FXML private TableColumn<Transaction, String>    colDescription;
    @FXML private TableColumn<Transaction, Void>      colActions;

    @FXML private DatePicker           dpDateTransaction;
    @FXML private Label                lblTotalTransactions;
    @FXML private Label                lblTableInfo;
    @FXML private TableView<Transaction> tableTransactions;
    @FXML private TextField            txtMontant;
    @FXML private TextArea             txtDescription;
    @FXML private TextField            txtRecherche;

    // ─── Pagination ────────────────────────────────────────────────────────
    @FXML private HBox  paginationBar;   // conteneur HBox défini dans le FXML (optionnel)
    // Si paginationBar est null (pas dans le FXML), on la crée et on l'ajoute dynamiquement.

    private static final int PAGE_SIZE = 3;
    private int currentPage = 0;

    // ─── Labels erreur ─────────────────────────────────────────────────────
    @FXML private Label lblCategorieError;
    @FXML private Label lblDateError;
    @FXML private Label lblMontantError;
    @FXML private Label lblTypeError;
    @FXML private Label lblStatutError;
    @FXML private Label lblDescriptionError;

    private final ObservableList<Transaction> transactionsList = FXCollections.observableArrayList();
    private FilteredList<Transaction>         filteredData;
    private ObservableList<Transaction>       pageData;   // données affichées sur la page courante

    private Transaction selectedTransaction = null;
    private boolean     isEditMode          = false;

    // ══════════════════════════════════════════════════════════════════════
    // Initialisation
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        initializeSearch();
        setupTableSelection();
        setupRealTimeValidation();
        refreshData();
    }

    private void initializeTable() {
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateTransaction"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeTransaction"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutTransaction"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏");
            private final Button btnDelete = new Button("🗑");
            private final HBox   hbox      = new HBox(8, btnEdit, btnDelete);

            {
                hbox.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e   -> editTransaction(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> deleteTransaction(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });

        // pageData est la liste directement liée au TableView
        pageData = FXCollections.observableArrayList();
        tableTransactions.setItems(pageData);
    }

    private void initializeSearch() {
        filteredData = new FilteredList<>(transactionsList, t -> true);

        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(t -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String f = newVal.toLowerCase();
                return (t.getCategorie()         != null && t.getCategorie().toLowerCase().contains(f))
                    || (t.getStatutTransaction() != null && t.getStatutTransaction().toLowerCase().contains(f))
                    || (t.getTypeTransaction()   != null && t.getTypeTransaction().toLowerCase().contains(f));
            });
            currentPage = 0;
            applyPage();
        });
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

    // ══════════════════════════════════════════════════════════════════════
    // Pagination
    // ══════════════════════════════════════════════════════════════════════

    /** Met à jour pageData selon currentPage et rafraîchit la barre de pagination */
    private void applyPage() {
        int total      = filteredData.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));

        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0)          currentPage = 0;

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        pageData.setAll(filteredData.subList(from, to));

        updateTableInfo();
        refreshPaginationBar(totalPages, total);
    }

    /** Reconstruit la barre de pagination et l'injecte dans paginationBar (HBox FXML) */
    private void refreshPaginationBar(int totalPages, int totalItems) {
        if (paginationBar == null) return;

        paginationBar.getChildren().clear();
        paginationBar.setAlignment(Pos.CENTER);
        paginationBar.setSpacing(6);

        int from = currentPage * PAGE_SIZE + 1;
        int to   = Math.min(from + PAGE_SIZE - 1, totalItems);

        // Info "X–Y sur N"
        Label info = new Label(totalItems == 0 ? "Aucun résultat"
                                               : from + "–" + to + " sur " + totalItems);
        info.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;-fx-padding:0 10 0 0;");

        // Bouton ◀
        Button btnPrev = buildNavBtn("◀", currentPage > 0);
        btnPrev.setOnAction(e -> { currentPage--; applyPage(); });

        // Numéros de page
        HBox nums = new HBox(4);
        nums.setAlignment(Pos.CENTER);
        for (int i = 0; i < totalPages; i++) {
            final int idx    = i;
            boolean   active = (i == currentPage);
            Label     lbl    = new Label(String.valueOf(i + 1));
            lbl.setMinSize(28, 28); lbl.setMaxSize(28, 28);
            lbl.setAlignment(Pos.CENTER);

            String styleActive  = "-fx-background-color:#4f46e5;-fx-text-fill:#fff;" +
                                  "-fx-background-radius:7;-fx-font-size:11px;-fx-font-weight:700;";
            String styleNormal  = "-fx-background-color:#e2e8f0;-fx-text-fill:#1e293b;" +
                                  "-fx-background-radius:7;-fx-font-size:11px;-fx-cursor:hand;";
            String styleHover   = "-fx-background-color:#c7d2fe;-fx-text-fill:#4f46e5;" +
                                  "-fx-background-radius:7;-fx-font-size:11px;-fx-cursor:hand;";

            lbl.setStyle(active ? styleActive : styleNormal);

            if (!active) {
                lbl.setOnMouseEntered(ev -> lbl.setStyle(styleHover));
                lbl.setOnMouseExited(ev  -> lbl.setStyle(styleNormal));
                lbl.setOnMouseClicked(ev -> { currentPage = idx; applyPage(); });
            }
            nums.getChildren().add(lbl);
        }

        // Bouton ▶
        Button btnNext = buildNavBtn("▶", currentPage < totalPages - 1);
        btnNext.setOnAction(e -> { currentPage++; applyPage(); });

        paginationBar.getChildren().addAll(info, btnPrev, nums, btnNext);
    }

    private Button buildNavBtn(String label, boolean enabled) {
        Button btn = new Button(label);
        btn.setMinSize(28, 28); btn.setMaxSize(28, 28);
        btn.setDisable(!enabled);
        String base  = "-fx-background-color:#e2e8f0;-fx-text-fill:#1e293b;" +
                       "-fx-background-radius:7;-fx-font-size:10px;-fx-cursor:hand;-fx-border-width:0;";
        String muted = "-fx-background-color:#f1f5f9;-fx-text-fill:#94a3b8;" +
                       "-fx-background-radius:7;-fx-font-size:10px;-fx-border-width:0;";
        String hover = "-fx-background-color:#c7d2fe;-fx-text-fill:#4f46e5;" +
                       "-fx-background-radius:7;-fx-font-size:10px;-fx-cursor:hand;-fx-border-width:0;";
        btn.setStyle(enabled ? base : muted);
        if (enabled) {
            btn.setOnMouseEntered(e -> btn.setStyle(hover));
            btn.setOnMouseExited(e  -> btn.setStyle(base));
        }
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Données
    // ══════════════════════════════════════════════════════════════════════

    private void refreshData() {
        transactionsList.setAll(service.getAll());
        currentPage = 0;
        applyPage();
        updateStats();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Validation
    // ══════════════════════════════════════════════════════════════════════

    private void setupRealTimeValidation() {
        cmbCategorie.valueProperty().addListener((obs, ov, v) -> {
            if (v != null && !v.trim().isEmpty()) setSuccess(lblCategorieError, "✓ Catégorie valide");
            else setError(lblCategorieError, "❌ La catégorie est obligatoire.");
        });

        dpDateTransaction.valueProperty().addListener((obs, ov, v) -> {
            if (v == null)                              setError(lblDateError, "❌ La date est obligatoire.");
            else if (v.isAfter(LocalDate.now()))        setError(lblDateError, "❌ La date ne peut pas être dans le futur.");
            else if (v.isBefore(LocalDate.now().minusYears(10))) setError(lblDateError, "❌ La date est trop ancienne (max 10 ans).");
            else setSuccess(lblDateError, "✓ Date valide");
        });

        txtMontant.textProperty().addListener((obs, ov, v) -> {
            if (v.trim().isEmpty()) { setError(lblMontantError, "❌ Le montant est obligatoire."); return; }
            try {
                double m = Double.parseDouble(v.trim());
                if (m <= 0)       setError(lblMontantError, "❌ Le montant doit être positif (> 0).");
                else if (m > 1e6) setError(lblMontantError, "❌ Le montant est trop élevé (max 1 000 000 DT).");
                else              setSuccess(lblMontantError, "✓ Montant valide");
            } catch (NumberFormatException e) {
                setError(lblMontantError, "❌ Format invalide. Utilisez des chiffres (ex: 1500.50).");
            }
        });

        cmbTypeTransaction.valueProperty().addListener((obs, ov, v) -> {
            if (v != null && !v.trim().isEmpty()) setSuccess(lblTypeError, "✓ Type valide");
            else setError(lblTypeError, "❌ Le type de transaction est obligatoire.");
        });

        cmbStatutTransaction.valueProperty().addListener((obs, ov, v) -> {
            if (v != null && !v.trim().isEmpty()) setSuccess(lblStatutError, "✓ Statut valide");
            else setError(lblStatutError, "❌ Le statut est obligatoire.");
        });

        txtDescription.textProperty().addListener((obs, ov, v) -> {
            String t = v.trim();
            if (t.isEmpty())        lblDescriptionError.setText("");
            else if (t.length() < 5)   setError(lblDescriptionError, "❌ La description doit contenir au moins 5 caractères.");
            else if (t.length() > 500) setError(lblDescriptionError, "❌ La description est trop longue (max 500 caractères).");
            else setSuccess(lblDescriptionError, "✓ Description valide (" + t.length() + "/500 caractères)");
        });
    }

    private boolean validateForm() {
        boolean valid = true;
        clearErrorLabels();

        if (cmbCategorie.getValue() == null || cmbCategorie.getValue().trim().isEmpty())
            { setError(lblCategorieError, "❌ La catégorie est obligatoire."); valid = false; }

        LocalDate date = dpDateTransaction.getValue();
        if (date == null)
            { setError(lblDateError, "❌ La date est obligatoire."); valid = false; }
        else if (date.isAfter(LocalDate.now()))
            { setError(lblDateError, "❌ La date ne peut pas être dans le futur."); valid = false; }
        else if (date.isBefore(LocalDate.now().minusYears(10)))
            { setError(lblDateError, "❌ La date est trop ancienne (max 10 ans)."); valid = false; }

        String mt = txtMontant.getText().trim();
        if (mt.isEmpty()) { setError(lblMontantError, "❌ Le montant est obligatoire."); valid = false; }
        else {
            try {
                double m = Double.parseDouble(mt);
                if (m <= 0)       { setError(lblMontantError, "❌ Le montant doit être positif (> 0)."); valid = false; }
                else if (m > 1e6) { setError(lblMontantError, "❌ Le montant est trop élevé (max 1 000 000 DT)."); valid = false; }
            } catch (NumberFormatException e) {
                setError(lblMontantError, "❌ Format invalide. Utilisez des chiffres (ex: 1500.50)."); valid = false;
            }
        }

        if (cmbTypeTransaction.getValue() == null || cmbTypeTransaction.getValue().trim().isEmpty())
            { setError(lblTypeError, "❌ Le type de transaction est obligatoire."); valid = false; }

        if (cmbStatutTransaction.getValue() == null || cmbStatutTransaction.getValue().trim().isEmpty())
            { setError(lblStatutError, "❌ Le statut est obligatoire."); valid = false; }

        String desc = txtDescription.getText().trim();
        if (!desc.isEmpty() && desc.length() < 5)
            { setError(lblDescriptionError, "❌ La description doit contenir au moins 5 caractères."); valid = false; }
        else if (desc.length() > 500)
            { setError(lblDescriptionError, "❌ La description est trop longue (max 500 caractères)."); valid = false; }

        return valid;
    }

    private void setError(Label label, String message) {
        if (label != null) { label.setText(message); label.setStyle("-fx-text-fill:#ef4444;-fx-font-size:12px;-fx-font-weight:bold;"); }
    }

    private void setSuccess(Label label, String message) {
        if (label != null) { label.setText(message); label.setStyle("-fx-text-fill:#22c55e;-fx-font-size:12px;-fx-font-weight:bold;"); }
    }

    private void clearErrorLabels() {
        for (Label l : new Label[]{lblCategorieError, lblDateError, lblMontantError,
                                    lblTypeError, lblStatutError, lblDescriptionError})
            if (l != null) l.setText("");
    }

    // ══════════════════════════════════════════════════════════════════════
    // CRUD handlers
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    void handleAjouter(ActionEvent event) {
        if (!validateForm()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez corriger les erreurs en rouge dans le formulaire.",
                      ButtonType.OK).showAndWait();
            return;
        }

        int       idUser    = AuthSession.getCurrentUser().getIdUser();
        String    emailUser = AuthSession.getCurrentUser().getEmail();
        String    categorie = cmbCategorie.getValue();
        LocalDate date      = dpDateTransaction.getValue();
        double    montant   = Double.parseDouble(txtMontant.getText().trim());
        String    type      = cmbTypeTransaction.getValue();
        String    statut    = cmbStatutTransaction.getValue();
        String    desc      = txtDescription.getText().trim();

        if (isEditMode && selectedTransaction != null) {
            service.edit(new Transaction(selectedTransaction.getIdTransaction(),
                    selectedTransaction.getIdUser(), categorie, date, montant, type, statut, desc));
            new Thread(() -> EmailUtil.envoyerConfirmationTransaction(emailUser, categorie, montant, type, statut)).start();
            alert(Alert.AlertType.INFORMATION, "Transaction modifiée avec succès ! Un email de confirmation a été envoyé.");
        } else {
            service.add(new Transaction(idUser, categorie, date, montant, type, statut, desc));
            new Thread(() -> EmailUtil.envoyerConfirmationTransaction(emailUser, categorie, montant, type, statut)).start();
            alert(Alert.AlertType.INFORMATION, "Transaction ajoutée avec succès ! Un email de confirmation a été envoyé.");
        }
        clearForm();
        refreshData();
    }

    @FXML void handleAnnuler(ActionEvent event) { clearForm(); }

    @FXML
    void handleSupprimer(ActionEvent event) {
        if (selectedTransaction != null) {
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                    "Voulez-vous vraiment supprimer cette transaction ?", ButtonType.OK, ButtonType.CANCEL);
            conf.setTitle("Confirmation de suppression");
            conf.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) deleteTransaction(selectedTransaction); });
        } else {
            alert(Alert.AlertType.WARNING, "Veuillez sélectionner une transaction à supprimer.");
        }
    }

    private void editTransaction(Transaction t) {
        selectedTransaction = t; populateForm(t);
        isEditMode = true; btnAjouter.setText("Modifier");
    }

    private void deleteTransaction(Transaction t) {
        service.remove(t); clearForm(); refreshData();
        alert(Alert.AlertType.INFORMATION, "Transaction supprimée avec succès !");
    }

    private void populateForm(Transaction t) {
        cmbCategorie.setValue(t.getCategorie());
        cmbTypeTransaction.setValue(t.getTypeTransaction());
        cmbStatutTransaction.setValue(t.getStatutTransaction());
        txtMontant.setText(String.valueOf(t.getMontant()));
        txtDescription.setText(t.getDescription());
        dpDateTransaction.setValue(t.getDateTransaction());
    }

    private void clearForm() {
        cmbCategorie.setValue(null); cmbTypeTransaction.setValue(null); cmbStatutTransaction.setValue(null);
        txtMontant.clear(); txtDescription.clear(); dpDateTransaction.setValue(null);
        selectedTransaction = null; isEditMode = false; btnAjouter.setText("Ajouter");
        clearErrorLabels();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Stats
    // ══════════════════════════════════════════════════════════════════════

    private void updateStats() {
        if (lblTotalTransactions != null)
            lblTotalTransactions.setText(String.valueOf(transactionsList.size()));
    }

    private void updateTableInfo() {
        if (lblTableInfo == null) return;
        int total      = filteredData.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        int from       = currentPage * PAGE_SIZE + 1;
        int to         = Math.min(from + PAGE_SIZE - 1, total);
        lblTableInfo.setText(total == 0 ? "Aucun résultat"
                : "Affichage " + from + "–" + to + " sur " + total
                  + "  (page " + (currentPage + 1) + "/" + totalPages + ")");
    }

    // ══════════════════════════════════════════════════════════════════════
    // Tri (boutons FXML conservés)
    // ══════════════════════════════════════════════════════════════════════

    @FXML void trierParDateAsc(ActionEvent e) {
        tableTransactions.getSortOrder().clear();
        colDate.setSortType(TableColumn.SortType.ASCENDING);
        tableTransactions.getSortOrder().add(colDate);
        currentPage = 0; applyPage();
    }

    @FXML void trierParDateDesc(ActionEvent e) {
        tableTransactions.getSortOrder().clear();
        colDate.setSortType(TableColumn.SortType.DESCENDING);
        tableTransactions.getSortOrder().add(colDate);
        currentPage = 0; applyPage();
    }

    @FXML void trierParMontantAsc(ActionEvent e) {
        tableTransactions.getSortOrder().clear();
        colMontant.setSortType(TableColumn.SortType.ASCENDING);
        tableTransactions.getSortOrder().add(colMontant);
        currentPage = 0; applyPage();
    }

    @FXML void trierParMontantDesc(ActionEvent e) {
        tableTransactions.getSortOrder().clear();
        colMontant.setSortType(TableColumn.SortType.DESCENDING);
        tableTransactions.getSortOrder().add(colMontant);
        currentPage = 0; applyPage();
    }

    @FXML void trierParStatut(ActionEvent e) {
        tableTransactions.getSortOrder().clear();
        colStatut.setSortType(TableColumn.SortType.ASCENDING);
        tableTransactions.getSortOrder().add(colStatut);
        currentPage = 0; applyPage();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Export PDF
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    void exporterPDF(ActionEvent e) {
        if (transactionsList.isEmpty()) { alert(Alert.AlertType.INFORMATION, "Aucune transaction à exporter !"); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        File file = fc.showSaveDialog(new Stage());
        if (file == null) return;

        try {
            Document doc = new Document(new PdfDocument(new PdfWriter(file.getAbsolutePath())));
            doc.add(new Paragraph("Liste des transactions").setBold().setFontSize(16));
            doc.add(new Paragraph(" "));
            Table tbl = new Table(new float[]{100, 80, 80, 80, 80, 180});
            for (String h : new String[]{"Catégorie","Date","Montant","Type","Statut","Description"})
                tbl.addHeaderCell(new Cell().add(new Paragraph(h)));
            for (Transaction t : filteredData) {
                tbl.addCell(t.getCategorie()         != null ? t.getCategorie()         : "");
                tbl.addCell(t.getDateTransaction()   != null ? t.getDateTransaction().toString() : "");
                tbl.addCell(String.valueOf(t.getMontant()));
                tbl.addCell(t.getTypeTransaction()   != null ? t.getTypeTransaction()   : "");
                tbl.addCell(t.getStatutTransaction() != null ? t.getStatutTransaction() : "");
                tbl.addCell(t.getDescription()       != null ? t.getDescription()       : "");
            }
            doc.add(tbl); doc.close();
            alert(Alert.AlertType.INFORMATION, "PDF exporté avec succès !");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            alert(Alert.AlertType.ERROR, "Impossible de créer le fichier PDF !");
        }
    }

    @FXML void envoyerSMS(ActionEvent e) { alert(Alert.AlertType.INFORMATION, "Fonction SMS non implémentée !"); }

    // ══════════════════════════════════════════════════════════════════════
    // Helper
    // ══════════════════════════════════════════════════════════════════════

    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
}