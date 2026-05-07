package com.example.task;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskBaselineRepository extends JpaRepository<TaskBaseline, Long> {
    List<TaskBaseline> findByProjectBaseline(ProjectBaseline projectBaseline);
}
