package com.example.studysprint.modules.groupes.services;

import com.example.studysprint.modules.groupes.models.StudyGroup;
import com.example.studysprint.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GroupService {
    private final Connection connection;
    private static List<StudyGroup> cache;
    private static boolean cacheDirty = true;

    // Initialize database connection for group operations
    public GroupService() {
        this.connection = MyDatabase.getConnection();
    }

    // Retrieve all study groups - base method for stream filtering
    public List<StudyGroup> getAll() {
        if (cache == null || cacheDirty) {
            cache = fetchAllFromDatabase();
            cacheDirty = false;
        }
        return cache;
    }

    // Fetch all study groups from the database (used to refresh the cache).
    private List<StudyGroup> fetchAllFromDatabase() {
        String sql = "SELECT * FROM study_groups";
        List<StudyGroup> groups = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                groups.add(mapRowToStudyGroup(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch study groups", e);
        }

        return groups;
    }

    // Mark the in-memory cache as dirty.
    private static void markCacheDirty() {
        cacheDirty = true;
    }

    // Search groups by keyword in name/subject fields
    public List<StudyGroup> search(String keyword) {
        String term = keyword == null ? "" : keyword.trim().toLowerCase();
        return getAll().stream()
                .filter(g -> (g.getName() != null && g.getName().toLowerCase().contains(term)) ||
                             (g.getSubject() != null && g.getSubject().toLowerCase().contains(term)))
                .toList();
    }

    // Sort groups by a supported column.
    public List<StudyGroup> sortBy(String column) {
        return switch (column != null ? column.toLowerCase() : "") {
            case "name" -> getAll().stream()
                .sorted(Comparator.comparing(StudyGroup::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList();
            case "created_at" -> getAll().stream()
                .sorted(Comparator.comparing(StudyGroup::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            case "privacy" -> getAll().stream()
                .sorted(Comparator.comparing(StudyGroup::getPrivacy, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList();
            default -> getAll();
        };
    }

    // Get group by ID
    public StudyGroup getById(int id) {
        return getAll().stream()
                .filter(g -> g.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // Filter groups by privacy setting
    public List<StudyGroup> filterByPrivacy(String privacy) {
        return getAll().stream()
                .filter(g -> g.getPrivacy() != null && g.getPrivacy().equalsIgnoreCase(privacy))
                .toList();
    }

    // Filter groups by subject/course name
    public List<StudyGroup> filterBySubject(String subject) {
        return getAll().stream()
                .filter(g -> g.getSubject() != null && g.getSubject().equalsIgnoreCase(subject))
                .toList();
    }

    // Get groups created by user
    public List<StudyGroup> getByCreator(int creatorId) {
        return getAll().stream()
                .filter(g -> g.getCreatedById() != null && g.getCreatedById() == creatorId)
                .toList();
    }

    // Count groups by privacy setting
    public long countByPrivacy(String privacy) {
        return getAll().stream()
                .filter(g -> g.getPrivacy() != null && g.getPrivacy().equalsIgnoreCase(privacy))
                .count();
    }

    // Insert a new study group into database
    public void add(StudyGroup g) {
        String sql = "INSERT INTO study_groups (name, description, privacy, subject, created_at, updated_at, last_activity, created_by_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp createdAt = g.getCreatedAt() != null ? g.getCreatedAt() : now;
        Timestamp updatedAt = g.getUpdatedAt() != null ? g.getUpdatedAt() : now;
        Timestamp lastActivity = g.getLastActivity() != null ? g.getLastActivity() : now;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, g.getName());
            ps.setString(2, g.getDescription());
            ps.setString(3, g.getPrivacy());
            ps.setString(4, g.getSubject());
            ps.setTimestamp(5, createdAt);
            ps.setTimestamp(6, updatedAt);
            ps.setTimestamp(7, lastActivity);
            if (g.getCreatedById() != null) {
                ps.setInt(8, g.getCreatedById());
            } else {
                ps.setNull(8, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add study group", e);
        }

        markCacheDirty();
    }

    // Update an existing study group in database
    public void update(StudyGroup g) {
        String sql = "UPDATE study_groups SET name = ?, description = ?, privacy = ?, subject = ?, updated_at = ?, last_activity = ?, created_by_id = ? WHERE id = ?";

        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp updatedAt = g.getUpdatedAt() != null ? g.getUpdatedAt() : now;
        Timestamp lastActivity = g.getLastActivity() != null ? g.getLastActivity() : now;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, g.getName());
            ps.setString(2, g.getDescription());
            ps.setString(3, g.getPrivacy());
            ps.setString(4, g.getSubject());
            ps.setTimestamp(5, updatedAt);
            ps.setTimestamp(6, lastActivity);
            if (g.getCreatedById() != null) {
                ps.setInt(7, g.getCreatedById());
            } else {
                ps.setNull(7, java.sql.Types.INTEGER);
            }
            ps.setInt(8, g.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update study group", e);
        }

        markCacheDirty();
    }

    // Delete a study group by identifier
    public void delete(int id) {
        String sql = "DELETE FROM study_groups WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete study group", e);
        }

        markCacheDirty();
    }

    // Map a SQL result row to StudyGroup model
    private StudyGroup mapRowToStudyGroup(ResultSet rs) throws SQLException {
        return new StudyGroup(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("privacy"),
                rs.getString("subject"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at"),
                rs.getTimestamp("last_activity"),
                rs.getObject("created_by_id") == null ? null : rs.getInt("created_by_id")
        );
    }
}
