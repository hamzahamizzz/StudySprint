package com.example.studysprint.modules.quizz.models;

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

    @Override
    public String toString() {
        return "Flashcard{id=" + id + ", front='" + front + "'}";
    }
}
