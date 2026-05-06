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
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private TextField nomField, prenomField, emailField, ageField, specialiteField, niveauEnseignementField, experienceField, telephoneField;
    @FXML private ComboBox<String> sexeCombo, etablissementCombo, niveauCombo;
    @FXML private ComboBox<ExternalApiService.Country> paysCombo;
    @FXML private Label roleLabel, fullNameLabel;
    @FXML private Label nomError, prenomError, emailError, telephoneError, ageError;
    @FXML private VBox studentFields, professorFields;
    @FXML private Button updateButton;

    private final UtilisateurService userService = new UtilisateurService();
    private Utilisateur currentUser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            setupForm();
            loadUserData();
        }
    }

    public void setTempUser(Utilisateur user) {
        this.currentUser = user;
        Platform.runLater(() -> {
            setupForm();
            loadUserData();
            validateFields();
        });
    }

    private void setupForm() {
        sexeCombo.getItems().setAll("M", "F");
        etablissementCombo.setEditable(true);

        // Load Countries
        ExternalApiService.fetchCountries().thenAccept(countries -> {
            Platform.runLater(() -> {
                paysCombo.getItems().setAll(countries);
                if (currentUser != null && currentUser.getPays() != null) {
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
        if (currentUser != null) {
            boolean isStudent = "ROLE_STUDENT".equals(currentUser.getRole());
            studentFields.setVisible(isStudent);
            studentFields.setManaged(isStudent);
            professorFields.setVisible(!isStudent);
            professorFields.setManaged(!isStudent);
            roleLabel.setText(isStudent ? "Étudiant" : "Professeur");

            if (isStudent) {
                ExternalApiService.fetchLevels().thenAccept(levels -> {
                    Platform.runLater(() -> {
                        niveauCombo.getItems().setAll(levels);
                        if (currentUser.getNiveau() != null) {
                            niveauCombo.setValue(currentUser.getNiveau());
                        }
                        validateFields();
                    });
                });
            }
        }

        addValidationListeners();
    }

    private void addValidationListeners() {
        nomField.textProperty().addListener((o, old, newVal) -> validateFields());
        prenomField.textProperty().addListener((o, old, newVal) -> validateFields());
        emailField.textProperty().addListener((o, old, newVal) -> validateFields());
        telephoneField.textProperty().addListener((o, old, newVal) -> validateFields());
        ageField.textProperty().addListener((o, old, newVal) -> validateFields());
        niveauCombo.valueProperty().addListener((o, old, newVal) -> validateFields());
        paysCombo.valueProperty().addListener((o, old, newVal) -> validateFields());
        sexeCombo.valueProperty().addListener((o, old, newVal) -> validateFields());
        etablissementCombo.getEditor().textProperty().addListener((o, old, newVal) -> validateFields());
    }

    private boolean isFormValid() {
        if (currentUser == null) return false;
        boolean isValid = true;

        // Validation Nom
        if (nomField.getText().trim().isEmpty()) {
            showFieldError(nomField, nomError, "Le nom est obligatoire.");
            isValid = false;
        } else {
            hideFieldError(nomField, nomError);
        }

        // Validation Prénom
        if (prenomField.getText().trim().isEmpty()) {
            showFieldError(prenomField, prenomError, "Le prénom est obligatoire.");
            isValid = false;
        } else {
            hideFieldError(prenomField, prenomError);
        }

        // Validation Email
        if (emailField.getText().trim().isEmpty() || !emailField.getText().contains("@")) {
            showFieldError(emailField, emailError, "Email invalide.");
            isValid = false;
        } else {
            hideFieldError(emailField, emailError);
        }

        // Validation Téléphone (Optionnel mais 8 chiffres si rempli)
        String tel = telephoneField.getText().trim();
        if (!tel.isEmpty() && !tel.matches("^\\d{8}$")) {
            showFieldError(telephoneField, telephoneError, "8 chiffres requis.");
            isValid = false;
        } else {
            hideFieldError(telephoneField, telephoneError);
        }

        if ("ROLE_STUDENT".equals(currentUser.getRole())) {
            // Validation Age
            String ageStr = ageField.getText().trim();
            if (ageStr.isEmpty()) {
                showFieldError(ageField, ageError, "L'âge est requis.");
                isValid = false;
            } else {
                try {
                    int age = Integer.parseInt(ageStr);
                    if (age <= 13) {
                        showFieldError(ageField, ageError, "Doit être > 13 ans.");
                        isValid = false;
                    } else {
                        hideFieldError(ageField, ageError);
                    }
                } catch (NumberFormatException e) {
                    showFieldError(ageField, ageError, "Format invalide.");
                    isValid = false;
                }
            }
            if (sexeCombo.getValue() == null) isValid = false;
            if (niveauCombo.getValue() == null) isValid = false;
        }

        if (paysCombo.getValue() == null) isValid = false;
        if (etablissementCombo.getEditor().getText().trim().isEmpty()) isValid = false;

        return isValid;
    }

    private void showFieldError(TextField field, Label errorLabel, String message) {
        field.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 1px;");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideFieldError(TextField field, Label errorLabel) {
        field.setStyle("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void validateFields() {
        updateButton.setDisable(!isFormValid());
    }

    private void loadUniversities(String countryName) {
        etablissementCombo.getItems().clear();
        ExternalApiService.fetchUniversities(countryName).thenAccept(unis -> {
            Platform.runLater(() -> {
                etablissementCombo.getItems().addAll(unis);
                if (currentUser != null && currentUser.getEtablissement() != null) {
                    etablissementCombo.getEditor().setText(currentUser.getEtablissement());
                }
            });
        });
    }

    private void loadUserData() {
        if (currentUser == null) return;
        fullNameLabel.setText(currentUser.getFullName());
        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());
        telephoneField.setText(currentUser.getTelephone());

        if ("ROLE_STUDENT".equals(currentUser.getRole())) {
            if (currentUser.getAge() != null) ageField.setText(String.valueOf(currentUser.getAge()));
            sexeCombo.setValue(currentUser.getSexe());
            if (currentUser.getNiveau() != null) niveauCombo.setValue(currentUser.getNiveau());
        } else {
            specialiteField.setText(currentUser.getSpecialite());
            niveauEnseignementField.setText(currentUser.getNiveauEnseignement());
            if (currentUser.getAnneesExperience() != null) experienceField.setText(String.valueOf(currentUser.getAnneesExperience()));
        }
    }

    @FXML
    private void handleUpdate() {
        if (!isFormValid()) {
            return;
        }

        currentUser.setNom(nomField.getText().trim());
        currentUser.setPrenom(prenomField.getText().trim());
        currentUser.setEmail(emailField.getText().trim());
        currentUser.setTelephone(telephoneField.getText().trim());
        if (paysCombo.getValue() != null) currentUser.setPays(paysCombo.getValue().getCode());
        currentUser.setEtablissement(etablissementCombo.getEditor().getText().trim());

        if ("ROLE_STUDENT".equals(currentUser.getRole())) {
            currentUser.setAge(Integer.parseInt(ageField.getText().trim()));
            currentUser.setSexe(sexeCombo.getValue());
            currentUser.setNiveau(niveauCombo.getValue());
        } else {
            currentUser.setSpecialite(specialiteField.getText().trim());
            currentUser.setNiveauEnseignement(niveauEnseignementField.getText().trim());
            if (!experienceField.getText().isEmpty()) currentUser.setAnneesExperience(Integer.parseInt(experienceField.getText().trim()));
        }

        try {
            if (currentUser.getId() == 0) {
                userService.add(currentUser);
                currentUser = userService.findByEmail(currentUser.getEmail());
            } else {
                userService.update(currentUser);
            }
            SessionManager.getInstance().setCurrentUser(currentUser);
            fullNameLabel.setText(currentUser.getFullName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        switchScene("/fxml/auth/login.fxml", "Connexion - StudySprint");
    }

    @FXML
    private void handleFaceId() {
        switchScene("/fxml/utilisateurs/face-enroll.fxml", "Enregistrement Face ID - StudySprint");
    }

    @FXML
    private void handleChangePassword() {
        String email = currentUser.getEmail();
        String code = String.format("%06d", new java.util.Random().nextInt(1000000));
        currentUser.setResetToken(code);
        currentUser.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(15));
        userService.updateResetToken(currentUser);
        ForgotPasswordController.targetEmail = email;

        new Thread(() -> {
            try {
                com.example.studysprint.utils.MailerService.sendVerificationCode(email, code);
                Platform.runLater(() -> switchScene("/fxml/auth/verify-code.fxml", "Vérification - StudySprint"));
            } catch (Exception e) {
                Platform.runLater(() -> showError("Erreur d'envoi", "L'envoi de l'email a échoué."));
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
