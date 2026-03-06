package com.myapp.config;

public class AdminSecuritySettings {
    private boolean requireBiometricOnAdminLogin;
    private boolean requireBiometricOnSensitiveActions;
    private boolean enableEmailOtp;

    public AdminSecuritySettings() {
        this.requireBiometricOnAdminLogin = false;
        this.requireBiometricOnSensitiveActions = false;
        this.enableEmailOtp = false;
    }

    public boolean isRequireBiometricOnAdminLogin() {
        return requireBiometricOnAdminLogin;
    }

    public void setRequireBiometricOnAdminLogin(boolean requireBiometricOnAdminLogin) {
        this.requireBiometricOnAdminLogin = requireBiometricOnAdminLogin;
    }

    public boolean isRequireBiometricOnSensitiveActions() {
        return requireBiometricOnSensitiveActions;
    }

    public void setRequireBiometricOnSensitiveActions(boolean requireBiometricOnSensitiveActions) {
        this.requireBiometricOnSensitiveActions = requireBiometricOnSensitiveActions;
    }

    public boolean isEnableEmailOtp() {
        return enableEmailOtp;
    }

    public void setEnableEmailOtp(boolean enableEmailOtp) {
        this.enableEmailOtp = enableEmailOtp;
    }
}
