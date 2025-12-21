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

    public Manager createOrUpdateManager(Manager manager) {
        return managerRepository.save(manager);
    }

    public Manager getManager(Long managerId) {
        return managerRepository.findById(managerId).orElse(null);
    }

    public void deleteManager(Long managerId) {
        managerRepository.deleteById(managerId);
    }
}
