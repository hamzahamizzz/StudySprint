package com.example.studysprint.utils;

import com.example.studysprint.modules.quizz.models.Difficulty;
import com.example.studysprint.modules.quizz.models.Quiz;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * OLS multivariate linear regression — pure Java, no external libraries.
 *
 * Features per sample:
 *   x1 = difficulty (EASY=1, MEDIUM=2, HARD=3)
 *   x2 = user avg score on same subject / 100  (0.5 if no prior history)
 *   x3 = log(1 + prior attempt count on this quiz)
 *   y  = score / 100
 *
 * beta = (Xt * X)^-1 * Xt * Y   (closed-form OLS, solved via Gauss-Jordan)
 */
public class ScorePredictionService {

    // ── Training data ─────────────────────────────────────────────────────

    /**
     * Returns rows of [y, x1, x2, x3] for all attempts by this user.
     */
    private double[][] loadTrainingData(long userId) throws SQLException {
        String sql =
            "SELECT qa.score/100.0," +
            "  CASE q.difficulty WHEN 'EASY' THEN 1.0 WHEN 'MEDIUM' THEN 2.0 ELSE 3.0 END," +
            "  COALESCE((SELECT AVG(qa2.score)/100.0 FROM quiz_attempts qa2" +
            "            JOIN quizzes q2 ON qa2.quiz_id=q2.id" +
            "            WHERE qa2.user_id=qa.user_id AND q2.subject_id=q.subject_id" +
            "            AND qa2.id<qa.id), 0.5)," +
            "  LOG(1 + (SELECT COUNT(*) FROM quiz_attempts qa3" +
            "           WHERE qa3.user_id=qa.user_id AND qa3.quiz_id=qa.quiz_id AND qa3.id<qa.id))" +
            " FROM quiz_attempts qa" +
            " JOIN quizzes q ON qa.quiz_id=q.id" +
            " WHERE qa.user_id=?";

        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/studysprint"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            List<double[]> rows = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new double[]{
                        rs.getDouble(1),  // y
                        rs.getDouble(2),  // x1
                        rs.getDouble(3),  // x2
                        rs.getDouble(4)   // x3
                    });
                }
            }
            return rows.toArray(new double[0][]);
        }
    }

    // ── Matrix operations ─────────────────────────────────────────────────

    double[][] multiply(double[][] a, double[][] b) {
        int aRows = a.length, aCols = a[0].length, bCols = b[0].length;
        double[][] result = new double[aRows][bCols];
        for (int i = 0; i < aRows; i++)
            for (int k = 0; k < aCols; k++)
                for (int j = 0; j < bCols; j++)
                    result[i][j] += a[i][k] * b[k][j];
        return result;
    }

    double[][] transpose(double[][] m) {
        int rows = m.length, cols = m[0].length;
        double[][] t = new double[cols][rows];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                t[j][i] = m[i][j];
        return t;
    }

    /**
     * Gauss-Jordan elimination on the augmented matrix [m | I4].
     * Throws ArithmeticException if any pivot is < 1e-12 (singular matrix).
     */
    double[][] invert4x4(double[][] m) {
        int n = 4;
        double[][] aug = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) aug[i][j] = m[i][j];
            aug[i][n + i] = 1.0;
        }

        for (int col = 0; col < n; col++) {
            // Find max pivot row
            int pivot = col;
            for (int row = col + 1; row < n; row++)
                if (Math.abs(aug[row][col]) > Math.abs(aug[pivot][col])) pivot = row;

            double[] tmp = aug[col]; aug[col] = aug[pivot]; aug[pivot] = tmp;

            double pivotVal = aug[col][col];
            if (Math.abs(pivotVal) < 1e-12)
                throw new ArithmeticException("Singular matrix");

            for (int j = 0; j < 2 * n; j++) aug[col][j] /= pivotVal;

            for (int row = 0; row < n; row++) {
                if (row == col) continue;
                double factor = aug[row][col];
                for (int j = 0; j < 2 * n; j++)
                    aug[row][j] -= factor * aug[col][j];
            }
        }

        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                inv[i][j] = aug[i][n + j];
        return inv;
    }

    // ── Model training ────────────────────────────────────────────────────

    /**
     * Returns OLS coefficients [w0, w1, w2, w3] where w0 is the bias term.
     * beta = (Xt * X)^-1 * Xt * Y
     */
    private double[] trainModel(double[][] data) {
        int n = data.length;

        // Build design matrix X (n×4) with bias column and label vector Y (n×1)
        double[][] X = new double[n][4];
        double[][] Y = new double[n][1];
        for (int i = 0; i < n; i++) {
            X[i][0] = 1.0;       // bias
            X[i][1] = data[i][1]; // x1
            X[i][2] = data[i][2]; // x2
            X[i][3] = data[i][3]; // x3
            Y[i][0] = data[i][0]; // y
        }

        double[][] Xt      = transpose(X);
        double[][] XtX     = multiply(Xt, X);
        double[][] XtXInv  = invert4x4(XtX);
        double[][] beta    = multiply(multiply(XtXInv, Xt), Y);

        return new double[]{ beta[0][0], beta[1][0], beta[2][0], beta[3][0] };
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Predicts the score [0,100] the user would get on the given quiz.
     * Returns empty if fewer than 5 training samples exist or the matrix is singular.
     */
    public Optional<Integer> predictScore(long userId, Quiz quiz) throws SQLException {
        double[][] data = loadTrainingData(userId);
        if (data.length < 5) return Optional.empty();

        double[] w;
        try {
            w = trainModel(data);
        } catch (ArithmeticException e) {
            return Optional.empty();
        }

        // Build prediction features
        double x1 = encodeDifficulty(quiz.getDifficulty());
        double x2 = fetchAvgScoreSameSubject(userId, quiz.getSubjectId()) / 100.0;
        double x3 = Math.log(1.0 + fetchAttemptCount(userId, quiz.getId()));

        double pred = w[0] + w[1] * x1 + w[2] * x2 + w[3] * x3;
        int clamped = (int) Math.max(0, Math.min(100, Math.round(pred * 100)));
        return Optional.of(clamped);
    }

    // ── Feature helpers ───────────────────────────────────────────────────

    private double encodeDifficulty(Difficulty d) {
        if (d == null) return 2.0;
        return switch (d) {
            case EASY   -> 1.0;
            case MEDIUM -> 2.0;
            case HARD   -> 3.0;
        };
    }

    private double fetchAvgScoreSameSubject(long userId, long subjectId) throws SQLException {
        String sql = "SELECT AVG(qa.score) FROM quiz_attempts qa" +
                     " JOIN quizzes q ON qa.quiz_id=q.id" +
                     " WHERE qa.user_id=? AND q.subject_id=?";
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/studysprint"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double v = rs.getDouble(1);
                    return rs.wasNull() ? 50.0 : v;
                }
            }
        }
        return 50.0;
    }

    private int fetchAttemptCount(long userId, long quizId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM quiz_attempts WHERE user_id=? AND quiz_id=?";
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/studysprint"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "");
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }
}
