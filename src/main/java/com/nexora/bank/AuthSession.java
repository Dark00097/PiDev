package com.nexora.bank;

import com.nexora.bank.Models.User;

public final class AuthSession {
    private static volatile User currentUser;

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
    }
}
