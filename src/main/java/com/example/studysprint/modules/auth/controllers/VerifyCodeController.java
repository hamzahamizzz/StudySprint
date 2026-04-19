package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.AppNavigator;
import com.example.studysprint.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDateTime;

public class VerifyCodeController {

    @FXML private TextField codeField;
    @FXML private Label codeError, instructionLabel;
    @FXML private Label sidebarFullNameLabel, sidebarRoleLabel;

    private final UtilisateurService userService = new UtilisateurService();

    @FXML
    public void initialize() {
        populateSidebarUser();
        if (ForgotPasswordController.targetEmail != null) {
            instructionLabel.setText("Entrez le code à 6 chiffres envoyé à " + ForgotPasswordController.targetEmail);
        }
    }

    private void populateSidebarUser() {
        Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            sidebarFullNameLabel.setText("Utilisateur");
            sidebarRoleLabel.setText("Invite");
            return;
        }
        sidebarFullNameLabel.setText(currentUser.getFullName());
        sidebarRoleLabel.setText(formatRole(currentUser));
    }

    @FXML
    private void onGoHome() {
        Stage stage = (Stage) codeField.getScene().getWindow();
        if (!AppNavigator.openDefaultForCurrentSession(stage, getClass())) {
            AppNavigator.switchTo(stage, AppNavigator.LOGIN_FXML, AppNavigator.LOGIN_TITLE, getClass());
        }
    }

    @FXML
    private void onGoGroups() {
        Stage stage = (Stage) codeField.getScene().getWindow();
        if (SessionManager.getInstance().getCurrentUser() == null) {
            AppNavigator.switchTo(stage, AppNavigator.LOGIN_FXML, AppNavigator.LOGIN_TITLE, getClass());
            return;
        }
        AppNavigator.switchTo(stage, "/fxml/groupes/GroupListView.fxml", "Groupes - StudySprint", getClass());
    }

    @FXML
    private void onOpenProfile() {
        Stage stage = (Stage) codeField.getScene().getWindow();
        if (SessionManager.getInstance().getCurrentUser() == null) {
            AppNavigator.switchTo(stage, AppNavigator.LOGIN_FXML, AppNavigator.LOGIN_TITLE, getClass());
            return;
        }
        AppNavigator.switchTo(stage, "/fxml/auth/profile.fxml", "Mon Profil - StudySprint", getClass());
    }

    @FXML
    private void onLogout() {
        SessionManager.getInstance().logout();
        Stage stage = (Stage) codeField.getScene().getWindow();
        AppNavigator.switchTo(stage, AppNavigator.LOGIN_FXML, AppNavigator.LOGIN_TITLE, getClass());
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
        Stage stage = (Stage) codeField.getScene().getWindow();
        AppNavigator.switchTo(stage, fxmlPath, title, getClass());
    }

    private String formatRole(Utilisateur user) {
        if (user == null || user.getRole() == null) {
            return "Utilisateur";
        }
        return switch (user.getRole().toUpperCase()) {
            case "ROLE_STUDENT" -> "Etudiant";
            case "ROLE_PROFESSOR" -> "Professeur";
            case "ROLE_ADMIN" -> "Administrateur";
            default -> "Utilisateur";
        };
    }
}
