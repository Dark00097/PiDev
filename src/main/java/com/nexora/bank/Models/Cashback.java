package com.nexora.bank.Models;

public class Cashback {
    private int idCashback;
    private int idPartenaire;
    private double montantAchat;
    private double tauxApplique;
    private double montantCashback;
    private String dateAchat;
    private String dateCredit;
    private String dateExpiration;
    private String statut;

    public Cashback() {
    }

    public Cashback(int idPartenaire, double montantAchat, double tauxApplique, double montantCashback, String dateAchat, String dateCredit, String dateExpiration, String statut) {
        this.idPartenaire = idPartenaire;
        this.montantAchat = montantAchat;
        this.tauxApplique = tauxApplique;
        this.montantCashback = montantCashback;
        this.dateAchat = dateAchat;
        this.dateCredit = dateCredit;
        this.dateExpiration = dateExpiration;
        this.statut = statut;
    }

    public Cashback(int idCashback, int idPartenaire, double montantAchat, double tauxApplique, double montantCashback, String dateAchat, String dateCredit, String dateExpiration, String statut) {
        this.idCashback = idCashback;
        this.idPartenaire = idPartenaire;
        this.montantAchat = montantAchat;
        this.tauxApplique = tauxApplique;
        this.montantCashback = montantCashback;
        this.dateAchat = dateAchat;
        this.dateCredit = dateCredit;
        this.dateExpiration = dateExpiration;
        this.statut = statut;
    }

    public int getIdCashback() {
        return idCashback;
    }

    public void setIdCashback(int idCashback) {
        this.idCashback = idCashback;
    }

    public int getIdPartenaire() {
        return idPartenaire;
    }

    public void setIdPartenaire(int idPartenaire) {
        this.idPartenaire = idPartenaire;
    }

    public double getMontantAchat() {
        return montantAchat;
    }

    public void setMontantAchat(double montantAchat) {
        this.montantAchat = montantAchat;
    }

    public double getTauxApplique() {
        return tauxApplique;
    }

    public void setTauxApplique(double tauxApplique) {
        this.tauxApplique = tauxApplique;
    }

    public double getMontantCashback() {
        return montantCashback;
    }

    public void setMontantCashback(double montantCashback) {
        this.montantCashback = montantCashback;
    }

    public String getDateAchat() {
        return dateAchat;
    }

    public void setDateAchat(String dateAchat) {
        this.dateAchat = dateAchat;
    }

    public String getDateCredit() {
        return dateCredit;
    }

    public void setDateCredit(String dateCredit) {
        this.dateCredit = dateCredit;
    }

    public String getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(String dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "Cashback{" +
                "idCashback=" + idCashback +
                ", idPartenaire=" + idPartenaire +
                ", montantAchat=" + montantAchat +
                ", tauxApplique=" + tauxApplique +
                ", montantCashback=" + montantCashback +
                ", dateAchat='" + dateAchat + '\'' +
                ", dateCredit='" + dateCredit + '\'' +
                ", dateExpiration='" + dateExpiration + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}
