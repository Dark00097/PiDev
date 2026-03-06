package com.nexora.bank.controllers;

import com.google.gson.*;
import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.Transaction;
import com.nexora.bank.Service.AIAnalysisService;
import com.nexora.bank.Service.TransactionService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AIPredictionController {

    private final TransactionService transactionService = new TransactionService();
    private final AIAnalysisService  aiService          = new AIAnalysisService();

    private static final String C_BG     = "#0f172a";
    private static final String C_CARD   = "#1e293b";
    private static final String C_BORDER = "#334155";
    private static final String C_PURPLE = "#8b5cf6";
    private static final String C_BLUE   = "#3b82f6";
    private static final String C_GREEN  = "#22c55e";
    private static final String C_ORANGE = "#f97316";
    private static final String C_RED    = "#ef4444";
    private static final String C_TEXT   = "#f1f5f9";
    private static final String C_MUTED  = "#94a3b8";

    // ══════════════════════════════════════════════════════════════════════════
    // Ouvrir la fenêtre
    // ══════════════════════════════════════════════════════════════════════════

    public void afficher(Stage parentStage) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parentStage);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("🔮 Analyse IA — Prédictions & Conseils");
        stage.setMinWidth(900);
        stage.setMinHeight(700);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:" + C_BG + ";");

        HBox    header       = buildHeader(stage);
        VBox    loadingPane  = buildLoadingPane();

        ScrollPane resultsScroll = new ScrollPane();
        resultsScroll.setFitToWidth(true);
        resultsScroll.setStyle(
            "-fx-background-color:" + C_BG + ";-fx-background:" + C_BG + ";");
        resultsScroll.setVisible(false);
        resultsScroll.setManaged(false);

        VBox resultsPane = new VBox(16);
        resultsPane.setPadding(new Insets(20));
        resultsPane.setStyle("-fx-background-color:" + C_BG + ";");
        resultsScroll.setContent(resultsPane);

        root.getChildren().addAll(header, loadingPane, resultsScroll);
        VBox.setVgrow(loadingPane,   Priority.ALWAYS);
        VBox.setVgrow(resultsScroll, Priority.ALWAYS);

        stage.setScene(new Scene(root, 920, 720));
        stage.show();

        lancerAnalyse(loadingPane, resultsScroll, resultsPane);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Lancer l'analyse en background
    // ══════════════════════════════════════════════════════════════════════════

    private void lancerAnalyse(VBox loadingPane, ScrollPane resultsScroll, VBox resultsPane) {
        new Thread(() -> {
            try {
                int userId = AuthSession.getCurrentUser().getIdUser();
                List<Transaction> transactions = transactionService.getAll().stream()
                    .filter(t -> t.getIdUser() == userId)
                    .sorted(Comparator.comparing(
                        Transaction::getDateTransaction,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(10)
                    .collect(Collectors.toList());

                if (transactions.isEmpty()) {
                    Platform.runLater(() -> showError(loadingPane,
                        "Aucune transaction trouvée pour votre compte."));
                    return;
                }

                // Construire JSON avec Gson
                JsonArray arr = new JsonArray();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (Transaction t : transactions) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("categorie",
                        t.getCategorie() != null ? t.getCategorie() : "Autre");
                    obj.addProperty("montant", t.getMontant());
                    obj.addProperty("type",
                        t.getTypeTransaction() != null ? t.getTypeTransaction() : "Debit");
                    obj.addProperty("date",
                        t.getDateTransaction() != null
                            ? t.getDateTransaction().format(fmt) : "N/A");
                    arr.add(obj);
                }

                String jsonResponse = aiService.analyserDepensesEtConseils(arr.toString());
                JsonObject data = JsonParser.parseString(jsonResponse).getAsJsonObject();

                Platform.runLater(() -> {
                    buildResultsUI(resultsPane, data, transactions);
                    loadingPane.setVisible(false);
                    loadingPane.setManaged(false);
                    resultsScroll.setVisible(true);
                    resultsScroll.setManaged(true);
                    FadeTransition ft =
                        new FadeTransition(Duration.millis(400), resultsScroll);
                    ft.setFromValue(0); ft.setToValue(1); ft.play();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                    showError(loadingPane, "Erreur IA : " + e.getMessage()));
            }
        }).start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UI Résultats
    // ══════════════════════════════════════════════════════════════════════════

    private void buildResultsUI(VBox pane, JsonObject data, List<Transaction> transactions) {
        pane.getChildren().clear();

        double totalPredit  = getDouble(data, "totalPredit", 0);
        int    scoreEpargne = getInt(data, "scoreEpargne", 50);
        String resume       = getStr(data, "resume", "Analyse de vos dépenses.");

        // ── Stats row ─────────────────────────────────────────────────────────
        HBox statsRow = new HBox(12);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.getChildren().addAll(
            buildStatCard("💰 Total prédit",
                String.format("%.0f DT", totalPredit), C_PURPLE, "mois prochain"),
            buildStatCard("⭐ Score épargne",
                scoreEpargne + "/100",
                scoreEpargne >= 70 ? C_GREEN : scoreEpargne >= 40 ? C_ORANGE : C_RED, ""),
            buildStatCard("📊 Transactions analysées",
                transactions.size() + " au total", C_BLUE, "historique récent")
        );

        // ── Résumé ────────────────────────────────────────────────────────────
        Label lblResume = new Label("💡 " + resume);
        lblResume.setWrapText(true);
        lblResume.setStyle(
            "-fx-text-fill:#cbd5e1;-fx-font-size:12px;-fx-font-style:italic;" +
            "-fx-background-color:#1e293b;-fx-padding:12 16 12 16;" +
            "-fx-background-radius:10;-fx-border-color:#334155;" +
            "-fx-border-radius:10;-fx-border-width:1;");

        // ── Graphique ─────────────────────────────────────────────────────────
        Label lblChart = buildSectionTitle("🔮 Prédictions par catégorie — Mois prochain");
        JsonArray predictions = data.has("predictions")
            ? data.getAsJsonArray("predictions") : new JsonArray();
        Canvas chart = buildBarChart(predictions);

        // ── Score jauge ───────────────────────────────────────────────────────
        Label lblScore = buildSectionTitle("⭐ Score d'épargne");
        HBox  gauge    = buildScoreGauge(scoreEpargne);

        // ── Conseils ──────────────────────────────────────────────────────────
        Label lblConseils  = buildSectionTitle("💡 Conseils personnalisés de l'IA");
        VBox  conseilsBox  = new VBox(10);
        JsonArray conseils = data.has("conseils")
            ? data.getAsJsonArray("conseils") : new JsonArray();
        for (JsonElement el : conseils)
            conseilsBox.getChildren().add(buildConseilCard(el.getAsJsonObject()));

        pane.getChildren().addAll(
            statsRow, lblResume,
            lblChart, chart,
            lblScore, gauge,
            lblConseils, conseilsBox
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Graphique barres animé
    // ══════════════════════════════════════════════════════════════════════════

    private Canvas buildBarChart(JsonArray predictions) {
        double width  = 860;
        double height = 280;
        Canvas canvas = new Canvas(width, height);
        canvas.setStyle("-fx-background-radius:12;");
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fond
        gc.setFill(Color.web(C_CARD));
        gc.fillRoundRect(0, 0, width, height, 16, 16);

        if (predictions.size() == 0) return canvas;

        int    nb     = predictions.size();
        double maxVal = 0;
        for (JsonElement el : predictions)
            maxVal = Math.max(maxVal,
                el.getAsJsonObject().get("montantPredit").getAsDouble());

        double chartH  = height - 70;
        double padLeft = 60;
        double barW    = Math.min(70, (width - padLeft - 40) / nb - 16);
        String[] colors = {C_PURPLE, C_BLUE, C_GREEN, C_ORANGE,
                           "#ec4899", "#14b8a6", "#f59e0b"};

        // Grille
        gc.setStroke(Color.web(C_BORDER));
        gc.setLineWidth(0.5);
        for (int g = 1; g <= 4; g++) {
            double y = 20 + (chartH - 20) * g / 4.0;
            gc.strokeLine(padLeft, y, width - 20, y);
            gc.setFill(Color.web(C_MUTED));
            gc.setFont(Font.font("System", 9));
            gc.fillText(String.format("%.0f", maxVal * (4 - g) / 4.0), 4, y + 3);
        }

        // Barres avec animation
        int idx = 0;
        for (JsonElement el : predictions) {
            JsonObject p      = el.getAsJsonObject();
            double val        = p.get("montantPredit").getAsDouble();
            String cat        = getStr(p, "categorie", "?");
            String tendance   = getStr(p, "tendance", "stable");
            String clr        = colors[idx % colors.length];
            String symb       = tendance.contains("hausse") ? " ↑"
                              : tendance.contains("baisse") ? " ↓" : " →";

            double targetH = maxVal > 0 ? (val / maxVal) * (chartH - 30) : 0;
            double x       = padLeft + idx * ((width - padLeft - 40) / nb) + 8;

            // Fermer les variables pour le lambda
            final double fx = x, fbw = barW, fval = val, ftH = targetH;
            final String fclr = clr, fcat = cat + symb;
            final int    fidx = idx;

            javafx.beans.property.SimpleDoubleProperty prop =
                new javafx.beans.property.SimpleDoubleProperty(0);
            prop.addListener((obs, o, n) -> {
                double h = n.doubleValue();
                double y = 20 + (chartH - 20) - h;

                // Effacer zone barre
                gc.setFill(Color.web(C_CARD));
                gc.fillRect(fx - 4, 18, fbw + 16, chartH + 10);

                // Redessiner grille locale
                gc.setStroke(Color.web(C_BORDER));
                gc.setLineWidth(0.5);
                for (int g = 1; g <= 4; g++) {
                    double gy = 20 + (chartH - 20) * g / 4.0;
                    gc.strokeLine(fx - 4, gy, fx + fbw + 12, gy);
                }

                // Barre
                gc.setFill(Color.web(fclr + "dd"));
                gc.fillRoundRect(fx, y, fbw, h, 8, 8);

                // Valeur
                if (h > 16) {
                    gc.setFill(Color.web(C_TEXT));
                    gc.setFont(Font.font("System", FontWeight.BOLD, 10));
                    gc.fillText(String.format("%.0f DT", fval), fx + 2, y - 5);
                }

                // Label catégorie
                gc.setFill(Color.web(C_MUTED));
                gc.setFont(Font.font("System", 9));
                String short_ = fcat.length() > 11 ? fcat.substring(0, 10) + "…" : fcat;
                gc.fillText(short_, fx, height - 10);
            });

            Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(prop, 0)),
                new KeyFrame(Duration.millis(900),
                    new KeyValue(prop, targetH, Interpolator.EASE_OUT))
            );
            PauseTransition delay = new PauseTransition(Duration.millis(idx * 130));
            delay.setOnFinished(e -> tl.play());
            delay.play();

            idx++;
        }
        return canvas;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Jauge score circulaire animée
    // ══════════════════════════════════════════════════════════════════════════

    private HBox buildScoreGauge(int score) {
        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(16));
        box.setStyle(
            "-fx-background-color:" + C_CARD + ";-fx-background-radius:12;" +
            "-fx-border-color:" + C_BORDER + ";-fx-border-radius:12;-fx-border-width:1;");

        Canvas gauge = new Canvas(130, 130);
        GraphicsContext gc = gauge.getGraphicsContext2D();
        String sc = score >= 70 ? C_GREEN : score >= 40 ? C_ORANGE : C_RED;

        // Animer de 0 → score
        Timeline tl = new Timeline();
        for (int i = 0; i <= score; i++) {
            final int fi = i;
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(i * 18L), e -> {
                gc.clearRect(0, 0, 130, 130);
                gc.setStroke(Color.web(C_BORDER));
                gc.setLineWidth(14);
                gc.strokeArc(12, 12, 106, 106, -225, 270,
                    javafx.scene.shape.ArcType.OPEN);
                gc.setStroke(Color.web(sc));
                gc.setLineWidth(14);
                double a = fi / 100.0 * 270;
                gc.strokeArc(12, 12, 106, 106, -225, -a,
                    javafx.scene.shape.ArcType.OPEN);
                gc.setFill(Color.web(C_TEXT));
                gc.setFont(Font.font("System", FontWeight.BOLD, 26));
                gc.fillText(fi + "", fi >= 100 ? 32 : fi >= 10 ? 42 : 53, 72);
                gc.setFill(Color.web(C_MUTED));
                gc.setFont(Font.font("System", 11));
                gc.fillText("/100", 40, 90);
            }));
        }
        tl.play();

        // Texte légende
        VBox legend = new VBox(8);
        String note = score >= 70 ? "🟢 Excellent épargnant !"
                    : score >= 40 ? "🟡 Potentiel d'amélioration"
                    : "🔴 Attention aux dépenses !";
        String desc = score >= 70 ? "Vous gérez très bien votre budget. Continuez !"
                    : score >= 40 ? "Quelques ajustements peuvent améliorer votre épargne."
                    : "Vos dépenses sont élevées. Suivez les conseils ci-dessous.";

        Label lblNote = new Label(note);
        lblNote.setStyle(
            "-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:" + sc + ";");
        Label lblDesc = new Label(desc);
        lblDesc.setWrapText(true); lblDesc.setMaxWidth(480);
        lblDesc.setStyle("-fx-font-size:11px;-fx-text-fill:" + C_MUTED + ";");

        // Barre colorée 0→40→70→100
        HBox barRow = new HBox(3);
        for (String c : new String[]{C_RED, C_ORANGE, C_GREEN}) {
            Region seg = new Region();
            seg.setPrefSize(76, 8);
            seg.setStyle("-fx-background-color:" + c + ";-fx-background-radius:4;");
            barRow.getChildren().add(seg);
        }
        HBox labelsRow = new HBox();
        for (String l : new String[]{"0", "40", "70", "100"}) {
            Label lbl = new Label(l);
            lbl.setPrefWidth(76);
            lbl.setStyle("-fx-font-size:9px;-fx-text-fill:" + C_MUTED + ";");
            labelsRow.getChildren().add(lbl);
        }

        legend.getChildren().addAll(lblNote, lblDesc, barRow, labelsRow);
        box.getChildren().addAll(gauge, legend);
        return box;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Carte conseil IA
    // ══════════════════════════════════════════════════════════════════════════

    private HBox buildConseilCard(JsonObject conseil) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setMaxWidth(Double.MAX_VALUE);

        String priorite = getStr(conseil, "priorite", "normale");
        String clr = switch (priorite.toLowerCase()) {
            case "haute", "critique" -> C_RED;
            case "moyenne"           -> C_ORANGE;
            default                  -> C_GREEN;
        };

        String baseStyle =
            "-fx-background-color:" + C_CARD + ";-fx-background-radius:12;" +
            "-fx-border-color:" + clr + "55;-fx-border-radius:12;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.2),8,0,0,2);";
        card.setStyle(baseStyle);

        // Emoji
        Label emoji = new Label(getStr(conseil, "emoji", "💡"));
        emoji.setStyle(
            "-fx-font-size:26px;-fx-background-color:" + clr + "22;" +
            "-fx-background-radius:12;-fx-padding:8 10 8 10;");

        // Texte
        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titre = new Label(getStr(conseil, "titre", "Conseil"));
        titre.setStyle(
            "-fx-font-size:13px;-fx-font-weight:700;-fx-text-fill:" + C_TEXT + ";");

        Label desc = new Label(getStr(conseil, "description", ""));
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size:11px;-fx-text-fill:" + C_MUTED + ";");

        content.getChildren().addAll(titre, desc);

        // Économie + badge
        double economie = conseil.has("economie")
            ? conseil.get("economie").getAsDouble() : 0;
        VBox econBox = new VBox(4);
        econBox.setAlignment(Pos.CENTER_RIGHT);

        Label lblEcon = new Label(String.format("💰 %.0f DT", economie));
        lblEcon.setStyle(
            "-fx-font-size:15px;-fx-font-weight:700;-fx-text-fill:" + C_GREEN + ";");
        Label lblSub = new Label("économisés/mois");
        lblSub.setStyle("-fx-font-size:9px;-fx-text-fill:" + C_MUTED + ";");
        Label badge = new Label(priorite.toUpperCase());
        badge.setStyle(
            "-fx-background-color:" + clr + "22;-fx-text-fill:" + clr + ";" +
            "-fx-font-size:8px;-fx-font-weight:700;-fx-padding:2 8 2 8;" +
            "-fx-background-radius:20;");

        econBox.getChildren().addAll(lblEcon, lblSub, badge);
        card.getChildren().addAll(emoji, content, econBox);

        // Hover
        String hoverStyle =
            "-fx-background-color:#263548;-fx-background-radius:12;" +
            "-fx-border-color:" + clr + ";-fx-border-radius:12;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian," + clr + "44,12,0,0,3);-fx-cursor:hand;";
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e  -> card.setStyle(baseStyle));

        return card;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers UI
    // ══════════════════════════════════════════════════════════════════════════

    private HBox buildHeader(Stage stage) {
        HBox h = new HBox();
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(16, 20, 16, 20));
        h.setStyle("-fx-background-color:#1e293b;" +
            "-fx-border-color:#334155;-fx-border-width:0 0 1 0;");

        VBox titles = new VBox(3);
        HBox.setHgrow(titles, Priority.ALWAYS);
        Label t = new Label("🔮 Analyse IA de vos finances");
        t.setStyle("-fx-font-size:18px;-fx-font-weight:700;-fx-text-fill:#f1f5f9;");
        Label s = new Label(
            "Prédictions & conseils personnalisés basés sur vos dernières transactions");
        s.setStyle("-fx-font-size:11px;-fx-text-fill:#64748b;");
        titles.getChildren().addAll(t, s);

        Button btnClose = new Button("✕ Fermer");
        btnClose.setStyle(
            "-fx-background-color:#dc2626;-fx-text-fill:#ffffff;" +
            "-fx-font-size:11px;-fx-padding:6 14 6 14;" +
            "-fx-background-radius:8;-fx-cursor:hand;");
        btnClose.setOnAction(e -> stage.close());

        h.getChildren().addAll(titles, btnClose);
        return h;
    }

    private VBox buildLoadingPane() {
        VBox pane = new VBox(16);
        pane.setAlignment(Pos.CENTER);
        pane.setStyle("-fx-background-color:" + C_BG + ";");

        Label spinner = new Label("🔮");
        spinner.setStyle("-fx-font-size:52px;");
        RotateTransition rt = new RotateTransition(Duration.seconds(2), spinner);
        rt.setByAngle(360); rt.setCycleCount(Animation.INDEFINITE); rt.play();

        Label lbl = new Label("L'IA analyse vos transactions...");
        lbl.setStyle(
            "-fx-font-size:16px;-fx-font-weight:700;-fx-text-fill:#94a3b8;");
        Label sub = new Label("Génération des prédictions, conseils et score en cours");
        sub.setStyle("-fx-font-size:12px;-fx-text-fill:#475569;");

        ProgressBar pb = new ProgressBar(-1);
        pb.setPrefWidth(300);
        pb.setStyle("-fx-accent:#8b5cf6;");

        pane.getChildren().addAll(spinner, lbl, sub, pb);
        return pane;
    }

    private VBox buildStatCard(String titre, String valeur, String color, String sub) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setPrefWidth(210);
        card.setStyle(
            "-fx-background-color:" + C_CARD + ";-fx-background-radius:12;" +
            "-fx-border-color:" + color + "55;-fx-border-radius:12;-fx-border-width:1;");

        Label lblT = new Label(titre);
        lblT.setStyle("-fx-font-size:10px;-fx-text-fill:" + C_MUTED + ";");
        Label lblV = new Label(valeur);
        lblV.setStyle(
            "-fx-font-size:20px;-fx-font-weight:700;-fx-text-fill:" + color + ";");

        card.getChildren().addAll(lblT, lblV);
        if (!sub.isEmpty()) {
            Label lblS = new Label(sub);
            lblS.setStyle("-fx-font-size:9px;-fx-text-fill:" + C_MUTED + ";");
            card.getChildren().add(lblS);
        }
        return card;
    }

    private Label buildSectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle(
            "-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:#e2e8f0;" +
            "-fx-padding:8 0 4 0;");
        return l;
    }

    private void showError(VBox pane, String msg) {
        pane.getChildren().clear();
        Label err = new Label("❌ " + msg);
        err.setStyle("-fx-font-size:13px;-fx-text-fill:#ef4444;" +
            "-fx-padding:20;");
        pane.setAlignment(Pos.CENTER);
        pane.getChildren().add(err);
    }

    // ── Gson helpers ──────────────────────────────────────────────────────────
    private String getStr(JsonObject o, String k, String def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def;
    }
    private double getDouble(JsonObject o, String k, double def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : def;
    }
    private int getInt(JsonObject o, String k, int def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : def;
    }
}