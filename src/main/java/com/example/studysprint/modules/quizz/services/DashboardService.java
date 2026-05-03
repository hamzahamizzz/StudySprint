package com.example.studysprint.modules.quizz.services;

import com.example.studysprint.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DashboardService {

    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("dd/MM");

    // ── Records ───────────────────────────────────────────────────────────

    public record TopQuiz(String title, double avgScore) {}

    public record DueCard(String front, LocalDate nextReview) {}

    public record DashboardData(
            // KPIs
            int    totalAttempts,
            double avgScore,
            int    streak,
            int    dueCount,
            // Chart
            List<String> chartDays,      // "dd/MM" labels, last 7 days
            List<Double> chartScores,    // AVG score per day (NaN = no data)
            // Heatmap (raw map for rendering)
            Map<String, Integer> heatmap, // "yyyy-MM-dd" → attempt count
            // Top quizzes
            List<TopQuiz> topQuizzes,
            // Due flashcards
            List<DueCard> dueCards
    ) {}

    // ── Main loader — uses a single dedicated connection ──────────────────

    public DashboardData loadAll(long userId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/studysprint?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "")) {

            // q1 — score per day, last 7 days
            Map<String, Double> scoreByDay = new LinkedHashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT DATE(completed_at) day, AVG(score) avg " +
                    "FROM quiz_attempts WHERE user_id=? " +
                    "AND completed_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                    "GROUP BY day ORDER BY day")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        java.sql.Date d = rs.getDate(1);
                        if (d != null) {
                            String label = d.toLocalDate().format(LABEL_FMT);
                            scoreByDay.put(label, rs.getDouble(2));
                        }
                    }
                }
            }

            // Build chart series — fill all 7 days, NaN for missing
            List<String> chartDays   = new ArrayList<>();
            List<Double> chartScores = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate day   = LocalDate.now().minusDays(i);
                String    label = day.format(LABEL_FMT);
                chartDays.add(label);
                chartScores.add(scoreByDay.getOrDefault(label, Double.NaN));
            }

            // q2 — heatmap, last 28 days
            Map<String, Integer> heatmap = new LinkedHashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT DATE(completed_at), COUNT(*) " +
                    "FROM quiz_attempts WHERE user_id=? " +
                    "AND completed_at >= DATE_SUB(NOW(), INTERVAL 28 DAY) " +
                    "GROUP BY DATE(completed_at)")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        java.sql.Date d = rs.getDate(1);
                        if (d != null) heatmap.put(d.toLocalDate().toString(), rs.getInt(2));
                    }
                }
            }

            // q3 — total attempts + global average
            int    totalAttempts = 0;
            double avgScore      = 0.0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*), COALESCE(AVG(score),0) FROM quiz_attempts WHERE user_id=?")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalAttempts = rs.getInt(1);
                        avgScore      = rs.getDouble(2);
                    }
                }
            }

            // q4 — streak (computed in Java from heatmap)
            int       streak = 0;
            LocalDate day    = LocalDate.now();
            while (heatmap.containsKey(day.toString())) {
                streak++;
                day = day.minusDays(1);
            }

            // q5 — due flashcard count
            int dueCount = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM flashcards f " +
                    "JOIN flashcard_decks d ON f.deck_id = d.id " +
                    "WHERE d.owner_id=? AND f.next_review <= CURDATE()")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) dueCount = rs.getInt(1);
                }
            }

            // q6 — top 3 quizzes by rating
            List<TopQuiz> topQuizzes = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT q.title, ROUND(AVG(r.score),1) avg " +
                    "FROM quizzes q JOIN quiz_ratings r ON q.id = r.quiz_id " +
                    "GROUP BY q.id, q.title ORDER BY avg DESC LIMIT 3")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) topQuizzes.add(new TopQuiz(rs.getString(1), rs.getDouble(2)));
                }
            }

            // q7 — next 5 due flashcard fronts
            List<DueCard> dueCards = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT f.front, f.next_review FROM flashcards f " +
                    "JOIN flashcard_decks d ON f.deck_id = d.id " +
                    "WHERE d.owner_id=? AND f.next_review <= CURDATE() " +
                    "ORDER BY f.next_review ASC LIMIT 5")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        java.sql.Date d2 = rs.getDate(2);
                        dueCards.add(new DueCard(rs.getString(1),
                                d2 != null ? d2.toLocalDate() : LocalDate.now()));
                    }
                }
            }

            return new DashboardData(totalAttempts, avgScore, streak, dueCount,
                    chartDays, chartScores, heatmap, topQuizzes, dueCards);
        }
    }
}
