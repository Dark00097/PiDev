package com.nexora.bank.Service;

import com.nexora.bank.Models.Cashback;
import com.nexora.bank.Utils.MyDB;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CashbackService {

    private final Connection connection;

    public CashbackService() {
        this.connection = MyDB.getInstance().getConn();
        ensureCashbackTable();
    }

    public List<Cashback> getAllCashbacks() {
        String sql = baseSelect() + " ORDER BY c.date_achat DESC, c.id_cashback DESC";
        return queryList(sql, null);
    }

    public List<Cashback> getCashbacksByUser(int idUser) {
        String sql = baseSelect() + " WHERE c.id_user = ? ORDER BY c.date_achat DESC, c.id_cashback DESC";
        return queryList(sql, ps -> ps.setInt(1, idUser));
    }

    public Optional<Cashback> findById(int idCashback) {
        String sql = baseSelect() + " WHERE c.id_cashback = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idCashback);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapCashback(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to find cashback.", ex);
        }
    }

    public int createCashback(Cashback cashback) {
        String sql = """
            INSERT INTO cashback_entries
                (id_user, id_partenaire, partenaire_nom, montant_achat, taux_applique, montant_cashback,
                 date_achat, date_credit, date_expiration, statut, transaction_ref)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillForWrite(ps, cashback);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to create cashback.", ex);
        }
    }

    public boolean updateCashbackAsAdmin(Cashback cashback) {
        String sql = """
            UPDATE cashback_entries
            SET id_user = ?, id_partenaire = ?, partenaire_nom = ?, montant_achat = ?, taux_applique = ?,
                montant_cashback = ?, date_achat = ?, date_credit = ?, date_expiration = ?, statut = ?, transaction_ref = ?
            WHERE id_cashback = ?
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            fillForWrite(ps, cashback);
            ps.setInt(12, cashback.getIdCashback());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update cashback.", ex);
        }
    }

    public boolean updateCashbackForUser(Cashback cashback, int idUser) {
        String sql = """
            UPDATE cashback_entries
            SET id_partenaire = ?, partenaire_nom = ?, montant_achat = ?, taux_applique = ?,
                montant_cashback = ?, date_achat = ?, date_credit = ?, date_expiration = ?, statut = ?, transaction_ref = ?
            WHERE id_cashback = ? AND id_user = ?
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            setNullableInt(ps, 1, cashback.getIdPartenaire());
            ps.setString(2, safeText(cashback.getPartenaireNom()));
            ps.setDouble(3, cashback.getMontantAchat());
            ps.setDouble(4, cashback.getTauxApplique());
            ps.setDouble(5, cashback.getMontantCashback());
            ps.setDate(6, toSqlDate(cashback.getDateAchat()));
            ps.setDate(7, toSqlDate(cashback.getDateCredit()));
            ps.setDate(8, toSqlDate(cashback.getDateExpiration()));
            ps.setString(9, safeText(cashback.getStatut()));
            ps.setString(10, safeText(cashback.getTransactionRef()));
            ps.setInt(11, cashback.getIdCashback());
            ps.setInt(12, idUser);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update cashback for user.", ex);
        }
    }

    public boolean deleteCashbackAsAdmin(int idCashback) {
        String sql = "DELETE FROM cashback_entries WHERE id_cashback = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idCashback);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete cashback.", ex);
        }
    }

    public boolean deleteCashbackForUser(int idCashback, int idUser) {
        String sql = "DELETE FROM cashback_entries WHERE id_cashback = ? AND id_user = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idCashback);
            ps.setInt(2, idUser);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete cashback for user.", ex);
        }
    }

    public double getCreditedTotalByUser(int idUser) {
        String sql = "SELECT COALESCE(SUM(montant_cashback), 0) FROM cashback_entries WHERE id_user = ? AND statut = 'Credite'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
                return 0;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to calculate credited total.", ex);
        }
    }

    public double getPendingTotalByUser(int idUser) {
        String sql = "SELECT COALESCE(SUM(montant_cashback), 0) FROM cashback_entries WHERE id_user = ? AND statut IN ('En attente', 'Valide')";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
                return 0;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to calculate pending total.", ex);
        }
    }

    public double getCurrentMonthTotalByUser(int idUser) {
        String sql = """
            SELECT COALESCE(SUM(montant_cashback), 0)
            FROM cashback_entries
            WHERE id_user = ?
              AND YEAR(date_achat) = YEAR(CURRENT_DATE())
              AND MONTH(date_achat) = MONTH(CURRENT_DATE())
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
                return 0;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to calculate monthly total.", ex);
        }
    }

    public long countActiveUsersWithCashback() {
        String sql = "SELECT COUNT(DISTINCT id_user) FROM cashback_entries";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to count users with cashback.", ex);
        }
    }

    public double getTotalCreditedGlobal() {
        String sql = "SELECT COALESCE(SUM(montant_cashback), 0) FROM cashback_entries WHERE statut = 'Credite'";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
            return 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to calculate global credited cashback.", ex);
        }
    }

    public double getCurrentMonthTotalGlobal() {
        String sql = """
            SELECT COALESCE(SUM(montant_cashback), 0)
            FROM cashback_entries
            WHERE YEAR(date_achat) = YEAR(CURRENT_DATE())
              AND MONTH(date_achat) = MONTH(CURRENT_DATE())
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
            return 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to calculate global month cashback.", ex);
        }
    }

    private interface StatementConfigurer {
        void configure(PreparedStatement ps) throws SQLException;
    }

    private List<Cashback> queryList(String sql, StatementConfigurer configurer) {
        List<Cashback> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (configurer != null) {
                configurer.configure(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapCashback(rs));
                }
            }
            return result;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to query cashback list.", ex);
        }
    }

    private String baseSelect() {
        return """
            SELECT c.id_cashback, c.id_user, CONCAT(COALESCE(u.prenom, ''), ' ', COALESCE(u.nom, '')) AS user_name,
                   c.id_partenaire, c.partenaire_nom, c.montant_achat, c.taux_applique, c.montant_cashback,
                   c.date_achat, c.date_credit, c.date_expiration, c.statut, c.transaction_ref
            FROM cashback_entries c
            LEFT JOIN users u ON u.idUser = c.id_user
            """;
    }

    private Cashback mapCashback(ResultSet rs) throws SQLException {
        Cashback cashback = new Cashback();
        cashback.setIdCashback(rs.getInt("id_cashback"));
        cashback.setIdUser(rs.getInt("id_user"));
        cashback.setUserDisplayName(safeText(rs.getString("user_name")).trim());

        int idPartenaire = rs.getInt("id_partenaire");
        cashback.setIdPartenaire(rs.wasNull() ? null : idPartenaire);

        cashback.setPartenaireNom(rs.getString("partenaire_nom"));
        cashback.setMontantAchat(rs.getDouble("montant_achat"));
        cashback.setTauxApplique(rs.getDouble("taux_applique"));
        cashback.setMontantCashback(rs.getDouble("montant_cashback"));

        Date dateAchat = rs.getDate("date_achat");
        Date dateCredit = rs.getDate("date_credit");
        Date dateExpiration = rs.getDate("date_expiration");
        cashback.setDateAchat(dateAchat == null ? null : dateAchat.toLocalDate());
        cashback.setDateCredit(dateCredit == null ? null : dateCredit.toLocalDate());
        cashback.setDateExpiration(dateExpiration == null ? null : dateExpiration.toLocalDate());

        cashback.setStatut(rs.getString("statut"));
        cashback.setTransactionRef(rs.getString("transaction_ref"));
        return cashback;
    }

    private void fillForWrite(PreparedStatement ps, Cashback cashback) throws SQLException {
        ps.setInt(1, cashback.getIdUser());
        setNullableInt(ps, 2, cashback.getIdPartenaire());
        ps.setString(3, safeText(cashback.getPartenaireNom()));
        ps.setDouble(4, cashback.getMontantAchat());
        ps.setDouble(5, cashback.getTauxApplique());
        ps.setDouble(6, cashback.getMontantCashback());
        ps.setDate(7, toSqlDate(cashback.getDateAchat()));
        ps.setDate(8, toSqlDate(cashback.getDateCredit()));
        ps.setDate(9, toSqlDate(cashback.getDateExpiration()));
        ps.setString(10, safeText(cashback.getStatut()));
        ps.setString(11, safeText(cashback.getTransactionRef()));
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null || value <= 0) {
            ps.setNull(index, java.sql.Types.INTEGER);
            return;
        }
        ps.setInt(index, value);
    }

    private Date toSqlDate(LocalDate localDate) {
        return localDate == null ? null : Date.valueOf(localDate);
    }

    private void ensureCashbackTable() {
        String createSql = """
            CREATE TABLE IF NOT EXISTS cashback_entries (
                id_cashback INT AUTO_INCREMENT PRIMARY KEY,
                id_user INT NOT NULL,
                id_partenaire INT NULL,
                partenaire_nom VARCHAR(120) NOT NULL,
                montant_achat DOUBLE NOT NULL,
                taux_applique DOUBLE NOT NULL,
                montant_cashback DOUBLE NOT NULL,
                date_achat DATE NOT NULL,
                date_credit DATE NULL,
                date_expiration DATE NULL,
                statut VARCHAR(30) NOT NULL,
                transaction_ref VARCHAR(120) NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_cashback_entries_user FOREIGN KEY (id_user) REFERENCES users(idUser) ON DELETE CASCADE,
                CONSTRAINT fk_cashback_entries_partenaire FOREIGN KEY (id_partenaire) REFERENCES partenaire(idPartenaire) ON DELETE SET NULL
            )
            """;

        try (Statement st = connection.createStatement()) {
            st.execute(createSql);
            ensureColumn("transaction_ref", "ALTER TABLE cashback_entries ADD COLUMN transaction_ref VARCHAR(120) NULL");
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to ensure cashback table.", ex);
        }
    }

    private void ensureColumn(String columnName, String alterSql) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, "cashback_entries", columnName)) {
            if (!columns.next()) {
                try (Statement st = connection.createStatement()) {
                    st.execute(alterSql);
                }
            }
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
