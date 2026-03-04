package com.nexora.bank.controllers;

import com.nexora.bank.Service.RoueFortuneEligibilityService;
import com.nexora.bank.Service.RoueFortuneEligibilityService.EligibilityResult;
import com.nexora.bank.Service.RoueFortuneService;
import com.nexora.bank.Service.RoueFortuneService.BlockResult;
import com.nexora.bank.Service.RoueFortuneService.TirageResult;
import com.nexora.bank.Utils.MyDB;
import com.nexora.bank.Utils.SessionManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.sql.Connection;

public class RoueFortuneController {

    @FXML private StackPane    rouePaneContainer;
    @FXML private Canvas       rouCanvas;
    @FXML private Button       btnTourner;
    @FXML private Button       btnFermer;
    @FXML private Label        lblRoueStatut;
    @FXML private Label        lblRoueMessage;
    @FXML private Label        lblBadgeStatut;
    @FXML private HBox         cardCondSolde;
    @FXML private HBox         cardCondBudget;
    @FXML private HBox         cardCondEpargne;
    @FXML private Label        lblCondSolde;
    @FXML private Label        lblCondBudget;
    @FXML private Label        lblCondEpargne;
    @FXML private Label        iconCondSolde;
    @FXML private Label        iconCondBudget;
    @FXML private Label        iconCondEpargne;
    @FXML private VBox         panelBarre;
    @FXML private VBox         panelRefus;
    @FXML private VBox         panelBonus;
    @FXML private ProgressBar  progressPoints;
    @FXML private Label        lblPoints;
    @FXML private Label        lblProgressText;
    @FXML private Label        lblRefusRaison;
    @FXML private Label        lblRefusSuggestion;
    @FXML private VBox         panelSolutionSolde;
    @FXML private VBox         panelSolutionBudget;
    @FXML private VBox         panelSolutionEpargne;
    @FXML private Label        lblSolutionSolde;
    @FXML private Label        lblSolutionBudget;
    @FXML private Label        lblSolutionEpargne;
    @FXML private Button       btnReVerifier;

    private final RoueFortuneEligibilityService eligSvc = new RoueFortuneEligibilityService();
    private final RoueFortuneService            rouSvc  = new RoueFortuneService();

    private static final int NB_SEG = 12;
    private static final int[] PTS  = {5, 10, 20, 15, 8, 12, 3, 7, 25, 2, 18, 6};
    private static final Color[] COULEURS = {
            Color.web("#FF4136"), Color.web("#FF851B"), Color.web("#FFDC00"),
            Color.web("#2ECC40"), Color.web("#0074D9"), Color.web("#B10DC9"),
            Color.web("#FF6B9D"), Color.web("#01FF70"), Color.web("#F012BE"),
            Color.web("#3D9970"), Color.web("#e84393"), Color.web("#7FDBFF")
    };

    private double  angle    = 0.0;
    private boolean enTour   = false;
    private int     totalPts = 0;
    private static final int MAX_PTS = 100;

    private Runnable onPointsUpdated;
    public void setOnPointsUpdated(Runnable r) { this.onPointsUpdated = r; }

