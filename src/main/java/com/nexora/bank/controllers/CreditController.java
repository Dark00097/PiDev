package com.nexora.bank.controllers;

import com.nexora.bank.Models.Credit;
import com.nexora.bank.Service.CreditService;
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

    // ── Statistiques ─────────────────────────────────────────────────────────
    @FXML private Label lblCreditsAccordes;
    @FXML private Label lblMontantTotal;
    @FXML private Label lblCreditsEnCours;

    // ── Formulaire ────────────────────────────────────────────────────────────
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

    // ── Tableau ───────────────────────────────────────────────────────────────
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

    // ── Recherche / info ──────────────────────────────────────────────────────
    @FXML private TextField txtRecherche;
    @FXML private Label     lblTableInfo;

    // ── État interne ──────────────────────────────────────────────────────────
    private final CreditService              creditService = new CreditService();
    private final ObservableList<Credit>     creditsList   = FXCollections.observableArrayList();
    private       FilteredList<Credit>       filteredData;
    private       Credit                     selectedCredit;
    private       boolean                    isEditMode;

    // ─────────────────────────────────────────────────────────────────────────
    //  INIT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        initializeSearch();
        setupAutoCalculation();
        loadCompteIds();
        loadCredits();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TABLE
    // ─────────────────────────────────────────────────────────────────────────

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

        // ── Badge statut ──────────────────────────────────────────────────────
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

        // ── Actions (edit + delete) ───────────────────────────────────────────
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

        // ── Sélection ligne → formulaire ──────────────────────────────────────
        tableCredits.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) editCredit(newVal);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RECHERCHE
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    //  AUTO-CALCUL
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    //  CHARGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    private void loadCompteIds() {
        cmbCompte.setItems(FXCollections.observableArrayList(creditService.getCompteIds()));
    }

    private void loadCredits() {
        creditsList.setAll(creditService.getAllCredits());
        updateStats();
        updateTableInfo();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STATS HEADER
    // ─────────────────────────────────────────────────────────────────────────

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
            lblTableInfo.setText(String.format("Affichage de %d sur %d crédits", filtered, total));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FORMULAIRE
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    //  ACTIONS FXML
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void handleAjouter(ActionEvent event) {
        try {
            Integer idCompte = cmbCompte.getValue();
            String  type     = cmbTypeCredit.getValue();
            String  statut   = cmbStatut.getValue();
            LocalDate date   = dpDateDemande.getValue();

            if (idCompte == null || type == null || statut == null || date == null) {
                showWarning("Validation", "Sélectionnez compte, type, date et statut.");
                return;
            }

            double montantDemande  = parseRequiredDouble(txtMontantDemande.getText(), "Montant demandé");
            int    duree           = (int) parseRequiredDouble(txtDuree.getText(), "Durée");
            double taux            = parseRequiredDouble(txtTauxInteret.getText(), "Taux d'intérêt");
            Double montantAccord   = parseOptionalDouble(txtMontantAccord.getText());
            double mensualite      = parseDouble(txtMensualite.getText(), 0);
            double montantRestant  = parseDouble(txtMontantRestant.getText(), montantAccord == null ? 0 : montantAccord);

            Credit credit = new Credit(0, idCompte, type, montantDemande, montantAccord,
                    duree, taux, mensualite, montantRestant, date.toString(), statut);

            if (isEditMode && selectedCredit != null) {
                credit.setIdCredit(selectedCredit.getIdCredit());
                boolean ok = creditService.updateCredit(credit);
                if (!ok) { showWarning("Erreur", "Échec de la modification du crédit."); return; }
                showInfo("Succès", "Crédit modifié avec succès.");
            } else {
                creditService.addCredit(credit);
                showInfo("Succès", "Crédit ajouté avec succès.");
            }

            clearForm();
            loadCredits();

        } catch (IllegalArgumentException ex) {
            showWarning("Erreur de saisie", ex.getMessage());
        } catch (RuntimeException ex) {
            showWarning("Erreur", "Opération impossible : " + ex.getMessage());
        }
    }

    @FXML
    void handleSupprimer(ActionEvent event) {
        if (selectedCredit == null) {
            showWarning("Avertissement", "Sélectionnez un crédit à supprimer.");
            return;
        }
        deleteCredit(selectedCredit);
    }

    @FXML
    void handleAnnuler(ActionEvent event) {
        clearForm();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EDIT / DELETE
    // ─────────────────────────────────────────────────────────────────────────

    private void editCredit(Credit credit) {
        selectedCredit = credit;
        populateForm(credit);
        isEditMode = true;
        btnAjouter.setText("Modifier");
    }

    private void deleteCredit(Credit credit) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le crédit #" + credit.getIdCredit() + " ?");
        confirm.setContentText("Cette action est irréversible.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            boolean deleted = creditService.deleteCredit(credit.getIdCredit());
            if (deleted) {
                clearForm();
                loadCredits();
                showInfo("Succès", "Crédit supprimé avec succès.");
            } else {
                showWarning("Erreur", "Échec de la suppression du crédit.");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TRIS
    // ─────────────────────────────────────────────────────────────────────────

    @FXML void trierParType(ActionEvent event)    { creditsList.sort(Comparator.comparing(c -> safeLower(c.getTypeCredit()))); }
    @FXML void trierParMontant(ActionEvent event) { creditsList.sort(Comparator.comparingDouble(Credit::getMontantDemande).reversed()); }
    @FXML void trierParDate(ActionEvent event)    { creditsList.sort(Comparator.comparing((Credit c) -> parseDate(c.getDateDemande()), Comparator.nullsLast(Comparator.naturalOrder())).reversed()); }
    @FXML void trierParStatut(ActionEvent event)  { creditsList.sort(Comparator.comparing(c -> safeLower(c.getStatut()))); }

    // ─────────────────────────────────────────────────────────────────────────
    //  PAGINATION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML void pageFirst(ActionEvent event) { tableCredits.scrollTo(0); }
    @FXML void pagePrev(ActionEvent event)  { tableCredits.scrollTo(Math.max(tableCredits.getSelectionModel().getSelectedIndex() - 10, 0)); }
    @FXML void pageNext(ActionEvent event)  { tableCredits.scrollTo(tableCredits.getSelectionModel().getSelectedIndex() + 10); }
    @FXML void pageLast(ActionEvent event)  { tableCredits.scrollTo(Integer.MAX_VALUE); }

    // ─────────────────────────────────────────────────────────────────────────
    //  EXPORT PDF
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void exporterPDF(ActionEvent event) {
        List<Credit> rows = new ArrayList<>(tableCredits.getItems());
        if (rows.isEmpty()) { showWarning("Export PDF", "Aucune donnée à exporter."); return; }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les crédits en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
        chooser.setInitialFileName("credits_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        File file = chooser.showSaveDialog(tableCredits.getScene().getWindow());
        if (file == null) return;
        try {
            PdfReportUtil.exportCredits(rows, file);
            showInfo("Export PDF", "PDF enregistré : " + file.getAbsolutePath());
        } catch (IOException ex) {
            showWarning("Export PDF", "Échec de l'export : " + ex.getMessage());
        }
    }

    @FXML
    void envoyerSMS(ActionEvent event) {
        showInfo("SMS", "Fonctionnalité d'envoi SMS en cours de développement.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STATISTIQUES — popup flottant (même style que CompteBancaireController)
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void ouvrirStatistiques(ActionEvent event) {

        // ── 1. DONNÉES ────────────────────────────────────────────────────────
        Map<String, Long> counts = new HashMap<>();
        creditsList.forEach(c -> counts.merge(
                c.getTypeCredit() != null ? c.getTypeCredit() : "Inconnu", 1L, Long::sum));
        long total = creditsList.size();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        counts.forEach((type, cnt) -> pieData.add(new PieChart.Data(type, cnt)));

        // ── 2. PIE CHART ──────────────────────────────────────────────────────
        PieChart chart = new PieChart(pieData);
        chart.setLegendVisible(false);
        chart.setLabelsVisible(false);
        chart.setStartAngle(90);
        chart.setClockwise(false);
        chart.setPrefSize(190, 190);
        chart.setMinSize(190, 190);
        chart.setMaxSize(190, 190);
        chart.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        // ── 3. LABEL CENTRAL % ────────────────────────────────────────────────
        Label centerLabel = new Label("");
        centerLabel.setMouseTransparent(true);
        centerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', sans-serif;");
        StackPane chartContainer = new StackPane(chart, centerLabel);
        chartContainer.setPrefSize(190, 190);
        chartContainer.setMinSize(190, 190);
        chartContainer.setMaxSize(190, 190);

        // ── 4. LÉGENDE ────────────────────────────────────────────────────────
        VBox legend = new VBox(10);
        legend.setAlignment(Pos.CENTER_LEFT);

        // ── 5. COULEURS + HOVER ───────────────────────────────────────────────
        Platform.runLater(() -> {
            for (PieChart.Data d : chart.getData()) {
                String name = safeLower(d.getName());
                String color;
                if      (name.contains("immobilier"))  color = "#00B4A0";
                else if (name.contains("consommation")) color = "#0A2540";
                else if (name.contains("auto"))         color = "#FACC15";
                else if (name.contains("professionnel")) color = "#3B82F6";
                else                                    color = "#9CA3AF";

                d.getNode().setStyle("-fx-pie-color: " + color + "; -fx-border-color: white; -fx-border-width: 2;");

                double pct = total > 0 ? (d.getPieValue() * 100.0 / total) : 0;

                ScaleTransition st = new ScaleTransition(Duration.millis(400), d.getNode());
                st.setFromX(0); st.setFromY(0); st.setToX(1); st.setToY(1);
                st.setInterpolator(Interpolator.EASE_OUT);
                st.play();

                final String fc = color;
                d.getNode().setOnMouseEntered(e -> {
                    d.getNode().setScaleX(1.07); d.getNode().setScaleY(1.07);
                    centerLabel.setText(String.format("%.0f%%", pct));
                    centerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + fc +
                            "; -fx-font-family: 'Segoe UI', sans-serif;");
                });
                d.getNode().setOnMouseExited(e -> {
                    d.getNode().setScaleX(1); d.getNode().setScaleY(1);
                    centerLabel.setText("");
                });

                Region swatch = new Region();
                swatch.setPrefSize(10, 10); swatch.setMinSize(10, 10);
                swatch.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");

                Label lbl = new Label(capitalise(d.getName()) + "   " + String.format("%.0f%%", pct));
                lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1F2937;" +
                        " -fx-font-family: 'Segoe UI', sans-serif;");

                HBox row = new HBox(8, swatch, lbl);
                row.setAlignment(Pos.CENTER_LEFT);
                legend.getChildren().add(row);
            }
        });

        // ── 6. TITRE ──────────────────────────────────────────────────────────
        Label title = new Label("Types de Crédits");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0A2540;" +
                " -fx-font-family: 'Segoe UI', sans-serif;");

        Label subtitle = new Label("Répartition des crédits par type");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280; -fx-font-family: 'Segoe UI', sans-serif;");

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: #E5E7EB;");

        VBox titleBox = new VBox(3, title, subtitle);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // ── 7. BOUTON FERMER ──────────────────────────────────────────────────
        Button closeBtn = new Button("Fermer");
        String btnN = "-fx-background-color:#FACC15;-fx-text-fill:#0A2540;-fx-font-size:12px;" +
                "-fx-font-weight:bold;-fx-padding:7 24;-fx-background-radius:9;-fx-cursor:hand;" +
                "-fx-font-family:'Segoe UI',sans-serif;";
        String btnH = "-fx-background-color:#EAB308;-fx-text-fill:#0A2540;-fx-font-size:12px;" +
                "-fx-font-weight:bold;-fx-padding:7 24;-fx-background-radius:9;-fx-cursor:hand;" +
                "-fx-font-family:'Segoe UI',sans-serif;";
        closeBtn.setStyle(btnN);
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(btnH));
        closeBtn.setOnMouseExited(e  -> closeBtn.setStyle(btnN));

        HBox btnRow = new HBox(closeBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        // ── 8. CARD ───────────────────────────────────────────────────────────
        HBox chartRow = new HBox(24, chartContainer, legend);
        chartRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, titleBox, sep, chartRow, btnRow);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(22, 26, 20, 26));
        card.setPrefWidth(400);
        card.setMaxWidth(400);
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian, rgba(10,37,64,0.25), 30, 0, 0, 8);"
        );

        // ── 9. STAGE TRANSPARENT ──────────────────────────────────────────────
        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Stage popupStage = new Stage();
        popupStage.initStyle(StageStyle.TRANSPARENT);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initOwner(ownerStage);
        popupStage.setResizable(false);

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: transparent;");
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popupStage.setScene(scene);

        closeBtn.setOnAction(e -> popupStage.close());

        popupStage.setOnShown(e -> {
            double cx = ownerStage.getX() + ownerStage.getWidth()  / 2;
            double cy = ownerStage.getY() + ownerStage.getHeight() / 2;
            popupStage.setX(cx - popupStage.getWidth()  / 2);
            popupStage.setY(cy - popupStage.getHeight() / 2);
        });

        // ── 10. ANIMATION ─────────────────────────────────────────────────────
        card.setScaleX(0.80); card.setScaleY(0.80); card.setOpacity(0);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(250), card);
        scaleIn.setToX(1); scaleIn.setToY(1);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), card);
        fadeIn.setToValue(1);

        popupStage.show();
        scaleIn.play();
        fadeIn.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────

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