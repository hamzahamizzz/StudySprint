package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.ExternalApiService;
import com.example.studysprint.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private TextField nomField, prenomField, emailField, ageField, niveauField, specialiteField, niveauEnseignementField, experienceField, telephoneField;
    @FXML private ComboBox<String> sexeCombo, etablissementCombo;
    @FXML private ComboBox<ExternalApiService.Country> paysCombo;
    @FXML private Label roleLabel, fullNameLabel;
    @FXML private VBox studentFields, professorFields;

    private final UtilisateurService userService = new UtilisateurService();
    private Utilisateur currentUser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            handleLogout();
            return;
        }

        setupForm();
        loadUserData();
    }

    private void setupForm() {
        sexeCombo.getItems().addAll("M", "F");
        etablissementCombo.setEditable(true);

        // Load Countries
        ExternalApiService.fetchCountries().thenAccept(countries -> {
            Platform.runLater(() -> {
                paysCombo.getItems().addAll(countries);
                // Set current country
                if (currentUser.getPays() != null) {
                    countries.stream()
                            .filter(c -> c.getCode().equalsIgnoreCase(currentUser.getPays()))
                            .findFirst()
                            .ifPresent(c -> {
                                paysCombo.setValue(c);
                                loadUniversities(c.getName());
                            });
                }
            });
        });

        // Update universities when country changes
        paysCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadUniversities(newVal.getName());
            }
        });

        // Role-based visibility
        boolean isStudent = "ROLE_STUDENT".equals(currentUser.getRole());
        studentFields.setVisible(isStudent);
        studentFields.setManaged(isStudent);
        professorFields.setVisible(!isStudent);
        professorFields.setManaged(!isStudent);
        roleLabel.setText(isStudent ? "Étudiant" : "Professeur");
    }

    private void loadUniversities(String countryName) {
        etablissementCombo.getItems().clear();
        ExternalApiService.fetchUniversities(countryName).thenAccept(unis -> {
            Platform.runLater(() -> {
                etablissementCombo.getItems().addAll(unis);
                if (currentUser.getEtablissement() != null) {
                    etablissementCombo.getEditor().setText(currentUser.getEtablissement());
                }
            });
        });
    }

    private void loadUserData() {
        fullNameLabel.setText(currentUser.getFullName());
        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        telephoneField.setText(currentUser.getTelephone());

        if ("ROLE_STUDENT".equals(currentUser.getRole())) {
            if (currentUser.getAge() != null) ageField.setText(String.valueOf(currentUser.getAge()));
            sexeCombo.setValue(currentUser.getSexe());
            niveauField.setText(currentUser.getNiveau());
        } else {
            specialiteField.setText(currentUser.getSpecialite());
            niveauEnseignementField.setText(currentUser.getNiveauEnseignement());
            if (currentUser.getAnneesExperience() != null) experienceField.setText(String.valueOf(currentUser.getAnneesExperience()));
        }
    }

    @FXML
    private void handleUpdate() {
        currentUser.setNom(nomField.getText().trim());
        currentUser.setPrenom(prenomField.getText().trim());
        currentUser.setEmail(emailField.getText().trim());
        currentUser.setTelephone(telephoneField.getText().trim());
        if (paysCombo.getValue() != null) currentUser.setPays(paysCombo.getValue().getCode());
        currentUser.setEtablissement(etablissementCombo.getEditor().getText().trim());

        if ("ROLE_STUDENT".equals(currentUser.getRole())) {
            if (!ageField.getText().isEmpty()) currentUser.setAge(Integer.parseInt(ageField.getText()));
            currentUser.setSexe(sexeCombo.getValue());
            currentUser.setNiveau(niveauField.getText());
        } else {
            currentUser.setSpecialite(specialiteField.getText());
            currentUser.setNiveauEnseignement(niveauEnseignementField.getText());
            if (!experienceField.getText().isEmpty()) currentUser.setAnneesExperience(Integer.parseInt(experienceField.getText()));
        }

        try {
            userService.update(currentUser);
            showSuccess("Succès", "Profil mis à jour avec succès !");
            fullNameLabel.setText(currentUser.getFullName());
        } catch (Exception e) {
            showError("Erreur", "Impossible de mettre à jour le profil.");
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        switchScene("/fxml/auth/login.fxml", "Connexion - StudySprint");
    }

    @FXML
    private void handleChangePassword() {
        String email = currentUser.getEmail();
        
        // Generate 6-digit code
        String code = String.format("%06d", new java.util.Random().nextInt(1000000));
        currentUser.setResetToken(code);
        currentUser.setResetTokenExpiresAt(java.time.LocalDateTime.now().plusMinutes(15));
        
        userService.updateResetToken(currentUser);
        ForgotPasswordController.targetEmail = email;

        // Send Email asynchronously
        new Thread(() -> {
            try {
                com.example.studysprint.utils.MailerService.sendVerificationCode(email, code);
                Platform.runLater(() -> switchScene("/fxml/auth/verify-code.fxml", "Vérification - StudySprint"));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Erreur d'envoi", "L'envoi de l'email a échoué. Vérifiez votre connexion.");
                    e.printStackTrace();
                });
            }
        }).start();
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) nomField.getScene().getWindow();
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
        alert.showAndWait();
    }
}
