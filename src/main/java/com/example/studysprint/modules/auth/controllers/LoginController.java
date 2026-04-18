package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label emailError, passwordError;

    private final UtilisateurService userService = new UtilisateurService();

    @FXML
    private void handleLogin() {
        if (validate()) {
            Utilisateur user = userService.authenticate(emailField.getText().trim(), passwordField.getText().trim());
            if (user != null) {
                // Robust status check (handle null or older accounts)
                String status = user.getStatut();
                if (status == null || status.isEmpty() || "actif".equalsIgnoreCase(status)) {
                    SessionManager.getInstance().setCurrentUser(user);
                    System.out.println("Login successful for: " + user.getEmail());
                    loadMainApp(user);
                } else if ("desactif".equalsIgnoreCase(status) || "inactif".equalsIgnoreCase(status)) {
                    // Redirect to account deactivated page
                    SessionManager.getInstance().setCurrentUser(user);
                    switchScene("/fxml/auth/account-deactivated.fxml", "Compte Désactivé - StudySprint");
                } else {
                    passwordError.setText("Compte " + status + ". Contactez un administrateur.");
                    passwordError.setVisible(true);
                }
            } else {
                passwordError.setText("Identifiants incorrects ou compte inexistant.");
                passwordError.setVisible(true);
            }
        }
    }

    @FXML
    private void handleGoToRegister() {
        switchScene("/fxml/auth/register.fxml", "Inscription - StudySprint");
    }

    @FXML
    private void handleForgotPassword() {
        switchScene("/fxml/auth/forgot-password.fxml", "Mot de passe oublié - StudySprint");
    }

    @FXML
    private void handleFaceLogin() {
        switchScene("/fxml/auth/face-login.fxml", "Connexion par Reconnaissance Faciale - StudySprint");
    }

    private boolean validate() {
        boolean isValid = true;
        emailError.setVisible(false);
        passwordError.setVisible(false);

        if (emailField.getText().isEmpty()) {
            emailError.setText("L'email est requis.");
            emailError.setVisible(true);
            isValid = false;
        }

        if (passwordField.getText().isEmpty()) {
            passwordError.setText("Le mot de passe est requis.");
            passwordError.setVisible(true);
            isValid = false;
        }

        return isValid;
    }

    private void loadMainApp(Utilisateur user) {
        if ("ROLE_ADMIN".equals(user.getRole())) {
            switchScene("/fxml/utilisateurs/main-admin-layout.fxml", "Tableau de Bord - StudySprint");
        } else {
            switchScene("/fxml/auth/profile.fxml", "Mon Profil - StudySprint");
        }
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
