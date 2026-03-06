package com.nexora.bank.Service;

import com.nexora.bank.Models.Reclamation;
import com.nexora.bank.Utils.MyDB;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReclamationService implements ICrud<Reclamation> {

    private final Connection     conn          = MyDB.getInstance().getConn();
    private final BadWordService badWordService = new BadWordService();

    // ══════════════════════════════════════════════════════════════════════════
    // CRUD (identique à votre version originale + correction date SQL)
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void add(Reclamation r) {
        boolean inappropriate = badWordService.containsBadWord(r.getDescription());
        r.setInappropriate(inappropriate);
        if (inappropriate) r.setStatus("Signalée");

        String SQL =
            "INSERT INTO reclamation " +
            "(idUser, idTransaction, dateReclamation, typeReclamation, " +
            " description, status, is_inappropriate, is_blurred) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setInt(1,    r.getIdUser());
            pst.setInt(2,    r.getIdTransaction());
            // ✅ dateReclamation est de type DATE en BDD
            pst.setDate(3,   r.getDateReclamation() != null
                             ? Date.valueOf(r.getDateReclamation()) : Date.valueOf(LocalDate.now()));
            pst.setString(4, r.getTypeReclamation());
            pst.setString(5, r.getDescription());
            pst.setString(6, r.getStatus());
            pst.setInt(7,    inappropriate ? 1 : 0);
            pst.setInt(8,    0);
            pst.executeUpdate();
            System.out.println("Reclamation ajoutée — inappropriate=" + inappropriate);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void edit(Reclamation r) {
        boolean inappropriate = badWordService.containsBadWord(r.getDescription());
        r.setInappropriate(inappropriate);

        String SQL =
            "UPDATE reclamation SET " +
            "idUser=?, idTransaction=?, dateReclamation=?, " +
            "typeReclamation=?, description=?, status=?, " +
            "is_inappropriate=?, is_blurred=? " +
            "WHERE idReclamation=?";
        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setInt(1,    r.getIdUser());
            pst.setInt(2,    r.getIdTransaction());
            pst.setDate(3,   r.getDateReclamation() != null
                             ? Date.valueOf(r.getDateReclamation()) : Date.valueOf(LocalDate.now()));
            pst.setString(4, r.getTypeReclamation());
            pst.setString(5, r.getDescription());
            pst.setString(6, r.getStatus());
            pst.setInt(7,    r.isInappropriate() ? 1 : 0);
            pst.setInt(8,    r.isBlurred()       ? 1 : 0);
            pst.setInt(9,    r.getIdReclamation());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void remove(Reclamation r) {
        try {
            PreparedStatement pst = conn.prepareStatement(
                "DELETE FROM reclamation WHERE idReclamation=?");
            pst.setInt(1, r.getIdReclamation());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Reclamation> getAll() {
        List<Reclamation> list = new ArrayList<>();
        try {
            ResultSet rs = conn.createStatement()
                .executeQuery("SELECT * FROM reclamation ORDER BY idReclamation DESC");
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public List<Reclamation> getByUser(int idUser) {
        List<Reclamation> list = new ArrayList<>();
        try {
            PreparedStatement pst = conn.prepareStatement(
                "SELECT * FROM reclamation WHERE idUser=? ORDER BY idReclamation DESC");
            pst.setInt(1, idUser);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public List<Reclamation> getByTransaction(int idTransaction) {
        List<Reclamation> list = new ArrayList<>();
        try {
            PreparedStatement pst = conn.prepareStatement(
                "SELECT * FROM reclamation WHERE idTransaction=?");
            pst.setInt(1, idTransaction);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public void toggleBlur(int idReclamation, boolean blurred) {
        try {
            PreparedStatement pst = conn.prepareStatement(
                "UPDATE reclamation SET is_blurred=? WHERE idReclamation=?");
            pst.setInt(1, blurred ? 1 : 0);
            pst.setInt(2, idReclamation);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public long countInappropriate() {
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM reclamation WHERE is_inappropriate=1");
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return 0;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ✅ NOUVEAU — Méthodes stats pour le Dashboard Admin
    // ══════════════════════════════════════════════════════════════════════════

    /** Compte le total de réclamations. */
    public int countTotal() {
        try {
            ResultSet rs = conn.createStatement()
                .executeQuery("SELECT COUNT(*) FROM reclamation");
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return 0;
    }

    /** Compte par statut. Ex: "En attente", "Résolue", "En cours", "Rejetée", "Signalée" */
    public int countByStatut(String statut) {
        try {
            PreparedStatement pst = conn.prepareStatement(
                "SELECT COUNT(*) FROM reclamation WHERE status=?");
            pst.setString(1, statut);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return 0;
    }

    /** Taux de résolution = (Résolues / Total) × 100 */
    public double getTauxResolution() {
        int total   = countTotal();
        int resolues = countByStatut("Résolue");
        return total == 0 ? 0.0 : (resolues * 100.0) / total;
    }

    /**
     * Temps moyen de résolution en jours.
     * Calcul basé sur la différence entre dateReclamation et aujourd'hui
     * pour les réclamations à statut "Résolue".
     */
    public double getTempsMoyenResolutionJours() {
        List<Reclamation> resolues = new ArrayList<>();
        try {
            PreparedStatement pst = conn.prepareStatement(
                "SELECT * FROM reclamation WHERE status='Résolue'");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) resolues.add(mapRow(rs));
        } catch (SQLException e) { System.out.println(e.getMessage()); }

        if (resolues.isEmpty()) return 0.0;

        double totalJours  = 0.0;
        LocalDate aujdhui  = LocalDate.now();
        for (Reclamation r : resolues) {
            try {
                LocalDate dateRec = LocalDate.parse(r.getDateReclamation());
                totalJours += ChronoUnit.DAYS.between(dateRec, aujdhui);
            } catch (Exception ignored) {}
        }
        return totalJours / resolues.size();
    }

    /** Nombre de réclamations par type → pour graphique. */
    public Map<String, Integer> countParType() {
        Map<String, Integer> result = new LinkedHashMap<>();
        try {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT typeReclamation, COUNT(*) as nb " +
                "FROM reclamation GROUP BY typeReclamation ORDER BY nb DESC");
            while (rs.next())
                result.put(rs.getString("typeReclamation"), rs.getInt("nb"));
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return result;
    }

    /** Nombre de réclamations par statut → pour graphique barres. */
    public Map<String, Integer> countParStatut() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String s : new String[]{"En attente", "En cours", "Résolue", "Rejetée", "Signalée"})
            result.put(s, countByStatut(s));
        return result;
    }

    // ── Helper mapping ResultSet → Reclamation ────────────────────────────────

    private Reclamation mapRow(ResultSet rs) throws SQLException {
        Reclamation r = new Reclamation();
        r.setIdReclamation(rs.getInt("idReclamation"));
        r.setIdUser(rs.getInt("idUser"));
        r.setIdTransaction(rs.getInt("idTransaction"));
        // ✅ dateReclamation est de type DATE en BDD → on lit avec getString (compatible)
        r.setDateReclamation(rs.getString("dateReclamation"));
        r.setTypeReclamation(rs.getString("typeReclamation"));
        r.setDescription(rs.getString("description"));
        r.setStatus(rs.getString("status"));
        r.setInappropriate(rs.getInt("is_inappropriate") == 1);
        r.setBlurred(rs.getInt("is_blurred") == 1);
        return r;
    }
}
