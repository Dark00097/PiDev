package com.nexora.bank.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.Properties;

public class TwilioSmsService {

    // Keep defaults empty: credentials must come from env/JVM properties/config file.
    private static final String LEGACY_ACCOUNT_SID = "";
    private static final String LEGACY_AUTH_TOKEN = "";
    private static final String LEGACY_FROM_NUMBER = "";
    private static final String LEGACY_TEST_NUMBER = "";

    private static final Properties FILE_CONFIG = loadConfig();

    private static final String ACCOUNT_SID = resolve(
        "NEXORA_TWILIO_ACCOUNT_SID", "twilio.account_sid", LEGACY_ACCOUNT_SID);
    private static final String AUTH_TOKEN = resolve(
        "NEXORA_TWILIO_AUTH_TOKEN", "twilio.auth_token", LEGACY_AUTH_TOKEN);
    private static final String FROM_NUMBER = resolve(
        "NEXORA_TWILIO_FROM_NUMBER", "twilio.from_number", LEGACY_FROM_NUMBER);
    private static final String TEST_NUMBER = resolve(
        "NEXORA_TWILIO_TEST_NUMBER", "twilio.test_number", LEGACY_TEST_NUMBER);
    private static final boolean FORCE_TEST_MODE = Boolean.parseBoolean(resolve(
        "NEXORA_TWILIO_FORCE_TEST", "twilio.force_test", "true"));

    private static final boolean SMS_CONFIGURED =
        !ACCOUNT_SID.isBlank() && !AUTH_TOKEN.isBlank() && !FROM_NUMBER.isBlank();

    public boolean sendSms(String toNumber, String message) {
        if (!SMS_CONFIGURED) {
            System.err.println("[TwilioSMS] Missing Twilio configuration, SMS skipped.");
            return false;
        }

        String destination;
        if (FORCE_TEST_MODE) {
            destination = normalizeNumber(TEST_NUMBER);
        } else {
            destination = normalizeNumber(toNumber);
            if (destination.isBlank()) {
                destination = normalizeNumber(TEST_NUMBER);
            }
        }
        if (destination.isBlank()) {
            System.err.println("[TwilioSMS] No destination phone number available.");
            return false;
        }
        if (FROM_NUMBER.equals(destination)) {
            System.err.println("[TwilioSMS] From and To numbers are identical, SMS blocked.");
            return false;
        }

        String apiUrl = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json";

        try {
            String body =
                "To=" + URLEncoder.encode(destination, StandardCharsets.UTF_8) +
                "&From=" + URLEncoder.encode(FROM_NUMBER, StandardCharsets.UTF_8) +
                "&Body=" + URLEncoder.encode(message == null ? "" : message, StandardCharsets.UTF_8);

            String credentials = ACCOUNT_SID + ":" + AUTH_TOKEN;
            String encodedAuth = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int status = conn.getResponseCode();
            if (status == 200 || status == 201) {
                System.out.println("[TwilioSMS] SMS sent successfully to " + destination + " (forceTest=" + FORCE_TEST_MODE + ")");
                return true;
            }

            try (InputStream err = conn.getErrorStream()) {
                if (err != null) {
                    String errorBody = new String(err.readAllBytes(), StandardCharsets.UTF_8);
                    System.err.println("[TwilioSMS] API error (" + status + "): " + errorBody);
                }
            }
            return false;
        } catch (Exception ex) {
            System.err.println("[TwilioSMS] sendSms failed: " + ex.getMessage());
            return false;
        }
    }

    public static String buildAccountValidationMessage(
        String numeroCompte,
        String typeCompte,
        double solde,
        String dateOuverture,
        double plafondRetrait,
        double plafondVirement,
        String nomClient
    ) {
        String nom = safe(nomClient, "Client");
        return "NEXORA BANK - Account activated\n" +
            "Hello " + nom + ",\n\n" +
            "Your bank account request has been approved.\n\n" +
            "Account number: " + safe(numeroCompte, "-") + "\n" +
            "Type: " + safe(typeCompte, "-") + "\n" +
            "Initial balance: " + formatAmount(solde) + " DT\n" +
            "Opening date: " + safe(dateOuverture, "-") + "\n" +
            "Withdrawal limit: " + formatAmount(plafondRetrait) + " DT\n" +
            "Transfer limit: " + formatAmount(plafondVirement) + " DT\n\n" +
            "Thank you for using NEXORA BANK.";
    }

    public static String buildAccountRefusalMessage(
        String numeroCompte,
        String typeCompte,
        String nomClient
    ) {
        String nom = safe(nomClient, "Client");
        return "NEXORA BANK - Request declined\n" +
            "Hello " + nom + ",\n\n" +
            "Your bank account request has been declined.\n\n" +
            "Account number: " + safe(numeroCompte, "-") + "\n" +
            "Type: " + safe(typeCompte, "-") + "\n\n" +
            "Please contact support for more details.";
    }

    private static String env(String key) {
        String value = System.getenv(key);
        return value == null ? "" : value.trim();
    }

    private static String prop(String key) {
        String value = System.getProperty(key);
        return value == null ? "" : value.trim();
    }

    private static String fileValue(String key) {
        if (FILE_CONFIG == null) {
            return "";
        }
        String value = FILE_CONFIG.getProperty(key);
        return value == null ? "" : value.trim();
    }

    private static String resolve(String envKey, String fileKey, String fallback) {
        String value = env(envKey);
        if (!value.isBlank()) {
            return value;
        }

        value = prop(envKey);
        if (!value.isBlank()) {
            return value;
        }

        value = fileValue(fileKey);
        if (!value.isBlank()) {
            return value;
        }

        return fallback == null ? "" : fallback.trim();
    }

    private static Properties loadConfig() {
        Path configPath = Path.of("config", "twilio.properties");
        if (!Files.exists(configPath)) {
            return null;
        }

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            properties.load(in);
            return properties;
        } catch (Exception ex) {
            System.err.println("[TwilioSMS] Unable to load config/twilio.properties: " + ex.getMessage());
            return null;
        }
    }

    private static String normalizeNumber(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private static String formatAmount(double amount) {
        return String.format(Locale.US, "%,.2f", amount);
    }
}
