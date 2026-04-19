package com.example.studysprint.modules.utilisateurs.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.AppNavigator;
import com.example.studysprint.utils.SessionManager;
import com.example.studysprint.utils.WebcamManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;

public class FaceEnrollController implements Initializable {

    @FXML private WebView webView;
    @FXML private Label statusLabel;
    @FXML private Button saveBtn;

    private WebEngine webEngine;
    private final UtilisateurService userService = new UtilisateurService();
    private String lastCapturedDescriptor = null;
    private volatile String latestBase64Frame = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        webEngine = webView.getEngine();
        setupLogging();
        
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", new FaceEnrollConnector());
                statusLabel.setText("Positionnez votre visage pour l'enregistrement.");
                
                WebcamManager.startCapture(this::onFrameCaptured);
            }
        });

        webView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) WebcamManager.stopCapture();
        });

        URL htmlUrl = getClass().getResource("/html/face-recognition.html");
        if (htmlUrl != null) {
            webEngine.load(htmlUrl.toExternalForm());
        }
    }

    private void onFrameCaptured(java.awt.image.BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            latestBase64Frame = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {}
    }

    public class FaceEnrollConnector {
        public String getLatestFrame() {
            return latestBase64Frame;
        }

        public void onFaceDetected(String descriptorJson) {
            Platform.runLater(() -> {
                WebcamManager.stopCapture();
                lastCapturedDescriptor = descriptorJson;
                saveBtn.setDisable(false);
                statusLabel.setText("Visage détecté ! Prêt pour l'enregistrement.");
                statusLabel.getStyleClass().removeAll("status-success", "status-error");
                statusLabel.getStyleClass().add("status-success");
            });
        }
    }

    @FXML
    private void handleSave() {
        if (lastCapturedDescriptor == null) return;
        Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            WebcamManager.stopCapture();
            currentUser.setFaceDescriptor(lastCapturedDescriptor);
            userService.update(currentUser);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText("Votre empreinte faciale a été enregistrée avec succès !");
            alert.showAndWait();
            
            handleCancel();
        }
    }

    @FXML
    private void handleCancel() {
        WebcamManager.stopCapture();
        Stage stage = (Stage) webView.getScene().getWindow();
        AppNavigator.switchTo(stage, AppNavigator.ADMIN_FXML, AppNavigator.ADMIN_TITLE, getClass());
    }

    private void setupLogging() {
        webEngine.setOnAlert(event -> System.out.println("JS Alert: " + event.getData()));
        // Bridge console.log to Java
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                webEngine.executeScript("console.log = function(msg) { alert(msg); };");
            }
        });
    }

}
