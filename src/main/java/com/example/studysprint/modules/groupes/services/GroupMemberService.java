package com.example.studysprint.modules.groupes.services;

import com.example.studysprint.modules.groupes.models.GroupMember;
import com.example.studysprint.utils.MyDataBase;

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
    private static boolean cacheInvalide = true;

    public GroupMemberService() {
        this.connection = MyDataBase.getInstance().getCnx();
    }
    public List<GroupMember> getAll() {
        if (cache == null || cacheInvalide) {
            cache = getAllFromDB();
            cacheInvalide = false;
        }
        return cache;
    }

    private List<GroupMember> getAllFromDB() {
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

    private static void setCacheInvalide() {
        cacheInvalide = true;
    }

    public List<GroupMember> getByGroup(int groupId) {
        return getAll().stream()
                .filter(m -> m.getGroupId() == groupId)
                .toList();
    }

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

        setCacheInvalide();
    }

    public void delete(int id) {
        String sql = "DELETE FROM group_members WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete group member", e);
        }

        setCacheInvalide();
    }

    public void removeByGroupAndUser(int groupId, int userId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove member", e);
        }

        setCacheInvalide();
    }

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

        setCacheInvalide();
    }

    public int countMembersForGroup(int groupId) {
        return getByGroup(groupId).size();
    }

    public Optional<String> getMemberRoleForUser(int groupId, int userId) {
        return getAll().stream()
                .filter(m -> m.getGroupId() == groupId && m.getUserId() == userId)
                .map(GroupMember::getMemberRole)
                .findFirst();
    }

    public List<GroupMember> filterMembersByRole(String role) {
        return getAll().stream()
                .filter(m -> m.getMemberRole() != null && m.getMemberRole().equalsIgnoreCase(role))
                .toList();
    }

    public long countMembersWithRole(String role) {
        return getAll().stream()
                .filter(m -> m.getMemberRole() != null && m.getMemberRole().equalsIgnoreCase(role))
                .count();
    }

    private GroupMember mapRowToMember(ResultSet rs) throws SQLException {
        return new GroupMember(
                rs.getInt("id"),
                rs.getString("member_role"),
                rs.getTimestamp("joined_at"),
                rs.getInt("group_id"),
                rs.getInt("user_id")
        );
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "member";
        }

        return role.trim().toLowerCase(Locale.ROOT);
    }
}
