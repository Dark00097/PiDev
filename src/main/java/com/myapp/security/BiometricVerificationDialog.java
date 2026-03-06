package com.myapp.security;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.util.Optional;

public final class BiometricVerificationDialog {

    private BiometricVerificationDialog() {
    }

    public static AuthResult promptAndVerify(String verificationMessage) {
        ButtonType verifyNow = new ButtonType("Verify now", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Biometric verification");
        confirmation.setHeaderText("Face ou empreinte digitale");
        confirmation.setContentText("Confirm your identity to continue.");
        confirmation.getButtonTypes().setAll(verifyNow, cancel);

        Optional<ButtonType> choice = confirmation.showAndWait();
        if (choice.isEmpty() || choice.get() != verifyNow) {
            return AuthResult.FAILED;
        }

        return WindowsBiometricAuth.verify(verificationMessage);
    }

    public static void showResultDialog(AuthResult result) {
        switch (result) {
            case VERIFIED -> showInfo("Biometric verification", "Verified.");
            case FAILED -> showError("Biometric verification", "Verification failed or cancelled.");
            case NOT_AVAILABLE -> showNotAvailableDialog();
            case ERROR -> showError(
                "Biometric verification",
                "Biometric verification helper is not available.\n\n"
                    + "Build command:\n"
                    + WindowsBiometricAuth.getBuildHint()
                    + "\n\nDebug:\n"
                    + WindowsBiometricAuth.getLastDebug()
            );
        }
    }

    public static void openDeviceBiometricSettings() throws IOException {
        new ProcessBuilder("cmd", "/c", "start", "ms-settings:signinoptions").start();
    }

    private static void showNotAvailableDialog() {
        ButtonType openSettings = new ButtonType("Open biometric settings", ButtonBar.ButtonData.OK_DONE);
        ButtonType close = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Biometric verification");
        alert.setHeaderText("Face ou empreinte digitale not configured");
        alert.setContentText("Biometrics aren't set up on this device.");
        alert.getButtonTypes().setAll(openSettings, close);

        Optional<ButtonType> selection = alert.showAndWait();
        if (selection.isPresent() && selection.get() == openSettings) {
            try {
                openDeviceBiometricSettings();
            } catch (IOException e) {
                showError("Biometric verification", "Failed to open device biometric settings.");
            }
        }
    }

    private static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
