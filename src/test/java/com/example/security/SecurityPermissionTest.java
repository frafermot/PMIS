package com.example.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.example.pmo.PMO;
import com.example.pmo.PMOService;
import com.example.portfolio.Portfolio;
import com.example.portfolio.PortfolioService;
import com.example.program.Program;
import com.example.program.ProgramService;
import com.example.project.Project;
import com.example.project.ProjectService;
import com.example.user.Role;
import com.example.user.User;
import com.example.user.UserService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
public class SecurityPermissionTest {

    @Autowired
    UserService userService;
    @Autowired
    PortfolioService portfolioService;
    @Autowired
    PMOService pmoService;
    @Autowired
    ProgramService programService;
    @Autowired
    ProjectService projectService;
    @Autowired
    SecurityService securityService;

    private User admin;
    private User manager1; // Portfolio Director
    private User manager2; // Program Director
    private User manager3; // PMO Director
    private User manager4; // Project Director
    private User regularUser;

    @BeforeEach
    public void setup() {
        // Set System role to create initial users
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("system", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));

        // Create users using "system" access
        admin = createUser("admin", Role.ADMIN);
        manager1 = createUser("manager1", Role.MANAGER);
        manager2 = createUser("manager2", Role.MANAGER);
        manager3 = createUser("manager3", Role.MANAGER); // Will be PMO Director
        manager4 = createUser("manager4", Role.MANAGER); // Will be Project Director
        regularUser = createUser("user", Role.USER);

