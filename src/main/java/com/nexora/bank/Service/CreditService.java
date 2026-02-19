package com.nexora.bank.Service;

import com.nexora.bank.Models.Credit;
import com.nexora.bank.Utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CreditService {
    private final Connection connection = MyDB.getInstance().getConn();

    public CreditService() {
        ensureUserColumnExists();
    }

    public List<Credit> getAllCredits() {
        String sql = """
                SELECT idCredit, idCompte, typeCredit, montantDemande, montantAccord,
                       duree, tauxInteret, mensualite, montantRestant, dateDemande, statut, idUser
                FROM credit
                ORDER BY idCredit DESC
                """;
        List<Credit> credits = new ArrayList<>();
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                credits.add(mapCredit(resultSet));
            }
            return credits;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch credits.", ex);
        }
    }

    public List<Credit> getCreditsByUser(int idUser) {
        String sql = """
                SELECT idCredit, idCompte, typeCredit, montantDemande, montantAccord,
                       duree, tauxInteret, mensualite, montantRestant, dateDemande, statut, idUser
                FROM credit
                WHERE idUser = ?
                ORDER BY idCredit DESC
                """;
        List<Credit> credits = new ArrayList<>();
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            statement.setInt(1, idUser);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    credits.add(mapCredit(resultSet));
                }
            }
            return credits;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch user credits.", ex);
        }
    }

    public Credit addCredit(Credit credit) {
        return addCreditInternal(credit, false);
    }

    public Credit addCreditForUser(Credit credit, int idUser) {
        credit.setIdUser(idUser);
        return addCreditInternal(credit, true);
    }

    private Credit addCreditInternal(Credit credit, boolean forceUser) {
        String sql = """
                INSERT INTO credit (
                    idCompte, typeCredit, montantDemande, montantAccord, duree,
                    tauxInteret, mensualite, montantRestant, dateDemande, statut, idUser
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = requireConnection().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            fillCreditStatement(statement, credit);
            // ✅ FIX: idUser — utilise NULL si 0 pour éviter violation de clé étrangère
            setIdUserParam(statement, 11, forceUser ? credit.getIdUser() : credit.getIdUser());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    credit.setIdCredit(generatedKeys.getInt(1));
                }
            }
            return credit;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to add credit.", ex);
        }
    }

    public boolean updateCredit(Credit credit) {
        String sql = """
                UPDATE credit
                SET idCompte = ?, typeCredit = ?, montantDemande = ?, montantAccord = ?,
                    duree = ?, tauxInteret = ?, mensualite = ?, montantRestant = ?,
                    dateDemande = ?, statut = ?, idUser = ?
                WHERE idCredit = ?
                """;
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            fillCreditStatement(statement, credit);
            // ✅ FIX: idUser — si 0 ou négatif, on met NULL au lieu de 0
            //         car 0 peut violer une contrainte de clé étrangère
            setIdUserParam(statement, 11, credit.getIdUser());
            statement.setInt(12, credit.getIdCredit());
            int rows = statement.executeUpdate();
            System.out.println("updateCredit → rows affected: " + rows + " for idCredit=" + credit.getIdCredit());
            return rows > 0;
        } catch (SQLException ex) {
            // ✅ FIX: On relance l'exception avec le message SQL pour mieux diagnostiquer
            throw new RuntimeException("Failed to update credit. SQLState=" + ex.getSQLState()
                    + " | Message=" + ex.getMessage(), ex);
        }
    }

    public boolean updateCreditForUser(Credit credit, int idUser) {
        String sql = """
                UPDATE credit
                SET idCompte = ?, typeCredit = ?, montantDemande = ?, montantAccord = ?,
                    duree = ?, tauxInteret = ?, mensualite = ?, montantRestant = ?,
                    dateDemande = ?, statut = ?, idUser = ?
                WHERE idCredit = ? AND idUser = ?
                """;
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            fillCreditStatement(statement, credit);
            statement.setInt(11, idUser);
            statement.setInt(12, credit.getIdCredit());
            statement.setInt(13, idUser);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update user credit.", ex);
        }
    }

    public boolean deleteCredit(int idCredit) {
        String sql = "DELETE FROM credit WHERE idCredit = ?";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            statement.setInt(1, idCredit);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete credit.", ex);
        }
    }

    public boolean deleteCreditForUser(int idCredit, int idUser) {
        String sql = "DELETE FROM credit WHERE idCredit = ? AND idUser = ?";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            statement.setInt(1, idCredit);
            statement.setInt(2, idUser);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete user credit.", ex);
        }
    }

    public List<Integer> getCompteIds() {
        String sql = "SELECT idCompte FROM compte ORDER BY idCompte";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getInt("idCompte"));
            }
            return ids;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch account IDs.", ex);
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

    public int getTotalCredits() {
        String sql = "SELECT COUNT(*) FROM credit";
        try (PreparedStatement ps = requireConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count credits.", e);
        }
        return 0;
    }

    public double getTotalMontantAccorde() {
        String sql = "SELECT SUM(montantAccord) FROM credit";
        try (PreparedStatement ps = requireConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to sum granted amount.", e);
        }
        return 0;
    }

    // ✅ FIX PRINCIPAL : fillCreditStatement remplit les paramètres 1 à 10
    //    idUser (11) est géré séparément via setIdUserParam()
    private void fillCreditStatement(PreparedStatement statement, Credit credit) throws SQLException {
        statement.setInt(1, credit.getIdCompte());
        statement.setString(2, credit.getTypeCredit());
        statement.setDouble(3, credit.getMontantDemande());
        if (credit.getMontantAccord() == null) {
            statement.setNull(4, Types.DOUBLE);
        } else {
            statement.setDouble(4, credit.getMontantAccord());
        }
        statement.setInt(5, credit.getDuree());
        statement.setDouble(6, credit.getTauxInteret());
        statement.setDouble(7, credit.getMensualite());
        statement.setDouble(8, credit.getMontantRestant());
        statement.setString(9, credit.getDateDemande());
        statement.setString(10, credit.getStatut());
        // NB: idUser (paramètre 11) est géré par setIdUserParam() pour éviter
        //     une violation de clé étrangère si idUser == 0
    }

    // ✅ FIX: Si idUser <= 0, on insère NULL plutôt que 0
    //         pour éviter une violation de contrainte FK (idUser=0 n'existe pas)
    private void setIdUserParam(PreparedStatement statement, int paramIndex, int idUser) throws SQLException {
        if (idUser <= 0) {
            statement.setNull(paramIndex, Types.INTEGER);
        } else {
            statement.setInt(paramIndex, idUser);
        }
    }

    private Credit mapCredit(ResultSet resultSet) throws SQLException {
        Double montantAccord = resultSet.getObject("montantAccord") != null
                ? resultSet.getDouble("montantAccord")
                : null;
        int idUser = resultSet.getInt("idUser");
        return new Credit(
                resultSet.getInt("idCredit"),
                resultSet.getInt("idCompte"),
                resultSet.getString("typeCredit"),
                resultSet.getDouble("montantDemande"),
                montantAccord,
                resultSet.getInt("duree"),
                resultSet.getDouble("tauxInteret"),
                resultSet.getDouble("mensualite"),
                resultSet.getDouble("montantRestant"),
                resultSet.getString("dateDemande"),
                resultSet.getString("statut"),
                idUser
        );
    }

    private Connection requireConnection() {
        if (connection == null) {
            throw new IllegalStateException("Database connection is unavailable.");
        }
        return connection;
    }

    private void ensureUserColumnExists() {
        Connection conn = requireConnection();
        try {
            if (!hasColumn(conn, "credit", "idUser")) {
                // ✅ FIX: DEFAULT NULL au lieu de DEFAULT 0 pour éviter les violations FK
                try (PreparedStatement ps = conn.prepareStatement(
                        "ALTER TABLE credit ADD COLUMN idUser INT DEFAULT NULL")) {
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to ensure credit.idUser column.", ex);
        }
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, tableName, columnName)) {
            return rs.next();
        }
    }
}