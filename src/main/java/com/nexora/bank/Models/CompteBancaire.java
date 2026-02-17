package com.nexora.bank.Models;

public class CompteBancaire {

    private int idCompte;
    private String numeroCompte;
    private double solde;
    private String dateOuverture;
    private String statutCompte;
    private double plafondRetrait;
    private double plafondVirement;
    private String typeCompte;

    public CompteBancaire() {
    }

    public CompteBancaire(String numeroCompte, double solde, String dateOuverture, String statutCompte, double plafondRetrait, double plafondVirement, String typeCompte) {
        this.numeroCompte = numeroCompte;
        this.solde = solde;
        this.dateOuverture = dateOuverture;
        this.statutCompte = statutCompte;
        this.plafondRetrait = plafondRetrait;
        this.plafondVirement = plafondVirement;
        this.typeCompte = typeCompte;
    }

    public CompteBancaire(int idCompte, String numeroCompte, double solde, String dateOuverture, String statutCompte, double plafondRetrait, double plafondVirement, String typeCompte) {
        this.idCompte = idCompte;
        this.numeroCompte = numeroCompte;
        this.solde = solde;
        this.dateOuverture = dateOuverture;
        this.statutCompte = statutCompte;
        this.plafondRetrait = plafondRetrait;
        this.plafondVirement = plafondVirement;
        this.typeCompte = typeCompte;
    }

    public int getIdCompte() {
        return idCompte;
    }

    public void setIdCompte(int idCompte) {
        this.idCompte = idCompte;
    }

    public String getNumeroCompte() {
        return numeroCompte;
    }

    public void setNumeroCompte(String numeroCompte) {
        this.numeroCompte = numeroCompte;
    }

    public double getSolde() {
        return solde;
    }

    public void setSolde(double solde) {
        this.solde = solde;
    }

    public String getDateOuverture() {
        return dateOuverture;
    }

    public void setDateOuverture(String dateOuverture) {
        this.dateOuverture = dateOuverture;
    }

    public String getStatutCompte() {
        return statutCompte;
    }

    public void setStatutCompte(String statutCompte) {
        this.statutCompte = statutCompte;
    }

    public double getPlafondRetrait() {
        return plafondRetrait;
    }

    public void setPlafondRetrait(double plafondRetrait) {
        this.plafondRetrait = plafondRetrait;
    }

    public double getPlafondVirement() {
        return plafondVirement;
    }

    public void setPlafondVirement(double plafondVirement) {
        this.plafondVirement = plafondVirement;
    }

    public String getTypeCompte() {
        return typeCompte;
    }

    public void setTypeCompte(String typeCompte) {
        this.typeCompte = typeCompte;
    }

    @Override
    public String toString() {
        return "CompteBancaire{" +
                "idCompte=" + idCompte +
                ", numeroCompte='" + numeroCompte + '\'' +
                ", solde=" + solde +
                ", dateOuverture='" + dateOuverture + '\'' +
                ", statutCompte='" + statutCompte + '\'' +
                ", plafondRetrait=" + plafondRetrait +
                ", plafondVirement=" + plafondVirement +
                ", typeCompte='" + typeCompte + '\'' +
                '}';
    }
}
