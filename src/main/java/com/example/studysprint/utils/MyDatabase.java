package com.example.studysprint.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton JDBC connection to the StudySprint MariaDB database.
 * Connection is lazy — only established when first requested.
 */
public class MyDatabase {

    private static final String URL      = "jdbc:mysql://localhost:3306/studysprint?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private static MyDatabase instance;
    private Connection connection;

    private MyDatabase() {
        // Lazy — connection established on first getConnection() call
    }

    public static synchronized MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    /**
     * Returns an open connection, (re)connecting if needed.
     * Throws SQLException so callers can handle DB-down gracefully.
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(1)) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[MyDatabase] Connected to studysprint.");
        }
        return connection;
    }

    /** @return true if the DB is reachable right now */
    public static boolean isAvailable() {
        try {
            getInstance().getConnection();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
