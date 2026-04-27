package com.example.studysprint.modules.quizz;

import com.example.studysprint.modules.quizz.models.Difficulty;
import com.example.studysprint.modules.quizz.models.Quiz;
import com.example.studysprint.modules.quizz.models.QuizAttempt;
import com.example.studysprint.modules.quizz.models.QuizRating;
import com.example.studysprint.modules.quizz.services.QuizService;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for QuizService.
 * Requires a running MariaDB instance with the studysprint database.
 * Tests use real INSERT/UPDATE/DELETE operations.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuizServiceTest {

    private static QuizService service;
    private static long insertedQuizId;

    @BeforeAll
    static void setUp() {
        service = new QuizService();
    }

    // ── Quiz CRUD ──────────────────────────────────────────────────────

    @Test @Order(1)
    void testAddQuiz() throws SQLException {
        Quiz q = new Quiz(1L, 1L, "Test Quiz JUnit", Difficulty.EASY, "[]");
        q.setPublished(false);
        service.addQuiz(q);
        assertTrue(q.getId() > 0, "L'id doit être généré après insertion");
        insertedQuizId = q.getId();
    }

    @Test @Order(2)
    void testGetQuizById() throws SQLException {
        Quiz q = service.getQuizById(insertedQuizId);
        assertNotNull(q, "Le quiz doit exister après insertion");
        assertEquals("Test Quiz JUnit", q.getTitle());
        assertEquals(Difficulty.EASY, q.getDifficulty());
    }

    @Test @Order(3)
    void testGetAllQuizzes() throws SQLException {
        List<Quiz> list = service.getAllQuizzes();
        assertFalse(list.isEmpty(), "La liste ne doit pas être vide");
    }

    @Test @Order(4)
    void testUpdateQuiz() throws SQLException {
        Quiz q = service.getQuizById(insertedQuizId);
        assertNotNull(q);
        q.setTitle("Test Quiz JUnit — MAJ");
        q.setDifficulty(Difficulty.HARD);
        service.updateQuiz(q);
        Quiz updated = service.getQuizById(insertedQuizId);
        assertNotNull(updated);
        assertEquals("Test Quiz JUnit — MAJ", updated.getTitle());
        assertEquals(Difficulty.HARD, updated.getDifficulty());
    }

    // ── QuizAttempt ────────────────────────────────────────────────────

    @Test @Order(5)
    void testAddAttempt() throws SQLException {
        QuizAttempt attempt = new QuizAttempt(1L, insertedQuizId, LocalDateTime.now(), 5);
        service.addAttempt(attempt);
        assertTrue(attempt.getId() > 0, "L'id de tentative doit être généré");
    }

    @Test @Order(6)
    void testGetAttemptsByQuiz() throws SQLException {
        List<QuizAttempt> list = service.getAttemptsByQuiz(insertedQuizId);
        assertFalse(list.isEmpty(), "Il doit y avoir au moins une tentative");
    }

    // ── QuizRating ─────────────────────────────────────────────────────

    @Test @Order(7)
    void testAddOrUpdateRating() throws SQLException {
        QuizRating r = new QuizRating(1L, insertedQuizId, 4);
        service.addOrUpdateRating(r);
        double avg = service.getAverageRating(insertedQuizId);
        assertTrue(avg >= 1 && avg <= 5, "La moyenne doit être entre 1 et 5");
    }

    // ── Cleanup ────────────────────────────────────────────────────────

    @Test @Order(8)
    void testDeleteQuiz() throws SQLException {
        service.deleteQuiz(insertedQuizId);
        Quiz deleted = service.getQuizById(insertedQuizId);
        assertNull(deleted, "Le quiz ne doit plus exister après suppression");
    }
}
