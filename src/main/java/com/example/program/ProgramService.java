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

    public Program createOrUpdate(Program program) {
        return programRepository.save(program);
    }

    public Program get(Long id) {
        return programRepository.findById(id).orElse(null);
    }

    public void delete(Long id) {
        programRepository.deleteById(id);
    }
}
