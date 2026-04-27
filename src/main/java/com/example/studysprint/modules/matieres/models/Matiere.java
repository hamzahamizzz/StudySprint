package com.example.studysprint.modules.matieres.models;

import java.sql.Timestamp;

public class Matiere {
    private Integer id;
    private String name;
    private String code;
    private String description;
    private Timestamp createdAt;
    private Integer createdById;

    public Matiere() {}

    public Matiere(Integer id, String name, String code, String description, Timestamp createdAt, Integer createdById) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.createdAt = createdAt;
        this.createdById = createdById;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Integer getCreatedById() { return createdById; }
    public void setCreatedById(Integer createdById) { this.createdById = createdById; }
}