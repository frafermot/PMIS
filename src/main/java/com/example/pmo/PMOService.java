package com.example.pmo;

import java.util.List;

import com.example.security.SecurityService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PMOService {

    private final PMORepository pmoRepository;
    private final SecurityService securityService;

    public PMOService(PMORepository pmoRepository, SecurityService securityService) {
        this.pmoRepository = pmoRepository;
        this.securityService = securityService;
    }

    public PMO createOrUpdate(PMO pmo) {
        // Allow system initialization (no user) or System Admin bypass
        if (securityService.getCurrentUser() == null || securityService.isSystemAdmin()) {
            return pmoRepository.save(pmo);
        }

        // Solo admins pueden crear o editar PMOs
        if (!securityService.isAdmin()) {
            throw new SecurityException("Solo los administradores pueden crear o editar PMOs");
        }
        return pmoRepository.save(pmo);
    }

    public PMO get(Long id) {
        return pmoRepository.findById(id).orElse(null);
    }

    public void delete(Long id) {
        // Allow system initialization (no user) or System Admin bypass
        if (securityService.getCurrentUser() == null || securityService.isSystemAdmin()) {
            pmoRepository.deleteById(id);
            return;
        }

        // Solo admins pueden eliminar PMOs
        if (!securityService.isAdmin()) {
            throw new SecurityException("Solo los administradores pueden eliminar PMOs");
        }
        pmoRepository.deleteById(id);
    }

    public List<PMO> getAll() {
        List<PMO> pmos = pmoRepository.findAllWithRelations();
        return pmos;
    }
}
