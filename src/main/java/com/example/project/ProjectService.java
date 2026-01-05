package com.example.project;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // Find all projects with details from the repository

    public Project createOrUpdate(Project project) {
        return projectRepository.save(project);
    }

    public Project get(Long id) {
        return projectRepository.findById(id).orElse(null);
    }

    public void delete(Long id) {
        projectRepository.deleteById(id);
    }
}
