package com.example.studysprint.modules.groupes.services;

import com.example.studysprint.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {

    public record UserDisplay(String fullName, String email, String initials) {
    }

    private final Connection connection;
    private final Map<Integer, UserDisplay> cacheById = new ConcurrentHashMap<>();

    public UserService() {
        this.connection = MyDatabase.getConnection();
    }

    // Load the user info needed by the UI.
    public UserDisplay getDisplay(Integer userId) {
        if (userId == null) {
            return unknown(null);
        }
        return cacheById.computeIfAbsent(userId, this::loadDisplayFromDatabase);
    }

    // Read the user's name and email from the database.
    private UserDisplay loadDisplayFromDatabase(int userId) {
        String sql = "SELECT prenom, nom, email FROM users WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return unknown(userId);
                }

                String prenom = rs.getString("prenom");
                String nom = rs.getString("nom");
                String email = rs.getString("email");

                String fullName = buildFullName(prenom, nom);
                String initials = initial(fullName.isBlank() ? "U" : fullName);
                return new UserDisplay(fullName.isBlank() ? ("Utilisateur #" + userId) : fullName,
                        email == null ? "" : email,
                        initials);
            }
        } catch (SQLException e) {
            return unknown(userId);
        }
    }

    // Combine first name and last name into one display string.
    private static String buildFullName(String firstName, String lastName) {
        String f = firstName == null ? "" : firstName.trim();
        String l = lastName == null ? "" : lastName.trim();
        return (f + " " + l).trim();
    }

    // Extract the first letter used in avatar labels.
    private static String initial(String value) {
        if (value == null || value.isBlank()) {
            return "U";
        }
        return value.substring(0, 1).toUpperCase();
    }

    // Return a fallback identity when the lookup fails.
    private static UserDisplay unknown(Integer userId) {
        String label = userId == null ? "Utilisateur" : ("Utilisateur #" + userId);
        return new UserDisplay(label, "", initial(label));
    }
}
