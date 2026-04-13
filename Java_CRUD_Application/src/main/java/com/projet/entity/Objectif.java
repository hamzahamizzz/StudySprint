package com.projet.entity;

import java.sql.Date;

public class Objectif {
    private Integer id;
    private String titre;
    private String description;
    private Date dateDebut;
    private Date dateFin;
    private String statut;
    private Integer etudiantId; // Maps to etudiant_id foreign key

    public Objectif() {
    }

    public Objectif(Integer id, String titre, String description, Date dateDebut, Date dateFin, String statut, Integer etudiantId) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.statut = statut;
        this.etudiantId = etudiantId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    public Date getDateFin() {
        return dateFin;
    }

    public void setDateFin(Date dateFin) {
        this.dateFin = dateFin;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Integer getEtudiantId() {
        return etudiantId;
    }

    public void setEtudiantId(Integer etudiantId) {
        this.etudiantId = etudiantId;
    }

    @Override
    public String toString() {
        return "Objectif{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", statut='" + statut + '\'' +
                ", etudiantId=" + etudiantId +
                '}';
    }
}
