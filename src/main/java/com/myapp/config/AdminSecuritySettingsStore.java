package com.myapp.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminSecuritySettingsStore {
    private static final Path SETTINGS_PATH = Paths.get("config", "admin_security_settings.json");
    private static final Pattern REQ_LOGIN_PATTERN =
        Pattern.compile("\"requireBiometricOnAdminLogin\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REQ_SENSITIVE_PATTERN =
        Pattern.compile("\"requireBiometricOnSensitiveActions\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern OTP_PATTERN =
        Pattern.compile("\"enableEmailOtp\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);

    public AdminSecuritySettings load() {
        ensureDirectory();

        if (Files.notExists(SETTINGS_PATH)) {
            AdminSecuritySettings defaults = new AdminSecuritySettings();
            save(defaults);
            return defaults;
        }

        try {
            String json = Files.readString(SETTINGS_PATH, StandardCharsets.UTF_8);
            AdminSecuritySettings settings = parseSettings(json);
            return settings == null ? new AdminSecuritySettings() : settings;
        } catch (Exception ex) {
            AdminSecuritySettings fallback = new AdminSecuritySettings();
            save(fallback);
            return fallback;
        }
    }

    public void save(AdminSecuritySettings settings) {
        ensureDirectory();
        AdminSecuritySettings payload = settings == null ? new AdminSecuritySettings() : settings;

        String json = toJson(payload);
        try {
            Files.writeString(SETTINGS_PATH, json, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save admin security settings.", ex);
        }
    }

    public Path getSettingsFilePath() {
        return SETTINGS_PATH.toAbsolutePath().normalize();
    }

    private void ensureDirectory() {
        try {
            Path parent = SETTINGS_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to initialize settings directory.", ex);
        }
    }

    private AdminSecuritySettings parseSettings(String json) {
        if (json == null || json.isBlank()) {
            return new AdminSecuritySettings();
        }

        AdminSecuritySettings settings = new AdminSecuritySettings();
        settings.setRequireBiometricOnAdminLogin(readBoolean(REQ_LOGIN_PATTERN, json, false));
        settings.setRequireBiometricOnSensitiveActions(readBoolean(REQ_SENSITIVE_PATTERN, json, false));
        settings.setEnableEmailOtp(readBoolean(OTP_PATTERN, json, false));
        return settings;
    }

    private boolean readBoolean(Pattern pattern, String json, boolean defaultValue) {
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(matcher.group(1));
    }

    private String toJson(AdminSecuritySettings settings) {
        return "{\n"
            + "  \"requireBiometricOnAdminLogin\": " + settings.isRequireBiometricOnAdminLogin() + ",\n"
            + "  \"requireBiometricOnSensitiveActions\": " + settings.isRequireBiometricOnSensitiveActions() + ",\n"
            + "  \"enableEmailOtp\": " + settings.isEnableEmailOtp() + "\n"
            + "}\n";
    }
}
