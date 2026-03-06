package com.nexora.bank.controllers;

import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.Cashback;
import com.nexora.bank.Models.CompteBancaire;
import com.nexora.bank.Models.Credit;
import com.nexora.bank.Models.Reclamation;
import com.nexora.bank.Models.Transaction;
import com.nexora.bank.Models.User;
import com.nexora.bank.Service.CashbackService;
import com.nexora.bank.Service.CompteBancaireService;
import com.nexora.bank.Service.CreditService;
import com.nexora.bank.Service.ReclamationService;
import com.nexora.bank.Service.TransactionService;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Controller for the Client Dashboard Overview Section.
 * All fx:id and onAction handlers match the FXML.
 */
public class UserDashboardOverviewSectionController {

    // ══════════════════════════════════════════════════════════════════════════════
    // FXML INJECTED FIELDS - Must match fx:id in FXML exactly
    // ══════════════════════════════════════════════════════════════════════════════

    // Header
    @FXML private Label lblWelcomeName;
    @FXML private Label lblLastRefresh;
    @FXML private Label lblRefreshDate;
    @FXML private Button btnRefresh;
    @FXML private FontIcon iconRefresh;

    // Hero Balance
    @FXML private Label lblTotalBalanceValue;
    @FXML private Label lblAccountsValue;
    @FXML private Label lblAccountStatus;
    @FXML private Label lblBalanceTrend;
    @FXML private StackPane trendIndicator;
    @FXML private FontIcon trendIcon;

    // KPI Metrics
    @FXML private Label lblTransactionsValue;
    @FXML private Label lblCreditsValue;
    @FXML private Label lblCashbackValue;
    @FXML private Label lblReclamationsValue;
    @FXML private Label lblTxComparison;
    @FXML private Label lblMonthlyDueValue;
    @FXML private Label lblCashbackCount;
    @FXML private Label lblRecOpen;
    @FXML private Label lblRecResolved;
    @FXML private Label lblPeriodIndicator;
    @FXML private ProgressBar txProgressBar;

    // Finance Panel
    @FXML private Label lblFinanceMonth;
    @FXML private Label lblMoneyInValue;
    @FXML private Label lblMoneyOutValue;
    @FXML private Label lblNetBalance;
    @FXML private Label lblNetTrend;
    @FXML private FontIcon netTrendIcon;
    @FXML private ProgressBar incomeProgress;
    @FXML private ProgressBar expenseProgress;

    // Service Status Panel
    @FXML private Label lblOverallStatus;
    @FXML private StackPane overallStatusBadge;
    @FXML private Label lblAccountHealth;
    @FXML private Label lblTransactionHealth;
    @FXML private Label lblCreditHealth;
    @FXML private Label lblCashbackHealth;
    @FXML private Label lblReclamationHealth;
    @FXML private Circle accountStatusDot;
    @FXML private Circle transactionStatusDot;
    @FXML private Circle creditStatusDot;
    @FXML private Circle cashbackStatusDot;
    @FXML private Circle reclamationStatusDot;

    // Recent Activities
    @FXML private Label lblRecentTransaction;
    @FXML private Label lblRecentCredit;
    @FXML private Label lblRecentCashback;
    @FXML private Label lblRecentReclamation;

    // Account Card
    @FXML private Label lblTopAccountNumber;
    @FXML private Label lblCardHolder;
    @FXML private Label lblTopAccountValue;

    // ══════════════════════════════════════════════════════════════════════════════
    // SERVICES
    // ══════════════════════════════════════════════════════════════════════════════

    private final CompteBancaireService compteService = new CompteBancaireService();
    private final TransactionService transactionService = new TransactionService();
    private final CreditService creditService = new CreditService();
    private final CashbackService cashbackService = new CashbackService();
    private final ReclamationService reclamationService = new ReclamationService();

    // ══════════════════════════════════════════════════════════════════════════════
    // FORMATTERS
    // ══════════════════════════════════════════════════════════════════════════════

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter REFRESH_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    private final DecimalFormat currencyFmt;
    private final DecimalFormat percentFmt;

