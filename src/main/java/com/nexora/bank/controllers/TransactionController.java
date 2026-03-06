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
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class TransactionController implements Initializable {

    private final TransactionService service = new TransactionService();

    // ── Formulaire ─────────────────────────────────────────────────────────
    @FXML private Button           btnAjouter;
    @FXML private Button           btnAnnuler;
    @FXML private Button           btnSupprimer;
    @FXML private ComboBox<String> cmbCategorie;
    @FXML private ComboBox<String> cmbStatutTransaction;
    @FXML private ComboBox<String> cmbTypeTransaction;
    @FXML private DatePicker       dpDateTransaction;
    @FXML private TextField        txtMontant;
    @FXML private TextArea         txtDescription;
    @FXML private TextField        txtRecherche;

    // ── Labels erreur ───────────────────────────────────────────────────────
    @FXML private Label lblCategorieError;
    @FXML private Label lblDateError;
    @FXML private Label lblMontantError;
    @FXML private Label lblTypeError;
    @FXML private Label lblStatutError;
    @FXML private Label lblDescriptionError;

    // ── Table ───────────────────────────────────────────────────────────────
    @FXML private TableView<Transaction>              tableTransactions;
    @FXML private TableColumn<Transaction, String>    colCategorie;
    @FXML private TableColumn<Transaction, String>    colDate;
    @FXML private TableColumn<Transaction, String>    colMontant;
    @FXML private TableColumn<Transaction, String>    colType;
    @FXML private TableColumn<Transaction, String>    colStatut;
    @FXML private TableColumn<Transaction, String>    colDescription;
    @FXML private TableColumn<Transaction, Void>      colActions;

    // ── Stats ───────────────────────────────────────────────────────────────
    @FXML private Label lblTotalTransactions;
    @FXML private Label lblMontantTotal;
    @FXML private Label lblTransactionsJour;
    @FXML private Label lblTableInfo;

    // ── Pagination ──────────────────────────────────────────────────────────
    @FXML private HBox paginationBar;
    private static final int PAGE_SIZE   = 5;
    private              int currentPage = 0;

    // ── Format ─────────────────────────────────────────────────────────────
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── État ────────────────────────────────────────────────────────────────
    private final ObservableList<Transaction> transactionsList = FXCollections.observableArrayList();
    private FilteredList<Transaction>         filteredData;
    private ObservableList<Transaction>       pageData;
    private Transaction                       selectedTransaction = null;
    private boolean                           isEditMode          = false;

    // Tri courant
    private String  sortField     = "date";
    private boolean sortAscending = false;

    // ══════════════════════════════════════════════════════════════════════
    // Initialisation
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
        initSearch();
        setupSelection();
        setupRealTimeValidation();
        refreshData();
    }

    private void initTable() {
        // ── Colonnes texte ────────────────────────────────────────────────
        colCategorie.setCellValueFactory(c ->
            new SimpleStringProperty(nvl(c.getValue().getCategorie(), "—")));

        colDate.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getDateTransaction() != null
                ? c.getValue().getDateTransaction().format(DATE_FMT) : "—"));

        // Montant formaté avec signe + couleur
        colMontant.setCellValueFactory(c -> {
            Transaction t = c.getValue();
            double m = t.getMontant() != null ? t.getMontant() : 0.0;
            boolean credit = "Credit".equalsIgnoreCase(t.getTypeTransaction());
            return new SimpleStringProperty((credit ? "+" : "−") + String.format("%.2f DT", m));
        });
        colMontant.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                boolean credit = item.startsWith("+");
                setStyle("-fx-font-weight:700;-fx-font-size:12px;" +
                         "-fx-text-fill:" + (credit ? "#059669" : "#0A2540") + ";");
            }
        });

        // Type badge
        colType.setCellValueFactory(c ->
            new SimpleStringProperty(nvl(c.getValue().getTypeTransaction(), "—")));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                boolean credit = "Credit".equalsIgnoreCase(item);
                String color = credit ? "#059669" : "#0A2540";
                String bg    = credit ? "#D1FAE5" : "#E2E8F0";
                Label badge = new Label((credit ? "↓ " : "↑ ") + item.toUpperCase());
                badge.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + color + ";" +
                    "-fx-font-size:9px;-fx-font-weight:700;-fx-padding:3 8 3 8;" +
                    "-fx-background-radius:20;-fx-border-color:" + color + "44;" +
                    "-fx-border-radius:20;-fx-border-width:1;");
                setGraphic(badge); setText(null);
            }
        });

        // Statut badge coloré
        colStatut.setCellValueFactory(c ->
            new SimpleStringProperty(nvl(c.getValue().getStatutTransaction(), "—")));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                String color = switch (item.toLowerCase()) {
                    case "valide", "terminee"  -> "#059669";
                    case "en attente"           -> "#D97706";
                    case "echouee", "annulee"   -> "#DC2626";
                    default                     -> "#64748B";
                };
                String icon = switch (item.toLowerCase()) {
                    case "valide", "terminee"  -> "✓ ";
                    case "en attente"           -> "⏳ ";
                    case "echouee", "annulee"   -> "✕ ";
                    default                     -> "• ";
                };
                Label badge = new Label(icon + item.toUpperCase());
                badge.setStyle("-fx-background-color:" + color + "18;-fx-text-fill:" + color + ";" +
                    "-fx-font-size:9px;-fx-font-weight:700;-fx-padding:3 8 3 8;" +
                    "-fx-background-radius:20;-fx-border-color:" + color + "44;" +
                    "-fx-border-radius:20;-fx-border-width:1;");
                setGraphic(badge); setText(null);
            }
        });

        colDescription.setCellValueFactory(c ->
            new SimpleStringProperty(truncate(nvl(c.getValue().getDescription(), "—"), 30)));

        // Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = buildActionBtn("✎", "#059669", "#D1FAE5", "Modifier");
            private final Button btnDel  = buildActionBtn("✕", "#DC2626", "#FEE2E2", "Supprimer");
            private final HBox   hbox    = new HBox(5, btnEdit, btnDel);
            {
                hbox.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> editTransaction(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e  -> deleteTransaction(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });

        pageData = FXCollections.observableArrayList();
        tableTransactions.setItems(pageData);
    }

    private Button buildActionBtn(String icon, String color, String bg, String tip) {
        Button btn = new Button(icon);
        btn.setMinSize(30, 30); btn.setMaxSize(30, 30);
        btn.setAlignment(Pos.CENTER);
        Tooltip t = new Tooltip(tip);
        t.setStyle("-fx-background-color:#0A2540;-fx-text-fill:#F1F5F9;-fx-font-size:10px;-fx-background-radius:6;");
        Tooltip.install(btn, t);
        String base  = "-fx-background-color:" + bg + ";-fx-text-fill:" + color + ";" +
                       "-fx-font-size:13px;-fx-background-radius:999;" +
                       "-fx-border-color:" + color + "44;-fx-border-radius:999;-fx-border-width:1;" +
                       "-fx-cursor:hand;-fx-padding:0;";
        String hover = "-fx-background-color:" + color + ";-fx-text-fill:#FFFFFF;" +
                       "-fx-font-size:13px;-fx-background-radius:999;" +
                       "-fx-border-color:transparent;-fx-border-radius:999;-fx-border-width:1;" +
                       "-fx-cursor:hand;-fx-padding:0;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private void initSearch() {
        filteredData = new FilteredList<>(transactionsList, t -> true);
        if (txtRecherche != null)
            txtRecherche.textProperty().addListener((obs, o, v) -> {
                filteredData.setPredicate(t -> {
                    if (v == null || v.isBlank()) return true;
                    String f = v.toLowerCase();
                    return (t.getCategorie()         != null && t.getCategorie().toLowerCase().contains(f))
                        || (t.getStatutTransaction() != null && t.getStatutTransaction().toLowerCase().contains(f))
                        || (t.getTypeTransaction()   != null && t.getTypeTransaction().toLowerCase().contains(f))
                        || (t.getDescription()       != null && t.getDescription().toLowerCase().contains(f));
                });
                currentPage = 0;
                applyPage();
            });
    }

    private void setupSelection() {
        tableTransactions.getSelectionModel().selectedItemProperty()
            .addListener((obs, o, n) -> {
                if (n != null) { editTransaction(n); }
            });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Pagination
    // ══════════════════════════════════════════════════════════════════════

    private void applyPage() {
        // Tri
        List<Transaction> sorted = filteredData.stream()
            .sorted(buildComparator())
            .toList();

        int total      = sorted.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0)          currentPage = 0;

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);
        pageData.setAll(sorted.subList(from, to));

        updateTableInfo(total, totalPages);
        buildPaginationBar(totalPages, total);
    }

    private Comparator<Transaction> buildComparator() {
        Comparator<Transaction> cmp = switch (sortField) {
            case "montant" -> Comparator.comparingDouble(t -> t.getMontant() != null ? t.getMontant() : 0.0);
            case "statut"  -> Comparator.comparing(t -> nvl(t.getStatutTransaction(), ""));
            default        -> Comparator.comparing(t -> t.getDateTransaction() != null
                ? t.getDateTransaction() : LocalDate.MIN);
        };
        return sortAscending ? cmp : cmp.reversed();
    }

    private void buildPaginationBar(int totalPages, int totalItems) {
        if (paginationBar == null) return;
        paginationBar.getChildren().clear();
        paginationBar.setAlignment(Pos.CENTER);
        paginationBar.setSpacing(5);

        // Bouton ◀
        Button btnPrev = buildPageNavBtn("◀", currentPage > 0);
        btnPrev.setOnAction(e -> { currentPage--; applyPage(); });

        // Numéros
        HBox nums = new HBox(4);
        nums.setAlignment(Pos.CENTER);
        for (int i = 0; i < totalPages; i++) {
            final int idx    = i;
            boolean   active = (i == currentPage);
            Label     lbl    = new Label(String.valueOf(i + 1));
            lbl.setMinSize(28, 28); lbl.setMaxSize(28, 28);
            lbl.setAlignment(Pos.CENTER);
            String styleActive = "-fx-background-color:#059669;-fx-text-fill:#FFFFFF;" +
                                 "-fx-background-radius:8;-fx-font-size:11px;-fx-font-weight:700;" +
                                 "-fx-effect:dropshadow(gaussian,rgba(5,150,105,0.35),6,0,0,1);";
            String styleNormal = "-fx-background-color:#E2E8F0;-fx-text-fill:#0A2540;" +
                                 "-fx-background-radius:8;-fx-font-size:11px;-fx-cursor:hand;";
            String styleHover  = "-fx-background-color:#D1FAE5;-fx-text-fill:#059669;" +
                                 "-fx-background-radius:8;-fx-font-size:11px;-fx-cursor:hand;";
            lbl.setStyle(active ? styleActive : styleNormal);
            if (!active) {
                lbl.setOnMouseEntered(ev -> lbl.setStyle(styleHover));
                lbl.setOnMouseExited(ev  -> lbl.setStyle(styleNormal));
                lbl.setOnMouseClicked(ev -> { currentPage = idx; applyPage(); });
            }
            nums.getChildren().add(lbl);
        }

        // Bouton ▶
        Button btnNext = buildPageNavBtn("▶", currentPage < totalPages - 1);
        btnNext.setOnAction(e -> { currentPage++; applyPage(); });

        // Info
        int from = currentPage * PAGE_SIZE + 1;
        int to   = Math.min(from + PAGE_SIZE - 1, totalItems);
        Label info = new Label(totalItems == 0 ? "Aucun résultat" : from + "–" + to + " / " + totalItems);
        info.setStyle("-fx-font-size:11px;-fx-text-fill:#64748B;-fx-padding:0 6 0 0;");

        paginationBar.getChildren().addAll(info, btnPrev, nums, btnNext);
    }

    private Button buildPageNavBtn(String label, boolean enabled) {
        Button btn = new Button(label);
        btn.setMinSize(28, 28); btn.setMaxSize(28, 28);
        btn.setDisable(!enabled);
        String base  = "-fx-background-color:#E2E8F0;-fx-text-fill:#0A2540;" +
                       "-fx-background-radius:8;-fx-font-size:10px;-fx-cursor:hand;-fx-border-width:0;";
        String muted = "-fx-background-color:#F1F5F9;-fx-text-fill:#94A3B8;" +
                       "-fx-background-radius:8;-fx-font-size:10px;-fx-border-width:0;";
        String hover = "-fx-background-color:#D1FAE5;-fx-text-fill:#059669;" +
                       "-fx-background-radius:8;-fx-font-size:10px;-fx-cursor:hand;-fx-border-width:0;";
        btn.setStyle(enabled ? base : muted);
        if (enabled) {
            btn.setOnMouseEntered(e -> btn.setStyle(hover));
            btn.setOnMouseExited(e  -> btn.setStyle(base));
        }
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Données & Stats
    // ══════════════════════════════════════════════════════════════════════

    private void refreshData() {
        transactionsList.setAll(service.getAll());
        filteredData = new FilteredList<>(transactionsList, t -> true);
        if (txtRecherche != null && !txtRecherche.getText().isBlank()) {
            String v = txtRecherche.getText().toLowerCase();
            filteredData.setPredicate(t ->
                (t.getCategorie()         != null && t.getCategorie().toLowerCase().contains(v))
             || (t.getStatutTransaction() != null && t.getStatutTransaction().toLowerCase().contains(v))
             || (t.getTypeTransaction()   != null && t.getTypeTransaction().toLowerCase().contains(v)));
        }
        currentPage = 0;
        applyPage();
        updateStats();
    }

    private void updateStats() {
        int total = transactionsList.size();
        if (lblTotalTransactions != null)
            lblTotalTransactions.setText(String.valueOf(total));

        if (lblMontantTotal != null) {
            double sum = transactionsList.stream()
                .mapToDouble(t -> t.getMontant() != null ? t.getMontant() : 0.0)
                .sum();
            lblMontantTotal.setText(String.format("%.2f DT", sum));
        }

        if (lblTransactionsJour != null) {
            long today = transactionsList.stream()
                .filter(t -> LocalDate.now().equals(t.getDateTransaction()))
                .count();
            lblTransactionsJour.setText(String.valueOf(today));
        }
    }

    private void updateTableInfo(int total, int totalPages) {
        if (lblTableInfo == null) return;
        int from = currentPage * PAGE_SIZE + 1;
        int to   = Math.min(from + PAGE_SIZE - 1, total);
        lblTableInfo.setText(total == 0 ? "Aucune transaction"
            : total + " transaction" + (total > 1 ? "s" : ""));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Tri
    // ══════════════════════════════════════════════════════════════════════

    @FXML void trierParDateAsc(ActionEvent e)    { sortField = "date";    sortAscending = true;  currentPage = 0; applyPage(); }
    @FXML void trierParDateDesc(ActionEvent e)   { sortField = "date";    sortAscending = false; currentPage = 0; applyPage(); }
    @FXML void trierParMontantAsc(ActionEvent e) { sortField = "montant"; sortAscending = true;  currentPage = 0; applyPage(); }
    @FXML void trierParMontantDesc(ActionEvent e){ sortField = "montant"; sortAscending = false; currentPage = 0; applyPage(); }
    @FXML void trierParStatut(ActionEvent e)     { sortField = "statut";  sortAscending = true;  currentPage = 0; applyPage(); }

    // ══════════════════════════════════════════════════════════════════════
    // CRUD
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    void handleAjouter(ActionEvent event) {
        if (!validateForm()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Corrigez les erreurs du formulaire.");
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
            showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Transaction modifiée !");
        } else {
            service.add(new Transaction(idUser, categorie, date, montant, type, statut, desc));
            new Thread(() -> EmailUtil.envoyerConfirmationTransaction(emailUser, categorie, montant, type, statut)).start();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Transaction ajoutée !");
        }
        clearForm();
        refreshData();
    }

    @FXML void handleAnnuler(ActionEvent e) { clearForm(); }

    @FXML
    void handleSupprimer(ActionEvent e) {
        if (selectedTransaction == null) {
            showAlert(Alert.AlertType.WARNING, "Info", "Sélectionnez une transaction."); return;
        }
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer cette transaction ?", ButtonType.OK, ButtonType.CANCEL);
        c.setTitle("Confirmation"); c.setHeaderText(null);
        c.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) deleteTransaction(selectedTransaction);
        });
    }

    private void editTransaction(Transaction t) {
        selectedTransaction = t; populateForm(t); isEditMode = true;
        if (btnAjouter != null) btnAjouter.setText("Modifier");
    }

    private void deleteTransaction(Transaction t) {
        service.remove(t); clearForm(); refreshData();
        showAlert(Alert.AlertType.INFORMATION, "Supprimé", "Transaction supprimée !");
    }

    private void populateForm(Transaction t) {
        if (cmbCategorie       != null) cmbCategorie.setValue(t.getCategorie());
        if (cmbTypeTransaction != null) cmbTypeTransaction.setValue(t.getTypeTransaction());
        if (cmbStatutTransaction != null) cmbStatutTransaction.setValue(t.getStatutTransaction());
        if (txtMontant         != null) txtMontant.setText(String.valueOf(t.getMontant()));
        if (txtDescription     != null) txtDescription.setText(t.getDescription());
        if (dpDateTransaction  != null) dpDateTransaction.setValue(t.getDateTransaction());
    }

    private void clearForm() {
        if (cmbCategorie         != null) cmbCategorie.setValue(null);
        if (cmbTypeTransaction   != null) cmbTypeTransaction.setValue(null);
        if (cmbStatutTransaction != null) cmbStatutTransaction.setValue(null);
        if (txtMontant           != null) txtMontant.clear();
        if (txtDescription       != null) txtDescription.clear();
        if (dpDateTransaction    != null) dpDateTransaction.setValue(null);
        selectedTransaction = null; isEditMode = false;
        if (btnAjouter != null) btnAjouter.setText("✅ Ajouter");
        clearErrorLabels();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Export PDF
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    void exporterPDF(ActionEvent e) {
        if (transactionsList.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Export", "Aucune transaction !"); return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(new Stage()); if (file == null) return;
        try {
            Document doc = new Document(new PdfDocument(new PdfWriter(file.getAbsolutePath())));
            doc.add(new Paragraph("Liste des transactions").setBold().setFontSize(16));
            doc.add(new Paragraph(" "));
            Table tbl = new Table(new float[]{100, 80, 80, 80, 80, 180});
            for (String h : new String[]{"Catégorie","Date","Montant","Type","Statut","Description"})
                tbl.addHeaderCell(new Cell().add(new Paragraph(h)));
            for (Transaction t : filteredData) {
                tbl.addCell(nvl(t.getCategorie(), ""));
                tbl.addCell(t.getDateTransaction() != null ? t.getDateTransaction().toString() : "");
                tbl.addCell(String.format("%.2f DT", t.getMontant() != null ? t.getMontant() : 0.0));
                tbl.addCell(nvl(t.getTypeTransaction(), ""));
                tbl.addCell(nvl(t.getStatutTransaction(), ""));
                tbl.addCell(nvl(t.getDescription(), ""));
            }
            doc.add(tbl); doc.close();
            showAlert(Alert.AlertType.INFORMATION, "Export", "✅ PDF exporté !");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de créer le PDF !");
        }
    }

    @FXML void envoyerSMS(ActionEvent e) {
        showAlert(Alert.AlertType.INFORMATION, "SMS", "Fonction SMS non implémentée !");
    }

    // ══════════════════════════════════════════════════════════════════════
    // Validation
    // ══════════════════════════════════════════════════════════════════════

    private void setupRealTimeValidation() {
        if (cmbCategorie != null) cmbCategorie.valueProperty().addListener((obs, ov, v) -> {
            if (v != null && !v.isBlank()) setSuccess(lblCategorieError, "✓ OK");
            else setError(lblCategorieError, "❌ Catégorie obligatoire.");
        });
        if (dpDateTransaction != null) dpDateTransaction.valueProperty().addListener((obs, ov, v) -> {
            if (v == null)                   setError(lblDateError, "❌ Date obligatoire.");
            else if (v.isAfter(LocalDate.now()))  setError(lblDateError, "❌ Date dans le futur.");
            else setSuccess(lblDateError, "✓ OK");
        });
        if (txtMontant != null) txtMontant.textProperty().addListener((obs, ov, v) -> {
            if (v.isBlank()) { setError(lblMontantError, "❌ Montant obligatoire."); return; }
            try {
                double m = Double.parseDouble(v.trim());
                if (m <= 0)       setError(lblMontantError, "❌ Montant doit être positif.");
                else if (m > 1e6) setError(lblMontantError, "❌ Max 1 000 000 DT.");
                else              setSuccess(lblMontantError, "✓ OK");
            } catch (NumberFormatException e) { setError(lblMontantError, "❌ Format invalide."); }
        });
        if (cmbTypeTransaction != null) cmbTypeTransaction.valueProperty().addListener((obs, ov, v) -> {
            if (v != null && !v.isBlank()) setSuccess(lblTypeError, "✓ OK");
            else setError(lblTypeError, "❌ Type obligatoire.");
        });
        if (cmbStatutTransaction != null) cmbStatutTransaction.valueProperty().addListener((obs, ov, v) -> {
            if (v != null && !v.isBlank()) setSuccess(lblStatutError, "✓ OK");
            else setError(lblStatutError, "❌ Statut obligatoire.");
        });
        if (txtDescription != null) txtDescription.textProperty().addListener((obs, ov, v) -> {
            String tx = v.trim();
            if (tx.isEmpty()) { if (lblDescriptionError != null) lblDescriptionError.setText(""); return; }
            if (tx.length() < 5)   setError(lblDescriptionError, "❌ Min 5 caractères.");
            else if (tx.length() > 500) setError(lblDescriptionError, "❌ Max 500 caractères.");
            else setSuccess(lblDescriptionError, "✓ " + tx.length() + "/500");
        });
    }

    private boolean validateForm() {
        boolean valid = true; clearErrorLabels();
        if (cmbCategorie == null || cmbCategorie.getValue() == null)
            { setError(lblCategorieError, "❌ Catégorie obligatoire."); valid = false; }
        LocalDate d = dpDateTransaction != null ? dpDateTransaction.getValue() : null;
        if (d == null) { setError(lblDateError, "❌ Date obligatoire."); valid = false; }
        else if (d.isAfter(LocalDate.now())) { setError(lblDateError, "❌ Date dans le futur."); valid = false; }
        String mt = txtMontant != null ? txtMontant.getText().trim() : "";
        if (mt.isEmpty()) { setError(lblMontantError, "❌ Montant obligatoire."); valid = false; }
        else { try { double m = Double.parseDouble(mt); if (m <= 0) { setError(lblMontantError, "❌ Positif requis."); valid = false; } }
               catch (NumberFormatException e) { setError(lblMontantError, "❌ Format invalide."); valid = false; } }
        if (cmbTypeTransaction == null || cmbTypeTransaction.getValue() == null)
            { setError(lblTypeError, "❌ Type obligatoire."); valid = false; }
        if (cmbStatutTransaction == null || cmbStatutTransaction.getValue() == null)
            { setError(lblStatutError, "❌ Statut obligatoire."); valid = false; }
        return valid;
    }

    private void setError(Label l, String msg)   { if (l != null) { l.setText(msg); l.setStyle("-fx-text-fill:#DC2626;-fx-font-size:10px;-fx-font-weight:bold;"); } }
    private void setSuccess(Label l, String msg) { if (l != null) { l.setText(msg); l.setStyle("-fx-text-fill:#059669;-fx-font-size:10px;-fx-font-weight:bold;"); } }

    private void clearErrorLabels() {
        for (Label l : new Label[]{lblCategorieError, lblDateError, lblMontantError,
                                    lblTypeError, lblStatutError, lblDescriptionError})
            if (l != null) l.setText("");
    }

    // ══════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.show();
    }

    private String nvl(String s, String def)   { return (s != null && !s.isBlank()) ? s : def; }
    private String truncate(String s, int max) { if (s == null) return ""; return s.length() <= max ? s : s.substring(0, max) + "…"; }
}