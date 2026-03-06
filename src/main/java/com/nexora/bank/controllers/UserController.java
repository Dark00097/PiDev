package com.nexora.bank.controllers;

import com.myapp.config.AdminSecuritySettings;
import com.myapp.config.AdminSecuritySettingsStore;
import com.myapp.security.AuthResult;
import com.myapp.security.BiometricVerificationDialog;
import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.User;
import com.nexora.bank.Service.UserService;
import com.nexora.bank.Utils.ProfileImageUtils;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
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
    
    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+0-9][0-9\\s-]{7,19}$");
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$");

    // FXML injected fields
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

    // Services and data
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
        applyNotificationRedirectTarget();
        
        // Apply entrance animations
        Platform.runLater(this::applyEntranceAnimations);
    }

    /**
     * Applies smooth entrance animations to key UI elements
     */
    private void applyEntranceAnimations() {
        // Animate stats cards
        if (lblTotalUsers != null && lblTotalUsers.getParent() != null) {
            animateNode(lblTotalUsers.getParent().getParent(), 0);
        }
        if (lblUsersActifs != null && lblUsersActifs.getParent() != null) {
            animateNode(lblUsersActifs.getParent().getParent(), 100);
        }
        if (lblAdmins != null && lblAdmins.getParent() != null) {
            animateNode(lblAdmins.getParent().getParent(), 200);
        }
    }

    private void animateNode(Node node, int delayMs) {
        if (node == null) return;
        
        node.setOpacity(0);
        node.setTranslateY(20);
        
        FadeTransition fade = new FadeTransition(Duration.millis(400), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(delayMs));
        
        TranslateTransition translate = new TranslateTransition(Duration.millis(400), node);
        translate.setFromY(20);
        translate.setToY(0);
        translate.setDelay(Duration.millis(delayMs));
        
        fade.play();
        translate.play();
    }

    private void initializeComboData() {
        cmbRole.setItems(FXCollections.observableArrayList("ROLE_USER", "ROLE_ADMIN"));
        cmbStatut.setItems(FXCollections.observableArrayList("PENDING", "ACTIVE", "DECLINED", "INACTIVE", "BANNED"));
        cmbRole.setValue("ROLE_USER");
        cmbStatut.setValue("PENDING");
    }

    private void initializeTable() {
        // Setup cell value factories
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Custom cell factory for Name column with avatar
        colNom.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                User rowUser = getTableRow() == null ? null : (User) getTableRow().getItem();
                if (rowUser == null) {
                    setText(item);
                    setGraphic(null);
                    return;
                }

                HBox container = new HBox(12);
                container.setAlignment(Pos.CENTER_LEFT);
                
                StackPane avatar = createUserPhotoThumb(rowUser);
                Label nameLabel = new Label(safe(rowUser.getNom()));
                nameLabel.getStyleClass().add("nx-user-name-cell");
                
                container.getChildren().addAll(avatar, nameLabel);
                setText(null);
                setGraphic(container);
            }
        });

        // Custom cell factory for Status column with badges
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

        // Custom cell factory for Role column with badges
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                
                Label badge = new Label(item.replace("ROLE_", ""));
                badge.getStyleClass().add("nx-badge");
                
                if ("ROLE_ADMIN".equalsIgnoreCase(item)) {
                    badge.getStyleClass().add("nx-badge-purple");
                } else {
                    badge.getStyleClass().add("nx-badge-info");
                }
                
                setGraphic(badge);
            }
        });

        // Custom cell factory for Actions column
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button edit = createActionButton("edit");
            private final Button approve = createActionButton("approve");
            private final Button decline = createActionButton("decline");
            private final Button delete = createActionButton("delete");
            private final HBox box = new HBox(8, edit, approve, decline, delete);

            {
                box.setAlignment(Pos.CENTER);

                edit.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    editUser(user);
                    pulseButton(edit);
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
                
                approve.setOpacity(approve.isDisabled() ? 0.4 : 1);
                decline.setOpacity(decline.isDisabled() ? 0.4 : 1);
                delete.setOpacity(delete.isDisabled() ? 0.4 : 1);
                
                setGraphic(box);
            }
        });

        // Selection listener
        tableUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedUser = newVal;
                populateForm(newVal);
                editMode = true;
                btnAjouter.setText("Enregistrer");
                updateSelectedUserContext(newVal);
            } else {
                updateSelectedUserContext(null);
            }
        });
    }

    private Button createActionButton(String type) {
        Button button = new Button();
        button.getStyleClass().add("nx-table-action");
        button.setMinSize(32, 32);
        button.setMaxSize(32, 32);

        SVGPath icon = new SVGPath();
        icon.getStyleClass().add("nx-action-icon");

        switch (type) {
            case "edit" -> {
                button.getStyleClass().add("nx-table-action-edit");
                icon.setContent("M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z");
                button.setTooltip(new Tooltip("Modifier"));
            }
            case "approve" -> {
                button.getStyleClass().add("nx-table-action-approve");
                icon.setContent("M20 6L9 17l-5-5");
                button.setTooltip(new Tooltip("Approuver"));
            }
            case "decline" -> {
                button.getStyleClass().add("nx-table-action-decline");
                icon.setContent("M18 6L6 18M6 6l12 12");
                button.setTooltip(new Tooltip("Refuser"));
            }
            default -> {
                button.getStyleClass().add("nx-table-action-delete");
                icon.setContent("M19 7l-.867 12.142A2 2 0 0 1 16.138 21H7.862a2 2 0 0 1-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v3M4 7h16");
                button.setTooltip(new Tooltip("Supprimer"));
            }
        }

        button.setGraphic(icon);
        return button;
    }

    private void pulseButton(Button button) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(0.9);
        scale.setToY(0.9);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }

    private StackPane createUserPhotoThumb(User user) {
        StackPane thumb = new StackPane();
        thumb.getStyleClass().add("nx-user-photo-thumb");
        thumb.setMinSize(36, 36);
        thumb.setMaxSize(36, 36);

        Image avatar = ProfileImageUtils.loadImageOrNull(
            user == null ? null : user.getProfileImagePath(), 36, 36
        );
        
        if (avatar != null) {
            ImageView imageView = new ImageView(avatar);
            imageView.setFitWidth(36);
            imageView.setFitHeight(36);
            imageView.setPreserveRatio(false);
            ProfileImageUtils.applyCircularClip(imageView, 36);
            thumb.getChildren().add(imageView);
            return thumb;
        }

        Label initials = new Label(buildInitials(user));
        initials.getStyleClass().add("nx-user-photo-fallback");
        thumb.getChildren().add(initials);
        return thumb;
    }

    private String buildInitials(User user) {
        String prenom = user == null ? "" : safe(user.getPrenom());
        String nom = user == null ? "" : safe(user.getNom());
        String source = (prenom + " " + nom).trim();
        
        if (source.isBlank()) {
            source = user == null ? "" : safe(user.getEmail());
        }
        if (source.isBlank()) {
            return "U";
        }
        
        String[] parts = source.split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return source.substring(0, 1).toUpperCase();
    }

    private void initializeSearch() {
        filteredData = new FilteredList<>(usersList, p -> true);
        
        txtRecherche.textProperty().addListener((obs, oldValue, newValue) -> {
            String needle = newValue == null ? "" : newValue.trim().toLowerCase();
            filteredData.setPredicate(user -> {
                if (needle.isBlank()) return true;
                return contains(user.getNom(), needle)
                    || contains(user.getPrenom(), needle)
                    || contains(user.getEmail(), needle)
                    || contains(user.getRole(), needle)
                    || contains(user.getStatus(), needle)
                    || contains(user.getTelephone(), needle);
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
            showError("Échec du chargement des utilisateurs depuis la base de données.");
            ex.printStackTrace();
        }
    }

    private void applyNotificationRedirectTarget() {
        Integer targetUserId = AuthSession.consumePendingUserManagementTargetId();
        if (targetUserId == null || targetUserId <= 0) return;

        Platform.runLater(() -> {
            User target = usersList.stream()
                .filter(user -> user.getIdUser() == targetUserId)
                .findFirst()
                .orElse(null);

            if (target == null) return;

            if (txtRecherche != null) {
                txtRecherche.setText(target.getEmail() == null ? "" : target.getEmail());
            }
            tableUsers.getSelectionModel().select(target);
            tableUsers.scrollTo(target);
        });
    }

    private void updateStats() {
        long active = usersList.stream()
            .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
            .count();
        long admins = usersList.stream()
            .filter(u -> "ROLE_ADMIN".equalsIgnoreCase(u.getRole()))
            .count();
        
        animateStatValue(lblTotalUsers, usersList.size());
        animateStatValue(lblUsersActifs, (int) active);
        animateStatValue(lblAdmins, (int) admins);
    }

    private void animateStatValue(Label label, int targetValue) {
        if (label == null) return;
        
        int currentValue = 0;
        try {
            currentValue = Integer.parseInt(label.getText());
        } catch (NumberFormatException ignored) {}
        
        if (currentValue == targetValue) {
            label.setText(String.valueOf(targetValue));
            return;
        }
        
        // Simple animation - just set the value
        // For a more complex animation, you could use Timeline
        label.setText(String.valueOf(targetValue));
    }

    private void updateTableInfo() {
        int filteredCount = filteredData == null ? usersList.size() : filteredData.size();
        lblTableInfo.setText(String.format("Affichage de %d sur %d entrées", filteredCount, usersList.size()));
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
        if (txtBanReason != null) txtBanReason.clear();
        
        cmbRole.setValue("ROLE_USER");
        cmbStatut.setValue("PENDING");
        selectedUser = null;
        editMode = false;
        btnAjouter.setText("Enregistrer");
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

        // Validation
        if (nom.isBlank() || prenom.isBlank() || email.isBlank() || tel.isBlank() || role == null || status == null) {
            showError("Veuillez remplir tous les champs obligatoires.");
            highlightEmptyFields();
            return;
        }
        if (!isValidEmail(email)) {
            showError("Format d'email invalide.");
            return;
        }
        if (!isValidPhone(tel)) {
            showError("Format de téléphone invalide.");
            return;
        }
        if (!editMode && typedPassword.isBlank()) {
            showError("Le mot de passe est requis pour créer un nouvel utilisateur.");
            return;
        }
        if (!typedPassword.isBlank() && !isStrongPassword(typedPassword)) {
            showError("Le mot de passe doit contenir au moins 8 caractères avec majuscule, minuscule, chiffre et symbole.");
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
                    showSuccess("Utilisateur mis à jour avec succès.");
                } else {
                    showError("Aucune modification n'a été enregistrée.");
                }
            } else {
                userService.createUserByAdmin(nom, prenom, email, tel, role, status, typedPassword);
                showSuccess("Utilisateur créé avec succès. Email de notification envoyé.");
            }

            clearForm();
            refreshUsers();
        } catch (Exception ex) {
            showError(ex.getMessage() == null ? "Échec de l'enregistrement." : ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void highlightEmptyFields() {
        // Add visual feedback for empty required fields
        if (txtNom.getText().isBlank()) txtNom.setStyle("-fx-border-color: #EF4444;");
        if (txtPrenom.getText().isBlank()) txtPrenom.setStyle("-fx-border-color: #EF4444;");
        if (txtEmail.getText().isBlank()) txtEmail.setStyle("-fx-border-color: #EF4444;");
        if (txtTelephone.getText().isBlank()) txtTelephone.setStyle("-fx-border-color: #EF4444;");
    }

    @FXML
    private void handleSupprimer() {
        if (selectedUser == null) {
            showError("Veuillez sélectionner un utilisateur.");
            return;
        }
        deleteUser(selectedUser);
    }

    @FXML
    private void handleApproveSelected() {
        if (selectedUser == null) {
            showError("Veuillez sélectionner un utilisateur en attente.");
            return;
        }
        handleApprove(selectedUser);
    }

    @FXML
    private void handleDeclineSelected() {
        if (selectedUser == null) {
            showError("Veuillez sélectionner un utilisateur en attente.");
            return;
        }
        handleDecline(selectedUser);
    }

    @FXML
    private void handleAnnuler() {
        clearForm();
        
        // Reset any error styling
        txtNom.setStyle("");
        txtPrenom.setStyle("");
        txtEmail.setStyle("");
        txtTelephone.setStyle("");
    }

    @FXML
    private void handleUpdatePasswordSelected() {
        if (selectedUser == null) {
            showError("Veuillez sélectionner un utilisateur.");
            return;
        }

        String newPassword = safe(txtNewPassword.getText());
        if (newPassword.isBlank()) {
            showError("Veuillez entrer un nouveau mot de passe.");
            return;
        }
        if (!isStrongPassword(newPassword)) {
            showError("Le mot de passe doit contenir au moins 8 caractères avec majuscule, minuscule, chiffre et symbole.");
            return;
        }

        try {
            if (userService.updateUserPassword(selectedUser.getIdUser(), newPassword, true)) {
                txtNewPassword.clear();
                showSuccess("Mot de passe mis à jour et email envoyé.");
            } else {
                showError("Le mot de passe n'a pas été mis à jour.");
            }
        } catch (Exception ex) {
            showError(ex.getMessage() == null ? "Échec de la mise à jour du mot de passe." : ex.getMessage());
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
        if (user == null) return;
        
        if ("ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
            showError("Un administrateur ne peut pas être supprimé.");
            return;
        }

        if (isBiometricRequiredForSensitiveActions()) {
            AuthResult authResult = BiometricVerificationDialog.promptAndVerify("Vérification administrateur NEXORA Bank");
            if (authResult != AuthResult.VERIFIED) {
                BiometricVerificationDialog.showResultDialog(authResult);
                showError(getSensitiveActionBiometricError(authResult));
                return;
            }
        }

        if (!confirm("Supprimer l'utilisateur " + user.getEmail() + " ?")) return;

        try {
            if (userService.deleteUser(user.getIdUser())) {
                clearForm();
                refreshUsers();
                showSuccess("Utilisateur supprimé.");
            }
        } catch (Exception ex) {
            showError("Échec de la suppression de l'utilisateur.");
            ex.printStackTrace();
        }
    }

    private void handleApprove(User user) {
        if (user == null) return;
        
        if (!"PENDING".equalsIgnoreCase(user.getStatus())) {
            showError("Seuls les utilisateurs en attente peuvent être approuvés.");
            return;
        }
        
        if (!confirm("Approuver l'utilisateur " + user.getEmail() + " ?")) return;

        try {
            if (userService.approveUser(user.getIdUser())) {
                refreshUsers();
                showSuccess("Utilisateur approuvé. Email de notification envoyé.");
            }
        } catch (Exception ex) {
            showError("Échec de l'approbation de l'utilisateur.");
            ex.printStackTrace();
        }
    }

    private void handleDecline(User user) {
        if (user == null) return;
        
        if (!"PENDING".equalsIgnoreCase(user.getStatus())) {
            showError("Seuls les utilisateurs en attente peuvent être refusés.");
            return;
        }
        
        if (!confirm("Refuser l'utilisateur " + user.getEmail() + " ?")) return;

        try {
            if (userService.declineUser(user.getIdUser())) {
                refreshUsers();
                showSuccess("Utilisateur refusé. Email de notification envoyé.");
            }
        } catch (Exception ex) {
            showError("Échec du refus de l'utilisateur.");
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleBanSelectedUser() {
        if (selectedUser == null) {
            showError("Veuillez sélectionner un utilisateur.");
            return;
        }
        if ("ROLE_ADMIN".equalsIgnoreCase(selectedUser.getRole())) {
            showError("Un administrateur ne peut pas être banni.");
            return;
        }
        if ("BANNED".equalsIgnoreCase(selectedUser.getStatus())) {
            showError("L'utilisateur est déjà banni.");
            return;
        }

        String reason = txtBanReason == null ? "" : safe(txtBanReason.getText());
        if (reason.isBlank()) {
            showError("Veuillez fournir un motif de bannissement.");
            return;
        }
        
        if (!confirm("Bannir l'utilisateur " + selectedUser.getEmail() + " ?")) return;

        try {
            if (userService.banUser(selectedUser.getIdUser(), reason)) {
                showSuccess("Utilisateur banni. Email de notification envoyé.");
                if (txtBanReason != null) txtBanReason.clear();
                refreshUsers();
            } else {
                showError("Échec du bannissement de l'utilisateur.");
            }
        } catch (Exception ex) {
            showError(ex.getMessage() == null ? "Échec du bannissement." : ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleUnbanSelectedUser() {
        if (selectedUser == null) {
            showError("Veuillez sélectionner un utilisateur.");
            return;
        }
        if (!"BANNED".equalsIgnoreCase(selectedUser.getStatus())) {
            showError("Seuls les utilisateurs bannis peuvent être débannis.");
            return;
        }
        
        if (!confirm("Débannir l'utilisateur " + selectedUser.getEmail() + " ?")) return;

        try {
            if (userService.unbanUser(selectedUser.getIdUser())) {
                showSuccess("Utilisateur débanni. Email de notification envoyé.");
                refreshUsers();
            } else {
                showError("Échec du débannissement de l'utilisateur.");
            }
        } catch (Exception ex) {
            showError(ex.getMessage() == null ? "Échec du débannissement." : ex.getMessage());
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
                // Header
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.newLineAtOffset(50, 780);
                content.showText("NEXORA Bank - Rapport Utilisateurs");
                content.endText();

                // Date
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(50, 760);
                content.showText("Généré le: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                content.endText();

                // Table content
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 9);
                content.newLineAtOffset(50, 730);
                
                int lineCount = 0;
                for (User user : usersList) {
                    String line = String.format(
                        "#%d | %s %s | %s | %s | %s",
                        user.getIdUser(),
                        safe(user.getNom()),
                        safe(user.getPrenom()),
                        safe(user.getEmail()),
                        safe(user.getRole()).replace("ROLE_", ""),
                        safe(user.getStatus())
                    );
                    content.showText(line);
                    content.newLineAtOffset(0, -16);
                    lineCount++;
                    if (lineCount >= 42) break;
                }
                content.endText();
            }

            document.save(outputFile);
            showSuccess("PDF exporté: " + outputFile.getAbsolutePath());
        } catch (Exception ex) {
            showError("Échec de l'export PDF.");
            ex.printStackTrace();
        }
    }

    @FXML
    private void envoyerSMS() {
        try {
            int count = userService.sendPendingReviewReminderEmails();
            showSuccess("Emails de rappel envoyés aux utilisateurs en attente: " + count);
        } catch (Exception ex) {
            showError("Échec de l'envoi des rappels.");
            ex.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private void updateSelectedUserContext(User user) {
        if (lblBanTarget == null) return;
        
        if (user == null) {
            lblBanTarget.setText("Aucun utilisateur");
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
            return false;
        }
    }

    private String getSensitiveActionBiometricError(AuthResult result) {
        return switch (result) {
            case FAILED -> "Suppression refusée: vérification biométrique échouée ou annulée.";
            case NOT_AVAILABLE -> "Suppression refusée: biométrie non configurée sur cet appareil.";
            case ERROR -> "Suppression refusée: erreur du système biométrique.";
            case VERIFIED -> "";
        };
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/User.css").toExternalForm());
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/User.css").toExternalForm());
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/User.css").toExternalForm());
        alert.showAndWait();
    }
}