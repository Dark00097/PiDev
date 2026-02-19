package com.nexora.bank.controllers;

import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.Cashback;
import com.nexora.bank.Models.Partenaire;
import com.nexora.bank.Models.User;
import com.nexora.bank.Service.CashbackService;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Controller for the user-facing Cashback Rewards section.
 * Displays the authenticated user's cashback history, balances, and partner information.
 * Includes forms for partner registration and cashback entry.
 * All data is scoped to the current user via AuthSession.
 */
public class UserDashboardCashbackSectionController {

    // ═══════════════════ FORM CONTAINERS ═══════════════════
    @FXML private VBox partnerFormContainer;
    @FXML private VBox cashbackFormContainer;

    // ═══════════════════ TAB BUTTONS ═══════════════════
    @FXML private Button tabPartner;
    @FXML private Button tabCashback;

    // ═══════════════════ PARTNER FORM FIELDS ═══════════════════
    @FXML private TextField txtPartnerName;
    @FXML private TextField txtBaseRate;
    @FXML private TextField txtMaxRate;
    @FXML private TextField txtMonthlyLimit;
    @FXML private DatePicker dpValidUntil;
    @FXML private TextArea txtPartnerDescription;

    // ═══════════════════ CASHBACK FORM FIELDS ═══════════════════
    @FXML private TextField txtPurchaseAmount;
    @FXML private TextField txtAppliedRate;
    @FXML private TextField txtCashbackAmount;
    @FXML private TextField txtTransactionRef;
    @FXML private DatePicker dpPurchaseDate;

    // ═══════════════════ KPI LABELS ═══════════════════
    @FXML private Label lblTotalRewards;
    @FXML private Label lblMembershipTier;
    @FXML private Label lblMonthAmount;
    @FXML private Label lblMonthName;
    @FXML private Label lblPartnerCount;
    @FXML private Label lblPendingAmount;
    @FXML private Label lblPendingCount;

    // ═══════════════════ QUICK STATS ═══════════════════
    @FXML private Label lblWeekEarnings;
    @FXML private Label lblPendingStat;
    @FXML private Label lblRedeemedStat;
    @FXML private Label lblAvailableBalance;

    // ═══════════════════ CONTAINERS ═══════════════════
    @FXML private FlowPane partnersGrid;
    @FXML private VBox cashbackListContainer;
    @FXML private VBox loadingContainer;
    @FXML private VBox emptyStateContainer;
    @FXML private VBox contentContainer;
    @FXML private ComboBox<String> filterPeriod;

    // ═══════════════════ MEMBERSHIP ICON ═══════════════════
    @FXML private FontIcon membershipIcon;

    private final CashbackService cashbackService = new CashbackService();
    private static final Duration ANIMATION_DURATION = Duration.millis(250);
    private static final String TAB_ACTIVE_CLASS = "form-tab-active";

    private int currentUserId = -1;

    @FXML
    private void initialize() {
        // Verify authentication
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null) {
            showEmptyState("Session expired", "Please log in again to view your rewards.");
            return;
        }
        currentUserId = currentUser.getIdUser();

        // Setup listeners for cashback calculation
        if (txtPurchaseAmount != null) {
            txtPurchaseAmount.textProperty().addListener((obs, oldVal, newVal) -> calculateCashback());
        }

