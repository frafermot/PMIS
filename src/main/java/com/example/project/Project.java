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
    private Integer id;

    @Column(name = "project_name", nullable = false)
    @NotBlank
    private String name;

    @Column(name = "project_director", unique = true, nullable = false)
    @NotNull
    private User director;

    @Column(name = "project_program", nullable = false)
    @NotNull
    private Program program;

    @Column(name = "project_sponsor", nullable = false)
    @NotNull
    private Manager sponsor;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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
