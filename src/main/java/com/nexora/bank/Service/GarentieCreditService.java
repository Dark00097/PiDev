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

    public GarentieCreditService() {
        ensureUserColumnsExist();
    }

    public List<GarantieCredit> getAllGaranties() {
        String sql = """
                SELECT idGarantie, idCredit, typeGarantie, description, adresseBien,
                       valeurEstimee, valeurRetenue, documentJustificatif,
                       dateEvaluation, nomGarant, statut, idUser
                FROM garantiecredit
                ORDER BY idGarantie DESC
                """;
        List<GarantieCredit> garanties = new ArrayList<>();
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                garanties.add(mapGarantie(resultSet));
            }
            return garanties;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch guarantees.", ex);
        }
    }

    public List<GarantieCredit> getGarantiesByUser(int idUser) {
        String sql = """
                SELECT g.idGarantie, g.idCredit, g.typeGarantie, g.description, g.adresseBien,
                       g.valeurEstimee, g.valeurRetenue, g.documentJustificatif,
                       g.dateEvaluation, g.nomGarant, g.statut, g.idUser
                FROM garantiecredit g
                LEFT JOIN credit c ON c.idCredit = g.idCredit
                WHERE g.idUser = ? OR c.idUser = ?
                ORDER BY g.idGarantie DESC
                """;
        List<GarantieCredit> garanties = new ArrayList<>();
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            statement.setInt(1, idUser);
            statement.setInt(2, idUser);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    garanties.add(mapGarantie(resultSet));
                }
            }
            return garanties;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch user guarantees.", ex);
        }
    }

    public GarantieCredit addGarantie(GarantieCredit garantie) {
        return addGarantieInternal(garantie, false);
    }

    public GarantieCredit addGarantieForUser(GarantieCredit garantie, int idUser) {
        garantie.setIdUser(idUser);
        return addGarantieInternal(garantie, true);
    }

    private GarantieCredit addGarantieInternal(GarantieCredit garantie, boolean forceUser) {
        String sql = """
                INSERT INTO garantiecredit (
                    idCredit, typeGarantie, description, adresseBien,
                    valeurEstimee, valeurRetenue, documentJustificatif,
                    dateEvaluation, nomGarant, statut, idUser
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = requireConnection().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            fillGarantieStatement(statement, garantie);
            statement.setInt(11, forceUser ? garantie.getIdUser() : Math.max(garantie.getIdUser(), 0));
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    garantie.setIdGarantie(generatedKeys.getInt(1));
                }
            }
            return garantie;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to add guarantee.", ex);
        }
    }

    public boolean updateGarantie(GarantieCredit garantie) {
        String sql = """
                UPDATE garantiecredit
                SET idCredit = ?, typeGarantie = ?, description = ?, adresseBien = ?,
                    valeurEstimee = ?, valeurRetenue = ?, documentJustificatif = ?,
                    dateEvaluation = ?, nomGarant = ?, statut = ?, idUser = ?
                WHERE idGarantie = ?
                """;
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            fillGarantieStatement(statement, garantie);
            statement.setInt(11, Math.max(garantie.getIdUser(), 0));
            statement.setInt(12, garantie.getIdGarantie());
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update guarantee.", ex);
        }
    }

    public boolean updateGarantieForUser(GarantieCredit garantie, int idUser) {
        String sql = """
                UPDATE garantiecredit
                SET idCredit = ?, typeGarantie = ?, description = ?, adresseBien = ?,
                    valeurEstimee = ?, valeurRetenue = ?, documentJustificatif = ?,
                    dateEvaluation = ?, nomGarant = ?, statut = ?, idUser = ?
                WHERE idGarantie = ? AND idUser = ?
                """;
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            fillGarantieStatement(statement, garantie);
            statement.setInt(11, idUser);
            statement.setInt(12, garantie.getIdGarantie());
            statement.setInt(13, idUser);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update user guarantee.", ex);
        }
    }

    public boolean deleteGarantie(int idGarantie) {
        String sql = "DELETE FROM garantiecredit WHERE idGarantie = ?";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            statement.setInt(1, idGarantie);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete guarantee.", ex);
        }
    }

    public boolean deleteGarantieForUser(int idGarantie, int idUser) {
        String sql = "DELETE FROM garantiecredit WHERE idGarantie = ? AND idUser = ?";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            statement.setInt(1, idGarantie);
            statement.setInt(2, idUser);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete user guarantee.", ex);
        }
    }

    public List<Integer> getCreditIds() {
        String sql = "SELECT idCredit FROM credit ORDER BY idCredit DESC";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getInt("idCredit"));
            }
            return ids;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch credit IDs.", ex);
        }
    }

    public List<Integer> getCreditIdsByUser(int idUser) {
        String sql = "SELECT idCredit FROM credit WHERE idUser = ? ORDER BY idCredit DESC";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            statement.setInt(1, idUser);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getInt("idCredit"));
                }
            }
            return ids;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch user credit IDs.", ex);
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
        return new GarantieCredit(
                resultSet.getInt("idGarantie"),
                resultSet.getInt("idCredit"),
                resultSet.getString("typeGarantie"),
                resultSet.getString("description"),
                resultSet.getString("adresseBien"),
                resultSet.getDouble("valeurEstimee"),
                resultSet.getDouble("valeurRetenue"),
                resultSet.getString("documentJustificatif"),
                resultSet.getString("dateEvaluation"),
                resultSet.getString("nomGarant"),
                resultSet.getString("statut"),
                resultSet.getInt("idUser")
        );
    }

    private Connection requireConnection() {
        if (connection == null) {
            throw new IllegalStateException("Database connection is unavailable.");
        }
        return connection;
    }

    private void ensureUserColumnsExist() {
        Connection conn = requireConnection();
        try {
            if (!hasColumn(conn, "credit", "idUser")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "ALTER TABLE credit ADD COLUMN idUser INT NOT NULL DEFAULT 0")) {
                    ps.executeUpdate();
                }
            }
            if (!hasColumn(conn, "garantiecredit", "idUser")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "ALTER TABLE garantiecredit ADD COLUMN idUser INT NOT NULL DEFAULT 0")) {
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to ensure user columns for guarantees.", ex);
        }
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }
}
