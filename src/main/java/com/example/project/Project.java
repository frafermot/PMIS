package com.example.project;

import com.example.manager.Manager;
import com.example.program.Program;
import com.example.user.User;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "project")
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "project_id", nullable = false)
    @NotNull
    private Long id;

    @Column(name = "project_name", nullable = false)
    @NotBlank
    private String name;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_id", nullable = false, unique = true)
    @NotNull
    private User director;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    @NotNull
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id", nullable = false)
    @NotNull
    private Manager sponsor;

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

    public User getDirector() {
        return director;
    }

    public void setDirector(User director) {
        this.director = director;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public Manager getSponsor() {
        return sponsor;
    }

    public void setSponsor(Manager sponsor) {
        this.sponsor = sponsor;
    }
}
