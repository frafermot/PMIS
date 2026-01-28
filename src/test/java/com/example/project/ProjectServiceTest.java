package com.example.project;

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
import com.example.program.Program;
import com.example.program.ProgramService;
import com.example.portfolio.Portfolio;
import com.example.portfolio.PortfolioService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class ProjectServiceTest {

    @Autowired
    ProjectService projectService;
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
        var program = new Program();
        program.setName("Program Name");
        program.setPortfolio(portfolio);
        program.setDirector(manager);
        programService.createOrUpdate(program);
        var director = new User();
        director.setName("Project Director");
        director.setUvus("proj_dir_uvus");
        userService.createOrUpdate(director);
        var name = "Project Name";
        var project = new Project();
        project.setName(name);
        project.setProgram(program);
        project.setSponsor(manager);
        project.setDirector(director);
        var createdProject = projectService.createOrUpdate(project);
        assertTrue(
                createdProject.getName().equals(name) &&
                        createdProject.getProgram().equals(program) &&
                        createdProject.getSponsor().equals(manager) &&
                        createdProject.getDirector().equals(director));
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
        var program = new Program();
        program.setName("Program Name");
        program.setPortfolio(portfolio);
        program.setDirector(manager);
        programService.createOrUpdate(program);
        var director = new User();
        director.setName("Project Director");
        director.setUvus("proj_dir_uvus");
        userService.createOrUpdate(director);
        var name = "Project Name";
        var project = new Project();
        project.setName(name);
        project.setProgram(program);
        project.setSponsor(manager);
        project.setDirector(director);
        var createdProject = projectService.createOrUpdate(project);
        var fetchedProject = projectService.get(createdProject.getId());
        assertTrue(
                createdProject.equals(fetchedProject));
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
        var program = new Program();
        program.setName("Program Name");
        program.setPortfolio(portfolio);
        program.setDirector(manager);
        programService.createOrUpdate(program);
        var director = new User();
        director.setName("Project Director");
        director.setUvus("proj_dir_uvus");
        userService.createOrUpdate(director);
        var name = "Project Name";
        var project = new Project();
        project.setName(name);
        project.setProgram(program);
        project.setSponsor(manager);
        project.setDirector(director);
        var createdProject = projectService.createOrUpdate(project);
        projectService.delete(createdProject.getId());
        var fetchedProject = projectService.get(createdProject.getId());
        assertNull(fetchedProject);
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
        var program = new Program();
        program.setName("Program Name");
        program.setPortfolio(portfolio);
        program.setDirector(manager);
        programService.createOrUpdate(program);
        var director1 = new User();
        director1.setName("Director One");
        director1.setUvus("dir_one_uvus");
        userService.createOrUpdate(director1);
        var director2 = new User();
        director2.setName("Director Two");
        director2.setUvus("dir_two_uvus");
        userService.createOrUpdate(director2);
        var project1 = new Project();
        project1.setName("Project One");
        project1.setProgram(program);
        project1.setSponsor(manager);
        project1.setDirector(director1);
        var project2 = new Project();
        project2.setName("Project Two");
        project2.setProgram(program);
        project2.setSponsor(manager);
        project2.setDirector(director2);
        var p1 = projectService.createOrUpdate(project1);
        var p2 = projectService.createOrUpdate(project2);
        var allProjects = projectService.getAll();
        assertTrue(
                !allProjects.isEmpty() &&
                        allProjects.contains(p1) &&
                        allProjects.contains(p2) &&
                        allProjects.equals(projectService.getAll()));
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
        program.setName("Test Program");
        program.setPortfolio(portfolio);
        program.setDirector(admin);
        programService.createOrUpdate(program);

        var director = new User();
        director.setName("Project Director");
        director.setUvus("proj_director");
        userService.createOrUpdate(director);

        var project = new Project();
        project.setName("Admin Project");
        project.setProgram(program);
        project.setSponsor(admin);
        project.setDirector(director);

        var createdProject = projectService.createOrUpdate(project);
        assertNotNull(createdProject);
        assertTrue(createdProject.getName().equals("Admin Project"));
    }

    @Test
    public void testCreateOrUpdateAsManager() {
        var manager = new User();
        manager.setName("Manager User");
        manager.setUvus("manager");
        manager.setRole(Role.MANAGER);
        userService.createOrUpdate(manager);

        // Create dependencies without auth
        var portfolio = new Portfolio();
        portfolio.setName("Test Portfolio");
        portfolio.setDirector(manager);
        var createdPortfolio = portfolioService.createOrUpdate(portfolio);

        var program = new Program();
        program.setName("Test Program");
        program.setPortfolio(createdPortfolio);
        program.setDirector(manager);
        var createdProgram = programService.createOrUpdate(program);

        var director = new User();
        director.setName("Project Director");
        director.setUvus("proj_director");
        userService.createOrUpdate(director);

        // Clear and set manager context
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "manager", "password",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_MANAGER"))));

        var project = new Project();
        project.setName("Manager Project");
        project.setProgram(createdProgram);
        project.setSponsor(manager);
        project.setDirector(director);

        var createdProject = projectService.createOrUpdate(project);
        assertNotNull(createdProject);
        assertTrue(createdProject.getName().equals("Manager Project"));

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
        program.setName("Test Program");
        program.setPortfolio(portfolio);
        program.setDirector(admin);
        programService.createOrUpdate(program);

        var director = new User();
        director.setName("Project Director");
        director.setUvus("proj_director");
        userService.createOrUpdate(director);

        var project = new Project();
        project.setName("Project to Delete");
        project.setProgram(program);
        project.setSponsor(admin);
        project.setDirector(director);
        var createdProject = projectService.createOrUpdate(project);

        projectService.delete(createdProject.getId());
        assertNull(projectService.get(createdProject.getId()));
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
        program.setName("Test Program");
        program.setPortfolio(createdPortfolio);
        program.setDirector(manager);
        var createdProgram = programService.createOrUpdate(program);

        var director = new User();
        director.setName("Project Director");
        director.setUvus("proj_director");
        userService.createOrUpdate(director);

        var project = new Project();
        project.setName("Project to Delete");
        project.setProgram(createdProgram);
        project.setSponsor(manager);
        project.setDirector(director);
        var createdProject = projectService.createOrUpdate(project);

        projectService.delete(createdProject.getId());
        assertNull(projectService.get(createdProject.getId()));
    }
}
