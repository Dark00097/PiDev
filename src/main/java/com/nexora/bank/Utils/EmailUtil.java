package com.nexora.bank.Utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

/**
 * Utilitaire d'envoi d'emails via Gmail SMTP.
 *
 * ⚠️ IMPORTANT : N'utilise pas ton mot de passe Gmail normal !
 *    Génère un "Mot de passe d'application" Google :
 *    → Mon compte Google > Sécurité > Validation en 2 étapes > Mots de passe des applications
 */
public class EmailUtil {

    // 🔧 Configure ici tes identifiants
    private static final String EXPEDITEUR_EMAIL = "najibabensaid8@gmail.com";     // ← change ici
    private static final String EXPEDITEUR_PASSWORD = "fcaj pxds mvzl fbuu" + //
                ""; // ← mot de passe d'application Google

    /**
     * Envoie un email de confirmation de transaction.
     *
     * @param destinataire  Email du client
     * @param categorie     Catégorie de la transaction
     * @param montant       Montant de la transaction
     * @param type          Type (Débit / Crédit)
     * @param statut        Statut de la transaction
     */
    public static void envoyerConfirmationTransaction(
            String destinataire,
            String categorie,
            double montant,
            String type,
            String statut
    ) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.debug", "true"); // ← affiche les détails dans la console

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EXPEDITEUR_EMAIL, EXPEDITEUR_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EXPEDITEUR_EMAIL, "Nexora Bank"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject("✅ Confirmation de votre transaction - Nexora Bank");
            message.setContent(buildEmailBody(categorie, montant, type, statut), "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("Email envoyé avec succès à : " + destinataire);

        } catch (Exception e) {
            System.out.println("❌ ERREUR ENVOI EMAIL : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Construit le corps HTML de l'email.
     */
    private static String buildEmailBody(String categorie, double montant, String type, String statut) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #e5e7eb; border-radius: 10px; overflow: hidden;">
                  <div style="background-color: #1e40af; padding: 24px; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 22px;">🏦 Nexora Bank</h1>
                  </div>
                  <div style="padding: 32px;">
                    <h2 style="color: #111827;">✅ Transaction confirmée</h2>
                    <p style="color: #6b7280;">Votre transaction a bien été enregistrée. Voici le récapitulatif :</p>
                    <table style="width: 100%%; border-collapse: collapse; margin-top: 16px;">
                      <tr style="background-color: #f3f4f6;">
                        <td style="padding: 12px; font-weight: bold; color: #374151;">Catégorie</td>
                        <td style="padding: 12px; color: #111827;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding: 12px; font-weight: bold; color: #374151;">Montant</td>
                        <td style="padding: 12px; color: #1e40af; font-weight: bold; font-size: 18px;">%.2f DT</td>
                      </tr>
                      <tr style="background-color: #f3f4f6;">
                        <td style="padding: 12px; font-weight: bold; color: #374151;">Type</td>
                        <td style="padding: 12px; color: #111827;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding: 12px; font-weight: bold; color: #374151;">Statut</td>
                        <td style="padding: 12px; color: #16a34a; font-weight: bold;">%s</td>
                      </tr>
                    </table>
                    <p style="margin-top: 24px; color: #6b7280; font-size: 13px;">
                      Si vous n'êtes pas à l'origine de cette transaction, contactez immédiatement notre support.
                    </p>
                  </div>
                  <div style="background-color: #f9fafb; padding: 16px; text-align: center;">
                    <p style="color: #9ca3af; font-size: 12px; margin: 0;">© 2025 Nexora Bank — Tous droits réservés</p>
                  </div>
                </div>
                """.formatted(categorie, montant, type, statut);
    }
}