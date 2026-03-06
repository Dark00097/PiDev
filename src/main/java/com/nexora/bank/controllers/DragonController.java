package com.nexora.bank.controllers;

import com.nexora.bank.Models.CompteBancaire;
import com.nexora.bank.Models.CoffreVirtuel;
import com.nexora.bank.Models.DragonState;
import com.nexora.bank.Service.CoffreVirtuelService;
import com.nexora.bank.Service.DragonService;
import com.nexora.bank.Utils.SessionManager;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.Random;

/**
 * DragonController — Hero Styliste évolutif et émotionnel.
 *
 * Images attendues dans : src/main/resources/com/nexora/bank/images/
 *   hero_heureux.png  → im2 (bras ouverts, sourire, confettis)
 *   hero_triste.png   → im1 (tête baissée, yeux mi-clos, triste)
 *   hero_fache.png    → im3 (yeux rouges, feu, bras croisés)
 *   hero_neutre.png   → im4 (regard sérieux, feu au menton, neutre/progressant)
 */
public class DragonController {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Label       lblDragonEmoji;
    @FXML private ImageView   imgAvatar;
    @FXML private StackPane   avatarPane;
    @FXML private Label       lblDragonNiveau;
    @FXML private Label       lblDragonHumeur;
    @FXML private Label       lblProgressionPct;
    @FXML private Label       lblNomCoffre;
    @FXML private Label       lblMessageDragon;
    @FXML private Label       lblConseil;
    @FXML private Label       lblSolde;
    @FXML private Label       lblDepenses;
    @FXML private Label       lblMontantCoffre;
    @FXML private ProgressBar progressBar;
    @FXML private Button      btnParler;
    @FXML private Button      btnNourrir;
    @FXML private VBox        nourirPanel;
    @FXML private TextField   txtMontantNourrir;
    @FXML private Button      btnConfirmerNourrir;
    @FXML private Button      btnAnnulerNourrir;
    @FXML private VBox        dragonContainer;

    // ── Services ──────────────────────────────────────────────────────────────
    private final DragonService        dragonService  = new DragonService();
    private final CoffreVirtuelService coffreService  = new CoffreVirtuelService();

    // ── Images avatar (une par état émotionnel) ───────────────────────────────
    /**
     * hero_heureux.png → im2 : Hero joyeux, bras ouverts (objectif atteint / heureux)
     * hero_triste.png  → im1 : Hero triste, tête baissée (aucun dépôt 2 mois / triste)
     * hero_fache.png   → im3 : Hero fâché, feu, bras croisés (inactivité extrême / caché)
     * hero_neutre.png  → im4 : Hero sérieux, progressant (neutre / content / fatigué)
     */
    private Image imgHeureux, imgTriste, imgFache, imgNeutre, imgPeutContent;

    // ── Tailles avatar selon le niveau de progression ─────────────────────────
    // BEBE(0-25%)=48px, PETIT(26-50%)=64px, ADOLESCENT(51-75%)=80px,
    // ADULTE(76-99%)=96px, LEGENDAIRE(100%)=112px
    private static final double[] AVATAR_SIZES = { 48, 64, 80, 96, 112 };

    // ── État courant ──────────────────────────────────────────────────────────
    private DragonState    currentState;
    private CompteBancaire currentCompte;
    private CoffreVirtuel  currentCoffre;

    /** Animations en cours (à stopper avant d'en lancer une nouvelle) */
    private final List<Animation> runningAnimations = new java.util.ArrayList<>();

