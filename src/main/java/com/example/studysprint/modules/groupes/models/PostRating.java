package com.example.studysprint.modules.groupes.models;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;

import java.sql.Timestamp;

public class PostRating {
    private Integer id;
    private Short rating;
    private Timestamp createdAt;
    private Integer postId;
    private Integer userId;
    private GroupPost post;
    private Utilisateur user;

    public PostRating() {
    }

    public PostRating(Integer id, Short rating, Timestamp createdAt, Integer postId, Integer userId) {
        this.id = id;
        this.rating = rating;
        this.createdAt = createdAt;
        this.postId = postId;
        this.userId = userId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Short getRating() {
        return rating;
    }

    public void setRating(Short rating) {
        this.rating = rating;
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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public GroupPost getPost() {
        return post;
    }

    public void setPost(GroupPost post) {
        this.post = post;
    }

    public Utilisateur getUser() {
        return user;
    }

    public void setUser(Utilisateur user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "PostRating{" +
                "id=" + id +
                ", rating=" + rating +
                ", createdAt=" + createdAt +
                ", postId=" + postId +
                ", userId=" + userId +
                '}';
    }
}
