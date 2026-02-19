package com.nexora.bank.Service;

import com.nexora.bank.Models.User;
import com.nexora.bank.Utils.EmailTemplates;
import com.nexora.bank.Utils.MyDB;
import com.nexora.bank.Utils.PasswordUtils;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long OTP_TTL_SECONDS = 300L;
    private static final String DEFAULT_ADMIN_EMAIL = "admin@nexora.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_DECLINED = "DECLINED";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String STATUS_BANNED = "BANNED";
    private static final int MYSQL_TABLE_NOT_IN_ENGINE = 1932;
    private static final String USER_SELECT_COLUMNS = """
        idUser, nom, prenom, email, telephone, role, status, password,
        created_at, account_opened_from, last_online_at, last_online_from, biometric_enabled
        """;

    private static final String SMTP_USERNAME = fromEnv("NEXORA_SMTP_EMAIL", "jfkdiekrjrjee06@gmail.com");
    private static final String SMTP_PASSWORD = fromEnv("NEXORA_SMTP_APP_PASSWORD", "hkmromvmrirxvhvj");
    private static final String SMTP_FROM_NAME = fromEnv("NEXORA_SMTP_FROM_NAME", "NEXORA Mail");

    private static final Map<String, OtpEntry> SIGNUP_OTP_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, OtpEntry> PASSWORD_RESET_OTP_CACHE = new ConcurrentHashMap<>();

    private final Connection connection;

    public UserService() {
        this.connection = MyDB.getInstance().getConn();
        if (this.connection != null) {
            ensureUsersTable();
            ensureUsersProfileColumns();
            harmonizeStatusConstraint();
            ensureDefaultAdmin();
        }
    }

    public void sendSignupOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        validateEmailFormat(normalizedEmail);

        if (emailExists(normalizedEmail)) {
            throw new IllegalStateException("This email is already registered.");
        }

        String otpCode = generateOtpCode();
        Instant expiresAt = Instant.now().plusSeconds(OTP_TTL_SECONDS);
        SIGNUP_OTP_CACHE.put(normalizedEmail, new OtpEntry(otpCode, expiresAt));
        sendOtpEmail(normalizedEmail, otpCode, expiresAt);
    }

    public boolean verifySignupOtp(String email, String otpCode) {
        return verifyOtpCode(SIGNUP_OTP_CACHE, normalizeEmail(email), otpCode);
    }

    public void sendPasswordResetOtpForUser(int idUser) {
        User user = requireRegularUser(idUser);
        String email = normalizeEmail(user.getEmail());
        String otpCode = generateOtpCode();
        Instant expiresAt = Instant.now().plusSeconds(OTP_TTL_SECONDS);
        PASSWORD_RESET_OTP_CACHE.put(email, new OtpEntry(otpCode, expiresAt));
        sendOtpEmail(email, otpCode, expiresAt);
    }

    public boolean resetOwnPasswordWithOtp(int idUser, String otpCode, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }

        User user = requireRegularUser(idUser);
        String email = normalizeEmail(user.getEmail());
        if (!verifyOtpCode(PASSWORD_RESET_OTP_CACHE, email, otpCode)) {
            throw new IllegalArgumentException("Invalid or expired OTP.");
        }

        String sql = "UPDATE users SET password = ? WHERE idUser = ? AND role = ?";
        String passwordHash = PasswordUtils.hashPassword(newPassword.trim());
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, passwordHash);
            preparedStatement.setInt(2, idUser);
            preparedStatement.setString(3, ROLE_USER);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to reset password.", ex);
        }
    }

    public User registerUser(String nom, String prenom, String email, String telephone, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        validateEmailFormat(normalizedEmail);

        if (emailExists(normalizedEmail)) {
            throw new IllegalStateException("This email is already registered.");
        }

        String passwordHash = PasswordUtils.hashPassword(rawPassword);
        String accountOpenedFrom = resolveClientContext();
        String sql = """
            INSERT INTO users (nom, prenom, email, telephone, role, status, password, account_opened_from, biometric_enabled)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, nom);
            preparedStatement.setString(2, prenom);
            preparedStatement.setString(3, normalizedEmail);
            preparedStatement.setString(4, telephone);
            preparedStatement.setString(5, ROLE_USER);
            preparedStatement.setString(6, STATUS_PENDING);
            preparedStatement.setString(7, passwordHash);
            preparedStatement.setString(8, accountOpenedFrom);
            preparedStatement.setBoolean(9, false);
            preparedStatement.executeUpdate();

            User saved = new User(nom, prenom, normalizedEmail, telephone, ROLE_USER, STATUS_PENDING, passwordHash);
            saved.setAccountOpenedFrom(accountOpenedFrom);
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    saved.setIdUser(generatedKeys.getInt(1));
                }
            }
            return saved;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to create user account.", ex);
        }
    }

    public User createUserByAdmin(
        String nom,
        String prenom,
        String email,
        String telephone,
        String role,
        String status,
        String rawPassword
    ) {
        String normalizedEmail = normalizeEmail(email);
        validateEmailFormat(normalizedEmail);

        if (safeText(nom).isBlank() || safeText(prenom).isBlank() || safeText(telephone).isBlank()) {
            throw new IllegalArgumentException("All fields are required.");
        }
        if (rawPassword == null || rawPassword.trim().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (emailExists(normalizedEmail)) {
            throw new IllegalStateException("This email is already registered.");
        }

        String normalizedRole = normalizeRole(role);
        String normalizedStatus = normalizeStatus(status);
        String passwordHash = PasswordUtils.hashPassword(rawPassword.trim());
        String sql = """
            INSERT INTO users (nom, prenom, email, telephone, role, status, password, account_opened_from, biometric_enabled)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, safeText(nom));
            preparedStatement.setString(2, safeText(prenom));
            preparedStatement.setString(3, normalizedEmail);
            preparedStatement.setString(4, safeText(telephone));
            preparedStatement.setString(5, normalizedRole);
            preparedStatement.setString(6, normalizedStatus);
            preparedStatement.setString(7, passwordHash);
            preparedStatement.setString(8, resolveClientContext() + " (created by admin)");
            preparedStatement.setBoolean(9, false);
            preparedStatement.executeUpdate();

            User saved = new User(
                safeText(nom),
                safeText(prenom),
                normalizedEmail,
                safeText(telephone),
                normalizedRole,
                normalizedStatus,
                passwordHash
            );
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    saved.setIdUser(generatedKeys.getInt(1));
                }
            }

            sendPlainEmail(
                normalizedEmail,
                EmailTemplates.accountCreatedByAdminSubject(),
                EmailTemplates.accountCreatedByAdminBody(
                    safeText(prenom).isBlank() ? safeText(nom) : safeText(prenom),
                    normalizedRole,
                    normalizedStatus,
                    rawPassword.trim()
                )
            );
            return saved;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to create user by admin.", ex);
        }
    }

    public Optional<User> authenticate(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        Optional<User> userOptional = findByEmail(normalizedEmail);
        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();
        if (!PasswordUtils.verifyPassword(rawPassword, user.getPassword())) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public void markUserOnline(int idUser) {
        if (idUser <= 0) {
            return;
        }
        String sql = """
            UPDATE users
            SET last_online_at = CURRENT_TIMESTAMP, last_online_from = ?
            WHERE idUser = ?
            """;
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, resolveClientContext());
            preparedStatement.setInt(2, idUser);
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update user online activity.", ex);
        }
    }

    public List<User> getAllUsers() {
        String sql = """
            SELECT %s
            FROM users
            ORDER BY idUser DESC
            """.formatted(USER_SELECT_COLUMNS);

        List<User> users = new ArrayList<>();
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
            return users;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch users.", ex);
        }
    }

    public boolean updateUser(User user) {
        if (user == null || user.getIdUser() <= 0) {
            return false;
        }

        String role = normalizeRole(user.getRole());
        String status = normalizeStatus(user.getStatus());
        String sql = """
            UPDATE users
            SET nom = ?, prenom = ?, email = ?, telephone = ?, role = ?, status = ?
            WHERE idUser = ?
            """;

        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, safeText(user.getNom()));
            preparedStatement.setString(2, safeText(user.getPrenom()));
            preparedStatement.setString(3, normalizeEmail(user.getEmail()));
            preparedStatement.setString(4, safeText(user.getTelephone()));
            preparedStatement.setString(5, role);
            preparedStatement.setString(6, status);
            preparedStatement.setInt(7, user.getIdUser());
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update user.", ex);
        }
    }

    public boolean updateUserOwnProfile(int idUser, String nom, String prenom, String email, String telephone) {
        if (idUser <= 0) {
            throw new IllegalArgumentException("Invalid user id.");
        }

        String normalizedEmail = normalizeEmail(email);
        validateEmailFormat(normalizedEmail);

        if (safeText(nom).isBlank() || safeText(prenom).isBlank() || safeText(telephone).isBlank()) {
            throw new IllegalArgumentException("All profile fields are required.");
        }
        if (emailExistsForAnotherUser(normalizedEmail, idUser)) {
            throw new IllegalStateException("This email is already used by another account.");
        }

        User target = requireRegularUser(idUser);
        if (!ROLE_USER.equalsIgnoreCase(target.getRole())) {
            throw new IllegalStateException("Only a regular user can update this profile.");
        }

        String sql = """
            UPDATE users
            SET nom = ?, prenom = ?, email = ?, telephone = ?
            WHERE idUser = ? AND role = ?
            """;
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, safeText(nom));
            preparedStatement.setString(2, safeText(prenom));
            preparedStatement.setString(3, normalizedEmail);
            preparedStatement.setString(4, safeText(telephone));
            preparedStatement.setInt(5, idUser);
            preparedStatement.setString(6, ROLE_USER);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update profile.", ex);
        }
    }

    public boolean updateUserOwnPassword(int idUser, String currentPassword, String newPassword) {
        if (idUser <= 0) {
            throw new IllegalArgumentException("Invalid user id.");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Current password is required.");
        }
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }

        User target = requireRegularUser(idUser);
        if (!PasswordUtils.verifyPassword(currentPassword, target.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        String sql = "UPDATE users SET password = ? WHERE idUser = ? AND role = ?";
        String passwordHash = PasswordUtils.hashPassword(newPassword.trim());
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, passwordHash);
            preparedStatement.setInt(2, idUser);
            preparedStatement.setString(3, ROLE_USER);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update password.", ex);
        }
    }

    public boolean updateUserBiometricPreference(int idUser, boolean enabled) {
        User target = requireRegularUser(idUser);
        if (!ROLE_USER.equalsIgnoreCase(target.getRole())) {
            throw new IllegalStateException("Only regular user biometric preference can be changed here.");
        }

        String sql = "UPDATE users SET biometric_enabled = ? WHERE idUser = ? AND role = ?";
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setBoolean(1, enabled);
            preparedStatement.setInt(2, idUser);
            preparedStatement.setString(3, ROLE_USER);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update biometric preference.", ex);
        }
    }

    public boolean updateAdminOwnProfile(int idUser, String nom, String prenom, String email, String telephone) {
        if (idUser <= 0) {
            throw new IllegalArgumentException("Invalid admin id.");
        }

        String normalizedEmail = normalizeEmail(email);
        validateEmailFormat(normalizedEmail);

        if (safeText(nom).isBlank() || safeText(prenom).isBlank() || safeText(telephone).isBlank()) {
            throw new IllegalArgumentException("All profile fields are required.");
        }
        if (emailExistsForAnotherUser(normalizedEmail, idUser)) {
            throw new IllegalStateException("This email is already used by another account.");
        }

        Optional<User> adminOptional = findById(idUser);
        if (adminOptional.isEmpty()) {
            return false;
        }

        User admin = adminOptional.get();
        if (!ROLE_ADMIN.equalsIgnoreCase(admin.getRole())) {
            throw new IllegalStateException("Only admin profile can be updated on this page.");
        }

        String sql = """
            UPDATE users
            SET nom = ?, prenom = ?, email = ?, telephone = ?
            WHERE idUser = ? AND role = ?
            """;

        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, safeText(nom));
            preparedStatement.setString(2, safeText(prenom));
            preparedStatement.setString(3, normalizedEmail);
            preparedStatement.setString(4, safeText(telephone));
            preparedStatement.setInt(5, idUser);
            preparedStatement.setString(6, ROLE_ADMIN);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update admin profile.", ex);
        }
    }

    public boolean updateAdminOwnPassword(int idUser, String currentPassword, String newPassword) {
        if (idUser <= 0) {
            throw new IllegalArgumentException("Invalid admin id.");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Current password is required.");
        }
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }

        Optional<User> adminOptional = findById(idUser);
        if (adminOptional.isEmpty()) {
            return false;
        }

        User admin = adminOptional.get();
        if (!ROLE_ADMIN.equalsIgnoreCase(admin.getRole())) {
            throw new IllegalStateException("Only admin password can be changed on this page.");
        }
        if (!PasswordUtils.verifyPassword(currentPassword, admin.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        String sql = "UPDATE users SET password = ? WHERE idUser = ? AND role = ?";
        String passwordHash = PasswordUtils.hashPassword(newPassword.trim());
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, passwordHash);
            preparedStatement.setInt(2, idUser);
            preparedStatement.setString(3, ROLE_ADMIN);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update admin password.", ex);
        }
    }

    public boolean deleteUser(int idUser) {
        String sql = "DELETE FROM users WHERE idUser = ?";
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setInt(1, idUser);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to delete user.", ex);
        }
    }

    public boolean approveUser(int idUser) {
        return updateStatusAndNotify(idUser, STATUS_ACTIVE, "approved");
    }

    public boolean declineUser(int idUser) {
        return updateStatusAndNotify(idUser, STATUS_DECLINED, "declined");
    }

    public boolean banUser(int idUser, String reason) {
        Optional<User> targetOptional = findById(idUser);
        if (targetOptional.isEmpty()) {
            return false;
        }

        User target = targetOptional.get();
        if (ROLE_ADMIN.equalsIgnoreCase(target.getRole())) {
            throw new IllegalStateException("Admin user cannot be banned.");
        }

        String sql = "UPDATE users SET status = ? WHERE idUser = ?";
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, STATUS_BANNED);
            preparedStatement.setInt(2, idUser);
            boolean updated = preparedStatement.executeUpdate() > 0;
            if (updated) {
                sendPlainEmail(
                    target.getEmail(),
                    EmailTemplates.accountBannedSubject(),
                    EmailTemplates.accountBannedBody(
                        safeText(target.getPrenom()).isBlank() ? safeText(target.getNom()) : safeText(target.getPrenom()),
                        safeText(reason)
                    )
                );
            }
            return updated;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to ban user.", ex);
        }
    }

    public boolean unbanUser(int idUser) {
        Optional<User> targetOptional = findById(idUser);
        if (targetOptional.isEmpty()) {
            return false;
        }

        User target = targetOptional.get();
        if (ROLE_ADMIN.equalsIgnoreCase(target.getRole())) {
            throw new IllegalStateException("Admin user cannot be unbanned.");
        }

        String sql = "UPDATE users SET status = ? WHERE idUser = ?";
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, STATUS_ACTIVE);
            preparedStatement.setInt(2, idUser);
            boolean updated = preparedStatement.executeUpdate() > 0;
            if (updated) {
                sendPlainEmail(
                    target.getEmail(),
                    EmailTemplates.accountUnbannedSubject(),
                    EmailTemplates.accountUnbannedBody(
                        safeText(target.getPrenom()).isBlank() ? safeText(target.getNom()) : safeText(target.getPrenom())
                    )
                );
            }
            return updated;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to unban user.", ex);
        }
    }

    public boolean updateUserPassword(int idUser, String rawPassword, boolean notifyByEmail) {
        if (rawPassword == null || rawPassword.trim().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        Optional<User> targetOptional = findById(idUser);
        if (targetOptional.isEmpty()) {
            return false;
        }

        User target = targetOptional.get();
        if (ROLE_ADMIN.equalsIgnoreCase(target.getRole())) {
            throw new IllegalStateException("Admin password cannot be changed from this page.");
        }

        String sql = "UPDATE users SET password = ? WHERE idUser = ?";
        String passwordHash = PasswordUtils.hashPassword(rawPassword.trim());
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, passwordHash);
            preparedStatement.setInt(2, idUser);
            boolean updated = preparedStatement.executeUpdate() > 0;
            if (updated && notifyByEmail) {
                sendPlainEmail(
                    target.getEmail(),
                    EmailTemplates.passwordResetByAdminSubject(),
                    EmailTemplates.passwordResetByAdminBody(rawPassword.trim())
                );
            }
            return updated;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update user password.", ex);
        }
    }

    public int sendPendingReviewReminderEmails() {
        List<User> users = getAllUsers();
        int sentCount = 0;
        for (User user : users) {
            if (ROLE_USER.equalsIgnoreCase(user.getRole()) && STATUS_PENDING.equalsIgnoreCase(user.getStatus())) {
                sendStatusEmail(user.getEmail(), "pending");
                sentCount++;
            }
        }
        return sentCount;
    }

    public Optional<User> findByEmail(String email) {
        String sql = """
            SELECT %s
            FROM users
            WHERE email = ?
            LIMIT 1
            """.formatted(USER_SELECT_COLUMNS);

        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, normalizeEmail(email));
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch user by email.", ex);
        }
    }

    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, normalizeEmail(email));
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to check if email exists.", ex);
        }
    }

    public Optional<User> findByIdPublic(int idUser) {
        return findById(idUser);
    }

    public void sendCustomEmail(String recipientEmail, String subject, String htmlContent) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is required.");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Email subject is required.");
        }
        if (htmlContent == null || htmlContent.isBlank()) {
            throw new IllegalArgumentException("Email content is required.");
        }
        sendPlainEmail(recipientEmail.trim(), subject.trim(), htmlContent);
    }

    private void ensureUsersTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                idUser INT NOT NULL AUTO_INCREMENT,
                nom VARCHAR(80) NOT NULL,
                prenom VARCHAR(80) NOT NULL,
                email VARCHAR(190) NOT NULL,
                telephone VARCHAR(30) NOT NULL,
                role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
                status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                password VARCHAR(255) NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                account_opened_from VARCHAR(180) NOT NULL DEFAULT 'Unknown device',
                last_online_at TIMESTAMP NULL DEFAULT NULL,
                last_online_from VARCHAR(180) NULL,
                biometric_enabled TINYINT(1) NOT NULL DEFAULT 0,
                PRIMARY KEY (idUser),
                UNIQUE KEY uq_users_email (email),
                KEY idx_users_role (role)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
            """;

        Connection db = requireConnection();
        try (Statement statement = db.createStatement()) {
            statement.execute(sql);
            validateUsersTable(statement);
        } catch (SQLException ex) {
            if (isUsersTableBroken(ex)) {
                rebuildUsersTable(db, sql);
                return;
            }
            throw new RuntimeException("Failed to ensure users table.", ex);
        }
    }

    private void ensureUsersProfileColumns() {
        Connection db = requireConnection();
        try (Statement statement = db.createStatement()) {
            ensureColumnExists(db, statement, "account_opened_from",
                "ALTER TABLE users ADD COLUMN account_opened_from VARCHAR(180) NOT NULL DEFAULT 'Unknown device'");
            ensureColumnExists(db, statement, "last_online_at",
                "ALTER TABLE users ADD COLUMN last_online_at TIMESTAMP NULL DEFAULT NULL");
            ensureColumnExists(db, statement, "last_online_from",
                "ALTER TABLE users ADD COLUMN last_online_from VARCHAR(180) NULL");
            ensureColumnExists(db, statement, "biometric_enabled",
                "ALTER TABLE users ADD COLUMN biometric_enabled TINYINT(1) NOT NULL DEFAULT 0");

            statement.executeUpdate(
                "UPDATE users SET account_opened_from = 'Unknown device' WHERE account_opened_from IS NULL OR TRIM(account_opened_from) = ''"
            );
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to ensure users profile/security columns.", ex);
        }
    }

    private void ensureColumnExists(Connection db, Statement statement, String column, String ddl) throws SQLException {
        if (columnExists(db, column)) {
            return;
        }
        statement.execute(ddl);
    }

    private boolean columnExists(Connection db, String columnName) throws SQLException {
        DatabaseMetaData metaData = db.getMetaData();
        try (ResultSet columns = metaData.getColumns(db.getCatalog(), null, "users", columnName)) {
            if (columns.next()) {
                return true;
            }
        }
        try (ResultSet columns = metaData.getColumns(db.getCatalog(), null, "USERS", columnName.toUpperCase())) {
            return columns.next();
        }
    }

    private void validateUsersTable(Statement statement) throws SQLException {
        statement.executeQuery("SELECT 1 FROM users LIMIT 1");
    }

    private void rebuildUsersTable(Connection db, String createSql) {
        try (Statement statement = db.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS users");
            statement.execute(createSql);
            validateUsersTable(statement);
        } catch (SQLException rebuildEx) {
            throw new RuntimeException("Failed to rebuild users table.", rebuildEx);
        }
    }

    private boolean isUsersTableBroken(SQLException ex) {
        if (ex == null) {
            return false;
        }
        if (ex.getErrorCode() == MYSQL_TABLE_NOT_IN_ENGINE) {
            return true;
        }
        String message = ex.getMessage();
        if (message != null) {
            String normalized = message.toLowerCase();
            if (normalized.contains("doesn't exist in engine")) {
                return true;
            }
        }
        return isUsersTableBroken(ex.getNextException());
    }

    private void harmonizeStatusConstraint() {
        Connection db = requireConnection();
        try (Statement statement = db.createStatement()) {
            try {
                statement.execute("ALTER TABLE users DROP CONSTRAINT chk_users_status");
            } catch (SQLException ignored) {
            }
            try {
                statement.execute("ALTER TABLE users DROP CHECK chk_users_status");
            } catch (SQLException ignored) {
            }
            try {
                statement.execute(
                    "ALTER TABLE users ADD CONSTRAINT chk_users_status CHECK (status in ('PENDING','ACTIVE','DECLINED','INACTIVE','BANNED'))"
                );
            } catch (SQLException ignored) {
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to align users status constraint.", ex);
        }
    }

    private void ensureDefaultAdmin() {
        String insertSql = """
            INSERT INTO users (nom, prenom, email, telephone, role, status, password, account_opened_from, biometric_enabled)
            SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?
            WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = ?)
            """;

        Connection db = requireConnection();
        try {
            executeEnsureDefaultAdmin(db, insertSql);
        } catch (SQLException ex) {
            if (isUsersTableBroken(ex)) {
                ensureUsersTable();
                ensureUsersProfileColumns();
                try {
                    executeEnsureDefaultAdmin(db, insertSql);
                    return;
                } catch (SQLException retryEx) {
                    throw new RuntimeException("Failed to ensure default admin user.", retryEx);
                }
            }
            throw new RuntimeException("Failed to ensure default admin user.", ex);
        }
    }

    private void executeEnsureDefaultAdmin(Connection db, String insertSql) throws SQLException {
        try (PreparedStatement preparedStatement = db.prepareStatement(insertSql)) {
            preparedStatement.setString(1, "System");
            preparedStatement.setString(2, "Admin");
            preparedStatement.setString(3, DEFAULT_ADMIN_EMAIL);
            preparedStatement.setString(4, "+21600000000");
            preparedStatement.setString(5, ROLE_ADMIN);
            preparedStatement.setString(6, STATUS_ACTIVE);
            preparedStatement.setString(7, PasswordUtils.hashPassword(DEFAULT_ADMIN_PASSWORD));
            preparedStatement.setString(8, "System bootstrap");
            preparedStatement.setBoolean(9, false);
            preparedStatement.setString(10, DEFAULT_ADMIN_EMAIL);
            preparedStatement.executeUpdate();
        }
    }

    private void sendOtpEmail(String recipientEmail, String otpCode, Instant expiresAt) {
        sendPlainEmail(
            recipientEmail,
            EmailTemplates.otpSubject(),
            EmailTemplates.otpBody(otpCode, expiresAt)
        );
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User(
            resultSet.getInt("idUser"),
            resultSet.getString("nom"),
            resultSet.getString("prenom"),
            resultSet.getString("email"),
            resultSet.getString("telephone"),
            resultSet.getString("role"),
            resultSet.getString("status"),
            resultSet.getString("password")
        );

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        Timestamp lastOnlineAt = resultSet.getTimestamp("last_online_at");
        user.setCreatedAt(createdAt == null ? "" : createdAt.toLocalDateTime().toString());
        user.setAccountOpenedFrom(resultSet.getString("account_opened_from"));
        user.setLastOnlineAt(lastOnlineAt == null ? "" : lastOnlineAt.toLocalDateTime().toString());
        user.setLastOnlineFrom(resultSet.getString("last_online_from"));
        user.setBiometricEnabled(resultSet.getBoolean("biometric_enabled"));
        return user;
    }

    private String generateOtpCode() {
        int number = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(number);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeRole(String role) {
        String value = role == null ? "" : role.trim().toUpperCase();
        if (ROLE_ADMIN.equals(value)) {
            return ROLE_ADMIN;
        }
        return ROLE_USER;
    }

    private String normalizeStatus(String status) {
        String value = status == null ? "" : status.trim().toUpperCase();
        if (STATUS_ACTIVE.equals(value) || STATUS_PENDING.equals(value)
            || STATUS_DECLINED.equals(value) || STATUS_INACTIVE.equals(value) || STATUS_BANNED.equals(value)) {
            return value;
        }
        return STATUS_PENDING;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private void validateEmailFormat(String email) {
        if (email.isBlank() || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
    }

    private static String fromEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private boolean updateStatusAndNotify(int idUser, String newStatus, String notificationType) {
        Optional<User> targetOptional = findById(idUser);
        if (targetOptional.isEmpty()) {
            return false;
        }

        User target = targetOptional.get();
        if (ROLE_ADMIN.equalsIgnoreCase(target.getRole())) {
            return false;
        }

        String sql = "UPDATE users SET status = ? WHERE idUser = ?";
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, newStatus);
            preparedStatement.setInt(2, idUser);
            boolean updated = preparedStatement.executeUpdate() > 0;
            if (updated) {
                sendStatusEmail(target.getEmail(), notificationType);
            }
            return updated;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update user status.", ex);
        }
    }

    private Optional<User> findById(int idUser) {
        String sql = """
            SELECT %s
            FROM users
            WHERE idUser = ?
            LIMIT 1
            """.formatted(USER_SELECT_COLUMNS);
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setInt(1, idUser);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to find user by id.", ex);
        }
    }

    private boolean emailExistsForAnotherUser(String email, int currentUserId) {
        String sql = "SELECT 1 FROM users WHERE email = ? AND idUser <> ? LIMIT 1";
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, normalizeEmail(email));
            preparedStatement.setInt(2, currentUserId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to check duplicate email.", ex);
        }
    }

    private User requireRegularUser(int idUser) {
        Optional<User> userOptional = findById(idUser);
        if (userOptional.isEmpty()) {
            throw new IllegalStateException("User account not found.");
        }
        User user = userOptional.get();
        if (!ROLE_USER.equalsIgnoreCase(user.getRole())) {
            throw new IllegalStateException("This action is only available for regular user accounts.");
        }
        return user;
    }

    private boolean verifyOtpCode(Map<String, OtpEntry> cache, String normalizedEmail, String otpCode) {
        OtpEntry otpEntry = cache.get(normalizedEmail);
        if (otpEntry == null) {
            return false;
        }
        if (Instant.now().isAfter(otpEntry.expiresAt())) {
            cache.remove(normalizedEmail);
            return false;
        }
        boolean valid = otpEntry.code().equals(otpCode == null ? "" : otpCode.trim());
        if (valid) {
            cache.remove(normalizedEmail);
        }
        return valid;
    }

    private String resolveClientContext() {
        String userName = System.getProperty("user.name", "unknown-user");
        String hostName = "unknown-host";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }
        String os = System.getProperty("os.name", "Unknown OS");
        return userName + "@" + hostName + " (" + os + ")";
    }

    private void sendStatusEmail(String recipientEmail, String type) {
        String subject;
        String content;

        switch (type) {
            case "approved" -> {
                subject = EmailTemplates.accountApprovedSubject();
                content = EmailTemplates.accountApprovedBody();
            }
            case "declined" -> {
                subject = EmailTemplates.accountDeclinedSubject();
                content = EmailTemplates.accountDeclinedBody();
            }
            default -> {
                subject = EmailTemplates.accountPendingSubject();
                content = EmailTemplates.accountPendingBody();
            }
        }

        sendPlainEmail(recipientEmail, subject, content);
    }

    private void sendPlainEmail(String recipientEmail, String subject, String content) {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USERNAME, SMTP_FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=UTF-8");
            Transport.send(message);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send email notification.", ex);
        }
    }

    private record OtpEntry(String code, Instant expiresAt) {
    }

    private Connection requireConnection() {
        if (connection == null) {
            throw new IllegalStateException("Database connection is unavailable.");
        }
        return connection;
    }
}