        // Setup filter
        if (filterPeriod != null) {
            filterPeriod.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> loadCashbackHistory()
            );
        }

        // Show partner form by default
        showPartnerForm();

        // Load data asynchronously
        loadData();
    }

    /**
     * Reload all data - can be called externally to refresh.
     */
    public void refreshData() {
        if (currentUserId > 0) {
            loadData();
        }
    }

    // ═══════════════════ TAB NAVIGATION ═══════════════════

    @FXML
    private void showPartnerForm() {
        if (partnerFormContainer == null || cashbackFormContainer == null) return;
        updateTabStyles(tabPartner);
        setVisibleManaged(partnerFormContainer, true);
        setVisibleManaged(cashbackFormContainer, false);
        animateFadeIn(partnerFormContainer);
    }

    @FXML
    private void showCashbackForm() {
        if (partnerFormContainer == null || cashbackFormContainer == null) return;
        updateTabStyles(tabCashback);
        setVisibleManaged(partnerFormContainer, false);
        setVisibleManaged(cashbackFormContainer, true);
        animateFadeIn(cashbackFormContainer);
    }

    private void updateTabStyles(Button activeTab) {
        if (tabPartner != null) tabPartner.getStyleClass().remove(TAB_ACTIVE_CLASS);
        if (tabCashback != null) tabCashback.getStyleClass().remove(TAB_ACTIVE_CLASS);
        if (activeTab != null && !activeTab.getStyleClass().contains(TAB_ACTIVE_CLASS)) {
            activeTab.getStyleClass().add(TAB_ACTIVE_CLASS);
        }
    }

    // ═══════════════════ FORM ACTIONS ═══════════════════

    @FXML
    private void addPartner() {
        if (validatePartnerForm()) {
            showNotification("Partenaire ajouté",
                "Le partenaire \"" + txtPartnerName.getText() + "\" a été enregistré avec succès !");
            clearPartnerForm();
        }
    }

    @FXML
    private void clearPartnerForm() {
        if (txtPartnerName != null) txtPartnerName.clear();
        if (txtBaseRate != null) txtBaseRate.clear();
        if (txtMaxRate != null) txtMaxRate.clear();
        if (txtMonthlyLimit != null) txtMonthlyLimit.clear();
        if (dpValidUntil != null) dpValidUntil.setValue(null);
        if (txtPartnerDescription != null) txtPartnerDescription.clear();
    }

    @FXML
    private void recordCashback() {
        if (validateCashbackForm()) {
            String amount = txtCashbackAmount != null ? txtCashbackAmount.getText() : "0.00";
            showNotification("Cashback enregistré",
                "Le cashback de $" + amount + " a été enregistré et sera crédité sous 3-5 jours ouvrables.");
            clearCashbackForm();
        }
    }

    @FXML
    private void clearCashbackForm() {
        if (txtPurchaseAmount != null) txtPurchaseAmount.clear();
        if (txtCashbackAmount != null) txtCashbackAmount.setText("0.00");
        if (txtTransactionRef != null) txtTransactionRef.clear();
        if (dpPurchaseDate != null) dpPurchaseDate.setValue(null);
    }

    @FXML
    private void viewRewardsHistory() {
        if (filterPeriod != null) {
            filterPeriod.setValue("All time");
        }
        loadCashbackHistory();
        showNotification("Historique des récompenses", "Ouverture de l'historique complet de vos récompenses...");
    }

    @FXML
    private void redeemRewards() {
        if (currentUserId <= 0) {
            showNotification("Erreur", "Veuillez vous connecter pour échanger vos récompenses.");
            return;
        }

        double available = cashbackService.getAvailableBalanceByUser(currentUserId);
        if (available <= 0) {
            showNotification("Aucune récompense disponible",
                "Vous n'avez pas encore de récompenses cashback disponibles pour l'échange.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Échanger les récompenses");
        alert.setHeaderText("Échanger vos récompenses disponibles ?");
        alert.setContentText(String.format(
            "Vous avez %.2f DT disponibles. Voulez-vous :\n\n" +
            "- Transférer vers votre compte bancaire\n" +
            "- Appliquer au prochain achat\n" +
            "- Convertir en cartes cadeaux",
            available
        ));

        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/UserDashboard.css").toExternalForm()
        );

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showNotification("Échange lancé",
                    "L'échange de vos récompenses a été lancé. Vous recevrez un email de confirmation bientôt.");
            }
        });
    }

    // ═══════════════════ CASHBACK CALCULATION ═══════════════════

    private void calculateCashback() {
        try {
            if (txtPurchaseAmount != null && !txtPurchaseAmount.getText().isEmpty() && txtAppliedRate != null) {
                double amount = Double.parseDouble(txtPurchaseAmount.getText().replace(",", ""));
                double rate = Double.parseDouble(txtAppliedRate.getText().replace("%", ""));
                double cashback = CashbackService.calculateCashbackAmount(amount, rate);

                if (txtCashbackAmount != null) {
                    txtCashbackAmount.setText(String.format("%.2f", cashback));
                }
            }
        } catch (NumberFormatException e) {
            // Ignore parsing errors
        }
    }

    // ═══════════════════ VALIDATION ═══════════════════

    private boolean validatePartnerForm() {
        if (txtPartnerName == null || txtPartnerName.getText().trim().isEmpty()) {
            showNotification("Erreur de validation", "Veuillez saisir un nom de partenaire.");
            return false;
        }
        if (txtBaseRate == null || txtBaseRate.getText().trim().isEmpty()) {
            showNotification("Erreur de validation", "Veuillez saisir un taux de cashback de base.");
            return false;
        }
        return true;
    }

    private boolean validateCashbackForm() {
        if (txtPurchaseAmount == null || txtPurchaseAmount.getText().trim().isEmpty()) {
            showNotification("Erreur de validation", "Veuillez saisir un montant d'achat.");
            return false;
        }
        if (dpPurchaseDate == null || dpPurchaseDate.getValue() == null) {
            showNotification("Erreur de validation", "Veuillez sélectionner une date d'achat.");
            return false;
        }
        return true;
    }

    // ═══════════════════ DATA LOADING ═══════════════════

    private void loadData() {
        showLoading(true);

        Task<Void> loadTask = new Task<>() {
            private double totalEarned, availableBalance, pendingAmount, monthAmount, redeemedAmount;
            private int partnerCount, pendingCount;
            private String tier;
            private List<Cashback> cashbacks;
            private List<Object[]> partnerEarnings;
            private List<Partenaire> partners;

            @Override
            protected Void call() {
                // All DB queries run on background thread
                try {
                    totalEarned = cashbackService.getTotalEarnedByUser(currentUserId);
                    availableBalance = cashbackService.getAvailableBalanceByUser(currentUserId);
                    pendingAmount = cashbackService.getPendingByUser(currentUserId);
                    monthAmount = cashbackService.getCurrentMonthByUser(currentUserId);
                    redeemedAmount = cashbackService.getRedeemedByUser(currentUserId);
                    partnerCount = cashbackService.getActivePartnerCountByUser(currentUserId);
                    pendingCount = cashbackService.getPendingCountByUser(currentUserId);
                    tier = CashbackService.getMembershipTier(totalEarned);
                    cashbacks = cashbackService.getCashbacksByUserId(currentUserId);
                    partnerEarnings = cashbackService.getEarningsPerPartnerByUser(currentUserId);
                    partners = cashbackService.getAllPartners();
                } catch (Exception e) {
                    System.err.println("Error loading cashback data: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    updateKPIs(totalEarned, availableBalance, pendingAmount, monthAmount,
                              redeemedAmount, partnerCount, pendingCount, tier);
                    buildPartnerCards(partnerEarnings, partners);
                    buildCashbackHistory(cashbacks);
                    showLoading(false);

                    if (cashbacks.isEmpty() && partnerEarnings.isEmpty()) {
                        showEmptyState("No cashback yet",
                            "Start shopping with our partners to earn cashback rewards!");
                    } else {
                        showContent();
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showLoading(false);
                    showEmptyState("Error loading data",
                        "Unable to load your cashback data. Please try again later.");
                });
            }
        };

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    // ═══════════════════ KPI UPDATES ═══════════════════

    private void updateKPIs(double totalEarned, double availableBalance, double pendingAmount,
                           double monthAmount, double redeemedAmount, int partnerCount,
                           int pendingCount, String tier) {
        if (lblTotalRewards != null) {
            lblTotalRewards.setText(formatCurrency(totalEarned));
        }
        if (lblMembershipTier != null) {
            lblMembershipTier.setText("Membre " + tier);
        }
        if (membershipIcon != null) {
            membershipIcon.setIconLiteral(CashbackService.getMembershipTierIcon(tier));
        }
        if (lblMonthAmount != null) {
            lblMonthAmount.setText("+" + formatCurrency(monthAmount));
        }
        if (lblMonthName != null) {
            lblMonthName.setText(LocalDate.now().getMonth().toString().substring(0, 1)
                + LocalDate.now().getMonth().toString().substring(1).toLowerCase()
                + " " + LocalDate.now().getYear());
        }
        if (lblPartnerCount != null) {
            lblPartnerCount.setText(String.valueOf(partnerCount));
        }
        if (lblPendingAmount != null) {
            lblPendingAmount.setText(formatCurrency(pendingAmount));
        }
        if (lblPendingCount != null) {
            lblPendingCount.setText(pendingCount + " transaction" + (pendingCount != 1 ? "s" : ""));
        }

        // Quick stats
        if (lblWeekEarnings != null) {
            lblWeekEarnings.setText("+" + formatCurrency(monthAmount));
        }
        if (lblPendingStat != null) {
            lblPendingStat.setText(formatCurrency(pendingAmount));
        }
        if (lblRedeemedStat != null) {
            lblRedeemedStat.setText(formatCurrency(redeemedAmount));
        }
        if (lblAvailableBalance != null) {
            lblAvailableBalance.setText(formatCurrency(availableBalance));
        }
    }

    // ═══════════════════ PARTNER CARDS ═══════════════════

    private void buildPartnerCards(List<Object[]> partnerEarnings, List<Partenaire> allPartners) {
        if (partnersGrid == null) return;
        partnersGrid.getChildren().clear();

        if (partnerEarnings.isEmpty() && allPartners.isEmpty()) {
            Label noPartners = new Label("No partner activity yet");
            noPartners.getStyleClass().add("empty-hint");
            partnersGrid.getChildren().add(noPartners);
            return;
        }

        // Show partners the user has earned from
        for (Object[] pe : partnerEarnings) {
            String name = (String) pe[0];
            String category = (String) pe[1];
            double earned = (double) pe[2];
            double maxRate = (double) pe[3];

            VBox card = createPartnerCard(name, category, earned, maxRate);
            partnersGrid.getChildren().add(card);
        }

        // If user has fewer than 3 partner cards, show some available partners
        if (partnerEarnings.size() < 3 && allPartners != null) {
            for (Partenaire p : allPartners) {
                boolean alreadyShown = partnerEarnings.stream()
                    .anyMatch(pe -> pe[0].equals(p.getNom()));
                if (!alreadyShown) {
                    VBox card = createPartnerCard(p.getNom(), p.getCategorie(), 0, p.getTauxCashbackMax());
                    partnersGrid.getChildren().add(card);
                    if (partnersGrid.getChildren().size() >= 4) break;
                }
            }
        }
    }

    private VBox createPartnerCard(String name, String category, double earned, double maxRate) {
        VBox card = new VBox();
        card.getStyleClass().add("partner-card");
        card.setPrefWidth(180);
        card.setSpacing(0);

        // Header
        HBox header = new HBox();
        header.getStyleClass().addAll("partner-card-header", getPartnerHeaderClass(category));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);

        StackPane badge = new StackPane();
        badge.getStyleClass().add("partner-badge");
        FontIcon icon = new FontIcon(CashbackService.getCategoryIcon(category));
        icon.getStyleClass().add("partner-badge-icon");
        badge.getChildren().add(icon);

        VBox nameBox = new VBox();
        nameBox.setSpacing(1);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("partner-card-name");
        Label catLabel = new Label(category != null ? category : "General");
        catLabel.getStyleClass().add("partner-card-category");
        nameBox.getChildren().addAll(nameLabel, catLabel);

        header.getChildren().addAll(badge, nameBox);

        // Body
        VBox body = new VBox();
        body.getStyleClass().add("partner-card-body");
        body.setSpacing(8);

        HBox rateRow = createInfoRow("Cashback rate", "Up to " + String.format("%.0f", maxRate) + "%",
                                     "partner-rate-label", "partner-rate-value");
        HBox earningsRow = createInfoRow("Vos gains", formatCurrency(earned),
                                         "partner-earnings-label", "partner-earnings-value");

        body.getChildren().addAll(rateRow, earningsRow);
        card.getChildren().addAll(header, body);

        return card;
    }

    private HBox createInfoRow(String label, String value, String labelClass, String valueClass) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.getStyleClass().add(labelClass);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label val = new Label(value);
        val.getStyleClass().add(valueClass);

        row.getChildren().addAll(lbl, spacer, val);
        return row;
    }

    private String getPartnerHeaderClass(String category) {
        if (category == null) return "partner-header-default";
        switch (category.toLowerCase()) {
            case "voyage": return "partner-header-travel";
            case "shopping": return "partner-header-shopping";
            case "restauration": return "partner-header-dining";
            case "carburant": return "partner-header-fuel";
            case "divertissement": return "partner-header-entertainment";
            case "technologie": return "partner-header-tech";
            default: return "partner-header-default";
        }
    }

    // ═══════════════════ CASHBACK HISTORY ═══════════════════

    private void loadCashbackHistory() {
        if (currentUserId <= 0) return;

        List<Cashback> cashbacks = cashbackService.getCashbacksByUserId(currentUserId);
        String filter = filterPeriod != null ? filterPeriod.getValue() : null;

        if (filter != null && !filter.equals("All time") && !filter.equals("Toute periode")) {
            LocalDate cutoff = LocalDate.now();
            switch (filter) {
                case "Today": cutoff = LocalDate.now(); break;
                case "This week": cutoff = LocalDate.now().minusDays(7); break;
                case "This month": cutoff = LocalDate.now().withDayOfMonth(1); break;
            }
            final LocalDate finalCutoff = cutoff;
            cashbacks.removeIf(c -> {
                LocalDate date = parseDate(c.getDateAchat());
                return date != null && date.isBefore(finalCutoff);
            });
        }

        buildCashbackHistory(cashbacks);
    }

    private void buildCashbackHistory(List<Cashback> cashbacks) {
        if (cashbackListContainer == null) return;
        cashbackListContainer.getChildren().clear();

        if (cashbacks.isEmpty()) {
            VBox emptyHist = new VBox();
            emptyHist.setAlignment(Pos.CENTER);
            emptyHist.setSpacing(12);
            emptyHist.setPadding(new Insets(32));
            emptyHist.getStyleClass().add("empty-history");

            FontIcon emptyIcon = new FontIcon("fas-inbox");
            emptyIcon.getStyleClass().add("empty-history-icon");

            Label emptyLabel = new Label("No cashback transactions found");
            emptyLabel.getStyleClass().add("empty-history-label");

            Label emptyHint = new Label("Your cashback rewards will appear here as you shop with our partners");
            emptyHint.getStyleClass().add("empty-history-hint");
            emptyHint.setWrapText(true);

            emptyHist.getChildren().addAll(emptyIcon, emptyLabel, emptyHint);
            cashbackListContainer.getChildren().add(emptyHist);
            return;
        }

        for (Cashback cb : cashbacks) {
            HBox item = createCashbackItem(cb);
            cashbackListContainer.getChildren().add(item);
        }

        // Animate items in
        animateListItems();
    }

    private HBox createCashbackItem(Cashback cb) {
        HBox item = new HBox();
        item.setSpacing(14);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("cashback-item");

        // Add status-specific style
        String statut = cb.getStatut() != null ? cb.getStatut() : "";
        switch (statut) {
            case "En attente":
            case "Valide":
                item.getStyleClass().add("cashback-item-pending");
                break;
            case "Expire":
            case "Annule":
                item.getStyleClass().add("cashback-item-expired");
                break;
        }

        // Icon
        String category = cb.getPartenaireCategorie();
        StackPane iconPane = new StackPane();
        iconPane.getStyleClass().addAll("cashback-item-icon", getCashbackIconClass(statut, category));
        FontIcon icon;
        if ("Expire".equals(statut) || "Annule".equals(statut)) {
            icon = new FontIcon("fas-times");
        } else {
            icon = new FontIcon(CashbackService.getCategoryIcon(category));
        }
        icon.getStyleClass().add("cashback-item-icon-glyph");
        iconPane.getChildren().add(icon);

        // Details
        VBox details = new VBox();
        details.setSpacing(4);
        HBox.setHgrow(details, Priority.ALWAYS);

        HBox nameRow = new HBox();
        nameRow.setSpacing(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        String partnerName = cb.getPartenaireNom() != null ? cb.getPartenaireNom() : "Partner #" + cb.getIdPartenaire();
        Label merchantLabel = new Label(partnerName);
        merchantLabel.getStyleClass().add("cashback-item-merchant");

        StackPane badge = new StackPane();
        badge.getStyleClass().addAll("cashback-badge", getStatusBadgeClass(statut));
        Label badgeLabel = new Label(getStatusDisplayText(statut));
        badge.getChildren().add(badgeLabel);

        nameRow.getChildren().addAll(merchantLabel, badge);

        HBox infoRow = new HBox();
        infoRow.setSpacing(16);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        Label purchaseLabel = new Label("Achat: " + formatCurrency(cb.getMontantAchat()));
        purchaseLabel.getStyleClass().add("cashback-item-purchase");

        Label separator = new Label("-");
        separator.getStyleClass().add("cashback-separator");

        Label dateLabel = new Label(formatDisplayDate(cb.getDateAchat()));
        dateLabel.getStyleClass().add("cashback-item-date");

        infoRow.getChildren().addAll(purchaseLabel, separator, dateLabel);
        details.getChildren().addAll(nameRow, infoRow);

        // Amount
        VBox amountBox = new VBox();
        amountBox.setAlignment(Pos.CENTER_RIGHT);
        amountBox.setSpacing(2);

        String amountPrefix = ("Expire".equals(statut) || "Annule".equals(statut)) ? "" : "+";
        Label amountLabel = new Label(amountPrefix + formatCurrency(cb.getMontantCashback()));
        amountLabel.getStyleClass().addAll("cashback-item-amount", getAmountClass(statut));

        Label rateLabel = new Label(
            ("Expire".equals(statut)) ? "Non réclamé" : "Taux " + String.format("%.0f", cb.getTauxApplique()) + "%"
        );
        rateLabel.getStyleClass().add("cashback-item-rate");

        amountBox.getChildren().addAll(amountLabel, rateLabel);

        item.getChildren().addAll(iconPane, details, amountBox);
        return item;
    }

    // ═══════════════════ UI STATE MANAGEMENT ═══════════════════

    private void showLoading(boolean show) {
        if (loadingContainer != null) {
            loadingContainer.setVisible(show);
            loadingContainer.setManaged(show);
        }
        if (contentContainer != null) {
            contentContainer.setVisible(!show);
            contentContainer.setManaged(!show);
        }
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisible(false);
            emptyStateContainer.setManaged(false);
        }
    }

    private void showContent() {
        if (loadingContainer != null) {
            loadingContainer.setVisible(false);
            loadingContainer.setManaged(false);
        }
        if (contentContainer != null) {
            contentContainer.setVisible(true);
            contentContainer.setManaged(true);
            animateFadeIn(contentContainer);
        }
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisible(false);
            emptyStateContainer.setManaged(false);
        }
    }

    private void showEmptyState(String title, String message) {
        if (loadingContainer != null) {
            loadingContainer.setVisible(false);
            loadingContainer.setManaged(false);
        }
        if (contentContainer != null) {
            contentContainer.setVisible(false);
            contentContainer.setManaged(false);
        }
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisible(true);
            emptyStateContainer.setManaged(true);
            animateFadeIn(emptyStateContainer);
        }
    }

    // ═══════════════════ ANIMATIONS ═══════════════════

    private void animateFadeIn(javafx.scene.Node node) {
        if (node == null) return;
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        fade.play();
    }

    private void animateListItems() {
        if (cashbackListContainer == null) return;
        for (int i = 0; i < cashbackListContainer.getChildren().size(); i++) {
            javafx.scene.Node child = cashbackListContainer.getChildren().get(i);
            child.setOpacity(0);
            child.setTranslateY(10);

            FadeTransition fade = new FadeTransition(Duration.millis(200), child);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(i * 50));
            fade.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition slide = new TranslateTransition(Duration.millis(200), child);
            slide.setFromY(10);
            slide.setToY(0);
            slide.setDelay(Duration.millis(i * 50));
            slide.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(fade, slide).play();
        }
    }

    // ═══════════════════ HELPERS ═══════════════════

    private void setVisibleManaged(VBox node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private String formatCurrency(double amount) {
        return String.format("%.2f DT", amount);
    }

    private String formatDisplayDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "-";
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalDate today = LocalDate.now();
            if (date.equals(today)) return "Aujourd'hui";
            if (date.equals(today.minusDays(1))) return "Hier";
            return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        } catch (DateTimeParseException e) {
            return dateStr;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String getStatusBadgeClass(String statut) {
        if (statut == null) return "badge-pending";
        switch (statut) {
            case "Credite": return "badge-credited";
            case "Valide": return "badge-validated";
            case "En attente": return "badge-pending";
            case "Expire": return "badge-expired";
            case "Annule": return "badge-cancelled";
            case "Echange": return "badge-redeemed";
            default: return "badge-pending";
        }
    }

    private String getStatusDisplayText(String statut) {
        if (statut == null) return "En attente";
        switch (statut) {
            case "Credite": return "Crédité";
            case "Valide": return "Validé";
            case "En attente": return "En attente";
            case "Expire": return "Expiré";
            case "Annule": return "Annulé";
            case "Echange": return "Échange";
            default: return statut;
        }
    }

    private String getAmountClass(String statut) {
        if (statut == null) return "amount-pending";
        switch (statut) {
            case "Credite": return "amount-credited";
            case "Valide": return "amount-validated";
            case "En attente": return "amount-pending";
            case "Expire":
            case "Annule": return "amount-expired";
            case "Echange": return "amount-redeemed";
            default: return "amount-pending";
        }
    }

    private String getCashbackIconClass(String statut, String category) {
        if ("Expire".equals(statut) || "Annule".equals(statut)) return "cashback-icon-expired";
        if (category == null) return "cashback-icon-default";
        switch (category.toLowerCase()) {
            case "voyage": return "cashback-icon-travel";
            case "shopping": return "cashback-icon-shopping";
            case "restauration": return "cashback-icon-dining";
            case "carburant": return "cashback-icon-fuel";
            case "divertissement": return "cashback-icon-entertainment";
            case "technologie": return "cashback-icon-tech";
            default: return "cashback-icon-default";
        }
    }

    private void showNotification(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/UserDashboard.css").toExternalForm()
        );
        alert.show();
    }
}
