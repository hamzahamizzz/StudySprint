package com.example.studysprint.modules.quizz.models;

import java.time.LocalDateTime;

/**
 * Maps the `quiz_attempts` table.
 */
public class QuizAttempt {

    private long id;
    private long userId;
    private long quizId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;  // nullable
    private Double score;               // nullable DECIMAL 5,2
    private int totalQuestions;
    private int correctCount;
    private Integer durationSeconds;    // nullable

    public QuizAttempt() {}

    public QuizAttempt(long userId, long quizId, LocalDateTime startedAt, int totalQuestions) {
        this.userId         = userId;
        this.quizId         = quizId;
        this.startedAt      = startedAt;
        this.totalQuestions = totalQuestions;
    }

    // ── Getters & Setters ──────────────────────────────────────────────

    public long getId()                               { return id; }
    public void setId(long id)                        { this.id = id; }

    public long getUserId()                           { return userId; }
    public void setUserId(long userId)                { this.userId = userId; }

    public long getQuizId()                           { return quizId; }
    public void setQuizId(long quizId)                { this.quizId = quizId; }

    public LocalDateTime getStartedAt()               { return startedAt; }
    public void setStartedAt(LocalDateTime t)         { this.startedAt = t; }

    public LocalDateTime getCompletedAt()             { return completedAt; }
    public void setCompletedAt(LocalDateTime t)       { this.completedAt = t; }

    public Double getScore()                          { return score; }
    public void setScore(Double score)                { this.score = score; }

    public int getTotalQuestions()                    { return totalQuestions; }
    public void setTotalQuestions(int n)              { this.totalQuestions = n; }

    public int getCorrectCount()                      { return correctCount; }
    public void setCorrectCount(int n)                { this.correctCount = n; }

    public Integer getDurationSeconds()               { return durationSeconds; }
    public void setDurationSeconds(Integer d)         { this.durationSeconds = d; }

    @Override
    public String toString() {
        return "QuizAttempt{id=" + id + ", quizId=" + quizId + ", score=" + score + "}";
    }
}
