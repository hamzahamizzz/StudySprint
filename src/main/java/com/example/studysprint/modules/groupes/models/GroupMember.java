package com.example.studysprint.modules.groupes.models;

import java.sql.Timestamp;

public class GroupMember {
    private Integer id;
    private String memberRole;
    private Timestamp joinedAt;
    private Integer groupId;
    private Integer userId;
    private StudyGroup group;
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
