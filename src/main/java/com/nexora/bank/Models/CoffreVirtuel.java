package com.nexora.bank.Models;

public class CoffreVirtuel {

    private int idCoffre;
    private String nom;
    private double objectifMontant;
    private double montantActuel;
    private String dateCreation;
    private String dateObjectifs;
    private String status;
    private boolean estVerrouille;
    private int idCompte;


    public CoffreVirtuel(){};

    public CoffreVirtuel(String nom, double objectifMontant, double montantActuel, String dateCreation, String dateObjectifs, String status, boolean estVerrouille, int idCompte) {
        this.nom = nom;
        this.objectifMontant = objectifMontant;
        this.montantActuel = montantActuel;
        this.dateCreation = dateCreation;
        this.dateObjectifs = dateObjectifs;
        this.status = status;
        this.estVerrouille = estVerrouille;
        this.idCompte = idCompte;
    }

    public CoffreVirtuel(int idCoffre, String nom, double objectifMontant, double montantActuel, String dateCreation, String dateObjectifs, String status, boolean estVerrouille, int idCompte) {
        this.idCoffre = idCoffre;
        this.nom = nom;
        this.objectifMontant = objectifMontant;
        this.montantActuel = montantActuel;
        this.dateCreation = dateCreation;
        this.dateObjectifs = dateObjectifs;
        this.status = status;
        this.estVerrouille = estVerrouille;
        this.idCompte = idCompte;
    }

    public int getIdCoffre() {
        return idCoffre;
    }

    public void setIdCoffre(int idCoffre) {
        this.idCoffre = idCoffre;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public double getObjectifMontant() {
        return objectifMontant;
    }

    public void setObjectifMontant(double objectifMontant) {
        this.objectifMontant = objectifMontant;
    }

    public double getMontantActuel() {
        return montantActuel;
    }

    public void setMontantActuel(double montantActuel) {
        this.montantActuel = montantActuel;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getDateObjectifs() {
        return dateObjectifs;
    }

    public void setDateObjectifs(String dateObjectifs) {
        this.dateObjectifs = dateObjectifs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isEstVerrouille() {
        return estVerrouille;
    }

    public void setEstVerrouille(boolean estVerrouille) {
        this.estVerrouille = estVerrouille;
    }

    public int getIdCompte() {
        return idCompte;
    }

    public void setIdCompte(int idCompte) {
        this.idCompte = idCompte;
    }

    @Override
    public String toString() {
        return "CoffreVirtuel{" +
                "idCoffre=" + idCoffre +
                ", nom='" + nom + '\'' +
                ", objectifMontant=" + objectifMontant +
                ", montantActuel=" + montantActuel +
                ", dateCreation='" + dateCreation + '\'' +
                ", dateObjectifs='" + dateObjectifs + '\'' +
                ", status='" + status + '\'' +
                ", estVerrouille=" + estVerrouille +
                ", idCompte=" + idCompte +
                '}';
    }
}
