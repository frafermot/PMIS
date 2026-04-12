package com.example.pmo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PMORepository extends JpaRepository<PMO, Long> {

    @Query("SELECT p FROM PMO p LEFT JOIN FETCH p.portfolio LEFT JOIN FETCH p.director")
    List<PMO> findAllWithRelations();

    long countByDirectorId(Long directorId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE PMO p SET p.director = null WHERE p.director.id = :directorId")
    void unassignDirector(@org.springframework.data.repository.query.Param("directorId") Long directorId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM PMO p WHERE p.portfolio.id = :portfolioId")
    void deleteByPortfolioId(@org.springframework.data.repository.query.Param("portfolioId") Long portfolioId);

}
