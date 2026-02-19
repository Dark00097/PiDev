package com.nexora.bank.Service;

import com.nexora.bank.Models.Transaction;
import com.nexora.bank.Models.ICrud;
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

    @Override
    public void add(Transaction t) {
        String SQL = "INSERT INTO transactions " +
                "(idUser, categorie, dateTransaction, montant, typeTransaction, statutTransaction, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try {
            String montantChiffre = EncryptionUtil.encrypt(t.getMontant());

            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setInt(1, t.getIdUser());
            pst.setString(2, t.getCategorie());
            pst.setDate(3, t.getDateTransaction() != null ? Date.valueOf(t.getDateTransaction()) : null);
            pst.setString(4, montantChiffre);
            pst.setString(5, t.getTypeTransaction());
            pst.setString(6, t.getStatutTransaction());
            pst.setString(7, t.getDescription());
            pst.executeUpdate();
            System.out.println("Transaction ajoutée avec succès (montant chiffré)");
        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erreur de chiffrement : " + e.getMessage());
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
            System.out.println("Transaction modifiée avec succès (montant chiffré)");
        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erreur de chiffrement : " + e.getMessage());
        }
    }

    @Override
    public void remove(Transaction t) {
        String SQL = "DELETE FROM transactions WHERE idTransaction = ?";
        try {
            PreparedStatement pst = conn.prepareStatement(SQL);
            pst.setInt(1, t.getIdTransaction());
            pst.executeUpdate();
            System.out.println("Transaction supprimée avec succès");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Transaction> getAll() {
        String req = "SELECT * FROM transactions";
        ArrayList<Transaction> transactions = new ArrayList<>();

        try {
            Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {
                Transaction t = new Transaction();
                t.setIdTransaction(rs.getInt("idTransaction"));
                t.setIdUser(rs.getInt("idUser"));
                t.setCategorie(rs.getString("categorie"));
                Date sqlDate = rs.getDate("dateTransaction");
                t.setDateTransaction(sqlDate != null ? sqlDate.toLocalDate() : null);

                String montantChiffre = rs.getString("montant");
                t.setMontant(EncryptionUtil.decrypt(montantChiffre));

                t.setTypeTransaction(rs.getString("typeTransaction"));
                t.setStatutTransaction(rs.getString("statutTransaction"));
                t.setDescription(rs.getString("description"));
                transactions.add(t);
            }
        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erreur de déchiffrement : " + e.getMessage());
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

                String montantChiffre = rs.getString("montant");
                t.setMontant(EncryptionUtil.decrypt(montantChiffre));

                t.setTypeTransaction(rs.getString("typeTransaction"));
                t.setStatutTransaction(rs.getString("statutTransaction"));
                t.setDescription(rs.getString("description"));
                transactions.add(t);
            }
        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erreur de déchiffrement : " + e.getMessage());
        }

        return transactions;
    }

    public List<String> getCategories() {
        String req = "SELECT DISTINCT categorie FROM transactions WHERE categorie IS NOT NULL ORDER BY categorie";
        List<String> categories = new ArrayList<>();

        try {
            Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                categories.add(rs.getString("categorie"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return categories;
    }
}