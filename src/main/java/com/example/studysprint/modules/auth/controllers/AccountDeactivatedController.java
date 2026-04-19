package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.ReactivationService;
import com.example.studysprint.utils.AppNavigator;
import com.example.studysprint.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AccountDeactivatedController implements Initializable {

    @FXML private Label emailLabel, statusLabel;
    @FXML private TextArea reasonArea;
    @FXML private Button submitBtn;

    private final ReactivationService reactivationService = new ReactivationService();
    private Utilisateur currentUser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            emailLabel.setText(currentUser.getEmail());
            
            if (reactivationService.hasPendingRequest(currentUser.getId())) {
                showAlreadyRequested();
            }
        }
    }

    @FXML
    private void handleSubmitRequest() {
        String reason = reasonArea.getText().trim();
        // Justification is now optional, so no empty check
        
        reactivationService.submitRequest(currentUser.getId(), reason);
        showAlreadyRequested();
        statusLabel.setText("Votre demande a été envoyée avec succès.");
        statusLabel.getStyleClass().removeAll("status-success", "status-error");
        statusLabel.getStyleClass().add("status-success");
        statusLabel.setVisible(true);
    }

    private void showAlreadyRequested() {
        submitBtn.setDisable(true);
        reasonArea.setDisable(true);
        reasonArea.setPromptText("Une demande est déjà en cours de traitement.");
    }

    @FXML
    private void handleBackToLogin() {
        SessionManager.getInstance().logout();
        Stage stage = (Stage) emailLabel.getScene().getWindow();
        AppNavigator.switchTo(stage, AppNavigator.LOGIN_FXML, AppNavigator.LOGIN_TITLE, getClass());
    }
}
