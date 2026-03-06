package com.nexora.bank.controllers;

import com.nexora.bank.SceneRouter;
import com.nexora.bank.Service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class ForgotPasswordController {

    @FXML private TextField txtEmail;
    @FXML private TextField txtOtp;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblMessage;
    @FXML private Label lblStepEmail;
    @FXML private Label lblStepOtp;
    @FXML private Label lblStepPassword;
    @FXML private VBox stepEmailBox;
    @FXML private VBox stepOtpBox;
    @FXML private VBox stepPasswordBox;

    private final UserService userService = new UserService();
    private String resetEmail;
    private boolean otpVerified;

    @FXML
    private void initialize() {
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
        resetEmail = "";
        otpVerified = false;
        showStep(1);
    }

    @FXML
    private void handleSendOtp() {
        String email = value(txtEmail);
        if (email.isBlank()) {
            showError("Please enter your email first.");
            return;
        }

        try {
            userService.sendPasswordResetOtpByEmail(email);
            resetEmail = email;
            otpVerified = false;
            txtOtp.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();
            showStep(2);
            showMessage("OTP sent to " + email + ". It expires in 5 minutes.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to send OTP.");
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleVerifyOtp() {
        if (resetEmail.isBlank()) {
            showError("Start with your email first.");
            showStep(1);
            return;
        }

        String otp = value(txtOtp);
        if (otp.isBlank()) {
            showError("Please enter OTP.");
            return;
        }

        try {
            userService.verifyPasswordResetOtpByEmail(resetEmail, otp);
            otpVerified = true;
            showStep(3);
            showMessage("OTP verified. You can now enter your new password.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("OTP verification failed.");
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleResetPassword() {
        if (resetEmail.isBlank()) {
            showError("Start with your email first.");
            showStep(1);
            return;
        }
        if (!otpVerified) {
            showError("Please verify OTP before setting a new password.");
            showStep(2);
            return;
        }

        String newPassword = txtNewPassword.getText() == null ? "" : txtNewPassword.getText().trim();
        String confirmPassword = txtConfirmPassword.getText() == null ? "" : txtConfirmPassword.getText().trim();

        if (newPassword.isBlank() || confirmPassword.isBlank()) {
            showError("Please fill password fields.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        if (newPassword.length() < 8) {
            showError("New password must be at least 8 characters.");
            return;
        }

        try {
            boolean updated = userService.resetPasswordByVerifiedEmail(resetEmail, newPassword);
            if (!updated) {
                showError("Password reset failed.");
                return;
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("Password updated");
            alert.setContentText("Your password was updated successfully. You can now login.");
            alert.showAndWait();

            SceneRouter.show("/fxml/Login.fxml", "NEXORA BANK - Login", 1200, 760, 980, 680);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Password reset failed.");
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleResendOtp() {
        if (resetEmail.isBlank()) {
            showError("Enter your email first.");
            showStep(1);
            return;
        }
        try {
            userService.sendPasswordResetOtpByEmail(resetEmail);
            otpVerified = false;
            txtOtp.clear();
            showStep(2);
            showMessage("A new OTP was sent to " + resetEmail + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to resend OTP.");
            ex.printStackTrace();
        }
    }

    @FXML
    private void openLogin() {
        SceneRouter.show("/fxml/Login.fxml", "NEXORA BANK - Login", 1200, 760, 980, 680);
    }

    @FXML
    private void openHome() {
        SceneRouter.show("/fxml/Home.fxml", "NEXORA BANK - Welcome", 1200, 760, 980, 680);
    }

    private String value(TextField field) {
        if (field == null || field.getText() == null) {
            return "";
        }
        return field.getText().trim();
    }

    private void showStep(int stepNumber) {
        setStepVisibility(stepEmailBox, stepNumber == 1);
        setStepVisibility(stepOtpBox, stepNumber == 2);
        setStepVisibility(stepPasswordBox, stepNumber == 3);

        setStepIndicator(lblStepEmail, stepNumber >= 1, stepNumber == 1);
        setStepIndicator(lblStepOtp, stepNumber >= 2, stepNumber == 2);
        setStepIndicator(lblStepPassword, stepNumber >= 3, stepNumber == 3);
    }

    private void setStepVisibility(VBox box, boolean visible) {
        if (box == null) {
            return;
        }
        box.setVisible(visible);
        box.setManaged(visible);
    }

    private void setStepIndicator(Label label, boolean done, boolean current) {
        if (label == null) {
            return;
        }
        if (current) {
            label.setStyle("-fx-background-color: #00b4a0; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 999;");
            return;
        }
        if (done) {
            label.setStyle("-fx-background-color: #14c7b3; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 999;");
            return;
        }
        label.setStyle("-fx-background-color: #edf3f9; -fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 999;");
    }

    private void showMessage(String message) {
        lblMessage.setText(message);
        lblMessage.setStyle("-fx-text-fill: #009f8d; -fx-font-size: 12px; -fx-font-weight: 600;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }

    private void showError(String message) {
        lblMessage.setText(message);
        lblMessage.setStyle("-fx-text-fill: #c93535; -fx-font-size: 12px; -fx-font-weight: 600;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }
}
