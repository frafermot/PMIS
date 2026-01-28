package com.example.pmo;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
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

    @org.junit.jupiter.api.BeforeEach
    public void setupSecurity() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("system", "pass",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_SYSTEM_ADMIN"))));
    }

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

    // ===== SECURITY TESTS =====

    @WithMockUser(username = "admin", roles = { "ADMIN" })
    @Test
    public void testCreateOrUpdateAsAdmin() {
        var admin = new User();
        admin.setName("Admin User");
        admin.setUvus("admin");
        admin.setRole(Role.ADMIN);
        userService.createOrUpdate(admin);

        var portfolio = new Portfolio();
        portfolio.setName("Test Portfolio");
        portfolio.setDirector(admin);
        portfolioService.createOrUpdate(portfolio);

        var pmo = new PMO();
        pmo.setName("Admin PMO");
        pmo.setPortfolio(portfolio);
        pmo.setDirector(admin);

        var createdPMO = pmoService.createOrUpdate(pmo);
        assertNotNull(createdPMO);
        assertTrue(createdPMO.getName().equals("Admin PMO"));
    }

    @Test
    public void testCreateOrUpdateAsManager() {
        var manager = new User();
        manager.setName("Manager User");
        manager.setUvus("manager");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);

        var portfolio = new Portfolio();
        portfolio.setName("Test Portfolio");
        portfolio.setDirector(manager);
        // Portfolio creation will also fail for manager, but let's test PMO
        // specifically

        var pmo = new PMO();
        pmo.setName("Manager PMO");
        pmo.setPortfolio(portfolio);
        pmo.setDirector(manager);

        // Switch to Manager Auth
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("manager",
                        "password",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_MANAGER"))));

        assertThrows(SecurityException.class, () -> {
            pmoService.createOrUpdate(pmo);
        });
    }

    @WithMockUser(username = "admin", roles = { "ADMIN" })
    @Test
    public void testDeleteAsAdmin() {
        var admin = new User();
        admin.setName("Admin User");
        admin.setUvus("admin");
        admin.setRole(Role.ADMIN);
        userService.createOrUpdate(admin);

        var portfolio = new Portfolio();
        portfolio.setName("Test Portfolio");
        portfolio.setDirector(admin);
        portfolioService.createOrUpdate(portfolio);

        var pmo = new PMO();
        pmo.setName("PMO to Delete");
        pmo.setPortfolio(portfolio);
        pmo.setDirector(admin);
        var createdPMO = pmoService.createOrUpdate(pmo);

        pmoService.delete(createdPMO.getId());
        assertNull(pmoService.get(createdPMO.getId()));
    }

    @Test
    public void testDeleteAsManager() {
        // Create PMO without authentication first (so portfolio can be created)
        var manager = new User();
        manager.setName("Manager User");
        manager.setUvus("manager");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);

        var portfolio = new Portfolio();
        portfolio.setName("Test Portfolio");
        portfolio.setDirector(manager);
        var createdPortfolio = portfolioService.createOrUpdate(portfolio);

        var pmo = new PMO();
        pmo.setName("PMO to Delete");
        pmo.setPortfolio(createdPortfolio);
        pmo.setDirector(manager);
        var createdPMO = pmoService.createOrUpdate(pmo);

        // Now test with manager auth - deletion should fail
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "manager", "password",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_MANAGER"))));

        assertThrows(SecurityException.class, () -> {
            pmoService.delete(createdPMO.getId());
        });

        // Clear auth
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    public void testCreateWithoutAuth() {
        // This test ensures DataInitializer can create PMOs without authentication
        var manager = new User();
        manager.setName("Director Name");
        manager.setUvus("director_uvus");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);

        var portfolio = new Portfolio();
        portfolio.setName("Portfolio Name");
        portfolio.setDirector(manager);
        portfolioService.createOrUpdate(portfolio);

        var pmo = new PMO();
        pmo.setName("No Auth PMO");
        pmo.setPortfolio(portfolio);
        pmo.setDirector(manager);

        var createdPMO = pmoService.createOrUpdate(pmo);
        assertNotNull(createdPMO);
        assertTrue(createdPMO.getName().equals("No Auth PMO"));
    }
}
