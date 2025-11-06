package com.example.user;

import jakarta.persistence.*;

@Entity
@Table(name = "user")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "user_id")
    private Integer id;

    @Column(name = "user_name", nullable = false)
    private String name;

    @Column(name = "user_uvus", unique = true, nullable = false)
    private String uvus;

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

    public String getUvus() {
        return uvus;
    }

    public void setUvus(String uvus) {
        this.uvus = uvus;
    }
}
