package com.example.studysprint.modules.quizz.flow;

import com.example.studysprint.modules.quizz.models.Flashcard;
import com.example.studysprint.modules.quizz.models.FlashcardDeck;
import com.example.studysprint.modules.quizz.services.FlashcardService;
import com.example.studysprint.utils.MyDatabase;
import com.example.studysprint.utils.SM2Algorithm;
import com.example.studysprint.utils.SM2Algorithm.SM2Result;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration du flow SM-2 : updateSM2, getDueFlashcards, getDueCount, getNextSessionDate.
 * Nécessite la base studysprint accessible ET la migration SM-2 appliquée
 * (colonnes ease_factor, interval_days, repetitions, next_review).
 *
 * Tous les enregistrements créés sont nettoyés en @AfterAll.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SM2FlowIT {

    private static FlashcardService service;
    private static SM2Algorithm sm2;
    private static long ownerId;
    private static long subjectId;
    private static long deckId;
    private static long cardId1;
    private static long cardId2;

    @BeforeAll
    static void setUp() {
        service = new FlashcardService();
        sm2     = new SM2Algorithm();
        try (Connection ignored = MyDatabase.getInstance().getConnection()) {
            ownerId   = service.getFirstUserId();
            subjectId = service.getFirstSubjectId();
            Assumptions.assumeTrue(ownerId   > 0, "Aucun user en base");
            Assumptions.assumeTrue(subjectId > 0, "Aucun subject en base");
        } catch (SQLException e) {
            Assumptions.assumeTrue(false, "DB indisponible : " + e.getMessage());
        }
    }

    @AfterAll
    static void tearDown() {
        if (deckId > 0) {
            try { service.deleteDeck(deckId); } catch (Exception ignored) {}
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Setup : créer un deck + 2 cartes avec des next_review différentes
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(1)
    void sm2flow_setup_createDeckAndCards() throws SQLException {
        FlashcardDeck deck = new FlashcardDeck(ownerId, subjectId, "SM2 Flow IT Deck");
        deck.setPublished(true);
        service.addDeck(deck);
        deckId = deck.getId();
        assertTrue(deckId > 0);

        Flashcard c1 = new Flashcard(deckId, "SM2 front 1", "SM2 back 1", 1);
        Flashcard c2 = new Flashcard(deckId, "SM2 front 2", "SM2 back 2", 2);
        service.addFlashcard(c1);
        service.addFlashcard(c2);
        cardId1 = c1.getId();
        cardId2 = c2.getId();
        assertTrue(cardId1 > 0);
        assertTrue(cardId2 > 0);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 1 : updateSM2 — persiste tous les champs SM-2
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(2)
    void sm2flow_updateSM2_persistsAllFields() throws SQLException {
        // Assume SM-2 migration ran (columns exist)
        SM2Result result = sm2.calculate(5, 0, 2.5f, 1);
        try {
            service.updateSM2(cardId1, result);
        } catch (SQLException e) {
            // If SM-2 columns don't exist yet, skip gracefully
            Assumptions.assumeTrue(false, "Migration SM-2 non appliquée : " + e.getMessage());
        }

        // Read back and verify
        List<Flashcard> cards = service.getFlashcardsByDeck(deckId);
        Flashcard c1 = cards.stream().filter(c -> c.getId() == cardId1).findFirst().orElse(null);
        assertNotNull(c1);
        assertEquals(result.newRepetitions(), c1.getRepetitions());
        assertEquals(result.newIntervalDays(), c1.getIntervalDays());
        assertEquals(result.newEaseFactor(), c1.getEaseFactor(), 0.01f);
        assertEquals(result.nextReview(), c1.getNextReview());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 2 : getDueCount + getDueFlashcards — carte due today vs future
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(3)
    void sm2flow_getDueCount_cartesDuesToday() throws SQLException {
        // Set card1 → next_review = today (due)
        SM2Result dueResult = new SM2Result(0, 2.5f, 1, LocalDate.now());
        try {
            service.updateSM2(cardId1, dueResult);
        } catch (SQLException e) {
            Assumptions.assumeTrue(false, "Migration SM-2 non appliquée");
        }

        // Set card2 → next_review = far future (not due)
        SM2Result futureResult = new SM2Result(30, 2.5f, 3, LocalDate.now().plusDays(30));
        service.updateSM2(cardId2, futureResult);

        int due = service.getDueCount(deckId);
        assertTrue(due >= 1, "Au moins 1 carte doit être due aujourd'hui");
    }

    @Test @Order(4)
    void sm2flow_getDueFlashcards_contientCartesDuePasFuture() throws SQLException {
        try {
            List<Flashcard> due = service.getDueFlashcards(deckId);
            // card1 has next_review = today → due
            // card2 has next_review = today+30 → not due
            assertTrue(due.stream().anyMatch(c -> c.getId() == cardId1),
                    "Carte due aujourd'hui doit être retournée");
            assertFalse(due.stream().anyMatch(c -> c.getId() == cardId2),
                    "Carte future ne doit PAS être retournée");
        } catch (SQLException e) {
            Assumptions.assumeTrue(false, "Migration SM-2 non appliquée");
        }
    }

    @Test @Order(5)
    void sm2flow_getDueFlashcards_ordreParNextReview() throws SQLException {
        // Add a second due card with an earlier date
        SM2Result earlierResult = new SM2Result(0, 2.5f, 1, LocalDate.now().minusDays(1));
        SM2Result todayResult   = new SM2Result(0, 2.5f, 1, LocalDate.now());
        try {
            service.updateSM2(cardId2, earlierResult);
            service.updateSM2(cardId1, todayResult);
        } catch (SQLException e) {
            Assumptions.assumeTrue(false, "Migration SM-2 non appliquée");
        }

        List<Flashcard> due = service.getDueFlashcards(deckId);
        assertTrue(due.size() >= 2, "Les deux cartes doivent être dues");
        // Sorted ASC by next_review → card2 (yesterday) before card1 (today)
        long firstId = due.get(0).getId();
        assertEquals(cardId2, firstId, "La carte la plus en retard doit être en premier");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 3 : getNextSessionDate — date min des cartes non dues
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(6)
    void sm2flow_getNextSessionDate_retourneDateFuture() throws SQLException {
        // Set both cards to future dates
        SM2Result f1 = new SM2Result(5,  2.5f, 2, LocalDate.now().plusDays(5));
        SM2Result f2 = new SM2Result(10, 2.5f, 3, LocalDate.now().plusDays(10));
        try {
            service.updateSM2(cardId1, f1);
            service.updateSM2(cardId2, f2);
        } catch (SQLException e) {
            Assumptions.assumeTrue(false, "Migration SM-2 non appliquée");
        }

        LocalDate nextDate = service.getNextSessionDate(deckId);
        assertNotNull(nextDate, "Doit retourner une date future");
        // The minimum future date = today+5
        assertEquals(LocalDate.now().plusDays(5), nextDate,
                "Doit retourner la date la plus proche (today+5)");
    }

    @Test @Order(7)
    void sm2flow_getNextSessionDate_nullSiToutesLesCortesOntDueAujourdhui() throws SQLException {
        // Set both cards to today
        SM2Result today = new SM2Result(0, 2.5f, 1, LocalDate.now());
        try {
            service.updateSM2(cardId1, today);
            service.updateSM2(cardId2, today);
        } catch (SQLException e) {
            Assumptions.assumeTrue(false, "Migration SM-2 non appliquée");
        }

        LocalDate nextDate = service.getNextSessionDate(deckId);
        // All cards are due today → MIN(next_review WHERE next_review > CURDATE()) = null
        assertNull(nextDate, "Si toutes les cartes sont dues aujourd'hui, pas de date future");
    }
}
