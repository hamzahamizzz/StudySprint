package com.example.studysprint.utils;

import com.example.studysprint.modules.quizz.models.Quiz;
import com.example.studysprint.modules.quizz.services.QuizService;

import java.sql.*;
import java.util.*;

/**
 * User-based collaborative filtering — pure Java, no external ML libraries.
 * Similarity metric: cosine similarity on normalised quiz scores [0,1].
 */
public class RecommendationEngine {

    private static final int DEFAULT_K = 5;

    private final QuizService quizService = new QuizService();

    // ── Step 1 : load score matrix ────────────────────────────────────────

    /**
     * Returns userId → (quizId → normalised score in [0,1]).
     * Uses a dedicated connection so it never touches the shared singleton mid-session.
     */
    public Map<Long, Map<Long, Double>> loadUserScoreMatrix() throws SQLException {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        String sql = "SELECT user_id, quiz_id, AVG(score)/100.0 AS normalized "
                   + "FROM quiz_attempts GROUP BY user_id, quiz_id";
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/studysprint"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                long   userId  = rs.getLong("user_id");
                long   quizId  = rs.getLong("quiz_id");
                double norm    = rs.getDouble("normalized");
                matrix.computeIfAbsent(userId, k -> new HashMap<>()).put(quizId, norm);
            }
        }
        return matrix;
    }

    // ── Step 2 : cosine similarity ────────────────────────────────────────

    public double cosineSimilarity(Map<Long, Double> vecA, Map<Long, Double> vecB) {
        if (vecA == null || vecB == null || vecA.isEmpty() || vecB.isEmpty()) return 0.0;

        double dot   = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (Map.Entry<Long, Double> e : vecA.entrySet()) {
            double a = e.getValue();
            normA += a * a;
            Double b = vecB.get(e.getKey());
            if (b != null) dot += a * b;
        }
        for (double b : vecB.values()) normB += b * b;

        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ── Step 3 : k nearest neighbors ─────────────────────────────────────

    public List<Long> findKNearestNeighbors(long userId,
                                             Map<Long, Map<Long, Double>> matrix,
                                             int k) {
        Map<Long, Double> userVec = matrix.get(userId);
        if (userVec == null) return Collections.emptyList();

        List<long[]> sims = new ArrayList<>(); // [otherId_bits, sim_bits] — avoid boxing
        List<double[]> scored = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, Double>> entry : matrix.entrySet()) {
            long otherId = entry.getKey();
            if (otherId == userId) continue;
            double sim = cosineSimilarity(userVec, entry.getValue());
            if (sim <= 0.0) continue;
            scored.add(new double[]{ otherId, sim });
        }

        scored.sort((a, b) -> Double.compare(b[1], a[1])); // descending

        List<Long> neighbors = new ArrayList<>();
        for (int i = 0; i < Math.min(k, scored.size()); i++) {
            neighbors.add((long) scored.get(i)[0]);
        }
        return neighbors;
    }

    // ── Step 4 : aggregate neighbor scores for unseen quizzes ─────────────

    /**
     * Returns quizIds the target user has NOT attempted, ranked by weighted neighbor score.
     * Only quizzes where neighbors averaged >= 0.70 are included.
     */
    public List<Long> getRecommendedQuizIds(long userId, int maxResults) throws SQLException {
        Map<Long, Map<Long, Double>> matrix = loadUserScoreMatrix();

        Map<Long, Double> userVec = matrix.getOrDefault(userId, Collections.emptyMap());
        Set<Long> alreadyAttempted = new HashSet<>(userVec.keySet());

        List<Long> neighbors = findKNearestNeighbors(userId, matrix, DEFAULT_K);
        if (neighbors.isEmpty()) return Collections.emptyList();

        // Accumulate scores: quizId → sum of neighbour scores
        Map<Long, Double> scoreSum   = new HashMap<>();
        Map<Long, Integer> voteCount = new HashMap<>();

        for (long neighborId : neighbors) {
            Map<Long, Double> neighborVec = matrix.get(neighborId);
            if (neighborVec == null) continue;
            for (Map.Entry<Long, Double> e : neighborVec.entrySet()) {
                long   quizId = e.getKey();
                double score  = e.getValue();
                if (alreadyAttempted.contains(quizId)) continue;
                scoreSum.merge(quizId, score, Double::sum);
                voteCount.merge(quizId, 1, Integer::sum);
            }
        }

        // Compute average and filter >= 0.70
        List<long[]> candidates = new ArrayList<>();
        for (Map.Entry<Long, Double> e : scoreSum.entrySet()) {
            long   quizId = e.getKey();
            int    votes  = voteCount.getOrDefault(quizId, 1);
            double avg    = e.getValue() / votes;
            if (avg >= 0.70) {
                // Store as bits to avoid boxing overhead
                candidates.add(new long[]{ quizId, Double.doubleToLongBits(avg) });
            }
        }

        // Sort by average score descending
        candidates.sort((a, b) -> Double.compare(
                Double.longBitsToDouble(b[1]),
                Double.longBitsToDouble(a[1])));

        List<Long> result = new ArrayList<>();
        for (int i = 0; i < Math.min(maxResults, candidates.size()); i++) {
            result.add(candidates.get(i)[0]);
        }
        return result;
    }

    // ── Step 5 : resolve to Quiz objects, filter published ─────────────────

    public List<Quiz> getRecommendedQuizzes(long userId, int maxResults) {
        try {
            List<Long> ids = getRecommendedQuizIds(userId, maxResults * 2); // over-fetch for published filter
            List<Quiz> result = new ArrayList<>();
            for (long id : ids) {
                if (result.size() >= maxResults) break;
                try {
                    Quiz q = quizService.getQuizById(id);
                    if (q != null && q.isPublished()) result.add(q);
                } catch (Exception ignored) {}
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
