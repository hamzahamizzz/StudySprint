package com.example.studysprint.modules.groupes.services;

import com.example.studysprint.modules.groupes.models.GroupInvitation;
import com.example.studysprint.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class GroupInvitationService {
    private final Connection connection;

    public GroupInvitationService() {
        this.connection = MyDatabase.getConnection();
    }

    // Retrieve all group invitations
    public List<GroupInvitation> getAll() {
        String sql = "SELECT * FROM group_invitation ORDER BY invited_at DESC";
        List<GroupInvitation> invitations = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                invitations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all group invitations", e);
        }

        return invitations;
    }

    public List<GroupInvitation> getByGroup(int groupId) {
        String sql = "SELECT * FROM group_invitation WHERE group_id = ? ORDER BY invited_at DESC";
        List<GroupInvitation> invitations = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    invitations.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch group invitations", e);
        }

        return invitations;
    }

    public void add(GroupInvitation inv) {
        String sql = "INSERT INTO group_invitation (email, invited_at, code, status, role, responded_at, token, message, expires_at, group_id, invited_by_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Timestamp invitedAt = inv.getInvitedAt() != null ? inv.getInvitedAt() : new Timestamp(System.currentTimeMillis());

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, inv.getEmail());
            ps.setTimestamp(2, invitedAt);
            ps.setString(3, inv.getCode());
            ps.setString(4, inv.getStatus());
            ps.setString(5, inv.getRole());
            ps.setTimestamp(6, inv.getRespondedAt());
            ps.setString(7, inv.getToken());
            ps.setString(8, inv.getMessage());
            ps.setTimestamp(9, inv.getExpiresAt());
            ps.setInt(10, inv.getGroupId());
            ps.setInt(11, inv.getInvitedById());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add group invitation", e);
        }
    }

    public void updateStatus(int id, String status) {
        String sql = "UPDATE group_invitation SET status = ?, responded_at = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update invitation status", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM group_invitation WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete group invitation", e);
        }
    }

    // Filter invitations by status (Pending, Accepted, Declined, Expired)
    public List<GroupInvitation> filterByStatus(String status) {
        return getAll().stream()
                .filter(inv -> inv.getStatus() != null && inv.getStatus().equalsIgnoreCase(status))
                .toList();
    }

    // Filter invitations by role offered
    public List<GroupInvitation> filterByRole(String role) {
        return getAll().stream()
                .filter(inv -> inv.getRole() != null && inv.getRole().equalsIgnoreCase(role))
                .toList();
    }

    // Get all expired invitations
    public List<GroupInvitation> getExpiredInvitations() {
        long now = System.currentTimeMillis();
        return getAll().stream()
                .filter(inv -> inv.getExpiresAt() != null && inv.getExpiresAt().getTime() < now)
                .toList();
    }

    // Count pending invitations for a group
    public long countPendingInvitations(int groupId) {
        return getAll().stream()
                .filter(inv -> inv.getGroupId() == groupId && inv.getStatus() != null && inv.getStatus().equalsIgnoreCase("PENDING"))
                .count();
    }

    private GroupInvitation mapRow(ResultSet rs) throws SQLException {
        return new GroupInvitation(
                rs.getInt("id"),
                rs.getString("email"),
                rs.getTimestamp("invited_at"),
                rs.getString("code"),
                rs.getString("status"),
                rs.getString("role"),
                rs.getTimestamp("responded_at"),
                rs.getString("token"),
                rs.getString("message"),
                rs.getTimestamp("expires_at"),
                rs.getInt("group_id"),
                rs.getInt("invited_by_id")
        );
    }
}
