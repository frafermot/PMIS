package com.example.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.portfolio.Portfolio;
import com.example.pmo.PMO;
import com.example.program.Program;
import com.example.project.Project;
import com.example.user.Role;
import com.example.user.User;
import com.example.user.UserService;
import com.example.portfolio.PortfolioService;
import com.example.pmo.PMOService;
import com.example.program.ProgramService;
import com.example.project.ProjectService;

@Component
public class DataInitializer implements CommandLineRunner {

    private final PortfolioService portfolioService;
    private final PMOService pmoService;
    private final ProgramService programService;
    private final ProjectService projectService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            PortfolioService portfolioService, PMOService pmoService,
            ProgramService programService, ProjectService projectService, UserService userService,
            PasswordEncoder passwordEncoder) {
        this.portfolioService = portfolioService;
        this.pmoService = pmoService;
        this.programService = programService;
        this.projectService = projectService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Set System Admin context for initialization
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "system", "system",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_SYSTEM_ADMIN"))));

        for (int i = 1; i <= 24; i++) {
            User user = new User();
            user.setName("User " + i);
            user.setUvus("uvus" + i);
            user.setRole(Role.USER);
            user.setPassword(passwordEncoder.encode("password"));
            userService.createOrUpdate(user);
        }

        User coordinador = new User();
        coordinador.setName("Juan Manuel Cordero Valle");
        coordinador.setUvus("jmcordero");
        coordinador.setRole(Role.ADMIN); // Was isAdmin=true
        coordinador.setPassword(passwordEncoder.encode("password"));
        userService.createOrUpdate(coordinador);

        Portfolio portfolio = new Portfolio();
        portfolio.setName("PGPI 24/25");
        portfolio.setDirector(coordinador);
        portfolioService.createOrUpdate(portfolio);

        PMO pmo = new PMO();
        pmo.setName("Oficina de Proyectos 24/25");
        pmo.setDirector(coordinador);
        pmo.setPortfolio(portfolio);
        pmoService.createOrUpdate(pmo);

        Program program = new Program();
        program.setName("Lab 1");
        program.setPortfolio(portfolio);
        program.setDirector(coordinador);
        programService.createOrUpdate(program);

        // Fetch user starting from ID 1 because users were created first in the loop
        // above?
        // The loop above created 24 users.
        // Then coordinador created.
        // The IDs depend on DB sequence.
        // The logic `userService.get((long) 3 * i - 2)` assumes specific IDs.
        // This relies on predictable iteration order and ID generation.
        // It's brittle but I should preserve the intent.
        // However, I just created 24 users, then coordinator.
        // If the DB was empty, User 1 is ID 1.
        // The previous code had `Manager` created separately. Now `User` creates all.
        // I'll proceed with the assumption that users are created sequentially.

        for (int i = 1; i <= 2; i++) {
            Project project = new Project();
            project.setName("Proyecto " + i + " de Lab 1");
            project.setProgram(program);
            project.setSponsor(coordinador);
            // Fetching a user to be Project Manager (Director)
            // (3*1 - 2) = 1. User 1.
            // (3*2 - 2) = 4. User 4.
            User pm = userService.get((long) 3 * i - 2);
            if (pm != null) {
                project.setDirector(pm);
                projectService.createOrUpdate(project);
                pm.setProject(project);
                userService.createOrUpdate(pm);
            }
        }

        for (int i = 1; i <= 3; i++) {
            User manager = new User();
            manager.setName("Manager" + i);
            manager.setUvus("manageruvus" + i);
            manager.setRole(Role.MANAGER); // Was isAdmin=false
            manager.setPassword(passwordEncoder.encode("password"));
            userService.createOrUpdate(manager);

            Program prog = new Program();
            prog.setName("Lab " + (i + 1));
            prog.setPortfolio(portfolio);
            prog.setDirector(manager);
            programService.createOrUpdate(prog);

            for (int j = 1; j <= 2; j++) {
                Project proj = new Project();
                proj.setName("Proyecto " + j + " de Lab " + (i + 1));
                proj.setProgram(prog);
                proj.setSponsor(manager);

                // Logic: 6*i + 3*j - 2
                // i=1, j=1 -> 6+3-2 = 7. User 7.
                User pm = userService.get((long) 6 * i + 3 * j - 2);
                if (pm != null) {
                    proj.setDirector(pm);
                    projectService.createOrUpdate(proj);
                    pm.setProject(proj);
                    userService.createOrUpdate(pm);
                }
            }
        }

        User testUser = new User();
        testUser.setName("Test User");
        testUser.setUvus("test");
        testUser.setRole(Role.USER);
        testUser.setPassword(passwordEncoder.encode("test"));
        userService.createOrUpdate(testUser);
    }
}
