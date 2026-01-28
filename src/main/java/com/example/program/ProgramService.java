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
        // Admins pueden crear y editar cualquier programa
        // Portfolio Directors pueden editar programas de sus portfolios
        if (!securityService.isAdmin()) {
            boolean allowed = false;
            if (program.getId() != null) {
                // Editing existing program
                Program existing = programRepository.findById(program.getId()).orElse(null);
                if (existing != null && existing.getPortfolio() != null) {
                    allowed = securityService.isPortfolioDirector(existing.getPortfolio().getId());
                }
            } else {
                // Creating new program
                if (program.getPortfolio() != null) {
                    allowed = securityService.isPortfolioDirector(program.getPortfolio().getId());
                }
            }

            if (!allowed) {
                throw new SecurityException(
                        "Solo los administradores y directores de portfolio pueden gestionar programas");
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
        // Admins pueden eliminar cualquier programa
        // Portfolio Directors pueden eliminar programas de sus portfolios
        if (!securityService.isAdmin()) {
            Program program = programRepository.findById(id).orElse(null);
            if (program == null) {
                // Let repository handle not found or just return
                return;
            }
            // Check if user is director of the portfolio
            boolean isPortfolioDirector = false;
            if (program.getPortfolio() != null) {
                isPortfolioDirector = securityService.isPortfolioDirector(program.getPortfolio().getId());
            }

            if (!isPortfolioDirector) {
                throw new SecurityException(
                        "Solo los administradores y directores de portfolio pueden eliminar programas");
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
        // Admins pueden eliminar cualquier programa
        // Portfolio Directors pueden eliminar programas de sus portfolios
        if (!securityService.isAdmin()) {
            Program program = programRepository.findById(id).orElse(null);
            if (program == null) {
                return;
            }

            boolean isPortfolioDirector = false;
            if (program.getPortfolio() != null) {
                isPortfolioDirector = securityService.isPortfolioDirector(program.getPortfolio().getId());
            }

            if (!isPortfolioDirector) {
                throw new SecurityException(
                        "Solo los administradores y directores de portfolio pueden eliminar programas");
            }
        }
        userRepository.unassignUsersFromProjectsInProgram(id);
        projectRepository.deleteByProgramId(id);
        programRepository.deleteById(id);
    }
}
