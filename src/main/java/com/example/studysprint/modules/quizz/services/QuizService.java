package com.example.studysprint.modules.quizz.services;

import com.example.studysprint.modules.quizz.models.Difficulty;
import com.example.studysprint.modules.quizz.models.Quiz;
import com.example.studysprint.modules.quizz.models.QuizAttempt;
import com.example.studysprint.modules.quizz.models.QuizRating;
import com.example.studysprint.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizService {

    private Connection conn() throws SQLException {
        return MyDatabase.getInstance().getConnection();
    }

    // ═══════════════════════════════════════════
    // QUIZ CRUD
    // ═══════════════════════════════════════════

    public void addQuiz(Quiz quiz) throws SQLException {
        String sql = "INSERT INTO quizzes (owner_id, subject_id, chapter_id, title, difficulty, questions, " +
                     "is_published, generated_by_ai, template_key, ai_meta) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, quiz.getOwnerId());
            ps.setLong(2, quiz.getSubjectId());
            if (quiz.getChapterId() != null) ps.setLong(3, quiz.getChapterId()); else ps.setNull(3, Types.BIGINT);
            ps.setString(4, quiz.getTitle());
            ps.setString(5, quiz.getDifficulty() != null ? quiz.getDifficulty().name() : null);
            ps.setString(6, quiz.getQuestions());
            ps.setBoolean(7, quiz.isPublished());
            ps.setBoolean(8, quiz.isGeneratedByAi());
            ps.setString(9, quiz.getTemplateKey());
            ps.setString(10, quiz.getAiMeta());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) quiz.setId(rs.getLong(1));
            }
        }
    }

    public void updateQuiz(Quiz quiz) throws SQLException {
        String sql = "UPDATE quizzes SET subject_id=?, chapter_id=?, title=?, difficulty=?, questions=?, " +
                     "is_published=?, generated_by_ai=?, template_key=?, ai_meta=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, quiz.getSubjectId());
            if (quiz.getChapterId() != null) ps.setLong(2, quiz.getChapterId()); else ps.setNull(2, Types.BIGINT);
            ps.setString(3, quiz.getTitle());
            ps.setString(4, quiz.getDifficulty() != null ? quiz.getDifficulty().name() : null);
            ps.setString(5, quiz.getQuestions());
            ps.setBoolean(6, quiz.isPublished());
            ps.setBoolean(7, quiz.isGeneratedByAi());
            ps.setString(8, quiz.getTemplateKey());
            ps.setString(9, quiz.getAiMeta());
            ps.setLong(10, quiz.getId());
            ps.executeUpdate();
        }
    }

    public void deleteQuiz(long id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM quizzes WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public Quiz getQuizById(long id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM quizzes WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapQuiz(rs);
            }
        }
        return null;
    }

    public List<Quiz> getAllQuizzes() throws SQLException {
        List<Quiz> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM quizzes ORDER BY id DESC")) {
            while (rs.next()) list.add(mapQuiz(rs));
        }
        return list;
    }

    public List<Quiz> getQuizzesByOwner(long ownerId) throws SQLException {
        List<Quiz> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM quizzes WHERE owner_id=? ORDER BY id DESC")) {
            ps.setLong(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapQuiz(rs));
            }
        }
        return list;
    }

    public List<Quiz> getPublishedQuizzes() throws SQLException {
        List<Quiz> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM quizzes WHERE is_published=1 ORDER BY id DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapQuiz(rs));
            }
        }
        return list;
    }

    /** Returns the first existing subject_id, or -1 if the table is empty. */
    public long getFirstSubjectId() throws SQLException {
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM subjects ORDER BY id LIMIT 1")) {
            if (rs.next()) return rs.getLong(1);
        }
        return -1;
    }

    /** Returns the first existing user id, or -1 if the table is empty. */
    public long getFirstUserId() throws SQLException {
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM users ORDER BY id LIMIT 1")) {
            if (rs.next()) return rs.getLong(1);
        }
        return -1;
    }

    /**
     * Returns average rating as Double, or null if no ratings exist yet.
     */
    public Double getAverageRatingOrNull(long quizId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT AVG(score) FROM quiz_ratings WHERE quiz_id=?")) {
            ps.setLong(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double v = rs.getDouble(1);
                    return rs.wasNull() ? null : v;
                }
            }
        }
        return null;
    }

    private Quiz mapQuiz(ResultSet rs) throws SQLException {
        Quiz q = new Quiz();
        q.setId(rs.getLong("id"));
        q.setOwnerId(rs.getLong("owner_id"));
        q.setSubjectId(rs.getLong("subject_id"));
        long chap = rs.getLong("chapter_id");
        q.setChapterId(rs.wasNull() ? null : chap);
        q.setTitle(rs.getString("title"));
        q.setDifficulty(Difficulty.fromString(rs.getString("difficulty")));
        q.setQuestions(rs.getString("questions"));
        q.setPublished(rs.getBoolean("is_published"));
        q.setGeneratedByAi(rs.getBoolean("generated_by_ai"));
        q.setTemplateKey(rs.getString("template_key"));
        q.setAiMeta(rs.getString("ai_meta"));
        return q;
    }

    // ═══════════════════════════════════════════
    // QUIZ ATTEMPT CRUD
    // ═══════════════════════════════════════════

    public void addAttempt(QuizAttempt attempt) throws SQLException {
        String sql = "INSERT INTO quiz_attempts (user_id, quiz_id, started_at, completed_at, score, " +
                     "total_questions, correct_count, duration_seconds) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, attempt.getUserId());
            ps.setLong(2, attempt.getQuizId());
            ps.setObject(3, attempt.getStartedAt());
            ps.setObject(4, attempt.getCompletedAt());
            if (attempt.getScore() != null) ps.setDouble(5, attempt.getScore()); else ps.setNull(5, Types.DECIMAL);
            ps.setInt(6, attempt.getTotalQuestions());
            ps.setInt(7, attempt.getCorrectCount());
            if (attempt.getDurationSeconds() != null) ps.setInt(8, attempt.getDurationSeconds()); else ps.setNull(8, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) attempt.setId(rs.getLong(1));
            }
        }
    }

    public void updateAttempt(QuizAttempt attempt) throws SQLException {
        String sql = "UPDATE quiz_attempts SET completed_at=?, score=?, correct_count=?, duration_seconds=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setObject(1, attempt.getCompletedAt());
            if (attempt.getScore() != null) ps.setDouble(2, attempt.getScore()); else ps.setNull(2, Types.DECIMAL);
            ps.setInt(3, attempt.getCorrectCount());
            if (attempt.getDurationSeconds() != null) ps.setInt(4, attempt.getDurationSeconds()); else ps.setNull(4, Types.INTEGER);
            ps.setLong(5, attempt.getId());
            ps.executeUpdate();
        }
    }

    public List<QuizAttempt> getAttemptsByUser(long userId) throws SQLException {
        List<QuizAttempt> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM quiz_attempts WHERE user_id=? ORDER BY started_at DESC")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAttempt(rs));
            }
        }
        return list;
    }

    public List<QuizAttempt> getAttemptsByQuiz(long quizId) throws SQLException {
        List<QuizAttempt> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM quiz_attempts WHERE quiz_id=? ORDER BY started_at DESC")) {
            ps.setLong(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAttempt(rs));
            }
        }
        return list;
    }

    private QuizAttempt mapAttempt(ResultSet rs) throws SQLException {
        QuizAttempt a = new QuizAttempt();
        a.setId(rs.getLong("id"));
        a.setUserId(rs.getLong("user_id"));
        a.setQuizId(rs.getLong("quiz_id"));
        a.setStartedAt(rs.getObject("started_at", LocalDateTime.class));
        a.setCompletedAt(rs.getObject("completed_at", LocalDateTime.class));
        double score = rs.getDouble("score");
        a.setScore(rs.wasNull() ? null : score);
        a.setTotalQuestions(rs.getInt("total_questions"));
        a.setCorrectCount(rs.getInt("correct_count"));
        int dur = rs.getInt("duration_seconds");
        a.setDurationSeconds(rs.wasNull() ? null : dur);
        return a;
    }

    // ═══════════════════════════════════════════
    // QUIZ RATING CRUD
    // ═══════════════════════════════════════════

    public void addOrUpdateRating(QuizRating rating) throws SQLException {
        String sql = "INSERT INTO quiz_ratings (user_id, quiz_id, score, created_at) VALUES (?,?,?,?) " +
                     "ON DUPLICATE KEY UPDATE score=VALUES(score)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rating.getUserId());
            ps.setLong(2, rating.getQuizId());
            ps.setInt(3, rating.getScore());
            ps.setObject(4, rating.getCreatedAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) rating.setId(rs.getLong(1));
            }
        }
    }

    public double getAverageRating(long quizId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT AVG(score) FROM quiz_ratings WHERE quiz_id=?")) {
            ps.setLong(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0;
    }

    public List<QuizRating> getRatingsByQuiz(long quizId) throws SQLException {
        List<QuizRating> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM quiz_ratings WHERE quiz_id=? ORDER BY created_at DESC")) {
            ps.setLong(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QuizRating r = new QuizRating();
                    r.setId(rs.getLong("id"));
                    r.setUserId(rs.getLong("user_id"));
                    r.setQuizId(rs.getLong("quiz_id"));
                    r.setScore(rs.getInt("score"));
                    r.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                    list.add(r);
                }
            }
        }
        return list;
    }

    /**
     * Returns a map of quiz_id → average rating for all quizzes that have ratings.
     * Use this instead of calling getAverageRatingOrNull() per quiz (avoids N+1 queries).
     */
    public Map<Long, Double> getAllAverageRatings() throws SQLException {
        Map<Long, Double> map = new HashMap<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT quiz_id, AVG(score) as avg_score FROM quiz_ratings GROUP BY quiz_id")) {
            while (rs.next()) {
                map.put(rs.getLong("quiz_id"), rs.getDouble("avg_score"));
            }
        }
        return map;
    }

    /**
     * Returns a map of quiz_id → [attemptCount, avgScoreRounded].
     * Single GROUP BY query — no N+1.
     */
    public Map<Long, int[]> getQuizStats() throws SQLException {
        Map<Long, int[]> map = new HashMap<>();
        String sql = "SELECT quiz_id, COUNT(*) AS nb, AVG(score) AS avg_score "
                   + "FROM quiz_attempts WHERE completed_at IS NOT NULL GROUP BY quiz_id";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int nb       = rs.getInt("nb");
                double avg   = rs.getDouble("avg_score");
                if (rs.wasNull()) avg = 0;
                map.put(rs.getLong("quiz_id"), new int[]{ nb, (int) Math.round(avg) });
            }
        }
        return map;
    }

    /**
     * Toggles the is_published flag of a quiz in DB.
     */
    public void togglePublished(long quizId, boolean published) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE quizzes SET is_published=? WHERE id=?")) {
            ps.setBoolean(1, published);
            ps.setLong(2, quizId);
            ps.executeUpdate();
        }
    }
}
