//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.nexora.bank.Models;

public class GarantieCredit {
    private int idGarantie;
    private int idCredit;
    private String typeGarantie;
    private String description;
    private String adresseBien;
    private double valeurEstimee;
    private double valeurRetenue;
    private String documentJustificatif;
    private String dateEvaluation;
    private String nomGarant;
    private String statut;
    private int idUser;

    public GarantieCredit() {
    }

    public GarantieCredit(int idCredit, String typeGarantie, String description, String adresseBien, double valeurEstimee, double valeurRetenue, String documentJustificatif, String dateEvaluation, String nomGarant, String statut) {
        this.idCredit = idCredit;
        this.typeGarantie = typeGarantie;
        this.description = description;
        this.adresseBien = adresseBien;
        this.valeurEstimee = valeurEstimee;
        this.valeurRetenue = valeurRetenue;
        this.documentJustificatif = documentJustificatif;
        this.dateEvaluation = dateEvaluation;
        this.nomGarant = nomGarant;
        this.statut = statut;
        this.idUser = 0;
    }

    public GarantieCredit(int idGarantie, int idCredit, String typeGarantie, String description, String adresseBien, double valeurEstimee, double valeurRetenue, String documentJustificatif, String dateEvaluation, String nomGarant, String statut) {
        this(idGarantie, idCredit, typeGarantie, description, adresseBien, valeurEstimee, valeurRetenue, documentJustificatif, dateEvaluation, nomGarant, statut, 0);
    }

    public GarantieCredit(int idGarantie, int idCredit, String typeGarantie, String description, String adresseBien, double valeurEstimee, double valeurRetenue, String documentJustificatif, String dateEvaluation, String nomGarant, String statut, int idUser) {
        this.idGarantie = idGarantie;
        this.idCredit = idCredit;
        this.typeGarantie = typeGarantie;
        this.description = description;
        this.adresseBien = adresseBien;
        this.valeurEstimee = valeurEstimee;
        this.valeurRetenue = valeurRetenue;
        this.documentJustificatif = documentJustificatif;
        this.dateEvaluation = dateEvaluation;
        this.nomGarant = nomGarant;
        this.statut = statut;
        this.idUser = idUser;
    }

    public int getIdGarantie() {
        return this.idGarantie;
    }

    public void setIdGarantie(int idGarantie) {
        this.idGarantie = idGarantie;
    }

    public int getIdCredit() {
        return this.idCredit;
    }

    public void setIdCredit(int idCredit) {
        this.idCredit = idCredit;
    }

    public String getTypeGarantie() {
        return this.typeGarantie;
    }

    public void setTypeGarantie(String typeGarantie) {
        this.typeGarantie = typeGarantie;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAdresseBien() {
        return this.adresseBien;
    }

    public void setAdresseBien(String adresseBien) {
        this.adresseBien = adresseBien;
    }

    public double getValeurEstimee() {
        return this.valeurEstimee;
    }

    public void setValeurEstimee(double valeurEstimee) {
        this.valeurEstimee = valeurEstimee;
    }

    public double getValeurRetenue() {
        return this.valeurRetenue;
    }

    public void setValeurRetenue(double valeurRetenue) {
        this.valeurRetenue = valeurRetenue;
    }

    public String getDocumentJustificatif() {
        return this.documentJustificatif;
    }

    public void setDocumentJustificatif(String documentJustificatif) {
        this.documentJustificatif = documentJustificatif;
    }

    public String getDateEvaluation() {
        return this.dateEvaluation;
    }

    public void setDateEvaluation(String dateEvaluation) {
        this.dateEvaluation = dateEvaluation;
    }

    public String getNomGarant() {
        return this.nomGarant;
    }

    public void setNomGarant(String nomGarant) {
        this.nomGarant = nomGarant;
    }

    public String getStatut() {
        return this.statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
    public int getIdUser(){
        return this.idUser;
    }
    public void setIdUser(int idUser){
        this.idUser = idUser;
    }
    public String toString() {
        return "GarantieCredit{idGarantie=" + this.idGarantie + ", idCredit=" + this.idCredit + ", typeGarantie='" + this.typeGarantie + "', description='" + this.description + "', adresseBien='" + this.adresseBien + "', valeurEstimee=" + this.valeurEstimee + ", valeurRetenue=" + this.valeurRetenue + ", documentJustificatif='" + this.documentJustificatif + "', dateEvaluation='" + this.dateEvaluation + "', nomGarant='" + this.nomGarant + "', statut='" + this.statut + "', idUser=" + this.idUser + "'}";
    }
}
