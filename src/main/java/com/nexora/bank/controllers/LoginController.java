package com.nexora.bank.controllers;

import com.myapp.config.AdminSecuritySettings;
import com.myapp.config.AdminSecuritySettingsStore;
import com.myapp.security.AuthResult;
import com.myapp.security.BiometricVerificationDialog;
import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.User;
import com.nexora.bank.SceneRouter;
import com.nexora.bank.Service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Optional;

public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMessage;

    private final UserService userService = new UserService();
    private final AdminSecuritySettingsStore adminSecuritySettingsStore = new AdminSecuritySettingsStore();

    @FXML
    private void initialize() {
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }

    @FXML
    private void handleLogin() {
        System.out.println("Login attempt for: " + txtEmail.getText());
        String email = txtEmail.getText() == null ? "" : txtEmail.getText().trim();
        String password = txtPassword.getText() == null ? "" : txtPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill email and password.");
            return;
        }

        if (!email.contains("@")) {
            showError("Please enter a valid email address.");
            return;
        }

        try {
            System.out.println("Authenticating user: " + email);
            Optional<User> authenticatedUser = userService.authenticate(email, password);
            if (authenticatedUser.isEmpty()) {
                showError("Invalid email or password.");
                return;
            }

            User user = authenticatedUser.get();
            System.out.println("User authenticated: " + user.getEmail() + " | Role: " + user.getRole() + " | Status: " + user.getStatus());
            String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase();
            String status = user.getStatus() == null ? "" : user.getStatus().trim().toUpperCase();

            if ("ROLE_ADMIN".equals(role)) {
                System.out.println("Admin login - loading MainView.fxml");
                if (!"ACTIVE".equals(status)) {
                    showError("Admin account is inactive.");
                    return;
                }

                AdminSecuritySettings securitySettings = adminSecuritySettingsStore.load();
                if (securitySettings.isRequireBiometricOnAdminLogin()) {
                    AuthResult authResult = BiometricVerificationDialog.promptAndVerify("NEXORA Bank admin verification");
                    if (authResult != AuthResult.VERIFIED) {
                        BiometricVerificationDialog.showResultDialog(authResult);
                        showError(getAdminBiometricErrorMessage(authResult));
                        return;
                    }
                }
                userService.markUserOnline(user.getIdUser());
                user = userService.findByIdPublic(user.getIdUser()).orElse(user);
                AuthSession.setCurrentUser(user);
                SceneRouter.show("/fxml/MainView.fxml", "NEXORA BANK - Systeme de Gestion Bancaire", 1400, 900, 1200, 800);
                return;
            }

            if ("PENDING".equals(status)) {
                showError("Your account is waiting for admin approval.");
                return;
            }

            if ("DECLINED".equals(status)) {
                showError("Your account request was declined by admin.");
                return;
            }

            if ("BANNED".equals(status)) {
                showError("Your account is banned. Contact support.");
                return;
            }

            if (!"ACTIVE".equals(status)) {
                showError("Your account is inactive. Contact admin.");
                return;
            }

            if (user.isBiometricEnabled()) {
                AuthResult authResult = BiometricVerificationDialog.promptAndVerify("NEXORA Bank user verification");
                if (authResult != AuthResult.VERIFIED) {
                    BiometricVerificationDialog.showResultDialog(authResult);
                    showError("Login denied: biometric verification failed or was cancelled.");
                    return;
                }
            }

            userService.markUserOnline(user.getIdUser());
            user = userService.findByIdPublic(user.getIdUser()).orElse(user);
            AuthSession.setCurrentUser(user);
            System.out.println("Regular user login - loading UserDashboard.fxml");
            SceneRouter.show("/fxml/UserDashboard.fxml", "NEXORA BANK - User Dashboard", 1200, 760, 980, 680);
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Login failed. Please try again. Error: " + ex.getMessage());
        }
    }

    @FXML
    private void openHome() {
        SceneRouter.show("/fxml/Home.fxml", "NEXORA BANK - Welcome", 1200, 760, 980, 680);
    }

    @FXML
    private void openSignup() {
        SceneRouter.show("/fxml/Signup.fxml", "NEXORA BANK - Sign Up", 1200, 760, 980, 680);
    }

    private void showError(String message) {
        lblMessage.setText(message);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private String getAdminBiometricErrorMessage(AuthResult result) {
        return switch (result) {
            case FAILED -> "Admin login denied: biometric verification failed or was cancelled.";
            case NOT_AVAILABLE -> "Admin login denied: biometrics are not configured on this device.";
            case ERROR -> "Admin login denied: biometric verification helper is missing or failed.";
            case VERIFIED -> "";
        };
    }
}
