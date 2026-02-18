//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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
    private final Connection connection = MyDB.getInstance().getConn();

    public List<Credit> getAllCredits() {
        String sql = "SELECT idCredit, idCompte, typeCredit, montantDemande, montantAccord,\n       duree, tauxInteret, mensualite, montantRestant, dateDemande, statut\nFROM credit\nORDER BY idCredit DESC\n";
        List<Credit> credits = new ArrayList();

        try {
            Object var5;
            try (
                PreparedStatement statement = this.requireConnection().prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();
            ) {
                while(resultSet.next()) {
                    credits.add(this.mapCredit(resultSet));
                }

                var5 = credits;
            }

            return (List<Credit>)var5;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch credits.", ex);
        }
    }

    public Credit addCredit(Credit credit) {
        String sql = "INSERT INTO credit (\n    idCompte, typeCredit, montantDemande, montantAccord, duree,\n    tauxInteret, mensualite, montantRestant, dateDemande, statut\n)\nVALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)\n";

        try {
            Credit var12;
            try (PreparedStatement statement = this.requireConnection().prepareStatement(sql, 1)) {
                this.fillCreditStatement(statement, credit);
                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        credit.setIdCredit(generatedKeys.getInt(1));
                    }
                }

                var12 = credit;
            }

            return var12;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to add credit.", ex);
        }
    }

    public boolean updateCredit(Credit credit) {
        String sql = "UPDATE credit\nSET idCompte = ?, typeCredit = ?, montantDemande = ?, montantAccord = ?,\n    duree = ?, tauxInteret = ?, mensualite = ?, montantRestant = ?,\n    dateDemande = ?, statut = ?\nWHERE idCredit = ?\n";

        try {
            boolean var4;
            try (PreparedStatement statement = this.requireConnection().prepareStatement(sql)) {
                this.fillCreditStatement(statement, credit);
                statement.setInt(11, credit.getIdCredit());
                var4 = statement.executeUpdate() > 0;
            }

            return var4;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update credit.", ex);
        }
    }

    public boolean deleteCredit(int idCredit) {
        String sql = "DELETE FROM credit WHERE idCredit = ?";

        try {
            boolean var4;
            try (PreparedStatement statement = this.requireConnection().prepareStatement(sql)) {
                statement.setInt(1, idCredit);
                var4 = statement.executeUpdate() > 0;
            }

            return var4;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete credit.", ex);
        }
    }

    public List<Integer> getCompteIds() {
        String sql = "SELECT idCompte FROM compte ORDER BY idCompte";
        List<Integer> ids = new ArrayList();

        try {
            Object var5;
            try (
                PreparedStatement statement = this.requireConnection().prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();
            ) {
                while(resultSet.next()) {
                    ids.add(resultSet.getInt("idCompte"));
                }

                var5 = ids;
            }

            return (List<Integer>)var5;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch account IDs.", ex);
        }
    }

    public List<Integer> getCreditIds() {
        String sql = "SELECT idCredit FROM credit ORDER BY idCredit DESC";
        List<Integer> ids = new ArrayList();

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

    private void fillCreditStatement(PreparedStatement statement, Credit credit) throws SQLException {
        statement.setInt(1, credit.getIdCompte());
        statement.setString(2, credit.getTypeCredit());
        statement.setDouble(3, credit.getMontantDemande());
        if (credit.getMontantAccord() == null) {
            statement.setNull(4, 8);
        } else {
            statement.setDouble(4, credit.getMontantAccord());
        }

        statement.setInt(5, credit.getDuree());
        statement.setDouble(6, credit.getTauxInteret());
        statement.setDouble(7, credit.getMensualite());
        statement.setDouble(8, credit.getMontantRestant());
        statement.setString(9, credit.getDateDemande());
        statement.setString(10, credit.getStatut());
    }

    private Credit mapCredit(ResultSet resultSet) throws SQLException {
        Double montantAccord = resultSet.getObject("montantAccord") != null ? resultSet.getDouble("montantAccord") : null;
        return new Credit(resultSet.getInt("idCredit"), resultSet.getInt("idCompte"), resultSet.getString("typeCredit"), resultSet.getDouble("montantDemande"), montantAccord, resultSet.getInt("duree"), resultSet.getDouble("tauxInteret"), resultSet.getDouble("mensualite"), resultSet.getDouble("montantRestant"), resultSet.getString("dateDemande"), resultSet.getString("statut"));
    }

    private Connection requireConnection() {
        if (this.connection == null) {
            throw new IllegalStateException("Database connection is unavailable.");
        } else {
            return this.connection;
        }
    }
    public int getTotalCredits() {
    String sql = "SELECT COUNT(*) FROM credit";
    try (PreparedStatement ps = connection.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        if (rs.next()) {
            return rs.getInt(1);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return 0;
}
public double getTotalMontantAccorde() {
   String sql = "SELECT SUM(montantAccord) FROM credit";
    try  (PreparedStatement ps = connection.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        if (rs.next()) {
            return rs.getDouble(1);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return 0;
}


}
