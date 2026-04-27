package com.example.studysprint.modules.quizz.models;

/**
 * Maps the `flashcard_decks` table.
 */
public class FlashcardDeck {

    private long id;
    private long ownerId;
    private long subjectId;
    private Long chapterId;      // nullable
    private String title;
    private String cards;           // raw JSON string
    private boolean published;
    private boolean generatedByAi;
    private String templateKey;     // nullable
    private String aiMeta;          // nullable JSON string

    public FlashcardDeck() {}

    public FlashcardDeck(long ownerId, long subjectId, String title) {
        this.ownerId   = ownerId;
        this.subjectId = subjectId;
        this.title     = title;
    }

    // ── Getters & Setters ──────────────────────────────────────────────

    public long getId()                           { return id; }
    public void setId(long id)                    { this.id = id; }

    public long getOwnerId()                      { return ownerId; }
    public void setOwnerId(long ownerId)          { this.ownerId = ownerId; }

    public long getSubjectId()                    { return subjectId; }
    public void setSubjectId(long subjectId)      { this.subjectId = subjectId; }

    public Long getChapterId()                    { return chapterId; }
    public void setChapterId(Long chapterId)      { this.chapterId = chapterId; }

    public String getTitle()                      { return title; }
    public void setTitle(String title)            { this.title = title; }

    public String getCards()                      { return cards; }
    public void setCards(String cards)            { this.cards = cards; }

    public boolean isPublished()                  { return published; }
    public void setPublished(boolean published)   { this.published = published; }

    public boolean isGeneratedByAi()              { return generatedByAi; }
    public void setGeneratedByAi(boolean v)       { this.generatedByAi = v; }

    public String getTemplateKey()                { return templateKey; }
    public void setTemplateKey(String k)          { this.templateKey = k; }

    public String getAiMeta()                     { return aiMeta; }
    public void setAiMeta(String aiMeta)          { this.aiMeta = aiMeta; }

    @Override
    public String toString() {
        return "FlashcardDeck{id=" + id + ", title='" + title + "'}";
    }
}
