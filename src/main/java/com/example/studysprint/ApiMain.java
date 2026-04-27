package com.example.studysprint;

import com.example.studysprint.api.ApiServer;

public class ApiMain {
    public static void main(String[] args) {
        System.out.println("🚀 Démarrage de l'API StudySprint...");
        try {
            ApiServer.start();
            System.out.println("✅ API REST démarrée sur http://localhost:4567");

            // Garder l'application active
            System.out.println("📡 Appuyez sur Ctrl+C pour arrêter l'API");
            Thread.currentThread().join(); // Attendre indéfiniment

        } catch (Exception e) {
            System.err.println("❌ Erreur démarrage API: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
