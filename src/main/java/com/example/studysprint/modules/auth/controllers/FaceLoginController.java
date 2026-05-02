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
    private volatile BufferedImage latestFrame = null;
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
        latestFrame = image; // always keep latest frame

        long now = System.currentTimeMillis();
        boolean shouldDisplay = (now - lastDisplayTime) >= DISPLAY_INTERVAL_MS;
        if (shouldDisplay) {
            lastDisplayTime = now;
            Platform.runLater(() -> {
                cameraView.setImage(ImageConverter.toFXImage(image));
                if (!searching) statusLabel.setText("Scan en cours... Regardez la caméra.");
            });
        }

        // Try recognition (skip frames while already processing)
        if (!searching) {
            searching = true;
            new Thread(() -> {
                try {
                    // Average 3 frames (200ms apart) for a stable descriptor
                    final int SAMPLES = 3;
                    double[] averaged = null;
                    for (int i = 0; i < SAMPLES; i++) {
                        BufferedImage f = latestFrame;
                        if (f == null) { Thread.sleep(200); continue; }
                        double[] desc = FaceDescriptorUtil.computeDescriptor(f);
                        if (averaged == null) {
                            averaged = desc.clone();
                        } else {
                            for (int j = 0; j < averaged.length; j++) averaged[j] += desc[j];
                        }
                        if (i < SAMPLES - 1) Thread.sleep(200);
                    }
                    if (averaged != null)
                        for (int j = 0; j < averaged.length; j++) averaged[j] /= SAMPLES;

                    Utilisateur match = (averaged != null) ? findMatch(averaged) : null;

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
                            searching = false;
                        }
                    });
                } catch (Exception e) {
                    searching = false;
                }
            }).start();
        }
    }

    /**
     * Best-match strategy calibrated on real measured distances.
     * Real distances observed: same person ~0.7-1.5, different person ~1.5-3.0+
     * Threshold: 3.0 (accepts same person, rejects strangers)
     * Gap: 0.5 minimum separation between best and second-best
     */
    private static final double STRICT_THRESHOLD = 3.0;

    private Utilisateur findMatch(double[] scanned) {
        List<Utilisateur> users = userService.getAll();

        Utilisateur bestUser = null;
        double bestDist = Double.MAX_VALUE;
        double secondBestDist = Double.MAX_VALUE;

        System.out.println("[FaceLogin] ── Scanning " + users.size() + " users ──");

        for (Utilisateur u : users) {
            String stored = u.getFaceDescriptor();
            if (stored == null || stored.isBlank()) continue;
            try {
                double[] storedDesc = FaceDescriptorUtil.fromJson(stored);
                double dist = FaceDescriptorUtil.distance(scanned, storedDesc);
                // Skip descriptors that are corrupted (infinite distance = wrong length)
                if (Double.isInfinite(dist) || Double.isNaN(dist)) {
                    System.out.println("[FaceLogin] ⚠️ Skipping corrupt descriptor for " + u.getEmail());
                    continue;
                }
                System.out.printf("[FaceLogin] %s → dist=%.4f%n", u.getEmail(), dist);

                if (dist < bestDist) {
                    secondBestDist = bestDist;
                    bestDist = dist;
                    bestUser = u;
                } else if (dist < secondBestDist) {
                    secondBestDist = dist;
                }
            } catch (Exception e) {
                System.out.println("[FaceLogin] Error reading descriptor for " + u.getEmail());
            }
        }

        System.out.printf("[FaceLogin] Best dist=%.4f  SecondBest=%.4f  Threshold=%.1f%n",
                bestDist, secondBestDist, STRICT_THRESHOLD);

        // Reject if best distance exceeds strict threshold
        if (bestUser == null || bestDist >= STRICT_THRESHOLD) {
            System.out.println("[FaceLogin] ❌ No match (dist too high)");
            return null;
        }

        // If multiple enrolled users, ensure best is clearly better than second
        if (secondBestDist < Double.MAX_VALUE) {
            double gap = secondBestDist - bestDist;
            if (gap < 0.5) { // gap must be at least 0.5 (based on real measured distances)
                System.out.printf("[FaceLogin] ❌ Ambiguous match (gap=%.4f too small)%n", gap);
                return null;
            }
        }

        System.out.println("[FaceLogin] ✅ Match accepted: " + bestUser.getEmail());
        return bestUser;
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
