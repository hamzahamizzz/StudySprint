package com.example.studysprint.scratch;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.modules.utilisateurs.services.UtilisateurService;
import java.util.List;

public class CheckStudentAges {
    public static void main(String[] args) {
        UtilisateurService service = new UtilisateurService();
        List<Utilisateur> users = service.getAll();
        
        long totalStudents = users.stream()
                .filter(u -> "ROLE_STUDENT".equalsIgnoreCase(u.getRole()))
                .count();
        
        long studentsWithAge = users.stream()
                .filter(u -> "ROLE_STUDENT".equalsIgnoreCase(u.getRole()) && u.getAge() != null)
                .count();
                
        System.out.println("Total Students: " + totalStudents);
        System.out.println("Students with Age recorded: " + studentsWithAge);
        
        if (studentsWithAge > 0) {
            System.out.println("Details of students with age and gender:");
            users.stream()
                .filter(u -> "ROLE_STUDENT".equalsIgnoreCase(u.getRole()) && u.getAge() != null)
                .forEach(u -> System.out.println("- " + u.getNom() + " " + u.getPrenom() + " (Age: " + u.getAge() + ", Sexe: " + u.getSexe() + ")"));
        }
    }
}
