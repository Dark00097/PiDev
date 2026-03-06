package com.nexora.bank.Service;

import com.nexora.bank.Models.HistoryEntry;
import com.nexora.bank.Models.HistoryEntry.ActionType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestionnaire singleton de l'historique des actions.
 * ──────────────────────────────────────────────────────────────────────────
 * PERSISTANCE :
 *  • Chaque utilisateur possède son propre fichier JSON :
 *      <user.home>/.nexora/history/history_user_<userId>.json
 *  • Le fichier est chargé au premier appel à loadForUser(userId).
 *  • Chaque modification (add / remove / clear) déclenche une sauvegarde
 *    immédiate sur disque (écriture atomique via fichier temporaire).
 * ──────────────────────────────────────────────────────────────────────────
 */
public class HistoryManager {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static HistoryManager instance;

    public static HistoryManager getInstance() {
        if (instance == null) instance = new HistoryManager();
        return instance;
    }

    private HistoryManager() {}

    // ── État interne ──────────────────────────────────────────────────────────
    private final ObservableList<HistoryEntry> entries = FXCollections.observableArrayList();
    private int  currentUserId   = -1;
    private Path currentFilePath = null;

    // Répertoire de base : <user.home>/.nexora/history/
    private static final Path BASE_DIR = Paths.get(
            System.getProperty("user.home"), ".nexora", "history");

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ══════════════════════════════════════════════════════════════════════════
    // API PUBLIQUE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Charge l'historique de l'utilisateur depuis le disque.
     * À appeler juste après la connexion (dans initialize() du controller).
     */
    public void loadForUser(int userId) {
        if (userId == currentUserId && !entries.isEmpty()) return;
        currentUserId   = userId;
        currentFilePath = BASE_DIR.resolve("history_user_" + userId + ".json");
        entries.clear();
        entries.addAll(readFromDisk(currentFilePath));
        System.out.println("[History] Chargé " + entries.size()
                + " entrée(s) — userId=" + userId
                + " — fichier: " + currentFilePath);
    }

    /** Ajoute une entrée en tête de liste (la plus récente d'abord) et sauvegarde immédiatement. */
    public void add(ActionType type, String description) {
        entries.add(0, new HistoryEntry(type, description));
        saveToDisk();
    }

    /** Liste observable complète (compatible JavaFX bindings). */
    public ObservableList<HistoryEntry> getAll() { return entries; }

