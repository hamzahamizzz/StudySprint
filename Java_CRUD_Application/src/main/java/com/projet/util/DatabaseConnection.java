package com.projet.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/projet_symfony?serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    
    private static Connection connection;

    private DatabaseConnection() {
        // Private constructor to prevent instantiation
    }

    public static Connection getConnection() {
        if (connection == null) {
            try {
                // Ensure the driver is loaded
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Connected to the database successfully!");
            } catch (ClassNotFoundException | SQLException e) {
                System.err.println("Database connection failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return connection;
    }
}
