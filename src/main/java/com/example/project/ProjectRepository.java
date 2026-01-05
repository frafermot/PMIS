package com.example.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.director LEFT JOIN FETCH p.program LEFT JOIN FETCH p.sponsor")
    List<Project> findAllWithRelations();

}
