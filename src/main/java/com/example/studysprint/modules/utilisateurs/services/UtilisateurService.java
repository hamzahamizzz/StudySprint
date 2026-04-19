package com.example.studysprint.modules.utilisateurs.services;

import com.example.studysprint.modules.utilisateurs.interfaces.IService;
import com.example.studysprint.modules.utilisateurs.models.Utilisateur;
import com.example.studysprint.utils.MyDataBase;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UtilisateurService implements IService<Utilisateur> {

    private final Connection cnx;

    public UtilisateurService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Utilisateur u) {
        String req = "INSERT INTO `user` (`nom`, `prenom`, `email`, `mot_de_passe`, `role`, `statut`, `date_inscription`, `discr`, `pays`, `age`, `sexe`, `etablissement`, `niveau`, `specialite`, `niveau_enseignement`, `annees_experience`, `telephone`, `face_descriptor`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, u.getNom());
            pst.setString(2, u.getPrenom());
            pst.setString(3, u.getEmail());
            pst.setString(4, u.getMotDePasse());
            pst.setString(5, u.getRole());
            pst.setString(6, u.getStatut());
            pst.setTimestamp(7, Timestamp.valueOf(u.getDateInscription()));
            pst.setString(8, u.getDiscr());
            pst.setString(9, u.getPays());
            pst.setObject(10, u.getAge());
            pst.setString(11, u.getSexe());
            pst.setString(12, u.getEtablissement());
            pst.setString(13, u.getNiveau());
            pst.setString(14, u.getSpecialite());
            pst.setString(15, u.getNiveauEnseignement());
            pst.setObject(16, u.getAnneesExperience());
            pst.setString(17, u.getTelephone());
            pst.setString(18, u.getFaceDescriptor());

            pst.executeUpdate();
            
            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    u.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding user: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Utilisateur> getAll() {
        List<Utilisateur> list = new ArrayList<>();
        String req = "SELECT * FROM `user`";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                Utilisateur u = new Utilisateur();
                u.setId(rs.getInt("id"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmail(rs.getString("email"));
                u.setMotDePasse(rs.getString("mot_de_passe"));
                u.setRole(rs.getString("role"));
                u.setStatut(rs.getString("statut"));
                u.setDateInscription(rs.getTimestamp("date_inscription").toLocalDateTime());
                u.setDiscr(rs.getString("discr"));
                u.setPays(rs.getString("pays"));
                u.setAge((Integer) rs.getObject("age"));
                u.setSexe(rs.getString("sexe"));
                u.setEtablissement(rs.getString("etablissement"));
                u.setNiveau(rs.getString("niveau"));
                u.setSpecialite(rs.getString("specialite"));
                u.setNiveauEnseignement(rs.getString("niveau_enseignement"));
                u.setAnneesExperience((Integer) rs.getObject("annees_experience"));
                u.setTelephone(rs.getString("telephone"));
                u.setFaceDescriptor(rs.getString("face_descriptor"));
                list.add(u);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void update(Utilisateur u) {
        String req = "UPDATE `user` SET `nom` = ?, `prenom` = ?, `email` = ?, `mot_de_passe` = ?, `role` = ?, `statut` = ?, `pays` = ?, `age` = ?, `sexe` = ?, `etablissement` = ?, `niveau` = ?, `specialite` = ?, `niveau_enseignement` = ?, `annees_experience` = ?, `telephone` = ?, `face_descriptor` = ? WHERE `id` = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, u.getNom());
            pst.setString(2, u.getPrenom());
            pst.setString(3, u.getEmail());
            pst.setString(4, u.getMotDePasse());
            pst.setString(5, u.getRole());
            pst.setString(6, u.getStatut());
            pst.setString(7, u.getPays());
            pst.setObject(8, u.getAge());
            pst.setString(9, u.getSexe());
            pst.setString(10, u.getEtablissement());
            pst.setString(11, u.getNiveau());
            pst.setString(12, u.getSpecialite());
            pst.setString(13, u.getNiveauEnseignement());
            pst.setObject(14, u.getAnneesExperience());
            pst.setString(15, u.getTelephone());
            pst.setString(16, u.getFaceDescriptor());
            pst.setInt(17, u.getId());

            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(int id) {
        String req = "DELETE FROM `user` WHERE `id` = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // --- Reset Password Logic ---

    public Utilisateur findByEmail(String email) {
        String req = "SELECT * FROM `user` WHERE LOWER(`email`) = LOWER(?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUtilisateur(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by email: " + e.getMessage());
        }
        return null;
    }

    public void updateResetToken(Utilisateur u) {
        String req = "UPDATE `user` SET `reset_token` = ?, `reset_token_expires_at` = ? WHERE `id` = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, u.getResetToken());
            pst.setTimestamp(2, u.getResetTokenExpiresAt() != null ? Timestamp.valueOf(u.getResetTokenExpiresAt()) : null);
            pst.setInt(3, u.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating reset token: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Utilisateur findByResetToken(String token) {
        String req = "SELECT * FROM `user` WHERE `reset_token` = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, token);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUtilisateur(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by token: " + e.getMessage());
        }
        return null;
    }

    public void resetPassword(Utilisateur u, String plainPassword) {
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        String req = "UPDATE `user` SET `mot_de_passe` = ?, `reset_token` = NULL, `reset_token_expires_at` = NULL WHERE `id` = ?";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, hashedPassword);
            pst.setInt(2, u.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error resetting password: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Utilisateur mapResultSetToUtilisateur(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasse(rs.getString("mot_de_passe"));
        u.setRole(rs.getString("role"));
        u.setStatut(rs.getString("statut"));
        if (rs.getTimestamp("date_inscription") != null)
            u.setDateInscription(rs.getTimestamp("date_inscription").toLocalDateTime());
        u.setDiscr(rs.getString("discr"));
        u.setPays(rs.getString("pays"));
        u.setAge((Integer) rs.getObject("age"));
        u.setSexe(rs.getString("sexe"));
        u.setEtablissement(rs.getString("etablissement"));
        u.setNiveau(rs.getString("niveau"));
        u.setSpecialite(rs.getString("specialite"));
        u.setNiveauEnseignement(rs.getString("niveau_enseignement"));
        u.setAnneesExperience((Integer) rs.getObject("annees_experience"));
        u.setTelephone(rs.getString("telephone"));
        u.setFaceDescriptor(rs.getString("face_descriptor"));
        u.setResetToken(rs.getString("reset_token"));
        if (rs.getTimestamp("reset_token_expires_at") != null)
            u.setResetTokenExpiresAt(rs.getTimestamp("reset_token_expires_at").toLocalDateTime());
        return u;
    }

    // Security Methods
    public void register(Utilisateur u, String plainPassword) {
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        u.setMotDePasse(hashedPassword);
        add(u);
    }

    public Utilisateur authenticate(String email, String password) {
        String req = "SELECT * FROM `user` WHERE LOWER(`email`) = LOWER(?)";
        try (PreparedStatement pst = cnx.prepareStatement(req)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Utilisateur u = mapResultSetToUtilisateur(rs);
                    if (BCrypt.checkpw(password, u.getMotDePasse())) {
                        return u;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        return null;
    }

    // Filters using Java Stream (getAll() + Stream)
    public List<Utilisateur> search(String query) {
        String lowerQuery = query == null ? "" : query.toLowerCase();
        return getAll().stream()
                .filter(u -> safe(u.getNom()).contains(lowerQuery)
                          || safe(u.getPrenom()).contains(lowerQuery)
                          || safe(u.getEmail()).contains(lowerQuery))
                .collect(Collectors.toList());
    }

    public List<Utilisateur> getByRole(String role) {
        return getAll().stream()
                .filter(u -> role != null && role.equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toList());
    }

    public List<Utilisateur> getActifs() {
        return getAll().stream()
                .filter(u -> u.getStatut() == null
                          || u.getStatut().isEmpty()
                          || "actif".equalsIgnoreCase(u.getStatut()))
                .collect(Collectors.toList());
    }

    public List<Utilisateur> getByPays(String pays) {
        return getAll().stream()
                .filter(u -> pays != null && pays.equalsIgnoreCase(u.getPays()))
                .collect(Collectors.toList());
    }

    private String safe(String val) {
        return val == null ? "" : val.toLowerCase();
    }
}
