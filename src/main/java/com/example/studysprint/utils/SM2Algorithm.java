package com.example.studysprint.utils;

// SQL migration — run once:
// ALTER TABLE flashcards
//   ADD COLUMN ease_factor    FLOAT   DEFAULT 2.5,
//   ADD COLUMN interval_days  INT     DEFAULT 1,
//   ADD COLUMN repetitions    INT     DEFAULT 0,
//   ADD COLUMN next_review    DATE    DEFAULT (CURDATE());

import java.time.LocalDate;

public class SM2Algorithm {

    public record SM2Result(int newIntervalDays, float newEaseFactor,
                            int newRepetitions, LocalDate nextReview) {}

    public SM2Result calculate(int quality, int repetitions, float easeFactor, int intervalDays) {
        int   newInterval;
        float newEF;
        int   newReps;

        if (quality >= 3) {
            newInterval = switch (repetitions) {
                case 0  -> 1;
                case 1  -> 6;
                default -> Math.round(intervalDays * easeFactor);
            };
            float delta = 0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f);
            newEF   = Math.max(1.3f, easeFactor + delta);
            newReps = repetitions + 1;
        } else {
            newInterval = 1;
            newEF       = easeFactor;   // unchanged on failure
            newReps     = 0;
        }

        LocalDate nextReview = LocalDate.now().plusDays(newInterval);
        return new SM2Result(newInterval, newEF, newReps, nextReview);
    }

    /** Returns a 0-100 mastery score based on repetitions and ease factor. */
    public int getMasteryPercent(int reps, float ef) {
        return Math.min(100, reps * 15 + (int) ((ef - 1.3f) / 1.2f * 40));
    }
}
