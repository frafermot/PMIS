package com.example.program;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
public class ProgramServiceTest {

    @Autowired
    ProgramService programService;
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
        var portfolio = new Portfolio();
        portfolio.setName("Portfolio Name");
        portfolio.setDirector(manager);
        portfolioService.createOrUpdate(portfolio);
        var name = "Program Name";
        var program = new Program();
        program.setName(name);
        program.setPortfolio(portfolio);
        program.setDirector(manager);
        var createdProgram = programService.createOrUpdate(program);
        assertTrue(
                createdProgram.getName().equals(name) &&
                        createdProgram.getPortfolio().equals(portfolio) &&
                        createdProgram.getDirector().equals(manager));
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
        var name = "Program Name";
        var program = new Program();
        program.setName(name);
        program.setPortfolio(portfolio);
        program.setDirector(manager);
        var createdProgram = programService.createOrUpdate(program);
        var fetchedProgram = programService.get(createdProgram.getId());
        assertTrue(
                createdProgram.equals(fetchedProgram));
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
        var name = "Program Name";
        var program = new Program();
        program.setName(name);
        program.setPortfolio(portfolio);
        program.setDirector(manager);
        var createdProgram = programService.createOrUpdate(program);
        programService.delete(createdProgram.getId());
        var fetchedProgram = programService.get(createdProgram.getId());
        assertNull(fetchedProgram);
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
        var program1 = new Program();
        program1.setName("Program One");
        program1.setPortfolio(portfolio);
        program1.setDirector(manager);
        programService.createOrUpdate(program1);
        var program2 = new Program();
        program2.setName("Program Two");
        program2.setPortfolio(portfolio);
        program2.setDirector(manager);
        programService.createOrUpdate(program2);
        var allPrograms = programService.getAll();
        assertTrue(
                !allPrograms.isEmpty() &&
                        allPrograms.contains(program1) &&
                        allPrograms.contains(program2) &&
                        allPrograms.equals(programService.getAll()));
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

        var program = new Program();
        program.setName("Admin Program");
        program.setPortfolio(portfolio);
        program.setDirector(admin);

        var createdProgram = programService.createOrUpdate(program);
        assertNotNull(createdProgram);
        assertTrue(createdProgram.getName().equals("Admin Program"));
    }

    @Test
    public void testCreateOrUpdateAsManager() {
        var manager = new User();
        manager.setName("Manager User");
        manager.setUvus("manager");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);

        // Create portfolio without auth first
        var portfolio = new Portfolio();
        portfolio.setName("Test Portfolio");
        portfolio.setDirector(manager);
        var createdPortfolio = portfolioService.createOrUpdate(portfolio);

        // Clear context to avoid portfolio validation
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        // Now create with manager auth
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "manager", "password",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_MANAGER"))));

        var program = new Program();
        program.setName("Manager Program");
        program.setPortfolio(createdPortfolio);
        program.setDirector(manager);

        var createdProgram = programService.createOrUpdate(program);
        assertNotNull(createdProgram);
        assertTrue(createdProgram.getName().equals("Manager Program"));

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
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

        var program = new Program();
        program.setName("Program to Delete");
        program.setPortfolio(portfolio);
        program.setDirector(admin);
        var createdProgram = programService.createOrUpdate(program);

        programService.delete(createdProgram.getId());
        assertNull(programService.get(createdProgram.getId()));
    }

    @Test
    public void testDeleteAsManager() {
        var manager = new User();
        manager.setName("Manager User");
        manager.setUvus("manager");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);

        var portfolio = new Portfolio();
        portfolio.setName("Test Portfolio");
        portfolio.setDirector(manager);
        var createdPortfolio = portfolioService.createOrUpdate(portfolio);

        var program = new Program();
        program.setName("Program to Delete");
        program.setPortfolio(createdPortfolio);
        program.setDirector(manager);
        var createdProgram = programService.createOrUpdate(program);

        programService.delete(createdProgram.getId());
        assertNull(programService.get(createdProgram.getId()));
    }
}
