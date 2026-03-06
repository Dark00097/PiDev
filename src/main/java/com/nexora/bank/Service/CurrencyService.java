package com.nexora.bank.Service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * ══════════════════════════════════════════════════════════════════
 * CurrencyService
 * ──────────────────────────────────────────────────────────────────
 * Utilise uniquement Gson (déjà présent dans votre pom.xml).
 * Aucune dépendance supplémentaire nécessaire.
 *
 * 1. Détecte le pays via IP       → ip-api.com      (gratuit, sans clé)
 * 2. Récupère les taux de change  → frankfurter.app (gratuit, sans clé)
 * ══════════════════════════════════════════════════════════════════
 */
public class CurrencyService {

    // ── URLs des APIs gratuites ────────────────────────────────────
    private static final String IP_API_URL    = "http://ip-api.com/json/";
    private static final String RATES_API_URL = "https://api.frankfurter.app/latest?from=TND";

    // ── Constantes devises ─────────────────────────────────────────
    public static final String CURRENCY_TND = "TND";
    public static final String CURRENCY_EUR = "EUR";
    public static final String CURRENCY_USD = "USD";

    public static final String SYMBOL_TND = "DT";
    public static final String SYMBOL_EUR = "€";
    public static final String SYMBOL_USD = "$";

    // ── Cache session (1 seul appel réseau par démarrage) ──────────
    private static String cachedCurrency = null;
    private static double cachedRateEUR  = 0.0;
    private static double cachedRateUSD  = 0.0;

    // ══════════════════════════════════════════════════════════════
    // API publique
    // ══════════════════════════════════════════════════════════════

    /**
     * Détecte la devise selon l'IP. Résultat mis en cache.
     * @return "TND", "EUR" ou "USD"
     */
    public String detectCurrency() {
        if (cachedCurrency != null) return cachedCurrency;
        try {
            String countryCode = fetchCountryFromIP();
            cachedCurrency = mapCountryToCurrency(countryCode);
            fetchRates();
            System.out.println("🌍 Pays : " + countryCode + " → Devise : " + cachedCurrency);
        } catch (Exception e) {
            System.out.println("⚠️ Géolocalisation échouée → TND par défaut");
            cachedCurrency = CURRENCY_TND;
            cachedRateEUR  = 0.29;
            cachedRateUSD  = 0.32;
        }
        return cachedCurrency;
    }

    /**
     * Retourne le symbole de la devise détectée : "DT", "€" ou "$"
     */
    public String getSymbol() {
        return currencyToSymbol(detectCurrency());
    }

    /**
     * Convertit un montant TND → devise locale (pour l'affichage informatif).
     */
    public double convert(double montantTND) {
        switch (detectCurrency()) {
            case CURRENCY_EUR: return montantTND * getRateEUR();
            case CURRENCY_USD: return montantTND * getRateUSD();
            default:           return montantTND;
        }
    }

    /**
     * Convertit un montant devise locale → TND (pour le stockage en base).
     * Ex : utilisateur en France tape 100€ → on stocke 344.83 DT
     */
    public double convertToTND(double montantLocal) {
        switch (detectCurrency()) {
            case CURRENCY_EUR: return getRateEUR() > 0 ? montantLocal / getRateEUR() : montantLocal;
            case CURRENCY_USD: return getRateUSD() > 0 ? montantLocal / getRateUSD() : montantLocal;
            default:           return montantLocal; // déjà en TND
        }
    }

    public double getRateEUR() {
        if (cachedRateEUR == 0.0) fetchRates();
        return cachedRateEUR > 0 ? cachedRateEUR : 0.29;
    }

    public double getRateUSD() {
        if (cachedRateUSD == 0.0) fetchRates();
        return cachedRateUSD > 0 ? cachedRateUSD : 0.32;
    }

    /** Remet le cache à zéro (utile pour les tests) */
    public static void resetCache() {
        cachedCurrency = null;
        cachedRateEUR  = 0.0;
        cachedRateUSD  = 0.0;
    }

    // ══════════════════════════════════════════════════════════════
    // Appels réseau
    // ══════════════════════════════════════════════════════════════

    /**
     * Appelle ip-api.com et retourne le code pays (ex: "TN", "FR", "US").
     * Utilise Gson pour parser la réponse JSON.
     */
    private String fetchCountryFromIP() throws Exception {
        String response = httpGet(IP_API_URL);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        String status   = json.get("status").getAsString();
        if (!"success".equals(status))
            throw new Exception("ip-api échec : " + status);
        return json.get("countryCode").getAsString(); // ex: "TN"
    }

    /**
     * Appelle frankfurter.app pour charger les taux TND → EUR/USD.
     * Utilise Gson pour parser la réponse JSON.
     */
    private void fetchRates() {
        try {
            String response  = httpGet(RATES_API_URL);
            JsonObject json  = JsonParser.parseString(response).getAsJsonObject();
            JsonObject rates = json.getAsJsonObject("rates");

            cachedRateEUR = rates.has("EUR") ? rates.get("EUR").getAsDouble() : 0.29;
            cachedRateUSD = rates.has("USD") ? rates.get("USD").getAsDouble() : 0.32;

            System.out.printf("💱 Taux : 1 TND = %.4f EUR = %.4f USD%n",
                    cachedRateEUR, cachedRateUSD);
        } catch (Exception e) {
            System.out.println("⚠️ Taux indisponibles → valeurs par défaut");
            cachedRateEUR = 0.29;
            cachedRateUSD = 0.32;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════

    /**
     * Effectue un GET HTTP simple et retourne la réponse en String.
     * Timeout 5 secondes pour ne pas bloquer l'UI.
     */
    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("Accept", "application/json");

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    /**
     * Mappe un code pays ISO 3166-1 alpha-2 vers une devise.
     *
     *   TN        → TND (Dinar tunisien)
     *   Zone euro → EUR
     *   Reste     → USD
     */
    private String mapCountryToCurrency(String code) {
        if (code == null) return CURRENCY_TND;
        switch (code.toUpperCase()) {
            case "TN": return CURRENCY_TND;
            case "FR": case "DE": case "IT": case "ES": case "PT":
            case "BE": case "NL": case "AT": case "FI": case "IE":
            case "GR": case "LU": case "SK": case "SI": case "EE":
            case "LV": case "LT": case "CY": case "MT":
                return CURRENCY_EUR;
            default: return CURRENCY_USD;
        }
    }

    private String currencyToSymbol(String currency) {
        switch (currency) {
            case CURRENCY_EUR: return SYMBOL_EUR;
            case CURRENCY_USD: return SYMBOL_USD;
            default:           return SYMBOL_TND;
        }
    }
}