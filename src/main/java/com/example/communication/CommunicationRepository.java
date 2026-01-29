package com.example.communication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CommunicationRepository extends JpaRepository<Communication, Long> {
    List<Communication> findAllByCccId(Long cccId);

    List<Communication> findAllByCccIdOrderByUpdatedAtDesc(Long cccId);

    @Query("SELECT c FROM Communication c JOIN FETCH c.ccc ccc JOIN FETCH ccc.project WHERE c.id = :id")
    Optional<Communication> findByIdWithDetails(@Param("id") Long id);
}
