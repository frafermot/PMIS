package com.example.portfolio;

import com.example.program.Program;
import com.example.program.ProgramRepository;
import com.example.program.ProgramService;
import com.example.pmo.PMORepository;
import com.example.security.SecurityService;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final ProgramRepository programRepository;
    private final ProgramService programService;
    private final PMORepository pmoRepository;
    private final SecurityService securityService;

    public PortfolioService(PortfolioRepository portfolioRepository, ProgramRepository programRepository,
            ProgramService programService, PMORepository pmoRepository, SecurityService securityService) {
        this.portfolioRepository = portfolioRepository;
        this.programRepository = programRepository;
        this.programService = programService;
        this.pmoRepository = pmoRepository;
        this.securityService = securityService;
    }

    public Portfolio createOrUpdate(Portfolio portfolio) {
        // Solo admins pueden crear portfolios nuevos
        // Managers solo pueden editar portfolios donde son directores
        // Only admins can create or update portfolios
        if (securityService.getCurrentUser() != null && !securityService.isAdmin()
                && !securityService.isSystemAdmin()) {
            throw new SecurityException("Solo los administradores pueden crear o editar portfolios");
        }
        return portfolioRepository.save(portfolio);
    }

    public Portfolio get(Long id) {
        return portfolioRepository.findByIdWithDirector(id).orElse(null);
    }

    public void delete(Long id) {
        // Solo admins y managers (de sus propios portfolios) pueden eliminar
        // Only admins can delete portfolios
        if (securityService.getCurrentUser() != null && !securityService.isAdmin()
                && !securityService.isSystemAdmin()) {
            throw new SecurityException("Solo los administradores pueden eliminar portfolios");
        }
        pmoRepository.deleteByPortfolioId(id);
        portfolioRepository.deleteById(id);
    }

    public List<Portfolio> getAll() {
        // Admins ven todos los portfolios
        // Managers solo ven portfolios donde son directores O directores de programas
        if (securityService.isAdmin()) {
            return portfolioRepository.findAllWithDirector();
        } else if (securityService.isManager()) {
            // Managers see portfolios where they are director OR where they direct a
            // program
            return portfolioRepository.findByDirectorIdOrProgramDirectorId(securityService.getCurrentUser().getId());
        }
        return portfolioRepository.findAllWithDirector();
    }

    public boolean hasPrograms(Long id) {
        return programRepository.countByPortfolioId(id) > 0;
    }

    public void deleteWithCascade(Long id) {
        // Solo admins y managers (de sus propios portfolios) pueden eliminar
        // Only admins can delete portfolios
        if (securityService.getCurrentUser() != null && !securityService.isAdmin()
                && !securityService.isSystemAdmin()) {
            throw new SecurityException("Solo los administradores pueden eliminar portfolios");
        }
        List<Program> programs = programRepository.findAllByPortfolioId(id);
        for (Program program : programs) {
            programService.deleteWithCascade(program.getId());
        }
        pmoRepository.deleteByPortfolioId(id);
        portfolioRepository.deleteById(id);
    }
}
