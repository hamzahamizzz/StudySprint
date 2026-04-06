package com.example.studysprint.modules.groupes.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.sql.Timestamp;

@Entity
@Table(name = "group_posts")
public class GroupPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "post_type", nullable = false)
    private String postType;

    @Column(name = "title")
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "LONGTEXT")
    private String body;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(name = "ai_summary", columnDefinition = "LONGTEXT")
    private String aiSummary;

    @Column(name = "ai_category")
    private String aiCategory;

    @Column(name = "ai_tags", columnDefinition = "LONGTEXT")
    private String aiTags;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "group_id", nullable = false)
    private Integer groupId;

    @Column(name = "author_id", nullable = false)
    private Integer authorId;

    @Column(name = "parent_post_id")
    private Integer parentPostId;

    @ManyToOne
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private StudyGroup group;

    @ManyToOne
    @JoinColumn(name = "author_id", insertable = false, updatable = false)
    private User author;

    @ManyToOne
    @JoinColumn(name = "parent_post_id", insertable = false, updatable = false)
    private GroupPost parentPost;

    public GroupPost() {
    }

    public GroupPost(Integer id, String postType, String title, String body, String attachmentUrl,
                     String aiSummary, String aiCategory, String aiTags, Timestamp createdAt,
                     Integer groupId, Integer authorId, Integer parentPostId) {
        this.id = id;
        this.postType = postType;
        this.title = title;
        this.body = body;
        this.attachmentUrl = attachmentUrl;
        this.aiSummary = aiSummary;
        this.aiCategory = aiCategory;
        this.aiTags = aiTags;
        this.createdAt = createdAt;
        this.groupId = groupId;
        this.authorId = authorId;
        this.parentPostId = parentPostId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getAiCategory() {
        return aiCategory;
    }

    public void setAiCategory(String aiCategory) {
        this.aiCategory = aiCategory;
    }

    public String getAiTags() {
        return aiTags;
    }

    public void setAiTags(String aiTags) {
        this.aiTags = aiTags;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public Integer getParentPostId() {
        return parentPostId;
    }

    public void setParentPostId(Integer parentPostId) {
        this.parentPostId = parentPostId;
    }

    public StudyGroup getGroup() {
        return group;
    }

    public void setGroup(StudyGroup group) {
        this.group = group;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public GroupPost getParentPost() {
        return parentPost;
    }

    public void setParentPost(GroupPost parentPost) {
        this.parentPost = parentPost;
    }

    @Override
    public String toString() {
        return "GroupPost{" +
                "id=" + id +
                ", postType='" + postType + '\'' +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", attachmentUrl='" + attachmentUrl + '\'' +
                ", aiSummary='" + aiSummary + '\'' +
                ", aiCategory='" + aiCategory + '\'' +
                ", aiTags='" + aiTags + '\'' +
                ", createdAt=" + createdAt +
                ", groupId=" + groupId +
                ", authorId=" + authorId +
                ", parentPostId=" + parentPostId +
                '}';
    }
}
