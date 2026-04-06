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
@Table(name = "post_comment")
public class PostComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "depth", nullable = false)
    private Integer depth;

    @Column(name = "body", nullable = false, columnDefinition = "LONGTEXT")
    private String body;

    @Column(name = "is_bot", nullable = false)
    private Boolean isBot;

    @Column(name = "bot_name")
    private String botName;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "post_id", nullable = false)
    private Integer postId;

    @Column(name = "author_id", nullable = false)
    private Integer authorId;

    @Column(name = "parent_comment_id")
    private Integer parentCommentId;

    @ManyToOne
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private GroupPost post;

    @ManyToOne
    @JoinColumn(name = "author_id", insertable = false, updatable = false)
    private User author;

    @ManyToOne
    @JoinColumn(name = "parent_comment_id", insertable = false, updatable = false)
    private PostComment parentComment;

    public PostComment() {
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

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public PostComment getParentComment() {
        return parentComment;
    }

    public void setParentComment(PostComment parentComment) {
        this.parentComment = parentComment;
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
