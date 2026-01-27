package com.example.portfolio;

import com.example.user.User;

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
    @JoinColumn(name = "director_id")
    private User director;

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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Portfolio))
            return false;
        Portfolio portfolio = (Portfolio) o;
        return getId() != null && getId().equals(portfolio.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
