package com.nexora.bank.Service;

import java.util.Arrays;
import java.util.List;

public class BadWordService {

    // ── Liste de mots inappropriés (français + arabe translittéré) ────────────
    private static final List<String> BAD_WORDS = Arrays.asList(
        // Insultes françaises
        "idiot", "imbecile", "connard", "connasse", "salaud", "salope",
        "merde", "putain", "batard", "batarde", "con", "conne", "abruti",
        "abrutie", "cretin", "cretine", "enfoiré", "enfoiree", "ordure",
        "pourriture", "nul", "nulle", "incompetent", "incompetente",
        "voleur", "voleuse", "escroc", "arnaque", "arnaquer",
        "fraudeur", "fraudeuse", "criminel", "criminelle",
        // Menaces
        "je vais vous tuer", "je vous tue", "je vais porter plainte",
        "je vais vous detruire", "vous allez payer",
        // Arabe translittéré courant
        "kalb", "kelb", "barra", "wled", "7mar", "9a7ba",
        "hmar", "casba", "zamel", "sharmouta"
    );

    /**
     * Vérifie si le texte contient des mots inappropriés.
     * @return true si contient un bad word
     */
    public boolean containsBadWord(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase()
            .replace("é", "e").replace("è", "e").replace("ê", "e")
            .replace("à", "a").replace("â", "a")
            .replace("ô", "o").replace("î", "i").replace("û", "u")
            .replace("ç", "c");

        for (String word : BAD_WORDS) {
            if (lower.contains(word.toLowerCase())) return true;
        }
        return false;
    }

    /**
     * Retourne la liste des mots inappropriés trouvés.
     */
    public List<String> findBadWords(String text) {
        if (text == null || text.isBlank()) return List.of();
        String lower = text.toLowerCase()
            .replace("é", "e").replace("è", "e").replace("ê", "e")
            .replace("à", "a").replace("â", "a")
            .replace("ô", "o").replace("î", "i").replace("û", "u")
            .replace("ç", "c");

        return BAD_WORDS.stream()
            .filter(w -> lower.contains(w.toLowerCase()))
            .toList();
    }

    /**
     * Censure les mots inappropriés avec des étoiles.
     */
    public String censorText(String text) {
        if (text == null) return "";
        String result = text;
        for (String word : BAD_WORDS) {
            String stars = "*".repeat(word.length());
            result = result.replaceAll(
                "(?i)" + java.util.regex.Pattern.quote(word), stars);
        }
        return result;
    }
}