//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.nexora.bank.Models;

public class Credit {
    private int idCredit;
    private int idCompte;
    private String typeCredit;
    private double montantDemande;
    private Double montantAccord;
    private int duree;
    private double tauxInteret;
    private double mensualite;
    private double montantRestant;
    private String dateDemande;
    private String statut;
    private int idUser;

    public Credit() {
    }

    public Credit(String typeCredit, double montantDemande, Double montantAccord, int duree, double tauxInteret, double mensualite, double montantRestant, String dateDemande, String statut, int idUser) {
        this.idCompte = 0;
        this.typeCredit = typeCredit;
        this.montantDemande = montantDemande;
        this.montantAccord = montantAccord;
        this.duree = duree;
        this.tauxInteret = tauxInteret;
        this.mensualite = mensualite;
        this.montantRestant = montantRestant;
        this.dateDemande = dateDemande;
        this.statut = statut;
        this.idUser = idUser;
    }

    public Credit(int idCredit, String typeCredit, double montantDemande, Double montantAccord, int duree, double tauxInteret, double mensualite, double montantRestant, String dateDemande, String statut) {
        this.idCredit = idCredit;
        this.idCompte = 0;
        this.typeCredit = typeCredit;
        this.montantDemande = montantDemande;
        this.montantAccord = montantAccord;
        this.duree = duree;
        this.tauxInteret = tauxInteret;
        this.mensualite = mensualite;
        this.montantRestant = montantRestant;
        this.dateDemande = dateDemande;
        this.statut = statut;
        this.idUser = 0;
    }

    public Credit(int idCredit, int idCompte, String typeCredit, double montantDemande, Double montantAccord, int duree, double tauxInteret, double mensualite, double montantRestant, String dateDemande, String statut) {
        this(idCredit, idCompte, typeCredit, montantDemande, montantAccord, duree, tauxInteret, mensualite, montantRestant, dateDemande, statut, 0);
    }

    public Credit(int idCredit, int idCompte, String typeCredit, double montantDemande, Double montantAccord, int duree, double tauxInteret, double mensualite, double montantRestant, String dateDemande, String statut, int idUser) {
        this.idCredit = idCredit;
        this.idCompte = idCompte;
        this.typeCredit = typeCredit;
        this.montantDemande = montantDemande;
        this.montantAccord = montantAccord;
        this.duree = duree;
        this.tauxInteret = tauxInteret;
        this.mensualite = mensualite;
        this.montantRestant = montantRestant;
        this.dateDemande = dateDemande;
        this.statut = statut;
        this.idUser = idUser;
    }

    public int getIdCredit() {
        return this.idCredit;
    }

    public void setIdCredit(int idCredit) {
        this.idCredit = idCredit;
    }

    public int getIdCompte() {
        return this.idCompte;
    }

    public void setIdCompte(int idCompte) {
        this.idCompte = idCompte;
    }

    public String getTypeCredit() {
        return this.typeCredit;
    }

    public void setTypeCredit(String typeCredit) {
        this.typeCredit = typeCredit;
    }

    public double getMontantDemande() {
        return this.montantDemande;
    }

    public void setMontantDemande(double montantDemande) {
        this.montantDemande = montantDemande;
    }

    public Double getMontantAccord() {
        return this.montantAccord;
    }

    public void setMontantAccord(Double montantAccord) {
        this.montantAccord = montantAccord;
    }

    public int getDuree() {
        return this.duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public double getTauxInteret() {
        return this.tauxInteret;
    }

    public void setTauxInteret(double tauxInteret) {
        this.tauxInteret = tauxInteret;
    }

    public double getMensualite() {
        return this.mensualite;
    }

    public void setMensualite(double mensualite) {
        this.mensualite = mensualite;
    }

    public double getMontantRestant() {
        return this.montantRestant;
    }

    public void setMontantRestant(double montantRestant) {
        this.montantRestant = montantRestant;
    }

    public String getDateDemande() {
        return this.dateDemande;
    }

    public void setDateDemande(String dateDemande) {
        this.dateDemande = dateDemande;
    }

    public String getStatut() {
        return this.statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getIdUser() {
        return this.idUser;
    }
    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public String toString() {
        return "Credit{idCredit=" + idCredit + ", idCompte=" + idCompte + ", typeCredit='" + typeCredit + "', montantDemande=" + montantDemande + ", montantAccord=" + montantAccord + ", duree=" + this.duree + ", tauxInteret=" + this.tauxInteret + ", mensualite=" + this.mensualite + ", montantRestant=" + this.montantRestant + ", dateDemande='" + dateDemande + "', statut='" + statut + ", idUser="  + idUser + "'}";
    }
    
}
