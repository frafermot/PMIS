package com.example.program;

import com.example.project.ProjectRepository;
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

    public ProgramService(ProgramRepository programRepository, ProjectRepository projectRepository,
            UserRepository userRepository) {
        this.programRepository = programRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public Program createOrUpdate(Program program) {
        return programRepository.save(program);
    }

    public Program get(Long id) {
        return programRepository.findByIdWithRelations(id).orElse(null);
    }

    public void delete(Long id) {
        programRepository.deleteById(id);
    }

    public List<Program> getAll() {
        List<Program> programs = programRepository.findAllWithRelations();
        return programs;
    }

    public List<Program> getByPortfolioId(Long portfolioId) {
        return programRepository.findAllByPortfolioId(portfolioId);
    }

    public boolean hasProjects(Long id) {
        return projectRepository.countByProgramId(id) > 0;
    }

    public void deleteWithCascade(Long id) {
        userRepository.unassignUsersFromProjectsInProgram(id);
        projectRepository.deleteByProgramId(id);
        programRepository.deleteById(id);
    }
}
