package com.nexora.bank.Models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Représente une entrée dans l'historique des actions de l'application.
 *
 * NOUVEAUTÉ : méthode statique reconstruct() permettant de recréer une
 * entrée depuis le fichier JSON persistant (id + timestamp préservés).
 */
public class HistoryEntry {

    public enum ActionType {
        COMPTE_AJOUTE   ("fas-plus-circle", "#22C55E", "Ajout compte"),
        COMPTE_MODIFIE  ("fas-edit",        "#3B82F6", "Modification compte"),
        COMPTE_SUPPRIME ("fas-trash-alt",   "#EF4444", "Suppression compte"),
        COFFRE_AJOUTE   ("fas-lock",        "#8B5CF6", "Ajout coffre"),
        COFFRE_MODIFIE  ("fas-pen",         "#F97316", "Modification coffre"),
        COFFRE_SUPPRIME ("fas-trash-alt",   "#EF4444", "Suppression coffre");

        public final String icon;
        public final String color;
        public final String label;

        ActionType(String icon, String color, String label) {
            this.icon = icon; this.color = color; this.label = label;
        }
    }

    // ── Champs ────────────────────────────────────────────────────────────────
    private final String        id;
    private final ActionType    type;
    private final String        description;
    private final LocalDateTime timestamp;

    // ── Constructeur standard : nouvelle entrée (génère UUID + now) ───────────
    public HistoryEntry(ActionType type, String description) {
        this.id          = UUID.randomUUID().toString();
        this.type        = type;
        this.description = description;
        this.timestamp   = LocalDateTime.now();
    }

    // ── Constructeur privé : reconstruction depuis le disque ──────────────────
    private HistoryEntry(String id, ActionType type,
                         String description, LocalDateTime timestamp) {
        this.id          = id;
        this.type        = type;
        this.description = description;
        this.timestamp   = timestamp;
    }

    /**
     * Factory utilisée par HistoryManager.readFromDisk() pour reconstruire
     * une entrée depuis le JSON persistant sans générer un nouvel UUID
     * ni écraser le timestamp d'origine.
     */
    public static HistoryEntry reconstruct(String id, ActionType type,
                                           String description, LocalDateTime timestamp) {
        return new HistoryEntry(id, type, description, timestamp);
    }

    // ── Accesseurs ────────────────────────────────────────────────────────────
    public String        getId()          { return id; }
    public ActionType    getType()        { return type; }
    public String        getDescription() { return description; }
    public LocalDateTime getTimestamp()   { return timestamp; }

    /** Retourne la date formatée dd/MM/yyyy HH:mm:ss pour l'affichage. */
    public String getFormattedDate() {
        return timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    /** Texte recherchable (type + description + date) en minuscules. */
    public String getSearchableText() {
        return (type.label + " " + description + " " + getFormattedDate()).toLowerCase();
    }
}