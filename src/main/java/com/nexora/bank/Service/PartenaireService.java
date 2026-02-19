package com.nexora.bank.Service;

import com.nexora.bank.Models.Partenaire;
import com.nexora.bank.Utils.MyDB;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PartenaireService {

    private final Connection connection;

    public PartenaireService() {
        this.connection = MyDB.getInstance().getConn();
        ensurePartenaireTable();
    }

    public List<Partenaire> getAllPartenaires() {
        String sql = """
            SELECT idPartenaire, nom, categorie, description, tauxCashback, tauxCashbackMax,
                   plafondMensuel, conditions, status
            FROM partenaire
            ORDER BY nom ASC
            """;

        List<Partenaire> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapPartenaire(rs));
            }
            return result;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch partenaires.", ex);
        }
    }

    public Optional<Partenaire> findByName(String name) {
        String sql = """
            SELECT idPartenaire, nom, categorie, description, tauxCashback, tauxCashbackMax,
                   plafondMensuel, conditions, status
            FROM partenaire
            WHERE LOWER(TRIM(nom)) = LOWER(TRIM(?))
            LIMIT 1
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, safeText(name));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPartenaire(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to find partenaire by name.", ex);
        }
    }

    public int createPartenaire(Partenaire partenaire) {
        String sql = """
            INSERT INTO partenaire (nom, categorie, description, tauxCashback, tauxCashbackMax, plafondMensuel, conditions, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillPartenaire(ps, partenaire);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to create partenaire.", ex);
        }
    }

    public boolean updatePartenaire(Partenaire partenaire) {
        String sql = """
            UPDATE partenaire
            SET nom = ?, categorie = ?, description = ?, tauxCashback = ?, tauxCashbackMax = ?,
                plafondMensuel = ?, conditions = ?, status = ?
            WHERE idPartenaire = ?
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            fillPartenaire(ps, partenaire);
            ps.setInt(9, partenaire.getIdPartenaire());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update partenaire.", ex);
        }
    }

    public boolean deletePartenaire(int idPartenaire) {
        String sql = "DELETE FROM partenaire WHERE idPartenaire = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idPartenaire);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete partenaire.", ex);
        }
    }

    private void fillPartenaire(PreparedStatement ps, Partenaire partenaire) throws SQLException {
        ps.setString(1, safeText(partenaire.getNom()));
        ps.setString(2, safeText(partenaire.getCategorie()));
        ps.setString(3, safeText(partenaire.getDescription()));
        ps.setDouble(4, partenaire.getTauxCashback());
        ps.setDouble(5, partenaire.getTauxCashbackMax());
        ps.setDouble(6, partenaire.getPlafondMensuel());
        ps.setString(7, safeText(partenaire.getConditions()));
        String status = safeText(partenaire.getStatus());
        ps.setString(8, status.isBlank() ? "Actif" : status);
    }

    private Partenaire mapPartenaire(ResultSet rs) throws SQLException {
        return new Partenaire(
                rs.getInt("idPartenaire"),
                rs.getString("nom"),
                rs.getString("categorie"),
                rs.getString("description"),
                rs.getDouble("tauxCashback"),
                rs.getDouble("tauxCashbackMax"),
                rs.getDouble("plafondMensuel"),
                rs.getString("conditions"),
                rs.getString("status")
        );
    }

    private void ensurePartenaireTable() {
        String createSql = """
            CREATE TABLE IF NOT EXISTS partenaire (
                idPartenaire INT AUTO_INCREMENT PRIMARY KEY,
                nom VARCHAR(100) NOT NULL,
                categorie VARCHAR(50) NOT NULL,
                description VARCHAR(255) NULL,
                tauxCashback DOUBLE NOT NULL,
                tauxCashbackMax DOUBLE NOT NULL,
                plafondMensuel DOUBLE NOT NULL,
                conditions VARCHAR(255) NULL,
                status VARCHAR(30) NOT NULL DEFAULT 'Actif'
            )
            """;

        try (Statement st = connection.createStatement()) {
            st.execute(createSql);
            ensureColumn("status", "ALTER TABLE partenaire ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'Actif'");
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to ensure partenaire table.", ex);
        }
    }

    private void ensureColumn(String columnName, String alterSql) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, "partenaire", columnName)) {
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
