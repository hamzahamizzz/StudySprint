package com.example.studysprint.modules.utilisateurs.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
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

public class UserFormController implements Initializable {

    @FXML private Label titleLabel;
    @FXML private TextField nomField, prenomField, emailField, ageField, telField, experienceField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo, sexeCombo, etablissementCombo, niveauField, specialiteField, niveauEnseignementField;
    @FXML private ComboBox<ExternalApiService.Country> paysCombo;
    @FXML private Label nomError, prenomError, emailError, passwordError, paysError;
    @FXML private VBox passwordContainer, studentFields, professorFields;
    @FXML private Button saveButton;

    private final UtilisateurService userService = new UtilisateurService();
    private Utilisateur currentUtilisateur;
    private UsersListController parentController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        roleCombo.getItems().addAll("Admin", "Etudiant", "Professeur");
        roleCombo.setValue("Etudiant");
        
        sexeCombo.getItems().addAll("M", "F");
        sexeCombo.setValue("M");

        etablissementCombo.setEditable(true);

        // Load Levels, Specialites & Niveaux from API
        ExternalApiService.fetchLevels().thenAccept(levels ->
            Platform.runLater(() -> niveauField.getItems().addAll(levels)));
        ExternalApiService.fetchSpecialites().thenAccept(specs ->
            Platform.runLater(() -> specialiteField.getItems().addAll(specs)));
        ExternalApiService.fetchNiveauxEnseignement().thenAccept(niveaux ->
            Platform.runLater(() -> niveauEnseignementField.getItems().addAll(niveaux)));

