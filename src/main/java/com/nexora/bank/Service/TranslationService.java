package com.nexora.bank.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de traduction multilingue utilisant l'API Groq (LLaMA 3).
 * Traduit tous les textes de l'interface selon la langue choisie.
 */
public class TranslationService {

    private static final String GROQ_API_KEY = System.getenv("NEXORA_GROQ_API_KEY") != null
            ? System.getenv("NEXORA_GROQ_API_KEY")
            : (System.getenv("GROQ_API_KEY") != null ? System.getenv("GROQ_API_KEY") : "");
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL   = "llama-3.3-70b-versatile";

    // ── Langues disponibles ────────────────────────────────────────────────────
    public enum Language {
        FRENCH  ("fr", "Français",  "🇫🇷"),
        ENGLISH ("en", "English",   "🇬🇧"),
        ARABIC  ("ar", "العربية",   "🇹🇳"),
        SPANISH ("es", "Español",   "🇪🇸"),
        GERMAN  ("de", "Deutsch",   "🇩🇪"),
        ITALIAN ("it", "Italiano",  "🇮🇹");

        public final String code;
        public final String label;
        public final String flag;

        Language(String code, String label, String flag) {
            this.code = code; this.label = label; this.flag = flag;
        }
    }

    // ── Cache des traductions (langue → clé → texte traduit) ──────────────────
    private static final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();

    // ── Langue courante ────────────────────────────────────────────────────────
    private static Language currentLanguage = Language.FRENCH;

    public static Language getCurrentLanguage() { return currentLanguage; }
    public static void setCurrentLanguage(Language lang) { currentLanguage = lang; }

    /**
     * Tous les textes originaux (français) de la page à traduire.
     * Clé = identifiant unique, Valeur = texte français original.
     */
    public static final Map<String, String> ORIGINAL_TEXTS = new LinkedHashMap<>();
    static {
        // ── Header ──────────────────────────────────────────────────────────────
        ORIGINAL_TEXTS.put("section_title",      "Gestion des comptes");
        ORIGINAL_TEXTS.put("section_subtitle",   "Gerer les comptes, suivre les soldes et configurer les coffres virtuels");
        ORIGINAL_TEXTS.put("btn_refresh",        "Actualiser");
        ORIGINAL_TEXTS.put("btn_new_vault",      "Nouveau coffre");
        ORIGINAL_TEXTS.put("btn_language",       "Langue");

        // ── KPI ─────────────────────────────────────────────────────────────────
        ORIGINAL_TEXTS.put("kpi_balance_title",  "SOLDE TOTAL");
        ORIGINAL_TEXTS.put("kpi_balance_desc",   "Sur tous les comptes");
        ORIGINAL_TEXTS.put("kpi_accounts_title", "COMPTES ACTIFS");
        ORIGINAL_TEXTS.put("kpi_accounts_desc",  "Actuellement utilises");
        ORIGINAL_TEXTS.put("kpi_accounts_unit",  "comptes");
        ORIGINAL_TEXTS.put("kpi_vaults_title",   "COFFRES VIRTUELS");
        ORIGINAL_TEXTS.put("kpi_vaults_desc",    "Objectifs d epargne actifs");
        ORIGINAL_TEXTS.put("kpi_vaults_unit",    "objectifs en cours");
        ORIGINAL_TEXTS.put("kpi_health_title",   "SANTE DU COMPTE");
        ORIGINAL_TEXTS.put("kpi_health_desc",    "Etat de securite");
        ORIGINAL_TEXTS.put("kpi_health_value",   "Excellent");

        // ── Formulaire Compte ────────────────────────────────────────────────────
        ORIGINAL_TEXTS.put("card_details_title",    "Details du compte");
        ORIGINAL_TEXTS.put("card_details_subtitle", "Voir et modifier les informations du compte");
        ORIGINAL_TEXTS.put("form_account_number",   "Numero de compte");
        ORIGINAL_TEXTS.put("form_account_prompt",   "Saisir le numero de compte");
        ORIGINAL_TEXTS.put("form_balance",          "Balance");
        ORIGINAL_TEXTS.put("form_opening_date",     "Date d ouverture");
        ORIGINAL_TEXTS.put("form_date_prompt",      "Selectionner une date");
        ORIGINAL_TEXTS.put("form_status",           "Statut du compte");
        ORIGINAL_TEXTS.put("form_status_prompt",    "Selectionner");
        ORIGINAL_TEXTS.put("form_type",             "Type de compte");
        ORIGINAL_TEXTS.put("form_type_prompt",      "Selectionner le type");
        ORIGINAL_TEXTS.put("form_withdraw",         "Plafond de retrait");
        ORIGINAL_TEXTS.put("form_transfer",         "Plafond de virement");
        ORIGINAL_TEXTS.put("btn_clear",             "Vider");
        ORIGINAL_TEXTS.put("btn_delete",            "Supprimer");
        ORIGINAL_TEXTS.put("btn_save",              "Enregistrer les modifications");

        // ── Formulaire Coffre ────────────────────────────────────────────────────
        ORIGINAL_TEXTS.put("vault_form_title",      "Configuration du coffre virtuel");
        ORIGINAL_TEXTS.put("vault_form_subtitle",   "Creer un nouvel objectif d epargne");
        ORIGINAL_TEXTS.put("vault_form_account",    "Compte bancaire associe");
        ORIGINAL_TEXTS.put("vault_form_account_prompt", "Choisir un compte");
        ORIGINAL_TEXTS.put("vault_form_name",       "Nom du coffre");
        ORIGINAL_TEXTS.put("vault_form_objective",  "Montant objectif");
        ORIGINAL_TEXTS.put("vault_form_deposit",    "Depot initial");
        ORIGINAL_TEXTS.put("vault_form_date",       "Date cible");
        ORIGINAL_TEXTS.put("vault_form_status",     "Statut du coffre");
        ORIGINAL_TEXTS.put("vault_form_lock",       "Verrouiller le coffre jusqu a la date cible");
        ORIGINAL_TEXTS.put("vault_form_auto",       "Activer le depot automatique");
        ORIGINAL_TEXTS.put("btn_cancel",            "Annuler");
        ORIGINAL_TEXTS.put("btn_create_vault",      "Creer le coffre");

        // ── Grille Comptes ───────────────────────────────────────────────────────
        ORIGINAL_TEXTS.put("card_accounts_title",    "Vos comptes");
        ORIGINAL_TEXTS.put("card_accounts_subtitle", "Tous les comptes bancaires lies");
        ORIGINAL_TEXTS.put("search_prompt",          "Rechercher des comptes...");
        ORIGINAL_TEXTS.put("filter_all",             "All");
        ORIGINAL_TEXTS.put("filter_current",         "Courant");
        ORIGINAL_TEXTS.put("filter_savings",         "Epargne");
        ORIGINAL_TEXTS.put("filter_pro",             "Professionnel");

        // ── Coffres Section ──────────────────────────────────────────────────────
        ORIGINAL_TEXTS.put("vaults_title",           "Coffres Virtuels");
        ORIGINAL_TEXTS.put("vaults_subtitle",        "Objectifs d epargne associes a ce compte");
        ORIGINAL_TEXTS.put("btn_close",              "Fermer");
        ORIGINAL_TEXTS.put("ai_title",               "Liste des Recommandations de Coffre");
        ORIGINAL_TEXTS.put("ai_subtitle",            "Propositions intelligentes generees selon votre situation financiere");
    }

