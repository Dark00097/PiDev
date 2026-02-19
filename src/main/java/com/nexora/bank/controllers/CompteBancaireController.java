package com.nexora.bank.controllers;

import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.CompteBancaire;
import com.nexora.bank.Service.CompteBancaireService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javafx.scene.layout.Region;

import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.animation.ScaleTransition;

import javafx.animation.Interpolator;


public class CompteBancaireController implements Initializable {

    private final CompteBancaireService service = new CompteBancaireService();

    @FXML private Button btnAjouter;
    @FXML private Button btnAnnuler;
    @FXML private Button btnSupprimer;

    @FXML private ComboBox<String> cmbStatutCompte;
    @FXML private ComboBox<String> cmbTypeCompte;

    @FXML private TableColumn<CompteBancaire, Integer> colID;
    @FXML private TableColumn<CompteBancaire, String> colNumero;
    @FXML private TableColumn<CompteBancaire, Double> colSolde;
    @FXML private TableColumn<CompteBancaire, String> colDateOuverture;
    @FXML private TableColumn<CompteBancaire, String> colStatut;
    @FXML private TableColumn<CompteBancaire, Double> colPlafondRetrait;
    @FXML private TableColumn<CompteBancaire, Double> colPlafondVirement;
    @FXML private TableColumn<CompteBancaire, String> colType;
    @FXML private TableColumn<CompteBancaire, Void> colActions;

    @FXML private DatePicker dpDateOuverture;
    @FXML private Label lblClientsActifs;
    @FXML private Label lblTableInfo;
    @FXML private Label lblTotalComptes;
    @FXML private Label lblTotalDepots;

    @FXML private TableView<CompteBancaire> tableComptes;
    @FXML private TextField txtNumeroCompte;
    @FXML private TextField txtSolde;
    @FXML private TextField txtPlafondRetrait;
    @FXML private TextField txtPlafondVirement;
    @FXML private TextField txtRecherche;

    @FXML private Label lblNumeroCompteError;
    @FXML private Label lblSoldeError;
    @FXML private Label lblDateError;
    @FXML private Label lblStatusError;
    @FXML private Label lblPlafondRetraitError;
    @FXML private Label lblPlafondVirementError;
    @FXML private Label lblStatusCompteError;

