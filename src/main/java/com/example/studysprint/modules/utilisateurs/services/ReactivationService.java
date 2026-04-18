package com.example.studysprint.modules.utilisateurs.services;

import com.example.studysprint.modules.utilisateurs.models.ReactivationRequest;
import com.example.studysprint.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReactivationService {
    private final Connection cnx;

    public ReactivationService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    public boolean hasPendingRequest(int userId) {
        String req = "SELECT COUNT(*) FROM reactivation_requests WHERE user_id = ? AND status = 'PENDING'";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void submitRequest(int userId, String reason) {
        if (hasPendingRequest(userId)) return;

        String req = "INSERT INTO reactivation_requests (user_id, reason) VALUES (?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, userId);
            pst.setString(2, reason);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getPendingCount() {
        String req = "SELECT COUNT(*) FROM reactivation_requests WHERE status = 'PENDING'";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<ReactivationRequest> getPendingRequests() {
        List<ReactivationRequest> list = new ArrayList<>();
        String req = "SELECT r.*, u.email, u.nom, u.prenom FROM reactivation_requests r " +
                     "JOIN user u ON r.user_id = u.id " +
                     "WHERE r.status = 'PENDING' ORDER BY r.created_at DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                ReactivationRequest r = new ReactivationRequest();
                r.setId(rs.getInt("id"));
                r.setUserId(rs.getInt("user_id"));
                r.setReason(rs.getString("reason"));
                r.setStatus(rs.getString("status"));
                r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                r.setUserEmail(rs.getString("email"));
                r.setUserDisplayName(rs.getString("prenom") + " " + rs.getString("nom"));
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void processRequest(int requestId, boolean approve) {
        String newStatus = approve ? "APPROVED" : "REJECTED";
        String userStatus = approve ? "actif" : "desactif"; // Keep deactivated if rejected, or maybe another state

        try {
            cnx.setAutoCommit(false);
            
            // 1. Update request status
            String req1 = "UPDATE reactivation_requests SET status = ? WHERE id = ?";
            try (PreparedStatement pst1 = cnx.prepareStatement(req1)) {
                pst1.setString(1, newStatus);
                pst1.setInt(2, requestId);
                pst1.executeUpdate();
            }

            // 2. If approved, update user status
            if (approve) {
                String req2 = "UPDATE user u JOIN reactivation_requests r ON u.id = r.user_id " +
                             "SET u.statut = ? WHERE r.id = ?";
                try (PreparedStatement pst2 = cnx.prepareStatement(req2)) {
                    pst2.setString(1, userStatus);
                    pst2.setInt(2, requestId);
                    pst2.executeUpdate();
                }
            }

            cnx.commit();
        } catch (SQLException e) {
            try { cnx.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
        } finally {
            try { cnx.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}