    /**
     * Traduit tous les textes vers la langue cible via Groq API.
     * Retourne la Map traduite. En cas d'erreur, retourne les originaux.
     */
    public static Map<String, String> translateAll(Language targetLang, Runnable onDone) {
        if (targetLang == Language.FRENCH) {
            // Pas besoin de traduire, retourner les originaux
            cache.put("fr", new HashMap<>(ORIGINAL_TEXTS));
            if (onDone != null) javafx.application.Platform.runLater(onDone);
            return new HashMap<>(ORIGINAL_TEXTS);
        }

        // Vérifier le cache
        if (cache.containsKey(targetLang.code)) {
            if (onDone != null) javafx.application.Platform.runLater(onDone);
            return cache.get(targetLang.code);
        }

        // Traduire en arrière-plan
        new Thread(() -> {
            try {
                Map<String, String> translated = callGroqTranslation(targetLang);
                cache.put(targetLang.code, translated);
            } catch (Exception e) {
                System.err.println("[Translation] Erreur: " + e.getMessage());
                cache.put(targetLang.code, new HashMap<>(ORIGINAL_TEXTS));
            }
            if (onDone != null) javafx.application.Platform.runLater(onDone);
        }, "Translation-Thread").start();

        return null; // résultat disponible via callback
    }

    /** Récupère les traductions depuis le cache (doit être appelé après translateAll). */
    public static Map<String, String> getCached(Language lang) {
        if (lang == Language.FRENCH) return new HashMap<>(ORIGINAL_TEXTS);
        return cache.getOrDefault(lang.code, new HashMap<>(ORIGINAL_TEXTS));
    }

    /** Appelle Groq API pour traduire tous les textes en une seule requête. */
    private static Map<String, String> callGroqTranslation(Language targetLang) throws Exception {
        // Construire le JSON des textes à traduire
        JsonObject textsJson = new JsonObject();
        for (Map.Entry<String, String> entry : ORIGINAL_TEXTS.entrySet()) {
            textsJson.addProperty(entry.getKey(), entry.getValue());
        }

        String systemPrompt = "Tu es un traducteur professionnel. "
                + "Traduis les textes d'interface d'une application bancaire. "
                + "Réponds UNIQUEMENT avec un JSON valide, sans backticks, sans commentaires. "
                + "Garde exactement les mêmes clés JSON. Traduis seulement les valeurs. "
                + "Pour l'arabe, utilise l'arabe moderne standard (MSA). "
                + "Les textes sont courts (labels, boutons, titres). Sois concis et professionnel.";

        String userPrompt = "Traduis tous ces textes en " + targetLang.label
                + " (code langue: " + targetLang.code + ").\n"
                + "JSON à traduire:\n" + textsJson.toString()
                + "\n\nRéponds UNIQUEMENT avec le JSON traduit, même structure, mêmes clés.";

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
        body.addProperty("temperature", 0.2);
        body.addProperty("max_tokens", 2000);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> resp = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofString());

        System.out.println("[Translation] Groq HTTP: " + resp.statusCode());

        if (resp.statusCode() != 200) {
            throw new Exception("Groq HTTP " + resp.statusCode());
        }

        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
        String content = root.getAsJsonArray("choices").get(0)
                .getAsJsonObject().getAsJsonObject("message")
                .get("content").getAsString().trim();

        // Nettoyer les backticks si présents
        if (content.startsWith("```")) {
            int nl = content.indexOf('\n');
            if (nl != -1) content = content.substring(nl + 1);
            if (content.endsWith("```")) content = content.substring(0, content.lastIndexOf("```")).trim();
        }

        JsonObject translated = JsonParser.parseString(content).getAsJsonObject();
        Map<String, String> result = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : ORIGINAL_TEXTS.entrySet()) {
            String key = entry.getKey();
            result.put(key, translated.has(key) ? translated.get(key).getAsString() : entry.getValue());
        }

        System.out.println("[Translation] " + targetLang.label + " → " + result.size() + " textes traduits");
        return result;
    }
}
