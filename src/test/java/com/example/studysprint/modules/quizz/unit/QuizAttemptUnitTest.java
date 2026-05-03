package com.example.studysprint.modules.quizz.unit;

import com.example.studysprint.modules.quizz.models.QuizAttempt;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class QuizAttemptUnitTest {

    @Test
    void constructeur_initialiseChampsObligatoires() {
        LocalDateTime now = LocalDateTime.now();
        QuizAttempt a = new QuizAttempt(7L, 42L, now, 10);
        assertEquals(7L,  a.getUserId());
        assertEquals(42L, a.getQuizId());
        assertEquals(now, a.getStartedAt());
        assertEquals(10,  a.getTotalQuestions());
        assertEquals(0,   a.getCorrectCount(), "Compteur démarre à 0");
        assertNull(a.getCompletedAt(), "completedAt nul tant que non terminé");
        assertNull(a.getScore(),       "score nul tant que non calculé");
    }

    @Test
    void setScore_acceptePourcentageDecimal() {
        QuizAttempt a = new QuizAttempt();
        a.setScore(73.50);
        assertEquals(73.50, a.getScore(), 0.001);
    }

    @Test
    void completeAttempt_renseigneTous() {
        LocalDateTime start = LocalDateTime.now();
        QuizAttempt a = new QuizAttempt(1L, 1L, start, 5);
        a.setCompletedAt(start.plusMinutes(2));
        a.setCorrectCount(4);
        a.setScore(80.0);
        a.setDurationSeconds(120);

        assertEquals(4,    a.getCorrectCount());
        assertEquals(80.0, a.getScore());
        assertEquals(120,  a.getDurationSeconds());
        assertNotNull(a.getCompletedAt());
    }

    @Test
    void durationSeconds_nullableParDefaut() {
        QuizAttempt a = new QuizAttempt();
        assertNull(a.getDurationSeconds());
        a.setDurationSeconds(60);
        assertEquals(60, a.getDurationSeconds());
        a.setDurationSeconds(null);
        assertNull(a.getDurationSeconds());
    }
}
