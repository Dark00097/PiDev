package com.nexora.bank.Utils;

import java.time.Instant;

public class EmailTemplates {

    private static final String NAVY_BLUE = "#0A2540";
    private static final String TEAL = "#00B4A0";
    private static final String LIGHT_GRAY = "#F8FAFB";
    private static final String BORDER_GRAY = "#E4E7EB";
    private static final String TEXT_PRIMARY = "#1A1A1A";
    private static final String TEXT_SECONDARY = "#6B7280";
    private static final String SUCCESS_BG = "#ECFDF5";
    private static final String SUCCESS_BORDER = "#10B981";
    private static final String WARNING_BG = "#FEF3C7";
    private static final String WARNING_BORDER = "#F59E0B";
    private static final String ERROR_BG = "#FEE2E2";
    private static final String ERROR_BORDER = "#EF4444";
    
    private static final String LOGO_URL = "https://res.cloudinary.com/drj2hrug3/image/upload/v1771198690/logo_aohnu4.png";
    private static final String WORDMARK_URL = "https://res.cloudinary.com/drj2hrug3/image/upload/v1771198654/wordmark_pjoslk.png";

    // Base HTML Template with Enhanced Responsive Design
    private static String getEmailTemplate(String content, String preheader) {
        return "<!DOCTYPE html>"
            + "<html lang='en' xmlns='http://www.w3.org/1999/xhtml' xmlns:v='urn:schemas-microsoft-com:vml' xmlns:o='urn:schemas-microsoft-com:office:office'>"
            + "<head>"
            + "<meta charset='utf-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "<meta http-equiv='X-UA-Compatible' content='IE=edge'>"
            + "<meta name='x-apple-disable-message-reformatting'>"
            + "<meta name='format-detection' content='telephone=no,address=no,email=no,date=no'>"
            + "<title>NEXORA Bank</title>"
            + "<!--[if mso]>"
            + "<noscript>"
            + "<xml>"
            + "<o:OfficeDocumentSettings>"
            + "<o:PixelsPerInch>96</o:PixelsPerInch>"
            + "</o:OfficeDocumentSettings>"
            + "</xml>"
            + "</noscript>"
            + "<![endif]-->"
            + "<style>"
            + "* { margin: 0; padding: 0; box-sizing: border-box; }"
            + "body { margin: 0; padding: 0; -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; }"
            + "table, td { border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; }"
            + "img { border: 0; height: auto; line-height: 100%; outline: none; text-decoration: none; -ms-interpolation-mode: bicubic; display: block; }"
            + "p { margin: 0; padding: 0; }"
            + "a { text-decoration: none; }"
            + ".button:hover { opacity: 0.88 !important; transform: translateY(-1px); }"
            + "@media only screen and (max-width: 620px) {"
            + "  .email-container { width: 100% !important; }"
            + "  .content-padding { padding: 30px 24px !important; }"
            + "  .header-padding { padding: 36px 24px !important; }"
            + "  .footer-padding { padding: 32px 24px !important; }"
            + "  .mobile-text { font-size: 15px !important; line-height: 24px !important; }"
            + "  .mobile-heading { font-size: 24px !important; line-height: 32px !important; }"
            + "  .mobile-subheading { font-size: 18px !important; }"
            + "  .button { padding: 14px 28px !important; font-size: 15px !important; }"
            + "  .otp-code { font-size: 28px !important; letter-spacing: 6px !important; }"
            + "  .divider-padding { padding: 0 24px !important; }"
            + "}"
            + "</style>"
            + "</head>"
            + "<body style='margin:0;padding:0;width:100%;background-color:#F0F4F8;'>"
            
            // Preheader text (hidden but shows in preview)
            + "<div style='display:none;font-size:1px;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden;color:#F0F4F8;'>"
            + preheader
            + "&#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847; &#847;"
            + "</div>"
            
            // Outer table
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='background-color:#F0F4F8;padding:40px 0;'>"
            + "<tr><td align='center' style='padding:0 20px;'>"
            
            // Main Container
            + "<!--[if mso]>"
            + "<table role='presentation' width='600' cellpadding='0' cellspacing='0' border='0'><tr><td>"
            + "<![endif]-->"
            + "<table role='presentation' class='email-container' width='600' cellpadding='0' cellspacing='0' border='0' style='max-width:600px;width:100%;background-color:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.05), 0 10px 20px rgba(0,0,0,0.08);'>"
            
            // Header with Logo
            + "<tr><td class='header-padding' style='background-color:" + NAVY_BLUE + ";padding:48px 40px;text-align:center;'>"
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0'>"
            + "<tr><td align='center'>"
            + "<img src='" + LOGO_URL + "' alt='NEXORA Logo' width='56' height='54' style='margin:0 auto 16px;display:block;'>"
            + "<img src='" + WORDMARK_URL + "' alt='NEXORA Bank' width='140' height='54' style='margin:0 auto;display:block;'>"
            + "</td></tr>"
            + "</table>"
            + "</td></tr>"
            
            // Main Content
            + "<tr><td class='content-padding' style='padding:48px 48px 40px;'>"
            + content
            + "</td></tr>"
            
            // Divider
            + "<tr><td class='divider-padding' style='padding:0 48px;'>"
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0'>"
            + "<tr><td style='border-top:1px solid " + BORDER_GRAY + ";'></td></tr>"
            + "</table>"
            + "</td></tr>"
            
            // Footer
            + "<tr><td class='footer-padding' style='padding:40px 48px;background-color:" + LIGHT_GRAY + ";'>"
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0'>"
            
            // Help section
            + "<tr><td align='center' style='padding-bottom:16px;'>"
            + "<p style='margin:0;font-size:14px;line-height:20px;color:" + TEXT_SECONDARY + ";'>"
            + "Need assistance? <a href='mailto:support@nexora.bank' style='color:" + TEAL + ";text-decoration:none;font-weight:500;'>Contact Support</a>"
            + "</p>"
            + "</td></tr>"
            
            // Divider line
            + "<tr><td style='padding:16px 0;'>"
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0'>"
            + "<tr><td style='border-top:1px solid " + BORDER_GRAY + ";'></td></tr>"
            + "</table>"
            + "</td></tr>"
            
            // Security notice
            + "<tr><td align='center' style='padding-bottom:12px;'>"
            + "<p style='margin:0;font-size:12px;line-height:18px;color:" + TEXT_SECONDARY + ";'>"
            + "This is an automated message from NEXORA Bank.<br>"
            + "Please do not reply to this email."
            + "</p>"
            + "</td></tr>"
            
            // Copyright
            + "<tr><td align='center'>"
            + "<p style='margin:0;font-size:12px;line-height:18px;color:#9CA3AF;'>"
            + "&copy; " + java.time.Year.now().getValue() + " NEXORA Bank. All rights reserved."
            + "</p>"
            + "</td></tr>"
            
            + "</table>"
            + "</td></tr>"
            
            + "</table>"
            + "<!--[if mso]>"
            + "</td></tr></table>"
            + "<![endif]-->"
            
            + "</td></tr></table>"
            + "</body>"
            + "</html>";
    }

