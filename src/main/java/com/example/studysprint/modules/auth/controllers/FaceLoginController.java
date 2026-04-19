package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.AppNavigator;
import com.example.studysprint.utils.SessionManager;
import com.example.studysprint.utils.WebcamManager;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.ResourceBundle;

public class FaceLoginController implements Initializable {

    @FXML private WebView webView;
    @FXML private Label statusLabel;

    private WebEngine webEngine;
    private final UtilisateurService userService = new UtilisateurService();
    private final Gson gson = new Gson();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        webEngine = webView.getEngine();
        setupLogging();
        
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", new FaceConnector());
                statusLabel.setText("Modèles IA chargés. Initialisation Caméra...");
                
                WebcamManager.startCapture(this::onFrameCaptured);
            }
        });

        webView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) WebcamManager.stopCapture();
        });

        URL fxmlUrl = getClass().getResource("/html/face-recognition.html");
        if (fxmlUrl != null) {
            webEngine.load(fxmlUrl.toExternalForm());
        }
    }

    private volatile String latestBase64Frame = null;

    private void onFrameCaptured(java.awt.image.BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            latestBase64Frame = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {}
    }

    public class FaceConnector {
        public String getLatestFrame() {
            return latestBase64Frame;
        }

        public void onFaceDetected(String descriptorJson) {
            Platform.runLater(() -> {
                statusLabel.setText("Visage détecté. Vérification en cours...");
                double[] scannedDescriptor = gson.fromJson(descriptorJson, double[].class);
                Utilisateur matchedUser = findMatch(scannedDescriptor);
                
                if (matchedUser != null) {
                    WebcamManager.stopCapture();
                    statusLabel.setText("Bienvenue, " + matchedUser.getFullName() + " !");
                    statusLabel.getStyleClass().removeAll("status-success", "status-error");
                    statusLabel.getStyleClass().add("status-success");
                    
                    new Thread(() -> {
                        try { Thread.sleep(1000); } catch (InterruptedException e) {}
                        Platform.runLater(() -> handleSuccessfulLogin(matchedUser));
                    }).start();
                } else {
                    statusLabel.setText("Utilisateur non reconnu. Nouvelle tentative...");
                    statusLabel.getStyleClass().removeAll("status-success", "status-error");
                    statusLabel.getStyleClass().add("status-error");
                    webEngine.executeScript("window.successLocked = false;");
                }
            });
        }
    }

    @FXML
    private void handleCancel() {
        WebcamManager.stopCapture();
        switchScene("/fxml/auth/login.fxml", "Connexion - StudySprint");
    }

    private Utilisateur findMatch(double[] scannedDescriptor) {
        List<Utilisateur> users = userService.getAll();
        Utilisateur bestMatch = null;
        double minDistance = 1.0;

        for (Utilisateur user : users) {
            String storedJson = user.getFaceDescriptor();
            if (storedJson == null || storedJson.isEmpty()) continue;
            try {
                double[] storedDescriptor = gson.fromJson(storedJson, double[].class);
                double distance = calculateEuclideanDistance(scannedDescriptor, storedDescriptor);
                if (distance < 0.6 && distance < minDistance) {
                    minDistance = distance;
                    bestMatch = user;
                }
            } catch (Exception e) {}
        }
        return bestMatch;
    }

    private double calculateEuclideanDistance(double[] vec1, double[] vec2) {
        if (vec1.length != vec2.length) return 2.0;
        double sum = 0;
        for (int i = 0; i < vec1.length; i++) sum += Math.pow(vec1[i] - vec2[i], 2);
        return Math.sqrt(sum);
    }

    private void handleSuccessfulLogin(Utilisateur user) {
        SessionManager.getInstance().setCurrentUser(user);
        Stage stage = (Stage) webView.getScene().getWindow();
        AppNavigator.openDefaultForUser(stage, user, getClass());
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

    private void switchScene(String fxmlPath, String title) {
        Stage stage = (Stage) webView.getScene().getWindow();
        AppNavigator.switchTo(stage, fxmlPath, title, getClass());
    }
}
