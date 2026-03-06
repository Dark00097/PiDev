package com.nexora.bank.controllers;

import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.Cashback;
import com.nexora.bank.Models.Partenaire;
import com.nexora.bank.Models.User;
import com.nexora.bank.Service.CashbackService;
import com.nexora.bank.Service.EmailNotificationService;
import com.nexora.bank.Service.GeminiCashbackAdvisorService;
import com.nexora.bank.Service.PartenaireService;
import com.nexora.bank.Utils.AIResponseFormatter;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class UserDashboardCashbackSectionController {

    private enum UiLanguage {
        FR,
        EN,
        AR
    }

    private enum RecentPeriodFilter {
        TODAY,
        THIS_WEEK,
        THIS_MONTH,
        ALL_PERIODS
    }

    private static final String I18N_TEXT_KEY = "i18n_text_key";
    private static final String I18N_PROMPT_KEY = "i18n_prompt_key";

    @FXML private VBox rootCashbackSection;
    @FXML private VBox partnerFormContainer;
    @FXML private VBox cashbackFormContainer;
    @FXML private ComboBox<String> cmbLanguage;

    @FXML private Button tabPartner;
    @FXML private Button tabCashback;

    @FXML private TextField txtPartnerName;
    @FXML private TextField txtBaseRate;
    @FXML private TextField txtMaxRate;
    @FXML private TextField txtMonthlyLimit;
    @FXML private DatePicker dpValidUntil;
    @FXML private TextArea txtPartnerDescription;

    @FXML private ComboBox<String> cmbPartnerSelection;
    @FXML private TextField txtPurchaseAmount;
    @FXML private TextField txtAppliedRate;
    @FXML private TextField txtCashbackAmount;
    @FXML private TextField txtTransactionRef;
    @FXML private DatePicker dpPurchaseDate;

    @FXML private Label lblKpiTotalRewards;
    @FXML private Label lblKpiThisMonth;
    @FXML private Label lblKpiMonthDescription;
    @FXML private Label lblKpiPartnerCount;
    @FXML private Label lblKpiPendingAmount;
    @FXML private Label lblTotalRewardsValue;
    @FXML private Label lblQuickPendingAmount;
    @FXML private ComboBox<String> cmbRecentPeriodFilter;
    @FXML private VBox cashbackListContainer;
    @FXML private Button btnAnalyzeCashbackAi;
    @FXML private Label lblCashbackAiStatus;
    @FXML private Label lblCashbackAiUpdatedAt;
    @FXML private TextArea txtCashbackAiAdvice;
    @FXML private Label lblPartner1Name;
    @FXML private Label lblPartner1Category;
    @FXML private Label lblPartner1Rate;
    @FXML private Label lblPartner1Rating;
    @FXML private Label lblPartner2Name;
    @FXML private Label lblPartner2Category;
    @FXML private Label lblPartner2Rate;
    @FXML private Label lblPartner2Rating;
    @FXML private Label lblPartner3Name;
    @FXML private Label lblPartner3Category;
    @FXML private Label lblPartner3Rate;
    @FXML private Label lblPartner3Rating;

    private static final Duration ANIMATION_DURATION = Duration.millis(250);
    private static final String TAB_ACTIVE_CLASS = "form-tab-active";

    private CashbackService cashbackService;
    private PartenaireService partenaireService;
    private final EmailNotificationService emailNotificationService = new EmailNotificationService();
    private final GeminiCashbackAdvisorService geminiCashbackAdvisorService = new GeminiCashbackAdvisorService();
    private String serviceInitError;

    private final ObservableList<Cashback> userCashbacks = FXCollections.observableArrayList();
    private List<Partenaire> cachedPartenaires = List.of();
    private Cashback editingCashback;
    private UiLanguage currentLanguage = UiLanguage.FR;
    private RecentPeriodFilter selectedRecentPeriodFilter = RecentPeriodFilter.ALL_PERIODS;
    private final Map<String, String> enTranslations = new HashMap<>();
    private final Map<String, String> arTranslations = new HashMap<>();

    @FXML
    private void initialize() {
        try {
            cashbackService = new CashbackService();
            partenaireService = new PartenaireService();
        } catch (Exception ex) {
            serviceInitError = ex.getMessage() == null ? "Cashback services initialization failed." : ex.getMessage();
        }

        if (txtPurchaseAmount != null) {
            txtPurchaseAmount.textProperty().addListener((obs, oldVal, newVal) -> calculateCashback());
        }
        if (txtAppliedRate != null) {
            txtAppliedRate.setEditable(false);
            txtAppliedRate.setFocusTraversable(false);
        }
        if (cmbPartnerSelection != null) {
            cmbPartnerSelection.valueProperty().addListener((obs, oldVal, newVal) -> calculateCashback());
        }
        setupCashbackAiSection();

        showCashbackForm();
        refreshDashboardData();
        initializeTranslations();
        initializeLanguageSwitcher();
        initializeRecentPeriodFilter();
        applyLanguage(currentLanguage);
    }

    private void initializeTranslations() {
        putTranslation("Langue", "Language", "اللغة");
        putTranslation("Recompenses cashback", "Cashback rewards", "مكافآت الكاش باك");
        putTranslation("Gagnez des recompenses chez les partenaires et suivez votre cashback",
            "Earn rewards with partners and track your cashback",
            "اكسب مكافآت مع الشركاء وتابع الكاش باك");
        putTranslation("Historique", "History", "السجل");
        putTranslation("Echanger les recompenses", "Redeem rewards", "استبدال المكافآت");
        putTranslation("RECOMPENSES TOTALES", "TOTAL REWARDS", "إجمالي المكافآت");
        putTranslation("Gains cumules", "Total earnings", "إجمالي الأرباح");
        putTranslation("Membre Or", "Gold Member", "عضو ذهبي");
        putTranslation("CE MOIS", "THIS MONTH", "هذا الشهر");
        putTranslation("PARTENAIRES ACTIFS", "ACTIVE PARTNERS", "الشركاء النشطون");
        putTranslation("Commercants inscrits", "Registered merchants", "التجار المسجلون");
        putTranslation("commercants", "merchants", "تجار");
        putTranslation("EN ATTENTE", "PENDING", "قيد الانتظار");
        putTranslation("En attente de credit", "Waiting for credit", "في انتظار الإيداع");
        putTranslation("Ajouter une nouvelle entree", "Add a new entry", "إضافة عملية جديدة");
        putTranslation("Saisir votre cashback", "Enter your cashback", "أدخل الكاش باك الخاص بك");
        putTranslation("Saisir cashback", "Submit cashback", "تسجيل الكاش باك");
        putTranslation("Selectionner un partenaire", "Select a partner", "اختر شريكًا");
        putTranslation("Montant achat", "Purchase amount", "مبلغ الشراء");
        putTranslation("Date achat", "Purchase date", "تاريخ الشراء");
        putTranslation("Selectionner une date", "Select a date", "اختر تاريخًا");
        putTranslation("Taux applique", "Applied rate", "النسبة المطبقة");
        putTranslation("Montant cashback", "Cashback amount", "مبلغ الكاش باك");
        putTranslation("Reference transaction (optionnel)", "Transaction reference (optional)", "مرجع العملية (اختياري)");
        putTranslation("Saisir recu ou numero de transaction", "Enter receipt or transaction number", "أدخل رقم الإيصال أو العملية");
        putTranslation("Cashback estime", "Estimated cashback", "الكاش باك المتوقع");
        putTranslation("Base sur votre achat", "Based on your purchase", "بناءً على عملية الشراء");
        putTranslation("Sera credite sous 3-5 jours", "Will be credited within 3-5 days", "سيتم الإيداع خلال 3-5 أيام");
        putTranslation("Vider", "Clear", "مسح");
        putTranslation("Vos recompenses", "Your rewards", "مكافآتك");
        putTranslation("Suivez vos gains et commercants partenaires", "Track your earnings and partner merchants", "تابع أرباحك والتجار الشركاء");
        putTranslation("Solde disponible", "Available balance", "الرصيد المتاح");
        putTranslation("Cette semaine", "This week", "هذا الأسبوع");
        putTranslation("Echange", "Redeemed", "المستبدل");
        putTranslation("Analyse cashback IA", "AI cashback analysis", "تحليل الكاش باك بالذكاء الاصطناعي");
        putTranslation("Suggestions intelligentes pour optimiser vos gains", "Smart suggestions to optimize your earnings", "اقتراحات ذكية لتحسين أرباحك");
        putTranslation("Pret", "Ready", "جاهز");
        putTranslation("Analyser cashback IA", "Analyze cashback AI", "تحليل الكاش باك بالذكاء الاصطناعي");
        putTranslation("Mise a jour:", "Updated:", "آخر تحديث:");
        putTranslation("Mise a jour: -", "Updated: -", "آخر تحديث: -");
        putTranslation("Commercants partenaires", "Partner merchants", "التجار الشركاء");
        putTranslation("Voir tout", "View all", "عرض الكل");
        putTranslation("Ajouter partenaire", "Add partner", "إضافة شريك");
        putTranslation("Cashback recents", "Recent cashback", "عمليات الكاش باك الأخيرة");
        putTranslation("Toute periode", "All periods", "كل الفترات");
        putTranslation("Aujourd hui", "Today", "اليوم");
        putTranslation("Ce mois", "This month", "هذا الشهر");
        putTranslation("Aucun cashback enregistre.", "No cashback recorded.", "لا توجد عمليات كاش باك.");
        putTranslation("Achat", "Purchase", "شراء");
        putTranslation("Ref", "Ref", "المرجع");
        putTranslation("Note", "Rating", "التقييم");
        putTranslation("Bonus", "Bonus", "المكافأة");
        putTranslation("Taux", "Rate", "النسبة");
        putTranslation("Noter", "Rate", "قيّم");
        putTranslation("Modifier note", "Edit rating", "تعديل التقييم");
        putTranslation("Pending", "Pending", "قيد الانتظار");
        putTranslation("Approved", "Approved", "مقبول");
        putTranslation("Rejected", "Rejected", "مرفوض");
        putTranslation("Partenaire", "Partner", "شريك");
        putTranslation("Categorie", "Category", "الفئة");
        putTranslation("Jusqu a", "Up to", "حتى");
        putTranslation("Jusqu a 0%", "Up to 0%", "حتى 0%");
        putTranslation("General", "General", "عام");
        putTranslation("Noter cashback", "Rate cashback", "تقييم الكاش باك");
        putTranslation("Selectionnez une note avec les etoiles", "Select a star rating", "اختر تقييمًا بالنجوم");
        putTranslation("Valider", "Confirm", "تأكيد");
        putTranslation("Cliquez sur les etoiles:", "Click on the stars:", "اضغط على النجوم:");
        putTranslation("Aucune etoile", "No stars", "بدون نجوم");
        putTranslation("Commentaire", "Comment", "تعليق");
        putTranslation("Commentaire optionnel", "Optional comment", "تعليق اختياري");
        putTranslation("Historique Cashback", "Cashback history", "سجل الكاش باك");
        putTranslation("Cashback", "Cashback", "كاش باك");
        putTranslation("Date", "Date", "التاريخ");
        putTranslation("Reference", "Reference", "المرجع");
        putTranslation("Votre note", "Your rating", "تقييمك");
        putTranslation("Decision bonus", "Bonus decision", "قرار المكافأة");
        putTranslation("Statut", "Status", "الحالة");
        putTranslation("Modifier", "Edit", "تعديل");
        putTranslation("Supprimer", "Delete", "حذف");
        putTranslation("Supprimer ce cashback ?", "Delete this cashback?", "حذف عملية الكاش باك هذه؟");
        putTranslation("Echanger vos recompenses disponibles ?", "Redeem your available rewards?", "استبدال مكافآتك المتاحة؟");
        putTranslation("Analyse IA en cours...", "AI analysis in progress...", "تحليل الذكاء الاصطناعي جارٍ...");
        putTranslation("Aucune recommandation generee.", "No recommendation generated.", "لم يتم توليد أي توصية.");
        putTranslation("Analyse IA terminee", "AI analysis completed", "اكتمل تحليل الذكاء الاصطناعي");
        putTranslation("Mode local (IA indisponible)", "Local mode (AI unavailable)", "الوضع المحلي (الذكاء الاصطناعي غير متاح)");
        putTranslation("Session", "Session", "الجلسة");
        putTranslation("Veuillez vous reconnecter.", "Please sign in again.", "يرجى تسجيل الدخول مرة أخرى.");
        putTranslation("Validation", "Validation", "التحقق");
        putTranslation("Erreur de validation", "Validation error", "خطأ في التحقق");
        putTranslation("Erreur", "Error", "خطأ");
        putTranslation("Succes", "Success", "نجاح");
        putTranslation("Information", "Information", "معلومة");
        putTranslation("Selectionnez un partenaire.", "Select a partner.", "اختر شريكًا.");
        putTranslation("Modification refusee.", "Update denied.", "تم رفض التعديل.");
        putTranslation("Creation impossible.", "Creation failed.", "تعذر الإنشاء.");
        putTranslation("Cashback modifie", "Cashback updated", "تم تعديل الكاش باك");
        putTranslation("Votre cashback a ete mis a jour.", "Your cashback was updated.", "تم تحديث الكاش باك الخاص بك.");
        putTranslation("Cashback enregistre", "Cashback saved", "تم حفظ الكاش باك");
        putTranslation("Le cashback a ete enregistre avec succes.", "Cashback was saved successfully.", "تم حفظ الكاش باك بنجاح.");
        putTranslation("Selectionnez une ligne a modifier.", "Select a row to edit.", "حدد سطرًا للتعديل.");
        putTranslation("Selectionnez une ligne a noter.", "Select a row to rate.", "حدد سطرًا للتقييم.");
        putTranslation("Selectionnez une ligne a supprimer.", "Select a row to delete.", "حدد سطرًا للحذف.");
        putTranslation("Suppression refusee.", "Deletion denied.", "تم رفض الحذف.");
        putTranslation("Echange lance", "Redeem started", "بدء الاستبدال");
        putTranslation("Demande de redemption enregistree.", "Redemption request registered.", "تم تسجيل طلب الاستبدال.");
        putTranslation("La note doit etre entre 0 et 5.", "Rating must be between 0 and 5.", "يجب أن يكون التقييم بين 0 و5.");
        putTranslation("Impossible d enregistrer votre note.", "Unable to save your rating.", "تعذر حفظ تقييمك.");
        putTranslation("Votre note est envoyee. L admin peut approuver ou refuser un bonus.",
            "Your rating was sent. Admin can approve or reject a bonus.",
            "تم إرسال تقييمك. يمكن للمسؤول قبول أو رفض المكافأة.");
        putTranslation("Cashback indisponible", "Cashback unavailable", "الكاش باك غير متاح");
        putTranslation("Veuillez saisir un nom de partenaire.", "Please enter a partner name.", "يرجى إدخال اسم شريك.");
        putTranslation("Veuillez saisir un taux de cashback de base.", "Please enter a base cashback rate.", "يرجى إدخال نسبة الكاش باك الأساسية.");
        putTranslation("Veuillez saisir un montant d achat.", "Please enter a purchase amount.", "يرجى إدخال مبلغ الشراء.");
        putTranslation("Montant d achat invalide.", "Invalid purchase amount.", "مبلغ الشراء غير صالح.");
        putTranslation("Le montant d achat doit etre superieur a 0.", "Purchase amount must be greater than 0.", "يجب أن يكون مبلغ الشراء أكبر من 0.");
        putTranslation("Veuillez selectionner une date d achat.", "Please select a purchase date.", "يرجى اختيار تاريخ الشراء.");
        putTranslation("Cliquez sur \"Analyser cashback IA\" pour obtenir des conseils personnalises.",
            "Click \"Analyze cashback AI\" to get personalized advice.",
            "اضغط \"تحليل الكاش باك بالذكاء الاصطناعي\" للحصول على نصائح مخصصة.");
    }

    private void initializeLanguageSwitcher() {
        if (cmbLanguage == null) {
            return;
        }
        cmbLanguage.getItems().setAll("Francais", "English", "العربية");
        cmbLanguage.setValue("Francais");
        cmbLanguage.valueProperty().addListener((obs, oldVal, newVal) -> {
            UiLanguage selected = languageFromLabel(newVal);
            if (selected != currentLanguage) {
                applyLanguage(selected);
            }
        });
    }

    private void applyLanguage(UiLanguage language) {
        currentLanguage = language == null ? UiLanguage.FR : language;

        if (rootCashbackSection != null) {
            rootCashbackSection.setNodeOrientation(currentLanguage == UiLanguage.AR
                ? NodeOrientation.RIGHT_TO_LEFT
                : NodeOrientation.LEFT_TO_RIGHT);
            registerTranslationKeys(rootCashbackSection);
            applyTranslations(rootCashbackSection);
        }

        if (cmbLanguage != null) {
            cmbLanguage.setPromptText(t("Langue"));
            String label = languageLabel(currentLanguage);
            if (!label.equals(cmbLanguage.getValue())) {
                cmbLanguage.setValue(label);
            }
        }

        if (lblKpiMonthDescription != null) {
            String monthName = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, resolveCurrentLocale());
            lblKpiMonthDescription.setText(monthName + " " + LocalDate.now().getYear());
        }

        refreshRecentPeriodFilterItems();
        renderCashbackList();
    }

    private void initializeRecentPeriodFilter() {
        if (cmbRecentPeriodFilter == null) {
            return;
        }
        refreshRecentPeriodFilterItems();
        cmbRecentPeriodFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            RecentPeriodFilter resolved = resolveRecentPeriodFilter(newVal);
            if (resolved != selectedRecentPeriodFilter) {
                selectedRecentPeriodFilter = resolved;
                renderCashbackList();
            }
        });
    }

    private void refreshRecentPeriodFilterItems() {
        if (cmbRecentPeriodFilter == null) {
            return;
        }
        cmbRecentPeriodFilter.getItems().setAll(
            periodFilterLabel(RecentPeriodFilter.TODAY),
            periodFilterLabel(RecentPeriodFilter.THIS_WEEK),
            periodFilterLabel(RecentPeriodFilter.THIS_MONTH),
            periodFilterLabel(RecentPeriodFilter.ALL_PERIODS)
        );
        cmbRecentPeriodFilter.setPromptText(periodFilterLabel(RecentPeriodFilter.ALL_PERIODS));
        cmbRecentPeriodFilter.setValue(periodFilterLabel(selectedRecentPeriodFilter));
    }

    private String periodFilterLabel(RecentPeriodFilter filter) {
        if (filter == null) {
            return t("Toute periode");
        }
        return switch (filter) {
            case TODAY -> t("Aujourd hui");
            case THIS_WEEK -> t("Cette semaine");
            case THIS_MONTH -> t("Ce mois");
            case ALL_PERIODS -> t("Toute periode");
        };
    }

    private RecentPeriodFilter resolveRecentPeriodFilter(String value) {
        if (matchesTranslationToken(value, "Aujourd hui")) {
            return RecentPeriodFilter.TODAY;
        }
        if (matchesTranslationToken(value, "Cette semaine")) {
            return RecentPeriodFilter.THIS_WEEK;
        }
        if (matchesTranslationToken(value, "Ce mois")) {
            return RecentPeriodFilter.THIS_MONTH;
        }
        return RecentPeriodFilter.ALL_PERIODS;
    }

    private boolean matchesTranslationToken(String value, String frToken) {
        String normalized = safe(value).trim();
        if (normalized.isEmpty()) {
            return false;
        }
        String enValue = safe(enTranslations.get(frToken));
        String arValue = safe(arTranslations.get(frToken));
        return normalized.equalsIgnoreCase(frToken)
            || normalized.equalsIgnoreCase(enValue)
            || normalized.equals(arValue);
    }

    private void registerTranslationKeys(Parent parent) {
        ArrayDeque<Node> queue = new ArrayDeque<>();
        queue.add(parent);

        while (!queue.isEmpty()) {
            Node node = queue.poll();
            registerTranslationKey(node);
            if (node instanceof Parent childParent) {
                queue.addAll(childParent.getChildrenUnmodifiable());
            }
        }
    }

    private void applyTranslations(Parent parent) {
        ArrayDeque<Node> queue = new ArrayDeque<>();
        queue.add(parent);

        while (!queue.isEmpty()) {
            Node node = queue.poll();
            applyTranslation(node);
            if (node instanceof Parent childParent) {
                queue.addAll(childParent.getChildrenUnmodifiable());
            }
        }
    }

    private void registerTranslationKey(Node node) {
        if (node instanceof Label label) {
            String text = safe(label.getText()).trim();
            if (!text.isEmpty()) {
                label.getProperties().putIfAbsent(I18N_TEXT_KEY, text);
            }
            return;
        }

        if (node instanceof Button button) {
            String text = safe(button.getText()).trim();
            if (!text.isEmpty()) {
                button.getProperties().putIfAbsent(I18N_TEXT_KEY, text);
            }
            return;
        }

        if (node instanceof TextField textField) {
            String prompt = safe(textField.getPromptText()).trim();
            if (!prompt.isEmpty()) {
                textField.getProperties().putIfAbsent(I18N_PROMPT_KEY, prompt);
            }
            return;
        }

        if (node instanceof TextArea textArea) {
            String prompt = safe(textArea.getPromptText()).trim();
            if (!prompt.isEmpty()) {
                textArea.getProperties().putIfAbsent(I18N_PROMPT_KEY, prompt);
            }
            return;
        }

        if (node instanceof DatePicker datePicker) {
            String prompt = safe(datePicker.getPromptText()).trim();
            if (!prompt.isEmpty()) {
                datePicker.getProperties().putIfAbsent(I18N_PROMPT_KEY, prompt);
            }
            return;
        }

        if (node instanceof ComboBox<?> comboBox && comboBox != cmbLanguage) {
            String prompt = safe(comboBox.getPromptText()).trim();
            if (!prompt.isEmpty()) {
                comboBox.getProperties().putIfAbsent(I18N_PROMPT_KEY, prompt);
            }
        }
    }

    private void applyTranslation(Node node) {
        if (node instanceof Label label) {
            Object key = label.getProperties().get(I18N_TEXT_KEY);
            if (key instanceof String source) {
                label.setText(t(source));
            }
            return;
        }

        if (node instanceof Button button) {
            Object key = button.getProperties().get(I18N_TEXT_KEY);
            if (key instanceof String source) {
                button.setText(t(source));
            }
            return;
        }

        if (node instanceof TextField textField) {
            Object key = textField.getProperties().get(I18N_PROMPT_KEY);
            if (key instanceof String source) {
                textField.setPromptText(t(source));
            }
            return;
        }

        if (node instanceof TextArea textArea) {
            Object key = textArea.getProperties().get(I18N_PROMPT_KEY);
            if (key instanceof String source) {
                textArea.setPromptText(t(source));
            }
            return;
        }

        if (node instanceof DatePicker datePicker) {
            Object key = datePicker.getProperties().get(I18N_PROMPT_KEY);
            if (key instanceof String source) {
                datePicker.setPromptText(t(source));
            }
            return;
        }

        if (node instanceof ComboBox<?> comboBox && comboBox != cmbLanguage) {
            Object key = comboBox.getProperties().get(I18N_PROMPT_KEY);
            if (key instanceof String source) {
                comboBox.setPromptText(t(source));
            }
        }
    }

    private void putTranslation(String fr, String en, String ar) {
        enTranslations.put(fr, en);
        arTranslations.put(fr, ar);
    }

    private String t(String frText) {
        String key = safe(frText);
        return switch (currentLanguage) {
            case EN -> enTranslations.getOrDefault(key, key);
            case AR -> arTranslations.getOrDefault(key, key);
            default -> key;
        };
    }

    private String languageLabel(UiLanguage language) {
        return switch (language) {
            case EN -> "English";
            case AR -> "العربية";
            default -> "Francais";
        };
    }

    private UiLanguage languageFromLabel(String label) {
        String normalized = safe(label).trim();
        if ("English".equalsIgnoreCase(normalized)) {
            return UiLanguage.EN;
        }
        if ("العربية".equals(normalized)) {
            return UiLanguage.AR;
        }
        return UiLanguage.FR;
    }

    private Locale resolveCurrentLocale() {
        return switch (currentLanguage) {
            case EN -> Locale.ENGLISH;
            case AR -> new Locale("ar");
            default -> Locale.FRENCH;
        };
    }

    @FXML
    private void showPartnerForm() {
        updateTabStyles(tabPartner);
        setVisibleManaged(partnerFormContainer, true);
        setVisibleManaged(cashbackFormContainer, false);
        animateFadeIn(partnerFormContainer);
    }

    @FXML
    private void showCashbackForm() {
        updateTabStyles(tabCashback);
        setVisibleManaged(partnerFormContainer, false);
        setVisibleManaged(cashbackFormContainer, true);
        animateFadeIn(cashbackFormContainer);
    }

    @FXML
    private void addPartner() {
        if (!ensureServicesReady()) {
            return;
        }
        if (!validatePartnerForm()) {
            return;
        }

        try {
            Partenaire partenaire = new Partenaire();
            partenaire.setNom(txtPartnerName.getText().trim());
            partenaire.setCategorie("General");
            partenaire.setDescription(txtPartnerDescription == null ? "" : txtPartnerDescription.getText().trim());
            partenaire.setTauxCashback(parseDouble(txtBaseRate.getText()));
            partenaire.setTauxCashbackMax(txtMaxRate == null || txtMaxRate.getText().isBlank()
                    ? partenaire.getTauxCashback()
                    : parseDouble(txtMaxRate.getText()));
            partenaire.setPlafondMensuel(txtMonthlyLimit == null || txtMonthlyLimit.getText().isBlank()
                    ? 0
                    : parseDouble(txtMonthlyLimit.getText()));
            partenaire.setConditions(dpValidUntil != null && dpValidUntil.getValue() != null
                    ? "Valable jusqu au " + dpValidUntil.getValue()
                    : "");
            partenaire.setStatus("Actif");

            partenaireService.createPartenaire(partenaire);
            showNotification("Partenaire ajoute", "Le partenaire a ete enregistre avec succes.");
            clearPartnerForm();
            refreshDashboardData();
        } catch (RuntimeException ex) {
            showNotification("Erreur", ex.getMessage());
        }
    }

    @FXML
    private void clearPartnerForm() {
        if (txtPartnerName != null) txtPartnerName.clear();
        if (txtBaseRate != null) txtBaseRate.clear();
        if (txtMaxRate != null) txtMaxRate.clear();
        if (txtMonthlyLimit != null) txtMonthlyLimit.clear();
        if (dpValidUntil != null) dpValidUntil.setValue(null);
        if (txtPartnerDescription != null) txtPartnerDescription.clear();
    }

    @FXML
    private void recordCashback() {
        if (!ensureServicesReady()) {
            return;
        }
        if (!validateCashbackForm()) {
            return;
        }

        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null || currentUser.getIdUser() <= 0) {
            showNotification("Session", "Veuillez vous reconnecter.");
            return;
        }

        try {
            Cashback cashback = new Cashback();
            cashback.setIdUser(currentUser.getIdUser());

            String selectedPartenaire = cmbPartnerSelection == null ? "" : cmbPartnerSelection.getValue();
            if (selectedPartenaire == null || selectedPartenaire.isBlank()) {
                showNotification("Validation", "Selectionnez un partenaire.");
                return;
            }
            cashback.setPartenaireNom(selectedPartenaire);
            Optional<Partenaire> selectedPartner = findPartnerByName(selectedPartenaire);
            cashback.setIdPartenaire(selectedPartner.map(Partenaire::getIdPartenaire).orElse(null));

            double purchaseAmount = parseDouble(txtPurchaseAmount.getText());
            double rating = selectedPartner.map(Partenaire::getRating).orElse(0.0);
            cashback.setMontantAchat(purchaseAmount);
            cashback.setTauxApplique(cashbackService.resolveEffectiveRatePercent(purchaseAmount, rating));
            cashback.setMontantCashback(cashbackService.calculateCashbackByAmountAndRating(purchaseAmount, rating));
            cashback.setDateAchat(dpPurchaseDate.getValue());
            cashback.setDateCredit(null);
            cashback.setDateExpiration(dpPurchaseDate.getValue() == null ? null : dpPurchaseDate.getValue().plusMonths(6));
            cashback.setStatut("En attente");
            cashback.setTransactionRef(txtTransactionRef == null ? "" : txtTransactionRef.getText().trim());

            if (editingCashback != null) {
                cashback.setIdCashback(editingCashback.getIdCashback());
                boolean updated = cashbackService.updateCashbackForUser(cashback, currentUser.getIdUser());
                if (!updated) {
                    showNotification("Erreur", "Modification refusee.");
                    return;
                }
                showNotification("Cashback modifie", "Votre cashback a ete mis a jour.");
            } else {
                int created = cashbackService.createCashback(cashback);
                if (created <= 0) {
                    showNotification("Erreur", "Creation impossible.");
                    return;
                }
                cashback.setIdCashback(created);
                try {
                    emailNotificationService.sendCashbackSubmittedEmail(currentUser, cashback);
                } catch (RuntimeException mailEx) {
                    showNotification("Information", "Cashback enregistre, mais l'email de confirmation n'a pas pu etre envoye.");
                }
                showNotification("Cashback enregistre", "Le cashback a ete enregistre avec succes.");
            }

            clearCashbackForm();
            refreshDashboardData();
        } catch (RuntimeException ex) {
            showNotification("Erreur", ex.getMessage());
        }
    }

    @FXML
    private void clearCashbackForm() {
        if (txtPurchaseAmount != null) txtPurchaseAmount.clear();
        if (txtAppliedRate != null) txtAppliedRate.setText("1.00");
        if (txtCashbackAmount != null) txtCashbackAmount.setText("0.00");
        if (txtTransactionRef != null) txtTransactionRef.clear();
        if (dpPurchaseDate != null) dpPurchaseDate.setValue(null);
        if (cmbPartnerSelection != null && !cmbPartnerSelection.getItems().isEmpty()) {
            cmbPartnerSelection.getSelectionModel().selectFirst();
        }
        editingCashback = null;
    }

    @FXML
    private void viewRewardsHistory() {
        if (!ensureServicesReady()) {
            return;
        }
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null || currentUser.getIdUser() <= 0) {
            showNotification("Session", "Veuillez vous reconnecter.");
            return;
        }

        List<Cashback> rows = cashbackService.getCashbacksByUser(currentUser.getIdUser());
        if (rows.isEmpty()) {
            showNotification("Historique", "Aucun cashback enregistre.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(t("Historique Cashback"));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        TableView<Cashback> table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        TableColumn<Cashback, String> colPartner = new TableColumn<>(t("Partenaire"));
        colPartner.setCellValueFactory(new PropertyValueFactory<>("partenaireNom"));

        TableColumn<Cashback, String> colPurchase = new TableColumn<>(t("Montant achat"));
        colPurchase.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getMontantAchat())));

        TableColumn<Cashback, String> colRate = new TableColumn<>(t("Taux"));
        colRate.setCellValueFactory(c -> new SimpleStringProperty(formatRate(c.getValue().getTauxApplique())));

        TableColumn<Cashback, String> colCashback = new TableColumn<>(t("Cashback"));
        colCashback.setCellValueFactory(c -> new SimpleStringProperty(String.format("+%.2f", c.getValue().getMontantCashback())));

        TableColumn<Cashback, String> colDate = new TableColumn<>(t("Date"));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(formatDate(c.getValue().getDateAchat())));

        TableColumn<Cashback, String> colRef = new TableColumn<>(t("Reference"));
        colRef.setCellValueFactory(c -> new SimpleStringProperty(safe(c.getValue().getTransactionRef())));

        TableColumn<Cashback, String> colUserRating = new TableColumn<>(t("Votre note"));
        colUserRating.setCellValueFactory(c -> new SimpleStringProperty(formatRatingDisplay(c.getValue().getUserRating())));

        TableColumn<Cashback, String> colBonusDecision = new TableColumn<>(t("Decision bonus"));
        colBonusDecision.setCellValueFactory(c -> new SimpleStringProperty(formatBonusDecision(c.getValue().getBonusDecision())));

        TableColumn<Cashback, String> colStatus = new TableColumn<>(t("Statut"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statut"));

        table.getColumns().addAll(colPartner, colPurchase, colRate, colCashback, colDate, colRef, colUserRating, colBonusDecision, colStatus);
        table.getItems().setAll(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button btnEdit = new Button(t("Modifier"));
        Button btnRate = new Button(t("Noter"));
        Button btnDelete = new Button(t("Supprimer"));

        btnEdit.setOnAction(event -> {
            Cashback selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showNotification("Historique", "Selectionnez une ligne a modifier.");
                return;
            }
            loadCashbackToForm(selected);
            dialog.close();
        });

        btnRate.setOnAction(event -> {
            Cashback selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showNotification("Historique", "Selectionnez une ligne a noter.");
                return;
            }
            rateCashback(selected, () -> {
                List<Cashback> refreshedRows = cashbackService.getCashbacksByUser(currentUser.getIdUser());
                table.getItems().setAll(refreshedRows);
                userCashbacks.setAll(refreshedRows);
                renderCashbackList();
            });
        });

        btnDelete.setOnAction(event -> {
            Cashback selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showNotification("Historique", "Selectionnez une ligne a supprimer.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, t("Supprimer ce cashback ?"), ButtonType.OK, ButtonType.CANCEL);
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                boolean deleted = cashbackService.deleteCashbackForUser(selected.getIdCashback(), currentUser.getIdUser());
                if (deleted) {
                    table.getItems().remove(selected);
                    refreshDashboardData();
                } else {
                    showNotification("Erreur", "Suppression refusee.");
                }
            }
        });

        HBox actions = new HBox(10, btnEdit, btnRate, btnDelete);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(12, table, actions);
        content.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        dialog.getDialogPane().setPrefWidth(850);
        dialog.getDialogPane().setPrefHeight(450);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    @FXML
    private void redeemRewards() {
        if (!ensureServicesReady()) {
            return;
        }
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null || currentUser.getIdUser() <= 0) {
            showNotification("Session", "Veuillez vous reconnecter.");
            return;
        }

        double available = cashbackService.getCreditedTotalByUser(currentUser.getIdUser());
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(t("Echanger les recompenses"));
        alert.setHeaderText(t("Echanger vos recompenses disponibles ?"));
        alert.setContentText(t("Solde disponible") + " : " + String.format("%.2f", available));

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showNotification("Echange lance", "Demande de redemption enregistree.");
            }
        });
    }

    @FXML
    private void handleAnalyzeCashbackAi() {
        if (!ensureServicesReady()) {
            return;
        }
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null || currentUser.getIdUser() <= 0) {
            showNotification("Session", "Veuillez vous reconnecter.");
            return;
        }

        List<Cashback> cashbacks = userCashbacks.isEmpty()
            ? cashbackService.getCashbacksByUser(currentUser.getIdUser())
            : new ArrayList<>(userCashbacks);
        List<Partenaire> partenaires = cachedPartenaires == null ? List.of() : new ArrayList<>(cachedPartenaires);

        setCashbackAiStatus(t("Analyse IA en cours..."), "#2563EB");
        if (btnAnalyzeCashbackAi != null) {
            btnAnalyzeCashbackAi.setDisable(true);
        }

        List<Cashback> finalCashbacks = cashbacks;
        List<Partenaire> finalPartenaires = partenaires;
        Task<String> aiTask = new Task<>() {
            @Override
            protected String call() {
                return geminiCashbackAdvisorService.analyzeCashbackProfile(currentUser, finalCashbacks, finalPartenaires);
            }
        };

        aiTask.setOnSucceeded(event -> Platform.runLater(() -> {
            String response = aiTask.getValue();
            String cleaned = AIResponseFormatter.stripMarkdown(response);
            if (txtCashbackAiAdvice != null) {
                txtCashbackAiAdvice.setText(cleaned.isBlank() ? t("Aucune recommandation generee.") : cleaned);
            }
            setCashbackAiStatus(t("Analyse IA terminee"), "#059669");
            if (lblCashbackAiUpdatedAt != null) {
                lblCashbackAiUpdatedAt.setText(t("Mise a jour:") + " " + LocalDateTime.now().format(localizedDateTimeFormatter()));
            }
            if (btnAnalyzeCashbackAi != null) {
                btnAnalyzeCashbackAi.setDisable(false);
            }
        }));

        aiTask.setOnFailed(event -> Platform.runLater(() -> {
            String fallback = geminiCashbackAdvisorService.buildLocalFallbackAdvice(currentUser, finalCashbacks, finalPartenaires);
            if (txtCashbackAiAdvice != null) {
                txtCashbackAiAdvice.setText(fallback);
            }
            setCashbackAiStatus(t("Mode local (IA indisponible)"), "#B45309");
            if (lblCashbackAiUpdatedAt != null) {
                lblCashbackAiUpdatedAt.setText(t("Mise a jour:") + " " + LocalDateTime.now().format(localizedDateTimeFormatter()));
            }
            if (btnAnalyzeCashbackAi != null) {
                btnAnalyzeCashbackAi.setDisable(false);
            }
        }));

        Thread worker = new Thread(aiTask, "gemini-cashback-advisor");
        worker.setDaemon(true);
        worker.start();
    }

    private void loadCashbackToForm(Cashback cashback) {
        editingCashback = cashback;
        showCashbackForm();
        if (cmbPartnerSelection != null) cmbPartnerSelection.setValue(cashback.getPartenaireNom());
        if (txtPurchaseAmount != null) txtPurchaseAmount.setText(String.valueOf(cashback.getMontantAchat()));
        if (txtAppliedRate != null) txtAppliedRate.setText(String.valueOf(cashback.getTauxApplique()));
        if (txtCashbackAmount != null) txtCashbackAmount.setText(String.format("%.2f", cashback.getMontantCashback()));
        if (txtTransactionRef != null) txtTransactionRef.setText(cashback.getTransactionRef());
        if (dpPurchaseDate != null) dpPurchaseDate.setValue(cashback.getDateAchat());
    }

    private void refreshDashboardData() {
        if (!ensureServicesReady()) {
            if (cmbPartnerSelection != null) {
                cmbPartnerSelection.getItems().clear();
            }
            userCashbacks.clear();
            renderCashbackList();
            if (lblKpiTotalRewards != null) lblKpiTotalRewards.setText(formatMoney(0));
            if (lblKpiThisMonth != null) lblKpiThisMonth.setText("+" + formatMoney(0));
            if (lblKpiPartnerCount != null) lblKpiPartnerCount.setText("0");
            if (lblKpiPendingAmount != null) lblKpiPendingAmount.setText(formatMoney(0));
            if (lblTotalRewardsValue != null) lblTotalRewardsValue.setText(formatMoney(0));
            if (lblQuickPendingAmount != null) lblQuickPendingAmount.setText(formatMoney(0));
            return;
        }

        User currentUser = AuthSession.getCurrentUser();

        List<Partenaire> partenaires = partenaireService.getAllPartenaires();
        cachedPartenaires = partenaires;
        if (cmbPartnerSelection != null) {
            cmbPartnerSelection.getItems().setAll(partenaires.stream().map(Partenaire::getNom).toList());
            if (!cmbPartnerSelection.getItems().isEmpty() && cmbPartnerSelection.getValue() == null) {
                cmbPartnerSelection.getSelectionModel().selectFirst();
            }
        }
        renderPartnerShowcase(partenaires);

        if (currentUser == null || currentUser.getIdUser() <= 0) {
            userCashbacks.clear();
            renderCashbackList();
            return;
        }

        userCashbacks.setAll(cashbackService.getCashbacksByUser(currentUser.getIdUser()));
        userCashbacks.sort(Comparator.comparing(Cashback::getDateAchat, Comparator.nullsLast(LocalDate::compareTo)).reversed());
        renderCashbackList();

        double total = cashbackService.getCreditedTotalByUser(currentUser.getIdUser());
        double month = cashbackService.getCurrentMonthTotalByUser(currentUser.getIdUser());
        double pending = cashbackService.getPendingTotalByUser(currentUser.getIdUser());

        if (lblKpiTotalRewards != null) lblKpiTotalRewards.setText(formatMoney(total));
        if (lblKpiThisMonth != null) lblKpiThisMonth.setText("+" + formatMoney(month));
        if (lblKpiMonthDescription != null) {
            String monthName = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, resolveCurrentLocale());
            lblKpiMonthDescription.setText(monthName + " " + LocalDate.now().getYear());
        }
        if (lblKpiPartnerCount != null) lblKpiPartnerCount.setText(String.valueOf(partenaires.size()));
        if (lblKpiPendingAmount != null) lblKpiPendingAmount.setText(formatMoney(pending));
        if (lblTotalRewardsValue != null) lblTotalRewardsValue.setText(formatMoney(total));
        if (lblQuickPendingAmount != null) lblQuickPendingAmount.setText(formatMoney(pending));
    }

    private void renderCashbackList() {
        if (cashbackListContainer == null) {
            return;
        }

        cashbackListContainer.getChildren().clear();
        List<Cashback> filteredCashbacks = userCashbacks.stream()
            .filter(this::matchesSelectedRecentPeriod)
            .limit(6)
            .toList();

        if (filteredCashbacks.isEmpty()) {
            Label empty = new Label(t("Aucun cashback enregistre."));
            empty.getStyleClass().add("cashback-item-date");
            cashbackListContainer.getChildren().add(empty);
            return;
        }

        for (Cashback cashback : filteredCashbacks) {
            cashbackListContainer.getChildren().add(buildCashbackRow(cashback));
        }
    }

    private boolean matchesSelectedRecentPeriod(Cashback cashback) {
        if (cashback == null) {
            return false;
        }
        LocalDate referenceDate = cashback.getCreatedAt() == null
            ? cashback.getDateAchat()
            : cashback.getCreatedAt().toLocalDate();
        if (referenceDate == null) {
            return selectedRecentPeriodFilter == RecentPeriodFilter.ALL_PERIODS;
        }

        LocalDate today = LocalDate.now();
        return switch (selectedRecentPeriodFilter) {
            case TODAY -> referenceDate.isEqual(today);
            case THIS_WEEK -> !referenceDate.isBefore(today.minusDays(6)) && !referenceDate.isAfter(today);
            case THIS_MONTH -> referenceDate.getYear() == today.getYear()
                && referenceDate.getMonthValue() == today.getMonthValue();
            case ALL_PERIODS -> true;
        };
    }

    private HBox buildCashbackRow(Cashback cashback) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("cashback-item");

        if ("En attente".equalsIgnoreCase(cashback.getStatut())) {
            row.getStyleClass().add("cashback-item-pending");
        } else if ("Expire".equalsIgnoreCase(cashback.getStatut())) {
            row.getStyleClass().add("cashback-item-expired");
        }

        VBox details = new VBox(4);
        HBox.setHgrow(details, Priority.ALWAYS);

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        Label merchant = new Label(cashback.getPartenaireNom());
        merchant.getStyleClass().add("cashback-item-merchant");

        Label status = new Label(safe(cashback.getStatut()));
        status.getStyleClass().add("cashback-item-rate");

        top.getChildren().addAll(merchant, status);

        HBox meta = new HBox(10);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label purchase = new Label(t("Achat") + " : " + formatMoney(cashback.getMontantAchat()));
        purchase.getStyleClass().add("cashback-item-purchase");

        Label date = new Label(formatDate(cashback.getDateAchat()));
        date.getStyleClass().add("cashback-item-date");

        Label ref = new Label(t("Ref") + " : " + (isBlank(cashback.getTransactionRef()) ? "-" : cashback.getTransactionRef()));
        ref.getStyleClass().add("cashback-item-date");

        Label rating = new Label(t("Note") + ": " + formatRatingDisplay(cashback.getUserRating()));
        rating.getStyleClass().add("cashback-item-date");

        Label bonusDecision = new Label(t("Bonus") + ": " + formatBonusDecision(cashback.getBonusDecision()));
        bonusDecision.getStyleClass().add("cashback-item-date");

        meta.getChildren().addAll(purchase, date, ref, rating, bonusDecision);
        details.getChildren().addAll(top, meta);

        VBox amounts = new VBox(2);
        amounts.setAlignment(Pos.CENTER_RIGHT);

        Label cashbackAmount = new Label("+" + formatMoney(cashback.getMontantCashback()));
        cashbackAmount.getStyleClass().add("cashback-item-amount");

        Label rate = new Label(t("Taux") + " " + formatRate(cashback.getTauxApplique()));
        rate.getStyleClass().add("cashback-item-rate");

        amounts.getChildren().addAll(cashbackAmount, rate);

        Button btnRate = new Button(cashback.getUserRating() == null ? t("Noter") : t("Modifier note"));
        btnRate.getStyleClass().add("btn-outline");
        btnRate.setOnAction(event -> rateCashback(cashback, this::refreshDashboardData));

        row.getChildren().addAll(details, amounts, btnRate);
        return row;
    }

    private void rateCashback(Cashback selected, Runnable onSuccessRefresh) {
        if (selected == null) {
            return;
        }
        User currentUser = AuthSession.getCurrentUser();
        if (currentUser == null || currentUser.getIdUser() <= 0) {
            showNotification("Session", "Veuillez vous reconnecter.");
            return;
        }

        int currentRounded = selected.getUserRating() == null ? 5 : (int) Math.round(selected.getUserRating());
        currentRounded = Math.max(0, Math.min(5, currentRounded));

        Dialog<Integer> ratingDialog = new Dialog<>();
        ratingDialog.setTitle(t("Noter cashback"));
        ratingDialog.setHeaderText(t("Selectionnez une note avec les etoiles"));
        ButtonType btnValidate = new ButtonType(t("Valider"), ButtonBar.ButtonData.OK_DONE);
        ratingDialog.getDialogPane().getButtonTypes().addAll(btnValidate, ButtonType.CANCEL);

        VBox ratingContent = new VBox(10);
        ratingContent.setAlignment(Pos.CENTER_LEFT);

        Label ratingHint = new Label(t("Cliquez sur les etoiles:"));
        ratingHint.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");

        HBox starsRow = new HBox(6);
        starsRow.setAlignment(Pos.CENTER_LEFT);

        List<Button> starButtons = new ArrayList<>();
        final int[] selectedStars = new int[] { currentRounded };
        for (int i = 1; i <= 5; i++) {
            Button starButton = new Button();
            starButton.setMinWidth(36);
            starButton.setPrefWidth(36);
            starButton.setStyle("-fx-font-size: 24px; -fx-background-color: transparent; -fx-padding: 0 2 0 2; -fx-cursor: hand;");
            final int value = i;
            starButton.setOnAction(event -> {
                selectedStars[0] = value;
                refreshStarButtons(starButtons, selectedStars[0]);
            });
            starButtons.add(starButton);
            starsRow.getChildren().add(starButton);
        }

        Button clearRating = new Button(t("Aucune etoile"));
        clearRating.setStyle("-fx-font-size: 12px; -fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 8; -fx-background-radius: 8;");
        clearRating.setOnAction(event -> {
            selectedStars[0] = 0;
            refreshStarButtons(starButtons, selectedStars[0]);
        });

        refreshStarButtons(starButtons, selectedStars[0]);

        ratingContent.getChildren().addAll(ratingHint, starsRow, clearRating);
        ratingDialog.getDialogPane().setContent(ratingContent);
        ratingDialog.setResultConverter(buttonType -> buttonType == btnValidate ? selectedStars[0] : null);

        Optional<Integer> ratingResult = ratingDialog.showAndWait();
        if (ratingResult.isEmpty()) {
            return;
        }

        double rating = ratingResult.get();
        if (rating < 0 || rating > 5) {
            showNotification("Validation", "La note doit etre entre 0 et 5.");
            return;
        }

        TextInputDialog commentDialog = new TextInputDialog(
            isBlank(selected.getUserRatingComment()) ? "" : selected.getUserRatingComment()
        );
        commentDialog.setTitle(t("Commentaire"));
        commentDialog.setHeaderText(t("Commentaire optionnel"));
        commentDialog.setContentText(t("Commentaire") + ":");
        String comment = commentDialog.showAndWait().orElse("");

        boolean rated = cashbackService.submitUserCashbackRating(selected.getIdCashback(), currentUser.getIdUser(), rating, comment);
        if (!rated) {
            showNotification("Erreur", "Impossible d enregistrer votre note.");
            return;
        }

        if (onSuccessRefresh != null) {
            onSuccessRefresh.run();
        }
        showNotification("Succes", "Votre note est envoyee. L admin peut approuver ou refuser un bonus.");
    }

    private void refreshStarButtons(List<Button> starButtons, int selectedStars) {
        if (starButtons == null) {
            return;
        }
        for (int i = 0; i < starButtons.size(); i++) {
            Button starButton = starButtons.get(i);
            boolean filled = i < selectedStars;
            String icon = filled ? "\u2605" : "\u2606";
            String color = filled ? "#F59E0B" : "#CBD5E1";
            starButton.setText(icon);
            starButton.setStyle(
                "-fx-font-size: 24px; " +
                "-fx-text-fill: " + color + "; " +
                "-fx-background-color: transparent; " +
                "-fx-padding: 0 2 0 2; " +
                "-fx-cursor: hand;"
            );
        }
    }

    private void calculateCashback() {
        try {
            if (txtPurchaseAmount == null || txtAppliedRate == null || txtCashbackAmount == null) {
                return;
            }
            double amount = parseDouble(txtPurchaseAmount.getText());
            double rating = resolveSelectedPartnerRating();
            double rate = cashbackService == null
                ? resolveLocalRate(amount) + resolveLocalBonus(rating)
                : cashbackService.resolveEffectiveRatePercent(amount, rating);
            double cashbackAmount = cashbackService == null
                ? amount * (rate / 100.0)
                : cashbackService.calculateCashbackByAmountAndRating(amount, rating);
            txtAppliedRate.setText(String.format(Locale.US, "%.2f", rate));
            txtCashbackAmount.setText(String.format(Locale.US, "%.2f", cashbackAmount));
        } catch (NumberFormatException ex) {
            txtCashbackAmount.setText("0.00");
            txtAppliedRate.setText("1.00");
        }
    }

    private boolean validatePartnerForm() {
        if (txtPartnerName == null || txtPartnerName.getText().trim().isEmpty()) {
            showNotification("Erreur de validation", "Veuillez saisir un nom de partenaire.");
            return false;
        }
        if (txtBaseRate == null || txtBaseRate.getText().trim().isEmpty()) {
            showNotification("Erreur de validation", "Veuillez saisir un taux de cashback de base.");
            return false;
        }
        return true;
    }

    private boolean validateCashbackForm() {
        if (txtPurchaseAmount == null || txtPurchaseAmount.getText().trim().isEmpty()) {
            showNotification("Erreur de validation", "Veuillez saisir un montant d achat.");
            return false;
        }
        double amount;
        try {
            amount = parseDouble(txtPurchaseAmount.getText());
        } catch (NumberFormatException ex) {
            showNotification("Erreur de validation", "Montant d achat invalide.");
            return false;
        }
        if (amount <= 0) {
            showNotification("Erreur de validation", "Le montant d achat doit etre superieur a 0.");
            return false;
        }
        if (dpPurchaseDate == null || dpPurchaseDate.getValue() == null) {
            showNotification("Erreur de validation", "Veuillez selectionner une date d achat.");
            return false;
        }
        return true;
    }

    private void updateTabStyles(Button activeTab) {
        if (tabPartner != null) {
            tabPartner.getStyleClass().remove(TAB_ACTIVE_CLASS);
        }
        if (tabCashback != null) {
            tabCashback.getStyleClass().remove(TAB_ACTIVE_CLASS);
        }
        if (activeTab != null && !activeTab.getStyleClass().contains(TAB_ACTIVE_CLASS)) {
            activeTab.getStyleClass().add(TAB_ACTIVE_CLASS);
        }
    }

    private void animateFadeIn(VBox node) {
        if (node == null) return;
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(ANIMATION_DURATION, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        fade.play();
    }

    private void setVisibleManaged(VBox node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(localizedDateFormatter());
    }

    private String formatMoney(double amount) {
        return "$" + String.format("%.2f", amount);
    }

    private double parseDouble(String value) {
        String normalized = value == null ? "0" : value.trim().replace(",", ".");
        if (normalized.isBlank()) {
            return 0;
        }
        return Double.parseDouble(normalized);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatRate(double rate) {
        return String.format(Locale.US, "%.2f%%", rate);
    }

    private DateTimeFormatter localizedDateFormatter() {
        return DateTimeFormatter.ofPattern("dd MMM yyyy", resolveCurrentLocale());
    }

    private DateTimeFormatter localizedDateTimeFormatter() {
        return DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", resolveCurrentLocale());
    }

    private String formatRatingDisplay(Double rating) {
        return toStars(rating);
    }

    private String toStars(Double ratingValue) {
        if (ratingValue == null) {
            return "\u2606\u2606\u2606\u2606\u2606";
        }
        int rounded = (int) Math.round(ratingValue);
        rounded = Math.max(0, Math.min(5, rounded));
        StringBuilder stars = new StringBuilder(5);
        for (int i = 1; i <= 5; i++) {
            stars.append(i <= rounded ? "\u2605" : "\u2606");
        }
        return stars.toString();
    }

    private String formatBonusDecision(String decision) {
        if (isBlank(decision)) {
            return t("Pending");
        }
        String normalized = safe(decision).trim();
        if ("Approved".equalsIgnoreCase(normalized)) {
            return t("Approved");
        }
        if ("Rejected".equalsIgnoreCase(normalized)) {
            return t("Rejected");
        }
        if ("Pending".equalsIgnoreCase(normalized)) {
            return t("Pending");
        }
        return normalized;
    }

    private double resolveLocalRate(double amount) {
        if (amount < 50.0) {
            return 1.0;
        }
        if (amount <= 200.0) {
            return 2.0;
        }
        return 3.0;
    }

    private double resolveLocalBonus(double rating) {
        return rating > 4.0 ? 1.0 : 0.0;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private Optional<Partenaire> findPartnerByName(String name) {
        if (isBlank(name)) {
            return Optional.empty();
        }
        if (cachedPartenaires != null && !cachedPartenaires.isEmpty()) {
            Optional<Partenaire> fromCache = cachedPartenaires.stream()
                .filter(p -> p != null && p.getNom() != null && p.getNom().trim().equalsIgnoreCase(name.trim()))
                .findFirst();
            if (fromCache.isPresent()) {
                return fromCache;
            }
        }
        return partenaireService.findByName(name);
    }

    private double resolveSelectedPartnerRating() {
        if (cmbPartnerSelection == null) {
            return 0.0;
        }
        String selectedName = cmbPartnerSelection.getValue();
        return findPartnerByName(selectedName).map(Partenaire::getRating).orElse(0.0);
    }

    private void renderPartnerShowcase(List<Partenaire> partenaires) {
        List<Partenaire> showcase = partenaires == null ? List.of() : partenaires.stream()
            .filter(p -> p != null && !isBlank(p.getNom()))
            .sorted(Comparator.comparingDouble(Partenaire::getRating).reversed())
            .limit(3)
            .collect(Collectors.toList());

        bindPartnerCard(showcase, 0, lblPartner1Name, lblPartner1Category, lblPartner1Rate, lblPartner1Rating);
        bindPartnerCard(showcase, 1, lblPartner2Name, lblPartner2Category, lblPartner2Rate, lblPartner2Rating);
        bindPartnerCard(showcase, 2, lblPartner3Name, lblPartner3Category, lblPartner3Rate, lblPartner3Rating);
    }

    private void bindPartnerCard(List<Partenaire> showcase, int index,
                                 Label lblName, Label lblCategory, Label lblRate, Label lblRating) {
        if (lblName == null || lblCategory == null || lblRate == null || lblRating == null) {
            return;
        }

        if (showcase == null || index >= showcase.size()) {
            lblName.setText(t("Partenaire"));
            lblCategory.setText(t("Categorie"));
            lblRate.setText(t("Jusqu a 0%"));
            lblRating.setText("\u2606\u2606\u2606\u2606\u2606");
            return;
        }

        Partenaire p = showcase.get(index);
        double maxRate = p.getTauxCashbackMax() > 0 ? p.getTauxCashbackMax() : p.getTauxCashback();
        lblName.setText(safe(p.getNom()));
        lblCategory.setText(isBlank(p.getCategorie()) ? t("General") : p.getCategorie());
        lblRate.setText(t("Jusqu a") + " " + String.format(Locale.US, "%.0f%%", maxRate));
        lblRating.setText(toStars(p.getRating()));
    }

    private boolean ensureServicesReady() {
        if (cashbackService != null && partenaireService != null) {
            return true;
        }
        String message = serviceInitError == null || serviceInitError.isBlank()
            ? "Service cashback indisponible. Verifiez la connexion base de donnees."
            : serviceInitError;
        showNotification("Cashback indisponible", message);
        return false;
    }

    private void setupCashbackAiSection() {
        if (txtCashbackAiAdvice != null) {
            txtCashbackAiAdvice.setEditable(false);
            txtCashbackAiAdvice.setWrapText(true);
            txtCashbackAiAdvice.setText(t("Cliquez sur \"Analyser cashback IA\" pour obtenir des conseils personnalises."));
        }
        if (lblCashbackAiUpdatedAt != null) {
            lblCashbackAiUpdatedAt.setText(t("Mise a jour: -"));
        }
        setCashbackAiStatus(t("Pret"), "#64748B");
    }

    private void setCashbackAiStatus(String text, String colorHex) {
        if (lblCashbackAiStatus == null) {
            return;
        }
        lblCashbackAiStatus.setText(text);
        lblCashbackAiStatus.setStyle(
            "-fx-text-fill: " + (colorHex == null ? "#64748B" : colorHex) + ";" +
            "-fx-font-weight: 600;"
        );
    }

    private void showNotification(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(t(title));
        alert.setHeaderText(null);
        alert.setContentText(t(message));
        alert.show();
    }
}


