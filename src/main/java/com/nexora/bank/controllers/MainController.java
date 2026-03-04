package com.nexora.bank.controllers;

import com.nexora.bank.SceneRouter;
import com.nexora.bank.AuthSession;
// ★ AJOUT 1/2 — import du panneau admin de validation
import com.nexora.bank.Service.NotificationService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainController implements Initializable {
    private static final String RESPONSIVE_CSS = "/css/Responsive.css";
    private static final String COMPACT_CLASS = "nx-compact";

    // =============================================
    // FXML INJECTED COMPONENTS
    // =============================================

    @FXML private VBox sidebar;
    @FXML private Button toggleSidebarBtn;
    @FXML private HBox topBar;
    @FXML private TextField searchField;
    @FXML private StackPane contentArea;
    @FXML private ScrollPane contentScrollPane;
    @FXML private VBox dashboardContent;
    @FXML private VBox sidebarNav;

    // DateTime Labels
    @FXML private Label currentDate;
    @FXML private Label currentTime;

    // Charts
    @FXML private AreaChart<String, Number> transactionChart;
    @FXML private PieChart accountPieChart;
    @FXML private BarChart<String, Number> weeklyBarChart;

    // Table
    @FXML private TableView<TransactionRow> recentTransactionsTable;

    // Sidebar Navigation Buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnGestionUser;
    @FXML private Button btnAdminAccount;
    @FXML private Button btnGestionCompte;
    @FXML private Button btnGestionTransaction;
    @FXML private Button btnGestionCredit;
    @FXML private Button btnGestionCashback;

    // ★ AJOUT 2/2 — badge dynamique de la cloche
    // Requiert fx:id="lblNotifBadge" dans MainView.fxml (voir commentaire en bas du fichier)
    @FXML private Label lblNotifBadge;

    // =============================================
    // STATE VARIABLES
    // =============================================

    private boolean sidebarCollapsed = false;
    private Button currentActiveButton;
    private boolean isDarkTheme = false;
    private ScheduledExecutorService clockExecutor;
    private Timeline pulseAnimation;
    private Timeline notificationPulse;
    private StackPane activePill;
    private TranslateTransition pillTransition;
    private static final double PILL_INSET_X = 12;
    private static final Duration PILL_ANIM_DURATION = Duration.millis(250);
    private static final Interpolator PILL_EASE = Interpolator.SPLINE(0.2, 0.8, 0.2, 1.0);

    private final Map<Node, Timeline> nodeAnimations = new HashMap<>();

    // =============================================
    // INITIALIZATION
    // =============================================

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentActiveButton = btnDashboard;

        initializeAnimations();
        initializeSidebar();
        initializeSearch();
        initializeClock();
        initializeCharts();
        initializeTable();
        initializeResponsiveMode();

        // ★ Charger le vrai compteur de comptes "En attente" dès l'ouverture
        refreshNotificationBadge();

        // ★ NOUVEAU — Polling toutes les 10 s pour maintenir le badge à jour
        //   sans intervention manuelle (refus admin côté BackOffice)
        Timeline badgePoller = new Timeline(
                new KeyFrame(Duration.seconds(10), e -> refreshNotificationBadge())
        );
        badgePoller.setCycleCount(Animation.INDEFINITE);
        badgePoller.play();

        Platform.runLater(() -> {
            playEntranceAnimations();
            setupSidebarPill();
            moveActivePill(currentActiveButton, false);
        });
    }

    // =============================================
    // ★ BADGE DE NOTIFICATION DYNAMIQUE
    // =============================================

    /**
     * Lit dans la BD le nombre de comptes dont statutCompte = "En attente"
     * et met à jour le Label badge de la cloche.
     *
     * Si lblNotifBadge est null (fx:id non encore ajouté dans MainView.fxml),
     * la méthode ne fait rien et ne lève pas d'exception.
     */
    public void refreshNotificationBadge() {
        if (lblNotifBadge == null) return;
        try {
            int count = new NotificationService().countPending();
            lblNotifBadge.setText(String.valueOf(count));
            lblNotifBadge.setVisible(count > 0);
            lblNotifBadge.setManaged(count > 0);
        } catch (Exception e) {
            System.err.println("[MainController] refreshNotificationBadge ERROR: " + e.getMessage());
        }
    }

    // =============================================
    // ANIMATION SYSTEM
    // =============================================

    private void initializeAnimations() {
        pulseAnimation = new Timeline();
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
        if (sidebar != null) {
            FadeTransition fadeIn = new FadeTransition(Duration.millis(600), sidebar);
            fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();
        }

        if (topBar != null) {
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(500), topBar);
            slideIn.setFromY(-50); slideIn.setToY(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), topBar);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);
            new ParallelTransition(slideIn, fadeIn).play();
        }

        if (dashboardContent != null) animateChildrenWithStagger(dashboardContent, 100);
    }

    private void animateChildrenWithStagger(VBox container, long delayBetween) {
        ObservableList<Node> children = container.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            child.setOpacity(0); child.setTranslateY(30);
            FadeTransition fade = new FadeTransition(Duration.millis(400), child);
            fade.setFromValue(0); fade.setToValue(1); fade.setDelay(Duration.millis(i * delayBetween));
            TranslateTransition translate = new TranslateTransition(Duration.millis(400), child);
            translate.setFromY(30); translate.setToY(0);
            translate.setDelay(Duration.millis(i * delayBetween));
            translate.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(fade, translate).play();
        }
    }

    private void animateCardHover(Node card, boolean entering) {
        if (entering) {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
            scale.setToX(1.02); scale.setToY(1.02); scale.play();
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 180, 160, 0.3)); shadow.setRadius(20); shadow.setSpread(0.1);
            card.setEffect(shadow);
        } else {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
            scale.setToX(1.0); scale.setToY(1.0); scale.play();
            card.setEffect(null);
        }
    }

    private void animateButtonClick(Button button) {
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(100), button);
        scaleDown.setToX(0.95); scaleDown.setToY(0.95);
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), button);
        scaleUp.setToX(1.0); scaleUp.setToY(1.0);
        new SequentialTransition(scaleDown, scaleUp).play();
    }

    private void createRippleEffect(Node node, MouseEvent event) {
        Circle ripple = new Circle(0);
        ripple.setFill(Color.rgb(0, 180, 160, 0.3));
        ripple.setMouseTransparent(true);
        if (node instanceof Pane) {
            Pane pane = (Pane) node;
            ripple.setCenterX(event.getX()); ripple.setCenterY(event.getY());
            Rectangle clip = new Rectangle(pane.getWidth(), pane.getHeight());
            clip.setArcWidth(20); clip.setArcHeight(20);
            pane.setClip(clip);
            pane.getChildren().add(ripple);
            Timeline rippleAnim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(ripple.radiusProperty(), 0),
                            new KeyValue(ripple.opacityProperty(), 0.5)),
                    new KeyFrame(Duration.millis(400),
                            new KeyValue(ripple.radiusProperty(), Math.max(pane.getWidth(), pane.getHeight())),
                            new KeyValue(ripple.opacityProperty(), 0))
            );
            rippleAnim.setOnFinished(e -> pane.getChildren().remove(ripple));
            rippleAnim.play();
        }
    }

    // =============================================
    // SIDEBAR FUNCTIONALITY
    // =============================================

    private void initializeSidebar() {
        addSidebarButtonEffects(btnDashboard);
        addSidebarButtonEffects(btnGestionUser);
        addSidebarButtonEffects(btnAdminAccount);
        addSidebarButtonEffects(btnGestionCompte);
        addSidebarButtonEffects(btnGestionTransaction);
        addSidebarButtonEffects(btnGestionCredit);
        addSidebarButtonEffects(btnGestionCashback);
    }

    private void setupSidebarPill() {
        if (sidebarNav == null || activePill != null) return;
        activePill = new StackPane();
        activePill.getStyleClass().add("nx-sidebar-pill");
        activePill.setManaged(false);
        activePill.setMouseTransparent(true);
        sidebarNav.getChildren().add(0, activePill);
        sidebarNav.widthProperty().addListener((obs, oldW, newW) -> updatePillWidth());
        updatePillWidth();
    }

    private void updatePillWidth() {
        if (activePill == null || sidebarNav == null) return;
        double width = Math.max(0, sidebarNav.getWidth() - (PILL_INSET_X * 2));
        activePill.setPrefWidth(width); activePill.setMinWidth(width); activePill.setMaxWidth(width);
        activePill.setLayoutX(PILL_INSET_X);
    }

    private void moveActivePill(Button target, boolean animate) {
        if (activePill == null || sidebarNav == null || target == null) return;
        Platform.runLater(() -> {
            Bounds bounds = target.getBoundsInParent();
            double targetY = bounds.getMinY(), targetH = bounds.getHeight();
            activePill.setPrefHeight(targetH); activePill.setMinHeight(targetH); activePill.setMaxHeight(targetH);
            if (pillTransition != null) pillTransition.stop();
            if (!animate) { activePill.setTranslateY(targetY); return; }
            pillTransition = new TranslateTransition(PILL_ANIM_DURATION, activePill);
            pillTransition.setToY(targetY); pillTransition.setInterpolator(PILL_EASE);
            pillTransition.play();
        });
    }

    private void fadeEmphasis(Button button, double targetOpacity) {
        if (button == null) return;
        button.lookupAll(".label, .ikonli-font-icon").forEach(node -> {
            FadeTransition fade = new FadeTransition(Duration.millis(180), node);
            fade.setToValue(targetOpacity); fade.setInterpolator(Interpolator.EASE_OUT); fade.play();
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
        double targetWidth = sidebarCollapsed ? 80 : 300;
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(300),
                new KeyValue(sidebar.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                new KeyValue(sidebar.minWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                new KeyValue(sidebar.maxWidthProperty(), targetWidth, Interpolator.EASE_BOTH)));
        timeline.play();
        if (toggleSidebarBtn != null && toggleSidebarBtn.getGraphic() != null) {
            RotateTransition rotate = new RotateTransition(Duration.millis(300), toggleSidebarBtn.getGraphic());
            rotate.setByAngle(sidebarCollapsed ? 180 : -180); rotate.play();
        }
        if (sidebarCollapsed) {
            sidebar.getStyleClass().add("nx-sidebar-collapsed");
            hideSidebarText();
            if (activePill != null) activePill.setVisible(false);
        } else {
            sidebar.getStyleClass().remove("nx-sidebar-collapsed");
            showSidebarText();
            if (activePill != null) {
                activePill.setVisible(true); updatePillWidth(); moveActivePill(currentActiveButton, false);
            }
        }
    }

    private void hideSidebarText() {
        sidebar.lookupAll(".nx-sidebar-text, .nx-sidebar-subtext, .nx-sidebar-section-title").forEach(node -> {
            FadeTransition fade = new FadeTransition(Duration.millis(150), node);
            fade.setToValue(0); fade.setOnFinished(e -> node.setVisible(false)); fade.play();
        });
    }

    private void showSidebarText() {
        sidebar.lookupAll(".nx-sidebar-text, .nx-sidebar-subtext, .nx-sidebar-section-title").forEach(node -> {
            node.setVisible(true);
            FadeTransition fade = new FadeTransition(Duration.millis(300), node);
            fade.setFromValue(0); fade.setToValue(1); fade.play();
        });
    }

    // =============================================
    // SEARCH FUNCTIONALITY
    // =============================================

    private void initializeSearch() {
        if (searchField == null) return;
        searchField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            Node wrapper = searchField.getParent();
            if (wrapper != null) {
                if (isFocused) {
                    wrapper.getStyleClass().add("nx-search-focused");
                    ScaleTransition scale = new ScaleTransition(Duration.millis(200), wrapper);
                    scale.setToX(1.02); scale.setToY(1.02); scale.play();
                } else {
                    wrapper.getStyleClass().remove("nx-search-focused");
                    ScaleTransition scale = new ScaleTransition(Duration.millis(200), wrapper);
                    scale.setToX(1.0); scale.setToY(1.0); scale.play();
                }
            }
        });
        searchField.textProperty().addListener((obs, oldVal, newVal) -> performSearch(newVal));
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        System.out.println("Searching for: " + query);
    }

    // =============================================
    // CLOCK / DATE-TIME
    // =============================================

    private void initializeClock() {
        updateDateTime();
        clockExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r); t.setDaemon(true); t.setName("Clock-Updater"); return t;
        });
        clockExecutor.scheduleAtFixedRate(() -> Platform.runLater(this::updateDateTime), 0, 1, TimeUnit.SECONDS);
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        if (currentDate != null) {
            String dateStr = now.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH));
            if (!dateStr.equals(currentDate.getText())) {
                currentDate.setText(dateStr);
                FadeTransition fade = new FadeTransition(Duration.millis(300), currentDate);
                fade.setFromValue(0.5); fade.setToValue(1); fade.play();
            }
        }
        if (currentTime != null) {
            currentTime.setText(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            ScaleTransition pulse = new ScaleTransition(Duration.millis(100), currentTime);
            pulse.setFromX(1.0); pulse.setFromY(1.0); pulse.setToX(1.02); pulse.setToY(1.02);
            pulse.setAutoReverse(true); pulse.setCycleCount(2); pulse.play();
        }
    }

    public void shutdown() {
        if (clockExecutor != null && !clockExecutor.isShutdown()) clockExecutor.shutdown();
        nodeAnimations.values().forEach(Timeline::stop);
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
        transactionChart.setLegendVisible(false); transactionChart.setAnimated(true); transactionChart.setCreateSymbols(true);
        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Entrées");
        incomeSeries.getData().addAll(
                new XYChart.Data<>("Jan", 320000), new XYChart.Data<>("Fév", 380000),
                new XYChart.Data<>("Mar", 420000), new XYChart.Data<>("Avr", 390000),
                new XYChart.Data<>("Mai", 510000), new XYChart.Data<>("Juin", 580000),
                new XYChart.Data<>("Juil", 620000), new XYChart.Data<>("Août", 710000),
                new XYChart.Data<>("Sep", 680000), new XYChart.Data<>("Oct", 750000),
                new XYChart.Data<>("Nov", 820000), new XYChart.Data<>("Déc", 890000)
        );
        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Sorties");
        expenseSeries.getData().addAll(
                new XYChart.Data<>("Jan", 250000), new XYChart.Data<>("Fév", 290000),
                new XYChart.Data<>("Mar", 310000), new XYChart.Data<>("Avr", 280000),
                new XYChart.Data<>("Mai", 350000), new XYChart.Data<>("Juin", 420000),
                new XYChart.Data<>("Juil", 480000), new XYChart.Data<>("Août", 520000),
                new XYChart.Data<>("Sep", 490000), new XYChart.Data<>("Oct", 560000),
                new XYChart.Data<>("Nov", 610000), new XYChart.Data<>("Déc", 680000)
        );
        transactionChart.getData().clear();
        transactionChart.getData().addAll(incomeSeries, expenseSeries);
        Platform.runLater(() -> {
            Node incomeLine = transactionChart.lookup(".series0");
            if (incomeLine != null) incomeLine.setStyle("-fx-stroke: #00B4A0; -fx-stroke-width: 3px;");
            Node expenseLine = transactionChart.lookup(".series1");
            if (expenseLine != null) expenseLine.setStyle("-fx-stroke: #F4C430; -fx-stroke-width: 3px;");
            int[] idx = {0};
            transactionChart.lookupAll(".chart-series-area-fill").forEach(fill -> {
                if (idx[0] == 0) fill.setStyle("-fx-fill: linear-gradient(to bottom, rgba(0,180,160,0.4), rgba(0,180,160,0.05));");
                else             fill.setStyle("-fx-fill: linear-gradient(to bottom, rgba(244,196,48,0.3), rgba(244,196,48,0.05));");
                idx[0]++;
            });
            for (XYChart.Series<String, Number> series : transactionChart.getData()) {
                for (XYChart.Data<String, Number> data : series.getData()) {
                    Node node = data.getNode();
                    if (node != null) {
                        Tooltip tooltip = new Tooltip(data.getXValue() + ": " + String.format("%,.0f", data.getYValue().doubleValue()) + " DT");
                        tooltip.getStyleClass().add("nx-tooltip");
                        Tooltip.install(node, tooltip);
                        node.setOnMouseEntered(e -> { node.setScaleX(1.5); node.setScaleY(1.5); });
                        node.setOnMouseExited(e ->  { node.setScaleX(1.0); node.setScaleY(1.0); });
                    }
                }
            }
        });
    }

    private void initializePieChart() {
        if (accountPieChart == null) return;
        accountPieChart.setClockwise(true); accountPieChart.setLabelsVisible(false);
        accountPieChart.setStartAngle(90); accountPieChart.setAnimated(true);
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Épargne", 45),
                new PieChart.Data("Courant", 35),
                new PieChart.Data("Professionnel", 20)
        );
        accountPieChart.setData(pieData);
        Platform.runLater(() -> {
            String[] colors = {"#00B4A0", "#0A2540", "#F4C430"};
            int i = 0;
            for (PieChart.Data data : pieData) {
                Node slice = data.getNode();
                if (slice != null) {
                    slice.setStyle("-fx-pie-color: " + colors[i % colors.length] + ";");
                    final int index = i;
                    slice.setOnMouseEntered(e -> {
                        slice.setScaleX(1.05); slice.setScaleY(1.05);
                        DropShadow shadow = new DropShadow();
                        shadow.setColor(Color.web(colors[index % colors.length], 0.5)); shadow.setRadius(15);
                        slice.setEffect(shadow);
                    });
                    slice.setOnMouseExited(e -> { slice.setScaleX(1.0); slice.setScaleY(1.0); slice.setEffect(null); });
                    Tooltip tooltip = new Tooltip(data.getName() + ": " + String.format("%.0f", data.getPieValue()) + "%");
                    tooltip.getStyleClass().add("nx-tooltip");
                    Tooltip.install(slice, tooltip);
                }
                i++;
            }
        });
    }

    private void initializeBarChart() {
        if (weeklyBarChart == null) return;
        weeklyBarChart.setAnimated(true); weeklyBarChart.setLegendVisible(false);
        weeklyBarChart.setCategoryGap(20); weeklyBarChart.setBarGap(4);
        XYChart.Series<String, Number> inbound = new XYChart.Series<>();
        inbound.setName("Entrées");
        inbound.getData().addAll(
                new XYChart.Data<>("Lun", 145), new XYChart.Data<>("Mar", 188),
                new XYChart.Data<>("Mer", 156), new XYChart.Data<>("Jeu", 210),
                new XYChart.Data<>("Ven", 245), new XYChart.Data<>("Sam", 132),
                new XYChart.Data<>("Dim", 98)
        );
        XYChart.Series<String, Number> outbound = new XYChart.Series<>();
        outbound.setName("Sorties");
        outbound.getData().addAll(
                new XYChart.Data<>("Lun", 95), new XYChart.Data<>("Mar", 125),
                new XYChart.Data<>("Mer", 108), new XYChart.Data<>("Jeu", 152),
                new XYChart.Data<>("Ven", 178), new XYChart.Data<>("Sam", 112),
                new XYChart.Data<>("Dim", 75)
        );
        weeklyBarChart.getData().clear();
        weeklyBarChart.getData().addAll(inbound, outbound);
        Platform.runLater(() -> {
            weeklyBarChart.lookupAll(".series0.chart-bar").forEach(bar -> {
                bar.setStyle("-fx-bar-fill: linear-gradient(to top, #009485, #00B4A0);");
                addBarHoverEffect(bar);
            });
            weeklyBarChart.lookupAll(".series1.chart-bar").forEach(bar -> {
                bar.setStyle("-fx-bar-fill: linear-gradient(to top, #D4A82A, #F4C430);");
                addBarHoverEffect(bar);
            });
        });
    }

    private void addBarHoverEffect(Node bar) {
        bar.setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), bar);
            scale.setToY(1.05); scale.play(); bar.setEffect(new Glow(0.3));
        });
        bar.setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(150), bar);
            scale.setToY(1.0); scale.play(); bar.setEffect(null);
        });
    }

    // =============================================
    // TABLE INITIALIZATION
    // =============================================

    private void initializeTable() {
        if (recentTransactionsTable == null) return;
        recentTransactionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TransactionRow, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> data.getValue().idProperty()); colId.setPrefWidth(100);
        colId.setCellFactory(col -> createAnimatedCell());

        TableColumn<TransactionRow, String> colClient = new TableColumn<>("Client");
        colClient.setCellValueFactory(data -> data.getValue().clientProperty()); colClient.setPrefWidth(180);
        colClient.setCellFactory(col -> createClientCell());

        TableColumn<TransactionRow, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(data -> data.getValue().typeProperty()); colType.setPrefWidth(120);
        colType.setCellFactory(col -> createTypeCell());

        TableColumn<TransactionRow, String> colAmount = new TableColumn<>("Montant");
        colAmount.setCellValueFactory(data -> data.getValue().amountProperty()); colAmount.setPrefWidth(130);
        colAmount.setCellFactory(col -> createAmountCell());

        TableColumn<TransactionRow, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(data -> data.getValue().dateProperty()); colDate.setPrefWidth(140);
        colDate.setCellFactory(col -> createAnimatedCell());

        TableColumn<TransactionRow, String> colStatus = new TableColumn<>("Statut");
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty()); colStatus.setPrefWidth(120);
        colStatus.setCellFactory(col -> createStatusCell());

        TableColumn<TransactionRow, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(100); colActions.setCellFactory(col -> createActionsCell());

        recentTransactionsTable.getColumns().clear();
        recentTransactionsTable.getColumns().addAll(colId, colClient, colType, colAmount, colDate, colStatus, colActions);

        ObservableList<TransactionRow> rows = FXCollections.observableArrayList(
                new TransactionRow("TX-3021", "Nadia Karim",    "Virement", "12,500 DT", "15 Jan 2025", "Succès"),
                new TransactionRow("TX-3020", "Hassan Mansour", "Dépôt",    "4,200 DT",  "15 Jan 2025", "Succès"),
                new TransactionRow("TX-3019", "Sami Belhadj",   "Retrait",  "1,800 DT",  "14 Jan 2025", "En attente"),
                new TransactionRow("TX-3018", "Yasmine Amara",  "Virement", "9,750 DT",  "14 Jan 2025", "Succès"),
                new TransactionRow("TX-3017", "Mehdi Rahal",    "Paiement", "2,300 DT",  "13 Jan 2025", "Échec"),
                new TransactionRow("TX-3016", "Amine Trabelsi", "Dépôt",    "15,000 DT", "13 Jan 2025", "Succès"),
                new TransactionRow("TX-3015", "Leïla Sassi",    "Virement", "6,450 DT",  "12 Jan 2025", "En cours"),
                new TransactionRow("TX-3014", "Rim Hammami",    "Paiement", "980 DT",    "12 Jan 2025", "Succès")
        );
        recentTransactionsTable.setItems(rows);

        recentTransactionsTable.setRowFactory(tv -> {
            TableRow<TransactionRow> row = new TableRow<>();
            row.setOnMouseEntered(e -> { if (!row.isEmpty()) row.setStyle("-fx-background-color: rgba(0, 180, 160, 0.08);"); });
            row.setOnMouseExited(e -> row.setStyle(""));
            return row;
        });
    }

    private TableCell<TransactionRow, String> createAnimatedCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                setText(item);
                FadeTransition fade = new FadeTransition(Duration.millis(300), this);
                fade.setFromValue(0); fade.setToValue(1); fade.play();
            }
        };
    }

    private TableCell<TransactionRow, String> createClientCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                HBox container = new HBox(10); container.setAlignment(Pos.CENTER_LEFT);
                StackPane avatar = new StackPane();
                avatar.getStyleClass().add("nx-table-avatar");
                avatar.setMinSize(32, 32); avatar.setMaxSize(32, 32);
                String[] parts = item.split(" ");
                String initials = parts.length >= 2
                        ? (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase()
                        : item.substring(0, Math.min(2, item.length())).toUpperCase();
                Label initialsLabel = new Label(initials);
                initialsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
                String[] colors = {"#00B4A0", "#0A2540", "#8B5CF6", "#F59E0B", "#3B82F6"};
                avatar.setStyle("-fx-background-color: " + colors[Math.abs(item.hashCode()) % colors.length] + "; -fx-background-radius: 16px;");
                avatar.getChildren().add(initialsLabel);
                Label nameLabel = new Label(item);
                nameLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #0A2540;");
                container.getChildren().addAll(avatar, nameLabel);
                setGraphic(container); setText(null);
            }
        };
    }

    private TableCell<TransactionRow, String> createTypeCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                HBox container = new HBox(8); container.setAlignment(Pos.CENTER_LEFT);
                FontIcon icon = new FontIcon(); icon.setIconSize(14);
                String iconLiteral, color;
                switch (item.toLowerCase()) {
                    case "virement": iconLiteral = "fas-exchange-alt"; color = "#8B5CF6"; break;
                    case "dépôt":    iconLiteral = "fas-arrow-down";   color = "#10B981"; break;
                    case "retrait":  iconLiteral = "fas-arrow-up";     color = "#F59E0B"; break;
                    case "paiement": iconLiteral = "fas-credit-card";  color = "#3B82F6"; break;
                    default:         iconLiteral = "fas-circle";       color = "#6B7280";
                }
                icon.setIconLiteral(iconLiteral); icon.setIconColor(Color.web(color));
                Label typeLabel = new Label(item);
                typeLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: 500;");
                container.getChildren().addAll(icon, typeLabel);
                setGraphic(container); setText(null);
            }
        };
    }

    private TableCell<TransactionRow, String> createAmountCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label amountLabel = new Label(item);
                amountLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #0A2540; -fx-font-size: 13px;");
                setGraphic(amountLabel); setText(null);
            }
        };
    }

    private TableCell<TransactionRow, String> createStatusCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().add("nx-status-chip");
                badge.setPadding(new Insets(4, 12, 4, 12));
                String value = item.toLowerCase(Locale.FRENCH);
                String bgColor, textColor;
                FontIcon icon = new FontIcon(); icon.setIconSize(10);
                if (value.contains("succès") || value.contains("valid")) {
                    bgColor = "rgba(16, 185, 129, 0.15)"; textColor = "#059669";
                    icon.setIconLiteral("fas-check-circle"); icon.setIconColor(Color.web("#059669"));
                } else if (value.contains("attente") || value.contains("en cours")) {
                    bgColor = "rgba(245, 158, 11, 0.15)"; textColor = "#D97706";
                    icon.setIconLiteral("fas-clock"); icon.setIconColor(Color.web("#D97706"));
                } else {
                    bgColor = "rgba(239, 68, 68, 0.15)"; textColor = "#DC2626";
                    icon.setIconLiteral("fas-times-circle"); icon.setIconColor(Color.web("#DC2626"));
                }
                badge.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor +
                        "; -fx-background-radius: 20px; -fx-font-size: 11px; -fx-font-weight: 600;");
                HBox container = new HBox(6); container.setAlignment(Pos.CENTER_LEFT);
                container.getChildren().addAll(icon, badge);
                setGraphic(container); setText(null);
            }
        };
    }

    private TableCell<TransactionRow, Void> createActionsCell() {
        return new TableCell<>() {
            private final Button viewBtn = new Button();
            private final Button editBtn = new Button();
            {
                FontIcon viewIcon = new FontIcon("fas-eye"); viewIcon.setIconSize(12); viewIcon.setIconColor(Color.web("#6B7280"));
                viewBtn.setGraphic(viewIcon); viewBtn.getStyleClass().add("nx-table-action-btn");
                viewBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6px;");

                FontIcon editIcon = new FontIcon("fas-edit"); editIcon.setIconSize(12); editIcon.setIconColor(Color.web("#6B7280"));
                editBtn.setGraphic(editIcon); editBtn.getStyleClass().add("nx-table-action-btn");
                editBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6px;");

                viewBtn.setOnMouseEntered(e -> viewIcon.setIconColor(Color.web("#00B4A0")));
                viewBtn.setOnMouseExited(e  -> viewIcon.setIconColor(Color.web("#6B7280")));
                editBtn.setOnMouseEntered(e -> editIcon.setIconColor(Color.web("#3B82F6")));
                editBtn.setOnMouseExited(e  -> editIcon.setIconColor(Color.web("#6B7280")));

                viewBtn.setOnAction(e -> showTransactionDetails(getTableView().getItems().get(getIndex())));
                editBtn.setOnAction(e -> editTransaction(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox actions = new HBox(4); actions.setAlignment(Pos.CENTER);
                actions.getChildren().addAll(viewBtn, editBtn);
                setGraphic(actions);
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
        styleAlert(alert); alert.showAndWait();
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
        if (event == null || !(event.getSource() instanceof Button)) return;
        setActiveButton((Button) event.getSource());
    }

    @FXML
    private void showDashboard() {
        setActiveButton(btnDashboard);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), contentArea);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(dashboardContent);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentArea);
            fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();
            animateChildrenWithStagger(dashboardContent, 50);
        });
        fadeOut.play();
    }

    @FXML
    private void showGestionUser() {
        setActiveButton(btnGestionUser);
        loadModulePage("Gestion Utilisateurs", new String[]{"Utilisateurs"}, new String[]{"User.fxml"});
    }

    @FXML
    private void showAdminAccountManagement() {
        setActiveButton(btnAdminAccount);
        loadModulePage("Admin Account Management", new String[]{"Security Settings"}, new String[]{"AdminSecuritySettingsView.fxml"});
    }

    @FXML
    private void showGestionCompte() {
        setActiveButton(btnGestionCompte);
        loadModulePage("Gestion Comptes Bancaires",
                new String[]{"Compte Bancaire", "Coffre Virtuel"},
                new String[]{"CompteBancaire.fxml", "CoffreVirtuel.fxml"});
    }

    @FXML
    private void showGestionCoffre() { showGestionCompte(); }

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
        loadModulePage("Gestion des Crédits", new String[]{"Crédit", "Garantie"}, new String[]{"Credit.fxml", "GarantieCredit.fxml"});
    }

    @FXML
    private void showGestionCashback() {
        setActiveButton(btnGestionCashback);
        loadModulePage("Gestion Cashback", new String[]{"Partenaire", "Cashback"}, new String[]{"Partenaire.fxml", "Cashback.fxml"});
    }

    // =============================================
    // MODULE PAGE LOADING
    // =============================================

    private void loadModulePage(String title, String[] entities, String[] fxmlFiles) {
        VBox moduleContent = new VBox(24);
        moduleContent.getStyleClass().add("nx-module-content");
        moduleContent.setPadding(new Insets(0));
        moduleContent.getChildren().add(createEnhancedPageHeader(title));
        moduleContent.getChildren().add(createEntityTabs(entities, fxmlFiles));
        StackPane entityContent = new StackPane();
        entityContent.setId("entityContent"); entityContent.getStyleClass().add("nx-entity-content");
        VBox.setVgrow(entityContent, Priority.ALWAYS);
        moduleContent.getChildren().add(entityContent);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), contentArea);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(moduleContent);
            if (fxmlFiles.length > 0) loadEntityContent(fxmlFiles[0], entityContent);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentArea);
            fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();
        });
        fadeOut.play();
    }

    private HBox createEnhancedPageHeader(String title) {
        HBox header = new HBox();
        header.getStyleClass().add("nx-page-header");
        header.setAlignment(Pos.CENTER_LEFT); header.setPadding(new Insets(0, 0, 20, 0));
        FontIcon icon = new FontIcon("fas-folder-open"); icon.setIconSize(28); icon.setIconColor(Color.web("#00B4A0"));
        StackPane iconContainer = new StackPane(icon);
        iconContainer.setStyle("-fx-background-color: rgba(0, 180, 160, 0.1); -fx-background-radius: 12px; -fx-padding: 12px;");
        VBox titleBox = new VBox(4); titleBox.setPadding(new Insets(0, 0, 0, 16));
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("nx-page-title");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #0A2540;");
        Label subtitleLabel = new Label("Gérer et superviser les données");
        subtitleLabel.getStyleClass().add("nx-page-subtitle");
        subtitleLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 14px;");
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(12);
        Button exportBtn = new Button("Exporter"); exportBtn.getStyleClass().addAll("nx-btn", "nx-btn-outline");
        FontIcon exportIcon = new FontIcon("fas-download"); exportIcon.setIconSize(14); exportBtn.setGraphic(exportIcon);
        Button addBtn = new Button("Ajouter"); addBtn.getStyleClass().addAll("nx-btn", "nx-btn-primary");
        FontIcon addIcon = new FontIcon("fas-plus"); addIcon.setIconSize(14); addIcon.setIconColor(Color.WHITE); addBtn.setGraphic(addIcon);
        actions.getChildren().addAll(exportBtn, addBtn);
        header.getChildren().addAll(iconContainer, titleBox, spacer, actions);
        return header;
    }

    private HBox createEntityTabs(String[] entities, String[] fxmlFiles) {
        HBox tabContainer = new HBox(8);
        tabContainer.getStyleClass().add("nx-entity-tabs");
        tabContainer.setPadding(new Insets(0, 0, 20, 0));
        tabContainer.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 12px; -fx-padding: 6px;");
        ToggleGroup toggleGroup = new ToggleGroup();
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: #6B7280; -fx-font-weight: 600; -fx-padding: 10px 20px; -fx-background-radius: 8px; -fx-cursor: hand;";
        String activeStyle  = "-fx-background-color: white; -fx-text-fill: #0A2540; -fx-font-weight: 700; -fx-padding: 10px 20px; -fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);";
        for (int i = 0; i < entities.length; i++) {
            final String fxmlFile = fxmlFiles[i];
            ToggleButton tab = new ToggleButton(entities[i]);
            tab.setToggleGroup(toggleGroup); tab.getStyleClass().add("nx-entity-tab");
            tab.setStyle(i == 0 ? activeStyle : normalStyle);
            if (i == 0) tab.setSelected(true);
            tab.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                tab.setStyle(isSelected ? activeStyle : normalStyle);
                if (isSelected) {
                    VBox moduleContent = (VBox) contentArea.getChildren().get(0);
                    StackPane entityContent = (StackPane) moduleContent.lookup("#entityContent");
                    if (entityContent != null) loadEntityContent(fxmlFile, entityContent);
                }
            });
            tabContainer.getChildren().add(tab);
        }
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        tabContainer.getChildren().add(spacer);
        Button statsBtn = new Button("Statistiques"); statsBtn.getStyleClass().addAll("nx-btn", "nx-btn-gold");
        FontIcon chartIcon = new FontIcon("fas-chart-pie"); chartIcon.setIconSize(14); statsBtn.setGraphic(chartIcon);
        statsBtn.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #F7D35C, #F4C430); -fx-text-fill: #0A2540; -fx-font-weight: 700; -fx-padding: 10px 20px; -fx-background-radius: 8px; -fx-cursor: hand;");
        statsBtn.setOnAction(e -> loadStatisticsView());
        tabContainer.getChildren().add(statsBtn);
        return tabContainer;
    }

    private void loadEntityContent(String fxmlFile, StackPane container) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Node content = loader.load();
            normalizeStyleClasses(content); attachResponsiveStylesheet(content); applyEntityLayoutTuning(content);
            FadeTransition fadeOut = new FadeTransition(Duration.millis(100), container);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                container.getChildren().clear(); container.getChildren().add(content);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), container);
                fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();
            });
            fadeOut.play();
        } catch (IOException e) {
            showErrorMessage("Erreur de chargement", "Impossible de charger: " + fxmlFile);
            e.printStackTrace();
        }
    }

    private void loadStatisticsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Statistics.fxml"));
            Node content = loader.load();
            normalizeStyleClasses(content); attachResponsiveStylesheet(content); applyEntityLayoutTuning(content);
            VBox moduleContent = (VBox) contentArea.getChildren().get(0);
            StackPane entityContent = (StackPane) moduleContent.lookup("#entityContent");
            if (entityContent != null) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(100), entityContent);
                fadeOut.setFromValue(1); fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> {
                    entityContent.getChildren().clear(); entityContent.getChildren().add(content);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(200), entityContent);
                    fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();
                });
                fadeOut.play();
            }
        } catch (IOException e) {
            showErrorMessage("Erreur", "Impossible de charger les statistiques"); e.printStackTrace();
        }
    }

    private void initializeResponsiveMode() {
        Platform.runLater(() -> {
            if (contentArea == null || contentArea.getScene() == null) return;
            Scene scene = contentArea.getScene();
            String responsiveUrl = getClass().getResource(RESPONSIVE_CSS).toExternalForm();
            if (!scene.getStylesheets().contains(responsiveUrl)) scene.getStylesheets().add(responsiveUrl);
            Parent root = scene.getRoot();
            normalizeStyleClasses(root); applyCompactClass(root, scene.getWidth());
            scene.widthProperty().addListener((obs, oldVal, newVal) -> {
                normalizeStyleClasses(root); applyCompactClass(root, newVal.doubleValue());
            });
        });
    }

    private void attachResponsiveStylesheet(Node content) {
        if (!(content instanceof Parent parent)) return;
        String responsiveUrl = getClass().getResource(RESPONSIVE_CSS).toExternalForm();
        if (!parent.getStylesheets().contains(responsiveUrl)) parent.getStylesheets().add(responsiveUrl);
    }

    private void applyCompactClass(Parent root, double width) {
        boolean compact = width <= 1420;
        if (compact) { if (!root.getStyleClass().contains(COMPACT_CLASS)) root.getStyleClass().add(COMPACT_CLASS); }
        else root.getStyleClass().remove(COMPACT_CLASS);
    }

    private void applyEntityLayoutTuning(Node content) {
        if (!(content instanceof Parent parent) || contentArea == null || contentArea.getScene() == null) return;
        double sceneWidth = contentArea.getScene().getWidth();
        boolean compact = sceneWidth <= 1420;
        double targetFormWidth = sceneWidth <= 1200 ? 380 : (compact ? 430 : 470);
        for (Node node : parent.lookupAll(".nx-card")) {
            if (node instanceof Region region) {
                double prefWidth = region.getPrefWidth();
                if (prefWidth >= 360 && prefWidth <= 460) region.setPrefWidth(targetFormWidth);
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
        if (node == null) return;
        ObservableList<String> classes = node.getStyleClass();
        if (!classes.isEmpty()) {
            List<String> normalized = new ArrayList<>();
            for (String raw : classes) {
                if (raw == null || raw.isBlank()) continue;
                for (String part : raw.split("[,\\s]+")) {
                    String cleaned = part.trim().replace(",", "");
                    if (!cleaned.isBlank() && !normalized.contains(cleaned)) normalized.add(cleaned);
                }
            }
            if (!normalized.equals(new ArrayList<>(classes))) classes.setAll(normalized);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) normalizeStyleClasses(child);
        }
    }

    // =============================================
    // TOP BAR ACTIONS
    // =============================================

    @FXML
    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        FadeTransition fade = new FadeTransition(Duration.millis(300), contentArea.getScene().getRoot());
        fade.setFromValue(1); fade.setToValue(0.8); fade.setAutoReverse(true); fade.setCycleCount(2); fade.play();
        if (isDarkTheme) contentArea.getScene().getRoot().getStyleClass().add("dark-theme");
        else             contentArea.getScene().getRoot().getStyleClass().remove("dark-theme");
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

    // ★ MODIFIÉ — ouvre le panneau admin de validation des comptes "En attente"
    @FXML
    private void openNotifications() {
        Window owner = contentArea.getScene().getWindow();
        new NotificationPanelController().show(owner);
        // Rafraîchit le badge juste après la fermeture du panneau
        Platform.runLater(this::refreshNotificationBadge);
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
            shutdown(); AuthSession.clear();
            SceneRouter.show("/fxml/Home.fxml", "NEXORA BANK - Welcome", 1200, 760, 980, 680);
        }
    }

    @FXML
    private void closeAlert(ActionEvent event) {
        if (event == null || event.getSource() == null) return;
        Node source = (Node) event.getSource();
        Node parent = source.getParent();
        if (parent != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(200), parent);
            fade.setFromValue(1); fade.setToValue(0);
            TranslateTransition slide = new TranslateTransition(Duration.millis(200), parent);
            slide.setToX(50);
            ParallelTransition parallel = new ParallelTransition(fade, slide);
            parallel.setOnFinished(e -> { parent.setVisible(false); parent.setManaged(false); });
            parallel.play();
        }
    }

    @FXML
    private void addNewEntity(MouseEvent event) {
        if (event.getSource() instanceof Node) createRippleEffect((Node) event.getSource(), event);
        showInfoAlert("Nouveau", "Sélectionnez le type d'entité à créer");
    }

    // =============================================
    // UTILITY METHODS
    // =============================================

    private void showNotificationPopup(String title, String message, String iconLiteral) {
        showInfoAlert(title, message);
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        styleAlert(alert); Platform.runLater(alert::show);
    }

    private void showErrorMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        styleAlert(alert); Platform.runLater(alert::show);
    }

    private void styleAlert(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-background-radius: 16px;");
        try {
            dialogPane.getStylesheets().add(getClass().getResource("/css/MainView.css").toExternalForm());
        } catch (Exception e) { /* ignore si le CSS est absent */ }
    }

    // =============================================
    // DATA MODEL
    // =============================================

    public static final class TransactionRow {
        private final StringProperty id, client, type, amount, date, status;

        public TransactionRow(String id, String client, String type, String amount, String date, String status) {
            this.id     = new SimpleStringProperty(id);
            this.client = new SimpleStringProperty(client);
            this.type   = new SimpleStringProperty(type);
            this.amount = new SimpleStringProperty(amount);
            this.date   = new SimpleStringProperty(date);
            this.status = new SimpleStringProperty(status);
        }

        public StringProperty idProperty()     { return id; }
        public StringProperty clientProperty() { return client; }
        public StringProperty typeProperty()   { return type; }
        public StringProperty amountProperty() { return amount; }
        public StringProperty dateProperty()   { return date; }
        public StringProperty statusProperty() { return status; }

        public String getId()     { return id.get(); }
        public String getClient() { return client.get(); }
        public String getType()   { return type.get(); }
        public String getAmount() { return amount.get(); }
        public String getDate()   { return date.get(); }
        public String getStatus() { return status.get(); }
    }
}

/*
 * ══════════════════════════════════════════════════════════════════
 * RAPPEL — Badge cloche dans MainView.fxml
 * ══════════════════════════════════════════════════════════════════
 *
 * Pour que lblNotifBadge fonctionne, trouver dans MainView.fxml le Label
 * qui affiche "7" (le badge fixe de la cloche) et ajouter fx:id :
 *
 *   AVANT :
 *   <Label text="7" styleClass="nx-topbar-badge nx-badge-pulse" .../>
 *
 *   APRÈS :
 *   <Label fx:id="lblNotifBadge" text="0"
 *          styleClass="nx-topbar-badge nx-badge-pulse" .../>
 *
 * Sans cette modification, le badge reste figé mais le panneau
 * NotificationPanelController fonctionne quand même parfaitement.
 * ══════════════════════════════════════════════════════════════════
 */