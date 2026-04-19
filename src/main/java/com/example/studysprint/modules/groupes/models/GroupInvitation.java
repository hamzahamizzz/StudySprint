package com.example.studysprint.modules.groupes.models;

import com.example.studysprint.modules.utilisateurs.models.Utilisateur;

import java.sql.Timestamp;

public class GroupInvitation {
    private Integer id;
    private String email;
    private Timestamp invitedAt;
    private String code;
    private String status;
    private String role;
    private Timestamp respondedAt;
    private String token;
    private String message;
    private Timestamp expiresAt;
    private Integer groupId;
    private Integer invitedById;
    private StudyGroup group;
    private Utilisateur invitedBy;

    public GroupInvitation() {
    }

    public GroupInvitation(Integer id, String email, Timestamp invitedAt, String code, String status,
                           String role, Timestamp respondedAt, String token, String message,
                           Timestamp expiresAt, Integer groupId, Integer invitedById) {
        this.id = id;
        this.email = email;
        this.invitedAt = invitedAt;
        this.code = code;
        this.status = status;
        this.role = role;
        this.respondedAt = respondedAt;
        this.token = token;
        this.message = message;
        this.expiresAt = expiresAt;
        this.groupId = groupId;
        this.invitedById = invitedById;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Timestamp getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(Timestamp invitedAt) {
        this.invitedAt = invitedAt;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Timestamp getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Timestamp respondedAt) {
        this.respondedAt = respondedAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getInvitedById() {
        return invitedById;
    }

    public void setInvitedById(Integer invitedById) {
        this.invitedById = invitedById;
    }

    public StudyGroup getGroup() {
        return group;
    }

    public void setGroup(StudyGroup group) {
        this.group = group;
    }

    public Utilisateur getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(Utilisateur invitedBy) {
        this.invitedBy = invitedBy;
    }

    @Override
    public String toString() {
        return "GroupInvitation{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", invitedAt=" + invitedAt +
                ", code='" + code + '\'' +
                ", status='" + status + '\'' +
                ", role='" + role + '\'' +
                ", respondedAt=" + respondedAt +
                ", token='" + token + '\'' +
                ", message='" + message + '\'' +
                ", expiresAt=" + expiresAt +
                ", groupId=" + groupId +
                ", invitedById=" + invitedById +
                '}';
    }
}
