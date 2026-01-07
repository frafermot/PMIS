package com.example.portfolio;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.manager.Manager;
import com.example.manager.ManagerService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class PortfolioServiceTest {
    
    @Autowired
    PortfolioService portfolioService;
    @Autowired
    ManagerService managerService;

    @Test
    public void testCreateOrUpdate() {
        var manager = new Manager();
        manager.setName("Director Name");
        manager.setUvus("director_uvus");
        manager.setIsAdmin(true);
        managerService.createOrUpdate(manager);
        var name = "Portfolio Name";
        var portfolio = new Portfolio();
        portfolio.setName(name);
        portfolio.setDirector(manager);
        var createdPortfolio = portfolioService.createOrUpdate(portfolio);
        assertTrue(
            createdPortfolio.getName().equals(name) &&
            createdPortfolio.getDirector().equals(manager));
    }

    @Test
    public void testGet() {
        var manager = new Manager();
        manager.setName("Director Name");
        manager.setUvus("director_uvus");
        manager.setIsAdmin(true);
        managerService.createOrUpdate(manager);
        var name = "Portfolio Name";
        var portfolio = new Portfolio();
        portfolio.setName(name);
        portfolio.setDirector(manager);
        var createdPortfolio = portfolioService.createOrUpdate(portfolio);
        var fetchedPortfolio = portfolioService.get(createdPortfolio.getId());
        assertTrue(
            createdPortfolio.equals(fetchedPortfolio));
    }

    @Test
    public void testDelete() {
        var manager = new Manager();
        manager.setName("Director Name");
        manager.setUvus("director_uvus");
        manager.setIsAdmin(true);
        managerService.createOrUpdate(manager);
        var name = "Portfolio Name";
        var portfolio = new Portfolio();
        portfolio.setName(name);
        portfolio.setDirector(manager);
        var createdPortfolio = portfolioService.createOrUpdate(portfolio);
        portfolioService.delete(createdPortfolio.getId());
        var fetchedPortfolio = portfolioService.get(createdPortfolio.getId());
        assertNull(fetchedPortfolio);
    }

    @Test
    public void testGetAll() {
        var manager = new Manager();
        manager.setName("Director Name");
        manager.setUvus("director_uvus");
        manager.setIsAdmin(true);
        managerService.createOrUpdate(manager);
        var portfolio1 = new Portfolio();
        portfolio1.setName("Portfolio Name 1");
        portfolio1.setDirector(manager);
        var portfolio2 = new Portfolio();
        portfolio2.setName("Portfolio Name 2");
        portfolio2.setDirector(manager);
        var pmo1 = portfolioService.createOrUpdate(portfolio1);
        var pmo2 = portfolioService.createOrUpdate(portfolio2);
        var allPMOs = portfolioService.getAll();
        assertTrue(
            allPMOs != null &&
             !allPMOs.isEmpty() &&
             allPMOs.contains(pmo1) &&
             allPMOs.contains(pmo2) &&
             allPMOs.equals(portfolioService.getAll()));
    }
}
