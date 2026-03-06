package com.nexora.bank.controllers;

import com.myapp.config.AdminSecuritySettings;
import com.myapp.config.AdminSecuritySettingsStore;
import com.myapp.security.AuthResult;
import com.myapp.security.BiometricVerificationDialog;
import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.User;
import com.nexora.bank.Service.UserService;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

public class AdminSecuritySettingsController {
    
    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern STRONG_PASSWORD_PATTERN =
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

    // Services
    private final AdminSecuritySettingsStore settingsStore = new AdminSecuritySettingsStore();
    private final UserService userService = new UserService();

    // FXML injected fields - Security Settings
    @FXML private CheckBox chkRequireBiometricOnAdminLogin;
    @FXML private CheckBox chkRequireBiometricOnSensitiveActions;
    @FXML private CheckBox chkEnableEmailOtp;
    @FXML private Label lblStatus;

    // FXML injected fields - Profile
    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelephone;
    @FXML private Label lblRole;
    @FXML private Label lblAccountStatus;

    // FXML injected fields - Password
    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmNewPassword;

    @FXML
    private void initialize() {
        loadSettings();
        loadCurrentAdminProfile();
        
        // Apply entrance animations
        Platform.runLater(this::applyEntranceAnimations);
    }

    /**
     * Applies smooth entrance animations to cards
     */
    private void applyEntranceAnimations() {
        // Animate cards sequentially
        animateNode(lblStatus.getScene().lookup(".nx-card-profile"), 0);
        animateNode(lblStatus.getScene().lookup(".nx-card-security"), 100);
        animateNode(lblStatus.getScene().lookup(".nx-card-biometric"), 200);
        animateNode(lblStatus.getScene().lookup(".nx-card-actions"), 300);
    }

    private void animateNode(Node node, int delayMs) {
        if (node == null) return;
        
        node.setOpacity(0);
        node.setTranslateY(30);
        
        FadeTransition fade = new FadeTransition(Duration.millis(500), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(delayMs));
        
        TranslateTransition translate = new TranslateTransition(Duration.millis(500), node);
        translate.setFromY(30);
        translate.setToY(0);
        translate.setDelay(Duration.millis(delayMs));
        
        fade.play();
        translate.play();
    }

    @FXML
    private void handleTestBiometricVerification() {
        updateStatus("Test de vérification biométrique en cours...");
        
        AuthResult result = BiometricVerificationDialog.promptAndVerify("Vérification admin NEXORA Bank");
        BiometricVerificationDialog.showResultDialog(result);
        
        String statusMessage = switch (result) {
            case VERIFIED -> "✓ Test biométrique réussi";
            case FAILED -> "✗ Test biométrique échoué ou annulé";
            case NOT_AVAILABLE -> "⚠ Biométrie non disponible sur cet appareil";
            case ERROR -> "✗ Erreur système biométrique";
        };
        
        updateStatus(statusMessage);
    }

    @FXML
    private void handleOpenBiometricSettings() {
        try {
            BiometricVerificationDialog.openDeviceBiometricSettings();
            updateStatus("Paramètres système ouverts. Configurez Face ID ou Empreinte.");
        } catch (IOException ex) {
            showError("Impossible d'ouvrir les paramètres biométriques du système.");
            updateStatus("Erreur: Impossible d'ouvrir les paramètres");
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
            
            updateStatus("✓ Paramètres de sécurité enregistrés");
            showSuccess("Paramètres de Sécurité", "Vos paramètres de sécurité ont été enregistrés avec succès.");
        } catch (Exception ex) {
            showError("Échec de l'enregistrement des paramètres de sécurité.");
            updateStatus("✗ Erreur d'enregistrement des paramètres");
        }
    }

    @FXML
    private void handleReloadSettings() {
        loadSettings();
        loadCurrentAdminProfile();
        updateStatus("✓ Paramètres rechargés depuis le fichier");
    }

    @FXML
    private void handleSaveProfile() {
        User admin = requireCurrentAdmin();
        if (admin == null) return;

        String nom = safe(txtNom.getText());
        String prenom = safe(txtPrenom.getText());
        String email = safe(txtEmail.getText());
        String telephone = safe(txtTelephone.getText());

        // Validation
        if (nom.isBlank() || prenom.isBlank() || email.isBlank() || telephone.isBlank()) {
            showError("Veuillez remplir tous les champs du profil.");
            highlightEmptyProfileFields();
            return;
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Format d'email invalide.");
            return;
        }

        try {
            boolean updated = userService.updateAdminOwnProfile(admin.getIdUser(), nom, prenom, email, telephone);
            if (!updated) {
                showError("Échec de la mise à jour du profil.");
                return;
            }

            // Refresh session with updated user data
            Optional<User> refreshed = userService.findByIdPublic(admin.getIdUser());
            refreshed.ifPresent(AuthSession::setCurrentUser);
            
            loadCurrentAdminProfile();
            updateStatus("✓ Profil administrateur mis à jour");
            showSuccess("Profil Administrateur", "Votre profil a été mis à jour avec succès.");
        } catch (Exception ex) {
            showError(ex.getMessage() == null ? "Échec de la mise à jour du profil." : ex.getMessage());
            updateStatus("✗ Erreur de mise à jour du profil");
        }
    }

    private void highlightEmptyProfileFields() {
        if (txtNom.getText().isBlank()) addErrorStyle(txtNom);
        if (txtPrenom.getText().isBlank()) addErrorStyle(txtPrenom);
        if (txtEmail.getText().isBlank()) addErrorStyle(txtEmail);
        if (txtTelephone.getText().isBlank()) addErrorStyle(txtTelephone);
    }

