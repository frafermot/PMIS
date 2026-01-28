package com.example.portfolio;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class PortfolioServiceTest {

    @Autowired
    PortfolioService portfolioService;
    @Autowired
    UserService userService;

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
        var manager = new User();
        manager.setName("Director Name");
        manager.setUvus("director_uvus");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);
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
        var manager = new User();
        manager.setName("Director Name");
        manager.setUvus("director_uvus");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);
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
        var manager = new User();
        manager.setName("Director Name");
        manager.setUvus("director_uvus");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);
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
        portfolio.setName("Admin Portfolio");
        portfolio.setDirector(admin);

        var createdPortfolio = portfolioService.createOrUpdate(portfolio);
        assertNotNull(createdPortfolio);
        assertTrue(createdPortfolio.getName().equals("Admin Portfolio"));
    }

    @Test
    public void testCreateOrUpdateAsManager() {
        var manager = new User();
        manager.setName("Manager User");
        manager.setUvus("manager");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);

        var portfolio = new Portfolio();
        portfolio.setName("Manager Portfolio");
        portfolio.setDirector(manager);

        // Switch to Manager Auth
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("manager",
                        "password",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_MANAGER"))));

        assertThrows(SecurityException.class, () -> {
            portfolioService.createOrUpdate(portfolio);
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
        portfolio.setName("Portfolio to Delete");
        portfolio.setDirector(admin);
        var createdPortfolio = portfolioService.createOrUpdate(portfolio);

        portfolioService.delete(createdPortfolio.getId());
        assertNull(portfolioService.get(createdPortfolio.getId()));
    }

    @Test
    public void testDeleteAsManager() {
        // Create portfolio without authentication first
        var manager = new User();
        manager.setName("Manager User");
        manager.setUvus("manager");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);

        var portfolio = new Portfolio();
        portfolio.setName("Portfolio to Delete");
        portfolio.setDirector(manager);
        var createdPortfolio = portfolioService.createOrUpdate(portfolio);

        // Now test with manager auth - deletion should fail
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "manager", "password",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_MANAGER"))));

        assertThrows(SecurityException.class, () -> {
            portfolioService.delete(createdPortfolio.getId());
        });

        // Clear auth
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @WithMockUser(username = "admin", roles = { "ADMIN" })
    @Test
    public void testDeleteWithCascadeAsAdmin() {
        var admin = new User();
        admin.setName("Admin User");
        admin.setUvus("admin");
        admin.setRole(Role.ADMIN);
        userService.createOrUpdate(admin);

        var portfolio = new Portfolio();
        portfolio.setName("Portfolio with Cascade");
        portfolio.setDirector(admin);
        var createdPortfolio = portfolioService.createOrUpdate(portfolio);

        portfolioService.deleteWithCascade(createdPortfolio.getId());
        assertNull(portfolioService.get(createdPortfolio.getId()));
    }

    @Test
    public void testDeleteWithCascadeAsManager() {
        // Create portfolio without authentication first
        var manager = new User();
        manager.setName("Manager User");
        manager.setUvus("manager");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);

        var portfolio = new Portfolio();
        portfolio.setName("Portfolio to Delete with Cascade");
        portfolio.setDirector(manager);
        var createdPortfolio = portfolioService.createOrUpdate(portfolio);

        // Now test with manager auth - deletion should fail
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "manager", "password",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_MANAGER"))));

        assertThrows(SecurityException.class, () -> {
            portfolioService.deleteWithCascade(createdPortfolio.getId());
        });

        // Clear auth
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    public void testCreateWithoutAuth() {
        // This test ensures DataInitializer can create Portfolios without
        // authentication
        var manager = new User();
        manager.setName("Director Name");
        manager.setUvus("director_uvus");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);

        var portfolio = new Portfolio();
        portfolio.setName("No Auth Portfolio");
        portfolio.setDirector(manager);

        var createdPortfolio = portfolioService.createOrUpdate(portfolio);
        assertNotNull(createdPortfolio);
        assertTrue(createdPortfolio.getName().equals("No Auth Portfolio"));
    }
}
