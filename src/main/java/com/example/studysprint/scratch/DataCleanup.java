package com.example.studysprint.scratch;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import java.util.List;
import java.util.Random;

public class DataCleanup {
    public static void main(String[] args) {
        UtilisateurService service = new UtilisateurService();
        List<Utilisateur> users = service.getAll();
        Random random = new Random();
        
        System.out.println("Starting Data Cleanup for " + users.size() + " users...");
        int updatedCount = 0;

        for (int i = 0; i < users.size(); i++) {
            Utilisateur u = users.get(i);
            boolean modified = false;

            // 1. Check Age
            if (u.getAge() == null || u.getAge() <= 0) {
                if ("ROLE_STUDENT".equalsIgnoreCase(u.getRole())) {
                    u.setAge(18 + random.nextInt(8)); // 18-25
                } else {
                    u.setAge(30 + random.nextInt(25)); // 30-55
                }
                modified = true;
            }

            // 2. Check Sexe
            if (u.getSexe() == null || u.getSexe().isEmpty() || "null".equals(u.getSexe())) {
                // Alternate based on index to keep it balanced
                u.setSexe(i % 2 == 0 ? "M" : "F");
                modified = true;
            }

            // 3. Check Pays
            if (u.getPays() == null || u.getPays().isEmpty() || "null".equals(u.getPays())) {
                u.setPays("TN"); // Default to Tunisia
                modified = true;
            }

            // 4. Check Etablissement
            if (u.getEtablissement() == null || u.getEtablissement().isEmpty() || "null".equals(u.getEtablissement())) {
                u.setEtablissement("ESPRIT"); // Default establishment
                modified = true;
            }

            // 5. Check Prof Experience
            if ("ROLE_PROFESSOR".equalsIgnoreCase(u.getRole())) {
                if (u.getAnneesExperience() == null || u.getAnneesExperience() < 0) {
                    u.setAnneesExperience(2 + random.nextInt(15));
                    modified = true;
                }
            }

            if (modified) {
                try {
                    service.update(u);
                    updatedCount++;
                    System.out.println("Updated user: " + u.getNom() + " " + u.getPrenom() + " (ID: " + u.getId() + ")");
                } catch (Exception e) {
                    System.err.println("Failed to update user " + u.getId() + ": " + e.getMessage());
                }
            }
        }

        System.out.println("Cleanup complete. Total users updated: " + updatedCount);
    }
}
