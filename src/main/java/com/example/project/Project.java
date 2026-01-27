package com.example.project;

import com.example.user.User;
import com.example.program.Program;

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
    @JoinColumn(name = "director_id", unique = true)
    private User director;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    @NotNull
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id")
    private User sponsor;

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

    public User getSponsor() {
        return sponsor;
    }

    public void setSponsor(User sponsor) {
        this.sponsor = sponsor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Project))
            return false;
        Project project = (Project) o;
        return getId() != null && getId().equals(project.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
