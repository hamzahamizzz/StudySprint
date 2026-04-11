package com.example.studysprint.scratch;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.utils.JpaUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.mindrot.jbcrypt.BCrypt;

public class AdminCreator {
    public static void main(String[] args) {
        String email = "admin@studysprint.com";
        String password = "adminpassword123";

        EntityManager em = JpaUtils.getEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            
            // Delete if exists to recreate
            em.createQuery("DELETE FROM Utilisateur u WHERE u.email = :email")
                    .setParameter("email", email)
                    .executeUpdate();

            if (false) { // Skip existence check since we just deleted it
                System.out.println("Admin account already exists.");
            } else {
                Utilisateur admin = new Utilisateur();
                admin.setNom("Admin");
                admin.setPrenom("StudySprint");
                admin.setEmail(email);
                String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
                admin.setMotDePasse(hashed);
                admin.setRole("ROLE_ADMIN");
                admin.setDiscr("admin");
                admin.setStatut("actif");
                
                em.persist(admin);
                System.out.println("Admin account created successfully!");
                System.out.println("Email: " + email);
                System.out.println("Password: " + password);
                System.out.println("Hash: " + hashed);
                System.out.println("Hash Length: " + hashed.length());
            }
            
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
            System.exit(0);
        }
    }
}
