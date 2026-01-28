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
        if (securityService.getCurrentUser() != null && !securityService.isAdmin()) {
            if (!securityService.isManager()) {
                throw new SecurityException("Solo los administradores y gestores pueden crear o editar proyectos");
            }
            // Si está editando, verificar permisos
            if (project.getId() != null) {
                Project existing = projectRepository.findById(project.getId()).orElse(null);
                if (existing == null || existing.getProgram() == null) {
                    throw new SecurityException("Proyecto o programa no encontrado");
                }
                // Permitir si es director del programa O director del portfolio
                boolean isProgramDirector = existing.getProgram().getDirector() != null &&
                        existing.getProgram().getDirector().getId().equals(securityService.getCurrentUser().getId());
                boolean isPortfolioDirector = existing.getProgram().getPortfolio() != null &&
                        existing.getProgram().getPortfolio().getDirector() != null &&
                        existing.getProgram().getPortfolio().getDirector().getId()
                                .equals(securityService.getCurrentUser().getId());

                if (!isProgramDirector && !isPortfolioDirector) {
                    throw new SecurityException(
                            "Solo puedes editar proyectos de programas donde eres director o de portfolios donde eres director");
                }
            } else {
                // Al crear, verificar que el programa pertenece a un portfolio del manager O
                // que es director del programa
                if (project.getProgram() == null) {
                    throw new SecurityException("Debes especificar un programa");
                }
                boolean isProgramDirector = project.getProgram().getDirector() != null &&
                        project.getProgram().getDirector().getId().equals(securityService.getCurrentUser().getId());
                boolean isPortfolioDirector = project.getProgram().getPortfolio() != null &&
                        project.getProgram().getPortfolio().getDirector() != null &&
                        project.getProgram().getPortfolio().getDirector().getId()
                                .equals(securityService.getCurrentUser().getId());

                if (!isProgramDirector && !isPortfolioDirector) {
                    throw new SecurityException(
                            "Solo puedes crear proyectos en programas donde eres director o de portfolios donde eres director");
                }
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
        if (securityService.getCurrentUser() != null && !securityService.isAdmin()) {
            if (!securityService.isManager()) {
                throw new SecurityException("Solo los administradores y gestores pueden eliminar proyectos");
            }
            Project project = projectRepository.findById(id).orElse(null);
            if (project == null || project.getProgram() == null) {
                throw new SecurityException("Proyecto o programa no encontrado");
            }
            // Permitir si es director del programa O director del portfolio
            boolean isProgramDirector = project.getProgram().getDirector() != null &&
                    project.getProgram().getDirector().getId().equals(securityService.getCurrentUser().getId());
            boolean isPortfolioDirector = project.getProgram().getPortfolio() != null &&
                    project.getProgram().getPortfolio().getDirector() != null &&
                    project.getProgram().getPortfolio().getDirector().getId()
                            .equals(securityService.getCurrentUser().getId());

            if (!isProgramDirector && !isPortfolioDirector) {
                throw new SecurityException(
                        "Solo puedes eliminar proyectos de programas donde eres director o de portfolios donde eres director");
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
        if (securityService.getCurrentUser() != null && !securityService.isAdmin()) {
            if (!securityService.isManager()) {
                throw new SecurityException("Solo los administradores y gestores pueden eliminar proyectos");
            }
            Project project = projectRepository.findById(id).orElse(null);
            if (project == null || project.getProgram() == null) {
                throw new SecurityException("Proyecto o programa no encontrado");
            }
            // Permitir si es director del programa O director del portfolio
            boolean isProgramDirector = project.getProgram().getDirector() != null &&
                    project.getProgram().getDirector().getId().equals(securityService.getCurrentUser().getId());
            boolean isPortfolioDirector = project.getProgram().getPortfolio() != null &&
                    project.getProgram().getPortfolio().getDirector() != null &&
                    project.getProgram().getPortfolio().getDirector().getId()
                            .equals(securityService.getCurrentUser().getId());

            if (!isProgramDirector && !isPortfolioDirector) {
                throw new SecurityException(
                        "Solo puedes eliminar proyectos de programas donde eres director o de portfolios donde eres director");
            }
        }
        userRepository.unassignUsersFromProject(id);
        projectRepository.deleteById(id);
    }
}
