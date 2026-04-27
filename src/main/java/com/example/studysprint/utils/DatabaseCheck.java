package com.example.studysprint.utils;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseCheck {
    public static boolean checkConnection() {
        try {
            Connection conn = MyDatabase.getConnection();
            if (conn == null || conn.isClosed()) {
                System.err.println("❌ ERREUR: Connexion à la base de données échouée (connexion nulle ou fermée)");
                return false;
            }
            System.out.println("✅ Connexion à la base de données établie avec succès!");
            return true;
        } catch (SQLException e) {
            System.err.println("❌ ERREUR SQL: " + e.getMessage());
            System.err.println("   Cause: " + e.getCause());
            System.err.println("   Vérifiez que:");
            System.err.println("   - MySQL est en cours d'exécution");
            System.err.println("   - La base 'studysprint' existe (CREATE DATABASE studysprint;)");
            System.err.println("   - L'utilisateur 'root' existe avec mot de passe vide");
            System.err.println("   - La connexion est sur localhost:3306");
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("❌ ERREUR: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

