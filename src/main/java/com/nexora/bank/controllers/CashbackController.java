package com.nexora.bank.controllers;

import com.nexora.bank.Models.Cashback;
import com.nexora.bank.Models.Partenaire;
import com.nexora.bank.Service.CashbackService;
import com.nexora.bank.Service.PartenaireService;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

public class CashbackController implements Initializable {

    @FXML private Label lblTotalCashback;
    @FXML private Label lblNombreBeneficiaires;
    @FXML private Label lblCashbackMois;

    @FXML private TextField txtUserId;
    @FXML private TextField txtMontantAchat;
    @FXML private TextField txtTauxApplique;
    @FXML private TextField txtMontantCashback;
    @FXML private DatePicker dpDateAchat;
    @FXML private DatePicker dpDateCredit;
    @FXML private DatePicker dpDateExpiration;
    @FXML private ComboBox<String> cmbStatut;
    @FXML private ComboBox<String> cmbPartenaire;

    @FXML private Button btnAjouter;
    @FXML private Button btnSupprimer;

    @FXML private TableView<Cashback> tableCashbacks;
    @FXML private TableColumn<Cashback, String> colUser;
    @FXML private TableColumn<Cashback, String> colPartenaire;
    @FXML private TableColumn<Cashback, String> colMontantAchat;
    @FXML private TableColumn<Cashback, String> colTaux;
    @FXML private TableColumn<Cashback, String> colMontantCashback;
    @FXML private TableColumn<Cashback, String> colDateAchat;
    @FXML private TableColumn<Cashback, String> colDateCredit;
    @FXML private TableColumn<Cashback, String> colDateExpiration;
    @FXML private TableColumn<Cashback, String> colStatut;
    @FXML private TableColumn<Cashback, Void> colActions;

    @FXML private TextField txtRecherche;
    @FXML private Label lblTableInfo;

    private final CashbackService cashbackService = new CashbackService();
    private final PartenaireService partenaireService = new PartenaireService();

