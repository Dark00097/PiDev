package com.nexora.bank.Models;

public class Credit {

    private int idCredit;
    private String typeCredit;
    private double montantDemande;
    private Double montantAccord; // nullable
    private int duree;
    private double tauxInteret;
    private double mensualite;
    private double montantRestant;
    private String dateDemande;
    private String statut;

    public Credit() {
    }

    public Credit(String typeCredit, double montantDemande, Double montantAccord, int duree, double tauxInteret, double mensualite, double montantRestant, String dateDemande, String statut) {
        this.typeCredit = typeCredit;
        this.montantDemande = montantDemande;
        this.montantAccord = montantAccord;
        this.duree = duree;
        this.tauxInteret = tauxInteret;
        this.mensualite = mensualite;
        this.montantRestant = montantRestant;
        this.dateDemande = dateDemande;
        this.statut = statut;
    }

    public Credit(int idCredit, String typeCredit, double montantDemande, Double montantAccord, int duree, double tauxInteret, double mensualite, double montantRestant, String dateDemande, String statut) {
        this.idCredit = idCredit;
        this.typeCredit = typeCredit;
        this.montantDemande = montantDemande;
        this.montantAccord = montantAccord;
        this.duree = duree;
        this.tauxInteret = tauxInteret;
        this.mensualite = mensualite;
        this.montantRestant = montantRestant;
        this.dateDemande = dateDemande;
        this.statut = statut;
    }

    public int getIdCredit() {
        return idCredit;
    }

    public void setIdCredit(int idCredit) {
        this.idCredit = idCredit;
    }

    public String getTypeCredit() {
        return typeCredit;
    }

    public void setTypeCredit(String typeCredit) {
        this.typeCredit = typeCredit;
    }

    public double getMontantDemande() {
        return montantDemande;
    }

    public void setMontantDemande(double montantDemande) {
        this.montantDemande = montantDemande;
    }

    public Double getMontantAccord() {
        return montantAccord;
    }

    public void setMontantAccord(Double montantAccord) {
        this.montantAccord = montantAccord;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public double getTauxInteret() {
        return tauxInteret;
    }

    public void setTauxInteret(double tauxInteret) {
        this.tauxInteret = tauxInteret;
    }

    public double getMensualite() {
        return mensualite;
    }

    public void setMensualite(double mensualite) {
        this.mensualite = mensualite;
    }

    public double getMontantRestant() {
        return montantRestant;
    }

    public void setMontantRestant(double montantRestant) {
        this.montantRestant = montantRestant;
    }

    public String getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(String dateDemande) {
        this.dateDemande = dateDemande;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "Credit{" +
                "idCredit=" + idCredit +
                ", typeCredit='" + typeCredit + '\'' +
                ", montantDemande=" + montantDemande +
                ", montantAccord=" + montantAccord +
                ", duree=" + duree +
                ", tauxInteret=" + tauxInteret +
                ", mensualite=" + mensualite +
                ", montantRestant=" + montantRestant +
                ", dateDemande='" + dateDemande + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}
