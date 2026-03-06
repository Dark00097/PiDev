package com.nexora.bank.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nexora.bank.Models.CompteBancaire;
import com.nexora.bank.Models.CoffreVirtuel;
import com.nexora.bank.Models.DragonState;
import com.nexora.bank.Utils.MyDB;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

public class DragonService {

    private static final String GROQ_API_KEY = System.getenv("NEXORA_GROQ_API_KEY") != null
            ? System.getenv("NEXORA_GROQ_API_KEY")
            : (System.getenv("GROQ_API_KEY") != null ? System.getenv("GROQ_API_KEY") : "");
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL   = "llama-3.3-70b-versatile";

    /**
     * Contexte du clic — détermine quel type de message l'IA doit générer.
     * Passé depuis DragonController lors du handleParler().
     */
    public enum ContexteClic {
        TRISTE,   // 😢 Hero triste → IA explique pourquoi il est triste + conseille
        FACHE,    // 😡 Hero fâché  → message fixe, pas d'appel IA
        CONTENT,  // 💪 Hero content → IA explique ce qu'il reste à faire
        VICTOIRE, // 🎉 Objectif atteint → message fixe, pas d'appel IA
        NORMAL    // Tout autre état → appel IA standard
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Build état métier
    // ─────────────────────────────────────────────────────────────────────────
    public DragonState buildDragonState(CompteBancaire compte, List<CoffreVirtuel> coffres, int idUser) {
        DragonState state = new DragonState();

        CoffreVirtuel coffre = choisirCoffrePrincipal(coffres);
        double montantActuel   = coffre != null ? coffre.getMontantActuel()   : 0;
        double objectifMontant = coffre != null && coffre.getObjectifMontant() > 0
                ? coffre.getObjectifMontant() : 1;
        String nomCoffre = coffre != null ? coffre.getNom() : "Aucun coffre";
        int pct = (int) Math.min(100, Math.round((montantActuel / objectifMontant) * 100));

        double depenses = getDepensesRecentes(idUser);

        state.setNiveau(DragonState.calculerNiveau(pct));
        state.setHumeur(DragonState.calculerHumeur(compte.getSolde(), depenses, compte.getStatutCompte()));
        state.setProgressionPct(pct);
        state.setMontantActuel(montantActuel);
        state.setObjectifMontant(objectifMontant);
        state.setSoldeCompte(compte.getSolde());
        state.setDepensesRecentes(depenses);
        state.setNomCoffre(nomCoffre);
        state.setStatutCompte(compte.getStatutCompte());
        state.setObjectifAtteint(pct >= 100);

        // ── Vérification abandon dépôt depuis 2 mois ────────────────────────────
        // La table coffrevirtuel n'a PAS de colonne dateDernierDepot.
        // On cherche dans les transactions : dernier "Virement Coffre Virtuel"
        // lié au compte du coffre, pour cet idUser.
        // Si aucune transaction de dépôt coffre depuis >= 2 mois → hero_fache
        boolean abandon2Mois = false;
        if (coffre != null) {
            abandon2Mois = verifierAbandonDepot2Mois(coffre, idUser);
        }
        state.setAbandonDepot2Mois(abandon2Mois);
        System.out.println("[Dragon] AbandonDepot2Mois=" + abandon2Mois
                + " | coffre=" + (coffre != null ? coffre.getNom() : "null")
                + " | pct=" + pct + "%");

        // ── CORRECTIF : Forcer l'humeur à TRISTE quand abandon ≥ 2 mois ────────
        // Sans ce correctif, calculerHumeur() peut retourner HEUREUX (solde élevé)
        // alors que le héros doit afficher l'état FÂCHÉ (hero_fache.png).
        // Le label humeur et le glow de l'avatar seront alors cohérents avec l'image.
        if (abandon2Mois) {
            state.setHumeur(DragonState.Humeur.TRISTE);
        }

        // ── Messages par défaut (priorité = abandon > pct) ─────────────────────
        if (abandon2Mois) {
            state.setMessageIA("Ça fait 2 mois que tu n'as rien déposé ! Je suis tellement fâché ! 😡🔥");
            state.setConseil("⚠️ Reprends vite tes dépôts ! Même 10 DT par semaine compte !");
        } else if (pct >= 100) {
            state.setMessageIA("BRAVO ! Tu es incroyable ! Objectif atteint à 100% ! 🎉🏆");
            state.setConseil("🏆 Crée un nouveau coffre pour ton prochain objectif.");
        } else if (pct >= 60) {
            double restant = objectifMontant - montantActuel;
            state.setMessageIA(String.format(Locale.US,
                    "Waouh tu progresses bien ! Il te reste seulement %.0f DT ! 💪", restant));
            state.setConseil(String.format(Locale.US,
                    "🎯 Continue ! %.0f DT restants pour atteindre ton objectif !", restant));
        } else if (pct >= 25) {
            state.setMessageIA("Je grandis grâce à toi ! Continuons ensemble ! 🌱");
            state.setConseil("📈 Un dépôt régulier accélère ta progression !");
        } else {
            state.setMessageIA("Je suis tout petit et j'ai besoin de toi pour grandir… 😢");
            state.setConseil("💬 Un petit dépôt régulier me fera tellement plaisir !");
        }
        return state;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Enrichissement IA standard (chargement initial)
    // ─────────────────────────────────────────────────────────────────────────
    public void enrichirAvecIA(DragonState state, Runnable onDone) {
        enrichirAvecIAContexte(state, ContexteClic.NORMAL, onDone);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Enrichissement IA avec contexte du clic
    // Appelé depuis DragonController.handleParler() avec le bon contexte
    // ─────────────────────────────────────────────────────────────────────────
    public void enrichirAvecIAContexte(DragonState state, ContexteClic contexte, Runnable onDone) {
        new Thread(() -> {
            try {
                // Construire le system prompt selon le contexte
                String systemPrompt = buildSystemPrompt(contexte);
                String userPrompt   = buildPromptContexte(state, contexte);

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
                body.addProperty("stream", false);
                body.addProperty("temperature", 0.85);
                body.addProperty("max_tokens", 300);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(GROQ_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + GROQ_API_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .build();

                HttpResponse<String> resp = HttpClient.newHttpClient()
                        .send(req, HttpResponse.BodyHandlers.ofString());

                System.out.println("[Dragon] Groq HTTP " + contexte + ": " + resp.statusCode());

                if (resp.statusCode() == 200) {
                    JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
                    JsonArray choices = root.getAsJsonArray("choices");
                    if (choices != null && choices.size() > 0) {
                        String content = choices.get(0).getAsJsonObject()
                                .getAsJsonObject("message")
                                .get("content").getAsString().trim()
                                .replaceAll("```json", "").replaceAll("```", "").trim();
                        JsonObject parsed = JsonParser.parseString(content).getAsJsonObject();
                        if (parsed.has("message")) state.setMessageIA(parsed.get("message").getAsString());
                        if (parsed.has("conseil"))  state.setConseil(parsed.get("conseil").getAsString());
                        System.out.println("[Dragon] IA [" + contexte + "]: " + state.getMessageIA());
                    }
                }
            } catch (Exception e) {
                System.err.println("[Dragon] Erreur Groq [" + contexte + "]: " + e.getMessage());
            }
            if (onDone != null) javafx.application.Platform.runLater(onDone);
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // System prompt selon le contexte du clic
    // ─────────────────────────────────────────────────────────────────────────
    private String buildSystemPrompt(ContexteClic contexte) {
        String base = "Tu es Draco, un dragon compagnon financier. Tu parles en français, 2-3 phrases max. "
                + "Réponds UNIQUEMENT en JSON valide sans backticks : {\"message\":\"...\",\"conseil\":\"...\"}. "
                + "conseil = un court conseil pratique avec emoji. ";

        switch (contexte) {
            case TRISTE:
                // 😢 L'IA doit motiver l'utilisateur qui a une faible progression (0-24%)
                // et l'encourager à faire un premier/prochain dépôt
                return base
                        + "CONTEXTE TRISTE : L'utilisateur a une très faible progression (0-24%). "
                        + "Commence par un message triste et innocent du type 'Je suis tout petit et j'ai besoin de toi pour grandir…' "
                        + "puis EXPLIQUE en 1 phrase pourquoi c'est important de commencer à épargner maintenant. "
                        + "Ton : émotionnel, sincèrement déçu mais bienveillant. Adapte selon l'âge du dragon.";

            case CONTENT:
                // 💪 L'IA commence par "Waouh tu progresses bien ! Continue !"
                // puis explique ce qu'il reste à faire (montant restant, encouragement)
                return base
                        + "CONTEXTE CONTENT : L'utilisateur progresse bien (entre 60% et 99%). "
                        + "Commence OBLIGATOIREMENT par 'Waouh tu progresses bien ! Continue !' "
                        + "puis ajoute EN MÊME PHRASE ou phrase suivante une info concrète sur ce qu'il reste "
                        + "(ex: 'Il te reste seulement X DT pour atteindre ton objectif !'). "
                        + "Ton : enthousiaste, motivant. Adapte selon l'âge du dragon.";

            case NORMAL:
            default:
                return base
                        + "Adapte ton TON à l'âge du dragon : "
                        + "BÉBÉ = enfantin, innocent, mots simples ; "
                        + "JEUNE = mature, encourageant, positif ; "
                        + "ADULTE/LÉGENDAIRE = confiant, sage, mentor financier, motivant.";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User prompt enrichi selon le contexte
    // ─────────────────────────────────────────────────────────────────────────
    private String buildPromptContexte(DragonState s, ContexteClic contexte) {
        double restant = s.getObjectifMontant() - s.getMontantActuel();

        String base = String.format(Locale.US,
                "Âge du dragon : %s. Ton : %s. ",
                s.getNiveau().label, DragonState.getTonParole(s.getNiveau()).instruction)
                + String.format(Locale.US,
                "Coffre '%s' à %d%% — montant actuel %.0f DT / objectif %.0f DT (il reste %.0f DT). "
                        + "Solde compte : %.0f DT. Dépenses ce mois : %.0f DT. Statut : '%s'.",
                s.getNomCoffre(), s.getProgressionPct(),
                s.getMontantActuel(), s.getObjectifMontant(), restant,
                s.getSoldeCompte(), s.getDepensesRecentes(), s.getStatutCompte());

        switch (contexte) {
            case TRISTE:
                return base + String.format(Locale.US,
                        " Aucun dépôt depuis 2 mois. "
                                + "Génère un message triste qui explique que l'objectif est compromis à cause de l'inactivité, "
                                + "et un conseil pour reprendre les dépôts.");

            case CONTENT:
                return base + String.format(Locale.US,
                        " L'utilisateur progresse bien. "
                                + "Génère un message qui commence par 'Waouh tu progresses bien ! Continue !' "
                                + "et mentionne qu'il reste %.0f DT pour atteindre l'objectif. "
                                + "Conseil : comment atteindre l'objectif plus vite.", restant);

            default:
                return base + " Génère un message et un conseil adaptés à la situation.";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ancien buildPrompt (gardé pour compatibilité)
    // ─────────────────────────────────────────────────────────────────────────
    private String buildPrompt(DragonState s) {
        return buildPromptContexte(s, ContexteClic.NORMAL);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilitaires privés
    // ─────────────────────────────────────────────────────────────────────────
    private CoffreVirtuel choisirCoffrePrincipal(List<CoffreVirtuel> coffres) {
        if (coffres == null || coffres.isEmpty()) return null;
        // Prend le premier coffre "Actif" (même logique que DragonController.choisirCoffre)
        return coffres.stream()
                .filter(c -> "Actif".equalsIgnoreCase(c.getStatus()))
                .findFirst()
                .orElse(coffres.get(0));
    }

    /**
     * Vérifie si aucun dépôt n'a été fait dans ce coffre depuis >= 2 mois.
     *
     * Stratégie : cherche dans la table transactions la dernière transaction
     * de type "Virement Coffre Virtuel" liée au compte du coffre, pour cet user.
     * Si la date est >= 2 mois dans le passé (ou aucune transaction) → true.
     *
     * @param coffre  le coffre à vérifier
     * @param idUser  l'utilisateur connecté
     * @return true si aucun dépôt depuis 2 mois
     */
    private boolean verifierAbandonDepot2Mois(CoffreVirtuel coffre, int idUser) {
        try {
            Connection conn = MyDB.getInstance().getConn();

            // Cherche la dernière transaction de dépôt vers ce coffre
            // Description = "Virement Coffre Virtuel" (utilisé dans toute l'app)
            // On filtre par idCompte du coffre ET idUser
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT MAX(STR_TO_DATE(dateTransaction, '%Y-%m-%d')) AS dernierDepot " +
                            "FROM transactions " +
                            "WHERE idUser = ? " +
                            "  AND idCompte = ? " +
                            "  AND typeTransaction = 'DEBIT' " +
                            "  AND description LIKE '%Coffre%'"
            );
            ps.setInt(1, idUser);
            ps.setInt(2, coffre.getIdCompte());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                java.sql.Date dernierDepot = rs.getDate("dernierDepot");
                if (dernierDepot != null) {
                    LocalDate dateDepot = dernierDepot.toLocalDate();
                    long moisEcoules = ChronoUnit.MONTHS.between(dateDepot, LocalDate.now());
                    System.out.println("[Dragon] Dernier dépôt coffre '" + coffre.getNom()
                            + "' : " + dateDepot + " → " + moisEcoules + " mois écoulés");
                    rs.close(); ps.close();
                    return moisEcoules >= 2;
                }
            }
            rs.close(); ps.close();

            // Aucune transaction de dépôt trouvée → utiliser dateCreation comme fallback
            String dateCreation = coffre.getDateCreation();
            if (dateCreation != null && !dateCreation.isEmpty()) {
                try {
                    LocalDate creation = LocalDate.parse(dateCreation, DateTimeFormatter.ISO_DATE);
                    long moisDepuisCreation = ChronoUnit.MONTHS.between(creation, LocalDate.now());
                    System.out.println("[Dragon] Aucun dépôt trouvé pour '" + coffre.getNom()
                            + "' — dateCreation=" + dateCreation + " → " + moisDepuisCreation + " mois");
                    return moisDepuisCreation >= 2;
                } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            System.err.println("[Dragon] SQL verifierAbandonDepot2Mois: " + e.getMessage());
        }
        return false;
    }

    private double getDepensesRecentes(int idUser) {
        try {
            Connection conn = MyDB.getInstance().getConn();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(montant),0) FROM transactions "
                            + "WHERE idUser=? AND typeTransaction='DEBIT' "
                            + "AND STR_TO_DATE(dateTransaction,'%Y-%m-%d') >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH)");
            ps.setInt(1, idUser);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
            rs.close(); ps.close();
        } catch (Exception e) {
            System.err.println("[Dragon] SQL depenses: " + e.getMessage());
        }
        return 0;
    }
}
