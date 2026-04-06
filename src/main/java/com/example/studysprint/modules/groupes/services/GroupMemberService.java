package com.example.studysprint.modules.groupes.services;

import com.example.studysprint.modules.groupes.models.GroupMember;
import com.example.studysprint.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GroupMemberService {
    private final Connection connection;

    public GroupMemberService() {
        this.connection = MyDatabase.getConnection();
    }

    // Retrieve all group members
    public List<GroupMember> getAll() {
        String sql = "SELECT * FROM group_members ORDER BY joined_at DESC";
        List<GroupMember> members = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                members.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all group members", e);
        }

        return members;
    }

    // Get all members of a specific group
    public List<GroupMember> getByGroup(int groupId) {
        String sql = "SELECT * FROM group_members WHERE group_id = ? ORDER BY joined_at DESC";
        List<GroupMember> members = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch group members", e);
        }

        return members;
    }

    public void add(GroupMember m) {
        String sql = "INSERT INTO group_members (member_role, joined_at, group_id, user_id) VALUES (?, ?, ?, ?)";
        Timestamp joinedAt = m.getJoinedAt() != null ? m.getJoinedAt() : new Timestamp(System.currentTimeMillis());

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, m.getMemberRole());
            ps.setTimestamp(2, joinedAt);
            ps.setInt(3, m.getGroupId());
            ps.setInt(4, m.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add group member", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM group_members WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete group member", e);
        }
    }

    // Count total members in a specific group
    public int countMembersForGroup(int groupId) {
        String sql = "SELECT COUNT(*) FROM group_members WHERE group_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count group members", e);
        }

        return 0;
    }

    // Get role of user in group (returns Optional)
    public java.util.Optional<String> getMemberRoleForUser(int groupId, int userId) {
        String sql = "SELECT member_role FROM group_members WHERE group_id = ? AND user_id = ? LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("member_role"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch member role", e);
        }

        return Optional.empty();
    }

    // Filter members by role (Admin, Moderator, Member)
    public List<GroupMember> filterMembersByRole(String role) {
        return getAll().stream()
                .filter(m -> m.getMemberRole() != null && m.getMemberRole().equalsIgnoreCase(role))
                .toList();
    }

    // Count members with specific role across all groups
    public long countMembersWithRole(String role) {
        return getAll().stream()
                .filter(m -> m.getMemberRole() != null && m.getMemberRole().equalsIgnoreCase(role))
                .count();
    }

    // Get all members of a group (stream-based alternative)
    public List<GroupMember> getMembersForGroup(int groupId) {
        return getAll().stream()
                .filter(m -> m.getGroupId() == groupId)
                .toList();
    }

    private GroupMember mapRow(ResultSet rs) throws SQLException {
        return new GroupMember(
                rs.getInt("id"),
                rs.getString("member_role"),
                rs.getTimestamp("joined_at"),
                rs.getInt("group_id"),
                rs.getInt("user_id")
        );
    }
}
