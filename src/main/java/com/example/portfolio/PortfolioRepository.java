package com.example.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.director")
    List<Portfolio> findAllWithDirector();

    long countByDirectorId(Long directorId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Portfolio p SET p.director = null WHERE p.director.id = :directorId")
    void unassignDirector(@org.springframework.data.repository.query.Param("directorId") Long directorId);

}