    /**
     * Recherche par mot-clé dans : type, description, date, heure.
     * Insensible à la casse et aux accents de base.
     */
    public List<HistoryEntry> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty())
            return new ArrayList<>(entries);
        String kw = keyword.trim().toLowerCase();
        return entries.stream()
                .filter(e -> e.getSearchableText().contains(kw))
                .collect(Collectors.toList());
    }

    /** Supprime une entrée par son ID et sauvegarde. */
    public void remove(String id) {
        entries.removeIf(e -> e.getId().equals(id));
        saveToDisk();
    }

    /** Vide tout l'historique pour l'utilisateur courant et sauvegarde. */
    public void clear() {
        entries.clear();
        saveToDisk();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PERSISTANCE JSON — sans dépendance externe (pas de Gson requis)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Sauvegarde atomique : écrit dans un .tmp puis renomme.
     * Si l'écriture échoue, le fichier d'origine reste intact.
     */
    private void saveToDisk() {
        if (currentFilePath == null) return;
        try {
            Files.createDirectories(BASE_DIR);
            Path tmp = currentFilePath.resolveSibling(
                    currentFilePath.getFileName() + ".tmp");

            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < entries.size(); i++) {
                sb.append(entryToJson(entries.get(i)));
                if (i < entries.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");

            Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Renommage atomique
            try {
                Files.move(tmp, currentFilePath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, currentFilePath, StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("[History] ✅ Sauvegardé " + entries.size()
                    + " entrée(s) → " + currentFilePath);
        } catch (Exception ex) {
            System.err.println("[History] ❌ Erreur sauvegarde : " + ex.getMessage());
        }
    }

    /**
     * Lecture du fichier JSON et reconstruction de la liste.
     * Tolère un fichier absent, vide ou partiellement corrompu.
     */
    private List<HistoryEntry> readFromDisk(Path path) {
        List<HistoryEntry> result = new ArrayList<>();
        if (!Files.exists(path)) {
            System.out.println("[History] Aucun historique trouvé pour cet utilisateur.");
            return result;
        }
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8).trim();
            if (raw.isEmpty() || raw.equals("[]")) return result;

            // Retirer les crochets [ ] englobants
            String content = raw;
            if (content.startsWith("[")) content = content.substring(1);
            if (content.endsWith("]"))   content = content.substring(0, content.lastIndexOf("]"));

            // Découper en objets JSON individuels
            for (String obj : splitJsonObjects(content)) {
                HistoryEntry e = jsonToEntry(obj.trim());
                if (e != null) result.add(e);
            }
            System.out.println("[History] 📂 Lu " + result.size() + " entrée(s).");
        } catch (Exception ex) {
            System.err.println("[History] ❌ Erreur lecture : " + ex.getMessage());
        }
        return result;
    }

    // ── Sérialisation ──────────────────────────────────────────────────────────
    private String entryToJson(HistoryEntry e) {
        return "  {"
                + "\"id\":"          + jsonStr(e.getId())
                + ",\"type\":"       + jsonStr(e.getType().name())
                + ",\"description\":" + jsonStr(e.getDescription())
                + ",\"timestamp\":"  + jsonStr(e.getTimestamp().format(DT_FMT))
                + "}";
    }

    // ── Désérialisation ────────────────────────────────────────────────────────
    private HistoryEntry jsonToEntry(String obj) {
        try {
            String id          = extractJsonString(obj, "id");
            String typeName    = extractJsonString(obj, "type");
            String description = extractJsonString(obj, "description");
            String tsStr       = extractJsonString(obj, "timestamp");
            if (id == null || typeName == null || description == null || tsStr == null)
                return null;
            ActionType    type = ActionType.valueOf(typeName);
            LocalDateTime ts   = LocalDateTime.parse(tsStr, DT_FMT);
            return HistoryEntry.reconstruct(id, type, description, ts);
        } catch (Exception ex) {
            System.err.println("[History] Entrée ignorée : " + ex.getMessage());
            return null;
        }
    }

    // ── Utilitaires JSON minimalistes ──────────────────────────────────────────

    /** Encode une chaîne Java en littéral JSON (avec échappements). */
    private String jsonStr(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    /** Extrait la valeur d'un champ JSON string depuis un objet brut. */
    private String extractJsonString(String obj, String key) {
        String search = "\"" + key + "\":\"";
        int start = obj.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < obj.length()) {
            char c = obj.charAt(i);
            if (c == '\\' && i + 1 < obj.length()) {
                char next = obj.charAt(i + 1);
                switch (next) {
                    case '"':  sb.append('"');  i += 2; break;
                    case '\\': sb.append('\\'); i += 2; break;
                    case 'n':  sb.append('\n'); i += 2; break;
                    case 'r':  sb.append('\r'); i += 2; break;
                    case 't':  sb.append('\t'); i += 2; break;
                    default:   sb.append(c);    i++;    break;
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Découpe un tableau JSON (sans crochets) en objets individuels {…}.
     * Gère la profondeur des accolades pour ne pas couper à l'intérieur.
     */
    private List<String> splitJsonObjects(String content) {
        List<String> result = new ArrayList<>();
        int depth = 0, start = -1;
        boolean inString = false;
        char prev = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"' && prev != '\\') inString = !inString;
            if (!inString) {
                if (c == '{') { if (depth == 0) start = i; depth++; }
                else if (c == '}') {
                    depth--;
                    if (depth == 0 && start >= 0) {
                        result.add(content.substring(start, i + 1));
                        start = -1;
                    }
                }
            }
            prev = c;
        }
        return result;
    }
}