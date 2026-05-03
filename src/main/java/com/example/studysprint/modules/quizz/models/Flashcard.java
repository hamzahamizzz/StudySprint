package com.example.studysprint.modules.quizz.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Maps the `flashcards` table.
 */
public class Flashcard {

    private long id;
    private long deckId;

    private String front;
    private String back;
    private String hint;            // nullable
    private int position;
    private LocalDateTime createdAt;

    // SM-2 spaced-repetition fields
    private float     easeFactor   = 2.5f;
    private int       repetitions  = 0;
    private int       intervalDays = 1;
    private LocalDate nextReview   = LocalDate.now();

    public Flashcard() {}

    public Flashcard(long deckId, String front, String back, int position) {
        this.deckId    = deckId;
        this.front     = front;
        this.back      = back;
        this.position  = position;
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────────

    public long getId()                           { return id; }
    public void setId(long id)                    { this.id = id; }

    public long getDeckId()                       { return deckId; }
    public void setDeckId(long deckId)            { this.deckId = deckId; }

    public String getFront()                      { return front; }
    public void setFront(String front)            { this.front = front; }

    public String getBack()                       { return back; }
    public void setBack(String back)              { this.back = back; }

    public String getHint()                       { return hint; }
    public void setHint(String hint)              { this.hint = hint; }

    public int getPosition()                      { return position; }
    public void setPosition(int position)         { this.position = position; }

    public LocalDateTime getCreatedAt()           { return createdAt; }
    public void setCreatedAt(LocalDateTime t)     { this.createdAt = t; }

    public float getEaseFactor()                  { return easeFactor; }
    public void setEaseFactor(float easeFactor)   { this.easeFactor = easeFactor; }

    public int getRepetitions()                   { return repetitions; }
    public void setRepetitions(int repetitions)   { this.repetitions = repetitions; }

    public int getIntervalDays()                  { return intervalDays; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }

    public LocalDate getNextReview()              { return nextReview; }
    public void setNextReview(LocalDate nextReview) { this.nextReview = nextReview; }

    @Override
    public String toString() {
        return "Flashcard{id=" + id + ", front='" + front + "'}";
    }
}
