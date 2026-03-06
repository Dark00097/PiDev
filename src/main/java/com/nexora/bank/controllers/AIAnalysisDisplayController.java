package com.nexora.bank.controllers;

import com.nexora.bank.Utils.AIResponseFormatter;
import com.nexora.bank.Utils.AIResponseFormatter.FormattedAnalysis;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class AIAnalysisDisplayController implements Initializable {

    @FXML private Label lblHeaderTitle;
    @FXML private Label lblHeaderSubtitle;
    @FXML private Label lblTimestamp;
    @FXML private Label lblAccountHolder;
    @FXML private Label lblSummary;
    @FXML private Label lblRiskLevel;
    @FXML private VBox sectionAccountHolder;
    @FXML private VBox sectionSummary;
    @FXML private VBox sectionRiskLevel;
    @FXML private VBox sectionSecurityAdvice;
    @FXML private VBox sectionSuspicious;
    @FXML private VBox containerRiskReasons;
    @FXML private VBox containerSecurityAdvice;
    @FXML private VBox containerSuspicious;
    @FXML private Button btnClose;

    private FormattedAnalysis currentAnalysis;
    private Consumer<Void> refreshCallback;
    private Runnable closeCallback;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        lblTimestamp.setText(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
        ));
    }

    public void loadAnalysis(String geminiResponse) {
        this.currentAnalysis = AIResponseFormatter.parseGeminiResponse(geminiResponse);
        displayAnalysis();
    }

    public void loadFormattedAnalysis(FormattedAnalysis analysis) {
        this.currentAnalysis = analysis;
        displayAnalysis();
    }

    public void setRefreshCallback(Consumer<Void> callback) {
        this.refreshCallback = callback;
    }

    public void setCloseCallback(Runnable callback) {
        this.closeCallback = callback;
    }

    private void displayAnalysis() {
        if (currentAnalysis == null) {
            hideAllSections();
            return;
        }

        if (isNotEmpty(currentAnalysis.getAccountHolder())) {
            lblAccountHolder.setText(currentAnalysis.getAccountHolder());
            sectionAccountHolder.setVisible(true);
            sectionAccountHolder.setManaged(true);
        } else {
            sectionAccountHolder.setVisible(false);
            sectionAccountHolder.setManaged(false);
        }

        if (isNotEmpty(currentAnalysis.getSummary())) {
            lblSummary.setText(currentAnalysis.getSummary());
            sectionSummary.setVisible(true);
            sectionSummary.setManaged(true);
        } else {
            sectionSummary.setVisible(false);
            sectionSummary.setManaged(false);
        }

        if (isNotEmpty(currentAnalysis.getRiskLevel())) {
            lblRiskLevel.setText(currentAnalysis.getRiskLevel());
            applyRiskLevelStyle(currentAnalysis.getRiskLevel());

            containerRiskReasons.getChildren().clear();
            for (String reason : currentAnalysis.getRiskReasons()) {
                containerRiskReasons.getChildren().add(createListItem(reason, "warning"));
            }

            sectionRiskLevel.setVisible(true);
            sectionRiskLevel.setManaged(true);
        } else {
            sectionRiskLevel.setVisible(false);
            sectionRiskLevel.setManaged(false);
        }

        if (!currentAnalysis.getSecurityAdvice().isEmpty()) {
            containerSecurityAdvice.getChildren().clear();
            for (String advice : currentAnalysis.getSecurityAdvice()) {
                containerSecurityAdvice.getChildren().add(createListItem(advice, "success"));
            }
            sectionSecurityAdvice.setVisible(true);
            sectionSecurityAdvice.setManaged(true);
        } else {
            sectionSecurityAdvice.setVisible(false);
            sectionSecurityAdvice.setManaged(false);
        }

        if (!currentAnalysis.getSuspiciousItems().isEmpty()) {
            containerSuspicious.getChildren().clear();
            for (String item : currentAnalysis.getSuspiciousItems()) {
                containerSuspicious.getChildren().add(createListItem(item, "danger"));
            }
            sectionSuspicious.setVisible(true);
            sectionSuspicious.setManaged(true);
        } else {
            sectionSuspicious.setVisible(false);
            sectionSuspicious.setManaged(false);
        }
    }

    private HBox createListItem(String text, String type) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("ai-list-item");

        StackPane iconContainer = new StackPane();
        iconContainer.getStyleClass().add("ai-list-item-icon");

        SVGPath icon = new SVGPath();
        switch (type) {
            case "success" -> {
                item.getStyleClass().add("ai-list-item-success");
                iconContainer.getStyleClass().add("ai-list-item-icon-check");
                icon.setContent("M20 6L9 17l-5-5");
            }
            case "warning" -> {
                item.getStyleClass().add("ai-list-item-warning");
                iconContainer.getStyleClass().add("ai-list-item-icon-warning");
                icon.setContent("M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z");
            }
            case "danger" -> {
                item.getStyleClass().add("ai-list-item-danger");
                iconContainer.getStyleClass().add("ai-list-item-icon-alert");
                icon.setContent("M12 8v4m0 4h.01M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z");
            }
            default -> icon.setContent("M9 12l2 2 4-4");
        }

        iconContainer.getChildren().add(icon);

        Label label = new Label(text);
        label.getStyleClass().add("ai-list-item-text");
        label.setWrapText(true);

        item.getChildren().addAll(iconContainer, label);
        return item;
    }

    private void applyRiskLevelStyle(String level) {
        lblRiskLevel.getStyleClass().removeAll(
            "ai-risk-badge-low",
            "ai-risk-badge-medium",
            "ai-risk-badge-high",
            "ai-risk-badge-critical"
        );

        switch (level.toUpperCase()) {
            case "LOW" -> lblRiskLevel.getStyleClass().add("ai-risk-badge-low");
            case "MEDIUM" -> lblRiskLevel.getStyleClass().add("ai-risk-badge-medium");
            case "HIGH" -> lblRiskLevel.getStyleClass().add("ai-risk-badge-high");
            case "CRITICAL" -> lblRiskLevel.getStyleClass().add("ai-risk-badge-critical");
            default -> {
            }
        }
    }

    private void hideAllSections() {
        sectionAccountHolder.setVisible(false);
        sectionAccountHolder.setManaged(false);
        sectionSummary.setVisible(false);
        sectionSummary.setManaged(false);
        sectionRiskLevel.setVisible(false);
        sectionRiskLevel.setManaged(false);
        sectionSecurityAdvice.setVisible(false);
        sectionSecurityAdvice.setManaged(false);
        sectionSuspicious.setVisible(false);
        sectionSuspicious.setManaged(false);
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @FXML
    private void handleClose() {
        if (closeCallback != null) {
            closeCallback.run();
        } else {
            Stage stage = (Stage) btnClose.getScene().getWindow();
            stage.close();
        }
    }

    @FXML
    private void handleExportReport() {
        if (currentAnalysis == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Security Report");
        fileChooser.setInitialFileName("security-report-" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".txt");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        Stage stage = (Stage) btnClose.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file == null) {
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("===============================================================");
            writer.println("                  NEXORA BANK SECURITY REPORT                  ");
            writer.println("===============================================================");
            writer.println();
            writer.println("Generated: " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss")
            ));
            writer.println();

            if (isNotEmpty(currentAnalysis.getAccountHolder())) {
                writer.println("ACCOUNT HOLDER");
                writer.println("---------------------------------------------------------------");
                writer.println(currentAnalysis.getAccountHolder());
                writer.println();
            }

            if (isNotEmpty(currentAnalysis.getSummary())) {
                writer.println("CURRENT SITUATION");
                writer.println("---------------------------------------------------------------");
                writer.println(currentAnalysis.getSummary());
                writer.println();
            }

            if (isNotEmpty(currentAnalysis.getRiskLevel())) {
                writer.println("SECURITY RISK ASSESSMENT");
                writer.println("---------------------------------------------------------------");
                writer.println("Risk Level: " + currentAnalysis.getRiskLevel());
                writer.println();
                if (!currentAnalysis.getRiskReasons().isEmpty()) {
                    writer.println("Contributing Factors:");
                    for (String reason : currentAnalysis.getRiskReasons()) {
                        writer.println("  - " + reason);
                    }
                    writer.println();
                }
            }

            if (!currentAnalysis.getSecurityAdvice().isEmpty()) {
                writer.println("SECURITY RECOMMENDATIONS");
                writer.println("---------------------------------------------------------------");
                for (String advice : currentAnalysis.getSecurityAdvice()) {
                    writer.println("  - " + advice);
                }
                writer.println();
            }

            if (!currentAnalysis.getSuspiciousItems().isEmpty()) {
                writer.println("ITEMS REQUIRING ATTENTION");
                writer.println("---------------------------------------------------------------");
                for (String item : currentAnalysis.getSuspiciousItems()) {
                    writer.println("  - " + item);
                }
                writer.println();
            }

            writer.println("===============================================================");
            writer.println("              End of NEXORA Bank Security Report              ");
            writer.println("===============================================================");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefreshAnalysis() {
        if (refreshCallback != null) {
            refreshCallback.accept(null);
        }
    }
}