        // Handle dynamic fields based on role
        roleCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateRoleFields(newVal);
        });

        // Load Countries
        ExternalApiService.fetchCountries().thenAccept(countries -> {
            Platform.runLater(() -> {
                paysCombo.getItems().setAll(countries);
                // If editing, re-select the correct country
                if (currentUtilisateur != null && currentUtilisateur.getPays() != null) {
                    countries.stream()
                            .filter(c -> c.getCode().equals(currentUtilisateur.getPays()))
                            .findFirst()
                            .ifPresent(c -> paysCombo.setValue(c));
                }
            });
        });

        // Load Universities when country changes
        paysCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                etablissementCombo.getItems().clear();
                etablissementCombo.setPromptText("Chargement...");
                ExternalApiService.fetchUniversities(newVal.getName()).thenAccept(unis -> {
                    Platform.runLater(() -> {
                        etablissementCombo.getItems().setAll(unis);
                        etablissementCombo.setPromptText("Sélectionnez l'établissement");
                        if (currentUtilisateur != null && currentUtilisateur.getEtablissement() != null) {
                            etablissementCombo.getEditor().setText(currentUtilisateur.getEtablissement());
                        }
                    });
                });
            }
        });

        updateRoleFields("Etudiant");
    }

    private void updateRoleFields(String role) {
        boolean isStudent = "Etudiant".equalsIgnoreCase(role);
        boolean isProfessor = "Professeur".equalsIgnoreCase(role);
        
        studentFields.setVisible(isStudent);
        studentFields.setManaged(isStudent);
        professorFields.setVisible(isProfessor);
        professorFields.setManaged(isProfessor);
    }

    public void setUtilisateur(Utilisateur u) {
        this.currentUtilisateur = u;
        if (u != null) {
            titleLabel.setText("Modifier l'Utilisateur");
            nomField.setText(u.getNom());
            prenomField.setText(u.getPrenom());
            emailField.setText(u.getEmail());
            
            // Role handling
            String roleDisplayName = "Etudiant";
            if ("ROLE_ADMIN".equals(u.getRole())) roleDisplayName = "Admin";
            else if ("ROLE_PROFESSOR".equals(u.getRole())) roleDisplayName = "Professeur";
            roleCombo.setValue(roleDisplayName);

            if (u.getAge() != null) ageField.setText(String.valueOf(u.getAge()));
            if (u.getSexe() != null) sexeCombo.setValue(u.getSexe());
            telField.setText(u.getTelephone());
            
            // Specific fields
            if (u.getNiveau() != null) niveauField.setValue(u.getNiveau());
            if (u.getSpecialite() != null) specialiteField.setValue(u.getSpecialite());
            if (u.getNiveauEnseignement() != null) niveauEnseignementField.setValue(u.getNiveauEnseignement());
            if (u.getAnneesExperience() != null) experienceField.setText(String.valueOf(u.getAnneesExperience()));
            
            // Password hidden in edit mode as requested
            passwordContainer.setVisible(false);
            passwordContainer.setManaged(false);

            // Country and Etablissement will be set by initialize's listeners after async fetch
        }
    }

    public void setParentController(UsersListController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void handleSave() {
        if (validate()) {
            boolean isNew = (currentUtilisateur == null);
            if (isNew) {
                currentUtilisateur = new Utilisateur();
                currentUtilisateur.setMotDePasse(passwordField.getText());
            }

            currentUtilisateur.setNom(nomField.getText().trim());
            currentUtilisateur.setPrenom(prenomField.getText().trim());
            currentUtilisateur.setEmail(emailField.getText().trim());
            
            if (paysCombo.getValue() != null) {
                currentUtilisateur.setPays(paysCombo.getValue().getCode());
            }

            String role = roleCombo.getValue();
            if ("Admin".equals(role)) {
                currentUtilisateur.setRole("ROLE_ADMIN");
                currentUtilisateur.setDiscr("admin");
            } else if ("Etudiant".equals(role)) {
                currentUtilisateur.setRole("ROLE_STUDENT");
                currentUtilisateur.setDiscr("student");
                currentUtilisateur.setNiveau(niveauField.getValue());
            } else {
                currentUtilisateur.setRole("ROLE_PROFESSOR");
                currentUtilisateur.setDiscr("professor");
                currentUtilisateur.setSpecialite(specialiteField.getValue());
                currentUtilisateur.setNiveauEnseignement(niveauEnseignementField.getValue());
                if (!experienceField.getText().isEmpty()) {
                    currentUtilisateur.setAnneesExperience(Integer.parseInt(experienceField.getText().trim()));
                }
            }
            
            String etablissement = etablissementCombo.getEditor().getText().trim();
            currentUtilisateur.setEtablissement(etablissement);
            
            if (!ageField.getText().isEmpty()) {
                currentUtilisateur.setAge(Integer.parseInt(ageField.getText()));
            }
            currentUtilisateur.setSexe(sexeCombo.getValue());
            currentUtilisateur.setTelephone(telField.getText().trim());

            try {
                if (isNew) {
                    userService.add(currentUtilisateur);
                } else {
                    userService.update(currentUtilisateur);
                }
                close();
            } catch (Exception e) {
                showError("Erreur lors de l'enregistrement", "L'email est peut-être déjà utilisé.");
            }
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private boolean validate() {
        boolean isValid = true;
        resetErrors();

        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String role = roleCombo.getValue();

        if (nomField.getText().trim().isEmpty()) { nomError.setText("Nom requis"); nomError.setVisible(true); isValid = false; }
        if (prenomField.getText().trim().isEmpty()) { prenomError.setText("Prénom requis"); prenomError.setVisible(true); isValid = false; }
        
        if (email.isEmpty() || !Pattern.matches("^[A-Za-z0-9+_.-]+@(.+)$", email)) {
            emailError.setText("Email invalide"); emailError.setVisible(true); isValid = false;
        }

        // Strong password for new users
        if (currentUtilisateur == null) {
            String passRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$";
            if (!Pattern.matches(passRegex, password)) {
                passwordError.setText("⚠ Le mot de passe doit contenir au minimum 8 caractères,\nune lettre majuscule (A-Z), une lettre minuscule (a-z) et un chiffre (0-9).");
                passwordError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                passwordError.setVisible(true);
                isValid = false;
            }
        }

        if (paysCombo.getValue() == null) {
            paysError.setText("Pays requis"); paysError.setVisible(true); isValid = false;
        }

        if (etablissementCombo.getEditor().getText().trim().isEmpty()) {
            showError("Champs vides", "L'établissement est obligatoire.");
            return false;
        }

        // ── Validation Age (> 13) ─────────────────────────────────────────
        String ageStr = ageField.getText().trim();
        if (ageStr.isEmpty()) {
            showError("Champs vides", "L'âge est obligatoire.");
            return false;
        }
        try {
            int age = Integer.parseInt(ageStr);
            if (age <= 13) {
                showError("Validation Age", "L'utilisateur doit avoir plus de 13 ans.");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Validation Age", "L'âge doit être un nombre valide.");
            return false;
        }

        // ── Validation Téléphone (Optionnel mais 8 chiffres si rempli) ───
        String tel = telField.getText().trim();
        if (!tel.isEmpty() && !Pattern.matches("^\\d{8}$", tel)) {
            showError("Validation Téléphone", "Le téléphone doit contenir exactement 8 chiffres.");
            return false;
        }

        // Specific fields validation
        if ("Etudiant".equals(role)) {
            if (niveauField.getValue() == null) {
                showError("Champs vides", "Le niveau est obligatoire pour un étudiant.");
                return false;
            }
        } else if ("Professeur".equals(role)) {
            if (specialiteField.getValue() == null || niveauEnseignementField.getValue() == null || experienceField.getText().isEmpty()) {
                showError("Champs vides", "La spécialité, le niveau et l'expérience sont obligatoires.");
                return false;
            }
        }

        return isValid;
    }

    private void resetErrors() {
        nomError.setVisible(false);
        prenomError.setVisible(false);
        emailError.setVisible(false);
        passwordError.setVisible(false);
        paysError.setVisible(false);
    }

    private void close() {
        ((Stage) nomField.getScene().getWindow()).close();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