    // Helper method for creating buttons
    private static String createButton(String text, String url, String color) {
        return "<table role='presentation' cellpadding='0' cellspacing='0' border='0' style='margin:28px 0;'>"
            + "<tr><td style='border-radius:8px;background-color:" + color + ";'>"
            + "<a href='" + url + "' class='button' style='display:inline-block;padding:16px 32px;font-size:16px;font-weight:600;color:#FFFFFF;text-decoration:none;border-radius:8px;transition:all 0.2s ease;'>"
            + text
            + "</a>"
            + "</td></tr>"
            + "</table>";
    }

    // Helper method for info boxes
    private static String createInfoBox(String content, String bgColor, String borderColor) {
        return "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin:24px 0;'>"
            + "<tr><td style='background-color:" + bgColor + ";border-left:4px solid " + borderColor + ";border-radius:8px;padding:20px;'>"
            + content
            + "</td></tr>"
            + "</table>";
    }

    // ==================== EMAIL TEMPLATES ====================

    // 1. OTP VERIFICATION
    public static String otpSubject() {
        return "Your NEXORA Verification Code";
    }

    public static String otpBody(String otpCode, Instant expiresAt) {
        String content = 
            // Greeting
            "<p style='margin:0 0 24px;font-size:16px;line-height:24px;color:" + TEXT_PRIMARY + ";' class='mobile-text'>"
            + "Hello,"
            + "</p>"
            
            // Main heading
            + "<h1 style='margin:0 0 16px;font-size:28px;font-weight:700;line-height:36px;color:" + NAVY_BLUE + ";letter-spacing:-0.5px;' class='mobile-heading'>"
            + "Verification Code"
            + "</h1>"
            
            // Description
            + "<p style='margin:0 0 32px;font-size:16px;line-height:24px;color:" + TEXT_SECONDARY + ";' class='mobile-text'>"
            + "Please use the following one-time password to complete your authentication. This code is valid for 5 minutes."
            + "</p>"
            
            // OTP Code Display
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin:32px 0;'>"
            + "<tr><td align='center' style='background-color:" + LIGHT_GRAY + ";border:2px solid " + BORDER_GRAY + ";border-radius:12px;padding:32px 24px;'>"
            + "<p style='margin:0 0 8px;font-size:12px;font-weight:600;letter-spacing:1px;color:" + TEXT_SECONDARY + ";text-transform:uppercase;'>"
            + "Your Code"
            + "</p>"
            + "<p class='otp-code' style='margin:0;font-size:36px;font-weight:700;letter-spacing:8px;color:" + NAVY_BLUE + ";font-family:Courier New,Consolas,monospace;'>"
            + otpCode
            + "</p>"
            + "</td></tr>"
            + "</table>"
            
            // Expiration info
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin:24px 0;'>"
            + "<tr>"
            + "<td style='padding:12px 0;border-bottom:1px solid " + BORDER_GRAY + ";'>"
            + "<p style='margin:0;font-size:14px;line-height:20px;color:" + TEXT_SECONDARY + ";'>"
            + "<strong style='color:" + TEXT_PRIMARY + ";font-weight:600;'>Valid for:</strong> 5 minutes"
            + "</p>"
            + "</td>"
            + "</tr>"
            + "<tr>"
            + "<td style='padding:12px 0;'>"
            + "<p style='margin:0;font-size:14px;line-height:20px;color:" + TEXT_SECONDARY + ";'>"
            + "<strong style='color:" + TEXT_PRIMARY + ";font-weight:600;'>Expires at (UTC):</strong> " + expiresAt
            + "</p>"
            + "</td>"
            + "</tr>"
            + "</table>"
            
            // Security warning
            + createInfoBox(
                "<p style='margin:0;font-size:14px;line-height:21px;color:#92400E;'>"
                + "<strong style='display:block;margin-bottom:6px;color:#78350F;'>🔒 Security Notice</strong>"
                + "Never share this code with anyone, including NEXORA staff. We will never ask for your verification code."
                + "</p>",
                WARNING_BG,
                WARNING_BORDER
            )
            
            // Footer note
            + "<p style='margin:32px 0 0;font-size:14px;line-height:21px;color:" + TEXT_SECONDARY + ";'>"
            + "If you didn't request this code, please ignore this email or contact our security team immediately."
            + "</p>";
        
        return getEmailTemplate(content, "Your verification code is " + otpCode + ". Valid for 5 minutes.");
    }

