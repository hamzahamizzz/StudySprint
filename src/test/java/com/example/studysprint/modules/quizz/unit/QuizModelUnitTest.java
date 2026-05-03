package com.example.studysprint.modules.quizz.unit;

import com.example.studysprint.modules.quizz.models.Difficulty;
import com.example.studysprint.modules.quizz.models.Quiz;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de Quiz — pas de DB.
 * Couvre le constructeur, getters/setters, et le parsing JSON de getQuestionCount().
 */
class QuizModelUnitTest {

    @Test
    void constructeur_initialiseTousLesChamps() {
        Quiz q = new Quiz(7L, 3L, "Mon quiz", Difficulty.HARD, "[]");
        assertEquals(7L,             q.getOwnerId());
        assertEquals(3L,             q.getSubjectId());
        assertEquals("Mon quiz",     q.getTitle());
        assertEquals(Difficulty.HARD, q.getDifficulty());
        assertEquals("[]",           q.getQuestions());
        assertFalse(q.isPublished(), "Par défaut non publié");
        assertFalse(q.isGeneratedByAi(), "Par défaut pas IA");
    }

    @Test
    void questionCount_jsonNull_retourneZero() {
        Quiz q = new Quiz();
        assertEquals(0, q.getQuestionCount());
    }

    @Test
    void questionCount_jsonVide_retourneZero() {
        Quiz q = new Quiz();
        q.setQuestions("");
        assertEquals(0, q.getQuestionCount());
        q.setQuestions("   ");
        assertEquals(0, q.getQuestionCount());
    }

    @Test
    void questionCount_tableauVide_retourneZero() {
        Quiz q = new Quiz();
        q.setQuestions("[]");
        assertEquals(0, q.getQuestionCount());
    }

    @Test
    void questionCount_tableauAvec3Questions_retourne3() {
        Quiz q = new Quiz();
        q.setQuestions("[{\"question\":\"Q1\"},{\"question\":\"Q2\"},{\"question\":\"Q3\"}]");
        assertEquals(3, q.getQuestionCount());
    }

    @Test
    void questionCount_jsonInvalide_retourneZero() {
        Quiz q = new Quiz();
        q.setQuestions("ceci n'est pas du json");
        assertEquals(0, q.getQuestionCount());
    }

    @Test
    void questionCount_jsonObjet_retourneZero() {
        Quiz q = new Quiz();
        q.setQuestions("{\"question\":\"Q1\"}"); // objet, pas array
        assertEquals(0, q.getQuestionCount());
    }

    @Test
    void setQuestions_resetCache() {
        Quiz q = new Quiz();
        q.setQuestions("[{\"q\":1}]");
        assertEquals(1, q.getQuestionCount());
        // Change → recompute
        q.setQuestions("[{\"q\":1},{\"q\":2}]");
        assertEquals(2, q.getQuestionCount());
    }

    @Test
    void chapterId_peutEtreNullable() {
        Quiz q = new Quiz();
        assertNull(q.getChapterId());
        q.setChapterId(42L);
        assertEquals(42L, q.getChapterId());
        q.setChapterId(null);
        assertNull(q.getChapterId());
    }

    @Test
    void toString_contientId() {
        Quiz q = new Quiz();
        q.setId(99L);
        q.setTitle("X");
        q.setDifficulty(Difficulty.EASY);
        String s = q.toString();
        assertTrue(s.contains("99"));
        assertTrue(s.contains("X"));
        assertTrue(s.contains("EASY"));
    }
}
