package com.nexora.bank.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiService {

    private static final String API_KEY = resolveApiKey();
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    public String ameliorerDescription(String descriptionBrute) throws IOException {
        if (API_KEY.isBlank()) {
            throw new IOException("Missing Groq key. Set NEXORA_GROQ_API_KEY or GROQ_API_KEY.");
        }

        String prompt =
            "Tu es un assistant bancaire professionnel. " +
            "Ameliore la description de reclamation suivante en la rendant " +
            "claire, formelle, precise et professionnelle, en francais. " +
            "Garde le meme sens, mais utilise un langage approprie pour " +
            "une reclamation bancaire officielle. " +
            "Reponds UNIQUEMENT avec la description amelioree, sans introduction ni explication.\n\n" +
            "Description originale : " + descriptionBrute;

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty(
            "content",
            "Tu es un assistant bancaire professionnel qui ameliore les reclamations en francais.");

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(systemMsg);
        messages.add(userMsg);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "llama-3.1-8b-instant");
        requestBody.addProperty("max_tokens", 1024);
        requestBody.addProperty("temperature", 0.7);
        requestBody.add("messages", messages);

        byte[] bodyBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);

        HttpURLConnection conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
        }

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300)
            ? conn.getInputStream()
            : conn.getErrorStream();

        String responseJson;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            responseJson = sb.toString();
        } finally {
            conn.disconnect();
        }

        if (status < 200 || status >= 300) {
            throw new IOException("Erreur Groq HTTP " + status + " : " + responseJson);
        }

        JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
        return root
            .getAsJsonArray("choices")
            .get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .get("content").getAsString()
            .trim();
    }

    private static String resolveApiKey() {
        String key = System.getenv("NEXORA_GROQ_API_KEY");
        if (key != null && !key.isBlank()) {
            return key.trim();
        }

        key = System.getenv("GROQ_API_KEY");
        if (key != null && !key.isBlank()) {
            return key.trim();
        }

        return "";
    }
}
