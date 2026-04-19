package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.AppNavigator;
import com.example.studysprint.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import java.util.regex.Pattern;

public class ResetPasswordController {

    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private Label passwordError, confirmPasswordError;
    @FXML private Label sidebarFullNameLabel, sidebarRoleLabel;

    private final UtilisateurService userService = new UtilisateurService();

    @FXML
    public void initialize() {
        populateSidebarUser();
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
        Stage stage = (Stage) passwordField.getScene().getWindow();
        if (!AppNavigator.openDefaultForCurrentSession(stage, getClass())) {
            AppNavigator.switchTo(stage, AppNavigator.LOGIN_FXML, AppNavigator.LOGIN_TITLE, getClass());
        }
    }

    @FXML
    private void onGoGroups() {
        Stage stage = (Stage) passwordField.getScene().getWindow();
        if (SessionManager.getInstance().getCurrentUser() == null) {
            AppNavigator.switchTo(stage, AppNavigator.LOGIN_FXML, AppNavigator.LOGIN_TITLE, getClass());
            return;
        }
        AppNavigator.switchTo(stage, "/fxml/groupes/GroupListView.fxml", "Groupes - StudySprint", getClass());
    }

    @FXML
    private void onOpenProfile() {
        Stage stage = (Stage) passwordField.getScene().getWindow();
        if (SessionManager.getInstance().getCurrentUser() == null) {
            AppNavigator.switchTo(stage, AppNavigator.LOGIN_FXML, AppNavigator.LOGIN_TITLE, getClass());
            return;
        }
        AppNavigator.switchTo(stage, "/fxml/auth/profile.fxml", "Mon Profil - StudySprint", getClass());
    }

    @FXML
    private void onLogout() {
        SessionManager.getInstance().logout();
        Stage stage = (Stage) passwordField.getScene().getWindow();
        AppNavigator.switchTo(stage, AppNavigator.LOGIN_FXML, AppNavigator.LOGIN_TITLE, getClass());
    }

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
        Stage stage = (Stage) passwordField.getScene().getWindow();
        AppNavigator.switchTo(stage, fxmlPath, title, getClass());
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
