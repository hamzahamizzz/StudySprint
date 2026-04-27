package com.example.studysprint.modules.quizz.models;

/**
 * Maps the `quizzes` table.
 * The `questions` column is stored as a JSON string in DB.
 */
public class Quiz {

    private long id;
    private long ownerId;
    private long subjectId;
    private Long chapterId;           // nullable
    private String title;
    private Difficulty difficulty;    // EASY | MEDIUM | HARD
    private String questions;         // raw JSON string
    private boolean published;
    private boolean generatedByAi;
    private String templateKey;       // nullable
    private String aiMeta;            // nullable JSON string

    private transient int questionCount = -1;

    public Quiz() {}

    public Quiz(long ownerId, long subjectId, String title, Difficulty difficulty, String questions) {
        this.ownerId    = ownerId;
        this.subjectId  = subjectId;
        this.title      = title;
        this.difficulty = difficulty;
        this.questions  = questions;
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

    public Difficulty getDifficulty()             { return difficulty; }
    public void setDifficulty(Difficulty d)       { this.difficulty = d; }

    public String getQuestions()                  { return questions; }
    public void setQuestions(String questions)    {
        this.questions = questions;
        this.questionCount = -1; // reset cache
    }

    public boolean isPublished()                  { return published; }
    public void setPublished(boolean published)   { this.published = published; }

    public boolean isGeneratedByAi()              { return generatedByAi; }
    public void setGeneratedByAi(boolean v)       { this.generatedByAi = v; }

    public String getTemplateKey()                { return templateKey; }
    public void setTemplateKey(String k)          { this.templateKey = k; }

    public String getAiMeta()                     { return aiMeta; }
    public void setAiMeta(String aiMeta)          { this.aiMeta = aiMeta; }

    public int getQuestionCount() {
        if (questionCount >= 0) return questionCount;
        if (questions == null || questions.isBlank()) { questionCount = 0; return 0; }
        try {
            com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode n = m.readTree(questions);
            questionCount = n.isArray() ? n.size() : 0;
        } catch (Exception e) { questionCount = 0; }
        return questionCount;
    }

    @Override
    public String toString() {
        return "Quiz{id=" + id + ", title='" + title + "', difficulty=" + (difficulty != null ? difficulty.name() : "null") + "}";
    }
}
