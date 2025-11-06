package com.example.project;

import com.example.manager.Manager;
import com.example.program.Program;
import com.example.user.User;

import jakarta.persistence.*;

@Entity
@Table(name = "project")
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "project_id")
    private Integer id;

    @Column(name = "project_name", nullable = false)
    private String name;

    @Column(name = "project_director", unique = true, nullable = false)
    private User director;

    @Column(name = "project_program", nullable = false)
    private Program program;

    @Column(name = "project_sponsor", nullable = false)
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
