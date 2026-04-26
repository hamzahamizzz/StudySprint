package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.auth.services.GoogleAuthService;
import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.SessionManager;
import com.google.api.services.oauth2.model.Userinfo;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label emailError, passwordError;
    @FXML private Button loginButton;

    private final UtilisateurService userService = new UtilisateurService();

    @FXML
    private void handleLogin() {
        if (validate()) {
            String email = emailField.getText().trim();
            String password = passwordField.getText().trim();

            // Prevent double clicks and show visual feedback
            loginButton.setDisable(true);
            loginButton.setText("Connexion en cours...");
            passwordError.setVisible(false);

            // Run authentication in background thread to prevent UI freeze
            new Thread(() -> {
                try {
                    Utilisateur user = userService.authenticate(email, password);

                    javafx.application.Platform.runLater(() -> {
                        loginButton.setDisable(false);
                        loginButton.setText("Se connecter");

                        if (user != null) {
                            String status = user.getStatut();
                            if (status == null || status.isEmpty() || "actif".equalsIgnoreCase(status)) {
                                SessionManager.getInstance().setCurrentUser(user);
                                System.out.println("Login successful for: " + user.getEmail());
                                loadMainApp(user);
                            } else if ("desactif".equalsIgnoreCase(status) || "inactif".equalsIgnoreCase(status)) {
                                SessionManager.getInstance().setCurrentUser(user);
                                switchScene("/fxml/auth/account-deactivated.fxml", "Compte Désactivé - StudySprint");
                            } else {
                                passwordError.setText("Compte " + status + ". Contactez un administrateur.");
                                passwordError.setVisible(true);
                            }
                        } else {
                            passwordError.setText("Identifiants incorrects ou compte inexistant.");
                            passwordError.setVisible(true);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        loginButton.setDisable(false);
                        loginButton.setText("Se connecter");
                        passwordError.setText("Erreur de connexion : Vérifiez votre base de données.");
                        passwordError.setVisible(true);
                    });
                }
            }).start();
        }
    }

    @FXML
    private void handleGoToRegister() {
        switchScene("/fxml/auth/register.fxml", "Inscription - StudySprint");
    }

    @FXML
    private void handleForgotPassword() {
        switchScene("/fxml/auth/forgot-password.fxml", "Mot de passe oublié - StudySprint");
    }

    @FXML
    private void handleFaceLogin() {
        switchScene("/fxml/auth/face-login.fxml", "Connexion par Reconnaissance Faciale - StudySprint");
    }

    @FXML
    private void handleGoogleLogin() {
        loginButton.setDisable(true);
        String originalText = loginButton.getText();
        loginButton.setText("Connexion Google...");
        passwordError.setVisible(false);

        new Thread(() -> {
            try {
                Userinfo info = GoogleAuthService.authenticate();
                
                Platform.runLater(() -> {
                    if (info != null && info.getEmail() != null) {
                        Utilisateur user = userService.findByEmail(info.getEmail());
                        if (user != null) {
                            SessionManager.getInstance().setCurrentUser(user);
                            System.out.println("Google login successful for: " + user.getEmail());
                            loadMainApp(user);
                        } else {
                            loginButton.setDisable(false);
                            loginButton.setText(originalText);
                            passwordError.setText("Aucun compte StudySprint lié à " + info.getEmail());
                            passwordError.setVisible(true);
                        }
                    } else {
                        loginButton.setDisable(false);
                        loginButton.setText(originalText);
                        passwordError.setText("Échec de la récupération du profil Google.");
                        passwordError.setVisible(true);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText(originalText);
                    passwordError.setText("Erreur Google Sign-In : " + e.getMessage());
                    passwordError.setVisible(true);
                });
            }
        }).start();
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
}
