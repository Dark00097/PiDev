package com.nexora.bank.Service;

import com.nexora.bank.Models.Reclamation;
import com.nexora.bank.Models.ICrud;
import com.nexora.bank.Utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReclamationService implements ICrud<Reclamation> {

    private Connection conn;

    public ReclamationService() {
        this.conn = MyDB.getInstance().getConn();
    }

    @Override
    public void add(Reclamation r) {
        String SQL = "INSERT INTO reclamation (idUser, idTransaction, dateReclamation, typeReclamation, description, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setInt(1, r.getIdUser());
            pst.setInt(2, r.getIdTransaction());
            pst.setString(3, r.getDateReclamation());
            pst.setString(4, r.getTypeReclamation());
            pst.setString(5, r.getDescription());
            pst.setString(6, r.getStatus());
            pst.executeUpdate();
            System.out.println("Reclamation ajoutée avec succès");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void edit(Reclamation r) {
        String SQL = "UPDATE reclamation SET " +
                     "idUser = ?, idTransaction = ?, dateReclamation = ?, " +
                     "typeReclamation = ?, description = ?, status = ? " +
                     "WHERE idReclamation = ?";

        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setInt(1, r.getIdUser());
            pst.setInt(2, r.getIdTransaction());
            pst.setString(3, r.getDateReclamation());
            pst.setString(4, r.getTypeReclamation());
            pst.setString(5, r.getDescription());
            pst.setString(6, r.getStatus());
            pst.setInt(7, r.getIdReclamation());
            pst.executeUpdate();
            System.out.println("Reclamation modifiée avec succès");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void remove(Reclamation r) {
        String SQL = "DELETE FROM reclamation WHERE idReclamation = ?";
        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setInt(1, r.getIdReclamation());
            pst.executeUpdate();
            System.out.println("Reclamation supprimée avec succès");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Reclamation> getAll() {
        List<Reclamation> reclamations = new ArrayList<>();
        String SQL = "SELECT * FROM reclamation";

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);

            while (rs.next()) {
                Reclamation r = new Reclamation();
                r.setIdReclamation(rs.getInt("idReclamation"));
                r.setIdUser(rs.getInt("idUser"));
                r.setIdTransaction(rs.getInt("idTransaction"));
                r.setDateReclamation(rs.getString("dateReclamation"));
                r.setTypeReclamation(rs.getString("typeReclamation"));
                r.setDescription(rs.getString("description"));
                r.setStatus(rs.getString("status"));
                reclamations.add(r);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return reclamations;
    }

    public List<Reclamation> getByUser(int idUser) {
        List<Reclamation> reclamations = new ArrayList<>();
        String SQL = "SELECT * FROM reclamation WHERE idUser = ?";

        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setInt(1, idUser);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Reclamation r = new Reclamation();
                r.setIdReclamation(rs.getInt("idReclamation"));
                r.setIdUser(rs.getInt("idUser"));
                r.setIdTransaction(rs.getInt("idTransaction"));
                r.setDateReclamation(rs.getString("dateReclamation"));
                r.setTypeReclamation(rs.getString("typeReclamation"));
                r.setDescription(rs.getString("description"));
                r.setStatus(rs.getString("status"));
                reclamations.add(r);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return reclamations;
    }

    public List<Reclamation> getByTransaction(int idTransaction) {
        List<Reclamation> reclamations = new ArrayList<>();
        String SQL = "SELECT * FROM reclamation WHERE idTransaction = ?";

        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setInt(1, idTransaction);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Reclamation r = new Reclamation();
                r.setIdReclamation(rs.getInt("idReclamation"));
                r.setIdUser(rs.getInt("idUser"));
                r.setIdTransaction(rs.getInt("idTransaction"));
                r.setDateReclamation(rs.getString("dateReclamation"));
                r.setTypeReclamation(rs.getString("typeReclamation"));
                r.setDescription(rs.getString("description"));
                r.setStatus(rs.getString("status"));
                reclamations.add(r);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return reclamations;
    }
}