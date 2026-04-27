package com.example.studysprint;

import com.example.studysprint.api.ApiServer;
import com.example.studysprint.utils.DatabaseCheck;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        System.out.println("\n=== 🚀 DÉMARRAGE DE STUDYSPRINT ===\n");
        
        // Ajouter un gestionnaire global d'exceptions non attrapées
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("\n❌ EXCEPTION NON ATTRAPÉE DANS LE THREAD: " + thread.getName());
            System.err.println("   Erreur: " + throwable.getMessage());
            System.err.println("   Classe: " + throwable.getClass().getName());
            throwable.printStackTrace();
        });
        
        try {
            // Vérifier la connexion à la base de données en premier
            System.out.println("🔍 Vérification de la connexion à la base de données...");
            if (!DatabaseCheck.checkConnection()) {
                System.err.println("\n❌ ERREUR CRITIQUE: Connection à MySQL impossible!");
                System.err.println("   L'application ne peut pas démarrer.");
                System.exit(1);
                return;
            }

            // Démarrer l'API dans un thread séparé
            Thread apiThread = new Thread(() -> {
                try {
                    ApiServer.start();
                    System.out.println("✅ API REST démarrée sur http://localhost:4567");
                } catch (Exception e) {
                    System.err.println("❌ Erreur démarrage API: " + e.getMessage());
                    e.printStackTrace();
                }
            }, "APIThread");
            apiThread.setDaemon(false);
            apiThread.start();

            System.out.println("📂 Chargement de l'interface MatiereListView.fxml...");
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/fxml/matieres/MatiereListView.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            System.out.println("✅ Interface chargée avec succès!");
            
            stage.setTitle("StudySprint - Matières");
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("✅ Application démarrée avec succès!\n");

            stage.setOnCloseRequest(e -> {
                System.out.println("🔄 Fermeture de l'application...");
                System.exit(0);
            });
        } catch (IOException e) {
            System.err.println("\n❌ ERREUR CRITIQUE: Impossible de charger l'interface FXML");
            System.err.println("   Fichier: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("\n❌ ERREUR CRITIQUE au démarrage!");
            System.err.println("   Erreur: " + e.getMessage());
            System.err.println("   Classe: " + e.getClass().getName());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

