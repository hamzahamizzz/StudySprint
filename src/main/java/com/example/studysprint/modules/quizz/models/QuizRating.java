package com.example.studysprint.modules.quizz.models;

import java.time.LocalDateTime;

/**
 * Maps the `quiz_ratings` table.
 * UNIQUE constraint: (user_id, quiz_id)
 */
public class QuizRating {

    private long id;
    private long userId;
    private long quizId;
    private int score;              // 1–5
    private LocalDateTime createdAt;

    public QuizRating() {}

    public QuizRating(long userId, long quizId, int score) {
        this.userId    = userId;
        this.quizId    = quizId;
        this.score     = score;
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────────

    public long getId()                           { return id; }
    public void setId(long id)                    { this.id = id; }

    public long getUserId()                       { return userId; }
    public void setUserId(long userId)            { this.userId = userId; }

    public long getQuizId()                       { return quizId; }
    public void setQuizId(long quizId)            { this.quizId = quizId; }

    public int getScore()                         { return score; }
    public void setScore(int score) {
        if (score < 1 || score > 5) throw new IllegalArgumentException("Score doit être entre 1 et 5");
        this.score = score;
    }

    public LocalDateTime getCreatedAt()           { return createdAt; }
    public void setCreatedAt(LocalDateTime t)     { this.createdAt = t; }

    @Override
    public String toString() {
        return "QuizRating{id=" + id + ", quizId=" + quizId + ", score=" + score + "}";
    }
}
