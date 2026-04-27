package com.example.studysprint;

import com.example.studysprint.api.ApiServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {
        // Démarrer l'API REST
        new Thread(() -> {
            try {
                ApiServer.start();
                System.out.println("✅ API REST démarrée sur http://localhost:4567");
            } catch (Exception e) {
                System.err.println("❌ Erreur API: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        // Charger l'interface avec gestion d'erreur
        try {
            System.out.println("📂 Chargement de MatiereListView.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matieres/MatiereListView.fxml"));
            Scene scene = new Scene(loader.load());
            stage.setTitle("StudySprint - Matières");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            System.out.println("✅ Interface affichée.");
        } catch (Exception e) {
            System.err.println("❌ ERREUR FATALE au chargement de l'interface :");
            e.printStackTrace();
            // Afficher une boîte de dialogue d'erreur
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erreur de démarrage");
            alert.setHeaderText("Impossible de charger l'interface");
            alert.setContentText(e.getClass().getSimpleName() + " : " + e.getMessage());
            alert.showAndWait();
            // Fermeture propre
            stage.close();
        }
    }

    @Override
    public void stop() throws Exception {
        // Arrêter le serveur Spark proprement
        spark.Spark.stop();
        // S'assurer que tous les threads restants (y compris Jetty) sont tués
        System.exit(0);
    }
}