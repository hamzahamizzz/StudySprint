package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;

public class VerifyCodeController {

    @FXML private TextField codeField;
    @FXML private Label codeError, instructionLabel;

    private final UtilisateurService userService = new UtilisateurService();

    @FXML
    public void initialize() {
        if (ForgotPasswordController.targetEmail != null) {
            instructionLabel.setText("Entrez le code à 6 chiffres envoyé à " + ForgotPasswordController.targetEmail);
        }
    }

    @FXML
    private void handleVerifyCode() {
        String code = codeField.getText().trim();
        codeError.setVisible(false);

        if (code.length() != 6) {
            codeError.setText("Le code doit contenir 6 chiffres.");
            codeError.setVisible(true);
            return;
        }

        Utilisateur user = userService.findByResetToken(code);
        
        if (user == null || !user.getEmail().equalsIgnoreCase(ForgotPasswordController.targetEmail)) {
            codeError.setText("Code de vérification invalide.");
            codeError.setVisible(true);
            return;
        }

        if (user.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            codeError.setText("Ce code a expiré. Veuillez en demander un nouveau.");
            codeError.setVisible(true);
            return;
        }

        // Code valid!
        switchScene("/fxml/auth/reset-password.fxml", "Nouveau mot de passe - StudySprint");
    }

    @FXML
    private void handleResendCode() {
        // Simple redirect to resend
        handleBackToEmail();
    }

    @FXML
    private void handleBackToEmail() {
        switchScene("/fxml/auth/forgot-password.fxml", "Mot de passe oublié - StudySprint");
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
