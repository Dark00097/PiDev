package com.nexora.bank.Service;

import com.nexora.bank.Models.Cashback;
import com.nexora.bank.Utils.MyDB;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private static final double TIER_1_MAX_AMOUNT = 50.0;
    private static final double TIER_2_MAX_AMOUNT = 200.0;
    private static final double TIER_1_RATE_PERCENT = 1.0;
    private static final double TIER_2_RATE_PERCENT = 2.0;
    private static final double TIER_3_RATE_PERCENT = 3.0;
    private static final double RATING_BONUS_THRESHOLD = 4.0;
    private static final double RATING_BONUS_PERCENT = 1.0;

    private final Connection connection;
    private final NotificationService notificationService;

    public CashbackService() {
        this.connection = MyDB.getInstance().getConn();
        this.notificationService = this.connection == null ? null : new NotificationService(this.connection);
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
            applyAmountBasedRule(cashback);
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
            applyAmountBasedRule(cashback);
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
            applyAmountBasedRule(cashback);
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

    public double resolveTierRatePercent(double amount) {
        if (amount < TIER_1_MAX_AMOUNT) {
            return TIER_1_RATE_PERCENT;
        }
        if (amount <= TIER_2_MAX_AMOUNT) {
            return TIER_2_RATE_PERCENT;
        }
        return TIER_3_RATE_PERCENT;
    }

    public double calculateCashbackByAmount(double amount) {
        return calculateCashbackByAmountAndRating(amount, 0);
    }

    public double resolveRatingBonusPercent(double rating) {
        return rating > RATING_BONUS_THRESHOLD ? RATING_BONUS_PERCENT : 0.0;
    }

    public double resolveEffectiveRatePercent(double amount, double partnerRating) {
        double normalizedAmount = Math.max(amount, 0);
        double baseRate = resolveTierRatePercent(normalizedAmount);
        return baseRate + resolveRatingBonusPercent(partnerRating);
    }

    public double calculateCashbackByAmountAndRating(double amount, double partnerRating) {
        double normalizedAmount = Math.max(amount, 0);
        double ratePercent = resolveEffectiveRatePercent(normalizedAmount, partnerRating);
        return roundCurrency(normalizedAmount * (ratePercent / 100.0));
    }

    public boolean grantAdminReward(int idCashback, double bonusAmount, String rewardNote) {
        double normalizedBonus = roundCurrency(Math.max(bonusAmount, 0));
        if (normalizedBonus <= 0) {
            return false;
        }

        Optional<Cashback> cashbackOpt = findById(idCashback);
        if (cashbackOpt.isEmpty()) {
            return false;
        }

        Cashback cashback = cashbackOpt.get();
        cashback.setMontantCashback(roundCurrency(cashback.getMontantCashback() + normalizedBonus));

        String note = safeText(rewardNote);
        String rewardToken = note.isBlank()
            ? "ADMIN_REWARD +" + String.format("%.2f", normalizedBonus)
            : "ADMIN_REWARD +" + String.format("%.2f", normalizedBonus) + " (" + note + ")";
        cashback.setTransactionRef(appendTransactionRef(cashback.getTransactionRef(), rewardToken));

        if ("En attente".equalsIgnoreCase(safeText(cashback.getStatut()))) {
            cashback.setStatut("Valide");
        }

        String sql = """
            UPDATE cashback_entries
            SET montant_cashback = ?, statut = ?, transaction_ref = ?, bonus_decision = ?, bonus_note = ?
            WHERE id_cashback = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, cashback.getMontantCashback());
            ps.setString(2, safeText(cashback.getStatut()));
            ps.setString(3, safeText(cashback.getTransactionRef()));
            ps.setString(4, "Approved");
            ps.setString(5, safeText(rewardNote));
            ps.setInt(6, idCashback);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                Cashback updatedCashback = findById(idCashback).orElse(cashback);
                runNotificationSafely(() -> {
                    notificationService.notifyUserCashbackRewardGranted(updatedCashback, normalizedBonus, rewardNote);
                    notificationService.notifyAdminsCashbackRewardGranted(updatedCashback, normalizedBonus, rewardNote);
                });
            }
            return updated;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to grant admin reward.", ex);
        }
    }

    public boolean setAdminBonusDecision(int idCashback, boolean approved, String decisionNote) {
        String decision = approved ? "Approved" : "Rejected";
        Optional<Cashback> cashbackSnapshot = findById(idCashback);
        String sql = """
            UPDATE cashback_entries
            SET bonus_decision = ?, bonus_note = ?
            WHERE id_cashback = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, decision);
            ps.setString(2, safeText(decisionNote));
            ps.setInt(3, idCashback);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                Cashback target = cashbackSnapshot.orElseGet(() -> findById(idCashback).orElse(null));
                if (target != null) {
                    runNotificationSafely(() -> {
                        notificationService.notifyUserCashbackBonusDecision(target, approved, decisionNote);
                        notificationService.notifyAdminsCashbackBonusDecision(target, approved, decisionNote);
                    });
                }
            }
            return updated;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update bonus decision.", ex);
        }
    }

    public boolean submitUserCashbackRating(int idCashback, int idUser, double rating, String comment) {
        double normalized = normalizeRating(rating);
        String sql = """
            UPDATE cashback_entries
            SET user_rating = ?, user_rating_comment = ?, bonus_decision = 'Pending'
            WHERE id_cashback = ? AND id_user = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, normalized);
            ps.setString(2, safeText(comment));
            ps.setInt(3, idCashback);
            ps.setInt(4, idUser);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                Cashback target = findById(idCashback).orElseGet(() -> fallbackCashbackForRating(idCashback, idUser, normalized, comment));
                runNotificationSafely(() -> {
                    notificationService.notifyAdminsCashbackRatingSubmitted(target);
                    notificationService.notifyUserCashbackRatingSubmitted(target);
                });
            }
            return updated;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to submit cashback rating.", ex);
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
                   c.date_achat, c.date_credit, c.date_expiration, c.statut, c.transaction_ref,
                   c.user_rating, c.user_rating_comment, c.bonus_decision, c.bonus_note, c.created_at
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
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        cashback.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());

        cashback.setStatut(rs.getString("statut"));
        cashback.setTransactionRef(rs.getString("transaction_ref"));
        double rating = rs.getDouble("user_rating");
        cashback.setUserRating(rs.wasNull() ? null : normalizeRating(rating));
        cashback.setUserRatingComment(rs.getString("user_rating_comment"));
        cashback.setBonusDecision(rs.getString("bonus_decision"));
        cashback.setBonusNote(rs.getString("bonus_note"));
        return cashback;
    }

    private void applyAmountBasedRule(Cashback cashback) {
        if (cashback == null) {
            return;
        }
        double normalizedAmount = Math.max(cashback.getMontantAchat(), 0);
        double partnerRating = resolvePartnerRating(cashback);
        cashback.setMontantAchat(normalizedAmount);
        cashback.setTauxApplique(resolveEffectiveRatePercent(normalizedAmount, partnerRating));
        cashback.setMontantCashback(calculateCashbackByAmountAndRating(normalizedAmount, partnerRating));
    }

    private double resolvePartnerRating(Cashback cashback) {
        if (cashback == null) {
            return 0.0;
        }

        if (cashback.getIdPartenaire() != null && cashback.getIdPartenaire() > 0) {
            String sqlById = "SELECT rating FROM partenaire WHERE idPartenaire = ? LIMIT 1";
            try (PreparedStatement ps = connection.prepareStatement(sqlById)) {
                ps.setInt(1, cashback.getIdPartenaire());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return normalizeRating(rs.getDouble(1));
                    }
                }
            } catch (SQLException ignored) {
            }
        }

        String partnerName = safeText(cashback.getPartenaireNom());
        if (partnerName.isBlank()) {
            return 0.0;
        }

        String sqlByName = "SELECT rating FROM partenaire WHERE LOWER(TRIM(nom)) = LOWER(TRIM(?)) LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sqlByName)) {
            ps.setString(1, partnerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return normalizeRating(rs.getDouble(1));
                }
            }
        } catch (SQLException ignored) {
        }
        return 0.0;
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

    private double roundCurrency(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String appendTransactionRef(String existing, String token) {
        String current = safeText(existing);
        String addition = safeText(token);
        if (addition.isBlank()) {
            return current;
        }
        if (current.isBlank()) {
            return addition;
        }
        return current + " | " + addition;
    }

    private double normalizeRating(double rating) {
        if (rating < 0) {
            return 0;
        }
        if (rating > 5) {
            return 5;
        }
        return rating;
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
                user_rating DOUBLE NULL,
                user_rating_comment VARCHAR(255) NULL,
                bonus_decision VARCHAR(20) NOT NULL DEFAULT 'Pending',
                bonus_note VARCHAR(255) NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_cashback_entries_user FOREIGN KEY (id_user) REFERENCES users(idUser) ON DELETE CASCADE,
                CONSTRAINT fk_cashback_entries_partenaire FOREIGN KEY (id_partenaire) REFERENCES partenaire(idPartenaire) ON DELETE SET NULL
            )
            """;

        try (Statement st = connection.createStatement()) {
            st.execute(createSql);
            ensureColumn("transaction_ref", "ALTER TABLE cashback_entries ADD COLUMN transaction_ref VARCHAR(120) NULL");
            ensureColumn("user_rating", "ALTER TABLE cashback_entries ADD COLUMN user_rating DOUBLE NULL");
            ensureColumn("user_rating_comment", "ALTER TABLE cashback_entries ADD COLUMN user_rating_comment VARCHAR(255) NULL");
            ensureColumn("bonus_decision", "ALTER TABLE cashback_entries ADD COLUMN bonus_decision VARCHAR(20) NOT NULL DEFAULT 'Pending'");
            ensureColumn("bonus_note", "ALTER TABLE cashback_entries ADD COLUMN bonus_note VARCHAR(255) NULL");
            ensureColumn("created_at", "ALTER TABLE cashback_entries ADD COLUMN created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP");
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

    private Cashback fallbackCashbackForRating(int idCashback, int idUser, double rating, String comment) {
        Cashback cashback = new Cashback();
        cashback.setIdCashback(idCashback);
        cashback.setIdUser(idUser);
        cashback.setPartenaireNom("");
        cashback.setUserRating(normalizeRating(rating));
        cashback.setUserRatingComment(safeText(comment));
        return cashback;
    }

    private void runNotificationSafely(Runnable action) {
        if (notificationService == null || action == null) {
            return;
        }
        try {
            action.run();
        } catch (Exception ex) {
            System.err.println("Notification dispatch failed: " + ex.getMessage());
        }
    }
}
