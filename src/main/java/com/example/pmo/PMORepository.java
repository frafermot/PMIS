package com.example.pmo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PMORepository extends JpaRepository<PMO, Long> {

    // Find all PMOs with details from the repository, @EntityGraph(attributePaths =
    // { "portfolio", "director" })
}
