package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;

public class GoogleOnboardingController {

    @FXML private VBox studentCard;
    @FXML private VBox teacherCard;
    @FXML private Button confirmButton;

    private String selectedRole = null;
    private Utilisateur tempUser;
    private final UtilisateurService userService = new UtilisateurService();

    public void setTempUser(Utilisateur user) {
        this.tempUser = user;
    }

    @FXML
    private void selectStudent() {
        selectedRole = "ROLE_STUDENT";
        studentCard.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #6c5ce7; -fx-border-width: 3; -fx-border-radius: 20; -fx-padding: 30;");
        teacherCard.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #dfe6e9; -fx-border-radius: 20; -fx-padding: 30;");
        confirmButton.setDisable(false);
    }

    @FXML
    private void selectTeacher() {
        selectedRole = "ROLE_TEACHER";
        teacherCard.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #6c5ce7; -fx-border-width: 3; -fx-border-radius: 20; -fx-padding: 30;");
        studentCard.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #dfe6e9; -fx-border-radius: 20; -fx-padding: 30;");
        confirmButton.setDisable(false);
    }

    @FXML
    private void handleConfirm() {
        if (selectedRole != null && tempUser != null) {
            tempUser.setRole(selectedRole);
            tempUser.setDiscr(selectedRole.equals("ROLE_STUDENT") ? "etudiant" : "enseignant");
            tempUser.setDateInscription(LocalDateTime.now());
            tempUser.setStatut("actif");
            
            // On ne fait PAS d'add ici, on le fera dans ProfileController
            navigateToProfile(tempUser);
        }
    }

    private void navigateToProfile(Utilisateur user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auth/profile.fxml"));
            Parent root = loader.load();
            
            ProfileController controller = loader.getController();
            controller.setTempUser(user); // Nouvelle méthode à créer
            
            Stage stage = (Stage) confirmButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Compléter mon Profil - StudySprint");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
