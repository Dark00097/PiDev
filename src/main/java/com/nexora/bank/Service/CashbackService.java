package com.nexora.bank.Service;

import com.nexora.bank.Models.Cashback;
import com.nexora.bank.Models.Partenaire;
import com.nexora.bank.Utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared service for cashback operations used by both admin and user-facing controllers.
 * Encapsulates all database queries, calculations, and business logic for cashback rewards.
 */
public class CashbackService {

    private final Connection conn;

    public CashbackService() {
        this.conn = MyDB.getInstance().getConn();
    }

    // ═══════════════════ CASHBACK CRUD ═══════════════════

    /**
     * Insert a new cashback record into the database.
     */
    public boolean addCashback(Cashback cashback) {
        String sql = "INSERT INTO cashback (idPartenaire, idTransaction, idUser, montantAchat, tauxApplique, " +
                     "montantCashback, dateAchat, dateCredit, dateExpiration, statut) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, cashback.getIdPartenaire());
            ps.setInt(2, cashback.getIdTransaction());
            ps.setInt(3, cashback.getIdUser());
            ps.setDouble(4, cashback.getMontantAchat());
            ps.setDouble(5, cashback.getTauxApplique());
            ps.setDouble(6, cashback.getMontantCashback());
            ps.setString(7, cashback.getDateAchat());
            ps.setString(8, cashback.getDateCredit());
            ps.setString(9, cashback.getDateExpiration());
            ps.setString(10, cashback.getStatut());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) {
                    cashback.setIdCashback(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error adding cashback: " + e.getMessage());
        }
        return false;
    }