    // 2. ACCOUNT APPROVED
    public static String accountApprovedSubject() {
        return "Welcome to NEXORA - Account Approved";
    }

    public static String accountApprovedBody() {
        String content = ""
            // Success badge
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin:0 0 24px;'>"
            + "<tr><td align='center'>"
            + "<div style='display:inline-block;background-color:" + SUCCESS_BG + ";color:" + SUCCESS_BORDER + ";padding:8px 16px;border-radius:20px;font-size:14px;font-weight:600;'>"
            + "✓ Approved"
            + "</div>"
            + "</td></tr>"
            + "</table>"
            
            // Main heading
            + "<h1 style='margin:0 0 16px;font-size:28px;font-weight:700;line-height:36px;color:" + NAVY_BLUE + ";text-align:center;letter-spacing:-0.5px;' class='mobile-heading'>"
            + "Account Approved!"
            + "</h1>"
            
            // Description
            + "<p style='margin:0 0 32px;font-size:16px;line-height:24px;color:" + TEXT_SECONDARY + ";text-align:center;' class='mobile-text'>"
            + "Congratulations! Your NEXORA Bank account has been successfully approved and activated."
            + "</p>"
            
            // Success info box
            + createInfoBox(
                "<h2 style='margin:0 0 12px;font-size:18px;font-weight:600;color:" + TEXT_PRIMARY + ";' class='mobile-subheading'>"
                + "What's Next?"
                + "</h2>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0'>"
                + "<tr><td style='padding:6px 0;'>"
                + "<p style='margin:0;font-size:15px;line-height:22px;color:" + TEXT_SECONDARY + ";'>"
                + "✓ Access your personalized dashboard"
                + "</p></td></tr>"
                + "<tr><td style='padding:6px 0;'>"
                + "<p style='margin:0;font-size:15px;line-height:22px;color:" + TEXT_SECONDARY + ";'>"
                + "✓ Explore all banking services and features"
                + "</p></td></tr>"
                + "<tr><td style='padding:6px 0;'>"
                + "<p style='margin:0;font-size:15px;line-height:22px;color:" + TEXT_SECONDARY + ";'>"
                + "✓ Set up your account preferences"
                + "</p></td></tr>"
                + "<tr><td style='padding:6px 0;'>"
                + "<p style='margin:0;font-size:15px;line-height:22px;color:" + TEXT_SECONDARY + ";'>"
                + "✓ Start managing your finances securely"
                + "</p></td></tr>"
                + "</table>",
                SUCCESS_BG,
                SUCCESS_BORDER
            )
            
            // CTA Button
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0'>"
            + "<tr><td align='center'>"
            + createButton("Access Your Dashboard", "https://nexora.bank/login", TEAL)
            + "</td></tr>"
            + "</table>"
            
            // Footer note
            + "<p style='margin:32px 0 0;font-size:14px;line-height:21px;color:" + TEXT_SECONDARY + ";text-align:center;'>"
            + "Welcome to the NEXORA family. We're here to help you succeed."
            + "</p>";
        
        return getEmailTemplate(content, "Your NEXORA account has been approved! Start banking now.");
    }

