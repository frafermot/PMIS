package com.example.pmo;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.user.User;
import com.example.user.UserService;
import com.example.user.Role;
import com.example.portfolio.Portfolio;
import com.example.portfolio.PortfolioService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class PMOServiceTest {

    @Autowired
    PMOService pmoService;
    @Autowired
    UserService userService;
    @Autowired
    PortfolioService portfolioService;

    @Test
    public void testCreateOrUpdate() {
        var manager = new User();
        manager.setName("Director Name");
        manager.setUvus("director_uvus");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);
        var portfolio = new Portfolio();
        portfolio.setName("Portfolio Name");
        portfolio.setDirector(manager);
        portfolioService.createOrUpdate(portfolio);
        var name = "PMO Name";
        var pmo = new PMO();
        pmo.setName(name);
        pmo.setPortfolio(portfolio);
        pmo.setDirector(manager);
        var createdPMO = pmoService.createOrUpdate(pmo);
        assertTrue(
                createdPMO.getName().equals(name) &&
                        createdPMO.getPortfolio().equals(portfolio) &&
                        createdPMO.getDirector().equals(manager));
    }

    @Test
    public void testGet() {
        var manager = new User();
        manager.setName("Director Name");
        manager.setUvus("director_uvus");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);
        var portfolio = new Portfolio();
        portfolio.setName("Portfolio Name");
        portfolio.setDirector(manager);
        portfolioService.createOrUpdate(portfolio);
        var name = "PMO Name";
        var pmo = new PMO();
        pmo.setName(name);
        pmo.setPortfolio(portfolio);
        pmo.setDirector(manager);
        var createdPMO = pmoService.createOrUpdate(pmo);
        var fetchedPMO = pmoService.get(createdPMO.getId());
        assertTrue(
                createdPMO.equals(fetchedPMO));
    }

    @Test
    public void testDelete() {
        var manager = new User();
        manager.setName("Director Name");
        manager.setUvus("director_uvus");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);
        var portfolio = new Portfolio();
        portfolio.setName("Portfolio Name");
        portfolio.setDirector(manager);
        portfolioService.createOrUpdate(portfolio);
        var name = "PMO Name";
        var pmo = new PMO();
        pmo.setName(name);
        pmo.setPortfolio(portfolio);
        pmo.setDirector(manager);
        var createdPMO = pmoService.createOrUpdate(pmo);
        pmoService.delete(createdPMO.getId());
        var fetchedPMO = pmoService.get(createdPMO.getId());
        assertNull(fetchedPMO);
    }

    @Test
    public void testGetAll() {
        var manager = new User();
        manager.setName("Director Name");
        manager.setUvus("director_uvus");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);
        var portfolio = new Portfolio();
        portfolio.setName("Portfolio Name");
        portfolio.setDirector(manager);
        portfolioService.createOrUpdate(portfolio);
        var pmo1 = new PMO();
        pmo1.setName("PMO One");
        pmo1.setPortfolio(portfolio);
        pmo1.setDirector(manager);
        pmoService.createOrUpdate(pmo1);
        var pmo2 = new PMO();
        pmo2.setName("PMO Two");
        pmo2.setPortfolio(portfolio);
        pmo2.setDirector(manager);
        pmoService.createOrUpdate(pmo2);
        var allPMOs = pmoService.getAll();
        assertTrue(
                !allPMOs.isEmpty() &&
                        allPMOs.contains(pmo1) &&
                        allPMOs.contains(pmo2) &&
                        allPMOs.equals(pmoService.getAll()));
    }
}
