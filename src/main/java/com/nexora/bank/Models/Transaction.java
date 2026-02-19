package com.nexora.bank.Models;

import java.time.LocalDate;

public class Transaction {

    private int idTransaction;
    private int idUser;
    private String categorie;
    private LocalDate dateTransaction;
    private Double montant;
    private String typeTransaction;
    private String statutTransaction;
    private String description;

    public Transaction() {}

    public Transaction(int idUser, String categorie, LocalDate dateTransaction, Double montant, String typeTransaction,
                       String statutTransaction, String description) {
        this.idUser = idUser;
        this.categorie = categorie;
        this.dateTransaction = dateTransaction;
        this.montant = montant;
        this.typeTransaction = typeTransaction;
        this.statutTransaction = statutTransaction;
        this.description = description;
    }

    public Transaction(int idTransaction, int idUser, String categorie, LocalDate dateTransaction, Double montant,
                       String typeTransaction, String statutTransaction, String description) {
        this.idTransaction = idTransaction;
        this.idUser = idUser;
        this.categorie = categorie;
        this.dateTransaction = dateTransaction;
        this.montant = montant;
        this.typeTransaction = typeTransaction;
        this.statutTransaction = statutTransaction;
        this.description = description;
    }

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

    @Override
    public String toString() {
        return "Transaction{" +
                "idTransaction=" + idTransaction +
                ", idUser=" + idUser +
                ", categorie='" + categorie + '\'' +
                ", dateTransaction=" + dateTransaction +
                ", montant=" + montant +
                ", typeTransaction='" + typeTransaction + '\'' +
                ", statutTransaction='" + statutTransaction + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}