    // 3. ACCOUNT DECLINED
    public static String accountDeclinedSubject() {
        return "NEXORA Account Application Update";
    }

    public static String accountDeclinedBody() {
        String content = 
            // Main heading
            "<h1 style='margin:0 0 16px;font-size:28px;font-weight:700;line-height:36px;color:" + NAVY_BLUE + ";letter-spacing:-0.5px;' class='mobile-heading'>"
            + "Account Application Update"
            + "</h1>"
            
            // Description
            + "<p style='margin:0 0 24px;font-size:16px;line-height:24px;color:" + TEXT_SECONDARY + ";' class='mobile-text'>"
            + "Thank you for your interest in NEXORA Bank. After careful review of your application, we regret to inform you that we are unable to approve your account at this time."
            + "</p>"
            
            // Info box
            + createInfoBox(
                "<h2 style='margin:0 0 12px;font-size:18px;font-weight:600;color:" + TEXT_PRIMARY + ";' class='mobile-subheading'>"
                + "What This Means"
                + "</h2>"
                + "<p style='margin:0;font-size:15px;line-height:22px;color:" + TEXT_SECONDARY + ";'>"
                + "This decision was made based on our current account eligibility criteria. We understand this may be disappointing, and we appreciate your understanding."
                + "</p>",
                LIGHT_GRAY,
                BORDER_GRAY
            )
            
            // Help section
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin:24px 0;'>"
            + "<tr><td style='background-color:#FFFBEB;border-radius:8px;padding:20px;border-left:4px solid " + WARNING_BORDER + ";'>"
            + "<p style='margin:0 0 12px;font-size:15px;font-weight:600;color:#92400E;'>"
            + "Need More Information?"
            + "</p>"
            + "<p style='margin:0;font-size:14px;line-height:21px;color:#78350F;'>"
            + "Our support team is available to discuss this decision and answer any questions you may have."
            + "</p>"
            + "</td></tr>"
            + "</table>"
            
            // CTA Button
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0'>"
            + "<tr><td align='center'>"
            + createButton("Contact Support", "mailto:support@nexora.bank", NAVY_BLUE)
            + "</td></tr>"
            + "</table>"
            
            // Footer note
            + "<p style='margin:32px 0 0;font-size:14px;line-height:21px;color:" + TEXT_SECONDARY + ";text-align:center;'>"
            + "We appreciate your interest in NEXORA Bank and wish you the best."
            + "</p>";
        
        return getEmailTemplate(content, "Update regarding your NEXORA account application.");
    }

