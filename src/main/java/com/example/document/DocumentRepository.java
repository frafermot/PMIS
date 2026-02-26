package com.example.document;

import java.util.List;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("SELECT d FROM Document d WHERE d.project.id = :projectId")
    List<Document> findByProjectId(Long projectId);

    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.project p LEFT JOIN FETCH p.program WHERE d.id = :id")
    Optional<Document> findByIdWithProjectAndProgram(@Param("id") Long id);

}
