package com.example.studysprint.modules.utilisateurs.controllers;

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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class FaceEnrollController implements Initializable {

    @FXML private ImageView cameraView;
    @FXML private Canvas overlayCanvas;
    @FXML private Label statusLabel;
    @FXML private Button saveBtn;

    private final UtilisateurService userService = new UtilisateurService();
    private volatile BufferedImage latestFrame = null;
    private volatile double[] capturedDescriptor = null;
    private boolean descriptorCaptured = false;
    private long lastDisplayTime = 0;
    private static final long DISPLAY_INTERVAL_MS = 66; // ~15 FPS max

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        statusLabel.setText("Initialisation de la caméra...");
        drawOvalGuide();
        WebcamManager.startCapture(this::onFrameCaptured);
    }

    /** Called from WebcamManager thread — stores latest frame */
    private void onFrameCaptured(BufferedImage image) {
        latestFrame = image;

        long now = System.currentTimeMillis();
        if (now - lastDisplayTime < DISPLAY_INTERVAL_MS) return; // throttle display
        lastDisplayTime = now;

        // Display in ImageView on FX thread
        Platform.runLater(() -> {
            if (image != null) {
                cameraView.setImage(ImageConverter.toFXImage(image));

                if (!descriptorCaptured) {
                    statusLabel.setText("✅ Caméra active. Centrez votre visage et cliquez sur 'Enregistrer'.");
                    statusLabel.setStyle("-fx-text-fill: #8b5cf6;");
                    saveBtn.setDisable(false);
                }
            }
        });
    }

    /** Draws the oval face guide + corner brackets on canvas */
    private void drawOvalGuide() {
        GraphicsContext gc = overlayCanvas.getGraphicsContext2D();
        double w = overlayCanvas.getWidth();
        double h = overlayCanvas.getHeight();

        gc.clearRect(0, 0, w, h);

        // Dimming overlay
        gc.setFill(Color.rgb(0, 0, 0, 0.38));
        gc.fillRect(0, 0, w, h);

        // Oval cutout
        double ow = 220, oh = 300;
        double ox = (w - ow) / 2, oy = (h - oh) / 2;
        gc.clearRect(ox, oy, ow, oh); // won't make real oval but simple rect guide

        // Oval border
        gc.setStroke(Color.rgb(139, 92, 246));
        gc.setLineWidth(3);
        gc.strokeOval(ox, oy, ow, oh);

        // Corner brackets
        gc.setStroke(Color.rgb(139, 92, 246));
        gc.setLineWidth(4);
        int cs = 30;
        // TL
        gc.strokeLine(15, 15, 15 + cs, 15);
        gc.strokeLine(15, 15, 15, 15 + cs);
        // TR
        gc.strokeLine(w - 15 - cs, 15, w - 15, 15);
        gc.strokeLine(w - 15, 15, w - 15, 15 + cs);
        // BL
        gc.strokeLine(15, h - 15, 15 + cs, h - 15);
        gc.strokeLine(15, h - 15 - cs, 15, h - 15);
        // BR
        gc.strokeLine(w - 15 - cs, h - 15, w - 15, h - 15);
        gc.strokeLine(w - 15, h - 15 - cs, w - 15, h - 15);
    }

    @FXML
    private void handleSave() {
        BufferedImage frame = latestFrame;
        if (frame == null) {
            statusLabel.setText("❌ Aucune image disponible. Vérifiez la caméra.");
            return;
        }

        statusLabel.setText("🔄 Calcul de l'empreinte faciale...");
        saveBtn.setDisable(true);

        new Thread(() -> {
            try {
                // Average descriptors over 5 consecutive frames for robustness
                final int SAMPLE_COUNT = 5;
                double[] averaged = null;

                for (int i = 0; i < SAMPLE_COUNT; i++) {
                    BufferedImage f = latestFrame;
                    if (f == null) throw new Exception("Aucune image disponible.");
                    double[] desc = FaceDescriptorUtil.computeDescriptor(f);
                    if (averaged == null) {
                        averaged = desc.clone();
                    } else {
                        for (int j = 0; j < averaged.length; j++) averaged[j] += desc[j];
                    }
                    Thread.sleep(100); // wait 100ms between samples
                }
                // Divide by count to get mean
                for (int j = 0; j < averaged.length; j++) averaged[j] /= SAMPLE_COUNT;

                String descriptorJson = FaceDescriptorUtil.toJson(averaged);

                Utilisateur currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    currentUser.setFaceDescriptor(descriptorJson);
                    userService.update(currentUser);
                    SessionManager.getInstance().setCurrentUser(currentUser);

                    Platform.runLater(() -> {
                        descriptorCaptured = true;
                        statusLabel.setText("✅ Empreinte enregistrée avec succès !");
                        statusLabel.setStyle("-fx-text-fill: #22c55e;");
                        WebcamManager.stopCapture();

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Succès");
                        alert.setHeaderText(null);
                        alert.setContentText("Votre Face ID a été enregistré ! Vous pouvez maintenant vous connecter par reconnaissance faciale.");
                        alert.showAndWait();
                        navigateToAdmin();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("❌ Erreur : " + e.getMessage());
                    saveBtn.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        WebcamManager.stopCapture();
        navigateToAdmin();
    }

    private void navigateToAdmin() {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        String target = (user != null && "ROLE_ADMIN".equals(user.getRole()))
                ? "/fxml/utilisateurs/main-admin-layout.fxml"
                : "/fxml/auth/profile.fxml";
        String title = (user != null && "ROLE_ADMIN".equals(user.getRole()))
                ? "Tableau de Bord - StudySprint"
                : "Mon Profil - StudySprint";
        try {
            Parent root = FXMLLoader.load(getClass().getResource(target));
            Stage stage = (Stage) cameraView.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
