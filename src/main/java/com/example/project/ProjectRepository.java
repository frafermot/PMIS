package com.example.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Find all projects with details from the repository,
    // @EntityGraph(attributePaths = { "director", "program", "sponsor" })
}
