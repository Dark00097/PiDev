package com.nexora.bank.Service;

import com.nexora.bank.Models.CoffreVirtuel;
import com.nexora.bank.Utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MODIFICATIONS apportées à CoffreVirtuelService :
 * ──────────────────────────────────────────────────
 * ★ getByCompte() → NOUVELLE méthode : retourne uniquement les coffres
 *                   liés à un compte bancaire spécifique (idCompte).
 *                   Utilisée quand l'utilisateur clique sur "Coffres"
 *                   dans une carte de compte du FrontOffice.
 * ★ mapRow()      → méthode utilitaire extraite pour éviter la duplication.
 */
public class CoffreVirtuelService implements ICrud<CoffreVirtuel> {

    Connection conn;

    public CoffreVirtuelService() {
        this.conn = MyDB.getInstance().getConn();
    }

    // ── INSERT ────────────────────────────────────────────────────────────────
    @Override
    public void add(CoffreVirtuel coffreVirtuel) {
        String SQL = "INSERT INTO coffrevirtuel " +
                "(nom, objectifMontant, montantActuel, dateCreation, " +
                " dateObjectifs, status, estVerrouille, idCompte, idUser) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setString(1,  coffreVirtuel.getNom());
            pst.setDouble(2,  coffreVirtuel.getObjectifMontant());
            pst.setDouble(3,  coffreVirtuel.getMontantActuel());
            pst.setString(4,  coffreVirtuel.getDateCreation());
            pst.setString(5,  coffreVirtuel.getDateObjectifs());
            pst.setString(6,  coffreVirtuel.getStatus());
            pst.setBoolean(7, coffreVirtuel.isEstVerrouille());
            pst.setInt(8,     coffreVirtuel.getIdCompte());
            // ★ idUser : NULL si 0 (admin sans compte user), sinon l'id du user connecté
            if (coffreVirtuel.getIdUser() > 0) {
                pst.setInt(9, coffreVirtuel.getIdUser());
            } else {
                pst.setNull(9, Types.INTEGER);
            }

            pst.executeUpdate();
            System.out.println("Coffre Virtuel ajouté avec succès");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    @Override
    public void edit(CoffreVirtuel coffreVirtuel) {
        String req = "UPDATE coffrevirtuel " +
                "SET nom=?, objectifMontant=?, montantActuel=?, dateCreation=?, " +
                "    dateObjectifs=?, status=?, estVerrouille=?, idCompte=?, idUser=? " +
                "WHERE idCoffre=?";

        try {
            PreparedStatement pst = conn.prepareStatement(req);
            pst.setString(1,  coffreVirtuel.getNom());
            pst.setDouble(2,  coffreVirtuel.getObjectifMontant());
            pst.setDouble(3,  coffreVirtuel.getMontantActuel());
            pst.setString(4,  coffreVirtuel.getDateCreation());
            pst.setString(5,  coffreVirtuel.getDateObjectifs());
            pst.setString(6,  coffreVirtuel.getStatus());
            pst.setBoolean(7, coffreVirtuel.isEstVerrouille());
            pst.setInt(8,     coffreVirtuel.getIdCompte());
            // ★ idUser : NULL si 0, sinon id du user connecté
            if (coffreVirtuel.getIdUser() > 0) {
                pst.setInt(9, coffreVirtuel.getIdUser());
            } else {
                pst.setNull(9, Types.INTEGER);
            }
            pst.setInt(10,    coffreVirtuel.getIdCoffre());

            pst.executeUpdate();
            System.out.println("Coffre Virtuel modifié avec succès");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    @Override
    public void remove(CoffreVirtuel coffreVirtuel) {
        String SQL = "DELETE FROM coffrevirtuel WHERE idCoffre = ?";
        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setInt(1, coffreVirtuel.getIdCoffre());
            pst.executeUpdate();
            System.out.println("Coffre Virtuel supprimé avec succès");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ── SELECT ALL (BackOffice Admin) ──────────────────────────────────────────
    @Override
    public List<CoffreVirtuel> getAll() {
        // Inchangé : l'admin BackOffice voit tous les coffres.
        String req = "SELECT * FROM coffrevirtuel";
        ArrayList<CoffreVirtuel> coffres = new ArrayList<>();
        try {
            Statement stm = conn.createStatement();
            ResultSet rs  = stm.executeQuery(req);
            while (rs.next()) {
                coffres.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return coffres;
    }

    // ★ NOUVELLE MÉTHODE : SELECT filtré par idCompte (FrontOffice Client) ─────
    /**
     * Retourne uniquement les coffres liés au compte bancaire sélectionné.
     * Appelée quand l'utilisateur clique sur "Coffres" dans une carte de compte.
     *
     * Sécurité : puisque les comptes sont déjà filtrés par idUser dans
     * CompteBancaireService.getByUser(), seuls les comptes du client connecté
     * peuvent être passés ici — donc leurs coffres sont automatiquement sécurisés.
     *
     * @param idCompte  ID du compte bancaire dont on veut voir les coffres.
     * @return Liste des coffres virtuels liés à ce compte.
     */
    public List<CoffreVirtuel> getByCompte(int idCompte) {
        String req = "SELECT * FROM coffrevirtuel WHERE idCompte = ?";
        ArrayList<CoffreVirtuel> coffres = new ArrayList<>();
        try {
            PreparedStatement pst = conn.prepareStatement(req);
            pst.setInt(1, idCompte);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                coffres.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return coffres;
    }

    // ★ NOUVELLE MÉTHODE : SELECT filtré par idUser (FrontOffice)
    /**
     * Retourne uniquement les coffres appartenant à l'utilisateur connecté.
     * @param idUser ID de l'utilisateur connecté
     * @return Liste des coffres liés à cet utilisateur
     */
    public List<CoffreVirtuel> getByUser(int idUser) {
        String req = "SELECT * FROM coffrevirtuel WHERE idUser = ?";
        ArrayList<CoffreVirtuel> coffres = new ArrayList<>();
        try {
            PreparedStatement pst = conn.prepareStatement(req);
            pst.setInt(1, idUser);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                coffres.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return coffres;
    }

    // ── Méthode utilitaire : mapping ResultSet → CoffreVirtuel ───────────────
    // ★ NOUVEAU : extraite pour éviter la duplication dans getAll() et getByCompte()
    private CoffreVirtuel mapRow(ResultSet rs) throws SQLException {
        CoffreVirtuel coffre = new CoffreVirtuel();
        coffre.setIdCoffre(rs.getInt("idCoffre"));
        coffre.setNom(rs.getString("nom"));
        coffre.setObjectifMontant(rs.getDouble("objectifMontant"));
        coffre.setMontantActuel(rs.getDouble("montantActuel"));
        coffre.setDateCreation(rs.getString("dateCreation"));
        coffre.setDateObjectifs(rs.getString("dateObjectifs"));
        coffre.setStatus(rs.getString("status"));
        coffre.setEstVerrouille(rs.getBoolean("estVerrouille"));
        coffre.setIdCompte(rs.getInt("idCompte"));
        coffre.setIdUser(rs.getInt("idUser"));  // ★ 0 si NULL en BD (getInt retourne 0 pour NULL)
        return coffre;
    }
}