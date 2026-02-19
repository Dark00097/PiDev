package com.nexora.bank.controllers;

import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.Reclamation;
import com.nexora.bank.Models.Transaction;
import com.nexora.bank.Service.ReclamationService;
import com.nexora.bank.Service.TransactionService;
import com.nexora.bank.Utils.EmailUtil;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class UserDashboardTransactionsSectionController implements Initializable {

    // ─── Services ─────────────────────────────────────────────────────────────
    private final TransactionService transactionService = new TransactionService();
    private final ReclamationService reclamationService = new ReclamationService();

    // ─── Panels ───────────────────────────────────────────────────────────────
    @FXML private VBox reclamationFormContainer;
    @FXML private VBox reclamationsDisplayContainer;

    // ─── Formulaire transaction ───────────────────────────────────────────────
    @FXML private ComboBox<String> cbCategorie;
    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TextField        txtMontant;
    @FXML private TextField        txtRecipient;
    @FXML private DatePicker       dpDate;
    @FXML private TextArea         txtDescription;

    // ─── Labels erreur transaction ────────────────────────────────────────────
    @FXML private Label lblCategorieError;
    @FXML private Label lblDateError;
    @FXML private Label lblMontantError;
    @FXML private Label lblTypeError;
    @FXML private Label lblStatutError;
    @FXML private Label lblDescriptionError;

    // ─── Formulaire réclamation ───────────────────────────────────────────────
    @FXML private ComboBox<String> cbTypeReclamation;
    @FXML private ComboBox<String> cbPrioriteReclamation;
    @FXML private TextArea         txtDescriptionReclamation;
    @FXML private Label            lblTypeReclamationError;
    @FXML private Label            lblDescriptionReclamationError;
    @FXML private Label            lblReclamationTransactionInfo;

    // ─── Zone d'affichage des cartes ─────────────────────────────────────────
    @FXML private VBox       transactionsCardsContainer;
    @FXML private ScrollPane transactionsScrollPane;

    // ─── Stats / recherche / tri ──────────────────────────────────────────────
    @FXML private Label            lblTotalTransactions;
    @FXML private Label            lblTableInfo;
    @FXML private TextField        txtRecherche;
    @FXML private Button           btnAjouter;
    @FXML private ComboBox<String> cbSortCritere;

    // ══════════════════════════════════════════════════════════════════════════
    // CONSTANTES DE TRI — uniquement Montant et Statut
    // ══════════════════════════════════════════════════════════════════════════
    private static final String SORT_MONTANT_DESC = "Montant décroissant";
    private static final String SORT_MONTANT_ASC  = "Montant croissant";
    private static final String SORT_STATUT       = "Statut A-Z";

    // ══════════════════════════════════════════════════════════════════════════
    // PALETTE — Thème "Cloud Steel" (cartes claires)
    // ══════════════════════════════════════════════════════════════════════════
    private static final String BG_CARD    = "#f0f4ff";
    private static final String BG_HOVER   = "#dce6f7";

    private static final String CLR_CREDIT  = "#0d9488";
    private static final String CLR_DEBIT   = "#4f46e5";
    private static final String CLR_PENDING = "#ea580c";
    private static final String CLR_FAILED  = "#dc2626";

    private static final String BTN_EDIT_CLR   = "#0d9488";
    private static final String BTN_EDIT_BG    = "#ccfbf1";
    private static final String BTN_DELETE_CLR = "#dc2626";
    private static final String BTN_DELETE_BG  = "#fee2e2";
    private static final String BTN_CLAIM_CLR  = "#ea580c";
    private static final String BTN_CLAIM_BG   = "#ffedd5";

    private static final String TXT_PRIMARY   = "#1e293b";
    private static final String TXT_SECONDARY = "#64748b";
    private static final String TXT_MUTED     = "#94a3b8";
    private static final String BORDER_CARD   = "#cbd5e1";

    // ─── État interne ─────────────────────────────────────────────────────────
    private final ObservableList<Transaction> transactionsList = FXCollections.observableArrayList();
    private FilteredList<Transaction>         filteredData;
    private Transaction selectedTransaction    = null;
    private Transaction reclamationTransaction = null;
    private boolean     isEditMode             = false;

    private String  sortField     = "montant";
    private boolean sortAscending = false;

    // ─── Pagination ───────────────────────────────────────────────────────────
    private static final int PAGE_SIZE = 3;
    private int currentPage = 0; // index base 0

    private static final Duration          ANIMATION_DURATION = Duration.millis(200);
    private static final DateTimeFormatter DATE_FMT           = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ══════════════════════════════════════════════════════════════════════════
    // Initialisation
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setVisibleManaged(reclamationFormContainer, false);
        setVisibleManaged(reclamationsDisplayContainer, false);
        setupSortComboBox();   // ← initialise le ComboBox de tri avec listener
        setupSearch();
        setupRealTimeValidation();
        refreshData();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tri — initialisation du ComboBox avec listener automatique
    // ══════════════════════════════════════════════════════════════════════════

    private void setupSortComboBox() {
        if (cbSortCritere == null) return;

        // Uniquement tri par Montant et par Statut
        cbSortCritere.setItems(FXCollections.observableArrayList(
            SORT_MONTANT_DESC,
            SORT_MONTANT_ASC,
            SORT_STATUT
        ));

        // Sélection par défaut
        cbSortCritere.setValue(SORT_MONTANT_DESC);

        // Listener : le tri s'applique automatiquement dès qu'une valeur est choisie
        cbSortCritere.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) appliquerTriDepuis(newVal);
        });
    }

    /** Applique le tri selon la valeur choisie dans le ComboBox */
    private void appliquerTriDepuis(String critere) {
        if (critere.equals(SORT_MONTANT_DESC))     { sortField = "montant"; sortAscending = false; }
        else if (critere.equals(SORT_MONTANT_ASC)) { sortField = "montant"; sortAscending = true;  }
        else if (critere.equals(SORT_STATUT))      { sortField = "statut";  sortAscending = true;  }
        currentPage = 0;
        if (filteredData != null) renderCards();
    }

    /** Handler du bouton "Trier" (appel FXML conservé) */
    @FXML
    private void appliquerTri() {
        if (cbSortCritere != null && cbSortCritere.getValue() != null) {
            appliquerTriDepuis(cbSortCritere.getValue());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Comparateur selon sortField / sortAscending
    // ══════════════════════════════════════════════════════════════════════════

    private Comparator<Transaction> buildComparator() {
        Comparator<Transaction> cmp;
        switch (sortField) {
            case "statut":
                cmp = Comparator.comparing(t -> nvl(t.getStatutTransaction(), ""));
                break;
            default: // "montant"
                cmp = Comparator.comparingDouble(t -> t.getMontant() != null ? t.getMontant() : 0.0);
                break;
        }
        return sortAscending ? cmp : cmp.reversed();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Données & rendu des cartes
    // ══════════════════════════════════════════════════════════════════════════

    private void refreshData() {
        transactionsList.setAll(transactionService.getAll());
        filteredData = new FilteredList<>(transactionsList, t -> true);
        applySearchFilter(txtRecherche != null ? txtRecherche.getText() : "");
        renderCards();
        updateStats();
    }

    private void renderCards() {
        if (transactionsCardsContainer == null) return;
        transactionsCardsContainer.getChildren().clear();
        transactionsCardsContainer.setSpacing(6);

        List<Transaction> sorted = filteredData.stream()
                .sorted(buildComparator())
                .collect(Collectors.toList());

        if (sorted.isEmpty()) {
            Label empty = new Label("Aucune transaction trouvée.");
            empty.setStyle("-fx-text-fill:" + TXT_SECONDARY + ";-fx-font-size:12px;-fx-padding:20 0;");
            transactionsCardsContainer.getChildren().add(empty);
            updateTableInfo(0);
            return;
        }

        // ── Calcul pagination ──────────────────────────────────────────────
        int totalPages = (int) Math.ceil((double) sorted.size() / PAGE_SIZE);
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0)          currentPage = 0;

        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex   = Math.min(fromIndex + PAGE_SIZE, sorted.size());
        List<Transaction> pageItems = sorted.subList(fromIndex, toIndex);

        // ── Rendu des cartes de la page courante ───────────────────────────
        for (Transaction t : pageItems)
            transactionsCardsContainer.getChildren().add(buildCard(t));

        // ── Barre de pagination ────────────────────────────────────────────
        transactionsCardsContainer.getChildren().add(buildPaginationBar(totalPages, sorted.size()));

        updateTableInfo(sorted.size());
    }

    /** Construit la barre de navigation pages */
    private HBox buildPaginationBar(int totalPages, int totalItems) {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(10, 0, 4, 0));

        // Info "Page X / Y  —  N transactions"
        int from = currentPage * PAGE_SIZE + 1;
        int to   = Math.min(from + PAGE_SIZE - 1, totalItems);
        Label info = new Label(from + "–" + to + " sur " + totalItems);
        info.setStyle("-fx-font-size:11px;-fx-text-fill:" + TXT_SECONDARY + ";-fx-padding:0 8 0 0;");

        // Bouton ◀ Précédent
        Button btnPrev = buildPageBtn("◀", currentPage > 0);
        btnPrev.setOnAction(e -> { currentPage--; renderCards(); });

        // Boutons numérotés
        HBox pageButtons = new HBox(4);
        pageButtons.setAlignment(Pos.CENTER);
        for (int i = 0; i < totalPages; i++) {
            final int pageIndex = i;
            boolean active = (i == currentPage);
            Label lbl = new Label(String.valueOf(i + 1));
            lbl.setMinSize(28, 28); lbl.setMaxSize(28, 28);
            lbl.setAlignment(Pos.CENTER);
            lbl.setStyle(active
                ? "-fx-background-color:#4f46e5;-fx-text-fill:#ffffff;" +
                  "-fx-background-radius:7;-fx-font-size:11px;-fx-font-weight:700;-fx-cursor:hand;"
                : "-fx-background-color:#e2e8f0;-fx-text-fill:" + TXT_PRIMARY + ";" +
                  "-fx-background-radius:7;-fx-font-size:11px;-fx-cursor:hand;"
            );
            if (!active) {
                lbl.setOnMouseEntered(ev -> lbl.setStyle(
                    "-fx-background-color:#c7d2fe;-fx-text-fill:#4f46e5;" +
                    "-fx-background-radius:7;-fx-font-size:11px;-fx-cursor:hand;"
                ));
                lbl.setOnMouseExited(ev -> lbl.setStyle(
                    "-fx-background-color:#e2e8f0;-fx-text-fill:" + TXT_PRIMARY + ";" +
                    "-fx-background-radius:7;-fx-font-size:11px;-fx-cursor:hand;"
                ));
                lbl.setOnMouseClicked(ev -> { currentPage = pageIndex; renderCards(); });
            }
            pageButtons.getChildren().add(lbl);
        }

        // Bouton ▶ Suivant
        Button btnNext = buildPageBtn("▶", currentPage < totalPages - 1);
        btnNext.setOnAction(e -> { currentPage++; renderCards(); });

        bar.getChildren().addAll(info, btnPrev, pageButtons, btnNext);
        return bar;
    }

    private Button buildPageBtn(String label, boolean enabled) {
        Button btn = new Button(label);
        btn.setMinSize(28, 28); btn.setMaxSize(28, 28);
        btn.setDisable(!enabled);
        String base = enabled
            ? "-fx-background-color:#e2e8f0;-fx-text-fill:" + TXT_PRIMARY + ";" +
              "-fx-background-radius:7;-fx-font-size:10px;-fx-cursor:hand;-fx-border-width:0;"
            : "-fx-background-color:#f1f5f9;-fx-text-fill:" + TXT_MUTED + ";" +
              "-fx-background-radius:7;-fx-font-size:10px;-fx-border-width:0;";
        btn.setStyle(base);
        if (enabled) {
            btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:#c7d2fe;-fx-text-fill:#4f46e5;" +
                "-fx-background-radius:7;-fx-font-size:10px;-fx-cursor:hand;-fx-border-width:0;"
            ));
            btn.setOnMouseExited(e -> btn.setStyle(base));
        }
        return btn;
    }

    private HBox buildCard(Transaction t) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8, 12, 8, 12));
        card.setMaxWidth(Double.MAX_VALUE);

        boolean isCredit  = "Credit".equalsIgnoreCase(t.getTypeTransaction());
        boolean isPending = "En attente".equalsIgnoreCase(t.getStatutTransaction());
        boolean isFailed  = "Echouee".equalsIgnoreCase(t.getStatutTransaction());

        String accent = isFailed ? CLR_FAILED
                      : isPending ? CLR_PENDING
                      : isCredit  ? CLR_CREDIT
                      : CLR_DEBIT;

        card.setStyle(cardStyle(accent, false));
        card.setOnMouseEntered(e -> card.setStyle(cardStyle(accent, true)));
        card.setOnMouseExited(e  -> card.setStyle(cardStyle(accent, false)));

        Region bar = new Region();
        bar.setMinWidth(3); bar.setMaxWidth(3); bar.setMinHeight(32);
        bar.setStyle("-fx-background-color:" + accent + ";-fx-background-radius:2;");

        Label icon = new Label(isFailed ? "✕" : isPending ? "◷" : isCredit ? "↓" : "↑");
        icon.setMinSize(28, 28); icon.setMaxSize(28, 28);
        icon.setAlignment(Pos.CENTER);
        icon.setStyle(
            "-fx-background-color:" + accent + "22;" +
            "-fx-background-radius:7;" +
            "-fx-text-fill:" + accent + ";" +
            "-fx-font-size:13px;" +
            "-fx-font-weight:bold;"
        );

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox row1 = new HBox(7);
        row1.setAlignment(Pos.CENTER_LEFT);
        Label lblCat = new Label(nvl(t.getCategorie(), "—"));
        lblCat.setStyle("-fx-font-weight:700;-fx-font-size:12px;-fx-text-fill:" + TXT_PRIMARY + ";");
        row1.getChildren().addAll(lblCat, buildBadge(t.getStatutTransaction(), accent));

        HBox row2 = new HBox(5);
        row2.setAlignment(Pos.CENTER_LEFT);
        row2.getChildren().addAll(
            buildMeta(nvl(t.getTypeTransaction(), "—")),
            buildSep(),
            buildMeta(t.getDateTransaction() != null ? t.getDateTransaction().format(DATE_FMT) : "—")
        );
        if (t.getDescription() != null && !t.getDescription().isBlank())
            row2.getChildren().addAll(buildSep(), buildMetaItalic(truncate(t.getDescription(), 30)));

        info.getChildren().addAll(row1, row2);

        Label montant = new Label((isCredit ? "+" : "−") + String.format("%.2f DT", t.getMontant()));
        montant.setMinWidth(88);
        montant.setAlignment(Pos.CENTER_RIGHT);
        montant.setStyle(
            "-fx-font-size:13px;-fx-font-weight:700;" +
            "-fx-text-fill:" + (isCredit ? CLR_CREDIT : CLR_DEBIT) + ";"
        );

        HBox actions = new HBox(4);
        actions.setAlignment(Pos.CENTER);
        actions.getChildren().addAll(
            buildIconBtn("✏",  BTN_EDIT_CLR,   BTN_EDIT_BG,   "Modifier",    e -> editTransaction(t)),
            buildIconBtn("🗑", BTN_DELETE_CLR, BTN_DELETE_BG, "Supprimer",   e -> confirmAndDelete(t)),
            buildIconBtn("⚑",  BTN_CLAIM_CLR,  BTN_CLAIM_BG,  "Réclamation", e -> openReclamationForm(t))
        );

        card.getChildren().addAll(bar, icon, info, montant, actions);
        return card;
    }

    private String cardStyle(String accent, boolean hover) {
        String bg     = hover ? BG_HOVER : BG_CARD;
        String border = hover ? accent + "88" : BORDER_CARD;
        String shadow = hover
            ? "dropshadow(gaussian,rgba(0,0,0,0.13),10,0,0,3)"
            : "dropshadow(gaussian,rgba(0,0,0,0.06),4,0,0,1)";
        return  "-fx-background-color:" + bg + ";" +
                "-fx-background-radius:10;" +
                "-fx-border-color:" + border + ";" +
                "-fx-border-radius:10;" +
                "-fx-border-width:1;" +
                "-fx-effect:" + shadow + ";" +
                (hover ? "-fx-cursor:hand;" : "");
    }

    private Button buildIconBtn(String icon, String color, String lightBg, String tooltip,
                                 javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(icon);
        Tooltip tip = new Tooltip(tooltip);
        tip.setStyle("-fx-background-color:#1e293b;-fx-text-fill:#f1f5f9;-fx-font-size:10px;-fx-background-radius:5;");
        Tooltip.install(btn, tip);
        btn.setOnAction(handler);

        String base =
            "-fx-background-color:" + lightBg + ";" +
            "-fx-text-fill:" + color + ";" +
            "-fx-font-size:11px;-fx-padding:3 7 3 7;" +
            "-fx-background-radius:6;-fx-border-color:" + color + "55;" +
            "-fx-border-radius:6;-fx-border-width:1;-fx-cursor:hand;" +
            "-fx-min-width:26;-fx-pref-width:26;-fx-max-width:26;" +
            "-fx-min-height:24;-fx-pref-height:24;-fx-max-height:24;";
        String hoverS =
            "-fx-background-color:" + color + ";-fx-text-fill:#ffffff;" +
            "-fx-font-size:11px;-fx-padding:3 7 3 7;" +
            "-fx-background-radius:6;-fx-border-color:transparent;" +
            "-fx-border-radius:6;-fx-border-width:1;-fx-cursor:hand;" +
            "-fx-min-width:26;-fx-pref-width:26;-fx-max-width:26;" +
            "-fx-min-height:24;-fx-pref-height:24;-fx-max-height:24;" +
            "-fx-effect:dropshadow(gaussian," + color + "66,5,0,0,1);";

        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverS));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private Label buildBadge(String statut, String color) {
        Label b = new Label(nvl(statut, "—").toUpperCase());
        b.setStyle(
            "-fx-background-color:" + color + "18;-fx-text-fill:" + color + ";" +
            "-fx-font-size:8px;-fx-font-weight:700;-fx-padding:1 6 1 6;" +
            "-fx-background-radius:20;-fx-border-color:" + color + "44;" +
            "-fx-border-radius:20;-fx-border-width:1;"
        );
        return b;
    }

    private Label buildMeta(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:10px;-fx-text-fill:" + TXT_SECONDARY + ";");
        return l;
    }

    private Label buildMetaItalic(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:10px;-fx-text-fill:" + TXT_MUTED + ";-fx-font-style:italic;");
        return l;
    }

    private Label buildSep() {
        Label s = new Label("·");
        s.setStyle("-fx-font-size:10px;-fx-text-fill:" + TXT_MUTED + ";");
        return s;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Recherche
    // ══════════════════════════════════════════════════════════════════════════

    private void setupSearch() {
        if (txtRecherche != null)
            txtRecherche.textProperty().addListener((obs, o, v) -> {
                currentPage = 0;
                applySearchFilter(v);
                renderCards();
            });
    }

    private void applySearchFilter(String v) {
        if (filteredData == null) return;
        filteredData.setPredicate(t -> {
            if (v == null || v.isBlank()) return true;
            String f = v.toLowerCase();
            return (t.getCategorie()         != null && t.getCategorie().toLowerCase().contains(f))
                || (t.getStatutTransaction() != null && t.getStatutTransaction().toLowerCase().contains(f))
                || (t.getTypeTransaction()   != null && t.getTypeTransaction().toLowerCase().contains(f))
                || (t.getDescription()       != null && t.getDescription().toLowerCase().contains(f));
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Stats
    // ══════════════════════════════════════════════════════════════════════════

    private void updateStats() {
        if (lblTotalTransactions != null)
            lblTotalTransactions.setText(String.valueOf(transactionsList.size()));
    }

    private void updateTableInfo(int count) {
        if (lblTableInfo != null)
            lblTableInfo.setText("Total : " + count + " transaction" + (count > 1 ? "s" : ""));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Réclamation
    // ══════════════════════════════════════════════════════════════════════════

    private void openReclamationForm(Transaction t) {
        reclamationTransaction = t;
        if (lblReclamationTransactionInfo != null)
            lblReclamationTransactionInfo.setText(
                "Transaction #" + t.getIdTransaction()
                + "  •  " + nvl(t.getCategorie(), "—")
                + "  •  " + String.format("%.2f DT", t.getMontant())
                + (t.getDateTransaction() != null ? "  •  " + t.getDateTransaction().format(DATE_FMT) : "")
            );
        clearReclamationForm();
        if (reclamationsDisplayContainer != null && reclamationsDisplayContainer.isVisible())
            setVisibleManaged(reclamationsDisplayContainer, false);
        setVisibleManaged(reclamationFormContainer, true);
        animateSlideIn(reclamationFormContainer);
    }

    @FXML
    private void submitComplaint() {
        if (!validateReclamationForm()) return;
        if (reclamationTransaction == null) { showNotification("Erreur", "Aucune transaction sélectionnée."); return; }
        int    idUser    = AuthSession.getCurrentUser().getIdUser();
        String date      = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String type      = cbTypeReclamation.getValue();
        String priorite  = cbPrioriteReclamation != null && cbPrioriteReclamation.getValue() != null
                           ? cbPrioriteReclamation.getValue() : "Medium";
        String descFinal = "[Priorité : " + priorite + "] " + txtDescriptionReclamation.getText().trim();

        reclamationService.add(new Reclamation(
                idUser, reclamationTransaction.getIdTransaction(), date, type, descFinal, "En attente"));

        showNotification("Réclamation soumise",
                "Enregistrée !\nTransaction #" + reclamationTransaction.getIdTransaction() + "  •  Statut : En attente");

        animateSlideOut(reclamationFormContainer, () -> {
            setVisibleManaged(reclamationFormContainer, false);
            clearReclamationForm(); reclamationTransaction = null;
            setVisibleManaged(reclamationsDisplayContainer, true);
            animateSlideIn(reclamationsDisplayContainer);
        });
    }

    private boolean validateReclamationForm() {
        boolean valid = true;
        if (lblTypeReclamationError        != null) lblTypeReclamationError.setText("");
        if (lblDescriptionReclamationError != null) lblDescriptionReclamationError.setText("");
        if (cbTypeReclamation == null || cbTypeReclamation.getValue() == null)
            { setError(lblTypeReclamationError, "❌ Le type est obligatoire."); valid = false; }
        String d = txtDescriptionReclamation != null ? txtDescriptionReclamation.getText().trim() : "";
        if (d.isEmpty())           { setError(lblDescriptionReclamationError, "❌ Description obligatoire."); valid = false; }
        else if (d.length() < 10)  { setError(lblDescriptionReclamationError, "❌ Au moins 10 caractères."); valid = false; }
        else if (d.length() > 500) { setError(lblDescriptionReclamationError, "❌ Max 500 caractères."); valid = false; }
        return valid;
    }

    private void clearReclamationForm() {
        if (cbTypeReclamation              != null) cbTypeReclamation.setValue(null);
        if (cbPrioriteReclamation          != null) cbPrioriteReclamation.setValue(null);
        if (txtDescriptionReclamation      != null) txtDescriptionReclamation.clear();
        if (lblTypeReclamationError        != null) lblTypeReclamationError.setText("");
        if (lblDescriptionReclamationError != null) lblDescriptionReclamationError.setText("");
    }

    @FXML private void showReclamationForm() {
        if (selectedTransaction == null) { showNotification("Info", "Sélectionnez une transaction."); return; }
        openReclamationForm(selectedTransaction);
    }
    @FXML private void hideReclamationForm() {
        animateSlideOut(reclamationFormContainer, () -> {
            setVisibleManaged(reclamationFormContainer, false);
            clearReclamationForm(); reclamationTransaction = null;
        });
    }
    @FXML private void showReclamations() {
        setVisibleManaged(reclamationFormContainer, false);
        setVisibleManaged(reclamationsDisplayContainer, true);
        animateSlideIn(reclamationsDisplayContainer);
    }
    @FXML private void hideReclamations() {
        animateSlideOut(reclamationsDisplayContainer, () -> setVisibleManaged(reclamationsDisplayContainer, false));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Transaction CRUD
    // ══════════════════════════════════════════════════════════════════════════

    @FXML private void newTransaction() { clearForm(); }

    @FXML
    private void saveTransaction() {
        if (!validateTransactionForm()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Corrigez les erreurs du formulaire.");
            return;
        }
        int       idUser    = AuthSession.getCurrentUser().getIdUser();
        String    emailUser = AuthSession.getCurrentUser().getEmail();
        String    cat       = cbCategorie.getValue();
        LocalDate date      = dpDate.getValue();
        double    montant   = Double.parseDouble(txtMontant.getText().trim());
        String    type      = cbType.getValue();
        String    statut    = cbStatut.getValue();
        String    desc      = txtDescription.getText().trim();

        if (isEditMode && selectedTransaction != null) {
            transactionService.edit(new Transaction(
                    selectedTransaction.getIdTransaction(), selectedTransaction.getIdUser(),
                    cat, date, montant, type, statut, desc));
            new Thread(() -> EmailUtil.envoyerConfirmationTransaction(emailUser, cat, montant, type, statut)).start();
            showNotification("Succès", "Transaction modifiée ! Email de confirmation envoyé.");
        } else {
            transactionService.add(new Transaction(idUser, cat, date, montant, type, statut, desc));
            new Thread(() -> EmailUtil.envoyerConfirmationTransaction(emailUser, cat, montant, type, statut)).start();
            showNotification("Succès", "Transaction ajoutée ! Email de confirmation envoyé.");
        }
        clearForm();
        refreshData();
    }

    @FXML
    private void deleteTransaction() {
        if (selectedTransaction == null) { showNotification("Info", "Sélectionnez une transaction."); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette transaction ?", ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Confirmation");
        a.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) confirmAndDelete(selectedTransaction); });
    }

    @FXML private void handleAnnuler() { clearForm(); }

    private void editTransaction(Transaction t) {
        selectedTransaction = t; populateForm(t);
        isEditMode = true;
        if (btnAjouter != null) btnAjouter.setText("Modifier");
    }

    private void confirmAndDelete(Transaction t) {
        transactionService.remove(t);
        clearForm(); refreshData();
        showNotification("Supprimé", "Transaction supprimée avec succès !");
    }

    private void populateForm(Transaction t) {
        if (cbCategorie    != null) cbCategorie.setValue(t.getCategorie());
        if (cbType         != null) cbType.setValue(t.getTypeTransaction());
        if (cbStatut       != null) cbStatut.setValue(t.getStatutTransaction());
        if (txtMontant     != null) txtMontant.setText(String.valueOf(t.getMontant()));
        if (txtDescription != null) txtDescription.setText(t.getDescription());
        if (dpDate         != null) dpDate.setValue(t.getDateTransaction());
    }

    @FXML
    private void clearForm() {
        if (cbCategorie    != null) cbCategorie.getSelectionModel().clearSelection();
        if (cbType         != null) cbType.getSelectionModel().clearSelection();
        if (cbStatut       != null) cbStatut.getSelectionModel().clearSelection();
        if (txtMontant     != null) txtMontant.clear();
        if (txtRecipient   != null) txtRecipient.clear();
        if (dpDate         != null) dpDate.setValue(null);
        if (txtDescription != null) txtDescription.clear();
        selectedTransaction = null; isEditMode = false;
        if (btnAjouter != null) btnAjouter.setText("Ajouter");
        clearErrorLabels();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Export PDF
    // ══════════════════════════════════════════════════════════════════════════

    @FXML private void exportTransactions() { exporterPDF(); }
    @FXML private void retryTransaction()   { showNotification("Nouvel essai", "Traitement en cours..."); }
    @FXML private void envoyerSMS()         { showNotification("SMS", "Fonction SMS non implémentée !"); }

    @FXML
    private void exporterPDF() {
        if (transactionsList.isEmpty()) { showNotification("Export", "Aucune transaction à exporter !"); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(new Stage());
        if (file == null) return;
        try {
            Document doc = new Document(new PdfDocument(new PdfWriter(file.getAbsolutePath())));
            doc.add(new Paragraph("Liste des transactions").setBold().setFontSize(16));
            doc.add(new Paragraph(" "));
            Table tbl = new Table(new float[]{100, 80, 80, 80, 80, 180});
            for (String h : new String[]{"Catégorie", "Date", "Montant", "Type", "Statut", "Description"})
                tbl.addHeaderCell(new Cell().add(new Paragraph(h)));
            for (Transaction t : filteredData) {
                tbl.addCell(nvl(t.getCategorie(), ""));
                tbl.addCell(t.getDateTransaction() != null ? t.getDateTransaction().toString() : "");
                tbl.addCell(String.valueOf(t.getMontant()));
                tbl.addCell(nvl(t.getTypeTransaction(), ""));
                tbl.addCell(nvl(t.getStatutTransaction(), ""));
                tbl.addCell(nvl(t.getDescription(), ""));
            }
            doc.add(tbl); doc.close();
            showNotification("Export", "PDF exporté avec succès !");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            showNotification("Erreur", "Impossible de créer le PDF !");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Validation
    // ══════════════════════════════════════════════════════════════════════════

    private boolean validateTransactionForm() {
        boolean v = true; clearErrorLabels();
        if (cbCategorie == null || cbCategorie.getValue() == null)
            { setError(lblCategorieError, "❌ Catégorie obligatoire."); v = false; }
        LocalDate d = dpDate != null ? dpDate.getValue() : null;
        if (d == null)                      { setError(lblDateError, "❌ Date obligatoire."); v = false; }
        else if (d.isAfter(LocalDate.now())) { setError(lblDateError, "❌ Date future non permise."); v = false; }
        String mt = txtMontant != null ? txtMontant.getText().trim() : "";
        if (mt.isEmpty()) { setError(lblMontantError, "❌ Montant obligatoire."); v = false; }
        else { try { double m = Double.parseDouble(mt);
            if (m <= 0) { setError(lblMontantError, "❌ Doit être positif."); v = false; }
        } catch (NumberFormatException e) { setError(lblMontantError, "❌ Format invalide."); v = false; } }
        if (cbType   == null || cbType.getValue()   == null) { setError(lblTypeError,   "❌ Type obligatoire.");   v = false; }
        if (cbStatut == null || cbStatut.getValue() == null) { setError(lblStatutError, "❌ Statut obligatoire."); v = false; }
        return v;
    }

    private void setupRealTimeValidation() {
        if (cbCategorie != null) cbCategorie.valueProperty().addListener((o, ov, v) ->
            { if (v != null && !v.isBlank()) setSuccess(lblCategorieError, "✓ OK"); else setError(lblCategorieError, "❌ Obligatoire."); });
        if (dpDate != null) dpDate.valueProperty().addListener((o, ov, v) ->
            { if (v == null) setError(lblDateError, "❌ Obligatoire."); else if (v.isAfter(LocalDate.now())) setError(lblDateError, "❌ Future."); else setSuccess(lblDateError, "✓ OK"); });
        if (txtMontant != null) txtMontant.textProperty().addListener((o, ov, v) -> {
            if (v.isBlank()) { setError(lblMontantError, "❌ Obligatoire."); return; }
            try { double m = Double.parseDouble(v);
                if (m <= 0) setError(lblMontantError, "❌ Positif requis."); else setSuccess(lblMontantError, "✓ OK");
            } catch (NumberFormatException e) { setError(lblMontantError, "❌ Format invalide."); }
        });
        if (cbType != null) cbType.valueProperty().addListener((o, ov, v) ->
            { if (v != null && !v.isBlank()) setSuccess(lblTypeError, "✓ OK"); else setError(lblTypeError, "❌ Obligatoire."); });
        if (cbStatut != null) cbStatut.valueProperty().addListener((o, ov, v) ->
            { if (v != null && !v.isBlank()) setSuccess(lblStatutError, "✓ OK"); else setError(lblStatutError, "❌ Obligatoire."); });
        if (cbTypeReclamation != null) cbTypeReclamation.valueProperty().addListener((o, ov, v) ->
            { if (v != null && !v.isBlank()) setSuccess(lblTypeReclamationError, "✓ OK"); else setError(lblTypeReclamationError, "❌ Obligatoire."); });
        if (txtDescriptionReclamation != null) txtDescriptionReclamation.textProperty().addListener((o, ov, v) -> {
            String tx = v.trim();
            if (tx.isEmpty()) { if (lblDescriptionReclamationError != null) lblDescriptionReclamationError.setText(""); return; }
            if (tx.length() < 10) setError(lblDescriptionReclamationError, "❌ Min 10 caractères.");
            else setSuccess(lblDescriptionReclamationError, "✓ " + tx.length() + "/500");
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private void setError(Label l, String msg) {
        if (l != null) { l.setText(msg); l.setStyle("-fx-text-fill:#dc2626;-fx-font-size:10px;-fx-font-weight:bold;"); }
    }
    private void setSuccess(Label l, String msg) {
        if (l != null) { l.setText(msg); l.setStyle("-fx-text-fill:#0d9488;-fx-font-size:10px;-fx-font-weight:bold;"); }
    }
    private void clearErrorLabels() {
        for (Label l : new Label[]{lblCategorieError, lblDateError, lblMontantError,
                                    lblTypeError, lblStatutError, lblDescriptionError})
            if (l != null) l.setText("");
    }
    private void setVisibleManaged(VBox node, boolean v) {
        if (node != null) { node.setVisible(v); node.setManaged(v); }
    }
    private void animateSlideIn(VBox node) {
        if (node == null) return;
        node.setOpacity(0); node.setTranslateY(-12);
        FadeTransition f = new FadeTransition(ANIMATION_DURATION, node);
        f.setFromValue(0); f.setToValue(1); f.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition s = new TranslateTransition(ANIMATION_DURATION, node);
        s.setFromY(-12); s.setToY(0); s.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(f, s).play();
    }
    private void animateSlideOut(VBox node, Runnable onFinished) {
        if (node == null) { if (onFinished != null) onFinished.run(); return; }
        FadeTransition f = new FadeTransition(Duration.millis(120), node);
        f.setFromValue(1); f.setToValue(0); f.setInterpolator(Interpolator.EASE_IN);
        TranslateTransition s = new TranslateTransition(Duration.millis(120), node);
        s.setFromY(0); s.setToY(-6); s.setInterpolator(Interpolator.EASE_IN);
        ParallelTransition pt = new ParallelTransition(f, s);
        pt.setOnFinished(e -> { if (onFinished != null) onFinished.run(); });
        pt.play();
    }
    private void showNotification(String title, String msg) { showAlert(Alert.AlertType.INFORMATION, title, msg); }
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.show();
    }
    private String nvl(String s, String def) { return (s != null && !s.isBlank()) ? s : def; }
    private String truncate(String s, int max) {
        if (s == null) return ""; return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}