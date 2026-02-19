package com.nexora.bank.Service;

import com.nexora.bank.Models.Credit;
import com.nexora.bank.Utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CreditService {

    public List<Integer> getCreditIdsByUser(int userId) {
        List<Integer> ids = new ArrayList<>();
        Connection conn = MyDB.getInstance().getConn();
        String sql = "SELECT idCredit FROM credit WHERE idUser = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt("idCredit"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting credit IDs: " + e.getMessage());
        }
        return ids;
    }

    public List<Credit> getAll() {
        List<Credit> credits = new ArrayList<>();
        Connection conn = MyDB.getInstance().getConn();
        String sql = "SELECT * FROM credit";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Credit c = new Credit();
                c.setIdCredit(rs.getInt("idCredit"));
                c.setTypeCredit(rs.getString("typeCredit"));
                c.setMontantDemande(rs.getDouble("montantDemande"));
                c.setMontantAccord(rs.getDouble("montantAccord"));
                c.setDuree(rs.getInt("duree"));
                c.setTauxInteret(rs.getDouble("tauxInteret"));
                c.setMensualite(rs.getDouble("mensualite"));
                c.setMontantRestant(rs.getDouble("montantRestant"));
                c.setDateDemande(rs.getString("dateDemande"));
                c.setStatut(rs.getString("statut"));
                credits.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Error getting credits: " + e.getMessage());
        }
        return credits;
    }
}
