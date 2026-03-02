package com.nexora.bank.Service;

import com.nexora.bank.Models.Notification;
import com.nexora.bank.Models.User;
import com.nexora.bank.Utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final int DEFAULT_FETCH_LIMIT = 15;

    private final Connection connection;

    public NotificationService() {
        this(MyDB.getInstance().getConn());
    }

    public NotificationService(Connection connection) {
        this.connection = connection;
        if (this.connection != null) {
            ensureNotificationsTable();
        }
    }

    public void notifyAdminsAboutPendingUser(User pendingUser) {
        if (pendingUser == null) {
            return;
        }

        String userName = ((safe(pendingUser.getPrenom()) + " " + safe(pendingUser.getNom())).trim());
        if (userName.isBlank()) {
            userName = safe(pendingUser.getEmail());
        }

        String title = "New user pending approval";
        String message = "User " + userName + " (" + safe(pendingUser.getEmail())
            + ") created an account and is waiting for admin review.";
        createAdminNotification("USER_SIGNUP_PENDING", title, message, pendingUser.getIdUser());
    }

    public void notifyUserAccountStatusChanged(int userId, String newStatus) {
        String normalizedStatus = safe(newStatus).toUpperCase();
        String title;
        String message;

        switch (normalizedStatus) {
            case "ACTIVE" -> {
                title = "Account approved";
                message = "Your account is now ACTIVE and ready to use.";
            }
            case "DECLINED" -> {
                title = "Account declined";
                message = "Your account request was declined by admin.";
            }
            case "INACTIVE" -> {
                title = "Account set inactive";
                message = "Your account status was changed to INACTIVE by admin.";
            }
            case "BANNED" -> {
                title = "Account banned";
                message = "Your account was banned by admin. Contact support for details.";
            }
            default -> {
                title = "Account status updated";
                message = "Your account status was updated by admin to " + normalizedStatus + ".";
            }
        }

        createUserNotification(userId, "ACCOUNT_STATUS", title, message, userId);
    }

    public void notifyUserAccountUpdatedByAdmin(int userId, String details) {
        if (userId <= 0) {
            return;
        }
        String message = safe(details).isBlank()
            ? "Your account details were updated by admin."
            : details;
        createUserNotification(userId, "ACCOUNT_UPDATED", "Account updated by admin", message, userId);
    }

    public void notifyUserPasswordChangedByAdmin(int userId) {
        if (userId <= 0) {
            return;
        }
        createUserNotification(
            userId,
            "PASSWORD_CHANGED",
            "Password changed by admin",
            "Your account password was reset by admin. Please use your new password to sign in.",
            userId
        );
    }

    public List<Notification> getRecentNotificationsFor(User user) {
        return getRecentNotificationsFor(user, DEFAULT_FETCH_LIMIT);
    }

    public List<Notification> getRecentNotificationsFor(User user, int limit) {
        if (user == null || user.getIdUser() <= 0) {
            return List.of();
        }

        int fetchLimit = limit <= 0 ? DEFAULT_FETCH_LIMIT : Math.min(limit, 50);
        boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(safe(user.getRole()));
        String sql = isAdmin
            ? """
                SELECT idNotification, recipient_user_id, recipient_role, related_user_id, type, title, message, is_read, created_at
                FROM notifications
                WHERE recipient_user_id = ? OR recipient_role = ?
                ORDER BY created_at DESC, idNotification DESC
                LIMIT ?
                """
            : """
                SELECT idNotification, recipient_user_id, recipient_role, related_user_id, type, title, message, is_read, created_at
                FROM notifications
                WHERE recipient_user_id = ?
                ORDER BY created_at DESC, idNotification DESC
                LIMIT ?
                """;

        List<Notification> notifications = new ArrayList<>();
        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            if (isAdmin) {
                preparedStatement.setInt(1, user.getIdUser());
                preparedStatement.setString(2, ROLE_ADMIN);
                preparedStatement.setInt(3, fetchLimit);
            } else {
                preparedStatement.setInt(1, user.getIdUser());
                preparedStatement.setInt(2, fetchLimit);
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    notifications.add(mapNotification(resultSet));
                }
            }
            return notifications;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch notifications.", ex);
        }
    }

    public int countUnreadFor(User user) {
        if (user == null || user.getIdUser() <= 0) {
            return 0;
        }

        boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(safe(user.getRole()));
        String sql = isAdmin
            ? "SELECT COUNT(*) FROM notifications WHERE is_read = 0 AND (recipient_user_id = ? OR recipient_role = ?)"
            : "SELECT COUNT(*) FROM notifications WHERE is_read = 0 AND recipient_user_id = ?";

        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            if (isAdmin) {
                preparedStatement.setInt(1, user.getIdUser());
                preparedStatement.setString(2, ROLE_ADMIN);
            } else {
                preparedStatement.setInt(1, user.getIdUser());
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return 0;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to count unread notifications.", ex);
        }
    }

    public int markAllAsRead(User user) {
        if (user == null || user.getIdUser() <= 0) {
            return 0;
        }

        boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(safe(user.getRole()));
        String sql = isAdmin
            ? "UPDATE notifications SET is_read = 1 WHERE is_read = 0 AND (recipient_user_id = ? OR recipient_role = ?)"
            : "UPDATE notifications SET is_read = 1 WHERE is_read = 0 AND recipient_user_id = ?";

        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            if (isAdmin) {
                preparedStatement.setInt(1, user.getIdUser());
                preparedStatement.setString(2, ROLE_ADMIN);
            } else {
                preparedStatement.setInt(1, user.getIdUser());
            }
            return preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to mark notifications as read.", ex);
        }
    }

    public void createAdminNotification(String type, String title, String message, Integer relatedUserId) {
        createNotification(null, ROLE_ADMIN, relatedUserId, type, title, message);
    }

    public void createUserNotification(int recipientUserId, String type, String title, String message, Integer relatedUserId) {
        if (recipientUserId <= 0) {
            return;
        }
        createNotification(recipientUserId, null, relatedUserId, type, title, message);
    }

    private void createNotification(
        Integer recipientUserId,
        String recipientRole,
        Integer relatedUserId,
        String type,
        String title,
        String message
    ) {
        String sql = """
            INSERT INTO notifications (recipient_user_id, recipient_role, related_user_id, type, title, message, is_read)
            VALUES (?, ?, ?, ?, ?, ?, 0)
            """;

        Connection db = requireConnection();
        try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
            if (recipientUserId == null) {
                preparedStatement.setNull(1, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(1, recipientUserId);
            }
            preparedStatement.setString(2, safe(recipientRole).isBlank() ? null : safe(recipientRole).toUpperCase());
            if (relatedUserId == null) {
                preparedStatement.setNull(3, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(3, relatedUserId);
            }
            preparedStatement.setString(4, safe(type).isBlank() ? "INFO" : safe(type).toUpperCase());
            preparedStatement.setString(5, safe(title).isBlank() ? "Notification" : safe(title));
            preparedStatement.setString(6, safe(message).isBlank() ? "You have a new notification." : safe(message));
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to create notification.", ex);
        }
    }

    private Notification mapNotification(ResultSet resultSet) throws SQLException {
        Notification notification = new Notification();
        notification.setIdNotification(resultSet.getInt("idNotification"));

        int recipientUserId = resultSet.getInt("recipient_user_id");
        notification.setRecipientUserId(resultSet.wasNull() ? null : recipientUserId);

        int relatedUserId = resultSet.getInt("related_user_id");
        notification.setRelatedUserId(resultSet.wasNull() ? null : relatedUserId);

        notification.setRecipientRole(resultSet.getString("recipient_role"));
        notification.setType(resultSet.getString("type"));
        notification.setTitle(resultSet.getString("title"));
        notification.setMessage(resultSet.getString("message"));
        notification.setRead(resultSet.getBoolean("is_read"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        notification.setCreatedAt(createdAt == null ? "" : createdAt.toLocalDateTime().toString());
        return notification;
    }

    private void ensureNotificationsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS notifications (
                idNotification INT NOT NULL AUTO_INCREMENT,
                recipient_user_id INT NULL,
                recipient_role VARCHAR(20) NULL,
                related_user_id INT NULL,
                type VARCHAR(40) NOT NULL DEFAULT 'INFO',
                title VARCHAR(160) NOT NULL,
                message TEXT NOT NULL,
                is_read TINYINT(1) NOT NULL DEFAULT 0,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (idNotification),
                KEY idx_notifications_recipient_user (recipient_user_id),
                KEY idx_notifications_recipient_role (recipient_role),
                KEY idx_notifications_created (created_at),
                KEY idx_notifications_read (is_read)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
            """;

        Connection db = requireConnection();
        try (Statement statement = db.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to ensure notifications table.", ex);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private Connection requireConnection() {
        if (connection == null) {
            throw new IllegalStateException("Database connection is unavailable.");
        }
        return connection;
    }
}
