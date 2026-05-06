package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.modules.auth.services.GoogleAuthService;
import com.example.studysprint.utils.SessionManager;
import com.google.api.services.oauth2.model.Userinfo;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label emailError, passwordError;
    @FXML private Button loginButton;

    private final UtilisateurService userService = new UtilisateurService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        emailError.setVisible(false);
        passwordError.setVisible(false);
    }

    @FXML
    private void handleLogin() {
        if (!validate()) return;

        String email = emailField.getText();
        String password = passwordField.getText();

        Utilisateur user = userService.authenticate(email, password);
        if (user != null) {
            if ("actif".equalsIgnoreCase(user.getStatut()) || user.getStatut() == null || user.getStatut().isEmpty()) {
                SessionManager.getInstance().setCurrentUser(user);
                loadMainApp(user);
            } else {
                SessionManager.getInstance().setCurrentUser(user);
                switchScene("/fxml/auth/account-deactivated.fxml", "Compte Suspendu - StudySprint");
            }
        } else {
            passwordError.setText("Email ou mot de passe incorrect.");
            passwordError.setVisible(true);
        }
    }

    @FXML
    private void handleGoogleLogin() {
        String originalText = loginButton.getText();
        loginButton.setDisable(true);
        loginButton.setText("Authentification Google...");
        passwordError.setVisible(false);

        new Thread(() -> {
            try {
                Userinfo userInfo = GoogleAuthService.authenticate();
                
                if (userInfo != null && userInfo.getEmail() != null) {
                    Utilisateur user = userService.findByEmail(userInfo.getEmail());

                    Platform.runLater(() -> {
                        if (user == null) {
                            System.out.println("Google login failed: Account not found for " + userInfo.getEmail());
                            showError("Compte introuvable", "Aucun compte n'est associé à cette adresse Google. Veuillez d'abord vous inscrire manuellement.");
                            resetLoginButton(originalText);
                        } else {
                            if ("actif".equalsIgnoreCase(user.getStatut()) || user.getStatut() == null || user.getStatut().isEmpty()) {
                                System.out.println("Google login success for: " + userInfo.getEmail());
                                SessionManager.getInstance().setCurrentUser(user);
                                loadMainApp(user);
                            } else {
                                SessionManager.getInstance().setCurrentUser(user);
                                switchScene("/fxml/auth/account-deactivated.fxml", "Compte Suspendu - StudySprint");
                            }
                        }
                    });
                } else {
                    System.out.println("Authentification Google annulée ou échouée.");
                    Platform.runLater(() -> resetLoginButton(originalText));
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    e.printStackTrace();
                    resetLoginButton(originalText);
                    passwordError.setText("Erreur Google : Connexion impossible.");
                    passwordError.setVisible(true);
                });
            }
        }).start();
    }

    private void resetLoginButton(String text) {
        loginButton.setDisable(false);
        loginButton.setText(text);
    }

    @FXML
    private void handleGoToRegister() {
        Platform.runLater(() -> switchScene("/fxml/auth/register.fxml", "Inscription - StudySprint"));
    }

    @FXML
    private void handleFaceLogin() {
        // Pour Face ID, on s'assure aussi que le changement de scène est propre
        Platform.runLater(() -> {
            try {
                switchScene("/fxml/auth/face-login.fxml", "Connexion Face ID - StudySprint");
            } catch (Exception e) {
                e.printStackTrace();
                showError("Erreur Face ID", "Impossible de charger l'interface de reconnaissance faciale.");
            }
        });
    }

    @FXML
    private void handleForgotPassword() {
        Platform.runLater(() -> switchScene("/fxml/auth/forgot-password.fxml", "Mot de passe oublié - StudySprint"));
    }

    private boolean validate() {
        boolean isValid = true;
        emailError.setVisible(false);
        passwordError.setVisible(false);

        if (emailField.getText().isEmpty()) {
            emailError.setText("L'email est requis.");
            emailError.setVisible(true);
            isValid = false;
        }

        if (passwordField.getText().isEmpty()) {
            passwordError.setText("Le mot de passe est requis.");
            passwordError.setVisible(true);
            isValid = false;
        }

        return isValid;
    }

    private void loadMainApp(Utilisateur user) {
        if ("ROLE_ADMIN".equals(user.getRole())) {
            switchScene("/fxml/utilisateurs/main-admin-layout.fxml", "Tableau de Bord - StudySprint");
        } else {
            switchScene("/fxml/auth/profile.fxml", "Mon Profil - StudySprint");
        }
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
