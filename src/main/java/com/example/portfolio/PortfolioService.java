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
        if (securityService.getCurrentUser() != null) {
            if (!securityService.isAdmin()) {
                // Si es manager, verificar que sea el director del portfolio
                if (!securityService.isManager()) {
                    throw new SecurityException("Solo los administradores y gestores pueden crear o editar portfolios");
                }
                // Si está editando un portfolio existente, verificar que sea el director
                if (portfolio.getId() != null) {
                    Portfolio existing = portfolioRepository.findById(portfolio.getId()).orElse(null);
                    if (existing == null || existing.getDirector() == null ||
                            !existing.getDirector().getId().equals(securityService.getCurrentUser().getId())) {
                        throw new SecurityException("Solo puedes editar portfolios donde eres director");
                    }
                } else {
                    // Managers no pueden crear nuevos portfolios
                    throw new SecurityException("Solo los administradores pueden crear nuevos portfolios");
                }
            }
        }
        return portfolioRepository.save(portfolio);
    }

    public Portfolio get(Long id) {
        return portfolioRepository.findByIdWithDirector(id).orElse(null);
    }

    public void delete(Long id) {
        // Solo admins y managers (de sus propios portfolios) pueden eliminar
        if (securityService.getCurrentUser() != null) {
            if (!securityService.isAdmin()) {
                if (!securityService.isManager()) {
                    throw new SecurityException("Solo los administradores y gestores pueden eliminar portfolios");
                }
                // Verificar que el manager sea el director del portfolio
                Portfolio portfolio = portfolioRepository.findById(id).orElse(null);
                if (portfolio == null || portfolio.getDirector() == null ||
                        !portfolio.getDirector().getId().equals(securityService.getCurrentUser().getId())) {
                    throw new SecurityException("Solo puedes eliminar portfolios donde eres director");
                }
            }
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
        if (securityService.getCurrentUser() != null) {
            if (!securityService.isAdmin()) {
                if (!securityService.isManager()) {
                    throw new SecurityException("Solo los administradores y gestores pueden eliminar portfolios");
                }
                // Verificar que el manager sea el director del portfolio
                Portfolio portfolio = portfolioRepository.findById(id).orElse(null);
                if (portfolio == null || portfolio.getDirector() == null ||
                        !portfolio.getDirector().getId().equals(securityService.getCurrentUser().getId())) {
                    throw new SecurityException("Solo puedes eliminar portfolios donde eres director");
                }
            }
        }
        List<Program> programs = programRepository.findAllByPortfolioId(id);
        for (Program program : programs) {
            programService.deleteWithCascade(program.getId());
        }
        pmoRepository.deleteByPortfolioId(id);
        portfolioRepository.deleteById(id);
    }
}
