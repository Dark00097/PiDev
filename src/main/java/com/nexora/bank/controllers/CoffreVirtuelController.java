package com.nexora.bank.controllers;

import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.CoffreVirtuel;
import com.nexora.bank.Models.CompteBancaire;  // ✅ NOUVEAU
import com.nexora.bank.Service.CoffreVirtuelService;
import com.nexora.bank.Service.CompteBancaireService;  // ✅ NOUVEAU

import javafx.beans.property.SimpleStringProperty;  // ✅ NOUVEAU
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class CoffreVirtuelController implements Initializable {

    @FXML private Button btnAjouter;
    @FXML private Button btnAnnuler;
    @FXML private Button btnSupprimer;

    @FXML private CheckBox chkEstVerrouille;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private ComboBox<CompteBancaire> cmbCompteBancaire;  // ✅ NOUVEAU

    @FXML private TableView<CoffreVirtuel> tableCoffres;
    @FXML private TableColumn<CoffreVirtuel, Void> colActions;
    @FXML private TableColumn<CoffreVirtuel, String> colDateCreation;
    @FXML private TableColumn<CoffreVirtuel, String> colDateObjectif;
    @FXML private TableColumn<CoffreVirtuel, Integer> colID;
    @FXML private TableColumn<CoffreVirtuel, Double> colMontantActuel;
    @FXML private TableColumn<CoffreVirtuel, String> colNom;
    @FXML private TableColumn<CoffreVirtuel, String> colCompte;  // ✅ NOUVEAU
    @FXML private TableColumn<CoffreVirtuel, Double> colObjectif;
    @FXML private TableColumn<CoffreVirtuel, String> colStatus;
    @FXML private TableColumn<CoffreVirtuel, Boolean> colVerrouille;
    @FXML private TableColumn<CoffreVirtuel, Void> colProgression;

    @FXML private DatePicker dpDateCreation;
    @FXML private DatePicker dpDateObjectif;
    @FXML private Label lblCoffresActifs;
    @FXML private Label lblMontantTotal;
    @FXML private Label lblTableInfo;
    @FXML private Label lblTotalCoffres;

    @FXML private TextField txtMontantActuel;
    @FXML private TextField txtNom;
    @FXML private TextField txtObjectifMontant;
    @FXML private TextField txtRecherche;

    @FXML private Label lblNomError;
    @FXML private Label lblObjectifError;
    @FXML private Label lblMontantError;
    @FXML private Label lblDateCreationError;
    @FXML private Label lblDateObjectifError;
    @FXML private Label lblCompteError;  // ✅ NOUVEAU

    private final ObservableList<CoffreVirtuel> coffresList = FXCollections.observableArrayList();
    private FilteredList<CoffreVirtuel> filteredData;
    private CoffreVirtuel selectedCoffre = null;
    private boolean isEditMode = false;

    private final CoffreVirtuelService service = new CoffreVirtuelService();
    private CompteBancaireService compteService;  // ✅ NOUVEAU

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        compteService = new CompteBancaireService();  // ✅ NOUVEAU

        initializeComboBoxCompte();  // ✅ NOUVEAU
        initializeTable();
        initializeSearch();
        setupTableSelection();
        refreshData();
    }

    // ✅ NOUVELLE MÉTHODE
    private void initializeComboBoxCompte() {
        // Charger tous les comptes bancaires
        ObservableList<CompteBancaire> comptes =
                FXCollections.observableArrayList(compteService.getAll());

        cmbCompteBancaire.setItems(comptes);

        // Afficher le numéro de compte dans la ComboBox
        cmbCompteBancaire.setCellFactory(param -> new ListCell<CompteBancaire>() {
            @Override
            protected void updateItem(CompteBancaire compte, boolean empty) {
                super.updateItem(compte, empty);
                if (empty || compte == null) {
                    setText(null);
                } else {
                    setText(compte.getNumeroCompte() + " - " + compte.getTypeCompte());
                }
            }
        });

        cmbCompteBancaire.setButtonCell(new ListCell<CompteBancaire>() {
            @Override
            protected void updateItem(CompteBancaire compte, boolean empty) {
                super.updateItem(compte, empty);
                if (empty || compte == null) {
                    setText(null);
                } else {
                    setText(compte.getNumeroCompte() + " - " + compte.getTypeCompte());
                }
            }
        });
    }

    private void initializeTable() {

        // ========== AJOUTEZ CES 2 LIGNES ICI ==========
        colActions.setPrefWidth(60);
        colActions.setMinWidth(60);


        colID.setCellValueFactory(new PropertyValueFactory<>("idCoffre"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colObjectif.setCellValueFactory(new PropertyValueFactory<>("objectifMontant"));
        colMontantActuel.setCellValueFactory(new PropertyValueFactory<>("montantActuel"));
        colDateCreation.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        colDateObjectif.setCellValueFactory(new PropertyValueFactory<>("dateObjectifs"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colVerrouille.setCellValueFactory(new PropertyValueFactory<>("estVerrouille"));

        // ✅ NOUVELLE COLONNE : Afficher le numéro de compte
        colCompte.setCellValueFactory(cellData -> {
            int idCompte = cellData.getValue().getIdCompte();

            if (idCompte == 0) {
                return new SimpleStringProperty("N/A");
            }

            // Chercher le compte correspondant
            CompteBancaire compte = compteService.getAll().stream()
                    .filter(c -> c.getIdCompte() == idCompte)
                    .findFirst()
                    .orElse(null);

            String numeroCompte = compte != null ? compte.getNumeroCompte() : "N/A";
            return new SimpleStringProperty(numeroCompte);
        });

        // -------------------- Colonne Progression avec barre colorée --------------------
        colProgression.setCellFactory(column -> new TableCell<CoffreVirtuel, Void>() {
            private final ProgressBar progressBar = new ProgressBar();

            {
                progressBar.setPrefWidth(100);
                progressBar.setPrefHeight(18);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    CoffreVirtuel coffre = getTableRow().getItem();
                    double objectif = coffre.getObjectifMontant();
                    double actuel = coffre.getMontantActuel();
                    double progression = objectif > 0 ? (actuel / objectif) : 0;

                    progressBar.setProgress(Math.min(progression, 1.0));

                    // Couleurs selon progression
                    String color;
                    if (progression >= 1.0) {
                        color = "#4CAF50"; // Vert - Objectif atteint (100%)
                    } else if (progression >= 0.5) {
                        color = "#FF9800"; // Orange - En bonne voie (50-99%)
                    } else {
                        color = "#F44336"; // Rouge - Début (0-49%)
                    }

                    progressBar.setStyle(
                            "-fx-accent: " + color + ";" +
                                    "-fx-control-inner-background: #E8E8E8;" +
                                    "-fx-background-radius: 4px;" +
                                    "-fx-background-insets: 0;"
                    );

                    setGraphic(progressBar);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // -------------------- Colonne Verrouillé avec icône --------------------
        colVerrouille.setCellFactory(column -> new TableCell<CoffreVirtuel, Boolean>() {
            private final Label iconLabel = new Label();

            @Override
            protected void updateItem(Boolean estVerrouille, boolean empty) {
                super.updateItem(estVerrouille, empty);
                if (empty || estVerrouille == null) {
                    setGraphic(null);
                } else {
                    if (estVerrouille) {
                        iconLabel.setText("🔒");
                        iconLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: red;");
                    } else {
                        iconLabel.setText("🗝");
                        iconLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: green;");
                    }
                    setGraphic(iconLabel);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colActions.setCellFactory(column -> new TableCell<CoffreVirtuel, Void>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final Button btnView = new Button();
            private final HBox hbox = new HBox(8);

            {
                // Modifier
                btnEdit.getStyleClass().addAll("nx-table-action", "nx-table-action-edit");
                SVGPath editIcon = new SVGPath();
                editIcon.setContent("M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z");
                editIcon.setStyle("-fx-fill: #00B4A0;");
                btnEdit.setGraphic(editIcon);
                btnEdit.setTooltip(new Tooltip("Modifier"));

                // Supprimer
                btnDelete.getStyleClass().addAll("nx-table-action", "nx-table-action-delete");
                SVGPath deleteIcon = new SVGPath();
                deleteIcon.setContent("M19 7l-.867 12.142A2 2 0 0 1 16.138 21H7.862a2 2 0 0 1-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v3M4 7h16");
                deleteIcon.setStyle("-fx-fill: #EF4444;");
                btnDelete.setGraphic(deleteIcon);
                btnDelete.setTooltip(new Tooltip("Supprimer"));

                // Voir
                btnView.getStyleClass().addAll("nx-table-action");
                SVGPath viewIcon = new SVGPath();
                viewIcon.setContent("M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8zm11 4a4 4 0 1 0 0-8 4 4 0 0 0 0 8z");
                viewIcon.setStyle("-fx-fill: #F4C430;");
                btnView.setGraphic(viewIcon);
                btnView.setTooltip(new Tooltip("Voir"));

                hbox.setAlignment(Pos.CENTER);
                hbox.getChildren().addAll(btnEdit, btnDelete, btnView);

                btnEdit.setOnAction(event -> {
                    CoffreVirtuel coffre = getTableView().getItems().get(getIndex());
                    editCoffre(coffre);
                });
                btnDelete.setOnAction(event -> {
                    CoffreVirtuel coffre = getTableView().getItems().get(getIndex());
                    deleteCoffre(coffre);
                });
                btnView.setOnAction(event -> {
                    CoffreVirtuel coffre = getTableView().getItems().get(getIndex());
                    showDetails(coffre);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    // -------------------- Recherche --------------------
    private void initializeSearch() {
        filteredData = new FilteredList<>(coffresList, p -> true);
        txtRecherche.textProperty().addListener((obs, oldValue, newValue) -> {
            filteredData.setPredicate(coffre -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String f = newValue.toLowerCase();
                return String.valueOf(coffre.getIdCoffre()).contains(f)
                        || (coffre.getNom() != null && coffre.getNom().toLowerCase().contains(f))
                        || String.valueOf(coffre.getObjectifMontant()).contains(f)
                        || String.valueOf(coffre.getMontantActuel()).contains(f)
                        || (coffre.getStatus() != null && coffre.getStatus().toLowerCase().contains(f))
                        || (coffre.getDateCreation() != null && coffre.getDateCreation().toLowerCase().contains(f))
                        || (coffre.getDateObjectifs() != null && coffre.getDateObjectifs().toLowerCase().contains(f));
            });
            updateTableInfo();
        });

        SortedList<CoffreVirtuel> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(tableCoffres.comparatorProperty());
        tableCoffres.setItems(sorted);
    }

    // -------------------- Selection --------------------
    private void setupTableSelection() {
        tableCoffres.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedCoffre = newSel;
                populateForm(newSel);
                isEditMode = true;
                btnAjouter.setText("Modifier");
            }
        });
    }

    // -------------------- CRUD --------------------
    @FXML
    void handleAjouter(ActionEvent event) {
        if (!validateForm()) return;

        String nom = txtNom.getText().trim();
        double objectif = parseDouble(txtObjectifMontant.getText().trim());
        double montant = parseDouble(txtMontantActuel.getText().trim());
        String dateCreation = dpDateCreation.getValue() != null
                ? dpDateCreation.getValue().format(DateTimeFormatter.ISO_DATE) : "";
        String dateObjectifs = dpDateObjectif.getValue() != null
                ? dpDateObjectif.getValue().format(DateTimeFormatter.ISO_DATE) : "";
        String status = cmbStatus.getValue();
        boolean estVerrouille = chkEstVerrouille.isSelected();

        // ✅ RÉCUPÉRER L'ID DU COMPTE SÉLECTIONNÉ
        CompteBancaire compteSelectionne = cmbCompteBancaire.getValue();
        int idCompte = compteSelectionne.getIdCompte();

        if (isEditMode && selectedCoffre != null) {
            selectedCoffre.setNom(nom);
            selectedCoffre.setObjectifMontant(objectif);
            selectedCoffre.setMontantActuel(montant);
            selectedCoffre.setDateCreation(dateCreation);
            selectedCoffre.setDateObjectifs(dateObjectifs);
            selectedCoffre.setStatus(status);
            selectedCoffre.setEstVerrouille(estVerrouille);
            selectedCoffre.setIdCompte(idCompte);  // ✅ NOUVEAU
            // ★ On préserve l'idUser existant (on ne le réécrase pas à chaque édition)

            service.edit(selectedCoffre);
            tableCoffres.refresh();
            showInfo("Succès", "Coffre virtuel modifié avec succès");

        } else {
            // ★ Récupération automatique du user connecté via AuthSession
            int idUserConnecte = 0;
            if (AuthSession.getCurrentUser() != null) {
                idUserConnecte = AuthSession.getCurrentUser().getIdUser();
            }
            CoffreVirtuel nouveau = new CoffreVirtuel(
                    nom, objectif, montant, dateCreation, dateObjectifs,
                    status, estVerrouille, idCompte, idUserConnecte  // ★ idUser injecté automatiquement
            );
            service.add(nouveau);
            showInfo("Succès", "Coffre virtuel ajouté avec succès");
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
        if (selectedCoffre == null) {
            showWarning("Avertissement", "Sélectionnez un coffre à supprimer.");
            return;
        }
        deleteCoffre(selectedCoffre);
    }

    private void editCoffre(CoffreVirtuel coffre) {
        selectedCoffre = coffre;
        populateForm(coffre);
        isEditMode = true;
        btnAjouter.setText("Modifier");
    }

    private void deleteCoffre(CoffreVirtuel coffre) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le coffre \"" + coffre.getNom() + "\" ?");
        confirm.setContentText("Cette action est irréversible.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            service.remove(coffre);
            clearForm();
            refreshData();
            showInfo("Succès", "Coffre virtuel supprimé avec succès");
        }
    }

    private void showDetails(CoffreVirtuel coffre) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails du Coffre");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(8);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().addAll(
                new Label("ID: " + coffre.getIdCoffre()),
                new Label("Nom: " + coffre.getNom()),
                new Label(String.format("Objectif: %,.2f DT", coffre.getObjectifMontant())),
                new Label(String.format("Montant Actuel: %,.2f DT", coffre.getMontantActuel())),
                new Label("Date Création: " + coffre.getDateCreation()),
                new Label("Date Objectifs: " + coffre.getDateObjectifs()),
                new Label("Status: " + coffre.getStatus()),
                new Label("Verrouillé: " + (coffre.isEstVerrouille() ? "Oui" : "Non"))
        );
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    // -------------------- Formulaire --------------------
    private void populateForm(CoffreVirtuel c) {
        txtNom.setText(c.getNom());
        txtObjectifMontant.setText(String.valueOf(c.getObjectifMontant()));
        txtMontantActuel.setText(String.valueOf(c.getMontantActuel()));
        cmbStatus.setValue(c.getStatus());
        chkEstVerrouille.setSelected(c.isEstVerrouille());

        try {
            dpDateCreation.setValue(c.getDateCreation() != null && !c.getDateCreation().isEmpty()
                    ? LocalDate.parse(c.getDateCreation()) : null);
            dpDateObjectif.setValue(c.getDateObjectifs() != null && !c.getDateObjectifs().isEmpty()
                    ? LocalDate.parse(c.getDateObjectifs()) : null);
        } catch (Exception e) {
            dpDateCreation.setValue(null);
            dpDateObjectif.setValue(null);
        }

        // ✅ SÉLECTIONNER LE COMPTE DANS LA COMBOBOX
        for (CompteBancaire compte : cmbCompteBancaire.getItems()) {
            if (compte.getIdCompte() == c.getIdCompte()) {
                cmbCompteBancaire.setValue(compte);
                break;
            }
        }
    }

    private void clearForm() {
        txtNom.clear();
        txtObjectifMontant.clear();
        txtMontantActuel.clear();
        cmbStatus.setValue(null);
        cmbCompteBancaire.setValue(null);  // ✅ NOUVEAU
        chkEstVerrouille.setSelected(false);
        dpDateCreation.setValue(null);
        dpDateObjectif.setValue(null);
        tableCoffres.getSelectionModel().clearSelection();
        selectedCoffre = null;
        isEditMode = false;
        btnAjouter.setText("Enregistrer");
    }

    // -------------------- Validation --------------------
    private boolean validateForm() {
        boolean valid = true;

        // Clear previous errors
        lblNomError.setText("");
        lblObjectifError.setText("");
        lblMontantError.setText("");
        lblDateCreationError.setText("");
        lblDateObjectifError.setText("");
        if (lblCompteError != null) lblCompteError.setText("");  // ✅ NOUVEAU

        // --- Nom ---
        String nom = txtNom.getText().trim();
        if (nom.isEmpty()) {
            lblNomError.setText("Le nom est obligatoire.");
            valid = false;
        } else if (nom.length() < 3 || nom.length() > 50) {
            lblNomError.setText("Le nom doit contenir entre 3 et 50 caractères.");
            valid = false;
        } else if (!nom.matches("[a-zA-Z0-9 ]+")) {
            lblNomError.setText("Caractères invalides (lettres, chiffres et espaces seulement).");
            valid = false;
        }

        // --- Objectif Montant ---
        double objectif = 0;
        try {
            objectif = Double.parseDouble(txtObjectifMontant.getText().trim());
            if (objectif <= 0 || objectif > 1_000_000) {
                lblObjectifError.setText("Doit être > 0 et < 1 000 000.");
                valid = false;
            }
        } catch (NumberFormatException e) {
            lblObjectifError.setText("Montant invalide.");
            valid = false;
        }

        // --- Montant Actuel ---
        double montant = 0;
        try {
            montant = Double.parseDouble(txtMontantActuel.getText().trim());
            if (montant < 0) {
                lblMontantError.setText("Montant >= 0.");
                valid = false;
            } else if (montant > objectif) {
                lblMontantError.setText("Ne peut dépasser l'objectif.");
                valid = false;
            }
        } catch (NumberFormatException e) {
            lblMontantError.setText("Montant invalide.");
            valid = false;
        }

        // --- Date Création ---
        LocalDate dateCreation = dpDateCreation.getValue();
        if (dateCreation == null) {
            lblDateCreationError.setText("Date de création obligatoire.");
            valid = false;
        } else if (dateCreation.isAfter(LocalDate.now())) {
            lblDateCreationError.setText("Date ne peut pas être dans le futur.");
            valid = false;
        }

        // --- Date Objectifs ---
        LocalDate dateObjectif = dpDateObjectif.getValue();
        if (dateObjectif != null && dateObjectif.isBefore(dateCreation)) {
            lblDateObjectifError.setText("Doit être postérieure à la date de création.");
            valid = false;
        }

        // ✅ VALIDATION COMPTE BANCAIRE
        if (cmbCompteBancaire.getValue() == null) {
            if (lblCompteError != null) {
                lblCompteError.setText("Veuillez sélectionner un compte bancaire.");
            }
            valid = false;
        }

        return valid;
    }

    private double parseDouble(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        return Double.parseDouble(s);
    }

    // -------------------- Stats --------------------
    private void updateStats() {
        int total = coffresList.size();
        double totalMontants = coffresList.stream().mapToDouble(CoffreVirtuel::getMontantActuel).sum();
        long actifs = coffresList.stream().filter(c -> "Actif".equalsIgnoreCase(c.getStatus())).count();
        lblTotalCoffres.setText(String.valueOf(total));
        lblMontantTotal.setText(String.format(java.util.Locale.US, "%,.2f DT", totalMontants));
        lblCoffresActifs.setText(String.valueOf(actifs));
    }

    private void updateTableInfo() {
        int total = coffresList.size();
        int filtered = filteredData != null ? filteredData.size() : total;
        lblTableInfo.setText(String.format("Affichage de %d sur %d coffres", filtered, total));
    }

    // -------------------- Pagination --------------------
    @FXML
    void pageFirst(ActionEvent event) { tableCoffres.scrollTo(0); }

    @FXML
    void pagePrev(ActionEvent event) {
        tableCoffres.scrollTo(Math.max(tableCoffres.getSelectionModel().getSelectedIndex() - 10, 0));
    }

    @FXML
    void pageNext(ActionEvent event) {
        tableCoffres.scrollTo(tableCoffres.getSelectionModel().getSelectedIndex() + 10);
    }

    @FXML
    void pageLast(ActionEvent event) {
        tableCoffres.scrollTo(Integer.MAX_VALUE);
    }

    // -------------------- Messages --------------------
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

    // -------------------- Rafraîchir données --------------------
    private void refreshData() {
        coffresList.setAll(service.getAll());
        updateStats();
        updateTableInfo();
    }

    @FXML
    void envoyerSMS(ActionEvent event) {
        showInfo("SMS", "Fonctionnalité d'envoi SMS en cours de développement.");
    }

    @FXML
    void exporterPDF(ActionEvent event) {
        showInfo("Export PDF", "Fonctionnalité d'export PDF en cours de développement.");
    }

    // -------------------- trier données --------------------
    @FXML
    void trierParDateCreation(ActionEvent event) {
        tableCoffres.getSortOrder().clear();
        tableCoffres.getSortOrder().add(colDateCreation);
        colDateCreation.setSortType(TableColumn.SortType.ASCENDING);
        tableCoffres.sort();
    }

    @FXML
    void trierParId(ActionEvent event) {
        tableCoffres.getSortOrder().clear();
        tableCoffres.getSortOrder().add(colID);
        colID.setSortType(TableColumn.SortType.ASCENDING);
        tableCoffres.sort();
    }

    @FXML
    void trierParMontantActuel(ActionEvent event) {
        tableCoffres.getSortOrder().clear();
        tableCoffres.getSortOrder().add(colMontantActuel);
        colMontantActuel.setSortType(TableColumn.SortType.ASCENDING);
        tableCoffres.sort();
    }

    @FXML
    void trierParNom(ActionEvent event) {
        tableCoffres.getSortOrder().clear();
        tableCoffres.getSortOrder().add(colNom);
        colNom.setSortType(TableColumn.SortType.ASCENDING);
        tableCoffres.sort();
    }

    @FXML
    void trierParObjectif(ActionEvent event) {
        tableCoffres.getSortOrder().clear();
        tableCoffres.getSortOrder().add(colObjectif);
        colObjectif.setSortType(TableColumn.SortType.ASCENDING);
        tableCoffres.sort();
    }

    @FXML
    void trierParStatut(ActionEvent event) {
        tableCoffres.getSortOrder().clear();
        tableCoffres.getSortOrder().add(colStatus);
        colStatus.setSortType(TableColumn.SortType.ASCENDING);
        tableCoffres.sort();
    }
}