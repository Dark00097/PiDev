package com.nexora.bank.controllers;

import com.myapp.config.AdminSecuritySettings;
import com.myapp.config.AdminSecuritySettingsStore;
import com.myapp.security.AuthResult;
import com.myapp.security.BiometricVerificationDialog;
import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.User;
import com.nexora.bank.Service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

public class AdminSecuritySettingsController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern STRONG_PASSWORD_PATTERN =
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

    private final AdminSecuritySettingsStore settingsStore = new AdminSecuritySettingsStore();
    private final UserService userService = new UserService();

    @FXML private CheckBox chkRequireBiometricOnAdminLogin;
    @FXML private CheckBox chkRequireBiometricOnSensitiveActions;
    @FXML private CheckBox chkEnableEmailOtp;
    @FXML private Label lblStatus;

    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelephone;
    @FXML private Label lblRole;
    @FXML private Label lblAccountStatus;

    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmNewPassword;

    @FXML
    private void initialize() {
        loadSettings();
        loadCurrentAdminProfile();
    }

    @FXML
    private void handleTestBiometricVerification() {
        AuthResult result = BiometricVerificationDialog.promptAndVerify("NEXORA Bank admin verification");
        BiometricVerificationDialog.showResultDialog(result);
        lblStatus.setText("Last biometric test result: " + result);
    }

    @FXML
    private void handleOpenBiometricSettings() {
        try {
            BiometricVerificationDialog.openDeviceBiometricSettings();
            lblStatus.setText("Opened biometric settings. Enable face or fingerprint there.");
        } catch (IOException ex) {
            showError("Failed to open biometric settings.");
        }
    }

    @FXML
    private void handleSaveSettings() {
        try {
            AdminSecuritySettings settings = new AdminSecuritySettings();
            settings.setRequireBiometricOnAdminLogin(chkRequireBiometricOnAdminLogin.isSelected());
            settings.setRequireBiometricOnSensitiveActions(chkRequireBiometricOnSensitiveActions.isSelected());
            settings.setEnableEmailOtp(chkEnableEmailOtp.isSelected());

            settingsStore.save(settings);
            lblStatus.setText("Security settings saved to: " + settingsStore.getSettingsFilePath());
            showInfo("Admin account management", "Security settings saved successfully.");
        } catch (Exception ex) {
            showError("Failed to save admin security settings.");
        }
    }

    @FXML
    private void handleReloadSettings() {
        loadSettings();
        loadCurrentAdminProfile();
    }

    @FXML
    private void handleSaveProfile() {
        User admin = requireCurrentAdmin();
        if (admin == null) {
            return;
        }

        String nom = safe(txtNom.getText());
        String prenom = safe(txtPrenom.getText());
        String email = safe(txtEmail.getText());
        String telephone = safe(txtTelephone.getText());

        if (nom.isBlank() || prenom.isBlank() || email.isBlank() || telephone.isBlank()) {
            showError("Please fill all profile fields.");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Invalid email format.");
            return;
        }

        try {
            boolean updated = userService.updateAdminOwnProfile(admin.getIdUser(), nom, prenom, email, telephone);
            if (!updated) {
                showError("Profile update failed.");
                return;
            }

            Optional<User> refreshed = userService.findByIdPublic(admin.getIdUser());
            refreshed.ifPresent(AuthSession::setCurrentUser);
            loadCurrentAdminProfile();
            lblStatus.setText("Admin profile updated successfully.");
            showInfo("Admin account management", "Profile updated successfully.");
        } catch (Exception ex) {
            showError(ex.getMessage() == null ? "Failed to update profile." : ex.getMessage());
        }
    }

    @FXML
    private void handleChangePassword() {
        User admin = requireCurrentAdmin();
        if (admin == null) {
            return;
        }

        String currentPassword = txtCurrentPassword.getText() == null ? "" : txtCurrentPassword.getText();
        String newPassword = txtNewPassword.getText() == null ? "" : txtNewPassword.getText();
        String confirmPassword = txtConfirmNewPassword.getText() == null ? "" : txtConfirmNewPassword.getText();

        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            showError("Please fill current password, new password, and confirmation.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showError("New password and confirmation do not match.");
            return;
        }
        if (!STRONG_PASSWORD_PATTERN.matcher(newPassword).matches()) {
            showError("New password must include uppercase, lowercase, number, special char, and 8+ length.");
            return;
        }

        AuthResult result = BiometricVerificationDialog.promptAndVerify("NEXORA Bank admin password change verification");
        if (result != AuthResult.VERIFIED) {
            BiometricVerificationDialog.showResultDialog(result);
            lblStatus.setText("Password change cancelled: biometric verification failed.");
            return;
        }

        try {
            boolean updated = userService.updateAdminOwnPassword(admin.getIdUser(), currentPassword, newPassword);
            if (!updated) {
                showError("Password update failed.");
                return;
            }

            txtCurrentPassword.clear();
            txtNewPassword.clear();
            txtConfirmNewPassword.clear();
            lblStatus.setText("Admin password updated successfully.");
            showInfo("Admin account management", "Password changed successfully.");
        } catch (Exception ex) {
            showError(ex.getMessage() == null ? "Failed to change password." : ex.getMessage());
        }
    }

    private void loadSettings() {
        try {
            AdminSecuritySettings settings = settingsStore.load();
            chkRequireBiometricOnAdminLogin.setSelected(settings.isRequireBiometricOnAdminLogin());
            chkRequireBiometricOnSensitiveActions.setSelected(settings.isRequireBiometricOnSensitiveActions());
            chkEnableEmailOtp.setSelected(settings.isEnableEmailOtp());
            lblStatus.setText("Loaded from: " + settingsStore.getSettingsFilePath());
        } catch (Exception ex) {
            lblStatus.setText("Failed to load settings.");
            showError("Failed to load admin security settings.");
        }
    }

    private void loadCurrentAdminProfile() {
        User admin = requireCurrentAdmin();
        if (admin == null) {
            return;
        }

        txtNom.setText(safe(admin.getNom()));
        txtPrenom.setText(safe(admin.getPrenom()));
        txtEmail.setText(safe(admin.getEmail()));
        txtTelephone.setText(safe(admin.getTelephone()));
        lblRole.setText(safe(admin.getRole()));
        lblAccountStatus.setText(safe(admin.getStatus()));
    }

    private User requireCurrentAdmin() {
        User current = AuthSession.getCurrentUser();
        if (current == null || current.getIdUser() <= 0) {
            showError("No active admin session found. Please login again.");
            return null;
        }

        Optional<User> refreshed = userService.findByIdPublic(current.getIdUser());
        if (refreshed.isEmpty()) {
            showError("Admin account not found. Please login again.");
            return null;
        }

        User admin = refreshed.get();
        if (!"ROLE_ADMIN".equalsIgnoreCase(safe(admin.getRole()))) {
            showError("Current session is not an admin session.");
            return null;
        }

        AuthSession.setCurrentUser(admin);
        return admin;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle("Admin account management");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
