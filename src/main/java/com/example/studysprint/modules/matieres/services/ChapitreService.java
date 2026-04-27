package com.example.studysprint.modules.matieres.services;

import com.example.studysprint.modules.matieres.models.Chapitre;
import com.example.studysprint.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChapitreService {
    private final Connection connection;

    public ChapitreService() {
        this.connection = MyDatabase.getConnection();
    }

    public List<Chapitre> getBySubjectId(int subjectId) {
        String sql = "SELECT * FROM chapters WHERE subject_id = ? ORDER BY order_no ASC, id ASC";
        List<Chapitre> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement chapitres", e);
        }
        return list;
    }

    public Chapitre getById(int id) {
        String sql = "SELECT * FROM chapters WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur", e);
        }
        return null;
    }

    public void add(Chapitre chapitre) {
        String sql = "INSERT INTO chapters (title, order_no, summary, content, attachment_url, created_at, subject_id, created_by_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, chapitre.getTitle());
            ps.setInt(2, chapitre.getOrderNo() != null ? chapitre.getOrderNo() : 0);
            ps.setString(3, chapitre.getSummary());
            ps.setString(4, chapitre.getContent());
            ps.setString(5, chapitre.getAttachmentUrl());
            ps.setTimestamp(6, chapitre.getCreatedAt() != null ? chapitre.getCreatedAt() : now);
            ps.setInt(7, chapitre.getSubjectId());
            ps.setObject(8, chapitre.getCreatedById() != null ? chapitre.getCreatedById() : 1, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) chapitre.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout chapitre", e);
        }
    }

    public void update(Chapitre chapitre) {
        String sql = "UPDATE chapters SET title = ?, order_no = ?, summary = ?, content = ?, attachment_url = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, chapitre.getTitle());
            ps.setInt(2, chapitre.getOrderNo() != null ? chapitre.getOrderNo() : 0);
            ps.setString(3, chapitre.getSummary());
            ps.setString(4, chapitre.getContent());
            ps.setString(5, chapitre.getAttachmentUrl());
            ps.setInt(6, chapitre.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur mise à jour chapitre", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM chapters WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression chapitre", e);
        }
    }

    private Chapitre mapRow(ResultSet rs) throws SQLException {
        return new Chapitre(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getInt("order_no"),
                rs.getString("summary"),
                rs.getString("content"),
                rs.getString("attachment_url"),
                null, // aiSummary - non disponible en base
                null, // aiKeyPoint - non disponible en base
                null, // aiTags - non disponible en base
                rs.getTimestamp("created_at"),
                rs.getInt("subject_id"),
                rs.getObject("created_by_id") == null ? null : rs.getInt("created_by_id")
        );
    }
}