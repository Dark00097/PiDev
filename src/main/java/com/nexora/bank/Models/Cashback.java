package com.nexora.bank.Models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Cashback {
    private int idCashback;
    private int idUser;
    private String userDisplayName;
    private Integer idPartenaire;
    private String partenaireNom;
    private double montantAchat;
    private double tauxApplique;
    private double montantCashback;
    private LocalDate dateAchat;
    private LocalDate dateCredit;
    private LocalDate dateExpiration;
    private LocalDateTime createdAt;
    private String statut;
    private String transactionRef;
    private Double userRating;
    private String userRatingComment;
    private String bonusDecision;
    private String bonusNote;

    public Cashback() {
    }

    public Cashback(int idUser, Integer idPartenaire, String partenaireNom, double montantAchat, double tauxApplique,
                    double montantCashback, LocalDate dateAchat, LocalDate dateCredit, LocalDate dateExpiration,
                    String statut, String transactionRef) {
        this.idUser = idUser;
        this.idPartenaire = idPartenaire;
        this.partenaireNom = partenaireNom;
        this.montantAchat = montantAchat;
        this.tauxApplique = tauxApplique;
        this.montantCashback = montantCashback;
        this.dateAchat = dateAchat;
        this.dateCredit = dateCredit;
        this.dateExpiration = dateExpiration;
        this.statut = statut;
        this.transactionRef = transactionRef;
    }

    public int getIdCashback() {
        return idCashback;
    }

    public void setIdCashback(int idCashback) {
        this.idCashback = idCashback;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    public Integer getIdPartenaire() {
        return idPartenaire;
    }

    public void setIdPartenaire(Integer idPartenaire) {
        this.idPartenaire = idPartenaire;
    }

    public String getPartenaireNom() {
        return partenaireNom;
    }

    public void setPartenaireNom(String partenaireNom) {
        this.partenaireNom = partenaireNom;
    }

    public double getMontantAchat() {
        return montantAchat;
    }

    public void setMontantAchat(double montantAchat) {
        this.montantAchat = montantAchat;
    }

    public double getTauxApplique() {
        return tauxApplique;
    }

    public void setTauxApplique(double tauxApplique) {
        this.tauxApplique = tauxApplique;
    }

    public double getMontantCashback() {
        return montantCashback;
    }

    public void setMontantCashback(double montantCashback) {
        this.montantCashback = montantCashback;
    }

    public LocalDate getDateAchat() {
        return dateAchat;
    }

    public void setDateAchat(LocalDate dateAchat) {
        this.dateAchat = dateAchat;
    }

    public LocalDate getDateCredit() {
        return dateCredit;
    }

    public void setDateCredit(LocalDate dateCredit) {
        this.dateCredit = dateCredit;
    }

    public LocalDate getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public Double getUserRating() {
        return userRating;
    }

    public void setUserRating(Double userRating) {
        this.userRating = userRating;
    }

    public String getUserRatingComment() {
        return userRatingComment;
    }

    public void setUserRatingComment(String userRatingComment) {
        this.userRatingComment = userRatingComment;
    }

    public String getBonusDecision() {
        return bonusDecision;
    }

    public void setBonusDecision(String bonusDecision) {
        this.bonusDecision = bonusDecision;
    }

    public String getBonusNote() {
        return bonusNote;
    }

    public void setBonusNote(String bonusNote) {
        this.bonusNote = bonusNote;
    }

    @Override
    public String toString() {
        return "Cashback{" +
                "idCashback=" + idCashback +
                ", idUser=" + idUser +
                ", userDisplayName='" + userDisplayName + '\'' +
                ", idPartenaire=" + idPartenaire +
                ", partenaireNom='" + partenaireNom + '\'' +
                ", montantAchat=" + montantAchat +
                ", tauxApplique=" + tauxApplique +
                ", montantCashback=" + montantCashback +
                ", dateAchat=" + dateAchat +
                ", dateCredit=" + dateCredit +
                ", dateExpiration=" + dateExpiration +
                ", createdAt=" + createdAt +
                ", statut='" + statut + '\'' +
                ", transactionRef='" + transactionRef + '\'' +
                ", userRating=" + userRating +
                ", userRatingComment='" + userRatingComment + '\'' +
                ", bonusDecision='" + bonusDecision + '\'' +
                ", bonusNote='" + bonusNote + '\'' +
                '}';
    }
}
