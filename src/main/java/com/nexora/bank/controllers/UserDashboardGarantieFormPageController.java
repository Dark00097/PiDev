package com.nexora.bank.controllers;

import com.nexora.bank.AuthSession;
import com.nexora.bank.SceneRouter;
import com.nexora.bank.Models.GarantieCredit;
import com.nexora.bank.Models.User;
import com.nexora.bank.Service.CreditService;
import com.nexora.bank.Service.GarentieCreditService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;

public class UserDashboardGarantieFormPageController {

    @FXML private Label lblUserInfo;
    @FXML private ComboBox<Integer> cbGarantieCreditId;
    @FXML private ComboBox<String> cbGarantieType;
    @FXML private TextField txtGarantieEstimatedValue;
    @FXML private TextField txtGarantieRetainedValue;
    @FXML private TextField txtGarantieAddress;
    @FXML private TextField txtGarantieNomGarant;
    @FXML private DatePicker dpGarantieDateEvaluation;
    @FXML private TextField txtGarantieDocument;
    @FXML private TextArea txtGarantieDescription;

    private final CreditService creditService = new CreditService();
    private final GarentieCreditService garantieService = new GarentieCreditService();
    private int currentUserId;

    @FXML
    private void initialize() {
        User u = AuthSession.getCurrentUser();
        currentUserId = u == null ? 0 : u.getIdUser();
        lblUserInfo.setText(u == null ? "Utilisateur non connecte" : "Utilisateur: " + safe(u.getPrenom()) + " " + safe(u.getNom()));

        cbGarantieType.setItems(FXCollections.observableArrayList(
                "Real Estate Hypotheque",
                "Vehicle Title",
                "Personnel Guarantee",
                "Bank Guarantee",
                "Insurance Policy",
                "Other Collateral"
        ));
        dpGarantieDateEvaluation.setValue(LocalDate.now());

        if (currentUserId > 0) {
            List<Integer> ids = creditService.getCreditIdsByUser(currentUserId);
            cbGarantieCreditId.setItems(FXCollections.observableArrayList(ids));
            if (!ids.isEmpty()) {
                cbGarantieCreditId.setValue(ids.get(0));
            }
        }
    }

    @FXML
    private void submitGarantie() {
        if (currentUserId <= 0) {
            warn("Session invalide", "Veuillez vous reconnecter.");
            return;
        }
        try {
            Integer idCredit = cbGarantieCreditId.getValue();
            if (idCredit == null) throw new IllegalArgumentException("Selectionnez un credit.");
            String type = text(cbGarantieType.getValue());
            if (type.isBlank()) throw new IllegalArgumentException("Selectionnez un type de garantie.");
            String adresse = text(txtGarantieAddress.getText());
            if (adresse.isBlank()) throw new IllegalArgumentException("Adresse du bien obligatoire.");
            String nomGarant = text(txtGarantieNomGarant.getText());
            if (nomGarant.isBlank()) throw new IllegalArgumentException("Nom du garant obligatoire.");
            LocalDate eval = dpGarantieDateEvaluation.getValue();
            if (eval == null) throw new IllegalArgumentException("Date d evaluation obligatoire.");

            double estimee = parseRequired(txtGarantieEstimatedValue.getText(), "Valeur estimee obligatoire.");
            double retenue = parseRequired(txtGarantieRetainedValue.getText(), "Valeur retenue obligatoire.");

            GarantieCredit g = new GarantieCredit(
                    0,
                    idCredit,
                    type,
                    text(txtGarantieDescription.getText()),
                    adresse,
                    estimee,
                    retenue,
                    text(txtGarantieDocument.getText()),
                    eval.toString(),
                    nomGarant,
                    "En attente",
                    currentUserId
            );
            garantieService.addGarantieForUser(g, currentUserId);
            info("Succes", "Garantie ajoutee avec succes.");
            clearForm();
        } catch (IllegalArgumentException ex) {
            warn("Validation", ex.getMessage());
        }
    }

    @FXML
    private void clearForm() {
        if (txtGarantieEstimatedValue != null) txtGarantieEstimatedValue.clear();
        if (txtGarantieRetainedValue != null) txtGarantieRetainedValue.clear();
        if (txtGarantieAddress != null) txtGarantieAddress.clear();
        if (txtGarantieNomGarant != null) txtGarantieNomGarant.clear();
        if (txtGarantieDocument != null) txtGarantieDocument.clear();
        if (txtGarantieDescription != null) txtGarantieDescription.clear();
        if (cbGarantieType != null) cbGarantieType.getSelectionModel().clearSelection();
        if (dpGarantieDateEvaluation != null) dpGarantieDateEvaluation.setValue(LocalDate.now());
    }

    @FXML
    private void backToDashboard() {
        Stage stage = (Stage) lblUserInfo.getScene().getWindow();
        stage.close();
    }

    private String text(String s) {
        return s == null ? "" : s.trim();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private double parseRequired(String s, String msg) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(msg);
        try {
            return Double.parseDouble(s.replace(",", ".").trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(msg);
        }
    }

    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }

    private void warn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }
}
