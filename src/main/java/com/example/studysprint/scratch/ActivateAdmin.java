package com.example.studysprint.scratch;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;

import java.util.List;

public class ActivateAdmin {
    public static void main(String[] args) {
        UtilisateurService userService = new UtilisateurService();
        List<Utilisateur> users = userService.getAll();
        
        Utilisateur admin = users.stream()
                .filter(u -> "admin@studysprint.com".equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .orElse(null);
        
        if (admin != null) {
            admin.setStatut("actif");
            userService.update(admin);
            System.out.println("COMPTE ACTIVER : admin@studysprint.com est maintenant actif.");
        } else {
            System.out.println("ERREUR : admin@studysprint.com non trouvé.");
        }
    }
}
