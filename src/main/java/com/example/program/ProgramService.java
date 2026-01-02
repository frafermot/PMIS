package com.example.program;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProgramService {

    private final ProgramRepository programRepository;

    public ProgramService(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }

    // Find all programs with details from the repository,

    public Program createOrUpdateProgram(Program program) {
        return programRepository.save(program);
    }

    public Program getProgramById(Long id) {
        return programRepository.findById(id).orElse(null);
    }

    public void deleteProgram(Long id) {
        programRepository.deleteById(id);
    }
}
