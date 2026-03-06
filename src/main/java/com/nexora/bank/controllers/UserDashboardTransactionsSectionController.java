package com.nexora.bank.controllers;

import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.Reclamation;
import com.nexora.bank.Models.Transaction;
import com.nexora.bank.Service.BadWordService;
import com.nexora.bank.Service.CurrencyService;
import com.nexora.bank.Service.GeminiService;
import com.nexora.bank.Service.QRCodeService;
import com.nexora.bank.Service.ReclamationService;
import com.nexora.bank.Service.StripeService;
import com.nexora.bank.Service.TransactionService;
import com.nexora.bank.Utils.EmailUtil;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.stripe.model.checkout.Session;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class UserDashboardTransactionsSectionController implements Initializable {

    // ─── Services ─────────────────────────────────────────────────────────────
    private final TransactionService transactionService = new TransactionService();
    private final ReclamationService reclamationService = new ReclamationService();
    private final StripeService      stripeService      = new StripeService();
    private final GeminiService      geminiService      = new GeminiService();
    private final QRCodeService      qrCodeService      = new QRCodeService();
    private final BadWordService     badWordService     = new BadWordService();
    private final CurrencyService    currencyService    = new CurrencyService();

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

    // ─── Labels devise ────────────────────────────────────────────────────────
    @FXML private Label lblCurrencySymbol;
    @FXML private Label lblMontantConverti;

    // ─── Formulaire réclamation ───────────────────────────────────────────────
    @FXML private ComboBox<String> cbTypeReclamation;
    @FXML private ComboBox<String> cbPrioriteReclamation;
    @FXML private TextArea         txtDescriptionReclamation;
    @FXML private Label            lblTypeReclamationError;
    @FXML private Label            lblDescriptionReclamationError;
    @FXML private Label            lblReclamationTransactionInfo;
    @FXML private Button           btnGenererDescription;

    // ─── Zone cartes ─────────────────────────────────────────────────────────
    @FXML private VBox       transactionsCardsContainer;
    @FXML private ScrollPane transactionsScrollPane;

    // ─── Stats / recherche / tri ──────────────────────────────────────────────
    @FXML private Label            lblTotalTransactions;
    @FXML private Label            lblTableInfo;
    @FXML private TextField        txtRecherche;
    @FXML private Button           btnAjouter;
    @FXML private Button           btnAnalyseIA;
    @FXML private ComboBox<String> cbSortCritere;

    // ── Constantes tri ─────────────────────────────────────────────────────────
    private static final String SORT_MONTANT_DESC = "Montant décroissant";
    private static final String SORT_MONTANT_ASC  = "Montant croissant";
    private static final String SORT_STATUT       = "Statut A-Z";

    // ── Palette Emerald ────────────────────────────────────────────────────────
    private static final String BG_CARD        = "#FFFFFF";
    private static final String BG_HOVER       = "#F0FDF4";
    private static final String CLR_CREDIT     = "#059669";
    private static final String CLR_DEBIT      = "#0A2540";
    private static final String CLR_PENDING    = "#D97706";
    private static final String CLR_FAILED     = "#DC2626";
    private static final String CLR_PAID       = "#10B981";
    private static final String BTN_EDIT_CLR   = "#059669";
    private static final String BTN_EDIT_BG    = "#D1FAE5";
    private static final String BTN_DELETE_CLR = "#DC2626";
    private static final String BTN_DELETE_BG  = "#FEE2E2";
    private static final String BTN_CLAIM_CLR  = "#D97706";
    private static final String BTN_CLAIM_BG   = "#FEF3C7";
    private static final String BTN_PAY_CLR    = "#059669";
    private static final String BTN_PAY_BG     = "#D1FAE5";
    private static final String TXT_PRIMARY    = "#0A2540";
    private static final String TXT_SECONDARY  = "#64748B";
    private static final String TXT_MUTED      = "#94A3B8";
    private static final String BORDER_CARD    = "#E2E8F0";

    // ── État interne ──────────────────────────────────────────────────────────
    private final ObservableList<Transaction> transactionsList = FXCollections.observableArrayList();
    private FilteredList<Transaction>         filteredData;
    private Transaction selectedTransaction    = null;
    private Transaction reclamationTransaction = null;
    private boolean     isEditMode             = false;
    private String      sortField              = "montant";
    private boolean     sortAscending          = false;

    // ── Pagination ────────────────────────────────────────────────────────────
    private static final int      PAGE_SIZE          = 4;
    private              int      currentPage        = 0;
    private static final Duration ANIMATION_DURATION = Duration.millis(200);
    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ══════════════════════════════════════════════════════════════════════════
    // Initialisation
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setVisibleManaged(reclamationFormContainer, false);
        setVisibleManaged(reclamationsDisplayContainer, false);
        setupSortComboBox();
        setupSearch();
        setupRealTimeValidation();
        initCurrency();
        refreshData();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Devise
    // ══════════════════════════════════════════════════════════════════════════

    private void initCurrency() {
        if (lblCurrencySymbol  != null) lblCurrencySymbol.setText("DT");
        if (lblMontantConverti != null) {
            lblMontantConverti.setText("Détection de votre devise...");
            lblMontantConverti.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:10px;-fx-font-style:italic;");
        }
        new Thread(() -> {
            String currency = currencyService.detectCurrency();
            String symbol   = currencyService.getSymbol();
            Platform.runLater(() -> {
                if (lblCurrencySymbol != null) lblCurrencySymbol.setText(symbol);
                if (lblMontantConverti != null) {
                    if (CurrencyService.CURRENCY_TND.equals(currency))
                        lblMontantConverti.setText("🇹🇳 Tunisie — Dinar tunisien (DT)");
                    else if (CurrencyService.CURRENCY_EUR.equals(currency))
                        lblMontantConverti.setText(String.format("🇪🇺 Zone Euro — 1 DT = %.4f €", currencyService.getRateEUR()));
                    else
                        lblMontantConverti.setText(String.format("🌍 Dollar — 1 DT = %.4f $", currencyService.getRateUSD()));
                    lblMontantConverti.setStyle("-fx-text-fill:#059669;-fx-font-size:10px;-fx-font-weight:bold;");
                }
                if (txtMontant != null && !txtMontant.getText().isBlank())
                    updateMontantConversion(txtMontant.getText().trim());
            });
        }).start();
    }

    private void updateMontantConversion(String texte) {
        if (lblMontantConverti == null || texte == null || texte.isBlank()) {
            if (lblMontantConverti != null) lblMontantConverti.setText(""); return;
        }
        try {
            double montant  = Double.parseDouble(texte);
            String currency = currencyService.detectCurrency();
            if (CurrencyService.CURRENCY_TND.equals(currency)) {
                lblMontantConverti.setText(String.format("🇹🇳 %.2f DT  ≈  € %.2f  |  $ %.2f",
                    montant, montant * currencyService.getRateEUR(), montant * currencyService.getRateUSD()));
                lblMontantConverti.setStyle("-fx-text-fill:#059669;-fx-font-size:10px;-fx-font-weight:bold;");
            } else if (CurrencyService.CURRENCY_EUR.equals(currency)) {
                double enTND = montant / currencyService.getRateEUR();
                lblMontantConverti.setText(String.format("🇪🇺 € %.2f  ≈  %.2f DT  |  $ %.2f",
                    montant, enTND, enTND * currencyService.getRateUSD()));
                lblMontantConverti.setStyle("-fx-text-fill:#0A2540;-fx-font-size:10px;-fx-font-weight:bold;");
            } else {
                double enTND = montant / currencyService.getRateUSD();
                lblMontantConverti.setText(String.format("🌍 $ %.2f  ≈  %.2f DT  |  € %.2f",
                    montant, enTND, enTND * currencyService.getRateEUR()));
                lblMontantConverti.setStyle("-fx-text-fill:#D97706;-fx-font-size:10px;-fx-font-weight:bold;");
            }
        } catch (NumberFormatException e) { lblMontantConverti.setText(""); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // IA
    // ══════════════════════════════════════════════════════════════════════════

    @FXML private void ouvrirAnalyseIA() {
        new AIPredictionController().afficher((Stage) btnAnalyseIA.getScene().getWindow());
    }

    @FXML
    private void genererDescriptionIA() {
        if (txtDescriptionReclamation == null) return;
        String texteActuel = txtDescriptionReclamation.getText().trim();
        if (texteActuel.isEmpty())    { showNotification("IA", "⚠️ Saisissez une description avant de l'améliorer."); return; }
        if (texteActuel.length() < 5) { showNotification("IA", "⚠️ Description trop courte."); return; }
        if (btnGenererDescription != null) { btnGenererDescription.setDisable(true); btnGenererDescription.setText("⏳ Génération..."); }
        if (lblDescriptionReclamationError != null) {
            lblDescriptionReclamationError.setStyle("-fx-text-fill:#059669;-fx-font-size:10px;-fx-font-weight:bold;");
            lblDescriptionReclamationError.setText("🤖 L'IA améliore votre description...");
        }
        String texteAEnvoyer = texteActuel;
        new Thread(() -> {
            try {
                String amelioree = geminiService.ameliorerDescription(texteAEnvoyer);
                Platform.runLater(() -> {
                    txtDescriptionReclamation.setText(amelioree);
                    txtDescriptionReclamation.positionCaret(amelioree.length());
                    if (lblDescriptionReclamationError != null) {
                        lblDescriptionReclamationError.setStyle("-fx-text-fill:#059669;-fx-font-size:10px;-fx-font-weight:bold;");
                        lblDescriptionReclamationError.setText("✅ Description améliorée par l'IA !");
                    }
                    if (btnGenererDescription != null) { btnGenererDescription.setDisable(false); btnGenererDescription.setText("✨ Améliorer avec IA"); }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblDescriptionReclamationError != null) {
                        lblDescriptionReclamationError.setStyle("-fx-text-fill:#DC2626;-fx-font-size:10px;-fx-font-weight:bold;");
                        lblDescriptionReclamationError.setText("❌ Erreur IA : " + e.getMessage());
                    }
                    if (btnGenererDescription != null) { btnGenererDescription.setDisable(false); btnGenererDescription.setText("✨ Améliorer avec IA"); }
                });
            }
        }).start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tri
    // ══════════════════════════════════════════════════════════════════════════

    private void setupSortComboBox() {
        if (cbSortCritere == null) return;
        cbSortCritere.setItems(FXCollections.observableArrayList(SORT_MONTANT_DESC, SORT_MONTANT_ASC, SORT_STATUT));
        cbSortCritere.setValue(SORT_MONTANT_DESC);
        cbSortCritere.valueProperty().addListener((obs, o, v) -> { if (v != null) appliquerTriDepuis(v); });
    }

    private void appliquerTriDepuis(String critere) {
        if      (critere.equals(SORT_MONTANT_DESC)) { sortField = "montant"; sortAscending = false; }
        else if (critere.equals(SORT_MONTANT_ASC))  { sortField = "montant"; sortAscending = true;  }
        else if (critere.equals(SORT_STATUT))       { sortField = "statut";  sortAscending = true;  }
        currentPage = 0;
        if (filteredData != null) renderCards();
    }

    @FXML private void appliquerTri() {
        if (cbSortCritere != null && cbSortCritere.getValue() != null)
            appliquerTriDepuis(cbSortCritere.getValue());
    }

    private Comparator<Transaction> buildComparator() {
        Comparator<Transaction> cmp = sortField.equals("statut")
            ? Comparator.comparing(t -> nvl(t.getStatutTransaction(), ""))
            : Comparator.comparingDouble(t -> t.getMontant() != null ? t.getMontant() : 0.0);
        return sortAscending ? cmp : cmp.reversed();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Données & rendu
    // ══════════════════════════════════════════════════════════════════════════

    private void refreshData() {
        transactionsList.setAll(transactionService.getAll());
        filteredData = new FilteredList<>(transactionsList, t -> true);
        applySearchFilter(txtRecherche != null ? txtRecherche.getText() : "");
        renderCards();
        updateStats();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FIX PAGINATION : renderCards() entièrement réécrit
    // ══════════════════════════════════════════════════════════════════════════

    private void renderCards() {
        if (transactionsCardsContainer == null) return;
        transactionsCardsContainer.getChildren().clear();
        transactionsCardsContainer.setSpacing(8);
        transactionsCardsContainer.setFillWidth(true);
        transactionsCardsContainer.setAlignment(Pos.TOP_LEFT);

        // 1. Trier une snapshot stable de filteredData
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

        // 2. Borner currentPage AVANT de calculer les indices
        int totalPages = (int) Math.ceil((double) sorted.size() / PAGE_SIZE);
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0)          currentPage = 0;

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, sorted.size());

        // 3. Ajouter les cartes de la page courante
        for (Transaction t : sorted.subList(from, to))
            transactionsCardsContainer.getChildren().add(buildCardWithReclamations(t));

        // 4. Capturer les valeurs finales pour la lambda (effectively final)
        final int totalPagesFinal = totalPages;
        final int totalItemsFinal = sorted.size();

        // 5. FIX : ajouter la barre de pagination via Platform.runLater()
        //    pour éviter que le bouton cliqué soit détruit pendant l'exécution
        //    de son propre handler (ce qui causait le blocage de pagination).
        Platform.runLater(() ->
            transactionsCardsContainer.getChildren().add(
                buildPaginationBar(totalPagesFinal, totalItemsFinal)
            )
        );

        updateTableInfo(totalItemsFinal);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Wrapper carte + QR + réclamations
    // ══════════════════════════════════════════════════════════════════════════

    private VBox buildCardWithReclamations(Transaction t) {
        VBox wrapper = new VBox(0);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setMaxHeight(Region.USE_PREF_SIZE);
        VBox.setVgrow(wrapper, Priority.NEVER);

        List<Reclamation> reclamations = reclamationService.getByTransaction(t.getIdTransaction());
        int nbRec = reclamations.size();

        // ── Panel réclamations ────────────────────────────────────────────────
        VBox reclamationsPanel = new VBox(8);
        reclamationsPanel.setVisible(false);
        reclamationsPanel.setManaged(false);
        reclamationsPanel.setPadding(new Insets(12, 16, 12, 20));
        reclamationsPanel.setStyle(
            "-fx-background-color:#F0FDF4;" +
            "-fx-border-color:#10B981;" +
            "-fx-border-width:0 0 0 3;");

        HBox panelHeader = new HBox(8);
        panelHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblPanelTitle = new Label("👁 Réclamations associées");
        lblPanelTitle.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:#059669;");
        Label lblPanelCount = new Label(nbRec == 0
            ? "aucune réclamation"
            : nbRec + " réclamation" + (nbRec > 1 ? "s" : ""));
        lblPanelCount.setStyle("-fx-font-size:10px;-fx-text-fill:#64748B;" +
            "-fx-background-color:" + (nbRec > 0 ? "#D1FAE5" : "#F1F5F9") + ";" +
            "-fx-padding:1 8 1 8;-fx-background-radius:20;");
        panelHeader.getChildren().addAll(lblPanelTitle, lblPanelCount);
        reclamationsPanel.getChildren().add(panelHeader);

        Region sep = new Region();
        sep.setPrefHeight(1); sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color:#A7F3D0;");
        reclamationsPanel.getChildren().add(sep);

        if (reclamations.isEmpty()) {
            HBox emptyBox = new HBox();
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(12));
            Label lblEmpty = new Label("😊 Aucune réclamation pour cette transaction");
            lblEmpty.setStyle("-fx-font-size:11px;-fx-text-fill:#94A3B8;-fx-font-style:italic;");
            emptyBox.getChildren().add(lblEmpty);
            reclamationsPanel.getChildren().add(emptyBox);
        } else {
            for (Reclamation r : reclamations)
                reclamationsPanel.getChildren().add(buildReclamationCard(r));
        }

        HBox card = buildCard(t, reclamationsPanel, nbRec);
        VBox qrContainer = (VBox) card.getUserData();

        wrapper.getChildren().add(card);
        if (qrContainer != null) wrapper.getChildren().add(qrContainer);
        wrapper.getChildren().add(reclamationsPanel);

        return wrapper;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Carte réclamation individuelle
    // ══════════════════════════════════════════════════════════════════════════

    private HBox buildReclamationCard(Reclamation r) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        String color = switch (nvl(r.getStatus(), "")) {
            case "Signalée"   -> "#DC2626";
            case "En attente" -> "#D97706";
            case "En cours"   -> "#2563EB";
            case "Résolue"    -> "#10B981";
            case "Rejetée"    -> "#64748B";
            default           -> "#94A3B8";
        };

        card.setStyle(
            "-fx-background-color:" + (r.isInappropriate() ? "#FFF5F5" : "#FFFFFF") + ";" +
            "-fx-background-radius:10;-fx-border-color:" + color + "44;" +
            "-fx-border-radius:10;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

        Label iconLbl = new Label(switch (nvl(r.getStatus(), "")) {
            case "Résolue"  -> "✅";
            case "Rejetée"  -> "❌";
            case "En cours" -> "🔄";
            case "Signalée" -> "⚠️";
            default         -> "🕐";
        });
        iconLbl.setMinSize(32, 32); iconLbl.setMaxSize(32, 32);
        iconLbl.setAlignment(Pos.CENTER);
        iconLbl.setStyle("-fx-background-color:" + color + "18;-fx-background-radius:8;-fx-font-size:14px;");

        VBox infos = new VBox(4);
        HBox.setHgrow(infos, Priority.ALWAYS);

        HBox ligne1 = new HBox(6); ligne1.setAlignment(Pos.CENTER_LEFT);
        Label lblType = new Label(nvl(r.getTypeReclamation(), "—"));
        lblType.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:#0A2540;");
        Label badgeStatut = new Label(nvl(r.getStatus(), "—").toUpperCase());
        badgeStatut.setStyle(
            "-fx-background-color:" + color + "18;-fx-text-fill:" + color + ";" +
            "-fx-font-size:8px;-fx-font-weight:700;-fx-padding:1 6 1 6;" +
            "-fx-background-radius:20;-fx-border-color:" + color + "55;" +
            "-fx-border-radius:20;-fx-border-width:1;");
        ligne1.getChildren().addAll(lblType, badgeStatut);
        if (r.isInappropriate()) {
            Label badgeBad = new Label("⚠ SIGNALÉE");
            badgeBad.setStyle("-fx-background-color:#FEE2E2;-fx-text-fill:#DC2626;" +
                "-fx-font-size:8px;-fx-font-weight:700;-fx-padding:1 6 1 6;-fx-background-radius:20;");
            ligne1.getChildren().add(badgeBad);
        }
        Label lblDate = new Label("📅 " + nvl(r.getDateReclamation(), "—"));
        lblDate.setStyle("-fx-font-size:10px;-fx-text-fill:#64748B;");
        String descText = r.isBlurred() ? "🔒 Contenu masqué par l'administrateur" : nvl(r.getDescription(), "—");
        Label lblDesc = new Label(truncate(descText, 100));
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-font-size:10px;" + (r.isBlurred()
            ? "-fx-text-fill:#94A3B8;-fx-font-style:italic;"
            : r.isInappropriate() ? "-fx-text-fill:#DC2626;" : "-fx-text-fill:#475569;"));
        if (r.isBlurred()) lblDesc.setEffect(new javafx.scene.effect.GaussianBlur(3));
        infos.getChildren().addAll(ligne1, lblDate, lblDesc);

        Label lblId = new Label("#" + r.getIdReclamation());
        lblId.setStyle("-fx-font-size:10px;-fx-text-fill:#94A3B8;-fx-font-weight:700;");
        card.getChildren().addAll(iconLbl, infos, lblId);
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Carte transaction principale
    // ══════════════════════════════════════════════════════════════════════════

    private HBox buildCard(Transaction t, VBox reclamationsPanel, int nbRec) {
        boolean isCredit   = "Credit".equalsIgnoreCase(t.getTypeTransaction());
        boolean isPending  = "En attente".equalsIgnoreCase(t.getStatutTransaction());
        boolean isFailed   = "Echouee".equalsIgnoreCase(t.getStatutTransaction());
        boolean isFullPaid = t.isFullyPaid();

        String accent = isFailed ? CLR_FAILED : isPending ? CLR_PENDING : isCredit ? CLR_CREDIT : CLR_DEBIT;

        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(72);
        card.setPrefHeight(72);
        card.setMaxHeight(72);
        card.setStyle(compactCardStyle(accent, false));
        card.setOnMouseEntered(e -> card.setStyle(compactCardStyle(accent, true)));
        card.setOnMouseExited(e  -> card.setStyle(compactCardStyle(accent, false)));

        // ── Icône ronde ────────────────────────────────────────────────────────
        Label iconCircle = new Label(isFailed ? "✕" : isPending ? "⏳" : isCredit ? "↓" : "↑");
        iconCircle.setMinSize(40, 40); iconCircle.setMaxSize(40, 40);
        iconCircle.setAlignment(Pos.CENTER);
        iconCircle.setStyle(
            "-fx-background-color:" + accent + "22;" +
            "-fx-background-radius:999;" +
            "-fx-text-fill:" + accent + ";" +
            "-fx-font-size:15px;-fx-font-weight:bold;");

        // ── Infos centre ───────────────────────────────────────────────────────
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        info.setMaxHeight(Region.USE_PREF_SIZE);

        HBox row1 = new HBox(6); row1.setAlignment(Pos.CENTER_LEFT);
        Label lblCat = new Label(nvl(t.getCategorie(), "—"));
        lblCat.setStyle("-fx-font-weight:700;-fx-font-size:13px;-fx-text-fill:" + TXT_PRIMARY + ";");
        row1.getChildren().addAll(lblCat, buildCompactBadge(nvl(t.getStatutTransaction(), "—"), accent));
        if (isFullPaid) row1.getChildren().add(buildCompactBadge("PAYÉE ✓", CLR_PAID));

        HBox row2 = new HBox(4); row2.setAlignment(Pos.CENTER_LEFT);
        row2.getChildren().addAll(
            buildMeta(nvl(t.getTypeTransaction(), "—")),
            buildSep(),
            buildMeta(t.getDateTransaction() != null ? t.getDateTransaction().format(DATE_FMT) : "—")
        );
        if (t.getDescription() != null && !t.getDescription().isBlank())
            row2.getChildren().addAll(buildSep(), buildMetaItalic(truncate(t.getDescription(), 22)));

        // ── Barre de progression ───────────────────────────────────────────────
        double pct = t.getProgressionPourcentage();
        HBox progressBar = new HBox();
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(3);
        progressBar.setMaxHeight(3);
        progressBar.setStyle("-fx-background-color:#E2E8F0;-fx-background-radius:10;");
        Region filled = new Region();
        filled.setPrefHeight(3); filled.setMaxHeight(3); filled.setPrefWidth(0);
        filled.setStyle("-fx-background-color:" + (isFullPaid ? CLR_PAID : accent) + ";-fx-background-radius:10;");
        progressBar.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW.doubleValue() > 0 && oldW.doubleValue() == 0) {
                double tw = newW.doubleValue() * pct / 100.0;
                Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,       new KeyValue(filled.prefWidthProperty(), 0)),
                    new KeyFrame(Duration.millis(800), new KeyValue(filled.prefWidthProperty(), tw, Interpolator.EASE_OUT))
                );
                Platform.runLater(tl::play);
            }
        });
        progressBar.getChildren().add(filled);
        info.getChildren().addAll(row1, row2, progressBar);

        // ── Montant ────────────────────────────────────────────────────────────
        double montantTND      = t.getMontant() != null ? t.getMontant() : 0.0;
        String currency        = currencyService.detectCurrency();
        double montantConverti = currencyService.convert(montantTND);
        String symbole         = currencyService.getSymbol();

        String texteMonant = CurrencyService.CURRENCY_TND.equals(currency)
            ? String.format("%.2f DT", montantTND)
            : String.format("%.2f %s", montantConverti, symbole);

        Label montantLabel = new Label((isCredit ? "+" : "−") + texteMonant);
        montantLabel.setMinWidth(85);
        montantLabel.setAlignment(Pos.CENTER_RIGHT);
        montantLabel.setStyle(
            "-fx-font-size:13px;-fx-font-weight:700;" +
            "-fx-text-fill:" + (isCredit ? CLR_CREDIT : CLR_DEBIT) + ";");

        // ── Boutons d'action ───────────────────────────────────────────────────
        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER);

        Button btnEdit  = buildRoundBtn("✎", BTN_EDIT_CLR,   BTN_EDIT_BG,   "Modifier");
        btnEdit.setOnAction(e -> editTransaction(t));

        Button btnDel   = buildRoundBtn("✕", BTN_DELETE_CLR, BTN_DELETE_BG, "Supprimer");
        btnDel.setOnAction(e -> confirmAndDelete(t));

        Button btnClaim = buildRoundBtn("⚑", BTN_CLAIM_CLR,  BTN_CLAIM_BG,  "Réclamation");
        btnClaim.setOnAction(e -> openReclamationForm(t));

        Button btnPay = buildPayBtn(t);
        Button btnEye = buildEyeButton(reclamationsPanel, nbRec);

        VBox   qrContainer = buildQRContainer(t);
        Button btnQR = buildRoundBtn("⬛", "#475569", "#F1F5F9", "QR Code");
        btnQR.setOnAction(e -> toggleQR(qrContainer, btnQR));

        actions.getChildren().addAll(btnEdit, btnDel, btnClaim, btnPay, btnEye, btnQR);
        card.getChildren().addAll(iconCircle, info, montantLabel, actions);
        card.setUserData(qrContainer);
        return card;
    }

    // ── Style compact carte ────────────────────────────────────────────────────
    private String compactCardStyle(String accent, boolean hover) {
        return "-fx-background-color:" + (hover ? BG_HOVER : BG_CARD) + ";" +
               "-fx-background-radius:14;" +
               "-fx-border-color:" + (hover ? accent + "88" : BORDER_CARD) + ";" +
               "-fx-border-radius:14;-fx-border-width:1;" +
               "-fx-effect:dropshadow(gaussian," +
               (hover ? "rgba(10,37,64,0.10)" : "rgba(10,37,64,0.05)") + "," +
               (hover ? "12" : "6") + ",0,0," + (hover ? "3" : "1") + ");" +
               (hover ? "-fx-cursor:hand;" : "");
    }

    // ── Bouton rond ───────────────────────────────────────────────────────────
    private Button buildRoundBtn(String icon, String color, String bg, String tooltipText) {
        Button btn = new Button(icon);
        btn.setMinSize(32, 32); btn.setMaxSize(32, 32);
        btn.setAlignment(Pos.CENTER);
        Tooltip tip = new Tooltip(tooltipText);
        tip.setStyle("-fx-background-color:#0A2540;-fx-text-fill:#F1F5F9;-fx-font-size:10px;-fx-background-radius:6;");
        Tooltip.install(btn, tip);
        String base  = "-fx-background-color:" + bg + ";-fx-text-fill:" + color + ";" +
                       "-fx-font-size:13px;-fx-background-radius:999;" +
                       "-fx-border-color:" + color + "44;-fx-border-radius:999;-fx-border-width:1;" +
                       "-fx-cursor:hand;-fx-padding:0;";
        String hover = "-fx-background-color:" + color + ";-fx-text-fill:#FFFFFF;" +
                       "-fx-font-size:13px;-fx-background-radius:999;" +
                       "-fx-border-color:transparent;-fx-border-radius:999;-fx-border-width:1;" +
                       "-fx-cursor:hand;-fx-padding:0;" +
                       "-fx-effect:dropshadow(gaussian," + color + "66,6,0,0,1);";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    // ── Bouton paiement Stripe ────────────────────────────────────────────────
    private Button buildPayBtn(Transaction t) {
        boolean paid  = t.isFullyPaid();
        String symbol = paid ? "✓" : "$";
        String color  = paid ? CLR_PAID : BTN_PAY_CLR;
        String bg     = paid ? "#D1FAE5" : BTN_PAY_BG;

        Button btn = new Button(symbol);
        btn.setMinSize(32, 32); btn.setMaxSize(32, 32);
        btn.setAlignment(Pos.CENTER);
        Tooltip tip = new Tooltip(paid ? "Transaction entièrement payée" : "Payer via Stripe");
        tip.setStyle("-fx-background-color:#0A2540;-fx-text-fill:#F1F5F9;-fx-font-size:10px;-fx-background-radius:6;");
        Tooltip.install(btn, tip);

        String base = "-fx-background-color:" + bg + ";-fx-text-fill:" + color + ";" +
                      "-fx-font-size:14px;-fx-font-weight:700;-fx-background-radius:999;" +
                      "-fx-border-color:" + color + "55;-fx-border-radius:999;-fx-border-width:1;" +
                      (paid ? "" : "-fx-cursor:hand;") + "-fx-padding:0;";
        btn.setStyle(base);
        btn.setDisable(paid);
        if (!paid) {
            String hover = "-fx-background-color:" + color + ";-fx-text-fill:#FFFFFF;" +
                           "-fx-font-size:14px;-fx-font-weight:700;-fx-background-radius:999;" +
                           "-fx-border-color:transparent;-fx-border-radius:999;-fx-border-width:1;" +
                           "-fx-cursor:hand;-fx-padding:0;" +
                           "-fx-effect:dropshadow(gaussian," + color + "66,6,0,0,1);";
            btn.setOnMouseEntered(e -> btn.setStyle(hover));
            btn.setOnMouseExited(e  -> btn.setStyle(base));
            btn.setOnAction(e -> payerViaStripe(t));
        }
        return btn;
    }

    // ── QR Container ─────────────────────────────────────────────────────────
    private VBox buildQRContainer(Transaction t) {
        VBox qrContainer = new VBox(4);
        qrContainer.setAlignment(Pos.CENTER_LEFT);
        qrContainer.setVisible(false);
        qrContainer.setManaged(false);
        qrContainer.setPadding(new Insets(8, 16, 8, 16));
        qrContainer.setStyle(
            "-fx-background-color:#F8FAFC;" +
            "-fx-border-color:#E2E8F0;" +
            "-fx-border-width:0 0 0 3;");

        String qrData = qrCodeService.formatTransactionData(
            t.getIdTransaction(), nvl(t.getCategorie(), "—"), t.getMontant(),
            nvl(t.getTypeTransaction(), "—"), nvl(t.getStatutTransaction(), "—"),
            t.getDateTransaction() != null ? t.getDateTransaction().format(DATE_FMT) : "—");
        Image qrImage = qrCodeService.generateQRCode(qrData);

        if (qrImage != null) {
            VBox qrFrame = new VBox();
            qrFrame.setAlignment(Pos.CENTER);
            qrFrame.setPadding(new Insets(4));
            qrFrame.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:8;" +
                "-fx-border-color:#E2E8F0;-fx-border-radius:8;-fx-border-width:1;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),6,0,0,2);");
            ImageView qrView = new ImageView(qrImage);
            qrView.setFitWidth(140); qrView.setFitHeight(140); qrView.setSmooth(false);
            qrFrame.getChildren().add(qrView);
            Label qrLabel = new Label("QR Transaction #" + t.getIdTransaction());
            qrLabel.setStyle("-fx-font-size:9px;-fx-text-fill:" + TXT_MUTED + ";");
            qrContainer.getChildren().addAll(qrFrame, qrLabel);
        }
        return qrContainer;
    }

    private void toggleQR(VBox qrContainer, Button btnQR) {
        boolean nowVisible = !qrContainer.isVisible();
        if (nowVisible) {
            qrContainer.setVisible(true); qrContainer.setManaged(true);
            FadeTransition ft = new FadeTransition(Duration.millis(200), qrContainer);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
            btnQR.setText("✖");
        } else {
            FadeTransition ft = new FadeTransition(Duration.millis(150), qrContainer);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(ev -> { qrContainer.setVisible(false); qrContainer.setManaged(false); });
            ft.play();
            btnQR.setText("⬛");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Bouton œil réclamations
    // ══════════════════════════════════════════════════════════════════════════

    private Button buildEyeButton(VBox reclamationsPanel, int nbRec) {
        Button btn = buildRoundBtn("👁",
            nbRec > 0 ? "#059669" : "#94A3B8",
            nbRec > 0 ? "#D1FAE5" : "#F1F5F9",
            nbRec == 0 ? "Aucune réclamation" : "Voir " + nbRec + " réclamation" + (nbRec > 1 ? "s" : ""));

        String closedStyle =
            "-fx-background-color:#DC2626;-fx-text-fill:#FFFFFF;" +
            "-fx-font-size:13px;-fx-background-radius:999;" +
            "-fx-border-color:transparent;-fx-border-radius:999;-fx-border-width:1;" +
            "-fx-cursor:hand;-fx-padding:0;";

        btn.setOnAction(e -> {
            boolean nowVisible = !reclamationsPanel.isVisible();
            if (nowVisible) {
                reclamationsPanel.setVisible(true); reclamationsPanel.setManaged(true);
                FadeTransition ft = new FadeTransition(Duration.millis(250), reclamationsPanel);
                ft.setFromValue(0); ft.setToValue(1); ft.play();
                btn.setText("✕"); btn.setStyle(closedStyle);
                btn.setOnMouseEntered(ev -> {}); btn.setOnMouseExited(ev -> {});
            } else {
                FadeTransition ft = new FadeTransition(Duration.millis(150), reclamationsPanel);
                ft.setFromValue(1); ft.setToValue(0);
                ft.setOnFinished(ev -> { reclamationsPanel.setVisible(false); reclamationsPanel.setManaged(false); });
                ft.play();
                btn.setText("👁");
                String baseR  = "-fx-background-color:" + (nbRec > 0 ? "#D1FAE5" : "#F1F5F9") + ";" +
                                "-fx-text-fill:" + (nbRec > 0 ? "#059669" : "#94A3B8") + ";" +
                                "-fx-font-size:13px;-fx-background-radius:999;" +
                                "-fx-border-color:" + (nbRec > 0 ? "#059669" : "#94A3B8") + "44;" +
                                "-fx-border-radius:999;-fx-border-width:1;-fx-cursor:hand;-fx-padding:0;";
                String hoverR = "-fx-background-color:" + (nbRec > 0 ? "#059669" : "#E2E8F0") + ";" +
                                "-fx-text-fill:#FFFFFF;-fx-font-size:13px;-fx-background-radius:999;" +
                                "-fx-border-color:transparent;-fx-border-radius:999;-fx-border-width:1;" +
                                "-fx-cursor:hand;-fx-padding:0;";
                btn.setStyle(baseR);
                btn.setOnMouseEntered(ev -> btn.setStyle(hoverR));
                btn.setOnMouseExited(ev  -> btn.setStyle(baseR));
            }
        });
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Stripe
    // ══════════════════════════════════════════════════════════════════════════

    private void payerViaStripe(Transaction t) {
        double resteAPayer = t.getMontantRestant();
        if (resteAPayer <= 0) { showNotification("Info", "Transaction déjà entièrement payée !"); return; }
        TextInputDialog dialog = new TextInputDialog(String.format("%.2f", resteAPayer));
        dialog.setTitle("Paiement Stripe");
        dialog.setHeaderText("Transaction : " + nvl(t.getCategorie(), "—") +
            "\nReste à payer : " + String.format("%.2f DT", resteAPayer));
        dialog.setContentText("Montant à payer maintenant (DT) :");
        dialog.showAndWait().ifPresent(input -> {
            try {
                double montantAPayer = Double.parseDouble(input.trim());
                if (montantAPayer <= 0) { showNotification("Erreur", "❌ Montant doit être positif !"); return; }
                if (montantAPayer > resteAPayer) {
                    showNotification("Erreur", "❌ Trop élevé ! Reste : " + String.format("%.2f DT", resteAPayer)); return;
                }
                Session session = stripeService.creerCheckoutSession(montantAPayer, t.getCategorie());
                Desktop.getDesktop().browse(new URI(session.getUrl()));
                transactionService.updateMontantPaye(t.getIdTransaction(), montantAPayer);
                double nouveauPct = Math.min(((t.getMontantPaye() + montantAPayer) / t.getMontant()) * 100, 100);
                refreshData();
                showNotification("Paiement Stripe",
                    "🌐 Stripe ouvert !\n💳 Payé : " + String.format("%.2f DT", montantAPayer) +
                    "\n📊 Progression : " + String.format("%.0f%%", nouveauPct) +
                    (nouveauPct >= 100 ? " ✅ PAYÉE !" : ""));
            } catch (NumberFormatException ex) { showNotification("Erreur", "❌ Format invalide (ex: 500.00)");
            } catch (com.stripe.exception.StripeException se) { showNotification("Erreur Stripe", "❌ " + se.getMessage());
            } catch (Exception ex) { ex.printStackTrace(); showNotification("Erreur", "❌ " + ex.getMessage()); }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FIX PAGINATION : buildPaginationBar() entièrement réécrit
    // ══════════════════════════════════════════════════════════════════════════

    private HBox buildPaginationBar(int totalPages, int totalItems) {
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(10, 0, 4, 0));
        bar.setMaxHeight(Region.USE_PREF_SIZE);

        int from = currentPage * PAGE_SIZE + 1;
        int to   = Math.min(from + PAGE_SIZE - 1, totalItems);
        Label info = new Label(from + "–" + to + " sur " + totalItems);
        info.setStyle("-fx-font-size:11px;-fx-text-fill:" + TXT_SECONDARY + ";-fx-padding:0 8 0 0;");

        // FIX : utiliser Platform.runLater dans les handlers pour éviter
        // la destruction du bouton pendant l'exécution de son propre listener
        Button btnPrev = buildPageNavBtn("◀", currentPage > 0);
        btnPrev.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                Platform.runLater(this::renderCards);
            }
        });

        HBox pageButtons = new HBox(4);
        pageButtons.setAlignment(Pos.CENTER);

        for (int i = 0; i < totalPages; i++) {
            final int idx    = i;
            boolean   active = (i == currentPage);
            Label lbl = new Label(String.valueOf(i + 1));
            lbl.setMinSize(28, 28); lbl.setMaxSize(28, 28); lbl.setAlignment(Pos.CENTER);
            lbl.setStyle(active
                ? "-fx-background-color:#059669;-fx-text-fill:#FFFFFF;-fx-background-radius:999;" +
                  "-fx-font-size:11px;-fx-font-weight:700;" +
                  "-fx-effect:dropshadow(gaussian,rgba(5,150,105,0.35),6,0,0,1);"
                : "-fx-background-color:#E2E8F0;-fx-text-fill:" + TXT_PRIMARY +
                  ";-fx-background-radius:999;-fx-font-size:11px;-fx-cursor:hand;");
            if (!active) {
                lbl.setOnMouseEntered(ev -> lbl.setStyle(
                    "-fx-background-color:#D1FAE5;-fx-text-fill:#059669;" +
                    "-fx-background-radius:999;-fx-font-size:11px;-fx-cursor:hand;"));
                lbl.setOnMouseExited(ev -> lbl.setStyle(
                    "-fx-background-color:#E2E8F0;-fx-text-fill:" + TXT_PRIMARY +
                    ";-fx-background-radius:999;-fx-font-size:11px;-fx-cursor:hand;"));
                // FIX : Platform.runLater pour les labels de page également
                lbl.setOnMouseClicked(ev -> {
                    currentPage = idx;
                    Platform.runLater(this::renderCards);
                });
            }
            pageButtons.getChildren().add(lbl);
        }

        // FIX : même correction pour le bouton suivant
        Button btnNext = buildPageNavBtn("▶", currentPage < totalPages - 1);
        btnNext.setOnAction(e -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                Platform.runLater(this::renderCards);
            }
        });

        bar.getChildren().addAll(info, btnPrev, pageButtons, btnNext);
        return bar;
    }

    private Button buildPageNavBtn(String label, boolean enabled) {
        Button btn = new Button(label);
        btn.setMinSize(28, 28); btn.setMaxSize(28, 28); btn.setDisable(!enabled);
        String base = enabled
            ? "-fx-background-color:#E2E8F0;-fx-text-fill:" + TXT_PRIMARY +
              ";-fx-background-radius:999;-fx-font-size:10px;-fx-cursor:hand;-fx-border-width:0;"
            : "-fx-background-color:#F1F5F9;-fx-text-fill:" + TXT_MUTED +
              ";-fx-background-radius:999;-fx-font-size:10px;-fx-border-width:0;";
        btn.setStyle(base);
        if (enabled) {
            btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:#D1FAE5;-fx-text-fill:#059669;" +
                "-fx-background-radius:999;-fx-font-size:10px;-fx-cursor:hand;-fx-border-width:0;"));
            btn.setOnMouseExited(e -> btn.setStyle(base));
        }
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Recherche
    // ══════════════════════════════════════════════════════════════════════════

    private void setupSearch() {
        if (txtRecherche != null)
            txtRecherche.textProperty().addListener((obs, o, v) -> {
                currentPage = 0; applySearchFilter(v); renderCards();
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

    private void updateStats()          { if (lblTotalTransactions != null) lblTotalTransactions.setText(String.valueOf(transactionsList.size())); }
    private void updateTableInfo(int c) { if (lblTableInfo != null) lblTableInfo.setText("Total : " + c + " transaction" + (c > 1 ? "s" : "")); }

    // ══════════════════════════════════════════════════════════════════════════
    // Réclamation
    // ══════════════════════════════════════════════════════════════════════════

    private void openReclamationForm(Transaction t) {
        reclamationTransaction = t;
        if (lblReclamationTransactionInfo != null)
            lblReclamationTransactionInfo.setText(
                "Transaction #" + t.getIdTransaction() + "  •  " + nvl(t.getCategorie(), "—") +
                "  •  " + String.format("%.2f DT", t.getMontant()) +
                (t.getDateTransaction() != null ? "  •  " + t.getDateTransaction().format(DATE_FMT) : ""));
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
        int    idUser   = AuthSession.getCurrentUser().getIdUser();
        String date     = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String type     = cbTypeReclamation.getValue();
        String priorite = cbPrioriteReclamation != null && cbPrioriteReclamation.getValue() != null
            ? cbPrioriteReclamation.getValue() : "Medium";
        String descFinal = "[Priorité : " + priorite + "] " + txtDescriptionReclamation.getText().trim();
        List<String> badWords = badWordService.findBadWords(descFinal);
        if (!badWords.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "⚠️ Votre description contient des mots inappropriés :\n" + String.join(", ", badWords) +
                "\n\nElle sera automatiquement signalée à l'administrateur.\nVoulez-vous quand même soumettre ?",
                ButtonType.OK, ButtonType.CANCEL);
            confirm.setTitle("⚠️ Contenu inapproprié détecté"); confirm.setHeaderText(null);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.CANCEL) return;
        }
        reclamationService.add(new Reclamation(
            idUser, reclamationTransaction.getIdTransaction(), date, type, descFinal, "En attente"));
        showNotification(
            !badWords.isEmpty() ? "Réclamation signalée ⚠️" : "Réclamation soumise ✅",
            !badWords.isEmpty()
                ? "Votre réclamation a été soumise mais signalée.\nTransaction #" + reclamationTransaction.getIdTransaction()
                : "Enregistrée !\nTransaction #" + reclamationTransaction.getIdTransaction() + "  •  Statut : En attente");
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
            { setError(lblTypeReclamationError, "❌ Type obligatoire."); valid = false; }
        String d = txtDescriptionReclamation != null ? txtDescriptionReclamation.getText().trim() : "";
        if      (d.isEmpty())      { setError(lblDescriptionReclamationError, "❌ Description obligatoire."); valid = false; }
        else if (d.length() < 10)  { setError(lblDescriptionReclamationError, "❌ Min 10 caractères.");       valid = false; }
        else if (d.length() > 500) { setError(lblDescriptionReclamationError, "❌ Max 500 caractères.");      valid = false; }
        return valid;
    }

    private void clearReclamationForm() {
        if (cbTypeReclamation         != null) cbTypeReclamation.setValue(null);
        if (cbPrioriteReclamation     != null) cbPrioriteReclamation.setValue(null);
        if (txtDescriptionReclamation != null) { txtDescriptionReclamation.clear(); txtDescriptionReclamation.setStyle(""); }
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
    // CRUD Transactions
    // ══════════════════════════════════════════════════════════════════════════

    @FXML private void newTransaction() { clearForm(); }

    @FXML
    private void saveTransaction() {
        if (!validateTransactionForm()) { showAlert(Alert.AlertType.WARNING, "Validation", "Corrigez les erreurs du formulaire."); return; }
        int       idUser    = AuthSession.getCurrentUser().getIdUser();
        String    emailUser = AuthSession.getCurrentUser().getEmail();
        String    cat       = cbCategorie.getValue();
        LocalDate date      = dpDate.getValue();
        double    montant   = currencyService.convertToTND(Double.parseDouble(txtMontant.getText().trim()));

        if (transactionService.isAnomalie(idUser, montant)) {
            double moyenne = transactionService.getMoyenneMontant(idUser);
            Alert alertAno = new Alert(Alert.AlertType.CONFIRMATION);
            alertAno.setTitle("⚠️ Montant inhabituel détecté"); alertAno.setHeaderText(null);
            alertAno.setContentText(
                "⚠️  Ce montant est inhabituellement élevé !\n\n" +
                "💰 Montant saisi   : " + String.format("%.2f DT", montant) + "\n" +
                "📊 Votre moyenne   : " + String.format("%.2f DT", moyenne) + "\n" +
                "📈 Seuil d'alerte  : " + String.format("%.2f DT", moyenne * 2.5) + "\n\n" +
                "Voulez-vous quand même enregistrer cette transaction ?");
            alertAno.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
            Optional<ButtonType> choix = alertAno.showAndWait();
            if (choix.isEmpty() || choix.get() == ButtonType.CANCEL) return;
        }

        String type = cbType.getValue(), statut = cbStatut.getValue(), desc = txtDescription.getText().trim();
        try {
            if (isEditMode && selectedTransaction != null) {
                transactionService.edit(new Transaction(
                    selectedTransaction.getIdTransaction(), selectedTransaction.getIdUser(),
                    cat, date, montant, type, statut, desc));
                new Thread(() -> EmailUtil.envoyerConfirmationTransaction(emailUser, cat, montant, type, statut)).start();
                showNotification("Succès", "✅ Transaction modifiée !");
            } else {
                transactionService.add(new Transaction(idUser, cat, date, montant, type, statut, desc));
                new Thread(() -> EmailUtil.envoyerConfirmationTransaction(emailUser, cat, montant, type, statut)).start();
                showNotification("Succès", "✅ Transaction ajoutée !\n📊 Progression : 0%\n💡 Cliquez $ pour payer via Stripe.");
            }
            clearForm(); refreshData();
        } catch (Exception e) { e.printStackTrace(); showAlert(Alert.AlertType.ERROR, "Erreur", "❌ " + e.getMessage()); }
    }

    @FXML private void deleteTransaction() {
        if (selectedTransaction == null) { showNotification("Info", "Sélectionnez une transaction."); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette transaction ?", ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Confirmation");
        a.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) confirmAndDelete(selectedTransaction); });
    }

    @FXML private void handleAnnuler() { clearForm(); }

    private void editTransaction(Transaction t) {
        selectedTransaction = t; populateForm(t); isEditMode = true;
        if (btnAjouter != null) btnAjouter.setText("Modifier");
    }

    private void confirmAndDelete(Transaction t) {
        transactionService.remove(t); clearForm(); refreshData();
        showNotification("Supprimé", "Transaction supprimée !");
    }

    private void populateForm(Transaction t) {
        if (cbCategorie    != null) cbCategorie.setValue(t.getCategorie());
        if (cbType         != null) cbType.setValue(t.getTypeTransaction());
        if (cbStatut       != null) cbStatut.setValue(t.getStatutTransaction());
        if (txtMontant     != null) txtMontant.setText(String.valueOf(t.getMontant()));
        if (txtDescription != null) txtDescription.setText(t.getDescription());
        if (dpDate         != null) dpDate.setValue(t.getDateTransaction());
    }

    @FXML private void clearForm() {
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

    @FXML private void exportTransactions() { exporterPDF(); }
    @FXML private void retryTransaction()   { showNotification("Nouvel essai", "Traitement en cours..."); }
    @FXML private void envoyerSMS()         { showNotification("SMS", "Fonction SMS non implémentée !"); }

    // Compatibility handler for older FXML versions that still bind onMouseClicked="#selectTransaction".
    @FXML
    private void selectTransaction(MouseEvent event) {
        if (event == null) return;
        if (selectedTransaction == null && !filteredData.isEmpty()) {
            selectedTransaction = filteredData.get(0);
            populateForm(selectedTransaction);
        }
    }

    @FXML
    private void exporterPDF() {
        if (transactionsList.isEmpty()) { showNotification("Export", "Aucune transaction à exporter !"); return; }
        FileChooser fc = new FileChooser(); fc.setTitle("Exporter PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(new Stage()); if (file == null) return;
        try {
            Document doc = new Document(new PdfDocument(new PdfWriter(file.getAbsolutePath())));
            doc.add(new Paragraph("Liste des transactions").setBold().setFontSize(16));
            doc.add(new Paragraph(" "));
            Table tbl = new Table(new float[]{90, 70, 80, 80, 70, 80, 120});
            for (String h : new String[]{"Catégorie","Date","Montant","Montant Payé","Progression","Type","Statut"})
                tbl.addHeaderCell(new Cell().add(new Paragraph(h)));
            for (Transaction t : filteredData) {
                tbl.addCell(nvl(t.getCategorie(), ""));
                tbl.addCell(t.getDateTransaction() != null ? t.getDateTransaction().toString() : "");
                tbl.addCell(String.format("%.2f DT", t.getMontant()));
                tbl.addCell(String.format("%.2f DT", t.getMontantPaye()));
                tbl.addCell(String.format("%.0f%%", t.getProgressionPourcentage()));
                tbl.addCell(nvl(t.getTypeTransaction(), ""));
                tbl.addCell(nvl(t.getStatutTransaction(), ""));
            }
            doc.add(tbl); doc.close();
            showNotification("Export", "✅ PDF exporté !");
        } catch (FileNotFoundException ex) { ex.printStackTrace(); showNotification("Erreur", "Impossible de créer le PDF !"); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Validation
    // ══════════════════════════════════════════════════════════════════════════

    private boolean validateTransactionForm() {
        boolean v = true; clearErrorLabels();
        if (cbCategorie == null || cbCategorie.getValue() == null)
            { setError(lblCategorieError, "❌ Catégorie obligatoire."); v = false; }
        LocalDate d = dpDate != null ? dpDate.getValue() : null;
        if      (d == null)                  { setError(lblDateError, "❌ Date obligatoire.");         v = false; }
        else if (d.isAfter(LocalDate.now())) { setError(lblDateError, "❌ Date future non permise."); v = false; }
        String mt = txtMontant != null ? txtMontant.getText().trim() : "";
        if (mt.isEmpty()) { setError(lblMontantError, "❌ Montant obligatoire."); v = false; }
        else {
            try {
                double m = Double.parseDouble(mt);
                if (m <= 0) { setError(lblMontantError, "❌ Positif requis."); v = false; }
            } catch (NumberFormatException e) { setError(lblMontantError, "❌ Format invalide."); v = false; }
        }
        if (cbType   == null || cbType.getValue()   == null) { setError(lblTypeError,   "❌ Type obligatoire.");   v = false; }
        if (cbStatut == null || cbStatut.getValue() == null) { setError(lblStatutError, "❌ Statut obligatoire."); v = false; }
        return v;
    }

    private void setupRealTimeValidation() {
        if (cbCategorie != null) cbCategorie.valueProperty().addListener((o, ov, v) -> {
            if (v != null && !v.isBlank()) setSuccess(lblCategorieError, "✓ OK"); else setError(lblCategorieError, "❌ Obligatoire.");
        });
        if (dpDate != null) dpDate.valueProperty().addListener((o, ov, v) -> {
            if (v == null) setError(lblDateError, "❌ Obligatoire.");
            else if (v.isAfter(LocalDate.now())) setError(lblDateError, "❌ Future.");
            else setSuccess(lblDateError, "✓ OK");
        });
        if (txtMontant != null) txtMontant.textProperty().addListener((o, ov, v) -> {
            if (v.isBlank()) { setError(lblMontantError, "❌ Obligatoire."); if (lblMontantConverti != null) lblMontantConverti.setText(""); return; }
            try { double m = Double.parseDouble(v); if (m <= 0) setError(lblMontantError, "❌ Positif requis."); else setSuccess(lblMontantError, "✓ OK"); }
            catch (NumberFormatException e) { setError(lblMontantError, "❌ Format invalide."); }
            updateMontantConversion(v);
        });
        if (cbType   != null) cbType.valueProperty().addListener((o, ov, v) -> {
            if (v != null && !v.isBlank()) setSuccess(lblTypeError, "✓ OK"); else setError(lblTypeError, "❌ Obligatoire.");
        });
        if (cbStatut != null) cbStatut.valueProperty().addListener((o, ov, v) -> {
            if (v != null && !v.isBlank()) setSuccess(lblStatutError, "✓ OK"); else setError(lblStatutError, "❌ Obligatoire.");
        });
        if (cbTypeReclamation != null) cbTypeReclamation.valueProperty().addListener((o, ov, v) -> {
            if (v != null && !v.isBlank()) setSuccess(lblTypeReclamationError, "✓ OK"); else setError(lblTypeReclamationError, "❌ Obligatoire.");
        });
        if (txtDescriptionReclamation != null) txtDescriptionReclamation.textProperty().addListener((o, ov, v) -> {
            String tx = v.trim();
            if (tx.isEmpty()) { if (lblDescriptionReclamationError != null) lblDescriptionReclamationError.setText(""); txtDescriptionReclamation.setStyle(""); return; }
            if (tx.length() < 10) { setError(lblDescriptionReclamationError, "❌ Min 10 caractères."); txtDescriptionReclamation.setStyle(""); return; }
            List<String> badWords = badWordService.findBadWords(tx);
            if (!badWords.isEmpty()) {
                setError(lblDescriptionReclamationError, "⚠️ Mots inappropriés : " + String.join(", ", badWords));
                txtDescriptionReclamation.setStyle("-fx-border-color:#DC2626;-fx-border-width:2;-fx-background-color:#FFF5F5;");
            } else {
                setSuccess(lblDescriptionReclamationError, "✓ " + tx.length() + "/500");
                txtDescriptionReclamation.setStyle("");
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers UI
    // ══════════════════════════════════════════════════════════════════════════

    private Label buildCompactBadge(String text, String color) {
        Label b = new Label(text.toUpperCase());
        b.setStyle("-fx-background-color:" + color + "18;-fx-text-fill:" + color + ";" +
            "-fx-font-size:8px;-fx-font-weight:700;-fx-padding:2 7 2 7;" +
            "-fx-background-radius:999;-fx-border-color:" + color + "44;" +
            "-fx-border-radius:999;-fx-border-width:1;");
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

    private void setError(Label l, String msg)   { if (l != null) { l.setText(msg); l.setStyle("-fx-text-fill:#DC2626;-fx-font-size:10px;-fx-font-weight:bold;"); } }
    private void setSuccess(Label l, String msg) { if (l != null) { l.setText(msg); l.setStyle("-fx-text-fill:#059669;-fx-font-size:10px;-fx-font-weight:bold;"); } }

    private void clearErrorLabels() {
        for (Label l : new Label[]{lblCategorieError, lblDateError, lblMontantError, lblTypeError, lblStatutError, lblDescriptionError})
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
    private String nvl(String s, String def)   { return (s != null && !s.isBlank()) ? s : def; }
    private String truncate(String s, int max) { if (s == null) return ""; return s.length() <= max ? s : s.substring(0, max) + "…"; }
}