        SecurityContextHolder.clearContext();
    }

    private User createUser(String username, Role role) {
        User u = new User();
        u.setName(username);
        u.setUvus(username);
        u.setRole(role);
        return userService.createOrUpdate(u);
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUvus(), "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
    }

    private void authenticateAsSystem() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("system", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
    }

    @Test
    public void testPortfolioDirectorCanManagePrograms() {
        authenticateAsSystem();

        // Setup Portfolio with manager1 as director
        Portfolio portfolio = new Portfolio();
        portfolio.setName("P1");
        portfolio.setDirector(manager1);
        portfolio = portfolioService.createOrUpdate(portfolio);

        // Authenticate as Portfolio Director
        authenticate(manager1);

        // Try to create Program
        Program program = new Program();
        program.setName("Prog1");
        program.setPortfolio(portfolio);
        program.setDirector(manager2); // Assign manager2 as Program Director
        Program created = programService.createOrUpdate(program);
        assertNotNull(created);

        // Verify update
        created.setName("Prog1 Updated");
        programService.createOrUpdate(created);

        // Verify delete
        programService.delete(created.getId());
    }

    @Test
    public void testNonPortfolioDirectorCannotManagePrograms() {
        authenticateAsSystem();

        // Setup Portfolio with manager1 as director
        Portfolio portfolio = new Portfolio();
        portfolio.setName("P1");
        portfolio.setDirector(manager1);
        portfolio = portfolioService.createOrUpdate(portfolio);

        // Authenticate as random manager (manager2)
        authenticate(manager2);

        // Try to create Program in manager1's portfolio
        Program program = new Program();
        program.setName("Prog1");
        program.setPortfolio(portfolio);
        program.setDirector(manager2);

        assertThrows(SecurityException.class, () -> {
            programService.createOrUpdate(program);
        });
    }

    @Test
    public void testProgramDirectorCanManageProjects() {
        authenticateAsSystem();

        // Setup
        Portfolio portfolio = new Portfolio();
        portfolio.setName("P1");
        portfolio.setDirector(manager1);
        portfolio = portfolioService.createOrUpdate(portfolio);

        Program program = new Program();
        program.setName("Prog1");
        program.setPortfolio(portfolio);
        program.setDirector(manager2); // manager2 is Program Director
        // Create program as admin/system
        program = programService.createOrUpdate(program);

        // Authenticate as Program Director (manager2)
        authenticate(manager2);

        // Create Project
        Project project = new Project();
        project.setName("Proj1");
        project.setProgram(program);
        project.setDirector(manager4); // manager4 will be Project Director
        Project created = projectService.createOrUpdate(project);
        assertNotNull(created);

        // Update
        created.setName("Proj1 Updated");
        projectService.createOrUpdate(created);

        // Delete
        projectService.delete(created.getId());
    }

    @Test
    public void testPortfolioDirectorCannotManageProjects() {
        // Portfolio Director should NOT be able to manage projects directly (strict)
        authenticateAsSystem();

        Portfolio portfolio = new Portfolio();
        portfolio.setName("P1");
        portfolio.setDirector(manager1);
        portfolio = portfolioService.createOrUpdate(portfolio);

        Program program = new Program();
        program.setName("Prog1");
        program.setPortfolio(portfolio);
        program.setDirector(manager2);
        program = programService.createOrUpdate(program);

        // Authenticate as Portfolio Director (manager1)
        authenticate(manager1);

        // Create Project
        Project project = new Project();
        project.setName("Proj1");
        project.setProgram(program);

        // Assert throws
        assertThrows(SecurityException.class, () -> {
            projectService.createOrUpdate(project);
        });
    }

    @Test
    public void testPmoDirectorCanManageUsers() {
        authenticateAsSystem();

        // Setup PMO with manager3 as director
        Portfolio portfolio = new Portfolio();
        portfolio.setName("P1");
        portfolio.setDirector(manager1);
        portfolio = portfolioService.createOrUpdate(portfolio);

        PMO pmo = new PMO();
        pmo.setName("PMO1");
        pmo.setPortfolio(portfolio);
        pmo.setDirector(manager3);
        pmo = pmoService.createOrUpdate(pmo);

        // Authenticate as PMO Director
        authenticate(manager3);

        // Create User (Role USER)
        User newUser = new User();
        newUser.setName("New User");
        newUser.setUvus("newuser");
        newUser.setRole(Role.USER);
        User created = userService.createOrUpdate(newUser);
        assertNotNull(created);

        // Cannot create Manager
        User newManager = new User();
        newManager.setName("New Manager");
        newManager.setUvus("newmanager");
        newManager.setRole(Role.MANAGER);
        assertThrows(SecurityException.class, () -> {
            userService.createOrUpdate(newManager);
        });

        // Delete User
        userService.delete(created.getId());
    }

    @Test
    public void testAdminCanManageManagers() {
        authenticate(admin);

        // Admin SHOULD allow managing managers
        User manager = new User();
        manager.setRole(Role.MANAGER);
        manager.setUvus("manager_test");
        manager.setName("Manager Test");
        User createdManager = userService.createOrUpdate(manager);
        assertNotNull(createdManager);
        assertEquals(Role.MANAGER, createdManager.getRole());
    }

    @Test
    public void testAdminCannotManageUsers() {
        authenticate(admin);

        // Admin SHOULD NOT allow managing users (only PMO Director can)
        User user = new User();
        user.setRole(Role.USER);
        user.setUvus("user_test");
        user.setName("User Test");

        assertThrows(SecurityException.class, () -> {
            userService.createOrUpdate(user);
        });
    }

    @Test
    public void testAdminCannotCreateProgram() {
        authenticate(admin);

        // Admin SHOULD NOT allow creating programs (only Portfolio Director can)
        Program program = new Program();
        program.setName("AdminProg");

        assertThrows(SecurityException.class, () -> {
            programService.createOrUpdate(program);
        });
    }

    @Test
    public void testAdminCannotCreateProject() {
        authenticate(admin);

        // Admin SHOULD NOT allow creating projects (only Program Director can)
        Project project = new Project();
        project.setName("AdminProj");

        assertThrows(SecurityException.class, () -> {
            projectService.createOrUpdate(project);
        });
    }

    @Test
    public void testProjectDirectorCanAssignUser() {
        authenticateAsSystem();

        // Setup Hierarchy
        Portfolio portfolio = new Portfolio();
        portfolio.setName("P1");
        portfolio.setDirector(manager1);
        portfolio = portfolioService.createOrUpdate(portfolio);

        Program program = new Program();
        program.setName("Prog1");
        program.setPortfolio(portfolio);
        program.setDirector(manager2);
        program = programService.createOrUpdate(program);

        Project project = new Project();
        project.setName("Proj1");
        project.setProgram(program);
        project.setDirector(manager4); // manager4 is Project Director
        project = projectService.createOrUpdate(project);

        // Authenticate as Project Director
        authenticate(manager4);

        // Assign existing user to project
        // Note: Project Director cannot create users, but assume they fetch existing
        // user and update project
        User u = regularUser; // existing user
        u.setProject(project);

        User updated = userService.createOrUpdate(u);
        assertNotNull(updated.getProject());
        assertTrue(updated.getProject().getId().equals(project.getId()));
    }

    @Test
    public void testProjectDirectorCannotAssignUserToOtherProject() {
        authenticateAsSystem();

        // Setup Hierarchy for Project 1
        Portfolio portfolio = new Portfolio();
        portfolio.setName("P1");
        portfolio.setDirector(manager1);
        portfolio = portfolioService.createOrUpdate(portfolio);
        Program program = new Program();
        program.setName("Prog1");
        program.setPortfolio(portfolio);
        program.setDirector(manager2);
        program = programService.createOrUpdate(program);
        Project project1 = new Project();
        project1.setName("Proj1");
        project1.setProgram(program);
        project1.setDirector(manager4);
        project1 = projectService.createOrUpdate(project1);

        // Another Project
        Project project2 = new Project();
        project2.setName("Proj2");
        project2.setProgram(program);
        // manager4 is NOT director of project2
        project2 = projectService.createOrUpdate(project2);

        // Authenticate as Project Director of Project 1
        authenticate(manager4);

        User u = regularUser;
        u.setProject(project2); // Try to assign to project 2

        assertThrows(SecurityException.class, () -> {
            userService.createOrUpdate(u);
        });
    }

    @Test
    public void testSystemAdminCanManageEverything() {
        authenticateAsSystem();

        // Portfolios
        Portfolio p = new Portfolio();
        p.setName("AdminP");
        // System admin can assign any director?
        // Or create without director? The test sets 'admin' as director.
        // Assuming 'admin' variable is available (it's a field).
        p.setDirector(admin);
        p = portfolioService.createOrUpdate(p);

        // PMO
        PMO pmo = new PMO();
        pmo.setName("AdminPMO");
        pmo.setPortfolio(p);
        pmoService.createOrUpdate(pmo);

        // Program
        Program prog = new Program();
        prog.setName("AdminProg");
        prog.setPortfolio(p);
        prog = programService.createOrUpdate(prog);

        // Project
        Project proj = new Project();
        proj.setName("AdminProj");
        proj.setProgram(prog);
        projectService.createOrUpdate(proj);

        // User
        User u = new User();
        u.setName("AdminCreated");
        u.setUvus("adminu");
        u.setRole(Role.MANAGER);
        userService.createOrUpdate(u);
    }
}
