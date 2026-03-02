package com.nexora.bank.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nexora.bank.Models.User;
import com.nexora.bank.Models.UserActionLog;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GeminiAccountAdvisorService {

    private static final String API_KEY = fromEnv("NEXORA_GEMINI_API_KEY", "AIzaSyCyzF-c4eg1AU8jnFduDxATUKWs5AF-1Q8");
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final String[] PREFERRED_MODELS = new String[] {
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-1.5-flash"
    };

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build();

    public String analyzeUserAccount(User user, List<UserActionLog> actions) {
        if (user == null) {
            throw new IllegalArgumentException("User is required for AI analysis.");
        }
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException("Gemini API key is missing.");
        }

        String prompt = buildPrompt(user, actions);
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

    private String buildPrompt(User user, List<UserActionLog> actions) {
        StringBuilder activitySummary = new StringBuilder();
        if (actions == null || actions.isEmpty()) {
            activitySummary.append("Aucune action recente disponible.");
        } else {
            int max = Math.min(actions.size(), 12);
            for (int i = 0; i < max; i++) {
                UserActionLog action = actions.get(i);
                activitySummary
                    .append("- [")
                    .append(safe(action.getCreatedAt()))
                    .append("] ")
                    .append(safe(action.getActionType()))
                    .append(" | source: ")
                    .append(safe(action.getActionSource()))
                    .append(" | details: ")
                    .append(safe(action.getDetails()))
                    .append("\n");
            }
        }

        return """
            Tu es un assistant de securite bancaire.
            Reponds exclusivement en francais, de maniere concise et claire.

            Format obligatoire de reponse:
            1) Situation actuelle: [resume court]
            2) Niveau de risque de securite: [LOW|MEDIUM|HIGH|CRITICAL]
               - [raisons]
            3) Recommandations de securite:
               - [5 points max]
            4) Elements suspects ou points d attention:
               - [points a surveiller ou "Aucun element suspect majeur."]

            PROFIL UTILISATEUR:
            - Nom complet: %s %s
            - Email: %s
            - Statut du compte: %s
            - Compte cree le: %s
            - Compte ouvert depuis: %s
            - Derniere connexion: %s
            - Source derniere connexion: %s
            - Biometrie activee: %s

            ACTIONS RECENTES:
            %s
            """.formatted(
            safe(user.getPrenom()),
            safe(user.getNom()),
            safe(user.getEmail()),
            safe(user.getStatus()),
            safe(user.getCreatedAt()),
            safe(user.getAccountOpenedFrom()),
            safe(user.getLastOnlineAt()),
            safe(user.getLastOnlineFrom()),
            user.isBiometricEnabled() ? "OUI" : "NON",
            activitySummary
        );
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private static String fromEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
