package com.example.task;

import com.example.project.Project;
import com.example.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.assignee a LEFT JOIN FETCH a.resource LEFT JOIN FETCH t.predecessor LEFT JOIN FETCH t.parentGroup WHERE t.project = :project ORDER BY t.startDate ASC")
    List<Task> findByProjectOrderByStartDateAsc(@Param("project") Project project);

    @Query("SELECT t FROM Task t WHERE t.assignee = :assignee AND t.id != :excludeTaskId AND " +
           "((t.startDate <= :endDate AND t.endDate >= :startDate))")
    List<Task> findOverlappingTasksForAssignee(
            @Param("assignee") User assignee,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            @Param("excludeTaskId") Long excludeTaskId
    );

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.assignee a LEFT JOIN FETCH a.resource LEFT JOIN FETCH t.predecessor LEFT JOIN FETCH t.parentGroup WHERE t.predecessor = :predecessor")
    List<Task> findByPredecessor(@Param("predecessor") Task predecessor);
}
