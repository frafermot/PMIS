package com.example.program;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {

    @Query("SELECT p FROM Program p LEFT JOIN FETCH p.director LEFT JOIN FETCH p.portfolio")
    List<Program> findAllWithRelations();

    @Query("SELECT p FROM Program p LEFT JOIN FETCH p.director LEFT JOIN FETCH p.portfolio WHERE p.id = :id")
    Optional<Program> findByIdWithRelations(@Param("id") Long id);

    @Query("SELECT p FROM Program p LEFT JOIN FETCH p.portfolio LEFT JOIN FETCH p.director WHERE p.director.id = :directorId")
    List<Program> findAllByDirectorIdWithRelations(@Param("directorId") Long directorId);

    long countByPortfolioId(Long portfolioId);

    @Query("SELECT p FROM Program p LEFT JOIN FETCH p.portfolio LEFT JOIN FETCH p.director WHERE p.portfolio.id = :portfolioId")
    List<Program> findAllByPortfolioId(@Param("portfolioId") Long portfolioId);

    long countByDirectorId(Long directorId);

    @Modifying
    @Query("UPDATE Program p SET p.director = null WHERE p.director.id = :directorId")
    void unassignDirector(@Param("directorId") Long directorId);

}
