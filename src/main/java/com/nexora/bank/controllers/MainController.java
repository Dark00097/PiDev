package com.nexora.bank.controllers;

import com.nexora.bank.SceneRouter;
import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.Cashback;
import com.nexora.bank.Models.CompteBancaire;
import com.nexora.bank.Models.Credit;
import com.nexora.bank.Models.Notification;
import com.nexora.bank.Models.Transaction;
import com.nexora.bank.Models.User;
import com.nexora.bank.Service.CashbackService;
import com.nexora.bank.Service.CompteBancaireService;
import com.nexora.bank.Service.CreditService;
import com.nexora.bank.Service.NotificationService;
import com.nexora.bank.Service.PendingCompteNotificationService;
import com.nexora.bank.Service.TransactionService;
import com.nexora.bank.Service.UserService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.chart.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.stage.Window;
import javafx.util.Duration;

import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MainController implements Initializable {
    private static final String RESPONSIVE_CSS = "/css/Responsive.css";
    private static final String COMPACT_CLASS = "nx-compact";

    // =============================================
    // FXML INJECTED COMPONENTS
    // =============================================
    
    @FXML private VBox sidebar;
    @FXML private HBox sidebarHeader;
    @FXML private StackPane sidebarLogoContainer;
    @FXML private VBox sidebarBrandText;
    @FXML private Region sidebarHeaderSpacer;
    @FXML private ScrollPane sidebarScroll;
    @FXML private VBox sidebarFooter;
    @FXML private VBox sidebarUserInfo;
    @FXML private Button toggleSidebarBtn;
    @FXML private HBox topBar;
    @FXML private TextField searchField;
    @FXML private StackPane contentArea;
    @FXML private ScrollPane contentScrollPane;
    @FXML private VBox dashboardContent;
    @FXML private VBox sidebarNav;
    @FXML private Label lblAdminNotificationCount;
    
    // DateTime Labels
    @FXML private Label currentDate;
    @FXML private Label currentTime;
    
    // Charts
    @FXML private AreaChart<String, Number> transactionChart;
    @FXML private PieChart accountPieChart;
    @FXML private BarChart<String, Number> weeklyBarChart;
    
    // Table
    @FXML private TableView<TransactionRow> recentTransactionsTable;

    // Dashboard dynamic labels
    @FXML private Label lblTotalUsersValue;
    @FXML private Label lblActiveAccountsValue;
    @FXML private Label lblTotalDepositsValue;
    @FXML private Label lblTransactionsValue;
    @FXML private Label lblQuickAccountsSub;
    @FXML private Label lblQuickTransactionsSub;
    @FXML private Label lblQuickCreditsSub;
    @FXML private Label lblQuickCashbackSub;
    @FXML private Label lblPendingCount;
    @FXML private Label lblPendingItem1Title;
    @FXML private Label lblPendingItem1Desc;
    @FXML private Label lblPendingItem1Time;
    @FXML private Label lblPendingItem2Title;
    @FXML private Label lblPendingItem2Desc;
    @FXML private Label lblPendingItem2Time;
    @FXML private Label lblPendingItem3Title;
    @FXML private Label lblPendingItem3Desc;
    @FXML private Label lblPendingItem3Time;
    @FXML private Label lblPendingItem4Title;
    @FXML private Label lblPendingItem4Desc;
    @FXML private Label lblPendingItem4Time;
    @FXML private Label lblPendingItem5Title;
    @FXML private Label lblPendingItem5Desc;
    @FXML private Label lblPendingItem5Time;
    
    // Sidebar Navigation Buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnGestionUser;
    @FXML private Button btnAdminAccount;
    @FXML private Button btnGestionCompte;
    @FXML private Button btnGestionTransaction;
    @FXML private Button btnGestionCredit;
    @FXML private Button btnGestionCashback;

    // =============================================
    // STATE VARIABLES
    // =============================================
    
    private boolean sidebarCollapsed = false;
    private Button currentActiveButton;
    private boolean isDarkTheme = false;
    private ScheduledExecutorService clockExecutor;
    private Timeline pulseAnimation;
    private Timeline notificationPulse;
    private Timeline notificationRefreshTimeline;
    private StackPane activePill;
    private TranslateTransition pillTransition;
    private static final double PILL_INSET_X = 12;
    private static final Duration PILL_ANIM_DURATION = Duration.millis(250);
    private static final Interpolator PILL_EASE = Interpolator.SPLINE(0.2, 0.8, 0.2, 1.0);
    private static final double SIDEBAR_EXPANDED_WIDTH = 280;
    private static final double SIDEBAR_COLLAPSED_WIDTH = 80;
    
    // Animation Timelines
    private final Map<Node, Timeline> nodeAnimations = new HashMap<>();
    private final NotificationService notificationService = new NotificationService();
    private final PendingCompteNotificationService pendingCompteNotificationService = new PendingCompteNotificationService();
    private final UserService userService = new UserService();
    private final CompteBancaireService compteBancaireService = new CompteBancaireService();
    private final TransactionService transactionService = new TransactionService();
    private final CreditService creditService = new CreditService();
    private final CashbackService cashbackService = new CashbackService();
    private List<User> dashboardUsers = List.of();
    private List<CompteBancaire> dashboardAccounts = List.of();
    private List<Transaction> dashboardTransactions = List.of();
    private List<Credit> dashboardCredits = List.of();
    private List<Cashback> dashboardCashbacks = List.of();
    private Map<Integer, String> userNamesById = Map.of();
    private ContextMenu notificationsMenu;

    // =============================================
    // INITIALIZATION
    // =============================================
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentActiveButton = btnDashboard;
        
        // Initialize all components
        initializeAnimations();
        initializeSidebar();
        initializeSearch();
        initializeClock();
        refreshDashboardData();
        initializeResponsiveMode();
        startNotificationAutoRefresh();
        refreshNotificationBadge();
        
        // Start entrance animations + pill setup
        Platform.runLater(() -> {
            playEntranceAnimations();
            setupSidebarPill();
            moveActivePill(currentActiveButton, false);
            refreshNotificationBadge();
        });
    }

    // =============================================
    // ANIMATION SYSTEM
    // =============================================
    
    private void initializeAnimations() {
        // Create reusable pulse animation
        pulseAnimation = new Timeline();
        
        // Notification badge pulse
        notificationPulse = createPulseAnimation();
        notificationPulse.play();
    }
    
    private Timeline createPulseAnimation() {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO),
            new KeyFrame(Duration.seconds(1))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.setAutoReverse(true);
        return timeline;
    }
    
    private void playEntranceAnimations() {
        // Fade in sidebar
        if (sidebar != null) {
            FadeTransition fadeIn = new FadeTransition(Duration.millis(600), sidebar);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        }
        
        // Slide in top bar
        if (topBar != null) {
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(500), topBar);
            slideIn.setFromY(-50);
            slideIn.setToY(0);
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), topBar);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            
            ParallelTransition parallel = new ParallelTransition(slideIn, fadeIn);
            parallel.play();
        }
        
        // Animate dashboard content with stagger effect
        if (dashboardContent != null) {
            animateChildrenWithStagger(dashboardContent, 100);
        }
    }
    
    private void animateChildrenWithStagger(VBox container, long delayBetween) {
        ObservableList<Node> children = container.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            child.setOpacity(0);
            child.setTranslateY(30);
            
            FadeTransition fade = new FadeTransition(Duration.millis(400), child);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(i * delayBetween));
            
            TranslateTransition translate = new TranslateTransition(Duration.millis(400), child);
            translate.setFromY(30);
            translate.setToY(0);
            translate.setDelay(Duration.millis(i * delayBetween));
            translate.setInterpolator(Interpolator.EASE_OUT);
            
            ParallelTransition parallel = new ParallelTransition(fade, translate);
            parallel.play();
        }
    }
    
    private void animateCardHover(Node card, boolean entering) {
        if (entering) {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
            scale.setToX(1.02);
            scale.setToY(1.02);
            scale.play();
            
            // Add glow effect
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 180, 160, 0.3));
            shadow.setRadius(20);
            shadow.setSpread(0.1);
            card.setEffect(shadow);
        } else {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
            
            // Remove glow
            card.setEffect(null);
        }
    }
    
    private void animateButtonClick(Button button) {
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(100), button);
        scaleDown.setToX(0.95);
        scaleDown.setToY(0.95);
        
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), button);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);
        
        SequentialTransition sequence = new SequentialTransition(scaleDown, scaleUp);
        sequence.play();
    }
    
    private void createRippleEffect(Node node, MouseEvent event) {
        // Create ripple circle
        Circle ripple = new Circle(0);
        ripple.setFill(Color.rgb(0, 180, 160, 0.3));
        ripple.setMouseTransparent(true);
        
        // Position at click point
        if (node instanceof Pane) {
            Pane pane = (Pane) node;
            ripple.setCenterX(event.getX());
            ripple.setCenterY(event.getY());
            
            // Clip to parent bounds
            Rectangle clip = new Rectangle(pane.getWidth(), pane.getHeight());
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            pane.setClip(clip);
            
            pane.getChildren().add(ripple);
            
            // Animate ripple
            Timeline rippleAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(ripple.radiusProperty(), 0),
                    new KeyValue(ripple.opacityProperty(), 0.5)
                ),
                new KeyFrame(Duration.millis(400),
                    new KeyValue(ripple.radiusProperty(), Math.max(pane.getWidth(), pane.getHeight())),
                    new KeyValue(ripple.opacityProperty(), 0)
                )
            );
            
            rippleAnim.setOnFinished(e -> pane.getChildren().remove(ripple));
            rippleAnim.play();
        }
    }

    // =============================================
    // SIDEBAR FUNCTIONALITY
    // =============================================
    
    private void initializeSidebar() {
        // Add hover effects to sidebar buttons
        addSidebarButtonEffects(btnDashboard);
        addSidebarButtonEffects(btnGestionUser);
        addSidebarButtonEffects(btnAdminAccount);
        addSidebarButtonEffects(btnGestionCompte);
        addSidebarButtonEffects(btnGestionTransaction);
        addSidebarButtonEffects(btnGestionCredit);
        addSidebarButtonEffects(btnGestionCashback);

        if (sidebarScroll != null) {
            sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sidebarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        }

        sidebarCollapsed = isSidebarCollapsedByLayout();
        applySidebarState(false);
    }

    private boolean isSidebarCollapsedByLayout() {
        if (sidebar == null) {
            return false;
        }
        return sidebar.getStyleClass().contains("nx-sidebar-collapsed")
                || sidebar.getPrefWidth() <= SIDEBAR_COLLAPSED_WIDTH + 1;
    }

    private void setupSidebarPill() {
        if (sidebarNav == null || activePill != null) return;
        activePill = new StackPane();
        activePill.getStyleClass().add("nx-sidebar-pill");
        activePill.setManaged(false);
        activePill.setMouseTransparent(true);
        activePill.setVisible(!sidebarCollapsed);
        sidebarNav.getChildren().add(0, activePill);
        sidebarNav.widthProperty().addListener((obs, oldW, newW) -> updatePillWidth());
        updatePillWidth();
    }

    private void updatePillWidth() {
        if (activePill == null || sidebarNav == null) return;
        double width = Math.max(0, sidebarNav.getWidth() - (PILL_INSET_X * 2));
        activePill.setPrefWidth(width);
        activePill.setMinWidth(width);
        activePill.setMaxWidth(width);
        activePill.setLayoutX(PILL_INSET_X);
    }

    private void moveActivePill(Button target, boolean animate) {
        if (activePill == null || sidebarNav == null || target == null) return;
        Platform.runLater(() -> {
            Bounds bounds = target.getBoundsInParent();
            double targetY = bounds.getMinY();
            double targetH = bounds.getHeight();
            activePill.setPrefHeight(targetH);
            activePill.setMinHeight(targetH);
            activePill.setMaxHeight(targetH);

            if (pillTransition != null) {
                pillTransition.stop();
            }

            if (!animate) {
                activePill.setTranslateY(targetY);
                return;
            }

            pillTransition = new TranslateTransition(PILL_ANIM_DURATION, activePill);
            pillTransition.setToY(targetY);
            pillTransition.setInterpolator(PILL_EASE);
            pillTransition.play();
        });
    }

    private void fadeEmphasis(Button button, double targetOpacity) {
        if (button == null) return;
        button.lookupAll(".label, .ikonli-font-icon").forEach(node -> {
            FadeTransition fade = new FadeTransition(Duration.millis(180), node);
            fade.setToValue(targetOpacity);
            fade.setInterpolator(Interpolator.EASE_OUT);
            fade.play();
        });
    }
    
    private void addSidebarButtonEffects(Button button) {
        if (button == null) return;
        button.setOnMouseEntered(e -> { });
        button.setOnMouseExited(e -> { });
    }
    
    @FXML
    private void toggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        applySidebarState(true);
    }

    private void applySidebarState(boolean animateWidth) {
        if (sidebar == null) {
            return;
        }

        double targetWidth = sidebarCollapsed ? SIDEBAR_COLLAPSED_WIDTH : SIDEBAR_EXPANDED_WIDTH;

        if (animateWidth) {
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(300),
                    new KeyValue(sidebar.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                    new KeyValue(sidebar.minWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                    new KeyValue(sidebar.maxWidthProperty(), targetWidth, Interpolator.EASE_BOTH)
                )
            );
            timeline.play();
        } else {
            sidebar.setPrefWidth(targetWidth);
            sidebar.setMinWidth(targetWidth);
            sidebar.setMaxWidth(targetWidth);
        }

        if (sidebarCollapsed) {
            if (!sidebar.getStyleClass().contains("nx-sidebar-collapsed")) {
                sidebar.getStyleClass().add("nx-sidebar-collapsed");
            }
            hideSidebarText();
        } else {
            sidebar.getStyleClass().remove("nx-sidebar-collapsed");
            showSidebarText();
        }

        if (animateWidth && toggleSidebarBtn != null && toggleSidebarBtn.getGraphic() != null) {
            RotateTransition rotate = new RotateTransition(Duration.millis(300), toggleSidebarBtn.getGraphic());
            rotate.setByAngle(sidebarCollapsed ? 180 : -180);
            rotate.play();
        }

        if (activePill != null) {
            activePill.setVisible(!sidebarCollapsed);
            if (!sidebarCollapsed) {
                updatePillWidth();
                moveActivePill(currentActiveButton, false);
            }
        }
    }

    private void hideSidebarText() {
        applySidebarContentVisibility(false);
    }

    private void showSidebarText() {
        applySidebarContentVisibility(true);
    }

    private void applySidebarContentVisibility(boolean expanded) {
        setVisibleAndManaged(sidebarLogoContainer, expanded);
        setVisibleAndManaged(sidebarBrandText, expanded);
        setVisibleAndManaged(sidebarHeaderSpacer, expanded);
        setVisibleAndManaged(sidebarFooter, expanded);
        setVisibleAndManaged(sidebarUserInfo, expanded);

        if (sidebarHeader != null) {
            sidebarHeader.setAlignment(expanded ? Pos.CENTER_LEFT : Pos.CENTER);
        }

        if (sidebar != null) {
            sidebar.lookupAll(".nx-sidebar-text, .nx-sidebar-subtext, .nx-sidebar-section-title")
                .forEach(node -> setVisibleAndManaged(node, expanded));
        }

        for (Button button : getSidebarButtons()) {
            if (button == null) {
                continue;
            }
            button.setAlignment(expanded ? Pos.CENTER_LEFT : Pos.CENTER);
        }
    }

    private List<Button> getSidebarButtons() {
        return Arrays.asList(
            btnDashboard,
            btnGestionUser,
            btnAdminAccount,
            btnGestionCompte,
            btnGestionTransaction,
            btnGestionCredit,
            btnGestionCashback
        );
    }

    private void setVisibleAndManaged(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setManaged(visible);
        node.setVisible(visible);
    }

    // =============================================
    // SEARCH FUNCTIONALITY
    // =============================================
    
    private void initializeSearch() {
        if (searchField == null) return;
        
        // Add focus animation
        searchField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            Node wrapper = searchField.getParent();
            if (wrapper != null) {
                if (isFocused) {
                    wrapper.getStyleClass().add("nx-search-focused");
                    ScaleTransition scale = new ScaleTransition(Duration.millis(200), wrapper);
                    scale.setToX(1.02);
                    scale.setToY(1.02);
                    scale.play();
                } else {
                    wrapper.getStyleClass().remove("nx-search-focused");
                    ScaleTransition scale = new ScaleTransition(Duration.millis(200), wrapper);
                    scale.setToX(1.0);
                    scale.setToY(1.0);
                    scale.play();
                }
            }
        });
        
        // Add search listener
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Implement search functionality
            performSearch(newVal);
        });
    }
    
    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            // Clear search results
            return;
        }
        
        // Implement search logic here
        System.out.println("Searching for: " + query);
    }

    // =============================================
    // CLOCK / DATE-TIME
    // =============================================
    
    private void initializeClock() {
        // Update immediately
        updateDateTime();
        
        // Schedule periodic updates
        clockExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("Clock-Updater");
            return t;
        });
        
        clockExecutor.scheduleAtFixedRate(() -> {
            Platform.runLater(this::updateDateTime);
        }, 0, 1, TimeUnit.SECONDS);
    }
    
    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        
        if (currentDate != null) {
            String dateStr = now.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH));
            if (!dateStr.equals(currentDate.getText())) {
                currentDate.setText(dateStr);
                // Animate date change
                FadeTransition fade = new FadeTransition(Duration.millis(300), currentDate);
                fade.setFromValue(0.5);
                fade.setToValue(1);
                fade.play();
            }
        }
        
        if (currentTime != null) {
            String timeStr = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            currentTime.setText(timeStr);
            
            // Pulse effect on seconds change
            ScaleTransition pulse = new ScaleTransition(Duration.millis(100), currentTime);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.02);
            pulse.setToY(1.02);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(2);
            pulse.play();
        }
    }
    
    public void shutdown() {
        if (clockExecutor != null && !clockExecutor.isShutdown()) {
            clockExecutor.shutdown();
        }
        if (notificationRefreshTimeline != null) {
            notificationRefreshTimeline.stop();
        }
        nodeAnimations.values().forEach(Timeline::stop);
    }

    // =============================================
    // DASHBOARD DATA INITIALIZATION
    // =============================================

    private void refreshDashboardData() {
        loadDashboardData();
        updateDashboardSummary();
        initializeCharts();
        initializeTable();
        updatePendingServicesSummary();
    }

    private void loadDashboardData() {
        dashboardUsers = safeFetchList(userService::getAllUsers);
        dashboardAccounts = safeFetchList(compteBancaireService::getAll);
        dashboardTransactions = safeFetchList(transactionService::getAll);
        dashboardCredits = safeFetchList(creditService::getAll);
        dashboardCashbacks = safeFetchList(cashbackService::getAllCashbacks);

        userNamesById = dashboardUsers.stream()
            .collect(Collectors.toMap(User::getIdUser, this::buildUserDisplayName, (left, right) -> left));
    }

    private void updateDashboardSummary() {
        long totalUsers = dashboardUsers.size();
        long activeAccounts = dashboardAccounts.stream().filter(this::isActiveAccount).count();
        double totalDeposits = dashboardAccounts.stream().mapToDouble(CompteBancaire::getSolde).sum();
        long totalTransactions = dashboardTransactions.size();

        setLabelText(lblTotalUsersValue, formatCount(totalUsers));
        setLabelText(lblActiveAccountsValue, formatCount(activeAccounts));
        setLabelText(lblTotalDepositsValue, formatAmount(totalDeposits));
        setLabelText(lblTransactionsValue, formatCount(totalTransactions));

        long transactionsThisMonth = dashboardTransactions.stream()
            .filter(t -> isInCurrentMonth(t.getDateTransaction()))
            .count();
        long creditsPending = dashboardCredits.stream()
            .filter(c -> isPendingLike(c.getStatut()))
            .count();
        double cashbackCredited = dashboardCashbacks.stream()
            .filter(c -> isCreditedCashback(c.getStatut()))
            .mapToDouble(Cashback::getMontantCashback)
            .sum();

        setLabelText(lblQuickAccountsSub, formatCount(activeAccounts) + " actifs");
        setLabelText(lblQuickTransactionsSub, formatCount(transactionsThisMonth) + " ce mois");
        setLabelText(lblQuickCreditsSub, formatCount(creditsPending) + " demandes en attente");
        setLabelText(lblQuickCashbackSub, formatAmountWithCurrency(cashbackCredited));
    }

    private void updatePendingServicesSummary() {
        long pendingUsers = dashboardUsers.stream().filter(user -> isPendingLike(user.getStatus())).count();
        int pendingAccountValidation = safeFetchInt(pendingCompteNotificationService::countPending);
        long pendingTransactions = dashboardTransactions.stream()
            .filter(tx -> isPendingLike(tx.getStatutTransaction()))
            .count();
        long pendingCredits = dashboardCredits.stream()
            .filter(credit -> isPendingLike(credit.getStatut()))
            .count();
        long pendingCashbacks = dashboardCashbacks.stream()
            .filter(cashback -> isPendingCashback(cashback.getStatut()))
            .count();

        long totalPending = pendingUsers + pendingAccountValidation + pendingTransactions + pendingCredits + pendingCashbacks;
        setLabelText(lblPendingCount, formatCount(totalPending));

        updatePendingItem(lblPendingItem1Title, lblPendingItem1Desc, lblPendingItem1Time,
            "Utilisateurs a valider",
            formatCount(pendingUsers) + " compte(s) en attente",
            "Service Utilisateurs");
        updatePendingItem(lblPendingItem2Title, lblPendingItem2Desc, lblPendingItem2Time,
            "Comptes bancaires a valider",
            formatCount(pendingAccountValidation) + " demande(s) de compte",
            "Service Compte Bancaire");
        updatePendingItem(lblPendingItem3Title, lblPendingItem3Desc, lblPendingItem3Time,
            "Transactions a traiter",
            formatCount(pendingTransactions) + " transaction(s) en attente",
            "Service Transactions");
        updatePendingItem(lblPendingItem4Title, lblPendingItem4Desc, lblPendingItem4Time,
            "Credits en attente",
            formatCount(pendingCredits) + " dossier(s) en attente",
            "Service Credit");
        updatePendingItem(lblPendingItem5Title, lblPendingItem5Desc, lblPendingItem5Time,
            "Cashback a confirmer",
            formatCount(pendingCashbacks) + " cashback(s) a valider",
            "Service Cashback");
    }

    private void updatePendingItem(Label title, Label desc, Label time, String titleValue, String descValue, String timeValue) {
        setLabelText(title, titleValue);
        setLabelText(desc, descValue);
        setLabelText(time, timeValue);
    }

    private <T> List<T> safeFetchList(Supplier<List<T>> supplier) {
        try {
            List<T> result = supplier.get();
            return result == null ? List.of() : result;
        } catch (Exception ex) {
            System.err.println("Dashboard data fetch failed: " + ex.getMessage());
            return List.of();
        }
    }

    private int safeFetchInt(Supplier<Integer> supplier) {
        try {
            Integer value = supplier.get();
            return value == null ? 0 : Math.max(value, 0);
        } catch (Exception ex) {
            System.err.println("Dashboard pending count fetch failed: " + ex.getMessage());
            return 0;
        }
    }

    private void setLabelText(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private String buildUserDisplayName(User user) {
        String firstName = user == null ? "" : safeLabel(user.getPrenom()).trim();
        String lastName = user == null ? "" : safeLabel(user.getNom()).trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? "User #" + (user == null ? "?" : user.getIdUser()) : fullName;
    }

    private boolean isActiveAccount(CompteBancaire account) {
        return normalized(account == null ? null : account.getStatutCompte()).contains("actif")
            || normalized(account == null ? null : account.getStatutCompte()).contains("active");
    }

    private boolean isPendingLike(String status) {
        String value = normalized(status);
        return value.contains("pending")
            || value.contains("attente")
            || value.contains("en cours")
            || value.contains("a valider")
            || value.contains("processing");
    }

    private boolean isCreditedCashback(String status) {
        String value = normalized(status);
        return value.contains("credite") || value.contains("credited");
    }

    private boolean isPendingCashback(String status) {
        String value = normalized(status);
        return value.contains("pending") || value.contains("attente") || value.contains("valide");
    }

    private boolean isInboundTransaction(String transactionType) {
        String value = normalized(transactionType);
        return value.contains("credit")
            || value.contains("depot")
            || value.contains("versement")
            || value.contains("inbound");
    }

    private boolean isInCurrentMonth(LocalDate date) {
        if (date == null) {
            return false;
        }
        LocalDate now = LocalDate.now();
        return date.getYear() == now.getYear() && date.getMonthValue() == now.getMonthValue();
    }

    private String normalized(String value) {
        if (value == null) {
            return "";
        }
        String clean = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return clean.toLowerCase(Locale.ROOT).replace('_', ' ').trim();
    }

    private String formatCount(long value) {
        return String.format(Locale.US, "%,d", Math.max(value, 0));
    }

    private String formatAmount(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    private String formatAmountWithCurrency(double value) {
        return formatAmount(value) + " DT";
    }

    private double safeAmount(Double value) {
        return value == null ? 0.0 : Math.max(value, 0.0);
    }

    private LocalDate safeDate(LocalDate value) {
        return value == null ? LocalDate.MIN : value;
    }

    private String formatTableDate(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH));
    }

    private String resolveTableClientName(int userId) {
        if (userId <= 0) {
            return "N/A";
        }
        return userNamesById.getOrDefault(userId, "User #" + userId);
    }

    // =============================================
    // CHARTS INITIALIZATION
    // =============================================
    
    private void initializeCharts() {
        initializeTransactionChart();
        initializePieChart();
        initializeBarChart();
    }
    
    private void initializeTransactionChart() {
        if (transactionChart == null) return;

        transactionChart.setLegendVisible(false);
        transactionChart.setAnimated(true);
        transactionChart.setCreateSymbols(true);

        String[] months = {"Jan", "Fev", "Mar", "Avr", "Mai", "Juin", "Juil", "Aout", "Sep", "Oct", "Nov", "Dec"};
        double[] inboundByMonth = new double[12];
        double[] outboundByMonth = new double[12];
        int currentYear = LocalDate.now().getYear();

        for (Transaction transaction : dashboardTransactions) {
            if (transaction == null || transaction.getDateTransaction() == null) {
                continue;
            }
            LocalDate txDate = transaction.getDateTransaction();
            if (txDate.getYear() != currentYear) {
                continue;
            }

            int monthIndex = txDate.getMonthValue() - 1;
            double amount = safeAmount(transaction.getMontant());
            if (isInboundTransaction(transaction.getTypeTransaction())) {
                inboundByMonth[monthIndex] += amount;
            } else {
                outboundByMonth[monthIndex] += amount;
            }
        }

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Entrees");
        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Sorties");

        for (int i = 0; i < months.length; i++) {
            incomeSeries.getData().add(new XYChart.Data<>(months[i], inboundByMonth[i]));
            expenseSeries.getData().add(new XYChart.Data<>(months[i], outboundByMonth[i]));
        }

        transactionChart.getData().clear();
        transactionChart.getData().addAll(incomeSeries, expenseSeries);

        // Style the chart series
        Platform.runLater(() -> {
            // Style income series (teal)
            Node incomeLine = transactionChart.lookup(".series0");
            if (incomeLine != null) {
                incomeLine.setStyle("-fx-stroke: #00B4A0; -fx-stroke-width: 3px;");
            }

            // Style expense series (gold)
            Node expenseLine = transactionChart.lookup(".series1");
            if (expenseLine != null) {
                expenseLine.setStyle("-fx-stroke: #F4C430; -fx-stroke-width: 3px;");
            }

            // Style area fills
            Set<Node> areaFills = transactionChart.lookupAll(".chart-series-area-fill");
            int index = 0;
            for (Node fill : areaFills) {
                if (index == 0) {
                    fill.setStyle("-fx-fill: linear-gradient(to bottom, rgba(0,180,160,0.4), rgba(0,180,160,0.05));");
                } else {
                    fill.setStyle("-fx-fill: linear-gradient(to bottom, rgba(244,196,48,0.3), rgba(244,196,48,0.05));");
                }
                index++;
            }

            // Add hover effects to data points
            for (XYChart.Series<String, Number> series : transactionChart.getData()) {
                for (XYChart.Data<String, Number> data : series.getData()) {
                    Node node = data.getNode();
                    if (node != null) {
                        Tooltip tooltip = new Tooltip(
                            data.getXValue() + ": " + String.format("%,.0f", data.getYValue().doubleValue()) + " DT"
                        );
                        tooltip.getStyleClass().add("nx-tooltip");
                        Tooltip.install(node, tooltip);

                        node.setOnMouseEntered(e -> {
                            node.setScaleX(1.5);
                            node.setScaleY(1.5);
                        });

                        node.setOnMouseExited(e -> {
                            node.setScaleX(1.0);
                            node.setScaleY(1.0);
                        });
                    }
                }
            }
        });
    }

    private void initializePieChart() {
        if (accountPieChart == null) return;

        accountPieChart.setClockwise(true);
        accountPieChart.setLabelsVisible(false);
        accountPieChart.setStartAngle(90);
        accountPieChart.setAnimated(true);

        Map<String, Long> typeCounts = new LinkedHashMap<>();
        typeCounts.put("Epargne", 0L);
        typeCounts.put("Courant", 0L);
        typeCounts.put("Professionnel", 0L);
        typeCounts.put("Autre", 0L);

        for (CompteBancaire account : dashboardAccounts) {
            String type = normalized(account == null ? null : account.getTypeCompte());
            if (type.contains("epargne")) {
                typeCounts.put("Epargne", typeCounts.get("Epargne") + 1);
            } else if (type.contains("courant")) {
                typeCounts.put("Courant", typeCounts.get("Courant") + 1);
            } else if (type.contains("professionnel") || type.contains("business")) {
                typeCounts.put("Professionnel", typeCounts.get("Professionnel") + 1);
            } else {
                typeCounts.put("Autre", typeCounts.get("Autre") + 1);
            }
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Long> entry : typeCounts.entrySet()) {
            if (entry.getValue() > 0) {
                pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }
        }

        if (pieData.isEmpty()) {
            pieData.add(new PieChart.Data("Aucun compte", 1));
        }

        accountPieChart.setData(pieData);

        // Style pie slices
        Platform.runLater(() -> {
            String[] colors = {"#00B4A0", "#0A2540", "#F4C430", "#8B5CF6", "#CBD5E1"};
            int i = 0;
            for (PieChart.Data data : pieData) {
                Node slice = data.getNode();
                if (slice != null) {
                    slice.setStyle("-fx-pie-color: " + colors[i % colors.length] + ";");

                    // Add hover effect
                    final int index = i;
                    slice.setOnMouseEntered(e -> {
                        slice.setScaleX(1.05);
                        slice.setScaleY(1.05);

                        DropShadow shadow = new DropShadow();
                        shadow.setColor(Color.web(colors[index % colors.length], 0.5));
                        shadow.setRadius(15);
                        slice.setEffect(shadow);
                    });

                    slice.setOnMouseExited(e -> {
                        slice.setScaleX(1.0);
                        slice.setScaleY(1.0);
                        slice.setEffect(null);
                    });

                    // Add tooltip
                    Tooltip tooltip = new Tooltip(
                        data.getName() + ": " + String.format("%.0f", data.getPieValue()) + " compte(s)"
                    );
                    tooltip.getStyleClass().add("nx-tooltip");
                    Tooltip.install(slice, tooltip);
                }
                i++;
            }
        });
    }

    private void initializeBarChart() {
        if (weeklyBarChart == null) return;

        weeklyBarChart.setAnimated(true);
        weeklyBarChart.setLegendVisible(false);
        weeklyBarChart.setCategoryGap(20);
        weeklyBarChart.setBarGap(4);

        String[] weekDays = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        int[] inboundByDay = new int[7];
        int[] outboundByDay = new int[7];

        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(6);

        for (Transaction transaction : dashboardTransactions) {
            if (transaction == null || transaction.getDateTransaction() == null) {
                continue;
            }

            LocalDate txDate = transaction.getDateTransaction();
            if (txDate.isBefore(startDay) || txDate.isAfter(today)) {
                continue;
            }

            int dayIndex = txDate.getDayOfWeek().getValue() - 1;
            if (isInboundTransaction(transaction.getTypeTransaction())) {
                inboundByDay[dayIndex]++;
            } else {
                outboundByDay[dayIndex]++;
            }
        }

        XYChart.Series<String, Number> inbound = new XYChart.Series<>();
        inbound.setName("Entrees");
        XYChart.Series<String, Number> outbound = new XYChart.Series<>();
        outbound.setName("Sorties");

        for (int i = 0; i < weekDays.length; i++) {
            inbound.getData().add(new XYChart.Data<>(weekDays[i], inboundByDay[i]));
            outbound.getData().add(new XYChart.Data<>(weekDays[i], outboundByDay[i]));
        }

        weeklyBarChart.getData().clear();
        weeklyBarChart.getData().addAll(inbound, outbound);

        // Style bars
        Platform.runLater(() -> {
            // Style inbound bars (teal)
            Set<Node> inboundBars = weeklyBarChart.lookupAll(".series0.chart-bar");
            for (Node bar : inboundBars) {
                bar.setStyle("-fx-bar-fill: linear-gradient(to top, #009485, #00B4A0);");
                addBarHoverEffect(bar);
            }

            // Style outbound bars (gold)
            Set<Node> outboundBars = weeklyBarChart.lookupAll(".series1.chart-bar");
            for (Node bar : outboundBars) {
                bar.setStyle("-fx-bar-fill: linear-gradient(to top, #D4A82A, #F4C430);");
                addBarHoverEffect(bar);
            }
        });
    }

    private void addBarHoverEffect(Node bar) {
        bar.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), bar);
            scale.setToY(1.05);
            scale.play();
            
            Glow glow = new Glow(0.3);
            bar.setEffect(glow);
        });
        
        bar.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), bar);
            scale.setToY(1.0);
            scale.play();
            
            bar.setEffect(null);
        });
    }

    // =============================================
    // TABLE INITIALIZATION
    // =============================================
    
    private void initializeTable() {
        if (recentTransactionsTable == null) return;

        recentTransactionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Create columns programmatically for better control
        TableColumn<TransactionRow, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> data.getValue().idProperty());
        colId.setPrefWidth(100);
        colId.setCellFactory(col -> createAnimatedCell());

        TableColumn<TransactionRow, String> colClient = new TableColumn<>("Client");
        colClient.setCellValueFactory(data -> data.getValue().clientProperty());
        colClient.setPrefWidth(180);
        colClient.setCellFactory(col -> createClientCell());

        TableColumn<TransactionRow, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(data -> data.getValue().typeProperty());
        colType.setPrefWidth(120);
        colType.setCellFactory(col -> createTypeCell());

        TableColumn<TransactionRow, String> colAmount = new TableColumn<>("Montant");
        colAmount.setCellValueFactory(data -> data.getValue().amountProperty());
        colAmount.setPrefWidth(130);
        colAmount.setCellFactory(col -> createAmountCell());

        TableColumn<TransactionRow, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(data -> data.getValue().dateProperty());
        colDate.setPrefWidth(140);
        colDate.setCellFactory(col -> createAnimatedCell());

        TableColumn<TransactionRow, String> colStatus = new TableColumn<>("Statut");
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());
        colStatus.setPrefWidth(120);
        colStatus.setCellFactory(col -> createStatusCell());

        TableColumn<TransactionRow, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(100);
        colActions.setCellFactory(col -> createActionsCell());

        recentTransactionsTable.getColumns().clear();
        recentTransactionsTable.getColumns().addAll(
            colId, colClient, colType, colAmount, colDate, colStatus, colActions
        );

        List<Transaction> sortedTransactions = new ArrayList<>(dashboardTransactions);
        sortedTransactions.sort(
            Comparator.comparing((Transaction tx) -> safeDate(tx.getDateTransaction())).reversed()
                .thenComparing(Comparator.comparingInt(Transaction::getIdTransaction).reversed())
        );

        ObservableList<TransactionRow> rows = FXCollections.observableArrayList();
        int limit = Math.min(sortedTransactions.size(), 8);
        for (int i = 0; i < limit; i++) {
            Transaction tx = sortedTransactions.get(i);
            String rowId = "TX-" + tx.getIdTransaction();
            String client = resolveTableClientName(tx.getIdUser());
            String type = safeLabel(tx.getTypeTransaction());
            String amount = formatAmountWithCurrency(safeAmount(tx.getMontant()));
            String date = formatTableDate(tx.getDateTransaction());
            String status = safeLabel(tx.getStatutTransaction());
            rows.add(new TransactionRow(rowId, client, type, amount, date, status));
        }

        recentTransactionsTable.setItems(rows);

        // Add row hover animation
        recentTransactionsTable.setRowFactory(tv -> {
            TableRow<TransactionRow> row = new TableRow<>();
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) {
                    row.setStyle("-fx-background-color: rgba(0, 180, 160, 0.08);");
                }
            });
            row.setOnMouseExited(e -> {
                row.setStyle("");
            });
            return row;
        });
    }

    private TableCell<TransactionRow, String> createAnimatedCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    
                    // Fade in animation
                    FadeTransition fade = new FadeTransition(Duration.millis(300), this);
                    fade.setFromValue(0);
                    fade.setToValue(1);
                    fade.play();
                }
            }
        };
    }
    
    private TableCell<TransactionRow, String> createClientCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox container = new HBox(10);
                    container.setAlignment(Pos.CENTER_LEFT);
                    
                    // Avatar
                    StackPane avatar = new StackPane();
                    avatar.getStyleClass().add("nx-table-avatar");
                    avatar.setMinSize(32, 32);
                    avatar.setMaxSize(32, 32);
                    
                    String initials = getInitials(item);
                    Label initialsLabel = new Label(initials);
                    initialsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
                    
                    // Random color based on name
                    String[] colors = {"#00B4A0", "#0A2540", "#8B5CF6", "#F59E0B", "#3B82F6"};
                    int colorIndex = Math.abs(item.hashCode()) % colors.length;
                    avatar.setStyle("-fx-background-color: " + colors[colorIndex] + "; -fx-background-radius: 16px;");
                    avatar.getChildren().add(initialsLabel);
                    
                    // Name
                    Label nameLabel = new Label(item);
                    nameLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #0A2540;");
                    
                    container.getChildren().addAll(avatar, nameLabel);
                    setGraphic(container);
                    setText(null);
                }
            }
            
            private String getInitials(String name) {
                String[] parts = name.split(" ");
                if (parts.length >= 2) {
                    return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
                }
                return name.substring(0, Math.min(2, name.length())).toUpperCase();
            }
        };
    }
    
    private TableCell<TransactionRow, String> createTypeCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox container = new HBox(8);
                    container.setAlignment(Pos.CENTER_LEFT);
                    
                    // Icon based on type
                    FontIcon icon = new FontIcon();
                    icon.setIconSize(14);
                    
                    String iconLiteral;
                    String color;
                    
                    String normalizedType = normalized(item);
                    if (normalizedType.contains("virement") || normalizedType.contains("transfer")) {
                        iconLiteral = "fas-exchange-alt";
                        color = "#8B5CF6";
                    } else if (normalizedType.contains("depot")
                        || normalizedType.contains("credit")
                        || normalizedType.contains("entree")) {
                        iconLiteral = "fas-arrow-down";
                        color = "#10B981";
                    } else if (normalizedType.contains("retrait")
                        || normalizedType.contains("debit")
                        || normalizedType.contains("sortie")) {
                        iconLiteral = "fas-arrow-up";
                        color = "#F59E0B";
                    } else if (normalizedType.contains("paiement") || normalizedType.contains("payment")) {
                        iconLiteral = "fas-credit-card";
                        color = "#3B82F6";
                    } else {
                        iconLiteral = "fas-circle";
                        color = "#6B7280";
                    }
                    
                    icon.setIconLiteral(iconLiteral);
                    icon.setIconColor(Color.web(color));
                    
                    Label typeLabel = new Label(item);
                    typeLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 500;");
                    
                    container.getChildren().addAll(icon, typeLabel);
                    setGraphic(container);
                    setText(null);
                }
            }
        };
    }
    
    private TableCell<TransactionRow, String> createAmountCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label amountLabel = new Label(item);
                    amountLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #0A2540; -fx-font-size: 13px;");
                    setGraphic(amountLabel);
                    setText(null);
                }
            }
        };
    }
    
    private TableCell<TransactionRow, String> createStatusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("nx-status-chip");
                    badge.setPadding(new Insets(4, 12, 4, 12));
                    
                    String value = normalized(item);
                    String bgColor, textColor;
                    FontIcon icon = new FontIcon();
                    icon.setIconSize(10);
                    
                    if (value.contains("succes")
                        || value.contains("valid")
                        || value.contains("terminee")
                        || value.contains("complete")
                        || value.contains("approuve")) {
                        bgColor = "rgba(16, 185, 129, 0.15)";
                        textColor = "#059669";
                        icon.setIconLiteral("fas-check-circle");
                        icon.setIconColor(Color.web("#059669"));
                    } else if (value.contains("attente")
                        || value.contains("en cours")
                        || value.contains("pending")
                        || value.contains("processing")) {
                        bgColor = "rgba(245, 158, 11, 0.15)";
                        textColor = "#D97706";
                        icon.setIconLiteral("fas-clock");
                        icon.setIconColor(Color.web("#D97706"));
                    } else {
                        bgColor = "rgba(239, 68, 68, 0.15)";
                        textColor = "#DC2626";
                        icon.setIconLiteral("fas-times-circle");
                        icon.setIconColor(Color.web("#DC2626"));
                    }
                    
                    badge.setStyle(
                        "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-background-radius: 20px;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: 600;"
                    );
                    
                    HBox container = new HBox(6);
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.getChildren().addAll(icon, badge);
                    
                    setGraphic(container);
                    setText(null);
                }
            }
        };
    }
    
    private TableCell<TransactionRow, Void> createActionsCell() {
        return new TableCell<>() {
            private final Button viewBtn = new Button();
            private final Button editBtn = new Button();
            
            {
                FontIcon viewIcon = new FontIcon("fas-eye");
                viewIcon.setIconSize(12);
                viewIcon.setIconColor(Color.web("#6B7280"));
                viewBtn.setGraphic(viewIcon);
                viewBtn.getStyleClass().addAll("nx-table-action-btn");
                viewBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6px;");
                
                FontIcon editIcon = new FontIcon("fas-edit");
                editIcon.setIconSize(12);
                editIcon.setIconColor(Color.web("#6B7280"));
                editBtn.setGraphic(editIcon);
                editBtn.getStyleClass().addAll("nx-table-action-btn");
                editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6px;");
                
                // Add hover effects
                viewBtn.setOnMouseEntered(e -> viewIcon.setIconColor(Color.web("#00B4A0")));
                viewBtn.setOnMouseExited(e -> viewIcon.setIconColor(Color.web("#6B7280")));
                editBtn.setOnMouseEntered(e -> editIcon.setIconColor(Color.web("#3B82F6")));
                editBtn.setOnMouseExited(e -> editIcon.setIconColor(Color.web("#6B7280")));
                
                viewBtn.setOnAction(e -> {
                    TransactionRow row = getTableView().getItems().get(getIndex());
                    showTransactionDetails(row);
                });
                
                editBtn.setOnAction(e -> {
                    TransactionRow row = getTableView().getItems().get(getIndex());
                    editTransaction(row);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox actions = new HBox(4);
                    actions.setAlignment(Pos.CENTER);
                    actions.getChildren().addAll(viewBtn, editBtn);
                    setGraphic(actions);
                }
            }
        };
    }
    
    private void showTransactionDetails(TransactionRow row) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la Transaction");
        alert.setHeaderText("Transaction: " + row.idProperty().get());
        alert.setContentText(
            "Client: " + row.clientProperty().get() + "\n" +
            "Type: " + row.typeProperty().get() + "\n" +
            "Montant: " + row.amountProperty().get() + "\n" +
            "Date: " + row.dateProperty().get() + "\n" +
            "Statut: " + row.statusProperty().get()
        );
        styleAlert(alert);
        alert.showAndWait();
    }
    
    private void editTransaction(TransactionRow row) {
        showInfoAlert("Modification", "Édition de la transaction " + row.idProperty().get());
    }

    // =============================================
    // NAVIGATION HANDLERS
    // =============================================
    
    private void setActiveButton(Button button) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("nx-sidebar-link-active");
            fadeEmphasis(currentActiveButton, 0.78);
        }
        
        button.getStyleClass().add("nx-sidebar-link-active");
        currentActiveButton = button;
        fadeEmphasis(button, 1.0);
        moveActivePill(button, true);
    }

    @FXML
    private void activateSidebarItem(ActionEvent event) {
        if (event == null || !(event.getSource() instanceof Button)) {
            return;
        }
        setActiveButton((Button) event.getSource());
    }
    
    @FXML
    private void showDashboard() {
        setActiveButton(btnDashboard);
        
        // Animate content transition
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), contentArea);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(dashboardContent);
            refreshDashboardData();
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentArea);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
            
            // Re-animate dashboard children
            animateChildrenWithStagger(dashboardContent, 50);
        });
        fadeOut.play();
    }
    
    @FXML
    private void showGestionUser() {
        setActiveButton(btnGestionUser);
        loadModulePage("Gestion Utilisateurs", 
            new String[]{"Utilisateurs"}, 
            new String[]{"User.fxml"});
    }

    @FXML
    private void showAdminAccountManagement() {
        setActiveButton(btnAdminAccount);
        loadModulePage(
            "Admin Account Management",
            new String[]{"Security Settings", "User Actions"},
            new String[]{"AdminSecuritySettingsView.fxml", "AdminUserActionsView.fxml"}
        );
    }
    
    @FXML
    private void showGestionCompte() {
        setActiveButton(btnGestionCompte);
        loadModulePage("Gestion Comptes Bancaires", 
            new String[]{"Compte Bancaire", "Coffre Virtuel"}, 
            new String[]{"CompteBancaire.fxml", "CoffreVirtuel.fxml"});
    }
    
    @FXML
    private void showGestionCoffre() {
        showGestionCompte();
    }
    
    @FXML
    private void showGestionTransaction() {
        setActiveButton(btnGestionTransaction);
        loadModulePage("Gestion des Transactions", 
            new String[]{"Transaction", "Reclamation"}, 
            new String[]{"Transaction.fxml", "Reclamation.fxml"});
    }
    
    @FXML
    private void showGestionCredit() {
        setActiveButton(btnGestionCredit);
        loadModulePage("Gestion des Crédits", 
            new String[]{"Crédit", "Garantie"}, 
            new String[]{"Credit.fxml", "GarantieCredit.fxml"});
    }
    
    @FXML
    private void showGestionCashback() {
        setActiveButton(btnGestionCashback);
        loadModulePage("Gestion Cashback", 
            new String[]{"Partenaire", "Cashback"}, 
            new String[]{"Partenaire.fxml", "Cashback.fxml"});
    }

    // =============================================
    // MODULE PAGE LOADING
    // =============================================
    
    private void loadModulePage(String title, String[] entities, String[] fxmlFiles) {
        VBox moduleContent = new VBox(24);
        moduleContent.getStyleClass().add("nx-module-content");
        moduleContent.setPadding(new Insets(0));
        
        // Page Header with animation
        HBox pageHeader = createEnhancedPageHeader(title);
        moduleContent.getChildren().add(pageHeader);
        
        // Entity Navigation Tabs
        HBox entityTabs = createEntityTabs(entities, fxmlFiles);
        moduleContent.getChildren().add(entityTabs);
        
        // Entity Content Container
        StackPane entityContent = new StackPane();
        entityContent.setId("entityContent");
        entityContent.getStyleClass().add("nx-entity-content");
        VBox.setVgrow(entityContent, Priority.ALWAYS);
        moduleContent.getChildren().add(entityContent);
        
        // Animated content transition
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), contentArea);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(moduleContent);
            
            // Load first entity
            if (fxmlFiles.length > 0) {
                loadEntityContent(fxmlFiles[0], entityContent);
            }
            
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentArea);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }
    
    private HBox createEnhancedPageHeader(String title) {
        HBox header = new HBox();
        header.getStyleClass().add("nx-page-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));
        
        // Icon
        FontIcon icon = new FontIcon("fas-folder-open");
        icon.setIconSize(28);
        icon.setIconColor(Color.web("#00B4A0"));
        
        StackPane iconContainer = new StackPane(icon);
        iconContainer.setStyle(
            "-fx-background-color: rgba(0, 180, 160, 0.1);" +
            "-fx-background-radius: 12px;" +
            "-fx-padding: 12px;"
        );
        
        VBox titleBox = new VBox(4);
        titleBox.setPadding(new Insets(0, 0, 0, 16));
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("nx-page-title");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #0A2540;");
        
        Label subtitleLabel = new Label("Gérer et superviser les données");
        subtitleLabel.getStyleClass().add("nx-page-subtitle");
        subtitleLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 14px;");
        
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Action buttons
        HBox actions = new HBox(12);
        
        Button exportBtn = new Button("Exporter");
        exportBtn.getStyleClass().addAll("nx-btn", "nx-btn-outline");
        FontIcon exportIcon = new FontIcon("fas-download");
        exportIcon.setIconSize(14);
        exportBtn.setGraphic(exportIcon);
        
        Button addBtn = new Button("Ajouter");
        addBtn.getStyleClass().addAll("nx-btn", "nx-btn-primary");
        FontIcon addIcon = new FontIcon("fas-plus");
        addIcon.setIconSize(14);
        addIcon.setIconColor(Color.WHITE);
        addBtn.setGraphic(addIcon);
        
        actions.getChildren().addAll(exportBtn, addBtn);
        
        header.getChildren().addAll(iconContainer, titleBox, spacer, actions);
        
        return header;
    }
    
    private HBox createEntityTabs(String[] entities, String[] fxmlFiles) {
        HBox tabContainer = new HBox(8);
        tabContainer.getStyleClass().add("nx-entity-tabs");
        tabContainer.setPadding(new Insets(0, 0, 20, 0));
        tabContainer.setStyle(
            "-fx-background-color: #F3F4F6;" +
            "-fx-background-radius: 12px;" +
            "-fx-padding: 6px;"
        );
        
        ToggleGroup toggleGroup = new ToggleGroup();
        
        for (int i = 0; i < entities.length; i++) {
            final int index = i;
            final String fxmlFile = fxmlFiles[i];
            
            ToggleButton tab = new ToggleButton(entities[i]);
            tab.setToggleGroup(toggleGroup);
            tab.getStyleClass().add("nx-entity-tab");
            
            // Style
            tab.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #6B7280;" +
                "-fx-font-weight: 600;" +
                "-fx-padding: 10px 20px;" +
                "-fx-background-radius: 8px;" +
                "-fx-cursor: hand;"
            );
            
            if (i == 0) {
                tab.setSelected(true);
                tab.setStyle(
                    "-fx-background-color: white;" +
                    "-fx-text-fill: #0A2540;" +
                    "-fx-font-weight: 700;" +
                    "-fx-padding: 10px 20px;" +
                    "-fx-background-radius: 8px;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);"
                );
            }
            
            tab.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    tab.setStyle(
                        "-fx-background-color: white;" +
                        "-fx-text-fill: #0A2540;" +
                        "-fx-font-weight: 700;" +
                        "-fx-padding: 10px 20px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);"
                    );
                    
                    // Load content
                    VBox moduleContent = (VBox) contentArea.getChildren().get(0);
                    StackPane entityContent = (StackPane) moduleContent.lookup("#entityContent");
                    if (entityContent != null) {
                        loadEntityContent(fxmlFile, entityContent);
                    }
                } else {
                    tab.setStyle(
                        "-fx-background-color: transparent;" +
                        "-fx-text-fill: #6B7280;" +
                        "-fx-font-weight: 600;" +
                        "-fx-padding: 10px 20px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-cursor: hand;"
                    );
                }
            });
            
            tabContainer.getChildren().add(tab);
        }
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        tabContainer.getChildren().add(spacer);
        
        // Statistics button
        Button statsBtn = new Button("Statistiques");
        statsBtn.getStyleClass().addAll("nx-btn", "nx-btn-gold");
        FontIcon chartIcon = new FontIcon("fas-chart-pie");
        chartIcon.setIconSize(14);
        statsBtn.setGraphic(chartIcon);
        statsBtn.setStyle(
            "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #F7D35C, #F4C430);" +
            "-fx-text-fill: #0A2540;" +
            "-fx-font-weight: 700;" +
            "-fx-padding: 10px 20px;" +
            "-fx-background-radius: 8px;" +
            "-fx-cursor: hand;"
        );
        StatisticsController.StatisticsContext statisticsContext = resolveStatisticsContext(fxmlFiles);
        statsBtn.setOnAction(e -> loadStatisticsView(statisticsContext));
        
        tabContainer.getChildren().add(statsBtn);
        
        return tabContainer;
    }
    
    private void loadEntityView(String fxmlFile, HBox buttonContainer, int activeIndex) {
        for (int i = 0; i < buttonContainer.getChildren().size() - 1; i++) {
            Node node = buttonContainer.getChildren().get(i);
            if (node instanceof Button) {
                Button btn = (Button) node;
                btn.getStyleClass().removeAll("nx-btn-primary", "nx-btn-outline");
                btn.getStyleClass().add(i == activeIndex ? "nx-btn-primary" : "nx-btn-outline");
            }
        }
        
        VBox moduleContent = (VBox) contentArea.getChildren().get(0);
        StackPane entityContent = (StackPane) moduleContent.lookup("#entityContent");
        if (entityContent != null) {
            loadEntityContent(fxmlFile, entityContent);
        }
    }
    
    private void loadEntityContent(String fxmlFile, StackPane container) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Node content = loader.load();
            normalizeStyleClasses(content);
            attachResponsiveStylesheet(content, fxmlFile);
            applyEntityLayoutTuning(content);
            
            // Animated transition
            FadeTransition fadeOut = new FadeTransition(Duration.millis(100), container);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                container.getChildren().clear();
                container.getChildren().add(content);
                
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), container);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            fadeOut.play();
            
        } catch (IOException e) {
            showErrorMessage("Erreur de chargement", "Impossible de charger: " + fxmlFile);
            e.printStackTrace();
        }
    }
    
    private StatisticsController.StatisticsContext resolveStatisticsContext(String[] fxmlFiles) {
        if (fxmlFiles == null) {
            return StatisticsController.StatisticsContext.GENERIC;
        }
        for (String fxmlFile : fxmlFiles) {
            if ("User.fxml".equalsIgnoreCase(fxmlFile)) {
                return StatisticsController.StatisticsContext.USER_MANAGEMENT;
            }
            if ("AdminSecuritySettingsView.fxml".equalsIgnoreCase(fxmlFile)) {
                return StatisticsController.StatisticsContext.ADMIN_SECURITY;
            }
        }
        return StatisticsController.StatisticsContext.GENERIC;
    }

    private void loadStatisticsView(StatisticsController.StatisticsContext context) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Statistics.fxml"));
            Node content = loader.load();
            StatisticsController statisticsController = loader.getController();
            if (statisticsController != null) {
                statisticsController.setContext(context);
            }
            normalizeStyleClasses(content);
            attachResponsiveStylesheet(content, "Statistics.fxml");
            applyEntityLayoutTuning(content);
            
            VBox moduleContent = (VBox) contentArea.getChildren().get(0);
            StackPane entityContent = (StackPane) moduleContent.lookup("#entityContent");
            
            if (entityContent != null) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(100), entityContent);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> {
                    entityContent.getChildren().clear();
                    entityContent.getChildren().add(content);
                    
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(200), entityContent);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();
                });
                fadeOut.play();
            }
        } catch (IOException e) {
            showErrorMessage("Erreur", "Impossible de charger les statistiques");
            e.printStackTrace();
        }
    }

    private void initializeResponsiveMode() {
        Platform.runLater(() -> {
            if (contentArea == null || contentArea.getScene() == null) {
                return;
            }

            Scene scene = contentArea.getScene();
            String responsiveUrl = getClass().getResource(RESPONSIVE_CSS).toExternalForm();

            if (!scene.getStylesheets().contains(responsiveUrl)) {
                scene.getStylesheets().add(responsiveUrl);
            }

            Parent root = scene.getRoot();
            normalizeStyleClasses(root);
            applyCompactClass(root, scene.getWidth());
            scene.widthProperty().addListener((obs, oldVal, newVal) -> {
                normalizeStyleClasses(root);
                applyCompactClass(root, newVal.doubleValue());
            });
        });
    }

    private void attachResponsiveStylesheet(Node content, String fxmlFile) {
        if (!(content instanceof Parent parent)) {
            return;
        }

        if (shouldSkipResponsiveStylesheet(fxmlFile)) {
            return;
        }

        String responsiveUrl = getClass().getResource(RESPONSIVE_CSS).toExternalForm();
        if (!parent.getStylesheets().contains(responsiveUrl)) {
            parent.getStylesheets().add(responsiveUrl);
        }
    }

    private boolean shouldSkipResponsiveStylesheet(String fxmlFile) {
        if (fxmlFile == null) {
            return false;
        }
        return "User.fxml".equalsIgnoreCase(fxmlFile)
            || "AdminSecuritySettingsView.fxml".equalsIgnoreCase(fxmlFile)
            || "AdminUserActionsView.fxml".equalsIgnoreCase(fxmlFile);
    }

    private void applyCompactClass(Parent root, double width) {
        boolean compact = width <= 1420;
        if (compact) {
            if (!root.getStyleClass().contains(COMPACT_CLASS)) {
                root.getStyleClass().add(COMPACT_CLASS);
            }
        } else {
            root.getStyleClass().remove(COMPACT_CLASS);
        }
    }

    private void applyEntityLayoutTuning(Node content) {
        if (!(content instanceof Parent parent) || contentArea == null || contentArea.getScene() == null) {
            return;
        }

        double sceneWidth = contentArea.getScene().getWidth();
        boolean compact = sceneWidth <= 1420;
        double targetFormWidth = sceneWidth <= 1200 ? 380 : (compact ? 430 : 470);

        for (Node node : parent.lookupAll(".nx-card")) {
            if (node instanceof Region region) {
                double prefWidth = region.getPrefWidth();
                if (prefWidth >= 360 && prefWidth <= 460) {
                    region.setPrefWidth(targetFormWidth);
                }
            }
        }

        for (Node node : parent.lookupAll(".nx-table")) {
            if (node instanceof TableView<?> tableView) {
                tableView.setFixedCellSize(compact ? 36 : 40);
                tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            }
        }
    }

    private void normalizeStyleClasses(Node node) {
        if (node == null) {
            return;
        }

        ObservableList<String> classes = node.getStyleClass();
        if (!classes.isEmpty()) {
            List<String> normalized = new ArrayList<>();
            for (String raw : classes) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String[] parts = raw.split("[,\\s]+");
                for (String part : parts) {
                    String cleaned = part.trim().replace(",", "");
                    if (!cleaned.isBlank() && !normalized.contains(cleaned)) {
                        normalized.add(cleaned);
                    }
                }
            }
            if (!normalized.equals(new ArrayList<>(classes))) {
                classes.setAll(normalized);
            }
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                normalizeStyleClasses(child);
            }
        }
    }

    // =============================================
    // TOP BAR ACTIONS
    // =============================================
    
    @FXML
    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        
        // Animate theme transition
        FadeTransition fade = new FadeTransition(Duration.millis(300), contentArea.getScene().getRoot());
        fade.setFromValue(1);
        fade.setToValue(0.8);
        fade.setAutoReverse(true);
        fade.setCycleCount(2);
        fade.play();
        
        if (isDarkTheme) {
            contentArea.getScene().getRoot().getStyleClass().add("dark-theme");
        } else {
            contentArea.getScene().getRoot().getStyleClass().remove("dark-theme");
        }
        
        showInfoAlert("Thème", isDarkTheme ? "Mode sombre activé" : "Mode clair activé");
    }
    
    @FXML
    private void openEmail() {
        showNotificationPopup("Messages", "Vous avez 3 nouveaux messages", "fas-envelope");
    }
    
    @FXML
    private void openHistory() {
        showNotificationPopup("Historique", "Affichage de l'historique des actions", "fas-history");
    }
    
    @FXML
    private void openNotifications(ActionEvent event) {
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null) {
            showInfoAlert("Notifications", "Session utilisateur introuvable.");
            return;
        }

        try {
            List<Notification> notifications = notificationService.getRecentNotificationsFor(currentUser, 12);
            showNotificationsPanel(event, notifications);
            notificationService.markAllAsRead(currentUser);
            refreshNotificationBadge();
        } catch (Exception ex) {
            showErrorMessage("Notifications", "Impossible de charger les notifications.");
        }
    }
    
    @FXML
    private void openSettings() {
        showInfoAlert("Paramètres", "Panneau de configuration en cours de développement");
    }
    
    @FXML
    private void showProfile() {
        showInfoAlert("Profil", "Profil utilisateur: Admin\nRôle: Administrateur\nDernier accès: Aujourd'hui");
    }
    
    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText("Voulez-vous vraiment vous déconnecter?");
        alert.setContentText("Votre session sera terminée.");
        styleAlert(alert);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            shutdown();
            AuthSession.clear();
            SceneRouter.show("/fxml/Home.fxml", "NEXORA BANK - Welcome", 1200, 760, 980, 680);
        }
    }
    
    @FXML
    private void closeAlert(ActionEvent event) {
        if (event == null || event.getSource() == null) return;
        
        Node source = (Node) event.getSource();
        Node parent = source.getParent();
        
        if (parent != null) {
            // Animate close
            FadeTransition fade = new FadeTransition(Duration.millis(200), parent);
            fade.setFromValue(1);
            fade.setToValue(0);
            
            TranslateTransition slide = new TranslateTransition(Duration.millis(200), parent);
            slide.setToX(50);
            
            ParallelTransition parallel = new ParallelTransition(fade, slide);
            parallel.setOnFinished(e -> {
                parent.setVisible(false);
                parent.setManaged(false);
            });
            parallel.play();
        }
    }
    
    @FXML
    private void addNewEntity(MouseEvent event) {
        // Create ripple effect
        if (event.getSource() instanceof Node) {
            createRippleEffect((Node) event.getSource(), event);
        }
        showInfoAlert("Nouveau", "Sélectionnez le type d'entité à créer");
    }

    // =============================================
    // UTILITY METHODS
    // =============================================

    private void refreshNotificationBadge() {
        if (lblAdminNotificationCount == null) {
            return;
        }

        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null) {
            lblAdminNotificationCount.setText("0");
            lblAdminNotificationCount.setVisible(true);
            lblAdminNotificationCount.setManaged(true);
            return;
        }

        try {
            int unreadCount = notificationService.countUnreadFor(currentUser);
            int pendingCount = 0;
            if ("ROLE_ADMIN".equalsIgnoreCase(safeLabel(currentUser.getRole()))) {
                pendingCount = pendingCompteNotificationService.countPending();
            }
            int totalCount = Math.max(unreadCount, 0) + Math.max(pendingCount, 0);
            lblAdminNotificationCount.setText(formatNotificationCount(totalCount));
            lblAdminNotificationCount.setVisible(true);
            lblAdminNotificationCount.setManaged(true);
        } catch (Exception ex) {
            lblAdminNotificationCount.setText("0");
            lblAdminNotificationCount.setVisible(true);
            lblAdminNotificationCount.setManaged(true);
        }
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

    private String safeLabel(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
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
        panel.setPrefWidth(380);
        panel.setMaxWidth(380);
        panel.setStyle(
            "-fx-background-color: #ffffff;" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: #dbe3ef;" +
            "-fx-border-radius: 14;" +
            "-fx-padding: 12;"
        );

        Label title = new Label("Notifications");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #0A2540;");
        panel.getChildren().add(title);

        User currentUser = AuthSession.getCurrentUser();
        boolean isAdmin = currentUser != null && "ROLE_ADMIN".equalsIgnoreCase(safeLabel(currentUser.getRole()));
        int pendingValidationCount = isAdmin ? pendingCompteNotificationService.countPending() : 0;

        if (isAdmin) {
            HBox pendingBox = new HBox(10);
            pendingBox.setAlignment(Pos.CENTER_LEFT);
            pendingBox.setStyle(
                "-fx-background-color: #fff8e6;" +
                "-fx-border-color: #fde68a;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 8 10;"
            );

            Label pendingLabel = new Label("Pending account validations: " + pendingValidationCount);
            pendingLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #92400e;");

            Region pendingSpacer = new Region();
            HBox.setHgrow(pendingSpacer, Priority.ALWAYS);

            Button openPanelButton = new Button("Open");
            openPanelButton.setStyle(
                "-fx-background-color: #f59e0b;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: 700;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 4 10;" +
                "-fx-cursor: hand;"
            );
            openPanelButton.setDisable(pendingValidationCount <= 0);
            openPanelButton.setOnAction(e -> {
                if (notificationsMenu != null) {
                    notificationsMenu.hide();
                }
                Window owner = anchor.getScene() == null ? null : anchor.getScene().getWindow();
                new NotificationPanelController().show(owner, this::refreshNotificationBadge);
            });

            pendingBox.getChildren().addAll(pendingLabel, pendingSpacer, openPanelButton);
            panel.getChildren().add(pendingBox);
        }

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
                    "-fx-border-radius: 10;" +
                    "-fx-cursor: hand;"
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
                row.setOnMouseClicked(e -> handleNotificationClick(notification));
                listContainer.getChildren().add(row);
            }
        }

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(320);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        panel.getChildren().add(scrollPane);

        notificationsMenu = new ContextMenu();
        CustomMenuItem customItem = new CustomMenuItem(panel, false);
        notificationsMenu.getItems().setAll(customItem);
        notificationsMenu.setAutoHide(true);
        notificationsMenu.show(anchor, Side.BOTTOM, -320, 8);
    }

    private void handleNotificationClick(Notification notification) {
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null || !"ROLE_ADMIN".equalsIgnoreCase(safeLabel(currentUser.getRole()))) {
            return;
        }
        if (notification == null) {
            return;
        }

        if (notificationsMenu != null) {
            notificationsMenu.hide();
        }

        if (isCashbackNotification(notification)) {
            showGestionCashback();
            return;
        }

        if (notification.getRelatedUserId() != null && notification.getRelatedUserId() > 0) {
            AuthSession.setPendingUserManagementTargetId(notification.getRelatedUserId());
            showGestionUser();
        }
    }

    private boolean isCashbackNotification(Notification notification) {
        if (notification == null) {
            return false;
        }
        String type = notification.getType();
        if (type == null) {
            return false;
        }
        return type.trim().toUpperCase(Locale.ROOT).startsWith("CASHBACK");
    }
    
    private void showNotificationPopup(String title, String message, String iconLiteral) {
        // Create popup notification
        VBox popup = new VBox(12);
        popup.getStyleClass().add("nx-notification-popup");
        popup.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16px;" +
            "-fx-padding: 20px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 8);" +
            "-fx-min-width: 300px;"
        );
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(24);
        icon.setIconColor(Color.web("#00B4A0"));
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0A2540;");
        
        header.getChildren().addAll(icon, titleLabel);
        
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        messageLabel.setWrapText(true);
        
        popup.getChildren().addAll(header, messageLabel);
        
        // Show as alert for now (could be replaced with actual popup)
        showInfoAlert(title, message);
    }
    
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        Platform.runLater(alert::show);
    }
    
    private void showErrorMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        Platform.runLater(alert::show);
    }
    
    private void styleAlert(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16px;"
        );
        
        // Try to apply custom stylesheet
        try {
            dialogPane.getStylesheets().add(
                getClass().getResource("/css/MainView.css").toExternalForm()
            );
        } catch (Exception e) {
            // Ignore if stylesheet not found
        }
    }

    // =============================================
    // DATA MODEL
    // =============================================
    
    public static final class TransactionRow {
        private final StringProperty id;
        private final StringProperty client;
        private final StringProperty type;
        private final StringProperty amount;
        private final StringProperty date;
        private final StringProperty status;
        
        public TransactionRow(String id, String client, String type, String amount, String date, String status) {
            this.id = new SimpleStringProperty(id);
            this.client = new SimpleStringProperty(client);
            this.type = new SimpleStringProperty(type);
            this.amount = new SimpleStringProperty(amount);
            this.date = new SimpleStringProperty(date);
            this.status = new SimpleStringProperty(status);
        }
        
        public StringProperty idProperty() { return id; }
        public StringProperty clientProperty() { return client; }
        public StringProperty typeProperty() { return type; }
        public StringProperty amountProperty() { return amount; }
        public StringProperty dateProperty() { return date; }
        public StringProperty statusProperty() { return status; }
        
        public String getId() { return id.get(); }
        public String getClient() { return client.get(); }
        public String getType() { return type.get(); }
        public String getAmount() { return amount.get(); }
        public String getDate() { return date.get(); }
        public String getStatus() { return status.get(); }
    }
}
