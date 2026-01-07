package com.example.portfolio;

import com.example.manager.Manager;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "portfolio")
public class Portfolio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "portfolio_id", nullable = false)
    @NotNull
    private Long id;

    @Column(name = "portfolio_name", nullable = false)
    @NotBlank
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_id", nullable = false)
    @NotNull
    private Manager director;

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

    public Manager getDirector() {
        return director;
    }

    public void setDirector(Manager director) {
        this.director = director;
    }
}
