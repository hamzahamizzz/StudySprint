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
@Table(name = "group_members")
public class GroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "member_role", nullable = false)
    private String memberRole;

    @Column(name = "joined_at", nullable = false)
    private Timestamp joinedAt;

    @Column(name = "group_id", nullable = false)
    private Integer groupId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @ManyToOne
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private StudyGroup group;

    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    public GroupMember() {
    }

    public GroupMember(Integer id, String memberRole, Timestamp joinedAt, Integer groupId, Integer userId) {
        this.id = id;
        this.memberRole = memberRole;
        this.joinedAt = joinedAt;
        this.groupId = groupId;
        this.userId = userId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(String memberRole) {
        this.memberRole = memberRole;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public StudyGroup getGroup() {
        return group;
    }

    public void setGroup(StudyGroup group) {
        this.group = group;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "GroupMember{" +
                "id=" + id +
                ", memberRole='" + memberRole + '\'' +
                ", joinedAt=" + joinedAt +
                ", groupId=" + groupId +
                ", userId=" + userId +
                '}';
    }
}
