package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.MailerService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label emailError;

    private final UtilisateurService userService = new UtilisateurService();
    
    // Shared state for the reset flow
    public static String targetEmail;

    @FXML
    private void handleRequestCode() {
        String email = emailField.getText().trim();
        emailError.setVisible(false);

        if (email.isEmpty()) {
            emailError.setText("Veuillez entrer votre adresse email.");
            emailError.setVisible(true);
            return;
        }

        Utilisateur user = userService.findByEmail(email);
        if (user == null) {
            // Security: Same message even if user not found, typical for reset flows
            // but for UX in development, we'll inform
            emailError.setText("Aucun compte n'est associé à cet email.");
            emailError.setVisible(true);
            return;
        }

        // Generate 6-digit code
        String code = String.format("%06d", new Random().nextInt(1000000));
        user.setResetToken(code);
        user.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(15));
        
        userService.updateResetToken(user);
        targetEmail = email;

        // Send Email asynchronously
        new Thread(() -> {
            try {
                MailerService.sendVerificationCode(email, code);
                Platform.runLater(() -> switchScene("/fxml/auth/verify-code.fxml", "Vérification - StudySprint"));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    emailError.setText("Erreur lors de l'envoi de l'email. Vérifiez votre connexion.");
                    emailError.setVisible(true);
                    e.printStackTrace();
                });
            }
        }).start();
    }

    @FXML
    private void handleBackToLogin() {
        switchScene("/fxml/auth/login.fxml", "Connexion - StudySprint");
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