    // 4. ACCOUNT PENDING
    public static String accountPendingSubject() {
        return "NEXORA Account Under Review";
    }

    public static String accountPendingBody() {
        String content = ""
            // Status badge
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin:0 0 24px;'>"
            + "<tr><td align='center'>"
            + "<div style='display:inline-block;background-color:" + WARNING_BG + ";color:#92400E;padding:8px 16px;border-radius:20px;font-size:14px;font-weight:600;'>"
            + "⏱ Under Review"
            + "</div>"
            + "</td></tr>"
            + "</table>"
            
            // Main heading
            + "<h1 style='margin:0 0 16px;font-size:28px;font-weight:700;line-height:36px;color:" + NAVY_BLUE + ";text-align:center;letter-spacing:-0.5px;' class='mobile-heading'>"
            + "Application Received"
            + "</h1>"
            
            // Description
            + "<p style='margin:0 0 32px;font-size:16px;line-height:24px;color:" + TEXT_SECONDARY + ";text-align:center;' class='mobile-text'>"
            + "Thank you for applying to NEXORA Bank. Your application is currently under review by our team."
            + "</p>"
            
            // Status info box
            + createInfoBox(
                "<h2 style='margin:0 0 12px;font-size:18px;font-weight:600;color:" + TEXT_PRIMARY + ";' class='mobile-subheading'>"
                + "Current Status"
                + "</h2>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0'>"
                + "<tr>"
                + "<td style='padding:8px 0;border-bottom:1px solid " + BORDER_GRAY + ";'>"
                + "<p style='margin:0;font-size:14px;color:" + TEXT_SECONDARY + ";'>"
                + "<strong style='color:" + TEXT_PRIMARY + ";'>Status:</strong> Pending Review"
                + "</p>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style='padding:8px 0;border-bottom:1px solid " + BORDER_GRAY + ";'>"
                + "<p style='margin:0;font-size:14px;color:" + TEXT_SECONDARY + ";'>"
                + "<strong style='color:" + TEXT_PRIMARY + ";'>Expected Timeline:</strong> 1-2 business days"
                + "</p>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style='padding:8px 0;'>"
                + "<p style='margin:0;font-size:14px;color:" + TEXT_SECONDARY + ";'>"
                + "<strong style='color:" + TEXT_PRIMARY + ";'>Next Step:</strong> Email notification upon review completion"
                + "</p>"
                + "</td>"
                + "</tr>"
                + "</table>",
                LIGHT_GRAY,
                BORDER_GRAY
            )
            
            // What happens next
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin:24px 0;'>"
            + "<tr><td style='background-color:#EFF6FF;border-radius:8px;padding:20px;border-left:4px solid " + TEAL + ";'>"
            + "<p style='margin:0 0 8px;font-size:15px;font-weight:600;color:" + TEXT_PRIMARY + ";'>"
            + "What Happens Next?"
            + "</p>"
            + "<p style='margin:0;font-size:14px;line-height:21px;color:#1E40AF;'>"
            + "Our team will carefully review your application. You'll receive an email notification as soon as a decision has been made. No action is required from you at this time."
            + "</p>"
            + "</td></tr>"
            + "</table>"
            
            // Footer note
            + "<p style='margin:32px 0 0;font-size:14px;line-height:21px;color:" + TEXT_SECONDARY + ";text-align:center;'>"
            + "Thank you for your patience. We'll be in touch soon."
            + "</p>";
        
        return getEmailTemplate(content, "Your NEXORA account application is under review.");
    }

