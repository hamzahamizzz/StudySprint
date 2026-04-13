package com.projet.service;

import com.projet.entity.Tache;
import com.projet.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TacheService {
    private Connection connection;

    public TacheService() {
        this.connection = DatabaseConnection.getConnection();
    }

    public void create(Tache tache) {
        String query = "INSERT INTO tache (titre, duree, priorite, statut, objectif_id, date) VALUES (?, ?, ?, ?, ?, CURDATE())";
        try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, tache.getTitre());
            pst.setInt(2, tache.getDuree());
            pst.setString(3, tache.getPriorite());
            pst.setString(4, tache.getStatut());
            pst.setInt(5, tache.getObjectifId());
            pst.executeUpdate();

            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    tache.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Tache findById(int id) {
        String query = "SELECT * FROM tache WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return new Tache(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getInt("duree"),
                        rs.getString("priorite"),
                        rs.getString("statut"),
                        rs.getInt("objectif_id")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Tache> findAll() {
        List<Tache> taches = new ArrayList<>();
        String query = "SELECT * FROM tache";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                taches.add(new Tache(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getInt("duree"),
                        rs.getString("priorite"),
                        rs.getString("statut"),
                        rs.getInt("objectif_id")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return taches;
    }

    public List<Tache> findByObjectifId(int objectifId) {
        List<Tache> taches = new ArrayList<>();
        String query = "SELECT * FROM tache WHERE objectif_id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, objectifId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                taches.add(new Tache(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getInt("duree"),
                        rs.getString("priorite"),
                        rs.getString("statut"),
                        rs.getInt("objectif_id")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return taches;
    }

    public void update(Tache tache) {
        String query = "UPDATE tache SET titre = ?, duree = ?, priorite = ?, statut = ?, objectif_id = ? WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, tache.getTitre());
            pst.setInt(2, tache.getDuree());
            pst.setString(3, tache.getPriorite());
            pst.setString(4, tache.getStatut());
            pst.setInt(5, tache.getObjectifId());
            pst.setInt(6, tache.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String query = "DELETE FROM tache WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
