package com.nexora.bank.Service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Service Speech-to-Text utilisant l'API Groq (Whisper).
 * Enregistre le micro, envoie l'audio à Groq et retourne le texte transcrit.
 * Ensuite parse le texte pour remplir automatiquement les champs du formulaire.
 */
public class SpeechToTextService {

    private static final String GROQ_API_KEY = System.getenv("NEXORA_GROQ_API_KEY") != null
            ? System.getenv("NEXORA_GROQ_API_KEY")
            : (System.getenv("GROQ_API_KEY") != null ? System.getenv("GROQ_API_KEY") : "");
    private static final String WHISPER_URL  = "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final String PARSE_URL    = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL   = "llama-3.3-70b-versatile";

    // ── Enregistrement audio ───────────────────────────────────────────────────
    private TargetDataLine microLine = null;
    private volatile boolean recording = false;
    private File  tempAudioFile = null;

    /**
     * Modèle des champs extraits du texte vocal.
     */
    public static class FormData {
        public String numeroCompte;
        public String solde;
        public String dateOuverture;
        public String statut;
        public String typeCompte;
        public String plafondRetrait;
        public String plafondVirement;
        public String rawText;   // texte brut reconnu
        public String error;     // message d'erreur si échec

        @Override
        public String toString() {
            return "Transcription : " + rawText;
        }
    }

    /** Démarre l'enregistrement microphone. */
    public void startRecording() throws Exception {
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new Exception("Microphone non supporté sur ce système.");
        }
        microLine = (TargetDataLine) AudioSystem.getLine(info);
        microLine.open(format);
        microLine.start();
        recording = true;

        tempAudioFile = File.createTempFile("nexora_voice_", ".wav");
        final File audioOut = tempAudioFile;

