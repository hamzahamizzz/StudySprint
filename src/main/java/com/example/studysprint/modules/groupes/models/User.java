package com.example.studysprint.modules.groupes.models;

import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;

public class User {
    private Integer id;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String role;
    private String statut;
    private Timestamp dateInscription;
    private Timestamp updatedAt;
    private String resetToken;
    private Timestamp resetTokenExpiresAt;
    private Timestamp lastActivityAt;
    private String pays;
    private String telephone;
    private Integer anneesExperience;
    private String faceDescriptor;
    private String discr;
    private Integer age;
    private String sexe;
    private String etablissement;
    private String niveau;
    private String specialite;
    private String niveauEnseignement;
    private List<GroupMember> memberships;
    private List<GroupPost> posts;
    private List<PostComment> comments;
    private List<PostLike> likes;
    private List<PostRating> ratings;
    private List<GroupInvitation> invitationsReceived;

    public User() {
        this.memberships = new ArrayList<>();
        this.posts = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.likes = new ArrayList<>();
        this.ratings = new ArrayList<>();
        this.invitationsReceived = new ArrayList<>();
    }

    public User(Integer id, String nom, String prenom, String email, String motDePasse, String role, String statut,
                Timestamp dateInscription, Timestamp updatedAt, String resetToken, Timestamp resetTokenExpiresAt,
                Timestamp lastActivityAt, String pays, String telephone, Integer anneesExperience,
                String faceDescriptor, String discr, Integer age, String sexe, String etablissement,
                String niveau, String specialite, String niveauEnseignement) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.statut = statut;
        this.dateInscription = dateInscription;
        this.updatedAt = updatedAt;
        this.resetToken = resetToken;
        this.resetTokenExpiresAt = resetTokenExpiresAt;
        this.lastActivityAt = lastActivityAt;
        this.pays = pays;
        this.telephone = telephone;
        this.anneesExperience = anneesExperience;
        this.faceDescriptor = faceDescriptor;
        this.discr = discr;
        this.age = age;
        this.sexe = sexe;
        this.etablissement = etablissement;
        this.niveau = niveau;
        this.specialite = specialite;
        this.niveauEnseignement = niveauEnseignement;
        this.memberships = new ArrayList<>();
        this.posts = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.likes = new ArrayList<>();
        this.ratings = new ArrayList<>();
        this.invitationsReceived = new ArrayList<>();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Timestamp getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(Timestamp dateInscription) {
        this.dateInscription = dateInscription;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public Timestamp getResetTokenExpiresAt() {
        return resetTokenExpiresAt;
    }

    public void setResetTokenExpiresAt(Timestamp resetTokenExpiresAt) {
        this.resetTokenExpiresAt = resetTokenExpiresAt;
    }

    public Timestamp getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Timestamp lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public String getPays() {
        return pays;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Integer getAnneesExperience() {
        return anneesExperience;
    }

    public void setAnneesExperience(Integer anneesExperience) {
        this.anneesExperience = anneesExperience;
    }

    public String getFaceDescriptor() {
        return faceDescriptor;
    }

    public void setFaceDescriptor(String faceDescriptor) {
        this.faceDescriptor = faceDescriptor;
    }

    public String getDiscr() {
        return discr;
    }

    public void setDiscr(String discr) {
        this.discr = discr;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getSexe() {
        return sexe;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public String getEtablissement() {
        return etablissement;
    }

    public void setEtablissement(String etablissement) {
        this.etablissement = etablissement;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getNiveauEnseignement() {
        return niveauEnseignement;
    }

    public void setNiveauEnseignement(String niveauEnseignement) {
        this.niveauEnseignement = niveauEnseignement;
    }

    public List<GroupMember> getMemberships() {
        return memberships;
    }

    public void setMemberships(List<GroupMember> memberships) {
        this.memberships = memberships;
    }

    public List<GroupPost> getPosts() {
        return posts;
    }

    public void setPosts(List<GroupPost> posts) {
        this.posts = posts;
    }

    public List<PostComment> getComments() {
        return comments;
    }

    public void setComments(List<PostComment> comments) {
        this.comments = comments;
    }

    public List<PostLike> getLikes() {
        return likes;
    }

    public void setLikes(List<PostLike> likes) {
        this.likes = likes;
    }

    public List<PostRating> getRatings() {
        return ratings;
    }

    public void setRatings(List<PostRating> ratings) {
        this.ratings = ratings;
    }

    public List<GroupInvitation> getInvitationsReceived() {
        return invitationsReceived;
    }

    public void setInvitationsReceived(List<GroupInvitation> invitationsReceived) {
        this.invitationsReceived = invitationsReceived;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}
