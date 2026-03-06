package com.nexora.bank.controllers;

import com.nexora.bank.SceneRouter;
import com.nexora.bank.Service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SignupController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtOtp;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblMessage;

    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        lblMessage.setVisible(false);
        lblMessage.setManaged(false);
    }

    @FXML
    private void handleSendOtp() {
        String email = value(txtEmail);
        if (email.isEmpty()) {
            showError("Please enter your email first.");
            return;
        }

        try {
            userService.sendSignupOtp(email);
            showMessage("OTP sent to " + email + ". It expires in 5 minutes.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Failed to send OTP email.");
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleSignup() {
        String fullName = value(txtFullName);
        String email = value(txtEmail);
        String phone = value(txtPhone);
        String otpCode = value(txtOtp);
        String password = txtPassword.getText() == null ? "" : txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText() == null ? "" : txtConfirmPassword.getText();

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || otpCode.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please complete all fields.");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            showError("Please enter a valid email address.");
            return;
        }

        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        if (!userService.verifySignupOtp(email, otpCode)) {
            showError("Invalid or expired OTP.");
            return;
        }

        String[] nameParts = splitName(fullName);
        String nom = nameParts[0];
        String prenom = nameParts[1];

        try {
            userService.registerUser(nom, prenom, email, phone, password);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("Account created");
            alert.setContentText("Your account was created and is pending admin approval. You can login after approval.");
            alert.show();

            SceneRouter.show("/fxml/Login.fxml", "NEXORA BANK - Login", 1200, 760, 980, 680);
        } catch (IllegalStateException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Signup failed. Please try again.");
            ex.printStackTrace();
        }

    }

    @FXML
    private void openHome() {
        SceneRouter.show("/fxml/Home.fxml", "NEXORA BANK - Welcome", 1200, 760, 980, 680);
    }

    @FXML
    private void openLogin() {
        SceneRouter.show("/fxml/Login.fxml", "NEXORA BANK - Login", 1200, 760, 980, 680);
    }

    private String value(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String[] splitName(String fullName) {
        String normalized = fullName.trim().replaceAll("\\s+", " ");
        int firstSpace = normalized.indexOf(' ');
        if (firstSpace <= 0) {
            return new String[]{normalized, "-"};
        }
        return new String[]{
            normalized.substring(0, firstSpace),
            normalized.substring(firstSpace + 1)
        };
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
