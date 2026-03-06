package com.nexora.bank.Models;

public class Reclamation {

    private int     idReclamation;
    private int     idUser;
    private int     idTransaction;
    private String  dateReclamation;
    private String  typeReclamation;
    private String  description;
    private String  status;
    private boolean isInappropriate; // ✅ NOUVEAU
    private boolean isBlurred;       // ✅ NOUVEAU

    public Reclamation() {}

    // Constructeur sans id (ajout)
    public Reclamation(int idUser, int idTransaction, String dateReclamation,
                       String typeReclamation, String description, String status) {
        this.idUser          = idUser;
        this.idTransaction   = idTransaction;
        this.dateReclamation = dateReclamation;
        this.typeReclamation = typeReclamation;
        this.description     = description;
        this.status          = status;
        this.isInappropriate = false;
        this.isBlurred       = false;
    }

    // Constructeur avec id (modification)
    public Reclamation(int idReclamation, int idUser, int idTransaction,
                       String dateReclamation, String typeReclamation,
                       String description, String status) {
        this.idReclamation   = idReclamation;
        this.idUser          = idUser;
        this.idTransaction   = idTransaction;
        this.dateReclamation = dateReclamation;
        this.typeReclamation = typeReclamation;
        this.description     = description;
        this.status          = status;
        this.isInappropriate = false;
        this.isBlurred       = false;
    }

    // Constructeur complet avec flags
    public Reclamation(int idReclamation, int idUser, int idTransaction,
                       String dateReclamation, String typeReclamation,
                       String description, String status,
                       boolean isInappropriate, boolean isBlurred) {
        this.idReclamation   = idReclamation;
        this.idUser          = idUser;
        this.idTransaction   = idTransaction;
        this.dateReclamation = dateReclamation;
        this.typeReclamation = typeReclamation;
        this.description     = description;
        this.status          = status;
        this.isInappropriate = isInappropriate;
        this.isBlurred       = isBlurred;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int    getIdReclamation()   { return idReclamation; }
    public void   setIdReclamation(int v) { this.idReclamation = v; }

    public int    getIdUser()          { return idUser; }
    public void   setIdUser(int v)     { this.idUser = v; }

    public int    getIdTransaction()   { return idTransaction; }
    public void   setIdTransaction(int v) { this.idTransaction = v; }

    public String getDateReclamation() { return dateReclamation; }
    public void   setDateReclamation(String v) { this.dateReclamation = v; }

    public String getTypeReclamation() { return typeReclamation; }
    public void   setTypeReclamation(String v) { this.typeReclamation = v; }

    public String getDescription()     { return description; }
    public void   setDescription(String v) { this.description = v; }

    public String getStatus()          { return status; }
    public void   setStatus(String v)  { this.status = v; }

    public boolean isInappropriate()   { return isInappropriate; }
    public void    setInappropriate(boolean v) { this.isInappropriate = v; }

    public boolean isBlurred()         { return isBlurred; }
    public void    setBlurred(boolean v) { this.isBlurred = v; }

    @Override
    public String toString() {
        return "Reclamation{id=" + idReclamation +
               ", user=" + idUser +
               ", transaction=" + idTransaction +
               ", type='" + typeReclamation + '\'' +
               ", status='" + status + '\'' +
               ", inappropriate=" + isInappropriate +
               ", blurred=" + isBlurred + '}';
    }
}