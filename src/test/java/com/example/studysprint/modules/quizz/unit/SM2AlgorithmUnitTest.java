package com.example.studysprint.modules.quizz.unit;

import com.example.studysprint.utils.SM2Algorithm;
import com.example.studysprint.utils.SM2Algorithm.SM2Result;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires purs pour SM2Algorithm — aucune DB, aucun réseau.
 */
class SM2AlgorithmUnitTest {

    private final SM2Algorithm sm2 = new SM2Algorithm();

    // ── calculate : chemin succès (quality >= 3) ──────────────────────────

    @Test
    void calculate_premiereSucession_intervalEgalUn() {
        SM2Result r = sm2.calculate(4, 0, 2.5f, 1);
        assertEquals(1, r.newIntervalDays(), "Rep=0 → interval=1");
        assertEquals(1, r.newRepetitions());
    }

    @Test
    void calculate_deuxiemeSucession_intervalEgalSix() {
        SM2Result r = sm2.calculate(4, 1, 2.5f, 1);
        assertEquals(6, r.newIntervalDays(), "Rep=1 → interval=6");
        assertEquals(2, r.newRepetitions());
    }

    @Test
    void calculate_troisiemeSucession_intervalMuliplieParEF() {
        // reps=2, ef=2.5, interval=6 → round(6 * 2.5) = 15
        SM2Result r = sm2.calculate(4, 2, 2.5f, 6);
        assertEquals(15, r.newIntervalDays(), "Rep>=2 → interval = round(prev * EF)");
        assertEquals(3, r.newRepetitions());
    }

    @Test
    void calculate_qualite5_augmenteEF() {
        float ef = 2.5f;
        SM2Result r = sm2.calculate(5, 1, ef, 1);
        assertTrue(r.newEaseFactor() > ef, "Quality=5 doit augmenter l'EF");
    }

    @Test
    void calculate_qualite3_EFResteProcheOuAugmenteLegerement() {
        float ef = 2.5f;
        SM2Result r = sm2.calculate(3, 2, ef, 6);
        // delta pour quality=3 : 0.1 - (5-3)*(0.08+(5-3)*0.02) = 0.1 - 2*(0.08+0.04) = 0.1-0.24 = -0.14
        assertTrue(r.newEaseFactor() >= 1.3f, "EF ne doit pas descendre sous 1.3");
    }

    @Test
    void calculate_efMinimum1Point3_quelle_que_soit_qualite() {
        // Avec ef=1.3 et quality=3, delta sera négatif → plancher à 1.3
        SM2Result r = sm2.calculate(3, 5, 1.3f, 10);
        assertEquals(1.3f, r.newEaseFactor(), 0.001f, "EF plancher = 1.3");
    }

    // ── calculate : chemin échec (quality < 3) ────────────────────────────

    @Test
    void calculate_qualite0_resetRepetitionsEtInterval() {
        SM2Result r = sm2.calculate(0, 5, 2.5f, 30);
        assertEquals(1, r.newIntervalDays(), "Blackout → interval reset à 1");
        assertEquals(0, r.newRepetitions(), "Blackout → reps reset à 0");
        assertEquals(2.5f, r.newEaseFactor(), 0.001f, "Blackout → EF inchangé");
    }

    @Test
    void calculate_qualite2_resetAussi() {
        SM2Result r = sm2.calculate(2, 3, 2.2f, 12);
        assertEquals(1, r.newIntervalDays());
        assertEquals(0, r.newRepetitions());
        assertEquals(2.2f, r.newEaseFactor(), 0.001f);
    }

    // ── calculate : nextReview ─────────────────────────────────────────────

    @Test
    void calculate_nextReview_estDansFutur() {
        SM2Result r = sm2.calculate(4, 0, 2.5f, 1);
        assertTrue(r.nextReview().isAfter(LocalDate.now().minusDays(1)),
                "nextReview doit être aujourd'hui ou dans le futur");
    }

    @Test
    void calculate_intervalUnJour_nextReviewDemain() {
        SM2Result r = sm2.calculate(4, 0, 2.5f, 1);
        assertEquals(LocalDate.now().plusDays(1), r.nextReview());
    }

    // ── getMasteryPercent ─────────────────────────────────────────────────

    @Test
    void getMasteryPercent_zeroReps_faible() {
        int pct = sm2.getMasteryPercent(0, 2.5f);
        // 0*15 + (2.5-1.3)/1.2*40 = 0 + 40 = 40
        assertEquals(40, pct);
    }

    @Test
    void getMasteryPercent_efMinimum_zeroReps_retourne0() {
        int pct = sm2.getMasteryPercent(0, 1.3f);
        // 0*15 + 0 = 0
        assertEquals(0, pct);
    }

    @Test
    void getMasteryPercent_highReps_plafond100() {
        // Avec reps=10 et ef=2.5 : 10*15 + 40 = 190 → capped à 100
        int pct = sm2.getMasteryPercent(10, 2.5f);
        assertEquals(100, pct, "Mastery plafonné à 100");
    }

    @Test
    void getMasteryPercent_progress_incremental() {
        int p0 = sm2.getMasteryPercent(0, 2.5f);
        int p2 = sm2.getMasteryPercent(2, 2.5f);
        int p5 = sm2.getMasteryPercent(5, 2.5f);
        assertTrue(p0 < p2, "Plus de reps = plus de maîtrise");
        assertTrue(p2 < p5);
    }

    // ── Qualité 4 (correct) — vérification formule delta ─────────────────

    @Test
    void calculate_qualite4_efAugmenteDe0() {
        // delta pour quality=4 : 0.1 - (5-4)*(0.08+(5-4)*0.02) = 0.1 - 1*0.1 = 0.0
        float ef = 2.5f;
        SM2Result r = sm2.calculate(4, 2, ef, 6);
        assertEquals(ef, r.newEaseFactor(), 0.001f, "Quality=4 → EF inchangé");
    }
}
