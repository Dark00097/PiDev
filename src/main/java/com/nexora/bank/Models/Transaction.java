package com.nexora.bank.Models;

public class Transaction {

    private int idTransaction;
    private String categorie;
    private String dateTransaction;
    private Double montant;
    private String typeTransaction;
    private String statutTransaction;
    private Double soldeApres;
    private String description;

    public Transaction() {
    }

    public Transaction(String categorie, String dateTransaction, Double montant, String typeTransaction, String statutTransaction, Double soldeApres, String description) {
        this.categorie = categorie;
        this.dateTransaction = dateTransaction;
        this.montant = montant;
        this.typeTransaction = typeTransaction;
        this.statutTransaction = statutTransaction;
        this.soldeApres = soldeApres;
        this.description = description;
    }

    public Transaction(int idTransaction, String categorie, String dateTransaction, Double montant, String typeTransaction, String statutTransaction, Double soldeApres, String description) {
        this.idTransaction = idTransaction;
        this.categorie = categorie;
        this.dateTransaction = dateTransaction;
        this.montant = montant;
        this.typeTransaction = typeTransaction;
        this.statutTransaction = statutTransaction;
        this.soldeApres = soldeApres;
        this.description = description;
    }

    public int getIdTransaction() {
        return idTransaction;
    }

    public void setIdTransaction(int idTransaction) {
        this.idTransaction = idTransaction;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getDateTransaction() {
        return dateTransaction;
    }

    public void setDateTransaction(String dateTransaction) {
        this.dateTransaction = dateTransaction;
    }

    public Double getMontant() {
        return montant;
    }

    public void setMontant(Double montant) {
        this.montant = montant;
    }

    public String getTypeTransaction() {
        return typeTransaction;
    }

    public void setTypeTransaction(String typeTransaction) {
        this.typeTransaction = typeTransaction;
    }

    public String getStatutTransaction() {
        return statutTransaction;
    }

    public void setStatutTransaction(String statutTransaction) {
        this.statutTransaction = statutTransaction;
    }

    public Double getSoldeApres() {
        return soldeApres;
    }

    public void setSoldeApres(Double soldeApres) {
        this.soldeApres = soldeApres;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "idTransaction=" + idTransaction +
                ", categorie='" + categorie + '\'' +
                ", dateTransaction='" + dateTransaction + '\'' +
                ", montant=" + montant +
                ", typeTransaction='" + typeTransaction + '\'' +
                ", statutTransaction='" + statutTransaction + '\'' +
                ", soldeApres=" + soldeApres +
                ", description='" + description + '\'' +
                '}';
    }
}
