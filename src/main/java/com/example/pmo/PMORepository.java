package com.example.pmo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PMORepository extends JpaRepository<PMO, Long> {

    @Query("SELECT p FROM PMO p LEFT JOIN FETCH p.portfolio LEFT JOIN FETCH p.director")
    List<PMO> findAllWithRelations();

}
