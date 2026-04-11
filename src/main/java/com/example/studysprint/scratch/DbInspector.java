package com.example.studysprint.scratch;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.utils.JpaUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;

public class DbInspector {
    public static void main(String[] args) {
        EntityManager em = JpaUtils.getEntityManager();
        try {
            System.out.println("Checking database schema for table 'user'...");
            
            // Check column metadata (MySQL specific)
            Query query = em.createNativeQuery("SHOW COLUMNS FROM user LIKE 'mot_de_passe'");
            Object[] result = (Object[]) query.getSingleResult();
            if (result != null) {
                System.out.println("Column: " + result[0]);
                System.out.println("Type: " + result[1]);
            }

            System.out.println("\nChecking existing users password lengths:");
            List<Object[]> users = em.createNativeQuery("SELECT email, LENGTH(mot_de_passe) FROM user").getResultList();
            for (Object[] row : users) {
                System.out.println("Email: " + row[0] + " | Password Length: " + row[1]);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
            System.exit(0);
        }
    }
}
