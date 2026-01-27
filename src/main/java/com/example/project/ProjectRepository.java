package com.example.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.director LEFT JOIN FETCH p.program LEFT JOIN FETCH p.sponsor")
    List<Project> findAllWithRelations();

    long countByProgramId(Long programId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Project p WHERE p.program.id = :programId")
    void deleteByProgramId(@org.springframework.data.repository.query.Param("programId") Long programId);

    long countByDirectorId(Long directorId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Project p SET p.director = null WHERE p.director.id = :directorId")
    void unassignDirector(@org.springframework.data.repository.query.Param("directorId") Long directorId);

    long countBySponsorId(Long sponsorId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Project p SET p.sponsor = null WHERE p.sponsor.id = :sponsorId")
    void unassignSponsor(@org.springframework.data.repository.query.Param("sponsorId") Long sponsorId);

}