    private void addErrorStyle(TextField field) {
        field.getParent().setStyle("-fx-border-color: #EF4444;");
        
        // Reset after delay
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> field.getParent().setStyle(""));
            } catch (InterruptedException ignored) {}
        }).start();
    }

    @FXML
    private void handleChangePassword() {
        User admin = requireCurrentAdmin();
        if (admin == null) return;

        String currentPassword = txtCurrentPassword.getText() == null ? "" : txtCurrentPassword.getText();
        String newPassword = txtNewPassword.getText() == null ? "" : txtNewPassword.getText();
        String confirmPassword = txtConfirmNewPassword.getText() == null ? "" : txtConfirmNewPassword.getText();

        // Validation
        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            showError("Veuillez remplir tous les champs de mot de passe.");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showError("Le nouveau mot de passe et sa confirmation ne correspondent pas.");
            return;
        }
        
        if (!STRONG_PASSWORD_PATTERN.matcher(newPassword).matches()) {
            showError("Le mot de passe doit contenir au moins 8 caractères avec majuscule, minuscule, chiffre et symbole.");
            return;
        }

        // Biometric verification required for password change
        updateStatus("Vérification biométrique requise...");
        
        AuthResult result = BiometricVerificationDialog.promptAndVerify(
            "Vérification de changement de mot de passe admin NEXORA Bank"
        );
        
        if (result != AuthResult.VERIFIED) {
            BiometricVerificationDialog.showResultDialog(result);
            updateStatus("✗ Changement de mot de passe annulé: vérification biométrique échouée");
            return;
        }

        try {
            boolean updated = userService.updateAdminOwnPassword(admin.getIdUser(), currentPassword, newPassword);
            if (!updated) {
                showError("Échec du changement de mot de passe. Vérifiez votre mot de passe actuel.");
                return;
            }

            // Clear password fields on success
            txtCurrentPassword.clear();
            txtNewPassword.clear();
            txtConfirmNewPassword.clear();
            
            updateStatus("✓ Mot de passe administrateur changé avec succès");
            showSuccess("Sécurité du Compte", "Votre mot de passe a été changé avec succès.");
        } catch (Exception ex) {
            showError(ex.getMessage() == null ? "Échec du changement de mot de passe." : ex.getMessage());
            updateStatus("✗ Erreur de changement de mot de passe");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    private void loadSettings() {
        try {
            AdminSecuritySettings settings = settingsStore.load();
            chkRequireBiometricOnAdminLogin.setSelected(settings.isRequireBiometricOnAdminLogin());
            chkRequireBiometricOnSensitiveActions.setSelected(settings.isRequireBiometricOnSensitiveActions());
            chkEnableEmailOtp.setSelected(settings.isEnableEmailOtp());
            updateStatus("Paramètres chargés depuis: " + settingsStore.getSettingsFilePath());
        } catch (Exception ex) {
            updateStatus("Erreur de chargement des paramètres");
            showError("Échec du chargement des paramètres de sécurité.");
        }
    }

    private void loadCurrentAdminProfile() {
        User admin = requireCurrentAdmin();
        if (admin == null) return;

        txtNom.setText(safe(admin.getNom()));
        txtPrenom.setText(safe(admin.getPrenom()));
        txtEmail.setText(safe(admin.getEmail()));
        txtTelephone.setText(safe(admin.getTelephone()));
        
        // Update badges
        updateRoleBadge(safe(admin.getRole()));
        updateStatusBadge(safe(admin.getStatus()));
    }

    private void updateRoleBadge(String role) {
        if (lblRole != null) {
            lblRole.setText(role.replace("ROLE_", ""));
        }
    }

    private void updateStatusBadge(String status) {
        if (lblAccountStatus != null) {
            lblAccountStatus.setText(status);
            
            // Update badge styling based on status
            lblAccountStatus.getStyleClass().removeAll(
                "nx-status-active", "nx-status-pending", "nx-status-banned"
            );
            
            switch (status.toUpperCase()) {
                case "ACTIVE" -> lblAccountStatus.getStyleClass().add("nx-status-active");
                case "PENDING" -> lblAccountStatus.getStyleClass().add("nx-status-pending");
                case "BANNED" -> lblAccountStatus.getStyleClass().add("nx-status-banned");
            }
        }
    }

    private void updateStatus(String message) {
        if (lblStatus != null) {
            lblStatus.setText(message);
        }
    }

    private User requireCurrentAdmin() {
        User current = AuthSession.getCurrentUser();
        if (current == null || current.getIdUser() <= 0) {
            showError("Aucune session administrateur active. Veuillez vous reconnecter.");
            return null;
        }

        Optional<User> refreshed = userService.findByIdPublic(current.getIdUser());
        if (refreshed.isEmpty()) {
            showError("Compte administrateur introuvable. Veuillez vous reconnecter.");
            return null;
        }

        User admin = refreshed.get();
        if (!"ROLE_ADMIN".equalsIgnoreCase(safe(admin.getRole()))) {
            showError("La session courante n'est pas une session administrateur.");
            return null;
        }

        AuthSession.setCurrentUser(admin);
        return admin;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de Sécurité");
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        alert.showAndWait();
    }

    private void styleAlert(Alert alert) {
        try {
            alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/AdminSettings.css").toExternalForm()
            );
        } catch (Exception ignored) {}
    }
}