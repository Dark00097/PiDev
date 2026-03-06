package com.nexora.bank.controllers;

import com.nexora.bank.Models.Reclamation;
import com.nexora.bank.Models.Transaction;
import com.nexora.bank.Service.ReclamationService;
import com.nexora.bank.Service.SentimentService;
import com.nexora.bank.Service.SentimentService.SentimentResult;
import com.nexora.bank.Service.TransactionService;
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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

public class ReclamationController implements Initializable {

    // ── Services ───────────────────────────────────────────────────────────────
    private final ReclamationService reclamationService = new ReclamationService();
    private final TransactionService transactionService = new TransactionService();
    private final SentimentService   sentimentService   = new SentimentService();

    // Cache sentiment : évite de rappeler l'IA à chaque re-rendu de cellule
    private final ConcurrentHashMap<Integer, SentimentResult> sentimentCache =
        new ConcurrentHashMap<>();

    // ── KPI Cards ─────────────────────────────────────────────────────────────
    @FXML private Label lblNombreReclamations;
    @FXML private Label lblEnAttente;
    @FXML private Label lblResolues;
    @FXML private Label lblInappropriees;
    @FXML private Label lblEnCours;
    @FXML private Label lblRejetees;

    // ── Formulaire ─────────────────────────────────────────────────────────────
    @FXML private ComboBox<String> cmbTransaction;
    @FXML private DatePicker       dpDateReclamation;
    @FXML private ComboBox<String> cmbTypeReclamation;
    @FXML private TextArea         txtDescription;
    @FXML private ComboBox<String> cmbStatut;
    @FXML private Label            lblTransactionError;
    @FXML private Label            lblDateError;
    @FXML private Label            lblTypeError;
    @FXML private Label            lblDescriptionError;
    @FXML private Label            lblStatutError;
    @FXML private Button           btnAjouter;
    @FXML private Button           btnSupprimer;

    // ── Table ──────────────────────────────────────────────────────────────────
    @FXML private TableView<Reclamation>           tableReclamations;
    @FXML private TableColumn<Reclamation, String> colId;
    @FXML private TableColumn<Reclamation, String> colTransaction;
    @FXML private TableColumn<Reclamation, String> colDate;
    @FXML private TableColumn<Reclamation, String> colType;
    @FXML private TableColumn<Reclamation, String> colDescription;
    @FXML private TableColumn<Reclamation, String> colStatut;
    @FXML private TableColumn<Reclamation, String> colActions;
    @FXML private Label                             lblTableInfo;

    // ── Recherche & filtre ─────────────────────────────────────────────────────
    @FXML private TextField txtRecherche;
    @FXML private CheckBox  chkFiltreInappropriees;

    // ── État interne ───────────────────────────────────────────────────────────
    private final ObservableList<Reclamation> reclamationsList = FXCollections.observableArrayList();
    private FilteredList<Reclamation>         filteredData;
    private Reclamation                       selectedReclamation = null;
    private boolean                           isEditMode          = false;
    private List<Transaction>                 allTransactions;

    // ══════════════════════════════════════════════════════════════════════════
    // Initialisation
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupSearch();
        loadTransactions();
        refreshData();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Table — avec badges sentiment + bouton 💬
    // ══════════════════════════════════════════════════════════════════════════