    private final ObservableList<CompteBancaire> comptesList = FXCollections.observableArrayList();
    private FilteredList<CompteBancaire> filteredData;
    private CompteBancaire selectedCompte = null;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        initializeSearch();
        setupTableSelection();
        refreshData();
    }

    private void initializeTable() {
        colActions.setPrefWidth(50);
        colActions.setMinWidth(50);

        colID.setCellValueFactory(new PropertyValueFactory<>("idCompte"));
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroCompte"));
        colSolde.setCellValueFactory(new PropertyValueFactory<>("solde"));
        colDateOuverture.setCellValueFactory(new PropertyValueFactory<>("dateOuverture"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statutCompte"));
        colPlafondRetrait.setCellValueFactory(new PropertyValueFactory<>("plafondRetrait"));
        colPlafondVirement.setCellValueFactory(new PropertyValueFactory<>("plafondVirement"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeCompte"));

        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final Button btnView = new Button();
            private final HBox hbox = new HBox(8);

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

                btnView.getStyleClass().addAll("nx-table-action");
                SVGPath viewIcon = new SVGPath();
                viewIcon.setContent("M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8zm11 4a4 4 0 1 0 0-8 4 4 0 0 0 0 8z");
                viewIcon.setStyle("-fx-fill: #F4C430;");
                btnView.setGraphic(viewIcon);
                btnView.setTooltip(new Tooltip("Voir"));

                hbox.setAlignment(Pos.CENTER);
                hbox.getChildren().addAll(btnEdit, btnDelete, btnView);

                btnEdit.setOnAction(event -> {
                    CompteBancaire compte = getTableView().getItems().get(getIndex());
                    editCompte(compte);
                });
                btnDelete.setOnAction(event -> {
                    CompteBancaire compte = getTableView().getItems().get(getIndex());
                    deleteCompte(compte);
                });
                btnView.setOnAction(event -> {
                    CompteBancaire compte = getTableView().getItems().get(getIndex());
                    showCompteCoffres(compte);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void initializeSearch() {
        filteredData = new FilteredList<>(comptesList, p -> true);
        txtRecherche.textProperty().addListener((obs, oldValue, newValue) -> {
            filteredData.setPredicate(compte -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String f = newValue.toLowerCase();
                return String.valueOf(compte.getIdCompte()).contains(f)
                        || (compte.getNumeroCompte() != null && compte.getNumeroCompte().toLowerCase().contains(f))
                        || (compte.getStatutCompte() != null && compte.getStatutCompte().toLowerCase().contains(f))
                        || (compte.getTypeCompte() != null && compte.getTypeCompte().toLowerCase().contains(f));
            });
            updateTableInfo();
        });
        SortedList<CompteBancaire> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(tableComptes.comparatorProperty());
        tableComptes.setItems(sorted);
    }

    private void setupTableSelection() {
        tableComptes.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedCompte = newSel;
                populateForm(newSel);
                isEditMode = true;
                btnAjouter.setText("Modifier");
            }
        });
    }

    private void refreshData() {
        // BackOffice admin : charge TOUS les comptes (pas de filtre idUser)
        comptesList.setAll(service.getAll());
        updateStats();
        updateTableInfo();
    }

    @FXML
    void handleAjouter(ActionEvent event) {
        if (!validateForm()) return;
        String numero = txtNumeroCompte.getText().trim();
        double solde = parseDouble(txtSolde.getText().trim());
        double plafRetrait = parseDouble(txtPlafondRetrait.getText().trim());
        double plafVirement = parseDouble(txtPlafondVirement.getText().trim());
        LocalDate date = dpDateOuverture.getValue();
        String dateStr = date != null ? date.format(DateTimeFormatter.ISO_DATE) : "";
        String statut = cmbStatutCompte.getValue();
        String type = cmbTypeCompte.getValue();

        if (isEditMode && selectedCompte != null) {
            // ★ CORRECTION : on passe selectedCompte.getIdUser() pour préserver l'idUser existant.
            // Avant : new CompteBancaire(idCompte, numero, solde, dateStr, statut, plafRetrait, plafVirement, type)
            //         → ERREUR : constructeur attendait 9 arguments, reçus 8.
            CompteBancaire updated = new CompteBancaire(
                    selectedCompte.getIdCompte(),
                    numero, solde, dateStr, statut,
                    plafRetrait, plafVirement, type,
                    selectedCompte.getIdUser()  // ★ idUser préservé depuis l'objet sélectionné
            );
            service.edit(updated);
            showInfo("Succès", "Compte modifié avec succès");
        } else {
            // ★ AuthSession : idUser du user connecté est injecté automatiquement.
            //   Si aucun user n'est connecté (admin BackOffice pur), idUser = 0 → NULL en BD.
            int idUserConnecte = 0;
            if (AuthSession.getCurrentUser() != null) {
                idUserConnecte = AuthSession.getCurrentUser().getIdUser();
            }
            CompteBancaire nouveau = new CompteBancaire(
                    numero, solde, dateStr, statut,
                    plafRetrait, plafVirement, type,
                    idUserConnecte  // ★ idUser rempli automatiquement depuis AuthSession
            );
            service.add(nouveau);
            showInfo("Succès", "Compte ajouté avec succès");
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
        if (selectedCompte == null) {
            showWarning("Avertissement", "Sélectionnez un compte à supprimer.");
            return;
        }
        deleteCompte(selectedCompte);
    }

    private void editCompte(CompteBancaire compte) {
        selectedCompte = compte;
        populateForm(compte);
        isEditMode = true;
        btnAjouter.setText("Modifier");
    }

    private void deleteCompte(CompteBancaire compte) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le compte " + compte.getNumeroCompte() + " ?");
        confirm.setContentText("Cette action est irréversible.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            service.remove(compte);
            clearForm();
            refreshData();
            showInfo("Succès", "Compte supprimé avec succès");
        }
    }

    private void populateForm(CompteBancaire c) {
        txtNumeroCompte.setText(c.getNumeroCompte());
        txtSolde.setText(String.valueOf(c.getSolde()));
        txtPlafondRetrait.setText(String.valueOf(c.getPlafondRetrait()));
        txtPlafondVirement.setText(String.valueOf(c.getPlafondVirement()));
        cmbStatutCompte.setValue(c.getStatutCompte());
        cmbTypeCompte.setValue(c.getTypeCompte());
        try {
            dpDateOuverture.setValue(c.getDateOuverture() != null && !c.getDateOuverture().isEmpty()
                    ? LocalDate.parse(c.getDateOuverture())
                    : null);
        } catch (Exception e) {
            dpDateOuverture.setValue(null);
        }
    }

    private void clearForm() {
        txtNumeroCompte.clear();
        txtSolde.clear();
        txtPlafondRetrait.clear();
        txtPlafondVirement.clear();
        cmbStatutCompte.setValue(null);
        cmbTypeCompte.setValue(null);
        dpDateOuverture.setValue(null);
        tableComptes.getSelectionModel().clearSelection();
        selectedCompte = null;
        isEditMode = false;
        btnAjouter.setText("Enregistrer");
    }

    private boolean validateForm() {
        boolean valid = true;

        lblNumeroCompteError.setText("");
        lblSoldeError.setText("");
        lblDateError.setText("");
        lblStatusError.setText("");
        lblPlafondRetraitError.setText("");
        lblPlafondVirementError.setText("");
        lblStatusCompteError.setText("");

        String numero = txtNumeroCompte.getText().trim();
        if (numero.isEmpty()) {
            lblNumeroCompteError.setText("Le numéro de compte est obligatoire.");
            valid = false;
        } else if (!numero.matches("^CB-\\d{3,}$")) {
            lblNumeroCompteError.setText("Format invalide. Exemple: CB-001");
            valid = false;
        }

        try {
            double solde = Double.parseDouble(txtSolde.getText().trim());
            if (solde < 0) {
                lblSoldeError.setText("Le solde ne peut pas être négatif.");
                valid = false;
            }
        } catch (NumberFormatException e) {
            lblSoldeError.setText("Solde invalide.");
            valid = false;
        }

        LocalDate dateOuverture = dpDateOuverture.getValue();
        if (dateOuverture == null) {
            lblDateError.setText("Date obligatoire.");
            valid = false;
        } else if (dateOuverture.isAfter(LocalDate.now())) {
            lblDateError.setText("La date ne peut pas être dans le futur.");
            valid = false;
        }

        if (cmbStatutCompte.getValue() == null) {
            lblStatusError.setText("Veuillez sélectionner un statut.");
            valid = false;
        }

        try {
            double plafondRetrait = Double.parseDouble(txtPlafondRetrait.getText().trim());
            if (plafondRetrait < 0) {
                lblPlafondRetraitError.setText("Le plafond doit être ≥ 0.");
                valid = false;
            }
        } catch (NumberFormatException e) {
            lblPlafondRetraitError.setText("Montant invalide.");
            valid = false;
        }

        try {
            double plafondVirement = Double.parseDouble(txtPlafondVirement.getText().trim());
            if (plafondVirement < 0) {
                lblPlafondVirementError.setText("Le plafond doit être ≥ 0.");
                valid = false;
            }
        } catch (NumberFormatException e) {
            lblPlafondVirementError.setText("Montant invalide.");
            valid = false;
        }

        if (cmbTypeCompte.getValue() == null) {
            lblStatusCompteError.setText("Veuillez sélectionner un type de compte.");
            valid = false;
        }

        return valid;
    }


    private void updateStats() {
        if (lblTotalComptes != null)
            lblTotalComptes.setText(String.valueOf(comptesList.size()));

        if (lblClientsActifs != null) {
            long actifs = comptesList.stream()
                    .filter(c -> "Actif".equalsIgnoreCase(c.getStatutCompte())
                            || "Active".equalsIgnoreCase(c.getStatutCompte()))
                    .count();
            lblClientsActifs.setText(String.valueOf(actifs));
        }

        if (lblTotalDepots != null) {
            double total = comptesList.stream().mapToDouble(CompteBancaire::getSolde).sum();
            lblTotalDepots.setText(String.format(java.util.Locale.US, "%,.2f DT", total));
        }
    }

    private void updateTableInfo() {
        if (lblTableInfo != null) {
            int total = comptesList.size();
            int filtered = filteredData != null ? filteredData.size() : total;
            lblTableInfo.setText(String.format("Affichage de %d sur %d comptes", filtered, total));
        }
    }

    private double parseDouble(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
    }


    @FXML
    void trierParId(ActionEvent event) { comptesList.sort(Comparator.comparingInt(CompteBancaire::getIdCompte)); }
    @FXML
    void trierParNumero(ActionEvent event) { comptesList.sort(Comparator.comparing(CompteBancaire::getNumeroCompte)); }
    @FXML
    void trierParSoldeAsc(ActionEvent event) { comptesList.sort(Comparator.comparing(CompteBancaire::getSolde)); }
    @FXML
    void trierParSoldeDesc(ActionEvent event) { comptesList.sort(Comparator.comparing(CompteBancaire::getSolde).reversed()); }
    @FXML
    void trierParDate(ActionEvent event) { comptesList.sort(Comparator.comparing(CompteBancaire::getDateOuverture)); }
    @FXML
    void trierParStatut(ActionEvent event) { comptesList.sort(Comparator.comparing(CompteBancaire::getStatutCompte)); }

    @FXML
    void exporterPDF(ActionEvent event) {
        CompteBancaire compte = tableComptes.getSelectionModel().getSelectedItem();
        if (compte == null) {
            showWarning("Export PDF", "Sélectionnez un compte dans le tableau.");
            return;
        }
        try {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle("Enregistrer le PDF");
            chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
            chooser.setInitialFileName("Compte_" + compte.getIdCompte() + ".pdf");
            java.io.File file = chooser.showSaveDialog(tableComptes.getScene().getWindow());
            if (file == null) return;

            org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument();
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
            doc.addPage(page);

            org.apache.pdfbox.pdmodel.PDPageContentStream cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);

            java.awt.image.BufferedImage logoImg = null;
            try (java.io.InputStream is = getClass().getResourceAsStream("/images/logo.png")) {
                if (is != null) logoImg = javax.imageio.ImageIO.read(is);
            }
            if (logoImg != null) {
                org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImage =
                        org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(doc, logoImg);
                float imgW = 80, imgH = 80;
                cs.drawImage(pdImage, 40, page.getMediaBox().getHeight() - 100, imgW, imgH);
            }

            cs.setNonStrokingColor(244, 196, 48);
            cs.addRect(0, page.getMediaBox().getHeight() - 120, page.getMediaBox().getWidth(), 120);
            cs.fill();

            cs.beginText();
            cs.setNonStrokingColor(10, 37, 64);
            cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 24);
            cs.newLineAtOffset(140, page.getMediaBox().getHeight() - 60);
            cs.showText("NEXORA BANK - Compte Bancaire");
            cs.endText();

            float margin = 32f;
            float contentTop = page.getMediaBox().getHeight() - 180;
            float cardWidth = page.getMediaBox().getWidth() - 2 * margin;
            float cardHeight = 280f;
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
            cs.showText("Informations du compte");
            cs.endText();

            cs.setStrokingColor(241, 245, 249);
            cs.setLineWidth(1f);
            cs.moveTo(margin + 16, contentTop - 32);
            cs.lineTo(margin + cardWidth - 16, contentTop - 32);
            cs.stroke();

            float labelX = margin + 20;
            float valueX = margin + 220;
            float y = contentTop - 54;

            String[][] rows = new String[][]{
                    {"ID", String.valueOf(compte.getIdCompte())},
                    {"Numero", safe(compte.getNumeroCompte())},
                    {"Solde", String.format(java.util.Locale.US, "%,.2f DT", compte.getSolde())},
                    {"Date ouverture", safe(compte.getDateOuverture())},
                    {"Statut", safe(compte.getStatutCompte())},
                    {"Plafond Retrait", String.format(java.util.Locale.US, "%,.2f DT", compte.getPlafondRetrait())},
                    {"Plafond Virement", String.format(java.util.Locale.US, "%,.2f DT", compte.getPlafondVirement())},
                    {"Type", safe(compte.getTypeCompte())}
            };

            for (String[] row : rows) {
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 12);
                cs.setNonStrokingColor(10, 37, 64);
                cs.newLineAtOffset(labelX, y);
                cs.showText(row[0]);
                cs.endText();

                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
                cs.setNonStrokingColor(0, 0, 0);
                cs.newLineAtOffset(valueX, y);
                cs.showText(row[1] != null ? row[1] : "");
                cs.endText();

                y -= 22;
            }

            cs.setStrokingColor(230, 231, 235);
            cs.setLineWidth(1);
            cs.moveTo(margin + 16, contentTop - cardHeight + 16);
            cs.lineTo(margin + cardWidth - 16, contentTop - cardHeight + 16);
            cs.stroke();

            cs.close();
            doc.save(file);
            doc.close();
            showInfo("Export PDF", "PDF enregistré: " + file.getAbsolutePath());
        } catch (Exception e) {
            showWarning("Export PDF", "Erreur lors de l'export: " + e.getMessage());
        }
    }

    @FXML
    void envoyerSMS(ActionEvent event) {
        showInfo("SMS", "Fonctionnalité d'envoi SMS en cours de développement.");
    }

    //statistique
    @FXML
    void ouvrirStatistiques(ActionEvent event) {

        // ── 1. DONNÉES ────────────────────────────────────────────────
        Map<String, Long> counts = new HashMap<>();
        comptesList.forEach(c ->
                counts.merge(
                        c.getTypeCompte() != null ? c.getTypeCompte() : "Inconnu",
                        1L, Long::sum
                )
        );
        long total = comptesList.size();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        counts.forEach((type, cnt) -> pieData.add(new PieChart.Data(type, cnt)));

        // ── 2. PIE CHART ──────────────────────────────────────────────
        PieChart chart = new PieChart(pieData);
        chart.setLegendVisible(false);
        chart.setLabelsVisible(false);
        chart.setStartAngle(90);
        chart.setClockwise(false);
        chart.setPrefSize(190, 190);
        chart.setMinSize(190, 190);
        chart.setMaxSize(190, 190);
        chart.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        // ── 3. LABEL CENTRAL % ────────────────────────────────────────
        Label centerLabel = new Label("");
        centerLabel.setMouseTransparent(true);
        centerLabel.setStyle(
                "-fx-font-size: 22px; -fx-font-weight: bold;" +
                        "-fx-font-family: 'Segoe UI', sans-serif;"
        );
        StackPane chartContainer = new StackPane(chart, centerLabel);
        chartContainer.setPrefSize(190, 190);
        chartContainer.setMinSize(190, 190);
        chartContainer.setMaxSize(190, 190);

        // ── 4. LÉGENDE ────────────────────────────────────────────────
        VBox legend = new VBox(10);
        legend.setAlignment(Pos.CENTER_LEFT);

        // ── 5. COULEURS + HOVER ───────────────────────────────────────
        Platform.runLater(() -> {
            for (PieChart.Data d : chart.getData()) {
                String name = d.getName().toLowerCase();
                String color;
                if (name.contains("epargne")) {
                    color = "#00B4A0";
                } else if (name.contains("courant")) {
                    color = "#0A2540";
                } else if (name.contains("professionnel")) {
                    color = "#FACC15";
                } else {
                    color = "#9CA3AF";
                }

                d.getNode().setStyle(
                        "-fx-pie-color: " + color + "; -fx-border-color: white; -fx-border-width: 2;"
                );

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

        // ── 6. TITRE ──────────────────────────────────────────────────
        Label title = new Label("Types de Comptes");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0A2540;" +
                "-fx-font-family: 'Segoe UI', sans-serif;");

        Label subtitle = new Label("Répartition des comptes");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;" +
                "-fx-font-family: 'Segoe UI', sans-serif;");

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: #E5E7EB;");

        VBox titleBox = new VBox(3, title, subtitle);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // ── 7. BOUTON FERMER ──────────────────────────────────────────
        Button closeBtn = new Button("Fermer");
        String btnN = "-fx-background-color:#FACC15;-fx-text-fill:#0A2540;-fx-font-size:12px;" +
                "-fx-font-weight:bold;-fx-padding:7 24;-fx-background-radius:9;-fx-cursor:hand;" +
                "-fx-font-family:'Segoe UI',sans-serif;";
        String btnH = "-fx-background-color:#EAB308;-fx-text-fill:#0A2540;-fx-font-size:12px;" +
                "-fx-font-weight:bold;-fx-padding:7 24;-fx-background-radius:9;-fx-cursor:hand;" +
                "-fx-font-family:'Segoe UI',sans-serif;";
        closeBtn.setStyle(btnN);
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(btnH));
        closeBtn.setOnMouseExited (e -> closeBtn.setStyle(btnN));

        HBox btnRow = new HBox(closeBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        // ── 8. CARD ───────────────────────────────────────────────────
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

        // ── 9. STAGE TRANSPARENT TAILLE FIXE = CARD ───────────────────
        // La scène est exactement la taille de la card (400 × ~330)
        // Pas d'overlay, pas de fond — juste la card flottante

        Stage ownerStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Stage popupStage = new Stage();
        popupStage.initStyle(StageStyle.TRANSPARENT);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initOwner(ownerStage);
        popupStage.setResizable(false);

        // Scène de taille fixe = card uniquement, fond transparent
        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: transparent;");
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popupStage.setScene(scene);

        closeBtn.setOnAction(e -> popupStage.close());

        // Centrer sur le stage parent
        popupStage.setOnShown(e -> {
            double cx = ownerStage.getX() + ownerStage.getWidth()  / 2;
            double cy = ownerStage.getY() + ownerStage.getHeight() / 2;
            popupStage.setX(cx - popupStage.getWidth()  / 2);
            popupStage.setY(cy - popupStage.getHeight() / 2);
        });

        // ── 10. ANIMATION ─────────────────────────────────────────────
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

    @FXML
    void showChart(ActionEvent event) {
        // Données pour le graphique PieChart
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        Map<String, Long> countByType = new HashMap<>();
        for (CompteBancaire c : comptesList) {
            String type = c.getTypeCompte() != null ? c.getTypeCompte() : "Autre";
            countByType.merge(type, 1L, Long::sum);
        }
        double total = comptesList.size();
        countByType.forEach((type, count) ->
                pieData.add(new PieChart.Data(type, count)));

        PieChart chart = new PieChart(pieData);
        chart.setTitle("Répartition par type");
        chart.setPrefSize(320, 260);
        chart.setLabelsVisible(false);
        chart.setLegendVisible(false);

        Label centerLabel = new Label();
        centerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        StackPane chartContainer = new StackPane(chart, centerLabel);
        chartContainer.setPrefSize(320, 260);

        VBox legend = new VBox(8);
        legend.setAlignment(Pos.CENTER_LEFT);

        Platform.runLater(() -> {
            String[] palette = {"#00B4A0", "#0A2540", "#FACC15", "#9CA3AF", "#EF4444"};
            int idx = 0;
            for (PieChart.Data d : chart.getData()) {
                String color;
                String name = d.getName().toLowerCase();
                if (name.contains("courant")) color = "#00B4A0";
                else if (name.contains("epargne") || name.contains("épargne")) color = "#0A2540";
                else if (name.contains("professionnel")) color = "#FACC15";
                else color = "#9CA3AF";

                d.getNode().setStyle(
                        "-fx-pie-color: " + color + "; -fx-border-color: white; -fx-border-width: 2;"
                );

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

        Label title = new Label("Types de Comptes");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0A2540;" +
                "-fx-font-family: 'Segoe UI', sans-serif;");

        Label subtitle = new Label("Répartition des comptes");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;" +
                "-fx-font-family: 'Segoe UI', sans-serif;");

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color: #E5E7EB;");

        VBox titleBox = new VBox(3, title, subtitle);
        titleBox.setAlignment(Pos.CENTER_LEFT);

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
            double cx = ownerStage.getX() + ownerStage.getWidth() / 2;
            double cy = ownerStage.getY() + ownerStage.getHeight() / 2;
            popupStage.setX(cx - popupStage.getWidth() / 2);
            popupStage.setY(cy - popupStage.getHeight() / 2);
        });

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

    private String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @FXML void pageFirst(ActionEvent event) { tableComptes.scrollTo(0); }
    @FXML void pagePrev(ActionEvent event)  { tableComptes.scrollTo(Math.max(tableComptes.getSelectionModel().getSelectedIndex() - 10, 0)); }
    @FXML void pageNext(ActionEvent event)  { tableComptes.scrollTo(tableComptes.getSelectionModel().getSelectedIndex() + 10); }
    @FXML void pageLast(ActionEvent event)  { tableComptes.scrollTo(Integer.MAX_VALUE); }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace('\u202F', ' ').replace('\u00A0', ' ');
    }

    private void showCompteCoffres(CompteBancaire compte) {
        Stage ownerStage = (Stage) tableComptes.getScene().getWindow();
        CompteCoffresDialog dialog = new CompteCoffresDialog(compte);
        dialog.show(ownerStage);
    }
}