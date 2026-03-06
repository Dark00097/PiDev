package com.nexora.bank.controllers;

import com.nexora.bank.Models.CoffreVirtuel;
import com.nexora.bank.Models.CompteBancaire;
import com.nexora.bank.Models.Credit;
import com.nexora.bank.Models.DragonState;
import com.nexora.bank.Service.*;
import com.nexora.bank.Utils.SessionManager;
import com.nexora.bank.Models.HistoryEntry;
import com.nexora.bank.Models.HistoryEntry.ActionType;
import com.nexora.bank.Service.TranslationService.Language;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nexora.bank.Utils.MyDB;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.nexora.bank.Service.RoueFortuneEligibilityService.EligibilityResult;
import com.nexora.bank.Service.RoueFortuneService.TirageResult;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

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

    // ─── Labels d'erreur formulaire Compte ───────────────────────────────────
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

    // ─── Labels d'erreur formulaire Coffre ───────────────────────────────────
    @FXML private Label lblCoffreCompteError;
    @FXML private Label lblCoffreNameError;
    @FXML private Label lblCoffreObjectifError;
    @FXML private Label lblCoffreDepotError;
    @FXML private Label lblCoffreDateError;
    @FXML private Label lblCoffreStatutError;
    @FXML private Label lblCoffreFormTitle;
    @FXML private Label lblCoffreFormSubtitle;
    @FXML private Label lblCoffreBtnText;
    @FXML private VBox  aiSuggestionsSectionRight;
    @FXML private FlowPane aiSuggestionsFlowRight;

    // ─── Labels traduisibles ─────────────────────────────────────────────────
    @FXML private Label lblSectionTitle;
    @FXML private Label lblSectionSubtitle;
    @FXML private Label lblBtnRefresh;
    @FXML private Label lblBtnLanguage;
    @FXML private Label lblBtnNewVault;
    @FXML private Label lblKpiBalanceTitle;
    @FXML private Label lblKpiBalanceDesc;
    @FXML private Label lblKpiAccountsTitle;
    @FXML private Label lblKpiAccountsDesc;
    @FXML private Label lblKpiAccountsUnit;
    @FXML private Label lblKpiVaultsTitle;
    @FXML private Label lblKpiVaultsDesc;
    @FXML private Label lblKpiVaultsUnit;
    @FXML private Label lblKpiHealthTitle;
    @FXML private Label lblKpiHealthDesc;
    @FXML private Label lblKpiHealthValue;
    @FXML private Label lblCardDetailsTitle;
    @FXML private Label lblCardDetailsSubtitle;
    @FXML private Label lblFormAccountNumber;
    @FXML private Label lblFormBalance;
    @FXML private Label lblFormDate;
    @FXML private Label lblFormStatus;
    @FXML private Label lblFormType;
    @FXML private Label lblFormWithdraw;
    @FXML private Label lblFormTransfer;
    @FXML private Label lblBtnClear;
    @FXML private Label lblBtnDelete;
    @FXML private Label lblBtnSave;
    @FXML private Label lblBtnCancel;
    @FXML private Label lblCardAccountsTitle;
    @FXML private Label lblCardAccountsSubtitle;
    @FXML private Label lblVaultsTitle;
    @FXML private Label lblVaultsSubtitle;
    @FXML private Label lblBtnCloseVaults;
    @FXML private javafx.scene.control.Button btnLanguage;
    @FXML private javafx.scene.control.Button btnHistory;
    @FXML private javafx.scene.control.Button btnRoueFortune;

    @FXML private javafx.scene.layout.HBox         hboxRoueBarre;
    @FXML private javafx.scene.layout.StackPane    barreProgression;
    @FXML private Label                            lblHeaderPoints;
    @FXML private Label                            lblHeaderPct;

    @FXML private javafx.scene.layout.HBox   voiceBanner;
    @FXML private javafx.scene.layout.StackPane voiceMicPane;
    @FXML private org.kordamp.ikonli.javafx.FontIcon voiceMicIcon;
    @FXML private Label lblVoiceTitle;
    @FXML private Label lblVoiceSubtitle;
    @FXML private Label lblVoiceStatus;

    private final SpeechToTextService speechService = new SpeechToTextService();

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
    private final CreditService         creditService = new CreditService();
    private final DragonService         dragonService = new DragonService();
    private final RevenueService revenueService = new RevenueService();

    // ─── État local ───────────────────────────────────────────────────────────
    private ObservableList<CompteBancaire> allAccounts = FXCollections.observableArrayList();
    private CompteBancaire selectedCompte       = null;
    private CompteBancaire compteAfficheCoffres = null;
    private String         activeFilter         = "All";
    private CoffreVirtuel  selectedCoffre       = null;
    private boolean        isEditCoffreMode     = false;

    @FXML private VBox surplusSuggestionSection;

    private static final Duration ANIM_DUR = Duration.millis(250);

    private Map<String, Object> surplusData;

    private Timeline pollingTimeline;

    // ─── Initialisation ───────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        setVisibleManaged(coffreFormContainer, false);
        setVisibleManaged(coffresDisplayContainer, false);
        setupSearchListener();
        setupAccountNumberFormat();
        loadAccountsFromDB();
        checkCoffreAlerts();            // ★ NOUVEAU — Vérification alertes coffres
        initializeCoffreCompte();
        startPolling();

        int uid = SessionManager.getInstance().getCurrentUserId();
        if (uid > 0) {
            HistoryManager.getInstance().loadForUser(uid);
            System.out.println("[History] Historique chargé pour userId=" + uid);
        }
        new Thread(this::mettreAJourBarreHeader, "Roue-Header").start();
        checkAndShowSurplusSuggestion();
    }

    private void setupSearchListener() {
        if (txtSearch != null)
            txtSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        setupAccountNumberListener();
    }

    private void setupAccountNumberListener() {
        if (txtAccountNumber == null) return;
        txtAccountNumber.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                String val = txtAccountNumber.getText().trim();
                if (!val.isEmpty() && !val.matches("CB-\\d{3}")) {
                    String formatted = formatAccountNumber(val);
                    txtAccountNumber.setText(formatted);
                }
            }
        });
        txtAccountNumber.textProperty().addListener((obs, oldVal, newVal) -> {
            if (lblAccountNumberError == null) return;
            if (newVal.isEmpty()) {
                lblAccountNumberError.setText("");
            } else if (newVal.matches("CB-\\d{3}")) {
                lblAccountNumberError.setStyle("-fx-text-fill: #22C55E;");
                lblAccountNumberError.setText("✓ Format valide");
            } else {
                lblAccountNumberError.setStyle("-fx-text-fill: #F97316;");
                lblAccountNumberError.setText("Format : CB-XXX (3 chiffres)");
            }
        });
    }

    private void setupAccountNumberFormat() {
        if (txtAccountNumber == null) return;
        txtAccountNumber.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            if (newVal.isEmpty()) return;
            if (!newVal.startsWith("CB-")) {
                String digits = newVal.replaceAll("[^0-9]", "");
                String formatted = "CB-" + (digits.length() > 3 ? digits.substring(0, 3) : digits);
                txtAccountNumber.setText(formatted);
                txtAccountNumber.positionCaret(formatted.length());
                return;
            }
            String afterPrefix = newVal.substring(3);
            String digitsOnly = afterPrefix.replaceAll("[^0-9]", "");
            if (digitsOnly.length() > 3) digitsOnly = digitsOnly.substring(0, 3);
            String formatted = "CB-" + digitsOnly;
            if (!formatted.equals(newVal)) {
                txtAccountNumber.setText(formatted);
                txtAccountNumber.positionCaret(formatted.length());
            }
        });
        txtAccountNumber.setPromptText("CB-123");
        txtAccountNumber.setTooltip(new Tooltip("Format : CB- suivi de 3 chiffres"));
    }

    // ─── Traduction multilingue ───────────────────────────────────────────────

    @FXML
    private void handleLanguage() {
        javafx.scene.control.ContextMenu langMenu = new javafx.scene.control.ContextMenu();
        langMenu.setStyle("-fx-background-color: white; -fx-border-color: #E5E7EB; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.15),12,0,0,4);");
        for (Language lang : Language.values()) {
            javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(lang.flag + "  " + lang.label);
            item.setStyle("-fx-font-size: 13px; -fx-padding: 8 16;"
                    + (lang == TranslationService.getCurrentLanguage() ? " -fx-font-weight:bold; -fx-text-fill:#0D9488;" : ""));
            item.setOnAction(e -> changeLanguage(lang));
            langMenu.getItems().add(item);
        }
        if (btnLanguage != null)
            langMenu.show(btnLanguage, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    // ════════════════════════════════════════════════════════════════
    // SAISIE VOCALE
    // ════════════════════════════════════════════════════════════════

    @FXML
    private void handleVoiceInput() {
        if (speechService.isRecording()) {
            stopVoiceRecording();
        } else {
            startVoiceRecording();
        }
    }

    private void startVoiceRecording() {
        try {
            speechService.startRecording();
            if (voiceMicIcon != null)  voiceMicIcon.setIconLiteral("fas-stop-circle");
            if (voiceMicPane != null)  voiceMicPane.setStyle("-fx-background-color: #EF4444; -fx-padding: 14 20;");
            if (voiceBanner != null)   voiceBanner.setStyle("-fx-background-color: linear-gradient(to right, #DC2626, #EF4444);-fx-padding: 0; -fx-cursor: hand;");
            if (lblVoiceStatus != null) lblVoiceStatus.setText("⏺ En cours...");
            if (lblVoiceTitle != null)  lblVoiceTitle.setText("Enregistrement en cours...");
            if (lblVoiceSubtitle != null) lblVoiceSubtitle.setText("Parlez maintenant — cliquez à nouveau pour arrêter");
            FadeTransition pulse = new FadeTransition(Duration.millis(600), voiceMicIcon);
            pulse.setFromValue(1.0); pulse.setToValue(0.3);
            pulse.setCycleCount(Animation.INDEFINITE); pulse.setAutoReverse(true);
            pulse.play();
            if (voiceMicIcon != null) voiceMicIcon.setUserData(pulse);
        } catch (Exception e) {
            showVoiceError("Impossible d'accéder au microphone : " + e.getMessage());
        }
    }

    private void stopVoiceRecording() {
        if (voiceMicIcon != null && voiceMicIcon.getUserData() instanceof FadeTransition) {
            ((FadeTransition) voiceMicIcon.getUserData()).stop();
            voiceMicIcon.setOpacity(1.0);
        }
        if (voiceMicIcon != null)  voiceMicIcon.setIconLiteral("fas-spinner");
        if (voiceBanner != null)   voiceBanner.setStyle("-fx-background-color: linear-gradient(to right, #7C3AED, #6D28D9);-fx-padding: 0; -fx-cursor: hand;");
        if (lblVoiceStatus != null) lblVoiceStatus.setText("⏳ Analyse...");
        if (lblVoiceTitle != null)  lblVoiceTitle.setText("Analyse en cours...");
        if (lblVoiceSubtitle != null) lblVoiceSubtitle.setText("Transcription et extraction des données...");
        java.io.File audioFile = speechService.stopRecording();
        new Thread(() -> {
            try {
                javafx.application.Platform.runLater(() -> {
                    if (lblVoiceSubtitle != null) lblVoiceSubtitle.setText("Transcription vocale en cours...");
                });
                String transcription = speechService.transcribe(audioFile);
                System.out.println("[Voice] Transcription: " + transcription);
                if (transcription.isEmpty()) {
                    javafx.application.Platform.runLater(() ->
                            showVoiceError("Aucun texte reconnu. Veuillez réessayer en parlant plus clairement."));
                    return;
                }
                javafx.application.Platform.runLater(() -> {
                    if (lblVoiceSubtitle != null) lblVoiceSubtitle.setText("Extraction des informations...");
                });
                SpeechToTextService.FormData formData = speechService.parseWithAI(transcription);
                javafx.application.Platform.runLater(() -> fillFormAndConfirm(formData));
            } catch (Exception e) {
                System.err.println("[Voice] Erreur: " + e.getMessage());
                javafx.application.Platform.runLater(() ->
                        showVoiceError("Erreur lors de la reconnaissance vocale : " + e.getMessage()));
            } finally {
                if (audioFile != null) audioFile.delete();
            }
        }, "Voice-Processing").start();
    }

    private void fillFormAndConfirm(SpeechToTextService.FormData fd) {
        if (fd.error != null && !fd.error.isEmpty()) {
            showVoiceError(fd.error);
            return;
        }
        int filled = 0;
        if (txtAccountNumber != null && fd.numeroCompte != null && !fd.numeroCompte.isEmpty()) {
            String num = formatAccountNumber(fd.numeroCompte);
            txtAccountNumber.setText(num);
            filled++;
        }
        if (txtBalance != null && fd.solde != null && !fd.solde.isEmpty()) {
            txtBalance.setText(fd.solde);
            filled++;
        }
        if (dpOpeningDate != null && fd.dateOuverture != null && !fd.dateOuverture.isEmpty()) {
            try { dpOpeningDate.setValue(java.time.LocalDate.parse(fd.dateOuverture)); filled++; }
            catch (Exception ex) { }
        }
        if (cbType != null && fd.typeCompte != null && !fd.typeCompte.isEmpty()) {
            cbType.setValue(fd.typeCompte);
            filled++;
        }
        if (cbStatus != null && fd.statut != null && !fd.statut.isEmpty()) {
            cbStatus.setValue(fd.statut);
            filled++;
        }
        if (txtWithdrawLimit != null && fd.plafondRetrait != null && !fd.plafondRetrait.isEmpty()) {
            txtWithdrawLimit.setText(fd.plafondRetrait);
            filled++;
        }
        if (txtTransferLimit != null && fd.plafondVirement != null && !fd.plafondVirement.isEmpty()) {
            txtTransferLimit.setText(fd.plafondVirement);
            filled++;
        }
        if (voiceBanner != null) voiceBanner.setStyle("-fx-background-color: linear-gradient(to right, #059669, #10B981);-fx-padding: 0; -fx-cursor: hand;");
        if (voiceMicIcon != null) voiceMicIcon.setIconLiteral("fas-check-circle");
        if (lblVoiceTitle != null)
            lblVoiceTitle.setText("✅ " + filled + " champ" + (filled > 1 ? "s remplis" : " rempli") + " automatiquement !");
        if (lblVoiceSubtitle != null)
            lblVoiceSubtitle.setText("Transcription : " + (fd.rawText != null ? fd.rawText : ""));
        if (lblVoiceStatus != null) lblVoiceStatus.setText("🎤 Prêt");
        javafx.animation.PauseTransition _pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(4));
        _pause.setOnFinished(e -> resetVoiceBanner());
        _pause.play();
    }

    private String formatAccountNumber(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String cleaned = raw.trim().toUpperCase();
        if (cleaned.startsWith("CB-")) cleaned = cleaned.substring(3);
        String digitsOnly = cleaned.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) return "CB-" + cleaned.substring(0, Math.min(3, cleaned.length()));
        if (digitsOnly.length() < 3) digitsOnly = String.format("%03d", Integer.parseInt(digitsOnly));
        else if (digitsOnly.length() > 3) digitsOnly = digitsOnly.substring(0, 3);
        return "CB-" + digitsOnly;
    }

    private void showVoiceError(String msg) {
        resetVoiceBanner();
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle("Saisie vocale"); alert.setHeaderText(null); alert.setContentText(msg);
        alert.show();
    }

    private void resetVoiceBanner() {
        if (voiceMicIcon != null) voiceMicIcon.setIconLiteral("fas-microphone");
        if (voiceMicIcon != null) voiceMicIcon.setOpacity(1.0);
        if (voiceMicPane != null) voiceMicPane.setStyle("-fx-background-color: rgba(0,0,0,0.15); -fx-padding: 14 20;");
        if (voiceBanner  != null) voiceBanner.setStyle("-fx-background-color: linear-gradient(to right, #0D9488, #0891B2); -fx-padding: 0; -fx-cursor: hand;");
        if (lblVoiceStatus   != null) lblVoiceStatus.setText("🎤 Prêt");
        if (lblVoiceTitle    != null) lblVoiceTitle.setText("Saisie vocale intelligente");
        if (lblVoiceSubtitle != null) lblVoiceSubtitle.setText("Cliquez et dictez vos informations — le formulaire se remplit automatiquement");
    }

    @FXML
    private void handleHistory() {
        showHistoryWindow();
    }

    private void showHistoryWindow() {
        Stage stage = new Stage();
        stage.setTitle("Historique des actions");
        stage.initModality(Modality.NONE);
        stage.setResizable(true);
        stage.setWidth(760);
        stage.setHeight(580);
        org.kordamp.ikonli.javafx.FontIcon histIco = new org.kordamp.ikonli.javafx.FontIcon("fas-history");
        histIco.setStyle("-fx-fill: #0D9488; -fx-icon-size: 20;");
        Label titleLbl = new Label("Historique des actions");
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0A2540;");
        final Label countLbl = new Label();
        countLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
        Region titleSpacer = new Region(); HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        Button clearAllBtn = new Button("Tout effacer");
        clearAllBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444;-fx-border-color: #EF4444; -fx-border-radius: 8; -fx-background-radius: 8;-fx-padding: 6 14; -fx-font-size: 12px; -fx-cursor: hand;");
        HBox titleBar = new HBox(10, histIco, titleLbl, titleSpacer, countLbl, clearAllBtn);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setStyle("-fx-padding: 16 24 12 24; -fx-background-color: white;");
        org.kordamp.ikonli.javafx.FontIcon searchIco = new org.kordamp.ikonli.javafx.FontIcon("fas-search");
        searchIco.setStyle("-fx-fill: #9CA3AF; -fx-icon-size: 14;");
        javafx.scene.control.TextField searchField = new javafx.scene.control.TextField();
        searchField.setPromptText("Rechercher par action, date, heure...");
        searchField.setStyle("-fx-background-color: white; -fx-border-color: #E5E7EB;-fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 8 16;-fx-font-size: 13px; -fx-pref-width: 340;");
        HBox searchBar = new HBox(8, searchIco, searchField);
        searchBar.setAlignment(Pos.CENTER);
        searchBar.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #E5E7EB;-fx-border-width: 0 0 1 0; -fx-padding: 14 24;");
        final VBox listContainer = new VBox(0);
        listContainer.setStyle("-fx-background-color: #F9FAFB;");
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #F9FAFB; -fx-background: #F9FAFB;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        refreshHistoryList(listContainer, countLbl, searchField.getText());
        searchField.textProperty().addListener((obs, oldVal, newVal) ->
                refreshHistoryList(listContainer, countLbl, newVal));
        clearAllBtn.setOnAction(e -> {
            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Effacer l'historique");
            confirm.setHeaderText("Effacer tout l'historique ?");
            confirm.setContentText("Cette action est irréversible.");
            confirm.showAndWait().ifPresent(r -> {
                if (r == javafx.scene.control.ButtonType.OK) {
                    HistoryManager.getInstance().clear();
                    refreshHistoryList(listContainer, countLbl, searchField.getText());
                }
            });
        });
        VBox root = new VBox(0, titleBar, searchBar, scrollPane);
        root.setStyle("-fx-background-color: #F9FAFB;");
        Scene scene = new Scene(root, 760, 580);
        stage.setScene(scene);
        stage.show();
    }

    private void refreshHistoryList(VBox listContainer, Label countLbl, String keyword) {
        listContainer.getChildren().clear();
        java.util.List<HistoryEntry> list = HistoryManager.getInstance().search(keyword);
        countLbl.setText(list.size() + " action" + (list.size() > 1 ? "s" : ""));
        if (list.isEmpty()) {
            Label emptyLbl = new Label("Aucune action enregistrée");
            emptyLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #9CA3AF; -fx-padding: 40;");
            VBox emptyBox = new VBox(emptyLbl);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPrefHeight(200);
            listContainer.getChildren().add(emptyBox);
            return;
        }
        for (HistoryEntry entry : list) {
            listContainer.getChildren().add(buildHistoryRow(entry, listContainer, countLbl, keyword));
        }
    }

    private HBox buildHistoryRow(HistoryEntry entry, VBox listContainer, Label countLbl, String keyword) {
        org.kordamp.ikonli.javafx.FontIcon entryIco = new org.kordamp.ikonli.javafx.FontIcon(entry.getType().icon);
        entryIco.setStyle("-fx-fill: " + entry.getType().color + "; -fx-icon-size: 16;");
        StackPane iconCircle = new StackPane(entryIco);
        iconCircle.setStyle("-fx-background-color: " + entry.getType().color + "1A;-fx-background-radius: 50%; -fx-min-width: 38; -fx-min-height: 38;-fx-max-width: 38; -fx-max-height: 38;");
        Label typeBadge = new Label(entry.getType().label);
        typeBadge.setStyle("-fx-background-color: " + entry.getType().color + "22;-fx-text-fill: " + entry.getType().color + "; -fx-font-size: 10px;-fx-font-weight: 700; -fx-background-radius: 6; -fx-padding: 2 8;");
        Label descLbl = new Label(entry.getDescription());
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #374151;");
        descLbl.setMaxWidth(420);
        VBox textBox = new VBox(4, typeBadge, descLbl);
        org.kordamp.ikonli.javafx.FontIcon clockIco = new org.kordamp.ikonli.javafx.FontIcon("fas-clock");
        clockIco.setStyle("-fx-fill: #9CA3AF; -fx-icon-size: 11;");
        Label dateLbl = new Label(entry.getFormattedDate());
        dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
        HBox dateBox = new HBox(4, clockIco, dateLbl);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        Region rowSpacer = new Region(); HBox.setHgrow(rowSpacer, Priority.ALWAYS);
        org.kordamp.ikonli.javafx.FontIcon trashIco = new org.kordamp.ikonli.javafx.FontIcon("fas-times");
        trashIco.setStyle("-fx-fill: #9CA3AF; -fx-icon-size: 12;");
        Button deleteBtn = new Button();
        deleteBtn.setGraphic(trashIco);
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 6;");
        deleteBtn.setOnMouseEntered(ev -> trashIco.setStyle("-fx-fill: #EF4444; -fx-icon-size: 12;"));
        deleteBtn.setOnMouseExited(ev  -> trashIco.setStyle("-fx-fill: #9CA3AF; -fx-icon-size: 12;"));
        VBox rightBox = new VBox(4, dateBox);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        String normalStyle = "-fx-background-color: white; -fx-padding: 14 20;-fx-border-color: transparent transparent #F3F4F6 transparent; -fx-border-width: 0 0 1 0;";
        String hoverStyle  = "-fx-background-color: #F0FDF4; -fx-padding: 14 20;-fx-border-color: transparent transparent #F3F4F6 transparent; -fx-border-width: 0 0 1 0;";
        HBox row = new HBox(12, iconCircle, textBox, rowSpacer, rightBox, deleteBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(normalStyle);
        row.setOnMouseEntered(ev -> row.setStyle(hoverStyle));
        row.setOnMouseExited(ev  -> row.setStyle(normalStyle));
        row.setOpacity(0); row.setTranslateX(-10);
        FadeTransition ft = new FadeTransition(Duration.millis(180), row);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(180), row);
        tt.setFromX(-10); tt.setToX(0);
        new ParallelTransition(ft, tt).play();
        deleteBtn.setOnAction(ev -> {
            HistoryManager.getInstance().remove(entry.getId());
            refreshHistoryList(listContainer, countLbl, keyword);
        });
        return row;
    }

    private void changeLanguage(Language lang) {
        if (lang == TranslationService.getCurrentLanguage()) return;
        TranslationService.setCurrentLanguage(lang);
        if (lblBtnLanguage != null) lblBtnLanguage.setText("⏳ " + lang.flag);
        if (lang == Language.FRENCH) { applyTranslations(TranslationService.getCached(lang)); return; }
        TranslationService.translateAll(lang, () -> applyTranslations(TranslationService.getCached(lang)));
    }

    private void applyTranslations(java.util.Map<String, String> t) {
        if (t == null) return;
        Language lang = TranslationService.getCurrentLanguage();
        java.util.function.BiConsumer<Label, String> set = (lbl, key) -> {
            if (lbl != null && t.containsKey(key)) lbl.setText(t.get(key));
        };
        set.accept(lblSectionTitle,        "section_title");
        set.accept(lblSectionSubtitle,     "section_subtitle");
        set.accept(lblBtnRefresh,          "btn_refresh");
        set.accept(lblBtnNewVault,         "btn_new_vault");
        if (lblBtnLanguage != null) lblBtnLanguage.setText(lang.flag + " " + lang.label);
        set.accept(lblKpiBalanceTitle,     "kpi_balance_title");
        set.accept(lblKpiBalanceDesc,      "kpi_balance_desc");
        set.accept(lblKpiAccountsTitle,    "kpi_accounts_title");
        set.accept(lblKpiAccountsDesc,     "kpi_accounts_desc");
        set.accept(lblKpiAccountsUnit,     "kpi_accounts_unit");
        set.accept(lblKpiVaultsTitle,      "kpi_vaults_title");
        set.accept(lblKpiVaultsDesc,       "kpi_vaults_desc");
        set.accept(lblKpiVaultsUnit,       "kpi_vaults_unit");
        set.accept(lblKpiHealthTitle,      "kpi_health_title");
        set.accept(lblKpiHealthDesc,       "kpi_health_desc");
        set.accept(lblKpiHealthValue,      "kpi_health_value");
        set.accept(lblCardDetailsTitle,    "card_details_title");
        set.accept(lblCardDetailsSubtitle, "card_details_subtitle");
        set.accept(lblFormAccountNumber,   "form_account_number");
        set.accept(lblFormBalance,         "form_balance");
        set.accept(lblFormDate,            "form_opening_date");
        set.accept(lblFormStatus,          "form_status");
        set.accept(lblFormType,            "form_type");
        set.accept(lblFormWithdraw,        "form_withdraw");
        set.accept(lblFormTransfer,        "form_transfer");
        set.accept(lblBtnClear,            "btn_clear");
        set.accept(lblBtnDelete,           "btn_delete");
        set.accept(lblBtnSave,             "btn_save");
        set.accept(lblBtnCancel,           "btn_cancel");
        set.accept(lblCardAccountsTitle,    "card_accounts_title");
        set.accept(lblCardAccountsSubtitle, "card_accounts_subtitle");
        if (txtSearch != null && t.containsKey("search_prompt")) txtSearch.setPromptText(t.get("search_prompt"));
        if (btnFilterAll != null && t.containsKey("filter_all")) btnFilterAll.setText(t.get("filter_all"));
        if (btnFilterCourant != null && t.containsKey("filter_current")) btnFilterCourant.setText(t.get("filter_current"));
        if (btnFilterEpargne != null && t.containsKey("filter_savings")) btnFilterEpargne.setText(t.get("filter_savings"));
        if (btnFilterProfessionnel != null && t.containsKey("filter_pro")) btnFilterProfessionnel.setText(t.get("filter_pro"));
        set.accept(lblVaultsTitle,         "vaults_title");
        set.accept(lblVaultsSubtitle,      "vaults_subtitle");
        set.accept(lblBtnCloseVaults,      "btn_close");
        javafx.scene.Node root = lblSectionTitle != null && lblSectionTitle.getScene() != null
                ? lblSectionTitle.getScene().getRoot() : null;
        if (root != null) root.setNodeOrientation(lang == Language.ARABIC
                ? javafx.geometry.NodeOrientation.RIGHT_TO_LEFT
                : javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
    }

    private void startPolling() {
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            int idUser = SessionManager.getInstance().getCurrentUserId();
            List<CompteBancaire> fresh = (idUser > 0)
                    ? service.getByUser(idUser) : service.getAll();
            Set<Integer> currentIds = new HashSet<>();
            for (CompteBancaire c : allAccounts) currentIds.add(c.getIdCompte());
            Set<Integer> freshIds = new HashSet<>();
            for (CompteBancaire c : fresh) freshIds.add(c.getIdCompte());
            boolean hasChanges = false;
            if (!currentIds.equals(freshIds)) {
                hasChanges = true;
            } else {
                for (CompteBancaire freshAccount : fresh) {
                    for (CompteBancaire currentAccount : allAccounts) {
                        if (freshAccount.getIdCompte() == currentAccount.getIdCompte()) {
                            if (!freshAccount.getStatutCompte().equalsIgnoreCase(currentAccount.getStatutCompte())) {
                                hasChanges = true;
                            }
                            break;
                        }
                    }
                }
            }
            if (hasChanges) {
                allAccounts.setAll(fresh);
                updateKPIs();
                applyFilter();
            }
        }));
        pollingTimeline.setCycleCount(Animation.INDEFINITE);
        pollingTimeline.play();
    }

    public void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
            pollingTimeline = null;
        }
    }

    private void initializeCoffreCompte() {
        cmbCoffreCompte.setItems(allAccounts);
        cmbCoffreCompte.setCellFactory(p -> new ListCell<CompteBancaire>() {
            @Override protected void updateItem(CompteBancaire c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getNumeroCompte() + " - " + c.getTypeCompte());
            }
        });
        cmbCoffreCompte.setButtonCell(new ListCell<CompteBancaire>() {
            @Override protected void updateItem(CompteBancaire c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getNumeroCompte() + " - " + c.getTypeCompte());
            }
        });
    }

    // ─── Chargement BD ────────────────────────────────────────────────────────

    private void loadAccountsFromDB() {
        int idUser = SessionManager.getInstance().getCurrentUserId();
        List<CompteBancaire> list = (idUser > 0) ? service.getByUser(idUser) : service.getAll();
        allAccounts.setAll(list);
        updateKPIs();
        renderAccountCards(allAccounts);
    }

    private void updateKPIs() {
        if (lblTotalBalance != null) {
            double total = allAccounts.stream()
                    .filter(c -> !"En attente".equalsIgnoreCase(c.getStatutCompte()))
                    .mapToDouble(CompteBancaire::getSolde).sum();
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
            long total = allAccounts.stream()
                    .mapToLong(c -> coffreService.getByCompte(c.getIdCompte()).size())
                    .sum();
            lblVaultCount.setText(String.valueOf(total));
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
        String search = txtSearch != null ? txtSearch.getText().toLowerCase() : "";
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

    // ─── Grille de comptes ────────────────────────────────────────────────────

    private void renderAccountCards(List<CompteBancaire> comptes) {
        if (comptesGrid == null) return;
        comptesGrid.getChildren().clear();
        for (CompteBancaire c : comptes) {
            boolean isPending = "En attente".equalsIgnoreCase(c.getStatutCompte());
            comptesGrid.getChildren().add(isPending ? buildPendingAccountCard(c) : buildAccountCard(c));
        }
        comptesGrid.getChildren().add(buildAddCard());
    }

    private VBox buildPendingAccountCard(CompteBancaire compte) {
        String type = compte.getTypeCompte() != null ? compte.getTypeCompte() : "Courant";
        String headerStyle, typeLabel, iconLiteral;
        switch (type) {
            case "Epargne":
                headerStyle = "account-card-header-savings";
                typeLabel   = "COMPTE EPARGNE";
                iconLiteral = "fas-piggy-bank"; break;
            case "Professionnel":
                headerStyle = "account-card-header-business";
                typeLabel   = "COMPTE PROFESSIONNEL";
                iconLiteral = "fas-briefcase"; break;
            default:
                headerStyle = "account-card-header-current";
                typeLabel   = "COMPTE COURANT";
                iconLiteral = "fas-credit-card"; break;
        }
        String numero = compte.getNumeroCompte() != null ? compte.getNumeroCompte() : "";
        String last4  = numero.length() >= 4 ? numero.substring(numero.length() - 4) : numero;
        FontIcon typeIcon = new FontIcon(iconLiteral); typeIcon.getStyleClass().add("account-type-icon");
        StackPane typeBadge = new StackPane(typeIcon); typeBadge.getStyleClass().add("account-type-badge");
        Label typeLbl = new Label(typeLabel); typeLbl.getStyleClass().add("account-type-label");
        Label pendingBadgeLbl = new Label("⏳ En attente");
        pendingBadgeLbl.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E;-fx-font-size: 10px; -fx-font-weight: 700;-fx-background-radius: 6; -fx-padding: 2 8;");
        Region headerSpacer = new Region(); HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(10, typeBadge, typeLbl, headerSpacer, pendingBadgeLbl);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().addAll("account-card-header", headerStyle);
        Label maskedLbl  = new Label("---- ---- ---- ");
        maskedLbl.getStyleClass().add("account-number-masked");
        Label visibleLbl = new Label(last4);
        visibleLbl.getStyleClass().add("account-number-visible");
        HBox numberBox = new HBox(8, maskedLbl, visibleLbl); numberBox.setAlignment(Pos.CENTER_LEFT);
        Label balanceLbl   = new Label("Solde initial");
        balanceLbl.getStyleClass().add("balance-label");
        Label balanceValue = new Label(String.format(Locale.US, "%,.2f DT", compte.getSolde()));
        balanceValue.getStyleClass().add("balance-value");
        VBox balanceBox = new VBox(4, balanceLbl, balanceValue);
        Label dateLbl = new Label("Ouverture : " + (compte.getDateOuverture() != null ? compte.getDateOuverture() : "-"));
        dateLbl.getStyleClass().add("last-activity");
        VBox body = new VBox(12, numberBox, balanceBox, dateLbl);
        body.getStyleClass().add("account-card-body");
        body.setEffect(new GaussianBlur(6.0));
        body.setOpacity(0.45);
        FontIcon clockIco = new FontIcon("fas-clock");
        clockIco.setStyle("-fx-fill: #F59E0B; -fx-icon-size: 34;");
        Label waitTitle = new Label("Validation en cours");
        waitTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #92400E;-fx-wrap-text: true;");
        waitTitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Label waitDesc = new Label("En attente d'acceptation par l'admin");
        waitDesc.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #78350F;-fx-wrap-text: true; -fx-font-style: italic;");
        waitDesc.setWrapText(true);
        waitDesc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        VBox overlayContent = new VBox(8, clockIco, waitTitle, waitDesc);
        overlayContent.setAlignment(Pos.CENTER);
        overlayContent.setPadding(new Insets(12, 8, 12, 8));
        FadeTransition pulse = new FadeTransition(Duration.millis(1400), overlayContent);
        pulse.setFromValue(0.65); pulse.setToValue(1.0);
        pulse.setCycleCount(Animation.INDEFINITE); pulse.setAutoReverse(true);
        pulse.play();
        StackPane overlay = new StackPane(overlayContent);
        overlay.setStyle("-fx-background-color: rgba(255, 251, 235, 0.72);-fx-background-radius: 0 0 10 10;");
        overlay.setAlignment(Pos.CENTER);
        StackPane bodyStack = new StackPane(body, overlay);
        FontIcon lockIco = new FontIcon("fas-lock"); lockIco.getStyleClass().add("account-action-icon");
        Button coffresBtn = new Button(); coffresBtn.setGraphic(new HBox(6, lockIco, new Label("Coffres")));
        coffresBtn.getStyleClass().add("account-action-btn"); coffresBtn.setDisable(true);
        FontIcon histIco = new FontIcon("fas-history"); histIco.getStyleClass().add("account-action-icon");
        Button histBtn = new Button(); histBtn.setGraphic(new HBox(6, histIco, new Label("Historique")));
        histBtn.getStyleClass().add("account-action-btn"); histBtn.setDisable(true);
        HBox footer = new HBox(10, coffresBtn, histBtn); footer.getStyleClass().add("account-card-footer");
        VBox card = new VBox(0, header, bodyStack, footer);
        card.getStyleClass().add("account-card");
        card.setPrefWidth(260);
        card.setOpacity(0.80);
        card.setStyle("-fx-border-color: #F59E0B; -fx-border-width: 2.5;-fx-border-radius: 14; -fx-background-radius: 14;-fx-cursor: default;-fx-effect: dropshadow(gaussian, rgba(245,158,11,0.25), 10, 0, 0, 3);");
        card.setOpacity(0); card.setTranslateY(12);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), card);
        fadeIn.setFromValue(0); fadeIn.setToValue(0.80);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(500), card);
        slideIn.setFromY(12); slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fadeIn, slideIn).play();
        return card;
    }

    private VBox buildAccountCard(CompteBancaire compte) {
        String type = compte.getTypeCompte() != null ? compte.getTypeCompte() : "Courant";
        String headerStyle, typeLabel, typeTag, iconLiteral;
        switch (type) {
            case "Epargne":
                headerStyle = "account-card-header-savings";  typeLabel = "COMPTE EPARGNE";
                typeTag = "Haut rendement"; iconLiteral = "fas-piggy-bank"; break;
            case "Professionnel":
                headerStyle = "account-card-header-business"; typeLabel = "COMPTE PROFESSIONNEL";
                typeTag = "Entreprise"; iconLiteral = "fas-briefcase"; break;
            default:
                headerStyle = "account-card-header-current";  typeLabel = "COMPTE COURANT";
                typeTag = "Principal"; iconLiteral = "fas-credit-card"; break;
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
        Button histBtn = new Button(); histBtn.setGraphic(histHBox); histBtn.getStyleClass().add("account-action-btn");
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
        Label lbl    = new Label("Lier un nouveau compte");            lbl.getStyleClass().add("add-account-label");
        Label subLbl = new Label("Connecter un autre compte bancaire"); subLbl.getStyleClass().add("add-account-sublabel");
        VBox card = new VBox(12, iconWrapper, lbl, subLbl);
        card.getStyleClass().addAll("account-card", "account-card-add");
        card.setPrefWidth(260); card.setAlignment(Pos.CENTER);
        card.setOnMouseClicked(e -> addNewAccount());
        return card;
    }

    // ─── Coffres ──────────────────────────────────────────────────────────────

    private void handleShowCoffres(CompteBancaire compte) {
        compteAfficheCoffres = compte;
        List<CoffreVirtuel> coffres = coffreService.getByCompte(compte.getIdCompte());
        renderCoffreCards(coffres);
        if (!coffresDisplayContainer.isVisible()) {
            setVisibleManaged(coffresDisplayContainer, true);
            animateSlideIn(coffresDisplayContainer);
        }
        loadAISuggestionsRight();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SURPLUS REVENU
    // ═══════════════════════════════════════════════════════════════════════════

    private void checkAndShowSurplusSuggestion() {
        int idUser = SessionManager.getInstance().getCurrentUserId();
        if (idUser <= 0) return;
        new Thread(() -> {
            Map<String, Object> surplus = revenueService.detectSurplus(idUser);
            if (surplus == null) return;
            List<CoffreVirtuel> coffres = coffreService.getByUser(idUser);
            if (coffres == null || coffres.isEmpty()) return;
            Platform.runLater(() -> buildSurplusPanel(surplus, coffres));
        }, "SurplusDetection").start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ★ NOUVEAU — SYSTÈME D'ALERTES COFFRES VIRTUELS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Point d'entrée : vérifie tous les coffres de l'utilisateur au démarrage.
     * Lance la détection en background thread pour ne pas bloquer l'UI.
     *
     * Deux cas détectés :
     *   1) Progression >= 100% → félicitations + options (transférer / nouveau coffre)
     *   2) Date dépassée + progression < 100% → alerte + options (prolonger / clôturer)
     */
    private void checkCoffreAlerts() {
        int idUser = SessionManager.getInstance().getCurrentUserId();
        if (idUser <= 0) return;

        new Thread(() -> {
            List<CoffreVirtuel> coffres = coffreService.getByUser(idUser);
            if (coffres == null || coffres.isEmpty()) return;

            LocalDate today = LocalDate.now();
            List<CoffreVirtuel> coffresObjectifAtteint = new ArrayList<>();
            List<CoffreVirtuel> coffresObjectifDepasse = new ArrayList<>();

            for (CoffreVirtuel coffre : coffres) {
                // Ignorer coffres sans date ou déjà fermés
                if (coffre.getDateObjectifs() == null || coffre.getDateObjectifs().isEmpty()) continue;
                if ("Ferme".equalsIgnoreCase(coffre.getStatus())
                        || "Clôturé".equalsIgnoreCase(coffre.getStatus())) continue;

                double progression = (coffre.getObjectifMontant() > 0)
                        ? coffre.getMontantActuel() / coffre.getObjectifMontant() : 0.0;

                LocalDate dateObjectif;
                try {
                    dateObjectif = LocalDate.parse(coffre.getDateObjectifs());
                } catch (Exception ex) {
                    continue;
                }

                if (progression >= 1.0) {
                    coffresObjectifAtteint.add(coffre);
                } else if (today.isAfter(dateObjectif)) {
                    coffresObjectifDepasse.add(coffre);
                }
            }

            Platform.runLater(() -> afficherAlertesCoffres(coffresObjectifAtteint, coffresObjectifDepasse));

        }, "CoffreAlert-Check").start();
    }

    /**
     * Orchestre l'affichage des alertes en séquence avec 800ms de délai entre chaque.
     * Affiche d'abord les félicitations, ensuite les alertes de dépassement.
     */
    private void afficherAlertesCoffres(
            List<CoffreVirtuel> atteints,
            List<CoffreVirtuel> depasses) {

        List<Runnable> queue = new ArrayList<>();
        for (CoffreVirtuel c : atteints)  queue.add(() -> afficherDialogObjectifAtteint(c));
        for (CoffreVirtuel c : depasses)  queue.add(() -> afficherDialogObjectifDepasse(c));
        if (queue.isEmpty()) return;

        for (int i = 0; i < queue.size(); i++) {
            final Runnable task = queue.get(i);
            PauseTransition delay = new PauseTransition(Duration.millis(800L * (i + 1)));
            delay.setOnFinished(e -> task.run());
            delay.play();
        }
    }

    /**
     * Dialog vert ✅ — Objectif atteint à 100%.
     * Propose : transférer vers le compte bancaire OU créer un nouveau coffre.
     */
    private void afficherDialogObjectifAtteint(CoffreVirtuel coffre) {
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);

        // ── Colonne gauche ────────────────────────────────────────────────────
        Label trophyLbl = new Label("🏆");
        trophyLbl.setStyle("-fx-font-size: 60px;");
        TranslateTransition bounce = new TranslateTransition(Duration.millis(500), trophyLbl);
        bounce.setFromY(0); bounce.setToY(-12);
        bounce.setAutoReverse(true); bounce.setCycleCount(Animation.INDEFINITE);
        bounce.setInterpolator(Interpolator.EASE_OUT); bounce.play();

        Label badgeLbl = new Label("🎉 Félicitations !");
        badgeLbl.setStyle(
                "-fx-background-color: rgba(34,197,94,0.2); -fx-text-fill: #4ADE80;" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 4 14;"
        );
        VBox leftBox = new VBox(14, trophyLbl, badgeLbl);
        leftBox.setAlignment(Pos.CENTER); leftBox.setPrefWidth(110);

        // ── Colonne droite ────────────────────────────────────────────────────
        Label titleLbl = new Label("Objectif atteint ! 🎊");
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label nomLbl = new Label("Coffre : " + coffre.getNom());
        nomLbl.setStyle("-fx-text-fill: #A5B4FC; -fx-font-size: 13px; -fx-font-weight: 600;");

        double montant = coffre.getMontantActuel();
        Label montantLbl = new Label(String.format("Montant épargné : %.2f DT 💰", montant));
        montantLbl.setStyle("-fx-text-fill: #4ADE80; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label descLbl = new Label(
                "Bravo ! Vous avez atteint votre objectif d'épargne.\n" +
                        "Que souhaitez-vous faire avec ce montant ?"
        );
        descLbl.setWrapText(true); descLbl.setMaxWidth(340);
        descLbl.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 12px; -fx-line-spacing: 3;");

        // Bouton Transférer
        FontIcon transferIcon = new FontIcon("fas-exchange-alt");
        transferIcon.setStyle("-fx-fill: white; -fx-icon-size: 13;");
        Label transferLbl = new Label("Transférer vers mon compte");
        transferLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        Button btnTransferer = new Button();
        btnTransferer.setGraphic(new HBox(7, transferIcon, transferLbl));
        ((HBox) btnTransferer.getGraphic()).setAlignment(Pos.CENTER);
        btnTransferer.setStyle(
                "-fx-background-color: linear-gradient(to right, #059669, #10B981);" +
                        "-fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(16,185,129,0.4), 8, 0, 0, 2);"
        );
        btnTransferer.setMaxWidth(Double.MAX_VALUE);

        // Bouton Nouveau Coffre
        FontIcon plusIcon = new FontIcon("fas-plus-circle");
        plusIcon.setStyle("-fx-fill: white; -fx-icon-size: 13;");
        Label plusLbl = new Label("Créer un nouveau coffre");
        plusLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        Button btnNouveauCoffre = new Button();
        btnNouveauCoffre.setGraphic(new HBox(7, plusIcon, plusLbl));
        ((HBox) btnNouveauCoffre.getGraphic()).setAlignment(Pos.CENTER);
        btnNouveauCoffre.setStyle(
                "-fx-background-color: linear-gradient(to right, #6366F1, #8B5CF6);" +
                        "-fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.4), 8, 0, 0, 2);"
        );
        btnNouveauCoffre.setMaxWidth(Double.MAX_VALUE);

        // Bouton Plus tard
        Button btnFermer = new Button("Plus tard");
        btnFermer.setStyle(
                "-fx-background-color: transparent; -fx-border-color: #475569;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 7 16;"
        );

        VBox btnsBox = new VBox(10, btnTransferer, btnNouveauCoffre, btnFermer);
        btnsBox.setMaxWidth(Double.MAX_VALUE);

        VBox rightBox = new VBox(12, titleLbl, nomLbl, montantLbl, descLbl, btnsBox);
        rightBox.setPrefWidth(370); rightBox.setMaxWidth(370);

        // ── Assemblage ────────────────────────────────────────────────────────
        HBox mainContent = new HBox(20, leftBox, rightBox);
        mainContent.setAlignment(Pos.CENTER_LEFT);

        Button btnX = new Button("✕");
        btnX.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #475569;" +
                        "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4;"
        );
        Region topSpacer = new Region(); HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topBar = new HBox(topSpacer, btnX);
        topBar.setStyle("-fx-padding: 0 0 4 0;");

        VBox root = new VBox(8, topBar, mainContent);
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #0f172a, #1e1b4b);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #22C55E; -fx-border-width: 1.5; -fx-border-radius: 18;" +
                        "-fx-padding: 18 22;" +
                        "-fx-effect: dropshadow(gaussian, rgba(34,197,94,0.5), 28, 0, 0, 6);"
        );

        Scene scene = new Scene(root, 530, 320);
        scene.setFill(null);
        dialog.setScene(scene);
        centrerDialog(dialog, 530, 320);

        // Animation entrée
        root.setOpacity(0); root.setTranslateY(18);
        FadeTransition ft = new FadeTransition(Duration.millis(300), root); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), root);
        tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();

        // Actions
        btnFermer.setOnAction(e -> dialog.close());
        btnX.setOnAction(e -> dialog.close());

        btnTransferer.setOnAction(e -> {
            dialog.close();
            transfererCoffreVersCompte(coffre);
        });

        btnNouveauCoffre.setOnAction(e -> {
            dialog.close();
            coffre.setStatus("Ferme");
            coffreService.edit(coffre);
            if (compteAfficheCoffres != null) handleShowCoffres(compteAfficheCoffres);
            Platform.runLater(this::toggleCoffreForm);
        });

        dialog.show();
    }

    /**
     * Dialog orange ⏰ — Date dépassée, objectif non atteint.
     * Propose : prolonger la durée (1/3/6 mois ou 1 an) OU clôturer le coffre.
     */
    private void afficherDialogObjectifDepasse(CoffreVirtuel coffre) {
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);

        double objectif = coffre.getObjectifMontant();
        double actuel   = coffre.getMontantActuel();
        double pct      = (objectif > 0) ? Math.min(100.0, actuel / objectif * 100.0) : 0.0;
        double manquant = Math.max(0, objectif - actuel);

        // ── Colonne gauche ────────────────────────────────────────────────────
        Label alertEmoji = new Label("⏰");
        alertEmoji.setStyle("-fx-font-size: 54px;");
        ScaleTransition pulse = new ScaleTransition(Duration.millis(800), alertEmoji);
        pulse.setFromX(1.0); pulse.setToX(1.15);
        pulse.setFromY(1.0); pulse.setToY(1.15);
        pulse.setAutoReverse(true); pulse.setCycleCount(Animation.INDEFINITE); pulse.play();

        Label badgeLbl = new Label("⚠️ Date dépassée");
        badgeLbl.setStyle(
                "-fx-background-color: rgba(245,158,11,0.2); -fx-text-fill: #FBBF24;" +
                        "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 20; -fx-padding: 4 10;"
        );
        VBox leftBox = new VBox(14, alertEmoji, badgeLbl);
        leftBox.setAlignment(Pos.CENTER); leftBox.setPrefWidth(110);

        // ── Colonne droite ────────────────────────────────────────────────────
        Label titleLbl = new Label("Objectif non atteint 😔");
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;");

        Label nomLbl = new Label("Coffre : " + coffre.getNom());
        nomLbl.setStyle("-fx-text-fill: #FCA5A5; -fx-font-size: 13px; -fx-font-weight: 600;");

        // Barre de progression
        StackPane barBg = new StackPane();
        barBg.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 5;");
        barBg.setPrefHeight(8); barBg.setMaxWidth(Double.MAX_VALUE);
        String barColor = pct >= 60 ? "#F59E0B" : pct >= 25 ? "#EF4444" : "#991B1B";
        Region barFill = new Region();
        barFill.setStyle("-fx-background-color: " + barColor + "; -fx-background-radius: 5;");
        barFill.setPrefHeight(8);
        barFill.prefWidthProperty().bind(barBg.widthProperty().multiply(pct / 100.0));
        barBg.getChildren().add(barFill);
        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);

        Label progressLbl = new Label(String.format(
                "Progression : %.0f / %.0f DT  (%.0f%%)  —  Il manque %.2f DT",
                actuel, objectif, pct, manquant
        ));
        progressLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        VBox progressBox = new VBox(6, barBg, progressLbl);

        Label descLbl = new Label(
                "La date limite de ce coffre est passée.\n" +
                        "Souhaitez-vous prolonger la durée ou clôturer le coffre ?"
        );
        descLbl.setWrapText(true); descLbl.setMaxWidth(330);
        descLbl.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 12px; -fx-line-spacing: 3;");

        // Sélecteur durée
        Label prolongerLabel = new Label("Prolonger de :");
        prolongerLabel.setStyle("-fx-text-fill: #A5B4FC; -fx-font-size: 12px; -fx-font-weight: 600;");
        ComboBox<String> cbDuree = new ComboBox<>();
        cbDuree.getItems().addAll("1 mois", "3 mois", "6 mois", "1 an");
        cbDuree.setValue("3 mois");
        cbDuree.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);" +
                        "-fx-border-color: #4B5563; -fx-border-radius: 8; -fx-background-radius: 8;" +
                        "-fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 4 10;"
        );
        HBox dureeRow = new HBox(10, prolongerLabel, cbDuree);
        dureeRow.setAlignment(Pos.CENTER_LEFT);

        // Bouton Prolonger
        FontIcon calIcon = new FontIcon("fas-calendar-plus");
        calIcon.setStyle("-fx-fill: white; -fx-icon-size: 13;");
        Label calLbl = new Label("Prolonger la durée");
        calLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        Button btnProlonger = new Button();
        btnProlonger.setGraphic(new HBox(7, calIcon, calLbl));
        ((HBox) btnProlonger.getGraphic()).setAlignment(Pos.CENTER);
        btnProlonger.setStyle(
                "-fx-background-color: linear-gradient(to right, #0D9488, #0891B2);" +
                        "-fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.4), 8, 0, 0, 2);"
        );
        btnProlonger.setMaxWidth(Double.MAX_VALUE);

        // Bouton Clôturer
        FontIcon trashIcon = new FontIcon("fas-times-circle");
        trashIcon.setStyle("-fx-fill: white; -fx-icon-size: 13;");
        Label trashLbl = new Label("Clôturer le coffre");
        trashLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        Button btnCloturer = new Button();
        btnCloturer.setGraphic(new HBox(7, trashIcon, trashLbl));
        ((HBox) btnCloturer.getGraphic()).setAlignment(Pos.CENTER);
        btnCloturer.setStyle(
                "-fx-background-color: linear-gradient(to right, #DC2626, #EF4444);" +
                        "-fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(239,68,68,0.4), 8, 0, 0, 2);"
        );
        btnCloturer.setMaxWidth(Double.MAX_VALUE);

        VBox btnsBox = new VBox(10, dureeRow, btnProlonger, btnCloturer);
        VBox rightBox = new VBox(10, titleLbl, nomLbl, progressBox, descLbl, btnsBox);
        rightBox.setPrefWidth(360); rightBox.setMaxWidth(360);

        // ── Assemblage ────────────────────────────────────────────────────────
        HBox mainContent = new HBox(20, leftBox, rightBox);
        mainContent.setAlignment(Pos.CENTER_LEFT);

        Button btnX = new Button("✕");
        btnX.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #475569;" +
                        "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4;"
        );
        Region topSpacer = new Region(); HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topBar = new HBox(topSpacer, btnX);

        VBox root = new VBox(8, topBar, mainContent);
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #0f172a, #1c1917);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #F59E0B; -fx-border-width: 1.5; -fx-border-radius: 18;" +
                        "-fx-padding: 18 22;" +
                        "-fx-effect: dropshadow(gaussian, rgba(245,158,11,0.5), 28, 0, 0, 6);"
        );

        Scene scene = new Scene(root, 530, 380);
        scene.setFill(null);
        dialog.setScene(scene);
        centrerDialog(dialog, 530, 380);

        // Animation entrée
        root.setOpacity(0); root.setTranslateY(18);
        FadeTransition ft = new FadeTransition(Duration.millis(300), root); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), root);
        tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();

        // Actions
        btnX.setOnAction(e -> dialog.close());

        btnProlonger.setOnAction(e -> {
            String choix = cbDuree.getValue();
            int moisAjoutes = switch (choix) {
                case "1 mois" -> 1;
                case "3 mois" -> 3;
                case "6 mois" -> 6;
                case "1 an"   -> 12;
                default       -> 3;
            };
            LocalDate ancienneDate;
            try { ancienneDate = LocalDate.parse(coffre.getDateObjectifs()); }
            catch (Exception ex) { ancienneDate = LocalDate.now(); }
            LocalDate nouvelleDate = ancienneDate.plusMonths(moisAjoutes);
            coffre.setDateObjectifs(nouvelleDate.format(DateTimeFormatter.ISO_DATE));
            coffreService.edit(coffre);

            HistoryManager.getInstance().add(ActionType.COFFRE_MODIFIE,
                    "Prolongation coffre : " + coffre.getNom() +
                            " | Nouvelle date : " + nouvelleDate +
                            " | Progression actuelle : " + String.format("%.0f%%", pct)
            );

            dialog.close();
            if (compteAfficheCoffres != null) handleShowCoffres(compteAfficheCoffres);
            updateKPIs();
            showNotificationStyle("Coffre prolongé ✅",
                    "La date limite a été repoussée au " +
                            nouvelleDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    "#10B981");
        });

        btnCloturer.setOnAction(e -> {
            // Dialog de confirmation
            Stage confirm = new Stage(StageStyle.TRANSPARENT);
            confirm.initModality(Modality.APPLICATION_MODAL);

            Label iconLbl = new Label("🗑️");
            iconLbl.setStyle("-fx-font-size: 36px;");
            Label msgLbl = new Label(
                    "Êtes-vous sûr de vouloir clôturer le coffre\n\"" + coffre.getNom() + "\" ?"
            );
            msgLbl.setWrapText(true);
            msgLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 13px; -fx-text-alignment: center;");

            Button btnOui = new Button("Oui, clôturer");
            btnOui.setStyle(
                    "-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold;" +
                            "-fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;"
            );
            Button btnNon = new Button("Annuler");
            btnNon.setStyle(
                    "-fx-background-color: transparent; -fx-border-color: #475569;" +
                            "-fx-border-radius: 8; -fx-text-fill: #94A3B8;" +
                            "-fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;"
            );
            HBox confirmBtns = new HBox(12, btnNon, btnOui);
            confirmBtns.setAlignment(Pos.CENTER);

            VBox confirmRoot = new VBox(16, iconLbl, msgLbl, confirmBtns);
            confirmRoot.setAlignment(Pos.CENTER);
            confirmRoot.setStyle(
                    "-fx-background-color: #1E293B; -fx-background-radius: 14;" +
                            "-fx-border-color: #EF4444; -fx-border-width: 1; -fx-border-radius: 14;" +
                            "-fx-padding: 24 28;"
            );

            Scene cs = new Scene(confirmRoot, 340, 200);
            cs.setFill(null);
            confirm.setScene(cs);
            centrerDialog(confirm, 340, 200);

            btnNon.setOnAction(ev -> confirm.close());
            btnOui.setOnAction(ev -> {
                coffre.setStatus("Ferme");
                coffreService.edit(coffre);

                HistoryManager.getInstance().add(ActionType.COFFRE_SUPPRIME,
                        "Clôture coffre (objectif non atteint) : " + coffre.getNom() +
                                " | Progression finale : " + String.format("%.0f%%", pct) +
                                " | Montant récupéré : " + String.format("%.2f DT", actuel)
                );

                confirm.close();
                dialog.close();
                loadAccountsFromDB();
                updateKPIs();
                showNotificationStyle("Coffre clôturé",
                        coffre.getNom() + " a été clôturé. Montant récupéré : " +
                                String.format("%.2f DT", actuel), "#F59E0B");
            });
            confirm.show();
        });

        dialog.show();
    }

    /**
     * Transfère le montant du coffre vers le compte bancaire associé,
     * remet le coffre à zéro et le clôture.
     */
    private void transfererCoffreVersCompte(CoffreVirtuel coffre) {
        CompteBancaire compteAssocie = allAccounts.stream()
                .filter(c -> c.getIdCompte() == coffre.getIdCompte())
                .findFirst().orElse(null);

        if (compteAssocie == null) {
            showNotification("Erreur", "Compte associé introuvable pour ce coffre.");
            return;
        }

        double montant = coffre.getMontantActuel();
        compteAssocie.setSolde(compteAssocie.getSolde() + montant);
        service.edit(compteAssocie);

        coffre.setMontantActuel(0);
        coffre.setStatus("Ferme");
        coffreService.edit(coffre);

        HistoryManager.getInstance().add(ActionType.COFFRE_MODIFIE,
                "Transfert coffre → compte : " + coffre.getNom() +
                        " | Montant transféré : " + String.format("%.2f DT", montant) +
                        " | Compte destinataire : " + compteAssocie.getNumeroCompte()
        );

        loadAccountsFromDB();
        updateKPIs();
        showNotificationStyle("Transfert effectué ✅",
                String.format("%.2f DT transférés vers le compte %s",
                        montant, compteAssocie.getNumeroCompte()),
                "#10B981");
    }

    /** Centre un Stage sur l'écran principal. */
    private void centrerDialog(Stage dialog, double width, double height) {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        dialog.setX((screen.getWidth()  - width)  / 2);
        dialog.setY((screen.getHeight() - height) / 2);
    }

    /** Toast de notification stylisé — apparaît en bas à droite, disparaît après 3.5s. */
    private void showNotificationStyle(String titre, String message, String color) {
        Stage toast = new Stage(StageStyle.TRANSPARENT);
        toast.initModality(Modality.NONE);

        Label iconLbl = new Label();
        iconLbl.setStyle("-fx-font-size: 16px;");
        if (color.contains("10B981") || color.contains("059669")) iconLbl.setText("✅");
        else if (color.contains("EF4444") || color.contains("DC2626")) iconLbl.setText("❌");
        else iconLbl.setText("ℹ️");

        Label titreLbl = new Label(titre);
        titreLbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label msgLbl = new Label(message);
        msgLbl.setWrapText(true); msgLbl.setMaxWidth(260);
        msgLbl.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 11px;");

        VBox textBox = new VBox(3, titreLbl, msgLbl);
        HBox content = new HBox(12, iconLbl, textBox);
        content.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(content);
        root.setStyle(
                "-fx-background-color: #1E293B; -fx-background-radius: 12;" +
                        "-fx-border-color: " + color + "; -fx-border-width: 1.5; -fx-border-radius: 12;" +
                        "-fx-padding: 14 18;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 14, 0, 0, 4);"
        );

        Scene scene = new Scene(root, 320, 80);
        scene.setFill(null);
        toast.setScene(scene);

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        toast.setX(screen.getMaxX() - 340);
        toast.setY(screen.getMaxY() - 100);

        root.setOpacity(0);
        toast.show();
        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), root);
        fadeIn.setToValue(1);
        fadeIn.setOnFinished(e -> {
            PauseTransition wait = new PauseTransition(Duration.seconds(3.5));
            wait.setOnFinished(ev -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(eOut -> toast.close());
                fadeOut.play();
            });
            wait.play();
        });
        fadeIn.play();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FIN SYSTÈME D'ALERTES COFFRES
    // ══════════════════════════════════════════════════════════════════════════

    private void buildSurplusPanel(Map<String, Object> surplus, List<CoffreVirtuel> coffres) {
        String message        = (String) surplus.get("message");
        double montantEpargne = (double) surplus.get("montantEpargne");
        double surplusAmount  = (double) surplus.get("surplus");

        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox leftSection = new VBox(18);
        leftSection.setPrefWidth(400); leftSection.setMinWidth(400);
        leftSection.setAlignment(Pos.TOP_LEFT);
        leftSection.setStyle("-fx-background-color: linear-gradient(to bottom, #0f2942, #0a1f35);-fx-padding: 30 28 28 28;");

        FontIcon robotIcon = new FontIcon("fas-robot");
        robotIcon.setStyle("-fx-fill: #60a5fa; -fx-icon-size: 42;");
        StackPane robotPane = new StackPane(robotIcon);
        robotPane.setStyle("-fx-background-color: rgba(59,130,246,0.15);-fx-background-radius: 50%;-fx-min-width: 72; -fx-min-height: 72;-fx-max-width: 72; -fx-max-height: 72;");
        ScaleTransition robotPulse = new ScaleTransition(Duration.millis(1200), robotPane);
        robotPulse.setFromX(1.0); robotPulse.setToX(1.08);
        robotPulse.setFromY(1.0); robotPulse.setToY(1.08);
        robotPulse.setAutoReverse(true); robotPulse.setCycleCount(Animation.INDEFINITE); robotPulse.play();

        Label badgeLbl = new Label("  🤖  Conseil IA · Surplus détecté  ");
        badgeLbl.setStyle("-fx-background-color: rgba(59,130,246,0.2);-fx-text-fill: #60a5fa;-fx-font-size: 11px; -fx-font-weight: bold;-fx-background-radius: 20; -fx-padding: 5 12;");
        Label titleLbl2 = new Label("Revenu exceptionnel ce mois-ci !");
        titleLbl2.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;");
        Label messageLbl = new Label(message);
        messageLbl.setWrapText(true); messageLbl.setMaxWidth(360);
        messageLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px; -fx-line-spacing: 3;");
        Separator sepH = new Separator();
        sepH.setStyle("-fx-background-color: rgba(255,255,255,0.1);");

        VBox surplusCard = new VBox(10);
        surplusCard.setStyle("-fx-background-color: rgba(52,211,153,0.1);-fx-background-radius: 12;-fx-border-color: rgba(52,211,153,0.3);-fx-border-radius: 12; -fx-padding: 14 16;");
        HBox row1 = new HBox(10); row1.setAlignment(Pos.CENTER_LEFT);
        FontIcon trendIcon = new FontIcon("fas-chart-line");
        trendIcon.setStyle("-fx-fill: #34d399; -fx-icon-size: 16;");
        Label surplusLbl = new Label(String.format("Surplus détecté : +%.2f DT ce mois", surplusAmount));
        surplusLbl.setStyle("-fx-text-fill: #34d399; -fx-font-size: 12px; -fx-font-weight: bold;");
        row1.getChildren().addAll(trendIcon, surplusLbl);
        HBox row2 = new HBox(10); row2.setAlignment(Pos.CENTER_LEFT);
        FontIcon piggyIcon = new FontIcon("fas-piggy-bank");
        piggyIcon.setStyle("-fx-fill: #818cf8; -fx-icon-size: 16;");
        Label montantLbl2 = new Label(String.format("Montant conseillé à épargner : %.2f DT  (20%%)", montantEpargne));
        montantLbl2.setStyle("-fx-text-fill: #818cf8; -fx-font-size: 12px; -fx-font-weight: bold;");
        row2.getChildren().addAll(piggyIcon, montantLbl2);
        surplusCard.getChildren().addAll(row1, row2);

        Button btnFermer2 = new Button("Ignorer la suggestion");
        btnFermer2.setStyle("-fx-background-color: transparent; -fx-border-color: #475569;-fx-border-radius: 8; -fx-text-fill: #64748b; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 8 16;");
        btnFermer2.setOnAction(e -> dialog.close());
        leftSection.getChildren().addAll(robotPane, badgeLbl, titleLbl2, messageLbl, sepH, surplusCard, btnFermer2);

        VBox rightSection = new VBox(0);
        rightSection.setPrefWidth(460); rightSection.setMinWidth(460);
        rightSection.setStyle("-fx-background-color: #0f172a; -fx-padding: 0;");

        HBox rightHeader = new HBox(10); rightHeader.setAlignment(Pos.CENTER_LEFT);
        rightHeader.setStyle("-fx-background-color: #1e293b; -fx-padding: 18 24;");
        FontIcon vaultIcon2 = new FontIcon("fas-piggy-bank");
        vaultIcon2.setStyle("-fx-fill: #818cf8; -fx-icon-size: 18;");
        VBox headerText = new VBox(2);
        Label headerTitle = new Label("Vos coffres virtuels");
        headerTitle.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label headerSub = new Label("Choisissez où épargner votre surplus");
        headerSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        headerText.getChildren().addAll(headerTitle, headerSub);
        rightHeader.getChildren().addAll(vaultIcon2, headerText);

        VBox coffresList = new VBox(0);
        coffresList.setStyle("-fx-background-color: #0f172a;");
        for (int i = 0; i < coffres.size(); i++) {
            CoffreVirtuel coffre = coffres.get(i);
            HBox rowCoffre = buildCoffreSurplusRow(coffre, montantEpargne, dialog);
            if (i % 2 == 0) rowCoffre.setStyle(rowCoffre.getStyle() + "-fx-background-color: rgba(255,255,255,0.02);");
            coffresList.getChildren().add(rowCoffre);
            if (i < coffres.size() - 1) {
                Separator s = new Separator();
                s.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 0;");
                coffresList.getChildren().add(s);
            }
        }
        ScrollPane scroll = new ScrollPane(coffresList);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setFitToWidth(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        rightSection.getChildren().addAll(rightHeader, scroll);

        HBox mainContent2 = new HBox(0, leftSection, rightSection);

        Button btnX2 = new Button("✕");
        btnX2.setStyle("-fx-background-color: transparent; -fx-text-fill: #475569;-fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 2 8;");
        btnX2.setOnMouseEntered(e -> btnX2.setStyle("-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #ef4444;-fx-font-size: 16px; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 2 8;"));
        btnX2.setOnMouseExited(e -> btnX2.setStyle("-fx-background-color: transparent; -fx-text-fill: #475569;-fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 2 8;"));
        btnX2.setOnAction(e -> dialog.close());
        Region topSpacer2 = new Region(); HBox.setHgrow(topSpacer2, Priority.ALWAYS);
        HBox topBar2 = new HBox(topSpacer2, btnX2);
        topBar2.setAlignment(Pos.CENTER_RIGHT);
        topBar2.setStyle("-fx-background-color: #0a1628; -fx-padding: 8 10 0 10;");

        VBox root2 = new VBox(0, topBar2, mainContent2);
        root2.setStyle("-fx-background-color: #0a1628; -fx-background-radius: 18;-fx-border-color: #1e3a5f; -fx-border-width: 1.5; -fx-border-radius: 18;-fx-effect: dropshadow(gaussian, rgba(59,130,246,0.4), 30, 0, 0, 8);");

        Scene scene2 = new Scene(root2, 860, 520);
        scene2.setFill(null);
        dialog.setScene(scene2);
        centrerDialog(dialog, 860, 520);

        root2.setOpacity(0); root2.setTranslateY(20);
        FadeTransition fadeIn2 = new FadeTransition(Duration.millis(320), root2); fadeIn2.setFromValue(0); fadeIn2.setToValue(1);
        TranslateTransition slideIn2 = new TranslateTransition(Duration.millis(320), root2);
        slideIn2.setFromY(20); slideIn2.setToY(0); slideIn2.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fadeIn2, slideIn2).play();
        dialog.show();
    }

    private HBox buildCoffreSurplusRow(CoffreVirtuel coffre, double montantEpargne, Stage parentDialog) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 14 20; -fx-cursor: default;");

        FontIcon icon = new FontIcon(coffre.isEstVerrouille() ? "fas-lock" : "fas-piggy-bank");
        icon.setStyle("-fx-fill: " + (coffre.isEstVerrouille() ? "#f59e0b" : "#818cf8") + "; -fx-icon-size: 22;");
        StackPane iconPane = new StackPane(icon);
        iconPane.setStyle("-fx-background-color: " + (coffre.isEstVerrouille() ? "rgba(245,158,11,0.12)" : "rgba(129,140,248,0.12)") + ";-fx-background-radius: 10; -fx-min-width: 46; -fx-min-height: 46; -fx-max-width: 46; -fx-max-height: 46;");

        VBox infos = new VBox(5); HBox.setHgrow(infos, Priority.ALWAYS);
        Label nomLbl = new Label(coffre.getNom());
        nomLbl.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-font-weight: bold;");

        double objectif = coffre.getObjectifMontant();
        double actu     = coffre.getMontantActuel();
        double pct      = (objectif > 0) ? Math.min(100.0, actu / objectif * 100.0) : 0.0;

        StackPane barBg2 = new StackPane();
        barBg2.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 4;");
        barBg2.setPrefHeight(6); barBg2.setMinWidth(160); barBg2.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(barBg2, Priority.ALWAYS);
        String barColor2 = pct >= 100 ? "#22c55e" : pct >= 60 ? "#6366f1" : pct >= 25 ? "#f59e0b" : "#ef4444";
        HBox barFill2 = new HBox();
        barFill2.setStyle("-fx-background-color: " + barColor2 + "; -fx-background-radius: 4;");
        barFill2.setPrefHeight(6);
        barFill2.prefWidthProperty().bind(barBg2.widthProperty().multiply(pct / 100.0));
        barBg2.getChildren().add(barFill2);
        StackPane.setAlignment(barFill2, Pos.CENTER_LEFT);
        Label progressLbl2 = new Label(String.format("%.0f / %.0f DT  ·  %.0f%%", actu, objectif, pct));
        progressLbl2.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        infos.getChildren().addAll(nomLbl, barBg2, progressLbl2);

        Button btnAjouter = new Button();
        if (pct >= 100) {
            FontIcon checkIcon = new FontIcon("fas-check-circle");
            checkIcon.setStyle("-fx-fill: #22c55e; -fx-icon-size: 13;");
            Label checkLbl = new Label("Objectif atteint");
            checkLbl.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px;");
            btnAjouter.setGraphic(new HBox(5, checkIcon, checkLbl));
            btnAjouter.setStyle("-fx-background-color: rgba(34,197,94,0.08); -fx-border-color: rgba(34,197,94,0.3);-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 14; -fx-cursor: default;");
            btnAjouter.setDisable(true);
        } else {
            FontIcon plusIcon2 = new FontIcon("fas-plus");
            plusIcon2.setStyle("-fx-fill: white; -fx-icon-size: 12;");
            VBox btnContent = new VBox(2); btnContent.setAlignment(Pos.CENTER);
            Label btnLine1 = new Label("Ajouter surplus");
            btnLine1.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
            Label btnLine2 = new Label(String.format("+ %.2f DT", montantEpargne));
            btnLine2.setStyle("-fx-text-fill: #a5b4fc; -fx-font-size: 10px;");
            btnContent.getChildren().addAll(btnLine1, btnLine2);
            btnAjouter.setGraphic(btnContent);
            btnAjouter.setStyle("-fx-background-color: linear-gradient(to bottom, #6366f1, #4f46e5);-fx-background-radius: 10; -fx-border-color: #818cf8; -fx-border-width: 0.5;-fx-border-radius: 10; -fx-cursor: hand; -fx-padding: 8 18;-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.4), 8, 0, 0, 2);");
        }

        btnAjouter.setOnAction(e -> {
            try {
                double restant  = objectif - coffre.getMontantActuel();
                double aAjouter = Math.min(montantEpargne, restant);
                if (aAjouter <= 0) return;
                coffre.setMontantActuel(coffre.getMontantActuel() + aAjouter);
                coffre.setDateDernierDepot(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
                coffreService.edit(coffre);
                double newActuel = coffre.getMontantActuel();
                double newPct    = (objectif > 0) ? Math.min(100.0, newActuel / objectif * 100.0) : 0.0;
                progressLbl2.setText(String.format("%.0f / %.0f DT  ·  %.0f%%", newActuel, objectif, newPct));
                barFill2.prefWidthProperty().unbind();
                barFill2.prefWidthProperty().bind(barBg2.widthProperty().multiply(newPct / 100.0));
                FontIcon okIcon = new FontIcon("fas-check-circle");
                okIcon.setStyle("-fx-fill: white; -fx-icon-size: 14;");
                Label okLbl = new Label(String.format("%.2f DT ajoutés !", aAjouter));
                okLbl.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
                btnAjouter.setGraphic(new HBox(6, okIcon, okLbl));
                btnAjouter.setStyle("-fx-background-color: linear-gradient(to bottom, #22c55e, #16a34a);-fx-background-radius: 10; -fx-border-color: #4ade80; -fx-border-width: 0.5;-fx-border-radius: 10; -fx-padding: 8 18;");
                btnAjouter.setDisable(true);
                updateKPIs();
                HistoryManager.getInstance().add(ActionType.COFFRE_MODIFIE,
                        "Surplus ajouté → Coffre: " + coffre.getNom() +
                                " | Montant: +" + String.format("%.2f", aAjouter) + " DT" +
                                " | Nouveau total: " + String.format("%.2f", newActuel) + " DT");
                PauseTransition pauseT = new PauseTransition(Duration.seconds(2));
                pauseT.setOnFinished(ev -> parentDialog.close());
                pauseT.play();
            } catch (Exception ex) {
                System.err.println("[Surplus] Erreur: " + ex.getMessage());
            }
        });

        row.getChildren().addAll(iconPane, infos, btnAjouter);
        return row;
    }

    private void renderCoffreCards(List<CoffreVirtuel> coffres) {
        if (coffresCardsContainer == null) return;
        coffresCardsContainer.getChildren().clear();
        if (coffres.isEmpty()) {
            Label empty = new Label("Aucun coffre pour ce compte. Cliquez sur Nouveau coffre pour en creer un !");
            empty.getStyleClass().add("vaults-section-subtitle"); empty.setWrapText(true);
            coffresCardsContainer.getChildren().add(empty);
            return;
        }
        for (CoffreVirtuel coffre : coffres)
            coffresCardsContainer.getChildren().add(buildCoffreCard(coffre));
    }

    private VBox buildCoffreCard(CoffreVirtuel coffre) {
        String iconLit      = coffre.isEstVerrouille() ? "fas-lock" : "fas-piggy-bank";
        String wrapperStyle = coffre.isEstVerrouille() ? "vault-icon-wrapper vault-icon-emergency" : "vault-icon-wrapper vault-icon-vacation";
        String indicStyle   = coffre.isEstVerrouille() ? "vault-status-indicator vault-status-locked" : "vault-status-indicator vault-status-active";

        FontIcon icon = new FontIcon(iconLit); icon.getStyleClass().add("vault-icon");
        StackPane iconWrapper = new StackPane(icon); iconWrapper.getStyleClass().addAll(wrapperStyle.split(" "));
        Label nomLbl = new Label(coffre.getNom()); nomLbl.getStyleClass().add("vault-name");
        String dateStr = (coffre.getDateObjectifs() != null && !coffre.getDateObjectifs().isEmpty())
                ? "Objectif : " + coffre.getDateObjectifs() : "Sans echeance";
        Label dateLbl = new Label(dateStr); dateLbl.getStyleClass().add("vault-target-date");
        VBox nameBox = new VBox(2, nomLbl, dateLbl); HBox.setHgrow(nameBox, Priority.ALWAYS);
        StackPane indicator = new StackPane(); indicator.getStyleClass().addAll(indicStyle.split(" "));
        if (coffre.isEstVerrouille()) {
            FontIcon li = new FontIcon("fas-lock"); li.getStyleClass().add("vault-lock-icon");
            indicator.getChildren().add(li);
        }
        HBox topRow = new HBox(10, iconWrapper, nameBox, indicator); topRow.setAlignment(Pos.CENTER_LEFT);

        double objectif = coffre.getObjectifMontant(), actuel = coffre.getMontantActuel();
        double progression = objectif > 0 ? Math.min(actuel / objectif, 1.0) : 0;
        int pct = (int)(progression * 100);
        Label currentLbl = new Label(String.format(Locale.US, "%,.2f DT", actuel)); currentLbl.getStyleClass().add("vault-current");
        Label targetLbl  = new Label(String.format(Locale.US, "sur %,.2f DT", objectif)); targetLbl.getStyleClass().add("vault-target");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox amountsRow = new HBox(currentLbl, gap, targetLbl); amountsRow.setAlignment(Pos.CENTER_LEFT);
        String barColor;
        if (pct >= 100)     { barColor = "#22C55E"; }
        else if (pct >= 26) { barColor = "#F97316"; }
        else                { barColor = "#EF4444"; }
        javafx.scene.layout.HBox progressTrack = new javafx.scene.layout.HBox();
        progressTrack.setPrefHeight(8); progressTrack.setMaxWidth(Double.MAX_VALUE);
        progressTrack.setStyle("-fx-background-color: #E5E7EB; -fx-background-radius: 6;");
        javafx.scene.layout.Region progressFill = new javafx.scene.layout.Region();
        progressFill.setPrefHeight(8);
        progressFill.setStyle("-fx-background-color: " + barColor + "; -fx-background-radius: 6;");
        progressFill.prefWidthProperty().bind(progressTrack.widthProperty().multiply(progression));
        progressTrack.getChildren().add(progressFill);
        Label pctLbl = new Label(pct >= 100 ? "100% terminé ✓" : pct + "%" + (coffre.isEstVerrouille() ? " — Verrouillé" : ""));
        pctLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + barColor + ";");
        VBox progressBox = new VBox(6, amountsRow, progressTrack, pctLbl);

        DragonState dragonState = compteAfficheCoffres != null
                ? dragonService.buildDragonState(compteAfficheCoffres, Collections.singletonList(coffre), SessionManager.getInstance().getCurrentUserId())
                : null;
        StackPane dragonIconWrapper = buildDragonIconForCard(coffre, dragonState, pct);

        FontIcon trashIcon = new FontIcon("fas-trash-alt"); trashIcon.setStyle("-fx-fill: #EF4444; -fx-icon-size: 13;");
        Button btnSuppr = new Button(); btnSuppr.setGraphic(trashIcon);
        btnSuppr.setTooltip(new Tooltip("Supprimer ce coffre"));
        String supprStyle = "-fx-background-color: transparent; -fx-cursor: hand; -fx-border-color: #EF4444; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 5 9;";
        String supprHover = "-fx-background-color: #FEE2E2; -fx-cursor: hand; -fx-border-color: #EF4444; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 5 9;";
        btnSuppr.setStyle(supprStyle);
        btnSuppr.setOnMouseEntered(e -> btnSuppr.setStyle(supprHover));
        btnSuppr.setOnMouseExited(e  -> btnSuppr.setStyle(supprStyle));
        btnSuppr.setOnAction(e -> deleteCoffre(coffre));
        Region cardSpacer = new Region(); HBox.setHgrow(cardSpacer, Priority.ALWAYS);
        HBox cardFooter = new HBox(8, dragonIconWrapper, cardSpacer, btnSuppr);
        cardFooter.setAlignment(Pos.CENTER_LEFT); cardFooter.setStyle("-fx-padding: 4 0 0 0;");

        VBox card = new VBox(14, topRow, progressBox, cardFooter);
        card.getStyleClass().add("vault-card"); card.setPrefWidth(240); card.setStyle("-fx-cursor: hand;");
        card.setOnMouseClicked(ev -> {
            if (ev.getTarget() instanceof Button || ev.getTarget() instanceof FontIcon) return;
            Node n = ev.getTarget() instanceof Node ? (Node) ev.getTarget() : null;
            while (n != null) {
                if (n.getStyleClass().contains("dragon-icon-wrapper")) return;
                n = n.getParent();
            }
            populateCoffreForm(coffre);
        });
        return card;
    }

    private static double dragonScaleFactor(int pct) {
        return 0.5 + 0.7 * (Math.min(100, pct) / 100.0);
    }

    private static final String[] DRAGON_BG_COLOR    = { "#86EFAC", "#93C5FD", "#C4B5FD", "#FDBA74", "#FDE047" };
    private static final String[] DRAGON_BORDER_COLOR = { "#22C55E", "#3B82F6", "#8B5CF6", "#EA580C", "#CA8A04" };
    private static final double[] AVATAR_CARD_SIZES   = { 38, 44, 50, 56, 64 };

    private Image imgHeroHeureux     = null;
    private Image imgHeroPeutContent = null;
    private Image imgHeroTriste      = null;
    private Image imgHeroFache       = null;
    private Image imgHeroNeutre      = null;

    private Image loadHeroImage(String... paths) {
        for (String p : paths) {
            try {
                java.net.URL url = getClass().getResource(p);
                if (url == null)
                    url = Thread.currentThread().getContextClassLoader()
                            .getResource(p.startsWith("/") ? p.substring(1) : p);
                if (url != null) {
                    Image img = new Image(url.toExternalForm(), true);
                    return img;
                }
            } catch (Exception ignored) {}
        }
        for (String p : paths) {
            try {
                java.net.URL loc = getClass().getProtectionDomain().getCodeSource().getLocation();
                if (loc != null) {
                    java.io.File base = new java.io.File(loc.toURI());
                    java.io.File projectDir = base;
                    for (int i = 0; i < 4; i++) {
                        if (projectDir.getParentFile() != null)
                            projectDir = projectDir.getParentFile();
                    }
                    String fileName = p.contains("/") ? p.substring(p.lastIndexOf('/') + 1) : p;
                    java.io.File[] candidates = {
                            new java.io.File(base, "images/" + fileName),
                            new java.io.File(projectDir, "src/main/resources/images/" + fileName)
                    };
                    for (java.io.File f : candidates) {
                        if (f.exists()) {
                            Image img = new Image(f.toURI().toString());
                            if (!img.isError()) return img;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Image getHeroAvatarForState(DragonState state) {
        if (imgHeroHeureux == null)
            imgHeroHeureux = loadHeroImage("/images/hero_heureux.png", "images/hero_heureux.png");
        if (imgHeroPeutContent == null)
            imgHeroPeutContent = loadHeroImage("/images/hero_peutContent.jpg", "images/hero_peutContent.jpg", "/images/hero_peutContent.png");
        if (imgHeroTriste == null)
            imgHeroTriste = loadHeroImage("/images/hero_triste.png", "images/hero_triste.png");
        if (imgHeroFache == null)
            imgHeroFache = loadHeroImage("/images/hero_fache.png", "images/hero_fache.png");
        if (imgHeroNeutre == null)
            imgHeroNeutre = loadHeroImage("/images/hero_neutre.png", "images/hero_neutre.png", "/images/avatar_hero.png");
        if (state == null) return imgHeroNeutre;
        int pct = state.getProgressionPct();
        if (state.isAbandonDepot2Mois()) return imgHeroFache != null ? imgHeroFache : imgHeroNeutre;
        if (pct >= 100 || state.isObjectifAtteint()) return imgHeroHeureux != null ? imgHeroHeureux : imgHeroNeutre;
        if (pct >= 60) return imgHeroPeutContent != null ? imgHeroPeutContent : imgHeroNeutre;
        if (pct >= 25) return imgHeroNeutre;
        return imgHeroTriste != null ? imgHeroTriste : imgHeroNeutre;
    }

    private Image getHeroAvatar() { return getHeroAvatarForState(null); }

    private StackPane buildDragonIconForCard(CoffreVirtuel coffre, DragonState state, int pct) {
        DragonState.TamagotchiEtat etat = state != null
                ? DragonState.calculerTamagotchiEtat(state) : DragonState.TamagotchiEtat.STABLE;
        double scale = dragonScaleFactor(pct);
        int idx = state != null ? Math.min(state.getNiveau().ordinal(), AVATAR_CARD_SIZES.length - 1) : 0;
        Image avatar = getHeroAvatarForState(state);
        boolean hasAvatar = avatar != null && !avatar.isError();
        Node dragonNode;
        if (hasAvatar) {
            double size = AVATAR_CARD_SIZES[idx];
            ImageView iv = new ImageView(avatar);
            iv.setFitWidth(size); iv.setFitHeight(size);
            iv.setPreserveRatio(true); iv.setSmooth(true);
            dragonNode = iv;
        } else {
            String[] fallbackEmoji = { "🐣", "🐉", "🐲", "🔥", "👑" };
            Label lbl = new Label(fallbackEmoji[idx]);
            lbl.setStyle("-fx-font-size: 20px; -fx-cursor: hand;");
            dragonNode = lbl;
        }
        String bg     = hasAvatar ? "transparent" : DRAGON_BG_COLOR[idx];
        String border = hasAvatar ? "transparent" : DRAGON_BORDER_COLOR[idx];
        if (!hasAvatar && pct >= 100) {
            bg = "linear-gradient(to bottom, #FEF08A 0%, #FDE047 50%, #FACC15 100%)";
            border = "#CA8A04";
        }
        StackPane dragonPane = new StackPane(dragonNode);
        dragonPane.getStyleClass().add("dragon-icon-wrapper");
        dragonPane.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 50%; -fx-cursor: hand; "
                + "-fx-border-color: " + border + "; -fx-border-width: " + (hasAvatar ? "0" : "2") + "; -fx-border-radius: 50%;");
        dragonPane.setMinSize(70, 70); dragonPane.setMaxSize(70, 70);
        dragonPane.setScaleX(scale); dragonPane.setScaleY(scale);
        Tooltip.install(dragonPane, new Tooltip("Clique pour que Draco te parle de ce coffre"));
        appliquerAnimationHero(dragonPane, state, pct);
        StackPane wrapper = new StackPane(dragonPane);
        wrapper.getStyleClass().add("dragon-icon-wrapper");
        wrapper.setMinSize(70, 70); wrapper.setMaxSize(70, 70);
        if (pct >= 100) lancerParticulesVictoire(wrapper);
        dragonPane.setOnMouseClicked(ev -> {
            ev.consume();
            if (state == null) return;
            onDragonClicked(coffre, state, dragonPane);
        });
        return wrapper;
    }

    private void appliquerAnimationHero(StackPane dragonPane, DragonState state, int pct) {
        if (dragonPane == null) return;
        if (pct >= 100) {
            RotateTransition rt = new RotateTransition(Duration.millis(600), dragonPane);
            rt.setFromAngle(-8); rt.setToAngle(8); rt.setAutoReverse(true); rt.setCycleCount(Animation.INDEFINITE);
            TranslateTransition saut = new TranslateTransition(Duration.millis(400), dragonPane);
            saut.setFromY(0); saut.setToY(-10); saut.setAutoReverse(true); saut.setCycleCount(Animation.INDEFINITE);
            new ParallelTransition(rt, saut).play(); return;
        }
        if (state != null && state.isAbandonDepot2Mois()) {
            TranslateTransition descente = new TranslateTransition(Duration.millis(1500), dragonPane);
            descente.setFromY(0); descente.setToY(5); descente.setAutoReverse(true); descente.setCycleCount(Animation.INDEFINITE);
            FadeTransition larmes = new FadeTransition(Duration.millis(2000), dragonPane);
            larmes.setFromValue(1.0); larmes.setToValue(0.6); larmes.setAutoReverse(true); larmes.setCycleCount(Animation.INDEFINITE);
            new ParallelTransition(descente, larmes).play(); return;
        }
        if (state != null && state.getHumeur() == DragonState.Humeur.CACHE) {
            TranslateTransition shake = new TranslateTransition(Duration.millis(80), dragonPane);
            shake.setFromX(-4); shake.setToX(4); shake.setAutoReverse(true); shake.setCycleCount(Animation.INDEFINITE);
            ScaleTransition tension = new ScaleTransition(Duration.millis(200), dragonPane);
            tension.setFromX(dragonPane.getScaleX()); tension.setToX(dragonPane.getScaleX() * 1.05);
            tension.setFromY(dragonPane.getScaleY()); tension.setToY(dragonPane.getScaleY() * 1.05);
            tension.setAutoReverse(true); tension.setCycleCount(Animation.INDEFINITE);
            new ParallelTransition(shake, tension).play(); return;
        }
        if (pct >= 60) {
            TranslateTransition bounce = new TranslateTransition(Duration.millis(500), dragonPane);
            bounce.setFromY(0); bounce.setToY(-12); bounce.setAutoReverse(true); bounce.setCycleCount(Animation.INDEFINITE);
            bounce.setInterpolator(Interpolator.EASE_OUT);
            RotateTransition rt = new RotateTransition(Duration.millis(1000), dragonPane);
            rt.setFromAngle(-5); rt.setToAngle(5); rt.setAutoReverse(true); rt.setCycleCount(Animation.INDEFINITE);
            new ParallelTransition(bounce, rt).play();
        } else if (pct >= 25) {
            TranslateTransition tt = new TranslateTransition(Duration.millis(1600), dragonPane);
            tt.setFromY(0); tt.setToY(-8); tt.setAutoReverse(true); tt.setCycleCount(Animation.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH); tt.play();
        } else {
            TranslateTransition tt = new TranslateTransition(Duration.millis(2200), dragonPane);
            tt.setFromY(0); tt.setToY(-5); tt.setAutoReverse(true); tt.setCycleCount(Animation.INDEFINITE);
            tt.setInterpolator(Interpolator.EASE_BOTH); tt.play();
        }
    }

    private void lancerParticulesVictoire(StackPane wrapper) {
        StackPane particlePane = new StackPane();
        particlePane.setMouseTransparent(true); particlePane.setPickOnBounds(false);
        double[] angles = { 0, 45, 90, 135, 180, 225, 270, 315 };
        for (double angle : angles) {
            Circle dot = new Circle(3, Color.web("#F59E0B", 0.9));
            double rad = Math.toRadians(angle);
            TranslateTransition pt = new TranslateTransition(Duration.millis(500), dot);
            pt.setToX(Math.cos(rad) * 16); pt.setToY(Math.sin(rad) * 16);
            FadeTransition ft = new FadeTransition(Duration.millis(500), dot);
            ft.setFromValue(1); ft.setToValue(0);
            pt.play(); ft.play();
            particlePane.getChildren().add(dot);
        }
        wrapper.getChildren().add(0, particlePane);
    }

    private void onDragonClicked(CoffreVirtuel coffre, DragonState state, StackPane dragonPane) {
        if (dragonPane != null) {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(150), dragonPane);
            pulse.setFromX(dragonPane.getScaleX()); pulse.setToX(dragonPane.getScaleX() * 1.2);
            pulse.setFromY(dragonPane.getScaleY()); pulse.setToY(dragonPane.getScaleY() * 1.2);
            pulse.setAutoReverse(true); pulse.setCycleCount(2); pulse.play();
        }
        String messageInstant = getMessageInstant(state);
        String conseilInstant = getConseilInstant(state);
        state.setMessageIA(messageInstant);
        state.setConseil(conseilInstant);
        showDragonParlerDialog(coffre, state, true);
        dragonService.enrichirAvecIA(state, () -> Platform.runLater(() -> {
            playDragonSpeakSound();
            speakDragonMessage(state.getMessageIA());
            showDragonIAEnrichissement(coffre, state);
        }));
    }

    private String getMessageInstant(DragonState state) {
        if (state == null) return "Bonjour ! Je suis Draco, ton compagnon financier ! 🐉";
        if (state.isObjectifAtteint()) return "🎉 BRAVO ! Tu as atteint ton objectif ! Je suis tellement fier de toi ! On forme une équipe imbattable !";
        if (state.isAbandonDepot2Mois() || state.getHumeur() == DragonState.Humeur.TRISTE)
            return "😢 Pourquoi n'as-tu pas ajouté un petit montant dans ce coffre ? Cela fait si longtemps...";
        if (state.getHumeur() == DragonState.Humeur.CACHE)
            return "😡 C'est vraiment nul ! Ton compte est bloqué et je ne peux rien faire !";
        int pct = state.getProgressionPct();
        if (pct >= 60) return "💪 Wahou, tu progresses vraiment bien ! Continue comme ça !";
        if (pct >= 25) return "🌱 Je grandis grâce à toi ! Chaque petit dépôt me rend plus fort.";
        return "🐣 Bonjour ! Je suis tout petit mais je crois très fort en toi !";
    }

    private String getConseilInstant(DragonState state) {
        if (state == null) return "💡 Commence à épargner dès aujourd'hui !";
        if (state.isObjectifAtteint()) return "🏆 Crée un nouveau coffre pour ton prochain objectif !";
        if (state.isAbandonDepot2Mois() || state.getHumeur() == DragonState.Humeur.TRISTE)
            return "💬 Un petit dépôt régulier me ferait tellement plaisir !";
        if (state.getHumeur() == DragonState.Humeur.CACHE)
            return "⚠️ Contacte ton conseiller pour débloquer ton compte.";
        int pct = state.getProgressionPct();
        if (pct >= 60) return "🚀 Tu es à " + pct + "% ! Continue, l'objectif est proche !";
        if (pct >= 25) return "📈 " + pct + "% atteint. Un dépôt régulier accélère tout !";
        return "✨ Commence petit mais commence aujourd'hui !";
    }

    private void showDragonParlerDialog(CoffreVirtuel coffre, DragonState state, boolean isInstant) {
        DragonState.TamagotchiEtat etat = DragonState.calculerTamagotchiEtat(state);
        String heroEmoji;
        if (state.isObjectifAtteint())                                                     heroEmoji = "🎉";
        else if (state.isAbandonDepot2Mois() || state.getHumeur() == DragonState.Humeur.TRISTE) heroEmoji = "😢";
        else if (state.getHumeur() == DragonState.Humeur.CACHE)                            heroEmoji = "😡";
        else if (state.getProgressionPct() >= 60)                                          heroEmoji = "💪";
        else if (state.getProgressionPct() >= 25)                                          heroEmoji = "🌱";
        else                                                                               heroEmoji = "🐣";

        Stage dialogStage = new Stage(StageStyle.TRANSPARENT);
        dialogStage.initModality(Modality.NONE);
        dialogStage.setResizable(false);

        Image heroImg = getHeroAvatarForState(state);
        Node heroNode;
        if (heroImg != null && !heroImg.isError()) {
            ImageView iv = new ImageView(heroImg);
            iv.setFitWidth(90); iv.setFitHeight(90); iv.setPreserveRatio(true); iv.setSmooth(true);
            heroNode = iv;
        } else {
            Label lbl = new Label(heroEmoji); lbl.setStyle("-fx-font-size: 56px;"); heroNode = lbl;
        }
        StackPane heroPane = new StackPane(heroNode); heroPane.setMinSize(90, 90); heroPane.setMaxSize(90, 90);
        appliquerAnimationHero(heroPane, state, state.getProgressionPct());

        Label titleLbl = new Label(heroEmoji + " Draco — " + coffre.getNom());
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e0e7ff;");
        Label niveauLbl = new Label(state.getNiveau().emoji + " " + state.getNiveau().label + "  ·  " + state.getHumeur().emoji + " " + state.getHumeur().label + "  ·  " + state.getProgressionPct() + "%");
        niveauLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #818cf8;");
        Label messageLbl = new Label(state.getMessageIA());
        messageLbl.setWrapText(true); messageLbl.setMaxWidth(320);
        messageLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #e0e7ff; -fx-font-style: italic; -fx-padding: 0 0 4 0;");
        Label iaBadge = new Label(isInstant ? "⏳ Draco réfléchit encore..." : "✅ Message enrichi par IA");
        iaBadge.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (isInstant ? "#F59E0B" : "#22C55E") + ";-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 6; -fx-padding: 3 8;");
        Label conseilLbl = new Label(state.getConseil());
        conseilLbl.setWrapText(true); conseilLbl.setMaxWidth(320);
        conseilLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #fbbf24; -fx-padding: 8 12;-fx-background-color: rgba(245,158,11,0.1); -fx-background-radius: 8;");
        ProgressBar pb = new ProgressBar(state.getProgressionPct() / 100.0);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setStyle("-fx-accent: #6366f1; -fx-background-color: rgba(99,102,241,0.15);-fx-background-radius: 6; -fx-pref-height: 8;");
        Label pctLbl = new Label(state.getProgressionPct() + "% — " + String.format("%.0f / %.0f DT", state.getMontantActuel(), state.getObjectifMontant()));
        pctLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #818cf8;");
        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color: rgba(99,102,241,0.3); -fx-text-fill: white;-fx-border-color: #6366f1; -fx-border-radius: 8; -fx-background-radius: 8;-fx-font-size: 12px; -fx-padding: 7 18; -fx-cursor: hand;");
        btnFermer.setOnAction(e -> dialogStage.close());
        Button btnNourrir = new Button("💰 Nourrir Draco");
        btnNourrir.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white;-fx-background-radius: 8; -fx-font-size: 12px; -fx-padding: 7 18;-fx-cursor: hand; -fx-font-weight: bold;");
        btnNourrir.setOnAction(e -> { dialogStage.close(); populateCoffreForm(coffre); });
        HBox btnsBox = new HBox(10, btnFermer, btnNourrir); btnsBox.setAlignment(Pos.CENTER_RIGHT);
        VBox rightBox = new VBox(10, titleLbl, niveauLbl, messageLbl, iaBadge, conseilLbl, new VBox(4, pb, pctLbl), btnsBox);
        rightBox.setMaxWidth(340);
        HBox content = new HBox(16, heroPane, rightBox); content.setAlignment(Pos.TOP_LEFT);
        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: #818cf8;-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0;");
        btnX.setOnAction(e -> dialogStage.close());
        Region topSpacer = new Region(); HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topBar = new HBox(topSpacer, btnX);
        VBox root = new VBox(6, topBar, content);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f172a, #1e1b4b);-fx-background-radius: 16; -fx-border-color: #6366f1; -fx-border-width: 1.5;-fx-border-radius: 16; -fx-padding: 14 18;-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.6), 24, 0, 0, 6);");
        Scene scene = new Scene(root, 480, 280); scene.setFill(null);
        dialogStage.setScene(scene);
        root.setOpacity(0); root.setTranslateY(20);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(280), root); fadeIn.setToValue(1);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(280), root); slideIn.setToY(0);
        new ParallelTransition(fadeIn, slideIn).play();
        dialogStage.show();
    }

    private void showDragonIAEnrichissement(CoffreVirtuel coffre, DragonState state) {
        Stage miniStage = new Stage(StageStyle.TRANSPARENT);
        miniStage.initModality(Modality.NONE);
        Label icon = new Label("✨"); icon.setStyle("-fx-font-size: 16px;");
        Label msg = new Label("Draco a un nouveau message : " + state.getMessageIA());
        msg.setWrapText(true); msg.setMaxWidth(280);
        msg.setStyle("-fx-font-size: 12px; -fx-text-fill: #e0e7ff;");
        Button btnVoir = new Button("Voir");
        btnVoir.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white;-fx-background-radius: 8; -fx-font-size: 11px; -fx-padding: 4 12; -fx-cursor: hand;");
        btnVoir.setOnAction(e -> { miniStage.close(); showDragonParlerDialog(coffre, state, false); });
        Button btnIgnorer = new Button("Ignorer");
        btnIgnorer.setStyle("-fx-background-color: transparent; -fx-text-fill: #818cf8;-fx-border-color: #818cf8; -fx-border-radius: 8; -fx-background-radius: 8;-fx-font-size: 11px; -fx-padding: 4 12; -fx-cursor: hand;");
        btnIgnorer.setOnAction(e -> miniStage.close());
        HBox btns = new HBox(8, btnIgnorer, btnVoir); btns.setAlignment(Pos.CENTER_RIGHT);
        VBox root = new VBox(8, new HBox(8, icon, msg), btns);
        root.setStyle("-fx-background-color: linear-gradient(to right, #1e1b4b, #0f172a);-fx-background-radius: 12; -fx-border-color: #6366f1; -fx-border-width: 1;-fx-border-radius: 12; -fx-padding: 12 16;-fx-effect: dropshadow(gaussian, rgba(99,102,241,0.5), 16, 0, 0, 4);");
        Scene scene = new Scene(root, 340, 110); scene.setFill(null);
        miniStage.setScene(scene);
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        miniStage.setX(screenBounds.getMaxX() - 360);
        miniStage.setY(screenBounds.getMaxY() - 130);
        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), root); ft.setToValue(1); ft.play();
        miniStage.show();
        new Timeline(new KeyFrame(Duration.seconds(8), e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), root);
            fadeOut.setToValue(0); fadeOut.setOnFinished(ev -> miniStage.close()); fadeOut.play();
        })).play();
    }

    private void playDragonSpeakSound() {
        try {
            java.net.URL url = getClass().getResource("/sounds/dragon_speak.wav");
            if (url != null) {
                try {
                    Class<?> audioClipClass = Class.forName("javafx.scene.media.AudioClip");
                    Object clip = audioClipClass.getConstructor(String.class).newInstance(url.toExternalForm());
                    audioClipClass.getMethod("play").invoke(clip);
                    return;
                } catch (Throwable t) { }
            }
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Throwable ignored) { }
    }

    private void speakDragonMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;
        String text = message.trim().replace("\r", " ").replace("\n", " ");
        if (text.length() > 350) text = text.substring(0, 347) + "...";
        final String toSpeak = text;
        new Thread(() -> {
            try {
                if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                    java.io.File tmp = java.io.File.createTempFile("draco_tts_", ".txt");
                    try {
                        java.nio.file.Files.write(tmp.toPath(), toSpeak.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        String path = tmp.getAbsolutePath().replace("'", "''");
                        String ps = "Add-Type -AssemblyName System.Speech; $s = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                                + "try { $s.SelectVoiceByHints([System.Speech.Synthesis.VoiceGender]::Neutral, [System.Speech.Synthesis.VoiceAge]::Adult, 0, [System.Globalization.CultureInfo]::GetCultureInfo('fr-FR')) } catch {}; "
                                + "$s.Speak([System.IO.File]::ReadAllText('" + path + "', [System.Text.Encoding]::UTF8))";
                        ProcessBuilder pb2 = new ProcessBuilder("powershell", "-NoProfile", "-Command", ps);
                        pb2.redirectErrorStream(true);
                        Process p = pb2.start(); p.waitFor();
                    } finally { tmp.delete(); }
                }
            } catch (Throwable t) { System.err.println("[Draco TTS] " + t.getMessage()); }
        }, "Draco-TTS").start();
    }

    private void populateCoffreForm(CoffreVirtuel coffre) {
        selectedCoffre = coffre; isEditCoffreMode = true;
        if (txtCoffreName      != null) txtCoffreName.setText(coffre.getNom());
        if (txtCoffreObjectif  != null) txtCoffreObjectif.setText(String.valueOf(coffre.getObjectifMontant()));
        if (txtCoffreDepotInitial != null) txtCoffreDepotInitial.setText(String.valueOf(coffre.getMontantActuel()));
        if (cbCoffreStatut     != null) cbCoffreStatut.setValue(coffre.getStatus());
        if (chkVerrouiller     != null) chkVerrouiller.setSelected(coffre.isEstVerrouille());
        if (dpCoffreDateCible  != null) {
            try { dpCoffreDateCible.setValue((coffre.getDateObjectifs() != null && !coffre.getDateObjectifs().isEmpty()) ? LocalDate.parse(coffre.getDateObjectifs()) : null); }
            catch (Exception ex) { dpCoffreDateCible.setValue(null); }
        }
        if (cmbCoffreCompte != null) { for (CompteBancaire c : allAccounts) { if (c.getIdCompte() == coffre.getIdCompte()) { cmbCoffreCompte.setValue(c); break; } } }
        if (lblCoffreFormTitle    != null) lblCoffreFormTitle.setText("Modifier le coffre virtuel");
        if (lblCoffreFormSubtitle != null) lblCoffreFormSubtitle.setText("Modifier : " + coffre.getNom());
        if (lblCoffreBtnText      != null) lblCoffreBtnText.setText("Enregistrer les modifications");
        if (coffreFormContainer != null && !coffreFormContainer.isVisible()) { setVisibleManaged(coffreFormContainer, true); animateSlideIn(coffreFormContainer); }
    }

    private void deleteCoffre(CoffreVirtuel coffre) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le coffre"); confirm.setHeaderText("Supprimer \"" + coffre.getNom() + "\" ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                HistoryManager.getInstance().add(ActionType.COFFRE_SUPPRIME,
                        "Nom: " + coffre.getNom() + " | Objectif: " + String.format("%.2f", coffre.getObjectifMontant()) + " DT" +
                                " | Montant atteint: " + String.format("%.2f", coffre.getMontantActuel()) + " DT" +
                                " | Statut: " + coffre.getStatus() + " | Verrouillé: " + (coffre.isEstVerrouille() ? "Oui" : "Non"));
                coffreService.remove(coffre);
                if (selectedCoffre != null && selectedCoffre.getIdCoffre() == coffre.getIdCoffre()) { clearCoffreFormAndResetMode(); setVisibleManaged(coffreFormContainer, false); }
                if (compteAfficheCoffres != null) handleShowCoffres(compteAfficheCoffres);
                updateKPIs();
                showNotification("Supprimé", "Le coffre a été supprimé.");
            }
        });
    }

    @FXML private void showCoffres() { if (!coffresDisplayContainer.isVisible()) { setVisibleManaged(coffresDisplayContainer, true); animateSlideIn(coffresDisplayContainer); } }
    @FXML private void hideCoffres() { animateSlideOut(coffresDisplayContainer, () -> { setVisibleManaged(coffresDisplayContainer, false); compteAfficheCoffres = null; }); }

    @FXML private void toggleCoffreForm() {
        boolean show = !coffreFormContainer.isVisible();
        if (show) {
            clearCoffreFormAndResetMode();
            setVisibleManaged(coffreFormContainer, true);
            animateSlideIn(coffreFormContainer);
            CompteBancaire defaultAccount = selectedCompte != null ? selectedCompte : allAccounts.stream().findFirst().orElse(null);
            if (defaultAccount != null && cmbCoffreCompte != null) cmbCoffreCompte.setValue(defaultAccount);
            if (!coffresDisplayContainer.isVisible()) { setVisibleManaged(coffresDisplayContainer, true); animateSlideIn(coffresDisplayContainer); }
            loadAISuggestionsRight();
        } else { animateSlideOut(coffreFormContainer, () -> { setVisibleManaged(coffreFormContainer, false); clearCoffreFormAndResetMode(); }); }
    }
    @FXML private void hideCoffreForm() { animateSlideOut(coffreFormContainer, () -> { setVisibleManaged(coffreFormContainer, false); clearCoffreFormAndResetMode(); }); }

    private boolean validateCoffreForm() {
        boolean valid = true; clearCoffreErrors();
        if (cmbCoffreCompte.getValue() == null) { lblCoffreCompteError.setText("Veuillez sélectionner un compte."); valid = false; }
        String nom = txtCoffreName.getText().trim();
        if (nom.isEmpty()) { lblCoffreNameError.setText("Le nom est obligatoire."); valid = false; }
        else if (nom.length() < 3 || nom.length() > 50) { lblCoffreNameError.setText("Le nom doit faire entre 3 et 50 caractères."); valid = false; }
        double objectif = 0;
        try { objectif = Double.parseDouble(txtCoffreObjectif.getText().trim());
            if (objectif <= 0) { lblCoffreObjectifError.setText("L'objectif doit être > 0."); valid = false; }
            else if (objectif > 1_000_000) { lblCoffreObjectifError.setText("Max 1 000 000."); valid = false; }
        } catch (NumberFormatException e) { lblCoffreObjectifError.setText("Montant invalide."); valid = false; }
        double depot = 0;
        try { depot = Double.parseDouble(txtCoffreDepotInitial.getText().trim());
            if (depot < 0) { lblCoffreDepotError.setText("Le dépôt ne peut pas être négatif."); valid = false; }
            else if (depot > objectif) { lblCoffreDepotError.setText("Le dépôt dépasse l'objectif."); valid = false; }
        } catch (NumberFormatException e) { lblCoffreDepotError.setText("Montant invalide."); valid = false; }
        if (dpCoffreDateCible.getValue() != null && dpCoffreDateCible.getValue().isBefore(LocalDate.now())) { lblCoffreDateError.setText("La date doit être dans le futur."); valid = false; }
        if (cbCoffreStatut.getValue() == null) { lblCoffreStatutError.setText("Statut obligatoire."); valid = false; }
        return valid;
    }

    private void clearCoffreErrors() {
        lblCoffreCompteError.setText(""); lblCoffreNameError.setText(""); lblCoffreObjectifError.setText("");
        lblCoffreDepotError.setText(""); lblCoffreDateError.setText(""); lblCoffreStatutError.setText("");
    }

    @FXML private void saveCoffre() {
        if (!validateCoffreForm()) return;
        CompteBancaire selectedAccount = cmbCoffreCompte.getValue();
        if (selectedAccount == null) { showNotification("Erreur", "Veuillez sélectionner un compte."); return; }
        int idUser = SessionManager.getInstance().getCurrentUserId();
        String nom = txtCoffreName.getText().trim();
        double objectif = parseDouble(txtCoffreObjectif.getText()), depotInitial = parseDouble(txtCoffreDepotInitial.getText());
        String dateCible = dpCoffreDateCible.getValue() != null ? dpCoffreDateCible.getValue().format(DateTimeFormatter.ISO_DATE) : "";
        String statut = cbCoffreStatut.getValue() != null ? cbCoffreStatut.getValue() : "Actif";
        boolean verrouille = chkVerrouiller.isSelected();
        if (isEditCoffreMode && selectedCoffre != null) {
            double ancienMontant = selectedCoffre.getMontantActuel();
            selectedCoffre.setNom(nom); selectedCoffre.setObjectifMontant(objectif); selectedCoffre.setMontantActuel(depotInitial);
            if (depotInitial > ancienMontant) selectedCoffre.setDateDernierDepot(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
            selectedCoffre.setDateObjectifs(dateCible); selectedCoffre.setStatus(statut);
            selectedCoffre.setEstVerrouille(verrouille); selectedCoffre.setIdCompte(selectedAccount.getIdCompte()); selectedCoffre.setIdUser(idUser);
            coffreService.edit(selectedCoffre);
            HistoryManager.getInstance().add(ActionType.COFFRE_MODIFIE, "Nom: " + nom + " | Objectif: " + String.format("%.2f", objectif) + " DT | Montant actuel: " + String.format("%.2f", depotInitial) + " DT | Compte: " + selectedAccount.getNumeroCompte());
            showNotification("Modifié", "Coffre '" + nom + "' mis à jour.");
        } else {
            CoffreVirtuel nouveau = new CoffreVirtuel(nom, objectif, depotInitial, LocalDate.now().format(DateTimeFormatter.ISO_DATE), dateCible, statut, verrouille, selectedAccount.getIdCompte(), idUser);
            if (depotInitial > 0) nouveau.setDateDernierDepot(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
            coffreService.add(nouveau);
            HistoryManager.getInstance().add(ActionType.COFFRE_AJOUTE, "Nom: " + nom + " | Objectif: " + String.format("%.2f", objectif) + " DT | Dépôt initial: " + String.format("%.2f", depotInitial) + " DT | Compte: " + selectedAccount.getNumeroCompte());
            showNotification("Succès", "Coffre '" + nom + "' créé.");
        }
        CompteBancaire compteToRefresh = selectedAccount != null ? selectedAccount : compteAfficheCoffres;
        if (compteToRefresh != null) handleShowCoffres(compteToRefresh);
        updateKPIs(); hideCoffreForm(); clearCoffreFormAndResetMode();
    }

    private void clearCoffreForm() {
        if (txtCoffreName != null) txtCoffreName.clear(); if (txtCoffreObjectif != null) txtCoffreObjectif.clear();
        if (txtCoffreDepotInitial != null) txtCoffreDepotInitial.clear(); if (dpCoffreDateCible != null) dpCoffreDateCible.setValue(null);
        if (cbCoffreStatut != null) cbCoffreStatut.setValue(null); if (chkVerrouiller != null) chkVerrouiller.setSelected(false);
        if (chkDepotAuto != null) chkDepotAuto.setSelected(false); if (cmbCoffreCompte != null) cmbCoffreCompte.setValue(null);
    }
    private void clearCoffreFormAndResetMode() {
        clearCoffreForm(); selectedCoffre = null; isEditCoffreMode = false;
        if (lblCoffreFormTitle    != null) lblCoffreFormTitle.setText("Configuration du coffre virtuel");
        if (lblCoffreFormSubtitle != null) lblCoffreFormSubtitle.setText("Créer un nouvel objectif d epargne");
        if (lblCoffreBtnText      != null) lblCoffreBtnText.setText("Créer le coffre");
    }

    @FXML private void addNewAccount() { clearForm(); selectedCompte = null; }

    private boolean validateAccountForm() {
        boolean valid = true; clearAccountErrors();
        String numero = txtAccountNumber.getText().trim();
        if (numero.isEmpty()) { lblAccountNumberError.setText("Le numéro est obligatoire."); valid = false; }
        else if (!numero.matches("CB-\\d{3}")) { lblAccountNumberError.setText("Format requis : CB- suivi de 3 chiffres (ex: CB-123)"); valid = false; }
        try { double s = Double.parseDouble(txtBalance.getText().trim()); if (s < 0) { lblBalanceError.setText("Solde négatif."); valid = false; } }
        catch (NumberFormatException e) { lblBalanceError.setText("Solde invalide."); valid = false; }
        if (dpOpeningDate.getValue() == null) { lblOpeningDateError.setText("Date obligatoire."); valid = false; }
        else if (dpOpeningDate.getValue().isAfter(LocalDate.now())) { lblOpeningDateError.setText("Date dans le futur."); valid = false; }
        if (cbStatus.getValue() == null) { lblStatusError.setText("Statut obligatoire."); valid = false; }
        if (cbType.getValue() == null)   { lblTypeError.setText("Type obligatoire."); valid = false; }
        try { double p = Double.parseDouble(txtWithdrawLimit.getText().trim()); if (p < 0) { lblWithdrawLimitError.setText("Plafond négatif."); valid = false; } }
        catch (NumberFormatException e) { lblWithdrawLimitError.setText("Plafond invalide."); valid = false; }
        try { double p = Double.parseDouble(txtTransferLimit.getText().trim()); if (p < 0) { lblTransferLimitError.setText("Plafond négatif."); valid = false; } }
        catch (NumberFormatException e) { lblTransferLimitError.setText("Plafond invalide."); valid = false; }
        return valid;
    }

    private void clearAccountErrors() {
        lblAccountNumberError.setText(""); lblBalanceError.setText(""); lblOpeningDateError.setText("");
        lblStatusError.setText(""); lblTypeError.setText(""); lblWithdrawLimitError.setText(""); lblTransferLimitError.setText("");
    }

    @FXML
    private void saveAccount() {
        if (!validateAccountForm()) return;
        int    idUser  = SessionManager.getInstance().getCurrentUserId();
        String numero  = txtAccountNumber.getText().trim();
        double solde   = parseDouble(txtBalance.getText().trim());
        double plafRet = parseDouble(txtWithdrawLimit.getText().trim());
        double plafVir = parseDouble(txtTransferLimit.getText().trim());
        String dateStr = dpOpeningDate.getValue() != null ? dpOpeningDate.getValue().format(DateTimeFormatter.ISO_DATE) : "";
        String type    = cbType.getValue();
        if (selectedCompte != null) {
            String statut = cbStatus.getValue();
            CompteBancaire updated = new CompteBancaire(selectedCompte.getIdCompte(), numero, solde, dateStr, statut, plafRet, plafVir, type, idUser);
            service.edit(updated);
            HistoryManager.getInstance().add(ActionType.COMPTE_MODIFIE, "Numéro: " + numero + " | Type: " + type + " | Solde: " + String.format("%.2f", solde) + " DT | Statut: " + statut);
            showNotification("Modifié", "Compte mis à jour avec succès.");
            clearForm(); selectedCompte = null;
            loadAccountsFromDB();
        } else {
            CompteBancaire nouveau = new CompteBancaire(numero, solde, dateStr, "En attente", plafRet, plafVir, type, idUser);
            service.add(nouveau);
            HistoryManager.getInstance().add(ActionType.COMPTE_AJOUTE, "Numéro: " + numero + " | Type: " + type + " | Solde: " + String.format("%.2f", solde) + " DT | Statut: En attente");
            clearForm(); selectedCompte = null;
            loadAccountsFromDB();
            showDialoguePendant();
        }
    }

    private void showDialoguePendant() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Demande envoyée");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        FontIcon clockIco = new FontIcon("fas-clock");
        clockIco.setStyle("-fx-fill: #F59E0B; -fx-icon-size: 44;");
        Label title = new Label("Demande transmise à l'administrateur !");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #0A2540;");
        title.setWrapText(true);
        Label desc = new Label("Votre demande d'ouverture de compte a bien été reçue.\n\nVotre compte apparaît sur votre tableau de bord\navec le statut « En attente » jusqu'à validation.\n\nDès que l'administrateur approuvera la demande,\nvotre compte deviendra automatiquement actif.");
        desc.setStyle("-fx-text-fill: #4B5563; -fx-font-size: 13px;"); desc.setWrapText(true);
        VBox content = new VBox(14, clockIco, title, desc);
        content.setAlignment(Pos.CENTER); content.setPadding(new Insets(20)); content.setPrefWidth(360);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-background-radius: 14;");
        dialog.showAndWait();
    }

    private static class AISuggestion {
        String name; double goalAmount; int months; double monthlyRecommended; int relevanceScore; String justification; String source;
    }

    private static class FinancialProfile {
        double monthlyIncome; double monthlyExpenses; double currentBalance;
        double monthlyCreditPayments; double remainingCreditAmount; int existingGoalsCount;
    }

    private FinancialProfile buildFinancialProfile() {
        int idUser = SessionManager.getInstance().getCurrentUserId();
        double balance = allAccounts.stream().mapToDouble(CompteBancaire::getSolde).sum();
        List<Credit> credits = creditService.getCreditsByUser(idUser);
        double monthlyCredit = credits.stream().mapToDouble(Credit::getMensualite).sum();
        double remainingCredit = credits.stream().mapToDouble(Credit::getMontantRestant).sum();
        int goalsCount = coffreService.getByUser(idUser).size();
        double income = 0.0, expenses = 0.0;
        try {
            Connection conn = MyDB.getInstance().getConn();
            String sql = "SELECT t.typeTransaction, SUM(t.montant) AS total FROM transactions t JOIN compte c ON t.idCompte = c.idCompte WHERE c.idUser = ? GROUP BY t.typeTransaction";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idUser);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String type = rs.getString("typeTransaction");
                double total = rs.getDouble("total");
                if ("CREDIT".equalsIgnoreCase(type)) income += total / 3.0;
                else if ("DEBIT".equalsIgnoreCase(type)) expenses += total / 3.0;
            }
            rs.close(); ps.close();
        } catch (SQLException ex) { System.err.println("[AI] Erreur SQL: " + ex.getMessage()); }
        FinancialProfile fp = new FinancialProfile();
        fp.monthlyIncome = income; fp.monthlyExpenses = expenses; fp.currentBalance = balance;
        fp.monthlyCreditPayments = monthlyCredit; fp.remainingCreditAmount = remainingCredit; fp.existingGoalsCount = goalsCount;
        return fp;
    }

    private void loadAISuggestionsRight() {
        prepareSuggestionTarget(aiSuggestionsSectionRight, aiSuggestionsFlowRight);
        new Thread(() -> {
            FinancialProfile fp = buildFinancialProfile();
            List<AISuggestion> suggestions = fetchAISuggestions(fp);
            javafx.application.Platform.runLater(() -> renderInto(aiSuggestionsFlowRight, suggestions));
        }).start();
    }

    private void prepareSuggestionTarget(VBox section, FlowPane flow) {
        if (flow != null) flow.getChildren().clear();
        if (section != null) { section.setVisible(true); section.setManaged(true); }
        Label loading = new Label("Chargement des recommandations AI...");
        loading.getStyleClass().add("vaults-section-subtitle");
        if (flow != null) flow.getChildren().add(loading);
    }

    private void renderInto(FlowPane flow, List<AISuggestion> suggestions) {
        if (flow == null) return;
        flow.getChildren().clear();
        if (suggestions == null || suggestions.isEmpty()) suggestions = generateOfflineSuggestions(buildFinancialProfile());
        renderAISuggestionCardsInto(flow, suggestions);
    }

    private List<AISuggestion> fetchAISuggestions(FinancialProfile fp) {
        ArrayList<AISuggestion> list = new ArrayList<>();
        try {
            String apiKey = System.getenv("NEXORA_GROQ_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) apiKey = System.getenv("GROQ_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) return list;
            JsonObject userData = new JsonObject();
            userData.addProperty("revenu_mensuel_DT", fp.monthlyIncome);
            userData.addProperty("depenses_mensuelles_DT", fp.monthlyExpenses);
            userData.addProperty("solde_total_DT", fp.currentBalance);
            userData.addProperty("mensualite_credit_DT", fp.monthlyCreditPayments);
            userData.addProperty("montant_restant_credit_DT", fp.remainingCreditAmount);
            userData.addProperty("nombre_coffres_existants", fp.existingGoalsCount);
            String systemPrompt = "Tu es un assistant financier expert. Tu analyses les donnees financieres reelles d un client bancaire tunisien et tu proposes des coffres d epargne personnalises. Tu reponds UNIQUEMENT en JSON valide, sans aucun texte avant ou apres, sans markdown.";
            String userPrompt = "Voici les donnees financieres reelles du client : " + userData.toString() + "\n\nAnalyse cette situation et propose EXACTEMENT 3 coffres d epargne personnalises. Reponds UNIQUEMENT avec ce JSON exact :\n{\"proposals\":[{\"name\":\"...\",\"goal_amount\":1234.50,\"months\":12,\"monthly_recommended\":102.87,\"relevance_score\":85,\"justification\":\"...\"}]}";
            JsonArray messages = new JsonArray();
            JsonObject sysMsg = new JsonObject(); sysMsg.addProperty("role", "system"); sysMsg.addProperty("content", systemPrompt);
            JsonObject userMsg = new JsonObject(); userMsg.addProperty("role", "user"); userMsg.addProperty("content", userPrompt);
            messages.add(sysMsg); messages.add(userMsg);
            JsonObject body = new JsonObject(); body.add("messages", messages);
            body.addProperty("model", "llama-3.3-70b-versatile"); body.addProperty("stream", false);
            body.addProperty("temperature", 0.7); body.addProperty("max_tokens", 1000);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Content-Type", "application/json").header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            String content = null;
            JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (root.has("error")) return list;
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                if (message != null && message.has("content")) content = message.get("content").getAsString();
            }
            if (content == null || content.isEmpty()) return list;
            String clean = content.trim();
            if (clean.startsWith("```")) { int firstNl = clean.indexOf('\n'); if (firstNl != -1) clean = clean.substring(firstNl + 1); if (clean.endsWith("```")) clean = clean.substring(0, clean.lastIndexOf("```")).trim(); }
            JsonObject parsed = JsonParser.parseString(clean).getAsJsonObject();
            JsonArray proposals = parsed.getAsJsonArray("proposals");
            if (proposals == null) return list;
            for (JsonElement el : proposals) {
                JsonObject o = el.getAsJsonObject();
                AISuggestion s = new AISuggestion();
                s.name = safeString(o, "name"); s.goalAmount = safeDouble(o, "goal_amount"); s.months = safeInt(o, "months");
                s.monthlyRecommended = safeDouble(o, "monthly_recommended"); s.relevanceScore = (int) Math.round(safeDouble(o, "relevance_score"));
                s.justification = safeString(o, "justification"); s.source = "IA"; list.add(s);
            }
        } catch (Exception e) { System.err.println("[AI] Erreur Groq: " + e.getMessage()); }
        return list;
    }

    private String safeString(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
    private double safeDouble(JsonObject o, String k) { try { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0.0; } catch (Exception e) { return 0.0; } }
    private int safeInt(JsonObject o, String k) { try { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : 0; } catch (Exception e) { return 0; } }

    private void renderAISuggestionCardsInto(FlowPane flow, List<AISuggestion> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            Label empty = new Label("Aucune recommandation disponible pour le moment.");
            empty.getStyleClass().add("vaults-section-subtitle"); flow.getChildren().add(empty); return;
        }
        for (AISuggestion s : suggestions) flow.getChildren().add(buildSuggestionCard(s));
    }

    private VBox buildSuggestionCard(AISuggestion s) {
        FontIcon wand = new FontIcon("fas-magic"); wand.setStyle("-fx-fill: #14C9B3; -fx-icon-size: 14;");
        Label title = new Label(s.name); title.getStyleClass().add("ai-card-title");
        Label sub = new Label("Proposition intelligente"); sub.getStyleClass().add("ai-card-subtitle");
        Region headerSpacer = new Region(); HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Label scoreChip = new Label("⭐ " + Math.max(0, Math.min(100, s.relevanceScore)) + "%");
        StackPane scoreBadge = new StackPane(scoreChip); scoreBadge.getStyleClass().add("ai-score-badge");
        Label sourceChipLbl = new Label(s.source != null ? s.source : "");
        StackPane sourceChip = new StackPane(sourceChipLbl); sourceChip.getStyleClass().add("ai-source-chip");
        HBox header = new HBox(10, wand, new VBox(2, title, sub), headerSpacer, sourceChip, scoreBadge);
        header.getStyleClass().add("ai-card-header"); header.setAlignment(Pos.CENTER_LEFT);
        Label objectifLbl = new Label(String.format(Locale.US, "Objectif : %,.2f DT", s.goalAmount)); objectifLbl.getStyleClass().add("ai-metric");
        Label dureeLbl = new Label("Durée estimée : " + s.months + " mois"); dureeLbl.getStyleClass().add("ai-metric");
        Label monthlyLbl = new Label(String.format(Locale.US, "Montant recommandé : %,.2f DT/mois", s.monthlyRecommended)); monthlyLbl.getStyleClass().add("ai-metric-strong");
        Label justLbl = new Label(s.justification); justLbl.getStyleClass().add("ai-justify"); justLbl.setWrapText(true);
        TextField objectifEdit = new TextField(String.valueOf(s.goalAmount)); objectifEdit.setPromptText("Montant cible (DT)");
        TextField monthsEdit = new TextField(String.valueOf(s.months)); monthsEdit.setPromptText("Durée (mois)");
        HBox editRow = new HBox(8, objectifEdit, monthsEdit); editRow.setAlignment(Pos.CENTER_LEFT);
        Button addBtn = new Button("Ajouter ce coffre"); addBtn.getStyleClass().add("ai-add-btn");
        Button refuseBtn = new Button("Refuser"); refuseBtn.getStyleClass().add("ai-refuse-btn");
        HBox actionsRow = new HBox(8, addBtn, refuseBtn); actionsRow.getStyleClass().add("ai-actions");
        VBox body = new VBox(8, objectifLbl, dureeLbl, monthlyLbl, justLbl, editRow); body.getStyleClass().add("ai-card-body");
        VBox card = new VBox(header, body, actionsRow); card.getStyleClass().add("ai-card"); card.setPrefWidth(280);
        addBtn.setOnAction(e -> {
            double goal = parseDouble(objectifEdit.getText());
            int months = 0; try { months = Integer.parseInt(monthsEdit.getText().trim()); } catch (Exception ex) { months = s.months; }
            CompteBancaire account = cmbCoffreCompte.getValue() != null ? cmbCoffreCompte.getValue()
                    : (selectedCompte != null ? selectedCompte : allAccounts.stream().findFirst().orElse(null));
            if (account == null) { showNotification("Erreur", "Aucun compte sélectionné."); return; }
            String dateCible = months > 0 ? LocalDate.now().plusMonths(months).format(DateTimeFormatter.ISO_DATE) : "";
            int idUser = SessionManager.getInstance().getCurrentUserId();
            CoffreVirtuel nouveau = new CoffreVirtuel(s.name, goal, 0.0, LocalDate.now().format(DateTimeFormatter.ISO_DATE), dateCible, "Actif", false, account.getIdCompte(), idUser);
            coffreService.add(nouveau);
            if (aiSuggestionsFlowRight != null) { aiSuggestionsFlowRight.getChildren().remove(card); if (aiSuggestionsFlowRight.getChildren().isEmpty()) loadAISuggestionsRight(); }
            handleShowCoffres(account); updateKPIs();
            showNotification("Coffre ajouté", "Le coffre '" + s.name + "' a été créé avec succès !");
        });
        refuseBtn.setOnAction(e -> {
            if (aiSuggestionsFlowRight != null) { aiSuggestionsFlowRight.getChildren().remove(card); if (aiSuggestionsFlowRight.getChildren().isEmpty()) loadAISuggestionsRight(); }
        });
        return card;
    }

    private List<AISuggestion> generateOfflineSuggestions(FinancialProfile fp) {
        ArrayList<AISuggestion> list = new ArrayList<>();
        double capacity = Math.max(0.0, fp.monthlyIncome - fp.monthlyExpenses - fp.monthlyCreditPayments);
        if (capacity <= 0) capacity = 200.0;
        String[] names = {"Fonds d'urgence", "Projet personnel", "Epargne long terme"};
        String[] descriptions = {
                "Une reserve de 3 a 6 mois de depenses est le premier objectif financier recommande.",
                "Definissez un objectif concret et epargnez regulierement.",
                "L epargne long terme genere des interets composes. Commencer tot fait toute la difference."
        };
        double[] percents = {0.20, 0.30, 0.40};
        int[] durations = {6, 8, 12};
        for (int i = 0; i < 3; i++) {
            AISuggestion s = new AISuggestion();
            s.name = names[i]; s.monthlyRecommended = Math.max(50, Math.round(capacity * percents[i]));
            s.months = durations[i]; s.goalAmount = Math.round(s.monthlyRecommended * s.months);
            s.relevanceScore = (int)Math.min(100, Math.round(60 + percents[i] * 100));
            s.justification = descriptions[i]; s.source = "Offline"; list.add(s);
        }
        return list;
    }

    @FXML
    private void deleteAccount() {
        if (selectedCompte == null) { showNotification("Erreur", "Sélectionnez un compte."); return; }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer"); alert.setHeaderText("Supprimer ce compte ?"); alert.setContentText("Cette action est irréversible.");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                HistoryManager.getInstance().add(ActionType.COMPTE_SUPPRIME, "Numéro: " + selectedCompte.getNumeroCompte() + " | Type: " + selectedCompte.getTypeCompte() + " | Solde: " + String.format("%.2f", selectedCompte.getSolde()) + " DT");
                service.remove(selectedCompte); clearForm(); selectedCompte = null; loadAccountsFromDB(); showNotification("Supprimé", "Compte supprimé.");
            }
        });
    }

    @FXML private void clearForm() {
        if (txtAccountNumber != null) txtAccountNumber.clear(); if (txtBalance != null) txtBalance.clear();
        if (dpOpeningDate != null) dpOpeningDate.setValue(null); if (cbStatus != null) cbStatus.getSelectionModel().clearSelection();
        if (cbType != null) cbType.getSelectionModel().clearSelection(); if (txtWithdrawLimit != null) txtWithdrawLimit.clear();
        if (txtTransferLimit != null) txtTransferLimit.clear(); selectedCompte = null;
    }
    @FXML private void refreshData() { loadAccountsFromDB(); showNotification("Actualisé", "Données actualisées."); }
    @FXML private void selectAccount(MouseEvent event) { }

    private void populateForm(CompteBancaire c) {
        selectedCompte = c;
        if (txtAccountNumber != null) txtAccountNumber.setText(c.getNumeroCompte());
        if (txtBalance != null)       txtBalance.setText(String.valueOf(c.getSolde()));
        if (txtWithdrawLimit != null) txtWithdrawLimit.setText(String.valueOf(c.getPlafondRetrait()));
        if (txtTransferLimit != null) txtTransferLimit.setText(String.valueOf(c.getPlafondVirement()));
        if (cbStatus != null)         cbStatus.setValue(c.getStatutCompte());
        if (cbType != null)           cbType.setValue(c.getTypeCompte());
        if (dpOpeningDate != null) {
            try { dpOpeningDate.setValue(c.getDateOuverture() != null && !c.getDateOuverture().isEmpty() ? LocalDate.parse(c.getDateOuverture()) : null); }
            catch (Exception ex) { dpOpeningDate.setValue(null); }
        }
    }

    private double parseDouble(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
    }

    private void animateSlideIn(VBox node) {
        if (node == null) return;
        node.setOpacity(0); node.setTranslateY(-20);
        FadeTransition fade = new FadeTransition(ANIM_DUR, node); fade.setFromValue(0); fade.setToValue(1); fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(ANIM_DUR, node); slide.setFromY(-20); slide.setToY(0); slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }
    private void animateSlideOut(VBox node, Runnable onFinished) {
        if (node == null) { if (onFinished != null) onFinished.run(); return; }
        FadeTransition fade = new FadeTransition(Duration.millis(150), node); fade.setFromValue(1); fade.setToValue(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(150), node); slide.setFromY(0); slide.setToY(-10);
        ParallelTransition pt = new ParallelTransition(fade, slide); pt.setOnFinished(e -> { if (onFinished != null) onFinished.run(); }); pt.play();
    }
    private void setVisibleManaged(VBox node, boolean v) { if (node != null) { node.setVisible(v); node.setManaged(v); } }
    private void showNotification(String title, String message) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.show(); }

    // ══════════════════════════════════════════════════════════════════════════
    // ROUE DE LA FORTUNE
    // ══════════════════════════════════════════════════════════════════════════
    @FXML private javafx.scene.layout.StackPane rouOverlay;
    @FXML private Canvas      rouCanvas;
    @FXML private Button      btnTourner;
    @FXML private Label       lblRoueStatut;
    @FXML private Label       lblRoueMessage;
    @FXML private VBox        panelBarre;
    @FXML private VBox        panelRefus;
    @FXML private Label       lblCondSolde;
    @FXML private Label       lblCondBudget;
    @FXML private Label       lblCondEpargne;
    @FXML private Label       lblPoints;
    @FXML private ProgressBar progressPoints;
    @FXML private Label       lblProgressText;
    @FXML private Label       lblRefusRaison;
    @FXML private Label       lblRefusSuggestion;

    private final RoueFortuneEligibilityService rouEligSvc = new RoueFortuneEligibilityService();
    private final RoueFortuneService            rouSvc     = new RoueFortuneService();

    private static final int     ROU_NB  = 12;
    private static final int[]   ROU_PTS = {5, 10, 20, 15, 8, 12, 3, 7, 25, 2, 18, 6};
    private static final Color[] ROU_COL = {
            Color.web("#FF4136"), Color.web("#FF851B"), Color.web("#FFDC00"),
            Color.web("#2ECC40"), Color.web("#0074D9"), Color.web("#B10DC9"),
            Color.web("#FF6B9D"), Color.web("#01FF70"), Color.web("#F012BE"),
            Color.web("#3D9970"), Color.web("#FF4136"), Color.web("#7FDBFF")
    };
    private double  rouAngle    = 0.0;
    private boolean rouTourne   = false;
    private int     rouTotalPts = 0;
    private static final int ROU_MAX = 100;

    @FXML
    private void handleRoueFortune() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sections/RoueFortune.fxml"));
            javafx.scene.Parent root = loader.load();
            com.nexora.bank.controllers.RoueFortuneController rouCtrl = loader.getController();
            rouCtrl.setOnPointsUpdated(this::mettreAJourBarreHeader);
            Stage stage = new Stage(); stage.initStyle(StageStyle.UNDECORATED);
            if (btnHistory != null && btnHistory.getScene() != null) {
                stage.initOwner(btnHistory.getScene().getWindow());
                javafx.stage.Window w = btnHistory.getScene().getWindow();
                stage.setX(w.getX() + (w.getWidth()  - 900) / 2);
                stage.setY(w.getY() + (w.getHeight() - 610) / 2);
            }
            stage.setScene(new Scene(root)); stage.setResizable(false);
            root.setOpacity(0); stage.show();
            FadeTransition ft = new FadeTransition(Duration.millis(300), root);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
        } catch (Exception e) {
            e.printStackTrace(); showNotification("Erreur", "Impossible d'ouvrir la Roue : " + e.getMessage());
        }
    }

    @FXML
    private void closeRoueOverlay() {
        if (rouOverlay == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(200), rouOverlay);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> { rouOverlay.setVisible(false); rouOverlay.setManaged(false); });
        ft.play();
    }

    private void rouVerifierEligibilite() {
        rouSetStatut("Analyse de ton profil financier...", "#9CA3AF");
        rouSetBouton(false, "Verification...");
        int userId = SessionManager.getInstance().getCurrentUserId();
        new Thread(() -> {
            try {
                Connection conn = MyDB.getInstance().getConn();
                if (rouSvc.aDejaJoueCeMois(userId, conn)) {
                    Platform.runLater(() -> { rouSetStatut("Tu as deja tourne la roue ce mois-ci !", "#F97316"); rouSetBouton(false, "Deja joue ce mois"); });
                    return;
                }
                EligibilityResult elig = rouEligSvc.checkEligibility(userId, conn);
                Platform.runLater(() -> {
                    rouAfficherConditions(elig);
                    if (elig.eligible) { rouSetStatut("Eligible ! Clique pour tourner !", "#22C55E"); rouSetBouton(true, "Lancer la roue !"); }
                    else { rouSetStatut("Non eligible ce mois-ci", "#EF4444"); rouAfficherRefus(elig); rouSetBouton(false, "Conditions non remplies"); }
                });
            } catch (Exception e) { Platform.runLater(() -> { rouSetStatut("Erreur : " + e.getMessage(), "#EF4444"); rouSetBouton(true, "Lancer la roue !"); }); }
        }, "Roue-Check").start();
    }

    @FXML
    private void lancerRoue() {
        if (rouTourne) return;
        rouTourne = true; rouSetBouton(false, "En rotation...");
        int seg = (int)(Math.random() * ROU_NB);
        double aps = 360.0 / ROU_NB;
        double dest = 5 * 360.0 + (360.0 - (seg * aps + aps / 2)) + 270.0;
        Timeline tl = new Timeline();
        for (int i = 0; i <= 120; i++) {
            double t = (double) i / 120; double eased = 1 - Math.pow(1 - t, 3);
            double ang = rouAngle + dest * eased;
            tl.getKeyFrames().add(new KeyFrame(Duration.seconds(4.0 * t), e -> rouDessiner(ang % 360)));
        }
        tl.setOnFinished(e -> {
            rouAngle = dest % 360; rouTourne = false;
            int uid = SessionManager.getInstance().getCurrentUserId();
            new Thread(() -> {
                try {
                    Connection conn = MyDB.getInstance().getConn();
                    TirageResult res = rouSvc.tournerRoue(uid, conn);
                    Platform.runLater(() -> { rouTotalPts = res.totalPoints; rouAfficherResultat(res); });
                } catch (Exception ex) { Platform.runLater(() -> rouSetBouton(false, "Erreur")); }
            }, "Roue-Tirage").start();
        });
        tl.play();
    }

    private void rouDessiner(double offset) {
        if (rouCanvas == null) return;
        GraphicsContext gc = rouCanvas.getGraphicsContext2D();
        double W = rouCanvas.getWidth(), H = rouCanvas.getHeight();
        double cx = W / 2, cy = H / 2, r = Math.min(W, H) / 2 - 15;
        double aps = 360.0 / ROU_NB;
        gc.clearRect(0, 0, W, H);
        gc.setFill(new RadialGradient(0, 0, cx, cy, r + 12, false, CycleMethod.NO_CYCLE, new Stop(0.85, Color.TRANSPARENT), new Stop(1.0, Color.color(0, 0, 0, 0.4))));
        gc.fillOval(cx - r - 12, cy - r - 12, (r + 12) * 2, (r + 12) * 2);
        gc.setFill(Color.web("#2D2D2D")); gc.fillOval(cx - r - 8, cy - r - 8, (r + 8) * 2, (r + 8) * 2);
        gc.setFill(Color.web("#D4AF37")); gc.fillOval(cx - r - 4, cy - r - 4, (r + 4) * 2, (r + 4) * 2);
        for (int i = 0; i < ROU_NB; i++) {
            double sa = offset + i * aps - aps / 2;
            gc.setFill(ROU_COL[i]); gc.fillArc(cx - r, cy - r, r * 2, r * 2, -sa, -aps, javafx.scene.shape.ArcType.ROUND);
            gc.setStroke(Color.web("#1A1A1A")); gc.setLineWidth(1.5); gc.strokeArc(cx - r, cy - r, r * 2, r * 2, -sa, -aps, javafx.scene.shape.ArcType.ROUND);
            double ma = Math.toRadians(sa + aps / 2);
            double tx = cx + r * 0.72 * Math.cos(ma), ty = cy + r * 0.72 * Math.sin(ma);
            gc.save(); gc.translate(tx, ty); gc.rotate(sa + aps / 2 + 90);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 13)); gc.setTextAlign(TextAlignment.CENTER);
            gc.setFill(Color.color(0, 0, 0, 0.5)); gc.fillText(String.valueOf(ROU_PTS[i]), 1, 1);
            gc.setFill(Color.WHITE); gc.fillText(String.valueOf(ROU_PTS[i]), 0, 0); gc.restore();
        }
        double rc = r * 0.18;
        gc.setFill(Color.web("#D4AF37")); gc.fillOval(cx - rc - 2, cy - rc - 2, (rc+2)*2, (rc+2)*2);
        gc.setFill(new RadialGradient(0, 0, cx - rc*0.3, cy - rc*0.3, rc, false, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#FFE066")), new Stop(1, Color.web("#C8960C"))));
        gc.fillOval(cx - rc, cy - rc, rc * 2, rc * 2);
        double fw = 14, fh = 22;
        gc.setFill(Color.web("#CC1111")); gc.fillPolygon(new double[]{cx, cx-fw/2, cx+fw/2}, new double[]{cy-r-4, cy-r+fh, cy-r+fh}, 3);
        gc.setFill(Color.web("#FF3333")); gc.fillPolygon(new double[]{cx, cx-fw/3, cx+fw/3}, new double[]{cy-r-4, cy-r+fh*0.6, cy-r+fh*0.6}, 3);
        gc.setStroke(Color.web("#8B0000")); gc.setLineWidth(1); gc.strokePolygon(new double[]{cx, cx-fw/2, cx+fw/2}, new double[]{cy-r-4, cy-r+fh, cy-r+fh}, 3);
    }

    private void rouResetUI() {
        if (panelBarre != null) { panelBarre.setVisible(false); panelBarre.setManaged(false); }
        if (panelRefus != null) { panelRefus.setVisible(false); panelRefus.setManaged(false); }
        if (lblRoueMessage != null) lblRoueMessage.setText("");
        rouSetStatut("Cliquez pour verifier votre eligibilite", "#9CA3AF");
    }
    private void rouSetStatut(String txt, String color) { if (lblRoueStatut == null) return; lblRoueStatut.setText(txt); lblRoueStatut.setStyle("-fx-text-fill:" + color + ";-fx-font-size:12px;"); }
    private void rouSetBouton(boolean on, String txt) {
        if (btnTourner == null) return; btnTourner.setDisable(!on); btnTourner.setText(txt);
        btnTourner.setStyle(on ? "-fx-background-color:linear-gradient(to right,#7C3AED,#4F46E5);-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:13px;-fx-background-radius:22;-fx-padding:11 26;-fx-cursor:hand;"
                : "-fx-background-color:#4B5563;-fx-text-fill:white;-fx-font-size:13px;-fx-background-radius:22;-fx-padding:11 26;");
    }
    private void rouAfficherConditions(EligibilityResult r) { rouCond(lblCondSolde, "Solde positif", r.conditionSolde); rouCond(lblCondBudget, "Budget loisirs", r.conditionBudget); rouCond(lblCondEpargne, "Epargne mensuelle", r.conditionEpargne); }
    private void rouCond(Label lbl, String txt, boolean ok) { if (lbl == null) return; lbl.setText((ok ? "OK " : "X ") + txt); lbl.setStyle("-fx-text-fill:" + (ok ? "#22C55E" : "#EF4444") + ";-fx-font-size:12px;-fx-font-weight:700;"); }
    private void rouAfficherRefus(EligibilityResult r) { if (panelRefus != null) { panelRefus.setVisible(true); panelRefus.setManaged(true); } if (lblRefusRaison != null) lblRefusRaison.setText(r.message); if (lblRefusSuggestion != null) lblRefusSuggestion.setText(r.suggestion != null ? r.suggestion : ""); }
    private void rouAfficherResultat(TirageResult res) {
        if (panelBarre != null) { panelBarre.setVisible(true); panelBarre.setManaged(true); }
        if (lblRoueMessage != null) { lblRoueMessage.setText(res.message); lblRoueMessage.setStyle("-fx-text-fill:#FFD700;-fx-font-weight:bold;-fx-font-size:14px;"); FadeTransition ft = new FadeTransition(Duration.millis(600), lblRoueMessage); ft.setFromValue(0); ft.setToValue(1); ft.play(); }
        rouSetStatut("Tu as gagne " + res.pointsGagnes + " points !", "#22C55E");
        rouUpdateBarre(res.pointsGagnes); rouSetBouton(false, "A la prochaine fois !");
    }
    private void rouUpdateBarre(int gained) {
        if (progressPoints == null) return;
        double pct = Math.min((double) rouTotalPts / ROU_MAX, 1.0);
        new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(progressPoints.progressProperty(), progressPoints.getProgress())), new KeyFrame(Duration.millis(800), new KeyValue(progressPoints.progressProperty(), pct, Interpolator.EASE_OUT))).play();
        if (lblPoints != null) lblPoints.setText(rouTotalPts + " / " + ROU_MAX + " pts");
        if (lblProgressText != null && gained > 0) {
            int rest = ROU_MAX - rouTotalPts;
            String msg = rouTotalPts >= ROU_MAX ? "Objectif atteint ! Champion NEXORA !" : gained >= 20 ? "Jackpot ! +" + gained + " pts. Plus que " + rest + " pts !" : gained >= 10 ? "Super ! +" + gained + " pts. Encore " + rest + " pts !" : "Bravo ! +" + gained + " pts. Plus que " + rest + " pts !";
            lblProgressText.setText(msg);
        }
    }

    private void mettreAJourBarreHeader() {
        try {
            int userId = SessionManager.getInstance().getCurrentUserId();
            if (userId <= 0) return;
            Connection conn = MyDB.getInstance().getConn();
            com.nexora.bank.Service.RoueFortuneService svc = new com.nexora.bank.Service.RoueFortuneService();
            svc.initTable(conn);
            int pts = svc.getTotalPoints(userId, conn);
            final int finalPts = pts;
            Platform.runLater(() -> {
                double pct = Math.min((double) finalPts / 100.0, 1.0); int pctInt = (int)(pct * 100);
                if (lblHeaderPoints != null) lblHeaderPoints.setText(finalPts + " pts");
                if (lblHeaderPct != null) {
                    lblHeaderPct.setText(pctInt + "%");
                    String c = pctInt >= 100 ? "#FFD700" : pctInt >= 50 ? "#22C55E" : pctInt >= 25 ? "#FBBF24" : "#A78BFA";
                    lblHeaderPct.setStyle("-fx-text-fill:" + c + "; -fx-font-size:9px; -fx-font-weight:700;");
                }
                if (barreProgression != null) {
                    new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(barreProgression.maxWidthProperty(), barreProgression.getMaxWidth())), new KeyFrame(Duration.millis(800), new KeyValue(barreProgression.maxWidthProperty(), 90.0 * pct, Interpolator.EASE_OUT))).play();
                    String bg = pctInt >= 100 ? "linear-gradient(to right,#FFD700,#FFA500)" : pctInt >= 50 ? "linear-gradient(to right,#22C55E,#16A34A)" : pctInt >= 25 ? "linear-gradient(to right,#FBBF24,#F59E0B)" : "linear-gradient(to right,#7C3AED,#A78BFA)";
                    barreProgression.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:4;");
                }
            });
        } catch (Exception e) { System.err.println("[Roue] Erreur barre header : " + e.getMessage()); }
    }
}
