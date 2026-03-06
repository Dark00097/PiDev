package com.nexora.bank.Models;

public class Partenaire {

    private int idPartenaire;
    private String nom;
    private String categorie;
    private String description;
    private double tauxCashback;
    private double tauxCashbackMax;
    private double plafondMensuel;
    private String conditions;
    private String status;

    public Partenaire() {
    }

    public Partenaire(String nom, String categorie, String description, double tauxCashback, double tauxCashbackMax, double plafondMensuel, String conditions, String status) {
        this.nom = nom;
        this.categorie = categorie;
        this.description = description;
        this.tauxCashback = tauxCashback;
        this.tauxCashbackMax = tauxCashbackMax;
        this.plafondMensuel = plafondMensuel;
        this.conditions = conditions;
        this.status = status;
    }

    public Partenaire(int idPartenaire, String nom, String categorie, String description, double tauxCashback, double tauxCashbackMax, double plafondMensuel, String conditions, String status) {
        this.idPartenaire = idPartenaire;
        this.nom = nom;
        this.categorie = categorie;
        this.description = description;
        this.tauxCashback = tauxCashback;
        this.tauxCashbackMax = tauxCashbackMax;
        this.plafondMensuel = plafondMensuel;
        this.conditions = conditions;
        this.status = status;
    }

    public int getIdPartenaire() {
        return idPartenaire;
    }

    public void setIdPartenaire(int idPartenaire) {
        this.idPartenaire = idPartenaire;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getTauxCashback() {
        return tauxCashback;
    }

    public void setTauxCashback(double tauxCashback) {
        this.tauxCashback = tauxCashback;
    }

    public double getTauxCashbackMax() {
        return tauxCashbackMax;
    }

    public void setTauxCashbackMax(double tauxCashbackMax) {
        this.tauxCashbackMax = tauxCashbackMax;
    }

    public double getPlafondMensuel() {
        return plafondMensuel;
    }

    public void setPlafondMensuel(double plafondMensuel) {
        this.plafondMensuel = plafondMensuel;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Partenaire{" +
                "idPartenaire=" + idPartenaire +
                ", nom='" + nom + '\'' +
                ", categorie='" + categorie + '\'' +
                ", description='" + description + '\'' +
                ", tauxCashback=" + tauxCashback +
                ", tauxCashbackMax=" + tauxCashbackMax +
                ", plafondMensuel=" + plafondMensuel +
                ", conditions='" + conditions + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
