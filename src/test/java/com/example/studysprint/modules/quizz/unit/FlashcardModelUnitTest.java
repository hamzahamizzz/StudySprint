package com.example.studysprint.modules.quizz.unit;

import com.example.studysprint.modules.quizz.models.Flashcard;
import com.example.studysprint.modules.quizz.models.FlashcardDeck;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires des modèles Flashcard / FlashcardDeck — sans DB.
 */
class FlashcardModelUnitTest {

    @Test
    void flashcard_constructeur_initialiseTout() {
        Flashcard c = new Flashcard(5L, "front", "back", 2);
        assertEquals(5L,      c.getDeckId());
        assertEquals("front", c.getFront());
        assertEquals("back",  c.getBack());
        assertEquals(2,       c.getPosition());
        assertNotNull(c.getCreatedAt());
        assertNull(c.getHint(), "Pas d'indice par défaut");
    }

    @Test
    void flashcard_setHint_accepteNullEtTexte() {
        Flashcard c = new Flashcard();
        c.setHint("indice");
        assertEquals("indice", c.getHint());
        c.setHint(null);
        assertNull(c.getHint());
    }

    @Test
    void flashcard_position_modifiable() {
        Flashcard c = new Flashcard(1L, "f", "b", 1);
        c.setPosition(99);
        assertEquals(99, c.getPosition());
    }

    @Test
    void flashcardDeck_constructeur_initialiseTitre() {
        FlashcardDeck d = new FlashcardDeck(1L, 2L, "Mon deck");
        assertEquals(1L,         d.getOwnerId());
        assertEquals(2L,         d.getSubjectId());
        assertEquals("Mon deck", d.getTitle());
        assertFalse(d.isPublished(), "Par défaut non publié");
    }

    @Test
    void flashcardDeck_published_modifiable() {
        FlashcardDeck d = new FlashcardDeck(1L, 1L, "X");
        d.setPublished(true);
        assertTrue(d.isPublished());
        d.setPublished(false);
        assertFalse(d.isPublished());
    }

    @Test
    void flashcardDeck_chapterId_nullable() {
        FlashcardDeck d = new FlashcardDeck();
        assertNull(d.getChapterId());
        d.setChapterId(7L);
        assertEquals(7L, d.getChapterId());
    }
}
