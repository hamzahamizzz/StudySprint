package com.example.studysprint.modules.quizz.unit;

import com.example.studysprint.utils.RecommendationEngine;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires purs pour les méthodes de calcul de RecommendationEngine.
 * Aucune DB, aucun réseau — couvre cosineSimilarity et findKNearestNeighbors.
 */
class RecommendationEngineUnitTest {

    private final RecommendationEngine engine = new RecommendationEngine();

    // ── cosineSimilarity ──────────────────────────────────────────────────

    @Test
    void cosineSimilarity_veceursIdentiques_retourne1() {
        Map<Long, Double> v = Map.of(1L, 0.8, 2L, 0.6, 3L, 1.0);
        assertEquals(1.0, engine.cosineSimilarity(v, v), 1e-9);
    }

    @Test
    void cosineSimilarity_veceursOrthogonaux_retourne0() {
        Map<Long, Double> a = Map.of(1L, 1.0, 2L, 0.0);
        Map<Long, Double> b = Map.of(1L, 0.0, 2L, 1.0);
        assertEquals(0.0, engine.cosineSimilarity(a, b), 1e-9);
    }

    @Test
    void cosineSimilarity_aucuneCleCommunne_retourne0() {
        Map<Long, Double> a = Map.of(1L, 0.9);
        Map<Long, Double> b = Map.of(2L, 0.9);
        assertEquals(0.0, engine.cosineSimilarity(a, b), 1e-9);
    }

    @Test
    void cosineSimilarity_nullOuVide_retourne0() {
        Map<Long, Double> v = Map.of(1L, 0.5);
        assertEquals(0.0, engine.cosineSimilarity(null, v));
        assertEquals(0.0, engine.cosineSimilarity(v, null));
        assertEquals(0.0, engine.cosineSimilarity(null, null));
        assertEquals(0.0, engine.cosineSimilarity(Map.of(), v));
        assertEquals(0.0, engine.cosineSimilarity(v, Map.of()));
    }

    @Test
    void cosineSimilarity_normalisationCorrectePourValeursQuelconques() {
        // v1 = [1, 0], v2 = [1, 1] → dot=1, |v1|=1, |v2|=sqrt(2) → cos=1/sqrt(2) ≈ 0.7071
        Map<Long, Double> a = new HashMap<>();
        a.put(1L, 1.0);
        a.put(2L, 0.0);
        Map<Long, Double> b = new HashMap<>();
        b.put(1L, 1.0);
        b.put(2L, 1.0);
        assertEquals(1.0 / Math.sqrt(2.0), engine.cosineSimilarity(a, b), 1e-9);
    }

    @Test
    void cosineSimilarity_symetrique() {
        Map<Long, Double> a = Map.of(1L, 0.5, 2L, 0.8, 3L, 0.3);
        Map<Long, Double> b = Map.of(1L, 0.9, 2L, 0.1, 3L, 0.7);
        double ab = engine.cosineSimilarity(a, b);
        double ba = engine.cosineSimilarity(b, a);
        assertEquals(ab, ba, 1e-9, "La similarité cosinus est symétrique");
    }

    @Test
    void cosineSimilarity_borneEntre0Et1_pourValeursPositives() {
        Map<Long, Double> a = Map.of(1L, 0.7, 2L, 0.3, 3L, 0.9);
        Map<Long, Double> b = Map.of(1L, 0.1, 2L, 0.6, 3L, 0.5);
        double sim = engine.cosineSimilarity(a, b);
        assertTrue(sim >= 0.0 && sim <= 1.0, "Cosinus ∈ [0,1] pour scores normalisés positifs");
    }

    // ── findKNearestNeighbors ─────────────────────────────────────────────

    @Test
    void findKNearestNeighbors_userInconnu_retourneListeVide() {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        matrix.put(1L, Map.of(10L, 0.8, 11L, 0.5));
        matrix.put(2L, Map.of(10L, 0.9, 11L, 0.6));
        List<Long> result = engine.findKNearestNeighbors(99L, matrix, 3);
        assertTrue(result.isEmpty(), "User inconnu → liste vide");
    }

    @Test
    void findKNearestNeighbors_matrixVide_retourneListeVide() {
        List<Long> result = engine.findKNearestNeighbors(1L, new HashMap<>(), 3);
        assertTrue(result.isEmpty());
    }

    @Test
    void findKNearestNeighbors_exclutUserLuiMeme() {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        matrix.put(1L, Map.of(10L, 0.8));
        matrix.put(2L, Map.of(10L, 0.8));
        List<Long> result = engine.findKNearestNeighbors(1L, matrix, 5);
        assertFalse(result.contains(1L), "L'user lui-même ne doit pas apparaître");
    }

    @Test
    void findKNearestNeighbors_k1_retourneLePlusSimilaire() {
        // user 1 est similaire à user 2 (quiz identiques) mais pas du tout à user 3
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        matrix.put(1L, Map.of(10L, 0.9, 11L, 0.8));           // cible
        matrix.put(2L, Map.of(10L, 0.9, 11L, 0.8));           // très proche
        matrix.put(3L, Map.of(20L, 0.5, 21L, 0.6));           // aucune clé commune
        List<Long> result = engine.findKNearestNeighbors(1L, matrix, 1);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0), "User 2 est le voisin le plus proche");
    }

    @Test
    void findKNearestNeighbors_trieParSimDecroissant() {
        // user 1 (cible) : quiz 10=1.0, quiz 11=0.0
        // user 3 : quiz 10=1.0, quiz 11=0.0  → cosine=1.0 (vecteur identique)
        // user 2 : quiz 10=1.0, quiz 11=1.0  → cosine = dot/|a||b| = 1/(1*sqrt(2)) ≈ 0.707
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        Map<Long, Double> target = new HashMap<>(); target.put(10L, 1.0); target.put(11L, 0.0);
        Map<Long, Double> near   = new HashMap<>(); near.put(10L, 1.0);   near.put(11L, 0.0);
        Map<Long, Double> far    = new HashMap<>(); far.put(10L, 1.0);    far.put(11L, 1.0);
        matrix.put(1L, target);
        matrix.put(3L, near);   // sim = 1.0
        matrix.put(2L, far);    // sim ≈ 0.707
        List<Long> result = engine.findKNearestNeighbors(1L, matrix, 2);
        assertEquals(2, result.size());
        assertEquals(3L, result.get(0), "User 3 (sim=1.0) doit être en premier");
        assertEquals(2L, result.get(1), "User 2 (sim≈0.707) doit être en second");
    }

    @Test
    void findKNearestNeighbors_kPlusGrandQueNeighbors_retourneTous() {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        matrix.put(1L, Map.of(10L, 0.8));
        matrix.put(2L, Map.of(10L, 0.6));
        // Demande k=10 mais 1 seul voisin possible
        List<Long> result = engine.findKNearestNeighbors(1L, matrix, 10);
        assertEquals(1, result.size());
    }

    @Test
    void findKNearestNeighbors_exclutSimilaritesNullesEtNegatives() {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        matrix.put(1L, Map.of(10L, 0.9));
        // user 2 n'a aucune clé commune → sim = 0.0 → exclu
        matrix.put(2L, Map.of(99L, 0.5));
        List<Long> result = engine.findKNearestNeighbors(1L, matrix, 5);
        assertFalse(result.contains(2L), "User avec sim=0 ne doit pas être voisin");
    }
}