    // 5. PASSWORD RESET BY ADMIN
    public static String passwordResetByAdminSubject() {
        return "NEXORA Password Reset - Action Required";
    }

    public static String passwordResetByAdminBody(String newPassword) {
        String content = ""
            // Alert badge
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin:0 0 24px;'>"
            + "<tr><td align='center'>"
            + "<div style='display:inline-block;background-color:" + ERROR_BG + ";color:#991B1B;padding:8px 16px;border-radius:20px;font-size:14px;font-weight:600;'>"
            + "🔐 Security Alert"
            + "</div>"
            + "</td></tr>"
            + "</table>"
            
            // Main heading
            + "<h1 style='margin:0 0 16px;font-size:28px;font-weight:700;line-height:36px;color:" + NAVY_BLUE + ";text-align:center;letter-spacing:-0.5px;' class='mobile-heading'>"
            + "Password Reset"
            + "</h1>"
            
            // Description
            + "<p style='margin:0 0 32px;font-size:16px;line-height:24px;color:" + TEXT_SECONDARY + ";text-align:center;' class='mobile-text'>"
            + "Your account password has been reset by a NEXORA administrator."
            + "</p>"
            
            // Warning box
            + createInfoBox(
                "<p style='margin:0 0 12px;font-size:15px;font-weight:600;color:#991B1B;'>"
                + "⚠️ Important Security Information"
                + "</p>"
                + "<p style='margin:0;font-size:14px;line-height:21px;color:#7F1D1D;'>"
                + "For security reasons, you must change this temporary password immediately after logging in."
                + "</p>",
                ERROR_BG,
                ERROR_BORDER
            )
            
            // Temporary password display
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin:32px 0;'>"
            + "<tr><td align='center' style='background-color:" + LIGHT_GRAY + ";border:2px solid " + BORDER_GRAY + ";border-radius:12px;padding:24px;'>"
            + "<p style='margin:0 0 8px;font-size:12px;font-weight:600;letter-spacing:1px;color:" + TEXT_SECONDARY + ";text-transform:uppercase;'>"
            + "Temporary Password"
            + "</p>"
            + "<p style='margin:0;font-size:20px;font-weight:700;letter-spacing:2px;color:" + NAVY_BLUE + ";font-family:Courier New,Consolas,monospace;word-break:break-all;'>"
            + newPassword
            + "</p>"
            + "</td></tr>"
            + "</table>"
            
            // Instructions
            + createInfoBox(
                "<h2 style='margin:0 0 12px;font-size:18px;font-weight:600;color:" + TEXT_PRIMARY + ";' class='mobile-subheading'>"
                + "Required Actions"
                + "</h2>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0'>"
                + "<tr><td style='padding:6px 0 6px 0;'>"
                + "<p style='margin:0;font-size:15px;line-height:22px;color:" + TEXT_SECONDARY + ";'>"
                + "<strong style='color:" + TEXT_PRIMARY + ";'>1.</strong> Log in using the temporary password above"
                + "</p></td></tr>"
                + "<tr><td style='padding:6px 0;'>"
                + "<p style='margin:0;font-size:15px;line-height:22px;color:" + TEXT_SECONDARY + ";'>"
                + "<strong style='color:" + TEXT_PRIMARY + ";'>2.</strong> Navigate to Account Settings"
                + "</p></td></tr>"
                + "<tr><td style='padding:6px 0;'>"
                + "<p style='margin:0;font-size:15px;line-height:22px;color:" + TEXT_SECONDARY + ";'>"
                + "<strong style='color:" + TEXT_PRIMARY + ";'>3.</strong> Create a new, strong password"
                + "</p></td></tr>"
                + "<tr><td style='padding:6px 0;'>"
                + "<p style='margin:0;font-size:15px;line-height:22px;color:" + TEXT_SECONDARY + ";'>"
                + "<strong style='color:" + TEXT_PRIMARY + ";'>4.</strong> Confirm and save your new password"
                + "</p></td></tr>"
                + "</table>",
                LIGHT_GRAY,
                BORDER_GRAY
            )
            
            // CTA Button
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0'>"
            + "<tr><td align='center'>"
            + createButton("Log In & Change Password", "https://nexora.bank/login", TEAL)
            + "</td></tr>"
            + "</table>"
            
            // Security notice
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin:32px 0 0;'>"
            + "<tr><td style='background-color:#FEF3C7;border-radius:8px;padding:16px;border-left:4px solid " + WARNING_BORDER + ";'>"
            + "<p style='margin:0;font-size:13px;line-height:19px;color:#78350F;'>"
            + "<strong style='color:#92400E;'>Didn't request this change?</strong> If you did not authorize this password reset, please contact our security team immediately at security@nexora.bank"
            + "</p>"
            + "</td></tr>"
            + "</table>";
        
        return getEmailTemplate(content, "Your NEXORA password has been reset. Action required.");
    }

