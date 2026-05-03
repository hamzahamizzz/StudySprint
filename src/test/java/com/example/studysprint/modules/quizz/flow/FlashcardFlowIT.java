package com.example.studysprint.modules.quizz.flow;

import com.example.studysprint.modules.quizz.models.Flashcard;
import com.example.studysprint.modules.quizz.models.FlashcardDeck;
import com.example.studysprint.modules.quizz.services.FlashcardService;
import com.example.studysprint.utils.MyDatabase;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration des FLOWS Flashcard.
 * Couvre : publication, drag&drop reorder (positions), CRUD cartes en cascade,
 * suppression deck → cascade cartes, comptage live.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlashcardFlowIT {

    private static FlashcardService service;
    private static long ownerId;
    private static long subjectId;
    private static long deckId;
    private static long deckDraftId;
    private static boolean dbAvailable = true;

    @BeforeAll
    static void setUp() {
        service = new FlashcardService();
        try (Connection ignored = MyDatabase.getInstance().getConnection()) {
            ownerId   = service.getFirstUserId();
            subjectId = service.getFirstSubjectId();
            Assumptions.assumeTrue(ownerId   > 0, "Aucun user en base");
            Assumptions.assumeTrue(subjectId > 0, "Aucun subject en base");
        } catch (SQLException e) {
            dbAvailable = false;
            Assumptions.assumeTrue(false, "DB indisponible : " + e.getMessage());
        }
    }

    @AfterAll
    static void tearDown() {
        if (!dbAvailable) return;
        for (long id : new long[]{ deckId, deckDraftId }) {
            if (id > 0) {
                try { service.deleteDeck(id); } catch (Exception ignored) {}
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 1 : Publication — getPublishedDecks ne retourne pas les drafts
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(1)
    void flow_getPublishedDecks_filtre() throws SQLException {
        FlashcardDeck pub = new FlashcardDeck(ownerId, subjectId, "Flow Deck — publié");
        pub.setPublished(true);
        service.addDeck(pub);
        deckId = pub.getId();

        FlashcardDeck draft = new FlashcardDeck(ownerId, subjectId, "Flow Deck — brouillon");
        draft.setPublished(false);
        service.addDeck(draft);
        deckDraftId = draft.getId();

        List<FlashcardDeck> published = service.getPublishedDecks();
        assertTrue(published.stream().anyMatch(d -> d.getId() == deckId));
        assertFalse(published.stream().anyMatch(d -> d.getId() == deckDraftId));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 2 : Ajout cartes + comptage live
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(2)
    void flow_ajoutCartes_incrementeCompteur() throws SQLException {
        for (int i = 1; i <= 4; i++) {
            Flashcard c = new Flashcard(deckId, "front-" + i, "back-" + i, i);
            service.addFlashcard(c);
            assertTrue(c.getId() > 0);
        }
        assertEquals(4, service.countFlashcardsByDeck(deckId));
    }

    @Test @Order(3)
    void flow_getFlashcardsByDeck_retourneTriParPosition() throws SQLException {
        List<Flashcard> cards = service.getFlashcardsByDeck(deckId);
        assertEquals(4, cards.size());
        for (int i = 0; i < cards.size(); i++) {
            assertEquals(i + 1, cards.get(i).getPosition(),
                    "Cartes doivent être triées par position croissante");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 3 : Drag & drop — reorder via updatePosition (offset temporaire)
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(4)
    void flow_dragDropReorder_inverseOrdreSansConflit() throws SQLException {
        // Récupère les 4 cartes en ordre 1..4
        List<Flashcard> before = service.getFlashcardsByDeck(deckId);
        assertEquals(4, before.size());

        // Simule la stratégie 2-phases du controller :
        // 1) positions temporaires 10001..10004
        for (int i = 0; i < before.size(); i++) {
            service.updatePosition(before.get(i).getId(), 10001 + i);
        }
        // 2) inversion 4..1
        for (int i = 0; i < before.size(); i++) {
            int newPos = before.size() - i; // 4, 3, 2, 1
            service.updatePosition(before.get(i).getId(), newPos);
        }

        List<Flashcard> after = service.getFlashcardsByDeck(deckId);
        assertEquals(4, after.size());

        // Le 1er en ordre tri (position=1) doit être l'ancien 4ème
        assertEquals(before.get(3).getId(), after.get(0).getId(),
                "Après inversion, ancien 4ème = nouveau 1er");
        assertEquals(before.get(0).getId(), after.get(3).getId(),
                "Après inversion, ancien 1er = nouveau 4ème");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 4 : Update + Delete d'une carte
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(5)
    void flow_updateCarte_persisteFrontEtHint() throws SQLException {
        List<Flashcard> cards = service.getFlashcardsByDeck(deckId);
        Flashcard c = cards.get(0);
        c.setFront("RECTO MODIFIÉ");
        c.setHint("Indice ajouté");
        service.updateFlashcard(c);

        List<Flashcard> after = service.getFlashcardsByDeck(deckId);
        Flashcard reloaded = after.stream().filter(x -> x.getId() == c.getId()).findFirst().orElse(null);
        assertNotNull(reloaded);
        assertEquals("RECTO MODIFIÉ", reloaded.getFront());
        assertEquals("Indice ajouté", reloaded.getHint());
    }

    @Test @Order(6)
    void flow_deleteCarte_decrementeCompteur() throws SQLException {
        List<Flashcard> cards = service.getFlashcardsByDeck(deckId);
        int countBefore = cards.size();
        long idToDelete = cards.get(0).getId();
        service.deleteFlashcard(idToDelete);

        assertEquals(countBefore - 1, service.countFlashcardsByDeck(deckId));
        assertTrue(service.getFlashcardsByDeck(deckId).stream()
                .noneMatch(c -> c.getId() == idToDelete));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 5 : Update deck — métadonnées
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(7)
    void flow_updateDeck_changeTitreEtPublished() throws SQLException {
        FlashcardDeck d = service.getDeckById(deckId);
        d.setTitle("Flow Deck — TITRE MAJ");
        d.setPublished(false);
        service.updateDeck(d);

        FlashcardDeck reloaded = service.getDeckById(deckId);
        assertEquals("Flow Deck — TITRE MAJ", reloaded.getTitle());
        assertFalse(reloaded.isPublished(), "Dépublication appliquée");

        // Maintenant il ne doit plus apparaître dans getPublishedDecks
        assertFalse(service.getPublishedDecks().stream().anyMatch(x -> x.getId() == deckId));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 6 : Suppression deck → cartes supprimées en cascade (FK)
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(8)
    void flow_deleteDeck_supprimeCartesEnCascade() throws SQLException {
        long localDeckId;
        FlashcardDeck d = new FlashcardDeck(ownerId, subjectId, "Flow Deck cascade");
        d.setPublished(true);
        service.addDeck(d);
        localDeckId = d.getId();

        Flashcard c1 = new Flashcard(localDeckId, "f1", "b1", 1);
        Flashcard c2 = new Flashcard(localDeckId, "f2", "b2", 2);
        service.addFlashcard(c1);
        service.addFlashcard(c2);
        assertEquals(2, service.countFlashcardsByDeck(localDeckId));

        service.deleteDeck(localDeckId);

        assertNull(service.getDeckById(localDeckId), "Deck supprimé");
        assertEquals(0, service.countFlashcardsByDeck(localDeckId),
                "Cartes du deck supprimées en cascade");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Flow 7 : Filtrage par owner
    // ═══════════════════════════════════════════════════════════════════

    @Test @Order(9)
    void flow_getDecksByOwner_filtre() throws SQLException {
        List<FlashcardDeck> mine = service.getDecksByOwner(ownerId);
        assertTrue(mine.stream().allMatch(d -> d.getOwnerId() == ownerId));
        assertTrue(mine.stream().anyMatch(d -> d.getId() == deckId));
    }
}
