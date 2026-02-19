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
    }

    public GarantieCredit(int idGarantie, int idCredit, String typeGarantie, String description, String adresseBien, double valeurEstimee, double valeurRetenue, String documentJustificatif, String dateEvaluation, String nomGarant, String statut) {
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
    }

    public int getIdGarantie() {
        return idGarantie;
    }

    public void setIdGarantie(int idGarantie) {
        this.idGarantie = idGarantie;
    }

    public int getIdCredit() {
        return idCredit;
    }

    public void setIdCredit(int idCredit) {
        this.idCredit = idCredit;
    }

    public String getTypeGarantie() {
        return typeGarantie;
    }

    public void setTypeGarantie(String typeGarantie) {
        this.typeGarantie = typeGarantie;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAdresseBien() {
        return adresseBien;
    }

    public void setAdresseBien(String adresseBien) {
        this.adresseBien = adresseBien;
    }

    public double getValeurEstimee() {
        return valeurEstimee;
    }

    public void setValeurEstimee(double valeurEstimee) {
        this.valeurEstimee = valeurEstimee;
    }

    public double getValeurRetenue() {
        return valeurRetenue;
    }

    public void setValeurRetenue(double valeurRetenue) {
        this.valeurRetenue = valeurRetenue;
    }

    public String getDocumentJustificatif() {
        return documentJustificatif;
    }

    public void setDocumentJustificatif(String documentJustificatif) {
        this.documentJustificatif = documentJustificatif;
    }

    public String getDateEvaluation() {
        return dateEvaluation;
    }

    public void setDateEvaluation(String dateEvaluation) {
        this.dateEvaluation = dateEvaluation;
    }

    public String getNomGarant() {
        return nomGarant;
    }

    public void setNomGarant(String nomGarant) {
        this.nomGarant = nomGarant;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    @Override
    public String toString() {
        return "GarantieCredit{" +
                "idGarantie=" + idGarantie +
                ", idCredit=" + idCredit +
                ", typeGarantie='" + typeGarantie + '\'' +
                ", description='" + description + '\'' +
                ", adresseBien='" + adresseBien + '\'' +
                ", valeurEstimee=" + valeurEstimee +
                ", valeurRetenue=" + valeurRetenue +
                ", documentJustificatif='" + documentJustificatif + '\'' +
                ", dateEvaluation='" + dateEvaluation + '\'' +
                ", nomGarant='" + nomGarant + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}
