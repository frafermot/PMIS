package com.example.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.project")
    List<User> findAllWithProject();

    User findByUvus(String uvus);

    List<User> findAllByRole(Role role);

    List<User> findAllByRoleIn(List<Role> roles);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE User u SET u.project = null WHERE u.project.id IN (SELECT p.id FROM Project p WHERE p.program.id = :programId)")
    void unassignUsersFromProjectsInProgram(
            @org.springframework.data.repository.query.Param("programId") Long programId);

    long countByProjectId(Long projectId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE User u SET u.project = null WHERE u.project.id = :projectId")
    void unassignUsersFromProject(@org.springframework.data.repository.query.Param("projectId") Long projectId);

}
