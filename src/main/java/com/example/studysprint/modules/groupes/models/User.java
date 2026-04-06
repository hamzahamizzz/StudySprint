package com.example.studysprint.modules.groupes.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "prenom", nullable = false)
    private String prenom;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @Column(name = "role")
    private String role;

    @Column(name = "statut", nullable = false)
    private String statut;

    @Column(name = "date_inscription", nullable = false)
    private Timestamp dateInscription;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private Timestamp resetTokenExpiresAt;

    @Column(name = "last_activity_at")
    private Timestamp lastActivityAt;

    @Column(name = "pays")
    private String pays;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "annees_experience")
    private Integer anneesExperience;

    @Column(name = "face_descriptor", columnDefinition = "LONGTEXT")
    private String faceDescriptor;

    @Column(name = "discr", nullable = false)
    private String discr;

    @Column(name = "age")
    private Integer age;

    @Column(name = "sexe")
    private String sexe;

    @Column(name = "etablissement")
    private String etablissement;

    @Column(name = "niveau")
    private String niveau;

    @Column(name = "specialite")
    private String specialite;

    @Column(name = "niveau_enseignement")
    private String niveauEnseignement;

    public User() {
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
