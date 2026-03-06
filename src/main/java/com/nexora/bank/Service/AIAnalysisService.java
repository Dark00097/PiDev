package com.nexora.bank.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nexora.bank.Utils.AIResponseFormatter;
import com.nexora.bank.Utils.AIResponseFormatter.FormattedAnalysis;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class AIAnalysisService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public FormattedAnalysis getFormattedAnalysis(String rawGeminiResponse) {
        return AIResponseFormatter.parseGeminiResponse(rawGeminiResponse);
    }

    public String getPlainTextAnalysis(String rawGeminiResponse) {
        FormattedAnalysis analysis = AIResponseFormatter.parseGeminiResponse(rawGeminiResponse);
        return AIResponseFormatter.toPlainText(analysis);
    }

    public String stripMarkdown(String text) {
        return AIResponseFormatter.stripMarkdown(text);
    }

    public CompletableFuture<FormattedAnalysis> getFormattedAnalysisAsync(String rawGeminiResponse) {
        return CompletableFuture.supplyAsync(() ->
            AIResponseFormatter.parseGeminiResponse(rawGeminiResponse)
        );
    }

    public String analyserDepensesEtConseils(String transactionsJson) throws Exception {
        String prompt = """
            Tu es un conseiller financier expert d'une banque tunisienne.
            Voici les dernieres transactions du client en JSON :
            %s

            Reponds UNIQUEMENT en JSON valide, sans markdown, sans explication, sans backticks.
            Structure exacte :
            {
              "predictions": [
                {"categorie": "Alimentation", "montantPredit": 450.0, "tendance": "hausse"},
                {"categorie": "Transport", "montantPredit": 120.0, "tendance": "stable"}
              ],
              "totalPredit": 1200.0,
              "scoreEpargne": 65,
              "conseils": [
                {
                  "emoji": "🍔",
                  "titre": "Reduire les restaurants",
                  "description": "Vous depensez 40%% en restauration. Economisez 150 DT/mois.",
                  "economie": 150.0,
                  "priorite": "haute"
                }
              ],
              "resume": "Votre profil montre une hausse de 12%% ce mois."
            }
            """.formatted(transactionsJson);

        return appelGroq(prompt);
    }

    public String analyserSentiment(String description) throws Exception {
        String prompt = """
            Tu es un expert en analyse de sentiment pour une banque.
            Analyse cette reclamation et reponds UNIQUEMENT en JSON valide, sans markdown.

            Reclamation : "%s"

            Structure JSON exacte :
            {
              "sentiment": "furieux",
              "score": 85,
              "urgence": "haute",
              "emotion_dominante": "colere",
              "priorite_suggeree": "critique",
              "temps_reponse_suggere": "2 heures",
              "resume_professionnel": "Client tres insatisfait.",
              "reponse_suggeree": "Nous vous presentons nos sinceres excuses..."
            }

            Valeurs possibles pour sentiment : furieux, mecontent, neutre, satisfait, urgent
            Valeurs possibles pour urgence   : critique, haute, moyenne, faible
            """.formatted(description.replace("\"", "'"));

        return appelGroq(prompt);
    }

    private String appelGroq(String prompt) throws Exception {
        String apiKey = resolveGroqApiKey();

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.add("messages", messages);
        body.addProperty("temperature", 0.3);
        body.addProperty("max_tokens", 1500);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GROQ_API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
            .build();

        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Groq API error " + response.statusCode() + ": " + response.body());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        String content = root
            .getAsJsonArray("choices").get(0)
            .getAsJsonObject().getAsJsonObject("message")
            .get("content").getAsString();

        content = content.replaceAll("(?s)```json\\s*", "")
                         .replaceAll("(?s)```\\s*", "")
                         .trim();
        JsonParser.parseString(content);
        return content;
    }

    private String resolveGroqApiKey() {
        String key = System.getenv("NEXORA_GROQ_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getenv("GROQ_API_KEY");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                "Missing Groq API key. Set NEXORA_GROQ_API_KEY or GROQ_API_KEY in environment variables.");
        }
        return key.trim();
    }
}
