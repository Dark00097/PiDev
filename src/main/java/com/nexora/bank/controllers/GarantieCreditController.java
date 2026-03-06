package com.nexora.bank.controllers;

import com.nexora.bank.Models.GarantieCredit;
import com.nexora.bank.Service.GarentieCreditService;
import javafx.animation.*;
import javafx.application.Platform;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * ══════════════════════════════════════════════════════════════════
 * GarantieCreditController — style unifié avec CompteBancaireController
 * ══════════════════════════════════════════════════════════════════
 *
 * Alignements apportés :
 *  - Même structure initialize() → initializeTable() + initializeSearch() + setupTableSelection() + refreshData()
 *  - colActions avec 3 boutons : Edit (vert), Delete (rouge), View (jaune) + SVGPath icons
 *  - Tri via @FXML methods (trierParX)
 *  - Export PDF inline via PDFBox (même style de mise en page que CompteBancaireController)
 *  - Popup statistiques PieChart avec animation ScaleTransition + FadeTransition
 *  - Stats labels : lblNombreGaranties, lblGarantiesActives, lblValeurTotale
 *  - clearForm() / populateForm() / validateForm() / showInfo() / showWarning()
 *  - isEditMode + selectedGarantie pattern
 */
public class GarantieCreditController implements Initializable {

    private static final DateTimeFormatter TABLE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final GarentieCreditService garantieService = new GarentieCreditService();

    // ── Stats ──────────────────────────────────────────────────────
    @FXML private Label lblNombreGaranties;
    @FXML private Label lblValeurTotale;
    @FXML private Label lblGarantiesActives;
    @FXML private Label lblTableInfo;

    // ── Form ───────────────────────────────────────────────────────
    @FXML private ComboBox<Integer> cmbCredit;
    @FXML private ComboBox<String>  cmbTypeGarantie;
    @FXML private ComboBox<String>  cmbStatut;
    @FXML private TextArea          txtDescription;
    @FXML private TextField         txtAdresseBien;
    @FXML private TextField         txtValeurEstimee;
    @FXML private TextField         txtValeurRetenue;
    @FXML private TextField         txtDocumentJustificatif;
    @FXML private TextField         txtNomGarant;
    @FXML private TextField         txtRecherche;
    @FXML private DatePicker        dpDateEvaluation;
    @FXML private Button            btnAjouter;
    @FXML private Button            btnAnnuler;
    @FXML private Button            btnSupprimer;

    // ── Table ──────────────────────────────────────────────────────
    @FXML private TableView<GarantieCredit>              tableGaranties;
    @FXML private TableColumn<GarantieCredit, String>    colCredit;
    @FXML private TableColumn<GarantieCredit, String>    colType;
    @FXML private TableColumn<GarantieCredit, String>    colValeurEstimee;
    @FXML private TableColumn<GarantieCredit, String>    colValeurRetenue;
    @FXML private TableColumn<GarantieCredit, String>    colAdresse;
    @FXML private TableColumn<GarantieCredit, String>    colGarant;
    @FXML private TableColumn<GarantieCredit, String>    colDate;
    @FXML private TableColumn<GarantieCredit, String>    colStatut;
    @FXML private TableColumn<GarantieCredit, Void>      colActions;

    // ── State ──────────────────────────────────────────────────────
    private final ObservableList<GarantieCredit> garantiesList = FXCollections.observableArrayList();
    private FilteredList<GarantieCredit> filteredData;
    private GarantieCredit selectedGarantie = null;
    private boolean isEditMode = false;

