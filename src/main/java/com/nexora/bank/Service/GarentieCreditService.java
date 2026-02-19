package com.nexora.bank.Service;

import com.nexora.bank.Models.GarantieCredit;
import com.nexora.bank.Utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GarentieCreditService {

    public void addGarantieForUser(GarantieCredit garantie, int userId) {
        Connection conn = MyDB.getInstance().getConn();
        String sql = "INSERT INTO garantie_credit (idCredit, typeGarantie, description, adresseBien, valeurEstimee, valeurRetenue, documentJustificatif, dateEvaluation, nomGarant, statut, idUser) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, garantie.getIdCredit());
            ps.setString(2, garantie.getTypeGarantie());
            ps.setString(3, garantie.getDescription());
            ps.setString(4, garantie.getAdresseBien());
            ps.setDouble(5, garantie.getValeurEstimee());
            ps.setDouble(6, garantie.getValeurRetenue());
            ps.setString(7, garantie.getDocumentJustificatif());
            ps.setString(8, garantie.getDateEvaluation());
            ps.setString(9, garantie.getNomGarant());
            ps.setString(10, garantie.getStatut());
            ps.setInt(11, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding garantie: " + e.getMessage());
        }
    }

    public List<GarantieCredit> getAll() {
        List<GarantieCredit> garanties = new ArrayList<>();
        Connection conn = MyDB.getInstance().getConn();
        String sql = "SELECT * FROM garantie_credit";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                GarantieCredit g = new GarantieCredit();
                g.setIdGarantie(rs.getInt("idGarantie"));
                g.setIdCredit(rs.getInt("idCredit"));
                g.setTypeGarantie(rs.getString("typeGarantie"));
                g.setDescription(rs.getString("description"));
                g.setAdresseBien(rs.getString("adresseBien"));
                g.setValeurEstimee(rs.getDouble("valeurEstimee"));
                g.setValeurRetenue(rs.getDouble("valeurRetenue"));
                g.setDocumentJustificatif(rs.getString("documentJustificatif"));
                g.setDateEvaluation(rs.getString("dateEvaluation"));
                g.setNomGarant(rs.getString("nomGarant"));
                g.setStatut(rs.getString("statut"));
                garanties.add(g);
            }
        } catch (SQLException e) {
            System.err.println("Error getting garanties: " + e.getMessage());
        }
        return garanties;
    }
}
