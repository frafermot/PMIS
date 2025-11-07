package com.example.pmo;

import com.example.manager.Manager;
import com.example.portfolio.Portfolio;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "pmo")
public class PMO {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "pmo_id", nullable = false)
    @NotNull
    private Integer id;

    @Column(name = "pmo_name", nullable = false)
    @NotBlank
    private String name;

    @Column(name = "pmo_portfolio", nullable = false)
    @NotNull
    private Portfolio portfolio;

    @Column(name = "pmo_director", nullable = false)
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