    // 6. ACCOUNT CREATED BY ADMIN
    public static String accountCreatedByAdminSubject() {
        return "Your NEXORA Account Has Been Created";
    }

    public static String accountCreatedByAdminBody(String firstName, String role, String status, String tempPassword) {
        String safeFirstName = firstName == null || firstName.isBlank() ? "User" : firstName;
        String safeRole = role == null ? "ROLE_USER" : role;
        String safeStatus = status == null ? "PENDING" : status;

        String content =
            "<h1 style='margin:0 0 16px;font-size:28px;font-weight:700;line-height:36px;color:" + NAVY_BLUE + ";letter-spacing:-0.5px;' class='mobile-heading'>"
            + "Welcome to NEXORA"
            + "</h1>"
            + "<p style='margin:0 0 24px;font-size:16px;line-height:24px;color:" + TEXT_SECONDARY + ";' class='mobile-text'>"
            + "Hello " + safeFirstName + ", your account was created by a NEXORA administrator."
            + "</p>"
            + createInfoBox(
                "<p style='margin:0 0 10px;font-size:14px;line-height:20px;color:" + TEXT_SECONDARY + ";'>"
                + "<strong style='color:" + TEXT_PRIMARY + ";'>Role:</strong> " + safeRole
                + "</p>"
                + "<p style='margin:0;font-size:14px;line-height:20px;color:" + TEXT_SECONDARY + ";'>"
                + "<strong style='color:" + TEXT_PRIMARY + ";'>Status:</strong> " + safeStatus
                + "</p>",
                LIGHT_GRAY,
                BORDER_GRAY
            )
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin:28px 0;'>"
            + "<tr><td align='center' style='background-color:" + LIGHT_GRAY + ";border:2px solid " + BORDER_GRAY + ";border-radius:12px;padding:20px;'>"
            + "<p style='margin:0 0 8px;font-size:12px;font-weight:600;letter-spacing:1px;color:" + TEXT_SECONDARY + ";text-transform:uppercase;'>"
            + "Temporary Password"
            + "</p>"
            + "<p style='margin:0;font-size:20px;font-weight:700;letter-spacing:2px;color:" + NAVY_BLUE + ";font-family:Courier New,Consolas,monospace;word-break:break-all;'>"
            + tempPassword
            + "</p>"
            + "</td></tr>"
            + "</table>"
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0'>"
            + "<tr><td align='center'>"
            + createButton("Login to NEXORA", "https://nexora.bank/login", TEAL)
            + "</td></tr>"
            + "</table>"
            + "<p style='margin:24px 0 0;font-size:14px;line-height:21px;color:" + TEXT_SECONDARY + ";'>"
            + "For security, please change your password after first login."
            + "</p>";

        return getEmailTemplate(content, "Your NEXORA account has been created by admin.");
    }

