package com.example.program;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {

    @Query("SELECT p FROM Program p LEFT JOIN FETCH p.portfolio LEFT JOIN FETCH p.director")
    List<Program> findAllWithRelations();

    long countByPortfolioId(Long portfolioId);

    List<Program> findAllByPortfolioId(Long portfolioId);

    long countByDirectorId(Long directorId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Program p SET p.director = null WHERE p.director.id = :directorId")
    void unassignDirector(@org.springframework.data.repository.query.Param("directorId") Long directorId);

}
