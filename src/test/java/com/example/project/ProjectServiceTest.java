package com.example.project;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
}
