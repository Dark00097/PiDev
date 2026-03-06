package com.nexora.bank.Service;

import com.nexora.bank.Models.Transaction;
import com.nexora.bank.Utils.MyDB;
import com.nexora.bank.Utils.EncryptionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionService implements ICrud<Transaction> {

    Connection conn;

    public TransactionService() {
        this.conn = MyDB.getInstance().getConn();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CRUD
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void add(Transaction t) {
        String SQL = "INSERT INTO transactions " +
                "(idUser, categorie, dateTransaction, montant, typeTransaction, " +
                "statutTransaction, description, montantPaye) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            String montantChiffre     = EncryptionUtil.encrypt(t.getMontant());
            String montantPayeChiffre = EncryptionUtil.encrypt(0.0);

            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setInt(1, t.getIdUser());
            pst.setString(2, t.getCategorie());
            pst.setDate(3, t.getDateTransaction() != null ? Date.valueOf(t.getDateTransaction()) : null);
            pst.setString(4, montantChiffre);
            pst.setString(5, t.getTypeTransaction());
            pst.setString(6, t.getStatutTransaction());
            pst.setString(7, t.getDescription());
            pst.setString(8, montantPayeChiffre);
            pst.executeUpdate();
            System.out.println("✅ Transaction ajoutée avec succès");
        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Erreur chiffrement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void edit(Transaction t) {
        String req = "UPDATE transactions SET " +
                "idUser = ?, categorie = ?, dateTransaction = ?, montant = ?, typeTransaction = ?, " +
                "statutTransaction = ?, description = ? " +
                "WHERE idTransaction = ?";

        try {
            String montantChiffre = EncryptionUtil.encrypt(t.getMontant());

            PreparedStatement pst = conn.prepareStatement(req);
            pst.setInt(1, t.getIdUser());
            pst.setString(2, t.getCategorie());
            pst.setDate(3, t.getDateTransaction() != null ? Date.valueOf(t.getDateTransaction()) : null);
            pst.setString(4, montantChiffre);
            pst.setString(5, t.getTypeTransaction());
            pst.setString(6, t.getStatutTransaction());
            pst.setString(7, t.getDescription());
            pst.setInt(8, t.getIdTransaction());
            pst.executeUpdate();
            System.out.println("✅ Transaction modifiée avec succès");
        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Erreur chiffrement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void remove(Transaction t) {
        String SQL = "DELETE FROM transactions WHERE idTransaction = ?";
        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setInt(1, t.getIdTransaction());
            pst.executeUpdate();
            System.out.println("✅ Transaction supprimée avec succès");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Transaction> getAll() {
        String req = "SELECT * FROM transactions";
        ArrayList<Transaction> transactions = new ArrayList<>();

        try {
            Statement stm = conn.createStatement();
            ResultSet rs  = stm.executeQuery(req);

            while (rs.next()) {
                Transaction t = new Transaction();
                t.setIdTransaction(rs.getInt("idTransaction"));
                t.setIdUser(rs.getInt("idUser"));
                t.setCategorie(rs.getString("categorie"));

                Date sqlDate = rs.getDate("dateTransaction");
                t.setDateTransaction(sqlDate != null ? sqlDate.toLocalDate() : null);

                t.setMontant(EncryptionUtil.decrypt(rs.getString("montant")));

                t.setTypeTransaction(rs.getString("typeTransaction"));
                t.setStatutTransaction(rs.getString("statutTransaction"));
                t.setDescription(rs.getString("description"));

                String montantPayeChiffre = rs.getString("montantPaye");
                if (montantPayeChiffre != null && !montantPayeChiffre.isEmpty()) {
                    Double mp = EncryptionUtil.decrypt(montantPayeChiffre);
                    t.setMontantPaye(mp != null ? mp : 0.0);
                } else {
                    t.setMontantPaye(0.0);
                }

                transactions.add(t);
            }
        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Erreur déchiffrement : " + e.getMessage());
            e.printStackTrace();
        }

        return transactions;
    }

    public List<Transaction> getByUser(int idUser) {
        String req = "SELECT * FROM transactions WHERE idUser = ?";
        ArrayList<Transaction> transactions = new ArrayList<>();

        try {
            PreparedStatement pst = conn.prepareStatement(req);
            pst.setInt(1, idUser);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Transaction t = new Transaction();
                t.setIdTransaction(rs.getInt("idTransaction"));
                t.setIdUser(rs.getInt("idUser"));
                t.setCategorie(rs.getString("categorie"));

                Date sqlDate = rs.getDate("dateTransaction");
                t.setDateTransaction(sqlDate != null ? sqlDate.toLocalDate() : null);

                t.setMontant(EncryptionUtil.decrypt(rs.getString("montant")));

                t.setTypeTransaction(rs.getString("typeTransaction"));
                t.setStatutTransaction(rs.getString("statutTransaction"));
                t.setDescription(rs.getString("description"));

                String montantPayeChiffre = rs.getString("montantPaye");
                if (montantPayeChiffre != null && !montantPayeChiffre.isEmpty()) {
                    Double mp = EncryptionUtil.decrypt(montantPayeChiffre);
                    t.setMontantPaye(mp != null ? mp : 0.0);
                } else {
                    t.setMontantPaye(0.0);
                }

                transactions.add(t);
            }
        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Erreur déchiffrement : " + e.getMessage());
            e.printStackTrace();
        }

        return transactions;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Mettre à jour montantPaye après chaque paiement Stripe
    // ══════════════════════════════════════════════════════════════════════════

    public void updateMontantPaye(int idTransaction, double montantAPayer) {
        String selectSQL = "SELECT montantPaye FROM transactions WHERE idTransaction = ?";
        try {
            PreparedStatement pstSelect = conn.prepareStatement(selectSQL);
            pstSelect.setInt(1, idTransaction);
            ResultSet rs = pstSelect.executeQuery();

            double ancienMontantPaye = 0.0;
            if (rs.next()) {
                String montantPayeChiffre = rs.getString("montantPaye");
                if (montantPayeChiffre != null && !montantPayeChiffre.isEmpty()) {
                    Double decrypted = EncryptionUtil.decrypt(montantPayeChiffre);
                    ancienMontantPaye = decrypted != null ? decrypted : 0.0;
                }
            }

            double nouveauMontantPaye = ancienMontantPaye + montantAPayer;

            String updateSQL = "UPDATE transactions SET montantPaye = ? WHERE idTransaction = ?";
            PreparedStatement pstUpdate = conn.prepareStatement(updateSQL);
            pstUpdate.setString(1, EncryptionUtil.encrypt(nouveauMontantPaye));
            pstUpdate.setInt(2, idTransaction);
            pstUpdate.executeUpdate();

            System.out.println("✅ montantPaye : " + ancienMontantPaye + " → " + nouveauMontantPaye);

        } catch (SQLException e) {
            System.out.println("Erreur SQL updateMontantPaye : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Erreur updateMontantPaye : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ✅ ANOMALIE — Calcule la moyenne des montants de l'utilisateur
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Retourne la moyenne des montants des transactions de l'utilisateur.
     * Les montants sont chiffrés en base → on les déchiffre un par un.
     * Retourne 0.0 si aucune transaction.
     */
    public double getMoyenneMontant(int idUser) {
        String req = "SELECT montant FROM transactions WHERE idUser = ?";
        double total = 0.0;
        int count    = 0;

        try {
            PreparedStatement pst = conn.prepareStatement(req);
            pst.setInt(1, idUser);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                try {
                    Double montant = EncryptionUtil.decrypt(rs.getString("montant"));
                    if (montant != null && montant > 0) {
                        total += montant;
                        count++;
                    }
                } catch (Exception e) {
                    // ignorer les lignes non déchiffrables
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur getMoyenneMontant : " + e.getMessage());
        }

        return count > 0 ? total / count : 0.0;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ✅ ANOMALIE — Détecte si un montant est anormal (> 2.5× la moyenne)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Retourne true si le montant dépasse 2.5× la moyenne de l'utilisateur.
     *
     * Règle :
     *  - Si l'utilisateur n'a aucune transaction → seuil par défaut = 5000 DT
     *  - Sinon → seuil = moyenne × 2.5
     *
     * Exemple : moyenne = 300 DT → alerte si montant > 750 DT
     */
    public boolean isAnomalie(int idUser, double montant) {
        double moyenne = getMoyenneMontant(idUser);

        if (moyenne == 0.0) {
            // Aucun historique → seuil fixe de 5000 DT
            return montant > 5000.0;
        }

        double seuil = moyenne * 2.5;
        System.out.println("🔍 Anomalie check : montant=" + montant
                + " | moyenne=" + moyenne
                + " | seuil=" + seuil
                + " | anomalie=" + (montant > seuil));

        return montant > seuil;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Catégories distinctes
    // ══════════════════════════════════════════════════════════════════════════

    public List<String> getCategories() {
        String req = "SELECT DISTINCT categorie FROM transactions WHERE categorie IS NOT NULL ORDER BY categorie";
        List<String> categories = new ArrayList<>();
        try {
            Statement stm = conn.createStatement();
            ResultSet rs  = stm.executeQuery(req);
            while (rs.next()) categories.add(rs.getString("categorie"));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return categories;
    }
}
