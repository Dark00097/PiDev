package com.nexora.bank.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  RoueFortuneEligibilityService
 *  Vérifie l'éligibilité d'un utilisateur à la Roue de la Fortune via Groq AI.
 *
 *  PIPELINE :
 *   1. Requêtes SQL directes → extraction des données du mois en cours
 *   2. Envoi à Groq LLaMA 3 → analyse intelligente des 3 conditions
 *   3. Retour du verdict + message personnalisé + suggestion corrective
 *
 *  STRUCTURE DES DONNÉES (base projetpidev) :
 *   • transactions(idTransaction, idCompte, idUser, categorie, dateTransaction,
 *                  montant, typeTransaction, statutTransaction, soldeApres, description)
 *   • compte(idCompte, numeroCompte, solde, typeCompte, idUser)
 *   • coffrevirtuel(idCoffre, nom, montantActuel, objectifMontant, idUser)
 * ══════════════════════════════════════════════════════════════════════════════
 */
public class RoueFortuneEligibilityService {

    private static final String GROQ_API_KEY = System.getenv("NEXORA_GROQ_API_KEY") != null
            ? System.getenv("NEXORA_GROQ_API_KEY")
            : (System.getenv("GROQ_API_KEY") != null ? System.getenv("GROQ_API_KEY") : "");
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL   = "llama-3.3-70b-versatile";

    // ── Résultat d'éligibilité ────────────────────────────────────────────────
    public static class EligibilityResult {
        public boolean eligible;
        public boolean conditionSolde;
        public boolean conditionBudget;
        public boolean conditionEpargne;
        public String  message;         // message principal (bravo / refus)
        public String  suggestion;      // conseil correctif si refus
        public String  solutionSolde;    // action immédiate pour débloquer condition solde
        public String  solutionBudget;   // action immédiate pour débloquer condition budget
        public String  solutionEpargne;  // action immédiate pour débloquer condition épargne
        public String  detail;           // détail JSON brut de l'analyse
        public int     conditionsOk;     // nombre de conditions validées
    }

    // ── Données financières extraites de la DB ────────────────────────────────
    public static class FinancialSnapshot {
        public int    userId;
        public String moisCourant;
        // Condition 1 — Solde
        public List<Double> soldesApres = new ArrayList<>();
        public boolean      soldeNegatifDetecte = false;
        // Condition 2 — Budget global
        public double totalDepensesLoisirs = 0;    // renommé pour compatibilité : total DEBIT du mois
        public double totalCredits         = 0;
        public double budgetMensuel        = 0;
        public double seuilLoisirs         = 0;    // 80% des revenus
        // Condition 3 — Épargne
        public boolean transactionEpargneExiste = false;
        public double  montantEpargne           = 0;
        // Infos complémentaires
        public double  soldeActuel = 0;
        public int     nbTransactions = 0;
    }

