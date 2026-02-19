package com.nexora.bank.Service;

import com.nexora.bank.Models.Cashback;
import com.nexora.bank.Models.User;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class EmailNotificationService {

    private static final String SMTP_USERNAME = fromEnv("NEXORA_SMTP_EMAIL", "jfkdiekrjrjee06@gmail.com");
    private static final String SMTP_PASSWORD = fromEnv("NEXORA_SMTP_APP_PASSWORD", "hkmromvmrirxvhvj");
    private static final String SMTP_FROM_NAME = fromEnv("NEXORA_SMTP_FROM_NAME", "NEXORA Mail");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void sendCashbackSubmittedEmail(User user, Cashback cashback) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email is required for cashback notification.");
        }

        String fullName = ((safe(user.getPrenom()) + " " + safe(user.getNom())).trim());
        if (fullName.isBlank()) {
            fullName = user.getEmail().trim();
        }

        String subject = "Cashback submitted successfully - NEXORA";
        String content = """
            <html>
            <body style='font-family:Segoe UI,Arial,sans-serif;background:#f4f7fb;padding:24px;'>
              <div style='max-width:620px;margin:0 auto;background:#fff;border:1px solid #e6ebf2;border-radius:12px;padding:24px;'>
                <h2 style='margin:0 0 12px;color:#0A2540;'>Cashback received</h2>
                <p style='margin:0 0 16px;color:#334155;'>Hello %s,</p>
                <p style='margin:0 0 16px;color:#334155;'>Your cashback request has been submitted successfully.</p>
                <table style='width:100%%;border-collapse:collapse;'>
                  <tr><td style='padding:8px 0;color:#64748b;'>Partner</td><td style='padding:8px 0;color:#0f172a;font-weight:600;'>%s</td></tr>
                  <tr><td style='padding:8px 0;color:#64748b;'>Purchase amount</td><td style='padding:8px 0;color:#0f172a;font-weight:600;'>%.2f</td></tr>
                  <tr><td style='padding:8px 0;color:#64748b;'>Rate</td><td style='padding:8px 0;color:#0f172a;font-weight:600;'>%.2f%%</td></tr>
                  <tr><td style='padding:8px 0;color:#64748b;'>Cashback amount</td><td style='padding:8px 0;color:#0f172a;font-weight:600;'>%.2f</td></tr>
                  <tr><td style='padding:8px 0;color:#64748b;'>Purchase date</td><td style='padding:8px 0;color:#0f172a;font-weight:600;'>%s</td></tr>
                  <tr><td style='padding:8px 0;color:#64748b;'>Status</td><td style='padding:8px 0;color:#0f172a;font-weight:600;'>%s</td></tr>
                </table>
                <p style='margin:18px 0 0;color:#64748b;font-size:13px;'>You will be notified when this cashback is credited.</p>
              </div>
            </body>
            </html>
            """.formatted(
                escapeHtml(fullName),
                escapeHtml(safe(cashback.getPartenaireNom())),
                cashback.getMontantAchat(),
                cashback.getTauxApplique(),
                cashback.getMontantCashback(),
                cashback.getDateAchat() == null ? "-" : cashback.getDateAchat().format(DATE_FORMATTER),
                escapeHtml(safe(cashback.getStatut()))
        );

        sendPlainEmail(user.getEmail().trim(), subject, content);
    }

    private void sendPlainEmail(String recipientEmail, String subject, String content) {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USERNAME, SMTP_FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=UTF-8");
            Transport.send(message);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send cashback email notification.", ex);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String fromEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
