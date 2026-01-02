package com.example.user;

import com.example.project.Project;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "user_id", nullable = false)
    @NotNull
    private Long id;

    @Column(name = "user_name", nullable = false)
    @NotBlank
    private String name;

    @Column(name = "user_uvus", unique = true, nullable = false)
    @NotBlank
    private String uvus;

    // Change nullable to false, we will asociate user with project after creating
    // it
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUvus() {
        return uvus;
    }

    public void setUvus(String uvus) {
        this.uvus = uvus;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
