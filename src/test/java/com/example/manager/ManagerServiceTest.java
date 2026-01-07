package com.example.manager;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class ManagerServiceTest {
    
    @Autowired
    ManagerService managerService;

    @Test
    public void testCreateOrUpdate() {
        var name = "John Doe";
        var uvus = "jdoe";
        var isAdmin = true;
        var manager = new Manager();
        manager.setName(name);
        manager.setUvus(uvus);
        manager.setIsAdmin(isAdmin);
        var createdManager = managerService.createOrUpdate(manager);
        assertTrue(
            createdManager.getName().equals(name) &&
            createdManager.getUvus().equals(uvus) &&
            createdManager.getIsAdmin().equals(isAdmin));
    }

    @Test
    public void testGet() {
        var name = "Jane Smith";
        var uvus = "jsmith";
        var isAdmin = false;
        var manager = new Manager();
        manager.setName(name);
        manager.setUvus(uvus);
        manager.setIsAdmin(isAdmin);
        var createdManager = managerService.createOrUpdate(manager);
        var fetchedManager = managerService.get(createdManager.getId());
        assertTrue(
            createdManager.equals(fetchedManager));
    }

    @Test
    public void testDelete() {
        var name = "Alice Johnson";
        var uvus = "ajohnson";
        var isAdmin = true;
        var manager = new Manager();
        manager.setName(name);
        manager.setUvus(uvus);
        manager.setIsAdmin(isAdmin);
        var createdManager = managerService.createOrUpdate(manager);
        managerService.delete(createdManager.getId());
        var fetchedManager = managerService.get(createdManager.getId());
        assertNull(fetchedManager);
    }

    @Test
    public void testGetAll() {
        var manager1 = new Manager();
        manager1.setName("Manager One");
        manager1.setUvus("manager_one");
        manager1.setIsAdmin(false);
        managerService.createOrUpdate(manager1);
        var manager2 = new Manager();
        manager2.setName("Manager Two");
        manager2.setUvus("manager_two");
        manager2.setIsAdmin(true);
        managerService.createOrUpdate(manager2);
        var allManagers = managerService.getAll();
        assertTrue(
            !allManagers.isEmpty() &&
            allManagers.contains(manager1) &&
            allManagers.contains(manager2) &&
            allManagers.equals(managerService.getAll()));
    }
}
