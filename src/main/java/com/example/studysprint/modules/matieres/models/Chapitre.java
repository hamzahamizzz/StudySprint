package com.example.studysprint.modules.matieres.models;

import java.sql.Timestamp;

public class Chapitre {
    private Integer id;
    private String title;
    private Integer orderNo;
    private String summary;
    private String content;
    private String attachmentUrl;
    private String aiSummary;
    private String aiKeyPoint;
    private String aiTags;
    private Timestamp createdAt;
    private Integer subjectId;
    private Integer createdById;

    public Chapitre() {}

    public Chapitre(Integer id, String title, Integer orderNo, String summary, String content,
                    String attachmentUrl, String aiSummary, String aiKeyPoint, String aiTags,
                    Timestamp createdAt, Integer subjectId, Integer createdById) {
        this.id = id;
        this.title = title;
        this.orderNo = orderNo;
        this.summary = summary;
        this.content = content;
        this.attachmentUrl = attachmentUrl;
        this.aiSummary = aiSummary;
        this.aiKeyPoint = aiKeyPoint;
        this.aiTags = aiTags;
        this.createdAt = createdAt;
        this.subjectId = subjectId;
        this.createdById = createdById;
    }

    // Getters et setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public String getAiKeyPoint() { return aiKeyPoint; }
    public void setAiKeyPoint(String aiKeyPoint) { this.aiKeyPoint = aiKeyPoint; }

    public String getAiTags() { return aiTags; }
    public void setAiTags(String aiTags) { this.aiTags = aiTags; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }

    public Integer getCreatedById() { return createdById; }
    public void setCreatedById(Integer createdById) { this.createdById = createdById; }
}