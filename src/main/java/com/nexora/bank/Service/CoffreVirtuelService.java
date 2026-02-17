package com.nexora.bank.Service;

import com.nexora.bank.Models.CoffreVirtuel;
import com.nexora.bank.Service.ICrud;
import com.nexora.bank.Utils.MyDB;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CoffreVirtuelService implements ICrud<CoffreVirtuel> {

    Connection conn;

    public CoffreVirtuelService() {
        this.conn = MyDB.getInstance().getConn();
    }

    @Override
    public void add(CoffreVirtuel coffreVirtuel) {

        String SQL = "INSERT INTO coffrevirtuel " +
                "(nom, objectifMontant, montantActuel, dateCreation, dateObjectifs, status, estVerrouille, idCompte) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement pst = conn.prepareStatement(SQL);

            pst.setString(1, coffreVirtuel.getNom());
            pst.setDouble(2, coffreVirtuel.getObjectifMontant());
            pst.setDouble(3, coffreVirtuel.getMontantActuel());
            pst.setString(4, coffreVirtuel.getDateCreation());
            pst.setString(5, coffreVirtuel.getDateObjectifs());
            pst.setString(6, coffreVirtuel.getStatus());
            pst.setBoolean(7, coffreVirtuel.isEstVerrouille());
            pst.setInt(8, coffreVirtuel.getIdCompte());

            pst.executeUpdate();
            System.out.println("Coffre Virtuel ajouté avec succès");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    @Override
    public void edit(CoffreVirtuel coffreVirtuel) {

        String req = "UPDATE coffrevirtuel " +
                "SET nom=?, objectifMontant=?, montantActuel=?, dateCreation=?, dateObjectifs=?, status=?, estVerrouille=?, idCompte=? " +
                "WHERE idCoffre=?";

        try {
            PreparedStatement pst = conn.prepareStatement(req);

            pst.setString(1, coffreVirtuel.getNom());
            pst.setDouble(2, coffreVirtuel.getObjectifMontant());
            pst.setDouble(3, coffreVirtuel.getMontantActuel());
            pst.setString(4, coffreVirtuel.getDateCreation());
            pst.setString(5, coffreVirtuel.getDateObjectifs());
            pst.setString(6, coffreVirtuel.getStatus());
            pst.setBoolean(7, coffreVirtuel.isEstVerrouille());
            pst.setInt(8, coffreVirtuel.getIdCompte());
            pst.setInt(9, coffreVirtuel.getIdCoffre());

            pst.executeUpdate();
            System.out.println("Coffre Virtuel modifié avec succès");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void remove(CoffreVirtuel coffreVirtuel) {

        String SQL = "DELETE FROM coffrevirtuel WHERE idCoffre = ?";

        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setInt(1, coffreVirtuel.getIdCoffre());
            pst.executeUpdate();
            System.out.println("Coffre Virtuel supprimé avec succès");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<CoffreVirtuel> getAll() {
        String req = "SELECT * FROM coffrevirtuel";
        ArrayList<CoffreVirtuel> coffres = new ArrayList<>();

        try {
            Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {

                CoffreVirtuel coffre = new CoffreVirtuel();
                coffre.setIdCoffre(rs.getInt("idCoffre"));
                coffre.setNom(rs.getString("nom"));
                coffre.setObjectifMontant(rs.getDouble("objectifMontant"));
                coffre.setMontantActuel(rs.getDouble("montantActuel"));
                coffre.setDateCreation(rs.getString("dateCreation"));
                coffre.setDateObjectifs(rs.getString("dateObjectifs"));
                coffre.setStatus(rs.getString("status"));
                coffre.setEstVerrouille(rs.getBoolean("estVerrouille"));
                coffre.setIdCompte(rs.getInt("idCompte"));

                coffres.add(coffre);
            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return coffres;
    }

}

