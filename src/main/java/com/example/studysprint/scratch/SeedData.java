package com.example.studysprint.scratch;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import org.mindrot.jbcrypt.BCrypt;
import java.time.LocalDateTime;

public class SeedData {
    public static void main(String[] args) {
        UtilisateurService userService = new UtilisateurService();
        
        try {
            String hashed = BCrypt.hashpw("password123", BCrypt.gensalt());

            // 1. Plus d'étudiants (15-18)
            createStudent(userService, "Sami", "Ben Ali", "sami@test.com", hashed, 17, "TN", "Lycée Pilote");
            createStudent(userService, "Ines", "Gharbi", "ines@test.com", hashed, 18, "TN", "Lycée Carnot");
            
            // 2. Plus d'étudiants (23-26)
            createStudent(userService, "Thomas", "Dupont", "thomas@test.com", hashed, 24, "FR", "Sorbonne");
            createStudent(userService, "Sarah", "Miller", "sarah@test.com", hashed, 25, "US", "MIT");
            
            // 3. Plus de profs (Expertise 10+)
            createProf(userService, "Dr. Ahmed", "Mansour", "mansour@test.com", hashed, 45, "TN", "ENIT", 15);
            createProf(userService, "Elena", "Rossi", "elena@test.com", hashed, 50, "IT", "Uni Roma", 22);

            // 4. Diversité pays
            createStudent(userService, "Yuki", "Tanaka", "yuki@test.com", hashed, 20, "JP", "Tokyo Uni");
            createStudent(userService, "Omar", "Hassan", "omar@test.com", hashed, 22, "EG", "Cairo Uni");

            System.out.println("SEED SUCCESS: 8 new users added.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createStudent(UtilisateurService service, String nom, String prenom, String email, String pwd, int age, String pays, String univ) {
        Utilisateur u = new Utilisateur();
        u.setNom(nom); u.setPrenom(prenom); u.setEmail(email); u.setMotDePasse(pwd);
        u.setRole("ROLE_STUDENT"); u.setAge(age); u.setPays(pays); u.setEtablissement(univ);
        u.setStatut("actif"); u.setDateInscription(LocalDateTime.now().minusDays((int)(Math.random()*100)));
        service.add(u);
    }

    private static void createProf(UtilisateurService service, String nom, String prenom, String email, String pwd, int age, String pays, String univ, int exp) {
        Utilisateur u = new Utilisateur();
        u.setNom(nom); u.setPrenom(prenom); u.setEmail(email); u.setMotDePasse(pwd);
        u.setRole("ROLE_PROFESSOR"); u.setAge(age); u.setPays(pays); u.setEtablissement(univ);
        u.setAnneesExperience(exp);
        u.setStatut("actif"); u.setDateInscription(LocalDateTime.now().minusDays((int)(Math.random()*100)));
        service.add(u);
    }
}
