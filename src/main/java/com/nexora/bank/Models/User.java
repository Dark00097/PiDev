package com.nexora.bank.Models;

public class User {

    private int idUser;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String role;
    private String status;
    private String password;
    private String createdAt;
    private String accountOpenedFrom;
    private String accountOpenedLocation;
    private Double accountOpenedLatitude;
    private Double accountOpenedLongitude;
    private String lastOnlineAt;
    private String lastOnlineFrom;
    private String profileImagePath;
    private boolean biometricEnabled;


    public User() {
    }


    public User(String nom, String prenom, String email, String telephone, String role, String status, String password) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.role = role;
        this.status = status;
        this.password = password;
    }

    public User(int idUser, String nom, String prenom, String email, String telephone, String role, String status, String password) {
        this.idUser = idUser;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.role = role;
        this.status = status;
        this.password = password;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getAccountOpenedFrom() {
        return accountOpenedFrom;
    }

    public void setAccountOpenedFrom(String accountOpenedFrom) {
        this.accountOpenedFrom = accountOpenedFrom;
    }

    public String getLastOnlineAt() {
        return lastOnlineAt;
    }

    public void setLastOnlineAt(String lastOnlineAt) {
        this.lastOnlineAt = lastOnlineAt;
    }

    public String getLastOnlineFrom() {
        return lastOnlineFrom;
    }

    public void setLastOnlineFrom(String lastOnlineFrom) {
        this.lastOnlineFrom = lastOnlineFrom;
    }

    public String getAccountOpenedLocation() {
        return accountOpenedLocation;
    }

    public void setAccountOpenedLocation(String accountOpenedLocation) {
        this.accountOpenedLocation = accountOpenedLocation;
    }

    public Double getAccountOpenedLatitude() {
        return accountOpenedLatitude;
    }

    public void setAccountOpenedLatitude(Double accountOpenedLatitude) {
        this.accountOpenedLatitude = accountOpenedLatitude;
    }

    public Double getAccountOpenedLongitude() {
        return accountOpenedLongitude;
    }

    public void setAccountOpenedLongitude(Double accountOpenedLongitude) {
        this.accountOpenedLongitude = accountOpenedLongitude;
    }

    public String getProfileImagePath() {
        return profileImagePath;
    }

    public void setProfileImagePath(String profileImagePath) {
        this.profileImagePath = profileImagePath;
    }

    public boolean isBiometricEnabled() {
        return biometricEnabled;
    }

    public void setBiometricEnabled(boolean biometricEnabled) {
        this.biometricEnabled = biometricEnabled;
    }

    @Override
    public String toString() {
        return "User{" +
                "idUser=" + idUser +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", telephone='" + telephone + '\'' +
                ", role='" + role + '\'' +
                ", status='" + status + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", accountOpenedFrom='" + accountOpenedFrom + '\'' +
                ", accountOpenedLocation='" + accountOpenedLocation + '\'' +
                ", accountOpenedLatitude=" + accountOpenedLatitude +
                ", accountOpenedLongitude=" + accountOpenedLongitude +
                ", lastOnlineAt='" + lastOnlineAt + '\'' +
                ", lastOnlineFrom='" + lastOnlineFrom + '\'' +
                ", profileImagePath='" + profileImagePath + '\'' +
                ", biometricEnabled=" + biometricEnabled +
                '}';
    }
}
