//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.nexora.bank.Utils;

import com.nexora.bank.Models.Credit;
import com.nexora.bank.Models.GarantieCredit;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public final class PdfReportUtil {
    private static final float MARGIN = 32.0F;
    private static final float LINE_HEIGHT = 14.0F;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private PdfReportUtil() {
    }

    public static void exportCredits(List<Credit> credits, File file) throws IOException {
        try (PDDocument document = new PDDocument()) {
            WriterState writer = startPage(document, "Credits Report", "Rows: " + credits.size());
            writer.y = writeLine(writer.stream, writer.y, "ID  Compte  Type                Demande      Accord       Duree  Taux    Statut     Date");
            writer.y = writeLine(writer.stream, writer.y, repeat("-", 120));

            for(Credit credit : credits) {
                if (writer.y <= 32.0F) {
                    writer.stream.close();
                    writer = startPage(document, "Credits Report (continued)", "");
                }

                String row = String.format("%-3s %-6s %-19s %-12s %-12s %-6s %-7s %-10s %-10s", credit.getIdCredit(), credit.getIdCompte(), truncate(safeText(credit.getTypeCredit()), 19), formatMoney(credit.getMontantDemande()), credit.getMontantAccord() == null ? "-" : formatMoney(credit.getMontantAccord()), credit.getDuree(), String.format("%.2f%%", credit.getTauxInteret()), truncate(safeText(credit.getStatut()), 10), truncate(safeText(credit.getDateDemande()), 10));
                writer.y = writeLine(writer.stream, writer.y, row);
            }

            writer.stream.close();
            document.save(file);
        }

    }

    public static void exportGaranties(List<GarantieCredit> garanties, File file) throws IOException {
        try (PDDocument document = new PDDocument()) {
            WriterState writer = startPage(document, "Garanties Report", "Rows: " + garanties.size());
            writer.y = writeLine(writer.stream, writer.y, "ID  Credit  Type                Valeur Est.   Valeur Ret.  Garant          Statut     Date");
            writer.y = writeLine(writer.stream, writer.y, repeat("-", 120));

            for(GarantieCredit garantie : garanties) {
                if (writer.y <= 32.0F) {
                    writer.stream.close();
                    writer = startPage(document, "Garanties Report (continued)", "");
                }

                String row = String.format("%-3s %-6s %-19s %-12s %-12s %-15s %-10s %-10s", garantie.getIdGarantie(), garantie.getIdCredit(), truncate(safeText(garantie.getTypeGarantie()), 19), formatMoney(garantie.getValeurEstimee()), formatMoney(garantie.getValeurRetenue()), truncate(safeText(garantie.getNomGarant()), 15), truncate(safeText(garantie.getStatut()), 10), truncate(safeText(garantie.getDateEvaluation()), 10));
                writer.y = writeLine(writer.stream, writer.y, row);
            }

            writer.stream.close();
            document.save(file);
        }

    }

    private static WriterState startPage(PDDocument document, String title, String subtitle) throws IOException {
        PDRectangle landscape = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        PDPage page = new PDPage(landscape);
        document.addPage(page);
        PDPageContentStream stream = new PDPageContentStream(document, page);
        float y = page.getMediaBox().getHeight() - 32.0F;
        y = writeHeader(stream, y, title, subtitle);
        return new WriterState(stream, y);
    }

    private static float writeHeader(PDPageContentStream stream, float y, String title, String subtitle) throws IOException {
        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA_BOLD, 14.0F);
        stream.newLineAtOffset(32.0F, y);
        stream.showText(sanitizeForPdf(title));
        stream.endText();
        y -= 14.0F;
        LocalDateTime var10000 = LocalDateTime.now();
        String generated = "Generated: " + var10000.format(DATE_TIME_FORMAT);
        String detail = subtitle != null && !subtitle.isBlank() ? subtitle + " | " + generated : generated;
        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA, 10.0F);
        stream.newLineAtOffset(32.0F, y);
        stream.showText(sanitizeForPdf(detail));
        stream.endText();
        return y - 14.0F;
    }

    private static float writeLine(PDPageContentStream stream, float y, String text) throws IOException {
        stream.beginText();
        stream.setFont(PDType1Font.COURIER, 9.0F);
        stream.newLineAtOffset(32.0F, y);
        stream.showText(sanitizeForPdf(text));
        stream.endText();
        return y - 14.0F;
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        } else if (maxLength <= 3) {
            return value.substring(0, maxLength);
        } else {
            String var10000 = value.substring(0, maxLength - 3);
            return var10000 + "...";
        }
    }

    private static String formatMoney(double amount) {
        return String.format("%,.2f", amount);
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static String repeat(String value, int count) {
        return value.repeat(Math.max(0, count));
    }

    private static String sanitizeForPdf(String text) {
        return text != null && !text.isBlank() ? text.replaceAll("[^\\x20-\\x7E]", "?") : "";
    }

    private static final class WriterState {
        private final PDPageContentStream stream;
        private float y;

        private WriterState(PDPageContentStream stream, float y) {
            this.stream = stream;
            this.y = y;
        }
    }
}