    /**
     * Update an existing cashback record.
     */
    public boolean updateCashback(Cashback cashback) {
        String sql = "UPDATE cashback SET idPartenaire=?, idTransaction=?, idUser=?, montantAchat=?, " +
                     "tauxApplique=?, montantCashback=?, dateAchat=?, dateCredit=?, dateExpiration=?, statut=? " +
                     "WHERE idCashback=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cashback.getIdPartenaire());
            ps.setInt(2, cashback.getIdTransaction());
            ps.setInt(3, cashback.getIdUser());
            ps.setDouble(4, cashback.getMontantAchat());
            ps.setDouble(5, cashback.getTauxApplique());
            ps.setDouble(6, cashback.getMontantCashback());
            ps.setString(7, cashback.getDateAchat());
            ps.setString(8, cashback.getDateCredit());
            ps.setString(9, cashback.getDateExpiration());
            ps.setString(10, cashback.getStatut());
            ps.setInt(11, cashback.getIdCashback());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating cashback: " + e.getMessage());
        }
        return false;
    }

    /**
     * Delete a cashback record by ID.
     */
    public boolean deleteCashback(int idCashback) {
        String sql = "DELETE FROM cashback WHERE idCashback=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCashback);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting cashback: " + e.getMessage());
        }
        return false;
    }

    // ═══════════════════ ADMIN QUERIES (ALL RECORDS) ═══════════════════

    /**
     * Retrieve all cashback records with partner names (admin view).
     */
    public List<Cashback> getAllCashbacks() {
        List<Cashback> list = new ArrayList<>();
        String sql = "SELECT c.*, p.nom AS partenaireNom, p.categorie AS partenaireCategorie " +
                     "FROM cashback c LEFT JOIN partenaire p ON c.idPartenaire = p.idPartenaire " +
                     "ORDER BY c.dateAchat DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToCashback(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all cashbacks: " + e.getMessage());
        }
        return list;
    }

    /**
     * Get total cashback amount across all users (admin stat).
     */
    public double getTotalCashbackAll() {
        String sql = "SELECT COALESCE(SUM(montantCashback), 0) FROM cashback WHERE statut = 'Credite'";
        return querySingleDouble(sql);
    }

    /**
     * Get count of unique beneficiaries (admin stat).
     */
    public long getBeneficiaryCount() {
        String sql = "SELECT COUNT(DISTINCT idUser) FROM cashback WHERE statut IN ('Credite', 'Valide')";
        return querySingleLong(sql);
    }

    /**
     * Get total cashback for the current month (admin stat).
     */
    public double getCurrentMonthCashbackAll() {
        String sql = "SELECT COALESCE(SUM(montantCashback), 0) FROM cashback " +
                     "WHERE dateAchat LIKE CONCAT(DATE_FORMAT(CURDATE(), '%Y-%m'), '%')";
        return querySingleDouble(sql);
    }

    // ═══════════════════ USER QUERIES (FILTERED BY USER) ═══════════════════

    /**
     * Retrieve cashback records for a specific user with partner details.
     * This is the primary query for the user-facing rewards page.
     */
    public List<Cashback> getCashbacksByUserId(int userId) {
        List<Cashback> list = new ArrayList<>();
        String sql = "SELECT c.*, p.nom AS partenaireNom, p.categorie AS partenaireCategorie " +
                     "FROM cashback c LEFT JOIN partenaire p ON c.idPartenaire = p.idPartenaire " +
                     "WHERE c.idUser = ? ORDER BY c.dateAchat DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToCashback(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user cashbacks: " + e.getMessage());
        }
        return list;
    }

    /**
     * Get total earned cashback for a user (only credited/approved).
     */
    public double getTotalEarnedByUser(int userId) {
        String sql = "SELECT COALESCE(SUM(montantCashback), 0) FROM cashback " +
                     "WHERE idUser = ? AND statut = 'Credite'";
        return querySingleDoubleParam(sql, userId);
    }

    /**
     * Get available (redeemable) cashback balance for a user.
     * Available = Credited and not expired.
     */
    public double getAvailableBalanceByUser(int userId) {
        String sql = "SELECT COALESCE(SUM(montantCashback), 0) FROM cashback " +
                     "WHERE idUser = ? AND statut = 'Credite' " +
                     "AND (dateExpiration IS NULL OR dateExpiration >= DATE_FORMAT(CURDATE(), '%Y-%m-%d'))";
        return querySingleDoubleParam(sql, userId);
    }

    /**
     * Get pending cashback amount for a user.
     */
    public double getPendingByUser(int userId) {
        String sql = "SELECT COALESCE(SUM(montantCashback), 0) FROM cashback " +
                     "WHERE idUser = ? AND statut IN ('En attente', 'Valide')";
        return querySingleDoubleParam(sql, userId);
    }

    /**
     * Get cashback earned this month for a user.
     */
    public double getCurrentMonthByUser(int userId) {
        String sql = "SELECT COALESCE(SUM(montantCashback), 0) FROM cashback " +
                     "WHERE idUser = ? AND dateAchat LIKE CONCAT(DATE_FORMAT(CURDATE(), '%Y-%m'), '%')";
        return querySingleDoubleParam(sql, userId);
    }

    /**
     * Get count of pending transactions for a user.
     */
    public int getPendingCountByUser(int userId) {
        String sql = "SELECT COUNT(*) FROM cashback WHERE idUser = ? AND statut IN ('En attente', 'Valide')";
        return (int) querySingleLongParam(sql, userId);
    }

    /**
     * Get total redeemed cashback for a user.
     */
    public double getRedeemedByUser(int userId) {
        String sql = "SELECT COALESCE(SUM(montantCashback), 0) FROM cashback " +
                     "WHERE idUser = ? AND statut = 'Echange'";
        return querySingleDoubleParam(sql, userId);
    }

    /**
     * Get count of active partners the user has earned cashback from.
     */
    public int getActivePartnerCountByUser(int userId) {
        String sql = "SELECT COUNT(DISTINCT idPartenaire) FROM cashback " +
                     "WHERE idUser = ? AND statut IN ('Credite', 'Valide', 'En attente')";
        return (int) querySingleLongParam(sql, userId);
    }

    // ═══════════════════ PARTNER QUERIES ═══════════════════

    /**
     * Get all active partners.
     */
    public List<Partenaire> getAllPartners() {
        List<Partenaire> list = new ArrayList<>();
        String sql = "SELECT * FROM partenaire ORDER BY nom";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Partenaire p = new Partenaire();
                p.setIdPartenaire(rs.getInt("idPartenaire"));
                p.setNom(rs.getString("nom"));
                p.setCategorie(rs.getString("categorie"));
                p.setDescription(rs.getString("description"));
                p.setTauxCashback(rs.getDouble("tauxCashback"));
                p.setTauxCashbackMax(rs.getDouble("tauxCashbackMax"));
                p.setPlafondMensuel(rs.getDouble("plafondMensuel"));
                p.setConditions(rs.getString("conditions"));
                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching partners: " + e.getMessage());
        }
        return list;
    }

    /**
     * Get partner name by ID.
     */
    public String getPartnerNameById(int idPartenaire) {
        String sql = "SELECT nom FROM partenaire WHERE idPartenaire = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPartenaire);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("nom");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching partner name: " + e.getMessage());
        }
        return "Unknown";
    }

    /**
     * Get earnings per partner for a specific user.
     * Returns a list of Object[] where [0]=partnerName, [1]=category, [2]=totalEarned, [3]=cashbackRate
     */
    public List<Object[]> getEarningsPerPartnerByUser(int userId) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT p.nom, p.categorie, COALESCE(SUM(c.montantCashback), 0) AS totalEarned, " +
                     "p.tauxCashbackMax " +
                     "FROM cashback c JOIN partenaire p ON c.idPartenaire = p.idPartenaire " +
                     "WHERE c.idUser = ? AND c.statut IN ('Credite', 'Valide') " +
                     "GROUP BY p.idPartenaire, p.nom, p.categorie, p.tauxCashbackMax " +
                     "ORDER BY totalEarned DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getString("nom"),
                    rs.getString("categorie"),
                    rs.getDouble("totalEarned"),
                    rs.getDouble("tauxCashbackMax")
                });
            }
        } catch (SQLException e) {
            System.err.println("Error fetching earnings per partner: " + e.getMessage());
        }
        return list;
    }

    // ═══════════════════ CALCULATION HELPERS ═══════════════════

    /**
     * Calculate cashback amount from purchase amount and rate.
     * Shared between admin and user controllers.
     */
    public static double calculateCashbackAmount(double purchaseAmount, double rate) {
        return purchaseAmount * rate / 100.0;
    }

    /**
     * Determine the membership tier based on total earned cashback.
     */
    public static String getMembershipTier(double totalEarned) {
        if (totalEarned >= 500) return "Platine";
        if (totalEarned >= 200) return "Or";
        if (totalEarned >= 50) return "Argent";
        return "Bronze";
    }

    /**
     * Get the icon name for a membership tier.
     */
    public static String getMembershipTierIcon(String tier) {
        switch (tier) {
            case "Platine": return "fas-gem";
            case "Or": return "fas-star";
            case "Argent": return "fas-medal";
            default: return "fas-award";
        }
    }

    /**
     * Get icon literal for a partner category.
     */
    public static String getCategoryIcon(String category) {
        if (category == null) return "fas-store";
        switch (category.toLowerCase()) {
            case "voyage": return "fas-plane";
            case "shopping": return "fas-shopping-cart";
            case "restauration": return "fas-utensils";
            case "carburant": return "fas-gas-pump";
            case "divertissement": return "fas-film";
            case "technologie": return "fas-laptop";
            case "sante": return "fas-heartbeat";
            default: return "fas-store";
        }
    }

    // ═══════════════════ PRIVATE HELPERS ═══════════════════

    private Cashback mapResultSetToCashback(ResultSet rs) throws SQLException {
        Cashback c = new Cashback();
        c.setIdCashback(rs.getInt("idCashback"));
        c.setIdPartenaire(rs.getInt("idPartenaire"));
        c.setIdTransaction(rs.getInt("idTransaction"));
        c.setIdUser(rs.getInt("idUser"));
        c.setMontantAchat(rs.getDouble("montantAchat"));
        c.setTauxApplique(rs.getDouble("tauxApplique"));
        c.setMontantCashback(rs.getDouble("montantCashback"));
        c.setDateAchat(rs.getString("dateAchat"));
        c.setDateCredit(rs.getString("dateCredit"));
        c.setDateExpiration(rs.getString("dateExpiration"));
        c.setStatut(rs.getString("statut"));
        // Joined fields
        try {
            c.setPartenaireNom(rs.getString("partenaireNom"));
            c.setPartenaireCategorie(rs.getString("partenaireCategorie"));
        } catch (SQLException ignored) {
            // These columns may not be present in all queries
        }
        return c;
    }

    private double querySingleDouble(String sql) {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("Error in query: " + e.getMessage());
        }
        return 0.0;
    }

    private double querySingleDoubleParam(String sql, int param) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("Error in parameterized query: " + e.getMessage());
        }
        return 0.0;
    }

    private long querySingleLong(String sql) {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println("Error in query: " + e.getMessage());
        }
        return 0;
    }

    private long querySingleLongParam(String sql, int param) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println("Error in parameterized query: " + e.getMessage());
        }
        return 0;
    }
}
