package com.example.studysprint.utils;

import com.example.studysprint.modules.quizz.models.Difficulty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.Optional;

public class AdaptiveEngine {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final OllamaService ollama = new OllamaService();

    public record AdaptiveRecommendation(
            String concept,
            Difficulty recommendedDifficulty,
            String explanation,
            String suggestedQuizTitle,
            long matchingQuizId
    ) {}

    // ── Public API ────────────────────────────────────────────────────────

    public boolean needsAdaptation(long userId, long quizId) throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt, AVG(score) AS avg FROM quiz_attempts "
                   + "WHERE user_id=? AND quiz_id=?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int    cnt = rs.getInt("cnt");
                    double avg = rs.getDouble("avg");
                    return cnt >= 3 && avg < 50.0;
                }
            }
        }
        return false;
    }

    public AdaptiveRecommendation getRecommendation(long userId, long quizId) throws Exception {
        // 1. Fetch quiz metadata
        String title, difficultyStr, questions;
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT title, difficulty, questions FROM quizzes WHERE id=?")) {
            ps.setLong(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new Exception("Quiz introuvable id=" + quizId);
                title         = rs.getString("title");
                difficultyStr = rs.getString("difficulty");
                questions     = rs.getString("questions");
            }
        }

        // 2. Call Ollama
        String raw = ollama.generateAdaptiveRecommendation(title, difficultyStr, questions);

        // 3. Clean backticks and parse JSON
        String json = raw.replaceAll("```json", "").replaceAll("```", "").trim();
        int start = json.indexOf('{');
        int end   = json.lastIndexOf('}');
        if (start >= 0 && end > start) json = json.substring(start, end + 1);
        JsonNode node = MAPPER.readTree(json);

        String concept          = node.path("concept").asText("").trim();
        String recDiff          = node.path("recommended_difficulty").asText("EASY").trim().toUpperCase();
        String explanation      = node.path("explanation").asText("").trim();
        String suggestedTitle   = node.path("suggested_quiz_title").asText("").trim();

        Difficulty recDifficulty;
        try { recDifficulty = Difficulty.valueOf(recDiff); }
        catch (IllegalArgumentException e) { recDifficulty = Difficulty.EASY; }

        // 4. Find matching published quiz
        long matchingQuizId = 0L;
        if (!suggestedTitle.isBlank()) {
            String searchSql = "SELECT id FROM quizzes WHERE is_published=true "
                             + "AND title LIKE CONCAT('%',?,'%') AND difficulty=? LIMIT 1";
            try (Connection conn = MyDatabase.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(searchSql)) {
                ps.setString(1, suggestedTitle);
                ps.setString(2, recDifficulty.name());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) matchingQuizId = rs.getLong(1);
                }
            }
        }

        // 5. Return
        return new AdaptiveRecommendation(concept, recDifficulty, explanation,
                                          suggestedTitle, matchingQuizId);
    }

    public Optional<AdaptiveRecommendation> analyze(long userId, long quizId) {
        if (!ollama.isAvailable()) return Optional.empty();
        try {
            if (!needsAdaptation(userId, quizId)) return Optional.empty();
            return Optional.of(getRecommendation(userId, quizId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
