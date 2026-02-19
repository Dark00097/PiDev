package com.nexora.bank.controllers;

import com.nexora.bank.Models.Credit;
import com.nexora.bank.Service.CreditService;
import com.nexora.bank.Service.UserService;
import com.nexora.bank.Utils.PdfReportUtil;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class CreditController implements Initializable {

    private static final DateTimeFormatter TABLE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // â”€â”€ Statistiques â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private Label lblCreditsAccordes;
    @FXML private Label lblMontantTotal;
    @FXML private Label lblCreditsEnCours;

    // â”€â”€ Formulaire â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private ComboBox<Integer> cmbCompte;
    @FXML private ComboBox<String>  cmbTypeCredit;
    @FXML private ComboBox<String>  cmbStatut;
    @FXML private TextField         txtMontantDemande;
    @FXML private TextField         txtMontantAccord;
    @FXML private TextField         txtDuree;
    @FXML private TextField         txtTauxInteret;
    @FXML private TextField         txtMensualite;
    @FXML private TextField         txtMontantRestant;
    @FXML private DatePicker        dpDateDemande;
    @FXML private Button            btnAjouter;

    // â”€â”€ Tableau â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private TableView<Credit>                tableCredits;
    @FXML private TableColumn<Credit, String>      colCompte;
    @FXML private TableColumn<Credit, String>      colType;
    @FXML private TableColumn<Credit, String>      colMontantDemande;
    @FXML private TableColumn<Credit, String>      colMontantAccord;
    @FXML private TableColumn<Credit, String>      colDuree;
    @FXML private TableColumn<Credit, String>      colTaux;
    @FXML private TableColumn<Credit, String>      colMensualite;
    @FXML private TableColumn<Credit, String>      colRestant;
    @FXML private TableColumn<Credit, String>      colDate;
    @FXML private TableColumn<Credit, String>      colStatut;
    @FXML private TableColumn<Credit, Void>        colActions;

    // â”€â”€ Recherche / info â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private TextField txtRecherche;
    @FXML private Label     lblTableInfo;

    // â”€â”€ Ã‰tat interne â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private final CreditService              creditService = new CreditService();
    private final UserService                userService   = new UserService();
    private final ObservableList<Credit>     creditsList   = FXCollections.observableArrayList();
    private       FilteredList<Credit>       filteredData;
    private       Credit                     selectedCredit;
    private       boolean                    isEditMode;

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  INIT
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        initializeSearch();
        setupAutoCalculation();
        loadCompteIds();
        loadCredits();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  TABLE
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void initializeTable() {
        colCompte.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().getIdCompte())));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeCredit"));
        colMontantDemande.setCellValueFactory(cell ->
                new SimpleStringProperty(formatAmount(cell.getValue().getMontantDemande())));
        colMontantAccord.setCellValueFactory(cell -> {
            Double m = cell.getValue().getMontantAccord();
            return new SimpleStringProperty(m == null ? "-" : formatAmount(m));
        });
        colDuree.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDuree() + " mois"));
        colTaux.setCellValueFactory(cell ->
                new SimpleStringProperty(formatPercent(cell.getValue().getTauxInteret())));
        colMensualite.setCellValueFactory(cell ->
                new SimpleStringProperty(formatAmount(cell.getValue().getMensualite())));
        colRestant.setCellValueFactory(cell ->
                new SimpleStringProperty(formatAmount(cell.getValue().getMontantRestant())));
        colDate.setCellValueFactory(cell ->
                new SimpleStringProperty(formatDateForTable(cell.getValue().getDateDemande())));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // â”€â”€ Badge statut â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().add("nx-badge");
                String low = item.toLowerCase();
                if      (low.contains("accept"))   badge.getStyleClass().add("nx-badge-success");
                else if (low.contains("cours"))    badge.getStyleClass().add("nx-badge-warning");
                else if (low.contains("rembours")) badge.getStyleClass().add("nx-badge-info");
                else                               badge.getStyleClass().add("nx-badge-error");
                setGraphic(badge);
            }
        });

        // â”€â”€ Actions (edit + delete) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnEdit   = new Button();
            private final Button btnDelete = new Button();
            private final HBox   hbox      = new HBox(8);
            {
                btnEdit.getStyleClass().addAll("nx-table-action", "nx-table-action-edit");
                SVGPath editIcon = new SVGPath();
                editIcon.setContent("M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z");
                editIcon.setStyle("-fx-fill: #00B4A0;");
                btnEdit.setGraphic(editIcon);
                btnEdit.setTooltip(new Tooltip("Modifier"));

                btnDelete.getStyleClass().addAll("nx-table-action", "nx-table-action-delete");
                SVGPath deleteIcon = new SVGPath();
                deleteIcon.setContent("M19 7l-.867 12.142A2 2 0 0 1 16.138 21H7.862a2 2 0 0 1-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v3M4 7h16");
                deleteIcon.setStyle("-fx-fill: #EF4444;");
                btnDelete.setGraphic(deleteIcon);
                btnDelete.setTooltip(new Tooltip("Supprimer"));

                hbox.setAlignment(Pos.CENTER);
                hbox.getChildren().addAll(btnEdit, btnDelete);

                btnEdit.setOnAction(e -> {
                    Credit c = getTableView().getItems().get(getIndex());
                    editCredit(c);
                });
                btnDelete.setOnAction(e -> {
                    Credit c = getTableView().getItems().get(getIndex());
                    deleteCredit(c);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });

        // â”€â”€ SÃ©lection ligne â†’ formulaire â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        tableCredits.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) editCredit(newVal);
        });
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  RECHERCHE
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void initializeSearch() {
        filteredData = new FilteredList<>(creditsList, c -> true);
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredData.setPredicate(credit -> q.isEmpty()
                    || String.valueOf(credit.getIdCredit()).contains(q)
                    || String.valueOf(credit.getIdCompte()).contains(q)
                    || safeLower(credit.getTypeCredit()).contains(q)
                    || safeLower(credit.getStatut()).contains(q));
            updateTableInfo();
        });
        SortedList<Credit> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(tableCredits.comparatorProperty());
        tableCredits.setItems(sorted);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  AUTO-CALCUL
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void setupAutoCalculation() {
        txtMontantAccord.textProperty().addListener((obs, o, n) -> { calculateMensualite(); syncMontantRestant(); });
        txtDuree.textProperty().addListener((obs, o, n) -> calculateMensualite());
        txtTauxInteret.textProperty().addListener((obs, o, n) -> calculateMensualite());
    }

    private void calculateMensualite() {
        try {
            double montant    = parseDouble(txtMontantAccord.getText(), 0);
            int    duree      = (int) parseDouble(txtDuree.getText(), 0);
            double tauxAnnuel = parseDouble(txtTauxInteret.getText(), 0);
            if (montant <= 0 || duree <= 0) { txtMensualite.clear(); return; }
            double mensualite;
            if (tauxAnnuel <= 0) {
                mensualite = montant / duree;
            } else {
                double tauxM = tauxAnnuel / 100.0 / 12.0;
                double num   = montant * tauxM * Math.pow(1 + tauxM, duree);
                double den   = Math.pow(1 + tauxM, duree) - 1;
                mensualite   = den == 0 ? 0 : num / den;
            }
            txtMensualite.setText(String.format("%.2f", mensualite));
        } catch (NumberFormatException e) {
            txtMensualite.clear();
        }
    }

    private void syncMontantRestant() {
        if (txtMontantRestant.getText() == null || txtMontantRestant.getText().isBlank())
            txtMontantRestant.setText(txtMontantAccord.getText());
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  CHARGEMENT
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void loadCompteIds() {
        cmbCompte.setItems(FXCollections.observableArrayList(creditService.getCompteIds()));
    }

    private void loadCredits() {
        creditsList.setAll(creditService.getAllCredits());
        updateStats();
        updateTableInfo();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  STATS HEADER
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void updateStats() {
        long accordes = creditsList.stream().filter(c -> {
            String s = safeLower(c.getStatut());
            return s.contains("accept") || s.contains("cours") || s.contains("rembours");
        }).count();
        double total = creditsList.stream()
                .map(Credit::getMontantAccord).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).sum();
        long enCours = creditsList.stream()
                .filter(c -> safeLower(c.getStatut()).contains("cours")).count();

        if (lblCreditsAccordes != null) lblCreditsAccordes.setText(String.valueOf(accordes));
        if (lblMontantTotal    != null) lblMontantTotal.setText(formatAmount(total));
        if (lblCreditsEnCours  != null) lblCreditsEnCours.setText(String.valueOf(enCours));
    }

    private void updateTableInfo() {
        if (lblTableInfo != null) {
            int total    = creditsList.size();
            int filtered = filteredData != null ? filteredData.size() : total;
            lblTableInfo.setText(String.format("Affichage de %d sur %d crÃ©dits", filtered, total));
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  FORMULAIRE
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void populateForm(Credit c) {
        if (!cmbCompte.getItems().contains(c.getIdCompte()))
            cmbCompte.getItems().add(c.getIdCompte());
        cmbCompte.setValue(c.getIdCompte());
        cmbTypeCredit.setValue(c.getTypeCredit());
        txtMontantDemande.setText(String.valueOf(c.getMontantDemande()));
        txtMontantAccord.setText(c.getMontantAccord() == null ? "" : String.valueOf(c.getMontantAccord()));
        txtDuree.setText(String.valueOf(c.getDuree()));
        txtTauxInteret.setText(String.valueOf(c.getTauxInteret()));
        txtMensualite.setText(String.valueOf(c.getMensualite()));
        txtMontantRestant.setText(String.valueOf(c.getMontantRestant()));
        dpDateDemande.setValue(parseDate(c.getDateDemande()));
        cmbStatut.setValue(c.getStatut());
    }

    private void clearForm() {
        cmbCompte.setValue(null);
        cmbTypeCredit.setValue(null);
        txtMontantDemande.clear();
        txtMontantAccord.clear();
        txtDuree.clear();
        txtTauxInteret.clear();
        txtMensualite.clear();
        txtMontantRestant.clear();
        dpDateDemande.setValue(null);
        cmbStatut.setValue(null);
        selectedCredit = null;
        isEditMode     = false;
        btnAjouter.setText("Enregistrer");
        tableCredits.getSelectionModel().clearSelection();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  ACTIONS FXML
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML
    void handleAjouter(ActionEvent event) {
        try {
            Integer idCompte = cmbCompte.getValue();
            String  type     = cmbTypeCredit.getValue();
            String  statut   = cmbStatut.getValue();
            LocalDate date   = dpDateDemande.getValue();

            if (idCompte == null || type == null || statut == null || date == null) {
                showWarning("Validation", "SÃ©lectionnez compte, type, date et statut.");
                return;
            }

            double montantDemande  = parseRequiredDouble(txtMontantDemande.getText(), "Montant demandÃ©");
            int    duree           = (int) parseRequiredDouble(txtDuree.getText(), "DurÃ©e");
            double taux            = parseRequiredDouble(txtTauxInteret.getText(), "Taux d'intÃ©rÃªt");
            Double montantAccord   = parseOptionalDouble(txtMontantAccord.getText());
            double mensualite      = parseDouble(txtMensualite.getText(), 0);
            double montantRestant  = parseDouble(txtMontantRestant.getText(), montantAccord == null ? 0 : montantAccord);

            Credit credit = new Credit(0, idCompte, type, montantDemande, montantAccord,
                    duree, taux, mensualite, montantRestant, date.toString(), statut);

            if (isEditMode && selectedCredit != null) {
                credit.setIdCredit(selectedCredit.getIdCredit());
                boolean ok = creditService.updateCredit(credit);
                if (!ok) { showWarning("Erreur", "Ã‰chec de la modification du crÃ©dit."); return; }
                showInfo("SuccÃ¨s", "CrÃ©dit modifiÃ© avec succÃ¨s.");
            } else {
                creditService.addCredit(credit);
                showInfo("SuccÃ¨s", "CrÃ©dit ajoutÃ© avec succÃ¨s.");
            }

            clearForm();
            loadCredits();

        } catch (IllegalArgumentException ex) {
            showWarning("Erreur de saisie", ex.getMessage());
        } catch (RuntimeException ex) {
            showWarning("Erreur", "OpÃ©ration impossible : " + ex.getMessage());
        }
    }

    @FXML
    void handleSupprimer(ActionEvent event) {
        if (selectedCredit == null) {
            showWarning("Avertissement", "SÃ©lectionnez un crÃ©dit Ã  supprimer.");
            return;
        }
        deleteCredit(selectedCredit);
    }

    @FXML
    void handleAnnuler(ActionEvent event) {
        clearForm();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  EDIT / DELETE
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void editCredit(Credit credit) {
        selectedCredit = credit;
        populateForm(credit);
        isEditMode = true;
        btnAjouter.setText("Modifier");
    }

    private void deleteCredit(Credit credit) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le crÃ©dit #" + credit.getIdCredit() + " ?");
        confirm.setContentText("Cette action est irrÃ©versible.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            boolean deleted = creditService.deleteCredit(credit.getIdCredit());
            if (deleted) {
                clearForm();
                loadCredits();
                showInfo("SuccÃ¨s", "CrÃ©dit supprimÃ© avec succÃ¨s.");
            } else {
                showWarning("Erreur", "Ã‰chec de la suppression du crÃ©dit.");
            }
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  TRIS
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML void trierParType(ActionEvent event)    { creditsList.sort(Comparator.comparing(c -> safeLower(c.getTypeCredit()))); }
    @FXML void trierParMontant(ActionEvent event) { creditsList.sort(Comparator.comparingDouble(Credit::getMontantDemande).reversed()); }
    @FXML void trierParDate(ActionEvent event)    { creditsList.sort(Comparator.comparing((Credit c) -> parseDate(c.getDateDemande()), Comparator.nullsLast(Comparator.naturalOrder())).reversed()); }
    @FXML void trierParStatut(ActionEvent event)  { creditsList.sort(Comparator.comparing(c -> safeLower(c.getStatut()))); }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  PAGINATION
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML void pageFirst(ActionEvent event) { tableCredits.scrollTo(0); }
    @FXML void pagePrev(ActionEvent event)  { tableCredits.scrollTo(Math.max(tableCredits.getSelectionModel().getSelectedIndex() - 10, 0)); }
    @FXML void pageNext(ActionEvent event)  { tableCredits.scrollTo(tableCredits.getSelectionModel().getSelectedIndex() + 10); }
    @FXML void pageLast(ActionEvent event)  { tableCredits.scrollTo(Integer.MAX_VALUE); }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  EXPORT PDF
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML
    void exporterPDF(ActionEvent event) {
        List<Credit> rows = new ArrayList<>(tableCredits.getItems());
        if (rows.isEmpty()) { showWarning("Export PDF", "Aucune donnÃ©e Ã  exporter."); return; }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les crÃ©dits en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        chooser.setInitialFileName("credits_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        File file = chooser.showSaveDialog(tableCredits.getScene().getWindow());
        if (file == null) return;
        try {
            PdfReportUtil.exportCredits(rows, file);
            showInfo("Export PDF", "PDF enregistrÃ© : " + file.getAbsolutePath());
        } catch (IOException ex) {
            showWarning("Export PDF", "Ã‰chec de l'export : " + ex.getMessage());
        }
    }

    @FXML
    void envoyerEmail(ActionEvent event) {
        Credit credit = selectedCredit != null ? selectedCredit : tableCredits.getSelectionModel().getSelectedItem();
        if (credit == null) {
            showWarning("Envoyer Email", "Selectionnez un credit dans le tableau.");
            return;
        }
        if (credit.getIdUser() <= 0) {
            showWarning("Envoyer Email", "Ce credit n'est lie a aucun utilisateur.");
            return;
        }

        var userOptional = userService.findByIdPublic(credit.getIdUser());
        if (userOptional.isEmpty()) {
            showWarning("Envoyer Email", "Utilisateur introuvable pour ce credit.");
            return;
        }

        var user = userOptional.get();
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            showWarning("Envoyer Email", "L'utilisateur n'a pas d'adresse email.");
            return;
        }

        String fullName = ((user.getPrenom() == null ? "" : user.getPrenom().trim()) + " "
                + (user.getNom() == null ? "" : user.getNom().trim())).trim();
        if (fullName.isBlank()) fullName = user.getEmail();

        String montantAccordTxt = credit.getMontantAccord() == null ? "-" : formatAmount(credit.getMontantAccord());
        String subject = "NEXORA - Details de votre credit #" + credit.getIdCredit();
        String html = """
                <html><body style='font-family:Segoe UI,Arial,sans-serif;color:#1f2937;'>
                <h2 style='color:#0A2540;'>Details de votre credit</h2>
                <p>Bonjour %s,</p>
                <p>Voici les informations de votre credit:</p>
                <table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;'>
                  <tr><td><b>ID Credit</b></td><td>%d</td></tr>
                  <tr><td><b>ID Compte</b></td><td>%d</td></tr>
                  <tr><td><b>Type Credit</b></td><td>%s</td></tr>
                  <tr><td><b>Montant Demande</b></td><td>%s</td></tr>
                  <tr><td><b>Montant Accord</b></td><td>%s</td></tr>
                  <tr><td><b>Duree</b></td><td>%d mois</td></tr>
                  <tr><td><b>Taux Interet</b></td><td>%s</td></tr>
                  <tr><td><b>Mensualite</b></td><td>%s</td></tr>
                  <tr><td><b>Montant Restant</b></td><td>%s</td></tr>
                  <tr><td><b>Date Demande</b></td><td>%s</td></tr>
                  <tr><td><b>Statut</b></td><td>%s</td></tr>
                </table>
                <p style='margin-top:16px;'>Merci de votre confiance,<br/>NEXORA BANK</p>
                </body></html>
                """.formatted(
                fullName,
                credit.getIdCredit(),
                credit.getIdCompte(),
                credit.getTypeCredit() == null ? "-" : credit.getTypeCredit(),
                formatAmount(credit.getMontantDemande()),
                montantAccordTxt,
                credit.getDuree(),
                formatPercent(credit.getTauxInteret()),
                formatAmount(credit.getMensualite()),
                formatAmount(credit.getMontantRestant()),
                credit.getDateDemande() == null ? "-" : credit.getDateDemande(),
                credit.getStatut() == null ? "-" : credit.getStatut()
        );

        try {
            userService.sendCustomEmail(user.getEmail(), subject, html);
            showInfo("Envoyer Email", "Email envoye a " + user.getEmail());
        } catch (RuntimeException ex) {
            showWarning("Envoyer Email", "Echec d'envoi: " + ex.getMessage());
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  STATISTIQUES â€” popup flottant (mÃªme style que CompteBancaireController)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @FXML
    void ouvrirStatistiques(ActionEvent event) {
        if (creditsList.isEmpty()) {
            showWarning("Statistiques", "Aucun credit a analyser.");
            return;
        }

        Map<String, Long> counts = new HashMap<>();
        creditsList.forEach(c -> counts.merge(
                c.getTypeCredit() != null ? c.getTypeCredit() : "Inconnu",
                1L, Long::sum
        ));

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        counts.forEach((type, cnt) -> pieData.add(new PieChart.Data(type + " (" + cnt + ")", cnt)));

        PieChart chart = new PieChart(pieData);
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        chart.setClockwise(true);
        chart.setStartAngle(90);
        chart.setPrefSize(460, 300);
        chart.setStyle("-fx-background-color: transparent;");

        Label title = new Label("Statistiques des Credits");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0A2540;");
        Label sub = new Label("Repartition par type de credit");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color:#FACC15;-fx-text-fill:#0A2540;-fx-font-size:12px;" +
                "-fx-font-weight:bold;-fx-padding:7 24;-fx-background-radius:9;-fx-cursor:hand;");

        HBox footer = new HBox(closeBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(12, title, sub, chart, footer);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:14;" +
                "-fx-effect:dropshadow(gaussian, rgba(10,37,64,0.22), 28, 0, 0, 8);");

        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(ownerStage);
        popup.setResizable(false);

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        closeBtn.setOnAction(e -> popup.close());

        popup.setOnShown(e -> {
            popup.setX(ownerStage.getX() + ownerStage.getWidth() / 2 - popup.getWidth() / 2);
            popup.setY(ownerStage.getY() + ownerStage.getHeight() / 2 - popup.getHeight() / 2);
        });

        popup.show();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  UTILITAIRES
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private double parseDouble(String value, double def) {
        if (value == null || value.isBlank()) return def;
        try { return Double.parseDouble(value.trim().replace(",", ".")); }
        catch (NumberFormatException e) { return def; }
    }

    private double parseRequiredDouble(String value, String fieldName) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(fieldName + " est obligatoire.");
        return parseDouble(value, 0);
    }

    private Double parseOptionalDouble(String value) {
        return (value == null || value.isBlank()) ? null : parseDouble(value, 0);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException e) { return null; }
    }

    private String formatDateForTable(String value) {
        LocalDate date = parseDate(value);
        return date == null ? (value == null ? "" : value) : date.format(TABLE_DATE_FORMAT);
    }

    private String formatAmount(double value)  { return String.format("%,.2f DT", value); }
    private String formatPercent(double value) { return String.format("%.2f%%", value); }
    private String safeLower(String value)     { return value == null ? "" : value.toLowerCase(); }
    private String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}
