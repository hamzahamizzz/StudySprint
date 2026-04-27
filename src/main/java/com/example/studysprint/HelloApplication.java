package com.example.studysprint;

import com.example.studysprint.api.ApiServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    public void start(Stage stage) throws IOException {
        // Démarrer l'API dans un thread séparé
        Thread apiThread = new Thread(() -> {
            try {
                ApiServer.start();
                System.out.println("✅ API REST démarrée sur http://localhost:4567");
            } catch (Exception e) {
                System.err.println("❌ Erreur démarrage API: " + e.getMessage());
                e.printStackTrace();
            }
        });
        apiThread.setDaemon(false); // Garder le thread vivant
        apiThread.start();

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/fxml/matieres/MatiereListView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("StudySprint - Matières");
        stage.setMaximized(true);
        stage.show();

        // S'assurer que l'application reste ouverte
        stage.setOnCloseRequest(e -> {
            System.out.println("🔄 Fermeture de l'application...");
            // Ici on pourrait arrêter proprement l'API si nécessaire
        });
    }
}
