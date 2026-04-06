package com.example.studysprint.modules.groupes.services;

import com.example.studysprint.modules.groupes.models.PostLike;
import com.example.studysprint.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PostLikeService {
    private final Connection connection;

    public PostLikeService() {
        this.connection = MyDatabase.getConnection();
    }

    // Retrieve all post likes
    public List<PostLike> getAll() {
        String sql = "SELECT * FROM post_like ORDER BY created_at DESC";
        List<PostLike> likes = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                likes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all post likes", e);
        }

        return likes;
    }

    // Toggle like: remove if exists, add if not
    public void toggleLike(int postId, int userId) {
        getAll().stream()
                .filter(like -> like.getPostId() == postId && like.getUserId() == userId)
                .findFirst()
                .ifPresentOrElse(
                    like -> deleteLike(like.getId()),
                    () -> addLike(postId, userId)
                );
    }

    // Count total likes for a post
    public int countByPost(int postId) {
        return (int) getAll().stream()
                .filter(like -> like.getPostId() == postId)
                .count();
    }

    // Get all likes for a post
    public List<PostLike> getByPost(int postId) {
        return getAll().stream()
                .filter(like -> like.getPostId() == postId)
                .toList();
    }

    // Get all likes by a user
    public List<PostLike> getByUser(int userId) {
        return getAll().stream()
                .filter(like -> like.getUserId() == userId)
                .toList();
    }

    private void addLike(int postId, int userId) {
        String sql = "INSERT INTO post_like (created_at, post_id, user_id) VALUES (?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setInt(2, postId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add post like", e);
        }
    }

    private void deleteLike(int id) {
        String sql = "DELETE FROM post_like WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete post like", e);
        }
    }

    private PostLike mapRow(ResultSet rs) throws SQLException {
        return new PostLike(
                rs.getInt("id"),
                rs.getTimestamp("created_at"),
                rs.getInt("post_id"),
                rs.getInt("user_id")
        );
    }
}
