package com.example.studysprint.modules.quizz;

import com.example.studysprint.modules.quizz.models.Flashcard;
import com.example.studysprint.modules.quizz.models.FlashcardDeck;
import com.example.studysprint.modules.quizz.services.FlashcardService;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FlashcardService.
 * Requires a running MariaDB instance with the studysprint database.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlashcardServiceTest {

    private static FlashcardService service;
    private static long deckId;
    private static long cardId;

    @BeforeAll
    static void setUp() {
        service = new FlashcardService();
    }

    // ── FlashcardDeck CRUD ─────────────────────────────────────────────

    @Test @Order(1)
    void testAddDeck() throws SQLException {
        FlashcardDeck deck = new FlashcardDeck(1L, 1L, "Test Deck JUnit");
        deck.setPublished(false);
        service.addDeck(deck);
        assertTrue(deck.getId() > 0, "L'id du deck doit être généré");
        deckId = deck.getId();
    }

    @Test @Order(2)
    void testGetDeckById() throws SQLException {
        FlashcardDeck deck = service.getDeckById(deckId);
        assertNotNull(deck, "Le deck doit exister");
        assertEquals("Test Deck JUnit", deck.getTitle());
    }

    @Test @Order(3)
    void testUpdateDeck() throws SQLException {
        FlashcardDeck deck = service.getDeckById(deckId);
        assertNotNull(deck);
        deck.setTitle("Test Deck JUnit — MAJ");
        service.updateDeck(deck);
        FlashcardDeck updated = service.getDeckById(deckId);
        assertNotNull(updated);
        assertEquals("Test Deck JUnit — MAJ", updated.getTitle());
    }

    @Test @Order(4)
    void testGetAllDecks() throws SQLException {
        List<FlashcardDeck> list = service.getAllDecks();
        assertFalse(list.isEmpty());
    }

    // ── Flashcard CRUD ─────────────────────────────────────────────────

    @Test @Order(5)
    void testAddFlashcard() throws SQLException {
        Flashcard c = new Flashcard(deckId, "Qu'est-ce que Java ?", "Un langage orienté objet.", 1);
        c.setHint("JVM");
        service.addFlashcard(c);
        assertTrue(c.getId() > 0, "L'id de la carte doit être généré");
        cardId = c.getId();
    }

    @Test @Order(6)
    void testGetFlashcardsByDeck() throws SQLException {
        List<Flashcard> cards = service.getFlashcardsByDeck(deckId);
        assertFalse(cards.isEmpty(), "Le deck doit contenir au moins une carte");
    }

    @Test @Order(7)
    void testCountFlashcards() throws SQLException {
        int count = service.countFlashcardsByDeck(deckId);
        assertTrue(count >= 1);
    }

    @Test @Order(8)
    void testUpdateFlashcard() throws SQLException {
        List<Flashcard> cards = service.getFlashcardsByDeck(deckId);
        assertFalse(cards.isEmpty());
        Flashcard c = cards.get(0);
        c.setFront("Qu'est-ce que JVM ?");
        service.updateFlashcard(c);
        List<Flashcard> updated = service.getFlashcardsByDeck(deckId);
        assertEquals("Qu'est-ce que JVM ?", updated.get(0).getFront());
    }

    @Test @Order(9)
    void testDeleteFlashcard() throws SQLException {
        service.deleteFlashcard(cardId);
        int count = service.countFlashcardsByDeck(deckId);
        assertEquals(0, count);
    }

    // ── Cleanup ────────────────────────────────────────────────────────

    @Test @Order(10)
    void testDeleteDeck() throws SQLException {
        service.deleteDeck(deckId);
        FlashcardDeck deleted = service.getDeckById(deckId);
        assertNull(deleted, "Le deck ne doit plus exister");
    }
}
