package com.nexora.bank.controllers;

import com.nexora.bank.Models.CoffreVirtuel;
import com.nexora.bank.Models.CompteBancaire;
import com.nexora.bank.Service.CoffreVirtuelService;
import com.nexora.bank.Service.CompteBancaireService;
import com.nexora.bank.Utils.SessionManager;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * MODIFICATIONS apportées :
 * ──────────────────────────
 * ★ [SECURITE]  loadAccountsFromDB()   → service.getByUser(idUser) au lieu de getAll()
 * ★ [SECURITE]  saveAccount()          → passe idUser dans l'objet CompteBancaire
 * ★ [SECURITE]  deleteAccount()        → idUser déjà présent dans selectedCompte
 * ★ [NOUVEAU]   CoffreVirtuelService   → chargement des vrais coffres depuis la BD
 * ★ [NOUVEAU]   handleShowCoffres()    → coffreService.getByCompte(idCompte)
 * ★ [NOUVEAU]   renderCoffreCards()    → génère les cartes coffres dynamiquement
 * ★ [NOUVEAU]   buildCoffreCard()      → carte individuelle avec barre de progression
 * ★ [CORRECTION] saveCoffre()          → constructeur CoffreVirtuel à 9 params (+ idUser)
 * ★ [NOUVEAU]   updateKPIs()           → lblVaultCount = vrai nombre de coffres du user
 */
public class UserDashboardAccountsSectionController {

    // ─── Containers ───────────────────────────────────────────────────────────
    @FXML private VBox     coffreFormContainer;
    @FXML private VBox     coffresDisplayContainer;
    @FXML private FlowPane coffresCardsContainer;

    // ─── Champs formulaire Compte ──────────────────────────────────────────────
    @FXML private TextField        txtAccountNumber;
    @FXML private TextField        txtBalance;
    @FXML private DatePicker       dpOpeningDate;
    @FXML private ComboBox<String> cbStatus;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField        txtWithdrawLimit;
    @FXML private TextField        txtTransferLimit;

    // ─── Labels d'erreur pour le formulaire Compte ────────────────────────────
    @FXML private Label lblAccountNumberError;
    @FXML private Label lblBalanceError;
    @FXML private Label lblOpeningDateError;
    @FXML private Label lblStatusError;
    @FXML private Label lblTypeError;
    @FXML private Label lblWithdrawLimitError;
    @FXML private Label lblTransferLimitError;

    // ─── Champs formulaire Coffre ──────────────────────────────────────────────
    @FXML private TextField        txtCoffreName;
    @FXML private TextField        txtCoffreObjectif;
    @FXML private TextField        txtCoffreDepotInitial;
    @FXML private DatePicker       dpCoffreDateCible;
    @FXML private ComboBox<String> cbCoffreStatut;
    @FXML private CheckBox         chkVerrouiller;
    @FXML private CheckBox         chkDepotAuto;
    @FXML private ComboBox<CompteBancaire> cmbCoffreCompte;

    // ─── Labels d'erreur pour le formulaire Coffre ────────────────────────────
    @FXML private Label lblCoffreCompteError;
    @FXML private Label lblCoffreNameError;
    @FXML private Label lblCoffreObjectifError;
    @FXML private Label lblCoffreDepotError;
    @FXML private Label lblCoffreDateError;
    @FXML private Label lblCoffreStatutError;
    // ★ NOUVEAU : label du bouton "Créer/Modifier" et titre du formulaire
    @FXML private Label lblCoffreFormTitle;
    @FXML private Label lblCoffreFormSubtitle;
    @FXML private Label lblCoffreBtnText;

    // ─── KPI Labels ───────────────────────────────────────────────────────────
    @FXML private Label lblTotalBalance;
    @FXML private Label lblTotalBalanceTrend;
    @FXML private Label lblActiveAccounts;
    @FXML private Label lblVaultCount;

    // ─── Grille comptes & filtres ─────────────────────────────────────────────
    @FXML private FlowPane comptesGrid;
    @FXML private TextField txtSearch;
    @FXML private Button    btnFilterAll;
    @FXML private Button    btnFilterCourant;
    @FXML private Button    btnFilterEpargne;
    @FXML private Button    btnFilterProfessionnel;

    // ─── Services ─────────────────────────────────────────────────────────────
    private final CompteBancaireService service       = new CompteBancaireService();
    private final CoffreVirtuelService  coffreService = new CoffreVirtuelService();

    // ─── État local ───────────────────────────────────────────────────────────
    private ObservableList<CompteBancaire> allAccounts = FXCollections.observableArrayList();
    private CompteBancaire selectedCompte       = null;
    private CompteBancaire compteAfficheCoffres = null;
    private String activeFilter = "All";

    // ★ NOUVEAU : état édition coffre
    private CoffreVirtuel selectedCoffre   = null;
    private boolean       isEditCoffreMode = false;

    private static final Duration ANIMATION_DURATION = Duration.millis(250);