    // ── Init ──────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        cacherNourirPanel();
        chargerAvatars();
    }

    /**
     * Charge les 4 images du héros selon l'état émotionnel.
     * Chemin du projet : src/main/resources/images/hero_xxx.png
     * → Classpath resource : /images/hero_xxx.png
     */
    private void chargerAvatars() {
        imgHeureux     = loadImage("/images/hero_heureux.png",     "images/hero_heureux.png");
        imgTriste      = loadImage("/images/hero_triste.png",      "images/hero_triste.png");
        imgFache       = loadImage("/images/hero_fache.png",       "images/hero_fache.png");
        imgNeutre      = loadImage("/images/hero_neutre.png",      "images/hero_neutre.png");
        imgPeutContent = loadImage("/images/hero_peutContent.jpg", "images/hero_peutContent.jpg",
                "/images/hero_peutContent.png", "images/hero_peutContent.png");

        // Fallback sur l'ancienne image unique si les nouvelles sont absentes
        if (imgNeutre == null)
            imgNeutre = loadImage("/images/avatar_hero.png", "/images/avatar_hero.jpg",
                    "images/avatar_hero.png", "images/avatar_hero.jpg");

        // Afficher l'image neutre par défaut au démarrage
        if (imgAvatar != null && imgNeutre != null) imgAvatar.setImage(imgNeutre);
    }

    /**
     * Charge une image depuis le classpath avec fallback chemin absolu.
     * @param paths un ou plusieurs chemins classpath à essayer dans l'ordre
     */
    private Image loadImage(String... paths) {
        // Stratégie 1 : classpath getResource
        for (String path : paths) {
            try {
                var url = getClass().getResource(path);
                if (url == null)
                    url = Thread.currentThread().getContextClassLoader()
                            .getResource(path.startsWith("/") ? path.substring(1) : path);
                if (url != null) {
                    Image img = new Image(url.toExternalForm(), true);
                    System.out.println("[Dragon] ✅ Image : " + path);
                    return img;
                }
            } catch (Exception ignored) {}
        }
        // Stratégie 2 : chemin absolu src/main/resources
        for (String path : paths) {
            try {
                var loc = getClass().getProtectionDomain().getCodeSource().getLocation();
                if (loc != null) {
                    java.io.File base = new java.io.File(loc.toURI());
                    java.io.File projectDir = base;
                    for (int i = 0; i < 4; i++) {
                        if (projectDir.getParentFile() != null)
                            projectDir = projectDir.getParentFile();
                    }
                    String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
                    java.io.File[] candidates = {
                            new java.io.File(base, "images/" + fileName),
                            new java.io.File(projectDir, "src/main/resources/images/" + fileName)
                    };
                    for (java.io.File f : candidates) {
                        if (f.exists()) {
                            Image img = new Image(f.toURI().toString());
                            if (!img.isError()) {
                                System.out.println("[Dragon] ✅ Image fichier : " + f.getAbsolutePath());
                                return img;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        System.err.println("[Dragon] ❌ Image introuvable : " + paths[0]
                + " — vérifiez src/main/resources/images/");
        return null;
    }

    /**
     * Choisit l'image selon la règle de progression du coffre.
     *
     * ┌─────────────────────────────────────┬──────────────────┐
     * │ Condition                           │ Image affichée   │
     * ├─────────────────────────────────────┼──────────────────┤
     * │ Aucun dépôt depuis 2 mois (BD)      │ hero_fache  (im3)│  ← PRIORITÉ 1
     * │ 100% — objectif atteint             │ hero_heureux(im2)│  ← PRIORITÉ 2
     * │ 60–99% — presque atteint            │ hero_neutre (im4)│  ← PRIORITÉ 3
     * │ 26–59% — progression normale        │ hero_neutre (im4)│  ← PRIORITÉ 4
     * │ 0–25%  — progression faible         │ hero_triste (im1)│  ← PRIORITÉ 5
     * └─────────────────────────────────────┴──────────────────┘
     */
    private Image choisirImage(DragonState s) {
        // ── PRIORITÉ 1 : aucun dépôt depuis 2 mois (vérifié en base de données)
        if (s.isAbandonDepot2Mois()) {
            System.out.println("[Dragon] Image → hero_fache  (aucun dépôt depuis 2 mois)");
            return imgFache != null ? imgFache : imgNeutre;
        }

        int pct = s.getProgressionPct();

        // ── PRIORITÉ 2 : objectif atteint à 100%
        if (pct >= 100) {
            System.out.println("[Dragon] Image → hero_heureux (100% — objectif atteint)");
            return imgHeureux != null ? imgHeureux : imgNeutre;
        }

        // ── PRIORITÉ 3 : 60–99% → hero_peutContent (presque atteint, content)
        if (pct >= 60) {
            System.out.println("[Dragon] Image → hero_peutContent (" + pct + "% — presque atteint)");
            return imgPeutContent != null ? imgPeutContent : imgNeutre;
        }

        // ── PRIORITÉ 4 : 25–59% → hero_neutre (progression normale)
        if (pct >= 25) {
            System.out.println("[Dragon] Image → hero_neutre  (" + pct + "% — en progression)");
            return imgNeutre;
        }

        // ── PRIORITÉ 5 : 0–24% progression faible → hero_triste
        System.out.println("[Dragon] Image → hero_triste  (" + pct + "% — progression faible)");
        return imgTriste != null ? imgTriste : imgNeutre;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Point d'entrée public : appelé depuis UserDashboardAccountsSectionController
    // ─────────────────────────────────────────────────────────────────────────
    public void updateDragon(CompteBancaire compte) {
        this.currentCompte = compte;
        int idUser = SessionManager.getInstance().getCurrentUserId();
        List<CoffreVirtuel> coffres = coffreService.getByCompte(compte.getIdCompte());
        this.currentCoffre = choisirCoffre(coffres);

        // Construire l'état métier (instantané, hors-ligne)
        DragonState state = dragonService.buildDragonState(compte, coffres, idUser);
        this.currentState = state;

        // Mettre à jour l'UI immédiatement
        updateUI(state);
        stopAllAnimations();
        animerAvatar(state);

        // Enrichir les messages via IA en arrière-plan
        dragonService.enrichirAvecIA(state, () -> updateMessages(state));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mise à jour UI complète
    // ─────────────────────────────────────────────────────────────────────────
    private void updateUI(DragonState s) {
        // ── Taille de l'avatar selon le niveau de progression ─────────────
        safe(imgAvatar, () -> {
            double size = getAvatarSize(s.getNiveau());
            imgAvatar.setFitWidth(size);
            imgAvatar.setFitHeight(size);
            // Charger l'image correspondant à l'état émotionnel
            Image img = choisirImage(s);
            if (img != null) imgAvatar.setImage(img);
            // Réinitialiser transformations
            imgAvatar.setScaleX(1); imgAvatar.setScaleY(1);
            imgAvatar.setTranslateX(0); imgAvatar.setTranslateY(0);
            imgAvatar.setOpacity(1);
            imgAvatar.setRotate(0);
        });

        // ── Glow coloré autour de l'avatar selon l'humeur ─────────────────
        safe(avatarPane, () -> {
            // CORRECTIF : glow rouge si abandon ≥ 2 mois (hero_fache)
            String glowColor = s.isAbandonDepot2Mois() ? "#EF4444" : s.getHumeur().color;
            avatarPane.setStyle(
                    "-fx-background-color: radial-gradient(center 50% 50%, radius 60%, " +
                            toRgba(glowColor, 0.3) + ", transparent);" +
                            "-fx-background-radius: 50%;" +
                            "-fx-padding: 8;" +
                            "-fx-effect: dropshadow(gaussian, " + glowColor + ", 18, 0.4, 0, 0);"
            );
        });

        safe(lblDragonNiveau, () -> lblDragonNiveau.setText(s.getNiveau().emoji + " " + s.getNiveau().label));
        safe(lblDragonHumeur, () -> {
            // ── CORRECTIF : si abandon ≥ 2 mois → afficher "😡 Fâché" (rouge)
            // même si calculerHumeur() a retourné HEUREUX (solde élevé, etc.)
            if (s.isAbandonDepot2Mois()) {
                lblDragonHumeur.setText("😡 Fâché");
                lblDragonHumeur.setStyle("-fx-text-fill:#EF4444;-fx-font-size:12px;-fx-font-weight:bold;");
            } else {
                lblDragonHumeur.setText(s.getHumeur().emoji + " " + s.getHumeur().label);
                lblDragonHumeur.setStyle("-fx-text-fill:" + s.getHumeur().color +
                        ";-fx-font-size:12px;-fx-font-weight:bold;");
            }
        });

        safe(progressBar,       () -> progressBar.setProgress(s.getProgressionPct() / 100.0));
        safe(lblProgressionPct, () -> lblProgressionPct.setText(s.getProgressionPct() + "%"));
        safe(lblNomCoffre,      () -> lblNomCoffre.setText(s.getNomCoffre()));
        safe(lblMontantCoffre,  () -> lblMontantCoffre.setText(
                String.format("%.2f / %.2f DT", s.getMontantActuel(), s.getObjectifMontant())));
        safe(lblSolde,          () -> lblSolde.setText(String.format("%.2f DT", s.getSoldeCompte())));
        safe(lblDepenses,       () -> lblDepenses.setText(String.format("%.2f DT", s.getDepensesRecentes())));

        if (txtMontantNourrir != null && s.getObjectifMontant() > s.getMontantActuel()) {
            double suggestion = Math.min(50, s.getObjectifMontant() - s.getMontantActuel());
            txtMontantNourrir.setPromptText("Suggestion : " + String.format("%.0f", suggestion) + " DT");
        }

        updateMessages(s);
    }

    private void updateMessages(DragonState s) {
        safe(lblMessageDragon, () -> lblMessageDragon.setText(s.getMessageIA()));
        safe(lblConseil,       () -> lblConseil.setText(s.getConseil()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Taille avatar selon le niveau
    // ─────────────────────────────────────────────────────────────────────────
    private double getAvatarSize(DragonState.Niveau niveau) {
        if (niveau == null) return AVATAR_SIZES[0];
        switch (niveau) {
            case BEBE:        return AVATAR_SIZES[0]; // 48px  — tout petit
            case PETIT:       return AVATAR_SIZES[1]; // 64px  — enfant
            case ADOLESCENT:  return AVATAR_SIZES[2]; // 80px  — intermédiaire
            case ADULTE:      return AVATAR_SIZES[3]; // 96px  — fort et confiant
            case LEGENDAIRE:  return AVATAR_SIZES[4]; // 112px — majestueux
            default:          return AVATAR_SIZES[0];
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Animation avatar — même priorités que choisirImage()
    // ─────────────────────────────────────────────────────────────────────────
    private void animerAvatar(DragonState s) {
        if (imgAvatar == null) return;

        // PRIORITÉ 1 : aucun dépôt depuis 2 mois (BD) → fâché
        if (s.isAbandonDepot2Mois()) {
            animerFache();
            return;
        }

        int pct = s.getProgressionPct();

        // PRIORITÉ 2 : 100% objectif atteint → victoire + confettis
        if (pct >= 100) {
            animerVictoire();
            lancerConfettis();
            return;
        }

        // PRIORITÉ 3 : 60-99% presque atteint → rebond content
        if (pct >= 60) {
            animerContent();
            return;
        }

        // PRIORITÉ 4 : 26-59% progression normale → lévitation
        if (pct >= 26) {
            animerLevitation();
            return;
        }

        // PRIORITÉ 5 : 0–24% progression faible → animation bébé (douce et lente)
        animerBebe();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Animations individuelles
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 🐣 Bébé (0-25%) : flottement très doux et lent — animation apaisante
     */
    private void animerBebe() {
        TranslateTransition tt = new TranslateTransition(Duration.millis(2200), imgAvatar);
        tt.setFromY(0); tt.setToY(-8);
        tt.setAutoReverse(true);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        jouer(tt);
    }

    /**
     * 🌱 En croissance (25-60%) : lévitation légère — progression stable
     */
    private void animerLevitation() {
        TranslateTransition tt = new TranslateTransition(Duration.millis(1600), imgAvatar);
        tt.setFromY(0); tt.setToY(-14);
        tt.setAutoReverse(true);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        jouer(tt);
    }

    /**
     * 💪 Content (60-99%) : rebond joyeux + légère rotation — enthousiaste
     */
    private void animerContent() {
        // Rebond vertical
        TranslateTransition bounce = new TranslateTransition(Duration.millis(500), imgAvatar);
        bounce.setFromY(0); bounce.setToY(-20);
        bounce.setAutoReverse(true);
        bounce.setCycleCount(Animation.INDEFINITE);
        bounce.setInterpolator(Interpolator.EASE_OUT);

        // Légère rotation (bras levés simulés)
        RotateTransition rotate = new RotateTransition(Duration.millis(1000), imgAvatar);
        rotate.setFromAngle(-5); rotate.setToAngle(5);
        rotate.setAutoReverse(true);
        rotate.setCycleCount(Animation.INDEFINITE);

        ParallelTransition pt = new ParallelTransition(bounce, rotate);
        jouer(pt);
    }

    /**
     * 🎉 Objectif atteint (100%) : danse — rotation + saut + scale
     */
    private void animerVictoire() {
        // Saut
        TranslateTransition saut = new TranslateTransition(Duration.millis(400), imgAvatar);
        saut.setFromY(0); saut.setToY(-30);
        saut.setAutoReverse(true);
        saut.setCycleCount(Animation.INDEFINITE);
        saut.setInterpolator(Interpolator.EASE_OUT);

        // Rotation (danse)
        RotateTransition rotation = new RotateTransition(Duration.millis(600), imgAvatar);
        rotation.setFromAngle(-15); rotation.setToAngle(15);
        rotation.setAutoReverse(true);
        rotation.setCycleCount(Animation.INDEFINITE);

        // Scale (pulsation)
        ScaleTransition scale = new ScaleTransition(Duration.millis(400), imgAvatar);
        scale.setFromX(1); scale.setToX(1.15);
        scale.setFromY(1); scale.setToY(1.15);
        scale.setAutoReverse(true);
        scale.setCycleCount(Animation.INDEFINITE);

        ParallelTransition danse = new ParallelTransition(saut, rotation, scale);
        jouer(danse);
    }

    /**
     * 😢 Triste (abandon 2 mois) : tremblement lent + légère opacité réduite + descente
     */
    private void animerTriste() {
        // Descente légère (tête baissée)
        TranslateTransition descente = new TranslateTransition(Duration.millis(1500), imgAvatar);
        descente.setFromY(0); descente.setToY(6);
        descente.setAutoReverse(true);
        descente.setCycleCount(Animation.INDEFINITE);
        descente.setInterpolator(Interpolator.EASE_BOTH);

        // Tremblement horizontal lent
        TranslateTransition tremblement = new TranslateTransition(Duration.millis(300), imgAvatar);
        tremblement.setFromX(0); tremblement.setToX(-3);
        tremblement.setAutoReverse(true);
        tremblement.setCycleCount(Animation.INDEFINITE);

        // Larmes (fade pulsé)
        FadeTransition larmes = new FadeTransition(Duration.millis(2000), imgAvatar);
        larmes.setFromValue(1.0); larmes.setToValue(0.6);
        larmes.setAutoReverse(true);
        larmes.setCycleCount(Animation.INDEFINITE);

        SequentialTransition seq = new SequentialTransition(descente);
        ParallelTransition pt = new ParallelTransition(tremblement, larmes);
        jouer(seq);
        jouer(pt);
    }

    /**
     * 😡 Fâché (inactivité extrême) : tremblement rapide + pulsation d'opacité
     */
    private void animerFache() {
        // Tremblement rapide horizontal
        TranslateTransition shake = new TranslateTransition(Duration.millis(80), imgAvatar);
        shake.setFromX(-5); shake.setToX(5);
        shake.setAutoReverse(true);
        shake.setCycleCount(Animation.INDEFINITE);

        // Pulsation de scale (tension)
        ScaleTransition tension = new ScaleTransition(Duration.millis(200), imgAvatar);
        tension.setFromX(1); tension.setToX(1.05);
        tension.setFromY(1); tension.setToY(1.05);
        tension.setAutoReverse(true);
        tension.setCycleCount(Animation.INDEFINITE);

        // Légère variation d'opacité (vapeur/frustration)
        FadeTransition vapeur = new FadeTransition(Duration.millis(400), imgAvatar);
        vapeur.setFromValue(1.0); vapeur.setToValue(0.8);
        vapeur.setAutoReverse(true);
        vapeur.setCycleCount(Animation.INDEFINITE);

        ParallelTransition pt = new ParallelTransition(shake, tension, vapeur);
        jouer(pt);
    }

    /**
     * ✨ Animation "Nourrir" : petit saut de joie quand on ajoute un dépôt
     */
    private void animerNourrir() {
        if (imgAvatar == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(200), imgAvatar);
        st.setFromX(1); st.setToX(1.4);
        st.setFromY(1); st.setToY(1.4);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play(); // animation one-shot, pas dans runningAnimations
    }

    /**
     * 🎊 Confettis : labels emoji animés qui tombent autour de l'avatar
     * Lancés uniquement quand l'objectif est atteint (100%)
     */
    private void lancerConfettis() {
        if (avatarPane == null) return;
        String[] confettiEmojis = {"🎉", "⭐", "✨", "🎊", "💫", "🌟"};
        Random rng = new Random();

        for (int i = 0; i < 8; i++) {
            Label confetti = new Label(confettiEmojis[rng.nextInt(confettiEmojis.length)]);
            confetti.setStyle("-fx-font-size: 14px; -fx-opacity: 0;");
            confetti.setTranslateX(rng.nextInt(80) - 40);
            confetti.setTranslateY(rng.nextInt(20) - 10);
            avatarPane.getChildren().add(confetti);

            // Apparition
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), confetti);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);

            // Chute
            TranslateTransition chute = new TranslateTransition(Duration.millis(1200), confetti);
            chute.setToY(confetti.getTranslateY() + 60 + rng.nextInt(40));

            // Disparition
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), confetti);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);

            SequentialTransition seq = new SequentialTransition(
                    new ParallelTransition(fadeIn, chute), fadeOut
            );
            int delay = rng.nextInt(600);
            seq.setDelay(Duration.millis(delay));

            Label finalConfetti = confetti;
            seq.setOnFinished(e -> avatarPane.getChildren().remove(finalConfetti));
            seq.play();
        }

        // Relancer les confettis toutes les 3 secondes tant que l'objectif est atteint
        Timeline relance = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (currentState != null && currentState.isObjectifAtteint()) {
                lancerConfettis();
            }
        }));
        relance.setCycleCount(3);
        relance.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gestion des animations en cours
    // ─────────────────────────────────────────────────────────────────────────

    /** Enregistre et lance une animation */
    private void jouer(Animation anim) {
        runningAnimations.add(anim);
        anim.play();
    }

    /** Stoppe toutes les animations en cours et réinitialise l'avatar */
    private void stopAllAnimations() {
        for (Animation anim : runningAnimations) {
            anim.stop();
        }
        runningAnimations.clear();

        if (imgAvatar != null) {
            imgAvatar.setTranslateX(0);
            imgAvatar.setTranslateY(0);
            imgAvatar.setRotate(0);
            imgAvatar.setOpacity(1);
            imgAvatar.setScaleX(1);
            imgAvatar.setScaleY(1);
        }
        if (lblDragonEmoji != null) {
            lblDragonEmoji.setTranslateX(0);
            lblDragonEmoji.setTranslateY(0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Actions boutons
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Clic sur le héros ou bouton "Parler" — logique à deux temps :
     *
     * ÉTAPE 1 (instantané) : afficher le message défini par le cahier des charges.
     * ÉTAPE 2 (asynchrone) : appeler l'API Groq pour enrichir/remplacer le message.
     *
     * ┌────────────┬──────────────────────────────────────────────┬──────────┐
     * │ État       │ Message instantané                           │ API Groq │
     * ├────────────┼──────────────────────────────────────────────┼──────────┤
     * │ 😢 Triste  │ "Pourquoi tu m'as abandonné ? Je suis si    │ ✅ OUI   │
     * │            │  triste…"                                    │ (explique│
     * │            │                                              │ pourquoi)│
     * ├────────────┼──────────────────────────────────────────────┼──────────┤
     * │ 😡 Fâché   │ "C'est vraiment nul ! Tu n'as rien fait     │ ❌ NON   │
     * │            │  depuis des mois !"                          │ (fixe)   │
     * ├────────────┼──────────────────────────────────────────────┼──────────┤
     * │ 💪 Content │ "Waouh tu progresses bien ! Continue !"     │ ✅ OUI   │
     * │  (60-99%)  │                                              │ (il reste│
     * │            │                                              │ X DT...) │
     * ├────────────┼──────────────────────────────────────────────┼──────────┤
     * │ 🎉 Victoire│ "BRAVO ! Tu es incroyable ! Objectif        │ ❌ NON   │
     * │  (100%)    │  atteint !"                                  │ (fixe)   │
     * ├────────────┼──────────────────────────────────────────────┼──────────┤
     * │ 🐣🌱 Autres│ Message état courant                        │ ✅ OUI   │
     * │            │                                              │ (normal) │
     * └────────────┴──────────────────────────────────────────────┴──────────┘
     */
    @FXML
    private void handleParler() {
        if (currentState == null) return;

        // ── Animation pulse sur l'avatar (tous les états) ─────────────────
        if (imgAvatar != null) {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), imgAvatar);
            st.setFromX(1); st.setToX(1.15);
            st.setFromY(1); st.setToY(1.15);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();
        }

        // ── Déterminer le contexte du clic ────────────────────────────────
        DragonService.ContexteClic contexte = detecterContexte(currentState);

        // ── ÉTAPE 1 : Afficher le message instantané selon l'état ─────────
        afficherMessageInstantane(contexte, currentState);

        // ── ÉTAPE 2 : Appeler l'API si nécessaire pour enrichir ───────────
        switch (contexte) {
            case FACHE:
            case VICTOIRE:
                // Messages fixes → pas d'appel API
                System.out.println("[Dragon] Message fixe, pas d'appel IA pour : " + contexte);
                break;

            case TRISTE:
                // L'IA explique pourquoi le héros est triste + conseille de reprendre
                safe(lblMessageDragon, () -> lblMessageDragon.setText(
                        lblMessageDragon.getText() + " ⏳ Draco réfléchit…"));
                dragonService.enrichirAvecIAContexte(currentState, contexte,
                        () -> updateMessages(currentState));
                break;

            case CONTENT:
                // L'IA commence par "Waouh tu progresses !" + explique ce qui reste
                safe(lblMessageDragon, () -> lblMessageDragon.setText(
                        lblMessageDragon.getText() + " ⏳ Draco calcule…"));
                dragonService.enrichirAvecIAContexte(currentState, contexte,
                        () -> updateMessages(currentState));
                break;

            case NORMAL:
            default:
                // Enrichissement IA standard
                dragonService.enrichirAvecIAContexte(currentState, contexte,
                        () -> updateMessages(currentState));
                break;
        }
    }

    /**
     * Détecte le contexte du clic à partir de l'état courant du héros.
     */
    /**
     * Contexte du clic — aligné avec choisirImage() et animerAvatar().
     *
     * Abandon 2 mois (BD) → FACHE
     * 100%               → VICTOIRE
     * 60–99%             → CONTENT
     * 26–59%             → NORMAL
     * 0–25%              → TRISTE
     */
    private DragonService.ContexteClic detecterContexte(DragonState s) {
        // Priorité 1 : abandon dépôt 2 mois (vérif BD)
        if (s.isAbandonDepot2Mois()) {
            return DragonService.ContexteClic.FACHE;
        }

        int pct = s.getProgressionPct();

        // Priorité 2 : 100% atteint
        if (pct >= 100) {
            return DragonService.ContexteClic.VICTOIRE;
        }

        // Priorité 3 : 60–99% presque atteint
        if (pct >= 60) {
            return DragonService.ContexteClic.CONTENT;
        }

        // Priorité 4 : 25–59% progression normale
        if (pct >= 25) {
            return DragonService.ContexteClic.NORMAL;
        }

        // Priorité 5 : 0–24% progression faible → message triste
        return DragonService.ContexteClic.TRISTE;
    }

    /**
     * Affiche immédiatement le message instantané selon le contexte.
     * Ces messages sont toujours visibles en premier, avant la réponse IA.
     */
    /**
     * Messages instantanés alignés avec la logique choisirImage() :
     *
     * FACHE   → abandon dépôt 2 mois (BD)
     * VICTOIRE→ 100% atteint
     * CONTENT → 60–99% presque atteint
     * NORMAL  → 26–59% progression normale
     * TRISTE  → 0–25% progression faible
     */
    private void afficherMessageInstantane(DragonService.ContexteClic contexte, DragonState s) {
        switch (contexte) {

            case FACHE:
                // 😡 Aucun dépôt depuis 2 mois — message fâché, pas d'appel IA
                safe(lblMessageDragon, () -> lblMessageDragon.setText(
                        "Ça fait 2 mois que tu n'as rien déposé ! Je suis tellement fâché ! 😡🔥"));
                safe(lblConseil, () -> lblConseil.setText(
                        "⚠️ Reprends vite tes dépôts ! Même 10 DT par semaine compte !"));
                break;

            case VICTOIRE:
                // 🎉 100% objectif atteint — message victoire, pas d'appel IA
                safe(lblMessageDragon, () -> lblMessageDragon.setText(
                        "BRAVO ! Tu es incroyable ! Objectif atteint à 100% ! 🎉🏆"));
                safe(lblConseil, () -> lblConseil.setText(
                        "🏆 Crée un nouveau coffre pour ton prochain grand objectif !"));
                break;

            case CONTENT:
                // 💪 60–99% — message content, l'IA enrichira avec les détails restants
                double restant = s.getObjectifMontant() - s.getMontantActuel();
                safe(lblMessageDragon, () -> lblMessageDragon.setText(
                        "Waouh tu progresses bien ! Continue comme ça ! 💪"));
                safe(lblConseil, () -> lblConseil.setText(
                        String.format("🎯 Il te reste seulement %.0f DT pour atteindre ton objectif !", restant)));
                break;

            case TRISTE:
                // 😢 0–25% progression faible — message triste, l'IA enrichira
                safe(lblMessageDragon, () -> lblMessageDragon.setText(
                        "Je suis tout petit et j'ai besoin de toi pour grandir… 😢"));
                safe(lblConseil, () -> lblConseil.setText(
                        "💬 Un petit dépôt régulier me fera tellement plaisir !"));
                break;

            case NORMAL:
            default:
                // 26–59% progression normale → l'IA enrichit le message
                break;
        }
    }

    @FXML
    private void handleNourrir() {
        if (nourirPanel == null) return;
        boolean show = !nourirPanel.isVisible();
        nourirPanel.setVisible(show);
        nourirPanel.setManaged(show);
    }

    @FXML
    private void handleConfirmerNourrir() {
        if (currentCoffre == null || currentCompte == null) return;
        double montant;
        try {
            montant = Double.parseDouble(txtMontantNourrir.getText().trim());
        } catch (Exception e) {
            montant = Math.min(50, currentState.getObjectifMontant() - currentState.getMontantActuel());
        }
        if (montant <= 0) return;

        double nouveau = Math.min(
                currentCoffre.getMontantActuel() + montant,
                currentCoffre.getObjectifMontant());
        currentCoffre.setMontantActuel(nouveau);

        if (nouveau >= currentCoffre.getObjectifMontant()) {
            currentCoffre.setStatus("Cloture");
        }
        coffreService.edit(currentCoffre);

        System.out.println("[Dragon] Nourri +" + montant + " DT → total: " + nouveau + " DT");

        animerNourrir();
        cacherNourirPanel();
        if (txtMontantNourrir != null) txtMontantNourrir.clear();
        updateDragon(currentCompte);
    }

    @FXML
    private void handleAnnulerNourrir() {
        cacherNourirPanel();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilitaires
    // ─────────────────────────────────────────────────────────────────────────

    private void cacherNourirPanel() {
        if (nourirPanel != null) {
            nourirPanel.setVisible(false);
            nourirPanel.setManaged(false);
        }
    }

    private CoffreVirtuel choisirCoffre(List<CoffreVirtuel> coffres) {
        if (coffres == null || coffres.isEmpty()) return null;
        return coffres.stream()
                .filter(c -> "Actif".equalsIgnoreCase(c.getStatus()))
                .findFirst()
                .orElse(coffres.get(0));
    }

    /** Exécute une action seulement si le nœud FXML n'est pas null */
    private void safe(Object node, Runnable action) {
        if (node != null) action.run();
    }

    /** Convertit un hex color (#RRGGBB) en rgba(r,g,b,alpha) pour les styles CSS JavaFX */
    private String toRgba(String hex, double alpha) {
        try {
            if (hex.startsWith("#") && hex.length() == 7) {
                int r = Integer.parseInt(hex.substring(1, 3), 16);
                int g = Integer.parseInt(hex.substring(3, 5), 16);
                int b = Integer.parseInt(hex.substring(5, 7), 16);
                return String.format("rgba(%d,%d,%d,%.2f)", r, g, b, alpha);
            }
        } catch (Exception ignored) {}
        return "rgba(99,102,241,0.3)";
    }
}