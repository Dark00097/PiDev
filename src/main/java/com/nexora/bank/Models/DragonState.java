package com.nexora.bank.Models;

public class DragonState {

    public enum Niveau {
        BEBE       ("🐣", "Bébé Dragon",       0,  25),
        PETIT      ("🐉", "Petit Dragon",      26,  50),
        ADOLESCENT ("🐲", "Dragon Adolescent", 51,  75),
        ADULTE     ("🔥", "Dragon Adulte",     76,  99),
        LEGENDAIRE ("👑", "Dragon Légendaire", 100, 100);

        public final String emoji;
        public final String label;
        public final int    minPct;
        public final int    maxPct;

        Niveau(String emoji, String label, int minPct, int maxPct) {
            this.emoji  = emoji;  this.label  = label;
            this.minPct = minPct; this.maxPct = maxPct;
        }
    }

    public enum Humeur {
        HEUREUX ("😄", "Heureux", "#22C55E"),
        CONTENT ("🙂", "Content", "#84CC16"),
        FATIGUE ("😓", "Fatigué", "#F59E0B"),
        TRISTE  ("😢", "Triste",  "#F97316"),
        CACHE   ("🙈", "Caché",   "#6B7280");

        public final String emoji;
        public final String label;
        public final String color;

        Humeur(String emoji, String label, String color) {
            this.emoji = emoji; this.label = label; this.color = color;
        }
    }

    /** 4 états visuels Tamagotchi : couleur, écran (visage), antenne, messages par défaut. */
    public enum TamagotchiEtat {
        STABLE           ("#7DD3FC", "😊", "#22C55E", "Bonjour 👋 ! Ta situation est stable. Continue comme ça !", "💡 Tes dépenses sont équilibrées cette semaine."),
        DEPENSES_ELEVEES ("#FCD34D", "😐", "#F59E0B", "Hmm… Tes dépenses ont augmenté. On peut améliorer ça ensemble.", "📊 Réduis tes dépenses pour retrouver l'équilibre."),
        PROCHE_DECOUVERT ("#9CA3AF", "😟", "#EF4444", "Attention ⚠️ Ton solde diminue. Transférer vers ton coffre renforcerait ta sécurité.", "💬 Astuce : Mettre de côté avant de dépenser."),
        OBJECTIF_ATTEINT ("#3B82F6", "😄", "#F59E0B", "Objectif atteint ! Je passe au niveau supérieur !", "🏆 Crée un nouveau coffre pour continuer.");

        public final String couleurPrincipale;
        public final String ecran;       // visage
        public final String antenneHex;
        public final String messageDefaut;
        public final String conseilDefaut;

        TamagotchiEtat(String couleurPrincipale, String ecran, String antenneHex, String messageDefaut, String conseilDefaut) {
            this.couleurPrincipale = couleurPrincipale;
            this.ecran = ecran;
            this.antenneHex = antenneHex;
            this.messageDefaut = messageDefaut;
            this.conseilDefaut = conseilDefaut;
        }
    }

    /** Détermine l'état Tamagotchi à afficher à partir de la situation. */
    public static TamagotchiEtat calculerTamagotchiEtat(DragonState s) {
        if (s == null) return TamagotchiEtat.STABLE;
        if (s.getProgressionPct() >= 100) return TamagotchiEtat.OBJECTIF_ATTEINT;
        if (s.getHumeur() == Humeur.TRISTE || s.getHumeur() == Humeur.CACHE || s.getSoldeCompte() <= 50)
            return TamagotchiEtat.PROCHE_DECOUVERT;
        if (s.getHumeur() == Humeur.FATIGUE || (s.getObjectifMontant() > 0 && s.getDepensesRecentes() > s.getSoldeCompte() * 0.5))
            return TamagotchiEtat.DEPENSES_ELEVEES;
        return TamagotchiEtat.STABLE;
    }