    // ─── Initialisation ───────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        setVisibleManaged(coffreFormContainer, false);
        setVisibleManaged(coffresDisplayContainer, false);
        setupSearchListener();
        loadAccountsFromDB();
        initializeCoffreCompte();
    }

    private void setupSearchListener() {
        if (txtSearch != null)
            txtSearch.textProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void initializeCoffreCompte() {
        cmbCoffreCompte.setItems(allAccounts);
        cmbCoffreCompte.setCellFactory(param -> new ListCell<CompteBancaire>() {
            @Override
            protected void updateItem(CompteBancaire compte, boolean empty) {
                super.updateItem(compte, empty);
                setText((empty || compte == null) ? null
                        : compte.getNumeroCompte() + " - " + compte.getTypeCompte());
            }
        });
        cmbCoffreCompte.setButtonCell(new ListCell<CompteBancaire>() {
            @Override
            protected void updateItem(CompteBancaire compte, boolean empty) {
                super.updateItem(compte, empty);
                setText((empty || compte == null) ? null
                        : compte.getNumeroCompte() + " - " + compte.getTypeCompte());
            }
        });
    }

    // ─── Chargement BD ────────────────────────────────────────────────────────

    private void loadAccountsFromDB() {
        int idUser = SessionManager.getInstance().getCurrentUserId();
        List<CompteBancaire> list = (idUser > 0)
                ? service.getByUser(idUser)
                : service.getAll();
        allAccounts.setAll(list);
        updateKPIs();
        renderAccountCards(allAccounts);
    }

    private void updateKPIs() {
        if (lblTotalBalance != null) {
            double total = allAccounts.stream().mapToDouble(CompteBancaire::getSolde).sum();
            lblTotalBalance.setText(String.format(Locale.US, "%,.2f DT", total));
        }
        if (lblActiveAccounts != null) {
            long active = allAccounts.stream()
                    .filter(c -> "Active".equalsIgnoreCase(c.getStatutCompte())
                            || "Actif".equalsIgnoreCase(c.getStatutCompte()))
                    .count();
            lblActiveAccounts.setText(String.valueOf(active));
        }
        if (lblVaultCount != null) {
            long totalCoffres = allAccounts.stream()
                    .mapToLong(c -> coffreService.getByCompte(c.getIdCompte()).size())
                    .sum();
            lblVaultCount.setText(String.valueOf(totalCoffres));
        }
    }

    // ─── Filtres ──────────────────────────────────────────────────────────────

    @FXML private void filterAll()           { activeFilter = "All";           updateFilterTabStyles(); applyFilter(); }
    @FXML private void filterCourant()       { activeFilter = "Courant";       updateFilterTabStyles(); applyFilter(); }
    @FXML private void filterEpargne()       { activeFilter = "Epargne";       updateFilterTabStyles(); applyFilter(); }
    @FXML private void filterProfessionnel() { activeFilter = "Professionnel"; updateFilterTabStyles(); applyFilter(); }

    private void updateFilterTabStyles() {
        Button[] tabs    = {btnFilterAll, btnFilterCourant, btnFilterEpargne, btnFilterProfessionnel};
        String[] filters = {"All", "Courant", "Epargne", "Professionnel"};
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == null) continue;
            boolean active = filters[i].equals(activeFilter);
            tabs[i].getStyleClass().removeAll("filter-tab", "filter-tab-active");
            tabs[i].getStyleClass().add(active ? "filter-tab-active" : "filter-tab");
        }
    }

    private void applyFilter() {
        String search = (txtSearch != null) ? txtSearch.getText().toLowerCase() : "";
        List<CompteBancaire> filtered = allAccounts.stream()
                .filter(c -> {
                    boolean matchType = "All".equals(activeFilter)
                            || (c.getTypeCompte() != null && c.getTypeCompte().equalsIgnoreCase(activeFilter));
                    boolean matchSearch = search.isEmpty()
                            || (c.getNumeroCompte() != null && c.getNumeroCompte().toLowerCase().contains(search))
                            || (c.getStatutCompte() != null && c.getStatutCompte().toLowerCase().contains(search))
                            || (c.getTypeCompte()   != null && c.getTypeCompte().toLowerCase().contains(search));
                    return matchType && matchSearch;
                })
                .collect(Collectors.toList());
        renderAccountCards(filtered);
    }

    // ─── Cartes Comptes ───────────────────────────────────────────────────────

    private void renderAccountCards(List<CompteBancaire> comptes) {
        if (comptesGrid == null) return;
        comptesGrid.getChildren().clear();
        for (CompteBancaire c : comptes) comptesGrid.getChildren().add(buildAccountCard(c));
        comptesGrid.getChildren().add(buildAddCard());
    }

    private VBox buildAccountCard(CompteBancaire compte) {
        String type = compte.getTypeCompte() != null ? compte.getTypeCompte() : "Courant";
        String headerStyle, typeLabel, typeTag, iconLiteral;
        switch (type) {
            case "Epargne":
                headerStyle = "account-card-header-savings";  typeLabel = "COMPTE EPARGNE";
                typeTag = "Haut rendement";                   iconLiteral = "fas-piggy-bank"; break;
            case "Professionnel":
                headerStyle = "account-card-header-business"; typeLabel = "COMPTE PROFESSIONNEL";
                typeTag = "Entreprise";                       iconLiteral = "fas-briefcase"; break;
            default:
                headerStyle = "account-card-header-current";  typeLabel = "COMPTE COURANT";
                typeTag = "Principal";                        iconLiteral = "fas-credit-card"; break;
        }

        String statut      = compte.getStatutCompte() != null ? compte.getStatutCompte() : "";
        String statusStyle = ("Active".equalsIgnoreCase(statut) || "Actif".equalsIgnoreCase(statut))
                ? "status-active" : "Bloque".equalsIgnoreCase(statut) ? "status-blocked" : "status-closed";
        String numero = compte.getNumeroCompte() != null ? compte.getNumeroCompte() : "";
        String last4  = numero.length() >= 4 ? numero.substring(numero.length() - 4) : numero;

        FontIcon typeIcon = new FontIcon(iconLiteral); typeIcon.getStyleClass().add("account-type-icon");
        StackPane typeBadge = new StackPane(typeIcon); typeBadge.getStyleClass().add("account-type-badge");
        Label typeLbl = new Label(typeLabel); typeLbl.getStyleClass().add("account-type-label");
        Label tagLbl  = new Label(typeTag);   tagLbl.getStyleClass().add("account-tag");
        VBox titleBox = new VBox(1, typeLbl, tagLbl);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        FontIcon menuIcon = new FontIcon("fas-ellipsis-v"); menuIcon.getStyleClass().add("account-menu-icon");
        Button menuBtn = new Button(); menuBtn.setGraphic(menuIcon); menuBtn.getStyleClass().add("account-menu-btn");
        HBox header = new HBox(10, typeBadge, titleBox, spacer, menuBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().addAll("account-card-header", headerStyle);

        Label maskedLbl  = new Label("---- ---- ---- "); maskedLbl.getStyleClass().add("account-number-masked");
        Label visibleLbl = new Label(last4);             visibleLbl.getStyleClass().add("account-number-visible");
        HBox numberBox   = new HBox(8, maskedLbl, visibleLbl); numberBox.setAlignment(Pos.CENTER_LEFT);
        Label balanceLbl   = new Label("Solde disponible"); balanceLbl.getStyleClass().add("balance-label");
        Label balanceValue = new Label(String.format(Locale.US, "%,.2f DT", compte.getSolde()));
        balanceValue.getStyleClass().add("balance-value");
        if (compte.getSolde() == 0) balanceValue.getStyleClass().add("balance-zero");
        VBox balanceBox = new VBox(4, balanceLbl, balanceValue);
        Label statusBadgeLbl = new Label(statut);
        StackPane statusBadge = new StackPane(statusBadgeLbl);
        statusBadge.getStyleClass().addAll("status-badge", statusStyle);
        String activityText = ("Active".equalsIgnoreCase(statut) || "Actif".equalsIgnoreCase(statut))
                ? "Derniere activite : aujourd hui"
                : "Bloque".equalsIgnoreCase(statut) ? "Verification en attente" : "Compte ferme";
        Label activityLbl = new Label(activityText); activityLbl.getStyleClass().add("last-activity");
        HBox statusBox = new HBox(8, statusBadge, activityLbl); statusBox.setAlignment(Pos.CENTER_LEFT);
        VBox body = new VBox(12, numberBox, balanceBox, statusBox); body.getStyleClass().add("account-card-body");

        FontIcon lockIcon = new FontIcon("fas-lock"); lockIcon.getStyleClass().add("account-action-icon");
        HBox coffresHBox  = new HBox(6, lockIcon, new Label("Coffres")); coffresHBox.setAlignment(Pos.CENTER);
        Button coffresBtn = new Button(); coffresBtn.setGraphic(coffresHBox);
        coffresBtn.getStyleClass().add("account-action-btn");
        coffresBtn.setOnAction(e -> handleShowCoffres(compte));

        FontIcon histIcon = new FontIcon("fas-history"); histIcon.getStyleClass().add("account-action-icon");
        HBox histHBox     = new HBox(6, histIcon, new Label("Historique")); histHBox.setAlignment(Pos.CENTER);
        Button histBtn = new Button(); histBtn.setGraphic(histHBox);
        histBtn.getStyleClass().add("account-action-btn");

        HBox footer = new HBox(10, coffresBtn, histBtn); footer.getStyleClass().add("account-card-footer");

        VBox card = new VBox(0, header, body, footer);
        card.getStyleClass().add("account-card");
        card.setPrefWidth(260);
        card.setOnMouseClicked((MouseEvent ev) -> populateForm(compte));
        return card;
    }

    private VBox buildAddCard() {
        FontIcon plusIcon = new FontIcon("fas-plus"); plusIcon.getStyleClass().add("add-account-icon");
        StackPane iconWrapper = new StackPane(plusIcon); iconWrapper.getStyleClass().add("add-account-icon-wrapper");
        Label lbl    = new Label("Lier un nouveau compte");           lbl.getStyleClass().add("add-account-label");
        Label subLbl = new Label("Connecter un autre compte bancaire"); subLbl.getStyleClass().add("add-account-sublabel");
        VBox card = new VBox(12, iconWrapper, lbl, subLbl);
        card.getStyleClass().addAll("account-card", "account-card-add");
        card.setPrefWidth(260); card.setAlignment(Pos.CENTER);
        card.setOnMouseClicked(e -> addNewAccount());
        return card;
    }

    // ─── Coffres Display ──────────────────────────────────────────────────────

    private void handleShowCoffres(CompteBancaire compte) {
        compteAfficheCoffres = compte;
        List<CoffreVirtuel> coffres = coffreService.getByCompte(compte.getIdCompte());
        renderCoffreCards(coffres);
        if (!coffresDisplayContainer.isVisible()) {
            setVisibleManaged(coffresDisplayContainer, true);
            animateSlideIn(coffresDisplayContainer);
        }
    }

    private void renderCoffreCards(List<CoffreVirtuel> coffres) {
        if (coffresCardsContainer == null) return;
        coffresCardsContainer.getChildren().clear();
        if (coffres.isEmpty()) {
            Label empty = new Label("Aucun coffre pour ce compte. Cliquez sur Nouveau coffre pour en creer un !");
            empty.getStyleClass().add("vaults-section-subtitle");
            empty.setWrapText(true);
            coffresCardsContainer.getChildren().add(empty);
            return;
        }
        for (CoffreVirtuel coffre : coffres)
            coffresCardsContainer.getChildren().add(buildCoffreCard(coffre));
    }

    private VBox buildCoffreCard(CoffreVirtuel coffre) {
        String iconLit      = coffre.isEstVerrouille() ? "fas-lock"       : "fas-piggy-bank";
        String wrapperStyle = coffre.isEstVerrouille() ? "vault-icon-wrapper vault-icon-emergency"
                : "vault-icon-wrapper vault-icon-vacation";
        String indicStyle   = coffre.isEstVerrouille() ? "vault-status-indicator vault-status-locked"
                : "vault-status-indicator vault-status-active";

        FontIcon icon = new FontIcon(iconLit); icon.getStyleClass().add("vault-icon");
        StackPane iconWrapper = new StackPane(icon);
        iconWrapper.getStyleClass().addAll(wrapperStyle.split(" "));

        Label nomLbl  = new Label(coffre.getNom()); nomLbl.getStyleClass().add("vault-name");
        String dateStr = (coffre.getDateObjectifs() != null && !coffre.getDateObjectifs().isEmpty())
                ? "Objectif : " + coffre.getDateObjectifs() : "Sans echeance";
        Label dateLbl = new Label(dateStr); dateLbl.getStyleClass().add("vault-target-date");
        VBox nameBox  = new VBox(2, nomLbl, dateLbl); HBox.setHgrow(nameBox, Priority.ALWAYS);

        StackPane indicator = new StackPane();
        indicator.getStyleClass().addAll(indicStyle.split(" "));
        if (coffre.isEstVerrouille()) {
            FontIcon lockIco = new FontIcon("fas-lock"); lockIco.getStyleClass().add("vault-lock-icon");
            indicator.getChildren().add(lockIco);
        }
        HBox topRow = new HBox(10, iconWrapper, nameBox, indicator); topRow.setAlignment(Pos.CENTER_LEFT);

        double objectif    = coffre.getObjectifMontant();
        double actuel      = coffre.getMontantActuel();
        double progression = objectif > 0 ? Math.min(actuel / objectif, 1.0) : 0;
        int    pct         = (int)(progression * 100);

        Label currentLbl = new Label(String.format(Locale.US, "%,.2f DT", actuel));
        currentLbl.getStyleClass().add("vault-current");
        Label targetLbl = new Label(String.format(Locale.US, "sur %,.2f DT", objectif));
        targetLbl.getStyleClass().add("vault-target");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox amountsRow = new HBox(currentLbl, gap, targetLbl); amountsRow.setAlignment(Pos.CENTER_LEFT);

        StackPane progressTrack = new StackPane(); progressTrack.getStyleClass().add("progress-track");
        StackPane progressFill  = new StackPane();
        String fillClass = pct >= 100 ? "progress-fill"
                : pct >= 50 ? "progress-fill progress-fill-alt" : "progress-fill progress-fill-warning";
        progressFill.getStyleClass().addAll(fillClass.split(" "));
        progressFill.setMaxWidth(Double.MAX_VALUE);
        progressFill.setStyle(String.format(Locale.US, "-fx-max-width: %.2f%%;", progression * 100));
        progressTrack.getChildren().add(progressFill);
        progressTrack.setAlignment(Pos.CENTER_LEFT);

        String statusText = pct >= 100 ? "100% termine"
                : pct + "% termine" + (coffre.isEstVerrouille() ? " - Verrouille" : "");
        Label pctLbl = new Label(statusText); pctLbl.getStyleClass().add("vault-percentage");

        VBox progressBox = new VBox(8, amountsRow, progressTrack, pctLbl);

        // ★ NOUVEAU : bouton Supprimer sur la carte
        FontIcon trashIcon = new FontIcon("fas-trash-alt");
        trashIcon.setStyle("-fx-fill: #EF4444; -fx-icon-size: 13;");
        Button btnSuppr = new Button();
        btnSuppr.setGraphic(trashIcon);
        btnSuppr.setTooltip(new Tooltip("Supprimer ce coffre"));
        btnSuppr.setStyle(
                "-fx-background-color: transparent; -fx-cursor: hand; " +
                        "-fx-border-color: #EF4444; -fx-border-radius: 6; -fx-background-radius: 6; " +
                        "-fx-padding: 5 9;");
        btnSuppr.setOnMouseEntered(e -> btnSuppr.setStyle(
                "-fx-background-color: #FEE2E2; -fx-cursor: hand; " +
                        "-fx-border-color: #EF4444; -fx-border-radius: 6; -fx-background-radius: 6; " +
                        "-fx-padding: 5 9;"));
        btnSuppr.setOnMouseExited(e -> btnSuppr.setStyle(
                "-fx-background-color: transparent; -fx-cursor: hand; " +
                        "-fx-border-color: #EF4444; -fx-border-radius: 6; -fx-background-radius: 6; " +
                        "-fx-padding: 5 9;"));
        btnSuppr.setOnAction(e -> deleteCoffre(coffre));

        Region cardSpacer = new Region(); HBox.setHgrow(cardSpacer, Priority.ALWAYS);
        HBox cardFooter = new HBox(cardSpacer, btnSuppr);
        cardFooter.setAlignment(Pos.CENTER_RIGHT);
        cardFooter.setStyle("-fx-padding: 4 0 0 0;");

        VBox card = new VBox(14, topRow, progressBox, cardFooter);
        card.getStyleClass().add("vault-card");
        card.setPrefWidth(240);
        card.setStyle("-fx-cursor: hand;");

        // ★ NOUVEAU : clic sur la carte -> populer le formulaire coffre en mode édition
        card.setOnMouseClicked(ev -> {
            // On ne déclenche pas le clic si on a cliqué sur le bouton supprimer
            if (ev.getTarget() instanceof Button || ev.getTarget() instanceof FontIcon) return;
            populateCoffreForm(coffre);
        });

        return card;
    }

    // ★ NOUVELLE MÉTHODE : peupler le formulaire coffre + passer en mode édition
    private void populateCoffreForm(CoffreVirtuel coffre) {
        selectedCoffre   = coffre;
        isEditCoffreMode = true;

        // Remplir les champs
        if (txtCoffreName      != null) txtCoffreName.setText(coffre.getNom());
        if (txtCoffreObjectif  != null) txtCoffreObjectif.setText(String.valueOf(coffre.getObjectifMontant()));
        if (txtCoffreDepotInitial != null) txtCoffreDepotInitial.setText(String.valueOf(coffre.getMontantActuel()));
        if (cbCoffreStatut     != null) cbCoffreStatut.setValue(coffre.getStatus());
        if (chkVerrouiller     != null) chkVerrouiller.setSelected(coffre.isEstVerrouille());
        if (dpCoffreDateCible  != null) {
            try {
                dpCoffreDateCible.setValue(
                        (coffre.getDateObjectifs() != null && !coffre.getDateObjectifs().isEmpty())
                                ? LocalDate.parse(coffre.getDateObjectifs()) : null);
            } catch (Exception ex) { dpCoffreDateCible.setValue(null); }
        }
        // Sélectionner le compte associé dans la ComboBox
        if (cmbCoffreCompte != null) {
            for (CompteBancaire c : allAccounts) {
                if (c.getIdCompte() == coffre.getIdCompte()) {
                    cmbCoffreCompte.setValue(c);
                    break;
                }
            }
        }

        // Mettre à jour les textes du formulaire
        if (lblCoffreFormTitle    != null) lblCoffreFormTitle.setText("Modifier le coffre virtuel");
        if (lblCoffreFormSubtitle != null) lblCoffreFormSubtitle.setText("Modifier les informations du coffre : " + coffre.getNom());
        if (lblCoffreBtnText      != null) lblCoffreBtnText.setText("Enregistrer les modifications");

        // Ouvrir le formulaire s'il n'est pas visible
        if (coffreFormContainer != null && !coffreFormContainer.isVisible()) {
            setVisibleManaged(coffreFormContainer, true);
            animateSlideIn(coffreFormContainer);
        }
    }

    // ★ NOUVELLE MÉTHODE : supprimer un coffre avec confirmation
    private void deleteCoffre(CoffreVirtuel coffre) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le coffre");
        confirm.setHeaderText("Supprimer \"" + coffre.getNom() + "\" ?");
        confirm.setContentText("Cette action est irréversible. Le coffre sera définitivement supprimé.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                coffreService.remove(coffre);
                // Si on était en train d'éditer ce coffre, réinitialiser le formulaire
                if (selectedCoffre != null && selectedCoffre.getIdCoffre() == coffre.getIdCoffre()) {
                    clearCoffreFormAndResetMode();
                    setVisibleManaged(coffreFormContainer, false);
                }
                // Rafraîchir les cartes coffres du compte concerné
                if (compteAfficheCoffres != null) {
                    handleShowCoffres(compteAfficheCoffres);
                }
                updateKPIs();
                showNotification("Supprimé", "Le coffre a été supprimé avec succès.");
            }
        });
    }

    @FXML private void showCoffres() {
        if (!coffresDisplayContainer.isVisible()) {
            setVisibleManaged(coffresDisplayContainer, true);
            animateSlideIn(coffresDisplayContainer);
        }
    }

    @FXML private void hideCoffres() {
        animateSlideOut(coffresDisplayContainer, () -> {
            setVisibleManaged(coffresDisplayContainer, false);
            compteAfficheCoffres = null;
        });
    }

    // ─── Coffre Form ──────────────────────────────────────────────────────────

    @FXML private void toggleCoffreForm() {
        boolean show = !coffreFormContainer.isVisible();
        if (show) {
            // Ouverture en mode "Nouveau coffre" → on remet tout à zéro
            clearCoffreFormAndResetMode();
            setVisibleManaged(coffreFormContainer, true);
            animateSlideIn(coffreFormContainer);
        } else {
            animateSlideOut(coffreFormContainer, () -> {
                setVisibleManaged(coffreFormContainer, false);
                clearCoffreFormAndResetMode();
            });
        }
    }

    @FXML private void hideCoffreForm() {
        animateSlideOut(coffreFormContainer, () -> {
            setVisibleManaged(coffreFormContainer, false);
            clearCoffreFormAndResetMode();
        });
    }

    private boolean validateCoffreForm() {
        boolean valid = true;
        clearCoffreErrors();

        if (cmbCoffreCompte.getValue() == null) {
            lblCoffreCompteError.setText("Veuillez s\u00e9lectionner un compte.");
            valid = false;
        }
        String nom = txtCoffreName.getText().trim();
        if (nom.isEmpty()) {
            lblCoffreNameError.setText("Le nom est obligatoire.");
            valid = false;
        } else if (nom.length() < 3 || nom.length() > 50) {
            lblCoffreNameError.setText("Le nom doit faire entre 3 et 50 caract\u00e8res.");
            valid = false;
        }
        double objectif = 0;
        try {
            objectif = Double.parseDouble(txtCoffreObjectif.getText().trim());
            if (objectif <= 0) {
                lblCoffreObjectifError.setText("L'objectif doit \u00eatre > 0.");
                valid = false;
            } else if (objectif > 1_000_000) {
                lblCoffreObjectifError.setText("L'objectif ne peut pas d\u00e9passer 1 000 000.");
                valid = false;
            }
        } catch (NumberFormatException e) {
            lblCoffreObjectifError.setText("Montant invalide.");
            valid = false;
        }
        double depot = 0;
        try {
            depot = Double.parseDouble(txtCoffreDepotInitial.getText().trim());
            if (depot < 0) {
                lblCoffreDepotError.setText("Le d\u00e9p\u00f4t ne peut pas \u00eatre n\u00e9gatif.");
                valid = false;
            } else if (depot > objectif) {
                lblCoffreDepotError.setText("Le d\u00e9p\u00f4t ne peut pas d\u00e9passer l'objectif.");
                valid = false;
            }
        } catch (NumberFormatException e) {
            lblCoffreDepotError.setText("Montant invalide.");
            valid = false;
        }
        if (dpCoffreDateCible.getValue() != null && dpCoffreDateCible.getValue().isBefore(LocalDate.now())) {
            lblCoffreDateError.setText("La date cible doit \u00eatre dans le futur.");
            valid = false;
        }
        if (cbCoffreStatut.getValue() == null) {
            lblCoffreStatutError.setText("Veuillez s\u00e9lectionner un statut.");
            valid = false;
        }
        return valid;
    }

    private void clearCoffreErrors() {
        lblCoffreCompteError.setText("");
        lblCoffreNameError.setText("");
        lblCoffreObjectifError.setText("");
        lblCoffreDepotError.setText("");
        lblCoffreDateError.setText("");
        lblCoffreStatutError.setText("");
    }

    // ★ CORRECTION : constructeur CoffreVirtuel à 9 paramètres avec idUser
    @FXML
    private void saveCoffre() {
        if (!validateCoffreForm()) return;

        CompteBancaire selectedAccount = cmbCoffreCompte.getValue();
        int idUser = SessionManager.getInstance().getCurrentUserId();

        String nom          = txtCoffreName.getText().trim();
        double objectif     = parseDouble(txtCoffreObjectif.getText());
        double depotInitial = parseDouble(txtCoffreDepotInitial.getText());
        String dateCible    = (dpCoffreDateCible.getValue() != null)
                ? dpCoffreDateCible.getValue().format(DateTimeFormatter.ISO_DATE) : "";
        String statut       = (cbCoffreStatut.getValue() != null) ? cbCoffreStatut.getValue() : "Actif";
        boolean verrouille  = chkVerrouiller.isSelected();

        if (isEditCoffreMode && selectedCoffre != null) {
            // ★ MODE MODIFICATION : on réutilise l'idCoffre et la dateCreation existants
            selectedCoffre.setNom(nom);
            selectedCoffre.setObjectifMontant(objectif);
            selectedCoffre.setMontantActuel(depotInitial);
            selectedCoffre.setDateObjectifs(dateCible);
            selectedCoffre.setStatus(statut);
            selectedCoffre.setEstVerrouille(verrouille);
            selectedCoffre.setIdCompte(selectedAccount.getIdCompte());
            selectedCoffre.setIdUser(idUser);
            coffreService.edit(selectedCoffre);
            showNotification("Modifié", "Coffre '" + nom + "' mis à jour avec succès.");
        } else {
            // ★ MODE AJOUT
            String dateCreation = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            CoffreVirtuel nouveau = new CoffreVirtuel(
                    nom, objectif, depotInitial, dateCreation, dateCible,
                    statut, verrouille, selectedAccount.getIdCompte(), idUser);
            coffreService.add(nouveau);
            showNotification("Succès", "Coffre '" + nom + "' créé avec succès.");
        }

        handleShowCoffres(selectedAccount);
        updateKPIs();
        hideCoffreForm();
        clearCoffreFormAndResetMode();
    }

    private void clearCoffreForm() {
        if (txtCoffreName != null) txtCoffreName.clear();
        if (txtCoffreObjectif != null) txtCoffreObjectif.clear();
        if (txtCoffreDepotInitial != null) txtCoffreDepotInitial.clear();
        if (dpCoffreDateCible != null) dpCoffreDateCible.setValue(null);
        if (cbCoffreStatut != null) cbCoffreStatut.setValue(null);
        if (chkVerrouiller != null) chkVerrouiller.setSelected(false);
        if (chkDepotAuto != null) chkDepotAuto.setSelected(false);
        if (cmbCoffreCompte != null) cmbCoffreCompte.setValue(null);
    }

    // ★ NOUVELLE : vide le formulaire ET remet le mode en AJOUT
    private void clearCoffreFormAndResetMode() {
        clearCoffreForm();
        selectedCoffre   = null;
        isEditCoffreMode = false;
        // Remettre les labels en mode "Création"
        if (lblCoffreFormTitle    != null) lblCoffreFormTitle.setText("Configuration du coffre virtuel");
        if (lblCoffreFormSubtitle != null) lblCoffreFormSubtitle.setText("Créer un nouvel objectif d epargne");
        if (lblCoffreBtnText      != null) lblCoffreBtnText.setText("Créer le coffre");
    }

    // ─── Actions formulaire Compte ─────────────────────────────────────────────

    @FXML private void addNewAccount() { clearForm(); selectedCompte = null; }

    private boolean validateAccountForm() {
        boolean valid = true;
        clearAccountErrors();
        String numero = txtAccountNumber.getText().trim();
        if (numero.isEmpty()) {
            lblAccountNumberError.setText("Le num\u00e9ro de compte est obligatoire.");
            valid = false;
        } else if (numero.length() < 5) {
            lblAccountNumberError.setText("Le num\u00e9ro doit contenir au moins 5 caract\u00e8res.");
            valid = false;
        }
        try {
            double solde = Double.parseDouble(txtBalance.getText().trim());
            if (solde < 0) { lblBalanceError.setText("Le solde ne peut pas \u00eatre n\u00e9gatif."); valid = false; }
        } catch (NumberFormatException e) { lblBalanceError.setText("Solde invalide."); valid = false; }
        if (dpOpeningDate.getValue() == null) {
            lblOpeningDateError.setText("La date d'ouverture est obligatoire."); valid = false;
        } else if (dpOpeningDate.getValue().isAfter(LocalDate.now())) {
            lblOpeningDateError.setText("La date ne peut pas \u00eatre dans le futur."); valid = false;
        }
        if (cbStatus.getValue() == null) { lblStatusError.setText("Veuillez s\u00e9lectionner un statut."); valid = false; }
        if (cbType.getValue() == null) { lblTypeError.setText("Veuillez s\u00e9lectionner un type de compte."); valid = false; }
        try {
            double plafRet = Double.parseDouble(txtWithdrawLimit.getText().trim());
            if (plafRet < 0) { lblWithdrawLimitError.setText("Le plafond ne peut pas \u00eatre n\u00e9gatif."); valid = false; }
        } catch (NumberFormatException e) { lblWithdrawLimitError.setText("Plafond invalide."); valid = false; }
        try {
            double plafVir = Double.parseDouble(txtTransferLimit.getText().trim());
            if (plafVir < 0) { lblTransferLimitError.setText("Le plafond ne peut pas \u00eatre n\u00e9gatif."); valid = false; }
        } catch (NumberFormatException e) { lblTransferLimitError.setText("Plafond invalide."); valid = false; }
        return valid;
    }

    private void clearAccountErrors() {
        lblAccountNumberError.setText(""); lblBalanceError.setText("");
        lblOpeningDateError.setText("");   lblStatusError.setText("");
        lblTypeError.setText("");          lblWithdrawLimitError.setText("");
        lblTransferLimitError.setText("");
    }

    @FXML
    private void saveAccount() {
        if (!validateAccountForm()) return;
        int idUser = SessionManager.getInstance().getCurrentUserId();
        String numero  = txtAccountNumber.getText().trim();
        double solde   = parseDouble(txtBalance.getText().trim());
        double plafRet = parseDouble(txtWithdrawLimit.getText().trim());
        double plafVir = parseDouble(txtTransferLimit.getText().trim());
        String dateStr = dpOpeningDate.getValue() != null
                ? dpOpeningDate.getValue().format(DateTimeFormatter.ISO_DATE) : "";
        String statut  = cbStatus.getValue();
        String type    = cbType.getValue();
        if (selectedCompte != null) {
            CompteBancaire updated = new CompteBancaire(
                    selectedCompte.getIdCompte(), numero, solde, dateStr, statut, plafRet, plafVir, type, idUser);
            service.edit(updated);
            showNotification("Modifie", "Compte mis a jour avec succes.");
        } else {
            CompteBancaire nouveau = new CompteBancaire(
                    numero, solde, dateStr, statut, plafRet, plafVir, type, idUser);
            service.add(nouveau);
            showNotification("Succes", "Compte enregistre avec succes.");
        }
        clearForm(); selectedCompte = null;
        loadAccountsFromDB();
    }

    @FXML
    private void deleteAccount() {
        if (selectedCompte == null) {
            showNotification("Erreur", "Veuillez selectionner un compte a supprimer."); return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le compte");
        alert.setHeaderText("Voulez-vous vraiment supprimer ce compte ?");
        alert.setContentText("Cette action est irreversible.");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                service.remove(selectedCompte);
                clearForm(); selectedCompte = null;
                loadAccountsFromDB();
                showNotification("Supprime", "Le compte a ete supprime.");
            }
        });
    }

    @FXML
    private void clearForm() {
        if (txtAccountNumber != null) txtAccountNumber.clear();
        if (txtBalance != null)       txtBalance.clear();
        if (dpOpeningDate != null)    dpOpeningDate.setValue(null);
        if (cbStatus != null)         cbStatus.getSelectionModel().clearSelection();
        if (cbType != null)           cbType.getSelectionModel().clearSelection();
        if (txtWithdrawLimit != null) txtWithdrawLimit.clear();
        if (txtTransferLimit != null) txtTransferLimit.clear();
        selectedCompte = null;
    }

    @FXML
    private void refreshData() {
        loadAccountsFromDB();
        showNotification("Actualise", "Les donnees ont ete actualisees.");
    }

    @FXML
    private void selectAccount(MouseEvent event) { /* géré par populateForm via click sur carte */ }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void populateForm(CompteBancaire c) {
        selectedCompte = c;
        if (txtAccountNumber != null) txtAccountNumber.setText(c.getNumeroCompte());
        if (txtBalance != null)       txtBalance.setText(String.valueOf(c.getSolde()));
        if (txtWithdrawLimit != null) txtWithdrawLimit.setText(String.valueOf(c.getPlafondRetrait()));
        if (txtTransferLimit != null) txtTransferLimit.setText(String.valueOf(c.getPlafondVirement()));
        if (cbStatus != null)         cbStatus.setValue(c.getStatutCompte());
        if (cbType != null)           cbType.setValue(c.getTypeCompte());
        if (dpOpeningDate != null) {
            try {
                dpOpeningDate.setValue((c.getDateOuverture() != null && !c.getDateOuverture().isEmpty())
                        ? LocalDate.parse(c.getDateOuverture()) : null);
            } catch (Exception ex) { dpOpeningDate.setValue(null); }
        }
    }

    private boolean validateForm() {
        if (txtAccountNumber == null || txtAccountNumber.getText().trim().isEmpty()) {
            showNotification("Erreur", "Veuillez saisir un numero de compte."); return false; }
        try { Double.parseDouble(txtBalance.getText().trim()); }
        catch (NumberFormatException e) { showNotification("Erreur", "Solde invalide."); return false; }
        if (dpOpeningDate.getValue() == null) {
            showNotification("Erreur", "Date d ouverture obligatoire."); return false; }
        if (cbStatus.getValue() == null) {
            showNotification("Erreur", "Statut obligatoire."); return false; }
        if (cbType.getValue() == null) {
            showNotification("Erreur", "Type de compte obligatoire."); return false; }
        return true;
    }

    private double parseDouble(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
    }

    // ─── Animations ───────────────────────────────────────────────────────────

    private void animateSlideIn(VBox node) {
        if (node == null) return;
        node.setOpacity(0); node.setTranslateY(-20);
        FadeTransition      fade  = new FadeTransition(ANIMATION_DURATION, node);
        fade.setFromValue(0); fade.setToValue(1); fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(ANIMATION_DURATION, node);
        slide.setFromY(-20); slide.setToY(0); slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    private void animateSlideOut(VBox node, Runnable onFinished) {
        if (node == null) { if (onFinished != null) onFinished.run(); return; }
        FadeTransition      fade  = new FadeTransition(Duration.millis(150), node);
        fade.setFromValue(1); fade.setToValue(0); fade.setInterpolator(Interpolator.EASE_IN);
        TranslateTransition slide = new TranslateTransition(Duration.millis(150), node);
        slide.setFromY(0); slide.setToY(-10); slide.setInterpolator(Interpolator.EASE_IN);
        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setOnFinished(e -> { if (onFinished != null) onFinished.run(); });
        pt.play();
    }

    private void setVisibleManaged(VBox node, boolean visible) {
        if (node != null) { node.setVisible(visible); node.setManaged(visible); }
    }

    private void showNotification(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.show();
    }
}