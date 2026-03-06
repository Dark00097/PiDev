package com.nexora.bank.controllers;

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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReclamationController implements Initializable {

    @FXML private Label lblNombreReclamations;
    @FXML private Label lblEnAttente;
    @FXML private Label lblResolues;

    @FXML private TextField txtIdReclamation;
    @FXML private DatePicker dpDateReclamation;
    @FXML private ComboBox<String> cmbTypeReclamation;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cmbStatut;
    @FXML private TextField txtRecherche;

    @FXML private Button btnAjouter;

    @FXML private TableView<Reclamation> tableReclamations;
    @FXML private TableColumn<Reclamation, String> colIdReclamation;
    @FXML private TableColumn<Reclamation, String> colDateReclamation;
    @FXML private TableColumn<Reclamation, String> colTypeReclamation;
    @FXML private TableColumn<Reclamation, String> colDescription;
    @FXML private TableColumn<Reclamation, String> colIdUser;
    @FXML private TableColumn<Reclamation, String> colIdTransaction;
    @FXML private TableColumn<Reclamation, String> colStatut;
    @FXML private TableColumn<Reclamation, Void> colActions;

    @FXML private Label lblTableInfo;

    private final ObservableList<Reclamation> reclamationsList = FXCollections.observableArrayList();
    private FilteredList<Reclamation> filteredData;
    private Reclamation selectedReclamation;
    private boolean editMode;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTable();
        initializeSearch();
        loadSampleData();
        updateStats();
    }

    private void initializeTable() {
        colIdReclamation.setCellValueFactory(new PropertyValueFactory<>("idReclamation"));
        colDateReclamation.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getDateReclamation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        colTypeReclamation.setCellValueFactory(new PropertyValueFactory<>("typeReclamation"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colIdUser.setCellValueFactory(new PropertyValueFactory<>("idUser"));
        colIdTransaction.setCellValueFactory(new PropertyValueFactory<>("idTransaction"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colTypeReclamation.setCellFactory(col -> new TableCell<>() {
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
                    case "VIREMENT_NON_RECU" -> badge.getStyleClass().add("nx-badge-warning");
                    case "ERREUR_MONTANT" -> badge.getStyleClass().add("nx-badge-error");
                    case "TRANSACTION_INCONNUE" -> badge.getStyleClass().add("nx-badge-purple");
                    case "PROBLEME_CARTE" -> badge.getStyleClass().add("nx-badge-info");
                    default -> badge.getStyleClass().add("nx-badge-blue");
                }
                setGraphic(badge);
            }
        });

        colStatut.setCellFactory(col -> new TableCell<>() {
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
                    case "Resolue" -> badge.getStyleClass().add("nx-badge-success");
                    case "Rejetee" -> badge.getStyleClass().add("nx-badge-error");
                    case "En cours" -> badge.getStyleClass().add("nx-badge-info");
                    default -> badge.getStyleClass().add("nx-badge-warning");
                }
                setGraphic(badge);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button edit = new Button();
            private final Button delete = new Button();
            private final HBox box = new HBox(8, edit, delete);

            {
                edit.getStyleClass().addAll("nx-table-action", "nx-table-action-edit");
                delete.getStyleClass().addAll("nx-table-action", "nx-table-action-delete");

                SVGPath editIcon = new SVGPath();
                editIcon.setContent("M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z");
                editIcon.getStyleClass().add("nx-action-icon");
                edit.setGraphic(editIcon);

                SVGPath deleteIcon = new SVGPath();
                deleteIcon.setContent("M19 7l-.867 12.142A2 2 0 0 1 16.138 21H7.862a2 2 0 0 1-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v3M4 7h16");
                deleteIcon.getStyleClass().add("nx-action-icon");
                delete.setGraphic(deleteIcon);

                box.setAlignment(Pos.CENTER);
                edit.setOnAction(e -> editReclamation(getTableView().getItems().get(getIndex())));
                delete.setOnAction(e -> deleteReclamation(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableReclamations.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                selectedReclamation = newValue;
                populateForm(newValue);
                editMode = true;
                btnAjouter.setText("Modifier");
            }
        });
    }

    private void initializeSearch() {
        filteredData = new FilteredList<>(reclamationsList, p -> true);
        txtRecherche.textProperty().addListener((obs, oldValue, newValue) -> {
            String keyword = newValue == null ? "" : newValue.toLowerCase().trim();
            filteredData.setPredicate(r -> keyword.isEmpty()
                || r.getIdReclamation().toLowerCase().contains(keyword)
                || r.getTypeReclamation().toLowerCase().contains(keyword)
                || r.getIdUser().toLowerCase().contains(keyword)
                || r.getIdTransaction().toLowerCase().contains(keyword)
                || r.getDescription().toLowerCase().contains(keyword)
                || r.getStatut().toLowerCase().contains(keyword));
            updateTableInfo();
        });

        SortedList<Reclamation> sorted = new SortedList<>(filteredData);
        sorted.comparatorProperty().bind(tableReclamations.comparatorProperty());
        tableReclamations.setItems(sorted);
    }

    private void loadSampleData() {
        reclamationsList.addAll(
            new Reclamation("REC-2026-001", LocalDate.now().minusDays(2), "VIREMENT_NON_RECU", "Virement sortant non recu par le beneficiaire", "USR-0021", "TX-3021", "En attente"),
            new Reclamation("REC-2026-002", LocalDate.now().minusDays(1), "ERREUR_MONTANT", "Montant debite superieur a la somme confirmee", "USR-0014", "TX-3018", "En cours"),
            new Reclamation("REC-2026-003", LocalDate.now().minusDays(5), "PROBLEME_CARTE", "Carte bloquee apres paiement international", "USR-0009", "TX-2990", "Resolue"),
            new Reclamation("REC-2026-004", LocalDate.now().minusDays(3), "TRANSACTION_INCONNUE", "Operation non reconnue sur le compte", "USR-0030", "TX-3004", "Rejetee")
        );
        updateTableInfo();
    }

    private void updateStats() {
        lblNombreReclamations.setText(String.valueOf(reclamationsList.size()));
        long pending = reclamationsList.stream().filter(r -> "En attente".equals(r.getStatut())).count();
        long resolved = reclamationsList.stream().filter(r -> "Resolue".equals(r.getStatut())).count();
        lblEnAttente.setText(String.valueOf(pending));
        lblResolues.setText(String.valueOf(resolved));
    }

    private void updateTableInfo() {
        int filtered = filteredData == null ? 0 : filteredData.size();
        lblTableInfo.setText(String.format("Affichage de %d sur %d entrees", filtered, reclamationsList.size()));
    }

    private void populateForm(Reclamation reclamation) {
        txtIdReclamation.setText(reclamation.getIdReclamation());
        dpDateReclamation.setValue(reclamation.getDateReclamation());
        cmbTypeReclamation.setValue(reclamation.getTypeReclamation());
        txtDescription.setText(reclamation.getDescription());
        cmbStatut.setValue(reclamation.getStatut());
    }

    private void clearForm() {
        txtIdReclamation.clear();
        dpDateReclamation.setValue(null);
        cmbTypeReclamation.setValue(null);
        txtDescription.clear();
        cmbStatut.setValue(null);
        selectedReclamation = null;
        editMode = false;
        btnAjouter.setText("Ajouter");
        tableReclamations.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleAjouter() {
        String id = txtIdReclamation.getText() == null ? "" : txtIdReclamation.getText().trim();
        LocalDate date = dpDateReclamation.getValue();
        String type = cmbTypeReclamation.getValue();
        String description = txtDescription.getText() == null ? "" : txtDescription.getText().trim();
        String statut = cmbStatut.getValue();

        if (id.isEmpty()) {
            id = generateReclamationId();
        }

        if (date == null || type == null || description.isEmpty() || statut == null) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Remplissez tous les champs obligatoires.");
            return;
        }

        String idUser = (editMode && selectedReclamation != null)
            ? selectedReclamation.getIdUser()
            : generateUserId();
        String idTransaction = (editMode && selectedReclamation != null)
            ? selectedReclamation.getIdTransaction()
            : generateTransactionId();

        if (editMode && selectedReclamation != null) {
            selectedReclamation.setIdReclamation(id);
            selectedReclamation.setDateReclamation(date);
            selectedReclamation.setTypeReclamation(type);
            selectedReclamation.setDescription(description);
            selectedReclamation.setIdUser(idUser);
            selectedReclamation.setIdTransaction(idTransaction);
            selectedReclamation.setStatut(statut);
            tableReclamations.refresh();
        } else {
            reclamationsList.add(new Reclamation(id, date, type, description, idUser, idTransaction, statut));
        }

        clearForm();
        updateStats();
        updateTableInfo();
    }

    @FXML
    private void handleSupprimer() {
        if (selectedReclamation != null) {
            deleteReclamation(selectedReclamation);
        }
    }

    @FXML
    private void handleAnnuler() {
        clearForm();
    }

    private String generateReclamationId() {
        return generateNextId("REC-2026-", 3, Reclamation::getIdReclamation);
    }

    private String generateUserId() {
        return generateNextId("USR-", 4, Reclamation::getIdUser);
    }

    private String generateTransactionId() {
        return generateNextId("TX-", 4, Reclamation::getIdTransaction);
    }

    private String generateNextId(String prefix, int width, Function<Reclamation, String> extractor) {
        int max = reclamationsList.stream()
            .map(extractor)
            .mapToInt(value -> extractNumericSuffix(value, prefix))
            .max()
            .orElse(0);
        return String.format("%s%0" + width + "d", prefix, max + 1);
    }

    private int extractNumericSuffix(String value, String prefix) {
        if (value == null || !value.startsWith(prefix)) {
            return 0;
        }
        String suffix = value.substring(prefix.length());
        Matcher matcher = Pattern.compile("(\\d+)").matcher(suffix);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void editReclamation(Reclamation reclamation) {
        selectedReclamation = reclamation;
        populateForm(reclamation);
        editMode = true;
        btnAjouter.setText("Modifier");
    }

    private void deleteReclamation(Reclamation reclamation) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette reclamation ?", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.orElse(ButtonType.CANCEL) == ButtonType.OK) {
            reclamationsList.remove(reclamation);
            clearForm();
            updateStats();
            updateTableInfo();
        }
    }

    @FXML
    private void trierParDate() {
        reclamationsList.sort(Comparator.comparing(Reclamation::getDateReclamation).reversed());
    }

    @FXML
    private void trierParType() {
        reclamationsList.sort(Comparator.comparing(Reclamation::getTypeReclamation));
    }

    @FXML
    private void trierParStatut() {
        reclamationsList.sort(Comparator.comparing(Reclamation::getStatut));
    }

    @FXML
    private void exporterPDF() {
        showAlert(Alert.AlertType.INFORMATION, "Export", "Export PDF en developpement.");
    }

    @FXML
    private void envoyerSMS() {
        showAlert(Alert.AlertType.INFORMATION, "Notification", "Envoi notification client en developpement.");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class Reclamation {
        private String idReclamation;
        private LocalDate dateReclamation;
        private String typeReclamation;
        private String description;
        private String idUser;
        private String idTransaction;
        private String statut;

        public Reclamation(String idReclamation, LocalDate dateReclamation, String typeReclamation, String description,
                           String idUser, String idTransaction, String statut) {
            this.idReclamation = idReclamation;
            this.dateReclamation = dateReclamation;
            this.typeReclamation = typeReclamation;
            this.description = description;
            this.idUser = idUser;
            this.idTransaction = idTransaction;
            this.statut = statut;
        }

        public String getIdReclamation() {
            return idReclamation;
        }

        public void setIdReclamation(String idReclamation) {
            this.idReclamation = idReclamation;
        }

        public LocalDate getDateReclamation() {
            return dateReclamation;
        }

        public void setDateReclamation(LocalDate dateReclamation) {
            this.dateReclamation = dateReclamation;
        }

        public String getTypeReclamation() {
            return typeReclamation;
        }

        public void setTypeReclamation(String typeReclamation) {
            this.typeReclamation = typeReclamation;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getIdUser() {
            return idUser;
        }

        public void setIdUser(String idUser) {
            this.idUser = idUser;
        }

        public String getIdTransaction() {
            return idTransaction;
        }

        public void setIdTransaction(String idTransaction) {
            this.idTransaction = idTransaction;
        }

        public String getStatut() {
            return statut;
        }

        public void setStatut(String statut) {
            this.statut = statut;
        }
    }
}