    public UserDashboardOverviewSectionController() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator('.');
        currencyFmt = new DecimalFormat("#,##0.00", symbols);
        percentFmt = new DecimalFormat("+#,##0.0;-#,##0.0", symbols);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ══════════════════════════════════════════════════════════════════════════════

    @FXML
    private void initialize() {
        setupStaticUI();
        refreshOverview();
    }

    private void setupStaticUI() {
        LocalDate now = LocalDate.now();
        setText(lblFinanceMonth, capitalize(now.format(MONTH_FMT)));
        setText(lblPeriodIndicator, "Periode: " + now.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // PUBLIC REFRESH METHOD - Called by UserDashboardController
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Public method to refresh dashboard data.
     * Called by UserDashboardController when switching sections or refreshing.
     */
    public void refreshOverview() {
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null) {
            setDisconnectedState();
            return;
        }

        CompletableFuture.runAsync(() -> {
            int userId = currentUser.getIdUser();

            List<CompteBancaire> accounts = safeList(() -> compteService.getByUser(userId));
            List<Transaction> transactions = safeList(() -> transactionService.getByUser(userId));
            List<Credit> credits = safeList(() -> creditService.getCreditsByUser(userId));
            List<Cashback> cashbacks = safeList(() -> cashbackService.getCashbacksByUser(userId));
            List<Reclamation> reclamations = safeList(() -> reclamationService.getByUser(userId));

            Platform.runLater(() -> {
                updateUI(currentUser, accounts, transactions, credits, cashbacks, reclamations);
            });
        });
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // EVENT HANDLERS - Must match onAction in FXML
    // ══════════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleRefresh() {
        playRefreshAnimation();
        refreshOverview();
    }

    @FXML
    private void handleQuickTransfer() {
        System.out.println("[Dashboard] Quick Transfer action");
    }

    @FXML
    private void handleViewAccounts() {
        System.out.println("[Dashboard] View Accounts action");
    }

    @FXML
    private void handleClaimCashback() {
        System.out.println("[Dashboard] Claim Cashback action");
    }

    @FXML
    private void handleNewTransfer() {
        System.out.println("[Dashboard] New Transfer action");
    }

    @FXML
    private void handlePayBills() {
        System.out.println("[Dashboard] Pay Bills action");
    }

    @FXML
    private void handleRequestCredit() {
        System.out.println("[Dashboard] Request Credit action");
    }

    @FXML
    private void handleViewStatements() {
        System.out.println("[Dashboard] View Statements action");
    }

    @FXML
    private void handleContactSupport() {
        System.out.println("[Dashboard] Contact Support action");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // UI UPDATE
    // ══════════════════════════════════════════════════════════════════════════════

    private void updateUI(User user, List<CompteBancaire> accounts, List<Transaction> transactions,
                          List<Credit> credits, List<Cashback> cashbacks, List<Reclamation> reclamations) {

        LocalDate now = LocalDate.now();
        LocalDate lastMonth = now.minusMonths(1);

        // ═══ HEADER ═══
        setText(lblWelcomeName, buildUserName(user));
        setText(lblLastRefresh, "Derniere mise a jour");
        setText(lblRefreshDate, LocalDateTime.now().format(REFRESH_FMT));

        // ═══ BALANCE ═══
        double totalBalance = accounts.stream().mapToDouble(CompteBancaire::getSolde).sum();
        long activeAccounts = accounts.stream().filter(c -> isAccountActive(c.getStatutCompte())).count();

        setText(lblTotalBalanceValue, currencyFmt.format(totalBalance));
        setText(lblAccountsValue, String.valueOf(accounts.size()));
        setText(lblAccountStatus, activeAccounts == accounts.size() ? "Operationnels" : activeAccounts + "/" + accounts.size() + " actifs");

        // ═══ TRANSACTIONS ═══
        long txThisMonth = transactions.stream()
                .filter(t -> isSameMonth(t.getDateTransaction(), now)).count();
        long txLastMonth = transactions.stream()
                .filter(t -> isSameMonth(t.getDateTransaction(), lastMonth)).count();

        double moneyIn = transactions.stream()
                .filter(t -> isSameMonth(t.getDateTransaction(), now) && isMoneyIn(t.getTypeTransaction()))
                .mapToDouble(t -> safeAmount(t.getMontant())).sum();
        double moneyOut = transactions.stream()
                .filter(t -> isSameMonth(t.getDateTransaction(), now) && !isMoneyIn(t.getTypeTransaction()))
                .mapToDouble(t -> safeAmount(t.getMontant())).sum();

        long txPending = transactions.stream().filter(t -> isPending(t.getStatutTransaction())).count();

        setText(lblTransactionsValue, String.valueOf(txThisMonth));
        double txProgress = txLastMonth > 0 ? Math.min((double) txThisMonth / (txLastMonth * 1.5), 1.0) : 0.5;
        if (txProgressBar != null) txProgressBar.setProgress(txProgress);

        long txDiff = txThisMonth - txLastMonth;
        setText(lblTxComparison, "vs. mois dernier: " + (txDiff >= 0 ? "+" : "") + txDiff);

        // ═══ TREND ═══
        double netChange = moneyIn - moneyOut;
        double trendPct = totalBalance > 0 ? (netChange / totalBalance) * 100 : 0;
        updateTrendUI(trendPct);

        // ═══ CREDITS ═══
        long activeCredits = credits.stream().filter(c -> isCreditActive(c.getStatut())).count();
        double monthlyDue = credits.stream()
                .filter(c -> isCreditActive(c.getStatut()))
                .mapToDouble(Credit::getMensualite).sum();
        long creditPending = credits.stream().filter(c -> isPending(c.getStatut())).count();

        setText(lblCreditsValue, String.valueOf(activeCredits));
        setText(lblMonthlyDueValue, "Mensualites: " + currencyFmt.format(monthlyDue) + " DT");

        // ═══ CASHBACK ═══
        double cashbackAmount = cashbacks.stream()
                .filter(c -> isCashbackPending(c.getStatut()))
                .mapToDouble(Cashback::getMontantCashback).sum();
        long cashbackCount = cashbacks.stream().filter(c -> isCashbackPending(c.getStatut())).count();

        setText(lblCashbackValue, currencyFmt.format(cashbackAmount));
        setText(lblCashbackCount, cashbackCount + " recompenses en attente");

        // ═══ RECLAMATIONS ═══
        long recOpen = reclamations.stream().filter(r -> isReclamationOpen(r.getStatus())).count();
        long recResolved = reclamations.stream()
                .filter(r -> normalized(r.getStatus()).contains("resolu")).count();

        setText(lblReclamationsValue, String.valueOf(recOpen));
        setText(lblRecOpen, recOpen + " ouvertes");
        setText(lblRecResolved, recResolved + " resolues");

        // ═══ FINANCE PANEL ═══
        updateFinancePanel(moneyIn, moneyOut);

        // ═══ SERVICE STATUS ═══
        updateServiceStatus(activeAccounts, accounts.size(), txPending, creditPending, cashbackCount, recOpen);

        // ═══ RECENT ACTIVITIES ═══
        updateRecentActivities(transactions, credits, cashbacks, reclamations);

        // ═══ ACCOUNT CARD ═══
        updateAccountCard(user, accounts);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // UI UPDATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════════

    private void updateTrendUI(double pct) {
        boolean isPositive = pct >= 0;

        if (trendIndicator != null) {
            trendIndicator.getStyleClass().removeAll("ov-trend-up", "ov-trend-down");
            trendIndicator.getStyleClass().add(isPositive ? "ov-trend-up" : "ov-trend-down");
        }

        if (trendIcon != null) {
            trendIcon.setIconLiteral(isPositive ? "fas-arrow-up" : "fas-arrow-down");
        }
        
        setText(lblBalanceTrend, percentFmt.format(pct) + "% ce mois");
    }

    private void updateFinancePanel(double moneyIn, double moneyOut) {
        setText(lblMoneyInValue, "+" + currencyFmt.format(moneyIn) + " DT");
        setText(lblMoneyOutValue, "-" + currencyFmt.format(moneyOut) + " DT");

        double netBalance = moneyIn - moneyOut;
        boolean isPositive = netBalance >= 0;

        setText(lblNetBalance, (isPositive ? "+" : "") + currencyFmt.format(Math.abs(netBalance)) + " DT");
        
        if (lblNetBalance != null) {
            lblNetBalance.getStyleClass().removeAll("ov-net-value", "ov-net-value-negative");
            lblNetBalance.getStyleClass().add(isPositive ? "ov-net-value" : "ov-net-value-negative");
        }

        double maxAmt = Math.max(moneyIn, moneyOut);
        if (maxAmt > 0) {
            if (incomeProgress != null) incomeProgress.setProgress(moneyIn / maxAmt);
            if (expenseProgress != null) expenseProgress.setProgress(moneyOut / maxAmt);
        } else {
            if (incomeProgress != null) incomeProgress.setProgress(0);
            if (expenseProgress != null) expenseProgress.setProgress(0);
        }

        double trendPct = (moneyIn + moneyOut) > 0 ? (netBalance / (moneyIn + moneyOut)) * 100 : 0;
        setText(lblNetTrend, percentFmt.format(Math.abs(trendPct)) + "%");

        if (netTrendIcon != null) {
            netTrendIcon.setIconLiteral(isPositive ? "fas-arrow-up" : "fas-arrow-down");
            netTrendIcon.getStyleClass().removeAll("ov-net-trend-icon", "ov-net-trend-icon-negative");
            netTrendIcon.getStyleClass().add(isPositive ? "ov-net-trend-icon" : "ov-net-trend-icon-negative");
        }

        if (lblNetTrend != null) {
            lblNetTrend.getStyleClass().removeAll("ov-net-trend-text", "ov-net-trend-text-negative");
            lblNetTrend.getStyleClass().add(isPositive ? "ov-net-trend-text" : "ov-net-trend-text-negative");
        }
    }

    private void updateServiceStatus(long activeAccounts, int totalAccounts, long txPending,
                                      long creditPending, long cashbackPending, long recOpen) {
        // Overall status
        boolean allGood = txPending == 0 && creditPending == 0 && recOpen == 0;
        boolean hasWarnings = txPending > 0 || creditPending > 0;

        if (overallStatusBadge != null) {
            overallStatusBadge.getStyleClass().removeAll("ov-status-ok", "ov-status-warn", "ov-status-error");
            if (allGood) {
                overallStatusBadge.getStyleClass().add("ov-status-ok");
                setText(lblOverallStatus, "Operationnel");
            } else if (hasWarnings) {
                overallStatusBadge.getStyleClass().add("ov-status-warn");
                setText(lblOverallStatus, "Attention requise");
            } else {
                overallStatusBadge.getStyleClass().add("ov-status-error");
                setText(lblOverallStatus, "Action requise");
            }
        }

        // Individual services
        setText(lblAccountHealth, activeAccounts + "/" + totalAccounts + " actifs");
        setStatusDot(accountStatusDot, activeAccounts == totalAccounts);

        setText(lblTransactionHealth, txPending > 0 ? txPending + " en cours" : "Toutes traitees");
        setStatusDot(transactionStatusDot, txPending == 0);

        setText(lblCreditHealth, creditPending > 0 ? creditPending + " en attente" : "A jour");
        setStatusDot(creditStatusDot, creditPending == 0);

        setText(lblCashbackHealth, cashbackPending > 0 ? cashbackPending + " disponible(s)" : "Aucun");
        setStatusDot(cashbackStatusDot, true);

        setText(lblReclamationHealth, recOpen > 0 ? recOpen + " ticket(s)" : "Aucun ticket");
        setStatusDot(reclamationStatusDot, recOpen == 0);
    }

    private void setStatusDot(Circle dot, boolean isActive) {
        if (dot != null) {
            dot.getStyleClass().removeAll("ov-dot-active", "ov-dot-warning", "ov-dot-error");
            dot.getStyleClass().add(isActive ? "ov-dot-active" : "ov-dot-warning");
        }
    }

    private void updateRecentActivities(List<Transaction> transactions, List<Credit> credits,
                                         List<Cashback> cashbacks, List<Reclamation> reclamations) {
        // Latest Transaction
        Transaction latestTx = transactions.stream()
                .filter(t -> t.getDateTransaction() != null)
                .max(Comparator.comparing(Transaction::getDateTransaction)
                        .thenComparing(Transaction::getIdTransaction))
                .orElse(null);
        setText(lblRecentTransaction, formatTransaction(latestTx));

        // Latest Credit
        Credit latestCredit = credits.stream()
                .max(Comparator.comparing(c -> safeDate(parseDate(c.getDateDemande()))))
                .orElse(null);
        setText(lblRecentCredit, formatCredit(latestCredit));

        // Latest Cashback
        Cashback latestCashback = cashbacks.stream()
                .max(Comparator.comparing(this::getCashbackDate))
                .orElse(null);
        setText(lblRecentCashback, formatCashback(latestCashback));

        // Latest Reclamation
        Reclamation latestRec = reclamations.stream()
                .max(Comparator.comparing(r -> safeDate(parseDate(r.getDateReclamation()))))
                .orElse(null);
        setText(lblRecentReclamation, formatReclamation(latestRec));
    }

    private void updateAccountCard(User user, List<CompteBancaire> accounts) {
        CompteBancaire topAccount = accounts.stream()
                .max(Comparator.comparingDouble(CompteBancaire::getSolde))
                .orElse(null);

        if (topAccount != null) {
            String num = safeText(topAccount.getNumeroCompte());
            setText(lblTopAccountNumber, maskAccountNumber(num));
            setText(lblTopAccountValue, currencyFmt.format(topAccount.getSolde()) + " DT");
        } else {
            setText(lblTopAccountNumber, "**** **** **** ----");
            setText(lblTopAccountValue, "0.00 DT");
        }

        setText(lblCardHolder, buildUserName(user).toUpperCase());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // ANIMATIONS
    // ══════════════════════════════════════════════════════════════════════════════

    private void playRefreshAnimation() {
        if (iconRefresh != null) {
            RotateTransition rotate = new RotateTransition(Duration.millis(600), iconRefresh);
            rotate.setByAngle(360);
            rotate.setCycleCount(1);
            rotate.play();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // DISCONNECTED STATE
    // ══════════════════════════════════════════════════════════════════════════════

    private void setDisconnectedState() {
        setText(lblWelcomeName, "Non connecte");
        setText(lblTotalBalanceValue, "0.00");
        setText(lblAccountsValue, "0");
        setText(lblAccountStatus, "Non disponible");
        setText(lblBalanceTrend, "+0.00% ce mois");
        setText(lblTransactionsValue, "0");
        setText(lblCreditsValue, "0");
        setText(lblCashbackValue, "0.00");
        setText(lblReclamationsValue, "0");
        setText(lblTxComparison, "vs. mois dernier: --");
        setText(lblMonthlyDueValue, "Mensualites: 0.00 DT");
        setText(lblCashbackCount, "0 recompenses");
        setText(lblRecOpen, "0 ouvertes");
        setText(lblRecResolved, "0 resolues");
        setText(lblMoneyInValue, "+0.00 DT");
        setText(lblMoneyOutValue, "-0.00 DT");
        setText(lblNetBalance, "+0.00 DT");
        setText(lblNetTrend, "+0%");
        setText(lblOverallStatus, "Non connecte");
        setText(lblAccountHealth, "--");
        setText(lblTransactionHealth, "--");
        setText(lblCreditHealth, "--");
        setText(lblCashbackHealth, "--");
        setText(lblReclamationHealth, "--");
        setText(lblRecentTransaction, "Aucune transaction");
        setText(lblRecentCredit, "Aucun credit");
        setText(lblRecentCashback, "Aucun cashback");
        setText(lblRecentReclamation, "Aucune reclamation");
        setText(lblTopAccountNumber, "**** **** **** ----");
        setText(lblCardHolder, "NOM PRENOM");
        setText(lblTopAccountValue, "0.00 DT");
        setText(lblRefreshDate, "--/--/---- --:--");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ══════════════════════════════════════════════════════════════════════════════

    private <T> List<T> safeList(Supplier<List<T>> supplier) {
        try {
            List<T> list = supplier.get();
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("[Dashboard] Data fetch error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private boolean isSameMonth(LocalDate date, LocalDate ref) {
        return date != null && ref != null
                && date.getYear() == ref.getYear()
                && date.getMonthValue() == ref.getMonthValue();
    }

    private boolean isAccountActive(String status) {
        String s = normalized(status);
        return s.contains("actif") || s.contains("active");
    }

    private boolean isMoneyIn(String type) {
        String s = normalized(type);
        return s.contains("credit") || s.contains("depot") || s.contains("versement") || s.contains("recu");
    }

    private boolean isCreditActive(String status) {
        String s = normalized(status);
        if (s.isBlank()) return false;
        return !(s.contains("refuse") || s.contains("rejete") || s.contains("termine"));
    }

    private boolean isCashbackPending(String status) {
        String s = normalized(status);
        return s.contains("attente") || s.contains("pending") || s.contains("valide");
    }

    private boolean isReclamationOpen(String status) {
        String s = normalized(status);
        return s.contains("attente") || s.contains("cours") || s.contains("signale") || s.contains("ouverte");
    }

    private boolean isPending(String status) {
        String s = normalized(status);
        return s.contains("attente") || s.contains("pending") || s.contains("cours") || s.contains("processing");
    }

    private String normalized(String text) {
        if (text == null) return "";
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).trim();
    }

    private double safeAmount(Double val) {
        return val == null ? 0.0 : Math.max(val, 0.0);
    }

    private LocalDate parseDate(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return LocalDate.parse(val.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate getCashbackDate(Cashback c) {
        if (c == null) return LocalDate.MIN;
        if (c.getDateAchat() != null) return c.getDateAchat();
        if (c.getCreatedAt() != null) return c.getCreatedAt().toLocalDate();
        return LocalDate.MIN;
    }

    private LocalDate safeDate(LocalDate d) {
        return d == null ? LocalDate.MIN : d;
    }

    private String buildUserName(User user) {
        if (user == null) return "Client";
        String first = safeText(user.getPrenom());
        String last = safeText(user.getNom());
        String full = (first + " " + last).trim();
        return full.isBlank() ? "Client #" + user.getIdUser() : full;
    }

    private String maskAccountNumber(String num) {
        if (num == null || num.length() < 4) return "**** **** **** ----";
        String last4 = num.substring(num.length() - 4);
        return "**** **** **** " + last4;
    }

    private String formatTransaction(Transaction tx) {
        if (tx == null) return "Aucune transaction";
        String date = tx.getDateTransaction() != null ? " - " + tx.getDateTransaction().format(DATE_FMT) : "";
        return safeText(tx.getTypeTransaction()) + " " + currencyFmt.format(safeAmount(tx.getMontant())) + " DT" + date;
    }

    private String formatCredit(Credit c) {
        if (c == null) return "Aucun credit";
        LocalDate d = parseDate(c.getDateDemande());
        String date = d != null ? " - " + d.format(DATE_FMT) : "";
        return safeText(c.getStatut()) + " " + currencyFmt.format(c.getMontantDemande()) + " DT" + date;
    }

    private String formatCashback(Cashback c) {
        if (c == null) return "Aucun cashback";
        LocalDate d = getCashbackDate(c);
        String date = !d.equals(LocalDate.MIN) ? " - " + d.format(DATE_FMT) : "";
        return safeText(c.getStatut()) + " " + currencyFmt.format(c.getMontantCashback()) + " DT" + date;
    }

    private String formatReclamation(Reclamation r) {
        if (r == null) return "Aucune reclamation";
        LocalDate d = parseDate(r.getDateReclamation());
        String date = d != null ? " - " + d.format(DATE_FMT) : "";
        return safeText(r.getStatus()) + " - " + safeText(r.getTypeReclamation()) + date;
    }

    private String safeText(String val) {
        return val == null || val.isBlank() ? "-" : val.trim();
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private void setText(Label label, String value) {
        if (label != null) label.setText(value);
    }
}