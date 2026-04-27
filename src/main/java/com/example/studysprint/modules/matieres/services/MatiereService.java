package com.example.studysprint.modules.matieres.services;

import com.example.studysprint.modules.matieres.models.Matiere;
import com.example.studysprint.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MatiereService {
    private final Connection connection;
    private static List<Matiere> cache;
    private static boolean cacheInvalide = true;

    public MatiereService() {
        this.connection = MyDatabase.getConnection();
    }

    public List<Matiere> getAll() {
        if (cache == null || cacheInvalide) {
            cache = getAllFromDB();
            cacheInvalide = false;
        }
        return new ArrayList<>(cache);
    }

    private List<Matiere> getAllFromDB() {
        String sql = "SELECT * FROM subjects ORDER BY name";
        List<Matiere> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement matières", e);
        }
        return list;
    }

    public List<Matiere> search(String keyword) {
        String term = keyword == null ? "" : keyword.trim().toLowerCase();
        return getAll().stream()
                .filter(m -> (m.getName() != null && m.getName().toLowerCase().contains(term)) ||
                        (m.getCode() != null && m.getCode().toLowerCase().contains(term)) ||
                        (m.getDescription() != null && m.getDescription().toLowerCase().contains(term)))
                .toList();
    }

    public Matiere getById(int id) {
        return getAll().stream().filter(m -> m.getId() == id).findFirst().orElse(null);
    }

    public void add(Matiere m) {
        String sql = "INSERT INTO subjects (name, code, description, created_at, created_by_id) VALUES (?, ?, ?, ?, ?)";
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getCode());
            ps.setString(3, m.getDescription());
            ps.setTimestamp(4, m.getCreatedAt() != null ? m.getCreatedAt() : now);
            ps.setObject(5, m.getCreatedById() != null ? m.getCreatedById() : 1, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) m.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout matière", e);
        }
        setCacheInvalide();
    }

    public void update(Matiere m) {
        String sql = "UPDATE subjects SET name = ?, code = ?, description = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getCode());
            ps.setString(3, m.getDescription());
            ps.setInt(4, m.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur mise à jour matière", e);
        }
        setCacheInvalide();
    }

    public void delete(int id) {
        String sql = "DELETE FROM subjects WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression matière", e);
        }
        setCacheInvalide();
    }

    private Matiere mapRow(ResultSet rs) throws SQLException {
        return new Matiere(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("code"),
                rs.getString("description"),
                rs.getTimestamp("created_at"),
                rs.getObject("created_by_id") == null ? null : rs.getInt("created_by_id")
        );
    }

    private static void setCacheInvalide() {
        cacheInvalide = true;
    }
}