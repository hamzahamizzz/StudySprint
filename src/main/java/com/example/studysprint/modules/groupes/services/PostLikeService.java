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
    private static List<PostLike> cache;
    private static boolean cacheDirty = true;

    // Initialize database connection for like operations
    public PostLikeService() {
        this.connection = MyDatabase.getConnection();
    }

    // Retrieve all post likes
    public List<PostLike> getAll() {
        if (cache == null || cacheDirty) {
            cache = fetchAllFromDatabase();
            cacheDirty = false;
        }
        return cache;
    }

    // Fetch all likes from the database (used to refresh the cache).
    private List<PostLike> fetchAllFromDatabase() {
        String sql = "SELECT * FROM post_like ORDER BY created_at DESC";
        List<PostLike> likes = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                likes.add(mapRowToLike(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all post likes", e);
        }

        return likes;
    }

    // Mark the in-memory cache as dirty.
    private static void markCacheDirty() {
        cacheDirty = true;
    }

    // Toggle like for one user and post.
    public void toggleLike(int postId, int userId) {
        getAll().stream()
                .filter(like -> like.getPostId() == postId && like.getUserId() == userId)
                .findFirst()
                .ifPresentOrElse(
                    like -> deleteLikeById(like.getId()),
                    () -> insertLike(postId, userId)
                );
    }

    // Count likes for a post.
    public int countByPost(int postId) {
        return (int) getAll().stream()
                .filter(like -> like.getPostId() == postId)
                .count();
    }

    // Get likes for a post.
    public List<PostLike> getByPost(int postId) {
        return getAll().stream()
                .filter(like -> like.getPostId() == postId)
                .toList();
    }

    // Get likes created by a user.
    public List<PostLike> getByUser(int userId) {
        return getAll().stream()
                .filter(like -> like.getUserId() == userId)
                .toList();
    }

    // Insert a like record for user and post
    // Insert a like record for the given user and post.
    private void insertLike(int postId, int userId) {
        String sql = "INSERT INTO post_like (created_at, post_id, user_id) VALUES (?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setInt(2, postId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add post like", e);
        }

        markCacheDirty();
    }

    // Delete a like record by identifier
    // Delete a like record by its identifier.
    private void deleteLikeById(int id) {
        String sql = "DELETE FROM post_like WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete post like", e);
        }

        markCacheDirty();
    }

    // Map a SQL result row to PostLike model
    private PostLike mapRowToLike(ResultSet rs) throws SQLException {
        return new PostLike(
                rs.getInt("id"),
                rs.getTimestamp("created_at"),
                rs.getInt("post_id"),
                rs.getInt("user_id")
        );
    }
}