    /**
     * Point d'entrée principal.
     * Extrait les données DB, construit le contexte et interroge Groq.
     *
     * @param userId  ID de l'utilisateur connecté
     * @param conn    Connexion JDBC active
     * @return        EligibilityResult avec verdict + messages
     */
    public EligibilityResult checkEligibility(int userId, Connection conn) {
        try {
            // 1. Extraire les données financières du mois courant
            FinancialSnapshot snap = extractFinancialData(userId, conn);

            // 2. Analyser avec Groq AI
            return analyzeWithGroq(snap);

        } catch (Exception e) {
            System.err.println("[Roue] Erreur eligibilite: " + e.getMessage());
            // Fallback : analyse locale si Groq indisponible
            return fallbackLocalAnalysis(userId, conn);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EXTRACTION DES DONNÉES SQL
    // ══════════════════════════════════════════════════════════════════════════

    private FinancialSnapshot extractFinancialData(int userId, Connection conn) throws SQLException {
        FinancialSnapshot snap = new FinancialSnapshot();
        snap.userId = userId;

        // Mois courant au format YYYY-MM
        String moisCourant = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        snap.moisCourant = moisCourant;

        // ── Condition 1 : Vérification des soldes (soldeApres jamais négatif) ──
        String sqlSoldes = """
            SELECT soldeApres FROM transactions
            WHERE idUser = ? AND dateTransaction LIKE ?
            AND statutTransaction = 'VALIDED'
            ORDER BY dateTransaction ASC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sqlSoldes)) {
            ps.setInt(1, userId);
            ps.setString(2, moisCourant + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double s = rs.getDouble("soldeApres");
                snap.soldesApres.add(s);
                if (s < 0) snap.soldeNegatifDetecte = true;
            }
        }

        // ── Condition 2 : Budget global (somme TOUTES dépenses DEBIT du mois) ──
        String sqlLoisirs = """
            SELECT COALESCE(SUM(montant), 0) as total FROM transactions
            WHERE idUser = ? AND dateTransaction LIKE ?
            AND typeTransaction = 'DEBIT'
            AND statutTransaction = 'VALIDED'
            """;
        try (PreparedStatement ps = conn.prepareStatement(sqlLoisirs)) {
            ps.setInt(1, userId);
            ps.setString(2, moisCourant + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) snap.totalDepensesLoisirs = rs.getDouble("total");
        }

        // Total crédits du mois = revenu mensuel estimé
        String sqlBudget = """
            SELECT COALESCE(SUM(montant), 0) as totalCredit FROM transactions
            WHERE idUser = ? AND dateTransaction LIKE ?
            AND typeTransaction = 'CREDIT'
            AND statutTransaction = 'VALIDED'
            """;
        try (PreparedStatement ps = conn.prepareStatement(sqlBudget)) {
            ps.setInt(1, userId);
            ps.setString(2, moisCourant + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) snap.totalCredits = rs.getDouble("totalCredit");
        }
        snap.budgetMensuel = snap.totalCredits;
        snap.seuilLoisirs  = snap.budgetMensuel * 0.80; // Dépenses ≤ 80% des revenus

        // ── Condition 3 : Épargne (DEBIT vers Coffre ou catégorie Epargne) ──────
        String sqlEpargne = """
            SELECT COALESCE(SUM(montant), 0) as totalEpargne, COUNT(*) as nb
            FROM transactions
            WHERE idUser = ? AND dateTransaction LIKE ?
            AND typeTransaction = 'DEBIT'
            AND (LOWER(categorie) = 'epargne'
                 OR LOWER(description) LIKE '%coffre%'
                 OR LOWER(description) LIKE '%epargne%'
                 OR LOWER(description) LIKE '%virement coffre%')
            AND statutTransaction = 'VALIDED'
            """;
        try (PreparedStatement ps = conn.prepareStatement(sqlEpargne)) {
            ps.setInt(1, userId);
            ps.setString(2, moisCourant + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                snap.montantEpargne = rs.getDouble("totalEpargne");
                snap.transactionEpargneExiste = rs.getInt("nb") > 0;
            }
        }

        // ── Solde actuel du compte principal ─────────────────────────────────────
        String sqlSolde = """
            SELECT COALESCE(SUM(solde), 0) as totalSolde FROM compte
            WHERE idUser = ? AND statutCompte = 'Actif'
            """;
        try (PreparedStatement ps = conn.prepareStatement(sqlSolde)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) snap.soldeActuel = rs.getDouble("totalSolde");
        }

        snap.nbTransactions = snap.soldesApres.size();
        return snap;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ANALYSE VIA GROQ AI
    // ══════════════════════════════════════════════════════════════════════════

    private EligibilityResult analyzeWithGroq(FinancialSnapshot snap) throws Exception {
        String systemPrompt = """
            Tu es un conseiller financier IA pour NEXORA BANK.
            On te donne les données financières réelles d'un client pour le mois en cours.
            
            TU DOIS faire 2 choses pour chaque condition échouée :
            1. JUSTIFICATION : expliquer POURQUOI la condition échoue (avec les chiffres exacts du client)
            2. SOLUTION : dire EXACTEMENT quoi faire avec le montant précis à effectuer (calculé depuis ses données)
            
            LES 3 CONDITIONS :
            - conditionSolde   : aucun soldeApres négatif ce mois
            - conditionBudget  : total dépenses (DEBIT) ≤ 80% des revenus mensuels (CREDIT)
            - conditionEpargne : au moins 1 virement vers Coffre/Épargne ce mois
            
            RÈGLE ÉLIGIBILITÉ : les 3 conditions sont TOUTES obligatoires (calculé par le système).
            
            FORMAT JSON STRICT — réponds UNIQUEMENT avec ce JSON, rien d'autre :
            {
              "conditionSolde": true ou false,
              "conditionBudget": true ou false,
              "conditionEpargne": true ou false,
              "eligible": true ou false,
              "message": "phrase courte résumant la situation globale avec emoji (max 100 chars)",
              "suggestion": "résumé en 1 phrase de ce qu'il faut faire (max 150 chars), vide si eligible=true",
              "solutionSolde": "si conditionSolde=false → écris UNE phrase avec : le solde min exact atteint en DT ET le montant exact à recharger. Si true → chaîne vide",
              "solutionBudget": "si conditionBudget=false → écris UNE phrase avec : total dépenses exact en DT, seuil autorisé (80% revenus) en DT, montant exact du dépassement. Si true → chaîne vide",
              "solutionEpargne": "si conditionEpargne=false → écris UNE phrase avec : montant épargné actuel (0 DT), et le montant exact à virer = MAX(10, revenus*0.02) arrondi. Si true → chaîne vide"
            }
            
            INTERDIT : ne jamais inventer de chiffres. Utilise uniquement les données du message utilisateur.
            """;

        String userPrompt = String.format("""
            DONNÉES FINANCIÈRES DU CLIENT — MOIS %s :
            
            SOLDE :
            - Tous les soldeApres ce mois : %s
            - Solde minimum atteint : %.2f DT
            - Solde négatif détecté : %s
            - Montant à recharger si négatif : %.2f DT
            
            BUDGET GLOBAL :
            - Total dépenses (DEBIT) ce mois : %.2f DT
            - Revenus totaux (crédits) ce mois : %.2f DT
            - Seuil autorisé (80%% des revenus) : %.2f DT
            - Dépassement du seuil : %.2f DT
            
            ÉPARGNE :
            - Transaction vers coffre/épargne ce mois : %s
            - Montant épargné : %.2f DT
            - Montant suggéré à épargner (2%% revenus, min 10) : %.2f DT
            
            RÉSUMÉ CONDITIONS :
            - conditionSolde  → %s
            - conditionBudget → %s
            - conditionEpargne → %s
            - Nombre de transactions ce mois : %d
            """,
                snap.moisCourant,
                snap.soldesApres.toString(),
                snap.soldesApres.stream().mapToDouble(Double::doubleValue).min().orElse(0.0),
                snap.soldeNegatifDetecte ? "OUI" : "NON",
                snap.soldesApres.stream().mapToDouble(Double::doubleValue).min().orElse(0.0) < 0
                        ? Math.abs(snap.soldesApres.stream().mapToDouble(Double::doubleValue).min().orElse(0.0)) : 0.0,
                snap.totalDepensesLoisirs,
                snap.budgetMensuel,
                snap.seuilLoisirs,
                Math.max(0.0, snap.totalDepensesLoisirs - snap.seuilLoisirs),
                snap.transactionEpargneExiste ? "OUI" : "NON",
                snap.montantEpargne,
                Math.max(10.0, snap.budgetMensuel * 0.02),
                !snap.soldeNegatifDetecte ? "VALIDE" : "ECHOUE",
                (snap.budgetMensuel > 0
                        ? snap.totalDepensesLoisirs <= snap.seuilLoisirs
                        : snap.totalDepensesLoisirs == 0) ? "VALIDE" : "ECHOUE",
                snap.transactionEpargneExiste ? "VALIDE" : "ECHOUE",
                snap.nbTransactions
        );

        // Construire la requête Groq
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", systemPrompt);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);

        JsonArray messages = new JsonArray();
        messages.add(sysMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.add("messages", messages);
        body.addProperty("model", GROQ_MODEL);
        body.addProperty("temperature", 0.1);   // quasi-déterministe = solutions précises et cohérentes
        body.addProperty("max_tokens", 1200);   // suffisant pour JSON complet avec 3 solutions détaillées

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> resp = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofString());

        System.out.println("[Roue] Groq HTTP: " + resp.statusCode());
        System.out.println("[Roue] Groq RAW response: " + resp.body());
        if (resp.statusCode() != 200)
            throw new Exception("Groq HTTP " + resp.statusCode());

        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
        String content = root.getAsJsonArray("choices").get(0)
                .getAsJsonObject().getAsJsonObject("message")
                .get("content").getAsString().trim();

        // Nettoyer backticks
        if (content.startsWith("```")) {
            int nl = content.indexOf('\n');
            if (nl >= 0) content = content.substring(nl + 1);
            if (content.endsWith("```")) content = content.substring(0, content.lastIndexOf("```")).trim();
        }

        JsonObject parsed = JsonParser.parseString(content).getAsJsonObject();

        EligibilityResult result = new EligibilityResult();
        result.conditionSolde   = parsed.has("conditionSolde")   && parsed.get("conditionSolde").getAsBoolean();
        result.conditionBudget  = parsed.has("conditionBudget")  && parsed.get("conditionBudget").getAsBoolean();
        result.conditionEpargne = parsed.has("conditionEpargne") && parsed.get("conditionEpargne").getAsBoolean();
        result.message          = parsed.has("message")          ? parsed.get("message").getAsString()          : "";
        result.suggestion       = parsed.has("suggestion")       ? parsed.get("suggestion").getAsString()       : "";
        result.solutionSolde    = parsed.has("solutionSolde")    ? parsed.get("solutionSolde").getAsString()    : "";
        result.solutionBudget   = parsed.has("solutionBudget")   ? parsed.get("solutionBudget").getAsString()   : "";
        result.solutionEpargne  = parsed.has("solutionEpargne")  ? parsed.get("solutionEpargne").getAsString()  : "";
        result.detail           = content;

        // Java recalcule eligible lui-même — les 3 conditions sont TOUTES obligatoires
        result.conditionsOk = (result.conditionSolde ? 1 : 0)
                + (result.conditionBudget ? 1 : 0)
                + (result.conditionEpargne ? 1 : 0);
        result.eligible = result.conditionsOk == 3;  // 3/3 obligatoire

        System.out.println("[Roue] Résultat Groq — eligible=" + result.eligible
                + " | conditions=" + result.conditionsOk + "/3"
                + " | " + result.message);
        System.out.println("[Roue] Solutions extraites :"
                + "\n  solde   = '" + result.solutionSolde + "'"
                + "\n  budget  = '" + result.solutionBudget + "'"
                + "\n  epargne = '" + result.solutionEpargne + "'");
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FALLBACK LOCAL (si Groq indisponible)
    // ══════════════════════════════════════════════════════════════════════════

    private EligibilityResult fallbackLocalAnalysis(int userId, Connection conn) {
        EligibilityResult result = new EligibilityResult();
        try {
            FinancialSnapshot snap = extractFinancialData(userId, conn);

            result.conditionSolde   = !snap.soldeNegatifDetecte;
            result.conditionBudget  = snap.budgetMensuel > 0
                    ? snap.totalDepensesLoisirs <= snap.seuilLoisirs
                    : snap.totalDepensesLoisirs == 0;  // Pas de revenu = valide seulement si 0 dépenses
            result.conditionEpargne = snap.transactionEpargneExiste;
            result.conditionsOk     = (result.conditionSolde ? 1 : 0)
                    + (result.conditionBudget ? 1 : 0)
                    + (result.conditionEpargne ? 1 : 0);
            result.eligible = result.conditionsOk == 3;  // 3/3 obligatoire

            if (result.eligible) {
                result.message    = "🎉 Félicitations ! Tu remplis les conditions. Tourne la roue !";
                result.suggestion = "";
                result.solutionSolde = "";
                result.solutionBudget = "";
                result.solutionEpargne = "";
            } else {
                StringBuilder sb = new StringBuilder("❌ ");
                if (!result.conditionSolde)   sb.append("Solde négatif détecté. ");
                if (!result.conditionBudget)  sb.append("Budget global dépassé (> 80% revenus). ");
                if (!result.conditionEpargne) sb.append("Aucune épargne ce mois. ");
                result.message = sb.toString().trim();

                // Solutions avec montants exacts (fallback local)
                if (!result.conditionSolde) {
                    double soldeMin = snap.soldesApres.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                    double montantRecharge = soldeMin < 0 ? Math.abs(soldeMin) : 0;
                    result.solutionSolde = String.format(
                            "💰 Solde descendu à %.2f DT. Recharge ton compte de %.2f DT minimum pour corriger.", soldeMin, montantRecharge);
                }
                if (!result.conditionBudget) {
                    double depassement = snap.totalDepensesLoisirs - snap.seuilLoisirs;
                    result.solutionBudget = String.format(
                            "💳 Dépenses totales : %.2f DT, seuil autorisé (80%% revenus) = %.2f DT. Réduis tes dépenses de %.2f DT ce mois.",
                            snap.totalDepensesLoisirs, snap.seuilLoisirs, depassement);
                }
                if (!result.conditionEpargne) {
                    double montantSuggest = Math.max(10.0, snap.budgetMensuel * 0.02);
                    result.solutionEpargne = String.format(
                            "🏦 0 DT épargnés ce mois. Vire %.2f DT (2%% de tes revenus) vers ton Coffre Virtuel — la roue s'ouvre automatiquement !", montantSuggest);
                }

                result.suggestion = "⚠️ Analyse IA indisponible. Effectue les actions ci-dessus et clique Re-vérifier.";
            }
        } catch (Exception e) {
            result.eligible   = false;
            result.message    = "Impossible de vérifier l'éligibilité. Réessaie plus tard.";
            result.suggestion = "";
        }
        return result;
    }
}
