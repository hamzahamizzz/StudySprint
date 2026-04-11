package com.example.studysprint.modules.utilisateurs.controllers;

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
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class AdminProfileController implements Initializable {

    @FXML private Label avatarLabel;
    @FXML private Label fullNameLabel;
    @FXML private Label messageLabel;

    // Editable fields
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telField;
    @FXML private TextField ageField;

    // Country & Sexe ComboBoxes
    @FXML private ComboBox<ExternalApiService.Country> paysCombo;
    @FXML private ComboBox<String> sexeCombo;

    // Read-only fields
    @FXML private TextField roleField;
    @FXML private TextField dateField;
    @FXML private TextField statutField;

    private final UtilisateurService userService = new UtilisateurService();
    private Utilisateur currentAdmin;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentAdmin = SessionManager.getInstance().getCurrentUser();
        if (currentAdmin != null) {
            populateFields();
        }

        // Init sexe ComboBox
        sexeCombo.getItems().addAll("M", "F");

        // Load countries from API asynchronously
        ExternalApiService.fetchCountries().thenAccept(countries -> {
            Platform.runLater(() -> {
                paysCombo.getItems().setAll(countries);
                paysCombo.setPromptText("Sélectionnez un pays");

                // If admin already has a country stored, pre-select it
                if (currentAdmin != null && currentAdmin.getPays() != null) {
                    String savedCode = currentAdmin.getPays();
                    countries.stream()
                            .filter(c -> c.getCode().equalsIgnoreCase(savedCode)
                                      || c.getName().equalsIgnoreCase(savedCode))
                            .findFirst()
                            .ifPresent(c -> paysCombo.setValue(c));
                }
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            Platform.runLater(() -> paysCombo.setPromptText("Erreur de chargement"));
            return null;
        });
    }

    private void populateFields() {
        // Header avatar (initials)
        String initials = "";
        if (currentAdmin.getPrenom() != null && !currentAdmin.getPrenom().isEmpty())
            initials += currentAdmin.getPrenom().charAt(0);
        if (currentAdmin.getNom() != null && !currentAdmin.getNom().isEmpty())
            initials += currentAdmin.getNom().charAt(0);
        avatarLabel.setText(initials.toUpperCase());
        fullNameLabel.setText(currentAdmin.getFullName());

        // Editable fields
        nomField.setText(nvl(currentAdmin.getNom()));
        prenomField.setText(nvl(currentAdmin.getPrenom()));
        emailField.setText(nvl(currentAdmin.getEmail()));
        telField.setText(nvl(currentAdmin.getTelephone()));
        ageField.setText(currentAdmin.getAge() != null ? String.valueOf(currentAdmin.getAge()) : "");
        if (currentAdmin.getSexe() != null) sexeCombo.setValue(currentAdmin.getSexe());

        // Read-only fields
        roleField.setText("Administrateur");
        statutField.setText(currentAdmin.getStatut() != null ? currentAdmin.getStatut() : "actif");
        if (currentAdmin.getDateInscription() != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            dateField.setText(currentAdmin.getDateInscription().format(fmt));
        }
    }

    @FXML
    private void handleSave() {
        messageLabel.setVisible(false);

        // --- Validation ---
        String nom    = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email  = emailField.getText().trim();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
            showMessage("Le nom, le prénom et l'email sont obligatoires.", false);
            return;
        }

        if (!Pattern.matches("^[A-Za-z0-9+_.-]+@(.+)$", email)) {
            showMessage("Adresse email invalide.", false);
            return;
        }

        if (!ageField.getText().trim().isEmpty()) {
            try {
                int age = Integer.parseInt(ageField.getText().trim());
                if (age < 0 || age > 120) {
                    showMessage("Âge invalide (doit être entre 0 et 120).", false);
                    return;
                }
                currentAdmin.setAge(age);
            } catch (NumberFormatException e) {
                showMessage("L'âge doit être un nombre entier.", false);
                return;
            }
        } else {
            currentAdmin.setAge(null);
        }

        // --- Apply changes ---
        currentAdmin.setNom(nom);
        currentAdmin.setPrenom(prenom);
        currentAdmin.setEmail(email);
        currentAdmin.setTelephone(telField.getText().trim());
        currentAdmin.setSexe(sexeCombo.getValue());

        // Save country from ComboBox (store the ISO code)
        if (paysCombo.getValue() != null) {
            currentAdmin.setPays(paysCombo.getValue().getCode());
        }

        try {
            userService.update(currentAdmin);
            // Refresh the session with updated data
            SessionManager.getInstance().setCurrentUser(currentAdmin);

            // Refresh header avatar and name
            fullNameLabel.setText(currentAdmin.getFullName());
            String initials = "";
            if (currentAdmin.getPrenom() != null && !currentAdmin.getPrenom().isEmpty())
                initials += currentAdmin.getPrenom().charAt(0);
            if (currentAdmin.getNom() != null && !currentAdmin.getNom().isEmpty())
                initials += currentAdmin.getNom().charAt(0);
            avatarLabel.setText(initials.toUpperCase());

            showMessage("✓  Profil mis à jour avec succès !", true);
        } catch (Exception e) {
            showMessage("Erreur lors de la sauvegarde : " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleBack() {
        switchScene("/fxml/utilisateurs/users-list.fxml", "Gestion Utilisateurs - StudySprint");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void showMessage(String msg, boolean success) {
        messageLabel.setText(msg);
        messageLabel.setStyle(success
                ? "-fx-text-fill: #00b894; -fx-font-weight: bold;"
                : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        messageLabel.setVisible(true);
    }

    private String nvl(String value) {
        return value != null ? value : "";
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
}