    // 7. ACCOUNT BANNED
    public static String accountBannedSubject() {
        return "NEXORA Account Access Suspended";
    }

    public static String accountBannedBody(String firstName, String reason) {
        String safeFirstName = firstName == null || firstName.isBlank() ? "User" : firstName;
        String safeReason = reason == null || reason.isBlank() ? "Policy or security review." : reason;

        String content =
            "<h1 style='margin:0 0 16px;font-size:28px;font-weight:700;line-height:36px;color:" + NAVY_BLUE + ";letter-spacing:-0.5px;' class='mobile-heading'>"
            + "Account Suspended"
            + "</h1>"
            + "<p style='margin:0 0 24px;font-size:16px;line-height:24px;color:" + TEXT_SECONDARY + ";' class='mobile-text'>"
            + "Hello " + safeFirstName + ", your NEXORA account has been suspended by an administrator."
            + "</p>"
            + createInfoBox(
                "<p style='margin:0;font-size:14px;line-height:21px;color:#7F1D1D;'>"
                + "<strong style='color:#991B1B;'>Reason:</strong> " + safeReason
                + "</p>",
                ERROR_BG,
                ERROR_BORDER
            )
            + "<p style='margin:24px 0 0;font-size:14px;line-height:21px;color:" + TEXT_SECONDARY + ";'>"
            + "If you believe this is an error, contact support@nexora.bank."
            + "</p>";

        return getEmailTemplate(content, "Your NEXORA account access has been suspended.");
    }

    // 8. ACCOUNT UNBANNED
    public static String accountUnbannedSubject() {
        return "NEXORA Account Access Restored";
    }

    public static String accountUnbannedBody(String firstName) {
        String safeFirstName = firstName == null || firstName.isBlank() ? "User" : firstName;

        String content =
            "<h1 style='margin:0 0 16px;font-size:28px;font-weight:700;line-height:36px;color:" + NAVY_BLUE + ";letter-spacing:-0.5px;' class='mobile-heading'>"
            + "Access Restored"
            + "</h1>"
            + "<p style='margin:0 0 24px;font-size:16px;line-height:24px;color:" + TEXT_SECONDARY + ";' class='mobile-text'>"
            + "Hello " + safeFirstName + ", your NEXORA account has been reactivated. You can now log in again."
            + "</p>"
            + createInfoBox(
                "<p style='margin:0;font-size:14px;line-height:21px;color:#065F46;'>"
                + "Your account status is now active."
                + "</p>",
                SUCCESS_BG,
                SUCCESS_BORDER
            )
            + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0'>"
            + "<tr><td align='center'>"
            + createButton("Login to NEXORA", "https://nexora.bank/login", TEAL)
            + "</td></tr>"
            + "</table>";

        return getEmailTemplate(content, "Your NEXORA account access has been restored.");
    }
}
