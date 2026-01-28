package com.example.program;

import com.example.project.ProjectRepository;
import com.example.security.SecurityService;
import com.example.user.UserRepository;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProgramService {

    private final ProgramRepository programRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SecurityService securityService;

    public ProgramService(ProgramRepository programRepository, ProjectRepository projectRepository,
            UserRepository userRepository, SecurityService securityService) {
        this.programRepository = programRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.securityService = securityService;
    }

    public Program createOrUpdate(Program program) {
        // Admins pueden crear y editar cualquier programa
        // Managers pueden editar programas donde son directores O programas de
        // portfolios donde son directores
        if (securityService.getCurrentUser() != null && !securityService.isAdmin()) {
            if (!securityService.isManager()) {
                throw new SecurityException("Solo los administradores y gestores pueden crear o editar programas");
            }
            // Si está editando, verificar permisos
            if (program.getId() != null) {
                Program existing = programRepository.findById(program.getId()).orElse(null);
                if (existing == null) {
                    throw new SecurityException("Programa no encontrado");
                }
                // Permitir si es director del programa O director del portfolio
                boolean isDirector = existing.getDirector() != null &&
                        existing.getDirector().getId().equals(securityService.getCurrentUser().getId());
                boolean isPortfolioDirector = existing.getPortfolio() != null &&
                        existing.getPortfolio().getDirector() != null &&
                        existing.getPortfolio().getDirector().getId().equals(securityService.getCurrentUser().getId());

                if (!isDirector && !isPortfolioDirector) {
                    throw new SecurityException(
                            "Solo puedes editar programas donde eres director o de portfolios donde eres director");
                }
            } else {
                // Al crear, verificar que el portfolio pertenece al manager
                if (program.getPortfolio() == null || program.getPortfolio().getDirector() == null ||
                        !program.getPortfolio().getDirector().getId()
                                .equals(securityService.getCurrentUser().getId())) {
                    throw new SecurityException("Solo puedes crear programas en portfolios donde eres director");
                }
            }
        }
        return programRepository.save(program);
    }

    public Program get(Long id) {
        return programRepository.findByIdWithRelations(id).orElse(null);
    }

    public void delete(Long id) {
        // Admins pueden eliminar cualquier programa
        // Managers pueden eliminar programas donde son directores O de portfolios donde
        // son directores
        if (securityService.getCurrentUser() != null && !securityService.isAdmin()) {
            if (!securityService.isManager()) {
                throw new SecurityException("Solo los administradores y gestores pueden eliminar programas");
            }
            Program program = programRepository.findById(id).orElse(null);
            if (program == null) {
                throw new SecurityException("Programa no encontrado");
            }
            // Permitir si es director del programa O director del portfolio
            boolean isDirector = program.getDirector() != null &&
                    program.getDirector().getId().equals(securityService.getCurrentUser().getId());
            boolean isPortfolioDirector = program.getPortfolio() != null &&
                    program.getPortfolio().getDirector() != null &&
                    program.getPortfolio().getDirector().getId().equals(securityService.getCurrentUser().getId());

            if (!isDirector && !isPortfolioDirector) {
                throw new SecurityException(
                        "Solo puedes eliminar programas donde eres director o de portfolios donde eres director");
            }
        }
        programRepository.deleteById(id);
    }

    public List<Program> getAll() {
        // Admins ven todos los programas
        // Managers solo ven programas donde son directores
        if (securityService.isAdmin()) {
            return programRepository.findAllWithRelations();
        } else if (securityService.isManager()) {
            return programRepository.findAllByDirectorIdWithRelations(securityService.getCurrentUser().getId());
        }
        return programRepository.findAllWithRelations();
    }

    public List<Program> getByPortfolioId(Long portfolioId) {
        return programRepository.findAllByPortfolioId(portfolioId);
    }

    public boolean hasProjects(Long id) {
        return projectRepository.countByProgramId(id) > 0;
    }

    public void deleteWithCascade(Long id) {
        // Admins pueden eliminar cualquier programa
        // Managers pueden eliminar programas donde son directores O de portfolios donde
        // son directores
        if (securityService.getCurrentUser() != null && !securityService.isAdmin()) {
            if (!securityService.isManager()) {
                throw new SecurityException("Solo los administradores y gestores pueden eliminar programas");
            }
            Program program = programRepository.findById(id).orElse(null);
            if (program == null) {
                throw new SecurityException("Programa no encontrado");
            }
            // Permitir si es director del programa O director del portfolio
            boolean isDirector = program.getDirector() != null &&
                    program.getDirector().getId().equals(securityService.getCurrentUser().getId());
            boolean isPortfolioDirector = program.getPortfolio() != null &&
                    program.getPortfolio().getDirector() != null &&
                    program.getPortfolio().getDirector().getId().equals(securityService.getCurrentUser().getId());

            if (!isDirector && !isPortfolioDirector) {
                throw new SecurityException(
                        "Solo puedes eliminar programas donde eres director o de portfolios donde eres director");
            }
        }
        userRepository.unassignUsersFromProjectsInProgram(id);
        projectRepository.deleteByProgramId(id);
        programRepository.deleteById(id);
    }
}
