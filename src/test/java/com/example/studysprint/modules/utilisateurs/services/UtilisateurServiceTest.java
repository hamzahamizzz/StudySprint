package com.example.studysprint.modules.utilisateurs.services;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UtilisateurServiceTest {
    private static UtilisateurService service;
    private static int idTest;

    @BeforeAll
    static void setup() {
        service = new UtilisateurService();
    }

    @Test
    @Order(1)
    void testAddUtilisateur() {
        Utilisateur u = new Utilisateur();
        u.setNom("TestNom");
        u.setPrenom("TestPrenom");
        u.setEmail("test" + System.currentTimeMillis() + "@gmail.com");
        u.setMotDePasse("Pass123!");
        u.setRole("ROLE_STUDENT");
        u.setStatut("actif");
        u.setDiscr("student");
        
        service.add(u);
        idTest = u.getId();
        
        List<Utilisateur> list = service.getAll();
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(user -> user.getNom().equals("TestNom")));
    }

    @Test
    @Order(2)
    void testUpdateUtilisateur() {
        Utilisateur u = service.getAll().stream()
                .filter(user -> user.getId() == idTest)
                .findFirst()
                .orElse(null);
        
        assertNotNull(u);
        u.setNom("NomModifie");
        service.update(u);
        
        List<Utilisateur> list = service.getAll();
        assertTrue(list.stream().anyMatch(user -> user.getNom().equals("NomModifie")));
    }

    @Test
    @Order(3)
    void testDeleteUtilisateur() {
        service.delete(idTest);
        List<Utilisateur> list = service.getAll();
        assertFalse(list.stream().anyMatch(user -> user.getId() == idTest));
    }

    @AfterEach
    void cleanUp() {
        // Optionnel : Nettoyage après chaque test si nécessaire
        // Pour l'instant on laisse les tests s'enchaîner
    }
}
