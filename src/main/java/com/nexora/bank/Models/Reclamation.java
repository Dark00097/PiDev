package com.nexora.bank.Models;

public class Reclamation {

    private int idReclamation;
    private int idTransaction;
    private String dateReclamation;
    private String typeReclamation;
    private String description;
    private String status;

    public Reclamation() {
    }

    public Reclamation(int idTransaction, String dateReclamation, String typeReclamation, String description, String status) {
        this.idTransaction = idTransaction;
        this.dateReclamation = dateReclamation;
        this.typeReclamation = typeReclamation;
        this.description = description;
        this.status = status;
    }

    public Reclamation(int idReclamation, int idTransaction, String dateReclamation, String typeReclamation, String description, String status) {
        this.idReclamation = idReclamation;
        this.idTransaction = idTransaction;
        this.dateReclamation = dateReclamation;
        this.typeReclamation = typeReclamation;
        this.description = description;
        this.status = status;
    }

    public int getIdReclamation() {
        return idReclamation;
    }

    public void setIdReclamation(int idReclamation) {
        this.idReclamation = idReclamation;
    }

    public int getIdTransaction() {
        return idTransaction;
    }

    public void setIdTransaction(int idTransaction) {
        this.idTransaction = idTransaction;
    }

    public String getDateReclamation() {
        return dateReclamation;
    }

    public void setDateReclamation(String dateReclamation) {
        this.dateReclamation = dateReclamation;
    }

    public String getTypeReclamation() {
        return typeReclamation;
    }

    public void setTypeReclamation(String typeReclamation) {
        this.typeReclamation = typeReclamation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Reclamation{" +
                "idReclamation=" + idReclamation +
                ", idTransaction=" + idTransaction +
                ", dateReclamation='" + dateReclamation + '\'' +
                ", typeReclamation='" + typeReclamation + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
