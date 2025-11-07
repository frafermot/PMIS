package com.example.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "user")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "user_id", nullable = false)
    @NotNull
    private Integer id;

    @Column(name = "user_name", nullable = false)
    @NotBlank
    private String name;

    @Column(name = "user_uvus", unique = true, nullable = false)
    @NotBlank
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
