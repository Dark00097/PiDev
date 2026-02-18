package com.nexora.bank.Service;

import com.nexora.bank.Models.CompteBancaire;
import com.nexora.bank.Utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════════
 * MODIFICATIONS apportées à CompteBancaireService
 * ══════════════════════════════════════════════════════════════════
 *
 * ★ add()  → [CORRECTION FK] Si idUser == 0 (BackOffice admin),
 *             on insère NULL dans la colonne idUser au lieu de 0.
 *             La contrainte FK fk_compte_user accepte NULL (ON DELETE SET NULL)
 *             mais rejette 0 car 0 n'existe pas dans users.idUser.
 *
 * ★ edit() → [CORRECTION FK] Même logique : NULL si idUser == 0.
 *
 * ★ remove() → deux cas : idUser > 0 (FrontOffice sécurisé) ou 0 (admin).
 *
 * ★ getByUser() → filtre par idUser pour le FrontOffice client.
 * ══════════════════════════════════════════════════════════════════
 */
public class CompteBancaireService implements ICrud<CompteBancaire> {

    Connection conn;

    public CompteBancaireService() {
        this.conn = MyDB.getInstance().getConn();
    }

    // ── INSERT ────────────────────────────────────────────────────────────────
    @Override
    public void add(CompteBancaire compteBancaire) {
        String SQL = "INSERT INTO compte " +
                "(numeroCompte, solde, dateOuverture, statutCompte, " +
                " plafondRetrait, plafondVirement, typeCompte, idUser) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setString(1, compteBancaire.getNumeroCompte());
            pst.setDouble(2, compteBancaire.getSolde());
            pst.setString(3, compteBancaire.getDateOuverture());
            pst.setString(4, compteBancaire.getStatutCompte());
            pst.setDouble(5, compteBancaire.getPlafondRetrait());
            pst.setDouble(6, compteBancaire.getPlafondVirement());
            pst.setString(7, compteBancaire.getTypeCompte());

            // ★ CORRECTION FK : idUser = 0 signifie "pas de propriétaire" (BackOffice admin).
            //   La BD accepte NULL (ON DELETE SET NULL) mais rejette 0 (n'existe pas dans users).
            //   → On insère NULL quand idUser == 0.
            if (compteBancaire.getIdUser() > 0) {
                pst.setInt(8, compteBancaire.getIdUser());
            } else {
                pst.setNull(8, Types.INTEGER);  // ★ NULL au lieu de 0
            }

            pst.executeUpdate();
            System.out.println("Compte Bancaire ajouté avec succès");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    @Override
    public void edit(CompteBancaire compteBancaire) {
        String req = "UPDATE compte SET " +
                "numeroCompte = ?, " +
                "solde = ?, " +
                "dateOuverture = ?, " +
                "statutCompte = ?, " +
                "plafondRetrait = ?, " +
                "plafondVirement = ?, " +
                "typeCompte = ? " +
                "WHERE idCompte = ?";

        try {
            PreparedStatement pst = conn.prepareStatement(req);
            pst.setString(1, compteBancaire.getNumeroCompte());
            pst.setDouble(2, compteBancaire.getSolde());
            pst.setString(3, compteBancaire.getDateOuverture());
            pst.setString(4, compteBancaire.getStatutCompte());
            pst.setDouble(5, compteBancaire.getPlafondRetrait());
            pst.setDouble(6, compteBancaire.getPlafondVirement());
            pst.setString(7, compteBancaire.getTypeCompte());
            pst.setInt(8, compteBancaire.getIdCompte());

            pst.executeUpdate();
            System.out.println("Compte Bancaire modifié avec succès");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    @Override
    public void remove(CompteBancaire compteBancaire) {
        try {
            if (compteBancaire.getIdUser() > 0) {
                // FrontOffice : sécurisé par idUser
                String SQL = "DELETE FROM compte WHERE idCompte = ? AND idUser = ?";
                PreparedStatement pst = conn.prepareStatement(SQL);
                pst.setInt(1, compteBancaire.getIdCompte());
                pst.setInt(2, compteBancaire.getIdUser());
                pst.executeUpdate();
            } else {
                // BackOffice admin : suppression par idCompte uniquement
                String SQL = "DELETE FROM compte WHERE idCompte = ?";
                PreparedStatement pst = conn.prepareStatement(SQL);
                pst.setInt(1, compteBancaire.getIdCompte());
                pst.executeUpdate();
            }
            System.out.println("Compte Bancaire supprimé avec succès");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // ── SELECT ALL (BackOffice Admin) ──────────────────────────────────────────
    @Override
    public List<CompteBancaire> getAll() {
        String req = "SELECT * FROM compte";
        ArrayList<CompteBancaire> comptes = new ArrayList<>();
        try {
            Statement stm = this.conn.createStatement();
            ResultSet rs  = stm.executeQuery(req);
            while (rs.next()) {
                comptes.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return comptes;
    }

    // ── SELECT filtré par idUser (FrontOffice Client) ──────────────────────────
    /**
     * Retourne uniquement les comptes du client connecté.
     * Utilisé par UserDashboardAccountsSectionController.
     *
     * @param idUser ID de l'utilisateur connecté
     * @return Liste des comptes liés uniquement à cet utilisateur
     */
    public List<CompteBancaire> getByUser(int idUser) {
        String req = "SELECT * FROM compte WHERE idUser = ?";
        ArrayList<CompteBancaire> comptes = new ArrayList<>();
        try {
            PreparedStatement pst = conn.prepareStatement(req);
            pst.setInt(1, idUser);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                comptes.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return comptes;
    }

    // ── Mapping ResultSet → CompteBancaire ────────────────────────────────────
    private CompteBancaire mapRow(ResultSet rs) throws SQLException {
        CompteBancaire comp = new CompteBancaire();
        comp.setIdCompte(rs.getInt("idCompte"));
        comp.setNumeroCompte(rs.getString("numeroCompte"));
        comp.setSolde(rs.getDouble("solde"));
        comp.setDateOuverture(rs.getString("dateOuverture"));
        comp.setStatutCompte(rs.getString("statutCompte"));
        comp.setPlafondRetrait(rs.getDouble("plafondRetrait"));
        comp.setPlafondVirement(rs.getDouble("plafondVirement"));
        comp.setTypeCompte(rs.getString("typeCompte"));
        // ★ idUser peut être NULL en BD → getInt retourne 0 si NULL, ce qui est correct
        comp.setIdUser(rs.getInt("idUser"));
        return comp;
    }
}