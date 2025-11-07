package com.example.manager;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "manager")
public class Manager {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "manager_id", nullable = false)
    @NotNull
    private Integer id;

    @Column(name = "manager_name", nullable = false)
    @NotBlank
    private String name;

    @Column(name = "manager_uvus", unique = true, nullable = false)
    @NotBlank
    private String uvus;

    @Column(name = "manager_is_admin", nullable = false)
    @NotNull
    private Boolean isAdmin;

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

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
}
