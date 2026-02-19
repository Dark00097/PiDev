package com.nexora.bank.controllers;

import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.Credit;
import com.nexora.bank.Models.GarantieCredit;
import com.nexora.bank.Models.User;
import com.nexora.bank.Service.CreditService;
import com.nexora.bank.Service.GarentieCreditService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserDashboardCreditSectionController {

    @FXML private VBox rootCreditSection;
    @FXML private Label lblUserCreditInfo;
    @FXML private VBox garantieFormContainer;
    @FXML private VBox garantiesDisplayContainer;

    @FXML private ComboBox<Integer> cmbCompte;
    @FXML private TextField txtRequestedAmount;
    @FXML private TextField txtApprovedAmount;
    @FXML private ComboBox<String> cbDuree;
    @FXML private TextField txtInterestRate;
    @FXML private TextField txtMonthlyPayment;
    @FXML private TextField txtRemainingAmount;
    @FXML private ComboBox<String> cbPurpose;
    @FXML private ComboBox<String> cbCreditStatus;
    @FXML private DatePicker dpCreditDate;
    @FXML private TextArea txtNotes;
    @FXML private Button btnSubmitCredit;
    @FXML private Button btnDeleteCredit;

    @FXML private TableView<Credit> tblCredits;
    @FXML private TableColumn<Credit, String> colCreditId;
    @FXML private TableColumn<Credit, String> colCreditCompte;
    @FXML private TableColumn<Credit, String> colCreditType;
    @FXML private TableColumn<Credit, String> colCreditAmount;
    @FXML private TableColumn<Credit, String> colCreditStatus;
    @FXML private TableColumn<Credit, String> colCreditDate;

    @FXML private ComboBox<Integer> cbGarantieCreditId;
    @FXML private ComboBox<String> cbGarantieType;
    @FXML private TextField txtGarantieEstimatedValue;
    @FXML private TextField txtGarantieRetainedValue;
    @FXML private TextField txtGarantieAddress;
    @FXML private TextField txtGarantieNomGarant;
    @FXML private DatePicker dpGarantieDateEvaluation;
    @FXML private TextField txtGarantieDocument;
    @FXML private ComboBox<String> cbGarantieStatus;
    @FXML private TextArea txtGarantieDescription;
    @FXML private Button btnSubmitGarantie;
    @FXML private Button btnDeleteGarantie;

    @FXML private TableView<GarantieCredit> tblGaranties;
    @FXML private TableColumn<GarantieCredit, String> colGarantieId;
    @FXML private TableColumn<GarantieCredit, String> colGarantieCreditId;
    @FXML private TableColumn<GarantieCredit, String> colGarantieType;
    @FXML private TableColumn<GarantieCredit, String> colGarantieValue;
    @FXML private TableColumn<GarantieCredit, String> colGarantieStatus;
    @FXML private TableColumn<GarantieCredit, String> colGarantieDate;

    private final CreditService creditService = new CreditService();
    private final GarentieCreditService garantieService = new GarentieCreditService();

    private final ObservableList<Credit> credits = FXCollections.observableArrayList();
    private final ObservableList<GarantieCredit> garanties = FXCollections.observableArrayList();

    private Credit selectedCredit;
    private GarantieCredit selectedGarantie;
    private int currentUserId = 0;
    private String forcedCreditStatus;

    @FXML
    private void initialize() {
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getIdUser();
        }

        configureCreditFormDefaults();
        configureGarantieFormDefaults();
        configureCreditTable();
        configureGarantieTable();
        setupCalculationListeners();
        if (garantieFormContainer != null) {
            setVisibleManaged(garantieFormContainer, false);
        }
        if (garantiesDisplayContainer != null) {
            setVisibleManaged(garantiesDisplayContainer, true);
        }
        loadCompteIds();
        reloadAllData();
        updateUserInfoLabel(currentUser);

        boolean hasUser = currentUserId > 0;
        if (!hasUser && rootCreditSection != null) {
            rootCreditSection.setDisable(true);
            showWarning("Session utilisateur manquante", "Veuillez vous reconnecter pour gerer vos credits.");
        }
    }

    @FXML
    private void newCreditApplication() {
        clearCreditForm();
    }

    @FXML
    private void selectLoanType(MouseEvent event) {
        // Keep legacy UI interaction entrypoint.
    }

    @FXML
    private void submitApplication() {
        if (currentUserId <= 0) {
            showWarning("Session invalide", "Utilisateur non connecte.");
            return;
        }
        try {
            Credit credit = buildCreditFromForm();
            credit.setIdUser(currentUserId);
            if (selectedCredit == null) {
                creditService.addCreditForUser(credit, currentUserId);
                showInfo("Credit ajoute", "Votre demande de credit a ete enregistree.");
            } else {
                credit.setIdCredit(selectedCredit.getIdCredit());
                boolean ok = creditService.updateCreditForUser(credit, currentUserId);
                if (!ok) {
                    showWarning("Mise a jour impossible", "Ce credit n appartient pas a votre session.");
                    return;
                }
                showInfo("Credit mis a jour", "Le credit selectionne a ete modifie.");
            }
            reloadAllData();
            clearCreditForm();
        } catch (IllegalArgumentException ex) {
            showWarning("Validation", ex.getMessage());
        }
    }

    @FXML
    private void saveAsDraft() {
        forcedCreditStatus = "Brouillon";
        submitApplication();
        forcedCreditStatus = null;
    }

    @FXML
    private void deleteSelectedCredit() {
        if (selectedCredit == null) {
            showWarning("Selection requise", "Selectionnez un credit a supprimer.");
            return;
        }

        try {
            // Remove user guarantees linked to this credit first.
            List<GarantieCredit> toDelete = new ArrayList<>();
            for (GarantieCredit g : garanties) {
                if (g.getIdCredit() == selectedCredit.getIdCredit()) {
                    toDelete.add(g);
                }
            }
            for (GarantieCredit g : toDelete) {
                garantieService.deleteGarantieForUser(g.getIdGarantie(), currentUserId);
            }

            boolean ok = creditService.deleteCreditForUser(selectedCredit.getIdCredit(), currentUserId);
            if (!ok) {
                showWarning("Suppression impossible", "Ce credit n appartient pas a votre session.");
                return;
            }
            reloadAllData();
            clearCreditForm();
            clearGarantieForm();
            showInfo("Credit supprime", "Le credit et ses garanties associees ont ete supprimes.");
        } catch (RuntimeException ex) {
            showWarning("Erreur SQL", ex.getMessage());
        }
    }

    @FXML
    private void submitGuarantee() {
        if (currentUserId <= 0) {
            showWarning("Session invalide", "Utilisateur non connecte.");
            return;
        }
        try {
            GarantieCredit garantie = buildGarantieFromForm();
            garantie.setIdUser(currentUserId);
            if (selectedGarantie == null) {
                garantieService.addGarantieForUser(garantie, currentUserId);
                showInfo("Garantie ajoutee", "La garantie a ete enregistree.");
            } else {
                garantie.setIdGarantie(selectedGarantie.getIdGarantie());
                boolean ok = garantieService.updateGarantieForUser(garantie, currentUserId);
                if (!ok) {
                    showWarning("Mise a jour impossible", "Cette garantie n appartient pas a votre session.");
                    return;
                }
                showInfo("Garantie mise a jour", "La garantie selectionnee a ete modifiee.");
            }
            reloadAllData();
            clearGarantieForm();
        } catch (IllegalArgumentException ex) {
            showWarning("Validation garantie", ex.getMessage());
        }
    }

    @FXML
    private void deleteSelectedGarantie() {
        if (selectedGarantie == null) {
            showWarning("Selection requise", "Selectionnez une garantie a supprimer.");
            return;
        }
        boolean ok = garantieService.deleteGarantieForUser(selectedGarantie.getIdGarantie(), currentUserId);
        if (!ok) {
            showWarning("Suppression impossible", "Cette garantie n appartient pas a votre session.");
            return;
        }
        reloadAllData();
        clearGarantieForm();
        showInfo("Garantie supprimee", "La garantie selectionnee a ete supprimee.");
    }

    @FXML
    private void clearForm() {
        clearCreditForm();
    }

    @FXML
    private void clearGarantie() {
        clearGarantieForm();
    }

    @FXML
    private void viewPaymentSchedule() {
        showInfo("Echeancier", "Fonctionnalite en cours. CRUD credit et garantie sont actifs.");
    }

    @FXML
    private void openGarantiePage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sections/UserDashboardGarantieFormPage.fxml"));
            Parent root = loader.load();
            Stage owner = (Stage) rootCreditSection.getScene().getWindow();
            Stage modal = new Stage();
            modal.initOwner(owner);
            modal.initModality(Modality.WINDOW_MODAL);
            modal.setTitle("NEXORA BANK - Garantie Credit");
            modal.setScene(new Scene(root, 980, 760));
            modal.setMinWidth(900);
            modal.setMinHeight(680);
            modal.centerOnScreen();
            modal.showAndWait();
        } catch (IOException ex) {
            showWarning("Erreur", "Impossible d'ouvrir la page garantie: " + ex.getMessage());
        }
    }

    @FXML
    private void showGarantieForm() {
        if (garantiesDisplayContainer != null) {
            setVisibleManaged(garantiesDisplayContainer, true);
        }
        if (garantieFormContainer != null) {
            setVisibleManaged(garantieFormContainer, true);
        }
    }

    @FXML
    private void hideGarantieForm() {
        if (garantieFormContainer != null) {
            setVisibleManaged(garantieFormContainer, false);
        }
    }

    @FXML
    private void showGaranties() {
        if (garantiesDisplayContainer != null) {
            setVisibleManaged(garantiesDisplayContainer, true);
        }
    }

    @FXML
    private void hideGaranties() {
        if (garantiesDisplayContainer != null) {
            setVisibleManaged(garantiesDisplayContainer, false);
        }
    }

    private void configureCreditFormDefaults() {
        if (cbDuree != null) {
            cbDuree.setItems(FXCollections.observableArrayList(
                    "12", "24", "36", "48", "60", "120", "180", "240", "300", "360"
            ));
        }
        if (cbPurpose != null) {
            cbPurpose.setItems(FXCollections.observableArrayList(
                    "Hypotheque", "Pret auto", "Personnel", "Professionnel", "Education", "Sante", "Autre"
            ));
        }
        if (cbCreditStatus != null) {
            cbCreditStatus.setItems(FXCollections.observableArrayList(
                    "En attente", "Accepte", "Refuse", "En cours", "Rembourse", "Brouillon"
            ));
            cbCreditStatus.setValue("En attente");
        }
        if (txtInterestRate != null && (txtInterestRate.getText() == null || txtInterestRate.getText().isBlank())) {
            txtInterestRate.setText("3.5");
        }
        if (dpCreditDate != null && dpCreditDate.getValue() == null) {
            dpCreditDate.setValue(LocalDate.now());
        }
        if (btnSubmitCredit != null) {
            btnSubmitCredit.setText("Ajouter credit");
        }
        if (btnDeleteCredit != null) {
            btnDeleteCredit.setDisable(true);
        }
    }

    private void configureGarantieFormDefaults() {
        if (cbGarantieType != null) {
            cbGarantieType.setItems(FXCollections.observableArrayList(
                    "Real Estate Hypotheque",
                    "Vehicle Title",
                    "Personnel Guarantee",
                    "Bank Guarantee",
                    "Insurance Policy",
                    "Other Collateral"
            ));
        }
        if (cbGarantieStatus != null) {
            cbGarantieStatus.setItems(FXCollections.observableArrayList(
                    "En attente", "Verifiee", "Rejetee"
            ));
            cbGarantieStatus.setValue("En attente");
        }
        if (dpGarantieDateEvaluation != null && dpGarantieDateEvaluation.getValue() == null) {
            dpGarantieDateEvaluation.setValue(LocalDate.now());
        }
        if (btnSubmitGarantie != null) {
            btnSubmitGarantie.setText("Ajouter garantie");
        }
        if (btnDeleteGarantie != null) {
            btnDeleteGarantie.setDisable(true);
        }
    }

    private void configureCreditTable() {
        colCreditId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getIdCredit())));
        colCreditCompte.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getIdCompte())));
        colCreditType.setCellValueFactory(c -> new SimpleStringProperty(safeText(c.getValue().getTypeCredit())));
        colCreditAmount.setCellValueFactory(c -> new SimpleStringProperty(formatMoney(c.getValue().getMontantDemande())));
        colCreditStatus.setCellValueFactory(c -> new SimpleStringProperty(safeText(c.getValue().getStatut())));
        colCreditDate.setCellValueFactory(c -> new SimpleStringProperty(safeText(c.getValue().getDateDemande())));

        tblCredits.setItems(credits);
        tblCredits.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedCredit = newVal;
            if (newVal != null) {
                fillCreditForm(newVal);
                if (btnSubmitCredit != null) {
                    btnSubmitCredit.setText("Mettre a jour credit");
                }
                if (btnDeleteCredit != null) {
                    btnDeleteCredit.setDisable(false);
                }
                if (cbGarantieCreditId != null) {
                    cbGarantieCreditId.setValue(newVal.getIdCredit());
                }
            } else {
                if (btnSubmitCredit != null) {
                    btnSubmitCredit.setText("Ajouter credit");
                }
                if (btnDeleteCredit != null) {
                    btnDeleteCredit.setDisable(true);
                }
            }
        });
    }

    private void configureGarantieTable() {
        colGarantieId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getIdGarantie())));
        colGarantieCreditId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getIdCredit())));
        colGarantieType.setCellValueFactory(c -> new SimpleStringProperty(safeText(c.getValue().getTypeGarantie())));
        colGarantieValue.setCellValueFactory(c -> new SimpleStringProperty(formatMoney(c.getValue().getValeurEstimee())));
        colGarantieStatus.setCellValueFactory(c -> new SimpleStringProperty(safeText(c.getValue().getStatut())));
        colGarantieDate.setCellValueFactory(c -> new SimpleStringProperty(safeText(c.getValue().getDateEvaluation())));

        tblGaranties.setItems(garanties);
        tblGaranties.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedGarantie = newVal;
            if (newVal != null) {
                fillGarantieForm(newVal);
                if (btnSubmitGarantie != null) {
                    btnSubmitGarantie.setText("Mettre a jour garantie");
                }
                if (btnDeleteGarantie != null) {
                    btnDeleteGarantie.setDisable(false);
                }
            } else {
                if (btnSubmitGarantie != null) {
                    btnSubmitGarantie.setText("Ajouter garantie");
                }
                if (btnDeleteGarantie != null) {
                    btnDeleteGarantie.setDisable(true);
                }
            }
        });
    }

    private void setupCalculationListeners() {
        if (txtRequestedAmount != null) {
            txtRequestedAmount.textProperty().addListener((obs, ov, nv) -> {
                if (txtApprovedAmount != null && (txtApprovedAmount.getText() == null || txtApprovedAmount.getText().isBlank())) {
                    txtApprovedAmount.setText(nv);
                }
                calculateMonthlyPayment();
            });
        }
        if (txtApprovedAmount != null) {
            txtApprovedAmount.textProperty().addListener((obs, ov, nv) -> {
                if (txtRemainingAmount != null && (txtRemainingAmount.getText() == null || txtRemainingAmount.getText().isBlank())) {
                    txtRemainingAmount.setText(nv);
                }
                calculateMonthlyPayment();
            });
        }
        if (txtInterestRate != null) {
            txtInterestRate.textProperty().addListener((obs, ov, nv) -> calculateMonthlyPayment());
        }
        if (cbDuree != null) {
            cbDuree.valueProperty().addListener((obs, ov, nv) -> calculateMonthlyPayment());
        }
    }

    private void calculateMonthlyPayment() {
        try {
            double montant = parseDouble(textOrEmpty(txtApprovedAmount), 0);
            int months = parseInt(textOrEmpty(cbDuree != null ? cbDuree.getValue() : null), 0);
            double annualRate = parseDouble(textOrEmpty(txtInterestRate), 0);
            if (montant <= 0 || months <= 0) {
                if (txtMonthlyPayment != null) {
                    txtMonthlyPayment.clear();
                }
                return;
            }

            double monthly;
            if (annualRate <= 0) {
                monthly = montant / months;
            } else {
                double rate = annualRate / 100.0 / 12.0;
                double numerator = montant * rate * Math.pow(1 + rate, months);
                double denominator = Math.pow(1 + rate, months) - 1;
                monthly = denominator == 0 ? 0 : numerator / denominator;
            }
            if (txtMonthlyPayment != null) {
                txtMonthlyPayment.setText(String.format("%.2f", monthly));
            }
        } catch (Exception ignored) {
            if (txtMonthlyPayment != null) {
                txtMonthlyPayment.clear();
            }
        }
    }

    private void loadCompteIds() {
        if (cmbCompte == null) {
            return;
        }
        try {
            List<Integer> ids = creditService.getCompteIds();
            cmbCompte.setItems(FXCollections.observableArrayList(ids));
            if (!ids.isEmpty()) {
                cmbCompte.setValue(ids.get(0));
            }
        } catch (RuntimeException ex) {
            cmbCompte.setItems(FXCollections.observableArrayList());
        }
    }

    private void reloadAllData() {
        if (currentUserId <= 0) {
            return;
        }
        credits.setAll(creditService.getCreditsByUser(currentUserId));
        garanties.setAll(garantieService.getGarantiesByUser(currentUserId));

        List<Integer> userCreditIds = creditService.getCreditIdsByUser(currentUserId);
        if (cbGarantieCreditId != null) {
            cbGarantieCreditId.setItems(FXCollections.observableArrayList(userCreditIds));
            if (!userCreditIds.isEmpty() && cbGarantieCreditId.getValue() == null) {
                cbGarantieCreditId.setValue(userCreditIds.get(0));
            }
        }
    }

    private Credit buildCreditFromForm() {
        Integer idCompte = cmbCompte != null ? cmbCompte.getValue() : null;
        if (idCompte == null) {
            throw new IllegalArgumentException("Selectionnez un compte.");
        }

        String type = textOrEmpty(cbPurpose != null ? cbPurpose.getValue() : null);
        if (type.isBlank()) {
            throw new IllegalArgumentException("Selectionnez le type de pret.");
        }

        String statut = forcedCreditStatus != null
                ? forcedCreditStatus
                : textOrEmpty(cbCreditStatus != null ? cbCreditStatus.getValue() : null);
        if (statut.isBlank()) {
            statut = "En attente";
        }

        LocalDate date = dpCreditDate != null ? dpCreditDate.getValue() : null;
        if (date == null) {
            throw new IllegalArgumentException("Selectionnez la date de demande.");
        }

        double montantDemande = parseRequiredDouble(textOrEmpty(txtRequestedAmount), "Montant demande obligatoire.");
        Double montantAccord = parseOptionalDouble(textOrEmpty(txtApprovedAmount));
        int duree = parseIntRequired(textOrEmpty(cbDuree != null ? cbDuree.getValue() : null), "Duree invalide.");
        double taux = parseRequiredDouble(textOrEmpty(txtInterestRate), "Taux d interet obligatoire.");
        double mensualite = parseDouble(textOrEmpty(txtMonthlyPayment), 0);
        double restant = parseDouble(textOrEmpty(txtRemainingAmount), montantAccord == null ? montantDemande : montantAccord);

        Credit credit = new Credit(
                0,
                idCompte,
                type,
                montantDemande,
                montantAccord,
                duree,
                taux,
                mensualite,
                restant,
                date.toString(),
                statut,
                currentUserId
        );
        return credit;
    }

    private GarantieCredit buildGarantieFromForm() {
        Integer idCredit = cbGarantieCreditId != null ? cbGarantieCreditId.getValue() : null;
        if (idCredit == null) {
            throw new IllegalArgumentException("Selectionnez un credit pour la garantie.");
        }
        String type = textOrEmpty(cbGarantieType != null ? cbGarantieType.getValue() : null);
        if (type.isBlank()) {
            throw new IllegalArgumentException("Selectionnez un type de garantie.");
        }
        String adresse = textOrEmpty(txtGarantieAddress);
        if (adresse.isBlank()) {
            throw new IllegalArgumentException("Adresse du bien obligatoire.");
        }
        String nomGarant = textOrEmpty(txtGarantieNomGarant);
        if (nomGarant.isBlank()) {
            throw new IllegalArgumentException("Nom du garant obligatoire.");
        }
        LocalDate evaluation = dpGarantieDateEvaluation != null ? dpGarantieDateEvaluation.getValue() : null;
        if (evaluation == null) {
            throw new IllegalArgumentException("Date d evaluation obligatoire.");
        }
        double estimee = parseRequiredDouble(textOrEmpty(txtGarantieEstimatedValue), "Valeur estimee obligatoire.");
        double retenue = parseRequiredDouble(textOrEmpty(txtGarantieRetainedValue), "Valeur retenue obligatoire.");
        String document = textOrEmpty(txtGarantieDocument);
        String description = textOrEmpty(txtGarantieDescription);
        String statut = textOrEmpty(cbGarantieStatus != null ? cbGarantieStatus.getValue() : null);
        if (statut.isBlank()) {
            statut = "En attente";
        }

        return new GarantieCredit(
                0,
                idCredit,
                type,
                description,
                adresse,
                estimee,
                retenue,
                document,
                evaluation.toString(),
                nomGarant,
                statut,
                currentUserId
        );
    }

    private void fillCreditForm(Credit c) {
        if (cmbCompte != null) cmbCompte.setValue(c.getIdCompte());
        if (cbPurpose != null) cbPurpose.setValue(c.getTypeCredit());
        if (txtRequestedAmount != null) txtRequestedAmount.setText(String.valueOf(c.getMontantDemande()));
        if (txtApprovedAmount != null) txtApprovedAmount.setText(c.getMontantAccord() == null ? "" : String.valueOf(c.getMontantAccord()));
        if (cbDuree != null) cbDuree.setValue(String.valueOf(c.getDuree()));
        if (txtInterestRate != null) txtInterestRate.setText(String.valueOf(c.getTauxInteret()));
        if (txtMonthlyPayment != null) txtMonthlyPayment.setText(String.valueOf(c.getMensualite()));
        if (txtRemainingAmount != null) txtRemainingAmount.setText(String.valueOf(c.getMontantRestant()));
        if (cbCreditStatus != null) cbCreditStatus.setValue(c.getStatut());
        if (dpCreditDate != null) {
            try {
                dpCreditDate.setValue(LocalDate.parse(c.getDateDemande()));
            } catch (Exception ignored) {
                dpCreditDate.setValue(LocalDate.now());
            }
        }
    }

    private void fillGarantieForm(GarantieCredit g) {
        if (cbGarantieCreditId != null) cbGarantieCreditId.setValue(g.getIdCredit());
        if (cbGarantieType != null) cbGarantieType.setValue(g.getTypeGarantie());
        if (txtGarantieEstimatedValue != null) txtGarantieEstimatedValue.setText(String.valueOf(g.getValeurEstimee()));
        if (txtGarantieRetainedValue != null) txtGarantieRetainedValue.setText(String.valueOf(g.getValeurRetenue()));
        if (txtGarantieAddress != null) txtGarantieAddress.setText(g.getAdresseBien());
        if (txtGarantieNomGarant != null) txtGarantieNomGarant.setText(g.getNomGarant());
        if (txtGarantieDocument != null) txtGarantieDocument.setText(g.getDocumentJustificatif());
        if (txtGarantieDescription != null) txtGarantieDescription.setText(g.getDescription());
        if (cbGarantieStatus != null) cbGarantieStatus.setValue(g.getStatut());
        if (dpGarantieDateEvaluation != null) {
            try {
                dpGarantieDateEvaluation.setValue(LocalDate.parse(g.getDateEvaluation()));
            } catch (Exception ignored) {
                dpGarantieDateEvaluation.setValue(LocalDate.now());
            }
        }
    }

    private void clearCreditForm() {
        selectedCredit = null;
        if (tblCredits != null) {
            tblCredits.getSelectionModel().clearSelection();
        }
        if (txtRequestedAmount != null) txtRequestedAmount.clear();
        if (txtApprovedAmount != null) txtApprovedAmount.clear();
        if (cbDuree != null) cbDuree.getSelectionModel().clearSelection();
        if (txtInterestRate != null) txtInterestRate.setText("3.5");
        if (txtMonthlyPayment != null) txtMonthlyPayment.clear();
        if (txtRemainingAmount != null) txtRemainingAmount.clear();
        if (cbPurpose != null) cbPurpose.getSelectionModel().clearSelection();
        if (cbCreditStatus != null) cbCreditStatus.setValue("En attente");
        if (dpCreditDate != null) dpCreditDate.setValue(LocalDate.now());
        if (txtNotes != null) txtNotes.clear();
        if (btnSubmitCredit != null) btnSubmitCredit.setText("Ajouter credit");
        if (btnDeleteCredit != null) btnDeleteCredit.setDisable(true);
    }

    private void clearGarantieForm() {
        selectedGarantie = null;
        if (tblGaranties != null) {
            tblGaranties.getSelectionModel().clearSelection();
        }
        if (cbGarantieType != null) cbGarantieType.getSelectionModel().clearSelection();
        if (txtGarantieEstimatedValue != null) txtGarantieEstimatedValue.clear();
        if (txtGarantieRetainedValue != null) txtGarantieRetainedValue.clear();
        if (txtGarantieAddress != null) txtGarantieAddress.clear();
        if (txtGarantieNomGarant != null) txtGarantieNomGarant.clear();
        if (txtGarantieDocument != null) txtGarantieDocument.clear();
        if (txtGarantieDescription != null) txtGarantieDescription.clear();
        if (cbGarantieStatus != null) cbGarantieStatus.setValue("En attente");
        if (dpGarantieDateEvaluation != null) dpGarantieDateEvaluation.setValue(LocalDate.now());
        if (btnSubmitGarantie != null) btnSubmitGarantie.setText("Ajouter garantie");
        if (btnDeleteGarantie != null) btnDeleteGarantie.setDisable(true);
    }

    private void updateUserInfoLabel(User currentUser) {
        if (lblUserCreditInfo == null) {
            return;
        }
        if (currentUser == null) {
            lblUserCreditInfo.setText("Utilisateur: non connecte");
            return;
        }
        String prenom = safeText(currentUser.getPrenom());
        String nom = safeText(currentUser.getNom());
        String fullName = (prenom + " " + nom).trim();
        if (fullName.isBlank()) {
            fullName = safeText(currentUser.getEmail());
        }
        lblUserCreditInfo.setText("Utilisateur: " + fullName + " (#" + currentUser.getIdUser() + ")");
    }

    private void setVisibleManaged(VBox node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private String safeText(String v) {
        return v == null ? "" : v;
    }

    private String textOrEmpty(TextField tf) {
        return tf == null || tf.getText() == null ? "" : tf.getText().trim();
    }

    private String textOrEmpty(TextArea ta) {
        return ta == null || ta.getText() == null ? "" : ta.getText().trim();
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private double parseDouble(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.replace(",", ".").trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Double parseOptionalDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseDouble(value, 0);
    }

    private double parseRequiredDouble(String value, String err) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(err);
        }
        try {
            return Double.parseDouble(value.replace(",", ".").trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(err);
        }
    }

    private int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private int parseIntRequired(String value, String err) {
        int v = parseInt(value, -1);
        if (v <= 0) {
            throw new IllegalArgumentException(err);
        }
        return v;
    }

    private String formatMoney(double value) {
        return String.format("%,.2f", value);
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }

    private void showWarning(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }
}
