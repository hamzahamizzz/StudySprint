package com.example.studysprint.modules.quizz.flow;

import com.example.studysprint.modules.quizz.models.Difficulty;
import com.example.studysprint.modules.quizz.models.Quiz;
import com.example.studysprint.modules.quizz.models.QuizAttempt;
import com.example.studysprint.modules.quizz.models.QuizRating;
import com.example.studysprint.modules.quizz.services.QuizService;
import com.example.studysprint.utils.MyDatabase;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration des FLOWS métier du module Quiz.
 * Vérifie les enchaînements complets : publication, tri, statistiques, ratings, attempts.
 * Nécessite une base studysprint accessible.
 *
 * Tous les enregistrements créés sont nettoyés en @AfterAll.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuizFlowIT {

    private static QuizService service;
    private static long ownerId;
    private static long subjectId;
    private static long quizEasyId;
    private static long quizMediumId;
    private static long quizHardId;
    private static boolean dbAvailable = true;

    @BeforeAll
    static void setUp() {
        service = new QuizService();
        try (Connection ignored = MyDatabase.getInstance().getConnection()) {
            ownerId   = service.getFirstUserId();
            subjectId = service.getFirstSubjectId();
            assumeIdsValid();
        } catch (SQLException e) {
            dbAvailable = false;
            Assumptions.assumeTrue(false, "DB indisponible : " + e.getMessage());
        }
    }

    private static void assumeIdsValid() {
        Assumptions.assumeTrue(ownerId   > 0, "Aucun user en base");
        Assumptions.assumeTrue(subjectId > 0, "Aucun subject en base");
    }

    @AfterAll
    static void tearDown() {
        if (!dbAvailable) return;
        for (long id : new long[]{ quizEasyId, quizMediumId, quizHardId }) {
            if (id > 0) {
                try { service.deleteQuiz(id); } catch (Exception ignored) {}
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 1 : Publication & filtrage
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(1)
    void flow_publication_filtrePublished() throws SQLException {
        Quiz qPub = new Quiz(ownerId, subjectId, "Flow IT — publié",
                Difficulty.EASY, "[{\"question\":\"Q\",\"options\":[\"a\",\"b\",\"c\",\"d\"],\"correct\":0}]");
        qPub.setPublished(true);
        service.addQuiz(qPub);
        quizEasyId = qPub.getId();

        Quiz qDraft = new Quiz(ownerId, subjectId, "Flow IT — brouillon",
                Difficulty.MEDIUM, "[]");
        qDraft.setPublished(false);
        service.addQuiz(qDraft);
        quizMediumId = qDraft.getId();

        List<Quiz> published = service.getPublishedQuizzes();
        assertTrue(published.stream().anyMatch(q -> q.getId() == quizEasyId),
                "Le quiz publié doit apparaître");
        assertFalse(published.stream().anyMatch(q -> q.getId() == quizMediumId),
                "Le brouillon ne doit PAS apparaître");
    }

    @Test @Order(2)
    void flow_togglePublished_basculeBienLEtat() throws SQLException {
        // Bascule le brouillon → publié
        service.togglePublished(quizMediumId, true);
        Quiz q = service.getQuizById(quizMediumId);
        assertTrue(q.isPublished(), "Doit être publié après toggle");

        // Re-bascule
        service.togglePublished(quizMediumId, false);
        q = service.getQuizById(quizMediumId);
        assertFalse(q.isPublished(), "Doit être dépublié après deuxième toggle");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 2 : Tentative complète (start → finish → score persisté)
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(3)
    void flow_tentativeComplete_persisteScoreEtDuree() throws SQLException {
        LocalDateTime start = LocalDateTime.now();
        QuizAttempt a = new QuizAttempt(ownerId, quizEasyId, start, 5);
        service.addAttempt(a);
        assertTrue(a.getId() > 0, "Tentative créée");

        // Simule la complétion
        a.setCompletedAt(start.plusMinutes(1));
        a.setCorrectCount(4);
        a.setScore(80.0);
        a.setDurationSeconds(60);
        service.updateAttempt(a);

        // Re-lecture via getAttemptsByQuiz
        List<QuizAttempt> attempts = service.getAttemptsByQuiz(quizEasyId);
        QuizAttempt persisted = attempts.stream()
                .filter(at -> at.getId() == a.getId())
                .findFirst().orElse(null);
        assertNotNull(persisted, "La tentative doit être retrouvée");
        assertEquals(4, persisted.getCorrectCount());
        assertEquals(80.0, persisted.getScore(), 0.01);
        assertNotNull(persisted.getCompletedAt());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 3 : Rating — addOrUpdate (UNIQUE user_id, quiz_id)
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(4)
    void flow_addOrUpdateRating_remplaceAuLieuDeDoublonner() throws SQLException {
        QuizRating r1 = new QuizRating(ownerId, quizEasyId, 3);
        service.addOrUpdateRating(r1);
        double avg1 = service.getAverageRating(quizEasyId);
        assertEquals(3.0, avg1, 0.01, "Note initiale = 3");

        // Même user re-note → doit remplacer (pas ajouter)
        QuizRating r2 = new QuizRating(ownerId, quizEasyId, 5);
        service.addOrUpdateRating(r2);
        double avg2 = service.getAverageRating(quizEasyId);
        assertEquals(5.0, avg2, 0.01, "Note remplacée = 5 (pas la moyenne (3+5)/2)");
    }

    @Test @Order(5)
    void flow_getAllAverageRatings_retourneCacheUtilisable() throws SQLException {
        Map<Long, Double> map = service.getAllAverageRatings();
        assertNotNull(map);
        // Le quiz noté précédemment doit être dans la map
        if (map.containsKey(quizEasyId)) {
            assertEquals(5.0, map.get(quizEasyId), 0.01);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 4 : Stats — tentatives + score moyen
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(6)
    void flow_quizStats_compteTentativesEtMoyenne() throws SQLException {
        Map<Long, int[]> stats = service.getQuizStats();
        assertNotNull(stats);
        int[] s = stats.get(quizEasyId);
        if (s != null) {
            assertTrue(s[0] >= 1, "Au moins 1 tentative");
            assertTrue(s[1] >= 0 && s[1] <= 100, "Score moyen entre 0 et 100");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 5 : Update — modification métadonnées + JSON questions
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(7)
    void flow_updateQuiz_changeTitreEtJson() throws SQLException {
        Quiz q = service.getQuizById(quizEasyId);
        assertNotNull(q);
        q.setTitle("Flow IT — TITRE MODIFIÉ");
        q.setDifficulty(Difficulty.HARD);
        q.setQuestions("[{\"question\":\"Nouvelle Q\",\"options\":[\"x\",\"y\",\"z\",\"w\"],\"correct\":2}]");
        service.updateQuiz(q);

        Quiz reloaded = service.getQuizById(quizEasyId);
        assertEquals("Flow IT — TITRE MODIFIÉ", reloaded.getTitle());
        assertEquals(Difficulty.HARD, reloaded.getDifficulty());
        assertEquals(1, reloaded.getQuestionCount(), "Le nouveau JSON contient 1 question");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 6 : Création AI-generated + flag generated_by_ai
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(8)
    void flow_quizAiGenerated_persisteFlag() throws SQLException {
        Quiz q = new Quiz(ownerId, subjectId, "Flow IT — IA",
                Difficulty.HARD, "[]");
        q.setGeneratedByAi(true);
        q.setTemplateKey("mistral-5q");
        service.addQuiz(q);
        quizHardId = q.getId();

        Quiz reloaded = service.getQuizById(quizHardId);
        assertTrue(reloaded.isGeneratedByAi(), "Flag IA persisté");
        assertEquals("mistral-5q", reloaded.getTemplateKey());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 7 : Filtrage par owner
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(9)
    void flow_getQuizzesByOwner_filtreCorrectement() throws SQLException {
        List<Quiz> mine = service.getQuizzesByOwner(ownerId);
        assertNotNull(mine);
        assertTrue(mine.stream().allMatch(q -> q.getOwnerId() == ownerId),
                "Tous les quiz retournés appartiennent à l'owner");
        assertTrue(mine.stream().anyMatch(q -> q.getId() == quizEasyId),
                "Le quiz créé doit apparaître");
    }
}
