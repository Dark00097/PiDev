package com.nexora.bank.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIResponseFormatter {

    public static class AnalysisSection {
        private String title;
        private String content;
        private List<String> bulletPoints;
        private String riskLevel;
        private String riskColor;

        public AnalysisSection() {
            this.bulletPoints = new ArrayList<>();
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public List<String> getBulletPoints() { return bulletPoints; }
        public void addBulletPoint(String point) { this.bulletPoints.add(point); }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getRiskColor() { return riskColor; }
        public void setRiskColor(String riskColor) { this.riskColor = riskColor; }
    }

    public static class FormattedAnalysis {
        private String accountHolder;
        private String summary;
        private String riskLevel;
        private String riskColor;
        private List<String> riskReasons;
        private List<String> securityAdvice;
        private List<String> suspiciousItems;
        private String overallAssessment;

        public FormattedAnalysis() {
            this.riskReasons = new ArrayList<>();
            this.securityAdvice = new ArrayList<>();
            this.suspiciousItems = new ArrayList<>();
        }

        public String getAccountHolder() { return accountHolder; }
        public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getRiskColor() { return riskColor; }
        public void setRiskColor(String riskColor) { this.riskColor = riskColor; }
        public List<String> getRiskReasons() { return riskReasons; }
        public void addRiskReason(String reason) { this.riskReasons.add(reason); }
        public List<String> getSecurityAdvice() { return securityAdvice; }
        public void addSecurityAdvice(String advice) { this.securityAdvice.add(advice); }
        public List<String> getSuspiciousItems() { return suspiciousItems; }
        public void addSuspiciousItem(String item) { this.suspiciousItems.add(item); }
        public String getOverallAssessment() { return overallAssessment; }
        public void setOverallAssessment(String assessment) { this.overallAssessment = assessment; }
    }

    public static String stripMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String result = text;

        result = result.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        result = result.replaceAll("__([^_]+)__", "$1");

        result = result.replaceAll("(?<!\\*)\\*([^*]+)\\*(?!\\*)", "$1");
        result = result.replaceAll("(?<!_)_([^_]+)_(?!_)", "$1");

        result = result.replaceAll("^#{1,6}\\s*", "");
        result = result.replaceAll("\\n#{1,6}\\s*", "\n");

        result = result.replaceAll("^---+$", "");
        result = result.replaceAll("\\n---+\\n", "\n");

        result = result.replaceAll("```[^`]*```", "");
        result = result.replaceAll("`([^`]+)`", "$1");

        result = result.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");

        result = result.replaceAll("(?m)^[*-]\\s+", "- ");
        result = result.replaceAll("\\n[*-]\\s+", "\n- ");

        result = result.replaceAll("\\n{3,}", "\n\n");
        return result.trim();
    }

    public static FormattedAnalysis parseGeminiResponse(String rawResponse) {
        FormattedAnalysis analysis = new FormattedAnalysis();

        if (rawResponse == null || rawResponse.isEmpty()) {
            return analysis;
        }

        String cleaned = stripMarkdown(rawResponse);
        String lower = cleaned.toLowerCase(Locale.ROOT);

        Pattern namePattern = Pattern.compile("(?:analysis of|account activity for|regarding|analyse de|activite du compte de|concernant)\\s+([A-Za-z]+(?:\\s+\\d+)?\\s+[A-Za-z]+)'?s?", Pattern.CASE_INSENSITIVE);
        Matcher nameMatcher = namePattern.matcher(cleaned);
        if (nameMatcher.find()) {
            analysis.setAccountHolder(nameMatcher.group(1).trim());
        }

        String[] sections = cleaned.split("\\d+\\)\\s*");

        for (String section : sections) {
            section = section.trim();
            if (section.isEmpty()) {
                continue;
            }

            String lowerSection = section.toLowerCase(Locale.ROOT);

            if (startsWithAny(lowerSection, "current situation", "summary", "situation actuelle", "resume", "resume court")) {
                analysis.setSummary(extractSectionContent(section));
                continue;
            }

            if (containsAny(lowerSection, "security risk level", "risk level", "niveau de risque", "evaluation du risque", "risque de securite")) {
                parseRiskSection(section, analysis);
                continue;
            }

            if (containsAny(lowerSection, "security advice", "practical security", "recommandations de securite", "conseils de securite", "mesures de securite")
                || lowerSection.contains("recommand")
                || lowerSection.contains("conseil")) {
                List<String> advice = extractBulletPoints(section);
                advice.forEach(analysis::addSecurityAdvice);
                continue;
            }

            if (containsAny(lowerSection, "suspicious", "concerns", "elements suspects", "points d attention", "anormal", "a surveiller")
                || lowerSection.contains("suspect")
                || lowerSection.contains("attention")) {
                List<String> items = extractBulletPoints(section);
                if (items.isEmpty()) {
                    String content = extractSectionContent(section);
                    if (!content.isEmpty()) {
                        analysis.addSuspiciousItem(content);
                    }
                } else {
                    items.forEach(analysis::addSuspiciousItem);
                }
            }
        }

        if (isBlank(analysis.getSummary())) {
            String fallback = extractFirstParagraph(lower, cleaned);
            if (!fallback.isBlank()) {
                analysis.setSummary(fallback);
            }
        }

        return analysis;
    }

    private static String extractSectionContent(String section) {
        String content = section.replaceFirst("^[^:]+:\\s*", "");
        content = content.replaceFirst("^[A-Za-z\\s]+summary[:\\s]*", "");
        content = content.replaceFirst("^[A-Za-z\\s]+situation actuelle[:\\s]*", "");
        return content.trim();
    }

    private static void parseRiskSection(String section, FormattedAnalysis analysis) {
        Pattern riskPattern = Pattern.compile("(LOW|MEDIUM|HIGH|CRITICAL|FAIBLE|MOYEN|MODERE|ELEVE|HAUT|CRITIQUE)", Pattern.CASE_INSENSITIVE);
        Matcher riskMatcher = riskPattern.matcher(section);
        if (riskMatcher.find()) {
            String normalizedLevel = normalizeRiskLevel(riskMatcher.group(1));
            analysis.setRiskLevel(normalizedLevel);

            switch (normalizedLevel) {
                case "LOW" -> analysis.setRiskColor("#10B981");
                case "MEDIUM" -> analysis.setRiskColor("#F59E0B");
                case "HIGH" -> analysis.setRiskColor("#EF4444");
                case "CRITICAL" -> analysis.setRiskColor("#DC2626");
                default -> analysis.setRiskColor("#6B7280");
            }
        }

        List<String> reasons = extractBulletPoints(section);
        reasons.forEach(analysis::addRiskReason);
    }

    private static String normalizeRiskLevel(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LOW", "FAIBLE" -> "LOW";
            case "MEDIUM", "MOYEN", "MODERE" -> "MEDIUM";
            case "HIGH", "HAUT", "ELEVE" -> "HIGH";
            case "CRITICAL", "CRITIQUE" -> "CRITICAL";
            default -> "MEDIUM";
        };
    }

    private static List<String> extractBulletPoints(String section) {
        List<String> points = new ArrayList<>();
        String[] lines = section.split("\\n");

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("-") || line.startsWith("*") || line.startsWith(".") || line.matches("^\\d+\\..*")) {
                String point = line.replaceFirst("^[\\-*.]\\s*", "");
                point = point.replaceFirst("^\\d+\\.\\s*", "");
                point = stripMarkdown(point).trim();
                if (!point.isEmpty()) {
                    points.add(point);
                }
            }
        }

        return points;
    }

    private static String extractFirstParagraph(String lower, String original) {
        if (lower == null || original == null) {
            return "";
        }
        String[] paragraphs = original.split("\\n\\n+");
        for (String paragraph : paragraphs) {
            String trimmed = paragraph == null ? "" : paragraph.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (trimmed.matches("^\\d+\\).*")) {
                continue;
            }
            return trimmed;
        }
        return "";
    }

    private static boolean startsWithAny(String value, String... keys) {
        if (value == null) {
            return false;
        }
        for (String key : keys) {
            if (value.startsWith(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String value, String... keys) {
        if (value == null) {
            return false;
        }
        for (String key : keys) {
            if (value.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String toPlainText(FormattedAnalysis analysis) {
        StringBuilder sb = new StringBuilder();

        if (!isBlank(analysis.getAccountHolder())) {
            sb.append("Analyse du compte: ").append(analysis.getAccountHolder()).append("\n\n");
        }

        if (!isBlank(analysis.getSummary())) {
            sb.append("SITUATION ACTUELLE\n");
            sb.append(analysis.getSummary()).append("\n\n");
        }

        if (!isBlank(analysis.getRiskLevel())) {
            sb.append("NIVEAU DE RISQUE: ").append(analysis.getRiskLevel()).append("\n");
            if (!analysis.getRiskReasons().isEmpty()) {
                sb.append("Raisons:\n");
                for (String reason : analysis.getRiskReasons()) {
                    sb.append("  - ").append(reason).append("\n");
                }
            }
            sb.append("\n");
        }

        if (!analysis.getSecurityAdvice().isEmpty()) {
            sb.append("RECOMMANDATIONS DE SECURITE\n");
            for (String advice : analysis.getSecurityAdvice()) {
                sb.append("  - ").append(advice).append("\n");
            }
            sb.append("\n");
        }

        if (!analysis.getSuspiciousItems().isEmpty()) {
            sb.append("POINTS A SURVEILLER\n");
            for (String item : analysis.getSuspiciousItems()) {
                sb.append("  - ").append(item).append("\n");
            }
        }

        return sb.toString().trim();
    }
}
