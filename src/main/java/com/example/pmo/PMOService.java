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

    public PMO createOrUpdatePMO(PMO pmo) {
        return pmoRepository.save(pmo);
    }

    public PMO getPMOById(Long id) {
        return pmoRepository.findById(id).orElse(null);
    }

    public void deletePMO(Long id) {
        pmoRepository.deleteById(id);
    }
}
