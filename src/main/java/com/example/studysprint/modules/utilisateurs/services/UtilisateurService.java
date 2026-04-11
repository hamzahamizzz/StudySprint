package com.example.studysprint.modules.utilisateurs.services;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.utils.JpaUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.mindrot.jbcrypt.BCrypt;
import java.util.List;
import java.util.stream.Collectors;

public class UtilisateurService {

    public List<Utilisateur> getAll() {
        EntityManager em = JpaUtils.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM Utilisateur u", Utilisateur.class).getResultList();
        } finally {
            em.close();
        }
    }

    public void add(Utilisateur u) {
        EntityManager em = JpaUtils.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(u);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void update(Utilisateur u) {
        EntityManager em = JpaUtils.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(u);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(int id) {
        EntityManager em = JpaUtils.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Utilisateur u = em.find(Utilisateur.class, id);
            if (u != null) {
                em.remove(u);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    // Security Methods
    public void register(Utilisateur u, String plainPassword) {
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        u.setMotDePasse(hashedPassword);
        add(u);
    }

    public Utilisateur authenticate(String email, String password) {
        EntityManager em = JpaUtils.getEntityManager();
        try {
            System.out.println("Authenticating: " + email);
            Utilisateur user = em.createQuery("SELECT u FROM Utilisateur u WHERE LOWER(u.email) = LOWER(:email)", Utilisateur.class)
                    .setParameter("email", email)
                    .getSingleResult();
            
            System.out.println("User found: " + user.getEmail() + " | Status: " + user.getStatut());
            
            if (BCrypt.checkpw(password, user.getMotDePasse())) {
                System.out.println("Password match success.");
                return user;
            } else {
                System.out.println("Password mismatch.");
            }
        } catch (jakarta.persistence.NoResultException e) {
            System.out.println("No user found with email: " + email);
        } catch (Exception e) {
            System.out.println("Authentication error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            em.close();
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════
    // FILTRES VIA JAVA STREAM (getAll() unique + Stream)
    // ═══════════════════════════════════════════════════════

    /** Recherche multi-champs : nom, prénom, email */
    public List<Utilisateur> search(String query) {
        String lowerQuery = query == null ? "" : query.toLowerCase();
        return getAll().stream()
                .filter(u -> safe(u.getNom()).contains(lowerQuery)
                          || safe(u.getPrenom()).contains(lowerQuery)
                          || safe(u.getEmail()).contains(lowerQuery))
                .collect(Collectors.toList());
    }

    /** Filtre par rôle ex: "ROLE_STUDENT", "ROLE_PROFESSOR", "ROLE_ADMIN" */
    public List<Utilisateur> getByRole(String role) {
        return getAll().stream()
                .filter(u -> role != null && role.equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toList());
    }

    /** Retourne uniquement les comptes actifs */
    public List<Utilisateur> getActifs() {
        return getAll().stream()
                .filter(u -> u.getStatut() == null
                          || u.getStatut().isEmpty()
                          || "actif".equalsIgnoreCase(u.getStatut()))
                .collect(Collectors.toList());
    }

    /** Filtre par pays (code ISO) */
    public List<Utilisateur> getByPays(String pays) {
        return getAll().stream()
                .filter(u -> pays != null && pays.equalsIgnoreCase(u.getPays()))
                .collect(Collectors.toList());
    }

    // Helper null-safe pour les comparaisons Stream
    private String safe(String val) {
        return val == null ? "" : val.toLowerCase();
    }
}
