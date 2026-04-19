package com.example.studysprint.modules.groupes.models;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;

import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;

public class PostComment {
    private Integer id;
    private Integer depth;
    private String body;
    private Boolean isBot;
    private String botName;
    private Timestamp createdAt;
    private Integer postId;
    private Integer authorId;
    private Integer parentCommentId;
    private GroupPost post;
    private Utilisateur author;
    private PostComment parentComment;
    private List<PostComment> replies;

    public PostComment() {
        this.replies = new ArrayList<>();
    }

    public PostComment(Integer id, Integer depth, String body, Boolean isBot, String botName,
                       Timestamp createdAt, Integer postId, Integer authorId, Integer parentCommentId) {
        this.id = id;
        this.depth = depth;
        this.body = body;
        this.isBot = isBot;
        this.botName = botName;
        this.createdAt = createdAt;
        this.postId = postId;
        this.authorId = authorId;
        this.parentCommentId = parentCommentId;
        this.replies = new ArrayList<>();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Boolean getIsBot() {
        return isBot;
    }

    public void setIsBot(Boolean bot) {
        isBot = bot;
    }

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public Integer getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Integer parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public GroupPost getPost() {
        return post;
    }

    public void setPost(GroupPost post) {
        this.post = post;
    }

    public Utilisateur getAuthor() {
        return author;
    }

    public void setAuthor(Utilisateur author) {
        this.author = author;
    }

    public PostComment getParentComment() {
        return parentComment;
    }

    public void setParentComment(PostComment parentComment) {
        this.parentComment = parentComment;
    }

    public List<PostComment> getReplies() {
        return replies;
    }

    public void setReplies(List<PostComment> replies) {
        this.replies = replies;
    }

    @Override
    public String toString() {
        return "PostComment{" +
                "id=" + id +
                ", depth=" + depth +
                ", body='" + body + '\'' +
                ", isBot=" + isBot +
                ", botName='" + botName + '\'' +
                ", createdAt=" + createdAt +
                ", postId=" + postId +
                ", authorId=" + authorId +
                ", parentCommentId=" + parentCommentId +
                '}';
    }
}