    private Niveau niveau;
    private Humeur humeur;
    private int    progressionPct;
    private double montantActuel;
    private double objectifMontant;
    private double soldeCompte;
    private double depensesRecentes;
    private String nomCoffre;
    private String statutCompte;
    private String messageIA;
    private String conseil;
    /** true si objectif du coffre atteint (100%). */
    private boolean objectifAtteint;
    /** true si aucun dépôt depuis 2 mois. */
    private boolean abandonDepot2Mois;

    public static Niveau calculerNiveau(int pct) {
        if (pct >= 100) return Niveau.LEGENDAIRE;
        if (pct >= 76)  return Niveau.ADULTE;
        if (pct >= 51)  return Niveau.ADOLESCENT;
        if (pct >= 26)  return Niveau.PETIT;
        return Niveau.BEBE;
    }

    public static Humeur calculerHumeur(double solde, double depenses, String statut) {
        if ("Bloque".equalsIgnoreCase(statut) || "Cloture".equalsIgnoreCase(statut)) return Humeur.CACHE;
        if (solde <= 0)                    return Humeur.TRISTE;
        if (depenses > solde * 0.8)        return Humeur.FATIGUE;
        if (depenses > solde * 0.5)        return Humeur.CONTENT;
        return Humeur.HEUREUX;
    }

    public Niveau getNiveau()               { return niveau; }
    public void   setNiveau(Niveau n)       { this.niveau = n; }
    public Humeur getHumeur()               { return humeur; }
    public void   setHumeur(Humeur h)       { this.humeur = h; }
    public int    getProgressionPct()       { return progressionPct; }
    public void   setProgressionPct(int p)  { this.progressionPct = p; }
    public double getMontantActuel()        { return montantActuel; }
    public void   setMontantActuel(double m){ this.montantActuel = m; }
    public double getObjectifMontant()          { return objectifMontant; }
    public void   setObjectifMontant(double o)  { this.objectifMontant = o; }
    public double getSoldeCompte()          { return soldeCompte; }
    public void   setSoldeCompte(double s)  { this.soldeCompte = s; }
    public double getDepensesRecentes()         { return depensesRecentes; }
    public void   setDepensesRecentes(double d) { this.depensesRecentes = d; }
    public String getNomCoffre()            { return nomCoffre; }
    public void   setNomCoffre(String n)    { this.nomCoffre = n; }
    public String getStatutCompte()             { return statutCompte; }
    public void   setStatutCompte(String s)     { this.statutCompte = s; }
    public String getMessageIA()            { return messageIA; }
    public void   setMessageIA(String m)    { this.messageIA = m; }
    public String getConseil()              { return conseil; }
    public void   setConseil(String c)      { this.conseil = c; }
    public boolean isObjectifAtteint()      { return objectifAtteint; }
    public void   setObjectifAtteint(boolean b) { this.objectifAtteint = b; }
    public boolean isAbandonDepot2Mois()    { return abandonDepot2Mois; }
    public void   setAbandonDepot2Mois(boolean b) { this.abandonDepot2Mois = b; }

    /** Ton de parole selon l'âge du dragon (pour l'API). */
    public enum TonParole {
        BEBE   ("enfantin et innocent", "Tu es un bébé dragon. Parle avec des mots simples, mignon et innocent."),
        JEUNE  ("mature et encourageant", "Tu es un jeune dragon. Parle avec un ton mature, encourageant et positif."),
        ADULTE ("sagesse et motivation", "Tu es un dragon adulte ou légendaire. Parle comme un mentor financier : confiant, sage et motivant.");
        public final String description;
        public final String instruction;
        TonParole(String d, String i) { description = d; instruction = i; }
    }

    public static TonParole getTonParole(Niveau n) {
        if (n == null) return TonParole.BEBE;
        switch (n) {
            case LEGENDAIRE: case ADULTE: return TonParole.ADULTE;
            case ADOLESCENT: case PETIT:  return TonParole.JEUNE;
            default: return TonParole.BEBE;
        }
    }
}