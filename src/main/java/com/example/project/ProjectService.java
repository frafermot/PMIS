package com.example.project;

import com.example.user.UserRepository;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public Project createOrUpdate(Project project) {
        return projectRepository.save(project);
    }

    public Project get(Long id) {
        return projectRepository.findByIdWithRelations(id).orElse(null);
    }

    public void delete(Long id) {
        projectRepository.deleteById(id);
    }

    public List<Project> getAll() {
        List<Project> projects = projectRepository.findAllWithRelations();
        return projects;
    }

    public List<Project> getByProgramId(Long programId) {
        return projectRepository.findAllByProgramId(programId);
    }

    public List<Project> getByUserId(Long userId) {
        return projectRepository.findAllByUserId(userId);
    }

    public boolean hasAssignedUsers(Long id) {
        return userRepository.countByProjectId(id) > 0;
    }

    public void deleteSafe(Long id) {
        userRepository.unassignUsersFromProject(id);
        projectRepository.deleteById(id);
    }
}
