package com.example.studysprint.modules.quizz.unit;

import com.example.studysprint.modules.quizz.models.Difficulty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires purs (aucune DB) pour l'enum Difficulty.
 * Couvre fromString — parsing tolérant de la casse + valeurs nulles/invalides.
 */
class DifficultyUnitTest {

    @Test
    void fromString_majuscules_retourneEnum() {
        assertEquals(Difficulty.EASY,   Difficulty.fromString("EASY"));
        assertEquals(Difficulty.MEDIUM, Difficulty.fromString("MEDIUM"));
        assertEquals(Difficulty.HARD,   Difficulty.fromString("HARD"));
    }

    @Test
    void fromString_minuscules_retourneEnum() {
        assertEquals(Difficulty.EASY,   Difficulty.fromString("easy"));
        assertEquals(Difficulty.MEDIUM, Difficulty.fromString("medium"));
        assertEquals(Difficulty.HARD,   Difficulty.fromString("hard"));
    }

    @Test
    void fromString_casseMixte_retourneEnum() {
        assertEquals(Difficulty.EASY,   Difficulty.fromString("Easy"));
        assertEquals(Difficulty.MEDIUM, Difficulty.fromString("MeDiUm"));
    }

    @Test
    void fromString_null_retourneNull() {
        assertNull(Difficulty.fromString(null));
    }

    @Test
    void fromString_invalide_retourneNull() {
        assertNull(Difficulty.fromString("EXPERT"));
        assertNull(Difficulty.fromString(""));
        assertNull(Difficulty.fromString("   "));
    }

    @Test
    void enum_aTroisValeurs() {
        assertEquals(3, Difficulty.values().length);
    }
}
