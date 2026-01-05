package com.example.program;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {

    @Query("SELECT p FROM Program p LEFT JOIN FETCH p.portfolio LEFT JOIN FETCH p.director")
    List<Program> findAllWithRelations();

}
