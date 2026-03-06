package com.nexora.bank.controllers;

import com.myapp.security.AuthResult;
import com.myapp.security.BiometricVerificationDialog;
import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.User;
import com.nexora.bank.Models.UserActionLog;
import com.nexora.bank.Service.AIAnalysisService;
import com.nexora.bank.Service.GeminiAccountAdvisorService;
import com.nexora.bank.Service.UserService;
import com.nexora.bank.Utils.AIResponseFormatter.FormattedAnalysis;
import com.nexora.bank.Utils.ProfileImageUtils;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import javafx.util.Duration;

public class UserDashboardProfileSectionController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern STRONG_PASSWORD_PATTERN =
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

    @FXML private Label lblProfileName;
    @FXML private Label lblProfileEmail;
    @FXML private Label lblAccountStatus;
    @FXML private Label lblAccountOpenedAt;
    @FXML private Label lblAccountOpenedFrom;
    @FXML private Label lblOpenedLocation;
    @FXML private Label lblOpenedCoordinates;
    @FXML private Button btnOpenMap;
    @FXML private VBox boxInAppMap;
    @FXML private WebView webMapView;
    @FXML private Label lblLastOnlineAt;
    @FXML private Label lblLastOnlineFrom;
    @FXML private ImageView imgProfileAvatar;
    @FXML private FontIcon iconProfileAvatarFallback;

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
    @FXML private Label lblAiStatus;
    @FXML private Button btnSecureAccount;
    @FXML private VBox boxRecentActions;
    @FXML private VBox aiInlineAnalysisCard;
    @FXML private VBox aiEmptyState;
    @FXML private Label lblAiEmptyTitle;
    @FXML private Label lblAiEmptyText;
    @FXML private Label lblAiInlineTimestamp;
    @FXML private VBox sectionAiAccount;
    @FXML private Label lblAiAccountHolder;
    @FXML private VBox sectionAiSummary;
    @FXML private Label lblAiSummary;
    @FXML private VBox sectionAiRisk;
    @FXML private Label lblAiRiskLevel;
    @FXML private VBox boxAiRiskReasons;
    @FXML private VBox sectionAiAdvice;
    @FXML private VBox boxAiAdviceItems;
    @FXML private VBox sectionAiSuspicious;
    @FXML private VBox boxAiSuspiciousItems;

    private final UserService userService = new UserService();
    private final GeminiAccountAdvisorService geminiAccountAdvisorService = new GeminiAccountAdvisorService();
    private final AIAnalysisService aiAnalysisService = new AIAnalysisService();
    private List<UserActionLog> lastLoadedActions = List.of();
    private FormattedAnalysis latestAiAnalysis;
    private Runnable profileUpdatedCallback;
    private String currentMapUrl;

    @FXML
    private void initialize() {
        hideInlineAnalysisCard();
        setAiEmptyContent(false, null);
        showAiEmptyState(true);
        setSecureAccountButtonVisible(false);
        latestAiAnalysis = null;
        ProfileImageUtils.applyCircularClip(imgProfileAvatar, 84);
        configureMapWebView();
        refreshProfile();
    }

    public void setProfileUpdatedCallback(Runnable profileUpdatedCallback) {
        this.profileUpdatedCallback = profileUpdatedCallback;
    }

    public void refreshProfile() {
        User user = requireCurrentUser();
        if (user == null) {
            return;
        }

        try {
            userService.refreshAccountOpenedLocationIfMissing(user.getIdUser());
            Optional<User> refreshedWithLocation = userService.findByIdPublic(user.getIdUser());
            if (refreshedWithLocation.isPresent()) {
                user = refreshedWithLocation.get();
                AuthSession.setCurrentUser(user);
            }
        } catch (Exception ignored) {
        }

        txtNom.setText(safe(user.getNom()));
        txtPrenom.setText(safe(user.getPrenom()));
        txtEmail.setText(safe(user.getEmail()));
        txtTelephone.setText(safe(user.getTelephone()));

        String fullName = (safe(user.getPrenom()) + " " + safe(user.getNom())).trim();
        lblProfileName.setText(fullName.isEmpty() ? "Utilisateur" : fullName);
        lblProfileEmail.setText(safe(user.getEmail()));

        String status = isBlank(user.getStatus()) ? "ACTIF" : user.getStatus();
        lblAccountStatus.setText(status);
        applyStatusStyle(lblAccountStatus, status);

        lblAccountOpenedAt.setText(formatDateTimeShort(user.getCreatedAt()));
        lblAccountOpenedFrom.setText(buildOpenedFromText(user.getAccountOpenedFrom(), user.getAccountOpenedLocation()));
        lblOpenedLocation.setText(isBlank(user.getAccountOpenedLocation()) ? "Unknown location" : user.getAccountOpenedLocation());
        lblOpenedCoordinates.setText(formatCoordinates(user.getAccountOpenedLatitude(), user.getAccountOpenedLongitude()));
        if (btnOpenMap != null) {
            btnOpenMap.setDisable(user.getAccountOpenedLatitude() == null || user.getAccountOpenedLongitude() == null);
        }
        hideMapPanel();
        lblLastOnlineAt.setText(formatDateTimeShort(user.getLastOnlineAt()));
        lblLastOnlineFrom.setText(isBlank(user.getLastOnlineFrom()) ? "Aucune session" : user.getLastOnlineFrom());
        renderProfileAvatar(user);
        chkEnableBiometric.setSelected(user.isBiometricEnabled());
        loadRecentActions(user);
    }

    private void applyStatusStyle(Label label, String status) {
        String normalized = safe(status).toUpperCase();
        if (normalized.contains("ACTIF") || normalized.contains("ACTIVE") || normalized.contains("VERIFIED")) {
            label.setStyle("-fx-text-fill: #10b981; -fx-background-color: rgba(16, 185, 129, 0.15);");
        } else if (normalized.contains("PENDING") || normalized.contains("ATTENTE")) {
            label.setStyle("-fx-text-fill: #f59e0b; -fx-background-color: rgba(245, 158, 11, 0.15);");
        } else {
            label.setStyle("-fx-text-fill: #64748b; -fx-background-color: rgba(100, 116, 139, 0.15);");
        }
    }

    @FXML
    private void handleReloadProfile() {
        refreshProfile();
        setStatus(lblProfileStatus, "Profil actualise avec succes", true);
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
            setStatus(lblProfileStatus, "Tous les champs sont obligatoires", false);
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            setStatus(lblProfileStatus, "Format d email invalide", false);
            return;
        }

        try {
            boolean updated = userService.updateUserOwnProfile(user.getIdUser(), nom, prenom, email, telephone);
            if (!updated) {
                setStatus(lblProfileStatus, "Aucune modification effectuee", false);
                return;
            }

            Optional<User> refreshed = userService.findByIdPublic(user.getIdUser());
            refreshed.ifPresent(AuthSession::setCurrentUser);
            refreshProfile();
            setStatus(lblProfileStatus, "Profil mis a jour avec succes", true);
            notifyProfileUpdated();
        } catch (Exception ex) {
            setStatus(lblProfileStatus, ex.getMessage() == null ? "Erreur lors de la mise a jour" : ex.getMessage(), false);
        }
    }

    @FXML
    private void handleChooseProfileImage() {
        User user = requireCurrentUser();
        if (user == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(
            lblProfileName == null ? null : lblProfileName.getScene().getWindow()
        );
        if (selectedFile == null) {
            return;
        }

        try {
            String storedPath = ProfileImageUtils.storeProfileImage(selectedFile, user.getIdUser());
            boolean updated = userService.updateUserOwnProfileImage(user.getIdUser(), storedPath);
            if (!updated) {
                setStatus(lblProfileStatus, "Impossible de mettre a jour la photo de profil", false);
                return;
            }

            Optional<User> refreshed = userService.findByIdPublic(user.getIdUser());
            if (refreshed.isPresent()) {
                AuthSession.setCurrentUser(refreshed.get());
            }
            refreshProfile();
            setStatus(lblProfileStatus, "Photo de profil mise a jour avec succes", true);
            notifyProfileUpdated();
        } catch (Exception ex) {
            setStatus(lblProfileStatus, ex.getMessage() == null ? "Erreur pendant l enregistrement de la photo" : ex.getMessage(), false);
        }
    }

    @FXML
    private void handleOpenAccountOpenedMap() {
        User user = requireCurrentUser();
        if (user == null) {
            return;
        }

        Double lat = user.getAccountOpenedLatitude();
        Double lng = user.getAccountOpenedLongitude();
        if (lat == null || lng == null) {
            setStatus(lblProfileStatus, "Coordonnees non disponibles pour cette session", false);
            return;
        }

        double delta = 0.0125d;
        double lonMin = lng - delta;
        double latMin = lat - delta;
        double lonMax = lng + delta;
        double latMax = lat + delta;

        String mapUrl = String.format(
            Locale.US,
            "https://www.openstreetmap.org/export/embed.html?bbox=%f%%2C%f%%2C%f%%2C%f&layer=mapnik&marker=%f%%2C%f",
            lonMin, latMin, lonMax, latMax, lat, lng
        );

        try {
            if (webMapView == null || boxInAppMap == null) {
                setStatus(lblProfileStatus, "Composant carte indisponible", false);
                return;
            }
            boxInAppMap.setVisible(true);
            boxInAppMap.setManaged(true);
            currentMapUrl = mapUrl;
            webMapView.getEngine().load(mapUrl);
            Platform.runLater(() -> {
                webMapView.requestLayout();
                requestMapResize();
            });
        } catch (Exception ex) {
            setStatus(lblProfileStatus, "Impossible de charger la carte dans l application", false);
        }
    }

    @FXML
    private void handleHideAccountOpenedMap() {
        hideMapPanel();
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
            AuthResult result = BiometricVerificationDialog.promptAndVerify("Verification biometrique NEXORA");
            if (result != AuthResult.VERIFIED) {
                BiometricVerificationDialog.showResultDialog(result);
                chkEnableBiometric.setSelected(false);
                setStatus(lblSecurityStatus, "Activation annulee", false);
                return;
            }
        }

        try {
            boolean updated = userService.updateUserBiometricPreference(user.getIdUser(), enable);
            if (!updated) {
                chkEnableBiometric.setSelected(!enable);
                setStatus(lblSecurityStatus, "Impossible d enregistrer la preference", false);
                return;
            }

            Optional<User> refreshed = userService.findByIdPublic(user.getIdUser());
            refreshed.ifPresent(AuthSession::setCurrentUser);
            setStatus(lblSecurityStatus, enable ? "Biometrie activee" : "Biometrie desactivee", true);
        } catch (Exception ex) {
            chkEnableBiometric.setSelected(!enable);
            setStatus(lblSecurityStatus, ex.getMessage() == null ? "Erreur de mise a jour" : ex.getMessage(), false);
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
            setStatus(lblPasswordStatus, "Remplissez tous les champs", false);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            setStatus(lblPasswordStatus, "Les mots de passe ne correspondent pas", false);
            return;
        }
        if (!STRONG_PASSWORD_PATTERN.matcher(newPassword).matches()) {
            setStatus(lblPasswordStatus, "Mot de passe faible", false);
            return;
        }

        try {
            boolean updated = userService.updateUserOwnPassword(user.getIdUser(), currentPassword, newPassword);
            if (!updated) {
                setStatus(lblPasswordStatus, "Echec de la mise a jour", false);
                return;
            }

            txtCurrentPassword.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();
            setStatus(lblPasswordStatus, "Mot de passe modifie", true);
        } catch (Exception ex) {
            setStatus(lblPasswordStatus, ex.getMessage() == null ? "Erreur lors du changement" : ex.getMessage(), false);
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
            setStatus(lblResetStatus, "OTP envoye a votre email", true);
        } catch (Exception ex) {
            setStatus(lblResetStatus, ex.getMessage() == null ? "Echec de l envoi OTP" : ex.getMessage(), false);
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
            setStatus(lblResetStatus, "Remplissez tous les champs", false);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            setStatus(lblResetStatus, "Les mots de passe ne correspondent pas", false);
            return;
        }
        if (!STRONG_PASSWORD_PATTERN.matcher(newPassword).matches()) {
            setStatus(lblResetStatus, "Mot de passe faible", false);
            return;
        }

        try {
            boolean updated = userService.resetOwnPasswordWithOtp(user.getIdUser(), otp, newPassword);
            if (!updated) {
                setStatus(lblResetStatus, "Echec de reinitialisation", false);
                return;
            }

            txtResetOtp.clear();
            txtResetNewPassword.clear();
            txtResetConfirmPassword.clear();
            setStatus(lblResetStatus, "Mot de passe reinitialise", true);
        } catch (Exception ex) {
            setStatus(lblResetStatus, ex.getMessage() == null ? "Erreur de reinitialisation" : ex.getMessage(), false);
        }
    }

    @FXML
    private void handleAnalyzeAccountSecurity() {
        User user = requireCurrentUser();
        if (user == null) {
            return;
        }
        runAiAnalysis(user);
    }

    @FXML
    private void handleSecureMyAccount() {
        User user = requireCurrentUser();
        if (user == null) {
            return;
        }
        if (latestAiAnalysis == null) {
            setStatus(lblAiStatus, "Lancez d abord l analyse IA pour afficher le rapport.", false);
            return;
        }

        String nom = safe(txtNom.getText());
        String prenom = safe(txtPrenom.getText());
        String email = safe(txtEmail.getText());
        String telephone = safe(txtTelephone.getText());

        if (nom.isBlank() || prenom.isBlank() || email.isBlank() || telephone.isBlank()) {
            setStatus(lblProfileStatus, "Tous les champs sont obligatoires", false);
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            setStatus(lblProfileStatus, "Format d email invalide", false);
            return;
        }

        setStatus(lblAiStatus, "Securisation en cours...", true);
        setStatus(lblProfileStatus, "Generation d un nouveau mot de passe...", true);
        if (btnSecureAccount != null) {
            btnSecureAccount.setDisable(true);
        }

        Task<UserService.AiSecurityUpdateResult> secureTask = new Task<>() {
            @Override
            protected UserService.AiSecurityUpdateResult call() {
                return userService.secureUserOwnAccountWithAi(
                    user.getIdUser(),
                    nom,
                    prenom,
                    email,
                    telephone,
                    latestAiAnalysis
                );
            }
        };

        secureTask.setOnSucceeded(event -> Platform.runLater(() -> {
            if (btnSecureAccount != null) {
                btnSecureAccount.setDisable(false);
            }

            UserService.AiSecurityUpdateResult updateResult = secureTask.getValue();
            if (updateResult == null) {
                setStatus(lblAiStatus, "Aucun resultat de securisation", false);
                return;
            }

            Optional<User> refreshed = userService.findByIdPublic(user.getIdUser());
            refreshed.ifPresent(AuthSession::setCurrentUser);
            refreshProfile();
            notifyProfileUpdated();

            setStatus(lblAiStatus, "Securisation appliquee", true);
            setStatus(lblProfileStatus, "Nouveau mot de passe envoye par email.", true);
        }));

        secureTask.setOnFailed(event -> Platform.runLater(() -> {
            if (btnSecureAccount != null) {
                btnSecureAccount.setDisable(false);
            }
            Throwable error = secureTask.getException();
            String message = error == null || error.getMessage() == null
                ? "Echec de la securisation IA"
                : error.getMessage();
            setStatus(lblAiStatus, message, false);
            setStatus(lblProfileStatus, message, false);
        }));

        Thread worker = new Thread(secureTask, "ai-account-secure");
        worker.setDaemon(true);
        worker.start();
    }

    private void runAiAnalysis(User user) {
        List<UserActionLog> actions = resolveActionsForAnalysis(user);
        latestAiAnalysis = null;
        setSecureAccountButtonVisible(false);

        setStatus(lblAiStatus, "Analyse en cours...", true);
        hideInlineAnalysisCard();
        setAiEmptyContent(true, null);
        showAiEmptyState(true);

        List<UserActionLog> finalActions = actions;
        Task<String> aiTask = new Task<>() {
            @Override
            protected String call() {
                return geminiAccountAdvisorService.analyzeUserAccount(user, finalActions);
            }
        };

        aiTask.setOnSucceeded(event -> Platform.runLater(() -> {
            String advice = aiTask.getValue();
            FormattedAnalysis analysis = aiAnalysisService.getFormattedAnalysis(advice);
            latestAiAnalysis = analysis;
            renderInlineAnalysis(analysis);
            setSecureAccountButtonVisible(true);
            setStatus(lblAiStatus, "Analyse terminee. Cliquez sur 'Secure my account'.", true);
        }));

        aiTask.setOnFailed(event -> Platform.runLater(() -> {
            Throwable error = aiTask.getException();
            latestAiAnalysis = null;
            setSecureAccountButtonVisible(false);
            setStatus(lblAiStatus, error == null ? "Echec de l analyse" : "Erreur: " + error.getMessage(), false);
            setAiEmptyContent(false, "Impossible d afficher l analyse pour le moment.");
            showAiEmptyState(true);
        }));

        Thread worker = new Thread(aiTask, "gemini-account-advisor");
        worker.setDaemon(true);
        worker.start();
    }

    private List<UserActionLog> resolveActionsForAnalysis(User user) {
        List<UserActionLog> actions = lastLoadedActions;
        if (actions == null || actions.isEmpty()) {
            actions = userService.getRecentUserActions(user.getIdUser(), 20);
        }
        return actions;
    }

    private void showAiEmptyState(boolean show) {
        if (aiEmptyState != null) {
            aiEmptyState.setVisible(show);
            aiEmptyState.setManaged(show);
        }
    }

    private void setAiEmptyContent(boolean loading, String customMessage) {
        if (lblAiEmptyTitle == null || lblAiEmptyText == null) {
            return;
        }
        if (loading) {
            lblAiEmptyTitle.setText("Analyse en traitement");
            lblAiEmptyText.setText("Veuillez patienter pendant la generation du rapport IA...");
            return;
        }
        if (isNotEmpty(customMessage)) {
            lblAiEmptyTitle.setText("Analyse indisponible");
            lblAiEmptyText.setText(customMessage);
            return;
        }
        lblAiEmptyTitle.setText("Aucune analyse disponible");
        lblAiEmptyText.setText("Cliquez sur 'Analyser avec IA' pour obtenir un rapport personnalise");
    }

    private void renderInlineAnalysis(FormattedAnalysis analysis) {
        if (analysis == null || aiInlineAnalysisCard == null) {
            showAiEmptyState(true);
            return;
        }

        showAiEmptyState(false);
        aiInlineAnalysisCard.setVisible(true);
        aiInlineAnalysisCard.setManaged(true);

        if (lblAiInlineTimestamp != null) {
            lblAiInlineTimestamp.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        }

        toggleSection(sectionAiAccount, isNotEmpty(analysis.getAccountHolder()));
        if (lblAiAccountHolder != null) {
            lblAiAccountHolder.setText(isNotEmpty(analysis.getAccountHolder()) ? analysis.getAccountHolder() : "");
        }

        toggleSection(sectionAiSummary, isNotEmpty(analysis.getSummary()));
        if (lblAiSummary != null) {
            lblAiSummary.setText(isNotEmpty(analysis.getSummary()) ? analysis.getSummary() : "");
        }

        boolean hasRisk = isNotEmpty(analysis.getRiskLevel());
        toggleSection(sectionAiRisk, hasRisk);
        if (hasRisk) {
            lblAiRiskLevel.setText(analysis.getRiskLevel());
            applyInlineRiskStyle(analysis.getRiskLevel());
            populateInlineList(boxAiRiskReasons, analysis.getRiskReasons(), "warning");
        }

        toggleSection(sectionAiAdvice, analysis.getSecurityAdvice() != null && !analysis.getSecurityAdvice().isEmpty());
        if (analysis.getSecurityAdvice() != null) {
            populateInlineList(boxAiAdviceItems, analysis.getSecurityAdvice(), "success");
        }

        toggleSection(sectionAiSuspicious, analysis.getSuspiciousItems() != null && !analysis.getSuspiciousItems().isEmpty());
        if (analysis.getSuspiciousItems() != null) {
            populateInlineList(boxAiSuspiciousItems, analysis.getSuspiciousItems(), "danger");
        }
    }

    private void populateInlineList(VBox container, List<String> items, String type) {
        if (container == null) {
            return;
        }
        container.getChildren().clear();
        if (items == null || items.isEmpty()) {
            return;
        }
        for (String item : items) {
            if (isNotEmpty(item)) {
                container.getChildren().add(createInlineListItem(item, type));
            }
        }
    }

    private HBox createInlineListItem(String text, String type) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("nx-ai-list-item");
        if (type != null && !type.isBlank()) {
            row.getStyleClass().add("nx-ai-list-item-" + type);
        }

        Label bullet = new Label("-");
        bullet.getStyleClass().add("nx-ai-list-bullet");

        Label content = new Label(text);
        content.setWrapText(true);
        content.getStyleClass().add("nx-ai-list-text");

        row.getChildren().addAll(bullet, content);
        return row;
    }

    private void applyInlineRiskStyle(String level) {
        if (sectionAiRisk == null || lblAiRiskLevel == null) {
            return;
        }
        sectionAiRisk.getStyleClass().removeAll("nx-ai-risk-low", "nx-ai-risk-medium", "nx-ai-risk-high", "nx-ai-risk-critical");
        lblAiRiskLevel.getStyleClass().removeAll("nx-ai-risk-badge-low", "nx-ai-risk-badge-medium", "nx-ai-risk-badge-high", "nx-ai-risk-badge-critical");

        String normalized = level == null ? "" : level.trim().toUpperCase();
        switch (normalized) {
            case "LOW" -> {
                sectionAiRisk.getStyleClass().add("nx-ai-risk-low");
                lblAiRiskLevel.getStyleClass().add("nx-ai-risk-badge-low");
            }
            case "HIGH" -> {
                sectionAiRisk.getStyleClass().add("nx-ai-risk-high");
                lblAiRiskLevel.getStyleClass().add("nx-ai-risk-badge-high");
            }
            case "CRITICAL" -> {
                sectionAiRisk.getStyleClass().add("nx-ai-risk-critical");
                lblAiRiskLevel.getStyleClass().add("nx-ai-risk-badge-critical");
            }
            default -> {
                sectionAiRisk.getStyleClass().add("nx-ai-risk-medium");
                lblAiRiskLevel.getStyleClass().add("nx-ai-risk-badge-medium");
            }
        }
    }

    private void toggleSection(VBox section, boolean show) {
        if (section == null) {
            return;
        }
        section.setVisible(show);
        section.setManaged(show);
    }

    private void hideInlineAnalysisCard() {
        if (aiInlineAnalysisCard == null) {
            return;
        }
        aiInlineAnalysisCard.setVisible(false);
        aiInlineAnalysisCard.setManaged(false);
    }

    private void hideMapPanel() {
        if (boxInAppMap != null) {
            boxInAppMap.setVisible(false);
            boxInAppMap.setManaged(false);
        }
        if (webMapView != null) {
            webMapView.getEngine().load("about:blank");
        }
        currentMapUrl = null;
    }

    private void configureMapWebView() {
        if (webMapView == null) {
            return;
        }

        webMapView.getEngine().setJavaScriptEnabled(true);
        webMapView.setContextMenuEnabled(false);
        webMapView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                requestMapResize();
                scheduleMapFix(300);
                scheduleMapFix(900);
                scheduleMapFix(1600);
            }
        });

        webMapView.widthProperty().addListener((obs, oldWidth, newWidth) -> requestMapResize());
        webMapView.heightProperty().addListener((obs, oldHeight, newHeight) -> requestMapResize());
        if (boxInAppMap != null) {
            boxInAppMap.widthProperty().addListener((obs, oldWidth, newWidth) -> requestMapResize());
            boxInAppMap.heightProperty().addListener((obs, oldHeight, newHeight) -> requestMapResize());
        }
    }

    private void requestMapResize() {
        if (webMapView == null) {
            return;
        }

        if (webMapView.getEngine().getLoadWorker().getState() != Worker.State.SUCCEEDED) {
            return;
        }

        runMapResizeScript();
        scheduleMapFix(220);
        scheduleMapFix(650);
    }

    private void runMapResizeScript() {
        try {
            webMapView.getEngine().executeScript("""
                try {
                    if (document && document.head && !document.getElementById('nx-map-webview-fix')) {
                        const style = document.createElement('style');
                        style.id = 'nx-map-webview-fix';
                        style.textContent = `
                            html, body, #map {
                                width: 100% !important;
                                height: 100% !important;
                                margin: 0 !important;
                                overflow: hidden !important;
                            }
                            .leaflet-container img.leaflet-tile {
                                mix-blend-mode: normal !important;
                                image-rendering: auto !important;
                            }
                        `;
                        document.head.appendChild(style);
                    }

                    window.dispatchEvent(new Event('resize'));
                    let mapRef = window.map;
                    if (!mapRef) {
                        for (const key in window) {
                            const candidate = window[key];
                            if (candidate && typeof candidate.invalidateSize === 'function' && typeof candidate.getContainer === 'function') {
                                mapRef = candidate;
                                break;
                            }
                        }
                    }
                    if (mapRef) {
                        mapRef.invalidateSize(true);
                        if (typeof mapRef.eachLayer === 'function') {
                            mapRef.eachLayer(layer => {
                                if (layer && typeof layer.redraw === 'function') {
                                    layer.redraw();
                                }
                            });
                        }
                    }

                    const brokenTiles = document.querySelectorAll('img.leaflet-tile');
                    brokenTiles.forEach(tile => {
                        if (tile.complete && tile.naturalWidth === 0) {
                            const oldSrc = tile.src;
                            tile.src = '';
                            tile.src = oldSrc;
                        }
                    });

                    if (mapRef && typeof mapRef.getContainer === 'function') {
                        mapRef.getContainer().style.width = '100%';
                        mapRef.getContainer().style.height = '100%';
                    }
                } catch (e) {}
            """);
        } catch (Exception ignored) {
            if (currentMapUrl != null && !currentMapUrl.isBlank()) {
                try {
                    webMapView.getEngine().load(currentMapUrl);
                } catch (Exception ignoredAgain) {
                }
            }
        }
    }

    private void scheduleMapFix(double millis) {
        PauseTransition delay = new PauseTransition(Duration.millis(millis));
        delay.setOnFinished(event -> runMapResizeScript());
        delay.play();
    }

    private void renderProfileAvatar(User user) {
        if (imgProfileAvatar == null) {
            return;
        }

        Image avatar = user == null
            ? null
            : ProfileImageUtils.loadImageOrNull(user.getProfileImagePath(), 84, 84);

        boolean hasAvatar = avatar != null;
        imgProfileAvatar.setImage(avatar);
        imgProfileAvatar.setVisible(hasAvatar);
        imgProfileAvatar.setManaged(hasAvatar);

        if (iconProfileAvatarFallback != null) {
            iconProfileAvatarFallback.setVisible(!hasAvatar);
            iconProfileAvatarFallback.setManaged(!hasAvatar);
        }
    }

    private void loadRecentActions(User user) {
        if (user == null || user.getIdUser() <= 0) {
            return;
        }

        try {
            lastLoadedActions = userService.getRecentUserActions(user.getIdUser(), 20);
            renderRecentActions(lastLoadedActions);
        } catch (Exception ex) {
            lastLoadedActions = List.of();
            renderRecentActions(lastLoadedActions);
            setStatus(lblAiStatus, "Impossible de charger l historique", false);
        }
    }

    private void renderRecentActions(List<UserActionLog> actions) {
        if (boxRecentActions == null) {
            return;
        }

        boxRecentActions.getChildren().clear();
        if (actions == null || actions.isEmpty()) {
            VBox emptyBox = new VBox(8);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setStyle("-fx-padding: 40px 20px;");

            Label emptyText = new Label("Aucune action recente");
            emptyText.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

            emptyBox.getChildren().add(emptyText);
            boxRecentActions.getChildren().add(emptyBox);
            return;
        }

        for (UserActionLog action : actions) {
            VBox item = new VBox(4);
            item.getStyleClass().add("action-item");

            Label title = new Label(formatActionType(action.getActionType()));
            title.getStyleClass().add("action-item-title");

            String source = isBlank(action.getActionSource()) ? "Source inconnue" : safe(action.getActionSource());
            String time = formatDateTimeShort(action.getCreatedAt());
            String details = isBlank(action.getDetails()) ? "" : " - " + safe(action.getDetails());

            Label meta = new Label(time + " - " + source + details);
            meta.setWrapText(true);
            meta.getStyleClass().add("action-item-meta");

            item.getChildren().addAll(title, meta);
            boxRecentActions.getChildren().add(item);
        }
    }

    private String formatActionType(String actionType) {
        String value = safe(actionType).toUpperCase();
        return switch (value) {
            case "SIGNUP" -> "Inscription du compte";
            case "LOGIN" -> "Connexion utilisateur";
            case "LOGOUT" -> "Deconnexion";
            case "PASSWORD_CHANGE" -> "Changement mot de passe";
            case "PROFILE_UPDATE" -> "Mise a jour du profil";
            case "AI_ACCOUNT_SECURED" -> "Securisation IA du compte";
            default -> value.isBlank() ? "Action compte" : value;
        };
    }

    private User requireCurrentUser() {
        User current = AuthSession.getCurrentUser();
        if (current == null || current.getIdUser() <= 0) {
            setStatus(lblProfileStatus, "Session expiree. Veuillez vous reconnecter.", false);
            return null;
        }

        Optional<User> refreshed = userService.findByIdPublic(current.getIdUser());
        if (refreshed.isEmpty()) {
            setStatus(lblProfileStatus, "Compte introuvable. Veuillez vous reconnecter.", false);
            return null;
        }

        User user = refreshed.get();
        if (!"ROLE_USER".equalsIgnoreCase(safe(user.getRole()))) {
            setStatus(lblProfileStatus, "Acces reserve aux utilisateurs.", false);
            return null;
        }

        AuthSession.setCurrentUser(user);
        return user;
    }

    private void setStatus(Label label, String message, boolean success) {
        if (label == null) {
            return;
        }
        label.setText(message == null ? "" : message);
        if (success) {
            label.setStyle("-fx-text-fill: #059669; -fx-font-size: 12px; -fx-font-weight: 600; " +
                "-fx-background-color: rgba(16, 185, 129, 0.1); -fx-background-radius: 20; -fx-padding: 6 12;");
        } else {
            label.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px; -fx-font-weight: 600; " +
                "-fx-background-color: rgba(220, 38, 38, 0.1); -fx-background-radius: 20; -fx-padding: 6 12;");
        }
    }

    private String formatDateTimeShort(String value) {
        if (isBlank(value)) {
            return "-";
        }
        try {
            String clean = value.replace('T', ' ');
            if (clean.length() > 16) {
                clean = clean.substring(0, 16);
            }
            return clean;
        } catch (Exception e) {
            return value;
        }
    }

    private String formatCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return "-";
        }
        return String.format(Locale.US, "%.6f, %.6f", latitude, longitude);
    }

    private String buildOpenedFromText(String source, String location) {
        String cleanSource = isBlank(source) ? "Appareil inconnu" : source.trim();
        String cleanLocation = isBlank(location) ? "" : location.trim();
        if (cleanLocation.isBlank() || "Unknown location".equalsIgnoreCase(cleanLocation)) {
            return cleanSource;
        }
        return cleanSource + " | " + cleanLocation;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private boolean isNotEmpty(String value) {
        return !isBlank(value);
    }

    private void setSecureAccountButtonVisible(boolean visible) {
        if (btnSecureAccount == null) {
            return;
        }
        btnSecureAccount.setVisible(visible);
        btnSecureAccount.setManaged(visible);
        btnSecureAccount.setDisable(!visible);
    }

    private void notifyProfileUpdated() {
        if (profileUpdatedCallback != null) {
            profileUpdatedCallback.run();
        }
    }
}
