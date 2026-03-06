package com.nexora.bank.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nexora.bank.Models.Cashback;
import com.nexora.bank.Models.Partenaire;
import com.nexora.bank.Models.User;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GeminiCashbackAdvisorService {

    private static final String API_KEY = fromEnv("NEXORA_GEMINI_API_KEY");
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String[] PREFERRED_MODELS = new String[] {
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-1.5-flash"
    };

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build();

    public String analyzeCashbackProfile(User user, List<Cashback> cashbacks, List<Partenaire> partenaires) {
        if (user == null) {
            throw new IllegalArgumentException("User is required for cashback analysis.");
        }
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException("Gemini API key is missing.");
        }

        String prompt = buildPrompt(user, cashbacks, partenaires);
        List<String> candidateModels = resolveCandidateModels();
        StringBuilder errors = new StringBuilder();

        for (String model : candidateModels) {
            try {
                String advice = requestAnalysis(prompt, model);
                if (!advice.isBlank()) {
                    return advice;
                }
            } catch (Exception ex) {
                if (!errors.isEmpty()) {
                    errors.append(" | ");
                }
                errors.append(model).append(": ").append(ex.getMessage());
            }
        }

        throw new IllegalStateException("Gemini request failed. " + errors);
    }

    public String buildLocalFallbackAdvice(User user, List<Cashback> cashbacks, List<Partenaire> partenaires) {
        double totalPending = 0;
        double totalCredited = 0;
        int pendingCount = 0;
        int ratedCount = 0;

        if (cashbacks != null) {
            for (Cashback cashback : cashbacks) {
                if (cashback == null) {
                    continue;
                }
                String status = safe(cashback.getStatut()).toLowerCase(Locale.ROOT);
                if ("credite".equals(status)) {
                    totalCredited += cashback.getMontantCashback();
                } else if ("en attente".equals(status) || "valide".equals(status)) {
                    totalPending += cashback.getMontantCashback();
                    pendingCount++;
                }
                if (cashback.getUserRating() != null) {
                    ratedCount++;
                }
            }
        }

        String topPartner = "-";
        double bestRating = -1;
        if (partenaires != null) {
            for (Partenaire p : partenaires) {
                if (p != null && p.getRating() > bestRating) {
                    bestRating = p.getRating();
                    topPartner = safe(p.getNom());
                }
            }
        }

        return """
            Resume cashback:
            - Total credite: %.2f DT
            - Total en attente: %.2f DT (%d transactions)
            - Transactions notees: %d

            Recommandations:
            1) Priorisez les partenaires avec bonne note, ex: %s.
            2) Regroupez vos achats > 200 DT quand pertinent pour viser 3%% de base.
            3) Notez chaque cashback pour accelerer la decision de bonus admin.
            4) Verifiez les transactions "En attente" de plus de 7 jours.
            5) Gardez une reference transaction claire pour suivi rapide.

            Conseils personnalises:
            - Utilisateur: %s %s
            - Objectif court terme: augmenter le ratio credite/en attente.
            """.formatted(
            totalCredited,
            totalPending,
            pendingCount,
            ratedCount,
            topPartner,
            safe(user.getPrenom()),
            safe(user.getNom())
        );
    }

    private String requestAnalysis(String prompt, String model) throws Exception {
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.25);
        requestBody.add("generationConfig", generationConfig);

        String endpoint = BASE_URL + "/models/" + model + ":generateContent?key=" + API_KEY;
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(45))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " - " + extractErrorMessage(response.body()));
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates == null || candidates.size() == 0) {
            throw new IllegalStateException("No AI response received.");
        }

        JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
        JsonObject responseContent = firstCandidate.getAsJsonObject("content");
        if (responseContent == null) {
            throw new IllegalStateException("AI content payload missing.");
        }

        JsonArray responseParts = responseContent.getAsJsonArray("parts");
        if (responseParts == null || responseParts.size() == 0) {
            throw new IllegalStateException("AI response parts missing.");
        }

        StringBuilder advice = new StringBuilder();
        for (int i = 0; i < responseParts.size(); i++) {
            JsonObject responsePart = responseParts.get(i).getAsJsonObject();
            if (responsePart.has("text")) {
                advice.append(responsePart.get("text").getAsString());
            }
        }

        String output = advice.toString().trim();
        if (output.isBlank()) {
            throw new IllegalStateException("AI analysis is empty.");
        }
        return output;
    }

    private List<String> resolveCandidateModels() {
        Set<String> models = new LinkedHashSet<>();
        models.addAll(fetchAvailableModels());
        for (String preferred : PREFERRED_MODELS) {
            models.add(preferred);
        }
        return new ArrayList<>(models);
    }

    private List<String> fetchAvailableModels() {
        List<String> models = new ArrayList<>();
        String endpoint = BASE_URL + "/models?key=" + API_KEY;
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(25))
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return models;
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray apiModels = root.getAsJsonArray("models");
            if (apiModels == null) {
                return models;
            }

            for (int i = 0; i < apiModels.size(); i++) {
                JsonObject model = apiModels.get(i).getAsJsonObject();
                String name = model.has("name") ? model.get("name").getAsString() : "";
                if (name.startsWith("models/")) {
                    name = name.substring("models/".length());
                }
                if (name.isBlank()) {
                    continue;
                }
                if (!supportsGenerateContent(model)) {
                    continue;
                }
                if (name.contains("embedding")) {
                    continue;
                }
                models.add(name);
            }
        } catch (Exception ignored) {
        }
        return models;
    }

    private boolean supportsGenerateContent(JsonObject model) {
        if (!model.has("supportedGenerationMethods")) {
            return true;
        }
        JsonArray methods = model.getAsJsonArray("supportedGenerationMethods");
        if (methods == null) {
            return true;
        }
        for (int i = 0; i < methods.size(); i++) {
            String method = methods.get(i).getAsString();
            if ("generateContent".equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }

    private String extractErrorMessage(String body) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonObject error = root.getAsJsonObject("error");
            if (error != null && error.has("message")) {
                return error.get("message").getAsString();
            }
        } catch (Exception ignored) {
        }
        return body == null ? "Unknown error" : body;
    }

    private String buildPrompt(User user, List<Cashback> cashbacks, List<Partenaire> partenaires) {
        int totalTransactions = cashbacks == null ? 0 : cashbacks.size();
        double totalAmount = 0;
        double totalCashback = 0;
        int pendingCount = 0;
        int ratedCount = 0;

        StringBuilder txSummary = new StringBuilder();
        if (cashbacks == null || cashbacks.isEmpty()) {
            txSummary.append("- Aucune transaction cashback.\n");
        } else {
            int max = Math.min(cashbacks.size(), 12);
            for (int i = 0; i < max; i++) {
                Cashback c = cashbacks.get(i);
                if (c == null) {
                    continue;
                }
                totalAmount += Math.max(c.getMontantAchat(), 0);
                totalCashback += Math.max(c.getMontantCashback(), 0);
                if ("en attente".equalsIgnoreCase(safe(c.getStatut()))) {
                    pendingCount++;
                }
                if (c.getUserRating() != null) {
                    ratedCount++;
                }
                txSummary.append("- [")
                    .append(formatDate(c.getDateAchat()))
                    .append("] ")
                    .append(safe(c.getPartenaireNom()))
                    .append(" | achat: ")
                    .append(String.format(Locale.US, "%.2f", c.getMontantAchat()))
                    .append(" | cashback: ")
                    .append(String.format(Locale.US, "%.2f", c.getMontantCashback()))
                    .append(" | taux: ")
                    .append(String.format(Locale.US, "%.2f%%", c.getTauxApplique()))
                    .append(" | statut: ")
                    .append(safe(c.getStatut()))
                    .append(" | note user: ")
                    .append(c.getUserRating() == null ? "-" : String.format(Locale.US, "%.1f/5", c.getUserRating()))
                    .append("\n");
            }
        }

        StringBuilder partnerSummary = new StringBuilder();
        if (partenaires == null || partenaires.isEmpty()) {
            partnerSummary.append("- Aucun partenaire disponible.\n");
        } else {
            int max = Math.min(partenaires.size(), 8);
            for (int i = 0; i < max; i++) {
                Partenaire p = partenaires.get(i);
                if (p == null) {
                    continue;
                }
                partnerSummary.append("- ")
                    .append(safe(p.getNom()))
                    .append(" | rating: ")
                    .append(String.format(Locale.US, "%.1f/5", p.getRating()))
                    .append(" | taux max: ")
                    .append(String.format(Locale.US, "%.2f%%", p.getTauxCashbackMax() > 0 ? p.getTauxCashbackMax() : p.getTauxCashback()))
                    .append("\n");
            }
        }

        return """
            Tu es un conseiller cashback bancaire.
            Reponds uniquement en francais, sans markdown, en texte clair lisible.

            Format obligatoire:
            1) Synthese rapide (3 lignes max)
            2) Opportunites d optimisation cashback (3 points)
            3) Points de vigilance (2 points max)
            4) Plan d action concret pour 7 jours (3 actions numerotees)

            Regles metier a respecter:
            - Taux de base selon montant: <50 => 1%%, 50-200 => 2%%, >200 => 3%%
            - Bonus partenaire: +1%% si rating partenaire > 4
            - Si rating partenaire < 3: pas de bonus partenaire
            - Mentionner l importance de noter chaque cashback pour decision bonus admin

            Profil utilisateur:
            - Nom: %s %s
            - Email: %s
            - Statut compte: %s

            Resume chiffres:
            - Transactions cashback: %d
            - Total achat observe: %.2f
            - Total cashback observe: %.2f
            - Transactions en attente: %d
            - Transactions notees par utilisateur: %d

            Transactions recentes:
            %s

            Partenaires disponibles:
            %s
            """.formatted(
            safe(user.getPrenom()),
            safe(user.getNom()),
            safe(user.getEmail()),
            safe(user.getStatus()),
            totalTransactions,
            totalAmount,
            totalCashback,
            pendingCount,
            ratedCount,
            txSummary,
            partnerSummary
        );
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "-" : date.toString();
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private static String fromEnv(String key) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }

        java.nio.file.Path envFile = java.nio.file.Path.of(".env");
        if (!java.nio.file.Files.exists(envFile)) {
            return null;
        }

        try {
            for (String line : java.nio.file.Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0) {
                    continue;
                }

                String fileKey = trimmed.substring(0, separator).trim();
                if (!fileKey.equals(key)) {
                    continue;
                }

                String fileValue = trimmed.substring(separator + 1).trim();
                if ((fileValue.startsWith("\"") && fileValue.endsWith("\""))
                    || (fileValue.startsWith("'") && fileValue.endsWith("'"))) {
                    fileValue = fileValue.substring(1, fileValue.length() - 1);
                }
                return fileValue.isBlank() ? null : fileValue;
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}
