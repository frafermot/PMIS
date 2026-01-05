package com.example.manager;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ManagerService {

    private final ManagerRepository managerRepository;

    public ManagerService(ManagerRepository managerRepository) {
        this.managerRepository = managerRepository;
    }

    public Manager createOrUpdate(Manager manager) {
        return managerRepository.save(manager);
    }

    public Manager get(Long managerId) {
        return managerRepository.findById(managerId).orElse(null);
    }

    public void delete(Long managerId) {
        managerRepository.deleteById(managerId);
    }

    public List<Manager> getAll() {
        return managerRepository.findAll();
    }
}
