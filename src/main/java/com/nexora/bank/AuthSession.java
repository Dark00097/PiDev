package com.nexora.bank;

import com.nexora.bank.Models.User;

public final class AuthSession {
    private static volatile User currentUser;
    private static volatile Integer pendingUserManagementTargetId;

    private AuthSession() {
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
        pendingUserManagementTargetId = null;
    }

    public static void setPendingUserManagementTargetId(Integer userId) {
        pendingUserManagementTargetId = userId;
    }

    public static Integer consumePendingUserManagementTargetId() {
        Integer value = pendingUserManagementTargetId;
        pendingUserManagementTargetId = null;
        return value;
    }
}
