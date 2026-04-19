package com.example.studysprint.modules.utilisateurs.models;

import java.time.LocalDateTime;

public class Utilisateur {
    private int id;

    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String role;
    private String statut = "actif";
    private LocalDateTime dateInscription;
    private String discr = "user";

    private String pays;
    private Integer age;
    private String sexe;
    private String etablissement;
    private String niveau;
    private String specialite;
    private String niveauEnseignement;
    private Integer anneesExperience;

    private String telephone;
    private String faceDescriptor;

    private String resetToken;
    private LocalDateTime resetTokenExpiresAt;

    // ═══════════════════════════════════════════════════════════════════════
    // RELATIONS JPA — Cardinalités (activées lors de l'intégration)
    // ═══════════════════════════════════════════════════════════════════════
    //
    // ── Côté PROFESSEUR ────────────────────────────────────────────────────
    // Un professeur gère N groupes                   [@OneToMany ← Groupe.professeur_id]
    // @OneToMany(mappedBy = "professeur", fetch = FetchType.LAZY)
    // private List<Groupe> groupesGeres;
    //
    // Un professeur enseigne N matières               [@OneToMany ← Matiere.professeur_id]
    // @OneToMany(mappedBy = "professeur", fetch = FetchType.LAZY)
    // private List<Matiere> matieres;
    //
    // Un professeur crée N quizz                      [@OneToMany ← Quizz.createur_id]
    // @OneToMany(mappedBy = "createur", fetch = FetchType.LAZY)
    // private List<Quizz> quizzCrees;
    //
    // ── Côté ÉTUDIANT ──────────────────────────────────────────────────────
    // Un étudiant appartient à N groupes              [@ManyToMany ↔ Groupe via groupe_etudiant]
    // @ManyToMany(mappedBy = "etudiants", fetch = FetchType.LAZY)
    // private List<Groupe> groupesInscrits;
    //
    // Un étudiant a N objectifs personnels            [@OneToMany ← Objectif.etudiant_id]
    // @OneToMany(mappedBy = "etudiant", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    // private List<Objectif> objectifs;
    //
    // ═══════════════════════════════════════════════════════════════════════
    // CONVENTION DE NOMMAGE DES FK (à respecter par les autres modules)
    //   groupe.professeur_id     → user.id  (ROLE_PROFESSOR)
    //   groupe_etudiant.etudiant_id → user.id  (ROLE_STUDENT)
    //   matiere.professeur_id    → user.id  (ROLE_PROFESSOR)
    //   quizz.createur_id        → user.id  (ROLE_PROFESSOR)
    //   objectif.etudiant_id     → user.id  (ROLE_STUDENT)
    // ═══════════════════════════════════════════════════════════════════════

    public Utilisateur() {
        this.dateInscription = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }

    public String getDiscr() { return discr; }
    public void setDiscr(String discr) { this.discr = discr; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }

    public String getEtablissement() { return etablissement; }
    public void setEtablissement(String etablissement) { this.etablissement = etablissement; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getNiveauEnseignement() { return niveauEnseignement; }
    public void setNiveauEnseignement(String niveauEnseignement) { this.niveauEnseignement = niveauEnseignement; }

    public Integer getAnneesExperience() { return anneesExperience; }
    public void setAnneesExperience(Integer anneesExperience) { this.anneesExperience = anneesExperience; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getFaceDescriptor() { return faceDescriptor; }
    public void setFaceDescriptor(String faceDescriptor) { this.faceDescriptor = faceDescriptor; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getResetTokenExpiresAt() { return resetTokenExpiresAt; }
    public void setResetTokenExpiresAt(LocalDateTime resetTokenExpiresAt) { this.resetTokenExpiresAt = resetTokenExpiresAt; }

    public String getFullName() {
        return prenom + " " + nom;
    }

    @Override
    public String toString() {
        return getFullName() + " (" + email + ")";
    }
}
