package com.nexora.bank.Models;

import java.time.LocalDate;

public class Transaction {

    private int       idTransaction;
    private int       idUser;
    private String    categorie;
    private LocalDate dateTransaction;
    private Double    montant;
    private String    typeTransaction;
    private String    statutTransaction;
    private String    description;
    private Double    montantPaye; // ✅ Nouveau : montant payé via Stripe

    // ─── Constructeurs ────────────────────────────────────────────────────────

    public Transaction() {}

    public Transaction(int idUser, String categorie, LocalDate dateTransaction, Double montant,
                       String typeTransaction, String statutTransaction, String description) {
        this.idUser            = idUser;
        this.categorie         = categorie;
        this.dateTransaction   = dateTransaction;
        this.montant           = montant;
        this.typeTransaction   = typeTransaction;
        this.statutTransaction = statutTransaction;
        this.description       = description;
        this.montantPaye       = 0.0;
    }

    public Transaction(int idTransaction, int idUser, String categorie, LocalDate dateTransaction,
                       Double montant, String typeTransaction, String statutTransaction, String description) {
        this.idTransaction     = idTransaction;
        this.idUser            = idUser;
        this.categorie         = categorie;
        this.dateTransaction   = dateTransaction;
        this.montant           = montant;
        this.typeTransaction   = typeTransaction;
        this.statutTransaction = statutTransaction;
        this.description       = description;
        this.montantPaye       = 0.0;
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public int getIdTransaction() { return idTransaction; }
    public void setIdTransaction(int idTransaction) { this.idTransaction = idTransaction; }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public LocalDate getDateTransaction() { return dateTransaction; }
    public void setDateTransaction(LocalDate dateTransaction) { this.dateTransaction = dateTransaction; }

    public Double getMontant() { return montant; }
    public void setMontant(Double montant) { this.montant = montant; }

    public String getTypeTransaction() { return typeTransaction; }
    public void setTypeTransaction(String typeTransaction) { this.typeTransaction = typeTransaction; }

    public String getStatutTransaction() { return statutTransaction; }
    public void setStatutTransaction(String statutTransaction) { this.statutTransaction = statutTransaction; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getMontantPaye() { return montantPaye != null ? montantPaye : 0.0; }
    public void setMontantPaye(Double montantPaye) { this.montantPaye = montantPaye; }

    // ─── Méthodes utilitaires pour la progression ─────────────────────────────

    /**
     * Retourne le pourcentage payé (0 à 100)
     * Ex: montant=1000, montantPaye=500 → 50.0
     */
    public double getProgressionPourcentage() {
        if (montant == null || montant == 0) return 0;
        double paye = getMontantPaye();
        return Math.min((paye / montant) * 100.0, 100.0);
    }

    /**
     * Retourne le montant restant à payer
     * Ex: montant=1000, montantPaye=500 → 500.0
     */
    public double getMontantRestant() {
        return Math.max(0, (montant != null ? montant : 0) - getMontantPaye());
    }

    /**
     * Vérifie si la transaction est totalement payée
     */
    public boolean isFullyPaid() {
        return getProgressionPourcentage() >= 100.0;
    }

    // ─── toString ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Transaction{" +
                "idTransaction=" + idTransaction +
                ", idUser=" + idUser +
                ", categorie='" + categorie + '\'' +
                ", dateTransaction=" + dateTransaction +
                ", montant=" + montant +
                ", montantPaye=" + montantPaye +
                ", progression=" + String.format("%.1f", getProgressionPourcentage()) + "%" +
                ", typeTransaction='" + typeTransaction + '\'' +
                ", statutTransaction='" + statutTransaction + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}