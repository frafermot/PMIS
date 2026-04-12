package com.example.communication;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CccRepository extends JpaRepository<Ccc, Long> {
    Optional<Ccc> findByProjectId(Long projectId);
}
