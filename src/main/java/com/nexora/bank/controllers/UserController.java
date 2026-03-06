package com.nexora.bank.controllers;

import com.myapp.config.AdminSecuritySettings;
import com.myapp.config.AdminSecuritySettingsStore;
import com.myapp.security.AuthResult;
import com.myapp.security.BiometricVerificationDialog;
import com.nexora.bank.Models.User;
import com.nexora.bank.Service.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class UserController implements Initializable {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+0-9][0-9\\s-]{7,19}$");
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

    @FXML private Label lblTotalUsers;
    @FXML private Label lblUsersActifs;
    @FXML private Label lblAdmins;
    @FXML private Label lblBanTarget;
    @FXML private TextField txtNom;
    @FXML private TextField txtPrenom;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelephone;
    @FXML private TextField txtRecherche;
    @FXML private PasswordField txtNewPassword;
    @FXML private TextArea txtBanReason;
    @FXML private ComboBox<String> cmbRole;
    @FXML private ComboBox<String> cmbStatut;
    @FXML private Button btnAjouter;
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colTelephone;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatut;
    @FXML private TableColumn<User, Void> colActions;
    @FXML private Label lblTableInfo;

    private final UserService userService = new UserService();
    private final AdminSecuritySettingsStore adminSecuritySettingsStore = new AdminSecuritySettingsStore();
    private final ObservableList<User> usersList = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;
    private User selectedUser;
    private boolean editMode;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeComboData();
        initializeTable();
        initializeSearch();
        updateSelectedUserContext(null);
        refreshUsers();
    }

    private void initializeComboData() {
        cmbRole.setItems(FXCollections.observableArrayList("ROLE_USER", "ROLE_ADMIN"));
        cmbStatut.setItems(FXCollections.observableArrayList("PENDING", "ACTIVE", "DECLINED", "INACTIVE", "BANNED"));
        cmbRole.setValue("ROLE_USER");
        cmbStatut.setValue("PENDING");
    }

    private void initializeTable() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("status"));

        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item.toUpperCase());
                badge.getStyleClass().add("nx-badge");
                switch (item.toUpperCase()) {
                    case "ACTIVE" -> badge.getStyleClass().add("nx-badge-success");
                    case "PENDING" -> badge.getStyleClass().add("nx-badge-warning");
                    case "DECLINED" -> badge.getStyleClass().add("nx-badge-error");
                    case "BANNED" -> badge.getStyleClass().add("nx-badge-dark");
                    default -> badge.getStyleClass().add("nx-badge-info");
                }
                setGraphic(badge);
            }
        });

        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item.toUpperCase());
                badge.getStyleClass().add("nx-badge");
                if ("ROLE_ADMIN".equalsIgnoreCase(item)) {
                    badge.getStyleClass().add("nx-badge-purple");
                } else {
                    badge.getStyleClass().add("nx-badge-info");
                }
                setGraphic(badge);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button edit = createIconButton("edit");
            private final Button approve = createIconButton("approve");
            private final Button decline = createIconButton("decline");
            private final Button delete = createIconButton("delete");
            private final HBox box = new HBox(6, edit, approve, decline, delete);

            {
                box.setAlignment(Pos.CENTER);

                edit.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    editUser(user);
                });
                approve.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleApprove(user);
                });
                decline.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDecline(user);
                });
                delete.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                User user = getTableView().getItems().get(getIndex());
                boolean pending = "PENDING".equalsIgnoreCase(user.getStatus());
                boolean admin = "ROLE_ADMIN".equalsIgnoreCase(user.getRole());

                approve.setDisable(!pending || admin);
                decline.setDisable(!pending || admin);
                delete.setDisable(admin);
                setGraphic(box);
            }
        });

        tableUsers.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                selectedUser = n;
                populateForm(n);
                editMode = true;
                btnAjouter.setText("Enregistrer");
                updateSelectedUserContext(n);
            } else {
                updateSelectedUserContext(null);
            }
        });
    }

    private Button createIconButton(String type) {
        Button button = new Button();
        button.getStyleClass().add("nx-table-action");

        SVGPath icon = new SVGPath();
        icon.getStyleClass().add("nx-action-icon");

        switch (type) {
            case "edit" -> {
                button.getStyleClass().add("nx-table-action-edit");
                icon.setContent("M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z");
            }
            case "approve" -> {
                button.getStyleClass().add("nx-table-action-approve");
                icon.setContent("M20 6L9 17l-5-5");
            }
            case "decline" -> {
                button.getStyleClass().add("nx-table-action-decline");
                icon.setContent("M18 6L6 18M6 6l12 12");
            }
            default -> {
                button.getStyleClass().add("nx-table-action-delete");
                icon.setContent("M19 7l-.867 12.142A2 2 0 0 1 16.138 21H7.862a2 2 0 0 1-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v3M4 7h16");
            }
        }

        button.setGraphic(icon);
        return button;
    }

    private void initializeSearch() {
        filteredData = new FilteredList<>(usersList, p -> true);
        txtRecherche.textProperty().addListener((obs, oldValue, newValue) -> {
            String needle = newValue == null ? "" : newValue.trim().toLowerCase();
            filteredData.setPredicate(user -> {
                if (needle.isBlank()) {
                    return true;
                }
                return contains(user.getNom(), needle)
                    || contains(user.getPrenom(), needle)
                    || contains(user.getEmail(), needle)
                    || contains(user.getRole(), needle)
                    || contains(user.getStatus(), needle);
            });
            updateTableInfo();
        });

        SortedList<User> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(tableUsers.comparatorProperty());
        tableUsers.setItems(sorted);
    }

    private void refreshUsers() {
        try {
            List<User> data = userService.getAllUsers();
            usersList.setAll(data);
            updateStats();
            updateTableInfo();
            tableUsers.refresh();
        } catch (Exception ex) {
            showError("Failed to load users from database.");
            ex.printStackTrace();
        }
    }

    private void updateStats() {
        long active = usersList.stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).count();
        long admins = usersList.stream().filter(u -> "ROLE_ADMIN".equalsIgnoreCase(u.getRole())).count();
        lblTotalUsers.setText(String.valueOf(usersList.size()));
        lblUsersActifs.setText(String.valueOf(active));
        lblAdmins.setText(String.valueOf(admins));
    }

    private void updateTableInfo() {
        int filteredCount = filteredData == null ? usersList.size() : filteredData.size();
        lblTableInfo.setText(String.format("Affichage de %d sur %d entrees", filteredCount, usersList.size()));
    }

    private void populateForm(User user) {
        txtNom.setText(user.getNom());
        txtPrenom.setText(user.getPrenom());
        txtEmail.setText(user.getEmail());
        txtTelephone.setText(user.getTelephone());
        cmbRole.setValue(user.getRole() == null ? "ROLE_USER" : user.getRole().toUpperCase());
        cmbStatut.setValue(user.getStatus() == null ? "PENDING" : user.getStatus().toUpperCase());
    }

    private void clearForm() {
        txtNom.clear();
        txtPrenom.clear();
        txtEmail.clear();
        txtTelephone.clear();
        txtNewPassword.clear();
        if (txtBanReason != null) {
            txtBanReason.clear();
        }
        cmbRole.setValue("ROLE_USER");
        cmbStatut.setValue("PENDING");
        selectedUser = null;
        editMode = false;
        btnAjouter.setText("Ajouter");
        tableUsers.getSelectionModel().clearSelection();
        updateSelectedUserContext(null);
    }

    @FXML
    private void handleAjouter() {
        String nom = safe(txtNom.getText());
        String prenom = safe(txtPrenom.getText());
        String email = safe(txtEmail.getText());
        String tel = safe(txtTelephone.getText());
        String role = cmbRole.getValue();
        String status = cmbStatut.getValue();
        String typedPassword = safe(txtNewPassword.getText());

        if (nom.isBlank() || prenom.isBlank() || email.isBlank() || tel.isBlank() || role == null || status == null) {
            showError("Please fill all required fields.");
            return;
        }
        if (!isValidEmail(email)) {
            showError("Invalid email format.");
            return;
        }
        if (!isValidPhone(tel)) {
            showError("Invalid phone format.");
            return;
        }
        if (!editMode && typedPassword.isBlank()) {
            showError("Password is required when creating a new user.");
            return;
        }
        if (!typedPassword.isBlank() && !isStrongPassword(typedPassword)) {
            showError("Password must include upper/lowercase, number, special char, and be 8+ chars.");
            return;
        }

        try {
            if (editMode && selectedUser != null) {
                selectedUser.setNom(nom);
                selectedUser.setPrenom(prenom);
                selectedUser.setEmail(email);
                selectedUser.setTelephone(tel);
                selectedUser.setRole(role.toUpperCase());
                selectedUser.setStatus(status.toUpperCase());

                if (userService.updateUser(selectedUser)) {
                    if (!typedPassword.isBlank()) {
                        userService.updateUserPassword(selectedUser.getIdUser(), typedPassword, true);
                    }
                    showInfo("User updated successfully.");
                } else {
                    showError("No changes were saved.");
                }
            } else {
                userService.createUserByAdmin(nom, prenom, email, tel, role, status, typedPassword);
                showInfo("User created successfully. Email notification sent.");
            }

            clearForm();
            refreshUsers();
        } catch (Exception ex) {
            showError(ex.getMessage() == null ? "Failed to save user." : ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleSupprimer() {
        if (selectedUser == null) {
            showError("Select a user first.");
            return;
        }
        deleteUser(selectedUser);
    }

    @FXML
    private void handleApproveSelected() {
        if (selectedUser == null) {
            showError("Select a pending user first.");
            return;
        }
        handleApprove(selectedUser);
    }

    @FXML
    private void handleDeclineSelected() {
        if (selectedUser == null) {
            showError("Select a pending user first.");
            return;
        }
        handleDecline(selectedUser);
    }

    @FXML
    private void handleAnnuler() {
        clearForm();
    }

    @FXML
    private void handleUpdatePasswordSelected() {
        if (selectedUser == null) {
            showError("Select a user first.");
            return;
        }

        String newPassword = safe(txtNewPassword.getText());
        if (newPassword.isBlank()) {
            showError("Enter a new password first.");
            return;
        }
        if (!isStrongPassword(newPassword)) {
            showError("Password must include upper/lowercase, number, special char, and be 8+ chars.");
            return;
        }

        try {
            if (userService.updateUserPassword(selectedUser.getIdUser(), newPassword, true)) {
                txtNewPassword.clear();
                showInfo("Password updated and email sent.");
            } else {
                showError("Password was not updated.");
            }
        } catch (Exception ex) {
            showError(ex.getMessage() == null ? "Failed to update password." : ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void editUser(User user) {
        selectedUser = user;
        editMode = true;
        populateForm(user);
        btnAjouter.setText("Enregistrer");
    }

    private void deleteUser(User user) {
        if (user == null) {
            return;
        }
        if ("ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            showError("Admin user cannot be deleted.");
            return;
        }

        if (isBiometricRequiredForSensitiveActions()) {
            AuthResult authResult = BiometricVerificationDialog.promptAndVerify("NEXORA Bank admin verification");
            if (authResult != AuthResult.VERIFIED) {
                BiometricVerificationDialog.showResultDialog(authResult);
                showError(getSensitiveActionBiometricError(authResult));
                return;
            }
        }

        if (!confirm("Delete user " + user.getEmail() + "?")) {
            return;
        }

        try {
            if (userService.deleteUser(user.getIdUser())) {
                clearForm();
                refreshUsers();
                showInfo("User deleted.");
            }
        } catch (Exception ex) {
            showError("Failed to delete user.");
            ex.printStackTrace();
        }
    }

    private void handleApprove(User user) {
        if (user == null) {
            return;
        }
        if (!"PENDING".equalsIgnoreCase(user.getStatus())) {
            showError("Only pending users can be approved.");
            return;
        }
        if (!confirm("Approve user " + user.getEmail() + "?")) {
            return;
        }

        try {
            if (userService.approveUser(user.getIdUser())) {
                refreshUsers();
                showInfo("User approved. Email notification sent.");
            }
        } catch (Exception ex) {
            showError("Failed to approve user.");
            ex.printStackTrace();
        }
    }

    private void handleDecline(User user) {
        if (user == null) {
            return;
        }
        if (!"PENDING".equalsIgnoreCase(user.getStatus())) {
            showError("Only pending users can be declined.");
            return;
        }
        if (!confirm("Decline user " + user.getEmail() + "?")) {
            return;
        }

        try {
            if (userService.declineUser(user.getIdUser())) {
                refreshUsers();
                showInfo("User declined. Email notification sent.");
            }
        } catch (Exception ex) {
            showError("Failed to decline user.");
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleBanSelectedUser() {
        if (selectedUser == null) {
            showError("Select a user first.");
            return;
        }
        if ("ROLE_ADMIN".equalsIgnoreCase(selectedUser.getRole())) {
            showError("Admin user cannot be banned.");
            return;
        }
        if ("BANNED".equalsIgnoreCase(selectedUser.getStatus())) {
            showError("User is already banned.");
            return;
        }

        String reason = txtBanReason == null ? "" : safe(txtBanReason.getText());
        if (reason.isBlank()) {
            showError("Please provide a ban reason.");
            return;
        }
        if (!confirm("Ban user " + selectedUser.getEmail() + "?")) {
            return;
        }

        try {
            if (userService.banUser(selectedUser.getIdUser(), reason)) {
                showInfo("User banned and email notification sent.");
                if (txtBanReason != null) {
                    txtBanReason.clear();
                }
                refreshUsers();
            } else {
                showError("Failed to ban user.");
            }
        } catch (Exception ex) {
            showError(ex.getMessage() == null ? "Failed to ban user." : ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleUnbanSelectedUser() {
        if (selectedUser == null) {
            showError("Select a user first.");
            return;
        }
        if (!"BANNED".equalsIgnoreCase(selectedUser.getStatus())) {
            showError("Only banned users can be unbanned.");
            return;
        }
        if (!confirm("Unban user " + selectedUser.getEmail() + "?")) {
            return;
        }

        try {
            if (userService.unbanUser(selectedUser.getIdUser())) {
                showInfo("User unbanned and email notification sent.");
                refreshUsers();
            } else {
                showError("Failed to unban user.");
            }
        } catch (Exception ex) {
            showError(ex.getMessage() == null ? "Failed to unban user." : ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void exporterPDF() {
        File outputFile = new File(
            System.getProperty("user.home"),
            "nexora-users-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf"
        );

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 14);
                content.newLineAtOffset(50, 790);
                content.showText("NEXORA Users Report");
                content.endText();

                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(50, 770);
                int lineCount = 0;
                for (User user : usersList) {
                    String line = String.format(
                        "#%d | %s %s | %s | %s | %s | %s",
                        user.getIdUser(),
                        safe(user.getNom()),
                        safe(user.getPrenom()),
                        safe(user.getEmail()),
                        safe(user.getTelephone()),
                        safe(user.getRole()),
                        safe(user.getStatus())
                    );
                    content.showText(line);
                    content.newLineAtOffset(0, -14);
                    lineCount++;
                    if (lineCount >= 48) {
                        break;
                    }
                }
                content.endText();
            }

            document.save(outputFile);
            showInfo("PDF exported: " + outputFile.getAbsolutePath());
        } catch (Exception ex) {
            showError("Failed to export PDF.");
            ex.printStackTrace();
        }
    }

    @FXML
    private void envoyerSMS() {
        try {
            int count = userService.sendPendingReviewReminderEmails();
            showInfo("Reminder emails sent to pending users: " + count);
        } catch (Exception ex) {
            showError("Failed to send reminders.");
            ex.printStackTrace();
        }
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private void updateSelectedUserContext(User user) {
        if (lblBanTarget == null) {
            return;
        }
        if (user == null) {
            lblBanTarget.setText("No user selected");
            return;
        }
        lblBanTarget.setText(user.getEmail() + " [" + user.getStatus() + "]");
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    private boolean isStrongPassword(String password) {
        return password != null && STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    private boolean isBiometricRequiredForSensitiveActions() {
        try {
            AdminSecuritySettings settings = adminSecuritySettingsStore.load();
            return settings.isRequireBiometricOnSensitiveActions();
        } catch (Exception ex) {
            showError("Failed to load admin security settings.");
            return false;
        }
    }

    private String getSensitiveActionBiometricError(AuthResult result) {
        return switch (result) {
            case FAILED -> "Delete user denied: biometric verification failed or was cancelled.";
            case NOT_AVAILABLE -> "Delete user denied: biometrics are not configured on this device.";
            case ERROR -> "Delete user denied: biometric helper executable is missing or failed.";
            case VERIFIED -> "";
        };
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message).showAndWait();
    }
}
