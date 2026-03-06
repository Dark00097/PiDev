package com.nexora.bank.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nexora.bank.Utils.MyDB;
import com.nexora.bank.Utils.SessionManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class RevenueService {

    private static final String GROQ_API_KEY = System.getenv("NEXORA_GROQ_API_KEY") != null
            ? System.getenv("NEXORA_GROQ_API_KEY")
            : (System.getenv("GROQ_API_KEY") != null ? System.getenv("GROQ_API_KEY") : "");
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL   = "llama-3.3-70b-versatile";

    public Map<String, Object> detectSurplus(int idUser) {
        try {
            Connection conn = MyDB.getInstance().getConn();

            // Pas de colonne revenuMensuel → on utilise uniquement l'historique transactions
            double revenuDeclare = 0;

            // Récupérer transactions CREDIT des 4 derniers mois
            LocalDate now          = LocalDate.now();
            String currentMonth    = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String threeMonthsAgo  = now.minusMonths(3).format(DateTimeFormatter.ofPattern("yyyy-MM"));

            // ✅ NOUVEAU : Vérifier si la suggestion a déjà été affichée ce mois pour cet utilisateur
            if (surplusDejaAfficheCeMois(conn, idUser, currentMonth)) {
                System.out.println("[Surplus] Déjà affiché ce mois (" + currentMonth + ") pour user " + idUser + " → skip.");
                return null;
            }

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT SUBSTR(dateTransaction, 1, 7) AS month, SUM(montant) AS total " +
                            "FROM transactions " +
                            "WHERE idUser = ? AND typeTransaction = 'CREDIT' AND dateTransaction >= ? " +
                            "GROUP BY month"
            );
            ps.setInt(1, idUser);
            ps.setString(2, threeMonthsAgo + "-01");
            ResultSet rs = ps.executeQuery();

            Map<String, Double> monthlyRevenues = new HashMap<>();
            double totalHistorical   = 0;
            int    historicalMonths  = 0;

            while (rs.next()) {
                String month = rs.getString("month");
                double total = rs.getDouble("total");
                monthlyRevenues.put(month, total);
                if (!month.equals(currentMonth)) {
                    totalHistorical += total;
                    historicalMonths++;
                }
            }
            rs.close();
            ps.close();

            System.out.println("[Surplus] Mois courant : " + currentMonth);
            System.out.println("[Surplus] Mois historiques trouvés : " + historicalMonths);
            System.out.println("[Surplus] Revenus par mois : " + monthlyRevenues);

            if (historicalMonths == 0) {
                System.out.println("[Surplus] Pas d'historique → pas de détection possible.");
                return null;
            }

            double averageHistorical = totalHistorical / historicalMonths;
            double currentRevenue    = monthlyRevenues.getOrDefault(currentMonth, 0.0);

            System.out.println("[Surplus] Moyenne historique : " + averageHistorical + " DT");
            System.out.println("[Surplus] Revenu ce mois : " + currentRevenue + " DT");

            double surplus = currentRevenue - averageHistorical;

            System.out.println("[Surplus] Surplus calculé : " + surplus + " DT");

            if (surplus <= 0 || surplus / averageHistorical < 0.2) {
                System.out.println("[Surplus] Surplus insuffisant (< 20%) → pas de suggestion.");
                return null;
            }

            System.out.println("[Surplus] ✅ Surplus significatif détecté ! Appel API Groq...");

            // Appel API Groq
            String prompt = String.format(
                    "Revenu moyen des mois précédents: %.2f DT, revenu ce mois: %.2f DT, surplus: %.2f DT. " +
                            "Calcule exactement 20%% du surplus. " +
                            "Génère un message court et personnalisé en français. " +
                            "Réponds UNIQUEMENT avec ce JSON exact, sans texte avant ni après, sans markdown : " +
                            "{\"message\": \"...\", \"pourcentageEpargne\": 20, \"montantEpargne\": %.2f}",
                    averageHistorical, currentRevenue, surplus, surplus * 0.20
            );

            JsonObject response = callGroqApi(prompt);

            if (response == null) {
                // Fallback : construire la réponse manuellement sans API
                System.out.println("[Surplus] API échouée → fallback message.");
                double montantFallback = surplus * 0.20;
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("message", String.format(
                        "Ce mois-ci, votre revenu est supérieur de %.0f DT par rapport à votre moyenne habituelle. " +
                                "Souhaitez-vous épargner une partie de ce surplus dans l'un de vos coffres virtuels ?", surplus));
                fallback.put("surplus", surplus);
                fallback.put("montantEpargne", montantFallback);

                // ✅ NOUVEAU : Enregistrer l'affichage pour ne plus le montrer ce mois
                marquerSurplusAffiche(conn, idUser, currentMonth);
                return fallback;
            }

            String message        = response.get("message").getAsString();
            double montantEpargne = response.get("montantEpargne").getAsDouble();

            System.out.println("[Surplus] Message API : " + message);
            System.out.println("[Surplus] Montant épargne suggéré : " + montantEpargne + " DT");

            Map<String, Object> result = new HashMap<>();
            result.put("message", message);
            result.put("surplus", surplus);
            result.put("montantEpargne", montantEpargne);

            // ✅ NOUVEAU : Enregistrer l'affichage pour ne plus le montrer ce mois
            marquerSurplusAffiche(conn, idUser, currentMonth);
            return result;

        } catch (Exception e) {
            System.err.println("[Surplus] Erreur détection surplus: " + e.getMessage());
            return null;
        }
    }

    // =========================================================================
    // ✅ NOUVELLE MÉTHODE : Vérifier si la suggestion surplus a déjà été
    //    affichée ce mois pour cet utilisateur (lecture en BDD)
    // =========================================================================
    private boolean surplusDejaAfficheCeMois(Connection conn, int idUser, String mois) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM surplus_notifications WHERE idUser = ? AND moisAffiche = ?"
            );
            ps.setInt(1, idUser);
            ps.setString(2, mois);
            ResultSet rs = ps.executeQuery();
            boolean existe = rs.next();
            rs.close();
            ps.close();
            return existe;
        } catch (Exception e) {
            System.err.println("[Surplus] Erreur vérification notification: " + e.getMessage());
            return false; // En cas d'erreur, on laisse passer pour ne pas bloquer l'utilisateur
        }
    }

    // =========================================================================
    // ✅ NOUVELLE MÉTHODE : Enregistrer en BDD que la suggestion surplus a été
    //    affichée ce mois pour cet utilisateur
    // =========================================================================
    private void marquerSurplusAffiche(Connection conn, int idUser, String mois) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO surplus_notifications (idUser, moisAffiche) VALUES (?, ?)"
            );
            ps.setInt(1, idUser);
            ps.setString(2, mois);
            ps.executeUpdate();
            ps.close();
            System.out.println("[Surplus] ✅ Notification enregistrée → user " + idUser + " / mois " + mois);
        } catch (Exception e) {
            System.err.println("[Surplus] Erreur enregistrement notification: " + e.getMessage());
        }
    }

    private JsonObject callGroqApi(String prompt) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            JsonObject requestBody = new JsonObject();
            JsonArray  messages    = new JsonArray();

            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content",
                    "Tu es un assistant financier. Tu réponds UNIQUEMENT en JSON valide, " +
                            "sans texte avant ni après, sans balises markdown, sans backticks. " +
                            "Juste le JSON brut.");
            messages.add(systemMsg);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", prompt);
            messages.add(userMsg);

            requestBody.addProperty("model", GROQ_MODEL);
            requestBody.add("messages", messages);
            requestBody.addProperty("temperature", 0.3);  // Moins créatif = JSON plus stable
            requestBody.addProperty("max_tokens", 300);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + GROQ_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("[Surplus] Groq HTTP " + response.statusCode() + " : " + response.body());
                return null;
            }

            // Extraire le content de la réponse Groq
            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            String content = jsonResponse
                    .getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            System.out.println("[Surplus] Contenu brut Groq : " + content);

            // ── Nettoyer le contenu : enlever backticks markdown si présents ──
            String clean = content.trim();
            if (clean.startsWith("```")) {
                int firstNewline = clean.indexOf('\n');
                if (firstNewline != -1) clean = clean.substring(firstNewline + 1);
                if (clean.endsWith("```")) clean = clean.substring(0, clean.lastIndexOf("```")).trim();
            }

            // ── Extraire uniquement la partie JSON { ... } ─────────────────
            int startBrace = clean.indexOf('{');
            int endBrace   = clean.lastIndexOf('}');
            if (startBrace != -1 && endBrace != -1 && endBrace > startBrace) {
                clean = clean.substring(startBrace, endBrace + 1);
            }

            System.out.println("[Surplus] JSON nettoyé : " + clean);

            return JsonParser.parseString(clean).getAsJsonObject();

        } catch (Exception e) {
            System.err.println("[Surplus] Erreur API Groq: " + e.getMessage());
            return null;
        }
    }
}
