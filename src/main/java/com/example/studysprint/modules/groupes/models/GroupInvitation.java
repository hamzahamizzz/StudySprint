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
@Table(name = "group_invitation")
public class GroupInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "invited_at", nullable = false)
    private Timestamp invitedAt;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "responded_at")
    private Timestamp respondedAt;

    @Column(name = "token")
    private String token;

    @Column(name = "message", columnDefinition = "LONGTEXT")
    private String message;

    @Column(name = "expires_at")
    private Timestamp expiresAt;

    @Column(name = "group_id", nullable = false)
    private Integer groupId;

    @Column(name = "invited_by_id")
    private Integer invitedById;

    @ManyToOne
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private StudyGroup group;

    @ManyToOne
    @JoinColumn(name = "invited_by_id", insertable = false, updatable = false)
    private User invitedBy;

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

    public User getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(User invitedBy) {
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
