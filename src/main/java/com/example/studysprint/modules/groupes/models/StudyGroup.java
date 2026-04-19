package com.example.studysprint.modules.groupes.models;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;

import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;

public class StudyGroup {
    private Integer id;
    private String name;
    private String description;
    private String privacy;
    private String subject;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp lastActivity;
    private Integer createdById;
    private Utilisateur createdBy;
    private List<GroupMember> members;
    private List<GroupPost> posts;
    private List<GroupInvitation> invitations;

    public StudyGroup() {
        this.members = new ArrayList<>();
        this.posts = new ArrayList<>();
        this.invitations = new ArrayList<>();
    }

    public StudyGroup(Integer id, String name, String description, String privacy, String subject,
                      Timestamp createdAt, Timestamp updatedAt, Timestamp lastActivity, Integer createdById) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.privacy = privacy;
        this.subject = subject;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastActivity = lastActivity;
        this.createdById = createdById;
        this.members = new ArrayList<>();
        this.posts = new ArrayList<>();
        this.invitations = new ArrayList<>();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Timestamp getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Timestamp lastActivity) {
        this.lastActivity = lastActivity;
    }

    public Integer getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Integer createdById) {
        this.createdById = createdById;
    }

    public Utilisateur getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Utilisateur createdBy) {
        this.createdBy = createdBy;
    }

    public List<GroupMember> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMember> members) {
        this.members = members;
    }

    public List<GroupPost> getPosts() {
        return posts;
    }

    public void setPosts(List<GroupPost> posts) {
        this.posts = posts;
    }

    public List<GroupInvitation> getInvitations() {
        return invitations;
    }

    public void setInvitations(List<GroupInvitation> invitations) {
        this.invitations = invitations;
    }

    @Override
    public String toString() {
        return "StudyGroup{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", privacy='" + privacy + '\'' +
                ", subject='" + subject + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", lastActivity=" + lastActivity +
                ", createdById=" + createdById +
                '}';
    }
}
