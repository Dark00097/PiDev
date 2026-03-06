package com.nexora.bank.controllers;

import com.nexora.bank.Service.RoueFortuneEligibilityService;
import com.nexora.bank.Service.RoueFortuneEligibilityService.EligibilityResult;
import com.nexora.bank.Service.RoueFortuneService;
import com.nexora.bank.Service.RoueFortuneService.TirageResult;
import com.nexora.bank.Utils.MyDB;
import com.nexora.bank.Utils.SessionManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
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
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  RoueFortuneController — Fenêtre Stage dédiée
 *
 *  FLUX :
 *   1. ouvert via handleRoueFortune() dans UserDashboardAccountsSectionController
 *   2. Vérification éligibilité (3 conditions SQL + Groq AI)
 *   3a. Éligible  → roue animée (Canvas 330px, ease-out 4s)
 *                  → tirage → sauvegarde → message Groq
 *                  → si 100 pts → +50 DT sur compte bancaire
 *   3b. Non éligible → panel refus avec raison + suggestion Groq
 * ══════════════════════════════════════════════════════════════════════════════
 */
public class RoueFortuneController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private StackPane    rouePaneContainer;
    @FXML private Canvas       rouCanvas;
    @FXML private Button       btnTourner;
    @FXML private Button       btnFermer;
    @FXML private Label        lblRoueStatut;
    @FXML private Label        lblRoueMessage;
    @FXML private Label        lblBadgeStatut;
    // Conditions
    @FXML private HBox         cardCondSolde;
    @FXML private HBox         cardCondBudget;
    @FXML private HBox         cardCondEpargne;
    @FXML private Label        lblCondSolde;
    @FXML private Label        lblCondBudget;
    @FXML private Label        lblCondEpargne;
    @FXML private Label        iconCondSolde;
    @FXML private Label        iconCondBudget;
    @FXML private Label        iconCondEpargne;
    // Panels
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

    // ── Services ──────────────────────────────────────────────────────────────
    private final RoueFortuneEligibilityService eligSvc = new RoueFortuneEligibilityService();
    private final RoueFortuneService            rouSvc  = new RoueFortuneService();

    // ── Roue — configuration ──────────────────────────────────────────────────
    private static final int NB_SEG = 12;
    private static final int[] PTS  = {5, 10, 20, 15, 8, 12, 3, 7, 25, 2, 18, 6};
    private static final Color[] COULEURS = {
            Color.web("#FF4136"), Color.web("#FF851B"), Color.web("#FFDC00"),
            Color.web("#2ECC40"), Color.web("#0074D9"), Color.web("#B10DC9"),
            Color.web("#FF6B9D"), Color.web("#01FF70"), Color.web("#F012BE"),
            Color.web("#3D9970"), Color.web("#e84393"), Color.web("#7FDBFF")
    };

    // ── État ──────────────────────────────────────────────────────────────────
    private double  angle      = 0.0;
    private boolean enTour     = false;
    private int     totalPts   = 0;
    private static final int MAX_PTS = 100;

    // Callback vers le controller parent pour maj barre header
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
        // Cacher le panel refus et relancer la vérification
        setVisible(panelRefus, false);
        setBadge("🔄  Re-vérification...", "#FBBF24", "rgba(251,191,36,0.12)");
        setStatut("🔍  Nouveau scan de ton profil financier...", "#9CA3AF");
        verifierEligibilite();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FERMER LA FENÊTRE
    // ══════════════════════════════════════════════════════════════════════════
    @FXML
    private void fermerFenetre() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        FadeTransition ft = new FadeTransition(Duration.millis(180), stage.getScene().getRoot());
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> stage.close());
        ft.play();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VÉRIFICATION ÉLIGIBILITÉ via Groq AI
    // ══════════════════════════════════════════════════════════════════════════
    private void verifierEligibilite() {
        setBadge("⏳  Vérification...", "#FBBF24", "rgba(251,191,36,0.12)");
        setStatut("🔍  Analyse de ton profil financier ce mois-ci...", "#9CA3AF");
        setBouton(false, "🎡  Lancer la roue");

        int userId = SessionManager.getInstance().getCurrentUserId();

        new Thread(() -> {
            try {
                Connection conn = MyDB.getInstance().getConn();

                // Init table + charger points actuels
                rouSvc.initTable(conn);
                totalPts = rouSvc.getTotalPoints(userId, conn);

                // Afficher immédiatement la barre de progression avec les pts actuels
                Platform.runLater(() -> afficherBarrePoints(0));

                // Déjà joué ce mois ?
                if (rouSvc.aDejaJoueCeMois(userId, conn)) {
                    Platform.runLater(() -> {
                        setBadge("🔒  Déjà joué", "#F97316", "rgba(249,115,22,0.12)");
                        setStatut("⏰  Tu as déjà tourné la roue ce mois-ci.\nReviens le mois prochain !", "#F97316");
                        setBouton(false, "🔒  Déjà joué ce mois");
                        // Afficher quand même la progression actuelle
                        afficherBarrePoints(0);
                    });
                    return;
                }

                // Vérifier les 3 conditions (SQL + Groq analyse)
                EligibilityResult elig = eligSvc.checkEligibility(userId, conn);

                Platform.runLater(() -> {
                    afficherConditions(elig);

                    if (elig.eligible) {
                        // ── CAS 1 : ÉLIGIBLE ──────────────────────────────
                        setBadge("✅  Éligible !", "#22C55E", "rgba(34,197,94,0.12)");
                        setStatut("✅  " + elig.conditionsOk + "/3 conditions validées — Tu peux jouer !", "#22C55E");
                        activerBouton();
                    } else {
                        // ── CAS 2 : NON ÉLIGIBLE ──────────────────────────
                        setBadge("❌  Non éligible", "#EF4444", "rgba(239,68,68,0.12)");
                        setStatut("❌  " + elig.conditionsOk + "/3 conditions — Accès refusé ce mois", "#EF4444");
                        afficherRefus(elig);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setBadge("⚠️  Erreur", "#EF4444", "rgba(239,68,68,0.12)");
                    setStatut("⚠️  Erreur : " + e.getMessage(), "#EF4444");
                    // En cas d'erreur : laisser jouer (fail-safe)
                    activerBouton();
                });
            }
        }, "Roue-Eligibilite").start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LANCER LA ROUE (animation ease-out 4 secondes)
    // ══════════════════════════════════════════════════════════════════════════
    @FXML
    private void lancerRoue() {
        if (enTour) return;
        enTour = true;
        setBouton(false, "🌀  En rotation...");
        setStatut("🌀  La roue tourne... bonne chance !", "#A78BFA");

        // Tirage du segment gagnant
        int seg       = (int)(Math.random() * NB_SEG);
        int pointsGagnes = PTS[seg];

        // Calcul angle de destination (ease-out cubique)
        double aps  = 360.0 / NB_SEG;
        double dest = 5 * 360.0 + (360.0 - (seg * aps + aps / 2.0)) + 270.0;

        // Timeline 120 frames
        Timeline tl = new Timeline();
        int NF = 120;
        for (int i = 0; i <= NF; i++) {
            double t     = (double) i / NF;
            double eased = 1.0 - Math.pow(1.0 - t, 3);  // ease-out cubique
            double a     = angle + dest * eased;
            tl.getKeyFrames().add(new KeyFrame(
                    Duration.seconds(4.0 * t),
                    ev -> dessinerRoue(a % 360)
            ));
        }
        tl.setOnFinished(ev -> {
            angle  = dest % 360;
            enTour = false;
            enregistrerTirage(pointsGagnes);
        });
        tl.play();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ENREGISTRER TIRAGE + BONUS 50 DT SI 100 PTS
    // ══════════════════════════════════════════════════════════════════════════
    private void enregistrerTirage(int pointsGagnes) {
        int userId = SessionManager.getInstance().getCurrentUserId();

        new Thread(() -> {
            try {
                Connection conn = MyDB.getInstance().getConn();
                TirageResult res = rouSvc.tournerRoue(userId, conn);
                totalPts = res.totalPoints;

                // ── BONUS 50 DT si seuil 100 pts atteint → afficher dialog de choix ──
                boolean bonusDeclenche = res.totalPoints >= MAX_PTS;

                final boolean bonus = bonusDeclenche;
                Platform.runLater(() -> {
                    afficherResultat(res, bonus);
                    if (bonus) {
                        // Charger les comptes actifs et afficher le dialog de sélection
                        List<CompteInfo> comptes = chargerComptesActifs(userId, conn);
                        afficherDialogChoixCompte(userId, conn, comptes);
                    }
                });

                // Notifier la barre du header
                if (onPointsUpdated != null)
                    Platform.runLater(onPointsUpdated);

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatut("⚠️  Tirage enregistré mais erreur DB : " + e.getMessage(), "#EF4444");
                    setBouton(false, "🔒  Erreur");
                });
            }
        }, "Roue-Tirage").start();
    }

    // ── Modèle léger pour les comptes affichés dans le dialog ──────────────
    private static class CompteInfo {
        int    idCompte;
        String numero;
        String type;
        double solde;
        CompteInfo(int id, String num, String type, double solde) {
            this.idCompte = id; this.numero = num; this.type = type; this.solde = solde;
        }
    }

    /**
     * Charge tous les comptes ACTIFS de l'utilisateur.
     */
    private List<CompteInfo> chargerComptesActifs(int userId, Connection conn) {
        List<CompteInfo> liste = new ArrayList<>();
        String sql = "SELECT idCompte, numeroCompte, typeCompte, solde FROM compte " +
                "WHERE idUser=? AND statutCompte='Actif' ORDER BY idCompte ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                liste.add(new CompteInfo(
                        rs.getInt("idCompte"),
                        rs.getString("numeroCompte"),
                        rs.getString("typeCompte"),
                        rs.getDouble("solde")
                ));
            }
        } catch (Exception e) {
            System.err.println("[Roue] chargerComptesActifs: " + e.getMessage());
        }
        return liste;
    }

    /**
     * Affiche le dialog de choix du compte pour recevoir les 50 DT.
     * Chaque ligne = un compte actif + bouton "Ajouter 50 DT".
     * Après un clic réussi : remet les points à 0 et ferme le dialog.
     */
    private void afficherDialogChoixCompte(int userId, Connection conn, List<CompteInfo> comptes) {
        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(btnFermer.getScene().getWindow());

        // ── Titre ─────────────────────────────────────────────────────────────
        Label titre = new Label("🎁  Choisir le compte pour +50 DT");
        titre.setStyle("-fx-font-size:16px; -fx-font-weight:900; -fx-text-fill:#D4A82A;");

        Label sousTitre = new Label("Sélectionne le compte sur lequel tu souhaites recevoir ta récompense.");
        sousTitre.setStyle("-fx-font-size:12px; -fx-text-fill:#6B7280; -fx-wrap-text:true;");
        sousTitre.setMaxWidth(430);

        VBox header = new VBox(6, titre, sousTitre);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 12, 24));

        // ── Liste des comptes ──────────────────────────────────────────────────
        VBox listeComptes = new VBox(10);
        listeComptes.setPadding(new Insets(0, 24, 8, 24));

        if (comptes.isEmpty()) {
            Label vide = new Label("❌  Aucun compte actif trouvé.");
            vide.setStyle("-fx-text-fill:#EF4444; -fx-font-size:13px;");
            listeComptes.getChildren().add(vide);
        } else {
            for (CompteInfo c : comptes) {
                // ── Ligne compte ──────────────────────────────────────────────
                Label lblNumero = new Label("🏦  " + c.numero);
                lblNumero.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:#111827;");

                Label lblType = new Label(c.type);
                lblType.setStyle("-fx-font-size:11px; -fx-text-fill:#6B7280; " +
                        "-fx-background-color:#F3F4F6; -fx-background-radius:6; -fx-padding:2 8;");

                Label lblSolde = new Label(String.format("%.2f DT", c.solde));
                lblSolde.setStyle("-fx-font-size:13px; -fx-font-weight:700; -fx-text-fill:#059669;");

                VBox infos = new VBox(3, lblNumero, new HBox(6, lblType, lblSolde));
                ((HBox) infos.getChildren().get(1)).setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(infos, javafx.scene.layout.Priority.ALWAYS);

                Button btnAjouter = new Button("💰  Ajouter 50 DT");
                btnAjouter.setStyle(
                        "-fx-background-color:linear-gradient(to right,#16A34A,#059669);" +
                                "-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:12px;" +
                                "-fx-background-radius:20; -fx-padding:8 18; -fx-cursor:hand;");
                btnAjouter.setOnAction(ev -> {
                    btnAjouter.setDisable(true);
                    btnAjouter.setText("⏳  En cours...");
                    new Thread(() -> {
                        boolean ok = crediter50DT(userId, conn, c.idCompte);
                        if (ok) {
                            rouSvc.resetPoints(userId, conn);
                            totalPts = 0;
                        }
                        Platform.runLater(() -> {
                            if (ok) {
                                // Mise à jour du solde affiché dans la ligne
                                lblSolde.setText(String.format("%.2f DT  (+50 DT ✅)", c.solde + 50.0));
                                btnAjouter.setText("✅  Crédité !");
                                btnAjouter.setStyle(
                                        "-fx-background-color:#D1FAE5; -fx-text-fill:#065F46;" +
                                                "-fx-font-weight:bold; -fx-font-size:12px;" +
                                                "-fx-background-radius:20; -fx-padding:8 18;");
                                // Fermer le dialog après 1.5s
                                new Timeline(new KeyFrame(Duration.millis(1500),
                                        e2 -> dialog.close())).play();
                                // Notifier header
                                if (onPointsUpdated != null) onPointsUpdated.run();
                                afficherBarrePoints(0);
                            } else {
                                btnAjouter.setDisable(false);
                                btnAjouter.setText("❌  Réessayer");
                            }
                        });
                    }, "Roue-Credit").start();
                });

                HBox ligne = new HBox(14, infos, btnAjouter);
                ligne.setAlignment(Pos.CENTER_LEFT);
                ligne.setStyle(
                        "-fx-background-color:white; -fx-background-radius:12;" +
                                "-fx-border-color:#E5E7EB; -fx-border-radius:12; -fx-padding:14 16;");
                // Hover effect
                ligne.setOnMouseEntered(e -> ligne.setStyle(
                        "-fx-background-color:#F0FDF4; -fx-background-radius:12;" +
                                "-fx-border-color:#86EFAC; -fx-border-radius:12; -fx-padding:14 16;"));
                ligne.setOnMouseExited(e -> ligne.setStyle(
                        "-fx-background-color:white; -fx-background-radius:12;" +
                                "-fx-border-color:#E5E7EB; -fx-border-radius:12; -fx-padding:14 16;"));

                listeComptes.getChildren().add(ligne);
            }
        }

        // ── Bouton fermer ──────────────────────────────────────────────────────
        Button btnClose = new Button("Fermer");
        btnClose.setStyle(
                "-fx-background-color:#F3F4F6; -fx-text-fill:#6B7280;" +
                        "-fx-font-size:12px; -fx-background-radius:20; -fx-padding:8 22; -fx-cursor:hand;");
        btnClose.setOnAction(e -> dialog.close());
        HBox footer = new HBox(btnClose);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(6, 24, 18, 24));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#E5E7EB;");

        ScrollPane scroll = new ScrollPane(listeComptes);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        scroll.setMaxHeight(320);

        VBox root = new VBox(0, header, sep, scroll, footer);
        root.setStyle(
                "-fx-background-color:white; -fx-background-radius:18;" +
                        "-fx-border-color:#E5E7EB; -fx-border-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.22),28,0.3,0,6);");
        root.setPrefWidth(480);

        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);

        // Animation d'entrée
        root.setOpacity(0);
        root.setScaleX(0.92); root.setScaleY(0.92);
        FadeTransition ft = new FadeTransition(Duration.millis(250), root);
        ft.setFromValue(0); ft.setToValue(1);
        ScaleTransition st2 = new ScaleTransition(Duration.millis(250), root);
        st2.setFromX(0.92); st2.setToX(1.0);
        st2.setFromY(0.92); st2.setToY(1.0);
        st2.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, st2).play();

        dialog.show();
    }

    /**
     * Crédite 50 DT sur un compte spécifique choisi par l'utilisateur.
     */
    private boolean crediter50DT(int userId, Connection conn, int idCompte) {
        try {
            double soldeActuel = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT solde FROM compte WHERE idCompte=?")) {
                ps.setInt(1, idCompte);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) soldeActuel = rs.getDouble("solde");
                else { System.err.println("[Roue] Compte " + idCompte + " introuvable"); return false; }
            }

            double nouveauSolde = soldeActuel + 50.0;

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE compte SET solde=? WHERE idCompte=?")) {
                ps.setDouble(1, nouveauSolde);
                ps.setInt(2, idCompte);
                ps.executeUpdate();
            }

            String sqlTx = "INSERT INTO transactions " +
                    "(idCompte, idUser, categorie, dateTransaction, montant, typeTransaction, " +
                    " statutTransaction, soldeApres, description) " +
                    "VALUES (?, ?, 'Recompense', ?, 50.00, 'CREDIT', 'VALIDED', ?, " +
                    "'🎁 Récompense Roue de la Fortune — 100 pts atteints')";
            try (PreparedStatement ps = conn.prepareStatement(sqlTx)) {
                ps.setInt(1, idCompte);
                ps.setInt(2, userId);
                ps.setString(3, java.time.LocalDate.now().toString());
                ps.setDouble(4, nouveauSolde);
                ps.executeUpdate();
            }
            System.out.println("[Roue] ✅ +50 DT → compte " + idCompte + " | nouveau solde=" + nouveauSolde);
            return true;
        } catch (Exception e) {
            System.err.println("[Roue] ❌ crediter50DT: " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  AFFICHER RÉSULTAT APRÈS TIRAGE
    // ══════════════════════════════════════════════════════════════════════════
    private void afficherResultat(TirageResult res, boolean bonus50DT) {
        // Message principal avec animation fade
        if (lblRoueMessage != null) {
            lblRoueMessage.setText(res.message);
            FadeTransition ft = new FadeTransition(Duration.millis(700), lblRoueMessage);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
        }

        setStatut("🎊  Félicitations ! Reviens le mois prochain !", "#22C55E");
        setBadge("✅  Tour terminé", "#22C55E", "rgba(34,197,94,0.12)");
        setBouton(false, "🔒  À la prochaine fois !");

        // Barre de progression
        afficherBarrePoints(res.pointsGagnes);

        // Bonus 50 DT
        if (bonus50DT && panelBonus != null) {
            panelBonus.setVisible(true);
            panelBonus.setManaged(true);
            FadeTransition ftB = new FadeTransition(Duration.millis(800), panelBonus);
            ftB.setFromValue(0); ftB.setToValue(1);
            ftB.setDelay(Duration.millis(600));
            ftB.play();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  AFFICHER CONDITIONS
    // ══════════════════════════════════════════════════════════════════════════
    private void afficherConditions(EligibilityResult elig) {
        setCond(cardCondSolde,   lblCondSolde,   iconCondSolde,
                "Solde toujours positif",  elig.conditionSolde);
        setCond(cardCondBudget,  lblCondBudget,  iconCondBudget,
                "Budget mensuel respecté", elig.conditionBudget);
        setCond(cardCondEpargne, lblCondEpargne, iconCondEpargne,
                "Épargne mensuelle active", elig.conditionEpargne);

        // Animations séquentielles des cards
        animCond(cardCondSolde,   0);
        animCond(cardCondBudget,  150);
        animCond(cardCondEpargne, 300);
    }

    private void setCond(HBox card, Label lbl, Label icon, String texte, boolean ok) {
        if (ok) {
            if (card != null) card.setStyle(card.getStyle()
                    .replace("rgba(255,255,255,0.03)","rgba(34,197,94,0.08)")
                    .replace("rgba(255,255,255,0.07)","rgba(34,197,94,0.25)"));
            if (lbl  != null) { lbl.setText("✅  " + texte);  lbl.setStyle("-fx-text-fill:#22C55E; -fx-font-size:12px; -fx-font-weight:700;"); }
            if (icon != null) { icon.setText("✅"); }
        } else {
            if (card != null) card.setStyle(card.getStyle()
                    .replace("rgba(255,255,255,0.03)","rgba(239,68,68,0.08)")
                    .replace("rgba(255,255,255,0.07)","rgba(239,68,68,0.25)"));
            if (lbl  != null) { lbl.setText("❌  " + texte);  lbl.setStyle("-fx-text-fill:#EF4444; -fx-font-size:12px; -fx-font-weight:700;"); }
            if (icon != null) { icon.setText("❌"); }
        }
    }

    private void animCond(HBox card, int delayMs) {
        if (card == null) return;
        card.setOpacity(0);
        card.setTranslateX(20);
        FadeTransition ft = new FadeTransition(Duration.millis(400), card);
        ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(delayMs));
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), card);
        tt.setFromX(20); tt.setToX(0); tt.setDelay(Duration.millis(delayMs));
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  AFFICHER REFUS (cas 2 : condition non valide)
    // ══════════════════════════════════════════════════════════════════════════
    private void afficherRefus(EligibilityResult elig) {
        if (panelRefus == null) return;
        panelRefus.setVisible(true);
        panelRefus.setManaged(true);

        // Message principal de Groq AI
        if (lblRefusRaison != null) {
            String raison = elig.message != null && !elig.message.isEmpty()
                    ? elig.message
                    : buildRefusMessage(elig);
            lblRefusRaison.setText(raison);
        }

        // Solutions par condition — 100% générées par Groq AI avec les montants exacts
        // Si Groq n'a pas fourni de solution, on l'indique honnêtement
        afficherSolutionCond(panelSolutionSolde,   lblSolutionSolde,   elig.conditionSolde,
                elig.solutionSolde);
        afficherSolutionCond(panelSolutionBudget,  lblSolutionBudget,  elig.conditionBudget,
                elig.solutionBudget);
        afficherSolutionCond(panelSolutionEpargne, lblSolutionEpargne, elig.conditionEpargne,
                elig.solutionEpargne);

        // Suggestion globale Groq
        if (lblRefusSuggestion != null) {
            String sug = (elig.suggestion != null && !elig.suggestion.isEmpty())
                    ? elig.suggestion
                    : "Effectuez les actions ci-dessus puis cliquez sur Re-vérifier.";
            lblRefusSuggestion.setText("💡 " + sug);
        }

        // Slide-in avec animation
        panelRefus.setOpacity(0);
        panelRefus.setTranslateY(15);
        FadeTransition ft = new FadeTransition(Duration.millis(500), panelRefus);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), panelRefus);
        tt.setFromY(15); tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    /** Affiche ou cache un panel solution selon si la condition est échouée. */
    private void afficherSolutionCond(VBox panel, Label lbl, boolean condOk, String solutionGroq) {
        if (panel == null) return;
        if (condOk) {
            panel.setVisible(false);
            panel.setManaged(false);
        } else {
            panel.setVisible(true);
            panel.setManaged(true);
            if (lbl != null) {
                // Solution 100% issue de Groq AI avec les montants exacts
                String sol = (solutionGroq != null && !solutionGroq.isBlank())
                        ? solutionGroq
                        : "⚠️ Analyse indisponible. Vérifiez votre connexion et réessayez.";
                lbl.setText(sol);
            }
        }
    }

    /** Message de refus local si Groq ne fournit pas de message. */
    private String buildRefusMessage(EligibilityResult elig) {
        StringBuilder sb = new StringBuilder();
        if (!elig.conditionSolde)
            sb.append("❌ Solde négatif détecté ce mois. ");
        if (!elig.conditionBudget)
            sb.append("❌ Budget global dépassé (dépenses > 80% des revenus). ");
        if (!elig.conditionEpargne)
            sb.append("❌ Aucun virement vers le Coffre Virtuel. ");
        return sb.toString().trim();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BARRE DE PROGRESSION
    // ══════════════════════════════════════════════════════════════════════════
    private void afficherBarrePoints(int gained) {
        if (panelBarre == null) return;
        panelBarre.setVisible(true);
        panelBarre.setManaged(true);

        double pct    = Math.min((double) totalPts / MAX_PTS, 1.0);
        int    pctInt = (int)(pct * 100);

        // Animation de la barre
        new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(progressPoints.progressProperty(), progressPoints.getProgress())),
                new KeyFrame(Duration.millis(1000),
                        new KeyValue(progressPoints.progressProperty(), pct, Interpolator.EASE_OUT))
        ).play();

        // Changer la couleur de la barre selon le palier
        String barColor;
        if      (pctInt >= 100) barColor = "-fx-accent:#FFD700;";
        else if (pctInt >= 50)  barColor = "-fx-accent:#22C55E;";
        else if (pctInt >= 25)  barColor = "-fx-accent:#FBBF24;";
        else                    barColor = "-fx-accent:#A78BFA;";
        if (progressPoints != null)
            progressPoints.setStyle(barColor
                    + "-fx-background-color:rgba(255,255,255,0.07);"
                    + "-fx-background-radius:7; -fx-border-radius:7;");

        // Label points avec pourcentage
        if (lblPoints != null)
            lblPoints.setText(totalPts + " / " + MAX_PTS + " pts  —  " + pctInt + "%");

        // Message selon contexte
        if (lblProgressText != null) {
            int restant = MAX_PTS - totalPts;
            String msg;
            if (totalPts >= MAX_PTS) {
                msg = "🏆 OBJECTIF 100 PTS ATTEINT ! 50 DT viennent d'être crédités sur ton compte !";
            } else if (gained >= 20) {
                msg = "🔥 JACKPOT ! +" + gained + " pts ! Plus que " + restant + " pts pour décrocher 50 DT !";
            } else if (gained >= 10) {
                msg = "⭐ Super ! +" + gained + " pts en poche. Encore " + restant + " pts pour le bonus 50 DT !";
            } else if (gained > 0) {
                msg = "👏 Bravo ! +" + gained + " pts. Tu progresses ! Plus que " + restant + " pts pour 50 DT !";
            } else if (totalPts == 0) {
                msg = "🎯 Commence à accumuler des points — 100 pts = 50 DT sur ton compte !";
            } else {
                msg = "💪 " + totalPts + " pts accumulés. Encore " + restant + " pts pour gagner 50 DT !";
            }
            lblProgressText.setText(msg);
        }

        // Animation spéciale si objectif atteint
        if (totalPts >= MAX_PTS && panelBarre != null) {
            panelBarre.setStyle(
                    "-fx-background-color:rgba(255,215,0,0.15);"
                            + "-fx-border-color:#FFD700; -fx-border-radius:14;"
                            + "-fx-background-radius:14; -fx-padding:16 18;");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DESSIN DE LA ROUE (Canvas JavaFX)
    // ══════════════════════════════════════════════════════════════════════════
    private void dessinerRoue(double offset) {
        if (rouCanvas == null) return;
        GraphicsContext gc = rouCanvas.getGraphicsContext2D();
        double W = rouCanvas.getWidth(), H = rouCanvas.getHeight();
        double cx = W / 2, cy = H / 2;
        double r  = Math.min(W, H) / 2 - 14;
        double aps = 360.0 / NB_SEG;

        gc.clearRect(0, 0, W, H);

        // Ombre extérieure
        gc.setFill(new RadialGradient(0, 0, cx, cy, r + 14, false, CycleMethod.NO_CYCLE,
                new Stop(0.82, Color.TRANSPARENT),
                new Stop(1.0,  Color.color(0, 0, 0, 0.5))));
        gc.fillOval(cx - r - 14, cy - r - 14, (r + 14) * 2, (r + 14) * 2);

        // Bordure épaisse gris foncé
        gc.setFill(Color.web("#1F1F2E"));
        gc.fillOval(cx - r - 7, cy - r - 7, (r + 7) * 2, (r + 7) * 2);

        // Bordure dorée
        RadialGradient goldGrad = new RadialGradient(45, 0.3, cx - r * 0.3, cy - r * 0.3, r + 5,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFE566")),
                new Stop(0.5, Color.web("#D4AF37")),
                new Stop(1, Color.web("#8B6914")));
        gc.setFill(goldGrad);
        gc.fillOval(cx - r - 4, cy - r - 4, (r + 4) * 2, (r + 4) * 2);

        // Segments
        for (int i = 0; i < NB_SEG; i++) {
            double sa = offset + i * aps - aps / 2;

            // Segment
            gc.setFill(COULEURS[i]);
            gc.fillArc(cx - r, cy - r, r * 2, r * 2, -sa, -aps, ArcType.ROUND);

            // Séparateur subtil
            gc.setStroke(Color.color(0, 0, 0, 0.35));
            gc.setLineWidth(1.5);
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2, -sa, -aps, ArcType.ROUND);

            // Texte du segment
            double mid = Math.toRadians(sa + aps / 2);
            double tx  = cx + r * 0.68 * Math.cos(mid);
            double ty  = cy + r * 0.68 * Math.sin(mid);
            gc.save();
            gc.translate(tx, ty);
            gc.rotate(sa + aps / 2 + 90);
            // Ombre texte
            gc.setFill(Color.color(0, 0, 0, 0.5));
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(String.valueOf(PTS[i]), 1, 1);
            // Texte blanc
            gc.setFill(Color.WHITE);
            gc.fillText(String.valueOf(PTS[i]), 0, 0);
            gc.restore();
        }

        // Centre : disque métallique
        double rc = r * 0.14;
        // Anneau extérieur du centre
        gc.setFill(Color.web("#1F1F2E"));
        gc.fillOval(cx - rc - 3, cy - rc - 3, (rc + 3) * 2, (rc + 3) * 2);
        // Centre doré avec gradient
        RadialGradient centreGrad = new RadialGradient(0, 0, cx - rc * 0.35, cy - rc * 0.35, rc,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFF0A0")),
                new Stop(0.5, Color.web("#D4AF37")),
                new Stop(1, Color.web("#9A7A1A")));
        gc.setFill(centreGrad);
        gc.fillOval(cx - rc, cy - rc, rc * 2, rc * 2);

        // Flèche indicatrice (en haut, rouge)
        double fw = 13, fh = 22;
        // Corps flèche rouge
        gc.setFill(Color.web("#DC2626"));
        gc.fillPolygon(
                new double[]{cx, cx - fw / 2, cx + fw / 2},
                new double[]{cy - r - 3, cy - r + fh, cy - r + fh},
                3);
        // Reflet flèche
        gc.setFill(Color.web("#FF6B6B"));
        gc.fillPolygon(
                new double[]{cx, cx - fw / 3.5, cx},
                new double[]{cy - r - 3, cy - r + fh * 0.6, cy - r + fh * 0.6},
                3);
        // Contour
        gc.setStroke(Color.web("#7F1D1D"));
        gc.setLineWidth(1);
        gc.strokePolygon(
                new double[]{cx, cx - fw / 2, cx + fw / 2},
                new double[]{cy - r - 3, cy - r + fh, cy - r + fh},
                3);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS UI
    // ══════════════════════════════════════════════════════════════════════════

    private void resetPanels() {
        setVisible(panelBarre,  true);   // toujours visible pour montrer la progression
        setVisible(panelRefus,  false);
        setVisible(panelBonus,  false);
        if (lblRoueMessage != null) lblRoueMessage.setText("");
        // Initialiser la barre à 0 pts au départ (sera mis à jour en verifierEligibilite)
        if (progressPoints != null) progressPoints.setProgress(0);
        if (lblPoints != null) lblPoints.setText("0 / 100 pts");
        if (lblProgressText != null) lblProgressText.setText("Vérification des points en cours...");
    }

    private void setBadge(String txt, String color, String bg) {
        if (lblBadgeStatut == null) return;
        lblBadgeStatut.setText(txt);
        lblBadgeStatut.setStyle(
                "-fx-background-color:" + bg + ";"
                        + "-fx-text-fill:" + color + ";"
                        + "-fx-background-radius:20; -fx-padding:3 12;"
                        + "-fx-font-size:11px; -fx-font-weight:700;");
    }

    private void setStatut(String txt, String color) {
        if (lblRoueStatut == null) return;
        lblRoueStatut.setText(txt);
        lblRoueStatut.setStyle("-fx-text-fill:" + color
                + "; -fx-font-size:12px; -fx-text-alignment:center; -fx-alignment:center;");
    }

    private void setBouton(boolean on, String txt) {
        if (btnTourner == null) return;
        btnTourner.setDisable(!on);
        btnTourner.setText(txt);
        if (on) {
            btnTourner.setStyle(
                    "-fx-background-color:linear-gradient(to right,#7C3AED,#4F46E5);"
                            + "-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px;"
                            + "-fx-background-radius:28; -fx-padding:14 34; -fx-cursor:hand;"
                            + "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.7),18,0.4,0,4);");
        } else {
            btnTourner.setStyle(
                    "-fx-background-color:#1F2937; -fx-text-fill:rgba(255,255,255,0.35);"
                            + "-fx-font-weight:bold; -fx-font-size:14px;"
                            + "-fx-background-radius:28; -fx-padding:14 34; -fx-cursor:default;");
        }
    }

    private void activerBouton() {
        setBouton(true, "🎡  Lancer la roue !");
        // Pulsation pour attirer l'attention
        if (btnTourner == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(450), btnTourner);
        st.setFromX(1.0); st.setToX(1.06);
        st.setFromY(1.0); st.setToY(1.06);
        st.setCycleCount(8); st.setAutoReverse(true);
        st.setInterpolator(Interpolator.EASE_BOTH);
        st.play();
    }

    private void setVisible(VBox box, boolean v) {
        if (box == null) return;
        box.setVisible(v); box.setManaged(v);
    }
}