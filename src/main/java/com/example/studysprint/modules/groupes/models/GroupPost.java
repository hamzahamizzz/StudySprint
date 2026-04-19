package com.example.studysprint.modules.groupes.models;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;

import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;

public class GroupPost {
    private Integer id;
    private String postType;
    private String title;
    private String body;
    private String attachmentUrl;
    private String aiSummary;
    private String aiCategory;
    private String aiTags;
    private Timestamp createdAt;
    private Integer groupId;
    private Integer authorId;
    private Integer parentPostId;
    private StudyGroup group;
    private Utilisateur author;
    private GroupPost parentPost;
    private List<PostComment> comments;
    private List<PostLike> likes;
    private List<PostRating> ratings;

    public GroupPost() {
        this.comments = new ArrayList<>();
        this.likes = new ArrayList<>();
        this.ratings = new ArrayList<>();
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
        this.comments = new ArrayList<>();
        this.likes = new ArrayList<>();
        this.ratings = new ArrayList<>();
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

    public Utilisateur getAuthor() {
        return author;
    }

    public void setAuthor(Utilisateur author) {
        this.author = author;
    }

    public GroupPost getParentPost() {
        return parentPost;
    }

    public void setParentPost(GroupPost parentPost) {
        this.parentPost = parentPost;
    }

    public List<PostComment> getComments() {
        return comments;
    }

    public void setComments(List<PostComment> comments) {
        this.comments = comments;
    }

    public List<PostLike> getLikes() {
        return likes;
    }

    public void setLikes(List<PostLike> likes) {
        this.likes = likes;
    }

    public List<PostRating> getRatings() {
        return ratings;
    }

    public void setRatings(List<PostRating> ratings) {
        this.ratings = ratings;
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
