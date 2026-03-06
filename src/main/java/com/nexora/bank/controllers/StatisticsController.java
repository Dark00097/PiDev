package com.nexora.bank.controllers;

import com.myapp.config.AdminSecuritySettings;
import com.myapp.config.AdminSecuritySettingsStore;
import com.nexora.bank.Models.User;
import com.nexora.bank.Service.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class StatisticsController implements Initializable {

    public enum StatisticsContext {
        USER_MANAGEMENT,
        ADMIN_SECURITY,
        GENERIC
    }

    private static final String[] MONTH_LABELS = {
        "Jan", "Fev", "Mar", "Avr", "Mai", "Jun", "Jul", "Aou", "Sep", "Oct", "Nov", "Dec"
    };

    @FXML private Label lblTotalRevenu;
    @FXML private Label lblTotalClients;
    @FXML private Label lblTotalTransactions;
    @FXML private Label lblCreditsApprouves;

    @FXML private ComboBox<String> cmbPeriode;
    @FXML private LineChart<String, Number> chartTransactions;
    @FXML private PieChart chartComptes;
    @FXML private BarChart<String, Number> chartCredits;
    @FXML private ListView<String> listActivites;

    private final UserService userService = new UserService();
    private final AdminSecuritySettingsStore settingsStore = new AdminSecuritySettingsStore();

    private StatisticsContext context = StatisticsContext.GENERIC;
    private boolean initialized;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initialized = true;
        if (cmbPeriode != null) {
            cmbPeriode.valueProperty().addListener((obs, oldVal, newVal) -> loadContextualData());
        }
        loadContextualData();
    }

    public void setContext(StatisticsContext context) {
        this.context = context == null ? StatisticsContext.GENERIC : context;
        if (initialized) {
            loadContextualData();
        }
    }

    private void loadContextualData() {
        List<User> users = fetchUsers();
        switch (context) {
            case USER_MANAGEMENT -> loadUserManagementStatistics(users);
            case ADMIN_SECURITY -> loadAdminSecurityStatistics(users);
            case GENERIC -> loadUserManagementStatistics(users);
        }
    }

    private void loadUserManagementStatistics(List<User> users) {
        long totalUsers = users.size();
        long activeUsers = users.stream().filter(u -> "ACTIVE".equalsIgnoreCase(safe(u.getStatus()))).count();
        long pendingUsers = users.stream().filter(u -> "PENDING".equalsIgnoreCase(safe(u.getStatus()))).count();
        long adminUsers = users.stream().filter(u -> "ROLE_ADMIN".equalsIgnoreCase(safe(u.getRole()))).count();

        lblTotalRevenu.setText(String.valueOf(totalUsers));
        lblTotalClients.setText(String.valueOf(activeUsers));
        lblTotalTransactions.setText(String.valueOf(pendingUsers));
        lblCreditsApprouves.setText(String.valueOf(adminUsers));

        loadUserLineChart(users);
        loadUserPieChart(users);
        loadUserBarChart(users);
        loadUserRecentActivities(users);
    }

    private void loadAdminSecurityStatistics(List<User> users) {
        AdminSecuritySettings settings = settingsStore.load();
        int enabledControls = 0;
        if (settings.isRequireBiometricOnAdminLogin()) {
            enabledControls++;
        }
        if (settings.isRequireBiometricOnSensitiveActions()) {
            enabledControls++;
        }
        if (settings.isEnableEmailOtp()) {
            enabledControls++;
        }

        long admins = users.stream().filter(u -> "ROLE_ADMIN".equalsIgnoreCase(safe(u.getRole()))).count();
        long activeAdmins = users.stream().filter(u ->
            "ROLE_ADMIN".equalsIgnoreCase(safe(u.getRole()))
                && "ACTIVE".equalsIgnoreCase(safe(u.getStatus()))
        ).count();

        int securityScore = Math.round((enabledControls / 3.0f) * 100f);

        lblTotalRevenu.setText(securityScore + "%");
        lblTotalClients.setText(String.valueOf(enabledControls));
        lblTotalTransactions.setText(String.valueOf(admins));
        lblCreditsApprouves.setText(String.valueOf(activeAdmins));

        loadAdminLineChart(users);
        loadAdminPieChart(settings);
        loadAdminBarChart(users);
        loadAdminRecentActivities(settings, admins, activeAdmins);
    }

    private void loadUserLineChart(List<User> users) {
        chartTransactions.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nouveaux utilisateurs");

        Map<Month, Integer> monthlyCounts = new EnumMap<>(Month.class);
        for (Month month : Month.values()) {
            monthlyCounts.put(month, 0);
        }

        LocalDateTime periodStart = resolvePeriodStart();
        for (User user : users) {
            LocalDateTime createdAt = parseDateTime(user.getCreatedAt());
            if (createdAt == null) {
                continue;
            }
            if (periodStart != null && createdAt.isBefore(periodStart)) {
                continue;
            }
            monthlyCounts.computeIfPresent(createdAt.getMonth(), (m, c) -> c + 1);
        }

        for (int i = 0; i < 12; i++) {
            Month month = Month.of(i + 1);
            series.getData().add(new XYChart.Data<>(MONTH_LABELS[i], monthlyCounts.get(month)));
        }

        chartTransactions.getData().add(series);
    }

    private void loadUserPieChart(List<User> users) {
        long roleUser = users.stream().filter(u -> "ROLE_USER".equalsIgnoreCase(safe(u.getRole()))).count();
        long roleAdmin = users.stream().filter(u -> "ROLE_ADMIN".equalsIgnoreCase(safe(u.getRole()))).count();
        long others = Math.max(0, users.size() - roleUser - roleAdmin);

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
            new PieChart.Data("Users", roleUser),
            new PieChart.Data("Admins", roleAdmin),
            new PieChart.Data("Autres", others)
        );
        chartComptes.setData(pieChartData);
    }

    private void loadUserBarChart(List<User> users) {
        chartCredits.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Utilisateurs");
        series.getData().add(new XYChart.Data<>("ACTIVE", countByStatus(users, "ACTIVE")));
        series.getData().add(new XYChart.Data<>("PENDING", countByStatus(users, "PENDING")));
        series.getData().add(new XYChart.Data<>("DECLINED", countByStatus(users, "DECLINED")));
        series.getData().add(new XYChart.Data<>("BANNED", countByStatus(users, "BANNED")));
        chartCredits.getData().add(series);
    }

    private void loadUserRecentActivities(List<User> users) {
        ObservableList<String> activities = FXCollections.observableArrayList();

        users.stream()
            .sorted(Comparator.comparing(this::userMostRecentDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(8)
            .forEach(user -> activities.add(buildUserActivity(user)));

        if (activities.isEmpty()) {
            activities.add("Aucune activite recente disponible.");
        }
        listActivites.setItems(activities);
    }

    private void loadAdminLineChart(List<User> users) {
        chartTransactions.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Admins actifs par mois");

        Map<Month, Integer> monthlyCounts = new EnumMap<>(Month.class);
        for (Month month : Month.values()) {
            monthlyCounts.put(month, 0);
        }

        for (User user : users) {
            if (!"ROLE_ADMIN".equalsIgnoreCase(safe(user.getRole()))) {
                continue;
            }
            LocalDateTime date = parseDateTime(user.getLastOnlineAt());
            if (date == null) {
                date = parseDateTime(user.getCreatedAt());
            }
            if (date == null) {
                continue;
            }
            monthlyCounts.computeIfPresent(date.getMonth(), (m, c) -> c + 1);
        }

        for (int i = 0; i < 12; i++) {
            Month month = Month.of(i + 1);
            series.getData().add(new XYChart.Data<>(MONTH_LABELS[i], monthlyCounts.get(month)));
        }

        chartTransactions.getData().add(series);
    }

    private void loadAdminPieChart(AdminSecuritySettings settings) {
        int enabled = 0;
        if (settings.isRequireBiometricOnAdminLogin()) enabled++;
        if (settings.isRequireBiometricOnSensitiveActions()) enabled++;
        if (settings.isEnableEmailOtp()) enabled++;
        int disabled = Math.max(0, 3 - enabled);

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
            new PieChart.Data("Bio Login", settings.isRequireBiometricOnAdminLogin() ? 1 : 0),
            new PieChart.Data("Actions Sensibles", settings.isRequireBiometricOnSensitiveActions() ? 1 : 0),
            new PieChart.Data("OTP Email", settings.isEnableEmailOtp() ? 1 : 0),
            new PieChart.Data("Desactive", disabled)
        );
        chartComptes.setData(pieChartData);
    }

    private void loadAdminBarChart(List<User> users) {
        chartCredits.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Comptes admin");
        series.getData().add(new XYChart.Data<>("ACTIVE", countByRoleAndStatus(users, "ROLE_ADMIN", "ACTIVE")));
        series.getData().add(new XYChart.Data<>("PENDING", countByRoleAndStatus(users, "ROLE_ADMIN", "PENDING")));
        series.getData().add(new XYChart.Data<>("INACTIVE", countByRoleAndStatus(users, "ROLE_ADMIN", "INACTIVE")));
        series.getData().add(new XYChart.Data<>("BANNED", countByRoleAndStatus(users, "ROLE_ADMIN", "BANNED")));
        chartCredits.getData().add(series);
    }

    private void loadAdminRecentActivities(AdminSecuritySettings settings, long admins, long activeAdmins) {
        List<String> activityItems = new ArrayList<>();
        activityItems.add("Admins total: " + admins);
        activityItems.add("Admins actifs: " + activeAdmins);
        activityItems.add("Biometrie connexion: " + toEnabledDisabled(settings.isRequireBiometricOnAdminLogin()));
        activityItems.add("Biometrie actions sensibles: " + toEnabledDisabled(settings.isRequireBiometricOnSensitiveActions()));
        activityItems.add("OTP Email: " + toEnabledDisabled(settings.isEnableEmailOtp()));
        activityItems.add("Configuration: " + settingsStore.getSettingsFilePath());
        listActivites.setItems(FXCollections.observableArrayList(activityItems));
    }

    private long countByStatus(List<User> users, String status) {
        return users.stream().filter(u -> status.equalsIgnoreCase(safe(u.getStatus()))).count();
    }

    private long countByRoleAndStatus(List<User> users, String role, String status) {
        return users.stream().filter(u ->
            role.equalsIgnoreCase(safe(u.getRole())) && status.equalsIgnoreCase(safe(u.getStatus()))
        ).count();
    }

    private String buildUserActivity(User user) {
        String name = (safe(user.getPrenom()) + " " + safe(user.getNom())).trim();
        if (name.isBlank()) {
            name = safe(user.getEmail());
        }
        String status = safe(user.getStatus()).isBlank() ? "UNKNOWN" : safe(user.getStatus()).toUpperCase(Locale.ROOT);
        LocalDateTime timestamp = userMostRecentDate(user);
        String when = timestamp == null ? "date inconnue" : timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        return name + " - statut " + status + " (" + when + ")";
    }

    private LocalDateTime userMostRecentDate(User user) {
        LocalDateTime lastOnline = parseDateTime(user.getLastOnlineAt());
        if (lastOnline != null) {
            return lastOnline;
        }
        return parseDateTime(user.getCreatedAt());
    }

    private List<User> fetchUsers() {
        try {
            return userService.getAllUsers();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private LocalDateTime resolvePeriodStart() {
        String period = cmbPeriode == null ? null : cmbPeriode.getValue();
        LocalDateTime now = LocalDateTime.now();
        if (period == null) {
            return null;
        }
        return switch (period) {
            case "Aujourd'hui" -> now.toLocalDate().atStartOfDay();
            case "Cette semaine" -> now.minusDays(6).toLocalDate().atStartOfDay();
            case "Ce mois" -> now.withDayOfMonth(1).toLocalDate().atStartOfDay();
            case "Ce trimestre" -> now.withMonth(((now.getMonthValue() - 1) / 3) * 3 + 1).withDayOfMonth(1).toLocalDate().atStartOfDay();
            case "Cette annÃ©e", "Cette année" -> now.withDayOfYear(1).toLocalDate().atStartOfDay();
            default -> null;
        };
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        List<DateTimeFormatter> formats = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        );
        for (DateTimeFormatter formatter : formats) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private String toEnabledDisabled(boolean enabled) {
        return enabled ? "active" : "desactive";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @FXML
    private void refreshStats() {
        loadContextualData();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Actualisation");
        alert.setHeaderText(null);
        alert.setContentText("Les statistiques ont Ã©tÃ© actualisÃ©es.");
        alert.showAndWait();
    }
}
