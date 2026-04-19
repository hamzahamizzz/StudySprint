package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.regex.Pattern;

public class ResetPasswordController {

    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private Label passwordError, confirmPasswordError;

    private final UtilisateurService userService = new UtilisateurService();

    @FXML
    private void handleResetPassword() {
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        boolean isValid = true;

        passwordError.setVisible(false);
        confirmPasswordError.setVisible(false);

        // Standard robust password regex
        String passRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";
        if (!Pattern.matches(passRegex, password)) {
            passwordError.setText("Minimum 8 caractères, une majuscule et un chiffre.");
            passwordError.setVisible(true);
            isValid = false;
        } else if (!password.equals(confirm)) {
            confirmPasswordError.setText("Les mots de passe ne correspondent pas.");
            confirmPasswordError.setVisible(true);
            isValid = false;
        }

        if (!isValid) return;

        Utilisateur user = userService.findByEmail(ForgotPasswordController.targetEmail);
        if (user != null) {
            userService.resetPassword(user, password);
            showSuccess("Succès", "Votre mot de passe a été modifié. Vous pouvez maintenant vous connecter.");
            switchScene("/fxml/auth/login.fxml", "Connexion - StudySprint");
        } else {
            showError("Erreur", "Session expirée. Veuillez recommencer la procédure.");
            switchScene("/fxml/auth/forgot-password.fxml", "Mot de passe oublié - StudySprint");
        }
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) passwordField.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
