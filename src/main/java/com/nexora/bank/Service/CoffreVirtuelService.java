package com.nexora.bank.Service;

import com.nexora.bank.Models.CoffreVirtuel;
import com.nexora.bank.Utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CoffreVirtuelService {

    public List<CoffreVirtuel> getAll() {
        List<CoffreVirtuel> coffres = new ArrayList<>();
        Connection conn = MyDB.getInstance().getConn();
        String sql = "SELECT * FROM coffre_virtuel";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CoffreVirtuel c = new CoffreVirtuel();
                c.setIdCoffre(rs.getInt("idCoffre"));
                c.setNom(rs.getString("nom"));
                c.setObjectifMontant(rs.getDouble("objectifMontant"));
                c.setMontantActuel(rs.getDouble("montantActuel"));
                c.setDateCreation(rs.getString("dateCreation"));
                c.setDateObjectifs(rs.getString("dateObjectifs"));
                c.setStatus(rs.getString("status"));
                c.setEstVerrouille(rs.getBoolean("estVerrouille"));
                c.setIdCompte(rs.getInt("idCompte"));
                coffres.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Error getting coffres: " + e.getMessage());
        }
        return coffres;
    }
}
