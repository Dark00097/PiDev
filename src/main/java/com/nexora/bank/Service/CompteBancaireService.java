package com.nexora.bank.Service;

import com.nexora.bank.Models.CompteBancaire;
import com.nexora.bank.Service.ICrud;
import com.nexora.bank.Utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompteBancaireService implements ICrud<CompteBancaire> {


    // il va avoir la connexion entre pas de donnee
    Connection conn;

    public CompteBancaireService() {
        this.conn = MyDB.getInstance().getConn();
    }

    @Override
    public void add(CompteBancaire compteBancaire) {
        String SQL = "INSERT INTO compte " +
                "(numeroCompte, solde, dateOuverture, statutCompte, plafondRetrait, plafondVirement, typeCompte) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setString(1, compteBancaire.getNumeroCompte());
            pst.setDouble(2, compteBancaire.getSolde());
            pst.setString(3, compteBancaire.getDateOuverture());
            pst.setString(4, compteBancaire.getStatutCompte());
            pst.setDouble(5, compteBancaire.getPlafondRetrait());
            pst.setDouble(6, compteBancaire.getPlafondVirement());
            pst.setString(7, compteBancaire.getTypeCompte());

            pst.executeUpdate();
            System.out.println("Compte Bancaire ajouté avec succès");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void edit(CompteBancaire compteBancaire) {

        String req = "UPDATE compte SET " +
                "numeroCompte = ?, " +
                "solde = ?, " +
                "dateOuverture = ?, " +
                "statutCompte = ?, " +
                "plafondRetrait = ?, " +
                "plafondVirement = ?, " +
                "typeCompte = ? " +
                "WHERE idCompte = ?";

        try {
            PreparedStatement pst = conn.prepareStatement(req);

            pst.setString(1, compteBancaire.getNumeroCompte());
            pst.setDouble(2, compteBancaire.getSolde());
            pst.setString(3, compteBancaire.getDateOuverture());
            pst.setString(4, compteBancaire.getStatutCompte());
            pst.setDouble(5, compteBancaire.getPlafondRetrait());
            pst.setDouble(6, compteBancaire.getPlafondVirement());
            pst.setString(7, compteBancaire.getTypeCompte());
            pst.setInt(8, compteBancaire.getIdCompte());

            pst.executeUpdate();
            System.out.println("Compte Bancaire modifié avec succès");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void remove(CompteBancaire compteBancaire) {

        String SQL = "DELETE FROM compte WHERE idCompte = ?";
        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setInt(1, compteBancaire.getIdCompte());
            pst.executeUpdate();
            System.out.println("Compte Bancaire supprimé avec succès");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    @Override
    public List<CompteBancaire> getAll() {
        String req = "SELECT * FROM `compte`" ;
        ArrayList<CompteBancaire> comptes = new ArrayList<>();
        Statement stm;
        try {
            stm = this.conn.createStatement();
            ResultSet rs=  stm.executeQuery(req);
            while (rs.next()){
                CompteBancaire comp = new CompteBancaire();
                comp.setIdCompte(rs.getInt("idCompte"));
                comp.setNumeroCompte(rs.getString("numeroCompte"));
                comp.setSolde(rs.getDouble("solde"));
                comp.setDateOuverture(rs.getString("dateOuverture"));
                comp.setStatutCompte(rs.getString("statutCompte"));
                comp.setPlafondRetrait(rs.getDouble("plafondRetrait"));
                comp.setPlafondVirement(rs.getDouble("plafondVirement"));
                comp.setTypeCompte(rs.getString("typeCompte"));
                comptes.add(comp);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return comptes;
    }
}
