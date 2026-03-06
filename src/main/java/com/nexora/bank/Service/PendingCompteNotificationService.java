package com.nexora.bank.Service;

import com.nexora.bank.Utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PendingCompteNotificationService {

    private final TwilioSmsService smsService = new TwilioSmsService();
    private final NotificationService notificationService = new NotificationService();

    public static class CompteEnAttente {
        public int idCompte;
        public String numeroCompte;
        public String typeCompte;
        public double solde;
        public String dateOuverture;
        public double plafondRetrait;
        public double plafondVirement;
        public int idUser;
        public String nomClient;
        public String telephoneClient;
    }

    public List<CompteEnAttente> getPendingComptes() {
        Connection conn = getConnection();
        if (conn == null) {
            return List.of();
        }

        String sqlWithJoin =
            "SELECT c.idCompte, c.numeroCompte, c.typeCompte, c.solde, " +
            "       c.dateOuverture, c.plafondRetrait, c.plafondVirement, c.idUser, " +
            "       CONCAT(COALESCE(u.prenom,''), ' ', COALESCE(u.nom,'')) AS nomClient, " +
            "       COALESCE(u.telephone, '') AS telephone " +
            "FROM compte c " +
            "LEFT JOIN users u ON c.idUser = u.idUser " +
            "WHERE LOWER(TRIM(c.statutCompte)) = 'en attente' " +
            "ORDER BY c.idCompte DESC";

        try (PreparedStatement ps = conn.prepareStatement(sqlWithJoin);
             ResultSet rs = ps.executeQuery()) {
            List<CompteEnAttente> items = new ArrayList<>();
            while (rs.next()) {
                items.add(mapRow(rs, true));
            }
            return items;
        } catch (SQLException ex) {
            return getPendingComptesSimple(conn);
        }
    }

    public int countPending() {
        Connection conn = getConnection();
        if (conn == null) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM compte WHERE LOWER(TRIM(statutCompte)) = 'en attente'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            System.err.println("[PendingCompteNotificationService] countPending failed: " + ex.getMessage());
        }
        return 0;
    }

    public void accepterCompte(int idCompte) {
        if (idCompte <= 0) {
            return;
        }

        Connection conn = getConnection();
        if (conn == null) {
            return;
        }

        String sql = "UPDATE compte SET statutCompte = 'Actif' WHERE idCompte = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCompte);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("[PendingCompteNotificationService] accepterCompte failed: " + ex.getMessage());
            return;
        }

        CompteEnAttente compte = getCompteInfoById(idCompte);
        if (compte == null) {
            return;
        }

        notifyUser(compte.idUser, "COMPTE_APPROVED", "Bank account approved",
            "Your bank account " + safe(compte.numeroCompte, "-") + " is now active.");

        String sms = TwilioSmsService.buildAccountValidationMessage(
            compte.numeroCompte,
            compte.typeCompte,
            compte.solde,
            compte.dateOuverture,
            compte.plafondRetrait,
            compte.plafondVirement,
            compte.nomClient
        );
        sendSmsAsync(compte.telephoneClient, sms, "accept-" + idCompte);
    }

    public void refuserCompteAvecSms(int idCompte, boolean supprimer) {
        if (idCompte <= 0) {
            return;
        }

        CompteEnAttente compte = getCompteInfoById(idCompte);
        refuserCompte(idCompte, supprimer);

        if (compte == null) {
            return;
        }

        notifyUser(compte.idUser, "COMPTE_DECLINED", "Bank account request declined",
            "Your bank account request for " + safe(compte.numeroCompte, "-") + " has been declined.");

        String sms = TwilioSmsService.buildAccountRefusalMessage(
            compte.numeroCompte,
            compte.typeCompte,
            compte.nomClient
        );
        sendSmsAsync(compte.telephoneClient, sms, "refuse-" + idCompte);
    }

    public void refuserCompte(int idCompte, boolean supprimer) {
        Connection conn = getConnection();
        if (conn == null || idCompte <= 0) {
            return;
        }

        String sql = supprimer
            ? "DELETE FROM compte WHERE idCompte = ?"
            : "UPDATE compte SET statutCompte = 'Ferme' WHERE idCompte = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCompte);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("[PendingCompteNotificationService] refuserCompte failed: " + ex.getMessage());
        }
    }

    private CompteEnAttente getCompteInfoById(int idCompte) {
        Connection conn = getConnection();
        if (conn == null || idCompte <= 0) {
            return null;
        }

        String sqlWithJoin =
            "SELECT c.idCompte, c.numeroCompte, c.typeCompte, c.solde, " +
            "       c.dateOuverture, c.plafondRetrait, c.plafondVirement, c.idUser, " +
            "       CONCAT(COALESCE(u.prenom,''), ' ', COALESCE(u.nom,'')) AS nomClient, " +
            "       COALESCE(u.telephone, '') AS telephone " +
            "FROM compte c " +
            "LEFT JOIN users u ON c.idUser = u.idUser " +
            "WHERE c.idCompte = ?";

        try (PreparedStatement ps = conn.prepareStatement(sqlWithJoin)) {
            ps.setInt(1, idCompte);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs, true);
                }
            }
        } catch (SQLException ex) {
            // fallback below
        }

        String sqlSimple =
            "SELECT idCompte, numeroCompte, typeCompte, solde, dateOuverture, plafondRetrait, plafondVirement, idUser " +
            "FROM compte WHERE idCompte = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlSimple)) {
            ps.setInt(1, idCompte);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs, false);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[PendingCompteNotificationService] getCompteInfoById failed: " + ex.getMessage());
        }
        return null;
    }

    private List<CompteEnAttente> getPendingComptesSimple(Connection conn) {
        String sql =
            "SELECT idCompte, numeroCompte, typeCompte, solde, dateOuverture, plafondRetrait, plafondVirement, idUser " +
            "FROM compte WHERE LOWER(TRIM(statutCompte)) = 'en attente' ORDER BY idCompte DESC";
        List<CompteEnAttente> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(mapRow(rs, false));
            }
        } catch (SQLException ex) {
            System.err.println("[PendingCompteNotificationService] getPendingComptesSimple failed: " + ex.getMessage());
        }
        return items;
    }

    private CompteEnAttente mapRow(ResultSet rs, boolean withJoin) throws SQLException {
        CompteEnAttente item = new CompteEnAttente();
        item.idCompte = rs.getInt("idCompte");
        item.numeroCompte = rs.getString("numeroCompte");
        item.typeCompte = rs.getString("typeCompte");
        item.solde = rs.getDouble("solde");
        item.dateOuverture = rs.getString("dateOuverture");
        item.plafondRetrait = rs.getDouble("plafondRetrait");
        item.plafondVirement = rs.getDouble("plafondVirement");
        item.idUser = rs.getInt("idUser");

        if (withJoin) {
            item.nomClient = rs.getString("nomClient");
            item.telephoneClient = rs.getString("telephone");
        } else {
            item.nomClient = "Client";
            item.telephoneClient = "";
        }
        return item;
    }

    private void notifyUser(int idUser, String type, String title, String message) {
        if (idUser <= 0) {
            return;
        }
        try {
            notificationService.createUserNotification(idUser, type, title, message, idUser);
        } catch (Exception ex) {
            System.err.println("[PendingCompteNotificationService] notifyUser failed: " + ex.getMessage());
        }
    }

    private void sendSmsAsync(String phone, String message, String threadSuffix) {
        Thread thread = new Thread(() -> {
            boolean sent = smsService.sendSms(phone, message);
            System.out.println("[PendingCompteNotificationService] SMS dispatch (" + threadSuffix + "): " + (sent ? "OK" : "FAILED"));
        }, "nexora-sms-" + threadSuffix);
        thread.setDaemon(true);
        thread.start();
    }

    private Connection getConnection() {
        return MyDB.getInstance().getConn();
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }
}
