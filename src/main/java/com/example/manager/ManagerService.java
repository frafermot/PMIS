package com.example.manager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ManagerService {

    private final ManagerRepository managerRepository;

    public ManagerService(ManagerRepository managerRepository) {
        this.managerRepository = managerRepository;
    }

    // Find all managers with details from the repository

    public Manager createOrUpdate(Manager manager) {
        return managerRepository.save(manager);
    }

    public Manager get(Long managerId) {
        return managerRepository.findById(managerId).orElse(null);
    }

    public void delete(Long managerId) {
        managerRepository.deleteById(managerId);
    }
}