    private void setupTable() {
        if (tableReclamations == null) return;

        // Largeur colonne Actions : suffisante pour 2 badges + 4 boutons
        if (colActions != null) colActions.setPrefWidth(360);

        if (colId != null) colId.setCellValueFactory(
            c -> new SimpleStringProperty(String.valueOf(c.getValue().getIdReclamation())));
        if (colTransaction != null) colTransaction.setCellValueFactory(
            c -> new SimpleStringProperty("#" + c.getValue().getIdTransaction()));
        if (colDate != null) colDate.setCellValueFactory(
            c -> new SimpleStringProperty(nvl(c.getValue().getDateReclamation(), "—")));
        if (colType != null) colType.setCellValueFactory(
            c -> new SimpleStringProperty(
                nvl(c.getValue().getTypeReclamation(), "—").replace("_", " ")));
        if (colDescription != null) colDescription.setCellValueFactory(
            c -> new SimpleStringProperty(truncate(c.getValue().getDescription(), 45)));

        if (colStatut != null) {
            colStatut.setCellValueFactory(
                c -> new SimpleStringProperty(nvl(c.getValue().getStatus(), "—")));
            colStatut.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); return; }
                    String color = switch (item) {
                        case "Résolue"    -> "#22c55e";
                        case "En cours"   -> "#4f46e5";
                        case "En attente" -> "#ea580c";
                        case "Rejetée"    -> "#64748b";
                        case "Signalée"   -> "#dc2626";
                        default           -> "#94a3b8";
                    };
                    Label badge = new Label(item.toUpperCase());
                    badge.setStyle(
                        "-fx-background-color:" + color + "22;-fx-text-fill:" + color + ";" +
                        "-fx-font-size:9px;-fx-font-weight:700;-fx-padding:2 8 2 8;" +
                        "-fx-background-radius:20;-fx-border-color:" + color + "55;" +
                        "-fx-border-radius:20;-fx-border-width:1;");
                    setGraphic(badge); setText(null);
                }
            });
        }

        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {

                // ── Boutons (créés une seule fois par cellule) ─────────────────
                private final Button btnEdit = buildSmallBtn("✏ Modifier", "#0d9488", "#ccfbf1");
                private final Button btnDel  = buildSmallBtn("🗑 Suppr.",  "#dc2626", "#fee2e2");
                private final Button btnBlur = buildSmallBtn("👁 Masquer", "#4f46e5", "#e0e7ff");
                private final Button btnIA   = buildSmallBtn("💬 Réponse", "#7c3aed", "#ede9fe");

                // ── Badges sentiment ───────────────────────────────────────────
                private final Label badgeSentiment = new Label("⏳");
                private final Label badgeUrgence   = new Label();

                // ── Layout : badges ligne 1, boutons ligne 2 ───────────────────
                private final HBox rowBadges  = new HBox(4, badgeSentiment, badgeUrgence);
                private final HBox rowButtons = new HBox(4, btnEdit, btnDel, btnBlur, btnIA);
                private final VBox cellRoot   = new VBox(5, rowBadges, rowButtons);

                {
                    rowBadges.setAlignment(Pos.CENTER_LEFT);
                    rowButtons.setAlignment(Pos.CENTER_LEFT);
                    cellRoot.setPadding(new Insets(4, 2, 4, 2));

                    String ph = "-fx-background-color:#F1F5F9;-fx-text-fill:#94A3B8;" +
                                "-fx-font-size:9px;-fx-font-weight:700;" +
                                "-fx-padding:2 7 2 7;-fx-background-radius:20;";
                    badgeSentiment.setStyle(ph);
                    badgeUrgence.setStyle(ph);

                    // ── CORRECTION CLÉ : on stocke la Reclamation dans userData
                    //    du VBox root, mis à jour dans updateItem().
                    //    getIndex() est évité car il est instable pendant le rendu.
                    btnEdit.setOnAction(e -> {
                        Reclamation r = (Reclamation) cellRoot.getUserData();
                        if (r != null) editReclamation(r);
                    });
                    btnDel.setOnAction(e -> {
                        Reclamation r = (Reclamation) cellRoot.getUserData();
                        if (r != null) confirmDelete(r);
                    });
                    btnBlur.setOnAction(e -> {
                        Reclamation r = (Reclamation) cellRoot.getUserData();
                        if (r == null) return;
                        boolean nowBlurred = !r.isBlurred();
                        reclamationService.toggleBlur(r.getIdReclamation(), nowBlurred);
                        showInfo("Contenu", nowBlurred ? "Description masquée ✅" : "Description révélée ✅");
                        sentimentCache.remove(r.getIdReclamation());
                        refreshData();
                    });
                    btnIA.setOnAction(e -> {
                        Reclamation r = (Reclamation) cellRoot.getUserData();
                        if (r != null) afficherReponseIA(r);
                    });
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    // Cellule vide ou index invalide → rien n'afficher
                    if (empty || getIndex() < 0
                              || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }

                    Reclamation r = getTableView().getItems().get(getIndex());

                    // Stocker la référence pour les handlers (évite getIndex() dans lambda)
                    cellRoot.setUserData(r);

                    setText(null);
                    setGraphic(cellRoot);

                    // Adapter le libellé du bouton blur
                    btnBlur.setText(r.isBlurred() ? "👁 Révéler" : "👁 Masquer");

                    // ── Badges sentiment : cache d'abord ───────────────────────
                    SentimentResult cached = sentimentCache.get(r.getIdReclamation());
                    if (cached != null) {
                        applySentimentBadges(cached, badgeSentiment, badgeUrgence);
                        return;
                    }

                    // Reset placeholder
                    String ph = "-fx-background-color:#F1F5F9;-fx-text-fill:#94A3B8;" +
                                "-fx-font-size:9px;-fx-font-weight:700;" +
                                "-fx-padding:2 7 2 7;-fx-background-radius:20;";
                    badgeSentiment.setText("⏳");
                    badgeSentiment.setStyle(ph);
                    badgeUrgence.setText("");

                    // Nettoyer description (enlever le préfixe [Priorité : ...])
                    String desc = nvl(r.getDescription(), "");
                    if (desc.startsWith("[Priorité")) {
                        int idx = desc.indexOf("] ");
                        if (idx >= 0) desc = desc.substring(idx + 2);
                    }
                    final String descFinal = desc;
                    final int    rid       = r.getIdReclamation();

                    // Appel IA en arrière-plan
                    new Thread(() -> {
                        SentimentResult result = sentimentService.analyser(descFinal);
                        sentimentCache.put(rid, result);
                        Platform.runLater(() ->
                            applySentimentBadges(result, badgeSentiment, badgeUrgence));
                    }).start();
                }
            });
        }

        tableReclamations.getSelectionModel().selectedItemProperty()
            .addListener((obs, o, r) -> { if (r != null) editReclamation(r); });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Badges sentiment
    // ══════════════════════════════════════════════════════════════════════════

    private void applySentimentBadges(SentimentResult s,
                                      Label badgeSentiment,
                                      Label badgeUrgence) {
        String color = s.getColor();
        badgeSentiment.setText(s.getEmoji() + " " + s.sentiment.toUpperCase());
        badgeSentiment.setStyle(
            "-fx-background-color:" + color + "22;-fx-text-fill:" + color + ";" +
            "-fx-font-size:9px;-fx-font-weight:700;-fx-padding:2 7 2 7;" +
            "-fx-background-radius:20;-fx-border-color:" + color + "55;" +
            "-fx-border-radius:20;-fx-border-width:1;");
        Tooltip t1 = new Tooltip(
            "Émotion : " + s.emotionDominante + "\n" +
            "Score    : " + s.score + "/100\n" +
            "Réponse  : " + s.tempsReponseSuggere);
        t1.setStyle("-fx-background-color:#0A2540;-fx-text-fill:#F8FAFC;" +
                    "-fx-font-size:10px;-fx-background-radius:6;");
        Tooltip.install(badgeSentiment, t1);

        String urgColor = s.getUrgenceColor();
        String urgEmoji = switch (s.urgence.toLowerCase()) {
            case "critique" -> "🔴";
            case "haute"    -> "🟠";
            case "moyenne"  -> "🟡";
            default         -> "🟢";
        };
        badgeUrgence.setText(urgEmoji + " " + s.urgence.toUpperCase());
        badgeUrgence.setStyle(
            "-fx-background-color:" + urgColor + "22;-fx-text-fill:" + urgColor + ";" +
            "-fx-font-size:9px;-fx-font-weight:700;-fx-padding:2 7 2 7;" +
            "-fx-background-radius:20;-fx-border-color:" + urgColor + "55;" +
            "-fx-border-radius:20;-fx-border-width:1;");
        Tooltip t2 = new Tooltip(
            "Priorité : " + s.prioriteSuggeree + "\n" +
            "Délai    : " + s.tempsReponseSuggere);
        t2.setStyle("-fx-background-color:#0A2540;-fx-text-fill:#F8FAFC;" +
                    "-fx-font-size:10px;-fx-background-radius:6;");
        Tooltip.install(badgeUrgence, t2);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Dialog réponse IA — bouton 💬
    // ══════════════════════════════════════════════════════════════════════════

    private void afficherReponseIA(Reclamation r) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("💬 Réponse IA — Réclamation #" + r.getIdReclamation());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(530);

        Label loading = new Label("⏳ Analyse IA en cours...");
        loading.setStyle("-fx-font-size:13px;-fx-text-fill:#94A3B8;-fx-font-style:italic;");
        dialog.getDialogPane().setContent(loading);
        dialog.show();

        String desc = nvl(r.getDescription(), "");
        if (desc.startsWith("[Priorité")) {
            int idx = desc.indexOf("] ");
            if (idx >= 0) desc = desc.substring(idx + 2);
        }
        final String descFinal = desc;

        SentimentResult cached = sentimentCache.get(r.getIdReclamation());
        if (cached != null) {
            remplirDialogIA(dialog, cached);
        } else {
            new Thread(() -> {
                SentimentResult result = sentimentService.analyser(descFinal);
                sentimentCache.put(r.getIdReclamation(), result);
                Platform.runLater(() -> remplirDialogIA(dialog, result));
            }).start();
        }
    }

    private void remplirDialogIA(Dialog<Void> dialog, SentimentResult s) {
        // En-tête coloré
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 14, 10, 14));
        header.setStyle(
            "-fx-background-color:" + s.getColor() + "18;" +
            "-fx-background-radius:10;-fx-border-color:" + s.getColor() + "44;" +
            "-fx-border-radius:10;-fx-border-width:1;");

        Label emojiLbl = new Label(s.getEmoji());
        emojiLbl.setStyle("-fx-font-size:28px;");

        VBox sentInfos = new VBox(3);
        Label sentLbl  = new Label(s.sentiment.toUpperCase());
        sentLbl.setStyle("-fx-font-size:15px;-fx-font-weight:700;-fx-text-fill:" + s.getColor() + ";");
        Label scoreLbl = new Label("Score : " + s.score + "/100  •  Émotion : " + s.emotionDominante);
        scoreLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#64748B;");
        sentInfos.getChildren().addAll(sentLbl, scoreLbl);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox urgBox = new VBox(3);
        urgBox.setAlignment(Pos.CENTER_RIGHT);
        Label urgLbl  = new Label(s.urgence.toUpperCase());
        urgLbl.setStyle("-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:" + s.getUrgenceColor() + ";");
        Label timeLbl = new Label("⏱ " + s.tempsReponseSuggere);
        timeLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#64748B;");
        urgBox.getChildren().addAll(urgLbl, timeLbl);

        header.getChildren().addAll(emojiLbl, sentInfos, spacer, urgBox);

        VBox content = new VBox(12);
        content.setPadding(new Insets(6, 0, 0, 0));

        if (s.resumeProfessionnel != null && !s.resumeProfessionnel.isBlank()) {
            Label t = new Label("📋 Résumé professionnel");
            t.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:#0A2540;");
            Label txt = new Label(s.resumeProfessionnel);
            txt.setWrapText(true);
            txt.setStyle("-fx-font-size:11px;-fx-text-fill:#475569;" +
                "-fx-background-color:#F8FAFC;-fx-background-radius:8;-fx-padding:8 10 8 10;");
            content.getChildren().addAll(t, txt);
        }

        if (s.reponseSuggeree != null && !s.reponseSuggeree.isBlank()) {
            Label t = new Label("✍️ Réponse suggérée à envoyer au client");
            t.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:#0A2540;");
            TextArea ta = new TextArea(s.reponseSuggeree);
            ta.setWrapText(true);
            ta.setEditable(false);
            ta.setPrefRowCount(4);
            ta.setStyle("-fx-font-size:11px;-fx-background-color:#F0FDF4;" +
                "-fx-border-color:#A7F3D0;-fx-border-radius:8;-fx-background-radius:8;");
            content.getChildren().addAll(t, ta);
        }

        Label meta = new Label("📌 Priorité : " + s.prioriteSuggeree +
            "   •   ⏱ Délai : " + s.tempsReponseSuggere);
        meta.setStyle("-fx-font-size:10px;-fx-text-fill:#94A3B8;-fx-font-style:italic;");

        VBox wrapper = new VBox(12, header, content, meta);
        wrapper.setPadding(new Insets(4));
        wrapper.setMaxWidth(500);
        dialog.getDialogPane().setContent(wrapper);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Dashboard analyse
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void ouvrirAnalyse() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("📊 Analyse des Réclamations");
        popup.setMinWidth(860);
        popup.setMinHeight(620);

        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color:#F8FAFC;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label("📊 Tableau de bord — Analyse des Réclamations");
        titleLbl.setStyle("-fx-font-size:18px;-fx-font-weight:700;-fx-text-fill:#1e293b;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button btnClose = new Button("✕ Fermer");
        String baseClose  = "-fx-background-color:#FEE2E2;-fx-text-fill:#DC2626;-fx-font-weight:700;" +
                            "-fx-background-radius:8;-fx-border-color:#FECACA;-fx-border-radius:8;" +
                            "-fx-border-width:1;-fx-cursor:hand;-fx-padding:6 14;";
        String hoverClose = "-fx-background-color:#DC2626;-fx-text-fill:#FFFFFF;-fx-font-weight:700;" +
                            "-fx-background-radius:8;-fx-border-color:transparent;-fx-border-radius:8;" +
                            "-fx-border-width:1;-fx-cursor:hand;-fx-padding:6 14;";
        btnClose.setStyle(baseClose);
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(hoverClose));
        btnClose.setOnMouseExited(e  -> btnClose.setStyle(baseClose));
        btnClose.setOnAction(e -> popup.close());
        header.getChildren().addAll(titleLbl, sp, btnClose);

        Label loading = new Label("⏳ Chargement des analyses...");
        loading.setStyle("-fx-font-size:13px;-fx-text-fill:#94a3b8;-fx-font-style:italic;");

        HBox kpiRow    = new HBox(16);
        HBox kpiRow2   = new HBox(16);
        HBox chartsRow = new HBox(16);
        chartsRow.setMinHeight(200);

        root.getChildren().addAll(header, loading, kpiRow, kpiRow2, chartsRow);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#F8FAFC;-fx-background:#F8FAFC;");
        popup.setScene(new Scene(scroll, 900, 650));
        popup.show();

        new Thread(() -> {
            int    total     = reclamationService.countTotal();
            int    enAttente = reclamationService.countByStatut("En attente");
            int    resolues  = reclamationService.countByStatut("Résolue");
            int    enCours   = reclamationService.countByStatut("En cours");
            int    rejetees  = reclamationService.countByStatut("Rejetée");
            long   signalees = reclamationService.countInappropriate();
            double tauxRes   = reclamationService.getTauxResolution();
            double tempsMoy  = reclamationService.getTempsMoyenResolutionJours();
            Map<String, Integer> parStatut = reclamationService.countParStatut();
            Map<String, Integer> parType   = reclamationService.countParType();

            Platform.runLater(() -> {
                loading.setVisible(false); loading.setManaged(false);

                kpiRow.setSpacing(16);
                kpiRow.getChildren().addAll(
                    buildKpiCard("📋 Total",      String.valueOf(total),    "#4f46e5", "#EEF2FF"),
                    buildKpiCard("🕐 En attente", String.valueOf(enAttente),"#D97706", "#FEF3C7"),
                    buildKpiCard("✅ Résolues",    String.valueOf(resolues), "#059669", "#D1FAE5"),
                    buildKpiCard("⚠️ Signalées",  String.valueOf(signalees),"#DC2626", "#FEE2E2"));

                kpiRow2.setSpacing(16);
                kpiRow2.getChildren().addAll(
                    buildKpiCard("🔄 En cours",       String.valueOf(enCours), "#4f46e5","#E0E7FF"),
                    buildKpiCard("❌ Rejetées",        String.valueOf(rejetees),"#64748B","#F1F5F9"),
                    buildKpiCard("📈 Taux résolution", String.format("%.1f%%",tauxRes),  "#16a34a","#F0FDF4"),
                    buildKpiCard("⏱ Temps moyen",     String.format("%.1f j",tempsMoy),  "#2563eb","#EFF6FF"));

                chartsRow.setSpacing(16);
                chartsRow.getChildren().addAll(
                    buildBarChart(parStatut),
                    buildDonutChart(tauxRes, total, resolues),
                    buildTypeChart(parType));
            });
        }).start();
    }

    private VBox buildKpiCard(String label, String value, String color, String bg) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:12;" +
            "-fx-border-color:" + color + "33;-fx-border-radius:12;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,2);");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label v = new Label(value); v.setStyle("-fx-font-size:26px;-fx-font-weight:700;-fx-text-fill:" + color + ";");
        Label n = new Label(label); n.setStyle("-fx-font-size:11px;-fx-text-fill:#64748B;-fx-font-weight:600;");
        card.getChildren().addAll(v, n);
        return card;
    }

    private VBox buildBarChart(Map<String, Integer> parStatut) {
        VBox box = new VBox(10); box.setPadding(new Insets(16)); box.setPrefWidth(260); box.setStyle(cardStyle());
        Label title = new Label("📊 Réclamations par statut");
        title.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#1e293b;");
        box.getChildren().add(title);
        int max = parStatut.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        String[] colors = {"#ea580c","#4f46e5","#22c55e","#64748b","#dc2626"};
        int i = 0;
        for (Map.Entry<String, Integer> entry : parStatut.entrySet()) {
            String color = colors[i % colors.length];
            double pct   = max > 0 ? (entry.getValue() * 1.0 / max) : 0;
            Label lbl = new Label(entry.getKey()); lbl.setStyle("-fx-font-size:10px;-fx-text-fill:#475569;");
            HBox barBg = new HBox(); barBg.setMaxWidth(Double.MAX_VALUE); barBg.setPrefHeight(10);
            barBg.setStyle("-fx-background-color:#e2e8f0;-fx-background-radius:5;");
            Region bar = new Region(); bar.setPrefHeight(10); bar.setPrefWidth(pct * 180);
            bar.setStyle("-fx-background-color:" + color + ";-fx-background-radius:5;");
            barBg.getChildren().add(bar);
            Label cnt = new Label(String.valueOf(entry.getValue())); cnt.setMinWidth(20);
            cnt.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:" + color + ";");
            HBox row = new HBox(8, barBg, cnt); row.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(barBg, Priority.ALWAYS);
            box.getChildren().addAll(lbl, row); i++;
        }
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox buildDonutChart(double tauxResolution, int total, int resolues) {
        double radius = 55.0;
        double angle  = (tauxResolution / 100.0) * 360.0;
        String color  = tauxResolution >= 70 ? "#22c55e" : tauxResolution >= 40 ? "#ea580c" : "#dc2626";
        Circle bgC = new Circle(radius); bgC.setStyle("-fx-fill:#e2e8f0;");
        Arc arc = new Arc(0, 0, radius, radius, 90, -angle); arc.setType(ArcType.ROUND);
        arc.setStyle("-fx-fill:" + color + ";-fx-stroke:transparent;");
        Circle hole = new Circle(radius * 0.65); hole.setStyle("-fx-fill:#ffffff;");
        Label pct = new Label(String.format("%.0f%%", tauxResolution));
        pct.setStyle("-fx-font-size:18px;-fx-font-weight:700;-fx-text-fill:" + color + ";");
        StackPane donut = new StackPane(bgC, arc, hole, pct);
        donut.setPrefSize(radius * 2 + 16, radius * 2 + 16);
        Label t = new Label("🎯 Taux de résolution"); t.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#1e293b;");
        Label d = new Label(resolues + " résolues / " + total + " total"); d.setStyle("-fx-font-size:10px;-fx-text-fill:#64748b;");
        VBox w = new VBox(10, t, donut, d); w.setAlignment(Pos.CENTER); w.setPadding(new Insets(16));
        w.setPrefWidth(220); w.setStyle(cardStyle()); HBox.setHgrow(w, Priority.ALWAYS);
        return w;
    }

    private VBox buildTypeChart(Map<String, Integer> parType) {
        VBox box = new VBox(10); box.setPadding(new Insets(16)); box.setPrefWidth(260); box.setStyle(cardStyle());
        Label title = new Label("📋 Par type de réclamation");
        title.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#1e293b;");
        box.getChildren().add(title);
        if (parType.isEmpty()) {
            Label empty = new Label("Aucune donnée"); empty.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11px;-fx-font-style:italic;");
            box.getChildren().add(empty); HBox.setHgrow(box, Priority.ALWAYS); return box;
        }
        int max = parType.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        String[] clrs = {"#4f46e5","#0d9488","#ea580c","#dc2626","#64748b"};
        int i = 0;
        for (Map.Entry<String, Integer> entry : parType.entrySet()) {
            String color = clrs[i % clrs.length];
            double pct   = entry.getValue() * 1.0 / max;
            Label lbl = new Label(entry.getKey().replace("_", " ")); lbl.setStyle("-fx-font-size:10px;-fx-text-fill:#475569;");
            lbl.setMaxWidth(200); lbl.setWrapText(true);
            Region bar = new Region(); bar.setPrefHeight(8); bar.setPrefWidth(pct * 180);
            bar.setStyle("-fx-background-color:" + color + ";-fx-background-radius:4;");
            HBox barBg = new HBox(bar); barBg.setStyle("-fx-background-color:#e2e8f0;-fx-background-radius:4;");
            barBg.setPrefHeight(8); barBg.setMaxWidth(Double.MAX_VALUE);
            Label cnt = new Label(String.valueOf(entry.getValue()));
            cnt.setStyle("-fx-font-size:10px;-fx-font-weight:700;-fx-text-fill:" + color + ";");
            HBox row = new HBox(8, barBg, cnt); row.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(barBg, Priority.ALWAYS);
            box.getChildren().addAll(lbl, row); i++;
        }
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private String cardStyle() {
        return "-fx-background-color:#ffffff;-fx-background-radius:12;" +
               "-fx-border-color:#e2e8f0;-fx-border-radius:12;" +
               "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,2);";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Bouton helper
    // ══════════════════════════════════════════════════════════════════════════

    private Button buildSmallBtn(String label, String color, String bg) {
        Button btn = new Button(label);
        String base  = "-fx-background-color:" + bg + ";-fx-text-fill:" + color + ";" +
                       "-fx-font-size:10px;-fx-padding:3 7;-fx-background-radius:6;" +
                       "-fx-cursor:hand;-fx-border-color:" + color + "55;" +
                       "-fx-border-radius:6;-fx-border-width:1;";
        String hover = "-fx-background-color:" + color + ";-fx-text-fill:#ffffff;" +
                       "-fx-font-size:10px;-fx-padding:3 7;-fx-background-radius:6;-fx-cursor:hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Données
    // ══════════════════════════════════════════════════════════════════════════

    private void refreshData() {
        sentimentCache.clear();
        reclamationsList.setAll(reclamationService.getAll());
        filteredData = new FilteredList<>(reclamationsList, r -> true);
        applyFilters();
        if (tableReclamations != null) tableReclamations.setItems(filteredData);
        updateKpiLabels();
        updateTableInfo();
    }

    private void updateKpiLabels() {
        new Thread(() -> {
            int  total     = reclamationService.countTotal();
            int  enAttente = reclamationService.countByStatut("En attente");
            int  resolues  = reclamationService.countByStatut("Résolue");
            int  enCours   = reclamationService.countByStatut("En cours");
            int  rejetees  = reclamationService.countByStatut("Rejetée");
            long signalees = reclamationService.countInappropriate();
            Platform.runLater(() -> {
                setText(lblNombreReclamations, String.valueOf(total));
                setText(lblEnAttente,          String.valueOf(enAttente));
                setText(lblResolues,           String.valueOf(resolues));
                setText(lblInappropriees,      String.valueOf(signalees));
                setText(lblEnCours,            String.valueOf(enCours));
                setText(lblRejetees,           String.valueOf(rejetees));
            });
        }).start();
    }

    private void loadTransactions() {
        allTransactions = transactionService.getAll();
        if (cmbTransaction == null) return;
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Transaction t : allTransactions)
            items.add("#" + t.getIdTransaction() + " — " +
                      nvl(t.getCategorie(), "?") + " — " +
                      String.format("%.2f DT", t.getMontant() != null ? t.getMontant() : 0.0));
        cmbTransaction.setItems(items);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Recherche & Filtre
    // ══════════════════════════════════════════════════════════════════════════

    private void setupSearch() {
        if (txtRecherche != null)
            txtRecherche.textProperty().addListener((obs, o, v) -> applyFilters());
        if (chkFiltreInappropriees != null)
            chkFiltreInappropriees.selectedProperty().addListener((obs, o, v) -> applyFilters());
    }

    private void applyFilters() {
        if (filteredData == null) return;
        String  q             = txtRecherche != null ? txtRecherche.getText().toLowerCase() : "";
        boolean onlySignalees = chkFiltreInappropriees != null && chkFiltreInappropriees.isSelected();
        filteredData.setPredicate(r -> {
            if (onlySignalees && !r.isInappropriate()) return false;
            if (q.isBlank()) return true;
            return nvl(r.getTypeReclamation(), "").toLowerCase().contains(q)
                || nvl(r.getStatus(),          "").toLowerCase().contains(q)
                || nvl(r.getDescription(),     "").toLowerCase().contains(q)
                || String.valueOf(r.getIdTransaction()).contains(q);
        });
        updateTableInfo();
    }

    private void updateTableInfo() {
        if (lblTableInfo != null && filteredData != null)
            lblTableInfo.setText("Total : " + filteredData.size() + " réclamation(s)");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CRUD
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleAjouter() {
        if (!validateForm()) return;
        int    idTransaction = getSelectedTransactionId();
        String date          = dpDateReclamation.getValue().toString();
        String type          = cmbTypeReclamation.getValue();
        String desc          = txtDescription.getText().trim();
        String statut        = cmbStatut.getValue();

        if (isEditMode && selectedReclamation != null) {
            reclamationService.edit(new Reclamation(
                selectedReclamation.getIdReclamation(),
                selectedReclamation.getIdUser(),
                idTransaction, date, type, desc, statut));
            showInfo("Succès", "✅ Réclamation modifiée !");
        } else {
            reclamationService.add(new Reclamation(1, idTransaction, date, type, desc, statut));
            showInfo("Succès", "✅ Réclamation ajoutée !");
        }
        clearForm();
        refreshData();
    }

    @FXML
    private void handleSupprimer() {
        if (selectedReclamation == null) { showInfo("Info", "Sélectionnez une réclamation."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer la réclamation #" + selectedReclamation.getIdReclamation() + " ?",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                reclamationService.remove(selectedReclamation);
                clearForm(); refreshData();
                showInfo("Supprimé", "Réclamation supprimée !");
            }
        });
    }

    @FXML private void handleAnnuler() { clearForm(); }

    private void editReclamation(Reclamation r) {
        selectedReclamation = r;
        isEditMode          = true;
        if (cmbTypeReclamation != null) cmbTypeReclamation.setValue(r.getTypeReclamation());
        if (cmbStatut          != null) cmbStatut.setValue(r.getStatus());
        if (txtDescription     != null) txtDescription.setText(r.getDescription());
        if (dpDateReclamation  != null && r.getDateReclamation() != null) {
            try { dpDateReclamation.setValue(LocalDate.parse(r.getDateReclamation())); }
            catch (Exception ignored) {}
        }
        if (cmbTransaction != null && allTransactions != null) {
            for (int i = 0; i < allTransactions.size(); i++) {
                if (allTransactions.get(i).getIdTransaction() == r.getIdTransaction()) {
                    cmbTransaction.getSelectionModel().select(i); break;
                }
            }
        }
        if (btnAjouter != null) btnAjouter.setText("Modifier");
    }

    private void confirmDelete(Reclamation r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer réclamation #" + r.getIdReclamation() + " ?",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) { reclamationService.remove(r); refreshData(); }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Export PDF
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void exporterPDF() {
        if (reclamationsList.isEmpty()) { showInfo("Export", "Aucune réclamation à exporter !"); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(new Stage());
        if (file == null) return;
        try {
            Document doc = new Document(new PdfDocument(new PdfWriter(file.getAbsolutePath())));
            doc.add(new Paragraph("Rapport Réclamations — Dashboard Admin").setBold().setFontSize(16));
            doc.add(new Paragraph("Total : " + reclamationService.countTotal() +
                "  |  Taux résolution : " + String.format("%.1f%%", reclamationService.getTauxResolution()) +
                "  |  Temps moyen : " + String.format("%.1f jours", reclamationService.getTempsMoyenResolutionJours()))
                .setFontSize(11));
            doc.add(new Paragraph(" "));
            Table tbl = new Table(new float[]{50, 80, 85, 130, 200, 90});
            for (String h : new String[]{"ID","Transaction","Date","Type","Description","Statut"})
                tbl.addHeaderCell(new Cell().add(new Paragraph(h).setBold()));
            for (Reclamation r : filteredData) {
                tbl.addCell(String.valueOf(r.getIdReclamation()));
                tbl.addCell("#" + r.getIdTransaction());
                tbl.addCell(nvl(r.getDateReclamation(), ""));
                tbl.addCell(nvl(r.getTypeReclamation(), "").replace("_", " "));
                tbl.addCell(truncate(nvl(r.getDescription(), ""), 60));
                tbl.addCell(nvl(r.getStatus(), ""));
            }
            doc.add(tbl); doc.close();
            showInfo("Export", "✅ PDF exporté !");
        } catch (FileNotFoundException ex) {
            showInfo("Erreur", "❌ Impossible de créer le PDF !");
        }
    }

    @FXML private void envoyerSMS() { showInfo("SMS", "Fonctionnalité SMS — bientôt disponible !"); }

    // ══════════════════════════════════════════════════════════════════════════
    // Validation
    // ══════════════════════════════════════════════════════════════════════════

    private boolean validateForm() {
        boolean valid = true; clearErrors();
        if (cmbTransaction     == null || cmbTransaction.getValue()     == null) { setError(lblTransactionError,  "❌ Transaction obligatoire."); valid = false; }
        if (dpDateReclamation  == null || dpDateReclamation.getValue()  == null) { setError(lblDateError,         "❌ Date obligatoire.");        valid = false; }
        if (cmbTypeReclamation == null || cmbTypeReclamation.getValue() == null) { setError(lblTypeError,         "❌ Type obligatoire.");        valid = false; }
        String desc = txtDescription != null ? txtDescription.getText().trim() : "";
        if (desc.isEmpty())         { setError(lblDescriptionError, "❌ Description obligatoire."); valid = false; }
        else if (desc.length() < 5) { setError(lblDescriptionError, "❌ Min 5 caractères.");       valid = false; }
        if (cmbStatut == null || cmbStatut.getValue() == null) { setError(lblStatutError, "❌ Statut obligatoire."); valid = false; }
        return valid;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private int getSelectedTransactionId() {
        int idx = cmbTransaction != null ? cmbTransaction.getSelectionModel().getSelectedIndex() : -1;
        if (idx >= 0 && allTransactions != null && idx < allTransactions.size())
            return allTransactions.get(idx).getIdTransaction();
        return -1;
    }

    private void clearForm() {
        if (cmbTransaction     != null) cmbTransaction.getSelectionModel().clearSelection();
        if (dpDateReclamation  != null) dpDateReclamation.setValue(null);
        if (cmbTypeReclamation != null) cmbTypeReclamation.getSelectionModel().clearSelection();
        if (txtDescription     != null) txtDescription.clear();
        if (cmbStatut          != null) cmbStatut.getSelectionModel().clearSelection();
        selectedReclamation = null; isEditMode = false;
        if (btnAjouter != null) btnAjouter.setText("Ajouter");
        clearErrors();
    }

    private void clearErrors() {
        for (Label l : new Label[]{lblTransactionError, lblDateError,
                                   lblTypeError, lblDescriptionError, lblStatutError})
            if (l != null) l.setText("");
    }

    private void setError(Label l, String msg) {
        if (l != null) { l.setText(msg); l.setStyle("-fx-text-fill:#dc2626;-fx-font-size:10px;-fx-font-weight:bold;"); }
    }

    private void setText(Label l, String text) { if (l != null) l.setText(text); }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.show();
    }

    private String nvl(String s, String def) { return (s != null && !s.isBlank()) ? s : def; }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}