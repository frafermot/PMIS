package com.example.program;

import com.example.portfolio.Portfolio;
import com.example.manager.Manager;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "program")
public class Program {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "program_id", nullable = false)
    @NotNull
    private Integer id;

    @Column(name = "program_name", nullable = false)
    @NotBlank
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @NotNull
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_id", nullable = false)
    @NotNull
    private Manager director;

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

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public Manager getDirector() {
        return director;
    }

    public void setDirector(Manager director) {
        this.director = director;
    }
}
