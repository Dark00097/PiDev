package com.nexora.bank.controllers;

import com.nexora.bank.Models.CompteBancaire;
import com.nexora.bank.Models.CoffreVirtuel;
import com.nexora.bank.Service.CoffreVirtuelService;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.List;

public class CompteCoffresDialog {

    // ══════════════════════════════════════════════
    //  PALETTE NEXORA
    // ══════════════════════════════════════════════
    private static final String NAVY    = "#0A2540";
    private static final String TEAL    = "#00B4A0";
    private static final String YELLOW  = "#F4C430";
    private static final String WHITE   = "#FFFFFF";
    private static final String GRAY_50 = "#F9FAFB";
    private static final String GRAY_100= "#F3F4F6";
    private static final String GRAY_150= "#EBEDF0";
    private static final String GRAY_200= "#E5E7EB";
    private static final String GRAY_300= "#D1D5DB";
    private static final String GRAY_400= "#9CA3AF";
    private static final String GRAY_500= "#6B7280";
    private static final String TEXT    = "#1F2937";
    private static final String SUCCESS = "#10B981";
    private static final String DANGER  = "#EF4444";
    private static final String WARNING = "#F59E0B";

    private final CompteBancaire      compte;
    private final CoffreVirtuelService coffreService;
    private Stage                     drawerStage;
    private TableView<CoffreVirtuel>  tableCoffres;
    private TextField                 searchField;

    public CompteCoffresDialog(CompteBancaire compte) {
        this.compte       = compte;
        this.coffreService = new CoffreVirtuelService();
    }

    // ══════════════════════════════════════════════
    //  POINT D'ENTRÉE
    // ══════════════════════════════════════════════
    public void show(Stage ownerStage) {

        drawerStage = new Stage();
        drawerStage.initStyle(StageStyle.TRANSPARENT);
        drawerStage.initModality(Modality.APPLICATION_MODAL);
        drawerStage.initOwner(ownerStage);
        drawerStage.setResizable(false);

        // Le drawer prend toute la largeur disponible à droite de la sidebar
        // On laisse 280px pour laisser voir la sidebar gauche
        double sidebarW = 280;
        double drawerW  = ownerStage.getWidth() - sidebarW;
        double fullH    = ownerStage.getHeight();

        VBox drawer = buildDrawer(drawerW, fullH);

        // Overlay sombre sur la sidebar (280px à gauche)
        Region overlay = new Region();
        overlay.setPrefWidth(sidebarW);
        overlay.setPrefHeight(fullH);
        overlay.setStyle("-fx-background-color: rgba(10,37,64,0.30);");
        overlay.setOnMouseClicked(e -> closeDrawer(drawer));

        HBox root = new HBox(overlay, drawer);
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root, ownerStage.getWidth(), fullH);
        scene.setFill(Color.TRANSPARENT);

        drawerStage.setScene(scene);
        drawerStage.setX(ownerStage.getX());
        drawerStage.setY(ownerStage.getY());
        drawerStage.show();

