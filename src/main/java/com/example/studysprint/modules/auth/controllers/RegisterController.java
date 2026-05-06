package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.ExternalApiService;
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
import java.util.regex.Pattern;

public class RegisterController implements Initializable {

    @FXML private TextField nomField, prenomField, emailField, ageField, experienceField, telephoneField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private ComboBox<String> roleCombo, sexeCombo, etablissementCombo, niveauField, specialiteField, niveauEnseignementField;
    @FXML private ComboBox<ExternalApiService.Country> paysCombo;
    @FXML private Label nomError, prenomError, emailError, passwordError, confirmPasswordError, paysError, telephoneError;
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

            user.setTelephone(telephoneField.getText().trim());

            if ("Étudiant".equals(roleCombo.getValue())) {
                if (!ageField.getText().isEmpty()) user.setAge(Integer.parseInt(ageField.getText().trim()));
                user.setSexe(sexeCombo.getValue());
                user.setEtablissement(etablissement);
                user.setNiveau(niveauField.getValue());
            } else {
                user.setSpecialite(specialiteField.getValue());
                user.setNiveauEnseignement(niveauEnseignementField.getValue());
                if (!experienceField.getText().isEmpty()) user.setAnneesExperience(Integer.parseInt(experienceField.getText().trim()));
                user.setEtablissement(etablissement);
            }

            // Visual feedback
            nomField.setDisable(true);
            prenomField.setDisable(true);
            emailField.setDisable(true);
            passwordField.setDisable(true);

            new Thread(() -> {
                try {
                    userService.register(user, passwordField.getText());
                    Platform.runLater(() -> {
                        showSuccess("Compte créé !", "Vous pouvez maintenant vous connecter.");
                        handleGoToLogin();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        nomField.setDisable(false);
                        prenomField.setDisable(false);
                        emailField.setDisable(false);
                        passwordField.setDisable(false);
                        showInlineError(emailField, emailError, "Email déjà utilisé ou erreur réseau.");
                    });
                }
            }).start();
        }
    }

    @FXML
    private void handleGoToLogin() {
        switchScene("/fxml/auth/login.fxml", "Connexion - StudySprint");
    }

    @FXML private Label ageError; // Add ageError

    private boolean validate() {
        boolean isValid = true;
        resetErrors();

        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();
        String email    = emailField.getText().trim();
        boolean isStudent = "Étudiant".equals(roleCombo.getValue());

        // ── Champs de base ────────────────────────────────────────────────
        if (nomField.getText().trim().isEmpty()) {
            showInlineError(nomField, nomError, "Nom requis"); isValid = false;
        }
        if (prenomField.getText().trim().isEmpty()) {
            showInlineError(prenomField, prenomError, "Prénom requis"); isValid = false;
        }
        if (email.isEmpty() || !Pattern.matches("^[A-Za-z0-9+_.-]+@(.+)$", email)) {
            showInlineError(emailField, emailError, "Email invalide"); isValid = false;
        }

        // ── Mot de passe ─────────────────────────────────────────────────
        String passRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";
        if (!Pattern.matches(passRegex, password)) {
            showInlineError(passwordField, passwordError, "Min 8 caractères, 1 Majuscule, 1 minuscule, 1 chiffre."); isValid = false;
        } else if (!password.equals(confirm)) {
            showInlineError(confirmPasswordField, confirmPasswordError, "Les mots de passe ne correspondent pas."); isValid = false;
        }

        // ── Pays ─────────────────────────────────────────────────────────
        if (paysCombo.getValue() == null) {
            showInlineError(paysCombo, paysError, "Le pays est requis"); isValid = false;
        }

        // ── Établissement ────────────────────────────────────────────────
        String etablissement = etablissementCombo.getValue() != null
                ? etablissementCombo.getValue()
                : etablissementCombo.getEditor().getText().trim();
        if (etablissement == null || etablissement.isEmpty()) {
            etablissementCombo.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 1px;"); isValid = false;
        } else {
            etablissementCombo.setStyle("");
        }

        // ── Téléphone (Optionnel) ────────────────
        String phone = telephoneField.getText().trim();
        if (!phone.isEmpty() && !Pattern.matches("^\\d{8}$", phone)) {
            showInlineError(telephoneField, telephoneError, "Doit contenir 8 chiffres"); isValid = false;
        }

        // ── Champs spécifiques au rôle ───────────────────────────────────
        if (isStudent) {
            String ageStr = ageField.getText().trim();
            if (ageStr.isEmpty()) {
                showInlineError(ageField, ageError, "S'il vous plaît, l'âge est obligatoire."); isValid = false;
            } else {
                try {
                    int age = Integer.parseInt(ageStr);
                    if (age <= 13) {
                        showInlineError(ageField, ageError, "S'il vous plaît, l'âge est obligatoire et supérieur à 13.");
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    showInlineError(ageField, ageError, "Format invalide."); isValid = false;
                }
            }

            if (sexeCombo.getValue() == null) {
                sexeCombo.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 1px;"); isValid = false;
            } else { sexeCombo.setStyle(""); }

            if (niveauField.getValue() == null) {
                niveauField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 1px;"); isValid = false;
            } else { niveauField.setStyle(""); }

        } else {
            if (specialiteField.getValue() == null) {
                specialiteField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 1px;"); isValid = false;
            } else { specialiteField.setStyle(""); }

            if (niveauEnseignementField.getValue() == null) {
                niveauEnseignementField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 1px;"); isValid = false;
            } else { niveauEnseignementField.setStyle(""); }

            if (experienceField.getText().trim().isEmpty()) {
                experienceField.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 1px;"); isValid = false;
            } else { experienceField.setStyle(""); }
        }

        return isValid;
    }

    private void showInlineError(Control field, Label errorLabel, String message) {
        field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 1px;");
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void resetErrors() {
        Control[] fields = {nomField, prenomField, emailField, passwordField, confirmPasswordField, paysCombo, telephoneField, ageField, etablissementCombo, sexeCombo, niveauField, specialiteField, niveauEnseignementField, experienceField};
        for (Control field : fields) {
            if (field != null) field.setStyle("");
        }
        
        Label[] errors = {nomError, prenomError, emailError, passwordError, confirmPasswordError, paysError, telephoneError, ageError};
        for (Label error : errors) {
            if (error != null) {
                error.setVisible(false);
                error.setManaged(false);
            }
        }
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
        alert.setContentText(message);
        alert.showAndWait();
    }
}