    // ══════════════════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ══════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        dessinerRoue(0);
        resetPanels();
        verifierEligibilite();
    }

    @FXML
    private void reVerifierEligibilite() {
        setVisible(panelRefus, false);
        setBadge("🔄  Re-vérification...", "#FBBF24", "rgba(251,191,36,0.12)");
        setStatut("🔍  Nouveau scan de ton profil financier...", "#9CA3AF");
        verifierEligibilite();
    }

    @FXML
    private void fermerFenetre() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        FadeTransition ft = new FadeTransition(Duration.millis(180), stage.getScene().getRoot());
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> stage.close());
        ft.play();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VÉRIFICATION ÉLIGIBILITÉ
    //  ✅ FIX 1 : null passé au service — getFreshConn() utilisé en interne
    //  ✅ FIX 2 : BlockResult avec 8 cas anti-triche
    // ══════════════════════════════════════════════════════════════════════════
    private void verifierEligibilite() {
        setBadge("⏳  Vérification...", "#FBBF24", "rgba(251,191,36,0.12)");
        setStatut("🔍  Analyse de ton profil financier ce mois-ci...", "#9CA3AF");
        setBouton(false, "🎡  Lancer la roue");

        int userId = SessionManager.getInstance().getCurrentUserId();

        new Thread(() -> {
            try {
                rouSvc.initTable(null);
                totalPts = rouSvc.getTotalPoints(userId, null);
                Platform.runLater(() -> afficherBarrePoints(0));

                BlockResult check = rouSvc.verifierAvecRaison(userId, null);
                if (check.bloque) {
                    Platform.runLater(() -> {
                        if (check.raison != null && check.raison.contains("Manipulation")) {
                            setBadge("🚫  Triche détectée", "#EF4444", "rgba(239,68,68,0.12)");
                        } else if (check.raison != null && check.raison.contains("Internet")) {
                            setBadge("🌐  Connexion requise", "#F97316", "rgba(249,115,22,0.12)");
                        } else if (check.raison != null && check.raison.contains("Retour")) {
                            setBadge("⛔  Retour dans le temps", "#EF4444", "rgba(239,68,68,0.12)");
                        } else if (check.raison != null && check.raison.contains("cours")) {
                            setBadge("⏳  Traitement en cours", "#FBBF24", "rgba(251,191,36,0.12)");
                        } else {
                            setBadge("🔒  Déjà joué", "#F97316", "rgba(249,115,22,0.12)");
                        }
                        setStatut(check.raison != null ? check.raison
                                : "⏰ Tu as déjà tourné la roue ce mois-ci.", "#F97316");
                        setBouton(false, "🔒  Accès refusé");
                        afficherBarrePoints(0);
                        afficherToastBlocage(check.raison);
                    });
                    return;
                }

                Connection connElig = MyDB.getInstance().getConn();
                EligibilityResult elig = eligSvc.checkEligibility(userId, connElig);

                Platform.runLater(() -> {
                    afficherConditions(elig);
                    if (elig.eligible) {
                        setBadge("✅  Éligible !", "#22C55E", "rgba(34,197,94,0.12)");
                        setStatut("✅  " + elig.conditionsOk + "/3 conditions validées — Tu peux jouer !", "#22C55E");
                        activerBouton();
                    } else {
                        setBadge("❌  Non éligible", "#EF4444", "rgba(239,68,68,0.12)");
                        setStatut("❌  " + elig.conditionsOk + "/3 conditions — Accès refusé ce mois", "#EF4444");
                        afficherRefus(elig);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setBadge("⚠️  Erreur", "#EF4444", "rgba(239,68,68,0.12)");
                    setStatut("⚠️  Erreur : " + e.getMessage(), "#EF4444");
                    activerBouton();
                });
            }
        }, "Roue-Eligibilite").start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LANCER LA ROUE
    // ══════════════════════════════════════════════════════════════════════════
    @FXML
    private void lancerRoue() {
        if (enTour) return;
        enTour = true;
        setBouton(false, "🌀  En rotation...");
        setStatut("🌀  La roue tourne... bonne chance !", "#A78BFA");

        int seg          = (int)(Math.random() * NB_SEG);
        int pointsGagnes = PTS[seg];
        double aps  = 360.0 / NB_SEG;
        double dest = 5 * 360.0 + (360.0 - (seg * aps + aps / 2.0)) + 270.0;

        Timeline tl = new Timeline();
        int NF = 120;
        for (int i = 0; i <= NF; i++) {
            double t     = (double) i / NF;
            double eased = 1.0 - Math.pow(1.0 - t, 3);
            double a     = angle + dest * eased;
            tl.getKeyFrames().add(new KeyFrame(Duration.seconds(4.0 * t),
                    ev -> dessinerRoue(a % 360)));
        }
        tl.setOnFinished(ev -> {
            angle  = dest % 360;
            enTour = false;
            enregistrerTirage(pointsGagnes);
        });
        tl.play();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ENREGISTRER TIRAGE + BONUS 50 DT
    //  ✅ FIX 1 : tournerRoue(null) — service gère sa connexion en interne
    //  ✅ FIX 2 : ancienTotal = res.totalPoints - res.pointsGagnes (valeur BDD)
    //  ✅ FIX 3 : popup choix de compte avant crédit
    // ══════════════════════════════════════════════════════════════════════════
    private void enregistrerTirage(int pointsGagnesVisuel) {
        int userId = SessionManager.getInstance().getCurrentUserId();

        new Thread(() -> {
            try {
                TirageResult res = rouSvc.tournerRoue(userId, null);

                if (res.bloque) {
                    Platform.runLater(() -> {
                        afficherToastBlocage(res.raisonBlocage);
                        setBouton(false, "🔒  Accès refusé");
                        enTour = false;
                    });
                    return;
                }

                totalPts = res.totalPoints;
                int ancienTotal  = res.totalPoints - res.pointsGagnes;
                boolean bonusSeuil = res.totalPoints >= MAX_PTS && ancienTotal < MAX_PTS;

                if (bonusSeuil) {
                    Platform.runLater(() -> {
                        afficherResultat(res, false);
                        afficherChoixCompte(userId);
                    });
                } else {
                    Platform.runLater(() -> afficherResultat(res, false));
                }

                if (onPointsUpdated != null) Platform.runLater(onPointsUpdated);

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatut("⚠️  Tirage enregistré mais erreur DB : " + e.getMessage(), "#EF4444");
                    setBouton(false, "🔒  Erreur");
                });
            }
        }, "Roue-Tirage").start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  POPUP CHOIX DE COMPTE
    //  ✅ FIX : statutCompte='Actif' (valeur réelle BDD confirmée dans le SQL)
    // ══════════════════════════════════════════════════════════════════════════
    private void afficherChoixCompte(int userId) {
        try {
            java.util.List<String>  labels = new java.util.ArrayList<>();
            java.util.List<Integer> ids    = new java.util.ArrayList<>();
            java.util.List<Double>  soldes = new java.util.ArrayList<>();

            try (Connection c = MyDB.getInstance().getConn();
                 java.sql.PreparedStatement ps = c.prepareStatement(
                         "SELECT idCompte, numeroCompte, solde, typeCompte FROM compte " +
                                 "WHERE idUser=? AND statutCompte='Actif' ORDER BY idCompte ASC")) {
                ps.setInt(1, userId);
                java.sql.ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    ids.add(rs.getInt("idCompte"));
                    soldes.add(rs.getDouble("solde"));
                    labels.add(String.format("💳 %s — %s — Solde : %.2f DT",
                            rs.getString("typeCompte"),
                            rs.getString("numeroCompte"),
                            rs.getDouble("solde")));
                }
            }

            if (ids.isEmpty()) {
                setStatut("⚠️ Aucun compte actif trouvé.", "#EF4444");
                return;
            }

            Stage popup = new Stage(StageStyle.TRANSPARENT);
            popup.initModality(Modality.APPLICATION_MODAL);

            Label titre    = new Label("🎁 Félicitations ! 100 pts atteints !");
            titre.setStyle("-fx-text-fill:#FFD700; -fx-font-size:16px; -fx-font-weight:bold;");
            Label sousTitre = new Label("Choisissez le compte pour recevoir vos 50 DT :");
            sousTitre.setStyle("-fx-text-fill:#CBD5E1; -fx-font-size:12px;");

            VBox comptesBox = new VBox(10);
            javafx.scene.control.ToggleGroup group = new javafx.scene.control.ToggleGroup();

            for (int i = 0; i < labels.size(); i++) {
                javafx.scene.control.RadioButton rb =
                        new javafx.scene.control.RadioButton(labels.get(i));
                rb.setToggleGroup(group);
                rb.setStyle("-fx-text-fill:#E2E8F0; -fx-font-size:12px;" +
                        "-fx-background-color:rgba(255,255,255,0.05);" +
                        "-fx-background-radius:10; -fx-padding:10 16;" +
                        "-fx-border-color:rgba(124,58,237,0.3); -fx-border-radius:10;");
                if (i == 0) rb.setSelected(true);
                rb.setOnMouseEntered(e -> rb.setStyle(
                        "-fx-text-fill:#E2E8F0; -fx-font-size:12px;" +
                                "-fx-background-color:rgba(124,58,237,0.15);" +
                                "-fx-background-radius:10; -fx-padding:10 16;" +
                                "-fx-border-color:#7C3AED; -fx-border-radius:10; -fx-cursor:hand;"));
                rb.setOnMouseExited(e -> rb.setStyle(
                        "-fx-text-fill:#E2E8F0; -fx-font-size:12px;" +
                                "-fx-background-color:rgba(255,255,255,0.05);" +
                                "-fx-background-radius:10; -fx-padding:10 16;" +
                                "-fx-border-color:rgba(124,58,237,0.3); -fx-border-radius:10;"));
                comptesBox.getChildren().add(rb);
            }

            Button btnConfirmer = new Button("✅  Confirmer et recevoir 50 DT");
            btnConfirmer.setStyle(
                    "-fx-background-color:linear-gradient(to right,#7C3AED,#4F46E5);" +
                            "-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:13px;" +
                            "-fx-background-radius:12; -fx-padding:12 28; -fx-cursor:hand;" +
                            "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.6),14,0.3,0,3);");

            btnConfirmer.setOnAction(e -> {
                int selectedIdx = 0;
                for (int i = 0; i < comptesBox.getChildren().size(); i++) {
                    javafx.scene.control.RadioButton rb =
                            (javafx.scene.control.RadioButton) comptesBox.getChildren().get(i);
                    if (rb.isSelected()) { selectedIdx = i; break; }
                }
                final int    compteChoisi = ids.get(selectedIdx);
                final double soldeChoisi  = soldes.get(selectedIdx);
                popup.close();

                new Thread(() -> {
                    boolean ok = crediter50DTSurCompte(userId, compteChoisi, soldeChoisi);
                    if (ok) {
                        rouSvc.resetPoints(userId, null);
                        totalPts = 0;
                        Platform.runLater(() -> {
                            if (panelBonus != null) {
                                panelBonus.setVisible(true); panelBonus.setManaged(true);
                                FadeTransition ft = new FadeTransition(Duration.millis(800), panelBonus);
                                ft.setFromValue(0); ft.setToValue(1); ft.play();
                            }
                            afficherBarrePoints(0);
                            if (onPointsUpdated != null) onPointsUpdated.run();
                        });
                    }
                }, "Roue-Credit").start();
            });

            VBox root = new VBox(18, titre, sousTitre, comptesBox, btnConfirmer);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setStyle("-fx-background-color:#0F172A; -fx-background-radius:20;" +
                    "-fx-border-color:#7C3AED; -fx-border-width:2; -fx-border-radius:20;" +
                    "-fx-padding:28 32;" +
                    "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.5),24,0.3,0,6);");

            Scene scene = new Scene(root, 420, 100 + labels.size() * 65);
            scene.setFill(null);
            popup.setScene(scene);

            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            popup.setX((screen.getWidth()  - 420) / 2);
            popup.setY((screen.getHeight() - scene.getHeight()) / 2);

            root.setOpacity(0);
            popup.show();
            FadeTransition ft = new FadeTransition(Duration.millis(300), root);
            ft.setToValue(1); ft.play();

        } catch (Exception e) {
            System.err.println("[Roue] Erreur popup: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CRÉDITER 50 DT — try-with-resources garantit connexion fraîche
    // ══════════════════════════════════════════════════════════════════════════
    private boolean crediter50DTSurCompte(int userId, int idCompte, double soldeActuel) {
        try (Connection conn = MyDB.getInstance().getConn()) {
            double nouveauSolde = soldeActuel + 50.0;
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "UPDATE compte SET solde=? WHERE idCompte=?")) {
                ps.setDouble(1, nouveauSolde); ps.setInt(2, idCompte); ps.executeUpdate();
            }
            String sqlTx = "INSERT INTO transactions " +
                    "(idCompte, idUser, categorie, dateTransaction, montant, typeTransaction," +
                    " statutTransaction, soldeApres, description) " +
                    "VALUES (?, ?, 'Recompense', ?, 50.00, 'CREDIT', 'VALIDED', ?, " +
                    "'🎁 Récompense Roue de la Fortune — 100 pts atteints')";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sqlTx)) {
                ps.setInt(1, idCompte); ps.setInt(2, userId);
                ps.setString(3, java.time.LocalDate.now().toString());
                ps.setDouble(4, nouveauSolde); ps.executeUpdate();
            }
            System.out.println("[Roue] ✅ +50 DT crédités sur compte idCompte=" + idCompte
                    + " | Nouveau solde: " + nouveauSolde);
            return true;
        } catch (Exception e) {
            System.err.println("[Roue] ❌ Erreur crédit 50 DT: " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TOAST DE BLOCAGE
    // ══════════════════════════════════════════════════════════════════════════
    private void afficherToastBlocage(String raison) {
        if (raison == null) return;
        try {
            Stage toast = new Stage(StageStyle.TRANSPARENT);
            toast.initModality(Modality.NONE);

            String emoji, borderColor;
            if      (raison.contains("Manipulation") || raison.contains("Retour")) { emoji = "🚫"; borderColor = "#EF4444"; }
            else if (raison.contains("Internet") || raison.contains("Connexion"))  { emoji = "🌐"; borderColor = "#F97316"; }
            else if (raison.contains("cours"))                                      { emoji = "⏳"; borderColor = "#FBBF24"; }
            else                                                                    { emoji = "🔒"; borderColor = "#F97316"; }

            String titre = raison.contains("Manipulation") ? "🚫 Triche détectée"
                    : raison.contains("Internet") ? "🌐 Connexion requise"
                    : raison.contains("Retour")   ? "⛔ Retour dans le temps"
                    : raison.contains("cours")    ? "⏳ Traitement en cours"
                    : "🔒 Accès refusé";

            Label iconLbl  = new Label(emoji);
            iconLbl.setStyle("-fx-font-size:20px;");
            Label titreLbl = new Label(titre);
            titreLbl.setStyle("-fx-text-fill:white; -fx-font-size:13px; -fx-font-weight:bold;");
            Label msgLbl   = new Label(raison.split("\n")[0].trim());
            msgLbl.setWrapText(true); msgLbl.setMaxWidth(260);
            msgLbl.setStyle("-fx-text-fill:#CBD5E1; -fx-font-size:11px;");

            Button btnVoir    = new Button("Voir");
            Button btnIgnorer = new Button("Ignorer");
            btnVoir.setStyle("-fx-background-color:" + borderColor + "; -fx-text-fill:white;" +
                    "-fx-font-size:11px; -fx-font-weight:bold;" +
                    "-fx-background-radius:6; -fx-padding:4 12; -fx-cursor:hand;");
            btnIgnorer.setStyle("-fx-background-color:rgba(255,255,255,0.08); -fx-text-fill:#94A3B8;" +
                    "-fx-font-size:11px; -fx-background-radius:6; -fx-padding:4 12; -fx-cursor:hand;");

            javafx.scene.layout.HBox btns = new javafx.scene.layout.HBox(8, btnIgnorer, btnVoir);
            btns.setAlignment(Pos.CENTER_RIGHT);
            javafx.scene.layout.VBox textBox = new javafx.scene.layout.VBox(3, titreLbl, msgLbl);
            javafx.scene.layout.HBox content = new javafx.scene.layout.HBox(12, iconLbl, textBox);
            content.setAlignment(Pos.CENTER_LEFT);
            javafx.scene.layout.HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10, content, btns);
            root.setStyle("-fx-background-color:#1E293B; -fx-background-radius:14;" +
                    "-fx-border-color:" + borderColor + "; -fx-border-width:1.5;" +
                    "-fx-border-radius:14; -fx-padding:14 18;" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),18,0,0,6);");

            Scene scene = new Scene(root, 330, 100);
            scene.setFill(null);
            toast.setScene(scene);

            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            toast.setX(screen.getMaxX() - 355);
            toast.setY(screen.getMaxY() - 120);

            btnIgnorer.setOnAction(e -> toast.close());
            btnVoir.setOnAction(e -> toast.close());
            root.setOpacity(0);
            toast.show();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), root);
            fadeIn.setToValue(1);
            fadeIn.setOnFinished(e -> {
                PauseTransition wait = new PauseTransition(Duration.seconds(4));
                wait.setOnFinished(ev -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(eOut -> toast.close());
                    fadeOut.play();
                });
                wait.play();
            });
            fadeIn.play();

        } catch (Exception e) {
            System.err.println("[Toast] Erreur: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  AFFICHER RÉSULTAT
    // ══════════════════════════════════════════════════════════════════════════
    private void afficherResultat(TirageResult res, boolean bonus50DT) {
        if (lblRoueMessage != null) {
            lblRoueMessage.setText(res.message);
            FadeTransition ft = new FadeTransition(Duration.millis(700), lblRoueMessage);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
        }
        setStatut("🎊  Félicitations ! Reviens le mois prochain !", "#22C55E");
        setBadge("✅  Tour terminé", "#22C55E", "rgba(34,197,94,0.12)");
        setBouton(false, "🔒  À la prochaine fois !");
        afficherBarrePoints(res.pointsGagnes);
        if (bonus50DT && panelBonus != null) {
            panelBonus.setVisible(true); panelBonus.setManaged(true);
            FadeTransition ftB = new FadeTransition(Duration.millis(800), panelBonus);
            ftB.setFromValue(0); ftB.setToValue(1); ftB.setDelay(Duration.millis(600)); ftB.play();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CONDITIONS
    // ══════════════════════════════════════════════════════════════════════════
    private void afficherConditions(EligibilityResult elig) {
        setCond(cardCondSolde,   lblCondSolde,   iconCondSolde,   "Solde toujours positif",   elig.conditionSolde);
        setCond(cardCondBudget,  lblCondBudget,  iconCondBudget,  "Budget mensuel respecté",  elig.conditionBudget);
        setCond(cardCondEpargne, lblCondEpargne, iconCondEpargne, "Épargne mensuelle active", elig.conditionEpargne);
        animCond(cardCondSolde, 0); animCond(cardCondBudget, 150); animCond(cardCondEpargne, 300);
    }

    private void setCond(HBox card, Label lbl, Label icon, String texte, boolean ok) {
        if (ok) {
            if (card != null) card.setStyle(card.getStyle()
                    .replace("rgba(255,255,255,0.03)","rgba(34,197,94,0.08)")
                    .replace("rgba(255,255,255,0.07)","rgba(34,197,94,0.25)"));
            if (lbl  != null) { lbl.setText("✅  " + texte); lbl.setStyle("-fx-text-fill:#22C55E; -fx-font-size:12px; -fx-font-weight:700;"); }
            if (icon != null) icon.setText("✅");
        } else {
            if (card != null) card.setStyle(card.getStyle()
                    .replace("rgba(255,255,255,0.03)","rgba(239,68,68,0.08)")
                    .replace("rgba(255,255,255,0.07)","rgba(239,68,68,0.25)"));
            if (lbl  != null) { lbl.setText("❌  " + texte); lbl.setStyle("-fx-text-fill:#EF4444; -fx-font-size:12px; -fx-font-weight:700;"); }
            if (icon != null) icon.setText("❌");
        }
    }

    private void animCond(HBox card, int delayMs) {
        if (card == null) return;
        card.setOpacity(0); card.setTranslateX(20);
        FadeTransition ft = new FadeTransition(Duration.millis(400), card);
        ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(delayMs));
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), card);
        tt.setFromX(20); tt.setToX(0); tt.setDelay(Duration.millis(delayMs));
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REFUS
    // ══════════════════════════════════════════════════════════════════════════
    private void afficherRefus(EligibilityResult elig) {
        if (panelRefus == null) return;
        panelRefus.setVisible(true); panelRefus.setManaged(true);
        if (lblRefusRaison != null)
            lblRefusRaison.setText(elig.message != null && !elig.message.isEmpty()
                    ? elig.message : buildRefusMessage(elig));
        afficherSolutionCond(panelSolutionSolde,   lblSolutionSolde,   elig.conditionSolde,   elig.solutionSolde);
        afficherSolutionCond(panelSolutionBudget,  lblSolutionBudget,  elig.conditionBudget,  elig.solutionBudget);
        afficherSolutionCond(panelSolutionEpargne, lblSolutionEpargne, elig.conditionEpargne, elig.solutionEpargne);
        if (lblRefusSuggestion != null)
            lblRefusSuggestion.setText("💡 " + (elig.suggestion != null && !elig.suggestion.isEmpty()
                    ? elig.suggestion : "Effectuez les actions ci-dessus puis cliquez sur Re-vérifier."));
        panelRefus.setOpacity(0); panelRefus.setTranslateY(15);
        FadeTransition ft = new FadeTransition(Duration.millis(500), panelRefus);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), panelRefus);
        tt.setFromY(15); tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    private void afficherSolutionCond(VBox panel, Label lbl, boolean condOk, String sol) {
        if (panel == null) return;
        if (condOk) { panel.setVisible(false); panel.setManaged(false); return; }
        panel.setVisible(true); panel.setManaged(true);
        if (lbl != null) lbl.setText(sol != null && !sol.isBlank() ? sol
                : "⚠️ Analyse indisponible. Vérifiez votre connexion et réessayez.");
    }

    private String buildRefusMessage(EligibilityResult elig) {
        StringBuilder sb = new StringBuilder();
        if (!elig.conditionSolde)   sb.append("❌ Solde négatif détecté ce mois. ");
        if (!elig.conditionBudget)  sb.append("❌ Budget global dépassé (dépenses > 80% des revenus). ");
        if (!elig.conditionEpargne) sb.append("❌ Aucun virement vers le Coffre Virtuel. ");
        return sb.toString().trim();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BARRE DE PROGRESSION
    // ══════════════════════════════════════════════════════════════════════════
    private void afficherBarrePoints(int gained) {
        if (panelBarre == null) return;
        panelBarre.setVisible(true); panelBarre.setManaged(true);
        double pct    = Math.min((double) totalPts / MAX_PTS, 1.0);
        int    pctInt = (int)(pct * 100);
        new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressPoints.progressProperty(), progressPoints.getProgress())),
                new KeyFrame(Duration.millis(1000), new KeyValue(progressPoints.progressProperty(), pct, Interpolator.EASE_OUT))
        ).play();
        String barColor = pctInt >= 100 ? "-fx-accent:#FFD700;"
                : pctInt >= 50 ? "-fx-accent:#22C55E;" : pctInt >= 25 ? "-fx-accent:#FBBF24;" : "-fx-accent:#A78BFA;";
        if (progressPoints != null) progressPoints.setStyle(barColor
                + "-fx-background-color:rgba(255,255,255,0.07);-fx-background-radius:7;-fx-border-radius:7;");
        if (lblPoints != null) lblPoints.setText(totalPts + " / " + MAX_PTS + " pts  —  " + pctInt + "%");
        if (lblProgressText != null) {
            int restant = MAX_PTS - totalPts;
            lblProgressText.setText(
                    totalPts >= MAX_PTS ? "🏆 OBJECTIF 100 PTS ATTEINT ! 50 DT viennent d'être crédités sur ton compte !"
                            : gained >= 20 ? "🔥 JACKPOT ! +" + gained + " pts ! Plus que " + restant + " pts pour décrocher 50 DT !"
                            : gained >= 10 ? "⭐ Super ! +" + gained + " pts. Encore " + restant + " pts pour le bonus 50 DT !"
                            : gained >  0  ? "👏 Bravo ! +" + gained + " pts. Plus que " + restant + " pts pour 50 DT !"
                            : totalPts == 0 ? "🎯 Commence à accumuler des points — 100 pts = 50 DT sur ton compte !"
                            : "💪 " + totalPts + " pts accumulés. Encore " + restant + " pts pour gagner 50 DT !");
        }
        if (totalPts >= MAX_PTS && panelBarre != null)
            panelBarre.setStyle("-fx-background-color:rgba(255,215,0,0.15);"
                    + "-fx-border-color:#FFD700;-fx-border-radius:14;-fx-background-radius:14;-fx-padding:16 18;");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DESSIN ROUE
    // ══════════════════════════════════════════════════════════════════════════
    private void dessinerRoue(double offset) {
        if (rouCanvas == null) return;
        GraphicsContext gc = rouCanvas.getGraphicsContext2D();
        double W = rouCanvas.getWidth(), H = rouCanvas.getHeight();
        double cx = W/2, cy = H/2, r = Math.min(W,H)/2-14, aps = 360.0/NB_SEG;
        gc.clearRect(0,0,W,H);
        gc.setFill(new RadialGradient(0,0,cx,cy,r+14,false,CycleMethod.NO_CYCLE,
                new Stop(0.82,Color.TRANSPARENT),new Stop(1.0,Color.color(0,0,0,0.5))));
        gc.fillOval(cx-r-14,cy-r-14,(r+14)*2,(r+14)*2);
        gc.setFill(Color.web("#1F1F2E")); gc.fillOval(cx-r-7,cy-r-7,(r+7)*2,(r+7)*2);
        gc.setFill(new RadialGradient(45,0.3,cx-r*0.3,cy-r*0.3,r+5,false,CycleMethod.NO_CYCLE,
                new Stop(0,Color.web("#FFE566")),new Stop(0.5,Color.web("#D4AF37")),new Stop(1,Color.web("#8B6914"))));
        gc.fillOval(cx-r-4,cy-r-4,(r+4)*2,(r+4)*2);
        for (int i=0;i<NB_SEG;i++) {
            double sa=offset+i*aps-aps/2;
            gc.setFill(COULEURS[i]); gc.fillArc(cx-r,cy-r,r*2,r*2,-sa,-aps,ArcType.ROUND);
            gc.setStroke(Color.color(0,0,0,0.35)); gc.setLineWidth(1.5);
            gc.strokeArc(cx-r,cy-r,r*2,r*2,-sa,-aps,ArcType.ROUND);
            double mid=Math.toRadians(sa+aps/2), tx=cx+r*0.68*Math.cos(mid), ty=cy+r*0.68*Math.sin(mid);
            gc.save(); gc.translate(tx,ty); gc.rotate(sa+aps/2+90);
            gc.setFill(Color.color(0,0,0,0.5)); gc.setFont(Font.font("Arial",FontWeight.BOLD,13));
            gc.setTextAlign(TextAlignment.CENTER); gc.fillText(String.valueOf(PTS[i]),1,1);
            gc.setFill(Color.WHITE); gc.fillText(String.valueOf(PTS[i]),0,0); gc.restore();
        }
        double rc=r*0.14;
        gc.setFill(Color.web("#1F1F2E")); gc.fillOval(cx-rc-3,cy-rc-3,(rc+3)*2,(rc+3)*2);
        gc.setFill(new RadialGradient(0,0,cx-rc*0.35,cy-rc*0.35,rc,false,CycleMethod.NO_CYCLE,
                new Stop(0,Color.web("#FFF0A0")),new Stop(0.5,Color.web("#D4AF37")),new Stop(1,Color.web("#9A7A1A"))));
        gc.fillOval(cx-rc,cy-rc,rc*2,rc*2);
        double fw=13,fh=22;
        gc.setFill(Color.web("#DC2626"));
        gc.fillPolygon(new double[]{cx,cx-fw/2,cx+fw/2},new double[]{cy-r-3,cy-r+fh,cy-r+fh},3);
        gc.setFill(Color.web("#FF6B6B"));
        gc.fillPolygon(new double[]{cx,cx-fw/3.5,cx},new double[]{cy-r-3,cy-r+fh*0.6,cy-r+fh*0.6},3);
        gc.setStroke(Color.web("#7F1D1D")); gc.setLineWidth(1);
        gc.strokePolygon(new double[]{cx,cx-fw/2,cx+fw/2},new double[]{cy-r-3,cy-r+fh,cy-r+fh},3);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS UI
    // ══════════════════════════════════════════════════════════════════════════
    private void resetPanels() {
        setVisible(panelBarre,true); setVisible(panelRefus,false); setVisible(panelBonus,false);
        if (lblRoueMessage != null) lblRoueMessage.setText("");
        if (progressPoints != null) progressPoints.setProgress(0);
        if (lblPoints != null) lblPoints.setText("0 / 100 pts");
        if (lblProgressText != null) lblProgressText.setText("Vérification des points en cours...");
    }

    private void setBadge(String txt, String color, String bg) {
        if (lblBadgeStatut == null) return;
        lblBadgeStatut.setText(txt);
        lblBadgeStatut.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+color+";" +
                "-fx-background-radius:20;-fx-padding:3 12;-fx-font-size:11px;-fx-font-weight:700;");
    }

    private void setStatut(String txt, String color) {
        if (lblRoueStatut == null) return;
        lblRoueStatut.setText(txt);
        lblRoueStatut.setStyle("-fx-text-fill:"+color+";-fx-font-size:12px;" +
                "-fx-text-alignment:center;-fx-alignment:center;");
    }

    private void setBouton(boolean on, String txt) {
        if (btnTourner == null) return;
        btnTourner.setDisable(!on); btnTourner.setText(txt);
        btnTourner.setStyle(on
                ? "-fx-background-color:linear-gradient(to right,#7C3AED,#4F46E5);" +
                "-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14px;" +
                "-fx-background-radius:28;-fx-padding:14 34;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.7),18,0.4,0,4);"
                : "-fx-background-color:#1F2937;-fx-text-fill:rgba(255,255,255,0.35);" +
                "-fx-font-weight:bold;-fx-font-size:14px;" +
                "-fx-background-radius:28;-fx-padding:14 34;-fx-cursor:default;");
    }

    private void activerBouton() {
        setBouton(true, "🎡  Lancer la roue !");
        if (btnTourner == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(450), btnTourner);
        st.setFromX(1.0); st.setToX(1.06); st.setFromY(1.0); st.setToY(1.06);
        st.setCycleCount(8); st.setAutoReverse(true); st.setInterpolator(Interpolator.EASE_BOTH);
        st.play();
    }

    private void setVisible(VBox box, boolean v) {
        if (box == null) return;
        box.setVisible(v); box.setManaged(v);
    }
}