package com.example.studysprint.modules.home.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.utils.AppNavigator;
import com.example.studysprint.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class HomeController {
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label fullNameLabel;
    @FXML
    private Label roleLabel;

    @FXML
    public void initialize() {
        Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || AppNavigator.isAdmin(currentUser)) {
            Platform.runLater(() -> {
                Stage stage = (Stage) welcomeLabel.getScene().getWindow();
                if (currentUser == null) {
                    AppNavigator.switchTo(stage, AppNavigator.LOGIN_FXML, AppNavigator.LOGIN_TITLE, getClass());
                } else {
                    AppNavigator.openDefaultForUser(stage, currentUser, getClass());
                }
            });
            return;
        }

        welcomeLabel.setText("Bonjour, " + currentUser.getPrenom() + " !");
        fullNameLabel.setText(currentUser.getFullName());
        roleLabel.setText(formatRole(currentUser));
    }

    @FXML
    private void openGroups() {
        openScene("/fxml/groupes/GroupListView.fxml", "Groupes - StudySprint");
    }

    @FXML
    private void openProfile() {
        openScene("/fxml/auth/profile.fxml", "Mon profil - StudySprint");
    }

    @FXML
    private void openChangePassword() {
        openProfile();
    }

    @FXML
    private void logout() {
        SessionManager.getInstance().logout();
        openScene("/fxml/auth/login.fxml", "Connexion - StudySprint");
    }

    private void openScene(String fxmlPath, String title) {
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        if (!AppNavigator.switchTo(stage, fxmlPath, title, getClass())) {
            throw new RuntimeException("Impossible d'ouvrir la vue : " + fxmlPath);
        }
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