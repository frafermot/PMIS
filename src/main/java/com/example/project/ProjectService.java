package com.example.project;

import com.example.user.UserRepository;
import com.example.security.SecurityService;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SecurityService securityService;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository,
            SecurityService securityService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.securityService = securityService;
    }

    public Project createOrUpdate(Project project) {
        // Admins pueden crear y editar cualquier proyecto
        // Managers pueden editar proyectos donde son directores del programa O
        // directores del portfolio
        // Admins can create and edit any project
        // Portfolio Directors can edit projects in their portfolio
        // Program Directors can edit projects in their program
        if (!securityService.isAdmin()) {
            boolean allowed = false;

            if (project.getId() != null) {
                // Editing existing
                Project existing = projectRepository.findById(project.getId()).orElse(null);
                if (existing != null && existing.getProgram() != null) {
                    if (securityService.isProgramDirector(existing.getProgram().getId())) {
                        allowed = true;
                    } else if (existing.getProgram().getPortfolio() != null &&
                            securityService.isPortfolioDirector(existing.getProgram().getPortfolio().getId())) {
                        allowed = true;
                    }
                }
            } else {
                // Creating new
                if (project.getProgram() != null) {
                    if (securityService.isProgramDirector(project.getProgram().getId())) {
                        allowed = true;
                    } else if (project.getProgram().getPortfolio() != null &&
                            securityService.isPortfolioDirector(project.getProgram().getPortfolio().getId())) {
                        allowed = true;
                    }
                }
            }

            if (!allowed) {
                throw new SecurityException(
                        "Solo los administradores, directores de portfolio y directores de programa pueden gestionar proyectos");
            }
        }
        return projectRepository.save(project);
    }

    public Project get(Long id) {
        return projectRepository.findByIdWithRelations(id).orElse(null);
    }

    public void delete(Long id) {
        // Admins pueden eliminar cualquier proyecto
        // Managers pueden eliminar proyectos donde son directores del programa O
        // directores del portfolio
        // Admins can delete any project
        // Portfolio Directors can delete projects in their portfolio
        // Program Directors can delete projects in their program
        if (!securityService.isAdmin()) {
            Project project = projectRepository.findById(id).orElse(null);
            if (project == null)
                return;

            boolean allowed = false;
            if (project.getProgram() != null) {
                if (securityService.isProgramDirector(project.getProgram().getId())) {
                    allowed = true;
                } else if (project.getProgram().getPortfolio() != null &&
                        securityService.isPortfolioDirector(project.getProgram().getPortfolio().getId())) {
                    allowed = true;
                }
            }
            if (!allowed) {
                throw new SecurityException(
                        "Solo los administradores, directores de portfolio y directores de programa pueden eliminar proyectos");
            }
        }
        // Smart delete: desasignar todos los usuarios del proyecto antes de eliminarlo
        // para evitar violación de integridad referencial
        userRepository.unassignUsersFromProject(id);
        projectRepository.deleteById(id);
    }

    public List<Project> getAll() {
        // Admins ven todos los proyectos
        // Managers solo ven proyectos de programas donde son directores
        if (securityService.isAdmin()) {
            return projectRepository.findAllWithRelations();
        } else if (securityService.isManager()) {
            return projectRepository.findAllByProgramDirectorIdWithRelations(securityService.getCurrentUser().getId());
        }
        return projectRepository.findAllWithRelations();
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
        // Admins pueden eliminar cualquier proyecto
        // Managers pueden eliminar proyectos donde son directores del programa O
        // directores del portfolio
        // Admins can delete any project
        // Portfolio Directors can delete projects in their portfolio
        // Program Directors can delete projects in their program
        if (!securityService.isAdmin()) {
            Project project = projectRepository.findById(id).orElse(null);
            if (project == null)
                return;

            boolean allowed = false;
            if (project.getProgram() != null) {
                if (securityService.isProgramDirector(project.getProgram().getId())) {
                    allowed = true;
                } else if (project.getProgram().getPortfolio() != null &&
                        securityService.isPortfolioDirector(project.getProgram().getPortfolio().getId())) {
                    allowed = true;
                }
            }
            if (!allowed) {
                throw new SecurityException(
                        "Solo los administradores, directores de portfolio y directores de programa pueden eliminar proyectos");
            }
        }
        userRepository.unassignUsersFromProject(id);
        projectRepository.deleteById(id);
    }
}