        // Animation slide-in depuis la droite
        drawer.setTranslateX(drawerW);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), drawer);
        slideIn.setToX(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeOverlay = new FadeTransition(Duration.millis(220), overlay);
        fadeOverlay.setFromValue(0);
        fadeOverlay.setToValue(1);

        slideIn.play();
        fadeOverlay.play();
    }

    public void show() {
        Stage primary = (Stage) Stage.getWindows().stream()
                .filter(w -> w instanceof Stage && w.isShowing())
                .findFirst().orElse(null);
        if (primary != null) show(primary);
    }

    private void closeDrawer(VBox drawer) {
        double w = drawer.getPrefWidth();
        TranslateTransition out = new TranslateTransition(Duration.millis(240), drawer);
        out.setToX(w);
        out.setInterpolator(Interpolator.EASE_IN);
        out.setOnFinished(e -> drawerStage.close());
        out.play();
    }

    // ══════════════════════════════════════════════
    //  DRAWER PRINCIPAL
    // ══════════════════════════════════════════════
    private VBox buildDrawer(double w, double h) {
        VBox drawer = new VBox(0);
        drawer.setPrefWidth(w);
        drawer.setMinWidth(w);
        drawer.setMaxWidth(w);
        drawer.setPrefHeight(h);
        drawer.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-effect: dropshadow(gaussian, rgba(10,37,64,0.25), 40, 0, -10, 0);"
        );

        VBox header  = buildHeader(drawer);
        HBox stats   = buildStats();
        VBox table   = buildTableSection(w);
        HBox footer  = buildFooter(drawer);

        VBox.setVgrow(table, Priority.ALWAYS);
        drawer.getChildren().addAll(header, stats, table, footer);
        return drawer;
    }

    // ══════════════════════════════════════════════
    //  HEADER  —  titre + infos compte + bouton ✕
    // ══════════════════════════════════════════════
    private VBox buildHeader(VBox drawer) {
        VBox header = new VBox(10);
        header.setPadding(new Insets(18, 24, 14, 24));
        header.setStyle(
                "-fx-background-color: " + GRAY_50 + ";" +
                        "-fx-border-color: transparent transparent " + GRAY_200 + " transparent;" +
                        "-fx-border-width: 0 0 1.5 0;"
        );

        // ── Ligne 1 : Icône + Titre + Bouton ✕ ───
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        SVGPath lockIcon = new SVGPath();
        lockIcon.setContent("M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z");
        lockIcon.setFill(Color.web(NAVY));
        lockIcon.setScaleX(1.1);
        lockIcon.setScaleY(1.1);

        Label titleLbl = new Label("Coffres Virtuels Associés");
        titleLbl.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + NAVY + ";" +
                        "-fx-font-family: 'Segoe UI', sans-serif;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── Bouton ✕ bien visible ─────────────────
        Button btnX = new Button("✕");
        String xBase =
                "-fx-background-color: " + DANGER + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-cursor: hand;" +
                        "-fx-min-width: 36px; -fx-min-height: 36px;" +
                        "-fx-max-width: 36px; -fx-max-height: 36px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(239,68,68,0.35), 10, 0, 0, 3);";
        String xHover =
                "-fx-background-color: #C81E1E;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-cursor: hand;" +
                        "-fx-min-width: 36px; -fx-min-height: 36px;" +
                        "-fx-max-width: 36px; -fx-max-height: 36px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(200,30,30,0.5), 14, 0, 0, 4);" +
                        "-fx-scale-x: 1.08; -fx-scale-y: 1.08;";
        btnX.setStyle(xBase);
        btnX.setOnMouseEntered(e -> btnX.setStyle(xHover));
        btnX.setOnMouseExited (e -> btnX.setStyle(xBase));
        btnX.setOnAction(e -> closeDrawer(drawer));

        topBar.getChildren().addAll(lockIcon, titleLbl, spacer, btnX);

        // ── Ligne 2 : infos compte ─────────────────
        HBox infoBar = new HBox(40);
        infoBar.setAlignment(Pos.CENTER_LEFT);
        infoBar.setPadding(new Insets(4, 0, 0, 0));

        infoBar.getChildren().addAll(
                infoCell("Numéro Compte", compte.getNumeroCompte()),
                infoCell("Type",          compte.getTypeCompte()),
                infoCell("Solde",         String.format("%.2f DT", compte.getSolde())),
                statutCell()
        );

        header.getChildren().addAll(topBar, infoBar);
        return header;
    }

    private VBox infoCell(String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: " + GRAY_500 + "; -fx-font-family: 'Segoe UI', sans-serif;");
        Label v = new Label(value != null ? value : "—");
        v.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + TEXT + "; -fx-font-family: 'Segoe UI', sans-serif;");
        return new VBox(2, l, v);
    }

    private VBox statutCell() {
        Label l = new Label("Statut");
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: " + GRAY_500 + "; -fx-font-family: 'Segoe UI', sans-serif;");
        Label v = new Label(compte.getStatutCompte());
        String color = "Actif".equalsIgnoreCase(compte.getStatutCompte()) ? TEAL : DANGER;
        v.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-font-family: 'Segoe UI', sans-serif;");
        return new VBox(2, l, v);
    }

    // ══════════════════════════════════════════════
    //  STATISTIQUES
    // ══════════════════════════════════════════════
    private HBox buildStats() {
        HBox bar = new HBox(14);
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.setAlignment(Pos.CENTER);
        bar.setStyle("-fx-background-color: " + GRAY_50 + ";");

        List<CoffreVirtuel> coffres = coffreService.getAll().stream()
                .filter(c -> c.getIdCompte() == compte.getIdCompte()).toList();

        double totalE = coffres.stream().mapToDouble(CoffreVirtuel::getMontantActuel).sum();
        double totalO = coffres.stream().mapToDouble(CoffreVirtuel::getObjectifMontant).sum();
        long   actifs = coffres.stream().filter(c -> "Actif".equalsIgnoreCase(c.getStatus())).count();
        double prog   = totalO > 0 ? totalE / totalO * 100 : 0;

        bar.getChildren().addAll(
                statCard("Total Coffres",  String.valueOf(coffres.size()),         TEAL),
                statCard("Total Épargné",  String.format("%.2f DT", totalE),      TEAL),
                statCard("Objectif Total", String.format("%.2f DT", totalO),      "#EC4899"),
                statCard("Coffres Actifs", String.valueOf(actifs),                 TEAL),
                statCard("Progression",    String.format("%.1f%%", prog),          TEAL)
        );
        return bar;
    }

    private VBox statCard(String title, String value, String color) {
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 10px; -fx-text-fill: " + GRAY_500 + "; -fx-font-family: 'Segoe UI', sans-serif;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-font-family: 'Segoe UI', sans-serif;");

        VBox card = new VBox(5, t, v);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + GRAY_200 + ";" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(10,37,64,0.06), 8, 0, 0, 2);"
        );
        HBox.setHgrow(card, Priority.ALWAYS);

        String base  = card.getStyle();
        String hover = "-fx-background-color:" + WHITE + ";-fx-background-radius:12;" +
                "-fx-border-color:" + color + ";-fx-border-radius:12;-fx-border-width:1.5;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,180,160,0.18),14,0,0,4);-fx-translate-y:-2;";
        card.setOnMouseEntered(e -> card.setStyle(hover));
        card.setOnMouseExited (e -> { card.setStyle(base); card.setTranslateY(0); });
        return card;
    }

    // ══════════════════════════════════════════════
    //  TABLEAU  —  pleine largeur, toutes colonnes
    // ══════════════════════════════════════════════
    private VBox buildTableSection(double drawerW) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(12, 24, 10, 24));
        section.setStyle("-fx-background-color: " + GRAY_50 + ";");
        VBox.setVgrow(section, Priority.ALWAYS);

        // ── Barre : titre | recherche centrée | vide ──
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(2, 0, 6, 0));

        Label tableTitle = new Label("📋  Liste Détaillée des Coffres");
        tableTitle.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + NAVY + ";" +
                        "-fx-font-family: 'Segoe UI', sans-serif;"
        );

        // Spacer gauche
        Region left = new Region();
        HBox.setHgrow(left, Priority.ALWAYS);

        // Champ de recherche centré
        searchField = new TextField();
        searchField.setPromptText("🔍  Rechercher un coffre...");
        searchField.setPrefWidth(260);
        searchField.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-border-color: " + GRAY_300 + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-padding: 8 14;" +
                        "-fx-font-size: 12px;" +
                        "-fx-text-fill: " + TEXT + ";" +
                        "-fx-prompt-text-fill: " + GRAY_400 + ";" +
                        "-fx-font-family: 'Segoe UI', sans-serif;"
        );
        searchField.focusedProperty().addListener((obs, old, focused) -> {
            String border = focused ? TEAL : GRAY_300;
            searchField.setStyle(
                    "-fx-background-color: " + WHITE + ";" +
                            "-fx-border-color: " + border + ";" +
                            "-fx-border-radius: 10; -fx-background-radius: 10;" +
                            "-fx-border-width: 1.5; -fx-padding: 8 14;" +
                            "-fx-font-size: 12px; -fx-text-fill: " + TEXT + ";" +
                            "-fx-prompt-text-fill: " + GRAY_400 + ";" +
                            "-fx-font-family: 'Segoe UI', sans-serif;"
            );
        });
        searchField.textProperty().addListener((obs, o, n) -> filterTable(n));

        // Spacer droit = même taille que le titre → recherche au milieu
        Region right = new Region();
        right.setPrefWidth(tableTitle.getPrefWidth());
        HBox.setHgrow(right, Priority.ALWAYS);

        bar.getChildren().addAll(tableTitle, left, searchField, right);

        // ── TableView pleine largeur ───────────────
        tableCoffres = new TableView<>();
        tableCoffres.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableCoffres.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-table-cell-border-color: " + GRAY_150 + ";" +
                        "-fx-control-inner-background: " + WHITE + ";" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );
        VBox.setVgrow(tableCoffres, Priority.ALWAYS);

        String cs = "-fx-alignment: CENTER; -fx-font-size: 12px;" +
                "-fx-font-family: 'Segoe UI', sans-serif; -fx-text-fill: " + TEXT + ";";

        // ── Colonnes ──────────────────────────────
        TableColumn<CoffreVirtuel, Integer> colId  = simpleCol("ID",             "idCoffre",  cs);
        TableColumn<CoffreVirtuel, String>  colNom = simpleCol("Nom du Coffre",  "nom",       cs);

        TableColumn<CoffreVirtuel, Double> colObj = new TableColumn<>("Objectif");
        colObj.setCellValueFactory(new PropertyValueFactory<>("objectifMontant"));
        colObj.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean e) {
                super.updateItem(v, e); setStyle(cs);
                setText(e || v == null ? null : String.format("%.2f DT", v));
            }
        });

        TableColumn<CoffreVirtuel, Double> colAct = new TableColumn<>("Montant Actuel");
        colAct.setCellValueFactory(new PropertyValueFactory<>("montantActuel"));
        colAct.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean e) {
                super.updateItem(v, e); setStyle(cs);
                setText(e || v == null ? null : String.format("%.2f DT", v));
            }
        });

        TableColumn<CoffreVirtuel, Void> colProg = new TableColumn<>("Progression");
        colProg.setCellFactory(c -> new TableCell<>() {
            private final ProgressBar pb  = new ProgressBar();
            private final Label       pct = new Label();
            private final VBox        box = new VBox(3, pb, pct);
            {
                pb.setPrefHeight(7);
                pb.setMaxWidth(Double.MAX_VALUE);
                pct.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
                box.setAlignment(Pos.CENTER);
            }
            @Override protected void updateItem(Void item, boolean e) {
                super.updateItem(item, e);
                if (e || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                CoffreVirtuel cv = getTableRow().getItem();
                double p = cv.getObjectifMontant() > 0 ? cv.getMontantActuel() / cv.getObjectifMontant() : 0;
                pb.setProgress(Math.min(p, 1.0));
                pct.setText(String.format("%.0f%%", p * 100));
                String col = p >= 1.0 ? SUCCESS : p >= 0.5 ? WARNING : DANGER;
                pb.setStyle("-fx-accent:" + col + ";-fx-control-inner-background:" + GRAY_100 + ";");
                pct.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + col + ";");
                setGraphic(box);
            }
        });

        TableColumn<CoffreVirtuel, String> colStat = new TableColumn<>("Statut");
        colStat.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStat.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean e) {
                super.updateItem(s, e);
                if (e || s == null) { setText(null); setStyle(cs); return; }
                setText(s);
                String bg = "Actif".equalsIgnoreCase(s) ? TEAL : DANGER;
                setStyle("-fx-background-color:" + bg + ";-fx-text-fill:white;" +
                        "-fx-background-radius:7;-fx-padding:5 10;-fx-font-weight:bold;" +
                        "-fx-font-size:11px;-fx-alignment:center;-fx-font-family:'Segoe UI',sans-serif;");
            }
        });

        TableColumn<CoffreVirtuel, String>  colDC  = simpleCol("Date Création", "dateCreation",  cs);
        TableColumn<CoffreVirtuel, String>  colDO  = simpleCol("Date Objectif", "dateObjectifs", cs);

        TableColumn<CoffreVirtuel, Boolean> colV = new TableColumn<>("🔒");
        colV.setCellValueFactory(new PropertyValueFactory<>("estVerrouille"));
        colV.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean e) {
                super.updateItem(v, e);
                setText(e || v == null ? null : v ? "🔒" : "🔓");
                setStyle("-fx-alignment:center;-fx-font-size:13px;");
            }
        });

        tableCoffres.getColumns().addAll(
                colId, colNom, colObj, colAct, colProg, colStat, colDC, colDO, colV
        );
        tableCoffres.setPlaceholder(new Label("💼  Aucun coffre virtuel lié à ce compte"));
        loadCoffres();

        section.getChildren().addAll(bar, tableCoffres);
        return section;
    }

    @SuppressWarnings("unchecked")
    private <T> TableColumn<CoffreVirtuel, T> simpleCol(String title, String prop, String style) {
        TableColumn<CoffreVirtuel, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setStyle(style);
        return col;
    }

    // ══════════════════════════════════════════════
    //  FOOTER
    // ══════════════════════════════════════════════
    private HBox buildFooter(VBox drawer) {
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(12, 24, 14, 24));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle(
                "-fx-background-color: " + WHITE + ";" +
                        "-fx-border-color: " + GRAY_200 + " transparent transparent transparent;" +
                        "-fx-border-width: 1.5 0 0 0;"
        );

        Button btnExport  = styledBtn("⬇  Exporter",
                YELLOW, NAVY, "#DBA800", NAVY);
        Button btnRefresh = styledBtn("🔄  Actualiser",
                TEAL, WHITE, "#00967F", WHITE);
        Button btnClose   = styledBtn("Fermer",
                GRAY_150, TEXT, GRAY_300, NAVY);

        btnExport.setOnAction(e  -> exportToPDF());
        btnRefresh.setOnAction(e -> { loadCoffres(); searchField.clear(); });
        btnClose.setOnAction(e   -> closeDrawer(drawer));

        footer.getChildren().addAll(btnExport, btnRefresh, btnClose);
        return footer;
    }

    private Button styledBtn(String label, String bg, String fg, String bgH, String fgH) {
        Button btn = new Button(label);
        String base =
                "-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                        "-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:8 20;" +
                        "-fx-background-radius:9;-fx-cursor:hand;-fx-font-family:'Segoe UI',sans-serif;";
        String hover =
                "-fx-background-color:" + bgH + ";-fx-text-fill:" + fgH + ";" +
                        "-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:8 20;" +
                        "-fx-background-radius:9;-fx-cursor:hand;-fx-font-family:'Segoe UI',sans-serif;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited (e -> btn.setStyle(base));
        return btn;
    }

    // ══════════════════════════════════════════════
    //  DONNÉES
    // ══════════════════════════════════════════════
    private void loadCoffres() {
        List<CoffreVirtuel> list = coffreService.getAll().stream()
                .filter(c -> c.getIdCompte() == compte.getIdCompte()).toList();
        tableCoffres.setItems(FXCollections.observableArrayList(list));
    }

    private void filterTable(String text) {
        if (text == null || text.isBlank()) { loadCoffres(); return; }
        String f = text.toLowerCase();
        tableCoffres.setItems(FXCollections.observableArrayList(
                coffreService.getAll().stream()
                        .filter(c -> c.getIdCompte() == compte.getIdCompte())
                        .filter(c -> c.getNom().toLowerCase().contains(f)
                                || c.getStatus().toLowerCase().contains(f))
                        .toList()
        ));
    }

    private void exportToPDF() {
        System.out.println("Export PDF...");
    }
}