    private final ObservableList<Cashback> cashbacksList = FXCollections.observableArrayList();
    private FilteredList<Cashback> filteredData;
    private Cashback selectedCashback;
    private boolean isEditMode;

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        initializeSearch();
        setupAutoCalculation();
        setupTableSelection();
        cmbPartenaire.setVisibleRowCount(8);
        cmbStatut.setVisibleRowCount(6);
        loadPartenaires();
        reloadCashbacks();
    }

    private void initializeTable() {
        colUser.setCellValueFactory(c -> {
            Cashback cashback = c.getValue();
            String displayName = cashback.getUserDisplayName() == null || cashback.getUserDisplayName().isBlank()
                    ? "User #" + cashback.getIdUser()
                    : cashback.getUserDisplayName().trim() + " (#" + cashback.getIdUser() + ")";
            return new SimpleStringProperty(displayName);
        });
        colPartenaire.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPartenaireNom()));
        colMontantAchat.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getMontantAchat())));
        colTaux.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTauxApplique() + "%"));
        colMontantCashback.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DT", c.getValue().getMontantCashback())));
        colDateAchat.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getDateAchat())));
        colDateCredit.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getDateCredit())));
        colDateExpiration.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getDateExpiration())));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));

        colMontantCashback.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText("+ " + item);
                setStyle("-fx-text-fill: #D4A82A; -fx-font-weight: 700;");
            }
        });

        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(item);
                badge.getStyleClass().add("nx-badge");
                switch (item) {
                    case "Credite" -> badge.getStyleClass().add("nx-badge-success");
                    case "Valide" -> badge.getStyleClass().add("nx-badge-info");
                    case "En attente" -> badge.getStyleClass().add("nx-badge-warning");
                    case "Expire", "Annule" -> badge.getStyleClass().add("nx-badge-error");
                    default -> { }
                }
                setGraphic(badge);
            }
        });

        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox hbox = new HBox(8);

            {
                btnEdit.getStyleClass().addAll("nx-table-action", "nx-table-action-edit");
                SVGPath editIcon = new SVGPath();
                editIcon.setContent("M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z");
                editIcon.getStyleClass().add("nx-action-icon");
                btnEdit.setGraphic(editIcon);

                btnDelete.getStyleClass().addAll("nx-table-action", "nx-table-action-delete");
                SVGPath deleteIcon = new SVGPath();
                deleteIcon.setContent("M19 7l-.867 12.142A2 2 0 0 1 16.138 21H7.862a2 2 0 0 1-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v3M4 7h16");
                deleteIcon.getStyleClass().add("nx-action-icon");
                btnDelete.setGraphic(deleteIcon);

                hbox.setAlignment(Pos.CENTER);
                hbox.getChildren().addAll(btnEdit, btnDelete);

                btnEdit.setOnAction(event -> editCashback(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(event -> deleteCashback(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void initializeSearch() {
        filteredData = new FilteredList<>(cashbacksList, c -> true);
        txtRecherche.textProperty().addListener((obs, old, newValue) -> {
            String filter = newValue == null ? "" : newValue.trim().toLowerCase();
            filteredData.setPredicate(cashback -> {
                if (filter.isEmpty()) {
                    return true;
                }
                String userToken = String.valueOf(cashback.getIdUser());
                String userName = cashback.getUserDisplayName() == null ? "" : cashback.getUserDisplayName().toLowerCase();
                return userToken.contains(filter)
                        || userName.contains(filter)
                        || safe(cashback.getPartenaireNom()).toLowerCase().contains(filter)
                        || safe(cashback.getStatut()).toLowerCase().contains(filter);
            });
            updateTableInfo();
        });

        SortedList<Cashback> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableCashbacks.comparatorProperty());
        tableCashbacks.setItems(sortedData);
    }

    private void setupAutoCalculation() {
        txtMontantAchat.textProperty().addListener((obs, old, value) -> calculateCashback());
        txtTauxApplique.textProperty().addListener((obs, old, value) -> calculateCashback());
    }

    private void setupTableSelection() {
        tableCashbacks.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value == null) {
                return;
            }
            selectedCashback = value;
            populateForm(value);
            isEditMode = true;
            btnAjouter.setText("Modifier");
        });
    }

    private void loadPartenaires() {
        List<Partenaire> partenaires = partenaireService.getAllPartenaires();
        Set<String> names = new LinkedHashSet<>();
        for (Partenaire partenaire : partenaires) {
            if (partenaire.getNom() != null && !partenaire.getNom().isBlank()) {
                names.add(partenaire.getNom().trim());
            }
        }

        // Fallback when partner table is empty: keep admin UX usable.
        if (names.isEmpty()) {
            names.add("Carrefour");
            names.add("Zara");
            names.add("Technopolis");
            names.add("Pizza Hut");
            names.add("Tunisair");
        }

        cmbPartenaire.getItems().setAll(names);
    }

    private void reloadCashbacks() {
        cashbacksList.setAll(cashbackService.getAllCashbacks());
        updateStats();
        updateTableInfo();
    }

    private void calculateCashback() {
        try {
            double montant = parseDouble(txtMontantAchat.getText());
            double taux = parseDouble(txtTauxApplique.getText());
            txtMontantCashback.setText(String.format("%.2f", montant * taux / 100));
        } catch (NumberFormatException ex) {
            txtMontantCashback.setText("0.00");
        }
    }

    private void updateStats() {
        lblTotalCashback.setText(String.format("%,.2f DT", cashbackService.getTotalCreditedGlobal()));
        lblNombreBeneficiaires.setText(String.valueOf(cashbackService.countActiveUsersWithCashback()));
        lblCashbackMois.setText(String.format("%,.2f DT", cashbackService.getCurrentMonthTotalGlobal()));
    }

    private void updateTableInfo() {
        int filteredCount = filteredData == null ? 0 : filteredData.size();
        lblTableInfo.setText(String.format("Affichage de %d sur %d entrees", filteredCount, cashbacksList.size()));
    }

    private void populateForm(Cashback cashback) {
        txtUserId.setText(String.valueOf(cashback.getIdUser()));
        txtMontantAchat.setText(String.valueOf(cashback.getMontantAchat()));
        txtTauxApplique.setText(String.valueOf(cashback.getTauxApplique()));
        txtMontantCashback.setText(String.valueOf(cashback.getMontantCashback()));
        dpDateAchat.setValue(cashback.getDateAchat());
        dpDateCredit.setValue(cashback.getDateCredit());
        dpDateExpiration.setValue(cashback.getDateExpiration());
        cmbStatut.setValue(cashback.getStatut());
        cmbPartenaire.setValue(cashback.getPartenaireNom());
    }

    private void clearForm() {
        txtUserId.clear();
        txtMontantAchat.clear();
        txtTauxApplique.clear();
        txtMontantCashback.setText("0.00");
        dpDateAchat.setValue(null);
        dpDateCredit.setValue(null);
        dpDateExpiration.setValue(null);
        cmbStatut.setValue(null);
        cmbPartenaire.setValue(null);

        selectedCashback = null;
        isEditMode = false;
        btnAjouter.setText("Ajouter");
        tableCashbacks.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleAjouter() {
        if (!validateForm()) {
            return;
        }

        try {
            Cashback cashback = buildFromForm();
            if (isEditMode && selectedCashback != null) {
                cashback.setIdCashback(selectedCashback.getIdCashback());
                if (!cashbackService.updateCashbackAsAdmin(cashback)) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Mise a jour impossible.");
                    return;
                }
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Cashback modifie avec succes.");
            } else {
                int createdId = cashbackService.createCashback(cashback);
                if (createdId <= 0) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Creation impossible.");
                    return;
                }
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Cashback ajoute avec succes.");
            }

            clearForm();
            reloadCashbacks();
        } catch (RuntimeException ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        if (selectedCashback == null) {
            showAlert(Alert.AlertType.WARNING, "Avertissement", "Selectionnez un cashback.");
            return;
        }
        deleteCashback(selectedCashback);
    }

    @FXML
    private void handleAnnuler() {
        clearForm();
    }

    private Cashback buildFromForm() {
        Cashback cashback = new Cashback();
        cashback.setIdUser(Integer.parseInt(txtUserId.getText().trim()));
        cashback.setPartenaireNom(cmbPartenaire.getValue() == null ? "" : cmbPartenaire.getValue().trim());

        Optional<Partenaire> partenaireOpt = partenaireService.findByName(cashback.getPartenaireNom());
        cashback.setIdPartenaire(partenaireOpt.map(Partenaire::getIdPartenaire).orElse(null));

        cashback.setMontantAchat(parseDouble(txtMontantAchat.getText()));
        cashback.setTauxApplique(parseDouble(txtTauxApplique.getText()));
        cashback.setMontantCashback(parseDouble(txtMontantCashback.getText()));
        cashback.setDateAchat(dpDateAchat.getValue());
        cashback.setDateCredit(dpDateCredit.getValue());
        cashback.setDateExpiration(dpDateExpiration.getValue());
        cashback.setStatut(cmbStatut.getValue());
        cashback.setTransactionRef("");
        return cashback;
    }

    private void editCashback(Cashback cashback) {
        selectedCashback = cashback;
        populateForm(cashback);
        isEditMode = true;
        btnAjouter.setText("Modifier");
    }

    private void deleteCashback(Cashback cashback) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer ce cashback ?",
                ButtonType.OK,
                ButtonType.CANCEL);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        boolean deleted = cashbackService.deleteCashbackAsAdmin(cashback.getIdCashback());
        if (!deleted) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible.");
            return;
        }

        clearForm();
        reloadCashbacks();
    }

    private boolean validateForm() {
        if (txtUserId.getText().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "L identifiant utilisateur est obligatoire.");
            return false;
        }
        if (cmbPartenaire.getValue() == null || cmbPartenaire.getValue().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Selectionnez un partenaire.");
            return false;
        }
        if (txtMontantAchat.getText().isBlank() || txtTauxApplique.getText().isBlank() || txtMontantCashback.getText().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Les champs montant/taux sont obligatoires.");
            return false;
        }
        if (dpDateAchat.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Validation", "La date d achat est obligatoire.");
            return false;
        }
        if (cmbStatut.getValue() == null || cmbStatut.getValue().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Le statut est obligatoire.");
            return false;
        }

        try {
            Integer.parseInt(txtUserId.getText().trim());
            parseDouble(txtMontantAchat.getText());
            parseDouble(txtTauxApplique.getText());
            parseDouble(txtMontantCashback.getText());
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Valeurs numeriques invalides.");
            return false;
        }

        return true;
    }

    @FXML
    private void trierParDateAchat() {
        cashbacksList.sort(Comparator.comparing(Cashback::getDateAchat, Comparator.nullsLast(LocalDate::compareTo)).reversed());
    }

    @FXML
    private void trierParMontant() {
        cashbacksList.sort(Comparator.comparing(Cashback::getMontantAchat).reversed());
    }

    @FXML
    private void trierParCashback() {
        cashbacksList.sort(Comparator.comparing(Cashback::getMontantCashback).reversed());
    }

    @FXML
    private void trierParStatut() {
        cashbacksList.sort(Comparator.comparing(c -> safe(c.getStatut())));
    }

    @FXML
    private void exporterPDF() {
        showAlert(Alert.AlertType.INFORMATION, "Export", "Export PDF en developpement.");
    }

    @FXML
    private void envoyerSMS() {
        showAlert(Alert.AlertType.INFORMATION, "SMS", "Envoi SMS en developpement.");
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DISPLAY_DATE_FORMAT);
    }

    private double parseDouble(String text) {
        return Double.parseDouble(text == null ? "0" : text.trim().replace(",", "."));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
