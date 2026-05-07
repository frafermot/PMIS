package com.example.task;

import com.example.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectBaselineRepository extends JpaRepository<ProjectBaseline, Long> {
    List<ProjectBaseline> findByProjectOrderByCreatedAtDesc(Project project);
}
