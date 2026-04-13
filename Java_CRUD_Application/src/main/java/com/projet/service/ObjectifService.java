package com.projet.service;

import com.projet.entity.Objectif;
import com.projet.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ObjectifService {
    private Connection connection;

    public ObjectifService() {
        this.connection = DatabaseConnection.getConnection();
    }

    public void create(Objectif objectif) {
        String query = "INSERT INTO objectif (titre, description, date_debut, date_fin, statut, etudiant_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, objectif.getTitre());
            pst.setString(2, objectif.getDescription());
            pst.setDate(3, objectif.getDateDebut());
            pst.setDate(4, objectif.getDateFin());
            pst.setString(5, objectif.getStatut());
            pst.setInt(6, objectif.getEtudiantId());
            pst.executeUpdate();

            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    objectif.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Objectif findById(int id) {
        String query = "SELECT * FROM objectif WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return new Objectif(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getDate("date_debut"),
                        rs.getDate("date_fin"),
                        rs.getString("statut"),
                        rs.getInt("etudiant_id")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Objectif> findAll() {
        List<Objectif> objectifs = new ArrayList<>();
        String query = "SELECT * FROM objectif";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                objectifs.add(new Objectif(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getDate("date_debut"),
                        rs.getDate("date_fin"),
                        rs.getString("statut"),
                        rs.getInt("etudiant_id")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return objectifs;
    }

    public List<Objectif> findByEtudiantId(int etudiantId) {
        List<Objectif> objectifs = new ArrayList<>();
        String query = "SELECT * FROM objectif WHERE etudiant_id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, etudiantId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                objectifs.add(new Objectif(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getDate("date_debut"),
                        rs.getDate("date_fin"),
                        rs.getString("statut"),
                        rs.getInt("etudiant_id")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return objectifs;
    }

    public void update(Objectif objectif) {
        String query = "UPDATE objectif SET titre = ?, description = ?, date_debut = ?, date_fin = ?, statut = ?, etudiant_id = ? WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, objectif.getTitre());
            pst.setString(2, objectif.getDescription());
            pst.setDate(3, objectif.getDateDebut());
            pst.setDate(4, objectif.getDateFin());
            pst.setString(5, objectif.getStatut());
            pst.setInt(6, objectif.getEtudiantId());
            pst.setInt(7, objectif.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String query = "DELETE FROM objectif WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
