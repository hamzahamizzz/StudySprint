package com.example.studysprint.modules.quizz.unit;

import com.example.studysprint.modules.quizz.models.QuizRating;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de QuizRating — pas de DB.
 * Vérifie la validation 1..5 du score.
 */
class QuizRatingUnitTest {

    @Test
    void constructeur_creeNoteValide() {
        QuizRating r = new QuizRating(1L, 10L, 4);
        assertEquals(1L,  r.getUserId());
        assertEquals(10L, r.getQuizId());
        assertEquals(4,   r.getScore());
        assertNotNull(r.getCreatedAt(), "createdAt initialisé par défaut");
    }

    @Test
    void setScore_valeurValide_passe() {
        QuizRating r = new QuizRating();
        for (int i = 1; i <= 5; i++) {
            r.setScore(i);
            assertEquals(i, r.getScore());
        }
    }

    @Test
    void setScore_zero_leve() {
        QuizRating r = new QuizRating();
        assertThrows(IllegalArgumentException.class, () -> r.setScore(0));
    }

    @Test
    void setScore_six_leve() {
        QuizRating r = new QuizRating();
        assertThrows(IllegalArgumentException.class, () -> r.setScore(6));
    }

    @Test
    void setScore_negatif_leve() {
        QuizRating r = new QuizRating();
        assertThrows(IllegalArgumentException.class, () -> r.setScore(-3));
    }
}
