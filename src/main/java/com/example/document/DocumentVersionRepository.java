package com.example.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    @Query("SELECT v FROM DocumentVersion v LEFT JOIN FETCH v.createdBy WHERE v.document.id = :documentId ORDER BY v.createdAt DESC")
    List<DocumentVersion> findByDocumentIdOrderByCreatedAtDesc(@Param("documentId") Long documentId);
}
