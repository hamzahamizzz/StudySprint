package com.example.studysprint.modules.auth.controllers;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import com.example.studysprint.utils.FaceDescriptorUtil;
import com.example.studysprint.utils.ImageConverter;
import com.example.studysprint.utils.SessionManager;
import com.example.studysprint.utils.WebcamManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class FaceLoginController implements Initializable {

    @FXML private ImageView cameraView;
    @FXML private Canvas overlayCanvas;
    @FXML private Label statusLabel;

    private final UtilisateurService userService = new UtilisateurService();
    private volatile boolean searching = false;
    private volatile boolean found = false;
    private long lastDisplayTime = 0;
    private static final long DISPLAY_INTERVAL_MS = 66; // ~15 FPS max

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        statusLabel.setText("Initialisation de la caméra...");
        drawOverlay();
        WebcamManager.startCapture(this::onFrameCaptured);
    }

    private void onFrameCaptured(BufferedImage image) {
        if (image == null || found) return;

        long now = System.currentTimeMillis();
        boolean shouldDisplay = (now - lastDisplayTime) >= DISPLAY_INTERVAL_MS;
        if (shouldDisplay) {
            lastDisplayTime = now;
            Platform.runLater(() -> {
                cameraView.setImage(ImageConverter.toFXImage(image));
                if (!searching) statusLabel.setText("Scan en cours... Regardez la caméra.");
            });
        }

        // Try recognition every ~1 second (skip frames while processing)
        if (!searching) {
            searching = true;
            new Thread(() -> {
                try {
                    double[] descriptor = FaceDescriptorUtil.computeDescriptor(image);
                    Utilisateur match = findMatch(descriptor);

                    Platform.runLater(() -> {
                        if (match != null) {
                            found = true;
                            WebcamManager.stopCapture();
                            statusLabel.setText("✅ Bienvenue, " + match.getFullName() + " !");
                            statusLabel.setStyle("-fx-text-fill: #22c55e;");
                            new Thread(() -> {
                                try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                                Platform.runLater(() -> handleSuccessfulLogin(match));
                            }).start();
                        } else {
                            statusLabel.setText("Visage non reconnu. Continuez...");
                            searching = false; // allow next attempt
                        }
                    });
                } catch (Exception e) {
                    searching = false;
                }
            }).start();
        }
    }

    private Utilisateur findMatch(double[] scanned) {
        List<Utilisateur> users = userService.getAll();
        for (Utilisateur u : users) {
            String stored = u.getFaceDescriptor();
            if (stored == null || stored.isBlank()) continue;
            try {
                double[] storedDesc = FaceDescriptorUtil.fromJson(stored);
                if (FaceDescriptorUtil.isSamePerson(scanned, storedDesc)) {
                    System.out.println("[FaceLogin] Match: " + u.getEmail()
                            + " dist=" + FaceDescriptorUtil.distance(scanned, storedDesc));
                    return u;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    @FXML
    private void handleCancel() {
        WebcamManager.stopCapture();
        switchScene("/fxml/auth/login.fxml", "Connexion - StudySprint");
    }

    private void handleSuccessfulLogin(Utilisateur user) {
        SessionManager.getInstance().setCurrentUser(user);
        String fxmlPath = "ROLE_ADMIN".equals(user.getRole())
                ? "/fxml/utilisateurs/main-admin-layout.fxml"
                : "/fxml/auth/profile.fxml";
        switchScene(fxmlPath, "StudySprint");
    }

    private void switchScene(String path, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) cameraView.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void drawOverlay() {
        GraphicsContext gc = overlayCanvas.getGraphicsContext2D();
        double w = overlayCanvas.getWidth(), h = overlayCanvas.getHeight();
        gc.setFill(Color.rgb(0, 0, 0, 0.35));
        gc.fillRect(0, 0, w, h);
        double ow = 220, oh = 300, ox = (w - ow) / 2, oy = (h - oh) / 2;
        gc.clearRect(ox, oy, ow, oh);
        gc.setStroke(Color.rgb(34, 197, 94));
        gc.setLineWidth(3);
        gc.strokeOval(ox, oy, ow, oh);
        // Corner brackets
        gc.setStroke(Color.rgb(34, 197, 94));
        gc.setLineWidth(4);
        int cs = 30;
        gc.strokeLine(15, 15, 15 + cs, 15); gc.strokeLine(15, 15, 15, 15 + cs);
        gc.strokeLine(w - 15 - cs, 15, w - 15, 15); gc.strokeLine(w - 15, 15, w - 15, 15 + cs);
        gc.strokeLine(15, h - 15, 15 + cs, h - 15); gc.strokeLine(15, h - 15 - cs, 15, h - 15);
        gc.strokeLine(w - 15 - cs, h - 15, w - 15, h - 15); gc.strokeLine(w - 15, h - 15 - cs, w - 15, h - 15);
    }
}
