package com.example.portfolio;

import com.example.manager.Manager;

import jakarta.persistence.*;

@Entity
@Table(name = "portfolio")
public class Portfolio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "portfolio_id")
    private Integer id;

    @Column(name = "portfolio_name", nullable = false)
    private String name;

    @Column(name = "portfolio_director", nullable = false)
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

    public Manager getDirector() {
        return director;
    }

    public void setDirector(Manager director) {
        this.director = director;
    }
}
