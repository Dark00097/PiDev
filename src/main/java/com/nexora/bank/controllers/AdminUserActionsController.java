package com.nexora.bank.controllers;

import com.nexora.bank.Models.User;
import com.nexora.bank.Models.UserActionLog;
import com.nexora.bank.Service.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminUserActionsController implements Initializable {

    private static final int ACTIONS_LIMIT = 2000;

    @FXML private Label lblTotalActions;
    @FXML private Label lblUniqueUsers;
    @FXML private Label lblTodayActions;
    @FXML private Label lblTableInfo;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbActionType;
    @FXML private Button btnRefresh;

    @FXML private TableView<ActionRow> tableActions;
    @FXML private TableColumn<ActionRow, String> colDate;
    @FXML private TableColumn<ActionRow, String> colUser;
    @FXML private TableColumn<ActionRow, String> colRole;
    @FXML private TableColumn<ActionRow, String> colAction;
    @FXML private TableColumn<ActionRow, String> colSource;
    @FXML private TableColumn<ActionRow, String> colDetails;

    private final UserService userService = new UserService();
    private final ObservableList<ActionRow> actionRows = FXCollections.observableArrayList();
    private FilteredList<ActionRow> filteredRows;
    private final Map<Integer, User> usersById = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        initializeFilters();
        loadActions();
    }

    private void initializeTable() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateDisplay"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("userDisplay"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colAction.setCellValueFactory(new PropertyValueFactory<>("actionType"));
        colSource.setCellValueFactory(new PropertyValueFactory<>("source"));
        colDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
    }

    private void initializeFilters() {
        filteredRows = new FilteredList<>(actionRows, row -> true);

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
        if (cmbActionType != null) {
            cmbActionType.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }

        SortedList<ActionRow> sortedRows = new SortedList<>(filteredRows);
        sortedRows.comparatorProperty().bind(tableActions.comparatorProperty());
        tableActions.setItems(sortedRows);
    }

    @FXML
    private void handleRefresh() {
        loadActions();
    }

    private void loadActions() {
        try {
            usersById.clear();
            for (User user : userService.getAllUsers()) {
                usersById.put(user.getIdUser(), user);
            }

            List<ActionRow> rows = userService.getAllUserActions(ACTIONS_LIMIT).stream()
                .map(this::toActionRow)
                .collect(Collectors.toList());

            actionRows.setAll(rows);
            updateActionTypeOptions(rows);
            applyFilters();
            updateStats(rows);
        } catch (Exception ex) {
            actionRows.clear();
            updateActionTypeOptions(List.of());
            updateStats(List.of());
            updateTableInfo(0, 0);
        }
    }

    private ActionRow toActionRow(UserActionLog action) {
        int userId = action.getIdUser();
        User user = usersById.get(userId);

        String userDisplay;
        String role;
        if (user != null) {
            String name = (safe(user.getPrenom()) + " " + safe(user.getNom())).trim();
            userDisplay = name.isBlank() ? safe(user.getEmail()) : name;
            role = safe(user.getRole()).isBlank() ? "UNKNOWN" : safe(user.getRole()).replace("ROLE_", "");
        } else {
            userDisplay = "User #" + userId;
            role = "UNKNOWN";
        }

        LocalDateTime timestamp = parseDateTime(action.getCreatedAt());
        String dateDisplay = timestamp == null
            ? safe(action.getCreatedAt())
            : timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        return new ActionRow(
            userId,
            dateDisplay,
            userDisplay,
            role,
            normalizeActionType(action.getActionType()),
            safe(action.getActionSource()),
            safe(action.getDetails()),
            timestamp
        );
    }

    private void applyFilters() {
        String needle = txtSearch == null ? "" : safe(txtSearch.getText()).toLowerCase(Locale.ROOT);
        String selectedType = cmbActionType == null ? "Tous" : safe(cmbActionType.getValue());

        filteredRows.setPredicate(row -> {
            boolean typeMatch = "Tous".equalsIgnoreCase(selectedType) || row.getActionType().equalsIgnoreCase(selectedType);
            if (!typeMatch) {
                return false;
            }
            if (needle.isBlank()) {
                return true;
            }
            return contains(row.getDateDisplay(), needle)
                || contains(row.getUserDisplay(), needle)
                || contains(row.getRole(), needle)
                || contains(row.getActionType(), needle)
                || contains(row.getSource(), needle)
                || contains(row.getDetails(), needle);
        });

        updateTableInfo(filteredRows.size(), actionRows.size());
    }

    private void updateActionTypeOptions(List<ActionRow> rows) {
        String previous = cmbActionType == null ? "Tous" : safe(cmbActionType.getValue());
        List<String> types = rows.stream()
            .map(ActionRow::getActionType)
            .filter(type -> !type.isBlank())
            .distinct()
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.toList());

        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("Tous");
        items.addAll(types);

        if (cmbActionType != null) {
            cmbActionType.setItems(items);
            cmbActionType.setValue(items.contains(previous) ? previous : "Tous");
        }
    }

    private void updateStats(List<ActionRow> rows) {
        long uniqueUsers = rows.stream().map(ActionRow::getUserId).filter(id -> id > 0).distinct().count();
        LocalDate today = LocalDate.now();
        long todayActions = rows.stream()
            .filter(row -> row.getTimestamp() != null && row.getTimestamp().toLocalDate().equals(today))
            .count();

        if (lblTotalActions != null) {
            lblTotalActions.setText(String.valueOf(rows.size()));
        }
        if (lblUniqueUsers != null) {
            lblUniqueUsers.setText(String.valueOf(uniqueUsers));
        }
        if (lblTodayActions != null) {
            lblTodayActions.setText(String.valueOf(todayActions));
        }
    }

    private void updateTableInfo(int filteredCount, int totalCount) {
        if (lblTableInfo != null) {
            lblTableInfo.setText("Affichage de " + filteredCount + " sur " + totalCount + " actions");
        }
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

    private String normalizeActionType(String value) {
        return safe(value).toUpperCase(Locale.ROOT);
    }

    private boolean contains(String value, String needle) {
        return safe(value).toLowerCase(Locale.ROOT).contains(needle);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static class ActionRow {
        private final int userId;
        private final SimpleStringProperty dateDisplay;
        private final SimpleStringProperty userDisplay;
        private final SimpleStringProperty role;
        private final SimpleStringProperty actionType;
        private final SimpleStringProperty source;
        private final SimpleStringProperty details;
        private final LocalDateTime timestamp;

        public ActionRow(
            int userId,
            String dateDisplay,
            String userDisplay,
            String role,
            String actionType,
            String source,
            String details,
            LocalDateTime timestamp
        ) {
            this.userId = userId;
            this.dateDisplay = new SimpleStringProperty(dateDisplay);
            this.userDisplay = new SimpleStringProperty(userDisplay);
            this.role = new SimpleStringProperty(role);
            this.actionType = new SimpleStringProperty(actionType);
            this.source = new SimpleStringProperty(source);
            this.details = new SimpleStringProperty(details);
            this.timestamp = timestamp;
        }

        public int getUserId() {
            return userId;
        }

        public String getDateDisplay() {
            return dateDisplay.get();
        }

        public String getUserDisplay() {
            return userDisplay.get();
        }

        public String getRole() {
            return role.get();
        }

        public String getActionType() {
            return actionType.get();
        }

        public String getSource() {
            return source.get();
        }

        public String getDetails() {
            return details.get();
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}
