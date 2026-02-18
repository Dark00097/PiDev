//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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
    private final Connection connection = MyDB.getInstance().getConn();

    public List<GarantieCredit> getAllGaranties() {
        String sql = "SELECT idGarantie, idCredit, typeGarantie, description, adresseBien,\n       valeurEstimee, valeurRetenue, documentJustificatif,\n       dateEvaluation, nomGarant, statut\nFROM garantiecredit\nORDER BY idGarantie DESC\n";
        List<GarantieCredit> garanties = new ArrayList<>();


        try {
            Object var5;
            try (
                PreparedStatement statement = this.requireConnection().prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();
            ) {
                while(resultSet.next()) {
                    garanties.add(this.mapGarantie(resultSet));
                }

                var5 = garanties;
            }

            return (List<GarantieCredit>)var5;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch guarantees.", ex);
        }
    }

    public GarantieCredit addGarantie(GarantieCredit garantie) {
        String sql = "INSERT INTO garantiecredit (\n    idCredit, typeGarantie, description, adresseBien,\n    valeurEstimee, valeurRetenue, documentJustificatif,\n    dateEvaluation, nomGarant, statut\n)\nVALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)\n";

        try {
            GarantieCredit var12;
            try (PreparedStatement statement = this.requireConnection().prepareStatement(sql, 1)) {
                this.fillGarantieStatement(statement, garantie);
                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        garantie.setIdGarantie(generatedKeys.getInt(1));
                    }
                }

                var12 = garantie;
            }

            return var12;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to add guarantee.", ex);
        }
    }

    public boolean updateGarantie(GarantieCredit garantie) {
        String sql = "UPDATE garantiecredit\nSET idCredit = ?, typeGarantie = ?, description = ?, adresseBien = ?,\n    valeurEstimee = ?, valeurRetenue = ?, documentJustificatif = ?,\n    dateEvaluation = ?, nomGarant = ?, statut = ?\nWHERE idGarantie = ?\n";

        try {
            boolean var4;
            try (PreparedStatement statement = this.requireConnection().prepareStatement(sql)) {
                this.fillGarantieStatement(statement, garantie);
                statement.setInt(11, garantie.getIdGarantie());
                var4 = statement.executeUpdate() > 0;
            }

            return var4;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update guarantee.", ex);
        }
    }

    public boolean deleteGarantie(int idGarantie) {
        String sql = "DELETE FROM garantiecredit WHERE idGarantie = ?";

        try {
            boolean var4;
            try (PreparedStatement statement = this.requireConnection().prepareStatement(sql)) {
                statement.setInt(1, idGarantie);
                var4 = statement.executeUpdate() > 0;
            }

            return var4;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete guarantee.", ex);
        }
    }

    public List<Integer> getCreditIds() {
        String sql = "SELECT idCredit FROM credit ORDER BY idCredit DESC";
        List<Integer> ids = new ArrayList<>();

        try {
            Object var5;
            try (
                PreparedStatement statement = this.requireConnection().prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();
            ) {
                while(resultSet.next()) {
                    ids.add(resultSet.getInt("idCredit"));
                }

                var5 = ids;
            }

            return (List<Integer>)var5;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch credit IDs.", ex);
        }
    }

    private void fillGarantieStatement(PreparedStatement statement, GarantieCredit garantie) throws SQLException {
        statement.setInt(1, garantie.getIdCredit());
        statement.setString(2, garantie.getTypeGarantie());
        statement.setString(3, garantie.getDescription());
        statement.setString(4, garantie.getAdresseBien());
        statement.setDouble(5, garantie.getValeurEstimee());
        statement.setDouble(6, garantie.getValeurRetenue());
        statement.setString(7, garantie.getDocumentJustificatif());
        statement.setString(8, garantie.getDateEvaluation());
        statement.setString(9, garantie.getNomGarant());
        statement.setString(10, garantie.getStatut());
    }

    private GarantieCredit mapGarantie(ResultSet resultSet) throws SQLException {
        return new GarantieCredit(resultSet.getInt("idGarantie"), resultSet.getInt("idCredit"), resultSet.getString("typeGarantie"), resultSet.getString("description"), resultSet.getString("adresseBien"), resultSet.getDouble("valeurEstimee"), resultSet.getDouble("valeurRetenue"), resultSet.getString("documentJustificatif"), resultSet.getString("dateEvaluation"), resultSet.getString("nomGarant"), resultSet.getString("statut"));
    }

    private Connection requireConnection() {
        if (this.connection == null) {
            throw new IllegalStateException("Database connection is unavailable.");
        } else {
            return this.connection;
        }
    }
}
