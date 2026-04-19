package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.AppNavigator;
import com.example.studysprint.utils.ExternalApiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class RegisterController implements Initializable {

    @FXML private TextField nomField, prenomField, emailField, ageField, experienceField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private ComboBox<String> roleCombo, sexeCombo, etablissementCombo, niveauField, specialiteField, niveauEnseignementField;
    @FXML private ComboBox<ExternalApiService.Country> paysCombo;
    @FXML private Label nomError, prenomError, emailError, passwordError, confirmPasswordError, paysError;
    @FXML private VBox studentFields, professorFields;

    private final UtilisateurService userService = new UtilisateurService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        roleCombo.getItems().addAll("Étudiant", "Professeur");
        roleCombo.setValue("Étudiant");
        
        sexeCombo.getItems().addAll("M", "F");
        sexeCombo.setValue("M");

        // Load Levels, Specialites & Niveaux from API
        ExternalApiService.fetchLevels().thenAccept(levels ->
            Platform.runLater(() -> niveauField.getItems().addAll(levels)));
        ExternalApiService.fetchSpecialites().thenAccept(specs ->
            Platform.runLater(() -> specialiteField.getItems().addAll(specs)));
        ExternalApiService.fetchNiveauxEnseignement().thenAccept(niveaux ->
            Platform.runLater(() -> niveauEnseignementField.getItems().addAll(niveaux)));

        // Handle dynamic fields based on role
        roleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isStudent = "Étudiant".equals(newVal);
            studentFields.setVisible(isStudent);
            studentFields.setManaged(isStudent);
            professorFields.setVisible(!isStudent);
            professorFields.setManaged(!isStudent);
        });

        // Load Countries
        ExternalApiService.fetchCountries().thenAccept(countries -> {
            Platform.runLater(() -> paysCombo.getItems().addAll(countries));
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });

        // Load Universities when country changes
        paysCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                etablissementCombo.getItems().clear();
                etablissementCombo.setPromptText("Chargement...");
                ExternalApiService.fetchUniversities(newVal.getName()).thenAccept(unis -> {
                    Platform.runLater(() -> {
                        etablissementCombo.getItems().addAll(unis);
                        etablissementCombo.setPromptText("Sélectionnez votre établissement");
                    });
                });
            }
        });

        etablissementCombo.setEditable(true);
    }

    @FXML
    private void handleRegister() {
        if (validate()) {
            Utilisateur user = new Utilisateur();
            user.setNom(nomField.getText().trim());
            user.setPrenom(prenomField.getText().trim());
            user.setEmail(emailField.getText().trim());
            if (paysCombo.getValue() != null) user.setPays(paysCombo.getValue().getCode());
            user.setRole("Étudiant".equals(roleCombo.getValue()) ? "ROLE_STUDENT" : "ROLE_PROFESSOR");
            user.setDiscr("Étudiant".equals(roleCombo.getValue()) ? "student" : "professor");

            // Determine etablissement value (ComboBox selection or typed text)
            String etablissement = etablissementCombo.getValue() != null
                    ? etablissementCombo.getValue()
                    : etablissementCombo.getEditor().getText().trim();

            if ("Étudiant".equals(roleCombo.getValue())) {
                if (!ageField.getText().isEmpty()) user.setAge(Integer.parseInt(ageField.getText()));
                user.setSexe(sexeCombo.getValue());
                user.setEtablissement(etablissement);
                user.setNiveau(niveauField.getValue());
            } else {
                user.setSpecialite(specialiteField.getValue());
                user.setNiveauEnseignement(niveauEnseignementField.getValue());
                if (!experienceField.getText().isEmpty()) user.setAnneesExperience(Integer.parseInt(experienceField.getText().trim()));
                user.setEtablissement(etablissement);
            }

            try {
                userService.register(user, passwordField.getText());
                showSuccess("Compte créé !", "Vous pouvez maintenant vous connecter.");
                handleGoToLogin();
            } catch (Exception e) {
                showError("Erreur", "L'email est peut-être déjà utilisé.");
            }
        }
    }

    @FXML
    private void handleGoToLogin() {
        switchScene("/fxml/auth/login.fxml", "Connexion - StudySprint");
    }

    private boolean validate() {
        boolean isValid = true;
        resetErrors();

        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();
        String email    = emailField.getText().trim();
        boolean isStudent = "Étudiant".equals(roleCombo.getValue());

        // ── Champs de base ────────────────────────────────────────────────
        if (nomField.getText().trim().isEmpty()) {
            nomError.setText("Nom requis"); nomError.setVisible(true); isValid = false;
        }
        if (prenomField.getText().trim().isEmpty()) {
            prenomError.setText("Prénom requis"); prenomError.setVisible(true); isValid = false;
        }
        if (email.isEmpty() || !Pattern.matches("^[A-Za-z0-9+_.-]+@(.+)$", email)) {
            emailError.setText("Email invalide"); emailError.setVisible(true); isValid = false;
        }

        // ── Mot de passe ─────────────────────────────────────────────────
        String passRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";
        if (!Pattern.matches(passRegex, password)) {
            passwordError.setText("⚠ Le mot de passe doit contenir au minimum 8 caractères,\nune lettre majuscule (A-Z), une lettre minuscule (a-z) et un chiffre (0-9).");
            passwordError.setVisible(true);
            isValid = false;
        } else if (!password.equals(confirm)) {
            confirmPasswordError.setText("Ne correspond pas");
            confirmPasswordError.setVisible(true);
            isValid = false;
        }

        // ── Pays ─────────────────────────────────────────────────────────
        if (paysCombo.getValue() == null) {
            paysError.setText("Pays requis"); paysError.setVisible(true); isValid = false;
        }

        // Stop here if basic fields are invalid
        if (!isValid) return false;

        // ── Établissement ────────────────────────────────────────────────
        // Accept either a dropdown selection OR typed text
        String etablissement = etablissementCombo.getValue() != null
                ? etablissementCombo.getValue()
                : etablissementCombo.getEditor().getText().trim();
        if (etablissement == null || etablissement.isEmpty()) {
            showError("Champ manquant", "Veuillez sélectionner ou saisir votre établissement.");
            return false;
        }

        // ── Champs spécifiques au rôle ───────────────────────────────────
        if (isStudent) {
            if (ageField.getText().trim().isEmpty()) {
                showError("Champ manquant", "L'âge est obligatoire."); return false;
            }
            if (sexeCombo.getValue() == null) {
                showError("Champ manquant", "Le sexe est obligatoire."); return false;
            }
            if (niveauField.getValue() == null) {
                showError("Champ manquant", "Le niveau d'étude est obligatoire. \nSi la liste est vide, attendez quelques secondes le chargement.");
                return false;
            }
        } else {
            if (specialiteField.getValue() == null) {
                showError("Champ manquant", "La spécialité est obligatoire."); return false;
            }
            if (niveauEnseignementField.getValue() == null) {
                showError("Champ manquant", "Le niveau d'enseignement est obligatoire."); return false;
            }
            if (experienceField.getText().trim().isEmpty()) {
                showError("Champ manquant", "Les années d'expérience sont obligatoires."); return false;
            }
        }

        return true;
    }

    private void resetErrors() {
        nomError.setVisible(false);
        prenomError.setVisible(false);
        emailError.setVisible(false);
        passwordError.setVisible(false);
        confirmPasswordError.setVisible(false);
        paysError.setVisible(false);
    }

    private void switchScene(String fxmlPath, String title) {
        Stage stage = (Stage) nomField.getScene().getWindow();
        AppNavigator.switchTo(stage, fxmlPath, title, getClass());
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
