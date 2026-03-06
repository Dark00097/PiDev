package com.nexora.bank.controllers;

import com.myapp.security.AuthResult;
import com.myapp.security.BiometricVerificationDialog;
import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.User;
import com.nexora.bank.Service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Optional;
import java.util.regex.Pattern;

public class UserDashboardProfileSectionController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern STRONG_PASSWORD_PATTERN =
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

    @FXML private Label lblProfileName;
    @FXML private Label lblProfileEmail;
    @FXML private Label lblAccountOpenedFrom;
    @FXML private Label lblLastOnlineAt;
    @FXML private Label lblLastOnlineFrom;

    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelephone;

    @FXML private CheckBox chkEnableBiometric;

    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;

    @FXML private TextField txtResetOtp;
    @FXML private PasswordField txtResetNewPassword;
    @FXML private PasswordField txtResetConfirmPassword;

    @FXML private Label lblProfileStatus;
    @FXML private Label lblSecurityStatus;
    @FXML private Label lblPasswordStatus;
    @FXML private Label lblResetStatus;

    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        refreshProfile();
    }

    public void refreshProfile() {
        User user = requireCurrentUser();
        if (user == null) {
            return;
        }

        txtNom.setText(safe(user.getNom()));
        txtPrenom.setText(safe(user.getPrenom()));
        txtEmail.setText(safe(user.getEmail()));
        txtTelephone.setText(safe(user.getTelephone()));

        lblProfileName.setText((safe(user.getPrenom()) + " " + safe(user.getNom())).trim());
        lblProfileEmail.setText(safe(user.getEmail()));
        lblAccountOpenedFrom.setText(isBlank(user.getAccountOpenedFrom()) ? "Unknown device" : user.getAccountOpenedFrom());
        lblLastOnlineAt.setText(formatDateTime(user.getLastOnlineAt()));
        lblLastOnlineFrom.setText(isBlank(user.getLastOnlineFrom()) ? "No session recorded yet" : user.getLastOnlineFrom());
        chkEnableBiometric.setSelected(user.isBiometricEnabled());
    }

    @FXML
    private void handleReloadProfile() {
        refreshProfile();
        setStatus(lblProfileStatus, "Profile refreshed.", true);
    }

    @FXML
    private void handleSaveProfile() {
        User user = requireCurrentUser();
        if (user == null) {
            return;
        }

        String nom = safe(txtNom.getText());
        String prenom = safe(txtPrenom.getText());
        String email = safe(txtEmail.getText());
        String telephone = safe(txtTelephone.getText());

        if (nom.isBlank() || prenom.isBlank() || email.isBlank() || telephone.isBlank()) {
            setStatus(lblProfileStatus, "All profile fields are required.", false);
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            setStatus(lblProfileStatus, "Invalid email format.", false);
            return;
        }

        try {
            boolean updated = userService.updateUserOwnProfile(user.getIdUser(), nom, prenom, email, telephone);
            if (!updated) {
                setStatus(lblProfileStatus, "No changes saved.", false);
                return;
            }

            Optional<User> refreshed = userService.findByIdPublic(user.getIdUser());
            refreshed.ifPresent(AuthSession::setCurrentUser);
            refreshProfile();
            setStatus(lblProfileStatus, "Profile updated successfully.", true);
        } catch (Exception ex) {
            setStatus(lblProfileStatus, ex.getMessage() == null ? "Failed to update profile." : ex.getMessage(), false);
        }
    }

    @FXML
    private void handleToggleBiometric() {
        User user = requireCurrentUser();
        if (user == null) {
            chkEnableBiometric.setSelected(false);
            return;
        }

        boolean enable = chkEnableBiometric.isSelected();
        if (enable) {
            AuthResult result = BiometricVerificationDialog.promptAndVerify("NEXORA Bank user biometric setup verification");
            if (result != AuthResult.VERIFIED) {
                BiometricVerificationDialog.showResultDialog(result);
                chkEnableBiometric.setSelected(false);
                setStatus(lblSecurityStatus, "Biometric activation cancelled.", false);
                return;
            }
        }

        try {
            boolean updated = userService.updateUserBiometricPreference(user.getIdUser(), enable);
            if (!updated) {
                chkEnableBiometric.setSelected(!enable);
                setStatus(lblSecurityStatus, "Unable to save biometric preference.", false);
                return;
            }

            Optional<User> refreshed = userService.findByIdPublic(user.getIdUser());
            refreshed.ifPresent(AuthSession::setCurrentUser);
            setStatus(lblSecurityStatus, enable ? "Biometric login enabled." : "Biometric login disabled.", true);
        } catch (Exception ex) {
            chkEnableBiometric.setSelected(!enable);
            setStatus(lblSecurityStatus, ex.getMessage() == null ? "Failed to update biometric preference." : ex.getMessage(), false);
        }
    }

    @FXML
    private void handleChangePassword() {
        User user = requireCurrentUser();
        if (user == null) {
            return;
        }

        String currentPassword = txtCurrentPassword.getText() == null ? "" : txtCurrentPassword.getText();
        String newPassword = txtNewPassword.getText() == null ? "" : txtNewPassword.getText();
        String confirmPassword = txtConfirmPassword.getText() == null ? "" : txtConfirmPassword.getText();

        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            setStatus(lblPasswordStatus, "Fill current, new and confirm password fields.", false);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            setStatus(lblPasswordStatus, "New password and confirmation do not match.", false);
            return;
        }
        if (!STRONG_PASSWORD_PATTERN.matcher(newPassword).matches()) {
            setStatus(lblPasswordStatus, "Password must include uppercase, lowercase, number, special char, and be 8+ chars.", false);
            return;
        }

        try {
            boolean updated = userService.updateUserOwnPassword(user.getIdUser(), currentPassword, newPassword);
            if (!updated) {
                setStatus(lblPasswordStatus, "Password update failed.", false);
                return;
            }

            txtCurrentPassword.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();
            setStatus(lblPasswordStatus, "Password changed successfully.", true);
        } catch (Exception ex) {
            setStatus(lblPasswordStatus, ex.getMessage() == null ? "Failed to update password." : ex.getMessage(), false);
        }
    }

    @FXML
    private void handleSendResetOtp() {
        User user = requireCurrentUser();
        if (user == null) {
            return;
        }
        try {
            userService.sendPasswordResetOtpForUser(user.getIdUser());
            setStatus(lblResetStatus, "OTP sent to your email. It expires in 5 minutes.", true);
        } catch (Exception ex) {
            setStatus(lblResetStatus, ex.getMessage() == null ? "Failed to send OTP." : ex.getMessage(), false);
        }
    }

    @FXML
    private void handleResetPasswordWithOtp() {
        User user = requireCurrentUser();
        if (user == null) {
            return;
        }

        String otp = safe(txtResetOtp.getText());
        String newPassword = txtResetNewPassword.getText() == null ? "" : txtResetNewPassword.getText();
        String confirmPassword = txtResetConfirmPassword.getText() == null ? "" : txtResetConfirmPassword.getText();

        if (otp.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            setStatus(lblResetStatus, "Enter OTP, new password and confirmation.", false);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            setStatus(lblResetStatus, "New password and confirmation do not match.", false);
            return;
        }
        if (!STRONG_PASSWORD_PATTERN.matcher(newPassword).matches()) {
            setStatus(lblResetStatus, "Password must include uppercase, lowercase, number, special char, and be 8+ chars.", false);
            return;
        }

        try {
            boolean updated = userService.resetOwnPasswordWithOtp(user.getIdUser(), otp, newPassword);
            if (!updated) {
                setStatus(lblResetStatus, "Password reset failed.", false);
                return;
            }

            txtResetOtp.clear();
            txtResetNewPassword.clear();
            txtResetConfirmPassword.clear();
            setStatus(lblResetStatus, "Password reset successful.", true);
        } catch (Exception ex) {
            setStatus(lblResetStatus, ex.getMessage() == null ? "Failed to reset password." : ex.getMessage(), false);
        }
    }

    private User requireCurrentUser() {
        User current = AuthSession.getCurrentUser();
        if (current == null || current.getIdUser() <= 0) {
            setStatus(lblProfileStatus, "Session expired. Please login again.", false);
            return null;
        }

        Optional<User> refreshed = userService.findByIdPublic(current.getIdUser());
        if (refreshed.isEmpty()) {
            setStatus(lblProfileStatus, "User account not found. Please login again.", false);
            return null;
        }

        User user = refreshed.get();
        if (!"ROLE_USER".equalsIgnoreCase(safe(user.getRole()))) {
            setStatus(lblProfileStatus, "This profile page is only available for user accounts.", false);
            return null;
        }

        AuthSession.setCurrentUser(user);
        return user;
    }

    private void setStatus(Label label, String message, boolean success) {
        if (label == null) {
            return;
        }
        label.setText(message);
        label.setStyle(success
            ? "-fx-text-fill: #0f766e; -fx-font-size: 12px; -fx-font-weight: 600;"
            : "-fx-text-fill: #b91c1c; -fx-font-size: 12px; -fx-font-weight: 600;");
    }

    private String formatDateTime(String value) {
        if (isBlank(value)) {
            return "No activity yet";
        }
        return value.replace('T', ' ');
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }
}
