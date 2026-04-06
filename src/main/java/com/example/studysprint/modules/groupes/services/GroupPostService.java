package com.example.studysprint.modules.groupes.services;

import com.example.studysprint.modules.groupes.models.GroupPost;
import com.example.studysprint.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class GroupPostService {
    private final Connection connection;

    public GroupPostService() {
        this.connection = MyDatabase.getConnection();
    }

    // Retrieve all group posts
    public List<GroupPost> getAll() {
        String sql = "SELECT * FROM group_posts ORDER BY created_at DESC";
        List<GroupPost> posts = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                posts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all group posts", e);
        }

        return posts;
    }

    // Get all posts for a specific group
    public List<GroupPost> getByGroup(int groupId) {
        String sql = "SELECT * FROM group_posts WHERE group_id = ? ORDER BY created_at DESC";
        List<GroupPost> posts = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch group posts", e);
        }

        return posts;
    }

    // Search posts by keyword in title/body fields
    public List<GroupPost> search(String keyword) {
        String term = keyword == null ? "" : keyword.trim().toLowerCase();
        return getAll().stream()
                .filter(p -> (p.getTitle() != null && p.getTitle().toLowerCase().contains(term)) ||
                             (p.getBody() != null && p.getBody().toLowerCase().contains(term)))
                .toList();
    }

    public void add(GroupPost p) {
        String sql = "INSERT INTO group_posts (post_type, title, body, attachment_url, ai_summary, ai_category, ai_tags, created_at, group_id, author_id, parent_post_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Timestamp createdAt = p.getCreatedAt() != null ? p.getCreatedAt() : new Timestamp(System.currentTimeMillis());

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getPostType());
            ps.setString(2, p.getTitle());
            ps.setString(3, p.getBody());
            ps.setString(4, p.getAttachmentUrl());
            ps.setString(5, p.getAiSummary());
            ps.setString(6, p.getAiCategory());
            ps.setString(7, p.getAiTags());
            ps.setTimestamp(8, createdAt);
            ps.setInt(9, p.getGroupId());
            ps.setInt(10, p.getAuthorId());
            if (p.getParentPostId() != null) {
                ps.setInt(11, p.getParentPostId());
            } else {
                ps.setNull(11, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add group post", e);
        }
    }

    public void update(GroupPost p) {
        String sql = "UPDATE group_posts SET post_type = ?, title = ?, body = ?, attachment_url = ?, ai_summary = ?, ai_category = ?, ai_tags = ?, created_at = ?, group_id = ?, author_id = ?, parent_post_id = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getPostType());
            ps.setString(2, p.getTitle());
            ps.setString(3, p.getBody());
            ps.setString(4, p.getAttachmentUrl());
            ps.setString(5, p.getAiSummary());
            ps.setString(6, p.getAiCategory());
            ps.setString(7, p.getAiTags());
            ps.setTimestamp(8, p.getCreatedAt());
            ps.setInt(9, p.getGroupId());
            ps.setInt(10, p.getAuthorId());
            if (p.getParentPostId() != null) {
                ps.setInt(11, p.getParentPostId());
            } else {
                ps.setNull(11, java.sql.Types.INTEGER);
            }
            ps.setInt(12, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update group post", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM group_posts WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete group post", e);
        }
    }

    // Filter posts by type (Text, Image, Video, Poll, etc)
    public List<GroupPost> filterByPostType(String postType) {
        return getAll().stream()
                .filter(p -> p.getPostType() != null && p.getPostType().equalsIgnoreCase(postType))
                .toList();
    }

    // Get all posts authored by user
    public List<GroupPost> filterByAuthor(int authorId) {
        return getAll().stream()
                .filter(p -> p.getAuthorId() == authorId)
                .toList();
    }

    // Get posts with specific category tag
    public List<GroupPost> getPostsWithCategory(String category) {
        return getAll().stream()
                .filter(p -> p.getAiCategory() != null && p.getAiCategory().equalsIgnoreCase(category))
                .toList();
    }

    // Count total posts by author
    public long countPostsByAuthor(int authorId) {
        return getAll().stream()
                .filter(p -> p.getAuthorId() == authorId)
                .count();
    }

    // Get posts with attachments
    public List<GroupPost> getPostsWithAttachments() {
        return getAll().stream()
                .filter(p -> p.getAttachmentUrl() != null && !p.getAttachmentUrl().isBlank())
                .toList();
    }

    private GroupPost mapRow(ResultSet rs) throws SQLException {
        return new GroupPost(
                rs.getInt("id"),
                rs.getString("post_type"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getString("attachment_url"),
                rs.getString("ai_summary"),
                rs.getString("ai_category"),
                rs.getString("ai_tags"),
                rs.getTimestamp("created_at"),
                rs.getInt("group_id"),
                rs.getInt("author_id"),
                rs.getObject("parent_post_id") == null ? null : rs.getInt("parent_post_id")
        );
    }
}
