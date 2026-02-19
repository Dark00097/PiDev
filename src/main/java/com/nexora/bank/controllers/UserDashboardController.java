package com.nexora.bank.controllers;

import com.nexora.bank.AuthSession;
import com.nexora.bank.SceneRouter;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class UserDashboardController {

    @FXML private Button btnCompte;
    @FXML private Button btnTransaction;
    @FXML private Button btnCredit;
    @FXML private Button btnCashback;

    @FXML private VBox sectionCompte;
    @FXML private VBox sectionTransaction;
    @FXML private VBox sectionCredit;
    @FXML private VBox sectionCashback;

    @FXML private Label lblUserName;
    @FXML private Label lblPageTitle;
    @FXML private Label lblPageSubtitle;
    @FXML private Label lblBreadcrumb;

    private static final String NAV_ACTIVE_CLASS = "nav-button-active";
    private static final Duration TRANSITION_DURATION = Duration.millis(300);

    private Button currentActiveButton;
    private VBox currentActiveSection;

    private final String[][] pageInfo = {
        {"Bank Accounts", "Manage and monitor your accounts", "Accounts"},
        {"Transactions", "View your transaction history", "Transactions"},
        {"Credit Services", "Loans, credit cards & financing", "Credit"},
        {"Rewards", "Cashback and exclusive offers", "Rewards"}
    };

    @FXML
    private void initialize() {
        setupTooltips();
        setupHoverEffects();
        showCompte();
        
        // Initialize user name if session exists
        if (AuthSession.getCurrentUser() != null && lblUserName != null) {
            String prenom = AuthSession.getCurrentUser().getPrenom();
            String nom = AuthSession.getCurrentUser().getNom();
            String fullName = ((prenom == null ? "" : prenom.trim()) + " " + (nom == null ? "" : nom.trim())).trim();
            if (fullName.isEmpty()) {
                fullName = AuthSession.getCurrentUser().getEmail();
            }
            lblUserName.setText(fullName);
        }
    }

    private void setupTooltips() {
        createModernTooltip(btnCompte, "View and manage your bank accounts");
        createModernTooltip(btnTransaction, "Track all your transactions");
        createModernTooltip(btnCredit, "Explore credit services");
        createModernTooltip(btnCashback, "Check your rewards and cashback");
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
        Button[] buttons = {btnCompte, btnTransaction, btnCredit, btnCashback};
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
    private void showCompte() {
        switchSection(sectionCompte, btnCompte, 0);
    }

    @FXML
    private void showTransaction() {
        switchSection(sectionTransaction, btnTransaction, 1);
    }

    @FXML
    private void showCredit() {
        switchSection(sectionCredit, btnCredit, 2);
    }

    @FXML
    private void showCashback() {
        switchSection(sectionCashback, btnCashback, 3);
    }

    private void switchSection(VBox newSection, Button newButton, int pageIndex) {
        if (newSection == currentActiveSection) {
            return;
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
        setVisibleManaged(sectionCompte, false);
        setVisibleManaged(sectionTransaction, false);
        setVisibleManaged(sectionCredit, false);
        setVisibleManaged(sectionCashback, false);
    }

    private void animateSectionIn(VBox section) {
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

    private void animateSectionOut(VBox section, Runnable onFinished) {
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
        Button[] allButtons = {btnCompte, btnTransaction, btnCredit, btnCashback};
        
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

    @FXML
    private void openHome() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sign Out");
        alert.setHeaderText("Are you sure you want to sign out?");
        alert.setContentText("You will be redirected to the home page.");
        
        // Style the alert
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/UserDashboard.css").toExternalForm()
        );
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                AuthSession.clear();
                SceneRouter.show("/fxml/Home.fxml", "NEXORA BANK - Welcome", 1200, 760, 980, 680);
            }
        });
    }

    @FXML
    private void openNotifications() {
        showInfoAlert("Notifications", "You have 3 new notifications",
            "- Transfer completed successfully\n- New cashback offer available\n- Security update completed");
    }

    @FXML
    private void openSettings() {
        showInfoAlert("Settings", "Settings Panel", 
            "Profile settings, security options, and preferences will be available here.");
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

    private void setVisibleManaged(VBox node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }
}

