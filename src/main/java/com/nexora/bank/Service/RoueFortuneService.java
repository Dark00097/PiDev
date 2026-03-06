package com.nexora.bank.Service;

import java.sql.*;
import java.util.Random;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  RoueFortuneService
 *  Gère les points de la Roue de la Fortune : tirage, accumulation, messages.
 *
 *  SÉCURITÉ ANTI-TRICHE :
 *   Le champ `dernierMois` ne peut jamais reculer grâce à GREATEST().
 *   Même si l'utilisateur change la date système (XAMPP local suit le PC),
 *   revenir à un mois antérieur est impossible car on conserve toujours
 *   le mois le plus élevé jamais enregistré.
 *
 *   Exemple :
 *     dernierMois = '2026-04'  (après avoir triché en avançant la date)
 *     User remet PC en '2026-03'  →  NOW() = '2026-03'
 *     '2026-03' <= '2026-04'  →  BLOQUÉ ✅
 * ══════════════════════════════════════════════════════════════════════════════
 */
public class RoueFortuneService {

    private static final int[] SEGMENTS_POINTS = {5, 10, 20, 15, 8, 12, 3, 7, 25, 2, 18, 6};
    private static final Random RANDOM = new Random();

    /**
     * Crée la table roue_fortune_points si elle n'existe pas.
     */
    public void initTable(Connection conn) {
        String sql = """
            CREATE TABLE IF NOT EXISTS roue_fortune_points (
                idUser       INT NOT NULL PRIMARY KEY,
                totalPoints  INT NOT NULL DEFAULT 0,
                dernierTour  VARCHAR(10) DEFAULT NULL,
                dernierMois  VARCHAR(7)  DEFAULT NULL,
                pointsGagnes INT NOT NULL DEFAULT 0
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
            System.out.println("[Roue] Table roue_fortune_points prête.");
        } catch (Exception e) {
            System.err.println("[Roue] Init table: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ✅ ANTI-TRICHE : aDejaJoueCeMois
    //
    //  Logique : le mois actuel (NOW) est comparé au dernierMois en BDD.
    //  Si NOW() <= dernierMois  →  bloqué.
    //  Cela couvre 2 cas :
    //    1. Même mois         : '2026-03' = '2026-03'  → bloqué ✅
    //    2. Recul de date     : '2026-03' < '2026-04'  → bloqué ✅  (anti-triche)
    //  Seul le cas avancement légitime (mois suivant) passe :
    //    3. Mois suivant réel : '2026-04' > '2026-04'  → autorisé ✅
    // ══════════════════════════════════════════════════════════════════════════
    public boolean aDejaJoueCeMois(int userId, Connection conn) {
        String sql = """
            SELECT 1
            FROM roue_fortune_points
            WHERE idUser = ?
              AND dernierMois IS NOT NULL
              AND DATE_FORMAT(NOW(), '%Y-%m') <= dernierMois
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            boolean bloque = rs.next();
            System.out.println("[Roue] aDejaJoueCeMois userId=" + userId + " → bloqué=" + bloque);
            return bloque;
        } catch (Exception e) {
            System.err.println("[Roue] aDejaJoue: " + e.getMessage());
            return true; // Par sécurité : bloquer si erreur
        }
    }

    /**
     * ✅ ANTI-TRICHE : Effectue le tirage de la roue.
     *
     * Lors de la sauvegarde, `dernierMois` est mis à jour avec GREATEST() :
     * on conserve toujours la valeur la plus haute entre l'ancienne et NOW().
     * Ainsi, même si le PC recule sa date, le champ ne peut pas diminuer.
     */
    public TirageResult tournerRoue(int userId, Connection conn) {
        TirageResult res = new TirageResult();

        try {
            // Double vérification avant tirage
            if (aDejaJoueCeMois(userId, conn)) {
                System.out.println("[Roue] ⛔ Tirage bloqué pour userId=" + userId);
                res.pointsGagnes = 0;
                res.totalPoints  = getTotalPoints(userId, conn);
                res.message      = "⛔ Tu as déjà tourné la roue ce mois-ci. Reviens le mois prochain !";
                res.date         = getDateServeur(conn);
                return res;
            }

            int pointsGagnes = SEGMENTS_POINTS[RANDOM.nextInt(SEGMENTS_POINTS.length)];
            res.pointsGagnes = pointsGagnes;
            res.date         = getDateServeur(conn);

            // Vérifier si la ligne existe déjà
            boolean existe      = false;
            int     ancienTotal = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT totalPoints FROM roue_fortune_points WHERE idUser = ?")) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) { existe = true; ancienTotal = rs.getInt("totalPoints"); }
            }

            // Plafonner à 100 : si le total dépasse 100, on force exactement 100
            // pour déclencher le bonus au seuil exact.
            int nouveauTotal = Math.min(ancienTotal + pointsGagnes, 100);
            res.totalPoints  = nouveauTotal;

            if (existe) {
                // ✅ GREATEST() : dernierMois ne peut jamais reculer
                String upd = """
                    UPDATE roue_fortune_points
                    SET totalPoints  = ?,
                        dernierTour  = DATE_FORMAT(NOW(), '%Y-%m-%d'),
                        dernierMois  = GREATEST(COALESCE(dernierMois, '0000-00'), DATE_FORMAT(NOW(), '%Y-%m')),
                        pointsGagnes = ?
                    WHERE idUser = ?
                    """;
                try (PreparedStatement ps = conn.prepareStatement(upd)) {
                    ps.setInt(1, nouveauTotal);
                    ps.setInt(2, pointsGagnes);
                    ps.setInt(3, userId);
                    ps.executeUpdate();
                }
            } else {
                String ins = """
                    INSERT INTO roue_fortune_points
                        (idUser, totalPoints, dernierTour, dernierMois, pointsGagnes)
                    VALUES
                        (?, ?, DATE_FORMAT(NOW(), '%Y-%m-%d'), DATE_FORMAT(NOW(), '%Y-%m'), ?)
                    """;
                try (PreparedStatement ps = conn.prepareStatement(ins)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, nouveauTotal);
                    ps.setInt(3, pointsGagnes);
                    ps.executeUpdate();
                }
            }

            res.message = buildMessage(pointsGagnes, nouveauTotal);
            System.out.println("[Roue] ✅ Tirage userId=" + userId
                    + " → +" + pointsGagnes + " pts | Total: " + nouveauTotal);

        } catch (Exception e) {
            System.err.println("[Roue] Erreur tirage: " + e.getMessage());
            res.message = "Bravo ! Tu as gagné " + res.pointsGagnes + " points !";
        }
        return res;
    }

    /**
     * Récupère le total de points actuel d'un utilisateur.
     */
    public int getTotalPoints(int userId, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT totalPoints FROM roue_fortune_points WHERE idUser = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("totalPoints");
        } catch (Exception e) {
            System.err.println("[Roue] getTotalPoints: " + e.getMessage());
        }
        return 0;
    }

    private String getDateServeur(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT DATE_FORMAT(NOW(), '%Y-%m-%d') AS d")) {
            if (rs.next()) return rs.getString("d");
        }
        throw new SQLException("Impossible de lire la date serveur.");
    }

    private String buildMessage(int gained, int total) {
        String[] exclamations = {"🎉", "🌟", "🏆", "💫", "🔥", "⚡", "🎊"};
        String excl = exclamations[RANDOM.nextInt(exclamations.length)];
        if (gained >= 20)
            return excl + " JACKPOT ! Tu as gagné " + gained + " points ! Total : " + total + " pts. Continue comme ça !";
        if (gained >= 15)
            return excl + " Excellent ! +" + gained + " points dans ta poche ! Total : " + total + " pts.";
        if (gained >= 10)
            return excl + " Super ! Tu as décroché " + gained + " points ! Total : " + total + " pts.";
        if (gained >= 5)
            return "👏 Bravo ! Tu as gagné " + gained + " points. Continue d'épargner ! Total : " + total + " pts.";
        return "✨ Tu as gagné " + gained + " points ! Chaque point compte. Total : " + total + " pts.";
    }

    /**
     * Remet les points à 0 après attribution du bonus 50 DT.
     */
    public void resetPoints(int userId, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE roue_fortune_points SET totalPoints=0, pointsGagnes=0 WHERE idUser=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            System.out.println("[Roue] Points remis à 0 pour userId=" + userId);
        } catch (Exception e) {
            System.err.println("[Roue] resetPoints: " + e.getMessage());
        }
    }

    public static class TirageResult {
        public int    pointsGagnes;
        public int    totalPoints;
        public String date;
        public String message;
    }
}