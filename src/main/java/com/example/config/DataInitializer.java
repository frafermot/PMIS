package com.example.config;


import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.manager.Manager;
import com.example.portfolio.Portfolio;
import com.example.pmo.PMO;
import com.example.program.Program;
import com.example.project.Project;
import com.example.user.User;

import com.example.manager.ManagerService;
import com.example.portfolio.PortfolioService;
import com.example.pmo.PMOService;
import com.example.program.ProgramService;
import com.example.project.ProjectService;
import com.example.user.UserService;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ManagerService managerService;
    private final PortfolioService portfolioService;
    private final PMOService pmoService;
    private final ProgramService programService;
    private final ProjectService projectService;
    private final UserService userService;
    
    public DataInitializer(
        ManagerService managerService, PortfolioService portfolioService, PMOService pmoService,
        ProgramService programService, ProjectService projectService, UserService userService) {

        this.managerService = managerService;
        this.portfolioService = portfolioService;
        this.pmoService = pmoService;
        this.programService = programService;
        this.projectService = projectService;
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {

        for (int i = 1; i <= 24; i++) {
            User user = new User();
            user.setName("User " + i);
            user.setUvus("uvus" + i);
            userService.createOrUpdateUser(user);
        }

        Manager coordinador = new Manager();
        coordinador.setName("Juan Manuel Cordero Valle");
        coordinador.setUvus("jmcordero");
        coordinador.setIsAdmin(true);
        managerService.createOrUpdateManager(coordinador);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("PGPI 24/25");
        portfolio.setDirector(coordinador);
        portfolioService.createOrUpdatePortfolio(portfolio);

        PMO pmo = new PMO();
        pmo.setName("Oficina de Proyectos 24/25");
        pmo.setDirector(coordinador);
        pmo.setPortfolio(portfolio);
        pmoService.createOrUpdatePMO(pmo);

        Program program = new Program();
        program.setName("Lab 1");
        program.setPortfolio(portfolio);
        program.setDirector(coordinador);
        programService.createOrUpdateProgram(program);

        for (int i = 1; i <= 2; i++) {
            Project project = new Project();
            project.setName("Proyecto " + i + " de Lab 1");
            project.setProgram(program);
            project.setSponsor(coordinador);
            User pm = userService.getUser((long) 3 * i - 2);
            project.setDirector(pm);
            projectService.createOrUpdateProject(project);
            pm.setProject(project);
            userService.createOrUpdateUser(pm);
        }

        for (int i = 1; i <= 3; i++) {
            Manager manager = new Manager();
            manager.setName("Manager" + i);
            manager.setUvus("manageruvus" + i);
            manager.setIsAdmin(false);
            managerService.createOrUpdateManager(manager);
        
            Program prog = new Program();
            prog.setName("Lab " + (i+1));
            prog.setPortfolio(portfolio);
            prog.setDirector(manager);
            programService.createOrUpdateProgram(prog);

            for (int j = 1; j <= 2; j++) {
                Project proj = new Project();
                proj.setName("Proyecto " + j + " de Lab " + (i+1));
                proj.setProgram(prog);
                proj.setSponsor(manager);
                User pm = userService.getUser((long) 6 * i + 3*j - 2);
                proj.setDirector(pm);
                projectService.createOrUpdateProject(proj);
                pm.setProject(proj);
                userService.createOrUpdateUser(pm);
            }
        }
    }
}