    // ══════════════════════════════════════════════════════════════
    // INITIALIZE
    // ══════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        initializeSearch();
        setupTableSelection();
        loadCreditIds();
        refreshData();
    }

    // ══════════════════════════════════════════════════════════════
    // TABLE
    // ══════════════════════════════════════════════════════════════

    private void initializeTable() {
        colActions.setPrefWidth(80);
        colActions.setMinWidth(80);

        colCredit.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cell.getValue().getIdCredit())));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeGarantie"));
        colValeurEstimee.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(formatAmount(cell.getValue().getValeurEstimee())));
        colValeurRetenue.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(formatAmount(cell.getValue().getValeurRetenue())));
        colAdresse.setCellValueFactory(new PropertyValueFactory<>("adresseBien"));
        colGarant.setCellValueFactory(new PropertyValueFactory<>("nomGarant"));
        colDate.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(formatDateForTable(cell.getValue().getDateEvaluation())));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Badge coloré pour statut (identique à CompteBancaireController pattern)
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().add("nx-badge");
                String l = item.toLowerCase();
                if (l.contains("accept"))                               badge.getStyleClass().add("nx-badge-success");
                else if (l.contains("attente") || l.contains("cours")) badge.getStyleClass().add("nx-badge-warning");
                else if (l.contains("refus")   || l.contains("annul")) badge.getStyleClass().add("nx-badge-error");
                setGraphic(badge);
            }
        });

        // ── Actions : Edit / Delete / View (même style que CompteBancaireController) ──
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnEdit   = new Button();
            private final Button btnDelete = new Button();
            private final Button btnView   = new Button();
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

                btnView.getStyleClass().add("nx-table-action");
                SVGPath viewIcon = new SVGPath();
                viewIcon.setContent("M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8zm11 4a4 4 0 1 0 0-8 4 4 0 0 0 0 8z");
                viewIcon.setStyle("-fx-fill: #F4C430;");
                btnView.setGraphic(viewIcon);
                btnView.setTooltip(new Tooltip("Voir détails"));

                hbox.setAlignment(Pos.CENTER);
                hbox.getChildren().addAll(btnEdit, btnDelete, btnView);

                btnEdit.setOnAction(e -> editGarantie(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> deleteGarantie(getTableView().getItems().get(getIndex())));
                btnView.setOnAction(e -> showGarantieDetails(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void initializeSearch() {
        filteredData = new FilteredList<>(garantiesList, g -> true);
        txtRecherche.textProperty().addListener((obs, oldValue, newValue) -> {
            filteredData.setPredicate(g -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String f = newValue.toLowerCase();
                return String.valueOf(g.getIdGarantie()).contains(f)
                        || String.valueOf(g.getIdCredit()).contains(f)
                        || safeLower(g.getTypeGarantie()).contains(f)
                        || safeLower(g.getNomGarant()).contains(f)
                        || safeLower(g.getStatut()).contains(f);
            });
            updateTableInfo();
        });
        SortedList<GarantieCredit> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(tableGaranties.comparatorProperty());
        tableGaranties.setItems(sorted);
    }

    private void setupTableSelection() {
        tableGaranties.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedGarantie = newSel;
                populateForm(newSel);
                isEditMode = true;
                btnAjouter.setText("Modifier");
            }
        });
    }

    private void refreshData() {
        garantiesList.setAll(garantieService.getAllGaranties());
        updateStats();
        updateTableInfo();
    }

    // ══════════════════════════════════════════════════════════════
    // ACTIONS FXML
    // ══════════════════════════════════════════════════════════════

    @FXML
    void handleAjouter(ActionEvent event) {
        if (!validateForm()) return;

        Integer idCredit      = cmbCredit.getValue();
        String  type          = cmbTypeGarantie.getValue();
        String  statut        = cmbStatut.getValue();
        LocalDate date        = dpDateEvaluation.getValue();
        String  description   = safeText(txtDescription.getText());
        String  adresse       = safeText(txtAdresseBien.getText());
        double  valEstimee    = parseDouble(txtValeurEstimee.getText(), 0);
        double  valRetenue    = parseDouble(txtValeurRetenue.getText(), valEstimee);
        String  document      = safeText(txtDocumentJustificatif.getText());
        String  garant        = safeText(txtNomGarant.getText());
        String  dateStr       = date != null ? date.format(DateTimeFormatter.ISO_DATE) : "";

        GarantieCredit g = new GarantieCredit(
                idCredit, type, description, adresse,
                valEstimee, valRetenue, document, dateStr, garant, statut
        );

        if (isEditMode && selectedGarantie != null) {
            g.setIdGarantie(selectedGarantie.getIdGarantie());
            boolean ok = garantieService.updateGarantie(g);
            if (!ok) { showWarning("Erreur", "Échec de la modification."); return; }
            showInfo("Succès", "Garantie modifiée avec succès.");
        } else {
            garantieService.addGarantie(g);
            showInfo("Succès", "Garantie ajoutée avec succès.");
        }

        clearForm();
        refreshData();
    }

    @FXML
    void handleAnnuler(ActionEvent event) {
        clearForm();
    }

    @FXML
    void handleSupprimer(ActionEvent event) {
        if (selectedGarantie == null) {
            showWarning("Avertissement", "Sélectionnez une garantie à supprimer.");
            return;
        }
        deleteGarantie(selectedGarantie);
    }

    // ══════════════════════════════════════════════════════════════
    // CRUD HELPERS
    // ══════════════════════════════════════════════════════════════

    private void editGarantie(GarantieCredit garantie) {
        selectedGarantie = garantie;
        populateForm(garantie);
        isEditMode = true;
        btnAjouter.setText("Modifier");
    }

    private void deleteGarantie(GarantieCredit garantie) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la garantie #" + garantie.getIdGarantie() + " ?");
        confirm.setContentText("Cette action est irréversible.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            boolean deleted = garantieService.deleteGarantie(garantie.getIdGarantie());
            if (deleted) {
                clearForm();
                refreshData();
                showInfo("Succès", "Garantie supprimée avec succès.");
            } else {
                showWarning("Erreur", "Échec de suppression.");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // FORM
    // ══════════════════════════════════════════════════════════════

    private void populateForm(GarantieCredit g) {
        if (!cmbCredit.getItems().contains(g.getIdCredit()))
            cmbCredit.getItems().add(g.getIdCredit());
        cmbCredit.setValue(g.getIdCredit());
        cmbTypeGarantie.setValue(g.getTypeGarantie());
        cmbStatut.setValue(g.getStatut());
        txtDescription.setText(g.getDescription());
        txtAdresseBien.setText(g.getAdresseBien());
        txtValeurEstimee.setText(String.valueOf(g.getValeurEstimee()));
        txtValeurRetenue.setText(String.valueOf(g.getValeurRetenue()));
        txtDocumentJustificatif.setText(g.getDocumentJustificatif());
        txtNomGarant.setText(g.getNomGarant());
        dpDateEvaluation.setValue(parseDate(g.getDateEvaluation()));
    }

    private void clearForm() {
        cmbCredit.setValue(null);
        cmbTypeGarantie.setValue(null);
        cmbStatut.setValue(null);
        txtDescription.clear();
        txtAdresseBien.clear();
        txtValeurEstimee.clear();
        txtValeurRetenue.clear();
        txtDocumentJustificatif.clear();
        txtNomGarant.clear();
        dpDateEvaluation.setValue(null);
        tableGaranties.getSelectionModel().clearSelection();
        selectedGarantie = null;
        isEditMode = false;
        btnAjouter.setText("Enregistrer");
    }

    private boolean validateForm() {
        if (cmbCredit.getValue() == null) {
            showWarning("Validation", "Sélectionnez un crédit.");
            return false;
        }
        if (cmbTypeGarantie.getValue() == null) {
            showWarning("Validation", "Sélectionnez un type de garantie.");
            return false;
        }
        if (dpDateEvaluation.getValue() == null) {
            showWarning("Validation", "La date d'évaluation est obligatoire.");
            return false;
        }
        if (dpDateEvaluation.getValue().isAfter(LocalDate.now())) {
            showWarning("Validation", "La date ne peut pas être dans le futur.");
            return false;
        }
        if (txtValeurEstimee.getText().isBlank()) {
            showWarning("Validation", "La valeur estimée est obligatoire.");
            return false;
        }
        try { Double.parseDouble(txtValeurEstimee.getText().trim()); }
        catch (NumberFormatException e) { showWarning("Validation", "Valeur estimée invalide."); return false; }
        if (cmbStatut.getValue() == null) {
            showWarning("Validation", "Sélectionnez un statut.");
            return false;
        }
        return true;
    }

    // ══════════════════════════════════════════════════════════════
    // TRIS
    // ══════════════════════════════════════════════════════════════

    @FXML void trierParType(ActionEvent event)    { garantiesList.sort(Comparator.comparing(g -> safeLower(g.getTypeGarantie()))); }
    @FXML void trierParValeur(ActionEvent event)  { garantiesList.sort(Comparator.comparing(GarantieCredit::getValeurRetenue).reversed()); }
    @FXML void trierParStatut(ActionEvent event)  { garantiesList.sort(Comparator.comparing(g -> safeLower(g.getStatut()))); }
    @FXML void trierParCredit(ActionEvent event)  { garantiesList.sort(Comparator.comparingInt(GarantieCredit::getIdCredit)); }

    @FXML
    void trierParDate(ActionEvent event) {
        garantiesList.sort(
            Comparator.comparing(
                (GarantieCredit g) -> parseDate(g.getDateEvaluation()),
                Comparator.nullsLast(Comparator.naturalOrder())
            ).reversed()
        );
    }

    // ══════════════════════════════════════════════════════════════
    // STATISTIQUES — popup PieChart (même style que CompteBancaireController)
    // ══════════════════════════════════════════════════════════════

    @FXML
    void ouvrirStatistiques(ActionEvent event) {
        // ── 1. Données ────────────────────────────────────────────
        Map<String, Long> counts = new LinkedHashMap<>();
        garantiesList.forEach(g ->
                counts.merge(
                        g.getTypeGarantie() != null ? g.getTypeGarantie() : "Inconnu",
                        1L, Long::sum
                )
        );
        long total = garantiesList.size();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        counts.forEach((type, cnt) -> pieData.add(new PieChart.Data(type, cnt)));

        // ── 2. PieChart ───────────────────────────────────────────
        PieChart chart = new PieChart(pieData);
        chart.setLegendVisible(false);
        chart.setLabelsVisible(false);
        chart.setStartAngle(90);
        chart.setClockwise(false);
        chart.setPrefSize(190, 190);
        chart.setMinSize(190, 190);
        chart.setMaxSize(190, 190);
        chart.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        // ── 3. Label central % ────────────────────────────────────
        Label centerLabel = new Label("");
        centerLabel.setMouseTransparent(true);
        centerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', sans-serif;");
        StackPane chartContainer = new StackPane(chart, centerLabel);
        chartContainer.setPrefSize(190, 190);
        chartContainer.setMinSize(190, 190);
        chartContainer.setMaxSize(190, 190);

        // ── 4. Légende ────────────────────────────────────────────
        VBox legend = new VBox(10);
        legend.setAlignment(Pos.CENTER_LEFT);

        // ── 5. Couleurs + hover ───────────────────────────────────
        String[] palette = {"#00B4A0", "#0A2540", "#FACC15", "#EF4444", "#9CA3AF"};
        final int[] colorIdx = {0};

        Platform.runLater(() -> {
            for (PieChart.Data d : chart.getData()) {
                String color = palette[colorIdx[0] % palette.length];
                colorIdx[0]++;

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
                    centerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;" +
                            "-fx-text-fill: " + fc + "; -fx-font-family: 'Segoe UI', sans-serif;");
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
                        "-fx-font-family: 'Segoe UI', sans-serif;");

                HBox row = new HBox(8, swatch, lbl);
                row.setAlignment(Pos.CENTER_LEFT);
                legend.getChildren().add(row);
            }
        });

        // ── 6. Titre ──────────────────────────────────────────────
        Label title    = new Label("Types de Garanties");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0A2540;" +
                "-fx-font-family: 'Segoe UI', sans-serif;");
        Label subtitle = new Label("Répartition des garanties");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280; -fx-font-family: 'Segoe UI', sans-serif;");
        Region sep = new Region();
        sep.setPrefHeight(1); sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: #E5E7EB;");
        VBox titleBox = new VBox(3, title, subtitle);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // ── 7. Bouton Fermer ──────────────────────────────────────
        Button closeBtn = new Button("Fermer");
        String btnN = "-fx-background-color:#FACC15;-fx-text-fill:#0A2540;-fx-font-size:12px;" +
                "-fx-font-weight:bold;-fx-padding:7 24;-fx-background-radius:9;-fx-cursor:hand;" +
                "-fx-font-family:'Segoe UI',sans-serif;";
        String btnH = "-fx-background-color:#EAB308;-fx-text-fill:#0A2540;-fx-font-size:12px;" +
                "-fx-font-weight:bold;-fx-padding:7 24;-fx-background-radius:9;-fx-cursor:hand;" +
                "-fx-font-family:'Segoe UI',sans-serif;";
        closeBtn.setStyle(btnN);
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(btnH));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(btnN));

        HBox btnRow = new HBox(closeBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        // ── 8. Card ───────────────────────────────────────────────
        HBox chartRow = new HBox(24, chartContainer, legend);
        chartRow.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(14, titleBox, sep, chartRow, btnRow);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(22, 26, 20, 26));
        card.setPrefWidth(400); card.setMaxWidth(400);
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian, rgba(10,37,64,0.25), 30, 0, 0, 8);"
        );

        // ── 9. Stage transparent ──────────────────────────────────
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

        // ── 10. Animation ─────────────────────────────────────────
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

    // ══════════════════════════════════════════════════════════════
    // EXPORT PDF — inline PDFBox (même style que CompteBancaireController)
    // ══════════════════════════════════════════════════════════════

    @FXML
    void exporterPDF(ActionEvent event) {
        GarantieCredit g = tableGaranties.getSelectionModel().getSelectedItem();
        if (g == null) {
            showWarning("Export PDF", "Sélectionnez une garantie dans le tableau.");
            return;
        }
        try {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle("Enregistrer le PDF");
            chooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
            chooser.setInitialFileName("Garantie_" + g.getIdGarantie() + ".pdf");
            java.io.File file = chooser.showSaveDialog(tableGaranties.getScene().getWindow());
            if (file == null) return;

            org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument();
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(
                    org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
            doc.addPage(page);

            org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                    new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);

            // Logo
            java.awt.image.BufferedImage logoImg = null;
            try (java.io.InputStream is = getClass().getResourceAsStream("/images/logo.png")) {
                if (is != null) logoImg = javax.imageio.ImageIO.read(is);
            }
            if (logoImg != null) {
                org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImage =
                        org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(doc, logoImg);
                cs.drawImage(pdImage, 40, page.getMediaBox().getHeight() - 100, 80, 80);
            }

            // Header band
            cs.setNonStrokingColor(244, 196, 48);
            cs.addRect(0, page.getMediaBox().getHeight() - 120, page.getMediaBox().getWidth(), 120);
            cs.fill();

            cs.beginText();
            cs.setNonStrokingColor(10, 37, 64);
            cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 22);
            cs.newLineAtOffset(140, page.getMediaBox().getHeight() - 62);
            cs.showText("NEXORA BANK - Garantie de Crédit");
            cs.endText();

            // Card
            float margin      = 32f;
            float contentTop  = page.getMediaBox().getHeight() - 180;
            float cardWidth   = page.getMediaBox().getWidth() - 2 * margin;
            float cardHeight  = 300f;
            cs.setNonStrokingColor(255, 255, 255);
            cs.addRect(margin, contentTop - cardHeight, cardWidth, cardHeight);
            cs.fill();
            cs.setStrokingColor(229, 231, 235);
            cs.setLineWidth(1.2f);
            cs.addRect(margin, contentTop - cardHeight, cardWidth, cardHeight);
            cs.stroke();

            cs.beginText();
            cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 14);
            cs.setNonStrokingColor(10, 37, 64);
            cs.newLineAtOffset(margin + 16, contentTop - 24);
            cs.showText("Informations de la garantie");
            cs.endText();

            cs.setStrokingColor(241, 245, 249);
            cs.setLineWidth(1f);
            cs.moveTo(margin + 16, contentTop - 32);
            cs.lineTo(margin + cardWidth - 16, contentTop - 32);
            cs.stroke();

            float labelX = margin + 20;
            float valueX = margin + 200;
            float y = contentTop - 54;

            String[][] rows = {
                    {"ID Garantie",       String.valueOf(g.getIdGarantie())},
                    {"ID Crédit",         String.valueOf(g.getIdCredit())},
                    {"Type",              safe(g.getTypeGarantie())},
                    {"Statut",            safe(g.getStatut())},
                    {"Valeur estimée",    String.format(java.util.Locale.US, "%,.2f DT", g.getValeurEstimee())},
                    {"Valeur retenue",    String.format(java.util.Locale.US, "%,.2f DT", g.getValeurRetenue())},
                    {"Adresse du bien",   safe(g.getAdresseBien())},
                    {"Nom garant",        safe(g.getNomGarant())},
                    {"Date évaluation",   safe(g.getDateEvaluation())},
                    {"Document",          safe(g.getDocumentJustificatif())}
            };

            for (String[] row : rows) {
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 11);
                cs.setNonStrokingColor(10, 37, 64);
                cs.newLineAtOffset(labelX, y);
                cs.showText(row[0]);
                cs.endText();

                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 11);
                cs.setNonStrokingColor(0, 0, 0);
                cs.newLineAtOffset(valueX, y);
                cs.showText(row[1] != null ? row[1] : "");
                cs.endText();
                y -= 22;
            }

            cs.close();
            doc.save(file);
            doc.close();
            showInfo("Export PDF", "PDF enregistré : " + file.getAbsolutePath());
        } catch (Exception e) {
            showWarning("Export PDF", "Erreur lors de l'export : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // SMS
    // ══════════════════════════════════════════════════════════════

    @FXML
    void envoyerSMS(ActionEvent event) {
        showInfo("SMS", "Fonctionnalité d'envoi SMS en cours de développement.");
    }

    // ══════════════════════════════════════════════════════════════
    // PAGINATION (scroll)
    // ══════════════════════════════════════════════════════════════

    @FXML void pageFirst(ActionEvent event) { tableGaranties.scrollTo(0); }
    @FXML void pagePrev(ActionEvent event)  { tableGaranties.scrollTo(Math.max(tableGaranties.getSelectionModel().getSelectedIndex() - 10, 0)); }
    @FXML void pageNext(ActionEvent event)  { tableGaranties.scrollTo(tableGaranties.getSelectionModel().getSelectedIndex() + 10); }
    @FXML void pageLast(ActionEvent event)  { tableGaranties.scrollTo(Integer.MAX_VALUE); }

    // ══════════════════════════════════════════════════════════════
    // DÉTAIL (bouton View jaune)
    // ══════════════════════════════════════════════════════════════

    private void showGarantieDetails(GarantieCredit g) {
        Stage ownerStage = (Stage) tableGaranties.getScene().getWindow();

        // Card détail
        String[][] infos = {
                {"ID Garantie",      String.valueOf(g.getIdGarantie())},
                {"ID Crédit",        String.valueOf(g.getIdCredit())},
                {"Type",             safe(g.getTypeGarantie())},
                {"Statut",           safe(g.getStatut())},
                {"Valeur estimée",   String.format(java.util.Locale.US, "%,.2f DT", g.getValeurEstimee())},
                {"Valeur retenue",   String.format(java.util.Locale.US, "%,.2f DT", g.getValeurRetenue())},
                {"Adresse du bien",  safe(g.getAdresseBien())},
                {"Nom garant",       safe(g.getNomGarant())},
                {"Date évaluation",  safe(g.getDateEvaluation())},
                {"Document",         safe(g.getDocumentJustificatif())}
        };

        VBox rows = new VBox(6);
        for (String[] info : infos) {
            Label key = new Label(info[0] + " :");
            key.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #0A2540; -fx-min-width: 140px;");
            Label val = new Label(info[1]);
            val.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
            HBox row = new HBox(8, key, val);
            row.setAlignment(Pos.CENTER_LEFT);
            rows.getChildren().add(row);
        }

        Label title = new Label("Détails de la Garantie #" + g.getIdGarantie());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0A2540;" +
                "-fx-font-family: 'Segoe UI', sans-serif;");
        Region sep = new Region();
        sep.setPrefHeight(1); sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: #E5E7EB;");

        Button closeBtn = new Button("Fermer");
        String btnStyle = "-fx-background-color:#FACC15;-fx-text-fill:#0A2540;-fx-font-size:12px;" +
                "-fx-font-weight:bold;-fx-padding:7 24;-fx-background-radius:9;-fx-cursor:hand;";
        closeBtn.setStyle(btnStyle);

        HBox btnRow = new HBox(closeBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(12, title, sep, rows, btnRow);
        card.setPadding(new Insets(22, 26, 20, 26));
        card.setPrefWidth(420);
        card.setStyle(
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian, rgba(10,37,64,0.25), 30, 0, 0, 8);"
        );

        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initOwner(ownerStage);
        popup.setResizable(false);

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: transparent;");
        root.setPadding(new Insets(20));
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        closeBtn.setOnAction(e -> popup.close());

        popup.setOnShown(e -> {
            popup.setX(ownerStage.getX() + ownerStage.getWidth()  / 2 - popup.getWidth()  / 2);
            popup.setY(ownerStage.getY() + ownerStage.getHeight() / 2 - popup.getHeight() / 2);
        });

        card.setScaleX(0.80); card.setScaleY(0.80); card.setOpacity(0);
        ScaleTransition st = new ScaleTransition(Duration.millis(250), card);
        st.setToX(1); st.setToY(1); st.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition ft = new FadeTransition(Duration.millis(250), card);
        ft.setToValue(1);
        popup.show();
        st.play(); ft.play();
    }

    // ══════════════════════════════════════════════════════════════
    // UTILS
    // ══════════════════════════════════════════════════════════════

    private void loadCreditIds() {
        List<Integer> creditIds = garantieService.getCreditIds();
        cmbCredit.setItems(FXCollections.observableArrayList(creditIds));
    }

    private void updateStats() {
        if (lblNombreGaranties != null)
            lblNombreGaranties.setText(String.valueOf(garantiesList.size()));

        if (lblValeurTotale != null) {
            double total = garantiesList.stream().mapToDouble(GarantieCredit::getValeurRetenue).sum();
            lblValeurTotale.setText(formatAmount(total));
        }

        if (lblGarantiesActives != null) {
            long actives = garantiesList.stream()
                    .filter(g -> safeLower(g.getStatut()).contains("accept"))
                    .count();
            lblGarantiesActives.setText(String.valueOf(actives));
        }
    }

    private void updateTableInfo() {
        if (lblTableInfo != null) {
            int total    = garantiesList.size();
            int filtered = filteredData != null ? filteredData.size() : total;
            lblTableInfo.setText(String.format("Affichage de %d sur %d garanties", filtered, total));
        }
    }

    private String formatAmount(double value) {
        return String.format(java.util.Locale.US, "%,.2f DT", value);
    }

    private String formatDateForTable(String value) {
        LocalDate date = parseDate(value);
        if (date == null) return value == null ? "" : value;
        return date.format(TABLE_DATE_FORMAT);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException e) { return null; }
    }

    private double parseDouble(String value, double defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try { return Double.parseDouble(value.trim().replace(",", ".")); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    private String safeLower(String s) { return s == null ? "" : s.toLowerCase(); }
    private String safeText(String s)  { return s == null ? "" : s.trim(); }
    private String safe(String s)      { return s == null ? "" : s.replace('\u202F', ' ').replace('\u00A0', ' '); }
    private String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(message);
        a.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(message);
        a.showAndWait();
    }
}