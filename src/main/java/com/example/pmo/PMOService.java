package com.example.pmo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PMOService {

    private final PMORepository pmoRepository;

    public PMOService(PMORepository pmoRepository) {
        this.pmoRepository = pmoRepository;
    }

    // Find all PMOs with details from the repository

    public PMO createOrUpdate(PMO pmo) {
        return pmoRepository.save(pmo);
    }

    public PMO get(Long id) {
        return pmoRepository.findById(id).orElse(null);
    }

    public void delete(Long id) {
        pmoRepository.deleteById(id);
    }
}
