package com.example.studysprint.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static MyDataBase instance;
    private final String URL = "jdbc:mysql://localhost:3306/studysprint";
    private final String USERNAME = "root";
    private final String PASSWORD = "";
    private Connection cnx;

    private MyDataBase() {
        try {
            this.cnx = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connected to Database StudySprint (JDBC) .....");
        } catch (SQLException e) {
            System.err.println("JDBC Connection Error: " + e.getMessage());
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}
