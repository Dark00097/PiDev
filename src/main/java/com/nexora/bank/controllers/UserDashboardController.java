package com.nexora.bank.controllers;

import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.Notification;
import com.nexora.bank.Models.User;
import com.nexora.bank.SceneRouter;
import com.nexora.bank.Service.NotificationService;
import com.nexora.bank.Utils.ProfileImageUtils;
import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class UserDashboardController {

    @FXML private Button btnDashboard;
    @FXML private Button btnCompte;
    @FXML private Button btnTransaction;
    @FXML private Button btnCredit;
    @FXML private Button btnCashback;
    @FXML private Button btnSettings;

    @FXML private Node sectionDashboard;
    @FXML private Node sectionCompte;
    @FXML private Node sectionTransaction;
    @FXML private Node sectionCredit;
    @FXML private Node sectionCashback;
    @FXML private Node sectionProfile;
    @FXML private UserDashboardOverviewSectionController sectionDashboardController;
    @FXML private UserDashboardProfileSectionController sectionProfileController;

    @FXML private Label lblUserName;
    @FXML private Label lblPageTitle;
    @FXML private Label lblPageSubtitle;
    @FXML private Label lblBreadcrumb;
    @FXML private Label lblUserNotificationCount;
    @FXML private ImageView imgNavbarAvatar;
    @FXML private FontIcon iconNavbarAvatarFallback;

    private static final String NAV_ACTIVE_CLASS = "nav-button-active";
    private static final Duration TRANSITION_DURATION = Duration.millis(300);

    private Button currentActiveButton;
    private Node currentActiveSection;
    private final NotificationService notificationService = new NotificationService();
    private ContextMenu notificationsMenu;
    private Timeline notificationRefreshTimeline;

    private final String[][] pageInfo = {
        {"Tableau de bord client", "Vue globale de toutes vos activites", "Dashboard"},
        {"Comptes bancaires", "Gerez et suivez vos comptes", "Comptes"},
        {"Transactions", "Consultez l historique de vos transactions", "Transactions"},
        {"Services de credit", "Prets, cartes de credit et financement", "Credit"},
        {"Recompenses", "Cashback et offres exclusives", "Recompenses"},
        {"Mon profil", "Gerez votre identite, securite et acces", "Profil"}
    };

    @FXML
    private void initialize() {
        setupTooltips();
        setupHoverEffects();
        showDashboard();
        startNotificationAutoRefresh();
        refreshNotificationBadge();

        if (sectionProfileController != null) {
            sectionProfileController.setProfileUpdatedCallback(this::refreshCurrentUserVisuals);
        }
        refreshCurrentUserVisuals();
        refreshNotificationBadge();
    }

    private void setupTooltips() {
        createModernTooltip(btnDashboard, "Vue globale de vos services");
        createModernTooltip(btnCompte, "Voir et gerer vos comptes bancaires");
        createModernTooltip(btnTransaction, "Suivre toutes vos transactions");
        createModernTooltip(btnCredit, "Explorer les services de credit");
        createModernTooltip(btnCashback, "Consulter vos recompenses et cashback");
        createModernTooltip(btnSettings, "Ouvrir votre profil et vos parametres de securite");
    }

    private void createModernTooltip(Button button, String text) {
        if (button != null) {
            Tooltip tooltip = new Tooltip(text);
            tooltip.setShowDelay(Duration.millis(300));
            tooltip.setHideDelay(Duration.millis(100));
            Tooltip.install(button, tooltip);
        }
    }

    private void setupHoverEffects() {
        Button[] buttons = {btnDashboard, btnCompte, btnTransaction, btnCredit, btnCashback};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.setOnMouseEntered(e -> {
                    if (!btn.getStyleClass().contains(NAV_ACTIVE_CLASS)) {
                        animateButtonHover(btn, true);
                    }
                });
                btn.setOnMouseExited(e -> {
                    if (!btn.getStyleClass().contains(NAV_ACTIVE_CLASS)) {
                        animateButtonHover(btn, false);
                    }
                });
            }
        }
    }

    private void animateButtonHover(Button button, boolean entering) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
        scale.setToX(entering ? 1.03 : 1.0);
        scale.setToY(entering ? 1.03 : 1.0);
        scale.setInterpolator(Interpolator.EASE_BOTH);
        scale.play();
    }

    @FXML
    private void showDashboard() {
        if (sectionDashboardController != null) {
            sectionDashboardController.refreshOverview();
        }
        switchSection(sectionDashboard, btnDashboard, 0);
    }

    @FXML
    private void showCompte() {
        switchSection(sectionCompte, btnCompte, 1);
    }

    @FXML
    private void showTransaction() {
        switchSection(sectionTransaction, btnTransaction, 2);
    }

    @FXML
    private void showCredit() {
        switchSection(sectionCredit, btnCredit, 3);
    }

    @FXML
    private void showCashback() {
        switchSection(sectionCashback, btnCashback, 4);
    }

    @FXML
    private void showProfile() {
        if (sectionProfileController != null) {
            sectionProfileController.refreshProfile();
        }
        refreshCurrentUserVisuals();
        switchSection(sectionProfile, null, 5);
    }

    private void refreshCurrentUserVisuals() {
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        if (lblUserName != null) {
            String prenom = currentUser.getPrenom();
            String nom = currentUser.getNom();
            String fullName = ((prenom == null ? "" : prenom.trim()) + " " + (nom == null ? "" : nom.trim())).trim();
            if (fullName.isEmpty()) {
                fullName = currentUser.getEmail();
            }
            lblUserName.setText(fullName);
        }

        if (imgNavbarAvatar != null) {
            ProfileImageUtils.applyCircularClip(imgNavbarAvatar, 42);
            Image avatar = ProfileImageUtils.loadImageOrNull(currentUser.getProfileImagePath(), 42, 42);
            boolean hasAvatar = avatar != null;
            imgNavbarAvatar.setImage(avatar);
            imgNavbarAvatar.setVisible(hasAvatar);
            imgNavbarAvatar.setManaged(hasAvatar);

            if (iconNavbarAvatarFallback != null) {
                iconNavbarAvatarFallback.setVisible(!hasAvatar);
                iconNavbarAvatarFallback.setManaged(!hasAvatar);
            }
        }
    }

    private void switchSection(Node newSection, Button newButton, int pageIndex) {
        if (newSection == currentActiveSection) {
            if (newSection == sectionDashboard && sectionDashboardController != null) {
                sectionDashboardController.refreshOverview();
            }
            return;
        }

        if (newSection == sectionDashboard && sectionDashboardController != null) {
            sectionDashboardController.refreshOverview();
        }

        // Update page header
        updatePageHeader(pageIndex);

        // Animate out current section
        if (currentActiveSection != null) {
            animateSectionOut(currentActiveSection, () -> {
                setVisibleManaged(currentActiveSection, false);
                
                // Show and animate in new section
                setVisibleManaged(newSection, true);
                animateSectionIn(newSection);
            });
        } else {
            // First load - just show the section
            hideAllSections();
            setVisibleManaged(newSection, true);
            animateSectionIn(newSection);
        }

        // Update navigation state
        updateNavigation(newButton);
        setSettingsButtonActive(newSection == sectionProfile);
        
        currentActiveSection = newSection;
        currentActiveButton = newButton;
    }

    private void updatePageHeader(int pageIndex) {
        if (lblPageTitle != null && lblPageSubtitle != null && pageIndex < pageInfo.length) {
            // Fade out, update, fade in
            FadeTransition fadeOut = new FadeTransition(Duration.millis(100), lblPageTitle);
            fadeOut.setToValue(0.5);
            fadeOut.setOnFinished(e -> {
                lblPageTitle.setText(pageInfo[pageIndex][0]);
                lblPageSubtitle.setText(pageInfo[pageIndex][1]);
                if (lblBreadcrumb != null) {
                    lblBreadcrumb.setText(pageInfo[pageIndex][2]);
                }
                
                FadeTransition fadeIn = new FadeTransition(Duration.millis(150), lblPageTitle);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        }
    }

    private void hideAllSections() {
        setVisibleManaged(sectionDashboard, false);
        setVisibleManaged(sectionCompte, false);
        setVisibleManaged(sectionTransaction, false);
        setVisibleManaged(sectionCredit, false);
        setVisibleManaged(sectionCashback, false);
        setVisibleManaged(sectionProfile, false);
    }

    private void animateSectionIn(Node section) {
        if (section == null) return;

        section.setOpacity(0);
        section.setTranslateY(20);
        section.setScaleX(0.98);
        section.setScaleY(0.98);

        ParallelTransition entrance = new ParallelTransition();

        // Fade in
        FadeTransition fade = new FadeTransition(TRANSITION_DURATION, section);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        // Slide up
        TranslateTransition slide = new TranslateTransition(TRANSITION_DURATION, section);
        slide.setFromY(20);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));

        // Scale up
        ScaleTransition scale = new ScaleTransition(TRANSITION_DURATION, section);
        scale.setFromX(0.98);
        scale.setFromY(0.98);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_OUT);

        entrance.getChildren().addAll(fade, slide, scale);
        entrance.play();
    }

    private void animateSectionOut(Node section, Runnable onFinished) {
        if (section == null) {
            if (onFinished != null) onFinished.run();
            return;
        }

        FadeTransition fade = new FadeTransition(Duration.millis(150), section);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setInterpolator(Interpolator.EASE_IN);
        fade.setOnFinished(e -> {
            if (onFinished != null) onFinished.run();
        });
        fade.play();
    }

    private void updateNavigation(Button activeButton) {
        Button[] allButtons = {btnDashboard, btnCompte, btnTransaction, btnCredit, btnCashback};
        
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.getStyleClass().remove(NAV_ACTIVE_CLASS);
                btn.setScaleX(1.0);
                btn.setScaleY(1.0);
            }
        }

        if (activeButton != null && !activeButton.getStyleClass().contains(NAV_ACTIVE_CLASS)) {
            activeButton.getStyleClass().add(NAV_ACTIVE_CLASS);
            
            // Subtle bounce animation for active button
            ScaleTransition bounce = new ScaleTransition(Duration.millis(200), activeButton);
            bounce.setFromX(0.95);
            bounce.setFromY(0.95);
            bounce.setToX(1.0);
            bounce.setToY(1.0);
            bounce.setInterpolator(Interpolator.EASE_OUT);
            bounce.play();
        }
    }

    private void setSettingsButtonActive(boolean active) {
        if (btnSettings == null) {
            return;
        }
        if (active) {
            if (!btnSettings.getStyleClass().contains("navbar-action-button-active")) {
                btnSettings.getStyleClass().add("navbar-action-button-active");
            }
        } else {
            btnSettings.getStyleClass().remove("navbar-action-button-active");
        }
    }

    @FXML
    private void openHome() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Deconnexion");
        alert.setHeaderText("Voulez-vous vraiment vous deconnecter ?");
        alert.setContentText("Vous serez redirige vers la page d accueil.");
        
        // Style the alert
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/UserDashboard.css").toExternalForm()
        );
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                AuthSession.clear();
                SceneRouter.show("/fxml/Home.fxml", "NEXORA BANK - Accueil", 1200, 760, 980, 680);
            }
        });
    }

    @FXML
    private void openNotifications(ActionEvent event) {
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null) {
            showInfoAlert("Notifications", "Session utilisateur introuvable.", "");
            return;
        }

        try {
            List<Notification> notifications = notificationService.getRecentNotificationsFor(currentUser, 12);
            showNotificationsPanel(event, notifications);
            notificationService.markAllAsRead(currentUser);
            refreshNotificationBadge();
        } catch (Exception ex) {
            showInfoAlert("Notifications", "Impossible de charger les notifications.", "");
        }
    }

    @FXML
    private void openSettings() {
        showProfile();
    }

    private void showInfoAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/UserDashboard.css").toExternalForm()
        );
        alert.show();
    }

    private void refreshNotificationBadge() {
        if (lblUserNotificationCount == null) {
            return;
        }

        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null) {
            lblUserNotificationCount.setText("0");
            lblUserNotificationCount.setVisible(true);
            lblUserNotificationCount.setManaged(true);
            return;
        }

        try {
            int unreadCount = notificationService.countUnreadFor(currentUser);
            lblUserNotificationCount.setText(formatNotificationCount(unreadCount));
            lblUserNotificationCount.setVisible(true);
            lblUserNotificationCount.setManaged(true);
        } catch (Exception ex) {
            lblUserNotificationCount.setText("0");
            lblUserNotificationCount.setVisible(true);
            lblUserNotificationCount.setManaged(true);
        }
    }

    private String safeLabel(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String formatNotificationCount(int count) {
        if (count < 0) {
            return "0";
        }
        if (count > 99) {
            return "99+";
        }
        return String.valueOf(count);
    }

    private void startNotificationAutoRefresh() {
        if (notificationRefreshTimeline != null) {
            notificationRefreshTimeline.stop();
        }
        notificationRefreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(8), e -> refreshNotificationBadge())
        );
        notificationRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        notificationRefreshTimeline.play();
    }

    private void showNotificationsPanel(ActionEvent event, List<Notification> notifications) {
        if (event == null || !(event.getSource() instanceof Node anchor)) {
            return;
        }

        if (notificationsMenu != null && notificationsMenu.isShowing()) {
            notificationsMenu.hide();
            return;
        }

        VBox panel = new VBox(10);
        panel.setPrefWidth(360);
        panel.setMaxWidth(360);
        panel.setStyle(
            "-fx-background-color: #ffffff;" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: #dbe3ef;" +
            "-fx-border-radius: 14;" +
            "-fx-padding: 12;"
        );

        Label title = new Label("Notifications");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #0A2540;");

        VBox listContainer = new VBox(8);
        if (notifications == null || notifications.isEmpty()) {
            Label empty = new Label("Aucune notification.");
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            listContainer.getChildren().add(empty);
        } else {
            for (Notification notification : notifications) {
                VBox row = new VBox(3);
                row.setStyle(
                    "-fx-background-color: #f8fbff;" +
                    "-fx-background-radius: 10;" +
                    "-fx-padding: 10;" +
                    "-fx-border-color: #e8eef7;" +
                    "-fx-border-radius: 10;"
                );

                Label rowTitle = new Label(safeLabel(notification.getTitle()));
                rowTitle.setWrapText(true);
                rowTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

                Label rowMessage = new Label(safeLabel(notification.getMessage()));
                rowMessage.setWrapText(true);
                rowMessage.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");

                Label rowTime = new Label("At: " + safeLabel(notification.getCreatedAt()));
                rowTime.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

                row.getChildren().addAll(rowTitle, rowMessage, rowTime);
                listContainer.getChildren().add(row);
            }
        }

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(300);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        panel.getChildren().addAll(title, scrollPane);

        notificationsMenu = new ContextMenu();
        CustomMenuItem customItem = new CustomMenuItem(panel, false);
        notificationsMenu.getItems().setAll(customItem);
        notificationsMenu.setAutoHide(true);
        notificationsMenu.show(anchor, Side.BOTTOM, -300, 8);
    }

    private void setVisibleManaged(Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }
}
