package com.nexora.bank.controllers;

import com.nexora.bank.Models.CoffreVirtuel;
import com.nexora.bank.Models.CompteBancaire;
import com.nexora.bank.Models.Credit;

import com.nexora.bank.Service.*;
import com.nexora.bank.Utils.SessionManager;


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


    // ─── État local ───────────────────────────────────────────────────────────
    private ObservableList<CompteBancaire> allAccounts = FXCollections.observableArrayList();
    private CompteBancaire selectedCompte       = null;
    private CompteBancaire compteAfficheCoffres = null;
    private String         activeFilter         = "All";
    private CoffreVirtuel  selectedCoffre       = null;
    private boolean        isEditCoffreMode     = false;

    @FXML private VBox surplusSuggestionSection;



    // ─── Filtres ──────────────────────────────────────────────────────────────


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


}