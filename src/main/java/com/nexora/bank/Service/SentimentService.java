package com.nexora.bank.Service;

import com.google.gson.*;

public class SentimentService {

    private final AIAnalysisService aiService = new AIAnalysisService();

    public SentimentResult analyser(String description) {
        try {
            String     json = aiService.analyserSentiment(description);
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();

            return new SentimentResult(
                getStr(data, "sentiment",             "neutre"),
                data.has("score") ? data.get("score").getAsInt() : 50,
                getStr(data, "urgence",               "moyenne"),
                getStr(data, "emotion_dominante",     "neutre"),
                getStr(data, "priorite_suggeree",     "normale"),
                getStr(data, "temps_reponse_suggere", "24 heures"),
                getStr(data, "resume_professionnel",  ""),
                getStr(data, "reponse_suggeree",      "")
            );
        } catch (Exception e) {
            System.err.println("[SentimentService] Erreur : " + e.getMessage());
            return SentimentResult.defaut();
        }
    }

    private String getStr(JsonObject obj, String key, String def) {
        return obj.has(key) && !obj.get(key).isJsonNull()
            ? obj.get(key).getAsString() : def;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Résultat
    // ══════════════════════════════════════════════════════════════════════════

    public static class SentimentResult {
        public final String sentiment;
        public final int    score;
        public final String urgence;
        public final String emotionDominante;
        public final String prioriteSuggeree;
        public final String tempsReponseSuggere;
        public final String resumeProfessionnel;
        public final String reponseSuggeree;

        public SentimentResult(String sentiment, int score, String urgence,
                String emotionDominante, String prioriteSuggeree,
                String tempsReponseSuggere, String resumeProfessionnel,
                String reponseSuggeree) {
            this.sentiment            = sentiment;
            this.score                = score;
            this.urgence              = urgence;
            this.emotionDominante     = emotionDominante;
            this.prioriteSuggeree     = prioriteSuggeree;
            this.tempsReponseSuggere  = tempsReponseSuggere;
            this.resumeProfessionnel  = resumeProfessionnel;
            this.reponseSuggeree      = reponseSuggeree;
        }

        public static SentimentResult defaut() {
            return new SentimentResult(
                "neutre", 50, "moyenne", "neutre",
                "normale", "24 heures", "", "");
        }

        public String getColor() {
            return switch (sentiment.toLowerCase()) {
                case "furieux"   -> "#dc2626";
                case "mecontent" -> "#ea580c";
                case "urgent"    -> "#f59e0b";
                case "satisfait" -> "#22c55e";
                default          -> "#64748b";
            };
        }

        public String getEmoji() {
            return switch (sentiment.toLowerCase()) {
                case "furieux"   -> "😡";
                case "mecontent" -> "😞";
                case "urgent"    -> "🚨";
                case "satisfait" -> "😊";
                default          -> "😐";
            };
        }

        public String getUrgenceColor() {
            return switch (urgence.toLowerCase()) {
                case "critique" -> "#dc2626";
                case "haute"    -> "#ea580c";
                case "moyenne"  -> "#f59e0b";
                default         -> "#22c55e";
            };
        }
    }
}