package com.example.studysprint.modules.groupes.services;

import com.example.studysprint.modules.groupes.models.PostRating;
import com.example.studysprint.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PostRatingService {
    private final Connection connection;

    public PostRatingService() {
        this.connection = MyDatabase.getConnection();
    }

    // Retrieve all post ratings
    public List<PostRating> getAll() {
        String sql = "SELECT * FROM post_rating ORDER BY created_at DESC";
        List<PostRating> ratings = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ratings.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all post ratings", e);
        }

        return ratings;
    }

    // Rate or update rating (validates 1-5 range)
    public void rate(int postId, int userId, int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        getAll().stream()
                .filter(r -> r.getPostId() == postId && r.getUserId() == userId)
                .findFirst()
                .ifPresentOrElse(
                    r -> updateRating(r.getId(), rating),
                    () -> addRating(postId, userId, rating)
                );
    }

    // Calculate average rating for a post
    public double averageRating(int postId) {
        return getAll().stream()
                .filter(r -> r.getPostId() == postId)
                .mapToInt(PostRating::getRating)
                .average()
                .orElse(0.0);
    }

    // Get all ratings for a post
    public List<PostRating> getByPost(int postId) {
        return getAll().stream()
                .filter(r -> r.getPostId() == postId)
                .toList();
    }

    // Get user's rating for a post (returns Optional)
    public java.util.Optional<PostRating> getUserRating(int postId, int userId) {
        return getAll().stream()
                .filter(r -> r.getPostId() == postId && r.getUserId() == userId)
                .findFirst();
    }

    private void addRating(int postId, int userId, int rating) {
        String sql = "INSERT INTO post_rating (rating, created_at, post_id, user_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setShort(1, (short) rating);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setInt(3, postId);
            ps.setInt(4, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add post rating", e);
        }
    }

    private void updateRating(int id, int rating) {
        String sql = "UPDATE post_rating SET rating = ?, created_at = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setShort(1, (short) rating);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update post rating", e);
        }
    }

    private PostRating mapRow(ResultSet rs) throws SQLException {
        return new PostRating(
                rs.getInt("id"),
                rs.getShort("rating"),
                rs.getTimestamp("created_at"),
                rs.getInt("post_id"),
                rs.getInt("user_id")
        );
    }
}
