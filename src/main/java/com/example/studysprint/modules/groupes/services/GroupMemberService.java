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
import java.util.Locale;

public class GroupMemberService {
    private final Connection connection;
    private static List<GroupMember> cache;
    private static boolean cacheDirty = true;

    // Initialize database connection for member operations
    public GroupMemberService() {
        this.connection = MyDatabase.getConnection();
    }

    // Retrieve all group members
    public List<GroupMember> getAll() {
        if (cache == null || cacheDirty) {
            cache = fetchAllFromDatabase();
            cacheDirty = false;
        }
        return cache;
    }

    // Fetch all members from the database (used to refresh the cache).
    private List<GroupMember> fetchAllFromDatabase() {
        String sql = "SELECT * FROM group_members ORDER BY joined_at DESC";
        List<GroupMember> members = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                members.add(mapRowToMember(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all group members", e);
        }

        return members;
    }

    // Mark the in-memory cache as dirty.
    private static void markCacheDirty() {
        cacheDirty = true;
    }

    // Get all members of a specific group
    public List<GroupMember> getByGroup(int groupId) {
        return getAll().stream()
                .filter(m -> m.getGroupId() == groupId)
                .toList();
    }

    // Insert a new member into a group
    public void add(GroupMember m) {
        String sql = "INSERT INTO group_members (member_role, joined_at, group_id, user_id) VALUES (?, ?, ?, ?)";
        Timestamp joinedAt = m.getJoinedAt() != null ? m.getJoinedAt() : new Timestamp(System.currentTimeMillis());
        String normalizedRole = normalizeRole(m.getMemberRole());

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalizedRole);
            ps.setTimestamp(2, joinedAt);
            ps.setInt(3, m.getGroupId());
            ps.setInt(4, m.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add group member", e);
        }

        markCacheDirty();
    }

    // Delete a group member by identifier
    public void delete(int id) {
        String sql = "DELETE FROM group_members WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete group member", e);
        }

        markCacheDirty();
    }

    // Update member role by identifier
    public void updateRole(int id, String role) {
        String sql = "UPDATE group_members SET member_role = ? WHERE id = ?";
        String normalizedRole = normalizeRole(role);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalizedRole);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update member role", e);
        }

        markCacheDirty();
    }

    // Count members in a group.
    public int countMembersForGroup(int groupId) {
        return getByGroup(groupId).size();
    }

    // Get member role for a user in a group.
    public Optional<String> getMemberRoleForUser(int groupId, int userId) {
        return getAll().stream()
                .filter(m -> m.getGroupId() == groupId && m.getUserId() == userId)
                .map(GroupMember::getMemberRole)
                .findFirst();
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

    // Map a SQL result row to GroupMember model
    private GroupMember mapRowToMember(ResultSet rs) throws SQLException {
        return new GroupMember(
                rs.getInt("id"),
                rs.getString("member_role"),
                rs.getTimestamp("joined_at"),
                rs.getInt("group_id"),
                rs.getInt("user_id")
        );
    }

    // Normalize role values before saving to the database.
    private String normalizeRole(String role) {
        if (role == null) {
            return "member";
        }

        return role.trim().toLowerCase(Locale.ROOT);
    }
}