        new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(microLine);
                 OutputStream os = Files.newOutputStream(audioOut.toPath())) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, audioOut);
            } catch (Exception e) {
                if (recording) System.err.println("[Voice] Erreur enregistrement: " + e.getMessage());
            }
        }, "Voice-Recorder").start();
    }

    /** Arrête l'enregistrement et retourne le fichier WAV. */
    public File stopRecording() {
        recording = false;
        if (microLine != null) {
            microLine.stop();
            microLine.close();
            microLine = null;
        }
        return tempAudioFile;
    }

    /**
     * Envoie le fichier audio à Groq Whisper et retourne la transcription.
     */
    public String transcribe(File audioFile) throws Exception {
        if (audioFile == null || !audioFile.exists())
            throw new Exception("Fichier audio introuvable.");

        // Construction multipart/form-data
        String boundary = "----NexoraBoundary" + System.currentTimeMillis();
        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos, "UTF-8"), true);

        // -- model field
        pw.append("--").append(boundary).append("\r\n");
        pw.append("Content-Disposition: form-data; name=\"model\"").append("\r\n\r\n");
        pw.append("whisper-large-v3").append("\r\n");

        // -- language field
        pw.append("--").append(boundary).append("\r\n");
        pw.append("Content-Disposition: form-data; name=\"language\"").append("\r\n\r\n");
        pw.append("fr").append("\r\n");

        // -- response_format
        pw.append("--").append(boundary).append("\r\n");
        pw.append("Content-Disposition: form-data; name=\"response_format\"").append("\r\n\r\n");
        pw.append("json").append("\r\n");

        // -- file field header
        pw.append("--").append(boundary).append("\r\n");
        pw.append("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"").append("\r\n");
        pw.append("Content-Type: audio/wav").append("\r\n\r\n");
        pw.flush();

        baos.write(audioBytes);

        pw.append("\r\n--").append(boundary).append("--\r\n");
        pw.flush();

        byte[] body = baos.toByteArray();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WHISPER_URL))
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[Voice] Whisper HTTP: " + response.statusCode());

        if (response.statusCode() != 200) {
            throw new Exception("Groq Whisper erreur " + response.statusCode() + ": " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return json.has("text") ? json.get("text").getAsString().trim() : "";
    }

    /**
     * Analyse le texte transcrit avec LLaMA 3 pour extraire les champs du formulaire.
     * Le LLM comprend les formulations naturelles en français.
     */
    public FormData parseWithAI(String transcribedText) {
        FormData fd = new FormData();
        fd.rawText = transcribedText;

        if (transcribedText == null || transcribedText.trim().isEmpty()) {
            fd.error = "Aucun texte reconnu.";
            return fd;
        }

        try {
            String systemPrompt = "Tu es un assistant qui extrait des informations bancaires depuis un texte oral. "
                    + "Retourne UNIQUEMENT un JSON valide sans backticks avec ces champs : "
                    + "{ \"numeroCompte\": \"\", \"solde\": \"\", \"dateOuverture\": \"\", "
                    + "\"statut\": \"\", \"typeCompte\": \"\", \"plafondRetrait\": \"\", \"plafondVirement\": \"\" }. "
                    + "Règles importantes : "
                    + "- numeroCompte : le numéro de compte (chiffres/lettres). "
                    + "- solde : uniquement le nombre (ex: '1500.00'). "
                    + "- dateOuverture : format YYYY-MM-DD si possible, sinon vide. "
                    + "- statut : 'Active', 'Bloque' ou 'Ferme' uniquement. "
                    + "- typeCompte : 'Courant', 'Epargne' ou 'Professionnel' uniquement. "
                    + "- plafondRetrait : uniquement le nombre. "
                    + "- plafondVirement : uniquement le nombre. "
                    + "Si une information n'est pas mentionnée, laisse le champ vide ''. "
                    + "Ne mets AUCUN texte en dehors du JSON.";

            String userPrompt = "Extrait les informations bancaires de ce texte :\n\"" + transcribedText + "\"";

            String bodyJson = "{"
                    + "\"model\":\"" + GROQ_MODEL + "\","
                    + "\"messages\":["
                    + "{\"role\":\"system\",\"content\":" + escapeJson(systemPrompt) + "},"
                    + "{\"role\":\"user\",\"content\":" + escapeJson(userPrompt) + "}"
                    + "],"
                    + "\"temperature\":0.1,\"max_tokens\":400}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PARSE_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + GROQ_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[Voice] Parse HTTP: " + response.statusCode());

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            String content = root.getAsJsonArray("choices").get(0)
                    .getAsJsonObject().getAsJsonObject("message")
                    .get("content").getAsString().trim();

            // Nettoyer backticks
            if (content.startsWith("```")) {
                int nl = content.indexOf('\n');
                if (nl != -1) content = content.substring(nl + 1);
                if (content.endsWith("```")) content = content.substring(0, content.lastIndexOf("```")).trim();
            }

            JsonObject parsed = JsonParser.parseString(content).getAsJsonObject();
            fd.numeroCompte    = safeGet(parsed, "numeroCompte");
            fd.solde           = safeGet(parsed, "solde");
            fd.dateOuverture   = safeGet(parsed, "dateOuverture");
            fd.statut          = safeGet(parsed, "statut");
            fd.typeCompte      = safeGet(parsed, "typeCompte");
            fd.plafondRetrait  = safeGet(parsed, "plafondRetrait");
            fd.plafondVirement = safeGet(parsed, "plafondVirement");

            System.out.println("[Voice] Extraction réussie : " + content);

        } catch (Exception e) {
            System.err.println("[Voice] Erreur parsing AI: " + e.getMessage());
            // Fallback : parsing basique par mots-clés
            fd = parseWithKeywords(transcribedText);
            fd.rawText = transcribedText;
        }

        return fd;
    }

    /**
     * Parsing de secours par mots-clés si l'API est indisponible.
     */
    private FormData parseWithKeywords(String text) {
        FormData fd = new FormData();
        fd.rawText = text;
        String lower = text.toLowerCase();

        // Numéro de compte
        java.util.regex.Matcher numMatcher = java.util.regex.Pattern
                .compile("(?:numéro|numero|compte)[^\\d]*(\\w{4,20})", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(text);
        if (numMatcher.find()) fd.numeroCompte = numMatcher.group(1);

        // Solde / montant
        java.util.regex.Matcher soldeMatcher = java.util.regex.Pattern
                .compile("(?:solde|montant|balance)[^\\d]*(\\d+(?:[.,]\\d{1,2})?)")
                .matcher(lower);
        if (soldeMatcher.find()) fd.solde = soldeMatcher.group(1).replace(',', '.');

        // Type de compte
        if (lower.contains("épargne") || lower.contains("epargne")) fd.typeCompte = "Epargne";
        else if (lower.contains("professionnel")) fd.typeCompte = "Professionnel";
        else if (lower.contains("courant")) fd.typeCompte = "Courant";

        // Statut
        if (lower.contains("actif") || lower.contains("active") || lower.contains("ouvert")) fd.statut = "Active";
        else if (lower.contains("bloqué") || lower.contains("bloque")) fd.statut = "Bloque";
        else if (lower.contains("fermé") || lower.contains("ferme")) fd.statut = "Ferme";

        // Plafond retrait
        java.util.regex.Matcher retraitMatcher = java.util.regex.Pattern
                .compile("(?:retrait|retirer)[^\\d]*(\\d+(?:[.,]\\d{1,2})?)")
                .matcher(lower);
        if (retraitMatcher.find()) fd.plafondRetrait = retraitMatcher.group(1).replace(',', '.');

        // Plafond virement
        java.util.regex.Matcher virementMatcher = java.util.regex.Pattern
                .compile("(?:virement|virer)[^\\d]*(\\d+(?:[.,]\\d{1,2})?)")
                .matcher(lower);
        if (virementMatcher.find()) fd.plafondVirement = virementMatcher.group(1).replace(',', '.');

        return fd;
    }

    private String safeGet(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString().trim() : "";
    }

    private String escapeJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    public boolean isRecording() { return recording; }
}
