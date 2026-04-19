package com.example.studysprint.modules.utilisateurs.controllers;

import com.example.studysprint.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainAdminController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button usersBtn, statsBtn, requestsBtn;
    @FXML private StackPane badgeContainer;
    @FXML private Label requestBadge;

    private final com.example.studysprint.modules.utilisateurs.services.ReactivationService reactivationService = new com.example.studysprint.modules.utilisateurs.services.ReactivationService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Load the users list by default
        showUsersList();
        refreshBadge();
    }

    public void refreshBadge() {
        int count = reactivationService.getPendingCount();
        if (count > 0) {
            requestBadge.setText(String.valueOf(count));
            badgeContainer.setVisible(true);
        } else {
            badgeContainer.setVisible(false);
        }
    }

    @FXML
    private void showUsersList() {
        setActiveButton(usersBtn);
        loadView("/fxml/utilisateurs/users-list.fxml");
    }

    @FXML
    private void showStats() {
        setActiveButton(statsBtn);
        loadView("/fxml/utilisateurs/users-stats.fxml");
    }

    @FXML
    private void showReactivationRequests() {
        setActiveButton(requestsBtn);
        loadView("/fxml/utilisateurs/reactivation-requests.fxml");
    }

    @FXML
    private void showMyProfile() {
        loadView("/fxml/utilisateurs/admin-profile.fxml");
        resetButtons(); // Neither tab is active
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auth/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion - StudySprint");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxmlPath) {
        try {
            System.out.println("DEBUG: Loading view " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            
            // If we're loading the requests view, give it a reference to this controller to refresh the badge
            if (fxmlPath.contains("reactivation-requests")) {
                Object controller = loader.getController();
                if (controller instanceof ReactivationRequestsController) {
                    ((ReactivationRequestsController) controller).setMainAdminController(this);
                }
            }

            contentArea.getChildren().setAll(view);
            System.out.println("DEBUG: View loaded successfully.");
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR loading view: " + fxmlPath);
            e.printStackTrace();
            
            // Show alert to user for diagnostic
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erreur de Navigation");
            alert.setHeaderText("Impossible de charger la vue : " + fxmlPath);
            alert.setContentText(e.getMessage() != null ? e.getMessage() : e.toString());
            alert.showAndWait();
        }
    }

    private void setActiveButton(Button activeBtn) {
        resetButtons();
        activeBtn.setStyle("-fx-background-color: #6c5ce7; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5; -fx-alignment: CENTER_LEFT;");
    }

    private void resetButtons() {
        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";
        usersBtn.setStyle(baseStyle);
        statsBtn.setStyle(baseStyle);
        requestsBtn.setStyle(baseStyle);
    }
}
