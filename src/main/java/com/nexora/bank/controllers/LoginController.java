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
import javafx.scene.layout.VBox;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class LoginController {
    private static final int FAILED_ATTEMPTS_BEFORE_MATH = 3;

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private VBox boxMathChallenge;
    @FXML private Label lblMathQuestion;
    @FXML private TextField txtMathAnswer;
    @FXML private Label lblMessage;

    private final UserService userService = new UserService();
    private final AdminSecuritySettingsStore adminSecuritySettingsStore = new AdminSecuritySettingsStore();
    private int failedAttempts;
    private Integer currentMathAnswer;

    @FXML
    private void initialize() {
        failedAttempts = 0;
        currentMathAnswer = null;
        if (boxMathChallenge != null) {
            boxMathChallenge.setVisible(false);
            boxMathChallenge.setManaged(false);
        }
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }

    @FXML
    private void handleLogin() {
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

        if (isMathChallengeRequired() && !verifyMathChallenge()) {
            showError("Too many failed attempts. Please solve the math challenge.");
            return;
        }

        try {
            Optional<User> authenticatedUser = userService.authenticate(email, password);
            if (authenticatedUser.isEmpty()) {
                registerFailedAttempt();
                return;
            }

            resetFailedAttemptProtection();
            User user = authenticatedUser.get();
            String role = user.getRole() == null ? "" : user.getRole().trim().toUpperCase();
            String status = user.getStatus() == null ? "" : user.getStatus().trim().toUpperCase();

            if ("ROLE_ADMIN".equals(role)) {
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
            SceneRouter.show("/fxml/UserDashboard.fxml", "NEXORA BANK - User Dashboard", 1200, 760, 980, 680);
        } catch (Exception ex) {
            showError("Login failed. Please try again.");
            ex.printStackTrace();
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

    @FXML
    private void openForgotPassword() {
        SceneRouter.show("/fxml/ForgotPassword.fxml", "NEXORA BANK - Reset Password", 1200, 760, 980, 680);
    }

    private void showError(String message) {
        lblMessage.setText(message);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void registerFailedAttempt() {
        failedAttempts++;
        if (failedAttempts < FAILED_ATTEMPTS_BEFORE_MATH) {
            showError("Invalid email or password.");
            return;
        }

        generateMathChallenge();
        if (failedAttempts == FAILED_ATTEMPTS_BEFORE_MATH) {
            showError("Invalid login. Next attempt requires solving the math challenge.");
            return;
        }
        showError("Invalid login. Solve the math challenge to continue.");
    }

    private boolean isMathChallengeRequired() {
        return failedAttempts >= FAILED_ATTEMPTS_BEFORE_MATH;
    }

    private boolean verifyMathChallenge() {
        if (!isMathChallengeRequired()) {
            return true;
        }

        if (currentMathAnswer == null) {
            generateMathChallenge();
            return false;
        }

        String answerText = txtMathAnswer == null ? "" : txtMathAnswer.getText().trim();
        if (answerText.isBlank()) {
            return false;
        }

        try {
            int providedAnswer = Integer.parseInt(answerText);
            if (providedAnswer == currentMathAnswer) {
                if (txtMathAnswer != null) {
                    txtMathAnswer.clear();
                }
                currentMathAnswer = null;
                return true;
            }
        } catch (NumberFormatException ignored) {
        }

        generateMathChallenge();
        if (txtMathAnswer != null) {
            txtMathAnswer.clear();
        }
        return false;
    }

    private void generateMathChallenge() {
        int left = ThreadLocalRandom.current().nextInt(1, 10);
        int right = ThreadLocalRandom.current().nextInt(1, 10);
        currentMathAnswer = left + right;
        if (lblMathQuestion != null) {
            lblMathQuestion.setText("Security check: " + left + " + " + right + " = ?");
        }
        if (boxMathChallenge != null) {
            boxMathChallenge.setVisible(true);
            boxMathChallenge.setManaged(true);
        }
    }

    private void resetFailedAttemptProtection() {
        failedAttempts = 0;
        currentMathAnswer = null;
        if (txtMathAnswer != null) {
            txtMathAnswer.clear();
        }
        if (boxMathChallenge != null) {
            boxMathChallenge.setVisible(false);
            boxMathChallenge.setManaged(false);
        }
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
