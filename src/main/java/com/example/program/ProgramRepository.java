package com.example.program;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {

    // Find all programs with details from the repository,
    // @EntityGraph(attributePaths = { "portfolio", "director" })
}
