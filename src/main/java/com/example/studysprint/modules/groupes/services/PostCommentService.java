package com.example.studysprint.modules.groupes.services;

import com.example.studysprint.modules.groupes.models.PostComment;
import com.example.studysprint.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PostCommentService {
    private final Connection connection;
    private static List<PostComment> cache;
    private static boolean cacheInvalide = true;

    public PostCommentService() {
        this.connection = MyDataBase.getInstance().getCnx();
    }
    public List<PostComment> getAll() {
        if (cache == null || cacheInvalide) {
            cache = getAllFromDB();
            cacheInvalide = false;
        }
        return cache;
    }

    private List<PostComment> getAllFromDB() {
        String sql = "SELECT * FROM post_comment ORDER BY created_at ASC";
        List<PostComment> comments = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                comments.add(mapRowToComment(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all post comments", e);
        }

        return comments;
    }

    private static void setCacheInvalide() {
        cacheInvalide = true;
    }

    public List<PostComment> getByPost(int postId) {
        return getAll().stream()
                .filter(c -> c.getPostId() == postId)
                .toList();
    }

    public void add(PostComment c) {
        String sql = "INSERT INTO post_comment (depth, body, is_bot, bot_name, created_at, post_id, author_id, parent_comment_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Timestamp createdAt = c.getCreatedAt() != null ? c.getCreatedAt() : new Timestamp(System.currentTimeMillis());

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, c.getDepth());
            ps.setString(2, c.getBody());
            ps.setBoolean(3, c.getIsBot() != null && c.getIsBot());
            ps.setString(4, c.getBotName());
            ps.setTimestamp(5, createdAt);
            ps.setInt(6, c.getPostId());
            ps.setInt(7, c.getAuthorId());
            if (c.getParentCommentId() != null) {
                ps.setInt(8, c.getParentCommentId());
            } else {
                ps.setNull(8, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add post comment", e);
        }

        setCacheInvalide();
    }

    public void delete(int id) {
        String sql = "DELETE FROM post_comment WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete post comment", e);
        }

        setCacheInvalide();
    }

    public List<PostComment> filterByBot() {
        return getAll().stream()
                .filter(c -> c.getIsBot() != null && c.getIsBot())
                .toList();
    }

    public List<PostComment> filterByDepth(int depth) {
        return getAll().stream()
                .filter(c -> c.getDepth() == depth)
                .toList();
    }

    public long countCommentsByAuthor(int authorId) {
        return getAll().stream()
                .filter(c -> c.getAuthorId() == authorId)
                .count();
    }

    public List<PostComment> getByParentCommentId(int parentCommentId) {
        return getAll().stream()
                .filter(c -> parentCommentId == (c.getParentCommentId() != null ? c.getParentCommentId() : -1))
                .toList();
    }

    private PostComment mapRowToComment(ResultSet rs) throws SQLException {
        return new PostComment(
                rs.getInt("id"),
                rs.getInt("depth"),
                rs.getString("body"),
                rs.getBoolean("is_bot"),
                rs.getString("bot_name"),
                rs.getTimestamp("created_at"),
                rs.getInt("post_id"),
                rs.getInt("author_id"),
                rs.getObject("parent_comment_id") == null ? null : rs.getInt("parent_comment_id")
        );
    }
}
