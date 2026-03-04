package com.nexora.bank.Service;

import com.nexora.bank.Models.User;
import com.nexora.bank.Models.UserActionLog;
import com.nexora.bank.Utils.AIResponseFormatter.FormattedAnalysis;
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
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long OTP_TTL_SECONDS = 300L;
    private static final int PASSWORD_RESET_MAX_FAILED_ATTEMPTS = 5;
    private static final long PASSWORD_RESET_BLOCK_SECONDS = 600L;
    private static final String DEFAULT_ADMIN_EMAIL = "admin@nexora.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_DECLINED = "DECLINED";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String STATUS_BANNED = "BANNED";
    private static final int AI_STRONG_PASSWORD_LENGTH = 14;
    private static final String PASSWORD_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String PASSWORD_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String PASSWORD_DIGITS = "0123456789";
    private static final String PASSWORD_SYMBOLS = "!@#$%^&*()-_=+[]{}<>?";
    private static final String PASSWORD_ALL = PASSWORD_LOWER + PASSWORD_UPPER + PASSWORD_DIGITS + PASSWORD_SYMBOLS;
    private static final int MYSQL_TABLE_NOT_IN_ENGINE = 1932;
    private static final String USER_SELECT_COLUMNS = """
        idUser, nom, prenom, email, telephone, role, status, password,
        created_at, account_opened_from, account_opened_location, account_opened_lat, account_opened_lng,
        last_online_at, last_online_from, profile_image_path, biometric_enabled
        """;

    private static final String SMTP_USERNAME = fromEnv("NEXORA_SMTP_EMAIL", "jfkdiekrjrjee06@gmail.com");
    private static final String SMTP_PASSWORD = fromEnv("NEXORA_SMTP_APP_PASSWORD", "hkmromvmrirxvhvj");
    private static final String SMTP_FROM_NAME = fromEnv("NEXORA_SMTP_FROM_NAME", "NEXORA Mail");

    private static final Map<String, OtpEntry> SIGNUP_OTP_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, OtpEntry> PASSWORD_RESET_OTP_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, PasswordResetGuard> PASSWORD_RESET_GUARD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Instant> PASSWORD_RESET_VERIFIED_CACHE = new ConcurrentHashMap<>();

    private final Connection connection;
    private final NotificationService notificationService;
    private final GeoLocationService geoLocationService;

    public UserService() {
        this.connection = MyDB.getInstance().getConn();
        this.geoLocationService = new GeoLocationService();
        this.notificationService = this.connection == null ? null : new NotificationService(this.connection);
        if (this.connection != null) {
            ensureUsersTable();
            ensureUsersProfileColumns();
            ensureUserActivityTable();
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
        assertPasswordResetNotBlocked(email);
        PASSWORD_RESET_VERIFIED_CACHE.remove(email);
        String otpCode = generateOtpCode();
        Instant expiresAt = Instant.now().plusSeconds(OTP_TTL_SECONDS);
        PASSWORD_RESET_OTP_CACHE.put(email, new OtpEntry(otpCode, expiresAt));
        sendOtpEmail(email, otpCode, expiresAt);
    }

    public void sendPasswordResetOtpByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        validateEmailFormat(normalizedEmail);
        assertPasswordResetNotBlocked(normalizedEmail);

        Optional<User> userOptional = findByEmail(normalizedEmail);
        if (userOptional.isEmpty()) {
            throw new IllegalStateException("No user account found for this email.");
        }

        User user = userOptional.get();
        if (!ROLE_USER.equalsIgnoreCase(user.getRole())) {
            throw new IllegalStateException("Password reset from this page is available for user accounts only.");
        }

        PASSWORD_RESET_VERIFIED_CACHE.remove(normalizedEmail);
        String otpCode = generateOtpCode();
        Instant expiresAt = Instant.now().plusSeconds(OTP_TTL_SECONDS);
        PASSWORD_RESET_OTP_CACHE.put(normalizedEmail, new OtpEntry(otpCode, expiresAt));
        sendOtpEmail(normalizedEmail, otpCode, expiresAt);
    }

    public boolean verifyPasswordResetOtpByEmail(String email, String otpCode) {
        String normalizedEmail = normalizeEmail(email);
        validateEmailFormat(normalizedEmail);

        Optional<User> userOptional = findByEmail(normalizedEmail);
        if (userOptional.isEmpty()) {
            throw new IllegalStateException("No user account found for this email.");
        }

        User user = userOptional.get();
        if (!ROLE_USER.equalsIgnoreCase(user.getRole())) {
            throw new IllegalStateException("Password reset from this page is available for user accounts only.");
        }

        verifyPasswordResetOtpWithProtection(normalizedEmail, otpCode);
        PASSWORD_RESET_VERIFIED_CACHE.put(normalizedEmail, Instant.now().plusSeconds(PASSWORD_RESET_BLOCK_SECONDS));
        return true;
    }

    public boolean resetOwnPasswordWithOtp(int idUser, String otpCode, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }

        User user = requireRegularUser(idUser);
        String email = normalizeEmail(user.getEmail());
        verifyPasswordResetOtpWithProtection(email, otpCode);

        String sql = "UPDATE users SET password = ? WHERE idUser = ? AND role = ?";
        String passwordHash = PasswordUtils.hashPassword(newPassword.trim());
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, passwordHash);
            preparedStatement.setInt(2, idUser);
            preparedStatement.setString(3, ROLE_USER);
            boolean updated = preparedStatement.executeUpdate() > 0;
            if (updated) {
                logUserAction(idUser, "PASSWORD_RESET_OTP", resolveClientContext(), "Password reset using OTP.");
            }
            return updated;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to reset password.", ex);
        }
    }

    public boolean resetPasswordByEmailWithOtp(String email, String otpCode, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }

        String normalizedEmail = normalizeEmail(email);
        validateEmailFormat(normalizedEmail);

        Optional<User> userOptional = findByEmail(normalizedEmail);
        if (userOptional.isEmpty()) {
            throw new IllegalStateException("No user account found for this email.");
        }

        User user = userOptional.get();
        if (!ROLE_USER.equalsIgnoreCase(user.getRole())) {
            throw new IllegalStateException("Password reset from this page is available for user accounts only.");
        }

        verifyPasswordResetOtpWithProtection(normalizedEmail, otpCode);

        String sql = "UPDATE users SET password = ? WHERE idUser = ? AND role = ?";
        String passwordHash = PasswordUtils.hashPassword(newPassword.trim());
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, passwordHash);
            preparedStatement.setInt(2, user.getIdUser());
            preparedStatement.setString(3, ROLE_USER);
            boolean updated = preparedStatement.executeUpdate() > 0;
            if (updated) {
                logUserAction(
                    user.getIdUser(),
                    "PASSWORD_RESET_OTP",
                    resolveClientContext(),
                    "Password reset after OTP verification."
                );
            }
            return updated;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to reset password.", ex);
        }
    }

    public boolean resetPasswordByVerifiedEmail(String email, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }

        String normalizedEmail = normalizeEmail(email);
        validateEmailFormat(normalizedEmail);
        assertPasswordResetVerified(normalizedEmail);

        Optional<User> userOptional = findByEmail(normalizedEmail);
        if (userOptional.isEmpty()) {
            throw new IllegalStateException("No user account found for this email.");
        }

        User user = userOptional.get();
        if (!ROLE_USER.equalsIgnoreCase(user.getRole())) {
            throw new IllegalStateException("Password reset from this page is available for user accounts only.");
        }

        String sql = "UPDATE users SET password = ? WHERE idUser = ? AND role = ?";
        String passwordHash = PasswordUtils.hashPassword(newPassword.trim());
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, passwordHash);
            preparedStatement.setInt(2, user.getIdUser());
            preparedStatement.setString(3, ROLE_USER);
            boolean updated = preparedStatement.executeUpdate() > 0;
            if (updated) {
                PASSWORD_RESET_VERIFIED_CACHE.remove(normalizedEmail);
            }
            return updated;
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
        GeoLocationService.LocationSnapshot accountLocation = geoLocationService.resolveCurrentLocation();
        String sql = """
            INSERT INTO users (
                nom, prenom, email, telephone, role, status, password,
                account_opened_from, account_opened_location, account_opened_lat, account_opened_lng, biometric_enabled
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            preparedStatement.setString(9, safeLocation(accountLocation.displayName()));
            setNullableDouble(preparedStatement, 10, accountLocation.latitude());
            setNullableDouble(preparedStatement, 11, accountLocation.longitude());
            preparedStatement.setBoolean(12, false);
            preparedStatement.executeUpdate();

            User saved = new User(nom, prenom, normalizedEmail, telephone, ROLE_USER, STATUS_PENDING, passwordHash);
            saved.setAccountOpenedFrom(accountOpenedFrom);
            saved.setAccountOpenedLocation(safeLocation(accountLocation.displayName()));
            saved.setAccountOpenedLatitude(accountLocation.latitude());
            saved.setAccountOpenedLongitude(accountLocation.longitude());
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    saved.setIdUser(generatedKeys.getInt(1));
                }
            }
            logUserAction(
                saved.getIdUser(),
                "SIGNUP",
                accountOpenedFrom,
                "Account created and waiting for admin approval."
            );
            runNotificationSafely(() -> notificationService.notifyAdminsAboutPendingUser(saved));
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
        GeoLocationService.LocationSnapshot accountLocation = geoLocationService.resolveCurrentLocation();
        String sql = """
            INSERT INTO users (
                nom, prenom, email, telephone, role, status, password,
                account_opened_from, account_opened_location, account_opened_lat, account_opened_lng, biometric_enabled
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            preparedStatement.setString(9, safeLocation(accountLocation.displayName()));
            setNullableDouble(preparedStatement, 10, accountLocation.latitude());
            setNullableDouble(preparedStatement, 11, accountLocation.longitude());
            preparedStatement.setBoolean(12, false);
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

            runNotificationSafely(() -> {
                notificationService.notifyUserAccountUpdatedByAdmin(
                    saved.getIdUser(),
                    "Your account was created by admin. Current status: " + normalizedStatus + "."
                );
                if (STATUS_ACTIVE.equalsIgnoreCase(normalizedStatus)
                    || STATUS_DECLINED.equalsIgnoreCase(normalizedStatus)
                    || STATUS_INACTIVE.equalsIgnoreCase(normalizedStatus)
                    || STATUS_BANNED.equalsIgnoreCase(normalizedStatus)) {
                    notificationService.notifyUserAccountStatusChanged(saved.getIdUser(), normalizedStatus);
                }
            });
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
            String source = resolveClientContext();
            preparedStatement.setString(1, source);
            preparedStatement.setInt(2, idUser);
            preparedStatement.executeUpdate();
            logUserAction(idUser, "LOGIN", source, "User login recorded.");
            refreshAccountOpenedLocationIfMissing(idUser);
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update user online activity.", ex);
        }
    }

    public void refreshAccountOpenedLocationIfMissing(int idUser) {
        if (idUser <= 0) {
            return;
        }

        GeoLocationService.LocationSnapshot snapshot = geoLocationService.resolveCurrentLocation();
        String location = safeLocation(snapshot.displayName());
        Double latitude = snapshot.latitude();
        Double longitude = snapshot.longitude();

        if ("Unknown location".equalsIgnoreCase(location) && latitude == null && longitude == null) {
            return;
        }

        String sql = """
            UPDATE users
            SET account_opened_location = ?, account_opened_lat = ?, account_opened_lng = ?
            WHERE idUser = ?
              AND (
                   account_opened_location IS NULL
                OR TRIM(account_opened_location) = ''
                OR account_opened_location = 'Unknown location'
                OR account_opened_lat IS NULL
                OR account_opened_lng IS NULL
              )
            """;

        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setString(1, location);
            setNullableDouble(preparedStatement, 2, latitude);
            setNullableDouble(preparedStatement, 3, longitude);
            preparedStatement.setInt(4, idUser);
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Failed to backfill account opened location: " + ex.getMessage());
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

    public List<UserActionLog> getRecentUserActions(int idUser, int limit) {
        if (idUser <= 0) {
            return List.of();
        }

        int effectiveLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        String sql = """
            SELECT idAction, idUser, action_type, action_source, details, created_at
            FROM user_activity_log
            WHERE idUser = ?
            ORDER BY created_at DESC, idAction DESC
            LIMIT ?
            """;

        List<UserActionLog> actions = new ArrayList<>();
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setInt(1, idUser);
            preparedStatement.setInt(2, effectiveLimit);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    actions.add(mapUserAction(resultSet));
                }
            }
            return actions;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch user activity logs.", ex);
        }
    }

    public List<UserActionLog> getAllUserActions(int limit) {
        int effectiveLimit = limit <= 0 ? 500 : Math.min(limit, 5000);
        String sql = """
            SELECT idAction, idUser, action_type, action_source, details, created_at
            FROM user_activity_log
            ORDER BY created_at DESC, idAction DESC
            LIMIT ?
            """;

        List<UserActionLog> actions = new ArrayList<>();
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setInt(1, effectiveLimit);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    actions.add(mapUserAction(resultSet));
                }
            }
            return actions;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch all user activity logs.", ex);
        }
    }

    public boolean updateUser(User user) {
        if (user == null || user.getIdUser() <= 0) {
            return false;
        }

        Optional<User> beforeOptional = findById(user.getIdUser());
        if (beforeOptional.isEmpty()) {
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
            boolean updated = preparedStatement.executeUpdate() > 0;
            if (updated && !ROLE_ADMIN.equalsIgnoreCase(role)) {
                User before = beforeOptional.get();
                String details = buildAccountUpdateDetails(before, user, role, status);
                runNotificationSafely(() -> {
                    notificationService.notifyUserAccountUpdatedByAdmin(user.getIdUser(), details);
                    if (!Objects.equals(safeText(before.getStatus()).toUpperCase(), status)) {
                        notificationService.notifyUserAccountStatusChanged(user.getIdUser(), status);
                    }
                });
            }
            return updated;
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
            boolean updated = preparedStatement.executeUpdate() > 0;
            if (updated) {
                logUserAction(idUser, "PROFILE_UPDATE", resolveClientContext(), "Profile fields were updated.");
            }
            return updated;
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
            boolean updated = preparedStatement.executeUpdate() > 0;
            if (updated) {
                logUserAction(idUser, "PASSWORD_CHANGE", resolveClientContext(), "Password changed by user.");
            }
            return updated;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update password.", ex);
        }
    }

    public record AiSecurityUpdateResult(
        boolean profileUpdated,
        boolean passwordStrengthened,
        boolean emailSent,
        String temporaryPassword,
        String riskLevel
    ) {
    }

    public AiSecurityUpdateResult secureUserOwnAccountWithAi(
        int idUser,
        String nom,
        String prenom,
        String email,
        String telephone,
        FormattedAnalysis analysis
    ) {
        if (idUser <= 0) {
            throw new IllegalArgumentException("Invalid user id.");
        }

        String cleanedNom = safeText(nom);
        String cleanedPrenom = safeText(prenom);
        String cleanedTelephone = safeText(telephone);
        String normalizedEmail = normalizeEmail(email);

        if (cleanedNom.isBlank() || cleanedPrenom.isBlank() || cleanedTelephone.isBlank()) {
            throw new IllegalArgumentException("All profile fields are required.");
        }

        validateEmailFormat(normalizedEmail);
        if (emailExistsForAnotherUser(normalizedEmail, idUser)) {
            throw new IllegalStateException("This email is already used by another account.");
        }

        User target = requireRegularUser(idUser);
        boolean profileNeedsUpdate =
            !Objects.equals(safeText(target.getNom()), cleanedNom)
                || !Objects.equals(safeText(target.getPrenom()), cleanedPrenom)
                || !Objects.equals(normalizeEmail(target.getEmail()), normalizedEmail)
                || !Objects.equals(safeText(target.getTelephone()), cleanedTelephone);

        String riskLevel = normalizeAiRiskLevel(analysis == null ? null : analysis.getRiskLevel());
        boolean passwordNeedsStrengthening = true;
        String generatedPassword = passwordNeedsStrengthening ? generateStrongPassword() : null;

        Connection db = requireConnection();
        boolean previousAutoCommit;
        try {
            previousAutoCommit = db.getAutoCommit();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to start secure account update.", ex);
        }

        boolean profileUpdated = false;
        boolean passwordUpdated = false;
        boolean emailSent = false;

        try {
            db.setAutoCommit(false);

            if (profileNeedsUpdate) {
                String profileSql = """
                    UPDATE users
                    SET nom = ?, prenom = ?, email = ?, telephone = ?
                    WHERE idUser = ? AND role = ?
                    """;
                try (PreparedStatement preparedStatement = db.prepareStatement(profileSql)) {
                    preparedStatement.setString(1, cleanedNom);
                    preparedStatement.setString(2, cleanedPrenom);
                    preparedStatement.setString(3, normalizedEmail);
                    preparedStatement.setString(4, cleanedTelephone);
                    preparedStatement.setInt(5, idUser);
                    preparedStatement.setString(6, ROLE_USER);
                    profileUpdated = preparedStatement.executeUpdate() > 0;
                }
                if (!profileUpdated) {
                    throw new IllegalStateException("Unable to secure account profile fields.");
                }
            }

            if (passwordNeedsStrengthening) {
                String passwordSql = "UPDATE users SET password = ? WHERE idUser = ? AND role = ?";
                String passwordHash = PasswordUtils.hashPassword(generatedPassword);
                try (PreparedStatement preparedStatement = db.prepareStatement(passwordSql)) {
                    preparedStatement.setString(1, passwordHash);
                    preparedStatement.setInt(2, idUser);
                    preparedStatement.setString(3, ROLE_USER);
                    passwordUpdated = preparedStatement.executeUpdate() > 0;
                }
                if (!passwordUpdated) {
                    throw new IllegalStateException("Unable to strengthen account password.");
                }
            }

            String firstName = cleanedPrenom.isBlank() ? cleanedNom : cleanedPrenom;
            sendPlainEmail(
                normalizedEmail,
                EmailTemplates.accountSecuredSubject(),
                EmailTemplates.accountSecuredBody(
                    firstName,
                    profileUpdated,
                    passwordUpdated,
                    generatedPassword,
                    riskLevel,
                    collectAiSecurityHighlights(analysis)
                )
            );
            emailSent = true;

            logUserAction(
                idUser,
                "AI_ACCOUNT_SECURED",
                resolveClientContext(),
                buildAiSecurityActionDetails(profileUpdated, passwordUpdated, riskLevel)
            );

            db.commit();
            return new AiSecurityUpdateResult(profileUpdated, passwordUpdated, emailSent, generatedPassword, riskLevel);
        } catch (Exception ex) {
            try {
                db.rollback();
            } catch (SQLException rollbackEx) {
                ex.addSuppressed(rollbackEx);
            }
            throw ex instanceof RuntimeException
                ? (RuntimeException) ex
                : new RuntimeException("Failed to secure account with AI.", ex);
        } finally {
            try {
                db.setAutoCommit(previousAutoCommit);
            } catch (SQLException ignored) {
            }
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
            boolean updated = preparedStatement.executeUpdate() > 0;
            if (updated) {
                logUserAction(
                    idUser,
                    "BIOMETRIC_PREF",
                    resolveClientContext(),
                    enabled ? "Biometric login enabled." : "Biometric login disabled."
                );
            }
            return updated;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update biometric preference.", ex);
        }
    }

    public boolean updateUserOwnProfileImage(int idUser, String profileImagePath) {
        User target = requireRegularUser(idUser);
        if (!ROLE_USER.equalsIgnoreCase(target.getRole())) {
            throw new IllegalStateException("Only regular users can update this profile image.");
        }

        String sql = "UPDATE users SET profile_image_path = ? WHERE idUser = ? AND role = ?";
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            String cleanedPath = safeText(profileImagePath);
            preparedStatement.setString(1, cleanedPath.isBlank() ? null : cleanedPath);
            preparedStatement.setInt(2, idUser);
            preparedStatement.setString(3, ROLE_USER);
            boolean updated = preparedStatement.executeUpdate() > 0;
            if (updated) {
                logUserAction(idUser, "PROFILE_IMAGE_UPDATE", resolveClientContext(), "Profile image updated.");
            }
            return updated;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to update profile image.", ex);
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
                runNotificationSafely(() -> notificationService.notifyUserAccountStatusChanged(target.getIdUser(), STATUS_BANNED));
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
                runNotificationSafely(() -> notificationService.notifyUserAccountStatusChanged(target.getIdUser(), STATUS_ACTIVE));
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
            if (updated) {
                runNotificationSafely(() -> notificationService.notifyUserPasswordChangedByAdmin(target.getIdUser()));
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
                account_opened_location VARCHAR(200) NOT NULL DEFAULT 'Unknown location',
                account_opened_lat DECIMAL(10,7) NULL,
                account_opened_lng DECIMAL(10,7) NULL,
                last_online_at TIMESTAMP NULL DEFAULT NULL,
                last_online_from VARCHAR(180) NULL,
                profile_image_path VARCHAR(600) NULL,
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
            ensureColumnExists(db, statement, "account_opened_location",
                "ALTER TABLE users ADD COLUMN account_opened_location VARCHAR(200) NOT NULL DEFAULT 'Unknown location'");
            ensureColumnExists(db, statement, "account_opened_lat",
                "ALTER TABLE users ADD COLUMN account_opened_lat DECIMAL(10,7) NULL");
            ensureColumnExists(db, statement, "account_opened_lng",
                "ALTER TABLE users ADD COLUMN account_opened_lng DECIMAL(10,7) NULL");
            ensureColumnExists(db, statement, "last_online_at",
                "ALTER TABLE users ADD COLUMN last_online_at TIMESTAMP NULL DEFAULT NULL");
            ensureColumnExists(db, statement, "last_online_from",
                "ALTER TABLE users ADD COLUMN last_online_from VARCHAR(180) NULL");
            ensureColumnExists(db, statement, "profile_image_path",
                "ALTER TABLE users ADD COLUMN profile_image_path VARCHAR(600) NULL");
            ensureColumnExists(db, statement, "biometric_enabled",
                "ALTER TABLE users ADD COLUMN biometric_enabled TINYINT(1) NOT NULL DEFAULT 0");

            statement.executeUpdate(
                "UPDATE users SET account_opened_from = 'Unknown device' WHERE account_opened_from IS NULL OR TRIM(account_opened_from) = ''"
            );
            statement.executeUpdate(
                "UPDATE users SET account_opened_location = 'Unknown location' WHERE account_opened_location IS NULL OR TRIM(account_opened_location) = ''"
            );
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to ensure users profile/security columns.", ex);
        }
    }

    private void ensureUserActivityTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS user_activity_log (
                idAction INT NOT NULL AUTO_INCREMENT,
                idUser INT NOT NULL,
                action_type VARCHAR(50) NOT NULL,
                action_source VARCHAR(180) NULL,
                details VARCHAR(255) NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (idAction),
                KEY idx_user_activity_user (idUser),
                KEY idx_user_activity_created (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
            """;

        Connection db = requireConnection();
        try (Statement statement = db.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to ensure user activity log table.", ex);
        }
    }

    private void logUserAction(int idUser, String actionType, String actionSource, String details) {
        if (idUser <= 0) {
            return;
        }

        String sql = """
            INSERT INTO user_activity_log (idUser, action_type, action_source, details)
            VALUES (?, ?, ?, ?)
            """;
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            preparedStatement.setInt(1, idUser);
            preparedStatement.setString(2, safeText(actionType).isBlank() ? "ACTION" : safeText(actionType).toUpperCase());
            preparedStatement.setString(3, safeText(actionSource).isBlank() ? null : safeText(actionSource));
            preparedStatement.setString(4, safeText(details).isBlank() ? null : safeText(details));
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Failed to log user action: " + ex.getMessage());
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
            INSERT INTO users (
                nom, prenom, email, telephone, role, status, password,
                account_opened_from, account_opened_location, account_opened_lat, account_opened_lng, biometric_enabled
            )
            SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
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
            preparedStatement.setString(9, "Unknown location");
            setNullableDouble(preparedStatement, 10, null);
            setNullableDouble(preparedStatement, 11, null);
            preparedStatement.setBoolean(12, false);
            preparedStatement.setString(13, DEFAULT_ADMIN_EMAIL);
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
        user.setAccountOpenedLocation(resultSet.getString("account_opened_location"));
        user.setAccountOpenedLatitude(readNullableDouble(resultSet, "account_opened_lat"));
        user.setAccountOpenedLongitude(readNullableDouble(resultSet, "account_opened_lng"));
        user.setLastOnlineAt(lastOnlineAt == null ? "" : lastOnlineAt.toLocalDateTime().toString());
        user.setLastOnlineFrom(resultSet.getString("last_online_from"));
        user.setProfileImagePath(resultSet.getString("profile_image_path"));
        user.setBiometricEnabled(resultSet.getBoolean("biometric_enabled"));
        return user;
    }

    private UserActionLog mapUserAction(ResultSet resultSet) throws SQLException {
        UserActionLog action = new UserActionLog();
        action.setIdAction(resultSet.getInt("idAction"));
        action.setIdUser(resultSet.getInt("idUser"));
        action.setActionType(resultSet.getString("action_type"));
        action.setActionSource(resultSet.getString("action_source"));
        action.setDetails(resultSet.getString("details"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        action.setCreatedAt(createdAt == null ? "" : createdAt.toLocalDateTime().toString());
        return action;
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

    private String normalizeAiRiskLevel(String riskLevel) {
        String normalized = riskLevel == null ? "" : riskLevel.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LOW", "FAIBLE" -> "LOW";
            case "HIGH", "ELEVE", "HAUT" -> "HIGH";
            case "CRITICAL", "CRITIQUE" -> "CRITICAL";
            default -> "MEDIUM";
        };
    }

    private boolean shouldStrengthenPasswordFromAi(FormattedAnalysis analysis, String riskLevel) {
        if ("HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel)) {
            return true;
        }

        if (analysis == null) {
            return false;
        }

        return containsPasswordHardeningHint(analysis.getSecurityAdvice())
            || containsPasswordHardeningHint(analysis.getRiskReasons())
            || containsPasswordHardeningHint(analysis.getSuspiciousItems());
    }

    private boolean containsPasswordHardeningHint(List<String> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }

        for (String item : items) {
            String text = safeText(item).toLowerCase(Locale.ROOT);
            if (text.contains("password") || text.contains("mot de passe")) {
                if (text.contains("weak")
                    || text.contains("faible")
                    || text.contains("reuse")
                    || text.contains("reused")
                    || text.contains("comprom")
                    || text.contains("breach")
                    || text.contains("change")
                    || text.contains("rotate")
                    || text.contains("renouvel")) {
                    return true;
                }
            }
        }
        return false;
    }

    private String generateStrongPassword() {
        StringBuilder passwordBuilder = new StringBuilder(AI_STRONG_PASSWORD_LENGTH);
        passwordBuilder.append(randomChar(PASSWORD_LOWER));
        passwordBuilder.append(randomChar(PASSWORD_UPPER));
        passwordBuilder.append(randomChar(PASSWORD_DIGITS));
        passwordBuilder.append(randomChar(PASSWORD_SYMBOLS));

        while (passwordBuilder.length() < AI_STRONG_PASSWORD_LENGTH) {
            passwordBuilder.append(randomChar(PASSWORD_ALL));
        }

        char[] chars = passwordBuilder.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    private char randomChar(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }

    private String buildAiSecurityActionDetails(boolean profileUpdated, boolean passwordUpdated, String riskLevel) {
        List<String> details = new ArrayList<>();
        details.add("AI risk level: " + (riskLevel == null || riskLevel.isBlank() ? "MEDIUM" : riskLevel));
        if (profileUpdated) {
            details.add("Profile fields updated");
        }
        if (passwordUpdated) {
            details.add("Password strengthened");
        }
        return String.join("; ", details);
    }

    private List<String> collectAiSecurityHighlights(FormattedAnalysis analysis) {
        List<String> highlights = new ArrayList<>();
        if (analysis == null) {
            return highlights;
        }

        appendHighlights(highlights, analysis.getSecurityAdvice());
        appendHighlights(highlights, analysis.getRiskReasons());
        appendHighlights(highlights, analysis.getSuspiciousItems());

        if (highlights.size() > 5) {
            return new ArrayList<>(highlights.subList(0, 5));
        }
        return highlights;
    }

    private void appendHighlights(List<String> target, List<String> source) {
        if (target == null || source == null) {
            return;
        }

        for (String item : source) {
            String cleaned = safeText(item);
            if (cleaned.isBlank()) {
                continue;
            }
            boolean exists = target.stream().anyMatch(existing -> existing.equalsIgnoreCase(cleaned));
            if (!exists) {
                target.add(cleaned);
            }
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeLocation(String value) {
        String cleaned = safeText(value);
        return cleaned.isBlank() ? "Unknown location" : cleaned;
    }

    private void setNullableDouble(PreparedStatement preparedStatement, int index, Double value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, Types.DECIMAL);
            return;
        }
        preparedStatement.setDouble(index, value);
    }

    private Double readNullableDouble(ResultSet resultSet, String column) throws SQLException {
        double value = resultSet.getDouble(column);
        return resultSet.wasNull() ? null : value;
    }

    private String buildAccountUpdateDetails(User before, User after, String updatedRole, String updatedStatus) {
        if (before == null || after == null) {
            return "Your account details were updated by admin.";
        }

        StringJoiner joiner = new StringJoiner("; ");

        if (!Objects.equals(safeText(before.getNom()), safeText(after.getNom()))
            || !Objects.equals(safeText(before.getPrenom()), safeText(after.getPrenom()))) {
            joiner.add("Name information was updated");
        }
        if (!Objects.equals(normalizeEmail(before.getEmail()), normalizeEmail(after.getEmail()))) {
            joiner.add("Email was updated");
        }
        if (!Objects.equals(safeText(before.getTelephone()), safeText(after.getTelephone()))) {
            joiner.add("Phone number was updated");
        }

        String previousRole = normalizeRole(before.getRole());
        if (!Objects.equals(previousRole, updatedRole)) {
            joiner.add("Role changed to " + updatedRole);
        }

        String previousStatus = normalizeStatus(before.getStatus());
        if (!Objects.equals(previousStatus, updatedStatus)) {
            joiner.add("Status changed from " + previousStatus + " to " + updatedStatus);
        }

        String details = joiner.toString();
        return details.isBlank()
            ? "Your account details were updated by admin."
            : details + ".";
    }

    private void runNotificationSafely(Runnable action) {
        if (notificationService == null || action == null) {
            return;
        }
        try {
            action.run();
        } catch (Exception ex) {
            System.err.println("Notification dispatch failed: " + ex.getMessage());
        }
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
                runNotificationSafely(() -> notificationService.notifyUserAccountStatusChanged(target.getIdUser(), newStatus));
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

    private void verifyPasswordResetOtpWithProtection(String normalizedEmail, String otpCode) {
        assertPasswordResetNotBlocked(normalizedEmail);

        OtpEntry otpEntry = PASSWORD_RESET_OTP_CACHE.get(normalizedEmail);
        String providedOtp = otpCode == null ? "" : otpCode.trim();
        boolean valid = otpEntry != null
            && !Instant.now().isAfter(otpEntry.expiresAt())
            && otpEntry.code().equals(providedOtp);

        if (!valid) {
            if (otpEntry == null || Instant.now().isAfter(otpEntry.expiresAt())) {
                PASSWORD_RESET_OTP_CACHE.remove(normalizedEmail);
            }
            registerPasswordResetFailure(normalizedEmail);
            return;
        }

        PASSWORD_RESET_OTP_CACHE.remove(normalizedEmail);
        PASSWORD_RESET_GUARD_CACHE.remove(normalizedEmail);
    }

    private void assertPasswordResetNotBlocked(String normalizedEmail) {
        PasswordResetGuard guard = PASSWORD_RESET_GUARD_CACHE.get(normalizedEmail);
        if (guard == null || guard.blockedUntil() == null) {
            return;
        }

        Instant now = Instant.now();
        Instant blockedUntil = guard.blockedUntil();
        if (!now.isBefore(blockedUntil)) {
            PASSWORD_RESET_GUARD_CACHE.remove(normalizedEmail);
            return;
        }

        throw buildPasswordResetBlockedException(blockedUntil, "Too many failed OTP attempts.");
    }

    private void registerPasswordResetFailure(String normalizedEmail) {
        Instant now = Instant.now();
        PasswordResetGuard current = PASSWORD_RESET_GUARD_CACHE.get(normalizedEmail);
        if (current != null && current.blockedUntil() != null && now.isBefore(current.blockedUntil())) {
            throw buildPasswordResetBlockedException(current.blockedUntil(), "Too many failed OTP attempts.");
        }

        int failedAttempts = current == null ? 1 : current.failedAttempts() + 1;
        if (failedAttempts >= PASSWORD_RESET_MAX_FAILED_ATTEMPTS) {
            Instant blockedUntil = now.plusSeconds(PASSWORD_RESET_BLOCK_SECONDS);
            PASSWORD_RESET_GUARD_CACHE.put(normalizedEmail, new PasswordResetGuard(0, blockedUntil));
            PASSWORD_RESET_OTP_CACHE.remove(normalizedEmail);
            throw buildPasswordResetBlockedException(blockedUntil, "Too many failed OTP attempts.");
        }

        PASSWORD_RESET_GUARD_CACHE.put(normalizedEmail, new PasswordResetGuard(failedAttempts, null));
        int remaining = PASSWORD_RESET_MAX_FAILED_ATTEMPTS - failedAttempts;
        throw new IllegalArgumentException("Invalid or expired OTP. Remaining attempts: " + remaining + ".");
    }

    private void assertPasswordResetVerified(String normalizedEmail) {
        Instant verifiedUntil = PASSWORD_RESET_VERIFIED_CACHE.get(normalizedEmail);
        if (verifiedUntil == null) {
            throw new IllegalStateException("Please verify OTP first.");
        }

        if (Instant.now().isAfter(verifiedUntil)) {
            PASSWORD_RESET_VERIFIED_CACHE.remove(normalizedEmail);
            throw new IllegalStateException("OTP verification expired. Please request a new OTP.");
        }
    }

    private IllegalStateException buildPasswordResetBlockedException(Instant blockedUntil, String prefix) {
        long secondsLeft = Math.max(1L, blockedUntil.getEpochSecond() - Instant.now().getEpochSecond());
        long minutesLeft = (secondsLeft + 59L) / 60L;
        return new IllegalStateException(prefix + " Try again in " + minutesLeft + " minute(s).");
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

    private record PasswordResetGuard(int failedAttempts, Instant blockedUntil) {
    }

    private Connection requireConnection() {
        if (connection == null) {
            throw new IllegalStateException("Database connection is unavailable.");
        }
        return connection;
    }
}
